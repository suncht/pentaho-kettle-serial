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

package org.pentaho.di.trans.steps.salesforceupdate;

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
import org.pentaho.di.trans.steps.salesforceinput.SalesforceConnectionUtils;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

@Step(id="SalesforceUpdate", i18nPackageName="org.pentaho.di.trans.steps.salesforceupdate", name="SalesforceUpdate.TypeLongDesc.SalesforceUpdate", description="SalesforceUpdate.TypeTooltipDesc.SalesforceUpdate", categoryDescription="i18n:org.pentaho.di.trans.step:BaseStep.Category.Output", image="SFUD.svg", documentationUrl="http://wiki.pentaho.com/display/EAI/Salesforce+Update")
public class SalesforceUpdateMeta extends BaseStepMeta implements StepMetaInterface {
  private static Class<?> PKG = SalesforceUpdateMeta.class; // for i18n purposes, needed by Translator2!!

  /** The salesforce url */
  private String targeturl;

  /** The userName */
  private String username;

  /** The password */
  private String password;

  /** The module */
  private String module;

  /** Field value to update */
  private String[] updateLookup;

  /** boolean indicating if field uses External id */
  private Boolean[] useExternalId;

  /** Stream name to update value with */
  private String[] updateStream;

  /** Batch size */
  private String batchSize;

  /** The time out */
  private String timeout;

  private boolean useCompression;

  private boolean rollbackAllChangesOnError;

  public SalesforceUpdateMeta() {
    super(); // allocate BaseStepMeta
  }

  /**
   * @return Returns the rollbackAllChangesOnError.
   */
  public boolean isRollbackAllChangesOnError() {
    return rollbackAllChangesOnError;
  }

