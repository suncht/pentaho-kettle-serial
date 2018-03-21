/*!
 * Copyright 2010 - 2015 Pentaho Corporation.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.pentaho.di.ui.repository.pur;

import org.pentaho.di.repository.pur.PurRepositoryMeta;

public interface IRepositoryConfigDialogCallback {
  /**
   * On a successful configuration of a repostory, this method is invoked
   * 
   * @param repositoryMeta
   */
  void onSuccess( PurRepositoryMeta repositoryMeta );

  /**
   * On a user cancelation from the repository configuration dialog, this method is invoked
   */
  void onCancel();

  /**
   * On any error caught during the repository configuration process, this method is invoked
   * 
   * @param t
   */
  void onError( Throwable t );
}
