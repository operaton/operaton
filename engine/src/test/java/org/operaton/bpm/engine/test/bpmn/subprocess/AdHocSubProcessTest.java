/*
 * Copyright 2026 FINOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.AdHocActivity;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.AD_HOC_ACTIVE_ACTIVITY_IDS;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.AD_HOC_COMPLETED_ACTIVITY_IDS;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.AD_HOC_COMPLETION_CONDITION_SATISFIED;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.AD_HOC_LAST_COMPLETED_ACTIVITY_ID;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.NUMBER_OF_ACTIVE_AD_HOC_ACTIVITIES;
import static org.operaton.bpm.engine.impl.bpmn.behavior.AdHocSubProcessActivityBehavior.NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES;

class AdHocSubProcessTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ManagementService managementService;
  RepositoryService repositoryService;
  RuntimeService runtimeService;
  TaskService taskService;

  @Deployment
  @Test
  void testTriggerAdHocActivityAndCompleteSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    List<Task> adHocTasks = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .orderByTaskName()
        .asc()
        .list();

    assertThat(adHocTasks).hasSize(2);

    // find tasks by definition key instead of relying on order
    Task taskA = adHocTasks.stream()
        .filter(t -> "taskA".equals(t.getTaskDefinitionKey()))
        .findFirst()
        .orElse(null);
    Task taskB = adHocTasks.stream()
        .filter(t -> "taskB".equals(t.getTaskDefinitionKey()))
        .findFirst()
        .orElse(null);

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNotNull();

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();
    assertNoAdHocInternalStateVariable(processInstance.getId(), adHocExecution.getId());

    taskService.complete(taskA.getId());
    taskService.complete(taskB.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment
  @Test
  void testParallelOrderingStartsConfiguredActivitiesInParallel() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessParallelOrdering");

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNotNull();

    taskService.complete(taskA.getId());
    taskService.complete(taskB.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.modelIdleNoInitialTasks.bpmn20.xml")
  @Test
  void testParallelOrderingAllowsSameActivityWhileActive() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskA"), null);
    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskA"), null);

    List<Task> taskAInstances = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .list();

    assertThat(taskAInstances).hasSize(2);

    for (Task task : taskAInstances) {
      taskService.complete(task.getId());
    }

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.modelIdleNoInitialTasks.bpmn20.xml")
  @Test
  void testDiscoverParallelStartableAdHocActivities() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    List<AdHocActivity> startableActivities = runtimeService.getStartableAdHocActivities(adHocExecution.getId());
    assertStartableAdHocActivity(startableActivities, "taskA", "Task A", "userTask");
    assertStartableAdHocActivity(startableActivities, "taskB", "Task B", "userTask");
    assertThat(startableActivities).hasSize(2);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskA"), null);

    startableActivities = runtimeService.getStartableAdHocActivities(adHocExecution.getId());
    assertStartableAdHocActivity(startableActivities, "taskA", "Task A", "userTask");
    assertStartableAdHocActivity(startableActivities, "taskB", "Task B", "userTask");
    assertThat(startableActivities).hasSize(2);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testSequentialOrderingStartsSingleConfiguredActivity.bpmn20.xml")
  @Test
  void testSequentialOrderingStartsSingleConfiguredActivity() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessSequentialOrdering");

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNull();

    taskService.complete(taskA.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testSequentialOrderingAllowsOneActiveActivityAtATime.bpmn20.xml")
  @Test
  void testDiscoverSequentialStartableAdHocActivities() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessSequentialOrdering");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    List<AdHocActivity> startableActivities = runtimeService.getStartableAdHocActivities(adHocExecution.getId());
    assertStartableAdHocActivity(startableActivities, "taskA", "Task A", "userTask");
    assertStartableAdHocActivity(startableActivities, "taskB", "Task B", "userTask");
    assertThat(startableActivities).hasSize(2);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskA"), null);

    startableActivities = runtimeService.getStartableAdHocActivities(adHocExecution.getId());
    assertThat(startableActivities).isEmpty();

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    taskService.complete(taskA.getId());

    adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    startableActivities = runtimeService.getStartableAdHocActivities(adHocExecution.getId());
    assertStartableAdHocActivity(startableActivities, "taskA", "Task A", "userTask");
    assertStartableAdHocActivity(startableActivities, "taskB", "Task B", "userTask");
    assertThat(startableActivities).hasSize(2);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testSequentialOrderingAllowsOneActiveActivityAtATime.bpmn20.xml")
  @Test
  void testSequentialOrderingAllowsOneActiveActivityAtATime() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessSequentialOrdering");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskA"), null);

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    assertThat(taskA).isNotNull();

    try {
      runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskB"), null);
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("Sequential adHocSubProcess 'adHocSubProcess' already has an active child activity",
          e.getMessage());
    }

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult()).isNull();

    taskService.complete(taskA.getId());

    adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    try {
      runtimeService.triggerAdHocActivities(adHocExecution.getId(), Arrays.asList("taskA", "taskB"), null);
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("Sequential adHocSubProcess 'adHocSubProcess' can trigger only one activity per request",
          e.getMessage());
    }

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskB"), null);

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskB).isNotNull();

    taskService.complete(taskB.getId());

    adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    runtimeService.completeAdHocSubProcess(adHocExecution.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testSequentialOrderingBlocksWhileAsyncBeforeActivityWaitsForJob.bpmn20.xml")
  @Test
  void testSequentialOrderingBlocksWhileAsyncBeforeActivityWaitsForJob() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessSequentialAsync");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskA"), null);

    Job asyncBeforeJob = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertThat(asyncBeforeJob).isNotNull();
    assertThat(runtimeService.getStartableAdHocActivities(adHocExecution.getId())).isEmpty();

    try {
      runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskB"), null);
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("Sequential adHocSubProcess 'adHocSubProcess' already has an active child activity",
          e.getMessage());
    }

    managementService.executeJob(asyncBeforeJob.getId());

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    assertThat(taskA).isNotNull();
    taskService.complete(taskA.getId());

    adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();
    List<AdHocActivity> startableActivities = runtimeService.getStartableAdHocActivities(adHocExecution.getId());
    assertStartableAdHocActivity(startableActivities, "taskA", "Task A", "userTask");
    assertStartableAdHocActivity(startableActivities, "taskB", "Task B", "userTask");
    assertThat(startableActivities).hasSize(2);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskB"), null);

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskB).isNotNull();
    taskService.complete(taskB.getId());

    adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    runtimeService.completeAdHocSubProcess(adHocExecution.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testSequentialOrderingBlocksWhileAsyncBeforeActivityWaitsForJob.bpmn20.xml")
  @Test
  void testCompleteAdHocSubProcessFailsWhileAsyncBeforeActivityWaitsForJob() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessSequentialAsync");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskA"), null);

    try {
      runtimeService.completeAdHocSubProcess(adHocExecution.getId());
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("has active child activities and cannot be completed", e.getMessage());
    }

    Job asyncBeforeJob = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertThat(asyncBeforeJob).isNotNull();
    managementService.executeJob(asyncBeforeJob.getId());

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    assertThat(taskA).isNotNull();
    taskService.complete(taskA.getId());

    runtimeService.completeAdHocSubProcess(adHocExecution.getId());

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult()).isNotNull();
  }

  @Deployment
  @Test
  void testParallelActivationRespectsActiveTasksList() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
        "adHocSubProcessBasic",
        Collections.singletonMap("initialTaskIds", Collections.singletonList("taskB")));

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskA).isNull();
    assertThat(taskB).isNotNull();

    taskService.complete(taskB.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testMissingActiveTasksCollectionFailsAdHocStart.bpmn20.xml")
  @Test
  void testMissingActiveTasksCollectionLeavesAdHocSubProcessActive() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .count()).isZero();

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();
    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult()).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testParallelActivationRespectsActiveTasksList.bpmn20.xml")
  @Test
  void testEmptyActiveTasksCollectionLeavesAdHocSubProcessActive() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
        "adHocSubProcessBasic",
    Collections.singletonMap("initialTaskIds", Collections.emptyList()));

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .count()).isZero();

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();
    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult()).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.modelIdleNoInitialTasks.bpmn20.xml")
  @Test
  void testTriggerAdHocActivitiesAfterIdleStartActivatesTasks() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();
    assertNoAdHocInternalStateVariable(processInstance.getId(), adHocExecution.getId());

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Arrays.asList("taskA", "taskB"), null);

    assertNoAdHocInternalStateVariable(processInstance.getId(), adHocExecution.getId());

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();
    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNotNull();

    taskService.complete(taskA.getId());
    taskService.complete(taskB.getId());

    assertThat(runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .activityId("adHocSubProcess")
      .singleResult()).isNull();

    assertThat(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult()).isNotNull();

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.modelIdleNoInitialTasks.bpmn20.xml")
  @Test
  void testCompleteAdHocSubProcessAfterIdleStart() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    if (adHocExecution != null) {
      runtimeService.completeAdHocSubProcess(adHocExecution.getId());
    }

    assertThat(runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult()).isNull();

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult()).isNotNull();
  }

    @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.modelIdleNoInitialTasks.bpmn20.xml")
    @Test
    void testCompleteAdHocSubProcessWithVariablesAfterIdleStart() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Execution adHocExecution = runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .activityId("adHocSubProcess")
      .singleResult();

    assertThat(adHocExecution).isNotNull();

    runtimeService.completeAdHocSubProcess(adHocExecution.getId(),
      Collections.singletonMap("completionReason", "manual"));

    assertThat(runtimeService.getVariable(processInstance.getId(), "completionReason")).isEqualTo("manual");

    assertThat(runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .activityId("adHocSubProcess")
      .singleResult()).isNull();

    assertThat(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult()).isNotNull();
    }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testCompleteAdHocSubProcessCancelsActivitiesWhenCancelRemainingInstancesTrue.bpmn20.xml")
  @Test
  void testCompleteAdHocSubProcessCancelsActivitiesWhenCancelRemainingInstancesTrue() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    runtimeService.completeAdHocSubProcess(adHocExecution.getId());

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult()).isNull();

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult()).isNull();

    assertThat(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult()).isNotNull();

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testCompleteAdHocSubProcessFailsWhenActivitiesAreActiveAndCancelRemainingInstancesFalse.bpmn20.xml")
  @Test
  void testCompleteAdHocSubProcessFailsWhenActivitiesAreActiveAndCancelRemainingInstancesFalse() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessNoCancelRemaining");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    try {
      runtimeService.completeAdHocSubProcess(adHocExecution.getId());
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("has active child activities and cannot be completed", e.getMessage());
    }

    assertThat(runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult()).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testAdHocCommandsFailForNonAdHocExecution.bpmn20.xml")
  @Test
  void testCompleteAdHocSubProcessFailsForNonAdHocExecution() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleUserTaskProcess");

    Execution execution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("userTask")
        .singleResult();

    try {
      runtimeService.completeAdHocSubProcess(execution.getId());
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("is not waiting in an adHocSubProcess", e.getMessage());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testStarterActivitiesFlowToDownstreamTask.bpmn20.xml")
  @Test
  void testStarterActivitiesFlowToDownstreamTask() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithDownstreamFlow");

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    Task taskC = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskC")
        .singleResult();

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNotNull();
    assertThat(taskC).isNull();

    taskService.complete(taskA.getId());

    taskC = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskC")
        .singleResult();

    assertThat(taskC).isNotNull();

    taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskB).isNotNull();

    taskService.complete(taskB.getId());
    taskService.complete(taskC.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment
  @Test
  void testNonStarterConfiguredTasksFailAdHocStart() {
    try {
      runtimeService.startProcessInstanceByKey("adHocSubProcessWithDownstreamFlow");
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent(
          "activeTasksCollection contains non-startable activities in adHocSubProcess 'adHocSubProcess': [taskC]",
          e.getMessage());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testOperatonNamespaceActiveTasksCollectionStartsConfiguredTasks.bpmn20.xml")
  @Test
  void testOperatonNamespaceActiveTasksCollectionStartsConfiguredTasks() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testTriggerAdHocActivityWithUnknownActivityId.bpmn20.xml")
  @Test
  void testTriggerAdHocActivityWithUnknownActivityId() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    try {
      runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("doesNotExist"), null);
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("adHoc activity 'doesNotExist' does not exist in adHocSubProcess adHocSubProcess", e.getMessage());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testTriggerMultipleAdHocActivitiesWithActivityVariables.bpmn20.xml")
  @Test
  void testTriggerMultipleAdHocActivitiesWithActivityVariables() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithThreeTasks");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    Map<String, Map<String, Object>> activityVariables = new LinkedHashMap<>();
    Map<String, Object> taskBVariables = new HashMap<>();
    taskBVariables.put("assigneeHint", "john");
    Map<String, Object> taskCVariables = new HashMap<>();
    taskCVariables.put("assigneeHint", "mary");
    activityVariables.put("taskB", taskBVariables);
    activityVariables.put("taskC", taskCVariables);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Arrays.asList("taskB", "taskC"), activityVariables);

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    Task taskC = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskC")
        .singleResult();

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNotNull();
    assertThat(taskC).isNotNull();

    assertThat(runtimeService.getVariableLocal(taskB.getExecutionId(), "assigneeHint")).isEqualTo("john");
    assertThat(runtimeService.getVariableLocal(taskC.getExecutionId(), "assigneeHint")).isEqualTo("mary");
    assertThat(runtimeService.getVariableLocal(taskA.getExecutionId(), "assigneeHint")).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testTriggerMultipleAdHocActivitiesWithActivityVariables.bpmn20.xml")
  @Test
  void testTriggerMultipleAdHocActivitiesFailsAllWhenOneIsInvalid() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithThreeTasks");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    try {
      runtimeService.triggerAdHocActivities(adHocExecution.getId(), Arrays.asList("taskB", "doesNotExist"), null);
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("adHoc activity 'doesNotExist' does not exist", e.getMessage());
    }

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();
    Task taskC = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskC")
        .singleResult();

    assertThat(taskB).isNull();
    assertThat(taskC).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testAdHocCommandsFailForNonAdHocExecution.bpmn20.xml")
  @Test
  void testTriggerAdHocActivityFailsForNonAdHocExecution() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleUserTaskProcess");

    Execution execution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("userTask")
        .singleResult();

    try {
      runtimeService.triggerAdHocActivities(execution.getId(), Collections.singletonList("taskA"), null);
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("is not waiting in an adHocSubProcess", e.getMessage());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testStarterActivitiesFlowToDownstreamTask.bpmn20.xml")
  @Test
  void testTriggerAdHocActivityFailsForNonStarterActivity() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithDownstreamFlow");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    try {
      runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskC"), null);
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("adHoc activity 'taskC' is not startable in adHocSubProcess adHocSubProcess", e.getMessage());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testTriggerEmbeddedSubProcessAdHocActivity.bpmn20.xml")
  @Test
  void testTriggerEmbeddedSubProcessAdHocActivity() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithEmbeddedSubProcess");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    List<AdHocActivity> startableActivities = runtimeService.getStartableAdHocActivities(adHocExecution.getId());
    assertStartableAdHocActivity(startableActivities, "embeddedSubProcess", "Embedded SubProcess", "subProcess");
    assertStartableAdHocActivity(startableActivities, "taskB", "Task B", "userTask");
    assertThat(startableActivities).hasSize(2);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("embeddedSubProcess"), null);

    Task embeddedTask = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("embeddedTask")
        .singleResult();

    assertThat(embeddedTask).isNotNull();
    taskService.complete(embeddedTask.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testTriggerTransactionAdHocActivity.bpmn20.xml")
  @Test
  void testTriggerTransactionAdHocActivity() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithTransaction");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    List<AdHocActivity> startableActivities = runtimeService.getStartableAdHocActivities(adHocExecution.getId());
    assertStartableAdHocActivity(startableActivities, "transaction", "Transaction", "transaction");
    assertStartableAdHocActivity(startableActivities, "taskB", "Task B", "userTask");
    assertThat(startableActivities).hasSize(2);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("transaction"), null);

    Task transactionTask = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("transactionTask")
        .singleResult();

    assertThat(transactionTask).isNotNull();
    taskService.complete(transactionTask.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testTriggerCallActivityAdHocActivity.bpmn20.xml")
  @Test
  void testTriggerCallActivityAdHocActivity() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithCallActivity");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    List<AdHocActivity> startableActivities = runtimeService.getStartableAdHocActivities(adHocExecution.getId());
    assertStartableAdHocActivity(startableActivities, "callActivity", "Call Activity", "callActivity");
    assertStartableAdHocActivity(startableActivities, "taskB", "Task B", "userTask");
    assertThat(startableActivities).hasSize(2);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("callActivity"), null);

    ProcessInstance calledProcessInstance = runtimeService.createProcessInstanceQuery()
        .superProcessInstanceId(processInstance.getId())
        .singleResult();

    assertThat(calledProcessInstance).isNotNull();

    Task calledTask = taskService.createTaskQuery()
        .processInstanceId(calledProcessInstance.getId())
        .taskDefinitionKey("calledTask")
        .singleResult();

    assertThat(calledTask).isNotNull();
    taskService.complete(calledTask.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testBoundaryEventOnTriggeredAdHocActivityContinuesInsideScope.bpmn20.xml")
  @Test
  void testBoundaryEventOnTriggeredAdHocActivityContinuesInsideScope() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithChildBoundaryEvent");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskA"), null);

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    assertThat(taskA).isNotNull();

    Job boundaryTimer = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertThat(boundaryTimer).isNotNull();
    managementService.executeJob(boundaryTimer.getId());

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult()).isNull();

    Task boundaryTask = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("boundaryTask")
        .singleResult();

    assertThat(boundaryTask).isNotNull();
    taskService.complete(boundaryTask.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testStartabilityExcludesEventSubProcessesAndCompensationHandlers.bpmn20.xml")
  @Test
  void testStartabilityExcludesEventSubProcessesAndCompensationHandlers() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithNonStartableHandlers");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertThat(adHocExecution).isNotNull();

    List<AdHocActivity> startableActivities = runtimeService.getStartableAdHocActivities(adHocExecution.getId());
    assertStartableAdHocActivity(startableActivities, "taskA", "Task A", "userTask");
    assertThat(startableActivities).hasSize(1);

    try {
      runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("eventSubProcess"), null);
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("adHoc activity 'eventSubProcess' is not startable in adHocSubProcess adHocSubProcess",
          e.getMessage());
    }

    try {
      runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("compensationTask"), null);
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("adHoc activity 'compensationTask' is not startable in adHocSubProcess adHocSubProcess",
          e.getMessage());
    }
  }

  @Deployment
  @Test
  void testCompletionConditionCancelsRemainingActivities() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
        "adHocSubProcessWithCompletionCondition",
        Collections.singletonMap("approved", false));

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNotNull();

    // Completing taskA with approved=true triggers completion condition
    // which cancels taskB (due to cancelRemainingInstances="true")
    taskService.complete(taskA.getId(), Collections.singletonMap("approved", true));

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult()).isNull();

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();

    Task remainingTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertThat(remainingTasks.getTaskDefinitionKey()).isEqualTo("taskAfter");

    taskService.complete(taskAfter.getId());
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testCompletionConditionDefersUntilActiveActivitiesFinish.bpmn20.xml")
  @Test
  void testCompletionConditionDefersUntilActiveActivitiesFinish() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
        "adHocSubProcessWithDeferredCompletion",
        Collections.singletonMap("approved", false));

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNotNull();

    taskService.complete(taskA.getId(), Collections.singletonMap("approved", true));

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNull();

    taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskB).isNotNull();

    taskService.complete(taskB.getId());

    taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testCompletionConditionUsesCompletedActivityContext.bpmn20.xml")
  @Test
  void testCompletionConditionUsesCompletedActivityContext() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithCompletionContext");

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNotNull();

    taskService.complete(taskA.getId());

    taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskB).isNotNull();
    assertThat(((Number) runtimeService.getVariableLocal(taskB.getExecutionId(),
        NUMBER_OF_COMPLETED_AD_HOC_ACTIVITIES)).intValue()).isEqualTo(1);
    assertThat(((Number) runtimeService.getVariableLocal(taskB.getExecutionId(),
        NUMBER_OF_ACTIVE_AD_HOC_ACTIVITIES)).intValue()).isEqualTo(1);
    assertThat(runtimeService.getVariableLocal(taskB.getExecutionId(), AD_HOC_ACTIVE_ACTIVITY_IDS))
      .isEqualTo(Collections.singletonList("taskB"));
    assertThat(runtimeService.getVariableLocal(taskB.getExecutionId(), AD_HOC_COMPLETED_ACTIVITY_IDS))
      .isEqualTo(Collections.singletonList("taskA"));
    assertThat(runtimeService.getVariableLocal(taskB.getExecutionId(), AD_HOC_LAST_COMPLETED_ACTIVITY_ID))
      .isEqualTo("taskA");

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult()).isNull();

    taskService.complete(taskB.getId());

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult()).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testCompletionConditionUsesActiveCountContext.bpmn20.xml")
  @Test
  void testCompletionConditionUsesActiveCountContext() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithActiveCountContext");

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNotNull();

    taskService.complete(taskA.getId());

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult()).isNull();

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult()).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testCompletionConditionSatisfactionIsLatched.bpmn20.xml")
  @Test
  void testCompletionConditionSatisfactionIsLatchedUntilActiveActivitiesFinish() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithLatchedCompletion");

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNotNull();

    taskService.complete(taskA.getId());

    taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertThat(taskB).isNotNull();
    assertThat(runtimeService.getVariableLocal(taskB.getExecutionId(), AD_HOC_COMPLETION_CONDITION_SATISFIED))
      .isEqualTo(Boolean.TRUE);
    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult()).isNull();

    taskService.complete(taskB.getId());

    assertThat(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult()).isNotNull();
  }

  @Deployment
  @Test
  void testBoundaryErrorEventOnAdHocSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBoundaryError");

    Task boundaryTask = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("boundaryTask")
        .singleResult();

    Task adHocTask = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(boundaryTask).isNotNull();
    assertThat(adHocTask).isNull();
    assertThat(taskAfter).isNull();

    taskService.complete(boundaryTask.getId());
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
  }

  @Deployment
  @Test
  void testMultiInstanceParallelAdHocSubProcessStartsAllInstances() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessMultiInstanceParallel");

    List<Task> adHocTasks = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .orderByTaskName()
        .asc()
        .list();

    assertThat(adHocTasks).hasSize(4);

    long taskACount = adHocTasks.stream().filter(t -> "taskA".equals(t.getTaskDefinitionKey())).count();
    long taskBCount = adHocTasks.stream().filter(t -> "taskB".equals(t.getTaskDefinitionKey())).count();
    assertThat(taskACount).isEqualTo(2);
    assertThat(taskBCount).isEqualTo(2);

    for (Task task : adHocTasks) {
      taskService.complete(task.getId());
    }

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

  @Deployment
  @Test
  void testMultiInstanceSequentialAdHocSubProcessStartsOneInstanceAtATime() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessMultiInstanceSequential");

    List<Task> firstInstanceTasks = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .orderByTaskName()
        .asc()
        .list();

    assertThat(firstInstanceTasks).hasSize(2);

    long firstInstanceTaskACount = firstInstanceTasks.stream().filter(t -> "taskA".equals(t.getTaskDefinitionKey())).count();
    long firstInstanceTaskBCount = firstInstanceTasks.stream().filter(t -> "taskB".equals(t.getTaskDefinitionKey())).count();
    assertThat(firstInstanceTaskACount).isEqualTo(1);
    assertThat(firstInstanceTaskBCount).isEqualTo(1);

    for (Task task : firstInstanceTasks) {
      taskService.complete(task.getId());
    }

    List<Task> secondInstanceTasks = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .orderByTaskName()
        .asc()
        .list();

    assertThat(secondInstanceTasks).hasSize(2);

    long secondInstanceTaskACount = secondInstanceTasks.stream().filter(t -> "taskA".equals(t.getTaskDefinitionKey())).count();
    long secondInstanceTaskBCount = secondInstanceTasks.stream().filter(t -> "taskB".equals(t.getTaskDefinitionKey())).count();
    assertThat(secondInstanceTaskACount).isEqualTo(1);
    assertThat(secondInstanceTaskBCount).isEqualTo(1);

    for (Task task : secondInstanceTasks) {
      taskService.complete(task.getId());
    }

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertThat(taskAfter).isNotNull();
  }

    @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testAutoCompleteAttributeFalseKeepsAdHocSubProcessOpen.bpmn20.xml")
    @Test
    void testAutoCompleteAttributeFalseKeepsAdHocSubProcessOpen() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessAutoCompleteAttributeFalse");

    Task taskA = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskA")
      .singleResult();

    Task taskB = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskB")
      .singleResult();

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNotNull();

    taskService.complete(taskA.getId());
    taskService.complete(taskB.getId());

    Execution adHocExecution = runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .activityId("adHocSubProcess")
      .singleResult();

    assertThat(adHocExecution).isNotNull();
    assertThat(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult()).isNull();

    runtimeService.completeAdHocSubProcess(adHocExecution.getId());

    assertThat(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult()).isNotNull();
    }

    @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testOperatonNamespaceAutoCompleteAttributeFalseKeepsAdHocSubProcessOpen.bpmn20.xml")
    @Test
    void testOperatonNamespaceAutoCompleteAttributeFalseKeepsAdHocSubProcessOpen() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessAutoCompleteAttributeFalseOperatonNamespace");

    Task taskA = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskA")
      .singleResult();

    assertThat(taskA).isNotNull();

    taskService.complete(taskA.getId());

    Execution adHocExecution = runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .activityId("adHocSubProcess")
      .singleResult();

    assertThat(adHocExecution).isNotNull();
    assertThat(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult()).isNull();

    runtimeService.completeAdHocSubProcess(adHocExecution.getId());

    assertThat(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult()).isNotNull();
    }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testAutoCompleteAttributeTrueAutoCompletesAdHocSubProcess.bpmn20.xml")
  @Test
  void testAutoCompleteAttributeTrueAutoCompletesAdHocSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessAutoCompleteAttributeTrue");

    Task taskA = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskA")
      .singleResult();

    Task taskB = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskB")
      .singleResult();

    assertThat(taskA).isNotNull();
    assertThat(taskB).isNotNull();

    taskService.complete(taskA.getId());
    taskService.complete(taskB.getId());

    assertThat(runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .activityId("adHocSubProcess")
      .singleResult()).isNull();

    assertThat(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult()).isNotNull();
  }

  @Test
  void testAutoCompletePropertyFailsParse() {
    String resource = "org/operaton/bpm/engine/test/bpmn/subprocess/"
        + "AdHocSubProcessTest.testInvalidAutoCompletePropertyFailsParse.bpmn20.xml";

    try {
      repositoryService.createDeployment()
          .name(resource)
          .addClasspathResource(resource)
          .deploy();
      fail("Expected ParseException");
    } catch (ParseException e) {
      testRule.assertTextPresent(
          "Unsupported ad-hoc extension property 'autoComplete'; use extension attribute 'autoComplete' on the adHocSubProcess element",
          e.getMessage());
    }
  }

  @Test
  void testInvalidAutoCompleteAttributeFailsParse() {
    String resource = "org/operaton/bpm/engine/test/bpmn/subprocess/"
        + "AdHocSubProcessTest.testInvalidAutoCompleteAttributeFailsParse.bpmn20.xml";

    try {
      repositoryService.createDeployment()
          .name(resource)
          .addClasspathResource(resource)
          .deploy();
      fail("Expected ParseException");
    } catch (ParseException e) {
      testRule.assertTextPresent(
          "Invalid value 'maybe' for ad-hoc extension attribute 'autoComplete'; expected boolean value",
          e.getMessage());
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testSequentialOrderingRejectsMultipleConfiguredInitialActivities.bpmn20.xml")
  @Test
  void testSequentialOrderingRejectsMultipleConfiguredInitialActivities() {
    try {
      runtimeService.startProcessInstanceByKey("adHocSubProcessSequentialOrdering");
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent(
          "Sequential adHocSubProcess 'adHocSubProcess' can activate only one activity from activeTasksCollection",
          e.getMessage());
    }
  }

  protected void assertNoAdHocInternalStateVariable(String processInstanceId, String adHocExecutionId) {
    assertThat(runtimeService.getVariableLocal(adHocExecutionId, "adHocActivityStarted")).isNull();
    assertThat(runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(processInstanceId)
        .variableName("adHocActivityStarted")
        .count()).isZero();
  }

  protected void assertStartableAdHocActivity(List<AdHocActivity> activities,
      String activityId,
      String activityName,
      String activityType) {
    AdHocActivity activity = activities.stream()
        .filter(candidate -> activityId.equals(candidate.getActivityId()))
        .findFirst()
        .orElse(null);

    assertThat(activity).isNotNull();
    assertThat(activity.getActivityName()).isEqualTo(activityName);
    assertThat(activity.getActivityType()).isEqualTo(activityType);
  }
}
