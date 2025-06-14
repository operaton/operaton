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
package org.operaton.bpm.engine.rest.dto.history;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricCaseInstanceQuery;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.VariableQueryParameterDto;
import org.operaton.bpm.engine.rest.dto.converter.*;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.*;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.Status;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import static java.lang.Boolean.TRUE;

public class HistoricCaseInstanceQueryDto extends AbstractQueryDto<HistoricCaseInstanceQuery> {

  public static final String SORT_BY_CASE_INSTANCE_ID_VALUE = "instanceId";
  public static final String SORT_BY_CASE_DEFINITION_ID_VALUE = "definitionId";
  public static final String SORT_BY_CASE_INSTANCE_BUSINESS_KEY_VALUE = "businessKey";
  public static final String SORT_BY_CASE_INSTANCE_CREATE_TIME_VALUE = "createTime";
  public static final String SORT_BY_CASE_INSTANCE_CLOSE_TIME_VALUE = "closeTime";
  public static final String SORT_BY_CASE_INSTANCE_DURATION_VALUE = "duration";
  private static final String SORT_BY_TENANT_ID = "tenantId";

  public static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_CASE_INSTANCE_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_CASE_DEFINITION_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_CASE_INSTANCE_BUSINESS_KEY_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_CASE_INSTANCE_CREATE_TIME_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_CASE_INSTANCE_CLOSE_TIME_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_CASE_INSTANCE_DURATION_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
  }

  public String caseInstanceId;
  public Set<String> caseInstanceIds;
  public String caseDefinitionId;
  public String caseDefinitionKey;
  public String caseDefinitionName;
  public String caseDefinitionNameLike;
  public List<String> caseDefinitionKeyNotIn;
  public String caseInstanceBusinessKey;
  public String caseInstanceBusinessKeyLike;
  public String superCaseInstanceId;
  public String subCaseInstanceId;
  private String superProcessInstanceId;
  private String subProcessInstanceId;
  private List<String> tenantIds;
  private Boolean withoutTenantId;
  public String createdBy;
  public List<String> caseActivityIdIn;

  public Date createdBefore;
  public Date createdAfter;
  public Date closedBefore;
  public Date closedAfter;

  public Boolean active;
  public Boolean completed;
  public Boolean terminated;
  public Boolean closed;
  public Boolean notClosed;

  protected List<VariableQueryParameterDto> variables;

  protected Boolean variableValuesIgnoreCase;
  protected Boolean variableNamesIgnoreCase;

  public HistoricCaseInstanceQueryDto() {}

  public HistoricCaseInstanceQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("caseInstanceId")
  public void setCaseInstanceId(String caseInstanceId) {
    this.caseInstanceId = caseInstanceId;
  }

  @OperatonQueryParam(value = "caseInstanceIds", converter = StringSetConverter.class)
  public void setCaseInstanceIds(Set<String> caseInstanceIds) {
    this.caseInstanceIds = caseInstanceIds;
  }

  @OperatonQueryParam("caseDefinitionId")
  public void setCaseDefinitionId(String caseDefinitionId) {
    this.caseDefinitionId = caseDefinitionId;
  }

  @OperatonQueryParam("caseDefinitionName")
  public void setCaseDefinitionName(String caseDefinitionName) {
    this.caseDefinitionName = caseDefinitionName;
  }

  @OperatonQueryParam("caseDefinitionNameLike")
  public void setCaseDefinitionNameLike(String caseDefinitionNameLike) {
    this.caseDefinitionNameLike = caseDefinitionNameLike;
  }

  @OperatonQueryParam("caseDefinitionKey")
  public void setCaseDefinitionKey(String caseDefinitionKey) {
    this.caseDefinitionKey = caseDefinitionKey;
  }

  @OperatonQueryParam(value = "caseDefinitionKeyNotIn", converter = StringListConverter.class)
  public void setCaseDefinitionKeyNotIn(List<String> caseDefinitionKeys) {
    this.caseDefinitionKeyNotIn = caseDefinitionKeys;
  }

  @OperatonQueryParam("caseInstanceBusinessKey")
  public void setCaseInstanceBusinessKey(String caseInstanceBusinessKey) {
    this.caseInstanceBusinessKey = caseInstanceBusinessKey;
  }

  @OperatonQueryParam("caseInstanceBusinessKeyLike")
  public void setCaseInstanceBusinessKeyLike(String caseInstanceBusinessKeyLike) {
    this.caseInstanceBusinessKeyLike = caseInstanceBusinessKeyLike;
  }

  @OperatonQueryParam("superCaseInstanceId")
  public void setSuperCaseInstanceId(String superCaseInstanceId) {
    this.superCaseInstanceId = superCaseInstanceId;
  }

  @OperatonQueryParam("subCaseInstanceId")
  public void setSubCaseInstanceId(String subCaseInstanceId) {
    this.subCaseInstanceId = subCaseInstanceId;
  }

  @OperatonQueryParam("superProcessInstanceId")
  public void setSuperProcessInstanceId(String superProcessInstanceId) {
    this.superProcessInstanceId = superProcessInstanceId;
  }

  @OperatonQueryParam("subProcessInstanceId")
  public void setSubProcessInstanceId(String subProcessInstanceId) {
    this.subProcessInstanceId = subProcessInstanceId;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
  }

  @OperatonQueryParam(value = "withoutTenantId", converter = BooleanConverter.class)
  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  @OperatonQueryParam("createdBy")
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  @OperatonQueryParam(value = "createdBefore", converter = DateConverter.class)
  public void setCreatedBefore(Date createdBefore) {
    this.createdBefore = createdBefore;
  }

  @OperatonQueryParam(value = "createdAfter", converter = DateConverter.class)
  public void setCreatedAfter(Date createdAfter) {
    this.createdAfter = createdAfter;
  }

  @OperatonQueryParam(value = "closedBefore", converter = DateConverter.class)
  public void setClosedBefore(Date closedBefore) {
    this.closedBefore = closedBefore;
  }

  @OperatonQueryParam(value = "closedAfter", converter = DateConverter.class)
  public void setClosedAfter(Date closedAfter) {
    this.closedAfter = closedAfter;
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

  @OperatonQueryParam(value = "closed", converter = BooleanConverter.class)
  public void setClosed(Boolean closed) {
    this.closed = closed;
  }

  @OperatonQueryParam(value = "notClosed", converter = BooleanConverter.class)
  public void setNotClosed(Boolean notClosed) {
    this.notClosed = notClosed;
  }

  @OperatonQueryParam(value = "variables", converter = VariableListConverter.class)
  public void setVariables(List<VariableQueryParameterDto> variables) {
    this.variables = variables;
  }

  @OperatonQueryParam(value = "variableValuesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableValuesIgnoreCase(Boolean variableValuesIgnoreCase) {
    this.variableValuesIgnoreCase = variableValuesIgnoreCase;
  }

  @OperatonQueryParam(value = "variableNamesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableNamesIgnoreCase(Boolean variableNamesIgnoreCase) {
    this.variableNamesIgnoreCase = variableNamesIgnoreCase;
  }

  @OperatonQueryParam(value = "caseActivityIdIn", converter = StringListConverter.class)
  public void setCaseActivityIdIn(List<String> caseActivityIdIn) {
    this.caseActivityIdIn = caseActivityIdIn;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected HistoricCaseInstanceQuery createNewQuery(ProcessEngine engine) {
    return engine.getHistoryService().createHistoricCaseInstanceQuery();
  }

  @Override
  protected void applyFilters(HistoricCaseInstanceQuery query) {

    if (caseInstanceId != null) {
      query.caseInstanceId(caseInstanceId);
    }
    if (caseInstanceIds != null) {
      query.caseInstanceIds(caseInstanceIds);
    }
    if (caseDefinitionId != null) {
      query.caseDefinitionId(caseDefinitionId);
    }
    if (caseDefinitionKey != null) {
      query.caseDefinitionKey(caseDefinitionKey);
    }
    if (caseDefinitionName != null) {
      query.caseDefinitionName(caseDefinitionName);
    }
    if (caseDefinitionNameLike != null) {
      query.caseDefinitionNameLike(caseDefinitionNameLike);
    }
    if (caseDefinitionKeyNotIn != null) {
      query.caseDefinitionKeyNotIn(caseDefinitionKeyNotIn);
    }
    if (caseInstanceBusinessKey != null) {
      query.caseInstanceBusinessKey(caseInstanceBusinessKey);
    }
    if (caseInstanceBusinessKeyLike != null) {
      query.caseInstanceBusinessKeyLike(caseInstanceBusinessKeyLike);
    }
    if (superCaseInstanceId != null) {
      query.superCaseInstanceId(superCaseInstanceId);
    }
    if (subCaseInstanceId != null) {
      query.subCaseInstanceId(subCaseInstanceId);
    }
    if (superProcessInstanceId != null) {
      query.superProcessInstanceId(superProcessInstanceId);
    }
    if (subProcessInstanceId != null) {
      query.subProcessInstanceId(subProcessInstanceId);
    }
    if (tenantIds != null && !tenantIds.isEmpty()) {
      query.tenantIdIn(tenantIds.toArray(new String[tenantIds.size()]));
    }
    if (TRUE.equals(withoutTenantId)) {
      query.withoutTenantId();
    }
    if (createdBy != null) {
      query.createdBy(createdBy);
    }
    if (createdBefore != null) {
      query.createdBefore(createdBefore);
    }
    if (createdAfter != null) {
      query.createdAfter(createdAfter);
    }
    if (closedBefore != null) {
      query.closedBefore(closedBefore);
    }
    if (closedAfter != null) {
      query.closedAfter(closedAfter);
    }
    if (active != null && active) {
      query.active();
    }
    if (completed != null && completed) {
      query.completed();
    }
    if (terminated != null && terminated) {
      query.terminated();
    }
    if (closed != null && closed) {
      query.closed();
    }
    if (notClosed != null && notClosed) {
      query.notClosed();
    }
    if (caseActivityIdIn != null && !caseActivityIdIn.isEmpty()) {
      query.caseActivityIdIn(caseActivityIdIn.toArray(new String[caseActivityIdIn.size()]));
    }
    if(Boolean.TRUE.equals(variableNamesIgnoreCase)) {
      query.matchVariableNamesIgnoreCase();
    }
    if(Boolean.TRUE.equals(variableValuesIgnoreCase)) {
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
  }

  @Override
  protected void applySortBy(HistoricCaseInstanceQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (sortBy.equals(SORT_BY_CASE_INSTANCE_ID_VALUE)) {
      query.orderByCaseInstanceId();
    } else if (sortBy.equals(SORT_BY_CASE_DEFINITION_ID_VALUE)) {
      query.orderByCaseDefinitionId();
    } else if (sortBy.equals(SORT_BY_CASE_INSTANCE_BUSINESS_KEY_VALUE)) {
      query.orderByCaseInstanceBusinessKey();
    } else if (sortBy.equals(SORT_BY_CASE_INSTANCE_CREATE_TIME_VALUE)) {
      query.orderByCaseInstanceCreateTime();
    } else if (sortBy.equals(SORT_BY_CASE_INSTANCE_CLOSE_TIME_VALUE)) {
      query.orderByCaseInstanceCloseTime();
    } else if (sortBy.equals(SORT_BY_CASE_INSTANCE_DURATION_VALUE)) {
      query.orderByCaseInstanceDuration();
    } else if (sortBy.equals(SORT_BY_TENANT_ID)) {
      query.orderByTenantId();
    }
  }

}
