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

package org.pentaho.di.trans.step;

import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.changed.ChangedFlag;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.core.xml.XMLInterface;
import org.w3c.dom.Node;

/**
 * This class contains the metadata to handle proper error handling on a step level.
 *
 * @author Matt
 *
 */
public class StepErrorMeta extends ChangedFlag implements XMLInterface, Cloneable {
  public static final String XML_TAG = "error";

  /** The source step that can send the error rows */
  private StepMeta sourceStep;

  /** The target step to send the error rows to */
  private StepMeta targetStep;

  /** Is the error handling enabled? */
  private boolean enabled;

  /** the name of the field value to contain the number of errors (null or empty means it's not needed) */
  private String nrErrorsValuename;

  /** the name of the field value to contain the error description(s) (null or empty means it's not needed) */
  private String errorDescriptionsValuename;

  /**
   * the name of the field value to contain the fields for which the error(s) occured (null or empty means it's not
   * needed)
   */
  private String errorFieldsValuename;

  /** the name of the field value to contain the error code(s) (null or empty means it's not needed) */
  private String errorCodesValuename;

  /** The maximum number of errors allowed before we stop processing with a hard error */
  private String maxErrors = "";

  /** The maximum percent of errors allowed before we stop processing with a hard error */
  private String maxPercentErrors = "";

  /** The minimum number of rows to read before the percentage evaluation takes place */
  private String minPercentRows = "";

  private VariableSpace variables;

  /**
   * Create a new step error handling metadata object
   *
   * @param sourceStep
   *          The source step that can send the error rows
   */
  public StepErrorMeta( VariableSpace space, StepMeta sourceStep ) {
    this.sourceStep = sourceStep;
    this.enabled = false;
    this.variables = space;
  }

  /**
   * Create a new step error handling metadata object
   *
   * @param sourceStep
   *          The source step that can send the error rows
   * @param targetStep
   *          The target step to send the error rows to
   */
  public StepErrorMeta( VariableSpace space, StepMeta sourceStep, StepMeta targetStep ) {
    this.sourceStep = sourceStep;
    this.targetStep = targetStep;
    this.enabled = false;
    this.variables = space;
  }

  /**
   * Create a new step error handling metadata object
   *
   * @param sourceStep
   *          The source step that can send the error rows
   * @param targetStep
   *          The target step to send the error rows to
   * @param nrErrorsValuename
   *          the name of the field value to contain the number of errors (null or empty means it's not needed)
   * @param errorDescriptionsValuename
   *          the name of the field value to contain the error description(s) (null or empty means it's not needed)
   * @param errorFieldsValuename
   *          the name of the field value to contain the fields for which the error(s) occured (null or empty means it's
   *          not needed)
   * @param errorCodesValuename
   *          the name of the field value to contain the error code(s) (null or empty means it's not needed)
   */
  public StepErrorMeta( VariableSpace space, StepMeta sourceStep, StepMeta targetStep, String nrErrorsValuename,
    String errorDescriptionsValuename, String errorFieldsValuename, String errorCodesValuename ) {
    this.sourceStep = sourceStep;
    this.targetStep = targetStep;
    this.enabled = false;
    this.nrErrorsValuename = nrErrorsValuename;
    this.errorDescriptionsValuename = errorDescriptionsValuename;
    this.errorFieldsValuename = errorFieldsValuename;
    this.errorCodesValuename = errorCodesValuename;
    this.variables = space;
  }

  public StepErrorMeta clone() {
    try {
      return (StepErrorMeta) super.clone();
    } catch ( CloneNotSupportedException e ) {
      return null;
    }
  }

