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

package org.pentaho.di.ui.spoon;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.antlr.misc.OrderedHashSet;
import org.eclipse.swt.widgets.Composite;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.LanguageChoice;
import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.XulException;
import org.pentaho.ui.xul.XulOverlay;
import org.pentaho.ui.xul.containers.XulDeck;
import org.pentaho.ui.xul.containers.XulToolbar;
import org.pentaho.ui.xul.containers.XulVbox;
import org.pentaho.ui.xul.dom.Document;
import org.pentaho.ui.xul.impl.XulEventHandler;
import org.pentaho.ui.xul.swt.tags.SwtDeck;
import org.pentaho.ui.xul.swt.tags.SwtToolbarbutton;

/**
 * Singleton Object controlling SpoonPerspectives.
 * 
 * A Perspective is an optional Spoon mode that can be added by a SpoonPlugin.
 * Perspectives take over the look of the entire application by replacing the main UI area.
 * 
 * @author nbaker
 *
 */
public class SpoonPerspectiveManager {
  private static SpoonPerspectiveManager instance = new SpoonPerspectiveManager();

  private final Map<Class<? extends SpoonPerspective>, SpoonPerspective> perspectives;

  private final Map<SpoonPerspective, PerspectiveInitializer> initializerMap;

  @SuppressWarnings("rawtypes")
  private final OrderedHashSet orderedPerspectives;

  private XulDeck deck;

  private SpoonPerspective activePerspective;

  private XulDomContainer domContainer;

  private boolean forcePerspective = false;

  private String startupPerspective = null;

  public String getStartupPerspective() {
    return startupPerspective;
  }

  public void setStartupPerspective(String startupPerspective) {
    this.startupPerspective = startupPerspective;
  }

  protected static class SpoonPerspectiveComparator implements Comparator<SpoonPerspective> {
    public int compare(SpoonPerspective o1, SpoonPerspective o2) {
      return o1.getId().compareTo(o2.getId());
    }
  }

  private class PerspectiveInitializer {
    private final SpoonPerspective per;

    private final XulVbox box;

    private final XulToolbar mainToolbar;

    private final SwtToolbarbutton btn;

    public PerspectiveInitializer(SpoonPerspective per, XulVbox box, XulToolbar mainToolbar, SwtToolbarbutton btn) {
      super();
      this.per = per;
      this.box = box;
      this.mainToolbar = mainToolbar;
      this.btn = btn;
    }

    public void initialize() {
      per.getUI().setParent((Composite) box.getManagedObject());
      per.getUI().layout();
      ((Composite) mainToolbar.getManagedObject()).layout(true, true);

      per.addPerspectiveListener(new SpoonPerspectiveListener() {
        public void onActivation() {
          btn.setSelected(true);
        }

        public void onDeactication() {
          btn.setSelected(false);
        }
      });
    }
  }

  @SuppressWarnings("rawtypes")
  private SpoonPerspectiveManager() {
    perspectives = new LinkedHashMap<Class<? extends SpoonPerspective>, SpoonPerspective>();
    initializerMap = new HashMap<SpoonPerspective, PerspectiveInitializer>();
    orderedPerspectives = new OrderedHashSet();
  }

  /**
   * Returns the single instance of this class.
   * 
   * @return SpoonPerspectiveManager instance.
   */
  public static SpoonPerspectiveManager getInstance() {
    return instance;
  }

  /**
   * Sets the deck used by the Perspective Manager to display Perspectives in.
   * 
   * @param deck
   */
  public void setDeck(XulDeck deck) {
    this.deck = deck;
  }

  /**
   * Receives the main XUL document comprising the menuing system and main layout of Spoon. 
   * Perspectives are able to modify these areas when activated. Any other areas need to be 
   * modified via a SpoonPlugin.
   * 
   * @param doc
   */
  public void setXulDoc(XulDomContainer doc) {
    this.domContainer = doc;
  }

  /**
   * Adds a SpoonPerspective making it available to be activated later.
   * 
   * @param perspective
   */
  public void addPerspective(SpoonPerspective perspective) {
    if (activePerspective == null) {
      activePerspective = perspective;
    }
    perspectives.put(perspective.getClass(), perspective);
    orderedPerspectives.add(perspective);
    if (domContainer != null) {
      initialize();
    }
  }

  /**
   * Returns an unmodifiable List of perspectives in no set order.
   * 
   * @return
   */
  @SuppressWarnings("unchecked")
  public List<SpoonPerspective> getPerspectives() {
    return Collections.unmodifiableList(orderedPerspectives.elements());
  }

  private void unloadPerspective(SpoonPerspective per) {
    per.setActive(false);
    List<XulOverlay> overlays = per.getOverlays();
    if (overlays != null) {
      for (XulOverlay overlay : overlays) {
        try {
          domContainer.removeOverlay(overlay.getOverlayUri());
        } catch (XulException e) {
          e.printStackTrace();
        }
      }
    }
    Spoon.getInstance().enableMenus();
  }

