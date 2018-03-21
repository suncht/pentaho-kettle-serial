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

package org.pentaho.di.ui.repository.repositoryexplorer.model;

import java.util.List;

import org.pentaho.di.cluster.ClusterSchema;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.ui.xul.XulEventSourceAdapter;

public class UICluster extends XulEventSourceAdapter {

  private ClusterSchema cluster;

  public UICluster( ClusterSchema clusterSchema ) {
    this.cluster = clusterSchema;
  }

  public String getName() {
    if ( cluster != null ) {
      return cluster.getName();
    }
    return null;
  }

  public ClusterSchema getClusterSchema() {
    return this.cluster;
  }

  public String getServerList() {
    if ( cluster != null ) {
      List<SlaveServer> slaves = cluster.getSlaveServers();
      if ( slaves != null ) {
        StringBuilder sb = new StringBuilder();
        for ( SlaveServer slave : slaves ) {
          // Append separator before slave
          if ( sb.length() > 0 ) {
            sb.append( ", " );
          }
          sb.append( slave.getName() );
        }

        if ( sb.length() > 0 ) {
          return sb.toString();
        }
      }
    }
    return null;
  }

}
