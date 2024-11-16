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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.OptimizeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.optimize.OptimizeHistoricIdentityLinkLogEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.IdentityLinkType;
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
public class GetHistoricIdentityLinkLogsForOptimizeTest {

  public static final String IDENTITY_LINK_ADD = "add";
  public static final String IDENTITY_LINK_DELETE = "delete";
  public ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testHelper);

  private OptimizeService optimizeService;

  protected static final String userId = "testUser";
  protected static final String assignerId = "testAssigner";
  protected static final String groupId = "testGroup";

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
    createGroup();
    identityService.setAuthenticatedUserId(userId);
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
    identityService.clearAuthentication();
  }

  @Test
  public void allNecessaryInformationIsAvailable() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask")
        .name("task")
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    identityService.setAuthenticatedUserId(assignerId);
    taskService.addCandidateUser(taskId, userId);

    // when
    List<OptimizeHistoricIdentityLinkLogEntity> identityLinkLogs =
      optimizeService.getHistoricIdentityLinkLogs(pastDate(), null, 10);

    // then
    assertThat(identityLinkLogs.size()).isEqualTo(1);
    assertThatIdentityLinksHaveAllImportantInformation(identityLinkLogs.get(0), processInstance);
  }

  @Test
  public void performCandidateOperations() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask")
        .name("task")
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    identityService.setAuthenticatedUserId(assignerId);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    taskService.addCandidateUser(taskId, userId);
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    taskService.deleteCandidateUser(taskId, userId);
    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    taskService.addCandidateGroup(taskId, groupId);
    Date nowPlus6Seconds = new Date(now.getTime() + 6000L);
    ClockUtil.setCurrentTime(nowPlus6Seconds);
    taskService.deleteCandidateGroup(taskId, groupId);
    Date nowPlus8Seconds = new Date(now.getTime() + 8000L);
    ClockUtil.setCurrentTime(nowPlus8Seconds);

    // when
    List<OptimizeHistoricIdentityLinkLogEntity> identityLinkLogs =
      optimizeService.getHistoricIdentityLinkLogs(pastDate(), null, 10);

    // then
    assertThat(identityLinkLogs.size()).isEqualTo(4);
    assertThat(identityLinkLogs.get(0).getUserId()).isEqualTo(userId);
    assertThat(identityLinkLogs.get(0).getOperationType()).isEqualTo(IDENTITY_LINK_ADD);
    assertThat(identityLinkLogs.get(0).getType()).isEqualTo(IdentityLinkType.CANDIDATE);
    assertThat(identityLinkLogs.get(1).getUserId()).isEqualTo(userId);
    assertThat(identityLinkLogs.get(1).getOperationType()).isEqualTo(IDENTITY_LINK_DELETE);
    assertThat(identityLinkLogs.get(1).getType()).isEqualTo(IdentityLinkType.CANDIDATE);
    assertThat(identityLinkLogs.get(2).getGroupId()).isEqualTo(groupId);
    assertThat(identityLinkLogs.get(2).getOperationType()).isEqualTo(IDENTITY_LINK_ADD);
    assertThat(identityLinkLogs.get(2).getType()).isEqualTo(IdentityLinkType.CANDIDATE);
    assertThat(identityLinkLogs.get(3).getGroupId()).isEqualTo(groupId);
    assertThat(identityLinkLogs.get(3).getOperationType()).isEqualTo(IDENTITY_LINK_DELETE);
    assertThat(identityLinkLogs.get(3).getType()).isEqualTo(IdentityLinkType.CANDIDATE);
  }

  @Test
  public void performAssigneeOperations() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent("startEvent")
      .userTask("userTask")
        .name("task")
      .endEvent("endEvent")
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");
    identityService.setAuthenticatedUserId(assignerId);
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    claimAllUserTasks();
    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    unclaimAllUserTasks();

    // when
    List<OptimizeHistoricIdentityLinkLogEntity> identityLinkLogs =
      optimizeService.getHistoricIdentityLinkLogs(pastDate(), null, 10);

    // then
    assertThat(identityLinkLogs.size()).isEqualTo(2);
    assertThat(identityLinkLogs.get(0).getUserId()).isEqualTo(userId);
    assertThat(identityLinkLogs.get(0).getOperationType()).isEqualTo(IDENTITY_LINK_ADD);
    assertThat(identityLinkLogs.get(0).getType()).isEqualTo(IdentityLinkType.ASSIGNEE);
    assertThat(identityLinkLogs.get(1).getUserId()).isEqualTo(userId);
    assertThat(identityLinkLogs.get(1).getOperationType()).isEqualTo(IDENTITY_LINK_DELETE);
    assertThat(identityLinkLogs.get(0).getType()).isEqualTo(IdentityLinkType.ASSIGNEE);
  }

  @Test
  public void occurredAfterParameterWorks() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask("userTask")
        .operatonAssignee(userId)
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    taskService.addCandidateUser(taskId, userId);

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    taskService.deleteCandidateUser(taskId, userId);

    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    taskService.addCandidateUser(taskId, userId);

    // when
    List<OptimizeHistoricIdentityLinkLogEntity> identityLinkLogs =
      optimizeService.getHistoricIdentityLinkLogs(now, null, 10);

    // then
    assertThat(identityLinkLogs.size()).isEqualTo(2);
  }

  @Test
  public void occurredAtParameterWorks() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask("userTask")
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    taskService.addCandidateUser(taskId, userId);

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    taskService.deleteCandidateUser(taskId, userId);

    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    taskService.addCandidateUser(taskId, userId);

    // when
    List<OptimizeHistoricIdentityLinkLogEntity> identityLinkLogs =
      optimizeService.getHistoricIdentityLinkLogs(null, now, 10);

    // then
    assertThat(identityLinkLogs.size()).isEqualTo(1);
  }

  @Test
  public void occurredAfterAndOccurredAtParameterWorks() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask("userTask")
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    taskService.addCandidateUser(taskId, userId);

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    taskService.deleteCandidateUser(taskId, userId);

    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    taskService.addCandidateUser(taskId, userId);

    // when
    List<OptimizeHistoricIdentityLinkLogEntity> identityLinkLogs =
      optimizeService.getHistoricIdentityLinkLogs(now, now, 10);

    // then
    assertThat(identityLinkLogs.size()).isZero();
  }

  @Test
  public void maxResultsParameterWorks() {
     // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask()
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.addCandidateUser(taskId, userId);
    taskService.deleteCandidateUser(taskId, userId);
    taskService.addCandidateUser(taskId, userId);
    taskService.deleteCandidateUser(taskId, userId);
    taskService.addCandidateUser(taskId, userId);

    // when
    List<OptimizeHistoricIdentityLinkLogEntity> identityLinkLogs =
      optimizeService.getHistoricIdentityLinkLogs(pastDate(), null, 3);

    // then
    assertThat(identityLinkLogs.size()).isEqualTo(3);
  }

  @Test
  public void resultIsSortedByTimestamp() {
    // given
    BpmnModelInstance simpleDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .userTask("userTask")
      .endEvent()
      .done();
    testHelper.deploy(simpleDefinition);
    runtimeService.startProcessInstanceByKey("process");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    Date now = new Date();
    ClockUtil.setCurrentTime(now);
    taskService.addCandidateUser(taskId, userId);

    Date nowPlus2Seconds = new Date(now.getTime() + 2000L);
    ClockUtil.setCurrentTime(nowPlus2Seconds);
    taskService.deleteCandidateUser(taskId, userId);

    Date nowPlus4Seconds = new Date(now.getTime() + 4000L);
    ClockUtil.setCurrentTime(nowPlus4Seconds);
    taskService.addCandidateUser(taskId, userId);

    // when
    List<OptimizeHistoricIdentityLinkLogEntity> identityLinkLogs =
      optimizeService.getHistoricIdentityLinkLogs(pastDate(), null, 4);

    // then
    assertThat(identityLinkLogs.size()).isEqualTo(3);
    assertThat(identityLinkLogs.get(0).getOperationType()).isEqualTo(IDENTITY_LINK_ADD);
    assertThat(identityLinkLogs.get(1).getOperationType()).isEqualTo(IDENTITY_LINK_DELETE);
    assertThat(identityLinkLogs.get(2).getOperationType()).isEqualTo(IDENTITY_LINK_ADD);
  }

  private Date pastDate() {
    return new Date(2L);
  }

  private void claimAllUserTasks() {
    List<Task> list = taskService.createTaskQuery().list();
    for (Task task : list) {
      taskService.claim(task.getId(), userId);
    }
  }

  private void unclaimAllUserTasks() {
    List<Task> list = taskService.createTaskQuery().list();
    for (Task task : list) {
      taskService.setAssignee(task.getId(), null);
    }
  }

  protected void createUser(String userId) {
    User user = identityService.newUser(userId);
    identityService.saveUser(user);
  }

  protected void createGroup() {
    Group group = identityService.newGroup(GetHistoricIdentityLinkLogsForOptimizeTest.groupId);
    identityService.saveGroup(group);
  }

  private void assertThatIdentityLinksHaveAllImportantInformation(OptimizeHistoricIdentityLinkLogEntity identityLinkLog,
                                                                  ProcessInstance processInstance) {
    assertThat(identityLinkLog).isNotNull();
    assertThat(identityLinkLog.getUserId()).isEqualTo(userId);
    assertThat(identityLinkLog.getTaskId()).isEqualTo(taskService.createTaskQuery().singleResult().getId());
    assertThat(identityLinkLog.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
    assertThat(identityLinkLog.getAssignerId()).isEqualTo(assignerId);
    assertThat(identityLinkLog.getGroupId()).isNull();
    assertThat(identityLinkLog.getOperationType()).isEqualTo(IDENTITY_LINK_ADD);
    assertThat(identityLinkLog.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
    assertThat(identityLinkLog.getProcessDefinitionKey()).isEqualTo("process");
    assertThat(identityLinkLog.getProcessInstanceId()).isEqualTo(processInstance.getId());
  }

}
