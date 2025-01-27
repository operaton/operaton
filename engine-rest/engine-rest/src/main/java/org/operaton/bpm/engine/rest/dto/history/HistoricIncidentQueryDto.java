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

import static java.lang.Boolean.TRUE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Date;

import jakarta.ws.rs.core.MultivaluedMap;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricIncidentQuery;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.converter.BooleanConverter;
import org.operaton.bpm.engine.rest.dto.converter.DateConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringListConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Roman Smirnov
 *
 */
public class HistoricIncidentQueryDto extends AbstractQueryDto<HistoricIncidentQuery> {

  private static final String SORT_BY_INCIDENT_ID = "incidentId";
  private static final String SORT_BY_INCIDENT_MESSAGE = "incidentMessage";
  private static final String SORT_BY_CREATE_TIME = "createTime";
  private static final String SORT_BY_END_TIME = "endTime";
  private static final String SORT_BY_INCIDENT_TYPE = "incidentType";
  private static final String SORT_BY_EXECUTION_ID = "executionId";
  private static final String SORT_BY_ACTIVITY_ID = "activityId";
  private static final String SORT_BY_PROCESS_INSTANCE_ID = "processInstanceId";
  private static final String SORT_BY_PROCESS_DEFINITION_ID = "processDefinitionId";
  private static final String SORT_BY_PROCESS_DEFINITION_KEY = "processDefinitionKey";
  private static final String SORT_BY_CAUSE_INCIDENT_ID = "causeIncidentId";
  private static final String SORT_BY_ROOT_CAUSE_INCIDENT_ID = "rootCauseIncidentId";
  private static final String SORT_BY_CONFIGURATION = "configuration";
  private static final String SORT_BY_HISTORY_CONFIGURATION = "historyConfiguration";
  private static final String SORT_BY_TENANT_ID = "tenantId";
  private static final String SORT_BY_INCIDENT_STATE = "incidentState";

