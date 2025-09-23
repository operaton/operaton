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
package org.operaton.bpm.engine.test.history.useroperationlog;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.task.DelegationState;
import org.operaton.bpm.engine.task.Task;

import static org.operaton.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_CREATE;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_DELETE;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.OPERATION_TYPE_UPDATE;
import static org.operaton.bpm.engine.impl.persistence.entity.TaskEntity.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Danny Gräf
 */
class UserOperationLogTaskServiceAndBeanTest extends AbstractUserOperationLogTest {

  protected Task task;

  @AfterEach
  void tearDown() {


    if (task != null) {
      taskService.deleteTask(task.getId(), true);
    }
  }

  @Test
  void testBeanPropertyChanges() {
    TaskEntity entity = new TaskEntity();

    // assign and validate changes
    entity.setAssignee("icke");
    Map<String, PropertyChange> changes = entity.getPropertyChanges();
    assertThat(changes).hasSize(1);
    assertThat(changes.get(ASSIGNEE).getOrgValue()).isNull();
    assertThat(changes.get(ASSIGNEE).getNewValue()).isEqualTo("icke");

    // assign it again
    entity.setAssignee("er");
    changes = entity.getPropertyChanges();
    assertThat(changes).hasSize(1);

    // original value is still null because the task was not saved
    assertThat(changes.get(ASSIGNEE).getOrgValue()).isNull();
    assertThat(changes.get(ASSIGNEE).getNewValue()).isEqualTo("er");

    // set a due date
    entity.setDueDate(new Date());
    changes = entity.getPropertyChanges();
    assertThat(changes).hasSize(2);
  }

  @Test
  void testNotTrackChangeToTheSameValue() {
    TaskEntity entity = new TaskEntity();

    // get and set a properties
    entity.setPriority(entity.getPriority());
    entity.setOwner(entity.getOwner());
    entity.setFollowUpDate(entity.getFollowUpDate());

    // should not track this change
    assertThat(entity.getPropertyChanges()).isEmpty();
  }

  @Test
  void testRemoveChangeWhenSetBackToTheOrgValue() {
    TaskEntity entity = new TaskEntity();

    // set an owner (default is null)
    entity.setOwner("icke");

    // should track this change
    assertThat(entity.getPropertyChanges()).isNotEmpty();

    // reset the owner
    entity.setOwner(null);

    // the change is removed
    assertThat(entity.getPropertyChanges()).isEmpty();
  }

  @Test
  void testAllTrackedProperties() {
    Date yesterday = new Date(new Date().getTime() - 86400000);
    Date tomorrow = new Date(new Date().getTime() + 86400000);

    TaskEntity entity = new TaskEntity();

    // call all tracked setter methods
    entity.setAssignee("er");
    entity.setDelegationState(DelegationState.PENDING);
    entity.setDeleted(true);
    entity.setDescription("a description");
    entity.setDueDate(tomorrow);
    entity.setFollowUpDate(yesterday);
    entity.setName("to do");
    entity.setOwner("icke");
    entity.setParentTaskId("parent");
    entity.setPriority(73);

    // and validate the change list
    Map<String, PropertyChange> changes = entity.getPropertyChanges();
    assertThat(changes.get(ASSIGNEE).getNewValue()).isEqualTo("er");
    assertThat(changes.get(DELEGATION).getNewValue()).isSameAs(DelegationState.PENDING);
    assertThat(changes.get(DESCRIPTION).getNewValue()).isEqualTo("a description");
    assertThat(changes.get(DUE_DATE).getNewValue()).isEqualTo(tomorrow);
    assertThat(changes.get(FOLLOW_UP_DATE).getNewValue()).isEqualTo(yesterday);
    assertThat(changes.get(NAME).getNewValue()).isEqualTo("to do");
    assertThat(changes.get(OWNER).getNewValue()).isEqualTo("icke");
    assertThat(changes.get(PARENT_TASK).getNewValue()).isEqualTo("parent");
    assertThat(changes.get(PRIORITY).getNewValue()).isEqualTo(73);

    // DELETE property is not validated here; it is set directly on task deletion
  }

  @Test
  void testDeleteTask() {
    // given: a single task
    task = taskService.newTask();
    taskService.saveTask(task);

    // then: delete the task
    taskService.deleteTask(task.getId(), "duplicated");

    // expect: one entry for the deletion
    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_DELETE);
    assertThat(query.count()).isEqualTo(1);

