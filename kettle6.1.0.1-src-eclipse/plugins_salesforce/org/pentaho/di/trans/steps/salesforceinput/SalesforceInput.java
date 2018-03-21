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

package org.pentaho.di.trans.steps.salesforceinput;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Read data from Salesforce module, convert them to rows and writes these to one or more output streams.
 *
 * @author Samatar
 * @since 10-06-2007
 */
public class SalesforceInput extends BaseStep implements StepInterface {
  private static Class<?> PKG = SalesforceInputMeta.class; // for i18n purposes, needed by Translator2!!

  private SalesforceInputMeta meta;
  private SalesforceInputData data;

  public SalesforceInput( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
    Trans trans ) {
    super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
  }

  public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
    if ( first ) {
      first = false;

      // Create the output row meta-data
      data.outputRowMeta = new RowMeta();

      meta.getFields( data.outputRowMeta, getStepname(), null, null, this, repository, metaStore );

      // For String to <type> conversions, we allocate a conversion meta data row as well...
      //
      data.convertRowMeta = data.outputRowMeta.cloneToType( ValueMetaInterface.TYPE_STRING );

      // Let's query Salesforce
      data.connection.query( meta.isSpecifyQuery() );

      data.limitReached = true;
      data.recordcount = data.connection.getQueryResultSize();
      if ( data.recordcount > 0 ) {
        data.limitReached = false;
        data.nrRecords = data.connection.getRecordsCount();
      }
      if ( log.isDetailed() ) {
        logDetailed( BaseMessages.getString( PKG, "SalesforceInput.Log.RecordCount" ) + " : " + data.recordcount );
      }

    }

    Object[] outputRowData = null;

