/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2016 by Pentaho : http://www.pentaho.com
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

package org.pentaho.di.ui.repository.repositoryexplorer.controllers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.common.annotations.VisibleForTesting;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.repository.repositoryexplorer.ContextChangeVetoer;
import org.pentaho.di.ui.repository.repositoryexplorer.ContextChangeVetoer.TYPE;
import org.pentaho.di.ui.repository.repositoryexplorer.ContextChangeVetoerCollection;
import org.pentaho.di.ui.repository.repositoryexplorer.ControllerInitializationException;
import org.pentaho.di.ui.repository.repositoryexplorer.IUISupportController;
import org.pentaho.di.ui.repository.repositoryexplorer.RepositoryExplorer;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIObjectCreationException;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIObjectRegistry;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryContent;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryDirectory;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryObject;
import org.pentaho.di.ui.repository.repositoryexplorer.model.UIRepositoryObjects;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.XulException;
import org.pentaho.ui.xul.binding.Binding;
import org.pentaho.ui.xul.binding.BindingConvertor;
import org.pentaho.ui.xul.binding.BindingFactory;
import org.pentaho.ui.xul.components.XulConfirmBox;
import org.pentaho.ui.xul.components.XulMessageBox;
import org.pentaho.ui.xul.components.XulPromptBox;
import org.pentaho.ui.xul.containers.XulTree;
import org.pentaho.ui.xul.dnd.DropEvent;
import org.pentaho.ui.xul.impl.AbstractXulEventHandler;
import org.pentaho.ui.xul.swt.SwtBindingFactory;
import org.pentaho.ui.xul.swt.custom.DialogConstant;
import org.pentaho.ui.xul.swt.tags.SwtDialog;
import org.pentaho.ui.xul.util.XulDialogCallback;

/**
 *
 * This is the XulEventHandler for the browse panel of the repository explorer. It sets up the bindings for browse
 * functionality.
 *
 */
public class BrowseController extends AbstractXulEventHandler implements IUISupportController, IBrowseController {

  private static Class<?> PKG = RepositoryExplorer.class; // for i18n purposes, needed by Translator2!!

  protected UIRepositoryDirectory repoDir;

  protected XulTree folderTree;

  protected XulTree fileTable;

  protected UIRepositoryDirectory repositoryDirectory;

  protected ContextChangeVetoerCollection contextChangeVetoers;

  protected BindingFactory bf;

  protected Binding directoryBinding, selectedItemsBinding;

  protected List<UIRepositoryDirectory> selectedFolderItems;

  protected List<UIRepositoryObject> selectedFileItems;

  protected List<UIRepositoryDirectory> repositoryDirectories;

  protected Repository repository;

  List<UIRepositoryObject> repositoryObjects;

  List<UIRepositoryObject> repositoryItems;

  protected MainController mainController;

  protected XulMessageBox messageBox;

  protected XulConfirmBox confirmBox;

  private Shell shell;

  /**
   * Allows for lookup of a UIRepositoryDirectory by ObjectId. This allows the reuse of instances that are inside a UI
   * tree.
   */
  protected Map<ObjectId, UIRepositoryDirectory> dirMap;

  private PropertyChangeListener fileChildrenListener = new PropertyChangeListener() {

    public void propertyChange( PropertyChangeEvent arg0 ) {
      try {
        firePropertyChange( "selectedRepoDirChildren", null, getSelectedRepoDirChildren() );
      } catch ( Exception e ) {
        throw new RuntimeException( e );
      }
    }

  };

  public BrowseController() {
  }

  // begin PDI-3326 hack

  private void fireRepositoryDirectoryChange() {
    firePropertyChange( "repositoryDirectory", null, repositoryDirectory );
  }

  private void fireFoldersAndItemsChange( List<UIRepositoryDirectory> previousValue,
      UIRepositoryObjects previousRepoObjects ) {
    firePropertyChange( "repositoryDirectories", previousValue, getRepositoryDirectories() );
    firePropertyChange( "selectedRepoDirChildren", previousRepoObjects, getSelectedRepoDirChildren() );
  }

  // end PDI-3326 hack

