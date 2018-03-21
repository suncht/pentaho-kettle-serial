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

package org.pentaho.di.ui.trans.step;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.SourceToTargetMapping;
import org.pentaho.di.core.database.DatabaseInterface;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LoggingObjectInterface;
import org.pentaho.di.core.logging.LoggingObjectType;
import org.pentaho.di.core.logging.SimpleLoggingObject;
import org.pentaho.di.core.plugins.PluginInterface;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.plugins.StepPluginType;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.laf.BasePropertyHandler;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.database.dialog.DatabaseDialog;
import org.pentaho.di.ui.core.database.wizard.CreateDatabaseWizard;
import org.pentaho.di.ui.core.dialog.EnterMappingDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.dialog.ShowBrowserDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.core.widget.ComboVar;
import org.pentaho.di.ui.core.widget.TableView;
import org.pentaho.metastore.api.IMetaStore;

/**
 * This class provides functionality common to Step Dialogs.
 */
public class BaseStepDialog extends Dialog {
  
  /** The package name used for internationalization */
  private static Class<?> PKG = StepInterface.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

  /** The logging object interface for this dialog. */
  public static final LoggingObjectInterface loggingObject = new SimpleLoggingObject("Step dialog", LoggingObjectType.STEPDIALOG, null);

  /** The variable bindings for this dialog. */
  protected static VariableSpace variables = new Variables();

  /** The step name. */
  protected String stepname;

  /** The Step name label. */
  protected Label wlStepname;

  /** The Step name UI component. */
  protected Text wStepname;

  /** The FormData for the step name and its label. */
  protected FormData fdlStepname, fdStepname;

  /** Common dialog buttons. */
  protected Button wOK, wGet, wPreview, wSQL, wCreate, wCancel;

  /** FormData for the common dialog buttons. */
  protected FormData fdOK, fdGet, fdPreview, fdSQL, fdCreate, fdCancel;

  /** Listeners for the common dialog buttons. */
  protected Listener lsOK, lsGet, lsPreview, lsSQL, lsCreate, lsCancel;

  /** The metadata for the associated transformation. */
  protected TransMeta transMeta;

  /** A reference to the shell. */
  protected Shell shell;

  /** A listener adapter for default widget selection. */
  protected SelectionAdapter lsDef;

  /** A listener for dialog resizing. */
  protected Listener lsResize;

  /** Whether the dialog (and its backup) have changed. */
  protected boolean changed, backupChanged;

  /** The base step meta. */
  protected StepMetaInterface baseStepMeta;

  /** The UI properties. */
  protected PropsUI props;

  /** The associated repository. */
  protected Repository repository;
  
  /** The MetaStore to use */
  protected IMetaStore metaStore;

  /** The step meta for this dialog. */
  protected StepMeta stepMeta;

  /** The log channel for this dialog. */
  protected LogChannel log;
  
  /** A constant indicating a center button alignment. */
  protected static final int BUTTON_ALIGNMENT_CENTER = 0;

  /** A constant indicating a left button alignment. */
  protected static final int BUTTON_ALIGNMENT_LEFT = 1;

  /** A constant indicating a right button alignment. */
  protected static final int BUTTON_ALIGNMENT_RIGHT = 2;

  /** The button alignment (defaults to center). */
  protected static int buttonAlignment = BUTTON_ALIGNMENT_CENTER;
  
  /** A reference to a database dialog. */
  protected DatabaseDialog databaseDialog;
  
  static {
    // Get the button alignment
    buttonAlignment = getButtonAlignment();
  }

  /**
   * Instantiates a new base step dialog.
   *
   * @param parent the parent shell
   * @param baseStepMeta the associated base step metadata
   * @param transMeta the associated transformation metadata
   * @param stepname the step name
   */
  public BaseStepDialog(Shell parent, BaseStepMeta baseStepMeta, TransMeta transMeta, String stepname) {
    super(parent, SWT.NONE);

    this.log = new LogChannel(baseStepMeta);
    this.transMeta = transMeta;
    this.stepname = stepname;
    this.stepMeta = transMeta.findStep(stepname);
    this.baseStepMeta = (StepMetaInterface) baseStepMeta;
    this.backupChanged = baseStepMeta.hasChanged();
    this.props = PropsUI.getInstance();
  }

  /**
   * Instantiates a new base step dialog.
   *
   * @param parent the parent shell
   * @param baseStepMeta the associated base step metadata
   * @param transMeta the associated transformation metadata
   * @param stepname the step name
   */
  public BaseStepDialog(Shell parent, StepMetaInterface baseStepMeta, TransMeta transMeta, String stepname){
     super(parent, SWT.NONE);

    this.log = new LogChannel(baseStepMeta);
    this.transMeta = transMeta;
    this.stepname = stepname;
    this.stepMeta = transMeta.findStep(stepname);
    this.baseStepMeta = baseStepMeta;
    this.backupChanged = baseStepMeta.hasChanged();
    this.props = PropsUI.getInstance();
  }
  