  /**
   * @param rollbackAllChangesOnError
   *          The rollbackAllChangesOnError to set.
   */
  public void setRollbackAllChangesOnError( boolean rollbackAllChangesOnError ) {
    this.rollbackAllChangesOnError = rollbackAllChangesOnError;
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
   * @param TimeOut
   *          The TimeOut to set.
   */
  public void setTimeOut( String TimeOut ) {
    this.timeout = TimeOut;
  }

  /**
   * @return Returns the TimeOut.
   */
  public String getTimeOut() {
    return timeout;
  }

  /**
   * @return Returns the updateLookup.
   */
  public String[] getUpdateLookup() {
    return updateLookup;
  }

  /**
   * @param updateLookup
   *          The updateLookup to set.
   */
  public void setUpdateLookup( String[] updateLookup ) {
    this.updateLookup = updateLookup;
  }

  /**
   * @return Returns the useExternalId.
   */
  public Boolean[] getUseExternalId() {
    return useExternalId;
  }

  /**
   * @param useExternalId
   *          The useExternalId to set.
   */
  public void setUseExternalId( Boolean[] useExternalId ) {
    this.useExternalId = useExternalId;
  }

  /**
   * @return Returns the updateStream.
   */
  public String[] getUpdateStream() {
    return updateStream;
  }

  /**
   * @param updateStream
   *          The updateStream to set.
   */
  public void setUpdateStream( String[] updateStream ) {
    this.updateStream = updateStream;
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
   * @param batch
   *          size.
   */
  public void setBatchSize( String value ) {
    this.batchSize = value;
  }

  /**
   * @return Returns the batchSize.
   */
  public String getBatchSize() {
    return this.batchSize;
  }

  public int getBatchSizeInt() {
    return Const.toInt( this.batchSize, 10 );
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

  public void loadXML( Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore ) throws KettleXMLException {
    readData( stepnode );
  }

  public Object clone() {
    SalesforceUpdateMeta retval = (SalesforceUpdateMeta) super.clone();

    int nrvalues = updateLookup.length;

    retval.allocate( nrvalues );

    for ( int i = 0; i < nrvalues; i++ ) {
      retval.updateLookup[i] = updateLookup[i];
      retval.updateStream[i] = updateStream[i];
      retval.useExternalId[i] = useExternalId[i];
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
    retval.append( "    " + XMLHandler.addTagValue( "batchSize", batchSize ) );

    retval.append( "    <fields>" + Const.CR );

    for ( int i = 0; i < updateLookup.length; i++ ) {
      retval.append( "      <field>" ).append( Const.CR );
      retval.append( "        " ).append( XMLHandler.addTagValue( "name", updateLookup[i] ) );
      retval.append( "        " ).append( XMLHandler.addTagValue( "field", updateStream[i] ) );
      retval.append( "        " ).append(
        XMLHandler.addTagValue( "useExternalId", useExternalId[i].booleanValue() ) );
      retval.append( "      </field>" ).append( Const.CR );
    }

    retval.append( "      </fields>" + Const.CR );
    retval.append( "    " + XMLHandler.addTagValue( "timeout", timeout ) );
    retval.append( "    " + XMLHandler.addTagValue( "useCompression", useCompression ) );
    retval.append( "    " + XMLHandler.addTagValue( "rollbackAllChangesOnError", rollbackAllChangesOnError ) );
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

      batchSize = XMLHandler.getTagValue( stepnode, "batchSize" );

      Node fields = XMLHandler.getSubNode( stepnode, "fields" );
      int nrFields = XMLHandler.countNodes( fields, "field" );

      allocate( nrFields );

      for ( int i = 0; i < nrFields; i++ ) {
        Node fnode = XMLHandler.getSubNodeByNr( fields, "field", i );

        updateLookup[i] = XMLHandler.getTagValue( fnode, "name" );
        updateStream[i] = XMLHandler.getTagValue( fnode, "field" );
        if ( updateStream[i] == null ) {
          updateStream[i] = updateLookup[i]; // default: the same name!
        }
        String updateValue = XMLHandler.getTagValue( fnode, "useExternalId" );
        if ( updateValue == null ) {
          // default FALSE
          useExternalId[i] = Boolean.FALSE;
        } else {
          if ( updateValue.equalsIgnoreCase( "Y" ) ) {
            useExternalId[i] = Boolean.TRUE;
          } else {
            useExternalId[i] = Boolean.FALSE;
          }
        }

      }
      useCompression = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "useCompression" ) );
      rollbackAllChangesOnError =
        "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "rollbackAllChangesOnError" ) );
      timeout = XMLHandler.getTagValue( stepnode, "timeout" );
    } catch ( Exception e ) {
      throw new KettleXMLException( "Unable to load step info from XML", e );
    }
  }

  public void allocate( int nrvalues ) {
    updateLookup = new String[nrvalues];
    updateStream = new String[nrvalues];
    useExternalId = new Boolean[nrvalues];
  }

  public void setDefault() {
    targeturl = SalesforceConnectionUtils.TARGET_DEFAULT_URL;
    password = "";
    module = "Account";
    batchSize = "10";

    int nrFields = 0;
    allocate( nrFields );

    for ( int i = 0; i < nrFields; i++ ) {
      updateLookup[i] = "name" + ( i + 1 );
      updateStream[i] = "field" + ( i + 1 );
      useExternalId[i] = Boolean.FALSE;
    }
    useCompression = false;
    rollbackAllChangesOnError = false;
    timeout = "60000";
  }

  /* This function adds meta data to the rows being pushed out */
  public void getFields( RowMetaInterface r, String name, RowMetaInterface[] info, StepMeta nextStep,
    VariableSpace space, Repository repository, IMetaStore metaStore ) throws KettleStepException {

  }

  public void readRep( Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases ) throws KettleException {
    try {
      targeturl = rep.getStepAttributeString( id_step, "targeturl" );
      module = rep.getStepAttributeString( id_step, "module" );
      username = rep.getStepAttributeString( id_step, "username" );
      password = rep.getStepAttributeString( id_step, "password" );
      batchSize = rep.getStepAttributeString( id_step, "batchSize" );
      int nrFields = rep.countNrStepAttributes( id_step, "field_name" );
      allocate( nrFields );

      for ( int i = 0; i < nrFields; i++ ) {
        updateLookup[i] = rep.getStepAttributeString( id_step, i, "field_name" );
        updateStream[i] = rep.getStepAttributeString( id_step, i, "field_attribut" );
        useExternalId[i] =
          Boolean.valueOf( rep.getStepAttributeBoolean( id_step, i, "field_useExternalId", false ) );
      }
      useCompression = rep.getStepAttributeBoolean( id_step, "useCompression" );
      timeout = rep.getStepAttributeString( id_step, "timeout" );
      rollbackAllChangesOnError = rep.getStepAttributeBoolean( id_step, "rollbackAllChangesOnError" );
    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString(
        PKG, "SalesforceUpdateMeta.Exception.ErrorReadingRepository" ), e );
    }
  }

  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step ) throws KettleException {
    try {
      rep.saveStepAttribute( id_transformation, id_step, "targeturl", targeturl );
      rep.saveStepAttribute( id_transformation, id_step, "batchSize", batchSize );
      rep.saveStepAttribute( id_transformation, id_step, "module", module );
      rep.saveStepAttribute( id_transformation, id_step, "username", username );
      rep.saveStepAttribute( id_transformation, id_step, "password", password );

      for ( int i = 0; i < updateLookup.length; i++ ) {
        rep.saveStepAttribute( id_transformation, id_step, i, "field_name", updateLookup[i] );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_attribut", updateStream[i] );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_useExternalId", useExternalId[i]
          .booleanValue() );

      }
      rep.saveStepAttribute( id_transformation, id_step, "useCompression", useCompression );
      rep.saveStepAttribute( id_transformation, id_step, "timeout", timeout );
      rep.saveStepAttribute( id_transformation, id_step, "rollbackAllChangesOnError", rollbackAllChangesOnError );
    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString(
        PKG, "SalesforceUpdateMeta.Exception.ErrorSavingToRepository", "" + id_step ), e );
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
          PKG, "SalesforceUpdateMeta.CheckResult.NoInputExpected" ), stepMeta );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SalesforceUpdateMeta.CheckResult.NoInput" ), stepMeta );
    }
    remarks.add( cr );

    // check URL
    if ( Const.isEmpty( targeturl ) ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceUpdateMeta.CheckResult.NoURL" ), stepMeta );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SalesforceUpdateMeta.CheckResult.URLOk" ), stepMeta );
    }
    remarks.add( cr );

    // check username
    if ( Const.isEmpty( username ) ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceUpdateMeta.CheckResult.NoUsername" ), stepMeta );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SalesforceUpdateMeta.CheckResult.UsernameOk" ), stepMeta );
    }
    remarks.add( cr );

    // check module
    if ( Const.isEmpty( module ) ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceUpdateMeta.CheckResult.NoModule" ), stepMeta );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SalesforceUpdateMeta.CheckResult.ModuleOk" ), stepMeta );
    }
    remarks.add( cr );

    // check return fields
    if ( updateLookup.length == 0 ) {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(
          PKG, "SalesforceUpdateMeta.CheckResult.NoFields" ), stepMeta );
    } else {
      cr =
        new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString(
          PKG, "SalesforceUpdateMeta.CheckResult.FieldsOk" ), stepMeta );
    }
    remarks.add( cr );
  }

  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr,
    TransMeta transMeta, Trans trans ) {
    return new SalesforceUpdate( stepMeta, stepDataInterface, cnr, transMeta, trans );
  }

  public StepDataInterface getStepData() {
    return new SalesforceUpdateData();
  }

  public boolean supportsErrorHandling() {
    return true;
  }

}
