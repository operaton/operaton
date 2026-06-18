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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

public class AdHocSubProcessTest extends PluggableProcessEngineTest {

  @Deployment
  @Test
  public void testTriggerAdHocActivityAndCompleteSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    List<Task> adHocTasks = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .orderByTaskName()
        .asc()
        .list();

    assertEquals(2, adHocTasks.size());

    // find tasks by definition key instead of relying on order
    Task taskA = adHocTasks.stream()
        .filter(t -> "taskA".equals(t.getTaskDefinitionKey()))
        .findFirst()
        .orElse(null);
    Task taskB = adHocTasks.stream()
        .filter(t -> "taskB".equals(t.getTaskDefinitionKey()))
        .findFirst()
        .orElse(null);

    assertNotNull(taskA);
    assertNotNull(taskB);

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertNotNull(adHocExecution);
    assertNoAdHocInternalStateVariable(processInstance.getId(), adHocExecution.getId());

    taskService.complete(taskA.getId());
    taskService.complete(taskB.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertNotNull(taskAfter);
  }

  @Deployment
  @Test
  public void testParallelOrderingStartsConfiguredActivitiesInParallel() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessParallelOrdering");

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertNotNull(taskA);
    assertNotNull(taskB);

    taskService.complete(taskA.getId());
    taskService.complete(taskB.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertNotNull(taskAfter);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.modelIdleNoInitialTasks.bpmn20.xml")
  @Test
  public void testParallelOrderingAllowsSameActivityWhileActive() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertNotNull(adHocExecution);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskA"), null);
    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskA"), null);

    List<Task> taskAInstances = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .list();

    assertEquals(2, taskAInstances.size());

    for (Task task : taskAInstances) {
      taskService.complete(task.getId());
    }

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertNotNull(taskAfter);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testSequentialOrderingStartsSingleConfiguredActivity.bpmn20.xml")
  @Test
  public void testSequentialOrderingStartsSingleConfiguredActivity() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessSequentialOrdering");

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertNotNull(taskA);
    assertNull(taskB);

    taskService.complete(taskA.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertNotNull(taskAfter);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testSequentialOrderingAllowsOneActiveActivityAtATime.bpmn20.xml")
  @Test
  public void testSequentialOrderingAllowsOneActiveActivityAtATime() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessSequentialOrdering");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertNotNull(adHocExecution);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskA"), null);

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    assertNotNull(taskA);

    try {
      runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskB"), null);
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("Sequential adHocSubProcess 'adHocSubProcess' already has an active child activity",
          e.getMessage());
    }

    assertNull(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult());

    taskService.complete(taskA.getId());

    adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertNotNull(adHocExecution);

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

    assertNotNull(taskB);

    taskService.complete(taskB.getId());

    adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertNotNull(adHocExecution);

    runtimeService.completeAdHocSubProcess(adHocExecution.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertNotNull(taskAfter);
  }