  public void init( Repository repository ) throws ControllerInitializationException {
    try {
      this.repository = repository;

      mainController = (MainController) this.getXulDomContainer().getEventHandler( "mainController" );
      try {
        this.repositoryDirectory =
            UIObjectRegistry.getInstance().constructUIRepositoryDirectory( repository.loadRepositoryDirectoryTree(),
                null, repository );
      } catch ( UIObjectCreationException uoe ) {
        this.repositoryDirectory =
            new UIRepositoryDirectory( repository.loadRepositoryDirectoryTree(), null, repository );
      }
      dirMap = new HashMap<ObjectId, UIRepositoryDirectory>();
      populateDirMap( repositoryDirectory );

      bf = new SwtBindingFactory();
      bf.setDocument( this.getXulDomContainer().getDocumentRoot() );
      messageBox = (XulMessageBox) document.createElement( "messagebox" );
      createBindings();
    } catch ( Exception e ) {
      throw new ControllerInitializationException( e );
    }
  }

  protected void createBindings() {
    shell = ( (SwtDialog) document.getElementById( "repository-explorer-dialog" ) ).getShell();

    folderTree = (XulTree) document.getElementById( "folder-tree" );
    fileTable = (XulTree) document.getElementById( "file-table" );

    if ( !repositoryDirectory.isVisible() ) {
      folderTree.setHiddenrootnode( true );
    } else {
      folderTree.setHiddenrootnode( false );
    }
    BindingConvertor<List<?>, Boolean> checkIfMultipleItemsAreSelected = new BindingConvertor<List<?>, Boolean>() {

      @Override
      public Boolean sourceToTarget( List<?> value ) {
        return value != null && value.size() == 1 && value.get( 0 ) != null;
      }

      @Override
      public List<?> targetToSource( Boolean value ) {
        return null;
      }
    };
    bf.setBindingType( Binding.Type.ONE_WAY );
    bf.createBinding( fileTable, "selectedItems", "file-context-rename", "!disabled", checkIfMultipleItemsAreSelected );
    bf.createBinding( fileTable, "selectedItems", this, "selectedFileItems" );

    // begin PDI-3326 hack

    PropertyChangeListener childrenListener = new PropertyChangeListener() {

      public void propertyChange( PropertyChangeEvent evt ) {
        fireRepositoryDirectoryChange();
      }

    };

    repositoryDirectory.addPropertyChangeListener( "children", childrenListener );

    // end PDI-3326 hack

    directoryBinding = createDirectoryBinding();

    // Bind the selected index from the folder tree to the list of repository objects in the file table.
    bf.setBindingType( Binding.Type.ONE_WAY );

    bf.createBinding( folderTree, "selectedItems", this, "selectedFolderItems" );

    bf.setBindingType( Binding.Type.ONE_WAY );
    selectedItemsBinding = bf.createBinding( this, "selectedRepoDirChildren", fileTable, "elements" );

    // bindings can be added here in subclasses
    doCreateBindings();

    try {
      // Fires the population of the repository tree of folders.
      directoryBinding.fireSourceChanged();
    } catch ( Exception e ) {
      // convert to runtime exception so it bubbles up through the UI
      throw new RuntimeException( e );
    }

    try {
      // Set the initial selected directory as the users home directory
      RepositoryDirectoryInterface homeDir = repository.getUserHomeDirectory();
      int currentDir = 0;
      String[] homePath = homeDir == null ? null : homeDir.getPathArray();
      if ( homePath != null ) {
        UIRepositoryDirectory tempRoot = repositoryDirectory;

        // Check to see if the first item in homePath is the root directory
        if ( homePath.length > 0 && tempRoot.getName().equalsIgnoreCase( homePath[currentDir] ) ) {
          if ( homePath.length == 1 ) {
            // The home directory is home root
            setSelectedFolderItems( Arrays.asList( tempRoot ) );
          }
          // We have used the first element. Increment to the next
          currentDir++;
        }

        // Traverse the tree until we find our destination
        for ( ; currentDir < homePath.length; currentDir++ ) {
          for ( UIRepositoryObject uiObj : tempRoot ) {
            if ( uiObj instanceof UIRepositoryDirectory ) {
              if ( uiObj.getName().equalsIgnoreCase( homePath[currentDir] ) ) {
                // We have a match. Let's move on to the next
                tempRoot = (UIRepositoryDirectory) uiObj;
                break;
              }
            }
          }
        }
        // If we have traversed as many directories as there are in the path, we have found the directory
        if ( homePath.length == currentDir ) {
          setSelectedFolderItems( Arrays.asList( tempRoot ) );
          folderTree.setSelectedItems( this.selectedFolderItems );
        }
      }
    } catch ( Exception e ) {
      throw new RuntimeException( e );
    }
  }

