package org.pentaho.di.core.extension;

/**
 * Convenience class you can use to pass more than one object in an extension point
 *  
 * @author matt
 *
 */
public class ExtensionPointContext {
  private Object subject;
  private Object parent;
  private Object relation;
  public ExtensionPointContext(Object subject, Object parent, Object relation) {
    super();
    this.subject = subject;
    this.parent = parent;
    this.relation = relation;
  }
  public Object getSubject() {
    return subject;
  }
  public void setSubject(Object subject) {
    this.subject = subject;
  }
  public Object getParent() {
    return parent;
  }
  public void setParent(Object parent) {
    this.parent = parent;
  }
  public Object getRelation() {
    return relation;
  }
  public void setRelation(Object relation) {
    this.relation = relation;
  }
}
