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

import org.operaton.bpm.engine.history.CleanableHistoricDecisionInstanceReportResult;

public class CleanableHistoricDecisionInstanceReportResultEntity implements CleanableHistoricDecisionInstanceReportResult {

  protected String decisionDefinitionId;
  protected String decisionDefinitionKey;
  protected String decisionDefinitionName;
  protected int decisionDefinitionVersion;
  protected Integer historyTimeToLive;
  protected long finishedDecisionInstanceCount;
  protected long cleanableDecisionInstanceCount;
  protected String tenantId;

  @Override
  public String getDecisionDefinitionId() {
    return decisionDefinitionId;
  }

  public void setDecisionDefinitionId(String decisionDefinitionId) {
    this.decisionDefinitionId = decisionDefinitionId;
  }

  @Override
  public String getDecisionDefinitionKey() {
    return decisionDefinitionKey;
  }

  public void setDecisionDefinitionKey(String decisionDefinitionKey) {
    this.decisionDefinitionKey = decisionDefinitionKey;
  }

  @Override
  public String getDecisionDefinitionName() {
    return decisionDefinitionName;
  }

  public void setDecisionDefinitionName(String decisionDefinitionName) {
    this.decisionDefinitionName = decisionDefinitionName;
  }

  @Override
  public int getDecisionDefinitionVersion() {
    return decisionDefinitionVersion;
  }

  public void setDecisionDefinitionVersion(int decisionDefinitionVersion) {
    this.decisionDefinitionVersion = decisionDefinitionVersion;
  }

  @Override
  public Integer getHistoryTimeToLive() {
    return historyTimeToLive;
  }

  public void setHistoryTimeToLive(Integer historyTimeToLive) {
    this.historyTimeToLive = historyTimeToLive;
  }

  @Override
  public long getFinishedDecisionInstanceCount() {
    return finishedDecisionInstanceCount;
  }

  public void setFinishedDecisionInstanceCount(long finishedDecisionInstanceCount) {
    this.finishedDecisionInstanceCount = finishedDecisionInstanceCount;
  }

  @Override
  public long getCleanableDecisionInstanceCount() {
    return cleanableDecisionInstanceCount;
  }

  public void setCleanableDecisionInstanceCount(long cleanableDecisionInstanceCount) {
    this.cleanableDecisionInstanceCount = cleanableDecisionInstanceCount;
  }

  @Override
  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
        + "[decisionDefinitionId = %s, decisionDefinitionKey = %s, decisionDefinitionName = %s, decisionDefinitionVersion = %s, historyTimeToLive = %s, finishedDecisionInstanceCount = %s, cleanableDecisionInstanceCount = %s, tenantId = ".formatted(decisionDefinitionId, decisionDefinitionKey, decisionDefinitionName, decisionDefinitionVersion).formatted(historyTimeToLive, finishedDecisionInstanceCount, cleanableDecisionInstanceCount) + tenantId
        + "]";
  }

}
