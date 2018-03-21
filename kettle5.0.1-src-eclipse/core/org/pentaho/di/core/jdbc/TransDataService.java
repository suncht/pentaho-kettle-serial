package org.pentaho.di.core.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.pentaho.di.core.jdbc.FieldVariableMapping.MappingType;
import org.pentaho.di.core.sql.ServiceCacheMethod;
import org.pentaho.di.core.xml.XMLHandler;
import org.pentaho.di.core.xml.XMLInterface;
import org.pentaho.di.repository.ObjectId;
import org.w3c.dom.Node;

/**
 * This class describes a transformation data service containing a name, a description as well as a transformation to give the data, the step from which to read.
 * Later we will also add additional information about this service like the database cache location for this service, retention period and so on.
 *  
 * @author matt
 *
 */
public class TransDataService implements XMLInterface {

  public static final String XML_TAG_VARIABLE_MAPS = "variable-maps";
  public static final String XML_TAG_VARIABLE_MAP = "variable-map";

  private String name;
  
  private String fileName;
  
  private ObjectId objectId;
  
  private String serviceStepName;
  
  private List<FieldVariableMapping> fieldVariableMappings;
  
  private boolean dual;
  
  private ServiceCacheMethod cacheMethod;

  public TransDataService() {
    this((String)null);
  }
  
  /**
   * @param name
   */
  public TransDataService(String name) {
    this(name, null, null, null, null, ServiceCacheMethod.None);
  }


  public TransDataService(Node serviceNode) {
    this(
        XMLHandler.getTagValue(serviceNode, "name"),
        XMLHandler.getTagValue(serviceNode, "filename"),
        null,
        XMLHandler.getTagValue(serviceNode, "service_step"),
        extractFieldVariableMapping(serviceNode),
        ServiceCacheMethod.None
       );
  }

  /**
   * @param name
   * @param fileName
   * @param repositoryName
   * @param repositoryId
   * @param serviceStepName
   */
  public TransDataService(String name, String fileName, ObjectId objectId, String serviceStepName) {
    this(name, fileName, objectId, serviceStepName, new ArrayList<FieldVariableMapping>(), ServiceCacheMethod.None);
  }
  
  /**
   * @param name
   * @param fileName
   * @param repositoryName
   * @param repositoryId
   * @param serviceStepName
   */
  public TransDataService(String name, String fileName, ObjectId objectId, String serviceStepName, List<FieldVariableMapping> fieldVariableMappings, ServiceCacheMethod cacheMethod) {
    this.name = name;
    this.fileName = fileName;
    this.objectId = objectId;
    this.serviceStepName = serviceStepName;
    this.fieldVariableMappings = fieldVariableMappings;
    this.cacheMethod = cacheMethod;
  }

  private static List<FieldVariableMapping> extractFieldVariableMapping(Node serviceNode) {
    List<FieldVariableMapping> map = new ArrayList<FieldVariableMapping>();
    
    List<Node> nodes = XMLHandler.getNodes(XMLHandler.getSubNode(serviceNode, XML_TAG_VARIABLE_MAPS), XML_TAG_VARIABLE_MAP);
    for (Node node : nodes) {
      String field = XMLHandler.getTagValue(node, "field");
      String target = XMLHandler.getTagValue(node, "target");
      String variable = XMLHandler.getTagValue(node, "variable");
      MappingType mappingType = FieldVariableMapping.MappingType.getMappingType( XMLHandler.getTagValue(node, "type") );
      map.add( new FieldVariableMapping(field, target, variable, mappingType) );
    }
    
    return map;
  }

  @Override
  public String getXML() {
    StringBuilder xml = new StringBuilder();
    xml.append(XMLHandler.addTagValue("name", name));
    xml.append(XMLHandler.addTagValue("filename", fileName));
    xml.append(XMLHandler.addTagValue("service_step", serviceStepName));
    xml.append(XMLHandler.openTag(XML_TAG_VARIABLE_MAPS));
    List<FieldVariableMapping> list = new ArrayList<FieldVariableMapping>(fieldVariableMappings);
    Collections.sort(list, new Comparator<FieldVariableMapping>() {
        @Override
        public int compare(FieldVariableMapping o1, FieldVariableMapping o2) {
          return o1.getFieldName().compareTo(o2.getFieldName());
        }
      });
    
    for (FieldVariableMapping mapping : list) {
      xml.append(XMLHandler.openTag(XML_TAG_VARIABLE_MAP));
      xml.append(XMLHandler.addTagValue("field", mapping.getFieldName()));
      xml.append(XMLHandler.addTagValue("variable", mapping.getVariableName()));
      xml.append(XMLHandler.addTagValue("type", mapping.getMappingType().name()));
      xml.append(XMLHandler.closeTag(XML_TAG_VARIABLE_MAP));
    }
    xml.append(XMLHandler.closeTag(XML_TAG_VARIABLE_MAPS));
    return xml.toString();
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @return the fileName
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * @param fileName the fileName to set
   */
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  /**
   * @return the serviceStepName
   */
  public String getServiceStepName() {
    return serviceStepName;
  }

  /**
   * @param serviceStepName the serviceStepName to set
   */
  public void setServiceStepName(String serviceStepName) {
    this.serviceStepName = serviceStepName;
  }

  /**
   * @return the dual
   */
  public boolean isDual() {
    return dual;
  }

  /**
   * @param dual the dual to set
   */
  public void setDual(boolean dual) {
    this.dual = dual;
  }

  /**
   * @return the fieldVariableMappings
   */
  public List<FieldVariableMapping> getFieldVariableMappings() {
    return fieldVariableMappings;
  }

  /**
   * @param fieldVariableMappings the fieldVariableMappings to set
   */
  public void setFieldVariableMappings(List<FieldVariableMapping> fieldVariableMappings) {
    this.fieldVariableMappings = fieldVariableMappings;
  }

  /**
   * @return the objectId
   */
  public ObjectId getObjectId() {
    return objectId;
  }

  /**
   * @param objectId the objectId to set
   */
  public void setObjectId(ObjectId objectId) {
    this.objectId = objectId;
  }

  /**
   * @return the cacheMethod
   */
  public ServiceCacheMethod getCacheMethod() {
    return cacheMethod;
  }

  /**
   * @param cacheMethod the cacheMethod to set
   */
  public void setCacheMethod(ServiceCacheMethod cacheMethod) {
    this.cacheMethod = cacheMethod;
  }
}
