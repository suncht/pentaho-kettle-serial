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

import java.util.List;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.encryption.Encr;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
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

@Step(id="SalesforceInput", i18nPackageName="org.pentaho.di.trans.steps.salesforceinput", name="SalesforceInput.TypeLongDesc.SalesforceInput", description="SalesforceInput.TypeTooltipDesc.SalesforceInput", categoryDescription="i18n:org.pentaho.di.trans.step:BaseStep.Category.Input", image="SFI.svg", documentationUrl="http://wiki.pentaho.com/display/EAI/SalesForce+Input")
public class SalesforceInputMeta extends BaseStepMeta implements StepMetaInterface {
  public static String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

  private static Class<?> PKG = SalesforceInputMeta.class; // for i18n purposes, needed by Translator2!!

  /** Flag indicating that we should include the generated SQL in the output */
  private boolean includeSQL;

  /** The name of the field in the output containing the generated SQL */
  private String sqlField;

  /** Flag indicating that we should include the server Timestamp in the output */
  private boolean includeTimestamp;

  /** The name of the field in the output containing the server Timestamp */
  private String timestampField;

  /** Flag indicating that we should include the filename in the output */
  private boolean includeTargetURL;

  /** The name of the field in the output containing the filename */
  private String targetURLField;

  /** Flag indicating that we should include the module in the output */
  private boolean includeModule;

  /** The name of the field in the output containing the module */
  private String moduleField;

  /** Flag indicating that a deletion date field should be included in the output */
  private boolean includeDeletionDate;

  /** The name of the field in the output containing the deletion Date */
  private String deletionDateField;

  /** Flag indicating that a row number field should be included in the output */
  private boolean includeRowNumber;

  /** The name of the field in the output containing the row number */
  private String rowNumberField;

  /** The salesforce url */
  private String targeturl;

  /** The userName */
  private String username;

  /** The password */
  private String password;

  /** The module */
  private String module;

  /** The condition */
  private String condition;

  /** The time out */
  private String timeout;

  /** The maximum number or lines to read */
  private String rowLimit;

  /** The fields to return... */
  private SalesforceInputField[] inputFields;

  /** option: specify query **/
  private boolean specifyQuery;

  private boolean useCompression;

  // ** query entered by user **/
  private String query;

  private int nrFields;

  private String readTo;
  private String readFrom;

  /** records filter */
  private int recordsFilter;

  /** Query all records including deleted ones **/
  private boolean queryAll;

  public SalesforceInputMeta() {
    super(); // allocate BaseStepMeta
  }

  /**
   * @return Returns the input fields.
   */
  public SalesforceInputField[] getInputFields() {
    return inputFields;
  }

  /**
   * @param inputFields
   *          The input fields to set.
   */
  public void setInputFields( SalesforceInputField[] inputFields ) {
    this.inputFields = inputFields;
  }

  /**
   * @return Returns the UserName.
   */
  public String getUserName() {
    return username;
  }

  /**
   * @param user_name
   *          The UserNAme to set.
   */
  public void setUserName( String user_name ) {
    this.username = user_name;
  }

  /**
   * @return Returns the Password.
   */
  public String getPassword() {
    return password;
  }

  /**
   * @param passwd
   *          The password to set.
   */
  public void setPassword( String passwd ) {
    this.password = passwd;
  }

  /**
   * @return Returns the module.
   */
  public String getModule() {
    return module;
  }

  /**
   * @param module
   *          The module to set.
   */
  public void setModule( String module ) {
    this.module = module;
  }

  /**
   * @return Returns the query.
   */
  public String getQuery() {
    return query;
  }

  /**
   * @param query
   *          The query to set.
   */
  public void setQuery( String query ) {
    this.query = query;
  }

  /**
   * @return Returns the specifyQuery.
   */
  public boolean isSpecifyQuery() {
    return specifyQuery;
  }

  /**
   * @param specifyQuery
   *          The specifyQuery to set.
   */
  public void setSpecifyQuery( boolean specifyQuery ) {
    this.specifyQuery = specifyQuery;
  }

  /**
   * @return Returns the useCompression.
   */
  public boolean isUsingCompression() {
    return useCompression;
  }

  /**
   * @param useCompression
   *          The useCompression to set.
   */
  public void setUseCompression( boolean useCompression ) {
    this.useCompression = useCompression;
  }

