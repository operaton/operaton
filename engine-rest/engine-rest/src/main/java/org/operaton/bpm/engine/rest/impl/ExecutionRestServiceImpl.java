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
import org.operaton.bpm.engine.rest.ExecutionRestService;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.runtime.ExecutionDto;
import org.operaton.bpm.engine.rest.dto.runtime.ExecutionQueryDto;
import org.operaton.bpm.engine.rest.sub.runtime.ExecutionResource;
import org.operaton.bpm.engine.rest.sub.runtime.impl.ExecutionResourceImpl;
import org.operaton.bpm.engine.rest.util.QueryUtil;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ExecutionQuery;

public class ExecutionRestServiceImpl extends AbstractRestProcessEngineAware implements ExecutionRestService {

  public ExecutionRestServiceImpl(String engineName, ObjectMapper objectMapper) {
    super(engineName, objectMapper);
  }

  @Override
  public ExecutionResource getExecution(String executionId) {
    return new ExecutionResourceImpl(getProcessEngine(), executionId, getObjectMapper());
  }

  @Override
  public List<ExecutionDto> getExecutions(UriInfo uriInfo, Integer firstResult,
      Integer maxResults) {
    ExecutionQueryDto queryDto = new ExecutionQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    return queryExecutions(queryDto, firstResult, maxResults);
  }

  @Override
  public List<ExecutionDto> queryExecutions(
      ExecutionQueryDto queryDto, Integer firstResult, Integer maxResults) {
    ProcessEngine engine = getProcessEngine();
    queryDto.setObjectMapper(getObjectMapper());
    ExecutionQuery query = queryDto.toQuery(engine);

    List<Execution> matchingExecutions = QueryUtil.list(query, firstResult, maxResults);

    List<ExecutionDto> executionResults = new ArrayList<>();
    for (Execution execution : matchingExecutions) {
      ExecutionDto resultExecution = ExecutionDto.fromExecution(execution);
      executionResults.add(resultExecution);
    }
    return executionResults;
  }

  @Override
  public CountResultDto getExecutionsCount(UriInfo uriInfo) {
    ExecutionQueryDto queryDto = new ExecutionQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    return queryExecutionsCount(queryDto);
  }

  @Override
  public CountResultDto queryExecutionsCount(ExecutionQueryDto queryDto) {
    ProcessEngine engine = getProcessEngine();
    queryDto.setObjectMapper(getObjectMapper());
    ExecutionQuery query = queryDto.toQuery(engine);

    long count = query.count();
    CountResultDto result = new CountResultDto();
    result.setCount(count);

    return result;
  }
}
