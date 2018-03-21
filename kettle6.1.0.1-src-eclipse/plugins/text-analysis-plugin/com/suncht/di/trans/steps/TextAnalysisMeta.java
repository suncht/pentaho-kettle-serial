package com.suncht.di.trans.steps;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.core.variables.VariableSpace;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStepMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Node;

public class TextAnalysisMeta extends BaseStepMeta implements StepMetaInterface {
	private String fileDir = "";

	@Override
	public void setDefault() {
	}

	@Override
	public String getDialogClassName() {
		// 在这里可以自定义step dialog的全限定类名
		return super.getDialogClassName();
	}

	@Override
	public StepInterface getStep(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		return new TextAnalysis(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	@Override
	public StepDataInterface getStepData() {
		return new TextAnalysisData();
	}

	@Override
	public void getFields(RowMetaInterface r, String name, RowMetaInterface[] info, StepMeta nextStep, VariableSpace space, Repository repository, IMetaStore metaStore) throws KettleStepException {
		ValueMetaInterface vValue = null;
		try {
			vValue = ValueMetaFactory.createValueMeta("fileName", ValueMetaInterface.TYPE_STRING);
			r.addValueMeta(vValue);

			vValue = ValueMetaFactory.createValueMeta("content", ValueMetaInterface.TYPE_STRING);
			r.addValueMeta(vValue);
		} catch (KettlePluginException e) {
			throw new KettleStepException(e);
		}

	}

	// 返回DubboClientMeta信息,以xml格式返回
	@Override
	public String getXML() throws KettleException {
		StringBuffer retval = new StringBuffer();
		retval.append("    " + XMLHandler.addTagValue("fileDir", getFileDir()));

		return retval.toString();
	}

	// 从资源库中获取DubboClientMeta信息
	@Override
	public void readRep(Repository rep, IMetaStore metaStore, ObjectId id_step, List<DatabaseMeta> databases) throws KettleException {
		// 应用配置
		String _fileDir = rep.getStepAttributeString(id_step, "fileDir");
		if (StringUtils.isNotBlank(_fileDir)) {
			setFileDir(_fileDir);
		}
	}

	// 从xml节点中加载DubboClientMeta信息
	@Override
	public void loadXML(Node stepnode, List<DatabaseMeta> databases, IMetaStore metaStore) throws KettleXMLException {
		// 应用配置
		setFileDir(XMLHandler.getTagValue(stepnode, "fileDir"));
	}

	// 保存DubboClientMeta信息到资源库中
	@Override
	public void saveRep(Repository rep, IMetaStore metaStore, ObjectId id_transformation, ObjectId id_step) throws KettleException {
		// 应用配置
		rep.saveStepAttribute(id_transformation, id_step, "fileDir", getFileDir());
	}

	public String getFileDir() {
		return fileDir;
	}

	public void setFileDir(String fileDir) {
		this.fileDir = fileDir;
	}

	@Override
	public Object clone() {
		return super.clone();
	}

}