  protected void doCreateBindings() {
  }

  protected Binding createDirectoryBinding() {
    bf.setBindingType( Binding.Type.ONE_WAY );
    return bf.createBinding( this, "repositoryDirectory", folderTree, "elements" );
  }

  public String getName() {
    return "browseController";
  }

  public UIRepositoryDirectory getRepositoryDirectory() {
    return repositoryDirectory;
  }

  protected void populateDirMap( UIRepositoryDirectory repDir ) {
    dirMap.put( repDir.getObjectId(), repDir );
    for ( UIRepositoryObject obj : repDir ) {
      if ( obj instanceof UIRepositoryDirectory ) {
        populateDirMap( (UIRepositoryDirectory) obj );
      }
    }
  }

  public void expandAllFolders() {
    folderTree.expandAll();
  }

  public void collapseAllFolders() {
    folderTree.collapseAll();
  }

  public void openContent() {
    Collection<UIRepositoryObject> content = fileTable.getSelectedItems();
    openContent( content.toArray() );
  }

  public void openContent( Object[] items ) {
    if ( ( items != null ) && ( items.length > 0 ) ) {
      for ( Object o : items ) {
        if ( o instanceof UIRepositoryDirectory ) {
          ( (UIRepositoryDirectory) o ).toggleExpanded();
          List<Object> selectedFolder = new ArrayList<Object>();
          selectedFolder.add( o );
          folderTree.setSelectedItems( selectedFolder );
        } else if ( ( mainController != null && mainController.getCallback() != null )
            && ( o instanceof UIRepositoryContent ) ) {

          try {
            mainController.getCallback().open( (UIRepositoryContent) o, null );
          } catch ( Exception e ) {
            mainController.handleLostRepository( e );
          }
          // TODO: fire request to close dialog

        }
      }
    }
  }

  public void renameContent() throws Exception {
    try {
      Collection<UIRepositoryContent> content = fileTable.getSelectedItems();
      UIRepositoryObject contentToRename = content.iterator().next();
      renameRepositoryObject( contentToRename );
      if ( contentToRename instanceof UIRepositoryDirectory ) {
        directoryBinding.fireSourceChanged();
      }
      selectedItemsBinding.fireSourceChanged();
    } catch ( Throwable th ) {
      if ( mainController == null || !mainController.handleLostRepository( th ) ) {
        messageBox.setTitle( BaseMessages.getString( PKG, "Dialog.Error" ) );
        messageBox.setAcceptLabel( BaseMessages.getString( PKG, "Dialog.Ok" ) );
        messageBox.setMessage( BaseMessages.getString( PKG, th.getLocalizedMessage() ) );
        messageBox.open();
      }
    }
  }

  protected void confirm( String title, String message, final Callable<Void> onAccept ) throws XulException {
    confirmBox = (XulConfirmBox) document.createElement( "confirmbox" );
    confirmBox.setTitle( BaseMessages.getString( PKG, title ) );
    confirmBox.setMessage( BaseMessages.getString( PKG, message ) );
    confirmBox.setAcceptLabel( BaseMessages.getString( PKG, "Dialog.Ok" ) );
    confirmBox.setCancelLabel( BaseMessages.getString( PKG, "Dialog.Cancel" ) );
    confirmBox.addDialogCallback( new XulDialogCallback<Object>() {

      public void onClose( XulComponent sender, Status returnCode, Object retVal ) {
        if ( returnCode == Status.ACCEPT ) {
          try {
            onAccept.call();
          } catch ( Exception e ) {
            if ( mainController == null || !mainController.handleLostRepository( e ) ) {
              messageBox.setTitle( BaseMessages.getString( PKG, "Dialog.Error" ) );
              messageBox.setAcceptLabel( BaseMessages.getString( PKG, "Dialog.Ok" ) );
              messageBox.setMessage( BaseMessages.getString( PKG, e.getLocalizedMessage() ) );
              messageBox.open();
            }
          }
        }
      }

      public void onError( XulComponent sender, Throwable t ) {
        throw new RuntimeException( t );
      }
    } );
    confirmBox.open();
  }

