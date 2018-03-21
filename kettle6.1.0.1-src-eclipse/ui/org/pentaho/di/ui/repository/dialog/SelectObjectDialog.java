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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.RepositoryElementMetaInterface;
import org.pentaho.di.repository.RepositoryExtended;
import org.pentaho.di.repository.RepositoryObjectType;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.repository.RepositoryDirectoryUI;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import java.util.regex.Pattern;

/**
 * Allows the user to make a selection of an Object in the repository
 *
 * @author Matt
 * @since 19-06-2003
 */
public class SelectObjectDialog extends Dialog {
  private static Class<?> PKG = RepositoryDialogInterface.class; // for
  // i18n
  // purposes,
  // needed
  // by
  // Translator2!!

  private Label wlTree;
  private Tree wTree;
  private FormData fdlTree, fdTree, fdexpandAll;

  private Button wOK, wCancel;
  private Listener lsOK, lsCancel;

  private Shell shell;
  private SelectionAdapter lsDef;

  private String shellText;
  private String lineText;
  private PropsUI props;

  private Color dircolor;
  private Repository rep;

  private String objectName;
  private RepositoryDirectoryInterface objectDirectory;
  private TreeColumn nameColumn;
  private TreeColumn userColumn;
  private TreeColumn changedColumn;
  private int sortColumn;
  private boolean ascending;
  private TreeColumn typeColumn;
  private RepositoryObjectType objectType;
  private boolean showTrans;
  private boolean showJobs;
  private TreeColumn descriptionColumn;

  private ToolItem expandAll, collapseAll, goSearch, wfilter;

  private String filterString = null;
  private Text searchText = null;
  private Pattern pattern = null;

  private RepositoryDirectoryInterface directoryTree;

  // private RepositoryCapabilities capabilities;

  private boolean includeDeleted;

  private ToolItem wbRegex;

  private RepositoryElementMetaInterface repositoryObject;

  private ObjectId objectId;

  public SelectObjectDialog( Shell parent, Repository rep ) {
    this( parent, rep, true, true );
  }

  public SelectObjectDialog( Shell parent, Repository rep, boolean showTransformations, boolean showJobs ) {
    super( parent, SWT.NONE );

    this.props = PropsUI.getInstance();
    this.rep = rep;
    this.showTrans = showTransformations;
    this.showJobs = showJobs;

    // TODO: make this a configuration option in the dialog!
    //
    this.includeDeleted = false;

    shellText = BaseMessages.getString( PKG, "SelectObjectDialog.Dialog.Main.Title" );
    lineText = BaseMessages.getString( PKG, "SelectObjectDialog.Dialog.Object.Title" );
    objectName = null;
    objectDirectory = null;

    sortColumn = 0;
    ascending = false;

  }

  public String open() {
    Shell parent = getParent();
    dircolor = GUIResource.getInstance().getColorDirectory();

    shell =
        new Shell( parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.SHEET | SWT.RESIZE | SWT.MIN | SWT.MAX );
    props.setLook( shell );
    shell.setImage( GUIResource.getInstance().getImageSpoon() );

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );
    shell.setText( shellText );

    int margin = Const.MARGIN;

    ToolBar treeTb = new ToolBar( shell, SWT.HORIZONTAL | SWT.FLAT );
    props.setLook( treeTb );

    wfilter = new ToolItem( treeTb, SWT.SEPARATOR );
    searchText = new Text( treeTb, SWT.SEARCH | SWT.CANCEL );
    searchText.setToolTipText( BaseMessages
        .getString( PKG, "RepositoryExplorerDialog.Search.FilterString.ToolTip" ) );
    wfilter.setControl( searchText );
    wfilter.setWidth( 100 );

    wbRegex = new ToolItem( treeTb, SWT.CHECK );
    wbRegex.setImage( GUIResource.getInstance().getImageRegexSmall() );
    wbRegex.setToolTipText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Search.UseRegex" ) );

    goSearch = new ToolItem( treeTb, SWT.PUSH );
    goSearch.setImage( GUIResource.getInstance().getImageSearchSmall() );
    goSearch.setToolTipText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Search.Run" ) );

