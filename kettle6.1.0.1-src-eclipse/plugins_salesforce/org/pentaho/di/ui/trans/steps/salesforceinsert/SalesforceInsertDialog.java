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

package org.pentaho.di.ui.trans.steps.salesforceinsert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sforce.soap.partner.Field;
import com.sforce.soap.partner.FieldType;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.Props;
import org.pentaho.di.core.SourceToTargetMapping;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.salesforceinput.SalesforceConnection;
import org.pentaho.di.trans.steps.salesforceinput.SalesforceConnectionUtils;
import org.pentaho.di.trans.steps.salesforceinsert.SalesforceInsertMeta;
import org.pentaho.di.ui.core.dialog.EnterMappingDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.core.widget.LabelTextVar;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.core.widget.TextVar;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.ui.trans.step.TableItemInsertListener;

public class SalesforceInsertDialog extends BaseStepDialog implements StepDialogInterface {

  private static Class<?> PKG = SalesforceInsertMeta.class; // for i18n purposes, needed by Translator2!!

  private CTabFolder wTabFolder;
  private FormData fdTabFolder;

  private CTabItem wGeneralTab;

  private Composite wGeneralComp;

  private FormData fdGeneralComp;

  private FormData fdlModule, fdModule;

  private FormData fdlBatchSize, fdBatchSize;

  private FormData fdUserName, fdURL, fdPassword;

  private Label wlModule, wlBatchSize;

  private Map<String, Integer> inputFields;

  private ColumnInfo[] ciReturn;

  private Button wDoMapping;
  private FormData fdDoMapping;

  private Label wlReturn;
  private TableView wReturn;
  private FormData fdlReturn, fdReturn;

  private Button wGetLU;
  private FormData fdGetLU;
  private Listener lsGetLU;

  private SalesforceInsertMeta input;

  private LabelTextVar wUserName, wURL, wPassword;

  private TextVar wBatchSize;

  private ComboVar wModule;

  private Button wTest;

  private FormData fdTest;
  private Listener lsTest;

  private Group wConnectionGroup;
  private FormData fdConnectionGroup;

  private Group wSettingsGroup, wOutFieldsGroup;
  private FormData fdSettingsGroup, fdOutFieldsGroup;

  private Label wlSalesforceIDFieldName;
  private FormData fdlSalesforceIDFieldName;

  private TextVar wSalesforceIDFieldName;
  private FormData fdSalesforceIDFieldName;

  private Label wlUseCompression;
  private FormData fdlUseCompression;
  private Button wUseCompression;
  private FormData fdUseCompression;

  private Label wlTimeOut;
  private FormData fdlTimeOut;
  private TextVar wTimeOut;
  private FormData fdTimeOut;

  /**
   * List of ColumnInfo that should have the field names of the selected database table
   */
  private List<ColumnInfo> tableFieldColumns = new ArrayList<ColumnInfo>();
  private boolean gotModule = false;
  private boolean gotFields = false;

  private Label wlRollbackAllChangesOnError;
  private FormData fdlRollbackAllChangesOnError;
  private Button wRollbackAllChangesOnError;
  private FormData fdRollbackAllChangesOnError;

  private boolean getModulesListError = false; /* True if error getting modules list */

  public SalesforceInsertDialog( Shell parent, Object in, TransMeta transMeta, String sname ) {
    super( parent, (BaseStepMeta) in, transMeta, sname );
    input = (SalesforceInsertMeta) in;
    inputFields = new HashMap<String, Integer>();
  }

