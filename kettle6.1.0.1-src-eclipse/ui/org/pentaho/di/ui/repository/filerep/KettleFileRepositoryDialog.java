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

package org.pentaho.di.ui.repository.filerep;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.RepositoriesMeta;
import org.pentaho.di.repository.RepositoryMeta;
import org.pentaho.di.repository.filerep.KettleFileRepositoryMeta;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.repository.dialog.RepositoryDialogInterface;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

public class KettleFileRepositoryDialog implements RepositoryDialogInterface {
  private static Class<?> PKG = RepositoryDialogInterface.class; // for i18n purposes, needed by Translator2!!

  private MODE mode;
  private Label wlBaseDir;

  private Button wbBaseDir;

  private Text wBaseDir;

  private FormData fdlBaseDir, fdBaseDir, fdbBaseDir;

  private Label wlReadOnly;
  private Button wReadOnly;
  private FormData fdlReadOnly, fdReadOnly;

  private Label wlHidesHiddenFiles;
  private Button wHidesHiddenFiles;
  private FormData fdlHidesHiddenFiles, fdHidesHiddenFiles;

  private Label wlId;

  private Text wId;

  private FormData fdlId, fdId;

  private Label wlName;

  private Text wName;

  private FormData fdlName, fdName;

  private Button wOK, wCancel;

  private Listener lsOK, lsCancel;

  private Display display;

  private Shell shell;

  private PropsUI props;

  private KettleFileRepositoryMeta input;
  // private RepositoriesMeta repositories;
  private RepositoriesMeta masterRepositoriesMeta;

  private String masterRepositoryName;

  public KettleFileRepositoryDialog( Shell parent, int style, RepositoryMeta repositoryMeta,
    RepositoriesMeta repositoriesMeta ) {
    this.display = parent.getDisplay();
    this.props = PropsUI.getInstance();
    this.input = (KettleFileRepositoryMeta) repositoryMeta;
    this.masterRepositoriesMeta = repositoriesMeta.clone();
    this.masterRepositoryName = repositoryMeta.getName();

    // this.repositories = repositoriesMeta;
    shell = new Shell( parent, style | SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
    shell.setText( BaseMessages.getString( PKG, "KettleFileRepositoryDialog.Dialog.Main.Title" ) );
  }

  public KettleFileRepositoryMeta open( final MODE mode ) {
    this.mode = mode;
    props.setLook( shell );

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );
    shell.setImage( GUIResource.getInstance().getImageSpoon() );
    shell.setText( BaseMessages.getString( PKG, "KettleFileRepositoryDialog.Dialog.Main.Title2" ) );

    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

    // Add the connection buttons :
    wbBaseDir = new Button( shell, SWT.PUSH );
    wbBaseDir.setText( BaseMessages.getString( PKG, "System.Button.Browse" ) );

    fdbBaseDir = new FormData();
    fdbBaseDir.right = new FormAttachment( 100, 0 );
    fdbBaseDir.top = new FormAttachment( 0, margin );
    wbBaseDir.setLayoutData( fdbBaseDir );

    wBaseDir = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wBaseDir );
    fdBaseDir = new FormData();
    fdBaseDir.left = new FormAttachment( middle, 0 );
    fdBaseDir.top = new FormAttachment( wbBaseDir, 0, SWT.CENTER );
    fdBaseDir.right = new FormAttachment( wbBaseDir, -margin );
    wBaseDir.setLayoutData( fdBaseDir );

    // Base directory line
    wlBaseDir = new Label( shell, SWT.RIGHT );
    wlBaseDir.setText( BaseMessages.getString( PKG, "KettleFileRepositoryDialog.Label.BaseDirectory" ) );
    props.setLook( wlBaseDir );
    fdlBaseDir = new FormData();
    fdlBaseDir.left = new FormAttachment( 0, 0 );
    fdlBaseDir.right = new FormAttachment( middle, -margin );
    fdlBaseDir.top = new FormAttachment( wbBaseDir, 0, SWT.CENTER );
    wlBaseDir.setLayoutData( fdlBaseDir );

