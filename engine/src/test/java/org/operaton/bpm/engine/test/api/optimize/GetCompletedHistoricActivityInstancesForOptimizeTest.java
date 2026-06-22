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
package org.operaton.bpm.engine.test.api.optimize;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.OptimizeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.event.HistoryEvent;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.delegate.ExecutionListener.EVENTNAME_START;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class GetCompletedHistoricActivityInstancesForOptimizeTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  private OptimizeService optimizeService;

  protected String userId = "test";

  protected static final String VARIABLE_NAME = "aVariableName";
  protected static final String VARIABLE_VALUE = "aVariableValue";

  IdentityService identityService;
  RuntimeService runtimeService;
  AuthorizationService authorizationService;
  TaskService taskService;


  @BeforeEach
  void init() {
    ProcessEngineConfigurationImpl config =
      engineRule.getProcessEngineConfiguration();
    optimizeService = config.getOptimizeService();

    createUser(userId);
  }

  @AfterEach
  void cleanUp() {
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Group group : identityService.createGroupQuery().list()) {
      identityService.deleteGroup(group.getId());
    }
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
    ClockUtil.reset();
  }

  @Test
  void getCompletedHistoricActivityInstances() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
        .name("start")
      .endEvent("endEvent")
        .name("end")
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");

    // when
    List<HistoricActivityInstance> completedHistoricActivityInstances =
      optimizeService.getCompletedHistoricActivityInstances(pastDate(), null, 10);

    // then
    assertThat(completedHistoricActivityInstances).hasSize(2);
    assertThatActivitiesHaveAllImportantInformation(completedHistoricActivityInstances);
  }

  @Test
  void fishedAfterParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask("userTask")
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(now);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    completeAllUserTasks();
    List<HistoricActivityInstance> completedHistoricActivityInstances =
      optimizeService.getCompletedHistoricActivityInstances(now, null, 10);

    // then
    Set<String> allowedActivityIds = new HashSet<>(List.of("userTask", "endEvent"));
    assertThat(completedHistoricActivityInstances).hasSize(2);
    assertTrue(allowedActivityIds.contains(completedHistoricActivityInstances.get(0).getActivityId()));
    assertTrue(allowedActivityIds.contains(completedHistoricActivityInstances.get(1).getActivityId()));
  }

  @Test
  void fishedAtParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask")
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(now);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    completeAllUserTasks();
    List<HistoricActivityInstance> completedHistoricActivityInstances =
      optimizeService.getCompletedHistoricActivityInstances(null, now, 10);

    // then
    assertThat(completedHistoricActivityInstances).hasSize(1);
    assertThat(completedHistoricActivityInstances.get(0).getActivityId()).isEqualTo("startEvent");
  }

  @Test
  void fishedAfterAndFinishedAtParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask")
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(now);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    completeAllUserTasks();
    List<HistoricActivityInstance> completedHistoricActivityInstances =
      optimizeService.getCompletedHistoricActivityInstances(now, now, 10);

    // then
    assertThat(completedHistoricActivityInstances).isEmpty();
  }

  @Test
  void maxResultsParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .serviceTask()
        .operatonExpression("${true}")
      .serviceTask()
        .operatonExpression("${true}")
      .serviceTask()
        .operatonExpression("${true}")
      .serviceTask()
        .operatonExpression("${true}")
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    List<HistoricActivityInstance> completedHistoricActivityInstances =
      optimizeService.getCompletedHistoricActivityInstances(pastDate(), null, 3);

    // then
    assertThat(completedHistoricActivityInstances).hasSize(3);
  }

  @Test
  void resultIsSortedByEndTime() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .serviceTask("ServiceTask1")
        .operatonExpression("${true}")
        .operatonExecutionListenerClass(EVENTNAME_START, ShiftTimeByOneMinuteListener.class.getName())
      .serviceTask("ServiceTask2")
        .operatonExpression("${true}")
        .operatonExecutionListenerClass(EVENTNAME_START, ShiftTimeByOneMinuteListener.class.getName())
      .serviceTask("ServiceTask3")
        .operatonExpression("${true}")
        .operatonExecutionListenerClass(EVENTNAME_START, ShiftTimeByOneMinuteListener.class.getName())
      .endEvent("endEvent")
        .operatonExecutionListenerClass(EVENTNAME_START, ShiftTimeByOneMinuteListener.class.getName())
      .done();
    testHelper.deploy(simpleDefinition);
    ClockUtil.setCurrentTime(new Date());
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    ClockUtil.reset();

    // when
    List<HistoricActivityInstance> completedHistoricActivityInstances =
      optimizeService.getCompletedHistoricActivityInstances(pastDate(), null, 4);

    // then
    assertThat(completedHistoricActivityInstances).hasSize(4);
    assertThat(completedHistoricActivityInstances.get(0).getActivityId()).isEqualTo("startEvent");
    assertThat(completedHistoricActivityInstances.get(1).getActivityId()).isEqualTo("ServiceTask1");
    assertThat(completedHistoricActivityInstances.get(2).getActivityId()).isEqualTo("ServiceTask2");
    assertThat(completedHistoricActivityInstances.get(3).getActivityId()).isEqualTo("ServiceTask3");
  }

  @Test
  void fetchOnlyCompletedActivities() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    List<HistoricActivityInstance> completedHistoricActivityInstances =
      optimizeService.getCompletedHistoricActivityInstances(pastDate(), null, 10);

    // then
    assertThat(completedHistoricActivityInstances).hasSize(1);
    assertThat(completedHistoricActivityInstances.get(0).getActivityId()).isEqualTo("startEvent");
  }


  private Date pastDate() {
    return new Date(2L);
  }

  private void completeAllUserTasks() {
    List<Task> list = taskService.createTaskQuery().list();
    for (Task task : list) {
      taskService.claim(task.getId(), userId);
      taskService.complete(task.getId());
    }
  }

  protected void createUser(String userId) {
    User user = identityService.newUser(userId);
    identityService.saveUser(user);
  }

  private void assertThatActivitiesHaveAllImportantInformation(List<HistoricActivityInstance> completedHistoricActivityInstances) {
    HistoricActivityInstance startEvent = null;
    HistoricActivityInstance endEvent = null;
    for (HistoricActivityInstance completedHistoricActivityInstance : completedHistoricActivityInstances) {
      if ("startEvent".equals(completedHistoricActivityInstance.getActivityId())) {
        startEvent = completedHistoricActivityInstance;
      } else if ("endEvent".equals(completedHistoricActivityInstance.getActivityId())) {
        endEvent = completedHistoricActivityInstance;
      }
    }
    assertThat(startEvent).isNotNull();
    assertThat(startEvent.getActivityName()).isEqualTo("start");
    assertThat(startEvent.getActivityType()).isEqualTo("startEvent");
    assertThat(startEvent.getStartTime()).isNotNull();
    assertThat(startEvent.getEndTime()).isNotNull();
    assertThat(startEvent.getProcessDefinitionKey()).isEqualTo("process");
    assertThat(startEvent.getProcessDefinitionId()).isNotNull();
    assertThat(((HistoryEvent) startEvent).getSequenceCounter()).isPositive();

    assertThat(endEvent).isNotNull();
    assertThat(endEvent.getActivityName()).isEqualTo("end");
    assertThat(endEvent.getActivityType()).isEqualTo("noneEndEvent");
    assertThat(endEvent.getStartTime()).isNotNull();
    assertThat(endEvent.getEndTime()).isNotNull();
    assertThat(endEvent.getProcessDefinitionKey()).isEqualTo("process");
    assertThat(endEvent.getProcessDefinitionId()).isNotNull();
    assertThat(((HistoryEvent) endEvent).getSequenceCounter()).isPositive();
  }

}
