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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.task.Event;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.task.IdentityLinkType;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.tasklistener.util.GetIdentityLinksTaskListener;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.commons.testing.ProcessEngineLoggingRule;
import org.junit.Rule;
import org.junit.Test;

import ch.qos.logback.classic.Level;



/**
 * @author Tom Baeyens
 * @author Falko Menge
 */
public class TaskIdentityLinksTest extends PluggableProcessEngineTest {

  @Rule
  public ProcessEngineLoggingRule loggingRule = new ProcessEngineLoggingRule().level(Level.ERROR);

  @Deployment(resources="org/operaton/bpm/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
  @Test
  public void testCandidateUserLink() {
    runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

    String taskId = taskService
      .createTaskQuery()
      .singleResult()
      .getId();

    taskService.addCandidateUser(taskId, "kermit");

    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
    IdentityLink identityLink = identityLinks.get(0);

    assertNull(identityLink.getGroupId());
    assertThat(identityLink.getUserId()).isEqualTo("kermit");
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
    assertThat(identityLink.getTaskId()).isEqualTo(taskId);

    assertThat(identityLinks.size()).isEqualTo(1);

    identityLinks = taskService.getIdentityLinksForTask(taskId);
    assertThat(identityLinks.size()).isEqualTo(1);

    taskService.deleteCandidateUser(taskId, "kermit");

    assertThat(taskService.getIdentityLinksForTask(taskId).size()).isEqualTo(0);
  }

  @Deployment(resources="org/operaton/bpm/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
  @Test
  public void testCandidateGroupLink() {
    try {
      identityService.setAuthenticatedUserId("demo");

      runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

      String taskId = taskService
          .createTaskQuery()
          .singleResult()
          .getId();

      taskService.addCandidateGroup(taskId, "muppets");

      List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
      IdentityLink identityLink = identityLinks.get(0);

      assertThat(identityLink.getGroupId()).isEqualTo("muppets");
      assertNull("kermit", identityLink.getUserId());
      assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.CANDIDATE);
      assertThat(identityLink.getTaskId()).isEqualTo(taskId);

      assertThat(identityLinks.size()).isEqualTo(1);

      if (processEngineConfiguration.getHistoryLevel().getId()>= ProcessEngineConfigurationImpl.HISTORYLEVEL_FULL) {
        List<Event> taskEvents = taskService.getTaskEvents(taskId);
        assertThat(taskEvents.size()).isEqualTo(1);
        Event taskEvent = taskEvents.get(0);
        assertThat(taskEvent.getAction()).isEqualTo(Event.ACTION_ADD_GROUP_LINK);
        List<String> taskEventMessageParts = taskEvent.getMessageParts();
        assertThat(taskEventMessageParts.get(0)).isEqualTo("muppets");
        assertThat(taskEventMessageParts.get(1)).isEqualTo(IdentityLinkType.CANDIDATE);
        assertThat(taskEventMessageParts.size()).isEqualTo(2);
      }

      taskService.deleteCandidateGroup(taskId, "muppets");

      if (processEngineConfiguration.getHistoryLevel().getId()>= ProcessEngineConfigurationImpl.HISTORYLEVEL_FULL) {
        List<Event> taskEvents = taskService.getTaskEvents(taskId);
        Event taskEvent = findTaskEvent(taskEvents, Event.ACTION_DELETE_GROUP_LINK);
        assertThat(taskEvent.getAction()).isEqualTo(Event.ACTION_DELETE_GROUP_LINK);
        List<String> taskEventMessageParts = taskEvent.getMessageParts();
        assertThat(taskEventMessageParts.get(0)).isEqualTo("muppets");
        assertThat(taskEventMessageParts.get(1)).isEqualTo(IdentityLinkType.CANDIDATE);
        assertThat(taskEventMessageParts.size()).isEqualTo(2);
        assertThat(taskEvents.size()).isEqualTo(2);
      }

      assertThat(taskService.getIdentityLinksForTask(taskId).size()).isEqualTo(0);
    } finally {
      identityService.clearAuthentication();
    }
  }

  @Test
  public void testAssigneeLink() {
    Task task = taskService.newTask("task");
    task.setAssignee("assignee");
    taskService.saveTask(task);

    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
    assertNotNull(identityLinks);
    assertThat(identityLinks.size()).isEqualTo(1);

    IdentityLink identityLink = identityLinks.get(0);
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.ASSIGNEE);
    assertThat(identityLink.getUserId()).isEqualTo("assignee");
    assertThat(identityLink.getTaskId()).isEqualTo("task");

    // second call should return the same list size
    identityLinks = taskService.getIdentityLinksForTask(task.getId());
    assertThat(identityLinks.size()).isEqualTo(1);

    taskService.deleteTask(task.getId(), true);
  }

  @Test
  public void testOwnerLink() {
    Task task = taskService.newTask("task");
    task.setOwner("owner");
    taskService.saveTask(task);

    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(task.getId());
    assertNotNull(identityLinks);
    assertThat(identityLinks.size()).isEqualTo(1);

    IdentityLink identityLink = identityLinks.get(0);
    assertThat(identityLink.getType()).isEqualTo(IdentityLinkType.OWNER);
    assertThat(identityLink.getUserId()).isEqualTo("owner");
    assertThat(identityLink.getTaskId()).isEqualTo("task");

    // second call should return the same list size
    identityLinks = taskService.getIdentityLinksForTask(task.getId());
    assertThat(identityLinks.size()).isEqualTo(1);

    taskService.deleteTask(task.getId(), true);
  }

