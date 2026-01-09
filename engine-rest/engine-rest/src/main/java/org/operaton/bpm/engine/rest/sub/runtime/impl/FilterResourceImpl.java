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
package org.operaton.bpm.engine.rest.sub.runtime.impl;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Variant;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.impl.VariableInstanceQueryImpl;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.operaton.bpm.engine.query.Query;
import org.operaton.bpm.engine.rest.FilterRestService;
import org.operaton.bpm.engine.rest.dto.AbstractQueryDto;
import org.operaton.bpm.engine.rest.dto.CountResultDto;
import org.operaton.bpm.engine.rest.dto.ResourceOptionsDto;
import org.operaton.bpm.engine.rest.dto.runtime.FilterDto;
import org.operaton.bpm.engine.rest.dto.task.TaskDto;
import org.operaton.bpm.engine.rest.dto.task.TaskQueryDto;
import org.operaton.bpm.engine.rest.exception.InvalidRequestException;
import org.operaton.bpm.engine.rest.hal.EmptyHalCollection;
import org.operaton.bpm.engine.rest.hal.EmptyHalResource;
import org.operaton.bpm.engine.rest.hal.Hal;
import org.operaton.bpm.engine.rest.hal.HalCollectionResource;
import org.operaton.bpm.engine.rest.hal.HalResource;
import org.operaton.bpm.engine.rest.hal.HalVariableValue;
import org.operaton.bpm.engine.rest.hal.task.HalTask;
import org.operaton.bpm.engine.rest.hal.task.HalTaskList;
import org.operaton.bpm.engine.rest.impl.AbstractAuthorizedRestResource;
import org.operaton.bpm.engine.rest.sub.runtime.FilterResource;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;

import static org.operaton.bpm.engine.authorization.Permissions.DELETE;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.FILTER;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * @author Sebastian Menski
 */
@SuppressWarnings("java:S1452")
public class FilterResourceImpl extends AbstractAuthorizedRestResource implements FilterResource {

  private static final Pattern EMPTY_JSON_BODY = Pattern.compile("\\s*\\{\\s*\\}\\s*");
  private static final String PROPERTIES_VARIABLES_KEY = "variables";
  private static final String PROPERTIES_VARIABLES_NAME_KEY = "name";
  private static final List<Variant> VARIANTS = Variant.mediaTypes(MediaType.APPLICATION_JSON_TYPE, Hal.APPLICATION_HAL_JSON_TYPE).add().build();

  protected FilterService filterService;
  protected Filter dbFilter;

  public FilterResourceImpl(String processEngineName, ObjectMapper objectMapper, String filterId, String relativeRootResourcePath) {
    super(processEngineName, FILTER, filterId, objectMapper);
    setRelativeRootResourceUri(relativeRootResourcePath);
    filterService = getProcessEngine().getFilterService();
  }

  @Override
  public FilterDto getFilter(Boolean itemCount) {
    Filter filter = getDbFilter();
    FilterDto dto = FilterDto.fromFilter(filter);
    if (itemCount != null && itemCount) {
      dto.setItemCount(filterService.count(filter.getId()));
    }
    return dto;
  }

  protected Filter getDbFilter() {
    if (dbFilter == null) {
      dbFilter = filterService.getFilter(resourceId);

      if (dbFilter == null) {
        throw filterNotFound(null);
      }
    }
    return dbFilter;
  }

  @Override
  public void deleteFilter() {
    try {
      filterService.deleteFilter(resourceId);
    }
    catch (NullValueException e) {
      throw filterNotFound(e);
    }
  }

  @Override
  public void updateFilter(FilterDto filterDto) {
    Filter filter = getDbFilter();

    try {
      filterDto.updateFilter(filter, getProcessEngine());
    }
    catch (NotValidException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Unable to update filter with invalid content");
    }

    filterService.saveFilter(filter);
  }

  @Override
  public Object executeSingleResult(Request request) {
    Variant variant = request.selectVariant(VARIANTS);
    if (variant != null) {
      if (MediaType.APPLICATION_JSON_TYPE.equals(variant.getMediaType())) {
        return executeJsonSingleResult();
      }
      else if (Hal.APPLICATION_HAL_JSON_TYPE.equals(variant.getMediaType())) {
        return executeHalSingleResult();
      }
    }
    throw new InvalidRequestException(Status.NOT_ACCEPTABLE, "No acceptable content-type found");
  }

