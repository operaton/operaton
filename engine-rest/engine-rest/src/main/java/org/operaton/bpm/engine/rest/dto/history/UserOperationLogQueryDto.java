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

import java.util.Date;
import java.util.Map;
import jakarta.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.converter.DateConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;

/**
 * @author Danny Gräf
 */
public class UserOperationLogQueryDto extends AbstractQueryDto<UserOperationLogQuery> {

  public static final String TIMESTAMP = "timestamp";

  protected String deploymentId;
  protected String processDefinitionId;
  protected String processDefinitionKey;
  protected String processInstanceId;
  protected String executionId;
  protected String caseDefinitionId;
  protected String caseInstanceId;
  protected String caseExecutionId;
  protected String taskId;
  protected String jobId;
  protected String jobDefinitionId;
  protected String batchId;
  protected String userId;
  protected String operationId;
  protected String externalTaskId;
  protected String operationType;
  protected String entityType;
  protected String property;
  protected String category;
  protected Date afterTimestamp;
  protected Date beforeTimestamp;

  protected String[] entityTypes;
  protected String[] categories;

  public UserOperationLogQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return TIMESTAMP.equals(value);
  }

  @Override
  protected UserOperationLogQuery createNewQuery(ProcessEngine engine) {
    return engine.getHistoryService().createUserOperationLogQuery();
  }

  @Override
  protected void applyFilters(UserOperationLogQuery query) {
    if (deploymentId != null) {
      query.deploymentId(deploymentId);
    }
    if (processDefinitionId != null) {
      query.processDefinitionId(processDefinitionId);
    }
    if (processDefinitionKey != null) {
      query.processDefinitionKey(processDefinitionKey);
    }
    if (processInstanceId != null) {
      query.processInstanceId(processInstanceId);
    }
    if (executionId != null) {
      query.executionId(executionId);
    }
    if (caseDefinitionId != null) {
      query.caseDefinitionId(caseDefinitionId);
    }
    if (caseInstanceId != null) {
      query.caseInstanceId(caseInstanceId);
    }
    if (caseExecutionId != null) {
      query.caseExecutionId(caseExecutionId);
    }
    if (taskId != null) {
      query.taskId(taskId);
    }
    if (jobId != null) {
      query.jobId(jobId);
    }
    if (jobDefinitionId != null) {
      query.jobDefinitionId(jobDefinitionId);
    }
    if (batchId != null) {
      query.batchId(batchId);
    }
    if (userId != null) {
      query.userId(userId);
    }
    if (operationId != null) {
      query.operationId(operationId);
    }
    if (externalTaskId != null) {
      query.externalTaskId(externalTaskId);
    }
    if (operationType != null) {
      query.operationType(operationType);
    }
    if (entityType != null) {
      query.entityType(entityType);
    }
    if (entityTypes != null) {
      query.entityTypeIn(entityTypes);
    }
    if (category != null) {
      query.category(category);
    }
    if (categories != null) {
      query.categoryIn(categories);
    }
    if (property != null) {
      query.property(property);
    }
    if (afterTimestamp != null) {
      query.afterTimestamp(afterTimestamp);
    }
    if (beforeTimestamp != null) {
      query.beforeTimestamp(beforeTimestamp);
    }
  }

  @Override
  protected void applySortBy(UserOperationLogQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (TIMESTAMP.equals(sortBy)) {
      query.orderByTimestamp();
    }
  }

  @OperatonQueryParam("deploymentId")
  public void setDeploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
  }

  @OperatonQueryParam("processDefinitionId")
  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  @OperatonQueryParam("processDefinitionKey")
  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  @OperatonQueryParam("processInstanceId")
  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  @OperatonQueryParam("executionId")
  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  @OperatonQueryParam("caseDefinitionId")
  public void setCaseDefinitionId(String caseDefinitionId) {
    this.caseDefinitionId = caseDefinitionId;
  }

  @OperatonQueryParam("caseInstanceId")
  public void setCaseInstanceId(String caseInstanceId) {
    this.caseInstanceId = caseInstanceId;
  }

  @OperatonQueryParam("caseExecutionId")
  public void setCaseExecutionId(String caseExecutionId) {
    this.caseExecutionId = caseExecutionId;
  }

  @OperatonQueryParam("taskId")
  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  @OperatonQueryParam("jobId")
  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  @OperatonQueryParam("jobDefinitionId")
  public void setJobDefinitionId(String jobDefinitionId) {
    this.jobDefinitionId = jobDefinitionId;
  }

  @OperatonQueryParam("batchId")
  public void setBatchId(String batchId) {
    this.batchId = batchId;
  }

  @OperatonQueryParam("userId")
  public void setUserId(String userId) {
    this.userId = userId;
  }

  @OperatonQueryParam("operationId")
  public void setOperationId(String operationId) {
    this.operationId = operationId;
  }

  @OperatonQueryParam("externalTaskId")
  public void setExternalTaskId(String externalTaskId) {
    this.externalTaskId = externalTaskId;
  }

  @OperatonQueryParam("operationType")
  public void setOperationType(String operationType) {
    this.operationType = operationType;
  }

  @OperatonQueryParam("entityType")
  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  @OperatonQueryParam(value = "entityTypeIn", converter = StringArrayConverter.class)
  public void setEntityTypeIn(String[] entityTypes) {
    this.entityTypes = entityTypes;
  }

  @OperatonQueryParam("category")
  public void setcategory(String category) {
    this.category = category;
  }

  @OperatonQueryParam(value = "categoryIn", converter = StringArrayConverter.class)
  public void setCategoryIn(String[] categories) {
    this.categories = categories;
  }

  @OperatonQueryParam("property")
  public void setProperty(String property) {
    this.property = property;
  }

  @OperatonQueryParam(value = "afterTimestamp", converter = DateConverter.class)
  public void setAfterTimestamp(Date after) {
    this.afterTimestamp = after;
  }

  @OperatonQueryParam(value = "beforeTimestamp", converter = DateConverter.class)
  public void setBeforeTimestamp(Date before) {
    this.beforeTimestamp = before;
  }
}
