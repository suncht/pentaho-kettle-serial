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

package org.pentaho.di.shared;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs.FileObject;
import org.pentaho.di.cluster.ClusterSchema;
import org.pentaho.di.cluster.SlaveServer;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleXMLException;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.core.vfs.KettleVFS;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.partition.PartitionSchema;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.metastore.api.IMetaStore;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Based on a piece of XML, this factory will give back a list of objects.
 * In other words, it does XML de-serialisation
 * 
 * @author Matt
 *
 */
public class SharedObjects
{
	private static Class<?> PKG = SharedObjects.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private static final String XML_TAG = "sharedobjects";
	
	private String filename;
	
	private Map<SharedEntry, SharedObjectInterface> objectsMap;
	
    private class SharedEntry
    {
        public String className;
        public String objectName;
        
        /**
         * @param className
         * @param objectName
         */
        public SharedEntry(String className, String objectName)
        {
            this.className = className;
            this.objectName = objectName;
        }
        
        public boolean equals(Object obj)
        {
            SharedEntry sharedEntry = (SharedEntry) obj;
            return className.equals(sharedEntry.className) && objectName.equalsIgnoreCase(objectName);
        }
        
        public int hashCode()
        {
            return className.hashCode() ^ objectName.hashCode();
        }
        
    }

    public SharedObjects(String sharedObjectsFile) throws KettleXMLException
    {
        try
        {
            this.filename = createFilename(sharedObjectsFile);
            this.objectsMap = new Hashtable<SharedEntry, SharedObjectInterface>();
    
            // Extra information
            FileObject file = KettleVFS.getFileObject(filename);
            
            // If we have a shared file, load the content, otherwise, just keep this one empty
            if (file.exists()) 
            {
                Document document = XMLHandler.loadXMLFile(file);
                Node sharedObjectsNode = XMLHandler.getSubNode(document, XML_TAG);
                if (sharedObjectsNode!=null)
                {
                    List<SlaveServer> privateSlaveServers = new ArrayList<SlaveServer>();
                    List<DatabaseMeta> privateDatabases = new ArrayList<DatabaseMeta>();

                    
                    NodeList childNodes = sharedObjectsNode.getChildNodes();
                    // First load databases & slaves
                    //
                    for (int i=0;i<childNodes.getLength();i++)
                    {
                        Node node = childNodes.item(i);
                        String nodeName = node.getNodeName();
                        
                        SharedObjectInterface isShared = null;
        
                        if (nodeName.equals(DatabaseMeta.XML_TAG))
                        {    
                            DatabaseMeta sharedDatabaseMeta = new DatabaseMeta(node);
                            isShared = sharedDatabaseMeta;
                            privateDatabases.add(sharedDatabaseMeta);
                        }
                        else if (nodeName.equals(SlaveServer.XML_TAG)) 
                        {
                            SlaveServer sharedSlaveServer = new SlaveServer(node);
                            isShared = sharedSlaveServer;
                            privateSlaveServers.add(sharedSlaveServer);
                        }
    
                        if (isShared!=null)
                        {
                            isShared.setShared(true);
                            storeObject(isShared);
                        }
                    }
    
                    // Then load the other objects that might reference databases & slaves
                    //
                    for (int i=0;i<childNodes.getLength();i++)
                    {
                        Node node = childNodes.item(i);
                        String nodeName = node.getNodeName();
                        
                        SharedObjectInterface isShared = null;
        
                        if (nodeName.equals(StepMeta.XML_TAG))        
                        { 
                            StepMeta stepMeta = new StepMeta(node, privateDatabases, (IMetaStore)null);
                            stepMeta.setDraw(false); // don't draw it, keep it in the tree.
                            isShared = stepMeta;
                        }
                        else if (nodeName.equals(PartitionSchema.XML_TAG)) 
                        {
                            isShared = new PartitionSchema(node);
                        }
                        else if (nodeName.equals(ClusterSchema.XML_TAG)) 
                        {   
                            isShared = new ClusterSchema(node, privateSlaveServers);
                        }
                        
                        if (isShared!=null)
                        {
                            isShared.setShared(true);
                            storeObject(isShared);
                        }
                    }
                }
            }
        }
        catch(Exception e)
        {
            throw new KettleXMLException(BaseMessages.getString(PKG, "SharedOjects.Readingfile.UnexpectedError", sharedObjectsFile), e);
        }
    }
    
