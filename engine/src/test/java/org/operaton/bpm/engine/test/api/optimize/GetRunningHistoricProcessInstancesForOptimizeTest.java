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
import java.util.List;

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
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.OptimizeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;


@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class GetRunningHistoricProcessInstancesForOptimizeTest {
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
  void getRunningHistoricProcessInstances() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");

    // when
    List<HistoricProcessInstance> runningHistoricProcessInstances =
      optimizeService.getRunningHistoricProcessInstances(pastDate(), null, 10);

    // then
    assertThat(runningHistoricProcessInstances).hasSize(1);
    assertThatInstanceHasAllImportantInformation(runningHistoricProcessInstances.get(0));
  }

  @Test
  void startedAfterParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    runtimeService.startProcessInstanceByKey("process");

    // when
    List<HistoricProcessInstance> runningHistoricProcessInstances =
      optimizeService.getRunningHistoricProcessInstances(now, null, 10);

    // then
    assertThat(runningHistoricProcessInstances).hasSize(1);
  }

  @Test
  void startedAtParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    ProcessInstance processInstance =
      runtimeService.startProcessInstanceByKey("process");
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    runtimeService.startProcessInstanceByKey("process");

    // when
    List<HistoricProcessInstance> runningHistoricProcessInstances =
      optimizeService.getRunningHistoricProcessInstances(null, now, 10);

    // then
    assertThat(runningHistoricProcessInstances).hasSize(1);
    assertThat(runningHistoricProcessInstances.get(0).getId()).isEqualTo(processInstance.getId());
  }

  @Test
  void startedAfterAndStartedAtParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    runtimeService.startProcessInstanceByKey("process");
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    runtimeService.startProcessInstanceByKey("process");

    // when
    List<HistoricProcessInstance> runningHistoricProcessInstances =
      optimizeService.getRunningHistoricProcessInstances(now, now, 10);

    // then
    assertThat(runningHistoricProcessInstances).isEmpty();
  }

  @Test
  void maxResultsParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");
    runtimeService.startProcessInstanceByKey("process");

    // when
    List<HistoricProcessInstance> runningHistoricProcessInstances =
      optimizeService.getRunningHistoricProcessInstances(pastDate(), null, 3);

    // then
    assertThat(runningHistoricProcessInstances).hasSize(3);
  }

  @Test
  void resultIsSortedByStartTime() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    Date nowPlus1Second = new Date(now.getTime() + 1000L);
    ClockUtil.setCurrentTime(nowPlus1Second);
    ProcessInstance processInstance1 =
      runtimeService.startProcessInstanceByKey("process");
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    ProcessInstance processInstance2 =
      runtimeService.startProcessInstanceByKey("process");
    Date nowPlus4Seconds = new Date(nowPlus2Seconds.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    ProcessInstance processInstance3 =
      runtimeService.startProcessInstanceByKey("process");

    // when
    List<HistoricProcessInstance> runningHistoricProcessInstances =
      optimizeService.getRunningHistoricProcessInstances(new Date(now.getTime()), null, 10);

    // then
    assertThat(runningHistoricProcessInstances).hasSize(3);
    assertThat(runningHistoricProcessInstances.get(0).getId()).isEqualTo(processInstance1.getId());
    assertThat(runningHistoricProcessInstances.get(1).getId()).isEqualTo(processInstance2.getId());
    assertThat(runningHistoricProcessInstances.get(2).getId()).isEqualTo(processInstance3.getId());
  }

  @Test
  void fetchOnlyRunningProcessInstances() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");
    completeAllUserTasks();
    ProcessInstance runningProcessInstance =
      runtimeService.startProcessInstanceByKey("process");

    // when
    List<HistoricProcessInstance> runningHistoricProcessInstances =
      optimizeService.getRunningHistoricProcessInstances(pastDate(), null, 10);

    // then
    assertThat(runningHistoricProcessInstances).hasSize(1);
    assertThat(runningHistoricProcessInstances.get(0).getId()).isEqualTo(runningProcessInstance.getId());
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

  private void assertThatInstanceHasAllImportantInformation(HistoricProcessInstance historicProcessInstance) {
    assertThat(historicProcessInstance).isNotNull();
    assertThat(historicProcessInstance.getId()).isNotNull();
    assertThat(historicProcessInstance.getProcessDefinitionKey()).isEqualTo("process");
    assertThat(historicProcessInstance.getProcessDefinitionVersion()).isNotNull();
    assertThat(historicProcessInstance.getProcessDefinitionId()).isNotNull();
    assertThat(historicProcessInstance.getStartTime()).isNotNull();
    assertThat(historicProcessInstance.getEndTime()).isNull();
  }

}