  /**
   * @return Returns the queryAll.
   */
  public boolean isQueryAll() {
    return queryAll;
  }

  /**
   * @param queryAll
   *          The queryAll to set.
   */
  public void setQueryAll( boolean value ) {
    this.queryAll = value;
  }

  /**
   * @return Returns the condition.
   */
  public String getCondition() {
    return condition;
  }

  /**
   * @param condition
   *          The condition to set.
   */
  public void setCondition( String condition ) {
    this.condition = condition;
  }

  /**
   * @return Returns the targeturl.
   */
  public String getTargetURL() {
    return targeturl;
  }

  /**
   * @param url
   *          The url to set.
   */
  public void setTargetURL( String urlvalue ) {
    this.targeturl = urlvalue;
  }

  /**
   * @param TargetURLField
   *          The TargetURLField to set.
   */
  public void setTargetURLField( String TargetURLField ) {
    this.targetURLField = TargetURLField;
  }

  /**
   * @param sqlField
   *          The sqlField to set.
   */
  public void setSQLField( String sqlField ) {
    this.sqlField = sqlField;
  }

  /**
   * @param timestampField
   *          The timestampField to set.
   */
  public void setTimestampField( String timestampField ) {
    this.timestampField = timestampField;
  }

  /**
   * @param ModuleField
   *          The ModuleField to set.
   */
  public void setModuleField( String module_field ) {
    this.moduleField = module_field;
  }

  public int getRecordsFilter() {
    return recordsFilter;
  }

  public void setRecordsFilter( int recordsFilter ) {
    this.recordsFilter = recordsFilter;
  }

  /**
   * @return Returns the includeTargetURL.
   */
  public boolean includeTargetURL() {
    return includeTargetURL;
  }

  /**
   * @return Returns the includeSQL.
   */
  public boolean includeSQL() {
    return includeSQL;
  }

  /**
   * @param includeSQL
   *          to set.
   */
  public void setIncludeSQL( boolean includeSQL ) {
    this.includeSQL = includeSQL;
  }

  /**
   * @return Returns the includeTimestamp.
   */
  public boolean includeTimestamp() {
    return includeTimestamp;
  }

  /**
   * @param includeTimestamp
   *          to set.
   */
  public void setIncludeTimestamp( boolean includeTimestamp ) {
    this.includeTimestamp = includeTimestamp;
  }

  /**
   * @return Returns the includeModule.
   */
  public boolean includeModule() {
    return includeTargetURL;
  }

  /**
   * @param includeTargetURL
   *          The includeTargetURL to set.
   */
  public void setIncludeTargetURL( boolean includeTargetURL ) {
    this.includeTargetURL = includeTargetURL;
  }

  /**
   * @param includeModule
   *          The includeModule to set.
   */
  public void setIncludeModule( boolean includemodule ) {
    this.includeModule = includemodule;
  }

  /**
   * @return Returns the includeRowNumber.
   */
  public boolean includeRowNumber() {
    return includeRowNumber;
  }

  /**
   * @param includeRowNumber
   *          The includeRowNumber to set.
   */
  public void setIncludeRowNumber( boolean includeRowNumber ) {
    this.includeRowNumber = includeRowNumber;
  }

  /**
   * @return Returns the includeDeletionDate.
   */
  public boolean includeDeletionDate() {
    return includeDeletionDate;
  }

  /**
   * @param includeDeletionDate
   *          The includeDeletionDate to set.
   */
  public void setIncludeDeletionDate( boolean includeDeletionDate ) {
    this.includeDeletionDate = includeDeletionDate;
  }

  /**
   * @return Returns the rowLimit.
   */
  public String getRowLimit() {
    return rowLimit;
  }

  /**
   * @return Returns the TimeOut.
   */
  public String getTimeOut() {
    return timeout;
  }

  /**
   * @param rowLimit
   *          The rowLimit to set.
   */
  public void setRowLimit( String rowLimit ) {
    this.rowLimit = rowLimit;
  }

  /**
   * @param TimeOut
   *          The TimeOut to set.
   */
  public void setTimeOut( String TimeOut ) {
    this.timeout = TimeOut;
  }

  /**
   * @return Returns the rowNumberField.
   */
  public String getRowNumberField() {
    return rowNumberField;
  }

  /**
   * @return Returns the deletionDateField.
   */
  public String getDeletionDateField() {
    return deletionDateField;
  }

