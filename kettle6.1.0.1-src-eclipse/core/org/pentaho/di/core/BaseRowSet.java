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

package org.pentaho.di.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.pentaho.di.core.row.RowMetaInterface;

/**
 * Contains the base RowSet class to help implement RowSet variants.
 *
 * @author Matt
 * @since 22-01-2010
 *
 */
abstract class BaseRowSet implements Comparable<RowSet>, RowSet {
  protected RowMetaInterface rowMeta;

  protected AtomicBoolean done;
  protected String originStepName;
  protected AtomicInteger originStepCopy;
  protected String destinationStepName;
  protected AtomicInteger destinationStepCopy;

  protected String remoteSlaveServerName;

  /**
   * Create new non-blocking-queue with maxSize capacity.
   *
   * @param maxSize
   */
  public BaseRowSet() {
    // not done putting data into this RowSet
    done = new AtomicBoolean( false );

    originStepCopy = new AtomicInteger( 0 );
    destinationStepCopy = new AtomicInteger( 0 );
  }

  /**
   * Compares using the target steps and copy, not the source. That way, re-partitioning is always done in the same way.
   *
   */
  @Override
  public int compareTo( RowSet rowSet ) {
    String target = remoteSlaveServerName + "." + destinationStepName + "." + destinationStepCopy.intValue();
    String comp =
      rowSet.getRemoteSlaveServerName()
        + "." + rowSet.getDestinationStepName() + "." + rowSet.getDestinationStepCopy();

    return target.compareTo( comp );
  }

  public boolean equals( BaseRowSet rowSet ) {
    return compareTo( rowSet ) == 0;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#putRow(org.pentaho.di.core.row.RowMetaInterface, java.lang.Object[])
   */
  @Override
  public abstract boolean putRow( RowMetaInterface rowMeta, Object[] rowData );

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#putRowWait(org.pentaho.di.core.row.RowMetaInterface, java.lang.Object[],
   * long, java.util.concurrent.TimeUnit)
   */
  @Override
  public abstract boolean putRowWait( RowMetaInterface rowMeta, Object[] rowData, long time, TimeUnit tu );

  // default getRow with wait time = 100ms
  //
  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#getRow()
   */
  @Override
  public abstract Object[] getRow();

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#getRowImmediate()
   */
  @Override
  public abstract Object[] getRowImmediate();

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#getRowWait(long, java.util.concurrent.TimeUnit)
   */
  @Override
  public abstract Object[] getRowWait( long timeout, TimeUnit tu );

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#setDone()
   */
  @Override
  public void setDone() {
    done.set( true );
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#isDone()
   */
  @Override
  public boolean isDone() {
    return done.get();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#getOriginStepName()
   */
  @Override
  public String getOriginStepName() {
    synchronized ( originStepName ) {
      return originStepName;
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#getOriginStepCopy()
   */
  @Override
  public int getOriginStepCopy() {
    return originStepCopy.get();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#getDestinationStepName()
   */
  @Override
  public String getDestinationStepName() {
    return destinationStepName;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#getDestinationStepCopy()
   */
  @Override
  public int getDestinationStepCopy() {
    return destinationStepCopy.get();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#getName()
   */
  @Override
  public String getName() {
    return toString();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#size()
   */
  @Override
  public abstract int size();

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#setThreadNameFromToCopy(java.lang.String, int, java.lang.String, int)
   */
  @Override
  public void setThreadNameFromToCopy( String from, int from_copy, String to, int to_copy ) {
    if ( originStepName == null ) {
      originStepName = from;
    } else {
      synchronized ( originStepName ) {
        originStepName = from;
      }
    }

    originStepCopy.set( from_copy );

    if ( destinationStepName == null ) {
      destinationStepName = to;
    } else {
      synchronized ( destinationStepName ) {
        destinationStepName = to;
      }
    }

    destinationStepCopy.set( to_copy );
  }

  @Override
  public String toString() {
    StringBuffer str;
    synchronized ( originStepName ) {
      str = new StringBuffer( originStepName );
    }
    str.append( "." );
    synchronized ( originStepCopy ) {
      str.append( originStepCopy );
    }
    str.append( " - " );
    synchronized ( destinationStepName ) {
      str.append( destinationStepName );
    }
    str.append( "." );
    synchronized ( destinationStepCopy ) {
      str.append( destinationStepCopy );
    }
    if ( !Const.isEmpty( remoteSlaveServerName ) ) {
      synchronized ( remoteSlaveServerName ) {
        str.append( " (" );
        str.append( remoteSlaveServerName );
        str.append( ")" );
      }
    }
    return str.toString();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#getRowMeta()
   */
  @Override
  public RowMetaInterface getRowMeta() {
    return rowMeta;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#setRowMeta(org.pentaho.di.core.row.RowMetaInterface)
   */
  @Override
  public void setRowMeta( RowMetaInterface rowMeta ) {
    this.rowMeta = rowMeta;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#getRemoteSlaveServerName()
   */
  @Override
  public String getRemoteSlaveServerName() {
    return remoteSlaveServerName;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.di.core.RowSetInterface#setRemoteSlaveServerName(java.lang.String)
   */
  @Override
  public void setRemoteSlaveServerName( String remoteSlaveServerName ) {
    this.remoteSlaveServerName = remoteSlaveServerName;
  }

  /**
   * By default we don't report blocking, only for monitored transformations.
   *
   * @return true if this row set is blocking on reading or writing.
   */
  @Override
  public boolean isBlocking() {
    return false;
  }

}
