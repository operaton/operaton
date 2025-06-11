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
package org.operaton.bpm.engine.test.standalone.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.el.ExpressionManager;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.task.TaskDecorator;
import org.operaton.bpm.engine.impl.task.TaskDefinition;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.task.IdentityLinkType;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Roman Smirnov
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class TaskDecoratorTest {

  protected TaskEntity task;
  protected TaskDefinition taskDefinition;
  protected TaskDecorator taskDecorator;
  protected ExpressionManager expressionManager;

  ProcessEngineConfigurationImpl processEngineConfiguration;
  TaskService taskService;

  @BeforeEach
  void setUp() {
    task = (TaskEntity) taskService.newTask();
    taskService.saveTask(task);

    expressionManager = processEngineConfiguration
        .getExpressionManager();

    taskDefinition = new TaskDefinition(null);
    taskDecorator = new TaskDecorator(taskDefinition, expressionManager);
  }

  @AfterEach
  void tearDown() {
    taskService.deleteTask(task.getId(), true);
  }

  protected void decorate(TaskEntity task, TaskDecorator decorator) {
    processEngineConfiguration
      .getCommandExecutorTxRequired()
      .execute(new DecorateTaskCommand(task, decorator));
  }

  @Test
  void testDecorateName() {
    // given
    String aTaskName = "A Task Name";
    Expression nameExpression = expressionManager.createExpression(aTaskName);
    taskDefinition.setNameExpression(nameExpression);

    // when
    decorate(task, taskDecorator);

    // then
    assertThat(task.getName()).isEqualTo(aTaskName);
  }

  @Test
  void testDecorateNameFromVariable() {
    // given
    String aTaskName = "A Task Name";
    taskService.setVariable(task.getId(), "taskName", aTaskName);

    Expression nameExpression = expressionManager.createExpression("${taskName}");
    taskDefinition.setNameExpression(nameExpression);

    // when
    decorate(task, taskDecorator);

    // then
    assertThat(task.getName()).isEqualTo(aTaskName);
  }

  @Test
  void testDecorateDescription() {
    // given
    String aDescription = "This is a Task";
    Expression descriptionExpression = expressionManager.createExpression(aDescription);
    taskDefinition.setDescriptionExpression(descriptionExpression);

    // when
    decorate(task, taskDecorator);

    // then
    assertThat(task.getDescription()).isEqualTo(aDescription);
  }

  @Test
  void testDecorateDescriptionFromVariable() {
    // given
    String aDescription = "This is a Task";
    taskService.setVariable(task.getId(), "description", aDescription);

    Expression descriptionExpression = expressionManager.createExpression("${description}");
    taskDefinition.setDescriptionExpression(descriptionExpression);

    // when
    decorate(task, taskDecorator);

    // then
    assertThat(task.getDescription()).isEqualTo(aDescription);
  }

  @Test
  void testDecorateDueDate() {
    // given
    String aDueDate = "2014-06-01";
    Date dueDate = DateTimeUtil.parseDate(aDueDate);

    Expression dueDateExpression = expressionManager.createExpression(aDueDate);
    taskDefinition.setDueDateExpression(dueDateExpression);

    // when
    decorate(task, taskDecorator);

    // then
    assertThat(task.getDueDate()).isEqualTo(dueDate);
  }

  @Test
  void testDecorateDueDateFromVariable() {
    // given
    String aDueDate = "2014-06-01";
    Date dueDate = DateTimeUtil.parseDate(aDueDate);
    taskService.setVariable(task.getId(), "dueDate", dueDate);

    Expression dueDateExpression = expressionManager.createExpression("${dueDate}");
    taskDefinition.setDueDateExpression(dueDateExpression);

    // when
    decorate(task, taskDecorator);

    // then
    assertThat(task.getDueDate()).isEqualTo(dueDate);
  }

  @Test
  void testDecorateFollowUpDate() {
    // given
    String aFollowUpDate = "2014-06-01";
    Date followUpDate = DateTimeUtil.parseDate(aFollowUpDate);

    Expression followUpDateExpression = expressionManager.createExpression(aFollowUpDate);
    taskDefinition.setFollowUpDateExpression(followUpDateExpression);

    // when
    decorate(task, taskDecorator);

    // then
    assertThat(task.getFollowUpDate()).isEqualTo(followUpDate);
  }

  @Test
  void testDecorateFollowUpDateFromVariable() {
    // given
    String aFollowUpDateDate = "2014-06-01";
    Date followUpDate = DateTimeUtil.parseDate(aFollowUpDateDate);
    taskService.setVariable(task.getId(), "followUpDate", followUpDate);

    Expression followUpDateExpression = expressionManager.createExpression("${followUpDate}");
    taskDefinition.setFollowUpDateExpression(followUpDateExpression);

    // when
    decorate(task, taskDecorator);

    // then
    assertThat(task.getFollowUpDate()).isEqualTo(followUpDate);
  }

  @Test
  void testDecoratePriority() {
    // given
    String aPriority = "10";
    Expression priorityExpression = expressionManager.createExpression(aPriority);
    taskDefinition.setPriorityExpression(priorityExpression);

    // when
    decorate(task, taskDecorator);

    // then
    assertThat(task.getPriority()).isEqualTo(Integer.parseInt(aPriority));
  }

  @Test
  void testDecoratePriorityFromVariable() {
    // given
    int aPriority = 10;
    taskService.setVariable(task.getId(), "priority", aPriority);

    Expression priorityExpression = expressionManager.createExpression("${priority}");
    taskDefinition.setPriorityExpression(priorityExpression);

    // when
    decorate(task, taskDecorator);

    // then
    assertThat(task.getPriority()).isEqualTo(aPriority);
  }

  @Test
  void testDecorateAssignee() {
    // given
    String aAssignee = "john";
    Expression assigneeExpression = expressionManager.createExpression(aAssignee);
    taskDefinition.setAssigneeExpression(assigneeExpression);

    // when
    decorate(task, taskDecorator);

    // then
    assertThat(task.getAssignee()).isEqualTo(aAssignee);
  }

  @Test
  void testDecorateAssigneeFromVariable() {
    // given
    String aAssignee = "john";
    taskService.setVariable(task.getId(), "assignee", aAssignee);

    Expression assigneeExpression = expressionManager.createExpression("${assignee}");
    taskDefinition.setAssigneeExpression(assigneeExpression);

    // when
    decorate(task, taskDecorator);

    // then
    assertThat(task.getAssignee()).isEqualTo(aAssignee);
  }

  @Test
  void testDecorateCandidateUsers() {
    // given
    List<String> aCandidateUserList = new ArrayList<>();
    aCandidateUserList.add("john");
    aCandidateUserList.add("peter");
    aCandidateUserList.add("mary");

    for (String candidateUser : aCandidateUserList) {
      Expression candidateUserExpression = expressionManager.createExpression(candidateUser);
      taskDefinition.addCandidateUserIdExpression(candidateUserExpression);
    }

    // when
    decorate(task, taskDecorator);

    // then
    Set<IdentityLink> candidates = task.getCandidates();
    assertThat(candidates).hasSize(3);

    for (IdentityLink identityLink : candidates) {
      String taskId = identityLink.getTaskId();
      assertThat(taskId).isEqualTo(task.getId());

      assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);

      String userId = identityLink.getUserId();
      if ("john".equals(userId)) {
        assertThat(userId).isEqualTo("john");
      } else if ("peter".equals(userId)) {
        assertThat(userId).isEqualTo("peter");
      } else if ("mary".equals(userId)) {
        assertThat(userId).isEqualTo("mary");
      } else {
        fail("Unexpected user: " + userId);
      }
    }

  }

  @Test
  void testDecorateCandidateUsersFromVariable() {
    // given
    taskService.setVariable(task.getId(), "john", "john");
    taskService.setVariable(task.getId(), "peter", "peter");
    taskService.setVariable(task.getId(), "mary", "mary");

    List<String> aCandidateUserList = new ArrayList<>();
    aCandidateUserList.add("${john}");
    aCandidateUserList.add("${peter}");
    aCandidateUserList.add("${mary}");

    for (String candidateUser : aCandidateUserList) {
      Expression candidateUserExpression = expressionManager.createExpression(candidateUser);
      taskDefinition.addCandidateUserIdExpression(candidateUserExpression);
    }

    // when
    decorate(task, taskDecorator);

    // then
    Set<IdentityLink> candidates = task.getCandidates();
    assertThat(candidates).hasSize(3);

    for (IdentityLink identityLink : candidates) {
      String taskId = identityLink.getTaskId();
      assertThat(taskId).isEqualTo(task.getId());

      assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);

      String userId = identityLink.getUserId();
      if ("john".equals(userId)) {
        assertThat(userId).isEqualTo("john");
      } else if ("peter".equals(userId)) {
        assertThat(userId).isEqualTo("peter");
      } else if ("mary".equals(userId)) {
        assertThat(userId).isEqualTo("mary");
      } else {
        fail("Unexpected user: " + userId);
      }
    }
  }

  @Test
  void testDecorateCandidateGroups() {
    // given
    List<String> aCandidateGroupList = new ArrayList<>();
    aCandidateGroupList.add("management");
    aCandidateGroupList.add("accounting");
    aCandidateGroupList.add("backoffice");

    for (String candidateGroup : aCandidateGroupList) {
      Expression candidateGroupExpression = expressionManager.createExpression(candidateGroup);
      taskDefinition.addCandidateGroupIdExpression(candidateGroupExpression);
    }

    // when
    decorate(task, taskDecorator);

    // then
    Set<IdentityLink> candidates = task.getCandidates();
    assertThat(candidates).hasSize(3);

    for (IdentityLink identityLink : candidates) {
      String taskId = identityLink.getTaskId();
      assertThat(taskId).isEqualTo(task.getId());

      assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);

      String groupId = identityLink.getGroupId();
      if ("management".equals(groupId)) {
        assertThat(groupId).isEqualTo("management");
      } else if ("accounting".equals(groupId)) {
        assertThat(groupId).isEqualTo("accounting");
      } else if ("backoffice".equals(groupId)) {
        assertThat(groupId).isEqualTo("backoffice");
      } else {
        fail("Unexpected group: " + groupId);
      }
    }

  }

  @Test
  void testDecorateCandidateGroupsFromVariable() {
    // given
    taskService.setVariable(task.getId(), "management", "management");
    taskService.setVariable(task.getId(), "accounting", "accounting");
    taskService.setVariable(task.getId(), "backoffice", "backoffice");

    List<String> aCandidateGroupList = new ArrayList<>();
    aCandidateGroupList.add("${management}");
    aCandidateGroupList.add("${accounting}");
    aCandidateGroupList.add("${backoffice}");

    for (String candidateGroup : aCandidateGroupList) {
      Expression candidateGroupExpression = expressionManager.createExpression(candidateGroup);
      taskDefinition.addCandidateGroupIdExpression(candidateGroupExpression);
    }

    // when
    decorate(task, taskDecorator);

    // then
    Set<IdentityLink> candidates = task.getCandidates();
    assertThat(candidates).hasSize(3);

    for (IdentityLink identityLink : candidates) {
      String taskId = identityLink.getTaskId();
      assertThat(taskId).isEqualTo(task.getId());

      assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);

      String groupId = identityLink.getGroupId();
      if ("management".equals(groupId)) {
        assertThat(groupId).isEqualTo("management");
      } else if ("accounting".equals(groupId)) {
        assertThat(groupId).isEqualTo("accounting");
      } else if ("backoffice".equals(groupId)) {
        assertThat(groupId).isEqualTo("backoffice");
      } else {
        fail("Unexpected group: " + groupId);
      }
    }
  }

  protected class DecorateTaskCommand implements Command<Void> {

    protected TaskEntity task;
    protected TaskDecorator decorator;

    public DecorateTaskCommand(TaskEntity task, TaskDecorator decorator) {
     this.task = task;
     this.decorator = decorator;
    }

    @Override
    public Void execute(CommandContext commandContext) {
      decorator.decorate(task, task);
      return null;
    }

  }

  protected class DeleteTaskCommand implements Command<Void> {

    protected TaskEntity task;

    public DeleteTaskCommand(TaskEntity task) {
     this.task = task;
    }

    @Override
    public Void execute(CommandContext commandContext) {
      commandContext
        .getTaskManager()
        .deleteTask(task, null, true, false);

      return null;
    }

  }

}