  public void deleteContent() throws Exception {
    for ( Object object : fileTable.getSelectedItems() ) {
      if ( object instanceof UIRepositoryObject ) {
        final UIRepositoryObject repoObject = (UIRepositoryObject) object;
        Callable<Void> deleteCallable = new Callable<Void>() {

          @Override
          public Void call() throws Exception {
            deleteContent( repoObject );
            return null;
          }
        };
        // If content to be deleted is a folder we will display a warning message
        // notwithstanding the folder is empty or not. If you choose to delete this folder, all its
        // item(s) will be lost. If the user accept this, then we will delete that folder
        // otherwise we will end this method call
        if ( repoObject instanceof UIRepositoryDirectory ) {
          confirm( "BrowseController.DeleteNonEmptyFolderWarningTitle",
            "BrowseController.DeleteFolderWarningMessage", deleteCallable );
        } else {
          confirm( "BrowseController.DeleteFileWarningTitle",
            "BrowseController.DeleteFileWarningMessage", deleteCallable );
        }
      }
    }
  }

  private String newName = null;

  protected void deleteContent( UIRepositoryObject repoObject ) throws Exception {
    repoObject.delete();
    if ( repoObject instanceof UIRepositoryDirectory ) {
      directoryBinding.fireSourceChanged();
      if ( repoDir != null ) {
        repoDir.refresh();
      }
    }
    selectedItemsBinding.fireSourceChanged();
  }

  public void createFolder() throws Exception {

    try {

      Collection<UIRepositoryDirectory> directories = folderTree.getSelectedItems();
      if ( directories == null || directories.size() == 0 ) {
        return;
      }
      UIRepositoryDirectory selectedFolder = directories.iterator().next();

      // First, ask for a name for the folder
      XulPromptBox prompt = promptForName( null );
      prompt.addDialogCallback( new XulDialogCallback<String>() {
        public void onClose( XulComponent component, Status status, String value ) {
          if ( status == Status.ACCEPT ) {
            newName = value;
          } else {
            newName = null;
          }
        }

        public void onError( XulComponent component, Throwable err ) {
          throw new RuntimeException( err );
        }
      } );

      prompt.open();

      if ( newName != null ) {
        if ( selectedFolder == null ) {
          selectedFolder = repositoryDirectory;
        }
        //Do an explicit check here to see if the folder already exists in the ui
        //This is to prevent a double message being sent in case the folder does
        //not exist in the ui but does exist in the repo (PDI-5202)
        boolean folderExistsInUI = selectedFolder.contains( newName );
        if ( folderExistsInUI ) {
          throw new Exception(
            BaseMessages.getString(
              PKG,
              "BrowserController.DirAlreadyExistsInUI",
              newName
            )
          );
        }
        //PDI-5202
        String newNameInRepo = selectedFolder.checkDirNameExistsInRepo( newName );
        if ( newNameInRepo != null ) {
          messageBox.setTitle( BaseMessages.getString( PKG, "Dialog.Warning" ) );
          messageBox.setAcceptLabel( BaseMessages.getString( PKG, "Dialog.Ok" ) );
          messageBox.setMessage(
            BaseMessages.getString(
              PKG,
              "BrowserController.DirAlreadyExistsInRepository",
              newNameInRepo
            )
          );
          messageBox.open();
          newName = newNameInRepo;
        }
        UIRepositoryDirectory newDir = selectedFolder.createFolder( newName );
        dirMap.put( newDir.getObjectId(), newDir );

        directoryBinding.fireSourceChanged();
        selectedItemsBinding.fireSourceChanged();

        this.folderTree.setSelectedItems( Collections.singletonList( selectedFolder ) );

      }
      newName = null;
    } catch ( Exception e ) {
      if ( mainController == null || !mainController.handleLostRepository( e ) ) {
        messageBox.setTitle( BaseMessages.getString( PKG, "Dialog.Error" ) );
        messageBox.setAcceptLabel( BaseMessages.getString( PKG, "Dialog.Ok" ) );
        messageBox.setMessage( BaseMessages.getString( PKG, e.getLocalizedMessage() ) );
        messageBox.open();
      }
    }
  }

