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
package org.operaton.bpm.engine.test.history;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLog;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLogQuery;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.task.IdentityLinkType;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
@ExtendWith(ProcessEngineExtension.class)
class HistoricIdentityLinkLogQueryTest {
  private static final String A_USER_ID = "aUserId";
  private static final String A_GROUP_ID = "aGroupId";
  private static final int NUMBER_OF_USERS = 3;
  private static final String A_ASSIGNER_ID = "aAssignerId";

  private static final String INVALID_USER_ID = "InvalidUserId";
  private static final String INVALID_TASK_ID = "InvalidTask";
  private static final String INVALID_GROUP_ID = "InvalidGroupId";
  private static final String INVALID_ASSIGNER_ID = "InvalidAssignerId";
  private static final String INVALID_HISTORY_EVENT_TYPE = "InvalidEventType";
  private static final String INVALID_IDENTITY_LINK_TYPE = "InvalidIdentityLinkType";
  private static final String INVALID_PROCESS_DEFINITION_ID = "InvalidProcessDefinitionId";
  private static final String INVALID_PROCESS_DEFINITION_KEY = "InvalidProcessDefinitionKey";
  private static final String GROUP_1 = "Group1";
  private static final String USER_1 = "User1";
  private static final String PROCESS_DEFINITION_KEY = "oneTaskProcess";
  private static final String PROCESS_DEFINITION_KEY_MULTIPLE_CANDIDATE_USER = "oneTaskProcessForHistoricIdentityLinkWithMultipleCanidateUser";
  private static final String IDENTITY_LINK_ADD="add";
  private static final String IDENTITY_LINK_DELETE="delete";

  TaskService taskService;
  HistoryService historyService;
  IdentityService identityService;
  RepositoryService repositoryService;
  RuntimeService runtimeService;

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryAddTaskCandidateforAddIdentityLink() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();

