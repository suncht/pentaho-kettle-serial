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

package org.pentaho.di.trans.steps.sapinput.mock;

import java.util.Collection;
import java.util.Vector;

import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.trans.steps.sapinput.sap.SAPConnection;
import org.pentaho.di.trans.steps.sapinput.sap.SAPConnectionParams;
import org.pentaho.di.trans.steps.sapinput.sap.SAPException;
import org.pentaho.di.trans.steps.sapinput.sap.SAPField;
import org.pentaho.di.trans.steps.sapinput.sap.SAPFunction;
import org.pentaho.di.trans.steps.sapinput.sap.SAPFunctionSignature;
import org.pentaho.di.trans.steps.sapinput.sap.SAPResultSet;
import org.pentaho.di.trans.steps.sapinput.sap.SAPRow;
import org.pentaho.di.trans.steps.sapinput.sap.impl.SAPRowIterator;

public class SAPConnectionMock implements SAPConnection {

  private Collection<SAPFunction> sfc = new Vector<SAPFunction>();

  public SAPConnectionMock() {
    sfc.add( new SAPFunction( "SearchCustomer", "SAP Testfunction with senseful params", "Group A", "", "" ) );
    sfc.add( new SAPFunction( "SAPFunction0", "SAP Testfunction 0", "Group A", "", "" ) );
    sfc.add( new SAPFunction( "SAPFunction1", "SAP Testfunction 1", "Group A", "", "" ) );
    sfc.add( new SAPFunction( "SAPFunction2", "SAP Testfunction 2", "Group A", "", "" ) );
    sfc.add( new SAPFunction( "SAPFunction3", "SAP Testfunction 3", "Group B", "C", "" ) );
    sfc.add( new SAPFunction( "SAPFunction4", "SAP Testfunction 4", "Group B", "C", "" ) );
    sfc.add( new SAPFunction( "SAPFunction5", "SAP Testfunction 5", "Group C", "C", "" ) );
    sfc.add( new SAPFunction( "SAPFunction6", "SAP Testfunction 6", "Group C", "C", "" ) );
    sfc.add( new SAPFunction( "SAPFunction7", "SAP Testfunction 7", "Group C", "", "" ) );
    sfc.add( new SAPFunction( "SAPFunction8", "SAP Testfunction 8", "Group C", "", "" ) );
    sfc.add( new SAPFunction( "SAPFunction9", "SAP Testfunction 9", "Group C", "", "" ) );
  }

  public void open( DatabaseMeta sapConnection ) throws SAPException {
    // TODO Auto-generated method stub
  }

  public void open( SAPConnectionParams params ) throws SAPException {
    // TODO Auto-generated method stub
  }

  public void close() {
    // TODO Auto-generated method stub
  }

  public Collection<SAPFunction> getFunctions( String query ) throws SAPException {
    Collection<SAPFunction> sfc = new Vector<SAPFunction>();
    for ( SAPFunction sapFunction : this.sfc ) {
      // This emulates a longer wait time, showing a wait cursor to notify the user
      //
      try {
        Thread.sleep( 250 );
      } catch ( InterruptedException e ) { /* Ignore */
      }

      if ( sapFunction.getName().contains( query ) ) {
        sfc.add( sapFunction );
      }
    }
    return sfc;
  }

  public SAPFunction getFunction( String name ) throws SAPException {
    for ( SAPFunction sapFunction : this.sfc ) {
      if ( sapFunction.getName().equals( name ) ) {
        return sapFunction;
      }
    }
    return null;
  }

  public SAPFunctionSignature getFunctionSignature( SAPFunction function ) throws SAPException {
    SAPFunctionSignature sfs = new SAPFunctionSignature();
    if ( function.getName().equalsIgnoreCase( "SearchCustomer" ) ) {
      sfs.addInput( new SAPField( "Name", "", "input_single" ) );
      sfs.addInput( new SAPField( "Zipcode", "", "input_single" ) );
      sfs.addOutput( new SAPField( "Name", "", "output_single" ) );
      sfs.addOutput( new SAPField( "Firstname", "", "output_single" ) );
      sfs.addOutput( new SAPField( "Adress", "", "output_single" ) );
      sfs.addOutput( new SAPField( "Zipcode", "", "output_single" ) );
      sfs.addOutput( new SAPField( "CustomerGroup", "", "output_single" ) );
    } else {
      sfs.addInput( new SAPField( "Field1", "", "input_single" ) );
      sfs.addInput( new SAPField( "Field2", "", "input_single" ) );
      sfs.addInput( new SAPField( "Field3", "", "input_single" ) );
      sfs.addOutput( new SAPField( "Field4", "", "output_single" ) );
      sfs.addOutput( new SAPField( "Field5", "", "output_single" ) );
      sfs.addOutput( new SAPField( "Field6", "", "output_single" ) );
    }

    // Simulate a longer wait by sleeping a bit...
    // This tests the wait cursor in the UI...
    //
    try {
      Thread.sleep( 1500 );
    } catch ( InterruptedException e ) { /* Ignore */
    }

    return sfs;
  }

  public SAPResultSet executeFunctionUncursored( SAPFunction function, Collection<SAPField> input,
    Collection<SAPField> output ) throws SAPException {
    SAPResultSet srs = new SAPResultSet();
    if ( function.getName().equalsIgnoreCase( "SearchCustomer" ) ) {
      for ( int i = 1; i <= 9; i++ ) {
        SAPRow sr = new SAPRow();
        sr.addField( new SAPField( "Name", "", "String", "Casters" + i ) );
        sr.addField( new SAPField( "Firstname", "", "String", "Matt" + i ) );
        sr.addField( new SAPField( "Adress", "", "String", "Pentahoway 77" ) );
        sr.addField( new SAPField( "Zipcode", "", "Number", 12345 + ( i * 10000 ) ) );
        sr.addField( new SAPField( "CustomerGroup", "", "String", "ABC" + i ) );
        srs.addRow( sr );
      }
    } else {
      for ( int i = 1; i <= 9; i++ ) {
        SAPRow sr = new SAPRow();
        sr.addField( new SAPField( "Field4", "", "String", "Testvalue" + i ) );
        sr.addField( new SAPField( "Field5", "", "Number", 12345 + ( i * 10000 ) ) );
        sr.addField( new SAPField( "Field6", "", "Decimal", 77.88 + ( i * 10 ) ) );
        srs.addRow( sr );
      }
    }
    return srs;
  }

  // this method cannot be mocked
  // for testcases use the executeFunctionUncursored
  public SAPRowIterator executeFunctionCursored( SAPFunction function, Collection<SAPField> input,
    Collection<SAPField> output ) throws SAPException {
    return null;
  }
}
