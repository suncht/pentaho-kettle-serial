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

package org.pentaho.di.trans.steps.jsonoutput;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.annotations.Step;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaString;
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
import org.pentaho.di.trans.step.StepMetaInjectionInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/**
 * This class knows how to handle the MetaData for the Json output step
 * 
 * @since 14-june-2010
 * 
 */
@Step( id = "JsonOutput", image = "JSO.svg", i18nPackageName = "org.pentaho.di.trans.steps.jsonoutput",
    name = "JsonOutput.name", description = "JsonOutput.description", categoryDescription = "JsonOutput.category" )
public class JsonOutputMeta extends BaseStepMeta implements StepMetaInterface {
  private static Class<?> PKG = JsonOutputMeta.class; // for i18n purposes, needed by Translator2!!

  /** Operations type */
  private int operationType;

  /**
   * The operations description
   */
  public static final String[] operationTypeDesc = {
    BaseMessages.getString( PKG, "JsonOutputMeta.operationType.OutputValue" ),
    BaseMessages.getString( PKG, "JsonOutputMeta.operationType.WriteToFile" ),
    BaseMessages.getString( PKG, "JsonOutputMeta.operationType.Both" ) };

  /**
   * The operations type codes
   */
  public static final String[] operationTypeCode = { "outputvalue", "writetofile", "both" };

  public static final int OPERATION_TYPE_OUTPUT_VALUE = 0;

  public static final int OPERATION_TYPE_WRITE_TO_FILE = 1;

  public static final int OPERATION_TYPE_BOTH = 2;

  /** The encoding to use for reading: null or empty string means system default encoding */
  private String encoding;

  /** The name value containing the resulting Json fragment */
  private String outputValue;

  /** The name of the json bloc */
  private String jsonBloc;

  private String nrRowsInBloc;

  /* THE FIELD SPECIFICATIONS ... */

  /** The output fields */
  private JsonOutputField[] outputFields;

  private boolean AddToResult;

  /** Whether to push the output into the output of a servlet with the executeTrans Carte/DI-Server servlet */
  private boolean servletOutput;

  /** The base name of the output file */
  private String fileName;

  /** The file extention in case of a generated filename */
  private String extension;

  /** Flag to indicate the we want to append to the end of an existing file (if it exists) */
  private boolean fileAppended;

  /** Flag to indicate whether or not to create JSON structures compatible with pre PDI-4.3.0 */
  private boolean compatibilityMode;

  /** Flag: add the stepnr in the filename */
  private boolean stepNrInFilename;

  /** Flag: add the partition number in the filename */
  private boolean partNrInFilename;

  /** Flag: add the date in the filename */
  private boolean dateInFilename;

  /** Flag: add the time in the filename */
  private boolean timeInFilename;

  /** Flag: create parent folder if needed */
  private boolean createparentfolder;

  private boolean DoNotOpenNewFileInit;

  public JsonOutputMeta() {
    super(); // allocate BaseStepMeta
  }

  public boolean isDoNotOpenNewFileInit() {
    return DoNotOpenNewFileInit;
  }

  public void setDoNotOpenNewFileInit( boolean DoNotOpenNewFileInit ) {
    this.DoNotOpenNewFileInit = DoNotOpenNewFileInit;
  }

  /**
   * @return Returns the create parent folder flag.
   */
  public boolean isCreateParentFolder() {
    return createparentfolder;
  }

  /**
   * @param createparentfolder
   *          The create parent folder flag to set.
   */
  public void setCreateParentFolder( boolean createparentfolder ) {
    this.createparentfolder = createparentfolder;
  }

  /**
   * @return Returns the extension.
   */
  public String getExtension() {
    return extension;
  }

  /**
   * @param extension
   *          The extension to set.
   */
  public void setExtension( String extension ) {
    this.extension = extension;
  }

  /**
   * @return Returns the fileAppended.
   */
  public boolean isFileAppended() {
    return fileAppended;
  }

  /**
   * @param fileAppended
   *          The fileAppended to set.
   */
  public void setFileAppended( boolean fileAppended ) {
    this.fileAppended = fileAppended;
  }

  /**
   * @return Returns the fileName.
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * @return Returns the timeInFilename.
   */
  public boolean isTimeInFilename() {
    return timeInFilename;
  }

  /**
   * @return Returns the dateInFilename.
   */
  public boolean isDateInFilename() {
    return dateInFilename;
  }

