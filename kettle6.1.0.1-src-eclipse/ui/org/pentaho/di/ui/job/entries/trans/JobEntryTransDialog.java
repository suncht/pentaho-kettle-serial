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

package org.pentaho.di.ui.job.entries.trans;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.ObjectLocationSpecificationMethod;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogLevel;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entries.trans.JobEntryTrans;
import org.pentaho.di.job.entry.JobEntryDialogInterface;
import org.pentaho.di.job.entry.JobEntryInterface;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryElementMetaInterface;
import org.pentaho.di.repository.RepositoryObject;
import org.pentaho.di.repository.RepositoryObjectType;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.job.dialog.JobDialog;
import org.pentaho.di.ui.job.entry.JobEntryDialog;
import org.pentaho.di.ui.repository.dialog.SelectObjectDialog;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.trans.dialog.TransDialog;
import org.pentaho.di.ui.trans.step.BaseStepDialog;

import java.io.File;
import java.io.IOException;

/**
 * This dialog allows you to edit the transformation job entry (JobEntryTrans)
 *
 * @author Matt
 * @since 19-06-2003
 */
public class JobEntryTransDialog extends JobEntryDialog implements JobEntryDialogInterface {
  private static Class<?> PKG = JobEntryTrans.class; // for i18n purposes, needed by Translator2!!

  private static final String[] FILE_FILTERLOGNAMES = new String[] {
    BaseMessages.getString( PKG, "JobTrans.Fileformat.TXT" ),
    BaseMessages.getString( PKG, "JobTrans.Fileformat.LOG" ),
    BaseMessages.getString( PKG, "JobTrans.Fileformat.All" ) };

  private Label wlName;

  private Composite wSpec;
  private FormData fdSpec;

  private Text wName;
  private FormData fdlName, fdName;

  private Button wbTransname;
  private TextVar wTransname;

  private TextVar wDirectory;

  private Button wbFilename;
  private TextVar wFilename;

  private Button wNewTrans;

  private Composite wLogging;

  private Label wlSetLogfile;
  private Button wSetLogfile;

  private Label wlLogfile;
  private TextVar wLogfile;

  private Button wbLogFilename;
  private FormData fdbLogFilename;

  private Label wlCreateParentFolder;
  private Button wCreateParentFolder;
  private FormData fdlCreateParentFolder, fdCreateParentFolder;

  private Label wlLogext;
  private TextVar wLogext;

  private Label wlAddDate;
  private Button wAddDate;

  private Label wlAddTime;
  private Button wAddTime;

  private Label wlLoglevel;
  private CCombo wLoglevel;

  private Label wlPrevious;
  private Button wPrevious;

  private Label wlPrevToParams;
  private Button wPrevToParams;

  private Label wlEveryRow;
  private Button wEveryRow;

  private Label wlClearRows;
  private Button wClearRows;

  private Label wlClearFiles;
  private Button wClearFiles;

  private Label wlCluster;
  private Button wCluster;

  private Label wlLogRemoteWork;
  private Button wLogRemoteWork;

  private TableView wFields;

  private TableView wParameters;

  private Label wlSlaveServer;
  private ComboVar wSlaveServer;

  private Label wlWaitingToFinish;
  private Button wWaitingToFinish;

  private Label wlFollowingAbortRemotely;
  private Button wFollowingAbortRemotely;

  private Button wOK, wCancel;

  private Listener lsOK, lsCancel;

  private Shell shell;

  private SelectionAdapter lsDef;

  private JobEntryTrans jobEntry;

  private boolean backupChanged;

  private Label wlAppendLogfile;

  private Button wAppendLogfile;

  private Label wlPassParams;
  private Button wPassParams;

  private Button wbGetParams;

  private Display display;

  private Button radioFilename;
  private Button radioByName;
  private Button radioByReference;

  private Button wbByReference;
  private Text wByReference;

  private Composite wAdvanced;

  private ObjectId referenceObjectId;
  private ObjectLocationSpecificationMethod specificationMethod;

  public JobEntryTransDialog( Shell parent, JobEntryInterface jobEntryInt, Repository rep, JobMeta jobMeta ) {
    super( parent, jobEntryInt, rep, jobMeta );
    jobEntry = (JobEntryTrans) jobEntryInt;
  }

