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

package org.pentaho.di.trans.steps.http;

import java.io.InputStreamReader;
import java.net.UnknownHostException;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;
import org.pentaho.di.cluster.SlaveConnectionManager;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.exception.KettleValueException;
import org.pentaho.di.core.row.RowDataUtil;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.BaseStep;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;

/**
 * Retrieves values from a database by calling database stored procedures or functions
 *
 * @author Matt
 * @since 26-apr-2003
 */
public class HTTP extends BaseStep implements StepInterface {
  private static Class<?> PKG = HTTPMeta.class; // for i18n purposes, needed by Translator2!! $NON-NLS-1$

  private HTTPMeta meta;
  private HTTPData data;

  public HTTP( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans ) {
    super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
  }

  private Object[] execHttp( RowMetaInterface rowMeta, Object[] row ) throws KettleException {
    if ( first ) {
      first = false;
      data.argnrs = new int[meta.getArgumentField().length];

      for ( int i = 0; i < meta.getArgumentField().length; i++ ) {
        data.argnrs[i] = rowMeta.indexOfValue( meta.getArgumentField()[i] );
        if ( data.argnrs[i] < 0 ) {
          logError( BaseMessages.getString( PKG, "HTTP.Log.ErrorFindingField" ) + meta.getArgumentField()[i] + "]" );
          throw new KettleStepException( BaseMessages.getString( PKG, "HTTP.Exception.CouldnotFindField", meta
              .getArgumentField()[i] ) );
        }
      }
    }

    return callHttpService( rowMeta, row );
  }

