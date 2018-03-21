/*
 *   This software is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This software is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with this software.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Copyright 2011 De Bortoli Wines Pty Limited (Australia)
 */

package org.pentaho.di.ui.trans.steps.openerp.objectoutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.SourceToTargetMapping;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.steps.openerp.objectoutput.OpenERPObjectOutputData;
import org.pentaho.di.trans.steps.openerp.objectoutput.OpenERPObjectOutputMeta;
import org.pentaho.di.ui.core.dialog.EnterMappingDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.ui.trans.step.TableItemInsertListener;

public class OpenERPObjectOutputDialog extends BaseStepDialog implements StepDialogInterface {

  private static Class<?> PKG = OpenERPObjectOutputMeta.class; // for i18n purposes, needed by Translator2!!

  ColumnInfo[] fieldMappingColumnInfo;
  private ColumnInfo[] keyFieldsViewColinf;

  private final OpenERPObjectOutputMeta meta;
  private Label labelStepName;
  private Text textStepName;
  private CCombo addConnectionLine;
  private Label labelModelName;
  private CCombo comboModelName;
  private Label labelCommitBatchSize;
  private Text textCommitBatchSize;
  private Label labelOutputIDField;
  private Button buttonOutputIDField;
  private Label labelIDFieldName;
  private Text textIDFieldName;
  private Label labelKeyFields;
  private Button buttonGetKeyFields;
  private TableView tableViewKeyFields;
  private Label labelFieldMappings;
  private TableView tableViewFieldMappings;
  private Button buttonGetMappingFields;
  private Button buttonDoMappings;
  private Button buttonOk;
  private Button buttonCancel;

  public OpenERPObjectOutputDialog( Shell parent, Object in, TransMeta transMeta, String sname ) {
    super( parent, (BaseStepMeta) in, transMeta, sname );
    this.meta = (OpenERPObjectOutputMeta) in;
  }

