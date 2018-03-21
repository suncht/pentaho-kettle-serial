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

package org.pentaho.di.trans.steps.setvalueconstant;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.util.StringUtil;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;


/**
 * Replace Field value by a constant value.
 * 
 * @author Samatar
 * @since 30-06-2008
 */

public class SetValueConstant extends BaseStep implements StepInterface
{
	private static Class<?> PKG = SetValueConstantMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private SetValueConstantMeta meta;
	private SetValueConstantData data;
	
	public SetValueConstant(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(SetValueConstantMeta)smi;
		data=(SetValueConstantData)sdi;

		Object[] r=getRow();    // get row, set busy!
		if (r==null)  // no more input to be expected...
		{
			setOutputDone();
			return false;
		}
		
		if(first)
		{
			first=false;
		
			// What's the format of the output row?
			data.outputRowMeta = getInputRowMeta().clone();
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this, repository, metaStore);
			// Create convert meta-data objects that will contain Date & Number formatters
            //data.convertRowMeta = data.outputRowMeta.clone();
            
            // For String to <type> conversions, we allocate a conversion meta data row as well...
			//
			data.convertRowMeta = data.outputRowMeta.cloneToType(ValueMetaInterface.TYPE_STRING);            
			
			// Consider only selected fields
			if(meta.getFieldName()!=null && meta.getFieldName().length>0)
			{
				data.fieldnrs=new int[meta.getFieldName().length];
				data.realReplaceByvalues=new String[meta.getReplaceValue().length];
				for (int i=0;i<meta.getFieldName().length;i++)
				{
					// Check if this field was specified only one time
					for (int j=0;j<meta.getFieldName().length;j++)
					{
						if(meta.getFieldName()[j].equals(meta.getFieldName()[i]))
						{
							if(j!=i) throw new KettleException(BaseMessages.getString(PKG, "SetValueConstant.Log.FieldSpecifiedMoreThatOne",meta.getFieldName()[i],""+i,""+j));
						}
					}
					
					data.fieldnrs[i]=data.outputRowMeta.indexOfValue(meta.getFieldName()[i] );
					
					if (data.fieldnrs[i]<0)
					{
						logError(BaseMessages.getString(PKG, "SetValueConstant.Log.CanNotFindField",meta.getFieldName()[i]));
						throw new KettleException(BaseMessages.getString(PKG, "SetValueConstant.Log.CanNotFindField",meta.getFieldName()[i]));
					}
					
					if(meta.isSetEmptyString()[i]) 
					{
						// Just set empty string
						data.realReplaceByvalues[i]= StringUtil.EMPTY_STRING;
					}
					else
					{
						// set specified value
						if(meta.isUseVars())
							data.realReplaceByvalues[i]=environmentSubstitute(meta.getReplaceValue()[i]);
						else
							data.realReplaceByvalues[i]=meta.getReplaceValue()[i];
					}
	 			}
			}else
				throw new KettleException(BaseMessages.getString(PKG, "SetValueConstant.Log.SelectFieldsEmpty"));

			data.fieldnr=data.fieldnrs.length;

		} // end if first

		try
		{
			 updateField(r);
			 putRow(data.outputRowMeta, r);  // copy row to output rowset(s);
		}  catch(Exception e)
        {
	        boolean sendToErrorRow=false;
	        String errorMessage = null;
	        
        	if (getStepMeta().isDoingErrorHandling())
        	{
                 sendToErrorRow = true;
                 errorMessage = e.toString();
        	}
        	else
        	{
        		logError(BaseMessages.getString(PKG, "SetValueConstant.Log.ErrorInStep",e.getMessage())); 
				setErrors(1);
				stopAll();
				setOutputDone();  // signal end to receiver(s)
				return false;
        	}
        	if (sendToErrorRow)
        	{
        	   // Simply add this row to the error row
        	   putError(data.outputRowMeta, r, 1, errorMessage, null, "SVC001");
        	}
        }
		return true;
	}
	private void updateField(Object[] r) throws Exception
	{
		// Loop through fields
		for(int i=0;i<data.fieldnr;i++)
		{		
			// DO CONVERSION OF THE DEFAULT VALUE ...
			// Entered by user
			ValueMetaInterface targetValueMeta = data.outputRowMeta.getValueMeta(data.fieldnrs[i]);
			ValueMetaInterface sourceValueMeta = data.convertRowMeta.getValueMeta(data.fieldnrs[i]);
			if(!Const.isEmpty(meta.getReplaceMask()[i])) sourceValueMeta.setConversionMask(meta.getReplaceMask()[i]);
			r[data.fieldnrs[i]] = targetValueMeta.convertData(sourceValueMeta, data.realReplaceByvalues[i]);
		}
	}
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(SetValueConstantMeta)smi;
		data=(SetValueConstantData)sdi;
		
		if (super.init(smi, sdi))
		{
		    // Add init code here.
		    return true;
		}
		return false;
	}
	
}
