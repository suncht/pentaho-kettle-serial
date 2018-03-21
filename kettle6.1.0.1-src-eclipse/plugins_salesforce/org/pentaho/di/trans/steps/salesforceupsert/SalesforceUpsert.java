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

package org.pentaho.di.trans.steps.salesforceupsert;

import java.util.ArrayList;
import org.apache.axis.message.MessageElement;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.salesforceinput.SalesforceConnection;
import org.pentaho.di.trans.steps.salesforceutils.SalesforceUtils;

import com.google.common.annotations.VisibleForTesting;
import com.sforce.soap.partner.sobject.SObject;

/**
 * Read data from Salesforce module, convert them to rows and writes these to one or more output streams.
 *
 * @author jstairs,Samatar
 * @since 10-06-2007
 */
public class SalesforceUpsert extends BaseStep implements StepInterface {
  private static Class<?> PKG = SalesforceUpsertMeta.class; // for i18n purposes, needed by Translator2!!

  private SalesforceUpsertMeta meta;
  private SalesforceUpsertData data;

  public SalesforceUpsert( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr,
    TransMeta transMeta, Trans trans ) {
    super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
  }

  public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {

    // get one row ... This does some basic initialization of the objects, including loading the info coming in
    Object[] outputRowData = getRow();

    if ( outputRowData == null ) {
      if ( data.iBufferPos > 0 ) {
        flushBuffers();
      }
      setOutputDone();
      return false;
    }

    // If we haven't looked at a row before then do some basic setup.
    if ( first ) {
      first = false;

      data.sfBuffer = new SObject[meta.getBatchSizeInt()];
      data.outputBuffer = new Object[meta.getBatchSizeInt()][];

      // get total fields in the grid
      data.nrfields = meta.getUpdateLookup().length;

      // Check if field list is filled
      if ( data.nrfields == 0 ) {
        throw new KettleException( BaseMessages.getString(
          PKG, "SalesforceUpsertDialog.FieldsMissing.DialogMessage" ) );
      }

      // Create the output row meta-data
      data.inputRowMeta = getInputRowMeta().clone();
      data.outputRowMeta = data.inputRowMeta.clone();
      meta.getFields( data.outputRowMeta, getStepname(), null, null, this, repository, metaStore );

      // Build the mapping of input position to field name
      data.fieldnrs = new int[meta.getUpdateStream().length];
      for ( int i = 0; i < meta.getUpdateStream().length; i++ ) {
        data.fieldnrs[i] = getInputRowMeta().indexOfValue( meta.getUpdateStream()[i] );
        if ( data.fieldnrs[i] < 0 ) {
          throw new KettleException( BaseMessages.getString( PKG, "SalesforceUpsert.FieldNotFound", meta
            .getUpdateStream()[i] ) );
        }
      }
    }

    try {
      writeToSalesForce( outputRowData );
    } catch ( Exception e ) {
      throw new KettleStepException( BaseMessages.getString( PKG, "SalesforceUpsert.log.Exception" ), e );
    }
    return true;
  }

