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

package org.pentaho.di.core.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;

import org.jfree.text.TextUtilities;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.di.laf.BasePropertyHandler;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.reporting.libraries.base.util.WaitingImageObserver;

public class SwingGC implements GCInterface {

  private static BufferedImage imageLocked;

  private static BufferedImage imageStepError;

  private static BufferedImage imageEdit;

  private static BufferedImage imageContextMenu;

  private static BufferedImage imageTrue;

  private static BufferedImage imageFalse;

  private static BufferedImage imageErrorHop;

  private static BufferedImage imageInfoHop;

  private static BufferedImage imageHopTarget;

  private static BufferedImage imageHopInput;

  private static BufferedImage imageHopOutput;

  private static BufferedImage imageArrow;

  private static BufferedImage imageCopyHop;

  private static BufferedImage imageLoadBalance;

  private static BufferedImage imageCheckpoint;

  private static BufferedImage imageDatabase;

  private static BufferedImage imageParallelHop;

  private static BufferedImage imageUnconditionalHop;

  private static BufferedImage imageStart;

  private static BufferedImage imageDummy;

  private static BufferedImage imageBusy;

  private static BufferedImage imageInject;

  protected Color background;

  protected Color black;
  protected Color red;
  protected Color yellow;
  protected Color orange;
  protected Color green;
  protected Color blue;
  protected Color magenta;
  protected Color gray;
  protected Color lightGray;
  protected Color darkGray;
  protected Color lightBlue;

  private Graphics2D gc;

  private int iconsize;

  private Map<String, BufferedImage> stepImages;
  private Map<String, BufferedImage> entryImages;

  private BufferedImage image;
  private ImageObserver observer;

  private Point area;

  private int alpha;

  private Font fontGraph;

  private Font fontNote;

  private Font fontSmall;

  private int lineWidth;
  private ELineStyle lineStyle;

  private int yOffset;

  private int xOffset;

  private boolean drawingPixelatedImages;

  public SwingGC(ImageObserver observer, Point area, int iconsize, int xOffset, int yOffset) throws KettleException {
    this.image = new BufferedImage(area.x, area.y, BufferedImage.TYPE_INT_RGB);
    this.gc = image.createGraphics();
    this.observer = observer;
    this.stepImages = SwingGUIResource.getInstance().getStepImages();
    this.entryImages = SwingGUIResource.getInstance().getEntryImages();
    this.iconsize = iconsize;
    this.area = area;
    this.xOffset = xOffset;
    this.yOffset = yOffset;

    init();
  }

  public SwingGC(Graphics2D gc, Rectangle2D rect, int iconsize, int xOffset, int yOffset) throws KettleException {
    this.image = null;
    this.gc = gc;
    this.observer = null;
    this.stepImages = SwingGUIResource.getInstance().getStepImages();
    this.entryImages = SwingGUIResource.getInstance().getEntryImages();
    this.iconsize = iconsize;
    this.area = new Point((int) rect.getWidth(), (int) rect.getHeight());
    this.xOffset = xOffset;
    this.yOffset = yOffset;

    init();
  }