  @Deployment
  @Test
  public void testParallelActivationRespectsActiveTasksList() {
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

    assertNull(taskA);
    assertNotNull(taskB);

    taskService.complete(taskB.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertNotNull(taskAfter);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testMissingActiveTasksCollectionFailsAdHocStart.bpmn20.xml")
  @Test
  public void testMissingActiveTasksCollectionLeavesAdHocSubProcessActive() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    assertEquals(0, taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .count());

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertNotNull(adHocExecution);
    assertNull(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testParallelActivationRespectsActiveTasksList.bpmn20.xml")
  @Test
  public void testEmptyActiveTasksCollectionLeavesAdHocSubProcessActive() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
        "adHocSubProcessBasic",
    Collections.singletonMap("initialTaskIds", Collections.emptyList()));

    assertEquals(0, taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .count());

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertNotNull(adHocExecution);
    assertNull(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.modelIdleNoInitialTasks.bpmn20.xml")
  @Test
  public void testTriggerAdHocActivitiesAfterIdleStartActivatesTasks() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertNotNull(adHocExecution);
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

    assertNotNull(taskA);
    assertNotNull(taskB);

    taskService.complete(taskA.getId());
    taskService.complete(taskB.getId());

    assertNull(runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .activityId("adHocSubProcess")
      .singleResult());

    assertNotNull(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult());

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.modelIdleNoInitialTasks.bpmn20.xml")
  @Test
  public void testCompleteAdHocSubProcessAfterIdleStart() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    if (adHocExecution != null) {
      runtimeService.completeAdHocSubProcess(adHocExecution.getId());
    }

    assertNull(runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult());

    assertNotNull(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult());
  }

    @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.modelIdleNoInitialTasks.bpmn20.xml")
    @Test
    public void testCompleteAdHocSubProcessWithVariablesAfterIdleStart() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Execution adHocExecution = runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .activityId("adHocSubProcess")
      .singleResult();

    assertNotNull(adHocExecution);

    runtimeService.completeAdHocSubProcess(adHocExecution.getId(),
      Collections.singletonMap("completionReason", "manual"));

    assertEquals("manual", runtimeService.getVariable(processInstance.getId(), "completionReason"));

    assertNull(runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .activityId("adHocSubProcess")
      .singleResult());

    assertNotNull(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult());
    }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testCompleteAdHocSubProcessCancelsActivitiesWhenCancelRemainingInstancesTrue.bpmn20.xml")
  @Test
  public void testCompleteAdHocSubProcessCancelsActivitiesWhenCancelRemainingInstancesTrue() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertNotNull(adHocExecution);

    runtimeService.completeAdHocSubProcess(adHocExecution.getId());

    assertNull(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult());

    assertNull(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult());

    assertNotNull(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult());

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testCompleteAdHocSubProcessFailsWhenActivitiesAreActiveAndCancelRemainingInstancesFalse.bpmn20.xml")
  @Test
  public void testCompleteAdHocSubProcessFailsWhenActivitiesAreActiveAndCancelRemainingInstancesFalse() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessNoCancelRemaining");

    Execution adHocExecution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult();

    assertNotNull(adHocExecution);

    try {
      runtimeService.completeAdHocSubProcess(adHocExecution.getId());
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent("has active child activities and cannot be completed", e.getMessage());
    }

    assertNotNull(runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId("adHocSubProcess")
        .singleResult());
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testAdHocCommandsFailForNonAdHocExecution.bpmn20.xml")
  @Test
  public void testCompleteAdHocSubProcessFailsForNonAdHocExecution() {
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
  public void testStarterActivitiesFlowToDownstreamTask() {
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

    assertNotNull(taskA);
    assertNotNull(taskB);
    assertNull(taskC);

    taskService.complete(taskA.getId());

    taskC = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskC")
        .singleResult();

    assertNotNull(taskC);

    taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertNotNull(taskB);

    taskService.complete(taskB.getId());
    taskService.complete(taskC.getId());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertNotNull(taskAfter);
  }

  @Deployment
  @Test
  public void testNonStarterConfiguredTasksFailAdHocStart() {
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
  public void testOperatonNamespaceActiveTasksCollectionStartsConfiguredTasks() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");

    Task taskA = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskA")
        .singleResult();

    Task taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertNotNull(taskA);
    assertNull(taskB);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testTriggerAdHocActivityWithUnknownActivityId.bpmn20.xml")
  @Test
  public void testTriggerAdHocActivityWithUnknownActivityId() {
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
  public void testTriggerMultipleAdHocActivitiesWithActivityVariables() {
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

    assertNotNull(taskA);
    assertNotNull(taskB);
    assertNotNull(taskC);

    assertEquals("john", runtimeService.getVariableLocal(taskB.getExecutionId(), "assigneeHint"));
    assertEquals("mary", runtimeService.getVariableLocal(taskC.getExecutionId(), "assigneeHint"));
    assertNull(runtimeService.getVariableLocal(taskA.getExecutionId(), "assigneeHint"));
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testTriggerMultipleAdHocActivitiesWithActivityVariables.bpmn20.xml")
  @Test
  public void testTriggerMultipleAdHocActivitiesFailsAllWhenOneIsInvalid() {
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

    assertNull(taskB);
    assertNull(taskC);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testAdHocCommandsFailForNonAdHocExecution.bpmn20.xml")
  @Test
  public void testTriggerAdHocActivityFailsForNonAdHocExecution() {
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
  public void testTriggerAdHocActivityFailsForNonStarterActivity() {
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

  @Deployment
  @Test
  public void testCompletionConditionCancelsRemainingActivities() {
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

    assertNotNull(taskA);
    assertNotNull(taskB);

    // Completing taskA with approved=true triggers completion condition
    // which cancels taskB (due to cancelRemainingInstances="true")
    taskService.complete(taskA.getId(), Collections.singletonMap("approved", true));

    assertNull(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult());

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertNotNull(taskAfter);

    Task remainingTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
    assertEquals("taskAfter", remainingTasks.getTaskDefinitionKey());

    taskService.complete(taskAfter.getId());
    assertTrue(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count() == 0);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testCompletionConditionDefersUntilActiveActivitiesFinish.bpmn20.xml")
  @Test
  public void testCompletionConditionDefersUntilActiveActivitiesFinish() {
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

    assertNotNull(taskA);
    assertNotNull(taskB);

    taskService.complete(taskA.getId(), Collections.singletonMap("approved", true));

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertNull(taskAfter);

    taskB = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskB")
        .singleResult();

    assertNotNull(taskB);

    taskService.complete(taskB.getId());

    taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertNotNull(taskAfter);
  }

  @Deployment
  @Test
  public void testBoundaryErrorEventOnAdHocSubProcess() {
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

    assertNotNull(boundaryTask);
    assertNull(adHocTask);
    assertNull(taskAfter);

    taskService.complete(boundaryTask.getId());
    assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count());
  }

  @Deployment
  @Test
  public void testMultiInstanceParallelAdHocSubProcessStartsAllInstances() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessMultiInstanceParallel");

    List<Task> adHocTasks = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .orderByTaskName()
        .asc()
        .list();

    assertEquals(4, adHocTasks.size());

    long taskACount = adHocTasks.stream().filter(t -> "taskA".equals(t.getTaskDefinitionKey())).count();
    long taskBCount = adHocTasks.stream().filter(t -> "taskB".equals(t.getTaskDefinitionKey())).count();
    assertEquals(2, taskACount);
    assertEquals(2, taskBCount);

    for (Task task : adHocTasks) {
      taskService.complete(task.getId());
    }

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertNotNull(taskAfter);
  }

  @Deployment
  @Test
  public void testMultiInstanceSequentialAdHocSubProcessStartsOneInstanceAtATime() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessMultiInstanceSequential");

    List<Task> firstInstanceTasks = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .orderByTaskName()
        .asc()
        .list();

    assertEquals(2, firstInstanceTasks.size());

    long firstInstanceTaskACount = firstInstanceTasks.stream().filter(t -> "taskA".equals(t.getTaskDefinitionKey())).count();
    long firstInstanceTaskBCount = firstInstanceTasks.stream().filter(t -> "taskB".equals(t.getTaskDefinitionKey())).count();
    assertEquals(1, firstInstanceTaskACount);
    assertEquals(1, firstInstanceTaskBCount);

    for (Task task : firstInstanceTasks) {
      taskService.complete(task.getId());
    }

    List<Task> secondInstanceTasks = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .orderByTaskName()
        .asc()
        .list();

    assertEquals(2, secondInstanceTasks.size());

    long secondInstanceTaskACount = secondInstanceTasks.stream().filter(t -> "taskA".equals(t.getTaskDefinitionKey())).count();
    long secondInstanceTaskBCount = secondInstanceTasks.stream().filter(t -> "taskB".equals(t.getTaskDefinitionKey())).count();
    assertEquals(1, secondInstanceTaskACount);
    assertEquals(1, secondInstanceTaskBCount);

    for (Task task : secondInstanceTasks) {
      taskService.complete(task.getId());
    }

    Task taskAfter = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult();

    assertNotNull(taskAfter);
  }

    @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testAutoCompleteAttributeFalseKeepsAdHocSubProcessOpen.bpmn20.xml")
    @Test
    public void testAutoCompleteAttributeFalseKeepsAdHocSubProcessOpen() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessAutoCompleteAttributeFalse");

    Task taskA = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskA")
      .singleResult();

    Task taskB = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskB")
      .singleResult();

    assertNotNull(taskA);
    assertNotNull(taskB);

    taskService.complete(taskA.getId());
    taskService.complete(taskB.getId());

    Execution adHocExecution = runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .activityId("adHocSubProcess")
      .singleResult();

    assertNotNull(adHocExecution);
    assertNull(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult());

    runtimeService.completeAdHocSubProcess(adHocExecution.getId());

    assertNotNull(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult());
    }

    @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testOperatonNamespaceAutoCompleteAttributeFalseKeepsAdHocSubProcessOpen.bpmn20.xml")
    @Test
    public void testOperatonNamespaceAutoCompleteAttributeFalseKeepsAdHocSubProcessOpen() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessAutoCompleteAttributeFalseOperatonNamespace");

    Task taskA = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskA")
      .singleResult();

    assertNotNull(taskA);

    taskService.complete(taskA.getId());

    Execution adHocExecution = runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .activityId("adHocSubProcess")
      .singleResult();

    assertNotNull(adHocExecution);
    assertNull(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult());

    runtimeService.completeAdHocSubProcess(adHocExecution.getId());

    assertNotNull(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult());
    }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testAutoCompleteAttributeTrueAutoCompletesAdHocSubProcess.bpmn20.xml")
  @Test
  public void testAutoCompleteAttributeTrueAutoCompletesAdHocSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessAutoCompleteAttributeTrue");

    Task taskA = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskA")
      .singleResult();

    Task taskB = taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskB")
      .singleResult();

    assertNotNull(taskA);
    assertNotNull(taskB);

    taskService.complete(taskA.getId());
    taskService.complete(taskB.getId());

    assertNull(runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .activityId("adHocSubProcess")
      .singleResult());

    assertNotNull(taskService.createTaskQuery()
      .processInstanceId(processInstance.getId())
      .taskDefinitionKey("taskAfter")
      .singleResult());
  }

  @Test
    public void testAutoCompletePropertyFailsParse() {
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
  public void testInvalidAutoCompleteAttributeFailsParse() {
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
  public void testSequentialOrderingRejectsMultipleConfiguredInitialActivities() {
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
    assertNull(runtimeService.getVariableLocal(adHocExecutionId, "adHocActivityStarted"));
    assertEquals(0, runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(processInstanceId)
        .variableName("adHocActivityStarted")
        .count());
  }
}
