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

package org.pentaho.di.ui.spoon.trans;

import java.util.ArrayList;
import java.util.Map;
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
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.spoon.XulSpoonResourceBundle;
import org.pentaho.di.ui.spoon.XulSpoonSettingsManager;
import org.pentaho.di.ui.spoon.delegates.SpoonDelegate;
import org.pentaho.di.ui.xul.KettleXulLoader;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.XulLoader;
import org.pentaho.ui.xul.components.XulToolbarbutton;
import org.pentaho.ui.xul.containers.XulToolbar;
import org.pentaho.ui.xul.impl.XulEventHandler;

public class TransLogDelegate extends SpoonDelegate implements XulEventHandler {
  private static Class<?> PKG = Spoon.class; // for i18n purposes, needed by Translator2!!

  private static final String XUL_FILE_TRANS_LOG_TOOLBAR = "ui/trans-log-toolbar.xul";

  private TransGraph transGraph;

  private CTabItem transLogTab;

  private StyledText transLogText;

  private XulToolbar toolbar;

  private Composite transLogComposite;

  private LogBrowser logBrowser;

  /**
   * @param spoon
   */
  public TransLogDelegate( Spoon spoon, TransGraph transGraph ) {
    super( spoon );
    this.transGraph = transGraph;
  }

  public void addTransLog() {
    // First, see if we need to add the extra view...
    //
    if ( transGraph.extraViewComposite == null || transGraph.extraViewComposite.isDisposed() ) {
      transGraph.addExtraView();
    } else {
      if ( transLogTab != null && !transLogTab.isDisposed() ) {
        // just set this one active and get out...
        //
        transGraph.extraViewTabFolder.setSelection( transLogTab );
        return;
      }
    }

    // Add a transLogTab : display the logging...
    //
    transLogTab = new CTabItem( transGraph.extraViewTabFolder, SWT.NONE );
    transLogTab.setImage( GUIResource.getInstance().getImageShowLog() );
    transLogTab.setText( BaseMessages.getString( PKG, "Spoon.TransGraph.LogTab.Name" ) );

    transLogComposite = new Composite( transGraph.extraViewTabFolder, SWT.NO_BACKGROUND | SWT.NO_FOCUS );
    transLogComposite.setLayout( new FormLayout() );

    addToolBar();

    Control toolbarControl = (Control) toolbar.getManagedObject();

    toolbarControl.setLayoutData( new FormData() );
    FormData fd = new FormData();
    fd.left = new FormAttachment( 0, 0 ); // First one in the left top corner
    fd.top = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( 100, 0 );
    toolbarControl.setLayoutData( fd );

    toolbarControl.setParent( transLogComposite );

    transLogText = new StyledText( transLogComposite, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL );
    spoon.props.setLook( transLogText );
    FormData fdText = new FormData();
    fdText.left = new FormAttachment( 0, 0 );
    fdText.right = new FormAttachment( 100, 0 );
    fdText.top = new FormAttachment( (Control) toolbar.getManagedObject(), 0 );
    fdText.bottom = new FormAttachment( 100, 0 );
    transLogText.setLayoutData( fdText );

    logBrowser = new LogBrowser( transLogText, transGraph );
    logBrowser.installLogSniffer();

    // If the transformation is closed, we should dispose of all the logging information in the buffer and registry for
    // this transformation
    //
    transGraph.addDisposeListener( new DisposeListener() {
      public void widgetDisposed( DisposeEvent event ) {
        if ( transGraph.trans != null ) {
          KettleLogStore.discardLines( transGraph.trans.getLogChannelId(), true );
        }
      }
    } );

    transLogTab.setControl( transLogComposite );

    transGraph.extraViewTabFolder.setSelection( transLogTab );
  }

