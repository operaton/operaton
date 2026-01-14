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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.CommandLogger;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.operaton.bpm.engine.impl.variable.serializer.AbstractTypedValueSerializer;
import org.operaton.bpm.engine.impl.variable.serializer.VariableSerializers;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Christian Lipphardt (Camunda)
 */
public class HistoricVariableInstanceQueryImpl extends AbstractQuery<HistoricVariableInstanceQuery, HistoricVariableInstance> implements
        HistoricVariableInstanceQuery {

  private static final CommandLogger LOG = ProcessEngineLogger.CMD_LOGGER;

  @Serial private static final long serialVersionUID = 1L;

  private List<String> variableNameIn;
  protected String variableId;
  protected String processInstanceId;
  protected String processDefinitionId;
  protected String processDefinitionKey;
  protected String caseInstanceId;
  protected String variableName;
  protected String variableNameLike;
  protected QueryVariableValue queryVariableValue;
  protected Boolean variableNamesIgnoreCase;
  protected Boolean variableValuesIgnoreCase;
  protected String[] variableTypes;
  protected String[] taskIds;
  protected String[] executionIds;
  protected String[] caseExecutionIds;
  protected String[] caseActivityIds;
  protected String[] activityInstanceIds;

  protected String[] tenantIds;
  protected boolean isTenantIdSet;

  protected String[] processInstanceIds;
  protected boolean includeDeleted;

  protected boolean isByteArrayFetchingEnabled = true;
  protected boolean isCustomObjectDeserializationEnabled = true;
  protected String variableIdAfter;
  protected Date createdAfter;

  public HistoricVariableInstanceQueryImpl() {
  }

  public HistoricVariableInstanceQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  public HistoricVariableInstanceQuery idAfter(String id) {
    variableIdAfter = id;
    return this;
  }

  public String getVariableIdAfter() {
    return variableIdAfter;
  }

  @Override
  public HistoricVariableInstanceQuery createdAfter(Date date) {
    createdAfter = date;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery variableNameIn(String... names) {
    ensureNotNull("Variable names", (Object[]) names);
    variableNameIn = Arrays.asList(names);
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery variableId(String id) {
    ensureNotNull("variableId", id);
    this.variableId = id;
    return this;
  }

  @Override
  public HistoricVariableInstanceQueryImpl processInstanceId(String processInstanceId) {
    ensureNotNull("processInstanceId", processInstanceId);
    this.processInstanceId = processInstanceId;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery processDefinitionId(String processDefinitionId) {
    ensureNotNull("processDefinitionId", processDefinitionId);
    this.processDefinitionId = processDefinitionId;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery processDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery caseInstanceId(String caseInstanceId) {
    ensureNotNull("caseInstanceId", caseInstanceId);
    this.caseInstanceId = caseInstanceId;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery variableTypeIn(String... variableTypes) {
    ensureNotNull("Variable types", (Object[]) variableTypes);
    this.variableTypes = lowerCase(variableTypes);
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery matchVariableNamesIgnoreCase() {
    this.variableNamesIgnoreCase = true;
    if (queryVariableValue != null) {
      queryVariableValue.variableNameIgnoreCase = true;
    }
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery matchVariableValuesIgnoreCase() {
    this.variableValuesIgnoreCase = true;
    if (queryVariableValue != null) {
      queryVariableValue.variableValueIgnoreCase = true;
    }
    return this;
  }

  private String[] lowerCase(String... variableTypes) {
    for (int i = 0; i < variableTypes.length; i++) {
      variableTypes[i] = variableTypes[i].toLowerCase();
    }
    return variableTypes;
  }

  /** Only select historic process variables with the given process instance ids. */
  @Override
  public HistoricVariableInstanceQuery processInstanceIdIn(String... processInstanceIds) {
    ensureNotNull("Process Instance Ids", (Object[]) processInstanceIds);
    this.processInstanceIds = processInstanceIds;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery taskIdIn(String... taskIds) {
    ensureNotNull("Task Ids", (Object[]) taskIds);
    this.taskIds = taskIds;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery executionIdIn(String... executionIds) {
    ensureNotNull("Execution Ids", (Object[]) executionIds);
    this.executionIds = executionIds;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery caseExecutionIdIn(String... caseExecutionIds) {
    ensureNotNull("Case execution ids", (Object[]) caseExecutionIds);
    this.caseExecutionIds = caseExecutionIds;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery caseActivityIdIn(String... caseActivityIds) {
    ensureNotNull("Case activity ids", (Object[]) caseActivityIds);
    this.caseActivityIds = caseActivityIds;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery activityInstanceIdIn(String... activityInstanceIds) {
    ensureNotNull("Activity Instance Ids", (Object[]) activityInstanceIds);
    this.activityInstanceIds = activityInstanceIds;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery variableName(String variableName) {
    ensureNotNull("variableName", variableName);
    this.variableName = variableName;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery variableValueEquals(String variableName, Object variableValue) {
    ensureNotNull("variableName", variableName);
    ensureNotNull("variableValue", variableValue);
    this.variableName = variableName;
    queryVariableValue = new QueryVariableValue(variableName, variableValue, QueryOperator.EQUALS, true, Boolean.TRUE.equals(variableNamesIgnoreCase), Boolean.TRUE.equals(variableValuesIgnoreCase));
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery variableNameLike(String variableNameLike) {
    ensureNotNull("variableNameLike", variableNameLike);
    this.variableNameLike = variableNameLike;
    return this;
  }

  protected void ensureVariablesInitialized() {
    if (this.queryVariableValue != null) {
      ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
      VariableSerializers variableSerializers = processEngineConfiguration.getVariableSerializers();
      String dbType = processEngineConfiguration.getDatabaseType();
      queryVariableValue.initialize(variableSerializers, dbType);
    }
  }

  @Override
  public HistoricVariableInstanceQuery disableBinaryFetching() {
    isByteArrayFetchingEnabled = false;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery disableCustomObjectDeserialization() {
    this.isCustomObjectDeserializationEnabled = false;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    this.tenantIds = tenantIds;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery withoutTenantId() {
    this.tenantIds = null;
    this.isTenantIdSet = true;
    return this;
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    ensureVariablesInitialized();
    return commandContext.getHistoricVariableInstanceManager().findHistoricVariableInstanceCountByQueryCriteria(this);
  }

  @Override
  public List<HistoricVariableInstance> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    ensureVariablesInitialized();
    List<HistoricVariableInstance> historicVariableInstances = commandContext
            .getHistoricVariableInstanceManager()
            .findHistoricVariableInstancesByQueryCriteria(this, page);

    if (historicVariableInstances!=null) {
      for (HistoricVariableInstance historicVariableInstance: historicVariableInstances) {

        HistoricVariableInstanceEntity variableInstanceEntity = (HistoricVariableInstanceEntity) historicVariableInstance;
        if (shouldFetchValue(variableInstanceEntity)) {
          try {
            variableInstanceEntity.getTypedValue(isCustomObjectDeserializationEnabled);

          } catch(Exception t) {
            // do not fail if one of the variables fails to load
            LOG.exceptionWhileGettingValueForVariable(t);
          }
        }

      }
    }
    return historicVariableInstances;
  }

  protected boolean shouldFetchValue(HistoricVariableInstanceEntity entity) {
    // do not fetch values for byte arrays eagerly (unless requested by the user)
    return isByteArrayFetchingEnabled
        || !AbstractTypedValueSerializer.BINARY_VALUE_TYPES.contains(entity.getSerializer().getType().getName());
  }

  // order by /////////////////////////////////////////////////////////////////

  @Override
  public HistoricVariableInstanceQuery orderByProcessInstanceId() {
    orderBy(HistoricVariableInstanceQueryProperty.PROCESS_INSTANCE_ID);
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery orderByVariableName() {
    orderBy(HistoricVariableInstanceQueryProperty.VARIABLE_NAME);
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery orderByTenantId() {
    orderBy(HistoricVariableInstanceQueryProperty.TENANT_ID);
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery orderByVariableId() {
    orderBy(HistoricVariableInstanceQueryProperty.VARIABLE_ID);
    return this;
  }

  @Override
  public HistoricVariableInstanceQuery orderByCreationTime() {
    orderBy(HistoricVariableInstanceQueryProperty.CREATE_TIME);
    return this;
  }

  // getters and setters //////////////////////////////////////////////////////

  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public String getCaseInstanceId() {
    return caseInstanceId;
  }

  public String[] getActivityInstanceIds() {
    return activityInstanceIds;
  }

  public String[] getProcessInstanceIds() {
    return processInstanceIds;
  }

  public String[] getTaskIds() {
    return taskIds;
  }

  public String[] getExecutionIds() {
    return executionIds;
  }

  public String[] getCaseExecutionIds() {
    return caseExecutionIds;
  }

  public String[] getCaseActivityIds() {
    return caseActivityIds;
  }

  public boolean isTenantIdSet() {
    return isTenantIdSet;
  }

  public String getVariableName() {
    return variableName;
  }

  public String getVariableNameLike() {
    return variableNameLike;
  }

  public QueryVariableValue getQueryVariableValue() {
    return queryVariableValue;
  }

  public Boolean getVariableNamesIgnoreCase() {
    return variableNamesIgnoreCase;
  }

  public Boolean getVariableValuesIgnoreCase() {
    return variableValuesIgnoreCase;
  }

  @Override
  public HistoricVariableInstanceQuery includeDeleted() {
    includeDeleted = true;
    return this;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  public List<String> getVariableNameIn() {
    return variableNameIn;
  }

  public Date getCreatedAfter() {
    return createdAfter;
  }

}
