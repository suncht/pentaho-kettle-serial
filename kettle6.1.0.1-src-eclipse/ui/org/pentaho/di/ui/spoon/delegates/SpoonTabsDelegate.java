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

package org.pentaho.di.ui.spoon.delegates;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.pentaho.di.base.AbstractMeta;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.EngineMetaInterface;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleFileException;
import org.pentaho.di.core.gui.SpoonInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.repository.ObjectRevision;
import org.pentaho.di.repository.RepositoryOperation;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.repository.RepositorySecurityUI;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.spoon.SpoonBrowser;
import org.pentaho.di.ui.spoon.TabItemInterface;
import org.pentaho.di.ui.spoon.TabMapEntry;
import org.pentaho.di.ui.spoon.TabMapEntry.ObjectType;
import org.pentaho.di.ui.spoon.job.JobGraph;
import org.pentaho.di.ui.spoon.trans.TransGraph;
import org.pentaho.ui.util.Launch;
import org.pentaho.ui.util.Launch.Status;
import org.pentaho.xul.swt.tab.TabItem;
import org.pentaho.xul.swt.tab.TabSet;

public class SpoonTabsDelegate extends SpoonDelegate {
  private static Class<?> PKG = Spoon.class; // for i18n purposes, needed by Translator2!!

  /**
   * This contains a list of the tab map entries
   */
  private List<TabMapEntry> tabMap;

  public SpoonTabsDelegate( Spoon spoon ) {
    super( spoon );
    tabMap = new ArrayList<TabMapEntry>();
  }

  public boolean tabClose( TabItem item ) throws KettleException {
    // Try to find the tab-item that's being closed.
    List<TabMapEntry> collection = new ArrayList<TabMapEntry>();
    collection.addAll( tabMap );

    boolean createPerms = !RepositorySecurityUI
        .verifyOperations( Spoon.getInstance().getShell(), Spoon.getInstance().getRepository(), false,
            RepositoryOperation.MODIFY_TRANSFORMATION, RepositoryOperation.MODIFY_JOB );

    boolean close = true;
    boolean canSave = true;
    for ( TabMapEntry entry : collection ) {
      if ( item.equals( entry.getTabItem() ) ) {
        TabItemInterface itemInterface = entry.getObject();
        if ( itemInterface.getManagedObject() != null
            && AbstractMeta.class.isAssignableFrom( itemInterface.getManagedObject().getClass() ) ) {
          canSave = !( (AbstractMeta) itemInterface.getManagedObject() ).hasMissingPlugins();
        }
        if ( canSave ) {
          // Can we close this tab? Only allow users with create content perms to save
          if ( !itemInterface.canBeClosed() && createPerms ) {
            int reply = itemInterface.showChangedWarning();
            if ( reply == SWT.YES ) {
              close = itemInterface.applyChanges();
            } else {
              if ( reply == SWT.CANCEL ) {
                close = false;
              } else {
                close = true;
              }
            }
          }
        }

        // Also clean up the log/history associated with this
        // transformation/job
        //
        if ( close ) {
          if ( entry.getObject() instanceof TransGraph ) {
            TransMeta transMeta = (TransMeta) entry.getObject().getManagedObject();
            spoon.delegates.trans.closeTransformation( transMeta );
            spoon.refreshTree();
            // spoon.refreshCoreObjects();
          } else if ( entry.getObject() instanceof JobGraph ) {
            JobMeta jobMeta = (JobMeta) entry.getObject().getManagedObject();
            spoon.delegates.jobs.closeJob( jobMeta );
            spoon.refreshTree();
            // spoon.refreshCoreObjects();
          } else if ( entry.getObject() instanceof SpoonBrowser ) {
            this.removeTab( entry );
            spoon.refreshTree();
          } else if ( entry.getObject() instanceof Composite ) {
            Composite comp = (Composite) entry.getObject();
            if ( comp != null && !comp.isDisposed() ) {
              comp.dispose();
            }
          }
        }

        break;
      }
    }

    return close;
  }

