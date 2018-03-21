/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2013 by Pentaho : http://www.pentaho.com
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

package org.pentaho.di.trans.steps.ifnull;

import java.util.HashMap;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
 * @author Samatar
 * @since 16-06-2008
 *
 */
public class IfNullData extends BaseStepData implements StepDataInterface {

  public RowMetaInterface outputRowMeta;
  public RowMetaInterface convertRowMeta;

  public int[] fieldnrs;
  public int fieldnr;
  public String realReplaceByValue;
  public String realconversionMask;
  public boolean realSetEmptyString;

  public HashMap<String, Integer> ListTypes;
  public String[] defaultValues;
  public String[] defaultMasks;
  public boolean[] setEmptyString;

  public IfNullData() {
    super();
    ListTypes = new HashMap<String, Integer>();
  }

}