    // ReadOnly line
    wlReadOnly = new Label( shell, SWT.RIGHT );
    wlReadOnly.setText( BaseMessages.getString( PKG, "KettleFileRepositoryDialog.Label.ReadOnly" ) );
    props.setLook( wlReadOnly );
    fdlReadOnly = new FormData();
    fdlReadOnly.left = new FormAttachment( 0, 0 );
    fdlReadOnly.top = new FormAttachment( wBaseDir, margin );
    fdlReadOnly.right = new FormAttachment( middle, -margin );
    wlReadOnly.setLayoutData( fdlReadOnly );
    wReadOnly = new Button( shell, SWT.CHECK );
    props.setLook( wReadOnly );
    fdReadOnly = new FormData();
    fdReadOnly.left = new FormAttachment( middle, 0 );
    fdReadOnly.top = new FormAttachment( wBaseDir, margin );
    fdReadOnly.right = new FormAttachment( 100, 0 );
    wReadOnly.setLayoutData( fdReadOnly );

    // HidesHiddenFiles line
    wlHidesHiddenFiles = new Label( shell, SWT.RIGHT );
    wlHidesHiddenFiles
      .setText( BaseMessages.getString( PKG, "KettleFileRepositoryDialog.Label.HidesHiddenFiles" ) );
    props.setLook( wlHidesHiddenFiles );
    fdlHidesHiddenFiles = new FormData();
    fdlHidesHiddenFiles.left = new FormAttachment( 0, 0 );
    fdlHidesHiddenFiles.top = new FormAttachment( wReadOnly, margin );
    fdlHidesHiddenFiles.right = new FormAttachment( middle, -margin );
    wlHidesHiddenFiles.setLayoutData( fdlHidesHiddenFiles );
    wHidesHiddenFiles = new Button( shell, SWT.CHECK );
    props.setLook( wHidesHiddenFiles );
    fdHidesHiddenFiles = new FormData();
    fdHidesHiddenFiles.left = new FormAttachment( middle, 0 );
    fdHidesHiddenFiles.top = new FormAttachment( wReadOnly, margin );
    fdHidesHiddenFiles.right = new FormAttachment( 100, 0 );
    wHidesHiddenFiles.setLayoutData( fdHidesHiddenFiles );