  public Object executeJsonSingleResult() {
    return queryJsonSingleResult(null);
  }

  @Override
  public Object querySingleResult(Request request, String extendingQuery) {
    Variant variant = request.selectVariant(VARIANTS);
    if (variant != null) {
      if (MediaType.APPLICATION_JSON_TYPE.equals(variant.getMediaType())) {
        return queryJsonSingleResult(extendingQuery);
      }
      else if (Hal.APPLICATION_HAL_JSON_TYPE.equals(variant.getMediaType())) {
        return queryHalSingleResult(extendingQuery);
      }
    }
    throw new InvalidRequestException(Status.NOT_ACCEPTABLE, "No acceptable content-type found");
  }

  public Object queryJsonSingleResult(String extendingQuery) {
    Object entity = executeFilterSingleResult(extendingQuery);

    if (entity != null) {
      return convertToDto(entity);
    }
    else {
      return null;
    }
  }

  public HalResource<?> executeHalSingleResult() {
    return queryHalSingleResult(null);
  }

  public HalResource<?> queryHalSingleResult(String extendingQuery) {
    Object entity = executeFilterSingleResult(extendingQuery);

    if (entity != null) {
      return convertToHalResource(entity);
    }
    else {
      return EmptyHalResource.INSTANCE;
    }
  }

  protected Object executeFilterSingleResult(String extendingQuery) {
    try {
      return  filterService.singleResult(resourceId, convertQuery(extendingQuery));
    }
    catch (NullValueException e) {
      throw filterNotFound(e);
    }
    catch (NotValidException e) {
      throw invalidQuery(e);
    }
    catch (ProcessEngineException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Filter does not returns a valid single result");
    }
  }

  @Override
  public Object executeList(Request request, Integer firstResult, Integer maxResults) {
    Variant variant = request.selectVariant(VARIANTS);
    if (variant != null) {
      if (MediaType.APPLICATION_JSON_TYPE.equals(variant.getMediaType())) {
        return executeJsonList(firstResult, maxResults);
      }
      else if (Hal.APPLICATION_HAL_JSON_TYPE.equals(variant.getMediaType())) {
        return executeHalList(firstResult, maxResults);
      }
    }
    throw new InvalidRequestException(Status.NOT_ACCEPTABLE, "No acceptable content-type found");
  }

  public List<Object> executeJsonList(Integer firstResult, Integer maxResults) {
    return queryJsonList(null, firstResult, maxResults);
  }

  @Override
  public Object queryList(Request request, String extendingQuery, Integer firstResult, Integer maxResults) {
    Variant variant = request.selectVariant(VARIANTS);
    if (variant != null) {
      if (MediaType.APPLICATION_JSON_TYPE.equals(variant.getMediaType())) {
        return queryJsonList(extendingQuery, firstResult ,maxResults);
      }
      else if (Hal.APPLICATION_HAL_JSON_TYPE.equals(variant.getMediaType())) {
        return queryHalList(extendingQuery, firstResult, maxResults);
      }
    }
    throw new InvalidRequestException(Status.NOT_ACCEPTABLE, "No acceptable content-type found");
  }

  public List<Object> queryJsonList(String extendingQuery, Integer firstResult, Integer maxResults) {
    List<?> entities = executeFilterList(extendingQuery, firstResult, maxResults);

    if (entities != null && !entities.isEmpty()) {
      return convertToDtoList(entities);
    }
    else {
      return emptyList();
    }
  }

  public HalResource<?> executeHalList(Integer firstResult, Integer maxResults) {
    return queryHalList(null, firstResult, maxResults);
  }

  public HalResource<?> queryHalList(String extendingQuery, Integer firstResult, Integer maxResults) {
    List<?> entities = executeFilterList(extendingQuery, firstResult, maxResults);
    long count = executeFilterCount(extendingQuery);

    if (entities != null && !entities.isEmpty()) {
      return convertToHalCollection(entities, count);
    }
    else {
      return new EmptyHalCollection(count);
    }
  }

