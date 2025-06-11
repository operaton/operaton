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
package org.operaton.bpm.engine.rest.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.core.UriInfo;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.BatchQuery;
import org.operaton.bpm.engine.batch.BatchStatistics;
import org.operaton.bpm.engine.batch.BatchStatisticsQuery;
import org.operaton.bpm.engine.rest.BatchRestService;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchQueryDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchStatisticsDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchStatisticsQueryDto;
import org.operaton.bpm.engine.rest.sub.batch.BatchResource;
import org.operaton.bpm.engine.rest.sub.batch.impl.BatchResourceImpl;
import org.operaton.bpm.engine.rest.util.QueryUtil;

public class BatchRestServiceImpl extends AbstractRestProcessEngineAware implements BatchRestService {

  public BatchRestServiceImpl(String engineName, ObjectMapper objectMapper) {
    super(engineName, objectMapper);
  }

  @Override
  public BatchResource getBatch(String batchId) {
    return new BatchResourceImpl(getProcessEngine(), batchId);
  }

  @Override
  public List<BatchDto> getBatches(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    BatchQueryDto queryDto = new BatchQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    BatchQuery query = queryDto.toQuery(getProcessEngine());

    List<Batch> matchingBatches = QueryUtil.list(query, firstResult, maxResults);

    List<BatchDto> batchResults = new ArrayList<>();
    for (Batch matchingBatch : matchingBatches) {
      batchResults.add(BatchDto.fromBatch(matchingBatch));
    }
    return batchResults;
  }

  @Override
  public CountResultDto getBatchesCount(UriInfo uriInfo) {
    ProcessEngine processEngine = getProcessEngine();
    BatchQueryDto queryDto = new BatchQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    BatchQuery query = queryDto.toQuery(processEngine);

    long count = query.count();
    return new CountResultDto(count);
  }

  @Override
  public List<BatchStatisticsDto> getStatistics(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    BatchStatisticsQueryDto queryDto = new BatchStatisticsQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    BatchStatisticsQuery query = queryDto.toQuery(getProcessEngine());

    List<BatchStatistics> batchStatisticsList = QueryUtil.list(query, firstResult, maxResults);

    List<BatchStatisticsDto> statisticsResults = new ArrayList<>();
    for (BatchStatistics batchStatistics : batchStatisticsList) {
      statisticsResults.add(BatchStatisticsDto.fromBatchStatistics(batchStatistics));
    }

    return statisticsResults;
  }

  @Override
  public CountResultDto getStatisticsCount(UriInfo uriInfo) {
    BatchStatisticsQueryDto queryDto = new BatchStatisticsQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    BatchStatisticsQuery query = queryDto.toQuery(getProcessEngine());

    long count = query.count();
    return new CountResultDto(count);
  }

}
