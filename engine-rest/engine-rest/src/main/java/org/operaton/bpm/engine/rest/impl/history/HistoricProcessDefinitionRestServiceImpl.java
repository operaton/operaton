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
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.CleanableHistoricProcessInstanceReport;
import org.operaton.bpm.engine.history.CleanableHistoricProcessInstanceReportResult;
import org.operaton.bpm.engine.history.HistoricActivityStatistics;
import org.operaton.bpm.engine.history.HistoricActivityStatisticsQuery;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.history.CleanableHistoricProcessInstanceReportDto;
import org.operaton.bpm.engine.rest.dto.history.CleanableHistoricProcessInstanceReportResultDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricActivityStatisticsDto;
import org.operaton.bpm.engine.rest.history.HistoricProcessDefinitionRestService;
import org.operaton.bpm.engine.rest.impl.AbstractRestProcessEngineAware;
import org.operaton.bpm.engine.rest.util.QueryUtil;

public class HistoricProcessDefinitionRestServiceImpl extends AbstractRestProcessEngineAware implements HistoricProcessDefinitionRestService {

  public HistoricProcessDefinitionRestServiceImpl(ObjectMapper objectMapper, ProcessEngine processEngine) {
    super(processEngine.getName(), objectMapper);
  }

  @Override
  public List<HistoricActivityStatisticsDto> getHistoricActivityStatistics(UriInfo uriInfo, String processDefinitionId) {
    HistoricActivityStatisticsQueryDto queryDto = new HistoricActivityStatisticsQueryDto(getObjectMapper(), processDefinitionId, uriInfo.getQueryParameters());

    HistoricActivityStatisticsQuery query = queryDto.toQuery(getProcessEngine());

    List<HistoricActivityStatisticsDto> result = new ArrayList<>();

    List<HistoricActivityStatistics> statistics = query.unlimitedList();

    for (HistoricActivityStatistics currentStatistics : statistics) {
      result.add(HistoricActivityStatisticsDto.fromHistoricActivityStatistics(currentStatistics));
    }

    return result;
  }

  @Override
  public List<CleanableHistoricProcessInstanceReportResultDto> getCleanableHistoricProcessInstanceReport(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    CleanableHistoricProcessInstanceReportDto queryDto = new CleanableHistoricProcessInstanceReportDto(objectMapper, uriInfo.getQueryParameters());
    CleanableHistoricProcessInstanceReport query = queryDto.toQuery(getProcessEngine());

    List<CleanableHistoricProcessInstanceReportResult> reportResult = QueryUtil.list(query, firstResult, maxResults);

    return CleanableHistoricProcessInstanceReportResultDto.convert(reportResult);
  }

  @Override
  public CountResultDto getCleanableHistoricProcessInstanceReportCount(UriInfo uriInfo) {
    CleanableHistoricProcessInstanceReportDto queryDto = new CleanableHistoricProcessInstanceReportDto(objectMapper, uriInfo.getQueryParameters());
    queryDto.setObjectMapper(objectMapper);
    CleanableHistoricProcessInstanceReport query = queryDto.toQuery(getProcessEngine());

    long count = query.count();
    CountResultDto result = new CountResultDto();
    result.setCount(count);

    return result;
  }

}