  /**
   * @param dateInFilename
   *          The dateInFilename to set.
   */
  public void setDateInFilename( boolean dateInFilename ) {
    this.dateInFilename = dateInFilename;
  }

  /**
   * @param timeInFilename
   *          The timeInFilename to set.
   */
  public void setTimeInFilename( boolean timeInFilename ) {
    this.timeInFilename = timeInFilename;
  }

  /**
   * @param fileName
   *          The fileName to set.
   */
  public void setFileName( String fileName ) {
    this.fileName = fileName;
  }

  /**
   * @return Returns the Add to result filesname flag.
   */
  public boolean AddToResult() {
    return AddToResult;
  }

  public int getOperationType() {
    return operationType;
  }

  public static int getOperationTypeByDesc( String tt ) {
    if ( tt == null ) {
      return 0;
    }

    for ( int i = 0; i < operationTypeDesc.length; i++ ) {
      if ( operationTypeDesc[i].equalsIgnoreCase( tt ) ) {
        return i;
      }
    }
    // If this fails, try to match using the code.
    return getOperationTypeByCode( tt );
  }

  public void setOperationType( int operationType ) {
    this.operationType = operationType;
  }

  public static String getOperationTypeDesc( int i ) {
    if ( i < 0 || i >= operationTypeDesc.length ) {
      return operationTypeDesc[0];
    }
    return operationTypeDesc[i];
  }

  private static int getOperationTypeByCode( String tt ) {
    if ( tt == null ) {
      return 0;
    }

    for ( int i = 0; i < operationTypeCode.length; i++ ) {
      if ( operationTypeCode[i].equalsIgnoreCase( tt ) ) {
        return i;
      }
    }
    return 0;
  }

  /**
   * @return Returns the outputFields.
   */
  public JsonOutputField[] getOutputFields() {
    return outputFields;
  }

  /**
   * @param outputFields
   *          The outputFields to set.
   */
  public void setOutputFields( JsonOutputField[] outputFields ) {
    this.outputFields = outputFields;
  }

  public void loadXML( Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore ) throws KettleXMLException {
    readData( stepnode );
  }

  public void allocate( int nrfields ) {
    outputFields = new JsonOutputField[nrfields];
  }

  public Object clone() {
    JsonOutputMeta retval = (JsonOutputMeta) super.clone();
    int nrfields = outputFields.length;

    retval.allocate( nrfields );

    for ( int i = 0; i < nrfields; i++ ) {
      retval.outputFields[i] = (JsonOutputField) outputFields[i].clone();
    }

    return retval;
  }

  /**
   * @param AddToResult
   *          The Add file to result to set.
   */
  public void setAddToResult( boolean AddToResult ) {
    this.AddToResult = AddToResult;
  }

  private void readData( Node stepnode ) throws KettleXMLException {
    try {
      outputValue = XMLHandler.getTagValue( stepnode, "outputValue" );
      jsonBloc = XMLHandler.getTagValue( stepnode, "jsonBloc" );
      nrRowsInBloc = XMLHandler.getTagValue( stepnode, "nrRowsInBloc" );
      operationType = getOperationTypeByCode( Const.NVL( XMLHandler.getTagValue( stepnode, "operation_type" ), "" ) );
      compatibilityMode = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "compatibility_mode" ) );