  /**
   * Instantiates a new base step dialog.
   *
   * @param parent the parent shell
   * @param nr the number of rows
   * @param in the base step metadata
   * @param tr the transformation metadata
   */
  public BaseStepDialog(Shell parent, int nr, BaseStepMeta in, TransMeta tr) {
    this(parent, in, tr, null);
  }

  /**
   * Sets the shell image.
   *
   * @param shell the shell
   * @param stepMetaInterface the step meta interface
   */
  public void setShellImage(Shell shell, StepMetaInterface stepMetaInterface) {
    try {
      final PluginInterface plugin = PluginRegistry.getInstance().getPlugin(StepPluginType.class, stepMeta.getStepMetaInterface());
      if (!Const.isEmpty(plugin.getDocumentationUrl())) {
        createHelpButton(shell, stepMeta, plugin);
      }

      String id = plugin.getIds()[0];
      if (id != null) {
        shell.setImage(GUIResource.getInstance().getImagesSteps().get(id));
      }
    } catch (Throwable e) {
      // Ignore
    }
  }

  /**
   * Dispose this dialog.
   */
  public void dispose() {
    WindowProperty winprop = new WindowProperty(shell);
    props.setScreen(winprop);
    shell.dispose();
  }

  /**
   * Set the shell size, based upon the previous time the geometry was saved in the Properties file.
   */
  public void setSize() {
    setSize(shell);
  }

  /**
   * Sets the button positions.
   *
   * @param buttons the buttons
   * @param margin the margin between buttons
   * @param lastControl the last control
   */
  protected void setButtonPositions(Button buttons[], int margin, Control lastControl) {
    BaseStepDialog.positionBottomButtons(shell, buttons, margin, lastControl);
  }

  /**
   * Position the specified buttons at the bottom of the parent composite.
   * Also, make the buttons all the same width: the width of the largest button.
   * <P>
   * The default alignment for buttons in the system will be used. This is set as an LAF
   * property with the key <code>Button_Position</code> and has the valid values
   * of <code>left, center, right</code> with <code>center</code> being the default.
   * 
   * @param composite the composite
   * @param buttons The buttons to position.
   * @param margin The margin between the buttons in pixels
   * @param lastControl the last control
   */
  public static final void positionBottomButtons(Composite composite, Button buttons[], int margin, Control lastControl) {
    // Determine the largest button in the array
    Rectangle largest = null;
    for (int i = 0; i < buttons.length; i++) {
      buttons[i].pack(true);
      Rectangle r = buttons[i].getBounds();
      if (largest == null || r.width > largest.width)
        largest = r;

      // Also, set the tooltip the same as the name if we don't have one...
      if (buttons[i].getToolTipText() == null) {
        buttons[i].setToolTipText(Const.replace(buttons[i].getText(), "&", ""));  
      }
    }

    // Make buttons a bit larger... (nicer)
    largest.width += 10;
    if ((largest.width % 2) == 1)
      largest.width++;

    // Compute the left side of the 1st button (based on the system button alignment)
    switch (buttonAlignment) {
      case BUTTON_ALIGNMENT_CENTER:
        centerButtons(buttons, largest.width, margin, lastControl);
        break;
      case BUTTON_ALIGNMENT_LEFT:
        leftAlignButtons(buttons, largest.width, margin, lastControl);
        break;
      case BUTTON_ALIGNMENT_RIGHT:
        rightAlignButtons(buttons, largest.width, margin, lastControl);
        break;
      default:
        break;
    }
    if (Const.isOSX())
    {
        Shell parentShell=composite.getShell();
        final List<TableView> tableViews = new ArrayList<TableView>();
        getTableViews(parentShell, tableViews);
        for (final Button button : buttons) {
        	// We know the table views
        	// We also know that if a button is hit, the table loses focus
        	// In that case, we can apply the content of an open text editor...
        	//
        	button.addSelectionListener(new SelectionAdapter() {
			
				public void widgetSelected(SelectionEvent e) {
					for (TableView view : tableViews)
					{
						view.applyOSXChanges();
					}
				}
			});
        }
    }
  }
  
  /**
   * Gets the table views.
   *
   * @param parentControl the parent control
   * @param tableViews the table views
   * @return the table views
   */
  private static final void getTableViews(Control parentControl, List<TableView> tableViews)
  {
	if (parentControl instanceof TableView) 
	{
		tableViews.add((TableView) parentControl);
	}
	else
	{
		if (parentControl instanceof Composite)
		{
			Control[] children = ((Composite)parentControl).getChildren();
			for (Control child : children)
			{
				getTableViews(child, tableViews);
			}
		}
		else
		{
			if (parentControl instanceof Shell)
			{
				Control[] children = ((Shell)parentControl).getChildren();
				for (Control child : children)
				{
					getTableViews(child, tableViews);
				}
				
			}
		}
	}
  }