    expandAll = new ToolItem( treeTb, SWT.PUSH );
    expandAll.setImage( GUIResource.getInstance().getImageExpandAll() );
    collapseAll = new ToolItem( treeTb, SWT.PUSH );
    collapseAll.setImage( GUIResource.getInstance().getImageCollapseAll() );
    fdexpandAll = new FormData();
    fdexpandAll.right = new FormAttachment( 100, -margin );
    fdexpandAll.top = new FormAttachment( 0, margin );
    treeTb.setLayoutData( fdexpandAll );

    // From step line
    wlTree = new Label( shell, SWT.NONE );
    wlTree.setText( lineText );
    props.setLook( wlTree );
    fdlTree = new FormData();
    fdlTree.left = new FormAttachment( 0, 0 );
    fdlTree.top = new FormAttachment( 0, margin );
    wlTree.setLayoutData( fdlTree );

    wTree = new Tree( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
    wTree.setHeaderVisible( true );
    props.setLook( wTree );

    // Add some columns to it as well...
    nameColumn = new TreeColumn( wTree, SWT.LEFT );
    nameColumn.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Column.Name" ) );
    nameColumn.setWidth( 350 );
    nameColumn.addListener( SWT.Selection, new Listener() {
      public void handleEvent( Event e ) {
        setSort( 0 );
      }
    } );

    // No sorting on the type column just yet.
    typeColumn = new TreeColumn( wTree, SWT.LEFT );
    typeColumn.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Column.Type" ) );
    typeColumn.setWidth( 100 );
    typeColumn.addListener( SWT.Selection, new Listener() {
      public void handleEvent( Event e ) {
        setSort( 1 );
      }
    } );

    userColumn = new TreeColumn( wTree, SWT.LEFT );
    userColumn.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Column.User" ) );
    userColumn.setWidth( 100 );
    userColumn.addListener( SWT.Selection, new Listener() {
      public void handleEvent( Event e ) {
        setSort( 2 );
      }
    } );

    changedColumn = new TreeColumn( wTree, SWT.LEFT );
    changedColumn.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Column.Changed" ) );
    changedColumn.setWidth( 120 );
    changedColumn.addListener( SWT.Selection, new Listener() {
      public void handleEvent( Event e ) {
        setSort( 3 );
      }
    } );

    descriptionColumn = new TreeColumn( wTree, SWT.LEFT );
    descriptionColumn.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Column.Description" ) );
    descriptionColumn.setWidth( 120 );
    descriptionColumn.addListener( SWT.Selection, new Listener() {
      public void handleEvent( Event e ) {
        setSort( 4 );
      }
    } );

    props.setLook( wTree );
    fdTree = new FormData();
    fdTree.left = new FormAttachment( 0, 0 );
    fdTree.right = new FormAttachment( 100, 0 );
    fdTree.top = new FormAttachment( treeTb, margin );
    fdTree.bottom = new FormAttachment( 100, -30 );
    wTree.setLayoutData( fdTree );

    // Some buttons
    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    lsOK = new Listener() {
      public void handleEvent( Event e ) {
        ok();
      }
    };
    wOK.addListener( SWT.Selection, lsOK );
    wOK.setEnabled( false );

    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );
    lsCancel = new Listener() {
      public void handleEvent( Event e ) {
        cancel();
      }
    };
    wCancel.addListener( SWT.Selection, lsCancel );

    BaseStepDialog.positionBottomButtons( shell, new Button[] { wOK, wCancel }, margin, null );
    // Add listeners

    lsDef = new SelectionAdapter() {
      public void widgetDefaultSelected( SelectionEvent e ) {
        ok();
      }
    };
    wTree.addSelectionListener( lsDef );
    wTree.addKeyListener( new KeyAdapter() {
      public void keyPressed( KeyEvent e ) {
        if ( e.character == SWT.CR ) {
          ok();
        }
      }
    } );