  protected List<?> executeFilterList(String extendingQueryString, Integer firstResult, Integer maxResults) {
    Query<?, ?> extendingQuery = convertQuery(extendingQueryString);
    try {
      if (firstResult != null || maxResults != null) {
        if (firstResult == null) {
          firstResult = 0;
        }
        if (maxResults == null) {
          maxResults = Integer.MAX_VALUE;
        }
        return filterService.listPage(resourceId, extendingQuery, firstResult, maxResults);
      } else {
        return filterService.list(resourceId, extendingQuery);
      }
    }
    catch (NullValueException e) {
      throw filterNotFound(e);
    }
    catch (NotValidException e) {
      throw invalidQuery(e);
    }
  }

  @Override
  public CountResultDto executeCount() {
    return queryCount(null);
  }

  @Override
  public CountResultDto queryCount(String extendingQuery) {
    return new CountResultDto(executeFilterCount(extendingQuery));
  }

  protected long executeFilterCount(String extendingQuery) {
    try {
      return filterService.count(resourceId, convertQuery(extendingQuery));
    }
    catch (NullValueException e) {
      throw filterNotFound(e);
    }
    catch (NotValidException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Filter cannot be extended by an invalid query");
    }
  }

  @Override
  public ResourceOptionsDto availableOperations(UriInfo context) {

    ResourceOptionsDto dto = new ResourceOptionsDto();

    UriBuilder baseUriBuilder = context.getBaseUriBuilder()
      .path(relativeRootResourcePath)
      .path(FilterRestService.PATH)
      .path(resourceId);

    URI baseUri = baseUriBuilder.build();

    if (isAuthorized(READ)) {
      dto.addReflexiveLink(baseUri, HttpMethod.GET, "self");

      URI singleResultUri = baseUriBuilder.clone().path("/singleResult").build();
      dto.addReflexiveLink(singleResultUri, HttpMethod.GET, "singleResult");
      dto.addReflexiveLink(singleResultUri, HttpMethod.POST, "singleResult");

      URI listUri = baseUriBuilder.clone().path("/list").build();
      dto.addReflexiveLink(listUri, HttpMethod.GET, "list");
      dto.addReflexiveLink(listUri, HttpMethod.POST, "list");

      URI countUri = baseUriBuilder.clone().path("/count").build();
      dto.addReflexiveLink(countUri, HttpMethod.GET, "count");
      dto.addReflexiveLink(countUri, HttpMethod.POST, "count");
    }

    if (isAuthorized(DELETE)) {
      dto.addReflexiveLink(baseUri, HttpMethod.DELETE, "delete");
    }

    if (isAuthorized(UPDATE)) {
      dto.addReflexiveLink(baseUri, HttpMethod.PUT, "update");
    }

    return dto;
  }

  protected Query<?,?> convertQuery(String queryString) {
    if (isEmptyJson(queryString)) {
      return null;
    }
    else {
      String resourceType = getDbFilter().getResourceType();
      AbstractQueryDto<?> queryDto = getQueryDtoForQuery(queryString, resourceType);
      queryDto.setObjectMapper(getObjectMapper());
      return queryDto.toQuery(getProcessEngine());
    }
  }

  protected Object convertToDto(Object entity) {
    if (isEntityOfClass(entity, Task.class)) {
      return TaskDto.fromEntity((Task) entity);
    }
    else {
      throw unsupportedEntityClass(entity);
    }
  }

  protected List<Object> convertToDtoList(List<?> entities) {
    List<Object> dtoList = new ArrayList<>();
    for (Object entity : entities) {
      dtoList.add(convertToDto(entity));
    }
    return dtoList;
  }

  protected HalResource<?> convertToHalResource(Object entity) {
    if (isEntityOfClass(entity, Task.class)) {
      return convertToHalTask((Task) entity);
    }
    else {
      throw unsupportedEntityClass(entity);
    }
  }

