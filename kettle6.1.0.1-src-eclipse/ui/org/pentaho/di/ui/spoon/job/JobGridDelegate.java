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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.gui.JobTracker;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobEntryResult;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.TreeMemory;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.spoon.delegates.SpoonDelegate;

public class JobGridDelegate extends SpoonDelegate {

  private static Class<?> PKG = JobGraph.class; // for i18n purposes, needed by Translator2!!

  public static final long REFRESH_TIME = 100L;
  public static final long UPDATE_TIME_VIEW = 1000L;
  private static final String STRING_CHEF_LOG_TREE_NAME = "Job Log Tree";

  private JobGraph jobGraph;
  private CTabItem jobGridTab;
  private Tree wTree;

  public JobTracker jobTracker;
  public int previousNrItems;

  private int nrRow = 0;

  /**
   * @param spoon
   * @param transGraph
   */
  public JobGridDelegate( Spoon spoon, JobGraph transGraph ) {
    super( spoon );
    this.jobGraph = transGraph;
  }

  /**
   * Add a grid with the execution metrics per step in a table view
   *
   */
  public void addJobGrid() {

    // First, see if we need to add the extra view...
    //
    if ( jobGraph.extraViewComposite == null || jobGraph.extraViewComposite.isDisposed() ) {
      jobGraph.addExtraView();
    } else {
      if ( jobGridTab != null && !jobGridTab.isDisposed() ) {
        // just set this one active and get out...
        //
        jobGraph.extraViewTabFolder.setSelection( jobGridTab );
        return;
      }
    }

    jobGridTab = new CTabItem( jobGraph.extraViewTabFolder, SWT.NONE );
    jobGridTab.setImage( GUIResource.getInstance().getImageShowGrid() );
    jobGridTab.setText( BaseMessages.getString( PKG, "Spoon.TransGraph.GridTab.Name" ) );

    addControls();

    jobGridTab.setControl( wTree );

    jobGraph.extraViewTabFolder.setSelection( jobGridTab );
  }

