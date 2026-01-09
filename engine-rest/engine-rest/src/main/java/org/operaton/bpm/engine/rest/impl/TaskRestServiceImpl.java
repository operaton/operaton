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
import java.util.Map;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Variant;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.rest.TaskRestService;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.VariableValueDto;
import org.operaton.bpm.engine.rest.dto.task.TaskDto;
import org.operaton.bpm.engine.rest.dto.task.TaskQueryDto;
import org.operaton.bpm.engine.rest.dto.task.TaskWithAttachmentAndCommentDto;
import org.operaton.bpm.engine.rest.dto.task.TaskWithVariablesDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.hal.Hal;
import org.operaton.bpm.engine.rest.hal.task.HalTaskList;
import org.operaton.bpm.engine.rest.sub.task.TaskReportResource;
import org.operaton.bpm.engine.rest.sub.task.TaskResource;
import org.operaton.bpm.engine.rest.sub.task.impl.TaskReportResourceImpl;
import org.operaton.bpm.engine.rest.sub.task.impl.TaskResourceImpl;
import org.operaton.bpm.engine.rest.util.QueryUtil;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.variable.VariableMap;

public class TaskRestServiceImpl extends AbstractRestProcessEngineAware implements TaskRestService {

  private static final List<Variant> VARIANTS = Variant.mediaTypes(MediaType.APPLICATION_JSON_TYPE, Hal.APPLICATION_HAL_JSON_TYPE).add().build();

  public TaskRestServiceImpl(String engineName, final ObjectMapper objectMapper) {
    super(engineName, objectMapper);
  }

  @Override
  public Object getTasks(Request request, UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    Variant variant = request.selectVariant(VARIANTS);
    if (variant != null) {
      if (MediaType.APPLICATION_JSON_TYPE.equals(variant.getMediaType())) {
        return getJsonTasks(uriInfo, firstResult, maxResults);
      }
      else if (Hal.APPLICATION_HAL_JSON_TYPE.equals(variant.getMediaType())) {
        return getHalTasks(uriInfo, firstResult, maxResults);
      }
    }
    throw new InvalidRequestException(Response.Status.NOT_ACCEPTABLE, "No acceptable content-type found");
  }

  public List<TaskDto> getJsonTasks(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    // get list of tasks
    TaskQueryDto queryDto = new TaskQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    return queryTasks(queryDto, firstResult, maxResults);
  }

  public HalTaskList getHalTasks(UriInfo uriInfo, Integer firstResult, Integer maxResults) {
    TaskQueryDto queryDto = new TaskQueryDto(getObjectMapper(), uriInfo.getQueryParameters());

    ProcessEngine engine = getProcessEngine();
    TaskQuery query = queryDto.toQuery(engine);

    // get list of tasks
    List<Task> matchingTasks = executeTaskQuery(firstResult, maxResults, query);

    // get total count
    long count = query.count();

    return HalTaskList.generate(matchingTasks, count, engine);
  }

  @Override
  public List<TaskDto> queryTasks(TaskQueryDto queryDto, Integer firstResult,
      Integer maxResults) {
    ProcessEngine engine = getProcessEngine();
    queryDto.setObjectMapper(getObjectMapper());
    TaskQuery query = queryDto.toQuery(engine);

    List<Task> matchingTasks = executeTaskQuery(firstResult, maxResults, query);


    boolean withTaskVariables = Boolean.TRUE.equals(queryDto.getWithTaskVariablesInReturn());
    boolean withTaskLocalVariables = Boolean.TRUE.equals(queryDto.getWithTaskLocalVariablesInReturn());
    boolean withCommentInfo = Boolean.TRUE.equals(queryDto.getWithCommentAttachmentInfo());

    if (withTaskVariables || withTaskLocalVariables) {
      return getVariablesForTasks(engine, matchingTasks, withTaskVariables, withCommentInfo);
    }
    if (withCommentInfo) {
      return matchingTasks.stream().map(TaskWithAttachmentAndCommentDto::fromEntity).toList();
    }
    return matchingTasks.stream().map(TaskDto::fromEntity).toList();
  }

  protected List<Task> executeTaskQuery(Integer firstResult, Integer maxResults, TaskQuery query) {

    // enable initialization of form key:
    query.initializeFormKeys();
    return QueryUtil.list(query, firstResult, maxResults);
  }

  @Override
  public CountResultDto getTasksCount(UriInfo uriInfo) {
    TaskQueryDto queryDto = new TaskQueryDto(getObjectMapper(), uriInfo.getQueryParameters());
    return queryTasksCount(queryDto);
  }

  @Override
  public CountResultDto queryTasksCount(TaskQueryDto queryDto) {
    ProcessEngine engine = getProcessEngine();
    queryDto.setObjectMapper(getObjectMapper());
    TaskQuery query = queryDto.toQuery(engine);

    long count = query.count();
    CountResultDto result = new CountResultDto();
    result.setCount(count);

    return result;
  }

  @Override
  public TaskResource getTask(String id,
                              boolean withCommentAttachmentInfo,
                              boolean withTaskVariablesInReturn,
                              boolean withTaskLocalVariablesInReturn) {
    return new TaskResourceImpl(getProcessEngine(), id, relativeRootResourcePath, getObjectMapper(),
        withCommentAttachmentInfo, withTaskVariablesInReturn, withTaskLocalVariablesInReturn);
  }

  @Override
  public void createTask(TaskDto taskDto) {
    ProcessEngine engine = getProcessEngine();
    TaskService taskService = engine.getTaskService();

    Task newTask = taskService.newTask(taskDto.getId());
    taskDto.updateTask(newTask);

    try {
      taskService.saveTask(newTask);

    } catch (NotValidException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Could not save task: " + e.getMessage());
    }

  }

  @Override
  public TaskReportResource getTaskReportResource() {
    return new TaskReportResourceImpl(getProcessEngine());
  }

  private List<TaskDto> getVariablesForTasks(ProcessEngine engine,
                                             List<Task> matchingTasks,
                                             boolean withTaskVariablesInReturn,
                                             boolean withCommentAndAttachments) {
    TaskService taskService = engine.getTaskService();
    List<TaskDto> tasks = new ArrayList<>();
    for (Task task : matchingTasks) {
      VariableMap taskVariables;
      if (withTaskVariablesInReturn) {
        taskVariables = taskService.getVariablesTyped(task.getId(), true);
      } else {
        taskVariables = taskService.getVariablesLocalTyped(task.getId(), true);
      }
      Map<String, VariableValueDto> taskVariablesDto = VariableValueDto.fromMap(taskVariables);
      if (withCommentAndAttachments) {
        tasks.add(TaskWithAttachmentAndCommentDto.fromEntity(task, taskVariablesDto));
      } else {
        tasks.add(TaskWithVariablesDto.fromEntity(task, taskVariablesDto));
      }
    }
    return tasks;
  }
}
