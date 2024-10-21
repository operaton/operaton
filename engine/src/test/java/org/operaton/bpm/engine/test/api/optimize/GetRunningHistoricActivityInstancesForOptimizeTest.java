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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Date;
import java.util.List;

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
import org.operaton.bpm.engine.runtime.ProcessInstance;
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
public class GetRunningHistoricActivityInstancesForOptimizeTest {

  protected static final String VARIABLE_NAME = "aVariableName";
  protected static final String VARIABLE_VALUE = "aVariableValue";

  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testHelper);

  protected String userId = "test";
  private OptimizeService optimizeService;
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
  public void getRunningHistoricActivityInstances() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .name("start")
      .userTask("userTask")
      .name("task")
      .endEvent("endEvent")
      .name("end")
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");

    // when
    List<HistoricActivityInstance> runningHistoricActivityInstances =
      optimizeService.getRunningHistoricActivityInstances(pastDate(), null, 10);

    // then
    assertThat(runningHistoricActivityInstances.size(), is(1));
    HistoricActivityInstance activityInstance = runningHistoricActivityInstances.get(0);
    assertThatActivitiesHaveAllImportantInformation(activityInstance);
  }

  @Test
  public void startedAfterParameterWorks() {
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
    ProcessInstance secondProcessInstance =
      engineRule.getRuntimeService().startProcessInstanceByKey("process");
    List<HistoricActivityInstance> runningHistoricActivityInstances =
      optimizeService.getRunningHistoricActivityInstances(now, null, 10);

    // then
    assertThat(runningHistoricActivityInstances.size(), is(1));
    assertThat(runningHistoricActivityInstances.get(0).getProcessInstanceId(), is(secondProcessInstance.getId()));
  }

  @Test
  public void startedAtParameterWorks() {
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
    ProcessInstance firstProcessInstance =
      engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    List<HistoricActivityInstance> runningHistoricActivityInstances =
      optimizeService.getRunningHistoricActivityInstances(null, now, 10);

    // then
    assertThat(runningHistoricActivityInstances.size(), is(1));
    assertThat(runningHistoricActivityInstances.get(0).getProcessInstanceId(), is(firstProcessInstance.getId()));
  }

  @Test
  public void startedAfterAndStartedAtParameterWorks() {
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
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    List<HistoricActivityInstance> runningHistoricActivityInstances =
      optimizeService.getRunningHistoricActivityInstances(now, now, 10);

    // then
    assertThat(runningHistoricActivityInstances.size(), is(0));
  }

  @Test
  public void maxResultsParameterWorks() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask")
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    engineRule.getRuntimeService().startProcessInstanceByKey("process");
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    List<HistoricActivityInstance> runningHistoricActivityInstances =
      optimizeService.getRunningHistoricActivityInstances(pastDate(), null, 3);

    // then
    assertThat(runningHistoricActivityInstances.size(), is(3));
  }

  @Test
  public void resultIsSortedByStartTime() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask")
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
    ClockUtil.setCurrentTime(new Date());
    ProcessInstance firstProcessInstance =
      engineRule.getRuntimeService().startProcessInstanceByKey("process");
    shiftTimeByOneMinute();
    ProcessInstance secondProcessInstance =
      engineRule.getRuntimeService().startProcessInstanceByKey("process");
    shiftTimeByOneMinute();
    ProcessInstance thirdProcessInstance =
      engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    List<HistoricActivityInstance> runningHistoricActivityInstances =
      optimizeService.getRunningHistoricActivityInstances(pastDate(), null, 4);

    // then
    assertThat(runningHistoricActivityInstances.size(), is(3));
    assertThat(runningHistoricActivityInstances.get(0).getProcessInstanceId(), is(firstProcessInstance.getId()));
    assertThat(runningHistoricActivityInstances.get(1).getProcessInstanceId(), is(secondProcessInstance.getId()));
    assertThat(runningHistoricActivityInstances.get(2).getProcessInstanceId(), is(thirdProcessInstance.getId()));
  }

  public void shiftTimeByOneMinute() {
    Long oneMinute = 1000L * 60L;
    Date shiftedTimeByOneMinute = new Date(ClockUtil.getCurrentTime().getTime() + oneMinute);
    ClockUtil.setCurrentTime(shiftedTimeByOneMinute);
  }

  @Test
  public void fetchOnlyRunningActivities() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask")
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    engineRule.getRuntimeService().startProcessInstanceByKey("process");

    // when
    List<HistoricActivityInstance> runningHistoricActivityInstances =
      optimizeService.getRunningHistoricActivityInstances(pastDate(), null, 10);

    // then
    assertThat(runningHistoricActivityInstances.size(), is(1));
    assertThat(runningHistoricActivityInstances.get(0).getActivityId(), is("userTask"));

    // when
    completeAllUserTasks();
    runningHistoricActivityInstances =
      optimizeService.getRunningHistoricActivityInstances(pastDate(), null, 10);

    // then
    assertThat(runningHistoricActivityInstances.size(), is(0));
  }

  private Date pastDate() {
    return new Date(2L);
  }

  // test fetches only completed, even if there are still running activities

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

  private void assertThatActivitiesHaveAllImportantInformation(HistoricActivityInstance activityInstance) {
    assertThat(activityInstance, notNullValue());
    assertThat(activityInstance.getActivityName(), is("task"));
    assertThat(activityInstance.getActivityType(), is("userTask"));
    assertThat(activityInstance.getStartTime(), notNullValue());
    assertThat(activityInstance.getEndTime(), nullValue());
    assertThat(activityInstance.getProcessDefinitionKey(), is("process"));
    assertThat(activityInstance.getProcessDefinitionId(), notNullValue());
    assertThat(((HistoryEvent) activityInstance).getSequenceCounter(), notNullValue());
  }

}