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

import java.io.InputStream;
import java.net.URI;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.rest.CaseInstanceRestService;
import org.operaton.bpm.engine.rest.dto.HistoryTimeToLiveDto;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.dto.repository.CaseDefinitionDiagramDto;
import org.operaton.bpm.engine.rest.dto.repository.CaseDefinitionDto;
import org.operaton.bpm.engine.rest.dto.runtime.CaseInstanceDto;
import org.operaton.bpm.engine.rest.dto.runtime.CreateCaseInstanceDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.exception.RestException;
import org.operaton.bpm.engine.rest.sub.repository.CaseDefinitionResource;
import org.operaton.bpm.engine.rest.util.URLEncodingUtil;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.variable.VariableMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 * @author Roman Smirnov
 *
 */
public class CaseDefinitionResourceImpl implements CaseDefinitionResource {

  protected ProcessEngine engine;
  protected String caseDefinitionId;
  protected String rootResourcePath;
  protected ObjectMapper objectMapper;

  public CaseDefinitionResourceImpl(ProcessEngine engine, String caseDefinitionId, String rootResourcePath, ObjectMapper objectMapper) {
    this.engine = engine;
    this.caseDefinitionId = caseDefinitionId;
    this.rootResourcePath = rootResourcePath;
    this.objectMapper = objectMapper;
  }

  @Override
  public CaseDefinitionDto getCaseDefinition() {
    RepositoryService repositoryService = engine.getRepositoryService();

    CaseDefinition definition = null;

    try {
      definition = repositoryService.getCaseDefinition(caseDefinitionId);
    } catch (NotFoundException e) {
      throw new InvalidRequestException(Status.NOT_FOUND, e, e.getMessage());
    } catch (NotValidException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, e.getMessage());
    } catch (ProcessEngineException e) {
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e);
    }

    return CaseDefinitionDto.fromCaseDefinition(definition);
  }

  @Override
  public CaseDefinitionDiagramDto getCaseDefinitionCmmnXml() {
    InputStream caseModelInputStream = null;
    try {
      caseModelInputStream = engine.getRepositoryService().getCaseModel(caseDefinitionId);

      byte[] caseModel = IoUtil.readInputStream(caseModelInputStream, "caseModelCmmnXml");
      return CaseDefinitionDiagramDto.create(caseDefinitionId, new String(caseModel, UTF_8));
    } catch (NotFoundException e) {
      throw new InvalidRequestException(Status.NOT_FOUND, e, e.getMessage());
    } catch (NotValidException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, e.getMessage());
    } catch (ProcessEngineException e) {
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e);
    } finally {
      IoUtil.closeSilently(caseModelInputStream);
    }
  }

  @Override
  public CaseInstanceDto createCaseInstance(UriInfo context, CreateCaseInstanceDto parameters) {
    CaseService caseService = engine.getCaseService();

    CaseInstance instance = null;
    try {

      String businessKey = parameters.getBusinessKey();
      VariableMap variables = VariableValueDto.toMap(parameters.getVariables(), engine, objectMapper);

      instance = caseService
          .withCaseDefinition(caseDefinitionId)
          .businessKey(businessKey)
          .setVariables(variables)
          .create();

    } catch (RestException e) {
      String errorMessage = "Cannot instantiate case definition %s: %s".formatted(caseDefinitionId, e.getMessage());
      throw new InvalidRequestException(e.getStatus(), e, errorMessage);

    } catch (NotFoundException e) {
      String errorMessage = "Cannot instantiate case definition %s: %s".formatted(caseDefinitionId, e.getMessage());
      throw new InvalidRequestException(Status.NOT_FOUND, e, errorMessage);

    } catch (NotValidException e) {
      String errorMessage = "Cannot instantiate case definition %s: %s".formatted(caseDefinitionId, e.getMessage());
      throw new InvalidRequestException(Status.BAD_REQUEST, e, errorMessage);

    } catch (NotAllowedException e) {
      String errorMessage = "Cannot instantiate case definition %s: %s".formatted(caseDefinitionId, e.getMessage());
      throw new InvalidRequestException(Status.FORBIDDEN, e, errorMessage);

    } catch (ProcessEngineException e) {
      String errorMessage = "Cannot instantiate case definition %s: %s".formatted(caseDefinitionId, e.getMessage());
      throw new RestException(Status.INTERNAL_SERVER_ERROR, e, errorMessage);

    }

    CaseInstanceDto result = CaseInstanceDto.fromCaseInstance(instance);

    URI uri = context.getBaseUriBuilder()
      .path(rootResourcePath)
      .path(CaseInstanceRestService.PATH)
      .path(instance.getId())
      .build();

    result.addReflexiveLink(uri, HttpMethod.GET, "self");

    return result;
  }

  @Override
  public Response getCaseDefinitionDiagram() {
    CaseDefinition definition = engine.getRepositoryService().getCaseDefinition(caseDefinitionId);
    InputStream caseDiagram = engine.getRepositoryService().getCaseDiagram(caseDefinitionId);
    if (caseDiagram == null) {
      return Response.noContent().build();
    } else {
      String fileName = definition.getDiagramResourceName();
      return Response.ok(caseDiagram).header("Content-Disposition", URLEncodingUtil.buildAttachmentValue(fileName))
          .type(ProcessDefinitionResourceImpl.getMediaTypeForFileSuffix(fileName)).build();
    }
  }

  @Override
  public void updateHistoryTimeToLive(HistoryTimeToLiveDto historyTimeToLiveDto) {
    engine.getRepositoryService().updateCaseDefinitionHistoryTimeToLive(caseDefinitionId, historyTimeToLiveDto.getHistoryTimeToLive());
  }

}
