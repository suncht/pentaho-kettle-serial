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

package org.pentaho.di.repository;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.pentaho.di.cluster.ClusterSchema;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.ObjectLocationSpecificationMethod;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.gui.SpoonFactory;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.imp.ImportRules;
import org.pentaho.di.imp.rule.ImportValidationFeedback;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.job.JobEntryJob;
import org.pentaho.di.job.entries.trans.JobEntryTrans;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.partition.PartitionSchema;
import org.pentaho.di.shared.SharedObjectInterface;
import org.pentaho.di.shared.SharedObjects;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.mapping.MappingMeta;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;

public class RepositoryImporter implements IRepositoryImporter {
  private static Class<?>              PKG           = RepositoryImporter.class; 

  private Repository rep;
  private LogChannelInterface log;

  private SharedObjects sharedObjects;
  private RepositoryDirectoryInterface baseDirectory;
  private RepositoryDirectoryInterface root;
  
  private boolean overwrite;
  private boolean askOverwrite  = true;

  private String versionComment;

  private boolean continueOnError;

  private String transDirOverride = null;
  private String jobDirOverride = null;

  private ImportRules importRules;

  private List<String> limitDirs;
  
  private List<RepositoryObject> referencingObjects;

  private List<Exception> exceptions;

  public RepositoryImporter(Repository repository) {
    this(repository, new ImportRules(), new ArrayList<String>());
  }
  
  public RepositoryImporter(Repository repository, ImportRules importRules, List<String> limitDirs) {
      this.log = new LogChannel("Repository import"); 
      this.rep = repository;
      this.importRules = importRules;
      this.limitDirs = limitDirs;
      this.exceptions = new ArrayList<Exception>();
  }
  
  public synchronized void importAll(RepositoryImportFeedbackInterface feedback, String fileDirectory, String[] filenames, RepositoryDirectoryInterface baseDirectory, boolean overwrite, boolean continueOnError, String versionComment) {
    this.baseDirectory = baseDirectory;
    this.overwrite = overwrite;
    this.continueOnError = continueOnError;
    this.versionComment = versionComment;
    
    referencingObjects=new ArrayList<RepositoryObject>();

    feedback.setLabel(BaseMessages.getString(PKG, "RepositoryImporter.ImportXML.Label"));
    try {
      
      loadSharedObjects();

      RepositoryImportLocation.setRepositoryImportLocation(baseDirectory);

      for (int ii = 0; ii < filenames.length; ++ii) {

        final String filename = (!Const.isEmpty(fileDirectory)) ? fileDirectory + Const.FILE_SEPARATOR + filenames[ii] : filenames[ii];
        if (log.isBasic())
          log.logBasic("Import objects from XML file [" + filename + "]");  
        feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.WhichFile.Log", filename));

        // To where?
        feedback.setLabel(BaseMessages.getString(PKG, "RepositoryImporter.WhichDir.Label"));

        // Read it using SAX...
        //
        try {
          RepositoryExportSaxParser parser = new RepositoryExportSaxParser(filename, feedback);          
          parser.parse(this);
        } 
        catch (FileNotFoundException fnfe) {
           addException(fnfe);
           feedback.showError(BaseMessages.getString(PKG, "PurRepositoryImporter.ErrorGeneral.Title"), 
                 BaseMessages.getString(PKG, "PurRepositoryImporter.FileNotFound.Message", filename), fnfe);
           
         }
         catch (SAXParseException spe) {
           addException(spe);
           feedback.showError(BaseMessages.getString(PKG, "PurRepositoryImporter.ErrorGeneral.Title"), 
                  BaseMessages.getString(PKG, "PurRepositoryImporter.ParseError.Message", filename), spe);
         }
        catch(Exception e) { 
          feedback.showError(BaseMessages.getString(PKG, "RepositoryImporter.ErrorGeneral.Title"), 
              BaseMessages.getString(PKG, "RepositoryImporter.ErrorGeneral.Message"), e);
        }        
      }
      
      // Correct those jobs and transformations that contain references to other objects.
      //
      for (RepositoryObject ro : referencingObjects) {
        if (ro.getObjectType()==RepositoryObjectType.TRANSFORMATION) {
          TransMeta transMeta = rep.loadTransformation(ro.getObjectId(), null);
          try {
            transMeta.lookupRepositoryReferences(rep);
          } catch (KettleException e) {
           // log and continue; might fail from exports performed before PDI-5294
            feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.LookupRepoRefsError.Log", transMeta.getName()));
          }
          rep.save(transMeta, "import object reference specification", null);
        }
        if (ro.getObjectType()==RepositoryObjectType.JOB) {
          JobMeta jobMeta = rep.loadJob(ro.getObjectId(), null);
          try {
            jobMeta.lookupRepositoryReferences(rep);
          } catch (KettleException e) {
            // log and continue; might fail from exports performed before PDI-5294
            feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.LookupRepoRefsError.Log", jobMeta.getName()));
          }
          rep.save(jobMeta, "import object reference specification", null);
        }
      }
      
      feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.ImportFinished.Log"));
    } catch (KettleException e) {
      feedback.showError(BaseMessages.getString(PKG, "RepositoryImporter.ErrorGeneral.Title"), 
          BaseMessages.getString(PKG, "RepositoryImporter.ErrorGeneral.Message"), e);
    } finally {
      // set the repository import location to null when done!
      RepositoryImportLocation.setRepositoryImportLocation(null); 
    }
  }

