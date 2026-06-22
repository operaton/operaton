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

import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.MultivaluedMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.CleanableHistoricCaseInstanceReport;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.converter.BooleanConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;

public class CleanableHistoricCaseInstanceReportDto extends AbstractQueryDto<CleanableHistoricCaseInstanceReport> {

  protected String[] caseDefinitionIdIn;
  protected String[] caseDefinitionKeyIn;
  protected String[] tenantIdIn;
  protected Boolean withoutTenantId;
  protected Boolean compact;

  protected static final String SORT_BY_FINISHED_VALUE = "finished";

  private static final List<String> VALID_SORT_BY_VALUES = List.of(SORT_BY_FINISHED_VALUE);

  public CleanableHistoricCaseInstanceReportDto() {
  }

  public CleanableHistoricCaseInstanceReportDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam(value = "caseDefinitionIdIn", converter = StringArrayConverter.class)
  public void setCaseDefinitionIdIn(String[] caseDefinitionIdIn) {
    this.caseDefinitionIdIn = caseDefinitionIdIn;
  }

  @OperatonQueryParam(value = "caseDefinitionKeyIn", converter = StringArrayConverter.class)
  public void setCaseDefinitionKeyIn(String[] caseDefinitionKeyIn) {
    this.caseDefinitionKeyIn = caseDefinitionKeyIn;
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
  protected CleanableHistoricCaseInstanceReport createNewQuery(ProcessEngine engine) {
    return engine.getHistoryService().createCleanableHistoricCaseInstanceReport();
  }

  @Override
  protected void applyFilters(CleanableHistoricCaseInstanceReport query) {
    if (caseDefinitionIdIn != null && caseDefinitionIdIn.length > 0) {
      query.caseDefinitionIdIn(caseDefinitionIdIn);
    }
    if (caseDefinitionKeyIn != null && caseDefinitionKeyIn.length > 0) {
      query.caseDefinitionKeyIn(caseDefinitionKeyIn);
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
  protected void applySortBy(CleanableHistoricCaseInstanceReport query, String sortBy, Map<String, Object> parameters, ProcessEngine engine) {
    if (SORT_BY_FINISHED_VALUE.equals(sortBy)) {
      query.orderByFinished();
    }
  }
}
