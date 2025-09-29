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
package org.operaton.bpm.engine.test.api.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.TaskQueryImpl;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.impl.mock.Mocks;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.query.Query;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Sebastian Menski
 */
class TaskQueryExpressionTest {

  protected Task testTask;
  protected User testUser;
  protected User anotherUser;
  protected User userWithoutGroups;
  protected Group group1;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurationResource("org/operaton/bpm/engine/test/api/task/task-query-expression-test.operaton.cfg.xml")
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  IdentityService identityService;

  @BeforeEach
  void setUp() {
    group1 = createGroup("group1");
    Group group2 = createGroup("group2");
    Group group3 = createGroup("group3");

    testUser = createUser("user", group1.getId(), group2.getId());
    anotherUser = createUser("anotherUser", group3.getId());
    userWithoutGroups = createUser("userWithoutGroups");

    setTime(1427547759000L);
    testTask = createTestTask("task");
    // shift time to force distinguishable create times
    adjustTime(2 * 60);
    Task anotherTask = createTestTask("anotherTask");
    Task assignedCandidateTask = createTestTask("assignedCandidateTask");

    taskService.setOwner(testTask.getId(), testUser.getId());
    taskService.setAssignee(testTask.getId(), testUser.getId());

    taskService.addCandidateUser(anotherTask.getId(), testUser.getId());
    taskService.addCandidateGroup(anotherTask.getId(), group1.getId());

    taskService.setAssignee(assignedCandidateTask.getId(), testUser.getId());
    taskService.addCandidateUser(assignedCandidateTask.getId(), testUser.getId());
    taskService.addCandidateGroup(assignedCandidateTask.getId(), group1.getId());
  }

  @Test
  void testQueryByAssigneeExpression() {
    assertCount(taskQuery().taskAssigneeExpression("${'" + testUser.getId() + "'}"), 2);
    assertCount(taskQuery().taskAssigneeExpression("${'" + anotherUser.getId() + "'}"), 0);

    setCurrentUser(testUser);
    assertCount(taskQuery().taskAssigneeExpression("${currentUser()}"), 2);

    setCurrentUser(anotherUser);
    assertCount(taskQuery().taskAssigneeExpression("${currentUser()}"), 0);
  }

  @Test
  void testQueryByAssigneeLikeExpression() {
    assertCount(taskQuery().taskAssigneeLikeExpression("${'%" + testUser.getId().substring(2) + "'}"), 2);
    assertCount(taskQuery().taskAssigneeLikeExpression("${'%" + anotherUser.getId().substring(2) + "'}"), 0);

    setCurrentUser(testUser);
    assertCount(taskQuery().taskAssigneeLikeExpression("${'%'.concat(currentUser())}"), 2);

    setCurrentUser(anotherUser);
    assertCount(taskQuery().taskAssigneeLikeExpression("${'%'.concat(currentUser())}"), 0);
  }

  @Test
  void testQueryByOwnerExpression() {
    assertCount(taskQuery().taskOwnerExpression("${'" + testUser.getId() + "'}"), 1);
    assertCount(taskQuery().taskOwnerExpression("${'" + anotherUser.getId() + "'}"), 0);

    setCurrentUser(testUser);
    assertCount(taskQuery().taskOwnerExpression("${currentUser()}"), 1);

    setCurrentUser(anotherUser);
    assertCount(taskQuery().taskOwnerExpression("${currentUser()}"), 0);
  }

  @Test
  void testQueryByInvolvedUserExpression() {
    assertCount(taskQuery().taskInvolvedUserExpression("${'" + testUser.getId() + "'}"), 3);
    assertCount(taskQuery().taskInvolvedUserExpression("${'" + anotherUser.getId() + "'}"), 0);

    setCurrentUser(testUser);
    assertCount(taskQuery().taskInvolvedUserExpression("${currentUser()}"), 3);

    setCurrentUser(anotherUser);
    assertCount(taskQuery().taskInvolvedUserExpression("${currentUser()}"), 0);
  }

