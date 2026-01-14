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
package org.operaton.bpm.engine.test.api.authorization.history;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricActivityStatistics;
import org.operaton.bpm.engine.history.HistoricActivityStatisticsQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ_HISTORY;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
class HistoricActivityStatisticsAuthorizationTest extends AuthorizationTest {

  protected static final String PROCESS_KEY = "oneTaskProcess";

  @Override
  @BeforeEach
  public void setUp() {
    testRule.deploy(
        "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml");
    super.setUp();
  }

  // historic activity statistics query //////////////////////////////////

  @Test
  void testQueryWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    // when
    HistoricActivityStatisticsQuery query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId);

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithReadHistoryPermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricActivityStatisticsQuery query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId);

    // then
    verifyQueryResults(query, 1);
    verifyStatisticsResult(query.singleResult(), 3, 0, 0, 0);
  }

  @Test
  void testQueryWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricActivityStatisticsQuery query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId);

    // then
    verifyQueryResults(query, 1);
    verifyStatisticsResult(query.singleResult(), 3, 0, 0, 0);
  }

  @Test
  void testQueryMultiple() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricActivityStatisticsQuery query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId);

    // then
    verifyQueryResults(query, 1);
    verifyStatisticsResult(query.singleResult(), 3, 0, 0, 0);
  }

  @Test
  void shouldNotFindStatisticsWithRevokedReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);
    createRevokeAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricActivityStatisticsQuery query = historyService.createHistoricActivityStatisticsQuery(processDefinitionId);

    // then
    verifyQueryResults(query, 0);
  }

  // historic activity statistics query (including finished) //////////////////////////////////

  @Test
  void testQueryIncludingFinishedWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    String taskId = selectAnyTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryIncludingFinishedWithReadHistoryPermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    String taskId = selectAnyTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished();

    // then
    verifyQueryResults(query, 3);
    List<HistoricActivityStatistics> statistics = query.list();

    HistoricActivityStatistics start = getStatisticsByKey(statistics, "theStart");
    verifyStatisticsResult(start, 0, 3, 0, 0);

    HistoricActivityStatistics task = getStatisticsByKey(statistics, "theTask");
    verifyStatisticsResult(task, 2, 1, 0, 0);

    HistoricActivityStatistics end = getStatisticsByKey(statistics, "theEnd");
    verifyStatisticsResult(end, 0, 1, 0, 0);
  }

  @Test
  void testQueryIncludingFinishedWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    String taskId = selectAnyTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished();

    // then
    verifyQueryResults(query, 3);
    List<HistoricActivityStatistics> statistics = query.list();

    HistoricActivityStatistics start = getStatisticsByKey(statistics, "theStart");
    verifyStatisticsResult(start, 0, 3, 0, 0);

    HistoricActivityStatistics task = getStatisticsByKey(statistics, "theTask");
    verifyStatisticsResult(task, 2, 1, 0, 0);

    HistoricActivityStatistics end = getStatisticsByKey(statistics, "theEnd");
    verifyStatisticsResult(end, 0, 1, 0, 0);
  }

  // historic activity statistics query (including canceled) //////////////////////////////////

  @Test
  void testQueryIncludingCanceledWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstanceId, null);
    enableAuthorization();

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeCanceled();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryIncludingCanceledWithReadHistoryPermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstanceId, null);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeCanceled();

    // then
    verifyQueryResults(query, 1);
    List<HistoricActivityStatistics> statistics = query.list();

    HistoricActivityStatistics task = getStatisticsByKey(statistics, "theTask");
    verifyStatisticsResult(task, 2, 0, 1, 0);
  }

  @Test
  void testQueryIncludingCanceledWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstanceId, null);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeCanceled();

    // then
    verifyQueryResults(query, 1);
    List<HistoricActivityStatistics> statistics = query.list();

    HistoricActivityStatistics task = getStatisticsByKey(statistics, "theTask");
    verifyStatisticsResult(task, 2, 0, 1, 0);
  }

  // historic activity statistics query (including complete scope) //////////////////////////////////

  @Test
  void testQueryIncludingCompleteScopeWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    String taskId = selectAnyTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeCompleteScope();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryIncludingCompleteScopeWithReadHistoryPermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    String taskId = selectAnyTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeCompleteScope();

    // then
    verifyQueryResults(query, 2);
    List<HistoricActivityStatistics> statistics = query.list();

    HistoricActivityStatistics task = getStatisticsByKey(statistics, "theTask");
    verifyStatisticsResult(task, 2, 0, 0, 0);

    HistoricActivityStatistics end = getStatisticsByKey(statistics, "theEnd");
    verifyStatisticsResult(end, 0, 0, 0, 1);
  }

  @Test
  void testQueryIncludingCompleteScopeWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);

    String taskId = selectAnyTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeCompleteScope();

    // then
    verifyQueryResults(query, 2);
    List<HistoricActivityStatistics> statistics = query.list();

    HistoricActivityStatistics task = getStatisticsByKey(statistics, "theTask");
    verifyStatisticsResult(task, 2, 0, 0, 0);

    HistoricActivityStatistics end = getStatisticsByKey(statistics, "theEnd");
    verifyStatisticsResult(end, 0, 0, 0, 1);
  }

  // historic activity statistics query (including all) //////////////////////////////////

  @Test
  void testQueryIncludingAllWithoutAuthorization() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstanceId, null);
    enableAuthorization();

    String taskId = selectAnyTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished()
        .includeCanceled()
        .includeCompleteScope();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryIncludingAllWithReadHistoryPermissionOnProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstanceId, null);
    enableAuthorization();

    String taskId = selectAnyTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, userId, READ_HISTORY);

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished()
        .includeCanceled()
        .includeCompleteScope();

    // then
    verifyQueryResults(query, 3);
    List<HistoricActivityStatistics> statistics = query.list();

    HistoricActivityStatistics start = getStatisticsByKey(statistics, "theStart");
    verifyStatisticsResult(start, 0, 3, 0, 0);

    HistoricActivityStatistics task = getStatisticsByKey(statistics, "theTask");
    verifyStatisticsResult(task, 1, 2, 1, 0);

    HistoricActivityStatistics end = getStatisticsByKey(statistics, "theEnd");
    verifyStatisticsResult(end, 0, 1, 0, 1);
  }

  @Test
  void testQueryIncludingAllWithReadHistoryPermissionOnAnyProcessDefinition() {
    // given
    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    startProcessInstanceByKey(PROCESS_KEY);
    startProcessInstanceByKey(PROCESS_KEY);
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    disableAuthorization();
    runtimeService.deleteProcessInstance(processInstanceId, null);
    enableAuthorization();

    String taskId = selectAnyTask().getId();
    disableAuthorization();
    taskService.complete(taskId);
    enableAuthorization();

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_HISTORY);

    // when
    HistoricActivityStatisticsQuery query = historyService
        .createHistoricActivityStatisticsQuery(processDefinitionId)
        .includeFinished()
        .includeCanceled()
        .includeCompleteScope();

    // then
    verifyQueryResults(query, 3);
    List<HistoricActivityStatistics> statistics = query.list();

    HistoricActivityStatistics start = getStatisticsByKey(statistics, "theStart");
    verifyStatisticsResult(start, 0, 3, 0, 0);

    HistoricActivityStatistics task = getStatisticsByKey(statistics, "theTask");
    verifyStatisticsResult(task, 1, 2, 1, 0);

    HistoricActivityStatistics end = getStatisticsByKey(statistics, "theEnd");
    verifyStatisticsResult(end, 0, 1, 0, 1);
  }

  // helper ////////////////////////////////////////////////////////

  protected void verifyStatisticsResult(HistoricActivityStatistics statistics, int instances, int finished, int canceled, int completeScope) {
    assertThat(statistics.getInstances()).as("Instances").isEqualTo(instances);
    assertThat(statistics.getFinished()).as("Finished").isEqualTo(finished);
    assertThat(statistics.getCanceled()).as("Canceled").isEqualTo(canceled);
    assertThat(statistics.getCompleteScope()).as("Complete Scope").isEqualTo(completeScope);
  }

  protected HistoricActivityStatistics getStatisticsByKey(List<HistoricActivityStatistics> statistics, String key) {
    for (HistoricActivityStatistics result : statistics) {
      if (key.equals(result.getId())) {
        return result;
      }
    }
    fail("No statistics found for key '%s'.".formatted(key));
    return null;
  }

  protected Task selectAnyTask() {
    disableAuthorization();
    Task task = taskService.createTaskQuery().listPage(0, 1).get(0);
    enableAuthorization();
    return task;
  }

}
