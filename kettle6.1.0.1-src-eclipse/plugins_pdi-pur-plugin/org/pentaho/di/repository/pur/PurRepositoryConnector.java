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
package org.pentaho.di.repository.pur;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.xml.ws.WebServiceException;

import org.apache.commons.lang.BooleanUtils;
import org.pentaho.di.core.encryption.Encr;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleSecurityException;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.util.ExecutorUtil;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.IUser;
import org.pentaho.di.repository.RepositorySecurityManager;
import org.pentaho.di.repository.RepositorySecurityProvider;
import org.pentaho.di.repository.pur.PurRepository;
import org.pentaho.di.repository.pur.model.EEUserInfo;
import org.pentaho.di.ui.repository.pur.services.IAbsSecurityManager;
import org.pentaho.di.ui.repository.pur.services.IAbsSecurityProvider;
import org.pentaho.di.ui.repository.pur.services.IAclService;
import org.pentaho.di.ui.repository.pur.services.IConnectionAclService;
import org.pentaho.di.ui.repository.pur.services.ILockService;
import org.pentaho.di.ui.repository.pur.services.IRevisionService;
import org.pentaho.di.ui.repository.pur.services.IRoleSupportSecurityManager;
import org.pentaho.di.ui.repository.pur.services.ITrashService;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.repository2.unified.webservices.jaxws.IUnifiedRepositoryJaxwsWebService;
import org.pentaho.platform.repository2.unified.webservices.jaxws.UnifiedRepositoryToWebServiceAdapter;

import com.pentaho.pdi.ws.IRepositorySyncWebService;
import com.pentaho.pdi.ws.RepositorySyncException;
import com.sun.xml.ws.client.ClientTransportException;

public class PurRepositoryConnector implements IRepositoryConnector {
  private static final String SINGLE_DI_SERVER_INSTANCE = "singleDiServerInstance";
  private static final String REMOTE_DI_SERVER_INSTANCE = "remoteDiServerInstance";
  private static Class<?> PKG = PurRepository.class;
  private final LogChannelInterface log;
  private final PurRepository purRepository;
  private final PurRepositoryMeta repositoryMeta;
  private final RootRef rootRef;
  private ServiceManager serviceManager;

  public PurRepositoryConnector( PurRepository purRepository, PurRepositoryMeta repositoryMeta, RootRef rootRef ) {
    log = new LogChannel( this.getClass().getSimpleName() );
    if ( purRepository != null & purRepository.getLog() != null ) {
      log.setLogLevel( purRepository.getLog().getLogLevel() );
    }
    this.purRepository = purRepository;
    this.repositoryMeta = repositoryMeta;
    this.rootRef = rootRef;
  }

  private boolean allowedActionsContains( AbsSecurityProvider provider, String action ) throws KettleException {
    List<String> allowedActions = provider.getAllowedActions( IAbsSecurityProvider.NAMESPACE );
    for ( String actionName : allowedActions ) {
      if ( action != null && action.equals( actionName ) ) {
        return true;
      }
    }
    return false;
  }

