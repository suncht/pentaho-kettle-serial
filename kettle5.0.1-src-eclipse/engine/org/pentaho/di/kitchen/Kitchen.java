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

package org.pentaho.di.kitchen;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.logging.FileLoggingEventListener;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.logging.LogLevel;
import org.pentaho.di.core.parameters.NamedParams;
import org.pentaho.di.core.parameters.NamedParamsDefault;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.RepositoryPluginType;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.core.util.ExecutorUtil;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.metastore.MetaStoreConst;
import org.pentaho.di.pan.CommandLineOption;
import org.pentaho.di.repository.RepositoriesMeta;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.RepositoryMeta;
import org.pentaho.di.resource.ResourceUtil;
import org.pentaho.di.resource.TopLevelResource;
import org.pentaho.di.version.BuildVersion;
import org.pentaho.metastore.stores.delegate.DelegatingMetaStore;

import com.google.gdata.util.common.base.Pair;

public class Kitchen {
  private static Class<?> PKG = Kitchen.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

  public static final String STRING_KITCHEN = "Kitchen";

  private static FileLoggingEventListener fileAppender;

  public static void main(String[] a) throws Exception {
    final ExecutorService executor = ExecutorUtil.getExecutor();
    final RepositoryPluginType repositoryPluginType = RepositoryPluginType.getInstance();
    
    final Future<Pair<KettlePluginException, Future<KettleException>>> repositoryRegisterFuture = 
        executor.submit(new Callable<Pair<KettlePluginException, Future<KettleException>>>() {

      @Override
      public Pair<KettlePluginException, Future<KettleException>> call() throws Exception {
        PluginRegistry.addPluginType(repositoryPluginType);
        try {
          KettleClientEnvironment.init();
        } catch (KettlePluginException e) {
          return Pair.of(e, null);
        }
        
        Future<KettleException> kettleEnvironmentInitFuture = executor.submit(new Callable<KettleException>() {

          @Override
          public KettleException call() throws Exception {
            try {
              KettleEnvironment.init();
              KettleClientEnvironment.getInstance().setClient(KettleClientEnvironment.ClientType.KITCHEN);
            } catch (KettleException e) {
              return e;
            }
            return null;
          }
        });
        return Pair.of(null, kettleEnvironmentInitFuture);
      }
    });

    List<String> args = new ArrayList<String>();
    for (int i = 0; i < a.length; i++) {
      if (a[i].length() > 0)
        args.add(a[i]);
    }

    DelegatingMetaStore metaStore = new DelegatingMetaStore();
    metaStore.addMetaStore(MetaStoreConst.openLocalPentahoMetaStore());
    metaStore.setActiveMetaStoreName(metaStore.getName());
    
    RepositoryMeta repositoryMeta = null;
    Job job = null;

    StringBuffer optionRepname, optionUsername, optionPassword, optionJobname, 
      optionDirname, optionFilename, optionLoglevel,optionLogfile, optionLogfileOld, 
      optionListdir, optionListjobs, optionListrep, optionNorep, optionVersion, 
      optionListParam, optionExport
      ;
    NamedParams optionParams = new NamedParamsDefault();
    NamedParams customOptions = new NamedParamsDefault();

    CommandLineOption maxLogLinesOption = new CommandLineOption(
        "maxloglines", BaseMessages.getString(PKG, "Kitchen.CmdLine.MaxLogLines"), new StringBuffer());  
    CommandLineOption maxLogTimeoutOption = new CommandLineOption(
        "maxlogtimeout", BaseMessages.getString(PKG, "Kitchen.CmdLine.MaxLogTimeout"), new StringBuffer());  

    CommandLineOption options[] = new CommandLineOption[] {
        new CommandLineOption("rep", BaseMessages.getString(PKG, "Kitchen.CmdLine.RepName"),
            optionRepname = new StringBuffer()),
        new CommandLineOption("user", BaseMessages.getString(PKG, "Kitchen.CmdLine.RepUsername"),
            optionUsername = new StringBuffer()),
        new CommandLineOption("pass", BaseMessages.getString(PKG, "Kitchen.CmdLine.RepPassword"),
            optionPassword = new StringBuffer()),
        new CommandLineOption("job", BaseMessages.getString(PKG, "Kitchen.CmdLine.RepJobName"),
            optionJobname = new StringBuffer()),
        new CommandLineOption("dir", BaseMessages.getString(PKG, "Kitchen.CmdLine.RepDir"),
            optionDirname = new StringBuffer()),
        new CommandLineOption("file", BaseMessages.getString(PKG, "Kitchen.CmdLine.XMLJob"),
            optionFilename = new StringBuffer()),
        new CommandLineOption("level", BaseMessages.getString(PKG, "Kitchen.CmdLine.LogLevel"),
            optionLoglevel = new StringBuffer()),
        new CommandLineOption("logfile", BaseMessages.getString(PKG, "Kitchen.CmdLine.LogFile"),
            optionLogfile = new StringBuffer()),
        new CommandLineOption("log", BaseMessages.getString(PKG, "Kitchen.CmdLine.LogFileOld"),
            optionLogfileOld = new StringBuffer(), false, true),
        new CommandLineOption("listdir", BaseMessages.getString(PKG, "Kitchen.CmdLine.ListDir"),
            optionListdir = new StringBuffer(), true, false),
        new CommandLineOption("listjobs", BaseMessages.getString(PKG, "Kitchen.CmdLine.ListJobsDir"),
            optionListjobs = new StringBuffer(), true, false),
        new CommandLineOption("listrep", BaseMessages.getString(PKG, "Kitchen.CmdLine.ListAvailableReps"),
            optionListrep = new StringBuffer(), true, false),
        new CommandLineOption("norep", BaseMessages.getString(PKG, "Kitchen.CmdLine.NoRep"),
            optionNorep = new StringBuffer(), true, false),
        new CommandLineOption("version", BaseMessages.getString(PKG, "Kitchen.CmdLine.Version"),
            optionVersion = new StringBuffer(), true, false),
        new CommandLineOption("param", BaseMessages.getString(PKG, "Kitchen.ComdLine.Param"), 
            optionParams, false),
        new CommandLineOption("listparam", BaseMessages.getString(PKG, "Kitchen.ComdLine.ListParam"),
            optionListParam = new StringBuffer(), true, false),
        new CommandLineOption("export", BaseMessages.getString(PKG, "Kitchen.ComdLine.Export"),
            optionExport = new StringBuffer(), true, false),
        new CommandLineOption("custom", BaseMessages.getString(PKG, "Kitchen.ComdLine.Custom"),
            customOptions, false), 
        maxLogLinesOption, 
        maxLogTimeoutOption, 
         };

    if (args.size() == 0) {
      CommandLineOption.printUsage(options);
      exitJVM(9);
    }

    LogChannelInterface log = new LogChannel(STRING_KITCHEN);

    CommandLineOption.parseArguments(args, options, log);

    configureLogging(maxLogLinesOption, maxLogTimeoutOption);

    String kettleRepname = Const.getEnvironmentVariable("KETTLE_REPOSITORY", null);
    String kettleUsername = Const.getEnvironmentVariable("KETTLE_USER", null);
    String kettlePassword = Const.getEnvironmentVariable("KETTLE_PASSWORD", null);

    if (!Const.isEmpty(kettleRepname))
      optionRepname = new StringBuffer(kettleRepname);
    if (!Const.isEmpty(kettleUsername))
      optionUsername = new StringBuffer(kettleUsername);
    if (!Const.isEmpty(kettlePassword))
      optionPassword = new StringBuffer(kettlePassword);

    if (Const.isEmpty(optionLogfile) && !Const.isEmpty(optionLogfileOld)) {
      // if the old style of logging name is filled in, and the new one is not
      // overwrite the new by the old
      optionLogfile = optionLogfileOld;
    }

    Pair<KettlePluginException, Future<KettleException>> repositoryRegisterResults = repositoryRegisterFuture.get();
    KettlePluginException repositoryRegisterException = repositoryRegisterResults.getFirst();
    if (repositoryRegisterException != null) {
      throw repositoryRegisterException;
    }
    Future<KettleException> kettleInitFuture = repositoryRegisterResults.getSecond();

    if (!Const.isEmpty(optionLogfile)) {
      fileAppender = new FileLoggingEventListener(optionLogfile.toString(), true);
      KettleLogStore.getAppender().addLoggingEventListener(fileAppender);
    } else {
      fileAppender = null;
    }

    if (!Const.isEmpty(optionLoglevel)) {
      log.setLogLevel(LogLevel.getLogLevelForCode(optionLoglevel.toString()));
      log.logMinimal(BaseMessages.getString(PKG, "Kitchen.Log.LogLevel", log.getLogLevel().getDescription()));
    }

    if (!Const.isEmpty(optionVersion)) {
      BuildVersion buildVersion = BuildVersion.getInstance();
      log.logBasic(BaseMessages.getString(PKG, "Kitchen.Log.KettleVersion", buildVersion.getVersion(),
          buildVersion.getRevision(), buildVersion.getBuildDate()));
      if (a.length == 1)
        exitJVM(6);
    }

    // Start the action...
    //
    if (!Const.isEmpty(optionRepname) && !Const.isEmpty(optionUsername)) {
      if (log.isDetailed())
        log.logDetailed(BaseMessages.getString(PKG, "Kitchen.Log.RepUsernameSupplied"));
    }

    log.logMinimal(BaseMessages.getString(PKG, "Kitchen.Log.Starting"));

    Date start, stop;
    Calendar cal;
    SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    cal = Calendar.getInstance();
    start = cal.getTime();

    if (log.isDebug())
      log.logDebug(BaseMessages.getString(PKG, "Kitchen.Log.AllocateNewJob"));

    JobMeta jobMeta = new JobMeta();
    // In case we use a repository...
    Repository repository = null;

    try {
      // Read kettle job specified on command-line?
      if (!Const.isEmpty(optionRepname) || !Const.isEmpty(optionFilename)) {
        if (log.isDebug())
          log.logDebug(BaseMessages.getString(PKG, "Kitchen.Log.ParsingCommandLine"));
        if (!Const.isEmpty(optionRepname) && !"Y".equalsIgnoreCase(optionNorep.toString())) {
          if (log.isDebug())
            log.logDebug(BaseMessages.getString(PKG, "Kitchen.Log.LoadingRep"));

          RepositoriesMeta repsinfo = new RepositoriesMeta();
          try {
            repsinfo.readData();
          } catch (Exception e) {
            throw new KettleException(BaseMessages.getString(PKG, "Kitchen.Error.NoRepDefinied"), e);
          }

          if (log.isDebug())
            log.logDebug(BaseMessages.getString(PKG, "Kitchen.Log.FindingRep", "" + optionRepname));
          repositoryMeta = repsinfo.findRepository(optionRepname.toString());
          if (repositoryMeta != null) {
            // Define and connect to the repository...
            if (log.isDebug())
              log.logDebug(BaseMessages.getString(PKG, "Kitchen.Log.Alocate&ConnectRep"));

            repository = PluginRegistry.getInstance().loadClass(RepositoryPluginType.class, repositoryMeta,
                Repository.class);
            repository.init(repositoryMeta);

            repository.connect(optionUsername != null ? optionUsername.toString() : null,
                optionPassword != null ? optionPassword.toString() : null);

            RepositoryDirectoryInterface directory = repository.loadRepositoryDirectoryTree(); // Default = root

            // Add the IMetaStore of the repository to our delegation
            //
            if (repository.getMetaStore()!=null) {
              metaStore.addMetaStore(repository.getMetaStore());
            }
            
            // Find the directory name if one is specified...
            if (!Const.isEmpty(optionDirname)) {
              directory = directory.findDirectory(optionDirname.toString());
            }

            if (directory != null) {
              // Check username, password
              if (log.isDebug())
                log.logDebug(BaseMessages.getString(PKG, "Kitchen.Log.CheckUserPass"));

              // Load a job
              if (!Const.isEmpty(optionJobname)) {
                if (log.isDebug())
                  log.logDebug(BaseMessages.getString(PKG, "Kitchen.Log.LoadingJobInfo"));
                blockAndThrow(kettleInitFuture);
                jobMeta = repository.loadJob(optionJobname.toString(), directory, null, null); // reads last version
                if (log.isDebug())
                  log.logDebug(BaseMessages.getString(PKG, "Kitchen.Log.AllocateJob"));

                job = new Job(repository, jobMeta);
              } else
              // List the jobs in the repository
              if ("Y".equalsIgnoreCase(optionListjobs.toString())) {
                if (log.isDebug())
                  log.logDebug(BaseMessages.getString(PKG, "Kitchen.Log.GettingLostJobsInDirectory", "" + directory));

                String jobnames[] = repository.getJobNames(directory.getObjectId(), false);
                for (int i = 0; i < jobnames.length; i++) {
                  System.out.println(jobnames[i]);
                }
              } else
              // List the directories in the repository
              if ("Y".equalsIgnoreCase(optionListdir.toString())) {
                String dirnames[] = repository.getDirectoryNames(directory.getObjectId());
                for (int i = 0; i < dirnames.length; i++) {
                  System.out.println(dirnames[i]);
                }
              }
            } else {
              System.out.println(BaseMessages.getString(PKG, "Kitchen.Error.CanNotFindSuppliedDirectory", optionDirname
                  + ""));

              repositoryMeta = null;
            }
          } else {
            System.out.println(BaseMessages.getString(PKG, "Kitchen.Error.NoRepProvided"));
          }
        }

        // Try to load if from file anyway.
        if (!Const.isEmpty(optionFilename) && job == null) {
          blockAndThrow(kettleInitFuture);
          jobMeta = new JobMeta(optionFilename.toString(), null, null);
          job = new Job(null, jobMeta);
        }
      } else if ("Y".equalsIgnoreCase(optionListrep.toString())) {
        RepositoriesMeta ri = new RepositoriesMeta();
        ri.readData();

        System.out.println(BaseMessages.getString(PKG, "Kitchen.Log.ListRep"));

        for (int i = 0; i < ri.nrRepositories(); i++) {
          RepositoryMeta rinfo = ri.getRepository(i);
          System.out.println("#" + (i + 1) + " : " + rinfo.getName() + " [" + rinfo.getDescription() + "]  id="
              + rinfo.getId());
        }
      }
    } catch (KettleException e) {
      job = null;
      jobMeta = null;
      System.out.println(BaseMessages.getString(PKG, "Kitchen.Error.StopProcess", e.getMessage()));

    }

    if (job == null) {
      if (!"Y".equalsIgnoreCase(optionListjobs.toString()) && !"Y".equalsIgnoreCase(optionListdir.toString())
          && !"Y".equalsIgnoreCase(optionListrep.toString())) {
        System.out.println(BaseMessages.getString(PKG, "Kitchen.Error.canNotLoadJob"));
      }

      exitJVM(7);
    }

    if (!Const.isEmpty(optionExport.toString())) {

      try {
        // Export the resources linked to the currently loaded file...
        //
        TopLevelResource topLevelResource = ResourceUtil.serializeResourceExportInterface(optionExport.toString(),
            job.getJobMeta(), job, repository, metaStore);
        String launchFile = topLevelResource.getResourceName();
        String message = ResourceUtil.getExplanation(optionExport.toString(), launchFile, job.getJobMeta());
        System.out.println();
        System.out.println(message);

        // Setting the list parameters option will make kitchen exit below in the parameters section
        //
        optionListParam = new StringBuffer("Y");
      } catch (Exception e) {
        System.out.println(Const.getStackTracker(e));
        exitJVM(2);
      }
    }

    Result result = null;

    int returnCode = 0;

    try {
      // Set the command line arguments on the job ...
      //
      if (args.size() == 0) {
        job.setArguments(null);
      } else {
        job.setArguments(args.toArray(new String[args.size()]));
      }
      job.initializeVariablesFrom(null);
      job.setLogLevel(log.getLogLevel());
      job.getJobMeta().setInternalKettleVariables(job);

      // Map the command line named parameters to the actual named parameters. Skip for
      // the moment any extra command line parameter not known in the job.
      String[] jobParams = jobMeta.listParameters();
      for (String param : jobParams) {
        String value = optionParams.getParameterValue(param);
        if (value != null) {
          job.getJobMeta().setParameterValue(param, value);
        }
      }
      job.copyParametersFrom(job.getJobMeta());

      // Put the parameters over the already defined variable space. Parameters get priority.
      //
      job.activateParameters();
      
      // Set custom options in the job extension map as Strings
      //
      for (String optionName : customOptions.listParameters()) {
        String optionValue = customOptions.getParameterValue(optionName);
        if (optionName!=null && optionValue!=null) {
          job.getExtensionDataMap().put(optionName, optionValue);
        }
      }

      // List the parameters defined in this job
      // Then simply exit...
      //
      if ("Y".equalsIgnoreCase(optionListParam.toString())) {
        for (String parameterName : job.listParameters()) {
          String value = job.getParameterValue(parameterName);
          String deflt = job.getParameterDefault(parameterName);
          String descr = job.getParameterDescription(parameterName);

          if (deflt != null) {
            System.out.println("Parameter: " + parameterName + "=" + Const.NVL(value, "") + ", default=" + deflt
                + " : " + Const.NVL(descr, ""));
          } else {
            System.out.println("Parameter: " + parameterName + "=" + Const.NVL(value, "") + " : "
                + Const.NVL(descr, ""));
          }
        }

        // stop right here...
        //
        exitJVM(7); // same as the other list options
      }

      job.start();
      job.waitUntilFinished();
      result = job.getResult(); // Execute the selected job.
    } finally {
      if (repository != null)
        repository.disconnect();
    }

    log.logMinimal(BaseMessages.getString(PKG, "Kitchen.Log.Finished"));

    if (result != null && result.getNrErrors() != 0) {
      log.logError(BaseMessages.getString(PKG, "Kitchen.Error.FinishedWithErrors"));
      returnCode = 1;
    }
    cal = Calendar.getInstance();
    stop = cal.getTime();
    String begin = df.format(start).toString();
    String end = df.format(stop).toString();

    log.logMinimal(BaseMessages.getString(PKG, "Kitchen.Log.StartStop", begin, end));

    long seconds = (stop.getTime() - start.getTime()) / 1000;
    if (seconds <= 60) {
      log.logMinimal(BaseMessages.getString(PKG, "Kitchen.Log.ProcessEndAfter", String.valueOf(seconds)));
    } else if (seconds <= 60 * 60) {
      int min = (int) (seconds / 60);
      int rem = (int) (seconds % 60);
      log.logMinimal(BaseMessages.getString(PKG, "Kitchen.Log.ProcessEndAfterLong", String.valueOf(min),
          String.valueOf(rem), String.valueOf(seconds)));
    } else if (seconds <= 60 * 60 * 24) {
      int rem;
      int hour = (int) (seconds / (60 * 60));
      rem = (int) (seconds % (60 * 60));
      int min = rem / 60;
      rem = rem % 60;
      log.logMinimal(BaseMessages.getString(PKG, "Kitchen.Log.ProcessEndAfterLonger", String.valueOf(hour),
          String.valueOf(min), String.valueOf(rem), String.valueOf(seconds)));
    } else {
      int rem;
      int days = (int) (seconds / (60 * 60 * 24));
      rem = (int) (seconds % (60 * 60 * 24));
      int hour = rem / (60 * 60);
      rem = rem % (60 * 60);
      int min = rem / 60;
      rem = rem % 60;
      log.logMinimal(BaseMessages.getString(PKG, "Kitchen.Log.ProcessEndAfterLongest", String.valueOf(days),
          String.valueOf(hour), String.valueOf(min), String.valueOf(rem), String.valueOf(seconds)));
    }

    if (fileAppender != null) {
        fileAppender.close();
        KettleLogStore.getAppender().removeLoggingEventListener(fileAppender);
    }

    exitJVM(returnCode);

  }
  
