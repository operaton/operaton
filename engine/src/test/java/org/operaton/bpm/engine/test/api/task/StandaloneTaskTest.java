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
package org.operaton.bpm.engine.test.api.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;

import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class StandaloneTaskTest extends PluggableProcessEngineTest {

  @Before
  public void setUp() {

    identityService.saveUser(identityService.newUser("kermit"));
    identityService.saveUser(identityService.newUser("gonzo"));
  }

  @After
  public void tearDown() {
    identityService.deleteUser("kermit");
    identityService.deleteUser("gonzo");
    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      taskService.deleteTask(task.getId(), true);
    }
  }

  @Test
  public void testCreateToComplete() {

    // Create and save task
    Task task = taskService.newTask();
    task.setName("testTask");
    taskService.saveTask(task);
    String taskId = task.getId();

    // Add user as candidate user
    taskService.addCandidateUser(taskId, "kermit");
    taskService.addCandidateUser(taskId, "gonzo");

    // Retrieve task list for jbarrez
    List<Task> tasks = taskService.createTaskQuery().taskCandidateUser("kermit").list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("testTask");

    // Retrieve task list for tbaeyens
    tasks = taskService.createTaskQuery().taskCandidateUser("gonzo").list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("testTask");

    // Claim task
    taskService.claim(taskId, "kermit");

    // Tasks shouldn't appear in the candidate tasklists anymore
    assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").list()).isEmpty();
    assertThat(taskService.createTaskQuery().taskCandidateUser("gonzo").list()).isEmpty();

    // Complete task
    taskService.deleteTask(taskId, true);

    // Task should be removed from runtime data
    // TODO: check for historic data when implemented!
    assertThat(taskService.createTaskQuery().taskId(taskId).singleResult()).isNull();
  }

  @Test
  public void testOptimisticLockingThrownOnMultipleUpdates() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    // first modification
    Task task1 = taskService.createTaskQuery().taskId(taskId).singleResult();
    Task task2 = taskService.createTaskQuery().taskId(taskId).singleResult();

    task1.setDescription("first modification");
    taskService.saveTask(task1);

    // second modification on the initial instance
    task2.setDescription("second modification");
    try {
      taskService.saveTask(task2);
      fail("should get an exception here as the task was modified by someone else.");
    } catch (OptimisticLockingException expected) {
      //  exception was thrown as expected
    }
  }

  // See http://jira.codehaus.org/browse/ACT-1290
  @Test
  public void testRevisionUpdatedOnSave() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    assertThat(((TaskEntity) task).getRevision()).isEqualTo(1);

    task.setDescription("first modification");
    taskService.saveTask(task);
    assertThat(((TaskEntity) task).getRevision()).isEqualTo(2);

    task.setDescription("second modification");
    taskService.saveTask(task);
    assertThat(((TaskEntity) task).getRevision()).isEqualTo(3);
  }

  @Test
  public void testSaveTaskWithGenericResourceId() {
    Task task = taskService.newTask("*");
    try {
      taskService.saveTask(task);
      fail("it should not be possible to save a task with the generic resource id *");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("Entity Task[*] has an invalid id: id cannot be *. * is a reserved identifier", e.getMessage());
    }
  }

  @Test
  public void shouldSetLastUpdatedOnUpdate() {
    // given
    Task task = taskService.newTask("shouldSetLastUpdatedOnUpdate");
    task.setAssignee("myself");
    taskService.saveTask(task);

    // make sure time passes between save and update
    ClockUtil.offset(1000L);

    // when
    taskService.setVariable(task.getId(), "myVar", "varVal");

    // then
    Task taskResult = taskService.createTaskQuery().taskId("shouldSetLastUpdatedOnUpdate").singleResult();
    assertThat(taskResult.getLastUpdated()).isNotNull();
    assertThat(taskResult.getCreateTime()).isBefore(taskResult.getLastUpdated());
  }

  @Test
  public void shouldNotSetLastUpdatedOnCreate() {
    // given
    Task task = taskService.newTask("shouldNotSetLastUpdatedOnCreate");
    task.setAssignee("myself");

    // when
    taskService.saveTask(task);

    // then
    Task taskResult = taskService.createTaskQuery().taskId("shouldNotSetLastUpdatedOnCreate").singleResult();
    assertThat(taskResult.getLastUpdated()).isNull();
  }

}