  /**
   * Load the shared objects up front, replace them in the xforms/jobs loaded from XML.
   * We do this for performance reasons.
   * 
   * @throws KettleException
   */
  private void loadSharedObjects() throws KettleException {
    sharedObjects = new SharedObjects();
    
    for (ObjectId id : rep.getDatabaseIDs(false)) {
      DatabaseMeta databaseMeta = rep.loadDatabaseMeta(id, null);
      validateImportedElement(importRules, databaseMeta);
      sharedObjects.storeObject(databaseMeta);
    }
    List<SlaveServer> slaveServers = new ArrayList<SlaveServer>();
    for (ObjectId id : rep.getSlaveIDs(false)) {
      SlaveServer slaveServer = rep.loadSlaveServer(id, null);
      validateImportedElement(importRules, slaveServer);
      sharedObjects.storeObject(slaveServer);
      slaveServers.add(slaveServer);
    }
    for (ObjectId id : rep.getClusterIDs(false)) {
      ClusterSchema clusterSchema = rep.loadClusterSchema(id, slaveServers, null);
      validateImportedElement(importRules, clusterSchema);
      sharedObjects.storeObject(clusterSchema);
    }
    for (ObjectId id : rep.getPartitionSchemaIDs(false)) {
      PartitionSchema partitionSchema = rep.loadPartitionSchema(id, null);
      validateImportedElement(importRules, partitionSchema);
      sharedObjects.storeObject(partitionSchema);
    }
  }

  /**
   * Validates the repository element that is about to get imported against the list of import rules.
   * @param the import rules to validate against.
   * @param subject
   * @throws KettleException
   */
  public static void validateImportedElement(ImportRules importRules, Object subject) throws KettleException {
    List<ImportValidationFeedback> feedback = importRules.verifyRules(subject);
    List<ImportValidationFeedback> errors = ImportValidationFeedback.getErrors(feedback);
    if (!errors.isEmpty()) {
      StringBuffer message = new StringBuffer(BaseMessages.getString(PKG, "RepositoryImporter.ValidationFailed.Message", subject.toString()));
      message.append(Const.CR);
      for (ImportValidationFeedback error: errors) {
        message.append(" - ");
        message.append(error.toString());
        message.append(Const.CR);
      }
      throw new KettleException(message.toString());
    }
  }

  public void addLog(String line) {
    log.logBasic(line);
  }

  public void setLabel(String labelText) {
    log.logBasic(labelText);
  }

