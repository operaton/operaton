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
package org.operaton.bpm.engine.impl.history.event;

import java.io.Serial;
import java.util.Date;

import org.operaton.bpm.engine.history.UserOperationLogEntry;

/**
 * @author Danny Gräf
 */
public class UserOperationLogEntryEventEntity extends HistoryEvent implements UserOperationLogEntry {

  @Serial private static final long serialVersionUID = 1L;

  protected String operationId;
  protected String operationType;
  protected String jobId;
  protected String jobDefinitionId;
  protected String taskId;
  protected String userId;
  protected Date timestamp;
  protected String property;
  protected String orgValue;
  protected String newValue;
  protected String entityType;
  protected String deploymentId;
  protected String tenantId;
  protected String batchId;
  protected String category;
  protected String externalTaskId;
  protected String annotation;

  @Override
  public String getOperationId() {
    return operationId;
  }

  @Override
  public String getOperationType() {
    return operationType;
  }

  @Override
  public String getTaskId() {
    return taskId;
  }

  @Override
  public String getUserId() {
    return userId;
  }

  @Override
  public Date getTimestamp() {
    return timestamp;
  }

  @Override
  public String getProperty() {
    return property;
  }

  @Override
  public String getOrgValue() {
    return orgValue;
  }

  @Override
  public String getNewValue() {
    return newValue;
  }

  public void setOperationId(String operationId) {
    this.operationId = operationId;
  }

  public void setOperationType(String operationType) {
    this.operationType = operationType;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public void setOrgValue(String orgValue) {
    this.orgValue = orgValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  @Override
  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  @Override
  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  @Override
  public String getJobDefinitionId() {
    return jobDefinitionId;
  }

  public void setJobDefinitionId(String jobDefinitionId) {
    this.jobDefinitionId = jobDefinitionId;
  }

  @Override
  public String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public String getBatchId() {
    return batchId;
  }

  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }

  @Override
  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  @Override
  public String getRootProcessInstanceId() {
    return rootProcessInstanceId;
  }

  @Override
  public void setRootProcessInstanceId(String rootProcessInstanceId) {
    this.rootProcessInstanceId = rootProcessInstanceId;
  }

  @Override
  public String getExternalTaskId() {
    return externalTaskId;
  }

  public void setExternalTaskId(String externalTaskId) {
    this.externalTaskId = externalTaskId;
  }

  @Override
  public String getAnnotation() {
    return annotation;
  }

  public void setAnnotation(String annotation) {
    this.annotation = annotation;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
        + "[taskId=%s, deploymentId=%s, processDefinitionKey=%s, jobId=%s, jobDefinitionId=%s, batchId=%s, operationId=%s, operationType=".formatted(taskId, deploymentId, processDefinitionKey, jobId).formatted(jobDefinitionId, batchId, operationId) + operationType
        + ", userId=%s, timestamp=%s, property=%s, orgValue=%s, newValue=%s, id=%s, eventType=%s, executionId=".formatted(userId, timestamp, property, orgValue).formatted(newValue, id, eventType) + executionId
        + ", processDefinitionId=%s, rootProcessInstanceId=%s, processInstanceId=%s, externalTaskId=%s, tenantId=%s, entityType=%s, category=%s, annotation=".formatted(processDefinitionId, rootProcessInstanceId, processInstanceId, externalTaskId).formatted(tenantId, entityType, category) + annotation
        + "]";
  }
}
