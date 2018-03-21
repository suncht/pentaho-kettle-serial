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

package org.pentaho.di.trans.steps.monetdbagilemart;

import org.pentaho.di.core.Props;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.monetdbbulkloader.MonetDBBulkLoader;
import org.pentaho.di.trans.steps.monetdbbulkloader.MonetDBBulkLoaderMeta;

public class MonetDBAgileMartMeta extends MonetDBBulkLoaderMeta {

  protected long rowLimit = getLongProperty( "AgileBIRowLimit", 100000 ); // have a nice default

  public long getRowLimit() {
    return rowLimit;
  }

  public void setRowLimit( long limit ) {
    rowLimit = limit;
  }

  public static String getStringProperty( String name, String defaultValue ) {

    String value = Props.isInitialized() ? Props.getInstance().getProperty( name ) : null;
    if ( value == null ) {
      value = defaultValue;
    }
    return value;

  }

  public static long getLongProperty( String name, long defaultValue ) {

    String valueStr = Props.isInitialized() ? Props.getInstance().getProperty( name ) : null;
    try {
      long value = Long.parseLong( valueStr );
      return value;
    } catch ( NumberFormatException e ) {
      // the value for this property is not a valid number
    }
    return defaultValue;
  }

  protected void setupDatabaseMeta() {

    if ( this.getDatabaseMeta() == null ) {
      if ( getParentStepMeta() != null ) {
        TransMeta transMeta = getParentStepMeta().getParentTransMeta();
        if ( transMeta != null ) {
          setDatabaseMeta( transMeta.findDatabase( transMeta.environmentSubstitute( getDbConnectionName() ) ) );
        }
      }
    }

  }

  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr,
    TransMeta transMeta, Trans trans ) {

    setupDatabaseMeta();

    ( (MonetDBAgileMartMeta) stepMeta.getStepMetaInterface() ).setDatabaseMeta( this.getDatabaseMeta() );

    return new MonetDBAgileMart( stepMeta, stepDataInterface, cnr, transMeta, trans );
  }

  public Object clone() {
    MonetDBAgileMartMeta retval = (MonetDBAgileMartMeta) super.clone();

    return retval;
  }

  @Override
  public void setDefault() {

    setupDatabaseMeta();

    this.setFieldTable( null );

    this.setBufferSize( getStringProperty( "MonetDBDefaultBufferSize", "100000" ) );
    this.setSchemaName( getStringProperty( "MonetDBDefaultSchemaName", "" ) );
    this.setLogFile( getStringProperty( "MonetDBDefaultLogFile", "" ) );
    this.setEncoding( getStringProperty( "MonetDBDefaultEncoding", "UTF-8" ) );
    this.setTruncate( true );
    this.setAutoSchema( true );
    this.setAutoStringWidths( true );

    allocate( 0 );
  }

  @Override
  public DatabaseMeta getDatabaseMeta( MonetDBBulkLoader loader ) {
    setupDatabaseMeta();
    return getDatabaseMeta();
  }

  @Override
  public String getXML() {
    setupDatabaseMeta();
    return super.getXML();
  }
}
