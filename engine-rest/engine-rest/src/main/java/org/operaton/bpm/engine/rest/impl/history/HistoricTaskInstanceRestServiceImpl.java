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
package org.operaton.bpm.engine.rest.impl.history;

import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricTaskInstanceQuery;
import org.operaton.bpm.engine.history.HistoricTaskInstanceReportResult;
import org.operaton.bpm.engine.history.ReportResult;
import org.operaton.bpm.engine.rest.dto.AbstractReportDto;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricTaskInstanceDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricTaskInstanceQueryDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricTaskInstanceReportQueryDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricTaskInstanceReportResultDto;
import org.operaton.bpm.engine.rest.dto.history.ReportResultDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.history.HistoricTaskInstanceRestService;
import org.operaton.bpm.engine.rest.util.QueryUtil;

/**
 * @author Roman Smirnov
 *
 */
public class HistoricTaskInstanceRestServiceImpl implements HistoricTaskInstanceRestService {

  protected ObjectMapper objectMapper;
  protected ProcessEngine processEngine;

  public HistoricTaskInstanceRestServiceImpl(ObjectMapper objectMapper, ProcessEngine processEngine) {
    this.objectMapper = objectMapper;
    this.processEngine = processEngine;
  }

  @Override
  public List<HistoricTaskInstanceDto> getHistoricTaskInstances(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    HistoricTaskInstanceQueryDto queryDto = new HistoricTaskInstanceQueryDto(objectMapper, uriInfo.getQueryParameters());
    return queryHistoricTaskInstances(queryDto, firstResult, maxResults);
  }

  @Override
  public List<HistoricTaskInstanceDto> queryHistoricTaskInstances(HistoricTaskInstanceQueryDto queryDto, Integer firstResult, Integer maxResults) {
    queryDto.setObjectMapper(objectMapper);
    HistoricTaskInstanceQuery query = queryDto.toQuery(processEngine);

    List<HistoricTaskInstance> match = QueryUtil.list(query, firstResult, maxResults);

    List<HistoricTaskInstanceDto> result = new ArrayList<>();
    for (HistoricTaskInstance taskInstance : match) {
      HistoricTaskInstanceDto taskInstanceDto = HistoricTaskInstanceDto.fromHistoricTaskInstance(taskInstance);
      result.add(taskInstanceDto);
    }
    return result;
  }

  @Override
  public CountResultDto getHistoricTaskInstancesCount(UriInfo uriInfo) {
    HistoricTaskInstanceQueryDto queryDto = new HistoricTaskInstanceQueryDto(objectMapper, uriInfo.getQueryParameters());
    return queryHistoricTaskInstancesCount(queryDto);
  }

  @Override
  public CountResultDto queryHistoricTaskInstancesCount(HistoricTaskInstanceQueryDto queryDto) {
    queryDto.setObjectMapper(objectMapper);
    HistoricTaskInstanceQuery query = queryDto.toQuery(processEngine);

    long count = query.count();
    CountResultDto result = new CountResultDto();
    result.setCount(count);

    return result;
  }

  @Override
  public Response getHistoricTaskInstanceReport(UriInfo uriInfo) {
    HistoricTaskInstanceReportQueryDto queryDto = new HistoricTaskInstanceReportQueryDto(objectMapper, uriInfo.getQueryParameters());
    Response response;

    if (AbstractReportDto.REPORT_TYPE_DURATION.equals(queryDto.getReportType())) {
      List<ReportResult> reportResults = queryDto.executeReport(processEngine);
      response = Response.ok(generateDurationDto(reportResults)).build();
    } else if (AbstractReportDto.REPORT_TYPE_COUNT.equals(queryDto.getReportType())) {
      List<HistoricTaskInstanceReportResult> reportResults = queryDto.executeCompletedReport(processEngine);
      response = Response.ok(generateCountDto(reportResults)).build();
    } else {
      throw new InvalidRequestException(Response.Status.BAD_REQUEST, "Parameter reportType is not set.");
    }

    return response;
  }

  protected List<HistoricTaskInstanceReportResultDto> generateCountDto(List<HistoricTaskInstanceReportResult> results) {
    List<HistoricTaskInstanceReportResultDto> dtoList = new ArrayList<>();

    for( HistoricTaskInstanceReportResult result : results ) {
      dtoList.add(HistoricTaskInstanceReportResultDto.fromHistoricTaskInstanceReportResult(result));
    }

    return dtoList;
  }

  protected List<ReportResultDto> generateDurationDto(List<? extends ReportResult> results) {
    List<ReportResultDto> dtoList = new ArrayList<>();

    for( ReportResult result : results ) {
      dtoList.add(ReportResultDto.fromReportResult(result));
    }

    return dtoList;
  }

}
