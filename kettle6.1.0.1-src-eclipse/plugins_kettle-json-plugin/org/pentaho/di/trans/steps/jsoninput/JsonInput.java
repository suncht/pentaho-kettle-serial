/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2016 by Pentaho : http://www.pentaho.com
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

package org.pentaho.di.trans.steps.jsoninput;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.poi.util.IOUtils;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.QueueRowSet;
import org.pentaho.di.core.ResultFile;
import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.fileinput.BaseFileInputStep;
import org.pentaho.di.trans.steps.fileinput.IBaseFileInputReader;
import org.pentaho.di.trans.steps.jsoninput.reader.FastJsonReader;
import org.pentaho.di.trans.steps.jsoninput.reader.InputsReader;
import org.pentaho.di.trans.steps.jsoninput.reader.RowOutputConverter;

/**
 * Read Json files, parse them and convert them to rows and writes these to one or more output streams.
 *
 * @author Samatar
 * @since 20-06-2010
 * @author edube
 * @author jadametz
 */
public class JsonInput extends BaseFileInputStep<JsonInputMeta, JsonInputData> implements StepInterface {
  private static Class<?> PKG = JsonInputMeta.class; // for i18n purposes, needed by Translator2!!

  private RowOutputConverter rowOutputConverter;

  public JsonInput( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans ) {
    super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
  }

  @Override
  protected boolean init() {
    data.rownr = 1L;
    data.nrInputFields = meta.getInputFields().length;
    data.repeatedFields = new BitSet( data.nrInputFields );
    // Take care of variable substitution
    for ( int i = 0; i < data.nrInputFields; i++ ) {
      JsonInputField field = meta.getInputFields()[i];
      field.setPath( environmentSubstitute( field.getPath() ) );
      if ( field.isRepeated() ) {
        data.repeatedFields.set( i );
      }
    }
    try {
      // Init a new JSON reader
      createReader();
    } catch ( KettleException e ) {
      logError( e.getMessage() );
      return false;
    }
    return true;
  }

  @Override
  public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
    if ( first ) {
      first = false;
      prepareToRowProcessing();
    }

