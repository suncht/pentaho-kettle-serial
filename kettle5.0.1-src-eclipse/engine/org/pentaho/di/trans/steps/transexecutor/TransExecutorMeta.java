/*******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2012 by Pentaho : http://www.pentaho.com
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

package org.pentaho.di.trans.steps.transexecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.ObjectLocationSpecificationMethod;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.HasRepositoryInterface;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.RepositoryImportLocation;
import org.pentaho.di.repository.RepositoryObject;
import org.pentaho.di.repository.RepositoryObjectType;
import org.pentaho.di.repository.StringObjectId;
import org.pentaho.di.resource.ResourceDefinition;
import org.pentaho.di.resource.ResourceEntry;
import org.pentaho.di.resource.ResourceEntry.ResourceType;
import org.pentaho.di.resource.ResourceNamingInterface;
import org.pentaho.di.resource.ResourceReference;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransMeta.TransformationType;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepIOMeta;
import org.pentaho.di.trans.step.StepIOMetaInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.step.errorhandling.Stream;
import org.pentaho.di.trans.step.errorhandling.StreamIcon;
import org.pentaho.di.trans.step.errorhandling.StreamInterface;
import org.pentaho.di.trans.step.errorhandling.StreamInterface.StreamType;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/**
 * Meta-data for the Trans Executor step.
 * 
 * @since 18-mar-2013
 * @author Matt
 * 
 */

public class TransExecutorMeta extends BaseStepMeta implements StepMetaInterface, HasRepositoryInterface {
  private static Class<?> PKG = TransExecutorMeta.class; // for i18n purposes, needed by Translator2!!

  private String transName;

  private String fileName;

  private String directoryPath;

  private ObjectId transObjectId;

  private ObjectLocationSpecificationMethod specificationMethod;

  /** The number of input rows that are sent as result rows to the job in one go, defaults to "1" */
  private String groupSize;

  /** Optional name of a field to group rows together that are sent together to the job as result rows (empty default) */
  private String groupField;

  /** Optional time in ms that is spent waiting and accumulating rows before they are given to the job as result rows (empty default, "0") */
  private String groupTime;

  private TransExecutorParameters parameters;

  private String executionResultTargetStep;

  private StepMeta executionResultTargetStepMeta;

  /** The optional name of the output field that will contain the execution time of the transformation in (Integer in ms) */
  private String executionTimeField;

  /** The optional name of the output field that will contain the execution result (Boolean) */
  private String executionResultField;

  /** The optional name of the output field that will contain the number of errors (Integer) */
  private String executionNrErrorsField;

  /** The optional name of the output field that will contain the number of rows read (Integer) */
  private String executionLinesReadField;

  /** The optional name of the output field that will contain the number of rows written (Integer) */
  private String executionLinesWrittenField;

  /** The optional name of the output field that will contain the number of rows input (Integer) */
  private String executionLinesInputField;

  /** The optional name of the output field that will contain the number of rows output (Integer) */
  private String executionLinesOutputField;

  /** The optional name of the output field that will contain the number of rows rejected (Integer) */
  private String executionLinesRejectedField;

  /** The optional name of the output field that will contain the number of rows updated (Integer) */
  private String executionLinesUpdatedField;

  /** The optional name of the output field that will contain the number of rows deleted (Integer) */
  private String executionLinesDeletedField;

  /** The optional name of the output field that will contain the number of files retrieved (Integer) */
  private String executionFilesRetrievedField;

  /** The optional name of the output field that will contain the exit status of the last executed shell script (Integer) */
  private String executionExitStatusField;

  /** The optional name of the output field that will contain the log text of the transformation execution (String) */
  private String executionLogTextField;

  /** The optional name of the output field that will contain the log channel ID of the transformation execution (String) */
  private String executionLogChannelIdField;

  /** The optional step to send the result rows to */
  private String outputRowsSourceStep;

  private StepMeta outputRowsSourceStepMeta;

  private String[] outputRowsField;

  private int[] outputRowsType;

  private int[] outputRowsLength;

  private int[] outputRowsPrecision;

  /** The optional step to send the result files to */
  private String resultFilesTargetStep;

  private StepMeta resultFilesTargetStepMeta;

  private String resultFilesFileNameField;

  /**
   * This repository object is injected from the outside at runtime or at design
   * time. It comes from either Spoon or Trans
   */
  private Repository repository;

  private IMetaStore metaStore;

  public TransExecutorMeta() {
    super(); // allocate BaseStepMeta

    parameters = new TransExecutorParameters();
    outputRowsField = new String[0];
  }

  public Object clone() {
    Object retval = super.clone();
    return retval;
  }

