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
package org.operaton.bpm.engine.test.api.history;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricDetailQuery;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.ProcessInstanceQueryTest;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.commons.utils.CollectionUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Frederik Heremans
 * @author Falko Menge
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
public class HistoryServiceTest {

  public static final String ONE_TASK_PROCESS = "oneTaskProcess";
  private static final Logger LOG = ProcessEngineLogger.TEST_LOGGER.getLogger();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected HistoryService historyService;
  protected TaskService taskService;
  protected IdentityService identityService;
  protected ManagementService managementService;
  protected CaseService caseService;

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testHistoricProcessInstanceQuery() {
    // With a clean ProcessEngine, no instances should be available
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isOne();

    // Complete the task and check if the size is count 1
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertThat(tasks).hasSize(1);
    taskService.complete(tasks.get(0).getId());
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isOne();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testHistoricProcessInstanceQueryOrderBy() {
    // With a clean ProcessEngine, no instances should be available
    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);

    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertThat(tasks).hasSize(1);
    taskService.complete(tasks.get(0).getId());

    historyService.createHistoricTaskInstanceQuery().orderByDeleteReason().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByExecutionId().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByHistoricActivityInstanceId().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByHistoricActivityInstanceStartTime().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByHistoricTaskInstanceDuration().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByHistoricTaskInstanceEndTime().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByProcessDefinitionId().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByProcessInstanceId().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByTaskAssignee().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByTaskDefinitionKey().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByTaskDescription().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByTaskId().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByTaskName().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByTaskOwner().asc().list();
    historyService.createHistoricTaskInstanceQuery().orderByTaskPriority().asc().list();
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testHistoricTaskInstanceQueryTaskNameCaseInsensitive() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);

    // when
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertThat(tasks).hasSize(1);
    taskService.complete(tasks.get(0).getId());

    // then
    List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().taskName("my task").list();
    assertThat(historicTasks).hasSize(1);

    // CAM-12186: check that query is case-insensitive
    List<HistoricTaskInstance> historicTasksUcFirst = historyService.createHistoricTaskInstanceQuery().taskName("My task").list();
    assertThat(historicTasksUcFirst).hasSize(1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testHistoricTaskInstanceQueryTaskNameLikeCaseInsensitive() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);

    // when
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertThat(tasks).hasSize(1);
    taskService.complete(tasks.get(0).getId());

    // then
    List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().taskNameLike("my task").list();
    assertThat(historicTasks).hasSize(1);

    // CAM-12186: check that query is case-insensitive
    List<HistoricTaskInstance> historicTasksUcFirst = historyService.createHistoricTaskInstanceQuery().taskNameLike("My task").list();
    assertThat(historicTasksUcFirst).hasSize(1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testHistoricTaskInstanceQueryTaskDescriptionCaseInsensitive() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);

    // when
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertThat(tasks).hasSize(1);
    taskService.complete(tasks.get(0).getId());

    // then
    List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().taskDescription("my description").list();
    assertThat(historicTasks).hasSize(1);

    // CAM-12186: check that query is case-insensitive
    List<HistoricTaskInstance> historicTasksUcFirst = historyService.createHistoricTaskInstanceQuery().taskDescription("My description").list();
    assertThat(historicTasksUcFirst).hasSize(1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testHistoricTaskInstanceQueryTaskDescriptionLikeCaseInsensitive() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);

    // when
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertThat(tasks).hasSize(1);
    taskService.complete(tasks.get(0).getId());

    // then
    List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("my description").list();
    assertThat(historicTasks).hasSize(1);

