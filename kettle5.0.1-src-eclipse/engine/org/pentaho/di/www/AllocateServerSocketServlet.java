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

package org.pentaho.di.www;

import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.xml.XMLHandler;

/**
 * This servlet allows a client (TransSplitter in our case) to ask for a port number.<br>
 * This port number will be allocated in such a way that the port number is unique for a given hostname.<br>
 * This in turn will ensure that all the slaves will use valid port numbers, even if multiple slaves run on the same host.
 * 
 * @author matt
 * 
 */
public class AllocateServerSocketServlet extends BaseHttpServlet implements CartePluginInterface {
  private static final long serialVersionUID = 3634806745372015720L;

  public static final String CONTEXT_PATH = "/kettle/allocateSocket";

  public static final String PARAM_RANGE_START = "rangeStart";
  public static final String PARAM_HOSTNAME = "host";
  public static final String PARAM_ID = "id";
  public static final String PARAM_TRANSFORMATION_NAME = "trans";
  public static final String PARAM_SOURCE_SLAVE = "sourceSlave";
  public static final String PARAM_SOURCE_STEPNAME = "sourceStep";
  public static final String PARAM_SOURCE_STEPCOPY = "sourceCopy";
  public static final String PARAM_TARGET_SLAVE = "targetSlave";
  public static final String PARAM_TARGET_STEPNAME = "targetStep";
  public static final String PARAM_TARGET_STEPCOPY = "targetCopy";

  public static final String XML_TAG_PORT = "port";

  public AllocateServerSocketServlet() {
  }

  public AllocateServerSocketServlet(TransformationMap transformationMap) {
    super(transformationMap);
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    if (isJettyMode() && !request.getContextPath().startsWith(CONTEXT_PATH))
      return;

    if (log.isDebug())
      logDebug("Reservation of port number of step requested");
    response.setStatus(HttpServletResponse.SC_OK);

    boolean useXML = "Y".equalsIgnoreCase(request.getParameter("xml"));

    String rangeStart = request.getParameter(PARAM_RANGE_START);
    String hostname = request.getParameter(PARAM_HOSTNAME);
    String clusteredRunId = request.getParameter(PARAM_ID);
    String transName = request.getParameter(PARAM_TRANSFORMATION_NAME);
    String sourceSlaveName = request.getParameter(PARAM_SOURCE_SLAVE);
    String sourceStepName = request.getParameter(PARAM_SOURCE_STEPNAME);
    String sourceStepCopy = request.getParameter(PARAM_SOURCE_STEPCOPY);
    String targetSlaveName = request.getParameter(PARAM_TARGET_SLAVE);
    String targetStepName = request.getParameter(PARAM_TARGET_STEPNAME);
    String targetStepCopy = request.getParameter(PARAM_TARGET_STEPCOPY);

    if (useXML) {
      response.setContentType("text/xml");
      response.setCharacterEncoding(Const.XML_ENCODING);
    } else {
      response.setContentType("text/html");
    }

    SocketPortAllocation port = getTransformationMap().allocateServerSocketPort(Const.toInt(rangeStart, 40000), hostname, clusteredRunId, transName, sourceSlaveName,
        sourceStepName, sourceStepCopy, targetSlaveName, targetStepName, targetStepCopy);

    PrintStream out = new PrintStream(response.getOutputStream());
    if (useXML) {
      out.print(XMLHandler.getXMLHeader(Const.XML_ENCODING));
      out.print(XMLHandler.addTagValue(XML_TAG_PORT, port.getPort()));
    } else {
      Encoder encoder = ESAPI.encoder();
      out.println("<HTML>");
      out.println("<HEAD><TITLE>Allocation of a server socket port number</TITLE></HEAD>");
      out.println("<BODY>");
      out.println("<H1>Status</H1>");

      out.println("<p>");
      out.println("Run ID : " + encoder.encodeForHTML(clusteredRunId) + "<br>");
      out.println("Host name : " + encoder.encodeForHTML(hostname) + "<br>");
      out.println("Transformation name : " + encoder.encodeForHTML(transName) + "<br>");
      out.println("Source step : " + encoder.encodeForHTML(sourceStepName) + "." + encoder.encodeForHTML(sourceStepCopy) + "<br>");
      out.println("Target step : " + encoder.encodeForHTML(targetStepName) + "." + encoder.encodeForHTML(targetStepCopy) + "<br>");
      out.println("Step copy: " + encoder.encodeForHTML(sourceStepCopy) + "<br>");
      out.println("<p>");
      out.println("--> port : " + encoder.encodeForHTML(port.toString()) + "<br>");

      out.println("<p>");
      out.println("</BODY>");
      out.println("</HTML>");
    }
  }

  public String toString() {
    return "Servet socket port number reservation request";
  }

  public String getService() {
    return CONTEXT_PATH + " (" + toString() + ")";
  }
  
  public String getContextPath() {
    return CONTEXT_PATH;
  }

}
