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
package org.operaton.bpm.engine.test.api.optimize;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.OptimizeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class GetCompletedHistoricTaskInstancesForOptimizeTest {

  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testHelper);

  private OptimizeService optimizeService;

  protected String userId = "test";

  private IdentityService identityService;
  private RuntimeService runtimeService;
  private AuthorizationService authorizationService;
  private TaskService taskService;


  @Before
  public void init() {
    ProcessEngineConfigurationImpl config =
      engineRule.getProcessEngineConfiguration();
    optimizeService = config.getOptimizeService();
    identityService = engineRule.getIdentityService();
    runtimeService = engineRule.getRuntimeService();
    authorizationService = engineRule.getAuthorizationService();
    taskService = engineRule.getTaskService();

    createUser(userId);
  }

  @After
  public void cleanUp() {
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
  public void getCompletedHistoricTaskInstances() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask")
        .name("task")
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");
    completeAllUserTasks();

    // when
    List<HistoricTaskInstance> completedHistoricTaskInstances =
      optimizeService.getCompletedHistoricTaskInstances(null, null, 10);

    // then
    assertThat(completedHistoricTaskInstances).hasSize(1);
    assertThatTasksHaveAllImportantInformation(completedHistoricTaskInstances.get(0));
  }

  @Test
  public void fishedAfterParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask("userTask1")
      .userTask("userTask2")
      .userTask("userTask3")
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    completeAllUserTasks();

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    completeAllUserTasks();

    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    completeAllUserTasks();

    // when
    List<HistoricTaskInstance> completedHistoricTaskInstances =
      optimizeService.getCompletedHistoricTaskInstances(now, null, 10);

    // then
    Set<String> allowedTaskIds = new HashSet<>(Arrays.asList("userTask2", "userTask3"));
    assertThat(completedHistoricTaskInstances).hasSize(2);
    assertTrue(allowedTaskIds.contains(completedHistoricTaskInstances.get(0).getTaskDefinitionKey()));
    assertTrue(allowedTaskIds.contains(completedHistoricTaskInstances.get(1).getTaskDefinitionKey()));
  }

  @Test
  public void fishedAtParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask1")
      .userTask("userTask2")
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    completeAllUserTasks();

    // when
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    completeAllUserTasks();
    List<HistoricTaskInstance> completedHistoricTaskInstances =
      optimizeService.getCompletedHistoricTaskInstances(null, now, 10);

    // then
    assertThat(completedHistoricTaskInstances).hasSize(1);
    assertThat(completedHistoricTaskInstances.get(0).getTaskDefinitionKey()).isEqualTo("userTask1");
  }

  @Test
  public void fishedAfterAndFinishedAtParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask1")
      .userTask("userTask2")
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    completeAllUserTasks();

    // when
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    completeAllUserTasks();
    List<HistoricTaskInstance> completedHistoricTaskInstances =
      optimizeService.getCompletedHistoricTaskInstances(now, now, 10);

    // then
    assertThat(completedHistoricTaskInstances.size()).isZero();
  }

  @Test
  public void maxResultsParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .userTask()
      .userTask()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    completeAllUserTasks();
    completeAllUserTasks();
    completeAllUserTasks();
    completeAllUserTasks();

    // when
    List<HistoricTaskInstance> completedHistoricTaskInstances =
      optimizeService.getCompletedHistoricTaskInstances(pastDate(), null, 3);

    // then
    assertThat(completedHistoricTaskInstances).hasSize(3);
  }

  @Test
  public void resultIsSortedByEndTime() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask("userTask1")
      .userTask("userTask2")
      .userTask("userTask3")
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    completeAllUserTasks();

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    completeAllUserTasks();

    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    completeAllUserTasks();

    // when
    List<HistoricTaskInstance> completedHistoricTaskInstances =
      optimizeService.getCompletedHistoricTaskInstances(pastDate(), null, 4);

    // then
    assertThat(completedHistoricTaskInstances).hasSize(3);
    assertThat(completedHistoricTaskInstances.get(0).getTaskDefinitionKey()).isEqualTo("userTask1");
    assertThat(completedHistoricTaskInstances.get(1).getTaskDefinitionKey()).isEqualTo("userTask2");
    assertThat(completedHistoricTaskInstances.get(2).getTaskDefinitionKey()).isEqualTo("userTask3");
  }

  @Test
  public void fetchOnlyCompletedTasks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask1")
      .userTask("userTask2")
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    completeAllUserTasks();

    // when
    List<HistoricTaskInstance> completedHistoricTaskInstances =
      optimizeService.getCompletedHistoricTaskInstances(pastDate(), null, 10);

    // then
    assertThat(completedHistoricTaskInstances).hasSize(1);
    assertThat(completedHistoricTaskInstances.get(0).getTaskDefinitionKey()).isEqualTo("userTask1");
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

  private void assertThatTasksHaveAllImportantInformation(HistoricTaskInstance completedHistoricTaskInstance) {
    assertThat(completedHistoricTaskInstance).isNotNull();
    assertThat(completedHistoricTaskInstance.getId()).isNotNull();
    assertThat(completedHistoricTaskInstance.getTaskDefinitionKey()).isEqualTo("userTask");
    assertThat(completedHistoricTaskInstance.getName()).isEqualTo("task");
    assertThat(completedHistoricTaskInstance.getStartTime()).isNotNull();
    assertThat(completedHistoricTaskInstance.getEndTime()).isNotNull();
    assertThat(completedHistoricTaskInstance.getProcessDefinitionKey()).isEqualTo("process");
    assertThat(completedHistoricTaskInstance.getProcessDefinitionId()).isNotNull();
    assertThat(completedHistoricTaskInstance.getAssignee()).isEqualTo(userId);
  }

}
