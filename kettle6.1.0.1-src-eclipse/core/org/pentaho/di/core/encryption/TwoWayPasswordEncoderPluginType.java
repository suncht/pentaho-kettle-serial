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

package org.pentaho.di.core.encryption;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.plugins.BasePluginType;
import org.pentaho.di.core.plugins.PluginAnnotationType;
import org.pentaho.di.core.plugins.PluginFolderInterface;
import org.pentaho.di.core.plugins.PluginMainClassType;
import org.pentaho.di.core.plugins.PluginTypeInterface;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This class represents the value meta plugin type.
 *
 * @author matt
 *
 */

@PluginMainClassType( TwoWayPasswordEncoderInterface.class )
@PluginAnnotationType( TwoWayPasswordEncoderPlugin.class )
public class TwoWayPasswordEncoderPluginType extends BasePluginType implements PluginTypeInterface {

  private static TwoWayPasswordEncoderPluginType twoWayPasswordEncoderPluginType;

  private TwoWayPasswordEncoderPluginType() {
    super( TwoWayPasswordEncoderPlugin.class, "TWOWAYPASSWORDENCODERPLUGIN", "TwoWayPasswordEncoder" );
    populateFolders( "passwordencoder" );
  }

  public static TwoWayPasswordEncoderPluginType getInstance() {
    if ( twoWayPasswordEncoderPluginType == null ) {
      twoWayPasswordEncoderPluginType = new TwoWayPasswordEncoderPluginType();
    }
    return twoWayPasswordEncoderPluginType;
  }

  /**
   * Scan & register internal plugins
   */
  @Override
  protected void registerNatives() throws KettlePluginException {
    // Scan the native steps...
    //
    String passwordEncoderPluginsXmlFile = Const.XML_FILE_KETTLE_PASSWORD_ENCODER_PLUGINS;
    String alternative = System.getProperty( Const.KETTLE_PASSWORD_ENCODER_PLUGINS_FILE, null );
    if ( !Const.isEmpty( alternative ) ) {
      passwordEncoderPluginsXmlFile = alternative;
    }

    // Load the plugins for this file...
    //
    try {
      InputStream inputStream = getClass().getResourceAsStream( passwordEncoderPluginsXmlFile );
      if ( inputStream == null ) {
        inputStream = getClass().getResourceAsStream( "/" + passwordEncoderPluginsXmlFile );
      }
      // Retry to load a regular file...
      if ( inputStream == null && !Const.isEmpty( alternative ) ) {
        try {
          inputStream = new FileInputStream( passwordEncoderPluginsXmlFile );
        } catch ( Exception e ) {
          throw new KettlePluginException( "Unable to load native password encoder plugins '"
            + passwordEncoderPluginsXmlFile + "'", e );
        }
      }
      if ( inputStream == null ) {
        return;
        // throw new KettlePluginException( "Unable to find native password encoder plugins definition file: "  + passwordEncoderPluginsXmlFile );
      }
      Document document = XMLHandler.loadXMLFile( inputStream, null, true, false );

      // Document document = XMLHandler.loadXMLFile(kettleStepsXmlFile);

      Node stepsNode = XMLHandler.getSubNode( document, "password-encoder-plugins" );
      List<Node> stepNodes = XMLHandler.getNodes( stepsNode, "password-encoder-plugin" );
      for ( Node stepNode : stepNodes ) {
        registerPluginFromXmlResource( stepNode, null, this.getClass(), true, null );
      }

    } catch ( KettleXMLException e ) {
      throw new KettlePluginException( "Unable to read the kettle metadata plugins XML config file: "
        + passwordEncoderPluginsXmlFile, e );
    }
  }

  @Override
  protected void registerXmlPlugins() throws KettlePluginException {
    for ( PluginFolderInterface folder : pluginFolders ) {

      if ( folder.isPluginXmlFolder() ) {
        List<FileObject> pluginXmlFiles = findPluginXmlFiles( folder.getFolder() );
        for ( FileObject file : pluginXmlFiles ) {

          try {
            Document document = XMLHandler.loadXMLFile( file );
            Node pluginNode = XMLHandler.getSubNode( document, "plugin" );
            if ( pluginNode != null ) {
              registerPluginFromXmlResource( pluginNode, KettleVFS.getFilename( file.getParent() ), this
                .getClass(), false, file.getParent().getURL() );
            }
          } catch ( Exception e ) {
            // We want to report this plugin.xml error, perhaps an XML typo or something like that...
            //
            log.logError( "Error found while reading password encoder plugin.xml file: " + file.getName().toString(), e );
          }
        }
      }
    }
  }

  @Override
  protected String extractCategory( Annotation annotation ) {
    return null;
  }

  @Override
  protected String extractDesc( Annotation annotation ) {
    return ( (TwoWayPasswordEncoderPlugin) annotation ).description();
  }

  @Override
  protected String extractID( Annotation annotation ) {
    return ( (TwoWayPasswordEncoderPlugin) annotation ).id();
  }

  @Override
  protected String extractName( Annotation annotation ) {
    return ( (TwoWayPasswordEncoderPlugin) annotation ).name();
  }

  @Override
  protected String extractImageFile( Annotation annotation ) {
    return null;
  }

  @Override
  protected boolean extractSeparateClassLoader( Annotation annotation ) {
    return ( (TwoWayPasswordEncoderPlugin) annotation ).isSeparateClassLoaderNeeded();
  }

  @Override
  protected String extractI18nPackageName( Annotation annotation ) {
    return null;
  }

  @Override
  protected void addExtraClasses( Map<Class<?>, String> classMap, Class<?> clazz, Annotation annotation ) {
  }

  @Override
  protected String extractDocumentationUrl( Annotation annotation ) {
    return ( (TwoWayPasswordEncoderPlugin) annotation ).documentationUrl();
  }

  @Override
  protected String extractCasesUrl( Annotation annotation ) {
    return ( (TwoWayPasswordEncoderPlugin) annotation ).casesUrl();
  }

  @Override
  protected String extractForumUrl( Annotation annotation ) {
    return ( (TwoWayPasswordEncoderPlugin) annotation ).forumUrl();
  }

  @Override
  protected String extractClassLoaderGroup( Annotation annotation ) {
    return ( (TwoWayPasswordEncoderPlugin) annotation ).classLoaderGroup();
  }
}