  public void removeTab( TabMapEntry tabMapEntry ) {
    for ( TabMapEntry entry : getTabs() ) {
      if ( tabMapEntry.equals( entry ) ) {
        tabMap.remove( tabMapEntry );
      }
    }
    if ( !tabMapEntry.getTabItem().isDisposed() ) {
      tabMapEntry.getTabItem().dispose();
    }
  }

  public List<TabMapEntry> getTabs() {
    List<TabMapEntry> list = new ArrayList<TabMapEntry>();
    list.addAll( tabMap );
    return list;
  }

  public TabMapEntry getTab( TabItem tabItem ) {
    for ( TabMapEntry tabMapEntry : tabMap ) {
      if ( tabMapEntry.getTabItem().equals( tabItem ) ) {
        return tabMapEntry;
      }
    }
    return null;
  }

  public EngineMetaInterface getActiveMeta() {
    TabSet tabfolder = spoon.tabfolder;
    if ( tabfolder == null ) {
      return null;
    }
    TabItem tabItem = tabfolder.getSelected();
    if ( tabItem == null ) {
      return null;
    }

    // What transformation is in the active tab?
    // TransLog, TransGraph & TransHist contain the same transformation
    //
    TabMapEntry mapEntry = getTab( tabfolder.getSelected() );
    EngineMetaInterface meta = null;
    if ( mapEntry != null ) {
      if ( mapEntry.getObject() instanceof TransGraph ) {
        meta = ( mapEntry.getObject() ).getMeta();
      }
      if ( mapEntry.getObject() instanceof JobGraph ) {
        meta = ( mapEntry.getObject() ).getMeta();
      }
    }

    return meta;
  }

  public String makeSlaveTabName( SlaveServer slaveServer ) {
    return "Slave server: " + slaveServer.getName();
  }

  public boolean addSpoonBrowser( String name, String urlString ) {
    return addSpoonBrowser( name, urlString, true, null, true );
  }

  public boolean addSpoonBrowser( String name, String urlString, boolean showControls ) {
    return addSpoonBrowser( name, urlString, true, null, showControls );
  }

  public boolean addSpoonBrowser( String name, String urlString, LocationListener listener ) {
    boolean ok = addSpoonBrowser( name, urlString, true, listener, true );
    return ok;
  }

  public boolean addSpoonBrowser( String name, String urlString, LocationListener listener, boolean showControls ) {
    return addSpoonBrowser( name, urlString, true, listener, showControls );
  }

  public boolean addSpoonBrowser( String name, String urlString, boolean isURL, LocationListener listener ) {
    return addSpoonBrowser( name, urlString, isURL, listener, true );
  }

  public boolean addSpoonBrowser( String name, String urlString, boolean isURL, LocationListener listener, boolean showControls ) {
    TabSet tabfolder = spoon.tabfolder;

    try {
      // OK, now we have the HTML, create a new browset tab.

      // See if there already is a tab for this browser
      // If no, add it
      // If yes, select that tab
      //
      TabMapEntry tabMapEntry = findTabMapEntry( name, ObjectType.BROWSER );
      if ( tabMapEntry == null ) {
        CTabFolder cTabFolder = tabfolder.getSwtTabset();
        final SpoonBrowser browser = new SpoonBrowser( cTabFolder, spoon, urlString, isURL, showControls, listener );

        browser.getBrowser().addOpenWindowListener( new OpenWindowListener() {

          @Override
          public void open( WindowEvent event ) {
            if ( event.required ) {
              event.browser = browser.getBrowser();
            }
          }
        } );

        TabItem tabItem = new TabItem( tabfolder, name, name );
        tabItem.setImage( GUIResource.getInstance().getImageLogoSmall() );
        tabItem.setControl( browser.getComposite() );

        tabMapEntry =
          new TabMapEntry( tabItem, isURL ? urlString : null, name, null, null, browser, ObjectType.BROWSER );
        tabMap.add( tabMapEntry );
      }
      int idx = tabfolder.indexOf( tabMapEntry.getTabItem() );

      // keep the focus on the graph
      tabfolder.setSelected( idx );
      return true;
    } catch ( Throwable e ) {
      boolean ok = false;
      if ( isURL ) {
        // Retry to show the welcome page in an external browser.
        //
        Status status = Launch.openURL( urlString );
        ok = status.equals( Status.Success );
      }
      if ( !ok ) {
        // Log an error
        //
        log.logError( "Unable to open browser tab", e );
        return false;
      } else {
        return true;
      }
    }
  }

