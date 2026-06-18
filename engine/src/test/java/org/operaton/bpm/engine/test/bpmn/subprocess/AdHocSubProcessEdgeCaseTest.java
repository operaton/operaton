/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.runtime.AdHocActivity;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;

public class AdHocSubProcessEdgeCaseTest extends PluggableProcessEngineTest {

  private static final String IDLE_MODEL =
      "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.modelIdleNoInitialTasks.bpmn20.xml";
  private static final String NON_AD_HOC_MODEL =
      "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testAdHocCommandsFailForNonAdHocExecution.bpmn20.xml";
  private static final String THREE_TASK_MODEL =
      "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testTriggerMultipleAdHocActivitiesWithActivityVariables.bpmn20.xml";
  private static final String CANCEL_REMAINING_MODEL =
      "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testCompleteAdHocSubProcessCancelsActivitiesWhenCancelRemainingInstancesTrue.bpmn20.xml";
  private static final String NO_CANCEL_REMAINING_MODEL =
      "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testCompleteAdHocSubProcessFailsWhenActivitiesAreActiveAndCancelRemainingInstancesFalse.bpmn20.xml";
  private static final String EMBEDDED_SUB_PROCESS_MODEL =
      "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testTriggerEmbeddedSubProcessAdHocActivity.bpmn20.xml";
  private static final String CALL_ACTIVITY_MODEL =
      "org/operaton/bpm/engine/test/bpmn/subprocess/AdHocSubProcessTest.testTriggerCallActivityAdHocActivity.bpmn20.xml";

  @Deployment(resources = IDLE_MODEL)
  @Test
  public void testDiscoverStartableAdHocActivitiesFailsForUnknownExecution() {
    assertBadUserRequestContains(() -> runtimeService.getStartableAdHocActivities("doesNotExist"),
        "execution doesNotExist doesn't exist");
  }

  @Deployment(resources = NON_AD_HOC_MODEL)
  @Test
  public void testDiscoverStartableAdHocActivitiesFailsForNonAdHocExecution() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleUserTaskProcess");
    Execution execution = findExecution(processInstance, "userTask");

