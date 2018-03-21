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

package org.pentaho.di.core;

import java.math.BigDecimal;
import java.util.Date;

import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.repository.LongObjectId;
import org.pentaho.di.repository.ObjectId;

public class RowMetaAndData implements Cloneable {
  private RowMetaInterface rowMeta;

  private Object[] data;

  public RowMetaAndData() {
    clear();
  }

  /**
   * @param rowMeta
   * @param data
   */
  public RowMetaAndData( RowMetaInterface rowMeta, Object... data ) {
    this.rowMeta = rowMeta;
    this.data = data;
  }

  @Override
  public RowMetaAndData clone() {
    RowMetaAndData c = new RowMetaAndData();
    c.rowMeta = rowMeta.clone();
    try {
      c.data = rowMeta.cloneRow( data );
    } catch ( KettleValueException e ) {
      throw new RuntimeException( "Problem with clone row detected in RowMetaAndData", e );
    }

    return c;
  }

  @Override
  public String toString() {
    try {
      return rowMeta.getString( data );
    } catch ( KettleValueException e ) {
      return rowMeta.toString() + ", error presenting data: " + e.toString();
    }
  }

  /**
   * @return the data
   */
  public Object[] getData() {
    return data;
  }

  /**
   * @param data
   *          the data to set
   */
  public void setData( Object[] data ) {
    this.data = data;
  }

  /**
   * @return the rowMeta
   */
  public RowMetaInterface getRowMeta() {
    return rowMeta;
  }

  /**
   * @param rowMeta
   *          the rowMeta to set
   */
  public void setRowMeta( RowMetaInterface rowMeta ) {
    this.rowMeta = rowMeta;
  }

  @Override
  public int hashCode() {
    try {
      return rowMeta.hashCode( data );
    } catch ( KettleValueException e ) {
      throw new RuntimeException(
        "Row metadata and data: unable to calculate hashcode because of a data conversion problem", e );
    }
  }

  @Override
  public boolean equals( Object obj ) {
    try {
      return rowMeta.compare( data, ( (RowMetaAndData) obj ).getData() ) == 0;
    } catch ( KettleValueException e ) {
      throw new RuntimeException(
        "Row metadata and data: unable to compare rows because of a data conversion problem", e );
    }
  }

  public void addValue( ValueMetaInterface valueMeta, Object valueData ) {
    if ( valueMeta.isInteger() && ( valueData instanceof ObjectId ) ) {
      valueData = new LongObjectId( (ObjectId) valueData ).longValue();
    }
    data = RowDataUtil.addValueData( data, rowMeta.size(), valueData );
    rowMeta.addValueMeta( valueMeta );
  }

  public void addValue( String valueName, int valueType, Object valueData ) {
    addValue( new ValueMeta( valueName, valueType ), valueData );
  }

  public void clear() {
    rowMeta = new RowMeta();
    data = new Object[] {};
  }

  public long getInteger( String valueName, long def ) throws KettleValueException {
    int idx = rowMeta.indexOfValue( valueName );
    if ( idx < 0 ) {
      throw new KettleValueException( "Unknown column '" + valueName + "'" );
    }
    return getInteger( idx, def );
  }

  public long getInteger( int index, long def ) throws KettleValueException {
    Long number = rowMeta.getInteger( data, index );
    if ( number == null ) {
      return def;
    }
    return number.longValue();
  }

  public Long getInteger( String valueName ) throws KettleValueException {
    int idx = rowMeta.indexOfValue( valueName );
    if ( idx < 0 ) {
      throw new KettleValueException( "Unknown column '" + valueName + "'" );
    }
    return rowMeta.getInteger( data, idx );
  }

  public Long getInteger( int index ) throws KettleValueException {
    return rowMeta.getInteger( data, index );
  }

  public double getNumber( String valueName, double def ) throws KettleValueException {
    int idx = rowMeta.indexOfValue( valueName );
    if ( idx < 0 ) {
      throw new KettleValueException( "Unknown column '" + valueName + "'" );
    }
    return getNumber( idx, def );
  }

