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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Variant;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.ReportResult;
import org.operaton.bpm.engine.history.SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.converter.ReportResultToCsvConverter;
import org.operaton.bpm.engine.rest.dto.history.DeleteHistoricProcessInstancesDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricProcessInstanceDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricProcessInstanceQueryDto;
import org.operaton.bpm.engine.rest.dto.history.HistoricProcessInstanceReportDto;
import org.operaton.bpm.engine.rest.dto.history.ReportResultDto;
import org.operaton.bpm.engine.rest.dto.history.batch.removaltime.SetRemovalTimeToHistoricProcessInstancesDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.history.HistoricProcessInstanceRestService;
import org.operaton.bpm.engine.rest.sub.history.HistoricProcessInstanceResource;
import org.operaton.bpm.engine.rest.sub.history.impl.HistoricProcessInstanceResourceImpl;
import org.operaton.bpm.engine.rest.util.QueryUtil;
import org.operaton.bpm.engine.rest.util.URLEncodingUtil;

public class HistoricProcessInstanceRestServiceImpl implements HistoricProcessInstanceRestService {

  public static final MediaType APPLICATION_CSV_TYPE = new MediaType("application", "csv");
  public static final MediaType TEXT_CSV_TYPE = new MediaType("text", "csv");
  public static final List<Variant> VARIANTS = Variant.mediaTypes(MediaType.APPLICATION_JSON_TYPE, APPLICATION_CSV_TYPE, TEXT_CSV_TYPE).add().build();

  protected ObjectMapper objectMapper;
  protected ProcessEngine processEngine;

  public HistoricProcessInstanceRestServiceImpl(ObjectMapper objectMapper, ProcessEngine processEngine) {
    this.objectMapper = objectMapper;
    this.processEngine = processEngine;
  }

  @Override
  public HistoricProcessInstanceResource getHistoricProcessInstance(String processInstanceId) {
    return new HistoricProcessInstanceResourceImpl(processEngine, processInstanceId);
  }

  @Override
  public List<HistoricProcessInstanceDto> getHistoricProcessInstances(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    HistoricProcessInstanceQueryDto queryHistoriProcessInstanceDto = new HistoricProcessInstanceQueryDto(objectMapper, uriInfo.getQueryParameters());
    return queryHistoricProcessInstances(queryHistoriProcessInstanceDto, firstResult, maxResults);
  }

  @Override
  public List<HistoricProcessInstanceDto> queryHistoricProcessInstances(HistoricProcessInstanceQueryDto queryDto, Integer firstResult, Integer maxResults) {
    queryDto.setObjectMapper(objectMapper);
    HistoricProcessInstanceQuery query = queryDto.toQuery(processEngine);

    List<HistoricProcessInstance> matchingHistoricProcessInstances = QueryUtil.list(query, firstResult, maxResults);

    List<HistoricProcessInstanceDto> historicProcessInstanceDtoResults = new ArrayList<>();
    for (HistoricProcessInstance historicProcessInstance : matchingHistoricProcessInstances) {
      HistoricProcessInstanceDto resultHistoricProcessInstanceDto = HistoricProcessInstanceDto.fromHistoricProcessInstance(historicProcessInstance);
      historicProcessInstanceDtoResults.add(resultHistoricProcessInstanceDto);
    }
    return historicProcessInstanceDtoResults;
  }

  @Override
  public CountResultDto getHistoricProcessInstancesCount(UriInfo uriInfo) {
    HistoricProcessInstanceQueryDto queryDto = new HistoricProcessInstanceQueryDto(objectMapper, uriInfo.getQueryParameters());
    return queryHistoricProcessInstancesCount(queryDto);
  }

  @Override
  public CountResultDto queryHistoricProcessInstancesCount(HistoricProcessInstanceQueryDto queryDto) {
    queryDto.setObjectMapper(objectMapper);
    HistoricProcessInstanceQuery query = queryDto.toQuery(processEngine);

    long count = query.count();
    CountResultDto result = new CountResultDto();
    result.setCount(count);

    return result;
  }

