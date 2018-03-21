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

package org.pentaho.di.trans.steps.loadfileinput;

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
import org.pentaho.di.core.vfs.KettleVFS;
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
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;



public class LoadFileInputMeta extends BaseStepMeta implements StepMetaInterface
{	
	private static Class<?> PKG = LoadFileInputMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	public static final String[] RequiredFilesDesc = new String[] { BaseMessages.getString(PKG, "System.Combo.No"), BaseMessages.getString(PKG, "System.Combo.Yes") };
	public static final String[] RequiredFilesCode = new String[] {"N", "Y"};
	
	private static final String NO = "N";
	private static final String YES = "Y";
	
	/** Array of filenames */
	private  String  fileName[]; 

	/** Wildcard or filemask (regular expression) */
	private  String  fileMask[];
	
	
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

	/** The fields to import... */
	private LoadFileInputField inputFields[];
	
    /** The encoding to use for reading: null or empty string means system default encoding */
    private String encoding;
    
    /**  Dynamic FilenameField    */
    private  String DynamicFilenameField;
    
    /**  Is In fields     */
    private  boolean fileinfield;
    
    /** Flag: add result filename **/
    private boolean addresultfile;
    
	/** Array of boolean values as string, indicating if a file is required. */
	private  String  fileRequired[];
	
	/** Flag : do we ignore empty file? */
	private boolean IsIgnoreEmptyFile;
	
	/** Array of boolean values as string, indicating if we need to fetch sub folders. */
	private  String  includeSubFolders[];
	
	
    /** Additional fields  **/
    private String shortFileFieldName;
    private String pathFieldName;
    private String hiddenFieldName;
    private String lastModificationTimeFieldName;
    private String uriNameFieldName;
    private String rootUriNameFieldName;
    private String extensionFieldName;
	
	public LoadFileInputMeta()
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
	public String[] getFileRequired() {
		return fileRequired;
	}
    