    public static final String createFilename(String sharedObjectsFile)
    {
        String filename;
        if (Const.isEmpty(sharedObjectsFile))
        {
            // First fallback is the environment/kettle variable ${KETTLE_SHARED_OBJECTS}
            // This points to the file
            filename = Variables.getADefaultVariableSpace().getVariable(Const.KETTLE_SHARED_OBJECTS);
            
            // Last line of defence...
            if (Const.isEmpty(filename))
            {
                filename = Const.getSharedObjectsFile();
            }
        }
        else
        {
            filename = sharedObjectsFile;
        }
        return filename;
    }

    public SharedObjects() throws KettleXMLException
    {
        this(null);
    }

    public Map<SharedEntry, SharedObjectInterface> getObjectsMap()
    {
        return objectsMap;
    }

    public void setObjectsMap(Map<SharedEntry, SharedObjectInterface> objects)
    {
        this.objectsMap = objects;
    }

    /**
     * Store the sharedObject in the object map.
     * It is possible to have 2 different types of shared object with the same name.
     * They will be stored separately.
     * 
     * @param sharedObject
     */
    public void storeObject(SharedObjectInterface sharedObject)
    {
        SharedEntry key = new SharedEntry(sharedObject.getClass().getName(), sharedObject.getName());
        objectsMap.put(key, sharedObject);
    }
    
    /**
     * Remove the sharedObject from the object map.
     * 
     * @param sharedObject
     */
    public void removeObject(SharedObjectInterface sharedObject)
    {
        SharedEntry key = new SharedEntry(sharedObject.getClass().getName(), sharedObject.getName());
        objectsMap.remove(key);
    }

    public void saveToFile() throws IOException, KettleException {
      FileObject fileObject = KettleVFS.getFileObject(filename);
  
      if (fileObject.exists()) {
        // Create a backup before overwriting...
        //
        FileObject backupFile = KettleVFS.getFileObject(filename + ".backup");
        fileObject.moveTo(backupFile);
      }
  
      OutputStream outputStream = KettleVFS.getOutputStream(fileObject, false);
  
      PrintStream out = new PrintStream(outputStream);
  
      out.print(XMLHandler.getXMLHeader(Const.XML_ENCODING));
      out.println("<" + XML_TAG + ">");
  
      Collection<SharedObjectInterface> collection = objectsMap.values();
      for (SharedObjectInterface sharedObject : collection) {
        out.println(sharedObject.getXML());
      }
  
      out.println("</" + XML_TAG + ">");
  
      out.flush();
      out.close();
      outputStream.close();
    }

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
  /**
   * Return the shared object with the given class and name
   * @param clazz The class of the shared object
   * @param objectName the name of the object
   * @return The shared object or null if none was found.
   */
  public SharedObjectInterface getSharedObject(Class<SharedObjectInterface> clazz, String objectName) {
    return getSharedObject(clazz.getName(), objectName);   
  }
  
  /**
   * Return the shared object with the given class name and object name
   * @param clasName The class name of the shared object
   * @param objectName the name of the object
   * @return The shared object or null if none was found.
   */
  public SharedObjectInterface getSharedObject(String className, String objectName) {
    SharedEntry entry = new SharedEntry(className, objectName);
    return objectsMap.get(entry);   
  }

  /**
   * Get the shared database with the specified name
   * @param name The name of the shared database
   * @return The database or null if nothing was found.
   */
  public DatabaseMeta getSharedDatabase(String name) {
    return (DatabaseMeta)getSharedObject(DatabaseMeta.class.getName(), name);
  }
}
