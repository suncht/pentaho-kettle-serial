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

package org.pentaho.di.trans.steps.excelinput;

import org.pentaho.di.core.spreadsheet.KCell;

/**
 * Represent 1 row in a an Excel sheet.
 */
public class ExcelInputRow {

  public final String sheetName;
  public final int rownr;
  public final KCell[] cells;

  public ExcelInputRow( String sheetName, int rownr, KCell[] cells ) {
    this.sheetName = sheetName;
    this.rownr = rownr;
    this.cells = cells;
  }
}
