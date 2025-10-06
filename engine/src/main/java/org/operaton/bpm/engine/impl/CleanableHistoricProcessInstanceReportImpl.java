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

import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.CleanableHistoricProcessInstanceReport;
import org.operaton.bpm.engine.history.CleanableHistoricProcessInstanceReportResult;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

import static org.operaton.bpm.engine.ProcessEngineConfiguration.HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

public class CleanableHistoricProcessInstanceReportImpl extends AbstractQuery<CleanableHistoricProcessInstanceReport, CleanableHistoricProcessInstanceReportResult> implements CleanableHistoricProcessInstanceReport {

  @Serial private static final long serialVersionUID = 1L;

  protected String[] processDefinitionIdIn;
  protected String[] processDefinitionKeyIn;
  protected String[] tenantIdIn;
  protected boolean isTenantIdSet;
  protected boolean isCompact;

  protected Date currentTimestamp;

  protected boolean isHistoryCleanupStrategyRemovalTimeBased;

  public CleanableHistoricProcessInstanceReportImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public CleanableHistoricProcessInstanceReport processDefinitionIdIn(String... processDefinitionIds) {
    ensureNotNull(NotValidException.class, "", "processDefinitionIdIn", (Object[]) processDefinitionIds);
    this.processDefinitionIdIn = processDefinitionIds;
    return this;
  }

  @Override
  public CleanableHistoricProcessInstanceReport processDefinitionKeyIn(String... processDefinitionKeys) {
    ensureNotNull(NotValidException.class, "", "processDefinitionKeyIn", (Object[]) processDefinitionKeys);
    this.processDefinitionKeyIn = processDefinitionKeys;
    return this;
  }

  @Override
  public CleanableHistoricProcessInstanceReport tenantIdIn(String... tenantIds) {
    ensureNotNull(NotValidException.class, "", "tenantIdIn", (Object[]) tenantIds);
    this.tenantIdIn = tenantIds;
    isTenantIdSet = true;
    return this;
  }

  @Override
  public CleanableHistoricProcessInstanceReport withoutTenantId() {
    this.tenantIdIn = null;
    isTenantIdSet = true;
    return this;
  }

  @Override
  public CleanableHistoricProcessInstanceReport compact() {
    this.isCompact = true;
    return this;
  }

  @Override
  public CleanableHistoricProcessInstanceReport orderByFinished() {
    orderBy(CleanableHistoricInstanceReportProperty.FINISHED_AMOUNT);
    return this;
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    provideHistoryCleanupStrategy(commandContext);

    checkQueryOk();
    return commandContext
        .getHistoricProcessInstanceManager()
        .findCleanableHistoricProcessInstancesReportCountByCriteria(this);
  }

  @Override
  public List<CleanableHistoricProcessInstanceReportResult> executeList(CommandContext commandContext, final Page page) {
    provideHistoryCleanupStrategy(commandContext);

    checkQueryOk();
    return commandContext
        .getHistoricProcessInstanceManager()
        .findCleanableHistoricProcessInstancesReportByCriteria(this, page);
  }

  public Date getCurrentTimestamp() {
    return currentTimestamp;
  }

  public void setCurrentTimestamp(Date currentTimestamp) {
    this.currentTimestamp = currentTimestamp;
  }

  public String[] getProcessDefinitionIdIn() {
    return processDefinitionIdIn;
  }

  public String[] getProcessDefinitionKeyIn() {
    return processDefinitionKeyIn;
  }

  public String[] getTenantIdIn() {
    return tenantIdIn;
  }

  public void setTenantIdIn(String[] tenantIdIn) {
    this.tenantIdIn = tenantIdIn;
  }

  public boolean isTenantIdSet() {
    return isTenantIdSet;
  }

  public boolean isCompact() {
    return isCompact;
  }

  protected void provideHistoryCleanupStrategy(CommandContext commandContext) {
    String historyCleanupStrategy = commandContext.getProcessEngineConfiguration()
      .getHistoryCleanupStrategy();

    isHistoryCleanupStrategyRemovalTimeBased = HISTORY_CLEANUP_STRATEGY_REMOVAL_TIME_BASED.equals(historyCleanupStrategy);
  }

  public boolean isHistoryCleanupStrategyRemovalTimeBased() {
    return isHistoryCleanupStrategyRemovalTimeBased;
  }

}
