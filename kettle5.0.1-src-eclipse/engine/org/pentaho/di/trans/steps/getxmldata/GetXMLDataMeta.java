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

package org.pentaho.di.trans.steps.getxmldata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs.FileObject;
import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.fileinput.FileInputList;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.variables.VariableSpace;
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
import org.pentaho.di.trans.step.StepMetaInjectionInterface;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;


/**
 * Store run-time data on the getXMLData step.
 */
public class GetXMLDataMeta extends BaseStepMeta implements StepMetaInterface
{	
	private static Class<?> PKG = GetXMLDataMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private static final String YES = "Y";
	
	public static final String[] RequiredFilesDesc = new String[] { BaseMessages.getString(PKG, "System.Combo.No"), BaseMessages.getString(PKG, "System.Combo.Yes") };
	public static final String[] RequiredFilesCode = new String[] {"N", "Y"};
	
	public static final String AT= "@";
	public static final String N0DE_SEPARATOR = "/";
	
	/** Array of filenames */
	private  String  fileName[]; 

	/** Wildcard or filemask (regular expression) */
	private  String  fileMask[];
	
	/** Array of boolean values as string, indicating if a file is required. */
	private  String  fileRequired[];
	
	
	/** Wildcard or filemask to exclude (regular expression) */
	private String             excludeFileMask[];
 	 
	/** Flag indicating that we should include the filename in the output */
	private  boolean includeFilename;
	
	/** The name of the field in the output containing the filename */
	private  String  filenameField;
	
	/** Flag indicating that a row number field should be included in the output */
	private  boolean includeRowNumber;
	
	/** The name of the field in the output containing the row number*/
	private  String  rowNumberField;
	
	/** The maximum number or lines to read */
	private  long  rowLimit;

    /** The XPath location to loop over */
    private  String  loopxpath;

	/** The fields to import... */
	private GetXMLDataField inputFields[];
	
    /** The encoding to use for reading: null or empty string means system default encoding */
    private String encoding;
    
    /**  Is In fields     */
    private  String xmlField;
    
    /**  Is In fields     */
    private  boolean inFields;
    
    /**  Is a File     */
    private  boolean IsAFile;
    
    /** Flag: add result filename **/
    private boolean addResultFile;

    /** Flag: set Namespace aware **/
    private boolean nameSpaceAware;
    
    /** Flag: set XML Validating **/
    private boolean validating;
    
	/** Flag : do we process use tokens? */
	private boolean usetoken;
	
	/** Flag : do we ignore empty files */
	private boolean IsIgnoreEmptyFile;
	
	/** Array of boolean values as string, indicating if we need to fetch sub folders. */
	private  String  includeSubFolders[];
	
	/** Flag : do not fail if no file */
	private boolean doNotFailIfNoFile;
	
	/** Flag : ignore comments */
	private boolean ignorecomments;
	
	/** Flag : read url as source */
	private boolean readurl;
	
	// Given this path activates the streaming algorithm to process large files
	private String prunePath;
	
    
    /** Additional fields  **/
    private String shortFileFieldName;
    private String pathFieldName;
    private String hiddenFieldName;
    private String lastModificationTimeFieldName;
    private String uriNameFieldName;
    private String rootUriNameFieldName;
    private String extensionFieldName;
    private String sizeFieldName;
    	
	public GetXMLDataMeta()
	{
		super(); // allocate BaseStepMeta
	}

	/**
	 * @return Returns the shortFileFieldName.
	 */
    public String getShortFileNameField()
    {
    	return shortFileFieldName;
    }
    /**
	 * @param field The shortFileFieldName to set.
	 */
    public void setShortFileNameField(String field)
    {
    	shortFileFieldName=field;
    }
	
	/**
	 * @return Returns the pathFieldName.
	 */
    public String getPathField()
    {
    	return pathFieldName;
    }
    /**
	 * @param field The pathFieldName to set.
	 */
    public void setPathField(String field)
    {
    	this.pathFieldName=field;
    }
	/**
	 * @return Returns the hiddenFieldName.
	 */
    public String isHiddenField()
    {
    	return hiddenFieldName;
    }
    /**
	 * @param field The hiddenFieldName to set.
	 */
    public void setIsHiddenField(String field)
    {
    	hiddenFieldName=field;
    }
	/**
	 * @return Returns the lastModificationTimeFieldName.
	 */
    public String getLastModificationDateField()
    {
    	return lastModificationTimeFieldName;
    }
    /**
	 * @param field The lastModificationTimeFieldName to set.
	 */
    public void setLastModificationDateField(String field)
    {
    	lastModificationTimeFieldName=field;
    }
    /**
	 * @return Returns the uriNameFieldName.
	 */
    public String getUriField()
    {
    	return uriNameFieldName;
    }
    /**
	 * @param field The uriNameFieldName to set.
	 */
    public void setUriField(String field)
    {
    	uriNameFieldName=field;
    }
    /**
	 * @return Returns the uriNameFieldName.
	 */
    public String getRootUriField()
    {
    	return rootUriNameFieldName;
    }
    /**
	 * @param field The rootUriNameFieldName to set.
	 */
    public void setRootUriField(String field)
    {
    	rootUriNameFieldName=field;
    }
    /**
	 * @return Returns the extensionFieldName.
	 */
    public String getExtensionField()
    {
    	return extensionFieldName;
    }
    /**
	 * @param field The extensionFieldName to set.
	 */
    public void setExtensionField(String field)
    {
    	extensionFieldName=field;
    }
    /**
	 * @return Returns the sizeFieldName.
	 */
    public String getSizeField()
    {
    	return sizeFieldName;
    }
    /**
	 * @param field The sizeFieldName to set.
	 */
    public void setSizeField(String field)
    {
    	sizeFieldName=field;
    }
	/** 
	 * @return the add result filesname flag
	 */
	public boolean addResultFile()
	{
		return addResultFile;
	}
	

	
	/** 
	 * @return the validating flag
	 */
	public boolean isValidating()
	{
		return validating;
	}
	
