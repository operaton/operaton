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
package org.operaton.bpm.engine.impl.batch.history;

import java.io.Serial;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricIncidentManager;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricJobLogManager;

public class HistoricBatchEntity extends HistoryEvent implements HistoricBatch, DbEntity {

  @Serial private static final long serialVersionUID = 1L;

  protected String type;

  protected int totalJobs;
  protected int batchJobsPerSeed;
  protected int invocationsPerBatchJob;

  protected String seedJobDefinitionId;
  protected String monitorJobDefinitionId;
  protected String batchJobDefinitionId;

  protected String tenantId;
  protected String createUserId;

  protected Date startTime;
  protected Date endTime;
  protected Date executionStartTime;

  @Override
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public int getTotalJobs() {
    return totalJobs;
  }

  public void setTotalJobs(int totalJobs) {
    this.totalJobs = totalJobs;
  }

  @Override
  public int getBatchJobsPerSeed() {
    return batchJobsPerSeed;
  }

  public void setBatchJobsPerSeed(int batchJobsPerSeed) {
    this.batchJobsPerSeed = batchJobsPerSeed;
  }

  @Override
  public int getInvocationsPerBatchJob() {
    return invocationsPerBatchJob;
  }

  public void setInvocationsPerBatchJob(int invocationsPerBatchJob) {
    this.invocationsPerBatchJob = invocationsPerBatchJob;
  }

  @Override
  public String getSeedJobDefinitionId() {
    return seedJobDefinitionId;
  }

  public void setSeedJobDefinitionId(String seedJobDefinitionId) {
    this.seedJobDefinitionId = seedJobDefinitionId;
  }

  @Override
  public String getMonitorJobDefinitionId() {
    return monitorJobDefinitionId;
  }

  public void setMonitorJobDefinitionId(String monitorJobDefinitionId) {
    this.monitorJobDefinitionId = monitorJobDefinitionId;
  }

  @Override
  public String getBatchJobDefinitionId() {
    return batchJobDefinitionId;
  }

  public void setBatchJobDefinitionId(String batchJobDefinitionId) {
    this.batchJobDefinitionId = batchJobDefinitionId;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public String getCreateUserId() {
    return createUserId;
  }

  public void setCreateUserId(String createUserId) {
    this.createUserId = createUserId;
  }

  @Override
  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  @Override
  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }

  @Override
  public Date getExecutionStartTime() {
    return executionStartTime;
  }

  public void setExecutionStartTime(final Date executionStartTime) {
    this.executionStartTime = executionStartTime;
  }

  @Override
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<>();
    persistentState.put("endTime", endTime);
    persistentState.put("executionStartTime", executionStartTime);
    return persistentState;
  }

  public void delete() {
    HistoricIncidentManager historicIncidentManager = Context.getCommandContext().getHistoricIncidentManager();
    historicIncidentManager.deleteHistoricIncidentsByJobDefinitionId(seedJobDefinitionId);
    historicIncidentManager.deleteHistoricIncidentsByJobDefinitionId(monitorJobDefinitionId);
    historicIncidentManager.deleteHistoricIncidentsByJobDefinitionId(batchJobDefinitionId);

    HistoricJobLogManager historicJobLogManager = Context.getCommandContext().getHistoricJobLogManager();
    historicJobLogManager.deleteHistoricJobLogsByJobDefinitionId(seedJobDefinitionId);
    historicJobLogManager.deleteHistoricJobLogsByJobDefinitionId(monitorJobDefinitionId);
    historicJobLogManager.deleteHistoricJobLogsByJobDefinitionId(batchJobDefinitionId);

    Context.getCommandContext().getHistoricBatchManager().delete(this);
  }

}
