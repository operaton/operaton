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
package org.operaton.bpm.engine.test.history;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Nico Rehwaldt
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
class HistoricActivityInstanceStateTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  HistoryService historyService;

  @ParameterizedTest
  @CsvSource({
      "org/operaton/bpm/engine/test/history/HistoricActivityInstanceStateTest.testSingleEndEvent.bpmn, start, 1",
      "org/operaton/bpm/engine/test/history/HistoricActivityInstanceStateTest.testSingleEndActivity.bpmn, start, 1",
      "org/operaton/bpm/engine/test/history/HistoricActivityInstanceStateTest.testSingleEndEventAfterParallelJoin.bpmn, parallelJoin, 2",
      "org/operaton/bpm/engine/test/history/HistoricActivityInstanceStateTest.testIntermediateTask.bpmn, intermediateTask, 1"
  })
  void processShouldHaveExpectedHistoricActivityInstances (String bpmnResource, String activityId, int expectedNonCompletingActivityInstanceCount) {
    // given
    testRule.deploy(bpmnResource);

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    testRule.assertProcessEnded(processInstance.getId());

    // then
    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertNonCompletingActivityInstance(allInstances, activityId, expectedNonCompletingActivityInstanceCount);
    assertNonCanceledActivityInstance(allInstances, activityId);

    assertIsCompletingActivityInstances(allInstances, "end", 1);
    assertNonCanceledActivityInstance(allInstances, "end");
  }

  @Deployment
  @Test
  void testEndParallelJoin() {
    startProcess();

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertNonCompletingActivityInstance(allInstances, "task1", 1);
    assertNonCanceledActivityInstance(allInstances, "task1");

    assertNonCompletingActivityInstance(allInstances, "task2", 1);
    assertNonCanceledActivityInstance(allInstances, "task2");

    assertIsCompletingActivityInstances(allInstances, "parallelJoinEnd", 2);
    assertNonCanceledActivityInstance(allInstances, "parallelJoinEnd");
  }

  @Deployment
  @Test
  void testTwoEndEvents() {
    startProcess();

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertNonCompletingActivityInstance(allInstances, "parallelSplit", 1);
    assertNonCanceledActivityInstance(allInstances, "parallelSplit", 1);

    assertIsCompletingActivityInstances(allInstances, "end1", 1);
    assertNonCanceledActivityInstance(allInstances, "end1");

    assertIsCompletingActivityInstances(allInstances, "end2", 1);
    assertNonCanceledActivityInstance(allInstances, "end2");
  }

  @Deployment
  @Test
  void testTwoEndActivities() {
    startProcess();

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertNonCompletingActivityInstance(allInstances, "parallelSplit", 1);
    assertNonCanceledActivityInstance(allInstances, "parallelSplit");

    assertIsCompletingActivityInstances(allInstances, "end1", 1);
    assertNonCanceledActivityInstance(allInstances, "end1");

    assertIsCompletingActivityInstances(allInstances, "end2", 1);
    assertNonCanceledActivityInstance(allInstances, "end2");
  }

  @Deployment
  @Test
  void testSingleEndEventAndSingleEndActivity() {
    startProcess();

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertNonCompletingActivityInstance(allInstances, "parallelSplit", 1);
    assertNonCanceledActivityInstance(allInstances, "parallelSplit");

    assertIsCompletingActivityInstances(allInstances, "end1");
    assertNonCanceledActivityInstance(allInstances, "end1");

    assertIsCompletingActivityInstances(allInstances, "end2");
    assertNonCanceledActivityInstance(allInstances, "end2");
  }

  @Deployment
  @Test
  void testSimpleSubProcess() {
    startProcess();

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertNonCompletingActivityInstance(allInstances, "intermediateSubprocess", 1);
    assertNonCanceledActivityInstance(allInstances, "intermediateSubprocess");

    assertIsCompletingActivityInstances(allInstances, "subprocessEnd", 1);
    assertNonCanceledActivityInstance(allInstances, "subprocessEnd");

    assertIsCompletingActivityInstances(allInstances, "end", 1);
    assertNonCanceledActivityInstance(allInstances, "end");
  }

  @Deployment
  @Test
  void testParallelMultiInstanceSubProcess() {
    startProcess();

    List<HistoricActivityInstance> activityInstances = getEndActivityInstances();

    assertThat(activityInstances).hasSize(7);

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertIsCompletingActivityInstances(allInstances, "intermediateSubprocess", 3);
    assertNonCanceledActivityInstance(allInstances, "intermediateSubprocess");

    assertIsCompletingActivityInstances(allInstances, "subprocessEnd", 3);
    assertNonCanceledActivityInstance(allInstances, "subprocessEnd");

    assertNonCompletingActivityInstance(allInstances, "intermediateSubprocess#multiInstanceBody", 1);
    assertNonCanceledActivityInstance(allInstances, "intermediateSubprocess#multiInstanceBody");

    assertIsCompletingActivityInstances(allInstances, "end", 1);
    assertNonCanceledActivityInstance(allInstances, "end");
  }

  @Deployment
  @Test
  void testSequentialMultiInstanceSubProcess() {
    startProcess();

    List<HistoricActivityInstance> activityInstances = getEndActivityInstances();

    assertThat(activityInstances).hasSize(7);

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertIsCompletingActivityInstances(allInstances, "intermediateSubprocess", 3);
    assertNonCanceledActivityInstance(allInstances, "intermediateSubprocess");

    assertIsCompletingActivityInstances(allInstances, "subprocessEnd", 3);
    assertNonCanceledActivityInstance(allInstances, "subprocessEnd");

    assertNonCompletingActivityInstance(allInstances, "intermediateSubprocess#multiInstanceBody", 1);
    assertNonCanceledActivityInstance(allInstances, "intermediateSubprocess#multiInstanceBody");

    assertIsCompletingActivityInstances(allInstances, "end", 1);
    assertNonCanceledActivityInstance(allInstances, "end");
  }

  @Deployment
  @Test
  void testBoundaryErrorCancel() {
    ProcessInstance processInstance = startProcess();
    runtimeService.correlateMessage("continue");
    testRule.assertProcessEnded(processInstance.getId());


    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertNonCanceledActivityInstance(allInstances, "start");
    assertNonCompletingActivityInstance(allInstances, "start");

    assertNonCanceledActivityInstance(allInstances, "subprocessStart");
    assertNonCompletingActivityInstance(allInstances, "subprocessStart");

    assertNonCanceledActivityInstance(allInstances, "gtw");
    assertNonCompletingActivityInstance(allInstances, "gtw");

    assertIsCanceledActivityInstances(allInstances, "subprocess", 1);
    assertNonCompletingActivityInstance(allInstances, "subprocess");

    assertIsCanceledActivityInstances(allInstances, "errorSubprocessEnd", 1);
    assertNonCompletingActivityInstance(allInstances, "errorSubprocessEnd");

    assertIsCanceledActivityInstances(allInstances, "userTask", 1);
    assertNonCompletingActivityInstance(allInstances, "userTask");

    assertNonCanceledActivityInstance(allInstances, "subprocessBoundary");
    assertNonCompletingActivityInstance(allInstances, "subprocessBoundary");

    assertNonCanceledActivityInstance(allInstances, "endAfterBoundary");
    assertIsCompletingActivityInstances(allInstances, "endAfterBoundary", 1);
  }

  @Deployment
  @Test
  void testBoundarySignalCancel() {
    ProcessInstance processInstance = startProcess();

    // should wait in user task
    assertThat(processInstance.isEnded()).isFalse();

    // signal sub process
    runtimeService.signalEventReceived("interrupt");

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertNonCompletingActivityInstance(allInstances, "subprocess");
    assertIsCanceledActivityInstances(allInstances, "subprocess", 1);

    assertIsCanceledActivityInstances(allInstances, "userTask", 1);
    assertNonCompletingActivityInstance(allInstances, "userTask");

    assertNonCanceledActivityInstance(allInstances, "subprocessBoundary");
    assertNonCompletingActivityInstance(allInstances, "subprocessBoundary");

    assertNonCanceledActivityInstance(allInstances, "endAfterBoundary");
    assertIsCompletingActivityInstances(allInstances, "endAfterBoundary", 1);
  }

  @Deployment
  @Test
  void testEventSubprocessErrorCancel() {
    ProcessInstance processInstance = startProcess();
    runtimeService.correlateMessage("continue");
    testRule.assertProcessEnded(processInstance.getId());

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertIsCanceledActivityInstances(allInstances, "userTask", 1);
    assertNonCompletingActivityInstance(allInstances, "userTask");

    assertIsCanceledActivityInstances(allInstances, "errorEnd", 1);
    assertNonCompletingActivityInstance(allInstances, "errorEnd");

    assertNonCanceledActivityInstance(allInstances, "eventSubprocessStart");
    assertNonCompletingActivityInstance(allInstances, "eventSubprocessStart");

    assertNonCanceledActivityInstance(allInstances, "eventSubprocessEnd");
    assertIsCompletingActivityInstances(allInstances, "eventSubprocessEnd", 1);
  }

  @Deployment
  @Test
  void testEventSubprocessMessageCancel() {
    startProcess();

    runtimeService.correlateMessage("message");

    assertThat(runtimeService.createProcessInstanceQuery().singleResult()).isNull();

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertIsCanceledActivityInstances(allInstances, "userTask", 1);
    assertNonCompletingActivityInstance(allInstances, "userTask");

    assertNonCanceledActivityInstance(allInstances, "eventSubprocessStart");
    assertNonCompletingActivityInstance(allInstances, "eventSubprocessStart");

    assertNonCanceledActivityInstance(allInstances, "eventSubprocessEnd");
    assertIsCompletingActivityInstances(allInstances, "eventSubprocessEnd", 1);
  }

  @Deployment
  @Test
  void testEventSubprocessSignalCancel() {
    ProcessInstance processInstance = startProcess();
    runtimeService.correlateMessage("continue");
    testRule.assertProcessEnded(processInstance.getId());

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertIsCanceledActivityInstances(allInstances, "userTask", 1);
    assertNonCompletingActivityInstance(allInstances, "userTask");

    // fails due to CAM-4527: end execution listeners are executed twice for the signal end event
    assertIsCanceledActivityInstances(allInstances, "signalEnd", 1);
    assertNonCompletingActivityInstance(allInstances, "signalEnd");

    assertNonCanceledActivityInstance(allInstances, "eventSubprocessStart");
    assertNonCompletingActivityInstance(allInstances, "eventSubprocessStart");

    assertNonCanceledActivityInstance(allInstances, "eventSubprocessEnd");
    assertIsCompletingActivityInstances(allInstances, "eventSubprocessEnd", 1);
  }

  @Deployment
  @Test
  void testEndTerminateEventCancel() {
    ProcessInstance processInstance = startProcess();
    runtimeService.correlateMessage("continue");
    testRule.assertProcessEnded(processInstance.getId());

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertIsCanceledActivityInstances(allInstances, "userTask", 1);
    assertNonCompletingActivityInstance(allInstances, "userTask");

    assertNonCanceledActivityInstance(allInstances, "terminateEnd");
    assertIsCompletingActivityInstances(allInstances, "terminateEnd", 1);

  }

  @Deployment
  @Test
  void testEndTerminateEventCancelInSubprocess() {
    ProcessInstance processInstance = startProcess();
    runtimeService.correlateMessage("continue");
    testRule.assertProcessEnded(processInstance.getId());

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertNonCompletingActivityInstance(allInstances, "subprocess");
    assertNonCanceledActivityInstance(allInstances, "subprocess");

    assertIsCanceledActivityInstances(allInstances, "userTask", 1);
    assertNonCompletingActivityInstance(allInstances, "userTask");

    assertNonCanceledActivityInstance(allInstances, "terminateEnd");
    assertIsCompletingActivityInstances(allInstances, "terminateEnd", 1);

    assertIsCompletingActivityInstances(allInstances, "end", 1);
    assertNonCanceledActivityInstance(allInstances, "end");

  }

  @Deployment
  @Test
  void testEndTerminateEventCancelWithSubprocess() {
    ProcessInstance processInstance = startProcess();
    runtimeService.correlateMessage("continue");
    testRule.assertProcessEnded(processInstance.getId());

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertIsCanceledActivityInstances(allInstances, "subprocess", 1);
    assertNonCompletingActivityInstance(allInstances, "subprocess");

    assertIsCanceledActivityInstances(allInstances, "userTask", 1);
    assertNonCompletingActivityInstance(allInstances, "userTask");

    assertNonCanceledActivityInstance(allInstances, "terminateEnd");
    assertIsCompletingActivityInstances(allInstances, "terminateEnd", 1);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/HistoricActivityInstanceStateTest.testCancelProcessInstanceInUserTask.bpmn",
      "org/operaton/bpm/engine/test/history/HistoricActivityInstanceStateTest.testEndTerminateEventWithCallActivity.bpmn"})
  @Test
  void testEndTerminateEventCancelWithCallActivity() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process1");
    runtimeService.correlateMessage("continue");
    testRule.assertProcessEnded(processInstance.getId());

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertIsCanceledActivityInstances(allInstances, "callActivity", 1);
    assertNonCompletingActivityInstance(allInstances, "callActivity");

    assertIsCanceledActivityInstances(allInstances, "userTask", 1);
    assertNonCompletingActivityInstance(allInstances, "userTask");

    assertNonCanceledActivityInstance(allInstances, "terminateEnd");
    assertIsCompletingActivityInstances(allInstances, "terminateEnd", 1);

  }

  @Deployment
  @Test
  void testCancelProcessInstanceInUserTask() {
    ProcessInstance processInstance = startProcess();

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertIsCanceledActivityInstances(allInstances, "userTask", 1);
    assertNonCompletingActivityInstance(allInstances, "userTask");

  }

  @Deployment
  @Test
  void testCancelProcessInstanceInSubprocess() {
    ProcessInstance processInstance = startProcess();

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertIsCanceledActivityInstances(allInstances, "userTask", 1);
    assertNonCompletingActivityInstance(allInstances, "userTask");

    assertIsCanceledActivityInstances(allInstances, "subprocess", 1);
    assertNonCompletingActivityInstance(allInstances, "subprocess");

  }

  @Deployment
  @Test
  void testCancelProcessWithParallelGateway() {
    ProcessInstance processInstance = startProcess();

    runtimeService.deleteProcessInstance(processInstance.getId(), "test");

    List<HistoricActivityInstance> allInstances = getAllActivityInstances();

    assertIsCanceledActivityInstances(allInstances, "userTask1", 1);
    assertNonCompletingActivityInstance(allInstances, "userTask1");

    assertIsCanceledActivityInstances(allInstances, "userTask2", 1);
    assertNonCompletingActivityInstance(allInstances, "userTask2");

    assertIsCanceledActivityInstances(allInstances, "subprocess", 1);
    assertNonCompletingActivityInstance(allInstances, "subprocess");

  }

  private void assertIsCanceledActivityInstances(List<HistoricActivityInstance> allInstances, String activityId, int count) {
    assertCorrectCanceledState(allInstances, activityId, count, true);
  }

  private void assertNonCanceledActivityInstance(List<HistoricActivityInstance> instances, String activityId) {
    assertNonCanceledActivityInstance(instances, activityId, -1);
  }

  private void assertNonCanceledActivityInstance(List<HistoricActivityInstance> instances, String activityId, int count) {
    assertCorrectCanceledState(instances, activityId, count, false);
  }

  private void assertCorrectCanceledState(List<HistoricActivityInstance> allInstances, String activityId, int expectedCount, boolean canceled) {
    int found = 0;

    for (HistoricActivityInstance instance : allInstances) {
      if (instance.getActivityId().equals(activityId)) {
        found++;
        assertThat(instance.isCanceled()).as("expect <%s> to be %scanceled".formatted(activityId, canceled ? "" : "non-")).isEqualTo(canceled);
      }
    }

    assertThat(found).as("contains entry for activity <" + activityId + ">").isPositive();

    if (expectedCount != -1) {
      assertThat(expectedCount).as("contains <" + expectedCount + "> entries for activity <" + activityId + ">").isEqualTo(found);
    }
  }

  private void assertIsCompletingActivityInstances(List<HistoricActivityInstance> allInstances, String activityId) {
    assertIsCompletingActivityInstances(allInstances, activityId, -1);
  }

  private void assertIsCompletingActivityInstances(List<HistoricActivityInstance> allInstances, String activityId, int count) {
    assertCorrectCompletingState(allInstances, activityId, count, true);
  }

  private void assertNonCompletingActivityInstance(List<HistoricActivityInstance> instances, String activityId) {
    assertNonCompletingActivityInstance(instances, activityId, -1);
  }

  private void assertNonCompletingActivityInstance(List<HistoricActivityInstance> instances, String activityId, int count) {
    assertCorrectCompletingState(instances, activityId, count, false);
  }

  private void assertCorrectCompletingState(List<HistoricActivityInstance> allInstances, String activityId, int expectedCount, boolean completing) {
    int found = 0;

    for (HistoricActivityInstance instance : allInstances) {
      if (instance.getActivityId().equals(activityId)) {
        found++;
        assertThat(instance.isCompleteScope()).as("expect <%s> to be %scompleting".formatted(activityId, completing ? "" : "non-")).isEqualTo(completing);
      }
    }

    assertThat(found).as("contains entry for activity <" + activityId + ">").isPositive();

    if (expectedCount != -1) {
      assertThat(expectedCount).as("contains <" + expectedCount + "> entries for activity <" + activityId + ">").isEqualTo(found);
    }
  }

  private List<HistoricActivityInstance> getEndActivityInstances() {
    return historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceEndTime().asc().completeScope().list();
  }

  private List<HistoricActivityInstance> getAllActivityInstances() {
    return historyService.createHistoricActivityInstanceQuery().orderByHistoricActivityInstanceStartTime().asc().list();
  }

  private ProcessInstance startProcess() {
    return runtimeService.startProcessInstanceByKey("process");
  }
}
