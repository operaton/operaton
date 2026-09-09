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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.util.CompareUtil;
import org.operaton.bpm.engine.impl.util.ImmutablePair;
import org.operaton.bpm.engine.impl.variable.serializer.VariableSerializers;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureEmpty;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotContainsEmptyString;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotContainsNull;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotEmpty;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Tom Baeyens
 * @author Falko Menge
 * @author Bernd Ruecker
 */
public @NullMarked class HistoricProcessInstanceQueryImpl extends AbstractVariableQueryImpl<HistoricProcessInstanceQuery, HistoricProcessInstance> implements HistoricProcessInstanceQuery {

  @Serial private static final long serialVersionUID = 1L;
  private static final String MSG_ALREADY_QUERYING = "Already querying for historic process instance with another state";
  protected @Nullable String processInstanceId;
  protected @Nullable String rootProcessInstanceId;
  protected @Nullable String processDefinitionId;
  protected @Nullable String processDefinitionName;
  protected @Nullable String processDefinitionNameLike;
  protected @Nullable String businessKey;
  protected String@Nullable[] businessKeyIn;
  protected @Nullable String businessKeyLike;
  protected boolean finished;
  protected boolean unfinished;
  protected boolean withJobsRetrying;
  protected boolean withIncidents;
  protected boolean withRootIncidents;
  protected @Nullable String incidentType;
  protected @Nullable String incidentStatus;
  protected @Nullable String incidentMessage;
  protected @Nullable String incidentMessageLike;
  protected @Nullable String startedBy;
  protected boolean isRootProcessInstances;
  protected @Nullable String superProcessInstanceId;
  protected @Nullable String subProcessInstanceId;
  protected @Nullable String superCaseInstanceId;
  protected @Nullable String subCaseInstanceId;
  private @Nullable List<String> processKeyNotIn;
  protected @Nullable Date startedBefore;
  protected @Nullable Date startedAfter;
  protected @Nullable Date finishedBefore;
  protected @Nullable Date finishedAfter;
  protected @Nullable Date executedActivityAfter;
  protected @Nullable Date executedActivityBefore;
  protected @Nullable Date executedJobAfter;
  protected @Nullable Date executedJobBefore;
  protected @Nullable String processDefinitionKey;
  protected String@Nullable[] processDefinitionKeys;
  private @Nullable Set<String> processInstanceIds;
  protected String@Nullable[] processInstanceIdNotIn;
  protected String@Nullable[] tenantIds;
  protected boolean isTenantIdSet;
  protected String@Nullable[] executedActivityIds;
  protected String@Nullable[] activeActivityIds;
  protected String@Nullable[] activityIds;
  protected String@Nullable[] incidentIds;
  private final Set<String> state = new HashSet<>();

  protected @Nullable String caseInstanceId;

  private List<HistoricProcessInstanceQueryImpl> queries = new ArrayList<>(Collections.singletonList(this));
  protected boolean isOrQueryActive;

  private final Map<String, Set<QueryVariableValue>> queryVariableNameToValuesMap = new HashMap<>();

  public HistoricProcessInstanceQueryImpl() {
  }

  public HistoricProcessInstanceQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public HistoricProcessInstanceQueryImpl processInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery processInstanceIds(Set<String> processInstanceIds) {
    ensureNotEmpty("Set of process instance ids", processInstanceIds);
    this.processInstanceIds = processInstanceIds;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery processInstanceIdNotIn(String... processInstanceIdNotIn){
    ensureNotNull("processInstanceIdNotIn", (Object[]) processInstanceIdNotIn);
    this.processInstanceIdNotIn = processInstanceIdNotIn;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery rootProcessInstanceId(String rootProcessInstanceId) {
    ensureNotNull("Root process instance id", rootProcessInstanceId);
    this.rootProcessInstanceId = rootProcessInstanceId;
    return this;
  }

  @Override
  public HistoricProcessInstanceQueryImpl processDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery processDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery processDefinitionKeyIn(String... processDefinitionKeys) {
    ensureNotNull("processDefinitionKeys", (Object[]) processDefinitionKeys);
    this.processDefinitionKeys = processDefinitionKeys;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery processDefinitionName(String processDefinitionName) {
    this.processDefinitionName = processDefinitionName;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery processDefinitionNameLike(String nameLike) {
    this.processDefinitionNameLike = nameLike;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery processInstanceBusinessKey(String businessKey) {
    this.businessKey = businessKey;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery processInstanceBusinessKeyIn(String... businessKeyIn) {
    this.businessKeyIn = businessKeyIn;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery processInstanceBusinessKeyLike(String businessKeyLike) {
    this.businessKeyLike = businessKeyLike;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery finished() {
    this.finished = true;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery unfinished() {
    this.unfinished = true;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery withIncidents() {
    this.withIncidents = true;

    return this;
  }

  @Override
  public HistoricProcessInstanceQuery withRootIncidents() {
    this.withRootIncidents = true;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery incidentIdIn(String... incidentIds) {
    ensureNotNull("incidentIds", (Object[]) incidentIds);
    this.incidentIds = incidentIds;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery incidentType(String incidentType) {
    ensureNotNull("incident type", incidentType);
    this.incidentType = incidentType;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery incidentStatus(String status) {
    this.incidentStatus = status;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery incidentMessage(String incidentMessage) {
    ensureNotNull("incidentMessage", incidentMessage);
    this.incidentMessage = incidentMessage;

    return this;
  }

  @Override
  public HistoricProcessInstanceQuery incidentMessageLike(String incidentMessageLike) {
    ensureNotNull("incidentMessageLike", incidentMessageLike);
    this.incidentMessageLike = incidentMessageLike;

    return this;
  }

  @Override
  public HistoricProcessInstanceQuery withJobsRetrying(){
    this.withJobsRetrying = true;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery startedBy(String userId) {
    this.startedBy = userId;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery processDefinitionKeyNotIn(List<String> processDefinitionKeys) {
    ensureNotContainsNull("processDefinitionKeys", processDefinitionKeys);
    ensureNotContainsEmptyString("processDefinitionKeys", processDefinitionKeys);
    this.processKeyNotIn = processDefinitionKeys;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery startedAfter(Date date) {
    startedAfter = date;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery startedBefore(Date date) {
    startedBefore = date;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery finishedAfter(Date date) {
    finishedAfter = date;
    finished = true;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery finishedBefore(Date date) {
    finishedBefore = date;
    finished = true;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery rootProcessInstances() {
    if (superProcessInstanceId != null) {
      throw new BadUserRequestException("Invalid query usage: cannot set both rootProcessInstances and superProcessInstanceId");
    }
    if (superCaseInstanceId != null) {
      throw new BadUserRequestException("Invalid query usage: cannot set both rootProcessInstances and superCaseInstanceId");
    }
    isRootProcessInstances = true;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery superProcessInstanceId(String superProcessInstanceId) {
    if (isRootProcessInstances) {
      throw new BadUserRequestException("Invalid query usage: cannot set both rootProcessInstances and superProcessInstanceId");
    }
    this.superProcessInstanceId = superProcessInstanceId;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery subProcessInstanceId(String subProcessInstanceId) {
    this.subProcessInstanceId = subProcessInstanceId;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery superCaseInstanceId(String superCaseInstanceId) {
    if (isRootProcessInstances) {
      throw new BadUserRequestException("Invalid query usage: cannot set both rootProcessInstances and superCaseInstanceId");
    }
    this.superCaseInstanceId = superCaseInstanceId;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery subCaseInstanceId(String subCaseInstanceId) {
    this.subCaseInstanceId = subCaseInstanceId;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery caseInstanceId(String caseInstanceId) {
    this.caseInstanceId = caseInstanceId;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    this.tenantIds = tenantIds;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery withoutTenantId() {
    tenantIds = null;
    isTenantIdSet = true;
    return this;
  }

  @Override
  protected boolean hasExcludingConditions() {
    return super.hasExcludingConditions()
      || (finished && unfinished)
      || CompareUtil.areNotInAscendingOrder(startedAfter, startedBefore)
      || CompareUtil.areNotInAscendingOrder(finishedAfter, finishedBefore)
      || CompareUtil.elementIsContainedInList(processDefinitionKey, processKeyNotIn)
      || CompareUtil.elementIsNotContainedInList(processInstanceId, processInstanceIds)
      || CompareUtil.elementIsContainedInArray(processInstanceId, processInstanceIdNotIn)
      || CompareUtil.elementsAreContainedInArray(processInstanceIds, processInstanceIdNotIn);
  }

  @Override
  public HistoricProcessInstanceQuery orderByProcessInstanceBusinessKey() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByProcessInstanceBusinessKey() within 'or' query");
    }
    return orderBy(HistoricProcessInstanceQueryProperty.BUSINESS_KEY);
  }

  @Override
  public HistoricProcessInstanceQuery orderByProcessInstanceDuration() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByProcessInstanceDuration() within 'or' query");
    }
    return orderBy(HistoricProcessInstanceQueryProperty.DURATION);
  }

  @Override
  public HistoricProcessInstanceQuery orderByProcessInstanceStartTime() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByProcessInstanceStartTime() within 'or' query");
    }
    return orderBy(HistoricProcessInstanceQueryProperty.START_TIME);
  }

  @Override
  public HistoricProcessInstanceQuery orderByProcessInstanceEndTime() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByProcessInstanceEndTime() within 'or' query");
    }
    return orderBy(HistoricProcessInstanceQueryProperty.END_TIME);
  }

  @Override
  public HistoricProcessInstanceQuery orderByProcessDefinitionId() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByProcessDefinitionId() within 'or' query");
    }
    return orderBy(HistoricProcessInstanceQueryProperty.PROCESS_DEFINITION_ID);
  }

  @Override
  public HistoricProcessInstanceQuery orderByProcessDefinitionKey() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByProcessDefinitionKey() within 'or' query");
    }
    return orderBy(HistoricProcessInstanceQueryProperty.PROCESS_DEFINITION_KEY);
  }

  @Override
  public HistoricProcessInstanceQuery orderByProcessDefinitionName() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByProcessDefinitionName() within 'or' query");
    }
    return orderBy(HistoricProcessInstanceQueryProperty.PROCESS_DEFINITION_NAME);
  }

  @Override
  public HistoricProcessInstanceQuery orderByProcessDefinitionVersion() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByProcessDefinitionVersion() within 'or' query");
    }
    return orderBy(HistoricProcessInstanceQueryProperty.PROCESS_DEFINITION_VERSION);
  }

  @Override
  public HistoricProcessInstanceQuery orderByProcessInstanceId() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByProcessInstanceId() within 'or' query");
    }
    return orderBy(HistoricProcessInstanceQueryProperty.PROCESS_INSTANCE_ID_);
  }

  @Override
  public HistoricProcessInstanceQuery orderByTenantId() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByTenantId() within 'or' query");
    }
    return orderBy(HistoricProcessInstanceQueryProperty.TENANT_ID);
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    ensureVariablesInitialized();
    return commandContext
      .getHistoricProcessInstanceManager()
      .findHistoricProcessInstanceCountByQueryCriteria(this);
  }

  @Override
  public List<HistoricProcessInstance> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    ensureVariablesInitialized();
    return commandContext
      .getHistoricProcessInstanceManager()
      .findHistoricProcessInstancesByQueryCriteria(this, page);
  }

  @Override
  public List<String> executeIdsList(CommandContext commandContext) {
    checkQueryOk();
    ensureVariablesInitialized();
    return commandContext
        .getHistoricProcessInstanceManager()
        .findHistoricProcessInstanceIds(this);
  }

  @Override
  public List<ImmutablePair<String, String>> executeDeploymentIdMappingsList(CommandContext commandContext) {
    checkQueryOk();
    ensureVariablesInitialized();
    return commandContext
        .getHistoricProcessInstanceManager()
        .findDeploymentIdMappingsByQueryCriteria(this);
  }

  @Override
  public List<QueryVariableValue> getQueryVariableValues() {
    return queryVariableNameToValuesMap.values()
        .stream()
        .flatMap(Set::stream)
        .toList();
  }

  public Map<String, Set<QueryVariableValue>> getQueryVariableNameToValuesMap() {
    return queryVariableNameToValuesMap;
  }

  @Override
  protected void ensureVariablesInitialized() {
    super.ensureVariablesInitialized();

    if (!queries.isEmpty()) {
      ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
      VariableSerializers variableSerializers = processEngineConfiguration.getVariableSerializers();
      String dbType = processEngineConfiguration.getDatabaseType();

      for (HistoricProcessInstanceQueryImpl orQuery: queries) {
        for (var variableValue : orQuery.getQueryVariableValues()) {
          variableValue.initialize(variableSerializers, dbType);
        }
      }
    }
  }

  @Override
  protected void addVariable(String name, Object value, QueryOperator operator, boolean processInstanceScope) {
    QueryVariableValue queryVariableValue = createQueryVariableValue(name, value, operator, processInstanceScope);

    Set<QueryVariableValue> queryVariableValues = queryVariableNameToValuesMap.get(name);
    if (queryVariableValues == null) {
      queryVariableNameToValuesMap.put(name, new HashSet<>(Collections.singletonList(queryVariableValue)));

    } else {
      queryVariableValues.add(queryVariableValue);

    }
  }

  public List<HistoricProcessInstanceQueryImpl> getQueries() {
    return queries;
  }

  public void addOrQuery(HistoricProcessInstanceQueryImpl orQuery) {
    orQuery.isOrQueryActive = true;
    this.queries.add(orQuery);
  }

  public void setOrQueryActive() {
    isOrQueryActive = true;
  }

  public boolean isOrQueryActive() {
    return isOrQueryActive;
  }

  public String@Nullable[] getActiveActivityIds() {
    return activeActivityIds;
  }

  public String@Nullable[] getActivityIds() {
    return activityIds;
  }

  public @Nullable String getBusinessKey() {
    return businessKey;
  }

  public String@Nullable[] getBusinessKeyIn() {
    return businessKeyIn;
  }

  public @Nullable String getBusinessKeyLike() {
    return businessKeyLike;
  }

  public String@Nullable[] getExecutedActivityIds() {
    return executedActivityIds;
  }

  public @Nullable Date getExecutedActivityAfter() {
    return executedActivityAfter;
  }

  public @Nullable Date getExecutedActivityBefore() {
    return executedActivityBefore;
  }

  public @Nullable String getRootProcessInstanceId() {
    return rootProcessInstanceId;
  }

  public @Nullable Date getExecutedJobAfter() {
    return executedJobAfter;
  }

  public @Nullable Date getExecutedJobBefore() {
    return executedJobBefore;
  }

  public boolean isOpen() {
    return unfinished;
  }

  public boolean isUnfinished() {
    return unfinished;
  }

  public boolean isFinished() {
    return finished;
  }

  public @Nullable String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public @Nullable String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  public String@Nullable[] getProcessDefinitionKeys() {
    return processDefinitionKeys;
  }

  public @Nullable String getProcessDefinitionIdLike() {
    return processDefinitionKey + ":%:%";
  }

  public @Nullable String getProcessDefinitionName() {
    return processDefinitionName;
  }

  public @Nullable String getProcessDefinitionNameLike() {
    return processDefinitionNameLike;
  }

  public @Nullable String getProcessInstanceId() {
    return processInstanceId;
  }

  public @Nullable Set<String> getProcessInstanceIds() {
    return processInstanceIds;
  }

  public String@Nullable[] getProcessInstanceIdNotIn() {
    return processInstanceIdNotIn;
  }

  public @Nullable String getStartedBy() {
    return startedBy;
  }

  public @Nullable String getSuperProcessInstanceId() {
    return superProcessInstanceId;
  }

  public void setSuperProcessInstanceId(String superProcessInstanceId) {
    this.superProcessInstanceId = superProcessInstanceId;
  }

  public List<String> getProcessKeyNotIn() {
    return processKeyNotIn;
  }

  public @Nullable Date getStartedAfter() {
    return startedAfter;
  }

  public @Nullable Date getStartedBefore() {
    return startedBefore;
  }

  public @Nullable Date getFinishedAfter() {
    return finishedAfter;
  }

  public @Nullable Date getFinishedBefore() {
    return finishedBefore;
  }

  public @Nullable String getCaseInstanceId() {
    return caseInstanceId;
  }

  public @Nullable String getIncidentType() {
    return incidentType;
  }

  public @Nullable String getIncidentMessage() {
    return this.incidentMessage;
  }

  public @Nullable String getIncidentMessageLike() {
    return this.incidentMessageLike;
  }

  public @Nullable String getIncidentStatus() {
    return incidentStatus;
  }

  public Set<String> getState() {
    return state;
  }

  public @Nullable Date getFinishDateBy() {
    return finishDateBy;
  }

  public @Nullable Date getStartDateBy() {
    return startDateBy;
  }

  public @Nullable Date getStartDateOn() {
    return startDateOn;
  }

  public @Nullable Date getStartDateOnBegin() {
    return startDateOnBegin;
  }

  public @Nullable Date getStartDateOnEnd() {
    return startDateOnEnd;
  }

  public @Nullable Date getFinishDateOn() {
    return finishDateOn;
  }

  public @Nullable Date getFinishDateOnBegin() {
    return finishDateOnBegin;
  }

  public @Nullable Date getFinishDateOnEnd() {
    return finishDateOnEnd;
  }

  public boolean isTenantIdSet() {
    return isTenantIdSet;
  }

  public boolean getIsTenantIdSet() {
    return isTenantIdSet;
  }

  public boolean isWithJobsRetrying(){
    return withJobsRetrying;
  }

  public boolean isWithIncidents() {
    return withIncidents;
  }

  public boolean isWithRootIncidents() {
    return withRootIncidents;
  }

  // below is deprecated and to be removed in 5.12

  @Nullable protected Date startDateBy;
  @Nullable protected Date startDateOn;
  @Nullable protected Date finishDateBy;
  @Nullable protected Date finishDateOn;
  @Nullable protected Date startDateOnBegin;
  @Nullable protected Date startDateOnEnd;
  @Nullable protected Date finishDateOnBegin;
  @Nullable protected Date finishDateOnEnd;

  /**
   * @deprecated since 1.0, use {@link #startedAfter(Date)} and {@link #startedBefore(Date)} instead.
   */
  @Deprecated(since = "1.0")
  @Override
  public HistoricProcessInstanceQuery startDateBy(Date date) {
    this.startDateBy = this.calculateMidnight(date);
    return this;
  }

  /**
   * @deprecated since 1.0, use {@link #startedAfter(Date)} and {@link #startedBefore(Date)} instead.
   */
  @Deprecated(since = "1.0")
  @Override
  public HistoricProcessInstanceQuery startDateOn(Date date) {
    this.startDateOn = date;
    this.startDateOnBegin = this.calculateMidnight(date);
    this.startDateOnEnd = this.calculateBeforeMidnight(date);
    return this;
  }

  /**
   * @deprecated since 1.0, use {@link #finishedAfter(Date)} and {@link #finishedBefore(Date)} instead.
   */
  @Deprecated(since = "1.0")
  @Override
  public HistoricProcessInstanceQuery finishDateBy(Date date) {
    this.finishDateBy = this.calculateBeforeMidnight(date);
    return this;
  }

  /**
   * @deprecated since 1.0, use {@link #finishedAfter(Date)} and {@link #finishedBefore(Date)} instead.
   */
  @Deprecated(since = "1.0")
  @Override
  public HistoricProcessInstanceQuery finishDateOn(Date date) {
    this.finishDateOn = date;
    this.finishDateOnBegin = this.calculateMidnight(date);
    this.finishDateOnEnd = this.calculateBeforeMidnight(date);
    return this;
  }

  /**
   * @deprecated since 1.0, internal utility method.
   */
  @Deprecated(since = "1.0")
  private Date calculateBeforeMidnight(Date date){
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.add(Calendar.DAY_OF_MONTH, 1);
    cal.add(Calendar.SECOND, -1);
    return cal.getTime();
  }

  /**
   * @deprecated since 1.0, internal utility method.
   */
  @Deprecated(since = "1.0")
  private Date calculateMidnight(Date date){
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.set(Calendar.MILLISECOND, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.HOUR, 0);
    return cal.getTime();
  }

  public boolean isRootProcessInstances() {
    return isRootProcessInstances;
  }

  public @Nullable String getSubProcessInstanceId() {
    return subProcessInstanceId;
  }

  public @Nullable String getSuperCaseInstanceId() {
    return superCaseInstanceId;
  }

  public @Nullable String getSubCaseInstanceId() {
    return subCaseInstanceId;
  }

  public String[] getTenantIds() {
    return Objects.requireNonNullElse(tenantIds, new String[0]);
  }

  public String[] getIncidentIds() {
    return Objects.requireNonNullElse(incidentIds, new String[0]);
  }

  @Override
  public HistoricProcessInstanceQuery executedActivityAfter(Date date) {
    this.executedActivityAfter = date;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery executedActivityBefore(Date date) {
    this.executedActivityBefore = date;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery executedJobAfter(Date date) {
    this.executedJobAfter = date;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery executedJobBefore(Date date) {
    this.executedJobBefore = date;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery executedActivityIdIn(String @Nullable... ids) {
    ensureNotNull(BadUserRequestException.class, "activity ids", (Object[]) ids);
    ensureNotContainsNull(BadUserRequestException.class, "activity ids", Arrays.asList(ids));
    this.executedActivityIds = ids;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery activeActivityIdIn(String @Nullable... ids) {
    ensureNotNull(BadUserRequestException.class, "activity ids", (Object[]) ids);
    ensureNotContainsNull(BadUserRequestException.class, "activity ids", Arrays.asList(ids));
    this.activeActivityIds = ids;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery activityIdIn(String @Nullable... ids) {
    ensureNotNull(BadUserRequestException.class, "activity ids", (Object[]) ids);
    ensureNotContainsNull(BadUserRequestException.class, "activity ids", Arrays.asList(ids));
    this.activityIds = ids;
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery active() {
    if (!isOrQueryActive) {
      ensureEmpty(BadUserRequestException.class, MSG_ALREADY_QUERYING, state);
    }
    state.add(HistoricProcessInstance.STATE_ACTIVE);
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery suspended() {
    if (!isOrQueryActive) {
      ensureEmpty(BadUserRequestException.class, MSG_ALREADY_QUERYING, state);
    }
    state.add(HistoricProcessInstance.STATE_SUSPENDED);
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery completed() {
    if (!isOrQueryActive) {
      ensureEmpty(BadUserRequestException.class, MSG_ALREADY_QUERYING, state);
    }
    state.add(HistoricProcessInstance.STATE_COMPLETED);
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery externallyTerminated() {
    if (!isOrQueryActive) {
      ensureEmpty(BadUserRequestException.class, MSG_ALREADY_QUERYING, state);
    }
    state.add(HistoricProcessInstance.STATE_EXTERNALLY_TERMINATED);
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery internallyTerminated() {
    if (!isOrQueryActive) {
      ensureEmpty(BadUserRequestException.class, MSG_ALREADY_QUERYING, state);
    }
    state.add(HistoricProcessInstance.STATE_INTERNALLY_TERMINATED);
    return this;
  }

  @Override
  public HistoricProcessInstanceQuery or() {
    if (this != queries.get(0)) {
      throw new ProcessEngineException("Invalid query usage: cannot set or() within 'or' query");
    }

    HistoricProcessInstanceQueryImpl orQuery = new HistoricProcessInstanceQueryImpl();
    orQuery.isOrQueryActive = true;
    orQuery.queries = queries;
    queries.add(orQuery);
    return orQuery;
  }

  @Override
  public HistoricProcessInstanceQuery endOr() {
    if (!queries.isEmpty() && this != queries.get(queries.size()-1)) {
      throw new ProcessEngineException("Invalid query usage: cannot set endOr() before or()");
    }

    return queries.get(0);
  }

}
