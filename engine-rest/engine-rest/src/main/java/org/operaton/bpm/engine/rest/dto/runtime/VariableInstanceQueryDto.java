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
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringListConverter;
import org.operaton.bpm.engine.rest.dto.converter.VariableListConverter;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;

import static org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto.*;

/**
 * @author roman.smirnov
 */
public class VariableInstanceQueryDto extends AbstractQueryDto<VariableInstanceQuery> {

  private static final String SORT_BY_VARIABLE_NAME_VALUE = "variableName";
  private static final String SORT_BY_VARIABLE_TYPE_VALUE = "variableType";
  private static final String SORT_BY_ACTIVITY_INSTANCE_ID_VALUE = "activityInstanceId";
  private static final String SORT_BY_TENANT_ID = "tenantId";

  private static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_VARIABLE_NAME_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_VARIABLE_TYPE_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_ACTIVITY_INSTANCE_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
  }

  protected String variableName;
  protected String variableNameLike;
  protected List<VariableQueryParameterDto> variableValues;
  protected Boolean variableNamesIgnoreCase;
  protected Boolean variableValuesIgnoreCase;
  protected String[] executionIdIn;
  protected String[] processInstanceIdIn;
  protected String[] caseExecutionIdIn;
  protected String[] caseInstanceIdIn;
  protected String[] taskIdIn;
  protected String[] batchIdIn;
  protected String[] variableScopeIdIn;
  protected String[] activityInstanceIdIn;
  private List<String> tenantIds;

  public VariableInstanceQueryDto() {}

  public VariableInstanceQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("variableName")
  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }

  @OperatonQueryParam("variableNameLike")
  public void setVariableNameLike(String variableNameLike) {
    this.variableNameLike = variableNameLike;
  }

  @OperatonQueryParam(value = "variableValues", converter = VariableListConverter.class)
  public void setVariableValues(List<VariableQueryParameterDto> variableValues) {
    this.variableValues = variableValues;
  }

  @OperatonQueryParam(value = "variableNamesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableNamesIgnoreCase(Boolean variableNamesIgnoreCase) {
    this.variableNamesIgnoreCase = variableNamesIgnoreCase;
  }

  @OperatonQueryParam(value = "variableValuesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableValuesIgnoreCase(Boolean variableValuesIgnoreCase) {
    this.variableValuesIgnoreCase = variableValuesIgnoreCase;
  }

  @OperatonQueryParam(value="executionIdIn", converter = StringArrayConverter.class)
  public void setExecutionIdIn(String[] executionIdIn) {
    this.executionIdIn = executionIdIn;
  }

  @OperatonQueryParam(value="processInstanceIdIn", converter = StringArrayConverter.class)
  public void setProcessInstanceIdIn(String[] processInstanceIdIn) {
    this.processInstanceIdIn = processInstanceIdIn;
  }

  @OperatonQueryParam(value="caseExecutionIdIn", converter = StringArrayConverter.class)
  public void setCaseExecutionIdIn(String[] caseExecutionIdIn) {
    this.caseExecutionIdIn = caseExecutionIdIn;
  }

  @OperatonQueryParam(value="caseInstanceIdIn", converter = StringArrayConverter.class)
  public void setCaseInstanceIdIn(String[] caseInstanceIdIn) {
    this.caseInstanceIdIn = caseInstanceIdIn;
  }

  @OperatonQueryParam(value="taskIdIn", converter = StringArrayConverter.class)
  public void setTaskIdIn(String[] taskIdIn) {
    this.taskIdIn = taskIdIn;
  }

  @OperatonQueryParam(value="batchIdIn", converter = StringArrayConverter.class)
  public void setBatchIdIn(String[] batchIdIn) {
    this.batchIdIn = batchIdIn;
  }

  @OperatonQueryParam(value="variableScopeIdIn", converter = StringArrayConverter.class)
  public void setVariableScopeIdIn(String[] variableScopeIdIn) {
    this.variableScopeIdIn = variableScopeIdIn;
  }

  @OperatonQueryParam(value="activityInstanceIdIn", converter = StringArrayConverter.class)
  public void setActivityInstanceIdIn(String[] activityInstanceIdIn) {
    this.activityInstanceIdIn = activityInstanceIdIn;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected VariableInstanceQuery createNewQuery(ProcessEngine engine) {
    return engine.getRuntimeService().createVariableInstanceQuery();
  }

  @Override
  protected void applyFilters(VariableInstanceQuery query) {
    if (variableName != null) {
      query.variableName(variableName);
    }

    if (variableNameLike != null) {
      query.variableNameLike(variableNameLike);
    }

    if(Boolean.TRUE.equals(variableNamesIgnoreCase)) {
      query.matchVariableNamesIgnoreCase();
    }

    if(Boolean.TRUE.equals(variableValuesIgnoreCase)) {
      query.matchVariableValuesIgnoreCase();
    }

    if (variableValues != null) {
      for (VariableQueryParameterDto variableQueryParam : variableValues) {
        String varName = variableQueryParam.getName();
        String op = variableQueryParam.getOperator();
        Object variableValue = variableQueryParam.resolveValue(objectMapper);

        if (EQUALS_OPERATOR_NAME.equals(op)) {
          query.variableValueEquals(varName, variableValue);
        } else if (GREATER_THAN_OPERATOR_NAME.equals(op)) {
          query.variableValueGreaterThan(varName, variableValue);
        } else if (GREATER_THAN_OR_EQUALS_OPERATOR_NAME.equals(op)) {
          query.variableValueGreaterThanOrEqual(varName, variableValue);
        } else if (LESS_THAN_OPERATOR_NAME.equals(op)) {
          query.variableValueLessThan(varName, variableValue);
        } else if (LESS_THAN_OR_EQUALS_OPERATOR_NAME.equals(op)) {
          query.variableValueLessThanOrEqual(varName, variableValue);
        } else if (NOT_EQUALS_OPERATOR_NAME.equals(op)) {
          query.variableValueNotEquals(varName, variableValue);
        } else if (LIKE_OPERATOR_NAME.equals(op)) {
          query.variableValueLike(varName, String.valueOf(variableValue));
        } else {
          throw new InvalidRequestException(Status.BAD_REQUEST, "Invalid variable comparator specified: %s".formatted(op));
        }
      }
    }

    if (executionIdIn != null && executionIdIn.length > 0) {
      query.executionIdIn(executionIdIn);
    }

    if (processInstanceIdIn != null && processInstanceIdIn.length > 0) {
      query.processInstanceIdIn(processInstanceIdIn);
    }

    if (caseExecutionIdIn != null && caseExecutionIdIn.length > 0) {
      query.caseExecutionIdIn(caseExecutionIdIn);
    }

    if (caseInstanceIdIn != null && caseInstanceIdIn.length > 0) {
      query.caseInstanceIdIn(caseInstanceIdIn);
    }

    if (taskIdIn != null && taskIdIn.length > 0) {
      query.taskIdIn(taskIdIn);
    }

    if (batchIdIn != null && batchIdIn.length > 0) {
      query.batchIdIn(batchIdIn);
    }

    if (variableScopeIdIn != null && variableScopeIdIn.length > 0) {
      query.variableScopeIdIn(variableScopeIdIn);
    }

    if (activityInstanceIdIn != null && activityInstanceIdIn.length > 0) {
      query.activityInstanceIdIn(activityInstanceIdIn);
    }
    if (tenantIds != null && !tenantIds.isEmpty()) {
      query.tenantIdIn(tenantIds.toArray(new String[0]));
    }
  }

  @Override
  protected void applySortBy(VariableInstanceQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (SORT_BY_VARIABLE_NAME_VALUE.equals(sortBy)) {
      query.orderByVariableName();
    } else if (SORT_BY_VARIABLE_TYPE_VALUE.equals(sortBy)) {
      query.orderByVariableType();
    } else if (SORT_BY_ACTIVITY_INSTANCE_ID_VALUE.equals(sortBy)) {
      query.orderByActivityInstanceId();
    } else if (SORT_BY_TENANT_ID.equals(sortBy)) {
      query.orderByTenantId();
    }
  }

}
