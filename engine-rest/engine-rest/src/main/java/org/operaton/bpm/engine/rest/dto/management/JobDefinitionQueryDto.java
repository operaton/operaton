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
package org.operaton.bpm.engine.rest.dto.management;

import static java.lang.Boolean.TRUE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.converter.BooleanConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringListConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author roman.smirnov
 */
public class JobDefinitionQueryDto extends AbstractQueryDto<JobDefinitionQuery> {

  private static final String SORT_BY_JOB_DEFINITION_ID = "jobDefinitionId";
  private static final String SORT_BY_ACTIVITY_ID = "activityId";
  private static final String SORT_BY_PROCESS_DEFINITION_ID = "processDefinitionId";
  private static final String SORT_BY_PROCESS_DEFINITION_KEY = "processDefinitionKey";
  private static final String SORT_BY_JOB_TYPE = "jobType";
  private static final String SORT_BY_JOB_CONFIGURATION = "jobConfiguration";
  private static final String SORT_BY_TENANT_ID = "tenantId";

  private static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();

    VALID_SORT_BY_VALUES.add(SORT_BY_JOB_DEFINITION_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_ACTIVITY_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_DEFINITION_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_DEFINITION_KEY);
    VALID_SORT_BY_VALUES.add(SORT_BY_JOB_TYPE);
    VALID_SORT_BY_VALUES.add(SORT_BY_JOB_CONFIGURATION);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
  }

  protected String jobDefinitionId;
  protected String[] activityIdIn;
  protected String processDefinitionId;
  protected String processDefinitionKey;
  protected String jobType;
  protected String jobConfiguration;
  protected Boolean active;
  protected Boolean suspended;
  protected Boolean withOverridingJobPriority;
  protected List<String> tenantIds;
  protected Boolean withoutTenantId;
  protected Boolean includeJobDefinitionsWithoutTenantId;

  public JobDefinitionQueryDto() {}

  public JobDefinitionQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("jobDefinitionId")
  public void setJobDefinitionId(String jobDefinitionId) {
    this.jobDefinitionId = jobDefinitionId;
  }

  @OperatonQueryParam(value="activityIdIn", converter = StringArrayConverter.class)
  public void setActivityIdIn(String[] activityIdIn) {
    this.activityIdIn = activityIdIn;
  }

  @OperatonQueryParam("processDefinitionId")
  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  @OperatonQueryParam("processDefinitionKey")
  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  @OperatonQueryParam("jobType")
  public void setJobType(String jobType) {
    this.jobType = jobType;
  }

  @OperatonQueryParam("jobConfiguration")
  public void setJobConfiguration(String jobConfiguration) {
    this.jobConfiguration = jobConfiguration;
  }

  @OperatonQueryParam(value="active", converter = BooleanConverter.class)
  public void setActive(Boolean active) {
    this.active = active;
  }

  @OperatonQueryParam(value="suspended", converter = BooleanConverter.class)
  public void setSuspended(Boolean suspended) {
    this.suspended = suspended;
  }

  @OperatonQueryParam(value="withOverridingJobPriority", converter = BooleanConverter.class)
  public void setWithOverridingJobPriority(Boolean withOverridingJobPriority) {
    this.withOverridingJobPriority = withOverridingJobPriority;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
  }

  @OperatonQueryParam(value = "withoutTenantId", converter = BooleanConverter.class)
  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  @OperatonQueryParam(value = "includeJobDefinitionsWithoutTenantId", converter = BooleanConverter.class)
  public void setIncludeJobDefinitionsWithoutTenantId(Boolean includeJobDefinitionsWithoutTenantId) {
    this.includeJobDefinitionsWithoutTenantId = includeJobDefinitionsWithoutTenantId;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected JobDefinitionQuery createNewQuery(ProcessEngine engine) {
    return engine.getManagementService().createJobDefinitionQuery();
  }

  @Override
  protected void applyFilters(JobDefinitionQuery query) {
    if (jobDefinitionId != null) {
      query.jobDefinitionId(jobDefinitionId);
    }

    if (activityIdIn != null && activityIdIn.length > 0) {
      query.activityIdIn(activityIdIn);
    }

    if (processDefinitionId != null) {
      query.processDefinitionId(processDefinitionId);
    }

    if (processDefinitionKey != null) {
      query.processDefinitionKey(processDefinitionKey);
    }

    if (jobType != null) {
      query.jobType(jobType);
    }

    if (jobConfiguration != null) {
      query.jobConfiguration(jobConfiguration);
    }

    if (TRUE.equals(active)) {
      query.active();
    }

    if (TRUE.equals(suspended)) {
      query.suspended();
    }

    if (TRUE.equals(withOverridingJobPriority)) {
      query.withOverridingJobPriority();
    }

    if (tenantIds != null && !tenantIds.isEmpty()) {
      query.tenantIdIn(tenantIds.toArray(new String[tenantIds.size()]));
    }
    if (TRUE.equals(withoutTenantId)) {
      query.withoutTenantId();
    }
    if (TRUE.equals(includeJobDefinitionsWithoutTenantId)) {
      query.includeJobDefinitionsWithoutTenantId();
    }
  }

  @Override
  protected void applySortBy(JobDefinitionQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (SORT_BY_JOB_DEFINITION_ID.equals(sortBy)) {
      query.orderByJobDefinitionId();
    } else if (SORT_BY_ACTIVITY_ID.equals(sortBy)) {
      query.orderByActivityId();
    } else if (SORT_BY_PROCESS_DEFINITION_ID.equals(sortBy)) {
      query.orderByProcessDefinitionId();
    } else if (SORT_BY_PROCESS_DEFINITION_KEY.equals(sortBy)) {
      query.orderByProcessDefinitionKey();
    } else if (SORT_BY_JOB_TYPE.equals(sortBy)) {
      query.orderByJobType();
    } else if (SORT_BY_JOB_CONFIGURATION.equals(sortBy)) {
      query.orderByJobConfiguration();
    } else if (SORT_BY_TENANT_ID.equals(sortBy)) {
      query.orderByTenantId();
    }
  }

}