	/** 
	 * @param validating the validating flag to set
	 */
	public void setValidating(boolean validating)
	{
		this.validating= validating;
	}
	/** 
	 * @return the readurl flag
	 */
	public boolean isReadUrl()
	{
		return readurl;
	}
	
	/** 
	 * @param readurl the readurl flag to set
	 */
	public void setReadUrl(boolean readurl)
	{
		this.readurl= readurl;
	}
	
	
	public void setAddResultFile(boolean addResultFile)
	{
		this.addResultFile=addResultFile;
	}
	
	/**
     * @return Returns the input fields.
     */
    public GetXMLDataField[] getInputFields()
    {
        return inputFields;
    }
    
    /**
     * @param inputFields The input fields to set.
     */
    public void setInputFields(GetXMLDataField[] inputFields)
    {
        this.inputFields = inputFields;
    }
    
	/**
	 * @return Returns the excludeFileMask.
	 */
	public String[] getExludeFileMask()
	{
		return excludeFileMask;
	}
	/**
	 * @param excludeFileMask The excludeFileMask to set.
	 */
	public void setExcludeFileMask(String[] excludeFileMask)
	{
		this.excludeFileMask = excludeFileMask;
	}
    
    /**
     * Get XML field.
     */
    public String getXMLField()
    {
        return xmlField;
    }
    
    /**
     * Set XML field.
     */ 
    public void setXMLField(String xmlField)
    {
        this.xmlField = xmlField;
    }
    
    /**  
     * Get the IsInFields.
     */
    public boolean isInFields()
    {
        return inFields;
    }
    
    /**  
     * @param inFields set the inFields.
     */
    public void setInFields(boolean inFields)
    {
        this.inFields = inFields;
    }

    /**
     * @return Returns the fileMask.
     */
    public String[] getFileMask()
    {
        return fileMask;
    }
    
    /**
     * @param fileMask The fileMask to set.
     */
    public void setFileMask(String[] fileMask)
    {
        this.fileMask = fileMask;
    }
    
	public String[] getFileRequired() {
		return fileRequired;
	}
    
	public void setFileRequired(String[] fileRequiredin) {
		for (int i=0;i<fileRequiredin.length;i++)
		{
			this.fileRequired[i] = getRequiredFilesCode(fileRequiredin[i]);
		}
	}

	public void setIncludeSubFolders(String[] includeSubFoldersin) {
		for (int i=0;i<includeSubFoldersin.length;i++)
		{
			this.includeSubFolders[i] = getRequiredFilesCode(includeSubFoldersin[i]);
		}
	}
    /**
     * @return Returns the fileName.
     */
    public String[] getFileName()
    {
        return fileName;
    }
    
    /**
     * @param fileName The fileName to set.
     */
    public void setFileName(String[] fileName)
    {
        this.fileName = fileName;
    }
    
    /**
     * @return Returns the filenameField.
     */
    public String getFilenameField()
    {
        return filenameField;
    }
    
    /**
     * @param filenameField The filenameField to set.
     */
    public void setFilenameField(String filenameField)
    {
        this.filenameField = filenameField;
    }
    
    /**
     * @return Returns the includeFilename.
     */
    public boolean includeFilename()
    {
        return includeFilename;
    }
    
    /**
     * @param includeFilename The includeFilename to set.
     */
    public void setIncludeFilename(boolean includeFilename)
    {
        this.includeFilename = includeFilename;
    }
    
    /**
     * @return Returns the includeRowNumber.
     */
    public boolean includeRowNumber()
    {
        return includeRowNumber;
    }
    
    /**
     * @param includeRowNumber The includeRowNumber to set.
     */
    public void setIncludeRowNumber(boolean includeRowNumber)
    {
        this.includeRowNumber = includeRowNumber;
    }
    
    /**
     * @return Returns the rowLimit.
     */
    public long getRowLimit()
    {
        return rowLimit;
    }
    
    /**
     * @param rowLimit The rowLimit to set.
     */
    public void setRowLimit(long rowLimit)
    {
        this.rowLimit = rowLimit;
    }
    
    /**
     * @return Returns the LoopXPath
     */
    public String getLoopXPath()
    {
        return loopxpath;
    }

    /**
     * @param loopxpath The loopxpath to set.
     */
    public void setLoopXPath(String loopxpath)
    {
        this.loopxpath = loopxpath;
    }
    
	/** 
	 * @param usetoken the "use token" flag to set
	 */
    public void setuseToken(boolean usetoken)
	{
		this.usetoken= usetoken;
	}
	
	/** 
	 * @return the use token flag
	 */
	public boolean isuseToken()
	{
		return usetoken;
	}
	