  /**
   * Returns the default alignment for the buttons. This is set in the 
   * LAF properties with the key <code>Button_Position</code>.
   * The valid values are:<UL>
   * <LI><code>left</code>
   * <LI><code>center</code>
   * <LI><code>right</code>
   * </UL>
   * NOTE: if the alignment is not provided or contains an invalid value, <code>center</code>
   * will be used as a default
   * @return a constant which indicates the button alignment
   */
  protected static int getButtonAlignment() {
    String buttonAlign = BasePropertyHandler.getProperty("Button_Position", "center").toLowerCase();  
    if ("center".equals(buttonAlign)) { 
      return BUTTON_ALIGNMENT_CENTER;
    } else if ("left".equals(buttonAlign)) { 
      return BUTTON_ALIGNMENT_LEFT;
    } else {
      return BUTTON_ALIGNMENT_RIGHT;
    }
  }

  /**
   * Creats a default FormData object with the top / bottom / and left set (this is done to 
   * cut down on repetative code lines.
   *
   * @param button the button to which this form data will be applied
   * @param width the width of the button
   * @param margin the margin between buttons
   * @param lastControl the last control above the buttons
   * @return the newly created FormData object
   */
  private static FormData createDefaultFormData(Button button, int width, int margin, Control lastControl) {
    FormData formData = new FormData();
    if (lastControl != null) {
      formData.top = new FormAttachment(lastControl, margin * 3);
    } else {
      formData.bottom = new FormAttachment(100, 0);
    }
    formData.right = new FormAttachment(button, width + margin);
    return formData;
  }

  /**
   * Aligns the buttons as left-aligned on the dialog.
   *
   * @param buttons the array of buttons to align
   * @param width the standardized width of all the buttons  
   * @param margin the margin between buttons
   * @param lastControl (optional) the bottom most control used for aligning the buttons relative
   *   to the bottom of the controls on the dialog
   */
  protected static void leftAlignButtons(Button[] buttons, int width, int margin, Control lastControl) {
    for (int i = 0; i < buttons.length; ++i) {
      FormData formData = createDefaultFormData(buttons[i], width, margin, lastControl);

      // Set the left side of the buttons (either offset from the edge, or relative to the previous button)
      if (i == 0) {
        formData.left = new FormAttachment(0, margin);
      } else {
        formData.left = new FormAttachment(buttons[i - 1], margin);
      }

      // Apply the layout data
      buttons[i].setLayoutData(formData);
    }
  }

  /**
   * Aligns the buttons as right-aligned on the dialog.
   *
   * @param buttons the array of buttons to align
   * @param width the standardized width of all the buttons  
   * @param margin the margin between buttons
   * @param lastControl (optional) the bottom most control used for aligning the buttons relative
   *   to the bottom of the controls on the dialog
   */
  protected static void rightAlignButtons(Button[] buttons, int width, int margin, Control lastControl) {
    for (int i = buttons.length - 1; i >= 0; --i) {
      FormData formData = createDefaultFormData(buttons[i], width, margin, lastControl);

      // Set the right side of the buttons (either offset from the edge, or relative to the previous button)
      if (i == buttons.length - 1) {
        formData.left = new FormAttachment(100, -(width + margin));
      } else {
        formData.left = new FormAttachment(buttons[i + 1], -(2 * (width + margin)) - margin);
      }

      // Apply the layout data
      buttons[i].setLayoutData(formData);
    }
  }

  /**
   * Aligns the buttons as centered on the dialog.
   *
   * @param buttons the array of buttons to align
   * @param width the standardized width of all the buttons  
   * @param margin the margin between buttons
   * @param lastControl (optional) the bottom most control used for aligning the buttons relative
   *   to the bottom of the controls on the dialog
   */
  protected static void centerButtons(Button[] buttons, int width, int margin, Control lastControl) {
    // Setup the middle button
    int middleButtonIndex = buttons.length / 2;
    FormData formData = createDefaultFormData(buttons[middleButtonIndex], width, margin, lastControl);

    // See if we have an even or odd number of buttons...
    int leftOffset = 0;
    if (buttons.length % 2 == 0) {
      // Even number of buttons - the middle is between buttons. The "middle" button is 
      // actually to the right of middle
      leftOffset = margin;
    } else {
      // Odd number of buttons - tht middle is in the middle of the button
      leftOffset = -(width + margin) / 2;
    }
    formData.left = new FormAttachment(50, leftOffset);
    buttons[middleButtonIndex].setLayoutData(formData);

    // Do the buttons to the right of the middle
    for (int i = middleButtonIndex + 1; i < buttons.length; ++i) {
      formData = createDefaultFormData(buttons[i], width, margin, lastControl);
      formData.left = new FormAttachment(buttons[i - 1], margin);
      buttons[i].setLayoutData(formData);
    }

    // Do the buttons to the left of the middle
    for (int i = middleButtonIndex - 1; i >= 0; --i) {
      formData = createDefaultFormData(buttons[i], width, margin, lastControl);
      formData.left = new FormAttachment(buttons[i + 1], -(2 * (width + margin)) - margin);
      buttons[i].setLayoutData(formData);
    }
  }

