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
import java.util.Date;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricDetailQuery;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.SortingDto;
import org.operaton.bpm.engine.rest.dto.converter.BooleanConverter;
import org.operaton.bpm.engine.rest.dto.converter.DateConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringListConverter;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;

import static java.lang.Boolean.TRUE;

/**
 * @author Roman Smirnov
 *
 */
public class HistoricDetailQueryDto extends AbstractQueryDto<HistoricDetailQuery> {

  private static final String SORT_BY_PROCESS_INSTANCE_ID = "processInstanceId";
  private static final String SORT_BY_VARIABLE_NAME = "variableName";
  private static final String SORT_BY_VARIABLE_TYPE = "variableType";
  private static final String SORT_BY_VARIABLE_REVISION = "variableRevision";
  private static final String SORT_BY_FORM_PROPERTY_ID = "formPropertyId";
  private static final String SORT_BY_TIME = "time";
  private static final String SORT_PARTIALLY_BY_OCCURENCE = "occurrence";
  private static final String SORT_BY_TENANT_ID = "tenantId";

  private static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_PROCESS_INSTANCE_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_VARIABLE_NAME);
    VALID_SORT_BY_VALUES.add(SORT_BY_VARIABLE_TYPE);
    VALID_SORT_BY_VALUES.add(SORT_BY_VARIABLE_REVISION);
    VALID_SORT_BY_VALUES.add(SORT_BY_FORM_PROPERTY_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_TIME);
    VALID_SORT_BY_VALUES.add(SORT_PARTIALLY_BY_OCCURENCE);
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
  }

  protected String processInstanceId;
  protected String executionId;
  protected String activityInstanceId;
  protected String caseInstanceId;
  protected String caseExecutionId;
  protected String variableInstanceId;
  protected String[] variableTypeIn;
  protected String variableNameLike;
  protected String taskId;
  protected Boolean formFields;
  protected Boolean variableUpdates;
  protected Boolean excludeTaskDetails;
  protected List<String> tenantIds;
  protected Boolean withoutTenantId;
  protected String[] processInstanceIdIn;
  protected String userOperationId;
  private Date occurredBefore;
  private Date occurredAfter;
  protected Boolean initial;

  public HistoricDetailQueryDto() {
  }

  public HistoricDetailQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam("processInstanceId")
  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  @OperatonQueryParam("executionId")
  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  @OperatonQueryParam("activityInstanceId")
  public void setActivityInstanceId(String activityInstanceId) {
    this.activityInstanceId = activityInstanceId;
  }

  @OperatonQueryParam("caseInstanceId")
  public void setCaseInstanceId(String caseInstanceId) {
    this.caseInstanceId = caseInstanceId;
  }

  @OperatonQueryParam("caseExecutionId")
  public void setCaseExecutionId(String caseExecutionId) {
    this.caseExecutionId = caseExecutionId;
  }

  @OperatonQueryParam("variableInstanceId")
  public void setVariableInstanceId(String variableInstanceId) {
    this.variableInstanceId = variableInstanceId;
  }

  @OperatonQueryParam(value="variableTypeIn", converter = StringArrayConverter.class)
  public void setVariableTypeIn(String[] variableTypeIn) {
    this.variableTypeIn = variableTypeIn;
  }

  @OperatonQueryParam("variableNameLike")
  public void setVariableNameLike(String variableNameLike) {
     this.variableNameLike = variableNameLike;
   }

  @OperatonQueryParam("taskId")
  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  @OperatonQueryParam(value = "formFields", converter = BooleanConverter.class)
  public void setFormFields(Boolean formFields) {
    this.formFields = formFields;
  }

  @OperatonQueryParam(value = "variableUpdates", converter = BooleanConverter.class)
  public void setVariableUpdates(Boolean variableUpdates) {
    this.variableUpdates = variableUpdates;
  }

  @OperatonQueryParam(value = "excludeTaskDetails", converter = BooleanConverter.class)
  public void setExcludeTaskDetails(Boolean excludeTaskDetails) {
    this.excludeTaskDetails = excludeTaskDetails;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIds) {
    this.tenantIds = tenantIds;
  }

  @OperatonQueryParam(value = "withoutTenantId", converter = BooleanConverter.class)
  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  @OperatonQueryParam(value="processInstanceIdIn", converter = StringArrayConverter.class)
  public void setProcessInstanceIdIn(String[] processInstanceIdIn) {
    this.processInstanceIdIn = processInstanceIdIn;
  }

  @OperatonQueryParam("userOperationId")
  public void setUserOperationId(String userOperationId) {
    this.userOperationId = userOperationId;
  }

  @OperatonQueryParam(value = "occurredBefore", converter = DateConverter.class)
  public void setOccurredBefore(Date occurredBefore) {
    this.occurredBefore = occurredBefore;
  }

  @OperatonQueryParam(value = "occurredAfter", converter = DateConverter.class)
  public void setOccurredAfter(Date occurredAfter) {
    this.occurredAfter = occurredAfter;
  }

  @OperatonQueryParam(value = "initial", converter = BooleanConverter.class)
  public void setInitial(Boolean initial) {
    this.initial = initial;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected HistoricDetailQuery createNewQuery(ProcessEngine engine) {
    return engine.getHistoryService().createHistoricDetailQuery();
  }

  @Override
  protected boolean sortOptionsValid() {
    if (sortings != null) {
      for (SortingDto sorting : sortings) {
        String sortingOrder = sorting.getSortOrder();
        String sortingBy = sorting.getSortBy();

        if (!VALID_SORT_BY_VALUES.contains(sortingBy)) {
          throw new InvalidRequestException(Response.Status.BAD_REQUEST, "sortBy parameter has invalid value: %s".formatted(sortingBy));
        }

        if (sortingBy == null || sortingOrder == null) {
          return false;
        }
      }
    }

    return super.sortOptionsValid();
  }

  @Override
  protected void applyFilters(HistoricDetailQuery query) {
    if (processInstanceId != null) {
      query.processInstanceId(processInstanceId);
    }
    if (executionId != null) {
      query.executionId(executionId);
    }
    if (activityInstanceId != null) {
      query.activityInstanceId(activityInstanceId);
    }
    if (caseInstanceId != null) {
      query.caseInstanceId(caseInstanceId);
    }
    if (caseExecutionId != null) {
      query.caseExecutionId(caseExecutionId);
    }
    if (variableInstanceId != null) {
      query.variableInstanceId(variableInstanceId);
    }
    if (variableTypeIn != null && variableTypeIn.length > 0) {
      query.variableTypeIn(variableTypeIn);
    }
    if(variableNameLike != null) {
      query.variableNameLike(variableNameLike);
    }
    if (taskId != null) {
      query.taskId(taskId);
    }
    if (formFields != null) {
      query.formFields();
    }
    if (variableUpdates != null) {
      query.variableUpdates();
    }
    if (excludeTaskDetails != null) {
      query.excludeTaskDetails();
    }
    if (tenantIds != null && !tenantIds.isEmpty()) {
      query.tenantIdIn(tenantIds.toArray(new String[tenantIds.size()]));
    }
    if (TRUE.equals(withoutTenantId)) {
      query.withoutTenantId();
    }
    if (processInstanceIdIn != null && processInstanceIdIn.length > 0) {
      query.processInstanceIdIn(processInstanceIdIn);
    }
    if (userOperationId != null) {
      query.userOperationId(userOperationId);
    }
    if (occurredBefore != null) {
      query.occurredBefore(occurredBefore);
    }
    if (occurredAfter != null) {
      query.occurredAfter(occurredAfter);
    }
    if (TRUE.equals(initial)) {
      query.initial();
    }
  }

  @Override
  protected void applySortBy(HistoricDetailQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (SORT_BY_PROCESS_INSTANCE_ID.equals(sortBy)) {
      query.orderByProcessInstanceId();
    } else if (SORT_BY_VARIABLE_NAME.equals(sortBy)) {
      query.orderByVariableName();
    } else if (SORT_BY_VARIABLE_TYPE.equals(sortBy)) {
      query.orderByVariableType();
    } else if (SORT_BY_VARIABLE_REVISION.equals(sortBy)) {
      query.orderByVariableRevision();
    } else if (SORT_BY_FORM_PROPERTY_ID.equals(sortBy)) {
      query.orderByFormPropertyId();
    } else if (SORT_BY_TIME.equals(sortBy)) {
      query.orderByTime();
    } else if (SORT_PARTIALLY_BY_OCCURENCE.equals(sortBy)) {
      query.orderPartiallyByOccurrence();
    } else if (SORT_BY_TENANT_ID.equals(sortBy)) {
      query.orderByTenantId();
    }
  }

}
