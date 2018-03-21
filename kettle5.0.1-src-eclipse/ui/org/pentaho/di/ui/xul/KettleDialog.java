package org.pentaho.di.ui.xul;

import org.pentaho.di.ui.core.PropsUI;
import org.pentaho.di.ui.core.gui.WindowProperty;
import org.pentaho.di.ui.trans.step.BaseStepDialog;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.containers.XulRoot;
import org.pentaho.ui.xul.dom.Element;
import org.pentaho.ui.xul.swt.tags.SwtDialog;

public class KettleDialog extends SwtDialog {

  public KettleDialog(Element self, XulComponent parent, XulDomContainer container, String tagName) {
    super(self, parent, container, tagName);
  }

  @Override
  public void show() {
    show(true);
  }
  
  @SuppressWarnings("deprecation")
  @Override
  public void show(boolean force) {
    if((force) || (!buttonsCreated)){
      setButtons();
    }
    
    isDialogHidden = false;

    dialog.getShell().setText(title);
    
    // Remember the size from a last time or do proper layouting of the window.
    //
    if (getWidth()>0 && getHeight()>0) {
      BaseStepDialog.setSize(getShell(), getWidth(), getHeight(), true);
    } else {
      BaseStepDialog.setSize(getShell());
    }
    
    width = getShell().getSize().x;
    height = getShell().getSize().y;
    
    dialog.getShell().layout(true,true);
    
    // Timing is everything - fire the onLoad events so that anyone who is trying to listens gets notified
    //
    notifyListeners(XulRoot.EVENT_ON_LOAD);
    
    
    
    returnCode = dialog.open();
  }
  
  
  @Override
  public void hide() {

    if(closing || dialog.getMainArea().isDisposed() || getParentShell(getParent()).isDisposed()
        || (getParent() instanceof SwtDialog && ((SwtDialog) getParent()).isDisposing()) ){
      return;
    }
    
    // Save the window location & size in the Kettle world...
    //
    WindowProperty windowProperty = new WindowProperty(getShell());
    PropsUI.getInstance().setScreen(windowProperty);
    
    super.hide();
  }
}