  /**
   * @param value
   *          the deletionDateField to set.
   */
  public void setDeletionDateField( String value ) {
    this.deletionDateField = value;
  }

  /**
   * @return Returns the targetURLField.
   */
  public String getTargetURLField() {
    return targetURLField;
  }

  /**
   * @return Returns the readFrom.
   */
  public String getReadFrom() {
    return readFrom;
  }

  /**
   * @param readFrom
   *          the readFrom to set.
   */
  public void setReadFrom( String readFrom ) {
    this.readFrom = readFrom;
  }

  /**
   * @return Returns the readTo.
   */
  public String getReadTo() {
    return readTo;
  }

  /**
   * @param readTo
   *          the readTo to set.
   */
  public void setReadTo( String readTo ) {
    this.readTo = readTo;
  }

  /**
   * @return Returns the sqlField.
   */
  public String getSQLField() {
    return sqlField;
  }

  /**
   * @return Returns the timestampField.
   */
  public String getTimestampField() {
    return timestampField;
  }

  /**
   * @return Returns the moduleField.
   */
  public String getModuleField() {
    return moduleField;
  }

  /**
   * @param rowNumberField
   *          The rowNumberField to set.
   */
  public void setRowNumberField( String rowNumberField ) {
    this.rowNumberField = rowNumberField;
  }

  public void loadXML( Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore ) throws KettleXMLException {
    readData( stepnode );
  }

  public Object clone() {
    SalesforceInputMeta retval = (SalesforceInputMeta) super.clone();

    int nrFields = inputFields.length;

    retval.allocate( nrFields );

    for ( int i = 0; i < nrFields; i++ ) {
      if ( inputFields[i] != null ) {
        retval.inputFields[i] = (SalesforceInputField) inputFields[i].clone();
      }
    }

    return retval;
  }

  public String getXML() {
    StringBuffer retval = new StringBuffer();
    retval.append( "    " + XMLHandler.addTagValue( "targeturl", targeturl ) );
    retval.append( "    " + XMLHandler.addTagValue( "username", username ) );
    retval.append( "    "
      + XMLHandler.addTagValue( "password", Encr.encryptPasswordIfNotUsingVariables( password ), false ) );
    retval.append( "    " + XMLHandler.addTagValue( "module", module ) );
    retval.append( "    " + XMLHandler.addTagValue( "condition", condition ) );
    retval.append( "    " + XMLHandler.addTagValue( "specifyQuery", specifyQuery ) );
    retval.append( "    " + XMLHandler.addTagValue( "query", query ) );
    retval.append( "    " + XMLHandler.addTagValue( "include_targeturl", includeTargetURL ) );
    retval.append( "    " + XMLHandler.addTagValue( "targeturl_field", targetURLField ) );
    retval.append( "    " + XMLHandler.addTagValue( "include_module", includeModule ) );
    retval.append( "    " + XMLHandler.addTagValue( "module_field", moduleField ) );
    retval.append( "    " + XMLHandler.addTagValue( "include_rownum", includeRowNumber ) );
    retval.append( "    " + XMLHandler.addTagValue( "include_deletion_date", includeDeletionDate ) );

    retval.append( "    " + XMLHandler.addTagValue( "deletion_date_field", deletionDateField ) );
    retval.append( "    " + XMLHandler.addTagValue( "rownum_field", rowNumberField ) );
    retval.append( "    " + XMLHandler.addTagValue( "include_sql", includeSQL ) );
    retval.append( "    " + XMLHandler.addTagValue( "sql_field", sqlField ) );
    retval.append( "    " + XMLHandler.addTagValue( "include_Timestamp", includeTimestamp ) );
    retval.append( "    " + XMLHandler.addTagValue( "timestamp_field", timestampField ) );
    retval.append( "    " + XMLHandler.addTagValue( "read_from", readFrom ) );
    retval.append( "    " + XMLHandler.addTagValue( "read_to", readTo ) );
    retval.append( "    "
      + XMLHandler
        .addTagValue( "records_filter", SalesforceConnectionUtils.getRecordsFilterCode( recordsFilter ) ) );
    retval.append( "    " + XMLHandler.addTagValue( "useCompression", useCompression ) );
    retval.append( "    " + XMLHandler.addTagValue( "queryAll", queryAll ) );

    retval.append( "    <fields>" + Const.CR );
    for ( int i = 0; i < inputFields.length; i++ ) {
      SalesforceInputField field = inputFields[i];
      retval.append( field.getXML() );
    }
    retval.append( "      </fields>" + Const.CR );
    retval.append( "    " + XMLHandler.addTagValue( "limit", rowLimit ) );
    retval.append( "    " + XMLHandler.addTagValue( "timeout", timeout ) );

    return retval.toString();
  }

