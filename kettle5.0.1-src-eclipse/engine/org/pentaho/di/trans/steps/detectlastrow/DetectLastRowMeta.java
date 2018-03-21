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

package org.pentaho.di.trans.steps.detectlastrow;

import java.util.List;

import org.pentaho.di.core.CheckResult;
import org.pentaho.di.core.CheckResultInterface;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransMeta.TransformationType;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

/**
 * @author Samatar
 * @since 03June2008 
 */
public class DetectLastRowMeta extends BaseStepMeta implements StepMetaInterface
{
	private static Class<?> PKG = DetectLastRowMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

    /** function result: new value name */
    private String       resultfieldname;
        
    /**
     * @return Returns the resultName.
     */
    public String getResultFieldName()
    {
        return resultfieldname;
    }

    /**
     * @param resultfieldname The resultfieldname to set.
     */
    public void setResultFieldName(String resultfieldname)
    {
        this.resultfieldname = resultfieldname;
    }
    
    public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException
    {
        readData(stepnode);
    }
 
    public Object clone()
    {
        DetectLastRowMeta retval = (DetectLastRowMeta) super.clone();
       
        return retval;
    }

    public void setDefault()
    {
        resultfieldname = "result"; 
    }
    
    public void getFields(RowMetaInterface row, String name, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException {

        if (!Const.isEmpty(resultfieldname))
        {
        	ValueMetaInterface v = new ValueMeta(space.environmentSubstitute(resultfieldname), ValueMeta.TYPE_BOOLEAN);
            v.setOrigin(name);
            row.addValueMeta(v);
        }
    }
	
    public String getXML()
    {
        StringBuilder retval = new StringBuilder();
        retval.append("    " + XMLHandler.addTagValue("resultfieldname", resultfieldname));  
        return retval.toString();
    }

    private void readData(Node stepnode) throws KettleXMLException
    {
    	try
		{
            resultfieldname = XMLHandler.getTagValue(stepnode, "resultfieldname"); 
        }
        catch (Exception e)
        {
            throw new KettleXMLException(BaseMessages.getString(PKG, "DetectLastRowMeta.Exception.UnableToReadStepInfo"), e); 
        }
    }

    public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases) throws KettleException {
      try {
        resultfieldname = rep.getStepAttributeString(id_step, "resultfieldname"); 
      } catch (Exception e) {
        throw new KettleException(BaseMessages.getString(PKG,
            "DetectLastRowMeta.Exception.UnexpectedErrorReadingStepInfo"), e); 
      }
    }

    public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step) throws KettleException
    {
        try
        {
            rep.saveStepAttribute(id_transformation, id_step, "resultfieldname", resultfieldname); 
        }
        catch (Exception e)
        {
            throw new KettleException(BaseMessages.getString(PKG, "DetectLastRowMeta.Exception.UnableToSaveStepInfo") + id_step, e); 
        }
    }

    public void check(List<CheckResultInterface> remarks, TransMeta transMeta, StepMeta stepMeta, RowMetaInterface prev, String input[], String output[], RowMetaInterface info, VariableSpace space, Repository repository, IMetaStore metaStore) {
        CheckResult cr;
        String error_message = ""; 
      
        if (Const.isEmpty(resultfieldname))
        {
            error_message = BaseMessages.getString(PKG, "DetectLastRowMeta.CheckResult.ResultFieldMissing"); 
            cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, error_message, stepMeta);
            remarks.add(cr);
        }
        else
        {
            error_message = BaseMessages.getString(PKG, "DetectLastRowMeta.CheckResult.ResultFieldOK"); 
            cr = new CheckResult(CheckResult.TYPE_RESULT_OK, error_message, stepMeta);
            remarks.add(cr);
        }
      
        // See if we have input streams leading to this step!
        if (input.length > 0)
        {
            cr = new CheckResult(CheckResult.TYPE_RESULT_OK, BaseMessages.getString(PKG, "DetectLastRowMeta.CheckResult.ReceivingInfoFromOtherSteps"), stepMeta); 
            remarks.add(cr);
        }
        else
        {
            cr = new CheckResult(CheckResult.TYPE_RESULT_ERROR, BaseMessages.getString(PKG, "DetectLastRowMeta.CheckResult.NoInpuReceived"), stepMeta); 
            remarks.add(cr);
        }
    }

    public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int cnr, TransMeta transMeta, Trans trans)
    {
        return new DetectLastRow(stepMeta, stepDataInterface, cnr, transMeta, trans);
    }

    public StepDataInterface getStepData()
    {
        return new DetectLastRowData();
    }
    
    public TransformationType[] getSupportedTransformationTypes() {
      return new TransformationType[] { TransformationType.Normal, };
    }
}