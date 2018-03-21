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

import java.lang.annotation.Annotation;
import java.util.Map;

import org.pentaho.di.core.exception.KettlePluginException;
import org.pentaho.di.core.gui.GUIOption;

/**
 * Plugins of this type can extend to capabilities of the PluginRegiestry
 *
 * User: nbaker
 * Date: 3/14/11
 */
@PluginMainClassType(PluginRegistryExtension.class)
@PluginExtraClassTypes(classTypes = { GUIOption.class })
@PluginAnnotationType(RegistryPlugin.class)
public class PluginRegistryPluginType extends BasePluginType implements
    PluginTypeInterface {

  private static PluginRegistryPluginType INSTANCE = new PluginRegistryPluginType();

  public PluginRegistryPluginType(){
    super(RegistryPlugin.class, "Plugin Extensions", "Plugin Registry Extension Types");
    populateFolders("pluginRegistry");
  }

  public static PluginRegistryPluginType getInstance(){
    return INSTANCE;
  }
  
  @Override
  protected void addExtraClasses(Map<Class<?>, String> classMap, Class<?> clazz, Annotation annotation) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void registerNatives() throws KettlePluginException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected void registerXmlPlugins() throws KettlePluginException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  protected String extractID(Annotation annotation) {
    return ((RegistryPlugin) annotation).id();
  }

  @Override
  protected String extractName(Annotation annotation) {
    return ((RegistryPlugin) annotation).name();
  }

  @Override
  protected String extractDesc(Annotation annotation) {
    return ((RegistryPlugin) annotation).description();
  }

  @Override
  protected String extractCategory(Annotation annotation) {
    return "";
  }

  @Override
  protected String extractImageFile(Annotation annotation) {
    return "";
  }

  @Override
  protected boolean extractSeparateClassLoader(Annotation annotation) {
    return false;
  }

  @Override
  protected String extractI18nPackageName(Annotation annotation) {
    return null;
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
