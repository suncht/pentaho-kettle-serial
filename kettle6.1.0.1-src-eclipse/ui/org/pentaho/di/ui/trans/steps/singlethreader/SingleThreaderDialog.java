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

package org.pentaho.di.ui.trans.steps.singlethreader;

import java.io.IOException;

import org.apache.commons.vfs2.FileObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.ObjectLocationSpecificationMethod;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.gui.SpoonFactory;
import org.pentaho.di.core.gui.SpoonInterface;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.RepositoryElementMetaInterface;
import org.pentaho.di.repository.RepositoryObject;
import org.pentaho.di.repository.RepositoryObjectType;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.mappinginput.MappingInputMeta;
import org.pentaho.di.trans.steps.mappingoutput.MappingOutputMeta;
import org.pentaho.di.trans.steps.singlethreader.SingleThreaderMeta;
import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.LabelTextVar;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.repository.dialog.SelectObjectDialog;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.trans.dialog.TransDialog;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.vfs.ui.VfsFileChooserDialog;

public class SingleThreaderDialog extends BaseStepDialog implements StepDialogInterface {
  private static Class<?> PKG = SingleThreaderMeta.class; // for i18n purposes, needed by Translator2!!

  private SingleThreaderMeta singleThreaderMeta;

  private Group gTransGroup;

  // File
  //
  private Button radioFilename;
  private Button wbbFilename;
  private TextVar wFilename;

  // Repository by name
  //
  private Button radioByName;
  private TextVar wTransname, wDirectory;
  private Button wbTrans;

  // Repository by reference
  //
  private Button radioByReference;
  private Button wbByReference;
  private TextVar wByReference;

  // Edit the mapping transformation in Spoon
  //
  private Button wEditTrans;
  private Button wNewTrans;

  private LabelTextVar wBatchSize;
  private LabelTextVar wInjectStep;
  private Button wGetInjectStep;
  private LabelTextVar wRetrieveStep;
  private Button wGetRetrieveStep;

  private TableView wParameters;

  private TransMeta mappingTransMeta = null;

  protected boolean transModified;

  private ModifyListener lsMod;

  private int middle;

  private int margin;

  private ObjectId referenceObjectId;
  private ObjectLocationSpecificationMethod specificationMethod;

  private Group gParametersGroup;

  private Button wPassParams;

  private Button wbGetParams;

  private LabelTextVar wBatchTime;

  public SingleThreaderDialog( Shell parent, Object in, TransMeta tr, String sname ) {
    super( parent, (BaseStepMeta) in, tr, sname );
    singleThreaderMeta = (SingleThreaderMeta) in;
    transModified = false;
  }

  public String open() {
    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MIN | SWT.MAX );
    props.setLook( shell );
    setShellImage( shell, singleThreaderMeta );