  /**
   * Add the controls to the tab
   */
  private void addControls() {

    // Create the tree table...
    wTree = new Tree( jobGraph.extraViewTabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
    wTree.setHeaderVisible( true );
    TreeMemory.addTreeListener( wTree, STRING_CHEF_LOG_TREE_NAME );

    TreeColumn column1 = new TreeColumn( wTree, SWT.LEFT );
    column1.setText( BaseMessages.getString( PKG, "JobLog.Column.JobJobEntry" ) );
    column1.setWidth( 200 );

    TreeColumn column2 = new TreeColumn( wTree, SWT.LEFT );
    column2.setText( BaseMessages.getString( PKG, "JobLog.Column.Comment" ) );
    column2.setWidth( 200 );

    TreeColumn column3 = new TreeColumn( wTree, SWT.LEFT );
    column3.setText( BaseMessages.getString( PKG, "JobLog.Column.Result" ) );
    column3.setWidth( 100 );

    TreeColumn column4 = new TreeColumn( wTree, SWT.LEFT );
    column4.setText( BaseMessages.getString( PKG, "JobLog.Column.Reason" ) );
    column4.setWidth( 200 );

    TreeColumn column5 = new TreeColumn( wTree, SWT.LEFT );
    column5.setText( BaseMessages.getString( PKG, "JobLog.Column.Filename" ) );
    column5.setWidth( 200 );

    TreeColumn column6 = new TreeColumn( wTree, SWT.RIGHT );
    column6.setText( BaseMessages.getString( PKG, "JobLog.Column.Nr" ) );
    column6.setWidth( 50 );

    TreeColumn column7 = new TreeColumn( wTree, SWT.RIGHT );
    column7.setText( BaseMessages.getString( PKG, "JobLog.Column.LogDate" ) );
    column7.setWidth( 120 );

    FormData fdTree = new FormData();
    fdTree.left = new FormAttachment( 0, 0 );
    fdTree.top = new FormAttachment( 0, 0 );
    fdTree.right = new FormAttachment( 100, 0 );
    fdTree.bottom = new FormAttachment( 100, 0 );
    wTree.setLayoutData( fdTree );

    final Timer tim = new Timer( "JobGrid: " + jobGraph.getMeta().getName() );
    TimerTask timtask = new TimerTask() {
      public void run() {
        Display display = jobGraph.getDisplay();
        if ( display != null && !display.isDisposed() ) {
          display.asyncExec( new Runnable() {
            public void run() {
              // Check if the widgets are not disposed.
              // This happens is the rest of the window is not yet disposed.
              // We ARE running in a different thread after all.
              //
              // TODO: add a "auto refresh" check box somewhere
              if ( !wTree.isDisposed() ) {
                refreshTreeTable();
              }
            }
          } );
        }
      }
    };
    tim.schedule( timtask, 10L, 2000L ); // refresh every 2 seconds...

    jobGraph.jobLogDelegate.getJobLogTab().addDisposeListener( new DisposeListener() {
      public void widgetDisposed( DisposeEvent disposeEvent ) {
        tim.cancel();
      }
    } );

  }

  /**
   * Refresh the data in the tree-table... Use the data from the JobTracker in the job
   */
  private void refreshTreeTable() {
    if ( jobTracker != null ) {
      int nrItems = jobTracker.getTotalNumberOfItems();

      if ( nrItems != previousNrItems ) {
        // Allow some flickering for now ;-)
        wTree.removeAll();

        // Re-populate this...
        TreeItem treeItem = new TreeItem( wTree, SWT.NONE );
        String jobName = jobTracker.getJobName();

        if ( Const.isEmpty( jobName ) ) {
          if ( !Const.isEmpty( jobTracker.getJobFilename() ) ) {
            jobName = jobTracker.getJobFilename();
          } else {
            jobName = BaseMessages.getString( PKG, "JobLog.Tree.StringToDisplayWhenJobHasNoName" );
          }
        }
        treeItem.setText( 0, jobName );
        TreeMemory.getInstance().storeExpanded( STRING_CHEF_LOG_TREE_NAME, new String[] { jobName }, true );

        for ( int i = 0; i < jobTracker.nrJobTrackers(); i++ ) {
          addTrackerToTree( jobTracker.getJobTracker( i ), treeItem );
        }
        previousNrItems = nrItems;

        TreeMemory.setExpandedFromMemory( wTree, STRING_CHEF_LOG_TREE_NAME );
      }
    }
  }

  private void addTrackerToTree( JobTracker jobTracker, TreeItem parentItem ) {
    try {
      if ( jobTracker != null ) {
        TreeItem treeItem = new TreeItem( parentItem, SWT.NONE );
        if ( nrRow % 2 != 0 ) {
          treeItem.setBackground( GUIResource.getInstance().getColorBlueCustomGrid() );
        }
        nrRow++;
        if ( jobTracker.nrJobTrackers() > 0 ) {
          // This is a sub-job: display the name at the top of the list...
          treeItem.setText( 0, BaseMessages.getString( PKG, "JobLog.Tree.JobPrefix" ) + jobTracker.getJobName() );

          // then populate the sub-job entries ...
          for ( int i = 0; i < jobTracker.nrJobTrackers(); i++ ) {
            addTrackerToTree( jobTracker.getJobTracker( i ), treeItem );
          }
        } else {
          JobEntryResult result = jobTracker.getJobEntryResult();
          if ( result != null ) {
            String jobEntryName = result.getJobEntryName();
            if ( !Const.isEmpty( jobEntryName ) ) {
              treeItem.setText( 0, jobEntryName );
              treeItem.setText( 4, Const.NVL( result.getJobEntryFilename(), "" ) );
            } else {
              treeItem.setText( 0, BaseMessages.getString( PKG, "JobLog.Tree.JobPrefix2" )
                + jobTracker.getJobName() );
            }
            String comment = result.getComment();
            if ( comment != null ) {
              treeItem.setText( 1, comment );
            }
            Result res = result.getResult();
            if ( res != null ) {
              treeItem.setText( 2, res.getResult()
                ? BaseMessages.getString( PKG, "JobLog.Tree.Success" ) : BaseMessages.getString(
                  PKG, "JobLog.Tree.Failure" ) );
              treeItem.setText( 5, Long.toString( res.getEntryNr() ) );
              if ( res.getResult() ) {
                treeItem.setForeground( GUIResource.getInstance().getColorSuccessGreen() );
              } else {
                treeItem.setForeground( GUIResource.getInstance().getColorRed() );
              }
            }
            String reason = result.getReason();
            if ( reason != null ) {
              treeItem.setText( 3, reason );
            }
            Date logDate = result.getLogDate();
            if ( logDate != null ) {
              treeItem.setText( 6, new SimpleDateFormat( "yyyy/MM/dd HH:mm:ss" ).format( logDate ) );
            }
          }
        }
        treeItem.setExpanded( true );
      }
    } catch ( Exception e ) {
      log.logError( Const.getStackTracker( e ) );
    }
  }

  public CTabItem getJobGridTab() {
    return jobGridTab;
  }

  public void setJobTracker( JobTracker jobTracker ) {
    this.jobTracker = jobTracker;

  }

}