  protected HalTask convertToHalTask(Task task) {
    HalTask halTask = HalTask.generate(task, getProcessEngine());
    Map<String, List<VariableInstance>> variableInstances = getVariableInstancesForTasks(halTask);
    if (!variableInstances.isEmpty()) {
      embedVariableValuesInHalTask(halTask, variableInstances);
    }
    return halTask;
  }

  @SuppressWarnings("unchecked")
  protected HalCollectionResource<?> convertToHalCollection(List<?> entities, long count) {
    if (isEntityOfClass(entities.get(0), Task.class)) {
      return convertToHalTaskList((List<Task>) entities, count);
    } else {
      throw unsupportedEntityClass(entities.get(0));
    }
  }

  @SuppressWarnings("unchecked")
  protected HalTaskList convertToHalTaskList(List<Task> tasks, long count) {
    HalTaskList halTasks = HalTaskList.generate(tasks, count, getProcessEngine());
    Map<String, List<VariableInstance>> variableInstances = getVariableInstancesForTasks(halTasks);
    if (!variableInstances.isEmpty()) {
      for (HalTask halTask : (List<HalTask>) halTasks.getEmbedded("task")) {
        embedVariableValuesInHalTask(halTask, variableInstances);
      }
    }
    return halTasks;
  }

  protected void embedVariableValuesInHalTask( HalTask halTask, Map<String, List<VariableInstance>> variableInstances) {
    List<HalResource<?>> variableValues = getVariableValuesForTask(halTask, variableInstances);
    halTask.addEmbedded("variable", variableValues);
  }

  protected AbstractQueryDto<?> getQueryDtoForQuery(String queryString, String resourceType) {
    try {
      if (EntityTypes.TASK.equals(resourceType)) {
        return getObjectMapper().readValue(queryString, TaskQueryDto.class);
      } else {
        throw new InvalidRequestException(Status.BAD_REQUEST, "Queries for resource type '%s' are currently not supported by filters.".formatted(resourceType));
      }
    } catch (IOException e) {
      throw new InvalidRequestException(Status.BAD_REQUEST, e, "Invalid query for resource type '%s'".formatted(resourceType));
    }
  }

  protected List<HalResource<?>> getVariableValuesForTask(HalTask halTask, Map<String, List<VariableInstance>> variableInstances) {
    // converted variables values
    List<HalResource<?>> variableValues = new ArrayList<>();

    // variable scope ids to check, ordered by visibility
    LinkedHashSet<String> variableScopeIds = getVariableScopeIds(halTask);

    // names of already converted variables
    Set<String> knownVariableNames = new HashSet<>();

    for (String variableScopeId : variableScopeIds) {
      if (variableInstances.containsKey(variableScopeId)) {
        for (VariableInstance variableInstance : variableInstances.get(variableScopeId)) {
          if (!knownVariableNames.contains(variableInstance.getName())) {
            variableValues.add(HalVariableValue.generateVariableValue(variableInstance, variableScopeId));
            knownVariableNames.add(variableInstance.getName());
          }
        }
      }
    }

    return variableValues;
  }

  @SuppressWarnings("unchecked")
  protected Map<String, List<VariableInstance>> getVariableInstancesForTasks(HalTaskList halTaskList) {
    List<HalTask> halTasks = (List<HalTask>) halTaskList.getEmbedded("task");
    return getVariableInstancesForTasks(halTasks.toArray(new HalTask[halTasks.size()]));
  }

  protected Map<String, List<VariableInstance>> getVariableInstancesForTasks(HalTask... halTasks) {
    if (halTasks != null && halTasks.length > 0) {
      List<String> variableNames = getFilterVariableNames();
      if (variableNames != null && !variableNames.isEmpty()) {
        LinkedHashSet<String> variableScopeIds = getVariableScopeIds(halTasks);
        return getSortedVariableInstances(variableNames, variableScopeIds);
      }
    }
    return emptyMap();
  }