  public boolean transOverwritePrompt(TransMeta transMeta) {
    return overwrite;
  }
  
  public boolean jobOverwritePrompt(JobMeta jobMeta) {
    return overwrite;
  }

  public void updateDisplay() {
  }
  
  public void showError(String title, String message, Exception e) {
    log.logError(message, e);
  }

  private void replaceSharedObjects(TransMeta transMeta) {
    for (SharedObjectInterface sharedObject : sharedObjects.getObjectsMap().values()) {
      // Database...
      //
      if (sharedObject instanceof DatabaseMeta) {
        DatabaseMeta databaseMeta = (DatabaseMeta) sharedObject;
        int index = transMeta.indexOfDatabase(databaseMeta);
        if (index<0) {
          transMeta.addDatabase(databaseMeta);
        } else {
          DatabaseMeta imported = transMeta.getDatabase(index);
          if (overwrite) {
            // Preserve the object id so we can update without having to look up the id
            imported.setObjectId(databaseMeta.getObjectId());
            imported.setChanged();
          } else {
            imported.replaceMeta(databaseMeta);
            // We didn't actually change anything
            imported.setChanged(false);
          }
        }
      }

      // Partition Schema...
      //
      if (sharedObject instanceof SlaveServer) {
        SlaveServer slaveServer = (SlaveServer) sharedObject;
        
        int index = transMeta.getSlaveServers().indexOf(slaveServer);
        if (index<0) {
          transMeta.getSlaveServers().add(slaveServer);
        } else {
          SlaveServer imported = transMeta.getSlaveServers().get(index);
          if (overwrite) {
            // Preserve the object id so we can update without having to look up the id
            imported.setObjectId(slaveServer.getObjectId());
            imported.setChanged();
          } else {
            imported.replaceMeta(slaveServer);
            // We didn't actually change anything
            imported.setChanged(false);
          }
        }
      }

      // Cluster Schema...
      //
      if (sharedObject instanceof ClusterSchema) {
        ClusterSchema clusterSchema = (ClusterSchema) sharedObject;
        
        int index = transMeta.getClusterSchemas().indexOf(clusterSchema);
        if (index<0) {
          transMeta.getClusterSchemas().add(clusterSchema);
        } else {
          ClusterSchema imported = transMeta.getClusterSchemas().get(index);
          if (overwrite) {
            // Preserve the object id so we can update without having to look up the id
            imported.setObjectId(clusterSchema.getObjectId());
            imported.setChanged();
          } else {
            imported.replaceMeta(clusterSchema);
            // We didn't actually change anything
            imported.setChanged(false);
          }
        }
      }

      // Partition Schema...
      //
      if (sharedObject instanceof PartitionSchema) {
        PartitionSchema partitionSchema = (PartitionSchema) sharedObject;
        
        int index = transMeta.getPartitionSchemas().indexOf(partitionSchema);
        if (index<0) {
          transMeta.getPartitionSchemas().add(partitionSchema);
        } else {
          PartitionSchema imported = transMeta.getPartitionSchemas().get(index);
          if (overwrite) {
            // Preserve the object id so we can update without having to look up the id
            imported.setObjectId(partitionSchema.getObjectId());
            imported.setChanged();
          } else {
            imported.replaceMeta(partitionSchema);
            // We didn't actually change anything
            imported.setChanged(false);
          }
        }
      }

    }
  }
  
