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
package org.operaton.bpm.engine.rest.sub.task.impl;

import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Variant;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.rest.dto.converter.TaskReportResultToCsvConverter;
import org.operaton.bpm.engine.rest.dto.task.TaskCountByCandidateGroupResultDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.task.TaskReportResource;
import org.operaton.bpm.engine.rest.util.URLEncodingUtil;
import org.operaton.bpm.engine.task.TaskCountByCandidateGroupResult;

public class TaskReportResourceImpl implements TaskReportResource {
  private static final MediaType APPLICATION_CSV_TYPE = new MediaType("application", "csv");
  private static final MediaType TEXT_CSV_TYPE = new MediaType("text", "csv");
  private static final List<Variant> VARIANTS = Variant.mediaTypes(MediaType.APPLICATION_JSON_TYPE, APPLICATION_CSV_TYPE, TEXT_CSV_TYPE).add().build();

  protected ProcessEngine engine;

  public TaskReportResourceImpl(ProcessEngine engine) {
    this.engine = engine;
  }

  @Override
  public Response getTaskCountByCandidateGroupReport(Request request) {
    Variant variant = request.selectVariant(VARIANTS);
    if (variant != null) {
      MediaType mediaType = variant.getMediaType();

      if (MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
        List<TaskCountByCandidateGroupResultDto> result = getTaskCountByCandidateGroupResultAsJson();
        return Response.ok(result, mediaType).build();
      }
      else if (APPLICATION_CSV_TYPE.equals(mediaType) || TEXT_CSV_TYPE.equals(mediaType)) {
        String csv = getReportResultAsCsv();
        return Response
          .ok(csv, mediaType)
          .header("Content-Disposition", URLEncodingUtil.buildAttachmentValue("task-count-by-candidate-group.csv"))
          .build();
      }
    }
    throw new InvalidRequestException(Status.NOT_ACCEPTABLE, "No acceptable content-type found");
  }

  @SuppressWarnings("unchecked")
  protected List<TaskCountByCandidateGroupResult> queryTaskCountByCandidateGroupReport() {
    TaskCountByCandidateGroupResultDto reportDto = new TaskCountByCandidateGroupResultDto();
    return reportDto.executeTaskCountByCandidateGroupReport(engine);
  }

  protected List<TaskCountByCandidateGroupResultDto> getTaskCountByCandidateGroupResultAsJson() {
    List<TaskCountByCandidateGroupResult> reports = queryTaskCountByCandidateGroupReport();
    List<TaskCountByCandidateGroupResultDto> result = new ArrayList<>();
    for (TaskCountByCandidateGroupResult report : reports) {
      result.add(TaskCountByCandidateGroupResultDto.fromTaskCountByCandidateGroupResultDto(report));
    }
    return result;
  }

  protected String getReportResultAsCsv() {
    List<TaskCountByCandidateGroupResult> reports = queryTaskCountByCandidateGroupReport();
    return TaskReportResultToCsvConverter.convertCandidateGroupReportResult(reports);
  }
}
