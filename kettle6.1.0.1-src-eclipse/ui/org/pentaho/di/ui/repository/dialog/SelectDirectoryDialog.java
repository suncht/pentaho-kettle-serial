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

package org.pentaho.di.ui.repository.dialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.RepositoryExtended;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.dialog.EnterStringDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.repository.RepositoryDirectoryUI;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import java.util.Collections;
import java.util.Set;

/**
 * This dialog represents an explorer type of interface on a given database connection. It shows the tables defined in
 * the visible schemas or catalogs on that connection. The interface also allows you to get all kinds of information on
 * those tables.
 *
 * @author Matt
 * @since 18-05-2003
 */
public class SelectDirectoryDialog extends Dialog {
  private static Class<?> PKG = RepositoryDialogInterface.class; // for i18n purposes, needed by Translator2!!

  private PropsUI props;
  private Repository rep;
  private Shell shell;

  private Tree wTree;
  private TreeItem tiTree;
  private Button wOK;
  private Button wRefresh;
  private Button wCancel;
  private RepositoryDirectoryInterface selection;
  private Color dircolor;

  private RepositoryDirectoryInterface repositoryTree;

  private boolean readOnly;

  private Set<String> restrictedPaths = Collections.<String>emptySet();

  public void setRestrictedPaths( Set<String> restrictedPaths ) {
    this.restrictedPaths = restrictedPaths;
  }

  private boolean isRestrictedPath( String path ) {
    return restrictedPaths.contains( path );
  }

  public SelectDirectoryDialog( Shell parent, int style, Repository rep ) {
    super( parent, style );
    this.props = PropsUI.getInstance();
    this.rep = rep;

    selection = null;

    readOnly = rep.getSecurityProvider().isReadOnly();
  }

  public RepositoryDirectoryInterface open() {
    dircolor = GUIResource.getInstance().getColorDirectory();

    Shell parent = getParent();
    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
    props.setLook( shell );
    shell.setImage( GUIResource.getInstance().getImageSpoon() );
    shell.setText( BaseMessages.getString( PKG, "SelectDirectoryDialog.Dialog.Main.Title" ) );

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );

    // Tree
    wTree = new Tree( shell, SWT.SINGLE | SWT.BORDER );
    props.setLook( wTree );