  private void init() throws KettleException {
    this.lineStyle = ELineStyle.SOLID;
    this.lineWidth = 1;
    this.alpha = 255;

    this.background = new Color(255, 255, 255);
    this.black = new Color(0, 0, 0);
    this.red = new Color(255, 0, 0);
    this.yellow = new Color(255, 255, 0);
    this.orange = new Color(255, 165, 0);
    this.green = new Color(0, 255, 0);
    this.blue = new Color(0, 0, 255);
    this.magenta = new Color(255, 0, 255);
    this.gray = new Color(128, 128, 128);
    this.lightGray = new Color(200, 200, 200);
    this.darkGray = new Color(80, 80, 80);
    this.lightBlue = new Color(135, 206, 250); // light sky blue

    imageLocked = getImageIcon(BasePropertyHandler.getProperty("Locked_image"));
    imageStepError = getImageIcon(BasePropertyHandler.getProperty("StepErrorLines_image"));
    imageEdit = getImageIcon(BasePropertyHandler.getProperty("Edit_image"));
    imageContextMenu = getImageIcon(BasePropertyHandler.getProperty("ContextMenu_image"));
    imageTrue = getImageIcon(BasePropertyHandler.getProperty("True_image"));
    imageFalse = getImageIcon(BasePropertyHandler.getProperty("False_image"));
    imageErrorHop = getImageIcon(BasePropertyHandler.getProperty("ErrorHop_image"));
    imageInfoHop = getImageIcon(BasePropertyHandler.getProperty("InfoHop_image"));
    imageHopTarget = getImageIcon(BasePropertyHandler.getProperty("HopTarget_image"));
    imageHopInput = getImageIcon(BasePropertyHandler.getProperty("HopInput_image"));
    imageHopOutput = getImageIcon(BasePropertyHandler.getProperty("HopOutput_image"));
    imageArrow = getImageIcon(BasePropertyHandler.getProperty("ArrowIcon_image"));
    imageCopyHop = getImageIcon(BasePropertyHandler.getProperty("CopyHop_image"));
    imageLoadBalance = getImageIcon(BasePropertyHandler.getProperty("LoadBalance_image"));
    imageCheckpoint = getImageIcon(BasePropertyHandler.getProperty("CheckeredFlag_image"));
    imageDatabase = getImageIcon(BasePropertyHandler.getProperty("Database_image"));
    imageParallelHop = getImageIcon(BasePropertyHandler.getProperty("ParallelHop_image"));
    imageUnconditionalHop = getImageIcon(BasePropertyHandler.getProperty("UnconditionalHop_image"));
    imageStart = getImageIcon(BasePropertyHandler.getProperty("STR_image"));
    imageDummy = getImageIcon(BasePropertyHandler.getProperty("DUM_image"));
    imageBusy = getImageIcon(BasePropertyHandler.getProperty("Busy_image"));
    imageInject = getImageIcon(BasePropertyHandler.getProperty("Inject_image"));

    fontGraph = new Font("FreeSans", Font.PLAIN, 10);
    fontNote = new Font("FreeSans", Font.PLAIN, 10);
    fontSmall = new Font("FreeSans", Font.PLAIN, 8);

    gc.setFont(fontGraph);

    gc.setColor(background);
    gc.fillRect(0, 0, area.x, area.y);
  }

  private BufferedImage getImageIcon(String fileName) throws KettleException {
    InputStream inputStream = null;
    BufferedImage image = null;
    if (fileName==null) {
      throw new KettleException("Image icon file name can not be null");
    }
    try {

      image = ImageIO.read(new File(fileName));
      if (image == null) {
        image = ImageIO.read(new File("/" + fileName));
      }
    } // we fail silently as the images may be available in a jar
    catch (IIOException iioe) {
    } catch (IOException ioe) {
    }

    if (image == null) {
      try {
        inputStream = getClass().getResourceAsStream(fileName);
        if (inputStream == null) {
          inputStream = getClass().getResourceAsStream("/" + fileName);
        }
        if (inputStream == null) {
          throw new KettleException("Unable to load image from classpath : '" + fileName + "'");
        }
        image = ImageIO.read(inputStream);
      } catch (IOException ioe) {
        throw new KettleException("Unable to close image reading stream", ioe);
      } finally {
        if (inputStream != null) {
          try {
            inputStream.close();
          } catch (IOException e) {
            throw new KettleException("Unable to close image reading stream", e);
          }
        }
      }
    }

    WaitingImageObserver observer = new WaitingImageObserver(image);
    observer.waitImageLoaded();

    return image;
  }

  public void dispose() {
  }

  public void drawLine(int x, int y, int x2, int y2) {
    gc.drawLine(x + xOffset, y + yOffset, x2 + xOffset, y2 + yOffset);
  }