  public double getNumber( int index, double def ) throws KettleValueException {
    Double number = rowMeta.getNumber( data, index );
    if ( number == null ) {
      return def;
    }
    return number.doubleValue();
  }

  public Date getDate( String valueName, Date def ) throws KettleValueException {
    int idx = rowMeta.indexOfValue( valueName );
    if ( idx < 0 ) {
      throw new KettleValueException( "Unknown column '" + valueName + "'" );
    }
    return getDate( idx, def );
  }

  public Date getDate( int index, Date def ) throws KettleValueException {
    Date date = rowMeta.getDate( data, index );
    if ( date == null ) {
      return def;
    }
    return date;
  }

  public BigDecimal getBigNumber( String valueName, BigDecimal def ) throws KettleValueException {
    int idx = rowMeta.indexOfValue( valueName );
    if ( idx < 0 ) {
      throw new KettleValueException( "Unknown column '" + valueName + "'" );
    }
    return getBigNumber( idx, def );
  }

  public BigDecimal getBigNumber( int index, BigDecimal def ) throws KettleValueException {
    BigDecimal number = rowMeta.getBigNumber( data, index );
    if ( number == null ) {
      return def;
    }
    return number;
  }

  public boolean getBoolean( String valueName, boolean def ) throws KettleValueException {
    int idx = rowMeta.indexOfValue( valueName );
    if ( idx < 0 ) {
      throw new KettleValueException( "Unknown column '" + valueName + "'" );
    }
    return getBoolean( idx, def );
  }

  public boolean getBoolean( int index, boolean def ) throws KettleValueException {
    Boolean b = rowMeta.getBoolean( data, index );
    if ( b == null ) {
      return def;
    }
    return b.booleanValue();
  }

  public String getString( String valueName, String def ) throws KettleValueException {
    int idx = rowMeta.indexOfValue( valueName );
    if ( idx < 0 ) {
      throw new KettleValueException( "Unknown column '" + valueName + "'" );
    }
    return getString( idx, def );
  }

  public String getString( int index, String def ) throws KettleValueException {
    String string = rowMeta.getString( data, index );
    if ( string == null ) {
      return def;
    }
    return string;
  }

  public byte[] getBinary( String valueName, byte[] def ) throws KettleValueException {
    int idx = rowMeta.indexOfValue( valueName );
    if ( idx < 0 ) {
      throw new KettleValueException( "Unknown column '" + valueName + "'" );
    }
    return getBinary( idx, def );
  }

  public byte[] getBinary( int index, byte[] def ) throws KettleValueException {
    byte[] bin = rowMeta.getBinary( data, index );
    if ( bin == null ) {
      return def;
    }
    return bin;
  }

  public int compare( RowMetaAndData compare, int[] is, boolean[] bs ) throws KettleValueException {
    return rowMeta.compare( data, compare.getData(), is );
  }

  public boolean isNumeric( int index ) {
    return rowMeta.getValueMeta( index ).isNumeric();
  }

  public int size() {
    return rowMeta.size();
  }

  public ValueMetaInterface getValueMeta( int index ) {
    return rowMeta.getValueMeta( index );
  }