      encoding = XMLHandler.getTagValue( stepnode, "encoding" );
      AddToResult = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "AddToResult" ) );
      fileName = XMLHandler.getTagValue( stepnode, "file", "name" );
      createparentfolder = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "file", "create_parent_folder" ) );
      extension = XMLHandler.getTagValue( stepnode, "file", "extention" );
      fileAppended = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "file", "append" ) );
      stepNrInFilename = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "file", "split" ) );
      partNrInFilename = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "file", "haspartno" ) );
      dateInFilename = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "file", "add_date" ) );
      timeInFilename = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "file", "add_time" ) );
      DoNotOpenNewFileInit = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "file", "DoNotOpenNewFileInit" ) );
      servletOutput = "Y".equalsIgnoreCase( XMLHandler.getTagValue( stepnode, "file", "servlet_output" ) );

      Node fields = XMLHandler.getSubNode( stepnode, "fields" );
      int nrfields = XMLHandler.countNodes( fields, "field" );

      allocate( nrfields );

      for ( int i = 0; i < nrfields; i++ ) {
        Node fnode = XMLHandler.getSubNodeByNr( fields, "field", i );

        outputFields[i] = new JsonOutputField();
        outputFields[i].setFieldName( XMLHandler.getTagValue( fnode, "name" ) );
        outputFields[i].setElementName( XMLHandler.getTagValue( fnode, "element" ) );
      }
    } catch ( Exception e ) {
      throw new KettleXMLException( "Unable to load step info from XML", e );
    }
  }

  public void setDefault() {
    encoding = Const.XML_ENCODING;
    outputValue = "outputValue";
    jsonBloc = "data";
    nrRowsInBloc = "1";
    operationType = OPERATION_TYPE_WRITE_TO_FILE;
    extension = "js";
    int nrfields = 0;

    allocate( nrfields );

    for ( int i = 0; i < nrfields; i++ ) {
      outputFields[i] = new JsonOutputField();
      outputFields[i].setFieldName( "field" + i );
      outputFields[i].setElementName( "field" + i );
    }
  }

  public void getFields( RowMetaInterface row, String name, RowMetaInterface[] info, StepMeta nextStep,
      VariableSpace space, Repository repository, IMetaStore metaStore ) throws KettleStepException {

    if ( getOperationType() != OPERATION_TYPE_WRITE_TO_FILE ) {
      ValueMetaInterface v =
          new ValueMetaString( space.environmentSubstitute( this.getOutputValue() ) );
      v.setOrigin( name );
      row.addValueMeta( v );
    }
  }

  public String getXML() {
    StringBuffer retval = new StringBuffer( 500 );

    retval.append( "    " ).append( XMLHandler.addTagValue( "outputValue", outputValue ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "jsonBloc", jsonBloc ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "nrRowsInBloc", nrRowsInBloc ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "operation_type", getOperationTypeCode( operationType ) ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "compatibility_mode", compatibilityMode ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "encoding", encoding ) );
    retval.append( "    " ).append( XMLHandler.addTagValue( "addtoresult", AddToResult ) );
    retval.append( "    <file>" + Const.CR );
    retval.append( "      " ).append( XMLHandler.addTagValue( "name", fileName ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "extention", extension ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "append", fileAppended ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "split", stepNrInFilename ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "haspartno", partNrInFilename ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "add_date", dateInFilename ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "add_time", timeInFilename ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "create_parent_folder", createparentfolder ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "DoNotOpenNewFileInit", DoNotOpenNewFileInit ) );
    retval.append( "      " ).append( XMLHandler.addTagValue( "servlet_output", servletOutput ) );
    retval.append( "      </file>" + Const.CR );

    retval.append( "    <fields>" ).append( Const.CR );
    for ( int i = 0; i < outputFields.length; i++ ) {
      JsonOutputField field = outputFields[i];

      if ( field.getFieldName() != null && field.getFieldName().length() != 0 ) {
        retval.append( "      <field>" ).append( Const.CR );
        retval.append( "        " ).append( XMLHandler.addTagValue( "name", field.getFieldName() ) );
        retval.append( "        " ).append( XMLHandler.addTagValue( "element", field.getElementName() ) );
        retval.append( "    </field>" + Const.CR );
      }
    }
    retval.append( "    </fields>" ).append( Const.CR );
    return retval.toString();
  }

  public void readRep( Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases )
    throws KettleException {
    try {
      outputValue = rep.getStepAttributeString( id_step, "outputValue" );
      jsonBloc = rep.getStepAttributeString( id_step, "jsonBloc" );
      nrRowsInBloc = rep.getStepAttributeString( id_step, "nrRowsInBloc" );

      operationType = getOperationTypeByCode( Const.NVL( rep.getStepAttributeString( id_step, "operation_type" ), "" ) );
      compatibilityMode = rep.getStepAttributeBoolean( id_step, "compatibility_mode" );
      encoding = rep.getStepAttributeString( id_step, "encoding" );
      AddToResult = rep.getStepAttributeBoolean( id_step, "addtoresult" );

      fileName = rep.getStepAttributeString( id_step, "file_name" );
      extension = rep.getStepAttributeString( id_step, "file_extention" );
      fileAppended = rep.getStepAttributeBoolean( id_step, "file_append" );
      stepNrInFilename = rep.getStepAttributeBoolean( id_step, "file_add_stepnr" );
      partNrInFilename = rep.getStepAttributeBoolean( id_step, "file_add_partnr" );
      dateInFilename = rep.getStepAttributeBoolean( id_step, "file_add_date" );
      timeInFilename = rep.getStepAttributeBoolean( id_step, "file_add_time" );
      createparentfolder = rep.getStepAttributeBoolean( id_step, "create_parent_folder" );
      DoNotOpenNewFileInit = rep.getStepAttributeBoolean( id_step, "DoNotOpenNewFileInit" );
      servletOutput = rep.getStepAttributeBoolean( id_step, "file_servlet_output" );

      int nrfields = rep.countNrStepAttributes( id_step, "field_name" );

      allocate( nrfields );

      for ( int i = 0; i < nrfields; i++ ) {
        outputFields[i] = new JsonOutputField();

        outputFields[i].setFieldName( rep.getStepAttributeString( id_step, i, "field_name" ) );
        outputFields[i].setElementName( rep.getStepAttributeString( id_step, i, "field_element" ) );
      }
    } catch ( Exception e ) {
      throw new KettleException( "Unexpected error reading step information from the repository", e );
    }
  }

  private static String getOperationTypeCode( int i ) {
    if ( i < 0 || i >= operationTypeCode.length ) {
      return operationTypeCode[0];
    }
    return operationTypeCode[i];
  }

  public void saveRep( Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step )
    throws KettleException {
    try {
      rep.saveStepAttribute( id_transformation, id_step, "outputValue", outputValue );
      rep.saveStepAttribute( id_transformation, id_step, "jsonBloc", jsonBloc );
      rep.saveStepAttribute( id_transformation, id_step, "nrRowsInBloc", nrRowsInBloc );

      rep.saveStepAttribute( id_transformation, id_step, "operation_type", getOperationTypeCode( operationType ) );
      rep.saveStepAttribute( id_transformation, id_step, "compatibility_mode", compatibilityMode );
      rep.saveStepAttribute( id_transformation, id_step, "encoding", encoding );
      rep.saveStepAttribute( id_transformation, id_step, "addtoresult", AddToResult );

      rep.saveStepAttribute( id_transformation, id_step, "file_name", fileName );
      rep.saveStepAttribute( id_transformation, id_step, "file_extention", extension );
      rep.saveStepAttribute( id_transformation, id_step, "file_append", fileAppended );
      rep.saveStepAttribute( id_transformation, id_step, "file_add_stepnr", stepNrInFilename );
      rep.saveStepAttribute( id_transformation, id_step, "file_add_partnr", partNrInFilename );
      rep.saveStepAttribute( id_transformation, id_step, "file_add_date", dateInFilename );
      rep.saveStepAttribute( id_transformation, id_step, "file_add_time", timeInFilename );
      rep.saveStepAttribute( id_transformation, id_step, "create_parent_folder", createparentfolder );
      rep.saveStepAttribute( id_transformation, id_step, "DoNotOpenNewFileInit", DoNotOpenNewFileInit );
      rep.saveStepAttribute( id_transformation, id_step, "file_servlet_output", servletOutput );

      for ( int i = 0; i < outputFields.length; i++ ) {
        JsonOutputField field = outputFields[i];

        rep.saveStepAttribute( id_transformation, id_step, i, "field_name", field.getFieldName() );
        rep.saveStepAttribute( id_transformation, id_step, i, "field_element", field.getElementName() );
      }
    } catch ( Exception e ) {
      throw new KettleException( "Unable to save step information to the repository for id_step=" + id_step, e );
    }
  }

  public String[] getFiles( String fileName ) {
    int copies = 1;
    int splits = 1;
    int parts = 1;

    if ( stepNrInFilename ) {
      copies = 3;
    }

    if ( partNrInFilename ) {
      parts = 3;
    }

    int nr = copies * parts * splits;
    if ( nr > 1 ) {
      nr++;
    }

    String[] retval = new String[nr];

    int i = 0;
    for ( int copy = 0; copy < copies; copy++ ) {
      for ( int part = 0; part < parts; part++ ) {
        for ( int split = 0; split < splits; split++ ) {
          retval[i] = buildFilename( fileName, copy, split );
          i++;
        }
      }
    }
    if ( i < nr ) {
      retval[i] = "...";
    }

    return retval;
  }

  public String buildFilename( String fileName, int stepnr, int splitnr ) {
    SimpleDateFormat daf = new SimpleDateFormat();

    // Replace possible environment variables...
    String retval = fileName;

    Date now = new Date();

    if ( dateInFilename ) {
      daf.applyPattern( "yyyMMdd" );
      String d = daf.format( now );
      retval += "_" + d;
    }
    if ( timeInFilename ) {
      daf.applyPattern( "HHmmss.SSS" );
      String t = daf.format( now );
      retval += "_" + t;
    }
    if ( stepNrInFilename ) {
      retval += "_" + stepnr;
    }

    if ( extension != null && extension.length() != 0 ) {
      retval += "." + extension;
    }

    return retval;
  }

  public void check( List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev,
      String[] input, String[] output, RowMetaInterface info, VariableSpace space, Repository repository,
      IMetaStore metaStore ) {

    CheckResult cr;
    if ( getOperationType() != JsonOutputMeta.OPERATION_TYPE_WRITE_TO_FILE ) {
      // We need to have output field name
      if ( Const.isEmpty( transMeta.environmentSubstitute( getOutputValue() ) ) ) {
        cr =
            new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString( PKG,
                "JsonOutput.Error.MissingOutputFieldName" ), stepMeta );
        remarks.add( cr );
      }
    }
    if ( Const.isEmpty( transMeta.environmentSubstitute( getFileName() ) ) ) {
      cr =
          new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString( PKG,
              "JsonOutput.Error.MissingTargetFilename" ), stepMeta );
      remarks.add( cr );
    }
    // Check output fields
    if ( prev != null && prev.size() > 0 ) {
      cr =
          new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString( PKG,
              "JsonOutputMeta.CheckResult.FieldsReceived", "" + prev.size() ), stepMeta );
      remarks.add( cr );

      String error_message = "";
      boolean error_found = false;

      // Starting from selected fields in ...
      for ( int i = 0; i < outputFields.length; i++ ) {
        int idx = prev.indexOfValue( outputFields[i].getFieldName() );
        if ( idx < 0 ) {
          error_message += "\t\t" + outputFields[i].getFieldName() + Const.CR;
          error_found = true;
        }
      }
      if ( error_found ) {
        error_message = BaseMessages.getString( PKG, "JsonOutputMeta.CheckResult.FieldsNotFound", error_message );
        cr = new CheckResult( CheckResult.TYPE_RESULT_ERROR, error_message, stepMeta );
        remarks.add( cr );
      } else {
        cr =
            new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString( PKG,
                "JsonOutputMeta.CheckResult.AllFieldsFound" ), stepMeta );
        remarks.add( cr );
      }
    }

    // See if we have input streams leading to this step!
    if ( input.length > 0 ) {
      cr =
          new CheckResult( CheckResult.TYPE_RESULT_OK, BaseMessages.getString( PKG,
              "JsonOutputMeta.CheckResult.ExpectedInputOk" ), stepMeta );
      remarks.add( cr );
    } else {
      cr =
          new CheckResult( CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString( PKG,
              "JsonOutputMeta.CheckResult.ExpectedInputError" ), stepMeta );
      remarks.add( cr );
    }

    cr =
        new CheckResult( CheckResult.TYPE_RESULT_COMMENT, BaseMessages.getString( PKG,
            "JsonOutputMeta.CheckResult.FilesNotChecked" ), stepMeta );
    remarks.add( cr );
  }

  public StepInterface getStep( StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta,
      Trans trans ) {
    return new JsonOutput( stepMeta, stepDataInterface, cnr, transMeta, trans );
  }

  public StepDataInterface getStepData() {
    return new JsonOutputData();
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding( String encoding ) {
    this.encoding = encoding;
  }

  /**
   * @return Returns the jsonBloc.
   */
  public String getJsonBloc() {
    return jsonBloc;
  }

  /**
   * @param jsonBloc
   *          The root node to set.
   */
  public void setJsonBloc( String jsonBloc ) {
    this.jsonBloc = jsonBloc;
  }

  /**
   * @return Returns the jsonBloc.
   */
  public String getNrRowsInBloc() {
    return nrRowsInBloc;
  }

  /**
   * @param nrRowsInBloc
   *          The nrRowsInBloc.
   */
  public void setNrRowsInBloc( String nrRowsInBloc ) {
    this.nrRowsInBloc = nrRowsInBloc;
  }

  public String getOutputValue() {
    return outputValue;
  }

  public void setOutputValue( String outputValue ) {
    this.outputValue = outputValue;
  }

  public boolean isServletOutput() {
    return servletOutput;
  }

  public void setServletOutput( boolean servletOutput ) {
    this.servletOutput = servletOutput;
  }

  public boolean isCompatibilityMode() {
    return compatibilityMode;
  }

  public void setCompatibilityMode( boolean compatibilityMode ) {
    this.compatibilityMode = compatibilityMode;
  }

  public StepMetaInjectionInterface getStepMetaInjectionInterface() {
    return new JsonOutputMetaInjection( this );
  }
}
