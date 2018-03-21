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

package org.pentaho.di.trans.steps.googleanalytics;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.encryption.Encr;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaFactory;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import com.google.gdata.client.analytics.AnalyticsService;
import com.google.gdata.client.analytics.DataQuery;
import com.google.gdata.data.analytics.DataEntry;
import com.google.gdata.data.analytics.DataFeed;
import com.google.gdata.data.analytics.DataSource;
import com.google.gdata.data.analytics.Dimension;
import com.google.gdata.data.analytics.Metric;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;



public class GaInputStep extends BaseStep implements StepInterface {

	private GaInputStepData data;
	private GaInputStepMeta meta;
	
	//private static Class<?> PKG = GaInputStep.class; // for i18n purposes
	
	public GaInputStep(StepMeta s, StepDataInterface stepDataInterface, int c, TransMeta t, Trans dis) {
		super(s, stepDataInterface, c, t, dis);
	}

	public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException {

		meta = (GaInputStepMeta) smi;
		data = (GaInputStepData) sdi;

		if (first) {
			
			first = false;
			
			data.outputRowMeta = new RowMeta();
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this, repository, metaStore);
			
            // stores the indices where to look for the key fields in the input rows
            data.conversionMeta = new ValueMetaInterface[meta.getFeedField().length];

            
            for (int i=0;i<meta.getFeedField().length;i++){
            	
            	// get output and from-string conversion format for each field
                ValueMetaInterface returnMeta = data.outputRowMeta.getValueMeta(i);
                
                ValueMetaInterface conversionMeta;
                if (meta.getFeedFieldType()[i].equals(GaInputStepMeta.FIELD_TYPE_CONFIDENCE_INTERVAL)){
                	conversionMeta = ValueMetaFactory.cloneValueMeta(returnMeta, ValueMetaInterface.TYPE_NUMBER);
                }
                else{
                	conversionMeta = ValueMetaFactory.cloneValueMeta(returnMeta, ValueMetaInterface.TYPE_STRING);	
                }
                
                conversionMeta.setConversionMask(meta.getConversionMask()[i]);
                
    			conversionMeta.setDecimalSymbol("."); // google analytics is en-US
    			conversionMeta.setGroupingSymbol(null); // google analytics uses no grouping symbol

                data.conversionMeta[i] = conversionMeta;
                
            }

		}
		
		// generate output row, make it correct size
		Object[] outputRow = RowDataUtil.allocateRowData(data.outputRowMeta.size());
		
		DataEntry entry = getNextDataEntry();
		