	/** 
	 * @return the IsIgnoreEmptyFile flag
	 */
	public boolean isIgnoreEmptyFile()
	{
		return IsIgnoreEmptyFile;
	}
	
	/** 
	 * @param IsIgnoreEmptyFile the IsIgnoreEmptyFile to set
	 */
	public void setIgnoreEmptyFile(boolean IsIgnoreEmptyFile)
	{
		this.IsIgnoreEmptyFile= IsIgnoreEmptyFile;
	}
	
	
	/** 
	 * @return the doNotFailIfNoFile flag
	 */
	public boolean isdoNotFailIfNoFile()
	{
		return doNotFailIfNoFile;
	}
	
	/** 
	 * @param doNotFailIfNoFile the doNotFailIfNoFile to set
	 */
	public void setdoNotFailIfNoFile(boolean doNotFailIfNoFile)
	{
		this.doNotFailIfNoFile= doNotFailIfNoFile;
	}
	
	/** 
	 * @return the ignorecomments flag
	 */
	public boolean isIgnoreComments()
	{
		return ignorecomments;
	}
	
	/** 
	 * @param ignorecomments the ignorecomments to set
	 */
	public void setIgnoreComments(boolean ignorecomments)
	{
		this.ignorecomments= ignorecomments;
	}
	/** 
	 * @param nameSpaceAware the name space aware flag to set
	 */
	public void setNamespaceAware(boolean nameSpaceAware)
	{
		this.nameSpaceAware= nameSpaceAware;
	}
	
	/** 
	 * @return the name space aware flag
	 */
	public boolean isNamespaceAware()
	{
		return nameSpaceAware;
	}
	
    /**
     * @return Returns the rowNumberField.
     */
    public String getRowNumberField()
    {
        return rowNumberField;
    }
    
    /**
     * @param rowNumberField The rowNumberField to set.
     */
    public void setRowNumberField(String rowNumberField)
    {
        this.rowNumberField = rowNumberField;
    }
    