    try {
      // We're terrible and load the entire repository, disable lazy loading if set
      if ( rep instanceof RepositoryExtended ) {
        RepositoryExtended repositoryExtended = (RepositoryExtended) this.rep;
        repositoryTree = repositoryExtended.loadRepositoryDirectoryTree( true );
      } else {
        repositoryTree = this.rep.loadRepositoryDirectoryTree();
      }
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
          BaseMessages.getString( PKG, "SelectDirectoryDialog.Dialog.ErrorRefreshingDirectoryTree.Title" ),
          BaseMessages.getString( PKG, "SelectDirectoryDialog.Dialog.ErrorRefreshingDirectoryTree.Message" ), e );
      return null;
    }

    if ( !getData() ) {
      return null;
    }

    // Buttons
    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );

    wRefresh = new Button( shell, SWT.PUSH );
    wRefresh.setText( BaseMessages.getString( PKG, "System.Button.Refresh" ) );

    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );

    FormData fdTree = new FormData();
    FormData fdOK = new FormData();
    FormData fdRefresh = new FormData();
    FormData fdCancel = new FormData();

    int margin = 10;

    fdTree.left = new FormAttachment( 0, 0 ); // To the right of the label
    fdTree.top = new FormAttachment( 0, 0 );
    fdTree.right = new FormAttachment( 100, 0 );
    fdTree.bottom = new FormAttachment( 100, -50 );
    wTree.setLayoutData( fdTree );

    fdOK.left = new FormAttachment( wTree, 0, SWT.CENTER );
    fdOK.bottom = new FormAttachment( 100, -margin );
    wOK.setLayoutData( fdOK );

    fdRefresh.left = new FormAttachment( wOK, 10 );
    fdRefresh.bottom = new FormAttachment( 100, -margin );
    wRefresh.setLayoutData( fdRefresh );

    fdCancel.left = new FormAttachment( wRefresh, 10 );
    fdCancel.bottom = new FormAttachment( 100, -margin );
    wCancel.setLayoutData( fdCancel );

    // Add listeners
    wCancel.addListener( SWT.Selection, new Listener() {
      public void handleEvent( Event e ) {
        dispose();
      }
    } );

    // Add listeners
    wOK.addListener( SWT.Selection, new Listener() {
      public void handleEvent( Event e ) {
        handleOK();
      }
    } );

    wTree.addSelectionListener( new SelectionAdapter() {
      private String getSelectedPath( SelectionEvent selectionEvent ) {
        TreeItem treeItem = (TreeItem) selectionEvent.item;
        String path;
        if ( treeItem.getParentItem() == null ) {
          path = treeItem.getText();
        } else {
          path = ConstUI.getTreePath( treeItem, 0 );
        }
        return path;
      }

      private boolean isSelectedPathRestricted( SelectionEvent selectionEvent ) {
        String path = getSelectedPath( selectionEvent );
        boolean isRestricted = isRestrictedPath( path );
        return isRestricted;
      }

      public void widgetDefaultSelected( SelectionEvent selectionEvent ) {
        if ( isSelectedPathRestricted( selectionEvent ) ) {
          return;
        }
        handleOK();
      }

      public void widgetSelected( SelectionEvent selectionEvent ) {
        boolean restricted = isSelectedPathRestricted( selectionEvent );
        wOK.setEnabled( !restricted );
      }
    } );

    wRefresh.addListener( SWT.Selection, new Listener() {
      public void handleEvent( Event e ) {
        getData();
      }
    } );

    wTree.addMenuDetectListener( new MenuDetectListener() {
      public void menuDetected( MenuDetectEvent e ) {
        setTreeMenu();
      }
    } );

    BaseStepDialog.setSize( shell );

    shell.open();
    Display display = parent.getDisplay();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
    return selection;
  }

  private boolean getData() {
    // Clear the tree top entry
    if ( tiTree != null && !tiTree.isDisposed() ) {
      tiTree.dispose();
    }

    tiTree = new TreeItem( wTree, SWT.NONE );
    tiTree.setImage( GUIResource.getInstance().getImageFolder() );
    RepositoryDirectoryUI.getDirectoryTree( tiTree, dircolor, repositoryTree );
    tiTree.setExpanded( true );

    return true;
  }

  public void setTreeMenu() {
    Menu mTree = null;

    TreeItem[] ti = wTree.getSelection(); // use SWT.SINGLE in wTree!!!!
    if ( ti.length == 1 ) {
      mTree = new Menu( wTree );

      /*
       * NEW Sub-directory
       */
      MenuItem miNew = new MenuItem( mTree, SWT.CASCADE );
      miNew.setText( BaseMessages.getString( PKG, "SelectDirectoryDialog.PopupMenu.Directory.New" ) );
      miNew.addSelectionListener( new SelectionAdapter() {
        public void widgetSelected( SelectionEvent e ) {
          if ( !readOnly ) {
            TreeItem ti = wTree.getSelection()[ 0 ];
            String[] str = ConstUI.getTreeStrings( ti );
            //
            // In which directory do we want create a subdirectory?
            //
            RepositoryDirectoryInterface dir = repositoryTree.findDirectory( str );
            if ( dir != null ) {
              //
              // What's the name of the new directory?
              //
              EnterStringDialog etd = new EnterStringDialog( shell,
                  BaseMessages.getString( PKG, "SelectDirectoryDialog.Dialog.EnterDirectoryName.Title" ),
                  BaseMessages.getString( PKG, "SelectDirectoryDialog.Dialog.EnterDirectoryName.Message" ),
                  BaseMessages.getString( PKG, "SelectDirectoryDialog.Dialog.EnterDirectoryName.Default" ) );
              String newdir = etd.open();
              if ( newdir != null ) {
                RepositoryDirectory subdir = new RepositoryDirectory( dir, newdir );
                try {
                  rep.saveRepositoryDirectory( subdir );
                  dir.addSubdirectory( subdir );
                  TreeItem tiNew = new TreeItem( ti, SWT.NONE );
                  tiNew.setText( newdir );
                  tiNew.setImage( GUIResource.getInstance().getImageFolder() );
                  wTree.setSelection( new TreeItem[] { tiNew } );
                } catch ( Exception exception ) {
                  new ErrorDialog( shell, BaseMessages.getString(
                      PKG, "SelectDirectoryDialog.Dialog.UnableToCreateDirectory.Message" ), BaseMessages
                      .getString( PKG, "SelectDirectoryDialog.Dialog.UnableToCreateDirectory.Title" ), exception );
                }
              }
            } else {
              MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
              mb.setMessage( BaseMessages.getString(
                  PKG, "SelectDirectoryDialog.Dialog.UnableToLocateDirectory.Message" ) );
              mb.setText( BaseMessages.getString(
                  PKG, "SelectDirectoryDialog.Dialog.UnableToLocateDirectory.Title" ) );
              mb.open();
            }
          } else {
            MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
            mb.setMessage( BaseMessages.getString( PKG, "SelectDirectoryDialog.Dialog.PermissionDenied.Message1" )
                + ( rep.getUserInfo() == null ? "" : rep.getUserInfo().getLogin() )
                + BaseMessages.getString( PKG, "SelectDirectoryDialog.Dialog.PermissionDenied.Message2" ) );
            mb.setText( BaseMessages.getString( PKG, "SelectDirectoryDialog.Dialog.PermissionDenied.Title" ) );
            mb.open();
          }
        }
      } );

      /*
       * RENAME directory
       */
      MenuItem miRen = new MenuItem( mTree, SWT.CASCADE );
      miRen.setText( BaseMessages.getString( PKG, "SelectDirectoryDialog.PopupMenu.Directory.Rename" ) );
      miRen.addSelectionListener( new SelectionAdapter() {
        public void widgetSelected( SelectionEvent e ) {
          if ( !readOnly ) {
            TreeItem ti = wTree.getSelection()[ 0 ];
            String[] str = ConstUI.getTreeStrings( ti );
            RepositoryDirectoryInterface dir = repositoryTree.findDirectory( str );
            if ( dir != null ) {
              //
              // What's the new name of the directory?
              //
              String oldName = dir.getName();
              EnterStringDialog etd = new EnterStringDialog( shell,
                  oldName,
                  BaseMessages.getString( PKG, "SelectDirectoryDialog.Dialog.EnterDirectoryNewName.Message" ),
                  BaseMessages.getString( PKG, "SelectDirectoryDialog.Dialog.EnterDirectoryNewName.Title" ) );
              String newName = etd.open();
              if ( newName != null && !newName.equals( oldName ) ) {
                dir.setName( newName );
                try {
                  rep.renameRepositoryDirectory( dir.getObjectId(), dir.getParent(), newName );
                  ti.setText( newName );
                  wTree.setSelection( ti );
                } catch ( Exception exception ) {
                  new ErrorDialog(
                      shell,
                      BaseMessages
                          .getString( PKG, "RepositoryExplorerDialog.Directory.Rename.UnexpectedError.Message1" )
                          + oldName + "]" + Const.CR
                          + BaseMessages
                          .getString( PKG, "RepositoryExplorerDialog.Directory.Rename.UnexpectedError.Message2" ),
                      BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Rename.UnexpectedError.Title" ),
                      exception );
                }
              }
            }
          }
        }
      } );

      MenuItem miDel = new MenuItem( mTree, SWT.CASCADE );
      miDel.setText( BaseMessages.getString( PKG, "SelectDirectoryDialog.PopupMenu.Directory.Delete" ) );
      miDel.addSelectionListener( new SelectionAdapter() {
        public void widgetSelected( SelectionEvent e ) {
          if ( !readOnly ) {
            TreeItem ti = wTree.getSelection()[ 0 ];
            String[] str = ConstUI.getTreeStrings( ti );
            RepositoryDirectoryInterface dir = repositoryTree.findDirectory( str );
            if ( dir != null ) {
              try {
                rep.deleteRepositoryDirectory( dir );
                ti.dispose();
              } catch ( KettleException exception ) {
                new ErrorDialog( shell,
                    BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Delete.ErrorRemoving.Title" ),
                    BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Delete.ErrorRemoving.Message1" ),
                    exception );
              }
            }
          }
        }
      } );

    }
    wTree.setMenu( mTree );
  }

  public void dispose() {
    props.setScreen( new WindowProperty( shell ) );
    shell.dispose();
  }

  public void handleOK() {
    TreeItem[] ti = wTree.getSelection();
    if ( ti.length == 1 ) {
      String[] tree = ConstUI.getTreeStrings( ti[ 0 ] );
      selection = repositoryTree.findDirectory( tree );
      dispose();
    }
  }
}