  @SuppressWarnings("unchecked")
  protected List<ReportResult> queryHistoricProcessInstanceReport(UriInfo uriInfo) {
    HistoricProcessInstanceReportDto reportDto = new HistoricProcessInstanceReportDto(objectMapper, uriInfo.getQueryParameters());
    return reportDto.executeReport(processEngine);
  }

  @Override
  public Response getHistoricProcessInstancesReport(UriInfo uriInfo, Request request) {
    Variant variant = request.selectVariant(VARIANTS);
    if (variant != null) {
      MediaType mediaType = variant.getMediaType();

      if (MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
        List<ReportResultDto> result = getReportResultAsJson(uriInfo);
        return Response.ok(result, mediaType).build();
      }
      else if (APPLICATION_CSV_TYPE.equals(mediaType) || TEXT_CSV_TYPE.equals(mediaType)) {
        String csv = getReportResultAsCsv(uriInfo);
        return Response
            .ok(csv, mediaType)
            .header("Content-Disposition", URLEncodingUtil.buildAttachmentValue("process-instance-report.csv"))
            .build();
      }
    }
    throw new InvalidRequestException(Status.NOT_ACCEPTABLE, "No acceptable content-type found");
  }

  @Override
  public BatchDto deleteAsync(DeleteHistoricProcessInstancesDto dto) {
    HistoryService historyService = processEngine.getHistoryService();

    HistoricProcessInstanceQuery historicProcessInstanceQuery = null;
    if (dto.getHistoricProcessInstanceQuery() != null) {
      historicProcessInstanceQuery = dto.getHistoricProcessInstanceQuery().toQuery(processEngine);
    }

    try {
      Batch batch;
      batch = historyService.deleteHistoricProcessInstancesAsync(dto.getHistoricProcessInstanceIds(), historicProcessInstanceQuery, dto.getDeleteReason());
      return BatchDto.fromBatch(batch);

    } catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
  }

  @Override
  public BatchDto setRemovalTimeAsync(SetRemovalTimeToHistoricProcessInstancesDto dto) {
    HistoryService historyService = processEngine.getHistoryService();

    HistoricProcessInstanceQuery historicProcessInstanceQuery = null;

    if (dto.getHistoricProcessInstanceQuery() != null) {
      historicProcessInstanceQuery = dto.getHistoricProcessInstanceQuery().toQuery(processEngine);

    }

    SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder builder =
      historyService.setRemovalTimeToHistoricProcessInstances();

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

    builder.byIds(dto.getHistoricProcessInstanceIds());
    builder.byQuery(historicProcessInstanceQuery);

    if (dto.isHierarchical()) {
      builder.hierarchical();

    }

    if (dto.isUpdateInChunks()) {
      builder.updateInChunks();
    }

    Integer chunkSize = dto.getUpdateChunkSize();
    if (chunkSize != null) {
      builder.chunkSize(chunkSize);
    }

    Batch batch = builder.executeAsync();
    return BatchDto.fromBatch(batch);
  }

  protected List<ReportResultDto> getReportResultAsJson(UriInfo uriInfo) {
    List<ReportResult> reports = queryHistoricProcessInstanceReport(uriInfo);
    List<ReportResultDto> result = new ArrayList<>();
    for (ReportResult report : reports) {
      result.add(ReportResultDto.fromReportResult(report));
    }
    return result;
  }

  protected String getReportResultAsCsv(UriInfo uriInfo) {
    List<ReportResult> reports = queryHistoricProcessInstanceReport(uriInfo);
    MultivaluedMap<String,String> queryParameters = uriInfo.getQueryParameters();
    String reportType = queryParameters.getFirst("reportType");
    return ReportResultToCsvConverter.convertReportResult(reports, reportType);
  }

  @Override
  public Response deleteHistoricVariableInstancesByProcessInstanceId(String processInstanceId) {
    try {
      processEngine.getHistoryService().deleteHistoricVariableInstancesByProcessInstanceId(processInstanceId);
    } catch (NotFoundException nfe) { // rewrite status code from bad request (400) to not found (404)
      throw new InvalidRequestException(Status.NOT_FOUND, nfe.getMessage());
    }
    // return no content (204) since resource is deleted
    return Response.noContent().build();
  }
}