    /**
     * @return the encoding
     */
    public String getEncoding()
    {
        return encoding;
    }

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }    
    
    public boolean getIsAFile()
    {
    	return IsAFile;
    }    
    
    public void setIsAFile(boolean IsAFile)
    {
    	this.IsAFile = IsAFile;
    }

	/**
	 * @return the prunePath
	 */
	public String getPrunePath() {
		return prunePath;
	}
	public String[] getIncludeSubFolders() {
		return includeSubFolders;
	}
	/**
	 * @param prunePath the prunePath to set
	 */
	public void setPrunePath(String prunePath) {
		this.prunePath = prunePath;
	}
	
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore)
  	    throws KettleXMLException
	{
    	readData(stepnode);
	}

	public Object clone()
	{
		GetXMLDataMeta retval = (GetXMLDataMeta)super.clone();
		
		int nrFiles  = fileName.length;
		int nrFields = inputFields.length;

		retval.allocate(nrFiles, nrFields);
		
        for (int i = 0; i < nrFiles; i++)
        {
            retval.fileName[i]     = fileName[i];
            retval.fileMask[i]     = fileMask[i];
            retval.excludeFileMask[i] = excludeFileMask[i];
            retval.fileRequired[i] = fileRequired[i];
            retval.includeSubFolders[i] = includeSubFolders[i];
        }
		
		for (int i=0;i<nrFields;i++)
		{
            if (inputFields[i]!=null)
            {
                retval.inputFields[i] = (GetXMLDataField)inputFields[i].clone();
            }
		}		
		return retval;
	}

    public String getXML()
    {
        StringBuffer retval=new StringBuffer(400);
        
        retval.append("    ").append(XMLHandler.addTagValue("include",         includeFilename));
        retval.append("    ").append(XMLHandler.addTagValue("include_field",   filenameField));
        retval.append("    ").append(XMLHandler.addTagValue("rownum",          includeRowNumber));
        retval.append("    ").append(XMLHandler.addTagValue("addresultfile",   addResultFile));
        retval.append("    ").append(XMLHandler.addTagValue("namespaceaware",  nameSpaceAware));
        retval.append("    ").append(XMLHandler.addTagValue("ignorecomments",  ignorecomments));
        retval.append("    ").append(XMLHandler.addTagValue("readurl",  readurl));
        retval.append("    ").append(XMLHandler.addTagValue("validating",      validating));
        retval.append("    "+XMLHandler.addTagValue("usetoken",   usetoken));
        retval.append("    "+XMLHandler.addTagValue("IsIgnoreEmptyFile",   IsIgnoreEmptyFile));
        retval.append("    "+XMLHandler.addTagValue("doNotFailIfNoFile",   doNotFailIfNoFile));
         
        retval.append("    ").append(XMLHandler.addTagValue("rownum_field",    rowNumberField));
        retval.append("    ").append(XMLHandler.addTagValue("encoding",        encoding));
        
        retval.append("    <file>").append(Const.CR);
        for (int i=0;i<fileName.length;i++)
        {
            retval.append("      ").append(XMLHandler.addTagValue("name",     fileName[i]));
            retval.append("      ").append(XMLHandler.addTagValue("filemask", fileMask[i]));
			retval.append("      ").append(XMLHandler.addTagValue("exclude_filemask", excludeFileMask[i]));
            retval.append("      ").append(XMLHandler.addTagValue("file_required", fileRequired[i]));
            retval.append("      ").append(XMLHandler.addTagValue("include_subfolders", includeSubFolders[i]));  
               
        }
        retval.append("    </file>").append(Const.CR);
        
        retval.append("    <fields>").append(Const.CR);
        for (int i=0;i<inputFields.length;i++)
        {
            GetXMLDataField field = inputFields[i];
            retval.append(field.getXML());
        }
        retval.append("    </fields>").append(Const.CR);
        
        retval.append("    ").append(XMLHandler.addTagValue("limit", rowLimit));
        retval.append("    ").append(XMLHandler.addTagValue("loopxpath", loopxpath));
        retval.append("    ").append(XMLHandler.addTagValue("IsInFields", inFields));
        retval.append("    ").append(XMLHandler.addTagValue("IsAFile", IsAFile));        
        retval.append("    ").append(XMLHandler.addTagValue("XmlField", xmlField));
        retval.append("    ").append(XMLHandler.addTagValue("prunePath", prunePath));
		retval.append("    ").append(XMLHandler.addTagValue("shortFileFieldName", shortFileFieldName));
		retval.append("    ").append(XMLHandler.addTagValue("pathFieldName", pathFieldName));
		retval.append("    ").append(XMLHandler.addTagValue("hiddenFieldName", hiddenFieldName));
		retval.append("    ").append(XMLHandler.addTagValue("lastModificationTimeFieldName", lastModificationTimeFieldName));
		retval.append("    ").append(XMLHandler.addTagValue("uriNameFieldName", uriNameFieldName));
		retval.append("    ").append(XMLHandler.addTagValue("rootUriNameFieldName", rootUriNameFieldName));
		retval.append("    ").append(XMLHandler.addTagValue("extensionFieldName", extensionFieldName));
		retval.append("    ").append(XMLHandler.addTagValue("sizeFieldName", sizeFieldName));
        return retval.toString();
    }
     public String getRequiredFilesDesc(String tt)
     {
    	 if(Const.isEmpty(tt)) return RequiredFilesDesc[0]; 
 		if(tt.equalsIgnoreCase(RequiredFilesCode[1]))
			return RequiredFilesDesc[1];
		else
			return RequiredFilesDesc[0]; 
     }
     public String getRequiredFilesCode(String tt)
     {
    	if(tt==null) return RequiredFilesCode[0]; 
 		if(tt.equals(RequiredFilesDesc[1]))
			return RequiredFilesCode[1];
		else
			return RequiredFilesCode[0]; 
     }
	private void readData(Node stepnode) throws KettleXMLException
	{
		try
		{
			includeFilename   = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "include"));
			filenameField     = XMLHandler.getTagValue(stepnode, "include_field");
			
			addResultFile     = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "addresultfile"));
			nameSpaceAware    = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "namespaceaware"));
			ignorecomments    = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "ignorecomments"));
			
			readurl    = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "readurl"));
			validating        = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "validating"));
			usetoken  = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "usetoken"));
			IsIgnoreEmptyFile  = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "IsIgnoreEmptyFile"));
			doNotFailIfNoFile  = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "doNotFailIfNoFile"));
			
			includeRowNumber  = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "rownum"));
			rowNumberField    = XMLHandler.getTagValue(stepnode, "rownum_field");
			encoding          = XMLHandler.getTagValue(stepnode, "encoding");
	
			Node filenode     = XMLHandler.getSubNode(stepnode,   "file");
			Node fields       = XMLHandler.getSubNode(stepnode,   "fields");
			int nrFiles       = XMLHandler.countNodes(filenode,  "name");
			int nrFields      = XMLHandler.countNodes(fields,    "field");
	
			allocate(nrFiles, nrFields);
			
			for (int i=0;i<nrFiles;i++)
			{
				Node filenamenode = XMLHandler.getSubNodeByNr(filenode, "name", i); 
				Node filemasknode = XMLHandler.getSubNodeByNr(filenode, "filemask", i); 
				Node excludefilemasknode     = XMLHandler.getSubNodeByNr(filenode, "exclude_filemask", i);
				Node fileRequirednode = XMLHandler.getSubNodeByNr(filenode, "file_required", i);
				Node includeSubFoldersnode = XMLHandler.getSubNodeByNr(filenode, "include_subfolders", i);
				fileName[i] = XMLHandler.getNodeValue(filenamenode);
				fileMask[i] = XMLHandler.getNodeValue(filemasknode);
				excludeFileMask[i]    = XMLHandler.getNodeValue(excludefilemasknode);
				fileRequired[i] = XMLHandler.getNodeValue(fileRequirednode);
				includeSubFolders[i] = XMLHandler.getNodeValue(includeSubFoldersnode);
			}
			
			for (int i=0;i<nrFields;i++)
			{
				Node fnode = XMLHandler.getSubNodeByNr(fields, "field", i);
				GetXMLDataField field = new GetXMLDataField(fnode);
				inputFields[i] = field;
			} 
			
			// Is there a limit on the number of rows we process?
			rowLimit = Const.toLong(XMLHandler.getTagValue(stepnode, "limit"), 0L);
            // Do we skip rows before starting to read
			loopxpath = XMLHandler.getTagValue(stepnode, "loopxpath");
			
			inFields = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "IsInFields"));
			IsAFile = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "IsAFile"));
			
			xmlField = XMLHandler.getTagValue(stepnode, "XmlField");
			prunePath = XMLHandler.getTagValue(stepnode, "prunePath");
            
			shortFileFieldName = XMLHandler.getTagValue(stepnode, "shortFileFieldName");
			pathFieldName = XMLHandler.getTagValue(stepnode, "pathFieldName");
			hiddenFieldName = XMLHandler.getTagValue(stepnode, "hiddenFieldName");
			lastModificationTimeFieldName = XMLHandler.getTagValue(stepnode, "lastModificationTimeFieldName");
			uriNameFieldName = XMLHandler.getTagValue(stepnode, "uriNameFieldName");
			rootUriNameFieldName = XMLHandler.getTagValue(stepnode, "rootUriNameFieldName");
			extensionFieldName = XMLHandler.getTagValue(stepnode, "extensionFieldName");
			sizeFieldName = XMLHandler.getTagValue(stepnode, "sizeFieldName");
		}
		catch(Exception e)
		{
			throw new KettleXMLException(BaseMessages.getString(PKG, "GetXMLDataMeta.Exception.ErrorLoadingXML", e.toString()));
		}
	}
	
	public void allocate(int nrfiles, int nrfields)
	{
	  allocateFiles(nrfiles);
		inputFields  = new GetXMLDataField[nrfields];
	}
	
  public void allocateFiles(int nrfiles)
  {
    fileName     = new String [nrfiles];
    fileMask     = new String [nrfiles];
    excludeFileMask = new String[nrfiles];
    fileRequired = new String[nrfiles];
    includeSubFolders = new String[nrfiles];
  }
	
	public void setDefault()
	{
	    shortFileFieldName=null;
	    pathFieldName=null;
	    hiddenFieldName=null;
	    lastModificationTimeFieldName=null;
	    uriNameFieldName=null;
	    rootUriNameFieldName=null;
	    extensionFieldName=null;
	    sizeFieldName=null;
	    
	    
		usetoken=false;
		IsIgnoreEmptyFile=false;
		doNotFailIfNoFile=true;
		includeFilename = false;
		filenameField = "";
		includeRowNumber = false;
		rowNumberField = "";
		IsAFile = false;
		addResultFile = false;
		nameSpaceAware = false;
		ignorecomments=false;
		readurl=false;
		validating = false;
		
		int nrFiles=0;
		int nrFields=0;
		loopxpath= "";

		allocate(nrFiles, nrFields);	
		
		for (int i=0;i<nrFiles;i++) 
		{
			fileName[i] = "filename"+(i+1);
			fileMask[i] = "";
			excludeFileMask[i]="";
			fileRequired[i] = RequiredFilesCode[0];
			includeSubFolders[i] = RequiredFilesCode[0];
		}
		
		for (int i=0;i<nrFields;i++)
		{
		    inputFields[i] = new GetXMLDataField("field"+(i+1));
		}

		rowLimit = 0;
		
		inFields = false;
		xmlField = "";
		prunePath = "";
	}
	
	public void getFields(RowMetaInterface r, String name, RowMetaInterface info[], StepMeta nextStep, VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException
	{		
		int i;
		for (i=0;i<inputFields.length;i++)
		{
			GetXMLDataField field = inputFields[i];       
	        
			int type=field.getType();
			if (type==ValueMeta.TYPE_NONE) type=ValueMeta.TYPE_STRING;
			try {
        ValueMetaInterface v = ValueMetaFactory.createValueMeta(space.environmentSubstitute(field.getName()), type);
        v.setLength(field.getLength());
        v.setPrecision(field.getPrecision());
        v.setOrigin(name);
        v.setConversionMask(field.getFormat());
        v.setDecimalSymbol(field.getDecimalSymbol());
        v.setGroupingSymbol(field.getGroupSymbol());
        v.setCurrencySymbol(field.getCurrencySymbol());
        r.addValueMeta(v);
			} catch(Exception e) {
			  throw new KettleStepException(e);
			}
		}
		
		if (includeFilename)
		{
			ValueMetaInterface v = new ValueMeta(space.environmentSubstitute(filenameField), ValueMeta.TYPE_STRING);
			v.setLength(250);
            v.setPrecision(-1);
			v.setOrigin(name);
			r.addValueMeta(v);
		}

		if (includeRowNumber)
		{
			ValueMetaInterface v = new ValueMeta(space.environmentSubstitute(rowNumberField), ValueMeta.TYPE_INTEGER);
	        v.setLength(ValueMetaInterface.DEFAULT_INTEGER_LENGTH, 0);
			v.setOrigin(name);
			r.addValueMeta(v);
		}
		// Add additional fields

		if(getShortFileNameField()!=null && getShortFileNameField().length()>0)
		{
			ValueMetaInterface v = new ValueMeta(space.environmentSubstitute(getShortFileNameField()), ValueMeta.TYPE_STRING);
			v.setLength(100, -1);
			v.setOrigin(name);
			r.addValueMeta(v);
		}
		if(getExtensionField()!=null && getExtensionField().length()>0)
		{
			ValueMetaInterface v = new ValueMeta(space.environmentSubstitute(getExtensionField()), ValueMeta.TYPE_STRING);
			v.setLength(100, -1);
			v.setOrigin(name);
			r.addValueMeta(v);
		}
		if(getPathField()!=null && getPathField().length()>0)
		{
			ValueMetaInterface v = new ValueMeta(space.environmentSubstitute(getPathField()), ValueMeta.TYPE_STRING);
			v.setLength(100, -1);
			v.setOrigin(name);
			r.addValueMeta(v);
		}
		if(getSizeField()!=null && getSizeField().length()>0)
		{
			ValueMetaInterface v = new ValueMeta(space.environmentSubstitute(getSizeField()), ValueMeta.TYPE_INTEGER);
			v.setOrigin(name);
			v.setLength(9);
			r.addValueMeta(v);
		}
		if(isHiddenField()!=null && isHiddenField().length()>0)
		{
			ValueMetaInterface v = new ValueMeta(space.environmentSubstitute(isHiddenField()), ValueMeta.TYPE_BOOLEAN);
			v.setOrigin(name);
			r.addValueMeta(v);
		}

		if(getLastModificationDateField()!=null && getLastModificationDateField().length()>0)
		{
			ValueMetaInterface v = new ValueMeta(space.environmentSubstitute(getLastModificationDateField()), ValueMeta.TYPE_DATE);
			v.setOrigin(name);
			r.addValueMeta(v);
		}
		if(getUriField()!=null && getUriField().length()>0)
		{
			ValueMetaInterface v = new ValueMeta(space.environmentSubstitute(getUriField()), ValueMeta.TYPE_STRING);
			v.setLength(100, -1);
			v.setOrigin(name);
			r.addValueMeta(v);
		}

		if(getRootUriField()!=null && getRootUriField().length()>0)
		{
			ValueMetaInterface v = new ValueMeta(space.environmentSubstitute(getRootUriField()), ValueMeta.TYPE_STRING);
			v.setLength(100, -1);
			v.setOrigin(name);
			r.addValueMeta(v);
		}
	}
	
	public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases) throws KettleException
	{
	
		try
		{
			includeFilename   =   rep.getStepAttributeBoolean(id_step, "include");  
			filenameField     =   rep.getStepAttributeString (id_step, "include_field");
			
			addResultFile     =   rep.getStepAttributeBoolean(id_step, "addresultfile");
			nameSpaceAware    =   rep.getStepAttributeBoolean(id_step, "namespaceaware");
			ignorecomments    =   rep.getStepAttributeBoolean(id_step, "ignorecomments");
			readurl    =   rep.getStepAttributeBoolean(id_step, "readurl");
			
			validating        =   rep.getStepAttributeBoolean(id_step, "validating");
			usetoken  =      rep.getStepAttributeBoolean(id_step, "usetoken");
			IsIgnoreEmptyFile  =      rep.getStepAttributeBoolean(id_step, "IsIgnoreEmptyFile");
			doNotFailIfNoFile  =      rep.getStepAttributeBoolean(id_step, "doNotFailIfNoFile");

			includeRowNumber  =   rep.getStepAttributeBoolean(id_step, "rownum");
			rowNumberField    =   rep.getStepAttributeString (id_step, "rownum_field");
			rowLimit          =   rep.getStepAttributeInteger(id_step, "limit");
			loopxpath      	  =   rep.getStepAttributeString(id_step, "loopxpath");
			encoding          =   rep.getStepAttributeString (id_step, "encoding");
	
			int nrFiles       =   rep.countNrStepAttributes(id_step, "file_name");
			int nrFields      =   rep.countNrStepAttributes(id_step, "field_name");
            
			allocate(nrFiles, nrFields);

			for (int i=0;i<nrFiles;i++)
			{
				fileName[i] =      rep.getStepAttributeString (id_step, i, "file_name"    );
				fileMask[i] =      rep.getStepAttributeString (id_step, i, "file_mask"    );
				excludeFileMask[i] = rep.getStepAttributeString(id_step, i, "exclude_file_mask");
				fileRequired[i] =  rep.getStepAttributeString(id_step, i, "file_required");
				includeSubFolders[i] =  rep.getStepAttributeString(id_step, i, "include_subfolders");
			}

			for (int i=0;i<nrFields;i++)
			{
			    GetXMLDataField field = new GetXMLDataField();
			    
				field.setName( rep.getStepAttributeString (id_step, i, "field_name") );
				field.setXPath( rep.getStepAttributeString (id_step, i, "field_xpath") );
				field.setElementType( GetXMLDataField.getElementTypeByCode( rep.getStepAttributeString (id_step, i, "element_type") ));
				field.setResultType( GetXMLDataField.getResultTypeByCode( rep.getStepAttributeString (id_step, i, "result_type") ) );
				field.setType( ValueMeta.getType( rep.getStepAttributeString (id_step, i, "field_type") ) );
				field.setFormat( rep.getStepAttributeString (id_step, i, "field_format") );
				field.setCurrencySymbol( rep.getStepAttributeString (id_step, i, "field_currency") );
				field.setDecimalSymbol( rep.getStepAttributeString (id_step, i, "field_decimal") );
				field.setGroupSymbol( rep.getStepAttributeString (id_step, i, "field_group") );
				field.setLength( (int)rep.getStepAttributeInteger(id_step, i, "field_length") );
				field.setPrecision( (int)rep.getStepAttributeInteger(id_step, i, "field_precision") );
				field.setTrimType( GetXMLDataField.getTrimTypeByCode( rep.getStepAttributeString (id_step, i, "field_trim_type") ));
				field.setRepeated( rep.getStepAttributeBoolean(id_step, i, "field_repeat") );
                
				inputFields[i] = field;
			}
			inFields = rep.getStepAttributeBoolean (id_step, "IsInFields");
			IsAFile    = rep.getStepAttributeBoolean (id_step, "IsAFile");
			
			xmlField   = rep.getStepAttributeString (id_step, "XmlField");
			prunePath   = rep.getStepAttributeString (id_step, "prunePath");
			
			shortFileFieldName = rep.getStepAttributeString(id_step, "shortFileFieldName");
			pathFieldName = rep.getStepAttributeString(id_step, "pathFieldName");
			hiddenFieldName = rep.getStepAttributeString(id_step, "hiddenFieldName");
			lastModificationTimeFieldName = rep.getStepAttributeString(id_step, "lastModificationTimeFieldName");
			uriNameFieldName = rep.getStepAttributeString(id_step, "uriNameFieldName");
			rootUriNameFieldName = rep.getStepAttributeString(id_step, "rootUriNameFieldName");
			extensionFieldName = rep.getStepAttributeString(id_step, "extensionFieldName");
			sizeFieldName = rep.getStepAttributeString(id_step, "sizeFieldName");
		}
		catch(Exception e)
		{
			throw new KettleException(BaseMessages.getString(PKG, "GetXMLDataMeta.Exception.ErrorReadingRepository"), e);
		}
	}
	
	public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step)
		throws KettleException
	{
		try
		{
			rep.saveStepAttribute(id_transformation, id_step, "include",         includeFilename);
			rep.saveStepAttribute(id_transformation, id_step, "include_field",   filenameField);
			rep.saveStepAttribute(id_transformation, id_step, "addresultfile",   addResultFile);
			rep.saveStepAttribute(id_transformation, id_step, "namespaceaware",   nameSpaceAware);
			rep.saveStepAttribute(id_transformation, id_step, "ignorecomments",   ignorecomments);
			rep.saveStepAttribute(id_transformation, id_step, "readurl",   readurl);
			
			rep.saveStepAttribute(id_transformation, id_step, "validating",   validating);
			rep.saveStepAttribute(id_transformation, id_step, "usetoken",   usetoken);
			rep.saveStepAttribute(id_transformation, id_step, "IsIgnoreEmptyFile",   IsIgnoreEmptyFile);
			rep.saveStepAttribute(id_transformation, id_step, "doNotFailIfNoFile",   doNotFailIfNoFile);
			
			rep.saveStepAttribute(id_transformation, id_step, "rownum",          includeRowNumber);
			rep.saveStepAttribute(id_transformation, id_step, "rownum_field",    rowNumberField);
			rep.saveStepAttribute(id_transformation, id_step, "limit",           rowLimit);
            rep.saveStepAttribute(id_transformation, id_step, "loopxpath",       loopxpath);
            rep.saveStepAttribute(id_transformation, id_step, "encoding",        encoding);
			
			for (int i=0;i<fileName.length;i++)
			{
				rep.saveStepAttribute(id_transformation, id_step, i, "file_name",     fileName[i]);
				rep.saveStepAttribute(id_transformation, id_step, i, "file_mask",     fileMask[i]);
				rep.saveStepAttribute(id_transformation, id_step, i, "exclude_file_mask", excludeFileMask[i]);
				rep.saveStepAttribute(id_transformation, id_step, i, "file_required", fileRequired[i]);
				rep.saveStepAttribute(id_transformation, id_step, i, "include_subfolders", includeSubFolders[i]);
			}
			
			for (int i=0;i<inputFields.length;i++)
			{
			    GetXMLDataField field = inputFields[i];
			    
				rep.saveStepAttribute(id_transformation, id_step, i, "field_name",          field.getName());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_xpath",         field.getXPath());
				rep.saveStepAttribute(id_transformation, id_step, i, "element_type",        field.getElementTypeCode());
				rep.saveStepAttribute(id_transformation, id_step, i, "result_type",         field.getResultTypeCode());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_type",          field.getTypeDesc());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_format",        field.getFormat());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_currency",      field.getCurrencySymbol());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_decimal",       field.getDecimalSymbol());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_group",         field.getGroupSymbol());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_length",        field.getLength());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_precision",     field.getPrecision());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_trim_type",     field.getTrimTypeCode());
				rep.saveStepAttribute(id_transformation, id_step, i, "field_repeat",        field.isRepeated());
			}
			rep.saveStepAttribute(id_transformation, id_step, "IsInFields",       inFields);
			rep.saveStepAttribute(id_transformation, id_step, "IsAFile",       IsAFile);
			
            rep.saveStepAttribute(id_transformation, id_step, "XmlField",        xmlField);
            rep.saveStepAttribute(id_transformation, id_step, "prunePath",        prunePath);
			rep.saveStepAttribute(id_transformation, id_step, "shortFileFieldName", shortFileFieldName);
			rep.saveStepAttribute(id_transformation, id_step, "pathFieldName", pathFieldName);
			rep.saveStepAttribute(id_transformation, id_step, "hiddenFieldName", hiddenFieldName);
			rep.saveStepAttribute(id_transformation, id_step, "lastModificationTimeFieldName", lastModificationTimeFieldName);
			rep.saveStepAttribute(id_transformation, id_step, "uriNameFieldName", uriNameFieldName);
			rep.saveStepAttribute(id_transformation, id_step, "rootUriNameFieldName", rootUriNameFieldName);
			rep.saveStepAttribute(id_transformation, id_step, "extensionFieldName", extensionFieldName);
		}
		catch(Exception e)
		{
			throw new KettleException(BaseMessages.getString(PKG, "GetXMLDataMeta.Exception.ErrorSavingToRepository", ""+id_step), e);
		}
	}

	  public FileInputList getFiles(VariableSpace space)
	  {
	    return FileInputList.createFileList(space, fileName, fileMask, excludeFileMask, fileRequired, includeSubFolderBoolean());
	  }
	  private boolean[] includeSubFolderBoolean()
     {
    	int len=fileName.length;
		boolean includeSubFolderBoolean[]= new boolean[len];
		for(int i=0; i<len; i++)
		{
			includeSubFolderBoolean[i]=YES.equalsIgnoreCase(includeSubFolders[i]);
		}
		return includeSubFolderBoolean;
     }

	
	public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info, VariableSpace space, Repository repository, IMetaStore metaStore)
	{
		CheckResult cr;

		// See if we get input...		
		if (input.length<=0)
		{		
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "GetXMLDataMeta.CheckResult.NoInputExpected"), stepMeta);
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, BaseMessages.getString(PKG, "GetXMLDataMeta.CheckResult.NoInput"), stepMeta);
			remarks.add(cr);
		}
		
		//	control Xpath	
		if (getLoopXPath()== null || Const.isEmpty(getLoopXPath()))
		{		
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "GetXMLDataMeta.CheckResult.NoLoopXpath"), stepMeta);
			remarks.add(cr);
		}
		if(getInputFields().length<=0)
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "GetXMLDataMeta.CheckResult.NoInputField"), stepMeta);
			remarks.add(cr);
		}		
		
		if(isInFields())
		{
			 if (Const.isEmpty(getXMLField()))
			 {
				 cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "GetXMLDataMeta.CheckResult.NoField"), stepMeta);
				 remarks.add(cr); 
			 }
			 else
			 {
				 cr = new CheckResult(CheckResult.TYPE_RESULT_OK, BaseMessages.getString(PKG, "GetXMLDataMeta.CheckResult.FieldOk"), stepMeta);
				 remarks.add(cr); 
			 }		 
		}
		else
		{
	        FileInputList fileInputList = getFiles(transMeta);
			// String files[] = getFiles();
			if (fileInputList==null || fileInputList.getFiles().size()==0)
			{
				cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "GetXMLDataMeta.CheckResult.NoFiles"), stepMeta);
				remarks.add(cr);
			}
			else
			{
				cr = new CheckResult(CheckResult.TYPE_RESULT_OK, BaseMessages.getString(PKG, "GetXMLDataMeta.CheckResult.FilesOk", ""+fileInputList.getFiles().size()), stepMeta);
				remarks.add(cr);
			}	
		}	
	}
	
	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta tr, Trans trans)
	{
		return new GetXMLData(stepMeta, stepDataInterface, cnr, tr, trans);
	}

	public StepDataInterface getStepData()
	{
		return new GetXMLDataData();
	}

    public boolean supportsErrorHandling()
    {
        return true;
    }

	/**
	 * Since the exported transformation that runs this will reside in a ZIP file, we can't reference files relatively.
	 * So what this does is turn the name of files into absolute paths OR it simply includes the resource in the ZIP file.
	 * For now, we'll simply turn it into an absolute path and pray that the file is on a shared drive or something like that.
	 * 
   * @param space the variable space to use 
   * @param definitions
   * @param resourceNamingInterface
   * @param repository The repository to optionally load other resources from (to be converted to XML)
   * @param metaStore the metaStore in which non-kettle metadata could reside. 
   * 
   * @return the filename of the exported resource
   */
  public String exportResources(VariableSpace space, Map<String, ResourceDefinition> definitions, ResourceNamingInterface resourceNamingInterface, Repository repository, IMetaStore metaStore) throws KettleException {
		try {
			// The object that we're modifying here is a copy of the original!
			// So let's change the filename from relative to absolute by grabbing the file object...
			// In case the name of the file comes from previous steps, forget about this!
			//
			List<String> newFilenames = new ArrayList<String>();
			
			if (!isInFields()) {
				FileInputList fileList = getFiles(space);
				if (fileList.getFiles().size()>0) {
					for (FileObject fileObject : fileList.getFiles()) {
						// From : ${Internal.Transformation.Filename.Directory}/../foo/bar.xml
						// To   : /home/matt/test/files/foo/bar.xml
						//
						// If the file doesn't exist, forget about this effort too!
						//
						if (fileObject.exists()) {
							// Convert to an absolute path and add it to the list.
							// 
							newFilenames.add(fileObject.getName().getPath());
						}
					}
					
					// Still here: set a new list of absolute filenames!
					//
					fileName = newFilenames.toArray(new String[newFilenames.size()]);
					fileMask = new String[newFilenames.size()]; // all null since converted to absolute path.
					fileRequired = new String[newFilenames.size()]; // all null, turn to "Y" :
					for (int i=0;i<newFilenames.size();i++) fileRequired[i]="Y";
				}
			}
			return null;
		} catch (Exception e) {
			throw new KettleException(e); 
		}
	}

  @Override
  public StepMetaInjectionInterface getStepMetaInjectionInterface() {
    return new GetXMLDataMetaInjection(this);
  }

}