  public void drawImage(EImage image, int locationX, int locationY) {

    BufferedImage img = getNativeImage(image);

    drawPixelatedImage(img, locationX, locationY);

    // gc.drawImage(img, locationX+xOffset, locationY+yOffset, observer);

  }

  public void drawPixelatedImage(BufferedImage img, int locationX, int locationY) {

    if (isDrawingPixelatedImages()) {
      BufferedImage bi = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D g2 = (Graphics2D) bi.getGraphics();
      g2.setColor(Color.WHITE);
      g2.fillRect(0, 0, img.getWidth(), img.getHeight());
      g2.drawImage(img, 0, 0, observer);
      g2.dispose();

      for (int x = 0; x < bi.getWidth(observer); x++) {
        for (int y = 0; y < bi.getHeight(observer); y++) {
          int rgb = bi.getRGB(x, y);
          gc.setColor(new Color(rgb));
          gc.setStroke(new BasicStroke(1.0f));
          gc.drawLine(locationX + xOffset + x, locationY + yOffset + y, locationX + xOffset + x, locationY + yOffset
              + y);
          // gc.drawLine(locationX+xOffset+x, locationY+yOffset+y,
          // locationX+xOffset+x+1, locationY+yOffset+y);
          // gc.drawLine(locationX+xOffset+x, locationY+yOffset+y+1,
          // locationX+xOffset+x+1, locationY+yOffset+y+1);
          // gc.fillRect(locationX+xOffset+x, locationY+yOffset+y, 1, 1);
        }
      }
    } else {
      while (!gc.drawImage(img, locationX, locationY, observer)) {
        // Wait
      }
    }

  }

  public Point getImageBounds(EImage image) {
    BufferedImage img = getNativeImage(image);
    int width = img.getWidth(observer);
    int height = img.getHeight(observer);
    return new Point(width, height);
  }

  public static final BufferedImage getNativeImage(EImage image) {
    switch (image) {
    case LOCK:
      return imageLocked;
    case STEP_ERROR:
      return imageStepError;
    case EDIT:
      return imageEdit;
    case CONTEXT_MENU:
      return imageContextMenu;
    case TRUE:
      return imageTrue;
    case FALSE:
      return imageFalse;
    case ERROR:
      return imageErrorHop;
    case INFO:
      return imageInfoHop;
    case TARGET:
      return imageHopTarget;
    case INPUT:
      return imageHopInput;
    case OUTPUT:
      return imageHopOutput;
    case ARROW:
      return imageArrow;
    case COPY_ROWS:
      return imageCopyHop;
    case LOAD_BALANCE:
      return imageLoadBalance;
    case CHECKPOINT:
      return imageCheckpoint;
    case DB:
      return imageDatabase;
    case PARALLEL:
      return imageParallelHop;
    case UNCONDITIONAL:
      return imageUnconditionalHop;
    case BUSY:
      return imageBusy;
    case INJECT:
      return imageInject;
      default:
        break;
    }
    return null;
  }

  public void drawPoint(int x, int y) {
    gc.drawLine(x + xOffset, y + yOffset, x + xOffset, y + yOffset);
  }

  public void drawPolygon(int[] polygon) {

    gc.drawPolygon(getSwingPolygon(polygon));
  }

  private Polygon getSwingPolygon(int[] polygon) {
    int nPoints = polygon.length / 2;
    int[] xPoints = new int[polygon.length / 2];
    int[] yPoints = new int[polygon.length / 2];
    for (int i = 0; i < nPoints; i++) {
      xPoints[i] = polygon[2 * i + 0] + xOffset;
      yPoints[i] = polygon[2 * i + 1] + yOffset;
    }

    return new Polygon(xPoints, yPoints, nPoints);
  }

