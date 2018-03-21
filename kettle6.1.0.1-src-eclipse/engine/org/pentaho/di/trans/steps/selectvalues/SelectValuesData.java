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

package org.pentaho.di.trans.steps.selectvalues;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
 * @author Matt
 * @since 24-jan-2005
 */
public class SelectValuesData extends BaseStepData implements StepDataInterface {
  public int[] fieldnrs;
  public int[] extraFieldnrs;
  public int[] removenrs;
  public int[] metanrs;

  public boolean firstselect;
  public boolean firstdeselect;
  public boolean firstmetadata;

  public RowMetaInterface selectRowMeta;
  public RowMetaInterface deselectRowMeta;
  public RowMetaInterface metadataRowMeta;

  public RowMetaInterface outputRowMeta;

  // The MODE, default = select...
  public boolean select; // "normal" selection of fields.
  public boolean deselect; // de-select mode
  public boolean metadata; // change meta-data (rename & change length/precision)
}
