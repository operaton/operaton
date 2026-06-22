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
import org.operaton.bpm.engine.runtime.CaseInstanceQuery;

import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.*;
import static java.lang.Boolean.TRUE;

/**
 * @author Roman Smirnov
 *
 */
public class CaseInstanceQueryDto extends AbstractQueryDto<CaseInstanceQuery> {

  protected static final String SORT_BY_INSTANCE_ID_VALUE = "caseInstanceId";
  protected static final String SORT_BY_DEFINITION_KEY_VALUE = "caseDefinitionKey";
  protected static final String SORT_BY_DEFINITION_ID_VALUE = "caseDefinitionId";
  protected static final String SORT_BY_TENANT_ID = "tenantId";

  protected static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_INSTANCE_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_DEFINITION_KEY_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_DEFINITION_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
  }

  protected String caseInstanceId;
  protected String businessKey;
  protected String caseDefinitionKey;
  protected String caseDefinitionId;
  protected String deploymentId;
  protected String superProcessInstance;
  protected String subProcessInstance;
  protected String superCaseInstance;
  protected String subCaseInstance;
  protected List<String> tenantIds;
  protected Boolean withoutTenantId;
  protected Boolean active;
  protected Boolean completed;
  protected Boolean terminated;

  protected List<VariableQueryParameterDto> variables;

  protected Boolean variableNamesIgnoreCase;
  protected Boolean variableValuesIgnoreCase;

  public CaseInstanceQueryDto() {
  }

  public CaseInstanceQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("caseInstanceId")
  public void setCaseInstanceId(String caseInstanceId) {
    this.caseInstanceId = caseInstanceId;
  }

  @OperatonQueryParam("businessKey")
  public void setBusinessKey(String businessKey) {
    this.businessKey = businessKey;
  }

  @OperatonQueryParam("caseDefinitionKey")
  public void setCaseDefinitionKey(String caseDefinitionKey) {
    this.caseDefinitionKey = caseDefinitionKey;
  }

  @OperatonQueryParam("caseDefinitionId")
  public void setCaseDefinitionId(String caseDefinitionId) {
    this.caseDefinitionId = caseDefinitionId;
  }

  @OperatonQueryParam("deploymentId")
  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  @OperatonQueryParam("superProcessInstance")
  public void setSuperProcessInstance(String superProcessInstance) {
    this.superProcessInstance = superProcessInstance;
  }

  @OperatonQueryParam("subProcessInstance")
  public void setSubProcessInstance(String subProcessInstance) {
    this.subProcessInstance = subProcessInstance;
  }

  @OperatonQueryParam("superCaseInstance")
  public void setSuperCaseInstance(String superCaseInstance) {
    this.superCaseInstance = superCaseInstance;
  }

  @OperatonQueryParam("subCaseInstance")
  public void setSubCaseInstance(String subCaseInstance) {
    this.subCaseInstance = subCaseInstance;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
  }

  @OperatonQueryParam(value = "withoutTenantId", converter = BooleanConverter.class)
  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  @OperatonQueryParam(value = "active", converter = BooleanConverter.class)
  public void setActive(Boolean active) {
    this.active = active;
  }

  @OperatonQueryParam(value = "completed", converter = BooleanConverter.class)
  public void setCompleted(Boolean completed) {
    this.completed = completed;
  }

  @OperatonQueryParam(value = "terminated", converter = BooleanConverter.class)
  public void setTerminated(Boolean terminated) {
    this.terminated = terminated;
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

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected CaseInstanceQuery createNewQuery(ProcessEngine engine) {
    return engine.getCaseService().createCaseInstanceQuery();
  }

  @Override
  protected void applyFilters(CaseInstanceQuery query) {
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
    if (deploymentId != null) {
      query.deploymentId(deploymentId );
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
    if (tenantIds != null && !tenantIds.isEmpty()) {
      query.tenantIdIn(tenantIds.toArray(new String[tenantIds.size()]));
    }
    if (TRUE.equals(withoutTenantId)) {
      query.withoutTenantId();
    }
    if (TRUE.equals(active)) {
      query.active();
    }
    if (TRUE.equals(completed)) {
      query.completed();
    }
    if (TRUE.equals(terminated)) {
      query.terminated();
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
  }

  @Override
  protected void applySortBy(CaseInstanceQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    switch (sortBy) {
    case SORT_BY_INSTANCE_ID_VALUE -> query.orderByCaseInstanceId();
    case SORT_BY_DEFINITION_KEY_VALUE -> query.orderByCaseDefinitionKey();
    case SORT_BY_DEFINITION_ID_VALUE -> query.orderByCaseDefinitionId();
    case SORT_BY_TENANT_ID -> query.orderByTenantId();
    default -> throw new InvalidRequestException(Status.BAD_REQUEST, "Invalid sort operator specified: %s".formatted(sortBy));
    }
  }

}