  private static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_INCIDENT_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_INCIDENT_MESSAGE);
    VALID_SORT_BY_VALUES.add(SORT_BY_CREATE_TIME);
    VALID_SORT_BY_VALUES.add(SORT_BY_END_TIME);
    VALID_SORT_BY_VALUES.add(SORT_BY_INCIDENT_TYPE);
    VALID_SORT_BY_VALUES.add(SORT_BY_EXECUTION_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_ACTIVITY_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_INSTANCE_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_DEFINITION_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_DEFINITION_KEY);
    VALID_SORT_BY_VALUES.add(SORT_BY_CAUSE_INCIDENT_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_ROOT_CAUSE_INCIDENT_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_CONFIGURATION);
    VALID_SORT_BY_VALUES.add(SORT_BY_HISTORY_CONFIGURATION);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_INCIDENT_STATE);
  }

  protected String incidentId;
  protected String incidentType;
  protected String incidentMessage;
  protected String incidentMessageLike;
  protected String processDefinitionId;
  protected String processDefinitionKey;
  protected String[] processDefinitionKeyIn;
  protected String processInstanceId;
  protected String executionId;
  protected Date createTimeBefore;
  protected Date createTimeAfter;
  protected Date endTimeBefore;
  protected Date endTimeAfter;
  protected String activityId;
  protected String failedActivityId;
  protected String causeIncidentId;
  protected String rootCauseIncidentId;
  protected String configuration;
  protected String historyConfiguration;
  protected Boolean open;
  protected Boolean resolved;
  protected Boolean deleted;
  protected List<String> tenantIds;
  protected Boolean withoutTenantId;
  protected List<String> jobDefinitionIds;


  public HistoricIncidentQueryDto() {}

  public HistoricIncidentQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
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

  @OperatonQueryParam("processDefinitionId")
  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  @OperatonQueryParam("processDefinitionKey")
  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  @OperatonQueryParam(value = "processDefinitionKeyIn", converter = StringArrayConverter.class)
  public void setProcessDefinitionKeyIn(String[] processDefinitionKeyIn) {
    this.processDefinitionKeyIn = processDefinitionKeyIn;
  }

  @OperatonQueryParam("processInstanceId")
  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  @OperatonQueryParam("executionId")
  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  @OperatonQueryParam(value="createTimeBefore", converter= DateConverter.class)
  public void setCreateTimeBefore(Date createTimeBefore) {
    this.createTimeBefore = createTimeBefore;
  }

  @OperatonQueryParam(value="createTimeAfter", converter= DateConverter.class)
  public void setCreateTimeAfter(Date createTimeAfter) {
    this.createTimeAfter = createTimeAfter;
  }

  @OperatonQueryParam(value="endTimeBefore", converter= DateConverter.class)
  public void setEndTimeBefore(Date endTimeBefore) {
    this.endTimeBefore = endTimeBefore;
  }

  @OperatonQueryParam(value="endTimeAfter", converter= DateConverter.class)
  public void setEndTimeAfter(Date endTimeAfter) {
    this.endTimeAfter = endTimeAfter;
  }

  @OperatonQueryParam("activityId")
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  @OperatonQueryParam("failedActivityId")
  public void setFailedActivityId(String activityId) {
    this.failedActivityId = activityId;
  }

  @OperatonQueryParam("causeIncidentId")
  public void setCauseIncidentId(String causeIncidentId) {
    this.causeIncidentId = causeIncidentId;
  }

  @OperatonQueryParam("rootCauseIncidentId")
  public void setRootCauseIncidentId(String rootCauseIncidentId) {
    this.rootCauseIncidentId = rootCauseIncidentId;
  }

  @OperatonQueryParam("configuration")
  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  @OperatonQueryParam("historyConfiguration")
  public void setHistoryConfiguration(String historyConfiguration) {
    this.historyConfiguration = historyConfiguration;
  }

  @OperatonQueryParam(value = "open", converter = BooleanConverter.class)
  public void setOpen(Boolean open) {
    this.open = open;
  }

  @OperatonQueryParam(value = "resolved", converter = BooleanConverter.class)
  public void setResolved(Boolean resolved) {
    this.resolved = resolved;
  }

  @OperatonQueryParam(value = "deleted", converter = BooleanConverter.class)
  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
  }

  @OperatonQueryParam(value = "withoutTenantId", converter = BooleanConverter.class)
  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  @OperatonQueryParam(value = "jobDefinitionIdIn", converter = StringListConverter.class)
  public void setJobDefinitionIdIn(List<String> jobDefinitionIds) {
    this.jobDefinitionIds = jobDefinitionIds;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected HistoricIncidentQuery createNewQuery(ProcessEngine engine) {
    return engine.getHistoryService().createHistoricIncidentQuery();
  }

  @Override
  protected void applyFilters(HistoricIncidentQuery query) {
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
    if (processDefinitionId != null) {
      query.processDefinitionId(processDefinitionId);
    }
    if (processDefinitionKey != null) {
      query.processDefinitionKey(processDefinitionKey);
    }
    if (processDefinitionKeyIn != null && processDefinitionKeyIn.length > 0) {
      query.processDefinitionKeyIn(processDefinitionKeyIn);
    }
    if (processInstanceId != null) {
      query.processInstanceId(processInstanceId);
    }
    if (executionId != null) {
      query.executionId(executionId);
    }
    if (createTimeBefore != null) {
      query.createTimeBefore(createTimeBefore);
    }
    if (createTimeAfter != null) {
      query.createTimeAfter(createTimeAfter);
    }
    if (endTimeBefore != null) {
      query.endTimeBefore(endTimeBefore);
    }
    if (endTimeAfter != null) {
      query.endTimeAfter(endTimeAfter);
    }
    if (activityId != null) {
      query.activityId(activityId);
    }
    if (failedActivityId != null) {
      query.failedActivityId(failedActivityId);
    }
    if (causeIncidentId != null) {
      query.causeIncidentId(causeIncidentId);
    }
    if (rootCauseIncidentId != null) {
      query.rootCauseIncidentId(rootCauseIncidentId);
    }
    if (configuration != null) {
      query.configuration(configuration);
    }
    if (historyConfiguration != null) {
      query.historyConfiguration(historyConfiguration);
    }
    if (open != null) {
      query.open();
    }
    if (resolved != null && resolved) {
      query.resolved();
    }
    if (deleted != null && deleted) {
      query.deleted();
    }
    if (tenantIds != null && !tenantIds.isEmpty()) {
      query.tenantIdIn(tenantIds.toArray(new String[tenantIds.size()]));
    }
    if (TRUE.equals(withoutTenantId)) {
      query.withoutTenantId();
    }
    if (jobDefinitionIds != null && !jobDefinitionIds.isEmpty()) {
      query.jobDefinitionIdIn(jobDefinitionIds.toArray(new String[jobDefinitionIds.size()]));
    }
  }

  @Override
  protected void applySortBy(HistoricIncidentQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (sortBy.equals(SORT_BY_INCIDENT_ID)) {
      query.orderByIncidentId();
    } else if (sortBy.equals(SORT_BY_INCIDENT_MESSAGE)) {
      query.orderByIncidentMessage();
    } else if (sortBy.equals(SORT_BY_CREATE_TIME)) {
      query.orderByCreateTime();
    } else if (sortBy.equals(SORT_BY_END_TIME)) {
      query.orderByEndTime();
    } else if (sortBy.equals(SORT_BY_INCIDENT_TYPE)) {
      query.orderByIncidentType();
    } else if (sortBy.equals(SORT_BY_EXECUTION_ID)) {
      query.orderByExecutionId();
    } else if (sortBy.equals(SORT_BY_ACTIVITY_ID)) {
      query.orderByActivityId();
    } else if (sortBy.equals(SORT_BY_PROCESS_INSTANCE_ID)) {
      query.orderByProcessInstanceId();
    } else if (sortBy.equals(SORT_BY_PROCESS_DEFINITION_KEY)) {
      query.orderByProcessDefinitionKey();
    } else if (sortBy.equals(SORT_BY_PROCESS_DEFINITION_ID)) {
      query.orderByProcessDefinitionId();
    } else if (sortBy.equals(SORT_BY_CAUSE_INCIDENT_ID)) {
      query.orderByCauseIncidentId();
    } else if (sortBy.equals(SORT_BY_ROOT_CAUSE_INCIDENT_ID)) {
      query.orderByRootCauseIncidentId();
    } else if (sortBy.equals(SORT_BY_CONFIGURATION)) {
      query.orderByConfiguration();
    } else if (sortBy.equals(SORT_BY_HISTORY_CONFIGURATION)) {
      query.orderByHistoryConfiguration();
    } else if (sortBy.equals(SORT_BY_TENANT_ID)) {
      query.orderByTenantId();
    } else if (sortBy.equals(SORT_BY_INCIDENT_STATE)) {
      query.orderByIncidentState();
    }
  }

}