  @SuppressWarnings("unchecked")
  protected List<String> getFilterVariableNames() {
    Map<String, Object> properties = getDbFilter().getProperties();
    if (properties != null) {
      try {
        List<Map<String, Object>> variables = (List<Map<String, Object>>) properties.get(PROPERTIES_VARIABLES_KEY);
        return collectVariableNames(variables);
      }
      catch (Exception e) {
        throw new InvalidRequestException(Status.INTERNAL_SERVER_ERROR, e, "Filter property '%s' has to be a list of variable definitions with a '%s' property".formatted(PROPERTIES_VARIABLES_KEY, PROPERTIES_VARIABLES_NAME_KEY));
      }
    }
    else {
      return emptyList();
    }
  }

  private List<String> collectVariableNames(List<Map<String, Object>> variables) {
    if (variables != null && !variables.isEmpty()) {
      List<String> variableNames = new ArrayList<>();
      for (Map<String, Object> variable : variables) {
        variableNames.add((String) variable.get(PROPERTIES_VARIABLES_NAME_KEY));
      }
      return variableNames;
    }
    else {
      return emptyList();
    }
  }

  protected LinkedHashSet<String> getVariableScopeIds(HalTask... halTasks) {
    // collect scope ids
    // the ordering is important because it specifies which variables are visible from a single task
    LinkedHashSet<String> variableScopeIds = new LinkedHashSet<>();
    if (halTasks != null && halTasks.length > 0) {
      for (HalTask halTask : halTasks) {
        variableScopeIds.add(halTask.getId());
        variableScopeIds.add(halTask.getExecutionId());
        variableScopeIds.add(halTask.getProcessInstanceId());
        variableScopeIds.add(halTask.getCaseExecutionId());
        variableScopeIds.add(halTask.getCaseInstanceId());
      }
    }

    // remove null from set which was probably added due an unset id
    variableScopeIds.remove(null);

    return variableScopeIds;
  }

  protected Map<String, List<VariableInstance>> getSortedVariableInstances(Collection<String> variableNames, Collection<String> variableScopeIds) {
    List<VariableInstance> variableInstances = queryVariablesInstancesByVariableScopeIds(variableNames, variableScopeIds);
    Map<String, List<VariableInstance>> sortedVariableInstances = new HashMap<>();
    for (VariableInstance variableInstance : variableInstances) {
      String variableScopeId = ((VariableInstanceEntity) variableInstance).getVariableScopeId();
      sortedVariableInstances
        .computeIfAbsent(variableScopeId, key -> new ArrayList<>())
        .add(variableInstance);
    }
    return sortedVariableInstances;
  }

  protected List<VariableInstance> queryVariablesInstancesByVariableScopeIds(Collection<String> variableNames, Collection<String> variableScopeIds) {

    VariableInstanceQueryImpl query = (VariableInstanceQueryImpl) getProcessEngine().getRuntimeService()
        .createVariableInstanceQuery()
        .disableBinaryFetching()
        .disableCustomObjectDeserialization()
        .variableNameIn(variableNames.toArray(new String[0]))
        .variableScopeIdIn(variableScopeIds.toArray(new String[0]));

    // the number of results is capped at:
    // #tasks * #variableNames * 5 (we have five variable scopes per task)
    // this value may exceed the configured query pagination limit, so we make an unbounded query.
    // As #tasks is bounded by the pagination limit, it will never load an unbounded number of variables.
    return query.unlimitedList();

  }

  protected boolean isEntityOfClass(Object entity, Class<?> entityClass) {
    return entityClass.isAssignableFrom(entity.getClass());
  }

  protected boolean isEmptyJson(String jsonString) {
    return jsonString == null || jsonString.trim().isEmpty() || EMPTY_JSON_BODY.matcher(jsonString).matches();
  }

  protected InvalidRequestException filterNotFound(Exception cause) {
    return new InvalidRequestException(Status.NOT_FOUND, cause, "Filter with id '%s' does not exist.".formatted(resourceId));
  }

  protected InvalidRequestException invalidQuery(Exception cause) {
    return new InvalidRequestException(Status.BAD_REQUEST, cause, "Filter cannot be extended by an invalid query");
  }

  protected InvalidRequestException unsupportedEntityClass(Object entity) {
    return new InvalidRequestException(Status.BAD_REQUEST, "Entities of class '%s' are currently not supported by filters.".formatted(entity.getClass().getCanonicalName()));
  }

}
