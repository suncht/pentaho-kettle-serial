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

package org.pentaho.di.ui.repository.controllers;

import java.util.ResourceBundle;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.RepositoryMeta;
import org.pentaho.di.ui.repository.ILoginCallback;
import org.pentaho.di.ui.repository.RepositoriesHelper;
import org.pentaho.di.ui.repository.dialog.RepositoryDialogInterface;
import org.pentaho.di.ui.repository.model.RepositoriesModel;
import org.pentaho.di.ui.repository.repositoryexplorer.ControllerInitializationException;
import org.pentaho.ui.xul.XulException;
import org.pentaho.ui.xul.binding.Binding;
import org.pentaho.ui.xul.binding.BindingConvertor;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.components.WaitBoxRunnable;
import org.pentaho.ui.xul.components.XulButton;
import org.pentaho.ui.xul.components.XulCheckbox;
import org.pentaho.ui.xul.components.XulConfirmBox;
import org.pentaho.ui.xul.components.XulMessageBox;
import org.pentaho.ui.xul.components.XulTextbox;
import org.pentaho.ui.xul.components.XulWaitBox;
import org.pentaho.ui.xul.containers.XulDialog;
import org.pentaho.ui.xul.containers.XulListbox;
import org.pentaho.ui.xul.impl.AbstractXulEventHandler;

public class RepositoriesController extends AbstractXulEventHandler {

  private static Class<?> PKG = RepositoryDialogInterface.class; // for i18n purposes, needed by Translator2!!

  private ResourceBundle messages;

  private BindingFactory bf;

  private XulDialog loginDialog;

  private XulTextbox username;

  private XulTextbox userPassword;

  private XulListbox availableRepositories;

  private XulButton repositoryEditButton;

  private XulButton repositoryRemoveButton;

  private XulCheckbox showAtStartup;

  private RepositoriesModel loginModel;

  private XulButton okButton;

  private XulButton cancelButton;

  private XulMessageBox messageBox;

  protected XulConfirmBox confirmBox;

  private RepositoriesHelper helper;
  private String preferredRepositoryName;
  private ILoginCallback callback;

  private Shell shell;

  public RepositoriesController() {
    super();
    loginModel = new RepositoriesModel();
  }

  public void init() throws ControllerInitializationException {
    // TODO Initialize the Repository Login Dialog
    try {
      messageBox = (XulMessageBox) document.createElement( "messagebox" );
      confirmBox = (XulConfirmBox) document.createElement( "confirmbox" );
    } catch ( Exception e ) {
      throw new ControllerInitializationException( e );
    }
    if ( bf != null ) {
      createBindings();
    }
  }

  private void createBindings() {
    loginDialog = (XulDialog) document.getElementById( "repository-login-dialog" );

    repositoryEditButton = (XulButton) document.getElementById( "repository-edit" );
    repositoryRemoveButton = (XulButton) document.getElementById( "repository-remove" );

    username = (XulTextbox) document.getElementById( "user-name" );
    userPassword = (XulTextbox) document.getElementById( "user-password" );
    availableRepositories = (XulListbox) document.getElementById( "available-repository-list" );
    showAtStartup = (XulCheckbox) document.getElementById( "show-login-dialog-at-startup" );
    okButton = (XulButton) document.getElementById( "repository-login-dialog_accept" );
    cancelButton = (XulButton) document.getElementById( "repository-login-dialog_cancel" );
    bf.setBindingType( Binding.Type.BI_DIRECTIONAL );
    bf.createBinding( loginModel, "username", username, "value" );
    bf.createBinding( loginModel, "password", userPassword, "value" );
    bf.createBinding( loginModel, "availableRepositories", availableRepositories, "elements" );
    bf.createBinding( loginModel, "selectedRepository", availableRepositories, "selectedItem" );
    bf.createBinding( loginModel, "showDialogAtStartup", showAtStartup, "checked" );
    bf.setBindingType( Binding.Type.ONE_WAY );
    bf.createBinding( loginModel, "valid", okButton, "!disabled" );

    BindingConvertor<RepositoryMeta, Boolean> buttonConverter = new BindingConvertor<RepositoryMeta, Boolean>() {
      @Override
      public Boolean sourceToTarget( RepositoryMeta value ) {
        return ( value == null );
      }

      @Override
      public RepositoryMeta targetToSource( Boolean value ) {
        return null;
      }
    };

    BindingConvertor<RepositoryMeta, Boolean> userpassConverter = new BindingConvertor<RepositoryMeta, Boolean>() {
      @Override
      public Boolean sourceToTarget( RepositoryMeta value ) {
        return ( value == null ) || !value.getRepositoryCapabilities().supportsUsers();
      }

      @Override
      public RepositoryMeta targetToSource( Boolean value ) {
        return null;
      }
    };

    bf.createBinding( loginModel, "selectedRepository", username, "disabled", userpassConverter );
    bf.createBinding( loginModel, "selectedRepository", userPassword, "disabled", userpassConverter );

    bf.createBinding( loginModel, "selectedRepository", repositoryEditButton, "disabled", buttonConverter );
    bf.createBinding( loginModel, "selectedRepository", repositoryRemoveButton, "disabled", buttonConverter );

    final Shell loginShell = (Shell) loginDialog.getRootObject();

    helper = new RepositoriesHelper( loginModel, document, loginShell );
    helper.setPreferredRepositoryName( preferredRepositoryName );
    helper.getMetaData();
  }

