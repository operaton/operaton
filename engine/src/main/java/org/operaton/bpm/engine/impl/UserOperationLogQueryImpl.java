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
import java.util.Date;
import java.util.List;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.util.CompareUtil;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Danny Gräf
 */
public class UserOperationLogQueryImpl extends AbstractQuery<UserOperationLogQuery, UserOperationLogEntry> implements UserOperationLogQuery {

  @Serial private static final long serialVersionUID = 1L;
  protected String deploymentId;
  protected String processDefinitionId;
  protected String processDefinitionKey;
  protected String processInstanceId;
  protected String executionId;
  protected String caseDefinitionId;
  protected String caseInstanceId;
  protected String caseExecutionId;
  protected String taskId;
  protected String jobId;
  protected String jobDefinitionId;
  protected String batchId;
  protected String userId;
  protected String operationId;
  protected String externalTaskId;
  protected String operationType;
  protected String property;
  protected String entityType;
  protected String category;
  protected Date timestampAfter;
  protected Date timestampBefore;
  protected String[] tenantIds;
  protected boolean isTenantIdSet;

  protected String[] entityTypes;
  protected String[] categories;

  public UserOperationLogQueryImpl() {
  }

  public UserOperationLogQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public UserOperationLogQuery deploymentId(String deploymentId) {
    ensureNotNull("deploymentId", deploymentId);
    this.deploymentId = deploymentId;
    return this;
  }

  @Override
  public UserOperationLogQuery processDefinitionId(String processDefinitionId) {
    ensureNotNull("processDefinitionId", processDefinitionId);
    this.processDefinitionId = processDefinitionId;
    return this;
  }

  @Override
  public UserOperationLogQuery processDefinitionKey(String processDefinitionKey) {
    ensureNotNull("processDefinitionKey", processDefinitionKey);
    this.processDefinitionKey = processDefinitionKey;
    return this;
  }

  @Override
  public UserOperationLogQuery processInstanceId(String processInstanceId) {
    ensureNotNull("processInstanceId", processInstanceId);
    this.processInstanceId = processInstanceId;
    return this;
  }

  @Override
  public UserOperationLogQuery executionId(String executionId) {
    ensureNotNull("executionId", executionId);
    this.executionId = executionId;
    return this;
  }

  @Override
  public UserOperationLogQuery caseDefinitionId(String caseDefinitionId) {
    ensureNotNull("caseDefinitionId", caseDefinitionId);
    this.caseDefinitionId = caseDefinitionId;
    return this;
  }

  @Override
  public UserOperationLogQuery caseInstanceId(String caseInstanceId) {
    ensureNotNull("caseInstanceId", caseInstanceId);
    this.caseInstanceId = caseInstanceId;
    return this;
  }

  @Override
  public UserOperationLogQuery caseExecutionId(String caseExecutionId) {
    ensureNotNull("caseExecutionId", caseExecutionId);
    this.caseExecutionId = caseExecutionId;
    return this;
  }


  @Override
  public UserOperationLogQuery taskId(String taskId) {
    ensureNotNull("taskId", taskId);
    this.taskId = taskId;
    return this;
  }

  @Override
  public UserOperationLogQuery jobId(String jobId) {
    ensureNotNull("jobId", jobId);
    this.jobId = jobId;
    return this;
  }

  @Override
  public UserOperationLogQuery jobDefinitionId(String jobDefinitionId) {
    ensureNotNull("jobDefinitionId", jobDefinitionId);
    this.jobDefinitionId = jobDefinitionId;
    return this;
  }

  @Override
  public UserOperationLogQuery batchId(String batchId) {
    ensureNotNull("batchId", batchId);
    this.batchId = batchId;
    return this;
  }

  @Override
  public UserOperationLogQuery userId(String userId) {
    ensureNotNull("userId", userId);
    this.userId = userId;
    return this;
  }

  @Override
  public UserOperationLogQuery operationId(String operationId) {
    ensureNotNull("operationId", operationId);
    this.operationId = operationId;
    return this;
  }

  @Override
  public UserOperationLogQuery externalTaskId(String externalTaskId) {
    ensureNotNull("externalTaskId", externalTaskId);
    this.externalTaskId = externalTaskId;
    return this;
  }

  @Override
  public UserOperationLogQuery operationType(String operationType) {
    ensureNotNull("operationType", operationType);
    this.operationType = operationType;
    return this;
  }

  @Override
  public UserOperationLogQuery property(String property) {
    ensureNotNull("property", property);
    this.property = property;
    return this;
  }

  @Override
  public UserOperationLogQuery entityType(String entityType) {
    ensureNotNull("entityType", entityType);
    this.entityType = entityType;
    return this;
  }

  @Override
  public UserOperationLogQuery entityTypeIn(String... entityTypes) {
    ensureNotNull("entity types", (Object[]) entityTypes);
    this.entityTypes = entityTypes;
    return this;
  }

  @Override
  public UserOperationLogQuery category(String category) {
    ensureNotNull("category", category);
    this.category = category;
    return this;
  }

  @Override
  public UserOperationLogQuery categoryIn(String... categories) {
    ensureNotNull("categories", (Object[]) categories);
    this.categories = categories;
    return this;
  }

  @Override
  public UserOperationLogQuery afterTimestamp(Date after) {
    this.timestampAfter = after;
    return this;
  }

  @Override
  public UserOperationLogQuery beforeTimestamp(Date before) {
    this.timestampBefore = before;
    return this;
  }

  @Override
  public UserOperationLogQuery orderByTimestamp() {
    return orderBy(OperationLogQueryProperty.TIMESTAMP);
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return commandContext
      .getOperationLogManager()
      .findOperationLogEntryCountByQueryCriteria(this);
  }

  @Override
  public List<UserOperationLogEntry> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    return commandContext
        .getOperationLogManager()
        .findOperationLogEntriesByQueryCriteria(this, page);
  }

  public boolean isTenantIdSet() {
    return isTenantIdSet;
  }

  @Override
  public UserOperationLogQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    this.tenantIds = tenantIds;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  public UserOperationLogQuery withoutTenantId() {
    this.tenantIds = null;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  protected boolean hasExcludingConditions() {
    return super.hasExcludingConditions() || CompareUtil.areNotInAscendingOrder(timestampAfter, timestampBefore);
  }
}
