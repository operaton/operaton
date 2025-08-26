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
import org.operaton.bpm.engine.history.HistoricJobLog;
import org.operaton.bpm.engine.history.HistoricJobLogQuery;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricJobLogDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricJobLogQueryDto;
import org.operaton.bpm.engine.rest.history.HistoricJobLogRestService;
import org.operaton.bpm.engine.rest.sub.history.HistoricJobLogResource;
import org.operaton.bpm.engine.rest.sub.history.impl.HistoricJobLogResourceImpl;
import org.operaton.bpm.engine.rest.util.QueryUtil;

/**
 * @author Roman Smirnov
 *
 */
public class HistoricJobLogRestServiceImpl implements HistoricJobLogRestService {

  protected ObjectMapper objectMapper;
  protected ProcessEngine processEngine;

  public HistoricJobLogRestServiceImpl(ObjectMapper objectMapper, ProcessEngine processEngine) {
    this.objectMapper = objectMapper;
    this.processEngine = processEngine;
  }

  @Override
  public HistoricJobLogResource getHistoricJobLog(String historicJobLogId) {
    return new HistoricJobLogResourceImpl(historicJobLogId, processEngine);
  }

  @Override
  public List<HistoricJobLogDto> getHistoricJobLogs(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    HistoricJobLogQueryDto queryDto = new HistoricJobLogQueryDto(objectMapper, uriInfo.getQueryParameters());
    return queryHistoricJobLogs(queryDto, firstResult, maxResults);
  }

  @Override
  public List<HistoricJobLogDto> queryHistoricJobLogs(HistoricJobLogQueryDto queryDto, Integer firstResult, Integer maxResults) {
    queryDto.setObjectMapper(objectMapper);
    HistoricJobLogQuery query = queryDto.toQuery(processEngine);

    List<HistoricJobLog> matchingHistoricJobLogs = QueryUtil.list(query, firstResult, maxResults);

    List<HistoricJobLogDto> results = new ArrayList<>();
    for (HistoricJobLog historicJobLog : matchingHistoricJobLogs) {
      HistoricJobLogDto result = HistoricJobLogDto.fromHistoricJobLog(historicJobLog);
      results.add(result);
    }

    return results;
  }

  @Override
  public CountResultDto getHistoricJobLogsCount(UriInfo uriInfo) {
    HistoricJobLogQueryDto queryDto = new HistoricJobLogQueryDto(objectMapper, uriInfo.getQueryParameters());
    return queryHistoricJobLogsCount(queryDto);
  }

  @Override
  public CountResultDto queryHistoricJobLogsCount(HistoricJobLogQueryDto queryDto) {
    queryDto.setObjectMapper(objectMapper);
    HistoricJobLogQuery query = queryDto.toQuery(processEngine);

    long count = query.count();
    CountResultDto result = new CountResultDto();
    result.setCount(count);

    return result;
  }

}
