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

package org.pentaho.di.starmodeler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.EngineMetaInterface;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.i18n.LanguageChoice;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.ObjectRevision;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.RepositoryObjectType;
import org.pentaho.metadata.model.Domain;
import org.pentaho.metadata.model.LogicalTable;
import org.pentaho.metadata.model.concept.types.LocalizedString;
import org.pentaho.metadata.util.SerializationService;

public class StarDomain implements EngineMetaInterface {

  private Domain domain;
  private RepositoryDirectoryInterface repositoryDirectory;
  private String locale;
  private ObjectId objectId;
  private ObjectRevision objectRevision;
  private String filename;
  private boolean changed;
  private Date createdDate;
  private String createdUser;
  private Date modifiedDate;
  private String modifiedUser;

  private List<LogicalTable> sharedDimensions;

  public StarDomain() {
    this.domain = new Domain();
    locale = LanguageChoice.getInstance().getDefaultLocale().toString();
    changed=true;
    sharedDimensions = new ArrayList<LogicalTable>();
  }

  @Override
  public void setRepositoryDirectory(RepositoryDirectoryInterface repositoryDirectory) {
    this.repositoryDirectory = repositoryDirectory;
  }

  @Override
  public void setName(String name) {
    domain.setName(new LocalizedString(locale, name));
  }

  @Override
  public String getDescription() {
    return ConceptUtil.getDescription(domain, locale);
  }

  @Override
  public void setDescription(String description) {
    domain.setDescription(new LocalizedString(locale, description));
  }

  @Override
  public ObjectId getObjectId() {
    return objectId;
  }

  @Override
  public RepositoryObjectType getRepositoryElementType() {
    return RepositoryObjectType.PLUGIN;
  }

  @Override
  public ObjectRevision getObjectRevision() {
    return objectRevision;
  }

  @Override
  public void setObjectRevision(ObjectRevision objectRevision) {
    this.objectRevision = objectRevision;
  }

  @Override
  public void setFilename(String filename) {
    this.filename = filename;
  }

  @Override
  public String getName() {
    return ConceptUtil.getName(domain, locale);
  }

  @Override
  public void nameFromFilename() {
    setName( Const.createName(filename) );
  }

  @Override
  public void clearChanged() {
    changed=false;
  }

  @Override
  public String getXML() throws KettleException {
    return new SerializationService().serializeDomain(domain);
  }

  @Override
  public String getFileType() {
    return "star";
  }

  @Override
  public String[] getFilterNames() {
    return new String[] { "Star models", };
  }

  @Override
  public String[] getFilterExtensions() {
    return new String[] { "*.star", };
  }

  @Override
  public String getDefaultExtension() {
    return "star";
  }

  @Override
  public void setObjectId(ObjectId objectId) {
    this.objectId=objectId;
  }

  @Override
  public Date getCreatedDate() {
    return createdDate;
  }

  @Override
  public void setCreatedDate(Date createdDate) {
    this.createdDate = createdDate;
  }

  @Override
  public boolean canSave() {
    return true; // changed;
  }

  @Override
  public String getCreatedUser() {
    return createdUser;
  }

  @Override
  public void setCreatedUser(String createdUser) {
    this.createdUser = createdUser;
  }

  @Override
  public Date getModifiedDate() {
    return modifiedDate;
  }

  @Override
  public void setModifiedDate(Date modifiedDate) {
    this.modifiedDate = modifiedDate;
  }

  @Override
  public void setModifiedUser(String modifiedUser) {
    this.modifiedUser = modifiedUser;
  }

  @Override
  public String getModifiedUser() {
    return modifiedUser;
  }

  @Override
  public RepositoryDirectoryInterface getRepositoryDirectory() {
    return repositoryDirectory;
  }

  @Override
  public String getFilename() {
    return filename;
  }

  @Override
  public void saveSharedObjects() throws KettleException {
    // Nothing so far
  }

  @Override
  public void setInternalKettleVariables() {
    // Not applicable
  }

  /**
   * @return the domain
   */
  public Domain getDomain() {
    return domain;
  }

  /**
   * @param domain the domain to set
   */
  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public boolean hasChanged() {
    return changed;
  }

  public void setChanged(boolean changed) {
    this.changed = changed;
  }

  public List<LogicalTable> getSharedDimensions() {
    return sharedDimensions;
  }
}
