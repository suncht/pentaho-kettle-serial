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

package org.pentaho.di.trans.steps.combinationlookup;

import java.sql.PreparedStatement;
import java.util.Map;

import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

/**
 * @author Matt
 * @since 24-jan-2005
 */
public class CombinationLookupData extends BaseStepData implements StepDataInterface {
  public Database db;
  public int[] keynrs; // nrs in row of the keys

  public Map<RowMetaAndData, Long> cache;

  public RowMetaInterface outputRowMeta;
  public RowMetaInterface lookupRowMeta;
  public RowMetaInterface insertRowMeta;
  public RowMetaInterface hashRowMeta;
  public String realTableName;
  public String realSchemaName;
  public boolean[] removeField;

  public String schemaTable;

  public PreparedStatement prepStatementLookup;
  public PreparedStatement prepStatementInsert;
  public long smallestCacheKey;

  /**
   * Default Constructor
   */
  public CombinationLookupData() {
    super();
    db = null;
    realTableName = null;
    realSchemaName = null;
  }
}
