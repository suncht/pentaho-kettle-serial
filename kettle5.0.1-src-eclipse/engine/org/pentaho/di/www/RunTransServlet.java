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
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransConfiguration;
import org.pentaho.di.trans.TransExecutionConfiguration;
import org.pentaho.di.trans.TransMeta;

public class RunTransServlet extends BaseHttpServlet implements CartePluginInterface {

  private static final long  serialVersionUID = 1192413943669836776L;

  private static Class<?>    PKG              = RunTransServlet.class; // i18n

  public static final String CONTEXT_PATH     = "/kettle/runTrans";

  public RunTransServlet() {
  }

  public RunTransServlet(TransformationMap transformationMap) {
    super(transformationMap);
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    if (isJettyMode() && !request.getContextPath().startsWith(CONTEXT_PATH)) {
      return;
    }

    if (log.isDebug())
      logDebug(BaseMessages.getString(PKG, "RunTransServlet.Log.RunTransRequested"));

    // Options taken from PAN
    //
    String[] knownOptions = new String[] { "trans", "level", };

    String transOption = request.getParameter("trans");
    String levelOption = request.getParameter("level");

    response.setStatus(HttpServletResponse.SC_OK);

    PrintWriter out = response.getWriter();

    try {

      final Repository repository = transformationMap.getSlaveServerConfig().getRepository();
      final TransMeta transMeta = loadTrans(repository, transOption);

      // Set the servlet parameters as variables in the transformation
      //
      String[] parameters = transMeta.listParameters();
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
            transMeta.setVariable(parameter, values[0]);
          } else {
            transMeta.setParameterValue(parameter, values[0]);
          }
        }
      }

      TransExecutionConfiguration transExecutionConfiguration = new TransExecutionConfiguration();
      LogLevel logLevel = LogLevel.getLogLevelForCode(levelOption);
      transExecutionConfiguration.setLogLevel(logLevel);
      TransConfiguration transConfiguration = new TransConfiguration(transMeta, transExecutionConfiguration);

      String carteObjectId = UUID.randomUUID().toString();
      SimpleLoggingObject servletLoggingObject = new SimpleLoggingObject(CONTEXT_PATH, LoggingObjectType.CARTE, null);
      servletLoggingObject.setContainerObjectId(carteObjectId);
      servletLoggingObject.setLogLevel(logLevel);

      // Create the transformation and store in the list...
      //
      final Trans trans = new Trans(transMeta, servletLoggingObject);
      
      // Pass information
      //
      trans.setRepository(repository);
      trans.setServletPrintWriter(out);
      trans.setServletReponse(response);
      trans.setServletRequest(request);
      
      // Setting variables
      //
      trans.initializeVariablesFrom(null);
      trans.getTransMeta().setInternalKettleVariables(trans);
      trans.injectVariables(transConfiguration.getTransExecutionConfiguration().getVariables());

      // Also copy the parameters over...
      //
      trans.copyParametersFrom(transMeta);
      trans.clearParameters();
      
      /*
       * String[] parameterNames = job.listParameters(); for (int idx = 0; idx <
       * parameterNames.length; idx++) { // Grab the parameter value set in the
       * job entry // String thisValue =
       * jobExecutionConfiguration.getParams().get(parameterNames[idx]); if
       * (!Const.isEmpty(thisValue)) { // Set the value as specified by the user
       * in the job entry // jobMeta.setParameterValue(parameterNames[idx],
       * thisValue); } }
       */
      transMeta.activateParameters();

      trans.setSocketRepository(getSocketRepository());

      getTransformationMap().addTransformation(trans.getName(), carteObjectId, trans, transConfiguration);

      // DO NOT disconnect from the shared repository connection when the job finishes.
      //
      String message = "Transformation '" + trans.getName() + "' was added to the list with id " + carteObjectId;
      logBasic(message);

      //
      try {
        // Execute the transformation...
        //
        trans.execute(null);
        
        WebResult webResult = new WebResult(WebResult.STRING_OK, "Transformation started", carteObjectId);
        out.println(webResult.getXML());
        out.flush();

      } catch (Exception executionException) {
        String logging = KettleLogStore.getAppender().getBuffer(trans.getLogChannelId(), false).toString();
        throw new KettleException("Error executing Transformation: " + logging, executionException);
      }
    } catch (Exception ex) {
      out.println(new WebResult(WebResult.STRING_ERROR, BaseMessages.getString(PKG, "RunTransServlet.Error.UnexpectedError", Const.CR + Const.getStackTracker(ex))));
    }
  }

  private TransMeta loadTrans(Repository repository, String transformationName) throws KettleException {

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
        int lastSlash = transformationName.lastIndexOf(RepositoryDirectory.DIRECTORY_SEPARATOR);
        if (lastSlash < 0) {
          directoryPath = "/";
          name = transformationName;
        } else {
          directoryPath = transformationName.substring(0, lastSlash);
          name = transformationName.substring(lastSlash + 1);
        }
        RepositoryDirectoryInterface directory = repository.loadRepositoryDirectoryTree().findDirectory(directoryPath);
  
        ObjectId transformationId = repository.getTransformationID(name, directory);
  
        TransMeta transMeta = repository.loadTransformation(transformationId, null);
        return transMeta;
      }
    }
  }

  public String toString() {
    return "Run Transformation";
  }

  public String getService() {
    return CONTEXT_PATH + " (" + toString() + ")";
  }
  
  @Override
  public String getContextPath() {
    return CONTEXT_PATH;
  }

}