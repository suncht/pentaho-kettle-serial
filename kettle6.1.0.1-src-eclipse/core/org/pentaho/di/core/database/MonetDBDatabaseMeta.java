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

import java.util.Collection;
import java.util.HashSet;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.row.ValueMetaInterface;

/**
 * Contains Generic Database Connection information through static final members
 *
 * @author Matt
 * @since 11-mrt-2005
 */

public class MonetDBDatabaseMeta extends BaseDatabaseMeta implements DatabaseInterface {

  public static ThreadLocal<Boolean> safeModeLocal = new ThreadLocal<Boolean>();

  public static final int DEFAULT_VARCHAR_LENGTH = 100;

  protected static final String FIELDNAME_PROTECTOR = "_";

  private static final int MAX_VARCHAR_LENGTH = Integer.MAX_VALUE;

  private static Collection<String> reservedWordAlt = new HashSet<String>();

  static {
    reservedWordAlt.add( "IS" );
    reservedWordAlt.add( "ISNULL" );
    reservedWordAlt.add( "NOTNULL" );
    reservedWordAlt.add( "IN" );
    reservedWordAlt.add( "BETWEEN" );
    reservedWordAlt.add( "OVERLAPS" );
    reservedWordAlt.add( "LIKE" );
    reservedWordAlt.add( "ILIKE" );
    reservedWordAlt.add( "NOT" );
    reservedWordAlt.add( "AND" );
    reservedWordAlt.add( "OR" );
    reservedWordAlt.add( "CHAR" );
    reservedWordAlt.add( "VARCHAR" );
    reservedWordAlt.add( "CLOB" );
    reservedWordAlt.add( "BLOB" );
    reservedWordAlt.add( "DECIMAL" );
    reservedWordAlt.add( "DEC" );
    reservedWordAlt.add( "NUMERIC" );
    reservedWordAlt.add( "TINYINT" );
    reservedWordAlt.add( "SMALLINT" );
    reservedWordAlt.add( "INT" );
    reservedWordAlt.add( "BIGINT" );
    reservedWordAlt.add( "REAL" );
    reservedWordAlt.add( "DOUBLE" );
    reservedWordAlt.add( "BOOLEAN" );
    reservedWordAlt.add( "DATE" );
    reservedWordAlt.add( "TIME" );
    reservedWordAlt.add( "TIMESTAMP" );
    reservedWordAlt.add( "INTERVAL" );
    reservedWordAlt.add( "YEAR" );
    reservedWordAlt.add( "MONTH" );
    reservedWordAlt.add( "DAY" );
    reservedWordAlt.add( "HOUR" );
    reservedWordAlt.add( "MINUTE" );
    reservedWordAlt.add( "SECOND" );
    reservedWordAlt.add( "TIMEZONE" );
    reservedWordAlt.add( "EXTRACT" );
    reservedWordAlt.add( "CURRENT_DATE" );
    reservedWordAlt.add( "CURRENT_TIME" );
    reservedWordAlt.add( "CURRENT_TIMESTAMP" );
    reservedWordAlt.add( "LOCALTIME" );
    reservedWordAlt.add( "LOCALTIMESTAMP" );
    reservedWordAlt.add( "CURRENT_TIME" );
    reservedWordAlt.add( "SERIAL" );
    reservedWordAlt.add( "START" );
    reservedWordAlt.add( "WITH" );
    reservedWordAlt.add( "INCREMENT" );
    reservedWordAlt.add( "CACHE" );
    reservedWordAlt.add( "CYCLE" );
    reservedWordAlt.add( "SEQUENCE" );
    reservedWordAlt.add( "GETANCHOR" );
    reservedWordAlt.add( "GETBASENAME" );
    reservedWordAlt.add( "GETCONTENT" );
    reservedWordAlt.add( "GETCONTEXT" );
    reservedWordAlt.add( "GETDOMAIN" );
    reservedWordAlt.add( "GETEXTENSION" );
    reservedWordAlt.add( "GETFILE" );
    reservedWordAlt.add( "GETHOST" );
    reservedWordAlt.add( "GETPORT" );
    reservedWordAlt.add( "GETPROTOCOL" );
    reservedWordAlt.add( "GETQUERY" );
    reservedWordAlt.add( "GETUSER" );
    reservedWordAlt.add( "GETROBOTURL" );
    reservedWordAlt.add( "ISURL" );
    reservedWordAlt.add( "NEWURL" );
    reservedWordAlt.add( "BROADCAST" );
    reservedWordAlt.add( "MASKLEN" );
    reservedWordAlt.add( "SETMASKLEN" );
    reservedWordAlt.add( "NETMASK" );
    reservedWordAlt.add( "HOSTMASK" );
    reservedWordAlt.add( "NETWORK" );
    reservedWordAlt.add( "TEXT" );
    reservedWordAlt.add( "ABBREV" );
    reservedWordAlt.add( "CREATE" );
    reservedWordAlt.add( "TYPE" );
    reservedWordAlt.add( "NAME" );
    reservedWordAlt.add( "DROP" );
    reservedWordAlt.add( "USER" );
  }