    lsMod = new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        singleThreaderMeta.setChanged();
      }
    };
    changed = singleThreaderMeta.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );
    shell.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.Shell.Title" ) );

    middle = props.getMiddlePct();
    margin = Const.MARGIN;

    // Stepname line
    wlStepname = new Label( shell, SWT.RIGHT );
    wlStepname.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.Stepname.Label" ) );
    props.setLook( wlStepname );
    fdlStepname = new FormData();
    fdlStepname.left = new FormAttachment( 0, 0 );
    fdlStepname.right = new FormAttachment( middle, -margin );
    fdlStepname.top = new FormAttachment( 0, margin );
    wlStepname.setLayoutData( fdlStepname );
    wStepname = new Text( shell, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    wStepname.setText( stepname );
    props.setLook( wStepname );
    wStepname.addModifyListener( lsMod );
    fdStepname = new FormData();
    fdStepname.left = new FormAttachment( middle, 0 );
    fdStepname.top = new FormAttachment( 0, margin );
    fdStepname.right = new FormAttachment( 100, 0 );
    wStepname.setLayoutData( fdStepname );

    // Show a group with 2 main options: a transformation in the repository
    // or on file
    //

    // //////////////////////////////////////////////////
    // The sub-transformation definition box
    // //////////////////////////////////////////////////
    //
    gTransGroup = new Group( shell, SWT.SHADOW_ETCHED_IN );
    gTransGroup.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.TransGroup.Label" ) );
    gTransGroup.setBackground( shell.getBackground() ); // the default looks
    // ugly
    FormLayout transGroupLayout = new FormLayout();
    transGroupLayout.marginLeft = margin * 2;
    transGroupLayout.marginTop = margin * 2;
    transGroupLayout.marginRight = margin * 2;
    transGroupLayout.marginBottom = margin * 2;
    gTransGroup.setLayout( transGroupLayout );

    // Radio button: The mapping is in a file
    //
    radioFilename = new Button( gTransGroup, SWT.RADIO );
    props.setLook( radioFilename );
    radioFilename.setSelection( false );
    radioFilename.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.RadioFile.Label" ) );
    radioFilename
      .setToolTipText( BaseMessages.getString( PKG, "SingleThreaderDialog.RadioFile.Tooltip", Const.CR ) );
    FormData fdFileRadio = new FormData();
    fdFileRadio.left = new FormAttachment( 0, 0 );
    fdFileRadio.right = new FormAttachment( 100, 0 );
    fdFileRadio.top = new FormAttachment( 0, 0 );
    radioFilename.setLayoutData( fdFileRadio );
    radioFilename.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        specificationMethod = ObjectLocationSpecificationMethod.FILENAME;
        setRadioButtons();
      }
    } );

    wbbFilename = new Button( gTransGroup, SWT.PUSH | SWT.CENTER ); // Browse
    props.setLook( wbbFilename );
    wbbFilename.setText( BaseMessages.getString( PKG, "System.Button.Browse" ) );
    wbbFilename.setToolTipText( BaseMessages.getString( PKG, "System.Tooltip.BrowseForFileOrDirAndAdd" ) );
    FormData fdbFilename = new FormData();
    fdbFilename.right = new FormAttachment( 100, 0 );
    fdbFilename.top = new FormAttachment( radioFilename, margin );
    wbbFilename.setLayoutData( fdbFilename );
    wbbFilename.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        selectFileTrans();
      }
    } );

    wFilename = new TextVar( transMeta, gTransGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wFilename );
    wFilename.addModifyListener( lsMod );
    FormData fdFilename = new FormData();
    fdFilename.left = new FormAttachment( 0, 25 );
    fdFilename.right = new FormAttachment( wbbFilename, -margin );
    fdFilename.top = new FormAttachment( wbbFilename, 0, SWT.CENTER );
    wFilename.setLayoutData( fdFilename );
    wFilename.addModifyListener( new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        specificationMethod = ObjectLocationSpecificationMethod.FILENAME;
        setRadioButtons();
      }
    } );

    // Radio button: The mapping is in the repository
    //
    radioByName = new Button( gTransGroup, SWT.RADIO );
    props.setLook( radioByName );
    radioByName.setSelection( false );
    radioByName.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.RadioRep.Label" ) );
    radioByName.setToolTipText( BaseMessages.getString( PKG, "SingleThreaderDialog.RadioRep.Tooltip", Const.CR ) );
    FormData fdRepRadio = new FormData();
    fdRepRadio.left = new FormAttachment( 0, 0 );
    fdRepRadio.right = new FormAttachment( 100, 0 );
    fdRepRadio.top = new FormAttachment( wbbFilename, 2 * margin );
    radioByName.setLayoutData( fdRepRadio );
    radioByName.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        specificationMethod = ObjectLocationSpecificationMethod.REPOSITORY_BY_NAME;
        setRadioButtons();
      }
    } );
    wbTrans = new Button( gTransGroup, SWT.PUSH | SWT.CENTER ); // Browse
    props.setLook( wbTrans );
    wbTrans.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.Select.Button" ) );
    wbTrans.setToolTipText( BaseMessages.getString( PKG, "System.Tooltip.BrowseForFileOrDirAndAdd" ) );
    FormData fdbTrans = new FormData();
    fdbTrans.right = new FormAttachment( 100, 0 );
    fdbTrans.top = new FormAttachment( radioByName, 2 * margin );
    wbTrans.setLayoutData( fdbTrans );
    wbTrans.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        selectRepositoryTrans();
      }
    } );

    wDirectory = new TextVar( transMeta, gTransGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wDirectory );
    wDirectory.addModifyListener( lsMod );
    FormData fdTransDir = new FormData();
    fdTransDir.left = new FormAttachment( middle + ( 100 - middle ) / 2, 0 );
    fdTransDir.right = new FormAttachment( wbTrans, -margin );
    fdTransDir.top = new FormAttachment( wbTrans, 0, SWT.CENTER );
    wDirectory.setLayoutData( fdTransDir );
    wDirectory.addModifyListener( new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        specificationMethod = ObjectLocationSpecificationMethod.REPOSITORY_BY_NAME;
        setRadioButtons();
      }
    } );

    wTransname = new TextVar( transMeta, gTransGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wTransname );
    wTransname.addModifyListener( lsMod );
    FormData fdTransName = new FormData();
    fdTransName.left = new FormAttachment( 0, 25 );
    fdTransName.right = new FormAttachment( wDirectory, -margin );
    fdTransName.top = new FormAttachment( wbTrans, 0, SWT.CENTER );
    wTransname.setLayoutData( fdTransName );
    wTransname.addModifyListener( new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        specificationMethod = ObjectLocationSpecificationMethod.REPOSITORY_BY_NAME;
        setRadioButtons();
      }
    } );

    // Radio button: The mapping is in the repository
    //
    radioByReference = new Button( gTransGroup, SWT.RADIO );
    props.setLook( radioByReference );
    radioByReference.setSelection( false );
    radioByReference.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.RadioRepByReference.Label" ) );
    radioByReference.setToolTipText( BaseMessages.getString(
      PKG, "SingleThreaderDialog.RadioRepByReference.Tooltip", Const.CR ) );
    FormData fdRadioByReference = new FormData();
    fdRadioByReference.left = new FormAttachment( 0, 0 );
    fdRadioByReference.right = new FormAttachment( 100, 0 );
    fdRadioByReference.top = new FormAttachment( wTransname, 2 * margin );
    radioByReference.setLayoutData( fdRadioByReference );
    radioByReference.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        specificationMethod = ObjectLocationSpecificationMethod.REPOSITORY_BY_REFERENCE;
        setRadioButtons();
      }
    } );

    wbByReference = new Button( gTransGroup, SWT.PUSH | SWT.CENTER );
    props.setLook( wbByReference );
    wbByReference.setImage( GUIResource.getInstance().getImageTransGraph() );
    wbByReference.setToolTipText( BaseMessages.getString( PKG, "SingleThreaderDialog.SelectTrans.Tooltip" ) );
    FormData fdbByReference = new FormData();
    fdbByReference.top = new FormAttachment( radioByReference, margin );
    fdbByReference.right = new FormAttachment( 100, 0 );
    wbByReference.setLayoutData( fdbByReference );
    wbByReference.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        selectTransformationByReference();
      }
    } );

    wByReference = new TextVar( transMeta, gTransGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER | SWT.READ_ONLY );
    props.setLook( wByReference );
    wByReference.addModifyListener( lsMod );
    FormData fdByReference = new FormData();
    fdByReference.top = new FormAttachment( radioByReference, margin );
    fdByReference.left = new FormAttachment( 0, 25 );
    fdByReference.right = new FormAttachment( wbByReference, -margin );
    wByReference.setLayoutData( fdByReference );
    wByReference.addModifyListener( new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        specificationMethod = ObjectLocationSpecificationMethod.REPOSITORY_BY_REFERENCE;
        setRadioButtons();
      }
    } );

    wNewTrans = new Button( gTransGroup, SWT.PUSH | SWT.CENTER ); // Browse
    props.setLook( wNewTrans );
    wNewTrans.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.New.Button" ) );
    FormData fdNewTrans = new FormData();
    fdNewTrans.left = new FormAttachment( 0, 0 );
    fdNewTrans.top = new FormAttachment( wByReference, 3 * margin );
    wNewTrans.setLayoutData( fdNewTrans );
    wNewTrans.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        newTransformation();
      }
    } );

    wEditTrans = new Button( gTransGroup, SWT.PUSH | SWT.CENTER ); // Browse
    props.setLook( wEditTrans );
    wEditTrans.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.Edit.Button" ) );
    wEditTrans.setToolTipText( BaseMessages.getString( PKG, "System.Tooltip.BrowseForFileOrDirAndAdd" ) );
    FormData fdEditTrans = new FormData();
    fdEditTrans.left = new FormAttachment( wNewTrans, 2 * margin );
    fdEditTrans.top = new FormAttachment( wByReference, 3 * margin );
    wEditTrans.setLayoutData( fdEditTrans );
    wEditTrans.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        editTrans();
      }
    } );

    FormData fdTransGroup = new FormData();
    fdTransGroup.left = new FormAttachment( 0, 0 );
    fdTransGroup.top = new FormAttachment( wStepname, 2 * margin );
    fdTransGroup.right = new FormAttachment( 100, 0 );
    // fdTransGroup.bottom = new FormAttachment(wStepname, 350);
    gTransGroup.setLayoutData( fdTransGroup );

    // Inject step
    //
    wGetInjectStep = new Button( shell, SWT.PUSH );
    wGetInjectStep.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.Button.Get" ) );
    FormData fdGetInjectStep = new FormData();
    fdGetInjectStep.top = new FormAttachment( gTransGroup, margin );
    fdGetInjectStep.right = new FormAttachment( 100, 0 );
    wGetInjectStep.setLayoutData( fdGetInjectStep );
    wGetInjectStep.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent arg0 ) {
        try {
          loadTransformation();
          String stepname = mappingTransMeta == null ? "" : Const.NVL( getInjectorStep( mappingTransMeta ), "" );
          wInjectStep.setText( stepname );
        } catch ( Exception e ) {
          new ErrorDialog( shell,
            BaseMessages.getString( PKG, "SingleThreaderDialog.ErrorLoadingTransformation.DialogTitle" ),
            BaseMessages.getString( PKG, "SingleThreaderDialog.ErrorLoadingTransformation.DialogMessage" ), e );
        }
      }
    } );

    wInjectStep = new LabelTextVar( transMeta, shell,
      BaseMessages.getString( PKG, "SingleThreaderDialog.InjectStep.Label" ),
      BaseMessages.getString( PKG, "SingleThreaderDialog.InjectStep.Tooltip" ) );
    wInjectStep.addModifyListener( lsMod );
    FormData fdInjectStep = new FormData();
    fdInjectStep.left = new FormAttachment( 0, 0 );
    fdInjectStep.top = new FormAttachment( gTransGroup, 2 * margin );
    fdInjectStep.right = new FormAttachment( wGetInjectStep, -margin );
    wInjectStep.setLayoutData( fdInjectStep );

    // Retrieve step...
    //
    wGetRetrieveStep = new Button( shell, SWT.PUSH );
    wGetRetrieveStep.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.Button.Get" ) );
    FormData fdGetRetrieveStep = new FormData();
    fdGetRetrieveStep.top = new FormAttachment( wInjectStep, 2 * margin );
    fdGetRetrieveStep.right = new FormAttachment( 100, 0 );
    wGetRetrieveStep.setLayoutData( fdGetRetrieveStep );
    wGetRetrieveStep.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent arg0 ) {
        try {
          loadTransformation();
          if ( mappingTransMeta != null ) {
            String[] stepNames = mappingTransMeta.getStepNames();
            EnterSelectionDialog d = new EnterSelectionDialog( shell, stepNames,
              BaseMessages.getString( PKG, "SingleThreaderDialog.SelectStep.Title" ),
              BaseMessages.getString( PKG, "SingleThreaderDialog.SelectStep.Message" ) );
            String step = d.open();
            if ( step != null ) {
              wRetrieveStep.setText( step );
            }
          }
        } catch ( Exception e ) {
          new ErrorDialog( shell,
            BaseMessages.getString( PKG, "SingleThreaderDialog.ErrorLoadingTransformation.DialogTitle" ),
            BaseMessages.getString( PKG, "SingleThreaderDialog.ErrorLoadingTransformation.DialogMessage" ), e );
        }
      }
    } );

    wRetrieveStep = new LabelTextVar( transMeta, shell,
      BaseMessages.getString( PKG, "SingleThreaderDialog.RetrieveStep.Label" ),
      BaseMessages.getString( PKG, "SingleThreaderDialog.RetrieveStep.Tooltip" ) );
    wRetrieveStep.addModifyListener( lsMod );
    FormData fdRetrieveStep = new FormData();
    fdRetrieveStep.left = new FormAttachment( 0, 0 );
    fdRetrieveStep.top = new FormAttachment( wInjectStep, margin );
    fdRetrieveStep.right = new FormAttachment( wGetRetrieveStep, -margin );
    wRetrieveStep.setLayoutData( fdRetrieveStep );

    // Here come the batch size, inject and retrieve fields...
    //
    wBatchSize =
      new LabelTextVar(
        transMeta, shell, BaseMessages.getString( PKG, "SingleThreaderDialog.BatchSize.Label" ), BaseMessages
          .getString( PKG, "SingleThreaderDialog.BatchSize.Tooltip" ) );
    FormData fdBatchSize = new FormData();
    fdBatchSize.left = new FormAttachment( 0, 0 );
    fdBatchSize.top = new FormAttachment( wRetrieveStep, margin );
    fdBatchSize.right = new FormAttachment( wGetRetrieveStep, -margin );
    wBatchSize.setLayoutData( fdBatchSize );

    wBatchTime =
      new LabelTextVar(
        transMeta, shell, BaseMessages.getString( PKG, "SingleThreaderDialog.BatchTime.Label" ), BaseMessages
          .getString( PKG, "SingleThreaderDialog.BatchTime.Tooltip" ) );
    wBatchTime.addModifyListener( lsMod );
    FormData fdBatchTime = new FormData();
    fdBatchTime.left = new FormAttachment( 0, 0 );
    fdBatchTime.top = new FormAttachment( wBatchSize, margin );
    fdBatchTime.right = new FormAttachment( wGetRetrieveStep, -margin );
    wBatchTime.setLayoutData( fdBatchTime );

    gParametersGroup = new Group( shell, SWT.SHADOW_ETCHED_IN );
    gParametersGroup.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.ParamGroup.Label" ) );
    gParametersGroup.setBackground( shell.getBackground() ); // the default looks ugly
    FormLayout paramGroupLayout = new FormLayout();
    paramGroupLayout.marginLeft = margin * 2;
    paramGroupLayout.marginTop = margin * 2;
    paramGroupLayout.marginRight = margin * 2;
    paramGroupLayout.marginBottom = margin * 2;
    gParametersGroup.setLayout( paramGroupLayout );

    // Pass all parameters down
    //
    Label wlPassParams = new Label( gParametersGroup, SWT.RIGHT );
    wlPassParams.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.PassAllParameters.Label" ) );
    props.setLook( wlPassParams );
    FormData fdlPassParams = new FormData();
    fdlPassParams.left = new FormAttachment( 0, 0 );
    fdlPassParams.top = new FormAttachment( 0, 0 );
    fdlPassParams.right = new FormAttachment( middle, -margin );
    wlPassParams.setLayoutData( fdlPassParams );
    wPassParams = new Button( gParametersGroup, SWT.CHECK );
    props.setLook( wPassParams );
    FormData fdPassParams = new FormData();
    fdPassParams.left = new FormAttachment( middle, 0 );
    fdPassParams.top = new FormAttachment( 0, 0 );
    fdPassParams.right = new FormAttachment( 100, 0 );
    wPassParams.setLayoutData( fdPassParams );
    wPassParams.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent arg0 ) {
        changed = true;
      }
    } );

    wbGetParams = new Button( gParametersGroup, SWT.PUSH );
    wbGetParams.setText( BaseMessages.getString( PKG, "SingleThreaderDialog.GetParameters.Button.Label" ) );
    FormData fdGetParams = new FormData();
    fdGetParams.top = new FormAttachment( wPassParams, margin );
    fdGetParams.right = new FormAttachment( 100, 0 );
    wbGetParams.setLayoutData( fdGetParams );
    wbGetParams.addSelectionListener( new SelectionAdapter() {
      @Override
      public void widgetSelected( SelectionEvent arg0 ) {
        getParameters( null ); // null: force reload of file from specification
      }
    } );

    final int parameterRows =
      singleThreaderMeta.getParameters() != null ? singleThreaderMeta.getParameters().length : 0;

    ColumnInfo[] colinf =
      new ColumnInfo[] {
        new ColumnInfo(
          BaseMessages.getString( PKG, "SingleThreaderDialog.Parameters.Parameter.Label" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ),
        new ColumnInfo(
          BaseMessages.getString( PKG, "SingleThreaderDialog.Parameters.Value.Label" ),
          ColumnInfo.COLUMN_TYPE_TEXT, false ), };
    colinf[1].setUsingVariables( true );

    wParameters =
      new TableView(
        transMeta, gParametersGroup, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI, colinf, parameterRows,
        lsMod, props );

    FormData fdParameters = new FormData();
    fdParameters.left = new FormAttachment( 0, 0 );
    fdParameters.top = new FormAttachment( wPassParams, margin );
    fdParameters.right = new FormAttachment( wbGetParams, -margin );
    fdParameters.bottom = new FormAttachment( 100, 0 );
    wParameters.setLayoutData( fdParameters );

    FormData fdParametersComp = new FormData();
    fdParametersComp.left = new FormAttachment( 0, 0 );
    fdParametersComp.top = new FormAttachment( wBatchTime, 0 );
    fdParametersComp.right = new FormAttachment( 100, 0 );
    fdParametersComp.bottom = new FormAttachment( 100, -50 );
    gParametersGroup.setLayoutData( fdParametersComp );

    // Some buttons
    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );

    setButtonPositions( new Button[] { wOK, wCancel }, margin, gParametersGroup );

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

    wCancel.addListener( SWT.Selection, lsCancel );
    wOK.addListener( SWT.Selection, lsOK );

    lsDef = new SelectionAdapter() {
      public void widgetDefaultSelected( SelectionEvent e ) {
        ok();
      }
    };

    wStepname.addSelectionListener( lsDef );
    wFilename.addSelectionListener( lsDef );
    wTransname.addSelectionListener( lsDef );
    wBatchSize.addSelectionListener( lsDef );
    wBatchTime.addSelectionListener( lsDef );
    wInjectStep.addSelectionListener( lsDef );

    // Detect X or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );

    // Set the shell size, based upon previous time...
    setSize();

    getData();
    singleThreaderMeta.setChanged( changed );

    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
    return stepname;
  }

  protected void selectTransformationByReference() {
    if ( repository != null ) {
      SelectObjectDialog sod = new SelectObjectDialog( shell, repository, true, false );
      sod.open();
      RepositoryElementMetaInterface repositoryObject = sod.getRepositoryObject();
      if ( repositoryObject != null ) {
        specificationMethod = ObjectLocationSpecificationMethod.REPOSITORY_BY_REFERENCE;
        getByReferenceData( repositoryObject );
        referenceObjectId = repositoryObject.getObjectId();
        setRadioButtons();
      }
    }
  }

  private void selectRepositoryTrans() {
    try {
      SelectObjectDialog sod = new SelectObjectDialog( shell, repository );
      String transName = sod.open();
      RepositoryDirectoryInterface repdir = sod.getDirectory();
      if ( transName != null && repdir != null ) {
        loadRepositoryTrans( transName, repdir );
        wTransname.setText( mappingTransMeta.getName() );
        wDirectory.setText( mappingTransMeta.getRepositoryDirectory().getPath() );
        wFilename.setText( "" );
        radioByName.setSelection( true );
        radioFilename.setSelection( false );
        specificationMethod = ObjectLocationSpecificationMethod.REPOSITORY_BY_NAME;
        setRadioButtons();
      }
    } catch ( KettleException ke ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "SingleThreaderDialog.ErrorSelectingObject.DialogTitle" ),
        BaseMessages.getString( PKG, "SingleThreaderDialog.ErrorSelectingObject.DialogMessage" ), ke );
    }
  }

  private void loadRepositoryTrans( String transName, RepositoryDirectoryInterface repdir ) throws KettleException {
    // Read the transformation...
    //
    mappingTransMeta = repository.loadTransformation(
      transMeta.environmentSubstitute( transName ), repdir, null, true, null );
    mappingTransMeta.clearChanged();
  }

  private void selectFileTrans() {
    String curFile = wFilename.getText();
    FileObject root = null;

    try {
      root = KettleVFS.getFileObject( curFile != null ? curFile : Const.getUserHomeDirectory() );

      VfsFileChooserDialog vfsFileChooser = Spoon.getInstance().getVfsFileChooserDialog( root.getParent(), root );
      FileObject file =
        vfsFileChooser.open(
          shell, null, Const.STRING_TRANS_FILTER_EXT, Const.getTransformationFilterNames(),
          VfsFileChooserDialog.VFS_DIALOG_OPEN_FILE );
      if ( file == null ) {
        return;
      }
      String fname = null;

      fname = file.getURL().getFile();

      if ( fname != null ) {

        loadFileTrans( fname );
        wFilename.setText( mappingTransMeta.getFilename() );
        wTransname.setText( Const.NVL( mappingTransMeta.getName(), "" ) );
        wDirectory.setText( "" );
        specificationMethod = ObjectLocationSpecificationMethod.FILENAME;
        setRadioButtons();
      }
    } catch ( IOException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "SingleThreaderDialog.ErrorLoadingTransformation.DialogTitle" ),
        BaseMessages.getString( PKG, "SingleThreaderDialog.ErrorLoadingTransformation.DialogMessage" ), e );
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "SingleThreaderDialog.ErrorLoadingTransformation.DialogTitle" ),
        BaseMessages.getString( PKG, "SingleThreaderDialog.ErrorLoadingTransformation.DialogMessage" ), e );
    }
  }

  private void loadFileTrans( String fname ) throws KettleException {
    mappingTransMeta = new TransMeta( transMeta.environmentSubstitute( fname ) );
    mappingTransMeta.clearChanged();
  }

  private void editTrans() {
    // Load the transformation again to make sure it's still there and
    // refreshed
    // It's an extra check to make sure it's still OK...
    //
    try {
      loadTransformation();

      // If we're still here, mappingTransMeta is valid.
      //
      SpoonInterface spoon = SpoonFactory.getInstance();
      if ( spoon != null ) {
        spoon.addTransGraph( mappingTransMeta );
      }
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "SingleThreaderDialog.ErrorShowingTransformation.Title" ),
        BaseMessages.getString( PKG, "SingleThreaderDialog.ErrorShowingTransformation.Message" ), e );
    }
  }

  private void loadTransformation() throws KettleException {
    switch ( specificationMethod ) {
      case FILENAME:
        loadFileTrans( wFilename.getText() );
        break;
      case REPOSITORY_BY_NAME:
        String realDirectory = transMeta.environmentSubstitute( wDirectory.getText() );
        String realTransname = transMeta.environmentSubstitute( wTransname.getText() );

        if ( Const.isEmpty( realDirectory ) || Const.isEmpty( realTransname ) ) {
          throw new KettleException(
            BaseMessages.getString( PKG, "SingleThreaderDialog.Exception.NoValidMappingDetailsFound" ) );
        }
        RepositoryDirectoryInterface repdir = repository.findDirectory( realDirectory );
        if ( repdir == null ) {
          throw new KettleException( BaseMessages.getString(
            PKG, "SingleThreaderDialog.Exception.UnableToFindRepositoryDirectory)" ) );
        }
        loadRepositoryTrans( realTransname, repdir );
        break;
      case REPOSITORY_BY_REFERENCE:
        mappingTransMeta = repository.loadTransformation( referenceObjectId, null ); // load the last version
        mappingTransMeta.clearChanged();
        break;
      default:
        break;
    }

    wInjectStep.setText( getInjectorStep( mappingTransMeta ) );
  }

  public void setActive() {
    radioByName.setEnabled( repository != null );
    radioByReference.setEnabled( repository != null );
    wFilename.setEnabled( radioFilename.getSelection() );
    wbbFilename.setEnabled( radioFilename.getSelection() );
    wTransname.setEnabled( repository != null && radioByName.getSelection() );

    wDirectory.setEnabled( repository != null && radioByName.getSelection() );

    wbTrans.setEnabled( repository != null && radioByName.getSelection() );

    wByReference.setEnabled( repository != null && radioByReference.getSelection() );
    wbByReference.setEnabled( repository != null && radioByReference.getSelection() );
  }

  protected void setRadioButtons() {
    radioFilename.setSelection( specificationMethod == ObjectLocationSpecificationMethod.FILENAME );
    radioByName.setSelection( specificationMethod == ObjectLocationSpecificationMethod.REPOSITORY_BY_NAME );
    radioByReference
      .setSelection( specificationMethod == ObjectLocationSpecificationMethod.REPOSITORY_BY_REFERENCE );
    setActive();
  }

  private void getByReferenceData( RepositoryElementMetaInterface transInf ) {
    String path = transInf.getRepositoryDirectory().getPath();
    if ( !path.endsWith( "/" ) ) {
      path += "/";
    }
    path += transInf.getName();
    wByReference.setText( path );
  }

  /**
   * Copy information from the meta-data input to the dialog fields.
   */
  public void getData() {
    specificationMethod = singleThreaderMeta.getSpecificationMethod();
    switch ( specificationMethod ) {
      case FILENAME:
        wFilename.setText( Const.NVL( singleThreaderMeta.getFileName(), "" ) );
        break;
      case REPOSITORY_BY_NAME:
        wDirectory.setText( Const.NVL( singleThreaderMeta.getDirectoryPath(), "" ) );
        wTransname.setText( Const.NVL( singleThreaderMeta.getTransName(), "" ) );
        break;
      case REPOSITORY_BY_REFERENCE:
        referenceObjectId = singleThreaderMeta.getTransObjectId();
        wByReference.setText( "" );
        getByReferenceData( referenceObjectId );
        break;
      default:
        break;
    }
    setRadioButtons();

    wBatchSize.setText( Const.NVL( singleThreaderMeta.getBatchSize(), "" ) );
    wBatchTime.setText( Const.NVL( singleThreaderMeta.getBatchTime(), "" ) );
    wInjectStep.setText( Const.NVL( singleThreaderMeta.getInjectStep(), "" ) );
    wRetrieveStep.setText( Const.NVL( singleThreaderMeta.getRetrieveStep(), "" ) );

    // Parameters
    //
    if ( singleThreaderMeta.getParameters() != null ) {
      for ( int i = 0; i < singleThreaderMeta.getParameters().length; i++ ) {
        TableItem ti = wParameters.table.getItem( i );
        if ( !Const.isEmpty( singleThreaderMeta.getParameters()[i] ) ) {
          ti.setText( 1, Const.NVL( singleThreaderMeta.getParameters()[i], "" ) );
          ti.setText( 2, Const.NVL( singleThreaderMeta.getParameterValues()[i], "" ) );
        }
      }
      wParameters.removeEmptyRows();
      wParameters.setRowNums();
      wParameters.optWidth( true );
    }

    wPassParams.setSelection( singleThreaderMeta.isPassingAllParameters() );

    try {
      loadTransformation();
    } catch ( Throwable t ) {
      // Skip the error, it becomes annoying otherwise
    }

    wStepname.selectAll();
    wStepname.setFocus();
  }

  private void getByReferenceData( ObjectId referenceObjectId ) {
    try {
      if ( repository == null ) {
        throw new KettleException(
          BaseMessages.getString( PKG, "SingleThreaderDialog.Exception.NotConnectedToRepository.Message" ) );
      }
      RepositoryObject transInf = repository.getObjectInformation(
        referenceObjectId,
        RepositoryObjectType.TRANSFORMATION );
      if ( transInf != null ) {
        getByReferenceData( transInf );
      }
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "SingleThreaderDialog.Exception.UnableToReferenceObjectId.Title" ),
        BaseMessages.getString( PKG, "SingleThreaderDialog.Exception.UnableToReferenceObjectId.Message" ), e );
    }
  }

  public static String getInjectorStep( TransMeta mappingTransMeta ) {
    for ( StepMeta stepMeta : mappingTransMeta.getSteps() ) {
      if ( stepMeta.getStepID().equals( "Injector" ) || stepMeta.getStepID().equals( "MappingInput" ) ) {
        return stepMeta.getName();
      }
    }
    return "";
  }

  private void cancel() {
    stepname = null;
    singleThreaderMeta.setChanged( changed );
    dispose();
  }

  private void getInfo( SingleThreaderMeta meta ) throws KettleException {

    loadTransformation();

    meta.setSpecificationMethod( specificationMethod );
    switch ( specificationMethod ) {
      case FILENAME:
        meta.setFileName( wFilename.getText() );
        meta.setDirectoryPath( null );
        meta.setTransName( null );
        meta.setTransObjectId( null );
        break;
      case REPOSITORY_BY_NAME:
        meta.setDirectoryPath( wDirectory.getText() );
        meta.setTransName( wTransname.getText() );
        meta.setFileName( null );
        meta.setTransObjectId( null );
        break;
      case REPOSITORY_BY_REFERENCE:
        meta.setFileName( null );
        meta.setDirectoryPath( null );
        meta.setTransName( null );
        meta.setTransObjectId( referenceObjectId );
        break;
      default:
        break;
    }

    meta.setBatchSize( wBatchSize.getText() );
    meta.setBatchTime( wBatchTime.getText() );
    meta.setInjectStep( wInjectStep.getText() );
    meta.setRetrieveStep( wRetrieveStep.getText() );

    // The parameters...
    //
    int nritems = wParameters.nrNonEmpty();
    int nr = 0;
    for ( int i = 0; i < nritems; i++ ) {
      String param = wParameters.getNonEmpty( i ).getText( 1 );
      if ( !Const.isEmpty( param ) ) {
        nr++;
      }
    }
    meta.setParameters( new String[nr] );
    meta.setParameterValues( new String[nr] );
    nr = 0;
    //CHECKSTYLE:Indentation:OFF
    for ( int i = 0; i < nritems; i++ ) {
      String param = wParameters.getNonEmpty( i ).getText( 1 );
      String value = wParameters.getNonEmpty( i ).getText( 2 );

      meta.getParameters()[nr] = param;
      meta.getParameterValues()[nr] = Const.NVL( value, "" );

      nr++;
    }

    meta.setPassingAllParameters( wPassParams.getSelection() );
  }

  private void ok() {
    if ( Const.isEmpty( wStepname.getText() ) ) {
      return;
    }

    stepname = wStepname.getText(); // return value

    try {
      getInfo( singleThreaderMeta );
      loadTransformation();
    } catch ( KettleException e ) {
      new ErrorDialog( shell, BaseMessages.getString(
        PKG, "SingleThreaderDialog.ErrorLoadingSpecifiedTransformation.Title" ), BaseMessages.getString(
        PKG, "SingleThreaderDialog.ErrorLoadingSpecifiedTransformation.Message" ), e );
    }

    dispose();
  }

  protected void getParameters( TransMeta mappingTransMeta ) {
    try {
      if ( mappingTransMeta == null ) {
        SingleThreaderMeta jet = new SingleThreaderMeta();
        getInfo( jet );
        mappingTransMeta = SingleThreaderMeta.loadSingleThreadedTransMeta( jet, repository, transMeta );
      }
      String[] parameters = mappingTransMeta.listParameters();

      String[] existing = wParameters.getItems( 1 );

      for ( int i = 0; i < parameters.length; i++ ) {
        if ( Const.indexOfString( parameters[i], existing ) < 0 ) {
          TableItem item = new TableItem( wParameters.table, SWT.NONE );
          item.setText( 1, parameters[i] );
        }
      }
      wParameters.removeEmptyRows();
      wParameters.setRowNums();
      wParameters.optWidth( true );
    } catch ( Exception e ) {
      new ErrorDialog( shell, BaseMessages.getString(
        PKG, "SingleThreaderDialog.Exception.UnableToLoadTransformation.Title" ), BaseMessages.getString(
        PKG, "SingleThreaderDialog.Exception.UnableToLoadTransformation.Message" ), e );
    }

  }

  /**
   * Ask the user to fill in the details...
   */
  protected void newTransformation() {

    // Get input fields for this step so we can put this metadata in the mapping
    //
    RowMetaInterface inFields = new RowMeta();
    try {
      inFields = transMeta.getPrevStepFields( stepname );
    } catch ( Exception e ) {
      // Just show the error but continue operations.
      //
      new ErrorDialog( shell, "Error", "Unable to get input fields from previous step", e );
    }

    TransMeta newTransMeta = new TransMeta();

    newTransMeta.getDatabases().addAll( transMeta.getDatabases() );
    newTransMeta.setRepository( transMeta.getRepository() );
    newTransMeta.setRepositoryDirectory( transMeta.getRepositoryDirectory() );

    // Pass some interesting settings from the parent transformations...
    //
    newTransMeta.setUsingUniqueConnections( transMeta.isUsingUniqueConnections() );

    // Add MappingInput and MappingOutput steps
    //
    String INPUTSTEP_NAME = "Mapping Input";
    MappingInputMeta inputMeta = new MappingInputMeta();
    inputMeta.allocate( inFields.size() );
    for ( int i = 0; i < inFields.size(); i++ ) {
      ValueMetaInterface valueMeta = inFields.getValueMeta( i );
      inputMeta.getFieldName()[i] = valueMeta.getName();
      inputMeta.getFieldType()[i] = valueMeta.getType();
      inputMeta.getFieldLength()[i] = valueMeta.getLength();
      inputMeta.getFieldPrecision()[i] = valueMeta.getPrecision();
    }
    StepMeta inputStep = new StepMeta( INPUTSTEP_NAME, inputMeta );
    inputStep.setLocation( 50, 50 );
    inputStep.setDraw( true );
    newTransMeta.addStep( inputStep );

    String OUTPUTSTEP_NAME = "Mapping Output";
    MappingOutputMeta outputMeta = new MappingOutputMeta();
    outputMeta.allocate( 0 );
    StepMeta outputStep = new StepMeta( OUTPUTSTEP_NAME, outputMeta );
    outputStep.setLocation( 500, 50 );
    outputStep.setDraw( true );
    newTransMeta.addStep( outputStep );
    newTransMeta.addTransHop( new TransHopMeta( inputStep, outputStep ) );

    TransDialog transDialog = new TransDialog( shell, SWT.NONE, newTransMeta, repository );
    if ( transDialog.open() != null ) {
      Spoon spoon = Spoon.getInstance();
      spoon.addTransGraph( newTransMeta );
      boolean saved = false;
      try {
        if ( repository != null ) {
          if ( !Const.isEmpty( newTransMeta.getName() ) ) {
            wStepname.setText( newTransMeta.getName() );
          }
          saved = spoon.saveToRepository( newTransMeta, false );
          if ( repository.getRepositoryMeta().getRepositoryCapabilities().supportsReferences() ) {
            specificationMethod = ObjectLocationSpecificationMethod.REPOSITORY_BY_REFERENCE;
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
        switch ( specificationMethod ) {
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

        // Connect mapping input/output
        //
        wInjectStep.setText( INPUTSTEP_NAME );
        wRetrieveStep.setText( OUTPUTSTEP_NAME );

      }
    }
  }

}
