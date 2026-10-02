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

import java.util.HashMap;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.HasDbReferences;
import org.operaton.bpm.engine.impl.db.HasDbRevision;
import org.operaton.bpm.engine.impl.jobexecutor.JobDeclaration;
import org.operaton.bpm.engine.management.JobDefinition;

/**
 *
 * @author Daniel Meyer
 *
 */
@NullMarked
public class JobDefinitionEntity implements JobDefinition, HasDbRevision, HasDbReferences, DbEntity {

  protected @Nullable String id;
  protected int revision;

  protected @Nullable String processDefinitionId;
  protected @Nullable String processDefinitionKey;

  /* Note: this is the id of the activity which is the cause that a Job is created.
   * If the Job corresponds to an event scope, it may or may not correspond to the
   * activity which defines the event scope.
   *
   * Example:
   * user task with attached timer event:
   * - timer event scope = user task
   * - activity which causes the job to be created = timer event.
   * => Job definition activityId will be activityId of the timer event, not the activityId of the user task.
   */
  protected @Nullable String activityId;

  /** timer, message, ... */
  protected @Nullable String jobType;
  protected @Nullable String jobConfiguration;

  // job definition is active by default
  protected int suspensionState = SuspensionState.ACTIVE.getStateCode();

  protected @Nullable Long jobPriority;

  protected @Nullable String tenantId;

  protected @Nullable String deploymentId;

  public JobDefinitionEntity() {
  }

  public JobDefinitionEntity(JobDeclaration<?, ?> jobDeclaration) {
    this.activityId = jobDeclaration.getActivityId();
    this.jobConfiguration = jobDeclaration.getJobConfiguration();
    this.jobType = jobDeclaration.getJobHandlerType();
  }

  @Override
  public Object getPersistentState() {
    HashMap<String, Object> state = new HashMap<>();
    state.put("processDefinitionId", processDefinitionId);
    state.put("processDefinitionKey", processDefinitionKey);
    state.put("activityId", activityId);
    state.put("jobType", jobType);
    state.put("jobConfiguration", jobConfiguration);
    state.put("suspensionState", suspensionState);
    state.put("jobPriority", jobPriority);
    state.put("tenantId", tenantId);
    state.put("deploymentId", deploymentId);
    return state;
  }

  // getters / setters /////////////////////////////////

  @Override
  public int getRevisionNext() {
    return revision + 1;
  }

  @Override
  public @Nullable String getId() {
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

  @Override
  public boolean isSuspended() {
    return SuspensionState.SUSPENDED.getStateCode() == suspensionState;
  }

  @Override
  public @Nullable String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public void setProcessDefinitionId(@Nullable String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  @Override
  public @Nullable String getActivityId() {
    return activityId;
  }

  public void setActivityId(@Nullable String activityId) {
    this.activityId = activityId;
  }

  @Override
  public @Nullable String getJobType() {
    return jobType;
  }

  public void setJobType(String jobType) {
    this.jobType = jobType;
  }

  @Override
  public @Nullable String getJobConfiguration() {
    return jobConfiguration;
  }

  public void setJobConfiguration(@Nullable String jobConfiguration) {
    this.jobConfiguration = jobConfiguration;
  }

  @Override
  public @Nullable String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  public void setProcessDefinitionKey(@Nullable String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  public int getSuspensionState() {
    return suspensionState;
  }

  public void setSuspensionState(int state) {
    this.suspensionState = state;
  }

  @Override
  public @Nullable Long getOverridingJobPriority() {
    return jobPriority;
  }

  public void setJobPriority(@Nullable Long jobPriority) {
    this.jobPriority = jobPriority;
  }

  @Override
  public @Nullable String getTenantId() {
    return tenantId;
  }

  public void setTenantId(@Nullable String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public @Nullable String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(@Nullable String deploymentId) {
    this.deploymentId = deploymentId;
  }
}
