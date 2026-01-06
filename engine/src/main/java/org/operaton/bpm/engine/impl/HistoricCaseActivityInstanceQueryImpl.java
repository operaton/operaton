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
import org.operaton.bpm.engine.history.HistoricCaseActivityInstance;
import org.operaton.bpm.engine.history.HistoricCaseActivityInstanceQuery;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.util.CompareUtil;

import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.ACTIVE;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.AVAILABLE;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.COMPLETED;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.DISABLED;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.ENABLED;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.SUSPENDED;
import static org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState.TERMINATED;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNull;

/**
 * @author Sebastian Menski
 */
public class HistoricCaseActivityInstanceQueryImpl extends AbstractQuery<HistoricCaseActivityInstanceQuery, HistoricCaseActivityInstance> implements HistoricCaseActivityInstanceQuery {

  @Serial private static final long serialVersionUID = 1L;
  private static final String CASE_ACTIVITY_STATE = "caseActivityState";

  protected String[] caseActivityInstanceIds;
  protected String[] caseActivityIds;

  protected String caseInstanceId;
  protected String caseDefinitionId;
  protected String caseActivityName;
  protected String caseActivityType;
  protected Date createdBefore;
  protected Date createdAfter;
  protected Date endedBefore;
  protected Date endedAfter;
  protected Boolean ended;
  protected Integer caseActivityInstanceState;
  protected Boolean required;
  protected String[] tenantIds;
  protected boolean isTenantIdSet;

  public HistoricCaseActivityInstanceQueryImpl() {
  }

  public HistoricCaseActivityInstanceQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return commandContext
      .getHistoricCaseActivityInstanceManager()
      .findHistoricCaseActivityInstanceCountByQueryCriteria(this);
  }

  @Override
  public List<HistoricCaseActivityInstance> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    return commandContext
      .getHistoricCaseActivityInstanceManager()
      .findHistoricCaseActivityInstancesByQueryCriteria(this, page);
  }

  @Override
  public HistoricCaseActivityInstanceQuery caseActivityInstanceId(String caseActivityInstanceId) {
    ensureNotNull(NotValidException.class, "caseActivityInstanceId", caseActivityInstanceId);
    return caseActivityInstanceIdIn(caseActivityInstanceId);
  }

  @Override
  public HistoricCaseActivityInstanceQuery caseActivityInstanceIdIn(String... caseActivityInstanceIds) {
    ensureNotNull(NotValidException.class, "caseActivityInstanceIds", (Object[]) caseActivityInstanceIds);
    this.caseActivityInstanceIds = caseActivityInstanceIds;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery caseInstanceId(String caseInstanceId) {
    ensureNotNull(NotValidException.class, "caseInstanceId", caseInstanceId);
    this.caseInstanceId = caseInstanceId;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery caseDefinitionId(String caseDefinitionId) {
    ensureNotNull(NotValidException.class, "caseDefinitionId", caseDefinitionId);
    this.caseDefinitionId = caseDefinitionId;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery caseExecutionId(String caseExecutionId) {
    ensureNotNull(NotValidException.class, "caseExecutionId", caseExecutionId);
    return caseActivityInstanceIdIn(caseExecutionId);
  }

  @Override
  public HistoricCaseActivityInstanceQuery caseActivityId(String caseActivityId) {
    ensureNotNull(NotValidException.class, "caseActivityId", caseActivityId);
    return caseActivityIdIn(caseActivityId);
  }

  @Override
  public HistoricCaseActivityInstanceQuery caseActivityIdIn(String... caseActivityIds) {
    ensureNotNull(NotValidException.class, "caseActivityIds", (Object[]) caseActivityIds);
    this.caseActivityIds = caseActivityIds;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery caseActivityName(String caseActivityName) {
    ensureNotNull(NotValidException.class, "caseActivityName", caseActivityName);
    this.caseActivityName = caseActivityName;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery caseActivityType(String caseActivityType) {
    ensureNotNull(NotValidException.class, "caseActivityType", caseActivityType);
    this.caseActivityType = caseActivityType;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery createdBefore(Date date) {
    ensureNotNull(NotValidException.class, "createdBefore", date);
    this.createdBefore = date;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery createdAfter(Date date) {
    ensureNotNull(NotValidException.class, "createdAfter", date);
    this.createdAfter = date;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery endedBefore(Date date) {
    ensureNotNull(NotValidException.class, "finishedBefore", date);
    this.endedBefore = date;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery endedAfter(Date date) {
    ensureNotNull(NotValidException.class, "finishedAfter", date);
    this.endedAfter = date;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery required() {
    this.required = true;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery ended() {
    this.ended = true;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery notEnded() {
    this.ended = false;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery available() {
    ensureCaseActivityInstanceStateIsNull();
    this.caseActivityInstanceState = AVAILABLE.getStateCode();
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery enabled() {
    ensureCaseActivityInstanceStateIsNull();
    this.caseActivityInstanceState = ENABLED.getStateCode();
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery disabled() {
    ensureCaseActivityInstanceStateIsNull();
    this.caseActivityInstanceState = DISABLED.getStateCode();
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery active() {
    ensureCaseActivityInstanceStateIsNull();
    this.caseActivityInstanceState = ACTIVE.getStateCode();
    return this;
  }

  public HistoricCaseActivityInstanceQuery suspended() {
    ensureCaseActivityInstanceStateIsNull();
    this.caseActivityInstanceState = SUSPENDED.getStateCode();
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery completed() {
    ensureCaseActivityInstanceStateIsNull();
    this.caseActivityInstanceState = COMPLETED.getStateCode();
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery terminated() {
    ensureCaseActivityInstanceStateIsNull();
    this.caseActivityInstanceState = TERMINATED.getStateCode();
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    this.tenantIds = tenantIds;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery withoutTenantId() {
    this.tenantIds = null;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  protected boolean hasExcludingConditions() {
    return super.hasExcludingConditions()
      || CompareUtil.areNotInAscendingOrder(createdAfter, createdBefore)
      || CompareUtil.areNotInAscendingOrder(endedAfter, endedBefore);
  }

  // ordering

  @Override
  public HistoricCaseActivityInstanceQuery orderByHistoricCaseActivityInstanceId() {
    orderBy(HistoricCaseActivityInstanceQueryProperty.HISTORIC_CASE_ACTIVITY_INSTANCE_ID);
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery orderByCaseInstanceId() {
    orderBy(HistoricCaseActivityInstanceQueryProperty.CASE_INSTANCE_ID);
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery orderByCaseExecutionId() {
    orderBy(HistoricCaseActivityInstanceQueryProperty.HISTORIC_CASE_ACTIVITY_INSTANCE_ID);
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery orderByCaseActivityId() {
    orderBy(HistoricCaseActivityInstanceQueryProperty.CASE_ACTIVITY_ID);
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery orderByCaseActivityName() {
    orderBy(HistoricCaseActivityInstanceQueryProperty.CASE_ACTIVITY_NAME);
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery orderByCaseActivityType() {
    orderBy(HistoricCaseActivityInstanceQueryProperty.CASE_ACTIVITY_TYPE);
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery orderByHistoricCaseActivityInstanceCreateTime() {
    orderBy(HistoricCaseActivityInstanceQueryProperty.CREATE);
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery orderByHistoricCaseActivityInstanceEndTime() {
    orderBy(HistoricCaseActivityInstanceQueryProperty.END);
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery orderByHistoricCaseActivityInstanceDuration() {
    orderBy(HistoricCaseActivityInstanceQueryProperty.DURATION);
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery orderByCaseDefinitionId() {
    orderBy(HistoricCaseActivityInstanceQueryProperty.CASE_DEFINITION_ID);
    return this;
  }

  @Override
  public HistoricCaseActivityInstanceQuery orderByTenantId() {
    return orderBy(HistoricCaseActivityInstanceQueryProperty.TENANT_ID);
  }

  // getter

  public String[] getCaseActivityInstanceIds() {
    return caseActivityInstanceIds;
  }

  public String getCaseInstanceId() {
    return caseInstanceId;
  }

  public String getCaseDefinitionId() {
    return caseDefinitionId;
  }

  public String[] getCaseActivityIds() {
    return caseActivityIds;
  }

  public String getCaseActivityName() {
    return caseActivityName;
  }

  public String getCaseActivityType() {
    return caseActivityType;
  }

  public Date getCreatedBefore() {
    return createdBefore;
  }

  public Date getCreatedAfter() {
    return createdAfter;
  }

  public Date getEndedBefore() {
    return endedBefore;
  }

  public Date getEndedAfter() {
    return endedAfter;
  }

  public Boolean getEnded() {
    return ended;
  }

  public Integer getCaseActivityInstanceState() {
    return caseActivityInstanceState;
  }

  public Boolean isRequired() {
    return required;
  }

  public boolean isTenantIdSet() {
    return isTenantIdSet;
  }

  private void ensureCaseActivityInstanceStateIsNull() {
    ensureNull(NotValidException.class, "Already querying for case activity instance state '%s'".formatted(caseActivityInstanceState),
      CASE_ACTIVITY_STATE, caseActivityInstanceState);
  }

}
