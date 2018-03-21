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

package org.pentaho.di.trans.steps.sapinput;

import java.util.ArrayList;

import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.steps.sapinput.sap.SAPConnectionFactory;
import org.pentaho.di.trans.steps.sapinput.sap.SAPException;
import org.pentaho.di.trans.steps.sapinput.sap.SAPField;
import org.pentaho.di.trans.steps.sapinput.sap.SAPRow;
import org.pentaho.di.trans.steps.sapinput.sap.impl.SAPRowIterator;

/**
 * Reads information from a database table by using freehand SQL
 *
 * @author Matt
 * @since 8-apr-2003
 */
public class SapInput extends BaseStep implements StepInterface {
  private static Class<?> PKG = SapInputMeta.class; // for i18n purposes, needed by Translator2!!

  private SapInputMeta meta;
  private SapInputData data;

  public SapInput( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
    Trans trans ) {
    super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
  }

  public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
    Object[] r = getRow();
    if ( r == null ) { // no more input to be expected...

      setOutputDone();
      return false;
    }

    if ( first ) {
      first = false;

      // Determine the output row metadata of this step
      //
      data.outputRowMeta = new RowMeta();
      meta.getFields( data.outputRowMeta, getStepname(), null, null, this, repository, metaStore );

      // Pre-calculate the indexes of the parameters for performance reasons...
      //
      data.parameterIndexes = new ArrayList<Integer>();
      for ( SapParameter parameter : meta.getParameters() ) {
        int index = getInputRowMeta().indexOfValue( parameter.getFieldName() );
        if ( index < 0 ) {
          throw new KettleException( "Unable to find field '" + parameter.getFieldName() + "'" );
        }
        data.parameterIndexes.add( index );
      }

      // Pre-calculate the output fields
      //
      data.output = new ArrayList<SAPField>();
      for ( SapOutputField outputField : meta.getOutputFields() ) {
        SAPField field =
          new SAPField( outputField.getSapFieldName(), outputField.getTableName(), "output_"
            + outputField.getSapType().getDescription() );
        data.output.add( field );
      }
    }

    // Assemble the list of input fields for the SAP function execution...
    //
    ArrayList<SAPField> input = new ArrayList<SAPField>();
    for ( int i = 0; i < meta.getParameters().size(); i++ ) {
      SapParameter parameter = meta.getParameters().get( i );
      int fieldIndex = data.parameterIndexes.get( i );
      ValueMetaInterface valueMeta = getInputRowMeta().getValueMeta( fieldIndex );
      Object value = valueMeta.convertToNormalStorageType( r[fieldIndex] );
      // TODO: figure out if the executeFunction needs the data to be in a specific data type or format!!
      // If so, value needs to be converted to the appropriate data type here.

      SAPField field =
        new SAPField( parameter.getParameterName(), parameter.getTableName(), "input_"
          + parameter.getSapType().getDescription(), value );
      input.add( field );
    }

    // Get the output...
    //
    SAPRowIterator resultSet;
    try {
      resultSet = data.sapConnection.executeFunctionCursored( meta.getFunction(), input, data.output );
    } catch ( SAPException e ) {
      throw new KettleException( e );
    }
    while ( resultSet.hasNext() ) {
      SAPRow sapRow = resultSet.next();
      Object[] outputRowData = RowDataUtil.allocateRowData( data.outputRowMeta.size() );
      int outputIndex = 0; // Makes it easier to add all sorts of fields later on, like row number, input fields, etc.

      for ( SAPField field : sapRow.getFields() ) {
        // TODO: Here we should check as well whether or not the correct data types are delivered from SAP.
        // Make sure that we don't pass the appropriate data types : String, long, double, Date, BigDecimal, Boolean,
        // byte[] ONLY!!
        //
        outputRowData[outputIndex++] = field.getValue();
      }

      // Pass the row along: row metadata and data need to correspond!!
      //
      putRow( data.outputRowMeta, outputRowData );

      if ( getTrans().isStopped() ) {
        break;
      }
    }

    return true;
  }

  public void dispose( StepMetaInterface smi, StepDataInterface sdi ) {
    if ( data.sapConnection != null ) {
      data.sapConnection.close();
    }
    super.dispose( smi, sdi );
  }

  public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
    meta = (SapInputMeta) smi;
    data = (SapInputData) sdi;

    if ( super.init( smi, sdi ) ) {
      // Verify some basic things first...
      //
      boolean passed = true;
      if ( meta.getFunction() == null ) {
        logError( BaseMessages.getString( PKG, "SapInput.Exception.FunctionIsNeeded" ) );
        passed = false;
      }

      if ( meta.getDatabaseMeta() == null ) {
        logError( BaseMessages.getString( PKG, "SapInput.Exception.DatabaseConnectionsIsNeeded" ) );
        passed = false;
      }
      if ( !passed ) {
        return false;
      }

      try {
        data.sapConnection = SAPConnectionFactory.create();
        data.sapConnection.open( meta.getDatabaseMeta() );
        return true;
      } catch ( SAPException e ) {
        logError( "An error occurred while connecting to SAP ERP, processing will be stopped:", e );
        setErrors( 1 );
        stopAll();
      }
    }

    return false;
  }

}
