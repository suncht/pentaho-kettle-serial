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

package org.pentaho.di.trans.steps.rest;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.core.MediaType;

import org.apache.commons.httpclient.auth.AuthScope;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.encryption.Encr;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.ApacheHttpClientConfig;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.core.util.MultivaluedMapImpl;


/**
 * @author Samatar
 * @since 16-jan-2011
 *
 */

public class Rest extends BaseStep implements StepInterface
{
	private static Class<?> PKG = RestMeta.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private RestMeta meta;
	private RestData data;
	
	public Rest(StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans)
	{
		super(stepMeta, stepDataInterface, copyNr, transMeta, trans);
	}
	

	
	private Object[] callRest(Object[] rowData) throws KettleException
    {
		// get dynamic url ?
		if(meta.isUrlInField()) {
			data.realUrl= data.inputRowMeta.getString(rowData,data.indexOfUrlField);
		}
		// get dynamic method?
		if(meta.isDynamicMethod()) {
			data.method= data.inputRowMeta.getString(rowData,data.indexOfMethod);
			if(Const.isEmpty(data.method)) {
				throw new KettleException(BaseMessages.getString(PKG, "Rest.Error.MethodMissing"));
			}
		}
		
		
		WebResource webResource =null;

		Client client=null;
		Object[] newRow = null;
	    if(rowData!=null) newRow=rowData.clone();

      	try {
      		if(isDetailed()) logDetailed(BaseMessages.getString(PKG, "Rest.Log.ConnectingToURL",data.realUrl));
      		
      		// create an instance of the com.sun.jersey.api.client.Client class
      		client = getClient();
      		// create a WebResource object, which encapsulates a web resource for the client
      		webResource  = client.resource(data.realUrl);     			
    		
            // used for calculating the responseTime
            long startTime = System.currentTimeMillis();
            

			if(data.useParams) {
				// Add parameters
				for(int i=0; i<data.nrParams; i++) {
					MultivaluedMapImpl queryParams = new MultivaluedMapImpl();
					String value = data.inputRowMeta.getString(rowData, data.indexOfParamFields[i]);
					queryParams.add(data.paramNames[i], value);
	        		if(isDebug()) logDebug(BaseMessages.getString(PKG, "Rest.Log.parameterValue",data.paramNames[i],value));
					webResource = webResource.queryParams(queryParams);
				}
			}

         WebResource.Builder builder = webResource.getRequestBuilder();
         if(data.useHeaders) {
				// Add headers
			   for(int i=0; i<data.nrheader; i++) {
					String value = data.inputRowMeta.getString(rowData, data.indexOfHeaderFields[i]);
					
					//  unsure if an already set header will be returned to builder
					builder = builder.header(data.headerNames[i], value);
	        		if(isDebug()) logDebug(BaseMessages.getString(PKG, "Rest.Log.HeaderValue",data.headerNames[i],value));
				}
			}

			ClientResponse response=null;
			String entityString="";
			if(data.useBody) {
				// Set Http request entity
				entityString=Const.NVL(data.inputRowMeta.getString(rowData, data.indexOfBodyField), "");
        		if(isDebug()) logDebug(BaseMessages.getString(PKG, "Rest.Log.BodyValue", entityString));
			}
			try {
				if(data.method.equals(RestMeta.HTTP_METHOD_GET)) {
					response = builder.get(ClientResponse.class);
				}else if (data.method.equals(RestMeta.HTTP_METHOD_POST)) {
	        		response = builder.type(data.mediaType).post(ClientResponse.class, entityString);
				}else if (data.method.equals(RestMeta.HTTP_METHOD_PUT)) {
	        		response = builder.type(data.mediaType).put(ClientResponse.class, entityString);
				}else if (data.method.equals(RestMeta.HTTP_METHOD_DELETE)) {
	        		response = builder.type(data.mediaType).delete(ClientResponse.class, entityString);
				}else if (data.method.equals(RestMeta.HTTP_METHOD_HEAD)) {
					response = builder.head();
				}else if (data.method.equals(RestMeta.HTTP_METHOD_OPTIONS)) {
					response = builder.options(ClientResponse.class);
				}else {
					throw new KettleException(BaseMessages.getString(PKG, "Rest.Error.UnknownMethod", data.method));
				}
			}catch(UniformInterfaceException u) {
				response= u.getResponse();
			}
            
			// Get response time
			long responseTime = System.currentTimeMillis() - startTime;
            if (isDetailed()) logDetailed(BaseMessages.getString(PKG, "Rest.Log.ResponseTime", String.valueOf(responseTime),data.realUrl));
			
			// Get status
            int status = response.getStatus();
		    // Display status code
            if(isDebug()) logDebug(BaseMessages.getString(PKG, "Rest.Log.ResponseCode",""+status));

            // Get Response
            String body = response.getEntity(String.class);
            
			// for output
            int returnFieldsOffset=data.inputRowMeta.size();
		    // add response to output
            if(!Const.isEmpty(data.resultFieldName)) {
            	newRow=RowDataUtil.addValueData(newRow, returnFieldsOffset, body);
            	returnFieldsOffset++;
            }
            
            // add status to output
            if (!Const.isEmpty(data.resultCodeFieldName)) {
            	newRow=RowDataUtil.addValueData(newRow, returnFieldsOffset, new Long(status));
            	returnFieldsOffset++;
            }  
            
            // add response time to output
            if (!Const.isEmpty(data.resultResponseFieldName)) {
            	newRow=RowDataUtil.addValueData(newRow, returnFieldsOffset, new Long(responseTime));
            }             
        } catch(Exception e) {
            throw new KettleException(BaseMessages.getString(PKG, "Rest.Error.CanNotReadURL",data.realUrl), e);

        } finally {
         if(webResource !=null) {
        	 webResource =null;
         }
         if(client!=null) {
        	 client.destroy();
         }
        }
  		
        return newRow;
    }
	private Client getClient() {

		Client c= ApacheHttpClient.create(data.config);
		if(data.basicAuthentication!=null) {
			c.addFilter(data.basicAuthentication); 
		}
		return c;
	}
	private void setConfig() throws KettleException  {
		
		if(data.config==null) {
			 //Use ApacheHttpClient for supporting proxy authentication.
			data.config = new DefaultApacheHttpClientConfig();
		
			if(!Const.isEmpty(data.realProxyHost)) {
				// PROXY CONFIGURATION
				data.config.getProperties().put(DefaultApacheHttpClientConfig.PROPERTY_PROXY_URI, "http://" + data.realProxyHost + ":" + data.realProxyPort);
				if (!Const.isEmpty(data.realHttpLogin) && !Const.isEmpty(data.realHttpPassword)) {
					data.config.getState().setProxyCredentials(AuthScope.ANY_REALM, data.realProxyHost, data.realProxyPort, data.realHttpLogin, data.realHttpPassword);
	            } 
			}else {
				if(!Const.isEmpty(data.realHttpLogin)) {
					// Basic authentication
					data.basicAuthentication = new HTTPBasicAuthFilter(data.realHttpLogin, data.realHttpPassword); 
				}
			}
		    if(meta.isPreemptive()) {
		    	data.config.getProperties().put(ApacheHttpClientConfig.PROPERTY_PREEMPTIVE_AUTHENTICATION, true);
		    }
			
		   // SSL TRUST STORE CONFIGURATION	
		   if(!Const.isEmpty(data.trustStoreFile)) {

			   try {
				   KeyStore trustStore = KeyStore.getInstance("JKS");     
				   trustStore.load(new FileInputStream(data.trustStoreFile),data.trustStorePassword.toCharArray());     
				   TrustManagerFactory tmf=TrustManagerFactory.getInstance("SunX509");     
				   tmf.init(trustStore); 
				   
				   SSLContext ctx = SSLContext.getInstance("SSL");
				   ctx.init(null, tmf.getTrustManagers(), null); 
				   
				   HostnameVerifier hv = new HostnameVerifier() { 
			             public boolean verify(String hostname, SSLSession session) { 
			                if(isDebug()) logDebug("Warning: URL Host: " + hostname  + " vs. " + session.getPeerHost()); 
			                 return true; 
			             } 
			         }; 

				   data.config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv,ctx));  
			  
			   } catch (NoSuchAlgorithmException e) {     
				 throw new KettleException(BaseMessages.getString(PKG, "Rest.Error.NoSuchAlgorithm"), e);
			   } catch (KeyStoreException e) {     
				   throw new KettleException(BaseMessages.getString(PKG, "Rest.Error.KeyStoreException"), e);
			   } catch (CertificateException e) {
				   throw new KettleException(BaseMessages.getString(PKG, "Rest.Error.CertificateException"), e);
			   } catch (FileNotFoundException e) {     
				   throw new KettleException(BaseMessages.getString(PKG, "Rest.Error.FileNotFound", data.trustStoreFile), e);
			   } catch (IOException e) {    
				   throw new KettleException(BaseMessages.getString(PKG, "Rest.Error.IOException"), e);
			   } catch (KeyManagementException e) {     
				   throw new KettleException(BaseMessages.getString(PKG, "Rest.Error.KeyManagementException"), e);
			   }
		   }

		}
	}
    public boolean processRow(StepMetaInterface smi, StepDataInterface sdi) throws KettleException
	{
		meta=(RestMeta)smi;
		data=(RestData)sdi;
		
		
		Object[] r=getRow();       // Get row from input rowset & set row busy!
		
		if (r==null) {
			// no more input to be expected...
			setOutputDone();
			return false;
		}
		if ( first ) {
			first=false;
			
			data.inputRowMeta = getInputRowMeta();
			data.outputRowMeta = data.inputRowMeta.clone();
			meta.getFields(data.outputRowMeta, getStepname(), null, null, this, repository, metaStore);
			
			// Let's set URL
			if(meta.isUrlInField()) {
				if(Const.isEmpty(meta.getUrlField())) {
					logError(BaseMessages.getString(PKG, "Rest.Log.NoField"));
					throw new KettleException(BaseMessages.getString(PKG, "Rest.Log.NoField"));
				}
				
				// cache the position of the field			
				if (data.indexOfUrlField<0) {	
					String realUrlfieldName= environmentSubstitute(meta.getUrlField());
					data.indexOfUrlField =data.inputRowMeta.indexOfValue(realUrlfieldName);
					if (data.indexOfUrlField<0) {
						// The field is unreachable !
						throw new KettleException(BaseMessages.getString(PKG, "Rest.Exception.ErrorFindingField",realUrlfieldName)); 
					}
				}
			}else {
				// Static URL
				data.realUrl=environmentSubstitute(meta.getUrl());
			}
			
			// Check Method
			if(meta.isDynamicMethod()) {
				String field= environmentSubstitute(meta.getMethodFieldName());
				if(Const.isEmpty(field))  {
					throw new KettleException(BaseMessages.getString(PKG, "Rest.Exception.MethodFieldMissing"));
				}
				data.indexOfMethod =data.inputRowMeta.indexOfValue(field);
				if (data.indexOfMethod<0) {
					// The field is unreachable !
					throw new KettleException(BaseMessages.getString(PKG, "Rest.Exception.ErrorFindingField",field)); 
				}
			}
			
			// set Headers
			int nrargs=meta.getHeaderName()==null?0:meta.getHeaderName().length;
			if(nrargs>0) {
				data.nrheader=nrargs;
				data.indexOfHeaderFields= new int[nrargs];
				data.headerNames= new String[nrargs];
				for (int i=0;i<nrargs;i++)  {
					// split into body / header
					data.headerNames[i]=environmentSubstitute(meta.getHeaderName()[i]);
					String field =environmentSubstitute(meta.getHeaderField()[i]);
	
					if(Const.isEmpty(field)) {
						throw new KettleException(BaseMessages.getString(PKG, "Rest.Exception.HeaderFieldEmpty")); 
					}
					data.indexOfHeaderFields[i]=data.inputRowMeta.indexOfValue(field);
					if(data.indexOfHeaderFields[i]<0) {
						throw new KettleException(BaseMessages.getString(PKG, "Rest.Exception.ErrorFindingField",field));
					}
				}

				data.useHeaders=true;
			}
			
			if(RestMeta.isActiveParameters(meta.getMethod())) {
				// Parameters
				int nrparams= meta.getParameterField()==null?0:meta.getParameterField().length;
				if(nrparams>0) {
				   data.nrParams=nrparams;
					data.paramNames=new String[nrparams];
					data.indexOfParamFields= new int[nrparams];
					for (int i=0;i<nrparams;i++)  {
						data.paramNames[i]=environmentSubstitute(meta.getParameterName()[i]);
						String field =environmentSubstitute(meta.getParameterField()[i]);
						if(Const.isEmpty(field)) {
							throw new KettleException(BaseMessages.getString(PKG, "Rest.Exception.ParamFieldEmpty")); 
						}
						data.indexOfParamFields[i]=data.inputRowMeta.indexOfValue(field);
						if(data.indexOfParamFields[i]<0) {
							throw new KettleException(BaseMessages.getString(PKG, "Rest.Exception.ErrorFindingField",field));
						}
					}
					data.useParams=true;
				}
			}

			// Do we need to set body
			if(RestMeta.isActiveBody(meta.getMethod())) {
				String field= environmentSubstitute(meta.getBodyField());
				if(!Const.isEmpty(field)) {
					data.indexOfBodyField= data.inputRowMeta.indexOfValue(field);
					if(data.indexOfBodyField<0) {
						throw new KettleException(BaseMessages.getString(PKG, "Rest.Exception.ErrorFindingField",field));
					}
					data.useBody=true;
				}
			}
		} // end if first

		
		try {
			
	        Object[] outputRowData = callRest(r);
	        
	    	putRow(data.outputRowMeta, outputRowData);  // copy row to output rowset(s);
			
            if (checkFeedback(getLinesRead()))   {
            	if(isDetailed()) logDetailed(BaseMessages.getString(PKG, "Rest.LineNumber")+getLinesRead()); 
            }
		} catch(KettleException e) {
			 boolean sendToErrorRow=false;
			 String errorMessage = null;
			 
			if (getStepMeta().isDoingErrorHandling()){
		         sendToErrorRow = true;
		         errorMessage = e.toString();
			} else {
				logError(BaseMessages.getString(PKG, "Rest.ErrorInStepRunning")+e.getMessage()); 
				setErrors(1);
                logError(Const.getStackTracker(e));
				stopAll();
				setOutputDone();  // signal end to receiver(s)
				return false;
			}

			if (sendToErrorRow) {
			   // Simply add this row to the error row
			   putError(getInputRowMeta(), r, 1, errorMessage, null, "Rest001");
			}

		}
			
		return true;
	}
	
	public boolean init(StepMetaInterface smi, StepDataInterface sdi)
	{
		meta=(RestMeta)smi;
		data=(RestData)sdi;

		if (super.init(smi, sdi)) {
			
			data.resultFieldName=environmentSubstitute(meta.getFieldName());
			data.resultCodeFieldName=environmentSubstitute(meta.getResultCodeFieldName());
			data.resultResponseFieldName=environmentSubstitute(meta.getResponseTimeFieldName());
			
			
			// get authentication settings once
			data.realProxyHost=environmentSubstitute(meta.getProxyHost());
			data.realProxyPort= Const.toInt(environmentSubstitute(meta.getProxyPort()), 8080);
			data.realHttpLogin=environmentSubstitute(meta.getHttpLogin());
			data.realHttpPassword=Encr.decryptPasswordOptionallyEncrypted(environmentSubstitute(meta.getHttpPassword()));
			
			
			if(!meta.isDynamicMethod()) {
				data.method= environmentSubstitute(meta.getMethod());
				if(Const.isEmpty(data.method)) {
					logError(BaseMessages.getString(PKG, "Rest.Error.MethodMissing"));
					return false;
				}
			}
			
			
			data.trustStoreFile= environmentSubstitute(meta.getTrustStoreFile());
			data.trustStorePassword= environmentSubstitute(meta.getTrustStorePassword());
			
			String applicationType= Const.NVL(meta.getApplicationType(), "");
            if(applicationType.equals(RestMeta.APPLICATION_TYPE_XML)) {
                data.mediaType =MediaType.APPLICATION_XML_TYPE;
            } else if(applicationType.equals(RestMeta.APPLICATION_TYPE_JSON)) {
                data.mediaType =MediaType.APPLICATION_JSON_TYPE;
            } else if(applicationType.equals(RestMeta.APPLICATION_TYPE_OCTET_STREAM)) {
                data.mediaType =MediaType.APPLICATION_OCTET_STREAM_TYPE;
            } else if(applicationType.equals(RestMeta.APPLICATION_TYPE_XHTML)) {
                data.mediaType =MediaType.APPLICATION_XHTML_XML_TYPE;
            } else if(applicationType.equals(RestMeta.APPLICATION_TYPE_FORM_URLENCODED)) {
                data.mediaType =MediaType.APPLICATION_FORM_URLENCODED_TYPE;
            } else if(applicationType.equals(RestMeta.APPLICATION_TYPE_ATOM_XML)) {
                data.mediaType =MediaType.APPLICATION_ATOM_XML_TYPE;
            } else if(applicationType.equals(RestMeta.APPLICATION_TYPE_SVG_XML)) {
            	data.mediaType =MediaType.APPLICATION_SVG_XML_TYPE;
            } else if (applicationType.equals(RestMeta.APPLICATION_TYPE_TEXT_XML)) {
              data.mediaType =MediaType.TEXT_XML_TYPE;
            } else {
                data.mediaType =MediaType.TEXT_PLAIN_TYPE;
            }
	           
			try {
				setConfig();
			}catch(Exception e){
				logError(BaseMessages.getString(PKG, "Rest.Error.Config"), e);
				return false;
			}
			
		    return true;
		}
		return false;
	}
		
	public void dispose(StepMetaInterface smi, StepDataInterface sdi)
	{
	    meta = (RestMeta)smi;
	    data = (RestData)sdi;
	    
	    data.config=null;
	    data.headerNames=null;
	    data.indexOfHeaderFields=null;
	    data.paramNames=null;
	 
	    super.dispose(smi, sdi);
	}

}
