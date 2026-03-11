/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.persistence.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.HasDbRevision;

public class AcquirableJobEntity implements DbEntity, HasDbRevision {

  public static final boolean DEFAULT_EXCLUSIVE = true;

  protected String id;
  protected int revision;

  protected String lockOwner;
  protected Date lockExpirationTime;
  protected Date duedate;

  protected String rootProcessInstanceId;
  protected String processInstanceId;

  protected boolean isExclusive = DEFAULT_EXCLUSIVE;


  @Override
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<>();
    persistentState.put("lockOwner", lockOwner);
    persistentState.put("lockExpirationTime", lockExpirationTime);
    persistentState.put("duedate", duedate);
    return persistentState;
  }

  @Override
  public int getRevisionNext() {
    return revision + 1;
  }

  // getters and setters //////////////////////////////////////////////////////

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public int getRevision() {
    return revision;
  }

  @Override
  public void setRevision(int revision) {
    this.revision = revision;
  }

  public Date getDuedate() {
    return duedate;
  }

  public void setDuedate(Date duedate) {
    this.duedate = duedate;
  }

  public String getLockOwner() {
    return lockOwner;
  }

  public void setLockOwner(String lockOwner) {
    this.lockOwner = lockOwner;
  }

  public Date getLockExpirationTime() {
    return lockExpirationTime;
  }

  public void setLockExpirationTime(Date lockExpirationTime) {
    this.lockExpirationTime = lockExpirationTime;
  }

  public String getRootProcessInstanceId() {
    return rootProcessInstanceId;
  }

  public void setRootProcessInstanceId(String rootProcessInstanceId) {
    this.rootProcessInstanceId = rootProcessInstanceId;
  }

  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public boolean isExclusive() {
    return isExclusive;
  }

  public void setExclusive(boolean isExclusive) {
    this.isExclusive = isExclusive;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    return prime * result + (id == null ? 0 : id.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AcquirableJobEntity other = (AcquirableJobEntity) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
        + "[id=" + id
        + ", revision=" + revision
        + ", lockOwner=" + lockOwner
        + ", lockExpirationTime=" + lockExpirationTime
        + ", duedate=" + duedate
        + ", rootProcessInstanceId=" + rootProcessInstanceId
        + ", processInstanceId=" + processInstanceId
        + ", isExclusive=" + isExclusive
        + "]";
  }

}