  public void setBindingFactory( BindingFactory bf ) {
    this.bf = bf;
  }

  public BindingFactory getBindingFactory() {
    return this.bf;
  }

  public String getName() {
    return "repositoryLoginController";
  }

  public void show() {
    if ( loginModel.getUsername() != null ) {
      userPassword.setFocus();
    } else {
      username.setFocus();
    }

    // PDI-7443: The repo list does not show the selected repo
    // make the layout play nice, this is necessary to have the selection box scroll reliably
    if ( availableRepositories.getRows() < 4 ) {
      availableRepositories.setRows( 4 );
    }

    int idx = loginModel.getRepositoryIndex( loginModel.getSelectedRepository() );
    if ( idx >= 0 ) {
      availableRepositories.setSelectedIndex( idx );
    }
    // END OF PDI-7443

    loginDialog.show();

  }

  public void login() {
    if ( loginModel.isValid() == false ) {
      return;
    }
    XulWaitBox box;
    try {
      box = (XulWaitBox) document.createElement( "waitbox" );
      box.setIndeterminate( true );
      box.setCanCancel( false );
      box.setTitle( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Connection.Wait.Title" ) );
      box.setMessage( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Connection.Wait.Message" ) );
      final Shell loginShell = (Shell) loginDialog.getRootObject();
      final Display display = loginShell.getDisplay();
      box.setDialogParent( loginShell );
      box.setRunnable( new WaitBoxRunnable( box ) {
        @Override
        public void run() {
          try {
            helper.loginToRepository();

            waitBox.stop();
            display.syncExec( new Runnable() {
              public void run() {
                loginDialog.hide();
                okButton.setDisabled( false );
                cancelButton.setDisabled( false );

                if ( helper.getConnectedRepository().getConnectMessage() != null ) {
                  getMessageBox().setTitle( BaseMessages.getString( PKG, "ConnectMessageTitle" ) );
                  getMessageBox().setMessage( helper.getConnectedRepository().getConnectMessage() );
                  getMessageBox().open();
                }

                getCallback().onSuccess( helper.getConnectedRepository() );
              }
            } );

          } catch ( final Throwable th ) {

            waitBox.stop();

            try {
              display.syncExec( new Runnable() {
                public void run() {

                  getCallback().onError( th );
                  okButton.setDisabled( false );
                  cancelButton.setDisabled( false );
                }
              } );
            } catch ( Exception e ) {
              e.printStackTrace();
            }

          }
        }

        @Override
        public void cancel() {
        }

      } );
      okButton.setDisabled( true );
      cancelButton.setDisabled( true );
      box.start();
    } catch ( XulException e1 ) {
      getCallback().onError( e1 );
    }
  }

  /**
   * Executed when the user clicks the new repository image from the Repository Login Dialog It present a new dialog
   * where the user can selected what type of repository to create
   */
  public void newRepository() {
    helper.newRepository();
  }

  /**
   * Executed when the user clicks the edit repository image from the Repository Login Dialog It presents an edit dialog
   * where the user can edit information about the currently selected repository
   */

  public void editRepository() {
    helper.editRepository();
  }

  /**
   * Executed when the user clicks the delete repository image from the Repository Login Dialog It prompts the user with
   * a warning about the action to be performed and upon the approval of this action from the user, the selected
   * repository is deleted
   */

  public void deleteRepository() {
    helper.deleteRepository();
  }

  /**
   * Executed when user clicks cancel button on the Repository Login Dialog
   */
  public void closeRepositoryLoginDialog() {
    loginDialog.hide();
    getCallback().onCancel();
  }

  /**
   * Executed when the user checks or uncheck the "show this dialog at startup checkbox" It saves the current selection.
   */
  public void updateShowDialogAtStartup() {
    helper.updateShowDialogOnStartup( showAtStartup.isChecked() );
  }

  public XulMessageBox getMessageBox() {
    return messageBox;
  }

  public void setMessageBox( XulMessageBox messageBox ) {
    this.messageBox = messageBox;
  }

  public void setMessages( ResourceBundle messages ) {
    this.messages = messages;
  }

  public ResourceBundle getMessages() {
    return messages;
  }

  public String getPreferredRepositoryName() {
    return preferredRepositoryName;
  }

  public void setPreferredRepositoryName( String preferredRepositoryName ) {
    this.preferredRepositoryName = preferredRepositoryName;
  }

  public void setCallback( ILoginCallback callback ) {
    this.callback = callback;
  }

  public ILoginCallback getCallback() {
    return callback;
  }

  public void setShell( final Shell shell ) {
    this.shell = shell;
  }

  public Shell getShell() {
    return shell;
  }
}