  public JobEntryInterface open() {
    Shell parent = getParent();
    display = parent.getDisplay();

    shell = new Shell( parent, props.getJobsDialogStyle() );
    props.setLook( shell );
    JobDialog.setShellImage( shell, jobEntry );

    ModifyListener lsMod = new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        jobEntry.setChanged();
      }
    };
    backupChanged = jobEntry.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );
    shell.setText( BaseMessages.getString( PKG, "JobTrans.Header" ) );

    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

    // Name line
    wlName = new Label( shell, SWT.RIGHT );
    wlName.setText( BaseMessages.getString( PKG, "JobTrans.JobStep.Label" ) );
    props.setLook( wlName );
    fdlName = new FormData();
    fdlName.left = new FormAttachment( 0, 0 );
    fdlName.top = new FormAttachment( 0, 0 );
    fdlName.right = new FormAttachment( middle, -margin );
    wlName.setLayoutData( fdlName );

    wName = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wName );
    wName.addModifyListener( lsMod );
    fdName = new FormData();
    fdName.top = new FormAttachment( 0, 0 );
    fdName.left = new FormAttachment( middle, 0 );
    fdName.right = new FormAttachment( 100, 0 );
    wName.setLayoutData( fdName );

    CTabFolder wTabFolder = new CTabFolder( shell, SWT.BORDER );
    props.setLook( wTabFolder, Props.WIDGET_STYLE_TAB );

    // Specification
    //
    CTabItem wSpecTab = new CTabItem( wTabFolder, SWT.NONE );
    wSpecTab.setText( BaseMessages.getString( PKG, "JobTrans.Specification.Group.Label" ) );

    ScrolledComposite wSSpec = new ScrolledComposite( wTabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
    wSSpec.setLayout( new FillLayout() );

    wSpec = new Composite( wSSpec, SWT.SHADOW_NONE );
    props.setLook( wSpec );

    FormLayout specLayout = new FormLayout();
    specLayout.marginWidth = Const.FORM_MARGIN;
    specLayout.marginHeight = Const.FORM_MARGIN;
    wSpec.setLayout( specLayout );

    // The specify by filename option...
    //
    Group gFilename = new Group( wSpec, SWT.SHADOW_ETCHED_IN );
    props.setLook( gFilename );
    FormLayout gFileLayout = new FormLayout();
    gFileLayout.marginWidth = Const.FORM_MARGIN;
    gFileLayout.marginHeight = Const.FORM_MARGIN;
    gFilename.setLayout( gFileLayout );

    radioFilename = new Button( gFilename, SWT.RADIO );
    props.setLook( radioFilename );
    radioFilename.setText( BaseMessages.getString( PKG, "JobTrans.TransformationFile.Label" ) );
    FormData fdRadioFilename = new FormData();
    fdRadioFilename.top = new FormAttachment( 0, 0 );
    fdRadioFilename.left = new FormAttachment( 0, 0 );
    fdRadioFilename.right = new FormAttachment( middle, -margin );
    radioFilename.setLayoutData( fdRadioFilename );
    radioFilename.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent arg0 ) {
        specificationMethod = ObjectLocationSpecificationMethod.FILENAME;
        setRadioButtons();
      }
    } );

    wbFilename = new Button( gFilename, SWT.PUSH | SWT.CENTER );
    props.setLook( wbFilename );
    wbFilename.setImage( GUIResource.getInstance().getImageTransGraph() );
    wbFilename.setToolTipText( BaseMessages.getString( PKG, "JobTrans.SelectTrans.Tooltip" ) );
    FormData fdbFilename = new FormData();
    fdbFilename.top = new FormAttachment( 0, 0 );
    fdbFilename.right = new FormAttachment( 100, 0 );
    wbFilename.setLayoutData( fdbFilename );

    wFilename = new TextVar( jobMeta, gFilename, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wFilename );
    wFilename.addModifyListener( lsMod );
    FormData fdFilename = new FormData();
    fdFilename.top = new FormAttachment( 0, 0 );
    fdFilename.left = new FormAttachment( middle, 0 );
    fdFilename.right = new FormAttachment( wbFilename, -margin );
    wFilename.setLayoutData( fdFilename );
    wFilename.addModifyListener( new ModifyListener() {

      public void modifyText( ModifyEvent arg0 ) {
        specificationMethod = ObjectLocationSpecificationMethod.FILENAME;
        setRadioButtons();
      }
    } );

    FormData fdgFilename = new FormData();
    fdgFilename.top = new FormAttachment( 0, 0 );
    fdgFilename.left = new FormAttachment( 0, 0 );
    fdgFilename.right = new FormAttachment( 100, 0 );
    gFilename.setLayoutData( fdgFilename );

    // The repository : specify by name radio option...
    //
    Group gByName = new Group( wSpec, SWT.SHADOW_ETCHED_IN );
    props.setLook( gByName );
    FormLayout gByNameLayout = new FormLayout();
    gByNameLayout.marginWidth = Const.FORM_MARGIN;
    gByNameLayout.marginHeight = Const.FORM_MARGIN;
    gByName.setLayout( gByNameLayout );

    radioByName = new Button( gByName, SWT.RADIO );
    props.setLook( radioByName );
    radioByName.setText( BaseMessages.getString( PKG, "JobTrans.NameOfTransformation.Label" ) );
    FormData fdRadioByName = new FormData();
    fdRadioByName.top = new FormAttachment( 0, 0 );
    fdRadioByName.left = new FormAttachment( 0, 0 );
    fdRadioByName.right = new FormAttachment( middle, -margin );
    radioByName.setLayoutData( fdRadioByName );
    radioByName.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent arg0 ) {
        specificationMethod = ObjectLocationSpecificationMethod.REPOSITORY_BY_NAME;
        setRadioButtons();
      }
    } );

    wbTransname = new Button( gByName, SWT.PUSH | SWT.CENTER );
    props.setLook( wbTransname );
    wbTransname.setImage( GUIResource.getInstance().getImageTransGraph() );
    wbTransname.setToolTipText( BaseMessages.getString( PKG, "JobTrans.SelectTransRep.Tooltip" ) );
    FormData fdbTransname = new FormData();
    fdbTransname.top = new FormAttachment( 0, 0 );
    fdbTransname.right = new FormAttachment( 100, 0 );
    wbTransname.setLayoutData( fdbTransname );

    wTransname = new TextVar( jobMeta, gByName, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wTransname );
    wTransname.addModifyListener( lsMod );
    FormData fdTransname = new FormData();
    fdTransname.top = new FormAttachment( 0, 0 );
    fdTransname.left = new FormAttachment( middle, 0 );
    fdTransname.right = new FormAttachment( wbTransname, -margin );
    wTransname.setLayoutData( fdTransname );

    wDirectory = new TextVar( jobMeta, gByName, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wDirectory );
    wDirectory.addModifyListener( lsMod );
    FormData fdDirectory = new FormData();
    fdDirectory.top = new FormAttachment( wTransname, margin * 2 );
    fdDirectory.left = new FormAttachment( middle, 0 );
    fdDirectory.right = new FormAttachment( 100, 0 );
    wDirectory.setLayoutData( fdDirectory );

    FormData fdgByName = new FormData();
    fdgByName.top = new FormAttachment( gFilename, margin );
    fdgByName.left = new FormAttachment( 0, 0 );
    fdgByName.right = new FormAttachment( 100, 0 );
    gByName.setLayoutData( fdgByName );

    // The specify by filename option...
    //

    Group gByReference = new Group( wSpec, SWT.SHADOW_ETCHED_IN );
    props.setLook( gByReference );
    FormLayout gByReferenceLayout = new FormLayout();
    gByReferenceLayout.marginWidth = Const.FORM_MARGIN;
    gByReferenceLayout.marginHeight = Const.FORM_MARGIN;
    gByReference.setLayout( gByReferenceLayout );

    radioByReference = new Button( gByReference, SWT.RADIO );
    props.setLook( radioByReference );
    radioByReference.setText( BaseMessages.getString( PKG, "JobTrans.TransformationByReference.Label" ) );
    FormData fdRadioByReference = new FormData();
    fdRadioByReference.top = new FormAttachment( 0, 0 );
    fdRadioByReference.left = new FormAttachment( 0, 0 );
    fdRadioByReference.right = new FormAttachment( middle, -margin );
    radioByReference.setLayoutData( fdRadioByReference );
    radioByReference.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent arg0 ) {
        specificationMethod = ObjectLocationSpecificationMethod.REPOSITORY_BY_REFERENCE;
        setRadioButtons();
      }
    } );

    wbByReference = new Button( gByReference, SWT.PUSH | SWT.CENTER );
    props.setLook( wbByReference );
    wbByReference.setImage( GUIResource.getInstance().getImageTransGraph() );
    wbByReference.setToolTipText( BaseMessages.getString( PKG, "JobTrans.SelectTrans.Tooltip" ) );
    FormData fdbByReference = new FormData();
    fdbByReference.top = new FormAttachment( 0, 0 );
    fdbByReference.right = new FormAttachment( 100, 0 );
    wbByReference.setLayoutData( fdbByReference );

    wByReference = new Text( gByReference, SWT.SINGLE | SWT.LEFT | SWT.BORDER | SWT.READ_ONLY );
    props.setLook( wByReference );
    wByReference.addModifyListener( lsMod );
    FormData fdByReference = new FormData();
    fdByReference.top = new FormAttachment( 0, 0 );
    fdByReference.left = new FormAttachment( middle, 0 );
    fdByReference.right = new FormAttachment( wbByReference, -margin );
    wByReference.setLayoutData( fdByReference );

    FormData fdgByReference = new FormData();
    fdgByReference.top = new FormAttachment( gByName, margin );
    fdgByReference.left = new FormAttachment( 0, 0 );
    fdgByReference.right = new FormAttachment( 100, 0 );
    gByReference.setLayoutData( fdgByReference );

    wNewTrans = new Button( wSpec, SWT.PUSH );
    wNewTrans.setText( BaseMessages.getString( PKG, "JobTrans.NewTransButton.Label" ) );
    FormData fdNewTrans = new FormData();
    fdNewTrans.bottom = new FormAttachment( 100, -margin );
    fdNewTrans.left = new FormAttachment( wByReference, 0, SWT.CENTER );
    wNewTrans.setLayoutData( fdNewTrans );
    wNewTrans.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent event ) {
        newTransformation();
      }
    } );

    wSpec.pack();
    Rectangle bounds = wSpec.getBounds();

    wSSpec.setContent( wSpec );
    wSSpec.setExpandHorizontal( true );
    wSSpec.setExpandVertical( true );
    wSSpec.setMinWidth( bounds.width );
    wSSpec.setMinHeight( bounds.height );

    wSpecTab.setControl( wSSpec );

    fdSpec = new FormData();
    fdSpec.left = new FormAttachment( 0, 0 );
    fdSpec.top = new FormAttachment( 0, 0 );
    fdSpec.right = new FormAttachment( 100, 0 );
    fdSpec.bottom = new FormAttachment( 100, 0 );
    wSpec.setLayoutData( fdSpec );

    // Advanced
    //
    CTabItem wAdvancedTab = new CTabItem( wTabFolder, SWT.NONE );
    wAdvancedTab.setText( BaseMessages.getString( PKG, "JobTrans.Advanced.Group.Label" ) );

    ScrolledComposite wSAdvanced = new ScrolledComposite( wTabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
    wSAdvanced.setLayout( new FillLayout() );

    wAdvanced = new Composite( wSAdvanced, SWT.SHADOW_NONE );
    props.setLook( wAdvanced );

    FormLayout advancedLayout = new FormLayout();
    advancedLayout.marginWidth = Const.FORM_MARGIN;
    advancedLayout.marginHeight = Const.FORM_MARGIN;
    wAdvanced.setLayout( advancedLayout );

    wlPrevious = new Label( wAdvanced, SWT.RIGHT );
    wlPrevious.setText( BaseMessages.getString( PKG, "JobTrans.Previous.Label" ) );
    props.setLook( wlPrevious );
    FormData fdlPrevious = new FormData();
    fdlPrevious.left = new FormAttachment( 0, 0 );
    fdlPrevious.top = new FormAttachment( 0, 0 );
    fdlPrevious.right = new FormAttachment( middle, -margin );
    wlPrevious.setLayoutData( fdlPrevious );
    wPrevious = new Button( wAdvanced, SWT.CHECK );
    props.setLook( wPrevious );
    wPrevious.setSelection( jobEntry.argFromPrevious );
    wPrevious.setToolTipText( BaseMessages.getString( PKG, "JobTrans.Previous.Tooltip" ) );
    FormData fdPrevious = new FormData();
    fdPrevious.left = new FormAttachment( middle, 0 );
    fdPrevious.top = new FormAttachment( 0, 0 );
    fdPrevious.right = new FormAttachment( 100, 0 );
    wPrevious.setLayoutData( fdPrevious );
    wPrevious.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        wFields.setEnabled( !jobEntry.argFromPrevious );
      }
    } );

    wlPrevToParams = new Label( wAdvanced, SWT.RIGHT );
    wlPrevToParams.setText( BaseMessages.getString( PKG, "JobTrans.PrevToParams.Label" ) );
    props.setLook( wlPrevToParams );
    FormData fdlPrevToParams = new FormData();
    fdlPrevToParams.left = new FormAttachment( 0, 0 );
    fdlPrevToParams.top = new FormAttachment( wPrevious, margin * 3 );
    fdlPrevToParams.right = new FormAttachment( middle, -margin );
    wlPrevToParams.setLayoutData( fdlPrevToParams );
    wPrevToParams = new Button( wAdvanced, SWT.CHECK );
    props.setLook( wPrevToParams );
    wPrevToParams.setSelection( jobEntry.paramsFromPrevious );
    wPrevToParams.setToolTipText( BaseMessages.getString( PKG, "JobTrans.PrevToParams.Tooltip" ) );
    FormData fdPrevToParams = new FormData();
    fdPrevToParams.left = new FormAttachment( middle, 0 );
    fdPrevToParams.top = new FormAttachment( wPrevious, margin * 3 );
    fdPrevToParams.right = new FormAttachment( 100, 0 );
    wPrevToParams.setLayoutData( fdPrevToParams );
    wPrevToParams.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        jobEntry.setChanged();
      }
    } );

    wlEveryRow = new Label( wAdvanced, SWT.RIGHT );
    wlEveryRow.setText( BaseMessages.getString( PKG, "JobTrans.ExecForEveryInputRow.Label" ) );
    props.setLook( wlEveryRow );
    FormData fdlEveryRow = new FormData();
    fdlEveryRow.left = new FormAttachment( 0, 0 );
    fdlEveryRow.top = new FormAttachment( wPrevToParams, margin );
    fdlEveryRow.right = new FormAttachment( middle, -margin );
    wlEveryRow.setLayoutData( fdlEveryRow );
    wEveryRow = new Button( wAdvanced, SWT.CHECK );
    props.setLook( wEveryRow );
    wEveryRow.setToolTipText( BaseMessages.getString( PKG, "JobTrans.ExecForEveryInputRow.Tooltip" ) );
    FormData fdEveryRow = new FormData();
    fdEveryRow.left = new FormAttachment( middle, 0 );
    fdEveryRow.top = new FormAttachment( wPrevToParams, margin );
    fdEveryRow.right = new FormAttachment( 100, 0 );
    wEveryRow.setLayoutData( fdEveryRow );

    // Clear the result rows before executing the transformation?
    //
    wlClearRows = new Label( wAdvanced, SWT.RIGHT );
    wlClearRows.setText( BaseMessages.getString( PKG, "JobTrans.ClearResultList.Label" ) );
    props.setLook( wlClearRows );
    FormData fdlClearRows = new FormData();
    fdlClearRows.left = new FormAttachment( 0, 0 );
    fdlClearRows.top = new FormAttachment( wEveryRow, margin );
    fdlClearRows.right = new FormAttachment( middle, -margin );
    wlClearRows.setLayoutData( fdlClearRows );
    wClearRows = new Button( wAdvanced, SWT.CHECK );
    props.setLook( wClearRows );
    FormData fdClearRows = new FormData();
    fdClearRows.left = new FormAttachment( middle, 0 );
    fdClearRows.top = new FormAttachment( wEveryRow, margin );
    fdClearRows.right = new FormAttachment( 100, 0 );
    wClearRows.setLayoutData( fdClearRows );

    // Clear the result files before executing the transformation?
    //
    wlClearFiles = new Label( wAdvanced, SWT.RIGHT );
    wlClearFiles.setText( BaseMessages.getString( PKG, "JobTrans.ClearResultFiles.Label" ) );
    props.setLook( wlClearFiles );
    FormData fdlClearFiles = new FormData();
    fdlClearFiles.left = new FormAttachment( 0, 0 );
    fdlClearFiles.top = new FormAttachment( wClearRows, margin );
    fdlClearFiles.right = new FormAttachment( middle, -margin );
    wlClearFiles.setLayoutData( fdlClearFiles );
    wClearFiles = new Button( wAdvanced, SWT.CHECK );
    props.setLook( wClearFiles );
    FormData fdClearFiles = new FormData();
    fdClearFiles.left = new FormAttachment( middle, 0 );
    fdClearFiles.top = new FormAttachment( wClearRows, margin );
    fdClearFiles.right = new FormAttachment( 100, 0 );
    wClearFiles.setLayoutData( fdClearFiles );

    // Clear the result rows before executing the transformation?
    //
    wlCluster = new Label( wAdvanced, SWT.RIGHT );
    wlCluster.setText( BaseMessages.getString( PKG, "JobTrans.RunTransInCluster.Label" ) );
    props.setLook( wlCluster );
    FormData fdlCluster = new FormData();
    fdlCluster.left = new FormAttachment( 0, 0 );
    fdlCluster.top = new FormAttachment( wClearFiles, margin );
    fdlCluster.right = new FormAttachment( middle, -margin );
    wlCluster.setLayoutData( fdlCluster );
    wCluster = new Button( wAdvanced, SWT.CHECK );
    props.setLook( wCluster );
    FormData fdCluster = new FormData();
    fdCluster.left = new FormAttachment( middle, 0 );
    fdCluster.top = new FormAttachment( wClearFiles, margin );
    fdCluster.right = new FormAttachment( 100, 0 );
    wCluster.setLayoutData( fdCluster );
    wCluster.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        setActive();
      }
    } );

    // Log clustering logging results locally?
    //
    wlLogRemoteWork = new Label( wAdvanced, SWT.RIGHT );
    wlLogRemoteWork.setText( BaseMessages.getString( PKG, "JobTrans.LogRemoteWork.Label" ) );
    props.setLook( wlLogRemoteWork );
    FormData fdlLogRemoteWork = new FormData();
    fdlLogRemoteWork.left = new FormAttachment( 0, 0 );
    fdlLogRemoteWork.top = new FormAttachment( wCluster, margin );
    fdlLogRemoteWork.right = new FormAttachment( middle, -margin );
    wlLogRemoteWork.setLayoutData( fdlLogRemoteWork );
    wLogRemoteWork = new Button( wAdvanced, SWT.CHECK );
    props.setLook( wLogRemoteWork );
    FormData fdLogRemoteWork = new FormData();
    fdLogRemoteWork.left = new FormAttachment( middle, 0 );
    fdLogRemoteWork.top = new FormAttachment( wCluster, margin );
    fdLogRemoteWork.right = new FormAttachment( 100, 0 );
    wLogRemoteWork.setLayoutData( fdLogRemoteWork );

    // The remote slave server
    //
    wlSlaveServer = new Label( wAdvanced, SWT.RIGHT );
    wlSlaveServer.setText( BaseMessages.getString( PKG, "JobTrans.SlaveServer.Label" ) );
    wlSlaveServer.setToolTipText( BaseMessages.getString( PKG, "JobTrans.SlaveServer.ToolTip" ) );
    props.setLook( wlSlaveServer );
    FormData fdlSlaveServer = new FormData();
    fdlSlaveServer.left = new FormAttachment( 0, 0 );
    fdlSlaveServer.right = new FormAttachment( middle, -margin );
    fdlSlaveServer.top = new FormAttachment( wLogRemoteWork, margin );
    wlSlaveServer.setLayoutData( fdlSlaveServer );
    wSlaveServer = new ComboVar( jobMeta, wAdvanced, SWT.SINGLE | SWT.BORDER );
    wSlaveServer.setItems( SlaveServer.getSlaveServerNames( jobMeta.getSlaveServers() ) );
    wSlaveServer.setToolTipText( BaseMessages.getString( PKG, "JobTrans.SlaveServer.ToolTip" ) );
    props.setLook( wSlaveServer );
    FormData fdSlaveServer = new FormData();
    fdSlaveServer.left = new FormAttachment( middle, 0 );
    fdSlaveServer.top = new FormAttachment( wLogRemoteWork, margin );
    fdSlaveServer.right = new FormAttachment( 100, 0 );
    wSlaveServer.setLayoutData( fdSlaveServer );
    wSlaveServer.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        setActive();
      }
    } );

    // Wait for the remote transformation to finish?
    //
    wlWaitingToFinish = new Label( wAdvanced, SWT.RIGHT );
    wlWaitingToFinish.setText( BaseMessages.getString( PKG, "JobTrans.WaitToFinish.Label" ) );
    props.setLook( wlWaitingToFinish );
    FormData fdlWaitingToFinish = new FormData();
    fdlWaitingToFinish.left = new FormAttachment( 0, 0 );
    fdlWaitingToFinish.top = new FormAttachment( wSlaveServer, margin );
    fdlWaitingToFinish.right = new FormAttachment( middle, -margin );
    wlWaitingToFinish.setLayoutData( fdlWaitingToFinish );
    wWaitingToFinish = new Button( wAdvanced, SWT.CHECK );
    props.setLook( wWaitingToFinish );
    FormData fdWaitingToFinish = new FormData();
    fdWaitingToFinish.left = new FormAttachment( middle, 0 );
    fdWaitingToFinish.top = new FormAttachment( wSlaveServer, margin );
    fdWaitingToFinish.right = new FormAttachment( 100, 0 );
    wWaitingToFinish.setLayoutData( fdWaitingToFinish );
    wWaitingToFinish.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        setActive();
      }
    } );

    // Follow a local abort remotely?
    //
    wlFollowingAbortRemotely = new Label( wAdvanced, SWT.RIGHT );
    wlFollowingAbortRemotely.setText( BaseMessages.getString( PKG, "JobTrans.AbortRemote.Label" ) );
    props.setLook( wlFollowingAbortRemotely );
    FormData fdlFollowingAbortRemotely = new FormData();
    fdlFollowingAbortRemotely.left = new FormAttachment( 0, 0 );
    fdlFollowingAbortRemotely.top = new FormAttachment( wWaitingToFinish, margin );
    fdlFollowingAbortRemotely.right = new FormAttachment( middle, -margin );
    wlFollowingAbortRemotely.setLayoutData( fdlFollowingAbortRemotely );
    wFollowingAbortRemotely = new Button( wAdvanced, SWT.CHECK );
    props.setLook( wFollowingAbortRemotely );
    FormData fdFollowingAbortRemotely = new FormData();
    fdFollowingAbortRemotely.left = new FormAttachment( middle, 0 );
    fdFollowingAbortRemotely.top = new FormAttachment( wWaitingToFinish, margin );
    fdFollowingAbortRemotely.right = new FormAttachment( 100, 0 );
    wFollowingAbortRemotely.setLayoutData( fdFollowingAbortRemotely );

    FormData fdAdvanced = new FormData();
    fdAdvanced.left = new FormAttachment( 0, 0 );
    fdAdvanced.top = new FormAttachment( 0, 0 );
    fdAdvanced.right = new FormAttachment( 100, 0 );
    fdAdvanced.bottom = new FormAttachment( 100, 0 );
    wAdvanced.setLayoutData( fdAdvanced );

    wAdvanced.pack();
    bounds = wAdvanced.getBounds();

    wSAdvanced.setContent( wAdvanced );
    wSAdvanced.setExpandHorizontal( true );
    wSAdvanced.setExpandVertical( true );
    wSAdvanced.setMinWidth( bounds.width );
    wSAdvanced.setMinHeight( bounds.height );

    wAdvancedTab.setControl( wSAdvanced );

    // Logging
    //
    CTabItem wLoggingTab = new CTabItem( wTabFolder, SWT.NONE );
    wLoggingTab.setText( BaseMessages.getString( PKG, "JobTrans.LogSettings.Group.Label" ) );

    ScrolledComposite wSLogging = new ScrolledComposite( wTabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
    wSLogging.setLayout( new FillLayout() );

    wLogging = new Composite( wSLogging, SWT.SHADOW_NONE );
    props.setLook( wLogging );

    FormLayout groupLayout = new FormLayout();
    groupLayout.marginWidth = Const.FORM_MARGIN;
    groupLayout.marginHeight = Const.FORM_MARGIN;

    wLogging.setLayout( groupLayout );

    // Set the logfile?
    wlSetLogfile = new Label( wLogging, SWT.RIGHT );
    wlSetLogfile.setText( BaseMessages.getString( PKG, "JobTrans.Specify.Logfile.Label" ) );
    props.setLook( wlSetLogfile );
    FormData fdlSetLogfile = new FormData();
    fdlSetLogfile.left = new FormAttachment( 0, 0 );
    fdlSetLogfile.top = new FormAttachment( 0, margin );
    fdlSetLogfile.right = new FormAttachment( middle, -margin );
    wlSetLogfile.setLayoutData( fdlSetLogfile );
    wSetLogfile = new Button( wLogging, SWT.CHECK );
    props.setLook( wSetLogfile );
    FormData fdSetLogfile = new FormData();
    fdSetLogfile.left = new FormAttachment( middle, 0 );
    fdSetLogfile.top = new FormAttachment( 0, margin );
    fdSetLogfile.right = new FormAttachment( 100, 0 );
    wSetLogfile.setLayoutData( fdSetLogfile );
    wSetLogfile.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        setActive();
      }
    } );
    // Append the logfile?
    wlAppendLogfile = new Label( wLogging, SWT.RIGHT );
    wlAppendLogfile.setText( BaseMessages.getString( PKG, "JobTrans.Append.Logfile.Label" ) );
    props.setLook( wlAppendLogfile );
    FormData fdlAppendLogfile = new FormData();
    fdlAppendLogfile.left = new FormAttachment( 0, 0 );
    fdlAppendLogfile.top = new FormAttachment( wSetLogfile, margin );
    fdlAppendLogfile.right = new FormAttachment( middle, -margin );
    wlAppendLogfile.setLayoutData( fdlAppendLogfile );
    wAppendLogfile = new Button( wLogging, SWT.CHECK );
    wAppendLogfile.setToolTipText( BaseMessages.getString( PKG, "JobTrans.Append.Logfile.Tooltip" ) );
    props.setLook( wAppendLogfile );
    FormData fdAppendLogfile = new FormData();
    fdAppendLogfile.left = new FormAttachment( middle, 0 );
    fdAppendLogfile.top = new FormAttachment( wSetLogfile, margin );
    fdAppendLogfile.right = new FormAttachment( 100, 0 );
    wAppendLogfile.setLayoutData( fdAppendLogfile );
    wAppendLogfile.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
      }
    } );

    // Set the logfile path + base-name
    wlLogfile = new Label( wLogging, SWT.RIGHT );
    wlLogfile.setText( BaseMessages.getString( PKG, "JobTrans.NameOfLogfile.Label" ) );
    props.setLook( wlLogfile );
    FormData fdlLogfile = new FormData();
    fdlLogfile.left = new FormAttachment( 0, 0 );
    fdlLogfile.top = new FormAttachment( wAppendLogfile, margin );
    fdlLogfile.right = new FormAttachment( middle, -margin );
    wlLogfile.setLayoutData( fdlLogfile );

    wbLogFilename = new Button( wLogging, SWT.PUSH | SWT.CENTER );
    props.setLook( wbLogFilename );
    wbLogFilename.setText( BaseMessages.getString( PKG, "JobTrans.Browse.Label" ) );
    fdbLogFilename = new FormData();
    fdbLogFilename.top = new FormAttachment( wAppendLogfile, margin );
    fdbLogFilename.right = new FormAttachment( 100, 0 );
    wbLogFilename.setLayoutData( fdbLogFilename );

    wLogfile = new TextVar( jobMeta, wLogging, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wLogfile.setText( "" );
    props.setLook( wLogfile );
    FormData fdLogfile = new FormData();
    fdLogfile.left = new FormAttachment( middle, 0 );
    fdLogfile.top = new FormAttachment( wAppendLogfile, margin );
    fdLogfile.right = new FormAttachment( wbLogFilename, -margin );
    wLogfile.setLayoutData( fdLogfile );

    // create parent folder?
    wlCreateParentFolder = new Label( wLogging, SWT.RIGHT );
    wlCreateParentFolder.setText( BaseMessages.getString( PKG, "JobTrans.Logfile.CreateParentFolder.Label" ) );
    props.setLook( wlCreateParentFolder );
    fdlCreateParentFolder = new FormData();
    fdlCreateParentFolder.left = new FormAttachment( 0, 0 );
    fdlCreateParentFolder.top = new FormAttachment( wLogfile, margin );
    fdlCreateParentFolder.right = new FormAttachment( middle, -margin );
    wlCreateParentFolder.setLayoutData( fdlCreateParentFolder );
    wCreateParentFolder = new Button( wLogging, SWT.CHECK );
    wCreateParentFolder.setToolTipText( BaseMessages
      .getString( PKG, "JobTrans.Logfile.CreateParentFolder.Tooltip" ) );
    props.setLook( wCreateParentFolder );
    fdCreateParentFolder = new FormData();
    fdCreateParentFolder.left = new FormAttachment( middle, 0 );
    fdCreateParentFolder.top = new FormAttachment( wLogfile, margin );
    fdCreateParentFolder.right = new FormAttachment( 100, 0 );
    wCreateParentFolder.setLayoutData( fdCreateParentFolder );
    wCreateParentFolder.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
      }
    } );

    // Set the logfile filename extention
    wlLogext = new Label( wLogging, SWT.RIGHT );
    wlLogext.setText( BaseMessages.getString( PKG, "JobTrans.LogfileExtension.Label" ) );
    props.setLook( wlLogext );
    FormData fdlLogext = new FormData();
    fdlLogext.left = new FormAttachment( 0, 0 );
    fdlLogext.top = new FormAttachment( wCreateParentFolder, margin );
    fdlLogext.right = new FormAttachment( middle, -margin );
    wlLogext.setLayoutData( fdlLogext );
    wLogext = new TextVar( jobMeta, wLogging, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wLogext.setText( "" );
    props.setLook( wLogext );
    FormData fdLogext = new FormData();
    fdLogext.left = new FormAttachment( middle, 0 );
    fdLogext.top = new FormAttachment( wCreateParentFolder, margin );
    fdLogext.right = new FormAttachment( 100, 0 );
    wLogext.setLayoutData( fdLogext );

    // Add date to logfile name?
    wlAddDate = new Label( wLogging, SWT.RIGHT );
    wlAddDate.setText( BaseMessages.getString( PKG, "JobTrans.Logfile.IncludeDate.Label" ) );
    props.setLook( wlAddDate );
    FormData fdlAddDate = new FormData();
    fdlAddDate.left = new FormAttachment( 0, 0 );
    fdlAddDate.top = new FormAttachment( wLogext, margin );
    fdlAddDate.right = new FormAttachment( middle, -margin );
    wlAddDate.setLayoutData( fdlAddDate );
    wAddDate = new Button( wLogging, SWT.CHECK );
    props.setLook( wAddDate );
    FormData fdAddDate = new FormData();
    fdAddDate.left = new FormAttachment( middle, 0 );
    fdAddDate.top = new FormAttachment( wLogext, margin );
    fdAddDate.right = new FormAttachment( 100, 0 );
    wAddDate.setLayoutData( fdAddDate );

    // Add time to logfile name?
    wlAddTime = new Label( wLogging, SWT.RIGHT );
    wlAddTime.setText( BaseMessages.getString( PKG, "JobTrans.Logfile.IncludeTime.Label" ) );
    props.setLook( wlAddTime );
    FormData fdlAddTime = new FormData();
    fdlAddTime.left = new FormAttachment( 0, 0 );
    fdlAddTime.top = new FormAttachment( wlAddDate, margin );
    fdlAddTime.right = new FormAttachment( middle, -margin );
    wlAddTime.setLayoutData( fdlAddTime );
    wAddTime = new Button( wLogging, SWT.CHECK );
    props.setLook( wAddTime );
    FormData fdAddTime = new FormData();
    fdAddTime.left = new FormAttachment( middle, 0 );
    fdAddTime.top = new FormAttachment( wlAddDate, margin );
    fdAddTime.right = new FormAttachment( 100, 0 );
    wAddTime.setLayoutData( fdAddTime );

    wlLoglevel = new Label( wLogging, SWT.RIGHT );
    wlLoglevel.setText( BaseMessages.getString( PKG, "JobTrans.Loglevel.Label" ) );
    props.setLook( wlLoglevel );
    FormData fdlLoglevel = new FormData();
    fdlLoglevel.left = new FormAttachment( 0, 0 );
    fdlLoglevel.right = new FormAttachment( middle, -margin );
    fdlLoglevel.top = new FormAttachment( wAddTime, margin );
    wlLoglevel.setLayoutData( fdlLoglevel );
    wLoglevel = new CCombo( wLogging, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER );
    wLoglevel.setItems( LogLevel.getLogLevelDescriptions() );
    props.setLook( wLoglevel );
    FormData fdLoglevel = new FormData();
    fdLoglevel.left = new FormAttachment( middle, 0 );
    fdLoglevel.top = new FormAttachment( wAddTime, margin );
    fdLoglevel.right = new FormAttachment( 100, 0 );
    wLoglevel.setLayoutData( fdLoglevel );

    FormData fdLogging = new FormData();
    fdLogging.left = new FormAttachment( 0, 0 );
    fdLogging.top = new FormAttachment( 0, 0 );
    fdLogging.right = new FormAttachment( 100, 0 );
    fdLogging.bottom = new FormAttachment( 100, 0 );
    wLogging.setLayoutData( fdLogging );

    wLogging.pack();
    bounds = wLogging.getBounds();

    wSLogging.setContent( wLogging );
    wSLogging.setExpandHorizontal( true );
    wSLogging.setExpandVertical( true );
    wSLogging.setMinWidth( bounds.width );
    wSLogging.setMinHeight( bounds.height );

    wLoggingTab.setControl( wSLogging );

    // Arguments
    //
    CTabItem wFieldTab = new CTabItem( wTabFolder, SWT.NONE );
    wFieldTab.setText( BaseMessages.getString( PKG, "JobTrans.Fields.Argument.Label" ) );

    FormLayout fieldLayout = new FormLayout();
    fieldLayout.marginWidth = Const.MARGIN;
    fieldLayout.marginHeight = Const.MARGIN;

    Composite wFieldComp = new Composite( wTabFolder, SWT.NONE );
    props.setLook( wFieldComp );
    wFieldComp.setLayout( fieldLayout );

    final int FieldsCols = 1;
    int rows = jobEntry.arguments == null ? 1 : ( jobEntry.arguments.length == 0 ? 0 : jobEntry.arguments.length );
    final int FieldsRows = rows;

    ColumnInfo[] colinf = new ColumnInfo[ FieldsCols ];
    colinf[ 0 ] =
      new ColumnInfo(
        BaseMessages.getString( PKG, "JobTrans.Fields.Argument.Label" ), ColumnInfo.COLUMN_TYPE_TEXT, false );
    colinf[ 0 ].setUsingVariables( true );

    wFields =
      new TableView(
        jobMeta, wFieldComp, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, colinf, FieldsRows, lsMod, props );

    FormData fdFields = new FormData();
    fdFields.left = new FormAttachment( 0, 0 );
    fdFields.top = new FormAttachment( 0, margin );
    fdFields.right = new FormAttachment( 100, 0 );
    fdFields.bottom = new FormAttachment( 100, 0 );
    wFields.setLayoutData( fdFields );

    FormData fdFieldsComp = new FormData();
    fdFieldsComp.left = new FormAttachment( 0, 0 );
    fdFieldsComp.top = new FormAttachment( 0, 0 );
    fdFieldsComp.right = new FormAttachment( 100, 0 );
    fdFieldsComp.bottom = new FormAttachment( 100, 0 );
    wFieldComp.setLayoutData( fdFieldsComp );

    wFieldComp.layout();
    wFieldTab.setControl( wFieldComp );

    // The parameters tab
    CTabItem wParametersTab = new CTabItem( wTabFolder, SWT.NONE );
    wParametersTab.setText( BaseMessages.getString( PKG, "JobTrans.Fields.Parameters.Label" ) );

    fieldLayout = new FormLayout();
    fieldLayout.marginWidth = Const.MARGIN;
    fieldLayout.marginHeight = Const.MARGIN;

    Composite wParameterComp = new Composite( wTabFolder, SWT.NONE );
    props.setLook( wParameterComp );
    wParameterComp.setLayout( fieldLayout );

    // Pass all parameters down
    //
    wlPassParams = new Label( wParameterComp, SWT.RIGHT );
    wlPassParams.setText( BaseMessages.getString( PKG, "JobTrans.PassAllParameters.Label" ) );
    props.setLook( wlPassParams );
    FormData fdlPassParams = new FormData();
    fdlPassParams.left = new FormAttachment( 0, 0 );
    fdlPassParams.top = new FormAttachment( 0, 0 );
    fdlPassParams.right = new FormAttachment( middle, -margin );
    wlPassParams.setLayoutData( fdlPassParams );
    wPassParams = new Button( wParameterComp, SWT.CHECK );
    props.setLook( wPassParams );
    FormData fdPassParams = new FormData();
    fdPassParams.left = new FormAttachment( middle, 0 );
    fdPassParams.top = new FormAttachment( 0, 0 );
    fdPassParams.right = new FormAttachment( 100, 0 );
    wPassParams.setLayoutData( fdPassParams );

    wbGetParams = new Button( wParameterComp, SWT.PUSH );
    wbGetParams.setText( BaseMessages.getString( PKG, "JobTrans.GetParameters.Button.Label" ) );
    FormData fdGetParams = new FormData();
    fdGetParams.top = new FormAttachment( wPassParams, margin );
    fdGetParams.right = new FormAttachment( 100, 0 );
    wbGetParams.setLayoutData( fdGetParams );
    wbGetParams.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent arg0 ) {
        getParameters( null ); // force reload from file specification
      }
    } );

    final int parameterRows = jobEntry.parameters != null ? jobEntry.parameters.length : 0;

    colinf =
      new ColumnInfo[] {
        new ColumnInfo(
          BaseMessages.getString( PKG, "JobTrans.Parameters.Parameter.Label" ), ColumnInfo.COLUMN_TYPE_TEXT,
          false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "JobTrans.Parameters.ColumnName.Label" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "JobTrans.Parameters.Value.Label" ), ColumnInfo.COLUMN_TYPE_TEXT,
          false ), };
    colinf[ 2 ].setUsingVariables( true );

    wParameters =
      new TableView(
        jobMeta, wParameterComp, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, colinf, parameterRows, lsMod,
        props );

    FormData fdParameters = new FormData();
    fdParameters.left = new FormAttachment( 0, 0 );
    fdParameters.top = new FormAttachment( wPassParams, margin );
    fdParameters.right = new FormAttachment( wbGetParams, -margin );
    fdParameters.bottom = new FormAttachment( 100, 0 );
    wParameters.setLayoutData( fdParameters );

    FormData fdParametersComp = new FormData();
    fdParametersComp.left = new FormAttachment( 0, 0 );
    fdParametersComp.top = new FormAttachment( 0, 0 );
    fdParametersComp.right = new FormAttachment( 100, 0 );
    fdParametersComp.bottom = new FormAttachment( 100, 0 );
    wParameterComp.setLayoutData( fdParametersComp );

    wParameterComp.layout();
    wParametersTab.setControl( wParameterComp );

    FormData fdTabFolder = new FormData();
    fdTabFolder.left = new FormAttachment( 0, 0 );
    fdTabFolder.top = new FormAttachment( wName, margin * 3 );
    fdTabFolder.right = new FormAttachment( 100, 0 );
    fdTabFolder.bottom = new FormAttachment( 100, -50 );
    wTabFolder.setLayoutData( fdTabFolder );

    wTabFolder.setSelection( 0 );

    // Some buttons
    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );

    BaseStepDialog.positionBottomButtons( shell, new Button[] { wOK, wCancel }, margin, wTabFolder );

    // Add listeners
    lsCancel = new Listener() {
      public void handleEvent( Event e ) {
        cancel();
      }
    };
    lsOK = new Listener() {
      public void handleEvent( Event e ) {
        ok();
      }
    };

    wOK.addListener( SWT.Selection, lsOK );
    wCancel.addListener( SWT.Selection, lsCancel );

    lsDef = new SelectionAdapter() {
      public void widgetDefaultSelected( SelectionEvent e ) {
        ok();
      }
    };
    wName.addSelectionListener( lsDef );
    wFilename.addSelectionListener( lsDef );

    wbTransname.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        selectTransformation();
      }
    } );

    wbFilename.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        pickFileVFS();
      }
    } );

    wbByReference.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        selectTransformationByReference();
      }
    } );

    wbLogFilename.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {

        FileDialog dialog = new FileDialog( shell, SWT.SAVE );
        dialog.setFilterExtensions( new String[] { "*.txt", "*'.log", "*" } );
        dialog.setFilterNames( FILE_FILTERLOGNAMES );

        if ( wLogfile.getText() != null ) {
          dialog.setFileName( jobMeta.environmentSubstitute( wLogfile.getText() ) );
        }

        if ( dialog.open() != null ) {
          wLogfile.setText( dialog.getFilterPath() + Const.FILE_SEPARATOR + dialog.getFileName() );
          String filename = dialog.getFilterPath() + Const.FILE_SEPARATOR + dialog.getFileName();
          FileObject file = null;
          try {
            file = KettleVFS.getFileObject( filename );
            // Set file extension ..
            wLogext.setText( file.getName().getExtension() );
            // Set filename without extension ...
            wLogfile.setText( wLogfile.getText().substring(
              0, wLogfile.getText().length() - wLogext.getText().length() - 1 ) );
          } catch ( Exception ex ) {
            // Ignore
          }
          if ( file != null ) {
            try {
              file.close();
            } catch ( IOException ex ) { /* Ignore */
            }
          }
        }

      }
    } );

    // Detect [X] or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );

    getData();
    setActive();

    BaseStepDialog.setSize( shell );

    shell.open();
    props.setDialogSize( shell, "JobTransDialogSize" );
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
    return jobEntry;
  }

  /**
   * Ask the user to fill in the details...
   */
  protected void newTransformation() {
    TransMeta newTransMeta = new TransMeta();

    newTransMeta.getDatabases().addAll( jobMeta.getDatabases() );
    newTransMeta.setRepository( rep );
    newTransMeta.setRepositoryDirectory( jobMeta.getRepositoryDirectory() );
    newTransMeta.setMetaStore( metaStore );

    TransDialog transDialog = new TransDialog( shell, SWT.NONE, newTransMeta, rep );
    if ( transDialog.open() != null ) {
      Spoon spoon = Spoon.getInstance();
      spoon.addTransGraph( newTransMeta );
      boolean saved = false;
      try {
        if ( rep != null ) {
          if ( !Const.isEmpty( newTransMeta.getName() ) ) {
            wName.setText( newTransMeta.getName() );
          }
          saved = spoon.saveToRepository( newTransMeta, false );
          if ( rep.getRepositoryMeta().getRepositoryCapabilities().supportsReferences() ) {
            specificationMethod = ObjectLocationSpecificationMethod.REPOSITORY_BY_REFERENCE;
            referenceObjectId = newTransMeta.getObjectId();
          } else {
            specificationMethod = ObjectLocationSpecificationMethod.REPOSITORY_BY_NAME;
          }
        } else {
          saved = spoon.saveToFile( newTransMeta );
          specificationMethod = ObjectLocationSpecificationMethod.FILENAME;
        }
      } catch ( Exception e ) {
        new ErrorDialog( shell, "Error", "Error saving new transformation", e );
      }
      if ( saved ) {
        setRadioButtons();
        switch( specificationMethod ) {
          case FILENAME:
            wFilename.setText( Const.NVL( newTransMeta.getFilename(), "" ) );
            break;
          case REPOSITORY_BY_NAME:
            wTransname.setText( Const.NVL( newTransMeta.getName(), "" ) );
            wDirectory.setText( newTransMeta.getRepositoryDirectory().getPath() );
            break;
          case REPOSITORY_BY_REFERENCE:
            getByReferenceData( newTransMeta.getObjectId() );
            break;
          default:
            break;
        }
        getParameters( newTransMeta );
      }

    }
  }

  protected void getParameters( TransMeta inputTransMeta ) {
    try {
      if ( inputTransMeta == null ) {
        JobEntryTrans jet = new JobEntryTrans();
        getInfo( jet );
        inputTransMeta = jet.getTransMeta( rep, metaStore, jobMeta );
      }
      String[] parameters = inputTransMeta.listParameters();

      String[] existing = wParameters.getItems( 1 );

      for ( int i = 0; i < parameters.length; i++ ) {
        if ( Const.indexOfString( parameters[ i ], existing ) < 0 ) {
          TableItem item = new TableItem( wParameters.table, SWT.NONE );
          item.setText( 1, parameters[ i ] );
        }
      }
      wParameters.removeEmptyRows();
      wParameters.setRowNums();
      wParameters.optWidth( true );
    } catch ( Exception e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "JobEntryTransDialog.Exception.UnableToLoadTransformation.Title" ),
        BaseMessages.getString( PKG, "JobEntryTransDialog.Exception.UnableToLoadTransformation.Message" ), e );
    }

  }

  protected void setRadioButtons() {
    radioFilename.setSelection( specificationMethod == ObjectLocationSpecificationMethod.FILENAME );
    radioByName.setSelection( specificationMethod == ObjectLocationSpecificationMethod.REPOSITORY_BY_NAME );
    radioByReference
      .setSelection( specificationMethod == ObjectLocationSpecificationMethod.REPOSITORY_BY_REFERENCE );
    setActive();
  }

  protected void selectTransformation() {
    if ( rep != null ) {
      SelectObjectDialog sod = new SelectObjectDialog( shell, rep, true, false );
      String transname = sod.open();
      if ( transname != null ) {
        wTransname.setText( transname );
        wDirectory.setText( sod.getDirectory().getPath() );
        // Copy it to the job entry name too...
        wName.setText( wTransname.getText() );
      }
    }
  }

  protected void selectTransformationByReference() {
    if ( rep != null ) {
      SelectObjectDialog sod = new SelectObjectDialog( shell, rep, true, false );
      sod.open();
      RepositoryElementMetaInterface repositoryObject = sod.getRepositoryObject();
      if ( repositoryObject != null ) {
        specificationMethod = ObjectLocationSpecificationMethod.REPOSITORY_BY_REFERENCE;
        updateByReferenceField( repositoryObject );
        referenceObjectId = repositoryObject.getObjectId();
        setRadioButtons();
      }
    }
  }

  protected void pickFileVFS() {

    FileDialog dialog = new FileDialog( shell, SWT.OPEN );
    dialog.setFilterExtensions( Const.STRING_TRANS_FILTER_EXT );
    dialog.setFilterNames( Const.getTransformationFilterNames() );
    String prevName = jobMeta.environmentSubstitute( wFilename.getText() );
    String parentFolder = null;
    try {
      parentFolder =
        KettleVFS.getFilename( KettleVFS
          .getFileObject( jobMeta.environmentSubstitute( jobMeta.getFilename() ) ).getParent() );
    } catch ( Exception e ) {
      // not that important
    }
    if ( !Const.isEmpty( prevName ) ) {
      try {
        if ( KettleVFS.fileExists( prevName ) ) {
          dialog.setFilterPath( KettleVFS.getFilename( KettleVFS.getFileObject( prevName ).getParent() ) );
        } else {

          if ( !prevName.endsWith( ".ktr" ) ) {
            prevName =
              "${"
                + Const.INTERNAL_VARIABLE_JOB_FILENAME_DIRECTORY + "}/" + Const.trim( wFilename.getText() )
                + ".ktr";
          }
          if ( KettleVFS.fileExists( prevName ) ) {
            specificationMethod = ObjectLocationSpecificationMethod.FILENAME;
            setRadioButtons();
            wFilename.setText( prevName );
            return;
          } else {
            // File specified doesn't exist. Ask if we should create the file...
            //
            MessageBox mb = new MessageBox( shell, SWT.YES | SWT.NO | SWT.ICON_QUESTION );
            mb.setMessage( BaseMessages.getString( PKG, "JobTrans.Dialog.CreateTransformationQuestion.Message" ) );
            mb.setText( BaseMessages.getString( PKG, "JobTrans.Dialog.CreateTransformationQuestion.Title" ) ); // Sorry!
            int answer = mb.open();
            if ( answer == SWT.YES ) {

              Spoon spoon = Spoon.getInstance();
              spoon.newTransFile();
              TransMeta transMeta = spoon.getActiveTransformation();
              transMeta.initializeVariablesFrom( jobEntry );
              transMeta.setFilename( jobMeta.environmentSubstitute( prevName ) );
              wFilename.setText( prevName );
              specificationMethod = ObjectLocationSpecificationMethod.FILENAME;
              setRadioButtons();
              spoon.saveFile();
              return;
            }
          }
        }
      } catch ( Exception e ) {
        dialog.setFilterPath( parentFolder );
      }
    } else if ( !Const.isEmpty( parentFolder ) ) {
      dialog.setFilterPath( parentFolder );
    }

    String fname = dialog.open();
    if ( fname != null ) {
      File file = new File( fname );
      String name = file.getName();
      String parentFolderSelection = file.getParentFile().toString();

      if ( !Const.isEmpty( parentFolder ) && parentFolder.equals( parentFolderSelection ) ) {
        wFilename.setText( "${" + Const.INTERNAL_VARIABLE_JOB_FILENAME_DIRECTORY + "}/" + name );
      } else {
        wFilename.setText( fname );
      }

    }
  }

  public void dispose() {
    WindowProperty winprop = new WindowProperty( shell );
    props.setScreen( winprop );
    shell.dispose();
  }

  public void setActive() {
    boolean supportsReferences =
      rep != null && rep.getRepositoryMeta().getRepositoryCapabilities().supportsReferences();

    wbLogFilename.setEnabled( wSetLogfile.getSelection() );
    radioByName.setEnabled( rep != null );
    radioByReference.setEnabled( rep != null && supportsReferences );
    wFilename.setEnabled( radioFilename.getSelection() );
    wbFilename.setEnabled( radioFilename.getSelection() );
    wTransname.setEnabled( rep != null && radioByName.getSelection() );

    wDirectory.setEnabled( rep != null && radioByName.getSelection() );

    wbTransname.setEnabled( rep != null && radioByName.getSelection() );

    wByReference.setEnabled( rep != null && radioByReference.getSelection() && supportsReferences );
    wbByReference.setEnabled( rep != null && radioByReference.getSelection() && supportsReferences );

    wlLogfile.setEnabled( wSetLogfile.getSelection() );
    wLogfile.setEnabled( wSetLogfile.getSelection() );

    wlLogext.setEnabled( wSetLogfile.getSelection() );
    wLogext.setEnabled( wSetLogfile.getSelection() );

    wlCreateParentFolder.setEnabled( wSetLogfile.getSelection() );
    wCreateParentFolder.setEnabled( wSetLogfile.getSelection() );

    wlAddDate.setEnabled( wSetLogfile.getSelection() );
    wAddDate.setEnabled( wSetLogfile.getSelection() );

    wlAddTime.setEnabled( wSetLogfile.getSelection() );
    wAddTime.setEnabled( wSetLogfile.getSelection() );

    wlLoglevel.setEnabled( wSetLogfile.getSelection() );
    wLoglevel.setEnabled( wSetLogfile.getSelection() );

    wAppendLogfile.setEnabled( wSetLogfile.getSelection() );
    wlAppendLogfile.setEnabled( wSetLogfile.getSelection() );

    wSlaveServer.setEnabled( !wCluster.getSelection() );
    wlSlaveServer.setEnabled( !wCluster.getSelection() );

    wlWaitingToFinish.setEnabled( !wCluster.getSelection() && !Const.isEmpty( wSlaveServer.getText() ) );
    wWaitingToFinish.setEnabled( !wCluster.getSelection() && !Const.isEmpty( wSlaveServer.getText() ) );

    wlFollowingAbortRemotely.setEnabled( !wCluster.getSelection()
      && wWaitingToFinish.getSelection() && !Const.isEmpty( wSlaveServer.getText() ) );
    wFollowingAbortRemotely.setEnabled( !wCluster.getSelection()
      && wWaitingToFinish.getSelection() && !Const.isEmpty( wSlaveServer.getText() ) );

    wlLogRemoteWork.setEnabled( wCluster.getSelection() );
    wLogRemoteWork.setEnabled( wCluster.getSelection() );
  }

  public void getData() {
    wName.setText( Const.NVL( jobEntry.getName(), "" ) );

    specificationMethod = jobEntry.getSpecificationMethod();
    switch( specificationMethod ) {
      case FILENAME:
        wFilename.setText( Const.NVL( jobEntry.getFilename(), "" ) );
        break;
      case REPOSITORY_BY_NAME:
        wDirectory.setText( Const.NVL( jobEntry.getDirectory(), "" ) );
        wTransname.setText( Const.NVL( jobEntry.getTransname(), "" ) );
        break;
      case REPOSITORY_BY_REFERENCE:
        referenceObjectId = jobEntry.getTransObjectId();
        wByReference.setText( "" );
        if ( rep != null && jobEntry.getTransObjectId() != null ) {
          getByReferenceData( jobEntry.getTransObjectId() );
        }
        break;
      default:
        break;
    }
    setRadioButtons();

    // Arguments
    if ( jobEntry.arguments != null ) {
      for ( int i = 0; i < jobEntry.arguments.length; i++ ) {
        TableItem ti = wFields.table.getItem( i );
        if ( jobEntry.arguments[ i ] != null ) {
          ti.setText( 1, jobEntry.arguments[ i ] );
        }
      }
      wFields.setRowNums();
      wFields.optWidth( true );
    }

    // Parameters
    if ( jobEntry.parameters != null ) {
      for ( int i = 0; i < jobEntry.parameters.length; i++ ) {
        TableItem ti = wParameters.table.getItem( i );
        if ( !Const.isEmpty( jobEntry.parameters[ i ] ) ) {
          ti.setText( 1, Const.NVL( jobEntry.parameters[ i ], "" ) );
          ti.setText( 2, Const.NVL( jobEntry.parameterFieldNames[ i ], "" ) );
          ti.setText( 3, Const.NVL( jobEntry.parameterValues[ i ], "" ) );
        }
      }
      wParameters.setRowNums();
      wParameters.optWidth( true );
    }

    wPassParams.setSelection( jobEntry.isPassingAllParameters() );

    if ( jobEntry.logfile != null ) {
      wLogfile.setText( jobEntry.logfile );
    }
    if ( jobEntry.logext != null ) {
      wLogext.setText( jobEntry.logext );
    }

    wPrevious.setSelection( jobEntry.argFromPrevious );
    wPrevToParams.setSelection( jobEntry.paramsFromPrevious );
    wEveryRow.setSelection( jobEntry.execPerRow );
    wSetLogfile.setSelection( jobEntry.setLogfile );
    wAddDate.setSelection( jobEntry.addDate );
    wAddTime.setSelection( jobEntry.addTime );
    wClearRows.setSelection( jobEntry.clearResultRows );
    wClearFiles.setSelection( jobEntry.clearResultFiles );
    wCluster.setSelection( jobEntry.isClustering() );
    wLogRemoteWork.setSelection( jobEntry.isLoggingRemoteWork() );
    if ( jobEntry.getRemoteSlaveServerName() != null ) {
      wSlaveServer.setText( jobEntry.getRemoteSlaveServerName() );
    }
    wWaitingToFinish.setSelection( jobEntry.isWaitingToFinish() );
    wFollowingAbortRemotely.setSelection( jobEntry.isFollowingAbortRemotely() );
    wAppendLogfile.setSelection( jobEntry.setAppendLogfile );
    wCreateParentFolder.setSelection( jobEntry.createParentFolder );
    if ( jobEntry.logFileLevel != null ) {
      wLoglevel.select( jobEntry.logFileLevel.getLevel() );
    }
  }

  private void getByReferenceData( ObjectId transObjectId ) {
    try {
      RepositoryObject transInf = rep.getObjectInformation( transObjectId, RepositoryObjectType.TRANSFORMATION );
      updateByReferenceField( transInf );
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "JobEntryTransDialog.Exception.UnableToReferenceObjectId.Title" ),
        BaseMessages.getString( PKG, "JobEntryTransDialog.Exception.UnableToReferenceObjectId.Message" ), e );
    }
  }

  private void updateByReferenceField( RepositoryElementMetaInterface element ) {
    String path = getPathOf( element );
    if ( path == null ) {
      path = "";
    }
    wByReference.setText( path );
  }

  private void cancel() {
    jobEntry.setChanged( backupChanged );

    jobEntry = null;
    dispose();
  }

  private void getInfo( JobEntryTrans jet ) throws KettleException {
    jet.setName( wName.getText() );
    jet.setSpecificationMethod( specificationMethod );
    switch( specificationMethod ) {
      case FILENAME:
        jet.setFileName( wFilename.getText() );
        if ( jet.getFilename().isEmpty() ) {
          throw new KettleException( BaseMessages.getString( PKG,
            "JobTrans.Dialog.Exception.NoValidMappingDetailsFound" ) );
        }

        jet.setDirectory( null );
        jet.setTransname( null );
        jet.setTransObjectId( null );
        break;
      case REPOSITORY_BY_NAME:
        jet.setDirectory( wDirectory.getText() );
        if ( jet.getDirectory().isEmpty() ) {
          throw new KettleException( BaseMessages.getString( PKG,
            "JobTrans.Dialog.Exception.UnableToFindRepositoryDirectory" ) );
        }

        jet.setTransname( wTransname.getText() );
        jet.setFileName( null );
        jet.setTransObjectId( null );
        break;
      case REPOSITORY_BY_REFERENCE:
        if ( referenceObjectId == null ) {
          throw new KettleException( BaseMessages.getString( PKG,
            "JobTrans.Dialog.Exception.ReferencedTransformationIdIsNull" ) );
        }

        jet.setFileName( null );
        jet.setDirectory( null );
        jet.setTransname( null );
        jet.setTransObjectId( referenceObjectId );
        break;
      default:
        break;
    }

    int nritems = wFields.nrNonEmpty();
    int nr = 0;
    for ( int i = 0; i < nritems; i++ ) {
      String arg = wFields.getNonEmpty( i ).getText( 1 );
      if ( arg != null && arg.length() != 0 ) {
        nr++;
      }
    }
    jet.arguments = new String[ nr ];
    nr = 0;
    for ( int i = 0; i < nritems; i++ ) {
      String arg = wFields.getNonEmpty( i ).getText( 1 );
      if ( arg != null && arg.length() != 0 ) {
        jet.arguments[ nr ] = arg;
        nr++;
      }
    }

    // Do the parameters
    nritems = wParameters.nrNonEmpty();
    nr = 0;
    for ( int i = 0; i < nritems; i++ ) {
      String param = wParameters.getNonEmpty( i ).getText( 1 );
      if ( param != null && param.length() != 0 ) {
        nr++;
      }
    }
    jet.parameters = new String[ nr ];
    jet.parameterFieldNames = new String[ nr ];
    jet.parameterValues = new String[ nr ];
    nr = 0;
    for ( int i = 0; i < nritems; i++ ) {
      String param = wParameters.getNonEmpty( i ).getText( 1 );
      String fieldName = wParameters.getNonEmpty( i ).getText( 2 );
      String value = wParameters.getNonEmpty( i ).getText( 3 );

      jet.parameters[ nr ] = param;

      if ( !Const.isEmpty( Const.trim( fieldName ) ) ) {
        jet.parameterFieldNames[ nr ] = fieldName;
      } else {
        jet.parameterFieldNames[ nr ] = "";
      }

      if ( !Const.isEmpty( Const.trim( value ) ) ) {
        jet.parameterValues[ nr ] = value;
      } else {
        jet.parameterValues[ nr ] = "";
      }

      nr++;
    }

    jet.setPassingAllParameters( wPassParams.getSelection() );

    jet.logfile = wLogfile.getText();
    jet.logext = wLogext.getText();

    if ( wLoglevel.getSelectionIndex() >= 0 ) {
      jet.logFileLevel = LogLevel.values()[ wLoglevel.getSelectionIndex() ];
    } else {
      jet.logFileLevel = LogLevel.BASIC;
    }

    jet.argFromPrevious = wPrevious.getSelection();
    jet.paramsFromPrevious = wPrevToParams.getSelection();
    jet.execPerRow = wEveryRow.getSelection();
    jet.setLogfile = wSetLogfile.getSelection();
    jet.addDate = wAddDate.getSelection();
    jet.addTime = wAddTime.getSelection();
    jet.clearResultRows = wClearRows.getSelection();
    jet.clearResultFiles = wClearFiles.getSelection();
    jet.setClustering( wCluster.getSelection() );
    jet.setLoggingRemoteWork( wLogRemoteWork.getSelection() );
    jet.createParentFolder = wCreateParentFolder.getSelection();

    jet.setRemoteSlaveServerName( wSlaveServer.getText() );
    jet.setAppendLogfile = wAppendLogfile.getSelection();
    jet.setWaitingToFinish( wWaitingToFinish.getSelection() );
    jet.setFollowingAbortRemotely( wFollowingAbortRemotely.getSelection() );

  }

  private void ok() {
    if ( Const.isEmpty( wName.getText() ) ) {
      MessageBox mb = new MessageBox( shell, SWT.OK | SWT.ICON_ERROR );
      mb.setText( BaseMessages.getString( PKG, "System.StepJobEntryNameMissing.Title" ) );
      mb.setMessage( BaseMessages.getString( PKG, "System.JobEntryNameMissing.Msg" ) );
      mb.open();
      return;
    }
    jobEntry.setName( wName.getText() );

    try {
      getInfo( jobEntry );
      jobEntry.setChanged();
      dispose();
    } catch ( KettleException e ) {
      new ErrorDialog( shell, BaseMessages.getString( PKG, "JobTrans.Dialog.ErrorShowingTransformation.Title" ),
        BaseMessages.getString( PKG, "JobTrans.Dialog.ErrorShowingTransformation.Message" ), e );
    }
  }
}
