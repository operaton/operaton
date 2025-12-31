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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.SuspensionState;
import org.operaton.bpm.engine.impl.util.ImmutablePair;
import org.operaton.bpm.engine.impl.variable.serializer.VariableSerializers;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotEmpty;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Frederik Heremans
 * @author Falko Menge
 * @author Daniel Meyer
 */
public class ProcessInstanceQueryImpl extends AbstractVariableQueryImpl<ProcessInstanceQuery, ProcessInstance> implements ProcessInstanceQuery {

  protected String processInstanceId;
  protected String rootProcessInstanceId;
  protected String businessKey;
  protected String businessKeyLike;
  protected String processDefinitionId;
  protected Set<String> processInstanceIds;
  protected String processDefinitionKey;
  protected String[] processDefinitionKeys;
  protected String[] processDefinitionKeyNotIn;
  protected String deploymentId;
  protected String superProcessInstanceId;
  protected String subProcessInstanceId;
  protected SuspensionState suspensionState;
  protected boolean withIncident;
  protected String incidentType;
  protected String incidentId;
  protected String incidentMessage;
  protected String incidentMessageLike;
  protected String caseInstanceId;
  protected String superCaseInstanceId;
  protected String subCaseInstanceId;
  protected String[] activityIds;
  protected boolean isRootProcessInstances;
  protected boolean isLeafProcessInstances;

  protected boolean isTenantIdSet;
  protected String[] tenantIds;
  protected boolean isProcessDefinitionWithoutTenantId;

  // or query /////////////////////////////
  protected List<ProcessInstanceQueryImpl> queries = new ArrayList<>(List.of(this));
  protected boolean isOrQueryActive;

  public ProcessInstanceQueryImpl() {
  }

  public ProcessInstanceQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public ProcessInstanceQueryImpl processInstanceId(String processInstanceId) {
    ensureNotNull("Process instance id", processInstanceId);
    this.processInstanceId = processInstanceId;
    return this;
  }

  @Override
  public ProcessInstanceQueryImpl rootProcessInstanceId(String rootProcessInstanceId) {
      ensureNotNull("Root process instance id", rootProcessInstanceId);
      this.rootProcessInstanceId = rootProcessInstanceId;
      return this;
  }

  @Override
  public ProcessInstanceQuery processInstanceIds(Set<String> processInstanceIds) {
    ensureNotEmpty("Set of process instance ids", processInstanceIds);
    this.processInstanceIds = processInstanceIds;
    return this;
  }

  @Override
  public ProcessInstanceQuery processInstanceBusinessKey(String businessKey) {
    ensureNotNull("Business key", businessKey);
    this.businessKey = businessKey;
    return this;
  }

  @Override
  public ProcessInstanceQuery processInstanceBusinessKey(String businessKey, String processDefinitionKey) {
    ensureNotNull("Business key", businessKey);
    this.businessKey = businessKey;
    this.processDefinitionKey = processDefinitionKey;
    return this;
  }

  @Override
  public ProcessInstanceQuery processInstanceBusinessKeyLike(String businessKeyLike) {
    this.businessKeyLike = businessKeyLike;
    return this;
  }

  @Override
  public ProcessInstanceQueryImpl processDefinitionId(String processDefinitionId) {
    ensureNotNull("Process definition id", processDefinitionId);
    this.processDefinitionId = processDefinitionId;
    return this;
  }

  @Override
  public ProcessInstanceQueryImpl processDefinitionKey(String processDefinitionKey) {
    ensureNotNull("Process definition key", processDefinitionKey);
    this.processDefinitionKey = processDefinitionKey;
    return this;
  }

  @Override
  public ProcessInstanceQuery processDefinitionKeyIn(String... processDefinitionKeys) {
    ensureNotNull("processDefinitionKeys", (Object[]) processDefinitionKeys);
    this.processDefinitionKeys = processDefinitionKeys;
    return this;
  }

  @Override
  public ProcessInstanceQuery processDefinitionKeyNotIn(String... processDefinitionKeys) {
    ensureNotNull("processDefinitionKeyNotIn", (Object[]) processDefinitionKeys);
    this.processDefinitionKeyNotIn = processDefinitionKeys;
    return this;
  }

  @Override
  public ProcessInstanceQuery deploymentId(String deploymentId) {
    ensureNotNull("Deployment id", deploymentId);
    this.deploymentId = deploymentId;
    return this;
  }

  @Override
  public ProcessInstanceQuery superProcessInstanceId(String superProcessInstanceId) {
    if (isRootProcessInstances) {
      throw new ProcessEngineException("Invalid query usage: cannot set both rootProcessInstances and superProcessInstanceId");
    }
    this.superProcessInstanceId = superProcessInstanceId;
    return this;
  }

  @Override
  public ProcessInstanceQuery subProcessInstanceId(String subProcessInstanceId) {
    this.subProcessInstanceId = subProcessInstanceId;
    return this;
  }