  public void exportFolder() throws Exception {
    for ( Object object : folderTree.getSelectedItems() ) {

      if ( object instanceof UIRepositoryDirectory ) {
        repoDir = (UIRepositoryDirectory) object;

        // Export the directories one by one...
        //
        if ( Spoon.getInstance().exportRepositoryDirectory( repoDir.getDirectory() ) ) {
          return;
        }
      }
    }
  }

  public void deleteFolder() throws Exception {
    UIRepositoryDirectory newSelectedItem = null;
    for ( Object object : folderTree.getSelectedItems() ) {
      if ( object instanceof UIRepositoryDirectory ) {
        repoDir = (UIRepositoryDirectory) object;
        newSelectedItem = repoDir.getParent();

        // If content to be deleted is a folder we will display a warning message
        // notwithstanding the folder is empty or not. If you choose to delete this folder, all its
        // item(s) will be lost. If the user accept this, then we will delete that folder
        // otherwise we will end this method call
        confirmBox = (XulConfirmBox) document.createElement( "confirmbox" );
        confirmBox.setTitle( BaseMessages.getString( PKG, "BrowseController.DeleteNonEmptyFolderWarningTitle" ) );
        confirmBox.setMessage( BaseMessages
          .getString( PKG, "BrowseController.DeleteFolderWarningMessage" ) );
        confirmBox.setAcceptLabel( BaseMessages.getString( PKG, "Dialog.Ok" ) );
        confirmBox.setCancelLabel( BaseMessages.getString( PKG, "Dialog.Cancel" ) );
        confirmBox.addDialogCallback( new XulDialogCallback<Object>() {

          public void onClose( XulComponent sender, Status returnCode, Object retVal ) {
            if ( returnCode == Status.ACCEPT ) {
              try {
                deleteFolder( repoDir );
              } catch ( Exception e ) {
                if ( mainController == null || !mainController.handleLostRepository( e ) ) {
                  new ErrorDialog( shell, BaseMessages.getString( PKG, "RepositoryExplorerDialog.ErrorDialog.Title" ),
                      BaseMessages.getString( PKG, "RepositoryExplorerDialog.ErrorDialog.Message" ), e );
                }
              }
            }
          }

          public void onError( XulComponent sender, Throwable t ) {
            throw new RuntimeException( t );
          }
        } );
        confirmBox.open();
        break;
      } else {
        deleteFolder( repoDir );
      }
    }

    // since old selected item is the now deleted one, set the parent as the selected item
    if ( newSelectedItem != null ) {
      folderTree.setSelectedItems( Arrays.asList( newSelectedItem ) );
    }
  }

  protected void deleteFolder( UIRepositoryDirectory repoDir ) throws Exception {
    repoDir.delete();
    directoryBinding.fireSourceChanged();
    selectedItemsBinding.fireSourceChanged();
    repoDir.refresh();
  }

  public void renameFolder() throws Exception {
    try {
      Collection<UIRepositoryDirectory> directory = folderTree.getSelectedItems();
      final UIRepositoryDirectory toRename = directory.iterator().next();
      renameRepositoryObject( toRename );
      directoryBinding.fireSourceChanged();
      selectedItemsBinding.fireSourceChanged();
    } catch ( Throwable th ) {
      if ( mainController == null || !mainController.handleLostRepository( th ) ) {
        messageBox.setTitle( BaseMessages.getString( PKG, "Dialog.Error" ) );
        messageBox.setAcceptLabel( BaseMessages.getString( PKG, "Dialog.Ok" ) );
        messageBox.setMessage( BaseMessages.getString( PKG, th.getLocalizedMessage() ) );
        messageBox.open();
      }
    }
  }

  protected void renameRepositoryObject( final UIRepositoryObject object ) throws XulException {
    XulPromptBox prompt = promptForName( object );
    prompt.addDialogCallback( new XulDialogCallback<String>() {
      public void onClose( XulComponent component, Status status, String value ) {
        if ( status == Status.ACCEPT ) {
          try {
            object.setName( value );
          } catch ( Exception e ) {
            if ( mainController == null || !mainController.handleLostRepository( e ) ) {
              throw new RuntimeException( e );
            }
          }
        }
      }

      public void onError( XulComponent component, Throwable err ) {
        throw new RuntimeException( err );
      }
    } );

    prompt.open();
  }

