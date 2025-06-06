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

import static java.lang.Boolean.TRUE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.converter.BooleanConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringListConverter;
import org.operaton.bpm.engine.runtime.EventSubscriptionQuery;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EventSubscriptionQueryDto extends AbstractQueryDto<EventSubscriptionQuery> {

  private static final String SORT_BY_TENANT_ID = "tenantId";
  private static final String SORT_BY_CREATED = "created";

  private static final List<String> VALID_SORT_BY_VALUES;
  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_TENANT_ID);
    VALID_SORT_BY_VALUES.add(SORT_BY_CREATED);
  }

  private String eventSubscriptionId;
  private String eventName;
  private String eventType;
  private String executionId;
  private String processInstanceId;
  private String activityId;
  private List<String> tenantIdIn;
  private Boolean withoutTenantId;
  private Boolean includeEventSubscriptionsWithoutTenantId;

  public EventSubscriptionQueryDto() {

  }

  public EventSubscriptionQueryDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  public String getEventSubscriptionId() {
    return eventSubscriptionId;
  }

  @OperatonQueryParam("eventSubscriptionId")
  public void setEventSubscriptionId(String eventSubscriptionId) {
    this.eventSubscriptionId = eventSubscriptionId;
  }

  public String getEventName() {
    return eventName;
  }

  @OperatonQueryParam("eventName")
  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public String getEventType() {
    return eventType;
  }

  @OperatonQueryParam("eventType")
  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getExecutionId() {
    return executionId;
  }

  @OperatonQueryParam("executionId")
  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public String getProcessInstanceId() {
    return processInstanceId;
  }

  @OperatonQueryParam("processInstanceId")
  public void setProcessInstanceId(String processInstanceId) {
    this.processInstanceId = processInstanceId;
  }

  public String getActivityId() {
    return activityId;
  }

  @OperatonQueryParam("activityId")
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public List<String> getTenantIdIn() {
    return tenantIdIn;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringListConverter.class)
  public void setTenantIdIn(List<String> tenantIdIn) {
    this.tenantIdIn = tenantIdIn;
  }

  public Boolean getWithoutTenantId() {
    return withoutTenantId;
  }

  @OperatonQueryParam(value = "withoutTenantId", converter = BooleanConverter.class)
  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  public Boolean getIncludeEventSubscriptionsWithoutTenantId() {
    return includeEventSubscriptionsWithoutTenantId;
  }

  @OperatonQueryParam(value = "includeEventSubscriptionsWithoutTenantId", converter = BooleanConverter.class)
  public void setIncludeEventSubscriptionsWithoutTenantId(Boolean includeEventSubscriptionsWithoutTenantId) {
    this.includeEventSubscriptionsWithoutTenantId = includeEventSubscriptionsWithoutTenantId;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected EventSubscriptionQuery createNewQuery(ProcessEngine engine) {
    return engine.getRuntimeService().createEventSubscriptionQuery();
  }

  @Override
  protected void applyFilters(EventSubscriptionQuery query) {
    if (eventSubscriptionId != null) {
      query.eventSubscriptionId(eventSubscriptionId);
    }
    if (eventName != null) {
      query.eventName(eventName);
    }
    if (eventType != null) {
      query.eventType(eventType);
    }
    if (executionId != null) {
      query.executionId(executionId);
    }
    if (processInstanceId != null) {
      query.processInstanceId(processInstanceId);
    }
    if (activityId != null) {
      query.activityId(activityId);
    }
    if (tenantIdIn != null && !tenantIdIn.isEmpty()) {
      query.tenantIdIn(tenantIdIn.toArray(new String[tenantIdIn.size()]));
    }
    if (TRUE.equals(withoutTenantId)) {
      query.withoutTenantId();
    }
    if (TRUE.equals(includeEventSubscriptionsWithoutTenantId)) {
      query.includeEventSubscriptionsWithoutTenantId();
    }
  }

  @Override
  protected void applySortBy(EventSubscriptionQuery query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (sortBy.equals(SORT_BY_CREATED)) {
      query.orderByCreated();
    } else if (sortBy.equals(SORT_BY_TENANT_ID)) {
      query.orderByTenantId();
    }
  }
}