  private static <T extends Throwable> void blockAndThrow(Future<T> future) throws T {
    try {
      T e = future.get();
      if (e != null) {
        throw e;
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Configure the central log store from the provided command line options
   * 
   * @param maxLogLinesOption Option for maximum log lines
   * @param maxLogTimeoutOption Option for log timeout
   * @throws KettleException Error parsing command line arguments
   */
  public static void configureLogging(final CommandLineOption maxLogLinesOption,
      final CommandLineOption maxLogTimeoutOption) throws KettleException {
    int maxLogLines = parseIntArgument(maxLogLinesOption, 0);
    if (Const.isEmpty(maxLogLinesOption.getArgument())) {
      maxLogLines = Const.toInt(EnvUtil.getSystemProperty(Const.KETTLE_MAX_LOG_SIZE_IN_LINES), 5000);
    }
    int maxLogTimeout = parseIntArgument(maxLogTimeoutOption, 0);
    if (Const.isEmpty(maxLogTimeoutOption.getArgument())) {
      maxLogTimeout = Const.toInt(EnvUtil.getSystemProperty(Const.KETTLE_MAX_LOG_TIMEOUT_IN_MINUTES), 1440);
    }
    KettleLogStore.init(maxLogLines, maxLogTimeout);
  }

  /**
   * Parse an argument as an integer.
   * 
   * @param option Command Line Option to parse argument of 
   * @param def Default if the argument is not set
   * @return The parsed argument or the default if the argument was not specified
   * @throws KettleException Error parsing provided argument as an integer
   */
  protected static int parseIntArgument(final CommandLineOption option, final int def) throws KettleException {
    if (!Const.isEmpty(option.getArgument())) {
      try {
        return Integer.parseInt(option.getArgument().toString());
      } catch (NumberFormatException ex) {
        throw new KettleException(BaseMessages.getString(PKG,
            "Kitchen.Error.InvalidNumberArgument", option.getOption(), option.getArgument())); 
      }
    }
    return def;
  }

  private static final void exitJVM(int status) {

    System.exit(status);
  }
}