  public void drawPolyline(int[] polyline) {
    int nPoints = polyline.length / 2;
    int[] xPoints = new int[polyline.length / 2];
    int[] yPoints = new int[polyline.length / 2];
    for (int i = 0; i < nPoints; i++) {
      xPoints[i] = polyline[2 * i + 0] + xOffset;
      yPoints[i] = polyline[2 * i + 1] + yOffset;
    }
    gc.drawPolyline(xPoints, yPoints, nPoints);
  }

  public void drawRectangle(int x, int y, int width, int height) {
    gc.drawRect(x + xOffset, y + yOffset, width, height);
  }

  public void drawRoundRectangle(int x, int y, int width, int height, int circleWidth, int circleHeight) {
    gc.drawRoundRect(x + xOffset, y + yOffset, width, height, circleWidth, circleHeight);
  }

  public void drawText(String text, int x, int y) {

    int height = gc.getFontMetrics().getHeight();

    String[] lines = text.split("\n");
    for (String line : lines) {
      gc.drawString(line, x + xOffset, y + height + yOffset);
      y += height;
    }
  }

  public void drawText(String text, int x, int y, boolean transparent) {
    drawText(text, x, y);
  }

  public void fillPolygon(int[] polygon) {
    switchForegroundBackgroundColors();
    gc.fillPolygon(getSwingPolygon(polygon));
    switchForegroundBackgroundColors();
  }

  public void fillRectangle(int x, int y, int width, int height) {
    switchForegroundBackgroundColors();
    gc.fillRect(x + xOffset, y + yOffset, width, height);
    switchForegroundBackgroundColors();
  }
  
  // TODO: complete code 
  public void fillGradientRectangle(int x, int y, int width, int height, boolean vertical) {
    fillRectangle(x, y, width, height);
  }

  public void fillRoundRectangle(int x, int y, int width, int height, int circleWidth, int circleHeight) {
    switchForegroundBackgroundColors();
    gc.fillRoundRect(x + xOffset, y + yOffset, width, height, circleWidth, circleHeight);
    switchForegroundBackgroundColors();
  }

  public Point getDeviceBounds() {
    return area;
  }