    // assert: details
    UserOperationLogEntry delete = query.singleResult();
    assertThat(delete.getProperty()).isEqualTo(DELETE);
    assertThat(Boolean.parseBoolean(delete.getOrgValue())).isFalse();
    assertThat(Boolean.parseBoolean(delete.getNewValue())).isTrue();
    assertThat(delete.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Test
  void testCompositeBeanInteraction() {
    // given: a manually created task
    task = taskService.newTask();

    // then: save the task without any property change
    taskService.saveTask(task);

    // expect: no entry
    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_CREATE);
    UserOperationLogEntry create = query.singleResult();
    assertThat(create).isNotNull();
    assertThat(create.getEntityType()).isEqualTo(EntityTypes.TASK);
    assertThat(create.getOrgValue()).isNull();
    assertThat(create.getNewValue()).isNull();
    assertThat(create.getProperty()).isNull();
    assertThat(create.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);

    task.setAssignee("icke");
    task.setName("to do");

    // then: save the task again
    taskService.saveTask(task);

    // expect: two update entries with the same operation id
    List<UserOperationLogEntry> entries = queryOperationDetails(OPERATION_TYPE_UPDATE).list();
    assertThat(entries).hasSize(2);
    assertThat(entries.get(1).getOperationId()).isEqualTo(entries.get(0).getOperationId());
    assertThat(entries.get(0).getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);
    assertThat(entries.get(1).getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Test
  void testMultipleValueChange() {
    // given: a single task
    task = taskService.newTask();
    taskService.saveTask(task);

    // then: change a property twice
    task.setName("a task");
    task.setName("to do");
    taskService.saveTask(task);
    UserOperationLogEntry update = queryOperationDetails(OPERATION_TYPE_UPDATE).singleResult();
    assertThat(update.getOrgValue()).isNull();
    assertThat(update.getNewValue()).isEqualTo("to do");
    assertThat(update.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_TASK_WORKER);
  }

  @Test
  void testSetDateProperty() {
    // given: a single task
    task = taskService.newTask();
    Date now = ClockUtil.getCurrentTime();
    task.setDueDate(now);
    taskService.saveTask(task);

    UserOperationLogEntry logEntry = historyService.createUserOperationLogQuery().singleResult();
    assertThat(logEntry.getNewValue()).isEqualTo(String.valueOf(now.getTime()));
  }

  @Test
  void testResetChange() {
    // given: a single task
    task = taskService.newTask();
    taskService.saveTask(task);

    // then: change the name
    String name = "a task";
    task.setName(name);
    taskService.saveTask(task);
    UserOperationLogEntry update = queryOperationDetails(OPERATION_TYPE_UPDATE).singleResult();
    assertThat(update.getOrgValue()).isNull();
    assertThat(update.getNewValue()).isEqualTo(name);

    // then: change the name some times and set it back to the original value
    task.setName("to do 1");
    task.setName("to do 2");
    task.setName(name);
    taskService.saveTask(task);

    // expect: there is no additional change tracked
    update = queryOperationDetails(OPERATION_TYPE_UPDATE).singleResult();
    assertThat(update.getOrgValue()).isNull();
    assertThat(update.getNewValue()).isEqualTo(name);
  }

  @Test
  void testConcurrentTaskChange() {
    // create a task
    task = taskService.newTask();
    taskService.saveTask(task);

    // change the bean property
    task.setAssignee("icke");

    // use the service method to do an other assignment
    taskService.setAssignee(task.getId(), "er");

    try { // now try to save the task and overwrite the change
      taskService.saveTask(task);
    } catch (Exception e) {
      assertThat(e).isNotNull(); // concurrent modification
    }
  }

  @Test
  void testCaseInstanceId() {
    // create new task
    task = taskService.newTask();
    taskService.saveTask(task);

    UserOperationLogQuery query = queryOperationDetails(OPERATION_TYPE_UPDATE);
    assertThat(query.count()).isZero();

    // set case instance id and save task
    task.setCaseInstanceId("aCaseInstanceId");
    taskService.saveTask(task);

    assertThat(query.count()).isEqualTo(1);

    UserOperationLogEntry entry = query.singleResult();
    assertThat(entry).isNotNull();

    assertThat(entry.getOrgValue()).isNull();
    assertThat(entry.getNewValue()).isEqualTo("aCaseInstanceId");
    assertThat(entry.getProperty()).isEqualTo(CASE_INSTANCE_ID);

    // change case instance id and save task
    task.setCaseInstanceId("anotherCaseInstanceId");
    taskService.saveTask(task);

    assertThat(query.count()).isEqualTo(2);

    List<UserOperationLogEntry> entries = query.list();
    assertThat(entries).hasSize(2);

    for (UserOperationLogEntry currentEntry : entries) {
      if (!currentEntry.getId().equals(entry.getId())) {
        assertThat(currentEntry.getOrgValue()).isEqualTo("aCaseInstanceId");
        assertThat(currentEntry.getNewValue()).isEqualTo("anotherCaseInstanceId");
        assertThat(currentEntry.getProperty()).isEqualTo(CASE_INSTANCE_ID);
      }
    }
  }

  private UserOperationLogQuery queryOperationDetails(String type) {
    return historyService.createUserOperationLogQuery().operationType(type);
  }

}
