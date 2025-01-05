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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.operaton.bpm.engine.*;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.ProcessInstanceQueryImpl;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.migration.MigrationPlan;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class BoundedNumberOfMaxResultsTest {

  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();

  protected ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testHelper);

  protected HistoryService historyService;
  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected IdentityService identityService;

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

  @Before
  public void assignServices() {
    historyService = engineRule.getHistoryService();
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
    identityService = engineRule.getIdentityService();
  }

  @Before
  public void enableMaxResultsLimit() {
    engineRule.getProcessEngineConfiguration()
        .setQueryMaxResultsLimit(10);
  }

  @Before
  public void authenticate() {
    engineRule.getIdentityService()
        .setAuthenticatedUserId("foo");
  }

  @After
  public void clearAuthentication() {
    engineRule.getIdentityService()
        .clearAuthentication();
  }

  @After
  public void resetQueryMaxResultsLimit() {
    engineRule.getProcessEngineConfiguration()
        .setQueryMaxResultsLimit(Integer.MAX_VALUE);
  }

  @Test
  public void shouldReturnUnboundedResults_UnboundMaxResults() {
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
  public void shouldReturnUnboundedResults_NotAuthenticated() {
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
  public void shouldReturnUnboundedResults_InsideCmd() {
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
  public void shouldReturnUnboundedResults_InsideCmd2() {
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
  public void shouldThrowException_UnboundedResultsForList() {
    // given
    ProcessInstanceQuery processInstanceQuery =
        runtimeService.createProcessInstanceQuery();

    // when/then
    assertThatThrownBy(() -> processInstanceQuery.list())
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("An unbound number of results is forbidden!");

  }

  @Test
  public void shouldThrowException_MaxResultsLimitExceeded() {
    // given
    ProcessInstanceQuery processInstanceQuery =
        runtimeService.createProcessInstanceQuery();

    // when/then
    assertThatThrownBy(() -> processInstanceQuery.listPage(0, 11))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Max results limit of 10 exceeded!");
  }

  @Test
  public void shouldNotThrowException_unboundedResultList() {
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
  public void shouldThrowExceptionWhenFilterQueryList_MaxResultsLimitExceeded() {
    // given
    Filter foo = engineRule.getFilterService().newTaskFilter("foo");
    foo.setQuery(taskService.createTaskQuery());
    engineRule.getFilterService().saveFilter(foo);

    String filterId = engineRule.getFilterService()
        .createFilterQuery()
        .singleResult()
        .getId();

    try {
      // when
      engineRule.getFilterService().list(filterId);
      fail("Exception expected!");
    } catch (BadUserRequestException e) {
      // then
      assertThat(e).hasMessage("An unbound number of results is forbidden!");
    }

    // clear
    engineRule.getFilterService().deleteFilter(filterId);
  }

  @Test
  public void shouldThrowExceptionWhenFilterQueryListPage_MaxResultsLimitExceeded() {
    // given
    Filter foo = engineRule.getFilterService().newTaskFilter("foo");
    foo.setQuery(taskService.createTaskQuery());
    engineRule.getFilterService().saveFilter(foo);

    String filterId = engineRule.getFilterService()
        .createFilterQuery()
        .singleResult()
        .getId();

    try {
      // when
      engineRule.getFilterService().listPage(filterId, 0, 11);
      fail("Exception expected!");
    } catch (BadUserRequestException e) {
      // then
      assertThat(e).hasMessage("Max results limit of 10 exceeded!");
    }

    // clear
    engineRule.getFilterService().deleteFilter(filterId);
  }

  @Test
  public void shouldThrowExceptionWhenExtendedFilterQueryList_MaxResultsLimitExceeded() {
    // given
    Filter foo = engineRule.getFilterService().newTaskFilter("foo");
    foo.setQuery(taskService.createTaskQuery());

    engineRule.getFilterService().saveFilter(foo);

    String filterId = engineRule.getFilterService()
        .createFilterQuery()
        .singleResult()
        .getId();

    TaskQuery extendingQuery = taskService.createTaskQuery()
        .taskCandidateGroup("aCandidateGroup");

    try {
      // when
      engineRule.getFilterService().list(filterId, extendingQuery);
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("An unbound number of results is forbidden!");
    }

    // clear
    engineRule.getFilterService().deleteFilter(filterId);
  }

  @Test
  public void shouldThrowExceptionWhenSyncSetRetriesForExternalTasks_MaxResultsLimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    testHelper.deploy(externalTaskProcess);

    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");

    try {
      // when
      engineRule.getExternalTaskService().updateRetries()
          .externalTaskQuery(engineRule.getExternalTaskService().createExternalTaskQuery())
          .set(5);
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @Test
  public void shouldSyncUpdateExternalTaskRetries() {
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
  public void shouldThrowExceptionWhenSyncSetRetriesForExtTasksByHistProcInstQuery_LimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    testHelper.deploy(externalTaskProcess);

    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");

    HistoricProcessInstanceQuery historicProcessInstanceQuery = engineRule.getHistoryService()
        .createHistoricProcessInstanceQuery();

    try {
      // when
      engineRule.getExternalTaskService().updateRetries()
          .historicProcessInstanceQuery(historicProcessInstanceQuery)
          .set(5);
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  public void shouldSyncUpdateExternalTaskRetriesProcInstQueryByHistProcInstQuery() {
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
  public void shouldSyncUpdateExternalTaskRetriesByProcInstQuery() {
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
  public void shouldThrowExceptionWhenSyncSetRetriesForExtTasksByProcInstQuery_LimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    testHelper.deploy(externalTaskProcess);

    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");

    try {
      // when
      engineRule.getExternalTaskService().updateRetries()
          .processInstanceQuery(engineRule.getRuntimeService().createProcessInstanceQuery())
          .set(5);
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  public void shouldSyncUpdateExternalTaskRetriesByHistProcInstQuery() {
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
  public void shouldThrowExceptionWhenSyncInstanceMigration_MaxResultsLimitExceeded() {
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

    try {
      // when
      runtimeService.newMigration(plan)
          .processInstanceQuery(runtimeService.createProcessInstanceQuery())
          .execute();
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @Test
  public void shouldSyncInstanceMigration() {
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
  public void shouldThrowExceptionWhenInstanceModification_MaxResultsLimitExceeded() {
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

    try {
      // when
      runtimeService.createModification(processDefinitionId)
          .startAfterActivity("userTask")
          .processInstanceQuery(runtimeService.createProcessInstanceQuery())
          .execute();
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @Test
  public void shouldSyncProcessInstanceModification() {
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
  public void shouldThrowExceptionWhenRestartProcessInstance_MaxResultsLimitExceeded() {
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

    try {
      // when
      runtimeService.restartProcessInstances(processDefinitionId)
          .historicProcessInstanceQuery(historyService.createHistoricProcessInstanceQuery())
          .startAfterActivity("startEvent")
          .execute();
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  public void shouldSyncRestartProcessInstance() {
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
  public void shouldThrowExceptionWhenUpdateProcessInstanceSuspensionState_LimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    testHelper.deploy(simpleProcess);

    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");

    try {
      // when
      runtimeService.updateProcessInstanceSuspensionState()
          .byProcessInstanceQuery(runtimeService.createProcessInstanceQuery())
          .suspend();
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @Test
  public void shouldThrowExceptionWhenUpdateProcessInstanceSuspensionStateByIds_LimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    testHelper.deploy(simpleProcess);

    List<String> instanceIds = new ArrayList<>();
    instanceIds.add(runtimeService.startProcessInstanceByKey("process").getId());
    instanceIds.add(runtimeService.startProcessInstanceByKey("process").getId());
    instanceIds.add(runtimeService.startProcessInstanceByKey("process").getId());

    try {
      // when
      runtimeService.updateProcessInstanceSuspensionState()
          .byProcessInstanceIds(instanceIds)
          .suspend();
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @Test
  public void shouldSyncUpdateProcessInstanceSuspensionState() {
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
  public void shouldThrowExceptionWhenUpdateProcInstSuspStateByHistProcInstQuery_LimitExceeded() {
    // given
    engineRule.getProcessEngineConfiguration().setQueryMaxResultsLimit(2);

    testHelper.deploy(simpleProcess);

    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");

    try {
      // when
      runtimeService.updateProcessInstanceSuspensionState()
          .byHistoricProcessInstanceQuery(historyService.createHistoricProcessInstanceQuery())
          .suspend();
      fail("Exception expected!");
    } catch (BadUserRequestException e) {

      // then
      assertThat(e).hasMessage("Max results limit of 2 exceeded!");
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Test
  public void shouldSyncUpdateProcessInstanceSuspensionStateByHistProcInstQuery() {
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
  public void shouldReturnResultWhenMaxResultsLimitNotExceeded() {
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
  public void shouldReturnResultWhenMaxResultsLimitNotExceeded2() {
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
  public void shouldReturnResultInsideJavaDelegate() {
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
  public void shouldReturnSingleResult_BoundedMaxResults() {
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
