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

package org.pentaho.di.trans.steps.replacestring;

import java.util.List;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.shared.SharedObjectInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;


public class ReplaceStringMeta extends BaseStepMeta implements StepMetaInterface {

	private static Class<?> PKG = ReplaceStringMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private String fieldInStream[];
	
	private String fieldOutStream[];

	private int[] useRegEx;
	
	private String replaceString[];
	
	private String replaceByString[];
	
    /** Flag : set empty string **/
    private boolean setEmptyString[];
	
	private String replaceFieldByString[];
	
	private int[] wholeWord;
	
	private int[] caseSensitive;


	public final static String caseSensitiveCode[] = { "no", "yes"};
	
	public static final String[] caseSensitiveDesc = new String[] { 
			BaseMessages.getString(PKG, "System.Combo.No"), 
			BaseMessages.getString(PKG, "System.Combo.Yes") };
	
	public final static int CASE_SENSITIVE_NO = 0;

	public final static int CASE_SENSITIVE_YES = 1;
	
	public static final String[] wholeWordDesc = new String[] { 
		BaseMessages.getString(PKG, "System.Combo.No"), 
		BaseMessages.getString(PKG, "System.Combo.Yes") };

	public final static String wholeWordCode[] = { "no", "yes"};

	public final static int WHOLE_WORD_NO = 0;

	public final static int WHOLE_WORD_YES = 1;

	
	public static final String[] useRegExDesc = new String[] { 
		BaseMessages.getString(PKG, "System.Combo.No"), 
		BaseMessages.getString(PKG, "System.Combo.Yes") };

	public final static String useRegExCode[] = { "no", "yes"};

	public final static int USE_REGEX_NO = 0;

	public final static int USE_REGEX_YES = 1;
	

	public ReplaceStringMeta() {
		super(); // allocate BaseStepMeta
	}

	/**
	 * @return Returns the fieldInStream.
	 */
	public String[] getFieldInStream() {
		return fieldInStream;
	}

	/**
	 * @param fieldInStream
	 *            The fieldInStream to set.
	 */
	public void setFieldInStream(String[] keyStream) {
		this.fieldInStream = keyStream;
	}
	public int[] getCaseSensitive() {
		return caseSensitive;
	}
	public int[] getWholeWord() {
		return wholeWord;
	}
	
	public int[] getUseRegEx() {
		return useRegEx;
	}
	/**
	 * @return the setEmptyString
	 */
	public boolean[] isSetEmptyString() {
		return setEmptyString;
	}

	/**
	 * @param setEmptyString the setEmptyString to set
	 */
	public void setEmptyString(boolean[] setEmptyString) {
		this.setEmptyString = setEmptyString;
	}

	/**
	 * @return Returns the fieldOutStream.
	 */
	public String[] getFieldOutStream() {
		return fieldOutStream;
	}
	/**
	 * @param keyStream  The fieldOutStream to set.
	 */
	public void setFieldOutStream(String[] keyStream) {
		this.fieldOutStream = keyStream;
	}
	
	public String[] getReplaceString() {
		return replaceString;
	}
	
	public void setReplaceString(String[] replaceString) {
		this.replaceString = replaceString;
	}
	public String[] getReplaceByString() {
		return replaceByString;
	}
	
