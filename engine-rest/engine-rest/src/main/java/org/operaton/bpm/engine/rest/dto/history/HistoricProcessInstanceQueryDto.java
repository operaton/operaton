/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.rest.dto.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.Status;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.HistoricProcessInstanceQueryImpl;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.VariableQueryParameterDto;
import org.operaton.bpm.engine.rest.dto.converter.BooleanConverter;
import org.operaton.bpm.engine.rest.dto.converter.DateConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringListConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringSetConverter;
import org.operaton.bpm.engine.rest.dto.converter.VariableListConverter;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;

import static java.lang.Boolean.TRUE;
import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.*;

public class HistoricProcessInstanceQueryDto extends AbstractQueryDto<HistoricProcessInstanceQuery> {

  private static final String SORT_BY_PROCESS_INSTANCE_ID_VALUE = "instanceId";
  private static final String SORT_BY_PROCESS_DEFINITION_ID_VALUE = "definitionId";
  private static final String SORT_BY_PROCESS_INSTANCE_BUSINESS_KEY_VALUE = "businessKey";
  private static final String SORT_BY_PROCESS_INSTANCE_START_TIME_VALUE = "startTime";
  private static final String SORT_BY_PROCESS_INSTANCE_END_TIME_VALUE = "endTime";
  private static final String SORT_BY_PROCESS_INSTANCE_DURATION_VALUE = "duration";
  private static final String SORT_BY_PROCESS_DEFINITION_KEY_VALUE = "definitionKey";
  private static final String SORT_BY_PROCESS_DEFINITION_NAME_VALUE = "definitionName";
  private static final String SORT_BY_PROCESS_DEFINITION_VERSION_VALUE = "definitionVersion";

  private static final String SORT_BY_TENANT_ID = "tenantId";

