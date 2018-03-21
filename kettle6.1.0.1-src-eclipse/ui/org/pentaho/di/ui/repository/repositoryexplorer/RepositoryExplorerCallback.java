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

package org.pentaho.di.ui.repository.repositoryexplorer;

import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryContent;

public interface RepositoryExplorerCallback {
  /**
   * request that specified object be opened in 'Spoon' display
   *
   * @param object
   * @return boolean indicating if repository explorer dialog should close
   */
  boolean open( UIRepositoryContent object, String revision ) throws Exception;

  /**
   * The method is called when a connection to current repository has been lost
   * @param message - error message
   * @return <code>true</code> if it is required to close the dialog and <code>false</code> otherwise
   */
  boolean error( String message ) throws Exception;

}