    wTree.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        wOK.setEnabled( !Boolean.TRUE.equals( wTree.getSelection()[ 0 ].getData( "isFolder" ) ) );
      }
    } );

    expandAll.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent event ) {
        expandAllItems( wTree.getItems(), true );
      }
    } );

    collapseAll.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent event ) {
        expandAllItems( wTree.getItems(), false );
      }
    } );
    goSearch.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent event ) {
        updateFilter();
      }
    } );

    searchText.addSelectionListener( new SelectionAdapter() {
      public void widgetDefaultSelected( SelectionEvent e ) {
        updateFilter();
      }
    } );
    // Detect [X] or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );

    try {

      // We're terrible and load the entire repository, disable lazy loading if set
      if ( rep instanceof RepositoryExtended ) {
        RepositoryExtended repositoryExtended = (RepositoryExtended) this.rep;
        directoryTree = repositoryExtended.loadRepositoryDirectoryTree( true );
      } else {
        directoryTree = this.rep.loadRepositoryDirectoryTree();
      }

    } catch ( KettleException e ) {
      new ErrorDialog( shell,
          BaseMessages.getString( PKG, "SelectObjectDialog.Dialog.ErrorRefreshingDirectoryTree.Title" ),
          BaseMessages.getString( PKG, "SelectObjectDialog.Dialog.ErrorRefreshingDirectoryTree.Message" ), e );
    } // catch()

    getData();
    wTree.setFocus();
    BaseStepDialog.setSize( shell );

    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !shell.getDisplay().readAndDispatch() ) {
        shell.getDisplay().sleep();
      }
    }
    return objectName;
  }

  private void expandAllItems( TreeItem[] treeitems, boolean expand ) {
    for ( TreeItem item : treeitems ) {
      item.setExpanded( expand );
      if ( item.getItemCount() > 0 ) {
        expandAllItems( item.getItems(), expand );
      }
    }
  }

  protected void updateFilter() {
    pattern = null;
    filterString = null;
    if ( searchText != null && !searchText.isDisposed() && !Const.isEmpty( searchText.getText() ) ) {
      if ( wbRegex.getSelection() ) {
        pattern = Pattern.compile( searchText.getText() );
      } else {
        filterString = searchText.getText().toUpperCase();
      }
    }
    refreshTree();
    if ( ( wbRegex.getSelection() && pattern != null ) || ( !wbRegex.getSelection() && filterString != null ) ) {
      while ( getNrEmptyFolders( wTree.getItems() ) > 0 ) {
        removeEmptyFolders( wTree.getItems() );
        try {
          Thread.sleep( 0, 1 );
        } catch ( InterruptedException e ) {
          // Ignore errors
        }
      }
      expandAllItems( wTree.getItems(), true );
    }
  }

  private void removeEmptyFolders( TreeItem[] treeitems ) {
    for ( TreeItem item : treeitems ) {
      if ( item.getImage().equals( GUIResource.getInstance().getImageArrow() ) && item.getItemCount() == 0 ) {
        item.dispose();
      } else {
        removeEmptyFolders( item.getItems() );
      }
    }
  }

  private int getNrEmptyFolders( TreeItem[] treeitems ) {
    int retval = 0;
    for ( TreeItem item : treeitems ) {
      if ( item.getImage().equals( GUIResource.getInstance().getImageArrow() ) && item.getItemCount() == 0 ) {
        retval++;
      } else {
        retval += getNrEmptyFolders( item.getItems() );
      }
    }
    return retval;
  }

  protected void setSort( int i ) {
    if ( sortColumn == i ) {
      ascending = !ascending;
    } else {
      sortColumn = i;
      ascending = true;
    }

    if ( sortColumn >= 0 && sortColumn < 5 ) {
      TreeColumn column = wTree.getColumn( sortColumn );
      wTree.setSortColumn( column );
      wTree.setSortDirection( ascending ? SWT.UP : SWT.DOWN );
    }
    refreshTree();
  }

  private void refreshTree() {
    try {
      wTree.removeAll();
      TreeItem ti = null;
      // If the directory is a root directory and is visible to the user we will
      // display that on the UI otherwise we will hide it
      if ( directoryTree.isRoot() && directoryTree.isVisible() ) {
        ti = new TreeItem( wTree, SWT.NONE );
        ti.setImage( GUIResource.getInstance().getImageFolder() );
        ti.setExpanded( true );
        RepositoryDirectoryUI.getTreeWithNames(
            ti, rep, dircolor, sortColumn, includeDeleted, ascending, showTrans, showJobs, directoryTree,
            filterString, pattern );
      } else {
        for ( int i = 0; i < directoryTree.getNrSubdirectories(); i++ ) {
          RepositoryDirectory subdir = directoryTree.getSubdirectory( i );
          ti = new TreeItem( wTree, SWT.NONE );
          ti.setImage( GUIResource.getInstance().getImageFolder() );
          RepositoryDirectoryUI.getTreeWithNames(
              ti, rep, dircolor, sortColumn, includeDeleted, ascending, showTrans, showJobs, subdir, filterString,
              pattern );
        }
      }
    } catch ( KettleException e ) {
      new ErrorDialog(
          shell, BaseMessages.getString( PKG, "SelectObjectDialog.Dialog.UnexpectedError.Title" ), BaseMessages
          .getString( PKG, "SelectObjectDialog.Dialog.UnexpectedError.Message" ), e );
    }
  }

  public void dispose() {
    props.setScreen( new WindowProperty( shell ) );
    shell.dispose();
  }

  public void getData() {
    setSort( 0 );
  }

  private void cancel() {
    objectName = null;
    dispose();
  }

  private void ok() {
    // Something has to be selected!
    if ( wTree.getSelectionCount() > 0 ) {
      TreeItem ti = wTree.getSelection()[ 0 ];

      // No directory!
      if ( !Boolean.TRUE.equals( wTree.getSelection()[ 0 ].getData( "isFolder" ) ) ) {
        int level = ConstUI.getTreeLevel( ti );
        if ( level > 0 ) {
          repositoryObject = (RepositoryElementMetaInterface) ti.getData();
          if ( repositoryObject != null ) {
            objectName = repositoryObject.getName();
            objectDirectory = repositoryObject.getRepositoryDirectory();
            objectId = repositoryObject.getObjectId();
            objectType = repositoryObject.getObjectType();
          } else {
            // For backward compatibility, we should rarely end up here...
            //
            String[] path = ConstUI.getTreeStrings( ti.getParentItem() );
            objectName = ti.getText( 0 );
            objectType = null;
            for ( RepositoryObjectType type : RepositoryObjectType.values() ) {
              if ( type.getTypeDescription().equalsIgnoreCase( ti.getText( 1 ) ) ) {
                objectType = type;
                break;
              }
            }
            objectDirectory = directoryTree.findDirectory( path );
          }

          if ( objectDirectory != null ) {
            dispose();
          } else {
            MessageBox mb = new MessageBox( shell, SWT.OK | SWT.ICON_ERROR );
            mb.setMessage( BaseMessages.getString( PKG, "SelectObjectDialog.Dialog.DirectoryNotFound.Message" ) );
            mb.setText( BaseMessages.getString( PKG, "SelectObjectDialog.Dialog.DirectoryNotFound.Title" ) );
            mb.open();
          }
        }
      }
    }
  }

  public RepositoryDirectoryInterface getDirectory() {
    return objectDirectory;
  }

  /**
   * @return the objectType
   */
  public RepositoryObjectType getObjectType() {
    return objectType;
  }

  /**
   * @return the objectName
   */
  public String getObjectName() {
    return objectName;
  }

  /**
   * @return the repositoryObject
   */
  public RepositoryElementMetaInterface getRepositoryObject() {
    return repositoryObject;
  }

  /**
   * @param repositoryObject the repositoryObject to set
   */
  public void setRepositoryObject( RepositoryElementMetaInterface repositoryObject ) {
    this.repositoryObject = repositoryObject;
  }

  /**
   * @return the objectId
   */
  public ObjectId getObjectId() {
    return objectId;
  }

  /**
   * @param objectId the objectId to set
   */
  public void setObjectId( ObjectId objectId ) {
    this.objectId = objectId;
  }
}