  public String getXML() {
    StringBuffer retval = new StringBuffer(300);

    retval.append("    ").append(
        XMLHandler.addTagValue("specification_method",
            specificationMethod == null ? null : specificationMethod.getCode()));
    retval.append("    ").append(
        XMLHandler.addTagValue("trans_object_id", transObjectId == null ? null : transObjectId.toString()));
    // Export a little bit of extra information regarding the reference since it doesn't really matter outside the same repository.
    //
    if (repository != null && transObjectId != null) {
      try {
        RepositoryObject objectInformation = repository.getObjectInformation(transObjectId,
            RepositoryObjectType.TRANSFORMATION);
        if (objectInformation != null) {
          transName = objectInformation.getName();
          directoryPath = objectInformation.getRepositoryDirectory().getPath();
        }
      } catch (KettleException e) {
        // Ignore object reference problems.  It simply means that the reference is no longer valid.
      }
    }
    retval.append("    ").append(XMLHandler.addTagValue("trans_name", transName)); 
    retval.append("    ").append(XMLHandler.addTagValue("filename", fileName)); 
    retval.append("    ").append(XMLHandler.addTagValue("directory_path", directoryPath)); 

    retval.append("    ").append(XMLHandler.addTagValue("group_size", groupSize)); 
    retval.append("    ").append(XMLHandler.addTagValue("group_field", groupField)); 
    retval.append("    ").append(XMLHandler.addTagValue("group_time", groupTime)); 

    // Add the mapping parameters too
    //
    retval.append("      ").append(parameters.getXML()).append(Const.CR); 

    // The output side...
    //
    retval
        .append("    ").append(XMLHandler.addTagValue("execution_result_target_step", executionResultTargetStepMeta == null ? null : executionResultTargetStepMeta.getName()));  
    retval.append("    ").append(XMLHandler.addTagValue("execution_time_field", executionTimeField));  
    retval.append("    ").append(XMLHandler.addTagValue("execution_result_field", executionResultField));  
    retval.append("    ").append(XMLHandler.addTagValue("execution_errors_field", executionNrErrorsField));  
    retval.append("    ").append(XMLHandler.addTagValue("execution_lines_read_field", executionLinesReadField));  
    retval.append("    ").append(XMLHandler.addTagValue("execution_lines_written_field", executionLinesWrittenField));  
    retval.append("    ").append(XMLHandler.addTagValue("execution_lines_input_field", executionLinesInputField));  
    retval.append("    ").append(XMLHandler.addTagValue("execution_lines_output_field", executionLinesOutputField));  
    retval.append("    ").append(XMLHandler.addTagValue("execution_lines_rejected_field", executionLinesRejectedField));  
    retval.append("    ").append(XMLHandler.addTagValue("execution_lines_updated_field", executionLinesUpdatedField));  
    retval.append("    ").append(XMLHandler.addTagValue("execution_lines_deleted_field", executionLinesDeletedField));  
    retval
        .append("    ").append(XMLHandler.addTagValue("execution_files_retrieved_field", executionFilesRetrievedField));  
    retval.append("    ").append(XMLHandler.addTagValue("execution_exit_status_field", executionExitStatusField));  
    retval.append("    ").append(XMLHandler.addTagValue("execution_log_text_field", executionLogTextField));  
    retval.append("    ").append(XMLHandler.addTagValue("execution_log_channelid_field", executionLogChannelIdField));  

    retval
        .append("    ").append(XMLHandler.addTagValue("result_rows_target_step", outputRowsSourceStepMeta == null ? null : outputRowsSourceStepMeta.getName()));  
    for (int i = 0; i < outputRowsField.length; i++) {
      retval.append("      ").append(XMLHandler.openTag("result_rows_field"));
      retval.append(XMLHandler.addTagValue("name", outputRowsField[i], false)); 
      retval.append(XMLHandler.addTagValue("type", ValueMeta.getTypeDesc(outputRowsType[i]), false)); 
      retval.append(XMLHandler.addTagValue("length", outputRowsLength[i], false)); 
      retval.append(XMLHandler.addTagValue("precision", outputRowsPrecision[i], false)); 
      retval.append(XMLHandler.closeTag("result_rows_field")).append(Const.CR);
    }

    retval
        .append("    ").append(XMLHandler.addTagValue("result_files_target_step", resultFilesTargetStepMeta == null ? null : resultFilesTargetStepMeta.getName()));  
    retval.append("    ").append(XMLHandler.addTagValue("result_files_file_name_field", resultFilesFileNameField));  

    return retval.toString();
  }

  public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {
    try {
      String method = XMLHandler.getTagValue(stepnode, "specification_method");
      specificationMethod = ObjectLocationSpecificationMethod.getSpecificationMethodByCode(method);
      String transId = XMLHandler.getTagValue(stepnode, "trans_object_id");
      transObjectId = Const.isEmpty(transId) ? null : new StringObjectId(transId);

      transName = XMLHandler.getTagValue(stepnode, "trans_name"); 
      fileName = XMLHandler.getTagValue(stepnode, "filename"); 
      directoryPath = XMLHandler.getTagValue(stepnode, "directory_path"); 

      groupSize = XMLHandler.getTagValue(stepnode, "group_size"); 
      groupField = XMLHandler.getTagValue(stepnode, "group_field"); 
      groupTime = XMLHandler.getTagValue(stepnode, "group_time"); 

      // Load the mapping parameters too..
      //
      Node mappingParametersNode = XMLHandler.getSubNode(stepnode, TransExecutorParameters.XML_TAG);
      parameters = new TransExecutorParameters(mappingParametersNode);

      // The output side...
      //
      executionResultTargetStep = XMLHandler.getTagValue(stepnode, "execution_result_target_step"); 
      executionTimeField = XMLHandler.getTagValue(stepnode, "execution_time_field"); 
      executionResultField = XMLHandler.getTagValue(stepnode, "execution_result_field"); 
      executionNrErrorsField = XMLHandler.getTagValue(stepnode, "execution_errors_field"); 
      executionLinesReadField = XMLHandler.getTagValue(stepnode, "execution_lines_read_field"); 
      executionLinesWrittenField = XMLHandler.getTagValue(stepnode, "execution_lines_written_field"); 
      executionLinesInputField = XMLHandler.getTagValue(stepnode, "execution_lines_input_field"); 
      executionLinesOutputField = XMLHandler.getTagValue(stepnode, "execution_lines_output_field"); 
      executionLinesRejectedField = XMLHandler.getTagValue(stepnode, "execution_lines_rejected_field"); 
      executionLinesUpdatedField = XMLHandler.getTagValue(stepnode, "execution_lines_updated_field"); 
      executionLinesDeletedField = XMLHandler.getTagValue(stepnode, "execution_lines_deleted_field"); 
      executionFilesRetrievedField = XMLHandler.getTagValue(stepnode, "execution_files_retrieved_field"); 
      executionExitStatusField = XMLHandler.getTagValue(stepnode, "execution_exit_status_field"); 
      executionLogTextField = XMLHandler.getTagValue(stepnode, "execution_log_text_field"); 
      executionLogChannelIdField = XMLHandler.getTagValue(stepnode, "execution_log_channelid_field"); 

      outputRowsSourceStep = XMLHandler.getTagValue(stepnode, "result_rows_target_step"); 

      int nrFields = XMLHandler.countNodes(stepnode, "result_rows_field");
      outputRowsField = new String[nrFields];
      outputRowsType = new int[nrFields];
      outputRowsLength = new int[nrFields];
      outputRowsPrecision = new int[nrFields];

      for (int i = 0; i < nrFields; i++) {

        Node fieldNode = XMLHandler.getSubNodeByNr(stepnode, "result_rows_field", i);

        outputRowsField[i] = XMLHandler.getTagValue(fieldNode, "name"); 
        outputRowsType[i] = ValueMeta.getType(XMLHandler.getTagValue(fieldNode, "type")); 
        outputRowsLength[i] = Const.toInt(XMLHandler.getTagValue(fieldNode, "length"), -1); 
        outputRowsPrecision[i] = Const.toInt(XMLHandler.getTagValue(fieldNode, "precision"), -1); 
      }

      resultFilesTargetStep = XMLHandler.getTagValue(stepnode, "result_files_target_step"); 
      resultFilesFileNameField = XMLHandler.getTagValue(stepnode, "result_files_file_name_field"); 
    } catch (Exception e) {
      throw new KettleXMLException(BaseMessages.getString(PKG,
          "TransExecutorMeta.Exception.ErrorLoadingTransExecutorDetailsFromXML"), e); 
    }
  }

