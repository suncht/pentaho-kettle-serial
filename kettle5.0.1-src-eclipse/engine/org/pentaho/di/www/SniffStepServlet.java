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
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Encoder;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.step.RowListener;
import org.pentaho.di.trans.step.StepInterface;

public class SniffStepServlet extends BaseHttpServlet implements CartePluginInterface {
  private static Class<?> PKG = GetTransStatusServlet.class; // for i18n purposes, needed by Translator2!! $NON-NLS-1$

  private static final long serialVersionUID = 3634806745372015720L;
  public static final String CONTEXT_PATH = "/kettle/sniffStep";

  public static final String	TYPE_INPUT  = "input";
  public static final String	TYPE_OUTPUT	= "output";

  public static final String	XML_TAG			= "step-sniff";

  final class MetaAndData {
	  public RowMetaInterface bufferRowMeta;
	  public List<Object[]> bufferRowData;
  }
  
  public SniffStepServlet() {
  }
  
  public SniffStepServlet(TransformationMap transformationMap) {
    super(transformationMap);
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    if (isJettyMode() && !request.getContextPath().startsWith(CONTEXT_PATH)) {
      return;
    }

    if (log.isDebug()) {
    	logDebug(BaseMessages.getString(PKG, "TransStatusServlet.Log.SniffStepRequested"));
    }

    String transName = request.getParameter("trans");
    String id = request.getParameter("id");
    String stepName = request.getParameter("step");
    int copyNr = Const.toInt(request.getParameter("copynr"), 0);
    final int nrLines = Const.toInt(request.getParameter("lines"), 0);
    String type = Const.NVL(request.getParameter("type"), TYPE_OUTPUT);
    boolean useXML = "Y".equalsIgnoreCase(request.getParameter("xml"));

    response.setStatus(HttpServletResponse.SC_OK);

    if (useXML) {
      response.setContentType("text/xml");
      response.setCharacterEncoding(Const.XML_ENCODING);
    } else {
      response.setContentType("text/html;charset=UTF-8");
    }

    PrintWriter out = response.getWriter();

    // ID is optional...
    //
    Trans trans;
    CarteObjectEntry entry;
    if (Const.isEmpty(id)) {
    	// get the first transformation that matches...
    	//
    	entry = getTransformationMap().getFirstCarteObjectEntry(transName);
    	if (entry==null) {
    		trans = null;
    	} else {
    		id = entry.getId();
    		trans = getTransformationMap().getTransformation(entry);
    	}
    } else {
    	// Take the ID into account!
    	//
    	entry = new CarteObjectEntry(transName, id);
    	trans = getTransformationMap().getTransformation(entry);
    }
    
    Encoder encoder = ESAPI.encoder();

    if (trans != null) {

      // Find the step to look at...
      //
      StepInterface step=null;
      List<StepInterface> stepInterfaces = trans.findBaseSteps(stepName);
      for (int i=0;i<stepInterfaces.size();i++) {
    	  StepInterface look = stepInterfaces.get(i);
    	  if (look.getCopy()==copyNr) {
    		  step=look;
    	  }
      }
      if (step!=null) {
	    
          // Add a listener to the transformation step...
          //
    	  final boolean read=type.equalsIgnoreCase(TYPE_INPUT);
    	  final boolean written=type.equalsIgnoreCase(TYPE_OUTPUT) || !read;
    	  final MetaAndData metaData = new MetaAndData();
    	  
    	  metaData.bufferRowMeta = null;
    	  metaData.bufferRowData = new ArrayList<Object[]>();

		  RowListener rowListener = new RowListener() {
				public void rowReadEvent(RowMetaInterface rowMeta, Object[] row) throws KettleStepException {
					if (read && metaData.bufferRowData.size()<nrLines) {
						metaData.bufferRowMeta = rowMeta;
						metaData.bufferRowData.add(row);
					}
				}
			
				public void rowWrittenEvent(RowMetaInterface rowMeta, Object[] row) throws KettleStepException {
					if (written && metaData.bufferRowData.size()<nrLines) {
						metaData.bufferRowMeta = rowMeta;
						metaData.bufferRowData.add(row);
					}
				}
			
				public void errorRowWrittenEvent(RowMetaInterface rowMeta, Object[] row) throws KettleStepException {
				}
		    };

    	  step.addRowListener(rowListener);
    	  
    	  // Wait until we have enough rows...
    	  //
    	  while (metaData.bufferRowData.size()<nrLines && step.isRunning() && !trans.isFinished() && !trans.isStopped()) {
    		  
    		  try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// Ignore
				//
				break;
			}
    	  }
    	  
    	  // Remove the row listener
    	  //
    	  step.removeRowListener(rowListener);
    	  
    	  // Pass along the rows of data...
    	  //
	      if (useXML) {

	    	// Send the result back as XML
	        //
	        response.setContentType("text/xml");
	        response.setCharacterEncoding(Const.XML_ENCODING);
	        out.print(XMLHandler.getXMLHeader(Const.XML_ENCODING));

	        out.println(XMLHandler.openTag(XML_TAG));
	    	
	        if (metaData.bufferRowMeta!=null) {
	        	
		        // Row Meta data
		        //
		        out.println(metaData.bufferRowMeta.getMetaXML());
		        
		        // Nr of lines
		        //
		    	out.println(XMLHandler.addTagValue("nr_rows", metaData.bufferRowData.size()));
		    	
		    	// Rows of data
		    	//
		    	for (int i=0;i<metaData.bufferRowData.size();i++) {
		    		Object[] rowData = metaData.bufferRowData.get(i);
		    		out.println(metaData.bufferRowMeta.getDataXML(rowData));
		    	}
	        }
	    	
	        out.println(XMLHandler.closeTag(XML_TAG));

	      } else {
	        response.setContentType("text/html;charset=UTF-8");
	
	        out.println("<HTML>");
	        out.println("<HEAD>");
	        out.println("<TITLE>" + BaseMessages.getString(PKG, "SniffStepServlet.SniffResults") + "</TITLE>");
	        out.println("<META http-equiv=\"Refresh\" content=\"10;url=" + convertContextPath(CONTEXT_PATH) + "?name=" + URLEncoder.encode(transName, "UTF-8") + "&id="+URLEncoder.encode(id, "UTF-8")+"\">");
	        out.println("<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
	        out.println("</HEAD>");
	        out.println("<BODY>");
	        out.println("<H1>" + encoder.encodeForHTML(BaseMessages.getString(PKG, "SniffStepServlet.SniffResultsForStep", stepName)) + "</H1>");
	
	        try {
	          out.println("<table border=\"1\">");
	          
		      if (metaData.bufferRowMeta!=null) {
		          // Print a header row containing all the field names...
		          //
		          out.print("<tr><th>#</th>");
		          for (ValueMetaInterface valueMeta : metaData.bufferRowMeta.getValueMetaList()) {
		        	  out.print("<th>" + valueMeta.getName()+"</th>" );
		          }
		          out.println("</tr>");
	          
		          // Now output the data rows...
		          //
		          for (int r=0;r<metaData.bufferRowData.size();r++) {
			    		Object[] rowData = metaData.bufferRowData.get(r);
				          out.print("<tr>");
				          out.println("<td>"+(r+1)+"</td>");
				          for (int v=0;v<metaData.bufferRowMeta.size();v++) {
				        	  ValueMetaInterface valueMeta = metaData.bufferRowMeta.getValueMeta(v);
				        	  Object valueData = rowData[v];
					          out.println("<td>"+valueMeta.getString(valueData)+"</td>");
				          }
				          out.println("</tr>");
		          }
		      }
	          
	          out.println("</table>");
	
	          out.println("<p>");
	
	        } catch (Exception ex) {
	          out.println("<p>");
	          out.println("<pre>");
	          out.println(encoder.encodeForHTML(Const.getStackTracker(ex)));
	          out.println("</pre>");
	        }
	
	        out.println("<p>");
	        out.println("</BODY>");
	        out.println("</HTML>");
	      }
      } else {
          if (useXML) {
              out.println(new WebResult(WebResult.STRING_ERROR, BaseMessages.getString(PKG, "SniffStepServlet.Log.CoundNotFindSpecStep", stepName)).getXML());
            } else {
              out.println("<H1>" + encoder.encodeForHTML(BaseMessages.getString(PKG, "SniffStepServlet.Log.CoundNotFindSpecStep", stepName)) + "</H1>");
              out.println("<a href=\"" + convertContextPath(GetStatusServlet.CONTEXT_PATH) + "\">" + BaseMessages.getString(PKG, "TransStatusServlet.BackToStatusPage") + "</a><p>");
            }
      }
    } else {
      if (useXML) {
        out.println(new WebResult(WebResult.STRING_ERROR, BaseMessages.getString(PKG, "SniffStepServlet.Log.CoundNotFindSpecTrans", transName)).getXML());
      } else {
        out.println("<H1>" + encoder.encodeForHTML(BaseMessages.getString(PKG, "SniffStepServlet.Log.CoundNotFindTrans", transName)) + "</H1>");
        out.println("<a href=\"" + convertContextPath(GetStatusServlet.CONTEXT_PATH) + "\">" + BaseMessages.getString(PKG, "TransStatusServlet.BackToStatusPage") + "</a><p>");
      }
    }
  }

  public String toString() {
    return "Trans Status Handler";
  }

  public String getService() {
    return CONTEXT_PATH + " (" + toString() + ")";
  }
  
  public String getContextPath() {
    return CONTEXT_PATH;
  }

}