  /**
   * Returns value as specified java type. Used for metadata injection.
   */
  public Object getAsJavaType( String valueName, Class<?> destinationType ) throws KettleValueException {
    int idx = rowMeta.indexOfValue( valueName );
    if ( idx < 0 ) {
      throw new KettleValueException( "Unknown column '" + valueName + "'" );
    }

    ValueMetaInterface metaType = rowMeta.getValueMeta( idx );
    // find by source value type
    switch ( metaType.getType() ) {
      case ValueMetaInterface.TYPE_STRING:
        String vs = rowMeta.getString( data, idx );
        if ( vs == null ) {
          return null;
        }
        if ( String.class.isAssignableFrom( destinationType ) ) {
          return vs;
        } else if ( int.class.isAssignableFrom( destinationType ) || Integer.class.isAssignableFrom(
            destinationType ) ) {
          return Integer.parseInt( vs );
        } else if ( long.class.isAssignableFrom( destinationType ) || Long.class.isAssignableFrom( destinationType ) ) {
          return Long.parseLong( vs );
        } else if ( boolean.class.isAssignableFrom( destinationType ) || Boolean.class.isAssignableFrom(
            destinationType ) ) {
          return "Y".equalsIgnoreCase( vs ) || "Yes".equalsIgnoreCase( vs ) || "true".equalsIgnoreCase( vs );
        } else if ( destinationType.isEnum() ) {
          for ( Object eo : destinationType.getEnumConstants() ) {
            Enum<?> e = (Enum<?>) eo;
            if ( e.name().equals( vs ) ) {
              return e;
            }
          }
          throw new KettleValueException( "Unknown value " + vs + " for enum " + destinationType );
        }
        break;
      case ValueMetaInterface.TYPE_BOOLEAN:
        Boolean vb = rowMeta.getBoolean( data, idx );
        if ( vb == null ) {
          return null;
        }
        if ( boolean.class.isAssignableFrom( destinationType ) || Boolean.class.isAssignableFrom( destinationType ) ) {
          return vb;
        } else if ( int.class.isAssignableFrom( destinationType ) || Integer.class.isAssignableFrom(
            destinationType ) ) {
          return vb ? 1 : 0;
        } else if ( long.class.isAssignableFrom( destinationType ) || Long.class.isAssignableFrom( destinationType ) ) {
          return vb ? 1L : 0L;
        } else if ( String.class.isAssignableFrom( destinationType ) ) {
          return vb ? "Y" : "N";
        }
        break;
      case ValueMetaInterface.TYPE_INTEGER:
        Long vi = rowMeta.getInteger( data, idx );
        if ( vi == null ) {
          return null;
        }
        if ( long.class.isAssignableFrom( destinationType ) || Long.class.isAssignableFrom( destinationType ) ) {
          return vi;
        } else if ( int.class.isAssignableFrom( destinationType ) || Integer.class.isAssignableFrom(
            destinationType ) ) {
          return vi.intValue();
        } else if ( String.class.isAssignableFrom( destinationType ) ) {
          return vi.toString();
        } else if ( boolean.class.isAssignableFrom( destinationType ) || Boolean.class.isAssignableFrom(
            destinationType ) ) {
          return vi.longValue() != 0;
        }
        break;
      case ValueMetaInterface.TYPE_NUMBER:
        Double vn = rowMeta.getNumber( data, idx );
        if ( vn == null ) {
          return null;
        }
        if ( double.class.isAssignableFrom( destinationType ) || Double.class.isAssignableFrom( destinationType ) ) {
          return vn.doubleValue();
        } else if ( long.class.isAssignableFrom( destinationType ) || Long.class.isAssignableFrom( destinationType ) ) {
          return vn;
        } else if ( int.class.isAssignableFrom( destinationType ) || Integer.class.isAssignableFrom(
            destinationType ) ) {
          return vn.intValue();
        } else if ( String.class.isAssignableFrom( destinationType ) ) {
          return vn.toString();
        } else if ( boolean.class.isAssignableFrom( destinationType ) || Boolean.class.isAssignableFrom(
            destinationType ) ) {
          return vn.longValue() != 0;
        }
        break;
    }

    throw new KettleValueException( "Unknown conversion from " + metaType.getTypeDesc() + " into " + destinationType );
  }

  public void removeValue( String valueName ) throws KettleValueException {
    int index = rowMeta.indexOfValue( valueName );
    if ( index < 0 ) {
      throw new KettleValueException( "Unable to find '" + valueName + "' in the row" );
    }
    removeValue( index );
  }

  public synchronized void removeValue( int index ) {
    rowMeta.removeValueMeta( index );
    data = RowDataUtil.removeItem( data, index );
  }
}