  public TabMapEntry findTabMapEntry( String tabItemText, ObjectType objectType ) {
    for ( TabMapEntry entry : tabMap ) {
      if ( entry.getTabItem().isDisposed() ) {
        continue;
      }
      if ( objectType == entry.getObjectType() && entry.getTabItem().getText().equalsIgnoreCase( tabItemText ) ) {
        return entry;
      }
    }
    return null;
  }

  public TabMapEntry findTabMapEntry( Object managedObject ) {
    for ( TabMapEntry entry : tabMap ) {
      if ( entry.getTabItem().isDisposed() ) {
        continue;
      }
      Object entryManagedObj = entry.getObject().getManagedObject();
      // make sure they are the same class before comparing them
      if ( entryManagedObj != null && managedObject != null ) {
        if ( entryManagedObj.getClass().equals( managedObject.getClass() ) ) {
          if ( entryManagedObj.equals( managedObject ) ) {
            return entry;
          }
        }
      }
    }
    return null;
  }

  /**
   * Finds the tab for the transformation that matches the metadata provided (either the file must be the same or the
   * repository id).
   *
   * @param trans
   *          Transformation metadata to look for
   * @return Tab with transformation open whose metadata matches {@code trans} or {@code null} if no tab exists.
   * @throws KettleFileException
   *           If there is a problem loading the file object for an open transformation with an invalid a filename.
   */
  public TabMapEntry findTabForTransformation( TransMeta trans ) throws KettleFileException {
    // File for the transformation we're looking for. It will be loaded upon first request.
    FileObject transFile = null;
    for ( TabMapEntry entry : tabMap ) {
      if ( entry == null || entry.getTabItem().isDisposed() ) {
        continue;
      }
      if ( trans.getFilename() != null && entry.getFilename() != null ) {
        // If the entry has a file name it is the same as trans iff. they originated from the same files
        FileObject entryFile = KettleVFS.getFileObject( entry.getFilename() );
        if ( transFile == null ) {
          transFile = KettleVFS.getFileObject( trans.getFilename() );
        }
        if ( entryFile.equals( transFile ) ) {
          return entry;
        }
      } else if ( trans.getObjectId() != null && entry.getObject() != null ) {
        EngineMetaInterface meta = entry.getObject().getMeta();
        if ( meta != null && trans.getObjectId().equals( meta.getObjectId() ) ) {
          // If the transformation has an object id and the entry shares the same id they are the same
          return entry;
        }
      }
    }
    // No tabs for the transformation exist and are not disposed
    return null;
  }

