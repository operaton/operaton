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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.CleanableHistoricDecisionInstanceReport;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.converter.BooleanConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CleanableHistoricDecisionInstanceReportDto extends AbstractQueryDto<CleanableHistoricDecisionInstanceReport>{

  protected String[] decisionDefinitionIdIn;
  protected String[] decisionDefinitionKeyIn;
  protected String[] tenantIdIn;
  protected Boolean withoutTenantId;
  protected Boolean compact;

  protected static final String SORT_BY_FINISHED_VALUE = "finished";

  public static final List<String> VALID_SORT_BY_VALUES;

  static {
    VALID_SORT_BY_VALUES = new ArrayList<>();
    VALID_SORT_BY_VALUES.add(SORT_BY_FINISHED_VALUE);
  }

  public CleanableHistoricDecisionInstanceReportDto() {
  }

  public CleanableHistoricDecisionInstanceReportDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam(value = "decisionDefinitionIdIn", converter = StringArrayConverter.class)
  public void setDecisionDefinitionIdIn(String[] decisionDefinitionIdIn) {
    this.decisionDefinitionIdIn = decisionDefinitionIdIn;
  }

  @OperatonQueryParam(value = "decisionDefinitionKeyIn", converter = StringArrayConverter.class)
  public void setDecisionDefinitionKeyIn(String[] decisionDefinitionKeyIn) {
    this.decisionDefinitionKeyIn = decisionDefinitionKeyIn;
  }

  @OperatonQueryParam(value = "tenantIdIn", converter = StringArrayConverter.class)
  public void setTenantIdIn(String[] tenantIdIn) {
    this.tenantIdIn = tenantIdIn;
  }

  @OperatonQueryParam(value = "withoutTenantId", converter = BooleanConverter.class)
  public void setWithoutTenantId(Boolean withoutTenantId) {
    this.withoutTenantId = withoutTenantId;
  }

  @OperatonQueryParam(value = "compact", converter = BooleanConverter.class)
  public void setCompact(Boolean compact) {
    this.compact = compact;
  }

  @Override
  protected boolean isValidSortByValue(String value) {
    return VALID_SORT_BY_VALUES.contains(value);
  }

  @Override
  protected CleanableHistoricDecisionInstanceReport createNewQuery(ProcessEngine engine) {
    return engine.getHistoryService().createCleanableHistoricDecisionInstanceReport();
  }

  @Override
  protected void applyFilters(CleanableHistoricDecisionInstanceReport query) {
    if (decisionDefinitionIdIn != null && decisionDefinitionIdIn.length > 0) {
      query.decisionDefinitionIdIn(decisionDefinitionIdIn);
    }
    if (decisionDefinitionKeyIn != null && decisionDefinitionKeyIn.length > 0) {
      query.decisionDefinitionKeyIn(decisionDefinitionKeyIn);
    }
    if (Boolean.TRUE.equals(withoutTenantId)) {
      query.withoutTenantId();
    }
    if (tenantIdIn != null && tenantIdIn.length > 0) {
      query.tenantIdIn(tenantIdIn);
    }
    if (Boolean.TRUE.equals(compact)) {
      query.compact();
    }
  }

  @Override
  protected void applySortBy(CleanableHistoricDecisionInstanceReport query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (sortBy.equals(SORT_BY_FINISHED_VALUE)) {
      query.orderByFinished();
    }
  }
}
