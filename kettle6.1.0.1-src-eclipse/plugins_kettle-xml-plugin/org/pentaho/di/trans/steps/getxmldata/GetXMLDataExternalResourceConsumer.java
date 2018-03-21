/*
 * ******************************************************************************
 *
 * Copyright (C) 2002-2015 by Pentaho : http://www.pentaho.com
 *
 * ******************************************************************************
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
 */

package org.pentaho.di.trans.steps.getxmldata;

import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.metaverse.api.IAnalysisContext;
import org.pentaho.metaverse.api.analyzer.kettle.step.BaseStepExternalResourceConsumer;
import org.pentaho.metaverse.api.model.ExternalResourceInfoFactory;
import org.pentaho.metaverse.api.model.IExternalResourceInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public class GetXMLDataExternalResourceConsumer
  extends BaseStepExternalResourceConsumer<GetXMLData, GetXMLDataMeta> {

  @Override
  public boolean isDataDriven( GetXMLDataMeta meta ) {
    // We can safely assume that the StepMetaInterface object we get back is a GetXMLDataMeta
    return meta.isInFields();
  }

  @Override
  public Collection<IExternalResourceInfo> getResourcesFromMeta( GetXMLDataMeta meta, IAnalysisContext context ) {
    Collection<IExternalResourceInfo> resources = Collections.emptyList();

    // We only need to collect these resources if we're not data-driven and there are no used variables in the
    // metadata relating to external files.
    if ( !isDataDriven( meta ) ) {
      StepMeta parentStepMeta = meta.getParentStepMeta();
      if ( parentStepMeta != null ) {
        TransMeta parentTransMeta = parentStepMeta.getParentTransMeta();
        if ( parentTransMeta != null ) {
          String[] paths = parentTransMeta.environmentSubstitute( meta.getFileName() );
          if ( paths != null ) {
            resources = new ArrayList<IExternalResourceInfo>( paths.length );

            for ( String path : paths ) {
              if ( !Const.isEmpty( path ) ) {
                try {

                  IExternalResourceInfo resource = ExternalResourceInfoFactory
                    .createFileResource( KettleVFS.getFileObject( path ), true );
                  if ( resource != null ) {
                    resources.add( resource );
                  } else {
                    throw new KettleFileException( "Error getting file resource!" );
                  }
                } catch ( KettleFileException kfe ) {
                  // TODO throw or ignore?
                }
              }
            }
          }
        }
      }
    }
    return resources;
  }

  @Override
  public Collection<IExternalResourceInfo> getResourcesFromRow(
    GetXMLData textFileInput, RowMetaInterface rowMeta, Object[] row ) {
    Collection<IExternalResourceInfo> resources = new LinkedList<IExternalResourceInfo>();
    // For some reason the step doesn't return the StepMetaInterface directly, so go around it
    GetXMLDataMeta meta = (GetXMLDataMeta) textFileInput.getStepMetaInterface();
    if ( meta == null ) {
      meta = (GetXMLDataMeta) textFileInput.getStepMeta().getStepMetaInterface();
    }

    try {
      if ( meta.getIsAFile() ) {
        String filename = ( meta == null ) ? null : rowMeta.getString( row, meta.getXMLField(), null );
        if ( !Const.isEmpty( filename ) ) {
          FileObject fileObject = KettleVFS.getFileObject( filename );
          resources.add( ExternalResourceInfoFactory.createFileResource( fileObject, true ) );
        }
      }
      // TODO URLs?
    } catch ( KettleException kve ) {
      // TODO throw exception or ignore?
    }

    return resources;
  }

  @Override
  public Class<GetXMLDataMeta> getMetaClass() {
    return GetXMLDataMeta.class;
  }
}