  private void addToolBar() {

    try {
      XulLoader loader = new KettleXulLoader();
      loader.setSettingsManager( XulSpoonSettingsManager.getInstance() );
      ResourceBundle bundle = new XulSpoonResourceBundle( Spoon.class );
      XulDomContainer xulDomContainer = loader.loadXul( XUL_FILE_TRANS_LOG_TOOLBAR, bundle );
      xulDomContainer.addEventHandler( this );
      toolbar = (XulToolbar) xulDomContainer.getDocumentRoot().getElementById( "nav-toolbar" );

      ToolBar swtToolBar = (ToolBar) toolbar.getManagedObject();
      spoon.props.setLook( swtToolBar, Props.WIDGET_STYLE_TOOLBAR );
      swtToolBar.layout( true, true );
    } catch ( Throwable t ) {
      log.logError( Const.getStackTracker( t ) );
      new ErrorDialog( transLogComposite.getShell(),
        BaseMessages.getString( PKG, "Spoon.Exception.ErrorReadingXULFile.Title" ),
        BaseMessages.getString( PKG, "Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_TRANS_LOG_TOOLBAR ),
        new Exception( t ) );
    }
  }

  public void showLogView() {

    // What button?
    //
    // XulToolbarButton showLogXulButton = toolbar.getButtonById("trans-show-log");
    // ToolItem toolBarButton = (ToolItem) showLogXulButton.getNativeObject();

    if ( transLogTab == null || transLogTab.isDisposed() ) {
      addTransLog();
    } else {
      transLogTab.dispose();

      transGraph.checkEmptyExtraView();
    }

    // spoon.addTransLog(transMeta);
  }

  public void showLogSettings() {
    spoon.setLog();
  }

  public void clearLog() {
    if ( transLogText != null && !transLogText.isDisposed() ) {
      transLogText.setText( "" );
    }
    Map<StepMeta, String> stepLogMap = transGraph.getStepLogMap();
    if ( stepLogMap != null ) {
      stepLogMap.clear();
      transGraph.getDisplay().asyncExec( new Runnable() {
        public void run() {
          transGraph.redraw();
        }
      } );
    }
  }

  public void showErrors() {
    String all = transLogText.getText();
    ArrayList<String> err = new ArrayList<String>();

    int i = 0;
    int startpos = 0;
    int crlen = Const.CR.length();

    while ( i < all.length() - crlen ) {
      if ( all.substring( i, i + crlen ).equalsIgnoreCase( Const.CR ) ) {
        String line = all.substring( startpos, i );
        String uLine = line.toUpperCase();
        if ( uLine.indexOf( BaseMessages.getString( PKG, "TransLog.System.ERROR" ) ) >= 0
          || uLine.indexOf( BaseMessages.getString( PKG, "TransLog.System.EXCEPTION" ) ) >= 0
          || uLine.indexOf( "ERROR" ) >= 0 || // i18n for compatibilty to non translated steps a.s.o.
          uLine.indexOf( "EXCEPTION" ) >= 0 // i18n for compatibilty to non translated steps a.s.o.
        ) {
          err.add( line );
        }
        // New start of line
        startpos = i + crlen;
      }

      i++;
    }
    String line = all.substring( startpos );
    String uLine = line.toUpperCase();
    if ( uLine.indexOf( BaseMessages.getString( PKG, "TransLog.System.ERROR2" ) ) >= 0
      || uLine.indexOf( BaseMessages.getString( PKG, "TransLog.System.EXCEPTION2" ) ) >= 0
      || uLine.indexOf( "ERROR" ) >= 0 || // i18n for compatibilty to non translated steps a.s.o.
      uLine.indexOf( "EXCEPTION" ) >= 0 // i18n for compatibilty to non translated steps a.s.o.
    ) {
      err.add( line );
    }

    if ( err.size() > 0 ) {
      String[] err_lines = new String[err.size()];
      for ( i = 0; i < err_lines.length; i++ ) {
        err_lines[i] = err.get( i );
      }

      EnterSelectionDialog esd = new EnterSelectionDialog( transGraph.getShell(), err_lines,
        BaseMessages.getString( PKG, "TransLog.Dialog.ErrorLines.Title" ),
        BaseMessages.getString( PKG, "TransLog.Dialog.ErrorLines.Message" ) );
      line = esd.open();
      if ( line != null ) {
        TransMeta transMeta = transGraph.getManagedObject();
        for ( i = 0; i < transMeta.nrSteps(); i++ ) {
          StepMeta stepMeta = transMeta.getStep( i );
          if ( line.indexOf( stepMeta.getName() ) >= 0 ) {
            spoon.editStep( transMeta, stepMeta );
          }
        }
        // System.out.println("Error line selected: "+line);
      }
    }
  }

  /**
   * @return the transLogTab
   */
  public CTabItem getTransLogTab() {
    return transLogTab;
  }

  public String getLoggingText() {
    if ( transLogText != null && !transLogText.isDisposed() ) {
      return transLogText.getText();
    } else {
      return null;
    }

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

  public LogBrowser getLogBrowser() {
    return logBrowser;
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
    return "translog";
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