  public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases)
      throws KettleException {
    String method = rep.getStepAttributeString(id_step, "specification_method");
    specificationMethod = ObjectLocationSpecificationMethod.getSpecificationMethodByCode(method);
    String transId = rep.getStepAttributeString(id_step, "trans_object_id");
    transObjectId = Const.isEmpty(transId) ? null : new StringObjectId(transId);
    transName = rep.getStepAttributeString(id_step, "trans_name"); 
    fileName = rep.getStepAttributeString(id_step, "filename"); 
    directoryPath = rep.getStepAttributeString(id_step, "directory_path"); 

    groupSize = rep.getStepAttributeString(id_step, "group_size"); 
    groupField = rep.getStepAttributeString(id_step, "group_field"); 
    groupTime = rep.getStepAttributeString(id_step, "group_time"); 

    parameters = new TransExecutorParameters(rep, id_step);

    executionResultTargetStep = rep.getStepAttributeString(id_step, "execution_result_target_step"); 
    executionTimeField = rep.getStepAttributeString(id_step, "execution_time_field"); 
    executionNrErrorsField = rep.getStepAttributeString(id_step, "execution_result_field"); 
    executionLinesReadField = rep.getStepAttributeString(id_step, "execution_errors_field"); 
    executionLinesWrittenField = rep.getStepAttributeString(id_step, "execution_lines_written_field"); 
    executionLinesInputField = rep.getStepAttributeString(id_step, "execution_lines_input_field"); 
    executionLinesOutputField = rep.getStepAttributeString(id_step, "execution_lines_output_field"); 
    executionLinesRejectedField = rep.getStepAttributeString(id_step, "execution_lines_rejected_field"); 
    executionLinesUpdatedField = rep.getStepAttributeString(id_step, "execution_lines_updated_field"); 
    executionLinesDeletedField = rep.getStepAttributeString(id_step, "execution_lines_deleted_field"); 
    executionFilesRetrievedField = rep.getStepAttributeString(id_step, "execution_files_retrieved_field"); 
    executionExitStatusField = rep.getStepAttributeString(id_step, "execution_exit_status_field"); 
    executionLogTextField = rep.getStepAttributeString(id_step, "execution_log_text_field"); 
    executionLogChannelIdField = rep.getStepAttributeString(id_step, "execution_log_channelid_field"); 

    outputRowsSourceStep = rep.getStepAttributeString(id_step, "result_rows_target_step"); 
    int nrFields = rep.countNrStepAttributes(id_step, "result_rows_field");
    outputRowsField = new String[nrFields];
    outputRowsType = new int[nrFields];
    outputRowsLength = new int[nrFields];
    outputRowsPrecision = new int[nrFields];

    for (int i = 0; i < nrFields; i++) {
      outputRowsField[i] = rep.getStepAttributeString(id_step, i, "result_rows_field");
      outputRowsType[i] = ValueMeta.getType(rep.getStepAttributeString(id_step, i, "result_rows_type"));
      outputRowsLength[i] = (int) rep.getStepAttributeInteger(id_step, i, "result_rows_length");
      outputRowsPrecision[i] = (int) rep.getStepAttributeInteger(id_step, i, "result_rows_precision");
    }

    resultFilesTargetStep = rep.getStepAttributeString(id_step, "result_files_target_step"); 
    resultFilesFileNameField = rep.getStepAttributeString(id_step, "result_files_file_name_field"); 
  }

  public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step)
      throws KettleException {
    rep.saveStepAttribute(id_transformation, id_step, "specification_method", specificationMethod == null ? null
        : specificationMethod.getCode());
    rep.saveStepAttribute(id_transformation, id_step, "trans_object_id",
        transObjectId == null ? null : transObjectId.toString());
    rep.saveStepAttribute(id_transformation, id_step, "filename", fileName); 
    rep.saveStepAttribute(id_transformation, id_step, "trans_name", transName); 
    rep.saveStepAttribute(id_transformation, id_step, "directory_path", directoryPath); 

    rep.saveStepAttribute(id_transformation, id_step, "group_size", groupSize); 
    rep.saveStepAttribute(id_transformation, id_step, "group_field", groupField); 
    rep.saveStepAttribute(id_transformation, id_step, "group_time", groupTime); 

    // save the mapping parameters too
    //
    parameters.saveRep(rep, metaStore, id_transformation, id_step);

    // The output side...
    //
    rep.saveStepAttribute(
        id_transformation,
        id_step,
        "execution_result_target_step", executionResultTargetStepMeta == null ? null : executionResultTargetStepMeta.getName()); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_time_field", executionTimeField); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_result_field", executionResultField); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_errors_field", executionNrErrorsField); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_lines_read_field", executionLinesReadField); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_lines_written_field", executionLinesWrittenField); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_lines_input_field", executionLinesInputField); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_lines_output_field", executionLinesOutputField); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_lines_rejected_field", executionLinesRejectedField); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_lines_updated_field", executionLinesUpdatedField); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_lines_deleted_field", executionLinesDeletedField); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_files_retrieved_field", executionFilesRetrievedField); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_exit_status_field", executionExitStatusField); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_log_text_field", executionLogTextField); 
    rep.saveStepAttribute(id_transformation, id_step, "execution_log_channelid_field", executionLogChannelIdField); 

    rep.saveStepAttribute(id_transformation, id_step,
        "result_rows_target_step", outputRowsSourceStepMeta == null ? null : outputRowsSourceStepMeta.getName()); 

    for (int i = 0; i < outputRowsField.length; i++) {
      rep.saveStepAttribute(id_transformation, id_step, i, "result_rows_field_name", outputRowsField[i]);
      rep.saveStepAttribute(id_transformation, id_step, i, "result_rows_field_type",
          ValueMeta.getTypeDesc(outputRowsType[i]));
      rep.saveStepAttribute(id_transformation, id_step, i, "result_rows_field_length", outputRowsLength[i]);
      rep.saveStepAttribute(id_transformation, id_step, i, "result_rows_field_precision", outputRowsPrecision[i]);
    }

    rep.saveStepAttribute(id_transformation, id_step,
        "result_files_target_step", resultFilesTargetStepMeta == null ? null : resultFilesTargetStepMeta.getName()); 
    rep.saveStepAttribute(id_transformation, id_step, "result_files_file_name_field", resultFilesFileNameField); 
  }

  public void setDefault() {
    specificationMethod = ObjectLocationSpecificationMethod.FILENAME;
    parameters = new TransExecutorParameters();
    parameters.setInheritingAllVariables(true);

    groupSize = "1";
    groupField = "";
    groupTime = "";

    executionTimeField = "ExecutionTime";  
    executionResultField = "ExecutionResult";  
    executionNrErrorsField = "ExecutionNrErrors";  
    executionLinesReadField = "ExecutionLinesRead"; 
    executionLinesWrittenField = "ExecutionLinesWritten"; 
    executionLinesInputField = "ExecutionLinesInput";  
    executionLinesOutputField = "ExecutionLinesOutput"; 
    executionLinesRejectedField = "ExecutionLinesRejected"; 
    executionLinesUpdatedField = "ExecutionLinesUpdated"; 
    executionLinesDeletedField = "ExecutionLinesDeleted"; 
    executionFilesRetrievedField = "ExecutionFilesRetrieved"; 
    executionExitStatusField = "ExecutionExitStatus"; 
    executionLogTextField = "ExecutionLogText"; 
    executionLogChannelIdField = "ExecutionLogChannelId"; 

    resultFilesFileNameField = "FileName";
  }

  public void getFields(RowMetaInterface row, String origin, RowMetaInterface info[], StepMeta nextStep, 
      VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException {

    row.clear();
    
    if (nextStep == null && outputRowsSourceStepMeta != null) {
      
      for (int i = 0; i < outputRowsField.length; i++) {
        ValueMetaInterface value = new ValueMeta(outputRowsField[i], outputRowsType[i], outputRowsLength[i],
            outputRowsPrecision[i]);
        row.addValueMeta(value);
      }
    } else if (nextStep != null && resultFilesTargetStepMeta != null && nextStep.equals(resultFilesTargetStepMeta)) {
      if (!Const.isEmpty(resultFilesFileNameField)) {
        ValueMetaInterface value = new ValueMeta("filename", ValueMeta.TYPE_STRING, 255, 0);
        row.addValueMeta(value);
      }
    } else if (nextStep != null && executionResultTargetStepMeta != null
        && nextStep.equals(executionResultTargetStepMeta)) {
      if (!Const.isEmpty(executionTimeField)) {
        ValueMetaInterface value = new ValueMeta(executionTimeField, ValueMeta.TYPE_INTEGER, 15, 0);
        row.addValueMeta(value);
      }
      if (!Const.isEmpty(executionResultField)) {
        ValueMetaInterface value = new ValueMeta(executionResultField, ValueMeta.TYPE_BOOLEAN);
        row.addValueMeta(value);
      }
      if (!Const.isEmpty(executionNrErrorsField)) {
        ValueMetaInterface value = new ValueMeta(executionNrErrorsField, ValueMeta.TYPE_INTEGER, 9, 0);
        row.addValueMeta(value);
      }
      if (!Const.isEmpty(executionLinesReadField)) {
        ValueMetaInterface value = new ValueMeta(executionLinesReadField, ValueMeta.TYPE_INTEGER, 9, 0);
        row.addValueMeta(value);
      }
      if (!Const.isEmpty(executionLinesWrittenField)) {
        ValueMetaInterface value = new ValueMeta(executionLinesWrittenField, ValueMeta.TYPE_INTEGER, 9, 0);
        row.addValueMeta(value);
      }
      if (!Const.isEmpty(executionLinesInputField)) {
        ValueMetaInterface value = new ValueMeta(executionLinesInputField, ValueMeta.TYPE_INTEGER, 9, 0);
        row.addValueMeta(value);
      }
      if (!Const.isEmpty(executionLinesOutputField)) {
        ValueMetaInterface value = new ValueMeta(executionLinesOutputField, ValueMeta.TYPE_INTEGER, 9, 0);
        row.addValueMeta(value);
      }
      if (!Const.isEmpty(executionLinesRejectedField)) {
        ValueMetaInterface value = new ValueMeta(executionLinesRejectedField, ValueMeta.TYPE_INTEGER, 9, 0);
        row.addValueMeta(value);
      }
      if (!Const.isEmpty(executionLinesUpdatedField)) {
        ValueMetaInterface value = new ValueMeta(executionLinesUpdatedField, ValueMeta.TYPE_INTEGER, 9, 0);
        row.addValueMeta(value);
      }
      if (!Const.isEmpty(executionLinesDeletedField)) {
        ValueMetaInterface value = new ValueMeta(executionLinesDeletedField, ValueMeta.TYPE_INTEGER, 9, 0);
        row.addValueMeta(value);
      }
      if (!Const.isEmpty(executionFilesRetrievedField)) {
        ValueMetaInterface value = new ValueMeta(executionFilesRetrievedField, ValueMeta.TYPE_INTEGER, 9, 0);
        row.addValueMeta(value);
      }
      if (!Const.isEmpty(executionExitStatusField)) {
        ValueMetaInterface value = new ValueMeta(executionExitStatusField, ValueMeta.TYPE_INTEGER, 3, 0);
        row.addValueMeta(value);
      }
      if (!Const.isEmpty(executionLogTextField)) {
        ValueMetaInterface value = new ValueMeta(executionLogTextField, ValueMeta.TYPE_STRING);
        value.setLargeTextField(true);
        row.addValueMeta(value);
      }
      if (!Const.isEmpty(executionLogChannelIdField)) {
        ValueMetaInterface value = new ValueMeta(executionLogChannelIdField, ValueMeta.TYPE_STRING, 50, 0);
        row.addValueMeta(value);
      }
    }
  }

  public String[] getInfoSteps() {
    String[] infoSteps = getStepIOMeta().getInfoStepnames();
    // Return null instead of empty array to preserve existing behavior
    return infoSteps.length == 0 ? null : infoSteps;
  }

  public String[] getTargetSteps() {

    List<String> targetSteps = new ArrayList<String>();

    if (!Const.isEmpty(resultFilesTargetStep)) {
      targetSteps.add(resultFilesTargetStep);
    }
    if (!Const.isEmpty(outputRowsSourceStep)) {
      targetSteps.add(outputRowsSourceStep);
    }

    if (targetSteps.isEmpty())
      return null;

    return targetSteps.toArray(new String[targetSteps.size()]);
  }

  @Deprecated
  public synchronized static final TransMeta loadTransMeta(TransExecutorMeta executorMeta, Repository rep,
      VariableSpace space) throws KettleException {
    return loadTransMeta(executorMeta, rep, null, space);
  }

  public synchronized static final TransMeta loadTransMeta(TransExecutorMeta executorMeta, Repository rep,
      IMetaStore metaStore, VariableSpace space) throws KettleException {
    TransMeta mappingTransMeta = null;

    switch (executorMeta.getSpecificationMethod()) {
      case FILENAME:
        String realFilename = space.environmentSubstitute(executorMeta.getFileName());
        try {
          // OK, load the meta-data from file...
          //
          // Don't set internal variables: they belong to the parent thread!
          //
          mappingTransMeta = new TransMeta(realFilename, metaStore, rep, true, space, null);
          LogChannel.GENERAL.logDetailed("Loading transformation from repository",
              "Transformation was loaded from XML file [" + realFilename + "]");
        } catch (Exception e) {
          throw new KettleException(BaseMessages.getString(PKG, "TransExecutorMeta.Exception.UnableToLoadTrans"), e);
        }
        break;

      case REPOSITORY_BY_NAME:
        String realTransname = space.environmentSubstitute(executorMeta.getTransName());
        String realDirectory = space.environmentSubstitute(executorMeta.getDirectoryPath());

        if (!Const.isEmpty(realTransname) && !Const.isEmpty(realDirectory) && rep != null) {
          RepositoryDirectoryInterface repdir = rep.findDirectory(realDirectory);
          if (repdir != null) {
            try {
              // reads the last revision in the repository...
              //
              mappingTransMeta = rep.loadTransformation(realTransname, repdir, null, true, null); // TODO: FIXME: pass in metaStore to repository?

              LogChannel.GENERAL.logDetailed("Loading transformation from repository", "Executor transformation ["
                  + realTransname + "] was loaded from the repository");
            } catch (Exception e) {
              throw new KettleException("Unable to load transformation [" + realTransname + "]", e);
            }
          } else {
            throw new KettleException(BaseMessages.getString(PKG,
                "TransExecutorMeta.Exception.UnableToLoadTrans", realTransname) + realDirectory);  
          }
        }
        break;

      case REPOSITORY_BY_REFERENCE:
        // Read the last revision by reference...
        mappingTransMeta = rep.loadTransformation(executorMeta.getTransObjectId(), null);
        break;
      default:
        break;
    }

    // Pass some important information to the mapping transformation metadata:
    //
    mappingTransMeta.copyVariablesFrom(space);
    mappingTransMeta.setRepository(rep);
    mappingTransMeta.setMetaStore(metaStore);
    mappingTransMeta.setFilename(mappingTransMeta.getFilename());

    return mappingTransMeta;
  }

  public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepinfo, RowMetaInterface prev,
      String input[], String output[], RowMetaInterface info, VariableSpace space, Repository repository, IMetaStore metaStore) {
    CheckResult cr;
    if (prev == null || prev.size() == 0) {
      cr = new CheckResult(CheckResultInterface.TYPE_RESULT_WARNING, BaseMessages.getString(PKG,
          "TransExecutorMeta.CheckResult.NotReceivingAnyFields"), stepinfo); 
      remarks.add(cr);
    } else {
      cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG,
          "TransExecutorMeta.CheckResult.StepReceivingFields", prev.size() + ""), stepinfo);  
      remarks.add(cr);
    }

    // See if we have input streams leading to this step!
    if (input.length > 0) {
      cr = new CheckResult(CheckResultInterface.TYPE_RESULT_OK, BaseMessages.getString(PKG,
          "TransExecutorMeta.CheckResult.StepReceivingFieldsFromOtherSteps"), stepinfo); 
      remarks.add(cr);
    } else {
      cr = new CheckResult(CheckResultInterface.TYPE_RESULT_ERROR, BaseMessages.getString(PKG,
          "TransExecutorMeta.CheckResult.NoInputReceived"), stepinfo); 
      remarks.add(cr);
    }
  }

  public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta tr,
      Trans trans) {
    return new TransExecutor(stepMeta, stepDataInterface, cnr, tr, trans);
  }

  @Override
  public List<ResourceReference> getResourceDependencies(TransMeta transMeta, StepMeta stepInfo) {
    List<ResourceReference> references = new ArrayList<ResourceReference>(5);
    String realFilename = transMeta.environmentSubstitute(fileName);
    String realTransname = transMeta.environmentSubstitute(transName);
    ResourceReference reference = new ResourceReference(stepInfo);
    references.add(reference);

    if (!Const.isEmpty(realFilename)) {
      // Add the filename to the references, including a reference to this step
      // meta data.
      //
      reference.getEntries().add(new ResourceEntry(realFilename, ResourceType.ACTIONFILE));
    } else if (!Const.isEmpty(realTransname)) {
      // Add the filename to the references, including a reference to this step
      // meta data.
      //
      reference.getEntries().add(new ResourceEntry(realTransname, ResourceType.ACTIONFILE));
      references.add(reference);
    }
    return references;
  }

  @Override
  public String exportResources(VariableSpace space, Map<String, ResourceDefinition> definitions,
      ResourceNamingInterface resourceNamingInterface, Repository repository, IMetaStore metaStore) throws KettleException {
    try {
      // Try to load the transformation from repository or file.
      // Modify this recursively too...
      // 
      // NOTE: there is no need to clone this step because the caller is
      // responsible for this.
      //
      // First load the executor transformation metadata...
      //
      TransMeta executorTransMeta = loadTransMeta(this, repository, space);

      // Also go down into the mapping transformation and export the files
      // there. (mapping recursively down)
      //
      String proposedNewFilename = executorTransMeta.exportResources(executorTransMeta, definitions,
          resourceNamingInterface, repository, metaStore);

      // To get a relative path to it, we inject
      // ${Internal.Transformation.Filename.Directory}
      //
      String newFilename = "${" + Const.INTERNAL_VARIABLE_TRANSFORMATION_FILENAME_DIRECTORY + "}/"
          + proposedNewFilename;

      // Set the correct filename inside the XML.
      //
      executorTransMeta.setFilename(newFilename);

      // exports always reside in the root directory, in case we want to turn
      // this into a file repository...
      //
      executorTransMeta.setRepositoryDirectory(new RepositoryDirectory());

      // change it in the entry
      //
      fileName = newFilename;

      return proposedNewFilename;
    } catch (Exception e) {
      throw new KettleException(BaseMessages.getString(PKG, "TransExecutorMeta.Exception.UnableToLoadTrans", fileName)); 
    }
  }

  public StepDataInterface getStepData() {
    return new TransExecutorData();
  }

  @Override
  public StepIOMetaInterface getStepIOMeta() {
    if (ioMeta == null) {

      ioMeta = new StepIOMeta(true, true, true, false, true, false);

      ioMeta.addStream(new Stream(StreamType.TARGET, executionResultTargetStepMeta, BaseMessages.getString(PKG,
          "TransExecutorMeta.ResultStream.Description"), StreamIcon.TARGET, null));
      ioMeta.addStream(new Stream(StreamType.TARGET, outputRowsSourceStepMeta, BaseMessages.getString(PKG,
          "TransExecutorMeta.ResultRowsStream.Description"), StreamIcon.TARGET, null));
      ioMeta.addStream(new Stream(StreamType.TARGET, resultFilesTargetStepMeta, BaseMessages.getString(PKG,
          "TransExecutorMeta.ResultFilesStream.Description"), StreamIcon.TARGET, null));
    }
    return ioMeta;
  }

  /**
   * When an optional stream is selected, this method is called to handled the ETL metadata implications of that.
   * @param stream The optional stream to handle.
   */
  public void handleStreamSelection(StreamInterface stream) {
    // This step targets another step.
    // Make sure that we don't specify the same step for more than 1 target...
    //
    List<StreamInterface> targets = getStepIOMeta().getTargetStreams();
    int index = targets.indexOf(stream);
    StepMeta step = targets.get(index).getStepMeta();
    switch (index) {
      case 0:
        setExecutionResultTargetStepMeta(step);
        break;
      case 1:
        setOutputRowsSourceStepMeta(step);
        break;
      case 2:
        setResultFilesTargetStepMeta(step);
        break;
      default:
        break;
    }

  }

  /**
   * Remove the cached {@link StepIOMeta} so it is recreated when it is next accessed.
   */
  public void resetStepIoMeta() {
  }

  @Override
  public void searchInfoAndTargetSteps(List<StepMeta> steps) {
    executionResultTargetStepMeta = StepMeta.findStep(steps, executionResultTargetStep);
    outputRowsSourceStepMeta = StepMeta.findStep(steps, outputRowsSourceStep);
    resultFilesTargetStepMeta = StepMeta.findStep(steps, resultFilesTargetStep);
  }

  public TransformationType[] getSupportedTransformationTypes() {
    return new TransformationType[] { TransformationType.Normal, };
  }

  @Override
  public boolean hasRepositoryReferences() {
    return specificationMethod == ObjectLocationSpecificationMethod.REPOSITORY_BY_REFERENCE;
  }

  @Override
  public void lookupRepositoryReferences(Repository repository) throws KettleException {
    // The correct reference is stored in the trans name and directory attributes...
    //
    RepositoryDirectoryInterface repositoryDirectoryInterface = RepositoryImportLocation.getRepositoryImportLocation()
        .findDirectory(directoryPath);
    transObjectId = repository.getTransformationID(transName, repositoryDirectoryInterface);
  }

  /**
   * @return the directoryPath
   */
  public String getDirectoryPath() {
    return directoryPath;
  }

  /**
   * @param directoryPath
   *          the directoryPath to set
   */
  public void setDirectoryPath(String directoryPath) {
    this.directoryPath = directoryPath;
  }

  /**
   * @return the fileName
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * @param fileName
   *          the fileName to set
   */
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  /**
   * @return the mappingParameters
   */
  public TransExecutorParameters getMappingParameters() {
    return parameters;
  }

  /**
   * @param mappingParameters
   *          the mappingParameters to set
   */
  public void setMappingParameters(TransExecutorParameters mappingParameters) {
    this.parameters = mappingParameters;
  }

  /**
   * @return the repository
   */
  public Repository getRepository() {
    return repository;
  }

  /**
   * @param repository
   *          the repository to set
   */
  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  /**
   * @return the specificationMethod
   */
  public ObjectLocationSpecificationMethod getSpecificationMethod() {
    return specificationMethod;
  }

  /**
   * @param specificationMethod
   *          the specificationMethod to set
   */
  public void setSpecificationMethod(ObjectLocationSpecificationMethod specificationMethod) {
    this.specificationMethod = specificationMethod;
  }

  /**
   * @return the transName
   */
  public String getTransName() {
    return transName;
  }

  /**
   * @param transName the transName to set
   */
  public void setTransName(String transName) {
    this.transName = transName;
  }

  /**
   * @return the transObjectId
   */
  public ObjectId getTransObjectId() {
    return transObjectId;
  }

  /**
   * @param transObjectId the transObjectId to set
   */
  public void setTransObjectId(ObjectId transObjectId) {
    this.transObjectId = transObjectId;
  }

  /**
   * @return the parameters
   */
  public TransExecutorParameters getParameters() {
    return parameters;
  }

  /**
   * @param parameters the parameters to set
   */
  public void setParameters(TransExecutorParameters parameters) {
    this.parameters = parameters;
  }

  /**
   * @return the executionTimeField
   */
  public String getExecutionTimeField() {
    return executionTimeField;
  }

  /**
   * @param executionTimeField the executionTimeField to set
   */
  public void setExecutionTimeField(String executionTimeField) {
    this.executionTimeField = executionTimeField;
  }

  /**
   * @return the executionResultField
   */
  public String getExecutionResultField() {
    return executionResultField;
  }

  /**
   * @param executionResultField the executionResultField to set
   */
  public void setExecutionResultField(String executionResultField) {
    this.executionResultField = executionResultField;
  }

  /**
   * @return the executionNrErrorsField
   */
  public String getExecutionNrErrorsField() {
    return executionNrErrorsField;
  }

  /**
   * @param executionNrErrorsField the executionNrErrorsField to set
   */
  public void setExecutionNrErrorsField(String executionNrErrorsField) {
    this.executionNrErrorsField = executionNrErrorsField;
  }

  /**
   * @return the executionLinesReadField
   */
  public String getExecutionLinesReadField() {
    return executionLinesReadField;
  }

  /**
   * @param executionLinesReadField the executionLinesReadField to set
   */
  public void setExecutionLinesReadField(String executionLinesReadField) {
    this.executionLinesReadField = executionLinesReadField;
  }

  /**
   * @return the executionLinesWrittenField
   */
  public String getExecutionLinesWrittenField() {
    return executionLinesWrittenField;
  }

  /**
   * @param executionLinesWrittenField the executionLinesWrittenField to set
   */
  public void setExecutionLinesWrittenField(String executionLinesWrittenField) {
    this.executionLinesWrittenField = executionLinesWrittenField;
  }

  /**
   * @return the executionLinesInputField
   */
  public String getExecutionLinesInputField() {
    return executionLinesInputField;
  }

  /**
   * @param executionLinesInputField the executionLinesInputField to set
   */
  public void setExecutionLinesInputField(String executionLinesInputField) {
    this.executionLinesInputField = executionLinesInputField;
  }

  /**
   * @return the executionLinesOutputField
   */
  public String getExecutionLinesOutputField() {
    return executionLinesOutputField;
  }

  /**
   * @param executionLinesOutputField the executionLinesOutputField to set
   */
  public void setExecutionLinesOutputField(String executionLinesOutputField) {
    this.executionLinesOutputField = executionLinesOutputField;
  }

  /**
   * @return the executionLinesRejectedField
   */
  public String getExecutionLinesRejectedField() {
    return executionLinesRejectedField;
  }

  /**
   * @param executionLinesRejectedField the executionLinesRejectedField to set
   */
  public void setExecutionLinesRejectedField(String executionLinesRejectedField) {
    this.executionLinesRejectedField = executionLinesRejectedField;
  }

  /**
   * @return the executionLinesUpdatedField
   */
  public String getExecutionLinesUpdatedField() {
    return executionLinesUpdatedField;
  }

  /**
   * @param executionLinesUpdatedField the executionLinesUpdatedField to set
   */
  public void setExecutionLinesUpdatedField(String executionLinesUpdatedField) {
    this.executionLinesUpdatedField = executionLinesUpdatedField;
  }

  /**
   * @return the executionLinesDeletedField
   */
  public String getExecutionLinesDeletedField() {
    return executionLinesDeletedField;
  }

  /**
   * @param executionLinesDeletedField the executionLinesDeletedField to set
   */
  public void setExecutionLinesDeletedField(String executionLinesDeletedField) {
    this.executionLinesDeletedField = executionLinesDeletedField;
  }

  /**
   * @return the executionFilesRetrievedField
   */
  public String getExecutionFilesRetrievedField() {
    return executionFilesRetrievedField;
  }

  /**
   * @param executionFilesRetrievedField the executionFilesRetrievedField to set
   */
  public void setExecutionFilesRetrievedField(String executionFilesRetrievedField) {
    this.executionFilesRetrievedField = executionFilesRetrievedField;
  }

  /**
   * @return the executionExitStatusField
   */
  public String getExecutionExitStatusField() {
    return executionExitStatusField;
  }

  /**
   * @param executionExitStatusField the executionExitStatusField to set
   */
  public void setExecutionExitStatusField(String executionExitStatusField) {
    this.executionExitStatusField = executionExitStatusField;
  }

  /**
   * @return the executionLogTextField
   */
  public String getExecutionLogTextField() {
    return executionLogTextField;
  }

  /**
   * @param executionLogTextField the executionLogTextField to set
   */
  public void setExecutionLogTextField(String executionLogTextField) {
    this.executionLogTextField = executionLogTextField;
  }

  /**
   * @return the executionLogChannelIdField
   */
  public String getExecutionLogChannelIdField() {
    return executionLogChannelIdField;
  }

  /**
   * @param executionLogChannelIdField the executionLogChannelIdField to set
   */
  public void setExecutionLogChannelIdField(String executionLogChannelIdField) {
    this.executionLogChannelIdField = executionLogChannelIdField;
  }

  /**
   * @return the groupSize
   */
  public String getGroupSize() {
    return groupSize;
  }

  /**
   * @param groupSize the groupSize to set
   */
  public void setGroupSize(String groupSize) {
    this.groupSize = groupSize;
  }

  /**
   * @return the groupField
   */
  public String getGroupField() {
    return groupField;
  }

  /**
   * @param groupField the groupField to set
   */
  public void setGroupField(String groupField) {
    this.groupField = groupField;
  }

  /**
   * @return the groupTime
   */
  public String getGroupTime() {
    return groupTime;
  }

  /**
   * @param groupTime the groupTime to set
   */
  public void setGroupTime(String groupTime) {
    this.groupTime = groupTime;
  }

  @Override
  public boolean excludeFromCopyDistributeVerification() {
    return true;
  }

  /**
   * @return the executionResultTargetStep
   */
  public String getExecutionResultTargetStep() {
    return executionResultTargetStep;
  }

  /**
   * @param executionResultTargetStep the executionResultTargetStep to set
   */
  public void setExecutionResultTargetStep(String executionResultTargetStep) {
    this.executionResultTargetStep = executionResultTargetStep;
  }

  /**
   * @return the executionResultTargetStepMeta
   */
  public StepMeta getExecutionResultTargetStepMeta() {
    return executionResultTargetStepMeta;
  }

  /**
   * @param executionResultTargetStepMeta the executionResultTargetStepMeta to set
   */
  public void setExecutionResultTargetStepMeta(StepMeta executionResultTargetStepMeta) {
    this.executionResultTargetStepMeta = executionResultTargetStepMeta;
  }

  /**
   * @return the resultFilesFileNameField
   */
  public String getResultFilesFileNameField() {
    return resultFilesFileNameField;
  }

  /**
   * @param resultFilesFileNameField the resultFilesFileNameField to set
   */
  public void setResultFilesFileNameField(String resultFilesFileNameField) {
    this.resultFilesFileNameField = resultFilesFileNameField;
  }

  /**
   * @return The objects referenced in the step, like a mapping, a transformation, ... 
   */
  public String[] getReferencedObjectDescriptions() {
    return new String[] { BaseMessages.getString(PKG, "TransExecutorMeta.ReferencedObject.Description"), };
  }

  private boolean isTransDefined() {
    return !Const.isEmpty(fileName) || transObjectId != null
        || (!Const.isEmpty(this.directoryPath) && !Const.isEmpty(transName));
  }

  public boolean[] isReferencedObjectEnabled() {
    return new boolean[] { isTransDefined(), };
  }

  /**
   * Load the referenced object
   * @param index the object index to load
   * @param rep the repository
   * @param space the variable space to use
   * @return the referenced object once loaded
   * @throws KettleException
   */
  public Object loadReferencedObject(int index, Repository rep, IMetaStore metaStore, VariableSpace space)
      throws KettleException {
    return loadTransMeta(this, rep, metaStore, space);
  }

  public IMetaStore getMetaStore() {
    return metaStore;
  }

  public void setMetaStore(IMetaStore metaStore) {
    this.metaStore = metaStore;
  }

  public String getOutputRowsSourceStep() {
    return outputRowsSourceStep;
  }

  public void setOutputRowsSourceStep(String outputRowsSourceStep) {
    this.outputRowsSourceStep = outputRowsSourceStep;
  }

  public StepMeta getOutputRowsSourceStepMeta() {
    return outputRowsSourceStepMeta;
  }

  public void setOutputRowsSourceStepMeta(StepMeta outputRowsSourceStepMeta) {
    this.outputRowsSourceStepMeta = outputRowsSourceStepMeta;
  }

  public String[] getOutputRowsField() {
    return outputRowsField;
  }

  public void setOutputRowsField(String[] outputRowsField) {
    this.outputRowsField = outputRowsField;
  }

  public int[] getOutputRowsType() {
    return outputRowsType;
  }

  public void setOutputRowsType(int[] outputRowsType) {
    this.outputRowsType = outputRowsType;
  }

  public int[] getOutputRowsLength() {
    return outputRowsLength;
  }

  public void setOutputRowsLength(int[] outputRowsLength) {
    this.outputRowsLength = outputRowsLength;
  }

  public int[] getOutputRowsPrecision() {
    return outputRowsPrecision;
  }

  public void setOutputRowsPrecision(int[] outputRowsPrecision) {
    this.outputRowsPrecision = outputRowsPrecision;
  }

  public String getResultFilesTargetStep() {
    return resultFilesTargetStep;
  }

  public void setResultFilesTargetStep(String resultFilesTargetStep) {
    this.resultFilesTargetStep = resultFilesTargetStep;
  }

  public StepMeta getResultFilesTargetStepMeta() {
    return resultFilesTargetStepMeta;
  }

  public void setResultFilesTargetStepMeta(StepMeta resultFilesTargetStepMeta) {
    this.resultFilesTargetStepMeta = resultFilesTargetStepMeta;
  }

}
