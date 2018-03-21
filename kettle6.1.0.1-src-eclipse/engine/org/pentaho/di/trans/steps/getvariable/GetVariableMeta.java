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

package org.pentaho.di.trans.steps.getvariable;

import java.util.List;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/*
 * Created on 05-aug-2003
 */
public class GetVariableMeta extends BaseStepMeta implements StepMetaInterface {
  private static Class<?> PKG = GetVariableMeta.class; // for i18n purposes, needed by Translator2!!

  private String[] fieldName;
  private String[] variableString;

  private int[] fieldType;

  private String[] fieldFormat;
  private int[] fieldLength;
  private int[] fieldPrecision;

  private String[] currency;
  private String[] decimal;
  private String[] group;

  private int[] trimType;

  public GetVariableMeta() {
    super(); // allocate BaseStepMeta
  }

  /**
   * @return Returns the fieldName.
   */
  public String[] getFieldName() {
    return fieldName;
  }

  /**
   * @param fieldName
   *          The fieldName to set.
   */
  public void setFieldName( String[] fieldName ) {
    this.fieldName = fieldName;
  }

  /**
   * @return Returns the strings containing variables.
   */
  public String[] getVariableString() {
    return variableString;
  }

  /**
   * @param variableString
   *          The variable strings to set.
   */
  public void setVariableString( String[] variableString ) {
    this.variableString = variableString;
  }

  public void loadXML( Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore ) throws KettleXMLException {
    readData( stepnode );
  }

  public void allocate( int count ) {
    fieldName = new String[count];
    variableString = new String[count];

    fieldType = new int[count];

    fieldFormat = new String[count];
    fieldLength = new int[count];
    fieldPrecision = new int[count];

    currency = new String[count];
    decimal = new String[count];
    group = new String[count];

    trimType = new int[count];
  }

  public Object clone() {
    GetVariableMeta retval = (GetVariableMeta) super.clone();

    int count = fieldName.length;

    retval.allocate( count );

    System.arraycopy( fieldName, 0, retval.fieldName, 0, count );
    System.arraycopy( variableString, 0, retval.variableString, 0, count );
    System.arraycopy( fieldType, 0, retval.fieldType, 0, count );
    System.arraycopy( fieldFormat, 0, retval.fieldFormat, 0, count );
    System.arraycopy( currency, 0, retval.currency, 0, count );
    System.arraycopy( decimal, 0, retval.decimal, 0, count );
    System.arraycopy( group, 0, retval.group, 0, count );
    System.arraycopy( fieldLength, 0, retval.fieldLength, 0, count );
    System.arraycopy( fieldPrecision, 0, retval.fieldPrecision, 0, count );
    System.arraycopy( trimType, 0, retval.trimType, 0, count );

    return retval;
  }

  private void readData( Node stepnode ) throws KettleXMLException {
    try {
      Node fields = XMLHandler.getSubNode( stepnode, "fields" );
      int count = XMLHandler.countNodes( fields, "field" );

      allocate( count );

      for ( int i = 0; i < count; i++ ) {
        Node fnode = XMLHandler.getSubNodeByNr( fields, "field", i );

        fieldName[i] = XMLHandler.getTagValue( fnode, "name" );
        variableString[i] = XMLHandler.getTagValue( fnode, "variable" );
        fieldType[i] = ValueMeta.getType( XMLHandler.getTagValue( fnode, "type" ) );
        fieldFormat[i] = XMLHandler.getTagValue( fnode, "format" );
        currency[i] = XMLHandler.getTagValue( fnode, "currency" );
        decimal[i] = XMLHandler.getTagValue( fnode, "decimal" );
        group[i] = XMLHandler.getTagValue( fnode, "group" );
        fieldLength[i] = Const.toInt( XMLHandler.getTagValue( fnode, "length" ), -1 );
        fieldPrecision[i] = Const.toInt( XMLHandler.getTagValue( fnode, "precision" ), -1 );
        trimType[i] = ValueMeta.getTrimTypeByCode( XMLHandler.getTagValue( fnode, "trim_type" ) );

        // Backward compatibility
        //
        if ( fieldType[i] == ValueMetaInterface.TYPE_NONE ) {
          fieldType[i] = ValueMetaInterface.TYPE_STRING;
        }
      }
    } catch ( Exception e ) {
      throw new KettleXMLException( "Unable to read step information from XML", e );
    }
  }

  public void setDefault() {
    int count = 0;

    allocate( count );

    for ( int i = 0; i < count; i++ ) {
      fieldName[i] = "field" + i;
      variableString[i] = "";
    }
  }

