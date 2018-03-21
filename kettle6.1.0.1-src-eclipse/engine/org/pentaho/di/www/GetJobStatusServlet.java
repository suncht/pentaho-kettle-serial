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

package org.pentaho.di.www;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;
import org.pentaho.di.cluster.HttpUtil;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.gui.Point;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.job.Job;

public class GetJobStatusServlet extends BaseHttpServlet implements CartePluginInterface {
  private static Class<?> PKG = GetJobStatusServlet.class; // for i18n purposes, needed by Translator2!!

  private static final long serialVersionUID = 3634806745372015720L;
  public static final String CONTEXT_PATH = "/kettle/jobStatus";

  public GetJobStatusServlet() {
  }

  public GetJobStatusServlet( JobMap jobMap ) {
    super( jobMap );
  }

  /**
<div id="mindtouch">
    <h1>/kettle/jobStatus</h1>
    <a name="GET"></a>
    <h2>GET</h2>
    <p>Retrieves status of the specified job.
  Status is returned as HTML or XML output depending on the input parameters.
  Status contains information about last execution of the job.</p>
    
    <p><b>Example Request:</b><br />
    <pre function="syntax.xml">
    GET /kettle/jobStatus/?name=dummy_job&xml=Y
    </pre>
    
    </p>
    <h3>Parameters</h3>
    <table class="pentaho-table">
    <tbody>
    <tr>
      <th>name</th>
      <th>description</th>
      <th>type</th>
    </tr>
    <tr>
    <td>name</td>
    <td>Name of the job to be used for status generation.</td>
    <td>query</td>
    </tr>
    <tr>
    <td>xml</td>
    <td>Boolean flag which defines output format <code>Y</code> forces XML output to be generated. 
  HTML is returned otherwise.</td>
    <td>boolean, optional</td>
    </tr>
    <tr>
    <td>id</td>
    <td>Carte id of the job to be used for status generation.</td>
    <td>query, optional</td>
    </tr>
    <tr>
    <td>from</td>
    <td>Start line number of the execution log to be included into response.</td>
    <td>integer, optional</td>
    </tr>
    </tbody>
    </table>
  
  <h3>Response Body</h3>

  <table class="pentaho-table">
    <tbody>
      <tr>
        <td align="right">element:</td>
        <td>(custom)</td>
      </tr>
      <tr>
        <td align="right">media types:</td>
        <td>text/xml, text/html</td>
      </tr>
    </tbody>
  </table>
    <p>Response XML or HTML response containing details about the job specified.
  If an error occurs during method invocation <code>result</code> field of the response 
  will contain <code>ERROR</code> status.</p>
    
    <p><b>Example Response:</b></p>
    <pre function="syntax.xml">
    <?xml version="1.0" encoding="UTF-8"?>
    <jobstatus>
    <jobname>dummy_job</jobname>
    <id>a4d54106-25db-41c5-b9f8-73afd42766a6</id>
    <status_desc>Finished</status_desc>
    <error_desc/>
    <logging_string>&#x3c;&#x21;&#x5b;CDATA&#x5b;H4sIAAAAAAAAADMyMDTRNzTUNzRXMDC3MjS2MjJQ0FVIKc3NrYzPyk8CsoNLEotKFPLTFEDc1IrU5NKSzPw8Xi4j4nRm5qUrpOaVFFUqRLuE&#x2b;vpGxhKj0y0zL7M4IzUFYieybgWNotTi0pwS2&#x2b;iSotLUWE1iTPNCdrhCGtRsXi4AOMIbLPwAAAA&#x3d;&#x5d;&#x5d;&#x3e;</logging_string>
    <first_log_line_nr>0</first_log_line_nr>
    <last_log_line_nr>20</last_log_line_nr>
    <result>
      <lines_input>0</lines_input>
      <lines_output>0</lines_output>
      <lines_read>0</lines_read>
      <lines_written>0</lines_written>
      <lines_updated>0</lines_updated>
      <lines_rejected>0</lines_rejected>
      <lines_deleted>0</lines_deleted>
      <nr_errors>0</nr_errors>
      <nr_files_retrieved>0</nr_files_retrieved>
      <entry_nr>0</entry_nr>
      <result>Y</result>
      <exit_status>0</exit_status>
      <is_stopped>N</is_stopped>
      <log_channel_id/>
      <log_text>null</log_text>
      <result-file></result-file>
      <result-rows></result-rows>
    </result>
  </jobstatus>
    </pre>
    
    <h3>Status Codes</h3>
    <table class="pentaho-table">
  <tbody>
    <tr>
      <th>code</th>
      <th>description</th>
    </tr>
    <tr>
      <td>200</td>
      <td>Request was processed.</td>
    </tr>
    <tr>
      <td>500</td>
      <td>Internal server error occurs during request processing.</td>
    </tr>
  </tbody>
</table>
</div>
  */
  public void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
    IOException {
    if ( isJettyMode() && !request.getContextPath().startsWith( CONTEXT_PATH ) ) {
      return;
    }

    if ( log.isDebug() ) {
      logDebug( BaseMessages.getString( PKG, "GetJobStatusServlet.Log.JobStatusRequested" ) );
    }

    String jobName = request.getParameter( "name" );
    String id = request.getParameter( "id" );
    boolean useXML = "Y".equalsIgnoreCase( request.getParameter( "xml" ) );
    int startLineNr = Const.toInt( request.getParameter( "from" ), 0 );

    response.setStatus( HttpServletResponse.SC_OK );

    if ( useXML ) {
      response.setContentType( "text/xml" );
      response.setCharacterEncoding( Const.XML_ENCODING );
    } else {
      response.setContentType( "text/html;charset=UTF-8" );
    }

    PrintWriter out = response.getWriter();

    // ID is optional...
    //
    Job job;
    CarteObjectEntry entry;
    if ( Const.isEmpty( id ) ) {
      // get the first job that matches...
      //
      entry = getJobMap().getFirstCarteObjectEntry( jobName );
      if ( entry == null ) {
        job = null;
      } else {
        id = entry.getId();
        job = getJobMap().getJob( entry );
      }
    } else {
      // Actually, just providing the ID should be enough to identify the job
      //
      if ( Const.isEmpty( jobName ) ) {
        // Take the ID into account!
        //
        job = getJobMap().findJob( id );
      } else {
        entry = new CarteObjectEntry( jobName, id );
        job = getJobMap().getJob( entry );
        if ( job != null ) {
          jobName = job.getJobname();
        }
      }
    }

    Encoder encoder = ESAPI.encoder();

    if ( job != null ) {
      String status = job.getStatus();
      int lastLineNr = KettleLogStore.getLastBufferLineNr();
      String logText =
        KettleLogStore.getAppender().getBuffer(
          job.getLogChannel().getLogChannelId(), false, startLineNr, lastLineNr ).toString();

      if ( useXML ) {
        response.setContentType( "text/xml" );
        response.setCharacterEncoding( Const.XML_ENCODING );
        out.print( XMLHandler.getXMLHeader( Const.XML_ENCODING ) );

        SlaveServerJobStatus jobStatus = new SlaveServerJobStatus( jobName, id, status );
        jobStatus.setFirstLoggingLineNr( startLineNr );
        jobStatus.setLastLoggingLineNr( lastLineNr );

        // The log can be quite large at times, we are going to put a base64 encoding around a compressed stream
        // of bytes to handle this one.
        String loggingString = HttpUtil.encodeBase64ZippedString( logText );
        jobStatus.setLoggingString( loggingString );

        // Also set the result object...
        //
        jobStatus.setResult( job.getResult() ); // might be null

        try {
          out.println( jobStatus.getXML() );
        } catch ( KettleException e ) {
          throw new ServletException( "Unable to get the job status in XML format", e );
        }
      } else {
        response.setContentType( "text/html" );

        out.println( "<HTML>" );
        out.println( "<HEAD>" );
        out
          .println( "<TITLE>"
            + BaseMessages.getString( PKG, "GetJobStatusServlet.KettleJobStatus" ) + "</TITLE>" );
        out.println( "<META http-equiv=\"Refresh\" content=\"10;url="
          + convertContextPath( GetJobStatusServlet.CONTEXT_PATH ) + "?name="
          + URLEncoder.encode( Const.NVL( jobName, "" ), "UTF-8" ) + "&id=" + URLEncoder.encode( id, "UTF-8" )
          + "\">" );
        out.println( "<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" );
        out.println( "</HEAD>" );
        out.println( "<BODY>" );
        out.println( "<H1>" + BaseMessages.getString( PKG, "GetJobStatusServlet.JobStatus" ) + "</H1>" );

        try {
          out.println( "<table border=\"1\">" );
          out.print( "<tr> <th>"
            + BaseMessages.getString( PKG, "GetJobStatusServlet.Jobname" ) + "</th> <th>"
            + BaseMessages.getString( PKG, "TransStatusServlet.TransStatus" ) + "</th> </tr>" );

          out.print( "<tr>" );
          out.print( "<td>" + Const.NVL( encoder.encodeForHTML( jobName ), "" ) + "</td>" );
          out.print( "<td>" + status + "</td>" );
          out.print( "</tr>" );
          out.print( "</table>" );

          out.print( "<p>" );

          // Show job image?
          //
          Point max = job.getJobMeta().getMaximum();
          max.x += 20;
          max.y += 20;
          out
            .print( "<iframe height=\""
              + max.y + "\" width=\"" + max.x + "\" seamless src=\""
              + convertContextPath( GetJobImageServlet.CONTEXT_PATH ) + "?name="
              + URLEncoder.encode( jobName, "UTF-8" ) + "&id=" + URLEncoder.encode( id, "UTF-8" )
              + "\"></iframe>" );
          out.print( "<p>" );

          // out.print("<a href=\"" + convertContextPath(GetJobImageServlet.CONTEXT_PATH) + "?name=" +
          // URLEncoder.encode(Const.NVL(jobName, ""), "UTF-8") + "&id="+id+"\">"
          // + BaseMessages.getString(PKG, "GetJobImageServlet.GetJobImage") + "</a>");
          // out.print("<p>");

          if ( job.isFinished() ) {
            out.print( "<a href=\""
              + convertContextPath( StartJobServlet.CONTEXT_PATH ) + "?name="
              + URLEncoder.encode( Const.NVL( jobName, "" ), "UTF-8" ) + "&id="
              + URLEncoder.encode( id, "UTF-8" ) + "\">"
              + BaseMessages.getString( PKG, "GetJobStatusServlet.StartJob" ) + "</a>" );
            out.print( "<p>" );
          } else {
            out.print( "<a href=\""
              + convertContextPath( StopJobServlet.CONTEXT_PATH ) + "?name="
              + URLEncoder.encode( Const.NVL( jobName, "" ), "UTF-8" ) + "&id="
              + URLEncoder.encode( id, "UTF-8" ) + "\">"
              + BaseMessages.getString( PKG, "GetJobStatusServlet.StopJob" ) + "</a>" );
            out.print( "<p>" );
          }

          out.println( "<p>" );

          out.print( "<a href=\""
            + convertContextPath( GetJobStatusServlet.CONTEXT_PATH ) + "?name="
            + URLEncoder.encode( Const.NVL( jobName, "" ), "UTF-8" ) + "&xml=y&id="
            + URLEncoder.encode( id, "UTF-8" ) + "\">"
            + BaseMessages.getString( PKG, "TransStatusServlet.ShowAsXml" ) + "</a><br>" );
          out.print( "<a href=\""
            + convertContextPath( GetStatusServlet.CONTEXT_PATH ) + "\">"
            + BaseMessages.getString( PKG, "TransStatusServlet.BackToStatusPage" ) + "</a><br>" );
          out.print( "<p><a href=\""
            + convertContextPath( GetJobStatusServlet.CONTEXT_PATH ) + "?name="
            + URLEncoder.encode( Const.NVL( jobName, "" ), "UTF-8" ) + "&id=" + URLEncoder.encode( id, "UTF-8" )
            + "\">" + BaseMessages.getString( PKG, "TransStatusServlet.Refresh" ) + "</a>" );

          // Put the logging below that.

          out.println( "<p>" );
          out.println( "<textarea id=\"joblog\" cols=\"120\" rows=\"20\" wrap=\"off\" "
            + "name=\"Job log\" readonly=\"readonly\">"
            + encoder.encodeForHTML( logText ) + "</textarea>" );

          out.println( "<script type=\"text/javascript\"> " );
          out.println( "  joblog.scrollTop=joblog.scrollHeight; " );
          out.println( "</script> " );
          out.println( "<p>" );
        } catch ( Exception ex ) {
          out.println( "<p>" );
          out.println( "<pre>" );
          out.println( encoder.encodeForHTML( Const.getStackTracker( ex ) ) );
          out.println( "</pre>" );
        }

        out.println( "<p>" );
        out.println( "</BODY>" );
        out.println( "</HTML>" );
      }
    } else {
      if ( useXML ) {
        out.println( new WebResult( WebResult.STRING_ERROR, BaseMessages.getString(
          PKG, "StartJobServlet.Log.SpecifiedJobNotFound", jobName, id ) ) );
      } else {
        out.println( "<H1>Job '" + encoder.encodeForHTML( jobName ) + "' could not be found.</H1>" );
        out.println( "<a href=\""
          + convertContextPath( GetStatusServlet.CONTEXT_PATH ) + "\">"
          + BaseMessages.getString( PKG, "TransStatusServlet.BackToStatusPage" ) + "</a><p>" );
      }
    }
  }

  public String toString() {
    return "Job Status Handler";
  }

  public String getService() {
    return CONTEXT_PATH + " (" + toString() + ")";
  }

  public String getContextPath() {
    return CONTEXT_PATH;
  }

}
