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

package org.pentaho.di.trans.steps.samplerows;

import java.util.HashSet;
import java.util.Set;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
 * @author Samatar
 * @since 24-jan-2008
 *
 */
public class SampleRowsData extends BaseStepData implements StepDataInterface
{

	public Set<Integer> range;
	public int maxLine;
	public boolean addlineField;
	public RowMetaInterface previousRowMeta;
	public RowMetaInterface outputRowMeta;
	public Object[] outputRow;
	public int NrPrevFields;
  public boolean considerRow;
	
	/**
	 * 
	 */
	public SampleRowsData()
	{
		super();
		range = new HashSet<Integer>();
		maxLine=0;
		addlineField=false;
		outputRow=null;
	}

}
