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

package org.pentaho.di.ui.spoon.job;

import java.util.ArrayList;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.spoon.XulSpoonResourceBundle;
import org.pentaho.di.ui.spoon.XulSpoonSettingsManager;
import org.pentaho.di.ui.spoon.delegates.SpoonDelegate;
import org.pentaho.di.ui.spoon.trans.LogBrowser;
import org.pentaho.di.ui.xul.KettleXulLoader;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.XulLoader;
import org.pentaho.ui.xul.components.XulToolbarbutton;
import org.pentaho.ui.xul.containers.XulToolbar;
import org.pentaho.ui.xul.impl.XulEventHandler;

public class JobLogDelegate extends SpoonDelegate implements XulEventHandler {
  private static Class<?> PKG = JobGraph.class; // for i18n purposes, needed by Translator2!!

  private static final String XUL_FILE_TRANS_LOG_TOOLBAR = "ui/job-log-toolbar.xul";

  private JobGraph jobGraph;

  private CTabItem jobLogTab;

  public StyledText jobLogText;

  /**
   * The number of lines in the log tab
   */
  // private int textSize;
  private Composite jobLogComposite;

  private XulToolbar toolbar;

  private LogBrowser logBrowser;

  /**
   * @param spoon
   */
  public JobLogDelegate( Spoon spoon, JobGraph jobGraph ) {
    super( spoon );
    this.jobGraph = jobGraph;
  }

  public void addJobLog() {
    // First, see if we need to add the extra view...
    //
    if ( jobGraph.extraViewComposite == null || jobGraph.extraViewComposite.isDisposed() ) {
      jobGraph.addExtraView();
    } else {
      if ( jobLogTab != null && !jobLogTab.isDisposed() ) {
        // just set this one active and get out...
        //
        jobGraph.extraViewTabFolder.setSelection( jobLogTab );
        return;
      }
    }

    // Add a transLogTab : display the logging...
    //
    jobLogTab = new CTabItem( jobGraph.extraViewTabFolder, SWT.NONE );
    jobLogTab.setImage( GUIResource.getInstance().getImageShowLog() );
    jobLogTab.setText( BaseMessages.getString( PKG, "JobGraph.LogTab.Name" ) );

    jobLogComposite = new Composite( jobGraph.extraViewTabFolder, SWT.NONE );
    jobLogComposite.setLayout( new FormLayout() );

    addToolBar();

    Control toolbarControl = (Control) toolbar.getManagedObject();
    //spoon.props.setLook( toolbarControl );

    toolbarControl.setLayoutData( new FormData() );
    FormData fd = new FormData();
    fd.left = new FormAttachment( 0, 0 ); // First one in the left top corner
    fd.top = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    toolbarControl.setLayoutData( fd );

    toolbarControl.setParent( jobLogComposite );

    jobLogText = new StyledText( jobLogComposite, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL );
    spoon.props.setLook( jobLogText );
    FormData fdText = new FormData();
    fdText.left = new FormAttachment( 0, 0 );
    fdText.right = new FormAttachment( 100, 0 );
    fdText.top = new FormAttachment( (Control) toolbar.getManagedObject(), 0 );
    fdText.bottom = new FormAttachment( 100, 0 );
    jobLogText.setLayoutData( fdText );

    logBrowser = new LogBrowser( jobLogText, jobGraph );
    logBrowser.installLogSniffer();

    // If the job is closed, we should dispose of all the logging information in the buffer and registry for it
    //
    jobGraph.addDisposeListener( new DisposeListener() {
      public void widgetDisposed( DisposeEvent event ) {
        if ( jobGraph.job != null ) {
          KettleLogStore.discardLines( jobGraph.job.getLogChannelId(), true );
        }
      }
    } );

    jobLogTab.setControl( jobLogComposite );

    jobGraph.extraViewTabFolder.setSelection( jobLogTab );
  }

