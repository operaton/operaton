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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.SuspensionState;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.impl.util.CompareUtil;
import org.operaton.bpm.engine.impl.util.ImmutablePair;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotEmpty;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Joram Barrez
 * @author Tom Baeyens
 * @author Falko Menge
 */
public class JobQueryImpl extends AbstractQuery<JobQuery, Job> implements JobQuery, Serializable {

  @Serial private static final long serialVersionUID = 1L;
  protected String activityId;
  protected String id;
  private Set<String> ids;
  protected String jobDefinitionId;
  protected String rootProcessInstanceId;
  protected String processInstanceId;
  private Set<String> processInstanceIds;
  protected String executionId;
  protected String processDefinitionId;
  protected String processDefinitionKey;
  protected boolean retriesLeft;
  protected boolean executable;
  protected boolean onlyTimers;
  protected boolean onlyMessages;
  protected Date duedateHigherThan;
  protected Date duedateLowerThan;
  protected Date duedateHigherThanOrEqual;
  protected Date duedateLowerThanOrEqual;
  protected Date createdBefore;
  protected Date createdAfter;
  protected Long priorityHigherThanOrEqual;
  protected Long priorityLowerThanOrEqual;
  protected boolean withException;
  protected String exceptionMessage;
  protected String failedActivityId;
  protected boolean noRetriesLeft;
  protected SuspensionState suspensionState;
  protected boolean acquired;

  protected boolean isTenantIdSet;
  protected String[] tenantIds;
  protected boolean includeJobsWithoutTenantId;

  public JobQueryImpl() {
  }

  public JobQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public JobQuery jobId(String jobId) {
    ensureNotNull("Provided job id", jobId);
    this.id = jobId;
    return this;
  }

  @Override
  public JobQuery jobIds(Set<String> ids) {
    ensureNotEmpty("Set of job ids", ids);
    this.ids = ids;
    return this;
  }

  @Override
  public JobQuery jobDefinitionId(String jobDefinitionId) {
    ensureNotNull("Provided job definition id", jobDefinitionId);
    this.jobDefinitionId = jobDefinitionId;
    return this;
  }

  @Override
  public JobQueryImpl rootProcessInstanceId(String rootProcessInstanceId) {
    ensureNotNull("Provided root process instance id", rootProcessInstanceId);
    this.rootProcessInstanceId = rootProcessInstanceId;
    return this;
  }

  @Override
  public JobQueryImpl processInstanceId(String processInstanceId) {
    ensureNotNull("Provided process instance id", processInstanceId);
    this.processInstanceId = processInstanceId;
    return this;
  }

  @Override
  public JobQuery processInstanceIds(Set<String> processInstanceIds) {
    ensureNotEmpty("Set of process instance ids", processInstanceIds);
    this.processInstanceIds = processInstanceIds;
    return this;
  }

  @Override
  public JobQueryImpl executionId(String executionId) {
    ensureNotNull("Provided execution id", executionId);
    this.executionId = executionId;
    return this;
  }

  @Override
  public JobQuery processDefinitionId(String processDefinitionId) {
    ensureNotNull("Provided process definition id", processDefinitionId);
    this.processDefinitionId = processDefinitionId;
    return this;
  }

  @Override
  public JobQuery processDefinitionKey(String processDefinitionKey) {
    ensureNotNull("Provided process instance key", processDefinitionKey);
    this.processDefinitionKey = processDefinitionKey;
    return this;
  }

  @Override
  public JobQuery activityId(String activityId) {
    ensureNotNull("Provided activity id", activityId);
    this.activityId = activityId;
    return this;
  }

  @Override
  public JobQuery withRetriesLeft() {
    retriesLeft = true;
    return this;
  }

  @Override
  public JobQuery executable() {
    executable = true;
    return this;
  }

  @Override
  public JobQuery timers() {
    if (onlyMessages) {
      throw new ProcessEngineException("Cannot combine onlyTimers() with onlyMessages() in the same query");
    }
    this.onlyTimers = true;
    return this;
  }

  @Override
  public JobQuery messages() {
    if (onlyTimers) {
      throw new ProcessEngineException("Cannot combine onlyTimers() with onlyMessages() in the same query");
    }
    this.onlyMessages = true;
    return this;
  }

  @Override
  public JobQuery duedateHigherThan(Date date) {
    ensureNotNull("Provided date", date);
    this.duedateHigherThan = date;
    return this;
  }

  @Override
  public JobQuery duedateLowerThan(Date date) {
    ensureNotNull("Provided date", date);
    this.duedateLowerThan = date;
    return this;
  }

  @Override
  public JobQuery duedateHigherThen(Date date) {
    return duedateHigherThan(date);
  }

  @Override
  public JobQuery duedateHigherThenOrEquals(Date date) {
    ensureNotNull("Provided date", date);
    this.duedateHigherThanOrEqual = date;
    return this;
  }

  @Override
  public JobQuery duedateLowerThen(Date date) {
    return duedateLowerThan(date);
  }

  @Override
  public JobQuery duedateLowerThenOrEquals(Date date) {
    ensureNotNull("Provided date", date);
    this.duedateLowerThanOrEqual = date;
    return this;
  }

  @Override
  public JobQuery createdBefore(Date date) {
    ensureNotNull("Provided date", date);
    this.createdBefore = date;
    return this;
  }

  @Override
  public JobQuery createdAfter(Date date) {
    ensureNotNull("Provided date", date);
    this.createdAfter = date;
    return this;
  }

  @Override
  public JobQuery priorityHigherThanOrEquals(long priority) {
    this.priorityHigherThanOrEqual = priority;
    return this;
  }

