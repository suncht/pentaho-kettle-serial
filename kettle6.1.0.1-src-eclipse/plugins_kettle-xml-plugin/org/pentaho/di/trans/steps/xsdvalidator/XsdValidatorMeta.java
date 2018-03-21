/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2015 by Pentaho : http://www.pentaho.com
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

package org.pentaho.di.trans.steps.xsdvalidator;

import java.util.List;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.resource.ResourceDefinition;
import org.pentaho.di.resource.ResourceNamingInterface;
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
 * Created on 14-08-2007
 *
 */
@Step( id = "XSDValidator", image = "XOU.svg", i18nPackageName = "org.pentaho.di.trans.steps.xsdvalidator",
    name = "XSDValidator.name", description = "XSDValidator.description",
    categoryDescription = "XSDValidator.category",
    documentationUrl = "http://wiki.pentaho.com/display/EAI/XSD+Validator" )
public class XsdValidatorMeta extends BaseStepMeta implements StepMetaInterface {
  private static Class<?> PKG = XsdValidatorMeta.class; // for i18n purposes, needed by Translator2!!

  private String xsdFilename;
  private String xmlStream;
  private String resultFieldname;
  private boolean addValidationMessage;
  private String validationMessageField;
  private boolean outputStringField;
  private String ifXmlValid;
  private String ifXmlInvalid;
  private boolean xmlSourceFile;
  private String xsdDefinedField;

  private String xsdSource;

  public String SPECIFY_FILENAME = "filename";
  public String SPECIFY_FIELDNAME = "fieldname";
  public String NO_NEED = "noneed";

  public void setXSDSource( String xsdsourcein ) {
    this.xsdSource = xsdsourcein;
  }

  public String getXSDSource() {
    return xsdSource;
  }

  public void setXSDDefinedField( String xsddefinedfieldin ) {
    this.xsdDefinedField = xsddefinedfieldin;
  }

  public String getXSDDefinedField() {
    return xsdDefinedField;
  }

  public boolean getXMLSourceFile() {
    return xmlSourceFile;
  }

  public void setXMLSourceFile( boolean xmlsourcefilein ) {
    this.xmlSourceFile = xmlsourcefilein;
  }

  public String getIfXmlValid() {
    return ifXmlValid;
  }

  public String getIfXmlInvalid() {
    return ifXmlInvalid;
  }

  public void setIfXMLValid( String ifXmlValid ) {
    this.ifXmlValid = ifXmlValid;
  }

  public void setIfXmlInvalid( String ifXmlInvalid ) {
    this.ifXmlInvalid = ifXmlInvalid;
  }

  public boolean getOutputStringField() {
    return outputStringField;
  }

  public void setOutputStringField( boolean outputStringField ) {
    this.outputStringField = outputStringField;
  }

  public String getValidationMessageField() {
    return validationMessageField;
  }

  public void setValidationMessageField( String validationMessageField ) {
    this.validationMessageField = validationMessageField;
  }

  public boolean useAddValidationMessage() {
    return addValidationMessage;
  }

  public void setAddValidationMessage( boolean addValidationMessage ) {
    this.addValidationMessage = addValidationMessage;
  }

  public XsdValidatorMeta() {
    super(); // allocate BaseStepMeta
  }

  /**
   * @return Returns the XSD filename.
   */
  public String getXSDFilename() {
    return xsdFilename;
  }

  public String getResultfieldname() {
    return resultFieldname;
  }

  public String getXMLStream() {
    return xmlStream;
  }

  /**
   * @param xdsFilename
   *          The XSD filename to set.
   */
  public void setXSDfilename( String xdsFilename ) {
    this.xsdFilename = xdsFilename;
  }

  public void setResultfieldname( String resultFieldname ) {
    this.resultFieldname = resultFieldname;
  }

  public void setXMLStream( String xmlStream ) {
    this.xmlStream = xmlStream;
  }

  public void loadXML( Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore ) throws KettleXMLException {
    readData( stepnode );
  }

  public Object clone() {
    XsdValidatorMeta retval = (XsdValidatorMeta) super.clone();

    return retval;
  }

  private void readData( Node stepnode ) throws KettleXMLException {
    try {

      xsdFilename = XMLHandler.getTagValue( stepnode, "xdsfilename" );
      xmlStream = XMLHandler.getTagValue( stepnode, "xmlstream" );
      resultFieldname = XMLHandler.getTagValue( stepnode, "resultfieldname" );
      xsdDefinedField = XMLHandler.getTagValue( stepnode, "xsddefinedfield" );
      xsdSource = XMLHandler.getTagValue( stepnode, "xsdsource" );

      addValidationMessage = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "addvalidationmsg" ) );

