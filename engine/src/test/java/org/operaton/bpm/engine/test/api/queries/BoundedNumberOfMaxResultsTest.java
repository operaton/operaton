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
package org.operaton.bpm.engine.test.api.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.ProcessInstanceQueryImpl;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

class BoundedNumberOfMaxResultsTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  HistoryService historyService;
  RuntimeService runtimeService;
  TaskService taskService;
  IdentityService identityService;
  FilterService filterService;

  protected BpmnModelInstance simpleProcess = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();

  protected BpmnModelInstance externalTaskProcess = Bpmn.createExecutableProcess("process")
      .startEvent()
      .serviceTask()
        .operatonExternalTask("aTopicName")
      .endEvent()
      .done();

  @BeforeEach
  void enableMaxResultsLimit() {
    engineRule.getProcessEngineConfiguration()
        .setQueryMaxResultsLimit(10);
  }

  @BeforeEach
  void authenticate() {
    engineRule.getIdentityService()
        .setAuthenticatedUserId("foo");
  }

  @AfterEach
  void clearAuthentication() {
    engineRule.getIdentityService()
        .clearAuthentication();
  }

  @AfterEach
  void resetQueryMaxResultsLimit() {
    engineRule.getProcessEngineConfiguration()
        .setQueryMaxResultsLimit(Integer.MAX_VALUE);
  }

  @Test
  void shouldReturnUnboundedResults_UnboundMaxResults() {
    // given
    engineRule.getProcessEngineConfiguration()
        .setQueryMaxResultsLimit(Integer.MAX_VALUE);

    ProcessInstanceQuery processInstanceQuery =
        runtimeService.createProcessInstanceQuery();

    // when
    List<ProcessInstance> processInstances = processInstanceQuery.list();

    // then
    assertThat(processInstances).isEmpty();
  }

  @Test
  void shouldReturnUnboundedResults_NotAuthenticated() {
    // given
    identityService.clearAuthentication();

    ProcessInstanceQuery processInstanceQuery =
        runtimeService.createProcessInstanceQuery();

    // when
    List<ProcessInstance> processInstances = processInstanceQuery.list();

    // then
    assertThat(processInstances).isEmpty();
  }

  @Test
  void shouldReturnUnboundedResults_InsideCmd() {
    // given
    engineRule.getProcessEngineConfiguration()
        .setQueryMaxResultsLimit(2);

    Task task = taskService.newTask();
    taskService.saveTask(task);

    engineRule.getProcessEngineConfiguration()
        .getCommandExecutorTxRequired()
        .execute((Command<Void>) commandContext -> {
      // when
      List<Task> tasks = commandContext.getProcessEngineConfiguration()
          .getTaskService()
          .createTaskQuery()
          .list();

      // then
      assertThat(tasks).hasSize(1);

      return null;
    });

    // clear
    taskService.deleteTask(task.getId(), true);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldReturnUnboundedResults_InsideCmd2() {
    // given
    engineRule.getProcessEngineConfiguration()
        .setQueryMaxResultsLimit(2);

    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent("startEvent")
        .endEvent()
        .done();

    String processDefinitionId = testHelper.deploy(process)
        .getDeployedProcessDefinitions()
        .get(0)
        .getId();

    String processInstanceId = runtimeService.startProcessInstanceByKey("process")
        .getProcessInstanceId();

    try {
      // when
      runtimeService.restartProcessInstances(processDefinitionId)
          .processInstanceIds(processInstanceId)
          .startAfterActivity("startEvent")
          .execute();

      // then
      // do not fail
    } catch (BadUserRequestException e) {
      fail("The query inside the command should not throw an exception!");
    }
  }

  @Test
  void shouldThrowException_UnboundedResultsForList() {
    // given
    ProcessInstanceQuery processInstanceQuery =
        runtimeService.createProcessInstanceQuery();

    // when/then
    assertThatThrownBy(processInstanceQuery::list)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("An unbound number of results is forbidden!");

  }

  @Test
  void shouldThrowException_MaxResultsLimitExceeded() {
    // given
    ProcessInstanceQuery processInstanceQuery =
        runtimeService.createProcessInstanceQuery();

    // when/then
    assertThatThrownBy(() -> processInstanceQuery.listPage(0, 11))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Max results limit of 10 exceeded!");
  }

  @Test
  void shouldNotThrowException_unboundedResultList() {
    // given
    ProcessInstanceQueryImpl processInstanceQuery =
        (ProcessInstanceQueryImpl) runtimeService.createProcessInstanceQuery();

    testHelper.deploy(simpleProcess);

    runtimeService.startProcessInstanceByKey("process");

    // when
    List<ProcessInstance> processInstances = processInstanceQuery.unlimitedList();

    // then
    assertThat(processInstances).hasSize(1);
  }

  @Test
  void shouldThrowExceptionWhenFilterQueryList_MaxResultsLimitExceeded() {
    // given
    Filter foo = filterService.newTaskFilter("foo");
    foo.setQuery(taskService.createTaskQuery());
    filterService.saveFilter(foo);

    String filterId = filterService
        .createFilterQuery()
        .singleResult()
        .getId();

    try {
      // when
      filterService.list(filterId);
      fail("Exception expected!");
    } catch (BadUserRequestException e) {
      // then
      assertThat(e).hasMessage("An unbound number of results is forbidden!");
    }

    // clear
    filterService.deleteFilter(filterId);
  }

  @Test
  void shouldThrowExceptionWhenFilterQueryListPage_MaxResultsLimitExceeded() {
    // given
    Filter foo = filterService.newTaskFilter("foo");
    foo.setQuery(taskService.createTaskQuery());
    filterService.saveFilter(foo);

    String filterId = filterService
        .createFilterQuery()
        .singleResult()
        .getId();

    try {
      // when
      filterService.listPage(filterId, 0, 11);
      fail("Exception expected!");
    } catch (BadUserRequestException e) {
      // then
      assertThat(e).hasMessage("Max results limit of 10 exceeded!");
    }

    // clear
    filterService.deleteFilter(filterId);
  }

  @Test
  void shouldThrowExceptionWhenExtendedFilterQueryList_MaxResultsLimitExceeded() {
    // given
    Filter foo = filterService.newTaskFilter("foo");
    foo.setQuery(taskService.createTaskQuery());

    filterService.saveFilter(foo);

    String filterId = filterService
        .createFilterQuery()
        .singleResult()
        .getId();

    TaskQuery extendingQuery = taskService.createTaskQuery()
        .taskCandidateGroup("aCandidateGroup");

    try {
      // when
      filterService.list(filterId, extendingQuery);
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("An unbound number of results is forbidden!");
    }

    // clear
    filterService.deleteFilter(filterId);
  }

  @Test
  void shouldThrowExceptionWhenSyncSetRetriesForExternalTasks_MaxResultsLimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    testHelper.deploy(externalTaskProcess);

    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    var externalTaskService = engineRule.getExternalTaskService().updateRetries()
          .externalTaskQuery(engineRule.getExternalTaskService().createExternalTaskQuery());

    try {
      // when
      externalTaskService.set(5);
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @Test
  void shouldSyncUpdateExternalTaskRetries() {
    // given
    testHelper.deploy(externalTaskProcess);

    runtimeService.startProcessInstanceByKey("process");

    try {
      // when
      engineRule.getExternalTaskService().updateRetries()
          .externalTaskQuery(engineRule.getExternalTaskService().createExternalTaskQuery())
          .set(5);
      // then: no exception
    } catch (BadUserRequestException e) {
      fail("No exception expected!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldThrowExceptionWhenSyncSetRetriesForExtTasksByHistProcInstQuery_LimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    testHelper.deploy(externalTaskProcess);

    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = engineRule.getHistoryService()
        .createHistoricProcessInstanceQuery();
    var externalTaskService = engineRule.getExternalTaskService().updateRetries()
          .historicProcessInstanceQuery(historicProcessInstanceQuery);

    try {
      // when
      externalTaskService.set(5);
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldSyncUpdateExternalTaskRetriesProcInstQueryByHistProcInstQuery() {
    // given
    testHelper.deploy(externalTaskProcess);

    runtimeService.startProcessInstanceByKey("process");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = engineRule.getHistoryService()
        .createHistoricProcessInstanceQuery();

    try {
      // when
      engineRule.getExternalTaskService().updateRetries()
          .historicProcessInstanceQuery(historicProcessInstanceQuery)
          .set(5);
      // then: no exception
    } catch (BadUserRequestException e) {
      fail("No exception expected!");
    }
  }

  @Test
  void shouldSyncUpdateExternalTaskRetriesByProcInstQuery() {
    // given
    testHelper.deploy(externalTaskProcess);

    runtimeService.startProcessInstanceByKey("process");

    try {
      // when
      engineRule.getExternalTaskService().updateRetries()
          .processInstanceQuery(engineRule.getRuntimeService().createProcessInstanceQuery())
          .set(5);
      // then: no exception
    } catch (BadUserRequestException e) {
      fail("No exception expected!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldThrowExceptionWhenSyncSetRetriesForExtTasksByProcInstQuery_LimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    testHelper.deploy(externalTaskProcess);

    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    var externalTaskService = engineRule.getExternalTaskService().updateRetries()
          .processInstanceQuery(engineRule.getRuntimeService().createProcessInstanceQuery());

    try {
      // when
      externalTaskService.set(5);
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldSyncUpdateExternalTaskRetriesByHistProcInstQuery() {
    // given
    testHelper.deploy(externalTaskProcess);

    runtimeService.startProcessInstanceByKey("process");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = engineRule.getHistoryService()
        .createHistoricProcessInstanceQuery();

    try {
      // when
      engineRule.getExternalTaskService().updateRetries()
          .historicProcessInstanceQuery(historicProcessInstanceQuery)
          .set(5);
      // then: no exception
    } catch (BadUserRequestException e) {
      fail("No exception expected!");
    }
  }

  @Test
  void shouldThrowExceptionWhenSyncInstanceMigration_MaxResultsLimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    String source =
        testHelper.deploy(simpleProcess)
            .getDeployedProcessDefinitions()
            .get(0)
            .getId();

    String target =
        testHelper.deploy(simpleProcess)
            .getDeployedProcessDefinitions()
            .get(0)
            .getId();

    MigrationPlan plan = runtimeService.createMigrationPlan(source, target)
        .mapEqualActivities()
        .build();

    runtimeService.startProcessInstanceById(source);
    runtimeService.startProcessInstanceById(source);
    runtimeService.startProcessInstanceById(source);
    var migrationPlanExecutionBuilder = runtimeService.newMigration(plan)
          .processInstanceQuery(runtimeService.createProcessInstanceQuery());

    try {
      // when
      migrationPlanExecutionBuilder.execute();
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @Test
  void shouldSyncInstanceMigration() {
    // given
    String source =
        testHelper.deploy(simpleProcess)
            .getDeployedProcessDefinitions()
            .get(0)
            .getId();

    String target =
        testHelper.deploy(simpleProcess)
            .getDeployedProcessDefinitions()
            .get(0)
            .getId();

    MigrationPlan plan = runtimeService.createMigrationPlan(source, target)
        .mapEqualActivities()
        .build();

    runtimeService.startProcessInstanceById(source);

    try {
      // when
      runtimeService.newMigration(plan)
          .processInstanceQuery(runtimeService.createProcessInstanceQuery())
          .execute();

      // then: no exception thrown
    } catch (BadUserRequestException e) {
      fail("No Exception expected!");
    }
  }

  @Test
  void shouldThrowExceptionWhenInstanceModification_MaxResultsLimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .userTask("userTask")
        .endEvent()
        .done();

    String processDefinitionId =
        testHelper.deploy(process)
            .getDeployedProcessDefinitions()
            .get(0)
            .getId();

    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    var modificationBuilder = runtimeService.createModification(processDefinitionId)
          .startAfterActivity("userTask")
          .processInstanceQuery(runtimeService.createProcessInstanceQuery());

    try {
      // when
      modificationBuilder.execute();
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @Test
  void shouldSyncProcessInstanceModification() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .userTask("userTask")
        .endEvent()
        .done();

    String processDefinitionId =
        testHelper.deploy(process)
            .getDeployedProcessDefinitions()
            .get(0)
            .getId();

    runtimeService.startProcessInstanceByKey("process");

    try {
      // when
      runtimeService.createModification(processDefinitionId)
          .startAfterActivity("userTask")
          .processInstanceQuery(runtimeService.createProcessInstanceQuery())
          .execute();

      // then: no exception is thrown
    } catch (BadUserRequestException e) {
      fail("Exception expected!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldThrowExceptionWhenRestartProcessInstance_MaxResultsLimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent("startEvent")
        .endEvent()
        .done();

    String processDefinitionId =
        testHelper.deploy(process)
            .getDeployedProcessDefinitions()
            .get(0)
            .getId();

    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    var restartProcessInstanceBuilder = runtimeService.restartProcessInstances(processDefinitionId)
          .historicProcessInstanceQuery(historyService.createHistoricProcessInstanceQuery())
          .startAfterActivity("startEvent");

    try {
      // when
      restartProcessInstanceBuilder.execute();
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldSyncRestartProcessInstance() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent("startEvent")
        .endEvent()
        .done();

    String processDefinitionId =
        testHelper.deploy(process)
            .getDeployedProcessDefinitions()
            .get(0)
            .getId();

    runtimeService.startProcessInstanceByKey("process");

    try {
      // when
      runtimeService.restartProcessInstances(processDefinitionId)
          .historicProcessInstanceQuery(historyService.createHistoricProcessInstanceQuery())
          .startAfterActivity("startEvent")
          .execute();

      // then: No Exception is thrown
    } catch (BadUserRequestException e) {

      fail("Exception expected!");
    }
  }

  @Test
  void shouldThrowExceptionWhenUpdateProcessInstanceSuspensionState_LimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    testHelper.deploy(simpleProcess);

    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    var updateProcessInstanceSuspensionStateSelectBuilder = runtimeService.updateProcessInstanceSuspensionState()
          .byProcessInstanceQuery(runtimeService.createProcessInstanceQuery());

    try {
      // when
      updateProcessInstanceSuspensionStateSelectBuilder.suspend();
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @Test
  void shouldThrowExceptionWhenUpdateProcessInstanceSuspensionStateByIds_LimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    testHelper.deploy(simpleProcess);

    List<String> instanceIds = new ArrayList<>();
    instanceIds.add(runtimeService.startProcessInstanceByKey("process").getId());
    instanceIds.add(runtimeService.startProcessInstanceByKey("process").getId());
    instanceIds.add(runtimeService.startProcessInstanceByKey("process").getId());
    var updateProcessInstanceSuspensionStateSelectBuilder = runtimeService.updateProcessInstanceSuspensionState()
          .byProcessInstanceIds(instanceIds);

    try {
      // when
      updateProcessInstanceSuspensionStateSelectBuilder.suspend();
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @Test
  void shouldSyncUpdateProcessInstanceSuspensionState() {
    // given
    testHelper.deploy(simpleProcess);

    runtimeService.startProcessInstanceByKey("process");

    try {
      // when
      runtimeService.updateProcessInstanceSuspensionState()
          .byProcessInstanceQuery(runtimeService.createProcessInstanceQuery())
          .suspend();

      // then: no exception expected
    } catch (BadUserRequestException e) {
      fail("No exception expected!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldThrowExceptionWhenUpdateProcInstSuspStateByHistProcInstQuery_LimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    testHelper.deploy(simpleProcess);

    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    var updateProcessInstanceSuspensionStateSelectBuilder = runtimeService.updateProcessInstanceSuspensionState()
          .byHistoricProcessInstanceQuery(historyService.createHistoricProcessInstanceQuery());

    try {
      // when
      updateProcessInstanceSuspensionStateSelectBuilder.suspend();
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldSyncUpdateProcessInstanceSuspensionStateByHistProcInstQuery() {
    // given
    testHelper.deploy(simpleProcess);

    runtimeService.startProcessInstanceByKey("process");

    try {
      // when
      runtimeService.updateProcessInstanceSuspensionState()
          .byHistoricProcessInstanceQuery(historyService.createHistoricProcessInstanceQuery())
          .suspend();

      // then: no exception expected
    } catch (BadUserRequestException e) {
      fail("No exception expected!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldReturnResultWhenMaxResultsLimitNotExceeded() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent("startEvent")
        .endEvent()
        .done();

    testHelper.deploy(process);

    runtimeService.startProcessInstanceByKey("process");

    HistoricProcessInstanceQuery historicProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery();

    // when
    List<HistoricProcessInstance> historicProcessInstances =
        historicProcessInstanceQuery.listPage(0, 10);

    // then
    assertThat(historicProcessInstances).hasSize(1);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldReturnResultWhenMaxResultsLimitNotExceeded2() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent("startEvent")
        .endEvent()
        .done();

    testHelper.deploy(process);

    runtimeService.startProcessInstanceByKey("process");

    HistoricProcessInstanceQuery historicProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery();

    // when
    List<HistoricProcessInstance> historicProcessInstances =
        historicProcessInstanceQuery.listPage(0, 9);

    // then
    assertThat(historicProcessInstances).hasSize(1);
  }

  @Test
  void shouldReturnResultInsideJavaDelegate() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent("startEvent")
        .serviceTask()
          .operatonClass(BoundedNumberOfMaxResultsDelegate.class)
        .endEvent()
        .done();

    testHelper.deploy(process);

    try {
      // when
      runtimeService.startProcessInstanceByKey("process");

      // then: should not fail
    } catch (BadUserRequestException e) {
      fail("Should not throw exception inside command!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  void shouldReturnSingleResult_BoundedMaxResults() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .endEvent()
        .done();

    testHelper.deploy(process);

    runtimeService.startProcessInstanceByKey("process");

    HistoricProcessInstanceQuery historicProcessInstanceQuery =
        historyService.createHistoricProcessInstanceQuery();

    // when
    HistoricProcessInstance processInstance = historicProcessInstanceQuery.singleResult();

    // then
    assertThat(processInstance).isNotNull();
  }

}
