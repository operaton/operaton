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
import java.util.Set;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.ConditionQueryParameterDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.converter.BooleanConverter;
import org.operaton.bpm.engine.rest.dto.converter.ConditionListConverter;
import org.operaton.bpm.engine.rest.dto.converter.DateConverter;
import org.operaton.bpm.engine.rest.dto.converter.LongConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringListConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringSetConverter;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.commons.utils.CollectionUtil;

import static java.lang.Boolean.TRUE;

public class JobQueryDto extends AbstractQueryDto<JobQuery> {

  private static final String SORT_BY_JOB_ID_VALUE = "jobId";
  private static final String SORT_BY_EXECUTION_ID_VALUE = "executionId";
  private static final String SORT_BY_PROCESS_INSTANCE_ID_VALUE = "processInstanceId";
  private static final String SORT_BY_PROCESS_DEFINITION_ID_VALUE = "processDefinitionId";
  private static final String SORT_BY_PROCESS_DEFINITION_KEY_VALUE = "processDefinitionKey";
  private static final String SORT_BY_JOB_RETRIES_VALUE = "jobRetries";
  private static final String SORT_BY_JOB_DUEDATE_VALUE = "jobDueDate";
  private static final String SORT_BY_JOB_PRIORITY_VALUE = "jobPriority";
  private static final String SORT_BY_TENANT_ID = "tenantId";