  protected XulPromptBox promptForName( final UIRepositoryObject object ) throws XulException {
    XulPromptBox prompt = (XulPromptBox) document.createElement( "promptbox" );
    String currentName =
        ( object == null ) ? BaseMessages.getString( PKG, "BrowserController.NewFolder" ) : object.getName();

    prompt.setTitle( BaseMessages.getString( PKG, "BrowserController.Name" ).concat( currentName ) );
    prompt.setButtons( new DialogConstant[] { DialogConstant.OK, DialogConstant.CANCEL } );

    prompt.setMessage( BaseMessages.getString( PKG, "BrowserController.NameLabel" ).concat( currentName ) );
    prompt.setValue( currentName );
    return prompt;
  }

  // Object being dragged from the hierarchical folder tree
  public void onDragFromGlobalTree( DropEvent event ) {
    event.setAccepted( true );
  }

  // Object being dragged from the file listing table
  public void onDragFromLocalTable( DropEvent event ) {
    event.setAccepted( true );
  }

  public void onDrop( DropEvent event ) {
    boolean result = false;
    try {
      List<Object> dirList = new ArrayList<Object>();
      List<UIRepositoryObject> moveList = new ArrayList<UIRepositoryObject>();
      UIRepositoryDirectory targetDirectory = null;

      if ( event.getDropParent() != null && event.getDropParent() instanceof UIRepositoryDirectory ) {
        targetDirectory = (UIRepositoryDirectory) event.getDropParent();
        if ( event.getDataTransfer().getData().size() > 0 ) {
          for ( Object o : event.getDataTransfer().getData() ) {
            if ( o instanceof UIRepositoryObject ) {
              moveList.add( (UIRepositoryObject) o );

              // Make sure only Folders are copied to the Directory Tree
              if ( o instanceof UIRepositoryDirectory ) {
                dirList.add( o );
              }
              result = true;
            }
          }
        }
      }

      if ( result == true ) {
        List<UIRepositoryObject> collisionObjects = new ArrayList<UIRepositoryObject>();

        // Check for overwriting
        for ( UIRepositoryObject newChild : moveList ) {
          for ( UIRepositoryObject currChild : targetDirectory.getRepositoryObjects() ) {
            if ( ( currChild instanceof UIRepositoryDirectory ) && ( newChild instanceof UIRepositoryDirectory )
                && ( currChild.getName().equalsIgnoreCase( newChild.getName() ) ) ) {
              messageBox.setTitle( BaseMessages.getString( PKG, "Dialog.Error" ) );
              messageBox.setAcceptLabel( BaseMessages.getString( PKG, "Dialog.Ok" ) );
              messageBox.setMessage( BaseMessages.getString( PKG,
                  "BrowseController.UnableToMove.DirectoryAlreadyExists", currChild.getPath() ) );
              messageBox.open();
              result = false;
              break;
            } else if ( !( currChild instanceof UIRepositoryDirectory )
                && ( currChild.getType().equalsIgnoreCase( newChild.getType() ) )
                && ( currChild.getName().equalsIgnoreCase( newChild.getName() ) ) ) {
              collisionObjects.add( currChild );
            }
          }
          if ( !result ) {
            break;
          }
        }

        // Prompt to overwrite
        if ( result && collisionObjects.size() > 0 ) {
          FileOverwriteDialogController fileOverwriteDialog =
              FileOverwriteDialogController.getInstance( getXulDomContainer().getOuterContext() instanceof Shell
                  ? (Shell) getXulDomContainer().getOuterContext() : null, collisionObjects );
          fileOverwriteDialog.show();
          if ( fileOverwriteDialog.isOverwriteFiles() ) {
            // Delete the files before moving
            for ( UIRepositoryObject o : collisionObjects ) {
              o.delete();
            }
          } else {
            // We are not moving the files
            result = false;
          }
        }

        // Make sure we are still moving the files
        if ( result ) {
          moveFiles( moveList, targetDirectory );
          // Set UI objects to appear in folder directory
          event.getDataTransfer().setData( dirList );
        }
      }
    } catch ( Exception e ) {
      if ( mainController == null || !mainController.handleLostRepository( e ) ) {
        result = false;
        event.setAccepted( false );
        messageBox.setTitle( BaseMessages.getString( PKG, "Dialog.Error" ) );
        messageBox.setAcceptLabel( BaseMessages.getString( PKG, "Dialog.Ok" ) );
        messageBox.setMessage(
            BaseMessages.getString( PKG, "BrowseController.UnableToMove", e.getLocalizedMessage() ) );
        messageBox.open();
      }
    }

    event.setAccepted( result );
  }

