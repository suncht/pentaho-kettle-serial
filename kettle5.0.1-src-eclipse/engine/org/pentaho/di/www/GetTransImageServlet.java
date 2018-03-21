package org.pentaho.di.www;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.gui.AreaOwner;
import org.pentaho.di.core.gui.Point;
import org.pentaho.di.core.gui.SwingGC;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.TransPainter;
import org.pentaho.di.trans.step.StepMeta;

public class GetTransImageServlet extends BaseHttpServlet implements CartePluginInterface {

  private static final long serialVersionUID = -4365372274638005929L;

  private static Class<?> PKG = GetTransImageServlet.class; // for i18n purposes, needed by Translator2!! $NON-NLS-1$

  public static final String CONTEXT_PATH = "/kettle/transImage";
  
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    if (isJettyMode() && !request.getContextPath().startsWith(CONTEXT_PATH)) {
      return;
    }

    if (log.isDebug())
      logDebug(BaseMessages.getString(PKG, "GetTransImageServlet.Log.TransImageRequested"));

    String transName = request.getParameter("name");
    String id = request.getParameter("id");

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
    
    try {
      if (trans != null) {
  
        response.setStatus(HttpServletResponse.SC_OK);
  
        response.setCharacterEncoding("UTF-8");
        response.setContentType("image/png");
        
  
        // Generate xform image
        //
        BufferedImage image = generateTransformationImage(trans.getTransMeta());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
          ImageIO.write(image, "png", os);
        } finally {
          os.flush();
        }
        response.setContentLength(os.size());
        
        OutputStream out = response.getOutputStream();
        out.write(os.toByteArray());
        
      }
    } catch( Exception e) {
      e.printStackTrace();
    }
  }

  private BufferedImage generateTransformationImage(TransMeta transMeta) throws Exception {
    float magnification = 1.0f;
    Point maximum = transMeta.getMaximum();
    maximum.multiply(magnification);
    
    SwingGC gc = new SwingGC(null, maximum, 32, 0, 0);
    TransPainter transPainter = new TransPainter(gc, transMeta, maximum, null, null, 
        null, null, null, new ArrayList<AreaOwner>(), new ArrayList<StepMeta>(), 32, 1, 0, 0, true, "Arial", 10);
    transPainter.setMagnification(magnification);
    transPainter.buildTransformationImage();
    
    BufferedImage image = (BufferedImage) gc.getImage();

    return image;
  }

  public String toString() {
    return "Trans Image Handler";
  }

  public String getService() {
    return CONTEXT_PATH + " (" + toString() + ")";
  }
  
  public String getContextPath() {
    return CONTEXT_PATH;
  }

}