	public void setFileRequired(String[] fileRequired) {
		this.fileRequired = fileRequired;
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
	 * @return the add result filesname flag
	 */
	public boolean addResultFile()
	{
		return addresultfile;
	}

	/** 
	 * @return the IsIgnoreEmptyFile flag
	 */
	public boolean isIgnoreEmptyFile()
	{
		return IsIgnoreEmptyFile;
	}
	
	/** 
	 * @param the IsIgnoreEmptyFile to set
	 */
	public void setIgnoreEmptyFile(boolean IsIgnoreEmptyFile)
	{
		this.IsIgnoreEmptyFile= IsIgnoreEmptyFile;
	}
	public void setAddResultFile(boolean addresultfile)
	{
		this.addresultfile=addresultfile;
	}
	
	/**
     * @return Returns the input fields.
     */
    public LoadFileInputField[] getInputFields()
    {
        return inputFields;
    }
    
    /**
     * @param inputFields The input fields to set.
     */
    public void setInputFields(LoadFileInputField[] inputFields)
    {
        this.inputFields = inputFields;
    }
    
    /************************************
     * get and set  FilenameField
    *************************************/
    /**  */
    public String getDynamicFilenameField()
    {
        return DynamicFilenameField;
    }
    
    /**  */ 
    public void setDynamicFilenameField(String DynamicFilenameField)
    {
        this.DynamicFilenameField = DynamicFilenameField;
    }
    
    /************************************
     * get et set  IsInFields
    *************************************/
    /**  */
    public boolean getIsInFields()
    {
        return fileinfield;
    }
    
    /**  */ 
    public void setIsInFields(boolean IsInFields)
    {
        this.fileinfield = IsInFields;
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
    
    /**
     * @return Returns the fileName.
     */
    public String[] getFileName()
    {
        return fileName;
    }
	public String[] getIncludeSubFolders() {
		return includeSubFolders;
	}

	public void setIncludeSubFolders(String[] includeSubFoldersin) {
		for (int i=0;i<includeSubFoldersin.length;i++)
		{
			this.includeSubFolders[i] = getRequiredFilesCode(includeSubFoldersin[i]);
		}
	}
	 public String getRequiredFilesCode(String tt)
	    {
	   	if(tt==null) return RequiredFilesCode[0]; 
			if(tt.equals(RequiredFilesDesc[1]))
				return RequiredFilesCode[1];
			else
				return RequiredFilesCode[0]; 
	    }
	  public String getRequiredFilesDesc(String tt)
	  {
	 	if(tt==null) return RequiredFilesDesc[0]; 
			if(tt.equals(RequiredFilesCode[1]))
				return RequiredFilesDesc[1];
			else
				return RequiredFilesDesc[0]; 
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

    
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore)
	throws KettleXMLException
	{
    	readData(stepnode);
	}

	public Object clone()
	{
		LoadFileInputMeta retval = (LoadFileInputMeta)super.clone();
		
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
                retval.inputFields[i] = (LoadFileInputField)inputFields[i].clone();
            }
		}		
		return retval;
	}

    
    public String getXML()
    {
        StringBuffer retval=new StringBuffer();
        
        retval.append("    "+XMLHandler.addTagValue("include",         includeFilename));
        retval.append("    "+XMLHandler.addTagValue("include_field",   filenameField));
        retval.append("    "+XMLHandler.addTagValue("rownum",          includeRowNumber));
        retval.append("    "+XMLHandler.addTagValue("addresultfile",   addresultfile));
        retval.append("    "+XMLHandler.addTagValue("IsIgnoreEmptyFile",   IsIgnoreEmptyFile));
        
        retval.append("    "+XMLHandler.addTagValue("rownum_field",    rowNumberField));
        retval.append("    "+XMLHandler.addTagValue("encoding",        encoding));
        
        retval.append("    <file>"+Const.CR);
        for (int i=0;i<fileName.length;i++)
        {
            retval.append("      "+XMLHandler.addTagValue("name",     fileName[i]));
            retval.append("      "+XMLHandler.addTagValue("filemask", fileMask[i]));
			retval.append("      ").append(XMLHandler.addTagValue("exclude_filemask", excludeFileMask[i]));
            retval.append("      ").append(XMLHandler.addTagValue("file_required", fileRequired[i]));
        	retval.append("      "+XMLHandler.addTagValue("include_subfolders", includeSubFolders[i]));
        }
        retval.append("      </file>"+Const.CR);
        
        retval.append("    <fields>"+Const.CR);
        for (int i=0;i<inputFields.length;i++)
        {
            LoadFileInputField field = inputFields[i];
            retval.append(field.getXML());
        }
        retval.append("      </fields>"+Const.CR);
        retval.append("    "+XMLHandler.addTagValue("limit", rowLimit));
        retval.append("    "+XMLHandler.addTagValue("IsInFields", fileinfield));
        retval.append("    "+XMLHandler.addTagValue("DynamicFilenameField", DynamicFilenameField));
		retval.append("    ").append(XMLHandler.addTagValue("shortFileFieldName", shortFileFieldName));
		retval.append("    ").append(XMLHandler.addTagValue("pathFieldName", pathFieldName));
		retval.append("    ").append(XMLHandler.addTagValue("hiddenFieldName", hiddenFieldName));
		retval.append("    ").append(XMLHandler.addTagValue("lastModificationTimeFieldName", lastModificationTimeFieldName));
		retval.append("    ").append(XMLHandler.addTagValue("uriNameFieldName", uriNameFieldName));
		retval.append("    ").append(XMLHandler.addTagValue("rootUriNameFieldName", rootUriNameFieldName));
		retval.append("    ").append(XMLHandler.addTagValue("extensionFieldName", extensionFieldName));
		
        return retval.toString();
    }

	private void readData(Node stepnode) throws KettleXMLException
	{
		try
		{
			includeFilename   = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "include"));
			filenameField     = XMLHandler.getTagValue(stepnode, "include_field");
			