  @Override
  public ProcessInstanceQuery caseInstanceId(String caseInstanceId) {
    ensureNotNull("caseInstanceId", caseInstanceId);
    this.caseInstanceId = caseInstanceId;
    return this;
  }

  @Override
  public ProcessInstanceQuery superCaseInstanceId(String superCaseInstanceId) {
    ensureNotNull("superCaseInstanceId", superCaseInstanceId);
    this.superCaseInstanceId = superCaseInstanceId;
    return this;
  }

  @Override
  public ProcessInstanceQuery subCaseInstanceId(String subCaseInstanceId) {
    ensureNotNull("subCaseInstanceId", subCaseInstanceId);
    this.subCaseInstanceId = subCaseInstanceId;
    return this;
  }

  @Override
  public ProcessInstanceQuery orderByProcessInstanceId() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByProcessInstanceId() within 'or' query");
    }

    orderBy(ProcessInstanceQueryProperty.PROCESS_INSTANCE_ID);
    return this;
  }

  @Override
  public ProcessInstanceQuery orderByProcessDefinitionId() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByProcessDefinitionId() within 'or' query");
    }

    orderBy(new QueryOrderingProperty(QueryOrderingProperty.RELATION_PROCESS_DEFINITION,
        ProcessInstanceQueryProperty.PROCESS_DEFINITION_ID));
    return this;
  }

  @Override
  public ProcessInstanceQuery orderByProcessDefinitionKey() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByProcessDefinitionKey() within 'or' query");
    }

    orderBy(new QueryOrderingProperty(QueryOrderingProperty.RELATION_PROCESS_DEFINITION,
        ProcessInstanceQueryProperty.PROCESS_DEFINITION_KEY));
    return this;
  }

  @Override
  public ProcessInstanceQuery orderByTenantId() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByTenantId() within 'or' query");
    }

    orderBy(ProcessInstanceQueryProperty.TENANT_ID);
    return this;
  }

  @Override
  public ProcessInstanceQuery orderByBusinessKey() {
    if (isOrQueryActive) {
      throw new ProcessEngineException("Invalid query usage: cannot set orderByBusinessKey() within 'or' query");
    }

    orderBy(ProcessInstanceQueryProperty.BUSINESS_KEY);
    return this;
  }

  @Override
  public ProcessInstanceQuery active() {
    this.suspensionState = SuspensionState.ACTIVE;
    return this;
  }

  @Override
  public ProcessInstanceQuery suspended() {
    this.suspensionState = SuspensionState.SUSPENDED;
    return this;
  }

  @Override
  public ProcessInstanceQuery withIncident() {
    this.withIncident = true;
    return this;
  }

  @Override
  public ProcessInstanceQuery incidentType(String incidentType) {
    ensureNotNull("incident type", incidentType);
    this.incidentType = incidentType;
    return this;
  }

  @Override
  public ProcessInstanceQuery incidentId(String incidentId) {
    ensureNotNull("incident id", incidentId);
    this.incidentId = incidentId;
    return this;
  }

  @Override
  public ProcessInstanceQuery incidentMessage(String incidentMessage) {
    ensureNotNull("incident message", incidentMessage);
    this.incidentMessage = incidentMessage;
    return this;
  }

  @Override
  public ProcessInstanceQuery incidentMessageLike(String incidentMessageLike) {
    ensureNotNull("incident messageLike", incidentMessageLike);
    this.incidentMessageLike = incidentMessageLike;
    return this;
  }

  @Override
  public ProcessInstanceQuery tenantIdIn(String... tenantIds) {
    ensureNotNull("tenantIds", (Object[]) tenantIds);
    this.tenantIds = tenantIds;
    isTenantIdSet = true;
    return this;
  }

  @Override
  public ProcessInstanceQuery withoutTenantId() {
    tenantIds = null;
    isTenantIdSet = true;
    return this;
  }

  @Override
  public ProcessInstanceQuery activityIdIn(String... activityIds) {
    ensureNotNull("activity ids", (Object[]) activityIds);
    this.activityIds = activityIds;
    return this;
  }

  @Override
  public ProcessInstanceQuery rootProcessInstances() {
    if (superProcessInstanceId != null) {
      throw new ProcessEngineException("Invalid query usage: cannot set both rootProcessInstances and superProcessInstanceId");
    }
    isRootProcessInstances = true;
    return this;
  }

  @Override
  public ProcessInstanceQuery leafProcessInstances() {
    if(subProcessInstanceId != null) {
      throw new ProcessEngineException("Invalid query usage: cannot set both leafProcessInstances and subProcessInstanceId");
    }
    isLeafProcessInstances = true;
    return this;
  }

  @Override
  public ProcessInstanceQuery processDefinitionWithoutTenantId() {
    isProcessDefinitionWithoutTenantId = true;
    return this;
  }

  //results /////////////////////////////////////////////////////////////////

  @Override
  protected void checkQueryOk() {
    ensureVariablesInitialized();

    super.checkQueryOk();
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();

    return commandContext
      .getExecutionManager()
      .findProcessInstanceCountByQueryCriteria(this);
  }

  @Override
  public List<ProcessInstance> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();

    return commandContext
      .getExecutionManager()
      .findProcessInstancesByQueryCriteria(this, page);
  }

  @Override
  public List<String> executeIdsList(CommandContext commandContext) {
    checkQueryOk();

    return commandContext
      .getExecutionManager()
      .findProcessInstancesIdsByQueryCriteria(this);
  }

  @Override
  public List<ImmutablePair<String, String>> executeDeploymentIdMappingsList(CommandContext commandContext) {
    checkQueryOk();

    return commandContext
      .getExecutionManager()
      .findDeploymentIdMappingsByQueryCriteria(this);
  }

  @Override
  protected void ensureVariablesInitialized() {
    super.ensureVariablesInitialized();

    if (!queries.isEmpty()) {
      ProcessEngineConfigurationImpl processEngineConfiguration = Context.getProcessEngineConfiguration();
      VariableSerializers variableSerializers = processEngineConfiguration.getVariableSerializers();
      String dbType = processEngineConfiguration.getDatabaseType();

      for (ProcessInstanceQueryImpl orQuery: queries) {
        for (var queryVariableValue : orQuery.getQueryVariableValues()) {
          queryVariableValue.initialize(variableSerializers, dbType);
        }
      }
    }
  }

  //getters /////////////////////////////////////////////////////////////////

  public String getProcessInstanceId() {
    return processInstanceId;
  }

  public String getRootProcessInstanceId() {
    return rootProcessInstanceId;
  }

  public Set<String> getProcessInstanceIds() {
    return processInstanceIds;
  }

  public List<ProcessInstanceQueryImpl> getQueries() {
    return queries;
  }

  public void addOrQuery(ProcessInstanceQueryImpl orQuery) {
    orQuery.isOrQueryActive = true;
    this.queries.add(orQuery);
  }

  public void setOrQueryActive() {
    isOrQueryActive = true;
  }

  public boolean isOrQueryActive() {
    return isOrQueryActive;
  }

  public String[] getActivityIds() {
    return activityIds;
  }

  public String getBusinessKey() {
    return businessKey;
  }

  public String getBusinessKeyLike() {
    return businessKeyLike;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  public String[] getProcessDefinitionKeys() {
    return processDefinitionKeys;
  }

  public String[] getProcessDefinitionKeyNotIn() {
    return processDefinitionKeyNotIn;
  }

  public String getDeploymentId() {
    return deploymentId;
  }

  public String getSuperProcessInstanceId() {
    return superProcessInstanceId;
  }

  public String getSubProcessInstanceId() {
    return subProcessInstanceId;
  }

  public SuspensionState getSuspensionState() {
    return suspensionState;
  }

  public void setSuspensionState(SuspensionState suspensionState) {
    this.suspensionState = suspensionState;
  }

  public boolean isWithIncident() {
    return withIncident;
  }

  public String getIncidentId() {
    return incidentId;
  }

  public String getIncidentType() {
    return incidentType;
  }

  public String getIncidentMessage() {
    return incidentMessage;
  }

  public String getIncidentMessageLike() {
    return incidentMessageLike;
  }

  public String getCaseInstanceId() {
    return caseInstanceId;
  }

  public String getSuperCaseInstanceId() {
    return superCaseInstanceId;
  }

  public String getSubCaseInstanceId() {
    return subCaseInstanceId;
  }

  public boolean isTenantIdSet() {
    return isTenantIdSet;
  }

  public boolean isRootProcessInstances() {
    return isRootProcessInstances;
  }

  public boolean isProcessDefinitionWithoutTenantId() {
    return isProcessDefinitionWithoutTenantId;
  }

  public boolean isLeafProcessInstances() {
    return isLeafProcessInstances;
  }

  public String[] getTenantIds() {
    return tenantIds;
  }

  @Override
  public ProcessInstanceQuery or() {
    if (this != queries.get(0)) {
      throw new ProcessEngineException("Invalid query usage: cannot set or() within 'or' query");
    }

    ProcessInstanceQueryImpl orQuery = new ProcessInstanceQueryImpl();
    orQuery.isOrQueryActive = true;
    orQuery.queries = queries;
    queries.add(orQuery);
    return orQuery;
  }

  @Override
  public ProcessInstanceQuery endOr() {
    if (!queries.isEmpty() && this != queries.get(queries.size()-1)) {
      throw new ProcessEngineException("Invalid query usage: cannot set endOr() before or()");
    }

    return queries.get(0);
  }
}
