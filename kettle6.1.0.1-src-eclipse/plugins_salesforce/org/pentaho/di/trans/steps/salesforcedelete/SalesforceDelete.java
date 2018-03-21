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

package org.pentaho.di.trans.steps.salesforcedelete;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.salesforceinput.SalesforceConnection;

/**
 * Read data from Salesforce module, convert them to rows and writes these to one or more output streams.
 *
 * @author jstairs,Samatar
 * @since 10-06-2007
 */
public class SalesforceDelete extends BaseStep implements StepInterface {
  private static Class<?> PKG = SalesforceDeleteMeta.class; // for i18n purposes, needed by Translator2!!

  private SalesforceDeleteMeta meta;
  private SalesforceDeleteData data;

  public SalesforceDelete( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr,
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

      data.deleteId = new String[meta.getBatchSizeInt()];
      data.outputBuffer = new Object[meta.getBatchSizeInt()][];

      // Create the output row meta-data
      data.outputRowMeta = getInputRowMeta().clone();
      meta.getFields( data.outputRowMeta, getStepname(), null, null, this, repository, metaStore );

      // Check deleteKeyField
      String realFieldName = environmentSubstitute( meta.getDeleteField() );
      if ( Const.isEmpty( realFieldName ) ) {
        throw new KettleException( BaseMessages.getString( PKG, "SalesforceDelete.Error.DeleteKeyFieldMissing" ) );
      }

      // return the index of the field in the input stream
      data.indexOfKeyField = getInputRowMeta().indexOfValue( realFieldName );
      if ( data.indexOfKeyField < 0 ) {
        // the field is unreachable!
        throw new KettleException( BaseMessages.getString(
          PKG, "SalesforceDelete.Error.CanNotFindFDeleteKeyField", realFieldName ) );
      }
    }