  @Test
  void testQueryByCandidateUserExpression() {
    assertCount(taskQuery().taskCandidateUserExpression("${'" + testUser.getId() + "'}"), 1);
    assertCount(taskQuery().taskCandidateUserExpression("${'" + testUser.getId() + "'}").includeAssignedTasks(), 2);
    assertCount(taskQuery().taskCandidateUserExpression("${'" + anotherUser.getId() + "'}"), 0);

    setCurrentUser(testUser);
    assertCount(taskQuery().taskCandidateUserExpression("${currentUser()}"), 1);
    assertCount(taskQuery().taskCandidateUserExpression("${currentUser()}").includeAssignedTasks(), 2);

    setCurrentUser(anotherUser);
    assertCount(taskQuery().taskCandidateUserExpression("${currentUser()}"), 0);
  }

  @Test
  void testQueryByCandidateGroupExpression() {
    assertCount(taskQuery().taskCandidateGroupExpression("${'" + group1.getId() + "'}"), 1);
    assertCount(taskQuery().taskCandidateGroupExpression("${'unknown'}"), 0);

    setCurrentUser(testUser);
    assertCount(taskQuery().taskCandidateGroupExpression("${currentUserGroups()[0]}"), 1);
    assertCount(taskQuery().taskCandidateGroupExpression("${currentUserGroups()[0]}").includeAssignedTasks(), 2);

    setCurrentUser(anotherUser);
    assertCount(taskQuery().taskCandidateGroupExpression("${currentUserGroups()[0]}"), 0);
  }