  @Override
  public int[] getAccessTypeList() {
    return new int[] {
      DatabaseMeta.TYPE_ACCESS_NATIVE, DatabaseMeta.TYPE_ACCESS_ODBC, DatabaseMeta.TYPE_ACCESS_JNDI };
  }

  /**
   * @see DatabaseInterface#getNotFoundTK(boolean)
   */
  @Override
  public int getNotFoundTK( boolean use_autoinc ) {
    if ( supportsAutoInc() && use_autoinc ) {
      return 1;
    }
    return super.getNotFoundTK( use_autoinc );
  }

  @Override
  public String getDriverClass() {
    if ( getAccessType() == DatabaseMeta.TYPE_ACCESS_NATIVE ) {
      return "nl.cwi.monetdb.jdbc.MonetDriver";
    } else {
      return "sun.jdbc.odbc.JdbcOdbcDriver"; // always ODBC!
    }

  }

  @Override
  public String getURL( String hostname, String port, String databaseName ) {
    if ( getAccessType() == DatabaseMeta.TYPE_ACCESS_NATIVE ) {
      if ( Const.isEmpty( port ) ) {
        return "jdbc:monetdb://" + hostname + "/" + databaseName;
      } else {
        return "jdbc:monetdb://" + hostname + ":" + port + "/" + databaseName;
      }
    } else {
      return "jdbc:odbc:" + databaseName;
    }
  }

  /**
   * Checks whether or not the command setFetchSize() is supported by the JDBC driver...
   *
   * @return true is setFetchSize() is supported!
   */
  @Override
  public boolean isFetchSizeSupported() {
    return false;
  }

  /**
   * @return true if the database supports bitmap indexes
   */
  @Override
  public boolean supportsBitmapIndex() {
    return true;
  }

  @Override
  public boolean supportsAutoInc() {
    return true;
  }

  @Override
  public boolean supportsBatchUpdates() {
    return true;
  }

  @Override
  public boolean supportsSetMaxRows() {
    return true;
  }

  /**
   * @param tableName
   *          The table to be truncated.
   * @return The SQL statement to truncate a table: remove all rows from it without a transaction
   */
  @Override
  public String getTruncateTableStatement( String tableName ) {
    return "DELETE FROM " + tableName;
  }

  /**
   * Generates the SQL statement to add a column to the specified table For this generic type, i set it to the most
   * common possibility.
   *
   * @param tablename
   *          The table to add
   * @param v
   *          The column defined as a value
   * @param tk
   *          the name of the technical key field
   * @param use_autoinc
   *          whether or not this field uses auto increment
   * @param pk
   *          the name of the primary key field
   * @param semicolon
   *          whether or not to add a semi-colon behind the statement.
   * @return the SQL statement to add a column to the specified table
   */
  @Override
  public String getAddColumnStatement( String tablename, ValueMetaInterface v, String tk, boolean use_autoinc,
    String pk, boolean semicolon ) {
    return "ALTER TABLE " + tablename + " ADD " + getFieldDefinition( v, tk, pk, use_autoinc, true, false );
  }

  /**
   * Generates the SQL statement to modify a column in the specified table
   *
   * @param tablename
   *          The table to add
   * @param v
   *          The column defined as a value
   * @param tk
   *          the name of the technical key field
   * @param use_autoinc
   *          whether or not this field uses auto increment
   * @param pk
   *          the name of the primary key field
   * @param semicolon
   *          whether or not to add a semi-colon behind the statement.
   * @return the SQL statement to modify a column in the specified table
   */
  @Override
  public String getModifyColumnStatement( String tablename, ValueMetaInterface v, String tk, boolean use_autoinc,
    String pk, boolean semicolon ) {
    return "ALTER TABLE " + tablename + " MODIFY " + getFieldDefinition( v, tk, pk, use_autoinc, true, false );
  }

