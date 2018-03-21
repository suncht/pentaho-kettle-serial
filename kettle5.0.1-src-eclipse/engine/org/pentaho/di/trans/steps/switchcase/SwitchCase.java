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

package org.pentaho.di.trans.steps.switchcase;

import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepIOMetaInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.step.errorhandling.StreamInterface;

/**
 * Filters input rows base on conditions.
 * 
 * @author Matt
 * @since 16-apr-2003, 07-nov-2004 (rewrite)
 */
public class SwitchCase extends BaseStep implements StepInterface
{
	private static Class<?> PKG = SwitchCaseMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private SwitchCaseMeta meta;
	private SwitchCaseData data;
	
	public SwitchCase(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	
	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(SwitchCaseMeta)smi;
		data=(SwitchCaseData)sdi;

		Object[] r=getRow();       // Get next usable row from input rowset(s)!
		if (r==null)  // no more input to be expected...
		{
			setOutputDone();
			return false;
		}
        
        if (first)
        {
        	first = false;
        	
            data.outputRowMeta = getInputRowMeta().clone();
            meta.getFields(getInputRowMeta(), getStepname(), null, null, this, repository, metaStore);

            data.fieldIndex = getInputRowMeta().indexOfValue(meta.getFieldname());
            if (data.fieldIndex<0) {
            	throw new KettleException(BaseMessages.getString(PKG, "SwitchCase.Exception.UnableToFindFieldName", meta.getFieldname())); 
            }

            data.inputValueMeta = getInputRowMeta().getValueMeta(data.fieldIndex); 
            
        	try {
        		StepIOMetaInterface ioMeta = meta.getStepIOMeta();
        		
        		// There is one case target for each target stream.
        		// The ioMeta object has one more target stream for the default target though.
        		//
        		List<StreamInterface> targetStreams = ioMeta.getTargetStreams();
        		for (int i=0;i<targetStreams.size();i++) {
        			SwitchCaseTarget target = (SwitchCaseTarget) targetStreams.get(i).getSubject();
        			if (target==null) {
        			   break; // Skip over default option
        			}
        			if (target.caseTargetStep==null) {
	        			throw new KettleException(BaseMessages.getString(PKG, "SwitchCase.Log.NoTargetStepSpecifiedForValue", target.caseValue)); 
	        		} else {
	        			RowSet rowSet = findOutputRowSet(target.caseTargetStep.getName());
	        			if (rowSet!=null) {
		            		try {
		            			Object value = data.valueMeta.convertDataFromString(target.caseValue, data.stringValueMeta, null, null, ValueMeta.TRIM_TYPE_NONE);
		            			
		            			// If we have a value and a rowset, we can store the combination in the map
		            			//
		            			if (data.valueMeta.isNull(value)) {
		            				data.nullRowSet = rowSet;
		            			} else {
		            				data.outputMap.put(value, rowSet);
		            			}
		            			
		            		}
		            		catch(Exception e) {
		            			throw new KettleException(BaseMessages.getString(PKG, "SwitchCase.Log.UnableToConvertValue", target.caseValue), e); 
		            		}
	        			} else {
	            			throw new KettleException(BaseMessages.getString(PKG, "SwitchCase.Log.UnableToFindTargetRowSetForStep", target.caseTargetStep)); 
	        			}
	        		}
	        	}
	        	
	        	if (meta.getDefaultTargetStep()!=null) {
	        		data.defaultRowSet = findOutputRowSet(meta.getDefaultTargetStep().getName());
	        	} else {
	        		data.defaultRowSet = null;
	        	}
        	}
        	catch(Exception e) {
        	    throw new KettleException(e);
        	}

        }

        // We already know the target values, but we need to make sure that the input data type is the same as the specified one.
        // Perhaps there is some conversion needed.
        //
        Object lookupData = data.valueMeta.convertData(data.inputValueMeta, r[data.fieldIndex]);
        
        // Determine the output rowset to use...
        //
        RowSet rowSet = null;
        if (lookupData == null) {
        	rowSet = data.nullRowSet;
        } else {
        	rowSet = data.outputMap.get(lookupData);
        }
        
        // If the rowset is still not found (unspecified key value, we drop down to the default option
        // For now: send it to the default step...
        //
        if (rowSet==null) {
        	if (data.defaultRowSet!=null) {
        		putRowTo(data.outputRowMeta, r, data.defaultRowSet);
        	}
        } else {
        	putRowTo(data.outputRowMeta, r, rowSet);
        }
        
        if (checkFeedback(getLinesRead())) 
        {
        	if (log.isBasic()) logBasic(BaseMessages.getString(PKG, "SwitchCase.Log.LineNumber")+getLinesRead()); 
        }
			
		return true;
	}

	/**
     * @see StepInterface#init( org.pentaho.di.trans.step.StepMetaInterface , org.pentaho.di.trans.step.StepDataInterface)
     */
    public boolean init(StepMetaInterface smi, StepDataInterface sdi)
    {
		meta=(SwitchCaseMeta)smi;
		data=(SwitchCaseData)sdi;

        if (super.init(smi, sdi))
        {
            data.outputMap = meta.isContains() ? new ContainsKeyToRowSetMap() : new KeyToRowSetMap();
        	
        	if (Const.isEmpty(meta.getFieldname())) {
        		logError(BaseMessages.getString(PKG, "SwitchCase.Log.NoFieldSpecifiedToSwitchWith")); 
        		return false;
        	}
        	
        	try {
        	  data.valueMeta = ValueMetaFactory.createValueMeta(meta.getFieldname(), meta.getCaseValueType());
        	  data.valueMeta.setConversionMask(meta.getCaseValueFormat());
        	  data.valueMeta.setGroupingSymbol(meta.getCaseValueGroup());
        	  data.valueMeta.setDecimalSymbol(meta.getCaseValueDecimal());
        	  data.stringValueMeta = ValueMetaFactory.cloneValueMeta(data.valueMeta, ValueMetaInterface.TYPE_STRING);
        	} catch(Exception e) {
        	  logError(BaseMessages.getString(PKG, "SwitchCase.Log.UnexpectedError", e));
        	}
        	
        	return true;
        }
        return false;
    }

}