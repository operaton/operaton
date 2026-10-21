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
package org.operaton.bpm.engine.impl;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.filter.FilterQuery;
import org.operaton.bpm.engine.impl.cmd.CreateFilterCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteFilterCmd;
import org.operaton.bpm.engine.impl.cmd.ExecuteFilterCountCmd;
import org.operaton.bpm.engine.impl.cmd.ExecuteFilterListCmd;
import org.operaton.bpm.engine.impl.cmd.ExecuteFilterListPageCmd;
import org.operaton.bpm.engine.impl.cmd.ExecuteFilterSingleResultCmd;
import org.operaton.bpm.engine.impl.cmd.GetFilterCmd;
import org.operaton.bpm.engine.impl.cmd.SaveFilterCmd;
import org.operaton.bpm.engine.impl.filter.FilterQueryImpl;
import org.operaton.bpm.engine.query.Query;


/**
 * @author Sebastian Menski
 */
public @NullMarked class FilterServiceImpl extends ServiceImpl implements FilterService {

  @Override
  public Filter newTaskFilter() {
    return getCommandExecutor().execute(new CreateFilterCmd(EntityTypes.TASK));
  }

  @Override
  public Filter newTaskFilter(String filterName) {
    return newTaskFilter().setName(filterName);
  }

  @Override
  public FilterQuery createFilterQuery() {
    return new FilterQueryImpl(commandExecutor);
  }

  @Override
  public FilterQuery createTaskFilterQuery() {
    return new FilterQueryImpl(commandExecutor).filterResourceType(EntityTypes.TASK);
  }

  @Override
  public Filter saveFilter(Filter filter) {
    return getCommandExecutor().execute(new SaveFilterCmd(filter));
  }

  @Override
  public Filter getFilter(String filterId) {
    return getCommandExecutor().execute(new GetFilterCmd(filterId));
  }

  @Override
  public void deleteFilter(String filterId) {
    getCommandExecutor().execute(new DeleteFilterCmd(filterId));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> List<T> list(String filterId) {
    return (List<T>) getCommandExecutor().execute(new ExecuteFilterListCmd(filterId));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, Q extends Query<?, T>> List<T> list(String filterId, @Nullable Q extendingQuery) {
    return (List<T>) getCommandExecutor().execute(new ExecuteFilterListCmd(filterId, extendingQuery));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> List<T> listPage(String filterId, int firstResult, int maxResults) {
    return (List<T>) getCommandExecutor().execute(new ExecuteFilterListPageCmd(filterId, firstResult, maxResults));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, Q extends Query<?, T>> List<T> listPage(String filterId, @Nullable Q extendingQuery, int firstResult, int maxResults) {
    return (List<T>) getCommandExecutor().execute(new ExecuteFilterListPageCmd(filterId, extendingQuery, firstResult, maxResults));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T singleResult(String filterId) {
    return (T) getCommandExecutor().execute(new ExecuteFilterSingleResultCmd(filterId));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T, Q extends Query<?, T>> T singleResult(String filterId, @Nullable Q extendingQuery) {
    return (T) getCommandExecutor().execute(new ExecuteFilterSingleResultCmd(filterId, extendingQuery));
  }

  @Override
  public Long count(String filterId) {
    return getCommandExecutor().execute(new ExecuteFilterCountCmd(filterId));
  }

  @Override
  public Long count(String filterId, @Nullable Query<?, ?> extendingQuery) {
    return getCommandExecutor().execute(new ExecuteFilterCountCmd(filterId, extendingQuery));
  }

}