    try {
      // get one row ...
      outputRowData = getOneRow();

      if ( outputRowData == null ) {
        setOutputDone();
        return false;
      }

      putRow( data.outputRowMeta, outputRowData ); // copy row to output rowset(s);

      if ( checkFeedback( getLinesInput() ) ) {
        if ( log.isDetailed() ) {
          logDetailed( BaseMessages.getString( PKG, "SalesforceInput.log.LineRow", "" + getLinesInput() ) );
        }
      }

      data.rownr++;
      data.recordIndex++;

      return true;
    } catch ( KettleException e ) {
      boolean sendToErrorRow = false;
      String errorMessage = null;
      if ( getStepMeta().isDoingErrorHandling() ) {
        sendToErrorRow = true;
        errorMessage = e.toString();
      } else {
        logError( BaseMessages.getString( PKG, "SalesforceInput.log.Exception", e.getMessage() ) );
        logError( Const.getStackTracker( e ) );
        setErrors( 1 );
        stopAll();
        setOutputDone(); // signal end to receiver(s)
        return false;
      }
      if ( sendToErrorRow ) {
        // Simply add this row to the error row
        putError( getInputRowMeta(), outputRowData, 1, errorMessage, null, "SalesforceInput001" );
      }
    }
    return true;
  }

  private Object[] getOneRow() throws KettleException {
    if ( data.limitReached || data.rownr >= data.recordcount ) {
      return null;
    }

    // Build an empty row based on the meta-data
    Object[] outputRowData = buildEmptyRow();

    try {

      // check for limit rows
      if ( data.limit > 0 && data.rownr >= data.limit ) {
        // User specified limit and we reached it
        // We end here
        data.limitReached = true;
        return null;
      } else {
        if ( data.rownr >= data.nrRecords || data.finishedRecord ) {
          if ( meta.getRecordsFilter() != SalesforceConnectionUtils.RECORDS_FILTER_UPDATED ) {
            // We retrieved all records available here
            // maybe we need to query more again ...
            if ( log.isDetailed() ) {
              logDetailed( BaseMessages.getString( PKG, "SalesforceInput.Log.NeedQueryMore", "" + data.rownr ) );
            }

            if ( data.connection.queryMore() ) {
              // We returned more result (query is not done yet)
              int nr = data.connection.getRecordsCount();
              data.nrRecords += nr;
              if ( log.isDetailed() ) {
                logDetailed( BaseMessages.getString( PKG, "SalesforceInput.Log.QueryMoreRetrieved", "" + nr ) );
              }

              // We need here to initialize recordIndex
              data.recordIndex = 0;

              data.finishedRecord = false;
            } else {
              // Query is done .. we finished !
              return null;
            }
          }
        }
      }

      // Return a record
      SalesforceRecordValue srvalue = data.connection.getRecord( data.recordIndex );
      data.finishedRecord = srvalue.isAllRecordsProcessed();

      if ( meta.getRecordsFilter() == SalesforceConnectionUtils.RECORDS_FILTER_DELETED ) {
        if ( srvalue.isRecordIndexChanges() ) {
          // We have moved forward...
          data.recordIndex = srvalue.getRecordIndex();
        }
        if ( data.finishedRecord && srvalue.getRecordValue() == null ) {
          // We processed all records
          return null;
        }
      }
      for ( int i = 0; i < data.nrfields; i++ ) {
        String value =
          data.connection.getRecordValue( srvalue.getRecordValue(), meta.getInputFields()[i].getField() );

        // DO Trimming!
        switch ( meta.getInputFields()[i].getTrimType() ) {
          case SalesforceInputField.TYPE_TRIM_LEFT:
            value = Const.ltrim( value );
            break;
          case SalesforceInputField.TYPE_TRIM_RIGHT:
            value = Const.rtrim( value );
            break;
          case SalesforceInputField.TYPE_TRIM_BOTH:
            value = Const.trim( value );
            break;
          default:
            break;
        }

        // DO CONVERSIONS...
        //
        ValueMetaInterface targetValueMeta = data.outputRowMeta.getValueMeta( i );
        ValueMetaInterface sourceValueMeta = data.convertRowMeta.getValueMeta( i );
        outputRowData[i] = targetValueMeta.convertData( sourceValueMeta, value );

        // Do we need to repeat this field if it is null?
        if ( meta.getInputFields()[i].isRepeated() ) {
          if ( data.previousRow != null && Const.isEmpty( value ) ) {
            outputRowData[i] = data.previousRow[i];
          }
        }

      } // End of loop over fields...

      int rowIndex = data.nrfields;

      // See if we need to add the url to the row...
      if ( meta.includeTargetURL() && !Const.isEmpty( meta.getTargetURLField() ) ) {
        outputRowData[rowIndex++] = data.connection.getURL();
      }

      // See if we need to add the module to the row...
      if ( meta.includeModule() && !Const.isEmpty( meta.getModuleField() ) ) {
        outputRowData[rowIndex++] = data.connection.getModule();
      }

      // See if we need to add the generated SQL to the row...
      if ( meta.includeSQL() && !Const.isEmpty( meta.getSQLField() ) ) {
        outputRowData[rowIndex++] = data.connection.getSQL();
      }

      // See if we need to add the server timestamp to the row...
      if ( meta.includeTimestamp() && !Const.isEmpty( meta.getTimestampField() ) ) {
        outputRowData[rowIndex++] = data.connection.getServerTimestamp();
      }

      // See if we need to add the row number to the row...
      if ( meta.includeRowNumber() && !Const.isEmpty( meta.getRowNumberField() ) ) {
        outputRowData[rowIndex++] = new Long( data.rownr );
      }

      if ( meta.includeDeletionDate() && !Const.isEmpty( meta.getDeletionDateField() ) ) {
        outputRowData[rowIndex++] = srvalue.getDeletionDate();
      }

      RowMetaInterface irow = getInputRowMeta();

      data.previousRow = irow == null ? outputRowData : irow.cloneRow( outputRowData ); // copy it to make
    } catch ( Exception e ) {
      throw new KettleException( BaseMessages
        .getString( PKG, "SalesforceInput.Exception.CanNotReadFromSalesforce" ), e );
    }

    return outputRowData;
  }

  /*
   * build the SQL statement to send to Salesforce
   */
  private String BuiltSOQl() {
    String sql = "";
    SalesforceInputField[] fields = meta.getInputFields();

    switch ( meta.getRecordsFilter() ) {
      case SalesforceConnectionUtils.RECORDS_FILTER_UPDATED:
        for ( int i = 0; i < data.nrfields; i++ ) {
          SalesforceInputField field = fields[i];
          sql += environmentSubstitute( field.getField() );
          if ( i < data.nrfields - 1 ) {
            sql += ",";
          }
        }
        break;
      case SalesforceConnectionUtils.RECORDS_FILTER_DELETED:
        sql += "SELECT ";
        for ( int i = 0; i < data.nrfields; i++ ) {
          SalesforceInputField field = fields[i];
          sql += environmentSubstitute( field.getField() );
          if ( i < data.nrfields - 1 ) {
            sql += ",";
          }
        }
        sql += " FROM " + environmentSubstitute( meta.getModule() ) + " WHERE isDeleted = true";
        break;
      default:
        sql += "SELECT ";
        for ( int i = 0; i < data.nrfields; i++ ) {
          SalesforceInputField field = fields[i];
          sql += environmentSubstitute( field.getField() );
          if ( i < data.nrfields - 1 ) {
            sql += ",";
          }
        }
        sql = sql + " FROM " + environmentSubstitute( meta.getModule() );
        if ( !Const.isEmpty( environmentSubstitute( meta.getCondition() ) ) ) {
          sql += " WHERE " + environmentSubstitute( meta.getCondition().replace( "\n\r", "" ).replace( "\n", "" ) );
        }
        break;
    }

    return sql;
  }

  /**
   * Build an empty row based on the meta-data.
   *
   * @return
   */
  private Object[] buildEmptyRow() {
    Object[] rowData = RowDataUtil.allocateRowData( data.outputRowMeta.size() );
    return rowData;
  }

  public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
    meta = (SalesforceInputMeta) smi;
    data = (SalesforceInputData) sdi;

    if ( super.init( smi, sdi ) ) {
      // get total fields in the grid
      data.nrfields = meta.getInputFields().length;

      // Check if field list is filled
      if ( data.nrfields == 0 ) {
        log.logError( BaseMessages.getString( PKG, "SalesforceInputDialog.FieldsMissing.DialogMessage" ) );
        return false;
      }

      String soSQL = environmentSubstitute( meta.getQuery() );
      // check target URL
      String realUrl = environmentSubstitute( meta.getTargetURL() );
      if ( Const.isEmpty( realUrl ) ) {
        log.logError( BaseMessages.getString( PKG, "SalesforceInput.TargetURLMissing.Error" ) );
        return false;
      }
      // check username
      String realUser = environmentSubstitute( meta.getUserName() );
      if ( Const.isEmpty( realUser ) ) {
        log.logError( BaseMessages.getString( PKG, "SalesforceInput.UsernameMissing.Error" ) );
        return false;
      }
      try {

        if ( meta.isSpecifyQuery() ) {
          // Check if user specified a query
          if ( Const.isEmpty( soSQL ) ) {
            log.logError( BaseMessages.getString( PKG, "SalesforceInputDialog.QueryMissing.DialogMessage" ) );
            return false;
          }
        } else {
          data.Module = environmentSubstitute( meta.getModule() );
          // Check if module is specified
          if ( Const.isEmpty( data.Module ) ) {
            log.logError( BaseMessages.getString( PKG, "SalesforceInputDialog.ModuleMissing.DialogMessage" ) );
            return false;
          }
          // check records filter
          if ( meta.getRecordsFilter() != SalesforceConnectionUtils.RECORDS_FILTER_ALL ) {
            String realFromDateString = environmentSubstitute( meta.getReadFrom() );
            if ( Const.isEmpty( realFromDateString ) ) {
              log.logError( BaseMessages.getString( PKG, "SalesforceInputDialog.FromDateMissing.DialogMessage" ) );
              return false;
            }
            String realToDateString = environmentSubstitute( meta.getReadTo() );
            if ( Const.isEmpty( realToDateString ) ) {
              log.logError( BaseMessages.getString( PKG, "SalesforceInputDialog.ToDateMissing.DialogMessage" ) );
              return false;
            }
            try {
              SimpleDateFormat dateFormat = new SimpleDateFormat( SalesforceInputMeta.DATE_TIME_FORMAT );
              data.startCal = new GregorianCalendar();
              data.startCal.setTime( dateFormat.parse( realFromDateString ) );
              data.endCal = new GregorianCalendar();
              data.endCal.setTime( dateFormat.parse( realToDateString ) );
              dateFormat = null;
            } catch ( Exception e ) {
              log.logError( BaseMessages.getString( PKG, "SalesforceInput.ErrorParsingDate" ), e );
              return false;
            }
          }
        }

        data.limit = Const.toLong( environmentSubstitute( meta.getRowLimit() ), 0 );

        // create a Salesforce connection
        data.connection =
          new SalesforceConnection( log, realUrl, realUser, environmentSubstitute( meta.getPassword() ) );
        // set timeout
        data.connection.setTimeOut( Const.toInt( environmentSubstitute( meta.getTimeOut() ), 0 ) );
        // Do we use compression?
        data.connection.setUsingCompression( meta.isUsingCompression() );
        // Do we have to query for all records included deleted records
        data.connection.queryAll( meta.isQueryAll() );

        // Build query if needed
        if ( meta.isSpecifyQuery() ) {
          // Free hand SOQL Query
          data.connection.setSQL( soSQL.replace( "\n\r", " " ).replace( "\n", " " ) );
        } else {
          // retrieve data from a module
          // Set condition if needed
          String realCondition = environmentSubstitute( meta.getCondition() );
          if ( !Const.isEmpty( realCondition ) ) {
            data.connection.setCondition( realCondition );
          }
          // Set module
          data.connection.setModule( data.Module );

          // Set calendars for update or deleted records
          if ( meta.getRecordsFilter() != SalesforceConnectionUtils.RECORDS_FILTER_ALL ) {
            data.connection.setCalendar( meta.getRecordsFilter(), data.startCal, data.endCal );
          }

          if ( meta.getRecordsFilter() == SalesforceConnectionUtils.RECORDS_FILTER_UPDATED ) {
            // Return fields list
            data.connection.setFieldsList( BuiltSOQl() );
          } else {
            // Build now SOQL
            data.connection.setSQL( BuiltSOQl() );
          }
        }

        // Now connect ...
        data.connection.connect();

        return true;
      } catch ( KettleException ke ) {
        logError( BaseMessages.getString( PKG, "SalesforceInput.Log.ErrorOccurredDuringStepInitialize" )
          + ke.getMessage() );
      }

      return true;
    }
    return false;
  }

  public void dispose( StepMetaInterface smi, StepDataInterface sdi ) {
    meta = (SalesforceInputMeta) smi;
    data = (SalesforceInputData) sdi;
    try {
      if ( data.connection != null ) {
        data.connection.close();
      }
      if ( data.outputRowMeta != null ) {
        data.outputRowMeta = null;
      }
      if ( data.convertRowMeta != null ) {
        data.convertRowMeta = null;
      }
      if ( data.previousRow != null ) {
        data.previousRow = null;
      }
      if ( data.startCal != null ) {
        data.startCal = null;
      }
      if ( data.endCal != null ) {
        data.endCal = null;
      }
    } catch ( Exception e ) { /* Ignore */
    }
    super.dispose( smi, sdi );
  }
}