  @VisibleForTesting
  void writeToSalesForce( Object[] rowData ) throws KettleException {
    try {

      if ( log.isDetailed() ) {
        logDetailed( BaseMessages.getString( PKG, "SalesforceUpsert.CalledWrite", data.iBufferPos, meta
          .getBatchSizeInt() ) );
      }
      // if there is room in the buffer
      if ( data.iBufferPos < meta.getBatchSizeInt() ) {
        // Reserve for empty fields
        ArrayList<String> fieldsToNull = new ArrayList<String>();
        ArrayList<MessageElement> upsertfields = new ArrayList<MessageElement>();

        // Add fields to update
        for ( int i = 0; i < data.nrfields; i++ ) {
          ValueMetaInterface valueMeta = data.inputRowMeta.getValueMeta( data.fieldnrs[i] );
          Object object = rowData[data.fieldnrs[i]];

          if ( valueMeta.isNull( object ) ) {
            // The value is null
            // We need to keep track of this field
            fieldsToNull.add( SalesforceUtils.getFieldToNullName( log, meta.getUpdateLookup()[i], meta
                .getUseExternalId()[i] ) );
          } else {
            Object normalObject = valueMeta.convertToNormalStorageType( object );
            upsertfields.add( SalesforceConnection.createMessageElement( meta.getUpdateLookup()[i], normalObject, meta
                .getUseExternalId()[i] ) );
          }
        }

        // build the SObject
        SObject sobjPass = new SObject();
        sobjPass.setType( data.realModule );
        if ( upsertfields.size() > 0 ) {
          sobjPass.set_any( upsertfields.toArray( new MessageElement[upsertfields.size()] ) );
        }
        if ( fieldsToNull.size() > 0 ) {
          // Set Null to fields
          sobjPass.setFieldsToNull( fieldsToNull.toArray( new String[fieldsToNull.size()] ) );
        }
        // Load the buffer array
        data.sfBuffer[data.iBufferPos] = sobjPass;
        data.outputBuffer[data.iBufferPos] = rowData;
        data.iBufferPos++;
      }

      if ( data.iBufferPos >= meta.getBatchSizeInt() ) {
        if ( log.isDetailed() ) {
          logDetailed( "Calling flush buffer from writeToSalesForce" );
        }
        flushBuffers();
      }
    } catch ( KettleException ke ) {
      throw ke;
    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString( PKG, "SalesforceUpsert.FailedInWrite", e.toString() ) );
    }
  }

  private void flushBuffers() throws KettleException {

    try {
      // create the object(s) by sending the array to the web service
      data.upsertResult = data.connection.upsert( meta.getUpsertField(), data.sfBuffer );
      int nr = data.upsertResult.length;
      for ( int j = 0; j < nr; j++ ) {
        if ( data.upsertResult[j].isSuccess() ) {
          String id = data.upsertResult[j].getId();
          if ( data.upsertResult[j].isCreated() ) {
            incrementLinesOutput();
            if ( log.isDetailed() ) {
              logDetailed( BaseMessages.getString( PKG, "SalesforceUpsert.ObjectCreated", id ) );
            }
          } else {
            incrementLinesUpdated();
            if ( log.isDetailed() ) {
              logDetailed( BaseMessages.getString( PKG, "SalesforceUpsert.ObjectUpdated", id ) );
            }
          }
          // write out the row with the SalesForce ID
          Object[] newRow = RowDataUtil.resizeArray( data.outputBuffer[j], data.outputRowMeta.size() );

          if ( data.realSalesforceFieldName != null ) {
            int newIndex = data.inputRowMeta.size();
            newRow[newIndex++] = id;
          }
          if ( log.isDetailed() ) {
            logDetailed( BaseMessages.getString( PKG, "SalesforceUpsert.NewRow", newRow[0] ) );
          }

          putRow( data.outputRowMeta, newRow ); // copy row to output rowset(s);

          if ( checkFeedback( getLinesInput() ) ) {
            if ( log.isDetailed() ) {
              logDetailed( BaseMessages.getString( PKG, "SalesforceUpsert.log.LineRow", "" + getLinesInput() ) );
            }
          }

        } else {
          // there were errors during the create call, go through the
          // errors
          // array and write them to the screen

          if ( !getStepMeta().isDoingErrorHandling() ) {
            if ( log.isDetailed() ) {
              logDetailed( BaseMessages.getString( PKG, "SalesforceUpsert.ErrorFound" ) );
            }

            // Only throw the first error
            //
            com.sforce.soap.partner.Error err = data.upsertResult[j].getErrors()[0];
            throw new KettleException( BaseMessages
              .getString( PKG, "SalesforceUpsert.Error.FlushBuffer", new Integer( j ), err.getStatusCode(), err
                .getMessage() ) );
          }

          String errorMessage = "";
          for ( int i = 0; i < data.upsertResult[j].getErrors().length; i++ ) {
            // get the next error
            com.sforce.soap.partner.Error err = data.upsertResult[j].getErrors()[i];
            errorMessage +=
              BaseMessages.getString( PKG, "SalesforceUpsert.Error.FlushBuffer", new Integer( j ), err
                .getStatusCode(), err.getMessage() );
          }

          // Simply add this row to the error row
          if ( log.isDebug() ) {
            logDebug( BaseMessages.getString( PKG, "SalesforceUpsert.PassingRowToErrorStep" ) );
          }
          putError( getInputRowMeta(), data.outputBuffer[j], 1, errorMessage, null, "SalesforceUpsert001" );
        }

      }

      // reset the buffers
      data.sfBuffer = new SObject[meta.getBatchSizeInt()];
      data.outputBuffer = new Object[meta.getBatchSizeInt()][];
      data.iBufferPos = 0;

    } catch ( Exception e ) {
      if ( !getStepMeta().isDoingErrorHandling() ) {
        if ( e instanceof KettleException ) {
          // I know, bad form usually. But I didn't want to duplicate the logic with a catch(KettleException). MB
          throw (KettleException) e;
        } else {
          throw new KettleException(
            BaseMessages.getString( PKG, "SalesforceUpsert.FailedUpsert", e.getMessage() ), e );
        }
      }
      // Simply add this row to the error row
      if ( log.isDebug() ) {
        logDebug( "Passing row to error step" );
      }

      for ( int i = 0; i < data.iBufferPos; i++ ) {
        putError( data.inputRowMeta, data.outputBuffer[i], 1, e.getMessage(), null, "SalesforceUpsert002" );
      }
    } finally {
      if ( data.upsertResult != null ) {
        data.upsertResult = null;
      }
    }

  }

  public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
    meta = (SalesforceUpsertMeta) smi;
    data = (SalesforceUpsertData) sdi;

    if ( super.init( smi, sdi ) ) {

      try {
        data.realModule = environmentSubstitute( meta.getModule() );
        // Check if module is specified
        if ( Const.isEmpty( data.realModule ) ) {
          log.logError( BaseMessages.getString( PKG, "SalesforceUpsertDialog.ModuleMissing.DialogMessage" ) );
          return false;
        }

        String realUser = environmentSubstitute( meta.getUserName() );
        // Check if username is specified
        if ( Const.isEmpty( realUser ) ) {
          log.logError( BaseMessages.getString( PKG, "SalesforceUpsertDialog.UsernameMissing.DialogMessage" ) );
          return false;
        }

        String salesfoceIdFieldname = environmentSubstitute( meta.getSalesforceIDFieldName() );
        if ( !Const.isEmpty( salesfoceIdFieldname ) ) {
          data.realSalesforceFieldName = salesfoceIdFieldname;
        }

        // initialize variables
        data.realURL = environmentSubstitute( meta.getTargetURL() );
        // create a Salesforce connection
        data.connection =
          new SalesforceConnection( log, data.realURL, realUser, environmentSubstitute( meta.getPassword() ) );
        // set timeout
        data.connection.setTimeOut( Const.toInt( environmentSubstitute( meta.getTimeOut() ), 0 ) );
        // Do we use compression?
        data.connection.setUsingCompression( meta.isUsingCompression() );
        // Do we need to rollback all changes on error
        data.connection.rollbackAllChangesOnError( meta.isRollbackAllChangesOnError() );
        // Now connect ...
        data.connection.connect();
        return true;
      } catch ( KettleException ke ) {
        logError( BaseMessages.getString( PKG, "SalesforceUpsert.Log.ErrorOccurredDuringStepInitialize" )
          + ke.getMessage() );
      }
      return true;
    }
    return false;
  }

  public void dispose( StepMetaInterface smi, StepDataInterface sdi ) {
    meta = (SalesforceUpsertMeta) smi;
    data = (SalesforceUpsertData) sdi;
    try {
      if ( data.outputBuffer != null ) {
        data.outputBuffer = null;
      }
      if ( data.sfBuffer != null ) {
        data.sfBuffer = null;
      }
      if ( data.connection != null ) {
        data.connection.close();
      }
    } catch ( Exception e ) { /* Ignore */
    }
    super.dispose( smi, sdi );
  }

}
