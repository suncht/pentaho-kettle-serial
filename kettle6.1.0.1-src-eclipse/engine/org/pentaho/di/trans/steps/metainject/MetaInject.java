/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2013 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.trans.steps.metainject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.injection.bean.BeanInjectionInfo;
import org.pentaho.di.core.injection.bean.BeanInjector;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.RowProducer;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransStoppedListener;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.RowAdapter;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInjectionMetaEntry;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInjectionInterface;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Read a simple CSV file Just output Strings found in the file...
 *
 * @author Matt
 * @since 2007-07-05
 */
public class MetaInject extends BaseStep implements StepInterface {
  private static Class<?> PKG = MetaInject.class; // for i18n purposes, needed
                                                  // by Translator2!!

  private MetaInjectMeta meta;
  private MetaInjectData data;

  public MetaInject( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
      Trans trans ) {
    super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
  }

  public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
    meta = (MetaInjectMeta) smi;
    data = (MetaInjectData) sdi;

    // Read the data from all input steps and keep it in memory...
    // Skip the step from which we stream data. Keep that available for runtime action.
    //
    data.rowMap = new HashMap<String, List<RowMetaAndData>>();
    for ( String prevStepName : getTransMeta().getPrevStepNames( getStepMeta() ) ) {
      // Don't read from the streaming source step
      //
      if ( !data.streaming || !prevStepName.equalsIgnoreCase( data.streamingSourceStepname ) ) {
        List<RowMetaAndData> list = new ArrayList<RowMetaAndData>();
        RowSet rowSet = findInputRowSet( prevStepName );
        Object[] row = getRowFrom( rowSet );
        while ( row != null ) {
          RowMetaAndData rd = new RowMetaAndData();
          rd.setRowMeta( rowSet.getRowMeta() );
          rd.setData( row );
          list.add( rd );

          row = getRowFrom( rowSet );
        }
        if ( !list.isEmpty() ) {
          data.rowMap.put( prevStepName, list );
        }
      }
    }

    for ( Map.Entry<String, StepMetaInterface> en : data.stepInjectionMetasMap.entrySet() ) {
      newInjection( en.getKey(), en.getValue() );
    }

    for ( String targetStep : data.stepInjectionMap.keySet() ) {
      if ( !data.stepInjectionMetasMap.containsKey( targetStep ) ) {
        oldInjection( targetStep );
      }
    }

    if ( log.isDetailed() ) {
      logDetailed( "XML of transformation after injection: " + data.transMeta.getXML() );
    }
    String targetFile = environmentSubstitute( meta.getTargetFile() );
    if ( !Const.isEmpty( targetFile ) ) {
      OutputStream os = null;
      try {
        os = KettleVFS.getOutputStream( targetFile, false );
        os.write( XMLHandler.getXMLHeader().getBytes( Const.XML_ENCODING ) );
        os.write( data.transMeta.getXML().getBytes( Const.XML_ENCODING ) );
      } catch ( IOException e ) {
        throw new KettleException( "Unable to write target file (ktr after injection) to file '" + targetFile + "'",
            e );
      } finally {
        if ( os != null ) {
          try {
            os.close();
          } catch ( Exception e ) {
            throw new KettleException( e );
          }
        }
      }
    }

    if ( !meta.isNoExecution() ) {
      // Now we can execute this modified transformation metadata.
      //
      final Trans injectTrans = new Trans( data.transMeta, this );
      if ( getTrans().getParentJob() != null ) {
        injectTrans.setParentJob( getTrans().getParentJob() ); // See PDI-13224
      }
      getTrans().addTransStoppedListener( new TransStoppedListener() {
        public void transStopped( Trans parentTrans ) {
          injectTrans.stopAll();
        }
      } );
      injectTrans.prepareExecution( null );

      // See if we need to stream some data over...
      //
      RowProducer rowProducer = null;
      if ( data.streaming ) {
        rowProducer = injectTrans.addRowProducer( data.streamingTargetStepname, 0 );
      }

      // Finally, add the mapping transformation to the active sub-transformations
      // map in the parent transformation
      //
      getTrans().getActiveSubtransformations().put( getStepname(), injectTrans );

      if ( !Const.isEmpty( meta.getSourceStepName() ) ) {
        StepInterface stepInterface = injectTrans.getStepInterface( meta.getSourceStepName(), 0 );
        if ( stepInterface == null ) {
          throw new KettleException( "Unable to find step '" + meta.getSourceStepName() + "' to read from." );
        }
        stepInterface.addRowListener( new RowAdapter() {
          @Override
          public void rowWrittenEvent( RowMetaInterface rowMeta, Object[] row ) throws KettleStepException {
            // Just pass along the data as output of this step...
            //
            MetaInject.this.putRow( rowMeta, row );
          }
        } );
      }

      injectTrans.startThreads();

      if ( data.streaming ) {
        // Deplete all the rows from the parent transformation into the modified transformation
        //
        RowSet rowSet = findInputRowSet( data.streamingSourceStepname );
        if ( rowSet == null ) {
          throw new KettleException( "Unable to find step '" + data.streamingSourceStepname + "' to stream data from" );
        }
        Object[] row = getRowFrom( rowSet );
        while ( row != null && !isStopped() ) {
          rowProducer.putRow( rowSet.getRowMeta(), row );
          row = getRowFrom( rowSet );
        }
        rowProducer.finished();
      }

      // Wait until the child transformation finished processing...
      //
      while ( !injectTrans.isFinished() && !injectTrans.isStopped() && !isStopped() ) {
        copyResult( injectTrans );

        // Wait a little bit.
        try {
          Thread.sleep( 50 );
        } catch ( Exception e ) {
          // Ignore errors
        }
      }
      copyResult( injectTrans );
    }