  protected void moveFiles( List<UIRepositoryObject> objects, UIRepositoryDirectory targetDirectory ) throws Exception {
    // Perform move
    for ( UIRepositoryObject o : objects ) {
      o.move( targetDirectory );
    }
  }

  private void messageBox( String message ) {
    messageBox( "Dialog.Error", "Dialog.Ok", message );
  }

  private void messageBox( String title, String acceptLabel, String message ) {
    messageBox.setTitle( BaseMessages.getString( PKG, title ) );
    messageBox.setAcceptLabel( BaseMessages.getString( PKG, acceptLabel ) );
    messageBox.setMessage( message );
    messageBox.open();
  }

  public void onDoubleClick( Object[] selectedItems ) {
    openContent( selectedItems );
  }

  public List<UIRepositoryDirectory> getSelectedFolderItems() {
    return selectedFolderItems;
  }

  public void setSelectedFolderItems( List<UIRepositoryDirectory> selectedFolderItems ) {
    if ( !compareFolderList( selectedFolderItems, this.selectedFolderItems ) ) {
      List<TYPE> pollResults = pollContextChangeVetoResults();
      if ( !contains( TYPE.CANCEL, pollResults ) ) {
        this.selectedFolderItems = selectedFolderItems;
        setRepositoryDirectories( selectedFolderItems );
      } else if ( contains( TYPE.CANCEL, pollResults ) ) {
        folderTree.setSelectedItems( this.selectedFolderItems );
        fileTable.setSelectedItems( this.selectedFileItems );
      }
    } else {
      setRepositoryDirectories( selectedFolderItems );
    }
  }

  public List<UIRepositoryObject> getSelectedFileItems() {
    return selectedFileItems;
  }

  public void setSelectedFileItems( List<UIRepositoryObject> selectedFileItems ) {
    if ( !compareFileList( selectedFileItems, this.selectedFileItems ) ) {
      List<TYPE> pollResults = pollContextChangeVetoResults();
      if ( !contains( TYPE.CANCEL, pollResults ) ) {
        this.selectedFileItems = selectedFileItems;
        setRepositoryObjects( selectedFileItems );
        setRepositoryItems( selectedFileItems );
      } else if ( contains( TYPE.CANCEL, pollResults ) ) {
        fileTable.setSelectedItems( this.selectedFileItems );
      }
    } else {
      setRepositoryItems( selectedFileItems );
    }
  }

  public Binding getSelectedItemsBinding() {
    return selectedItemsBinding;
  }

  public void setSelectedItemsBinding( Binding selectedItemsBinding ) {
    this.selectedItemsBinding = selectedItemsBinding;
  }

  public void setRepositoryObjects( List<UIRepositoryObject> selectedFileItems ) {
    this.repositoryObjects = selectedFileItems;
    firePropertyChange( "repositoryObjects", null, selectedFileItems );
  }

  public List<UIRepositoryObject> getRepositoryObjects() {
    return repositoryObjects;
  }

  public void setRepositoryItems( List<UIRepositoryObject> selectedItems ) {
    this.repositoryItems = selectedItems;
    firePropertyChange( "repositoryItems", null, repositoryItems );
  }

  public List<UIRepositoryObject> getRepositoryItems() {
    return repositoryItems;
  }

  public List<UIRepositoryDirectory> getRepositoryDirectories() {
    if ( repositoryDirectories != null && repositoryDirectories.size() == 0 ) {
      return null;
    }
    return repositoryDirectories;
  }