  public void getFields( RowMetaInterface inputRowMeta, String name, RowMetaInterface[] info, StepMeta nextStep,
    VariableSpace space, Repository repository, IMetaStore metaStore ) throws KettleStepException {
    // Determine the maximum length...
    //
    int length = -1;
    for ( int i = 0; i < fieldName.length; i++ ) {
      if ( variableString[i] != null ) {
        String string = space.environmentSubstitute( variableString[i] );
        if ( string.length() > length ) {
          length = string.length();
        }
      }
    }

    RowMetaInterface row = new RowMeta();
    for ( int i = 0; i < fieldName.length; i++ ) {
      ValueMetaInterface v = new ValueMeta( fieldName[i], fieldType[i] );
      if ( fieldLength[i] < 0 ) {
        v.setLength( length );
      } else {
        v.setLength( fieldLength[i] );
      }
      if ( fieldPrecision[i] >= 0 ) {
        v.setPrecision( fieldPrecision[i] );
      }
      v.setConversionMask( fieldFormat[i] );
      v.setGroupingSymbol( group[i] );
      v.setDecimalSymbol( decimal[i] );
      v.setCurrencySymbol( currency[i] );
      v.setTrimType( trimType[i] );
      v.setOrigin( name );

      row.addValueMeta( v );
    }

    inputRowMeta.mergeRowMeta( row );
  }

  public String getXML() {
    StringBuffer retval = new StringBuffer( 300 );

    retval.append( "    <fields>" ).append( Const.CR );
    for ( int i = 0; i < fieldName.length; i++ ) {
      if ( fieldName[i] != null && fieldName[i].length() != 0 ) {
        retval.append( "      <field>" ).append( Const.CR );
        retval.append( "        " ).append( XMLHandler.addTagValue( "name", fieldName[i] ) );
        retval.append( "        " ).append( XMLHandler.addTagValue( "variable", variableString[i] ) );
        retval
          .append( "        " ).append( XMLHandler.addTagValue( "type", ValueMeta.getTypeDesc( fieldType[i] ) ) );
        retval.append( "        " ).append( XMLHandler.addTagValue( "format", fieldFormat[i] ) );
        retval.append( "        " ).append( XMLHandler.addTagValue( "currency", currency[i] ) );
        retval.append( "        " ).append( XMLHandler.addTagValue( "decimal", decimal[i] ) );
        retval.append( "        " ).append( XMLHandler.addTagValue( "group", group[i] ) );
        retval.append( "        " ).append( XMLHandler.addTagValue( "length", fieldLength[i] ) );
        retval.append( "        " ).append( XMLHandler.addTagValue( "precision", fieldPrecision[i] ) );
        retval.append( "        " ).append(
          XMLHandler.addTagValue( "trim_type", ValueMeta.getTrimTypeCode( trimType[i] ) ) );

        retval.append( "      </field>" ).append( Const.CR );
      }
    }
    retval.append( "    </fields>" ).append( Const.CR );

    return retval.toString();
  }