  /**
   * Rename the tabs
   */
  public void renameTabs() {
    List<TabMapEntry> list = new ArrayList<TabMapEntry>( tabMap );
    for ( TabMapEntry entry : list ) {
      if ( entry.getTabItem().isDisposed() ) {
        // this should not be in the map, get rid of it.
        tabMap.remove( entry.getObjectName() );
        continue;
      }

      // TabItem before = entry.getTabItem();
      // PDI-1683: need to get the String here, otherwise using only the "before" instance below, the reference gets
      // changed and result is always the same
      // String beforeText=before.getText();
      //
      Object managedObject = entry.getObject().getManagedObject();
      if ( managedObject != null ) {
        if ( entry.getObject() instanceof TransGraph ) {
          TransMeta transMeta = (TransMeta) managedObject;
          String tabText = makeTabName( transMeta, entry.isShowingLocation() );
          entry.getTabItem().setText( tabText );
          String toolTipText = BaseMessages.getString( PKG, "Spoon.TabTrans.Tooltip", tabText );
          if ( Const.isWindows() && !Const.isEmpty( transMeta.getFilename() ) ) {
            toolTipText += Const.CR + Const.CR + transMeta.getFilename();
          }
          entry.getTabItem().setToolTipText( toolTipText );
        } else if ( entry.getObject() instanceof JobGraph ) {
          JobMeta jobMeta = (JobMeta) managedObject;
          entry.getTabItem().setText( makeTabName( jobMeta, entry.isShowingLocation() ) );
          String toolTipText =
            BaseMessages.getString(
              PKG, "Spoon.TabJob.Tooltip", makeTabName( jobMeta, entry.isShowingLocation() ) );
          if ( Const.isWindows() && !Const.isEmpty( jobMeta.getFilename() ) ) {
            toolTipText += Const.CR + Const.CR + jobMeta.getFilename();
          }
          entry.getTabItem().setToolTipText( toolTipText );
        }
      }

      /*
       * String after = entry.getTabItem().getText();
       *
       * if (!beforeText.equals(after)) // PDI-1683, could be improved to rename all the time {
       * entry.setObjectName(after);
       *
       * // Also change the transformation map if (entry.getObject() instanceof TransGraph) {
       * spoon.delegates.trans.removeTransformation(beforeText); spoon.delegates.trans.addTransformation(after,
       * (TransMeta) entry.getObject().getManagedObject()); } // Also change the job map if (entry.getObject()
       * instanceof JobGraph) { spoon.delegates.jobs.removeJob(beforeText); spoon.delegates.jobs.addJob(after, (JobMeta)
       * entry.getObject().getManagedObject()); } }
       */
    }
    spoon.setShellText();
  }

  public void addTab( TabMapEntry entry ) {
    tabMap.add( entry );
  }

  public String makeTabName( EngineMetaInterface transMeta, boolean showLocation ) {
    if ( Const.isEmpty( transMeta.getName() ) && Const.isEmpty( transMeta.getFilename() ) ) {
      return Spoon.STRING_TRANS_NO_NAME;
    }

    if ( Const.isEmpty( transMeta.getName() )
      || spoon.delegates.trans.isDefaultTransformationName( transMeta.getName() ) ) {
      transMeta.nameFromFilename();
    }

    String name = "";

    if ( showLocation ) {
      if ( !Const.isEmpty( transMeta.getFilename() ) ) {
        // Regular file...
        //
        name += transMeta.getFilename() + " : ";
      } else {
        // Repository object...
        //
        name += transMeta.getRepositoryDirectory().getPath() + " : ";
      }
    }

    name += transMeta.getName();
    if ( showLocation ) {
      ObjectRevision version = transMeta.getObjectRevision();
      if ( version != null ) {
        name += " : r" + version.getName();
      }
    }
    return name;
  }

  public void tabSelected( TabItem item ) {
    ArrayList<TabMapEntry> collection = new ArrayList<TabMapEntry>( tabMap );

    // See which core objects to show
    //
    for ( TabMapEntry entry : collection ) {
      boolean isTrans = ( entry.getObject() instanceof TransGraph );

      if ( item.equals( entry.getTabItem() ) ) {
        if ( isTrans || entry.getObject() instanceof JobGraph ) {
          EngineMetaInterface meta = entry.getObject().getMeta();
          if ( meta != null ) {
            meta.setInternalKettleVariables();
          }
          if ( spoon.getCoreObjectsState() != SpoonInterface.STATE_CORE_OBJECTS_SPOON ) {
            spoon.refreshCoreObjects();
          }
        }

        if ( entry.getObject() instanceof JobGraph ) {
          ( (JobGraph) entry.getObject() ).setFocus();
        } else if ( entry.getObject() instanceof TransGraph ) {
          ( (TransGraph) entry.getObject() ).setFocus();
        }

        break;
      }
    }

    // Also refresh the tree
    spoon.refreshTree();
    spoon.setShellText(); // calls also enableMenus() and markTabsChanged()

  }

  /*
   * private void setEnabled(String id,boolean enable) { spoon.getToolbar().getButtonById(id).setEnable(enable); }
   */

}
