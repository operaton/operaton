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
import java.util.List;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.UriInfo;
import org.operaton.bpm.engine.management.SchemaLogEntry;
import org.operaton.bpm.engine.management.SchemaLogQuery;
import org.operaton.bpm.engine.rest.SchemaLogRestService;
import org.operaton.bpm.engine.rest.dto.SchemaLogEntryDto;
import org.operaton.bpm.engine.rest.dto.SchemaLogQueryDto;
import org.operaton.bpm.engine.rest.util.QueryUtil;

/**
 * @author Miklas Boskamp
 *
 */
public class SchemaLogRestServiceImpl extends AbstractRestProcessEngineAware implements SchemaLogRestService {

  public SchemaLogRestServiceImpl(String engineName, ObjectMapper objectMapper) {
    super(engineName, objectMapper);
  }

  @Override
  public List<SchemaLogEntryDto> getSchemaLog(Request request, UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    return querySchemaLog(new SchemaLogQueryDto(getObjectMapper(), uriInfo.getQueryParameters()), firstResult, maxResults);
  }

  @Override
  public List<SchemaLogEntryDto> querySchemaLog(SchemaLogQueryDto dto, Integer firstResult, Integer maxResults) {
    SchemaLogQuery query = dto.toQuery(getProcessEngine());
    List<SchemaLogEntry> schemaLogEntries = QueryUtil.list(query, firstResult, maxResults);
    return SchemaLogEntryDto.fromSchemaLogEntries(schemaLogEntries);
  }
}
