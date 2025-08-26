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
import java.util.Date;
import java.util.List;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.batch.history.HistoricBatchQuery;
import org.operaton.bpm.engine.history.CleanableHistoricBatchReport;
import org.operaton.bpm.engine.history.CleanableHistoricBatchReportResult;
import org.operaton.bpm.engine.history.SetRemovalTimeSelectModeForHistoricBatchesBuilder;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.history.batch.CleanableHistoricBatchReportDto;
import org.operaton.bpm.engine.rest.dto.history.batch.CleanableHistoricBatchReportResultDto;
import org.operaton.bpm.engine.rest.dto.history.batch.HistoricBatchDto;
import org.operaton.bpm.engine.rest.dto.history.batch.HistoricBatchQueryDto;
import org.operaton.bpm.engine.rest.dto.history.batch.removaltime.SetRemovalTimeToHistoricBatchesDto;
import org.operaton.bpm.engine.rest.history.HistoricBatchRestService;
import org.operaton.bpm.engine.rest.sub.history.HistoricBatchResource;
import org.operaton.bpm.engine.rest.sub.history.impl.HistoricBatchResourceImpl;
import org.operaton.bpm.engine.rest.util.QueryUtil;

public class HistoricBatchRestServiceImpl implements HistoricBatchRestService {

  protected ObjectMapper objectMapper;
  protected ProcessEngine processEngine;

  public HistoricBatchRestServiceImpl(ObjectMapper objectMapper, ProcessEngine processEngine) {
    this.objectMapper = objectMapper;
    this.processEngine = processEngine;
  }

  @Override
  public HistoricBatchResource getHistoricBatch(String batchId) {
    return new HistoricBatchResourceImpl(processEngine, batchId);
  }

  @Override
  public List<HistoricBatchDto> getHistoricBatches(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    HistoricBatchQueryDto queryDto = new HistoricBatchQueryDto(objectMapper, uriInfo.getQueryParameters());
    HistoricBatchQuery query = queryDto.toQuery(processEngine);

    List<HistoricBatch> matchingBatches = QueryUtil.list(query, firstResult, maxResults);

    List<HistoricBatchDto> batchResults = new ArrayList<>();
    for (HistoricBatch matchingBatch : matchingBatches) {
      batchResults.add(HistoricBatchDto.fromBatch(matchingBatch));
    }
    return batchResults;
  }

  @Override
  public CountResultDto getHistoricBatchesCount(UriInfo uriInfo) {
    HistoricBatchQueryDto queryDto = new HistoricBatchQueryDto(objectMapper, uriInfo.getQueryParameters());
    HistoricBatchQuery query = queryDto.toQuery(processEngine);

    long count = query.count();
    return new CountResultDto(count);
  }

  @Override
  public List<CleanableHistoricBatchReportResultDto> getCleanableHistoricBatchesReport(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    CleanableHistoricBatchReportDto queryDto = new CleanableHistoricBatchReportDto(objectMapper, uriInfo.getQueryParameters());
    CleanableHistoricBatchReport query = queryDto.toQuery(processEngine);

    List<CleanableHistoricBatchReportResult> reportResult = QueryUtil.list(query, firstResult, maxResults);

    return CleanableHistoricBatchReportResultDto.convert(reportResult);
  }

  @Override
  public CountResultDto getCleanableHistoricBatchesReportCount(UriInfo uriInfo) {
    CleanableHistoricBatchReportDto queryDto = new CleanableHistoricBatchReportDto(objectMapper, uriInfo.getQueryParameters());
    queryDto.setObjectMapper(objectMapper);
    CleanableHistoricBatchReport query = queryDto.toQuery(processEngine);

    long count = query.count();
    CountResultDto result = new CountResultDto();
    result.setCount(count);

    return result;
  }

  @Override
  public BatchDto setRemovalTimeAsync(SetRemovalTimeToHistoricBatchesDto dto) {
    HistoryService historyService = processEngine.getHistoryService();

    HistoricBatchQuery historicBatchQuery = null;

    if (dto.getHistoricBatchQuery() != null) {
      historicBatchQuery = dto.getHistoricBatchQuery().toQuery(processEngine);

    }

    SetRemovalTimeSelectModeForHistoricBatchesBuilder builder =
      historyService.setRemovalTimeToHistoricBatches();

    if (dto.isCalculatedRemovalTime()) {
      builder.calculatedRemovalTime();

    }

    Date removalTime = dto.getAbsoluteRemovalTime();
    if (dto.getAbsoluteRemovalTime() != null) {
      builder.absoluteRemovalTime(removalTime);

    }

    if (dto.isClearedRemovalTime()) {
      builder.clearedRemovalTime();

    }

    builder.byIds(dto.getHistoricBatchIds());
    builder.byQuery(historicBatchQuery);

    Batch batch = builder.executeAsync();
    return BatchDto.fromBatch(batch);
  }

}
