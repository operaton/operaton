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
package org.operaton.bpm.engine.impl.persistence.entity;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.impl.AbstractQuery;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.QueryValidators.StoredQueryValidator;
import org.operaton.bpm.engine.impl.db.DbEntity;
import org.operaton.bpm.engine.impl.db.DbEntityLifecycleAware;
import org.operaton.bpm.engine.impl.db.EnginePersistenceLogger;
import org.operaton.bpm.engine.impl.db.HasDbReferences;
import org.operaton.bpm.engine.impl.db.HasDbRevision;
import org.operaton.bpm.engine.impl.json.JsonObjectConverter;
import org.operaton.bpm.engine.impl.json.JsonTaskQueryConverter;
import org.operaton.bpm.engine.impl.util.JsonUtil;
import org.operaton.bpm.engine.query.Query;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotEmpty;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNull;

/**
 * @author Sebastian Menski
 */
public class FilterEntity implements Filter, DbEntity, HasDbRevision, HasDbReferences, DbEntityLifecycleAware {

  private static final String QUERY = "query";

  protected static final EnginePersistenceLogger LOG = ProcessEngineLogger.PERSISTENCE_LOGGER;

  public static final Map<String, JsonObjectConverter<?>> QUERY_CONVERTER = Map.of(EntityTypes.TASK, new JsonTaskQueryConverter());

  protected String id;
  protected String resourceType;
  protected String name;
  protected String owner;
  protected AbstractQuery query;
  protected Map<String, Object> properties;
  protected int revision;

  protected FilterEntity() {
  }

  public FilterEntity(String resourceType) {
    setResourceType(resourceType);
    setQueryInternal("{}");
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getId() {
    return id;
  }

  public Filter setResourceType(String resourceType) {
    ensureNotEmpty(NotValidException.class, "Filter resource type must not be null or empty", "resourceType", resourceType);
    ensureNull(NotValidException.class, "Cannot overwrite filter resource type", "resourceType", this.resourceType);

    this.resourceType = resourceType;
    return this;
  }

  @Override
  public String getResourceType() {
    return resourceType;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Filter setName(String name) {
    ensureNotEmpty(NotValidException.class, "Filter name must not be null or empty", "name", name);
    this.name = name;
    return this;
  }

  @Override
  public String getOwner() {
    return owner;
  }

  @Override
  public Filter setOwner(String owner) {
    this.owner = owner;
    return this;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Query<?, ?>> T getQuery() {
    return (T) query;
  }

  public String getQueryInternal() {
    JsonObjectConverter<Object> converter = getConverter();
    return converter.toJson(query);
  }

  @Override
  public <T extends Query<?, ?>> Filter setQuery(T query) {
    ensureNotNull(NotValidException.class, QUERY, query);
    this.query = (AbstractQuery<?, ?>) query;
    return this;
  }

  public void setQueryInternal(String query) {
    ensureNotNull(NotValidException.class, QUERY, query);
    JsonObjectConverter<Object> converter = getConverter();
    this.query = (AbstractQuery<?, ?>) converter.toObject(JsonUtil.asObject(query));
  }

  @Override
  public Map<String, Object> getProperties() {
    return properties;
  }

  public String getPropertiesInternal() {
    return JsonUtil.asString(properties);
  }

  @Override
  public Filter setProperties(Map<String, Object> properties) {
    this.properties = properties;
    return this;
  }

  public void setPropertiesInternal(String properties) {
    if (properties != null) {
      JsonObject json = JsonUtil.asObject(properties);
      this.properties = JsonUtil.asMap(json);
    }
    else {
      this.properties = null;
    }
  }

  @Override
  public int getRevision() {
    return revision;
  }

  @Override
  public void setRevision(int revision) {
    this.revision = revision;
  }

  @Override
  public int getRevisionNext() {
    return revision + 1;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Query<?, ?>> Filter extend(T extendingQuery) {
    ensureNotNull(NotValidException.class, "extendingQuery", extendingQuery);

    if (!extendingQuery.getClass().equals(query.getClass())) {
      throw LOG.queryExtensionException(query.getClass().getName(), extendingQuery.getClass().getName());
    }

    FilterEntity copy = copyFilter();
    copy.setQuery(query.extend(extendingQuery));

    return copy;
  }

  @SuppressWarnings("unchecked")
  protected <T> JsonObjectConverter<T> getConverter() {
    JsonObjectConverter<T> converter = (JsonObjectConverter<T>) QUERY_CONVERTER.get(resourceType);
    if (converter != null) {
      return converter;
    }
    else {
      throw LOG.unsupportedResourceTypeException(resourceType);
    }
  }

  @Override
  public Object getPersistentState() {
    Map<String, Object> persistentState = new HashMap<>();
    persistentState.put("name", this.name);
    persistentState.put("owner", this.owner);
    persistentState.put(QUERY, this.query);
    persistentState.put("properties", this.properties);
    return persistentState;
  }

  protected FilterEntity copyFilter() {
    FilterEntity copy = new FilterEntity(getResourceType());
    copy.setName(getName());
    copy.setOwner(getOwner());
    copy.setQueryInternal(getQueryInternal());
    copy.setPropertiesInternal(getPropertiesInternal());
    return copy;
  }

  @Override
  public void postLoad() {
    if (query != null) {
      query.addValidator(StoredQueryValidator.get());
    }

  }
}
