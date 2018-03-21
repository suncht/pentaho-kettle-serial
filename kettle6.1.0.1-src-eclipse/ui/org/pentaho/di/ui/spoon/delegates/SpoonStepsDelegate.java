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

package org.pentaho.di.ui.spoon.delegates;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.gui.Point;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Partitioner;
import org.pentaho.di.trans.TransHopMeta;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDialogInterface;
import org.pentaho.di.trans.step.StepErrorMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.di.trans.step.StepPartitioningMeta;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.trans.step.StepErrorMetaDialog;

public class SpoonStepsDelegate extends SpoonDelegate {
  private static Class<?> PKG = Spoon.class; // for i18n purposes, needed by Translator2!!

  public SpoonStepsDelegate( Spoon spoon ) {
    super( spoon );
  }

  public void editStepErrorHandling( TransMeta transMeta, StepMeta stepMeta ) {
    if ( stepMeta != null && stepMeta.supportsErrorHandling() ) {
      StepErrorMeta stepErrorMeta = stepMeta.getStepErrorMeta();
      if ( stepErrorMeta == null ) {
        stepErrorMeta = new StepErrorMeta( transMeta, stepMeta );
      }
      List<StepMeta> targetSteps = transMeta.findNextSteps( stepMeta );

      // now edit this stepErrorMeta object:
      StepErrorMetaDialog dialog =
        new StepErrorMetaDialog( spoon.getShell(), stepErrorMeta, transMeta, targetSteps );
      if ( dialog.open() ) {
        stepMeta.setStepErrorMeta( stepErrorMeta );
        stepMeta.setChanged();
        spoon.refreshGraph();
      }
    }
  }

  public void dupeStep( TransMeta transMeta, StepMeta stepMeta ) {
    spoon.getLog().logDebug(
      toString(), BaseMessages.getString( PKG, "Spoon.Log.DuplicateStep" ) + stepMeta.getName() ); // Duplicate
    // step:

    StepMeta stMeta = (StepMeta) stepMeta.clone();
    if ( stMeta != null ) {
      String newname = transMeta.getAlternativeStepname( stepMeta.getName() );
      int nr = 2;
      while ( transMeta.findStep( newname ) != null ) {
        newname = stepMeta.getName() + " (copy " + nr + ")";
        nr++;
      }
      stMeta.setName( newname );
      // Don't select this new step!
      stMeta.setSelected( false );
      Point loc = stMeta.getLocation();
      stMeta.setLocation( loc.x + 20, loc.y + 20 );
      transMeta.addStep( stMeta );
      spoon.addUndoNew( transMeta, new StepMeta[] { (StepMeta) stMeta.clone() }, new int[] { transMeta
        .indexOfStep( stMeta ) } );
      spoon.refreshTree();
      spoon.refreshGraph();
    }
  }

  public void clipStep( StepMeta stepMeta ) {
    try {
      String xml = stepMeta.getXML();
      GUIResource.getInstance().toClipboard( xml );
    } catch ( Exception ex ) {
      new ErrorDialog( spoon.getShell(), "Error", "Error encoding to XML", ex );
    }
  }