    // All done!

    setOutputDone();

    return false;
  }

  private void newInjection( String targetStep, StepMetaInterface targetStepMeta ) throws KettleException {
    if ( log.isDetailed() ) {
      logDetailed( "Handing step '" + targetStep + "' injection!" );
    }
    BeanInjectionInfo injectionInfo = new BeanInjectionInfo( targetStepMeta.getClass() );
    BeanInjector injector = new BeanInjector( injectionInfo );

    // Collect all the metadata for this target step...
    //
    Map<TargetStepAttribute, SourceStepField> targetMap = meta.getTargetSourceMapping();
    for ( TargetStepAttribute target : targetMap.keySet() ) {
      SourceStepField source = targetMap.get( target );

      if ( target.getStepname().equalsIgnoreCase( targetStep ) ) {
        // This is the step to collect data for...
        // We also know which step to read the data from. (source)
        //
        List<RowMetaAndData> rows = data.rowMap.get( source.getStepname() );
        if ( rows != null && !rows.isEmpty() ) {
          // Which metadata key is this referencing? Find the attribute key in the metadata entries...
          //
          if ( injector.hasProperty( targetStepMeta, target.getAttributeKey() ) ) {
            // target step has specified key
            boolean skip = false;
            for ( RowMetaAndData r : rows ) {
              if ( r.getRowMeta().indexOfValue( source.getField() ) < 0 ) {
                logError( BaseMessages.getString( PKG, "MetaInject.SourceFieldIsNotDefined.Message", source.getField(),
                    getTransMeta().getName() ) );
                // source step doesn't contain specified field
                skip = true;
              }
            }
            if ( !skip ) {
              // specified field exist - need to inject
              injector.setProperty( targetStepMeta, target.getAttributeKey(), rows, source.getField() );
            }
          } else {
            // target step doesn't have specified key - just report but don't fail like in 6.0 (BACKLOG-6753)
            logError( BaseMessages.getString( PKG, "MetaInject.TargetKeyIsNotDefined.Message", target.getAttributeKey(),
                getTransMeta().getName() ) );
          }
        }
      }
    }
  }

  private void oldInjection( String targetStep ) throws KettleException {

    if ( log.isDetailed() ) {
      logDetailed( "Handing step '" + targetStep + "' injection!" );
    }

    // This is the injection interface:
    //
    StepMetaInjectionInterface injectionInterface = data.stepInjectionMap.get( targetStep );

    // This is the injection description:
    //
    List<StepInjectionMetaEntry> metadataEntries = injectionInterface.getStepInjectionMetadataEntries();

    // Create a new list of metadata injection entries...
    //
    List<StepInjectionMetaEntry> inject = new ArrayList<StepInjectionMetaEntry>();

    // Collect all the metadata for this target step...
    //
    Map<TargetStepAttribute, SourceStepField> targetMap = meta.getTargetSourceMapping();
    for ( TargetStepAttribute target : targetMap.keySet() ) {
      SourceStepField source = targetMap.get( target );

      if ( target.getStepname().equalsIgnoreCase( targetStep ) ) {
        // This is the step to collect data for...
        // We also know which step to read the data from. (source)
        //
        List<RowMetaAndData> rows = data.rowMap.get( source.getStepname() );
        if ( rows != null && rows.size() > 0 ) {
          // Which metadata key is this referencing? Find the attribute key in the metadata entries...
          //
          StepInjectionMetaEntry entry = findMetaEntry( metadataEntries, target.getAttributeKey() );
          if ( entry != null ) {
            if ( !target.isDetail() ) {
              setEntryValueIfFieldExists( entry, rows.get( 0 ), source );
              inject.add( entry );
            } else {
              // We are going to pass this entry N times for N target mappings
              // As such, we have to see if it's already in the injection list...
              //
              StepInjectionMetaEntry metaEntries = findMetaEntry( inject, entry.getKey() );
              if ( metaEntries == null ) {

                StepInjectionMetaEntry rootEntry = findDetailRootEntry( metadataEntries, entry );

                // Inject an empty copy
                //
                metaEntries = rootEntry.clone();
                metaEntries.setDetails( new ArrayList<StepInjectionMetaEntry>() );
                inject.add( metaEntries );

                // We also need to pre-populate the whole grid: X rows by Y attributes
                //
                StepInjectionMetaEntry metaEntry = rootEntry.getDetails().get( 0 );

                for ( int i = 0; i < rows.size(); i++ ) {
                  StepInjectionMetaEntry metaCopy = metaEntry.clone();
                  metaEntries.getDetails().add( metaCopy );
                  metaCopy.setDetails( new ArrayList<StepInjectionMetaEntry>() );

                  for ( StepInjectionMetaEntry me : metaEntry.getDetails() ) {
                    StepInjectionMetaEntry meCopy = me.clone();
                    metaCopy.getDetails().add( meCopy );
                  }
                }

                // From now on we can simply refer to the correct X,Y coordinate.
              } else {
                StepInjectionMetaEntry rootEntry = findDetailRootEntry( inject, metaEntries );
                metaEntries = rootEntry;
              }

              for ( int i = 0; i < rows.size(); i++ ) {
                RowMetaAndData row = rows.get( i );
                try {
                  List<StepInjectionMetaEntry> rowEntries = metaEntries.getDetails().get( i ).getDetails();

                  for ( StepInjectionMetaEntry rowEntry : rowEntries ) {
                    // We have to look up the sources for these targets again in the target-2-source mapping
                    // That is because we only want handle this as few times as possible...
                    //
                    SourceStepField detailSource = findDetailSource( targetMap, targetStep, rowEntry.getKey() );
                    if ( detailSource != null ) {
                      setEntryValueIfFieldExists( rowEntry, row, detailSource );
                    } else {
                      if ( log.isDetailed() ) {
                        logDetailed( "No detail source found for key: " + rowEntry.getKey() + " and target step: "
                            + targetStep );
                      }
                    }
                  }
                } catch ( Exception e ) {
                  throw new KettleException( "Unexpected error occurred while injecting metadata", e );
                }
              }

              if ( log.isDetailed() ) {
                logDetailed( "injected entry: " + entry );
              }
            }
            // End of TopLevel/Detail if block
          } else {
            if ( log.isDetailed() ) {
              logDetailed( "entry not found: " + target.getAttributeKey() );
            }
          }
        } else {
          if ( log.isDetailed() ) {
            logDetailed( "No rows found for source step: " + source.getStepname() );
          }
        }
      }
    }

    // Inject the metadata into the step!
    //
    injectionInterface.injectStepMetadataEntries( inject );
  }

  private void copyResult( Trans trans ) {
    Result result = trans.getResult();
    setLinesInput( result.getNrLinesInput() );
    setLinesOutput( result.getNrLinesOutput() );
    setLinesRead( result.getNrLinesRead() );
    setLinesWritten( result.getNrLinesWritten() );
    setLinesUpdated( result.getNrLinesUpdated() );
    setLinesRejected( result.getNrLinesRejected() );
    setErrors( result.getNrErrors() );
  }

  private StepInjectionMetaEntry findDetailRootEntry( List<StepInjectionMetaEntry> metadataEntries,
      StepInjectionMetaEntry entry ) {
    for ( StepInjectionMetaEntry rowsEntry : metadataEntries ) {
      for ( StepInjectionMetaEntry rowEntry : rowsEntry.getDetails() ) {
        for ( StepInjectionMetaEntry detailEntry : rowEntry.getDetails() ) {
          if ( detailEntry.equals( entry ) ) {
            return rowsEntry;
          }
        }
      }
    }
    return null;
  }

  private SourceStepField findDetailSource( Map<TargetStepAttribute, SourceStepField> targetMap, String targetStep,
      String key ) {
    return targetMap.get( new TargetStepAttribute( targetStep, key, true ) );
  }

  private StepInjectionMetaEntry findMetaEntry( List<StepInjectionMetaEntry> metadataEntries, String attributeKey ) {
    for ( StepInjectionMetaEntry entry : metadataEntries ) {
      if ( entry.getKey().equals( attributeKey ) ) {
        return entry;
      }
      entry = findMetaEntry( entry.getDetails(), attributeKey );
      if ( entry != null ) {
        return entry;
      }
    }
    return null;
  }

  /**
   * package-local visibility for testing purposes
   */
  void setEntryValueIfFieldExists( StepInjectionMetaEntry entry, RowMetaAndData row, SourceStepField source )
      throws KettleValueException {
    RowMetaInterface rowMeta = row.getRowMeta();
    if ( rowMeta.indexOfValue( source.getField() ) < 0 ) {
      return;
    }
    setEntryValue( entry, row, source );
  }

  /**
   * package-local visibility for testing purposes
   */
  static void setEntryValue( StepInjectionMetaEntry entry, RowMetaAndData row, SourceStepField source )
    throws KettleValueException {
    // A standard attribute, a single row of data...
    //
    Object value = null;
    switch ( entry.getValueType() ) {
      case ValueMetaInterface.TYPE_STRING:
        value = row.getString( source.getField(), null );
        break;
      case ValueMetaInterface.TYPE_BOOLEAN:
        value = row.getBoolean( source.getField(), false );
        break;
      case ValueMetaInterface.TYPE_INTEGER:
        value = row.getInteger( source.getField(), 0L );
        break;
      case ValueMetaInterface.TYPE_NUMBER:
        value = row.getNumber( source.getField(), 0.0D );
        break;
      case ValueMetaInterface.TYPE_DATE:
        value = row.getDate( source.getField(), null );
        break;
      case ValueMetaInterface.TYPE_BIGNUMBER:
        value = row.getBigNumber( source.getField(), null );
        break;
      default:
        break;
    }
    entry.setValue( value );
  }

  public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
    meta = (MetaInjectMeta) smi;
    data = (MetaInjectData) sdi;

    if ( super.init( smi, sdi ) ) {
      try {
        data.transMeta = loadTransformationMeta();
        data.transMeta.copyVariablesFrom( this );
        data.transMeta.copyParametersFrom( this.getTransMeta() );

        checkSoureStepsAvailability();
        checkTargetStepsAvailability();
        // Get a mapping between the step name and the injection...
        //
        // Get new injection info
        data.stepInjectionMetasMap = new HashMap<String, StepMetaInterface>();
        for ( StepMeta stepMeta : data.transMeta.getUsedSteps() ) {
          StepMetaInterface meta = stepMeta.getStepMetaInterface();
          if ( BeanInjectionInfo.isInjectionSupported( meta.getClass() ) ) {
            data.stepInjectionMetasMap.put( stepMeta.getName(), meta );
          }
        }
        // Get old injection info
        data.stepInjectionMap = new HashMap<String, StepMetaInjectionInterface>();
        for ( StepMeta stepMeta : data.transMeta.getUsedSteps() ) {
          StepMetaInjectionInterface injectionInterface =
              stepMeta.getStepMetaInterface().getStepMetaInjectionInterface();
          if ( injectionInterface != null ) {
            data.stepInjectionMap.put( stepMeta.getName(), injectionInterface );
          }
        }

        // See if we need to stream data from a specific step into the template
        //
        if ( meta.getStreamSourceStep() != null && !Const.isEmpty( meta.getStreamTargetStepname() ) ) {
          data.streaming = true;
          data.streamingSourceStepname = meta.getStreamSourceStep().getName();
          data.streamingTargetStepname = meta.getStreamTargetStepname();
        }

        return true;
      } catch ( Exception e ) {
        logError( BaseMessages.getString( PKG, "MetaInject.BadEncoding.Message" ), e );
        return false;
      }
    }

    return false;
  }

  private void checkTargetStepsAvailability() {
    Set<String> existedStepNames = convertToUpperCaseSet( data.transMeta.getStepNames() );
    Map<TargetStepAttribute, SourceStepField> targetMap = meta.getTargetSourceMapping();
    Set<TargetStepAttribute> unavailableTargetSteps = getUnavailableTargetSteps( targetMap, data.transMeta );
    Set<String> alreadyMarkedSteps = new HashSet<String>();
    for ( TargetStepAttribute currentTarget : unavailableTargetSteps ) {
      if ( alreadyMarkedSteps.contains( currentTarget.getStepname() ) ) {
        continue;
      }
      alreadyMarkedSteps.add( currentTarget.getStepname() );
      if ( existedStepNames.contains( currentTarget.getStepname().toUpperCase() ) ) {
        logError( BaseMessages.getString( PKG, "MetaInject.TargetStepIsNotUsed.Message", currentTarget.getStepname(),
            data.transMeta.getName() ) );
      } else {
        logError( BaseMessages.getString( PKG, "MetaInject.TargetStepIsNotDefined.Message", currentTarget.getStepname(),
            data.transMeta.getName() ) );
      }
    }
    // alreadyMarked contains wrong steps. Spoon can report error if it will not fail transformation [BACKLOG-6753]
  }

  public static void removeUnavailableStepsFromMapping( Map<TargetStepAttribute, SourceStepField> targetMap,
      Set<SourceStepField> unavailableSourceSteps, Set<TargetStepAttribute> unavailableTargetSteps ) {
    Iterator<Entry<TargetStepAttribute, SourceStepField>> targetMapIterator = targetMap.entrySet().iterator();
    while ( targetMapIterator.hasNext() ) {
      Entry<TargetStepAttribute, SourceStepField> entry = targetMapIterator.next();
      SourceStepField currentSourceStepField = entry.getValue();
      TargetStepAttribute currentTargetStepAttribute = entry.getKey();
      if ( unavailableSourceSteps.contains( currentSourceStepField ) || unavailableTargetSteps.contains(
          currentTargetStepAttribute ) ) {
        targetMapIterator.remove();
      }
    }
  }

  public static Set<TargetStepAttribute> getUnavailableTargetSteps( Map<TargetStepAttribute, SourceStepField> targetMap,
      TransMeta injectedTransMeta ) {
    Set<String> usedStepNames = getUsedStepsForReferencendTransformation( injectedTransMeta );
    Set<TargetStepAttribute> unavailableTargetSteps = new HashSet<TargetStepAttribute>();
    for ( TargetStepAttribute currentTarget : targetMap.keySet() ) {
      if ( !usedStepNames.contains( currentTarget.getStepname().toUpperCase() ) ) {
        unavailableTargetSteps.add( currentTarget );
      }
    }
    return Collections.unmodifiableSet( unavailableTargetSteps );
  }

  private static Set<String> getUsedStepsForReferencendTransformation( TransMeta transMeta ) {
    Set<String> usedStepNames = new HashSet<String>();
    for ( StepMeta currentStep : transMeta.getUsedSteps() ) {
      usedStepNames.add( currentStep.getName().toUpperCase() );
    }
    return usedStepNames;
  }

  public static Set<SourceStepField> getUnavailableSourceSteps( Map<TargetStepAttribute, SourceStepField> targetMap,
      TransMeta sourceTransMeta, StepMeta stepMeta ) {
    String[] stepNamesArray = sourceTransMeta.getPrevStepNames( stepMeta );
    Set<String> existedStepNames = convertToUpperCaseSet( stepNamesArray );
    Set<SourceStepField> unavailableSourceSteps = new HashSet<SourceStepField>();
    for ( SourceStepField currentSource : targetMap.values() ) {
      if ( !existedStepNames.contains( currentSource.getStepname().toUpperCase() ) ) {
        unavailableSourceSteps.add( currentSource );
      }
    }
    return Collections.unmodifiableSet( unavailableSourceSteps );
  }

  private void checkSoureStepsAvailability() {
    Map<TargetStepAttribute, SourceStepField> targetMap = meta.getTargetSourceMapping();
    Set<SourceStepField> unavailableSourceSteps =
        getUnavailableSourceSteps( targetMap, getTransMeta(), getStepMeta() );
    Set<String> alreadyMarkedSteps = new HashSet<String>();
    for ( SourceStepField currentSource : unavailableSourceSteps ) {
      if ( alreadyMarkedSteps.contains( currentSource.getStepname() ) ) {
        continue;
      }
      alreadyMarkedSteps.add( currentSource.getStepname() );
      logError( BaseMessages.getString( PKG, "MetaInject.SourceStepIsNotAvailable.Message", currentSource.getStepname(),
          getTransMeta().getName() ) );
    }
    // alreadyMarked contains wrong steps. Spoon can report error if it will not fail transformation [BACKLOG-6753]
  }

  /**
   * package-local visibility for testing purposes
   */
  static Set<String> convertToUpperCaseSet( String[] array ) {
    if ( array == null ) {
      return Collections.emptySet();
    }
    Set<String> strings = new HashSet<String>();
    for ( String currentString : array ) {
      strings.add( currentString.toUpperCase() );
    }
    return strings;
  }

  /**
   * package-local visibility for testing purposes
   */
  TransMeta loadTransformationMeta() throws KettleException {
    return MetaInjectMeta.loadTransformationMeta( meta, getTrans().getRepository(), getTrans().getMetaStore(), this );
  }

}
