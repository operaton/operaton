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

import java.io.Serial;
import java.util.List;

import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.history.HistoricJobLogQuery;
import org.operaton.bpm.engine.history.JobState;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.util.CompareUtil;
import org.operaton.commons.utils.CollectionUtil;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotContainsEmptyString;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotContainsNull;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotEmpty;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Roman Smirnov
 *
 */
public class HistoricJobLogQueryImpl extends AbstractQuery<HistoricJobLogQuery, HistoricJobLog> implements HistoricJobLogQuery {

  @Serial private static final long serialVersionUID = 1L;

  protected String id;
  protected String jobId;
  protected String jobExceptionMessage;
  protected String jobDefinitionId;
  protected String jobDefinitionType;
  protected String jobDefinitionConfiguration;
  protected String[] activityIds;
  protected String[] failedActivityIds;
  protected String[] executionIds;
  protected String processInstanceId;
  protected String processDefinitionId;
  protected String processDefinitionKey;
  protected String deploymentId;
  protected JobState state;
  protected Long jobPriorityHigherThanOrEqual;
  protected Long jobPriorityLowerThanOrEqual;
  protected String[] tenantIds;
  protected boolean isTenantIdSet;
  protected String hostname;

  public HistoricJobLogQueryImpl() {
  }

  public HistoricJobLogQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  // query parameter ////////////////////////////////////////////

  @Override
  public HistoricJobLogQuery logId(String historicJobLogId) {
    ensureNotNull(NotValidException.class, "historicJobLogId", historicJobLogId);
    this.id = historicJobLogId;
    return this;
  }

  @Override
  public HistoricJobLogQuery jobId(String jobId) {
    ensureNotNull(NotValidException.class, "jobId", jobId);
    this.jobId = jobId;
    return this;
  }

  @Override
  public HistoricJobLogQuery jobExceptionMessage(String jobExceptionMessage) {
    ensureNotNull(NotValidException.class, "jobExceptionMessage", jobExceptionMessage);
    this.jobExceptionMessage = jobExceptionMessage;
    return this;
  }

  @Override
  public HistoricJobLogQuery jobDefinitionId(String jobDefinitionId) {
    ensureNotNull(NotValidException.class, "jobDefinitionId", jobDefinitionId);
    this.jobDefinitionId = jobDefinitionId;
    return this;
  }

  @Override
  public HistoricJobLogQuery jobDefinitionType(String jobDefinitionType) {
    ensureNotNull(NotValidException.class, "jobDefinitionType", jobDefinitionType);
    this.jobDefinitionType = jobDefinitionType;
    return this;
  }

  @Override
  public HistoricJobLogQuery jobDefinitionConfiguration(String jobDefinitionConfiguration) {
    ensureNotNull(NotValidException.class, "jobDefinitionConfiguration", jobDefinitionConfiguration);
    this.jobDefinitionConfiguration = jobDefinitionConfiguration;
    return this;
  }

  @Override
  @SuppressWarnings("java:S1192")
  public HistoricJobLogQuery activityIdIn(String... activityIds) {
    ensureNotNull(NotValidException.class, "activityIds", activityIds);
    List<String> activityIdList = CollectionUtil.asArrayList(activityIds);
    ensureNotContainsNull("activityIds", activityIdList);
    ensureNotContainsEmptyString("activityIds", activityIdList);
    this.activityIds = activityIds;
    return this;
  }

  @Override
  @SuppressWarnings("java:S1192")
  public HistoricJobLogQuery failedActivityIdIn(String... activityIds) {
    ensureNotNull(NotValidException.class, "activityIds", activityIds);
    List<String> activityIdList = CollectionUtil.asArrayList(activityIds);
    ensureNotContainsNull("activityIds", activityIdList);
    ensureNotContainsEmptyString("activityIds", activityIdList);
    this.failedActivityIds = activityIds;
    return this;
  }

  @Override
  @SuppressWarnings("java:S1192")
  public HistoricJobLogQuery executionIdIn(String... executionIds) {
    ensureNotNull(NotValidException.class, "executionIds", executionIds);
    List<String> executionIdList = CollectionUtil.asArrayList(executionIds);
    ensureNotContainsNull("executionIds", executionIdList);
    ensureNotContainsEmptyString("executionIds", executionIdList);
    this.executionIds = executionIds;
    return this;
  }

  @Override
  public HistoricJobLogQuery processInstanceId(String processInstanceId) {
    ensureNotNull(NotValidException.class, "processInstanceId", processInstanceId);
    this.processInstanceId = processInstanceId;
    return this;
  }

  @Override
  public HistoricJobLogQuery processDefinitionId(String processDefinitionId) {
    ensureNotNull(NotValidException.class, "processDefinitionId", processDefinitionId);
    this.processDefinitionId = processDefinitionId;
    return this;
  }

  @Override
  public HistoricJobLogQuery processDefinitionKey(String processDefinitionKey) {
    ensureNotNull(NotValidException.class, "processDefinitionKey", processDefinitionKey);
    this.processDefinitionKey = processDefinitionKey;
    return this;
  }

  @Override
  public HistoricJobLogQuery deploymentId(String deploymentId) {
    ensureNotNull(NotValidException.class, "deploymentId", deploymentId);
    this.deploymentId = deploymentId;
    return this;
  }

  @Override
  public HistoricJobLogQuery jobPriorityHigherThanOrEquals(long priority) {
    this.jobPriorityHigherThanOrEqual = priority;
    return this;
  }

