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

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskAlreadyClaimedException;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.TaskServiceImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Attachment;
import org.operaton.bpm.engine.task.Comment;
import org.operaton.bpm.engine.task.DelegationState;
import org.operaton.bpm.engine.task.Event;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.task.IdentityLinkType;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.ProcessEngineBootstrapRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.ProcessBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Falko Menge
 */
public class TaskServiceTest {


  protected static final String TWO_TASKS_PROCESS = "org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml";

  protected static final String USER_TASK_THROW_ERROR = "throw-error";
  protected static final String ERROR_CODE = "300";
  protected static final String ESCALATION_CODE = "432";
  protected static final String PROCESS_KEY = "process";
  protected static final String USER_TASK_AFTER_CATCH = "after-catch";
  protected static final String USER_TASK_AFTER_THROW = "after-throw";
  protected static final String USER_TASK_THROW_ESCALATION = "throw-escalation";

  @ClassRule
  public static ProcessEngineBootstrapRule bootstrapRule = new ProcessEngineBootstrapRule(configuration ->
      configuration.setJavaSerializationFormatEnabled(true));
  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule(bootstrapRule);
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  private RuntimeService runtimeService;
  private TaskService taskService;
  private RepositoryService repositoryService;
  private HistoryService historyService;
  private CaseService caseService;
  private IdentityService identityService;
  private ProcessEngineConfigurationImpl processEngineConfiguration;

  private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss.SSS");

  @Before
  public void init() {
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
    repositoryService = engineRule.getRepositoryService();
    historyService = engineRule.getHistoryService();
    caseService = engineRule.getCaseService();
    identityService = engineRule.getIdentityService();
    processEngineConfiguration = engineRule.getProcessEngineConfiguration();
  }

  @After
  public void tearDown() {
    ClockUtil.setCurrentTime(new Date());
  }

  @Test
  public void testSaveTaskUpdate() throws Exception{

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
    Task task = taskService.newTask();
    task.setDescription("description");
    task.setName("taskname");
    task.setPriority(0);
    task.setAssignee("taskassignee");
    task.setOwner("taskowner");
    Date dueDate = sdf.parse("01/02/2003 04:05:06");
    task.setDueDate(dueDate);
    task.setCaseInstanceId("taskcaseinstanceid");
    taskService.saveTask(task);

    // Fetch the task again and update
    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertThat(task.getDescription()).isEqualTo("description");
    assertThat(task.getName()).isEqualTo("taskname");
    assertThat(task.getAssignee()).isEqualTo("taskassignee");
    assertThat(task.getOwner()).isEqualTo("taskowner");
    assertThat(task.getDueDate()).isEqualTo(dueDate);
    assertThat(task.getPriority()).isEqualTo(0);
    assertThat(task.getCaseInstanceId()).isEqualTo("taskcaseinstanceid");
    assertThat(task.getTaskState()).isEqualTo("Created");

    if (processEngineConfiguration.getHistoryLevel().getId()>= ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      HistoricTaskInstance historicTaskInstance = historyService
        .createHistoricTaskInstanceQuery()
        .taskId(task.getId())
        .singleResult();
      assertThat(historicTaskInstance.getName()).isEqualTo("taskname");
      assertThat(historicTaskInstance.getDescription()).isEqualTo("description");
      assertThat(historicTaskInstance.getAssignee()).isEqualTo("taskassignee");
      assertThat(historicTaskInstance.getOwner()).isEqualTo("taskowner");
      assertThat(historicTaskInstance.getDueDate()).isEqualTo(dueDate);
      assertThat(historicTaskInstance.getPriority()).isEqualTo(0);
      assertThat(historicTaskInstance.getCaseInstanceId()).isEqualTo("taskcaseinstanceid");
    }

    task.setName("updatedtaskname");
    task.setDescription("updateddescription");
    task.setPriority(1);
    task.setAssignee("updatedassignee");
    task.setOwner("updatedowner");
    dueDate = sdf.parse("01/02/2003 04:05:06");
    task.setDueDate(dueDate);
    task.setCaseInstanceId("updatetaskcaseinstanceid");
    taskService.saveTask(task);

    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertThat(task.getName()).isEqualTo("updatedtaskname");
    assertThat(task.getDescription()).isEqualTo("updateddescription");
    assertThat(task.getAssignee()).isEqualTo("updatedassignee");
    assertThat(task.getOwner()).isEqualTo("updatedowner");
    assertThat(task.getDueDate()).isEqualTo(dueDate);
    assertThat(task.getPriority()).isEqualTo(1);
    assertThat(task.getCaseInstanceId()).isEqualTo("updatetaskcaseinstanceid");
    assertThat(task.getTaskState()).isEqualTo("Updated");

    if (processEngineConfiguration.getHistoryLevel().getId()>= ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      HistoricTaskInstance historicTaskInstance = historyService
        .createHistoricTaskInstanceQuery()
        .taskId(task.getId())
        .singleResult();
      assertThat(historicTaskInstance.getName()).isEqualTo("updatedtaskname");
      assertThat(historicTaskInstance.getDescription()).isEqualTo("updateddescription");
      assertThat(historicTaskInstance.getAssignee()).isEqualTo("updatedassignee");
      assertThat(historicTaskInstance.getOwner()).isEqualTo("updatedowner");
      assertThat(historicTaskInstance.getDueDate()).isEqualTo(dueDate);
      assertThat(historicTaskInstance.getPriority()).isEqualTo(1);
      assertThat(historicTaskInstance.getCaseInstanceId()).isEqualTo("updatetaskcaseinstanceid");
    }

    // Finally, delete task
    taskService.deleteTask(task.getId(), true);
  }

  @Test
  public void testSaveTaskSetParentTaskId() {
    // given
    Task parent = taskService.newTask("parent");
    taskService.saveTask(parent);

    Task task = taskService.newTask("subTask");

    // when
    task.setParentTaskId("parent");

    // then
    taskService.saveTask(task);

    // update task
    task = taskService.createTaskQuery().taskId("subTask").singleResult();

    assertThat(task.getParentTaskId()).isEqualTo(parent.getId());

    taskService.deleteTask("parent", true);
    taskService.deleteTask("subTask", true);
  }

  @Test
  public void testSaveTaskWithNonExistingParentTask() {
    // given
    Task task = taskService.newTask();

    // when
    task.setParentTaskId("non-existing");

    // then
    try {
      taskService.saveTask(task);
      fail("It should not be possible to save a task with a non existing parent task.");
    } catch (NotValidException e) {
      // expected
    }
  }

  @Test
  public void testTaskOwner() {
    Task task = taskService.newTask();
    task.setOwner("johndoe");
    taskService.saveTask(task);

    // Fetch the task again and update
    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertThat(task.getOwner()).isEqualTo("johndoe");

    task.setOwner("joesmoe");
    taskService.saveTask(task);

    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertThat(task.getOwner()).isEqualTo("joesmoe");

    // Finally, delete task
    taskService.deleteTask(task.getId(), true);
  }

  @Test
  public void testDeleteTaskCommentNullTaskId() {
    try {
      taskService.deleteTaskComment(null, "test");
      fail("BadUserRequestException expected");
    } catch (BadUserRequestException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testDeleteTaskCommentNotExistingTaskId() {
    try {
      taskService.deleteTaskComment("notExistingId", "notExistingCommentId");
      fail("NullValueException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("No task exists with taskId: notExistingId", ae.getMessage());
    }
  }

  @Test
  public void testDeleteTaskCommentNotExistingCommentId() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    // Deleting non-existing comment should be silently ignored
    assertThatCode(() -> taskService.deleteTaskComment(taskId, "notExistingCommentId"))
      .doesNotThrowAnyException();

    // Finally, delete task
    taskService.deleteTask(taskId, true);
  }

  @Test
  public void testDeleteTaskCommentWithoutProcessInstance() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    //create a task comment
    Comment comment = taskService.createComment(taskId, null, "aMessage");
    String commentId = comment.getId();

    //delete a comment
    taskService.deleteTaskComment(taskId, commentId);

    //make sure the comment is not there.
    Comment shouldBeDeleted = taskService.getTaskComment(taskId, commentId);
    assertNull(shouldBeDeleted);

    // Finally, delete task
    taskService.deleteTask(taskId, true);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testDeleteTaskCommentWithProcessInstance() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    String taskId = task.getId();

    //create a task comment
    Comment comment = taskService.createComment(taskId, processInstance.getId(), "aMessage");
    String commentId = comment.getId();

    //delete a comment
    taskService.deleteTaskComment(taskId, commentId);

    //make sure the comment is not there.
    Comment shouldBeDeleted = taskService.getTaskComment(taskId, commentId);
    assertNull(shouldBeDeleted);
  }

  @Test
  public void testDeleteTaskCommentsNullTaskId() {
    try {
      taskService.deleteTaskComments(null);
      fail("BadUserRequestException expected");
    } catch (BadUserRequestException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testDeleteTaskCommentsNonExistingTaskId() {
    try {
      taskService.deleteTaskComments("nonExistingTaskId");
      fail("NullValueException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("No task exists with taskId:", ae.getMessage());
    }
  }

  @Test
  public void testDeleteTaskCommentsNoComments() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    // Deleting comments of a task that doesn't have any comments should be silently ignored
    assertThatCode(() -> taskService.deleteTaskComments(taskId))
      .doesNotThrowAnyException();

    // Finally, delete task
    taskService.deleteTask(taskId, true);
  }

  @Test
  public void testDeleteTaskComments() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    //create a task comment
    Comment comment = taskService.createComment(taskId, null, "aMessage");

    //delete a comment
    taskService.deleteTaskComments(taskId);

    //make sure the comment is not there.
    Comment shouldBeDeleted = taskService.getTaskComment(taskId, comment.getId());
    assertNull(shouldBeDeleted);

    // Finally, delete task
    taskService.deleteTask(taskId, true);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testDeleteProcessInstanceTaskComments() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    String taskId = task.getId();

    //create a task comment
    Comment comment = taskService.createComment(taskId, processInstance.getId(), "aMessage");

    //delete a comment
    taskService.deleteTaskComments(taskId);

    //make sure the comment is not there.
    Comment shouldBeDeleted = taskService.getTaskComment(taskId, comment.getId());
    assertNull(shouldBeDeleted);
  }

  @Test
  public void testUpdateTaskCommentNullCommentId() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();
    try {
      taskService.updateTaskComment(taskId, null, "aMessage");

      fail("NullValueException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("commentId is null", ae.getMessage());
    } finally {
      taskService.deleteTask(task.getId(), true);
    }
  }

  @Test
  public void testUpdateTaskCommentNullTaskId() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();
    Comment comment = taskService.createComment(taskId, null, "originalMessage");
    var commentId = comment.getId();

    try {
      taskService.updateTaskComment(null, commentId, "updatedMessage");
      fail("BadUserRequestException expected");
    } catch (BadUserRequestException ae) {
      testRule.assertTextPresent("Both process instance and task ids are null", ae.getMessage());
    } finally {
      taskService.deleteTask(taskId, true);
    }
  }

  @Test
  public void testUpdateTaskCommentNullMessage() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();
    Comment comment = taskService.createComment(taskId, null, "originalMessage");
    var commentId = comment.getId();

    try {
      taskService.updateTaskComment(taskId, commentId, null);
      fail("NullValueException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("message is null", ae.getMessage());
    } finally {
      taskService.deleteTask(task.getId(), true);
    }
  }

  @Test
  public void testUpdateTaskCommentNotExistingCommentId() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();
    taskService.createComment(taskId, null, "originalMessage");
    String nonExistingCommentId = "notExistingCommentId";

    try {
      taskService.updateTaskComment(taskId, nonExistingCommentId, "updatedMessage");
      fail("NullValueException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("No comment exists with commentId: " + nonExistingCommentId + " and taskId: " + taskId,
          ae.getMessage());
    } finally {
      taskService.deleteTask(task.getId(), true);
    }
  }

  @Test
  public void testUpdateTaskComment() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();
    Comment comment = taskService.createComment(taskId, null, "originalMessage");
    String updatedMessage = "updatedMessage";

    taskService.updateTaskComment(taskId, comment.getId(), updatedMessage);

    Comment actual = taskService.getTaskComment(taskId, comment.getId());

    assertThat(actual).isNotNull();
    assertThat(actual.getFullMessage()).isEqualTo(updatedMessage);
    // Finally, delete task
    taskService.deleteTask(taskId, true);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testUpdateProcessTaskComment() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    String taskId = task.getId();