  public String editStep( TransMeta transMeta, StepMeta stepMeta ) {
    boolean refresh = false;
    String stepname = null;
    try {
      String name = stepMeta.getName();

      // Before we do anything, let's store the situation the way it
      // was...
      //
      StepMeta before = (StepMeta) stepMeta.clone();
      StepDialogInterface dialog = spoon.getStepEntryDialog( stepMeta.getStepMetaInterface(), transMeta, name );
      if ( dialog != null ) {
        dialog.setRepository( spoon.getRepository() );
        dialog.setMetaStore( spoon.getMetaStore() );
        stepname = dialog.open();
      }

      if ( !Const.isEmpty( stepname ) ) {
        // Force the recreation of the step IO metadata object. (cached by default)
        //
        stepMeta.getStepMetaInterface().resetStepIoMeta();

        //
        // See if the new name the user enter, doesn't collide with
        // another step.
        // If so, change the stepname and warn the user!
        //
        String newname = stepname;
        StepMeta smeta = transMeta.findStep( newname, stepMeta );
        int nr = 2;
        while ( smeta != null ) {
          newname = stepname + " " + nr;
          smeta = transMeta.findStep( newname );
          nr++;
        }
        if ( nr > 2 ) {
          stepname = newname;
          MessageBox mb = new MessageBox( spoon.getShell(), SWT.OK | SWT.ICON_INFORMATION );
          mb.setMessage( BaseMessages.getString( PKG, "Spoon.Dialog.StepnameExists.Message", stepname ) );
          mb.setText( BaseMessages.getString( PKG, "Spoon.Dialog.StepnameExists.Title" ) );
          mb.open();
        }

        if ( !stepname.equals( name ) ) {
          refresh = true;
        }

        StepMeta newStepMeta = (StepMeta) stepMeta.clone();
        newStepMeta.setName( stepname );
        transMeta.notifyAllListeners( stepMeta, newStepMeta );
        stepMeta.setName( stepname );

        //
        // OK, so the step has changed...
        // Backup the situation for undo/redo
        //
        StepMeta after = (StepMeta) stepMeta.clone();
        spoon.addUndoChange( transMeta, new StepMeta[] { before }, new StepMeta[] { after }, new int[] { transMeta
          .indexOfStep( stepMeta ) } );
      } else {
        // Scenario: change connections and click cancel...
        // Perhaps new connections were created in the step dialog?
        if ( transMeta.haveConnectionsChanged() ) {
          refresh = true;
        }
      }
      spoon.refreshGraph(); // name is displayed on the graph too.

      // TODO: verify "double pathway" steps for bug #4365
      // After the step was edited we can complain about the possible
      // deadlock here.
      //
    } catch ( Throwable e ) {
      if ( spoon.getShell().isDisposed() ) {
        return null;
      }
      new ErrorDialog(
        spoon.getShell(), BaseMessages.getString( PKG, "Spoon.Dialog.UnableOpenDialog.Title" ), BaseMessages
          .getString( PKG, "Spoon.Dialog.UnableOpenDialog.Message" ), e );
    }

    if ( refresh ) {
      spoon.refreshTree(); // Perhaps new connections were created in
      // the step
      // dialog or the step name changed.
    }

    return stepname;
  }

  public void delSteps( TransMeta transformation, StepMeta[] steps ) {

    // Hops belonging to the deleting steps are placed in a single transaction and removed.
    List<TransHopMeta> transHops = new ArrayList<TransHopMeta>();
    int[] hopIndexes = new int[transformation.nrTransHops()];
    int hopIndex = 0;
    main: for ( int i = transformation.nrTransHops() - 1; i >= 0; i-- ) {
      TransHopMeta hi = transformation.getTransHop( i );
      for ( int j = 0; j < steps.length; j++ ) {
        if ( hi.getFromStep().equals( steps[j] ) || hi.getToStep().equals( steps[j] ) ) {
          int idx = transformation.indexOfTransHop( hi );
          transHops.add( (TransHopMeta) hi.clone() );
          hopIndexes[hopIndex] = idx;
          transformation.removeTransHop( idx );
          spoon.refreshTree();
          continue main;
        }
      }
      hopIndex++;
    }
    if ( !transHops.isEmpty() ) {
      TransHopMeta[] hops = transHops.toArray( new TransHopMeta[transHops.size()] );
      spoon.addUndoDelete( transformation, hops, hopIndexes );
    }

    // Deleting steps are placed all in a single transaction and removed.
    int[] positions = new int[steps.length];
    for ( int i = 0; i < steps.length; i++ ) {
      int pos = transformation.indexOfStep( steps[i] );
      transformation.removeStep( pos );
      positions[i] = pos;
    }
    spoon.addUndoDelete( transformation, steps, positions );

    spoon.refreshTree();
    spoon.refreshGraph();
  }
  