    Object[] outRow = null;
    try {
      // Grab a row
      outRow = getOneOutputRow();
      if ( outRow == null ) {
        setOutputDone(); // signal end to receiver(s)
        return false; // end of data or error.
      }

      if ( log.isRowLevel() ) {
        logRowlevel( BaseMessages.getString( PKG, "JsonInput.Log.ReadRow", data.outputRowMeta.getString( outRow ) ) );
      }
      incrementLinesInput();
      data.rownr++;

      putRow( data.outputRowMeta, outRow ); // copy row to output rowset(s);

      if ( meta.getRowLimit() > 0 && data.rownr > meta.getRowLimit() ) {
        // limit has been reached: stop now.
        setOutputDone();
        return false;
      }

    } catch ( Exception e ) {
      if ( getStepMeta().isDoingErrorHandling() ) {
        sendErrorRow( e.toString() );
      } else {
        logError( BaseMessages.getString( PKG, "JsonInput.ErrorInStepRunning", e.getMessage() ) );
        setErrors( getErrors() + 1 );
        stopAll();
        setOutputDone(); // signal end to receiver(s)
        return false;
      }
    }
    return true;
  }

  @Override
  protected void prepareToRowProcessing() throws KettleException, KettleStepException, KettleValueException {
    if ( !meta.isInFields() ) {
      data.outputRowMeta = new RowMeta();
      if ( !meta.isDoNotFailIfNoFile() && data.files.nrOfFiles() == 0 ) {
        String errMsg = BaseMessages.getString( PKG, "JsonInput.Log.NoFiles" );
        logError( errMsg );
        inputError( errMsg );
      }
    } else {
      data.readrow = getRow();
      data.inputRowMeta = getInputRowMeta();
      data.outputRowMeta = data.inputRowMeta.clone();

      // Check if source field is provided
      if ( Const.isEmpty( meta.getFieldValue() ) ) {
        logError( BaseMessages.getString( PKG, "JsonInput.Log.NoField" ) );
        throw new KettleException( BaseMessages.getString( PKG, "JsonInput.Log.NoField" ) );
      }

      // cache the position of the field
      if ( data.indexSourceField < 0 ) {
        data.indexSourceField = getInputRowMeta().indexOfValue( meta.getFieldValue() );
        if ( data.indexSourceField < 0 ) {
          logError( BaseMessages.getString( PKG, "JsonInput.Log.ErrorFindingField", meta.getFieldValue() ) );
          throw new KettleException( BaseMessages.getString( PKG, "JsonInput.Exception.CouldnotFindField",
              meta.getFieldValue() ) );
        }
      }

      // if RemoveSourceField option is set, we remove the source field from the output meta
      if ( meta.isRemoveSourceField() ) {
        data.outputRowMeta.removeValueMeta( data.indexSourceField );
        // Get total previous fields minus one since we remove source field
        data.totalpreviousfields = data.inputRowMeta.size() - 1;
      } else {
        // Get total previous fields
        data.totalpreviousfields = data.inputRowMeta.size();
      }
    }
    meta.getFields( data.outputRowMeta, getStepname(), null, null, this, repository, metaStore );

    // Create convert meta-data objects that will contain Date & Number formatters
    data.convertRowMeta = data.outputRowMeta.cloneToType( ValueMetaInterface.TYPE_STRING );
    data.inputs = new InputsReader( this, meta, data, new InputErrorHandler() ).iterator();
    // data.recordnr = 0;
    data.readerRowSet = new QueueRowSet();
    data.readerRowSet.setDone();
    this.rowOutputConverter = new RowOutputConverter( getLogChannel() );
  }

  private void addFileToResultFilesname( FileObject file ) {
    if ( meta.addResultFile() ) {
      // Add this to the result file names...
      ResultFile resultFile =
          new ResultFile( ResultFile.FILE_TYPE_GENERAL, file, getTransMeta().getName(), getStepname() );
      resultFile.setComment( BaseMessages.getString( PKG, "JsonInput.Log.FileAddedResult" ) );
      addResultFile( resultFile );
    }
  }

  public boolean onNewFile( FileObject file ) throws FileSystemException {
    if ( file == null ) {
      String errMsg = BaseMessages.getString( PKG, "JsonInput.Log.IsNotAFile", "null" );
      logError( errMsg );
      inputError( errMsg );
      return false;
    } else if ( !file.exists() ) {
      String errMsg = BaseMessages.getString( PKG, "JsonInput.Log.IsNotAFile", file.getName().getFriendlyURI() );
      logError( errMsg );
      inputError( errMsg );
      return false;
    }
    if ( hasAdditionalFileFields() ) {
      fillFileAdditionalFields( data, file );
    }
    if ( file.getContent().getSize() == 0 ) {
      // log only basic as a warning (was before logError)
      if ( meta.isIgnoreEmptyFile() ) {
        logBasic( BaseMessages.getString( PKG, "JsonInput.Error.FileSizeZero", "" + file.getName() ) );
      } else {
        logError( BaseMessages.getString( PKG, "JsonInput.Error.FileSizeZero", "" + file.getName() ) );
        incrementErrors();
        return false;
      }
    }
    return true;
  }

  @Override
  protected void fillFileAdditionalFields( JsonInputData data, FileObject file ) throws FileSystemException {
    super.fillFileAdditionalFields( data, file );
    data.filename = KettleVFS.getFilename( file );
    data.filenr++;
    if ( log.isDetailed() ) {
      logDetailed( BaseMessages.getString( PKG, "JsonInput.Log.OpeningFile", file.toString() ) );
    }
    addFileToResultFilesname( file );
  }

  private boolean parseNextInputToRowSet( InputStream input ) throws KettleException {
    if ( input != null ) {
      try {
        data.readerRowSet = data.reader.parse( input );
        return true;
      } catch ( KettleException ke ) {
        logInputError( ke );
      } catch ( Exception e ) {
        logInputError( e );
      }
    }
    return false;
  }

  private void logInputError( KettleException e ) {
    logError( e.getLocalizedMessage(), e );
    inputError( e.getLocalizedMessage() );
  }

  private void logInputError( Exception e ) {
    String errMsg = ( !meta.isInFields() || meta.getIsAFile() )
        ? BaseMessages.getString( PKG, "JsonReader.Error.ParsingFile", data.filename )
        : BaseMessages.getString( PKG, "JsonReader.Error.ParsingString", data.readrow[ data.indexSourceField ] );
    logError( errMsg, e );
    inputError( errMsg );
  }

  private void incrementErrors() {
    setErrors( getErrors() + 1 );
  }

  private void inputError( String errorMsg ) {
    if ( getStepMeta().isDoingErrorHandling() ) {
      sendErrorRow( errorMsg );
    } else {
      incrementErrors();
    }
  }

  private class InputErrorHandler implements InputsReader.ErrorHandler {

    @Override
    public void error( Exception e ) {
      logError( BaseMessages.getString( PKG, "JsonInput.Log.UnexpectedError", e.toString() ) );
      setErrors( getErrors() + 1 );
    }

    @Override
    public void fileOpenError( FileObject file, FileSystemException e ) {
      String msg = BaseMessages.getString(
          PKG, "JsonInput.Log.UnableToOpenFile", "" + data.filenr, file.toString(), e.toString() );
      logError( msg );
      inputError( msg );
    }

    @Override
    public void fileCloseError(  FileObject file, FileSystemException e ) {
      error( e );
    }
  }

  /**
   * get final row for output
   */
  private Object[] getOneOutputRow() throws KettleException {
    Object[] rawReaderRow = null;
    while ( ( rawReaderRow = data.readerRowSet.getRow() ) == null ) {
      if ( data.inputs.hasNext() && data.readerRowSet.isDone() ) {
        try ( InputStream nextIn = data.inputs.next() ) {
          boolean parsed = parseNextInputToRowSet( nextIn );
          if ( parsed && shouldOutputEmpty() ) {
            return buildBaseOutputRow();
          }
        } catch ( IOException e ) {
          logError( BaseMessages.getString( PKG, "JsonInput.Log.UnexpectedError", e.toString() ), e );
          incrementErrors();
        }
      } else {
        if ( isDetailed() ) {
          logDetailed( BaseMessages.getString( PKG, "JsonInput.Log.FinishedProcessing" ) );
        }
        return null;
      }
    }
    Object[] outputRow = rowOutputConverter.getRow( buildBaseOutputRow(), rawReaderRow, data );
    addExtraFields( outputRow, data );
    return outputRow;
  }

  private void sendErrorRow( String errorMsg ) {
    try {
      // same error as before
      String defaultErrCode = "JsonInput001";
      if ( data.readrow != null ) {
        putError( getInputRowMeta(), data.readrow, 1, errorMsg, meta.getFieldValue(), defaultErrCode );
      } else {
        // when no input only error fields are recognized
        putError( new RowMeta(), new Object[0], 1, errorMsg, null, defaultErrCode );
      }
    } catch ( KettleStepException e ) {
      logError( e.getLocalizedMessage(), e );
    }
  }

  private boolean shouldOutputEmpty() {
    return meta.isInFields() && isEmpty( data.readerRowSet );
  }

  private boolean hasAdditionalFileFields() {
    return data.file != null;
  }

  private boolean isEmpty( RowSet readerRowSet ) {
    return readerRowSet.size() == 0 && readerRowSet.isDone();
  }

  /**
   * allocates out row
   */
  private Object[] buildBaseOutputRow() {
    Object[] outputRowData;
    if ( data.readrow != null ) {
      if ( meta.isRemoveSourceField() && data.indexSourceField > -1 ) {
        // skip the source field in the output array
        int sz = data.readrow.length;
        outputRowData = RowDataUtil.allocateRowData( data.outputRowMeta.size() );
        int ii = 0;
        for ( int i = 0; i < sz; i++ ) {
          if ( i != data.indexSourceField ) {
            outputRowData[ii++] = data.readrow[i];
          }
        }
      } else {
        outputRowData = RowDataUtil.createResizedCopy( data.readrow, data.outputRowMeta.size() );
      }
    } else {
      outputRowData = RowDataUtil.allocateRowData( data.outputRowMeta.size() );
    }
    return outputRowData;
  }

  // should be refactored
  private void addExtraFields( Object[] outputRowData, JsonInputData data ) {
    int rowIndex = data.totalpreviousfields + data.nrInputFields;

    // See if we need to add the filename to the row...
    if ( meta.includeFilename() && !Const.isEmpty( meta.getFilenameField() ) ) {
      outputRowData[rowIndex++] = data.filename;
    }
    // See if we need to add the row number to the row...
    if ( meta.includeRowNumber() && !Const.isEmpty( meta.getRowNumberField() ) ) {
      outputRowData[rowIndex++] = new Long( data.rownr );
    }
    // Possibly add short filename...
    if ( meta.getShortFileNameField() != null && meta.getShortFileNameField().length() > 0 ) {
      outputRowData[rowIndex++] = data.shortFilename;
    }
    // Add Extension
    if ( meta.getExtensionField() != null && meta.getExtensionField().length() > 0 ) {
      outputRowData[rowIndex++] = data.extension;
    }
    // add path
    if ( meta.getPathField() != null && meta.getPathField().length() > 0 ) {
      outputRowData[rowIndex++] = data.path;
    }
    // Add Size
    if ( meta.getSizeField() != null && meta.getSizeField().length() > 0 ) {
      outputRowData[rowIndex++] = new Long( data.size );
    }
    // add Hidden
    if ( meta.isHiddenField() != null && meta.isHiddenField().length() > 0 ) {
      outputRowData[rowIndex++] = new Boolean( data.path );
    }
    // Add modification date
    if ( meta.getLastModificationDateField() != null && meta.getLastModificationDateField().length() > 0 ) {
      outputRowData[rowIndex++] = data.lastModificationDateTime;
    }
    // Add Uri
    if ( meta.getUriField() != null && meta.getUriField().length() > 0 ) {
      outputRowData[rowIndex++] = data.uriName;
    }
    // Add RootUri
    if ( meta.getRootUriField() != null && meta.getRootUriField().length() > 0 ) {
      outputRowData[rowIndex++] = data.rootUriName;
    }
  }

  private void createReader() throws KettleException {
    data.reader = new FastJsonReader( meta.getInputFields(), meta.isDefaultPathLeafToNull(), log );
    data.reader.setIgnoreMissingPath( meta.isIgnoreMissingPath() );
  }

  public void dispose( StepMetaInterface smi, StepDataInterface sdi ) {
    meta = (JsonInputMeta) smi;
    data = (JsonInputData) sdi;
    if ( data.file != null ) {
      IOUtils.closeQuietly( data.file );
    }
    data.inputs = null;
    data.reader = null;
    data.readerRowSet = null;
    data.repeatedFields = null;
    super.dispose( smi, sdi );
  }

  /**
   * Only to comply with super, does nothing good.
   * @throws NotImplementedException everytime
   */
  @Override
  protected IBaseFileInputReader createReader( JsonInputMeta meta, JsonInputData data, FileObject file )
    throws Exception {
    throw new NotImplementedException();
  }

}
