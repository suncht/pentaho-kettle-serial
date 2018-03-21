package org.pentaho.di.core.row.value;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.Date;

import org.pentaho.di.compatibility.Value;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.database.PostgreSQLDatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.ValueMetaInterface;

public class ValueMetaInternetAddress extends ValueMetaDate {
	
	public ValueMetaInternetAddress() {
		this(null);
	}
	
	public ValueMetaInternetAddress(String name) {
		super(name, ValueMetaInterface.TYPE_INET);
	}
	
	public InetAddress getInternetAddress(Object object) throws KettleValueException {
	  if (object==null) {
	    return null;
	  }
	  
    switch (type) {
    case TYPE_INET:
      switch (storageType) {
      case STORAGE_TYPE_NORMAL:
        return (InetAddress) object;
      case STORAGE_TYPE_BINARY_STRING:
        return (InetAddress) convertBinaryStringToNativeType((byte[]) object);
      case STORAGE_TYPE_INDEXED:
        return (InetAddress) index[((Integer) object).intValue()];
      default:
        throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
      }
    case TYPE_STRING:
      switch (storageType) {
      case STORAGE_TYPE_NORMAL:
        return convertStringToInternetAddress((String) object);
      case STORAGE_TYPE_BINARY_STRING:
        return convertStringToInternetAddress((String) convertBinaryStringToNativeType((byte[]) object));
      case STORAGE_TYPE_INDEXED:
        return convertStringToInternetAddress((String) index[((Integer) object).intValue()]);
      default:
        throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
      }
    case TYPE_NUMBER:
      switch (storageType) {
      case STORAGE_TYPE_NORMAL:
        return convertNumberToInternetAddress((Double) object);
      case STORAGE_TYPE_BINARY_STRING:
        return convertNumberToInternetAddress((Double) convertBinaryStringToNativeType((byte[]) object));
      case STORAGE_TYPE_INDEXED:
        return convertNumberToInternetAddress((Double) index[((Integer) object).intValue()]);
      default:
        throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
      }
    case TYPE_INTEGER:
      switch (storageType) {
      case STORAGE_TYPE_NORMAL:
        return convertIntegerToInternetAddress((Long) object);
      case STORAGE_TYPE_BINARY_STRING:
        return convertIntegerToInternetAddress((Long) convertBinaryStringToNativeType((byte[]) object));
      case STORAGE_TYPE_INDEXED:
        return convertIntegerToInternetAddress((Long) index[((Integer) object).intValue()]);
      default:
        throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
      }
    case TYPE_BIGNUMBER:
      switch (storageType) {
      case STORAGE_TYPE_NORMAL:
        return convertBigNumberToInternetAddress((BigDecimal) object);
      case STORAGE_TYPE_BINARY_STRING:
        return convertBigNumberToInternetAddress((BigDecimal) convertBinaryStringToNativeType((byte[]) object));
      case STORAGE_TYPE_INDEXED:
        return convertBigNumberToInternetAddress((BigDecimal) index[((Integer) object).intValue()]);
      default:
        throw new KettleValueException(toString() + " : Unknown storage type " + storageType + " specified.");
      }
    case TYPE_BOOLEAN:
      throw new KettleValueException(toString() + " : I don't know how to convert a boolean to a Internet address.");
    case TYPE_BINARY:
      throw new KettleValueException(toString() + " : I don't know how to convert a binary value to Internet address.");
    case TYPE_SERIALIZABLE:
      throw new KettleValueException(toString() + " : I don't know how to convert a serializable value to Internet address.");

    default:
      throw new KettleValueException(toString() + " : Unknown type " + type + " specified.");
    }
	}
	
	@Override
	public Date getDate(Object object) throws KettleValueException {
	   throw new KettleValueException(toStringMeta()+": it's not possible to convert from Internet Address to a date");
	}
	
	@Override
	public Long getInteger(Object object) throws KettleValueException {
	  InetAddress address = getInternetAddress(object);
    if (address==null) {
      return null;
    }
    long total = 0L;
    byte[] addr = address.getAddress();
    
    if (addr.length>8) {
      throw new KettleValueException("Unable to convert Internet Address v6 to an Integer: "+getString(object)+" (The precision is too high to be contained in a long integer value)");
    }
    
    for (int i=0;i<addr.length;i++) {
      total+=(addr[i]&0xFF)*((long)Math.pow(256, (addr.length-1-i)));
    }
    
    return total;
	}

	@Override
  public Double getNumber(Object object) throws KettleValueException {
    Long l = getInteger(object);
    if (l==null) {
      return null;
    }
    
    return Long.valueOf(l).doubleValue();
  }

	@Override
	public BigDecimal getBigNumber(Object object) throws KettleValueException {
    Long l = getInteger(object);
    if (l==null) {
      return null;
    }
    return BigDecimal.valueOf(l);
	}
	 
