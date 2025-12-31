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

import java.util.List;

import org.operaton.bpm.engine.impl.cmd.CommandLogger;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.operaton.bpm.engine.impl.util.CompareUtil;
import org.operaton.bpm.engine.impl.variable.serializer.AbstractTypedValueSerializer;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author roman.smirnov
 */
public class VariableInstanceQueryImpl extends AbstractVariableQueryImpl<VariableInstanceQuery, VariableInstance> implements VariableInstanceQuery {

  private static final CommandLogger LOG = ProcessEngineLogger.CMD_LOGGER;

  protected String variableId;
  protected String variableName;
  protected String[] variableNames;
  protected String variableNameLike;
  protected String[] executionIds;
  protected String[] processInstanceIds;
  protected String[] caseExecutionIds;
  protected String[] caseInstanceIds;
  protected String[] taskIds;
  protected String[] batchIds;
  protected String[] variableScopeIds;
  protected String[] activityInstanceIds;
  protected String[] tenantIds;

  protected boolean isByteArrayFetchingEnabled = true;
  protected boolean isCustomObjectDeserializationEnabled = true;

  public VariableInstanceQueryImpl() { }

  public VariableInstanceQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public VariableInstanceQuery variableId(String id) {
    ensureNotNull("id", id);
    this.variableId = id;
    return this;
  }

  @Override
  public VariableInstanceQuery variableName(String variableName) {
    this.variableName = variableName;
    return this;
  }

  @Override
  public VariableInstanceQuery variableNameIn(String... variableNames) {
    this.variableNames = variableNames;
    return this;
  }

  @Override
  public VariableInstanceQuery variableNameLike(String variableNameLike) {
    this.variableNameLike = variableNameLike;
    return this;
  }

  @Override
  public VariableInstanceQuery executionIdIn(String... executionIds) {
    this.executionIds = executionIds;
    return this;
  }

  @Override
  public VariableInstanceQuery processInstanceIdIn(String... processInstanceIds) {
    this.processInstanceIds = processInstanceIds;
    return this;
  }

  @Override
  public VariableInstanceQuery caseExecutionIdIn(String... caseExecutionIds) {
    this.caseExecutionIds = caseExecutionIds;
    return this;
  }

  @Override
  public VariableInstanceQuery caseInstanceIdIn(String... caseInstanceIds) {
    this.caseInstanceIds = caseInstanceIds;
    return this;
  }

  @Override
  public VariableInstanceQuery taskIdIn(String... taskIds) {
    this.taskIds = taskIds;
    return this;
  }

  @Override
  public VariableInstanceQuery batchIdIn(String... batchIds) {
    this.batchIds = batchIds;
    return this;
  }

  @Override
  public VariableInstanceQuery variableScopeIdIn(String... variableScopeIds) {
    this.variableScopeIds = variableScopeIds;
    return this;
  }

  @Override
  public VariableInstanceQuery activityInstanceIdIn(String... activityInstanceIds) {
    this.activityInstanceIds = activityInstanceIds;
    return this;
  }

  @Override
  public VariableInstanceQuery disableBinaryFetching() {
    this.isByteArrayFetchingEnabled = false;
    return this;
  }

  @Override
  public VariableInstanceQuery disableCustomObjectDeserialization() {
    this.isCustomObjectDeserializationEnabled = false;
    return this;
  }

  @Override
  public VariableInstanceQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    this.tenantIds = tenantIds;
    return this;
  }

  // ordering ////////////////////////////////////////////////////

  @Override
  public VariableInstanceQuery orderByVariableName() {
    orderBy(VariableInstanceQueryProperty.VARIABLE_NAME);
    return this;
  }

  @Override
  public VariableInstanceQuery orderByVariableType() {
    orderBy(VariableInstanceQueryProperty.VARIABLE_TYPE);
    return this;
  }

  @Override
  public VariableInstanceQuery orderByActivityInstanceId() {
    orderBy(VariableInstanceQueryProperty.ACTIVITY_INSTANCE_ID);
    return this;
  }

  @Override
  public VariableInstanceQuery orderByTenantId() {
    orderBy(VariableInstanceQueryProperty.TENANT_ID);
    return this;
  }

  @Override
  protected boolean hasExcludingConditions() {
    return super.hasExcludingConditions() || CompareUtil.elementIsNotContainedInArray(variableName, variableNames);
  }

  // results ////////////////////////////////////////////////////

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    ensureVariablesInitialized();
    return commandContext
      .getVariableInstanceManager()
      .findVariableInstanceCountByQueryCriteria(this);
  }

  @Override
  public List<VariableInstance> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    ensureVariablesInitialized();
    List<VariableInstance> result = commandContext
      .getVariableInstanceManager()
      .findVariableInstanceByQueryCriteria(this, page);

    if (result == null) {
      return result;
    }

    // iterate over the result array to initialize the value and serialized value of the variable
    for (VariableInstance variableInstance : result) {
      VariableInstanceEntity variableInstanceEntity = (VariableInstanceEntity) variableInstance;

      if (shouldFetchValue(variableInstanceEntity)) {
        try {
          variableInstanceEntity.getTypedValue(isCustomObjectDeserializationEnabled);

        } catch(Exception t) {
          // do not fail if one of the variables fails to load
          LOG.exceptionWhileGettingValueForVariable(t);
        }
      }

    }

    return result;
  }

  protected boolean shouldFetchValue(VariableInstanceEntity entity) {
    // do not fetch values for byte arrays eagerly (unless requested by the user)
    return isByteArrayFetchingEnabled
        || !AbstractTypedValueSerializer.BINARY_VALUE_TYPES.contains(entity.getSerializer().getType().getName());
  }

  // getters ////////////////////////////////////////////////////

  public String getVariableId() {
    return variableId;
  }

  public String getVariableName() {
    return variableName;
  }

  public String[] getVariableNames() {
    return variableNames;
  }

  public String getVariableNameLike() {
    return variableNameLike;
  }

  public String[] getExecutionIds() {
    return executionIds;
  }

  public String[] getProcessInstanceIds() {
    return processInstanceIds;
  }

  public String[] getCaseExecutionIds() {
    return caseExecutionIds;
  }

  public String[] getCaseInstanceIds() {
    return caseInstanceIds;
  }

  public String[] getTaskIds() {
    return taskIds;
  }

  public String[] getBatchIds() {
    return batchIds;
  }

  public String[] getVariableScopeIds() {
    return variableScopeIds;
  }

  public String[] getActivityInstanceIds() {
    return activityInstanceIds;
  }
}