  private Event findTaskEvent(List<Event> taskEvents, String action) {
    for (Event event: taskEvents) {
      if (action.equals(event.getAction())) {
        return event;
      }
    }
    throw new AssertionError("no task event found with action "+action);
  }

  @Deployment(resources="org/operaton/bpm/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
  @Test
  public void testCustomTypeUserLink() {
    runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

    String taskId = taskService
      .createTaskQuery()
      .singleResult()
      .getId();

    taskService.addUserIdentityLink(taskId, "kermit", "interestee");

    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
    IdentityLink identityLink = identityLinks.get(0);

    assertNull(identityLink.getGroupId());
    assertThat(identityLink.getUserId()).isEqualTo("kermit");
    assertThat(identityLink.getType()).isEqualTo("interestee");
    assertThat(identityLink.getTaskId()).isEqualTo(taskId);

    assertThat(identityLinks.size()).isEqualTo(1);

    taskService.deleteUserIdentityLink(taskId, "kermit", "interestee");

    assertThat(taskService.getIdentityLinksForTask(taskId).size()).isEqualTo(0);
  }

  @Deployment(resources="org/operaton/bpm/engine/test/api/task/IdentityLinksProcess.bpmn20.xml")
  @Test
  public void testCustomLinkGroupLink() {
    runtimeService.startProcessInstanceByKey("IdentityLinksProcess");

    String taskId = taskService
      .createTaskQuery()
      .singleResult()
      .getId();

    taskService.addGroupIdentityLink(taskId, "muppets", "playing");

    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
    IdentityLink identityLink = identityLinks.get(0);

    assertThat(identityLink.getGroupId()).isEqualTo("muppets");
    assertNull("kermit", identityLink.getUserId());
    assertThat(identityLink.getType()).isEqualTo("playing");
    assertThat(identityLink.getTaskId()).isEqualTo(taskId);

    assertThat(identityLinks.size()).isEqualTo(1);

    taskService.deleteGroupIdentityLink(taskId, "muppets", "playing");

    assertThat(taskService.getIdentityLinksForTask(taskId).size()).isEqualTo(0);
  }

  @Test
  public void testDeleteAssignee() {
    Task task = taskService.newTask();
    task.setAssignee("nonExistingUser");
    taskService.saveTask(task);

    taskService.deleteUserIdentityLink(task.getId(), "nonExistingUser", IdentityLinkType.ASSIGNEE);

    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertNull(task.getAssignee());
    assertThat(taskService.getIdentityLinksForTask(task.getId()).size()).isEqualTo(0);

    // cleanup
    taskService.deleteTask(task.getId(), true);
  }

  @Test
  public void testDeleteOwner() {
    Task task = taskService.newTask();
    task.setOwner("nonExistingUser");
    taskService.saveTask(task);

    taskService.deleteUserIdentityLink(task.getId(), "nonExistingUser", IdentityLinkType.OWNER);

    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertNull(task.getOwner());
    assertThat(taskService.getIdentityLinksForTask(task.getId()).size()).isEqualTo(0);

    // cleanup
    taskService.deleteTask(task.getId(), true);
  }

  @Test
  public void testAssigneeGetIdentityLinksInCompleteListener() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask("task1")
        .operatonTaskListenerClass(TaskListener.EVENTNAME_COMPLETE, GetIdentityLinksTaskListener.class.getName())
        .userTask("task2")
        .endEvent()
        .done();

    testRule.deploy(model);
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    Task task = taskService.createTaskQuery().singleResult();
    String taskId = task.getId();
    // create identity links
    task.setAssignee("elmo");
    taskService.saveTask(task);
    taskService.addUserIdentityLink(taskId, "kermit", "interestee");

    // when
    taskService.complete(taskId);

    // then no NPE is thrown and there were 2 identity links during the listener execution
    assertThat(loggingRule.getLog()).isEmpty();
    assertThat(runtimeService.getVariable(processInstanceId, "identityLinksSize")).isEqualTo(2);
    assertThat(runtimeService.getVariable(processInstanceId, "secondCallidentityLinksSize")).isEqualTo(2);
  }

  @Test
  public void testOwnerGetIdentityLinksInCompleteListener() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask("task1")
        .operatonTaskListenerClass(TaskListener.EVENTNAME_COMPLETE, GetIdentityLinksTaskListener.class.getName())
        .userTask("task2")
        .endEvent()
        .done();

    testRule.deploy(model);
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    Task task = taskService.createTaskQuery().singleResult();
    String taskId = task.getId();
    // create identity links
    task.setOwner("gonzo");
    taskService.saveTask(task);
    taskService.addUserIdentityLink(taskId, "kermit", "interestee");

    // when
    taskService.complete(taskId);

    // then no NPE is thrown and there were 2 identity links during the listener execution
    assertThat(loggingRule.getLog()).isEmpty();
    assertThat(runtimeService.getVariable(processInstanceId, "identityLinksSize")).isEqualTo(2);
    assertThat(runtimeService.getVariable(processInstanceId, "secondCallidentityLinksSize")).isEqualTo(2);
  }
}