  private void replaceSharedObjects(JobMeta transMeta) {
    for (SharedObjectInterface sharedObject : sharedObjects.getObjectsMap().values()) {
      // Database...
      //
      if (sharedObject instanceof DatabaseMeta) {
        DatabaseMeta databaseMeta = (DatabaseMeta) sharedObject;
        int index = transMeta.indexOfDatabase(databaseMeta);
        if (index<0) {
          transMeta.addDatabase(databaseMeta);
        } else {
          DatabaseMeta imported = transMeta.getDatabase(index);
          if (overwrite) {
            // Preserve the object id so we can update without having to look up the id
            imported.setObjectId(databaseMeta.getObjectId());
            imported.setChanged();
          } else {
            imported.replaceMeta(databaseMeta);
            // We didn't actually change anything
            imported.setChanged(false);
          }
        }
      }

      // Partition Schema...
      //
      if (sharedObject instanceof SlaveServer) {
        SlaveServer slaveServer = (SlaveServer) sharedObject;
        
        int index = transMeta.getSlaveServers().indexOf(slaveServer);
        if (index<0) {
          transMeta.getSlaveServers().add(slaveServer);
        } else {
          SlaveServer imported = transMeta.getSlaveServers().get(index);
          if (overwrite) {
            // Preserve the object id so we can update without having to look up the id
            imported.setObjectId(slaveServer.getObjectId());
            imported.setChanged();
          } else {
            imported.replaceMeta(slaveServer);
            // We didn't actually change anything
            imported.setChanged(false);
          }
        }
      }
    }
  }


  private void patchMappingSteps(TransMeta transMeta) {
    for (StepMeta stepMeta : transMeta.getSteps()) {
      if (stepMeta.isMapping()) {
        MappingMeta mappingMeta = (MappingMeta) stepMeta.getStepMetaInterface();
        if (mappingMeta.getSpecificationMethod() == ObjectLocationSpecificationMethod.REPOSITORY_BY_NAME) {
          if (transDirOverride != null) {
            mappingMeta.setDirectoryPath(transDirOverride);
            continue;
          }
          String newPath = baseDirectory.getPath();
          String extraPath = mappingMeta.getDirectoryPath();
          if (newPath.endsWith("/") && extraPath.startsWith("/")) {
            newPath=newPath.substring(0,newPath.length()-1);
          } else if (!newPath.endsWith("/") && !extraPath.startsWith("/")) {
            newPath+="/";
          } else if (extraPath.equals("/")) {
            extraPath="";
          }
          mappingMeta.setDirectoryPath(newPath+extraPath);
        }
      }
    }
  }

  private void patchJobEntries(JobMeta jobMeta) {
    for (JobEntryCopy copy : jobMeta.getJobCopies()) {
      if (copy.isTransformation()) {
        JobEntryTrans entry = (JobEntryTrans) copy.getEntry();
        if (entry.getSpecificationMethod() == ObjectLocationSpecificationMethod.REPOSITORY_BY_NAME) {
          if (transDirOverride != null) {
            entry.setDirectory(transDirOverride);
            continue;
          }
          String newPath = baseDirectory.getPath();
          String extraPath = Const.NVL(entry.getDirectory(), "/");
          if (newPath.endsWith("/") && extraPath.startsWith("/")) {
            newPath=newPath.substring(0,newPath.length()-1);
          } else if (!newPath.endsWith("/") && !extraPath.startsWith("/")) {
            newPath+="/";
          } else if (extraPath.equals("/")) {
            extraPath="";
          }
          entry.setDirectory(newPath+extraPath);
        }
      }
      if (copy.isJob()) {
        JobEntryJob entry = (JobEntryJob) copy.getEntry();
        if (entry.getSpecificationMethod() == ObjectLocationSpecificationMethod.REPOSITORY_BY_NAME) {
          if (jobDirOverride != null) {
            entry.setDirectory(jobDirOverride);
            continue;
          }
          String newPath = baseDirectory.getPath();
          String extraPath = Const.NVL(entry.getDirectory(), "/");
          if (newPath.endsWith("/") && extraPath.startsWith("/")) {
            newPath=newPath.substring(0,newPath.length()-1);
          } else if (!newPath.endsWith("/") && !extraPath.startsWith("/")) {
            newPath+="/";
          } else if (extraPath.equals("/")) {
            extraPath="";
          }
          entry.setDirectory(newPath+extraPath);
        }
      }
    }
  }

