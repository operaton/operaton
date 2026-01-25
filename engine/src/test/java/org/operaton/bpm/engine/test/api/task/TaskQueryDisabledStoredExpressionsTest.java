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
package org.operaton.bpm.engine.test.api.task;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.FilterEntity;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
public class TaskQueryDisabledStoredExpressionsTest {

  protected static final String EXPECTED_STORED_QUERY_FAILURE_MESSAGE =
      "Expressions are forbidden in stored queries. This behavior can be toggled in the process engine configuration";
  public static final String STATE_MANIPULATING_EXPRESSION =
      "${''.getClass().forName('" + TaskQueryDisabledStoredExpressionsTest.class.getName() + "').getField('MUTABLE_FIELD').setLong(null, 42)}";

  public static long mutableField;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurationResource("org/operaton/bpm/engine/test/api/task/task-query-disabled-stored-expressions-test.operaton.cfg.xml")
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  TaskService taskService;
  FilterService filterService;

  @BeforeEach
  void setUp() {
    mutableField = 0;
  }

  @Test
  void testStoreFilterWithoutExpression() {
    TaskQuery taskQuery = taskService.createTaskQuery().dueAfter(new Date());
    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(taskQuery);

    // saving the filter suceeds
    filterService.saveFilter(filter);
    assertThat(filterService.createFilterQuery().count()).isOne();

    // cleanup
    filterService.deleteFilter(filter.getId());
  }

  @Test
  void testStoreFilterWithExpression() {
    // given
    TaskQuery taskQuery = taskService.createTaskQuery().dueAfterExpression(STATE_MANIPULATING_EXPRESSION);
    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(taskQuery);

    // when/then
    assertThatThrownBy(() -> filterService.saveFilter(filter))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining(EXPECTED_STORED_QUERY_FAILURE_MESSAGE);
    assertThat(fieldIsUnchanged()).isTrue();
  }

  @Test
  void testUpdateFilterWithExpression() {
    // given a stored filter
    TaskQuery taskQuery = taskService.createTaskQuery().dueAfter(new Date());
    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(taskQuery);
    filterService.saveFilter(filter);

    // when - updating the filter with an expression does not succeed
    filter.setQuery(taskQuery.dueBeforeExpression(STATE_MANIPULATING_EXPRESSION));
    assertThat(filterService.createFilterQuery().count()).isOne();

    // then
    assertThatThrownBy(() -> filterService.saveFilter(filter))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining(EXPECTED_STORED_QUERY_FAILURE_MESSAGE);
    assertThat(fieldIsUnchanged()).isTrue();

    // cleanup
    filterService.deleteFilter(filter.getId());
  }

  @Test
  void testCannotExecuteStoredFilter() {
    final TaskQuery filterQuery = taskService.createTaskQuery().dueAfterExpression(STATE_MANIPULATING_EXPRESSION);

    // store a filter bypassing validation
    // the API way of doing this would be by reconfiguring the engine
    String filterId = processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
      FilterEntity filter = new FilterEntity(EntityTypes.TASK);
      filter.setQuery(filterQuery);
      filter.setName("filter");
      commandContext.getDbEntityManager().insert(filter);
      return filter.getId();
    });

    extendFilterAndValidateFailingQuery(filterId, null);

    // cleanup
    filterService.deleteFilter(filterId);
  }

  protected boolean fieldIsUnchanged() {
    return mutableField == 0;
  }

  protected void extendFilterAndValidateFailingQuery(String filterId, TaskQuery query) {
    // when/then
    assertThatThrownBy(() -> filterService.list(filterId, query))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining(EXPECTED_STORED_QUERY_FAILURE_MESSAGE);

    assertThat(fieldIsUnchanged()).isTrue();

    assertThatThrownBy(() -> filterService.count(filterId, query))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining(EXPECTED_STORED_QUERY_FAILURE_MESSAGE);

    assertThat(fieldIsUnchanged()).isTrue();
  }
}