  @Override
  public JobQuery priorityLowerThanOrEquals(long priority) {
    this.priorityLowerThanOrEqual = priority;
    return this;
  }

  @Override
  public JobQuery withException() {
    this.withException = true;
    return this;
  }

  @Override
  public JobQuery exceptionMessage(String exceptionMessage) {
    ensureNotNull("Provided exception message", exceptionMessage);
    this.exceptionMessage = exceptionMessage;
    return this;
  }

  @Override
  public JobQuery failedActivityId(String activityId) {
    ensureNotNull("Provided activity id", activityId);
    this.failedActivityId = activityId;
    return this;
  }

  @Override
  public JobQuery noRetriesLeft() {
    noRetriesLeft = true;
    return this;
  }

  @Override
  public JobQuery active() {
    suspensionState = SuspensionState.ACTIVE;
    return this;
  }

  @Override
  public JobQuery suspended() {
    suspensionState = SuspensionState.SUSPENDED;
    return this;
  }

  @Override
  public JobQuery acquired() {
    acquired = true;
    return this;
  }

  @Override
  protected boolean hasExcludingConditions() {
    return super.hasExcludingConditions()
      || CompareUtil.areNotInAscendingOrder(priorityHigherThanOrEqual, priorityLowerThanOrEqual)
      || hasExcludingDueDateParameters()
      || CompareUtil.areNotInAscendingOrder(createdAfter, createdBefore);
  }

  private boolean hasExcludingDueDateParameters() {
    List<Date> dueDates = new ArrayList<>();
    if (duedateHigherThan != null && duedateHigherThanOrEqual != null) {
      dueDates.add(CompareUtil.min(duedateHigherThan, duedateHigherThanOrEqual));
      dueDates.add(CompareUtil.max(duedateHigherThan, duedateHigherThanOrEqual));
    } else if (duedateHigherThan != null) {
      dueDates.add(duedateHigherThan);
    } else if (duedateHigherThanOrEqual != null) {
      dueDates.add(duedateHigherThanOrEqual);
    }

    if (duedateLowerThan != null && duedateLowerThanOrEqual != null) {
      dueDates.add(CompareUtil.min(duedateLowerThan, duedateLowerThanOrEqual));
      dueDates.add(CompareUtil.max(duedateLowerThan, duedateLowerThanOrEqual));
    } else if (duedateLowerThan != null) {
      dueDates.add(duedateLowerThan);
    } else if (duedateLowerThanOrEqual != null) {
      dueDates.add(duedateLowerThanOrEqual);
    }

    return CompareUtil.areNotInAscendingOrder(dueDates);
  }

  @Override
  public JobQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    this.tenantIds = tenantIds;
    isTenantIdSet = true;
    return this;
  }

  @Override
  public JobQuery withoutTenantId() {
    isTenantIdSet = true;
    this.tenantIds = null;
    return this;
  }

  @Override
  public JobQuery includeJobsWithoutTenantId() {
    this.includeJobsWithoutTenantId = true;
    return this;
  }

  //sorting //////////////////////////////////////////

  @Override
  public JobQuery orderByJobDuedate() {
    return orderBy(JobQueryProperty.DUEDATE);
  }

  @Override
  public JobQuery orderByExecutionId() {
    return orderBy(JobQueryProperty.EXECUTION_ID);
  }

  @Override
  public JobQuery orderByJobId() {
    return orderBy(JobQueryProperty.JOB_ID);
  }

  @Override
  public JobQuery orderByProcessInstanceId() {
    return orderBy(JobQueryProperty.PROCESS_INSTANCE_ID);
  }

  @Override
  public JobQuery orderByProcessDefinitionId() {
    return orderBy(JobQueryProperty.PROCESS_DEFINITION_ID);
  }

  @Override
  public JobQuery orderByProcessDefinitionKey() {
    return orderBy(JobQueryProperty.PROCESS_DEFINITION_KEY);
  }

  @Override
  public JobQuery orderByJobRetries() {
    return orderBy(JobQueryProperty.RETRIES);
  }

  @Override
  public JobQuery orderByJobPriority() {
    return orderBy(JobQueryProperty.PRIORITY);
  }

  @Override
  public JobQuery orderByTenantId() {
    return orderBy(JobQueryProperty.TENANT_ID);
  }

  //results //////////////////////////////////////////

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return commandContext
      .getJobManager()
      .findJobCountByQueryCriteria(this);
  }

  @Override
  public List<Job> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    return commandContext
      .getJobManager()
      .findJobsByQueryCriteria(this, page);
  }

  @Override
  public List<ImmutablePair<String, String>> executeDeploymentIdMappingsList(CommandContext commandContext) {
    checkQueryOk();
    return commandContext
      .getJobManager()
      .findDeploymentIdMappingsByQueryCriteria(this);
  }

  //getters //////////////////////////////////////////

  public Set<String> getIds() {
    return ids;
  }
  public String getRootProcessInstanceId() {
    return rootProcessInstanceId;
  }
  public String getProcessInstanceId() {
    return processInstanceId;
  }
  public Set<String> getProcessInstanceIds() {
    return processInstanceIds;
  }
  public String getExecutionId() {
    return executionId;
  }
  public boolean getRetriesLeft() {
    return retriesLeft;
  }
  public boolean getExecutable() {
    return executable;
  }
  public Date getNow() {
    return ClockUtil.getCurrentTime();
  }
  public boolean isWithException() {
    return withException;
  }
  public String getExceptionMessage() {
    return exceptionMessage;
  }
  public boolean getAcquired() {
    return acquired;
  }

}