	public String[] getFieldReplaceByString() {
		return replaceFieldByString;
	}

	
	public void setCaseSensitive(int[] caseSensitive) {
		this.caseSensitive = caseSensitive;
	}
	
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore)
    throws KettleXMLException
    {
    	readData(stepnode, databases);
	}
	public void allocate(int nrkeys) {
		fieldInStream = new String[nrkeys];
		fieldOutStream = new String[nrkeys];
		useRegEx= new int[nrkeys];
		replaceString = new String[nrkeys];
		replaceByString = new String[nrkeys];
	    setEmptyString = new boolean[nrkeys];
		replaceFieldByString = new String[nrkeys];
		wholeWord= new int[nrkeys];
		caseSensitive= new int[nrkeys];
	}

	public Object clone() {
		ReplaceStringMeta retval = (ReplaceStringMeta) super.clone();
		int nrkeys = fieldInStream.length;

		retval.allocate(nrkeys);

		for (int i = 0; i < nrkeys; i++) {
			retval.fieldInStream[i] = fieldInStream[i];
			retval.fieldOutStream[i] = fieldOutStream[i];
			retval.useRegEx[i] = useRegEx[i];
			retval.replaceString[i] = replaceString[i];
			retval.replaceByString[i] = replaceByString[i];
		    retval.setEmptyString[i]=setEmptyString[i];
			retval.replaceFieldByString[i] = replaceFieldByString[i];
			retval.wholeWord[i] = wholeWord[i];
			retval.caseSensitive[i] = caseSensitive[i];
		}

		return retval;
	}

	 private void readData(Node stepnode, List<? extends SharedObjectInterface> databases) throws KettleXMLException
	 {
		try
		{
			int nrkeys;

			Node lookup = XMLHandler.getSubNode(stepnode, "fields"); 
			nrkeys = XMLHandler.countNodes(lookup, "field"); 

			allocate(nrkeys);

			for (int i = 0; i < nrkeys; i++) {
				Node fnode = XMLHandler.getSubNodeByNr(lookup, "field", i); 

				fieldInStream[i] = Const.NVL(XMLHandler.getTagValue(fnode,"in_stream_name"), ""); 
				fieldOutStream[i] = Const.NVL(XMLHandler.getTagValue(fnode,"out_stream_name"), ""); 
				useRegEx[i] = getCaseSensitiveByCode(Const.NVL(XMLHandler.getTagValue(fnode,"use_regex"), ""));
				replaceString[i] = Const.NVL(XMLHandler.getTagValue(fnode, "replace_string"), ""); 
				replaceByString[i] = Const.NVL(XMLHandler.getTagValue(fnode, "replace_by_string"), ""); 
				String emptyString = XMLHandler.getTagValue(fnode, "set_empty_string");
	            
	            setEmptyString[i] = !Const.isEmpty(emptyString) && "Y".equalsIgnoreCase(emptyString);
				replaceFieldByString[i] = Const.NVL(XMLHandler.getTagValue(fnode, "replace_field_by_string"), ""); 
				wholeWord[i] = getWholeWordByCode(Const.NVL(XMLHandler.getTagValue(fnode,"whole_word"), ""));
				caseSensitive[i] = getCaseSensitiveByCode(Const.NVL(XMLHandler.getTagValue(fnode,"case_sensitive"), ""));
				
			}
		} catch (Exception e) {
			throw new KettleXMLException(
					BaseMessages.getString(PKG, "ReplaceStringMeta.Exception.UnableToReadStepInfoFromXML"), e); 
		}
	}

	public void setDefault() {
		fieldInStream = null;
		fieldOutStream = null;
		int nrkeys = 0;

		allocate(nrkeys);
	}

	public String getXML() {
		StringBuffer retval = new StringBuffer(500);

		retval.append("    <fields>").append(Const.CR); 

		for (int i = 0; i < fieldInStream.length; i++) {
			retval.append("      <field>").append(Const.CR); 
			retval.append("        ").append(XMLHandler.addTagValue("in_stream_name", fieldInStream[i]));  
			retval.append("        ").append(XMLHandler.addTagValue("out_stream_name", fieldOutStream[i])); 
			retval.append("        ").append(XMLHandler.addTagValue("use_regex",getUseRegExCode(useRegEx[i])));
			retval.append("        ").append(XMLHandler.addTagValue("replace_string", replaceString[i]));  
			retval.append("        ").append(XMLHandler.addTagValue("replace_by_string", replaceByString[i])); 
		    retval.append("        ").append(XMLHandler.addTagValue("set_empty_string", setEmptyString[i]));
			retval.append("        ").append(XMLHandler.addTagValue("replace_field_by_string", replaceFieldByString[i])); 
			retval.append("        ").append(XMLHandler.addTagValue("whole_word",getWholeWordCode(wholeWord[i])));
			retval.append("        ").append(XMLHandler.addTagValue("case_sensitive",getCaseSensitiveCode(caseSensitive[i])));
			retval.append("      </field>").append(Const.CR); 
		}

		retval.append("    </fields>").append(Const.CR); 

		return retval.toString();
	}

	 public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases)
     throws KettleException
     {
		try {
			int nrkeys = rep.countNrStepAttributes(id_step, "in_stream_name"); 

			allocate(nrkeys);
			for (int i = 0; i < nrkeys; i++) {
				fieldInStream[i] = Const.NVL(rep.getStepAttributeString(id_step, i,	"in_stream_name"), ""); 
				fieldOutStream[i] = Const.NVL(rep.getStepAttributeString(id_step, i,	"out_stream_name"), ""); 
				useRegEx[i] = getCaseSensitiveByCode(Const.NVL(rep.getStepAttributeString(id_step, i, "use_regex"), ""));
				replaceString[i] = Const.NVL(rep.getStepAttributeString(id_step, i,	"replace_string"), "");
				replaceByString[i] = Const.NVL(rep.getStepAttributeString(id_step, i,	"replace_by_string"), "");
			    setEmptyString[i] = rep.getStepAttributeBoolean(id_step, i, "set_empty_string", false);
				replaceFieldByString[i] = Const.NVL(rep.getStepAttributeString(id_step, i,	"replace_field_by_string"), "");
				wholeWord[i] = getWholeWordByCode(Const.NVL(rep.getStepAttributeString(id_step, i, "whole_world"), ""));
				caseSensitive[i] = getCaseSensitiveByCode(Const.NVL(rep.getStepAttributeString(id_step, i, "case_sensitive"), ""));
				
			}
		} catch (Exception e) {
			throw new KettleException(
					BaseMessages.getString(PKG, "ReplaceStringMeta.Exception.UnexpectedErrorInReadingStepInfo"), e); 
		}
	}

	public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step)
			throws KettleException {
		try {
			for (int i = 0; i < fieldInStream.length; i++) {
				rep.saveStepAttribute(id_transformation, id_step, i,"in_stream_name", fieldInStream[i]); 
				rep.saveStepAttribute(id_transformation, id_step, i,"out_stream_name", fieldOutStream[i]);
				rep.saveStepAttribute(id_transformation, id_step, i,"use_regex", getUseRegExCode(useRegEx[i]));
				rep.saveStepAttribute(id_transformation, id_step, i,"replace_string", replaceString[i]); 
				rep.saveStepAttribute(id_transformation, id_step, i,"replace_by_string", replaceByString[i]); 
			    rep.saveStepAttribute(id_transformation, id_step, i, "set_empty_string", setEmptyString[i]);
				rep.saveStepAttribute(id_transformation, id_step, i,"replace_field_by_string", replaceFieldByString[i]); 
				rep.saveStepAttribute(id_transformation, id_step, i,"whole_world", getWholeWordCode(wholeWord[i]));
				rep.saveStepAttribute(id_transformation, id_step, i,"case_sensitive", getCaseSensitiveCode(caseSensitive[i]));
				
			}
		} catch (Exception e) {
			throw new KettleException(BaseMessages.getString(PKG, "ReplaceStringMeta.Exception.UnableToSaveStepInfo") + id_step, e); 
		}
	}
	 public void getFields(RowMetaInterface inputRowMeta, String name, RowMetaInterface info[], StepMeta nextStep,
	            VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException
	  {
		 int nrFields=fieldInStream==null?0:fieldInStream.length;
		 for(int i=0;i<nrFields;i++) {
			String fieldName=space.environmentSubstitute(fieldOutStream[i]);

			if (!Const.isEmpty(fieldOutStream[i])){
				// We have a new field
				ValueMetaInterface valueMeta = new ValueMeta(fieldName, ValueMeta.TYPE_STRING);
				valueMeta.setOrigin(name);
				inputRowMeta.addValueMeta(valueMeta);
			}
		}
	}
	 public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepinfo,
	            RowMetaInterface prev, String input[], String output[], RowMetaInterface info, 
	            VariableSpace space, Repository repository, IMetaStore metaStore)
	  {

		CheckResult cr;
		String error_message = ""; 
		boolean first = true;
		boolean error_found = false;

		if (prev == null) {
			error_message += BaseMessages.getString(PKG, "ReplaceStringMeta.CheckResult.NoInputReceived") + Const.CR; 
			cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR,
					error_message, stepinfo);
			remarks.add(cr);
		} else {

			for (int i = 0; i < fieldInStream.length; i++) {
				String field = fieldInStream[i];

				ValueMetaInterface v = prev.searchValueMeta(field);
				if (v == null) {
					if (first) {
						first = false;
						error_message += BaseMessages.getString(PKG, "ReplaceStringMeta.CheckResult.MissingInStreamFields") + Const.CR; 
					}
					error_found = true;
					error_message += "\t\t" + field + Const.CR; 
				}
			}
			if (error_found) {
				cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR,
						error_message, stepinfo);
			} else {
				cr = new CheckResult(
						CheckResult.TYPE_RESULT_OK,
						BaseMessages.getString(PKG, "ReplaceStringMeta.CheckResult.FoundInStreamFields"), stepinfo); 
			}
			remarks.add(cr);

			// Check whether all are strings
			first = true;
			error_found = false;
			for (int i = 0; i < fieldInStream.length; i++) {
				String field = fieldInStream[i];

				ValueMetaInterface v = prev.searchValueMeta(field);
				if (v != null) {
					if (v.getType() != ValueMeta.TYPE_STRING) {
						if (first) {
							first = false;
							error_message += BaseMessages.getString(PKG, "ReplaceStringMeta.CheckResult.OperationOnNonStringFields") + Const.CR; 
						}
						error_found = true;
						error_message += "\t\t" + field + Const.CR; 
					}
				}
			}
			if (error_found) {
				cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR,
						error_message, stepinfo);
			} else {
				cr = new CheckResult(
						CheckResult.TYPE_RESULT_OK,BaseMessages.getString(PKG, "ReplaceStringMeta.CheckResult.AllOperationsOnStringFields"), stepinfo); 
			}
			remarks.add(cr);

			if (fieldInStream.length>0) {
				for (int idx = 0; idx < fieldInStream.length; idx++) {
					if (Const.isEmpty(fieldInStream[idx])) {
						cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR,BaseMessages.getString(PKG, "ReplaceStringMeta.CheckResult.InStreamFieldMissing", new Integer(idx + 1).toString()), stepinfo); 
						remarks.add(cr);
					
					}
				}
			}

			// Check if all input fields are distinct.
			for (int idx = 0; idx < fieldInStream.length; idx++) {
				for (int jdx = 0; jdx < fieldInStream.length; jdx++) {
					if (fieldInStream[idx].equals(fieldInStream[jdx])
							&& idx != jdx && idx < jdx) {
						error_message = BaseMessages.getString(PKG, "ReplaceStringMeta.CheckResult.FieldInputError", fieldInStream[idx]); 
						cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR,	error_message, stepinfo);
						remarks.add(cr);
					}
				}
			}

		}
	}

	
	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, 
			int cnr, TransMeta transMeta, Trans trans)
	{
		return new ReplaceString(stepMeta, stepDataInterface, cnr, transMeta, trans);
	}

	public StepDataInterface getStepData() {
		return new ReplaceStringData();
	}

	public boolean supportsErrorHandling() {
		return true;
	}

	private static String getCaseSensitiveCode(int i) {
		if (i < 0 || i >= caseSensitiveCode.length)
			return caseSensitiveCode[0];
		return caseSensitiveCode[i];
	}
	private static String getWholeWordCode(int i) {
		if (i < 0 || i >= wholeWordCode.length)
			return wholeWordCode[0];
		return wholeWordCode[i];
	}
	private static String getUseRegExCode(int i) {
		if (i < 0 || i >= useRegExCode.length)
			return useRegExCode[0];
		return useRegExCode[i];
	}
	public static String getCaseSensitiveDesc(int i) {
		if (i < 0 || i >= caseSensitiveDesc.length)
			return caseSensitiveDesc[0];
		return caseSensitiveDesc[i];
	}
	public static String getWholeWordDesc(int i) {
		if (i < 0 || i >= wholeWordDesc.length)
			return wholeWordDesc[0];
		return wholeWordDesc[i];
	}
	
	public static String getUseRegExDesc(int i) {
		if (i < 0 || i >= useRegExDesc.length)
			return useRegExDesc[0];
		return useRegExDesc[i];
	}
	
	
	private static int getCaseSensitiveByCode(String tt) {
		if (tt == null)
			return 0;

		for (int i = 0; i < caseSensitiveCode.length; i++) {
			if (caseSensitiveCode[i].equalsIgnoreCase(tt))
				return i;
		}
		return 0;
	}
	private static int getWholeWordByCode(String tt) {
		if (tt == null)
			return 0;

		for (int i = 0; i < wholeWordCode.length; i++) {
			if (wholeWordCode[i].equalsIgnoreCase(tt))
				return i;
		}
		return 0;
	}
	
	private static int getRegExByCode(String tt) {
		if (tt == null)
			return 0;

		for (int i = 0; i < useRegExCode.length; i++) {
			if (useRegExCode[i].equalsIgnoreCase(tt))
				return i;
		}
		return 0;
	}
	
	
	public static int getCaseSensitiveByDesc(String tt) {
		if (tt == null)
			return 0;

		for (int i = 0; i < caseSensitiveDesc.length; i++) {
			if (caseSensitiveDesc[i].equalsIgnoreCase(tt))
				return i;
		}

		// If this fails, try to match using the code.
		return getCaseSensitiveByCode(tt);
	}
	
	public static int getWholeWordByDesc(String tt) {
		if (tt == null)
			return 0;

		for (int i = 0; i < wholeWordDesc.length; i++) {
			if (wholeWordDesc[i].equalsIgnoreCase(tt))
				return i;
		}

		// If this fails, try to match using the code.
		return getWholeWordByCode(tt);
	}
	public static int getUseRegExByDesc(String tt) {
		if (tt == null)
			return 0;

		for (int i = 0; i < useRegExDesc.length; i++) {
			if (useRegExDesc[i].equalsIgnoreCase(tt))
				return i;
		}

		// If this fails, try to match using the code.
		return getRegExByCode(tt);
	}
	
}