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
package org.operaton.bpm.engine.rest.dto.externaltask;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.externaltask.ExternalTaskQuery;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.VariableQueryParameterDto;
import org.operaton.bpm.engine.rest.dto.converter.*;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;


/**
 * @author Thorben Lindhauer
 *
 */
public class ExternalTaskQueryDto extends AbstractQueryDto<ExternalTaskQuery> {

  public static final Map<String, Consumer<ExternalTaskQuery>> SORT_METHODS_BY_FIELD = Map.of(
      "id", ExternalTaskQuery::orderById,
      "lockExpirationTime", ExternalTaskQuery::orderByLockExpirationTime,
      "processInstanceId", ExternalTaskQuery::orderByProcessInstanceId,
      "processDefinitionId", ExternalTaskQuery::orderByProcessDefinitionId,
      "processDefinitionKey", ExternalTaskQuery::orderByProcessDefinitionKey,
      "tenantId", ExternalTaskQuery::orderByTenantId,
      "taskPriority", ExternalTaskQuery::orderByPriority,
      "createTime", ExternalTaskQuery::orderByCreateTime
  );

  protected String externalTaskId;
  protected Set<String> externalTaskIds;
  protected String activityId;
  protected List<String> activityIdIn;
  protected Date lockExpirationBefore;
  protected Date lockExpirationAfter;
  protected String topicName;
  protected Boolean locked;
  protected Boolean notLocked;
  protected String executionId;
  protected String processInstanceId;
  protected List<String> processInstanceIdIn;
  protected String processDefinitionKey;
  protected String[] processDefinitionKeyIn;
  protected String processDefinitionId;
  protected String processDefinitionName;
  protected String processDefinitionNameLike;
  protected Boolean active;
  protected Boolean suspended;
  protected Boolean withRetriesLeft;
  protected Boolean noRetriesLeft;
  protected String workerId;
  protected List<String> tenantIds;
  protected Long priorityHigherThanOrEquals;
  protected Long priorityLowerThanOrEquals;
  protected Boolean variableNamesIgnoreCase;
  protected Boolean variableValuesIgnoreCase;

  private List<VariableQueryParameterDto> processVariables;

  public ExternalTaskQueryDto() {
  }

  public ExternalTaskQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("externalTaskId")
  public void setExternalTaskId(String externalTaskId) {
    this.externalTaskId = externalTaskId;
  }

  @OperatonQueryParam(value = "externalTaskIdIn", converter = StringSetConverter.class)
  public void setExternalTaskIdIn(Set<String> externalTaskIds) {
    this.externalTaskIds = externalTaskIds;
  }

  @OperatonQueryParam("activityId")
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  @OperatonQueryParam(value = "activityIdIn", converter = StringListConverter.class)
  public void setActivityIdIn(List<String> activityIdIn) {
    this.activityIdIn = activityIdIn;
  }

  @OperatonQueryParam(value = "lockExpirationBefore", converter = DateConverter.class)
  public void setLockExpirationBefore(Date lockExpirationBefore) {
    this.lockExpirationBefore = lockExpirationBefore;
  }

  @OperatonQueryParam(value = "lockExpirationAfter", converter = DateConverter.class)
  public void setLockExpirationAfter(Date lockExpirationAfter) {
    this.lockExpirationAfter = lockExpirationAfter;
  }

  @OperatonQueryParam("topicName")
  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  @OperatonQueryParam(value = "locked", converter = BooleanConverter.class)
  public void setLocked(Boolean locked) {
    this.locked = locked;
  }

  @OperatonQueryParam(value = "notLocked", converter = BooleanConverter.class)
  public void setNotLocked(Boolean notLocked) {
    this.notLocked = notLocked;
  }