  @Override
  public String[] getReservedWords() {
    return reservedWordAlt.toArray( new String[] {} );
  }

  @Override
  public String getFieldDefinition( ValueMetaInterface v, String tk, String pk, boolean use_autoinc,
    boolean add_fieldname, boolean add_cr ) {
    StringBuffer retval = new StringBuffer();

    String fieldname = v.getName();
    int length = v.getLength();
    int precision = v.getPrecision();

    Boolean mode = MonetDBDatabaseMeta.safeModeLocal.get();
    boolean safeMode = mode != null && mode.booleanValue();

    if ( add_fieldname ) {
      // protect the fieldname
      if ( safeMode ) {
        fieldname = getSafeFieldname( fieldname );
      }

      retval.append( fieldname + " " );
    }

    int type = v.getType();
    switch ( type ) {
      case ValueMetaInterface.TYPE_TIMESTAMP:
      case ValueMetaInterface.TYPE_DATE:
        retval.append( "TIMESTAMP" );
        break;
      case ValueMetaInterface.TYPE_BOOLEAN:
        if ( supportsBooleanDataType() ) {
          retval.append( "BOOLEAN" );
        } else {
          retval.append( "CHAR(1)" );
        }
        break;
      case ValueMetaInterface.TYPE_NUMBER:
      case ValueMetaInterface.TYPE_INTEGER:
      case ValueMetaInterface.TYPE_BIGNUMBER:
        if ( fieldname.equalsIgnoreCase( tk ) || // Technical key
          fieldname.equalsIgnoreCase( pk ) // Primary key
        ) {
          if ( use_autoinc ) {
            retval.append( "SERIAL" );
          } else {
            retval.append( "BIGINT" );
          }
        } else {
          // Integer values...
          if ( precision == 0 ) {
            if ( length > 9 ) {
              if ( length < 19 ) {
                // can hold signed values between -9223372036854775808 and 9223372036854775807
                // 18 significant digits
                retval.append( "BIGINT" );
              } else {
                retval.append( "DECIMAL(" ).append( length ).append( ")" );
              }
            } else if ( type == ValueMetaInterface.TYPE_NUMBER ) {
              retval.append( "DOUBLE" );
            } else {
              retval.append( "BIGINT" );
            }
          } else {
            // Floating point values...
            if ( length > 15 ) {
              retval.append( "DECIMAL(" ).append( length );
              if ( precision > 0 ) {
                retval.append( ", " ).append( precision );
              }
              retval.append( ")" );
            } else {
              // A double-precision floating-point number is accurate to approximately 15 decimal places.
              // http://mysql.mirrors-r-us.net/doc/refman/5.1/en/numeric-type-overview.html
              retval.append( "DOUBLE" );
            }
          }
        }
        break;
      case ValueMetaInterface.TYPE_STRING:
        if ( length > getMaxVARCHARLength() ) {
          retval.append( "CLOB" );
        } else {
          retval.append( "VARCHAR(" );
          if ( length > 0 ) {
            retval.append( length );
          } else {
            if ( safeMode ) {
              retval.append( DEFAULT_VARCHAR_LENGTH );
            }
          }
          retval.append( ")" );
        }
        break;
      default:
        retval.append( " UNKNOWN" );
        break;
    }

    if ( add_cr ) {
      retval.append( Const.CR );
    }

    return retval.toString();
  }

  @Override
  public String[] getUsedLibraries() {
    return new String[] { "monetdb-jdbc-2.8.jar", };
  }

  /**
   * Returns the minimal SQL to launch in order to determine the layout of the resultset for a given database table
   *
   * @param tableName
   *          The name of the table to determine the layout for
   * @return The SQL to launch.
   */
  @Override
  public String getSQLQueryFields( String tableName ) {
    return "SELECT * FROM " + tableName + ";";
  }

  @Override
  public boolean supportsResultSetMetadataRetrievalOnly() {
    return true;
  }

  @Override
  public int getMaxVARCHARLength() {
    return MAX_VARCHAR_LENGTH;
  }
}
