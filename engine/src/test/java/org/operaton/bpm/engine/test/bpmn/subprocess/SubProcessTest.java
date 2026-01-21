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
package org.operaton.bpm.engine.test.bpmn.subprocess;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.persistence.entity.ActivityInstanceImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.subprocess.util.GetActInstanceDelegate;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ActivityInstanceAssert;
import org.operaton.commons.utils.CollectionUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joram Barrez
 * @author Falko Menge
 */
class SubProcessTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;
  RepositoryService repositoryService;

  @Deployment
  @Test
  void testSimpleSubProcess() {

    // After staring the process, the task in the subprocess should be active
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess");
    Task subProcessTask = taskService.createTaskQuery()
                                                   .processInstanceId(pi.getId())
                                                   .singleResult();
    assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

    // we have 3 levels in the activityInstance:
    // pd
    ActivityInstance rootActivityInstance = runtimeService.getActivityInstance(pi.getProcessInstanceId());
    assertThat(rootActivityInstance.getActivityId()).isEqualTo(pi.getProcessDefinitionId());
    //subprocess
    assertThat(rootActivityInstance.getChildActivityInstances()).hasSize(1);
    ActivityInstance subProcessInstance = rootActivityInstance.getChildActivityInstances()[0];
    assertThat(subProcessInstance.getActivityId()).isEqualTo("subProcess");
    // usertask
    assertThat(subProcessInstance.getChildActivityInstances()).hasSize(1);
    ActivityInstance userTaskInstance = subProcessInstance.getChildActivityInstances()[0];
    assertThat(userTaskInstance.getActivityId()).isEqualTo("subProcessTask");

    // After completing the task in the subprocess,
    // the subprocess scope is destroyed and the complete process ends
    taskService.complete(subProcessTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(pi.getId()).singleResult()).isNull();
  }

  /**
   * Same test case as before, but now with all automatic steps
   */
  @Deployment
  @Test
  void testSimpleAutomaticSubProcess() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcessAutomatic");
    assertThat(pi.isEnded()).isTrue();
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testSimpleSubProcessWithTimer() {

    Date startTime = new Date();

    // After staring the process, the task in the subprocess should be active
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcess");
    Task subProcessTask = taskService.createTaskQuery()
                                                   .processInstanceId(pi.getId())
                                                   .singleResult();
    assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

    // we have 3 levels in the activityInstance:
    // pd
    ActivityInstance rootActivityInstance = runtimeService.getActivityInstance(pi.getProcessInstanceId());
    assertThat(rootActivityInstance.getActivityId()).isEqualTo(pi.getProcessDefinitionId());
    //subprocess
    assertThat(rootActivityInstance.getChildActivityInstances()).hasSize(1);
    ActivityInstance subProcessInstance = rootActivityInstance.getChildActivityInstances()[0];
    assertThat(subProcessInstance.getActivityId()).isEqualTo("subProcess");
    // usertask
    assertThat(subProcessInstance.getChildActivityInstances()).hasSize(1);
    ActivityInstance userTaskInstance = subProcessInstance.getChildActivityInstances()[0];
    assertThat(userTaskInstance.getActivityId()).isEqualTo("subProcessTask");

    // Setting the clock forward 2 hours 1 second (timer fires in 2 hours) and fire up the job executor
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + (2 * 60 * 60 * 1000 ) + 1000));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // The subprocess should be left, and the escalated task should be active
    Task escalationTask = taskService.createTaskQuery()
                                                   .processInstanceId(pi.getId())
                                                   .singleResult();
    assertThat(escalationTask.getName()).isEqualTo("Fix escalated problem");
  }

  /**
   * A test case that has a timer attached to the subprocess,
   * where 2 concurrent paths are defined when the timer fires.
   */
  @Deployment
  public void IGNORE_testSimpleSubProcessWithConcurrentTimer() {

    // After staring the process, the task in the subprocess should be active
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleSubProcessWithConcurrentTimer");
    TaskQuery taskQuery = taskService
      .createTaskQuery()
      .processInstanceId(pi.getId())
      .orderByTaskName()
      .asc();

    Task subProcessTask = taskQuery.singleResult();
    assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

    // When the timer is fired (after 2 hours), two concurrent paths should be created
    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());

    List<Task> tasksAfterTimer = taskQuery.list();
    assertThat(tasksAfterTimer).hasSize(2);
    Task taskAfterTimer1 = tasksAfterTimer.get(0);
    Task taskAfterTimer2 = tasksAfterTimer.get(1);
    assertThat(taskAfterTimer1.getName()).isEqualTo("Task after timer 1");
    assertThat(taskAfterTimer2.getName()).isEqualTo("Task after timer 2");

    // Completing the two tasks should end the process instance
    taskService.complete(taskAfterTimer1.getId());
    taskService.complete(taskAfterTimer2.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  /**
   * Test case where the simple sub process of previous test cases
   * is nested within another subprocess.
   */
  @Deployment
  @Test
  void testNestedSimpleSubProcess() {

    // Start and delete a process with a nested subprocess when it is not yet ended
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("nestedSimpleSubProcess", CollectionUtil.singletonMap("someVar", "abc"));
    runtimeService.deleteProcessInstance(pi.getId(), "deleted");

    // After staring the process, the task in the inner subprocess must be active
    pi = runtimeService.startProcessInstanceByKey("nestedSimpleSubProcess");
    Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

    // now we have 4 levels in the activityInstance:
    // pd
    ActivityInstance rootActivityInstance = runtimeService.getActivityInstance(pi.getProcessInstanceId());
    assertThat(rootActivityInstance.getActivityId()).isEqualTo(pi.getProcessDefinitionId());
    //subprocess1
    assertThat(rootActivityInstance.getChildActivityInstances()).hasSize(1);
    ActivityInstance subProcessInstance1 = rootActivityInstance.getChildActivityInstances()[0];
    assertThat(subProcessInstance1.getActivityId()).isEqualTo("outerSubProcess");
    //subprocess2
    assertThat(rootActivityInstance.getChildActivityInstances()).hasSize(1);
    ActivityInstance subProcessInstance2 = subProcessInstance1.getChildActivityInstances()[0];
    assertThat(subProcessInstance2.getActivityId()).isEqualTo("innerSubProcess");
    // usertask
    assertThat(subProcessInstance2.getChildActivityInstances()).hasSize(1);
    ActivityInstance userTaskInstance = subProcessInstance2.getChildActivityInstances()[0];
    assertThat(userTaskInstance.getActivityId()).isEqualTo("innerSubProcessTask");

    // After completing the task in the subprocess,
    // both subprocesses are destroyed and the task after the subprocess should be active
    taskService.complete(subProcessTask.getId());
    Task taskAfterSubProcesses = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(taskAfterSubProcesses).isNotNull();
    assertThat(taskAfterSubProcesses.getName()).isEqualTo("Task after subprocesses");
    taskService.complete(taskAfterSubProcesses.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testNestedSimpleSubprocessWithTimerOnInnerSubProcess() {
    Date startTime = new Date();

    // After staring the process, the task in the subprocess should be active
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("nestedSubProcessWithTimer");
    Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

    // now we have 4 levels in the activityInstance:
    // pd
    ActivityInstance rootActivityInstance = runtimeService.getActivityInstance(pi.getProcessInstanceId());
    assertThat(rootActivityInstance.getActivityId()).isEqualTo(pi.getProcessDefinitionId());
    //subprocess1
    assertThat(rootActivityInstance.getChildActivityInstances()).hasSize(1);
    ActivityInstance subProcessInstance1 = rootActivityInstance.getChildActivityInstances()[0];
    assertThat(subProcessInstance1.getActivityId()).isEqualTo("outerSubProcess");
    //subprocess2
    assertThat(rootActivityInstance.getChildActivityInstances()).hasSize(1);
    ActivityInstance subProcessInstance2 = subProcessInstance1.getChildActivityInstances()[0];
    assertThat(subProcessInstance2.getActivityId()).isEqualTo("innerSubProcess");
    // usertask
    assertThat(subProcessInstance2.getChildActivityInstances()).hasSize(1);
    ActivityInstance userTaskInstance = subProcessInstance2.getChildActivityInstances()[0];
    assertThat(userTaskInstance.getActivityId()).isEqualTo("innerSubProcessTask");

    // Setting the clock forward 1 hour 1 second (timer fires in 1 hour) and fire up the job executor
    ClockUtil.setCurrentTime(new Date(startTime.getTime() + ( 60 * 60 * 1000 ) + 1000));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // The inner subprocess should be destroyed, and the escalated task should be active
    Task escalationTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(escalationTask.getName()).isEqualTo("Escalated task");

    // now we have 3 levels in the activityInstance:
    // pd
    rootActivityInstance = runtimeService.getActivityInstance(pi.getProcessInstanceId());
    assertThat(rootActivityInstance.getActivityId()).isEqualTo(pi.getProcessDefinitionId());
    //subprocess1
    assertThat(rootActivityInstance.getChildActivityInstances()).hasSize(1);
    subProcessInstance1 = rootActivityInstance.getChildActivityInstances()[0];
    assertThat(subProcessInstance1.getActivityId()).isEqualTo("outerSubProcess");
    //subprocess2
    assertThat(rootActivityInstance.getChildActivityInstances()).hasSize(1);
    ActivityInstance escalationTaskInst = subProcessInstance1.getChildActivityInstances()[0];
    assertThat(escalationTaskInst.getActivityId()).isEqualTo("escalationTask");

    // Completing the escalated task, destroys the outer scope and activates the task after the subprocess
    taskService.complete(escalationTask.getId());
    Task taskAfterSubProcess = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after subprocesses");
  }

  /**
   * Test case where the simple sub process of previous test cases
   * is nested within two other sub processes
   */
  @Deployment
  @Test
  void testDoubleNestedSimpleSubProcess() {
    // After staring the process, the task in the inner subprocess must be active
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("nestedSimpleSubProcess");
    Task subProcessTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(subProcessTask.getName()).isEqualTo("Task in subprocess");

    // After completing the task in the subprocess,
    // both subprocesses are destroyed and the task after the subprocess should be active
    taskService.complete(subProcessTask.getId());
    Task taskAfterSubProcesses = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(taskAfterSubProcesses.getName()).isEqualTo("Task after subprocesses");
  }

  @Deployment
  @Test
  void testSimpleParallelSubProcess() {

    // After starting the process, the two task in the subprocess should be active
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("simpleParallelSubProcess");
    List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(pi.getId()).orderByTaskName().asc().list();

    // Tasks are ordered by name (see query)
    Task taskA = subProcessTasks.get(0);
    Task taskB = subProcessTasks.get(1);
    assertThat(taskA.getName()).isEqualTo("Task A");
    assertThat(taskB.getName()).isEqualTo("Task B");

    ActivityInstance rootActivityInstance = runtimeService.getActivityInstance(pi.getProcessInstanceId());
    assertThat(rootActivityInstance.getActivityId()).isEqualTo(pi.getProcessDefinitionId());
    //subprocess1
    assertThat(rootActivityInstance.getChildActivityInstances()).hasSize(1);
    ActivityInstance subProcessInstance = rootActivityInstance.getChildActivityInstances()[0];
    assertThat(subProcessInstance.getActivityId()).isEqualTo("subProcess");
    // 2 tasks are present
    assertThat(subProcessInstance.getChildActivityInstances()).hasSize(2);

    // Completing both tasks, should destroy the subprocess and activate the task after the subprocess
    taskService.complete(taskA.getId());

    rootActivityInstance = runtimeService.getActivityInstance(pi.getProcessInstanceId());
    assertThat(rootActivityInstance.getActivityId()).isEqualTo(pi.getProcessDefinitionId());
    subProcessInstance = rootActivityInstance.getChildActivityInstances()[0];
    // 1 task + 1 join
    assertThat(subProcessInstance.getChildActivityInstances()).hasSize(2);

    taskService.complete(taskB.getId());
    Task taskAfterSubProcess = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after sub process");
  }

  @Deployment
  @Test
  void testSimpleParallelSubProcessWithTimer() {

    // After staring the process, the tasks in the subprocess should be active
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleParallelSubProcessWithTimer");
    List<Task> subProcessTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).orderByTaskName().asc().list();

    // Tasks are ordered by name (see query)
    Task taskA = subProcessTasks.get(0);
    Task taskB = subProcessTasks.get(1);
    assertThat(taskA.getName()).isEqualTo("Task A");
    assertThat(taskB.getName()).isEqualTo("Task B");

    Job job = managementService
      .createJobQuery()
      .processInstanceId(processInstance.getId())
      .singleResult();

    managementService.executeJob(job.getId());

    // The inner subprocess should be destroyed, and the tsk after the timer should be active
    Task taskAfterTimer = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(taskAfterTimer.getName()).isEqualTo("Task after timer");

    // Completing the task after the timer ends the process instance
    taskService.complete(taskAfterTimer.getId());
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment
  @Test
  void testTwoSubProcessInParallel() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("twoSubProcessInParallel");
    TaskQuery taskQuery = taskService
      .createTaskQuery()
      .processInstanceId(pi.getId())
      .orderByTaskName()
      .asc();
    List<Task> tasks = taskQuery.list();

    // After process start, both tasks in the subprocesses should be active
    assertThat(tasks.get(0).getName()).isEqualTo("Task in subprocess A");
    assertThat(tasks.get(1).getName()).isEqualTo("Task in subprocess B");

    // validate activity instance tree
    ActivityInstance rootActivityInstance = runtimeService.getActivityInstance(pi.getProcessInstanceId());
    assertThat(rootActivityInstance.getActivityId()).isEqualTo(pi.getProcessDefinitionId());
    assertThat(rootActivityInstance.getChildActivityInstances()).hasSize(2);
    ActivityInstance[] childActivityInstances = rootActivityInstance.getChildActivityInstances();
    for (ActivityInstance activityInstance : childActivityInstances) {
      assertThat(List.of("subProcessA", "subProcessB")).contains(activityInstance.getActivityId());
      ActivityInstance[] subProcessChildren = activityInstance.getChildActivityInstances();
      assertThat(subProcessChildren).hasSize(1);
      assertThat(List.of("subProcessATask", "subProcessBTask")).contains(subProcessChildren[0].getActivityId());
    }

    // Completing both tasks should active the tasks outside the subprocesses
    taskService.complete(tasks.get(0).getId());

    tasks = taskQuery.list();
    assertThat(tasks.get(0).getName()).isEqualTo("Task after subprocess A");
    assertThat(tasks.get(1).getName()).isEqualTo("Task in subprocess B");

    taskService.complete(tasks.get(1).getId());

    tasks = taskQuery.list();

    assertThat(tasks.get(0).getName()).isEqualTo("Task after subprocess A");
    assertThat(tasks.get(1).getName()).isEqualTo("Task after subprocess B");

    // Completing these tasks should end the process
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testTwoSubProcessInParallelWithinSubProcess() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("twoSubProcessInParallelWithinSubProcess");
    TaskQuery taskQuery = taskService
      .createTaskQuery()
      .processInstanceId(pi.getId())
      .orderByTaskName()
      .asc();
    List<Task> tasks = taskQuery.list();

    // After process start, both tasks in the subprocesses should be active
    Task taskA = tasks.get(0);
    Task taskB = tasks.get(1);
    assertThat(taskA.getName()).isEqualTo("Task in subprocess A");
    assertThat(taskB.getName()).isEqualTo("Task in subprocess B");

    // validate activity instance tree
    ActivityInstance rootActivityInstance = runtimeService.getActivityInstance(pi.getProcessInstanceId());
    ActivityInstanceAssert.assertThat(rootActivityInstance)
    .hasStructure(
        ActivityInstanceAssert
        .describeActivityInstanceTree(pi.getProcessDefinitionId())
          .beginScope("outerSubProcess")
            .beginScope("subProcessA")
              .activity("subProcessATask")
            .endScope()
            .beginScope("subProcessB")
              .activity("subProcessBTask")
        .done());

    // Completing both tasks should active the tasks outside the subprocesses
    taskService.complete(taskA.getId());
    taskService.complete(taskB.getId());

    Task taskAfterSubProcess = taskQuery.singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after subprocess");

    // Completing this task should end the process
    taskService.complete(taskAfterSubProcess.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testTwoNestedSubProcessesInParallelWithTimer() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("nestedParallelSubProcessesWithTimer");
    TaskQuery taskQuery = taskService
      .createTaskQuery()
      .processInstanceId(pi.getId())
      .orderByTaskName()
      .asc();
    List<Task> tasks = taskQuery.list();

    // After process start, both tasks in the subprocesses should be active
    Task taskA = tasks.get(0);
    Task taskB = tasks.get(1);
    assertThat(taskA.getName()).isEqualTo("Task in subprocess A");
    assertThat(taskB.getName()).isEqualTo("Task in subprocess B");

    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());

    Task taskAfterTimer = taskQuery.singleResult();
    assertThat(taskAfterTimer.getName()).isEqualTo("Task after timer");

    // Completing the task should end the process instance
    taskService.complete(taskAfterTimer.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  /**
   * @see <a href="http://jira.codehaus.org/browse/ACT-1072">http://jira.codehaus.org/browse/ACT-1072</a>
   */
  @Deployment
  @Test
  void testNestedSimpleSubProcessWithoutEndEvent() {
    testNestedSimpleSubProcess();
  }

  /**
   * @see <a href="http://jira.codehaus.org/browse/ACT-1072">http://jira.codehaus.org/browse/ACT-1072</a>
   */
  @Deployment
  @Test
  void testSimpleSubProcessWithoutEndEvent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testSimpleSubProcessWithoutEndEvent");
    testRule.assertProcessEnded(pi.getId());
  }

  /**
   * @see <a href="http://jira.codehaus.org/browse/ACT-1072">http://jira.codehaus.org/browse/ACT-1072</a>
   */
  @Deployment
  @Test
  void testNestedSubProcessesWithoutEndEvents() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testNestedSubProcessesWithoutEndEvents");
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/nestedSubProcess.bpmn20.xml", "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testInstanceSubProcessInstanceIdSet() {
    // given
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("nestedSimpleSubProcess");
    ActivityInstance rootActivityInstance = runtimeService.getActivityInstance(pi.getProcessInstanceId());
    ActivityInstance subProcessInstance = rootActivityInstance.getChildActivityInstances()[0];

    // when
    String subProcessInstanceId = ((ActivityInstanceImpl) subProcessInstance).getSubProcessInstanceId();

    // then
    assertThat(subProcessInstanceId).isNotNull();
    ProcessInstance subProcess = runtimeService.createProcessInstanceQuery().processDefinitionKey("simpleSubProcess").singleResult();
    assertThat(subProcess.getId()).isEqualTo(subProcessInstanceId);
  }

  @Deployment
  // SEE https://app.camunda.com/jira/browse/CAM-2169
  @Test
  void testActivityInstanceTreeNestedCmd() {
    GetActInstanceDelegate.activityInstance = null;
    runtimeService.startProcessInstanceByKey("process");

    ActivityInstance activityInstance = GetActInstanceDelegate.activityInstance;

    assertThat(activityInstance).isNotNull();
    ActivityInstance subProcessInstance = activityInstance.getChildActivityInstances()[0];
    assertThat(subProcessInstance).isNotNull();
    assertThat(subProcessInstance.getActivityId()).isEqualTo("SubProcess_1");

    ActivityInstance serviceTaskInstance = subProcessInstance.getChildActivityInstances()[0];
    assertThat(serviceTaskInstance).isNotNull();
    assertThat(serviceTaskInstance.getActivityId()).isEqualTo("ServiceTask_1");
  }

  @Deployment
  // SEE https://app.camunda.com/jira/browse/CAM-2169
  @Test
  void testActivityInstanceTreeNestedCmdAfterTx() {
    GetActInstanceDelegate.activityInstance = null;
    runtimeService.startProcessInstanceByKey("process");

    // send message
    runtimeService.correlateMessage("message");

    ActivityInstance activityInstance = GetActInstanceDelegate.activityInstance;

    assertThat(activityInstance).isNotNull();
    ActivityInstance subProcessInstance = activityInstance.getChildActivityInstances()[0];
    assertThat(subProcessInstance).isNotNull();
    assertThat(subProcessInstance.getActivityId()).isEqualTo("SubProcess_1");

    ActivityInstance serviceTaskInstance = subProcessInstance.getChildActivityInstances()[0];
    assertThat(serviceTaskInstance).isNotNull();
    assertThat(serviceTaskInstance.getActivityId()).isEqualTo("ServiceTask_1");
  }

  @Test
  void testConcurrencyInSubProcess() {

    org.operaton.bpm.engine.repository.Deployment deployment =
      repositoryService.createDeployment()
                  .addClasspathResource("org/operaton/bpm/engine/test/bpmn/subprocess/SubProcessTest.fixSystemFailureProcess.bpmn20.xml")
                  .deploy();

    // After staring the process, both tasks in the subprocess should be active
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("fixSystemFailure");
    List<Task> tasks = taskService.createTaskQuery()
                                  .processInstanceId(pi.getId())
                                  .orderByTaskName()
                                  .asc()
                                  .list();

    // Tasks are ordered by name (see query)
    assertThat(tasks).hasSize(2);
    Task investigateHardwareTask = tasks.get(0);
    Task investigateSoftwareTask = tasks.get(1);
    assertThat(investigateHardwareTask.getName()).isEqualTo("Investigate hardware");
    assertThat(investigateSoftwareTask.getName()).isEqualTo("Investigate software");

    // Completing both the tasks finishes the subprocess and enables the task after the subprocess
    taskService.complete(investigateHardwareTask.getId());
    taskService.complete(investigateSoftwareTask.getId());

    Task writeReportTask = taskService
      .createTaskQuery()
      .processInstanceId(pi.getId())
      .singleResult();
    assertThat(writeReportTask.getName()).isEqualTo("Write report");

    // Clean up
    repositoryService.deleteDeployment(deployment.getId(), true);
  }
}