  public String getXML() {
    StringBuffer xml = new StringBuffer( 300 );

    xml.append( "      " ).append( XMLHandler.openTag( XML_TAG ) ).append( Const.CR );
    xml.append( "        " ).append(
      XMLHandler.addTagValue( "source_step", sourceStep != null ? sourceStep.getName() : "" ) );
    xml.append( "        " ).append(
      XMLHandler.addTagValue( "target_step", targetStep != null ? targetStep.getName() : "" ) );
    xml.append( "        " ).append( XMLHandler.addTagValue( "is_enabled", enabled ) );
    xml.append( "        " ).append( XMLHandler.addTagValue( "nr_valuename", nrErrorsValuename ) );
    xml
      .append( "        " ).append(
        XMLHandler.addTagValue( "descriptions_valuename", errorDescriptionsValuename ) );
    xml.append( "        " ).append( XMLHandler.addTagValue( "fields_valuename", errorFieldsValuename ) );
    xml.append( "        " ).append( XMLHandler.addTagValue( "codes_valuename", errorCodesValuename ) );
    xml.append( "        " ).append( XMLHandler.addTagValue( "max_errors", maxErrors ) );
    xml.append( "        " ).append( XMLHandler.addTagValue( "max_pct_errors", maxPercentErrors ) );
    xml.append( "        " ).append( XMLHandler.addTagValue( "min_pct_rows", minPercentRows ) );
    xml.append( "      " ).append( XMLHandler.closeTag( XML_TAG ) ).append( Const.CR );

    return xml.toString();
  }

  public StepErrorMeta( VariableSpace variables, Node node, List<StepMeta> steps ) {
    this.variables = variables;

    sourceStep = StepMeta.findStep( steps, XMLHandler.getTagValue( node, "source_step" ) );
    targetStep = StepMeta.findStep( steps, XMLHandler.getTagValue( node, "target_step" ) );
    enabled = "Y".equals( XMLHandler.getTagValue( node, "is_enabled" ) );
    nrErrorsValuename = XMLHandler.getTagValue( node, "nr_valuename" );
    errorDescriptionsValuename = XMLHandler.getTagValue( node, "descriptions_valuename" );
    errorFieldsValuename = XMLHandler.getTagValue( node, "fields_valuename" );
    errorCodesValuename = XMLHandler.getTagValue( node, "codes_valuename" );
    maxErrors = XMLHandler.getTagValue( node, "max_errors" );
    maxPercentErrors = XMLHandler.getTagValue( node, "max_pct_errors" );
    minPercentRows = XMLHandler.getTagValue( node, "min_pct_rows" );
  }

  /**
   * @return the error codes valuename
   */
  public String getErrorCodesValuename() {
    return errorCodesValuename;
  }

  /**
   * @param errorCodesValuename
   *          the error codes valuename to set
   */
  public void setErrorCodesValuename( String errorCodesValuename ) {
    this.errorCodesValuename = errorCodesValuename;
  }

  /**
   * @return the error descriptions valuename
   */
  public String getErrorDescriptionsValuename() {
    return errorDescriptionsValuename;
  }

  /**
   * @param errorDescriptionsValuename
   *          the error descriptions valuename to set
   */
  public void setErrorDescriptionsValuename( String errorDescriptionsValuename ) {
    this.errorDescriptionsValuename = errorDescriptionsValuename;
  }

  /**
   * @return the error fields valuename
   */
  public String getErrorFieldsValuename() {
    return errorFieldsValuename;
  }

  /**
   * @param errorFieldsValuename
   *          the error fields valuename to set
   */
  public void setErrorFieldsValuename( String errorFieldsValuename ) {
    this.errorFieldsValuename = errorFieldsValuename;
  }

  /**
   * @return the nr errors valuename
   */
  public String getNrErrorsValuename() {
    return nrErrorsValuename;
  }

  /**
   * @param nrErrorsValuename
   *          the nr errors valuename to set
   */
  public void setNrErrorsValuename( String nrErrorsValuename ) {
    this.nrErrorsValuename = nrErrorsValuename;
  }

  /**
   * @return the target step
   */
  public StepMeta getTargetStep() {
    return targetStep;
  }

  /**
   * @param targetStep
   *          the target step to set
   */
  public void setTargetStep( StepMeta targetStep ) {
    this.targetStep = targetStep;
  }