    assertBadUserRequestContains(() -> runtimeService.getStartableAdHocActivities(execution.getId()),
        "is not waiting in an adHocSubProcess");
  }

  @Deployment(resources = IDLE_MODEL)
  @Test
  public void testTriggerAdHocActivitiesFailsForUnknownExecution() {
    assertBadUserRequestContains(
        () -> runtimeService.triggerAdHocActivities("doesNotExist", Collections.singletonList("taskA"), null),
        "execution doesNotExist doesn't exist");
  }

  @Deployment(resources = IDLE_MODEL)
  @Test
  public void testCompleteAdHocSubProcessFailsForUnknownExecution() {
    assertBadUserRequestContains(() -> runtimeService.completeAdHocSubProcess("doesNotExist"),
        "execution doesNotExist doesn't exist");
  }

  @Deployment(resources = IDLE_MODEL)
  @Test
  public void testEmptyTriggerRequestIsRejectedWithoutStartingActivities() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");
    Execution adHocExecution = findAdHocExecution(processInstance);

    assertBadUserRequestContains(() -> runtimeService.triggerAdHocActivities(adHocExecution.getId(),
        Collections.emptyList(), null), "activityIds is empty");

    assertNoTask(processInstance, "taskA");
    assertNoTask(processInstance, "taskB");
    assertNotNull(findAdHocExecution(processInstance));
  }

  @Deployment(resources = IDLE_MODEL)
  @Test
  public void testNullActivityIdIsRejectedWithoutStartingEarlierActivities() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");
    Execution adHocExecution = findAdHocExecution(processInstance);

    assertBadUserRequestContains(() -> runtimeService.triggerAdHocActivities(adHocExecution.getId(),
        Arrays.asList("taskA", null), null), "activityId is null");

    assertNoTask(processInstance, "taskA");
    assertNoTask(processInstance, "taskB");
    assertNotNull(findAdHocExecution(processInstance));
  }

  @Deployment(resources = IDLE_MODEL)
  @Test
  public void testDuplicateActivityIdsAreRejectedWithoutStartingActivities() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");
    Execution adHocExecution = findAdHocExecution(processInstance);

    assertBadUserRequestContains(() -> runtimeService.triggerAdHocActivities(adHocExecution.getId(),
        Arrays.asList("taskA", "taskA"), null), "duplicate adHoc activity 'taskA' in request");

    assertNoTask(processInstance, "taskA");
    assertNoTask(processInstance, "taskB");
    assertNotNull(findAdHocExecution(processInstance));
  }

  @Deployment(resources = THREE_TASK_MODEL)
  @Test
  public void testVariablesForNonRequestedActivityAreRejectedWithoutStartingTargets() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithThreeTasks");
    Execution adHocExecution = findAdHocExecution(processInstance);

    Map<String, Object> taskBVariables = new HashMap<>();
    taskBVariables.put("assigneeHint", "mary");
    Map<String, Map<String, Object>> activityVariables = new HashMap<>();
    activityVariables.put("taskB", taskBVariables);

    assertBadUserRequestContains(() -> runtimeService.triggerAdHocActivities(adHocExecution.getId(),
        Collections.singletonList("taskC"), activityVariables),
        "variables provided for non-requested adHoc activity 'taskB'");

    assertTaskExists(processInstance, "taskA");
    assertNoTask(processInstance, "taskB");
    assertNoTask(processInstance, "taskC");
  }

  @Deployment(resources = NO_CANCEL_REMAINING_MODEL)
  @Test
  public void testRejectedManualCompleteDoesNotPersistVariables() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessNoCancelRemaining");
    Execution adHocExecution = findAdHocExecution(processInstance);

    assertBadUserRequestContains(() -> runtimeService.completeAdHocSubProcess(adHocExecution.getId(),
        Collections.singletonMap("completionReason", "shouldNotBeStored")),
        "has active child activities and cannot be completed");

    assertNull(runtimeService.getVariable(processInstance.getId(), "completionReason"));
    assertTaskExists(processInstance, "taskA");
    assertTaskExists(processInstance, "taskB");
    assertNotNull(findAdHocExecution(processInstance));
  }

  @Deployment(resources = CANCEL_REMAINING_MODEL)
  @Test
  public void testManualCompleteWithVariablesCancelsActiveChildren() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessBasic");
    Execution adHocExecution = findAdHocExecution(processInstance);

    runtimeService.completeAdHocSubProcess(adHocExecution.getId(),
        Collections.singletonMap("completionReason", "operatorCancelled"));

    assertNoTask(processInstance, "taskA");
    assertNoTask(processInstance, "taskB");
    assertTaskExists(processInstance, "taskAfter");
    assertEquals("operatorCancelled", runtimeService.getVariable(processInstance.getId(), "completionReason"));
  }

  @Deployment
  @Test
  public void testManualCompleteCancelsAsyncBeforeActivity() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessAsyncManualComplete");
    Execution adHocExecution = findAdHocExecution(processInstance);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskA"), null);

    Job asyncBeforeJob = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertNotNull(asyncBeforeJob);

    runtimeService.completeAdHocSubProcess(adHocExecution.getId(),
        Collections.singletonMap("completionReason", "cancelAsync"));

    assertEquals(0L, managementService.createJobQuery().processInstanceId(processInstance.getId()).count());
    assertNoTask(processInstance, "taskA");
    assertTaskExists(processInstance, "taskAfter");
    assertEquals("cancelAsync", runtimeService.getVariable(processInstance.getId(), "completionReason"));
  }

  @Deployment(resources = EMBEDDED_SUB_PROCESS_MODEL)
  @Test
  public void testManualCompleteCancelsEmbeddedSubProcessActivity() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithEmbeddedSubProcess");
    Execution adHocExecution = findAdHocExecution(processInstance);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("embeddedSubProcess"), null);
    assertTaskExists(processInstance, "embeddedTask");

    runtimeService.completeAdHocSubProcess(adHocExecution.getId());

    assertNoTask(processInstance, "embeddedTask");
    assertTaskExists(processInstance, "taskAfter");
  }

  @Deployment(resources = CALL_ACTIVITY_MODEL)
  @Test
  public void testManualCompleteCancelsCallActivityProcessInstance() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithCallActivity");
    Execution adHocExecution = findAdHocExecution(processInstance);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("callActivity"), null);

    ProcessInstance calledProcessInstance = runtimeService.createProcessInstanceQuery()
        .superProcessInstanceId(processInstance.getId())
        .singleResult();

    assertNotNull(calledProcessInstance);
    assertTaskExists(calledProcessInstance, "calledTask");

    runtimeService.completeAdHocSubProcess(adHocExecution.getId());

    assertEquals(0L, runtimeService.createProcessInstanceQuery()
        .processInstanceId(calledProcessInstance.getId())
        .count());
    assertNoTask(calledProcessInstance, "calledTask");
    assertTaskExists(processInstance, "taskAfter");
  }

  @Deployment(resources = EMBEDDED_SUB_PROCESS_MODEL)
  @Test
  public void testParallelOrderingAllowsAdditionalTriggerWhileEmbeddedSubProcessIsActive() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithEmbeddedSubProcess");
    Execution adHocExecution = findAdHocExecution(processInstance);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("embeddedSubProcess"), null);
    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskB"), null);

    Task embeddedTask = assertTaskExists(processInstance, "embeddedTask");
    Task taskB = assertTaskExists(processInstance, "taskB");

    taskService.complete(taskB.getId());
    assertNull(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey("taskAfter")
        .singleResult());

    taskService.complete(embeddedTask.getId());
    assertTaskExists(processInstance, "taskAfter");
  }

  @Deployment
  @Test
  public void testSequentialOrderingBlocksWhileEmbeddedSubProcessIsActive() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessSequentialEmbedded");
    Execution adHocExecution = findAdHocExecution(processInstance);

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("embeddedSubProcess"), null);

    assertEquals(0, runtimeService.getStartableAdHocActivities(adHocExecution.getId()).size());
    String adHocExecutionId = adHocExecution.getId();
    assertBadUserRequestContains(() -> runtimeService.triggerAdHocActivities(adHocExecutionId,
        Collections.singletonList("taskB"), null), "already has an active child activity");

    Task embeddedTask = assertTaskExists(processInstance, "embeddedTask");
    taskService.complete(embeddedTask.getId());

    adHocExecution = findAdHocExecution(processInstance);
    assertStartableActivityIds(runtimeService.getStartableAdHocActivities(adHocExecution.getId()),
        "embeddedSubProcess", "taskB");

    runtimeService.triggerAdHocActivities(adHocExecution.getId(), Collections.singletonList("taskB"), null);
    Task taskB = assertTaskExists(processInstance, "taskB");
    taskService.complete(taskB.getId());

    runtimeService.completeAdHocSubProcess(findAdHocExecution(processInstance).getId());
    assertTaskExists(processInstance, "taskAfter");
  }

  @Deployment
  @Test
  public void testCompletionConditionCountsEmbeddedSubProcessCompletion() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("adHocSubProcessWithEmbeddedCompletion");

    Task embeddedTask = assertTaskExists(processInstance, "embeddedTask");
    assertTaskExists(processInstance, "taskB");

    taskService.complete(embeddedTask.getId());

    assertNoTask(processInstance, "taskB");
    assertTaskExists(processInstance, "taskAfter");
  }

  protected Execution findAdHocExecution(ProcessInstance processInstance) {
    return findExecution(processInstance, "adHocSubProcess");
  }

  protected Execution findExecution(ProcessInstance processInstance, String activityId) {
    Execution execution = runtimeService.createExecutionQuery()
        .processInstanceId(processInstance.getId())
        .activityId(activityId)
        .singleResult();

    assertNotNull(execution);
    return execution;
  }

  protected Task assertTaskExists(ProcessInstance processInstance, String taskDefinitionKey) {
    Task task = taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey(taskDefinitionKey)
        .singleResult();

    assertNotNull(task);
    return task;
  }

  protected void assertNoTask(ProcessInstance processInstance, String taskDefinitionKey) {
    assertNull(taskService.createTaskQuery()
        .processInstanceId(processInstance.getId())
        .taskDefinitionKey(taskDefinitionKey)
        .singleResult());
  }

  protected void assertStartableActivityIds(List<AdHocActivity> activities, String... expectedActivityIds) {
    assertEquals(expectedActivityIds.length, activities.size());
    for (String expectedActivityId : expectedActivityIds) {
      boolean found = activities.stream()
          .anyMatch(activity -> expectedActivityId.equals(activity.getActivityId()));
      if (!found) {
        fail("Expected startable ad-hoc activity " + expectedActivityId);
      }
    }
  }

  protected void assertBadUserRequestContains(AdHocCommand command, String expectedMessagePart) {
    try {
      command.execute();
      fail("Expected BadUserRequestException");
    } catch (BadUserRequestException e) {
      testRule.assertTextPresent(expectedMessagePart, e.getMessage());
    }
  }

  protected interface AdHocCommand {
    void execute();
  }
}
