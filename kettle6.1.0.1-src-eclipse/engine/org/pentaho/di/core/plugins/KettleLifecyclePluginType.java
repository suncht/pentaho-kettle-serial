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

package org.pentaho.di.core.plugins;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.annotations.KettleLifecyclePlugin;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.lifecycle.KettleLifecycleListener;
import org.pentaho.di.core.xml.XMLHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Defines a Kettle Environment lifecycle plugin type. These plugins are invoked at Kettle Environment initialization
 * and shutdown.
 */
@PluginMainClassType( KettleLifecycleListener.class )
@PluginAnnotationType( KettleLifecyclePlugin.class )
public class KettleLifecyclePluginType extends BasePluginType implements PluginTypeInterface {

  private static KettleLifecyclePluginType pluginType;

  private KettleLifecyclePluginType() {
    super( KettleLifecyclePlugin.class, "KETTLE LIFECYCLE LISTENERS", "Kettle Lifecycle Listener Plugin Type" );
    // We must call populate folders so PluginRegistry will look in the correct
    // locations for plugins (jars with annotations)
    populateFolders( null );
  }

  public static synchronized KettleLifecyclePluginType getInstance() {
    if ( pluginType == null ) {
      pluginType = new KettleLifecyclePluginType();
    }
    return pluginType;
  }

  @Override
  protected void registerNatives() throws KettlePluginException {
    // Scan the native repository types...
    //
    String xmlFile = Const.XML_FILE_KETTLE_LIFECYCLE_LISTENERS;

    // Load the plugins for this file...
    //
    try {
      InputStream inputStream = getClass().getResourceAsStream( xmlFile );
      if ( inputStream == null ) {
        inputStream = getClass().getResourceAsStream( "/" + xmlFile );
      }
      if ( inputStream == null ) {
        throw new KettlePluginException( "Unable to find native repository type definition file: " + xmlFile );
      }
      Document document = XMLHandler.loadXMLFile( inputStream, null, true, false );

      // Document document = XMLHandler.loadXMLFile(kettleStepsXmlFile);

      Node repsNode = XMLHandler.getSubNode( document, "listeners" );
      List<Node> repsNodes = XMLHandler.getNodes( repsNode, "listener" );
      for ( Node repNode : repsNodes ) {
        registerPluginFromXmlResource( repNode, null, this.getClass(), true, null );
      }
    } catch ( KettleXMLException e ) {
      throw new KettlePluginException( "Unable to read the kettle repositories XML config file: " + xmlFile, e );
    }
  }

  @Override
  protected void registerXmlPlugins() throws KettlePluginException {
    // Not supported
  }

  @Override
  protected String extractID( Annotation annotation ) {
    return ( (KettleLifecyclePlugin) annotation ).id();
  }

  @Override
  protected String extractName( Annotation annotation ) {
    return ( (KettleLifecyclePlugin) annotation ).name();
  }

  @Override
  protected String extractDesc( Annotation annotation ) {
    return "";
  }

  @Override
  protected String extractCategory( Annotation annotation ) {
    // No images, not shown in UI
    return "";
  }

  @Override
  protected String extractImageFile( Annotation annotation ) {
    // No images, not shown in UI
    return null;
  }

  @Override
  protected boolean extractSeparateClassLoader( Annotation annotation ) {
    return ( (KettleLifecyclePlugin) annotation ).isSeparateClassLoaderNeeded();
  }

  @Override
  protected String extractI18nPackageName( Annotation annotation ) {
    // No UI, no i18n
    return null;
  }

  @Override
  protected void addExtraClasses( Map<Class<?>, String> classMap, Class<?> clazz, Annotation annotation ) {
    classMap.put( KettleLifecyclePlugin.class, clazz.getName() );
  }

  @Override
  protected String extractDocumentationUrl( Annotation annotation ) {
    return null;
  }

  @Override
  protected String extractCasesUrl( Annotation annotation ) {
    return null;
  }

  @Override
  protected String extractForumUrl( Annotation annotation ) {
    return null;
  }

  @Override
  protected String extractClassLoaderGroup( Annotation annotation ) {
    return ( (KettleLifecyclePlugin) annotation ).classLoaderGroup();
  }
}
