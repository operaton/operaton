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

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import org.operaton.bpm.engine.history.JobState;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.persistence.entity.ByteArrayEntity;
import org.operaton.bpm.engine.impl.util.ExceptionUtil;
import org.operaton.bpm.engine.impl.util.StringUtil;

/**
 * @author Roman Smirnov
 *
 */
public @NullMarked class HistoricJobLogEvent extends HistoryEvent {

  @Serial private static final long serialVersionUID = 1L;

  protected @Nullable Date timestamp;

  protected @Nullable String jobId;

  protected @Nullable Date jobDueDate;

  protected int jobRetries;

  protected long jobPriority;

  protected @Nullable String jobExceptionMessage;

  protected @Nullable String exceptionByteArrayId;

  protected @Nullable String jobDefinitionId;

  protected @Nullable String jobDefinitionType;

  protected @Nullable String jobDefinitionConfiguration;

  protected @Nullable String activityId;

  protected @Nullable String failedActivityId;

  protected @Nullable String deploymentId;

  protected int state;

  protected @Nullable String tenantId;

  protected @Nullable String hostname;

  protected @Nullable String batchId;

  public @Nullable Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public @Nullable String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public @Nullable Date getJobDueDate() {
    return jobDueDate;
  }

  public void setJobDueDate(@Nullable Date jobDueDate) {
    this.jobDueDate = jobDueDate;
  }

  public int getJobRetries() {
    return jobRetries;
  }

  public void setJobRetries(int jobRetries) {
    this.jobRetries = jobRetries;
  }

  public long getJobPriority() {
    return jobPriority;
  }

  public void setJobPriority(long jobPriority) {
    this.jobPriority = jobPriority;
  }

  public @Nullable String getJobExceptionMessage() {
    return jobExceptionMessage;
  }

  public void setJobExceptionMessage(@Nullable String jobExceptionMessage) {
    // note: it is not a clean way to truncate where the history event is produced, since truncation is only
    //   relevant for relational history databases that follow our schema restrictions;
    //   a similar problem exists in JobEntity#setExceptionMessage where truncation may not be required for custom
    //   persistence implementations
    this.jobExceptionMessage = StringUtil.trimToMaximumLengthAllowed(jobExceptionMessage);
  }

  public @Nullable String getExceptionByteArrayId() {
    return exceptionByteArrayId;
  }

  public void setExceptionByteArrayId(@Nullable String exceptionByteArrayId) {
    this.exceptionByteArrayId = exceptionByteArrayId;
  }

  public @Nullable String getExceptionStacktrace() {
    ByteArrayEntity byteArray = getExceptionByteArray();
    return ExceptionUtil.getExceptionStacktrace(byteArray);
  }

  protected @Nullable ByteArrayEntity getExceptionByteArray() {
    if (exceptionByteArrayId != null) {
      return Context
        .getCommandContext()
        .getDbEntityManager()
        .selectById(ByteArrayEntity.class, exceptionByteArrayId);
    }

    return null;
  }

  public @Nullable String getJobDefinitionId() {
    return jobDefinitionId;
  }

  public void setJobDefinitionId(@Nullable String jobDefinitionId) {
    this.jobDefinitionId = jobDefinitionId;
  }

  public @Nullable String getJobDefinitionType() {
    return jobDefinitionType;
  }

  public void setJobDefinitionType(@Nullable String jobDefinitionType) {
    this.jobDefinitionType = jobDefinitionType;
  }

  public @Nullable String getJobDefinitionConfiguration() {
    return jobDefinitionConfiguration;
  }

  public void setJobDefinitionConfiguration(@Nullable String jobDefinitionConfiguration) {
    this.jobDefinitionConfiguration = jobDefinitionConfiguration;
  }

  public @Nullable String getActivityId() {
    return activityId;
  }

  public void setActivityId(@Nullable String activityId) {
    this.activityId = activityId;
  }

  public @Nullable String getDeploymentId() {
    return deploymentId;
  }

  public void setDeploymentId(@Nullable String deploymentId) {
    this.deploymentId = deploymentId;
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }

  public @Nullable String getTenantId() {
    return tenantId;
  }

  public void setTenantId(@Nullable String tenantId) {
    this.tenantId = tenantId;
  }

  public @Nullable String getHostname() {
    return hostname;
  }

  public void setHostname(@Nullable String hostname) {
    this.hostname = hostname;
  }

  public boolean isCreationLog() {
    return state == JobState.CREATED.getStateCode();
  }

  public boolean isFailureLog() {
    return state == JobState.FAILED.getStateCode();
  }

  public boolean isSuccessLog() {
    return state == JobState.SUCCESSFUL.getStateCode();
  }

  public boolean isDeletionLog() {
    return state == JobState.DELETED.getStateCode();
  }

  public String getFailedActivityId() {
    return failedActivityId;
  }

  public void setFailedActivityId(@Nullable String failedActivityId) {
    this.failedActivityId = failedActivityId;
  }

  public @Nullable String getBatchId() {
    return batchId;
  }

  public void setBatchId(@Nullable String batchId) {
    this.batchId = batchId;
  }
}
