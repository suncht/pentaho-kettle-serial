// CHECKSTYLE:FileLength:OFF
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.pentaho.di.cluster.ClusterSchema;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.RepositoryPluginType;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.partition.PartitionSchema;
import org.pentaho.di.repository.IUser;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryCapabilities;
import org.pentaho.di.repository.RepositoryDirectory;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.RepositoryElementInterface;
import org.pentaho.di.repository.RepositoryElementMetaInterface;
import org.pentaho.di.repository.RepositoryMeta;
import org.pentaho.di.repository.RepositoryObjectType;
import org.pentaho.di.repository.RepositorySecurityManager;
import org.pentaho.di.repository.RepositorySecurityProvider;
import org.pentaho.di.repository.UserInfo;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.ui.cluster.dialog.ClusterSchemaDialog;
import org.pentaho.di.ui.cluster.dialog.SlaveServerDialog;
import org.pentaho.di.ui.core.ConstUI;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.database.dialog.DatabaseDialog;
import org.pentaho.di.ui.core.dialog.EnterStringDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.core.widget.DoubleClickInterface;
import org.pentaho.di.ui.core.widget.TreeItemAccelerator;
import org.pentaho.di.ui.core.widget.TreeMemory;
import org.pentaho.di.ui.partition.dialog.PartitionSchemaDialog;
import org.pentaho.di.ui.repository.RepositoryDirectoryUI;
import org.pentaho.di.ui.repository.RepositorySecurityUI;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.w3c.dom.Document;

/**
 * This dialog displays the content of the repository in a Windows explorer like fashion. The user can manipulate the
 * objects in the repository as well as administer users, profiles, etc.
 *
 * @author Matt
 * @since 18-mei-2003
 *
 */
public class RepositoryExplorerDialog extends Dialog {
  private static Class<?> PKG = RepositoryExplorerDialog.class; // for i18n purposes, needed by Translator2!!

  private static final String REPOSITORY_PKG = "org.pentaho.di.ui.repository";

  public interface RepositoryExplorerCallback {
    /**
     * request that specified object be opened in 'Spoon' display
     *
     * @param object
     * @return boolean indicating if repository explorer dialog should close
     */
    boolean open( RepositoryObjectReference object );
  }

  /**
   * capture a reference to an object in the repository
   *
   * @author jgoldman
   *
   */
  public class RepositoryObjectReference {
    private RepositoryObjectType type; // Type of object
    private RepositoryDirectoryInterface directory; // The directory to which it belongs.
    private String name; // name of object being referenced
    private String versionLabel; // the version to be loaded

    public RepositoryObjectReference( RepositoryObjectType type, RepositoryDirectoryInterface dir, String name ) {
      this( type, dir, name, null );
    }

    public RepositoryObjectReference( RepositoryObjectType type, RepositoryDirectoryInterface dir, String name,
      String versionLabel ) {
      this.type = type;
      this.directory = dir;
      this.name = name;
      this.versionLabel = versionLabel;
    }

    public RepositoryDirectoryInterface getDirectory() {
      return directory;
    }

    public String getName() {
      return name;
    }

    public RepositoryObjectType getType() {
      return type;
    }

    public String getVersionLabel() {
      return versionLabel;
    }
  }

  private static final String STRING_DATABASES = BaseMessages.getString(
    PKG, "RepositoryExplorerDialog.Tree.String.Connections" );
  private static final String STRING_PARTITIONS = BaseMessages.getString(
    PKG, "RepositoryExplorerDialog.Tree.String.Partitions" );
  private static final String STRING_SLAVES = BaseMessages.getString(
    PKG, "RepositoryExplorerDialog.Tree.String.Slaves" );
  private static final String STRING_CLUSTERS = BaseMessages.getString(
    PKG, "RepositoryExplorerDialog.Tree.String.Clusters" );
  public static final String STRING_TRANSFORMATIONS = BaseMessages.getString(
    PKG, "RepositoryExplorerDialog.Tree.String.Transformations" );
  public static final String STRING_JOBS = BaseMessages.getString(
    PKG, "RepositoryExplorerDialog.Tree.String.Jobs" );
  private static final String STRING_USERS = BaseMessages.getString(
    PKG, "RepositoryExplorerDialog.Tree.String.Users" );

  private static final int ITEM_CATEGORY_NONE = 0;
  private static final int ITEM_CATEGORY_ROOT = 1;
  private static final int ITEM_CATEGORY_DATABASES_ROOT = 2;
  private static final int ITEM_CATEGORY_DATABASE = 3;
  private static final int ITEM_CATEGORY_TRANSFORMATIONS_ROOT = 4;
  private static final int ITEM_CATEGORY_TRANSFORMATION = 5;
  private static final int ITEM_CATEGORY_TRANSFORMATION_DIRECTORY = 6;
  private static final int ITEM_CATEGORY_JOBS_ROOT = 7;
  private static final int ITEM_CATEGORY_JOB = 8;
  private static final int ITEM_CATEGORY_JOB_DIRECTORY = 9;
  private static final int ITEM_CATEGORY_USERS_ROOT = 10;
  private static final int ITEM_CATEGORY_USER = 11;
  private static final int ITEM_CATEGORY_PARTITIONS_ROOT = 14;
  private static final int ITEM_CATEGORY_PARTITION = 15;
  private static final int ITEM_CATEGORY_SLAVES_ROOT = 16;
  private static final int ITEM_CATEGORY_SLAVE = 17;
  private static final int ITEM_CATEGORY_CLUSTERS_ROOT = 18;
  private static final int ITEM_CATEGORY_CLUSTER = 19;

  private Shell shell;
  private Tree wTree;
  private Button wOK;

  private LogChannelInterface log;
  private PropsUI props;
  private Repository rep;

  private String debug;

  private int sortColumn;
  private boolean ascending;
  private TreeColumn nameColumn;
  private TreeColumn typeColumn;
  private TreeColumn userColumn;
  private TreeColumn changedColumn;
  private TreeColumn descriptionColumn;
  private TreeColumn lockColumn;

  private RepositoryExplorerCallback callback;

  private RepositoryObjectReference lastOpened;
  private VariableSpace variableSpace;

  private ToolItem exportToXML, importFromXML, showHideDeleted, expandAll, collapseAll;

  private FormData fdTreeTb;
  private RepositoryDirectoryInterface directoryTree;
  private RepositoryMeta repositoryMeta;
  private RepositoryCapabilities capabilities;
  private boolean readonly;
  private RepositorySecurityProvider securityProvider;
  private RepositorySecurityManager securityManager;
  private boolean includeDeleted;
  private Map<String, RepositoryElementMetaInterface> objectMap;

  DatabaseDialog databaseDialog;

  private RepositoryExplorerDialog( Shell par, int style, Repository rep, VariableSpace variableSpace ) {
    super( par, style );
    this.props = PropsUI.getInstance();
    this.rep = rep;
    this.log = rep.getLog();
    this.variableSpace = variableSpace;

    sortColumn = 0;
    ascending = false;

    objectMap = new HashMap<String, RepositoryElementMetaInterface>();

    repositoryMeta = rep.getRepositoryMeta();
    capabilities = repositoryMeta.getRepositoryCapabilities();

    securityProvider = rep.getSecurityProvider();
    securityManager = rep.getSecurityManager();
    readonly = securityProvider.isReadOnly();

    includeDeleted = false;
  }

  public RepositoryExplorerDialog( Shell par, int style, Repository rep, RepositoryExplorerCallback callback,
    VariableSpace variableSpace ) {
    this( par, style, rep, variableSpace );
    this.callback = callback;
  }

  private static final String STRING_REPOSITORY_EXPLORER_TREE_NAME = "Repository Exporer Tree Name";

  private DatabaseDialog getDatabaseDialog() {
    if ( databaseDialog != null ) {
      return databaseDialog;
    }
    databaseDialog = new DatabaseDialog( shell );
    return databaseDialog;
  }

