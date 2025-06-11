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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.core.UriInfo;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.history.HistoricExternalTaskLog;
import org.operaton.bpm.engine.history.HistoricExternalTaskLogQuery;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricExternalTaskLogDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricExternalTaskLogQueryDto;
import org.operaton.bpm.engine.rest.history.HistoricExternalTaskLogRestService;
import org.operaton.bpm.engine.rest.sub.history.HistoricExternalTaskLogResource;
import org.operaton.bpm.engine.rest.sub.history.impl.HistoricExternalTaskLogResourceImpl;
import org.operaton.bpm.engine.rest.util.QueryUtil;

public class HistoricExternalTaskLogRestServiceImpl implements HistoricExternalTaskLogRestService {

  protected ObjectMapper objectMapper;
  protected ProcessEngine processEngine;

  public HistoricExternalTaskLogRestServiceImpl(ObjectMapper objectMapper, ProcessEngine processEngine) {
    this.objectMapper = objectMapper;
    this.processEngine = processEngine;
  }

  @Override
  public HistoricExternalTaskLogResource getHistoricExternalTaskLog(String historicExternalTaskLogId) {
    return new HistoricExternalTaskLogResourceImpl(historicExternalTaskLogId, processEngine);
  }

  @Override
  public List<HistoricExternalTaskLogDto> getHistoricExternalTaskLogs(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    HistoricExternalTaskLogQueryDto queryDto = new HistoricExternalTaskLogQueryDto(objectMapper, uriInfo.getQueryParameters());
    return queryHistoricExternalTaskLogs(queryDto, firstResult, maxResults);
  }

  @Override
  public List<HistoricExternalTaskLogDto> queryHistoricExternalTaskLogs(HistoricExternalTaskLogQueryDto queryDto, Integer firstResult, Integer maxResults) {
    queryDto.setObjectMapper(objectMapper);
    HistoricExternalTaskLogQuery query = queryDto.toQuery(processEngine);

    List<HistoricExternalTaskLog> matchingHistoricExternalTaskLogs = QueryUtil.list(query, firstResult, maxResults);

    List<HistoricExternalTaskLogDto> results = new ArrayList<>();
    for (HistoricExternalTaskLog historicExternalTaskLog : matchingHistoricExternalTaskLogs) {
      HistoricExternalTaskLogDto result = HistoricExternalTaskLogDto.fromHistoricExternalTaskLog(historicExternalTaskLog);
      results.add(result);
    }

    return results;
  }

  @Override
  public CountResultDto getHistoricExternalTaskLogsCount(UriInfo uriInfo) {
    HistoricExternalTaskLogQueryDto queryDto = new HistoricExternalTaskLogQueryDto(objectMapper, uriInfo.getQueryParameters());
    return queryHistoricExternalTaskLogsCount(queryDto);
  }

  @Override
  public CountResultDto queryHistoricExternalTaskLogsCount(HistoricExternalTaskLogQueryDto queryDto) {
    queryDto.setObjectMapper(objectMapper);
    HistoricExternalTaskLogQuery query = queryDto.toQuery(processEngine);

    long count = query.count();
    CountResultDto result = new CountResultDto();
    result.setCount(count);

    return result;
  }

}