      validationMessageField = XMLHandler.getTagValue( stepnode, "validationmsgfield" );
      ifXmlValid = XMLHandler.getTagValue( stepnode, "ifxmlvalid" );
      ifXmlInvalid = XMLHandler.getTagValue( stepnode, "ifxmlunvalid" );
      outputStringField = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "outputstringfield" ) );
      xmlSourceFile = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "xmlsourcefile" ) );

    } catch ( Exception e ) {
      throw new KettleXMLException( BaseMessages.getString( PKG,
          "XsdValidatorMeta.Exception.UnableToLoadStepInfoFromXML" ), e );
    }
  }

  public void setDefault() {
    xsdFilename = "";
    xmlStream = "";
    resultFieldname = "result";
    addValidationMessage = false;
    validationMessageField = "ValidationMsgField";
    ifXmlValid = "";
    ifXmlInvalid = "";
    outputStringField = false;
    xmlSourceFile = false;
    xsdDefinedField = "";
    xsdSource = SPECIFY_FILENAME;

  }

  public void getFields( RowMetaInterface inputRowMeta, String name, RowMetaInterface[] info, StepMeta nextStep,
      VariableSpace space, Repository repository, IMetaStore metaStore ) throws KettleStepException {
    if ( !Const.isEmpty( resultFieldname ) ) {
      if ( outputStringField ) {
        // Output field (String)
        ValueMetaInterface v =
            new ValueMeta( space.environmentSubstitute( getResultfieldname() ), ValueMeta.TYPE_STRING );
        inputRowMeta.addValueMeta( v );
      } else {

        // Output field (boolean)
        ValueMetaInterface v =
            new ValueMeta( space.environmentSubstitute( getResultfieldname() ), ValueMeta.TYPE_BOOLEAN );
        inputRowMeta.addValueMeta( v );
      }

    }
    // Add String Field that contain validation message (most the time, errors)
    if ( addValidationMessage && !Const.isEmpty( validationMessageField ) ) {
      ValueMetaInterface v =
          new ValueMeta( space.environmentSubstitute( validationMessageField ), ValueMeta.TYPE_STRING );
      inputRowMeta.addValueMeta( v );
    }

  }

  public String getXML() {
    StringBuffer retval = new StringBuffer();

    retval.append( "    " + XMLHandler.addTagValue( "xdsfilename", xsdFilename ) );
    retval.append( "    " + XMLHandler.addTagValue( "xmlstream", xmlStream ) );
    retval.append( "    " + XMLHandler.addTagValue( "resultfieldname", resultFieldname ) );
    retval.append( "    " + XMLHandler.addTagValue( "addvalidationmsg", addValidationMessage ) );
    retval.append( "    " + XMLHandler.addTagValue( "validationmsgfield", validationMessageField ) );
    retval.append( "    " + XMLHandler.addTagValue( "ifxmlunvalid", ifXmlInvalid ) );
    retval.append( "    " + XMLHandler.addTagValue( "ifxmlvalid", ifXmlValid ) );

    retval.append( "    " + XMLHandler.addTagValue( "outputstringfield", outputStringField ) );
    retval.append( "    " + XMLHandler.addTagValue( "xmlsourcefile", xmlSourceFile ) );
    retval.append( "    " + XMLHandler.addTagValue( "xsddefinedfield", xsdDefinedField ) );
    retval.append( "    " + XMLHandler.addTagValue( "xsdsource", xsdSource ) );

    return retval.toString();
  }

  public void readRep( Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases )
    throws KettleException {
    try {
      xsdFilename = rep.getStepAttributeString( id_step, "xdsfilename" );
      xmlStream = rep.getStepAttributeString( id_step, "xmlstream" );
      resultFieldname = rep.getStepAttributeString( id_step, "resultfieldname" );

      xmlSourceFile = rep.getStepAttributeBoolean( id_step, "xmlsourcefile" );
      addValidationMessage = rep.getStepAttributeBoolean( id_step, "addvalidationmsg" );
      validationMessageField = rep.getStepAttributeString( id_step, "validationmsgfield" );
      ifXmlValid = rep.getStepAttributeString( id_step, "ifxmlvalid" );
      ifXmlInvalid = rep.getStepAttributeString( id_step, "ifxmlunvalid" );

      outputStringField = rep.getStepAttributeBoolean( id_step, "outputstringfield" );
      xsdDefinedField = rep.getStepAttributeString( id_step, "xsddefinedfield" );
      xsdSource = rep.getStepAttributeString( id_step, "xsdsource" );

    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString( PKG,
          "XsdValidatorMeta.Exception.UnexpectedErrorInReadingStepInfo" ), e );
    }
  }

  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step )
    throws KettleException {
    try {
      rep.saveStepAttribute( id_transformation, id_step, "xdsfilename", xsdFilename );
      rep.saveStepAttribute( id_transformation, id_step, "xmlstream", xmlStream );
      rep.saveStepAttribute( id_transformation, id_step, "resultfieldname", resultFieldname );
      rep.saveStepAttribute( id_transformation, id_step, "xmlsourcefile", xmlSourceFile );
      rep.saveStepAttribute( id_transformation, id_step, "addvalidationmsg", addValidationMessage );
      rep.saveStepAttribute( id_transformation, id_step, "validationmsgfield", validationMessageField );
      rep.saveStepAttribute( id_transformation, id_step, "ifxmlvalid", ifXmlValid );
      rep.saveStepAttribute( id_transformation, id_step, "ifxmlunvalid", ifXmlInvalid );
      rep.saveStepAttribute( id_transformation, id_step, "outputstringfield", outputStringField );
      rep.saveStepAttribute( id_transformation, id_step, "xsddefinedfield", xsdDefinedField );
      rep.saveStepAttribute( id_transformation, id_step, "xsdsource", xsdSource );

    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString( PKG, "XsdValidatorMeta.Exception.UnableToSaveStepInfo" )
          + id_step, e );
    }
  }

  public void check( List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepinfo, RowMetaInterface prev,
      String[] input, String[] output, RowMetaInterface info, VariableSpace space, Repository repository,
      IMetaStore metaStore ) {
    CheckResult cr;

    // Check XML stream field
    if ( Const.isEmpty( xmlStream ) ) {
      cr =
          new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString( PKG,
              "XsdValidatorMeta.CheckResult.XMLStreamFieldEmpty" ), stepinfo );
      remarks.add( cr );
    } else {
      cr =
          new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString( PKG,
              "XsdValidatorMeta.CheckResult.XMLStreamFieldOK" ), stepinfo );
      remarks.add( cr );
    }

    // Check result fieldname
    if ( Const.isEmpty( resultFieldname ) ) {
      cr =
          new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString( PKG,
              "XsdValidatorMeta.CheckResult.ResultFieldEmpty" ), stepinfo );
      remarks.add( cr );
    } else {
      cr =
          new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString( PKG,
              "XsdValidatorMeta.CheckResult.ResultFieldOK" ), stepinfo );
      remarks.add( cr );
    }

    if ( xsdSource.equals( SPECIFY_FILENAME ) ) {
      if ( Const.isEmpty( xsdFilename ) ) {
        cr =
            new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString( PKG,
                "XsdValidatorMeta.CheckResult.XSDFieldEmpty" ), stepinfo );
        remarks.add( cr );
      }
    }

    if ( prev != null && prev.size() > 0 ) {
      cr =
          new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString( PKG,
              "XsdValidatorMeta.CheckResult.ConnectedStepOK", String.valueOf( prev.size() ) ), stepinfo );
      remarks.add( cr );
    } else {
      cr =
          new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString( PKG,
              "XsdValidatorMeta.CheckResult.NoInputReceived" ), stepinfo );
      remarks.add( cr );
    }

    // See if we have input streams leading to this step!
    if ( input.length > 0 ) {
      cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString( PKG,
              "XsdValidatorMeta.CheckResult.ExpectedInputOk" ), stepinfo );
      remarks.add( cr );
    } else {
      cr =
          new CheckResult( CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString( PKG,
              "XsdValidatorMeta.CheckResult.ExpectedInputError" ), stepinfo );
      remarks.add( cr );
    }
  }

  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
      Trans trans ) {
    return new XsdValidator( stepMeta, stepDataInterface, cnr, transMeta, trans );

  }

  public StepDataInterface getStepData() {
    return new XsdValidatorData();
  }

  public boolean supportsErrorHandling() {
    return true;
  }

  /**
   * Since the exported transformation that runs this will reside in a ZIP file, we can't reference files relatively. So
   * what this does is turn the name of files into absolute paths OR it simply includes the resource in the ZIP file.
   * For now, we'll simply turn it into an absolute path and pray that the file is on a shared drive or something like
   * that.
   * 
   * @param space
   *          the variable space to use
   * @param definitions
   * @param resourceNamingInterface
   * @param repository
   *          The repository to optionally load other resources from (to be converted to XML)
   * @param metaStore
   *          the metaStore in which non-kettle metadata could reside.
   * 
   * @return the filename of the exported resource
   */
  public String exportResources( VariableSpace space, Map<String, ResourceDefinition> definitions,
      ResourceNamingInterface resourceNamingInterface, Repository repository, IMetaStore metaStore )
    throws KettleException {
    try {
      // The object that we're modifying here is a copy of the original!
      // So let's change the filename from relative to absolute by grabbing the file object...
      // In case the name of the file comes from previous steps, forget about this!
      //

      // From : ${Internal.Transformation.Filename.Directory}/../foo/bar.xsd
      // To : /home/matt/test/files/foo/bar.xsd
      //
      if ( !Const.isEmpty( xsdFilename ) ) {
        FileObject fileObject = KettleVFS.getFileObject( space.environmentSubstitute( xsdFilename ), space );
        xsdFilename = resourceNamingInterface.nameResource( fileObject, space, true );
        return xsdFilename;
      }

      return null;
    } catch ( Exception e ) {
      throw new KettleException( e );
    }
  }
}
