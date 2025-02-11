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
package org.operaton.bpm.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLog;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLogQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.task.IdentityLinkType;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class HistoricIdentityLinkLogTest extends PluggableProcessEngineTest {
  private static final String A_USER_ID = "aUserId";
  private static final String B_USER_ID = "bUserId";
  private static final String C_USER_ID = "cUserId";
  private static final int NUMBER_OF_USERS = 3;
  private static final String A_GROUP_ID = "aGroupId";
  private static final String INVALID_USER_ID = "InvalidUserId";
  private static final String A_ASSIGNER_ID = "aAssignerId";
  private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";
  private static final String GROUP_1 = "Group1";
  private static final String USER_1 = "User1";
  private static final String OWNER_1 = "Owner1";
  private static final String IDENTITY_LINK_ADD="add";
  private static final String IDENTITY_LINK_DELETE="delete";

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testShouldAddTaskCandidateforAddIdentityLink() {

    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);

    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(1);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testShouldAddDelegateTaskCandidateforAddIdentityLink() {

    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addUserIdentityLink(taskId, A_USER_ID, IdentityLinkType.ASSIGNEE);
    taskService.delegateTask(taskId, B_USER_ID);
    taskService.deleteUserIdentityLink(taskId, B_USER_ID, IdentityLinkType.ASSIGNEE);
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    // Addition of A_USER, Deletion of A_USER, Addition of A_USER as owner, Addition of B_USER and deletion of B_USER
    assertThat(historicIdentityLinks.size()).isEqualTo(5);

    //Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.userId(A_USER_ID).count()).isEqualTo(3);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.userId(B_USER_ID).count()).isEqualTo(2);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.operationType(IDENTITY_LINK_ADD).count()).isEqualTo(3);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.operationType(IDENTITY_LINK_DELETE).count()).isEqualTo(2);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.type(IdentityLinkType.ASSIGNEE).count()).isEqualTo(4);
    assertThat(query.type(IdentityLinkType.OWNER).count()).isEqualTo(1);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testShouldAddClaimTaskCandidateforAddIdentityLink() {

    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.claim(taskId, A_USER_ID);

    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(1);

    //Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.userId(A_USER_ID).count()).isEqualTo(1);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.operationType(IDENTITY_LINK_ADD).count()).isEqualTo(1);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.operationType(IDENTITY_LINK_DELETE).count()).isEqualTo(0);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.type(IdentityLinkType.ASSIGNEE).count()).isEqualTo(1);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testShouldAddMultipleDelegateTaskCandidateforAddIdentityLink() {

    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addUserIdentityLink(taskId, A_USER_ID, IdentityLinkType.ASSIGNEE);
    taskService.delegateTask(taskId, B_USER_ID);
    taskService.delegateTask(taskId, C_USER_ID);
    taskService.deleteUserIdentityLink(taskId, C_USER_ID, IdentityLinkType.ASSIGNEE);
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    // Addition of A_USER, Deletion of A_USER, Addition of A_USER as owner,
    // Addition of B_USER, Deletion of B_USER, Addition of C_USER, Deletion of C_USER
    assertThat(historicIdentityLinks.size()).isEqualTo(7);

    //Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.userId(A_USER_ID).count()).isEqualTo(3);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.userId(B_USER_ID).count()).isEqualTo(2);


    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.userId(C_USER_ID).count()).isEqualTo(2);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.operationType(IDENTITY_LINK_ADD).count()).isEqualTo(4);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.operationType(IDENTITY_LINK_DELETE).count()).isEqualTo(3);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.type(IdentityLinkType.ASSIGNEE).count()).isEqualTo(6);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.type(IdentityLinkType.OWNER).count()).isEqualTo(1);
  }
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testShouldAddTaskCandidateForAddAndDeleteIdentityLink() {

    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(2);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testShouldAddGroupCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateGroup(taskId, A_GROUP_ID);
    taskService.deleteCandidateGroup(taskId, A_GROUP_ID);

    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(2);

    // Basic Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.groupId(A_GROUP_ID).count()).isEqualTo(2);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testShouldNotAddTaskCandidateForInvalidIdentityLinkDelete() {

    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.deleteCandidateUser(taskId, INVALID_USER_ID);

    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testShouldAddTaskAssigneeForAddandDeleteIdentityLink() {

    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    addAndDeleteUserWithAssigner(taskId, IdentityLinkType.ASSIGNEE);
    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(2);

    // Basic Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.type(IdentityLinkType.ASSIGNEE).count()).isEqualTo(2);
  }

  @SuppressWarnings("deprecation")
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testShouldAddAndRemoveIdentityLinksForProcessDefinition() {

    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);

    // Given
    ProcessDefinition latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).singleResult();
    assertNotNull(latestProcessDef);
    List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
    assertThat(links.size()).isEqualTo(0);

    // Add candiate group with process definition
    repositoryService.addCandidateStarterGroup(latestProcessDef.getId(), GROUP_1);
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(1);

    // Add candidate user for process definition
    repositoryService.addCandidateStarterUser(latestProcessDef.getId(), USER_1);
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(2);

    // Delete candiate group with process definition
    repositoryService.deleteCandidateStarterGroup(latestProcessDef.getId(), GROUP_1);
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(3);

    // Delete candidate user for process definition
    repositoryService.deleteCandidateStarterUser(latestProcessDef.getId(), USER_1);
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(4);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testShouldAddTaskOwnerForAddandDeleteIdentityLink() {

    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    addAndDeleteUserWithAssigner(taskId, IdentityLinkType.OWNER);

    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(2);

    // Basic Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.type(IdentityLinkType.OWNER).count()).isEqualTo(2);
  }

  @Test
  public void testShouldAddIdentityLinkForTaskCreationWithAssigneeAndOwner() {

    String taskAssigneeId = "Assigneee";
    String taskOwnerId = "Owner";
    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);

    Task taskAssignee = taskService.newTask(taskAssigneeId);
    taskAssignee.setAssignee(USER_1);
    taskService.saveTask(taskAssignee);

    Task taskOwner = taskService.newTask(taskOwnerId);
    taskOwner.setOwner(OWNER_1);
    taskService.saveTask(taskOwner);

    Task taskEmpty = taskService.newTask();
    taskService.saveTask(taskEmpty);

    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(2);

    // Basic Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.type(IdentityLinkType.ASSIGNEE).count()).isEqualTo(1);
    assertThat(query.userId(USER_1).count()).isEqualTo(1);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.type(IdentityLinkType.OWNER).count()).isEqualTo(1);
    assertThat(query.userId(OWNER_1).count()).isEqualTo(1);

    taskService.deleteTask(taskAssigneeId,true);
    taskService.deleteTask(taskOwnerId,true);
    taskService.deleteTask(taskEmpty.getId(), true);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testShouldAddIdentityLinkByProcessDefinitionAndStandalone() {

    String taskAssigneeId = "Assigneee";
    // Pre test
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);

    ProcessInstance processInstance = startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // given
    Task taskAssignee = taskService.newTask(taskAssigneeId);
    taskAssignee.setAssignee(USER_1);
    taskService.saveTask(taskAssignee);

    // if
    addAndDeleteUserWithAssigner(taskId, IdentityLinkType.ASSIGNEE);

    // then
    historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(3);

    // Basic Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.type(IdentityLinkType.ASSIGNEE).count()).isEqualTo(3);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.processDefinitionId(processInstance.getProcessDefinitionId()).count()).isEqualTo(2);
    assertThat(query.processDefinitionKey(PROCESS_DEFINITION_KEY).count()).isEqualTo(2);

    taskService.deleteTask(taskAssigneeId, true);
  }

  //CAM-7456
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testShouldNotDeleteIdentityLinkForTaskCompletion() {
    //given
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks.size()).isEqualTo(0);
    startProcessInstance(PROCESS_DEFINITION_KEY);

    Task task = taskService.createTaskQuery().singleResult();
    taskService.addCandidateUser(task.getId(), "demo");

    //when
    taskService.complete(task.getId());

    //then
    List<HistoricIdentityLinkLog> historicIdentityLinkLogs = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinkLogs.size()).isEqualTo(1);
    assertNotEquals(IDENTITY_LINK_DELETE, historicIdentityLinkLogs.get(0).getOperationType());
  }

  public void addAndDeleteUserWithAssigner(String taskId, String identityLinkType) {
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addUserIdentityLink(taskId, A_USER_ID, identityLinkType);
    taskService.deleteUserIdentityLink(taskId, A_USER_ID, identityLinkType);
  }

  public void addUserIdentityLinks(String taskId) {
    for (int userIndex = 1; userIndex <= NUMBER_OF_USERS; userIndex++)
      taskService.addUserIdentityLink(taskId, A_USER_ID + userIndex, IdentityLinkType.OWNER);
  }

  public void deleteUserIdentityLinks(String taskId) {
    for (int userIndex = 1; userIndex <= NUMBER_OF_USERS; userIndex++)
      taskService.deleteUserIdentityLink(taskId, A_USER_ID + userIndex, IdentityLinkType.OWNER);
  }

  protected ProcessInstance startProcessInstance(String key) {
    return runtimeService.startProcessInstanceByKey(key);
  }

}
