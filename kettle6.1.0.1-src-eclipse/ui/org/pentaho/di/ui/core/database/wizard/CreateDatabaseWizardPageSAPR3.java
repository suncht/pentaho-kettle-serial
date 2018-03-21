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

package org.pentaho.di.ui.core.database.wizard;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.database.SAPR3DatabaseMeta;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.ui.core.PropsUI;

/**
 *
 * On page one we select the database connection SAP/R3 specific settings 1) The data tablespace 2) The index tablespace
 *
 * @author Jens Bleuel
 * @since 22-mar-2006
 */
public class CreateDatabaseWizardPageSAPR3 extends WizardPage {
  private static Class<?> PKG = CreateDatabaseWizard.class; // for i18n purposes, needed by Translator2!!

  private Label wlHostname;
  private Text wHostname;
  private FormData fdlHostname, fdHostname;

  // SAP
  private Label wlSAPLanguage, wlSAPSystemNumber, wlSAPClient;
  private Text wSAPLanguage, wSAPSystemNumber, wSAPClient;
  private FormData fdlSAPLanguage, fdlSAPSystemNumber, fdlSAPClient;
  private FormData fdSAPLanguage, fdSAPSystemNumber, fdSAPClient;

  private PropsUI props;
  private DatabaseMeta info;

  public CreateDatabaseWizardPageSAPR3( String arg, PropsUI props, DatabaseMeta info ) {
    super( arg );
    this.props = props;
    this.info = info;

    setTitle( BaseMessages.getString( PKG, "CreateDatabaseWizardPageSAPR3.DialogTitle" ) );
    setDescription( BaseMessages.getString( PKG, "CreateDatabaseWizardPageSAPR3.DialogMessage" ) );

    setPageComplete( false );
  }

  public void createControl( Composite parent ) {
    int margin = Const.MARGIN;
    int middle = props.getMiddlePct();

    // create the composite to hold the widgets
    Composite composite = new Composite( parent, SWT.NONE );
    props.setLook( composite );

    FormLayout compLayout = new FormLayout();
    compLayout.marginHeight = Const.FORM_MARGIN;
    compLayout.marginWidth = Const.FORM_MARGIN;
    composite.setLayout( compLayout );

    // HOSTNAME
    wlHostname = new Label( composite, SWT.RIGHT );
    wlHostname.setText( BaseMessages.getString( PKG, "CreateDatabaseWizardPageSAPR3.Hostname.Label" ) );
    props.setLook( wlHostname );
    fdlHostname = new FormData();
    fdlHostname.top = new FormAttachment( 0, 0 );
    fdlHostname.left = new FormAttachment( 0, 0 );
    fdlHostname.right = new FormAttachment( middle, 0 );
    wlHostname.setLayoutData( fdlHostname );
    wHostname = new Text( composite, SWT.SINGLE | SWT.BORDER );
    props.setLook( wHostname );
    fdHostname = new FormData();
    fdHostname.top = new FormAttachment( 0, 0 );
    fdHostname.left = new FormAttachment( middle, margin );
    fdHostname.right = new FormAttachment( 100, 0 );
    wHostname.setLayoutData( fdHostname );
    wHostname.addModifyListener( new ModifyListener() {
      public void modifyText( ModifyEvent arg0 ) {
        setPageComplete( false );
      }
    } );

    wlSAPLanguage = new Label( composite, SWT.RIGHT );
    wlSAPLanguage.setText( BaseMessages.getString( PKG, "CreateDatabaseWizardPageSAPR3.Language.Label" ) );
    props.setLook( wlSAPLanguage );
    fdlSAPLanguage = new FormData();
    fdlSAPLanguage.top = new FormAttachment( wHostname, margin );
    fdlSAPLanguage.left = new FormAttachment( 0, 0 );
    fdlSAPLanguage.right = new FormAttachment( middle, 0 );
    wlSAPLanguage.setLayoutData( fdlSAPLanguage );
    wSAPLanguage = new Text( composite, SWT.SINGLE | SWT.BORDER );
    props.setLook( wSAPLanguage );
    fdSAPLanguage = new FormData();
    fdSAPLanguage.top = new FormAttachment( wHostname, margin );
    fdSAPLanguage.left = new FormAttachment( middle, margin );
    fdSAPLanguage.right = new FormAttachment( 100, 0 );
    wSAPLanguage.setLayoutData( fdSAPLanguage );
    wSAPLanguage.addModifyListener( new ModifyListener() {
      public void modifyText( ModifyEvent arg0 ) {
        setPageComplete( false );
      }
    } );

    wlSAPSystemNumber = new Label( composite, SWT.RIGHT );
    wlSAPSystemNumber.setText( BaseMessages.getString( PKG, "CreateDatabaseWizardPageSAPR3.SystemNumber.Label" ) );
    props.setLook( wlSAPSystemNumber );
    fdlSAPSystemNumber = new FormData();
    fdlSAPSystemNumber.top = new FormAttachment( wSAPLanguage, margin );
    fdlSAPSystemNumber.left = new FormAttachment( 0, 0 );
    fdlSAPSystemNumber.right = new FormAttachment( middle, 0 );
    wlSAPSystemNumber.setLayoutData( fdlSAPSystemNumber );
    wSAPSystemNumber = new Text( composite, SWT.SINGLE | SWT.BORDER );
    props.setLook( wSAPSystemNumber );
    fdSAPSystemNumber = new FormData();
    fdSAPSystemNumber.top = new FormAttachment( wSAPLanguage, margin );
    fdSAPSystemNumber.left = new FormAttachment( middle, margin );
    fdSAPSystemNumber.right = new FormAttachment( 100, 0 );
    wSAPSystemNumber.setLayoutData( fdSAPSystemNumber );
    wSAPSystemNumber.addModifyListener( new ModifyListener() {
      public void modifyText( ModifyEvent arg0 ) {
        setPageComplete( false );
      }
    } );

    wlSAPClient = new Label( composite, SWT.RIGHT );
    wlSAPClient.setText( BaseMessages.getString( PKG, "CreateDatabaseWizardPageSAPR3.SAPClient.Label" ) );
    props.setLook( wlSAPClient );
    fdlSAPClient = new FormData();
    fdlSAPClient.top = new FormAttachment( wSAPSystemNumber, margin );
    fdlSAPClient.left = new FormAttachment( 0, 0 );
    fdlSAPClient.right = new FormAttachment( middle, 0 );
    wlSAPClient.setLayoutData( fdlSAPClient );
    wSAPClient = new Text( composite, SWT.SINGLE | SWT.BORDER );
    props.setLook( wSAPClient );
    fdSAPClient = new FormData();
    fdSAPClient.top = new FormAttachment( wSAPSystemNumber, margin );
    fdSAPClient.left = new FormAttachment( middle, margin );
    fdSAPClient.right = new FormAttachment( 100, 0 );
    wSAPClient.setLayoutData( fdSAPClient );
    wSAPClient.addModifyListener( new ModifyListener() {
      public void modifyText( ModifyEvent arg0 ) {
        setPageComplete( false );
      }
    } );

    // set the composite as the control for this page
    setControl( composite );
  }