  public String open() {
    Shell parent = getParent();
    Display display = parent.getDisplay();

    shell = new Shell( parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
    props.setLook( shell );
    setShellImage( shell, input );

    ModifyListener lsMod = new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        input.setChanged();
      }
    };
    ModifyListener lsTableMod = new ModifyListener() {
      public void modifyText( ModifyEvent arg0 ) {
        input.setChanged();
        setModuleFieldCombo();
      }
    };
    SelectionAdapter lsSelection = new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        input.setChanged();
        setModuleFieldCombo();
      }
    };
    changed = input.hasChanged();

    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );
    shell.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.DialogTitle" ) );

    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

    // Stepname line
    wlStepname = new Label( shell, SWT.RIGHT );
    wlStepname.setText( BaseMessages.getString( PKG, "System.Label.StepName" ) );
    props.setLook( wlStepname );
    fdlStepname = new FormData();
    fdlStepname.left = new FormAttachment( 0, 0 );
    fdlStepname.top = new FormAttachment( 0, margin );
    fdlStepname.right = new FormAttachment( middle, -margin );
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

    wTabFolder = new CTabFolder( shell, SWT.BORDER );
    props.setLook( wTabFolder, Props.WIDGET_STYLE_TAB );

    // ////////////////////////
    // START OF FILE TAB ///
    // ////////////////////////
    wGeneralTab = new CTabItem( wTabFolder, SWT.NONE );
    wGeneralTab.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.General.Tab" ) );

    wGeneralComp = new Composite( wTabFolder, SWT.NONE );
    props.setLook( wGeneralComp );

    FormLayout generalLayout = new FormLayout();
    generalLayout.marginWidth = 3;
    generalLayout.marginHeight = 3;
    wGeneralComp.setLayout( generalLayout );

    // ///////////////////////////////
    // START OF Connection GROUP //
    // ///////////////////////////////

    wConnectionGroup = new Group( wGeneralComp, SWT.SHADOW_NONE );
    props.setLook( wConnectionGroup );
    wConnectionGroup.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.ConnectionGroup.Label" ) );

    FormLayout connectionGroupLayout = new FormLayout();
    connectionGroupLayout.marginWidth = 10;
    connectionGroupLayout.marginHeight = 10;
    wConnectionGroup.setLayout( connectionGroupLayout );

    // Webservice URL
    wURL = new LabelTextVar( transMeta, wConnectionGroup,
      BaseMessages.getString( PKG, "SalesforceInsertDialog.URL.Label" ),
      BaseMessages.getString( PKG, "SalesforceInsertDialog.URL.Tooltip" ) );
    props.setLook( wURL );
    wURL.addModifyListener( lsMod );
    fdURL = new FormData();
    fdURL.left = new FormAttachment( 0, 0 );
    fdURL.top = new FormAttachment( wStepname, margin );
    fdURL.right = new FormAttachment( 100, 0 );
    wURL.setLayoutData( fdURL );

    // UserName line
    wUserName = new LabelTextVar( transMeta, wConnectionGroup,
      BaseMessages.getString( PKG, "SalesforceInsertDialog.User.Label" ),
      BaseMessages.getString( PKG, "SalesforceInsertDialog.User.Tooltip" ) );
    props.setLook( wUserName );
    wUserName.addModifyListener( lsMod );
    fdUserName = new FormData();
    fdUserName.left = new FormAttachment( 0, 0 );
    fdUserName.top = new FormAttachment( wURL, margin );
    fdUserName.right = new FormAttachment( 100, 0 );
    wUserName.setLayoutData( fdUserName );

    // Password line
    wPassword = new LabelTextVar( transMeta, wConnectionGroup,
      BaseMessages.getString( PKG, "SalesforceInsertDialog.Password.Label" ),
      BaseMessages.getString( PKG, "SalesforceInsertDialog.Password.Tooltip" ), true );
    props.setLook( wPassword );
    wPassword.addModifyListener( lsMod );
    fdPassword = new FormData();
    fdPassword.left = new FormAttachment( 0, 0 );
    fdPassword.top = new FormAttachment( wUserName, margin );
    fdPassword.right = new FormAttachment( 100, 0 );
    wPassword.setLayoutData( fdPassword );

    // Test Salesforce connection button
    wTest = new Button( wConnectionGroup, SWT.PUSH );
    wTest.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.TestConnection.Label" ) );
    props.setLook( wTest );
    fdTest = new FormData();
    wTest.setToolTipText( BaseMessages.getString( PKG, "SalesforceInsertDialog.TestConnection.Tooltip" ) );
    // fdTest.left = new FormAttachment(middle, 0);
    fdTest.top = new FormAttachment( wPassword, margin );
    fdTest.right = new FormAttachment( 100, 0 );
    wTest.setLayoutData( fdTest );

    fdConnectionGroup = new FormData();
    fdConnectionGroup.left = new FormAttachment( 0, margin );
    fdConnectionGroup.top = new FormAttachment( wStepname, margin );
    fdConnectionGroup.right = new FormAttachment( 100, -margin );
    wConnectionGroup.setLayoutData( fdConnectionGroup );

    // ///////////////////////////////
    // END OF Connection GROUP //
    // ///////////////////////////////

    // ///////////////////////////////
    // START OF Settings GROUP //
    // ///////////////////////////////

    wSettingsGroup = new Group( wGeneralComp, SWT.SHADOW_NONE );
    props.setLook( wSettingsGroup );
    wSettingsGroup.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.SettingsGroup.Label" ) );

    FormLayout settingGroupLayout = new FormLayout();
    settingGroupLayout.marginWidth = 10;
    settingGroupLayout.marginHeight = 10;
    wSettingsGroup.setLayout( settingGroupLayout );

    // Timeout
    wlTimeOut = new Label( wSettingsGroup, SWT.RIGHT );
    wlTimeOut.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.TimeOut.Label" ) );
    props.setLook( wlTimeOut );
    fdlTimeOut = new FormData();
    fdlTimeOut.left = new FormAttachment( 0, 0 );
    fdlTimeOut.top = new FormAttachment( wSettingsGroup, margin );
    fdlTimeOut.right = new FormAttachment( middle, -margin );
    wlTimeOut.setLayoutData( fdlTimeOut );
    wTimeOut = new TextVar( transMeta, wSettingsGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wTimeOut );
    wTimeOut.addModifyListener( lsMod );
    fdTimeOut = new FormData();
    fdTimeOut.left = new FormAttachment( middle, 0 );
    fdTimeOut.top = new FormAttachment( wSettingsGroup, margin );
    fdTimeOut.right = new FormAttachment( 100, 0 );
    wTimeOut.setLayoutData( fdTimeOut );

    // Use compression?
    wlUseCompression = new Label( wSettingsGroup, SWT.RIGHT );
    wlUseCompression.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.UseCompression.Label" ) );
    props.setLook( wlUseCompression );
    fdlUseCompression = new FormData();
    fdlUseCompression.left = new FormAttachment( 0, 0 );
    fdlUseCompression.top = new FormAttachment( wTimeOut, margin );
    fdlUseCompression.right = new FormAttachment( middle, -margin );
    wlUseCompression.setLayoutData( fdlUseCompression );
    wUseCompression = new Button( wSettingsGroup, SWT.CHECK );
    props.setLook( wUseCompression );
    wUseCompression
      .setToolTipText( BaseMessages.getString( PKG, "SalesforceInsertDialog.UseCompression.Tooltip" ) );
    fdUseCompression = new FormData();
    fdUseCompression.left = new FormAttachment( middle, 0 );
    fdUseCompression.top = new FormAttachment( wTimeOut, margin );
    wUseCompression.setLayoutData( fdUseCompression );

    // Rollback all changes on error?
    wlRollbackAllChangesOnError = new Label( wSettingsGroup, SWT.RIGHT );
    wlRollbackAllChangesOnError.setText( BaseMessages.getString(
      PKG, "SalesforceInsertDialog.RollbackAllChangesOnError.Label" ) );
    props.setLook( wlRollbackAllChangesOnError );
    fdlRollbackAllChangesOnError = new FormData();
    fdlRollbackAllChangesOnError.left = new FormAttachment( 0, 0 );
    fdlRollbackAllChangesOnError.top = new FormAttachment( wUseCompression, margin );
    fdlRollbackAllChangesOnError.right = new FormAttachment( middle, -margin );
    wlRollbackAllChangesOnError.setLayoutData( fdlRollbackAllChangesOnError );
    wRollbackAllChangesOnError = new Button( wSettingsGroup, SWT.CHECK );
    props.setLook( wRollbackAllChangesOnError );
    wRollbackAllChangesOnError.setToolTipText( BaseMessages.getString(
      PKG, "SalesforceInsertDialog.RollbackAllChangesOnError.Tooltip" ) );
    fdRollbackAllChangesOnError = new FormData();
    fdRollbackAllChangesOnError.left = new FormAttachment( middle, 0 );
    fdRollbackAllChangesOnError.top = new FormAttachment( wUseCompression, margin );
    wRollbackAllChangesOnError.setLayoutData( fdRollbackAllChangesOnError );

    // BatchSize value
    wlBatchSize = new Label( wSettingsGroup, SWT.RIGHT );
    wlBatchSize.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.Limit.Label" ) );
    props.setLook( wlBatchSize );
    fdlBatchSize = new FormData();
    fdlBatchSize.left = new FormAttachment( 0, 0 );
    fdlBatchSize.top = new FormAttachment( wRollbackAllChangesOnError, margin );
    fdlBatchSize.right = new FormAttachment( middle, -margin );
    wlBatchSize.setLayoutData( fdlBatchSize );
    wBatchSize = new TextVar( transMeta, wSettingsGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wBatchSize );
    wBatchSize.addModifyListener( lsMod );
    fdBatchSize = new FormData();
    fdBatchSize.left = new FormAttachment( middle, 0 );
    fdBatchSize.top = new FormAttachment( wRollbackAllChangesOnError, margin );
    fdBatchSize.right = new FormAttachment( 100, 0 );
    wBatchSize.setLayoutData( fdBatchSize );

    // Module
    wlModule = new Label( wSettingsGroup, SWT.RIGHT );
    wlModule.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.Module.Label" ) );
    props.setLook( wlModule );
    fdlModule = new FormData();
    fdlModule.left = new FormAttachment( 0, 0 );
    fdlModule.top = new FormAttachment( wBatchSize, margin );
    fdlModule.right = new FormAttachment( middle, -margin );
    wlModule.setLayoutData( fdlModule );
    wModule = new ComboVar( transMeta, wSettingsGroup, SWT.SINGLE | SWT.READ_ONLY | SWT.BORDER );
    wModule.setEditable( true );
    props.setLook( wModule );
    wModule.addModifyListener( lsTableMod );
    wModule.addSelectionListener( lsSelection );
    fdModule = new FormData();
    fdModule.left = new FormAttachment( middle, 0 );
    fdModule.top = new FormAttachment( wBatchSize, margin );
    fdModule.right = new FormAttachment( 100, -margin );
    wModule.setLayoutData( fdModule );
    wModule.addFocusListener( new FocusListener() {
      public void focusLost( org.eclipse.swt.events.FocusEvent e ) {
        getModulesListError = false;
      }

      public void focusGained( org.eclipse.swt.events.FocusEvent e ) {
        // check if the URL and login credentials passed and not just had error
        if ( Const.isEmpty( wURL.getText() )
          || Const.isEmpty( wUserName.getText() ) || Const.isEmpty( wPassword.getText() )
          || ( getModulesListError ) ) {
          return;
        }

        Cursor busy = new Cursor( shell.getDisplay(), SWT.CURSOR_WAIT );
        shell.setCursor( busy );
        getModulesList();
        shell.setCursor( null );
        busy.dispose();
      }
    } );

    fdSettingsGroup = new FormData();
    fdSettingsGroup.left = new FormAttachment( 0, margin );
    fdSettingsGroup.top = new FormAttachment( wConnectionGroup, margin );
    fdSettingsGroup.right = new FormAttachment( 100, -margin );
    wSettingsGroup.setLayoutData( fdSettingsGroup );

    // ///////////////////////////////
    // END OF Settings GROUP //
    // ///////////////////////////////

    // ///////////////////////////////
    // START OF OutFields GROUP //
    // ///////////////////////////////

    wOutFieldsGroup = new Group( wGeneralComp, SWT.SHADOW_NONE );
    props.setLook( wOutFieldsGroup );
    wOutFieldsGroup.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.OutFieldsGroup.Label" ) );

    FormLayout OutFieldsGroupLayout = new FormLayout();
    OutFieldsGroupLayout.marginWidth = 10;
    OutFieldsGroupLayout.marginHeight = 10;
    wOutFieldsGroup.setLayout( OutFieldsGroupLayout );

    // SalesforceIDFieldName
    wlSalesforceIDFieldName = new Label( wOutFieldsGroup, SWT.RIGHT );
    wlSalesforceIDFieldName.setText( BaseMessages.getString(
      PKG, "SalesforceInsertDialog.SalesforceIDFieldName.Label" ) );
    props.setLook( wlSalesforceIDFieldName );
    fdlSalesforceIDFieldName = new FormData();
    fdlSalesforceIDFieldName.left = new FormAttachment( 0, 0 );
    fdlSalesforceIDFieldName.top = new FormAttachment( wSettingsGroup, margin );
    fdlSalesforceIDFieldName.right = new FormAttachment( middle, -margin );
    wlSalesforceIDFieldName.setLayoutData( fdlSalesforceIDFieldName );
    wSalesforceIDFieldName = new TextVar( transMeta, wOutFieldsGroup, SWT.SINGLE | SWT.LEFT | SWT.BORDER );
    props.setLook( wSalesforceIDFieldName );
    wSalesforceIDFieldName.setToolTipText( BaseMessages.getString(
      PKG, "SalesforceInsertDialog.SalesforceIDFieldName.Tooltip" ) );
    wSalesforceIDFieldName.addModifyListener( lsMod );
    fdSalesforceIDFieldName = new FormData();
    fdSalesforceIDFieldName.left = new FormAttachment( middle, 0 );
    fdSalesforceIDFieldName.top = new FormAttachment( wSettingsGroup, margin );
    fdSalesforceIDFieldName.right = new FormAttachment( 100, 0 );
    wSalesforceIDFieldName.setLayoutData( fdSalesforceIDFieldName );

    fdOutFieldsGroup = new FormData();
    fdOutFieldsGroup.left = new FormAttachment( 0, margin );
    fdOutFieldsGroup.top = new FormAttachment( wSettingsGroup, margin );
    fdOutFieldsGroup.right = new FormAttachment( 100, -margin );
    wOutFieldsGroup.setLayoutData( fdOutFieldsGroup );

    // ///////////////////////////////
    // END OF OutFields GROUP //
    // ///////////////////////////////

    // THE UPDATE/INSERT TABLE
    wlReturn = new Label( wGeneralComp, SWT.NONE );
    wlReturn.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.UpdateFields.Label" ) );
    props.setLook( wlReturn );
    fdlReturn = new FormData();
    fdlReturn.left = new FormAttachment( 0, 0 );
    fdlReturn.top = new FormAttachment( wOutFieldsGroup, margin );
    wlReturn.setLayoutData( fdlReturn );

    int UpInsCols = 3;
    int UpInsRows = ( input.getUpdateLookup() != null ? input.getUpdateLookup().length : 1 );

    ciReturn = new ColumnInfo[UpInsCols];
    ciReturn[0] =
      new ColumnInfo(
        BaseMessages.getString( PKG, "SalesforceInsertDialog.ColumnInfo.TableField" ),
        ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false );
    ciReturn[1] =
      new ColumnInfo(
        BaseMessages.getString( PKG, "SalesforceInsertDialog.ColumnInfo.StreamField" ),
        ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "" }, false );
    ciReturn[2] =
      new ColumnInfo(
        BaseMessages.getString( PKG, "SalesforceInsertDialog.ColumnInfo.UseExternalId" ),
        ColumnInfo.COLUMN_TYPE_CCOMBO, new String[] { "Y", "N" } );
    ciReturn[2].setToolTip( BaseMessages
      .getString( PKG, "SalesforceInsertDialog.ColumnInfo.UseExternalId.Tooltip" ) );
    tableFieldColumns.add( ciReturn[0] );
    wReturn =
      new TableView( transMeta, wGeneralComp, SWT.BORDER
        | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, ciReturn, UpInsRows, lsMod, props );

    wGetLU = new Button( wGeneralComp, SWT.PUSH );
    wGetLU.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.GetAndUpdateFields.Label" ) );
    fdGetLU = new FormData();
    fdGetLU.top = new FormAttachment( wlReturn, margin );
    fdGetLU.right = new FormAttachment( 100, 0 );
    wGetLU.setLayoutData( fdGetLU );

    wDoMapping = new Button( wGeneralComp, SWT.PUSH );
    wDoMapping.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.EditMapping.Label" ) );
    fdDoMapping = new FormData();
    fdDoMapping.top = new FormAttachment( wGetLU, margin );
    fdDoMapping.right = new FormAttachment( 100, 0 );
    wDoMapping.setLayoutData( fdDoMapping );

    wDoMapping.addListener( SWT.Selection, new Listener() {
      public void handleEvent( Event arg0 ) {
        generateMappings();
      }
    } );

    fdReturn = new FormData();
    fdReturn.left = new FormAttachment( 0, 0 );
    fdReturn.top = new FormAttachment( wlReturn, margin );
    fdReturn.right = new FormAttachment( wGetLU, -5 * margin );
    fdReturn.bottom = new FormAttachment( 100, -2 * margin );
    wReturn.setLayoutData( fdReturn );

    //
    // Search the fields in the background
    //

    final Runnable runnable = new Runnable() {
      public void run() {
        StepMeta stepMeta = transMeta.findStep( stepname );
        if ( stepMeta != null ) {
          try {
            RowMetaInterface row = transMeta.getPrevStepFields( stepMeta );

            // Remember these fields...
            for ( int i = 0; i < row.size(); i++ ) {
              inputFields.put( row.getValueMeta( i ).getName(), Integer.valueOf( i ) );
            }

            setComboBoxes();
            // Dislay in red missing field names
            Display.getDefault().asyncExec( new Runnable() {
              public void run() {
                if ( !wReturn.isDisposed() ) {
                  for ( int i = 0; i < wReturn.table.getItemCount(); i++ ) {
                    TableItem it = wReturn.table.getItem( i );
                    if ( !Const.isEmpty( it.getText( 2 ) ) ) {
                      if ( !inputFields.containsKey( it.getText( 2 ) ) ) {
                        it.setBackground( GUIResource.getInstance().getColorRed() );
                      }
                    }
                  }
                }
              }
            } );
          } catch ( KettleException e ) {
            logError( BaseMessages.getString( PKG, "System.Dialog.GetFieldsFailed.Message" ) );
          }
        }
      }
    };
    new Thread( runnable ).start();

    fdGeneralComp = new FormData();
    fdGeneralComp.left = new FormAttachment( 0, 0 );
    fdGeneralComp.top = new FormAttachment( wStepname, margin );
    fdGeneralComp.right = new FormAttachment( 100, 0 );
    fdGeneralComp.bottom = new FormAttachment( 100, 0 );
    wGeneralComp.setLayoutData( fdGeneralComp );

    wGeneralComp.layout();
    wGeneralTab.setControl( wGeneralComp );

    // THE BUTTONS
    wOK = new Button( shell, SWT.PUSH );
    wOK.setText( BaseMessages.getString( PKG, "System.Button.OK" ) );
    wCancel = new Button( shell, SWT.PUSH );
    wCancel.setText( BaseMessages.getString( PKG, "System.Button.Cancel" ) );

    setButtonPositions( new Button[] { wOK, wCancel }, margin, null );

    fdTabFolder = new FormData();
    fdTabFolder.left = new FormAttachment( 0, 0 );
    fdTabFolder.top = new FormAttachment( wStepname, margin );
    fdTabFolder.right = new FormAttachment( 100, 0 );
    fdTabFolder.bottom = new FormAttachment( wOK, -margin );
    wTabFolder.setLayoutData( fdTabFolder );

    // Add listeners
    lsOK = new Listener() {
      public void handleEvent( Event e ) {
        ok();
      }
    };
    lsTest = new Listener() {
      public void handleEvent( Event e ) {
        test();
      }
    };

    lsGetLU = new Listener() {
      public void handleEvent( Event e ) {
        getUpdate();
      }
    };
    lsCancel = new Listener() {
      public void handleEvent( Event e ) {
        cancel();
      }
    };

    wOK.addListener( SWT.Selection, lsOK );
    wGetLU.addListener( SWT.Selection, lsGetLU );
    wTest.addListener( SWT.Selection, lsTest );
    wCancel.addListener( SWT.Selection, lsCancel );

    lsDef = new SelectionAdapter() {
      public void widgetDefaultSelected( SelectionEvent e ) {
        ok();
      }
    };

    wStepname.addSelectionListener( lsDef );

    // Detect X or ALT-F4 or something that kills this window...
    shell.addShellListener( new ShellAdapter() {
      public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );

    wTabFolder.setSelection( 0 );

    // Set the shell size, based upon previous time...
    setSize();
    getData( input );
    input.setChanged( changed );

    shell.open();
    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
    return stepname;
  }

  private void getUpdate() {
    try {
      RowMetaInterface r = transMeta.getPrevStepFields( stepname );
      if ( r != null ) {
        TableItemInsertListener listener = new TableItemInsertListener() {
          public boolean tableItemInserted( TableItem tableItem, ValueMetaInterface v ) {
            tableItem.setText( 3, "Y" );
            return true;
          }
        };
        BaseStepDialog.getFieldsFromPrevious( r, wReturn, 1, new int[] { 1, 2 }, new int[] {}, -1, -1, listener );
      }
    } catch ( KettleException ke ) {
      new ErrorDialog(
        shell, BaseMessages.getString( PKG, "SalesforceInsertDialog.FailedToGetFields.DialogTitle" ),
        BaseMessages.getString( PKG, "SalesforceInsertDialog.FailedToGetFields.DialogMessage" ), ke );
    }
  }

  private void test() {
    SalesforceConnection connection = null;
    try {
      // check if the user is given
      if ( !checkUser() ) {
        return;
      }

      connection = getConnection();

      MessageBox mb = new MessageBox( shell, SWT.OK | SWT.ICON_INFORMATION );
      mb.setMessage( BaseMessages.getString( PKG, "SalesforceInsertDialog.Connected.OK", wUserName.getText() )
        + Const.CR );
      mb.setText( BaseMessages.getString( PKG, "SalesforceInsertDialog.Connected.Title.Ok" ) );
      mb.open();
    } catch ( Exception e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "SalesforceInsertDialog.Connected.Title.Error" ),
        BaseMessages.getString( PKG, "SalesforceInsertDialog.Connected.NOK", wUserName.getText() ),
        new Exception( e.getMessage() ) );
    } finally {
      if ( connection != null ) {
        try {
          connection.close();
        } catch ( Exception e ) {
          // Ignore close error
        }
      }
    }
  }

  /**
   * Read the data from the TextFileInputMeta object and show it in this dialog.
   *
   * @param in
   *          The SalesforceInsertMeta object to obtain the data from.
   */
  public void getData( SalesforceInsertMeta in ) {
    wURL.setText( Const.NVL( in.getTargetURL(), "" ) );
    wUserName.setText( Const.NVL( in.getUserName(), "" ) );
    wPassword.setText( Const.NVL( in.getPassword(), "" ) );
    wBatchSize.setText( in.getBatchSize() );
    wModule.setText( Const.NVL( in.getModule(), "Account" ) );
    wBatchSize.setText( "" + in.getBatchSize() );
    wSalesforceIDFieldName.setText( Const.NVL( in.getSalesforceIDFieldName(), "" ) );
    if ( isDebug() ) {
      logDebug( BaseMessages.getString( PKG, "SalesforceInsertDialog.Log.GettingFieldsInfo" ) );
    }

    if ( input.getUpdateLookup() != null ) {
      for ( int i = 0; i < input.getUpdateLookup().length; i++ ) {
        TableItem item = wReturn.table.getItem( i );
        if ( input.getUpdateLookup()[i] != null ) {
          item.setText( 1, input.getUpdateLookup()[i] );
        }
        if ( input.getUpdateStream()[i] != null ) {
          item.setText( 2, input.getUpdateStream()[i] );
        }
        if ( input.getUseExternalId()[i] == null || input.getUseExternalId()[i].booleanValue() ) {
          item.setText( 3, "Y" );
        } else {
          item.setText( 3, "N" );
        }
      }
    }

    wReturn.removeEmptyRows();
    wReturn.setRowNums();
    wReturn.optWidth( true );

    wTimeOut.setText( Const.NVL( in.getTimeOut(), SalesforceConnectionUtils.DEFAULT_TIMEOUT ) );
    wUseCompression.setSelection( in.isUsingCompression() );
    wRollbackAllChangesOnError.setSelection( in.isRollbackAllChangesOnError() );

    wStepname.selectAll();
    wStepname.setFocus();
  }

  private void cancel() {
    stepname = null;
    input.setChanged( changed );
    dispose();
  }

  private void ok() {
    try {
      getInfo( input );
    } catch ( KettleException e ) {
      new ErrorDialog(
        shell, BaseMessages.getString( PKG, "SalesforceInsertDialog.ErrorValidateData.DialogTitle" ),
        BaseMessages.getString( PKG, "SalesforceInsertDialog.ErrorValidateData.DialogMessage" ), e );
    }

    dispose();
  }

  private void getInfo( SalesforceInsertMeta in ) throws KettleException {
    stepname = wStepname.getText(); // return value

    // copy info to SalesforceInsertMeta class (input)
    in.setTargetURL( Const.NVL( wURL.getText(), SalesforceConnectionUtils.TARGET_DEFAULT_URL ) );
    in.setUserName( wUserName.getText() );
    in.setPassword( wPassword.getText() );
    in.setModule( Const.NVL( wModule.getText(), "Account" ) );
    in.setSalesforceIDFieldName( wSalesforceIDFieldName.getText() );
    in.setBatchSize( wBatchSize.getText() );

    int nrfields = wReturn.nrNonEmpty();

    in.allocate( nrfields );

    //CHECKSTYLE:Indentation:OFF
    for ( int i = 0; i < nrfields; i++ ) {
      TableItem item = wReturn.getNonEmpty( i );
      in.getUpdateLookup()[i] = item.getText( 1 );
      in.getUpdateStream()[i] = item.getText( 2 );
      in.getUseExternalId()[i] = Boolean.valueOf( "Y".equals( item.getText( 3 ) ) );
    }
    in.setUseCompression( wUseCompression.getSelection() );
    in.setTimeOut( Const.NVL( wTimeOut.getText(), "0" ) );
    in.setRollbackAllChangesOnError( wRollbackAllChangesOnError.getSelection() );
  }

  // check if module, username is given
  private boolean checkInput() {
    if ( Const.isEmpty( wModule.getText() ) ) {
      MessageBox mb = new MessageBox( shell, SWT.OK | SWT.ICON_ERROR );
      mb.setMessage( BaseMessages.getString( PKG, "SalesforceInsertDialog.ModuleMissing.DialogMessage" ) );
      mb.setText( BaseMessages.getString( PKG, "System.Dialog.Error.Title" ) );
      mb.open();
      return false;
    }
    return checkUser();
  }

  // check if module, username is given
  private boolean checkUser() {

    if ( Const.isEmpty( wUserName.getText() ) ) {
      MessageBox mb = new MessageBox( shell, SWT.OK | SWT.ICON_ERROR );
      mb.setMessage( BaseMessages.getString( PKG, "SalesforceInsertDialog.UsernameMissing.DialogMessage" ) );
      mb.setText( BaseMessages.getString( PKG, "System.Dialog.Error.Title" ) );
      mb.open();
      return false;
    }

    return true;
  }

  private SalesforceConnection getConnection() throws KettleException {
    String url = transMeta.environmentSubstitute( wURL.getText() );
    // Define a new Salesforce connection
    SalesforceConnection connection =
      new SalesforceConnection( log, url, transMeta.environmentSubstitute( wUserName.getText() ), transMeta
        .environmentSubstitute( wPassword.getText() ) );
    int realTimeOut = Const.toInt( transMeta.environmentSubstitute( wTimeOut.getText() ), 0 );
    connection.setTimeOut( realTimeOut );
    // connect to Salesforce
    connection.connect();

    return connection;
  }

  private String[] getFieldNames() throws KettleException {
    SalesforceConnection connection = null;
    String selectedModule = transMeta.environmentSubstitute( wModule.getText() );
    try {
      // Define a new Salesforce connection
      connection = getConnection();
      // return fieldsname for the module
      return connection.getFields( selectedModule );
    } catch ( Exception e ) {
      throw new KettleException( "Error getting fields from module [" + selectedModule + "]!", e );
    } finally {
      if ( connection != null ) {
        try {
          connection.close();
        } catch ( Exception e ) {
          // Ignore close errors
        }
      }
    }
  }

  /**
   * Reads in the fields from the previous steps and from the ONE next step and opens an EnterMappingDialog with this
   * information. After the user did the mapping, those information is put into the Select/Rename table.
   */
  private void generateMappings() {

    if ( !checkInput() ) {
      return;
    }

    // Determine the source and target fields...
    //
    RowMetaInterface sourceFields;
    RowMetaInterface targetFields = new RowMeta();

    try {
      sourceFields = transMeta.getPrevStepFields( stepMeta );
    } catch ( KettleException e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "SalesforceInsertDialog.DoMapping.UnableToFindSourceFields.Title" ),
        BaseMessages.getString( PKG, "SalesforceInsertDialog.DoMapping.UnableToFindSourceFields.Message" ), e );
      return;
    }

    try {
      SalesforceConnection connection = getConnection();
      Field[] fields = connection.getObjectFields( transMeta.environmentSubstitute( wModule.getText() ) );
      String[] fieldNames = connection.getFields( fields );

      FieldType dateType = FieldType.fromString( "date" );
      for ( int i = 0; i < fields.length; i++ ) {
        if ( dateType.equals( fields[ i ].getType() ) ) {
          // Mark date columns as TYPE_DATE to strip time part later
          targetFields.addValueMeta( ValueMetaFactory.createValueMeta( fieldNames[i], ValueMetaInterface.TYPE_DATE ) );
        } else {
          targetFields.addValueMeta( new ValueMeta( fieldNames[i] ) );
        }
      }
    } catch ( Exception e ) {
      new ErrorDialog( shell,
        BaseMessages.getString( PKG, "SalesforceInsertDialog.DoMapping.UnableToFindTargetFields.Title" ),
        BaseMessages.getString( PKG, "SalesforceInsertDialog.DoMapping.UnableToFindTargetFields.Message" ), e );
      return;
    }

    String[] inputNames = new String[sourceFields.size()];
    for ( int i = 0; i < sourceFields.size(); i++ ) {
      ValueMetaInterface value = sourceFields.getValueMeta( i );
      inputNames[i] = value.getName() + EnterMappingDialog.STRING_ORIGIN_SEPARATOR + value.getOrigin() + ")";
    }

    // Create the existing mapping list...
    //
    List<SourceToTargetMapping> mappings = new ArrayList<SourceToTargetMapping>();
    StringBuffer missingSourceFields = new StringBuffer();
    StringBuffer missingTargetFields = new StringBuffer();

    int nrFields = wReturn.nrNonEmpty();
    for ( int i = 0; i < nrFields; i++ ) {
      TableItem item = wReturn.getNonEmpty( i );
      String source = item.getText( 2 );
      String target = item.getText( 1 );

      int sourceIndex = sourceFields.indexOfValue( source );
      if ( sourceIndex < 0 ) {
        missingSourceFields.append( Const.CR + "   " + source + " --> " + target );
      }
      int targetIndex = targetFields.indexOfValue( target );
      if ( targetIndex < 0 ) {
        missingTargetFields.append( Const.CR + "   " + source + " --> " + target );
      }
      if ( sourceIndex < 0 || targetIndex < 0 ) {
        continue;
      }

      SourceToTargetMapping mapping = new SourceToTargetMapping( sourceIndex, targetIndex );
      mappings.add( mapping );
    }

    // show a confirm dialog if some missing field was found
    //
    if ( missingSourceFields.length() > 0 || missingTargetFields.length() > 0 ) {

      String message = "";
      if ( missingSourceFields.length() > 0 ) {
        message +=
          BaseMessages.getString(
            PKG, "SalesforceInsertDialog.DoMapping.SomeSourceFieldsNotFound", missingSourceFields.toString() )
            + Const.CR;
      }
      if ( missingTargetFields.length() > 0 ) {
        message +=
          BaseMessages.getString(
            PKG, "SalesforceInsertDialog.DoMapping.SomeTargetFieldsNotFound", missingSourceFields.toString() )
            + Const.CR;
      }
      message += Const.CR;
      message +=
        BaseMessages.getString( PKG, "SalesforceInsertDialog.DoMapping.SomeFieldsNotFoundContinue" ) + Const.CR;
      MessageDialog.setDefaultImage( GUIResource.getInstance().getImageSpoon() );
      boolean goOn =
        MessageDialog.openConfirm( shell, BaseMessages.getString(
          PKG, "SalesforceInsertDialog.DoMapping.SomeFieldsNotFoundTitle" ), message );
      if ( !goOn ) {
        return;
      }
    }
    EnterMappingDialog d =
      new EnterMappingDialog( SalesforceInsertDialog.this.shell, sourceFields.getFieldNames(), targetFields
        .getFieldNames(), mappings );
    mappings = d.open();

    // mappings == null if the user pressed cancel
    //
    if ( mappings != null ) {
      // Clear and re-populate!
      //
      wReturn.table.removeAll();
      wReturn.table.setItemCount( mappings.size() );
      for ( int i = 0; i < mappings.size(); i++ ) {
        SourceToTargetMapping mapping = mappings.get( i );
        TableItem item = wReturn.table.getItem( i );
        item.setText( 2, sourceFields.getValueMeta( mapping.getSourcePosition() ).getName() );
        item.setText( 1, targetFields.getValueMeta( mapping.getTargetPosition() ).getName() );
      }
      wReturn.setRowNums();
      wReturn.optWidth( true );
    }
  }

  protected void setComboBoxes() {
    // Something was changed in the row.
    //
    final Map<String, Integer> fields = new HashMap<String, Integer>();

    // Add the currentMeta fields...
    fields.putAll( inputFields );

    Set<String> keySet = fields.keySet();
    List<String> entries = new ArrayList<String>( keySet );

    String[] fieldNames = entries.toArray( new String[entries.size()] );
    Const.sortStrings( fieldNames );
    // return fields
    ciReturn[1].setComboValues( fieldNames );
  }

  public void setModuleFieldCombo() {
    if ( gotFields ) {
      return;
    }
    gotFields = true;
    Display display = shell.getDisplay();
    if ( !( display == null || display.isDisposed() ) ) {
      display.asyncExec( new Runnable() {
        public void run() {
          // clear
          for ( int i = 0; i < tableFieldColumns.size(); i++ ) {
            ColumnInfo colInfo = tableFieldColumns.get( i );
            colInfo.setComboValues( new String[] {} );
          }
          if ( wModule.isDisposed() ) {
            return;
          }
          String selectedModule = transMeta.environmentSubstitute( wModule.getText() );
          if ( !Const.isEmpty( selectedModule ) ) {
            try {
              // loop through the objects and find build the list of fields
              String[] fieldsName = getFieldNames();

              if ( fieldsName != null ) {
                for ( int i = 0; i < tableFieldColumns.size(); i++ ) {
                  ColumnInfo colInfo = tableFieldColumns.get( i );
                  colInfo.setComboValues( fieldsName );
                }
              }
            } catch ( Exception e ) {
              for ( int i = 0; i < tableFieldColumns.size(); i++ ) {
                ColumnInfo colInfo = tableFieldColumns.get( i );
                colInfo.setComboValues( new String[] {} );
              }
              // ignore any errors here. drop downs will not be
              // filled, but no problem for the user
            }
          }

        }
      } );
    }
  }

  private void getModulesList() {
    if ( !gotModule ) {
      SalesforceConnection connection = null;

      try {
        String selectedField = wModule.getText();
        wModule.removeAll();

        connection = getConnection();
        // return
        wModule.setItems( connection.getAllAvailableObjects( false ) );

        if ( !Const.isEmpty( selectedField ) ) {
          wModule.setText( selectedField );
        }

        gotModule = true;
        getModulesListError = false;

      } catch ( Exception e ) {
        new ErrorDialog( shell,
          BaseMessages.getString( PKG, "SalesforceInsertDialog.ErrorRetrieveModules.DialogTitle" ),
          BaseMessages.getString( PKG, "SalesforceInsertDialog.ErrorRetrieveData.ErrorRetrieveModules" ), e );
        getModulesListError = true;
      } finally {
        if ( connection != null ) {
          try {
            connection.close();
          } catch ( Exception e ) { /* Ignore */
          }
        }
      }
    }
  }
}
