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
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
public class TaskQueryDisabledAdhocExpressionsTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected static final String EXPECTED_ADHOC_QUERY_FAILURE_MESSAGE = "Expressions are forbidden in adhoc queries. "
      + "This behavior can be toggled in the process engine configuration";
  public static final String STATE_MANIPULATING_EXPRESSION =
      "${''.getClass().forName('" + TaskQueryDisabledAdhocExpressionsTest.class.getName() + "').getField('MUTABLE_FIELD').setLong(null, 42)}";

  public static long mutableField;

  ProcessEngineConfigurationImpl processEngineConfiguration;
  TaskService taskService;
  FilterService filterService;

  @Test
  void testDefaultSetting() {
    assertThat(processEngineConfiguration.isEnableExpressionsInStoredQueries()).isTrue();
    assertThat(processEngineConfiguration.isEnableExpressionsInAdhocQueries()).isFalse();
  }

  @BeforeEach
  void setUp() {
    mutableField = 0;
  }

  @Test
  void testAdhocExpressionsFail() {
    executeAndValidateFailingQuery(taskService.createTaskQuery().dueAfterExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().dueBeforeExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().dueDateExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().followUpAfterExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().followUpBeforeExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().followUpBeforeOrNotExistentExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().followUpDateExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().taskAssigneeExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().taskAssigneeLikeExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().taskCandidateGroupExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().taskCandidateGroupInExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().taskCandidateUserExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().taskCreatedAfterExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().taskCreatedBeforeExpression(STATE_MANIPULATING_EXPRESSION));
    executeAndValidateFailingQuery(taskService.createTaskQuery().taskOwnerExpression(STATE_MANIPULATING_EXPRESSION));
  }

  @Test
  void testExtendStoredFilterByExpression() {

    // given a stored filter
    TaskQuery taskQuery = taskService.createTaskQuery().dueAfterExpression("${now()}");
    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(taskQuery);
    filterService.saveFilter(filter);

    // it is possible to execute the stored query with an expression
    assertThat(filterService.count(filter.getId())).isEqualTo(Long.valueOf(0));
    assertThat(filterService.list(filter.getId())).isEmpty();

    // but it is not possible to executed the filter with an extended query that uses expressions
    extendFilterAndValidateFailingQuery(filter, taskService.createTaskQuery().dueAfterExpression(STATE_MANIPULATING_EXPRESSION));

    // cleanup
    filterService.deleteFilter(filter.getId());
  }

  @Test
  void testExtendStoredFilterByScalar() {
    // given a stored filter
    TaskQuery taskQuery = taskService.createTaskQuery().dueAfterExpression("${now()}");
    Filter filter = filterService.newTaskFilter("filter");
    filter.setQuery(taskQuery);
    filterService.saveFilter(filter);

    // it is possible to execute the stored query with an expression
    assertThat(filterService.count(filter.getId())).isEqualTo(Long.valueOf(0));
    assertThat(filterService.list(filter.getId())).isEmpty();

    // and it is possible to extend the filter query when not using an expression
    assertThat(filterService.count(filter.getId(), taskService.createTaskQuery().dueAfter(new Date()))).isEqualTo(Long.valueOf(0));
    assertThat(filterService.list(filter.getId(), taskService.createTaskQuery().dueAfter(new Date()))).isEmpty();

    // cleanup
    filterService.deleteFilter(filter.getId());
  }

  protected boolean fieldIsUnchanged() {
    return mutableField == 0;
  }

  protected void executeAndValidateFailingQuery(TaskQuery query) {
    // when/then
    assertThatThrownBy(query::list)
        .isInstanceOf(BadUserRequestException.class)
        .hasMessageContaining(EXPECTED_ADHOC_QUERY_FAILURE_MESSAGE);

    assertThat(fieldIsUnchanged()).isTrue();

    // when/then
    assertThatThrownBy(query::count)
        .isInstanceOf(BadUserRequestException.class)
        .hasMessageContaining(EXPECTED_ADHOC_QUERY_FAILURE_MESSAGE);

    assertThat(fieldIsUnchanged()).isTrue();
  }

  protected void extendFilterAndValidateFailingQuery(Filter filter, TaskQuery query) {
    // given
    String filterId = filter.getId();

    // when/then
    assertThatThrownBy(() -> filterService.list(filterId, query))
        .isInstanceOf(BadUserRequestException.class)
        .hasMessageContaining(EXPECTED_ADHOC_QUERY_FAILURE_MESSAGE);

    assertThat(fieldIsUnchanged()).isTrue();

    // when/then
    assertThatThrownBy(() -> filterService.count(filterId, query))
        .isInstanceOf(BadUserRequestException.class)
        .hasMessageContaining(EXPECTED_ADHOC_QUERY_FAILURE_MESSAGE);

    assertThat(fieldIsUnchanged()).isTrue();
  }
}
