package com.suncht.di.trans.steps;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

public class TextAnalysis extends BaseStep implements StepInterface {
	private static Class<?> PKG = TextAnalysisMeta.class; // for i18n purposes, needed by Translator2!!

	private TextAnalysisData data;
	private TextAnalysisMeta meta;

	public TextAnalysis(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}

	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {
		meta = (TextAnalysisMeta) smi;
		data = (TextAnalysisData) sdi;

		//		Object[] r = getRow(); // get row, blocks when needed!
		//		if (r == null) // no more input to be expected...
		//		{
		//			setOutputDone();
		//			return false;
		//		}

		data.outputRowMeta = new RowMeta();
		meta.getFields(data.outputRowMeta, getStepname(), null, null, this, repository, metaStore);

		String fileDir = meta.getFileDir();
		if (Const.isEmpty(fileDir)) {
			throw new KettleException(BaseMessages.getString(PKG, "TextAnalysis.Error.FileDirEmpty"));
		}

		File file = new File(fileDir);
		if (!file.exists()) {
			throw new KettleException(BaseMessages.getString(PKG, "TextAnalysis.Error.FileNoExists"));
		}

		//Object[] outputRow = RowDataUtil.addValueData(r, data.outputRowMeta.size() - 1, "dummy value");

		Object[] rowData = RowDataUtil.allocateRowData(data.outputRowMeta.size());
		if (file.isFile()) {
			String content = TikaUtil.getBody(file);
			if (StringUtils.isNotBlank(content)) {
				// 将结果传递到下一个步骤
				int index = 0;
				rowData[index++] = file.getName();
				rowData[index++] = content;

				putRow(data.outputRowMeta, rowData);
			}
		} else if (file.isDirectory()) {
			File[] files = file.listFiles();
			for (File f : files) {
				String content = TikaUtil.getBody(file);
				if (StringUtils.isNotBlank(content)) {
					// 将结果传递到下一个步骤
					putRow(data.outputRowMeta, new Object[] { file.getPath(), content });
				}
			}
		}

		// 结束状态设置
		setOutputDone();
		return false;
	}

	@Override
	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		return super.init(smi, sdi);
	}

	@Override
	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		super.dispose(smi, sdi);
	}
}
