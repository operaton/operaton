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
package org.operaton.bpm.engine.rest.sub.repository.impl;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.rest.dto.AnnotationDto;
import org.operaton.bpm.engine.rest.dto.runtime.IncidentDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.sub.runtime.IncidentResource;
import org.operaton.bpm.engine.runtime.Incident;

public class IncidentResourceImpl implements IncidentResource {

  protected ProcessEngine engine;
  protected String incidentId;
  protected ObjectMapper objectMapper;

  public IncidentResourceImpl(ProcessEngine engine, String incidentId, ObjectMapper objectMapper) {
    this.engine = engine;
    this.incidentId = incidentId;
    this.objectMapper = objectMapper;
  }

  @Override
  public IncidentDto getIncident() {
    Incident incident = engine.getRuntimeService().createIncidentQuery().incidentId(incidentId).singleResult();
    if (incident == null) {
      throw new InvalidRequestException(Status.NOT_FOUND, "No matching incident with id %s".formatted(incidentId));
    }
    return IncidentDto.fromIncident(incident);
  }

  @Override
  public void resolveIncident() {
    try {
      engine.getRuntimeService().resolveIncident(incidentId);
    } catch (NotFoundException e) {
      throw new InvalidRequestException(Status.NOT_FOUND, e.getMessage());
    } catch (BadUserRequestException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e.getMessage());
    }
  }

  @Override
  public Response setAnnotation(AnnotationDto annotationDto) {
    engine.getRuntimeService().setAnnotationForIncidentById(incidentId, annotationDto.getAnnotation());
    return Response.noContent().build();
  }

  @Override
  public Response clearAnnotation() {
    engine.getRuntimeService().clearAnnotationForIncidentById(incidentId);
    return Response.noContent().build();
  }
}