  @Override
  public HistoricJobLogQuery jobPriorityLowerThanOrEquals(long priority) {
    this.jobPriorityLowerThanOrEqual = priority;
    return this;
  }

  @Override
  public HistoricJobLogQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    this.tenantIds = tenantIds;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  public HistoricJobLogQuery withoutTenantId() {
    this.tenantIds = null;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  public HistoricJobLogQuery hostname(String hostname) {
    ensureNotEmpty("hostName", hostname);
    this.hostname = hostname;
    return this;
  }

  @Override
  public HistoricJobLogQuery creationLog() {
    setState(JobState.CREATED);
    return this;
  }

  @Override
  public HistoricJobLogQuery failureLog() {
    setState(JobState.FAILED);
    return this;
  }

  @Override
  public HistoricJobLogQuery successLog() {
    setState(JobState.SUCCESSFUL);
    return this;
  }

  @Override
  public HistoricJobLogQuery deletionLog() {
    setState(JobState.DELETED);
    return this;
  }

  @Override
  protected boolean hasExcludingConditions() {
    return super.hasExcludingConditions()
      || CompareUtil.areNotInAscendingOrder(jobPriorityHigherThanOrEqual, jobPriorityLowerThanOrEqual);
  }

  // order by //////////////////////////////////////////////

  @Override
  public HistoricJobLogQuery orderByTimestamp() {
    orderBy(HistoricJobLogQueryProperty.TIMESTAMP);
    return this;
  }

  @Override
  public HistoricJobLogQuery orderByJobId() {
    orderBy(HistoricJobLogQueryProperty.JOB_ID);
    return this;
  }

  @Override
  public HistoricJobLogQuery orderByJobDueDate() {
    orderBy(HistoricJobLogQueryProperty.DUEDATE);
    return this;
  }

  @Override
  public HistoricJobLogQuery orderByJobRetries() {
    orderBy(HistoricJobLogQueryProperty.RETRIES);
    return this;
  }

  @Override
  public HistoricJobLogQuery orderByJobPriority() {
    orderBy(HistoricJobLogQueryProperty.PRIORITY);
    return this;
  }

  @Override
  public HistoricJobLogQuery orderByJobDefinitionId() {
    orderBy(HistoricJobLogQueryProperty.JOB_DEFINITION_ID);
    return this;
  }

  @Override
  public HistoricJobLogQuery orderByActivityId() {
    orderBy(HistoricJobLogQueryProperty.ACTIVITY_ID);
    return this;
  }

  @Override
  public HistoricJobLogQuery orderByExecutionId() {
    orderBy(HistoricJobLogQueryProperty.EXECUTION_ID);
    return this;
  }

  @Override
  public HistoricJobLogQuery orderByProcessInstanceId() {
    orderBy(HistoricJobLogQueryProperty.PROCESS_INSTANCE_ID);
    return this;
  }

  @Override
  public HistoricJobLogQuery orderByProcessDefinitionId() {
    orderBy(HistoricJobLogQueryProperty.PROCESS_DEFINITION_ID);
    return this;
  }

  @Override
  public HistoricJobLogQuery orderByProcessDefinitionKey() {
    orderBy(HistoricJobLogQueryProperty.PROCESS_DEFINITION_KEY);
    return this;
  }

  @Override
  public HistoricJobLogQuery orderByDeploymentId() {
    orderBy(HistoricJobLogQueryProperty.DEPLOYMENT_ID);
    return this;
  }

  @Override
  public HistoricJobLogQuery orderPartiallyByOccurrence() {
    orderBy(HistoricJobLogQueryProperty.SEQUENCE_COUNTER);
    return this;
  }

  @Override
  public HistoricJobLogQuery orderByTenantId() {
    return orderBy(HistoricJobLogQueryProperty.TENANT_ID);
  }

  @Override
  public HistoricJobLogQuery orderByHostname() {
    return orderBy(HistoricJobLogQueryProperty.HOSTNAME);
  }

  // results //////////////////////////////////////////////////////////////

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return commandContext
      .getHistoricJobLogManager()
      .findHistoricJobLogsCountByQueryCriteria(this);
  }

  @Override
  public List<HistoricJobLog> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    return commandContext
        .getHistoricJobLogManager()
        .findHistoricJobLogsByQueryCriteria(this, page);
  }

  // getter //////////////////////////////////

  public boolean isTenantIdSet() {
    return isTenantIdSet;
  }

  public String getJobId() {
    return jobId;
  }

  public String getJobExceptionMessage() {
    return jobExceptionMessage;
  }

  public String getJobDefinitionId() {
    return jobDefinitionId;
  }

  public String getJobDefinitionType() {
    return jobDefinitionType;
  }

  public String getJobDefinitionConfiguration() {
    return jobDefinitionConfiguration;
  }

  public String[] getActivityIds() {
    return activityIds;
  }

  public String[] getFailedActivityIds() {
    return failedActivityIds;
  }

  public String[] getExecutionIds() {
    return executionIds;
  }

  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  public String getDeploymentId() {
    return deploymentId;
  }

  public JobState getState() {
    return state;
  }

  public String[] getTenantIds() {
    return tenantIds;
  }

  public String getHostname() {
    return hostname;
  }

  // setter //////////////////////////////////

  protected void setState(JobState state) {
    this.state = state;
  }

}
