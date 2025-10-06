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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricExternalTaskLogQuery;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.converter.BooleanConverter;
import org.operaton.bpm.engine.rest.dto.converter.LongConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;

import static java.lang.Boolean.TRUE;

public class HistoricExternalTaskLogQueryDto extends AbstractQueryDto<HistoricExternalTaskLogQuery> {

  protected static final String SORT_BY_TIMESTAMP = "timestamp";
  protected static final String SORT_BY_EXTERNAL_TASK_ID = "externalTaskId";
  protected static final String SORT_BY_RETRIES = "retries";
  protected static final String SORT_BY_PRIORITY = "priority";
  protected static final String SORT_BY_TOPIC_NAME = "topicName";
  protected static final String SORT_BY_WORKER_ID = "workerId";
  protected static final String SORT_BY_ACTIVITY_ID = "activityId";
  protected static final String SORT_BY_ACTIVITY_INSTANCE_ID = "activityInstanceId";
  protected static final String SORT_BY_EXECUTION_ID = "executionId";
  protected static final String SORT_BY_PROCESS_INSTANCE_ID = "processInstanceId";
  protected static final String SORT_BY_PROCESS_DEFINITION_ID = "processDefinitionId";
  protected static final String SORT_BY_PROCESS_DEFINITION_KEY = "processDefinitionKey";
  protected static final String SORT_BY_TENANT_ID = "tenantId";

  protected static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();