  @Test
  void testQueryByCandidateGroupsExpression() {
    setCurrentUser(testUser);
    assertCount(taskQuery().taskCandidateGroupInExpression("${currentUserGroups()}"), 1);
    assertCount(taskQuery().taskCandidateGroupInExpression("${currentUserGroups()}").includeAssignedTasks(), 2);

    setCurrentUser(anotherUser);

    assertCount(taskQuery().taskCandidateGroupInExpression("${currentUserGroups()}"), 0);

    setCurrentUser(userWithoutGroups);
    var taskQuery = taskQuery().taskCandidateGroupInExpression("${currentUserGroups()}");
    assertThatThrownBy(taskQuery::count).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByTaskCreatedBeforeExpression() {
    adjustTime(1);

    assertCount(taskQuery().taskCreatedBeforeExpression("${now()}"), 3);

    adjustTime(-5 * 60);

    assertCount(taskQuery().taskCreatedBeforeExpression("${now()}"), 0);

    setTime(testTask.getCreateTime());

    assertCount(taskQuery().taskCreatedBeforeExpression("${dateTime().plusMonths(2)}"), 3);

    assertCount(taskQuery().taskCreatedBeforeExpression("${dateTime().minusYears(1)}"), 0);
  }

  @Test
  void testQueryByTaskCreatedOnExpression() {
    setTime(testTask.getCreateTime());
    assertCount(taskQuery().taskCreatedOnExpression("${now()}"), 1);

    adjustTime(10);

    assertCount(taskQuery().taskCreatedOnExpression("${dateTime().minusSeconds(10)}"), 1);

    assertCount(taskQuery().taskCreatedOnExpression("${now()}"), 0);
  }

  @Test
  void testQueryByTaskCreatedAfterExpression() {
    adjustTime(1);

    assertCount(taskQuery().taskCreatedAfterExpression("${now()}"), 0);

    adjustTime(-5 * 60);

    assertCount(taskQuery().taskCreatedAfterExpression("${now()}"), 3);

    setTime(testTask.getCreateTime());

    assertCount(taskQuery().taskCreatedAfterExpression("${dateTime().plusMonths(2)}"), 0);

    assertCount(taskQuery().taskCreatedAfterExpression("${dateTime().minusYears(1)}"), 3);
  }

  @Test
  void testQueryByTaskUpdatedAfterExpression() {
    adjustTime(1);

    assertCount(taskQuery().taskUpdatedAfterExpression("${now()}"), 0);

    adjustTime(-5 * 60);

    assertCount(taskQuery().taskUpdatedAfterExpression("${now()}"), 3);

    setTime(testTask.getLastUpdated());

    assertCount(taskQuery().taskUpdatedAfterExpression("${dateTime().plusMonths(2)}"), 0);

    assertCount(taskQuery().taskUpdatedAfterExpression("${dateTime().minusYears(1)}"), 3);
  }

  @Test
  void testQueryByDueBeforeExpression() {
    adjustTime(1);

    assertCount(taskQuery().dueBeforeExpression("${now()}"), 3);

    adjustTime(-5 * 60);

    assertCount(taskQuery().dueBeforeExpression("${now()}"), 0);

    setTime(testTask.getCreateTime());

    assertCount(taskQuery().dueBeforeExpression("${dateTime().plusMonths(2)}"), 3);

    assertCount(taskQuery().dueBeforeExpression("${dateTime().minusYears(1)}"), 0);
  }

  @Test
  void testQueryByDueDateExpression() {
    setTime(testTask.getDueDate());
    assertCount(taskQuery().dueDateExpression("${now()}"), 1);

    adjustTime(10);

    assertCount(taskQuery().dueDateExpression("${dateTime().minusSeconds(10)}"), 1);

    assertCount(taskQuery().dueDateExpression("${now()}"), 0);
  }

  @Test
  void testQueryByDueAfterExpression() {
    adjustTime(1);

    assertCount(taskQuery().dueAfterExpression("${now()}"), 0);

    adjustTime(-5 * 60);

    assertCount(taskQuery().dueAfterExpression("${now()}"), 3);

    setTime(testTask.getCreateTime());

    assertCount(taskQuery().dueAfterExpression("${dateTime().plusMonths(2)}"), 0);

    assertCount(taskQuery().dueAfterExpression("${dateTime().minusYears(1)}"), 3);
  }

  @Test
  void testQueryByFollowUpBeforeExpression() {
    adjustTime(1);

    assertCount(taskQuery().followUpBeforeExpression("${now()}"), 3);

    adjustTime(-5 * 60);

    assertCount(taskQuery().followUpBeforeExpression("${now()}"), 0);

    setTime(testTask.getCreateTime());

    assertCount(taskQuery().followUpBeforeExpression("${dateTime().plusMonths(2)}"), 3);

    assertCount(taskQuery().followUpBeforeExpression("${dateTime().minusYears(1)}"), 0);
  }

  @Test
  void testQueryByFollowUpDateExpression() {
    setTime(testTask.getFollowUpDate());
    assertCount(taskQuery().followUpDateExpression("${now()}"), 1);

    adjustTime(10);

    assertCount(taskQuery().followUpDateExpression("${dateTime().minusSeconds(10)}"), 1);

    assertCount(taskQuery().followUpDateExpression("${now()}"), 0);
  }

  @Test
  void testQueryByFollowUpAfterExpression() {
    adjustTime(1);

    assertCount(taskQuery().followUpAfterExpression("${now()}"), 0);

    adjustTime(-5 * 60);

    assertCount(taskQuery().followUpAfterExpression("${now()}"), 3);

    setTime(testTask.getCreateTime());

    assertCount(taskQuery().followUpAfterExpression("${dateTime().plusMonths(2)}"), 0);

    assertCount(taskQuery().followUpAfterExpression("${dateTime().minusYears(1)}"), 3);
  }

  @Test
  void testQueryByProcessInstanceBusinessKeyExpression() {
    // given
    String aBusinessKey = "business key";
    Mocks.register("aBusinessKey", aBusinessKey);

    createBusinessKeyDeployment(aBusinessKey);

    // when
    TaskQuery taskQuery = taskQuery()
      .processInstanceBusinessKeyExpression("${ " + Mocks.getMocks().keySet().toArray()[0] + " }");

    // then
    assertCount(taskQuery, 1);
  }

  @Test
  void testQueryByProcessInstanceBusinessKeyLikeExpression() {
    // given
    String aBusinessKey = "business key";
    Mocks.register("aBusinessKeyLike", "%" + aBusinessKey.substring(5));

    createBusinessKeyDeployment(aBusinessKey);

    // when
    TaskQuery taskQuery = taskQuery()
      .processInstanceBusinessKeyLikeExpression("${ " + Mocks.getMocks().keySet().toArray()[0] + " }");

    // then
    assertCount(taskQuery, 1);
  }

  protected void createBusinessKeyDeployment(String aBusinessKey) {
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcessDefinition")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask()
        .endEvent()
        .done();

   testRule.deploy(modelInstance);

    runtimeService.startProcessInstanceByKey("aProcessDefinition", aBusinessKey);
  }

  @Test
  void testExpressionOverrideQuery() {
    String queryString = "query";
    String expressionString = "expression";
    String testStringExpression = "${'" + expressionString + "'}";

    Date queryDate = new DateTime(now()).minusYears(1).toDate();
    String testDateExpression = "${now()}";

    TaskQueryImpl taskQuery = (TaskQueryImpl) taskQuery()
      .taskAssignee(queryString)
      .taskAssigneeExpression(testStringExpression)
      .taskAssigneeLike(queryString)
      .taskAssigneeLikeExpression(testStringExpression)
      .taskOwnerExpression(queryString)
      .taskOwnerExpression(expressionString)
      .taskInvolvedUser(queryString)
      .taskInvolvedUserExpression(expressionString)
      .taskCreatedBefore(queryDate)
      .taskCreatedBeforeExpression(testDateExpression)
      .taskCreatedOn(queryDate)
      .taskCreatedOnExpression(testDateExpression)
      .taskCreatedAfter(queryDate)
      .taskCreatedAfterExpression(testDateExpression)
      .dueBefore(queryDate)
      .dueBeforeExpression(testDateExpression)
      .dueDate(queryDate)
      .dueDateExpression(testDateExpression)
      .dueAfter(queryDate)
      .dueAfterExpression(testDateExpression)
      .followUpBefore(queryDate)
      .followUpBeforeExpression(testDateExpression)
      .followUpDate(queryDate)
      .followUpDateExpression(testDateExpression)
      .followUpAfter(queryDate)
      .followUpAfterExpression(testDateExpression);

    // execute query so expression will be evaluated
    taskQuery.count();

    assertThat(taskQuery.getAssignee()).isEqualTo(expressionString);
    assertThat(taskQuery.getAssigneeLike()).isEqualTo(expressionString);
    assertThat(taskQuery.getOwner()).isEqualTo(expressionString);
    assertThat(taskQuery.getInvolvedUser()).isEqualTo(expressionString);
    assertThat(taskQuery.getCreateTimeBefore().after(queryDate)).isTrue();
    assertThat(taskQuery.getCreateTime().after(queryDate)).isTrue();
    assertThat(taskQuery.getCreateTimeAfter().after(queryDate)).isTrue();
    assertThat(taskQuery.getDueBefore().after(queryDate)).isTrue();
    assertThat(taskQuery.getDueDate().after(queryDate)).isTrue();
    assertThat(taskQuery.getDueAfter().after(queryDate)).isTrue();
    assertThat(taskQuery.getFollowUpBefore().after(queryDate)).isTrue();
    assertThat(taskQuery.getFollowUpDate().after(queryDate)).isTrue();
    assertThat(taskQuery.getFollowUpAfter().after(queryDate)).isTrue();

    // candidates has to be tested separately because they have to be set exclusively

    taskQuery = (TaskQueryImpl) taskQuery()
      .taskCandidateGroup(queryString)
      .taskCandidateGroupExpression(testStringExpression);

    // execute query so expression will be evaluated
    taskQuery.count();

    assertThat(taskQuery.getCandidateGroup()).isEqualTo(expressionString);

    taskQuery = (TaskQueryImpl) taskQuery()
      .taskCandidateUser(queryString)
      .taskCandidateUserExpression(testStringExpression);

    // execute query so expression will be evaluated
    taskQuery.count();

    assertThat(taskQuery.getCandidateUser()).isEqualTo(expressionString);

    setCurrentUser(testUser);
    List<String> queryList = List.of("query");
    String testGroupsExpression = "${currentUserGroups()}";

    taskQuery = (TaskQueryImpl) taskQuery()
      .taskCandidateGroupIn(queryList)
      .taskCandidateGroupInExpression(testGroupsExpression);

    // execute query so expression will be evaluated
    taskQuery.count();

    assertThat(taskQuery.getCandidateGroups()).hasSize(2);
  }

  @Test
  void testQueryOverrideExpression() {
    String queryString = "query";
    String expressionString = "expression";
    String testStringExpression = "${'" + expressionString + "'}";

    Date queryDate = new DateTime(now()).minusYears(1).toDate();
    String testDateExpression = "${now()}";

    TaskQueryImpl taskQuery = (TaskQueryImpl) taskQuery()
      .taskAssigneeExpression(testStringExpression)
      .taskAssignee(queryString)
      .taskAssigneeLikeExpression(testStringExpression)
      .taskAssigneeLike(queryString)
      .taskOwnerExpression(expressionString)
      .taskOwner(queryString)
      .taskInvolvedUserExpression(expressionString)
      .taskInvolvedUser(queryString)
      .taskCreatedBeforeExpression(testDateExpression)
      .taskCreatedBefore(queryDate)
      .taskCreatedOnExpression(testDateExpression)
      .taskCreatedOn(queryDate)
      .taskCreatedAfterExpression(testDateExpression)
      .taskCreatedAfter(queryDate)
      .taskUpdatedAfterExpression(testDateExpression)
      .taskUpdatedAfter(queryDate)
      .dueBeforeExpression(testDateExpression)
      .dueBefore(queryDate)
      .dueDateExpression(testDateExpression)
      .dueDate(queryDate)
      .dueAfterExpression(testDateExpression)
      .dueAfter(queryDate)
      .followUpBeforeExpression(testDateExpression)
      .followUpBefore(queryDate)
      .followUpDateExpression(testDateExpression)
      .followUpDate(queryDate)
      .followUpAfterExpression(testDateExpression)
      .followUpAfter(queryDate);

    // execute query so expression will be evaluated
    taskQuery.count();

    assertThat(queryString)
      .isEqualTo(taskQuery.getAssignee())
      .isEqualTo(taskQuery.getAssigneeLike())
      .isEqualTo(taskQuery.getOwner())
      .isEqualTo(taskQuery.getInvolvedUser());
    assertThat(taskQuery.getUpdatedAfter()).isEqualTo(queryDate);
    assertThat(taskQuery.getCreateTimeBefore()).isEqualTo(queryDate);
    assertThat(taskQuery.getCreateTime()).isEqualTo(queryDate);
    assertThat(taskQuery.getCreateTimeAfter()).isEqualTo(queryDate);
    assertThat(taskQuery.getDueBefore()).isEqualTo(queryDate);
    assertThat(taskQuery.getDueDate()).isEqualTo(queryDate);
    assertThat(taskQuery.getDueAfter()).isEqualTo(queryDate);
    assertThat(taskQuery.getFollowUpBefore()).isEqualTo(queryDate);
    assertThat(taskQuery.getFollowUpDate()).isEqualTo(queryDate);
    assertThat(taskQuery.getFollowUpAfter()).isEqualTo(queryDate);

    // candidates has to be tested separately because they have to be set exclusively

    taskQuery = (TaskQueryImpl) taskQuery()
      .taskCandidateGroupExpression(testStringExpression)
      .taskCandidateGroup(queryString);

    // execute query so expression will be evaluated
    taskQuery.count();

    assertThat(queryString).isEqualTo(taskQuery.getCandidateGroup());

    taskQuery = (TaskQueryImpl) taskQuery()
      .taskCandidateUserExpression(testStringExpression)
      .taskCandidateUser(queryString);

    // execute query so expression will be evaluated
    taskQuery.count();

    assertThat(queryString).isEqualTo(taskQuery.getCandidateUser());

    setCurrentUser(testUser);
    List<String> queryList = List.of("query");
    String testGroupsExpression = "${currentUserGroups()}";

    taskQuery = (TaskQueryImpl) taskQuery()
      .taskCandidateGroupInExpression(testGroupsExpression)
      .taskCandidateGroupIn(queryList);

    // execute query so expression will be evaluated
    taskQuery.count();

    assertThat(taskQuery.getCandidateGroups()).hasSize(1);
  }

  @Test
  void testQueryOr() {
    // given
    Date date = DateTimeUtil.now().plusDays(2).toDate();

    Task task1 = taskService.newTask();
    task1.setFollowUpDate(date);
    task1.setOwner("Luke Optim");
    task1.setName("taskForOr");
    taskService.saveTask(task1);

    Task task2 = taskService.newTask();
    task2.setDueDate(date);
    task2.setName("taskForOr");
    taskService.saveTask(task2);

    Task task3 = taskService.newTask();
    task3.setAssignee("John Munda");
    task3.setDueDate(date);
    task3.setName("taskForOr");
    taskService.saveTask(task3);

    Task task4 = taskService.newTask();
    task4.setName("taskForOr");
    taskService.saveTask(task4);

    // when
    List<Task> tasks = taskService.createTaskQuery()
      .taskName("taskForOr")
      .or()
        .followUpAfterExpression("${ now() }")
        .taskAssigneeLikeExpression("${ 'John%' }")
      .endOr()
      .or()
        .taskOwnerExpression("${ 'Luke Optim' }")
        .dueAfterExpression("${ now() }")
      .endOr()
      .list();

    // then
    assertThat(tasks).hasSize(2);
  }

  @Test
  void shouldRejectDueDateExpressionAndWithoutDueDateCombination() {
    var taskQuery = taskService.createTaskQuery().dueDateExpression("");
    assertThatThrownBy(taskQuery::withoutDueDate)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage");
  }

  @Test
  void shouldRejectWithoutDueDateAndDueDateExpressionCombination() {
    var taskQuery = taskService.createTaskQuery().withoutDueDate();
    assertThatThrownBy(() -> taskQuery.dueDateExpression(""))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage");
  }

  @Test
  void shouldRejectDueAfterExpressionAndWithoutDueDateCombination() {
    var taskQuery = taskService.createTaskQuery().dueAfterExpression("");
    assertThatThrownBy(taskQuery::withoutDueDate)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage");
  }

  @Test
  void shouldRejectWithoutDueDateAndDueAfterExpressionCombination() {
    var taskQuery = taskService.createTaskQuery().withoutDueDate();
    assertThatThrownBy(() -> taskQuery.dueAfterExpression(""))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage");
  }

  @Test
  void shouldRejectDueBeforeExpressionAndWithoutDueDateCombination() {
    var taskQuery = taskService.createTaskQuery().dueBeforeExpression("");
    assertThatThrownBy(taskQuery::withoutDueDate)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage");
  }

  @Test
  void shouldRejectWithoutDueDateAndDueBeforeExpressionCombination() {
    var taskQuery = taskService.createTaskQuery().withoutDueDate();
    assertThatThrownBy(() -> taskQuery.dueBeforeExpression(""))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage");
  }

  @AfterEach
  void tearDown() {
    Mocks.reset();

    for (Group group : identityService.createGroupQuery().list()) {
      identityService.deleteGroup(group.getId());
    }
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
    for (Task task : taskService.createTaskQuery().list()) {
      if (task.getProcessInstanceId() == null) {
        taskService.deleteTask(task.getId(), true);
      }
    }

    identityService.clearAuthentication();
  }


  protected TaskQuery taskQuery() {
    return taskService.createTaskQuery();
  }

  protected void assertCount(Query query, long count) {
    assertThat(query.count()).isEqualTo(count);
  }

  protected void setCurrentUser(User user) {
    List<Group> groups = identityService.createGroupQuery().groupMember(user.getId()).list();
    List<String> groupIds = new ArrayList<>();
    for (Group group : groups) {
      groupIds.add(group.getId());
    }

    identityService.setAuthentication(user.getId(), groupIds);
  }

  protected Group createGroup(String groupId) {
    Group group = identityService.newGroup(groupId);
    identityService.saveGroup(group);
    return group;
  }

  protected User createUser(String userId, String... groupIds) {
    User user = identityService.newUser(userId);
    identityService.saveUser(user);

    if (groupIds != null) {
      for (String groupId : groupIds) {
        identityService.createMembership(userId, groupId);
      }
    }

    return user;
  }

  protected Task createTestTask(String taskId) {
    Task task = taskService.newTask(taskId);
    task.setDueDate(task.getCreateTime());
    taskService.saveTask(task);
    // perform two saves to also set the lastUpdated property on the TaskEntity
    task.setFollowUpDate(task.getCreateTime());
    taskService.saveTask(task);
    return task;
  }

  protected Date now() {
    return ClockUtil.getCurrentTime();
  }

  protected void setTime(long time) {
    setTime(new Date(time));
  }

  protected void setTime(Date time) {
    ClockUtil.setCurrentTime(time);
  }

  /**
   * Changes the current time about the given amount in seconds.
   *
   * @param amount the amount to adjust the current time
   */
  protected void adjustTime(int amount) {
    long time = now().getTime() + amount * 1000;
    setTime(time);
  }

}