  public RepositoryObjectReference open() {
    debug = "opening repository explorer";

    try {
      debug = "open new independent shell";
      Shell parent = getParent();
      Display display = parent.getDisplay();
      shell = new Shell( display, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
      props.setLook( shell );
      shell.setImage( GUIResource.getInstance().getImageFolderConnections() );
      shell.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Title" ) + rep.getName() + "]" );

      FormLayout formLayout = new FormLayout();
      formLayout.marginWidth = Const.FORM_MARGIN;
      formLayout.marginHeight = Const.FORM_MARGIN;

      shell.setLayout( formLayout );

      // Add a small toolbar to expand/collapse all items
      //
      ToolBar treeTb = new ToolBar( shell, SWT.HORIZONTAL | SWT.FLAT );
      props.setLook( treeTb );
      fdTreeTb = new FormData();
      fdTreeTb.left = new FormAttachment( 0, 0 );
      fdTreeTb.top = new FormAttachment( 0, 0 );
      treeTb.setLayoutData( fdTreeTb );

      // Add the items...
      //
      exportToXML = new ToolItem( treeTb, SWT.PUSH );
      exportToXML.setImage( GUIResource.getInstance().getImageExport() );
      exportToXML.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.ToolItem.ExportToXML.Label" ) );
      exportToXML.setToolTipText( BaseMessages.getString(
        PKG, "RepositoryExplorerDialog.ToolItem.ExportToXML.Tooltip" ) );
      importFromXML = new ToolItem( treeTb, SWT.PUSH );
      importFromXML.setImage( GUIResource.getInstance().getImageImport() );
      importFromXML
        .setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.ToolItem.ImportFromXML.Label" ) );
      importFromXML.setToolTipText( BaseMessages.getString(
        PKG, "RepositoryExplorerDialog.ToolItem.ImportFromXML.Tooltip" ) );
      new ToolItem( treeTb, SWT.SEPARATOR );
      showHideDeleted = new ToolItem( treeTb, SWT.PUSH );
      showHideDeleted.setImage( GUIResource.getInstance().getImageShowDeleted() );
      showHideDeleted
        .setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.ToolItem.ShowDeleted.Label" ) );
      showHideDeleted.setToolTipText( BaseMessages.getString(
        PKG, "RepositoryExplorerDialog.ToolItem.ShowDeleted.Tooltip" ) );
      showHideDeleted.setEnabled( rep.getRepositoryMeta().getRepositoryCapabilities().supportsRevisions() );
      new ToolItem( treeTb, SWT.SEPARATOR );
      expandAll = new ToolItem( treeTb, SWT.PUSH );
      expandAll.setImage( GUIResource.getInstance().getImageExpandAll() );
      expandAll.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.ToolItem.ExpandAll.Label" ) );
      expandAll.setToolTipText( BaseMessages
        .getString( PKG, "RepositoryExplorerDialog.ToolItem.ExpandAll.Tooltip" ) );
      collapseAll = new ToolItem( treeTb, SWT.PUSH );
      collapseAll.setImage( GUIResource.getInstance().getImageCollapseAll() );
      collapseAll.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.ToolItem.CollapseAll.Label" ) );
      collapseAll.setToolTipText( BaseMessages.getString(
        PKG, "RepositoryExplorerDialog.ToolItem.CollapseAll.Tooltip" ) );

      // Tree
      wTree = new Tree( shell, SWT.MULTI | SWT.BORDER );
      wTree.setHeaderVisible( true );
      props.setLook( wTree );

      // Add some columns to it as well...
      nameColumn = new TreeColumn( wTree, SWT.LEFT );
      nameColumn.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Column.Name" ) );
      nameColumn.setWidth( 350 );
      nameColumn.setAlignment( 10 );
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

      lockColumn = new TreeColumn( wTree, SWT.LEFT );
      lockColumn.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Column.LockMessage" ) );
      lockColumn.setWidth( 120 );
      lockColumn.addListener( SWT.Selection, new Listener() {
        public void handleEvent( Event e ) {
          setSort( 5 );
        }
      } );

      // Add a memory to the tree.
      TreeMemory.addTreeListener( wTree, STRING_REPOSITORY_EXPLORER_TREE_NAME );

      // Buttons
      wOK = new Button( shell, SWT.PUSH );
      wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );

      FormData fdTree = new FormData();
      int margin = 10;

      fdTree.left = new FormAttachment( 0, 0 ); // To the right of the label
      fdTree.top = new FormAttachment( treeTb, 0 );
      fdTree.right = new FormAttachment( 100, 0 );
      fdTree.bottom = new FormAttachment( 100, -50 );
      wTree.setLayoutData( fdTree );

      BaseStepDialog.positionBottomButtons( shell, new Button[] { wOK, }, margin, null );

      // Add listeners
      wOK.addListener( SWT.Selection, new Listener() {
        public void handleEvent( Event e ) {
          close();
        }
      } );

      wTree.addMenuDetectListener( new MenuDetectListener() {
        public void menuDetected( MenuDetectEvent e ) {
          setTreeMenu();
        }
      } );

      wTree.addMouseListener( new MouseAdapter() {
        public void mouseDoubleClick( MouseEvent e ) {
          if ( e.button == 1 ) { // left double click!

            doDoubleClick();
          }
        }
      } );

      wTree.addKeyListener( new KeyAdapter() {
        public void keyPressed( KeyEvent e ) {
          // F2 --> rename...
          if ( e.keyCode == SWT.F2 ) {
            if ( !readonly ) {
              renameInTree();
            }
          }
          // F5 --> refresh...
          if ( e.keyCode == SWT.F5 ) {
            refreshTree();
          }
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

      importFromXML.addSelectionListener( new SelectionAdapter() {
        public void widgetSelected( SelectionEvent event ) {
          importAll();
        }
      } );

      exportToXML.addSelectionListener( new SelectionAdapter() {
        public void widgetSelected( SelectionEvent event ) {
          exportAll( null );
        }
      } );

      showHideDeleted.addSelectionListener( new SelectionAdapter() {
        public void widgetSelected( SelectionEvent event ) {
          includeDeleted = !includeDeleted;
          if ( includeDeleted ) {
            showHideDeleted.setText( BaseMessages.getString(
              PKG, "RepositoryExplorerDialog.ToolItem.HideDeleted.Label" ) );
            showHideDeleted.setToolTipText( BaseMessages.getString(
              PKG, "RepositoryExplorerDialog.ToolItem.HideDeleted.Tooltip" ) );
          } else {
            showHideDeleted.setText( BaseMessages.getString(
              PKG, "RepositoryExplorerDialog.ToolItem.ShowDeleted.Label" ) );
            showHideDeleted.setToolTipText( BaseMessages.getString(
              PKG, "RepositoryExplorerDialog.ToolItem.ShowDeleted.Tooltip" ) );
          }
          refreshTree();
        }
      } );

      // Drag & Drop
      Transfer[] ttypes = new Transfer[] { TextTransfer.getInstance() };

      DragSource ddSource = new DragSource( wTree, DND.DROP_MOVE );

      ddSource.setTransfer( ttypes );
      ddSource.addDragListener( new DragSourceListener() {
        public void dragStart( DragSourceEvent event ) {
          debug = "drag start";
          event.doit = true;
        }

        public void dragSetData( DragSourceEvent event ) {
          debug = "drag set data";

          event.data = "";
          event.doit = false;

          TreeItem[] ti = wTree.getSelection();
          if ( ti.length >= 1 ) {
            int cat = getItemCategory( ti[0] );
            //
            // Drag around a transformation...
            //
            if ( cat == ITEM_CATEGORY_TRANSFORMATION ) {
              debug = "drag set: drag around transformation";
              RepositoryDirectoryInterface repdir = getDirectory( ti[0] );
              if ( repdir != null ) {
                //
                // Pass info as a piece of XML
                //
                String xml = XMLHandler.getXMLHeader();
                xml += "<dragdrop>" + Const.CR;
                xml += "  " + XMLHandler.addTagValue( "directory", repdir.getPath() );
                xml += "  " + XMLHandler.addTagValue( "transformation", ti[0].getText() );
                xml += "</dragdrop>" + Const.CR;

                event.data = xml;
                event.doit = true;
              }
            } else if ( cat == ITEM_CATEGORY_JOB ) {
              debug = "drag set: drag around job";
              RepositoryDirectoryInterface repdir = getDirectory( ti[0] );
              if ( repdir != null ) {
                //
                // Pass info as a piece of XML
                //
                String xml = XMLHandler.getXMLHeader();
                xml += "<dragdrop>" + Const.CR;
                xml += "  " + XMLHandler.addTagValue( "directory", repdir.getPath() );
                xml += "  " + XMLHandler.addTagValue( "job", ti[0].getText() );
                xml += "</dragdrop>" + Const.CR;

                event.data = xml;
                event.doit = true;
              }
            } else {
              debug = "do nothing";
              String xml = XMLHandler.getXMLHeader();
              xml += "<dragdrop>" + Const.CR;
              xml += "</dragdrop>" + Const.CR;
              event.data = xml;
              event.doit = true;
            }
          }
        }

        public void dragFinished( DragSourceEvent event ) {
        }
      } );

      DropTarget ddTarget = new DropTarget( wTree, DND.DROP_MOVE );
      ddTarget.setTransfer( ttypes );
      ddTarget.addDropListener( new DropTargetListener() {
        public void dragEnter( DropTargetEvent event ) {
        }

        public void dragLeave( DropTargetEvent event ) {
          debug = "drag leave";
        }

        public void dragOperationChanged( DropTargetEvent event ) {
        }

        public void dragOver( DropTargetEvent event ) {
          debug = "drag over";
        }

        public void drop( DropTargetEvent event ) {
          try {
            debug = "Drop item in tree";

            if ( event.data == null ) // no data to copy, indicate failure in event.detail
            {
              event.detail = DND.DROP_NONE;
              return;
            }

            // event.feedback = DND.FEEDBACK_NONE;

            TreeItem ti = (TreeItem) event.item;
            if ( ti != null ) {
              debug = "Get category";
              int category = getItemCategory( ti );

              if ( category == ITEM_CATEGORY_TRANSFORMATION_DIRECTORY || category == ITEM_CATEGORY_TRANSFORMATION ) {
                debug = "Get directory";
                RepositoryDirectoryInterface repdir = getDirectory( ti );
                if ( repdir != null ) {
                  event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;

                  if ( moveTransformation( (String) event.data, repdir ) ) {
                    refreshTree();
                  } else {
                    MessageBox mb = new MessageBox( shell, SWT.ICON_INFORMATION | SWT.OK );
                    mb.setMessage( BaseMessages.getString(
                      PKG, "RepositoryExplorerDialog.Trans.Move.UnableToMove.Message" ) );
                    mb.setText( BaseMessages.getString(
                      PKG, "RepositoryExplorerDialog.Trans.Move.UnableToMove.Title" ) );
                    mb.open();
                  }
                }
              } else if ( category == ITEM_CATEGORY_JOB_DIRECTORY || category == ITEM_CATEGORY_JOB ) {
                debug = "Get directory";
                RepositoryDirectoryInterface repdir = getDirectory( ti );
                if ( repdir != null ) {
                  event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;

                  if ( moveJob( (String) event.data, repdir ) ) {
                    refreshTree();
                  } else {
                    MessageBox mb = new MessageBox( shell, SWT.ICON_INFORMATION | SWT.OK );
                    mb.setMessage( BaseMessages.getString(
                      PKG, "RepositoryExplorerDialog.Job.Move.UnableToMove.Message" ) );
                    mb.setText( BaseMessages.getString(
                      PKG, "RepositoryExplorerDialog.Job.Move.UnableToMove.Title" ) );
                    mb.open();
                  }
                }
              } else {
                MessageBox mb = new MessageBox( shell, SWT.ICON_INFORMATION | SWT.OK );
                mb.setMessage( BaseMessages.getString(
                  PKG, "RepositoryExplorerDialog.Trans.Move.SorryOneItemAtATime.Message" ) );
                mb.setText( BaseMessages.getString(
                  PKG, "RepositoryExplorerDialog.Trans.Move.SorryOneItemAtATime.Title" ) );
                mb.open();
              }
            }
          } catch ( Throwable e ) {
            new ErrorDialog(
              shell,
              BaseMessages.getString( PKG, "RepositoryExplorerDialog.Drop.UnexpectedError.Title" ), BaseMessages
                .getString( PKG, "RepositoryExplorerDialog.Drop.UnexpectedError.Message1" )
                + debug
                + "]"
                + Const.CR
                + BaseMessages.getString( PKG, "RepositoryExplorerDialog.Drop.UnexpectedError.Message2" ), e );
          }
        }

        public void dropAccept( DropTargetEvent event ) {
          debug = "drop accept";
        }
      } );

      // Detect X or ALT-F4 or something that kills this window...
      shell.addShellListener( new ShellAdapter() {
        public void shellClosed( ShellEvent e ) {
          close();
        }
      } );

      debug = "set screen size and position";

      BaseStepDialog.setSize( shell, 400, 480, true );

      setSort( 0 ); // refreshes too.

      shell.open();
      while ( !shell.isDisposed() ) {
        if ( !display.readAndDispatch() ) {
          display.sleep();
        }
      }
    } catch ( Throwable e ) {
      new ErrorDialog(
        shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Main.UnexpectedError.Title" ), BaseMessages.getString(
          PKG, "RepositoryExplorerDialog.Main.UnexpectedError.Message1" )
          + debug
          + "]"
          + Const.CR
          + BaseMessages.getString( PKG, "RepositoryExplorerDialog.Main.UnexpectedError.Message2" )
          + Const.CR
          + BaseMessages.getString( PKG, "RepositoryExplorerDialog.Main.UnexpectedError.Message3" ), e );
    }
    return lastOpened;

  }

  private void expandAllItems( TreeItem[] treeitems, boolean expand ) {
    for ( TreeItem item : treeitems ) {
      item.setExpanded( expand );
      TreeMemory.getInstance().storeExpanded(
        STRING_REPOSITORY_EXPLORER_TREE_NAME, ConstUI.getTreeStrings( item ), expand );
      if ( item.getItemCount() > 0 ) {
        expandAllItems( item.getItems(), expand );
      }
    }

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

  public RepositoryDirectoryInterface getDirectory( TreeItem ti ) {
    RepositoryDirectoryInterface repdir = null;

    int level = ConstUI.getTreeLevel( ti );
    String[] path = ConstUI.getTreeStrings( ti );

    if ( level > 1 ) {
      int cat = getItemCategory( ti );
      String[] realpath;
      switch ( cat ) {
        case ITEM_CATEGORY_JOB:
        case ITEM_CATEGORY_TRANSFORMATION:
          // The first 3 levels of text[] don't belong to the path to this transformation!
          realpath = new String[level - 2];
          for ( int i = 0; i < realpath.length; i++ ) {
            realpath[i] = path[i + 2];
          }

          repdir = directoryTree.findDirectory( realpath );

          break;
        case ITEM_CATEGORY_JOB_DIRECTORY:
        case ITEM_CATEGORY_TRANSFORMATION_DIRECTORY:
          // The first 3 levels of text[] don't belong to the path to this transformation!
          realpath = new String[level - 1];
          for ( int i = 0; i < realpath.length; i++ ) {
            realpath[i] = path[i + 2];
          }

          repdir = directoryTree.findDirectory( realpath );

          break;
        default:
          break;
      }
    }
    return repdir;
  }

  public boolean sameCategory( TreeItem[] tisel ) {
    if ( tisel.length == 0 ) {
      return false;
    }

    int cat = getItemCategory( tisel[0] );
    for ( int i = 1; i < tisel.length; i++ ) {
      if ( cat != getItemCategory( tisel[i] ) ) {
        return false;
      }
    }

    return true;
  }

  public void doDoubleClick() {
    final TreeItem[] tisel = wTree.getSelection();
    if ( tisel.length == 1 || sameCategory( tisel ) ) {
      final TreeItem ti = tisel[0];
      final int level = ConstUI.getTreeLevel( ti );

      int cat = getItemCategory( ti );
      if ( ( level >= 2 )
        && ( ( cat == ITEM_CATEGORY_JOB_DIRECTORY )
          || ( cat == ITEM_CATEGORY_TRANSFORMATION_DIRECTORY )
          || ( cat == ITEM_CATEGORY_JOB )
          || ( cat == ITEM_CATEGORY_TRANSFORMATION ) ) ) {
        String[] realpath;
        if ( ( cat == ITEM_CATEGORY_JOB_DIRECTORY ) || ( cat == ITEM_CATEGORY_TRANSFORMATION_DIRECTORY ) ) {
          // The first levels of path[] don't belong to the path to this directory!
          realpath = new String[level - 1];
        } else {
          // The first 3 levels of path[] don't belong to the path to this transformation or Job!
          realpath = new String[level - 2];
        }

        final String[] path = ConstUI.getTreeStrings( ti );
        for ( int i = 0; i < realpath.length; i++ ) {
          realpath[i] = path[i + 2];
        }
        // Find the directory in the directory tree...
        final RepositoryDirectoryInterface repdir = directoryTree.findDirectory( realpath );

        switch ( cat ) {
          case ITEM_CATEGORY_JOB_DIRECTORY:
          case ITEM_CATEGORY_TRANSFORMATION_DIRECTORY: {
            if ( !readonly ) {
              createDirectory( ti, repdir );
            }
            break;
          }
          case ITEM_CATEGORY_TRANSFORMATION: {
            openTransformation( ti.getText(), repdir );
            break;
          }
          case ITEM_CATEGORY_JOB: {
            openJob( ti.getText(), repdir );
            break;
          }
          default:
        }

      }
    }
  }

  public void setTreeMenu() {
    Menu mTree = new Menu( shell, SWT.POP_UP );

    final TreeItem[] tisel = wTree.getSelection();
    if ( tisel.length == 1 || sameCategory( tisel ) ) {
      final TreeItem ti = tisel[0];
      final int level = ConstUI.getTreeLevel( ti );
      final String[] path = ConstUI.getTreeStrings( ti );
      final String fullPath = ConstUI.getTreePath( ti, 0 );
      final String item = ti.getText();

      final RepositoryElementMetaInterface repositoryObject = objectMap.get( fullPath );

      int cat = getItemCategory( ti );

      switch ( cat ) {
        case ITEM_CATEGORY_ROOT:
          // Export all
          MenuItem miExp = new MenuItem( mTree, SWT.PUSH );
          miExp.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Objects.ExportAll" ) );
          SelectionAdapter lsExp = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              exportAll( directoryTree );
            }
          };
          miExp.addSelectionListener( lsExp );

          // Import all
          MenuItem miImp = new MenuItem( mTree, SWT.PUSH );
          miImp.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Objects.ImportAll" ) );
          SelectionAdapter lsImp = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              importAll();
            }
          };
          miImp.addSelectionListener( lsImp );
          miImp.setEnabled( !readonly );

          // Export transMeta
          MenuItem miTrans = new MenuItem( mTree, SWT.PUSH );
          miTrans
            .setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Objects.ExportTrans" ) );
          SelectionAdapter lsTrans = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              exportTransformations( directoryTree );
            }
          };
          miTrans.addSelectionListener( lsTrans );

          // Export jobs
          MenuItem miJobs = new MenuItem( mTree, SWT.PUSH );
          miJobs.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Objects.ExportJob" ) );
          SelectionAdapter lsJobs = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              exportJobs( directoryTree );
            }
          };
          miJobs.addSelectionListener( lsJobs );
          break;

        case ITEM_CATEGORY_DATABASES_ROOT:
          // New database
          MenuItem miNew = new MenuItem( mTree, SWT.PUSH );
          miNew.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.ConnectionsRoot.New" ) );
          SelectionAdapter lsNew = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              newDatabase();
            }
          };
          miNew.addSelectionListener( lsNew );
          miNew.setEnabled( !readonly );
          break;

        case ITEM_CATEGORY_DATABASE:
          // New database
          miNew = new MenuItem( mTree, SWT.PUSH );
          miNew.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Connections.New" ) );
          lsNew = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              newDatabase();
            }
          };
          miNew.addSelectionListener( lsNew );
          miNew.setEnabled( !readonly );
          // Edit database info
          MenuItem miEdit = new MenuItem( mTree, SWT.PUSH );
          miEdit.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Connections.Edit" ) );
          SelectionAdapter lsEdit = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              editDatabase( item );
            }
          };
          miEdit.addSelectionListener( lsEdit );
          miEdit.setEnabled( !readonly );
          // Delete database info
          MenuItem miDel = new MenuItem( mTree, SWT.PUSH );
          miDel.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Connections.Delete" ) );
          SelectionAdapter lsDel = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              delDatabase( item );
            }
          };
          miDel.addSelectionListener( lsDel );
          miDel.setEnabled( !readonly );
          break;

        case ITEM_CATEGORY_SLAVES_ROOT:
          // New slave
          miNew = new MenuItem( mTree, SWT.PUSH );
          miNew.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Slave.New" ) );
          lsNew = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              newSlaveServer();
            }
          };
          miNew.addSelectionListener( lsNew );
          miNew.setEnabled( !readonly );
          break;

        case ITEM_CATEGORY_SLAVE:
          // New slave
          miNew = new MenuItem( mTree, SWT.PUSH );
          miNew.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Slave.New" ) );
          lsNew = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              newSlaveServer();
            }
          };
          miNew.addSelectionListener( lsNew );
          miNew.setEnabled( !readonly );
          // Edit slave
          miEdit = new MenuItem( mTree, SWT.PUSH );
          miEdit.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Slave.Edit" ) );
          lsEdit = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              editSlaveServer( item );
            }
          };
          miEdit.addSelectionListener( lsEdit );
          miEdit.setEnabled( !readonly );
          // Delete slave
          miDel = new MenuItem( mTree, SWT.PUSH );
          miDel.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Slave.Delete" ) );
          lsDel = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              delSlaveServer( item );
            }
          };
          miDel.addSelectionListener( lsDel );
          miDel.setEnabled( !readonly );
          break;

        case ITEM_CATEGORY_PARTITIONS_ROOT:
          // New partition schema
          miNew = new MenuItem( mTree, SWT.PUSH );
          miNew.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.PartitionSchema.New" ) );
          lsNew = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              newPartitionSchema();
            }
          };
          miNew.addSelectionListener( lsNew );
          miNew.setEnabled( !readonly );
          break;

        case ITEM_CATEGORY_PARTITION:
          // New partition schema
          miNew = new MenuItem( mTree, SWT.PUSH );
          miNew.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.PartitionSchema.New" ) );
          lsNew = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              newPartitionSchema();
            }
          };
          miNew.addSelectionListener( lsNew );
          miNew.setEnabled( !readonly );
          // Edit partition schema
          miEdit = new MenuItem( mTree, SWT.PUSH );
          miEdit
            .setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.PartitionSchema.Edit" ) );
          lsEdit = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              editPartitionSchema( item );
            }
          };
          miEdit.addSelectionListener( lsEdit );
          miEdit.setEnabled( !readonly );
          // Delete partition schema
          miDel = new MenuItem( mTree, SWT.PUSH );
          miDel
            .setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.PartitionSchema.Delete" ) );
          lsDel = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              delPartitionSchema( item );
            }
          };
          miDel.addSelectionListener( lsDel );
          miDel.setEnabled( !readonly );
          break;

        case ITEM_CATEGORY_CLUSTERS_ROOT:
          // New cluster
          miNew = new MenuItem( mTree, SWT.PUSH );
          miNew.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Cluster.New" ) );
          lsNew = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              newCluster();
            }
          };
          miNew.addSelectionListener( lsNew );
          miNew.setEnabled( !readonly );
          break;

        case ITEM_CATEGORY_CLUSTER:
          // New cluster
          miNew = new MenuItem( mTree, SWT.PUSH );
          miNew.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Cluster.New" ) );
          lsNew = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              newCluster();
            }
          };
          miNew.addSelectionListener( lsNew );
          miNew.setEnabled( !readonly );
          // Edit cluster
          miEdit = new MenuItem( mTree, SWT.PUSH );
          miEdit.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Cluster.Edit" ) );
          lsEdit = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              editCluster( item );
            }
          };
          miEdit.addSelectionListener( lsEdit );
          miEdit.setEnabled( !readonly );
          // Delete cluster
          miDel = new MenuItem( mTree, SWT.PUSH );
          miDel.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Cluster.Delete" ) );
          lsDel = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              delCluster( item );
            }
          };
          miDel.addSelectionListener( lsDel );
          miDel.setEnabled( !readonly );
          break;

        case ITEM_CATEGORY_TRANSFORMATIONS_ROOT:
          break;

        case ITEM_CATEGORY_TRANSFORMATION:
          if ( level >= 2 && repositoryObject != null ) {
            final RepositoryDirectoryInterface repdir = repositoryObject.getRepositoryDirectory();

            if ( tisel.length == 1 ) {
              if ( !repositoryObject.isDeleted() ) {
                // Open transformation...
                MenuItem miOpen = new MenuItem( mTree, SWT.PUSH );
                miOpen.setText( BaseMessages.getString(
                  PKG, "RepositoryExplorerDialog.PopupMenu.Transformations.Open" ) );
                miOpen.addSelectionListener( new SelectionAdapter() {
                  public void widgetSelected( SelectionEvent e ) {
                    openTransformation( item, repdir );
                  }
                } );
                // Rename transformation
                MenuItem miRen = new MenuItem( mTree, SWT.PUSH );
                miRen.setText( BaseMessages.getString(
                  PKG, "RepositoryExplorerDialog.PopupMenu.Transformations.Rename" ) );
                miRen.addSelectionListener( new SelectionAdapter() {
                  public void widgetSelected( SelectionEvent e ) {
                    renameTransformation( item, repdir );
                  }
                } );
                miRen.setEnabled( !readonly );
              }
            }

            if ( repositoryObject.isDeleted() ) {
              if ( capabilities.supportsRevisions() ) {
                // Restore transformation
                MenuItem miRestore = new MenuItem( mTree, SWT.PUSH );
                miRestore.setText( BaseMessages.getString(
                  PKG, "RepositoryExplorerDialog.PopupMenu.Transformations.Restore" ) );
                miRestore.addSelectionListener( new SelectionAdapter() {
                  public void widgetSelected( SelectionEvent e ) {
                    restoreSelectedObjects();
                  }
                } );
              }
            } else {
              // Delete transformation
              miDel = new MenuItem( mTree, SWT.PUSH );
              miDel.setText( BaseMessages.getString(
                PKG, "RepositoryExplorerDialog.PopupMenu.Transformations.Delete" ) );
              miDel.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected( SelectionEvent e ) {
                  delSelectedObjects();
                }
              } );
              miDel.setEnabled( !readonly );
            }
          }
          break;

        case ITEM_CATEGORY_JOB_DIRECTORY:
        case ITEM_CATEGORY_TRANSFORMATION_DIRECTORY:
          if ( level >= 2 ) {
            // The first levels of path[] don't belong to the path to this directory!
            String[] realpath = new String[level - 1];
            for ( int i = 0; i < realpath.length; i++ ) {
              realpath[i] = path[i + 2];
            }

            // Find the directory in the directory tree...
            final RepositoryDirectoryInterface repdir = directoryTree.findDirectory( realpath );

            // Export xforms and jobs from directory
            miExp = new MenuItem( mTree, SWT.PUSH );
            miExp.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Objects.ExportAll" ) );
            lsExp = new SelectionAdapter() {
              public void widgetSelected( SelectionEvent e ) {
                exportAll( repdir );
              }
            };
            miExp.addSelectionListener( lsExp );
            miExp.setEnabled( !readonly );

            if ( cat == ITEM_CATEGORY_TRANSFORMATION_DIRECTORY ) {
              // Export transMeta
              miTrans = new MenuItem( mTree, SWT.PUSH );
              miTrans.setText( BaseMessages.getString(
                PKG, "RepositoryExplorerDialog.PopupMenu.Objects.ExportTrans" ) );
              lsTrans = new SelectionAdapter() {
                public void widgetSelected( SelectionEvent e ) {
                  exportTransformations( repdir );
                }
              };
              miTrans.addSelectionListener( lsTrans );
            }

            if ( cat == ITEM_CATEGORY_JOB_DIRECTORY ) {
              // Export jobs
              miJobs = new MenuItem( mTree, SWT.PUSH );
              miJobs
                .setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Objects.ExportJob" ) );
              lsJobs = new SelectionAdapter() {
                public void widgetSelected( SelectionEvent e ) {
                  exportJobs( repdir );
                }
              };
              miJobs.addSelectionListener( lsJobs );
            }

            // create directory...
            MenuItem miCreate = new MenuItem( mTree, SWT.PUSH );
            miCreate.setText( BaseMessages.getString(
              PKG, "RepositoryExplorerDialog.PopupMenu.TransDirectory.Create" ) );
            miCreate.addSelectionListener( new SelectionAdapter() {
              public void widgetSelected( SelectionEvent e ) {
                createDirectory( ti, repdir );
              }
            } );

            if ( level > 2 ) { // Can't rename or delete root directory...

              // Rename directory
              MenuItem miRename = new MenuItem( mTree, SWT.PUSH );
              miRename.setText( BaseMessages.getString(
                PKG, "RepositoryExplorerDialog.PopupMenu.TransDirectory.Rename" ) );
              miRename.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected( SelectionEvent e ) {
                  renameDirectory( ti, repdir );
                }
              } );
              // Delete directory
              MenuItem miDelete = new MenuItem( mTree, SWT.PUSH );
              miDelete.setText( BaseMessages.getString(
                PKG, "RepositoryExplorerDialog.PopupMenu.TransDirectory.Delete" ) );
              miDelete.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected( SelectionEvent e ) {
                  delDirectory( ti, repdir );
                }
              } );
            }
          }
          break;

        case ITEM_CATEGORY_JOBS_ROOT:
          break;
        case ITEM_CATEGORY_JOB:
          if ( level >= 2 && repositoryObject != null ) {
            final RepositoryDirectoryInterface repdir = repositoryObject.getRepositoryDirectory();

            if ( tisel.length == 1 ) {
              if ( !repositoryObject.isDeleted() ) {
                // Open job...
                MenuItem miOpen = new MenuItem( mTree, SWT.PUSH );
                miOpen.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Jobs.Open" ) );
                miOpen.addSelectionListener( new SelectionAdapter() {
                  public void widgetSelected( SelectionEvent e ) {
                    openJob( item, repdir );
                  }
                } );
                // Rename job
                MenuItem miRen = new MenuItem( mTree, SWT.PUSH );
                miRen.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Jobs.Rename" ) );
                miRen.addSelectionListener( new SelectionAdapter() {
                  public void widgetSelected( SelectionEvent e ) {
                    renameJob( ti, item, repdir );
                  }
                } );
                miRen.setEnabled( !readonly );
              }
            }
            if ( repositoryObject.isDeleted() ) {
              if ( capabilities.supportsRevisions() ) {
                // Restore job
                MenuItem miRestore = new MenuItem( mTree, SWT.PUSH );
                miRestore
                  .setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Jobs.Restore" ) );
                miRestore.addSelectionListener( new SelectionAdapter() {
                  public void widgetSelected( SelectionEvent e ) {
                    restoreSelectedObjects();
                  }
                } );
              }
            } else {
              // Delete job
              miDel = new MenuItem( mTree, SWT.PUSH );
              miDel.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Jobs.Delete" ) );
              miDel.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected( SelectionEvent e ) {
                  delSelectedObjects();
                }
              } );
              miDel.setEnabled( !readonly );
            }
          }
          break;

        case ITEM_CATEGORY_USERS_ROOT:
          mTree = new Menu( shell, SWT.POP_UP );
          // New user
          miNew = new MenuItem( mTree, SWT.PUSH );
          miNew.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.UsersRoot.New" ) );
          lsNew = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              newUser();
            }
          };
          miNew.addSelectionListener( lsNew );
          miNew.setEnabled( !readonly );
          break;

        case ITEM_CATEGORY_USER:
          // New user
          miNew = new MenuItem( mTree, SWT.PUSH );
          miNew.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Users.New" ) );
          lsNew = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              newUser();
            }
          };
          miNew.addSelectionListener( lsNew );
          miNew.setEnabled( !readonly );
          // Edit user info
          miEdit = new MenuItem( mTree, SWT.PUSH );
          miEdit.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Users.Edit" ) );
          lsEdit = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              editUser( item );
            }
          };
          miEdit.addSelectionListener( lsEdit );
          miEdit.setEnabled( !readonly );
          // Rename user
          MenuItem miRen = new MenuItem( mTree, SWT.PUSH );
          miRen.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Users.Rename" ) );
          SelectionAdapter lsRen = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              renameUser();
            }
          };
          miRen.addSelectionListener( lsRen );
          miRen.setEnabled( !readonly );
          // Delete user info
          miDel = new MenuItem( mTree, SWT.PUSH );
          miDel.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Users.Delete" ) );
          lsDel = new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
              delUser( item );
            }
          };
          miDel.addSelectionListener( lsDel );
          miDel.setEnabled( !readonly );
          break;

        default:
          mTree = null;
      }
    }

    ConstUI.displayMenu( mTree, wTree );
  }

  public void renameInTree() {
    TreeItem[] ti = wTree.getSelection();
    if ( ti.length == 1 ) {
      // Get the parent.
      final TreeItem item = ti[0];

      int level = ConstUI.getTreeLevel( item );
      String[] text = ConstUI.getTreeStrings( item );

      int cat = getItemCategory( item );

      switch ( cat ) {
        case ITEM_CATEGORY_DATABASE:
          renameDatabase();
          break;
        case ITEM_CATEGORY_TRANSFORMATION:
          String name = item.getText();

          // The first 3 levels of text[] don't belong to the path to this transformation!
          String[] path = new String[level - 2];
          for ( int i = 0; i < path.length; i++ ) {
            path[i] = text[i + 2];
          }

          // Find the directory in the directory tree...
          RepositoryDirectoryInterface repdir = directoryTree.findDirectory( path );

          if ( repdir != null ) {
            renameTransformation( name, repdir );
          }
          break;

        case ITEM_CATEGORY_JOB:
          name = item.getText();

          // The first 3 levels of text[] don't belong to the path to this transformation!
          path = new String[level - 2];
          for ( int i = 0; i < path.length; i++ ) {
            path[i] = text[i + 2];
          }

          // Find the directory in the directory tree...
          repdir = directoryTree.findDirectory( path );

          if ( repdir != null ) {
            renameJob( name, repdir );
          }
          break;

        case ITEM_CATEGORY_USER:
          renameUser();
          break;

        default:
          break;
      }
    }
  }

  public void close() {
    props.setScreen( new WindowProperty( shell ) );
    shell.dispose();
  }

  public void refreshTree() {
    try {
      // IUser userinfo = securityProvider.getUserInfo();

      wTree.removeAll();
      objectMap.clear();

      // Load the directory tree:
      directoryTree = rep.loadRepositoryDirectoryTree();

      TreeItem tiTree = new TreeItem( wTree, SWT.NONE );
      tiTree.setImage( GUIResource.getInstance().getImageFolderConnections() );
      tiTree.setText( rep.getName() == null ? "-" : rep.getName() );

      // The Databases...
      TreeItem tiParent = new TreeItem( tiTree, SWT.NONE );
      tiParent.setImage( GUIResource.getInstance().getImageBol() );
      tiParent.setText( STRING_DATABASES );

      if ( !!readonly ) {
        TreeItemAccelerator.addDoubleClick( tiParent, new DoubleClickInterface() {
          public void action( TreeItem treeItem ) {
            newDatabase();
          }
        } );
      }

      String[] names = rep.getDatabaseNames( includeDeleted );
      for ( int i = 0; i < names.length; i++ ) {
        TreeItem newDB = new TreeItem( tiParent, SWT.NONE );
        newDB.setImage( GUIResource.getInstance().getImageConnection() );
        newDB.setText( Const.NVL( names[i], "" ) );
        if ( !readonly ) {
          TreeItemAccelerator.addDoubleClick( newDB, new DoubleClickInterface() {
            public void action( TreeItem treeItem ) {
              editDatabase( treeItem.getText() );
            }
          } );
        }
      }

      // The partition schemas...
      tiParent = new TreeItem( tiTree, SWT.NONE );
      tiParent.setImage( GUIResource.getInstance().getImageBol() );
      tiParent.setText( STRING_PARTITIONS );
      if ( !readonly ) {
        TreeItemAccelerator.addDoubleClick( tiParent, new DoubleClickInterface() {
          public void action( TreeItem treeItem ) {
            newPartitionSchema();
          }
        } );
      }

      names = rep.getPartitionSchemaNames( includeDeleted );
      for ( int i = 0; i < names.length; i++ ) {
        TreeItem newItem = new TreeItem( tiParent, SWT.NONE );
        newItem.setImage( GUIResource.getInstance().getImageFolderConnections() );
        newItem.setText( Const.NVL( names[i], "" ) );
        if ( !readonly ) {
          TreeItemAccelerator.addDoubleClick( newItem, new DoubleClickInterface() {
            public void action( TreeItem treeItem ) {
              editPartitionSchema( treeItem.getText() );
            }
          } );
        }
      }

      // The slaves...
      tiParent = new TreeItem( tiTree, SWT.NONE );
      tiParent.setImage( GUIResource.getInstance().getImageBol() );
      tiParent.setText( STRING_SLAVES );
      if ( !readonly ) {
        TreeItemAccelerator.addDoubleClick( tiParent, new DoubleClickInterface() {
          public void action( TreeItem treeItem ) {
            newSlaveServer();
          }
        } );
      }

      names = rep.getSlaveNames( includeDeleted );
      for ( int i = 0; i < names.length; i++ ) {
        TreeItem newItem = new TreeItem( tiParent, SWT.NONE );
        newItem.setImage( GUIResource.getInstance().getImageSlave() );
        newItem.setText( Const.NVL( names[i], "" ) );
        if ( !readonly ) {
          TreeItemAccelerator.addDoubleClick( newItem, new DoubleClickInterface() {
            public void action( TreeItem treeItem ) {
              editSlaveServer( treeItem.getText() );
            }
          } );
        }
      }

      // The clusters ...
      tiParent = new TreeItem( tiTree, SWT.NONE );
      tiParent.setImage( GUIResource.getInstance().getImageBol() );
      tiParent.setText( STRING_CLUSTERS );
      if ( !readonly ) {
        TreeItemAccelerator.addDoubleClick( tiParent, new DoubleClickInterface() {
          public void action( TreeItem treeItem ) {
            newCluster();
          }
        } );
      }

      names = rep.getClusterNames( includeDeleted );
      for ( int i = 0; i < names.length; i++ ) {
        TreeItem newItem = new TreeItem( tiParent, SWT.NONE );
        newItem.setImage( GUIResource.getInstance().getImageCluster() );
        newItem.setText( Const.NVL( names[i], "" ) );
        if ( !readonly ) {
          TreeItemAccelerator.addDoubleClick( newItem, new DoubleClickInterface() {
            public void action( TreeItem treeItem ) {
              editCluster( treeItem.getText() );
            }
          } );
        }
      }

      // The transformations...
      TreeItem tiTrans = new TreeItem( tiTree, SWT.NONE );
      tiTrans.setImage( GUIResource.getInstance().getImageTransGraph() );
      tiTrans.setText( STRING_TRANSFORMATIONS );

      TreeItem newCat = new TreeItem( tiTrans, SWT.NONE );
      newCat.setImage( GUIResource.getInstance().getImageLogoSmall() );
      Color dircolor = GUIResource.getInstance().getColorDirectory();
      RepositoryDirectoryUI.getTreeWithNames(
        newCat, rep, dircolor, sortColumn, includeDeleted, ascending, true, false, directoryTree, null, null );

      // The Jobs...
      TreeItem tiJob = new TreeItem( tiTree, SWT.NONE );
      tiJob.setImage( GUIResource.getInstance().getImageJobGraph() );
      tiJob.setText( STRING_JOBS );

      TreeItem newJob = new TreeItem( tiJob, SWT.NONE );
      newJob.setImage( GUIResource.getInstance().getImageLogoSmall() );
      RepositoryDirectoryUI.getTreeWithNames(
        newJob, rep, dircolor, sortColumn, includeDeleted, ascending, false, true, directoryTree, null, null );

      //
      // Add the users or only yourself
      //
      if ( capabilities.supportsUsers() && capabilities.managesUsers() ) {
        TreeItem tiUser = new TreeItem( tiTree, SWT.NONE );
        tiUser.setImage( GUIResource.getInstance().getImageBol() );
        tiUser.setText( STRING_USERS );
        if ( !readonly ) {
          TreeItemAccelerator.addDoubleClick( tiUser, new DoubleClickInterface() {
            public void action( TreeItem treeItem ) {
              newUser();
            }
          } );
        }

        String[] users = securityProvider.getUserLogins();
        for ( int i = 0; i < users.length; i++ ) {
          if ( users[i] != null ) {
            // If users[i] is null TreeWidget will throw exceptions.
            // The solution is to verify on saving a user.
            TreeItem newUser = new TreeItem( tiUser, SWT.NONE );
            newUser.setImage( GUIResource.getInstance().getImageUser() );
            newUser.setText( users[i] );
            if ( !readonly ) {
              TreeItemAccelerator.addDoubleClick( newUser, new DoubleClickInterface() {
                public void action( TreeItem treeItem ) {
                  editUser( treeItem.getText() );
                }
              } );
            }
          }
        }
      }

      // Always expand the top level entry...
      TreeMemory.getInstance().storeExpanded(
        STRING_REPOSITORY_EXPLORER_TREE_NAME, new String[] { tiTree.getText() }, true );

      // Set the expanded flags based on the TreeMemory
      TreeMemory.setExpandedFromMemory( wTree, STRING_REPOSITORY_EXPLORER_TREE_NAME );

    } catch ( KettleException dbe ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Dialog.ErrorRefreshingTree.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.PopupMenu.Dialog.ErrorRefreshingTree.Message" ), dbe );
    }
  }

  public void openTransformation( String name, RepositoryDirectoryInterface repdir ) {
    lastOpened = new RepositoryObjectReference( RepositoryObjectType.TRANSFORMATION, repdir, name );
    if ( callback != null ) {
      if ( callback.open( lastOpened ) ) {
        close();
      }
    } else {
      close();
    }
  }

  public void openJob( String name, RepositoryDirectoryInterface repdir ) {
    lastOpened = new RepositoryObjectReference( RepositoryObjectType.JOB, repdir, name );
    if ( callback != null ) {
      if ( callback.open( lastOpened ) ) {
        close();
      }
    } else {
      close();
    }
  }

  public boolean delSelectedObjects() {
    TreeItem[] items = wTree.getSelection();
    boolean error = false;

    MessageBox mb = new MessageBox( shell, SWT.ICON_WARNING | SWT.YES | SWT.NO );
    mb
      .setMessage( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Delete.Confirm.Message1" )
        + ( items.length > 1
          ? BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Delete.Confirm.Message2" )
            + items.length + BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Delete.Confirm.Message3" )
          : BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Delete.Confirm.Message4" ) ) );
    mb.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Delete.Confirm.Title" ) );
    int answer = mb.open();

    if ( answer != SWT.YES ) {
      return false;
    }

    for ( int i = 0; i < items.length; i++ ) {
      final RepositoryElementMetaInterface repositoryObject = objectMap.get( ConstUI.getTreePath( items[i], 0 ) );
      if ( repositoryObject != null ) {

        try {
          switch ( repositoryObject.getObjectType() ) {
            case TRANSFORMATION:
              rep.deleteTransformation( repositoryObject.getObjectId() );
              break;
            case JOB:
              rep.deleteJob( repositoryObject.getObjectId() );
              break;
            default:
              break;
          }
        } catch ( Exception e ) {
          new ErrorDialog(
            shell,
            BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Delete.ErrorRemoving.Title" ), BaseMessages
              .getString( PKG, "RepositoryExplorerDialog.Trans.Delete.ErrorRemoving.Message" )
              + repositoryObject.getName() + "]", e );
          error = true;
        }
      }
    }
    refreshTree();

    return !error;
  }

  public boolean restoreSelectedObjects() {
    TreeItem[] items = wTree.getSelection();
    boolean error = false;

    for ( int i = 0; i < items.length; i++ ) {
      final RepositoryElementMetaInterface repositoryObject = objectMap.get( ConstUI.getTreePath( items[i], 0 ) );
      if ( repositoryObject != null ) {
        try {
          rep.undeleteObject( repositoryObject );
        } catch ( Exception e ) {
          new ErrorDialog(
            shell,
            BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Delete.ErrorRestoring.Title" ), BaseMessages
              .getString( PKG, "RepositoryExplorerDialog.Trans.Delete.ErrorRestoring.Message" ), e );
          error = true;
        }
      }
    }
    refreshTree();

    return !error;
  }

  public boolean renameTransformation( String name, RepositoryDirectoryInterface repdir ) {
    boolean retval = false;
    final TreeItem ti = wTree.getSelection()[0];

    if ( ti.getItemCount() == 0 ) {
      final String fname = name;
      final RepositoryDirectoryInterface frepdir = repdir;

      TreeEditor editor = new TreeEditor( wTree );
      editor.setItem( ti );
      final Text text = new Text( wTree, SWT.NONE );
      props.setLook( text );
      text.setText( name );
      text.addFocusListener( new FocusAdapter() {
        public void focusLost( FocusEvent arg0 ) {
          // Focus is lost: apply changes.
          String newname = text.getText();
          if ( renameTransformation( fname, newname, frepdir ) ) {
            ti.setText( newname );
          }
          text.dispose();
        }
      } );
      text.addKeyListener( new KeyAdapter() {
        public void keyPressed( KeyEvent e ) {
          // ESC --> Don't change tree item...
          if ( e.keyCode == SWT.ESC ) {
            text.dispose();
          }
          // ENTER --> Save changes...
          if ( e.character == SWT.CR ) {
            String newname = text.getText();
            if ( renameTransformation( fname, newname, frepdir ) ) {
              ti.setText( newname );
            }
            text.dispose();
          }
        }
      } );

      editor.horizontalAlignment = SWT.LEFT;
      editor.grabHorizontal = true;
      editor.grabVertical = true;
      editor.minimumWidth = 50;

      text.selectAll();
      text.setFocus();

      editor.layout();
      editor.setEditor( text );
    }
    return retval;
  }

  public boolean renameTransformation( String name, String newname, RepositoryDirectoryInterface repdir ) {
    boolean retval = false;

    try {
      if ( Const.isEmpty( newname ) ) {
        throw new KettleException( BaseMessages.getString(
          PKG, "RepositoryExplorerDialog.Exception.NameCanNotBeEmpty" ) );
      }
      if ( !name.equals( newname ) ) {
        ObjectId id = rep.getTransformationID( name, repdir );
        if ( id != null ) {
          // System.out.println("Renaming transformation ["+name+"] with ID = "+id);
          String comment = BaseMessages.getString( REPOSITORY_PKG, "Repository.Rename", name, newname );
          rep.renameTransformation( id, comment, repdir, newname );
          retval = true;
        }
      } else {
        MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
        mb
          .setMessage( BaseMessages.getString(
            PKG, "RepositoryExplorerDialog.Trans.Rename.ErrorFinding.Message1" )
            + name + "]" + Const.CR
            + BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Rename.ErrorFinding.Message2" ) );
        mb.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Rename.ErrorFinding.Title" ) );
        mb.open();
      }
    } catch ( KettleException dbe ) {
      new ErrorDialog(
        shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Rename.ErrorRenaming.Title" ), BaseMessages
          .getString( PKG, "RepositoryExplorerDialog.Trans.Rename.ErrorRenaming.Message" )
          + name + "]!", dbe );
    }

    return retval;
  }

  public boolean moveTransformation( String xml, RepositoryDirectoryInterface repdir ) {
    debug = "Move transformation";

    boolean retval = false;

    try {
      debug = "parse xml";
      Document doc = XMLHandler.loadXMLString( xml );

      String dirname = XMLHandler.getTagValue( doc, "dragdrop", "directory" );
      String transname = XMLHandler.getTagValue( doc, "dragdrop", "transformation" );

      if ( dirname != null && transname != null ) {
        debug = "dirname=" + dirname + ", transname=" + transname;

        // OK, find this transformation...
        RepositoryDirectoryInterface fromdir = directoryTree.findDirectory( dirname );
        if ( fromdir != null ) {
          debug = "fromdir found: move transformation!";
          ObjectId existingTransID = rep.getTransformationID( transname, repdir );
          if ( existingTransID == null ) {
            ObjectId id = rep.getTransformationID( transname, fromdir );
            rep.renameTransformation( id, repdir, transname );
            retval = true;
          } else {
            MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
            mb.setMessage( BaseMessages.getString(
              PKG, "RepositoryExplorerDialog.Trans.Move.ErrorDuplicate.Message", transname )
              + Const.CR );
            mb.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Move.ErrorDuplicate.Title" ) );
            mb.open();
          }
        } else {
          MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
          mb.setMessage( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Move.ErrorMoving.Message" )
            + dirname + "]" + Const.CR );
          mb.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Move.ErrorMoving.Title" ) );
          mb.open();
        }
      }
    } catch ( Exception dbe ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Move.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Move.UnexpectedError.Message" ), dbe );
    }

    return retval;
  }

  public boolean moveJob( String xml, RepositoryDirectoryInterface repdir ) {
    debug = "Move Job";

    boolean retval = false;

    try {
      debug = "parse xml";
      Document doc = XMLHandler.loadXMLString( xml );

      String dirname = XMLHandler.getTagValue( doc, "dragdrop", "directory" );
      String jobname = XMLHandler.getTagValue( doc, "dragdrop", "job" );

      if ( dirname != null && jobname != null ) {
        debug = "dirname=" + dirname + ", jobname=" + jobname;

        // OK, find this transformation...
        RepositoryDirectoryInterface fromdir = directoryTree.findDirectory( dirname );
        if ( fromdir != null ) {
          debug = "fromdir found: move job!";
          ObjectId existingjobID = rep.getJobId( jobname, repdir );
          if ( existingjobID == null ) {
            ObjectId id = rep.getJobId( jobname, fromdir );
            rep.renameJob( id, repdir, jobname );
            retval = true;
          } else {
            MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
            mb.setMessage( BaseMessages.getString(
              PKG, "RepositoryExplorerDialog.Job.Move.ErrorDuplicate.Message", jobname )
              + Const.CR );
            mb.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Job.Move.ErrorDuplicate.Title" ) );
            mb.open();
          }
        } else {
          MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
          mb.setMessage( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Job.Move.ErrorMoving.Message" )
            + dirname + "]" + Const.CR );
          mb.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Job.Move.ErrorMoving.Title" ) );
          mb.open();
        }
      }
    } catch ( Exception dbe ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Job.Move.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Trans.Move.UnexpectedError.Message" ), dbe );
    }

    return retval;
  }

  public boolean renameJob( String name, RepositoryDirectoryInterface repdir ) {
    boolean retval = false;

    final TreeItem ti = wTree.getSelection()[0];

    if ( ti.getItemCount() == 0 ) {
      final String fname = name;
      final RepositoryDirectoryInterface frepdir = repdir;

      TreeEditor editor = new TreeEditor( wTree );
      editor.setItem( ti );
      final Text text = new Text( wTree, SWT.NONE );
      props.setLook( text );
      text.setText( name );
      text.addFocusListener( new FocusAdapter() {
        public void focusLost( FocusEvent arg0 ) {
          // Focus is lost: apply changes
          String newname = text.getText();
          if ( renameJob( fname, newname, frepdir ) ) {
            ti.setText( newname );
          }
          text.dispose();
        }
      } );
      text.addKeyListener( new KeyAdapter() {
        public void keyPressed( KeyEvent e ) {
          // ESC --> Don't change tree item...
          if ( e.keyCode == SWT.ESC ) {
            text.dispose();
          }
          // ENTER --> Save changes...
          if ( e.character == SWT.CR ) {
            String newname = text.getText();
            if ( renameJob( fname, newname, frepdir ) ) {
              ti.setText( newname );
            }
            text.dispose();
          }
        }
      } );

      editor.horizontalAlignment = SWT.LEFT;
      editor.grabHorizontal = true;
      editor.grabVertical = true;
      editor.minimumWidth = 50;

      text.selectAll();
      text.setFocus();

      editor.layout();
      editor.setEditor( text );
    }
    return retval;
  }

  public boolean renameJob( String name, String newname, RepositoryDirectoryInterface repdir ) {
    boolean retval = false;

    try {
      if ( Const.isEmpty( newname ) ) {
        throw new KettleException( BaseMessages.getString(
          PKG, "RepositoryExplorerDialog.Exception.NameCanNotBeEmpty" ) );
      }
      if ( !name.equals( newname ) ) {
        ObjectId id = rep.getJobId( name, repdir );
        if ( id != null ) {
          System.out.println( "Renaming job [" + name + "] with ID = " + id );
          String comment = BaseMessages.getString( REPOSITORY_PKG, "Repository.Rename", name, newname );
          rep.renameJob( id, comment, repdir, newname );
          retval = true;
        }
      } else {
        MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
        mb
          .setMessage( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Job.Move.ErrorFinding.Message1" )
            + name + "]" + Const.CR
            + BaseMessages.getString( PKG, "RepositoryExplorerDialog.Job.Move.ErrorFinding.Message2" ) );
        mb.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Job.Move.ErrorFinding.Title" ) );
        mb.open();
      }
    } catch ( KettleException dbe ) {
      new ErrorDialog(
        shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Job.Move.UnexpectedError.Title" ), BaseMessages
          .getString( PKG, "RepositoryExplorerDialog.Job.Move.UnexpectedError.Message" )
          + name + "]", dbe );
    }

    return retval;
  }

  public void renameJob( TreeItem treeitem, String jobname, RepositoryDirectoryInterface repositorydir ) {
    final TreeItem ti = treeitem;
    final String name = jobname;
    final RepositoryDirectoryInterface repdir = repositorydir;

    TreeEditor editor = new TreeEditor( wTree );
    editor.setItem( ti );
    final Text text = new Text( wTree, SWT.NONE );
    props.setLook( text );
    text.setText( name );
    text.addFocusListener( new FocusAdapter() {
      public void focusLost( FocusEvent arg0 ) {
        // Focus is lost: apply changes
        String newname = text.getText();
        if ( renameJob( name, repdir, newname ) ) {
          ti.setText( newname );
        }
        text.dispose();
      }
    } );
    text.addKeyListener( new KeyAdapter() {
      public void keyPressed( KeyEvent e ) {
        // ESC --> Don't change tree item...
        if ( e.keyCode == SWT.ESC ) {
          text.dispose();
        }
        // ENTER --> Save changes...
        if ( e.character == SWT.CR ) {
          String newname = text.getText();
          if ( renameJob( name, repdir, newname ) ) {
            ti.setText( newname );
          }
          text.dispose();
        }
      }
    } );

    editor.horizontalAlignment = SWT.LEFT;
    editor.grabHorizontal = true;
    editor.grabVertical = true;
    editor.minimumWidth = 50;

    text.selectAll();
    text.setFocus();

    editor.layout();
    editor.setEditor( text );
  }

  public boolean renameJob( String name, RepositoryDirectoryInterface repdir, String newname ) {
    boolean retval = false;

    try {
      if ( Const.isEmpty( newname ) ) {
        throw new KettleException( BaseMessages.getString(
          PKG, "RepositoryExplorerDialog.Exception.NameCanNotBeEmpty" ) );
      }
      if ( !name.equals( newname ) ) {
        ObjectId id = rep.getJobId( name, repdir );
        if ( id != null ) {
          // System.out.println("Renaming transformation ["+name+"] with ID = "+id);
          String comment = BaseMessages.getString( REPOSITORY_PKG, "Repository.Rename", name, newname );
          rep.renameJob( id, comment, repdir, newname );
          retval = true;
        } else {
          MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
          mb
            .setMessage( BaseMessages.getString(
              PKG, "RepositoryExplorerDialog.Job.Rename.ErrorFinding.Message1" )
              + name + "]" + Const.CR
              + BaseMessages.getString( PKG, "RepositoryExplorerDialog.Job.Rename.ErrorFinding.Message2" ) );
          mb.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Job.Rename.ErrorFinding.Title" ) );
          mb.open();
        }
      }
    } catch ( KettleException dbe ) {
      new ErrorDialog(
        shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Job.Rename.UnexpectedError.Title" ), BaseMessages
          .getString( PKG, "RepositoryExplorerDialog.Job.Rename.UnexpectedError.Message" )
          + name + "]", dbe );
    }

    return retval;
  }

  public void editUser( String login ) {
    try {
      IUser uinfo = securityManager.loadUserInfo( login ); // Get UserInfo from repository...
      UserDialog ud = new UserDialog( shell, SWT.NONE, rep, uinfo );
      IUser ui = ud.open();
      if ( !readonly ) {
        if ( ui != null ) {
          securityManager.saveUserInfo( ui );
        }
      } else {
        MessageBox mb = new MessageBox( shell, SWT.ICON_WARNING | SWT.OK );
        mb.setMessage( BaseMessages.getString( PKG, "RepositoryExplorerDialog.User.Edit.NotAllowed.Message" ) );
        mb.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.User.Edit.NotAllowed.Title" ) );
        mb.open();
      }
      if ( ui != null && !login.equalsIgnoreCase( ui.getLogin() ) ) {
        refreshTree();
      }

    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.User.Edit.UnexpectedError.Message.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.User.Edit.UnexpectedError.Message" ), e );
    }
  }

  public void newUser() {
    UserDialog ud = new UserDialog( shell, SWT.NONE, rep, new UserInfo() );
    IUser ui = ud.open();
    if ( ui != null ) {
      /***************************
       * Removed by sboden as the user dialog already saves the id on pressing ok. Related defect #4228 on javaforge.
       *
       * // See if this user already exists... long uid = rep.getUserID(ui.getLogin()); if (uid<=0) { ui.saveRep(rep); }
       * else { MessageBox mb = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
       * mb.setMessage(BaseMessages.getString(PKG, "RepositoryExplorerDialog.User.New.AlreadyExists.Message"));
       * mb.setText(BaseMessages.getString(PKG, "RepositoryExplorerDialog.User.New.AlreadyExists.Title")); mb.open(); }
       ****************************/
      // Refresh tree...
      refreshTree();
    }
  }

  public void delUser( String login ) {
    try {
      ObjectId idUser = securityManager.getUserID( login );
      if ( idUser != null ) {
        securityManager.delUser( idUser );
      }
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.User.Delete.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.User.Delete.UnexpectedError.Message" ), e );
    }

    refreshTree();
  }

  public boolean renameUser() {
    boolean retval = false;
    final TreeItem ti = wTree.getSelection()[0];

    if ( ti.getItemCount() == 0 ) {
      final String name = ti.getText();
      TreeEditor editor = new TreeEditor( wTree );
      editor.setItem( ti );
      final Text text = new Text( wTree, SWT.NONE );
      props.setLook( text );
      text.setText( name );
      text.addFocusListener( new FocusAdapter() {
        public void focusLost( FocusEvent arg0 ) {
          // Focus is lost: apply changes
          String newname = text.getText();
          if ( renameUser( name, newname ) ) {
            ti.setText( newname );
          }
          text.dispose();
        }
      } );
      text.addKeyListener( new KeyAdapter() {
        public void keyPressed( KeyEvent e ) {
          // ESC --> Don't change tree item...
          if ( e.keyCode == SWT.ESC ) {
            text.dispose();
          }
          // ENTER --> Save changes...
          if ( e.character == SWT.CR ) {
            String newname = text.getText();
            if ( renameUser( name, newname ) ) {
              ti.setText( newname );
            }
            text.dispose();
          }
        }
      } );

      editor.horizontalAlignment = SWT.LEFT;
      editor.grabHorizontal = true;
      editor.grabVertical = true;
      editor.minimumWidth = 50;

      text.selectAll();
      text.setFocus();

      editor.layout();
      editor.setEditor( text );
    }
    return retval;
  }

  public boolean renameUser( String name, String newname ) {
    boolean retval = false;

    try {
      if ( Const.isEmpty( newname ) ) {
        throw new KettleException( BaseMessages.getString(
          PKG, "RepositoryExplorerDialog.Exception.NameCanNotBeEmpty" ) );
      }
      if ( !name.equals( newname ) ) {
        ObjectId id = securityManager.getUserID( name );
        if ( id != null ) {
          // System.out.println("Renaming user ["+name+"] with ID = "+id);
          securityManager.renameUser( id, newname );
          retval = true;
        } else {
          MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
          mb
            .setMessage( BaseMessages.getString(
              PKG, "RepositoryExplorerDialog.User.Rename.ErrorFinding.Message1" )
              + name + "]" + Const.CR
              + BaseMessages.getString( PKG, "RepositoryExplorerDialog.User.Rename.ErrorFinding.Message2" ) );
          mb.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.User.Rename.ErrorFinding.Title" ) );
          mb.open();
        }
      }
    } catch ( KettleException e ) {
      new ErrorDialog(
        shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.User.Rename.UnexpectedError.Title" ), BaseMessages
          .getString( PKG, "RepositoryExplorerDialog.User.Rename.UnexpectedError.Message" )
          + name + "]", e );
    }

    return retval;
  }

  public void editDatabase( String databasename ) {
    try {
      ObjectId idDatabase = rep.getDatabaseID( databasename );
      DatabaseMeta databaseMeta = rep.loadDatabaseMeta( idDatabase, null ); // reads last version

      getDatabaseDialog().setDatabaseMeta( databaseMeta );

      String name = getDatabaseDialog().open();
      if ( name != null ) {
        if ( !readonly ) {
          rep.insertLogEntry( "Updating database connection '"
            + getDatabaseDialog().getDatabaseMeta().getName() + "'" );
          rep.save( databaseMeta, Const.VERSION_COMMENT_EDIT_VERSION, null );
        } else {
          MessageBox mb = new MessageBox( shell, SWT.ICON_WARNING | SWT.OK );
          mb.setMessage( BaseMessages.getString(
            PKG, "RepositoryExplorerDialog.Connection.Edit.NotAllowed.Message" ) );
          mb.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Connection.Edit.NotAllowed.Title" ) );
          mb.open();
        }
        if ( !databasename.equalsIgnoreCase( name ) ) {
          refreshTree();
        }
      }
    } catch ( KettleException e ) {
      new ErrorDialog(
        shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Connection.Edit.UnexpectedError.Title" ), BaseMessages
          .getString( PKG, "RepositoryExplorerDialog.Connection.Edit.UnexpectedError.Message" )
          + databasename + "]", e );
    }
  }

  public void newDatabase() {
    try {
      DatabaseMeta databaseMeta = new DatabaseMeta();
      databaseMeta.initializeVariablesFrom( null );
      getDatabaseDialog().setDatabaseMeta( databaseMeta );

      String name = getDatabaseDialog().open();
      if ( name != null ) {
        // See if this user already exists...
        ObjectId idDatabase = rep.getDatabaseID( name );
        if ( idDatabase == null ) {
          rep.insertLogEntry( "Creating new database '" + databaseMeta.getName() + "'" );
          rep.save( databaseMeta, Const.VERSION_COMMENT_INITIAL_VERSION, null );
        } else {
          MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
          mb.setMessage( BaseMessages.getString(
            PKG, "RepositoryExplorerDialog.Connection.Create.AlreadyExists.Message" ) );
          mb.setText( BaseMessages.getString(
            PKG, "RepositoryExplorerDialog.Connection.Create.AlreadyExists.Title" ) );
          mb.open();
        }
        // Refresh tree...
        refreshTree();
      }
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Connection.Create.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Connection.Create.UnexpectedError.Message" ), e );
    }
  }

  public void delDatabase( String databasename ) {
    try {
      rep.deleteDatabaseMeta( databasename );

      refreshTree();
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Connection.Delete.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Connection.Delete.UnexpectedError.Message" ), e );
    }
  }

  public boolean renameDatabase() {
    boolean retval = false;
    final TreeItem ti = wTree.getSelection()[0];

    if ( ti.getItemCount() == 0 ) {
      final String name = ti.getText();
      TreeEditor editor = new TreeEditor( wTree );
      editor.setItem( ti );
      final Text text = new Text( wTree, SWT.NONE );
      props.setLook( text );
      text.setText( name );
      text.addFocusListener( new FocusAdapter() {
        public void focusLost( FocusEvent arg0 ) {
          // Focus is lost: apply changes
          String newname = text.getText();
          if ( renameDatabase( name, newname ) ) {
            ti.setText( newname );
          }
          text.dispose();
        }
      } );
      text.addKeyListener( new KeyAdapter() {
        public void keyPressed( KeyEvent e ) {
          // ESC --> Don't change tree item...
          if ( e.keyCode == SWT.ESC ) {
            text.dispose();
          }
          // ENTER --> Save changes...
          if ( e.character == SWT.CR ) {
            if ( ti.getText().equals( name ) ) // Only if the name wasn't changed already.
            {
              String newname = text.getText();
              if ( renameDatabase( name, newname ) ) {
                ti.setText( newname );
              }
              text.dispose();
            }
          }
        }
      } );

      editor.horizontalAlignment = SWT.LEFT;
      editor.grabHorizontal = true;
      editor.grabVertical = true;
      editor.minimumWidth = 50;

      text.selectAll();
      text.setFocus();

      editor.layout();
      editor.setEditor( text );
    }
    return retval;
  }

  public boolean renameDatabase( String name, String newname ) {
    boolean retval = false;

    try {
      if ( Const.isEmpty( newname ) ) {
        throw new KettleException( BaseMessages.getString(
          PKG, "RepositoryExplorerDialog.Exception.NameCanNotBeEmpty" ) );
      }

      if ( !name.equals( newname ) ) {
        ObjectId id = rep.getDatabaseID( name );
        if ( id != null ) {
          DatabaseMeta databaseMeta = rep.loadDatabaseMeta( id, null ); // reads last version
          databaseMeta.setName( newname );
          rep.insertLogEntry( "Renaming database connection '"
            + getDatabaseDialog().getDatabaseMeta().getName() + "'" );
          rep.save( databaseMeta, Const.VERSION_COMMENT_EDIT_VERSION, null );
          retval = true;
        } else {
          MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
          mb
            .setMessage( BaseMessages.getString(
              PKG, "RepositoryExplorerDialog.Connection.Rename.ErrorFinding.Message1" )
              + name + "]" + Const.CR
              + BaseMessages.getString( PKG, "RepositoryExplorerDialog.Connection.Rename.ErrorFinding.Message2" ) );
          mb.setText( BaseMessages
            .getString( PKG, "RepositoryExplorerDialog.Connection.Rename.ErrorFinding.Title" ) );
          mb.open();
        }
      }
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Connection.Rename.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Connection.Rename.UnexpectedError.Message" ), e );
    }

    return retval;
  }

  public void renameDirectory( TreeItem treeitem, RepositoryDirectoryInterface rd ) {
    final TreeItem ti = treeitem;
    final RepositoryDirectoryInterface repdir = rd;

    final String name = ti.getText();
    TreeEditor editor = new TreeEditor( wTree );
    editor.setItem( ti );
    final Text text = new Text( wTree, SWT.NONE );
    props.setLook( text );
    text.setText( name );
    text.addFocusListener( new FocusAdapter() {
      public void focusLost( FocusEvent arg0 ) {
        // Focus is lost: apply changes
        String newname = text.getText();
        if ( renameDirectory( repdir, name, newname ) ) {
          ti.setText( newname );
        }
        text.dispose();
      }
    } );
    text.addKeyListener( new KeyAdapter() {
      public void keyPressed( KeyEvent e ) {
        // ESC --> Don't change tree item...
        if ( e.keyCode == SWT.ESC ) {
          text.dispose();
        }
        // ENTER --> Save changes...
        if ( e.character == SWT.CR ) {
          String newname = text.getText();
          if ( renameDirectory( repdir, name, newname ) ) {
            ti.setText( newname );
          }
          text.dispose();
        }
      }
    } );

    editor.horizontalAlignment = SWT.LEFT;
    editor.grabHorizontal = true;
    editor.grabVertical = true;
    editor.minimumWidth = 50;

    text.selectAll();
    text.setFocus();

    editor.layout();
    editor.setEditor( text );
  }

  public boolean renameDirectory( RepositoryDirectoryInterface repdir, String name, String newname ) {
    boolean retval = false;

    try {
      if ( Const.isEmpty( newname ) ) {
        throw new KettleException( BaseMessages.getString(
          PKG, "RepositoryExplorerDialog.Exception.NameCanNotBeEmpty" ) );
      }
      if ( !name.equals( newname ) ) {
        repdir.setName( newname );
        try {
          rep.renameRepositoryDirectory( repdir.getObjectId(), repdir, newname );
          retval = true;
        } catch ( Exception exception ) {
          retval = false;
          new ErrorDialog(
            shell,
            BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Rename.UnexpectedError.Message1" )
              + name + "]" + Const.CR
              + BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Rename.UnexpectedError.Message2" ),
            BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Rename.UnexpectedError.Title" ),
            exception );
        }
      }
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Rename.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Rename.UnexpectedError.Message" ), e );
    }

    return retval;
  }

  public void createDirectory( TreeItem ti, RepositoryDirectoryInterface repdir ) {
    EnterStringDialog esd = new EnterStringDialog( shell,
      BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Create.AskName.Default" ),
      BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Create.AskName.Title" ),
      BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Create.AskName.Message" ) );
    String newdir = esd.open();
    if ( newdir != null ) {
      RepositoryDirectoryInterface rd = new RepositoryDirectory( repdir, newdir );
      String[] path = rd.getPathArray();

      RepositoryDirectoryInterface exists = directoryTree.findDirectory( path );
      if ( exists == null ) {
        try {
          rep.saveRepositoryDirectory( rd );
        } catch ( Exception exception ) {
          new ErrorDialog(
            shell,
            BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Create.UnexpectedError.Message1" )
              + newdir
              + BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Create.UnexpectedError.Message2" )
              + repdir.getPath() + "]",
            BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Create.UnexpectedError.Title" ),
            exception );

        }
      } else {
        MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
        mb
          .setMessage( BaseMessages.getString(
            PKG, "RepositoryExplorerDialog.Directory.Create.AlreadyExists.Message1" )
            + newdir + BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Create.AlreadyExists.Message2" )
            + repdir.getPath()
            + BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Create.AlreadyExists.Message3" ) );
        mb
          .setText( BaseMessages
            .getString( PKG, "RepositoryExplorerDialog.Directory.Create.AlreadyExists.Title" ) );
        mb.open();
      }
    }
  }

  public void delDirectory( TreeItem ti, RepositoryDirectoryInterface repdir ) {
    try {
      rep.deleteRepositoryDirectory( repdir );
      refreshTree();
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Delete.ErrorRemoving.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Directory.Delete.ErrorRemoving.Message1" ), e );
    }
  }

  public void exportTransformations( RepositoryDirectoryInterface root ) {
    try {
      DirectoryDialog dialog = new DirectoryDialog( shell, SWT.OPEN );
      if ( dialog.open() != null ) {
        String directory = dialog.getFilterPath();

        ObjectId[] dirids = ( ( root == null ) ? directoryTree : root ).getDirectoryIDs();
        for ( int d = 0; d < dirids.length; d++ ) {
          RepositoryDirectoryInterface repdir = directoryTree.findDirectory( dirids[d] );
          String[] trans = rep.getTransformationNames( dirids[d], false );

          // See if the directory exists...
          File dir = new File( directory + repdir.getPath() );
          if ( !dir.exists() ) {
            dir.mkdir();
            log.logBasic( "Exporting transformation", "Created directory [" + dir.getName() + "]" );
          }

          for ( int i = 0; i < trans.length; i++ ) {
            TransMeta ti = rep.loadTransformation( trans[i], repdir, null, true, null ); // reads last version
            if ( log.isBasic() ) {
              log.logBasic(
                "Exporting transformation", "[" + trans[i] + "] in directory [" + repdir.getPath() + "]" );
            }

            String xml = XMLHandler.getXMLHeader() + ti.getXML();

            String filename =
              directory + repdir.getPath() + Const.FILE_SEPARATOR + fixFileName( trans[i] ) + ".ktr";
            File f = new File( filename );
            try {
              FileOutputStream fos = new FileOutputStream( f );
              fos.write( xml.getBytes( Const.XML_ENCODING ) );
              fos.close();
            } catch ( IOException e ) {
              throw new RuntimeException( "Exporting transformation: Couldn't create file [" + filename + "]", e );
            }
          }
        }
      }
    } catch ( Exception e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.ExportTrans.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.ExportTrans.UnexpectedError.Message" ), e );
    }

  }

  private String fixFileName( String filename ) {
    filename = filename.replace( '/', '_' ); // do something with illegal file name chars
    if ( !( "/".equals( Const.FILE_SEPARATOR ) ) ) {
      filename = Const.replace( filename, Const.FILE_SEPARATOR, "_" );
    }
    return filename;
  }

  public void exportJobs( RepositoryDirectoryInterface root ) {
    try {
      DirectoryDialog dialog = new DirectoryDialog( shell, SWT.OPEN );
      if ( dialog.open() != null ) {
        String directory = dialog.getFilterPath();

        ObjectId[] dirids = ( ( null == root ) ? directoryTree : root ).getDirectoryIDs();
        for ( int d = 0; d < dirids.length; d++ ) {
          RepositoryDirectoryInterface repdir = directoryTree.findDirectory( dirids[d] );
          String[] jobs = rep.getJobNames( dirids[d], false );

          // See if the directory exists...
          File dir = new File( directory + repdir.getPath() );
          if ( !dir.exists() ) {
            dir.mkdir();
            log.logBasic( "Exporting Jobs", "Created directory [" + dir.getName() + "]" );
          }

          for ( int i = 0; i < jobs.length; i++ ) {
            JobMeta ji = rep.loadJob( jobs[i], repdir, null, null ); // reads last version
            log.logBasic( "Exporting Jobs", "[" + jobs[i] + "] in directory [" + repdir.getPath() + "]" );

            String xml = XMLHandler.getXMLHeader() + ji.getXML();

            String filename =
              directory + repdir.getPath() + Const.FILE_SEPARATOR + fixFileName( jobs[i] ) + ".kjb";
            File f = new File( filename );
            try {
              FileOutputStream fos = new FileOutputStream( f );
              fos.write( xml.getBytes( Const.XML_ENCODING ) );
              fos.close();
            } catch ( IOException e ) {
              throw new RuntimeException( "Exporting jobs: Couldn't create file [" + filename + "]", e );
            }
          }
        }
      }
    } catch ( Exception e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.ExportJobs.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.ExportJobs.UnexpectedError.Message" ), e );
    }
  }

  public void exportAll( RepositoryDirectoryInterface dir ) {
    FileDialog dialog = Spoon.getInstance().getExportFileDialog(); // new FileDialog( shell, SWT.SAVE | SWT.SINGLE );
    if ( dialog.open() == null ) {
      return;
    }

    String filename = dialog.getFilterPath() + Const.FILE_SEPARATOR + dialog.getFileName();

    // check if file is exists
    MessageBox box = RepositoryExportProgressDialog.checkIsFileIsAcceptable( shell, log, filename );
    int answer = ( box == null ) ? SWT.OK : box.open();
    if ( answer != SWT.OK ) {
      // seems user don't want to overwrite file...
      return;
    }

    if ( log.isBasic() ) {
      log.logBasic( "Exporting All", "Export objects to file [" + filename + "]" ); //$NON-NLS-3$
    }

    // check file is not empty
    RepositoryExportProgressDialog repd = new RepositoryExportProgressDialog( shell, rep, dir, filename );
    repd.open();
  }

  public void importAll() {
    FileDialog dialog = new FileDialog( shell, SWT.OPEN | SWT.MULTI );
    if ( dialog.open() != null ) {
      // Ask for a destination in the repository...
      //
      SelectDirectoryDialog sdd = new SelectDirectoryDialog( shell, SWT.NONE, rep );
      RepositoryDirectoryInterface baseDirectory = sdd.open();
      
      if ( baseDirectory != null ) {
        // Finally before importing, ask for a version comment (if applicable)
        //
        String versionComment = null;
        boolean versionOk = false;
        while ( !versionOk ) {
          versionComment =
            RepositorySecurityUI.getVersionComment( shell, rep, "Import of files into ["
              + baseDirectory.getPath() + "]", "", true );

          // if the version comment is null, the user hit cancel, exit.
          if ( rep != null
            && rep.getSecurityProvider() != null && versionComment == null ) {
            return;
          }
          if ( Const.isEmpty( versionComment ) && rep.getSecurityProvider().isVersionCommentMandatory() ) {
            if ( !RepositorySecurityUI.showVersionCommentMandatoryDialog( shell ) ) {
              versionOk = true;
            }
          } else {
            versionOk = true;
          }
        }

        String[] filenames = dialog.getFileNames();
        if ( filenames.length > 0 ) {
          RepositoryImportProgressDialog ripd =
            new RepositoryImportProgressDialog(
              shell, SWT.NONE, rep, dialog.getFilterPath(), filenames, baseDirectory, versionComment );
          ripd.open();

          refreshTree();
        }

      }
    }
  }

  private int getItemCategory( TreeItem ti ) {
    int cat = ITEM_CATEGORY_NONE;

    int level = ConstUI.getTreeLevel( ti );
    String[] path = ConstUI.getTreeStrings( ti );

    String item = "";
    String parent = "";

    if ( ti != null ) {
      item = ti.getText();
      if ( ti.getParentItem() != null ) {
        parent = ti.getParentItem().getText();
      }
    }

    // Level 1:
    if ( level == 0 ) {
      cat = ITEM_CATEGORY_ROOT;
    } else if ( level == 1 ) {
      if ( item.equals( STRING_USERS ) ) {
        cat = ITEM_CATEGORY_USERS_ROOT;
      } else if ( item.equals( STRING_DATABASES ) ) {
        cat = ITEM_CATEGORY_DATABASES_ROOT;
      } else if ( item.equals( STRING_PARTITIONS ) ) {
        cat = ITEM_CATEGORY_PARTITIONS_ROOT;
      } else if ( item.equals( STRING_SLAVES ) ) {
        cat = ITEM_CATEGORY_SLAVES_ROOT;
      } else if ( item.equals( STRING_CLUSTERS ) ) {
        cat = ITEM_CATEGORY_CLUSTERS_ROOT;
      } else if ( item.equals( STRING_TRANSFORMATIONS ) ) {
        cat = ITEM_CATEGORY_TRANSFORMATIONS_ROOT;
      } else if ( item.equals( STRING_JOBS ) ) {
        cat = ITEM_CATEGORY_JOBS_ROOT;
      }
    } else if ( level >= 2 ) {
      if ( parent.equals( STRING_USERS ) ) {
        cat = ITEM_CATEGORY_USER;
      } else if ( parent.equals( STRING_DATABASES ) ) {
        cat = ITEM_CATEGORY_DATABASE;
      } else if ( parent.equals( STRING_PARTITIONS ) ) {
        cat = ITEM_CATEGORY_PARTITION;
      } else if ( parent.equals( STRING_SLAVES ) ) {
        cat = ITEM_CATEGORY_SLAVE;
      } else if ( parent.equals( STRING_CLUSTERS ) ) {
        cat = ITEM_CATEGORY_CLUSTER;
      }

      final Color dircolor = GUIResource.getInstance().getColorDirectory();
      if ( path[1].equals( STRING_TRANSFORMATIONS ) ) {
        if ( ti.getForeground().equals( dircolor ) ) {
          cat = ITEM_CATEGORY_TRANSFORMATION_DIRECTORY;
        } else {
          cat = ITEM_CATEGORY_TRANSFORMATION;
        }
      } else if ( path[1].equals( STRING_JOBS ) ) {
        if ( ti.getForeground().equals( dircolor ) ) {
          cat = ITEM_CATEGORY_JOB_DIRECTORY;
        } else {
          cat = ITEM_CATEGORY_JOB;
        }
      }
    }

    return cat;
  }

  public void newSlaveServer() {
    try {
      SlaveServer slaveServer = new SlaveServer();
      SlaveServerDialog dd = new SlaveServerDialog( shell, slaveServer, rep.getSlaveServers() );
      if ( dd.open() ) {
        // See if this slave server already exists...
        ObjectId idSlave = rep.getSlaveID( slaveServer.getName() );
        if ( idSlave == null ) {
          rep.insertLogEntry( "Creating new slave server '" + slaveServer.getName() + "'" );
          rep.save( slaveServer, Const.VERSION_COMMENT_INITIAL_VERSION, null );
        } else {
          MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
          mb.setMessage( BaseMessages.getString(
            PKG, "RepositoryExplorerDialog.Slave.Create.AlreadyExists.Message" ) );
          mb.setText( BaseMessages.getString( PKG, "RepositoryExplorerDialog.Slave.Create.AlreadyExists.Title" ) );
          mb.open();
        }
        // Refresh tree...
        refreshTree();
      }
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Connection.Create.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Connection.Create.UnexpectedError.Message" ), e );
    }
  }

  public void editSlaveServer( String slaveName ) {
    try {
      ObjectId id = rep.getSlaveID( slaveName );
      SlaveServer slaveServer = rep.loadSlaveServer( id, null ); // Load the last version

      SlaveServerDialog dd = new SlaveServerDialog( shell, slaveServer );
      if ( dd.open() ) {
        rep.insertLogEntry( "Updating slave server '" + slaveServer.getName() + "'" );
        rep.save( slaveServer, Const.VERSION_COMMENT_EDIT_VERSION, null );
        if ( !slaveName.equalsIgnoreCase( slaveServer.getName() ) ) {
          refreshTree();
        }
      }
    } catch ( KettleException e ) {
      new ErrorDialog(
        shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Slave.Edit.UnexpectedError.Title" ), BaseMessages
          .getString( PKG, "RepositoryExplorerDialog.Slave.Edit.UnexpectedError.Message" )
          + slaveName + "]", e );
    }
  }

  public void delSlaveServer( String slaveName ) {
    try {
      ObjectId id = rep.getSlaveID( slaveName );
      if ( id != null ) {
        rep.deleteSlave( id );
      }

      refreshTree();
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Slave.Delete.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Slave.Delete.UnexpectedError.Message" ), e );
    }
  }

  public void newPartitionSchema() {
    try {
      PartitionSchema partitionSchema = new PartitionSchema();
      PartitionSchemaDialog dd =
        new PartitionSchemaDialog( shell, partitionSchema, rep.readDatabases(), variableSpace );
      if ( dd.open() ) {
        // See if this slave server already exists...
        ObjectId idPartitionSchema = rep.getPartitionSchemaID( partitionSchema.getName() );
        if ( idPartitionSchema == null ) {
          rep.insertLogEntry( "Creating new partition schema '" + partitionSchema.getName() + "'" );
          rep.save( partitionSchema, Const.VERSION_COMMENT_INITIAL_VERSION, null );
        } else {
          MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
          mb.setMessage( BaseMessages.getString(
            PKG, "RepositoryExplorerDialog.PartitionSchema.Create.AlreadyExists.Message" ) );
          mb.setText( BaseMessages.getString(
            PKG, "RepositoryExplorerDialog.PartitionSchema.Create.AlreadyExists.Title" ) );
          mb.open();
        }
        // Refresh tree...
        refreshTree();
      }
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.PartitionSchema.Create.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.PartitionSchema.Create.UnexpectedError.Message" ), e );
    }
  }

  public void editPartitionSchema( String partitionSchemaName ) {
    try {
      ObjectId id = rep.getPartitionSchemaID( partitionSchemaName );
      PartitionSchema partitionSchema = rep.loadPartitionSchema( id, null ); // Load the last version

      PartitionSchemaDialog dd =
        new PartitionSchemaDialog( shell, partitionSchema, rep.readDatabases(), variableSpace );
      if ( dd.open() ) {
        rep.insertLogEntry( "Updating partition schema '" + partitionSchema.getName() + "'" );
        rep.save( partitionSchema, Const.VERSION_COMMENT_EDIT_VERSION, null );
        if ( !partitionSchemaName.equalsIgnoreCase( partitionSchema.getName() ) ) {
          refreshTree();
        }
      }
    } catch ( KettleException e ) {
      new ErrorDialog(
        shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.PartitionSchema.Edit.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.PartitionSchema.Edit.UnexpectedError.Message" )
          + partitionSchemaName + "]", e );
    }
  }

  public void delPartitionSchema( String partitionSchemaName ) {
    try {
      ObjectId id = rep.getPartitionSchemaID( partitionSchemaName );
      if ( id != null ) {
        rep.deletePartitionSchema( id );
      }

      refreshTree();
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.PartitionSchema.Delete.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.PartitionSchema.Delete.UnexpectedError.Message" ), e );
    }
  }

  public void newCluster() {
    try {
      ClusterSchema cluster = new ClusterSchema();
      ClusterSchemaDialog dd = new ClusterSchemaDialog( shell, cluster, rep.getSlaveServers() );
      if ( dd.open() ) {
        // See if this slave server already exists...
        ObjectId idCluster = rep.getClusterID( cluster.getName() );
        if ( idCluster == null ) {
          rep.insertLogEntry( "Creating new cluster '" + cluster.getName() + "'" );
          rep.save( cluster, Const.VERSION_COMMENT_INITIAL_VERSION, null );
        } else {
          MessageBox mb = new MessageBox( shell, SWT.ICON_ERROR | SWT.OK );
          mb.setMessage( BaseMessages.getString(
            PKG, "RepositoryExplorerDialog.Cluster.Create.AlreadyExists.Message" ) );
          mb
            .setText( BaseMessages
              .getString( PKG, "RepositoryExplorerDialog.Cluster.Create.AlreadyExists.Title" ) );
          mb.open();
        }
        // Refresh tree...
        refreshTree();
      }
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Cluster.Create.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Cluster.Create.UnexpectedError.Message" ), e );
    }
  }

  public void editCluster( String clusterName ) {
    try {
      ObjectId id = rep.getClusterID( clusterName );
      ClusterSchema cluster = rep.loadClusterSchema( id, rep.getSlaveServers(), null ); // Load the last version

      ClusterSchemaDialog dd = new ClusterSchemaDialog( shell, cluster, rep.getSlaveServers() );
      if ( dd.open() ) {
        rep.insertLogEntry( "Updating cluster '" + cluster.getName() + "'" );
        rep.save( cluster, Const.VERSION_COMMENT_EDIT_VERSION, null );
        if ( !clusterName.equalsIgnoreCase( cluster.getName() ) ) {
          refreshTree();
        }
      }
    } catch ( KettleException e ) {
      //CHECKSTYLE:LineLength:OFF
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Cluster.Edit.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Cluster.Edit.UnexpectedError.Message" ) + clusterName + "]", e );
    }
  }

  public void delCluster( String clusterName ) {
    try {
      ObjectId id = rep.getClusterID( clusterName );
      if ( id != null ) {
        rep.deleteClusterSchema( id );
      }

      refreshTree();
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Cluster.Delete.UnexpectedError.Title" ),
        BaseMessages.getString( PKG, "RepositoryExplorerDialog.Cluster.Delete.UnexpectedError.Message" ), e );
    }
  }

  protected RepositoryRevisionBrowserDialogInterface getVersionBrowserDialog(
    final RepositoryElementInterface element ) throws Exception {
    return getVersionBrowserDialog( shell, rep, element );
  }

  public static final RepositoryRevisionBrowserDialogInterface getVersionBrowserDialog( Shell shell,
    Repository repository, final RepositoryElementInterface element ) throws Exception {

    String className = repository.getRepositoryMeta().getRevisionBrowserDialogClassName();
    PluginRegistry registry = PluginRegistry.getInstance();
    PluginInterface plugin =
      registry.getPlugin( RepositoryPluginType.class, repository.getRepositoryMeta().getId() );
    Class<? extends RepositoryRevisionBrowserDialogInterface> dialogClass = registry.getClass( plugin, className );
    Constructor<?> constructor =
      dialogClass.getConstructor( Shell.class, Integer.TYPE, Repository.class, RepositoryElementInterface.class );
    return (RepositoryRevisionBrowserDialogInterface) constructor.newInstance( new Object[] {
      shell, Integer.valueOf( SWT.NONE ), repository, element, } );
  }
}
