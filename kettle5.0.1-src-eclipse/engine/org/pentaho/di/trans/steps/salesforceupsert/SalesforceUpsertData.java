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

package org.pentaho.di.trans.steps.salesforceupsert;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.steps.salesforceinput.SalesforceConnection;

import com.sforce.soap.partner.UpsertResult;
import com.sforce.soap.partner.sobject.SObject;

/*
 * @author Samatar
 * @since 10-06-2007
 */
public class SalesforceUpsertData extends BaseStepData implements StepDataInterface 
{
	public RowMetaInterface inputRowMeta;
	public RowMetaInterface outputRowMeta;

    public int nrfields;
    public String realURL;
    public String realModule;
    public int[] fieldnrs;

	public SalesforceConnection connection;
	public UpsertResult[] upsertResult;
	
	public SObject[] sfBuffer;
	public Object[][] outputBuffer; 
	public int iBufferPos;
	
	public String realSalesforceFieldName;
    
    
	/**
	 * 
	 */
	public SalesforceUpsertData()
	{
		super();

		nrfields=0;
		
		connection=null;
		realURL=null;
		upsertResult=null;
		realModule=null;
		realSalesforceFieldName=null;
		iBufferPos=0;
	}
}