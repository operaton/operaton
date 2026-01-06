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
import org.operaton.bpm.engine.runtime.CaseExecutionQuery;

import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.*;
import static java.lang.Boolean.TRUE;

/**
 * @author Roman Smirnov
 *
 */
public class CaseExecutionQueryDto extends AbstractQueryDto<CaseExecutionQuery> {

  protected static final String SORT_BY_EXECUTION_ID_VALUE = "caseExecutionId";
  protected static final String SORT_BY_DEFINITION_KEY_VALUE = "caseDefinitionKey";
  protected static final String SORT_BY_DEFINITION_ID_VALUE = "caseDefinitionId";
  protected static final String SORT_BY_TENANT_ID = "tenantId";

  protected static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_EXECUTION_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_DEFINITION_KEY_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_DEFINITION_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
  }

  protected String caseExecutionId;
  protected String caseDefinitionKey;
  protected String caseDefinitionId;
  protected String caseInstanceId;
  protected String businessKey;
  protected String activityId;
  protected List<String> tenantIds;
  protected Boolean required;
  protected Boolean enabled;
  protected Boolean active;
  protected Boolean disabled;

  protected List<VariableQueryParameterDto> variables;
  protected List<VariableQueryParameterDto> caseInstanceVariables;

  protected Boolean variableNamesIgnoreCase;
  protected Boolean variableValuesIgnoreCase;

  public CaseExecutionQueryDto() {
  }

  public CaseExecutionQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("caseExecutionId")
  public void setCaseExecutionId(String caseExecutionId) {
    this.caseExecutionId = caseExecutionId;
  }

  @OperatonQueryParam("caseDefinitionKey")
  public void setCaseDefinitionKey(String caseDefinitionKey) {
    this.caseDefinitionKey = caseDefinitionKey;
  }

  @OperatonQueryParam("caseDefinitionId")
  public void setCaseDefinitionId(String caseDefinitionId) {
    this.caseDefinitionId = caseDefinitionId;
  }

  @OperatonQueryParam("caseInstanceId")
  public void setCaseInstanceId(String caseInstanceId) {
    this.caseInstanceId = caseInstanceId;
  }

  @OperatonQueryParam("businessKey")
  public void setBusinessKey(String businessKey) {
    this.businessKey = businessKey;
  }

  @OperatonQueryParam("activityId")
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
  }

  @OperatonQueryParam(value="required", converter = BooleanConverter.class)
  public void setRequired(Boolean required) {
    this.required = required;
  }

  @OperatonQueryParam(value="enabled", converter = BooleanConverter.class)
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  @OperatonQueryParam(value="active", converter = BooleanConverter.class)
  public void setActive(Boolean active) {
    this.active = active;
  }

  @OperatonQueryParam(value="disabled", converter = BooleanConverter.class)
  public void setDisabled(Boolean disabled) {
    this.disabled = disabled;
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

  @OperatonQueryParam(value = "caseInstanceVariables", converter = VariableListConverter.class)
  public void setCaseInstanceVariables(List<VariableQueryParameterDto> caseInstanceVariables) {
    this.caseInstanceVariables = caseInstanceVariables;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected CaseExecutionQuery createNewQuery(ProcessEngine engine) {
    return engine.getCaseService().createCaseExecutionQuery();
  }

  @Override
  protected void applyFilters(CaseExecutionQuery query) {
    if (caseExecutionId != null) {
      query.caseExecutionId(caseExecutionId);
    }

    if (caseInstanceId != null) {
      query.caseInstanceId(caseInstanceId);
    }

    if (businessKey != null) {
      query.caseInstanceBusinessKey(businessKey);
    }

    if (caseDefinitionKey != null) {
      query.caseDefinitionKey(caseDefinitionKey);
    }

    if (caseDefinitionId != null) {
      query.caseDefinitionId(caseDefinitionId);
    }

    if (activityId != null) {
      query.activityId(activityId);
    }

    if (tenantIds != null && !tenantIds.isEmpty()) {
      query.tenantIdIn(tenantIds.toArray(new String[tenantIds.size()]));
    }

    if (TRUE.equals(required)) {
      query.required();
    }

    if (TRUE.equals(active)) {
      query.active();
    }

    if (TRUE.equals(enabled)) {
      query.enabled();
    }

    if (TRUE.equals(disabled)) {
      query.disabled();
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

    if (caseInstanceVariables != null) {

      for (VariableQueryParameterDto variableQueryParam : caseInstanceVariables) {

        String variableName = variableQueryParam.getName();
        String op = variableQueryParam.getOperator();
        Object variableValue = variableQueryParam.resolveValue(objectMapper);

        switch (op) {
        case EQUALS_OPERATOR_NAME -> query.caseInstanceVariableValueEquals(variableName, variableValue);
        case GREATER_THAN_OPERATOR_NAME -> query.caseInstanceVariableValueGreaterThan(variableName, variableValue);
        case GREATER_THAN_OR_EQUALS_OPERATOR_NAME ->
          query.caseInstanceVariableValueGreaterThanOrEqual(variableName, variableValue);
        case LESS_THAN_OPERATOR_NAME -> query.caseInstanceVariableValueLessThan(variableName, variableValue);
        case LESS_THAN_OR_EQUALS_OPERATOR_NAME ->
          query.caseInstanceVariableValueLessThanOrEqual(variableName, variableValue);
        case NOT_EQUALS_OPERATOR_NAME -> query.caseInstanceVariableValueNotEquals(variableName, variableValue);
        case LIKE_OPERATOR_NAME -> query.caseInstanceVariableValueLike(variableName, String.valueOf(variableValue));
        default ->
          throw new InvalidRequestException(Status.BAD_REQUEST, "Invalid variable comparator specified: %s".formatted(op));
        }
      }
    }
  }

  protected void applySortBy(CaseExecutionQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    switch (sortBy) {
    case SORT_BY_EXECUTION_ID_VALUE -> query.orderByCaseExecutionId();
    case SORT_BY_DEFINITION_KEY_VALUE -> query.orderByCaseDefinitionKey();
    case SORT_BY_DEFINITION_ID_VALUE -> query.orderByCaseDefinitionId();
    case SORT_BY_TENANT_ID -> query.orderByTenantId();
    }
  }
}
