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

package org.pentaho.di.trans.steps.stepsmetrics;

import java.util.concurrent.ConcurrentHashMap;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;

/**
 * @author Samatar
 * @since 16-06-2008
 *
 */
public class StepsMetricsData extends BaseStepData implements StepDataInterface {

  boolean continueLoop;
  public ConcurrentHashMap<Integer, StepInterface> stepInterfaces;
  /** The metadata we send out */
  public RowMetaInterface outputRowMeta;

  public String realstepnamefield;
  public String realstepidfield;
  public String realsteplinesinputfield;
  public String realsteplinesoutputfield;
  public String realsteplinesreadfield;
  public String realsteplinesupdatedfield;
  public String realsteplineswrittentfield;
  public String realsteplineserrorsfield;
  public String realstepsecondsfield;

  public StepsMetricsData() {
    super();
    continueLoop = true;

    realstepnamefield = null;
    realstepidfield = null;
    realsteplinesinputfield = null;
    realsteplinesoutputfield = null;
    realsteplinesreadfield = null;
    realsteplinesupdatedfield = null;
    realsteplineswrittentfield = null;
    realsteplineserrorsfield = null;
    realstepsecondsfield = null;
  }
}
