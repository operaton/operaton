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
package org.operaton.bpm.engine.test.standalone.entity;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.standalone.entity.ExecutionOrderListener.ActivitySequenceCounterMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
class ExecutionSequenceCounterTest {

  @RegisterExtension
  static ProcessEngineExtension processEngineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(processEngineRule);
  
  RuntimeService runtimeService;
  ManagementService managementService;
  TaskService taskService;

  @BeforeEach
  void setUp() {
    ExecutionOrderListener.clearActivityExecutionOrder();
  }

  @Deployment
  @Test
  void testSequence() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when

    // then
    testRule.assertProcessEnded(processInstanceId);

    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    verifyOrder(order, "theStart", "theService1", "theService2", "theEnd");
  }

  @Deployment
  @Test
  void testForkSameSequenceLengthWithoutWaitStates() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when

    // then
    testRule.assertProcessEnded(processInstanceId);

    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    verifyOrder(order, "theStart", "theService", "fork", "theService1", "theEnd1", "theService2", "theEnd2");
  }

  @Deployment
  @Test
  void testForkSameSequenceLengthWithAsyncEndEvent() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    JobQuery jobQuery = managementService.createJobQuery();

    // when (1)

    // then (1)
    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(5);

    long lastSequenceCounter = 0;

    ActivitySequenceCounterMap theStartElement = order.get(0);
    assertThat(theStartElement.getActivityId()).isEqualTo("theStart");
    assertThat(theStartElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theStartElement.getSequenceCounter();

    ActivitySequenceCounterMap theForkElement = order.get(1);
    assertThat(theForkElement.getActivityId()).isEqualTo("theService");
    assertThat(theForkElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theForkElement.getSequenceCounter();

    ActivitySequenceCounterMap theServiceElement = order.get(2);
    assertThat(theServiceElement.getActivityId()).isEqualTo("fork");
    assertThat(theServiceElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theServiceElement.getSequenceCounter();

    ActivitySequenceCounterMap theService1Element = order.get(3);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");
    assertThat(theService1Element.getSequenceCounter()).isGreaterThan(lastSequenceCounter);

    ActivitySequenceCounterMap theService2Element = order.get(4);
    assertThat(theService2Element.getActivityId()).isEqualTo("theService2");
    assertThat(theService2Element.getSequenceCounter()).isGreaterThan(lastSequenceCounter);

    // when (2)
    String jobId = jobQuery.activityId("theEnd1").singleResult().getId();
    managementService.executeJob(jobId);

    // then (2)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(6);

    ActivitySequenceCounterMap theEnd1Element = order.get(5);
    assertThat(theEnd1Element.getActivityId()).isEqualTo("theEnd1");
    assertThat(theEnd1Element.getSequenceCounter()).isGreaterThan(theService1Element.getSequenceCounter());

    // when (3)
    jobId = jobQuery.activityId("theEnd2").singleResult().getId();
    managementService.executeJob(jobId);

    // then (3)
    testRule.assertProcessEnded(processInstanceId);

    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(7);

    ActivitySequenceCounterMap theEnd2Element = order.get(6);
    assertThat(theEnd2Element.getActivityId()).isEqualTo("theEnd2");
    assertThat(theEnd2Element.getSequenceCounter()).isGreaterThan(theService2Element.getSequenceCounter());
  }

  @Deployment
  @Test
  void testForkDifferentSequenceLengthWithoutWaitStates() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when

    // then
    testRule.assertProcessEnded(processInstanceId);

    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    verifyOrder(order, "theStart", "theService", "fork", "theService1", "theEnd1", "theService2", "theService3", "theEnd2");

  }

  @Deployment
  @Test
  void testForkDifferentSequenceLengthWithAsyncEndEvent() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    JobQuery jobQuery = managementService.createJobQuery();

    // when (1)

    // then (1)
    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(6);

    long lastSequenceCounter = 0;

    ActivitySequenceCounterMap theStartElement = order.get(0);
    assertThat(theStartElement.getActivityId()).isEqualTo("theStart");
    assertThat(theStartElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theStartElement.getSequenceCounter();

    ActivitySequenceCounterMap theForkElement = order.get(1);
    assertThat(theForkElement.getActivityId()).isEqualTo("theService");
    assertThat(theForkElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theForkElement.getSequenceCounter();

    ActivitySequenceCounterMap theServiceElement = order.get(2);
    assertThat(theServiceElement.getActivityId()).isEqualTo("fork");
    assertThat(theServiceElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theServiceElement.getSequenceCounter();

    ActivitySequenceCounterMap theService1Element = order.get(3);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");
    assertThat(theService1Element.getSequenceCounter()).isGreaterThan(lastSequenceCounter);

    ActivitySequenceCounterMap theService2Element = order.get(4);
    assertThat(theService2Element.getActivityId()).isEqualTo("theService2");
    assertThat(theService2Element.getSequenceCounter()).isGreaterThan(lastSequenceCounter);

    ActivitySequenceCounterMap theService3Element = order.get(5);
    assertThat(theService3Element.getActivityId()).isEqualTo("theService3");
    assertThat(theService3Element.getSequenceCounter()).isGreaterThan(theService2Element.getSequenceCounter());

    // when (2)
    String jobId = jobQuery.activityId("theEnd1").singleResult().getId();
    managementService.executeJob(jobId);

    // then (2)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(7);

    ActivitySequenceCounterMap theEnd1Element = order.get(6);
    assertThat(theEnd1Element.getActivityId()).isEqualTo("theEnd1");
    assertThat(theEnd1Element.getSequenceCounter()).isGreaterThan(theService1Element.getSequenceCounter());

    // when (3)
    jobId = jobQuery.activityId("theEnd2").singleResult().getId();
    managementService.executeJob(jobId);

    // then (3)
    testRule.assertProcessEnded(processInstanceId);

    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(8);

    ActivitySequenceCounterMap theEnd2Element = order.get(7);
    assertThat(theEnd2Element.getActivityId()).isEqualTo("theEnd2");
    assertThat(theEnd2Element.getSequenceCounter()).isGreaterThan(theService3Element.getSequenceCounter());
  }

  @Deployment
  @Test
  void testForkReplaceBy() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    JobQuery jobQuery = managementService.createJobQuery();

    // when (1)

    // then (1)
    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(2);

    ActivitySequenceCounterMap theService1Element = order.get(0);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");

    ActivitySequenceCounterMap theService3Element = order.get(1);
    assertThat(theService3Element.getActivityId()).isEqualTo("theService3");

    assertThat(theService3Element.getSequenceCounter()).isEqualTo(theService1Element.getSequenceCounter());

    // when (2)
    String jobId = jobQuery.activityId("theService4").singleResult().getId();
    managementService.executeJob(jobId);

    // then (2)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(5);

    ActivitySequenceCounterMap theService4Element = order.get(2);
    assertThat(theService4Element.getActivityId()).isEqualTo("theService4");
    assertThat(theService4Element.getSequenceCounter()).isGreaterThan(theService3Element.getSequenceCounter());

    ActivitySequenceCounterMap theService5Element = order.get(3);
    assertThat(theService5Element.getActivityId()).isEqualTo("theService5");
    assertThat(theService5Element.getSequenceCounter()).isGreaterThan(theService4Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd2Element = order.get(4);
    assertThat(theEnd2Element.getActivityId()).isEqualTo("theEnd2");
    assertThat(theEnd2Element.getSequenceCounter()).isGreaterThan(theService5Element.getSequenceCounter());

    // when (3)
    jobId = jobQuery.activityId("theService2").singleResult().getId();
    managementService.executeJob(jobId);

    // then (3)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(7);

    ActivitySequenceCounterMap theService2Element = order.get(5);
    assertThat(theService2Element.getActivityId()).isEqualTo("theService2");
    assertThat(theService2Element.getSequenceCounter()).isGreaterThan(theService1Element.getSequenceCounter());
    assertThat(theService2Element.getSequenceCounter()).isGreaterThan(theEnd2Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd1Element = order.get(6);
    assertThat(theEnd1Element.getActivityId()).isEqualTo("theEnd1");
    assertThat(theEnd1Element.getSequenceCounter()).isGreaterThan(theService2Element.getSequenceCounter());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/standalone/entity/ExecutionSequenceCounterTest.testForkReplaceBy.bpmn20.xml"})
  @Test
  void testForkReplaceByAnotherExecutionOrder() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();
    JobQuery jobQuery = managementService.createJobQuery();

    // when (1)

    // then (1)
    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(2);

    ActivitySequenceCounterMap theService1Element = order.get(0);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");

    ActivitySequenceCounterMap theService3Element = order.get(1);
    assertThat(theService3Element.getActivityId()).isEqualTo("theService3");

    assertThat(theService3Element.getSequenceCounter()).isEqualTo(theService1Element.getSequenceCounter());

    // when (2)
    String jobId = jobQuery.activityId("theService2").singleResult().getId();
    managementService.executeJob(jobId);

    // then (2)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(4);

    ActivitySequenceCounterMap theService2Element = order.get(2);
    assertThat(theService2Element.getActivityId()).isEqualTo("theService2");
    assertThat(theService2Element.getSequenceCounter()).isGreaterThan(theService1Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd1Element = order.get(3);
    assertThat(theEnd1Element.getActivityId()).isEqualTo("theEnd1");
    assertThat(theEnd1Element.getSequenceCounter()).isGreaterThan(theService2Element.getSequenceCounter());

    // when (3)
    jobId = jobQuery.activityId("theService4").singleResult().getId();
    managementService.executeJob(jobId);

    // then (3)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(7);

    ActivitySequenceCounterMap theService4Element = order.get(4);
    assertThat(theService4Element.getActivityId()).isEqualTo("theService4");
    assertThat(theService4Element.getSequenceCounter()).isGreaterThan(theService3Element.getSequenceCounter());
    assertThat(theService4Element.getSequenceCounter()).isGreaterThan(theEnd1Element.getSequenceCounter());

    ActivitySequenceCounterMap theService5Element = order.get(5);
    assertThat(theService5Element.getActivityId()).isEqualTo("theService5");
    assertThat(theService5Element.getSequenceCounter()).isGreaterThan(theService4Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd2Element = order.get(6);
    assertThat(theEnd2Element.getActivityId()).isEqualTo("theEnd2");
    assertThat(theEnd2Element.getSequenceCounter()).isGreaterThan(theService5Element.getSequenceCounter());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testForkReplaceByThreeBranches() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();
    JobQuery jobQuery = managementService.createJobQuery();

    // when (1)

    // then (1)
    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(3);

    ActivitySequenceCounterMap theService1Element = order.get(0);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");

    ActivitySequenceCounterMap theService3Element = order.get(1);
    assertThat(theService3Element.getActivityId()).isEqualTo("theService3");

    ActivitySequenceCounterMap theService6Element = order.get(2);
    assertThat(theService6Element.getActivityId()).isEqualTo("theService6");

    assertThat(theService3Element.getSequenceCounter()).isEqualTo(theService1Element.getSequenceCounter());
    assertThat(theService6Element.getSequenceCounter()).isEqualTo(theService3Element.getSequenceCounter());

    // when (2)
    String jobId = jobQuery.activityId("theService2").singleResult().getId();
    managementService.executeJob(jobId);

    // then (2)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(5);

    ActivitySequenceCounterMap theService2Element = order.get(3);
    assertThat(theService2Element.getActivityId()).isEqualTo("theService2");
    assertThat(theService2Element.getSequenceCounter()).isGreaterThan(theService1Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd1Element = order.get(4);
    assertThat(theEnd1Element.getActivityId()).isEqualTo("theEnd1");
    assertThat(theEnd1Element.getSequenceCounter()).isGreaterThan(theService2Element.getSequenceCounter());

    // when (3)
    jobId = jobQuery.activityId("theService4").singleResult().getId();
    managementService.executeJob(jobId);

    // then (3)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(8);

    ActivitySequenceCounterMap theService4Element = order.get(5);
    assertThat(theService4Element.getActivityId()).isEqualTo("theService4");
    assertThat(theService4Element.getSequenceCounter()).isGreaterThan(theService3Element.getSequenceCounter());

    ActivitySequenceCounterMap theService5Element = order.get(6);
    assertThat(theService5Element.getActivityId()).isEqualTo("theService5");
    assertThat(theService5Element.getSequenceCounter()).isGreaterThan(theService4Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd2Element = order.get(7);
    assertThat(theEnd2Element.getActivityId()).isEqualTo("theEnd2");
    assertThat(theEnd2Element.getSequenceCounter()).isGreaterThan(theService5Element.getSequenceCounter());

    // when (4)
    jobId = jobQuery.activityId("theService7").singleResult().getId();
    managementService.executeJob(jobId);

    // then (4)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(12);

    ActivitySequenceCounterMap theService7Element = order.get(8);
    assertThat(theService7Element.getActivityId()).isEqualTo("theService7");
    assertThat(theService7Element.getSequenceCounter()).isGreaterThan(theService6Element.getSequenceCounter());
    assertThat(theService7Element.getSequenceCounter()).isGreaterThan(theEnd2Element.getSequenceCounter());

    ActivitySequenceCounterMap theService8Element = order.get(9);
    assertThat(theService8Element.getActivityId()).isEqualTo("theService8");
    assertThat(theService8Element.getSequenceCounter()).isGreaterThan(theService7Element.getSequenceCounter());

    ActivitySequenceCounterMap theService9Element = order.get(10);
    assertThat(theService9Element.getActivityId()).isEqualTo("theService9");
    assertThat(theService9Element.getSequenceCounter()).isGreaterThan(theService8Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd3Element = order.get(11);
    assertThat(theEnd3Element.getActivityId()).isEqualTo("theEnd3");
    assertThat(theEnd3Element.getSequenceCounter()).isGreaterThan(theService9Element.getSequenceCounter());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  void testForkAndJoinSameSequenceLength() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when

    // then
    testRule.assertProcessEnded(processInstanceId);

    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(9);

    long lastSequenceCounter = 0;

    ActivitySequenceCounterMap theStartElement = order.get(0);
    assertThat(theStartElement.getActivityId()).isEqualTo("theStart");
    assertThat(theStartElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theStartElement.getSequenceCounter();

    ActivitySequenceCounterMap theForkElement = order.get(1);
    assertThat(theForkElement.getActivityId()).isEqualTo("theService");
    assertThat(theForkElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theForkElement.getSequenceCounter();

    ActivitySequenceCounterMap theServiceElement = order.get(2);
    assertThat(theServiceElement.getActivityId()).isEqualTo("fork");
    assertThat(theServiceElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theServiceElement.getSequenceCounter();

    ActivitySequenceCounterMap theService1Element = order.get(3);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");
    assertThat(theService1Element.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theService1Element.getSequenceCounter();

    ActivitySequenceCounterMap theJoin1Element = order.get(4);
    assertThat(theJoin1Element.getActivityId()).isEqualTo("join");
    assertThat(theJoin1Element.getSequenceCounter()).isGreaterThan(lastSequenceCounter);

    lastSequenceCounter = theForkElement.getSequenceCounter();

    ActivitySequenceCounterMap theService2Element = order.get(5);
    assertThat(theService2Element.getActivityId()).isEqualTo("theService2");
    assertThat(theService2Element.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theService2Element.getSequenceCounter();

    ActivitySequenceCounterMap theJoin2Element = order.get(6);
    assertThat(theJoin2Element.getActivityId()).isEqualTo("join");
    assertThat(theJoin2Element.getSequenceCounter()).isGreaterThan(lastSequenceCounter);

    ActivitySequenceCounterMap theService3Element = order.get(7);
    assertThat(theService3Element.getActivityId()).isEqualTo("theService3");
    assertThat(theService3Element.getSequenceCounter()).isGreaterThan(theJoin1Element.getSequenceCounter());
    assertThat(theService3Element.getSequenceCounter()).isGreaterThan(theJoin2Element.getSequenceCounter());
    lastSequenceCounter = theService3Element.getSequenceCounter();

    ActivitySequenceCounterMap theEndElement = order.get(8);
    assertThat(theEndElement.getActivityId()).isEqualTo("theEnd");
    assertThat(theEndElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
  }

  @Deployment
  @Test
  void testForkAndJoinDifferentSequenceLength() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when

    // then
    testRule.assertProcessEnded(processInstanceId);

    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(10);

    long lastSequenceCounter = 0;

    ActivitySequenceCounterMap theStartElement = order.get(0);
    assertThat(theStartElement.getActivityId()).isEqualTo("theStart");
    assertThat(theStartElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theStartElement.getSequenceCounter();

    ActivitySequenceCounterMap theForkElement = order.get(1);
    assertThat(theForkElement.getActivityId()).isEqualTo("theService");
    assertThat(theForkElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theForkElement.getSequenceCounter();

    ActivitySequenceCounterMap theServiceElement = order.get(2);
    assertThat(theServiceElement.getActivityId()).isEqualTo("fork");
    assertThat(theServiceElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theServiceElement.getSequenceCounter();

    ActivitySequenceCounterMap theService1Element = order.get(3);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");
    assertThat(theService1Element.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theService1Element.getSequenceCounter();

    ActivitySequenceCounterMap theJoin1Element = order.get(4);
    assertThat(theJoin1Element.getActivityId()).isEqualTo("join");
    assertThat(theJoin1Element.getSequenceCounter()).isGreaterThan(lastSequenceCounter);

    lastSequenceCounter = theForkElement.getSequenceCounter();

    ActivitySequenceCounterMap theService2Element = order.get(5);
    assertThat(theService2Element.getActivityId()).isEqualTo("theService2");
    assertThat(theService2Element.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theService2Element.getSequenceCounter();

    ActivitySequenceCounterMap theService3Element = order.get(6);
    assertThat(theService3Element.getActivityId()).isEqualTo("theService3");
    assertThat(theService3Element.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
    lastSequenceCounter = theService3Element.getSequenceCounter();

    ActivitySequenceCounterMap theJoin2Element = order.get(7);
    assertThat(theJoin2Element.getActivityId()).isEqualTo("join");
    assertThat(theJoin2Element.getSequenceCounter()).isGreaterThan(lastSequenceCounter);

    assertThat(theJoin2Element.getSequenceCounter()).isNotEqualTo(theJoin1Element.getSequenceCounter());

    ActivitySequenceCounterMap theService4Element = order.get(8);
    assertThat(theService4Element.getActivityId()).isEqualTo("theService4");
    assertThat(theService4Element.getSequenceCounter()).isGreaterThan(theJoin1Element.getSequenceCounter());
    assertThat(theService4Element.getSequenceCounter()).isGreaterThan(theJoin2Element.getSequenceCounter());
    lastSequenceCounter = theService4Element.getSequenceCounter();

    ActivitySequenceCounterMap theEndElement = order.get(9);
    assertThat(theEndElement.getActivityId()).isEqualTo("theEnd");
    assertThat(theEndElement.getSequenceCounter()).isGreaterThan(lastSequenceCounter);
  }

  @Deployment
  @Test
  void testForkAndJoinThreeBranchesDifferentSequenceLength() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when

    // then
    testRule.assertProcessEnded(processInstanceId);

    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(4);

    ActivitySequenceCounterMap theJoin1Element = order.get(0);
    assertThat(theJoin1Element.getActivityId()).isEqualTo("join");

    ActivitySequenceCounterMap theJoin2Element = order.get(1);
    assertThat(theJoin2Element.getActivityId()).isEqualTo("join");

    ActivitySequenceCounterMap theJoin3Element = order.get(2);
    assertThat(theJoin3Element.getActivityId()).isEqualTo("join");

    assertThat(theJoin2Element.getSequenceCounter()).isNotEqualTo(theJoin1Element.getSequenceCounter());
    assertThat(theJoin3Element.getSequenceCounter()).isNotEqualTo(theJoin2Element.getSequenceCounter());
    assertThat(theJoin1Element.getSequenceCounter()).isNotEqualTo(theJoin3Element.getSequenceCounter());

    ActivitySequenceCounterMap theService7Element = order.get(3);
    assertThat(theService7Element.getActivityId()).isEqualTo("theService7");
    assertThat(theService7Element.getSequenceCounter()).isGreaterThan(theJoin1Element.getSequenceCounter());
    assertThat(theService7Element.getSequenceCounter()).isGreaterThan(theJoin2Element.getSequenceCounter());
    assertThat(theService7Element.getSequenceCounter()).isGreaterThan(theJoin3Element.getSequenceCounter());
  }

  @Deployment
  @Test
  void testSequenceInsideSubProcess() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when

    // then
    testRule.assertProcessEnded(processInstanceId);

    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    verifyOrder(order, "theStart", "theService1", "subProcess", "innerStart", "innerService", "innerEnd", "theService2", "theEnd");
  }

  @Deployment
  @Test
  void testForkSameSequenceLengthInsideSubProcess() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when

    // then
    testRule.assertProcessEnded(processInstanceId);

    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(3);

    ActivitySequenceCounterMap innerEnd1Element = order.get(0);
    assertThat(innerEnd1Element.getActivityId()).isEqualTo("innerEnd1");

    ActivitySequenceCounterMap innerEnd2Element = order.get(1);
    assertThat(innerEnd2Element.getActivityId()).isEqualTo("innerEnd2");

    ActivitySequenceCounterMap theService1Element = order.get(2);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");

    assertThat(theService1Element.getSequenceCounter()).isGreaterThan(innerEnd1Element.getSequenceCounter());
    assertThat(theService1Element.getSequenceCounter()).isGreaterThan(innerEnd2Element.getSequenceCounter());
  }

  @Deployment
  @Test
  void testForkDifferentSequenceLengthInsideSubProcess() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when

    // then
    testRule.assertProcessEnded(processInstanceId);

    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(3);

    ActivitySequenceCounterMap innerEnd1Element = order.get(0);
    assertThat(innerEnd1Element.getActivityId()).isEqualTo("innerEnd1");

    ActivitySequenceCounterMap innerEnd2Element = order.get(1);
    assertThat(innerEnd2Element.getActivityId()).isEqualTo("innerEnd2");

    ActivitySequenceCounterMap theService1Element = order.get(2);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");

    assertThat(theService1Element.getSequenceCounter()).isGreaterThan(innerEnd1Element.getSequenceCounter());
    assertThat(theService1Element.getSequenceCounter()).isGreaterThan(innerEnd2Element.getSequenceCounter());
  }

  @Deployment
  @Test
  void testSequentialMultiInstance() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when

    // then
    testRule.assertProcessEnded(processInstanceId);

    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    verifyOrder(order, "theStart", "theService1", "theService2", "theService2", "theService3", "theEnd");
  }

  @Deployment
  @Test
  void testParallelMultiInstance() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when

    // then
    testRule.assertProcessEnded(processInstanceId);

    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(6);

    ActivitySequenceCounterMap theStartElement = order.get(0);
    assertThat(theStartElement.getActivityId()).isEqualTo("theStart");

    ActivitySequenceCounterMap theService1Element = order.get(1);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");
    assertThat(theService1Element.getSequenceCounter()).isGreaterThan(theStartElement.getSequenceCounter());

    ActivitySequenceCounterMap theService21Element = order.get(2);
    assertThat(theService21Element.getActivityId()).isEqualTo("theService2");
    assertThat(theService21Element.getSequenceCounter()).isGreaterThan(theService1Element.getSequenceCounter());

    ActivitySequenceCounterMap theService22Element = order.get(3);
    assertThat(theService22Element.getActivityId()).isEqualTo("theService2");
    assertThat(theService22Element.getSequenceCounter()).isGreaterThan(theService1Element.getSequenceCounter());

    ActivitySequenceCounterMap theService3Element = order.get(4);
    assertThat(theService3Element.getActivityId()).isEqualTo("theService3");
    assertThat(theService3Element.getSequenceCounter()).isGreaterThan(theService21Element.getSequenceCounter());
    assertThat(theService3Element.getSequenceCounter()).isGreaterThan(theService22Element.getSequenceCounter());

    ActivitySequenceCounterMap theEndElement = order.get(5);
    assertThat(theEndElement.getActivityId()).isEqualTo("theEnd");
    assertThat(theEndElement.getSequenceCounter()).isGreaterThan(theService3Element.getSequenceCounter());
  }

  @Deployment
  @Test
  void testLoop() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when

    // then
    testRule.assertProcessEnded(processInstanceId);

    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    verifyOrder(order, "theStart", "theService1", "join", "theScript", "fork", "join", "theScript", "fork", "theService2", "theEnd");
  }

  @Deployment
  @Test
  void testInterruptingBoundaryEvent() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)

    // then (1)
    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    verifyOrder(order, "theStart", "theService1", "theTask");

    // when (2)
    runtimeService.correlateMessage("newMessage");

    // then (2)
    testRule.assertProcessEnded(processInstanceId);

    order = ExecutionOrderListener.getActivityExecutionOrder();
    verifyOrder(order, "theStart", "theService1", "theTask", "messageBoundary", "theServiceAfterMessage", "theEnd2");
  }

  @Deployment
  @Test
  void testNonInterruptingBoundaryEvent() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    // when (1)

    // then (1)
    List<ActivitySequenceCounterMap> order = ExecutionOrderListener.getActivityExecutionOrder();
    verifyOrder(order, "theStart", "theService1", "theTask");

    // when (2)
    runtimeService.correlateMessage("newMessage");

    // then (2)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(6);

    ActivitySequenceCounterMap theService1Element = order.get(1);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");

    ActivitySequenceCounterMap theTaskElement = order.get(2);
    assertThat(theTaskElement.getActivityId()).isEqualTo("theTask");

    ActivitySequenceCounterMap messageBoundaryElement = order.get(3);
    assertThat(messageBoundaryElement.getActivityId()).isEqualTo("messageBoundary");
    assertThat(messageBoundaryElement.getSequenceCounter()).isGreaterThan(theService1Element.getSequenceCounter());
    assertThat(messageBoundaryElement.getSequenceCounter()).isLessThanOrEqualTo(theTaskElement.getSequenceCounter());

    ActivitySequenceCounterMap theServiceAfterMessageElement = order.get(4);
    assertThat(theServiceAfterMessageElement.getActivityId()).isEqualTo("theServiceAfterMessage");
    assertThat(theServiceAfterMessageElement.getSequenceCounter()).isGreaterThan(messageBoundaryElement.getSequenceCounter());

    ActivitySequenceCounterMap theEnd2Element = order.get(5);
    assertThat(theEnd2Element.getActivityId()).isEqualTo("theEnd2");
    assertThat(theEnd2Element.getSequenceCounter()).isGreaterThan(theServiceAfterMessageElement.getSequenceCounter());

    // when (3)
    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    // then (3)
    testRule.assertProcessEnded(processInstanceId);

    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(7);

    ActivitySequenceCounterMap theEnd1Element = order.get(6);
    assertThat(theEnd1Element.getActivityId()).isEqualTo("theEnd1");
    assertThat(theEnd1Element.getSequenceCounter()).isGreaterThan(theEnd2Element.getSequenceCounter());
  }

  protected void verifyOrder(List<ActivitySequenceCounterMap> actualOrder, String... expectedOrder) {
    assertThat(actualOrder).hasSize(expectedOrder.length);

    long lastActualSequenceCounter = 0;
    for (int i = 0; i < expectedOrder.length; i++) {
      ActivitySequenceCounterMap actual = actualOrder.get(i);

      String actualActivityId = actual.getActivityId();
      String expectedActivityId = expectedOrder[i];
      assertThat(expectedActivityId).isEqualTo(actualActivityId);

      long actualSequenceCounter = actual.getSequenceCounter();
      assertThat(actualSequenceCounter).isGreaterThan(lastActualSequenceCounter);

      lastActualSequenceCounter = actualSequenceCounter;
    }
  }

}