  /**
   * 
   * Activates the given instance of the class literal passed in. Activating a perspective first 
   * deactivates the current perspective removing any overlays its applied to the UI. It then switches
   * the main deck to display the perspective UI and applies the optional overlays to the main Spoon
   * XUL container.
   * 
   * @param clazz SpoonPerspective class literal  
   * @throws KettleException throws a KettleException if no perspective is found for the given parameter
   */
  public void activatePerspective(Class<? extends SpoonPerspective> clazz) throws KettleException {

    if (this.forcePerspective) {
      // we are currently prevented from switching perspectives
      return;
    }
    SpoonPerspective sp = perspectives.get(clazz);
    if (sp == null) {
      throw new KettleException("Could not locate perspective by class: " + clazz);
    }
    PerspectiveInitializer perspectiveInitializer = initializerMap.remove(sp);
    if (perspectiveInitializer != null) {
      perspectiveInitializer.initialize();
    }
    unloadPerspective(activePerspective);
    activePerspective = sp;

    List<XulOverlay> overlays = sp.getOverlays();
    if (overlays != null) {
      for (XulOverlay overlay : overlays) {
        try {
          ResourceBundle res = null;
          if (overlay.getResourceBundleUri() != null) {
            try {
              res = ResourceBundle.getBundle(overlay.getResourceBundleUri());
            } catch (MissingResourceException ignored) {
            }
          } else {
            try {
              res = ResourceBundle.getBundle(overlay.getOverlayUri().replace(".xul", ".properties"));
            } catch (MissingResourceException ignored) {
            }
          }
          if (res == null) {
            res = new XulSpoonResourceBundle(sp.getClass());
          }
          domContainer.loadOverlay(overlay.getOverlayUri(), res);
        } catch (XulException e) {
          e.printStackTrace();
        }
      }
    }

    List<XulEventHandler> theXulEventHandlers = sp.getEventHandlers();
    if (theXulEventHandlers != null) {
      for (XulEventHandler handler : theXulEventHandlers) {
        domContainer.addEventHandler(handler);
      }
    }

    sp.setActive(true);
    deck.setSelectedIndex(deck.getChildNodes().indexOf(deck.getElementById("perspective-" + sp.getId())));
    Spoon.getInstance().enableMenus();
  }

  /**
   * Returns the current active perspective.
   * @return active SpoonPerspective
   */
  public SpoonPerspective getActivePerspective() {
    return activePerspective;
  }

  /**
   * Returns whether this perspective manager is prevented from switching perspectives
   */
  public boolean isForcePerspective() {
    return forcePerspective;
  }

  /**
   * Sets whether this perspective manager is prevented from switching perspectives.
   * This is used when a startup perspective is requested on the command line parameter
   * to prevent other perpsectives from openeing.
   */
  public void setForcePerspective(boolean forcePerspective) {
    this.forcePerspective = forcePerspective;
  }

  public void removePerspective(SpoonPerspective per) {
    perspectives.remove(per);
    orderedPerspectives.remove(per);
    Document document = domContainer.getDocumentRoot();

    XulComponent comp = document.getElementById("perspective-" + per.getId());
    comp.getParent().removeChild(comp);

    comp = document.getElementById("perspective-btn-" + per.getId());
    comp.getParent().removeChild(comp);
    XulToolbar mainToolbar = (XulToolbar) domContainer.getDocumentRoot().getElementById("main-toolbar");
    ((Composite) mainToolbar.getManagedObject()).layout(true, true);

    deck.setSelectedIndex(0);

  }

  private List<SpoonPerspective> installedPerspectives = new ArrayList<SpoonPerspective>();

  public void initialize() {
    XulToolbar mainToolbar = (XulToolbar) domContainer.getDocumentRoot().getElementById("main-toolbar");
    SwtDeck deck = (SwtDeck) domContainer.getDocumentRoot().getElementById("canvas-deck");

    int y = 0;
    int perspectiveIdx = 0;
    Class<? extends SpoonPerspective> perClass = null;

    List<SpoonPerspective> perspectives = getPerspectives();
    if (this.startupPerspective != null) {
      for (int i = 0; i < perspectives.size(); i++) {
        if (perspectives.get(i).getId().equals(this.startupPerspective)) {
          perspectiveIdx = i;
          break;
        }
      }
    }

    for (SpoonPerspective per : getPerspectives()) {
      if (installedPerspectives.contains(per)) {
        y++;
        continue;
      }
      String name = per.getDisplayName(LanguageChoice.getInstance().getDefaultLocale());
      InputStream in = per.getPerspectiveIcon();

      SwtToolbarbutton btn = null;
      try {
        btn = (SwtToolbarbutton) domContainer.getDocumentRoot().createElement("toolbarbutton");
      } catch (XulException e) {
        e.printStackTrace();
      }
      btn.setType("toggle");
      btn.setLabel(name);
      btn.setTooltiptext(name);
      btn.setOnclick("spoon.loadPerspective(" + y + ")");
      btn.setId("perspective-btn-" + per.getId());
      mainToolbar.addChild(btn);
      if (in != null) {
        btn.setImageFromStream(in);
        try {
          in.close();
        } catch (IOException e1) {
        }
      }

      XulVbox box = deck.createVBoxCard();
      box.setId("perspective-" + per.getId());
      box.setFlex(1);
      deck.addChild(box);

      PerspectiveInitializer perspectiveInitializer = new PerspectiveInitializer(per, box, mainToolbar, btn);
      //Need to force init for main perspective even if it won't be shown
      if (perspectiveIdx == y || y == 0) {
        if (perspectiveIdx == y) {
          //we have a startup perspective. Hold onto the class
          btn.setSelected(true);
          perClass = per.getClass();
        }
        // force init
        perspectiveInitializer.initialize();
      } else {
        initializerMap.put(per, perspectiveInitializer);
      }
      y++;
      installedPerspectives.add(per);
    }
    deck.setSelectedIndex(perspectiveIdx);
    if (perClass != null) {
      // activate the startup perspective
      try {
        activatePerspective(perClass);
        // stop other perspectives from opening
        SpoonPerspectiveManager.getInstance().setForcePerspective(true);
      } catch (KettleException e) {
        // TODO Auto-generated catch block
      }
    }

  }
}