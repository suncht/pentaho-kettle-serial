/*
 *   This file is part of PaloKettlePlugin.
 *
 *   PaloKettlePlugin is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   PaloKettlePlugin is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with PaloKettlePlugin.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Portions Copyright 2008 Stratebi Business Solutions, S.L.
 *   Portions Copyright 2011 De Bortoli Wines Pty Limited (Australia)
 *   Portions Copyright 2011 - 2013 Pentaho Corporation
 */

package org.pentaho.di.trans.steps.palo.cellinput;

import org.pentaho.di.palo.core.PaloHelper;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.database.PALODatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.DefaultLogLevel;

public class PaloCellInputData extends BaseStepData implements StepDataInterface {
  public PaloHelper helper;

  public PaloCellInputData( DatabaseMeta databaseMeta ) throws KettleException {
    super();
    if ( !( databaseMeta.getDatabaseInterface() instanceof PALODatabaseMeta ) ) {
      throw new KettleException( "A connection of type PALO is expected" );
    }
    this.helper = new PaloHelper( databaseMeta, DefaultLogLevel.getLogLevel() );
  }
}