  /**
   * 
   * @param transnode
   *          The XML DOM node to read the transformation from
   * @return false if the import should be canceled.
   * @throws KettleException
   *           in case there is an unexpected error
   */
  private boolean importTransformation(Node transnode, RepositoryImportFeedbackInterface feedback) throws KettleException {
    //
    // Load transformation from XML into a directory, possibly created!
    //
    TransMeta transMeta = new TransMeta(transnode, null); // ignore shared objects
    replaceSharedObjects(transMeta);
    feedback.setLabel(BaseMessages.getString(PKG, "RepositoryImporter.ImportTrans.Label", Integer.toString(transformationNumber), transMeta.getName()));

    validateImportedElement(importRules, transMeta);

    // What's the directory path?
    String directoryPath = XMLHandler.getTagValue(transnode, "info", "directory");
    if (transDirOverride != null) {
      directoryPath = transDirOverride;
    }
    
    if (directoryPath.startsWith("/")) {
      // remove the leading root, we don't need it.
      directoryPath = directoryPath.substring(1);
    }

    // If we have a set of source directories to limit ourselves to, consider this.
    //
    if (limitDirs.size()>0 && Const.indexOfString(directoryPath, limitDirs)<0) {
      // Not in the limiting set of source directories, skip the import of this transformation...
      //
      feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.SkippedTransformationNotPartOfLimitingDirectories.Log", transMeta.getName()));
      return true;
    }
    
    RepositoryDirectoryInterface targetDirectory = getTargetDirectory(directoryPath, transDirOverride, feedback);

    // OK, we loaded the transformation from XML and all went well...
    // See if the transformation already existed!
    ObjectId existingId = rep.getTransformationID(transMeta.getName(), targetDirectory);
    if (existingId!=null && askOverwrite) {
      overwrite = feedback.transOverwritePrompt(transMeta);
      askOverwrite = feedback.isAskingOverwriteConfirmation();
    } else {
      updateDisplay();
    }

    if (existingId==null || overwrite) {
      transMeta.setObjectId(existingId);
      transMeta.setRepositoryDirectory(targetDirectory);
      patchMappingSteps(transMeta);

      try {
        // Keep info on who & when this transformation was created...
        if (transMeta.getCreatedUser() == null || transMeta.getCreatedUser().equals("-")) {
          transMeta.setCreatedDate(new Date());
          if (rep.getUserInfo() != null) {
            transMeta.setCreatedUser(rep.getUserInfo().getLogin());
          } else {
            transMeta.setCreatedUser(null);
          }
        }
        rep.save(transMeta, versionComment, this, overwrite);
        feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.TransSaved.Log", Integer.toString(transformationNumber), transMeta.getName()));

        if (transMeta.hasRepositoryReferences()) {
          referencingObjects.add(new RepositoryObject(transMeta.getObjectId(), transMeta.getName(), transMeta.getRepositoryDirectory(), null, null, RepositoryObjectType.TRANSFORMATION, null, false));
        }

      } catch (Exception e) {
        feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.ErrorSavingTrans.Log", Integer.toString(transformationNumber), transMeta.getName(), Const.getStackTracker(e)));

        if (!feedback.askContinueOnErrorQuestion(
            BaseMessages.getString(PKG, "RepositoryImporter.DoYouWantToContinue.Title"), 
            BaseMessages.getString(PKG, "RepositoryImporter.DoYouWantToContinue.Message"))) {
          return false;
        }
      }
    } else {
      feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.SkippedExistingTransformation.Log", transMeta.getName()));
    }
    return true;
  }

