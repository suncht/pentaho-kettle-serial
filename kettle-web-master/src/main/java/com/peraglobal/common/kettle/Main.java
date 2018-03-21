package com.peraglobal.common.kettle;

/**
 *      Copyright (C) 2008 10gen Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.ProgressNullMonitorListener;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.logging.LogLevel;
import org.pentaho.di.core.logging.LoggingBuffer;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.kdr.KettleDatabaseRepository;
import org.pentaho.di.repository.kdr.KettleDatabaseRepositoryMeta;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;

public class Main {

	public static void main(String[] args) throws Exception{
		KettleEnvironment.init();		
		//数据库连接元对象
    	//DatabaseMeta databaseMeta = new DatabaseMeta(null, "MYSQL", "Native", "172.17.7.78", "kettle-repository", "3306", "root", "123456");
    	DatabaseMeta databaseMeta = new DatabaseMeta(null, "MYSQL", "Native", "suncht", "kettle-repository", "3306", "root", "1234");
    	//资源库元对象
        KettleDatabaseRepositoryMeta repositoryInfo = new KettleDatabaseRepositoryMeta();
        repositoryInfo.setConnection(databaseMeta);
        //资源库
        KettleDatabaseRepository repository = new KettleDatabaseRepository();
        repository.init(repositoryInfo);
        repository.connect("admin", "admin");
        //判断是否连接成功
        if (repository.isConnected()) {
        	System.out.println( "connected" );
        }else{
        	System.out.println("error");
        }
        RepositoryDirectoryInterface jobDirectory = repository.loadRepositoryDirectoryTree().findDirectory("/job");
        //runTranslate(repository, jobDirectory, "kettle-test-6.2");
        runJob(repository, jobDirectory, "mysql-mysql");
	}
	
	public static void runTranslate(KettleDatabaseRepository repository, RepositoryDirectoryInterface directory, String transName) throws KettleException{
		TransMeta transMeta = repository.loadTransformation(transName, directory, new ProgressNullMonitorListener(), true, "1.0");
		transMeta.setCapturingStepPerformanceSnapShots(true);
		Trans trans = new Trans(transMeta);  
        trans.setLogLevel(LogLevel.DEBUG);
        trans.setMonitored(true);
		trans.setInitializing(true);
		trans.setPreparing(true);
		trans.setRunning(true);
		trans.setSafeModeEnabled(true);
		trans.execute(null);
		trans.waitUntilFinished();     
        if (trans.isFinished()){
        	System.out.println("执行成功");
        	/*Map<String, List<StepPerformanceSnapShot>> stepPerformanceSnapShots = trans.getStepPerformanceSnapShots();
        	stepPerformanceSnapShots.forEach((str, StepPerformanceSnapShotList) -> {
        		for (StepPerformanceSnapShot stepPerformanceSnapShot : StepPerformanceSnapShotList){
        			System.out.println(JSONObject.fromObject(stepPerformanceSnapShot).toString());
        		}        		
        	});*/
        	
        	String logChannelId = trans.getLogChannelId();
        	LoggingBuffer appender = KettleLogStore.getAppender();
        	String logText = appender.getBuffer(logChannelId, true).toString();
        	System.out.println(logText);
        }else{
        	System.out.println("执行失败");
        }
	}
	
	public static void runJob(KettleDatabaseRepository repository, RepositoryDirectoryInterface directory, String jobName) throws KettleException{	
        JobMeta jobMeta = repository.loadJob(jobName, directory, new ProgressNullMonitorListener(), null);
        Job job = new Job(repository, jobMeta);
        job.setDaemon(true);
        job.setLogLevel(LogLevel.DEBUG);
        job.run();
        job.waitUntilFinished();   
        if (job.isFinished()){
        	String logChannelId = job.getLogChannelId();
        	LoggingBuffer appender = KettleLogStore.getAppender();
        	String logText = appender.getBuffer(logChannelId, true).toString();
        	System.out.println(logText);
        }else{
        	System.out.println("执行失败");
        }
	}

	
	
}