  private void addToolBar() {

    try {
      XulLoader loader = new KettleXulLoader();
      loader.setSettingsManager( XulSpoonSettingsManager.getInstance() );
      ResourceBundle bundle = new XulSpoonResourceBundle( Spoon.class );
      XulDomContainer xulDomContainer = loader.loadXul( XUL_FILE_TRANS_LOG_TOOLBAR, bundle );
      xulDomContainer.addEventHandler( this );
      toolbar = (XulToolbar) xulDomContainer.getDocumentRoot().getElementById( "nav-toolbar" );

      ToolBar swtToolbar = (ToolBar) toolbar.getManagedObject();
      spoon.props.setLook( swtToolbar, Props.WIDGET_STYLE_TOOLBAR );
      swtToolbar.layout( true, true );
    } catch ( Throwable t ) {
      log.logError( Const.getStackTracker( t ) );
      new ErrorDialog( jobLogComposite.getShell(),
        BaseMessages.getString( PKG, "Spoon.Exception.ErrorReadingXULFile.Title" ),
        BaseMessages.getString( PKG, "Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_TRANS_LOG_TOOLBAR ),
        new Exception( t ) );
    }
  }

  public void clearLog() {
    if ( jobLogText != null && !jobLogText.isDisposed() ) {
      jobLogText.setText( "" );
    }
  }

  public void showLogSettings() {
    spoon.setLog();
  }

  public void showErrors() {
    String all = jobLogText.getText();
    ArrayList<String> err = new ArrayList<String>();

    int i = 0;
    int startpos = 0;
    int crlen = Const.CR.length();

    String line = null;
    String lineUpper = null;
    while ( i < all.length() - crlen ) {
      if ( all.substring( i, i + crlen ).equalsIgnoreCase( Const.CR ) ) {
        line = all.substring( startpos, i );
        lineUpper = line.toUpperCase();
        if ( lineUpper.indexOf( BaseMessages.getString( PKG, "JobLog.System.ERROR" ) ) >= 0
          || lineUpper.indexOf( BaseMessages.getString( PKG, "JobLog.System.EXCEPTION" ) ) >= 0 ) {
          err.add( line );
        }
        // New start of line
        startpos = i + crlen;
      }

      i++;
    }
    line = all.substring( startpos );
    lineUpper = line.toUpperCase();
    if ( lineUpper.indexOf( BaseMessages.getString( PKG, "JobLog.System.ERROR" ) ) >= 0
      || lineUpper.indexOf( BaseMessages.getString( PKG, "JobLog.System.EXCEPTION" ) ) >= 0 ) {
      err.add( line );
    }

    if ( err.size() > 0 ) {
      String[] err_lines = new String[err.size()];
      for ( i = 0; i < err_lines.length; i++ ) {
        err_lines[i] = err.get( i );
      }

      EnterSelectionDialog esd = new EnterSelectionDialog( jobGraph.getShell(), err_lines,
        BaseMessages.getString( PKG, "JobLog.Dialog.ErrorLines.Title" ),
        BaseMessages.getString( PKG, "JobLog.Dialog.ErrorLines.Message" ) );
      line = esd.open();
      if ( line != null ) {
        JobMeta jobMeta = jobGraph.getManagedObject();
        for ( i = 0; i < jobMeta.nrJobEntries(); i++ ) {
          JobEntryCopy entryCopy = jobMeta.getJobEntry( i );
          if ( line.indexOf( entryCopy.getName() ) >= 0 ) {
            spoon.editJobEntry( jobMeta, entryCopy );
          }
        }
        // System.out.println("Error line selected: "+line);
      }
    }
  }

  /**
   * @return the transLogTab
   */
  public CTabItem getJobLogTab() {
    return jobLogTab;
  }

  public void pauseLog() {
    XulToolbarbutton pauseContinueButton = (XulToolbarbutton) toolbar.getElementById( "log-pause" );
    ToolItem swtToolItem = (ToolItem) pauseContinueButton.getManagedObject();

    if ( logBrowser.isPaused() ) {
      logBrowser.setPaused( false );
      if ( pauseContinueButton != null ) {
        swtToolItem.setImage( GUIResource.getInstance().getImagePauseLog() );
      }
    } else {
      logBrowser.setPaused( true );
      if ( pauseContinueButton != null ) {
        swtToolItem.setImage( GUIResource.getInstance().getImageContinueLog() );
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.ui.xul.impl.XulEventHandler#getData()
   */
  public Object getData() {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.ui.xul.impl.XulEventHandler#getName()
   */
  public String getName() {
    return "joblog";
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.ui.xul.impl.XulEventHandler#getXulDomContainer()
   */
  public XulDomContainer getXulDomContainer() {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.ui.xul.impl.XulEventHandler#setData(java.lang.Object)
   */
  public void setData( Object data ) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.ui.xul.impl.XulEventHandler#setName(java.lang.String)
   */
  public void setName( String name ) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   *
   * @see org.pentaho.ui.xul.impl.XulEventHandler#setXulDomContainer(org.pentaho.ui.xul.XulDomContainer)
   */
  public void setXulDomContainer( XulDomContainer xulDomContainer ) {
    // TODO Auto-generated method stub

  }

}