  private Object[] callHttpService( RowMetaInterface rowMeta, Object[] rowData ) throws KettleException {
    String url = determineUrl( rowMeta, rowData );
    try {
      if ( isDetailed() ) {
        logDetailed( BaseMessages.getString( PKG, "HTTP.Log.Connecting", url ) );
      }

      // Prepare HTTP get
      //
      HttpClient httpclient = SlaveConnectionManager.getInstance().createHttpClient();
      HttpMethod method = new GetMethod( url );

      // Set timeout
      if ( data.realConnectionTimeout > -1 ) {
        httpclient.getHttpConnectionManager().getParams().setConnectionTimeout( data.realConnectionTimeout );
      }
      if ( data.realSocketTimeout > -1 ) {
        httpclient.getHttpConnectionManager().getParams().setSoTimeout( data.realSocketTimeout );
      }

      if ( !Const.isEmpty( data.realHttpLogin ) ) {
        httpclient.getParams().setAuthenticationPreemptive( true );
        Credentials defaultcreds = new UsernamePasswordCredentials( data.realHttpLogin, data.realHttpPassword );
        httpclient.getState().setCredentials( AuthScope.ANY, defaultcreds );
      }

      HostConfiguration hostConfiguration = new HostConfiguration();
      if ( !Const.isEmpty( data.realProxyHost ) ) {
        hostConfiguration.setProxy( data.realProxyHost, data.realProxyPort );
      }

      // Add Custom HTTP headers
      if ( data.useHeaderParameters ) {
        for ( int i = 0; i < data.header_parameters_nrs.length; i++ ) {
          method.addRequestHeader( data.headerParameters[i].getName(), data.inputRowMeta.getString( rowData,
              data.header_parameters_nrs[i] ) );
          if ( isDebug() ) {
            log.logDebug( BaseMessages.getString( PKG, "HTTPDialog.Log.HeaderValue",
                data.headerParameters[i].getName(), data.inputRowMeta
                    .getString( rowData, data.header_parameters_nrs[i] ) ) );
          }
        }
      }

      InputStreamReader inputStreamReader = null;
      Object[] newRow = null;
      if ( rowData != null ) {
        newRow = rowData.clone();
      }
      // Execute request
      //
      try {
        // used for calculating the responseTime
        long startTime = System.currentTimeMillis();

        int statusCode = httpclient.executeMethod( hostConfiguration, method );

        // calculate the responseTime
        long responseTime = System.currentTimeMillis() - startTime;
        if ( log.isDetailed() ) {
          log.logDetailed( BaseMessages.getString( PKG, "HTTP.Log.ResponseTime", responseTime, url ) );
        }

        String body = null;
        // The status code
        if ( isDebug() ) {
          logDebug( BaseMessages.getString( PKG, "HTTP.Log.ResponseStatusCode", "" + statusCode ) );
        }

        if ( statusCode != -1 ) {
          if ( statusCode == 204 ) {
            body = "";
          } else {
            // if the response is not 401: HTTP Authentication required
            if ( statusCode != 401 ) {
              // guess encoding
              //
              String encoding = meta.getEncoding();

              // Try to determine the encoding from the Content-Type value
              //
              if ( Const.isEmpty( encoding ) ) {
                String contentType = method.getResponseHeader( "Content-Type" ).getValue();
                if ( contentType != null && contentType.contains( "charset" ) ) {
                  encoding = contentType.replaceFirst( "^.*;\\s*charset\\s*=\\s*", "" ).replace( "\"", "" ).trim();
                }
              }

              if ( isDebug() ) {
                log.logDebug( toString(), BaseMessages.getString( PKG, "HTTP.Log.ResponseHeaderEncoding", encoding ) );
              }

              // the response
              if ( !Const.isEmpty( encoding ) ) {
                inputStreamReader = new InputStreamReader( method.getResponseBodyAsStream(), encoding );
              } else {
                inputStreamReader = new InputStreamReader( method.getResponseBodyAsStream() );
              }
              StringBuffer bodyBuffer = new StringBuffer();

              int c;
              while ( ( c = inputStreamReader.read() ) != -1 ) {
                bodyBuffer.append( (char) c );
              }

              inputStreamReader.close();

              body = bodyBuffer.toString();
              if ( isDebug() ) {
                logDebug( "Response body: " + body );
              }

            } else { // the status is a 401
              throw new KettleStepException( BaseMessages
                  .getString( PKG, "HTTP.Exception.Authentication", data.realUrl ) );

            }
          }
        }

        int returnFieldsOffset = rowMeta.size();
        if ( !Const.isEmpty( meta.getFieldName() ) ) {
          newRow = RowDataUtil.addValueData( newRow, returnFieldsOffset, body );
          returnFieldsOffset++;
        }

        if ( !Const.isEmpty( meta.getResultCodeFieldName() ) ) {
          newRow = RowDataUtil.addValueData( newRow, returnFieldsOffset, new Long( statusCode ) );
          returnFieldsOffset++;
        }
        if ( !Const.isEmpty( meta.getResponseTimeFieldName() ) ) {
          newRow = RowDataUtil.addValueData( newRow, returnFieldsOffset, new Long( responseTime ) );
        }

      } finally {
        if ( inputStreamReader != null ) {
          inputStreamReader.close();
        }
        // Release current connection to the connection pool once you are done
        method.releaseConnection();
        if ( data.realcloseIdleConnectionsTime > -1 ) {
          httpclient.getHttpConnectionManager().closeIdleConnections( data.realcloseIdleConnectionsTime );
        }
      }
      return newRow;
    } catch ( UnknownHostException uhe ) {
      throw new KettleException( BaseMessages.getString( PKG, "HTTP.Error.UnknownHostException", uhe.getMessage() ) );
    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString( PKG, "HTTP.Log.UnableGetResult", url ), e );
    }
  }

  private String determineUrl( RowMetaInterface outputRowMeta, Object[] row ) throws KettleValueException,
    KettleException {
    try {
      if ( meta.isUrlInField() ) {
        // get dynamic url
        data.realUrl = outputRowMeta.getString( row, data.indexOfUrlField );
      }
      StringBuffer url = new StringBuffer( data.realUrl ); // the base URL with variable substitution

      for ( int i = 0; i < data.argnrs.length; i++ ) {
        if ( i == 0 && url.indexOf( "?" ) < 0 ) {
          url.append( '?' );
        } else {
          url.append( '&' );
        }

        url.append( URIUtil.encodeWithinQuery( meta.getArgumentParameter()[i] ) );
        url.append( '=' );
        String s = outputRowMeta.getString( row, data.argnrs[i] );
        if ( s != null ) {
          s = URIUtil.encodeWithinQuery( s );
        }
        url.append( s );
      }

      return url.toString();
    } catch ( Exception e ) {
      throw new KettleException( BaseMessages.getString( PKG, "HTTP.Log.UnableCreateUrl" ), e );
    }
  }

  public boolean processRow( StepMetaInterface smi, StepDataInterface sdi ) throws KettleException {
    meta = (HTTPMeta) smi;
    data = (HTTPData) sdi;

    Object[] r = getRow(); // Get row from input rowset & set row busy!
    if ( r == null ) { // no more input to be expected...
      setOutputDone();
      return false;
    }

    if ( first ) {
      data.outputRowMeta = getInputRowMeta().clone();
      data.inputRowMeta = getInputRowMeta();
      meta.getFields( data.outputRowMeta, getStepname(), null, null, this, repository, metaStore );

      if ( meta.isUrlInField() ) {
        if ( Const.isEmpty( meta.getUrlField() ) ) {
          logError( BaseMessages.getString( PKG, "HTTP.Log.NoField" ) );
          throw new KettleException( BaseMessages.getString( PKG, "HTTP.Log.NoField" ) );
        }

        // cache the position of the field
        if ( data.indexOfUrlField < 0 ) {
          String realUrlfieldName = environmentSubstitute( meta.getUrlField() );
          data.indexOfUrlField = getInputRowMeta().indexOfValue( realUrlfieldName );
          if ( data.indexOfUrlField < 0 ) {
            // The field is unreachable !
            logError( BaseMessages.getString( PKG, "HTTP.Log.ErrorFindingField", realUrlfieldName ) );
            throw new KettleException( BaseMessages.getString( PKG, "HTTP.Exception.ErrorFindingField",
                realUrlfieldName ) );
          }
        }
      } else {
        data.realUrl = environmentSubstitute( meta.getUrl() );
      }

      // check for headers
      int nrHeaders = meta.getHeaderField().length;
      if ( nrHeaders > 0 ) {
        data.useHeaderParameters = true;
      }

      data.header_parameters_nrs = new int[nrHeaders];
      data.headerParameters = new NameValuePair[nrHeaders];

      // get the headers
      for ( int i = 0; i < nrHeaders; i++ ) {
        int fieldIndex = data.inputRowMeta.indexOfValue( meta.getHeaderField()[i] );
        if ( fieldIndex < 0 ) {
          logError( BaseMessages.getString( PKG,
                  "HTTP.Exception.ErrorFindingField" ) + meta.getHeaderField()[i] + "]" );
          throw new KettleStepException( BaseMessages.getString( PKG, "HTTP.Exception.ErrorFindingField", meta
              .getHeaderField()[i] ) );
        }

        data.header_parameters_nrs[i] = fieldIndex;
        data.headerParameters[i] =
            new NameValuePair( environmentSubstitute( meta.getHeaderParameter()[i] ), data.outputRowMeta.getString( r,
                data.header_parameters_nrs[i] ) );
      }

    } // end if first

    try {
      Object[] outputRowData = execHttp( getInputRowMeta(), r ); // add new values to the row
      putRow( data.outputRowMeta, outputRowData ); // copy row to output rowset(s);

      if ( checkFeedback( getLinesRead() ) ) {
        if ( isDetailed() ) {
          logDetailed( BaseMessages.getString( PKG, "HTTP.LineNumber" ) + getLinesRead() );
        }
      }
    } catch ( KettleException e ) {
      boolean sendToErrorRow = false;
      String errorMessage = null;

      if ( getStepMeta().isDoingErrorHandling() ) {
        sendToErrorRow = true;
        errorMessage = e.toString();
      } else {
        logError( BaseMessages.getString( PKG, "HTTP.ErrorInStepRunning" ) + e.getMessage() );
        setErrors( 1 );
        stopAll();
        setOutputDone(); // signal end to receiver(s)
        return false;
      }
      if ( sendToErrorRow ) {
        // Simply add this row to the error row
        putError( getInputRowMeta(), r, 1, errorMessage, null, "HTTP001" );
      }
    }

    return true;
  }

  public boolean init( StepMetaInterface smi, StepDataInterface sdi ) {
    meta = (HTTPMeta) smi;
    data = (HTTPData) sdi;

    if ( super.init( smi, sdi ) ) {
      // get authentication settings once
      data.realProxyHost = environmentSubstitute( meta.getProxyHost() );
      data.realProxyPort = Const.toInt( environmentSubstitute( meta.getProxyPort() ), 8080 );
      data.realHttpLogin = environmentSubstitute( meta.getHttpLogin() );
      data.realHttpPassword = environmentSubstitute( meta.getHttpPassword() );

      data.realSocketTimeout = Const.toInt( environmentSubstitute( meta.getSocketTimeout() ), -1 );
      data.realConnectionTimeout = Const.toInt( environmentSubstitute( meta.getSocketTimeout() ), -1 );

      return true;
    }
    return false;
  }

  public void dispose( StepMetaInterface smi, StepDataInterface sdi ) {
    meta = (HTTPMeta) smi;
    data = (HTTPData) sdi;

    super.dispose( smi, sdi );
  }

}
