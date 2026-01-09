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
import java.util.List;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricProcessInstanceReport;
import org.operaton.bpm.engine.rest.dto.AbstractReportDto;
import org.operaton.bpm.engine.rest.dto.OperatonQueryParam;
import org.operaton.bpm.engine.rest.dto.converter.DateConverter;
import org.operaton.bpm.engine.rest.dto.converter.StringArrayConverter;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;

/**
 * @author Roman Smirnov
 *
 */
public class HistoricProcessInstanceReportDto extends AbstractReportDto<HistoricProcessInstanceReport> {

  protected String[] processDefinitionIdIn;
  protected String[] processDefinitionKeyIn;
  protected Date startedAfter;
  protected Date startedBefore;

  public static final String REPORT_TYPE_DURATION = "duration";

  private static final List<String> VALID_REPORT_TYPE_VALUES = List.of(REPORT_TYPE_DURATION);

  public HistoricProcessInstanceReportDto() {
  }

  public HistoricProcessInstanceReportDto(ObjectMapper objectMapper, MultivaluedMap<String, String> queryParameters) {
    super(objectMapper, queryParameters);
  }

  @OperatonQueryParam(value = "processDefinitionIdIn", converter = StringArrayConverter.class)
  public void setProcessDefinitionIdIn(String[] processDefinitionIdIn) {
    this.processDefinitionIdIn = processDefinitionIdIn;
  }

  @OperatonQueryParam(value = "processDefinitionKeyIn", converter = StringArrayConverter.class)
  public void setProcessDefinitionKeyIn(String[] processDefinitionKeyIn) {
    this.processDefinitionKeyIn = processDefinitionKeyIn;
  }

  @OperatonQueryParam(value = "startedAfter", converter = DateConverter.class)
  public void setStartedAfter(Date startedAfter) {
    this.startedAfter = startedAfter;
  }

  @OperatonQueryParam(value = "startedBefore", converter = DateConverter.class)
  public void setStartedBefore(Date startedBefore) {
    this.startedBefore = startedBefore;
  }

  @Override
  protected HistoricProcessInstanceReport createNewReportQuery(ProcessEngine engine) {
    return engine.getHistoryService().createHistoricProcessInstanceReport();
  }

  @Override
  protected void applyFilters(HistoricProcessInstanceReport reportQuery) {
    if (processDefinitionIdIn != null && processDefinitionIdIn.length > 0) {
      reportQuery.processDefinitionIdIn(processDefinitionIdIn);
    }
    if (processDefinitionKeyIn != null && processDefinitionKeyIn.length > 0) {
      reportQuery.processDefinitionKeyIn(processDefinitionKeyIn);
    }
    if (startedAfter != null) {
      reportQuery.startedAfter(startedAfter);
    }
    if (startedBefore != null) {
      reportQuery.startedBefore(startedBefore);
    }
    if (!REPORT_TYPE_DURATION.equals(reportType)) {
      throw new InvalidRequestException(Response.Status.BAD_REQUEST, "Unknown report type %s".formatted(reportType));
    }
  }

}