  private static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_INSTANCE_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_DEFINITION_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_INSTANCE_BUSINESS_KEY_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_INSTANCE_START_TIME_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_INSTANCE_END_TIME_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_INSTANCE_DURATION_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_DEFINITION_KEY_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_DEFINITION_NAME_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_DEFINITION_VERSION_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
  }

  private String processInstanceId;
  private Set<String> processInstanceIds;
  private List<String> processInstanceIdNotIn;
  private String processDefinitionId;
  private String processDefinitionKey;
  private List<String> processDefinitionKeys;
  private String processDefinitionName;
  private String processDefinitionNameLike;
  private List<String> processDefinitionKeyNotIn;
  private String processInstanceBusinessKey;
  private List<String> processInstanceBusinessKeyIn;
  private String processInstanceBusinessKeyLike;
  private Boolean rootProcessInstances;
  private Boolean finished;
  private Boolean unfinished;
  private Boolean withIncidents;
  private Boolean withRootIncidents;
  private String incidentType;
  private String incidentStatus;
  private String incidentMessage;
  private String incidentMessageLike;
  private Date startedBefore;
  private Date startedAfter;
  private Date finishedBefore;
  private Date finishedAfter;
  private Date executedActivityAfter;
  private Date executedActivityBefore;
  private Date executedJobAfter;
  private Date executedJobBefore;
  private String startedBy;
  private String superProcessInstanceId;
  private String subProcessInstanceId;
  private String superCaseInstanceId;
  private String subCaseInstanceId;
  private String caseInstanceId;
  private List<String> tenantIds;
  private Boolean withoutTenantId;
  private List<String> executedActivityIdIn;
  private List<String> activeActivityIdIn;
  private List<String> activityIdIn;
  private Boolean active;
  private Boolean suspended;
  private Boolean completed;
  private Boolean externallyTerminated;
  private Boolean internallyTerminated;
  private List<String> incidentIds;

  private List<VariableQueryParameterDto> variables;

  protected Boolean variableNamesIgnoreCase;
  protected Boolean variableValuesIgnoreCase;

  private List<HistoricProcessInstanceQueryDto> orQueries;

  public HistoricProcessInstanceQueryDto() {}

  public HistoricProcessInstanceQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("orQueries")
  public void setOrQueries(List<HistoricProcessInstanceQueryDto> orQueries) {
    this.orQueries = orQueries;
  }

  @OperatonQueryParam("processInstanceId")
  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  @OperatonQueryParam(value = "processInstanceIds", converter = StringSetConverter.class)
  public void setProcessInstanceIds(Set<String> processInstanceIds) {
    this.processInstanceIds = processInstanceIds;
  }

  @OperatonQueryParam(value = "processInstanceIdNotIn", converter = StringListConverter.class)
  public void setProcessInstanceIdNotIn(List<String> processInstanceIdNotIn) {
    this.processInstanceIdNotIn = processInstanceIdNotIn;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  @OperatonQueryParam("processDefinitionId")
  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  @OperatonQueryParam("processDefinitionName")
  public void setProcessDefinitionName(String processDefinitionName) {
    this.processDefinitionName = processDefinitionName;
  }

  @OperatonQueryParam("processDefinitionNameLike")
  public void setProcessDefinitionNameLike(String processDefinitionNameLike) {
    this.processDefinitionNameLike = processDefinitionNameLike;
  }

  @OperatonQueryParam("processDefinitionKey")
  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  @OperatonQueryParam(value = "processDefinitionKeyIn", converter = StringListConverter.class)
  public void setProcessDefinitionKeyIn(List<String> processDefinitionKeys) {
    this.processDefinitionKeys = processDefinitionKeys;
  }

  @OperatonQueryParam(value = "processDefinitionKeyNotIn", converter = StringListConverter.class)
  public void setProcessDefinitionKeyNotIn(List<String> processDefinitionKeys) {
    this.processDefinitionKeyNotIn = processDefinitionKeys;
  }

  @OperatonQueryParam("processInstanceBusinessKey")
  public void setProcessInstanceBusinessKey(String processInstanceBusinessKey) {
    this.processInstanceBusinessKey = processInstanceBusinessKey;
  }

  @OperatonQueryParam(value = "processInstanceBusinessKeyIn", converter = StringListConverter.class)
  public void setProcessInstanceBusinessKeyIn(List<String> processInstanceBusinessKeyIn) {
    this.processInstanceBusinessKeyIn = processInstanceBusinessKeyIn;
  }

  @OperatonQueryParam("processInstanceBusinessKeyLike")
  public void setProcessInstanceBusinessKeyLike(String processInstanceBusinessKeyLike) {
    this.processInstanceBusinessKeyLike = processInstanceBusinessKeyLike;
  }

  @OperatonQueryParam(value = "rootProcessInstances", converter = BooleanConverter.class)
  public void setRootProcessInstances(Boolean rootProcessInstances) {
    this.rootProcessInstances = rootProcessInstances;
  }

  @OperatonQueryParam(value = "finished", converter = BooleanConverter.class)
  public void setFinished(Boolean finished) {
    this.finished = finished;
  }

  @OperatonQueryParam(value = "unfinished", converter = BooleanConverter.class)
  public void setUnfinished(Boolean unfinished) {
    this.unfinished = unfinished;
  }

  @OperatonQueryParam(value = "withIncidents", converter = BooleanConverter.class)
  public void setWithIncidents(Boolean withIncidents) {
    this.withIncidents = withIncidents;
  }

  @OperatonQueryParam(value = "withRootIncidents", converter = BooleanConverter.class)
  public void setWithRootIncidents(Boolean withRootIncidents) {
    this.withRootIncidents = withRootIncidents;
  }

  @OperatonQueryParam(value = "incidentStatus")
  public void setIncidentStatus(String status) {
    this.incidentStatus = status;
  }

  @OperatonQueryParam(value = "incidentMessage")
  public void setIncidentMessage(String incidentMessage) {
    this.incidentMessage = incidentMessage;
  }

  @OperatonQueryParam(value = "incidentMessageLike")
  public void setIncidentMessageLike(String incidentMessageLike) {
    this.incidentMessageLike = incidentMessageLike;
  }

  @OperatonQueryParam(value = "startedBefore", converter = DateConverter.class)
  public void setStartedBefore(Date startedBefore) {
    this.startedBefore = startedBefore;
  }

  @OperatonQueryParam(value = "startedAfter", converter = DateConverter.class)
  public void setStartedAfter(Date startedAfter) {
    this.startedAfter = startedAfter;
  }

  @OperatonQueryParam(value = "finishedBefore", converter = DateConverter.class)
  public void setFinishedBefore(Date finishedBefore) {
    this.finishedBefore = finishedBefore;
  }

  @OperatonQueryParam(value = "finishedAfter", converter = DateConverter.class)
  public void setFinishedAfter(Date finishedAfter) {
    this.finishedAfter = finishedAfter;
  }

  @OperatonQueryParam("startedBy")
  public void setStartedBy(String startedBy) {
    this.startedBy = startedBy;
  }

  @OperatonQueryParam("superProcessInstanceId")
  public void setSuperProcessInstanceId(String superProcessInstanceId) {
    this.superProcessInstanceId = superProcessInstanceId;
  }

  @OperatonQueryParam("subProcessInstanceId")
  public void setSubProcessInstanceId(String subProcessInstanceId) {
    this.subProcessInstanceId = subProcessInstanceId;
  }

  @OperatonQueryParam("superCaseInstanceId")
  public void setSuperCaseInstanceId(String superCaseInstanceId) {
    this.superCaseInstanceId = superCaseInstanceId;
  }

  @OperatonQueryParam("subCaseInstanceId")
  public void setSubCaseInstanceId(String subCaseInstanceId) {
    this.subCaseInstanceId = subCaseInstanceId;
  }

  @OperatonQueryParam("caseInstanceId")
  public void setCaseInstanceId(String caseInstanceId) {
    this.caseInstanceId = caseInstanceId;
  }

  @OperatonQueryParam(value = "variables", converter = VariableListConverter.class)
  public void setVariables(List<VariableQueryParameterDto> variables) {
    this.variables = variables;
  }

  @OperatonQueryParam(value = "variableNamesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableNamesIgnoreCase(Boolean variableNamesIgnoreCase) {
    this.variableNamesIgnoreCase = variableNamesIgnoreCase;
  }

  @OperatonQueryParam(value = "variableValuesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableValuesIgnoreCase(Boolean variableValuesIgnoreCase) {
    this.variableValuesIgnoreCase = variableValuesIgnoreCase;
  }

  public String getIncidentType() {
    return incidentType;
  }

  @OperatonQueryParam(value = "incidentType")
  public void setIncidentType(String incidentType) {
    this.incidentType = incidentType;
  }

  @OperatonQueryParam(value = "incidentIdIn", converter = StringListConverter.class)
  public void setIncidentIdIn(List<String> incidentIds) {
    this.incidentIds = incidentIds;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
  }

  @OperatonQueryParam(value = "withoutTenantId", converter = BooleanConverter.class)
  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  @OperatonQueryParam(value = "executedActivityAfter", converter = DateConverter.class)
  public void setExecutedActivityAfter(Date executedActivityAfter) {
    this.executedActivityAfter = executedActivityAfter;
  }

  @OperatonQueryParam(value = "executedActivityIdIn", converter = StringListConverter.class)
  public void setExecutedActivityIdIn(List<String> executedActivityIds) {
    this.executedActivityIdIn = executedActivityIds;
  }

  @OperatonQueryParam(value = "executedActivityBefore", converter = DateConverter.class)
  public void setExecutedActivityBefore(Date executedActivityBefore) {
    this.executedActivityBefore = executedActivityBefore;
  }

  @OperatonQueryParam(value = "activeActivityIdIn", converter = StringListConverter.class)
  public void setActiveActivityIdIn(List<String> activeActivityIdIn) {
    this.activeActivityIdIn = activeActivityIdIn;
  }

  @OperatonQueryParam(value = "activityIdIn", converter = StringListConverter.class)
  public void setActivityIdIn(List<String> activityIdIn) {
    this.activityIdIn = activityIdIn;
  }

  @OperatonQueryParam(value = "executedJobAfter", converter = DateConverter.class)
  public void setExecutedJobAfter(Date executedJobAfter) {
    this.executedJobAfter = executedJobAfter;
  }

  @OperatonQueryParam(value = "executedJobBefore", converter = DateConverter.class)
  public void setExecutedJobBefore(Date executedJobBefore) {
    this.executedJobBefore = executedJobBefore;
  }

  @OperatonQueryParam(value = "active", converter = BooleanConverter.class)
  public void setActive(Boolean active) {
    this.active = active;
  }

  @OperatonQueryParam(value = "suspended", converter = BooleanConverter.class)
  public void setSuspended(Boolean suspended) {
    this.suspended = suspended;
  }

  @OperatonQueryParam(value = "completed", converter = BooleanConverter.class)
  public void setCompleted(Boolean completed) {
    this.completed = completed;
  }

  @OperatonQueryParam(value = "externallyTerminated", converter = BooleanConverter.class)
  public void setExternallyTerminated(Boolean externallyTerminated) {
    this.externallyTerminated = externallyTerminated;
  }

  @OperatonQueryParam(value = "internallyTerminated", converter = BooleanConverter.class)
  public void setInternallyTerminated(Boolean internallyTerminated) {
    this.internallyTerminated = internallyTerminated;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected HistoricProcessInstanceQuery createNewQuery(ProcessEngine engine) {
    return engine.getHistoryService().createHistoricProcessInstanceQuery();
  }

  public List<HistoricProcessInstanceQueryDto> getOrQueries() {
    return orQueries;
  }

  @Override
  protected void applyFilters(HistoricProcessInstanceQuery query) {
    if (orQueries != null) {
      for (HistoricProcessInstanceQueryDto orQueryDto: orQueries) {
        HistoricProcessInstanceQueryImpl orQuery = new HistoricProcessInstanceQueryImpl();
        orQuery.setOrQueryActive();
        orQueryDto.applyFilters(orQuery);
        ((HistoricProcessInstanceQueryImpl) query).addOrQuery(orQuery);
      }
    }
    if (processInstanceId != null) {
      query.processInstanceId(processInstanceId);
    }
    if (processInstanceIds != null) {
      query.processInstanceIds(processInstanceIds);
    }
    if (processInstanceIdNotIn != null && !processInstanceIdNotIn.isEmpty()) {
      query.processInstanceIdNotIn(processInstanceIdNotIn.toArray(new String[0]));
    }
    if (processDefinitionId != null) {
      query.processDefinitionId(processDefinitionId);
    }
    if (processDefinitionKey != null) {
      query.processDefinitionKey(processDefinitionKey);
    }
    if (processDefinitionKeys != null && !processDefinitionKeys.isEmpty()) {
      query.processDefinitionKeyIn(processDefinitionKeys.toArray(new String[0]));
    }
    if (processDefinitionName != null) {
      query.processDefinitionName(processDefinitionName);
    }
    if (processDefinitionNameLike != null) {
      query.processDefinitionNameLike(processDefinitionNameLike);
    }
    if (processDefinitionKeyNotIn != null) {
      query.processDefinitionKeyNotIn(processDefinitionKeyNotIn);
    }
    if (processInstanceBusinessKey != null) {
      query.processInstanceBusinessKey(processInstanceBusinessKey);
    }
    if (processInstanceBusinessKeyIn != null && !processInstanceBusinessKeyIn.isEmpty()) {
      query.processInstanceBusinessKeyIn(processInstanceBusinessKeyIn.toArray(new String[0]));
    }
    if (processInstanceBusinessKeyLike != null) {
      query.processInstanceBusinessKeyLike(processInstanceBusinessKeyLike);
    }
    if (rootProcessInstances != null && rootProcessInstances) {
      query.rootProcessInstances();
    }
    if (finished != null && finished) {
      query.finished();
    }
    if (unfinished != null && unfinished) {
      query.unfinished();
    }
    if (withIncidents != null && withIncidents) {
      query.withIncidents();
    }
    if (withRootIncidents != null && withRootIncidents) {
      query.withRootIncidents();
    }
    if (incidentIds != null && !incidentIds.isEmpty()) {
      query.incidentIdIn(incidentIds.toArray(new String[0]));
    }
    if (incidentStatus != null) {
      query.incidentStatus(incidentStatus);
    }
    if (incidentType != null) {
      query.incidentType(incidentType);
    }
    if(incidentMessage != null) {
      query.incidentMessage(incidentMessage);
    }
    if(incidentMessageLike != null) {
      query.incidentMessageLike(incidentMessageLike);
    }
    if (startedBefore != null) {
      query.startedBefore(startedBefore);
    }
    if (startedAfter != null) {
      query.startedAfter(startedAfter);
    }
    if (finishedBefore != null) {
      query.finishedBefore(finishedBefore);
    }
    if (finishedAfter != null) {
      query.finishedAfter(finishedAfter);
    }
    if (startedBy != null) {
      query.startedBy(startedBy);
    }
    if (superProcessInstanceId != null) {
      query.superProcessInstanceId(superProcessInstanceId);
    }
    if (subProcessInstanceId != null) {
      query.subProcessInstanceId(subProcessInstanceId);
    }
    if (superCaseInstanceId != null) {
      query.superCaseInstanceId(superCaseInstanceId);
    }
    if (subCaseInstanceId != null) {
      query.subCaseInstanceId(subCaseInstanceId);
    }
    if (caseInstanceId != null) {
      query.caseInstanceId(caseInstanceId);
    }
    if (tenantIds != null && !tenantIds.isEmpty()) {
      query.tenantIdIn(tenantIds.toArray(new String[0]));
    }
    if (TRUE.equals(withoutTenantId)) {
      query.withoutTenantId();
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

        if (op.equals(EQUALS_OPERATOR_NAME)) {
          query.variableValueEquals(variableName, variableValue);
        } else if (op.equals(GREATER_THAN_OPERATOR_NAME)) {
          query.variableValueGreaterThan(variableName, variableValue);
        } else if (op.equals(GREATER_THAN_OR_EQUALS_OPERATOR_NAME)) {
          query.variableValueGreaterThanOrEqual(variableName, variableValue);
        } else if (op.equals(LESS_THAN_OPERATOR_NAME)) {
          query.variableValueLessThan(variableName, variableValue);
        } else if (op.equals(LESS_THAN_OR_EQUALS_OPERATOR_NAME)) {
          query.variableValueLessThanOrEqual(variableName, variableValue);
        } else if (op.equals(NOT_EQUALS_OPERATOR_NAME)) {
          query.variableValueNotEquals(variableName, variableValue);
        } else if (op.equals(LIKE_OPERATOR_NAME)) {
          query.variableValueLike(variableName, String.valueOf(variableValue));
        } else {
          throw new InvalidRequestException(Status.BAD_REQUEST, "Invalid variable comparator specified: " + op);
        }
      }
    }

    if (executedActivityAfter != null) {
      query.executedActivityAfter(executedActivityAfter);
    }

    if (executedActivityBefore != null) {
      query.executedActivityBefore(executedActivityBefore);
    }

    if (executedActivityIdIn != null && !executedActivityIdIn.isEmpty()) {
      query.executedActivityIdIn(executedActivityIdIn.toArray(new String[0]));
    }

    if (activeActivityIdIn != null && !activeActivityIdIn.isEmpty()) {
      query.activeActivityIdIn(activeActivityIdIn.toArray(new String[0]));
    }

    if(activityIdIn!= null && !activityIdIn.isEmpty()) {
      query.activityIdIn(activityIdIn.toArray(new String[0]));
    }

    if (executedJobAfter != null) {
      query.executedJobAfter(executedJobAfter);
    }

    if (executedJobBefore != null) {
      query.executedJobBefore(executedJobBefore);
    }

    if (active != null && active) {
      query.active();
    }
    if (suspended != null && suspended) {
      query.suspended();
    }
    if (completed != null && completed) {
      query.completed();
    }
    if (externallyTerminated != null && externallyTerminated) {
      query.externallyTerminated();
    }
    if (internallyTerminated != null && internallyTerminated) {
      query.internallyTerminated();
    }
  }

  @Override
  protected void applySortBy(HistoricProcessInstanceQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (sortBy.equals(SORT_BY_PROCESS_INSTANCE_ID_VALUE)) {
      query.orderByProcessInstanceId();
    } else if (sortBy.equals(SORT_BY_PROCESS_DEFINITION_ID_VALUE)) {
      query.orderByProcessDefinitionId();
    } else if (sortBy.equals(SORT_BY_PROCESS_DEFINITION_KEY_VALUE)) {
      query.orderByProcessDefinitionKey();
    } else if (sortBy.equals(SORT_BY_PROCESS_DEFINITION_NAME_VALUE)) {
      query.orderByProcessDefinitionName();
    } else if (sortBy.equals(SORT_BY_PROCESS_DEFINITION_VERSION_VALUE)) {
      query.orderByProcessDefinitionVersion();
    } else if (sortBy.equals(SORT_BY_PROCESS_INSTANCE_BUSINESS_KEY_VALUE)) {
      query.orderByProcessInstanceBusinessKey();
    } else if (sortBy.equals(SORT_BY_PROCESS_INSTANCE_START_TIME_VALUE)) {
      query.orderByProcessInstanceStartTime();
    } else if (sortBy.equals(SORT_BY_PROCESS_INSTANCE_END_TIME_VALUE)) {
      query.orderByProcessInstanceEndTime();
    } else if (sortBy.equals(SORT_BY_PROCESS_INSTANCE_DURATION_VALUE)) {
      query.orderByProcessInstanceDuration();
    } else if (sortBy.equals(SORT_BY_TENANT_ID)) {
      query.orderByTenantId();
    }
  }

}