  /**
   * Gets the modify listener tooltip text.
   *
   * @param textField the text field
   * @return the modify listener tooltip text
   */
  public static final ModifyListener getModifyListenerTooltipText(final Text textField) {
    return new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        // maybe replace this with extra arguments
        textField.setToolTipText(variables.environmentSubstitute(textField.getText()));
      }
    };
  }

  /**
   * Adds the databases to the Combo Box component.
   *
   * @param wConnection the Combo Box component
   */
  public void addDatabases(CCombo wConnection) {
	  addDatabases(wConnection, null);
  }

  /**
   * Adds the databases with the specified type to the Combo Box component.
   *
   * @param wConnection the Combo Box component
   * @param databaseType the database type
   */
  public void addDatabases(CCombo wConnection, Class<? extends DatabaseInterface> databaseType) {
    for (int i = 0; i < transMeta.nrDatabases(); i++) {
      DatabaseMeta ci = transMeta.getDatabase(i);
      if (databaseType==null || ci.getDatabaseInterface().getClass().equals(databaseType))
      {
    	  wConnection.add(ci.getName());
      }
    }
  }

  /**
   * Selects the database with the specified name in the Combo Box component.
   *
   * @param wConnection the Combo Box component
   * @param name the name of the database to select
   */
  public void selectDatabase(CCombo wConnection, String name) {
    int idx = wConnection.indexOf(name);
    if (idx >= 0) {
      wConnection.select(idx);
    }
  }

  /**
   * Adds the connection line.
   *
   * @param parent the parent UI component
   * @param previous the previous UI component
   * @param middle the middle
   * @param margin the margin
   * @return the the Combo Box component for the given parameters
   */
  public CCombo addConnectionLine(Composite parent, Control previous, int middle, int margin) {
	  return addConnectionLine(parent, previous, middle, margin, null);
	  }

  /**
   * Adds the connection line.
   *
   * @param parent the parent UI component
   * @param previous the previous UI component
   * @param middle the middle
   * @param margin the margin
   * @param databaseType the database type
   * @return the Combo Box component for the given parameters
   */
  public CCombo addConnectionLine(Composite parent, Control previous, int middle, int margin, Class<? extends DatabaseInterface> databaseType) {
    return addConnectionLine(parent, previous, middle, margin, new Label(parent, SWT.RIGHT), new Button(parent,
            SWT.PUSH), new Button(parent, SWT.PUSH), new Button(parent, SWT.PUSH), databaseType);
  }

  /**
   * Adds the connection line.
   *
   * @param parent the parent UI component
   * @param previous the previous UI component
   * @param middle the middle
   * @param margin the margin
   * @param wlConnection the connection label
   * @param wbnConnection the "new connection" button
   * @param wbeConnection the "edit connection" button
   * @return the Combo Box component for the given parameters
   */
  public CCombo addConnectionLine(Composite parent, Control previous, int middle, int margin, final Label wlConnection,
		  final Button wbwConnection, final Button wbnConnection, final Button wbeConnection) {
	  return addConnectionLine(parent, previous, middle, margin, wlConnection, wbwConnection, wbnConnection, wbeConnection, null);
  }
  
  /**
   * Adds the connection line.
   *
   * @param parent the parent UI component
   * @param previous the previous UI component
   * @param middle the middle
   * @param margin the margin
   * @param wlConnection the connection label
   * @param wbnConnection the "new connection" button
   * @param wbeConnection the "edit connection" button
   * @param databaseType the database type
   * @return the Combo Box component for the given parameters
   */
  public CCombo addConnectionLine(Composite parent, Control previous, int middle, int margin, final Label wlConnection,
		  final Button wbwConnection , final Button wbnConnection, final Button wbeConnection, final Class<? extends DatabaseInterface> databaseType) {
    final CCombo wConnection;
    final FormData fdlConnection, fdbConnection, fdeConnection, fdConnection, fdbwConnection;

    wConnection = new CCombo(parent, SWT.BORDER | SWT.READ_ONLY);
    props.setLook(wConnection);

    addDatabases(wConnection);

    wlConnection.setText(BaseMessages.getString(PKG, "BaseStepDialog.Connection.Label")); 
    props.setLook(wlConnection);
    fdlConnection = new FormData();
    fdlConnection.left = new FormAttachment(0, 0);
    fdlConnection.right = new FormAttachment(middle, -margin);
    if (previous != null)
      fdlConnection.top = new FormAttachment(previous, margin);
    else
      fdlConnection.top = new FormAttachment(0, 0);
    wlConnection.setLayoutData(fdlConnection);

    // 
    // Wizard button
    //
    wbwConnection.setText(BaseMessages.getString(PKG, "BaseStepDialog.WizardConnectionButton.Label")); 
    wbwConnection.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
    	CreateDatabaseWizard cdw = new CreateDatabaseWizard();
    	DatabaseMeta newDBInfo = cdw.createAndRunDatabaseWizard(shell, props, transMeta.getDatabases());
    	if (newDBInfo != null) {
             transMeta.addDatabase(newDBInfo);
             wConnection.removeAll();
             addDatabases(wConnection);
             selectDatabase(wConnection, newDBInfo.getName());
           }
      }
    });
    fdbwConnection = new FormData();
    fdbwConnection.right = new FormAttachment(100, 0);
    if (previous != null)
      fdbwConnection.top = new FormAttachment(previous, margin);
    else
      fdbwConnection.top = new FormAttachment(0, 0);
    wbwConnection.setLayoutData(fdbwConnection);

    
    // 
    // NEW button
    //
    wbnConnection.setText(BaseMessages.getString(PKG, "BaseStepDialog.NewConnectionButton.Label")); 
    wbnConnection.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        DatabaseMeta databaseMeta = new DatabaseMeta();
        databaseMeta.shareVariablesWith(transMeta);
        DatabaseDialog cid = getDatabaseDialog(shell);
        cid.setDatabaseMeta(databaseMeta);
        cid.setModalDialog(true);
        if (cid.open() != null) {
          transMeta.addDatabase(databaseMeta);
          wConnection.removeAll();
          addDatabases(wConnection, databaseType);
          selectDatabase(wConnection, databaseMeta.getName());
        }
      }
    });
    fdbConnection = new FormData();
    fdbConnection.right = new FormAttachment(wbwConnection, -margin);
    if (previous != null)
      fdbConnection.top = new FormAttachment(previous, margin);
    else
      fdbConnection.top = new FormAttachment(0, 0);
    wbnConnection.setLayoutData(fdbConnection);

    //
    // Edit button
    //
    wbeConnection.setText(BaseMessages.getString(PKG, "BaseStepDialog.EditConnectionButton.Label")); 
    wbeConnection.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        DatabaseMeta databaseMeta = transMeta.findDatabase(wConnection.getText());
        if (databaseMeta != null) {
          databaseMeta.shareVariablesWith(transMeta);
          
          DatabaseDialog cid = getDatabaseDialog(shell);
          cid.setDatabaseMeta(databaseMeta);
          cid.setModalDialog(true);
          if (cid.open() != null) {
            wConnection.removeAll();
            addDatabases(wConnection);
            selectDatabase(wConnection, databaseMeta.getName());
          }
        }
      }
    });
    fdeConnection = new FormData();
    fdeConnection.right = new FormAttachment(wbnConnection, -margin);
    if (previous != null)
      fdeConnection.top = new FormAttachment(previous, margin);
    else
      fdeConnection.top = new FormAttachment(0, 0);
    wbeConnection.setLayoutData(fdeConnection);

    //
    // what's left of the line: combo box
    //
    fdConnection = new FormData();
    fdConnection.left = new FormAttachment(middle, 0);
    if (previous != null)
      fdConnection.top = new FormAttachment(previous, margin);
    else
      fdConnection.top = new FormAttachment(0, 0);
    fdConnection.right = new FormAttachment(wbeConnection, -margin);
    wConnection.setLayoutData(fdConnection);

    return wConnection;
  }
  
  /**
   * Gets the database dialog.
   *
   * @param shell the shell
   * @return the database dialog
   */
  protected DatabaseDialog getDatabaseDialog(Shell shell){
    if(databaseDialog == null){
      databaseDialog = new DatabaseDialog(shell);
    }
    return databaseDialog;
  }

  /**
   * Store screen size.
   */
  public void storeScreenSize() {
    props.setScreen(new WindowProperty(shell));
  }

  public String toString() {
    return this.getClass().getName();
  }

  /**
   * Gets the repository associated with this dialog.
   *
   * @return Returns the repository.
   */
  public Repository getRepository() {
    return repository;
  }

  /**
   * Sets the repository associated with this dialog.
   *
   * @param repository The repository to set.
   */
  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  /**
   * Sets the minimal shell height.
   *
   * @param shell the shell
   * @param controls the controls to measure
   * @param margin the margin between the components
   * @param extra the extra padding
   */
  public static void setMinimalShellHeight(Shell shell, Control[] controls, int margin, int extra) {
    int height = 0;

    for (int i = 0; i < controls.length; i++) {
      Rectangle bounds = controls[i].getBounds();
      height += bounds.height + margin;
    }
    height += extra;
    shell.setSize(shell.getBounds().width, height);
  }

  /**
   * Sets the size of this dialog with respect to the given shell.
   *
   * @param shell the new size
   */
  public static void setSize(Shell shell) {
    setSize(shell, -1, -1, true);
  }

  /**
   * Sets the size of this dialog with respect to the given parameters.
   *
   * @param shell the shell
   * @param minWidth the minimum width
   * @param minHeight the minimum height
   * @param packIt true to pack the dialog components, false otherwise
   */
  public static void setSize(Shell shell, int minWidth, int minHeight, boolean packIt) {
    PropsUI props = PropsUI.getInstance();

    WindowProperty winprop = props.getScreen(shell.getText());
    if (winprop != null) {
      winprop.setShell(shell, minWidth, minHeight);
    } else {
      if (packIt)
        shell.pack();
      else
        shell.layout();

      // OK, sometimes this produces dialogs that are waay too big.
      // Try to limit this a bit, m'kay?
      // Use the same algorithm by cheating :-)
      //
      winprop = new WindowProperty(shell);
      winprop.setShell(shell, minWidth, minHeight);

      // Now, as this is the first time it gets opened, try to put it in the middle of the screen...
      Rectangle shellBounds = shell.getBounds();
      Monitor monitor = shell.getDisplay().getPrimaryMonitor();
      if (shell.getParent() != null)
      {
          monitor = shell.getParent().getMonitor();
      }
      Rectangle monitorClientArea = monitor.getClientArea();

      int middleX = monitorClientArea.x + (monitorClientArea.width - shellBounds.width) / 2;
      int middleY = monitorClientArea.y + (monitorClientArea.height - shellBounds.height) / 2;

      shell.setLocation(middleX, middleY);
    }
  }

  /**
   * Sets the traverse order for the given controls.
   *
   * @param controls the new traverse order
   */
  public static final void setTraverseOrder(final Control[] controls) {
    for (int i = 0; i < controls.length; i++) {
      final int controlNr = i;
      if (i < controls.length - 1) {
        controls[i].addTraverseListener(new TraverseListener() {
          public void keyTraversed(TraverseEvent te) {
            te.doit = false;
            // set focus on the next control.
            // What is the next control?
            int thisOne = controlNr + 1;
            while (!controls[thisOne].isEnabled()) {
              thisOne++;
              if (thisOne >= controls.length)
                thisOne = 0;
              if (thisOne == controlNr)
                return; // already tried all others, time to quit.
            }
            controls[thisOne].setFocus();
          }
        });
      } else // Link last item to first.
      {
        controls[i].addTraverseListener(new TraverseListener() {
          public void keyTraversed(TraverseEvent te) {
            te.doit = false;
            // set focus on the next control.
            // set focus on the next control.
            // What is the next control : 0
            int thisOne = 0;
            while (!controls[thisOne].isEnabled()) {
              thisOne++;
              if (thisOne >= controls.length)
                return; // already tried all others, time to quit.
            }
            controls[thisOne].setFocus();
          }
        });
      }
    }
  }

  /**
   * Gets unused fields from previous steps and inserts them as rows into a table view.
   *
   * @param transMeta the transformation metadata
   * @param stepMeta the step metadata
   * @param tableView the table view
   * @param keyColumn the key column
   * @param nameColumn the name column
   * @param dataTypeColumn the data type column
   * @param lengthColumn the length column
   * @param precisionColumn the precision column
   * @param listener a listener for tables insert events
   */
  public static final void getFieldsFromPrevious(TransMeta transMeta, StepMeta stepMeta, TableView tableView,
      int keyColumn, int nameColumn[], int dataTypeColumn[], int lengthColumn, int precisionColumn,
      TableItemInsertListener listener) {
    try {
      RowMetaInterface row = transMeta.getPrevStepFields(stepMeta);
      if (row != null) {
        getFieldsFromPrevious(row, tableView, keyColumn, nameColumn, dataTypeColumn, lengthColumn, precisionColumn,
            listener);
      }
    } catch (KettleException ke) {
      new ErrorDialog(
          tableView.getShell(),
          BaseMessages.getString(PKG, "BaseStepDialog.FailedToGetFields.Title"), BaseMessages.getString(PKG, "BaseStepDialog.FailedToGetFields.Message", stepMeta.getName()), ke);  
    }
  }

  /**
   * Gets unused fields from previous steps and inserts them as rows into a table view.
   *
   * @param row the input fields
   * @param tableView the table view to modify
   * @param keyColumn the column in the table view to match with the names of the fields, checks for existance if >0 
   * @param nameColumn the column numbers in which the name should end up in
   * @param dataTypeColumn the target column numbers in which the data type should end up in
   * @param lengthColumn the length column where the length should end up in (if >0)
   * @param precisionColumn the length column where the precision should end up in (if >0)
   * @param listener A listener that you can use to do custom modifications to the inserted table item, based on a value from the provided row
   */
  public static final void getFieldsFromPrevious(RowMetaInterface row, TableView tableView, int keyColumn,
      int nameColumn[], int dataTypeColumn[], int lengthColumn, int precisionColumn, TableItemInsertListener listener) {
    if (row == null || row.size() == 0)
      return; // nothing to do

    Table table = tableView.table;

    // get a list of all the non-empty keys (names)
    //
    List<String> keys = new ArrayList<String>();
    for (int i = 0; i < table.getItemCount(); i++) {
      TableItem tableItem = table.getItem(i);
      String key = tableItem.getText(keyColumn);
      if (!Const.isEmpty(key) && keys.indexOf(key) < 0)
        keys.add(key);
    }

    int choice = 0;

    if (keys.size() > 0) {
      // Ask what we should do with the existing data in the step.
      //
      MessageDialog md = new MessageDialog(tableView.getShell(), BaseMessages.getString(PKG, "BaseStepDialog.GetFieldsChoice.Title"),//"Warning!"  
          null, BaseMessages.getString(PKG, "BaseStepDialog.GetFieldsChoice.Message", "" + keys.size(), "" + row.size()),     //$NON-NLS-3$
          MessageDialog.WARNING, new String[] { BaseMessages.getString(PKG, "BaseStepDialog.AddNew"), 
              BaseMessages.getString(PKG, "BaseStepDialog.Add"), BaseMessages.getString(PKG, "BaseStepDialog.ClearAndAdd"),   
              BaseMessages.getString(PKG, "BaseStepDialog.Cancel"), }, 0); 
      MessageDialog.setDefaultImage(GUIResource.getInstance().getImageSpoon());
      int idx = md.open();
      choice = idx & 0xFF;
    }

    if (choice == 3 || choice == 255 /* 255 = escape pressed */)
      return; // Cancel clicked

    if (choice == 2) {
      tableView.clearAll(false);
    }

    for (int i = 0; i < row.size(); i++) {
      ValueMetaInterface v = row.getValueMeta(i);

      boolean add = true;

      if (choice == 0) // hang on, see if it's not yet in the table view
      {
        if (keys.indexOf(v.getName()) >= 0)
          add = false;
      }

      if (add) {
        TableItem tableItem = new TableItem(table, SWT.NONE);

        for (int c = 0; c < nameColumn.length; c++) {
          tableItem.setText(nameColumn[c], Const.NVL(v.getName(), "")); 
        }
        if ( dataTypeColumn != null )
        {
            for (int c = 0; c < dataTypeColumn.length; c++) {
                tableItem.setText(dataTypeColumn[c], v.getTypeDesc());
            }
        }
        if (lengthColumn > 0) {
          if (v.getLength() >= 0)
            tableItem.setText(lengthColumn, Integer.toString(v.getLength()));
        }
        if (precisionColumn > 0) {
          if (v.getPrecision() >= 0)
            tableItem.setText(precisionColumn, Integer.toString(v.getPrecision()));
        }

        if (listener != null) {
          if (!listener.tableItemInserted(tableItem, v)) {
            tableItem.dispose(); // remove it again
          }
        }
      }
    }
    tableView.removeEmptyRows();
    tableView.setRowNums();
    tableView.optWidth(true);
  }
  
  /**
   * Gets fields from previous steps and populate a ComboVar.
   *
   * @param comboVar the Combo Box (with Variables) to populate
   * @param transMeta the transformation metadata
   * @param stepMeta the step metadata
   */
  public static final void getFieldsFromPrevious(ComboVar comboVar,TransMeta transMeta,StepMeta stepMeta)
	 {
		 String selectedField=null;
		 int indexField=-1;
		 try{   
			 RowMetaInterface r = transMeta.getPrevStepFields(stepMeta);
			 selectedField=comboVar.getText();
			 comboVar.removeAll();
				
			 if (r!=null && !r.isEmpty()) {
	             r.getFieldNames();
	             comboVar.setItems(r.getFieldNames());
	             indexField=r.indexOfValue(selectedField);
			 }
			 // Select value if possible...
			 if(indexField>-1) comboVar.select(indexField); else { if(selectedField!=null) comboVar.setText(selectedField);}
		 }catch(KettleException ke){
				new ErrorDialog(comboVar.getShell(),BaseMessages.getString(PKG, "BaseStepDialog.FailedToGetFieldsPrevious.DialogTitle"),
						BaseMessages.getString(PKG, "BaseStepDialog.FailedToGetFieldsPrevious.DialogMessage"),ke);
			}
	 }
  
	/**
	 * Create a new field mapping between source and target steps.
	 * 
	 * @param shell the shell of the parent window
	 * @param sourceFields the source fields
	 * @param targetFields the target fields
	 * @param fieldMapping the list of source to target mappings to default to (can be empty but not null)
	 * 
	 * @throws KettleException in case something goes wrong during the field mapping
	 * 
	 */
	public static final void generateFieldMapping(Shell shell, RowMetaInterface sourceFields, RowMetaInterface targetFields, List<SourceToTargetMapping> fieldMapping) throws KettleException {
		// Build the mapping: let the user decide!!
		String[] source = sourceFields.getFieldNames();
		for (int i = 0; i < source.length; i++) {
			ValueMetaInterface v = sourceFields.getValueMeta(i);
			source[i] += EnterMappingDialog.STRING_ORIGIN_SEPARATOR + v.getOrigin() + ")";
		}
		String[] target = targetFields.getFieldNames();

		EnterMappingDialog dialog = new EnterMappingDialog(shell, source, target, fieldMapping);
		List<SourceToTargetMapping> newMapping = dialog.open();
		if (newMapping!=null) {
			fieldMapping.clear();
			fieldMapping.addAll(newMapping);
		}
	}

    /**
     * Checks if the log level is basic.
     *
     * @return true, if the log level is basic, false otherwise
     */
    public boolean isBasic() { return log.isBasic(); }
    
    /**
     * Checks if the log level is detailed.
     *
     * @return true, if the log level is detailed, false otherwise
     */
    public boolean isDetailed() { return log.isDetailed(); }
    
    /**
     * Checks if the log level is debug.
     *
     * @return true, if the log level is debug, false otherwise
     */
    public boolean isDebug() { return log.isDebug(); }
    
    /**
     * Checks if the log level is row level.
     *
     * @return true, if the log level is row level, false otherwise
     */
    public boolean isRowLevel() { return log.isRowLevel(); }
    
    /**
     * Log the message at a minimal logging level.
     *
     * @param message the message to log
     */
    public void logMinimal(String message) { log.logMinimal(message); }
    
    /**
     * Log the message with arguments at a minimal logging level.
     *
     * @param message the message
     * @param arguments the arguments
     */
    public void logMinimal(String message, Object...arguments) { log.logMinimal(message, arguments); }
    
    /**
     * Log the message at a basic logging level.
     *
     * @param message the message
     */
    public void logBasic(String message) { log.logBasic(message); }
    
    /**
     * Log the message with arguments at a basic logging level.
     *
     * @param message the message
     * @param arguments the arguments
     */
    public void logBasic(String message, Object...arguments) { log.logBasic(message, arguments); }
    
    /**
     * Log the message at a detailed logging level.
     *
     * @param message the message
     */
    public void logDetailed(String message) { log.logDetailed(message); }
    
    /**
     * Log the message with arguments at a detailed logging level.
     *
     * @param message the message
     * @param arguments the arguments
     */
    public void logDetailed(String message, Object...arguments) { log.logDetailed(message, arguments); }
    
    /**
     * Log the message at a debug logging level.
     *
     * @param message the message
     */
    public void logDebug(String message) { log.logDebug(message); }
    
    /**
     * Log the message with arguments at a debug logging level.
     *
     * @param message the message
     * @param arguments the arguments
     */
    public void logDebug(String message, Object...arguments) { log.logDebug(message, arguments); }
    
    /**
     * Log the message at a rowlevel logging level.
     *
     * @param message the message
     */
    public void logRowlevel(String message) { log.logRowlevel(message); }
    
    /**
     * Log the message with arguments at a rowlevel logging level.
     *
     * @param message the message
     * @param arguments the arguments
     */
    public void logRowlevel(String message, Object...arguments) { log.logRowlevel(message, arguments); }
    
    /**
     * Log the message at a error logging level.
     *
     * @param message the message
     */
    public void logError(String message) { log.logError(message); } 
    
    /**
     * Log the message with the associated Throwable object at a error logging level.
     *
     * @param message the message
     * @param e the e
     */
    public void logError(String message, Throwable e) { log.logError(message, e); }
    
    /**
     * Log the message with arguments at a error logging level.
     *
     * @param message the message
     * @param arguments the arguments
     */
    public void logError(String message, Object...arguments) { log.logError(message, arguments); }

    protected Button createHelpButton(final Shell shell, final StepMeta stepMeta, final PluginInterface plugin) {
      return createHelpButton(shell, "Step documentation for "+plugin.getName(), plugin);
    }
    
    public static Button createHelpButton(final Composite parent, final String title, final PluginInterface plugin) {
      Button button = new Button(parent, SWT.PUSH);
      button.setImage(GUIResource.getInstance().getImageHelpWeb());
      button.setText(BaseMessages.getString(PKG, "System.Button.Help"));
      button.setToolTipText(BaseMessages.getString(PKG, "System.Tooltip.Help"));
      FormData fdButton = new FormData();
      fdButton.left = new FormAttachment(0,0);
      fdButton.bottom = new FormAttachment(100, 0);
      button.setLayoutData(fdButton);
      
      button.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent arg0) {
          StringBuilder html = new StringBuilder();
          html.append("<HTML><TITLE>").append(plugin.getName()).append("</TITLE>");
          html.append("<HEAD>");
          html.append("<STYLE type=\"text/css\">");

          html.append("body {");
          html.append("  background-color:#a7c4e2;");
          html.append("  font-family:arial,helvetica,sans-serif;");
          html.append("  font-size:16px;");
          html.append("}");
            
          html.append("</STYLE>");
          
          html.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"docs/English/welcome/kettle.css\" />");
          html.append("</HEAD>");
          html.append("<BODY>");
          html.append("<b>Name</b>: ").append(plugin.getName()).append("<br>");
          html.append("<b>ID: ").append(plugin.getIds()[0]).append("<br>");
          if (!Const.isEmpty(plugin.getDescription())) {
            html.append("<b>Description</b>: ").append(plugin.getDescription()).append("<br>");
          }
          if (!Const.isEmpty(plugin.getImageFile())) {
            html.append("<b>Icon</b>: ").append(plugin.getImageFile()).append("<br>");
          }
          if (!Const.isEmpty(plugin.getDocumentationUrl())) {
            html.append("<b>Documenation</b>: <a href=\"").append(plugin.getDocumentationUrl()).append("\">").append("Click here to view the documentation").append("</a><br>");
          }
          if (!Const.isEmpty(plugin.getCasesUrl())) {
            html.append("<b>Cases</b>: <a href=\"").append(plugin.getCasesUrl()).append("\">").append("Click here to view related PDI cases").append("</a><br>");
          }
          if (!Const.isEmpty(plugin.getForumUrl())) {
            html.append("<b>Forum</b>: <a href=\"").append(plugin.getForumUrl()).append("\">").append("Click here to go to the forum").append("</a><br>");
          }
          html.append("</BODY></HTML>");
          
          ShowBrowserDialog browserDialog = new ShowBrowserDialog(parent.getShell(), title, html.toString());
          browserDialog.open();
        }
      });
      return button;
    }

    public IMetaStore getMetaStore() {
      return metaStore;
    }

    public void setMetaStore(IMetaStore metaStore) {
      this.metaStore = metaStore;
    }
}