  /**
   * @return The source step can send the error rows
   */
  public StepMeta getSourceStep() {
    return sourceStep;
  }

  /**
   * @param sourceStep
   *          The source step can send the error rows
   */
  public void setSourceStep( StepMeta sourceStep ) {
    this.sourceStep = sourceStep;
  }

  /**
   * @return the enabled flag: Is the error handling enabled?
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * @param enabled
   *          the enabled flag to set: Is the error handling enabled?
   */
  public void setEnabled( boolean enabled ) {
    this.enabled = enabled;
  }

  public RowMetaInterface getErrorFields() {
    return getErrorRowMeta( 0L, null, null, null );
  }

  public RowMetaInterface getErrorRowMeta( long nrErrors, String errorDescriptions, String fieldNames,
    String errorCodes ) {
    RowMetaInterface row = new RowMeta();

    String nrErr = variables.environmentSubstitute( getNrErrorsValuename() );
    if ( !Const.isEmpty( nrErr ) ) {
      ValueMetaInterface v = new ValueMeta( nrErr, ValueMetaInterface.TYPE_INTEGER );
      v.setLength( 3 );
      row.addValueMeta( v );
    }
    String errDesc = variables.environmentSubstitute( getErrorDescriptionsValuename() );
    if ( !Const.isEmpty( errDesc ) ) {
      ValueMetaInterface v = new ValueMeta( errDesc, ValueMetaInterface.TYPE_STRING );
      row.addValueMeta( v );
    }
    String errFields = variables.environmentSubstitute( getErrorFieldsValuename() );
    if ( !Const.isEmpty( errFields ) ) {
      ValueMetaInterface v = new ValueMeta( errFields, ValueMetaInterface.TYPE_STRING );
      row.addValueMeta( v );
    }
    String errCodes = variables.environmentSubstitute( getErrorCodesValuename() );
    if ( !Const.isEmpty( errCodes ) ) {
      ValueMetaInterface v = new ValueMeta( errCodes, ValueMetaInterface.TYPE_STRING );
      row.addValueMeta( v );
    }

    return row;
  }

  public void addErrorRowData( Object[] row, int startIndex, long nrErrors, String errorDescriptions,
    String fieldNames, String errorCodes ) {
    int index = startIndex;

    String nrErr = variables.environmentSubstitute( getNrErrorsValuename() );
    if ( !Const.isEmpty( nrErr ) ) {
      row[index] = new Long( nrErrors );
      index++;
    }
    String errDesc = variables.environmentSubstitute( getErrorDescriptionsValuename() );
    if ( !Const.isEmpty( errDesc ) ) {
      row[index] = errorDescriptions;
      index++;
    }
    String errFields = variables.environmentSubstitute( getErrorFieldsValuename() );
    if ( !Const.isEmpty( errFields ) ) {
      row[index] = fieldNames;
      index++;
    }
    String errCodes = variables.environmentSubstitute( getErrorCodesValuename() );
    if ( !Const.isEmpty( errCodes ) ) {
      row[index] = errorCodes;
      index++;
    }
  }

  /**
   * @return the maxErrors
   */
  public String getMaxErrors() {
    return maxErrors;
  }

  /**
   * @param maxErrors
   *          the maxErrors to set
   */
  public void setMaxErrors( String maxErrors ) {
    this.maxErrors = maxErrors;
  }

  /**
   * @return the maxPercentErrors
   */
  public String getMaxPercentErrors() {
    return maxPercentErrors;
  }

  /**
   * @param maxPercentErrors
   *          the maxPercentErrors to set
   */
  public void setMaxPercentErrors( String maxPercentErrors ) {
    this.maxPercentErrors = maxPercentErrors;
  }

  /**
   * @return the minRowsForPercent
   */
  public String getMinPercentRows() {
    return minPercentRows;
  }

  /**
   * @param minRowsForPercent
   *          the minRowsForPercent to set
   */
  public void setMinPercentRows( String minRowsForPercent ) {
    this.minPercentRows = minRowsForPercent;
  }
}