  public synchronized RepositoryConnectResult connect( final String username, final String password )
    throws KettleException, KettleSecurityException {
    if ( serviceManager != null ) {
      disconnect();
    }
    serviceManager = new WebServiceManager( repositoryMeta.getRepositoryLocation().getUrl(), username );
    RepositoryServiceRegistry purRepositoryServiceRegistry = new RepositoryServiceRegistry();
    IUser user1 = new EEUserInfo();
    final String decryptedPassword = Encr.decryptPasswordOptionallyEncrypted( password );
    final RepositoryConnectResult result = new RepositoryConnectResult( purRepositoryServiceRegistry );
    try {
      /*
       * Three scenarios: 1. Connect in process: username fetched using PentahoSessionHolder; no authentication occurs
       * 2. Connect externally with trust: username specified is assumed authenticated if IP of calling code is trusted
       * 3. Connect externally: authentication occurs normally (i.e. password is checked)
       */
      user1.setLogin( username );
      user1.setPassword( decryptedPassword );
      user1.setName( username );
      result.setUser( user1 );

      // We need to have the application context and the session available in order for us to skip authentication
      if ( PentahoSystem.getApplicationContext() != null && PentahoSessionHolder.getSession() != null ) {
        if ( inProcess() ) {
          // connect to the IUnifiedRepository through PentahoSystem
          // this assumes we're running in a BI Platform
          if ( log.isDebug() ) {
            log.logDebug( "begin connectInProcess()" );
          }
          String name = PentahoSessionHolder.getSession().getName();
          user1 = new EEUserInfo();
          user1.setLogin( name );
          user1.setName( name );
          user1.setPassword( decryptedPassword );
          result.setUnifiedRepository( PentahoSystem.get( IUnifiedRepository.class ) );
          result.setUser( user1 );
          result.setSuccess( true );

          if ( log.isDebug() ) {
            log.logDebug( "connected in process as '" + name + "' pur repository = " + result.getUnifiedRepository() );
          }

          // for now, there is no need to support the security manager
          // what about security provider?
          return result;
        }
      }

      ExecutorService executor = ExecutorUtil.getExecutor();

      Future<Boolean> authorizationWebserviceFuture = executor.submit( new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          // We need to add the service class in the list in the order of dependencies
          // IRoleSupportSecurityManager depends RepositorySecurityManager to be present
          if ( log.isBasic() ) {
            log.logBasic( BaseMessages.getString( PKG, "PurRepositoryConnector.CreateServiceProvider.Start" ) );
          }
          result.setSecurityProvider( new AbsSecurityProvider( purRepository, repositoryMeta, result.getUser(),
              serviceManager ) );
          if ( log.isBasic() ) {
            log.logBasic( BaseMessages.getString( PKG, "PurRepositoryConnector.CreateServiceProvider.End" ) ); //$NON-NLS-1$
          }
          // If the user does not have access to administer security we do not
          // need to added them to the service list
          if ( allowedActionsContains( (AbsSecurityProvider) result.getSecurityProvider(),
              IAbsSecurityProvider.ADMINISTER_SECURITY_ACTION ) ) {
            result.setSecurityManager( new AbsSecurityManager( purRepository, repositoryMeta, result.getUser(),
                serviceManager ) );
            // Set the reference of the security manager to security provider for user role list change event
            ( (PurRepositorySecurityProvider) result.getSecurityProvider() )
                .setUserRoleDelegate( ( (PurRepositorySecurityManager) result.getSecurityManager() )
                    .getUserRoleDelegate() );
            return true;
          }
          return false;
        }
      } );

      Future<WebServiceException> repoWebServiceFuture = executor.submit( new Callable<WebServiceException>() {

        @Override
        public WebServiceException call() throws Exception {
          try {
            IUnifiedRepositoryJaxwsWebService repoWebService = null;
            if ( log.isBasic() ) {
              log.logBasic( BaseMessages.getString( PKG, "PurRepositoryConnector.CreateRepositoryWebService.Start" ) ); //$NON-NLS-1$
            }
            repoWebService =
                serviceManager.createService( username, decryptedPassword, IUnifiedRepositoryJaxwsWebService.class ); //$NON-NLS-1$
            if ( log.isBasic() ) {
              log.logBasic( BaseMessages.getString( PKG, "PurRepositoryConnector.CreateRepositoryWebService.End" ) ); //$NON-NLS-1$
            }
            if ( log.isBasic() ) {
              log.logBasic( BaseMessages.getString( PKG, "PurRepositoryConnector.CreateUnifiedRepositoryToWebServiceAdapter.Start" ) ); //$NON-NLS-1$
            }
            result.setUnifiedRepository( new UnifiedRepositoryToWebServiceAdapter( repoWebService ) );
          } catch ( WebServiceException wse ) {
            return wse;
          }
          return null;
        }
      } );

      Future<Exception> syncWebserviceFuture = executor.submit( new Callable<Exception>() {

        @Override
        public Exception call() throws Exception {
          try {
            if ( log.isBasic() ) {
              log.logBasic( BaseMessages.getString( PKG, "PurRepositoryConnector.CreateRepositorySyncWebService.Start" ) );
            }
            IRepositorySyncWebService syncWebService =
                serviceManager.createService( username, decryptedPassword, IRepositorySyncWebService.class ); //$NON-NLS-1$
            if ( log.isBasic() ) {
              log.logBasic( BaseMessages.getString( PKG, "PurRepositoryConnector.CreateRepositorySyncWebService.Sync" ) ); //$NON-NLS-1$
            }
            syncWebService.sync( repositoryMeta.getName(), repositoryMeta.getRepositoryLocation().getUrl() );
          } catch ( RepositorySyncException e ) {
            log.logError( e.getMessage(), e );
            // this message will be presented to the user in spoon
            result.setConnectMessage( e.getMessage() );
            return null;
          } catch ( ClientTransportException e ) {
            // caused by authentication errors, etc
            return e;
          } catch ( WebServiceException e ) {
            // if we can speak to the repository okay but not the sync service, assume we're talking to a BA Server
            log.logError( e.getMessage(), e );
            return new Exception( BaseMessages.getString( PKG, "PurRepository.BAServerLogin.Message" ), e );
          }
          return null;
        }
      } );