			addresultfile  = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "addresultfile"));
			IsIgnoreEmptyFile  = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "IsIgnoreEmptyFile"));
			
			includeRowNumber  = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "rownum"));
			rowNumberField    = XMLHandler.getTagValue(stepnode, "rownum_field");
			encoding          = XMLHandler.getTagValue(stepnode, "encoding");
	
			Node filenode  = XMLHandler.getSubNode(stepnode,   "file");
			Node fields    = XMLHandler.getSubNode(stepnode,   "fields");
			int nrFiles     = XMLHandler.countNodes(filenode,  "name");
			int nrFields    = XMLHandler.countNodes(fields,    "field");
	
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
				LoadFileInputField field = new LoadFileInputField(fnode);
				inputFields[i] = field;
			} 
			
			// Is there a limit on the number of rows we process?
			rowLimit = Const.toLong(XMLHandler.getTagValue(stepnode, "limit"), 0L);
			
			fileinfield = "Y".equalsIgnoreCase(XMLHandler.getTagValue(stepnode, "IsInFields"));

			DynamicFilenameField = XMLHandler.getTagValue(stepnode, "DynamicFilenameField");
			shortFileFieldName = XMLHandler.getTagValue(stepnode, "shortFileFieldName");
			pathFieldName = XMLHandler.getTagValue(stepnode, "pathFieldName");
			hiddenFieldName = XMLHandler.getTagValue(stepnode, "hiddenFieldName");
			lastModificationTimeFieldName = XMLHandler.getTagValue(stepnode, "lastModificationTimeFieldName");
			uriNameFieldName = XMLHandler.getTagValue(stepnode, "uriNameFieldName");
			rootUriNameFieldName = XMLHandler.getTagValue(stepnode, "rootUriNameFieldName");
			extensionFieldName = XMLHandler.getTagValue(stepnode, "extensionFieldName");
			
		}
		catch(Exception e)
		{
			throw new KettleXMLException(BaseMessages.getString(PKG, "LoadFileInputMeta.Exception.ErrorLoadingXML", e.toString()));
		}
	}
	
	public void allocate(int nrfiles, int nrfields)
	{
		fileName   = new String [nrfiles];
		fileMask   = new String [nrfiles];
		excludeFileMask = new String[nrfiles];
		fileRequired = new String[nrfiles];
		includeSubFolders = new String[nrfiles];
		inputFields = new LoadFileInputField[nrfields];
        
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

		encoding= "";
		IsIgnoreEmptyFile=false;
		includeFilename    = false;
		filenameField = "";
		includeRowNumber    = false;
		rowNumberField = "";
		addresultfile=true;
		
		int nrFiles=0;
		int nrFields=0;


		allocate(nrFiles, nrFields);	
		
		for (int i=0;i<nrFiles;i++) 
		{
			fileName[i]="filename"+(i+1);
			fileMask[i]="";
			excludeFileMask[i]="";
			fileRequired[i] = RequiredFilesCode[0];
			includeSubFolders[i] = RequiredFilesCode[0];
		}
		
		for (int i=0;i<nrFields;i++)
		{
		    inputFields[i] = new LoadFileInputField("field"+(i+1));
		}

		rowLimit=0;
		
		fileinfield    = false;
		DynamicFilenameField = null;
	}
	
	public void getFields(RowMetaInterface r, String name, RowMetaInterface info[], StepMeta nextStep, VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException
	{
		if(!getIsInFields()) r.clear();
		int i;
		for (i=0;i<inputFields.length;i++)
		{
		    LoadFileInputField field = inputFields[i];
		    int type=field.getType();
		    
			switch (field.getElementType())
			{
				case LoadFileInputField.ELEMENT_TYPE_FILECONTENT:
					if (type==ValueMeta.TYPE_NONE) type=ValueMeta.TYPE_STRING;
					break;
				case LoadFileInputField.ELEMENT_TYPE_FILESIZE:
					if (type==ValueMeta.TYPE_NONE) type=ValueMeta.TYPE_INTEGER;
					break;
				default:
					break;
			}
		    
			try {
  			ValueMetaInterface v=ValueMetaFactory.createValueMeta(space.environmentSubstitute(field.getName()), type);
  			v.setLength(field.getLength());
  			v.setPrecision(field.getPrecision());
  			v.setConversionMask(field.getFormat());
  			v.setCurrencySymbol(field.getCurrencySymbol());
  			v.setDecimalSymbol(field.getDecimalSymbol());
  			v.setGroupingSymbol(field.getGroupSymbol());
  			v.setTrimType(field.getTrimType());
  			v.setOrigin(name);
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
	
	public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases)
	throws KettleException
	{
		try
		{
			includeFilename   =      rep.getStepAttributeBoolean(id_step, "include");  
			filenameField     =      rep.getStepAttributeString (id_step, "include_field");
			
			addresultfile  =      rep.getStepAttributeBoolean(id_step, "addresultfile");
			IsIgnoreEmptyFile  =      rep.getStepAttributeBoolean(id_step, "IsIgnoreEmptyFile");
			
			includeRowNumber  =      rep.getStepAttributeBoolean(id_step, "rownum");
			rowNumberField    =      rep.getStepAttributeString (id_step, "rownum_field");
			rowLimit          =      rep.getStepAttributeInteger(id_step, "limit");
			encoding          =      rep.getStepAttributeString (id_step, "encoding");
	
			int nrFiles     = rep.countNrStepAttributes(id_step, "file_name");
			int nrFields    = rep.countNrStepAttributes(id_step, "field_name");
            
			allocate(nrFiles, nrFields);

			for (int i=0;i<nrFiles;i++)
			{
				fileName[i] =      rep.getStepAttributeString (id_step, i, "file_name"    );
				fileMask[i] =      rep.getStepAttributeString (id_step, i, "file_mask"    );
				excludeFileMask[i] =      rep.getStepAttributeString (id_step, i, "exclude_file_mask"    );
				fileRequired[i] = rep.getStepAttributeString(id_step, i, "file_required");
                if(!YES.equalsIgnoreCase(fileRequired[i]))    	fileRequired[i] = NO;
                includeSubFolders[i] = rep.getStepAttributeString(id_step, i, "include_subfolders");
                if(!YES.equalsIgnoreCase(includeSubFolders[i])) includeSubFolders[i] = NO;
			}

			for (int i=0;i<nrFields;i++)
			{
			    LoadFileInputField field = new LoadFileInputField();
			    
				field.setName( rep.getStepAttributeString (id_step, i, "field_name") );
				field.setElementType( LoadFileInputField.getElementTypeByCode( rep.getStepAttributeString (id_step, i, "element_type") ));
				field.setType( ValueMeta.getType( rep.getStepAttributeString (id_step, i, "field_type") ) );
				field.setFormat( rep.getStepAttributeString (id_step, i, "field_format") );
				field.setCurrencySymbol( rep.getStepAttributeString (id_step, i, "field_currency") );
				field.setDecimalSymbol( rep.getStepAttributeString (id_step, i, "field_decimal") );
				field.setGroupSymbol( rep.getStepAttributeString (id_step, i, "field_group") );
				field.setLength( (int)rep.getStepAttributeInteger(id_step, i, "field_length") );
				field.setPrecision( (int)rep.getStepAttributeInteger(id_step, i, "field_precision") );
				field.setTrimType( LoadFileInputField.getTrimTypeByCode( rep.getStepAttributeString (id_step, i, "field_trim_type") ));
				field.setRepeated( rep.getStepAttributeBoolean(id_step, i, "field_repeat") );

                
				inputFields[i] = field;
			}
			fileinfield        =      rep.getStepAttributeBoolean (id_step, "IsInFields");
			
			DynamicFilenameField          =      rep.getStepAttributeString (id_step, "DynamicFilenameField");
			DynamicFilenameField          =      rep.getStepAttributeString (id_step, "DynamicFilenameField");
			shortFileFieldName = rep.getStepAttributeString(id_step, "shortFileFieldName");
			pathFieldName = rep.getStepAttributeString(id_step, "pathFieldName");
			hiddenFieldName = rep.getStepAttributeString(id_step, "hiddenFieldName");
			lastModificationTimeFieldName = rep.getStepAttributeString(id_step, "lastModificationTimeFieldName");
			rootUriNameFieldName = rep.getStepAttributeString(id_step, "rootUriNameFieldName");
			extensionFieldName = rep.getStepAttributeString(id_step, "extensionFieldName");
  

		}
		catch(Exception e)
		{
			throw new KettleException(BaseMessages.getString(PKG, "LoadFileInputMeta.Exception.ErrorReadingRepository"), e);
		}
	}
	
	public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step)
	throws KettleException
	{
		try
		{
			rep.saveStepAttribute(id_transformation, id_step, "include",         includeFilename);
			rep.saveStepAttribute(id_transformation, id_step, "include_field",   filenameField);
			rep.saveStepAttribute(id_transformation, id_step, "addresultfile",   addresultfile);
			rep.saveStepAttribute(id_transformation, id_step, "IsIgnoreEmptyFile" ,   IsIgnoreEmptyFile);
			
			rep.saveStepAttribute(id_transformation, id_step, "rownum",          includeRowNumber);
			rep.saveStepAttribute(id_transformation, id_step, "rownum_field",    rowNumberField);
			rep.saveStepAttribute(id_transformation, id_step, "limit",           rowLimit);
            rep.saveStepAttribute(id_transformation, id_step, "encoding",        encoding);
			
			for (int i=0;i<fileName.length;i++)
			{
				rep.saveStepAttribute(id_transformation, id_step, i, "file_name",     fileName[i]);
				rep.saveStepAttribute(id_transformation, id_step, i, "file_mask",     fileMask[i]);
				rep.saveStepAttribute(id_transformation, id_step, i, "excludefile_mask",     excludeFileMask[i]);
				rep.saveStepAttribute(id_transformation, id_step, i, "file_required", fileRequired[i]);
				rep.saveStepAttribute(id_transformation, id_step, i, "include_subfolders", includeSubFolders[i]);
			}
			
			for (int i=0;i<inputFields.length;i++)
			{
			    LoadFileInputField field = inputFields[i];
			    
				rep.saveStepAttribute(id_transformation, id_step, i, "field_name",          field.getName());
				rep.saveStepAttribute(id_transformation, id_step, i, "element_type",        field.getElementTypeCode());
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
			rep.saveStepAttribute(id_transformation, id_step, "IsInFields",       fileinfield);
			
            rep.saveStepAttribute(id_transformation, id_step, "DynamicFilenameField",        DynamicFilenameField);
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
			throw new KettleException(BaseMessages.getString(PKG, "LoadFileInputMeta.Exception.ErrorSavingToRepository", ""+id_step), e);
		}
	}
	
  public FileInputList  getFiles(VariableSpace space)
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
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "LoadFileInputMeta.CheckResult.NoInputExpected"), stepMeta);
			remarks.add(cr);
		}
		else
		{
			cr = new CheckResult(CheckResult.TYPE_RESULT_OK, BaseMessages.getString(PKG, "LoadFileInputMeta.CheckResult.NoInput"), stepMeta);
			remarks.add(cr);
		}	
		
		if(getIsInFields())
		{
			 if (Const.isEmpty(getDynamicFilenameField()))
			 {
				 cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "LoadFileInputMeta.CheckResult.NoField"), stepMeta);
				 remarks.add(cr); 
			 }
			 else
			 {
				 cr = new CheckResult(CheckResult.TYPE_RESULT_OK, BaseMessages.getString(PKG, "LoadFileInputMeta.CheckResult.FieldOk"), stepMeta);
				 remarks.add(cr); 
			 }		 
		}
		else
		{
	        FileInputList fileInputList = getFiles(transMeta);

			if (fileInputList==null || fileInputList.getFiles().size()==0)
			{
				cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "LoadFileInputMeta.CheckResult.NoFiles"), stepMeta);
				remarks.add(cr);
			}
			else
			{
				cr = new CheckResult(CheckResult.TYPE_RESULT_OK, BaseMessages.getString(PKG, "LoadFileInputMeta.CheckResult.FilesOk", ""+fileInputList.getFiles().size()), stepMeta);
				remarks.add(cr);
			}	
		}	
		
		
	}
	
  /**
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
        //
        if (!fileinfield) {
            for (int i=0;i<fileName.length;i++) {
              FileObject fileObject = KettleVFS.getFileObject(space.environmentSubstitute(fileName[i]), space);
              fileName[i] = resourceNamingInterface.nameResource(fileObject, space, Const.isEmpty(fileMask[i]));
            }
        }
        return null;
      } catch (Exception e) {
          throw new KettleException(e); 
      }
    }


	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans trans)
	{
		return new LoadFileInput(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}

	public StepDataInterface getStepData()
	{
		return new LoadFileInputData();
	}
    public boolean supportsErrorHandling()
    {
        return true;
    }
}
