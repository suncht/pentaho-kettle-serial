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

package org.pentaho.di.core.gui;

import java.util.LinkedList;
import java.util.List;

import org.pentaho.di.core.Const;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.job.JobEntryResult;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.job.entry.JobEntryCopy;

/**
 * Responsible for tracking the execution of a job as a hierarchy.
 * 
 * @author Matt
 * @since 30-mar-2006
 * 
 */
public class JobTracker {
  /** The trackers for each individual job entry */
  private List<JobTracker> jobTrackers;

  /** If the jobTrackers list is empty, then this is the result */
  private JobEntryResult   result;

  /** The parent job tracker, null if this is the root */
  private JobTracker       parentJobTracker;

  private String           jobName;

  private String           jobFilename;

  private int              maxChildren;

  /**
  * @param jobMeta the job metadata to keep track of (with maximum 5000 children)
  */
  public JobTracker(JobMeta jobMeta) {
    if (jobMeta != null) {
      this.jobName = jobMeta.getName();
      this.jobFilename = jobMeta.getFilename();
    }

    jobTrackers = new LinkedList<JobTracker>();
    maxChildren = Const.toInt(EnvUtil.getSystemProperty(Const.KETTLE_MAX_JOB_TRACKER_SIZE), 5000);
  }

  /**
   * @param jobMeta The job metadata to track
   * @param maxChildren The maximum number of children to keep track of (1000 is the default)
   */
  public JobTracker(JobMeta jobMeta, int maxChildren) {
    if (jobMeta != null) {
      this.jobName = jobMeta.getName();
      this.jobFilename = jobMeta.getFilename();
    }

    jobTrackers = new LinkedList<JobTracker>();
    this.maxChildren = maxChildren;
  }

  /**
   * Creates a jobtracker with a single result (maxChildren children are kept)
   * 
   * @param jobMeta the job metadata to keep track of
   * @param result the job entry result to track.
   */
  public JobTracker(JobMeta jobMeta, JobEntryResult result) {
    this(jobMeta);
    this.result = result;
  }

  /**
   * Creates a jobtracker with a single result
   * 
   * @param jobMeta the job metadata to keep track of
   * @param maxChildren The maximum number of children to keep track of
   * @param result the job entry result to track.
   */
  public JobTracker(JobMeta jobMeta, int maxChildren, JobEntryResult result) {
    this(jobMeta, maxChildren);
    this.result = result;
  }

  public void addJobTracker(JobTracker jobTracker) {
    synchronized(this) {
      jobTrackers.add(jobTracker);
      if (jobTrackers.size()>maxChildren+50) {
        jobTrackers = jobTrackers.subList(50, jobTrackers.size());
      }
    }
  }

  public JobTracker getJobTracker(int i) {
    return jobTrackers.get(i);
  }

  public int nrJobTrackers() {
    return jobTrackers.size();
  }

  /**
   * @return Returns the jobTrackers.
   */
  public List<JobTracker> getJobTrackers() {
    return jobTrackers;
  }

  /**
   * @param jobTrackers
   *          The jobTrackers to set.
   */
  public void setJobTrackers(List<JobTracker> jobTrackers) {
    this.jobTrackers = jobTrackers;
  }

  /**
   * @return Returns the result.
   */
  public JobEntryResult getJobEntryResult() {
    return result;
  }

  /**
   * @param result
   *          The result to set.
   */
  public void setJobEntryResult(JobEntryResult result) {
    this.result = result;
  }

  public void clear() {
    jobTrackers.clear();
    result = null;
  }

  /**
   * Finds the JobTracker for the job entry specified. Use this to
   * 
   * @param jobEntryCopy
   *          The entry to search the job tracker for
   * @return The JobTracker of null if none could be found...
   */
  public JobTracker findJobTracker(JobEntryCopy jobEntryCopy) {
    for (int i = jobTrackers.size() - 1; i >= 0; i--) {
      JobTracker tracker = getJobTracker(i);
      JobEntryResult result = tracker.getJobEntryResult();
      if (result != null) {
        if (jobEntryCopy.getName() != null && jobEntryCopy.getName().equals(result.getJobEntryName()) && jobEntryCopy.getNr() == result.getJobEntryNr()) {
          return tracker;
        }
      }
    }
    return null;
  }

  /**
   * @return Returns the parentJobTracker.
   */
  public JobTracker getParentJobTracker() {
    return parentJobTracker;
  }

  /**
   * @param parentJobTracker
   *          The parentJobTracker to set.
   */
  public void setParentJobTracker(JobTracker parentJobTracker) {
    this.parentJobTracker = parentJobTracker;
  }

  public int getTotalNumberOfItems() {
    int total = 1; // 1 = this one

    for (int i = 0; i < nrJobTrackers(); i++) {
      total += getJobTracker(i).getTotalNumberOfItems();
    }

    return total;
  }

  /**
   * @return the jobFilename
   */
  public String getJobFilename() {
    return jobFilename;
  }

  /**
   * @param jobFilename
   *          the jobFilename to set
   */
  public void setJobFilename(String jobFilename) {
    this.jobFilename = jobFilename;
  }

  /**
   * @return the jobName
   */
  public String getJobName() {
    return jobName;
  }

  /**
   * @param jobName
   *          the jobName to set
   */
  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  /**
   * @return the maxChildren
   */
  public int getMaxChildren() {
    return maxChildren;
  }

  /**
   * @param maxChildren
   *          the maxChildren to set
   */
  public void setMaxChildren(int maxChildren) {
    this.maxChildren = maxChildren;
  }
}