    try {
      writeToSalesForce( outputRowData );
    } catch ( Exception e ) {
      throw new KettleStepException( BaseMessages.getString( PKG, "SalesforceDelete.log.Exception" ), e );
    }
    return true;
  }

  private void writeToSalesForce( Object[] rowData ) throws KettleException {
    try {

      if ( log.isDetailed() ) {
        logDetailed( BaseMessages.getString( PKG, "SalesforceDelete.Log.WriteToSalesforce", data.iBufferPos, meta
          .getBatchSizeInt() ) );
      }

      // if there is room in the buffer
      if ( data.iBufferPos < meta.getBatchSizeInt() ) {

        // Load the buffer array
        data.deleteId[data.iBufferPos] = getInputRowMeta().getString( rowData, data.indexOfKeyField );
        data.outputBuffer[data.iBufferPos] = rowData;
        data.iBufferPos++;
      }

      if ( data.iBufferPos >= meta.getBatchSizeInt() ) {
        if ( log.isDetailed() ) {
          logDetailed( BaseMessages.getString( PKG, "SalesforceDelete.Log.CallingFlush" ) );
        }
        flushBuffers();
      }
    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString( PKG, "SalesforceDelete.Error.WriteToSalesforce", e
        .getMessage() ) );
    }
  }

  private void flushBuffers() throws KettleException {

    try {
      // create the object(s) by sending the array to the web service
      data.deleteResult = data.connection.delete( data.deleteId );
      int nr = data.deleteResult.length;
      for ( int j = 0; j < nr; j++ ) {
        if ( data.deleteResult[j].isSuccess() ) {

          putRow( data.outputRowMeta, data.outputBuffer[j] ); // copy row to output rowset(s);
          incrementLinesOutput();

          if ( checkFeedback( getLinesInput() ) ) {
            if ( log.isDetailed() ) {
              logDetailed( BaseMessages.getString( PKG, "SalesforceDelete.log.LineRow", String
                .valueOf( getLinesInput() ) ) );
            }
          }

        } else {
          // there were errors during the create call, go through the
          // errors
          // array and write them to the screen

          if ( !getStepMeta().isDoingErrorHandling() ) {
            if ( log.isDetailed() ) {
              logDetailed( BaseMessages.getString( PKG, "SalesforceDelete.Found.Error" ) );
            }

            com.sforce.soap.partner.Error err = data.deleteResult[j].getErrors()[0];
            throw new KettleException( BaseMessages
              .getString( PKG, "SalesforceDelete.Error.FlushBuffer", new Integer( j ), err.getStatusCode(), err
                .getMessage() ) );

          }
          String errorMessage = "";
          int nrErrors = data.deleteResult[j].getErrors().length;
          for ( int i = 0; i < nrErrors; i++ ) {
            // get the next error
            com.sforce.soap.partner.Error err = data.deleteResult[j].getErrors()[i];
            errorMessage +=
              BaseMessages.getString( PKG, "SalesforceDelete.Error.FlushBuffer", new Integer( j ), err
                .getStatusCode(), err.getMessage() );
          }
          // Simply add this row to the error row
          if ( log.isDebug() ) {
            logDebug( BaseMessages.getString( PKG, "SalesforceDelete.PassingRowToErrorStep" ) );
          }
          putError( getInputRowMeta(), data.outputBuffer[j], 1, errorMessage, null, "SalesforceDelete001" );
        }

      }

      // reset the buffers
      data.deleteId = new String[meta.getBatchSizeInt()];
      data.outputBuffer = new Object[meta.getBatchSizeInt()][];
      data.iBufferPos = 0;

    } catch ( Exception e ) {
      if ( !getStepMeta().isDoingErrorHandling() ) {
        throw new KettleException( BaseMessages
          .getString( PKG, "SalesforceDelete.FailedToDeleted", e.getMessage() ) );
      }
      // Simply add this row to the error row
      if ( log.isDebug() ) {
        logDebug( "Passing row to error step" );
      }

      for ( int i = 0; i < data.iBufferPos; i++ ) {
        putError( data.inputRowMeta, data.outputBuffer[i], 1, e.getMessage(), null, "SalesforceDelete002" );
      }
    } finally {
      if ( data.deleteResult != null ) {
        data.deleteResult = null;
      }
    }

  }

  public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
    meta = (SalesforceDeleteMeta) smi;
    data = (SalesforceDeleteData) sdi;

    if ( super.init( smi, sdi ) ) {

      try {
        data.realModule = environmentSubstitute( meta.getModule() );
        // Check if module is specified
        if ( Const.isEmpty( data.realModule ) ) {
          log.logError( BaseMessages.getString( PKG, "SalesforceDeleteDialog.ModuleMissing.DialogMessage" ) );
          return false;
        }

        String realUser = environmentSubstitute( meta.getUserName() );
        // Check if username is specified
        if ( Const.isEmpty( realUser ) ) {
          log.logError( BaseMessages.getString( PKG, "SalesforceDeleteDialog.UsernameMissing.DialogMessage" ) );
          return false;
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
        // Do we rollback all changes on error
        data.connection.rollbackAllChangesOnError( meta.isRollbackAllChangesOnError() );

        // Now connect ...
        data.connection.connect();

        return true;
      } catch ( KettleException ke ) {
        logError( BaseMessages.getString( PKG, "SalesforceDelete.Log.ErrorOccurredDuringStepInitialize" )
          + ke.getMessage() );
      }
      return true;
    }
    return false;
  }

  public void dispose( StepMetaInterface smi, StepDataInterface sdi ) {
    meta = (SalesforceDeleteMeta) smi;
    data = (SalesforceDeleteData) sdi;
    try {
      if ( data.outputBuffer != null ) {
        data.outputBuffer = null;
      }
      if ( data.deleteId != null ) {
        data.deleteId = null;
      }
      if ( data.connection != null ) {
        data.connection.close();
      }
    } catch ( Exception e ) { /* Ignore */
    }
    super.dispose( smi, sdi );
  }

}
