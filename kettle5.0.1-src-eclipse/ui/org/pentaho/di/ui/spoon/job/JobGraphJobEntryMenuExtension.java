package org.pentaho.di.ui.spoon.job;

import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryCopy;
import org.pentaho.ui.xul.XulDomContainer;
import org.pentaho.ui.xul.dom.Document;

public class JobGraphJobEntryMenuExtension {
  
  public Document doc;
  public JobMeta jobMeta;
  public JobGraph jobGraph;
  public JobEntryCopy jobEntry;
  public XulDomContainer xulDomContainer;
  
  public JobGraphJobEntryMenuExtension(XulDomContainer xulDomContainer, Document doc, JobMeta jobMeta, JobEntryCopy jobEntry, JobGraph jobGraph) {
    this.xulDomContainer = xulDomContainer;
    this.doc = doc;
    this.jobMeta = jobMeta;
    this.jobEntry = jobEntry;
    this.jobGraph = jobGraph;
  }

}