    // CAM-12186: check that query is case-insensitive
    List<HistoricTaskInstance> historicTasksUcFirst = historyService.createHistoricTaskInstanceQuery().taskDescriptionLike("My description").list();
    assertThat(historicTasksUcFirst).hasSize(1);
  }

  @SuppressWarnings("deprecation") // deprecated method is tested here
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testHistoricProcessInstanceUserIdAndActivityId() {
    identityService.setAuthenticatedUserId("johndoe");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertThat(historicProcessInstance.getStartUserId()).isEqualTo("johndoe");
    assertThat(historicProcessInstance.getStartActivityId()).isEqualTo("theStart");

    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertThat(tasks).hasSize(1);
    taskService.complete(tasks.get(0).getId());

    historicProcessInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertThat(historicProcessInstance.getEndActivityId()).isEqualTo("theEnd");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/history/orderProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/checkCreditProcess.bpmn20.xml"})
  @Test
  void testOrderProcessWithCallActivity() {
    // After the process has started, the 'verify credit history' task should be
    // active
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("orderProcess");
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task verifyCreditTask = taskQuery.singleResult();

    // Completing the task with approval, will end the subprocess and continue
    // the original process
    taskService.complete(verifyCreditTask.getId(), CollectionUtil.singletonMap("creditApproved", true));
    Task prepareAndShipTask = taskQuery.singleResult();
    assertThat(prepareAndShipTask.getName()).isEqualTo("Prepare and Ship");

    // verify
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
    assertThat(historicProcessInstance).isNotNull();
    assertThat(historicProcessInstance.getProcessDefinitionId()).contains("checkCreditProcess");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/orderProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/history/checkCreditProcess.bpmn20.xml"})
  @Test
  void testHistoricProcessInstanceQueryByProcessDefinitionKey() {

    String processDefinitionKey = ONE_TASK_PROCESS;
    runtimeService.startProcessInstanceByKey(processDefinitionKey);
    runtimeService.startProcessInstanceByKey("orderProcess");
    HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().processDefinitionKey(processDefinitionKey)
        .singleResult();
    assertThat(historicProcessInstance).isNotNull();
    assertThat(historicProcessInstance.getProcessDefinitionId()).startsWith(processDefinitionKey);
    assertThat(historicProcessInstance.getStartActivityId()).isEqualTo("theStart");

    // now complete the task to end the process instance
    Task task = taskService.createTaskQuery().processDefinitionKey("checkCreditProcess").singleResult();
    Map<String, Object> map = new HashMap<>();
    map.put("creditApproved", true);
    taskService.complete(task.getId(), map);

    // and make sure the super process instance is set correctly on the
    // HistoricProcessInstance
    HistoricProcessInstance historicProcessInstanceSub = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("checkCreditProcess")
        .singleResult();
    HistoricProcessInstance historicProcessInstanceSuper = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("orderProcess")
        .singleResult();
    assertThat(historicProcessInstanceSub.getSuperProcessInstanceId()).isEqualTo(historicProcessInstanceSuper.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/runtime/otherOneTaskProcess.bpmn20.xml"})
  @Test
  void testHistoricProcessInstanceQueryByProcessInstanceIds() {
    HashSet<String> processInstanceIds = new HashSet<>();
    for (int i = 0; i < 4; i++) {
      processInstanceIds.add(runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, i + "").getId());
    }
    processInstanceIds.add(runtimeService.startProcessInstanceByKey("otherOneTaskProcess", "1").getId());

    // start an instance that will not be part of the query
    runtimeService.startProcessInstanceByKey("otherOneTaskProcess", "2");

    HistoricProcessInstanceQuery processInstanceQuery = historyService.createHistoricProcessInstanceQuery().processInstanceIds(processInstanceIds);
    assertThat(processInstanceQuery.count()).isEqualTo(5);

    List<HistoricProcessInstance> processInstances = processInstanceQuery.list();
    assertThat(processInstances)
            .isNotNull()
            .hasSize(5);

    for (HistoricProcessInstance historicProcessInstance : processInstances) {
      assertThat(processInstanceIds).contains(historicProcessInstance.getId());
    }

    // making a query that has contradicting conditions should succeed
    assertThat(processInstanceQuery.processInstanceId("dummy").count()).isZero();
  }

  @Test
  void testHistoricProcessInstanceQueryByProcessInstanceIdsEmpty() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();
    var processInstanceIds = new HashSet<String>();

    // when/then
    assertThatThrownBy(() -> historicProcessInstanceQuery.processInstanceIds(processInstanceIds))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Set of process instance ids is empty");
  }

  @Test
  void testHistoricProcessInstanceQueryByProcessInstanceIdsNull() {
    // given
    var historicProcessInstanceQuery = historyService.createHistoricProcessInstanceQuery();

    // when/then
    assertThatThrownBy(() -> historicProcessInstanceQuery.processInstanceIds(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Set of process instance ids is null");
  }

  @Test
  void testQueryByRootProcessInstances() {
    // given
    String superProcess = "calling";
    String subProcess = "called";
    BpmnModelInstance callingInstance = ProcessModels.newModel(superProcess)
      .startEvent()
      .callActivity()
      .calledElement(subProcess)
      .endEvent()
      .done();

    BpmnModelInstance calledInstance = ProcessModels.newModel(subProcess)
      .startEvent()
      .userTask()
      .endEvent()
      .done();

   testRule.deploy(callingInstance, calledInstance);
    String processInstanceId1 = runtimeService.startProcessInstanceByKey(superProcess).getProcessInstanceId();

    // when
    List<HistoricProcessInstance> list = historyService
        .createHistoricProcessInstanceQuery()
        .rootProcessInstances()
        .list();

    // then
    assertThat(list).hasSize(1);
    assertThat(list.get(0).getId()).isEqualTo(processInstanceId1);
  }

  @Test
  void testQueryByRootProcessInstancesAndSuperProcess() {
    // given
    var historicProcessInstanceQuery1 = historyService.createHistoricProcessInstanceQuery()
      .rootProcessInstances();

    // when/then
    assertThatThrownBy(() -> historicProcessInstanceQuery1.superProcessInstanceId("processInstanceId"))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Invalid query usage: cannot set both rootProcessInstances and superProcessInstanceId");

    // given
    var historicProcessInstanceQuery2 = historyService.createHistoricProcessInstanceQuery()
      .superProcessInstanceId("processInstanceId");

    // when/then
    assertThatThrownBy(() -> historicProcessInstanceQuery2.rootProcessInstances())
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Invalid query usage: cannot set both rootProcessInstances and superProcessInstanceId");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/concurrentExecution.bpmn20.xml"})
  @Test
  void testHistoricVariableInstancesOnParallelExecution() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("rootValue", "test");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("concurrent", vars);

    List<Task> tasks = taskService.createTaskQuery().processInstanceId(pi.getId()).list();
    for (Task task : tasks) {
      Map<String, Object> variables = new HashMap<>();
      // set token local variable
      LOG.debug("setting variables on task {}, execution {}", task.getId(), task.getExecutionId());
      runtimeService.setVariableLocal(task.getExecutionId(), "parallelValue1", task.getName());
      runtimeService.setVariableLocal(task.getExecutionId(), "parallelValue2", "test");
      taskService.complete(task.getId(), variables);
    }
    taskService.complete(taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult().getId());

    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("rootValue", "test").count()).isOne();

    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("parallelValue1", "Receive Payment").count()).isOne();
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("parallelValue1", "Ship Order").count()).isOne();
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("parallelValue2", "test").count()).isOne();
  }

  /**
   * basically copied from {@link ProcessInstanceQueryTest}
   */
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryStringVariable() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    String processInstance1 = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, vars).getId();
    taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance1).singleResult().getId());

    vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("stringVar2", "ghijkl");
    String processInstance2 = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, vars).getId();
    taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance2).singleResult().getId());

    vars = new HashMap<>();
    vars.put("stringVar", "azerty");
    String processInstance3 = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, vars).getId();
    taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance3).singleResult().getId());

    // Test EQUAL on single string variable, should result in 2 matches
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().variableValueEquals("stringVar", "abcdef");
    List<HistoricProcessInstance> processInstances = query.list();
    assertThat(processInstances).hasSize(2);
    assertThat(processInstances).extracting("id").containsExactlyInAnyOrder(processInstance1, processInstance2);

    // Test EQUAL on two string variables, should result in single match
    query = historyService.createHistoricProcessInstanceQuery().variableValueEquals("stringVar", "abcdef").variableValueEquals("stringVar2", "ghijkl");
    HistoricProcessInstance resultInstance = query.singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance2);

    // Test NOT_EQUAL, should return only 1 resultInstance
    resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("stringVar", "abcdef").singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3);

    // Test GREATER_THAN, should return only matching 'azerty'
    resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueGreaterThan("stringVar", "abcdef").singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3);

    resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueGreaterThan("stringVar", "z").singleResult();
    assertThat(resultInstance).isNull();

    // Test GREATER_THAN_OR_EQUAL, should return 3 results
    processInstances = historyService.createHistoricProcessInstanceQuery().variableValueGreaterThanOrEqual("stringVar", "abcdef").list();
    assertThat(processInstances).extracting("id").containsExactlyInAnyOrder(processInstance1, processInstance2, processInstance3);
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThanOrEqual("stringVar", "z").count()).isZero();

    // Test LESS_THAN, should return 2 results
    processInstances = historyService.createHistoricProcessInstanceQuery().variableValueLessThan("stringVar", "abcdeg").list();
    assertThat(processInstances).extracting("id").containsExactlyInAnyOrder(processInstance1, processInstance2);

    processInstances = historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "z").list();
    assertThat(processInstances).extracting("id").containsExactlyInAnyOrder(processInstance1, processInstance2, processInstance3);

    // Test LESS_THAN_OR_EQUAL
    processInstances = historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "abcdef").list();
    assertThat(processInstances).extracting("id").containsExactlyInAnyOrder(processInstance1, processInstance2);

    processInstances = historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "z").list();
    assertThat(processInstances).extracting("id").containsExactlyInAnyOrder(processInstance1, processInstance2, processInstance3);

    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("stringVar", "aa").count()).isZero();

    // Test LIKE
    resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueLike("stringVar", "azert%").singleResult();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3);

    resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueLike("stringVar", "%y").singleResult();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3);

    resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueLike("stringVar", "%zer%").singleResult();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3);

    processInstances = historyService.createHistoricProcessInstanceQuery().variableValueLike("stringVar", "a%").list();
    assertThat(processInstances).extracting("id").containsExactlyInAnyOrder(processInstance1, processInstance2, processInstance3);
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLike("stringVar", "%x%").count()).isZero();
  }

  /**
   * Only do one second type, as the logic is same as in {@link ProcessInstanceQueryTest} and I do not want to duplicate
   * all test case logic here.
   * Basically copied from {@link ProcessInstanceQueryTest}
   */
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryDateVariable() throws Exception {
    Map<String, Object> vars = new HashMap<>();
    Date date1 = Calendar.getInstance().getTime();
    vars.put("dateVar", date1);

    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, vars);
    taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult().getId());

    Date date2 = Calendar.getInstance().getTime();
    vars = new HashMap<>();
    vars.put("dateVar", date1);
    vars.put("dateVar2", date2);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, vars);
    taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult().getId());

    Calendar nextYear = Calendar.getInstance();
    nextYear.add(Calendar.YEAR, 1);
    vars = new HashMap<>();
    vars.put("dateVar", nextYear.getTime());
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, vars);
    taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance3.getId()).singleResult().getId());

    Calendar nextMonth = Calendar.getInstance();
    nextMonth.add(Calendar.MONTH, 1);

    Calendar twoYearsLater = Calendar.getInstance();
    twoYearsLater.add(Calendar.YEAR, 2);

    Calendar oneYearAgo = Calendar.getInstance();
    oneYearAgo.add(Calendar.YEAR, -1);

    // Query on single short variable, should result in 2 matches
    HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery().variableValueEquals("dateVar", date1);
    List<HistoricProcessInstance> processInstances = query.list();
    assertThat(processInstances)
            .isNotNull()
            .hasSize(2);

    // Query on two short variables, should result in single value
    query = historyService.createHistoricProcessInstanceQuery().variableValueEquals("dateVar", date1).variableValueEquals("dateVar2", date2);
    HistoricProcessInstance resultInstance = query.singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance2.getId());

    // Query with unexisting variable value
    Date unexistingDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/01/1989 12:00:00");
    resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueEquals("dateVar", unexistingDate).singleResult();
    assertThat(resultInstance).isNull();

    // Test NOT_EQUALS
    resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("dateVar", date1).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    // Test GREATER_THAN
    resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueGreaterThan("dateVar", nextMonth.getTime()).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThan("dateVar", nextYear.getTime()).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThan("dateVar", oneYearAgo.getTime()).count()).isEqualTo(3);

    // Test GREATER_THAN_OR_EQUAL
    resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", nextMonth.getTime()).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    resultInstance = historyService.createHistoricProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", nextYear.getTime()).singleResult();
    assertThat(resultInstance).isNotNull();
    assertThat(resultInstance.getId()).isEqualTo(processInstance3.getId());

    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueGreaterThanOrEqual("dateVar", oneYearAgo.getTime()).count()).isEqualTo(3);

    // Test LESS_THAN
    processInstances = historyService.createHistoricProcessInstanceQuery().variableValueLessThan("dateVar", nextYear.getTime()).list();
    assertThat(processInstances).hasSize(2);

    List<String> expecedIds = List.of(processInstance1.getId(), processInstance2.getId());
    List<String> ids = new ArrayList<>(List.of(processInstances.get(0).getId(), processInstances.get(1).getId()));
    ids.removeAll(expecedIds);
    assertThat(ids).isEmpty();

    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThan("dateVar", date1).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThan("dateVar", twoYearsLater.getTime()).count()).isEqualTo(3);

    // Test LESS_THAN_OR_EQUAL
    processInstances = historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("dateVar", nextYear.getTime()).list();
    assertThat(processInstances).hasSize(3);

    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueLessThanOrEqual("dateVar", oneYearAgo.getTime()).count()).isZero();

    historyService.deleteHistoricProcessInstance(processInstance1.getId());
    historyService.deleteHistoricProcessInstance(processInstance2.getId());
    historyService.deleteHistoricProcessInstance(processInstance3.getId());
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testQueryMultipleVariableValuesEquals() {
    // given
    String var1 = "var1";
    String var2 = "var2";

    String val1 = "val1";
    String val2 = "val2";
    String val3 = "val3";

    VariableMap variables = Variables.createVariables().putValue(var1, val1).putValue(var2, val1);
    String processInstance1 = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, variables).getId();

    variables = Variables.createVariables().putValue(var1, val2).putValue(var2, val2);
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, variables).getId();

    variables = Variables.createVariables().putValue(var1, val3).putValue(var2, val3);
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, variables).getId();

    // when
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
        .variableValueEquals(var1, val1)
        .variableValueEquals(var2, val1)
        .list();

    assertThat(instances).extracting("id").containsExactly(processInstance1);
  }

  @Test
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  void testQueryMultipleVariableValuesEqualsAndNotEquals() {
    // given
    String var1 = "var1";
    String var2 = "var2";

    String val1 = "val1";
    String val2 = "val2";

    VariableMap variables = Variables.createVariables().putValue(var1, val1).putValue(var2, val1);
    String processInstance1 = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, variables).getId();

    variables = Variables.createVariables().putValue(var1, val1).putValue(var2, val2);
    String processInstance2 = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, variables).getId();

    variables = Variables.createVariables().putValue(var1, val2).putValue(var2, val2);
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, variables).getId();

    // when
    List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
        .variableValueEquals(var1, val1)
        .variableValueNotEquals(var2, "yet another value")
        .list();

    // then
    assertThat(instances).extracting("id").containsExactlyInAnyOrder(processInstance1, processInstance2);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testNativeHistoricProcessInstanceTest() {
    // just test that the query will be constructed and executed, details are tested in the TaskQueryTest
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    assertThat(historyService.createNativeHistoricProcessInstanceQuery().sql("SELECT count(*) FROM " + managementService.getTableName(HistoricProcessInstance.class)).count()).isOne();
    assertThat(historyService.createNativeHistoricProcessInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricProcessInstance.class)).list()).hasSize(1);
    assertThat(historyService.createNativeHistoricProcessInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricProcessInstance.class)).listPage(0, 1)).hasSize(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testNativeHistoricTaskInstanceTest() {
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    assertThat(historyService.createNativeHistoricTaskInstanceQuery().sql("SELECT count(*) FROM " + managementService.getTableName(HistoricProcessInstance.class)).count()).isOne();
    assertThat(historyService.createNativeHistoricTaskInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricProcessInstance.class)).list()).hasSize(1);
    assertThat(historyService.createNativeHistoricTaskInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricProcessInstance.class)).listPage(0, 1)).hasSize(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testNativeHistoricActivityInstanceTest() {
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    assertThat(historyService.createNativeHistoricActivityInstanceQuery().sql("SELECT count(*) FROM " + managementService.getTableName(HistoricProcessInstance.class)).count()).isOne();
    assertThat(historyService.createNativeHistoricActivityInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricProcessInstance.class)).list()).hasSize(1);
    assertThat(historyService.createNativeHistoricActivityInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricProcessInstance.class)).listPage(0, 1)).hasSize(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testNativeHistoricVariableInstanceTest() {
    Date date = Calendar.getInstance().getTime();
    Map<String, Object> vars = new HashMap<>();
    vars.put("stringVar", "abcdef");
    vars.put("dateVar", date);
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS, vars);

    assertThat(historyService.createNativeHistoricVariableInstanceQuery().sql("SELECT count(*) FROM " + managementService.getTableName(HistoricVariableInstance.class)).count()).isEqualTo(2);
    assertThat(historyService.createNativeHistoricVariableInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricVariableInstance.class)).listPage(0, 1)).hasSize(1);

    List<HistoricVariableInstance> variables = historyService.createNativeHistoricVariableInstanceQuery().sql("SELECT * FROM " + managementService.getTableName(HistoricVariableInstance.class)).list();
    assertThat(variables).hasSize(2);
    for (HistoricVariableInstance variable : variables) {
      assertThat(vars).containsKey(variable.getName());
      assertThat(variable.getValue()).isEqualTo(vars.get(variable.getName()));
      vars.remove(variable.getName());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueEqualsNumber() {
    // long
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS,
        Collections.singletonMap("var", 123L));

    // non-matching long
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS,
        Collections.singletonMap("var", 12345L));

    // short
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS,
        Collections.singletonMap("var", (short) 123));

    // double
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS,
        Collections.singletonMap("var", 123.0d));

    // integer
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS,
        Collections.singletonMap("var", 123));

    // untyped null (should not match)
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS,
        Collections.singletonMap("var", null));

    // typed null (should not match)
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS,
        Collections.singletonMap("var", Variables.longValue(null)));

    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS,
        Collections.singletonMap("var", "123"));

    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("var", Variables.numberValue(123L)).count()).isEqualTo(4);
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("var", Variables.numberValue(123.0d)).count()).isEqualTo(4);
    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);

    assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("var", Variables.numberValue(null)).count()).isOne();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstance() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isOne();

    runtimeService.deleteProcessInstance(processInstance.getId(), null);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isOne();

    historyService.deleteHistoricProcessInstance(processInstance.getId());
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteRunningProcessInstance() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isOne();
    var processInstanceId = processInstance.getId();

    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricProcessInstance(processInstanceId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Process instance is still running, cannot delete historic process instance");
  }

  @Test
  void testDeleteProcessInstanceWithFake() {
    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricProcessInstance("aFake"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No historic process instance found with id");
  }

  @Test
  void testDeleteProcessInstanceIfExistsWithFake() {
    assertThatCode(() -> historyService.deleteHistoricProcessInstanceIfExists("aFake")).doesNotThrowAnyException();
  }

  @Test
  void testDeleteProcessInstanceNullId() {
    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricProcessInstance(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("processInstanceId is null");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstances() {
    //given
    List<String> ids = prepareHistoricProcesses();

    //when
    historyService.deleteHistoricProcessInstances(ids);

    //then
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesWithFake() {
    //given
    List<String> ids = prepareHistoricProcesses();
    ids.add("aFake");

    // when
    assertThatThrownBy(() -> historyService.deleteHistoricProcessInstances(ids))
      // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No historic process instance found with id: [aFake]");

    // then expect no instance is deleted
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isEqualTo(2);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesIfExistsWithFake() {
    //given
    List<String> ids = prepareHistoricProcesses();
    ids.add("aFake");

    //when
    historyService.deleteHistoricProcessInstancesIfExists(ids);

    //then expect no exception and all instances are deleted
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteProcessInstancesWithNull() {
    assertThatThrownBy(() -> historyService.deleteHistoricProcessInstances(null)).isInstanceOf(ProcessEngineException.class);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteHistoricVariableAndDetails() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    String executionId = processInstance.getId();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isOne();
    runtimeService.setVariable(executionId, "myVariable", "testValue1");
    runtimeService.setVariable(executionId, "myVariable", "testValue2");
    runtimeService.setVariable(executionId, "myVariable", "testValue3");
    runtimeService.setVariable(executionId, "mySecondVariable", 5L);

    runtimeService.deleteProcessInstance(executionId, null);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isOne();

    HistoricVariableInstanceQuery histVariableQuery = historyService.createHistoricVariableInstanceQuery()
        .processInstanceId(executionId)
        .variableName("myVariable");
    HistoricVariableInstanceQuery secondHistVariableQuery = historyService.createHistoricVariableInstanceQuery()
        .processInstanceId(executionId)
        .variableName("mySecondVariable");
    assertThat(histVariableQuery.count()).isOne();
    assertThat(secondHistVariableQuery.count()).isOne();

    String variableInstanceId = histVariableQuery.singleResult().getId();
    String secondVariableInstanceId = secondHistVariableQuery.singleResult().getId();
    HistoricDetailQuery detailsQuery = historyService.createHistoricDetailQuery()
        .processInstanceId(executionId)
        .variableInstanceId(variableInstanceId);
    HistoricDetailQuery secondDetailsQuery = historyService.createHistoricDetailQuery()
        .processInstanceId(executionId)
        .variableInstanceId(secondVariableInstanceId);
    assertThat(detailsQuery.count()).isEqualTo(3);
    assertThat(secondDetailsQuery.count()).isOne();

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);

    // then
    assertThat(histVariableQuery.count()).isZero();
    assertThat(secondHistVariableQuery.count()).isOne();
    assertThat(detailsQuery.count()).isZero();
    assertThat(secondDetailsQuery.count()).isOne();
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteHistoricVariableAndDetailsOnRunningInstance() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    String executionId = processInstance.getId();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isOne();
    runtimeService.setVariable(executionId, "myVariable", "testValue1");
    runtimeService.setVariable(executionId, "myVariable", "testValue2");
    runtimeService.setVariable(executionId, "myVariable", "testValue3");

    VariableInstanceQuery variableQuery = runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(executionId)
        .variableName("myVariable");
    assertThat(variableQuery.count()).isOne();
    assertThat(variableQuery.singleResult().getValue()).isEqualTo("testValue3");

    HistoricVariableInstanceQuery histVariableQuery = historyService.createHistoricVariableInstanceQuery()
        .processInstanceId(executionId)
        .variableName("myVariable");
    assertThat(histVariableQuery.count()).isOne();

    String variableInstanceId = histVariableQuery.singleResult().getId();
    HistoricDetailQuery detailsQuery = historyService.createHistoricDetailQuery()
        .processInstanceId(executionId)
        .variableInstanceId(variableInstanceId);
    assertThat(detailsQuery.count()).isEqualTo(3);

    // when
    historyService.deleteHistoricVariableInstance(variableInstanceId);

    // then
    assertThat(histVariableQuery.count()).isZero();
    assertThat(detailsQuery.count()).isZero();
    assertThat(variableQuery.count()).isOne();
    assertThat(variableQuery.singleResult().getValue()).isEqualTo("testValue3");
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteHistoricVariableAndDetailsOnRunningInstanceAndSetAgain() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    String executionId = processInstance.getId();
    runtimeService.setVariable(executionId, "myVariable", "testValue1");
    runtimeService.setVariable(executionId, "myVariable", "testValue2");
    runtimeService.setVariable(executionId, "myVariable", "testValue3");

    VariableInstanceQuery variableQuery = runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(executionId)
        .variableName("myVariable");
    HistoricVariableInstanceQuery histVariableQuery = historyService.createHistoricVariableInstanceQuery()
        .processInstanceId(executionId)
        .variableName("myVariable");

    String variableInstanceId = histVariableQuery.singleResult().getId();

    HistoricDetailQuery detailsQuery = historyService.createHistoricDetailQuery()
        .processInstanceId(executionId)
        .variableInstanceId(variableInstanceId);


    historyService.deleteHistoricVariableInstance(variableInstanceId);

    assertThat(histVariableQuery.count()).isZero();
    assertThat(detailsQuery.count()).isZero();
    assertThat(variableQuery.count()).isOne();
    assertThat(variableQuery.singleResult().getValue()).isEqualTo("testValue3");

    // when
    runtimeService.setVariable(executionId, "myVariable", "testValue4");

    // then
    assertThat(histVariableQuery.count()).isOne();
    assertThat(detailsQuery.count()).isOne();
    assertThat(variableQuery.count()).isOne();
    assertThat(variableQuery.singleResult().getValue()).isEqualTo("testValue4");
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testDeleteHistoricVariableAndDetailsFromCase() {
    // given
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();
    caseService.setVariable(caseInstanceId, "myVariable", 1);
    caseService.setVariable(caseInstanceId, "myVariable", 2);
    caseService.setVariable(caseInstanceId, "myVariable", 3);

    HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();
    HistoricDetailQuery detailsQuery = historyService.createHistoricDetailQuery()
        .caseInstanceId(caseInstanceId)
        .variableInstanceId(variableInstance.getId());
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isOne();
    assertThat(detailsQuery.count()).isEqualTo(3);

    // when
    historyService.deleteHistoricVariableInstance(variableInstance.getId());

    // then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isZero();
    assertThat(detailsQuery.count()).isZero();
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testDeleteHistoricVariableAndDetailsFromCaseAndSetAgain() {
    // given
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();
    caseService.setVariable(caseInstanceId, "myVariable", 1);
    caseService.setVariable(caseInstanceId, "myVariable", 2);
    caseService.setVariable(caseInstanceId, "myVariable", 3);

    HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();
    HistoricDetailQuery detailsQuery = historyService.createHistoricDetailQuery()
        .caseInstanceId(caseInstanceId)
        .variableInstanceId(variableInstance.getId());
    historyService.deleteHistoricVariableInstance(variableInstance.getId());
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isZero();
    assertThat(detailsQuery.count()).isZero();

    // when
    caseService.setVariable(caseInstanceId, "myVariable", 4);

    // then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isOne();
    assertThat(detailsQuery.count()).isOne();
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testDeleteHistoricVariableAndDetailsFromStandaloneTask() {
    // given
    Task task = taskService.newTask();
    taskService.saveTask(task);
    taskService.setVariable(task.getId(), "testVariable", "testValue");
    taskService.setVariable(task.getId(), "testVariable", "testValue2");
    HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();
    HistoricDetailQuery detailsQuery = historyService.createHistoricDetailQuery()
        .taskId(task.getId())
        .variableInstanceId(variableInstance.getId());
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isOne();
    assertThat(detailsQuery.count()).isEqualTo(2);

    // when
    historyService.deleteHistoricVariableInstance(variableInstance.getId());

    // then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isZero();
    assertThat(detailsQuery.count()).isZero();

    taskService.deleteTask(task.getId(), true);
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testDeleteHistoricVariableAndDetailsFromStandaloneTaskAndSetAgain() {
    // given
    Task task = taskService.newTask();
    taskService.saveTask(task);
    taskService.setVariable(task.getId(), "testVariable", "testValue");
    taskService.setVariable(task.getId(), "testVariable", "testValue2");

    HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();
    HistoricDetailQuery detailsQuery = historyService.createHistoricDetailQuery()
        .taskId(task.getId())
        .variableInstanceId(variableInstance.getId());

    historyService.deleteHistoricVariableInstance(variableInstance.getId());
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isZero();
    assertThat(detailsQuery.count()).isZero();

    // when
    taskService.setVariable(task.getId(), "testVariable", "testValue3");

    // then
    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isOne();
    assertThat(detailsQuery.count()).isOne();

    taskService.deleteTask(task.getId(), true);
  }

  @Test
  void testDeleteUnknownHistoricVariable() {
    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricVariableInstance("fakeID"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No historic variable instance found with id: fakeID");
  }

  @Test
  void testDeleteHistoricVariableWithNull() {
    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricVariableInstance(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("variableInstanceId is null");
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteAllHistoricVariablesAndDetails() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    String executionId = processInstance.getId();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isOne();
    runtimeService.setVariable(executionId, "myVariable", "testValue1");
    runtimeService.setVariable(executionId, "myVariable", "testValue2");
    runtimeService.setVariable(executionId, "myVariable", "testValue3");
    runtimeService.setVariable(executionId, "mySecondVariable", 5L);
    runtimeService.setVariable(executionId, "mySecondVariable", 7L);

    runtimeService.deleteProcessInstance(executionId, null);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isOne();

    HistoricVariableInstanceQuery histVariableQuery = historyService.createHistoricVariableInstanceQuery().processInstanceId(executionId)
        .variableName("myVariable");
    HistoricVariableInstanceQuery secondHistVariableQuery = historyService.createHistoricVariableInstanceQuery().processInstanceId(executionId)
        .variableName("mySecondVariable");
    assertThat(histVariableQuery.count()).isOne();
    assertThat(secondHistVariableQuery.count()).isOne();

    String variableInstanceId = histVariableQuery.singleResult().getId();
    String secondVariableInstanceId = secondHistVariableQuery.singleResult().getId();
    HistoricDetailQuery detailsQuery = historyService.createHistoricDetailQuery().processInstanceId(executionId).variableInstanceId(variableInstanceId);
    HistoricDetailQuery secondDetailsQuery = historyService.createHistoricDetailQuery().processInstanceId(executionId)
        .variableInstanceId(secondVariableInstanceId);
    assertThat(detailsQuery.count()).isEqualTo(3);
    assertThat(secondDetailsQuery.count()).isEqualTo(2);

    // when
    historyService.deleteHistoricVariableInstancesByProcessInstanceId(executionId);

    // then
    assertThat(histVariableQuery.count()).isZero();
    assertThat(secondHistVariableQuery.count()).isZero();
    assertThat(detailsQuery.count()).isZero();
    assertThat(secondDetailsQuery.count()).isZero();
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteAllHistoricVariablesAndDetailsOnRunningInstance() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    String executionId = processInstance.getId();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isOne();
    runtimeService.setVariable(executionId, "myVariable", "testValue1");
    runtimeService.setVariable(executionId, "myVariable", "testValue2");
    runtimeService.setVariable(executionId, "myVariable", "testValue3");
    runtimeService.setVariable(executionId, "mySecondVariable", "testValue1");
    runtimeService.setVariable(executionId, "mySecondVariable", "testValue2");

    VariableInstanceQuery variableQuery = runtimeService.createVariableInstanceQuery().processInstanceIdIn(executionId).variableName("myVariable");
    VariableInstanceQuery secondVariableQuery = runtimeService.createVariableInstanceQuery().processInstanceIdIn(executionId).variableName("mySecondVariable");
    assertThat(variableQuery.count()).isOne();
    assertThat(secondVariableQuery.count()).isOne();
    assertThat(variableQuery.singleResult().getValue()).isEqualTo("testValue3");
    assertThat(secondVariableQuery.singleResult().getValue()).isEqualTo("testValue2");

    HistoricVariableInstanceQuery histVariableQuery = historyService.createHistoricVariableInstanceQuery().processInstanceId(executionId)
        .variableName("myVariable");
    HistoricVariableInstanceQuery secondHistVariableQuery = historyService.createHistoricVariableInstanceQuery().processInstanceId(executionId)
        .variableName("mySecondVariable");
    assertThat(histVariableQuery.count()).isOne();
    assertThat(secondHistVariableQuery.count()).isOne();

    String variableInstanceId = histVariableQuery.singleResult().getId();
    String secondVariableInstanceId = secondHistVariableQuery.singleResult().getId();
    HistoricDetailQuery detailsQuery = historyService.createHistoricDetailQuery().processInstanceId(executionId).variableInstanceId(variableInstanceId);
    HistoricDetailQuery secondDetailsQuery = historyService.createHistoricDetailQuery().processInstanceId(executionId).variableInstanceId(secondVariableInstanceId);
    assertThat(detailsQuery.count()).isEqualTo(3L);
    assertThat(secondDetailsQuery.count()).isEqualTo(2L);

    // when
    historyService.deleteHistoricVariableInstancesByProcessInstanceId(executionId);

    // then
    HistoricVariableInstanceQuery allHistVariableQuery = historyService.createHistoricVariableInstanceQuery().processInstanceId(executionId);
    HistoricDetailQuery allDetailsQuery = historyService.createHistoricDetailQuery().processInstanceId(executionId);
    assertThat(histVariableQuery.count()).isZero();
    assertThat(secondHistVariableQuery.count()).isZero();
    assertThat(allHistVariableQuery.count()).isZero();
    assertThat(detailsQuery.count()).isZero();
    assertThat(secondDetailsQuery.count()).isZero();
    assertThat(allDetailsQuery.count()).isZero();
    assertThat(variableQuery.count()).isOne();
    assertThat(variableQuery.singleResult().getValue()).isEqualTo("testValue3");
    assertThat(secondVariableQuery.singleResult().getValue()).isEqualTo("testValue2");
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteAllHistoricVariablesAndDetailsOnRunningInstanceAndSetAgain() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    String executionId = processInstance.getId();
    runtimeService.setVariable(executionId, "myVariable", "testValue1");
    runtimeService.setVariable(executionId, "myVariable", "testValue2");
    runtimeService.setVariable(executionId, "mySecondVariable", "testValue1");
    runtimeService.setVariable(executionId, "mySecondVariable", "testValue2");

    historyService.deleteHistoricVariableInstancesByProcessInstanceId(executionId);

    VariableInstanceQuery variableQuery = runtimeService.createVariableInstanceQuery().processInstanceIdIn(executionId).variableName("myVariable");
    VariableInstanceQuery secondVariableQuery = runtimeService.createVariableInstanceQuery().processInstanceIdIn(executionId).variableName("mySecondVariable");
    HistoricVariableInstanceQuery allHistVariableQuery = historyService.createHistoricVariableInstanceQuery().processInstanceId(executionId);
    HistoricDetailQuery allDetailsQuery = historyService.createHistoricDetailQuery().processInstanceId(executionId);
    assertThat(allHistVariableQuery.count()).isZero();
    assertThat(allDetailsQuery.count()).isZero();
    assertThat(variableQuery.count()).isOne();
    assertThat(secondVariableQuery.count()).isOne();
    assertThat(variableQuery.singleResult().getValue()).isEqualTo("testValue2");
    assertThat(secondVariableQuery.singleResult().getValue()).isEqualTo("testValue2");

    // when
    runtimeService.setVariable(executionId, "myVariable", "testValue3");
    runtimeService.setVariable(executionId, "mySecondVariable", "testValue3");

    // then
    HistoricVariableInstanceQuery histVariableQuery = historyService.createHistoricVariableInstanceQuery().processInstanceId(executionId).variableName("myVariable");
    HistoricVariableInstanceQuery secondHistVariableQuery = historyService.createHistoricVariableInstanceQuery().processInstanceId(executionId).variableName("mySecondVariable");
    HistoricDetailQuery detailsQuery = historyService.createHistoricDetailQuery().processInstanceId(executionId).variableInstanceId(histVariableQuery.singleResult().getId());
    HistoricDetailQuery secondDetailsQuery = historyService.createHistoricDetailQuery().processInstanceId(executionId).variableInstanceId(secondHistVariableQuery.singleResult().getId());
    assertThat(histVariableQuery.count()).isOne();
    assertThat(secondHistVariableQuery.count()).isOne();
    assertThat(allHistVariableQuery.count()).isEqualTo(2L);
    assertThat(detailsQuery.count()).isOne();
    assertThat(secondDetailsQuery.count()).isOne();
    assertThat(allDetailsQuery.count()).isEqualTo(2L);
    assertThat(variableQuery.count()).isOne();
    assertThat(secondVariableQuery.count()).isOne();
    assertThat(variableQuery.singleResult().getValue()).isEqualTo("testValue3");
    assertThat(secondVariableQuery.singleResult().getValue()).isEqualTo("testValue3");
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  void testDeleteAllHistoricVariablesOnEmpty() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    String executionId = processInstance.getId();
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isOne();

    runtimeService.deleteProcessInstance(executionId, null);
    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isZero();
    assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey(ONE_TASK_PROCESS).count()).isOne();

    HistoricVariableInstanceQuery histVariableQuery = historyService.createHistoricVariableInstanceQuery().processInstanceId(executionId);
    assertThat(histVariableQuery.count()).isZero();

    HistoricDetailQuery detailsQuery = historyService.createHistoricDetailQuery().processInstanceId(executionId);
    assertThat(detailsQuery.count()).isZero();

    // when
    historyService.deleteHistoricVariableInstancesByProcessInstanceId(executionId);

    // then
    assertThat(histVariableQuery.count()).isZero();
    assertThat(detailsQuery.count()).isZero();
  }

  @Test
  void testDeleteAllHistoricVariablesOnUnkownProcessInstance() {
    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricVariableInstancesByProcessInstanceId("fakeID"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("No historic process instance found with id: fakeID");
  }

  @Test
  void testDeleteAllHistoricVariablesWithNull() {
    // when/then
    assertThatThrownBy(() -> historyService.deleteHistoricVariableInstancesByProcessInstanceId(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("processInstanceId is null");
  }

  protected List<String> prepareHistoricProcesses() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS);

    List<String> processInstanceIds = new ArrayList<>(List.of(processInstance.getId(), processInstance2.getId()));
    runtimeService.deleteProcessInstances(processInstanceIds, null, true, true);

    return processInstanceIds;
  }
}
