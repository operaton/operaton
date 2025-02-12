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
package org.operaton.bpm.engine.test.standalone.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.standalone.entity.ExecutionOrderListener.ActivitySequenceCounterMap;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
public class ExecutionSequenceCounterTest extends PluggableProcessEngineTest {

  @Before
  public void setUp() {
    ExecutionOrderListener.clearActivityExecutionOrder();
  }

  @Deployment
  @Test
  public void testSequence() {
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
  public void testForkSameSequenceLengthWithoutWaitStates() {
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
  public void testForkSameSequenceLengthWithAsyncEndEvent() {
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
    assertTrue(theStartElement.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theStartElement.getSequenceCounter();

    ActivitySequenceCounterMap theForkElement = order.get(1);
    assertThat(theForkElement.getActivityId()).isEqualTo("theService");
    assertTrue(theForkElement.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theForkElement.getSequenceCounter();

    ActivitySequenceCounterMap theServiceElement = order.get(2);
    assertThat(theServiceElement.getActivityId()).isEqualTo("fork");
    assertTrue(theServiceElement.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theServiceElement.getSequenceCounter();

    ActivitySequenceCounterMap theService1Element = order.get(3);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");
    assertTrue(theService1Element.getSequenceCounter() > lastSequenceCounter);

    ActivitySequenceCounterMap theService2Element = order.get(4);
    assertThat(theService2Element.getActivityId()).isEqualTo("theService2");
    assertTrue(theService2Element.getSequenceCounter() > lastSequenceCounter);

    // when (2)
    String jobId = jobQuery.activityId("theEnd1").singleResult().getId();
    managementService.executeJob(jobId);

    // then (2)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(6);

    ActivitySequenceCounterMap theEnd1Element = order.get(5);
    assertThat(theEnd1Element.getActivityId()).isEqualTo("theEnd1");
    assertTrue(theEnd1Element.getSequenceCounter() > theService1Element.getSequenceCounter());

    // when (3)
    jobId = jobQuery.activityId("theEnd2").singleResult().getId();
    managementService.executeJob(jobId);

    // then (3)
    testRule.assertProcessEnded(processInstanceId);

    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(7);

    ActivitySequenceCounterMap theEnd2Element = order.get(6);
    assertThat(theEnd2Element.getActivityId()).isEqualTo("theEnd2");
    assertTrue(theEnd2Element.getSequenceCounter() > theService2Element.getSequenceCounter());
  }

  @Deployment
  @Test
  public void testForkDifferentSequenceLengthWithoutWaitStates() {
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
  public void testForkDifferentSequenceLengthWithAsyncEndEvent() {
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
    assertTrue(theStartElement.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theStartElement.getSequenceCounter();

    ActivitySequenceCounterMap theForkElement = order.get(1);
    assertThat(theForkElement.getActivityId()).isEqualTo("theService");
    assertTrue(theForkElement.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theForkElement.getSequenceCounter();

    ActivitySequenceCounterMap theServiceElement = order.get(2);
    assertThat(theServiceElement.getActivityId()).isEqualTo("fork");
    assertTrue(theServiceElement.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theServiceElement.getSequenceCounter();

    ActivitySequenceCounterMap theService1Element = order.get(3);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");
    assertTrue(theService1Element.getSequenceCounter() > lastSequenceCounter);

    ActivitySequenceCounterMap theService2Element = order.get(4);
    assertThat(theService2Element.getActivityId()).isEqualTo("theService2");
    assertTrue(theService2Element.getSequenceCounter() > lastSequenceCounter);

    ActivitySequenceCounterMap theService3Element = order.get(5);
    assertThat(theService3Element.getActivityId()).isEqualTo("theService3");
    assertTrue(theService3Element.getSequenceCounter() > theService2Element.getSequenceCounter() );

    // when (2)
    String jobId = jobQuery.activityId("theEnd1").singleResult().getId();
    managementService.executeJob(jobId);

    // then (2)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(7);

    ActivitySequenceCounterMap theEnd1Element = order.get(6);
    assertThat(theEnd1Element.getActivityId()).isEqualTo("theEnd1");
    assertTrue(theEnd1Element.getSequenceCounter() > theService1Element.getSequenceCounter());

    // when (3)
    jobId = jobQuery.activityId("theEnd2").singleResult().getId();
    managementService.executeJob(jobId);

    // then (3)
    testRule.assertProcessEnded(processInstanceId);

    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(8);

    ActivitySequenceCounterMap theEnd2Element = order.get(7);
    assertThat(theEnd2Element.getActivityId()).isEqualTo("theEnd2");
    assertTrue(theEnd2Element.getSequenceCounter() > theService3Element.getSequenceCounter());
  }

  @Deployment
  @Test
  public void testForkReplaceBy() {
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
    assertTrue(theService4Element.getSequenceCounter() > theService3Element.getSequenceCounter());

    ActivitySequenceCounterMap theService5Element = order.get(3);
    assertThat(theService5Element.getActivityId()).isEqualTo("theService5");
    assertTrue(theService5Element.getSequenceCounter() > theService4Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd2Element = order.get(4);
    assertThat(theEnd2Element.getActivityId()).isEqualTo("theEnd2");
    assertTrue(theEnd2Element.getSequenceCounter() > theService5Element.getSequenceCounter());

    // when (3)
    jobId = jobQuery.activityId("theService2").singleResult().getId();
    managementService.executeJob(jobId);

    // then (3)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(7);

    ActivitySequenceCounterMap theService2Element = order.get(5);
    assertThat(theService2Element.getActivityId()).isEqualTo("theService2");
    assertTrue(theService2Element.getSequenceCounter() > theService1Element.getSequenceCounter());
    assertTrue(theService2Element.getSequenceCounter() > theEnd2Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd1Element = order.get(6);
    assertThat(theEnd1Element.getActivityId()).isEqualTo("theEnd1");
    assertTrue(theEnd1Element.getSequenceCounter() > theService2Element.getSequenceCounter());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/standalone/entity/ExecutionSequenceCounterTest.testForkReplaceBy.bpmn20.xml"})
  @Test
  public void testForkReplaceByAnotherExecutionOrder() {
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
    assertTrue(theService2Element.getSequenceCounter() > theService1Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd1Element = order.get(3);
    assertThat(theEnd1Element.getActivityId()).isEqualTo("theEnd1");
    assertTrue(theEnd1Element.getSequenceCounter() > theService2Element.getSequenceCounter());

    // when (3)
    jobId = jobQuery.activityId("theService4").singleResult().getId();
    managementService.executeJob(jobId);

    // then (3)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(7);

    ActivitySequenceCounterMap theService4Element = order.get(4);
    assertThat(theService4Element.getActivityId()).isEqualTo("theService4");
    assertTrue(theService4Element.getSequenceCounter() > theService3Element.getSequenceCounter());
    assertTrue(theService4Element.getSequenceCounter() > theEnd1Element.getSequenceCounter());

    ActivitySequenceCounterMap theService5Element = order.get(5);
    assertThat(theService5Element.getActivityId()).isEqualTo("theService5");
    assertTrue(theService5Element.getSequenceCounter() > theService4Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd2Element = order.get(6);
    assertThat(theEnd2Element.getActivityId()).isEqualTo("theEnd2");
    assertTrue(theEnd2Element.getSequenceCounter() > theService5Element.getSequenceCounter());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  public void testForkReplaceByThreeBranches() {
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
    assertTrue(theService2Element.getSequenceCounter() > theService1Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd1Element = order.get(4);
    assertThat(theEnd1Element.getActivityId()).isEqualTo("theEnd1");
    assertTrue(theEnd1Element.getSequenceCounter() > theService2Element.getSequenceCounter());

    // when (3)
    jobId = jobQuery.activityId("theService4").singleResult().getId();
    managementService.executeJob(jobId);

    // then (3)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(8);

    ActivitySequenceCounterMap theService4Element = order.get(5);
    assertThat(theService4Element.getActivityId()).isEqualTo("theService4");
    assertTrue(theService4Element.getSequenceCounter() > theService3Element.getSequenceCounter());

    ActivitySequenceCounterMap theService5Element = order.get(6);
    assertThat(theService5Element.getActivityId()).isEqualTo("theService5");
    assertTrue(theService5Element.getSequenceCounter() > theService4Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd2Element = order.get(7);
    assertThat(theEnd2Element.getActivityId()).isEqualTo("theEnd2");
    assertTrue(theEnd2Element.getSequenceCounter() > theService5Element.getSequenceCounter());

    // when (4)
    jobId = jobQuery.activityId("theService7").singleResult().getId();
    managementService.executeJob(jobId);

    // then (4)
    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(12);

    ActivitySequenceCounterMap theService7Element = order.get(8);
    assertThat(theService7Element.getActivityId()).isEqualTo("theService7");
    assertTrue(theService7Element.getSequenceCounter() > theService6Element.getSequenceCounter());
    assertTrue(theService7Element.getSequenceCounter() > theEnd2Element.getSequenceCounter());

    ActivitySequenceCounterMap theService8Element = order.get(9);
    assertThat(theService8Element.getActivityId()).isEqualTo("theService8");
    assertTrue(theService8Element.getSequenceCounter() > theService7Element.getSequenceCounter());

    ActivitySequenceCounterMap theService9Element = order.get(10);
    assertThat(theService9Element.getActivityId()).isEqualTo("theService9");
    assertTrue(theService9Element.getSequenceCounter() > theService8Element.getSequenceCounter());

    ActivitySequenceCounterMap theEnd3Element = order.get(11);
    assertThat(theEnd3Element.getActivityId()).isEqualTo("theEnd3");
    assertTrue(theEnd3Element.getSequenceCounter() > theService9Element.getSequenceCounter());

    testRule.assertProcessEnded(processInstanceId);
  }

  @Deployment
  @Test
  public void testForkAndJoinSameSequenceLength() {
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
    assertTrue(theStartElement.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theStartElement.getSequenceCounter();

    ActivitySequenceCounterMap theForkElement = order.get(1);
    assertThat(theForkElement.getActivityId()).isEqualTo("theService");
    assertTrue(theForkElement.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theForkElement.getSequenceCounter();

    ActivitySequenceCounterMap theServiceElement = order.get(2);
    assertThat(theServiceElement.getActivityId()).isEqualTo("fork");
    assertTrue(theServiceElement.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theServiceElement.getSequenceCounter();

    ActivitySequenceCounterMap theService1Element = order.get(3);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");
    assertTrue(theService1Element.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theService1Element.getSequenceCounter();

    ActivitySequenceCounterMap theJoin1Element = order.get(4);
    assertThat(theJoin1Element.getActivityId()).isEqualTo("join");
    assertTrue(theJoin1Element.getSequenceCounter() > lastSequenceCounter);

    lastSequenceCounter = theForkElement.getSequenceCounter();

    ActivitySequenceCounterMap theService2Element = order.get(5);
    assertThat(theService2Element.getActivityId()).isEqualTo("theService2");
    assertTrue(theService2Element.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theService2Element.getSequenceCounter();

    ActivitySequenceCounterMap theJoin2Element = order.get(6);
    assertThat(theJoin2Element.getActivityId()).isEqualTo("join");
    assertTrue(theJoin2Element.getSequenceCounter() > lastSequenceCounter);

    ActivitySequenceCounterMap theService3Element = order.get(7);
    assertThat(theService3Element.getActivityId()).isEqualTo("theService3");
    assertTrue(theService3Element.getSequenceCounter() > theJoin1Element.getSequenceCounter());
    assertTrue(theService3Element.getSequenceCounter() > theJoin2Element.getSequenceCounter());
    lastSequenceCounter = theService3Element.getSequenceCounter();

    ActivitySequenceCounterMap theEndElement = order.get(8);
    assertThat(theEndElement.getActivityId()).isEqualTo("theEnd");
    assertTrue(theEndElement.getSequenceCounter() > lastSequenceCounter);
  }

  @Deployment
  @Test
  public void testForkAndJoinDifferentSequenceLength() {
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
    assertTrue(theStartElement.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theStartElement.getSequenceCounter();

    ActivitySequenceCounterMap theForkElement = order.get(1);
    assertThat(theForkElement.getActivityId()).isEqualTo("theService");
    assertTrue(theForkElement.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theForkElement.getSequenceCounter();

    ActivitySequenceCounterMap theServiceElement = order.get(2);
    assertThat(theServiceElement.getActivityId()).isEqualTo("fork");
    assertTrue(theServiceElement.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theServiceElement.getSequenceCounter();

    ActivitySequenceCounterMap theService1Element = order.get(3);
    assertThat(theService1Element.getActivityId()).isEqualTo("theService1");
    assertTrue(theService1Element.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theService1Element.getSequenceCounter();

    ActivitySequenceCounterMap theJoin1Element = order.get(4);
    assertThat(theJoin1Element.getActivityId()).isEqualTo("join");
    assertTrue(theJoin1Element.getSequenceCounter() > lastSequenceCounter);

    lastSequenceCounter = theForkElement.getSequenceCounter();

    ActivitySequenceCounterMap theService2Element = order.get(5);
    assertThat(theService2Element.getActivityId()).isEqualTo("theService2");
    assertTrue(theService2Element.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theService2Element.getSequenceCounter();

    ActivitySequenceCounterMap theService3Element = order.get(6);
    assertThat(theService3Element.getActivityId()).isEqualTo("theService3");
    assertTrue(theService3Element.getSequenceCounter() > lastSequenceCounter);
    lastSequenceCounter = theService3Element.getSequenceCounter();

    ActivitySequenceCounterMap theJoin2Element = order.get(7);
    assertThat(theJoin2Element.getActivityId()).isEqualTo("join");
    assertTrue(theJoin2Element.getSequenceCounter() > lastSequenceCounter);

    assertNotEquals(theJoin1Element.getSequenceCounter(), theJoin2Element.getSequenceCounter());

    ActivitySequenceCounterMap theService4Element = order.get(8);
    assertThat(theService4Element.getActivityId()).isEqualTo("theService4");
    assertTrue(theService4Element.getSequenceCounter() > theJoin1Element.getSequenceCounter());
    assertTrue(theService4Element.getSequenceCounter() > theJoin2Element.getSequenceCounter());
    lastSequenceCounter = theService4Element.getSequenceCounter();

    ActivitySequenceCounterMap theEndElement = order.get(9);
    assertThat(theEndElement.getActivityId()).isEqualTo("theEnd");
    assertTrue(theEndElement.getSequenceCounter() > lastSequenceCounter);
  }

  @Deployment
  @Test
  public void testForkAndJoinThreeBranchesDifferentSequenceLength() {
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

    assertNotEquals(theJoin1Element.getSequenceCounter(), theJoin2Element.getSequenceCounter());
    assertNotEquals(theJoin2Element.getSequenceCounter(), theJoin3Element.getSequenceCounter());
    assertNotEquals(theJoin3Element.getSequenceCounter(), theJoin1Element.getSequenceCounter());

    ActivitySequenceCounterMap theService7Element = order.get(3);
    assertThat(theService7Element.getActivityId()).isEqualTo("theService7");
    assertTrue(theService7Element.getSequenceCounter() > theJoin1Element.getSequenceCounter());
    assertTrue(theService7Element.getSequenceCounter() > theJoin2Element.getSequenceCounter());
    assertTrue(theService7Element.getSequenceCounter() > theJoin3Element.getSequenceCounter());
  }

  @Deployment
  @Test
  public void testSequenceInsideSubProcess() {
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
  public void testForkSameSequenceLengthInsideSubProcess() {
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

    assertTrue(theService1Element.getSequenceCounter() > innerEnd1Element.getSequenceCounter());
    assertTrue(theService1Element.getSequenceCounter() > innerEnd2Element.getSequenceCounter());
  }

  @Deployment
  @Test
  public void testForkDifferentSequenceLengthInsideSubProcess() {
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

    assertTrue(theService1Element.getSequenceCounter() > innerEnd1Element.getSequenceCounter());
    assertTrue(theService1Element.getSequenceCounter() > innerEnd2Element.getSequenceCounter());
  }

  @Deployment
  @Test
  public void testSequentialMultiInstance() {
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
  public void testParallelMultiInstance() {
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
    assertTrue(theService1Element.getSequenceCounter() > theStartElement.getSequenceCounter());

    ActivitySequenceCounterMap theService21Element = order.get(2);
    assertThat(theService21Element.getActivityId()).isEqualTo("theService2");
    assertTrue(theService21Element.getSequenceCounter() > theService1Element.getSequenceCounter());

    ActivitySequenceCounterMap theService22Element = order.get(3);
    assertThat(theService22Element.getActivityId()).isEqualTo("theService2");
    assertTrue(theService22Element.getSequenceCounter() > theService1Element.getSequenceCounter());

    ActivitySequenceCounterMap theService3Element = order.get(4);
    assertThat(theService3Element.getActivityId()).isEqualTo("theService3");
    assertTrue(theService3Element.getSequenceCounter() > theService21Element.getSequenceCounter());
    assertTrue(theService3Element.getSequenceCounter() > theService22Element.getSequenceCounter());

    ActivitySequenceCounterMap theEndElement = order.get(5);
    assertThat(theEndElement.getActivityId()).isEqualTo("theEnd");
    assertTrue(theEndElement.getSequenceCounter() > theService3Element.getSequenceCounter());
  }

  @Deployment
  @Test
  public void testLoop() {
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
  public void testInterruptingBoundaryEvent() {
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
  public void testNonInterruptingBoundaryEvent() {
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
    assertTrue(messageBoundaryElement.getSequenceCounter() > theService1Element.getSequenceCounter());
    assertFalse(messageBoundaryElement.getSequenceCounter() > theTaskElement.getSequenceCounter());

    ActivitySequenceCounterMap theServiceAfterMessageElement = order.get(4);
    assertThat(theServiceAfterMessageElement.getActivityId()).isEqualTo("theServiceAfterMessage");
    assertTrue(theServiceAfterMessageElement.getSequenceCounter() > messageBoundaryElement.getSequenceCounter());

    ActivitySequenceCounterMap theEnd2Element = order.get(5);
    assertThat(theEnd2Element.getActivityId()).isEqualTo("theEnd2");
    assertTrue(theEnd2Element.getSequenceCounter() > theServiceAfterMessageElement.getSequenceCounter());

    // when (3)
    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    // then (3)
    testRule.assertProcessEnded(processInstanceId);

    order = ExecutionOrderListener.getActivityExecutionOrder();
    assertThat(order).hasSize(7);

    ActivitySequenceCounterMap theEnd1Element = order.get(6);
    assertThat(theEnd1Element.getActivityId()).isEqualTo("theEnd1");
    assertTrue(theEnd1Element.getSequenceCounter() > theEnd2Element.getSequenceCounter());
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
      assertTrue(actualSequenceCounter > lastActualSequenceCounter);

      lastActualSequenceCounter = actualSequenceCounter;
    }
  }

}