  public void delStep( TransMeta transMeta, StepMeta stepMeta ) {
    spoon.getLog().logDebug(
      toString(), BaseMessages.getString( PKG, "Spoon.Log.DeleteStep" ) + stepMeta.getName() ); // "Delete
    // step:
    // "

    for ( int i = transMeta.nrTransHops() - 1; i >= 0; i-- ) {
      TransHopMeta hi = transMeta.getTransHop( i );
      if ( hi.getFromStep().equals( stepMeta ) || hi.getToStep().equals( stepMeta ) ) {
        int idx = transMeta.indexOfTransHop( hi );
        spoon.addUndoDelete( transMeta, new TransHopMeta[] { (TransHopMeta) hi.clone() }, new int[] { idx } );
        // ,true // the true flag was causing the hops to not get restored on Undo delete step with hop(s)
        // );
        transMeta.removeTransHop( idx );
        spoon.refreshTree();
      }
    }

    int pos = transMeta.indexOfStep( stepMeta );
    transMeta.removeStep( pos );
    spoon.addUndoDelete( transMeta, new StepMeta[] { stepMeta }, new int[] { pos } );

    spoon.refreshTree();
    spoon.refreshGraph();
  }  

  public StepDialogInterface getStepDialog( StepMetaInterface stepMeta, TransMeta transMeta, String stepName ) throws KettleException {
    String dialogClassName = stepMeta.getDialogClassName();

    Class<?> dialogClass;
    Class<?>[] paramClasses = new Class<?>[] { Shell.class, Object.class, TransMeta.class, String.class };
    Object[] paramArgs = new Object[] { spoon.getShell(), stepMeta, transMeta, stepName };
    Constructor<?> dialogConstructor;
    try {
      dialogClass = stepMeta.getClass().getClassLoader().loadClass( dialogClassName );
      dialogConstructor = dialogClass.getConstructor( paramClasses );
      return (StepDialogInterface) dialogConstructor.newInstance( paramArgs );
    } catch ( Exception e ) {
      // try the old way for compatibility
      Method method = null;
      try {
        Class<?>[] sig = new Class<?>[] { Shell.class, StepMetaInterface.class, TransMeta.class, String.class };
        method = stepMeta.getClass().getDeclaredMethod( "getDialog", sig );
        if ( method != null ) {
          return (StepDialogInterface) method.invoke( stepMeta, new Object[] {
            spoon.getShell(), stepMeta, transMeta, stepName } );
        }
      } catch ( Throwable t ) {
        // Ignore errors
      }

      String errorTitle =
        BaseMessages.getString( PKG, "Spoon.Dialog.ErrorCreatingStepDialog.Title" );
      String errorMsg =
        BaseMessages.getString( PKG, "Spoon.Dialog.ErrorCreatingStepDialog.Message", stepMeta.getDialogClassName() );
      new ErrorDialog(
        spoon.getShell(), errorTitle, errorMsg, e );

      throw new KettleException( e );
    }
  }

  public StepDialogInterface getPartitionerDialog( StepMeta stepMeta, StepPartitioningMeta partitioningMeta,
    TransMeta transMeta ) throws KettleException {
    Partitioner partitioner = partitioningMeta.getPartitioner();
    String dialogClassName = partitioner.getDialogClassName();

    Class<?> dialogClass;
    Class<?>[] paramClasses =
      new Class<?>[] { Shell.class, StepMeta.class, StepPartitioningMeta.class, TransMeta.class };
    Object[] paramArgs = new Object[] { spoon.getShell(), stepMeta, partitioningMeta, transMeta };
    Constructor<?> dialogConstructor;
    try {
      dialogClass = partitioner.getClass().getClassLoader().loadClass( dialogClassName );
      dialogConstructor = dialogClass.getConstructor( paramClasses );
      return (StepDialogInterface) dialogConstructor.newInstance( paramArgs );
    } catch ( Exception e ) {
      // try the old way for compatibility
      Method method = null;
      try {
        Class<?>[] sig = new Class<?>[] { Shell.class, StepMetaInterface.class, TransMeta.class };
        method = stepMeta.getClass().getDeclaredMethod( "getDialog", sig );
        if ( method != null ) {
          return (StepDialogInterface) method.invoke( stepMeta, new Object[] {
            spoon.getShell(), stepMeta, transMeta } );
        }
      } catch ( Throwable t ) {
        // Ignore errors
      }

      throw new KettleException( e );
    }

  }

}
