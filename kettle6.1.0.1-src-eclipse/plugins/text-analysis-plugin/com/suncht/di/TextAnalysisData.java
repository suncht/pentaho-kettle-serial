package com.suncht.di;

import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.step.BaseStepData;
import org.pentaho.di.trans.step.StepDataInterface;

public class TextAnalysisData extends BaseStepData implements StepDataInterface {
	public RowMetaInterface outputRowMeta;
}
