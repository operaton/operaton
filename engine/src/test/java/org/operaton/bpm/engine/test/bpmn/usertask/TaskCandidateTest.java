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
package org.operaton.bpm.engine.test.bpmn.usertask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class TaskCandidateTest extends PluggableProcessEngineTest {

  private static final String MANAGEMENT = "management";

  private static final String KERMIT = "kermit";

  private static final String GONZO = "gonzo";

  @Before
  public void setUp() {


    Group accountants = identityService.newGroup("accountancy");
    identityService.saveGroup(accountants);
    Group managers = identityService.newGroup(MANAGEMENT);
    identityService.saveGroup(managers);
    Group sales = identityService.newGroup("sales");
    identityService.saveGroup(sales);

    User kermit = identityService.newUser(KERMIT);
    identityService.saveUser(kermit);
    identityService.createMembership(KERMIT, "accountancy");

    User gonzo = identityService.newUser(GONZO);
    identityService.saveUser(gonzo);
    identityService.createMembership(GONZO, MANAGEMENT);
    identityService.createMembership(GONZO, "accountancy");
    identityService.createMembership(GONZO, "sales");
  }

  @After
  public void tearDown() {
    identityService.deleteUser(KERMIT);
    identityService.deleteUser(GONZO);
    identityService.deleteGroup("sales");
    identityService.deleteGroup("accountancy");
    identityService.deleteGroup(MANAGEMENT);


  }

  @Deployment
  @Test
  public void testSingleCandidateGroup() {

    // Deploy and start process
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("singleCandidateGroup");

    // Task should not yet be assigned to kermit
    List<Task> tasks = taskService
      .createTaskQuery()
      .taskAssignee(KERMIT)
      .list();
    assertTrue(tasks.isEmpty());

    // The task should be visible in the candidate task list
    tasks = taskService.createTaskQuery().taskCandidateUser(KERMIT).list();
    assertThat(tasks.size()).isEqualTo(1);
    Task task = tasks.get(0);
    assertThat(task.getName()).isEqualTo("Pay out expenses");

    // The above query again, now between 'or' and 'endOr'
    tasks = taskService.createTaskQuery().or().taskCandidateUser(KERMIT).endOr().list();
    assertThat(tasks.size()).isEqualTo(1);
    task = tasks.get(0);
    assertThat(task.getName()).isEqualTo("Pay out expenses");

    // Claim the task
    taskService.claim(task.getId(), KERMIT);

    // The task must now be gone from the candidate task list
    tasks = taskService.createTaskQuery().taskCandidateUser(KERMIT).list();
    assertTrue(tasks.isEmpty());

    // The task will be visible on the personal task list
    tasks = taskService
      .createTaskQuery()
      .taskAssignee(KERMIT)
      .list();
    assertThat(tasks.size()).isEqualTo(1);
    task = tasks.get(0);
    assertThat(task.getName()).isEqualTo("Pay out expenses");

    // Completing the task ends the process
    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  public void testMultipleCandidateGroups() {

    // Deploy and start process
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multipleCandidatesGroup");

    // Task should not yet be assigned to anyone
    List<Task> tasks = taskService
      .createTaskQuery()
      .taskAssignee(KERMIT)
      .list();

    assertTrue(tasks.isEmpty());
    tasks = taskService
      .createTaskQuery()
      .taskAssignee(GONZO)
      .list();

    assertTrue(tasks.isEmpty());

    // The task should be visible in the candidate task list of Gonzo and Kermit
    // and anyone in the management/accountancy group
    assertThat(taskService.createTaskQuery().taskCandidateUser(KERMIT).list().size()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateUser(GONZO).list().size()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateGroup(MANAGEMENT).count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateGroup("accountancy").count()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateGroup("sales").count()).isEqualTo(0);

    // Gonzo claims the task
    tasks = taskService.createTaskQuery().taskCandidateUser(GONZO).list();
    Task task = tasks.get(0);
    assertThat(task.getName()).isEqualTo("Approve expenses");
    taskService.claim(task.getId(), GONZO);

    // The task must now be gone from the candidate task lists
    assertTrue(taskService.createTaskQuery().taskCandidateUser(KERMIT).list().isEmpty());
    assertTrue(taskService.createTaskQuery().taskCandidateUser(GONZO).list().isEmpty());
    assertThat(taskService.createTaskQuery().taskCandidateGroup(MANAGEMENT).count()).isEqualTo(0);

    // The task will be visible on the personal task list of Gonzo
    assertThat(taskService
        .createTaskQuery()
        .taskAssignee(GONZO)
        .count()).isEqualTo(1);

    // But not on the personal task list of (for example) Kermit
    assertThat(taskService.createTaskQuery().taskAssignee(KERMIT).count()).isEqualTo(0);

    // Completing the task ends the process
    taskService.complete(task.getId());

    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  public void testMultipleCandidateUsers() {
    runtimeService.startProcessInstanceByKey("multipleCandidateUsersExample");

    assertThat(taskService.createTaskQuery().taskCandidateUser(GONZO).list().size()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateUser(KERMIT).list().size()).isEqualTo(1);
  }

  @Deployment
  @Test
  public void testMixedCandidateUserAndGroup() {
    runtimeService.startProcessInstanceByKey("mixedCandidateUserAndGroupExample");

    assertThat(taskService.createTaskQuery().taskCandidateUser(GONZO).list().size()).isEqualTo(1);
    assertThat(taskService.createTaskQuery().taskCandidateUser(KERMIT).list().size()).isEqualTo(1);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/usertask/groupTest.bpmn")
  @Test
  public void testInvolvedUserQuery() {

    // given
    identityService.createMembership(KERMIT, MANAGEMENT);

    runtimeService.startProcessInstanceByKey("Process_13pqtqg");

    // when
    List<Task> tasks = taskService.createTaskQuery()
        .taskInvolvedUser(KERMIT)
        .list();

    assertThat(tasks).hasSize(1);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  public void testInvolvedUserQueryForAssignee() {

    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.setAssignee(task.getId(), KERMIT);

    // when
    List<Task> tasks = taskService.createTaskQuery()
        .taskInvolvedUser(KERMIT)
        .list();

    // then
    assertThat(tasks).hasSize(1);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
  @Test
  public void testInvolvedUserQueryForOwner() {

    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.setOwner(task.getId(), KERMIT);

    // when
    List<Task> tasks = taskService.createTaskQuery()
        .taskInvolvedUser(KERMIT)
        .list();

    // then
    assertThat(tasks).hasSize(1);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/usertask/groupTest.bpmn")
  @Test
  public void testInvolvedUserQueryOr() {

    // given
    identityService.createMembership(KERMIT, MANAGEMENT);

    runtimeService.startProcessInstanceByKey("Process_13pqtqg");

    // when
    List<Task> tasks = taskService.createTaskQuery()
        .or()
          .taskCandidateGroup(MANAGEMENT)
          .taskInvolvedUser(KERMIT)
        .endOr()
        .list();

    assertThat(tasks).hasSize(2);
  }
}