  public void setData() {
    wHostname.setText( Const.NVL( info.getHostname(), "" ) );

    wSAPLanguage.setText( Const.NVL( info.getAttributes().getProperty(
      SAPR3DatabaseMeta.ATTRIBUTE_SAP_LANGUAGE, "" ), "" ) );
    wSAPSystemNumber.setText( Const.NVL( info.getAttributes().getProperty(
      SAPR3DatabaseMeta.ATTRIBUTE_SAP_SYSTEM_NUMBER, "" ), "" ) );
    wSAPClient.setText( Const.NVL(
      info.getAttributes().getProperty( SAPR3DatabaseMeta.ATTRIBUTE_SAP_CLIENT, "" ), "" ) );
  }

  public boolean canFlipToNextPage() {
    String server =
      wHostname.getText() != null ? wHostname.getText().length() > 0 ? wHostname.getText() : null : null;
    String language =
      wSAPLanguage.getText() != null
        ? wSAPLanguage.getText().length() > 0 ? wSAPLanguage.getText() : null : null;
    String systemNumber =
      wSAPSystemNumber.getText() != null ? wSAPSystemNumber.getText().length() > 0
        ? wSAPSystemNumber.getText() : null : null;
    String client =
      wSAPClient.getText() != null ? wSAPClient.getText().length() > 0 ? wSAPClient.getText() : null : null;

    if ( server == null || language == null || systemNumber == null || client == null ) {
      setErrorMessage( BaseMessages.getString( PKG, "CreateDatabaseWizardPageSAPR3.ErrorMessage.InvalidInput" ) );
      return false;
    } else {
      getDatabaseInfo();
      setErrorMessage( null );
      setMessage( BaseMessages.getString( PKG, "CreateDatabaseWizardPageSAPR3.Message.Next" ) );
      return true;
    }

  }

  public DatabaseMeta getDatabaseInfo() {
    if ( wHostname.getText() != null && wHostname.getText().length() > 0 ) {
      info.setHostname( wHostname.getText() );
    }

    if ( wSAPLanguage.getText() != null && wSAPLanguage.getText().length() > 0 ) {
      info.getAttributes().put( SAPR3DatabaseMeta.ATTRIBUTE_SAP_LANGUAGE, wSAPLanguage.getText() );
    }

    if ( wSAPSystemNumber.getText() != null && wSAPSystemNumber.getText().length() > 0 ) {
      info.getAttributes().put( SAPR3DatabaseMeta.ATTRIBUTE_SAP_SYSTEM_NUMBER, wSAPSystemNumber.getText() );
    }

    if ( wSAPClient.getText() != null && wSAPClient.getText().length() > 0 ) {
      info.getAttributes().put( SAPR3DatabaseMeta.ATTRIBUTE_SAP_CLIENT, wSAPClient.getText() );
    }

    return info;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.jface.wizard.WizardPage#getNextPage()
   */
  public IWizardPage getNextPage() {
    IWizard wiz = getWizard();
    return wiz.getPage( "2" );
  }

}
