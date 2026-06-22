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
package org.operaton.bpm.engine.rest.sub.management;

import jakarta.ws.rs.core.Response.Status;

import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.SetJobRetriesBuilder;
import org.operaton.bpm.engine.rest.dto.management.JobDefinitionDto;
import org.operaton.bpm.engine.rest.dto.management.JobDefinitionSuspensionStateDto;
import org.operaton.bpm.engine.rest.dto.runtime.JobDefinitionPriorityDto;
import org.operaton.bpm.engine.rest.dto.runtime.RetriesDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;

/**
 * @author roman.smirnov
 */
public class JobDefinitionResourceImpl implements JobDefinitionResource {

  protected ProcessEngine engine;
  protected String jobDefinitionId;

  public JobDefinitionResourceImpl(ProcessEngine engine, String jobDefinitionId) {
    this.engine = engine;
    this.jobDefinitionId = jobDefinitionId;
  }

  @Override
  public JobDefinitionDto getJobDefinition() {
    ManagementService managementService = engine.getManagementService();
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().jobDefinitionId(jobDefinitionId).singleResult();

    if (jobDefinition == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "Job Definition with id %s does not exist".formatted(jobDefinitionId));
    }

    return JobDefinitionDto.fromJobDefinition(jobDefinition);
  }

  @Override
  public void updateSuspensionState(JobDefinitionSuspensionStateDto dto) {
    try {
      dto.setJobDefinitionId(jobDefinitionId);
      dto.updateSuspensionState(engine);

    } catch (IllegalArgumentException e) {
      String message = "The suspension state of Job Definition with id %s could not be updated due to: %s".formatted(jobDefinitionId, e.getMessage());
      throw new InvalidRequestException(Status.BAD_REQUEST, e, message);
    }

  }

  @Override
  public void setJobRetries(RetriesDto dto) {
    try {
      SetJobRetriesBuilder builder = engine.getManagementService()
          .setJobRetries(dto.getRetries())
          .jobDefinitionId(jobDefinitionId);
      if (dto.isDueDateSet()) {
        builder.dueDate(dto.getDueDate());
      }
      builder.execute();
    } catch (AuthorizationException e) {
      throw e;
    } catch (ProcessEngineException e) {
      throw new InvalidRequestException(Status.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  @Override
  public void setJobPriority(JobDefinitionPriorityDto dto) {
    try {
      ManagementService managementService = engine.getManagementService();

      if (dto.getPriority() != null) {
        managementService.setOverridingJobPriorityForJobDefinition(jobDefinitionId, dto.getPriority(), dto.isIncludeJobs());
      }
      else {
        if (dto.isIncludeJobs()) {
          throw new InvalidRequestException(Status.BAD_REQUEST,
              "Cannot reset priority for job definition %s with includeJobs=true".formatted(jobDefinitionId));
        }

        managementService.clearOverridingJobPriorityForJobDefinition(jobDefinitionId);
      }

    } catch (AuthorizationException e) {
      throw e;
    } catch (NotFoundException e) {
      throw new InvalidRequestException(Status.NOT_FOUND, e.getMessage());
    } catch (ProcessEngineException e) {
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e.getMessage());
    }

  }

}
