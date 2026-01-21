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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.form.OperatonFormRef;
import org.operaton.bpm.engine.impl.TaskQueryImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.DelegationState;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.FileValue;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.*;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Joram Barrez
 * @author Frederik Heremans
 * @author Falko Menge
 */
class TaskQueryTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  IdentityService identityService;
  TaskService taskService;
  RuntimeService runtimeService;
  CaseService caseService;
  RepositoryService repositoryService;
  FilterService filterService;
  ManagementService managementService;

  private List<String> taskIds;

  // The range of Oracle's NUMBER field is limited to ~10e+125
  // which is below Double.MAX_VALUE, so we only test with the following
  // max value
  protected static final double MAX_DOUBLE_VALUE = 10E+124;

  @BeforeEach
  void setUp() throws Exception {

    identityService.saveUser(identityService.newUser("kermit"));
    identityService.saveUser(identityService.newUser("gonzo"));
    identityService.saveUser(identityService.newUser("fozzie"));

    identityService.saveGroup(identityService.newGroup("management"));
    identityService.saveGroup(identityService.newGroup("accountancy"));

    identityService.createMembership("kermit", "management");
    identityService.createMembership("kermit", "accountancy");
    identityService.createMembership("fozzie", "management");

    taskIds = generateTestTasks();
  }

  @AfterEach
  void tearDown() {
    identityService.deleteGroup("accountancy");
    identityService.deleteGroup("management");
    identityService.deleteUser("fozzie");
    identityService.deleteUser("gonzo");
    identityService.deleteUser("kermit");
    taskService.deleteTasks(taskIds, true);
  }

  @Test
  void tesBasicTaskPropertiesNotNull() {
    Task task = taskService.createTaskQuery().taskId(taskIds.get(0)).singleResult();
    assertThat(task.getDescription()).isNotNull();
    assertThat(task.getId()).isNotNull();
    assertThat(task.getName()).isNotNull();
    assertThat(task.getCreateTime()).isNotNull();
  }

  @Test
  void testQueryNoCriteria() {
    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isEqualTo(12);
    assertThat(query.list()).hasSize(12);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByTaskId() {
    String taskId = taskIds.get(0);
    TaskQuery query = taskService.createTaskQuery().taskId(taskId);
    assertThat(query.singleResult()).isNotNull();
    assertThat(query.count()).isOne();
    List<Task> foundTasks = query.list();
    assertThat(foundTasks).hasSize(1);
    List<String> foundTaskIds = foundTasks.stream().map(Task::getId).toList();
    assertThat(foundTaskIds).containsOnly(taskId);
  }

  @Test
  void testQueryByTaskIdIn() {
    String task0Id = taskIds.get(0);
    String task1Id = taskIds.get(1);
    TaskQuery query = taskService.createTaskQuery().taskIdIn(task0Id, task1Id);
    assertThat(query.count()).isEqualTo(2);
    List<Task> foundTasks = query.list();
    assertThat(foundTasks).hasSize(2);
    List<String> foundTaskIds = foundTasks.stream().map(Task::getId).toList();
    assertThat(foundTaskIds).containsOnly(task0Id, task1Id);
  }

  @Test
  void testQueryByInvalidTaskId() {
    TaskQuery query = taskService.createTaskQuery().taskId("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
    var taskQuery = taskService.createTaskQuery();

    assertThatThrownBy(() -> taskQuery.taskId(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByName() {
    TaskQuery query = taskService.createTaskQuery().taskName("testTask");
    assertThat(query.list()).hasSize(6);
    assertThat(query.count()).isEqualTo(6);

    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByInvalidName() {
    TaskQuery query = taskService.createTaskQuery().taskName("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
    var taskQuery = taskService.createTaskQuery().taskName(null);

    assertThatThrownBy(taskQuery::singleResult).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByNameLike() {
    TaskQuery query = taskService.createTaskQuery().taskNameLike("gonzo\\_%");
    assertThat(query.singleResult()).isNotNull();
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByInvalidNameLike() {
    TaskQuery query = taskService.createTaskQuery().taskName("1");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
    var taskQuery = taskService.createTaskQuery().taskName(null);

    assertThatThrownBy(taskQuery::singleResult).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByDescription() {
    TaskQuery query = taskService.createTaskQuery().taskDescription("testTask description");
    assertThat(query.list()).hasSize(6);
    assertThat(query.count()).isEqualTo(6);

    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByInvalidDescription() {
    TaskQuery query = taskService.createTaskQuery().taskDescription("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
    var taskQuery = taskService.createTaskQuery();

    assertThatThrownBy(() -> taskQuery.taskDescription(null)).isInstanceOf(ProcessEngineException.class);
  }


  /**
   * CAM-6363
   * <p>
   * Verify that search by name returns case-insensitive results
   * </p>
   */
  @Test
  void testTaskQueryLookupByNameCaseInsensitive() {
    TaskQuery query = taskService.createTaskQuery();
    query.taskName("testTask");


    List<Task> tasks = query.list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(6);

    query = taskService.createTaskQuery();
    query.taskName("TeStTaSk");

    tasks = query.list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(6);
  }

  /**
   * CAM-6165
   * <p>
   * Verify that search by name like returns case-insensitive results
   * </p>
   */
  @Test
  void testTaskQueryLookupByNameLikeCaseInsensitive() {
    TaskQuery query = taskService.createTaskQuery();
    query.taskNameLike("%task%");


    List<Task> tasks = query.list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(10);

    query = taskService.createTaskQuery();
    query.taskNameLike("%Task%");

    tasks = query.list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(10);
  }

  @Test
  void testQueryByDescriptionLike() {
    TaskQuery query = taskService.createTaskQuery().taskDescriptionLike("%gonzo\\_%");
    assertThat(query.singleResult()).isNotNull();
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByInvalidDescriptionLike() {
    TaskQuery query = taskService.createTaskQuery().taskDescriptionLike("invalid");
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();
    var taskQuery = taskService.createTaskQuery();

    assertThatThrownBy(() -> taskQuery.taskDescriptionLike(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByPriority() {
    TaskQuery query = taskService.createTaskQuery().taskPriority(10);
    assertThat(query.list()).hasSize(2);
    assertThat(query.count()).isEqualTo(2);

    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    query = taskService.createTaskQuery().taskPriority(100);
    assertThat(query.singleResult()).isNull();
    assertThat(query.list()).isEmpty();
    assertThat(query.count()).isZero();

    query = taskService.createTaskQuery().taskMinPriority(50);
    assertThat(query.list()).hasSize(3);

    query = taskService.createTaskQuery().taskMinPriority(10);
    assertThat(query.list()).hasSize(5);

    query = taskService.createTaskQuery().taskMaxPriority(10);
    assertThat(query.list()).hasSize(9);

    query = taskService.createTaskQuery().taskMaxPriority(3);
    assertThat(query.list()).hasSize(6);

    query = taskService.createTaskQuery().taskMinPriority(50).taskMaxPriority(10);
    assertThat(query.list()).isEmpty();

    query = taskService.createTaskQuery().taskPriority(30).taskMaxPriority(10);
    assertThat(query.list()).isEmpty();

    query = taskService.createTaskQuery().taskMinPriority(30).taskPriority(10);
    assertThat(query.list()).isEmpty();

    query = taskService.createTaskQuery().taskMinPriority(30).taskPriority(20).taskMaxPriority(10);
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testQueryByInvalidPriority() {
    var taskQuery = taskService.createTaskQuery();
    assertThatThrownBy(() -> taskQuery.taskPriority(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByAssignee() {
    TaskQuery query = taskService.createTaskQuery().taskAssignee("gonzo_");
    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
    assertThat(query.singleResult()).isNotNull();

    query = taskService.createTaskQuery().taskAssignee("kermit");
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    assertThat(query.singleResult()).isNull();
  }

  @Test
  void testQueryByAssigneeLike() {
    TaskQuery query = taskService.createTaskQuery().taskAssigneeLike("gonz%\\_");
    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
    assertThat(query.singleResult()).isNotNull();

    query = taskService.createTaskQuery().taskAssignee("gonz");
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    assertThat(query.singleResult()).isNull();
  }

  @Test
  void testQueryByNullAssignee() {
    var taskQuery = taskService.createTaskQuery();
    assertThatThrownBy(() -> taskQuery.taskAssignee(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByAssigneeInPositive() {
    // given
    String[] assignees = {"fozzie", "john", "mary"};

    // when
    TaskQuery query = taskService.createTaskQuery().taskAssigneeIn(assignees);

    // then
    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
  }

  @Test
  void testQueryByAssigneeInNegative() {
    // given
    String[] assignees = {"kermit", "gonzo"};

    // when
    TaskQuery query = taskService.createTaskQuery().taskAssigneeIn(assignees);

    // then
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testQueryByAssigneeAndAssigneeIn() {
    // given
    String assignee = "fozzie";
    String[] assignees = {"fozzie", "john", "mary"};

    // when
    TaskQuery query = taskService.createTaskQuery()
      .taskAssignee(assignee).taskAssigneeIn(assignees);

    // then
    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
  }

  @Test
  void testQueryByAssigneeInNull() {
    // given
    String[] assignees = null;

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    try {
      query.taskAssigneeIn(assignees);
      fail("Exception expected");
    } catch (Exception ex) {
      assertThat(ex.getMessage()).isEqualTo("Assignees is null");
    }
  }

  @Test
  void testQueryByAssigneeNotInPositive() {
    // given
    String[] assignees = {"fozzie", "john", "mary"};

    // when
    TaskQuery query = taskService.createTaskQuery().taskAssigneeNotIn(assignees);

    // then
    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
  }

  @Test
  void testQueryByAssigneeNotInNegative() {
    // given
    String[] assignees = {"gonzo_", "fozzie"};

    // when
    TaskQuery query = taskService.createTaskQuery().taskAssigneeNotIn(assignees);

    // then
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testQueryByAssigneeInAndAssigneeNotIn() {
    // given
    String[] assigneesIn = {"fozzie", "gonzo"};
    String[] assigneesNotIn = {"john", "mary"};

    // when
    TaskQuery query = taskService.createTaskQuery()
            .taskAssigneeIn(assigneesIn).taskAssigneeNotIn(assigneesNotIn);

    // then
    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
  }

  @Test
  void testQueryByAssigneeNotInNull() {
    // given
    String[] assignees = null;

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    try {
        query.taskAssigneeNotIn(assignees);
      fail("Exception expected");
    } catch (Exception ex) {
      assertThat(ex.getMessage()).isEqualTo("Assignees is null");
    }
  }

  @Test
  void testQueryByUnassigned() {
    TaskQuery query = taskService.createTaskQuery().taskUnassigned();
    assertThat(query.count()).isEqualTo(10);
    assertThat(query.list()).hasSize(10);
  }

  @Test
  void testQueryByAssigned() {
    TaskQuery query = taskService.createTaskQuery().taskAssigned();
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);
  }

  @Test
  void testQueryByCandidateUser() {
    // kermit is candidate for 12 tasks, two of them are already assigned
    TaskQuery query = taskService.createTaskQuery().taskCandidateUser("kermit");
    assertThat(query.count()).isEqualTo(10);
    assertThat(query.list()).hasSize(10);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test including assigned tasks
    query = taskService.createTaskQuery().taskCandidateUser("kermit").includeAssignedTasks();
    assertThat(query.count()).isEqualTo(12);
    assertThat(query.list()).hasSize(12);

    // fozzie is candidate for one task and her groups are candidate for 2 tasks, one of them is already assigned
    query = taskService.createTaskQuery().taskCandidateUser("fozzie");
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test including assigned tasks
    query = taskService.createTaskQuery().taskCandidateUser("fozzie").includeAssignedTasks();
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);

    // gonzo is candidate for one task, which is already assigned
    query = taskService.createTaskQuery().taskCandidateUser("gonzo");
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

    // test including assigned tasks
    query = taskService.createTaskQuery().taskCandidateUser("gonzo").includeAssignedTasks();
    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
  }

  @Test
  void testQueryByNullCandidateUser() {
    var taskQuery = taskService.createTaskQuery();
    assertThatThrownBy(() -> taskQuery.taskCandidateUser(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByIncludeAssignedTasksWithMissingCandidateUserOrGroup() {
    var taskQuery = taskService.createTaskQuery();
    assertThatThrownBy(taskQuery::includeAssignedTasks).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByIncludeAssignedTasksWithoutMissingCandidateUserOrGroup() {
    // We expect no exceptions when the there is at least 1 candidate user or group present
    try {
      taskService.createTaskQuery().taskCandidateUser("user").includeAssignedTasks();
    } catch (ProcessEngineException e) {
      fail("We expect no exceptions when a taskCandidateUser is present");
    }

    try {
      taskService.createTaskQuery().taskCandidateGroupLike("%group%").includeAssignedTasks();
    } catch (ProcessEngineException e) {
      fail("We expect no exceptions when a candidateGroupLike is present");
    }

    try {
      taskService.createTaskQuery().taskCandidateGroupIn(List.of("group")).includeAssignedTasks();
    } catch (ProcessEngineException e) {
      fail("We expect no exceptions when a taskCandidateGroupIn is present");
    }

    try {
      taskService.createTaskQuery().withCandidateGroups().includeAssignedTasks();
    } catch (ProcessEngineException e) {
      fail("We expect no exceptions when a withCandidateGroups is present");
    }

    try {
      taskService.createTaskQuery().withoutCandidateGroups().includeAssignedTasks();
    } catch (ProcessEngineException e) {
      fail("We expect no exceptions when a withoutCandidateGroups is present");
    }

    try {
      taskService.createTaskQuery().withCandidateUsers().includeAssignedTasks();
    } catch (ProcessEngineException e) {
      fail("We expect no exceptions when a withCandidateUsers is present");
    }

    try {
      taskService.createTaskQuery().withoutCandidateUsers().includeAssignedTasks();
    } catch (ProcessEngineException e) {
      fail("We expect no exceptions when a withoutCandidateUsers is present");
    }

    try {
      taskService.createTaskQuery().taskCandidateUserExpression("expression").includeAssignedTasks();
    } catch (ProcessEngineException e) {
      fail("We expect no exceptions when a taskCandidateUserExpression is present");
    }

    try {
      taskService.createTaskQuery().taskCandidateGroupExpression("expression").includeAssignedTasks();
    } catch (ProcessEngineException e) {
      fail("We expect no exceptions when a taskCandidateGroupExpression is present");
    }

    try {
      taskService.createTaskQuery().taskCandidateGroupInExpression("expression").includeAssignedTasks();
    } catch (ProcessEngineException e) {
      fail("We expect no exceptions when a taskCandidateGroupInExpression is present");
    }
  }

  @Test
  void testQueryByCandidateGroup() {
    // management group is candidate for 3 tasks, one of them is already assigned
    TaskQuery query = taskService.createTaskQuery().taskCandidateGroup("management");
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test including assigned tasks
    query = taskService.createTaskQuery().taskCandidateGroup("management").includeAssignedTasks();
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);


    // accountancy group is candidate for 3 tasks, one of them is already assigned
    query = taskService.createTaskQuery().taskCandidateGroup("accountancy");
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);

    // test including assigned tasks
    query = taskService.createTaskQuery().taskCandidateGroup("accountancy").includeAssignedTasks();
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);

    // sales group is candidate for no tasks
    query = taskService.createTaskQuery().taskCandidateGroup("sales");
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

    // test including assigned tasks
    query = taskService.createTaskQuery().taskCandidateGroup("sales").includeAssignedTasks();
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testQueryByCandidateGroupLike() {
    // management group is candidate for 3 tasks, one of them is already assigned
    TaskQuery query = taskService.createTaskQuery().taskCandidateGroupLike("management");
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test with "shortened" group name for like query
    query = taskService.createTaskQuery().taskCandidateGroupLike("mana%");
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test with "shortened" group name for like query (different part)
    query = taskService.createTaskQuery().taskCandidateGroupLike("%ment");
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test management candidates group with assigned tasks included
    query = taskService.createTaskQuery().taskCandidateGroupLike("management").includeAssignedTasks();
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test with "shortened" group name for like query (assigned tasks included)
    query = taskService.createTaskQuery().taskCandidateGroupLike("mana%").includeAssignedTasks();
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test with "shortened" group name for like query (different part, assigned tasks included)
    query = taskService.createTaskQuery().taskCandidateGroupLike("%ment").includeAssignedTasks();
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test query that matches tasks with the "management" the "accountancy" candidate groups
    // accountancy group is candidate for 3 tasks, one of them is already assigned
    query = taskService.createTaskQuery().taskCandidateGroupLike("%an%");
    assertThat(query.count()).isEqualTo(4);
    assertThat(query.list()).hasSize(4);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test query that matches tasks with the "management" the "accountancy" candidate groups (assigned tasks included)
    query = taskService.createTaskQuery().taskCandidateGroupLike("%an%").includeAssignedTasks();
    assertThat(query.count()).isEqualTo(5);
    assertThat(query.list()).hasSize(5);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByCandidateGroupLikeInsideAnOr() {
    // management group is candidate for 3 tasks, one of them is already assigned
    TaskQuery query = taskService.createTaskQuery().or().taskCandidateGroupLike("management").taskId("non-existing").endOr();
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test with "shortened" group name for like query
    query = taskService.createTaskQuery().or().taskCandidateGroupLike("mana%").taskId("non-existing").endOr();
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test with "shortened" group name for like query (different part)
    query = taskService.createTaskQuery().or().taskCandidateGroupLike("%ment").taskId("non-existing").endOr();
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test management candidates group with assigned tasks included
    query = taskService.createTaskQuery().or().taskCandidateGroupLike("management").includeAssignedTasks().taskId("non-existing").endOr();
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test with "shortened" group name for like query (assigned tasks included)
    query = taskService.createTaskQuery().or().taskCandidateGroupLike("mana%").includeAssignedTasks().taskId("non-existing").endOr();
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test with "shortened" group name for like query (different part, assigned tasks included)
    query = taskService.createTaskQuery().or().taskCandidateGroupLike("%ment").includeAssignedTasks().taskId("non-existing").endOr();
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test query that matches tasks with the "management" the "accountancy" candidate groups
    // accountancy group is candidate for 3 tasks, one of them is already assigned
    query = taskService.createTaskQuery().or().taskCandidateGroupLike("%an%").taskId("non-existing").endOr();
    assertThat(query.count()).isEqualTo(4);
    assertThat(query.list()).hasSize(4);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test query that matches tasks with the "management" the "accountancy" candidate groups (assigned tasks included)
    query = taskService.createTaskQuery().or().taskCandidateGroupLike("%an%").includeAssignedTasks().taskId("non-existing").endOr();
    assertThat(query.count()).isEqualTo(5);
    assertThat(query.list()).hasSize(5);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryWithCandidateGroups() {
    // test withCandidateGroups
    TaskQuery query = taskService.createTaskQuery().withCandidateGroups();
    assertThat(query.count()).isEqualTo(4);
    assertThat(query.list()).hasSize(4);

    assertThat(query.includeAssignedTasks().count()).isEqualTo(5);
    assertThat(query.includeAssignedTasks().list()).hasSize(5);
  }

  @Test
  void testQueryWithoutCandidateGroups() {
    // test withoutCandidateGroups
    TaskQuery query = taskService.createTaskQuery().withoutCandidateGroups();
    assertThat(query.count()).isEqualTo(6);
    assertThat(query.list()).hasSize(6);

    assertThat(query.includeAssignedTasks().count()).isEqualTo(7);
    assertThat(query.includeAssignedTasks().list()).hasSize(7);
  }

  @Test
  void shouldQueryWithoutCandidateGroupsAndUsers() {
    // given
    Task task = taskService.newTask();
    taskService.saveTask(task);
    taskIds.add(task.getId());

    // when
    TaskQuery query = taskService.createTaskQuery()
        .withoutCandidateGroups()
        .withoutCandidateUsers();

    // then
    assertThat(query.list()).extracting("id").containsExactly(task.getId());
  }

  @Test
  void testQueryByNullCandidateGroup() {
    var taskQuery = taskService.createTaskQuery();
    assertThatThrownBy(() -> taskQuery.taskCandidateGroup(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByCandidateGroupIn() {
    List<String> groups = List.of("management", "accountancy");
    TaskQuery query = taskService.createTaskQuery().taskCandidateGroupIn(groups);
    assertThat(query.count()).isEqualTo(4);
    assertThat(query.list()).hasSize(4);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test including assigned tasks
    query = taskService.createTaskQuery().taskCandidateGroupIn(groups).includeAssignedTasks();
    assertThat(query.count()).isEqualTo(5);
    assertThat(query.list()).hasSize(5);

    // Unexisting groups or groups that don't have candidate tasks shouldn't influence other results
    groups = List.of("management", "accountancy", "sales", "unexisting");
    query = taskService.createTaskQuery().taskCandidateGroupIn(groups);
    assertThat(query.count()).isEqualTo(4);
    assertThat(query.list()).hasSize(4);

    // test including assigned tasks
    query = taskService.createTaskQuery().taskCandidateGroupIn(groups).includeAssignedTasks();
    assertThat(query.count()).isEqualTo(5);
    assertThat(query.list()).hasSize(5);
  }

  @Test
  void testQueryByCandidateGroupInAndCandidateGroup() {
    List<String> groups = List.of("management", "accountancy");
    String candidateGroup = "management";
    TaskQuery query = taskService.createTaskQuery().taskCandidateGroupIn(groups).taskCandidateGroup(candidateGroup);
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test including assigned tasks
    query = taskService.createTaskQuery().taskCandidateGroupIn(groups).taskCandidateGroup(candidateGroup).includeAssignedTasks();
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);

    // Unexisting groups or groups that don't have candidate tasks shouldn't influence other results
    groups = List.of("management", "accountancy", "sales", "unexisting");
    query = taskService.createTaskQuery().taskCandidateGroupIn(groups).taskCandidateGroup(candidateGroup);
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.list()).hasSize(2);
    assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class);

    // test including assigned tasks
    query = taskService.createTaskQuery().taskCandidateGroupIn(groups).taskCandidateGroup(candidateGroup).includeAssignedTasks();
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);

    // sales group is candidate for no tasks
    query = taskService.createTaskQuery().taskCandidateGroupIn(groups).taskCandidateGroup("sales");
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

    // test including assigned tasks
    query = taskService.createTaskQuery().taskCandidateGroupIn(groups).taskCandidateGroup("sales").includeAssignedTasks();
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testQueryByCandidateGroupInAndCandidateGroupNotIntersected() {
    List<String> groups = List.of("accountancy");
    String candidateGroup = "management";
    TaskQuery query = taskService.createTaskQuery().taskCandidateGroupIn(groups).taskCandidateGroup(candidateGroup);
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testQueryByNullCandidateGroupIn() {
    var taskQuery = taskService.createTaskQuery();
    assertThatThrownBy(() -> taskQuery.taskCandidateGroupIn(null)).isInstanceOf(ProcessEngineException.class);
    List<String> emptyGroupIds = emptyList();
    assertThatThrownBy(() -> taskQuery.taskCandidateGroupIn(emptyGroupIds)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByDelegationState() {
    TaskQuery query = taskService.createTaskQuery().taskDelegationState(null);
    assertThat(query.count()).isEqualTo(12);
    assertThat(query.list()).hasSize(12);
    query = taskService.createTaskQuery().taskDelegationState(DelegationState.PENDING);
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    query = taskService.createTaskQuery().taskDelegationState(DelegationState.RESOLVED);
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

    String taskId= taskService.createTaskQuery().taskAssignee("gonzo_").singleResult().getId();
    taskService.delegateTask(taskId, "kermit");

    query = taskService.createTaskQuery().taskDelegationState(null);
    assertThat(query.count()).isEqualTo(11);
    assertThat(query.list()).hasSize(11);
    query = taskService.createTaskQuery().taskDelegationState(DelegationState.PENDING);
    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
    query = taskService.createTaskQuery().taskDelegationState(DelegationState.RESOLVED);
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

    taskService.resolveTask(taskId);

    query = taskService.createTaskQuery().taskDelegationState(null);
    assertThat(query.count()).isEqualTo(11);
    assertThat(query.list()).hasSize(11);
    query = taskService.createTaskQuery().taskDelegationState(DelegationState.PENDING);
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
    query = taskService.createTaskQuery().taskDelegationState(DelegationState.RESOLVED);
    assertThat(query.count()).isOne();
    assertThat(query.list()).hasSize(1);
  }

  @Test
  void testQueryCreatedOn() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

    // Exact matching of createTime, should result in 6 tasks
    Date createTime = sdf.parse("01/01/2001 01:01:01.000");

    TaskQuery query = taskService.createTaskQuery().taskCreatedOn(createTime);
    assertThat(query.count()).isEqualTo(6);
    assertThat(query.list()).hasSize(6);
  }

  @Test
  void testQueryCreatedBefore() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

    // Should result in 7 tasks
    Date before = sdf.parse("03/02/2002 02:02:02.000");

    TaskQuery query = taskService.createTaskQuery().taskCreatedBefore(before);
    assertThat(query.count()).isEqualTo(7);
    assertThat(query.list()).hasSize(7);

    before = sdf.parse("01/01/2001 01:01:01.000");
    query = taskService.createTaskQuery().taskCreatedBefore(before);
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testQueryCreatedAfter() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

    // Should result in 3 tasks
    Date after = sdf.parse("03/03/2003 03:03:03.000");

    TaskQuery query = taskService.createTaskQuery().taskCreatedAfter(after);
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.list()).hasSize(3);

    after = sdf.parse("05/05/2005 05:05:05.000");
    query = taskService.createTaskQuery().taskCreatedAfter(after);
    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Test
  void testCreateTimeCombinations() throws Exception {
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

    // Exact matching of createTime, should result in 6 tasks
    Date createTime = sdf.parse("01/01/2001 01:01:01.000");

    Date oneHourAgo = new Date(createTime.getTime() - 60 * 60 * 1000);
    Date oneHourLater = new Date(createTime.getTime() + 60 * 60 * 1000);

    assertThat(taskService.createTaskQuery()
        .taskCreatedAfter(oneHourAgo).taskCreatedOn(createTime).taskCreatedBefore(oneHourLater).count()).isEqualTo(6);
    assertThat(taskService.createTaskQuery()
        .taskCreatedAfter(oneHourLater).taskCreatedOn(createTime).taskCreatedBefore(oneHourAgo).count()).isZero();
    assertThat(taskService.createTaskQuery()
        .taskCreatedAfter(oneHourLater).taskCreatedOn(createTime).count()).isZero();
    assertThat(taskService.createTaskQuery()
        .taskCreatedOn(createTime).taskCreatedBefore(oneHourAgo).count()).isZero();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
  @Test
  void testTaskDefinitionKey() {

    // Start process instance, 2 tasks will be available
    runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

    // 1 task should exist with key "taskKey_1"
    List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("taskKey_1").list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(1);

    assertThat(tasks.get(0).getTaskDefinitionKey()).isEqualTo("taskKey_1");

    // No task should be found with unexisting key
    long count = taskService.createTaskQuery().taskDefinitionKey("unexistingKey").count();
    assertThat(count).isZero();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
  @Test
  void testTaskDefinitionKeyLike() {

    // Start process instance, 2 tasks will be available
    runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

    // Ends with matching, TaskKey_1 and TaskKey_123 match
    List<Task> tasks = taskService.createTaskQuery().taskDefinitionKeyLike("taskKey\\_1%").orderByTaskName().asc().list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(2);

    assertThat(tasks.get(0).getTaskDefinitionKey()).isEqualTo("taskKey_1");
    assertThat(tasks.get(1).getTaskDefinitionKey()).isEqualTo("taskKey_123");

    // Starts with matching, TaskKey_123 matches
    tasks = taskService.createTaskQuery().taskDefinitionKeyLike("%\\_123").orderByTaskName().asc().list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(1);

    assertThat(tasks.get(0).getTaskDefinitionKey()).isEqualTo("taskKey_123");

    // Contains matching, TaskKey_123 matches
    tasks = taskService.createTaskQuery().taskDefinitionKeyLike("%Key\\_12%").orderByTaskName().asc().list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(1);

    assertThat(tasks.get(0).getTaskDefinitionKey()).isEqualTo("taskKey_123");


    // No task should be found with unexisting key
    long count = taskService.createTaskQuery().taskDefinitionKeyLike("%unexistingKey%").count();
    assertThat(count).isZero();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
  @Test
  void testTaskDefinitionKeyIn() {

    // Start process instance, 2 tasks will be available
    runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

    // 1 Task should be found with TaskKey1
    List<Task> tasks = taskService.createTaskQuery().taskDefinitionKeyIn("taskKey_1").list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(1);

    assertThat(tasks.get(0).getTaskDefinitionKey()).isEqualTo("taskKey_1");

    // 2 Tasks should be found with TaskKey_1 and TaskKey_123
    tasks = taskService.createTaskQuery().taskDefinitionKeyIn("taskKey_1", "taskKey_123").orderByTaskName().asc().list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(2);

    assertThat(tasks.get(0).getTaskDefinitionKey()).isEqualTo("taskKey_1");
    assertThat(tasks.get(1).getTaskDefinitionKey()).isEqualTo("taskKey_123");

    // 2 Tasks should be found with TaskKey1, TaskKey123 and UnexistingKey
    tasks = taskService.createTaskQuery().taskDefinitionKeyIn("taskKey_1", "taskKey_123", "unexistingKey").orderByTaskName().asc().list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(2);

    assertThat(tasks.get(0).getTaskDefinitionKey()).isEqualTo("taskKey_1");
    assertThat(tasks.get(1).getTaskDefinitionKey()).isEqualTo("taskKey_123");

    // No task should be found with UnexistingKey
    long count = taskService.createTaskQuery().taskDefinitionKeyIn("unexistingKey").count();
    assertThat(count).isZero();

    count = taskService.createTaskQuery().taskDefinitionKey("unexistingKey").taskDefinitionKeyIn("taskKey1").count();
    assertThat(count).isZero();
  }

  @Test
  @Deployment(resources="org/operaton/bpm/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
  void testTaskDefinitionKeyNotInNoKeysProvided() {

    // Given
    // Start process instance, 2 tasks will be available with:
    // - process definition key "taskDefinitionKeyProcess"
    // - task definition keys "taskKey_1" & "taskKey_123"
    runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

    // When
    var tasks = taskService.createTaskQuery()
            .processDefinitionKey("taskDefinitionKeyProcess")
            .taskDefinitionKeyNotIn()
            .list();
    // Then
    assertThat(tasks)
            .extracting(Task::getTaskDefinitionKey)
            .containsExactly("taskKey_1", "taskKey_123");
  }

  @Test
  @Deployment(resources="org/operaton/bpm/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
  void testTaskDefinitionKeyNotInOneKeyProvided() {

    // Given
    // Start process instance, 2 tasks will be available with:
    // - process definition key "taskDefinitionKeyProcess"
    // - task definition keys "taskKey_1" & "taskKey_123"
    runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

    // When
    var tasks = taskService.createTaskQuery()
            .processDefinitionKey("taskDefinitionKeyProcess")
            .taskDefinitionKeyNotIn("taskKey_1")
            .list();
    // Then
    assertThat(tasks)
            .extracting(Task::getTaskDefinitionKey)
            .containsExactly("taskKey_123");
  }

  @Test
  @Deployment(resources="org/operaton/bpm/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
  void testTaskDefinitionKeyNotInAllKeysProvided() {

    // Given
    // Start process instance, 2 tasks will be available with:
    // - process definition key "taskDefinitionKeyProcess"
    // - task definition keys "taskKey_1" & "taskKey_123"
    runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

    // When
    var tasks = taskService.createTaskQuery()
            .processDefinitionKey("taskDefinitionKeyProcess")
            .taskDefinitionKeyNotIn("taskKey_1", "taskKey_123")
            .list();
    // Then
    assertThat(tasks)
            .isEmpty();
  }

  @Test
  @Deployment(resources="org/operaton/bpm/engine/test/api/task/taskDefinitionProcess.bpmn20.xml")
  void testTaskDefinitionKeyNotInInvalidKeyProvided() {

    // Given
    // Start process instance, 2 tasks will be available with:
    // - process definition key "taskDefinitionKeyProcess"
    // - task definition keys "taskKey_1" & "taskKey_123"
    runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");

    // When
    var tasks = taskService.createTaskQuery()
            .processDefinitionKey("taskDefinitionKeyProcess")
            .taskDefinitionKeyNotIn("I do not exist", "I don't exist either")
            .list();
    // Then
    assertThat(tasks)
            .extracting(Task::getTaskDefinitionKey)
            .containsExactly("taskKey_1", "taskKey_123");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testTaskVariableNameEqualsIgnoreCase() {
    String variableName = "someVariable";
    String variableValue = "someCamelCaseValue";

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.setVariableLocal(task.getId(), variableName, variableValue);

    // query for case-insensitive variable name should only return a result if case-insensitive search is used
    assertThat(taskService.createTaskQuery().matchVariableNamesIgnoreCase().taskVariableValueEquals(variableName.toLowerCase(), variableValue).count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals(variableName.toLowerCase(), variableValue).count()).isZero();

    // query should treat all variables case-insensitively, even when flag is set after variable
    assertThat(taskService.createTaskQuery().taskVariableValueEquals(variableName.toLowerCase(), variableValue).matchVariableNamesIgnoreCase().count()).isOne();
  }

  @Deployment
  @Test
  void testTaskVariableValueEquals() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // No task should be found for an unexisting var
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("unexistingVar", "value").count()).isZero();

    // Create a map with a variable for all default types
    Map<String, Object> variables = new HashMap<>();
    variables.put("longVar", 928374L);
    variables.put("shortVar", (short) 123);
    variables.put("integerVar", 1234);
    variables.put("stringVar", "stringValue");
    variables.put("booleanVar", true);
    Date date = Calendar.getInstance().getTime();
    variables.put("dateVar", date);
    variables.put("nullVar", null);

    taskService.setVariablesLocal(task.getId(), variables);

    // Test query matches
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("longVar", 928374L).count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("shortVar", (short) 123).count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("integerVar", 1234).count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("stringVar", "stringValue").count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("booleanVar", true).count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("dateVar", date).count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("nullVar", null).count()).isOne();

    // Test query for other values on existing variables
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("longVar", 999L).count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("shortVar", (short) 999).count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("integerVar", 999).count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("stringVar", "999").count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("booleanVar", false).count()).isZero();
    Calendar otherDate = Calendar.getInstance();
    otherDate.add(Calendar.YEAR, 1);
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("dateVar", otherDate.getTime()).count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("nullVar", "999").count()).isZero();

    // Test query for not equals
    assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("longVar", 999L).count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("shortVar", (short) 999).count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("integerVar", 999).count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("stringVar", "999").count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueNotEquals("booleanVar", false).count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testTaskVariableValueEquals.bpmn20.xml")
  @Test
  void testTaskVariableValueEqualsIgnoreCase() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    String variableName = "someVariable";
    String variableValue = "someCamelCaseValue";

    taskService.setVariableLocal(task.getId(), variableName, variableValue);

    // query for existing variable should return one result
    assertThat(taskService.createTaskQuery().taskVariableValueEquals(variableName, variableValue).count()).isOne();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueEquals(variableName, variableValue.toLowerCase()).count()).isOne();

    // query for non existing variable should return zero results
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("nonExistentVariable", variableValue.toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueEquals("nonExistentVariable", variableValue.toLowerCase()).count()).isZero();

    // query for existing variable with different value should return zero results
    assertThat(taskService.createTaskQuery().taskVariableValueEquals(variableName, "nonExistentValue").count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueEquals(variableName, "nonExistentValue".toLowerCase()).count()).isZero();

    // query for case-insensitive variable value should only return a result when case-insensitive search is used
    assertThat(taskService.createTaskQuery().taskVariableValueEquals(variableName, variableValue.toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueEquals(variableName, variableValue.toLowerCase()).count()).isOne();

    // query for case-insensitive variable with not equals operator should only return a result when case-sensitive search is used
    assertThat(taskService.createTaskQuery().taskVariableValueNotEquals(variableName, variableValue.toLowerCase()).count()).isOne();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueNotEquals(variableName, variableValue.toLowerCase()).count()).isZero();

    // query should treat all variables case-insensitively, even when flag is set after variable
    assertThat(taskService.createTaskQuery().taskVariableValueEquals(variableName, variableValue.toLowerCase()).matchVariableValuesIgnoreCase().count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testTaskVariableValueNameEqualsIgnoreCase() {
    String variableName = "someVariable";
    String variableValue = "someCamelCaseValue";

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.setVariableLocal(task.getId(), variableName, variableValue);

    // query for case-insensitive variable name should only return a result if case-insensitive search is used
    assertThat(taskService.createTaskQuery().matchVariableNamesIgnoreCase().matchVariableValuesIgnoreCase().taskVariableValueEquals(variableName.toLowerCase(), variableValue.toLowerCase()).count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals(variableName.toLowerCase(), variableValue).count()).isZero();

    // query should treat all variables case-insensitively, even when flag is set after variable
    assertThat(taskService.createTaskQuery().taskVariableValueEquals(variableName.toLowerCase(), variableValue).matchVariableNamesIgnoreCase().count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testTaskVariableValueEquals.bpmn20.xml")
  @Test
  void testTaskVariableValueLike() {

  	ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
  	Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

  	Map<String, Object> variables = new HashMap<>();
  	variables.put("stringVar", "stringValue");

  	taskService.setVariablesLocal(task.getId(), variables);

    assertThat(taskService.createTaskQuery().taskVariableValueLike("stringVar", "stringVal%").count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueLike("stringVar", "%ngValue").count()).isOne();
    assertThat(taskService.createTaskQuery().taskVariableValueLike("stringVar", "%ngVal%").count()).isOne();

    assertThat(taskService.createTaskQuery().taskVariableValueLike("stringVar", "stringVar%").count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueLike("stringVar", "%ngVar").count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueLike("stringVar", "%ngVar%").count()).isZero();

    assertThat(taskService.createTaskQuery().taskVariableValueLike("stringVar", "stringVal").count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueLike("nonExistingVar", "string%").count()).isZero();
    var taskQuery = taskService.createTaskQuery();

    // test with null value
    assertThatThrownBy(() -> taskQuery.taskVariableValueLike("stringVar", null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testTaskVariableValueEquals.bpmn20.xml")
  @Test
  void testTaskVariableValueLikeIgnoreCase() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "stringValue");

    taskService.setVariablesLocal(task.getId(), variables);

    assertThat(taskService.createTaskQuery().taskVariableValueLike("stringVar", "stringVal%".toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueLike("stringVar", "stringVal%".toLowerCase()).count()).isOne();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueLike("stringVar", "%ngValue".toLowerCase()).count()).isOne();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueLike("stringVar", "%ngVal%".toLowerCase()).count()).isOne();

    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueLike("stringVar", "stringVar%".toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueLike("stringVar", "%ngVar".toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueLike("stringVar", "%ngVar%".toLowerCase()).count()).isZero();

    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueLike("stringVar", "stringVal".toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().taskVariableValueLike("nonExistingVar", "stringVal%".toLowerCase()).count()).isZero();
    var taskQuery = taskService.createTaskQuery();

    // test with null value
    assertThatThrownBy(() -> taskQuery.taskVariableValueLike("stringVar", null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testTaskVariableValueEquals.bpmn20.xml")
  @Test
  void testTaskVariableValueCompare() {

  	ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
  	Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    var taskQuery = taskService.createTaskQuery();

  	Map<String, Object> variables = new HashMap<>();
  	variables.put("numericVar", 928374);
  	Date date = new GregorianCalendar(2014, 2, 2, 2, 2, 2).getTime();
  	variables.put("dateVar", date);
  	variables.put("stringVar", "ab");
  	variables.put("nullVar", null);

  	taskService.setVariablesLocal(task.getId(), variables);

    // test compare methods with numeric values
    assertThat(taskQuery.taskVariableValueGreaterThan("numericVar", 928373).count()).isOne();
    assertThat(taskQuery.taskVariableValueGreaterThan("numericVar", 928374).count()).isZero();
    assertThat(taskQuery.taskVariableValueGreaterThan("numericVar", 928375).count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.taskVariableValueGreaterThanOrEquals("numericVar", 928373).count()).isOne();
    assertThat(taskQuery.taskVariableValueGreaterThanOrEquals("numericVar", 928374).count()).isOne();
    assertThat(taskQuery.taskVariableValueGreaterThanOrEquals("numericVar", 928375).count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.taskVariableValueLessThan("numericVar", 928375).count()).isOne();
    assertThat(taskQuery.taskVariableValueLessThan("numericVar", 928374).count()).isZero();
    assertThat(taskQuery.taskVariableValueLessThan("numericVar", 928373).count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.taskVariableValueLessThanOrEquals("numericVar", 928375).count()).isOne();
    assertThat(taskQuery.taskVariableValueLessThanOrEquals("numericVar", 928374).count()).isOne();
    assertThat(taskQuery.taskVariableValueLessThanOrEquals("numericVar", 928373).count()).isZero();

    // test compare methods with date values
    Date before = new GregorianCalendar(2014, 2, 2, 2, 2, 1).getTime();
    Date after = new GregorianCalendar(2014, 2, 2, 2, 2, 3).getTime();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.taskVariableValueGreaterThan("dateVar", before).count()).isOne();
    assertThat(taskQuery.taskVariableValueGreaterThan("dateVar", date).count()).isZero();
    assertThat(taskQuery.taskVariableValueGreaterThan("dateVar", after).count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.taskVariableValueGreaterThanOrEquals("dateVar", before).count()).isOne();
    assertThat(taskQuery.taskVariableValueGreaterThanOrEquals("dateVar", date).count()).isOne();
    assertThat(taskQuery.taskVariableValueGreaterThanOrEquals("dateVar", after).count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.taskVariableValueLessThan("dateVar", after).count()).isOne();
    assertThat(taskQuery.taskVariableValueLessThan("dateVar", date).count()).isZero();
    assertThat(taskQuery.taskVariableValueLessThan("dateVar", before).count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.taskVariableValueLessThanOrEquals("dateVar", after).count()).isOne();
    assertThat(taskQuery.taskVariableValueLessThanOrEquals("dateVar", date).count()).isOne();
    assertThat(taskQuery.taskVariableValueLessThanOrEquals("dateVar", before).count()).isZero();

    //test with string values
    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.taskVariableValueGreaterThan("stringVar", "aa").count()).isOne();
    assertThat(taskQuery.taskVariableValueGreaterThan("stringVar", "ab").count()).isZero();
    assertThat(taskQuery.taskVariableValueGreaterThan("stringVar", "ba").count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.taskVariableValueGreaterThanOrEquals("stringVar", "aa").count()).isOne();
    assertThat(taskQuery.taskVariableValueGreaterThanOrEquals("stringVar", "ab").count()).isOne();
    assertThat(taskQuery.taskVariableValueGreaterThanOrEquals("stringVar", "ba").count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.taskVariableValueLessThan("stringVar", "ba").count()).isOne();
    assertThat(taskQuery.taskVariableValueLessThan("stringVar", "ab").count()).isZero();
    assertThat(taskQuery.taskVariableValueLessThan("stringVar", "aa").count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.taskVariableValueLessThanOrEquals("stringVar", "ba").count()).isOne();
    assertThat(taskQuery.taskVariableValueLessThanOrEquals("stringVar", "ab").count()).isOne();
    assertThat(taskQuery.taskVariableValueLessThanOrEquals("stringVar", "aa").count()).isZero();

    var taskQuery2 = taskService.createTaskQuery();
    // test with null value
    assertThatThrownBy(() -> taskQuery2.taskVariableValueGreaterThan("nullVar", null)).isInstanceOf(ProcessEngineException.class);
    assertThatThrownBy(() -> taskQuery2.taskVariableValueGreaterThanOrEquals("nullVar", null)).isInstanceOf(ProcessEngineException.class);
    assertThatThrownBy(() -> taskQuery2.taskVariableValueLessThan("nullVar", null)).isInstanceOf(ProcessEngineException.class);
    assertThatThrownBy(() -> taskQuery2.taskVariableValueLessThanOrEquals("nullVar", null)).isInstanceOf(ProcessEngineException.class);

    // test with boolean value
    assertThatThrownBy(() -> taskQuery2.taskVariableValueGreaterThan("nullVar", true)).isInstanceOf(ProcessEngineException.class);
    assertThatThrownBy(() -> taskQuery2.taskVariableValueGreaterThanOrEquals("nullVar", false)).isInstanceOf(ProcessEngineException.class);
    assertThatThrownBy(() -> taskQuery2.taskVariableValueLessThan("nullVar", true)).isInstanceOf(ProcessEngineException.class);
    assertThatThrownBy(() -> taskQuery2.taskVariableValueLessThanOrEquals("nullVar", false)).isInstanceOf(ProcessEngineException.class);

    // test non existing variable
    assertThat(taskQuery.taskVariableValueLessThanOrEquals("nonExisting", 123).count()).isZero();
  }

  @Deployment
  @Test
  void testProcessVariableValueEquals() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("longVar", 928374L);
    variables.put("shortVar", (short) 123);
    variables.put("integerVar", 1234);
    variables.put("stringVar", "stringValue");
    variables.put("booleanVar", true);
    Date date = Calendar.getInstance().getTime();
    variables.put("dateVar", date);
    variables.put("nullVar", null);

    // Start process-instance with all types of variables
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // Test query matches
    assertThat(taskService.createTaskQuery().processVariableValueEquals("longVar", 928374L).count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueEquals("shortVar", (short) 123).count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueEquals("integerVar", 1234).count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueEquals("stringVar", "stringValue").count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueEquals("booleanVar", true).count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueEquals("dateVar", date).count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueEquals("nullVar", null).count()).isOne();

    // Test query for other values on existing variables
    assertThat(taskService.createTaskQuery().processVariableValueEquals("longVar", 999L).count()).isZero();
    assertThat(taskService.createTaskQuery().processVariableValueEquals("shortVar", (short) 999).count()).isZero();
    assertThat(taskService.createTaskQuery().processVariableValueEquals("integerVar", 999).count()).isZero();
    assertThat(taskService.createTaskQuery().processVariableValueEquals("stringVar", "999").count()).isZero();
    assertThat(taskService.createTaskQuery().processVariableValueEquals("booleanVar", false).count()).isZero();
    Calendar otherDate = Calendar.getInstance();
    otherDate.add(Calendar.YEAR, 1);
    assertThat(taskService.createTaskQuery().processVariableValueEquals("dateVar", otherDate.getTime()).count()).isZero();
    assertThat(taskService.createTaskQuery().processVariableValueEquals("nullVar", "999").count()).isZero();

    // Test querying for task variables don't match the process-variables
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("longVar", 928374L).count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("shortVar", (short) 123).count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("integerVar", 1234).count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("stringVar", "stringValue").count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("booleanVar", true).count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("dateVar", date).count()).isZero();
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("nullVar", null).count()).isZero();

    // Test querying for task variables not equals
    assertThat(taskService.createTaskQuery().processVariableValueNotEquals("longVar", 999L).count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueNotEquals("shortVar", (short) 999).count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueNotEquals("integerVar", 999).count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueNotEquals("stringVar", "999").count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueNotEquals("booleanVar", false).count()).isOne();

    // and query for the existing variable with NOT should result in nothing found:
    assertThat(taskService.createTaskQuery().processVariableValueNotEquals("longVar", 928374L).count()).isZero();

    // Test combination of task-variable and process-variable
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    taskService.setVariableLocal(task.getId(), "taskVar", "theValue");
    taskService.setVariableLocal(task.getId(), "longVar", 928374L);

    assertThat(taskService.createTaskQuery()
      .processVariableValueEquals("longVar", 928374L)
      .taskVariableValueEquals("taskVar", "theValue")
      .count()).isOne();

    assertThat(taskService.createTaskQuery()
      .processVariableValueEquals("longVar", 928374L)
      .taskVariableValueEquals("longVar", 928374L)
      .count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableNameEqualsIgnoreCase() {
    String variableName = "someVariable";
    String variableValue = "someCamelCaseValue";
    Map<String, Object> variables = new HashMap<>();
    variables.put(variableName, variableValue);

    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // query for case-insensitive variable name should only return a result if case-insensitive search is used
    assertThat(taskService.createTaskQuery().matchVariableNamesIgnoreCase().processVariableValueEquals(variableName.toLowerCase(), variableValue).count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueEquals(variableName.toLowerCase(), variableValue).count()).isZero();

    // query should treat all variables case-insensitively, even when flag is set after variable
    assertThat(taskService.createTaskQuery().processVariableValueEquals(variableName.toLowerCase(), variableValue).matchVariableNamesIgnoreCase().count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testTaskVariableValueEquals.bpmn20.xml")
  @Test
  void testProcessVariableValueEqualsIgnoreCase() {
    String variableName = "someVariable";
    String variableValue = "someCamelCaseValue";
    Map<String, Object> variables = new HashMap<>();
    variables.put(variableName, variableValue);

    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // query for existing variable should return one result
    assertThat(taskService.createTaskQuery().processVariableValueEquals(variableName, variableValue).count()).isOne();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueEquals(variableName, variableValue.toLowerCase()).count()).isOne();

    // query for non existing variable should return zero results
    assertThat(taskService.createTaskQuery().processVariableValueEquals("nonExistentVariable", variableValue.toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueEquals("nonExistentVariable", variableValue.toLowerCase()).count()).isZero();

    // query for existing variable with different value should return zero results
    assertThat(taskService.createTaskQuery().processVariableValueEquals(variableName, "nonExistentValue").count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueEquals(variableName, "nonExistentValue".toLowerCase()).count()).isZero();

    // query for case-insensitive variable value should only return a result when case-insensitive search is used
    assertThat(taskService.createTaskQuery().processVariableValueEquals(variableName, variableValue.toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueEquals(variableName, variableValue.toLowerCase()).count()).isOne();

    // query for case-insensitive variable with not equals operator should only return a result when case-sensitive search is used
    assertThat(taskService.createTaskQuery().processVariableValueNotEquals(variableName, variableValue.toLowerCase()).count()).isOne();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotEquals(variableName, variableValue.toLowerCase()).count()).isZero();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessVariableValueEquals.bpmn20.xml")
  @Test
  void testProcessVariableValueLike() {

    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "stringValue");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    assertThat(taskService.createTaskQuery().processVariableValueLike("stringVar", "stringVal%").count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueLike("stringVar", "%ngValue").count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueLike("stringVar", "%ngVal%").count()).isOne();

    assertThat(taskService.createTaskQuery().processVariableValueLike("stringVar", "stringVar%").count()).isZero();
    assertThat(taskService.createTaskQuery().processVariableValueLike("stringVar", "%ngVar").count()).isZero();
    assertThat(taskService.createTaskQuery().processVariableValueLike("stringVar", "%ngVar%").count()).isZero();

    assertThat(taskService.createTaskQuery().processVariableValueLike("stringVar", "stringVal").count()).isZero();
    assertThat(taskService.createTaskQuery().processVariableValueLike("nonExistingVar", "string%").count()).isZero();
    var taskQuery = taskService.createTaskQuery();

    // test with null value
    assertThatThrownBy(() -> taskQuery.processVariableValueLike("stringVar", null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessVariableValueEquals.bpmn20.xml")
  @Test
  void testProcessVariableValueLikeIgnoreCase() {

    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "stringValue");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    assertThat(taskService.createTaskQuery().processVariableValueLike("stringVar", "stringVal%".toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueLike("stringVar", "stringVal%".toLowerCase()).count()).isOne();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueLike("stringVar", "%ngValue".toLowerCase()).count()).isOne();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueLike("stringVar", "%ngVal%".toLowerCase()).count()).isOne();

    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueLike("stringVar", "stringVar%".toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueLike("stringVar", "%ngVar".toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueLike("stringVar", "%ngVar%".toLowerCase()).count()).isZero();

    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueLike("stringVar", "stringVal".toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueLike("nonExistingVar", "stringVal%".toLowerCase()).count()).isZero();
    var taskQuery = taskService.createTaskQuery().matchVariableValuesIgnoreCase();

    // test with null value
    assertThatThrownBy(() -> taskQuery.processVariableValueLike("stringVar", null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessVariableValueEquals.bpmn20.xml")
  @Test
  void testProcessVariableValueNotLike() {

    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "stringValue");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    var taskQuery1 = taskService.createTaskQuery();
    assertThat(taskQuery1.processVariableValueNotLike("stringVar", "stringVal%").count()).isZero();
    assertThat(taskQuery1.processVariableValueNotLike("stringVar", "%ngValue").count()).isZero();
    assertThat(taskQuery1.processVariableValueNotLike("stringVar", "%ngVal%").count()).isZero();

    var taskQuery2 = taskService.createTaskQuery();
    assertThat(taskQuery2.processVariableValueNotLike("stringVar", "stringVar%").count()).isOne();
    assertThat(taskQuery2.processVariableValueNotLike("stringVar", "%ngVar").count()).isOne();
    assertThat(taskQuery2.processVariableValueNotLike("stringVar", "%ngVar%").count()).isOne();

    var taskQuery3 = taskService.createTaskQuery();
    assertThat(taskQuery3.processVariableValueNotLike("stringVar", "stringVal").count()).isOne();
    assertThat(taskQuery3.processVariableValueNotLike("nonExistingVar", "string%").count()).isZero();

    // test with null value
    var taskQuery4 = taskService.createTaskQuery();
    assertThatThrownBy(() -> taskQuery4.processVariableValueNotLike("stringVar", null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessVariableValueEquals.bpmn20.xml")
  @Test
  void testProcessVariableValueNotLikeIgnoreCase() {

    Map<String, Object> variables = new HashMap<>();
    variables.put("stringVar", "stringValue");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    assertThat(taskService.createTaskQuery().processVariableValueNotLike("stringVar", "stringVal%".toLowerCase()).count()).isOne();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "stringVal%".toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "%ngValue".toLowerCase()).count()).isZero();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "%ngVal%".toLowerCase()).count()).isZero();

    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "stringVar%".toLowerCase()).count()).isOne();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "%ngVar".toLowerCase()).count()).isOne();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "%ngVar%".toLowerCase()).count()).isOne();

    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("stringVar", "stringVal".toLowerCase()).count()).isOne();
    assertThat(taskService.createTaskQuery().matchVariableValuesIgnoreCase().processVariableValueNotLike("nonExistingVar", "stringVal%".toLowerCase()).count()).isZero();

    // test with null value
    var taskQuery = taskService.createTaskQuery().matchVariableValuesIgnoreCase();
    assertThatThrownBy(() -> taskQuery.processVariableValueNotLike("stringVar", null))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessVariableValueEquals.bpmn20.xml")
  @Test
  void testProcessVariableValueCompare() {

  	Map<String, Object> variables = new HashMap<>();
  	variables.put("numericVar", 928374);
  	Date date = new GregorianCalendar(2014, 2, 2, 2, 2, 2).getTime();
  	variables.put("dateVar", date);
  	variables.put("stringVar", "ab");
  	variables.put("nullVar", null);

    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    // test compare methods with numeric values
    var taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.processVariableValueGreaterThan("numericVar", 928373).count()).isOne();
    assertThat(taskQuery.processVariableValueGreaterThan("numericVar", 928374).count()).isZero();
    assertThat(taskQuery.processVariableValueGreaterThan("numericVar", 928375).count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.processVariableValueGreaterThanOrEquals("numericVar", 928373).count()).isOne();
    assertThat(taskQuery.processVariableValueGreaterThanOrEquals("numericVar", 928374).count()).isOne();
    assertThat(taskQuery.processVariableValueGreaterThanOrEquals("numericVar", 928375).count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.processVariableValueLessThan("numericVar", 928375).count()).isOne();
    assertThat(taskQuery.processVariableValueLessThan("numericVar", 928374).count()).isZero();
    assertThat(taskQuery.processVariableValueLessThan("numericVar", 928373).count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.processVariableValueLessThanOrEquals("numericVar", 928375).count()).isOne();
    assertThat(taskQuery.processVariableValueLessThanOrEquals("numericVar", 928374).count()).isOne();
    assertThat(taskQuery.processVariableValueLessThanOrEquals("numericVar", 928373).count()).isZero();

    // test compare methods with date values
    Date before = new GregorianCalendar(2014, 2, 2, 2, 2, 1).getTime();
    Date after = new GregorianCalendar(2014, 2, 2, 2, 2, 3).getTime();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.processVariableValueGreaterThan("dateVar", before).count()).isOne();
    assertThat(taskQuery.processVariableValueGreaterThan("dateVar", date).count()).isZero();
    assertThat(taskQuery.processVariableValueGreaterThan("dateVar", after).count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.processVariableValueGreaterThanOrEquals("dateVar", before).count()).isOne();
    assertThat(taskQuery.processVariableValueGreaterThanOrEquals("dateVar", date).count()).isOne();
    assertThat(taskQuery.processVariableValueGreaterThanOrEquals("dateVar", after).count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.processVariableValueLessThan("dateVar", after).count()).isOne();
    assertThat(taskQuery.processVariableValueLessThan("dateVar", date).count()).isZero();
    assertThat(taskQuery.processVariableValueLessThan("dateVar", before).count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.processVariableValueLessThanOrEquals("dateVar", after).count()).isOne();
    assertThat(taskQuery.processVariableValueLessThanOrEquals("dateVar", date).count()).isOne();
    assertThat(taskQuery.processVariableValueLessThanOrEquals("dateVar", before).count()).isZero();

    //test with string values
    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.processVariableValueGreaterThan("stringVar", "aa").count()).isOne();
    assertThat(taskQuery.processVariableValueGreaterThan("stringVar", "ab").count()).isZero();
    assertThat(taskQuery.processVariableValueGreaterThan("stringVar", "ba").count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.processVariableValueGreaterThanOrEquals("stringVar", "aa").count()).isOne();
    assertThat(taskQuery.processVariableValueGreaterThanOrEquals("stringVar", "ab").count()).isOne();
    assertThat(taskQuery.processVariableValueGreaterThanOrEquals("stringVar", "ba").count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.processVariableValueLessThan("stringVar", "ba").count()).isOne();
    assertThat(taskQuery.processVariableValueLessThan("stringVar", "ab").count()).isZero();
    assertThat(taskQuery.processVariableValueLessThan("stringVar", "aa").count()).isZero();

    taskQuery = taskService.createTaskQuery();
    assertThat(taskQuery.processVariableValueLessThanOrEquals("stringVar", "ba").count()).isOne();
    assertThat(taskQuery.processVariableValueLessThanOrEquals("stringVar", "ab").count()).isOne();
    assertThat(taskQuery.processVariableValueLessThanOrEquals("stringVar", "aa").count()).isZero();

    var taskQuery2 = taskService.createTaskQuery();
    // test with null value
    assertThatThrownBy(() -> taskQuery2.processVariableValueGreaterThan("nullVar", null)).isInstanceOf(ProcessEngineException.class);
    assertThatThrownBy(() -> taskQuery2.processVariableValueGreaterThanOrEquals("nullVar", null)).isInstanceOf(ProcessEngineException.class);
    assertThatThrownBy(() -> taskQuery2.processVariableValueLessThan("nullVar", null)).isInstanceOf(ProcessEngineException.class);
    assertThatThrownBy(() -> taskQuery2.processVariableValueLessThanOrEquals("nullVar", null)).isInstanceOf(ProcessEngineException.class);

    // test with boolean value
    assertThatThrownBy(() -> taskQuery2.processVariableValueGreaterThan("nullVar", true)).isInstanceOf(ProcessEngineException.class);
    assertThatThrownBy(() -> taskQuery2.processVariableValueGreaterThanOrEquals("nullVar", false)).isInstanceOf(ProcessEngineException.class);
    assertThatThrownBy(() -> taskQuery2.processVariableValueLessThan("nullVar", true)).isInstanceOf(ProcessEngineException.class);
    assertThatThrownBy(() -> taskQuery2.processVariableValueLessThanOrEquals("nullVar", false)).isInstanceOf(ProcessEngineException.class);

    // test non existing variable
    assertThat(taskQuery.processVariableValueLessThanOrEquals("nonExisting", 123).count()).isZero();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueEqualsNumber() {
    // long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 123L));

    // non-matching long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 12345L));

    // short
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", (short) 123));

    // double
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 123.0d));

    // integer
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 123));

    // untyped null (should not match)
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", null));

    // typed null (should not match)
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", Variables.longValue(null)));

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "123"));

    assertThat(taskService.createTaskQuery().processVariableValueEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(taskService.createTaskQuery().processVariableValueEquals("var", Variables.numberValue(123L)).count()).isEqualTo(4);
    assertThat(taskService.createTaskQuery().processVariableValueEquals("var", Variables.numberValue(123.0d)).count()).isEqualTo(4);
    assertThat(taskService.createTaskQuery().processVariableValueEquals("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);

    assertThat(taskService.createTaskQuery().processVariableValueEquals("var", Variables.numberValue(null)).count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testProcessVariableValueNumberComparison() {
    // long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 123L));

    // non-matching long
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 12345L));

    // short
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", (short) 123));

    // double
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 123.0d));

    // integer
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 123));

    // untyped null
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", null));

    // typed null
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", Variables.longValue(null)));

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "123"));

    assertThat(taskService.createTaskQuery().processVariableValueNotEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(taskService.createTaskQuery().processVariableValueGreaterThan("var", Variables.numberValue(123)).count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueGreaterThanOrEquals("var", Variables.numberValue(123)).count()).isEqualTo(5);
    assertThat(taskService.createTaskQuery().processVariableValueLessThan("var", Variables.numberValue(123)).count()).isZero();
    assertThat(taskService.createTaskQuery().processVariableValueLessThanOrEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testTaskVariableValueEqualsNumber() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    List<Task> tasks = taskService.createTaskQuery().processDefinitionKey("oneTaskProcess").list();
    assertThat(tasks).hasSize(8);
    taskService.setVariableLocal(tasks.get(0).getId(), "var", 123L);
    taskService.setVariableLocal(tasks.get(1).getId(), "var", 12345L);
    taskService.setVariableLocal(tasks.get(2).getId(), "var", (short) 123);
    taskService.setVariableLocal(tasks.get(3).getId(), "var", 123.0d);
    taskService.setVariableLocal(tasks.get(4).getId(), "var", 123);
    taskService.setVariableLocal(tasks.get(5).getId(), "var", null);
    taskService.setVariableLocal(tasks.get(6).getId(), "var", Variables.longValue(null));
    taskService.setVariableLocal(tasks.get(7).getId(), "var", "123");

    assertThat(taskService.createTaskQuery().taskVariableValueEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("var", Variables.numberValue(123L)).count()).isEqualTo(4);
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("var", Variables.numberValue(123.0d)).count()).isEqualTo(4);
    assertThat(taskService.createTaskQuery().taskVariableValueEquals("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);

    assertThat(taskService.createTaskQuery().taskVariableValueEquals("var", Variables.numberValue(null)).count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testVariableEqualsNumberMax() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", MAX_DOUBLE_VALUE));
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", Long.MAX_VALUE));

    assertThat(taskService.createTaskQuery().processVariableValueEquals("var", Variables.numberValue(MAX_DOUBLE_VALUE)).count()).isOne();
    assertThat(taskService.createTaskQuery().processVariableValueEquals("var", Variables.numberValue(Long.MAX_VALUE)).count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testVariableEqualsNumberLongValueOverflow() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", MAX_DOUBLE_VALUE));

    // this results in an overflow
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", (long) MAX_DOUBLE_VALUE));

    // the query should not find the long variable
    assertThat(taskService.createTaskQuery().processVariableValueEquals("var", Variables.numberValue(MAX_DOUBLE_VALUE)).count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  void testVariableEqualsNumberNonIntegerDoubleShouldNotMatchInteger() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", 42).putValue("var2", 52.4d));

    // querying by 42.4 should not match the integer variable 42
    assertThat(taskService.createTaskQuery().processVariableValueEquals("var", Variables.numberValue(42.4d)).count()).isZero();

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 42.4d));

    // querying by 52 should not find the double variable 52.4
    assertThat(taskService.createTaskQuery().processVariableValueEquals("var", Variables.numberValue(52)).count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testProcessDefinitionId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    List<Task> tasks = taskService.createTaskQuery().processDefinitionId(processInstance.getProcessDefinitionId()).list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(processInstance.getId());

    assertThat(taskService.createTaskQuery().processDefinitionId("unexisting").count()).isZero();
  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testProcessDefinitionKey() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    List<Task> tasks = taskService.createTaskQuery().processDefinitionKey("oneTaskProcess").list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(processInstance.getId());

    assertThat(taskService.createTaskQuery().processDefinitionKey("unexisting").count()).isZero();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/task/taskDefinitionProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testProcessDefinitionKeyIn() {

    // Start for each deployed process definition a process instance
    runtimeService.startProcessInstanceByKey("taskDefinitionKeyProcess");
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // 1 task should be found with oneTaskProcess
    List<Task> tasks = taskService.createTaskQuery().processDefinitionKeyIn("oneTaskProcess").list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(1);

    assertThat(tasks.get(0).getTaskDefinitionKey()).isEqualTo("theTask");

    // 2 Tasks should be found with both process definition keys
    tasks = taskService.createTaskQuery()
      .processDefinitionKeyIn("oneTaskProcess", "taskDefinitionKeyProcess")
      .list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(3);

    Set<String> keysFound = new HashSet<>();
    for (Task task : tasks) {
      keysFound.add(task.getTaskDefinitionKey());
    }
    assertThat(keysFound).containsExactlyInAnyOrder("taskKey_123", "theTask", "taskKey_1");

    // 1 Tasks should be found with oneTaskProcess,and NonExistingKey
    tasks = taskService.createTaskQuery().processDefinitionKeyIn("oneTaskProcess", "NonExistingKey").orderByTaskName().asc().list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(1);

    assertThat(tasks.get(0).getTaskDefinitionKey()).isEqualTo("theTask");

    // No task should be found with NonExistingKey
    long count = taskService.createTaskQuery().processDefinitionKeyIn("NonExistingKey").count();
    assertThat(count).isZero();

    count = taskService.createTaskQuery()
        .processDefinitionKeyIn("oneTaskProcess").processDefinitionKey("NonExistingKey").count();
    assertThat(count).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testProcessDefinitionName() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    List<Task> tasks = taskService.createTaskQuery().processDefinitionName("The%One%Task%Process").list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(processInstance.getId());

    assertThat(taskService.createTaskQuery().processDefinitionName("unexisting").count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testProcessDefinitionNameLike() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    List<Task> tasks = taskService.createTaskQuery().processDefinitionNameLike("The\\%One\\%Task%").list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(processInstance.getId());

    assertThat(taskService.createTaskQuery().processDefinitionNameLike("The One Task").count()).isZero();
    assertThat(taskService.createTaskQuery().processDefinitionNameLike("The Other Task%").count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testProcessInstanceBusinessKey() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess", "BUSINESS-KEY-1");

    assertThat(taskService.createTaskQuery().processDefinitionName("The%One%Task%Process").processInstanceBusinessKey("BUSINESS-KEY-1").list()).hasSize(1);
    assertThat(taskService.createTaskQuery().processInstanceBusinessKey("BUSINESS-KEY-1").list()).hasSize(1);
    assertThat(taskService.createTaskQuery().processInstanceBusinessKey("NON-EXISTING").count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testProcessInstanceBusinessKeyIn() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess", "BUSINESS-KEY-1");
    runtimeService.startProcessInstanceByKey("oneTaskProcess", "BUSINESS-KEY-2");

    // 1 task should be found with BUSINESS-KEY-1
    List<Task> tasks = taskService.createTaskQuery().processInstanceBusinessKeyIn("BUSINESS-KEY-1").list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(1);

    assertThat(tasks.get(0).getTaskDefinitionKey()).isEqualTo("theTask");

    // 2 tasks should be found with BUSINESS-KEY-1 and BUSINESS-KEY-2
    tasks = taskService.createTaskQuery()
      .processInstanceBusinessKeyIn("BUSINESS-KEY-1", "BUSINESS-KEY-2")
      .list();
    assertThat(tasks)
            .isNotNull()
            .hasSize(2);

    for (Task task : tasks) {
      assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");
    }

    // 1 tasks should be found with BUSINESS-KEY-1 and NON-EXISTING-KEY
    Task task = taskService.createTaskQuery()
      .processInstanceBusinessKeyIn("BUSINESS-KEY-1", "NON-EXISTING-KEY")
      .singleResult();

    assertThat(tasks).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("theTask");

    long count = taskService.createTaskQuery().processInstanceBusinessKeyIn("BUSINESS-KEY-1").processInstanceBusinessKey("NON-EXISTING-KEY")
        .count();
    assertThat(count).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testProcessInstanceBusinessKeyLike() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess", "BUSINESS-KEY-1");

    assertThat(taskService.createTaskQuery().processDefinitionName("The%One%Task%Process").processInstanceBusinessKey("BUSINESS-KEY-1").list()).hasSize(1);
    assertThat(taskService.createTaskQuery().processInstanceBusinessKeyLike("BUSINESS-KEY%").list()).hasSize(1);
    assertThat(taskService.createTaskQuery().processInstanceBusinessKeyLike("BUSINESS-KEY").count()).isZero();
    assertThat(taskService.createTaskQuery().processInstanceBusinessKeyLike("BUZINESS-KEY%").count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testTaskDueDate() throws Exception {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // Set due-date on task
    Date dueDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 01:12:13");
    task.setDueDate(dueDate);
    taskService.saveTask(task);

    assertThat(taskService.createTaskQuery().dueDate(dueDate).count()).isOne();

    Calendar otherDate = Calendar.getInstance();
    otherDate.add(Calendar.YEAR, 1);
    assertThat(taskService.createTaskQuery().dueDate(otherDate.getTime()).count()).isZero();

    Calendar priorDate = Calendar.getInstance();
    priorDate.setTime(dueDate);
    priorDate.roll(Calendar.YEAR, -1);
    assertThat(taskService.createTaskQuery().dueAfter(priorDate.getTime())
      .count()).isOne();

    assertThat(taskService.createTaskQuery()
      .dueBefore(otherDate.getTime()).count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testTaskDueBefore() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // Set due-date on task
    Calendar dueDateCal = Calendar.getInstance();
    task.setDueDate(dueDateCal.getTime());
    taskService.saveTask(task);

    Calendar oneHourAgo = Calendar.getInstance();
    oneHourAgo.setTime(dueDateCal.getTime());
    oneHourAgo.add(Calendar.HOUR, -1);

    Calendar oneHourLater = Calendar.getInstance();
    oneHourLater.setTime(dueDateCal.getTime());
    oneHourLater.add(Calendar.HOUR, 1);

    assertThat(taskService.createTaskQuery().dueBefore(oneHourLater.getTime()).count()).isOne();
    assertThat(taskService.createTaskQuery().dueBefore(oneHourAgo.getTime()).count()).isZero();

    // Update due-date to null, shouldn't show up anymore in query that matched before
    task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    task.setDueDate(null);
    taskService.saveTask(task);

    assertThat(taskService.createTaskQuery().dueBefore(oneHourLater.getTime()).count()).isZero();
    assertThat(taskService.createTaskQuery().dueBefore(oneHourAgo.getTime()).count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testTaskDueAfter() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // Set due-date on task
    Calendar dueDateCal = Calendar.getInstance();
    task.setDueDate(dueDateCal.getTime());
    taskService.saveTask(task);

    Calendar oneHourAgo = Calendar.getInstance();
    oneHourAgo.setTime(dueDateCal.getTime());
    oneHourAgo.add(Calendar.HOUR, -1);

    Calendar oneHourLater = Calendar.getInstance();
    oneHourLater.setTime(dueDateCal.getTime());
    oneHourLater.add(Calendar.HOUR, 1);

    assertThat(taskService.createTaskQuery().dueAfter(oneHourAgo.getTime()).count()).isOne();
    assertThat(taskService.createTaskQuery().dueAfter(oneHourLater.getTime()).count()).isZero();

    // Update due-date to null, shouldn't show up anymore in query that matched before
    task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    task.setDueDate(null);
    taskService.saveTask(task);

    assertThat(taskService.createTaskQuery().dueAfter(oneHourLater.getTime()).count()).isZero();
    assertThat(taskService.createTaskQuery().dueAfter(oneHourAgo.getTime()).count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testTaskDueDateCombinations() throws Exception {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // Set due-date on task
    Date dueDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 01:12:13");
    task.setDueDate(dueDate);
    taskService.saveTask(task);

    Date oneHourAgo = new Date(dueDate.getTime() - 60 * 60 * 1000);
    Date oneHourLater = new Date(dueDate.getTime() + 60 * 60 * 1000);

    assertThat(taskService.createTaskQuery()
      .dueAfter(oneHourAgo).dueDate(dueDate).dueBefore(oneHourLater).count()).isOne();
    assertThat(taskService.createTaskQuery()
        .dueAfter(oneHourLater).dueDate(dueDate).dueBefore(oneHourAgo).count()).isZero();
    assertThat(taskService.createTaskQuery()
        .dueAfter(oneHourLater).dueDate(dueDate).count()).isZero();
    assertThat(taskService.createTaskQuery()
        .dueDate(dueDate).dueBefore(oneHourAgo).count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void shouldQueryForTasksWithoutDueDate() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    task.setDueDate(ClockUtil.now());
    taskService.saveTask(task);

    // then
    assertThat(taskService.createTaskQuery().withoutDueDate().count()).isEqualTo(12);
  }

  @Test
  void shouldRejectDueDateAndWithoutDueDateCombination() {
    var taskQuery = taskService.createTaskQuery().dueDate(ClockUtil.now());
    assertThatThrownBy(taskQuery::withoutDueDate)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage");
  }

  @Test
  void shouldRejectWithoutDueDateAndDueDateCombination() {
    var taskQuery = taskService.createTaskQuery().withoutDueDate();
    Date now = ClockUtil.now();
    assertThatThrownBy(() -> taskQuery.dueDate(now))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage");
  }

  @Test
  void shouldRejectDueBeforeAndWithoutDueDateCombination() {
    var taskQuery = taskService.createTaskQuery().dueBefore(ClockUtil.now());
    assertThatThrownBy(taskQuery::withoutDueDate)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage");
  }

  @Test
  void shouldRejectWithoutDueDateAndDueBeforeCombination() {
    var taskQuery = taskService.createTaskQuery().withoutDueDate();
    Date now = ClockUtil.now();
    assertThatThrownBy(() -> taskQuery.dueBefore(now))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage");
  }

  @Test
  void shouldRejectDueAfterAndWithoutDueDateCombination() {
    var taskQuery = taskService.createTaskQuery().dueAfter(ClockUtil.now());
    assertThatThrownBy(taskQuery::withoutDueDate)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage");
  }

  @Test
  void shouldRejectWithoutDueDateAndDueAfterCombination() {
    var taskQuery = taskService.createTaskQuery().withoutDueDate();
    Date now = ClockUtil.now();
    assertThatThrownBy(() -> taskQuery.dueAfter(now))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Invalid query usage");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testFollowUpDate() throws Exception {
    Calendar otherDate = Calendar.getInstance();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // do not find any task instances with follow up date
    assertThat(taskService.createTaskQuery().followUpDate(otherDate.getTime()).count()).isZero();
    assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId())
      // we might have tasks from other test cases - so we limit to the current PI
      .followUpBeforeOrNotExistent(otherDate.getTime()).count()).isOne();

    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // set follow-up date on task
    Date followUpDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 01:12:13");
    task.setFollowUpDate(followUpDate);
    taskService.saveTask(task);

    assertThat(taskService.createTaskQuery().taskId(task.getId()).singleResult().getFollowUpDate()).isEqualTo(followUpDate);
    assertThat(taskService.createTaskQuery().followUpDate(followUpDate).count()).isOne();

    otherDate.setTime(followUpDate);

    otherDate.add(Calendar.YEAR, 1);
    assertThat(taskService.createTaskQuery().followUpDate(otherDate.getTime()).count()).isZero();
    assertThat(taskService.createTaskQuery().followUpBefore(otherDate.getTime()).count()).isOne();
    assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()) //
      .followUpBeforeOrNotExistent(otherDate.getTime()).count()).isOne();
    assertThat(taskService.createTaskQuery().followUpAfter(otherDate.getTime()).count()).isZero();

    otherDate.add(Calendar.YEAR, -2);
    assertThat(taskService.createTaskQuery().followUpAfter(otherDate.getTime()).count()).isOne();
    assertThat(taskService.createTaskQuery().followUpBefore(otherDate.getTime()).count()).isZero();
    assertThat(taskService.createTaskQuery().processInstanceId(processInstance.getId()) //
        .followUpBeforeOrNotExistent(otherDate.getTime()).count()).isZero();

    taskService.complete(task.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testFollowUpDateCombinations() throws Exception {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

    // Set follow-up date on task
    Date dueDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/02/2003 01:12:13");
    task.setFollowUpDate(dueDate);
    taskService.saveTask(task);

    Date oneHourAgo = new Date(dueDate.getTime() - 60 * 60 * 1000);
    Date oneHourLater = new Date(dueDate.getTime() + 60 * 60 * 1000);

    assertThat(taskService.createTaskQuery()
      .followUpAfter(oneHourAgo).followUpDate(dueDate).followUpBefore(oneHourLater).count()).isOne();
    assertThat(taskService.createTaskQuery()
        .followUpAfter(oneHourLater).followUpDate(dueDate).followUpBefore(oneHourAgo).count()).isZero();
    assertThat(taskService.createTaskQuery()
        .followUpAfter(oneHourLater).followUpDate(dueDate).count()).isZero();
    assertThat(taskService.createTaskQuery()
        .followUpDate(dueDate).followUpBefore(oneHourAgo).count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testQueryByActivityInstanceId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    String activityInstanceId = runtimeService.getActivityInstance(processInstance.getId())
                                              .getChildActivityInstances()[0].getId();

    assertThat(taskService.createTaskQuery().activityInstanceIdIn(activityInstanceId).list()).hasSize(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testQueryByMultipleActivityInstanceIds() {
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    String activityInstanceId1 = runtimeService.getActivityInstance(processInstance1.getId())
                                              .getChildActivityInstances()[0].getId();

    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    String activityInstanceId2 = runtimeService.getActivityInstance(processInstance2.getId())
                                              .getChildActivityInstances()[0].getId();

    List<Task> result1 = taskService.createTaskQuery().activityInstanceIdIn(activityInstanceId1).list();
    assertThat(result1).hasSize(1);
    assertThat(result1.get(0).getProcessInstanceId()).isEqualTo(processInstance1.getId());

    List<Task> result2 = taskService.createTaskQuery().activityInstanceIdIn(activityInstanceId2).list();
    assertThat(result2).hasSize(1);
    assertThat(result2.get(0).getProcessInstanceId()).isEqualTo(processInstance2.getId());

    assertThat(taskService.createTaskQuery().activityInstanceIdIn(activityInstanceId1, activityInstanceId2).list()).hasSize(2);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml"})
  @Test
  void testQueryByInvalidActivityInstanceId() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    assertThat(taskService.createTaskQuery().activityInstanceIdIn("anInvalidActivityInstanceId").list()).isEmpty();
  }

  @Test
  void testQueryPaging() {
    TaskQuery query = taskService.createTaskQuery().taskCandidateUser("kermit");

    assertThat(query.listPage(0, Integer.MAX_VALUE)).hasSize(10);

    // Verifying the un-paged results
    assertThat(query.count()).isEqualTo(10);
    assertThat(query.list()).hasSize(10);

    // Verifying paged results
    assertThat(query.listPage(0, 2)).hasSize(2);
    assertThat(query.listPage(2, 2)).hasSize(2);
    assertThat(query.listPage(4, 3)).hasSize(3);
    assertThat(query.listPage(9, 3)).hasSize(1);
    assertThat(query.listPage(9, 1)).hasSize(1);

    // Verifying odd usages
    assertThat(query.listPage(-1, -1)).isEmpty();
    assertThat(query.listPage(10, 2)).isEmpty(); // 9 is the last index with a result
    assertThat(query.listPage(0, 15)).hasSize(10); // there are only 10 tasks
  }

  @Test
  void testQuerySorting() {
    // default ordering is by id
    int expectedCount = 12;
    verifySortingAndCount(taskService.createTaskQuery(), expectedCount, taskById());
    verifySortingAndCount(taskService.createTaskQuery().orderByTaskId().asc(), expectedCount, taskById());
    verifySortingAndCount(taskService.createTaskQuery().orderByTaskName().asc(), expectedCount, taskByName());
    verifySortingAndCount(taskService.createTaskQuery().orderByTaskPriority().asc(), expectedCount, taskByPriority());
    verifySortingAndCount(taskService.createTaskQuery().orderByTaskAssignee().asc(), expectedCount, taskByAssignee());
    verifySortingAndCount(taskService.createTaskQuery().orderByTaskDescription().asc(), expectedCount, taskByDescription());
    verifySortingAndCount(taskService.createTaskQuery().orderByProcessInstanceId().asc(), expectedCount, taskByProcessInstanceId());
    verifySortingAndCount(taskService.createTaskQuery().orderByExecutionId().asc(), expectedCount, taskByExecutionId());
    verifySortingAndCount(taskService.createTaskQuery().orderByTaskCreateTime().asc(), expectedCount, taskByCreateTime());
    verifySortingAndCount(taskService.createTaskQuery().orderByDueDate().asc(), expectedCount, taskByDueDate());
    verifySortingAndCount(taskService.createTaskQuery().orderByFollowUpDate().asc(), expectedCount, taskByFollowUpDate());
    verifySortingAndCount(taskService.createTaskQuery().orderByCaseInstanceId().asc(), expectedCount, taskByCaseInstanceId());
    verifySortingAndCount(taskService.createTaskQuery().orderByCaseExecutionId().asc(), expectedCount, taskByCaseExecutionId());

    verifySortingAndCount(taskService.createTaskQuery().orderByTaskId().desc(), expectedCount, inverted(taskById()));
    verifySortingAndCount(taskService.createTaskQuery().orderByTaskName().desc(), expectedCount, inverted(taskByName()));
    verifySortingAndCount(taskService.createTaskQuery().orderByTaskPriority().desc(), expectedCount, inverted(taskByPriority()));
    verifySortingAndCount(taskService.createTaskQuery().orderByTaskAssignee().desc(), expectedCount, inverted(taskByAssignee()));
    verifySortingAndCount(taskService.createTaskQuery().orderByTaskDescription().desc(), expectedCount, inverted(taskByDescription()));
    verifySortingAndCount(taskService.createTaskQuery().orderByProcessInstanceId().desc(), expectedCount, inverted(taskByProcessInstanceId()));
    verifySortingAndCount(taskService.createTaskQuery().orderByExecutionId().desc(), expectedCount, inverted(taskByExecutionId()));
    verifySortingAndCount(taskService.createTaskQuery().orderByTaskCreateTime().desc(), expectedCount, inverted(taskByCreateTime()));
    verifySortingAndCount(taskService.createTaskQuery().orderByDueDate().desc(), expectedCount, inverted(taskByDueDate()));
    verifySortingAndCount(taskService.createTaskQuery().orderByFollowUpDate().desc(), expectedCount, inverted(taskByFollowUpDate()));
    verifySortingAndCount(taskService.createTaskQuery().orderByCaseInstanceId().desc(), expectedCount, inverted(taskByCaseInstanceId()));
    verifySortingAndCount(taskService.createTaskQuery().orderByCaseExecutionId().desc(), expectedCount, inverted(taskByCaseExecutionId()));

    verifySortingAndCount(taskService.createTaskQuery().orderByTaskId().taskName("testTask").asc(), 6, taskById());
    verifySortingAndCount(taskService.createTaskQuery().orderByTaskId().taskName("testTask").desc(), 6, inverted(taskById()));
  }

  @Test
  void testQuerySortingByNameShouldBeCaseInsensitive() {
    // create task with capitalized name
    Task task = taskService.newTask("caseSensitiveTestTask");
    task.setName("CaseSensitiveTestTask");
    taskService.saveTask(task);

    // create task filter
    Filter filter = filterService.newTaskFilter("taskNameOrdering");
    filterService.saveFilter(filter);

    List<String> sortedNames = getTaskNamesFromTasks(taskService.createTaskQuery().list());
    sortedNames.sort(String.CASE_INSENSITIVE_ORDER);

    // ascending ordering
    TaskQuery taskQuery = taskService.createTaskQuery().orderByTaskNameCaseInsensitive().asc();
    List<String> ascNames = getTaskNamesFromTasks(taskQuery.list());
    assertThat(ascNames).isEqualTo(sortedNames);

    // test filter merging
    ascNames = getTaskNamesFromTasks(filterService.list(filter.getId(), taskQuery));
    assertThat(ascNames).isEqualTo(sortedNames);

    // descending ordering

    // reverse sorted names to test descending ordering
    Collections.reverse(sortedNames);

    taskQuery = taskService.createTaskQuery().orderByTaskNameCaseInsensitive().desc();
    List<String> descNames = getTaskNamesFromTasks(taskQuery.list());
    assertThat(descNames).isEqualTo(sortedNames);

    // test filter merging
    descNames = getTaskNamesFromTasks(filterService.list(filter.getId(), taskQuery));
    assertThat(descNames).isEqualTo(sortedNames);

    // delete test task
    taskService.deleteTask(task.getId(), true);

    // delete filter
    filterService.deleteFilter(filter.getId());
  }

  @Test
  void testQueryOrderByTaskName() {

    // asc
    List<Task> tasks = taskService.createTaskQuery()
      .orderByTaskName()
      .asc()
      .list();
    assertThat(tasks).hasSize(12);


    List<String> taskNames = getTaskNamesFromTasks(tasks);
    assertThat(taskNames.get(0)).isEqualTo("accountancy description");
    assertThat(taskNames.get(1)).isEqualTo("accountancy description");
    assertThat(taskNames.get(2)).isEqualTo("gonzo_Task");
    assertThat(taskNames.get(3)).isEqualTo("managementAndAccountancyTask");
    assertThat(taskNames.get(4)).isEqualTo("managementTask");
    assertThat(taskNames.get(5)).isEqualTo("managementTask");
    assertThat(taskNames.get(6)).isEqualTo("testTask");
    assertThat(taskNames.get(7)).isEqualTo("testTask");
    assertThat(taskNames.get(8)).isEqualTo("testTask");
    assertThat(taskNames.get(9)).isEqualTo("testTask");
    assertThat(taskNames.get(10)).isEqualTo("testTask");
    assertThat(taskNames.get(11)).isEqualTo("testTask");

    // desc
    tasks = taskService.createTaskQuery()
      .orderByTaskName()
      .desc()
      .list();
    assertThat(tasks).hasSize(12);

    taskNames = getTaskNamesFromTasks(tasks);
    assertThat(taskNames.get(0)).isEqualTo("testTask");
    assertThat(taskNames.get(1)).isEqualTo("testTask");
    assertThat(taskNames.get(2)).isEqualTo("testTask");
    assertThat(taskNames.get(3)).isEqualTo("testTask");
    assertThat(taskNames.get(4)).isEqualTo("testTask");
    assertThat(taskNames.get(5)).isEqualTo("testTask");
    assertThat(taskNames.get(6)).isEqualTo("managementTask");
    assertThat(taskNames.get(7)).isEqualTo("managementTask");
    assertThat(taskNames.get(8)).isEqualTo("managementAndAccountancyTask");
    assertThat(taskNames.get(9)).isEqualTo("gonzo_Task");
    assertThat(taskNames.get(10)).isEqualTo("accountancy description");
    assertThat(taskNames.get(11)).isEqualTo("accountancy description");
  }

  public List<String> getTaskNamesFromTasks(List<Task> tasks) {
    List<String> names = new ArrayList<>();
    for (Task task : tasks) {
      names.add(task.getName());
    }
    return names;
  }

  @Test
  void testNativeQuery() {
    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();
    assertThat(managementService.getTableName(Task.class)).isEqualTo(tablePrefix + "ACT_RU_TASK");
    assertThat(managementService.getTableName(TaskEntity.class)).isEqualTo(tablePrefix + "ACT_RU_TASK");
    assertThat(taskService.createNativeTaskQuery().sql("SELECT * FROM " + managementService.getTableName(Task.class)).list()).hasSize(12);
    assertThat(taskService.createNativeTaskQuery().sql("SELECT count(*) FROM " + managementService.getTableName(Task.class)).count()).isEqualTo(12);

    assertThat(taskService.createNativeTaskQuery().sql("SELECT count(*) FROM " + tablePrefix + "ACT_RU_TASK T1, " + tablePrefix + "ACT_RU_TASK T2").count()).isEqualTo(144);

    // join task and variable instances
    assertThat(taskService.createNativeTaskQuery().sql("SELECT count(*) FROM " + managementService.getTableName(Task.class) + " T1, " + managementService.getTableName(VariableInstanceEntity.class) + " V1 WHERE V1.TASK_ID_ = T1.ID_").count()).isOne();
    List<Task> tasks = taskService.createNativeTaskQuery().sql("SELECT T1.* FROM " + managementService.getTableName(Task.class) + " T1, "+managementService.getTableName(VariableInstanceEntity.class)+" V1 WHERE V1.TASK_ID_ = T1.ID_").list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("gonzo_Task");

    // select with distinct
    assertThat(taskService.createNativeTaskQuery().sql("SELECT DISTINCT T1.* FROM " + tablePrefix + "ACT_RU_TASK T1").list()).hasSize(12);

    assertThat(taskService.createNativeTaskQuery().sql("SELECT count(*) FROM " + managementService.getTableName(Task.class) + " T WHERE T.NAME_ = 'gonzo_Task'").count()).isOne();
    assertThat(taskService.createNativeTaskQuery().sql("SELECT * FROM " + managementService.getTableName(Task.class) + " T WHERE T.NAME_ = 'gonzo_Task'").list()).hasSize(1);

    // use parameters
    assertThat(taskService.createNativeTaskQuery().sql("SELECT count(*) FROM " + managementService.getTableName(Task.class) + " T WHERE T.NAME_ = #{taskName}").parameter("taskName", "gonzo_Task").count()).isOne();
  }

  @Test
  void testNativeQueryPaging() {
    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();
    assertThat(managementService.getTableName(Task.class)).isEqualTo(tablePrefix + "ACT_RU_TASK");
    assertThat(managementService.getTableName(TaskEntity.class)).isEqualTo(tablePrefix + "ACT_RU_TASK");
    assertThat(taskService.createNativeTaskQuery().sql("SELECT * FROM " + managementService.getTableName(Task.class)).listPage(0, 5)).hasSize(5);
    assertThat(taskService.createNativeTaskQuery().sql("SELECT * FROM " + managementService.getTableName(Task.class)).listPage(10, 12)).hasSize(2);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByCaseDefinitionId() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseDefinitionId(caseDefinitionId);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidCaseDefinitionId() {
    TaskQuery query = taskService.createTaskQuery();

    query.caseDefinitionId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseDefinitionId(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByCaseDefinitionKey() {
    String caseDefinitionKey = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getKey();

    caseService
      .withCaseDefinitionByKey(caseDefinitionKey)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseDefinitionKey(caseDefinitionKey);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidCaseDefinitionKey() {
    TaskQuery query = taskService.createTaskQuery();

    query.caseDefinitionKey("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseDefinitionKey(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByCaseDefinitionName() {
    CaseDefinition caseDefinition = repositoryService
        .createCaseDefinitionQuery()
        .singleResult();

    String caseDefinitionId = caseDefinition.getId();
    String caseDefinitionName = caseDefinition.getName();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseDefinitionName(caseDefinitionName);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidCaseDefinitionName() {
    TaskQuery query = taskService.createTaskQuery();

    query.caseDefinitionName("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseDefinitionName(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn", "org/operaton/bpm/engine/test/api/repository/three_.cmmn"})
  @Test
  void testQueryByCaseDefinitionNameLike() {
    List<String> caseDefinitionIds = getCaseDefinitionIds();

    for (String caseDefinitionId : caseDefinitionIds) {
      caseService
          .withCaseDefinition(caseDefinitionId)
          .create();
    }
    TaskQuery query = taskService.createTaskQuery();

    query.caseDefinitionNameLike("One T%");
    verifyQueryResults(query, 1);

    query.caseDefinitionNameLike("%Task Case");
    verifyQueryResults(query, 1);

    query.caseDefinitionNameLike("%Task%");
    verifyQueryResults(query, 1);

    query.caseDefinitionNameLike("%z\\_");
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidCaseDefinitionNameLike() {
    TaskQuery query = taskService.createTaskQuery();

    query.caseDefinitionNameLike("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseDefinitionNameLike(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByCaseInstanceId() {
    String caseDefinitionId = getCaseDefinitionId();

    String caseInstanceId = caseService
      .withCaseDefinition(caseDefinitionId)
      .create()
      .getId();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceId(caseInstanceId);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources =
      {
          "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testQueryByCaseInstanceIdHierarchy.cmmn",
          "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testQueryByCaseInstanceIdHierarchy.bpmn20.xml"
      })
  @Test
  void testQueryByCaseInstanceIdHierarchy() {
    // given
    String caseInstanceId = caseService
      .withCaseDefinitionByKey("case")
      .create()
      .getId();

    String processTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_ProcessTask_1")
        .singleResult()
        .getId();
    assertThat(processTaskId).isNotNull();

    // then

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceId(caseInstanceId);

    verifyQueryResults(query, 2);

    for (Task task : query.list()) {
      assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
      taskService.complete(task.getId());
    }

    verifyQueryResults(query, 1);
    assertThat(query.singleResult().getCaseInstanceId()).isEqualTo(caseInstanceId);

    taskService.complete(query.singleResult().getId());

    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryByInvalidCaseInstanceId() {
    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseInstanceId(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByCaseInstanceBusinessKey() {
    String caseDefinitionId = getCaseDefinitionId();

    String businessKey = "aBusinessKey";

    caseService
      .withCaseDefinition(caseDefinitionId)
      .businessKey(businessKey)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceBusinessKey(businessKey);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidCaseInstanceBusinessKey() {
    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceBusinessKey("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseInstanceBusinessKey(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByCaseInstanceBusinessKeyLike() {
    String caseDefinitionId = getCaseDefinitionId();

    String businessKey = "aBusiness_Key";

    caseService
      .withCaseDefinition(caseDefinitionId)
      .businessKey(businessKey)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceBusinessKeyLike("aBus%");
    verifyQueryResults(query, 1);

    query.caseInstanceBusinessKeyLike("%siness\\_Key");
    verifyQueryResults(query, 1);

    query.caseInstanceBusinessKeyLike("%sines%");
    verifyQueryResults(query, 1);

    query.caseInstanceBusinessKeyLike("%sines%");
    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidCaseInstanceBusinessKeyLike() {
    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceBusinessKeyLike("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseInstanceBusinessKeyLike(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testQueryByCaseExecutionId() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .create();

    String humanTaskExecutionId = startDefaultCaseExecutionManually();

    TaskQuery query = taskService.createTaskQuery();

    query.caseExecutionId(humanTaskExecutionId);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidCaseExecutionId() {
    TaskQuery query = taskService.createTaskQuery();

    query.caseExecutionId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseExecutionId(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByNullCaseInstanceVariableValueEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aNullValue", null)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueEquals("aNullValue", null);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByStringCaseInstanceVariableValueEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueEquals("aStringValue", "abc");

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCaseInstanceVariableNameEqualsIgnoreCase() {
    String caseDefinitionId = getCaseDefinitionId();

    String variableName = "someVariable";
    String variableValue = "someCamelCaseValue";

    caseService.withCaseDefinition(caseDefinitionId).setVariable(variableName, variableValue).create();

    // query for case-insensitive variable name should only return a result if case-insensitive search is used
    assertThat(taskService.createTaskQuery().matchVariableNamesIgnoreCase().caseInstanceVariableValueEquals(variableName.toLowerCase(), variableValue).count()).isOne();
    assertThat(taskService.createTaskQuery().caseInstanceVariableValueEquals(variableName.toLowerCase(), variableValue).count()).isZero();

    // query should treat all variables case-insensitively, even when flag is set after variable
    assertThat(taskService.createTaskQuery().caseInstanceVariableValueEquals(variableName.toLowerCase(), variableValue).matchVariableNamesIgnoreCase().count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByStringCaseInstanceVariableValueEqualsIgnoreCase() {
    String caseDefinitionId = getCaseDefinitionId();

    String variableName = "someVariable";
    String variableValue = "someCamelCaseValue";

    caseService
    .withCaseDefinition(caseDefinitionId)
    .setVariable(variableName, variableValue)
    .create();

    TaskQuery query;

    // query for case-insensitive variable value should only return a result when case-insensitive search is used
    query = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueEquals(variableName, variableValue.toLowerCase());
    verifyQueryResults(query, 1);
    query = taskService.createTaskQuery().caseInstanceVariableValueEquals(variableName, variableValue.toLowerCase());
    verifyQueryResults(query, 0);

    // query for non existing variable should return zero results
    query = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueEquals("nonExistingVariable", variableValue.toLowerCase());
    verifyQueryResults(query, 0);

    // query for existing variable with different value should return zero results
    query = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueEquals(variableName, "nonExistentValue".toLowerCase());
    verifyQueryResults(query, 0);

    // query for case-insensitive variable with not equals operator should only return a result when case-sensitive search is used
    query = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueNotEquals(variableName, variableValue.toLowerCase());
    verifyQueryResults(query, 0);
    query = taskService.createTaskQuery().caseInstanceVariableValueNotEquals(variableName, variableValue.toLowerCase());
    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByBooleanCaseInstanceVariableValueEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aBooleanValue", true)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueEquals("aBooleanValue", true);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByShortCaseInstanceVariableValueEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aShortValue", (short) 123)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueEquals("aShortValue", (short) 123);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByIntegerCaseInstanceVariableValueEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("anIntegerValue", 456)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueEquals("anIntegerValue", 456);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByLongCaseInstanceVariableValueEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aLongValue", (long) 789)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueEquals("aLongValue", (long) 789);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByDateCaseInstanceVariableValueEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    Date now = new Date();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDateValue", now)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueEquals("aDateValue", now);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByDoubleCaseInstanceVariableValueEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDoubleValue", 1.5)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueEquals("aDoubleValue", 1.5);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByByteArrayCaseInstanceVariableValueEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aByteArrayValue", bytes)
      .create();

    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueEquals("aByteArrayValue", bytes);

    assertThatThrownBy(taskQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryBySerializableCaseInstanceVariableValueEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aSerializableValue", serializable)
      .create();

    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueEquals("aSerializableValue", serializable);

    assertThatThrownBy(taskQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByFileCaseInstanceVariableValueEquals() {
    FileValue fileValue = createDefaultFileValue();
    String variableName = "aFileValue";

    startDefaultCaseWithVariable(fileValue, variableName);
    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueEquals(variableName, fileValue);

    try {
      taskQuery.list();
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Variables of type File cannot be used to query");
    }
  }

  /**
   * Starts the one deployed case at the point of the manual activity PI_HumanTask_1
   * with the given variable.
   */
  protected void startDefaultCaseWithVariable(Object variableValue, String variableName) {
    String caseDefinitionId = getCaseDefinitionId();
    createCaseWithVariable(caseDefinitionId, variableValue, variableName);
  }

  /**
   * @return the case definition id if only one case is deployed.
   */
  protected String getCaseDefinitionId() {
    return repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();
  }

  /**
   * @return the case definition ids
   */
  protected List<String> getCaseDefinitionIds() {
    List<String> caseDefinitionIds = new ArrayList<>();
    List<CaseDefinition> caseDefinitions = repositoryService.createCaseDefinitionQuery().list();
    for (CaseDefinition caseDefinition: caseDefinitions) {
      caseDefinitionIds.add(caseDefinition.getId());
    }
    return caseDefinitionIds;
  }

  protected void createCaseWithVariable(String caseDefinitionId, Object variableValue, String variableName) {
    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable(variableName, variableValue)
      .create();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByStringCaseInstanceVariableValueNotEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueNotEquals("aStringValue", "abd");

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByBooleanCaseInstanceVariableValueNotEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aBooleanValue", true)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueNotEquals("aBooleanValue", false);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByShortCaseInstanceVariableValueNotEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aShortValue", (short) 123)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueNotEquals("aShortValue", (short) 124);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByIntegerCaseInstanceVariableValueNotEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("anIntegerValue", 456)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueNotEquals("anIntegerValue", 457);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByLongCaseInstanceVariableValueNotEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aLongValue", (long) 789)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueNotEquals("aLongValue", (long) 790);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByDateCaseInstanceVariableValueNotEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    Date now = new Date();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDateValue", now)
      .create();

    Date before = new Date(now.getTime() - 100000);

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueNotEquals("aDateValue", before);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByDoubleCaseInstanceVariableValueNotEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDoubleValue", 1.5)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueNotEquals("aDoubleValue", 1.6);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByFileCaseInstanceVariableValueNotEquals() {
    FileValue fileValue = createDefaultFileValue();
    String variableName = "aFileValue";

    startDefaultCaseWithVariable(fileValue, variableName);
    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueNotEquals(variableName, fileValue);
    try {
      taskQuery.list();
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Variables of type File cannot be used to query");
    }
  }

  /**
   * @return
   */
  protected FileValue createDefaultFileValue() {
    return Variables.fileValue("tst.txt").file("somebytes".getBytes()).create();
  }

  /**
   * Starts the case execution for oneTaskCase.cmmn<p>
   * Only works for testcases, which deploy that process.
   *
   * @return the execution id for the activity PI_HumanTask_1
   */
  protected String startDefaultCaseExecutionManually() {
    String humanTaskExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(humanTaskExecutionId)
      .manualStart();
    return humanTaskExecutionId;
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryBySerializableCaseInstanceVariableValueNotEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aSerializableValue", serializable)
      .create();

    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueNotEquals("aSerializableValue", serializable);

    assertThatThrownBy(taskQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByByteArrayCaseInstanceVariableValueNotEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aByteArrayValue", bytes)
      .create();

    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueNotEquals("aByteArrayValue", bytes);

    assertThatThrownBy(taskQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByNullCaseInstanceVariableValueGreaterThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aNullValue", null)
      .create();

    var taskQuery = taskService.createTaskQuery();

    assertThatThrownBy(() -> taskQuery.caseInstanceVariableValueGreaterThan("aNullValue", null)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByStringCaseInstanceVariableValueGreaterThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThan("aStringValue", "ab");

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByBooleanCaseInstanceVariableValueGreaterThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aBooleanValue", true)
      .create();

    var taskQuery = taskService.createTaskQuery();

    assertThatThrownBy(() -> taskQuery.caseInstanceVariableValueGreaterThan("aBooleanValue", false)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByShortCaseInstanceVariableValueGreaterThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aShortValue", (short) 123)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThan("aShortValue", (short) 122);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByIntegerCaseInstanceVariableValueGreaterThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("anIntegerValue", 456)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThan("anIntegerValue", 455);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByLongCaseInstanceVariableValueGreaterThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aLongValue", (long) 789)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThan("aLongValue", (long) 788);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByDateCaseInstanceVariableValueGreaterThan() {
    String caseDefinitionId = getCaseDefinitionId();

    Date now = new Date();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDateValue", now)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    Date before = new Date(now.getTime() - 100000);

    query.caseInstanceVariableValueGreaterThan("aDateValue", before);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByDoubleCaseInstanceVariableValueGreaterThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDoubleValue", 1.5)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThan("aDoubleValue", 1.4);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByByteArrayCaseInstanceVariableValueGreaterThan() {
    String caseDefinitionId = getCaseDefinitionId();

    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aByteArrayValue", bytes)
      .create();

    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueGreaterThan("aByteArrayValue", bytes);

    assertThatThrownBy(taskQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryBySerializableCaseInstanceVariableGreaterThan() {
    String caseDefinitionId = getCaseDefinitionId();

    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aSerializableValue", serializable)
      .create();

    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueGreaterThan("aSerializableValue", serializable);

    assertThatThrownBy(taskQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testQueryByFileCaseInstanceVariableValueGreaterThan() {
    FileValue fileValue = createDefaultFileValue();
    String variableName = "aFileValue";

    startDefaultCaseWithVariable(fileValue, variableName);
    startDefaultCaseExecutionManually();
    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueGreaterThan(variableName, fileValue);

    try {
      taskQuery.list();
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Variables of type File cannot be used to query");
      }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByNullCaseInstanceVariableValueGreaterThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aNullValue", null)
      .create();

    var taskQuery = taskService.createTaskQuery();

    assertThatThrownBy(() -> taskQuery.caseInstanceVariableValueGreaterThanOrEquals("aNullValue", null)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByStringCaseInstanceVariableValueGreaterThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThanOrEquals("aStringValue", "ab");

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThanOrEquals("aStringValue", "abc");

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByBooleanCaseInstanceVariableValueGreaterThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aBooleanValue", true)
      .create();

    TaskQuery taskQuery = taskService.createTaskQuery();

    assertThatThrownBy(() -> taskQuery.caseInstanceVariableValueGreaterThanOrEquals("aBooleanValue", false)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByShortCaseInstanceVariableValueGreaterThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aShortValue", (short) 123)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThanOrEquals("aShortValue", (short) 122);

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThanOrEquals("aShortValue", (short) 123);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByIntegerCaseInstanceVariableValueGreaterThanOrEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("anIntegerValue", 456)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThanOrEquals("anIntegerValue", 455);

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThanOrEquals("anIntegerValue", 456);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByLongCaseInstanceVariableValueGreaterThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aLongValue", (long) 789)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThanOrEquals("aLongValue", (long) 788);

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThanOrEquals("aLongValue", (long) 789);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByDateCaseInstanceVariableValueGreaterThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    Date now = new Date();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDateValue", now)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    Date before = new Date(now.getTime() - 100000);

    query.caseInstanceVariableValueGreaterThanOrEquals("aDateValue", before);

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThanOrEquals("aDateValue", now);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByDoubleCaseInstanceVariableValueGreaterThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDoubleValue", 1.5)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThanOrEquals("aDoubleValue", 1.4);

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueGreaterThanOrEquals("aDoubleValue", 1.5);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByByteArrayCaseInstanceVariableValueGreaterThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aByteArrayValue", bytes)
      .create();

    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueGreaterThanOrEquals("aByteArrayValue", bytes);

    assertThatThrownBy(taskQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryBySerializableCaseInstanceVariableGreaterThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aSerializableValue", serializable)
      .create();

    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueGreaterThanOrEquals("aSerializableValue", serializable);

    assertThatThrownBy(taskQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByFileCaseInstanceVariableValueGreaterThanOrEqual() {
    FileValue fileValue = createDefaultFileValue();
    String variableName = "aFileValue";

    startDefaultCaseWithVariable(fileValue, variableName);
    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueGreaterThanOrEquals(variableName, fileValue);

    try {
      taskQuery.list();
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Variables of type File cannot be used to query");
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByNullCaseInstanceVariableValueLessThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aNullValue", null)
      .create();

    var taskQuery = taskService.createTaskQuery();

    assertThatThrownBy(() -> taskQuery.caseInstanceVariableValueLessThan("aNullValue", null)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByStringCaseInstanceVariableValueLessThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThan("aStringValue", "abd");

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByBooleanCaseInstanceVariableValueLessThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aBooleanValue", true)
      .create();

    var taskQuery = taskService.createTaskQuery();

    assertThatThrownBy(() -> taskQuery.caseInstanceVariableValueLessThan("aBooleanValue", false)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByShortCaseInstanceVariableValueLessThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aShortValue", (short) 123)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThan("aShortValue", (short) 124);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByIntegerCaseInstanceVariableValueLessThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("anIntegerValue", 456)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThan("anIntegerValue", 457);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByLongCaseInstanceVariableValueLessThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aLongValue", (long) 789)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThan("aLongValue", (long) 790);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByDateCaseInstanceVariableValueLessThan() {
    String caseDefinitionId = getCaseDefinitionId();

    Date now = new Date();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDateValue", now)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    Date after = new Date(now.getTime() + 100000);

    query.caseInstanceVariableValueLessThan("aDateValue", after);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByDoubleCaseInstanceVariableValueLessThan() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDoubleValue", 1.5)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThan("aDoubleValue", 1.6);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByByteArrayCaseInstanceVariableValueLessThan() {
    String caseDefinitionId = getCaseDefinitionId();

    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aByteArrayValue", bytes)
      .create();

    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueLessThan("aByteArrayValue", bytes);

    assertThatThrownBy(taskQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryBySerializableCaseInstanceVariableLessThan() {
    String caseDefinitionId = getCaseDefinitionId();

    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aSerializableValue", serializable)
      .create();

    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueLessThan("aSerializableValue", serializable);

    assertThatThrownBy(taskQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByFileCaseInstanceVariableValueLessThan() {
    FileValue fileValue = createDefaultFileValue();
    String variableName = "aFileValue";

    startDefaultCaseWithVariable(fileValue, variableName);
    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueLessThan(variableName, fileValue);
    try {
      taskQuery.list();
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Variables of type File cannot be used to query");
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByNullCaseInstanceVariableValueLessThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aNullValue", null)
      .create();

    var taskQuery = taskService.createTaskQuery();

    assertThatThrownBy(() -> taskQuery.caseInstanceVariableValueLessThanOrEquals("aNullValue", null)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByStringCaseInstanceVariableValueLessThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThanOrEquals("aStringValue", "abd");

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThanOrEquals("aStringValue", "abc");

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByBooleanCaseInstanceVariableValueLessThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aBooleanValue", true)
      .create();

    var taskQuery = taskService.createTaskQuery();

    assertThatThrownBy(() -> taskQuery.caseInstanceVariableValueLessThanOrEquals("aBooleanValue", false)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByShortCaseInstanceVariableValueLessThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aShortValue", (short) 123)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThanOrEquals("aShortValue", (short) 124);

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThanOrEquals("aShortValue", (short) 123);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByIntegerCaseInstanceVariableValueLessThanOrEquals() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("anIntegerValue", 456)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThanOrEquals("anIntegerValue", 457);

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThanOrEquals("anIntegerValue", 456);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByLongCaseInstanceVariableValueLessThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aLongValue", (long) 789)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThanOrEquals("aLongValue", (long) 790);

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThanOrEquals("aLongValue", (long) 789);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByDateCaseInstanceVariableValueLessThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    Date now = new Date();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDateValue", now)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    Date after = new Date(now.getTime() + 100000);

    query.caseInstanceVariableValueLessThanOrEquals("aDateValue", after);

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThanOrEquals("aDateValue", now);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByDoubleCaseInstanceVariableValueLessThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aDoubleValue", 1.5)
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThanOrEquals("aDoubleValue", 1.6);

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLessThanOrEquals("aDoubleValue", 1.5);

    verifyQueryResults(query, 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByByteArrayCaseInstanceVariableValueLessThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aByteArrayValue", bytes)
      .create();

    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueLessThanOrEquals("aByteArrayValue", bytes);

    assertThatThrownBy(taskQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryBySerializableCaseInstanceVariableLessThanOrEqual() {
    String caseDefinitionId = getCaseDefinitionId();

    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aSerializableValue", serializable)
      .create();

    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueLessThanOrEquals("aSerializableValue", serializable);

    assertThatThrownBy(taskQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByFileCaseInstanceVariableValueLessThanOrEqual() {
    FileValue fileValue = createDefaultFileValue();
    String variableName = "aFileValue";

    startDefaultCaseWithVariable(fileValue, variableName);
    TaskQuery query = taskService.createTaskQuery();
    var taskQuery = query.caseInstanceVariableValueLessThanOrEquals(variableName, fileValue);
    try {
      taskQuery.list();
      fail("");
    } catch (ProcessEngineException e) {
      assertThat(e.getMessage()).contains("Variables of type File cannot be used to query");
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByNullCaseInstanceVariableValueLike() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aNullValue", null)
      .create();

    var taskQuery = taskService.createTaskQuery();

    assertThatThrownBy(() -> taskQuery.caseInstanceVariableValueLike("aNullValue", null)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByNullCaseInstanceVariableValueNotLike() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
            .withCaseDefinition(caseDefinitionId)
            .setVariable("aNullValue", null)
            .create();

    var taskQuery = taskService.createTaskQuery();

    assertThatThrownBy(() -> taskQuery.caseInstanceVariableValueNotLike("aNullValue", null)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByStringCaseInstanceVariableValueLike() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .setVariable("aStringValue", "abc")
      .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLike("aStringValue", "ab%");

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLike("aStringValue", "%bc");

    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLike("aStringValue", "%b%");

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByStringCaseInstanceVariableValueLikeIgnoreCase() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
    .withCaseDefinition(caseDefinitionId)
    .setVariable("aStringVariable", "aStringValue")
    .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueLike("aStringVariable", "aString%".toLowerCase());

    verifyQueryResults(query, 0);

    query = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueLike("aStringVariable", "aString%".toLowerCase());

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByStringCaseInstanceVariableValueNotLike() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
            .withCaseDefinition(caseDefinitionId)
            .setVariable("aStringValue", "abc")
            .create();

    TaskQuery query = taskService.createTaskQuery();
    query.caseInstanceVariableValueNotLike("aStringValue", "abc%");
    verifyQueryResults(query, 0);

    query = taskService.createTaskQuery();
    query.caseInstanceVariableValueNotLike("aStringValue", "%bc");
    verifyQueryResults(query, 0);

    query = taskService.createTaskQuery();
    query.caseInstanceVariableValueNotLike("aStringValue", "%b%");
    verifyQueryResults(query, 0);

    query = taskService.createTaskQuery();
    query.caseInstanceVariableValueNotLike("aStringValue", "abx%");
    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();
    query.caseInstanceVariableValueNotLike("aStringValue", "%be");
    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();
    query.caseInstanceVariableValueNotLike("aStringValue", "abd");
    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery();
    query.caseInstanceVariableValueNotLike("nonExistingVar", "%b%");
    verifyQueryResults(query, 0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByStringCaseInstanceVariableValueNotLikeIgnoreCase() {
    String caseDefinitionId = getCaseDefinitionId();

    caseService
            .withCaseDefinition(caseDefinitionId)
            .setVariable("aStringVariable", "aStringValue")
            .create();

    TaskQuery query = taskService.createTaskQuery();

    query.caseInstanceVariableValueNotLike("aStringVariable", "aString%".toLowerCase());
    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueNotLike("aStringVariable", "aString%".toLowerCase());
    verifyQueryResults(query, 0);

    query = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueNotLike("aStringVariable", "%ringValue".toLowerCase());
    verifyQueryResults(query, 0);

    query = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueNotLike("aStringVariable", "%ngVal%".toLowerCase());
    verifyQueryResults(query, 0);

    query = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueNotLike("aStringVariable", "aStrong%".toLowerCase());
    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueNotLike("aStringVariable", "%Strong".toLowerCase());
    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueNotLike("aStringVariable", "%ngVar%".toLowerCase());
    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueNotLike("aStringVariable", "stringVal".toLowerCase());
    verifyQueryResults(query, 1);

    query = taskService.createTaskQuery().matchVariableValuesIgnoreCase().caseInstanceVariableValueNotLike("nonExistingVar", "%String%".toLowerCase());
    verifyQueryResults(query, 0);
  }

  @Deployment
  @Test
  void testQueryByVariableInParallelBranch() {
    runtimeService.startProcessInstanceByKey("parallelGateway");

    // when there are two process variables of the same name but different types
    Execution task1Execution = runtimeService.createExecutionQuery().activityId("task1").singleResult();
    runtimeService.setVariableLocal(task1Execution.getId(), "var", 12345L);
    Execution task2Execution = runtimeService.createExecutionQuery().activityId("task2").singleResult();
    runtimeService.setVariableLocal(task2Execution.getId(), "var", 12345);

    // then the task query should be able to filter by both variables and return both tasks
    assertThat(taskService.createTaskQuery().processVariableValueEquals("var", 12345).count()).isEqualTo(2);
    assertThat(taskService.createTaskQuery().processVariableValueEquals("var", 12345L).count()).isEqualTo(2);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
  @Test
  void testQueryResultOrderingByProcessVariables() {
    // given three tasks with String process instance variables
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "bValue"));
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "cValue"));
    ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "aValue"));

    // when I make a task query with ascending variable ordering by String values
    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.STRING)
      .asc()
      .list();

    // then the tasks are ordered correctly
    assertThat(tasks).hasSize(3);
    // then in alphabetical order
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance3.getId());
    assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance1.getId());
    assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance2.getId());

    // when I make a task query with descending variable ordering by String values
    tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.STRING)
      .desc()
      .list();

    // then the tasks are ordered correctly
    assertThat(tasks).hasSize(3);
    // then in alphabetical order
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance2.getId());
    assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance1.getId());
    assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance3.getId());


    // when I make a task query with variable ordering by Integer values
    List<Task> unorderedTasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.INTEGER)
      .asc()
      .list();

    // then the tasks are in no particular ordering
    assertThat(unorderedTasks).hasSize(3);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testLocalExecutionVariable.bpmn20.xml")
  @Test
  void testQueryResultOrderingByExecutionVariables() {
    // given three tasks with String process instance variables
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("parallelGateway",
      Collections.singletonMap("var", "aValue"));
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("parallelGateway",
      Collections.singletonMap("var", "bValue"));
    ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("parallelGateway",
      Collections.singletonMap("var", "cValue"));

    // and some local variables on the tasks
    Task task1 = taskService.createTaskQuery().processInstanceId(instance1.getId()).singleResult();
    runtimeService.setVariableLocal(task1.getExecutionId(), "var", "cValue");

    Task task2 = taskService.createTaskQuery().processInstanceId(instance2.getId()).singleResult();
    runtimeService.setVariableLocal(task2.getExecutionId(), "var", "bValue");

    Task task3 = taskService.createTaskQuery().processInstanceId(instance3.getId()).singleResult();
    runtimeService.setVariableLocal(task3.getExecutionId(), "var", "aValue");

    // when I make a task query with ascending variable ordering by tasks variables
    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKey("parallelGateway")
      .orderByExecutionVariable("var", ValueType.STRING)
      .asc()
      .list();

    // then the tasks are ordered correctly by their local variables
    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance3.getId());
    assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance2.getId());
    assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance1.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
  @Test
  void testQueryResultOrderingByTaskVariables() {
    // given three tasks with String process instance variables
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.singletonMap("var", "aValue"));
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.singletonMap("var", "bValue"));
    ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
      Collections.singletonMap("var", "cValue"));

    // and some local variables on the tasks
    Task task1 = taskService.createTaskQuery().processInstanceId(instance1.getId()).singleResult();
    taskService.setVariableLocal(task1.getId(), "var", "cValue");

    Task task2 = taskService.createTaskQuery().processInstanceId(instance2.getId()).singleResult();
    taskService.setVariableLocal(task2.getId(), "var", "bValue");

    Task task3 = taskService.createTaskQuery().processInstanceId(instance3.getId()).singleResult();
    taskService.setVariableLocal(task3.getId(), "var", "aValue");

    // when I make a task query with ascending variable ordering by tasks variables
    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByTaskVariable("var", ValueType.STRING)
      .asc()
      .list();

    // then the tasks are ordered correctly by their local variables
    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance3.getId());
    assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance2.getId());
    assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance1.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  @Test
  void testQueryResultOrderingByCaseInstanceVariables() {
    // given three tasks with String case instance variables
    CaseInstance instance1 = caseService.createCaseInstanceByKey("oneTaskCase",
        Collections.singletonMap("var", "cValue"));
    CaseInstance instance2 = caseService.createCaseInstanceByKey("oneTaskCase",
        Collections.singletonMap("var", "aValue"));
    CaseInstance instance3 = caseService.createCaseInstanceByKey("oneTaskCase",
        Collections.singletonMap("var", "bValue"));

    // when I make a task query with ascending variable ordering by tasks variables
    List<Task> tasks = taskService.createTaskQuery()
      .caseDefinitionKey("oneTaskCase")
      .orderByCaseInstanceVariable("var", ValueType.STRING)
      .asc()
      .list();

    // then the tasks are ordered correctly by their local variables
    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getCaseInstanceId()).isEqualTo(instance2.getId());
    assertThat(tasks.get(1).getCaseInstanceId()).isEqualTo(instance3.getId());
    assertThat(tasks.get(2).getCaseInstanceId()).isEqualTo(instance1.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn")
  @Test
  void testQueryResultOrderingByCaseExecutionVariables() {
    // given three tasks with String case instance variables
    CaseInstance instance1 = caseService.createCaseInstanceByKey("oneTaskCase",
        Collections.singletonMap("var", "cValue"));
    CaseInstance instance2 = caseService.createCaseInstanceByKey("oneTaskCase",
        Collections.singletonMap("var", "aValue"));
    CaseInstance instance3 = caseService.createCaseInstanceByKey("oneTaskCase",
        Collections.singletonMap("var", "bValue"));

    // and local case execution variables
    CaseExecution caseExecution1 = caseService.createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .caseInstanceId(instance1.getId())
      .singleResult();

    caseService
      .withCaseExecution(caseExecution1.getId())
      .setVariableLocal("var", "aValue")
      .manualStart();

    CaseExecution caseExecution2 = caseService.createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .caseInstanceId(instance2.getId())
      .singleResult();

    caseService
      .withCaseExecution(caseExecution2.getId())
      .setVariableLocal("var", "bValue")
      .manualStart();

    CaseExecution caseExecution3 = caseService.createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .caseInstanceId(instance3.getId())
      .singleResult();

    caseService
      .withCaseExecution(caseExecution3.getId())
      .setVariableLocal("var", "cValue")
      .manualStart();

    // when I make a task query with ascending variable ordering by tasks variables
    List<Task> tasks = taskService.createTaskQuery()
        .caseDefinitionKey("oneTaskCase")
        .orderByCaseExecutionVariable("var", ValueType.STRING)
        .asc()
        .list();

    // then the tasks are ordered correctly by their local variables
    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getCaseInstanceId()).isEqualTo(instance1.getId());
    assertThat(tasks.get(1).getCaseInstanceId()).isEqualTo(instance2.getId());
    assertThat(tasks.get(2).getCaseInstanceId()).isEqualTo(instance3.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
  @Test
  void testQueryResultOrderingByVariablesWithNullValues() {
    // given three tasks with String process instance variables
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "bValue"));
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "cValue"));
    ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "aValue"));
    ProcessInstance instance4 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when I make a task query with variable ordering by String values
    List<Task> tasks = taskService.createTaskQuery()
        .processDefinitionKey("oneTaskProcess")
        .orderByProcessVariable("var", ValueType.STRING)
        .asc()
        .list();

    Task firstTask = tasks.get(0);

    // the null-valued task should be either first or last
    if (firstTask.getProcessInstanceId().equals(instance4.getId())) {
      // then the others in ascending order
      assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance3.getId());
      assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance1.getId());
      assertThat(tasks.get(3).getProcessInstanceId()).isEqualTo(instance2.getId());
    } else {
      assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance3.getId());
      assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance1.getId());
      assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance2.getId());
      assertThat(tasks.get(3).getProcessInstanceId()).isEqualTo(instance4.getId());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
  @Test
  void testQueryResultOrderingByVariablesWithMixedTypes() {
    // given three tasks with String and Integer process instance variables
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 42));
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "cValue"));
    ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "aValue"));

    // when I make a task query with variable ordering by String values
    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.STRING)
      .asc()
      .list();

    Task firstTask = tasks.get(0);

    // the numeric-valued task should be either first or last
    if (firstTask.getProcessInstanceId().equals(instance1.getId())) {
      // then the others in ascending order
      assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance3.getId());
      assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance2.getId());
    } else {
      assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance3.getId());
      assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance2.getId());
      assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance1.getId());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
  @Test
  void testQueryResultOrderingByStringVariableWithMixedCase() {
    // given three tasks with String and Integer process instance variables
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "a"));
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "B"));
    ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "c"));

    // when I make a task query with variable ordering by String values
    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.STRING)
      .asc()
      .list();

    // then the tasks are ordered correctly
    assertThat(tasks).hasSize(3);
    // first the numeric valued task (since it is treated like null-valued)
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance1.getId());
    // then the others in alphabetical order
    assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance2.getId());
    assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance3.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
  @Test
  void testQueryResultOrderingByVariablesOfAllPrimitiveTypes() {
    // given three tasks with String and Integer process instance variables
    ProcessInstance booleanInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", true));
    ProcessInstance shortInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", (short) 16));
    ProcessInstance longInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 500L));
    ProcessInstance intInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 400));
    ProcessInstance stringInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", "300"));
    ProcessInstance dateInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", new Date(1000L)));
    ProcessInstance doubleInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 42.5d));

    // when I make a task query with variable ordering by String values
    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.BOOLEAN)
      .asc()
      .list();

    verifyFirstOrLastTask(tasks, booleanInstance);

    tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.SHORT)
      .asc()
      .list();

    verifyFirstOrLastTask(tasks, shortInstance);

    tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.LONG)
      .asc()
      .list();

    verifyFirstOrLastTask(tasks, longInstance);

    tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.INTEGER)
      .asc()
      .list();

    verifyFirstOrLastTask(tasks, intInstance);

    tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.STRING)
      .asc()
      .list();

    verifyFirstOrLastTask(tasks, stringInstance);

    tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.DATE)
      .asc()
      .list();

    verifyFirstOrLastTask(tasks, dateInstance);

    tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.DOUBLE)
      .asc()
      .list();

    verifyFirstOrLastTask(tasks, doubleInstance);
  }

  @Test
  void testQueryByUnsupportedValueTypes() {
    var taskQuery = taskService.createTaskQuery();
    try {
      taskQuery.orderByProcessVariable("var", ValueType.BYTES);
      fail("this type is not supported");
    } catch (ProcessEngineException e) {
      // happy path
      testRule.assertTextPresent("Cannot order by variables of type byte", e.getMessage());
    }

    try {
      taskQuery.orderByProcessVariable("var", ValueType.NULL);
      fail("this type is not supported");
    } catch (ProcessEngineException e) {
      // happy path
      testRule.assertTextPresent("Cannot order by variables of type null", e.getMessage());
    }

    try {
      taskQuery.orderByProcessVariable("var", ValueType.NUMBER);
      fail("this type is not supported");
    } catch (ProcessEngineException e) {
      // happy path
      testRule.assertTextPresent("Cannot order by variables of type number", e.getMessage());
    }

    try {
      taskQuery.orderByProcessVariable("var", ValueType.OBJECT);
      fail("this type is not supported");
    } catch (ProcessEngineException e) {
      // happy path
      testRule.assertTextPresent("Cannot order by variables of type object", e.getMessage());
    }

    try {
      taskQuery.orderByProcessVariable("var", ValueType.FILE);
      fail("this type is not supported");
    } catch (ProcessEngineException e) {
      // happy path
      testRule.assertTextPresent("Cannot order by variables of type file", e.getMessage());
    }
  }

  /**
   * verify that either the first or the last task of the list belong to the given process instance
   */
  protected void verifyFirstOrLastTask(List<Task> tasks, ProcessInstance belongingProcessInstance) {
    if (tasks.isEmpty()) {
      fail("no tasks given");
    }

    int numTasks = tasks.size();
    boolean matches = tasks.get(0).getProcessInstanceId().equals(belongingProcessInstance.getId());
    matches = matches || tasks.get(numTasks - 1).getProcessInstanceId()
        .equals(belongingProcessInstance.getId());

    assertThat(matches).as("neither first nor last task belong to process instance " + belongingProcessInstance.getId()).isTrue();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
  @Test
  void testQueryResultOrderingByVariablesWithMixedTypesAndSameColumn() {
    // given three tasks with Integer and Long process instance variables
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 42));
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 800));
    ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Collections.singletonMap("var", 500L));

    // when I make a task query with variable ordering by String values
    List<Task> tasks = taskService.createTaskQuery()
        .processDefinitionKey("oneTaskProcess")
        .orderByProcessVariable("var", ValueType.INTEGER)
        .asc()
        .list();

    // then the tasks are ordered correctly
    assertThat(tasks).hasSize(3);

    Task firstTask = tasks.get(0);

    // the Long-valued task should be either first or last
    if (firstTask.getProcessInstanceId().equals(instance3.getId())) {
      // then the others in ascending order
      assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance1.getId());
      assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance2.getId());
    } else {
      assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance1.getId());
      assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance2.getId());
      assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance3.getId());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
  @Test
  void testQueryResultOrderingByTwoVariables() {
    // given three tasks with String process instance variables
    ProcessInstance bInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "b").putValue("var2", 14));
    ProcessInstance bInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "b").putValue("var2", 30));
    ProcessInstance cInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "c").putValue("var2", 50));
    ProcessInstance cInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "c").putValue("var2", 30));
    ProcessInstance aInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "a").putValue("var2", 14));
    ProcessInstance aInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "a").putValue("var2", 50));

    // when I make a task query with variable primary ordering by var values
    // and secondary ordering by var2 values
    List<Task> tasks = taskService.createTaskQuery()
        .processDefinitionKey("oneTaskProcess")
        .orderByProcessVariable("var", ValueType.STRING)
        .desc()
        .orderByProcessVariable("var2", ValueType.INTEGER)
        .asc()
        .list();

    // then the tasks are ordered correctly
    assertThat(tasks).hasSize(6);
    // var = c; var2 = 30
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(cInstance2.getId());
    // var = c; var2 = 50
    assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(cInstance1.getId());
    // var = b; var2 = 14
    assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(bInstance1.getId());
    // var = b; var2 = 30
    assertThat(tasks.get(3).getProcessInstanceId()).isEqualTo(bInstance2.getId());
    // var = a; var2 = 14
    assertThat(tasks.get(4).getProcessInstanceId()).isEqualTo(aInstance1.getId());
    // var = a; var2 = 50
    assertThat(tasks.get(5).getProcessInstanceId()).isEqualTo(aInstance2.getId());

    // when I make a task query with variable primary ordering by var2 values
    // and secondary ordering by var values
    tasks = taskService.createTaskQuery()
        .processDefinitionKey("oneTaskProcess")
        .orderByProcessVariable("var2", ValueType.INTEGER)
        .desc()
        .orderByProcessVariable("var", ValueType.STRING)
        .asc()
        .list();

    // then the tasks are ordered correctly
    assertThat(tasks).hasSize(6);
    // var = a; var2 = 50
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(aInstance2.getId());
    // var = c; var2 = 50
    assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(cInstance1.getId());
    // var = b; var2 = 30
    assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(bInstance2.getId());
    // var = c; var2 = 30
    assertThat(tasks.get(3).getProcessInstanceId()).isEqualTo(cInstance2.getId());
    // var = a; var2 = 14
    assertThat(tasks.get(4).getProcessInstanceId()).isEqualTo(aInstance1.getId());
    // var = b; var2 = 14
    assertThat(tasks.get(5).getProcessInstanceId()).isEqualTo(bInstance1.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
  @Test
  void testQueryResultOrderingByVariablesWithSecondaryOrderingByProcessInstanceId() {
    // given three tasks with String process instance variables
    ProcessInstance bInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "b"));
    ProcessInstance bInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "b"));
    ProcessInstance cInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "c"));
    ProcessInstance cInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "c"));
    ProcessInstance aInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "a"));
    ProcessInstance aInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", "a"));

    // when I make a task query with variable ordering by String values
    List<Task> tasks = taskService.createTaskQuery()
        .processDefinitionKey("oneTaskProcess")
        .orderByProcessVariable("var", ValueType.STRING)
        .asc()
        .orderByProcessInstanceId()
        .asc()
        .list();

    // then the tasks are ordered correctly
    assertThat(tasks).hasSize(6);

    // var = a
    verifyTasksSortedByProcessInstanceId(List.of(aInstance1, aInstance2),
        tasks.subList(0, 2));

    // var = b
    verifyTasksSortedByProcessInstanceId(List.of(bInstance1, bInstance2),
        tasks.subList(2, 4));

    // var = c
    verifyTasksSortedByProcessInstanceId(List.of(cInstance1, cInstance2),
        tasks.subList(4, 6));
  }

  @Test
  void testQueryResultOrderingWithInvalidParameters() {
    var taskQuery = taskService.createTaskQuery();
    assertThatThrownBy(() -> taskQuery.orderByProcessVariable(null, ValueType.STRING)).isInstanceOf(NullValueException.class);

    assertThatThrownBy(() -> taskQuery.orderByProcessVariable("var", null)).isInstanceOf(NullValueException.class);

    assertThatThrownBy(() -> taskQuery.orderByExecutionVariable(null, ValueType.STRING)).isInstanceOf(NullValueException.class);

    assertThatThrownBy(() -> taskQuery.orderByExecutionVariable("var", null)).isInstanceOf(NullValueException.class);

    assertThatThrownBy(() -> taskQuery.orderByTaskVariable(null, ValueType.STRING)).isInstanceOf(NullValueException.class);

    assertThatThrownBy(() -> taskQuery.orderByTaskVariable("var", null)).isInstanceOf(NullValueException.class);

    assertThatThrownBy(() -> taskQuery.orderByCaseInstanceVariable(null, ValueType.STRING)).isInstanceOf(NullValueException.class);

    assertThatThrownBy(() -> taskQuery.orderByCaseInstanceVariable("var", null)).isInstanceOf(NullValueException.class);

    assertThatThrownBy(() -> taskQuery.orderByCaseExecutionVariable(null, ValueType.STRING)).isInstanceOf(NullValueException.class);

    assertThatThrownBy(() -> taskQuery.orderByCaseExecutionVariable("var", null)).isInstanceOf(NullValueException.class);
  }

  protected void verifyTasksSortedByProcessInstanceId(List<ProcessInstance> expectedProcessInstances,
      List<Task> actualTasks) {

    assertThat(actualTasks).hasSize(expectedProcessInstances.size());
    List<ProcessInstance> instances = new ArrayList<>(expectedProcessInstances);

    instances.sort(Comparator.comparing(Execution::getId));

    for (int i = 0; i < instances.size(); i++) {
      assertThat(actualTasks.get(i).getProcessInstanceId()).isEqualTo(instances.get(i).getId());
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/oneTaskWithFormKeyProcess.bpmn20.xml"})
  @Test
  void testInitializeFormKeys() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // if initializeFormKeys
    Task task = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .initializeFormKeys()
      .singleResult();

    // then the form key is present
    assertThat(task.getFormKey()).isEqualTo("exampleFormKey");

    // if NOT initializeFormKeys
    task = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .singleResult();

    try {
      // then the form key is not retrievable
      task.getFormKey();
      fail("exception expected.");
    } catch (BadUserRequestException e) {
      assertThat(e.getMessage()).isEqualTo("ENGINE-03052 The form key / form reference is not initialized. You must call initializeFormKeys() on the task query before you can retrieve the form key or the form reference.");
    }
  }

  @Test
  @Deployment
  void testInitializeFormKeysOperatonFormRef() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("formRefProcess");

    // if initializeFormKeys
    Task task = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .initializeFormKeys()
        .singleResult();

    // then the form key is present
    OperatonFormRef operatonFormRef = task.getOperatonFormRef();
    assertThat(operatonFormRef.getKey()).isEqualTo("myForm");
    assertThat(operatonFormRef.getBinding()).isEqualTo("latest");
    assertThat(operatonFormRef.getVersion()).isNull();

    // if NOT initializeFormKeys
    Task task2 = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertThatThrownBy(task2::getOperatonFormRef).isInstanceOf(BadUserRequestException.class)
    .hasMessage("ENGINE-03052 The form key / form reference is not initialized. You must call initializeFormKeys() on the task query before you can retrieve the form key or the form reference.");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithFormKey.cmmn"})
  @Test
  void testInitializeFormKeysForCaseInstance() {
    String caseDefinitionId = getCaseDefinitionId();

    CaseInstance caseInstance = caseService
      .withCaseDefinition(caseDefinitionId)
      .create();

    Task task = taskService.createTaskQuery().initializeFormKeys().caseInstanceId(caseInstance.getId()).singleResult();
    assertThat(task.getFormKey()).isEqualTo("aFormKey");

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
  @Test
  void testQueryOrderByProcessVariableInteger() {
    ProcessInstance instance500 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", 500));
    ProcessInstance instance1000 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", 1000));
    ProcessInstance instance250 = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("var", 250));

    // asc
    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.INTEGER)
      .asc()
      .list();

    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance250.getId());
    assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance500.getId());
    assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance1000.getId());

    // desc
    tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.INTEGER)
      .desc()
      .list();

    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getProcessInstanceId()).isEqualTo(instance1000.getId());
    assertThat(tasks.get(1).getProcessInstanceId()).isEqualTo(instance500.getId());
    assertThat(tasks.get(2).getProcessInstanceId()).isEqualTo(instance250.getId());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
  @Test
  void testQueryOrderByTaskVariableInteger() {
    ProcessInstance instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance instance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance instance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task task500 = taskService.createTaskQuery().processInstanceId(instance1.getId()).singleResult();
    taskService.setVariableLocal(task500.getId(), "var", 500);

    Task task250 = taskService.createTaskQuery().processInstanceId(instance2.getId()).singleResult();
    taskService.setVariableLocal(task250.getId(), "var", 250);

    Task task1000 = taskService.createTaskQuery().processInstanceId(instance3.getId()).singleResult();
    taskService.setVariableLocal(task1000.getId(), "var", 1000);

    // asc
    List<Task> tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByTaskVariable("var", ValueType.INTEGER)
      .asc()
      .list();

    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getId()).isEqualTo(task250.getId());
    assertThat(tasks.get(1).getId()).isEqualTo(task500.getId());
    assertThat(tasks.get(2).getId()).isEqualTo(task1000.getId());

    // desc
    tasks = taskService.createTaskQuery()
      .processDefinitionKey("oneTaskProcess")
      .orderByProcessVariable("var", ValueType.INTEGER)
      .desc()
      .list();

    assertThat(tasks).hasSize(3);
    assertThat(tasks.get(0).getId()).isEqualTo(task1000.getId());
    assertThat(tasks.get(1).getId()).isEqualTo(task500.getId());
    assertThat(tasks.get(2).getId()).isEqualTo(task250.getId());
  }

  @Test
  void testQueryByParentTaskId() {
    String parentTaskId = "parentTask";
    Task parent = taskService.newTask(parentTaskId);
    taskService.saveTask(parent);

    Task sub1 = taskService.newTask("subTask1");
    sub1.setParentTaskId(parentTaskId);
    taskService.saveTask(sub1);

    Task sub2 = taskService.newTask("subTask2");
    sub2.setParentTaskId(parentTaskId);
    taskService.saveTask(sub2);

    TaskQuery query = taskService.createTaskQuery().taskParentTaskId(parentTaskId);

    verifyQueryResults(query, 2);

    taskService.deleteTask(parentTaskId, true);
  }

  @Test
  void testExtendTaskQueryList_ProcessDefinitionKeyIn() {
    // given
    String processDefinitionKey = "invoice";
    TaskQuery query = taskService
        .createTaskQuery()
        .processDefinitionKeyIn(processDefinitionKey);

    TaskQuery extendingQuery = taskService.createTaskQuery();

    // when
    TaskQuery result = ((TaskQueryImpl)query).extend(extendingQuery);

    // then
    String[] processDefinitionKeys = ((TaskQueryImpl) result).getProcessDefinitionKeys();
    assertThat(processDefinitionKeys).hasSize(1);
    assertThat(processDefinitionKeys[0]).isEqualTo(processDefinitionKey);
  }

  @Test
  void testExtendingTaskQueryList_ProcessDefinitionKeyIn() {
    // given
    String processDefinitionKey = "invoice";
    TaskQuery query = taskService.createTaskQuery();

    TaskQuery extendingQuery = taskService
        .createTaskQuery()
        .processDefinitionKeyIn(processDefinitionKey);

    // when
    TaskQuery result = ((TaskQueryImpl)query).extend(extendingQuery);

    // then
    String[] processDefinitionKeys = ((TaskQueryImpl) result).getProcessDefinitionKeys();
    assertThat(processDefinitionKeys).hasSize(1);
    assertThat(processDefinitionKeys[0]).isEqualTo(processDefinitionKey);
  }

  @Test
  void testExtendTaskQueryList_TaskDefinitionKeyIn() {
    // given
    String taskDefinitionKey = "assigneApprover";
    TaskQuery query = taskService
        .createTaskQuery()
        .taskDefinitionKeyIn(taskDefinitionKey);

    TaskQuery extendingQuery = taskService.createTaskQuery();

    // when
    TaskQuery result = ((TaskQueryImpl)query).extend(extendingQuery);

    // then
    String[] key = ((TaskQueryImpl) result).getKeys();
    assertThat(key).hasSize(1);
    assertThat(key[0]).isEqualTo(taskDefinitionKey);
  }

  @Test
  void testExtendingTaskQueryList_TaskDefinitionKeyIn() {
    // given
    String taskDefinitionKey = "assigneApprover";
    TaskQuery query = taskService.createTaskQuery();

    TaskQuery extendingQuery = taskService
        .createTaskQuery()
        .taskDefinitionKeyIn(taskDefinitionKey);

    // when
    TaskQuery result = ((TaskQueryImpl)query).extend(extendingQuery);

    // then
    String[] key = ((TaskQueryImpl) result).getKeys();
    assertThat(key).hasSize(1);
    assertThat(key[0]).isEqualTo(taskDefinitionKey);
  }

  @Test
  void testExtendTaskQueryList_TaskDefinitionKeyNotIn() {
    // given
    var taskDefinitionKey = "someKey";
    var query = taskService.createTaskQuery()
        .taskDefinitionKeyNotIn(taskDefinitionKey);

    var extendingQuery = taskService.createTaskQuery();

    // when
    var result = ((TaskQueryImpl)query).extend(extendingQuery);

    // then
    assertThat(((TaskQueryImpl) result).getKeyNotIn())
            .containsExactly(taskDefinitionKey);
  }

  @Test
  void testExtendingTaskQueryList_TaskDefinitionKeyNotIn() {
    // given
    var taskDefinitionKey = "someKey";
    var query = taskService.createTaskQuery();

    var extendingQuery = taskService
        .createTaskQuery()
        .taskDefinitionKeyNotIn(taskDefinitionKey);

    // when
    var result = ((TaskQueryImpl)query).extend(extendingQuery);

    // then
    assertThat(((TaskQueryImpl) result).getKeyNotIn())
            .containsExactly(taskDefinitionKey);
  }

  @Test
  void testQueryWithCandidateUsers() {
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
      .userTask()
        .operatonCandidateUsers("anna")
      .endEvent()
      .done();

   testRule.deploy(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    List<Task> tasks = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .withCandidateUsers()
        .list();
    assertThat(tasks).hasSize(1);
  }

  @Test
  void testQueryWithoutCandidateUsers() {
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
      .userTask()
        .operatonCandidateGroups("sales")
      .endEvent()
      .done();

   testRule.deploy(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    List<Task> tasks = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .withoutCandidateUsers()
        .list();
    assertThat(tasks).hasSize(1);
  }

  @Test
  void testQueryAssignedTasksWithCandidateUsers() {
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
      .userTask()
        .operatonCandidateUsers("anna")
        .operatonCandidateGroups("sales")
      .endEvent()
      .done();

   testRule.deploy(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    var taskQuery = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId());

    assertThatThrownBy(taskQuery::includeAssignedTasks).isInstanceOf(ProcessEngineException.class);
  }


  @Test
  void testQueryAssignedTasksWithoutCandidateUsers() {
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
      .userTask()
        .operatonCandidateGroups("sales")
      .endEvent()
      .done();

   testRule.deploy(process);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    var taskQuery = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId());

    assertThatThrownBy(taskQuery::includeAssignedTasks).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByNameNotEqual() {
    TaskQuery query = taskService.createTaskQuery().taskNameNotEqual("gonzo_Task");
    assertThat(query.list()).hasSize(11);
  }

  @Test
  void testQueryByNameNotLike() {
    TaskQuery query = taskService.createTaskQuery().taskNameNotLike("management%");
    assertThat(query.list()).hasSize(9);
    assertThat(query.count()).isEqualTo(9);

    query = taskService.createTaskQuery().taskNameNotLike("gonzo\\_%");
    assertThat(query.list()).hasSize(11);
    assertThat(query.count()).isEqualTo(11);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
  @Test
  void testQueryByProcessInstanceIdIn() {
    // given three process instances
    String instance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
    String instance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when filter for two of them and non existing one
    List<Task> tasks = taskService.createTaskQuery().processInstanceIdIn(instance1, instance2, "nonexisting").list();

    // then
    assertThat(tasks).hasSize(2);
    for (Task task : tasks) {
      assertThat(task.getProcessInstanceId()).isIn(instance1, instance2);
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.testProcessDefinition.bpmn20.xml")
  @Test
  void testQueryByProcessInstanceIdInNonExisting() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    List<Task> tasks = taskService.createTaskQuery().processInstanceIdIn("nonexisting").list();

    // then
    assertThat(tasks).isEmpty();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/task/TaskQueryTest.shouldContainOperatonFormRefIfInitialized.bpmn")
  @Test
  void shouldContainOperatonFormRefIfInitialized() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskFormRefVersion").getId();

    // when
    List<Task> withoutFormKeys = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
    List<Task> withFormKeys = taskService.createTaskQuery().processInstanceId(processInstanceId).initializeFormKeys().list();

    // then
    assertThat(withoutFormKeys).hasSize(1);
    assertThat(withFormKeys).hasSize(1);

    Task taskWithoutFormKey = withoutFormKeys.get(0);
    assertThatThrownBy(taskWithoutFormKey::getOperatonFormRef).isInstanceOf(BadUserRequestException.class)
    .hasMessage("ENGINE-03052 The form key / form reference is not initialized. You must call initializeFormKeys() on the task query before you can retrieve the form key or the form reference.");

    Task taskWithFormKey = withFormKeys.get(0);
    OperatonFormRef operatonFormRefWithFormKey = taskWithFormKey.getOperatonFormRef();

    assertThat(operatonFormRefWithFormKey).isNotNull();
    assertThat(operatonFormRefWithFormKey.getKey()).isEqualTo("key");
    assertThat(operatonFormRefWithFormKey.getBinding()).isEqualTo("version");
    assertThat(operatonFormRefWithFormKey.getVersion()).isEqualTo(3);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void shouldFindTaskLastUpdatedNullUseCreateDate() {
    // given
    Date beforeStart = getBeforeCurrentTime();
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // when
    // no update to task, lastUpdated = null

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskUpdatedAfter(beforeStart).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isNull();
    assertThat(taskResult.getCreateTime()).isAfter(beforeStart);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void shouldNotFindTaskLastUpdatedNullCreateDateBeforeQueryDate() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Date afterStart = getAfterCurrentTime();

    // when
    // no update to task, lastUpdated = null

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskUpdatedAfter(afterStart).singleResult();
    assertThat(taskResult).isNull();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void shouldNotFindTaskLastUpdatedBeforeQueryDate() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    Date lastUpdatedBeforeUpdate = task.getLastUpdated();
    task.setAssignee("myself");

    // assume
    assertThat(lastUpdatedBeforeUpdate).isNull();

    // when
    taskService.saveTask(task);

    // then
    Date afterUpdate = getAfterCurrentTime();
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskUpdatedAfter(afterUpdate).singleResult();
    assertThat(taskResult).isNull();
    taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isBefore(afterUpdate);
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void shouldNotFindTaskLastUpdatedEqualsQueryDate() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    task.setAssignee("myself");

    // when
    taskService.saveTask(task);

    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isNotNull();
    taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskUpdatedAfter(taskResult.getLastUpdated()).singleResult();
    assertThat(taskResult).isNull();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void shouldReturnResultsOrderedByLastUpdatedAsc() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Date beforeUpdates = getBeforeCurrentTime();

    Task task2 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
    taskService.setOwner(task2.getId(), "myself");

    ClockUtil.offset(1000L);

    Task task1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
    taskService.setVariableLocal(task1.getId(), "myVar", "varVal");

    ClockUtil.offset(2000L);

    Task task3 = taskService.createTaskQuery().processInstanceId(processInstance3.getId()).singleResult();
    taskService.setPriority(task3.getId(), 3);

    // when
    List<Task> tasks = taskService.createTaskQuery()
        .processInstanceIdIn(processInstance1.getId(), processInstance2.getId(), processInstance3.getId())
        .taskUpdatedAfter(beforeUpdates).orderByLastUpdated().asc().list();

    // then
    assertThat(tasks).hasSize(3);
    assertThat(tasks).extracting("id").containsExactly(task2.getId(), task1.getId(), task3.getId());
    assertThat(tasks).extracting("lastUpdated").isSorted();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void shouldReturnResultsOrderedByLastUpdatedDesc() {
    // given
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Date beforeUpdates = getBeforeCurrentTime();

    Task task2 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
    taskService.setOwner(task2.getId(), "myself");

    ClockUtil.offset(1000L);

    Task task1 = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
    taskService.setVariableLocal(task1.getId(), "myVar", "varVal");

    ClockUtil.offset(2000L);

    Task task3 = taskService.createTaskQuery().processInstanceId(processInstance3.getId()).singleResult();
    taskService.setPriority(task3.getId(), 3);

    // when
    List<Task> tasks = taskService.createTaskQuery()
        .processInstanceIdIn(processInstance1.getId(), processInstance2.getId(), processInstance3.getId())
        .taskUpdatedAfter(beforeUpdates).orderByLastUpdated().desc().list();

    // then
    assertThat(tasks).hasSize(3);
    assertThat(tasks).extracting("id").containsExactly(task3.getId(), task1.getId(), task2.getId());
    assertThat(tasks).extracting("lastUpdated").isSortedAccordingTo(Collections.reverseOrder());
  }

  @Test
  void shouldFindStandaloneTaskWithoutUpdateByLastUpdated() {
    // given
    Date beforeCreateTask = getBeforeCurrentTime();
    Task task = taskService.newTask("myTask");
    taskIds.add(task.getId());
    task.setAssignee("myself");
    Date beforeSave = getAfterCurrentTime();

    // when
    taskService.saveTask(task);

    // then
    Task taskResult = taskService.createTaskQuery().taskId("myTask").taskUpdatedAfter(beforeCreateTask).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isNull();
    assertThat(taskResult.getCreateTime()).isBefore(beforeSave);
  }

  @Test
  void shouldFindStandaloneTaskWithUpdateByLastUpdated() {
    // given
    Task task = taskService.newTask("myTask");
    taskIds.add(task.getId());
    task.setAssignee("myself");
    Date beforeSave = getBeforeCurrentTime();
    taskService.saveTask(task);
    // make sure time passes between create and update
    ClockUtil.offset(1000L);

    // when
    taskService.setPriority(task.getId(), 2);

    // then
    Task taskResult = taskService.createTaskQuery().taskId("myTask").taskUpdatedAfter(beforeSave).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.getLastUpdated()).isAfter(beforeSave);
    assertThat(taskResult.getLastUpdated()).isAfter(taskResult.getCreateTime());
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void shouldNotFindAttachmentAndCommentInfoWithoutQueryParam() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    task.setAssignee("myself");
    // when
    taskService.createComment(task.getId(), processInstance.getId(), "testComment");
    taskService.createAttachment("foo", task.getId(), processInstance.getId(), "testAttachment", "testDesc", "http://operaton.org");
    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.hasComment()).isFalse();
    assertThat(taskResult.hasAttachment()).isFalse();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  void shouldFindAttachmentAndCommentInfoWithQueryParam() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    // when
    taskService.createComment(task.getId(), processInstance.getId(), "testComment");
    taskService.createAttachment("foo", task.getId(), processInstance.getId(), "testAttachment",  "testDesc", "http://operaton.org");
    // then
    Task taskResult = taskService.createTaskQuery().processInstanceId(processInstance.getId()).withCommentAttachmentInfo().singleResult();
    assertThat(taskResult).isNotNull();
    assertThat(taskResult.hasComment()).isTrue();
    assertThat(taskResult.hasAttachment()).isTrue();
  }
  // ---------------------- HELPER ------------------------------

  // make sure that time passes between two fast operations
  private Date getAfterCurrentTime() {
    return new Date(ClockUtil.getCurrentTime().getTime() + 1000L);
  }

  //make sure that time passes between two fast operations
  private Date getBeforeCurrentTime() {
    return new Date(ClockUtil.getCurrentTime().getTime() - 1000L);
  }

  /**
   * Generates some test tasks.
   * - 6 tasks where kermit is a candidate
   * - 1 tasks where gonzo is assignee and kermit and gonzo are candidates
   * - 2 tasks assigned to management group
   * - 2 tasks assigned to accountancy group
   * - 1 task assigned to fozzie and to both the management and accountancy group
   */
  private List<String> generateTestTasks() throws Exception {
    List<String> ids = new ArrayList<>();

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");
    // 6 tasks for kermit
    ClockUtil.setCurrentTime(sdf.parse("01/01/2001 01:01:01.000"));
    for (int i = 0; i < 6; i++) {
      Task task = taskService.newTask();
      task.setName("testTask");
      task.setDescription("testTask description");
      task.setPriority(3);
      taskService.saveTask(task);
      ids.add(task.getId());
      taskService.addCandidateUser(task.getId(), "kermit");
    }

    ClockUtil.setCurrentTime(sdf.parse("02/02/2002 02:02:02.000"));
    // 1 task for gonzo
    Task task = taskService.newTask();
    task.setName("gonzo_Task");
    task.setDescription("gonzo_description");
    task.setPriority(4);
    taskService.saveTask(task);
    taskService.setAssignee(task.getId(), "gonzo_");
    taskService.setVariable(task.getId(), "testVar", "someVariable");
    taskService.addCandidateUser(task.getId(), "kermit");
    taskService.addCandidateUser(task.getId(), "gonzo");
    ids.add(task.getId());

    ClockUtil.setCurrentTime(sdf.parse("03/03/2003 03:03:03.000"));
    // 2 tasks for management group
    for (int i = 0; i < 2; i++) {
      task = taskService.newTask();
      task.setName("managementTask");
      task.setPriority(10);
      taskService.saveTask(task);
      taskService.addCandidateGroup(task.getId(), "management");
      ids.add(task.getId());
    }

    ClockUtil.setCurrentTime(sdf.parse("04/04/2004 04:04:04.000"));
    // 2 tasks for accountancy group
    for (int i = 0; i < 2; i++) {
      task = taskService.newTask();
      task.setName("accountancyTask");
      task.setName("accountancy description");
      taskService.saveTask(task);
      taskService.addCandidateGroup(task.getId(), "accountancy");
      ids.add(task.getId());
    }

    ClockUtil.setCurrentTime(sdf.parse("05/05/2005 05:05:05.000"));
    // 1 task assigned to management and accountancy group
    task = taskService.newTask();
    task.setName("managementAndAccountancyTask");
    taskService.saveTask(task);
    taskService.setAssignee(task.getId(), "fozzie");
    taskService.addCandidateGroup(task.getId(), "management");
    taskService.addCandidateGroup(task.getId(), "accountancy");
    ids.add(task.getId());

    ClockUtil.reset();

    return ids;
  }

}
