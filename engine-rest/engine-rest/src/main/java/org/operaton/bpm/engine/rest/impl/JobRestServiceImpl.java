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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.util.EnsureUtil;
import org.operaton.bpm.engine.management.SetJobRetriesByJobsAsyncBuilder;
import org.operaton.bpm.engine.rest.JobRestService;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.JobDeletionResponse;
import org.operaton.bpm.engine.rest.dto.JobSuspensionResponse;
import org.operaton.bpm.engine.rest.dto.ResponseStatus;
import org.operaton.bpm.engine.rest.dto.batch.BatchDto;
import org.operaton.bpm.engine.rest.dto.runtime.JobDto;
import org.operaton.bpm.engine.rest.dto.runtime.JobQueryDto;
import org.operaton.bpm.engine.rest.dto.runtime.JobSuspensionStateDto;
import org.operaton.bpm.engine.rest.dto.runtime.SetJobRetriesDto;
import org.operaton.bpm.engine.rest.dto.runtime.modification.JobActivateSuspendDto;
import org.operaton.bpm.engine.rest.dto.runtime.modification.JobDeletionDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.runtime.JobResource;
import org.operaton.bpm.engine.rest.sub.runtime.impl.JobResourceImpl;
import org.operaton.bpm.engine.rest.util.QueryUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.operaton.bpm.engine.rest.dto.MultiStatusResponseCode.MULTI_STATUS_CODE;

