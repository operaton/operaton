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
package org.operaton.bpm.engine.rest.sub.externaltask;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.operaton.bpm.engine.rest.dto.externaltask.CompleteExternalTaskDto;
import org.operaton.bpm.engine.rest.dto.externaltask.ExtendLockOnExternalTaskDto;
import org.operaton.bpm.engine.rest.dto.externaltask.ExternalTaskBpmnError;
import org.operaton.bpm.engine.rest.dto.externaltask.ExternalTaskDto;
import org.operaton.bpm.engine.rest.dto.externaltask.ExternalTaskFailureDto;
import org.operaton.bpm.engine.rest.dto.externaltask.LockExternalTaskDto;
import org.operaton.bpm.engine.rest.dto.runtime.PriorityDto;
import org.operaton.bpm.engine.rest.dto.runtime.RetriesDto;

/**
 * @author Thorben Lindhauer
 * @author Askar Akhmerov
 *
 */
public interface ExternalTaskResource {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  ExternalTaskDto getExternalTask();

  @GET
  @Path("/errorDetails")
  @Produces(MediaType.TEXT_PLAIN)
  String getErrorDetails();

  @PUT
  @Path("/retries")
  @Consumes(MediaType.APPLICATION_JSON)
  void setRetries(RetriesDto dto);

  @PUT
  @Path("/priority")
  @Consumes(MediaType.APPLICATION_JSON)
  void setPriority(PriorityDto dto);

  @POST
  @Path("/complete")
  @Consumes(MediaType.APPLICATION_JSON)
  void complete(CompleteExternalTaskDto dto);

  @POST
  @Path("/failure")
  @Consumes(MediaType.APPLICATION_JSON)
  void handleFailure(ExternalTaskFailureDto dto);

  @POST
  @Path("/bpmnError")
  @Consumes(MediaType.APPLICATION_JSON)
  void handleBpmnError(ExternalTaskBpmnError dto);

  @POST
  @Path("/lock")
  @Consumes(MediaType.APPLICATION_JSON)
  void lock(LockExternalTaskDto lockExternalTaskDto);

  @POST
  @Path("/extendLock")
  @Consumes(MediaType.APPLICATION_JSON)
  void extendLock(ExtendLockOnExternalTaskDto extendLockDto);

  @POST
  @Path("/unlock")
  void unlock();
}
