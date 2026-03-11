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
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.VariableQueryParameterDto;
import org.operaton.bpm.engine.rest.dto.converter.BooleanConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringListConverter;
import org.operaton.bpm.engine.rest.dto.converter.VariableListConverter;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.runtime.ExecutionQuery;

import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.*;
import static java.lang.Boolean.TRUE;

public class ExecutionQueryDto extends AbstractQueryDto<ExecutionQuery> {

  private static final String SORT_BY_INSTANCE_ID_VALUE = "instanceId";
  private static final String SORT_BY_DEFINITION_KEY_VALUE = "definitionKey";
  private static final String SORT_BY_DEFINITION_ID_VALUE = "definitionId";
  private static final String SORT_BY_TENANT_ID = "tenantId";

  private static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_INSTANCE_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_DEFINITION_KEY_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_DEFINITION_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
  }

  private String processDefinitionKey;
  private String businessKey;
  private String processDefinitionId;
  private String processInstanceId;
  private String activityId;
  private String signalEventSubscriptionName;
  private String messageEventSubscriptionName;
  private Boolean active;
  private Boolean suspended;
  private String incidentId;
  private String incidentType;
  private String incidentMessage;
  private String incidentMessageLike;
  private List<String> tenantIdIn;

  private List<VariableQueryParameterDto> variables;
  private List<VariableQueryParameterDto> processVariables;

  Boolean variableValuesIgnoreCase;
  Boolean variableNamesIgnoreCase;

  public ExecutionQueryDto() {

  }

  public ExecutionQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("processDefinitionKey")
  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  @OperatonQueryParam("businessKey")
  public void setBusinessKey(String businessKey) {
    this.businessKey = businessKey;
  }

  @OperatonQueryParam("processDefinitionId")
  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  @OperatonQueryParam("processInstanceId")
  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  @OperatonQueryParam("activityId")
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  @OperatonQueryParam("signalEventSubscriptionName")
  public void setSignalEventSubscriptionName(String signalEventSubscriptionName) {
    this.signalEventSubscriptionName = signalEventSubscriptionName;
  }

  @OperatonQueryParam("messageEventSubscriptionName")
  public void setMessageEventSubscriptionName(String messageEventSubscriptionName) {
    this.messageEventSubscriptionName = messageEventSubscriptionName;
  }

  @OperatonQueryParam(value = "variables", converter = VariableListConverter.class)
  public void setVariables(List<VariableQueryParameterDto> variables) {
    this.variables = variables;
  }

  @OperatonQueryParam(value = "processVariables", converter = VariableListConverter.class)
  public void setProcessVariables(List<VariableQueryParameterDto> processVariables) {
    this.processVariables = processVariables;
  }

  @OperatonQueryParam(value = "variableValuesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableValuesIgnoreCase(Boolean variableValuesIgnoreCase) {
    this.variableValuesIgnoreCase = variableValuesIgnoreCase;
  }

  @OperatonQueryParam(value = "variableNamesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableNamesIgnoreCase(Boolean variableNamesIgnoreCase) {
    this.variableNamesIgnoreCase = variableNamesIgnoreCase;
  }

  @OperatonQueryParam(value = "active", converter = BooleanConverter.class)
  public void setActive(Boolean active) {
    this.active = active;
  }

  @OperatonQueryParam(value = "suspended", converter = BooleanConverter.class)
  public void setSuspended(Boolean suspended) {
    this.suspended = suspended;
  }

  @OperatonQueryParam("incidentId")
  public void setIncidentId(String incidentId) {
    this.incidentId = incidentId;
  }

  @OperatonQueryParam("incidentType")
  public void setIncidentType(String incidentType) {
    this.incidentType = incidentType;
  }

  @OperatonQueryParam("incidentMessage")
  public void setIncidentMessage(String incidentMessage) {
    this.incidentMessage = incidentMessage;
  }

  @OperatonQueryParam("incidentMessageLike")
  public void setIncidentMessageLike(String incidentMessageLike) {
    this.incidentMessageLike = incidentMessageLike;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIdIn) {
    this.tenantIdIn = tenantIdIn;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected ExecutionQuery createNewQuery(ProcessEngine engine) {
    return engine.getRuntimeService().createExecutionQuery();
  }

  @Override
  protected void applyFilters(ExecutionQuery query) {
    if (processDefinitionKey != null) {
      query.processDefinitionKey(processDefinitionKey);
    }
    if (businessKey != null) {
      query.processInstanceBusinessKey(businessKey);
    }
    if (processDefinitionId != null) {
      query.processDefinitionId(processDefinitionId);
    }
    if (processInstanceId != null) {
      query.processInstanceId(processInstanceId);
    }
    if (activityId != null) {
      query.activityId(activityId);
    }
    if (signalEventSubscriptionName != null) {
      query.signalEventSubscriptionName(signalEventSubscriptionName);
    }
    if (messageEventSubscriptionName != null) {
      query.messageEventSubscriptionName(messageEventSubscriptionName);
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
    if (incidentType != null) {
      query.incidentType(incidentType);
    }
    if (incidentMessage != null) {
      query.incidentMessage(incidentMessage);
    }
    if (incidentMessageLike != null) {
      query.incidentMessageLike(incidentMessageLike);
    }
    if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
      query.tenantIdIn(tenantIdIn.toArray(new String[tenantIdIn.size()]));
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

        switch (op) {
        case EQUALS_OPERATOR_NAME -> query.variableValueEquals(variableName, variableValue);
        case GREATER_THAN_OPERATOR_NAME -> query.variableValueGreaterThan(variableName, variableValue);
        case GREATER_THAN_OR_EQUALS_OPERATOR_NAME -> query.variableValueGreaterThanOrEqual(variableName, variableValue);
        case LESS_THAN_OPERATOR_NAME -> query.variableValueLessThan(variableName, variableValue);
        case LESS_THAN_OR_EQUALS_OPERATOR_NAME -> query.variableValueLessThanOrEqual(variableName, variableValue);
        case NOT_EQUALS_OPERATOR_NAME -> query.variableValueNotEquals(variableName, variableValue);
        case LIKE_OPERATOR_NAME -> query.variableValueLike(variableName, String.valueOf(variableValue));
        default ->
          throw new InvalidRequestException(Status.BAD_REQUEST, "Invalid variable comparator specified: %s".formatted(op));
        }
      }
    }

    if (processVariables != null) {
      for (VariableQueryParameterDto variableQueryParam : processVariables) {
        String variableName = variableQueryParam.getName();
        String op = variableQueryParam.getOperator();
        Object variableValue = variableQueryParam.resolveValue(objectMapper);

        if (EQUALS_OPERATOR_NAME.equals(op)) {
          query.processVariableValueEquals(variableName, variableValue);
        } else if (NOT_EQUALS_OPERATOR_NAME.equals(op)) {
          query.processVariableValueNotEquals(variableName, variableValue);
        } else {
          throw new InvalidRequestException(Status.BAD_REQUEST, "Invalid process variable comparator specified: %s".formatted(op));
        }
      }
    }
  }

  @Override
  protected void applySortBy(ExecutionQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    switch (sortBy) {
    case SORT_BY_INSTANCE_ID_VALUE -> query.orderByProcessInstanceId();
    case SORT_BY_DEFINITION_KEY_VALUE -> query.orderByProcessDefinitionKey();
    case SORT_BY_DEFINITION_ID_VALUE -> query.orderByProcessDefinitionId();
    case SORT_BY_TENANT_ID -> query.orderByTenantId();
    default -> throw new InvalidRequestException(Status.BAD_REQUEST, "Invalid sort operator specified: %s".formatted(sortBy));
    }
  }
}