  public void readRep( Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases ) throws KettleException {
    try {
      int nrfields = rep.countNrStepAttributes( id_step, "field_name" );

      allocate( nrfields );

      for ( int i = 0; i < nrfields; i++ ) {
        fieldName[i] = rep.getStepAttributeString( id_step, i, "field_name" );
        variableString[i] = rep.getStepAttributeString( id_step, i, "field_variable" );
        fieldType[i] = ValueMeta.getType( rep.getStepAttributeString( id_step, i, "field_type" ) );

        fieldFormat[i] = rep.getStepAttributeString( id_step, i, "field_format" );
        currency[i] = rep.getStepAttributeString( id_step, i, "field_currency" );
        decimal[i] = rep.getStepAttributeString( id_step, i, "field_decimal" );
        group[i] = rep.getStepAttributeString( id_step, i, "field_group" );
        fieldLength[i] = (int) rep.getStepAttributeInteger( id_step, i, "field_length" );
        fieldPrecision[i] = (int) rep.getStepAttributeInteger( id_step, i, "field_precision" );
        trimType[i] = ValueMeta.getTrimTypeByCode( rep.getStepAttributeString( id_step, i, "field_trim_type" ) );

        // Backward compatibility
        //
        if ( fieldType[i] == ValueMetaInterface.TYPE_NONE ) {
          fieldType[i] = ValueMetaInterface.TYPE_STRING;
        }
      }

    } catch ( Exception e ) {
      throw new KettleException( "Unexpected error reading step information from the repository", e );
    }
  }

  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step ) throws KettleException {
    try {

      for ( int i = 0; i < fieldName.length; i++ ) {
        if ( fieldName[i] != null && fieldName[i].length() != 0 ) {
          rep.saveStepAttribute( id_transformation, id_step, i, "field_name", fieldName[i] );
          rep.saveStepAttribute( id_transformation, id_step, i, "field_variable", variableString[i] );
          rep
            .saveStepAttribute( id_transformation, id_step, i, "field_type", ValueMeta
              .getTypeDesc( fieldType[i] ) );
          rep.saveStepAttribute( id_transformation, id_step, i, "field_format", fieldFormat[i] );
          rep.saveStepAttribute( id_transformation, id_step, i, "field_currency", currency[i] );
          rep.saveStepAttribute( id_transformation, id_step, i, "field_decimal", decimal[i] );
          rep.saveStepAttribute( id_transformation, id_step, i, "field_group", group[i] );
          rep.saveStepAttribute( id_transformation, id_step, i, "field_length", fieldLength[i] );
          rep.saveStepAttribute( id_transformation, id_step, i, "field_precision", fieldPrecision[i] );
          rep.saveStepAttribute( id_transformation, id_step, i, "field_trim_type", ValueMeta
            .getTrimTypeCode( trimType[i] ) );
        }
      }
    } catch ( Exception e ) {
      throw new KettleException( "Unable to save step information to the repository for id_step=" + id_step, e );
    }

  }

  public void check( List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
    RowMetaInterface prev, String[] input, String[] output, RowMetaInterface info, VariableSpace space,
    Repository repository, IMetaStore metaStore ) {
    // See if we have input streams leading to this step!
    int nrRemarks = remarks.size();
    for ( int i = 0; i < fieldName.length; i++ ) {
      if ( Const.isEmpty( variableString[i] ) ) {
        CheckResult cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(
            PKG, "GetVariableMeta.CheckResult.VariableNotSpecified", fieldName[i] ), stepMeta );
        remarks.add( cr );
      }
    }
    if ( remarks.size() == nrRemarks ) {
      CheckResult cr =
        new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "GetVariableMeta.CheckResult.AllVariablesSpecified" ), stepMeta );
      remarks.add( cr );
    }
  }

  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr,
    TransMeta transMeta, Trans trans ) {
    return new GetVariable( stepMeta, stepDataInterface, cnr, transMeta, trans );
  }

  public StepDataInterface getStepData() {
    return new GetVariableData();
  }

  /**
   * @return the field type (ValueMetaInterface.TYPE_*)
   */
  public int[] getFieldType() {
    return fieldType;
  }

  /**
   * @param fieldType
   *          the field type to set (ValueMetaInterface.TYPE_*)
   */
  public void setFieldType( int[] fieldType ) {
    this.fieldType = fieldType;
  }

  /**
   * @return the fieldFormat
   */
  public String[] getFieldFormat() {
    return fieldFormat;
  }

  /**
   * @param fieldFormat
   *          the fieldFormat to set
   */
  public void setFieldFormat( String[] fieldFormat ) {
    this.fieldFormat = fieldFormat;
  }

  /**
   * @return the fieldLength
   */
  public int[] getFieldLength() {
    return fieldLength;
  }

  /**
   * @param fieldLength
   *          the fieldLength to set
   */
  public void setFieldLength( int[] fieldLength ) {
    this.fieldLength = fieldLength;
  }

  /**
   * @return the fieldPrecision
   */
  public int[] getFieldPrecision() {
    return fieldPrecision;
  }

  /**
   * @param fieldPrecision
   *          the fieldPrecision to set
   */
  public void setFieldPrecision( int[] fieldPrecision ) {
    this.fieldPrecision = fieldPrecision;
  }

  /**
   * @return the currency
   */
  public String[] getCurrency() {
    return currency;
  }

  /**
   * @param currency
   *          the currency to set
   */
  public void setCurrency( String[] currency ) {
    this.currency = currency;
  }

  /**
   * @return the decimal
   */
  public String[] getDecimal() {
    return decimal;
  }

  /**
   * @param decimal
   *          the decimal to set
   */
  public void setDecimal( String[] decimal ) {
    this.decimal = decimal;
  }

  /**
   * @return the group
   */
  public String[] getGroup() {
    return group;
  }

  /**
   * @param group
   *          the group to set
   */
  public void setGroup( String[] group ) {
    this.group = group;
  }

  /**
   * @return the trimType
   */
  public int[] getTrimType() {
    return trimType;
  }

  /**
   * @param trimType
   *          the trimType to set
   */
  public void setTrimType( int[] trimType ) {
    this.trimType = trimType;
  }
}
