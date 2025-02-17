/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.api.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Thorben Lindhauer
 *
 */
public class TaskQueryDisabledAdhocExpressionsTest extends PluggableProcessEngineTest {

  protected static final String EXPECTED_ADHOC_QUERY_FAILURE_MESSAGE = "Expressions are forbidden in adhoc queries. "
      + "This behavior can be toggled in the process engine configuration";
  public static final String STATE_MANIPULATING_EXPRESSION =
      "${''.getClass().forName('" + TaskQueryDisabledAdhocExpressionsTest.class.getName() + "').getField('MUTABLE_FIELD').setLong(null, 42)}";

  public static long MUTABLE_FIELD = 0;

  @Test
  public void testDefaultSetting() {
    assertThat(processEngineConfiguration.isEnableExpressionsInStoredQueries()).isTrue();
    assertThat(processEngineConfiguration.isEnableExpressionsInAdhocQueries()).isFalse();
  }

  @Before
  public void setUp() {
    MUTABLE_FIELD = 0;
  }

  @Test
  public void testAdhocExpressionsFail() {
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
  public void testExtendStoredFilterByExpression() {

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
  public void testExtendStoredFilterByScalar() {
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
    return MUTABLE_FIELD == 0;
  }

  protected void executeAndValidateFailingQuery(TaskQuery query) {
    try {
      query.list();
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent(EXPECTED_ADHOC_QUERY_FAILURE_MESSAGE, e.getMessage());
    }

    assertThat(fieldIsUnchanged()).isTrue();

    try {
      query.count();
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent(EXPECTED_ADHOC_QUERY_FAILURE_MESSAGE, e.getMessage());
    }

    assertThat(fieldIsUnchanged()).isTrue();
  }

  protected void extendFilterAndValidateFailingQuery(Filter filter, TaskQuery query) {
    try {
      filterService.list(filter.getId(), query);
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent(EXPECTED_ADHOC_QUERY_FAILURE_MESSAGE, e.getMessage());
    }

    assertThat(fieldIsUnchanged()).isTrue();

    try {
      filterService.count(filter.getId(), query);
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent(EXPECTED_ADHOC_QUERY_FAILURE_MESSAGE, e.getMessage());
    }

    assertThat(fieldIsUnchanged()).isTrue();
  }
}