  @Override
  public String open() {

    ModifyListener lsMod = new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        meta.setChanged();
      }
    };

    final Display display = getParent().getDisplay();
    shell = new Shell( getParent(), SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN );
    props.setLook( shell );
    setShellImage( shell, meta );
    FormLayout formLayout = new FormLayout();
    formLayout.marginWidth = Const.FORM_MARGIN;
    formLayout.marginHeight = Const.FORM_MARGIN;

    shell.setLayout( formLayout );
    int middle = props.getMiddlePct();
    int margin = Const.MARGIN;

    FormData fd;

    labelStepName = new Label( shell, SWT.RIGHT );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( 0, margin );
    labelStepName.setLayoutData( fd );

    textStepName = new Text( shell, SWT.BORDER );
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( 0, margin );
    textStepName.setLayoutData( fd );

    addConnectionLine = addConnectionLine( shell, textStepName, Const.MIDDLE_PCT, margin );

    labelModelName = new Label( shell, SWT.RIGHT );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( addConnectionLine, margin );
    labelModelName.setLayoutData( fd );

    comboModelName = new CCombo( shell, SWT.BORDER );
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( addConnectionLine, margin );
    comboModelName.setLayoutData( fd );

    labelCommitBatchSize = new Label( shell, SWT.RIGHT );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( comboModelName, margin );
    labelCommitBatchSize.setLayoutData( fd );

    textCommitBatchSize = new Text( shell, SWT.BORDER );
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( comboModelName, margin );
    textCommitBatchSize.setLayoutData( fd );

    labelOutputIDField = new Label( shell, SWT.RIGHT );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( textCommitBatchSize, margin );
    labelOutputIDField.setLayoutData( fd );

    buttonOutputIDField = new Button( shell, SWT.CHECK );
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( textCommitBatchSize, margin );
    buttonOutputIDField.setLayoutData( fd );

    labelIDFieldName = new Label( shell, SWT.RIGHT );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( labelOutputIDField, margin );
    labelIDFieldName.setLayoutData( fd );

    textIDFieldName = new Text( shell, SWT.BORDER );
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( labelOutputIDField, margin );
    textIDFieldName.setLayoutData( fd );

    labelKeyFields = new Label( shell, SWT.NONE );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( textIDFieldName, margin );
    labelKeyFields.setLayoutData( fd );

    keyFieldsViewColinf =
      new ColumnInfo[]{
        new ColumnInfo( getLocalizedKeyColumn( 0 ), ColumnInfo.COLUMN_TYPE_CCOMBO, new String[]{ "" }, false ),
        new ColumnInfo( getLocalizedKeyColumn( 1 ), ColumnInfo.COLUMN_TYPE_CCOMBO, new String[]{ "" }, false ),
        new ColumnInfo( getLocalizedKeyColumn( 2 ), ColumnInfo.COLUMN_TYPE_CCOMBO, new String[]{ "" }, false ) };

    tableViewKeyFields =
      new TableView( null, shell, SWT.MULTI | SWT.BORDER, keyFieldsViewColinf, 0, true, lsMod, props );
    tableViewKeyFields.setReadonly( false );
    tableViewKeyFields.setSortable( false );
    fd = new FormData();
    fd.left = new FormAttachment( 0, margin );
    fd.top = new FormAttachment( labelKeyFields, 3 * margin );
    fd.right = new FormAttachment( 100, -150 );
    fd.bottom = new FormAttachment( labelKeyFields, 200 );
    tableViewKeyFields.setLayoutData( fd );

    buttonGetKeyFields = new Button( shell, SWT.NONE );
    fd = new FormData();
    fd.left = new FormAttachment( tableViewKeyFields, margin );
    fd.top = new FormAttachment( labelKeyFields, 3 * margin );
    fd.right = new FormAttachment( 100, 0 );
    buttonGetKeyFields.setLayoutData( fd );

    labelFieldMappings = new Label( shell, SWT.NONE );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.top = new FormAttachment( tableViewKeyFields, margin );
    labelFieldMappings.setLayoutData( fd );

    fieldMappingColumnInfo =
      new ColumnInfo[]{
        new ColumnInfo( getLocalizedColumn( 0 ), ColumnInfo.COLUMN_TYPE_CCOMBO, new String[]{ "" }, false ),
        new ColumnInfo( getLocalizedColumn( 1 ), ColumnInfo.COLUMN_TYPE_CCOMBO, new String[]{ "" }, false ) };

    tableViewFieldMappings =
      new TableView( null, shell, SWT.FILL | SWT.BORDER, fieldMappingColumnInfo, 0, true, lsMod, props );
    tableViewFieldMappings.setSize( 477, 280 );
    tableViewFieldMappings.setBounds( 5, 125, 477, 280 );
    tableViewFieldMappings.setReadonly( false );
    tableViewFieldMappings.setSortable( true );
    fd = new FormData();
    fd.left = new FormAttachment( 0, margin );
    fd.top = new FormAttachment( labelFieldMappings, 3 * margin );
    fd.right = new FormAttachment( 100, -150 );
    fd.bottom = new FormAttachment( 100, -50 );
    tableViewFieldMappings.setLayoutData( fd );

    buttonGetMappingFields = new Button( shell, SWT.NONE );
    fd = new FormData();
    fd.left = new FormAttachment( tableViewFieldMappings, margin );
    fd.top = new FormAttachment( labelFieldMappings, 3 * margin );
    fd.right = new FormAttachment( 100, 0 );
    buttonGetMappingFields.setLayoutData( fd );

    buttonDoMappings = new Button( shell, SWT.NONE );
    fd = new FormData();
    fd.left = new FormAttachment( tableViewFieldMappings, margin );
    fd.top = new FormAttachment( buttonGetMappingFields, 3 * margin );
    fd.right = new FormAttachment( 100, 0 );
    buttonDoMappings.setLayoutData( fd );

    buttonOk = new Button( shell, SWT.CENTER );
    buttonCancel = new Button( shell, SWT.CENTER );
    buttonOk.setText( BaseMessages.getString( "System.Button.OK" ) );
    buttonCancel.setText( BaseMessages.getString( "System.Button.Cancel" ) );
    setButtonPositions( new Button[]{ buttonOk, buttonCancel }, margin, null );

    addConnectionLine.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        setModelComboOptions();
      }
    } );

    comboModelName.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        setTalbleFieldsOptions();
      }
    } );
    comboModelName.addFocusListener( new FocusListener() {

      @Override
      public void focusLost( FocusEvent arg0 ) {
        setTalbleFieldsOptions();
      }

      @Override
      public void focusGained( FocusEvent arg0 ) {
      }
    } );
    buttonOutputIDField.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent arg0 ) {
        textIDFieldName.setEnabled( buttonOutputIDField.getSelection() );
      }
    } );
    buttonGetKeyFields.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        getKeyFields();
      }
    } );
    buttonGetMappingFields.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        getUpdateFields();
      }
    } );
    buttonDoMappings.addListener( SWT.Selection, new Listener() {
      public void handleEvent( Event arg0 ) {
        generateMappings();
      }
    } );

    buttonCancel.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        cancel();
      }
    } );
    buttonOk.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        ok();
      }
    } );

    //
    // Search the fields in the background
    //
    final Runnable runnable = new Runnable() {
      public void run() {
        setModelComboOptions();
        setStreamFieldsOptions();
        setTalbleFieldsOptions();
      }
    };
    display.asyncExec( runnable );

    this.fillLocalizationData();
    this.fillStoredData();

    props.setLook( labelStepName );
    props.setLook( textStepName );
    props.setLook( addConnectionLine );
    props.setLook( labelModelName );
    props.setLook( comboModelName );
    props.setLook( labelCommitBatchSize );
    props.setLook( textCommitBatchSize );
    props.setLook( labelOutputIDField );
    props.setLook( buttonOutputIDField );
    props.setLook( labelIDFieldName );
    props.setLook( textIDFieldName );
    props.setLook( labelKeyFields );
    props.setLook( labelFieldMappings );

    meta.setChanged( changed );
    setSize();
    shell.open();

    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }

    return stepname;
  }

  private String getLocalizedKeyColumn( int columnIndex ) {
    switch ( columnIndex ) {

      case 0:
        return BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.KeyMappingModelField" );
      case 1:
        return BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.KeyMappingComparisonField" );
      case 2:
        return BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.KeyMappingStreamField" );
      default:
        return "";
    }
  }

  private void fillLocalizationData() {
    shell.setText( BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.Title" ) );
    labelStepName.setText( BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.StepName" ) );
    labelModelName.setText( BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.ModelName" ) );
    labelCommitBatchSize.setText( BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.CommitBatchSize" ) );
    labelOutputIDField.setText( BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.OutputIDField" ) );
    labelIDFieldName.setText( BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.OutputIDFieldName" ) );
    labelKeyFields.setText( BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.KeyMappingLabel" ) );
    buttonGetKeyFields.setText( BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.ButtonGetKeyFields" ) );
    buttonGetMappingFields.setText( BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.ButtonGetMappingFields" ) );
    labelFieldMappings.setText( BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.MappingLabel" ) );
    buttonDoMappings.setText( BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.ButtonDoMappings" ) );

  }

  private String getLocalizedColumn( int columnIndex ) {
    switch ( columnIndex ) {
      case 0:
        return BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.MappingModelField" );
      case 1:
        return BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.MappingStreamField" );
      default:
        return "";
    }
  }

  private void setModelComboOptions() {
    String[] objectList = getModelList();
    if ( objectList == null ) {
      return;
    }

    for ( String objectName : objectList ) {
      if ( comboModelName.indexOf( objectName ) == -1 ) {
        comboModelName.add( objectName );
      }
    }
  }

  private String[] getModelList() {
    String[] objectList = null;
    // Fill object name
    if ( addConnectionLine.getText() != null ) {
      DatabaseMeta dbMeta = transMeta.findDatabase( addConnectionLine.getText() );

      if ( dbMeta != null ) {

        OpenERPObjectOutputData data = null;
        try {
          data = new OpenERPObjectOutputData( dbMeta );
          data.helper.StartSession();
          objectList = data.helper.getModelList();
        } catch ( Exception e ) {
          return null;
        }
      }
    }
    return objectList;
  }

  private void setTalbleFieldsOptions() {
    String[] modelFieldName = getModelFieldNames();

    if ( modelFieldName == null ) {
      return;
    }

    fieldMappingColumnInfo[0].setComboValues( getModelFieldNames() );
    tableViewFieldMappings.optWidth( true );

    ArrayList<String> options = new ArrayList<String>();
    options.add( "id" );
    Collections.addAll( options, modelFieldName );

    keyFieldsViewColinf[0].setComboValues( options.toArray( new String[options.size()] ) );
    tableViewKeyFields.optWidth( true );
  }

  private String[] getModelFieldNames() {
    return getModelFieldNames( false );
  }

  private String[] getModelFieldNames( boolean showError ) {
    // Set table fields
    if ( addConnectionLine.getText() != null ) {
      DatabaseMeta dbMeta = transMeta.findDatabase( addConnectionLine.getText() );

      if ( dbMeta != null ) {

        OpenERPObjectOutputData data = null;
        try {
          data = new OpenERPObjectOutputData( dbMeta );
          data.helper.StartSession();

          // If errors should be reported, check that the model exists. If we don't check the
          // server just returns the generic can't parse int to string xmlrpc error
          if ( showError ) {
            String[] modelList = getModelList();

            boolean found = false;
            for ( String model : modelList ) {
              if ( model.equals( comboModelName.getText() ) ) {
                found = true;
                break;
              }
            }

            if ( !found ) {
              new ErrorDialog( shell,
                BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.ConnectionErrorTitle" ),
                BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.ConnectionErrorString" ),
                new Exception(
                  BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.ModelNotFoundError", comboModelName
                    .getText() ) ) );
              return null;
            }
          }

          return data.helper.getOutputFields( comboModelName.getText() );
        } catch ( Exception e ) {
          if ( showError ) {
            new ErrorDialog( shell,
              BaseMessages.getString( PKG, "OpenERPObjectOuputDialog.ConnectionErrorTitle" ),
              BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.ConnectionErrorString" ), e );
          }
          return null;
        }
      }
    }
    return null;
  }

  private void setStreamFieldsOptions() {
    String[] steamFields = getSteamFieldsNames();

    if ( steamFields != null ) {
      fieldMappingColumnInfo[1].setComboValues( new String[]{ } );
      fieldMappingColumnInfo[1].setComboValues( steamFields );
      tableViewFieldMappings.optWidth( true );

      keyFieldsViewColinf[2].setComboValues( new String[]{ } );
      keyFieldsViewColinf[2].setComboValues( steamFields );
      tableViewKeyFields.optWidth( true );
    }
  }

  private String[] getSteamFieldsNames() {
    return getSteamFieldsNames( false );
  }

  private String[] getSteamFieldsNames( boolean showError ) {
    String[] fields = null;

    // Set stream fields
    RowMetaInterface row;
    try {
      row = transMeta.getPrevStepFields( stepMeta );
      fields = new String[row.size()];
      for ( int i = 0; i < row.size(); i++ ) {
        fields[i] = row.getValueMeta( i ).getName();
      }
    } catch ( KettleStepException e ) {
      if ( showError ) {
        new ErrorDialog( shell,
          BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.UnableToFindStreamFieldsTitle" ), BaseMessages
          .getString( PKG, "OpenERPObjectOutputDialog.UnableToFindStreamFieldsMessage" ), e );
      }
      return null;
    }

    return fields;
  }

  /**
   * GRABBED FROM THE TABLE OUTPUT STEP
   * <p/>
   * Reads in the fields from the previous steps and from the ONE next step and opens an EnterMappingDialog with this
   * information. After the user did the mapping, those information is put into the Select/Rename table.
   */
  private void generateMappings() {

    // Determine the source and target fields...
    //
    String[] sourceFields = getSteamFieldsNames( true );
    if ( sourceFields == null ) {
      return;
    }

    String[] targetFields = getModelFieldNames( true );
    if ( targetFields == null ) {
      return;
    }

    List<String> sourceFieldList = Arrays.asList( sourceFields );
    List<String> targetFieldsList = Arrays.asList( targetFields );

    // Create the existing mapping list...
    //
    List<SourceToTargetMapping> mappings = new ArrayList<SourceToTargetMapping>();
    StringBuffer missingSourceFields = new StringBuffer();
    StringBuffer missingTargetFields = new StringBuffer();

    int nrFields = tableViewFieldMappings.nrNonEmpty();
    for ( int i = 0; i < nrFields; i++ ) {
      TableItem item = tableViewFieldMappings.getNonEmpty( i );
      String source = item.getText( 2 );
      String target = item.getText( 1 );

      int sourceIndex = sourceFieldList.indexOf( source );
      if ( sourceIndex < 0 ) {
        missingSourceFields.append( Const.CR + "   " + source + " --> " + target );
      }
      int targetIndex = targetFieldsList.indexOf( target );
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
          BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.DoMapping.SomeSourceFieldsNotFound",
            missingSourceFields.toString() )
            + Const.CR;
      }
      if ( missingTargetFields.length() > 0 ) {
        message +=
          BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.DoMapping.SomeTargetFieldsNotFound",
            missingSourceFields.toString() )
            + Const.CR;
      }
      message += Const.CR;
      message +=
        BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.DoMapping.SomeFieldsNotFoundContinue" ) + Const.CR;
      MessageDialog.setDefaultImage( GUIResource.getInstance().getImageSpoon() );
      boolean goOn =
        MessageDialog.openConfirm( shell, BaseMessages.getString( PKG,
          "OpenERPObjectOutputDialog.DoMapping.SomeFieldsNotFoundTitle" ), message );
      if ( !goOn ) {
        return;
      }
    }

    EnterMappingDialog d = new EnterMappingDialog( this.shell, sourceFields, targetFields, mappings );
    mappings = d.open();

    // mappings == null if the user pressed cancel
    //
    if ( mappings != null ) {
      // Clear and re-populate!
      //
      tableViewFieldMappings.table.removeAll();
      tableViewFieldMappings.table.setItemCount( mappings.size() );
      for ( int i = 0; i < mappings.size(); i++ ) {
        SourceToTargetMapping mapping = mappings.get( i );
        TableItem item = tableViewFieldMappings.table.getItem( i );
        item.setText( 2, sourceFields[mapping.getSourcePosition()] );
        item.setText( 1, targetFields[mapping.getTargetPosition()] );
      }
      tableViewFieldMappings.setRowNums();
      tableViewFieldMappings.optWidth( true );
    }
  }

  // Grabbed from the update step
  private void getKeyFields() {
    try {
      RowMetaInterface r = transMeta.getPrevStepFields( stepname );
      if ( r != null && !r.isEmpty() ) {
        TableItemInsertListener listener = new TableItemInsertListener() {
          public boolean tableItemInserted( TableItem tableItem, ValueMetaInterface v ) {
            tableItem.setText( 2, "=" );
            return true;
          }
        };
        BaseStepDialog.getFieldsFromPrevious( r, tableViewKeyFields, 1, new int[]{ 1, 3 }, new int[]{ }, -1, -1,
          listener );
      }
    } catch ( KettleException ke ) {
      new ErrorDialog( shell, BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.FailedToGetFields.Title" ),
        BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.FailedToGetFields.Message" ), ke );
    }
  }

  // Grabbed from the update step
  private void getUpdateFields() {
    try {
      RowMetaInterface r = transMeta.getPrevStepFields( stepname );
      if ( r != null && !r.isEmpty() ) {
        BaseStepDialog.getFieldsFromPrevious( r, tableViewFieldMappings, 1, new int[]{ 1, 2 }, new int[]{ }, -1, -1,
          null );
      }
    } catch ( KettleException ke ) {
      new ErrorDialog( shell, BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.FailedToGetFields.Title" ),
        BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.FailedToGetFields.Message" ), ke );
    }
  }

  private void setTextItem( TableItem item, int index, Object value ) {
    item.setText( index, ( value == null ? "" : value.toString() ) );
  }

  private void fillStoredData() {

    if ( stepname != null ) {
      textStepName.setText( stepname );
    }

    int index = addConnectionLine.indexOf( meta.getDatabaseMeta() != null ? meta.getDatabaseMeta().getName() : "" );
    if ( index >= 0 ) {
      addConnectionLine.select( index );
    }

    if ( meta.getModelName() != null ) {
      comboModelName.add( meta.getModelName() );
      comboModelName.select( 0 );
    }

    buttonOutputIDField.setSelection( meta.getOutputIDField() );
    textIDFieldName.setText( ( meta.getOutputIDFieldName() == null ? "" : meta.getOutputIDFieldName() ) );
    textIDFieldName.setEnabled( buttonOutputIDField.getSelection() );

    textCommitBatchSize.setText( String.valueOf( meta.getCommitBatchSize() ) );

    keyFieldsViewColinf[1].setComboValues( new String[]{ "=", "is null", "is not null" } );

    tableViewKeyFields.table.removeAll();
    tableViewKeyFields.table.setItemCount( meta.getKeyLookups().size() );
    for ( int i = 0; i < meta.getKeyLookups().size(); i++ ) {
      TableItem item = tableViewKeyFields.table.getItem( i );
      setTextItem( item, 1, meta.getKeyLookups().get( i )[0] );
      setTextItem( item, 2, meta.getKeyLookups().get( i )[1] );
      setTextItem( item, 3, meta.getKeyLookups().get( i )[2] );
    }
    tableViewKeyFields.removeEmptyRows();
    tableViewKeyFields.setRowNums();
    tableViewKeyFields.optWidth( true );

    tableViewFieldMappings.table.removeAll();
    tableViewFieldMappings.table.setItemCount( meta.getModelFields().length );
    for ( int i = 0; i < meta.getModelFields().length; i++ ) {
      TableItem item = tableViewFieldMappings.table.getItem( i );
      setTextItem( item, 1, meta.getModelFields()[i] );
      setTextItem( item, 2, meta.getStreamFields()[i] );
    }
    tableViewFieldMappings.removeEmptyRows();
    tableViewFieldMappings.setRowNums();
    tableViewFieldMappings.optWidth( true );
  }

  private void cancel() {
    stepname = null;
    meta.setChanged( changed );
    dispose();
  }

  private void ok() {
    if ( SaveToMeta( meta ) ) {
      dispose();
    }
  }

  private boolean SaveToMeta( OpenERPObjectOutputMeta targetMeta ) {
    stepname = textStepName.getText();

    DatabaseMeta dbMeta = transMeta.findDatabase( addConnectionLine.getText() );
    if ( dbMeta != null ) {
      try {
        new OpenERPObjectOutputData( dbMeta );
      } catch ( KettleException e ) {
        new ErrorDialog( shell, BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.ConnectionTypeErrorTitle" ),
          BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.ConnectionTypeErrorString" ), e );
        return false;
      }
    }

    int commitBatchSize = 0;
    try {
      commitBatchSize = Integer.parseInt( textCommitBatchSize.getText() );
    } catch ( NumberFormatException e ) {
      new ErrorDialog( shell, BaseMessages.getString( PKG, "OpenERPObjectOutputDialog.ParseErrorTitle" ), BaseMessages
        .getString( PKG, "OpenERPObjectOutputDialog.ParseErrorString", textCommitBatchSize.getText() ), e );
      return false;
    }

    meta.setOutputIDField( buttonOutputIDField.getSelection() );
    meta.setOutputIDFieldName( textIDFieldName.getText() );

    String[] targetTableFields = new String[tableViewFieldMappings.table.getItemCount()];
    String[] streamFields = new String[tableViewFieldMappings.table.getItemCount()];
    for ( int i = 0; i < tableViewFieldMappings.table.getItemCount(); i++ ) {
      targetTableFields[i] = tableViewFieldMappings.table.getItem( i ).getText( 1 );
      streamFields[i] = tableViewFieldMappings.table.getItem( i ).getText( 2 );
    }

    ArrayList<String[]> keyFields = new ArrayList<String[]>();
    tableViewKeyFields.removeEmptyRows();
    for ( int i = 0; i < tableViewKeyFields.table.getItemCount(); i++ ) {
      String[] keyMap = {
        tableViewKeyFields.table.getItem( i ).getText( 1 ),
        tableViewKeyFields.table.getItem( i ).getText( 2 ),
        tableViewKeyFields.table.getItem( i ).getText( 3 )
      };

      // Skip blank line returned by the table. If the first line is blank it is still returned.
      if ( keyMap[0] == "" && keyMap[1] == "" && keyMap[2] == "" ) {
        continue;
      }

      keyFields.add( keyMap );
    }

    targetMeta.setKeyLookups( keyFields );
    targetMeta.setModelFields( targetTableFields );
    targetMeta.setStreamFields( streamFields );
    targetMeta.setDatabaseMeta( transMeta.findDatabase( addConnectionLine.getText() ) );
    targetMeta.setModelName( comboModelName.getText() );
    targetMeta.setCommitBatchSize( commitBatchSize );
    targetMeta.setChanged( true );

    return true;

  }
}