  public void setAlpha(int alpha) {
    this.alpha = alpha;
    AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255);
    gc.setComposite(alphaComposite);
  }

  public int getAlpha() {
    return alpha;
  }

  public void setBackground(EColor color) {
    gc.setBackground(getColor(color));
  }

  private Color getColor(EColor color) {
    switch (color) {
    case BACKGROUND:
      return background;
    case BLACK:
      return black;
    case RED:
      return red;
    case YELLOW:
      return yellow;
    case ORANGE:
      return orange;
    case GREEN:
      return green;
    case BLUE:
      return blue;
    case MAGENTA:
      return magenta;
    case GRAY:
      return gray;
    case LIGHTGRAY:
      return lightGray;
    case DARKGRAY:
      return darkGray;
    case LIGHTBLUE:
      return lightBlue;
      default:
        break;
    }
    return null;
  }

  public void setFont(EFont font) {
    switch (font) {
    case GRAPH:
      gc.setFont(fontGraph);
      break;
    case NOTE:
      gc.setFont(fontNote);
      break;
    case SMALL:
      gc.setFont(fontSmall);
      break;
      default:
        break;
    }
  }

  public void setForeground(EColor color) {
    gc.setColor(getColor(color));
  }

  public void setLineStyle(ELineStyle lineStyle) {
    this.lineStyle = lineStyle;
    gc.setStroke(createStroke());
  }

  private Stroke createStroke() {
    float[] dash;
    switch (lineStyle) {
    case SOLID:
      dash = null;
      break;
    case DOT:
      dash = new float[] { 5, };
      break;
    case DASHDOT:
      dash = new float[] { 10, 5, 5, 5, };
      break;
    case PARALLEL:
      dash = new float[] { 10, 5, 10, 5, };
      break;
    default:
      throw new RuntimeException("Unhandled line style!");
    }
    return new BasicStroke(lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 2, dash, 0);
  }

  public void setLineWidth(int width) {
    this.lineWidth = width;
    gc.setStroke(createStroke());
  }

  public void setTransform(float translationX, float translationY, int shadowsize, float magnification) {
    AffineTransform transform = new AffineTransform();
    transform.translate(translationX + shadowsize * magnification, translationY + shadowsize * magnification);
    transform.scale(magnification, magnification);
    gc.setTransform(transform);
  }

  public Point textExtent(String text) {

    String[] lines = text.split(Const.CR);
    int maxWidth = 0;
    for (String line : lines) {
      Rectangle2D bounds = TextUtilities.getTextBounds(line, gc, gc.getFontMetrics());
      if (bounds.getWidth() > maxWidth)
        maxWidth = (int) bounds.getWidth();
    }
    int height = gc.getFontMetrics().getHeight() * lines.length;

    return new Point(maxWidth, height);
  }

  public void drawStepIcon(int x, int y, StepMeta stepMeta) {
    // Draw a blank rectangle to prevent alpha channel problems...
    //
    gc.fillRect(x + xOffset, y + yOffset, iconsize, iconsize);
    String steptype = stepMeta.getStepID();
    BufferedImage im = stepImages.get(steptype);
    if (im != null) // Draw the icon!
    {
      drawPixelatedImage(im, x + xOffset, y + xOffset);

      // gc.drawImage(im, x+xOffset, y+yOffset, observer);
    }
  }

  public void drawJobEntryIcon(int x, int y, JobEntryCopy jobEntryCopy) {
    if (jobEntryCopy == null)
      return; // Don't draw anything

    BufferedImage image = null;

    if (jobEntryCopy.isSpecial()) {
      if (jobEntryCopy.isStart()) {
        image = imageStart;
      }
      if (jobEntryCopy.isDummy()) {
        image = imageDummy;
      }
    } else {
      String configId = jobEntryCopy.getEntry().getPluginId();
      if (configId != null) {
        image = entryImages.get(configId);
      }
    }
    if (image == null) {
      return;
    }

    drawPixelatedImage(image, x + xOffset, y + xOffset);
    // gc.drawImage(image, x+xOffset, y+yOffset, observer);
  }

  public void setAntialias(boolean antiAlias) {
    if (antiAlias) {

      RenderingHints hints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
          RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      hints.add(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
      hints.add(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
      // hints.add(new RenderingHints(RenderingHints.KEY_ALPHA_INTERPOLATION,
      // RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY));
      gc.setRenderingHints(hints);
    }
  }

  public void setBackground(int r, int g, int b) {
    Color color = getColor(r, g, b);
    gc.setBackground(color);
  }

  public void setForeground(int r, int g, int b) {
    Color color = getColor(r, g, b);
    gc.setColor(color);
  }

  private Color getColor(int r, int g, int b) {
    return new Color(r, g, b);
  }

  public void setFont(String fontName, int fontSize, boolean fontBold, boolean fontItalic) {
    int style = Font.PLAIN;
    if (fontBold)
      style = Font.BOLD;
    if (fontItalic)
      style = style | Font.ITALIC;

    Font font = new Font(fontName, style, fontSize);
    gc.setFont(font);
  }

  public Object getImage() {
    return image;
  }

  public void switchForegroundBackgroundColors() {
    Color fg = gc.getColor();
    Color bg = gc.getBackground();

    gc.setColor(bg);
    gc.setBackground(fg);
  }

  public Point getArea() {
    return area;
  }

  /**
   * @return the drawingPixelatedImages
   */
  public boolean isDrawingPixelatedImages() {
    return drawingPixelatedImages;
  }

  /**
   * @param drawingPixelatedImages
   *          the drawingPixelatedImages to set
   */
  public void setDrawingPixelatedImages(boolean drawingPixelatedImages) {
    this.drawingPixelatedImages = drawingPixelatedImages;
  }

  @Override
  public void drawImage(BufferedImage image, int x, int y) {
    gc.drawImage(image, x, y, observer);
  }
}
