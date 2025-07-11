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
package org.operaton.bpm.engine.impl;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serializable;
import java.util.List;

import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.SuspensionState;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.JobDefinitionQuery;

/**
 * @author roman.smirnov
 */
public class JobDefinitionQueryImpl extends AbstractQuery<JobDefinitionQuery, JobDefinition> implements JobDefinitionQuery, Serializable {

  private static final long serialVersionUID = 1L;

  protected String id;
  protected String[] activityIds;
  protected String processDefinitionId;
  protected String processDefinitionKey;
  protected String jobType;
  protected String jobConfiguration;
  protected SuspensionState suspensionState;
  protected Boolean withOverridingJobPriority;

  protected boolean isTenantIdSet = false;
  protected String[] tenantIds;
  protected boolean includeJobDefinitionsWithoutTenantId = false;

  public JobDefinitionQueryImpl() {
  }

  public JobDefinitionQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public JobDefinitionQuery jobDefinitionId(String jobDefinitionId) {
    ensureNotNull("Job definition id", jobDefinitionId);
    this.id = jobDefinitionId;
    return this;
  }

  @Override
  public JobDefinitionQuery activityIdIn(String... activityIds) {
    ensureNotNull("Activity ids", (Object[]) activityIds);
    this.activityIds = activityIds;
    return this;
  }

  @Override
  public JobDefinitionQuery processDefinitionId(String processDefinitionId) {
    ensureNotNull("Process definition id", processDefinitionId);
    this.processDefinitionId = processDefinitionId;
    return this;
  }

  @Override
  public JobDefinitionQuery processDefinitionKey(String processDefinitionKey) {
    ensureNotNull("Process definition key", processDefinitionKey);
    this.processDefinitionKey = processDefinitionKey;
    return this;
  }

  @Override
  public JobDefinitionQuery jobType(String jobType) {
    ensureNotNull("Job type", jobType);
    this.jobType = jobType;
    return this;
  }

  @Override
  public JobDefinitionQuery jobConfiguration(String jobConfiguration) {
    ensureNotNull("Job configuration", jobConfiguration);
    this.jobConfiguration = jobConfiguration;
    return this;
  }

  @Override
  public JobDefinitionQuery active() {
    this.suspensionState = SuspensionState.ACTIVE;
    return this;
  }

  @Override
  public JobDefinitionQuery suspended() {
    this.suspensionState = SuspensionState.SUSPENDED;
    return this;
  }

  @Override
  public JobDefinitionQuery withOverridingJobPriority() {
    this.withOverridingJobPriority = true;
    return this;
  }

  @Override
  public JobDefinitionQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    this.tenantIds = tenantIds;
    isTenantIdSet = true;
    return this;
  }

  @Override
  public JobDefinitionQuery withoutTenantId() {
    isTenantIdSet = true;
    this.tenantIds = null;
    return this;
  }

  @Override
  public JobDefinitionQuery includeJobDefinitionsWithoutTenantId() {
    this.includeJobDefinitionsWithoutTenantId = true;
    return this;
  }

  // order by ///////////////////////////////////////////

  @Override
  public JobDefinitionQuery orderByJobDefinitionId() {
    return orderBy(JobDefinitionQueryProperty.JOB_DEFINITION_ID);
  }

  @Override
  public JobDefinitionQuery orderByActivityId() {
    return orderBy(JobDefinitionQueryProperty.ACTIVITY_ID);
  }

  @Override
  public JobDefinitionQuery orderByProcessDefinitionId() {
    return orderBy(JobDefinitionQueryProperty.PROCESS_DEFINITION_ID);
  }

  @Override
  public JobDefinitionQuery orderByProcessDefinitionKey() {
    return orderBy(JobDefinitionQueryProperty.PROCESS_DEFINITION_KEY);
  }

  @Override
  public JobDefinitionQuery orderByJobType() {
    return orderBy(JobDefinitionQueryProperty.JOB_TYPE);
  }

  @Override
  public JobDefinitionQuery orderByJobConfiguration() {
    return orderBy(JobDefinitionQueryProperty.JOB_CONFIGURATION);
  }

  @Override
  public JobDefinitionQuery orderByTenantId() {
    return orderBy(JobDefinitionQueryProperty.TENANT_ID);
  }

  // results ////////////////////////////////////////////

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return commandContext
      .getJobDefinitionManager()
      .findJobDefinitionCountByQueryCriteria(this);
  }

  @Override
  public List<JobDefinition> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    return commandContext
      .getJobDefinitionManager()
      .findJobDefinitionByQueryCriteria(this, page);
  }

  // getters /////////////////////////////////////////////

  public String getId() {
    return id;
  }

  public String[] getActivityIds() {
    return activityIds;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  public String getJobType() {
    return jobType;
  }

  public String getJobConfiguration() {
    return jobConfiguration;
  }

  public SuspensionState getSuspensionState() {
    return suspensionState;
  }

  public Boolean getWithOverridingJobPriority() {
    return withOverridingJobPriority;
  }

}
