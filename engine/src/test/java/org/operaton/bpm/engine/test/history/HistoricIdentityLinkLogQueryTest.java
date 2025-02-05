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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLog;
import org.operaton.bpm.engine.history.HistoricIdentityLinkLogQuery;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.task.IdentityLinkType;
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
public class HistoricIdentityLinkLogQueryTest extends PluggableProcessEngineTest {
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

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testQueryAddTaskCandidateforAddIdentityLink() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertEquals(0, historicIdentityLinks.size());

    // given
    ProcessInstance processInstance = startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);

    // Query test
    HistoricIdentityLinkLog historicIdentityLink = historyService.createHistoricIdentityLinkLogQuery().singleResult();
    assertEquals(A_USER_ID, historicIdentityLink.getUserId());
    assertEquals(historicIdentityLink.getTaskId(), taskId);
    assertEquals(IdentityLinkType.CANDIDATE, historicIdentityLink.getType());
    assertEquals(A_ASSIGNER_ID, historicIdentityLink.getAssignerId());
    assertNull(historicIdentityLink.getGroupId());
    assertEquals(IDENTITY_LINK_ADD, historicIdentityLink.getOperationType());
    assertEquals(historicIdentityLink.getProcessDefinitionId(), processInstance.getProcessDefinitionId());
    assertEquals(PROCESS_DEFINITION_KEY, historicIdentityLink.getProcessDefinitionKey());
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testGroupQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertEquals(0, historicIdentityLinks.size());

    // given
    ProcessInstance processInstance = startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateGroup(taskId, A_GROUP_ID);

    // Query test
    HistoricIdentityLinkLog historicIdentityLink = historyService.createHistoricIdentityLinkLogQuery().singleResult();
    assertNull(historicIdentityLink.getUserId());
    assertEquals(historicIdentityLink.getTaskId(), taskId);
    assertEquals(IdentityLinkType.CANDIDATE, historicIdentityLink.getType());
    assertEquals(A_ASSIGNER_ID, historicIdentityLink.getAssignerId());
    assertEquals(A_GROUP_ID, historicIdentityLink.getGroupId());
    assertEquals(IDENTITY_LINK_ADD, historicIdentityLink.getOperationType());
    assertEquals(historicIdentityLink.getProcessDefinitionId(), processInstance.getProcessDefinitionId());
    assertEquals(PROCESS_DEFINITION_KEY, historicIdentityLink.getProcessDefinitionKey());
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testValidIndividualQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertEquals(0, historicIdentityLinks.size());

    // given
    ProcessInstance processInstance = startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // Valid Individual Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(2, query.taskId(taskId).count());

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(2, query.type(IdentityLinkType.CANDIDATE).count());

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(2, query.userId(A_USER_ID).count());

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(2, query.assignerId(A_ASSIGNER_ID).count());

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(1, query.operationType(IDENTITY_LINK_DELETE).count());

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(1, query.operationType(IDENTITY_LINK_ADD).count());

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(2, query.processDefinitionId(processInstance.getProcessDefinitionId()).count());

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(2, query.processDefinitionKey(PROCESS_DEFINITION_KEY).count());

  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testValidGroupQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertEquals(0, historicIdentityLinks.size());

    // given
    ProcessInstance processInstance = startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // Valid group query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(2, query.taskId(taskId).count());
    assertEquals(2, query.type(IdentityLinkType.CANDIDATE).count());
    assertEquals(2, query.userId(A_USER_ID).count());
    assertEquals(2, query.assignerId(A_ASSIGNER_ID).count());
    assertEquals(2, query.processDefinitionId(processInstance.getProcessDefinitionId()).count());
    assertEquals(2, query.processDefinitionKey(PROCESS_DEFINITION_KEY).count());
    assertEquals(1, query.operationType(IDENTITY_LINK_DELETE).count());
    assertEquals(1, query.operationType(IDENTITY_LINK_ADD).count());
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testInvalidIndividualQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertEquals(0, historicIdentityLinks.size());

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // Invalid Individual Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(0, query.taskId(INVALID_TASK_ID).count());

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(0, query.type(INVALID_IDENTITY_LINK_TYPE).count());

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(0, query.userId(INVALID_USER_ID).count());

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(0, query.groupId(INVALID_GROUP_ID).count());

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(0, query.assignerId(INVALID_ASSIGNER_ID).count());

    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(0, query.operationType(INVALID_HISTORY_EVENT_TYPE).count());

  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testInvalidGroupQueryTaskCandidateForAddAndDeleteIdentityLink() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertEquals(0, historicIdentityLinks.size());

    // given
    startProcessInstance(PROCESS_DEFINITION_KEY);
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // if
    identityService.setAuthenticatedUserId(A_ASSIGNER_ID);
    taskService.addCandidateUser(taskId, A_USER_ID);
    taskService.deleteCandidateUser(taskId, A_USER_ID);

    // Invalid Individual Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(0, query.taskId(INVALID_TASK_ID).count());
    assertEquals(0, query.type(INVALID_IDENTITY_LINK_TYPE).count());
    assertEquals(0, query.userId(INVALID_USER_ID).count());
    assertEquals(0, query.groupId(INVALID_GROUP_ID).count());
    assertEquals(0, query.assignerId(INVALID_ASSIGNER_ID).count());
    assertEquals(0, query.operationType(INVALID_HISTORY_EVENT_TYPE).count());
    assertEquals(0, query.processDefinitionId(INVALID_PROCESS_DEFINITION_ID).count());
    assertEquals(0, query.processDefinitionKey(INVALID_PROCESS_DEFINITION_KEY).count());
  }

  /**
   * Should add 3 history records of identity link addition at 01-01-2016
   * 00:00.00 Should add 3 history records of identity link deletion at
   * 01-01-2016 12:00.00
   *
   * Should add 3 history records of identity link addition at 01-01-2016
   * 12:30.00 Should add 3 history records of identity link deletion at
   * 01-01-2016 21:00.00
   *
   * Test case: Query the number of added records at different time interval.
   */
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testShouldAddTaskOwnerForAddandDeleteIdentityLinkByTimeStamp() {

    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertEquals(0, historicIdentityLinks.size());

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
    assertEquals(6, query.dateBefore(newYearNoon(20)).count());
    assertEquals(3, query.operationType(IDENTITY_LINK_ADD).count());
    assertEquals(3, query.operationType(IDENTITY_LINK_DELETE).count());

    // Query records with time between 00:01 and 12:00
    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(6, query.dateBefore(newYearNoon(0)).count());
    assertEquals(1, query.dateAfter(newYearMorning(1)).count());
    assertEquals(0, query.operationType(IDENTITY_LINK_ADD).count());
    assertEquals(1, query.operationType(IDENTITY_LINK_DELETE).count());

    // Query records with time after 12:45
    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(1, query.dateAfter(newYearNoon(45)).count());
    assertEquals(0, query.operationType(IDENTITY_LINK_ADD).count());
    assertEquals(1, query.operationType(IDENTITY_LINK_DELETE).count());

    ClockUtil.setCurrentTime(new Date());
  }

  @SuppressWarnings("deprecation")
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testQueryAddAndRemoveIdentityLinksForProcessDefinition() {

    ProcessDefinition latestProcessDef = repositoryService.createProcessDefinitionQuery().processDefinitionKey(PROCESS_DEFINITION_KEY).singleResult();
    assertNotNull(latestProcessDef);
    List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(latestProcessDef.getId());
    assertEquals(0, links.size());

    // Add candiate group with process definition
    repositoryService.addCandidateStarterGroup(latestProcessDef.getId(), GROUP_1);
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertEquals(1, historicIdentityLinks.size());
    // Query test
    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(1, query.processDefinitionId(latestProcessDef.getId()).count());
    assertEquals(1, query.operationType(IDENTITY_LINK_ADD).count());
    assertEquals(1, query.groupId(GROUP_1).count());

    // Add candidate user for process definition
    repositoryService.addCandidateStarterUser(latestProcessDef.getId(), USER_1);
    // Query test
    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(2, query.processDefinitionId(latestProcessDef.getId()).count());
    assertEquals(2, query.processDefinitionKey(latestProcessDef.getKey()).count());
    assertEquals(2, query.operationType(IDENTITY_LINK_ADD).count());
    assertEquals(1, query.userId(USER_1).count());

    // Delete candiate group with process definition
    repositoryService.deleteCandidateStarterGroup(latestProcessDef.getId(), GROUP_1);
    // Query test
    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(3, query.processDefinitionId(latestProcessDef.getId()).count());
    assertEquals(3, query.processDefinitionKey(latestProcessDef.getKey()).count());
    assertEquals(2, query.groupId(GROUP_1).count());
    assertEquals(1, query.operationType(IDENTITY_LINK_DELETE).count());

    // Delete candidate user for process definition
    repositoryService.deleteCandidateStarterUser(latestProcessDef.getId(), USER_1);
    // Query test
    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(4, query.processDefinitionId(latestProcessDef.getId()).count());
    assertEquals(4, query.processDefinitionKey(latestProcessDef.getKey()).count());
    assertEquals(2, query.userId(USER_1).count());
    query = historyService.createHistoricIdentityLinkLogQuery();
    assertEquals(2, query.operationType(IDENTITY_LINK_DELETE).count());
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/OneTaskProcessWithMultipleCandidateUser.bpmn20.xml" })
  @Test
  public void testHistoricIdentityLinkQueryPaging() {
    startProcessInstance(PROCESS_DEFINITION_KEY_MULTIPLE_CANDIDATE_USER);

    HistoricIdentityLinkLogQuery query = historyService.createHistoricIdentityLinkLogQuery();

    assertEquals(4, query.listPage(0, 4).size());
    assertEquals(1, query.listPage(2, 1).size());
    assertEquals(2, query.listPage(1, 2).size());
    assertEquals(3, query.listPage(1, 4).size());
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/runtime/OneTaskProcessWithMultipleCandidateUser.bpmn20.xml" })
  @Test
  public void testHistoricIdentityLinkQuerySorting() {

    // Pre test - Historical identity link is added as part of deployment
    List<HistoricIdentityLinkLog> historicIdentityLinks = historyService.createHistoricIdentityLinkLogQuery().list();
    assertEquals(0, historicIdentityLinks.size());
    startProcessInstance(PROCESS_DEFINITION_KEY_MULTIPLE_CANDIDATE_USER);

    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByAssignerId().asc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByTime().asc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByGroupId().asc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByType().asc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByOperationType().asc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByProcessDefinitionId().asc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByProcessDefinitionKey().asc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByTaskId().asc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByUserId().asc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByTenantId().asc().list().size());
    assertEquals("aUser", historyService.createHistoricIdentityLinkLogQuery().orderByUserId().asc().list().get(0).getUserId());
    assertEquals("dUser", historyService.createHistoricIdentityLinkLogQuery().orderByUserId().asc().list().get(3).getUserId());

    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByAssignerId().desc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByTime().desc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByGroupId().desc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByType().desc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByOperationType().desc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByProcessDefinitionId().desc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByProcessDefinitionKey().desc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByTaskId().desc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByUserId().desc().list().size());
    assertEquals(4, historyService.createHistoricIdentityLinkLogQuery().orderByTenantId().desc().list().size());
    assertEquals("dUser", historyService.createHistoricIdentityLinkLogQuery().orderByUserId().desc().list().get(0).getUserId());
    assertEquals("aUser", historyService.createHistoricIdentityLinkLogQuery().orderByUserId().desc().list().get(3).getUserId());
  }

  public void addUserIdentityLinks(String taskId) {
    for (int userIndex = 1; userIndex <= NUMBER_OF_USERS; userIndex++)
      taskService.addUserIdentityLink(taskId, A_USER_ID + userIndex, IdentityLinkType.ASSIGNEE);
  }

  public void deleteUserIdentityLinks(String taskId) {
    for (int userIndex = 1; userIndex <= NUMBER_OF_USERS; userIndex++)
      taskService.deleteUserIdentityLink(taskId, A_USER_ID + userIndex, IdentityLinkType.ASSIGNEE);
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
