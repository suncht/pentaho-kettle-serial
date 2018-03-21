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

package org.pentaho.di.job.entries.filesexist;

import java.io.IOException;
import java.util.List;

import org.apache.commons.vfs.FileObject;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleDatabaseException;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryBase;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/**
 * This defines a Files exist job entry.
 * 
 * @author Samatar
 * @since 10-12-2007
 *
 */

public class JobEntryFilesExist extends JobEntryBase implements Cloneable, JobEntryInterface
{
	private static Class<?> PKG = JobEntryFilesExist.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private String filename;
	
	public String arguments[];
	
	public JobEntryFilesExist(String n)
	{
		super(n, ""); 
		filename=null;
		setID(-1L);	
	}

	public JobEntryFilesExist()
	{
		this("");
	}

    public Object clone()
    {
        JobEntryFilesExist je = (JobEntryFilesExist) super.clone();
        return je;
    }
    
	public String getXML()
	{
        StringBuffer retval = new StringBuffer();
		
		retval.append(super.getXML());		
		retval.append("      ").append(XMLHandler.addTagValue("filename",   filename));
		
		 retval.append("      <fields>").append(Const.CR); 
		    if (arguments != null) {
		      for (int i = 0; i < arguments.length; i++) {
		        retval.append("        <field>").append(Const.CR); 
		        retval.append("          ").append(XMLHandler.addTagValue("name", arguments[i]));
		        retval.append("        </field>").append(Const.CR); 
		      }
		    }
		    retval.append("      </fields>").append(Const.CR); 
		
		return retval.toString();
	}
	
	public void loadXML(Node entrynode, List<DatabaseMeta> databases, List<SlaveServer> slaveServers, Repository rep, IMetaStore metaStore) throws KettleXMLException
	{
		try
		{
			super.loadXML(entrynode, databases, slaveServers);
			filename      = XMLHandler.getTagValue(entrynode, "filename");
			
		    Node fields = XMLHandler.getSubNode(entrynode, "fields"); 

	        // How many field arguments?
	        int nrFields = XMLHandler.countNodes(fields, "field"); 
	        arguments = new String[nrFields];

	        // Read them all...
	        for (int i = 0; i < nrFields; i++) {
	        Node fnode = XMLHandler.getSubNodeByNr(fields, "field", i); 

	        arguments[i] = XMLHandler.getTagValue(fnode, "name"); 

	      }
		}
		catch(KettleXMLException xe)
		{
			throw new KettleXMLException(BaseMessages.getString(PKG, "JobEntryFilesExist.ERROR_0001_Cannot_Load_Job_Entry_From_Xml_Node", xe.getMessage()));
		}
	}

	public void loadRep(Repository rep, IMetaStore metaStore, ObjectId id_jobentry, List<DatabaseMeta> databases, List<SlaveServer> slaveServers) throws KettleException
	{
		try
		{
			filename = rep.getJobEntryAttributeString(id_jobentry, "filename");
			
			 // How many arguments?
	        int argnr = rep.countNrJobEntryAttributes(id_jobentry, "name"); 
	        arguments = new String[argnr];

	        // Read them all...
	        for (int a = 0; a < argnr; a++) 
	        {
	          arguments[a] = rep.getJobEntryAttributeString(id_jobentry, a, "name"); 
	        }
		}
		catch(KettleException dbe)
		{
			throw new KettleException(BaseMessages.getString(PKG, "JobEntryFilesExist.ERROR_0002_Cannot_Load_Job_From_Repository",""+id_jobentry, dbe.getMessage()));
		}
	}
	
	public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_job) throws KettleException
	{
		try
		{
			rep.saveJobEntryAttribute(id_job, getObjectId(), "filename", filename);
			
			   // save the arguments...
		    if (arguments != null) {
		       for (int i = 0; i < arguments.length; i++) {
		          rep.saveJobEntryAttribute(id_job, getObjectId(), i, "name", arguments[i]);
		       }
		    }
		}
		catch(KettleDatabaseException dbe)
		{
			throw new KettleException(BaseMessages.getString(PKG, "JobEntryFilesExist.ERROR_0003_Cannot_Save_Job_Entry",""+id_job, dbe.getMessage()));
		}
	}

	public void setFilename(String filename)
	{
		this.filename = filename;
	}
	
	public String getFilename()
	{
		return filename;
	}
    

	
	public Result execute(Result previousResult, int nr)
	{
		Result result = previousResult;
		result.setResult( false );
		int missingfiles=0;
		
		if (arguments != null) 
		{
		      for (int i = 0; i < arguments.length && !parentJob.isStopped(); i++) 
		      {
		    	  FileObject file =null;
		      
		    	  try
		            {
		    		   String realFilefoldername = environmentSubstitute(arguments[i]);
		    		   file = KettleVFS.getFileObject(realFilefoldername, this);
		    		  
		    		    if (file.exists() && file.isReadable())
		    		    {
		    		    	if(log.isDetailed())
		    		    		logDetailed(BaseMessages.getString(PKG, "JobEntryFilesExist.File_Exists", realFilefoldername)); 
		    		    }
		                else
		                {
		                	missingfiles ++;
		                	result.setNrErrors(missingfiles);
		                	if(log.isDetailed())
		                		logDetailed(BaseMessages.getString(PKG, "JobEntryFilesExist.File_Does_Not_Exist", realFilefoldername)); 
		                }
		    		  
		            }
		    	  	catch (Exception e)
		            {
		    	  		missingfiles ++;
		                result.setNrErrors(missingfiles);
		                logError(BaseMessages.getString(PKG, "JobEntryFilesExist.ERROR_0004_IO_Exception", e.toString()), e); 
		            }
		    	  	finally
		    	  	{
		    	  		if (file != null) {try {file.close();file=null;} catch (IOException ex) { /* Ignore */ }}
		    	  	}
		      }
		        
		}
		
		if(missingfiles==0) 
			result.setResult(true);
		
		return result;
	}    

	public boolean evaluates()
	{
		return true;
	}
    
   @Override
   public void check(List<CheckResultInterface> remarks, JobMeta jobMeta, VariableSpace space, Repository repository, IMetaStore metaStore) {
   }

}