  private void readData( Node stepnode ) throws KettleXMLException {
    try {
      targeturl = XMLHandler.getTagValue( stepnode, "targeturl" );
      username = XMLHandler.getTagValue( stepnode, "username" );
      password = XMLHandler.getTagValue( stepnode, "password" );
      if ( password != null && password.startsWith( "Encrypted" ) ) {
        password = Encr.decryptPassword( password.replace( "Encrypted", "" ).replace( " ", "" ) );
      }

      module = XMLHandler.getTagValue( stepnode, "module" );
      condition = XMLHandler.getTagValue( stepnode, "condition" );
      query = XMLHandler.getTagValue( stepnode, "query" );
      specifyQuery = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "specifyQuery" ) );
      includeTargetURL = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "include_targeturl" ) );
      targetURLField = XMLHandler.getTagValue( stepnode, "targeturl_field" );
      includeModule = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "include_module" ) );
      moduleField = XMLHandler.getTagValue( stepnode, "module_field" );
      includeRowNumber = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "include_rownum" ) );
      includeDeletionDate = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "include_deletion_date" ) );
      rowNumberField = XMLHandler.getTagValue( stepnode, "rownum_field" );
      deletionDateField = XMLHandler.getTagValue( stepnode, "deletion_date_field" );

      includeSQL = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "include_sql" ) );
      sqlField = XMLHandler.getTagValue( stepnode, "targetsql_field" );
      includeTimestamp = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "include_Timestamp" ) );
      timestampField = XMLHandler.getTagValue( stepnode, "timestamp_field" );
      readFrom = XMLHandler.getTagValue( stepnode, "read_from" );
      readTo = XMLHandler.getTagValue( stepnode, "read_to" );
      recordsFilter =
        SalesforceConnectionUtils.getRecordsFilterByCode( Const.NVL( XMLHandler.getTagValue(
          stepnode, "records_filter" ), "" ) );
      useCompression = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "useCompression" ) );
      queryAll = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "queryAll" ) );

      Node fields = XMLHandler.getSubNode( stepnode, "fields" );
      int nrFields = XMLHandler.countNodes( fields, "field" );

      allocate( nrFields );

      for ( int i = 0; i < nrFields; i++ ) {
        Node fnode = XMLHandler.getSubNodeByNr( fields, "field", i );
        SalesforceInputField field = new SalesforceInputField( fnode );
        inputFields[i] = field;
      }
      timeout = XMLHandler.getTagValue( stepnode, "timeout" );
      // Is there a limit on the number of rows we process?
      rowLimit = XMLHandler.getTagValue( stepnode, "limit" );
    } catch ( Exception e ) {
      throw new KettleXMLException( "Unable to load step info from XML", e );
    }
  }

  public void allocate( int nrfields ) {
    inputFields = new SalesforceInputField[nrfields];
  }

  public int getNrFields() {
    return nrFields;
  }

  public void setDefault() {
    includeDeletionDate = false;
    queryAll = false;
    readFrom = "";
    readTo = "";
    nrFields = 0;
    specifyQuery = false;
    query = "";
    targeturl = SalesforceConnectionUtils.TARGET_DEFAULT_URL;
    password = "";
    module = "Account";
    condition = "";
    includeTargetURL = false;
    targetURLField = "";
    includeModule = false;
    moduleField = "";
    includeRowNumber = false;
    rowNumberField = "";
    deletionDateField = "";
    useCompression = false;
    includeSQL = false;
    sqlField = "";
    includeTimestamp = false;
    timestampField = "";
    int nrFields = 0;
    allocate( nrFields );

    for ( int i = 0; i < nrFields; i++ ) {
      inputFields[i] = new SalesforceInputField( "field" + ( i + 1 ) );
    }

    rowLimit = "0";
    timeout = "60000";
  }

  public void getFields( RowMetaInterface r, String name, RowMetaInterface[] info, StepMeta nextStep,
    VariableSpace space, Repository repository, IMetaStore metaStore ) throws KettleStepException {
    int i;
    for ( i = 0; i < inputFields.length; i++ ) {
      SalesforceInputField field = inputFields[i];

      int type = field.getType();
      if ( type == ValueMeta.TYPE_NONE ) {
        type = ValueMeta.TYPE_STRING;
      }
      try {
        ValueMetaInterface v =
          ValueMetaFactory.createValueMeta( space.environmentSubstitute( field.getName() ), type );
        v.setLength( field.getLength() );
        v.setPrecision( field.getPrecision() );
        v.setOrigin( name );
        v.setConversionMask( field.getFormat() );
        v.setDecimalSymbol( field.getDecimalSymbol() );
        v.setGroupingSymbol( field.getGroupSymbol() );
        v.setCurrencySymbol( field.getCurrencySymbol() );
        r.addValueMeta( v );
      } catch ( Exception e ) {
        throw new KettleStepException( e );
      }
    }

    if ( includeTargetURL && !Const.isEmpty( targetURLField ) ) {
      ValueMetaInterface v = new ValueMeta( space.environmentSubstitute( targetURLField ), ValueMeta.TYPE_STRING );
      v.setLength( 250 );
      v.setPrecision( -1 );
      v.setOrigin( name );
      r.addValueMeta( v );
    }
    if ( includeModule && !Const.isEmpty( moduleField ) ) {
      ValueMetaInterface v = new ValueMeta( space.environmentSubstitute( moduleField ), ValueMeta.TYPE_STRING );
      v.setLength( 250 );
      v.setPrecision( -1 );
      v.setOrigin( name );
      r.addValueMeta( v );
    }

    if ( includeSQL && !Const.isEmpty( sqlField ) ) {
      ValueMetaInterface v = new ValueMeta( space.environmentSubstitute( sqlField ), ValueMeta.TYPE_STRING );
      v.setLength( 250 );
      v.setPrecision( -1 );
      v.setOrigin( name );
      r.addValueMeta( v );
    }
    if ( includeTimestamp && !Const.isEmpty( timestampField ) ) {
      ValueMetaInterface v = new ValueMeta( space.environmentSubstitute( timestampField ), ValueMeta.TYPE_DATE );
      v.setOrigin( name );
      r.addValueMeta( v );
    }

    if ( includeRowNumber && !Const.isEmpty( rowNumberField ) ) {
      ValueMetaInterface v = new ValueMeta( space.environmentSubstitute( rowNumberField ), ValueMeta.TYPE_INTEGER );
      v.setLength( ValueMetaInterface.DEFAULT_INTEGER_LENGTH, 0 );
      v.setOrigin( name );
      r.addValueMeta( v );
    }
    if ( includeDeletionDate && !Const.isEmpty( deletionDateField ) ) {
      ValueMetaInterface v = new ValueMeta( space.environmentSubstitute( deletionDateField ), ValueMeta.TYPE_DATE );
      v.setOrigin( name );
      r.addValueMeta( v );
    }

  }

  public void readRep( Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases ) throws KettleException {
    try {
      targeturl = rep.getStepAttributeString( id_step, "targeturl" );

      // H.kawaguchi Add 19-01-2009
      username = rep.getStepAttributeString( id_step, "username" );
      password = rep.getStepAttributeString( id_step, "password" );
      if ( password != null && password.startsWith( "Encrypted" ) ) {
        password = Encr.decryptPassword( password.replace( "Encrypted", "" ).replace( " ", "" ) );
      }
      // H.kawaguchi Add 19-01-2009

      module = rep.getStepAttributeString( id_step, "module" );

      // H.kawaguchi Add 19-01-2009
      condition = rep.getStepAttributeString( id_step, "condition" );
      // H.kawaguchi Add 19-01-2009
      query = rep.getStepAttributeString( id_step, "query" );
      specifyQuery = rep.getStepAttributeBoolean( id_step, "specifyQuery" );
      includeTargetURL = rep.getStepAttributeBoolean( id_step, "include_targeturl" );
      targetURLField = rep.getStepAttributeString( id_step, "targeturl_field" );
      includeModule = rep.getStepAttributeBoolean( id_step, "include_module" );
      moduleField = rep.getStepAttributeString( id_step, "module_field" );
      includeRowNumber = rep.getStepAttributeBoolean( id_step, "include_rownum" );
      includeDeletionDate = rep.getStepAttributeBoolean( id_step, "include_deletion_date" );
      rowNumberField = rep.getStepAttributeString( id_step, "rownum_field" );
      deletionDateField = rep.getStepAttributeString( id_step, "deletion_date_field" );
      includeSQL = rep.getStepAttributeBoolean( id_step, "include_sql" );
      sqlField = rep.getStepAttributeString( id_step, "sql_field" );
      includeTimestamp = rep.getStepAttributeBoolean( id_step, "include_Timestamp" );
      timestampField = rep.getStepAttributeString( id_step, "timestamp_field" );
      rowLimit = rep.getStepAttributeString( id_step, "limit" );
      timeout = rep.getStepAttributeString( id_step, "timeout" );
      readFrom = rep.getStepAttributeString( id_step, "read_from" );
      readTo = rep.getStepAttributeString( id_step, "read_to" );
      recordsFilter =
        SalesforceConnectionUtils.getRecordsFilterByCode( Const.NVL( rep.getStepAttributeString(
          id_step, "records_filter" ), "" ) );
      useCompression = rep.getStepAttributeBoolean( id_step, "useCompression" );
      queryAll = rep.getStepAttributeBoolean( id_step, "queryAll" );

      int nrFields = rep.countNrStepAttributes( id_step, "field_name" );

      allocate( nrFields );

      for ( int i = 0; i < nrFields; i++ ) {
        SalesforceInputField field = new SalesforceInputField();

        field.setName( rep.getStepAttributeString( id_step, i, "field_name" ) );
        field.setField( rep.getStepAttributeString( id_step, i, "field_attribut" ) );
        field.setIdLookup( rep.getStepAttributeBoolean( id_step, i, "field_idlookup" ) );
        field.setType( ValueMeta.getType( rep.getStepAttributeString( id_step, i, "field_type" ) ) );
        field.setFormat( rep.getStepAttributeString( id_step, i, "field_format" ) );
        field.setCurrencySymbol( rep.getStepAttributeString( id_step, i, "field_currency" ) );
        field.setDecimalSymbol( rep.getStepAttributeString( id_step, i, "field_decimal" ) );
        field.setGroupSymbol( rep.getStepAttributeString( id_step, i, "field_group" ) );
        field.setLength( (int) rep.getStepAttributeInteger( id_step, i, "field_length" ) );
        field.setPrecision( (int) rep.getStepAttributeInteger( id_step, i, "field_precision" ) );
        field.setTrimType( SalesforceInputField.getTrimTypeByCode( rep.getStepAttributeString(
          id_step, i, "field_trim_type" ) ) );
        field.setRepeated( rep.getStepAttributeBoolean( id_step, i, "field_repeat" ) );
        inputFields[i] = field;
      }
    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString(
        PKG, "SalesforceInputMeta.Exception.ErrorReadingRepository" ), e );
    }
  }

  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step ) throws KettleException {
    try {
      rep.saveStepAttribute( id_transformation, id_step, "targeturl", targeturl );

      // H.kawaguchi Add 19-01-2009
      rep.saveStepAttribute( id_transformation, id_step, "username", username );
      rep.saveStepAttribute( id_transformation, id_step, "password", Encr
        .encryptPasswordIfNotUsingVariables( password ) );
      // H.kawaguchi Add 19-01-2009

      rep.saveStepAttribute( id_transformation, id_step, "module", module );

      // H.kawaguchi Add 19-01-2009
      rep.saveStepAttribute( id_transformation, id_step, "condition", condition );
      // H.kawaguchi Add 19-01-2009
      rep.saveStepAttribute( id_transformation, id_step, "query", query );
      rep.saveStepAttribute( id_transformation, id_step, "specifyQuery", specifyQuery );

      rep.saveStepAttribute( id_transformation, id_step, "include_targeturl", includeTargetURL );
      rep.saveStepAttribute( id_transformation, id_step, "targeturl_field", targetURLField );
      rep.saveStepAttribute( id_transformation, id_step, "include_module", includeModule );
      rep.saveStepAttribute( id_transformation, id_step, "module_field", moduleField );
      rep.saveStepAttribute( id_transformation, id_step, "include_rownum", includeRowNumber );
      rep.saveStepAttribute( id_transformation, id_step, "include_deletion_date", includeDeletionDate );

      rep.saveStepAttribute( id_transformation, id_step, "include_sql", includeSQL );
      rep.saveStepAttribute( id_transformation, id_step, "sql_field", sqlField );
      rep.saveStepAttribute( id_transformation, id_step, "include_Timestamp", includeTimestamp );
      rep.saveStepAttribute( id_transformation, id_step, "timestamp_field", timestampField );
      rep.saveStepAttribute( id_transformation, id_step, "rownum_field", rowNumberField );
      rep.saveStepAttribute( id_transformation, id_step, "deletion_date_field", deletionDateField );

      rep.saveStepAttribute( id_transformation, id_step, "limit", rowLimit );
      rep.saveStepAttribute( id_transformation, id_step, "timeout", timeout );
      rep.saveStepAttribute( id_transformation, id_step, "read_from", readFrom );
      rep.saveStepAttribute( id_transformation, id_step, "read_to", readTo );
      rep.saveStepAttribute( id_transformation, id_step, "records_filter", SalesforceConnectionUtils
        .getRecordsFilterCode( recordsFilter ) );
      rep.saveStepAttribute( id_transformation, id_step, "useCompression", useCompression );
      rep.saveStepAttribute( id_transformation, id_step, "queryAll", queryAll );

      for ( int i = 0; i < inputFields.length; i++ ) {
        SalesforceInputField field = inputFields[i];

        rep.saveStepAttribute( id_transformation, id_step, i, "field_name", field.getName() );

        // H.kawaguchi Bug Fix 17-01-2009
        rep.saveStepAttribute( id_transformation, id_step, i, "field_attribut", field.getField() );
        // H.kawaguchi Bug Fix 17-01-2009
        rep.saveStepAttribute( id_transformation, id_step, i, "field_idlookup", field.isIdLookup() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_type", field.getTypeDesc() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_format", field.getFormat() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_currency", field.getCurrencySymbol() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_decimal", field.getDecimalSymbol() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_group", field.getGroupSymbol() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_length", field.getLength() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_precision", field.getPrecision() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_trim_type", field.getTrimTypeCode() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_repeat", field.isRepeated() );
      }
    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString(
        PKG, "SalesforceInputMeta.Exception.ErrorSavingToRepository", "" + id_step ), e );
    }
  }

  public void check( List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta,
    RowMetaInterface prev, String[] input, String[] output, RowMetaInterface info, VariableSpace space,
    Repository repository, IMetaStore metaStore ) {
    CheckResult cr;

    // See if we get input...
    if ( input.length > 0 ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.NoInputExpected" ), stepMeta );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.NoInput" ), stepMeta );
    }
    remarks.add( cr );

    // check URL
    if ( Const.isEmpty( targeturl ) ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.NoURL" ), stepMeta );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.URLOk" ), stepMeta );
    }
    remarks.add( cr );

    // check username
    if ( Const.isEmpty( username ) ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.NoUsername" ), stepMeta );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.UsernameOk" ), stepMeta );
    }
    remarks.add( cr );

    // check module
    if ( Const.isEmpty( module ) ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.NoModule" ), stepMeta );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.ModuleOk" ), stepMeta );
    }
    remarks.add( cr );

    // check return fields
    if ( inputFields.length == 0 ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.NoFields" ), stepMeta );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.FieldsOk" ), stepMeta );
    }
    remarks.add( cr );

    // check additionals fields
    if ( includeTargetURL && Const.isEmpty( targetURLField ) ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.NoTargetURLField" ), stepMeta );
      remarks.add( cr );
    }
    if ( includeSQL && Const.isEmpty( sqlField ) ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.NoSQLField" ), stepMeta );
      remarks.add( cr );
    }
    if ( includeModule && Const.isEmpty( moduleField ) ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.NoModuleField" ), stepMeta );
      remarks.add( cr );
    }
    if ( includeTimestamp && Const.isEmpty( timestampField ) ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.NoTimestampField" ), stepMeta );
      remarks.add( cr );
    }
    if ( includeRowNumber && Const.isEmpty( rowNumberField ) ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.NoRowNumberField" ), stepMeta );
      remarks.add( cr );
    }
    if ( includeDeletionDate && Const.isEmpty( deletionDateField ) ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceInputMeta.CheckResult.NoDeletionDateField" ), stepMeta );
      remarks.add( cr );
    }
  }

  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr,
    TransMeta transMeta, Trans trans ) {
    return new SalesforceInput( stepMeta, stepDataInterface, cnr, transMeta, trans );
  }

  public StepDataInterface getStepData() {
    return new SalesforceInputData();
  }
}