  public void setRepositoryDirectories( List<UIRepositoryDirectory> selectedFolderItems ) {
    List<UIRepositoryDirectory> previousVal = null;
    UIRepositoryObjects previousRepoObjects = null;
    try {
      if ( repositoryDirectories != null && repositoryDirectories.size() > 0 ) {
        previousVal = new ArrayList<UIRepositoryDirectory>();
        previousVal.addAll( repositoryDirectories );
        previousRepoObjects = getSelectedRepoDirChildren();
      }

      // Remove children listener
      if ( this.repositoryDirectories != null && this.repositoryDirectories.size() > 0 ) {
        this.repositoryDirectories.get( 0 ).getRepositoryObjects().removePropertyChangeListener( fileChildrenListener );
      }

      this.repositoryDirectories = selectedFolderItems;

      // Add children Listener
      if ( this.repositoryDirectories != null && this.repositoryDirectories.size() > 0 ) {
        this.repositoryDirectories.get( 0 ).getRepositoryObjects().addPropertyChangeListener( "children",
            fileChildrenListener );

      }
    } catch ( KettleException e ) {
      if ( mainController == null || !mainController.handleLostRepository( e ) ) {
        throw new RuntimeException( e );
      }
    }
    fireFoldersAndItemsChange( previousVal, previousRepoObjects );
  }

  public UIRepositoryObjects getSelectedRepoDirChildren() {
    UIRepositoryObjects repoObjects = null;
    if ( selectedFolderItems != null && selectedFolderItems.size() > 0 ) {
      try {
        repoObjects = repositoryDirectories.get( 0 ).getRepositoryObjects();
      } catch ( KettleException e ) {
        // convert to runtime exception so it bubbles up through the UI
        if ( mainController == null || !mainController.handleLostRepository( e ) ) {
          throw new RuntimeException( e );
        }
      }
    }
    return repoObjects;
  }

  public void addContextChangeVetoer( ContextChangeVetoer listener ) {
    if ( contextChangeVetoers == null ) {
      contextChangeVetoers = new ContextChangeVetoerCollection();
    }
    contextChangeVetoers.add( listener );
  }

  public void removeContextChangeVetoer( ContextChangeVetoer listener ) {
    if ( contextChangeVetoers != null ) {
      contextChangeVetoers.remove( listener );
    }
  }

  private boolean contains( TYPE type, List<TYPE> typeList ) {
    for ( TYPE t : typeList ) {
      if ( t.equals( type ) ) {
        return true;
      }
    }
    return false;
  }

  /**
   * Fire all current {@link ContextChangeVetoer}. Every one who has added their self as a vetoer has a change to vote
   * on what should happen.
   */
  List<TYPE> pollContextChangeVetoResults() {
    if ( contextChangeVetoers != null ) {
      return contextChangeVetoers.fireContextChange();
    } else {
      List<TYPE> returnValue = new ArrayList<TYPE>();
      returnValue.add( TYPE.NO_OP );
      return returnValue;
    }
  }

  boolean compareFolderList( List<UIRepositoryDirectory> rd1, List<UIRepositoryDirectory> rd2 ) {
    if ( rd1 != null && rd2 != null ) {
      if ( rd1.size() != rd2.size() ) {
        return false;
      }
      for ( int i = 0; i < rd1.size(); i++ ) {
        if ( rd1.get( i ) != null && rd2.get( i ) != null ) {
          if ( !rd1.get( i ).getName().equals( rd2.get( i ).getName() ) ) {
            return false;
          }
        }
      }
    } else {
      return false;
    }
    return true;
  }

  boolean compareFileList( List<UIRepositoryObject> ro1, List<UIRepositoryObject> ro2 ) {
    if ( ro1 != null && ro2 != null ) {
      if ( ro1.size() != ro2.size() ) {
        return false;
      }
      for ( int i = 0; i < ro1.size(); i++ ) {
        if ( ro1.get( i ) != null && ro2.get( i ) != null ) {
          if ( !ro1.get( i ).getName().equals( ro2.get( i ).getName() ) ) {
            return false;
          }
        }
      }
    } else {
      return false;
    }
    return true;
  }

  @VisibleForTesting
  void setDirMap( Map<ObjectId, UIRepositoryDirectory> dirMap ) {
    this.dirMap = dirMap;
  }

  @VisibleForTesting
  void setFolderTree( XulTree folderTree ) {
    this.folderTree = folderTree;
  }

  @VisibleForTesting
  void setRepositoryDirectory( UIRepositoryDirectory repositoryDirectory ) {
    this.repositoryDirectory = repositoryDirectory;
  }

  @VisibleForTesting
  void setDirectoryBinding( Binding directoryBinding ) {
    this.directoryBinding = directoryBinding;
  }
}
