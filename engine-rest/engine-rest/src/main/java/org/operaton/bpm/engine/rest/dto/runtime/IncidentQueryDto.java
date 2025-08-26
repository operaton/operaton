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
import java.util.Date;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.converter.DateConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringListConverter;
import org.operaton.bpm.engine.runtime.IncidentQuery;

/**
 * @author Roman Smirnov
 *
 */
public class IncidentQueryDto extends AbstractQueryDto<IncidentQuery> {

  private static final String SORT_BY_INCIDENT_ID = "incidentId";
  private static final String SORT_BY_INCIDENT_MESSAGE = "incidentMessage";
  private static final String SORT_BY_INCIDENT_TIMESTAMP = "incidentTimestamp";
  private static final String SORT_BY_INCIDENT_TYPE = "incidentType";
  private static final String SORT_BY_EXECUTION_ID = "executionId";
  private static final String SORT_BY_ACTIVITY_ID = "activityId";
  private static final String SORT_BY_PROCESS_INSTANCE_ID = "processInstanceId";
  private static final String SORT_BY_PROCESS_DEFINITION_ID = "processDefinitionId";
  private static final String SORT_BY_CAUSE_INCIDENT_ID = "causeIncidentId";
  private static final String SORT_BY_ROOT_CAUSE_INCIDENT_ID = "rootCauseIncidentId";
  private static final String SORT_BY_CONFIGURATION = "configuration";
  private static final String SORT_BY_TENANT_ID = "tenantId";

  private static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_INCIDENT_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_INCIDENT_MESSAGE);
    VALID_SORT_BY_VALUES.add(SORT_BY_INCIDENT_TIMESTAMP);
    VALID_SORT_BY_VALUES.add(SORT_BY_INCIDENT_TYPE);
    VALID_SORT_BY_VALUES.add(SORT_BY_EXECUTION_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_ACTIVITY_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_INSTANCE_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_DEFINITION_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_CAUSE_INCIDENT_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_ROOT_CAUSE_INCIDENT_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_CONFIGURATION);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
  }

  protected String incidentId;
  protected String incidentType;
  protected String incidentMessage;
  protected String incidentMessageLike;
  protected String processDefinitionId;
  protected String[] processDefinitionKeyIn;
  protected String processInstanceId;
  protected String executionId;
  protected Date incidentTimestampBefore;
  protected Date incidentTimestampAfter;
  protected String activityId;
  protected String failedActivityId;
  protected String causeIncidentId;
  protected String rootCauseIncidentId;
  protected String configuration;
  protected List<String> tenantIds;
  protected List<String> jobDefinitionIds;

  public IncidentQueryDto() {}

  public IncidentQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
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

  @OperatonQueryParam(value="incidentTimestampAfter", converter= DateConverter.class)
  public void setIncidentTimestampAfter(Date incidentTimestampAfter) {
    this.incidentTimestampAfter = incidentTimestampAfter;
  }

  @OperatonQueryParam(value="incidentTimestampBefore", converter= DateConverter.class)
  public void setIncidentTimestampBefore(Date incidentTimestampBefore) {
    this.incidentTimestampBefore = incidentTimestampBefore;
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

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
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
  protected IncidentQuery createNewQuery(ProcessEngine engine) {
    return engine.getRuntimeService().createIncidentQuery();
  }

  @Override
  protected void applyFilters(IncidentQuery query) {
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
    if (processDefinitionKeyIn != null && processDefinitionKeyIn.length > 0) {
      query.processDefinitionKeyIn(processDefinitionKeyIn);
    }
    if (processInstanceId != null) {
      query.processInstanceId(processInstanceId);
    }
    if (executionId != null) {
      query.executionId(executionId);
    }
    if (incidentTimestampBefore != null) {
      query.incidentTimestampBefore(incidentTimestampBefore);
    }
    if (incidentTimestampAfter != null) {
      query.incidentTimestampAfter(incidentTimestampAfter);
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
    if (tenantIds != null && !tenantIds.isEmpty()) {
      query.tenantIdIn(tenantIds.toArray(new String[tenantIds.size()]));
    }
    if (jobDefinitionIds != null && !jobDefinitionIds.isEmpty()) {
      query.jobDefinitionIdIn(jobDefinitionIds.toArray(new String[jobDefinitionIds.size()]));
    }
  }

  @Override
  protected void applySortBy(IncidentQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (SORT_BY_INCIDENT_ID.equals(sortBy)) {
      query.orderByIncidentId();
    } else if (SORT_BY_INCIDENT_MESSAGE.equals(sortBy)) {
      query.orderByIncidentMessage();
    } else if (SORT_BY_INCIDENT_TIMESTAMP.equals(sortBy)) {
      query.orderByIncidentTimestamp();
    } else if (SORT_BY_INCIDENT_TYPE.equals(sortBy)) {
      query.orderByIncidentType();
    } else if (SORT_BY_EXECUTION_ID.equals(sortBy)) {
      query.orderByExecutionId();
    } else if (SORT_BY_ACTIVITY_ID.equals(sortBy)) {
      query.orderByActivityId();
    } else if (SORT_BY_PROCESS_INSTANCE_ID.equals(sortBy)) {
      query.orderByProcessInstanceId();
    } else if (SORT_BY_PROCESS_DEFINITION_ID.equals(sortBy)) {
      query.orderByProcessDefinitionId();
    } else if (SORT_BY_CAUSE_INCIDENT_ID.equals(sortBy)) {
      query.orderByCauseIncidentId();
    } else if (SORT_BY_ROOT_CAUSE_INCIDENT_ID.equals(sortBy)) {
      query.orderByRootCauseIncidentId();
    } else if (SORT_BY_CONFIGURATION.equals(sortBy)) {
      query.orderByConfiguration();
    } else if (SORT_BY_TENANT_ID.equals(sortBy)) {
      query.orderByTenantId();
    }
  }

}