  @OperatonQueryParam("executionId")
  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  @OperatonQueryParam("processInstanceId")
  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  @OperatonQueryParam(value="processInstanceIdIn", converter = StringListConverter.class)
  public void setProcessInstanceIdIn(List<String> processInstanceIdIn) {
    this.processInstanceIdIn = processInstanceIdIn;
  }

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  @OperatonQueryParam("processDefinitionKey")
  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  @OperatonQueryParam(value = "processDefinitionKeyIn", converter = StringArrayConverter.class)
  public void setProcessDefinitionKeyIn(String[] processDefinitionKeyIn) {
    this.processDefinitionKeyIn = processDefinitionKeyIn;
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

  @OperatonQueryParam(value = "active", converter = BooleanConverter.class)
  public void setActive(Boolean active) {
    this.active = active;
  }

  @OperatonQueryParam(value = "suspended", converter = BooleanConverter.class)
  public void setSuspended(Boolean suspended) {
    this.suspended = suspended;
  }

  @OperatonQueryParam(value = "withRetriesLeft", converter = BooleanConverter.class)
  public void setWithRetriesLeft(Boolean withRetriesLeft) {
    this.withRetriesLeft = withRetriesLeft;
  }

  @OperatonQueryParam(value = "noRetriesLeft", converter = BooleanConverter.class)
  public void setNoRetriesLeft(Boolean noRetriesLeft) {
    this.noRetriesLeft = noRetriesLeft;
  }

  @OperatonQueryParam("workerId")
  public void setWorkerId(String workerId) {
    this.workerId = workerId;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
  }
  @OperatonQueryParam(value="priorityHigherThanOrEquals", converter = LongConverter.class)
  public void setPriorityHigherThanOrEquals(Long priorityHigherThanOrEquals) {
    this.priorityHigherThanOrEquals = priorityHigherThanOrEquals;
  }

  @OperatonQueryParam(value = "variableNamesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableNamesIgnoreCase(Boolean variableNamesCaseInsensitive) {
    this.variableNamesIgnoreCase = variableNamesCaseInsensitive;
  }

  @OperatonQueryParam(value ="variableValuesIgnoreCase", converter = BooleanConverter.class)
  public void setVariableValuesIgnoreCase(Boolean variableValuesCaseInsensitive) {
    this.variableValuesIgnoreCase = variableValuesCaseInsensitive;
  }

  @OperatonQueryParam(value = "processVariables", converter = VariableListConverter.class)
  public void setProcessVariables(List<VariableQueryParameterDto> processVariables) {
    this.processVariables = processVariables;
  }

  @OperatonQueryParam(value="priorityLowerThanOrEquals", converter = LongConverter.class)
  public void setPriorityLowerThanOrEquals(Long priorityLowerThanOrEquals) {
    this.priorityLowerThanOrEquals = priorityLowerThanOrEquals;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return SORT_METHODS_BY_FIELD.containsKey(value);
  }

  @Override
  protected ExternalTaskQuery createNewQuery(ProcessEngine engine) {
    return engine.getExternalTaskService().createExternalTaskQuery();
  }

  @Override
  protected void applyFilters(ExternalTaskQuery query) {
    if (externalTaskId != null) {
      query.externalTaskId(externalTaskId);
    }
    if (externalTaskIds != null && !externalTaskIds.isEmpty()) {
      query.externalTaskIdIn(externalTaskIds);
    }
    if (activityId != null) {
      query.activityId(activityId);
    }
    if (activityIdIn != null && !activityIdIn.isEmpty()) {
      query.activityIdIn(activityIdIn.toArray(new String[0]));
    }
    if (lockExpirationBefore != null) {
      query.lockExpirationBefore(lockExpirationBefore);
    }
    if (lockExpirationAfter != null) {
      query.lockExpirationAfter(lockExpirationAfter);
    }
    if (topicName != null) {
      query.topicName(topicName);
    }
    if (locked != null && locked) {
      query.locked();
    }
    if (notLocked != null && notLocked) {
      query.notLocked();
    }
    if (executionId != null) {
      query.executionId(executionId);
    }
    if (processInstanceId != null) {
      query.processInstanceId(processInstanceId);
    }
    if (processInstanceIdIn != null && !processInstanceIdIn.isEmpty()) {
      query.processInstanceIdIn(processInstanceIdIn.toArray(new String[0]));
    }
    if (processDefinitionKey != null) {
      query.processDefinitionKey(processDefinitionKey);
    }
    if (processDefinitionKeyIn != null && processDefinitionKeyIn.length > 0) {
      query.processDefinitionKeyIn(processDefinitionKeyIn);
    }
    if (processDefinitionId != null) {
      query.processDefinitionId(processDefinitionId);
    }
    if (processDefinitionName != null) {
      query.processDefinitionName(processDefinitionName);
    }
    if (processDefinitionNameLike != null) {
      query.processDefinitionNameLike(processDefinitionNameLike);
    }
    if (active != null && active) {
      query.active();
    }
    if (suspended != null && suspended) {
      query.suspended();
    }
    if (priorityHigherThanOrEquals != null) {
      query.priorityHigherThanOrEquals(priorityHigherThanOrEquals);
    }
    if (priorityLowerThanOrEquals != null) {
      query.priorityLowerThanOrEquals(priorityLowerThanOrEquals);
    }
    if (withRetriesLeft != null && withRetriesLeft) {
      query.withRetriesLeft();
    }
    if (noRetriesLeft != null && noRetriesLeft) {
      query.noRetriesLeft();
    }
    if(variableValuesIgnoreCase != null && variableValuesIgnoreCase) {
      query.matchVariableValuesIgnoreCase();
    }
    if(variableNamesIgnoreCase != null && variableNamesIgnoreCase) {
      query.matchVariableNamesIgnoreCase();
    }
    if (processVariables != null) {
      for (VariableQueryParameterDto variableQueryParam : processVariables) {
        String variableName = variableQueryParam.getName();
        String op = variableQueryParam.getOperator();
        Object variableValue = variableQueryParam.resolveValue(objectMapper);
        if (ConditionQueryParameterDto.EQUALS_OPERATOR_NAME.equals(op)) {
          query.processVariableValueEquals(variableName, variableValue);
        } else if (ConditionQueryParameterDto.NOT_EQUALS_OPERATOR_NAME.equals(op)) {
          query.processVariableValueNotEquals(variableName, variableValue);
        } else if (ConditionQueryParameterDto.GREATER_THAN_OPERATOR_NAME.equals(op)) {
          query.processVariableValueGreaterThan(variableName, variableValue);
        } else if (ConditionQueryParameterDto.GREATER_THAN_OR_EQUALS_OPERATOR_NAME.equals(op)) {
          query.processVariableValueGreaterThanOrEquals(variableName, variableValue);
        } else if (ConditionQueryParameterDto.LESS_THAN_OPERATOR_NAME.equals(op)) {
          query.processVariableValueLessThan(variableName, variableValue);
        } else if (ConditionQueryParameterDto.LESS_THAN_OR_EQUALS_OPERATOR_NAME.equals(op)) {
          query.processVariableValueLessThanOrEquals(variableName, variableValue);
        } else if (ConditionQueryParameterDto.LIKE_OPERATOR_NAME.equals(op)) {
          query.processVariableValueLike(variableName, String.valueOf(variableValue));
        } else if (ConditionQueryParameterDto.NOT_LIKE_OPERATOR_NAME.equals(op)) {
          query.processVariableValueNotLike(variableName, String.valueOf(variableValue));
        } else {
          throw new InvalidRequestException(Response.Status.BAD_REQUEST, "Invalid process variable comparator specified: %s".formatted(op));
        }
      }
    }
    if (workerId != null) {
      query.workerId(workerId);
    }
    if (tenantIds != null && !tenantIds.isEmpty()) {
      query.tenantIdIn(tenantIds.toArray(new String[0]));
    }
  }

  @Override
  protected void applySortBy(ExternalTaskQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    var sortByMethod = SORT_METHODS_BY_FIELD.get(sortBy);

    if (sortByMethod != null) {
      sortByMethod.accept(query);
    }
  }
}