  private static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_JOB_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_EXECUTION_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_INSTANCE_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_DEFINITION_ID_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_DEFINITION_KEY_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_JOB_RETRIES_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_JOB_DUEDATE_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_JOB_PRIORITY_VALUE);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
  }

  protected String activityId;
  protected String jobId;
  protected Set<String> jobIds;
  protected String executionId;
  protected String processInstanceId;
  protected Set<String> processInstanceIds;
  protected String processDefinitionId;
  protected String processDefinitionKey;
  protected Boolean withRetriesLeft;
  protected Boolean executable;
  protected Boolean timers;
  protected Boolean messages;
  protected Boolean withException;
  protected String exceptionMessage;
  protected String failedActivityId;
  protected Boolean noRetriesLeft;
  protected Boolean active;
  protected Boolean suspended;
  protected Long priorityHigherThanOrEquals;
  protected Long priorityLowerThanOrEquals;
  protected String jobDefinitionId;
  protected List<String> tenantIds;
  protected Boolean withoutTenantId;
  protected Boolean includeJobsWithoutTenantId;
  protected Boolean acquired;

  protected List<ConditionQueryParameterDto> dueDates;
  protected List<ConditionQueryParameterDto> createTimes;

  public JobQueryDto() {}

  public JobQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("activityId")
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  @OperatonQueryParam("failedActivityId")
  public void setFailedActivityId(String activityId) {
    this.failedActivityId = activityId;
  }

  @OperatonQueryParam("jobId")
  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  @OperatonQueryParam(value = "jobIds", converter = StringSetConverter.class)
  public void setJobIds(Set<String> jobIds) {
    this.jobIds = jobIds;
  }

  @OperatonQueryParam("executionId")
  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  @OperatonQueryParam("processInstanceId")
  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  @OperatonQueryParam(value = "processInstanceIds", converter = StringSetConverter.class)
  public void setProcessInstanceIds(Set<String> processInstanceIds) {
    this.processInstanceIds = processInstanceIds;
  }

  @OperatonQueryParam("processDefinitionId")
  public void setProcessDefinitionId(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  @OperatonQueryParam("processDefinitionKey")
  public void setProcessDefinitionKey(String processDefinitionKey) {
    this.processDefinitionKey = processDefinitionKey;
  }

  @OperatonQueryParam(value="withRetriesLeft", converter = BooleanConverter.class)
  public void setWithRetriesLeft(Boolean withRetriesLeft) {
    this.withRetriesLeft = withRetriesLeft;
  }

  @OperatonQueryParam(value="executable", converter = BooleanConverter.class)
  public void setExecutable(Boolean executable) {
    this.executable = executable;
  }

  @OperatonQueryParam(value="timers", converter = BooleanConverter.class)
  public void setTimers(Boolean timers) {
    this.timers = timers;
  }

  @OperatonQueryParam(value="withException", converter = BooleanConverter.class)
  public void setWithException(Boolean withException) {
    this.withException = withException;
  }

  @OperatonQueryParam(value="messages", converter = BooleanConverter.class)
  public void setMessages(Boolean messages) {
    this.messages = messages;
  }

  @OperatonQueryParam("exceptionMessage")
  public void setExceptionMessage(String exceptionMessage) {
    this.exceptionMessage = exceptionMessage;
  }

  @OperatonQueryParam(value = "dueDates", converter = ConditionListConverter.class)
  public void setDueDates(List<ConditionQueryParameterDto> dueDates) {
    this.dueDates = dueDates;
  }

  @OperatonQueryParam(value = "createTimes", converter = ConditionListConverter.class)
  public void setCreateTimes(List<ConditionQueryParameterDto> createTimes) {
    this.createTimes = createTimes;
  }

  @OperatonQueryParam(value="noRetriesLeft", converter = BooleanConverter.class)
  public void setNoRetriesLeft(Boolean noRetriesLeft) {
    this.noRetriesLeft = noRetriesLeft;
  }

  @OperatonQueryParam(value="active", converter = BooleanConverter.class)
  public void setActive(Boolean active) {
    this.active = active;
  }

  @OperatonQueryParam(value="suspended", converter = BooleanConverter.class)
  public void setSuspended(Boolean suspended) {
    this.suspended = suspended;
  }

  @OperatonQueryParam(value="priorityHigherThanOrEquals", converter = LongConverter.class)
  public void setPriorityHigherThanOrEquals(Long priorityHigherThanOrEquals) {
    this.priorityHigherThanOrEquals = priorityHigherThanOrEquals;
  }

  @OperatonQueryParam(value="priorityLowerThanOrEquals", converter = LongConverter.class)
  public void setPriorityLowerThanOrEquals(Long priorityLowerThanOrEquals) {
    this.priorityLowerThanOrEquals = priorityLowerThanOrEquals;
  }

  @OperatonQueryParam("jobDefinitionId")
  public void setJobDefinitionId(String jobDefinitionId) {
    this.jobDefinitionId = jobDefinitionId;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
  }

  @OperatonQueryParam(value = "withoutTenantId", converter = BooleanConverter.class)
  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  @OperatonQueryParam(value = "includeJobsWithoutTenantId", converter = BooleanConverter.class)
  public void setIncludeJobsWithoutTenantId(Boolean includeJobsWithoutTenantId) {
    this.includeJobsWithoutTenantId = includeJobsWithoutTenantId;
  }

  @OperatonQueryParam(value="acquired", converter = BooleanConverter.class)
  public void setAcquired(Boolean acquired) {
    this.acquired = acquired;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected JobQuery createNewQuery(ProcessEngine engine) {
    return engine.getManagementService().createJobQuery();
  }

  private abstract class ApplyDates {
    void run(List<ConditionQueryParameterDto> dates) {
      DateConverter dateConverter = new DateConverter();
      dateConverter.setObjectMapper(objectMapper);

      for (ConditionQueryParameterDto conditionQueryParam : dates) {
        String op = conditionQueryParam.getOperator();
        Date date;

        try {
          date = dateConverter.convertQueryParameterToType((String) conditionQueryParam.getValue());
        } catch (RestException e) {
          throw new InvalidRequestException(e.getStatus(), e, "Invalid %s format: %s".formatted(fieldName(), e.getMessage()));
        }

        if (ConditionQueryParameterDto.GREATER_THAN_OPERATOR_NAME.equals(op)) {
          setGreaterThan(date);
        } else if (ConditionQueryParameterDto.LESS_THAN_OPERATOR_NAME.equals(op)) {
          setLowerThan(date);
        } else {
          throw new InvalidRequestException(Status.BAD_REQUEST, "Invalid %s comparator specified: %s".formatted(fieldName(), op));
        }
      }
    }

    /**
     * @return a descriptive name of the target field, used in error-messages
     */
    abstract String fieldName();

    abstract void setGreaterThan(Date date);

    abstract void setLowerThan(Date date);
  }

  @Override
  protected void applyFilters(final JobQuery query) {
    if (activityId != null){
      query.activityId(activityId);
    }

    if (jobId != null) {
      query.jobId(jobId);
    }

    if (!CollectionUtil.isEmpty(jobIds)) {
      query.jobIds(jobIds);
    }

    if (executionId != null) {
      query.executionId(executionId);
    }

    if (processInstanceId != null) {
      query.processInstanceId(processInstanceId);
    }

    if (!CollectionUtil.isEmpty(processInstanceIds)) {
      query.processInstanceIds(processInstanceIds);
    }

    if (processDefinitionId != null) {
      query.processDefinitionId(processDefinitionId);
    }

    if (processDefinitionKey != null) {
      query.processDefinitionKey(processDefinitionKey);
    }

    if (TRUE.equals(withRetriesLeft)) {
      query.withRetriesLeft();
    }

    if (TRUE.equals(executable)) {
      query.executable();
    }

    if (TRUE.equals(timers)) {
      if (messages != null && messages) {
        throw new InvalidRequestException(Status.BAD_REQUEST, "Parameter timers cannot be used together with parameter messages.");
      }
      query.timers();
    }

    if (TRUE.equals(messages)) {
      if (timers != null && timers) {
        throw new InvalidRequestException(Status.BAD_REQUEST, "Parameter messages cannot be used together with parameter timers.");
      }
      query.messages();
    }

    if (TRUE.equals(withException)) {
      query.withException();
    }

    if (exceptionMessage != null) {
      query.exceptionMessage(exceptionMessage);
    }

    if (failedActivityId != null) {
      query.failedActivityId(failedActivityId);
    }

    if (TRUE.equals(noRetriesLeft)) {
      query.noRetriesLeft();
    }

    if (TRUE.equals(active)) {
      query.active();
    }

    if (TRUE.equals(suspended)) {
      query.suspended();
    }

    if (priorityHigherThanOrEquals != null) {
      query.priorityHigherThanOrEquals(priorityHigherThanOrEquals);
    }

    if (priorityLowerThanOrEquals != null) {
      query.priorityLowerThanOrEquals(priorityLowerThanOrEquals);
    }

    if (jobDefinitionId != null) {
      query.jobDefinitionId(jobDefinitionId);
    }

    if (dueDates != null) {
      new ApplyDates() {
        @Override
        void setGreaterThan(Date date) {
          query.duedateHigherThan(date);
        }

        @Override
        void setLowerThan(Date date) {
          query.duedateLowerThan(date);
        }

        @Override
        String fieldName() {
          return "due date";
        }
      }.run(dueDates);
    }

    if (createTimes != null) {
      new ApplyDates() {
        @Override
        void setGreaterThan(Date date) {
          query.createdAfter(date);
        }

        @Override
        void setLowerThan(Date date) {
          query.createdBefore(date);
        }

        @Override
        String fieldName() {
          return "create time";
        }
      }.run(createTimes);
    }

    if (!CollectionUtil.isEmpty(tenantIds)) {
      query.tenantIdIn(tenantIds.toArray(new String[0]));
    }
    if (TRUE.equals(withoutTenantId)) {
      query.withoutTenantId();
    }
    if (TRUE.equals(includeJobsWithoutTenantId)) {
      query.includeJobsWithoutTenantId();
    }
    if (TRUE.equals(acquired)) {
      query.acquired();
    }
  }

  @Override
  protected void applySortBy(JobQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (SORT_BY_JOB_ID_VALUE.equals(sortBy)) {
      query.orderByJobId();
    } else if (SORT_BY_EXECUTION_ID_VALUE.equals(sortBy)) {
      query.orderByExecutionId();
    } else if (SORT_BY_PROCESS_INSTANCE_ID_VALUE.equals(sortBy)) {
      query.orderByProcessInstanceId();
    } else if (SORT_BY_PROCESS_DEFINITION_ID_VALUE.equals(sortBy)) {
      query.orderByProcessDefinitionId();
    } else if (SORT_BY_PROCESS_DEFINITION_KEY_VALUE.equals(sortBy)) {
      query.orderByProcessDefinitionKey();
    } else if (SORT_BY_JOB_RETRIES_VALUE.equals(sortBy)) {
      query.orderByJobRetries();
    } else if (SORT_BY_JOB_DUEDATE_VALUE.equals(sortBy)) {
      query.orderByJobDuedate();
    } else if (SORT_BY_JOB_PRIORITY_VALUE.equals(sortBy)) {
      query.orderByJobPriority();
    } else if (SORT_BY_TENANT_ID.equals(sortBy)) {
      query.orderByTenantId();
    }
  }

}
