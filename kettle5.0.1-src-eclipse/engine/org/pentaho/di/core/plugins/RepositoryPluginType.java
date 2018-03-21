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

package org.pentaho.di.core.plugins;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs.FileObject;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.annotations.RepositoryPlugin;
import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryMeta;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This class represents the repository plugin type.
 * 
 * @author matt
 *
 */
@PluginMainClassType(Repository.class)
@PluginExtraClassTypes(
		classTypes = { RepositoryMeta.class }, 
		xmlNodeNames = { "meta-classname" }
	)
@PluginAnnotationType(RepositoryPlugin.class)
public class RepositoryPluginType extends BasePluginType implements PluginTypeInterface {

	private static RepositoryPluginType pluginType;
	
	private RepositoryPluginType() {
		super(RepositoryPlugin.class, "REPOSITORY_TYPE", "Repository type");
		populateFolders("repositories");
	}
	
	public static RepositoryPluginType getInstance() {
		if (pluginType==null) {
			pluginType=new RepositoryPluginType();
		}
		return pluginType;
	}
	

	/**
	 * Scan & register internal step plugins
	 */
	protected void registerNatives() throws KettlePluginException {
		// Scan the native repository types...
		//
		String xmlFile = Const.XML_FILE_KETTLE_REPOSITORIES;
		
		// Load the plugins for this file...
		//
		try {
			InputStream inputStream = getClass().getResourceAsStream(xmlFile);
			if (inputStream==null) {
				inputStream =  getClass().getResourceAsStream("/"+xmlFile);
			}
			if (inputStream==null) {
				throw new KettlePluginException("Unable to find native repository type definition file: "+xmlFile);
			}
			Document document = XMLHandler.loadXMLFile(inputStream, null, true, false);
			
			// Document document = XMLHandler.loadXMLFile(kettleStepsXmlFile);
			
			Node repsNode = XMLHandler.getSubNode(document, "repositories");
			List<Node> repsNodes = XMLHandler.getNodes(repsNode, "repository");
			for (Node repNode : repsNodes) {
				registerPluginFromXmlResource(repNode, null, this.getClass(), true, null);
			}			
		} catch (KettleXMLException e) {
			throw new KettlePluginException("Unable to read the kettle repositories XML config file: "+xmlFile, e);
		}
	}


	
	protected void registerXmlPlugins() throws KettlePluginException {
		for (PluginFolderInterface folder : pluginFolders) {
			
			if (folder.isPluginXmlFolder()) {
				List<FileObject> pluginXmlFiles = findPluginXmlFiles(folder.getFolder());
				for (FileObject file : pluginXmlFiles) {
					
					try {
						Document document = XMLHandler.loadXMLFile(file);
						Node pluginNode = XMLHandler.getSubNode(document, "plugin");

						registerPluginFromXmlResource(pluginNode, KettleVFS.getFilename(file.getParent()), this.getClass(), false, file.getParent().getURL());
					} catch(Exception e) {
						// We want to report this plugin.xml error, perhaps an XML typo or something like that...
						//
						log.logError("Error found while reading repository plugin.xml file: "+file.getName().toString(), e);
					}
				}
			}
		}
	}

  @Override
  protected String extractCategory(Annotation annotation) {
    return "";
  }

  @Override
  protected String extractDesc(Annotation annotation) {
    return ((RepositoryPlugin) annotation).description();
  }

  @Override
  protected String extractID(Annotation annotation) {
    return ((RepositoryPlugin) annotation).id();
  }

  @Override
  protected String extractName(Annotation annotation) {
    return ((RepositoryPlugin) annotation).name();
  }

  @Override
  protected String extractImageFile(Annotation annotation) {
    return null;
  }

  @Override
  protected boolean extractSeparateClassLoader(Annotation annotation) {
    return false;
  }

  @Override
  protected String extractI18nPackageName(Annotation annotation) {
    return ((RepositoryPlugin) annotation).i18nPackageName();
  }
  
	/**
	 * Extract extra classes information from a plugin annotation.
	 * 
	 * @param classMap
	 * @param annotation
	 */
	public void addExtraClasses(Map<Class<?>, String> classMap, Class<?> clazz, Annotation annotation) {
		RepositoryPlugin repositoryPlugin = (RepositoryPlugin) annotation;
		
		classMap.put(RepositoryMeta.class, repositoryPlugin.metaClass());
	}
	
  @Override
  protected String extractDocumentationUrl(Annotation annotation) {
    return null;
  }
  
  @Override
  protected String extractCasesUrl(Annotation annotation) {
    return null;
  }

  @Override
  protected String extractForumUrl(Annotation annotation) {
    return null;
  }

}