		if (entry != null && (meta.getRowLimit() <= 0 || getLinesWritten() < meta.getRowLimit())){ // another record to process
			// fill the output fields with look up data
	        for (int i = 0;i<meta.getFeedField().length;i++){
	        	
	        	String value = null;
	        	String fieldName = environmentSubstitute(meta.getFeedField()[i]);
	        	String fieldType = meta.getFeedFieldType()[i];
	        	
	        	// confidence intervals
	        	if(fieldType.equals(GaInputStepMeta.FIELD_TYPE_CONFIDENCE_INTERVAL)){
	        		Metric metric = entry.getMetric(fieldName);
	        		Double interval = metric.getConfidenceInterval();
	        		outputRow[i] = data.outputRowMeta.getValueMeta(i).convertData(data.conversionMeta[i], interval);
	        	}
	        	else if (fieldType.equals(GaInputStepMeta.FIELD_TYPE_DIMENSION)){
	            	Dimension dim = entry.getDimension(fieldName);
	            	value = dim.getValue();
	            	outputRow[i] = data.outputRowMeta.getValueMeta(i).convertData(data.conversionMeta[i], value);
	        	}
	        	else if (fieldType.equals(GaInputStepMeta.FIELD_TYPE_METRIC)){
	            	Metric metric = entry.getMetric(fieldName);
	            	value = metric.getValue();
	            	outputRow[i] = data.outputRowMeta.getValueMeta(i).convertData(data.conversionMeta[i], value);
	        	}
	        	else if (fieldType.equals(GaInputStepMeta.FIELD_TYPE_DATA_SOURCE_PROPERTY)){
	        		DataSource d = data.feed.getDataSources().size()>0?data.feed.getDataSources().get(0):null;
	        		if(d!= null){
	        			value = d.getProperty(fieldName); 
	        			outputRow[i] = data.outputRowMeta.getValueMeta(i).convertData(data.conversionMeta[i], value);
	        		}
	        	}
	        	else if (fieldType.equals(GaInputStepMeta.FIELD_TYPE_DATA_SOURCE_FIELD)){
	        		DataSource d = data.feed.getDataSources().size()>0?data.feed.getDataSources().get(0):null;
	        		if(d!= null){
	        			
	        			if (fieldName.equalsIgnoreCase(GaInputStepMeta.FIELD_DATA_SOURCE_TABLE_ID)){
		        			value = d.getTableId().getValue(); 
		        			outputRow[i] = data.outputRowMeta.getValueMeta(i).convertData(data.conversionMeta[i], value);
	        				
	        			}
	        			else if (fieldName.equalsIgnoreCase(GaInputStepMeta.FIELD_DATA_SOURCE_TABLE_NAME)){
		        			value = d.getTableName().getValue(); 
		        			outputRow[i] = data.outputRowMeta.getValueMeta(i).convertData(data.conversionMeta[i], value);
	        				
	        			}
	        			
	        		}
	        	}	        	
	        	
	        }

	        // copy row to possible alternate rowset(s)
			putRow(data.outputRowMeta, outputRow); 

			// Some basic logging
			if (checkFeedback(getLinesWritten())) {
				if (log.isBasic()) logBasic("Linenr " + getLinesWritten()); 
			}
			return true;			
		}
		else{
			setOutputDone();
			return false;
		}
		
		
	}

	protected DataQuery getQuery(){
		
		DataQuery query = null;
		try {
			query = new DataQuery(new URL(GaInputStepMeta.GA_DATA_URL));
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
			return null;
		}
		
		query.setIds(meta.isUseCustomTableId()?environmentSubstitute(meta.getGaCustomTableId()):meta.getGaProfileTableId());
		
		query.setStartDate(environmentSubstitute(meta.getStartDate()));
		query.setEndDate(environmentSubstitute(meta.getEndDate()));
		query.setDimensions(environmentSubstitute(meta.getDimensions()));
		query.setMetrics(environmentSubstitute(meta.getMetrics()));

		if (meta.isUseSegment()){
			if (meta.isUseCustomSegment()) {
				query.setSegment(environmentSubstitute(meta.getCustomSegment()));
			} else {
				query.setSegment(meta.getSegmentId());
			}
		}

		if (!Const.isEmpty(meta.getFilters())) {
			query.setFilters(environmentSubstitute(meta.getFilters()));
		}
		if (!Const.isEmpty(meta.getSort())) {
			query.setSort(environmentSubstitute(meta.getSort()));
		}
		
		if (!Const.isEmpty(meta.getGaApiKey())){
			// allow to use optionally encrypted environment variables
			query.setStringCustomParameter("key", Encr.decryptPasswordOptionallyEncrypted(environmentSubstitute(meta.getGaApiKey())));	
		}

		return query;
		
	}	
	
	private DataEntry getNextDataEntry() throws KettleException {
		
		// no query prepared yet?
		if (data.query == null){
			
			
			data.query = getQuery();
			// use default max results for now
			//data.query.setMaxResults(10000);

			if (log.isDetailed()){
				logDetailed("querying google analytics: "+data.query.getUrl());
			}
			
			String email = environmentSubstitute(meta.getGaEmail());
			// allow to use optionally encrypted environment variables
			String pass = Encr.decryptPasswordOptionallyEncrypted(environmentSubstitute(meta.getGaPassword()));

			AnalyticsService analyticsService = new AnalyticsService(environmentSubstitute(meta.getGaAppName()));
			
			try{
			
				analyticsService.setUserCredentials(email, pass);
				data.feed = analyticsService.getFeed(data.query.getUrl(), DataFeed.class);
				data.entryIndex = 0;

			} catch (AuthenticationException e1) {
				throw new KettleException(e1);

			} catch (IOException e2) {
				throw new KettleException(e2);
				
			} catch (ServiceException e3) {
				throw new KettleException(e3);
			}			
			
		}
		// query is there, check whether we hit the last entry and requery as necessary
		else if (data.entryIndex >= data.feed.getEntries().size()){
			if (data.feed.getStartIndex()+data.entryIndex <= data.feed.getTotalResults()){
				// need to query for next page
				data.query.setStartIndex(data.feed.getStartIndex()+data.entryIndex);

				if (log.isDetailed()){
					logDetailed("querying google analytics: "+data.query.getUrl());
				}
				
				String email = environmentSubstitute(meta.getGaEmail());
				String pass = environmentSubstitute(meta.getGaPassword());

				AnalyticsService analyticsService = new AnalyticsService(environmentSubstitute(meta.getGaAppName()));
				
				try{
				
					analyticsService.setUserCredentials(email, pass);
					data.feed = analyticsService.getFeed(data.query.getUrl(), DataFeed.class);
					data.entryIndex = 0;

				} catch (AuthenticationException e1) {
					throw new KettleException(e1);

				} catch (IOException e2) {
					throw new KettleException(e2);
					
				} catch (ServiceException e3) {
					throw new KettleException(e3);
				}			
				
				
			}
			                                 
		}
		
		List<DataEntry> entries = data.feed.getEntries();
		if (data.entryIndex < entries.size()){
			return entries.get(data.entryIndex++);
		}
		else{
			return null; // end of feed
		}
	}

	public boolean init(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (GaInputStepMeta) smi;
		data = (GaInputStepData) sdi;
						       
		return super.init(smi, sdi);
	}

	public void dispose(StepMetaInterface smi, StepDataInterface sdi) {
		meta = (GaInputStepMeta) smi;
		data = (GaInputStepData) sdi;
		
		super.dispose(smi, sdi);
	}

}