  private boolean importJob(Node jobnode, RepositoryImportFeedbackInterface feedback) throws KettleException {
    // Load the job from the XML node.
    //                
    JobMeta jobMeta = new JobMeta(jobnode, rep, false, SpoonFactory.getInstance());
    replaceSharedObjects(jobMeta);
    feedback.setLabel(BaseMessages.getString(PKG, "RepositoryImporter.ImportJob.Label", Integer.toString(jobNumber), jobMeta.getName()));
    validateImportedElement(importRules, jobMeta);
    
    // What's the directory path?
    String directoryPath = Const.NVL(XMLHandler.getTagValue(jobnode, "directory"), Const.FILE_SEPARATOR);

    if (jobDirOverride != null) {
      directoryPath = jobDirOverride;
    }
    
    if (directoryPath.startsWith("/")) {
      // remove the leading root, we don't need it.
      directoryPath = directoryPath.substring(1);
    }
    
    // If we have a set of source directories to limit ourselves to, consider this.
    //
    if (limitDirs.size()>0 && Const.indexOfString(directoryPath, limitDirs)<0) {
      // Not in the limiting set of source directories, skip the import of this transformation...
      //
      feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.SkippedJobNotPartOfLimitingDirectories.Log", jobMeta.getName()));
      return true;
    }

    RepositoryDirectoryInterface targetDirectory = getTargetDirectory(directoryPath, jobDirOverride, feedback);

    // OK, we loaded the job from XML and all went well...
    // See if the job already exists!
    ObjectId existintId = rep.getJobId(jobMeta.getName(), targetDirectory);
    if (existintId != null && askOverwrite) {
      overwrite = feedback.jobOverwritePrompt(jobMeta);
      askOverwrite = feedback.isAskingOverwriteConfirmation();
    } else {
      updateDisplay();
    }

    if (existintId == null || overwrite) {
      jobMeta.setRepositoryDirectory(targetDirectory);
      jobMeta.setObjectId(existintId);
      patchJobEntries(jobMeta);
      try {
        // Keep info on who & when this transformation was created...
        if (jobMeta.getCreatedUser() == null || jobMeta.getCreatedUser().equals("-")) {
          jobMeta.setCreatedDate(new Date());
          if (rep.getUserInfo() != null) {
            jobMeta.setCreatedUser(rep.getUserInfo().getLogin());
          } else {
            jobMeta.setCreatedUser(null);
          }
        }

        rep.save(jobMeta, versionComment, null, overwrite);
        
        if (jobMeta.hasRepositoryReferences()) {
          referencingObjects.add(new RepositoryObject(jobMeta.getObjectId(), jobMeta.getName(), jobMeta.getRepositoryDirectory(), null, null, RepositoryObjectType.JOB, null, false));
        }

        feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.JobSaved.Log", Integer.toString(jobNumber), jobMeta.getName()));
      } catch(Exception e) {
        feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.ErrorSavingJob.Log", Integer.toString(jobNumber), jobMeta.getName(), Const.getStackTracker(e)));

        if (!feedback.askContinueOnErrorQuestion(
            BaseMessages.getString(PKG, "RepositoryImporter.DoYouWantToContinue.Title"), 
            BaseMessages.getString(PKG, "RepositoryImporter.DoYouWantToContinue.Message"))) {
          return false;
        }
      }
    } else {
      feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.SkippedExistingJob.Log", jobMeta.getName()));
    }
    return true;
  }


  private int transformationNumber = 1;
  public boolean transformationElementRead(String xml, RepositoryImportFeedbackInterface feedback) {
    try {
      Document doc = XMLHandler.loadXMLString(xml);
      Node transformationNode = XMLHandler.getSubNode(doc, RepositoryExportSaxParser.STRING_TRANSFORMATION);
      if (!importTransformation(transformationNode, feedback)) {
        return false;
      }
      transformationNumber++;
    } catch (Exception e) {
      // Some unexpected error occurred during transformation import
      // This is usually a problem with a missing plugin or something
      // like that...
      //
      feedback.showError(BaseMessages.getString(PKG, "RepositoryImporter.UnexpectedErrorDuringTransformationImport.Title"), 
          BaseMessages.getString(PKG, "RepositoryImporter.UnexpectedErrorDuringTransformationImport.Message"), e);

      if (!feedback.askContinueOnErrorQuestion(BaseMessages.getString(PKG, "RepositoryImporter.DoYouWantToContinue.Title"), 
          BaseMessages.getString(PKG, "RepositoryImporter.DoYouWantToContinue.Message"))) {
        return false;
      }
    }
    return true;
  }

