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

import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.rest.IncidentRestService;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.runtime.IncidentDto;
import org.operaton.bpm.engine.rest.dto.runtime.IncidentQueryDto;
import org.operaton.bpm.engine.rest.sub.repository.impl.IncidentResourceImpl;
import org.operaton.bpm.engine.rest.sub.runtime.IncidentResource;
import org.operaton.bpm.engine.rest.util.QueryUtil;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.runtime.IncidentQuery;

/**
 * @author Roman Smirnov
 *
 */
public class IncidentRestServiceImpl extends AbstractRestProcessEngineAware implements IncidentRestService {

  public IncidentRestServiceImpl(String engineName, ObjectMapper objectMapper) {
    super(engineName, objectMapper);
  }

  @Override
  public List<IncidentDto> getIncidents(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    IncidentQueryDto queryDto = new IncidentQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    IncidentQuery query = queryDto.toQuery(getProcessEngine());

    List<Incident> queryResult = QueryUtil.list(query, firstResult, maxResults);

    List<IncidentDto> result = new ArrayList<>();
    for (Incident incident : queryResult) {
      IncidentDto dto = IncidentDto.fromIncident(incident);
      result.add(dto);
    }

    return result;
  }

  @Override
  public CountResultDto getIncidentsCount(UriInfo uriInfo) {
    IncidentQueryDto queryDto = new IncidentQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    IncidentQuery query = queryDto.toQuery(getProcessEngine());

    long count = query.count();
    CountResultDto result = new CountResultDto();
    result.setCount(count);

    return result;
  }

  @Override
  public IncidentResource getIncident(String incidentId) {
    return new IncidentResourceImpl(getProcessEngine(), incidentId, getObjectMapper());
  }
}