	@Override
	public Boolean getBoolean(Object object) throws KettleValueException {
	  throw new KettleValueException(toStringMeta()+": it's not possible to convert from an Internet Address to a Boolean");
	}
	
	@Override
	public String getString(Object object) throws KettleValueException {
	  return convertInternetAddressToString(getInternetAddress(object));
	}
	
  protected InetAddress convertBigNumberToInternetAddress(BigDecimal bd) throws KettleValueException {
    if (bd==null) {
      return null;
    }
    return convertIntegerToInternetAddress(bd.longValue());
  }

  protected InetAddress convertNumberToInternetAddress(Double d) throws KettleValueException {
    if (d==null) {
      return null;
    }
    long nanos = d.longValue();
    
    return convertIntegerToInternetAddress(nanos);
  }

  protected InetAddress convertIntegerToInternetAddress(Long l) throws KettleValueException {
    if (l==null) {
      return null;
    }

    byte[] addr;
    if (l>=Math.pow(256, 4)) {
      addr = new byte[16];
    } else {
      addr = new byte[4];
    }
    
    for (int i=0;i<addr.length;i++) {
      long mask = 0xFF<<(i*8);
      addr[addr.length-1-i] = (byte)((l & mask) >> (8*i));
    }
    
    try {
      return InetAddress.getByAddress(addr);
    } catch(Exception e) {
      throw new KettleValueException("Unable to convert an Integer to an internet address", e);
    }
  }

  protected synchronized InetAddress convertStringToInternetAddress(String string) throws KettleValueException {
    // See if trimming needs to be performed before conversion
    //
    string = Const.trimToType(string, getTrimType()); 

    if (Const.isEmpty(string))
      return null;

    try {
      return InetAddress.getByName(string);
    } catch (Exception e) {
      throw new KettleValueException(toString() + " : couldn't convert string [" + string + "] to an internet address", e);
    }
  }

  protected synchronized String convertInternetAddressToString(InetAddress inetAddress) throws KettleValueException {

    if (inetAddress==null) {
      return null;
    }
    
    return inetAddress.getHostAddress();
  }
  
  @Override
  public Object convertDataFromString(String pol, ValueMetaInterface convertMeta, String nullIf, String ifNull, int trim_type) throws KettleValueException {
    // null handling and conversion of value to null
    //
    String null_value = nullIf;
    if (null_value == null) {
      switch (convertMeta.getType()) {
      case Value.VALUE_TYPE_BOOLEAN:
        null_value = Const.NULL_BOOLEAN;
        break;
      case Value.VALUE_TYPE_STRING:
        null_value = Const.NULL_STRING;
        break;
      case Value.VALUE_TYPE_BIGNUMBER:
        null_value = Const.NULL_BIGNUMBER;
        break;
      case Value.VALUE_TYPE_NUMBER:
        null_value = Const.NULL_NUMBER;
        break;
      case Value.VALUE_TYPE_INTEGER:
        null_value = Const.NULL_INTEGER;
        break;
      case Value.VALUE_TYPE_DATE:
        null_value = Const.NULL_DATE;
        break;
      case Value.VALUE_TYPE_BINARY:
        null_value = Const.NULL_BINARY;
        break;
      default:
        null_value = Const.NULL_NONE;
        break;
      }
    }

    // See if we need to convert a null value into a String
    // For example, we might want to convert null into "Empty".
    //
    if (!Const.isEmpty(ifNull)) {
      // Note that you can't pull the pad method up here as a nullComp variable
      // because you could get an NPE since you haven't checked isEmpty(pol)
      // yet!
      if (Const.isEmpty(pol) || pol.equalsIgnoreCase(Const.rightPad(new StringBuffer(null_value), pol.length()))) {
        pol = ifNull;
      }
    }

    // See if the polled value is empty
    // In that case, we have a null value on our hands...
    //
    if (Const.isEmpty(pol)) {
      return null;
    } else {
      // if the null_value is specified, we try to match with that.
      //
      if (!Const.isEmpty(null_value)) {
        if (null_value.length() <= pol.length()) {
          // If the polled value is equal to the spaces right-padded null_value,
          // we have a match
          //
          if (pol.equalsIgnoreCase(Const.rightPad(new StringBuffer(null_value), pol.length()))) {
            return null;
          }
        }
      } else {
        // Verify if there are only spaces in the polled value...
        // We consider that empty as well...
        //
        if (Const.onlySpaces(pol)) {
          return null;
        }
      }
    }

    // Trimming
    switch (trim_type) {
    case ValueMetaInterface.TRIM_TYPE_LEFT: {
      StringBuffer strpol = new StringBuffer(pol);
      while (strpol.length() > 0 && strpol.charAt(0) == ' ')
        strpol.deleteCharAt(0);
      pol = strpol.toString();
    }
      break;
    case ValueMetaInterface.TRIM_TYPE_RIGHT: {
      StringBuffer strpol = new StringBuffer(pol);
      while (strpol.length() > 0 && strpol.charAt(strpol.length() - 1) == ' ')
        strpol.deleteCharAt(strpol.length() - 1);
      pol = strpol.toString();
    }
      break;
    case ValueMetaInterface.TRIM_TYPE_BOTH:
      StringBuffer strpol = new StringBuffer(pol);
      {
        while (strpol.length() > 0 && strpol.charAt(0) == ' ')
          strpol.deleteCharAt(0);
        while (strpol.length() > 0 && strpol.charAt(strpol.length() - 1) == ' ')
          strpol.deleteCharAt(strpol.length() - 1);
        pol = strpol.toString();
      }
      break;
    default:
      break;
    }

    // On with the regular program...
    // Simply call the ValueMeta routines to do the conversion
    // We need to do some effort here: copy all
    //
    return convertData(convertMeta, pol);
  }
    
