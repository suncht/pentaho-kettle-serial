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
package com.pentaho.repository.importexport;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.parameters.DuplicateParamException;
import org.pentaho.di.core.parameters.NamedParams;
import org.pentaho.di.core.parameters.NamedParamsDefault;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.util.IPdiContentProvider;
import org.pentaho.platform.engine.core.system.PentahoSystem;

public class PdiContentProvider implements IPdiContentProvider {

  private Log log = LogFactory.getLog( PdiContentProvider.class );

  IUnifiedRepository unifiedRepository = PentahoSystem.get( IUnifiedRepository.class, null );

  @Override
  public boolean hasUserParameters( String kettleFilePath ) {

    if ( !StringUtils.isEmpty( kettleFilePath ) ) {

      RepositoryFile file = unifiedRepository.getFile( kettleFilePath );

      if ( file != null ) {

        try {

          return hasUserParameters( getMeta( file ) );

        } catch ( KettleException e ) {
          log.error( e );
        }
      }
    }

    return false;
  }

  @Override
  public String[] getUserParameters( String kettleFilePath ) {

    List<String> userParams = new ArrayList<String>();

    if ( !StringUtils.isEmpty( kettleFilePath ) ) {

      RepositoryFile file = unifiedRepository.getFile( kettleFilePath );

      if ( file != null ) {

        try {

          NamedParams np = getMeta( file );

          if ( !isEmpty( np = filterUserParameters( np ) ) ) {

            return np.listParameters();
          }

        } catch ( KettleException e ) {
          log.error( e );
        }
      }
    }

    return userParams.toArray( new String[] {} );
  }

  private NamedParams filterUserParameters( NamedParams params ) {

    NamedParams userParams = new NamedParamsDefault();

    if ( !isEmpty( params ) ) {

      for ( String paramName : params.listParameters() ) {

        if ( isUserParameter( paramName ) ) {
          try {
            userParams.addParameterDefinition( paramName, StringUtils.EMPTY, StringUtils.EMPTY );
          } catch ( DuplicateParamException e ) {
            // ignore
          }
        }
      }
    }

    return userParams;
  }

  private NamedParams getMeta( RepositoryFile file ) throws KettleException {

    NamedParams meta = null;

    if ( file != null ) {

      String extension = FilenameUtils.getExtension( file.getName() );
      Repository repo = PDIImportUtil.connectToRepository( null );

      if ( "ktr".equalsIgnoreCase( extension ) ) {

        meta = new TransMeta( convertTransformation( file.getId() ), repo, true, null, null );

      } else if ( "kjb".equalsIgnoreCase( extension ) ) {

        meta = new JobMeta( convertJob( file.getId() ), repo, null );

      }
    }

    return meta;
  }

  private InputStream convertTransformation( Serializable fileId ) {
    return new StreamToTransNodeConverter( unifiedRepository ).convert( fileId );
  }

  private InputStream convertJob( Serializable fileId ) {
    return new StreamToJobNodeConverter( unifiedRepository ).convert( fileId );
  }

  private boolean isUserParameter( String paramName ) {

    if ( !StringUtils.isEmpty( paramName ) ) {
      // prevent rendering of protected/hidden/system parameters
      if ( paramName.startsWith( IPdiContentProvider.PROTECTED_PARAMETER_PREFIX ) ) {
        return false;
      }
    }
    return true;
  }

  private boolean hasUserParameters( NamedParams params ) {
    return !isEmpty( filterUserParameters( params ) );
  }

  private boolean isEmpty( NamedParams np ) {
    return np == null || np.listParameters() == null || np.listParameters().length == 0;
  }
}