    Comment comment = taskService.createComment(taskId, null, "originalMessage");
    String updatedMessage = "updatedMessage";

    taskService.updateTaskComment(taskId, comment.getId(), updatedMessage);

    Comment actual = taskService.getTaskComment(taskId, comment.getId());

    assertThat(actual).isNotNull();
    assertThat(actual.getFullMessage()).isEqualTo(updatedMessage);
  }

  @Test
  public void testDeleteProcessInstanceCommentNullId() {
    try {
      taskService.deleteProcessInstanceComment(null, null);
      fail("BadUserRequestException expected");
    } catch (BadUserRequestException ae) {
      testRule.assertTextPresent("processInstanceId is null", ae.getMessage());
    }
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testDeleteProcessInstanceCommentNotExistingCommentId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    // Deleting non-existing comment should be silently ignored
    assertThatCode(() -> taskService.deleteProcessInstanceComment(processInstanceId, "notExistingCommentId"))
      .doesNotThrowAnyException();
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testDeleteTaskProcessInstanceComment() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    String taskId = task.getId();
    String processInstanceId = processInstance.getId();

    //create a task comment
    Comment comment = taskService.createComment(taskId, null, "aMessage");

    //delete a comment
    taskService.deleteProcessInstanceComment(processInstanceId, comment.getId());

    //make sure the comment is not there.
    List<Comment> shouldBeDeletedLst = taskService.getProcessInstanceComments(processInstanceId);
    assertThat(shouldBeDeletedLst).isEmpty();
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testDeleteProcessInstanceComment() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    //create a task comment
    Comment comment = taskService.createComment(null, processInstanceId, "aMessage");

    //delete a comment
    taskService.deleteProcessInstanceComment(processInstanceId, comment.getId());

    //make sure the comment is not there.
    List<Comment> shouldBeDeletedLst = taskService.getProcessInstanceComments(processInstanceId);
    assertThat(shouldBeDeletedLst).isEmpty();
  }

  @Test
  public void testDeleteProcessInstanceCommentsNullId() {
    try {
      taskService.deleteProcessInstanceComments(null);
      fail("BadUserRequestException expected");
    } catch (BadUserRequestException ae) {
      testRule.assertTextPresent("processInstanceId is null", ae.getMessage());
    }
  }

  @Test
  public void testDeleteProcessInstanceCommentsNonExistingId() {
    try {
      taskService.deleteProcessInstanceComments("nonExistingId");
      fail("NullValueException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("No processInstance exists with processInstanceId:", ae.getMessage());
    }
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testDeleteProcessInstanceCommentsNoComments() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    // Deleting comments of a task that doesn't have any comments should be silently ignored
    assertThatCode(() -> taskService.deleteProcessInstanceComments(processInstanceId))
      .doesNotThrowAnyException();
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testDeleteProcessInstanceCommentsWithoutTaskComments() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    //create a task comment
    taskService.createComment(null, processInstanceId, "messageOne");
    taskService.createComment(null, processInstanceId, "messageTwo");

    //delete a comment
    taskService.deleteProcessInstanceComments(processInstanceId);

    //make sure the comment is not there.
    List<Comment> shouldBeDeletedLst = taskService.getProcessInstanceComments(processInstanceId);
    assertThat(shouldBeDeletedLst).isEmpty();
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testDeleteProcessInstanceCommentsWithTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    String taskId = task.getId();
    String processInstanceId = processInstance.getId();

    //create a task comment
    taskService.createComment(taskId, null, "messageOne");
    taskService.createComment(taskId, null, "messageTwo");

    //delete a comment
    taskService.deleteProcessInstanceComments(processInstanceId);

    //make sure the comment is not there.
    List<Comment> shouldBeDeletedLst = taskService.getProcessInstanceComments(processInstanceId);
    assertThat(shouldBeDeletedLst).isEmpty();
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testUpdateProcessInstanceCommentNullCommentId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    var processInstanceId = processInstance.getId();
    try {
      taskService.updateProcessInstanceComment(processInstanceId, null, "aMessage");
      fail("NullValueException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("commentId is null", ae.getMessage());
    }
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testUpdateProcessInstanceCommentNullProcessInstanceId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Comment comment = taskService.createComment(null, processInstance.getId(), "originalMessage");
    var commentId = comment.getId();

    try {
      taskService.updateProcessInstanceComment(null, commentId, "updatedMessage");
      fail("BadUserRequestException expected");
    } catch (BadUserRequestException ae) {
      testRule.assertTextPresent("Both process instance and task ids are null", ae.getMessage());
    }
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testUpdateProcessInstanceCommentNullMessage() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    Comment comment = taskService.createComment(null, processInstanceId, "originalMessage");
    var commentId = comment.getId();

    try {
      taskService.updateProcessInstanceComment(processInstanceId, commentId, null);
      fail("NullValueException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("message is null", ae.getMessage());
    }
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testUpdateProcessInstanceCommentNotExistingCommentId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    taskService.createComment(null, processInstanceId, "originalMessage");

    String nonExistingCommentId = "notExistingCommentId";
    try {
      taskService.updateProcessInstanceComment(processInstanceId, nonExistingCommentId, "updatedMessage");
      fail("NullValueException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent(
          "No comment exists with commentId: " + nonExistingCommentId + " and processInstanceId: " + processInstanceId,
          ae.getMessage());
    }
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testUpdateProcessInstanceCommentWithTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    String taskId = task.getId();
    String processInstanceId = processInstance.getId();

    Comment comment = taskService.createComment(taskId, processInstanceId, "originalMessage");
    String updatedMessage = "updatedMessage";

    taskService.updateProcessInstanceComment(processInstanceId, comment.getId(), updatedMessage);

    List<Comment> updateCommentLst = taskService.getProcessInstanceComments(processInstanceId);

    assertThat(updateCommentLst).hasSize(1);

    Comment actual = updateCommentLst.get(0);
    assertThat(actual.getFullMessage()).isEqualTo(updatedMessage);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testUpdateProcessInstanceCommentWithoutTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    Comment comment = taskService.createComment(null, processInstanceId, "originalMessage");
    String updatedMessage = "updatedMessage";

    taskService.updateProcessInstanceComment(processInstanceId, comment.getId(), updatedMessage);

    List<Comment> updateCommentLst = taskService.getProcessInstanceComments(processInstanceId);

    assertThat(updateCommentLst).hasSize(1);

    Comment actual = updateCommentLst.get(0);
    assertThat(actual.getFullMessage()).isEqualTo(updatedMessage);
  }

  @Test
  public void testTaskComments() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      Task task = taskService.newTask();
      task.setOwner("johndoe");
      taskService.saveTask(task);
      String taskId = task.getId();

      identityService.setAuthenticatedUserId("johndoe");
      // Fetch the task again and update
      Comment comment = taskService.createComment(taskId, null, "look at this \n       isn't this great? slkdjf sldkfjs ldkfjs ldkfjs ldkfj sldkfj sldkfj sldkjg laksfg sdfgsd;flgkj ksajdhf skjdfh ksjdhf skjdhf kalskjgh lskh dfialurhg kajsh dfuieqpgkja rzvkfnjviuqerhogiuvysbegkjz lkhf ais liasduh flaisduh ajiasudh vaisudhv nsfd");
      assertNotNull(comment.getId());
      assertThat(comment.getUserId()).isEqualTo("johndoe");
      assertThat(comment.getTaskId()).isEqualTo(taskId);
      assertNull(comment.getProcessInstanceId());
      assertThat(((Event) comment).getMessage()).isEqualTo("look at this isn't this great? slkdjf sldkfjs ldkfjs ldkfjs ldkfj sldkfj sldkfj sldkjg laksfg sdfgsd;flgkj ksajdhf skjdfh ksjdhf skjdhf kalskjgh lskh dfialurhg ...");
      assertThat(comment.getFullMessage()).isEqualTo("look at this \n       isn't this great? slkdjf sldkfjs ldkfjs ldkfjs ldkfj sldkfj sldkfj sldkjg laksfg sdfgsd;flgkj ksajdhf skjdfh ksjdhf skjdhf kalskjgh lskh dfialurhg kajsh dfuieqpgkja rzvkfnjviuqerhogiuvysbegkjz lkhf ais liasduh flaisduh ajiasudh vaisudhv nsfd");
      assertNotNull(comment.getTime());

      taskService.createComment(taskId, "pid", "one");
      taskService.createComment(taskId, "pid", "two");

      Set<String> expectedComments = new HashSet<>();
      expectedComments.add("one");
      expectedComments.add("two");

      Set<String> comments = new HashSet<>();
      for (Comment cmt: taskService.getProcessInstanceComments("pid")) {
        comments.add(cmt.getFullMessage());
      }

      assertThat(comments).isEqualTo(expectedComments);

      // Finally, delete task
      taskService.deleteTask(taskId, true);
    }
  }

  @Test
  public void testAddTaskCommentNull() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      Task task = taskService.newTask("testId");
      taskService.saveTask(task);
      var taskId = task.getId();
      try {
        taskService.createComment(taskId, null, null);
        fail("Expected process engine exception");
      }
      catch (ProcessEngineException e) {
        // expected
      }
      finally {
        taskService.deleteTask(task.getId(), true);
      }
    }
  }

  @Test
  public void testAddTaskNullComment() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      try {
        taskService.createComment(null, null, "test");
        fail("Expected process engine exception");
      }
      catch (ProcessEngineException e){
        // expected
      }
    }
  }

  @Test
  public void testTaskAttachments() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      Task task = taskService.newTask();
      task.setOwner("johndoe");
      taskService.saveTask(task);
      String taskId = task.getId();
      identityService.setAuthenticatedUserId("johndoe");
      // Fetch the task again and update
      taskService.createAttachment("web page", taskId, "someprocessinstanceid", "weatherforcast", "temperatures and more", "http://weather.com");
      Attachment attachment = taskService.getTaskAttachments(taskId).get(0);
      assertThat(attachment.getName()).isEqualTo("weatherforcast");
      assertThat(attachment.getDescription()).isEqualTo("temperatures and more");
      assertThat(attachment.getType()).isEqualTo("web page");
      assertThat(attachment.getTaskId()).isEqualTo(taskId);
      assertThat(attachment.getProcessInstanceId()).isEqualTo("someprocessinstanceid");
      assertThat(attachment.getUrl()).isEqualTo("http://weather.com");
      assertNull(taskService.getAttachmentContent(attachment.getId()));

      // Finally, clean up
      taskService.deleteTask(taskId);

      assertThat(taskService.getTaskComments(taskId).size()).isEqualTo(0);
      assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).list().size()).isEqualTo(1);

      taskService.deleteTask(taskId, true);
    }
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  public void testProcessAttachmentsOneProcessExecution() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

      // create attachment
      Attachment attachment = taskService.createAttachment("web page", null, processInstance.getId(), "weatherforcast", "temperatures and more",
          "http://weather.com");

      assertThat(attachment.getName()).isEqualTo("weatherforcast");
      assertThat(attachment.getDescription()).isEqualTo("temperatures and more");
      assertThat(attachment.getType()).isEqualTo("web page");
      assertNull(attachment.getTaskId());
      assertThat(attachment.getProcessInstanceId()).isEqualTo(processInstance.getId());
      assertThat(attachment.getUrl()).isEqualTo("http://weather.com");
      assertNull(taskService.getAttachmentContent(attachment.getId()));
    }
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/twoParallelTasksProcess.bpmn20.xml" })
  public void testProcessAttachmentsTwoProcessExecutions() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoParallelTasksProcess");

      // create attachment
      Attachment attachment = taskService.createAttachment("web page", null, processInstance.getId(), "weatherforcast", "temperatures and more",
          "http://weather.com");

      assertThat(attachment.getName()).isEqualTo("weatherforcast");
      assertThat(attachment.getDescription()).isEqualTo("temperatures and more");
      assertThat(attachment.getType()).isEqualTo("web page");
      assertNull(attachment.getTaskId());
      assertThat(attachment.getProcessInstanceId()).isEqualTo(processInstance.getId());
      assertThat(attachment.getUrl()).isEqualTo("http://weather.com");
      assertNull(taskService.getAttachmentContent(attachment.getId()));
    }
  }

  @Test
  public void testSaveAttachment() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      // given
      Task task = taskService.newTask();
      taskService.saveTask(task);

      String attachmentType = "someAttachment";
      String processInstanceId = "someProcessInstanceId";
      String attachmentName = "attachmentName";
      String attachmentDescription = "attachmentDescription";
      String url = "http://operaton.org";

      Attachment attachment = taskService.createAttachment(
          attachmentType,
          task.getId(),
          processInstanceId,
          attachmentName,
          attachmentDescription,
          url);

      // when
      attachment.setDescription("updatedDescription");
      attachment.setName("updatedName");
      taskService.saveAttachment(attachment);

      // then
      Attachment fetchedAttachment = taskService.getAttachment(attachment.getId());
      assertThat(fetchedAttachment.getId()).isEqualTo(attachment.getId());
      assertThat(fetchedAttachment.getType()).isEqualTo(attachmentType);
      assertThat(fetchedAttachment.getTaskId()).isEqualTo(task.getId());
      assertThat(fetchedAttachment.getProcessInstanceId()).isEqualTo(processInstanceId);
      assertThat(fetchedAttachment.getName()).isEqualTo("updatedName");
      assertThat(fetchedAttachment.getDescription()).isEqualTo("updatedDescription");
      assertThat(fetchedAttachment.getUrl()).isEqualTo(url);

      taskService.deleteTask(task.getId(), true);
    }
  }

  @Test
  public void testTaskDelegation() {
    Task task = taskService.newTask();
    task.setOwner("johndoe");
    task.delegate("joesmoe");
    taskService.saveTask(task);
    String taskId = task.getId();

    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertThat(task.getOwner()).isEqualTo("johndoe");
    assertThat(task.getAssignee()).isEqualTo("joesmoe");
    assertThat(task.getDelegationState()).isEqualTo(DelegationState.PENDING);

    taskService.resolveTask(taskId);
    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertThat(task.getOwner()).isEqualTo("johndoe");
    assertThat(task.getAssignee()).isEqualTo("johndoe");
    assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

    task.setAssignee(null);
    task.setDelegationState(null);
    taskService.saveTask(task);
    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertThat(task.getOwner()).isEqualTo("johndoe");
    assertNull(task.getAssignee());
    assertNull(task.getDelegationState());

    task.setAssignee("jackblack");
    task.setDelegationState(DelegationState.RESOLVED);
    taskService.saveTask(task);
    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertThat(task.getOwner()).isEqualTo("johndoe");
    assertThat(task.getAssignee()).isEqualTo("jackblack");
    assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

    // Finally, delete task
    taskService.deleteTask(taskId, true);
  }

  @Test
  public void testTaskDelegationThroughServiceCall() {
    Task task = taskService.newTask();
    task.setOwner("johndoe");
    taskService.saveTask(task);
    String taskId = task.getId();

    // Fetch the task again and update
    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertThat(task).isNotNull();

    taskService.delegateTask(taskId, "joesmoe");

    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertThat(task.getOwner()).isEqualTo("johndoe");
    assertThat(task.getAssignee()).isEqualTo("joesmoe");
    assertThat(task.getDelegationState()).isEqualTo(DelegationState.PENDING);

    taskService.resolveTask(taskId);

    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertThat(task.getOwner()).isEqualTo("johndoe");
    assertThat(task.getAssignee()).isEqualTo("johndoe");
    assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

    // Finally, delete task
    taskService.deleteTask(taskId, true);
  }

  @Test
  public void testTaskAssignee() {
    Task task = taskService.newTask();
    task.setAssignee("johndoe");
    taskService.saveTask(task);

    // Fetch the task again and update
    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertThat(task.getAssignee()).isEqualTo("johndoe");

    task.setAssignee("joesmoe");
    taskService.saveTask(task);

    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertThat(task.getAssignee()).isEqualTo("joesmoe");

    // Finally, delete task
    taskService.deleteTask(task.getId(), true);
  }


  @Test
  public void testSaveTaskNullTask() {
    try {
      taskService.saveTask(null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("task is null", ae.getMessage());
    }
  }

  @Test
  public void testDeleteTaskNullTaskId() {
    try {
      taskService.deleteTask(null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      // Expected exception
    }
  }

  @Test
  public void testDeleteTaskUnexistingTaskId() {
    // Deleting unexisting task should be silently ignored
    assertThatCode(() -> taskService.deleteTask("unexistingtaskid"))
      .doesNotThrowAnyException();
  }

  @Test
  public void testDeleteTasksNullTaskIds() {
    try {
      taskService.deleteTasks(null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      // Expected exception
    }
  }

  @Test
  public void testDeleteTasksTaskIdsUnexistingTaskId() {

    Task existingTask = taskService.newTask();
    taskService.saveTask(existingTask);

    // The unexisting taskId's should be silently ignored. Existing task should
    // have been deleted.
    taskService.deleteTasks(Arrays.asList("unexistingtaskid1", existingTask.getId()), true);

    existingTask = taskService.createTaskQuery().taskId(existingTask.getId()).singleResult();
    assertNull(existingTask);
  }

  @Test
  public void testClaimNullArguments() {
    try {
      taskService.claim(null, "userid");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testClaimUnexistingTaskId() {
    User user = identityService.newUser("user");
    identityService.saveUser(user);
    var userId = user.getId();

    try {
      taskService.claim("unexistingtaskid", userId);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingtaskid", ae.getMessage());
    }

    identityService.deleteUser(user.getId());
  }

  @Test
  public void testClaimAlreadyClaimedTaskByOtherUser() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    User user = identityService.newUser("user");
    identityService.saveUser(user);
    User secondUser = identityService.newUser("seconduser");
    identityService.saveUser(secondUser);

    // Claim task the first time
    taskService.claim(task.getId(), user.getId());
    var secondUserId = secondUser.getId();
    var taskId = task.getId();

    try {
      taskService.claim(taskId, secondUserId);
      fail("ProcessEngineException expected");
    } catch (TaskAlreadyClaimedException ae) {
      testRule.assertTextPresent("Task '" + task.getId() + "' is already claimed by someone else.", ae.getMessage());
    }

    taskService.deleteTask(task.getId(), true);
    identityService.deleteUser(user.getId());
    identityService.deleteUser(secondUser.getId());
  }

  @Test
  public void testClaimAlreadyClaimedTaskBySameUser() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    User user = identityService.newUser("user");
    identityService.saveUser(user);

    // Claim task the first time
    taskService.claim(task.getId(), user.getId());
    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();

    // Claim the task again with the same user. No exception should be thrown
    taskService.claim(task.getId(), user.getId());

    taskService.deleteTask(task.getId(), true);
    identityService.deleteUser(user.getId());
  }

  @Test
  public void testUnClaimTask() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    User user = identityService.newUser("user");
    identityService.saveUser(user);

    // Claim task the first time
    taskService.claim(task.getId(), user.getId());
    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertThat(task.getAssignee()).isEqualTo(user.getId());

    // Unclaim the task
    taskService.claim(task.getId(), null);

    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertNull(task.getAssignee());

    taskService.deleteTask(task.getId(), true);
    identityService.deleteUser(user.getId());
  }

  @Test
  public void testCompleteTaskNullTaskId() {
    try {
      taskService.complete(null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testCompleteTaskUnexistingTaskId() {
    try {
      taskService.complete("unexistingtask");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingtask", ae.getMessage());
    }
  }

  @Test
  public void testCompleteTaskWithParametersNullTaskId() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("myKey", "myValue");

    try {
      taskService.complete(null, variables);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testCompleteTaskWithParametersUnexistingTaskId() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("myKey", "myValue");

    try {
      taskService.complete("unexistingtask", variables);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingtask", ae.getMessage());
    }
  }

  @Test
  public void testCompleteTaskWithParametersNullParameters() {
    Task task = taskService.newTask();
    taskService.saveTask(task);

    String taskId = task.getId();
    taskService.complete(taskId, null);

    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      historyService.deleteHistoricTaskInstance(taskId);
    }

    // Fetch the task again
    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertNull(task);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCompleteTaskWithParametersEmptyParameters() {
    Task task = taskService.newTask();
    taskService.saveTask(task);

    String taskId = task.getId();
    taskService.complete(taskId, Collections.EMPTY_MAP);

    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      historyService.deleteHistoricTaskInstance(taskId);
    }

    // Fetch the task again
    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertNull(task);
  }


  @Deployment(resources = TWO_TASKS_PROCESS)
  @Test
  public void testCompleteWithParametersTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

    // Fetch first task
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("First task");

    // Complete first task
    Map<String, Object> taskParams = new HashMap<>();
    taskParams.put("myParam", "myValue");
    taskService.complete(task.getId(), taskParams);

    // Fetch second task
    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Second task");

    // Verify task parameters set on execution
    Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
    assertThat(variables.size()).isEqualTo(1);
    assertThat(variables.get("myParam")).isEqualTo("myValue");
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/task/TaskServiceTest.testCompleteTaskWithVariablesInReturn.bpmn20.xml" })
  @Test
  public void testCompleteTaskWithVariablesInReturn() {
    String processVarName = "processVar";
    String processVarValue = "processVarValue";

    String taskVarName = "taskVar";
    String taskVarValue = "taskVarValue";

    Map<String, Object> variables = new HashMap<>();
    variables.put(processVarName, processVarValue);

    runtimeService.startProcessInstanceByKey("TaskServiceTest.testCompleteTaskWithVariablesInReturn", variables);

    Task firstUserTask = taskService.createTaskQuery().taskName("First User Task").singleResult();
    taskService.setVariable(firstUserTask.getId(), "x", 1);
    // local variables should not be returned
    taskService.setVariableLocal(firstUserTask.getId(), "localVar", "localVarValue");

    Map<String, Object> additionalVariables = new HashMap<>();
    additionalVariables.put(taskVarName, taskVarValue);

    // After completion of firstUserTask a script Task sets 'x' = 5
    VariableMap vars = taskService.completeWithVariablesInReturn(firstUserTask.getId(), additionalVariables, true);

    assertThat(vars.size()).isEqualTo(3);
    assertThat(vars.get("x")).isEqualTo(5);
    assertThat(vars.getValueTyped("x").getType()).isEqualTo(ValueType.INTEGER);
    assertThat(vars.get(processVarName)).isEqualTo(processVarValue);
    assertThat(vars.get(taskVarName)).isEqualTo(taskVarValue);
    assertThat(vars.getValueTyped(taskVarName).getType()).isEqualTo(ValueType.STRING);

    additionalVariables = new HashMap<>();
    additionalVariables.put("x", 7);
    Task secondUserTask = taskService.createTaskQuery().taskName("Second User Task").singleResult();

    vars = taskService.completeWithVariablesInReturn(secondUserTask.getId(), additionalVariables, true);
    assertThat(vars.size()).isEqualTo(3);
    assertThat(vars.get("x")).isEqualTo(7);
    assertThat(vars.get(processVarName)).isEqualTo(processVarValue);
    assertThat(vars.get(taskVarName)).isEqualTo(taskVarValue);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  public void testCompleteStandaloneTaskWithVariablesInReturn() {
    String taskVarName = "taskVar";
    String taskVarValue = "taskVarValue";

    String taskId = "myTask";
    Task standaloneTask = taskService.newTask(taskId);
    taskService.saveTask(standaloneTask);

    Map<String, Object> variables = new HashMap<>();
    variables.put(taskVarName, taskVarValue);

    Map<String, Object> returnedVariables = taskService.completeWithVariablesInReturn(taskId, variables, true);
    // expect empty Map for standalone tasks
    assertThat(returnedVariables.size()).isEqualTo(0);

    historyService.deleteHistoricTaskInstance(taskId);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/twoParallelTasksProcess.bpmn20.xml" })
  @Test
  public void testCompleteTaskWithVariablesInReturnParallel() {
    String processVarName = "processVar";
    String processVarValue = "processVarValue";

    String task1VarName = "taskVar1";
    String task2VarName = "taskVar2";
    String task1VarValue = "taskVarValue1";
    String task2VarValue = "taskVarValue2";

    String additionalVar = "additionalVar";
    String additionalVarValue = "additionalVarValue";

    Map<String, Object> variables = new HashMap<>();
    variables.put(processVarName, processVarValue);
    runtimeService.startProcessInstanceByKey("twoParallelTasksProcess", variables);

    Task firstTask = taskService.createTaskQuery().taskName("First Task").singleResult();
    taskService.setVariable(firstTask.getId(), task1VarName, task1VarValue);
    Task secondTask = taskService.createTaskQuery().taskName("Second Task").singleResult();
    taskService.setVariable(secondTask.getId(), task2VarName, task2VarValue);

    Map<String, Object> vars = taskService.completeWithVariablesInReturn(firstTask.getId(), null, true);

    assertThat(vars.size()).isEqualTo(3);
    assertThat(vars.get(processVarName)).isEqualTo(processVarValue);
    assertThat(vars.get(task1VarName)).isEqualTo(task1VarValue);
    assertThat(vars.get(task2VarName)).isEqualTo(task2VarValue);

    Map<String, Object> additionalVariables = new HashMap<>();
    additionalVariables.put(additionalVar, additionalVarValue);

    vars = taskService.completeWithVariablesInReturn(secondTask.getId(), additionalVariables, true);
    assertThat(vars.size()).isEqualTo(4);
    assertThat(vars.get(processVarName)).isEqualTo(processVarValue);
    assertThat(vars.get(task1VarName)).isEqualTo(task1VarValue);
    assertThat(vars.get(task2VarName)).isEqualTo(task2VarValue);
    assertThat(vars.get(additionalVar)).isEqualTo(additionalVarValue);
  }

  /**
   * Tests that the variablesInReturn logic is not applied
   * when we call the regular complete API. This is a performance optimization.
   * Loading all variables may be expensive.
   */
  @Test
  public void testCompleteTaskAndDoNotDeserializeVariables()
  {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .operatonHistoryTimeToLive(180)
        .startEvent()
      .subProcess()
      .embeddedSubProcess()
      .startEvent()
      .userTask("task1")
      .userTask("task2")
      .endEvent()
      .subProcessDone()
      .endEvent()
      .done();

    testRule.deploy(process);

    runtimeService.startProcessInstanceByKey("process", Variables.putValue("var", "val"));

    final Task task = taskService.createTaskQuery().singleResult();

    // when
    final boolean hasLoadedAnyVariables =
      processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> {
        taskService.complete(task.getId());
        return !commandContext.getDbEntityManager().getCachedEntitiesByType(VariableInstanceEntity.class).isEmpty();
      });

    // then
    assertThat(hasLoadedAnyVariables).isFalse();
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml")
  public void testCompleteTaskWithVariablesInReturnShouldDeserializeObjectValue()
  {
    // given
    ObjectValue value = Variables.objectValue("value").create();
    VariableMap variables = Variables.createVariables().putValue("var", value);

    runtimeService.startProcessInstanceByKey("twoTasksProcess", variables);

    Task task = taskService.createTaskQuery().singleResult();

    // when
    VariableMap result = taskService.completeWithVariablesInReturn(task.getId(), null, true);

    // then
    ObjectValue returnedValue = result.getValueTyped("var");
    assertThat(returnedValue.isDeserialized()).isTrue();
    assertThat(returnedValue.getValue()).isEqualTo("value");
  }

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml")
  public void testCompleteTaskWithVariablesInReturnShouldNotDeserializeObjectValue()
  {
    // given
    ObjectValue value = Variables.objectValue("value").create();
    VariableMap variables = Variables.createVariables().putValue("var", value);

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("twoTasksProcess", variables);
    String serializedValue = ((ObjectValue) runtimeService.getVariableTyped(instance.getId(), "var")).getValueSerialized();

    Task task = taskService.createTaskQuery().singleResult();

    // when
    VariableMap result = taskService.completeWithVariablesInReturn(task.getId(), null, false);

    // then
    ObjectValue returnedValue = result.getValueTyped("var");
    assertThat(returnedValue.isDeserialized()).isFalse();
    assertThat(returnedValue.getValueSerialized()).isEqualTo(serializedValue);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn" })
  @Test
  public void testCompleteTaskWithVariablesInReturnCMMN() {
    String taskVariableName = "taskVar";
    String taskVariableValue = "taskVal";

    String caseDefinitionId = repositoryService.createCaseDefinitionQuery().singleResult().getId();
    caseService.withCaseDefinition(caseDefinitionId).create();

    Task task1 = taskService.createTaskQuery().singleResult();
    assertNotNull(task1);

    taskService.setVariable(task1.getId(), taskVariableName, taskVariableValue);
    Map<String, Object> vars = taskService.completeWithVariablesInReturn(task1.getId(), null, true);
    assertNull(vars);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCompleteTaskShouldCompleteCaseExecution() {
    // given
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();
    assertNotNull(caseExecutionId);

    Task task = taskService.createTaskQuery().singleResult();
    assertNotNull(task);

    // when
    taskService.complete(task.getId());

    // then

    task = taskService.createTaskQuery().singleResult();

    assertNull(task);

    CaseExecution caseExecution = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .singleResult();

    assertNull(caseExecution);

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isCompleted());
  }

  @Test
  public void testResolveTaskNullTaskId() {
    try {
      taskService.resolveTask(null);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testResolveTaskUnexistingTaskId() {
    try {
      taskService.resolveTask("unexistingtask");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingtask", ae.getMessage());
    }
  }

  @Test
  public void testResolveTaskWithParametersNullParameters() {
    Task task = taskService.newTask();
    task.setDelegationState(DelegationState.PENDING);
    taskService.saveTask(task);

    String taskId = task.getId();
    taskService.resolveTask(taskId, null);

    if (processEngineConfiguration.getHistoryLevel().getId()>= ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      historyService.deleteHistoricTaskInstance(taskId);
    }

    // Fetch the task again
    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

    taskService.deleteTask(taskId, true);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testResolveTaskWithParametersEmptyParameters() {
    Task task = taskService.newTask();
    task.setDelegationState(DelegationState.PENDING);
    taskService.saveTask(task);

    String taskId = task.getId();
    taskService.resolveTask(taskId, Collections.EMPTY_MAP);

    if (processEngineConfiguration.getHistoryLevel().getId()>= ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      historyService.deleteHistoricTaskInstance(taskId);
    }

    // Fetch the task again
    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

    taskService.deleteTask(taskId, true);
  }

  @Deployment(resources = TWO_TASKS_PROCESS)
  @Test
  public void testResolveWithParametersTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

    // Fetch first task
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("First task");

    task.delegate("johndoe");

    // Resolve first task
    Map<String, Object> taskParams = new HashMap<>();
    taskParams.put("myParam", "myValue");
    taskService.resolveTask(task.getId(), taskParams);

    // Verify that task is resolved
    task = taskService.createTaskQuery().taskDelegationState(DelegationState.RESOLVED).singleResult();
    assertThat(task.getName()).isEqualTo("First task");

    // Verify task parameters set on execution
    Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
    assertThat(variables.size()).isEqualTo(1);
    assertThat(variables.get("myParam")).isEqualTo("myValue");
  }

  @Test
  public void testSetAssignee() {
    User user = identityService.newUser("user");
    identityService.saveUser(user);

    Task task = taskService.newTask();
    assertNull(task.getAssignee());
    taskService.saveTask(task);

    // Set assignee
    taskService.setAssignee(task.getId(), user.getId());

    // Fetch task again
    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertThat(task.getAssignee()).isEqualTo(user.getId());

    identityService.deleteUser(user.getId());
    taskService.deleteTask(task.getId(), true);
  }

  @Test
  public void testSetAssigneeNullTaskId() {
    try {
      taskService.setAssignee(null, "userId");
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testSetAssigneeUnexistingTask() {
    User user = identityService.newUser("user");
    identityService.saveUser(user);
    var userId = user.getId();

    try {
      taskService.setAssignee("unexistingTaskId", userId);
      fail("ProcessEngineException expected");
    } catch (NotFoundException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingTaskId", ae.getMessage());
    }

    identityService.deleteUser(user.getId());
  }

  @Test
  public void testSetOwnerNullTaskId() {
    try {
      taskService.setOwner(null, "userId");
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testSetOwnerUnexistingTask() {
    User user = identityService.newUser("user");
    identityService.saveUser(user);
    var userId = user.getId();

    try {
      taskService.setOwner("unexistingTaskId", userId);
      fail("ProcessEngineException expected");
    } catch (NotFoundException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingTaskId", ae.getMessage());
    }

    identityService.deleteUser(user.getId());
  }

  @Test
  public void testSetOwnerNullUser() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    var taskId = task.getId();

    try {
      taskService.setOwner(taskId, null);
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("userId and groupId cannot both be null", ae.getMessage());
    }

    taskService.deleteTask(task.getId(), true);
  }

  @Test
  public void testAddCandidateUserDuplicate() {
    // Check behavior when adding the same user twice as candidate
    User user = identityService.newUser("user");
    identityService.saveUser(user);

    Task task = taskService.newTask();
    taskService.saveTask(task);

    taskService.addCandidateUser(task.getId(), user.getId());

    // Add as candidate the second time
    taskService.addCandidateUser(task.getId(), user.getId());

    identityService.deleteUser(user.getId());
    taskService.deleteTask(task.getId(), true);
  }

  @Test
  public void testAddCandidateUserNullTaskId() {
    try {
      taskService.addCandidateUser(null, "userId");
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testAddCandidateUserNullUserId() {
    try {
      taskService.addCandidateUser("taskId", null);
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("userId and groupId cannot both be null", ae.getMessage());
    }
  }

  @Test
  public void testAddCandidateUserUnexistingTask() {
    User user = identityService.newUser("user");
    identityService.saveUser(user);
    var userId = user.getId();

    try {
      taskService.addCandidateUser("unexistingTaskId", userId);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingTaskId", ae.getMessage());
    }

    identityService.deleteUser(user.getId());
  }

  @Test
  public void testAddCandidateGroupNullTaskId() {
    try {
      taskService.addCandidateGroup(null, "groupId");
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testAddCandidateGroupNullGroupId() {
    try {
      taskService.addCandidateGroup("taskId", null);
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("userId and groupId cannot both be null", ae.getMessage());
    }
  }

  @Test
  public void testAddCandidateGroupUnexistingTask() {
    Group group = identityService.newGroup("group");
    identityService.saveGroup(group);
    var groupId = group.getId();
    try {
      taskService.addCandidateGroup("unexistingTaskId", groupId);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingTaskId", ae.getMessage());
    }
    identityService.deleteGroup(group.getId());
  }

  @Test
  public void testAddGroupIdentityLinkNullTaskId() {
    try {
      taskService.addGroupIdentityLink(null, "groupId", IdentityLinkType.CANDIDATE);
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testAddGroupIdentityLinkNullUserId() {
    try {
      taskService.addGroupIdentityLink("taskId", null, IdentityLinkType.CANDIDATE);
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("userId and groupId cannot both be null", ae.getMessage());
    }
  }

  @Test
  public void testAddGroupIdentityLinkUnexistingTask() {
    User user = identityService.newUser("user");
    identityService.saveUser(user);
    var userId = user.getId();

    try {
      taskService.addGroupIdentityLink("unexistingTaskId", userId, IdentityLinkType.CANDIDATE);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingTaskId", ae.getMessage());
    }

    identityService.deleteUser(user.getId());
  }

  @Test
  public void testAddUserIdentityLinkNullTaskId() {
    try {
      taskService.addUserIdentityLink(null, "userId", IdentityLinkType.CANDIDATE);
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testAddUserIdentityLinkNullUserId() {
    try {
      taskService.addUserIdentityLink("taskId", null, IdentityLinkType.CANDIDATE);
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("userId and groupId cannot both be null", ae.getMessage());
    }
  }

  @Test
  public void testAddUserIdentityLinkUnexistingTask() {
    User user = identityService.newUser("user");
    identityService.saveUser(user);
    var userId = user.getId();

    try {
      taskService.addUserIdentityLink("unexistingTaskId", userId, IdentityLinkType.CANDIDATE);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingTaskId", ae.getMessage());
    }

    identityService.deleteUser(user.getId());
  }

  @Test
  public void testGetIdentityLinksWithCandidateUser() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    identityService.saveUser(identityService.newUser("kermit"));

    taskService.addCandidateUser(taskId, "kermit");
    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
    assertThat(identityLinks.size()).isEqualTo(1);
    assertThat(identityLinks.get(0).getUserId()).isEqualTo("kermit");
    assertNull(identityLinks.get(0).getGroupId());
    assertThat(identityLinks.get(0).getType()).isEqualTo(IdentityLinkType.CANDIDATE);

    //cleanup
    taskService.deleteTask(taskId, true);
    identityService.deleteUser("kermit");
  }

  @Test
  public void testGetIdentityLinksWithCandidateGroup() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    identityService.saveGroup(identityService.newGroup("muppets"));

    taskService.addCandidateGroup(taskId, "muppets");
    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
    assertThat(identityLinks.size()).isEqualTo(1);
    assertThat(identityLinks.get(0).getGroupId()).isEqualTo("muppets");
    assertNull(identityLinks.get(0).getUserId());
    assertThat(identityLinks.get(0).getType()).isEqualTo(IdentityLinkType.CANDIDATE);

    //cleanup
    taskService.deleteTask(taskId, true);
    identityService.deleteGroup("muppets");
  }

  @Test
  public void testGetIdentityLinksWithAssignee() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    identityService.saveUser(identityService.newUser("kermit"));

    taskService.claim(taskId, "kermit");
    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
    assertThat(identityLinks.size()).isEqualTo(1);
    assertThat(identityLinks.get(0).getUserId()).isEqualTo("kermit");
    assertNull(identityLinks.get(0).getGroupId());
    assertThat(identityLinks.get(0).getType()).isEqualTo(IdentityLinkType.ASSIGNEE);

    //cleanup
    taskService.deleteTask(taskId, true);
    identityService.deleteUser("kermit");
  }

  @Test
  public void testGetIdentityLinksWithNonExistingAssignee() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    taskService.claim(taskId, "nonExistingAssignee");
    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
    assertThat(identityLinks.size()).isEqualTo(1);
    assertThat(identityLinks.get(0).getUserId()).isEqualTo("nonExistingAssignee");
    assertNull(identityLinks.get(0).getGroupId());
    assertThat(identityLinks.get(0).getType()).isEqualTo(IdentityLinkType.ASSIGNEE);

    //cleanup
    taskService.deleteTask(taskId, true);
  }

  @Test
  public void testGetIdentityLinksWithOwner() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    identityService.saveUser(identityService.newUser("kermit"));
    identityService.saveUser(identityService.newUser("fozzie"));

    taskService.claim(taskId, "kermit");
    taskService.delegateTask(taskId, "fozzie");

    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
    assertThat(identityLinks.size()).isEqualTo(2);

    IdentityLink assignee = identityLinks.get(0);
    assertThat(assignee.getUserId()).isEqualTo("fozzie");
    assertNull(assignee.getGroupId());
    assertThat(assignee.getType()).isEqualTo(IdentityLinkType.ASSIGNEE);

    IdentityLink owner = identityLinks.get(1);
    assertThat(owner.getUserId()).isEqualTo("kermit");
    assertNull(owner.getGroupId());
    assertThat(owner.getType()).isEqualTo(IdentityLinkType.OWNER);

    //cleanup
    taskService.deleteTask(taskId, true);
    identityService.deleteUser("kermit");
    identityService.deleteUser("fozzie");
  }

  @Test
  public void testGetIdentityLinksWithNonExistingOwner() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    String taskId = task.getId();

    taskService.claim(taskId, "nonExistingOwner");
    taskService.delegateTask(taskId, "nonExistingAssignee");
    List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
    assertThat(identityLinks.size()).isEqualTo(2);

    IdentityLink assignee = identityLinks.get(0);
    assertThat(assignee.getUserId()).isEqualTo("nonExistingAssignee");
    assertNull(assignee.getGroupId());
    assertThat(assignee.getType()).isEqualTo(IdentityLinkType.ASSIGNEE);

    IdentityLink owner = identityLinks.get(1);
    assertThat(owner.getUserId()).isEqualTo("nonExistingOwner");
    assertNull(owner.getGroupId());
    assertThat(owner.getType()).isEqualTo(IdentityLinkType.OWNER);

    //cleanup
    taskService.deleteTask(taskId, true);
  }

  @Test
  public void testSetPriority() {
    Task task = taskService.newTask();
    taskService.saveTask(task);

    taskService.setPriority(task.getId(), 12345);

    // Fetch task again to check if the priority is set
    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertThat(task.getPriority()).isEqualTo(12345);

    taskService.deleteTask(task.getId(), true);
  }

  @Test
  public void testSetPriorityUnexistingTaskId() {
    try {
      taskService.setPriority("unexistingtask", 12345);
      fail("ProcessEngineException expected");
    } catch (NotFoundException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingtask", ae.getMessage());
    }
  }

  @Test
  public void testSetPriorityNullTaskId() {
    try {
      taskService.setPriority(null, 12345);
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testSetNameUnexistingTaskId() {
    try {
      taskService.setName("unexistingtask", "foo");
      fail("ProcessEngineException expected");
    } catch (NotFoundException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingtask", ae.getMessage());
    }
  }

  @Test
  public void testSetNameNullTaskId() {
    try {
      taskService.setName(null, "foo");
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testSetNameNullTaskName() {
    Task task = taskService.newTask();
    taskService.saveTask(task);
    var taskId = task.getId();

    try {
      taskService.setName(taskId, null);
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("value is null", ae.getMessage());
    }

    taskService.deleteTask(task.getId(), true);
  }

  @Test
  public void testSetDescriptionUnexistingTaskId() {
    try {
      taskService.setDescription("unexistingtask", "foo");
      fail("ProcessEngineException expected");
    } catch (NotFoundException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingtask", ae.getMessage());
    }
  }

  @Test
  public void testSetDescriptionNullTaskId() {
    try {
      taskService.setDescription(null, "foo");
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testSetDescriptionNullDescription() {
    // given
    Task task = taskService.newTask();
    taskService.saveTask(task);

    //when
    taskService.setDescription(task.getId(), null);

    // then
    task = taskService.createTaskQuery().singleResult();

    assertThat(task.getDescription()).isNull();

    taskService.deleteTask(task.getId(), true);
  }

  @Test
  public void testSetDueDateUnexistingTaskId() {
    Date dueDate = new Date();
    try {
      taskService.setDueDate("unexistingtask", dueDate);
      fail("ProcessEngineException expected");
    } catch (NotFoundException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingtask", ae.getMessage());
    }
  }

  @Test
  public void testSetDueDateNullTaskId() {
    Date dueDate = new Date();
    try {
      taskService.setDueDate(null, dueDate);
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testSetDueDateNullDueDate() {
    // given
    Task task = taskService.newTask();
    taskService.saveTask(task);

    // when
    taskService.setDueDate(task.getId(), null);

    // then
    task = taskService.createTaskQuery().singleResult();

    assertThat(task.getDescription()).isNull();

    taskService.deleteTask(task.getId(), true);
  }

  @Test
  public void testSetFollowUpDateUnexistingTaskId() {
    Date followUpDate = new Date();
    try {
      taskService.setFollowUpDate("unexistingtask", followUpDate);
      fail("ProcessEngineException expected");
    } catch (NotFoundException ae) {
      testRule.assertTextPresent("Cannot find task with id unexistingtask", ae.getMessage());
    }
  }

  @Test
  public void testSetFollowUpDateNullTaskId() {
    Date followUpDate = new Date();
    try {
      taskService.setFollowUpDate(null, followUpDate);
      fail("ProcessEngineException expected");
    } catch (NullValueException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Test
  public void testSetFollowUpDateNullFollowUpDate() {
    // given
    Task task = taskService.newTask();
    taskService.saveTask(task);

    // when
    taskService.setFollowUpDate(task.getId(), null);

    // then
    task = taskService.createTaskQuery().singleResult();

    assertThat(task.getDescription()).isNull();

    taskService.deleteTask(task.getId(), true);
  }

  /**
   * @see <a href="http://jira.codehaus.org/browse/ACT-1059">ACT-1059</a>
   */
  @Test
  public void testSetDelegationState() {
    Task task = taskService.newTask();
    task.setOwner("wuzh");
    task.delegate("other");
    taskService.saveTask(task);
    String taskId = task.getId();

    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertThat(task.getOwner()).isEqualTo("wuzh");
    assertThat(task.getAssignee()).isEqualTo("other");
    assertThat(task.getDelegationState()).isEqualTo(DelegationState.PENDING);

    task.setDelegationState(DelegationState.RESOLVED);
    taskService.saveTask(task);

    task = taskService.createTaskQuery().taskId(taskId).singleResult();
    assertThat(task.getOwner()).isEqualTo("wuzh");
    assertThat(task.getAssignee()).isEqualTo("other");
    assertThat(task.getDelegationState()).isEqualTo(DelegationState.RESOLVED);

    taskService.deleteTask(taskId, true);
  }

  private void checkHistoricVariableUpdateEntity(String variableName, String processInstanceId) {
    if (processEngineConfiguration.getHistoryLevel().getId() == ProcessEngineConfigurationImpl.HISTORYLEVEL_FULL) {
      boolean deletedVariableUpdateFound = false;

      List<HistoricDetail> resultSet = historyService.createHistoricDetailQuery().processInstanceId(processInstanceId).list();
      for (HistoricDetail currentHistoricDetail : resultSet) {
        assertTrue(currentHistoricDetail instanceof HistoricDetailVariableInstanceUpdateEntity);
        HistoricDetailVariableInstanceUpdateEntity historicVariableUpdate = (HistoricDetailVariableInstanceUpdateEntity) currentHistoricDetail;

        if (historicVariableUpdate.getName().equals(variableName) && historicVariableUpdate.getValue() == null) {
          if (deletedVariableUpdateFound) {
            fail("Mismatch: A HistoricVariableUpdateEntity with a null value already found");
          } else {
            deletedVariableUpdateFound = true;
          }
        }
      }

      assertTrue(deletedVariableUpdateFound);
    }
  }

  @Deployment(resources = {
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testRemoveVariable() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task currentTask = taskService.createTaskQuery().singleResult();

    taskService.setVariable(currentTask.getId(), "variable1", "value1");
    assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isEqualTo("value1");
    assertNull(taskService.getVariableLocal(currentTask.getId(), "variable1"));

    taskService.removeVariable(currentTask.getId(), "variable1");

    assertNull(taskService.getVariable(currentTask.getId(), "variable1"));

    checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
  }

  @Test
  public void testRemoveVariableNullTaskId() {
    try {
      taskService.removeVariable(null, "variable");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Deployment(resources = {
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testRemoveVariables() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task currentTask = taskService.createTaskQuery().singleResult();

    Map<String, Object> varsToDelete = new HashMap<>();
    varsToDelete.put("variable1", "value1");
    varsToDelete.put("variable2", "value2");
    taskService.setVariables(currentTask.getId(), varsToDelete);
    taskService.setVariable(currentTask.getId(), "variable3", "value3");

    assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isEqualTo("value1");
    assertThat(taskService.getVariable(currentTask.getId(), "variable2")).isEqualTo("value2");
    assertThat(taskService.getVariable(currentTask.getId(), "variable3")).isEqualTo("value3");
    assertNull(taskService.getVariableLocal(currentTask.getId(), "variable1"));
    assertNull(taskService.getVariableLocal(currentTask.getId(), "variable2"));
    assertNull(taskService.getVariableLocal(currentTask.getId(), "variable3"));

    taskService.removeVariables(currentTask.getId(), varsToDelete.keySet());

    assertNull(taskService.getVariable(currentTask.getId(), "variable1"));
    assertNull(taskService.getVariable(currentTask.getId(), "variable2"));
    assertThat(taskService.getVariable(currentTask.getId(), "variable3")).isEqualTo("value3");

    assertNull(taskService.getVariableLocal(currentTask.getId(), "variable1"));
    assertNull(taskService.getVariableLocal(currentTask.getId(), "variable2"));
    assertNull(taskService.getVariableLocal(currentTask.getId(), "variable3"));

    checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    checkHistoricVariableUpdateEntity("variable2", processInstance.getId());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRemoveVariablesNullTaskId() {
    try {
      taskService.removeVariables(null, Collections.EMPTY_LIST);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Deployment(resources = {
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testRemoveVariableLocal() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task currentTask = taskService.createTaskQuery().singleResult();

    taskService.setVariableLocal(currentTask.getId(), "variable1", "value1");
    assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isEqualTo("value1");
    assertThat(taskService.getVariableLocal(currentTask.getId(), "variable1")).isEqualTo("value1");

    taskService.removeVariableLocal(currentTask.getId(), "variable1");

    assertNull(taskService.getVariable(currentTask.getId(), "variable1"));
    assertNull(taskService.getVariableLocal(currentTask.getId(), "variable1"));

    checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
  }

  @Test
  public void testRemoveVariableLocalNullTaskId() {
    try {
      taskService.removeVariableLocal(null, "variable");
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Deployment(resources = {
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testRemoveVariablesLocal() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task currentTask = taskService.createTaskQuery().singleResult();

    Map<String, Object> varsToDelete = new HashMap<>();
    varsToDelete.put("variable1", "value1");
    varsToDelete.put("variable2", "value2");
    taskService.setVariablesLocal(currentTask.getId(), varsToDelete);
    taskService.setVariableLocal(currentTask.getId(), "variable3", "value3");

    assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isEqualTo("value1");
    assertThat(taskService.getVariable(currentTask.getId(), "variable2")).isEqualTo("value2");
    assertThat(taskService.getVariable(currentTask.getId(), "variable3")).isEqualTo("value3");
    assertThat(taskService.getVariableLocal(currentTask.getId(), "variable1")).isEqualTo("value1");
    assertThat(taskService.getVariableLocal(currentTask.getId(), "variable2")).isEqualTo("value2");
    assertThat(taskService.getVariableLocal(currentTask.getId(), "variable3")).isEqualTo("value3");

    taskService.removeVariables(currentTask.getId(), varsToDelete.keySet());

    assertNull(taskService.getVariable(currentTask.getId(), "variable1"));
    assertNull(taskService.getVariable(currentTask.getId(), "variable2"));
    assertThat(taskService.getVariable(currentTask.getId(), "variable3")).isEqualTo("value3");

    assertNull(taskService.getVariableLocal(currentTask.getId(), "variable1"));
    assertNull(taskService.getVariableLocal(currentTask.getId(), "variable2"));
    assertThat(taskService.getVariableLocal(currentTask.getId(), "variable3")).isEqualTo("value3");

    checkHistoricVariableUpdateEntity("variable1", processInstance.getId());
    checkHistoricVariableUpdateEntity("variable2", processInstance.getId());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRemoveVariablesLocalNullTaskId() {
    try {
      taskService.removeVariablesLocal(null, Collections.EMPTY_LIST);
      fail("ProcessEngineException expected");
    } catch (ProcessEngineException ae) {
      testRule.assertTextPresent("taskId is null", ae.getMessage());
    }
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testUserTaskOptimisticLocking() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task task1 = taskService.createTaskQuery().singleResult();
    Task task2 = taskService.createTaskQuery().singleResult();

    task1.setDescription("test description one");
    taskService.saveTask(task1);
    task2.setDescription("test description two");

    try {
      taskService.saveTask(task2);

      fail("Expecting exception");
    } catch(OptimisticLockingException e) {
      // Expected exception
    }
  }

  @Test
  public void testDeleteTaskWithDeleteReason() {
    // ACT-900: deleteReason can be manually specified - can only be validated when historyLevel > ACTIVITY
    if (processEngineConfiguration.getHistoryLevel().getId() >= ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {

      Task task = taskService.newTask();
      task.setName("test task");
      taskService.saveTask(task);

      assertNotNull(task.getId());

      taskService.deleteTask(task.getId(), "deleted for testing purposes");

      HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery()
        .taskId(task.getId()).singleResult();

      assertNotNull(historicTaskInstance);
      assertThat(historicTaskInstance.getDeleteReason()).isEqualTo("deleted for testing purposes");

      // Delete historic task that is left behind, will not be cleaned up because this is not part of a process
      taskService.deleteTask(task.getId(), true);

    }
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml" })
  @Test
  public void testDeleteTaskPartOfProcess() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().singleResult();
    assertNotNull(task);
    String taskId = task.getId();
    List<String> taskIds = List.of(taskId);

    try {
      taskService.deleteTask(taskId);
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).isEqualTo("The task cannot be deleted because is part of a running process");
    }

    try {
      taskService.deleteTask(taskId, true);
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).isEqualTo("The task cannot be deleted because is part of a running process");
    }

    try {
      taskService.deleteTask(taskId, "test");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).isEqualTo("The task cannot be deleted because is part of a running process");
    }

    try {
      taskService.deleteTasks(Collections.singletonList(taskId));
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).isEqualTo("The task cannot be deleted because is part of a running process");
    }

    try {
      taskService.deleteTasks(taskIds, true);
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).isEqualTo("The task cannot be deleted because is part of a running process");
    }

    try {
      taskService.deleteTasks(taskIds, "test");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).isEqualTo("The task cannot be deleted because is part of a running process");
    }

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testDeleteTaskPartOfCaseInstance() {
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    var caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();
    assertThat(caseExecution).isNotNull();

    Task task = taskService.createTaskQuery().singleResult();
    assertNotNull(task);
    String taskId = task.getId();
    var taskIds = Collections.singletonList(task.getId());

    try {
      taskService.deleteTask(taskId);
      fail("Should not be possible to delete task");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).isEqualTo("The task cannot be deleted because is part of a running case instance");
    }

    try {
      taskService.deleteTask(taskId, true);
      fail("Should not be possible to delete task");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).isEqualTo("The task cannot be deleted because is part of a running case instance");
    }

    try {
      taskService.deleteTask(taskId, "test");
      fail("Should not be possible to delete task");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).isEqualTo("The task cannot be deleted because is part of a running case instance");
    }

    try {
      taskService.deleteTasks(taskIds);
      fail("Should not be possible to delete task");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).isEqualTo("The task cannot be deleted because is part of a running case instance");
    }

    try {
      taskService.deleteTasks(taskIds, true);
      fail("Should not be possible to delete task");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).isEqualTo("The task cannot be deleted because is part of a running case instance");
    }

    try {
      taskService.deleteTasks(taskIds, "test");
      fail("Should not be possible to delete task");
    } catch(ProcessEngineException ae) {
      assertThat(ae.getMessage()).isEqualTo("The task cannot be deleted because is part of a running case instance");
    }

  }

  @Test
  public void testGetTaskCommentByTaskIdAndCommentId() {
    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      // create and save new task
      Task task = taskService.newTask();
      taskService.saveTask(task);

      String taskId = task.getId();

      // add comment to task
      Comment comment = taskService.createComment(taskId, null, "look at this \n       isn't this great? slkdjf sldkfjs ldkfjs ldkfjs ldkfj sldkfj sldkfj sldkjg laksfg sdfgsd;flgkj ksajdhf skjdfh ksjdhf skjdhf kalskjgh lskh dfialurhg kajsh dfuieqpgkja rzvkfnjviuqerhogiuvysbegkjz lkhf ais liasduh flaisduh ajiasudh vaisudhv nsfd");

      // select task comment for task id and comment id
      comment = taskService.getTaskComment(taskId, comment.getId());
      // check returned comment
      assertNotNull(comment.getId());
      assertThat(comment.getTaskId()).isEqualTo(taskId);
      assertNull(comment.getProcessInstanceId());
      assertThat(((Event) comment).getMessage()).isEqualTo("look at this isn't this great? slkdjf sldkfjs ldkfjs ldkfjs ldkfj sldkfj sldkfj sldkjg laksfg sdfgsd;flgkj ksajdhf skjdfh ksjdhf skjdhf kalskjgh lskh dfialurhg ...");
      assertThat(comment.getFullMessage()).isEqualTo("look at this \n       isn't this great? slkdjf sldkfjs ldkfjs ldkfjs ldkfj sldkfj sldkfj sldkjg laksfg sdfgsd;flgkj ksajdhf skjdfh ksjdhf skjdhf kalskjgh lskh dfialurhg kajsh dfuieqpgkja rzvkfnjviuqerhogiuvysbegkjz lkhf ais liasduh flaisduh ajiasudh vaisudhv nsfd");
      assertNotNull(comment.getTime());

      // delete task
      taskService.deleteTask(task.getId(), true);
    }
  }

  @Test
  public void testTaskAttachmentByTaskIdAndAttachmentId() throws ParseException {
    Date fixedDate = SDF.parse("01/01/2001 01:01:01.000");
    ClockUtil.setCurrentTime(fixedDate);

    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      // create and save task
      Task task = taskService.newTask();
      taskService.saveTask(task);
      String taskId = task.getId();

      // Fetch the task again and update
      // add attachment
      Attachment attachment = taskService.createAttachment("web page", taskId, "someprocessinstanceid", "weatherforcast", "temperatures and more", "http://weather.com");
      String attachmentId = attachment.getId();

      // get attachment for taskId and attachmentId
      attachment = taskService.getTaskAttachment(taskId, attachmentId);
      assertThat(attachment.getName()).isEqualTo("weatherforcast");
      assertThat(attachment.getDescription()).isEqualTo("temperatures and more");
      assertThat(attachment.getType()).isEqualTo("web page");
      assertThat(attachment.getTaskId()).isEqualTo(taskId);
      assertThat(attachment.getProcessInstanceId()).isEqualTo("someprocessinstanceid");
      assertThat(attachment.getUrl()).isEqualTo("http://weather.com");
      assertNull(taskService.getAttachmentContent(attachment.getId()));
      assertThat(attachment.getCreateTime()).isEqualTo(fixedDate);

      // delete attachment for taskId and attachmentId
      taskService.deleteTaskAttachment(taskId, attachmentId);

      // check if attachment deleted
      assertNull(taskService.getTaskAttachment(taskId, attachmentId));

      taskService.deleteTask(taskId, true);
    }
  }

  @Test
  public void testGetTaskAttachmentContentByTaskIdAndAttachmentId() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      // create and save task
      Task task = taskService.newTask();
      taskService.saveTask(task);
      String taskId = task.getId();

      // Fetch the task again and update
      // add attachment
      Attachment attachment = taskService.createAttachment("web page", taskId, "someprocessinstanceid", "weatherforcast", "temperatures and more", new ByteArrayInputStream("someContent".getBytes()));
      String attachmentId = attachment.getId();

      // get attachment for taskId and attachmentId
      InputStream taskAttachmentContent = taskService.getTaskAttachmentContent(taskId, attachmentId);
      assertNotNull(taskAttachmentContent);

      byte[] byteContent = IoUtil.readInputStream(taskAttachmentContent, "weatherforcast");
      assertThat(new String(byteContent)).isEqualTo("someContent");

      taskService.deleteTask(taskId, true);
    }
  }

  @Test
  public void testGetTaskAttachmentWithNullParameters() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      Attachment attachment = taskService.getTaskAttachment(null, null);
      assertNull(attachment);
    }
  }

  @Test
  public void testGetTaskAttachmentContentWithNullParameters() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      InputStream content = taskService.getTaskAttachmentContent(null, null);
      assertNull(content);
    }
  }

  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  @Test
  public void testCreateTaskAttachmentWithNullTaskAndProcessInstance() {
    var content = new ByteArrayInputStream("someContent".getBytes());
    try {
      taskService.createAttachment("web page", null, null, "weatherforcast", "temperatures and more", content);
      fail("expected process engine exception");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  @Test
  public void testCreateTaskAttachmentWithNullTaskId() throws ParseException {
    Date fixedDate = SDF.parse("01/01/2001 01:01:01.000");
    ClockUtil.setCurrentTime(fixedDate);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Attachment attachment = taskService.createAttachment("web page", null, processInstance.getId(), "weatherforcast", "temperatures and more", new ByteArrayInputStream("someContent".getBytes()));
    Attachment fetched = taskService.getAttachment(attachment.getId());
    assertThat(fetched).isNotNull();
    assertThat(fetched.getTaskId()).isNull();
    assertThat(fetched.getProcessInstanceId()).isNotNull();
    assertThat(fetched.getCreateTime()).isEqualTo(fixedDate);
    taskService.deleteAttachment(attachment.getId());
  }

  @Test
  public void testDeleteTaskAttachmentWithNullParameter() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      try {
        taskService.deleteAttachment(null);
        fail("expected process engine exception");
      } catch (ProcessEngineException e) {
        // expected
      }
    }
  }

  @Test
  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  public void testDeleteAttachment() {
    // given
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    Attachment attachment = taskService.createAttachment("web page", taskId, null, "weatherforcast", "temperatures and more",
        new ByteArrayInputStream("someContent".getBytes()));
    Attachment fetched = taskService.getAttachment(attachment.getId());
    assertThat(fetched).isNotNull();
    // when
    taskService.deleteAttachment(attachment.getId());
    // then
    fetched = taskService.getAttachment(attachment.getId());
    assertThat(fetched).isNull();
  }

  @Test
  public void testDeleteTaskAttachmentWithNullParameters() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      try {
        taskService.deleteTaskAttachment(null, null);
        fail("expected process engine exception");
      } catch (ProcessEngineException e) {
        // expected
      }
    }
  }

  @Test
  public void testDeleteTaskAttachmentWithTaskIdNull() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      try {
        taskService.deleteTaskAttachment(null, "myAttachmentId");
        fail("expected process engine exception");
      } catch(ProcessEngineException e) {
        // expected
      }
    }
  }

  @Test
  public void testGetTaskAttachmentsWithTaskIdNull() {
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      assertThat(taskService.getTaskAttachments(null)).isEqualTo(Collections.<Attachment>emptyList());
    }
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneSubProcess.bpmn20.xml"})
  @Test
  public void testUpdateVariablesLocal() {
    Map<String, Object> globalVars = new HashMap<>();
    globalVars.put("variable4", "value4");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", globalVars);

    Task currentTask = taskService.createTaskQuery().singleResult();
    Map<String, Object> localVars = new HashMap<>();
    localVars.put("variable1", "value1");
    localVars.put("variable2", "value2");
    localVars.put("variable3", "value3");
    taskService.setVariablesLocal(currentTask.getId(), localVars);

    Map<String, Object> modifications = new HashMap<>();
    modifications.put("variable1", "anotherValue1");
    modifications.put("variable2", "anotherValue2");

    List<String> deletions = new ArrayList<>();
    deletions.add("variable2");
    deletions.add("variable3");
    deletions.add("variable4");

    ((TaskServiceImpl) taskService).updateVariablesLocal(currentTask.getId(), modifications, deletions);

    assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isEqualTo("anotherValue1");
    assertNull(taskService.getVariable(currentTask.getId(), "variable2"));
    assertNull(taskService.getVariable(currentTask.getId(), "variable3"));
    assertThat(runtimeService.getVariable(processInstance.getId(), "variable4")).isEqualTo("value4");
  }

  @Test
  public void testUpdateVariablesLocalForNonExistingTaskId() {
    Map<String, Object> modifications = new HashMap<>();
    modifications.put("variable1", "anotherValue1");
    modifications.put("variable2", "anotherValue2");

    List<String> deletions = new ArrayList<>();
    deletions.add("variable2");
    deletions.add("variable3");
    deletions.add("variable4");

    try {
      ((TaskServiceImpl) taskService).updateVariablesLocal("nonExistingId", modifications, deletions);
      fail("expected process engine exception");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testUpdateVariablesLocaForNullTaskId() {
    Map<String, Object> modifications = new HashMap<>();
    modifications.put("variable1", "anotherValue1");
    modifications.put("variable2", "anotherValue2");

    List<String> deletions = new ArrayList<>();
    deletions.add("variable2");
    deletions.add("variable3");
    deletions.add("variable4");

    try {
      ((TaskServiceImpl) taskService).updateVariablesLocal(null, modifications, deletions);
      fail("expected process engine exception");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneSubProcess.bpmn20.xml"})
  @Test
  public void testUpdateVariables() {
    Map<String, Object> globalVars = new HashMap<>();
    globalVars.put("variable4", "value4");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess", globalVars);

    Task currentTask = taskService.createTaskQuery().singleResult();
    Map<String, Object> localVars = new HashMap<>();
    localVars.put("variable1", "value1");
    localVars.put("variable2", "value2");
    localVars.put("variable3", "value3");
    taskService.setVariablesLocal(currentTask.getId(), localVars);

    Map<String, Object> modifications = new HashMap<>();
    modifications.put("variable1", "anotherValue1");
    modifications.put("variable2", "anotherValue2");

    List<String> deletions = new ArrayList<>();
    deletions.add("variable2");
    deletions.add("variable3");
    deletions.add("variable4");

    ((TaskServiceImpl) taskService).updateVariables(currentTask.getId(), modifications, deletions);

    assertThat(taskService.getVariable(currentTask.getId(), "variable1")).isEqualTo("anotherValue1");
    assertNull(taskService.getVariable(currentTask.getId(), "variable2"));
    assertNull(taskService.getVariable(currentTask.getId(), "variable3"));
    assertNull(runtimeService.getVariable(processInstance.getId(), "variable4"));
  }

  @Test
  public void testUpdateVariablesForNonExistingTaskId() {
    Map<String, Object> modifications = new HashMap<>();
    modifications.put("variable1", "anotherValue1");
    modifications.put("variable2", "anotherValue2");

    List<String> deletions = new ArrayList<>();
    deletions.add("variable2");
    deletions.add("variable3");
    deletions.add("variable4");

    try {
      ((TaskServiceImpl) taskService).updateVariables("nonExistingId", modifications, deletions);
      fail("expected process engine exception");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testUpdateVariablesForNullTaskId() {
    Map<String, Object> modifications = new HashMap<>();
    modifications.put("variable1", "anotherValue1");
    modifications.put("variable2", "anotherValue2");

    List<String> deletions = new ArrayList<>();
    deletions.add("variable2");
    deletions.add("variable3");
    deletions.add("variable4");

    try {
      ((TaskServiceImpl) taskService).updateVariables(null, modifications, deletions);
      fail("expected process engine exception");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testTaskCaseInstanceId() {
    Task task = taskService.newTask();
    task.setCaseInstanceId("aCaseInstanceId");
    taskService.saveTask(task);

    // Fetch the task again and update
    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertThat(task.getCaseInstanceId()).isEqualTo("aCaseInstanceId");

    task.setCaseInstanceId("anotherCaseInstanceId");
    taskService.saveTask(task);

    task = taskService.createTaskQuery().taskId(task.getId()).singleResult();
    assertThat(task.getCaseInstanceId()).isEqualTo("anotherCaseInstanceId");

    // Finally, delete task
    taskService.deleteTask(task.getId(), true);

  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testGetVariablesTyped() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);
    String taskId = taskService.createTaskQuery().singleResult().getId();
    VariableMap variablesTyped = taskService.getVariablesTyped(taskId);
    assertThat(variablesTyped).isEqualTo(vars);
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testGetVariablesTypedDeserialize() {

    runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables()
          .putValue("broken", Variables.serializedObjectValue("broken")
              .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
              .objectTypeName("unexisting").create()));
    String taskId = taskService.createTaskQuery().singleResult().getId();

    // this works
    VariableMap variablesTyped = taskService.getVariablesTyped(taskId, false);
    assertNotNull(variablesTyped.getValueTyped("broken"));
    variablesTyped = taskService.getVariablesTyped(taskId, List.of("broken"), false);
    assertNotNull(variablesTyped.getValueTyped("broken"));

    // this does not
    try {
      taskService.getVariablesTyped(taskId);
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Cannot deserialize object", e.getMessage());
    }

    // this does not
    try {
      taskService.getVariablesTyped(taskId, List.of("broken"), true);
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Cannot deserialize object", e.getMessage());
    }
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testGetVariablesLocalTyped() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("variable1", "value1");
    vars.put("variable2", "value2");

    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.setVariablesLocal(taskId, vars);

    VariableMap variablesTyped = taskService.getVariablesLocalTyped(taskId);
    assertThat(variablesTyped).isEqualTo(vars);
  }

  @Deployment(resources={
  "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testGetVariablesLocalTypedDeserialize() {
    runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.setVariablesLocal(taskId, Variables.createVariables()
          .putValue("broken", Variables.serializedObjectValue("broken")
              .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
              .objectTypeName("unexisting").create()));

    // this works
    VariableMap variablesTyped = taskService.getVariablesLocalTyped(taskId, false);
    assertNotNull(variablesTyped.getValueTyped("broken"));
    variablesTyped = taskService.getVariablesLocalTyped(taskId, List.of("broken"), false);
    assertNotNull(variablesTyped.getValueTyped("broken"));

    // this does not
    try {
      taskService.getVariablesLocalTyped(taskId);
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Cannot deserialize object", e.getMessage());
    }

    // this does not
    try {
      taskService.getVariablesLocalTyped(taskId, List.of("broken"), true);
    } catch(ProcessEngineException e) {
      testRule.assertTextPresent("Cannot deserialize object", e.getMessage());
    }

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testHumanTaskCompleteWithVariables() {
    // given
    caseService.createCaseInstanceByKey("oneTaskCase");

    var humanTask = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();
    assertThat(humanTask).isNotNull();

    String taskId = taskService.createTaskQuery().singleResult().getId();

    String variableName = "aVariable";
    String variableValue = "aValue";

    // when
    taskService.complete(taskId, Variables.createVariables().putValue(variableName, variableValue));

    // then
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();

    assertThat(variable.getName()).isEqualTo(variableName);
    assertThat(variable.getValue()).isEqualTo(variableValue);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testHumanTaskWithLocalVariablesCompleteWithVariable() {
    // given
    caseService.createCaseInstanceByKey("oneTaskCase");

    var humanTask = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();
    assertThat(humanTask).isNotNull();

    String variableName = "aVariable";
    String variableValue = "aValue";
    String variableAnotherValue = "anotherValue";

    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.setVariableLocal(taskId, variableName, variableValue);

    // when
    taskService.complete(taskId, Variables.createVariables().putValue(variableName, variableAnotherValue));

    // then
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();

    assertThat(variable.getName()).isEqualTo(variableName);
    assertThat(variable.getValue()).isEqualTo(variableAnotherValue);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/twoTasksProcess.bpmn20.xml"})
  @Test
  public void testUserTaskWithLocalVariablesCompleteWithVariable() {
    // given
    runtimeService.startProcessInstanceByKey("twoTasksProcess");

    String variableName = "aVariable";
    String variableValue = "aValue";
    String variableAnotherValue = "anotherValue";

    String taskId = taskService.createTaskQuery().singleResult().getId();

    taskService.setVariableLocal(taskId, variableName, variableValue);

    // when
    taskService.complete(taskId, Variables.createVariables().putValue(variableName, variableAnotherValue));

    // then
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();

    assertThat(variable.getName()).isEqualTo(variableName);
    assertThat(variable.getValue()).isEqualTo(variableAnotherValue);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testHumanTaskLocalVariables() {
    // given
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    String variableName = "aVariable";
    String variableValue = "aValue";

    String taskId = taskService.createTaskQuery().singleResult().getId();

    // when
    taskService.setVariableLocal(taskId, variableName, variableValue);

    // then
    VariableInstance variableInstance = runtimeService
      .createVariableInstanceQuery()
      .taskIdIn(taskId)
      .singleResult();
    assertNotNull(variableInstance);

    assertThat(variableInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(variableInstance.getCaseExecutionId()).isEqualTo(humanTaskId);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testGetVariablesByEmptyList() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
    String taskId = taskService.createTaskQuery()
      .processInstanceId(processInstanceId)
      .singleResult()
      .getId();

    // when
    Map<String, Object> variables = taskService.getVariables(taskId, new ArrayList<>());

    // then
    assertNotNull(variables);
    assertTrue(variables.isEmpty());
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testGetVariablesTypedByEmptyList() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
    String taskId = taskService.createTaskQuery()
      .processInstanceId(processInstanceId)
      .singleResult()
      .getId();

    // when
    Map<String, Object> variables = taskService.getVariablesTyped(taskId, new ArrayList<>(), false);

    // then
    assertNotNull(variables);
    assertTrue(variables.isEmpty());
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testGetVariablesLocalByEmptyList() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
    String taskId = taskService.createTaskQuery()
      .processInstanceId(processInstanceId)
      .singleResult()
      .getId();

    // when
    Map<String, Object> variables = taskService.getVariablesLocal(taskId, new ArrayList<>());

    // then
    assertNotNull(variables);
    assertTrue(variables.isEmpty());
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testGetVariablesLocalTypedByEmptyList() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
    String taskId = taskService.createTaskQuery()
      .processInstanceId(processInstanceId)
      .singleResult()
      .getId();

    // when
    Map<String, Object> variables = taskService.getVariablesLocalTyped(taskId, new ArrayList<>(), false);

    // then
    assertNotNull(variables);
    assertTrue(variables.isEmpty());
  }

  @Test
  public void testHandleBpmnErrorWithNonexistingTask() {
    // given
    // non-existing task

    // when/then
    assertThatThrownBy(() -> taskService.handleBpmnError("non-existing", ERROR_CODE))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Cannot find task with id non-existing: task is null");
  }

  @Test
  public void testThrowBpmnErrorWithoutCatch() {
    // given
    BpmnModelInstance model =Bpmn.createExecutableProcess(PROCESS_KEY)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask(USER_TASK_THROW_ERROR)
        .userTask("skipped-error")
        .endEvent()
        .done();
    testRule.deploy(model);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ERROR);

    // when
    taskService.handleBpmnError(task.getId(), ERROR_CODE);

    // then
    List<ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).list();
    assertThat(processInstances.size()).isEqualTo(0);
  }

  @Test
  public void testHandleBpmnErrorWithErrorCodeVariable() {
    // given
    BpmnModelInstance model = createUserTaskProcessWithCatchBoundaryEvent();
    testRule.deploy(model);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ERROR);

    // when
    taskService.handleBpmnError(task.getId(), ERROR_CODE);

    // then
    Task taskAfterThrow = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskAfterThrow.getTaskDefinitionKey()).isEqualTo(USER_TASK_AFTER_CATCH);
    VariableInstance errorCodeVariable = runtimeService.createVariableInstanceQuery().variableName("errorCodeVar").singleResult();
    assertThat(errorCodeVariable.getValue()).isEqualTo(ERROR_CODE);
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testHandleBpmnErrorWithEmptyErrorCode() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    String taskId = task.getId();

    // when/then
    assertThatThrownBy(() -> taskService.handleBpmnError(taskId, ""))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("errorCode is empty");
  }

  @Test
  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  public void testHandleBpmnErrorWithNullErrorCode() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    String taskId = task.getId();

    // when/then
    assertThatThrownBy(() -> taskService.handleBpmnError(taskId, null))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("errorCode is null");
  }

  @Test
  public void testHandleBpmnErrorIncludingMessage() {
    // given
    BpmnModelInstance model = createUserTaskProcessWithCatchBoundaryEvent();
    testRule.deploy(model);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ERROR);
    String errorMessageValue = "Error message for ERROR-" + ERROR_CODE;

    // when
    taskService.handleBpmnError(task.getId(), ERROR_CODE, errorMessageValue);

    // then
    Task taskAfterThrow = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskAfterThrow.getTaskDefinitionKey()).isEqualTo(USER_TASK_AFTER_CATCH);
    VariableInstance errorMessageVariable = runtimeService.createVariableInstanceQuery().variableName("errorMessageVar").singleResult();
    assertThat(errorMessageVariable.getValue()).isEqualTo(errorMessageValue);
  }

  @Test
  public void testHandleBpmnErrorWithVariables() {
    // given
    BpmnModelInstance model = createUserTaskProcessWithCatchBoundaryEvent();
    testRule.deploy(model);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ERROR);
    String variableName = "foo";
    String variableValue = "bar";

    // when
    taskService.handleBpmnError(task.getId(), ERROR_CODE, null, Variables.createVariables().putValue(variableName, variableValue));

    // then
    Task taskAfterThrow = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskAfterThrow.getTaskDefinitionKey()).isEqualTo(USER_TASK_AFTER_CATCH);
    VariableInstance variablePassedDuringThrowError = runtimeService.createVariableInstanceQuery().variableName(variableName).singleResult();
    assertThat(variablePassedDuringThrowError.getValue()).isEqualTo(variableValue);
  }

  @Test
  public void testThrowBpmnErrorCatchInEventSubprocess() {
    // given
    String errorCodeVariableName = "errorCodeVar";
    String errorMessageVariableName = "errorMessageVar";
    BpmnModelInstance model = createUserTaskProcessWithEventSubprocess(errorCodeVariableName, errorMessageVariableName);
    testRule.deploy(model);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ERROR);
    String variableName = "foo";
    String variableValue = "bar";
    String errorMessageValue = "Error message for ERROR-" + ERROR_CODE;

    // when
    taskService.handleBpmnError(task.getId(), ERROR_CODE, errorMessageValue, Variables.createVariables().putValue(variableName, variableValue));

    // then
    Task taskAfterThrow = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskAfterThrow.getTaskDefinitionKey()).isEqualTo(USER_TASK_AFTER_CATCH);
    VariableInstance variablePassedDuringThrowError = runtimeService.createVariableInstanceQuery().variableName(variableName).singleResult();
    assertThat(variablePassedDuringThrowError.getValue()).isEqualTo(variableValue);
    VariableInstance errorMessageVariable = runtimeService.createVariableInstanceQuery().variableName(errorMessageVariableName).singleResult();
    assertThat(errorMessageVariable.getValue()).isEqualTo(errorMessageValue);
    VariableInstance errorCodeVariable = runtimeService.createVariableInstanceQuery().variableName(errorCodeVariableName).singleResult();
    assertThat(errorCodeVariable.getValue()).isEqualTo(ERROR_CODE);
  }

  @Test
  public void testHandleEscalationWithNonexistingTask() {
    // given
    // non-existing task

    // when/then
    assertThatThrownBy(() -> taskService.handleEscalation("non-existing", ESCALATION_CODE))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("Cannot find task with id non-existing: task is null");
  }

  @Test
  public void testHandleEscalationWithoutEscalationCode() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .operatonHistoryTimeToLive(180)
        .startEvent()
      .userTask(USER_TASK_THROW_ESCALATION).boundaryEvent("catch-escalation").escalation(ESCALATION_CODE)
      .userTask(USER_TASK_AFTER_CATCH).endEvent().moveToActivity(USER_TASK_THROW_ESCALATION)
      .userTask(USER_TASK_AFTER_THROW).endEvent().done();
    testRule.deploy(model);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ESCALATION);
    String taskId = task.getId();

    // when/then
    assertThatThrownBy(() -> taskService.handleEscalation(taskId, ""))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("escalationCode is empty");

    assertThatThrownBy(() -> taskService.handleEscalation(taskId, null))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("escalationCode is null");
  }

  @Test
  public void testThrowEscalationWithoutCatchEvent() {
    // given
    BpmnModelInstance model =Bpmn.createExecutableProcess(PROCESS_KEY)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask(USER_TASK_THROW_ESCALATION)
        .userTask("skipped-error")
        .endEvent()
        .done();
    testRule.deploy(model);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ESCALATION);
    String taskId = task.getId();

    // when/then
    assertThatThrownBy(() -> taskService.handleEscalation(taskId, ESCALATION_CODE))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Execution with id '" + task.getTaskDefinitionKey()
      + "' throws an escalation event with escalationCode '" + ESCALATION_CODE
      + "', but no escalation handler was defined.");
  }

  @Test
  public void testHandleEscalationInterruptEventWithVariables() {
    // given
    BpmnModelInstance model = Bpmn.createExecutableProcess(PROCESS_KEY)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask(USER_TASK_THROW_ESCALATION)
          .boundaryEvent("catch-escalation")
            .escalation(ESCALATION_CODE)
          .userTask(USER_TASK_AFTER_CATCH)
          .endEvent()
        .moveToActivity(USER_TASK_THROW_ESCALATION)
        .userTask(USER_TASK_AFTER_THROW)
        .endEvent()
        .done();
    testRule.deploy(model);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ESCALATION);

    // when
    taskService.handleEscalation(task.getId(), ESCALATION_CODE, Variables.createVariables().putValue("foo", "bar"));

    // then
    Task taskAfterThrow = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskAfterThrow.getTaskDefinitionKey()).isEqualTo(USER_TASK_AFTER_CATCH);
    assertThat(runtimeService.createVariableInstanceQuery().variableName("foo").singleResult().getValue()).isEqualTo("bar");
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/task/TaskServiceTest.handleUserTaskEscalation.bpmn20.xml" })
  public void testHandleEscalationNonInterruptWithVariables() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ESCALATION);

    // when
    taskService.handleEscalation(task.getId(), "301", Variables.createVariables().putValue("foo", "bar"));

    // then
    List<Task> list = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertThat(list.size()).isEqualTo(2);
    for (Task taskAfterThrow : list) {
      if (!taskAfterThrow.getTaskDefinitionKey().equals(task.getTaskDefinitionKey()) && !taskAfterThrow.getTaskDefinitionKey().equals("after-301")) {
        fail("Two task should be active:" + task.getTaskDefinitionKey() + " & "
                                          + "after-301");
      }
    }
    assertThat(runtimeService.createVariableInstanceQuery().variableName("foo").singleResult().getValue()).isEqualTo("bar");
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/task/TaskServiceTest.handleUserTaskEscalation.bpmn20.xml" })
  public void testHandleEscalationInterruptWithVariables() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ESCALATION);

    // when
    taskService.handleEscalation(task.getId(), "302", Variables.createVariables().putValue("foo", "bar"));

    // then
    Task taskAfterThrow = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskAfterThrow.getTaskDefinitionKey()).isEqualTo("after-302");
    assertThat(runtimeService.createVariableInstanceQuery().variableName("foo").singleResult().getValue()).isEqualTo("bar");
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/task/TaskServiceTest.handleUserTaskEscalation.bpmn20.xml" })
  public void testHandleEscalationNonInterruptEventSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ESCALATION);

    // when
    taskService.handleEscalation(task.getId(), "303");

    // then
    List<Task> list = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertThat(list.size()).isEqualTo(2);
    for (Task taskAfterThrow : list) {
      if (!taskAfterThrow.getTaskDefinitionKey().equals(task.getTaskDefinitionKey()) && !taskAfterThrow.getTaskDefinitionKey().equals("after-303")) {
        fail("Two task should be active:" + task.getTaskDefinitionKey() + " & "
                                          + "after-303");
      }
    }
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/task/TaskServiceTest.handleUserTaskEscalation.bpmn20.xml" })
  public void testHandleEscalationInterruptInEventSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ESCALATION);

    // when
    taskService.handleEscalation(task.getId(), "304", Variables.createVariables().putValue("foo", "bar"));

    // then
    Task taskAfterThrow = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskAfterThrow.getTaskDefinitionKey()).isEqualTo("after-304");
    assertThat(runtimeService.createVariableInstanceQuery().variableName("foo").singleResult().getValue()).isEqualTo("bar");
  }


  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/task/TaskServiceTest.handleUserTaskEscalation.bpmn20.xml" })
  public void testHandleEscalationNonInterruptEmbeddedSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ESCALATION);

    // when
    taskService.handleEscalation(task.getId(), "305");

    // then
    List<Task> list = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
    assertThat(list.size()).isEqualTo(2);
    for (Task taskAfterThrow : list) {
      if (!taskAfterThrow.getTaskDefinitionKey().equals(task.getTaskDefinitionKey()) && !taskAfterThrow.getTaskDefinitionKey().equals("after-305")) {
        fail("Two task should be active:" + task.getTaskDefinitionKey() + " & "
                                          + "after-305");
      }
    }
  }

  @Test
  @Deployment(resources = { "org/operaton/bpm/engine/test/api/task/TaskServiceTest.handleUserTaskEscalation.bpmn20.xml" })
  public void testHandleEscalationInterruptInEmbeddedSubprocess() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(task.getTaskDefinitionKey()).isEqualTo(USER_TASK_THROW_ESCALATION);

    // when
    taskService.handleEscalation(task.getId(), "306", Variables.createVariables().putValue("foo", "bar"));

    // then
    Task taskAfterThrow = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskAfterThrow.getTaskDefinitionKey()).isEqualTo("after-306");
    assertThat(runtimeService.createVariableInstanceQuery().variableName("foo").singleResult().getValue()).isEqualTo("bar");
  }

  protected BpmnModelInstance createUserTaskProcessWithCatchBoundaryEvent() {
    return Bpmn.createExecutableProcess(PROCESS_KEY)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask(USER_TASK_THROW_ERROR)
          .boundaryEvent("catch-error")
            .errorEventDefinition()
              .error(ERROR_CODE)
              .errorCodeVariable("errorCodeVar")
              .errorMessageVariable("errorMessageVar")
            .errorEventDefinitionDone()
          .userTask(USER_TASK_AFTER_CATCH)
          .endEvent()
        .moveToActivity(USER_TASK_THROW_ERROR)
        .userTask(USER_TASK_AFTER_THROW)
        .endEvent()
        .done();
  }

  protected BpmnModelInstance createUserTaskProcessWithEventSubprocess(
      String errorCodeVariable, String errorMessageVariableName) {
    ProcessBuilder processBuilder = Bpmn.createExecutableProcess(PROCESS_KEY);

    BpmnModelInstance model = processBuilder
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask(USER_TASK_THROW_ERROR)
        .userTask(USER_TASK_AFTER_THROW)
        .endEvent()
        .done();
    processBuilder.eventSubProcess()
       .startEvent("catch-error")
         .errorEventDefinition()
           .error(ERROR_CODE)
           .errorCodeVariable(errorCodeVariable)
           .errorMessageVariable(errorMessageVariableName)
         .errorEventDefinitionDone()
       .userTask(USER_TASK_AFTER_CATCH)
       .endEvent();
    return model;
  }

}
