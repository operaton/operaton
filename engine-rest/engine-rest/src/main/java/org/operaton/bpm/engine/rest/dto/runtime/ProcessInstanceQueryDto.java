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
package org.operaton.bpm.engine.rest.dto.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.ProcessInstanceQueryImpl;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.VariableQueryParameterDto;
import org.operaton.bpm.engine.rest.dto.converter.BooleanConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringListConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringSetConverter;
import org.operaton.bpm.engine.rest.dto.converter.VariableListConverter;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;

import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.*;
import static java.lang.Boolean.TRUE;

public class ProcessInstanceQueryDto extends AbstractQueryDto<ProcessInstanceQuery> {

  private static final String SORT_BY_INSTANCE_ID_VALUE = "instanceId";
  private static final String SORT_BY_DEFINITION_KEY_VALUE = "definitionKey";
  private static final String SORT_BY_DEFINITION_ID_VALUE = "definitionId";
  private static final String SORT_BY_TENANT_ID = "tenantId";
  private static final String SORT_BY_BUSINESS_KEY = "businessKey";

  private static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_INSTANCE_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_DEFINITION_KEY_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_DEFINITION_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_BUSINESS_KEY);
  }

  private String deploymentId;
  private String processDefinitionKey;
  private List<String> processDefinitionKeys;
  private List<String> processDefinitionKeyNotIn;
  private String businessKey;
  private String businessKeyLike;
  private String caseInstanceId;
  private String processDefinitionId;
  private String superProcessInstance;
  private String subProcessInstance;
  private String superCaseInstance;
  private String subCaseInstance;
  private Boolean active;
  private Boolean suspended;
  private Set<String> processInstanceIds;
  private Boolean withIncident;
  private String incidentId;
  private String rootProcessInstanceId;
  private String incidentType;
  private String incidentMessage;
  private String incidentMessageLike;
  private List<String> tenantIds;
  private Boolean withoutTenantId;
  private List<String> activityIds;
  private Boolean rootProcessInstances;
  private Boolean leafProcessInstances;
  private Boolean isProcessDefinitionWithoutTenantId;

  protected Boolean variableNamesIgnoreCase;
  protected Boolean variableValuesIgnoreCase;

  private List<VariableQueryParameterDto> variables;

  private List<ProcessInstanceQueryDto> orQueries;

  public ProcessInstanceQueryDto() {

  }

  public ProcessInstanceQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("orQueries")
  public void setOrQueries(List<ProcessInstanceQueryDto> orQueries) {
    this.orQueries = orQueries;
  }

  public Set<String> getProcessInstanceIds() {
    return processInstanceIds;
  }

  @OperatonQueryParam(value = "processInstanceIds", converter = StringSetConverter.class)
  public void setProcessInstanceIds(Set<String> processInstanceIds) {
		this.processInstanceIds = processInstanceIds;
  }

  public String getDeploymentId() {
    return deploymentId;
  }

  @OperatonQueryParam("deploymentId")
  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  public String getProcessDefinitionKey() {
    return processDefinitionKey;
  }

  @OperatonQueryParam("processDefinitionKey")
  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  public List<String> getProcessDefinitionKeys() {
    return processDefinitionKeys;
  }

  @OperatonQueryParam(value = "processDefinitionKeyIn", converter = StringListConverter.class)
  public void setProcessDefinitionKeyIn(List<String> processDefinitionKeys) {
    this.processDefinitionKeys = processDefinitionKeys;
  }

  public List<String> getProcessDefinitionKeyNotIn() {
    return processDefinitionKeyNotIn;
  }

  @OperatonQueryParam(value = "processDefinitionKeyNotIn", converter = StringListConverter.class)
  public void setProcessDefinitionKeyNotIn(List<String> processDefinitionKeys) {
    this.processDefinitionKeyNotIn = processDefinitionKeys;
  }

  public String getBusinessKey() {
    return businessKey;
  }

  @OperatonQueryParam("businessKey")
  public void setBusinessKey(String businessKey) {
    this.businessKey = businessKey;
  }

  public String getBusinessKeyLike() {
    return businessKeyLike;
  }

  @OperatonQueryParam("businessKeyLike")
  public void setBusinessKeyLike(String businessKeyLike) {
    this.businessKeyLike = businessKeyLike;
  }

  public String getCaseInstanceId() {
    return caseInstanceId;
  }

  @OperatonQueryParam("caseInstanceId")
  public void setCaseInstanceId(String caseInstanceId) {
    this.caseInstanceId = caseInstanceId;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  @OperatonQueryParam("processDefinitionId")
  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  public String getSuperProcessInstance() {
    return superProcessInstance;
  }

  @OperatonQueryParam("superProcessInstance")
  public void setSuperProcessInstance(String superProcessInstance) {
    this.superProcessInstance = superProcessInstance;
  }

  public String getSubProcessInstance() {
    return subProcessInstance;
  }

  @OperatonQueryParam("subProcessInstance")
  public void setSubProcessInstance(String subProcessInstance) {
    this.subProcessInstance = subProcessInstance;
  }

  public String getSuperCaseInstance() {
    return superCaseInstance;
  }

  @OperatonQueryParam("superCaseInstance")
  public void setSuperCaseInstance(String superCaseInstance) {
    this.superCaseInstance = superCaseInstance;
  }

  public String getSubCaseInstance() {
    return subCaseInstance;
  }

  @OperatonQueryParam("subCaseInstance")
  public void setSubCaseInstance(String subCaseInstance) {
    this.subCaseInstance = subCaseInstance;
  }

  public Boolean isActive() {
    return active;
  }

  @OperatonQueryParam(value = "active", converter = BooleanConverter.class)
  public void setActive(Boolean active) {
    this.active = active;
  }

  public Boolean isSuspended() {
    return suspended;
  }

  @OperatonQueryParam(value = "suspended", converter = BooleanConverter.class)
  public void setSuspended(Boolean suspended) {
    this.suspended = suspended;
  }

  public List<VariableQueryParameterDto> getVariables() {
    return variables;
  }

  @OperatonQueryParam(value = "variables", converter = VariableListConverter.class)
  public void setVariables(List<VariableQueryParameterDto> variables) {
    this.variables = variables;
  }

  public Boolean isVariableNamesIgnoreCase() {
    return variableNamesIgnoreCase;
  }

  @OperatonQueryParam(value = "variableNamesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableNamesIgnoreCase(Boolean variableNamesCaseInsensitive) {
    this.variableNamesIgnoreCase = variableNamesCaseInsensitive;
  }

  public Boolean isVariableValuesIgnoreCase() {
    return variableValuesIgnoreCase;
  }

  @OperatonQueryParam(value ="variableValuesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableValuesIgnoreCase(Boolean variableValuesCaseInsensitive) {
    this.variableValuesIgnoreCase = variableValuesCaseInsensitive;
  }

  public Boolean isWithIncident() {
    return withIncident;
  }

  @OperatonQueryParam(value = "withIncident", converter = BooleanConverter.class)
  public void setWithIncident(Boolean withIncident) {
    this.withIncident = withIncident;
  }

  public String getIncidentId() {
    return incidentId;
  }

  @OperatonQueryParam("incidentId")
  public void setIncidentId(String incidentId) {
    this.incidentId = incidentId;
  }

  public String getRootProcessInstanceId() {
    return rootProcessInstanceId;
  }

  @OperatonQueryParam("rootProcessInstanceId")
  public void setRootProcessInstanceId(String rootProcessInstanceId) {
    this.rootProcessInstanceId = rootProcessInstanceId;
  }

  public String getIncidentType() {
    return incidentType;
  }

  @OperatonQueryParam("incidentType")
  public void setIncidentType(String incidentType) {
    this.incidentType = incidentType;
  }

  public String getIncidentMessage() {
    return incidentMessage;
  }

  @OperatonQueryParam("incidentMessage")
  public void setIncidentMessage(String incidentMessage) {
    this.incidentMessage = incidentMessage;
  }

  public String getIncidentMessageLike() {
    return incidentMessageLike;
  }

  @OperatonQueryParam("incidentMessageLike")
  public void setIncidentMessageLike(String incidentMessageLike) {
    this.incidentMessageLike = incidentMessageLike;
  }

  public List<String> getTenantIdIn() {
    return tenantIds;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
  }

  public Boolean isWithoutTenantId() {
    return withoutTenantId;
  }

  @OperatonQueryParam(value = "withoutTenantId", converter = BooleanConverter.class)
  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  public List<String> getActivityIds() {
    return activityIds;
  }

  @OperatonQueryParam(value = "activityIdIn", converter = StringListConverter.class)
  public void setActivityIdIn(List<String> activityIds) {
    this.activityIds = activityIds;
  }

  public Boolean isRootProcessInstances() {
    return rootProcessInstances;
  }

  @OperatonQueryParam(value = "rootProcessInstances", converter = BooleanConverter.class)
  public void setRootProcessInstances(Boolean rootProcessInstances) {
    this.rootProcessInstances = rootProcessInstances;
  }


  public Boolean isLeafProcessInstances() {
    return leafProcessInstances;
  }

  @OperatonQueryParam(value = "leafProcessInstances", converter = BooleanConverter.class)
  public void setLeafProcessInstances(Boolean leafProcessInstances) {
    this.leafProcessInstances = leafProcessInstances;
  }

  public Boolean isProcessDefinitionWithoutTenantId() {
    return isProcessDefinitionWithoutTenantId;
  }

  @OperatonQueryParam(value = "processDefinitionWithoutTenantId", converter = BooleanConverter.class)
  public void setProcessDefinitionWithoutTenantId(Boolean isProcessDefinitionWithoutTenantId) {
    this.isProcessDefinitionWithoutTenantId = isProcessDefinitionWithoutTenantId;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected ProcessInstanceQuery createNewQuery(ProcessEngine engine) {
    return engine.getRuntimeService().createProcessInstanceQuery();
  }

  public List<ProcessInstanceQueryDto> getOrQueries() {
    return orQueries;
  }

  @Override
  protected void applyFilters(ProcessInstanceQuery query) {
    if (orQueries != null) {
      for (ProcessInstanceQueryDto orQueryDto: orQueries) {
        ProcessInstanceQueryImpl orQuery = new ProcessInstanceQueryImpl();
        orQuery.setOrQueryActive();
        orQueryDto.applyFilters(orQuery);
        ((ProcessInstanceQueryImpl) query).addOrQuery(orQuery);
      }
    }
    if (processInstanceIds != null) {
      query.processInstanceIds(processInstanceIds);
    }
    if (processDefinitionKey != null) {
      query.processDefinitionKey(processDefinitionKey);
    }
    if (processDefinitionKeys != null && !processDefinitionKeys.isEmpty()) {
      query.processDefinitionKeyIn(processDefinitionKeys.toArray(new String[processDefinitionKeys.size()]));
    }
    if (processDefinitionKeyNotIn != null && !processDefinitionKeyNotIn.isEmpty()) {
      query.processDefinitionKeyNotIn(processDefinitionKeyNotIn.toArray(new String[processDefinitionKeyNotIn.size()]));
    }
    if (deploymentId != null) {
      query.deploymentId(deploymentId);
    }
    if (businessKey != null) {
      query.processInstanceBusinessKey(businessKey);
    }
    if (businessKeyLike != null) {
      query.processInstanceBusinessKeyLike(businessKeyLike);
    }
    if(TRUE.equals(withIncident)) {
      query.withIncident();
    }
    if (caseInstanceId != null) {
      query.caseInstanceId(caseInstanceId);
    }
    if (processDefinitionId != null) {
      query.processDefinitionId(processDefinitionId);
    }
    if (superProcessInstance != null) {
      query.superProcessInstanceId(superProcessInstance);
    }
    if (subProcessInstance != null) {
      query.subProcessInstanceId(subProcessInstance);
    }
    if (superCaseInstance != null) {
      query.superCaseInstanceId(superCaseInstance);
    }
    if (subCaseInstance != null) {
      query.subCaseInstanceId(subCaseInstance);
    }
    if (TRUE.equals(active)) {
      query.active();
    }
    if (TRUE.equals(suspended)) {
      query.suspended();
    }
    if (incidentId != null) {
      query.incidentId(incidentId);
    }
    if (rootProcessInstanceId != null) {
      query.rootProcessInstanceId(rootProcessInstanceId);
    }
    if (incidentType != null) {
      query.incidentType(incidentType);
    }
    if (incidentMessage != null) {
      query.incidentMessage(incidentMessage);
    }
    if (incidentMessageLike != null) {
      query.incidentMessageLike(incidentMessageLike);
    }
    if (tenantIds != null && !tenantIds.isEmpty()) {
      query.tenantIdIn(tenantIds.toArray(new String[tenantIds.size()]));
    }
    if (TRUE.equals(withoutTenantId)) {
      query.withoutTenantId();
    }
    if (activityIds != null && !activityIds.isEmpty()) {
      query.activityIdIn(activityIds.toArray(new String[activityIds.size()]));
    }
    if (TRUE.equals(rootProcessInstances)) {
      query.rootProcessInstances();
    }
    if(TRUE.equals(leafProcessInstances)) {
      query.leafProcessInstances();
    }
    if (TRUE.equals(isProcessDefinitionWithoutTenantId)) {
      query.processDefinitionWithoutTenantId();
    }
    if(TRUE.equals(variableNamesIgnoreCase)) {
      query.matchVariableNamesIgnoreCase();
    }
    if(TRUE.equals(variableValuesIgnoreCase)) {
      query.matchVariableValuesIgnoreCase();
    }
    if (variables != null) {
      for (VariableQueryParameterDto variableQueryParam : variables) {
        String variableName = variableQueryParam.getName();
        String op = variableQueryParam.getOperator();
        Object variableValue = variableQueryParam.resolveValue(objectMapper);

        if (EQUALS_OPERATOR_NAME.equals(op)) {
          query.variableValueEquals(variableName, variableValue);
        } else if (GREATER_THAN_OPERATOR_NAME.equals(op)) {
          query.variableValueGreaterThan(variableName, variableValue);
        } else if (GREATER_THAN_OR_EQUALS_OPERATOR_NAME.equals(op)) {
          query.variableValueGreaterThanOrEqual(variableName, variableValue);
        } else if (LESS_THAN_OPERATOR_NAME.equals(op)) {
          query.variableValueLessThan(variableName, variableValue);
        } else if (LESS_THAN_OR_EQUALS_OPERATOR_NAME.equals(op)) {
          query.variableValueLessThanOrEqual(variableName, variableValue);
        } else if (NOT_EQUALS_OPERATOR_NAME.equals(op)) {
          query.variableValueNotEquals(variableName, variableValue);
        } else if (LIKE_OPERATOR_NAME.equals(op)) {
          query.variableValueLike(variableName, String.valueOf(variableValue));
        } else {
          throw new InvalidRequestException(Status.BAD_REQUEST, "Invalid variable comparator specified: %s".formatted(op));
        }
      }
    }
  }

  @Override
  protected void applySortBy(ProcessInstanceQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (SORT_BY_INSTANCE_ID_VALUE.equals(sortBy)) {
      query.orderByProcessInstanceId();
    } else if (SORT_BY_DEFINITION_KEY_VALUE.equals(sortBy)) {
      query.orderByProcessDefinitionKey();
    } else if (SORT_BY_DEFINITION_ID_VALUE.equals(sortBy)) {
      query.orderByProcessDefinitionId();
    } else if (SORT_BY_TENANT_ID.equals(sortBy)) {
      query.orderByTenantId();
    } else if (SORT_BY_BUSINESS_KEY.equals(sortBy)) {
      query.orderByBusinessKey();
    }
  }

}