public class JobRestServiceImpl extends AbstractRestProcessEngineAware
  implements JobRestService {

  private static final Logger LOG = LoggerFactory.getLogger(JobRestServiceImpl.class);
  private static final int MAX_JOBS_ALLOWED_FOR_BULK_DELETE = 200;
  private static final int MAX_JOBS_ALLOWED_FOR_BULK_SUSPEND_RESUME = 200;

  public JobRestServiceImpl(String engineName, ObjectMapper objectMapper) {
    super(engineName, objectMapper);
  }

  @Override
  public JobResource getJob(String jobId) {
    return new JobResourceImpl(getProcessEngine(), jobId);
  }

  @Override
  public List<JobDto> getJobs(UriInfo uriInfo, Integer firstResult,
                              Integer maxResults) {
    JobQueryDto queryDto = new JobQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    return queryJobs(queryDto, firstResult, maxResults);
  }

  @Override
  public List<JobDto> queryJobs(JobQueryDto queryDto, Integer firstResult,
                                Integer maxResults) {
    ProcessEngine engine = getProcessEngine();
    queryDto.setObjectMapper(getObjectMapper());
    JobQuery query = queryDto.toQuery(engine);

    List<Job> matchingJobs = QueryUtil.list(query, firstResult, maxResults);

    List<JobDto> jobResults = new ArrayList<>();
    for (Job job : matchingJobs) {
      JobDto resultJob = JobDto.fromJob(job);
      jobResults.add(resultJob);
    }
    return jobResults;
  }

  @Override
  public CountResultDto getJobsCount(UriInfo uriInfo) {
    JobQueryDto queryDto = new JobQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    return queryJobsCount(queryDto);
  }

  @Override
  public CountResultDto queryJobsCount(JobQueryDto queryDto) {
    ProcessEngine engine = getProcessEngine();
    queryDto.setObjectMapper(getObjectMapper());
    JobQuery query = queryDto.toQuery(engine);

    long count = query.count();
    CountResultDto result = new CountResultDto();
    result.setCount(count);

    return result;
  }

  @Override
  public BatchDto setRetries(SetJobRetriesDto setJobRetriesDto) {
    try {
      EnsureUtil.ensureNotNull("setJobRetriesDto", setJobRetriesDto);
      EnsureUtil.ensureNotNull("retries", setJobRetriesDto.getRetries());
    } catch (NullValueException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
    JobQuery jobQuery = null;
    if (setJobRetriesDto.getJobQuery() != null) {
      JobQueryDto jobQueryDto = setJobRetriesDto.getJobQuery();
      jobQueryDto.setObjectMapper(getObjectMapper());
      jobQuery = jobQueryDto.toQuery(getProcessEngine());
    }

    try {
      SetJobRetriesByJobsAsyncBuilder builder = getProcessEngine().getManagementService()
          .setJobRetriesByJobsAsync(setJobRetriesDto.getRetries().intValue())
          .jobIds(setJobRetriesDto.getJobIds())
          .jobQuery(jobQuery);
      if(setJobRetriesDto.isDueDateSet()) {
        builder.dueDate(setJobRetriesDto.getDueDate());
      }
      Batch batch = builder.executeAsync();
      return BatchDto.fromBatch(batch);
    } catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
  }

  @Override
  public void updateSuspensionState(JobSuspensionStateDto dto) {
    if (dto.getJobId() != null) {
      String message = "Either jobDefinitionId, processInstanceId, processDefinitionId or processDefinitionKey can be set to update the suspension state.";
      throw new InvalidRequestException(Status.BAD_REQUEST, message);
    }

    dto.updateSuspensionState(getProcessEngine());
  }

  @Override
  public Response updateSuspensionStateForJobs(JobActivateSuspendDto dto) {
    Set<String> jobIds = validateAndDeduplicateJobIds(dto == null ? null : dto.getJobIds(), MAX_JOBS_ALLOWED_FOR_BULK_SUSPEND_RESUME);
    boolean suspended = dto.isSuspended();

    List<JobSuspensionResponse> suspensionResponses = jobIds.stream()
        .map(jobId -> updateJobSuspensionState(jobId, suspended))
        .collect(Collectors.toList());

    return createBulkResponse(suspensionResponses.stream().anyMatch(this::hasFailure), suspensionResponses);
  }

  @Override
  public Response deleteJobs(JobDeletionDto dto) {
    Set<String> jobIds = validateAndDeduplicateJobIds(dto == null ? null : dto.getJobIds(), MAX_JOBS_ALLOWED_FOR_BULK_DELETE);

    List<JobDeletionResponse> deleteResponses = jobIds.stream()
        .map(this::deleteJob)
        .collect(Collectors.toList());

    return createBulkResponse(deleteResponses.stream().anyMatch(this::hasFailure), deleteResponses);
  }

  private JobSuspensionResponse updateJobSuspensionState(String jobId, boolean suspended) {
    try {
      JobResource currentJob = getJob(jobId);
      JobSuspensionStateDto dto = new JobSuspensionStateDto();
      dto.setJobId(currentJob.getJob().getId());
      dto.setSuspended(suspended);
      dto.updateSuspensionState(getProcessEngine());
      return new JobSuspensionResponse(jobId, ResponseStatus.SUCCESS, null);
    } catch (Exception e) {
      LOG.error("Unable to update suspension state for job id: {}", jobId, e);
      return new JobSuspensionResponse(jobId, ResponseStatus.FAILURE, e.getMessage());
    }
  }

  private JobDeletionResponse deleteJob(String jobId) {
    try {
      getJob(jobId).deleteJob();
      return new JobDeletionResponse(jobId, ResponseStatus.SUCCESS, null);
    } catch (Exception e) {
      LOG.error("Unable to delete job id: {}", jobId, e);
      return new JobDeletionResponse(jobId, ResponseStatus.FAILURE, e.getMessage());
    }
  }

  private Set<String> validateAndDeduplicateJobIds(List<String> jobIds, int limit) {
    boolean invalidInput = jobIds == null || jobIds.isEmpty()
        || jobIds.stream().anyMatch(jobId -> jobId == null || jobId.isBlank());
    if (invalidInput) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Please supply valid job ids as input.");
    }
    if (jobIds.size() > limit) {
      throw new InvalidRequestException(Status.BAD_REQUEST, "Input request exceeds the limit of %s.".formatted(limit));
    }
    return new HashSet<>(jobIds);
  }

  private <T> Response createBulkResponse(boolean hasFailures, List<T> responseEntity) {
    if (hasFailures) {
      return Response.status(MULTI_STATUS_CODE).entity(responseEntity).build();
    }
    return Response.status(Status.OK).entity(responseEntity).build();
  }

  private boolean hasFailure(JobSuspensionResponse response) {
    return ResponseStatus.FAILURE.equals(response.getStatus());
  }

  private boolean hasFailure(JobDeletionResponse response) {
    return ResponseStatus.FAILURE.equals(response.getStatus());
  }

}
