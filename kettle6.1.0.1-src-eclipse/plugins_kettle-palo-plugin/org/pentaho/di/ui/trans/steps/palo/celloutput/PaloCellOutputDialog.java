/*
 *   This file is part of PaloKettlePlugin.
 *
 *   PaloKettlePlugin is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   PaloKettlePlugin is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with PaloKettlePlugin.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Portions Copyright 2008 Stratebi Business Solutions, S.L.
 *   Portions Copyright 2011 De Bortoli Wines Pty Limited (Australia)
 *   Portions Copyright 2011 - 2013 Pentaho Corporation
 */

package org.pentaho.di.ui.trans.steps.palo.celloutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.palo.core.DimensionField;
import org.pentaho.di.palo.core.PaloHelper;
import org.pentaho.di.palo.core.PaloNameComparator;
import org.pentaho.di.palo.core.PaloOption;
import org.pentaho.di.palo.core.PaloOptionCollection;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.steps.palo.celloutput.PaloCellOutputData;
import org.pentaho.di.trans.steps.palo.celloutput.PaloCellOutputMeta;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.widget.ColumnInfo;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.di.ui.trans.steps.palo.cellinput.PaloCellInputDialog;

public class PaloCellOutputDialog extends BaseStepDialog implements StepDialogInterface {
  private static Class<?> PKG = PaloCellOutputMeta.class; // for i18n purposes,
  // needed by
  // Translator2!!

  public static void main( String[] args ) {
    try {
      PaloCellOutputDialog window = new PaloCellOutputDialog( null, new PaloCellOutputMeta(), null, "noname" );
      window.open();
    } catch ( Exception e ) {
      e.printStackTrace();
    }
  }

  private PaloCellOutputMeta meta;

  private TableView tableViewFields;
  private Text textStepName;
  private Combo comboCube;
  private Label labelStepName;
  private Label labelCube;
  private Label labelMeasureType;
  private Combo comboMeasureType;
  private Label labelUpdateMode;
  private Combo comboUpdateMode;
  private Label labelSplashMode;
  private Combo comboSplashMode;
  private Button buttonClearFields;
  private Button buttonGetFields;
  private Button buttonOk;
  private Button buttonCancel;
  private Label labelClearCube;
  private Button buttonClearCube;
  private Label labelCommitSize;
  private Text textCommitSize;
  private Label labelPreloadDimensionCache;
  private Button buttonPreloadDimensionCache;
  private Label labelEnableDimensionCache;
  private Button buttonEnableDimensionCache;
  private CCombo addConnectionLine;
  private ColumnInfo[] colinf;

  private PaloOptionCollection splashOptions = PaloHelper.getSplasModeOptions();
  private PaloOptionCollection updateOptions = PaloHelper.getUpdateModeOptions();

  public PaloCellOutputDialog( Shell parent, Object in, TransMeta transMeta, String sname ) {
    super( parent, (BaseStepMeta) in, transMeta, sname );
    this.meta = (PaloCellOutputMeta) in;
  }