    VALID_SORT_BY_VALUES.add(SORT_BY_TIMESTAMP);
    VALID_SORT_BY_VALUES.add(SORT_BY_EXTERNAL_TASK_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_RETRIES);
    VALID_SORT_BY_VALUES.add(SORT_BY_PRIORITY);
    VALID_SORT_BY_VALUES.add(SORT_BY_TOPIC_NAME);
    VALID_SORT_BY_VALUES.add(SORT_BY_WORKER_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_ACTIVITY_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_ACTIVITY_INSTANCE_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_EXECUTION_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_INSTANCE_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_DEFINITION_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_DEFINITION_KEY);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
  }

  protected String id;
  protected String externalTaskId;
  protected String topicName;
  protected String workerId;
  protected String errorMessage;
  protected String[] activityIds;
  protected String[] activityInstanceIds;
  protected String[] executionIds;
  protected String processInstanceId;
  protected String processDefinitionId;
  protected String processDefinitionKey;
  protected Long priorityHigherThanOrEquals;
  protected Long priorityLowerThanOrEquals;
  protected String[] tenantIds;
  protected Boolean withoutTenantId;
  protected Boolean creationLog;
  protected Boolean failureLog;
  protected Boolean successLog;
  protected Boolean deletionLog;

  public HistoricExternalTaskLogQueryDto() {}

  public HistoricExternalTaskLogQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("logId")
  public void setLogId(String id) {
    this.id = id;
  }

  @OperatonQueryParam("externalTaskId")
  public void setExternalTaskId(String externalTaskId) {
    this.externalTaskId = externalTaskId;
  }

  @OperatonQueryParam("topicName")
  public void setTopicName(String topicName) {
    this.topicName = topicName;
  }

  @OperatonQueryParam("workerId")
  public void setWorkerId(String workerId) {
    this.workerId = workerId;
  }

  @OperatonQueryParam("errorMessage")
  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  @OperatonQueryParam(value="activityIdIn", converter = StringArrayConverter.class)
  public void setActivityIdIn(String[] activityIds) {
    this.activityIds = activityIds;
  }

  @OperatonQueryParam(value="activityInstanceIdIn", converter = StringArrayConverter.class)
  public void setActivityInstanceIdIn(String[] activityInstanceIds) {
    this.activityInstanceIds = activityInstanceIds;
  }

  @OperatonQueryParam(value="executionIdIn", converter = StringArrayConverter.class)
  public void setExecutionIdIn(String[] executionIds) {
    this.executionIds = executionIds;
  }

  @OperatonQueryParam("processInstanceId")
  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  @OperatonQueryParam("processDefinitionId")
  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  @OperatonQueryParam("processDefinitionKey")
  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  @OperatonQueryParam(value="priorityHigherThanOrEquals", converter = LongConverter.class)
  public void setPriorityHigherThanOrEquals(Long priorityHigherThanOrEquals) {
    this.priorityHigherThanOrEquals = priorityHigherThanOrEquals;
  }

  @OperatonQueryParam(value="priorityLowerThanOrEquals", converter = LongConverter.class)
  public void setPriorityLowerThanOrEquals(Long priorityLowerThanOrEquals) {
    this.priorityLowerThanOrEquals = priorityLowerThanOrEquals;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringArrayConverter.class)
  public void setTenantIdIn(String[] tenantIds) {
    this.tenantIds = tenantIds;
  }

  @OperatonQueryParam(value = "withoutTenantId", converter = BooleanConverter.class)
  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  @OperatonQueryParam(value="creationLog", converter = BooleanConverter.class)
  public void setCreationLog(Boolean creationLog) {
    this.creationLog = creationLog;
  }

  @OperatonQueryParam(value="failureLog", converter = BooleanConverter.class)
  public void setFailureLog(Boolean failureLog) {
    this.failureLog = failureLog;
  }

  @OperatonQueryParam(value="successLog", converter = BooleanConverter.class)
  public void setSuccessLog(Boolean successLog) {
    this.successLog = successLog;
  }

  @OperatonQueryParam(value="deletionLog", converter = BooleanConverter.class)
  public void setDeletionLog(Boolean deletionLog) {
    this.deletionLog = deletionLog;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected HistoricExternalTaskLogQuery createNewQuery(ProcessEngine engine) {
    return engine.getHistoryService().createHistoricExternalTaskLogQuery();

  }

  @Override
  protected void applyFilters(HistoricExternalTaskLogQuery query) {
    if (id != null) {
      query.logId(id);
    }

    if (externalTaskId != null) {
      query.externalTaskId(externalTaskId);
    }

    if (topicName != null) {
      query.topicName(topicName);
    }

    if (workerId != null) {
      query.workerId(workerId);
    }

    if (errorMessage != null) {
      query.errorMessage(errorMessage);
    }

    if (activityIds != null && activityIds.length > 0) {
      query.activityIdIn(activityIds);
    }

    if (activityInstanceIds != null && activityInstanceIds.length > 0) {
      query.activityInstanceIdIn(activityInstanceIds);
    }

    if (executionIds != null && executionIds.length > 0) {
      query.executionIdIn(executionIds);
    }

    if (processInstanceId != null) {
      query.processInstanceId(processInstanceId);
    }

    if (processDefinitionId != null) {
      query.processDefinitionId(processDefinitionId);
    }

    if (processDefinitionKey != null) {
      query.processDefinitionKey(processDefinitionKey);
    }

    if (creationLog != null && creationLog) {
      query.creationLog();
    }

    if (failureLog != null && failureLog) {
      query.failureLog();
    }

    if (successLog != null && successLog) {
      query.successLog();
    }

    if (deletionLog != null && deletionLog) {
      query.deletionLog();
    }

    if (priorityHigherThanOrEquals != null) {
      query.priorityHigherThanOrEquals(priorityHigherThanOrEquals);
    }

    if (priorityLowerThanOrEquals != null) {
      query.priorityLowerThanOrEquals(priorityLowerThanOrEquals);
    }
    if (tenantIds != null && tenantIds.length > 0) {
      query.tenantIdIn(tenantIds);
    }
    if (TRUE.equals(withoutTenantId)) {
      query.withoutTenantId();
    }
  }

  @Override
  protected void applySortBy(HistoricExternalTaskLogQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (SORT_BY_TIMESTAMP.equals(sortBy)) {
      query.orderByTimestamp();
    } else if (SORT_BY_EXTERNAL_TASK_ID.equals(sortBy)) {
      query.orderByExternalTaskId();
    } else if (SORT_BY_RETRIES.equals(sortBy)) {
      query.orderByRetries();
    } else if (SORT_BY_PRIORITY.equals(sortBy)) {
      query.orderByPriority();
    } else if (SORT_BY_TOPIC_NAME.equals(sortBy)) {
      query.orderByTopicName();
    } else if (SORT_BY_WORKER_ID.equals(sortBy)) {
      query.orderByWorkerId();
    } else if (SORT_BY_ACTIVITY_ID.equals(sortBy)) {
      query.orderByActivityId();
    }else if (SORT_BY_ACTIVITY_INSTANCE_ID.equals(sortBy)) {
      query.orderByActivityInstanceId();
    } else if (SORT_BY_EXECUTION_ID.equals(sortBy)) {
      query.orderByExecutionId();
    } else if (SORT_BY_PROCESS_INSTANCE_ID.equals(sortBy)) {
      query.orderByProcessInstanceId();
    } else if (SORT_BY_PROCESS_DEFINITION_ID.equals(sortBy)) {
      query.orderByProcessDefinitionId();
    } else if (SORT_BY_PROCESS_DEFINITION_KEY.equals(sortBy)) {
      query.orderByProcessDefinitionKey();
    } else if (SORT_BY_TENANT_ID.equals(sortBy)) {
      query.orderByTenantId();
    }
  }
}
