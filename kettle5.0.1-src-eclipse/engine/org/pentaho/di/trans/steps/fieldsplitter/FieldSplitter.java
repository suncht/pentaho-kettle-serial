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

package org.pentaho.di.trans.steps.fieldsplitter;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;


/**
 * Split a single String fields into multiple parts based on certain conditions.
 * 
 * @author Matt
 * @since 31-Okt-2003
 * @author Daniel Einspanjer
 * @since 15-01-2008
 */
public class FieldSplitter extends BaseStep implements StepInterface
{
	private static Class<?> PKG = FieldSplitterMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private FieldSplitterMeta meta;
	private FieldSplitterData data;
	
	public FieldSplitter(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	private Object[] splitField(Object[] r) throws KettleException
	{
		if (first)
		{
			first=false;
			// get the RowMeta
			data.previousMeta = getInputRowMeta().clone();
			
			// search field
			data.fieldnr=data.previousMeta.indexOfValue(meta.getSplitField());
			if (data.fieldnr<0)
			{
				throw new KettleValueException(BaseMessages.getString(PKG, "FieldSplitter.Log.CouldNotFindFieldToSplit",meta.getSplitField()));  
			}

			// only String type allowed
			if (!data.previousMeta.getValueMeta(data.fieldnr).isString())
			{
				throw new KettleValueException((BaseMessages.getString(PKG, "FieldSplitter.Log.SplitFieldNotValid",meta.getSplitField())));  
			}

			// prepare the outputMeta
			//
			data.outputMeta= getInputRowMeta().clone();
			meta.getFields(data.outputMeta, getStepname(), null, null, this, repository, metaStore);
			
			// Now create objects to do string to data type conversion...
			//
			data.conversionMeta = data.outputMeta.cloneToType(ValueMetaInterface.TYPE_STRING);
			
			data.delimiter = environmentSubstitute(meta.getDelimiter());
			data.enclosure = environmentSubstitute(meta.getEnclosure());
		}
		
		String v=data.previousMeta.getString(r, data.fieldnr);
		
		// reserve room
		Object[] outputRow = RowDataUtil.allocateRowData(data.outputMeta.size());
		
		int nrExtraFields = meta.getFieldID().length - 1;
		
		for (int i=0;i<data.fieldnr;i++) outputRow[i] = r[i];
		for (int i=data.fieldnr+1;i<data.previousMeta.size();i++) outputRow[i+nrExtraFields] = r[i];

		// OK, now we have room in the middle to place the fields...
		//
		
		// Named values info.id[0] not filled in!
		boolean use_ids = meta.getFieldID().length>0 && meta.getFieldID()[0]!=null && meta.getFieldID()[0].length()>0;
		
		Object value=null;
		String[] splits = Const.splitString(v, data.delimiter, data.enclosure);
		if (use_ids)
		{
			if (log.isDebug()) logDebug(BaseMessages.getString(PKG, "FieldSplitter.Log.UsingIds")); 
			
			// We have to add info.field.length variables!
			for(int i=0; i < meta.getFieldName().length; i++)
			{
				String split = (splits == null || i>= splits.length) ? null : splits[i];
        
				// Optionally remove the indicator			
				if (split != null && meta.getFieldRemoveID()[i])
				{
				  final StringBuilder sb = new StringBuilder(split);
          final int idx = sb.indexOf(meta.getFieldID()[i]);
					sb.delete(idx, idx+meta.getFieldID()[i].length());
					split=sb.toString();
				}

				if (split==null) split=""; 
				if (log.isDebug()) logDebug(BaseMessages.getString(PKG, "FieldSplitter.Log.SplitInfo")+split); 

				try
				{
					ValueMetaInterface valueMeta = data.outputMeta.getValueMeta(data.fieldnr+i);
					ValueMetaInterface conversionValueMeta = data.conversionMeta.getValueMeta(data.fieldnr+i);
					value = valueMeta.convertDataFromString
					(
						split,
						conversionValueMeta,
						meta.getFieldNullIf()[i],
						meta.getFieldIfNull()[i],
						meta.getFieldTrimType()[i]
					);
				}
				catch(Exception e)
				{
					throw new KettleValueException(BaseMessages.getString(PKG, "FieldSplitter.Log.ErrorConvertingSplitValue",split,meta.getSplitField()+"]!"), e);   //$NON-NLS-3$
				}
				outputRow[data.fieldnr+i]=value;
			}
		}
		else
		{
			if (log.isDebug()) logDebug(BaseMessages.getString(PKG, "FieldSplitter.Log.UsingPositionOfValue")); 
			int prev=0;
			for (int i=0;i<meta.getFieldName().length;i++)
			{
				String pol = (splits == null || i>= splits.length) ? null : splits[i];
				if (log.isDebug()) logDebug(BaseMessages.getString(PKG, "FieldSplitter.Log.SplitFieldsInfo",pol,String.valueOf(prev)));  
				prev+=(pol==null?0:pol.length()) + data.delimiter.length();
				
				try
				{
					ValueMetaInterface valueMeta = data.outputMeta.getValueMeta(data.fieldnr+i); 
					ValueMetaInterface conversionValueMeta = data.conversionMeta.getValueMeta(data.fieldnr+i);
					value = valueMeta.convertDataFromString
					(
						pol,
						conversionValueMeta,
						meta.getFieldNullIf()[i],
						meta.getFieldIfNull()[i],
						meta.getFieldTrimType()[i]
					);
				}
				catch(Exception e)
				{
					throw new KettleValueException(BaseMessages.getString(PKG, "FieldSplitter.Log.ErrorConvertingSplitValue",pol,meta.getSplitField()+"]!"), e);   //$NON-NLS-3$
				}
				outputRow[data.fieldnr+i]=value;
			}
		}
		
		return outputRow;
	}
	
	public synchronized boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(FieldSplitterMeta)smi;
		data=(FieldSplitterData)sdi;

		Object[] r=getRow();   // get row from rowset, wait for our turn, indicate busy!
		if (r==null)  // no more input to be expected...
		{
			setOutputDone();
			return false;
		}
		
		Object[] outputRowData = splitField(r);
		putRow(data.outputMeta, outputRowData);

        if (checkFeedback(getLinesRead())) 
        {
        	if(log.isBasic()) logBasic(BaseMessages.getString(PKG, "FieldSplitter.Log.LineNumber")+getLinesRead()); 
        }
			
		return true;
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(FieldSplitterMeta)smi;
		data=(FieldSplitterData)sdi;
		
		if (super.init(smi, sdi))
		{
		    // Add init code here.
		    return true;
		}
		return false;
	}

}