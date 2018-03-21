package org.pentaho.di.core.extension;

public enum KettleExtensionPoint {

  // Some transformation points
  //
  TransformationPrepareExecution("TransformationPrepareExecution", "A transformation begins to prepare execution"),
  TransformationStartThreads("TransformationStartThreads", "A transformation begins to start"),
  TransformationStarted("TransformationStart", "A transformation has started"),
  TransformationFinish("TransformationFinish", "A transformation finishes"),
  TransformationMetaLoaded("TransformationMetaLoaded", "Transformation metadata was loaded"),
  TransPainterArrow("TransPainterArrow", "Draw additional information on top of a transformation hop (arrow)"),
  TransPainterStep("TransPainterStep", "Draw additional information on top of a transformation step icon"),
  SpoonTransMetaExecutionStart("SpoonTransMetaExecutionStart", "Spoon initiates the execution of a trans (TransMeta)"),
  SpoonTransExecutionConfiguration("SpoonTransExecutionConfiguration", "Right before Spoon configuration of transformation to be executed takes place"),

  JobStart("JobStart", "A job starts"),
  JobFinish("JobFinish", "A job finishes"),
  JobBeforeJobEntryExecution("JobBeforeJobEntryExecution", "Before a job entry executes"),
  JobAfterJobEntryExecution("JobAfterJobEntryExecution", "After a job entry executes"),
  JobBeginProcessing("JobBeginProcessing", "Start of a job at the end of the log table handling"),
  JobPainterArrow("JobPainterArrow", "Draw additional information on top of a job hop (arrow)"),
  JobPainterJobEntry("TransPainterJobEntry", "Draw additional information on top of a job entry copy icon"),
  JobGraphJobEntrySetMenu("JobGraphJobEntrySetMenu", "Manipulate the menu on right click on a job entry"),
  JobDialogShowRetrieveLogTableFields("JobDialogShowRetrieveLogTableFields", "Show or retrieve the contents of the fields of a log channel on the log channel composite"),
          
  JobMetaLoaded("JobMetaLoaded", "Job metadata was loaded"),
  SpoonJobMetaExecutionStart("SpoonJobMetaExecutionStart", "Spoon initiates the execution of a job (JobMeta)"),
  SpoonJobExecutionConfiguration("SpoonJobExecutionConfiguration", "Right before Spoon configuration of job to be executed takes place"),
  
  DatabaseConnected("DatabaseConnected", "A connection to a database was made"),
  
  DatabaseDisconnected("DatabaseDisconnected", "A connection to a database was terminated"),
  
  StepBeforeInitialize("StepBeforeInitialize", "Right before a step is about to be initialized"),
  StepAfterInitialize("StepAfterInitialize", "After a step is initialized"),
  
  StepBeforeStart("StepBeforeStart", "Right before a step is about to be started"),
  StepFinished("StepFinished", "After a step has finished"),
  ;

  public String id;

  public String description;

  private KettleExtensionPoint(String id, String description) {
    this.id = id;
    this.description = description;
  }
}
