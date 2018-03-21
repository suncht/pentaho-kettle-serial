/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2015 by Pentaho : http://www.pentaho.com
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

package org.pentaho.di.ui.spoon.delegates;

import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.partition.PartitionSchema;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.partition.dialog.PartitionSchemaDialog;
import org.pentaho.di.ui.spoon.Spoon;

public class SpoonPartitionsDelegate extends SpoonSharedObjectDelegate {
  public SpoonPartitionsDelegate( Spoon spoon ) {
    super( spoon );
  }

  public void newPartitioningSchema( TransMeta transMeta ) {
    PartitionSchema partitionSchema = new PartitionSchema();

    PartitionSchemaDialog dialog =
        new PartitionSchemaDialog( spoon.getShell(), partitionSchema, transMeta.getPartitionSchemas(), transMeta
            .getDatabases(), transMeta );
    if ( dialog.open() ) {
      List<PartitionSchema> partitions = transMeta.getPartitionSchemas();
      if ( isDuplicate( partitions, partitionSchema ) ) {
        new ErrorDialog(
          spoon.getShell(), getMessage( "Spoon.Dialog.ErrorSavingPartition.Title" ), getMessage(
          "Spoon.Dialog.ErrorSavingPartition.Message", partitionSchema.getName() ),
          new KettleException( getMessage( "Spoon.Dialog.ErrorSavingPartition.NotUnique" ) ) );
        return;
      }

      partitions.add( partitionSchema );

      if ( spoon.rep != null ) {
        try {
          if ( !spoon.rep.getSecurityProvider().isReadOnly() ) {
            spoon.rep.save( partitionSchema, Const.VERSION_COMMENT_INITIAL_VERSION, null );
          } else {
            throw new KettleException( BaseMessages.getString(
              PKG, "Spoon.Dialog.Exception.ReadOnlyRepositoryUser" ) );
          }
        } catch ( KettleException e ) {
          new ErrorDialog(
            spoon.getShell(), BaseMessages.getString( PKG, "Spoon.Dialog.ErrorSavingPartition.Title" ), BaseMessages
            .getString( PKG, "Spoon.Dialog.ErrorSavingPartition.Message", partitionSchema.getName() ), e );
        }
      }

      spoon.refreshTree();
    }
  }
}