    // Add the listeners
    // New connection
    wbBaseDir.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent arg0 ) {
        DirectoryDialog dialog = new DirectoryDialog( shell, SWT.NONE );
        dialog.setText( "Select root directory" );
        dialog.setMessage( "Select the repository root directory" );
        String folder = dialog.open();
        if ( folder != null ) {
          wBaseDir.setText( folder );
        }
      }
    } );

    // Name line
    wlName = new Label( shell, SWT.RIGHT );
    wlName.setText( BaseMessages.getString( PKG, "KettleFileRepositoryDialog.Label.Name" ) );
    props.setLook( wlName );
    fdlName = new FormData();
    fdlName.left = new FormAttachment( 0, 0 );
    fdlName.top = new FormAttachment( wHidesHiddenFiles, margin * 2 );
    fdlName.right = new FormAttachment( middle, -margin );
    wlName.setLayoutData( fdlName );
    wName = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wName );
    fdName = new FormData();
    fdName.left = new FormAttachment( middle, 0 );
    fdName.top = new FormAttachment( wHidesHiddenFiles, margin * 2 );
    fdName.right = new FormAttachment( 100, 0 );
    wName.setLayoutData( fdName );

    // Description line
    wlId = new Label( shell, SWT.RIGHT );
    wlId.setText( BaseMessages.getString( PKG, "KettleFileRepositoryDialog.Label.Description" ) );
    props.setLook( wlId );
    fdlId = new FormData();
    fdlId.left = new FormAttachment( 0, 0 );
    fdlId.top = new FormAttachment( wlName, margin * 3 );
    fdlId.right = new FormAttachment( middle, -margin );
    wlId.setLayoutData( fdlId );
    wId = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wId );
    fdId = new FormData();
    fdId.left = new FormAttachment( middle, 0 );
    fdId.top = new FormAttachment( wlName, margin * 3 );
    fdId.right = new FormAttachment( 100, 0 );
    wId.setLayoutData( fdId );

    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    lsOK = new Listener() {
      public void handleEvent( Event e ) {
        ok();
      }
    };
    wOK.addListener( SWT.Selection, lsOK );

    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );
    lsCancel = new Listener() {
      public void handleEvent( Event e ) {
        cancel();
      }
    };
    wCancel.addListener( SWT.Selection, lsCancel );

    BaseStepDialog.positionBottomButtons( shell, new Button[] { wOK, wCancel }, margin, wlId );
    // Detect X or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );

    getData();

    BaseStepDialog.setSize( shell );

    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
    return input;
  }

  public void dispose() {
    props.setScreen( new WindowProperty( shell ) );
    shell.dispose();
  }

  /**
   * Copy information from the meta-data input to the dialog fields.
   */
  public void getData() {
    wName.setText( Const.NVL( input.getName(), "" ) );
    wId.setText( Const.NVL( input.getDescription(), "" ) );
    wBaseDir.setText( Const.NVL( input.getBaseDirectory(), "" ) );
    wReadOnly.setSelection( input.isReadOnly() );
    wHidesHiddenFiles.setSelection( input.isHidingHiddenFiles() );
  }

  private void cancel() {
    input = null;
    dispose();
  }

  private void getInfo( KettleFileRepositoryMeta info ) {
    info.setName( wName.getText() );
    info.setDescription( wId.getText() );
    info.setBaseDirectory( wBaseDir.getText() );
    info.setReadOnly( wReadOnly.getSelection() );
    info.setHidingHiddenFiles( wHidesHiddenFiles.getSelection() );
  }

  private void ok() {
    getInfo( input );
    if ( input.getBaseDirectory() != null && input.getBaseDirectory().length() > 0 ) {
      if ( input.getName() != null && input.getName().length() > 0 ) {
        if ( input.getDescription() != null && input.getDescription().length() > 0 ) {
          // If MODE is ADD then check if the repository name does not exist in the repository list then close this
          // dialog
          // If MODE is EDIT then check if the repository name is the same as before if not check if the new name does
          // not exist in the repository. Otherwise return true to this method, which will mean that repository already
          // exist
          if ( mode == MODE.ADD ) {
            if ( masterRepositoriesMeta.searchRepository( input.getName() ) == null ) {
              dispose();
            } else {
              displayRepositoryAlreadyExistMessage( input.getName() );
            }
          } else {
            if ( masterRepositoryName.equals( input.getName() ) ) {
              dispose();
            } else if ( masterRepositoriesMeta.searchRepository( input.getName() ) == null ) {
              dispose();
            } else {
              displayRepositoryAlreadyExistMessage( input.getName() );
            }
          }
        } else {
          MessageBox box = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
          box.setMessage( BaseMessages.getString( PKG, "KettleFileRepositoryDialog.Dialog.ErrorNoName.Message" ) );
          box.setText( BaseMessages.getString( PKG, "KettleFileRepositoryDialog.Dialog.ErrorNoName.Title" ) );
          box.open();
        }
      } else {
        MessageBox box = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
        box.setMessage( BaseMessages.getString( PKG, "KettleFileRepositoryDialog.Dialog.ErrorNoId.Message" ) );
        box.setText( BaseMessages.getString( PKG, "KettleFileRepositoryDialog.Dialog.ErrorNoId.Title" ) );
        box.open();
      }
    } else {
      MessageBox box = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
      box.setMessage( BaseMessages.getString( PKG, "KettleFileRepositoryDialog.Dialog.ErrorNoBaseDir.Message" ) );
      box.setText( BaseMessages.getString( PKG, "KettleFileRepositoryDialog.Dialog.ErrorNoBaseDir.Title" ) );
      box.open();
    }
  }

  private void displayRepositoryAlreadyExistMessage( String name ) {
    MessageBox box = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
    box.setMessage( BaseMessages.getString( PKG, "RepositoryDialog.Dialog.ErrorIdExist.Message", name ) );
    box.setText( BaseMessages.getString( PKG, "RepositoryDialog.Dialog.Error.Title" ) );
    box.open();
  }
}
