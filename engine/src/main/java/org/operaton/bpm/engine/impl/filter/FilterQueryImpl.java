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
package org.operaton.bpm.engine.impl.filter;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serial;
import java.util.List;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.filter.FilterQuery;
import org.operaton.bpm.engine.impl.AbstractQuery;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

/**
 * @author Sebastian Menski
 */
public class FilterQueryImpl extends AbstractQuery<FilterQuery, Filter> implements FilterQuery {

  @Serial private static final long serialVersionUID = 1L;
  protected String filterId;
  protected String resourceType;
  protected String name;
  protected String nameLike;
  protected String owner;

  public FilterQueryImpl() {
  }

  public FilterQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }

  @Override
  public FilterQuery filterId(String filterId) {
    ensureNotNull("filterId", filterId);
    this.filterId = filterId;
    return this;
  }

  @Override
  public FilterQuery filterResourceType(String resourceType) {
    ensureNotNull("resourceType", resourceType);
    this.resourceType = resourceType;
    return this;
  }

  @Override
  public FilterQuery filterName(String name) {
    ensureNotNull("name", name);
    this.name = name;
    return this;
  }

  @Override
  public FilterQuery filterNameLike(String nameLike) {
    ensureNotNull("nameLike", nameLike);
    this.nameLike = nameLike;
    return this;
  }

  @Override
  public FilterQuery filterOwner(String owner) {
    ensureNotNull("owner", owner);
    this.owner = owner;
    return this;
  }

  @Override
  public FilterQuery orderByFilterId() {
    return orderBy(FilterQueryProperty.FILTER_ID);
  }

  @Override
  public FilterQuery orderByFilterResourceType() {
    return orderBy(FilterQueryProperty.RESOURCE_TYPE);
  }

  @Override
  public FilterQuery orderByFilterName() {
    return orderBy(FilterQueryProperty.NAME);
  }

  @Override
  public FilterQuery orderByFilterOwner() {
    return orderBy(FilterQueryProperty.OWNER);
  }

  @Override
  public List<Filter> executeList(CommandContext commandContext, Page page) {
    return commandContext
      .getFilterManager()
      .findFiltersByQueryCriteria(this);
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    return commandContext
      .getFilterManager()
      .findFilterCountByQueryCriteria(this);
  }

}