      WebServiceException repoException = repoWebServiceFuture.get();
      if ( repoException != null ) {
        log.logError( repoException.getMessage() );
        throw new Exception( BaseMessages.getString( PKG, "PurRepository.FailedLogin.Message" ), repoException );
      }

      Exception syncException = syncWebserviceFuture.get();
      if ( syncException != null ) {
        throw syncException;
      }

      Boolean isAdmin = authorizationWebserviceFuture.get();

      if ( log.isBasic() ) {
        log.logBasic( BaseMessages.getString( PKG, "PurRepositoryConnector.RegisterSecurityProvider.Start" ) );
      }
      purRepositoryServiceRegistry.registerService( RepositorySecurityProvider.class, result.getSecurityProvider() );
      purRepositoryServiceRegistry.registerService( IAbsSecurityProvider.class, result.getSecurityProvider() );
      if ( isAdmin ) {
        purRepositoryServiceRegistry.registerService( RepositorySecurityManager.class, result.getSecurityManager() );
        purRepositoryServiceRegistry.registerService( IRoleSupportSecurityManager.class, result.getSecurityManager() );
        purRepositoryServiceRegistry.registerService( IAbsSecurityManager.class, result.getSecurityManager() );
      }

      purRepositoryServiceRegistry.registerService( PurRepositoryRestService.PurRepositoryPluginApiRevision.class,
          serviceManager.createService( username, decryptedPassword,
              PurRepositoryRestService.PurRepositoryPluginApiRevision.class ) );

      purRepositoryServiceRegistry.registerService( IRevisionService.class, new UnifiedRepositoryRevisionService(
          result.getUnifiedRepository(), rootRef ) );
      purRepositoryServiceRegistry.registerService( IAclService.class, new UnifiedRepositoryConnectionAclService(
          result.getUnifiedRepository() ) );
      purRepositoryServiceRegistry.registerService( IConnectionAclService.class,
          new UnifiedRepositoryConnectionAclService( result.getUnifiedRepository() ) );
      purRepositoryServiceRegistry.registerService( ITrashService.class, new UnifiedRepositoryTrashService( result
          .getUnifiedRepository(), rootRef ) );
      purRepositoryServiceRegistry.registerService( ILockService.class, new UnifiedRepositoryLockService( result
          .getUnifiedRepository() ) );

      if ( log.isBasic() ) {
        log.logBasic( BaseMessages.getString( PKG, "PurRepositoryConnector.RepositoryServicesRegistered.End" ) );
      }

      result.setSuccess( true );
    } catch ( NullPointerException npe ) {
      result.setSuccess( false );
      throw new KettleException( BaseMessages.getString( PKG, "PurRepository.LoginException.Message" ) );
    } catch ( Throwable e ) {
      result.setSuccess( false );
      serviceManager.close();
      throw new KettleException( e );
    }
    return result;
  }

  @Override
  public synchronized void disconnect() {
    if ( serviceManager != null ) {
      serviceManager.close();
    }
    serviceManager = null;
  }

  public LogChannelInterface getLog() {
    return log;
  }

  @Override
  public ServiceManager getServiceManager() {
    return serviceManager;
  }

  public static boolean inProcess() {
    boolean inProcess = false;
    boolean remoteDiServer =
        BooleanUtils.toBoolean( PentahoSystem.getSystemSetting( REMOTE_DI_SERVER_INSTANCE, "false" ) ); //$NON-NLS-1$
    if ( "true".equals( PentahoSystem.getSystemSetting( SINGLE_DI_SERVER_INSTANCE, "true" ) ) ) { //$NON-NLS-1$ //$NON-NLS-2$
      inProcess = true;
    } else if ( !remoteDiServer && PentahoSystem.getApplicationContext().getFullyQualifiedServerURL() != null ) {
      inProcess = true;
    }
    return inProcess;
  }
}