  private int jobNumber = 1;
  public boolean jobElementRead(String xml, RepositoryImportFeedbackInterface feedback) {
    try {
      Document doc = XMLHandler.loadXMLString(xml);
      Node jobNode = XMLHandler.getSubNode(doc, RepositoryExportSaxParser.STRING_JOB);
      if (!importJob(jobNode, feedback)) {
        return false;
      }
      jobNumber++;
    } catch (Exception e) {
      // Some unexpected error occurred during job import
      // This is usually a problem with a missing plugin or something
      // like that...
      //
      showError(BaseMessages.getString(PKG, "RepositoryImporter.UnexpectedErrorDuringJobImport.Title"), 
          BaseMessages.getString(PKG, "RepositoryImporter.UnexpectedErrorDuringJobImport.Message"), e);
      
      if (!feedback.askContinueOnErrorQuestion(BaseMessages.getString(PKG, "RepositoryImporter.DoYouWantToContinue.Title"), 
          BaseMessages.getString(PKG, "RepositoryImporter.DoYouWantToContinue.Message"))) {
        return false;
      }
    }
    return true;
  }

  private RepositoryDirectoryInterface getTargetDirectory(String directoryPath, String dirOverride, RepositoryImportFeedbackInterface feedback) throws KettleException {
    RepositoryDirectoryInterface targetDirectory = null;
    if (dirOverride != null) {
      targetDirectory = rep.findDirectory(directoryPath);
      if (targetDirectory == null) {
        feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.CreateDir.Log", directoryPath, getRepositoryRoot().toString()));
        targetDirectory = rep.createRepositoryDirectory(getRepositoryRoot(), directoryPath);
      }
    } else {
      targetDirectory = baseDirectory.findDirectory(directoryPath);
      if (targetDirectory == null) {
        feedback.addLog(BaseMessages.getString(PKG, "RepositoryImporter.CreateDir.Log", directoryPath, baseDirectory.toString()));
        targetDirectory = rep.createRepositoryDirectory(baseDirectory, directoryPath);
      }
    }
    return targetDirectory;
  }
  
  private RepositoryDirectoryInterface getRepositoryRoot() throws KettleException {
    if (root == null) {
      root = rep.loadRepositoryDirectoryTree();
    }
    return root;
  }
  
  public void fatalXmlErrorEncountered(SAXParseException e) {
    showError(
        BaseMessages.getString(PKG, "RepositoryImporter.ErrorInvalidXML.Message"),
        BaseMessages.getString(PKG, "RepositoryImporter.ErrorInvalidXML.Title"),
        e
       );
  }

  public boolean askContinueOnErrorQuestion(String title, String message) {
    return continueOnError;
  }
  
  public void beginTask(String message, int nrWorks) {
    addLog(message);
  }

  public void done() {
  }

  public boolean isCanceled() {
    return false;
  }

  public void setTaskName(String taskName) {
    addLog(taskName);
  }

  public void subTask(String message) {
    addLog(message);
  }

  public void worked(int nrWorks) {
  }
  
  public String getTransDirOverride() {
    return transDirOverride;
  }

  public void setTransDirOverride(String transDirOverride) {
    this.transDirOverride = transDirOverride;
  }

  public String getJobDirOverride() {
    return jobDirOverride;
  }

  public void setJobDirOverride(String jobDirOverride) {
    this.jobDirOverride = jobDirOverride;
  }
  
  @Override
  public void setImportRules(ImportRules importRules) {
    this.importRules = importRules;
  }
  
  public ImportRules getImportRules() {
    return importRules;
  }
  
  @Override
  public boolean isAskingOverwriteConfirmation() {
    return askOverwrite;
  }
  
  private void addException(Exception exception) {
     if (this.exceptions == null) {
        this.exceptions = new ArrayList<Exception>();
     }
     exceptions.add(exception);
  }

  @Override
  public List<Exception> getExceptions() {
     return exceptions;
  }
}
