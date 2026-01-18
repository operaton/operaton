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
package org.operaton.bpm.engine.test.bpmn.gateway;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joram Barrez
 */
class ParallelGatewayTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;
  HistoryService historyService;

  /**
   * Case where there is a parallel gateway that splits into 3 paths of
   * execution, that are immediately joined, without any wait states in between.
   * In the end, no executions should be in the database.
   */
  @Deployment
  @Test
  void testSplitMergeNoWaitstates() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("forkJoinNoWaitStates");
    assertThat(processInstance.isEnded()).isTrue();
  }

  @Deployment
  @Test
  void testUnstructuredConcurrencyTwoForks() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("unstructuredConcurrencyTwoForks");
    assertThat(processInstance.isEnded()).isTrue();
  }

  @Deployment
  @Test
  void testUnstructuredConcurrencyTwoJoins() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("unstructuredConcurrencyTwoJoins");
    assertThat(processInstance.isEnded()).isTrue();
  }

  @Deployment
  @Test
  void testForkFollowedByOnlyEndEvents() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("forkFollowedByEndEvents");
    assertThat(processInstance.isEnded()).isTrue();
  }

  @Deployment
  @Test
  void testNestedForksFollowedByEndEvents() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("nestedForksFollowedByEndEvents");
    assertThat(processInstance.isEnded()).isTrue();
  }

  // ACT-482
  @Deployment
  @Test
  void testNestedForkJoin() {
    String pid = runtimeService.startProcessInstanceByKey("nestedForkJoin").getId();

    // After process start, only task 0 should be active
    TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
    List<Task> tasks = query.list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("Task 0");
    assertThat(runtimeService.getActivityInstance(pid).getChildActivityInstances()).hasSize(1);

    // Completing task 0 will create Task A and B
    taskService.complete(tasks.get(0).getId());
    tasks = query.list();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getName()).isEqualTo("Task A");
    assertThat(tasks.get(1).getName()).isEqualTo("Task B");
    assertThat(runtimeService.getActivityInstance(pid).getChildActivityInstances()).hasSize(2);

    // Completing task A should not trigger any new tasks
    taskService.complete(tasks.get(0).getId());
    tasks = query.list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("Task B");
    assertThat(runtimeService.getActivityInstance(pid).getChildActivityInstances()).hasSize(2);

    // Completing task B creates tasks B1 and B2
    taskService.complete(tasks.get(0).getId());
    tasks = query.list();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getName()).isEqualTo("Task B1");
    assertThat(tasks.get(1).getName()).isEqualTo("Task B2");
    assertThat(runtimeService.getActivityInstance(pid).getChildActivityInstances()).hasSize(3);

    // Completing B1 and B2 will activate both joins, and process reaches task C
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());
    tasks = query.list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("Task C");
    assertThat(runtimeService.getActivityInstance(pid).getChildActivityInstances()).hasSize(1);
  }

  /**
   * <a href="http://jira.codehaus.org/browse/ACT-1222">ACT-1222</a>
   */
  @Deployment
  @Test
  void testRecyclingExecutionWithCallActivity() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("parent-process").getId();

    // After process start we have two tasks, one from the parent and one from
    // the sub process
    TaskQuery query = taskService.createTaskQuery().orderByTaskName().asc();
    List<Task> tasks = query.list();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getName()).isEqualTo("Another task");
    assertThat(tasks.get(1).getName()).isEqualTo("Some Task");

    // we complete the task from the parent process, the root execution is
    // recycled, the task in the sub process is still there
    taskService.complete(tasks.get(1).getId());
    tasks = query.list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("Another task");

    // we end the task in the sub process and the sub process instance end is
    // propagated to the parent process
    taskService.complete(tasks.get(0).getId());
    assertThat(taskService.createTaskQuery().count()).isZero();

    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).finished().count()).isEqualTo(1);
  }

  @Deployment
  @Test
  void testCompletingJoin() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    assertThat(processInstance.isEnded()).isTrue();
  }

  @Deployment
  @Test
  void testAsyncParallelGateway() {

    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    assertThat(jobDefinition).isNotNull();
    assertThat(jobDefinition.getActivityId()).isEqualTo("parallelJoinEnd");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    assertThat(processInstance.isEnded()).isFalse();

    // there are two jobs to continue the gateway:
    List<Job> list = managementService.createJobQuery().list();
    assertThat(list).hasSize(2);

    managementService.executeJob(list.get(0).getId());
    managementService.executeJob(list.get(1).getId());

    assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNull();
  }

  @Deployment
  @Test
  void testAsyncParallelGatewayAfterScopeTask() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    assertThat(processInstance.isEnded()).isFalse();

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // there are two jobs to continue the gateway:
    List<Job> list = managementService.createJobQuery().list();
    assertThat(list).hasSize(2);

    managementService.executeJob(list.get(0).getId());
    managementService.executeJob(list.get(1).getId());

    assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNull();
  }

  @Deployment
  @Test
  void testCompletingJoinInSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    assertThat(processInstance.isEnded()).isTrue();
  }

  @Deployment
  @Test
  void testParallelGatewayBeforeAndInSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(3);

    ActivityInstance instance = runtimeService.getActivityInstance(processInstance.getId());
    assertThat(instance.getActivityName()).isEqualTo("Process1");
    ActivityInstance[] childActivityInstances = instance.getChildActivityInstances();
    for (ActivityInstance activityInstance : childActivityInstances) {
      if ("SubProcess_1".equals(activityInstance.getActivityId())) {
        ActivityInstance[] instances = activityInstance.getChildActivityInstances();
        for (ActivityInstance activityInstance2 : instances) {
          assertThat(activityInstance2.getActivityName()).isIn("Inner User Task 1", "Inner User Task 2");
        }
      } else {
        assertThat(activityInstance.getActivityName()).isEqualTo("Outer User Task");
      }
    }
  }

  @Deployment
  @Test
  void testForkJoin() {

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("forkJoin");
    TaskQuery query = taskService
                        .createTaskQuery()
                        .processInstanceId(pi.getId())
                        .orderByTaskName()
                        .asc();

    List<Task> tasks = query.list();
    assertThat(tasks).hasSize(2);
    // the tasks are ordered by name (see above)
    Task task1 = tasks.get(0);
    assertThat(task1.getName()).isEqualTo("Receive Payment");
    Task task2 = tasks.get(1);
    assertThat(task2.getName()).isEqualTo("Ship Order");

    // Completing both tasks will join the concurrent executions
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());

    tasks = query.list();
    assertThat(tasks).hasSize(1);
    assertThat(tasks.get(0).getName()).isEqualTo("Archive Order");
  }

  @Deployment
  @Test
  void testUnbalancedForkJoin() {

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("UnbalancedForkJoin");
    TaskQuery query = taskService.createTaskQuery()
                                 .processInstanceId(pi.getId())
                                 .orderByTaskName()
                                 .asc();

    List<Task> tasks = query.list();
    assertThat(tasks).hasSize(3);
    // the tasks are ordered by name (see above)
    Task task1 = tasks.get(0);
    assertThat(task1.getName()).isEqualTo("Task 1");
    Task task2 = tasks.get(1);
    assertThat(task2.getName()).isEqualTo("Task 2");

    // Completing the first task should *not* trigger the join
    taskService.complete(task1.getId());

    // Completing the second task should trigger the first join
    taskService.complete(task2.getId());

    tasks = query.list();
    Task task3 = tasks.get(0);
    assertThat(tasks).hasSize(2);
    assertThat(task3.getName()).isEqualTo("Task 3");
    Task task4 = tasks.get(1);
    assertThat(task4.getName()).isEqualTo("Task 4");

    // Completing the remaining tasks should trigger the second join and end the process
    taskService.complete(task3.getId());
    taskService.complete(task4.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Test
  void testRemoveConcurrentExecutionLocalVariablesOnJoin() {
   testRule.deploy(Bpmn.createExecutableProcess("process")
      .startEvent()
      .parallelGateway("fork")
      .userTask("task1")
      .parallelGateway("join")
      .userTask("afterTask")
      .endEvent()
      .moveToNode("fork")
      .userTask("task2")
      .connectTo("join")
      .done());

    // given
    runtimeService.startProcessInstanceByKey("process");

    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task task : tasks) {
      runtimeService.setVariableLocal(task.getExecutionId(), "var", "value");
    }

    // when
    taskService.complete(tasks.get(0).getId());
    taskService.complete(tasks.get(1).getId());

    // then
    assertThat(runtimeService.createVariableInstanceQuery().count()).isZero();
  }

  @Deployment
  @Test
  void testImplicitParallelGatewayAfterSignalBehavior() {
    // given
    Exception exceptionOccurred = null;
    runtimeService.startProcessInstanceByKey("process");
    Execution execution = runtimeService.createExecutionQuery()
      .activityId("service")
      .singleResult();

    // when
    try {
      runtimeService.signal(execution.getId());
    } catch (Exception e) {
      exceptionOccurred = e;
    }

    // then
    assertThat(exceptionOccurred).isNull();
    assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
  }

  @Deployment
  @Test
  void testExplicitParallelGatewayAfterSignalBehavior() {
    // given
    runtimeService.startProcessInstanceByKey("process");
    Execution execution = runtimeService.createExecutionQuery()
      .activityId("service")
      .singleResult();

    // when
    runtimeService.signal(execution.getId());

    // then
    assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testParallelGatewayCancellationHistoryEvent() {
    // given
    // a process with one splitting and one merging parallel gateway and two parallel sequence flows between them
    // one sequence flow has a wait state "Event_Wait", the other has none
    testRule.deploy(Bpmn.createExecutableProcess("parallelProcess")
        .startEvent()
        .parallelGateway("Gateway_in")
          .intermediateCatchEvent("Event_Wait")
          .message("Message_1")
        .parallelGateway("Gateway_out")
        .endEvent()
        .moveToNode("Gateway_in")
        .connectTo("Gateway_out")
        .done());

    var processInstance = runtimeService.startProcessInstanceByKey("parallelProcess");

    // when
    // cancel "Event_Wait" first
    // then cancel "Gateway_out" (merging parallel gateway)
    this.runtimeService.createProcessInstanceModification(processInstance.getId())
        .cancelAllForActivity("Event_Wait")
        .cancelAllForActivity("Gateway_out")
        .execute();

    Date currentTime = ClockUtil.now();

    // then
    // the whole process instance is canceled and history is produced
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult()).isNull();
    assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).singleResult()).isNotNull();

    //
    var historicActivityInstanceEventWait = this.historyService.createHistoricActivityInstanceQuery()
        .activityId("Event_Wait")
        .singleResult();
    assertThat(historicActivityInstanceEventWait.getEndTime()).isCloseTo(currentTime, 2000);

    var historicActivityInstanceMergingGateway = this.historyService.createHistoricActivityInstanceQuery()
        .activityId("Gateway_out")
        .singleResult();
    assertThat(historicActivityInstanceMergingGateway.getEndTime()).isCloseTo(currentTime, 2000);
  }
}
