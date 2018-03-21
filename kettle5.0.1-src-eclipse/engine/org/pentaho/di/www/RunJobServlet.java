package org.pentaho.di.www;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.logging.LogLevel;
import org.pentaho.di.core.logging.LoggingObjectType;
import org.pentaho.di.core.logging.SimpleLoggingObject;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobConfiguration;
import org.pentaho.di.job.JobExecutionConfiguration;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.repository.RepositoryDirectoryInterface;

public class RunJobServlet extends BaseHttpServlet implements CartePluginInterface {

  private static final long  serialVersionUID = 1192413943669836775L;

  private static Class<?>    PKG              = RunJobServlet.class; // i18n

  public static final String CONTEXT_PATH     = "/kettle/runJob";

  public RunJobServlet() {
  }

  public RunJobServlet(JobMap jobMap) {
    super(jobMap);
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    if (isJettyMode() && !request.getContextPath().startsWith(CONTEXT_PATH)) {
      return;
    }

    if (log.isDebug())
      logDebug(BaseMessages.getString(PKG, "RunJobServlet.Log.RunJobRequested"));

    // Options taken from PAN
    //
    String[] knownOptions = new String[] { "job", "level", };

    String transOption = request.getParameter("job");
    String levelOption = request.getParameter("level");

    response.setStatus(HttpServletResponse.SC_OK);

    PrintWriter out = response.getWriter();

    try {

      final Repository repository = transformationMap.getSlaveServerConfig().getRepository();
      final JobMeta jobMeta = loadJob(repository, transOption);

      // Set the servlet parameters as variables in the transformation
      //
      String[] parameters = jobMeta.listParameters();
      Enumeration<?> parameterNames = request.getParameterNames();
      while (parameterNames.hasMoreElements()) {
        String parameter = (String) parameterNames.nextElement();
        String[] values = request.getParameterValues(parameter);

        // Ignore the known options. set the rest as variables
        //
        if (Const.indexOfString(parameter, knownOptions) < 0) {
          // If it's a trans parameter, set it, otherwise simply set the
          // variable
          //
          if (Const.indexOfString(parameter, parameters) < 0) {
            jobMeta.setVariable(parameter, values[0]);
          } else {
            jobMeta.setParameterValue(parameter, values[0]);
          }
        }
      }

      JobExecutionConfiguration jobExecutionConfiguration = new JobExecutionConfiguration();
      LogLevel logLevel = LogLevel.getLogLevelForCode(levelOption);
      jobExecutionConfiguration.setLogLevel(logLevel);
      JobConfiguration jobConfiguration = new JobConfiguration(jobMeta, jobExecutionConfiguration);

      String carteObjectId = UUID.randomUUID().toString();
      SimpleLoggingObject servletLoggingObject = new SimpleLoggingObject(CONTEXT_PATH, LoggingObjectType.CARTE, null);
      servletLoggingObject.setContainerObjectId(carteObjectId);
      servletLoggingObject.setLogLevel(logLevel);

      // Create the transformation and store in the list...
      //
      final Job job = new Job(repository, jobMeta, servletLoggingObject);

      // Setting variables
      //
      job.initializeVariablesFrom(null);
      job.getJobMeta().setInternalKettleVariables(job);
      job.injectVariables(jobConfiguration.getJobExecutionConfiguration().getVariables());

      // Also copy the parameters over...
      //
      job.copyParametersFrom(jobMeta);
      job.clearParameters();
      /*
       * String[] parameterNames = job.listParameters(); for (int idx = 0; idx <
       * parameterNames.length; idx++) { // Grab the parameter value set in the
       * job entry // String thisValue =
       * jobExecutionConfiguration.getParams().get(parameterNames[idx]); if
       * (!Const.isEmpty(thisValue)) { // Set the value as specified by the user
       * in the job entry // jobMeta.setParameterValue(parameterNames[idx],
       * thisValue); } }
       */
      jobMeta.activateParameters();

      job.setSocketRepository(getSocketRepository());

      getJobMap().addJob(job.getJobname(), carteObjectId, job, jobConfiguration);

      // DO NOT disconnect from the shared repository connection when the job finishes.
      //
      String message = "Job '" + job.getJobname() + "' was added to the list with id " + carteObjectId;
      logBasic(message);

      //
      try {
        // Execute the transformation...
        //
        job.start();
        
        WebResult webResult = new WebResult(WebResult.STRING_OK, "Job started", carteObjectId);
        out.println(webResult.getXML());
        out.flush();

      } catch (Exception executionException) {
        String logging = KettleLogStore.getAppender().getBuffer(job.getLogChannelId(), false).toString();
        throw new KettleException("Error executing Job: " + logging, executionException);
      }
    } catch (Exception ex) {

      out.println(new WebResult(WebResult.STRING_ERROR, BaseMessages.getString(PKG, "RunJobServlet.Error.UnexpectedError", Const.CR + Const.getStackTracker(ex))));
    }
  }

  private JobMeta loadJob(Repository repository, String job) throws KettleException {

    if (repository == null) {
      throw new KettleException("Repository required.");
    } else {

      synchronized(repository) {
        // With a repository we need to load it from /foo/bar/Transformation
        // We need to extract the folder name from the path in front of the
        // name...
        //
        String directoryPath;
        String name;
        int lastSlash = job.lastIndexOf(RepositoryDirectory.DIRECTORY_SEPARATOR);
        if (lastSlash < 0) {
          directoryPath = "/";
          name = job;
        } else {
          directoryPath = job.substring(0, lastSlash);
          name = job.substring(lastSlash + 1);
        }
        RepositoryDirectoryInterface directory = repository.loadRepositoryDirectoryTree().findDirectory(directoryPath);
  
        ObjectId jobID = repository.getJobId(name, directory);
  
        JobMeta transJob = repository.loadJob(jobID, null);
        return transJob;
      }
    }
  }

  public String toString() {
    return "Run Job";
  }

  public String getService() {
    return CONTEXT_PATH + " (" + toString() + ")";
  }
  
  @Override
  public String getContextPath() {
    return CONTEXT_PATH;
  }

}