  /**
   * Convert the specified data to the data type specified in this object.
   * 
   * @param meta2
   *          the metadata of the object to be converted
   * @param data2
   *          the data of the object to be converted
   * @return the object in the data type of this value metadata object
   * @throws KettleValueException
   *           in case there is a data conversion error
   */
  @Override
  public Object convertData(ValueMetaInterface meta2, Object data2) throws KettleValueException {
    switch(meta2.getType()) {
    case TYPE_STRING: return convertStringToInternetAddress(meta2.getString(data2)); 
    case TYPE_INTEGER: return convertIntegerToInternetAddress(meta2.getInteger(data2));
    case TYPE_NUMBER: return convertNumberToInternetAddress(meta2.getNumber(data2));
    case TYPE_BIGNUMBER: return convertBigNumberToInternetAddress(meta2.getBigNumber(data2));
    default: 
      throw new KettleValueException(meta2.toStringMeta()+" : can't be converted to an Internet Address");
    }
  }
  
  @Override
  public Object cloneValueData(Object object) throws KettleValueException {
    InetAddress inetAddress = getInternetAddress(object);
    if (inetAddress==null) return null;

    try {
      return InetAddress.getByAddress(inetAddress.getAddress());
    } catch(Exception e) {
      throw new KettleValueException("Unable to clone Internet Address", e);
    }
  }
  
  @Override
  public ValueMetaInterface getValueFromSQLType(DatabaseMeta databaseMeta, String name, ResultSetMetaData rm, int index, boolean ignoreLength, boolean lazyConversion) throws KettleDatabaseException {

    try {
      int type = rm.getColumnType(index);
      if (type == java.sql.Types.OTHER) {
        
        String columnTypeName = rm.getColumnTypeName(index);
        if ("INET".equalsIgnoreCase(columnTypeName)) {
          
          ValueMetaInternetAddress valueMeta = new ValueMetaInternetAddress(name);

          // Also get original column details, comment, etc.
          //
          getOriginalColumnMetadata(valueMeta, rm, index, ignoreLength);
          return valueMeta;
        }
      }
      return null;
    } catch(Exception e) {
      throw new KettleDatabaseException("Error evaluating Internet address value metadata", e);
    }
  }
  
  @Override
  public Object getValueFromResultSet(DatabaseInterface databaseInterface, ResultSet resultSet, int index)
      throws KettleDatabaseException {

    try {
      
      return convertStringToInternetAddress(resultSet.getString(index+1));
      
    } catch(Exception e) {
      throw new KettleDatabaseException(toStringMeta()+" : Unable to get Internet Address from resultset at index "+index, e);
    }
    
  }
  
  @Override
  public void setPreparedStatementValue(DatabaseMeta databaseMeta, PreparedStatement preparedStatement, int index, Object data) throws KettleDatabaseException {
    
    try {
      
      preparedStatement.setObject(index, getString(data), Types.OTHER);
      
    } catch(Exception e) {
      throw new KettleDatabaseException(toStringMeta()+" : Unable to set Internet address value on prepared statement on index "+index, e);
    }
    
  }
  
  @Override
  public String getDatabaseColumnTypeDefinition(DatabaseInterface databaseInterface, String tk, String pk, boolean use_autoinc, boolean add_fieldname, boolean add_cr) {

    String retval=null;
    if (databaseInterface instanceof PostgreSQLDatabaseMeta) {
      if (add_fieldname) {
        retval=getName()+" ";
      } else {
        retval="";
      }
      retval+="INET";
      if (add_cr) {
        retval+=Const.CR;
      }
    }
    
    return retval;
  }
}