  public String open() {

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

    labelCube = new Label( shell, SWT.RIGHT );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( addConnectionLine, margin );
    labelCube.setLayoutData( fd );

    comboCube = new Combo( shell, SWT.READ_ONLY );
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( addConnectionLine, margin );
    comboCube.setLayoutData( fd );

    labelMeasureType = new Label( shell, SWT.RIGHT );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( comboCube, margin );
    labelMeasureType.setLayoutData( fd );

    comboMeasureType = new Combo( shell, SWT.READ_ONLY | SWT.FILL );
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( comboCube, margin );
    comboMeasureType.setLayoutData( fd );

    labelUpdateMode = new Label( shell, SWT.RIGHT );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( comboMeasureType, margin );
    labelUpdateMode.setLayoutData( fd );

    comboUpdateMode = new Combo( shell, SWT.READ_ONLY | SWT.FILL );
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( comboMeasureType, margin );
    comboUpdateMode.setLayoutData( fd );

    labelSplashMode = new Label( shell, SWT.RIGHT );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( comboUpdateMode, margin );
    labelSplashMode.setLayoutData( fd );

    comboSplashMode = new Combo( shell, SWT.READ_ONLY | SWT.FILL );
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( comboUpdateMode, margin );
    comboSplashMode.setLayoutData( fd );

    labelCommitSize = new Label( shell, SWT.RIGHT );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( comboSplashMode, margin );
    labelCommitSize.setLayoutData( fd );

    textCommitSize = new Text( shell, SWT.BORDER );
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( comboSplashMode, margin );
    textCommitSize.setLayoutData( fd );

    labelClearCube = new Label( shell, SWT.RIGHT );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( textCommitSize, margin );
    labelClearCube.setLayoutData( fd );

    buttonClearCube = new Button( shell, SWT.CHECK );
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( textCommitSize, margin );
    buttonClearCube.setLayoutData( fd );

    labelEnableDimensionCache = new Label( shell, SWT.RIGHT );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( buttonClearCube, margin );
    labelEnableDimensionCache.setLayoutData( fd );

    buttonEnableDimensionCache = new Button( shell, SWT.CHECK );
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( buttonClearCube, margin );
    buttonEnableDimensionCache.setLayoutData( fd );

    labelPreloadDimensionCache = new Label( shell, SWT.RIGHT );
    fd = new FormData();
    fd.left = new FormAttachment( 0, 0 );
    fd.right = new FormAttachment( middle, -margin );
    fd.top = new FormAttachment( buttonEnableDimensionCache, margin );
    labelPreloadDimensionCache.setLayoutData( fd );

    buttonPreloadDimensionCache = new Button( shell, SWT.CHECK );
    fd = new FormData();
    fd.left = new FormAttachment( middle, 0 );
    fd.right = new FormAttachment( 100, 0 );
    fd.top = new FormAttachment( buttonEnableDimensionCache, margin );
    buttonPreloadDimensionCache.setLayoutData( fd );

    ModifyListener lsMod = new ModifyListener() {
      public void modifyText( ModifyEvent e ) {
        meta.setChanged();
      }
    };

    colinf =
      new ColumnInfo[]{ new ColumnInfo( getLocalizedColumn( 0 ), ColumnInfo.COLUMN_TYPE_TEXT, false, true ),
        new ColumnInfo( getLocalizedColumn( 1 ), ColumnInfo.COLUMN_TYPE_CCOMBO, new String[]{ }, true ), };

    tableViewFields = new TableView( null, shell, SWT.NONE | SWT.BORDER, colinf, 10, true, lsMod, props );

    tableViewFields.setSize( 477, 105 );
    tableViewFields.setBounds( 5, 250, 477, 105 );
    tableViewFields.setReadonly( true );
    tableViewFields.setSortable( false );
    tableViewFields.table.removeAll();
    fd = new FormData();
    fd.left = new FormAttachment( 0, margin );
    fd.top = new FormAttachment( buttonPreloadDimensionCache, 3 * margin );
    fd.right = new FormAttachment( 100, -150 );
    fd.bottom = new FormAttachment( 100, -50 );
    tableViewFields.setLayoutData( fd );

    buttonGetFields = new Button( shell, SWT.NONE );
    fd = new FormData();
    fd.left = new FormAttachment( tableViewFields, margin );
    fd.top = new FormAttachment( buttonPreloadDimensionCache, 3 * margin );
    fd.right = new FormAttachment( 100, 0 );
    buttonGetFields.setLayoutData( fd );

    buttonClearFields = new Button( shell, SWT.NONE );
    fd = new FormData();
    fd.left = new FormAttachment( tableViewFields, margin );
    fd.top = new FormAttachment( buttonGetFields, margin );
    fd.right = new FormAttachment( 100, 0 );
    buttonClearFields.setLayoutData( fd );

    buttonOk = new Button( shell, SWT.CENTER );
    buttonCancel = new Button( shell, SWT.CENTER );
    buttonOk.setText( BaseMessages.getString( "System.Button.OK" ) );
    buttonCancel.setText( BaseMessages.getString( "System.Button.Cancel" ) );
    setButtonPositions( new Button[]{ buttonOk, buttonCancel }, margin, null );

    buttonGetFields.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        doGetFields();
      }
    } );
    buttonClearFields.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        doClearFields();

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
    addConnectionLine.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        doSelectConnection( false );
      }
    } );
    comboCube.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent e ) {
        doSelectCube();
      }
    } );
    buttonEnableDimensionCache.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( SelectionEvent arg0 ) {
        buttonPreloadDimensionCache.setEnabled( buttonEnableDimensionCache.getSelection() );
      }
    } );

    this.fillLocalizedData();
    this.fillStoredData();
    this.doSelectConnection( false );

    props.setLook( tableViewFields );
    props.setLook( textStepName );
    props.setLook( comboCube );
    props.setLook( labelStepName );
    props.setLook( labelCube );
    props.setLook( labelMeasureType );
    props.setLook( comboMeasureType );
    props.setLook( labelUpdateMode );
    props.setLook( comboUpdateMode );
    props.setLook( labelSplashMode );
    props.setLook( comboSplashMode );
    props.setLook( buttonClearFields );
    props.setLook( buttonGetFields );
    props.setLook( buttonOk );
    props.setLook( buttonCancel );
    props.setLook( addConnectionLine );
    props.setLook( buttonClearCube );
    props.setLook( labelClearCube );
    props.setLook( textCommitSize );
    props.setLook( labelCommitSize );
    props.setLook( labelPreloadDimensionCache );
    props.setLook( buttonPreloadDimensionCache );
    props.setLook( labelEnableDimensionCache );
    props.setLook( buttonEnableDimensionCache );

    shell.addShellListener( new ShellAdapter() {
      public void shellClosed( ShellEvent e ) {
        cancel();
      }
    } );
    meta.setChanged( changed );
    setSize();
    shell.open();

    PaloCellInputDialog.showPaloLibWarningDialog( shell );

    while ( !shell.isDisposed() ) {
      if ( !display.readAndDispatch() ) {
        display.sleep();
      }
    }
    return stepname;
  }

  private String getLocalizedColumn( int columnIndex ) {
    switch ( columnIndex ) {
      case 0:
        return BaseMessages.getString( PKG, "PaloCellOutputDialog.ColumnDimension" );
      case 1:
        return BaseMessages.getString( PKG, "PaloCellOutputDialog.ColumnField" );
      case 2:
        return BaseMessages.getString( PKG, "PaloCellOutputDialog.ColumnType" );
      default:
        return "";
    }
  }

  private void fillLocalizedData() {
    labelStepName.setText( BaseMessages.getString( PKG, "PaloCellOutputDialog.StepName" ) );
    shell.setText( BaseMessages.getString( PKG, "PaloCellOutputDialog.PaloCellOutput" ) );
    buttonGetFields.setText( BaseMessages.getString( PKG, "PaloCellOutputDialog.GetFields" ) );
    buttonClearFields.setText( BaseMessages.getString( PKG, "PaloCellOutputDialog.ClearFields" ) );
    labelCube.setText( BaseMessages.getString( PKG, "PaloCellOutputDialog.SelectCube" ) );
    labelMeasureType.setText( BaseMessages.getString( PKG, "PaloCellOutputDialog.SelectMeasureType" ) );
    labelUpdateMode.setText( BaseMessages.getString( PKG, "PaloCellOutputDialog.UpdateMode" ) );
    labelSplashMode.setText( BaseMessages.getString( PKG, "PaloCellOutputDialog.SplashMode" ) );
    labelClearCube.setText( BaseMessages.getString( PKG, "PaloCellOutputDialog.ClearCube" ) );
    labelCommitSize.setText( BaseMessages.getString( PKG, "PaloCellOutputDialog.CommitSize" ) );
    labelPreloadDimensionCache.setText( BaseMessages.getString( PKG, "PaloCellOutputDialog.PreloadDimensionCache" ) );
    labelEnableDimensionCache.setText( BaseMessages.getString( PKG, "PaloCellOutputDialog.EnableDimensionCache" ) );

    for ( PaloOption option : updateOptions ) {
      option.setDescription( BaseMessages.getString( PKG, "PaloCellOutputDialog.UpdateOptions." + option.getCode() ) );
    }

    for ( PaloOption option : splashOptions ) {
      option.setDescription( BaseMessages.getString( PKG, "PaloCellOutputDialog.SplashOptions." + option.getCode() ) );
    }

  }

  private void fillStoredData() {
    if ( stepname != null ) {
      textStepName.setText( stepname );
    }

    int index = addConnectionLine.indexOf( meta.getDatabaseMeta() != null ? meta.getDatabaseMeta().getName() : "" );
    if ( index >= 0 ) {
      addConnectionLine.select( index );
    }

    if ( meta.getCube() != null ) {
      comboCube.add( meta.getCube() );
      comboCube.select( 0 );
    }

    for ( PaloOption option : updateOptions ) {
      comboUpdateMode.add( option.getDescription() );
    }
    comboUpdateMode.select( comboUpdateMode.indexOf( this.updateOptions.getDescription( meta.getUpdateMode() ) ) );

    for ( PaloOption option : splashOptions ) {
      comboSplashMode.add( option.getDescription() );
    }
    comboSplashMode.select( comboSplashMode.indexOf( this.splashOptions.getDescription( meta.getSplashMode() ) ) );

    textCommitSize.setText( String.valueOf( meta.getCommitSize() ) );
    buttonEnableDimensionCache.setSelection( meta.getEnableDimensionCache() );
    buttonPreloadDimensionCache.setSelection( meta.getPreloadDimensionCache() );
    buttonPreloadDimensionCache.setEnabled( buttonEnableDimensionCache.getSelection() );

    comboMeasureType.setItems( new String[]{ "Numeric", "String" } );
    comboMeasureType.select( 0 );
    if ( meta.getMeasureType() != null ) {
      int indexType = comboMeasureType.indexOf( meta.getMeasureType() );
      if ( indexType >= 0 ) {
        comboMeasureType.select( indexType );
      }
    }

    tableViewFields.table.removeAll();

    if ( meta.getFields().size() > 0 ) {
      for ( DimensionField level : meta.getFields() ) {
        tableViewFields.add( level.getDimensionName(), level.getFieldName() );
      }
    }

    List<String> fieldNameList = null;
    try {
      RowMetaInterface r = transMeta.getPrevStepFields( stepname );
      fieldNameList = Arrays.asList( r.getFieldNames() );
      Collections.sort( fieldNameList );
    } catch ( Exception e ) {
      // ignore
    }
    tableViewFields.setColumnInfo( 1, new ColumnInfo( "Field", ColumnInfo.COLUMN_TYPE_CCOMBO, ( fieldNameList == null
      ? null : fieldNameList.toArray( new String[0] ) ), true ) );

    if ( meta.getMeasure() != null ) {
      final TableItem item = new TableItem( tableViewFields.table, SWT.NONE );
      item.setText( 1, meta.getMeasure().getDimensionName() );
      item.setText( 2, meta.getMeasure().getFieldName() );
      // item.setText(3,meta.getMeasure().getFieldType());
      item.setForeground( Display.getCurrent().getSystemColor( SWT.COLOR_DARK_GREEN ) );
    }
    tableViewFields.setRowNums();
    tableViewFields.optWidth( true );

    buttonClearCube.setSelection( meta.getClearCube() );

  }

  private void doSelectConnection( boolean clearCurrentData ) {
    try {
      if ( clearCurrentData ) {
        tableViewFields.table.removeAll();
        comboCube.removeAll();
      }

      if ( addConnectionLine.getText() != null ) {
        DatabaseMeta dbMeta = transMeta.findDatabase( addConnectionLine.getText() );
        if ( dbMeta != null ) {
          PaloCellOutputData data = new PaloCellOutputData( dbMeta );
          data.helper.connect();
          List<String> cubes = data.helper.getCubesNames();
          Collections.sort( cubes, new PaloNameComparator() );
          for ( String cubeName : cubes ) {
            if ( comboCube.indexOf( cubeName ) == -1 ) {
              comboCube.add( cubeName );
            }
          }
          data.helper.disconnect();
        }
      }
    } catch ( Exception ex ) {
      new ErrorDialog( shell, BaseMessages.getString( PKG, "PaloCellOutputDialog.RetreiveCubesErrorTitle" ),
        BaseMessages.getString( PKG, "PaloCellOutputDialog.RetreiveCubesError" ), ex );
    }
  }

  private void fillPreviousFieldTableViewColumn() throws KettleException {
    RowMetaInterface r = transMeta.getPrevStepFields( stepname );
    if ( r != null ) {
      List<String> fieldNameList = Arrays.asList( r.getFieldNames() );
      Collections.sort( fieldNameList );
      colinf[1] =
        new ColumnInfo( getLocalizedColumn( 1 ), ColumnInfo.COLUMN_TYPE_CCOMBO,
          fieldNameList.toArray( new String[0] ), true );
    }
  }

  private void doGetFields() {
    try {
      List<String> cubeDimensions = null;
      if ( comboCube.getText() != null && comboCube.getText() != "" ) {
        if ( addConnectionLine.getText() != null ) {
          DatabaseMeta dbMeta = transMeta.findDatabase( addConnectionLine.getText() );
          if ( dbMeta != null ) {
            PaloCellOutputData data = new PaloCellOutputData( dbMeta );
            data.helper.connect();
            cubeDimensions = data.helper.getCubeDimensions( comboCube.getText() );
            data.helper.disconnect();
          }
        }
        tableViewFields.table.removeAll();

        for ( int i = 0; i < cubeDimensions.size(); i++ ) {
          final TableItem item = new TableItem( tableViewFields.table, SWT.NONE );
          item.setText( 1, cubeDimensions.get( i ) );
          // item.setText(3, "String");

        }
        final TableItem item = new TableItem( tableViewFields.table, SWT.NONE );
        item.setText( 1, "Cube Measure" );
        item.setForeground( Display.getCurrent().getSystemColor( SWT.COLOR_DARK_GREEN ) );

        tableViewFields.removeEmptyRows();
        tableViewFields.setRowNums();
        tableViewFields.optWidth( true );
        tableViewFields.setReadonly( true );

      } else {
        new ErrorDialog(
          shell,
          BaseMessages.getString( PKG, "System.Dialog.GetFieldsFailed.Title" ),
          BaseMessages.getString( PKG, "System.Dialog.GetFieldsFailed.Message" ),
          new Exception( BaseMessages.getString( PKG, "PaloCellOutputDialog.SelectCubeFirstError" ) ) );
      }

      this.fillPreviousFieldTableViewColumn();

    } catch ( KettleException ke ) {
      new ErrorDialog(
        shell,
        BaseMessages.getString( PKG, "System.Dialog.GetFieldsFailed.Title" ),
        BaseMessages.getString( PKG, "System.Dialog.GetFieldsFailed.Message" ),
        ke );
    }
  }

  private void doClearFields() {
    tableViewFields.table.removeAll();
  }

  private void doSelectCube() {
    // tableViewFields.table.removeAll();
  }

  private void cancel() {
    stepname = null;
    meta.setChanged( changed );
    dispose();
  }

  private void ok() {
    try {
      getInfo( this.meta );
      dispose();
    } catch ( KettleException e ) {
      new ErrorDialog(
        shell,
        BaseMessages.getString( PKG, "PaloCellOutputDialog.FailedToSaveDataErrorTitle" ),
        BaseMessages.getString( PKG, "PaloCellOutputDialog.FailedToSaveDataError" ),
        e );
    }
  }

  private void getInfo( PaloCellOutputMeta myMeta ) throws KettleException {
    stepname = textStepName.getText();
    List<DimensionField> fields = new ArrayList<DimensionField>();

    if ( this.updateOptions.getCode( comboUpdateMode.getText() ) == "ADD"
      && this.splashOptions.getCode( comboSplashMode.getText() ) == "SET" ) {
      throw new KettleException(
        BaseMessages.getString( PKG, "PaloCellOutputDialog.UpdateSplashError",
          BaseMessages.getString( PKG, "PaloCellOutputDialog.UpdateMode" ),
          comboUpdateMode.getText(),
          BaseMessages.getString( PKG, "PaloCellOutputDialog.SplashMode" ),
          comboSplashMode.getText()
        )
      );
    }

    try {
      Integer.parseInt( this.textCommitSize.getText() );
    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString( PKG, "PaloCellOutputDialog.CommitSizeErrorMessage" ) );
    }

    for ( int i = 0; i < tableViewFields.table.getItemCount(); i++ ) {

      DimensionField field =
        new DimensionField( tableViewFields.table.getItem( i ).getText( 1 ), tableViewFields.table.getItem( i )
          .getText( 2 ), ""// tableViewFields.table.getItem(i).getText(3)
        );

      if ( i != tableViewFields.table.getItemCount() - 1 ) {
        // if(tableViewFields.table.getItem(i).getText(3)!="String")
        // throw new
        // KettleException("Dimension input field must be from String type");
        fields.add( field );
      } else {
        myMeta.setMeasureField( field );
      }
    }

    myMeta.setCube( this.comboCube.getText() );
    myMeta.setMeasureType( this.comboMeasureType.getText() );
    myMeta.setUpdateMode( this.updateOptions.getCode( comboUpdateMode.getText() ) );
    myMeta.setSplashMode( this.splashOptions.getCode( comboSplashMode.getText() ) );
    myMeta.setLevels( fields );
    myMeta.setClearCube( this.buttonClearCube.getSelection() );
    myMeta.setDatabaseMeta( transMeta.findDatabase( addConnectionLine.getText() ) );
    myMeta.setCommitSize( Integer.parseInt( this.textCommitSize.getText() ) );
    myMeta.setEnableDimensionCache( this.buttonEnableDimensionCache.getSelection() );
    if ( this.buttonEnableDimensionCache.getSelection() ) {
      myMeta.setPreloadDimensionCache( this.buttonPreloadDimensionCache.getSelection() );
    } else {
      myMeta.setPreloadDimensionCache( false );
    }
    myMeta.setChanged( true );

  }
}
