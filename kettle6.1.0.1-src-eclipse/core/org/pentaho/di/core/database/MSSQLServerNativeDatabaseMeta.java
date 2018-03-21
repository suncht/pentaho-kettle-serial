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

package org.pentaho.di.core.database;

import org.pentaho.di.core.Const;

public class MSSQLServerNativeDatabaseMeta extends MSSQLServerDatabaseMeta {
  public static final String ATTRIBUTE_USE_INTEGRATED_SECURITY = "MSSQLUseIntegratedSecurity";

  @Override
  public String getDriverClass() {
    if ( getAccessType() == DatabaseMeta.TYPE_ACCESS_ODBC ) {
      return "sun.jdbc.odbc.JdbcOdbcDriver";
    } else {
      return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }
  }

  @Override
  public String getURL( String hostname, String port, String databaseName ) {
    if ( getAccessType() == DatabaseMeta.TYPE_ACCESS_ODBC ) {
      return "jdbc:odbc:" + databaseName;
    } else {
      String useIntegratedSecurity = null;
      Object value = getAttributes().get( ATTRIBUTE_USE_INTEGRATED_SECURITY );
      if ( value != null && value instanceof String ) {
        useIntegratedSecurity = (String) value;
        // Check if the String can be parsed into a boolean
        try {
          Boolean.parseBoolean( useIntegratedSecurity );
        } catch ( IllegalArgumentException e ) {
          useIntegratedSecurity = "false";
        }
      }

      String url = "jdbc:sqlserver://" + hostname;

      if ( !Const.isEmpty( port ) && Const.toInt( port, -1 ) > 0 ) {
        url += ":" + port;
      }
      url += ";databaseName=" + databaseName + ";integratedSecurity=" + useIntegratedSecurity;

      return url;
    }
  }

  @Override
  public boolean supportsGetBlob() {
    return false;
  }
}
