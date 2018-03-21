/*******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2012 by Pentaho : http://www.pentaho.com
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

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.RowMetaAndData;
import org.pentaho.di.core.database.Database;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.logging.JobEntryLogTable;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogStatus;
import org.pentaho.di.core.logging.LogTableField;
import org.pentaho.di.core.logging.LogTableInterface;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
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

public class JobHistoryDelegate extends SpoonDelegate implements XulEventHandler {
  private static Class<?> PKG = JobGraph.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

  private static final String XUL_FILE_TRANS_GRID_TOOLBAR = "ui/job-history-toolbar.xul"; 

  private JobGraph jobGraph;

  private CTabItem jobHistoryTab;

  private List<ColumnInfo[]> columns;

  private List<Text> wText;

  private List<TableView> wFields;

  private XulToolbar toolbar;

  private Composite jobHistoryComposite;

  private JobMeta jobMeta;

  private CTabFolder tabFolder;

  private XulToolbarbutton refreshButton;
  private XulToolbarbutton fetchNextBatchButton;
  private XulToolbarbutton fetchAllButton;

  private TransHistoryModel[] models;
  
  private enum Mode { INITIAL, NEXT_BATCH, ALL }

  /**
   * @param spoon
   * @param jobGraph
   */
  public JobHistoryDelegate(Spoon spoon, JobGraph jobGraph) {
    super(spoon);
    this.jobGraph = jobGraph;
  }

  public void addJobHistory() {
    // First, see if we need to add the extra view...
    //
    if (jobGraph.extraViewComposite == null || jobGraph.extraViewComposite.isDisposed()) {
      jobGraph.addExtraView();
    } else {
      if (jobHistoryTab != null && !jobHistoryTab.isDisposed()) {
        // just set this one active and get out...
        //
        jobGraph.extraViewTabFolder.setSelection(jobHistoryTab);
        return;
      }
    }

    jobMeta = jobGraph.getManagedObject();

    // Add a tab to display the logging history tables...
    //
    jobHistoryTab = new CTabItem(jobGraph.extraViewTabFolder, SWT.NONE);
    jobHistoryTab.setImage(GUIResource.getInstance().getImageShowHistory());
    jobHistoryTab.setText(BaseMessages.getString(PKG, "Spoon.TransGraph.HistoryTab.Name"));

    // Create a composite, slam everything on there like it was in the history tab.
    //
    jobHistoryComposite = new Composite(jobGraph.extraViewTabFolder, SWT.NONE);
    jobHistoryComposite.setLayout(new FormLayout());
    spoon.props.setLook(jobHistoryComposite);

    addToolBar();

    Control toolbarControl = (Control) toolbar.getManagedObject();
    
    toolbarControl.setLayoutData(new FormData());
    FormData fd = new FormData();
    fd.left = new FormAttachment(0, 0); // First one in the left top corner
    fd.top = new FormAttachment(0, 0);
    fd.right = new FormAttachment(100, 0);
    toolbarControl.setLayoutData(fd);
    
    toolbarControl.setParent(jobHistoryComposite);
    
    addLogTableTabs();
    tabFolder.setSelection(0);
    
    tabFolder.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent arg0) {
        setMoreRows(true);
      }
      
      @Override
      public void widgetDefaultSelected(SelectionEvent arg0) {
      }
    });
    
    jobHistoryComposite.pack();
    jobHistoryTab.setControl(jobHistoryComposite);
    jobGraph.extraViewTabFolder.setSelection(jobHistoryTab);

    if (!Props.getInstance().disableInitialExecutionHistory()) {
      refreshAllHistory();
    }
  }

  private void addLogTableTabs() {

    models = new TransHistoryModel[jobMeta.getLogTables().size()];
    for (int i = 0; i < models.length; i++) {
      models[i] = new TransHistoryModel();
      models[i].logTable = jobMeta.getLogTables().get(i);
    }

    columns = new ArrayList<ColumnInfo[]>(models.length);
    wFields = new ArrayList<TableView>(models.length);
    wText = new ArrayList<Text>(models.length);

    // Create a nested tab folder in the tab item, on the history composite...
    //
    tabFolder = new CTabFolder(jobHistoryComposite, SWT.MULTI);
    spoon.props.setLook(tabFolder, Props.WIDGET_STYLE_TAB);

    for (TransHistoryModel model : models) {
      LogTableInterface logTable = model.logTable;
      CTabItem tabItem = new CTabItem(tabFolder, SWT.NONE);
      // tabItem.setImage(GUIResource.getInstance().getImageShowHistory());
      tabItem.setText(logTable.getLogTableType());

      Composite logTableComposite = new Composite(tabFolder, SWT.NONE);
      logTableComposite.setLayout(new FormLayout());
      spoon.props.setLook(logTableComposite);

      tabItem.setControl(logTableComposite);

      SashForm sash = new SashForm(logTableComposite, SWT.VERTICAL);
      sash.setLayout(new FillLayout());
      FormData fdSash = new FormData();
      fdSash.left = new FormAttachment(0, 0); // First one in the left top corner
      fdSash.top = new FormAttachment(0, 0);
      fdSash.right = new FormAttachment(100, 0);
      fdSash.bottom = new FormAttachment(100, 0);
      sash.setLayoutData(fdSash);

      List<ColumnInfo> columnList = new ArrayList<ColumnInfo>();
      List<LogTableField> fields = new ArrayList<LogTableField>();

      for (LogTableField field : logTable.getFields()) {
        if (field.isEnabled() && field.isVisible()) {
          fields.add(field);
          if (!field.isLogField()) {
            ColumnInfo column = new ColumnInfo(field.getName(), ColumnInfo.COLUMN_TYPE_TEXT, false, true);

            int valueType = field.getDataType();
            String conversionMask = null;
            
            switch (field.getDataType()) {
              case ValueMetaInterface.TYPE_INTEGER:
                conversionMask = "###,###,##0"; 
                column.setAllignement(SWT.RIGHT);
                break;
              case ValueMetaInterface.TYPE_DATE:
                conversionMask = "yyyy/MM/dd HH:mm:ss"; 
                column.setAllignement(SWT.CENTER);
                break;
              case ValueMetaInterface.TYPE_NUMBER:
                conversionMask = " ###,###,##0.00;-###,###,##0.00"; 
                column.setAllignement(SWT.RIGHT);
                break;
              case ValueMetaInterface.TYPE_STRING:
                column.setAllignement(SWT.LEFT);
                break;
              case ValueMetaInterface.TYPE_BOOLEAN:
                DatabaseMeta databaseMeta = logTable.getDatabaseMeta(); 
                if (databaseMeta!=null) {
                  if (!databaseMeta.supportsBooleanDataType()) {
                    // Boolean gets converted to String!
                    //
                    valueType=ValueMetaInterface.TYPE_STRING;
                  }
                }
                break;
              default:
                break;
            }
            ValueMetaInterface valueMeta = new ValueMeta(field.getFieldName(), valueType, field.getLength(), -1);
            if (conversionMask!=null) valueMeta.setConversionMask(conversionMask);
            
            column.setValueMeta(valueMeta);
            columnList.add(column);
          }
        }
      }
      model.logTableFields = fields;

      final int FieldsRows = 1;
      ColumnInfo[] colinf = columnList.toArray(new ColumnInfo[columnList.size()]);
      columns.add(colinf); // keep for later

      TableView tableView = new TableView(jobGraph.getManagedObject(), sash, SWT.BORDER | SWT.FULL_SELECTION | SWT.SINGLE, colinf, FieldsRows, true, // readonly!
          null, spoon.props);
      wFields.add(tableView);

      tableView.table.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent arg0) {
          showLogEntry();
        }
      });

      if (logTable.getLogField() != null) {

        Text text = new Text(sash, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        spoon.props.setLook(text);
        text.setVisible(true);
        wText.add(text);

        FormData fdText = new FormData();
        fdText.left = new FormAttachment(0, 0);
        fdText.top = new FormAttachment(0, 0);
        fdText.right = new FormAttachment(100, 0);
        fdText.bottom = new FormAttachment(100, 0);
        text.setLayoutData(fdText);

        sash.setWeights(new int[] { 70, 30, });
      } else {
        wText.add(null);
        sash.setWeights(new int[] { 100, });
      }

    }

    FormData fdTabFolder = new FormData();
    fdTabFolder.left = new FormAttachment(0, 0); // First one in the left top corner
    fdTabFolder.top = new FormAttachment((Control) toolbar.getManagedObject(), 0);
    fdTabFolder.right = new FormAttachment(100, 0);
    fdTabFolder.bottom = new FormAttachment(100, 0);
    tabFolder.setLayoutData(fdTabFolder);

  }

  private void addToolBar() {

    try {
      XulLoader loader = new KettleXulLoader();
      loader.setSettingsManager(XulSpoonSettingsManager.getInstance());
      ResourceBundle bundle = new XulSpoonResourceBundle(Spoon.class);
      XulDomContainer xulDomContainer = loader.loadXul(XUL_FILE_TRANS_GRID_TOOLBAR, bundle);
      xulDomContainer.addEventHandler(this);
      toolbar = (XulToolbar) xulDomContainer.getDocumentRoot().getElementById("nav-toolbar"); 

      refreshButton = (XulToolbarbutton) xulDomContainer.getDocumentRoot().getElementById("refresh-history"); 
      fetchNextBatchButton = (XulToolbarbutton) xulDomContainer.getDocumentRoot().getElementById("fetch-next-batch-history"); 
      fetchAllButton = (XulToolbarbutton) xulDomContainer.getDocumentRoot().getElementById("fetch-all-history"); 
      
      ToolBar swtToolBar = (ToolBar) toolbar.getManagedObject();
      swtToolBar.layout(true, true);
    } catch (Throwable t) {
      log.logError(Const.getStackTracker(t));
      new ErrorDialog(jobHistoryComposite.getShell(), BaseMessages.getString(PKG, "Spoon.Exception.ErrorReadingXULFile.Title"), BaseMessages.getString(PKG, "Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_TRANS_GRID_TOOLBAR), new Exception(t)); 
    }
  }

  /**
   * Public for XUL.
   */
  public void clearLogTable() {
    clearLogTable(tabFolder.getSelectionIndex());
  }

  /**
   * User requested to clear the log table.<br>
   * Better ask confirmation
   */
  private void clearLogTable(int index) {

    LogTableInterface logTable = models[index].logTable;

    if (logTable.isDefined()) {
      String schemaTable = logTable.getQuotedSchemaTableCombination();
      DatabaseMeta databaseMeta = logTable.getDatabaseMeta();

      MessageBox mb = new MessageBox(jobGraph.getShell(), SWT.YES | SWT.NO | SWT.ICON_QUESTION);
      mb.setMessage(BaseMessages.getString(PKG, "JobGraph.Dialog.AreYouSureYouWantToRemoveAllLogEntries.Message", schemaTable)); // Nothing found that matches your criteria 
      mb.setText(BaseMessages.getString(PKG, "JobGraph.Dialog.AreYouSureYouWantToRemoveAllLogEntries.Title")); // Sorry! 
      if (mb.open() == SWT.YES) {
        Database database = new Database(loggingObject, databaseMeta);
        try {
          database.connect();
          database.truncateTable(schemaTable);
        } catch (Exception e) {
          new ErrorDialog(jobGraph.getShell(), BaseMessages.getString(PKG, "JobGraph.Dialog.ErrorClearningLoggingTable.Title"), BaseMessages.getString(PKG, "JobGraph.Dialog.AreYouSureYouWantToRemoveAllLogEntries.Message"), e); 
        } finally {
          if (database != null) {
            database.disconnect();
          }

          refreshHistory();
          if (wText.get(index) != null) {
            wText.get(index).setText(""); 
          }
        }
      }
    }
  }
  

  /**
   * Public for XUL.
   */
  public void replayHistory() {
    int tabIndex = tabFolder.getSelectionIndex();
    int idx = wFields.get(tabIndex).getSelectionIndex();
    if (idx >= 0) {
      String fields[] = wFields.get(tabIndex).getItem(idx);
      int batchId = Const.toInt(fields[0], -1); 
      // String dateString = fields[13];
      // Date replayDate = XMLHandler.stringToDate(dateString);
    
      List<JobEntryCopyResult> results = null;
      boolean gotResults = false;
      
      // We check in the Job Entry Logging to see the results from all the various job entries that were executed.
      //
      JobEntryLogTable jeLogTable = jobMeta.getJobEntryLogTable();
      if (jeLogTable.isDefined()) {
        try {
          
          DatabaseMeta databaseMeta =  jobMeta.getJobEntryLogTable().getDatabaseMeta();
          Database db = new Database(Spoon.loggingObject, databaseMeta);
          try {
            db.connect();
            String schemaTable = databaseMeta.getQuotedSchemaTableCombination(jeLogTable.getActualSchemaName(), jeLogTable.getActualTableName());
            String sql = "SELECT * FROM "+schemaTable+" WHERE "+databaseMeta.quoteField(jeLogTable.getKeyField().getFieldName())+" = "+batchId;
            
            List<Object[]> rows = db.getRows(sql, 0);
            RowMetaInterface rowMeta = db.getReturnRowMeta();
            results = new ArrayList<JobEntryCopyResult>();
            
            int jobEntryNameIndex = rowMeta.indexOfValue( jeLogTable.findField(JobEntryLogTable.ID.JOBENTRYNAME.toString()).getFieldName() );
            int jobEntryResultIndex = rowMeta.indexOfValue( jeLogTable.findField(JobEntryLogTable.ID.RESULT.toString()).getFieldName() );
            int jobEntryErrorsIndex = rowMeta.indexOfValue( jeLogTable.findField(JobEntryLogTable.ID.ERRORS.toString()).getFieldName() );
            LogTableField copyNrField = jeLogTable.findField(JobEntryLogTable.ID.COPY_NR.toString());
            int jobEntryCopyNrIndex = copyNrField==null ? -1 : ( copyNrField.isEnabled() ? rowMeta.indexOfValue( copyNrField.getFieldName() ) : -1 );
            
            for (Object[] row : rows) {
              String jobEntryName = rowMeta.getString(row, jobEntryNameIndex);
              boolean jobEntryResult = rowMeta.getBoolean(row, jobEntryResultIndex);
              long errors = rowMeta.getInteger(row, jobEntryErrorsIndex);
              long copyNr = jobEntryCopyNrIndex<0 ? 0 : rowMeta.getInteger(row, jobEntryCopyNrIndex);
              JobEntryCopyResult result = new JobEntryCopyResult(jobEntryName, jobEntryResult, errors, (int)copyNr);
              results.add(result);
            }
            
          } finally {
            db.disconnect();
          }
          
          gotResults=true;
        } catch(Exception e) {
          new ErrorDialog(spoon.getShell(), BaseMessages.getString(PKG, "JobHistoryDelegate.ReplayHistory.UnexpectedErrorReadingJobEntryHistory.Text"),
              BaseMessages.getString(PKG, "JobHistoryDelegate.ReplayHistory.UnexpectedErrorReadingJobEntryHistory.Message"),
              e);
          
        }
      } else {
        MessageBox box = new MessageBox(spoon.getShell(), SWT.ICON_ERROR | SWT.OK);
        box.setText(BaseMessages.getString(PKG, "JobHistoryDelegate.ReplayHistory.NoJobEntryTable.Text"));
        box.setMessage(BaseMessages.getString(PKG, "JobHistoryDelegate.ReplayHistory.NoJobEntryTable.Message"));
        box.open();
      }
        
        // spoon.executeJob(jobGraph.getManagedObject(), true, false, replayDate, false);
      if (!gotResults) {

        // For some reason we have no execution results, simply list all the job entries so the user can choose...
        //
        results = new ArrayList<JobEntryCopyResult>();
        for (JobEntryCopy copy : jobMeta.getJobCopies()) {
          results.add(new JobEntryCopyResult(copy.getName(), null, null, copy.getNr()));
        }
      }
      
      // OK, now that we have our list of job entries, let's first try to find the first job-entry that had a false result or where errors>0
      // If the error was handled, we look further for a more appropriate target.
      //
      JobEntryCopy selection = null;
      boolean more = true;
      JobEntryCopy start = jobMeta.findStart();
      while (selection==null && more) {
        int nrNext = jobMeta.findNrNextJobEntries(start);
        more = nrNext>0;
        for (int n=0;n<nrNext;n++) {
          JobEntryCopy copy = jobMeta.findNextJobEntry(start, n);
          
          // See if we can find a result for this job entry...
          //
          JobEntryCopyResult result = JobEntryCopyResult.findResult(results, copy);
          if (result!=null) {
            
          }
        }
      }
      
      // Present all job entries to the user.
      //
      for (JobEntryCopyResult result : results) {
        System.out.println("Job entry copy result --  Name="+result.getJobEntryName()+", result="+result.getResult()+", errors="+result.getErrors()+", nr="+result.getCopyNr());
      }
      
      
    }
  }

  /**
   * Public for XUL. 
   */
  public void refreshHistory() {
    int tabIndex = tabFolder.getSelectionIndex();
    refreshHistory(tabIndex);
  }
  
  private void refreshAllHistory() {
    for (int i = 0; i < models.length; i++) {
      refreshHistory(i);
    }
  }
  
  /**
   * Background thread refreshes history data
   */
  private void refreshHistory(final int index) {
    new Thread(new Runnable() {
      public void run() {

              // do gui stuff here
              spoon.getDisplay().syncExec(new Runnable() {
                public void run() {
                  setQueryInProgress(true);
                }
               });

              
              final boolean moreRows = getHistoryData(index, Mode.INITIAL);
              
              
           // do gui stuff here
              spoon.getDisplay().syncExec(new Runnable() {
                public void run() {
                  displayHistoryData(index);
                  setQueryInProgress(false);
                  setMoreRows(moreRows);
                }
               });
            
          
        
      }
    }).start();
  }
  
  private void setMoreRows(final boolean moreRows) {
    fetchNextBatchButton.setDisabled(!moreRows);
  }
  
  /**
   * Don't allow more queries until this one finishes.
   * @param inProgress
   */
  private void setQueryInProgress(final boolean inProgress) {
    refreshButton.setDisabled(inProgress);
    fetchNextBatchButton.setDisabled(inProgress);
    fetchAllButton.setDisabled(inProgress);
  }
  
  private boolean getHistoryData(final int index, final Mode mode) {
    final int BATCH_SIZE = Props.getInstance().getLinesInHistoryFetchSize();
    boolean moreRows = false;
    LogTableInterface logTable = models[index].logTable;
    // See if there is a job loaded that has a connection table specified.
    // 
    if (jobMeta != null && !Const.isEmpty(jobMeta.getName()) && logTable.isDefined()) {
      Database database = null;
      try {
        DatabaseMeta logConnection = logTable.getDatabaseMeta();
        
        // open a connection
        database = new Database(loggingObject, logConnection);
        database.shareVariablesWith(jobMeta);
        database.connect();

        int queryLimit = 0;
        
        switch (mode) {
          case ALL:
            models[index].batchCount = 0;
            queryLimit = Props.getInstance().getMaxNrLinesInHistory();
            break;
          case NEXT_BATCH:
            models[index].batchCount++;
            queryLimit = BATCH_SIZE * models[index].batchCount;
            break;
          case INITIAL:
            models[index].batchCount = 1;
            queryLimit = BATCH_SIZE;
            break;
          default:
            break;
        }
        database.setQueryLimit(queryLimit);
        
        // First, we get the information out of the database table...
        //
        String schemaTable = logTable.getQuotedSchemaTableCombination();

        String sql = "SELECT "; 
        boolean first = true;
        for (LogTableField field : logTable.getFields()) {
          if (field.isEnabled() && field.isVisible()) {
            if (!first)
              sql += ", "; 
            first = false;
            sql += logConnection.quoteField(field.getFieldName());
          }
        }
        sql += " FROM " + schemaTable; 

        RowMetaAndData params = new RowMetaAndData();

        // Do we need to limit the amount of data?
        //
        LogTableField nameField = logTable.getNameField();
        LogTableField keyField = logTable.getKeyField();
        
        if (nameField != null) {
            sql += " WHERE " + logConnection.quoteField(nameField.getFieldName()) + " LIKE ?";  
            params.addValue(new ValueMeta("transname_literal", ValueMetaInterface.TYPE_STRING), jobMeta.getName()); 
        }
        
        if (keyField != null && keyField.isEnabled()) {
          sql += " ORDER BY " + logConnection.quoteField(keyField.getFieldName()) + " DESC"; 
        }

        ResultSet resultSet = database.openQuery(sql, params.getRowMeta(), params.getData());

        List<Object[]> rows = new ArrayList<Object[]>();
        Object[] rowData = database.getRow(resultSet);
        int rowsFetched = 1;
        while (rowData != null) {
          rows.add(rowData);
          rowData = database.getRow(resultSet);
          rowsFetched++;
        }
        
        if (rowsFetched >= queryLimit) {
          moreRows = true;
        }
        
        database.closeQuery(resultSet);

        models[index].rows = rows;
      } catch (Exception e) {
        LogChannel.GENERAL.logError("Unable to get rows of data from logging table "+models[index].logTable, e); 
        models[index].rows = new ArrayList<Object[]>();
      } finally {
        if (database != null)
          database.disconnect();
      }
    } else {
      models[index].rows = new ArrayList<Object[]>();
    }
    return moreRows;
  }

  private void displayHistoryData(final int index) {
    LogTableInterface logTable = models[index].logTable;
    List<Object[]> rows = models[index].rows;
    
    ColumnInfo[] colinf = columns.get(index);

    // Now, we're going to display the data in the table view
    //
    if (index>=wFields.size() || wFields.get(index).isDisposed()) {
      return;
    }

    int selectionIndex = wFields.get(index).getSelectionIndex();

    wFields.get(index).table.clearAll();

    if (rows != null && rows.size() > 0) {
      // OK, now that we have a series of rows, we can add them to the table view...
      // 
      for (int i = 0; i < rows.size(); i++) {
        Object[] rowData = rows.get(i);

        TableItem item = new TableItem(wFields.get(index).table, SWT.NONE);

        for (int c = 0; c < colinf.length; c++) {

          ColumnInfo column = colinf[c];

          ValueMetaInterface valueMeta = column.getValueMeta();
          String string = null;
          try {
            string = valueMeta.getString(rowData[c]);
          } catch (KettleValueException e) {
            log.logError("history data conversion issue", e); 
          }
          item.setText(c + 1, Const.NVL(string, "")); 
        }

        // Add some color
        //
        Long errors = null;
        LogStatus status = null;

        LogTableField errorsField = logTable.getErrorsField();
        if (errorsField != null) {
          int index1 = models[index].logTableFields.indexOf(errorsField);
          try {
            errors = colinf[index1].getValueMeta().getInteger(rowData[index1]);
          } catch (KettleValueException e) {
            log.logError("history data conversion issue", e); 
          }
        }
        LogTableField statusField = logTable.getStatusField();
        if (statusField != null) {
          int index1 = models[index].logTableFields.indexOf(statusField);
          String statusString = null;
          try {
            statusString = colinf[index1].getValueMeta().getString(rowData[index1]);
          } catch (KettleValueException e) {
            log.logError("history data conversion issue", e); 
          }
          if (statusString != null) {
            status = LogStatus.findStatus(statusString);
          }
        }

        if (errors != null && errors.longValue() > 0L) {
          item.setBackground(GUIResource.getInstance().getColorRed());
        } else if (status != null && LogStatus.STOP.equals(status)) {
          item.setBackground(GUIResource.getInstance().getColorYellow());
        }
      }

      wFields.get(index).removeEmptyRows();
      wFields.get(index).setRowNums();
      wFields.get(index).optWidth(true);
    } else {
      wFields.get(index).clearAll(false);
      // new TableItem(wFields.get(tabIndex).table, SWT.NONE); // Give it an item to prevent errors on various platforms.
    }

    if (selectionIndex >= 0 && selectionIndex < wFields.get(index).getItemCount()) {
      wFields.get(index).table.select(selectionIndex);
      showLogEntry();
    }
  }

  private void showLogEntry() {
    int tabIndex = tabFolder.getSelectionIndex();
    LogTableInterface logTable = models[tabIndex].logTable;
    List<LogTableField> fields = models[tabIndex].logTableFields;

    Text text = wText.get(tabIndex);

    if (text == null || text.isDisposed())
      return;

    List<Object[]> list = models[tabIndex].rows;

    if (list == null || list.size() == 0) {
      String message;
      if (logTable.isDefined()) {
        message = BaseMessages.getString(PKG, "JobHistory.PleaseRefresh.Message"); 
      } else {
        message = BaseMessages.getString(PKG, "JobHistory.HistoryConfiguration.Message"); 
      }
      text.setText(message);
      return;
    }

    // grab the selected line in the table:
    int nr = wFields.get(tabIndex).table.getSelectionIndex();
    if (nr >= 0 && list != null && nr < list.size()) {
      // OK, grab this one from the buffer...
      Object[] row = list.get(nr);

      // What is the name of the log field?
      //
      LogTableField logField = models[tabIndex].logTable.getLogField();
      if (logField != null) {
        int index = fields.indexOf(logField);
        if (index>=0) {
          String logText = row[index].toString();
  
          text.setText(Const.NVL(logText, "")); 
  
          text.setSelection(text.getText().length());
          text.showSelection();
        } else {
          text.setText(BaseMessages.getString(PKG, "JobHistory.HistoryConfiguration.NoLoggingFieldDefined")); 
        }
      }
    }
  }

  /**
   * @return the jobHistoryTab
   */
  public CTabItem getJobHistoryTab() {
    return jobHistoryTab;
  }

  /* (non-Javadoc)
   * @see org.pentaho.ui.xul.impl.XulEventHandler#getData()
   */
  public Object getData() {
    return null;
  }

  /* (non-Javadoc)
   * @see org.pentaho.ui.xul.impl.XulEventHandler#getName()
   */
  public String getName() {
    return "history"; 
  }

  /* (non-Javadoc)
   * @see org.pentaho.ui.xul.impl.XulEventHandler#getXulDomContainer()
   */
  public XulDomContainer getXulDomContainer() {
    return null;
  }

  /* (non-Javadoc)
   * @see org.pentaho.ui.xul.impl.XulEventHandler#setData(java.lang.Object)
   */
  public void setData(Object data) {
  }

  /* (non-Javadoc)
   * @see org.pentaho.ui.xul.impl.XulEventHandler#setName(java.lang.String)
   */
  public void setName(String name) {
  }

  /* (non-Javadoc)
   * @see org.pentaho.ui.xul.impl.XulEventHandler#setXulDomContainer(org.pentaho.ui.xul.XulDomContainer)
   */
  public void setXulDomContainer(XulDomContainer xulDomContainer) {
  }
  
  /**
   * XUL event: fetches next x records for current log table.
   */
  public void fetchNextBatch() {
    int tabIndex = tabFolder.getSelectionIndex();
    fetchNextBatch(tabIndex);
  }
  
  private void fetchNextBatch(final int index) {
    new Thread(new Runnable() {
      public void run() {

              // do gui stuff here
              spoon.getDisplay().syncExec(new Runnable() {
                public void run() {
                  setQueryInProgress(true);
                }
               });

              
              final boolean moreRows = getHistoryData(index, Mode.NEXT_BATCH);
              
              
           // do gui stuff here
              spoon.getDisplay().syncExec(new Runnable() {
                public void run() {
                  displayHistoryData(index);
                  setQueryInProgress(false);
                  setMoreRows(moreRows);
                }
               });
            
          
        
      }
    }).start();

  }
  
  /**
   * XUL event: loads all load records for current log table.
   */
  public void fetchAll() {
    int tabIndex = tabFolder.getSelectionIndex();
    fetchAll(tabIndex);
  }
  
  private void fetchAll(final int index) {
    new Thread(new Runnable() {
      public void run() {

              // do gui stuff here
              spoon.getDisplay().syncExec(new Runnable() {
                public void run() {
                  setQueryInProgress(true);
                }
               });

              
              final boolean moreRows = getHistoryData(index, Mode.ALL);
              
              
           // do gui stuff here
              spoon.getDisplay().syncExec(new Runnable() {
                public void run() {
                  displayHistoryData(index);
                  setQueryInProgress(false);
                  setMoreRows(moreRows);
                }
               });
            
          
        
      }
    }).start();
  }
  
  private static class TransHistoryModel {
    public List<LogTableField> logTableFields;
    public List<Object[]> rows;
    public LogTableInterface logTable;
    /**
     * Number of batches fetched so far. When the next batch is fetched, the number of rows displayed will be the max of
     * batchCount * BATCH_SIZE and resultSet row count.
     */
    public int batchCount;
  }
}