    // given
    ProcessInstance processInstance = startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);

    // Query test
    HistoricIdentityLinkLog historicIdentityLink = historyService.createHistoricIdentityLinkLogQuery().singleResult();
    assertThat(historicIdentityLink.getUserId()).isEqualTo(A_USER_ID);
    assertThat(taskId).isEqualTo(historicIdentityLink.getTaskId());
    assertThat(historicIdentityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
    assertThat(historicIdentityLink.getAssignerId()).isEqualTo(A_ASSIGNER_ID);
    assertThat(historicIdentityLink.getGroupId()).isNull();
    assertThat(historicIdentityLink.getOperationType()).isEqualTo(IDENTITY_LINK_ADD);
    assertThat(processInstance.getProcessDefinitionId()).isEqualTo(historicIdentityLink.getProcessDefinitionId());
    assertThat(historicIdentityLink.getProcessDefinitionKey()).isEqualTo(PROCESS_DEFINITION_KEY);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testGroupQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();

    // given
    ProcessInstance processInstance = startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateGroup(taskId, A_GROUP_ID);

    // Query test
    HistoricIdentityLinkLog historicIdentityLink = historyService.createHistoricIdentityLinkLogQuery().singleResult();
    assertThat(historicIdentityLink.getUserId()).isNull();
    assertThat(taskId).isEqualTo(historicIdentityLink.getTaskId());
    assertThat(historicIdentityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
    assertThat(historicIdentityLink.getAssignerId()).isEqualTo(A_ASSIGNER_ID);
    assertThat(historicIdentityLink.getGroupId()).isEqualTo(A_GROUP_ID);
    assertThat(historicIdentityLink.getOperationType()).isEqualTo(IDENTITY_LINK_ADD);
    assertThat(processInstance.getProcessDefinitionId()).isEqualTo(historicIdentityLink.getProcessDefinitionId());
    assertThat(historicIdentityLink.getProcessDefinitionKey()).isEqualTo(PROCESS_DEFINITION_KEY);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testValidIndividualQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();

    // given
    ProcessInstance processInstance = startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // Valid Individual Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.taskId(taskId).count()).isEqualTo(2);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.type(IdentityLinkType.CANDIDATE).count()).isEqualTo(2);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.userId(A_USER_ID).count()).isEqualTo(2);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.assignerId(A_ASSIGNER_ID).count()).isEqualTo(2);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.operationType(IDENTITY_LINK_DELETE).count()).isOne();

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.operationType(IDENTITY_LINK_ADD).count()).isOne();

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.processDefinitionId(processInstance.getProcessDefinitionId()).count()).isEqualTo(2);

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.processDefinitionKey(PROCESS_DEFINITION_KEY).count()).isEqualTo(2);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testValidGroupQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();

    // given
    ProcessInstance processInstance = startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // Valid group query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.taskId(taskId).count()).isEqualTo(2);
    assertThat(query.type(IdentityLinkType.CANDIDATE).count()).isEqualTo(2);
    assertThat(query.userId(A_USER_ID).count()).isEqualTo(2);
    assertThat(query.assignerId(A_ASSIGNER_ID).count()).isEqualTo(2);
    assertThat(query.processDefinitionId(processInstance.getProcessDefinitionId()).count()).isEqualTo(2);
    assertThat(query.processDefinitionKey(PROCESS_DEFINITION_KEY).count()).isEqualTo(2);
    assertThat(query.operationType(IDENTITY_LINK_DELETE).count()).isOne();
    assertThat(query.operationType(IDENTITY_LINK_ADD).count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testInvalidIndividualQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // Invalid Individual Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.taskId(INVALID_TASK_ID).count()).isZero();

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.type(INVALID_IDENTITY_LINK_TYPE).count()).isZero();

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.userId(INVALID_USER_ID).count()).isZero();

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.groupId(INVALID_GROUP_ID).count()).isZero();

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.assignerId(INVALID_ASSIGNER_ID).count()).isZero();

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.operationType(INVALID_HISTORY_EVENT_TYPE).count()).isZero();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testInvalidGroupQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // Invalid Individual Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.taskId(INVALID_TASK_ID).count()).isZero();
    assertThat(query.type(INVALID_IDENTITY_LINK_TYPE).count()).isZero();
    assertThat(query.userId(INVALID_USER_ID).count()).isZero();
    assertThat(query.groupId(INVALID_GROUP_ID).count()).isZero();
    assertThat(query.assignerId(INVALID_ASSIGNER_ID).count()).isZero();
    assertThat(query.operationType(INVALID_HISTORY_EVENT_TYPE).count()).isZero();
    assertThat(query.processDefinitionId(INVALID_PROCESS_DEFINITION_ID).count()).isZero();
    assertThat(query.processDefinitionKey(INVALID_PROCESS_DEFINITION_KEY).count()).isZero();
  }

  /**
   * Should add 3 history records of identity link addition at 01-01-2016
   * 00:00.00 Should add 3 history records of identity link deletion at
   * 01-01-2016 12:00.00
   *
   * <p>
   * Should add 3 history records of identity link addition at 01-01-2016
   * 12:30.00 Should add 3 history records of identity link deletion at
   * 01-01-2016 21:00.00
   * </p>
   *
   * <p>
   * Test case: Query the number of added records at different time interval.
   * </p>
   */
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testShouldAddTaskOwnerForAddandDeleteIdentityLinkByTimeStamp() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    ClockUtil.setCurrentTime(newYearMorning(0));
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    // Adds aUserId1, deletes aUserID1, adds aUserId2, deletes aUserId2, Adds aUserId3 - 5
    addUserIdentityLinks(taskId);

    ClockUtil.setCurrentTime(newYearNoon(0));
    //Deletes aUserId3
    deleteUserIdentityLinks(taskId);

    ClockUtil.setCurrentTime(newYearNoon(30));
    addUserIdentityLinks(taskId);

    ClockUtil.setCurrentTime(newYearEvening());
    deleteUserIdentityLinks(taskId);

    // Query records with time before 12:20
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.dateBefore(newYearNoon(20)).count()).isEqualTo(6);
    assertThat(query.operationType(IDENTITY_LINK_ADD).count()).isEqualTo(3);
    assertThat(query.operationType(IDENTITY_LINK_DELETE).count()).isEqualTo(3);

    // Query records with time between 00:01 and 12:00
    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.dateBefore(newYearNoon(0)).count()).isEqualTo(6);
    assertThat(query.dateAfter(newYearMorning(1)).count()).isOne();
    assertThat(query.operationType(IDENTITY_LINK_ADD).count()).isZero();
    assertThat(query.operationType(IDENTITY_LINK_DELETE).count()).isOne();

    // Query records with time after 12:45
    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.dateAfter(newYearNoon(45)).count()).isOne();
    assertThat(query.operationType(IDENTITY_LINK_ADD).count()).isZero();
    assertThat(query.operationType(IDENTITY_LINK_DELETE).count()).isOne();

    ClockUtil.setCurrentTime(new Date());
  }

  @SuppressWarnings("deprecation")
  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryAddAndRemoveIdentityLinksForProcessDefinition() {

    ProcessDefinition latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).singleResult();
    assertThat(latestProcessDef).isNotNull();
    List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
    assertThat(links).isEmpty();

    // Add candiate group with process definition
    repositoryService.addCandidateStarterGroup(latestProcessDef.getId(), GROUP_1);
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).hasSize(1);
    // Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.processDefinitionId(latestProcessDef.getId()).count()).isOne();
    assertThat(query.operationType(IDENTITY_LINK_ADD).count()).isOne();
    assertThat(query.groupId(GROUP_1).count()).isOne();

    // Add candidate user for process definition
    repositoryService.addCandidateStarterUser(latestProcessDef.getId(), USER_1);
    // Query test
    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.processDefinitionId(latestProcessDef.getId()).count()).isEqualTo(2);
    assertThat(query.processDefinitionKey(latestProcessDef.getKey()).count()).isEqualTo(2);
    assertThat(query.operationType(IDENTITY_LINK_ADD).count()).isEqualTo(2);
    assertThat(query.userId(USER_1).count()).isOne();

    // Delete candiate group with process definition
    repositoryService.deleteCandidateStarterGroup(latestProcessDef.getId(), GROUP_1);
    // Query test
    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.processDefinitionId(latestProcessDef.getId()).count()).isEqualTo(3);
    assertThat(query.processDefinitionKey(latestProcessDef.getKey()).count()).isEqualTo(3);
    assertThat(query.groupId(GROUP_1).count()).isEqualTo(2);
    assertThat(query.operationType(IDENTITY_LINK_DELETE).count()).isOne();

    // Delete candidate user for process definition
    repositoryService.deleteCandidateStarterUser(latestProcessDef.getId(), USER_1);
    // Query test
    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.processDefinitionId(latestProcessDef.getId()).count()).isEqualTo(4);
    assertThat(query.processDefinitionKey(latestProcessDef.getKey()).count()).isEqualTo(4);
    assertThat(query.userId(USER_1).count()).isEqualTo(2);
    query = historyService.createHistoricIdentityLinkLogQuery();
    assertThat(query.operationType(IDENTITY_LINK_DELETE).count()).isEqualTo(2);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/OneTaskProcessWithMultipleCandidateUser.bpmn20.xml"})
  @Test
  void testHistoricIdentityLinkQueryPaging() {
    startProcessInstance(PROCESS_DEFINITION_KEY_MULTIPLE_CANDIDATE_USER);

    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();

    assertThat(query.listPage(0, 4)).hasSize(4);
    assertThat(query.listPage(2, 1)).hasSize(1);
    assertThat(query.listPage(1, 2)).hasSize(2);
    assertThat(query.listPage(1, 4)).hasSize(3);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/OneTaskProcessWithMultipleCandidateUser.bpmn20.xml"})
  @Test
  void testHistoricIdentityLinkQuerySorting() {

    // Pre test - Historical identity link is added as part of deployment
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertThat(historicIdentityLinks).isEmpty();
    startProcessInstance(PROCESS_DEFINITION_KEY_MULTIPLE_CANDIDATE_USER);

    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByAssignerId().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByTime().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByGroupId().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByType().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByOperationType().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByProcessDefinitionId().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByProcessDefinitionKey().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByTaskId().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByUserId().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByTenantId().asc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByUserId().asc().list().get(0).getUserId()).isEqualTo("aUser");
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByUserId().asc().list().get(3).getUserId()).isEqualTo("dUser");

    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByAssignerId().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByTime().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByGroupId().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByType().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByOperationType().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByProcessDefinitionId().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByProcessDefinitionKey().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByTaskId().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByUserId().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByTenantId().desc().list()).hasSize(4);
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByUserId().desc().list().get(0).getUserId()).isEqualTo("dUser");
    assertThat(historyService.createHistoricIdentityLinkLogQuery().orderByUserId().desc().list().get(3).getUserId()).isEqualTo("aUser");
  }

  public void addUserIdentityLinks(String taskId) {
    for (int userIndex = 1;userIndex <= NUMBER_OF_USERS;userIndex++) {
      taskService.addUserIdentityLink(taskId, A_USER_ID + userIndex, IdentityLinkType.ASSIGNEE);
    }
  }

  public void deleteUserIdentityLinks(String taskId) {
    for (int userIndex = 1;userIndex <= NUMBER_OF_USERS;userIndex++) {
      taskService.deleteUserIdentityLink(taskId, A_USER_ID + userIndex, IdentityLinkType.ASSIGNEE);
    }
  }

  public Date newYearMorning(int minutes) {
    Calendar calendar = new GregorianCalendar();
    calendar.set(Calendar.YEAR, 2016);
    calendar.set(Calendar.MONTH, 0);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, minutes);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

  public Date newYearNoon(int minutes) {
    Calendar calendar = new GregorianCalendar();
    calendar.set(Calendar.YEAR, 2016);
    calendar.set(Calendar.MONTH, 0);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 12);
    calendar.set(Calendar.MINUTE, minutes);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

  public Date newYearEvening() {
    Calendar calendar = new GregorianCalendar();
    calendar.set(Calendar.YEAR, 2016);
    calendar.set(Calendar.MONTH, 0);
    calendar.set(Calendar.DAY_OF_MONTH, 1);
    calendar.set(Calendar.HOUR_OF_DAY, 21);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

  protected ProcessInstance startProcessInstance(String key) {
    return runtimeService.startProcessInstanceByKey(key);
  }
}
