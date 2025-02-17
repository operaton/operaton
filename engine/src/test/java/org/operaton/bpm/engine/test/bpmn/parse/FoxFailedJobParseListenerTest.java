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
package org.operaton.bpm.engine.test.bpmn.parse;

import static org.operaton.bpm.engine.impl.bpmn.parser.DefaultFailedJobParseListener.FAILED_JOB_CONFIGURATION;
import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessInstanceWithVariablesImpl;
import org.operaton.bpm.engine.impl.pvm.process.ActivityImpl;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

public class FoxFailedJobParseListenerTest extends PluggableProcessEngineTest {

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/parse/FoxFailedJobParseListenerTest.testUserTask.bpmn20.xml" })
  @Test
  public void testUserTaskParseFailedJobRetryTimeCycle() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("asyncUserTaskFailedJobRetryTimeCycle");

    ActivityImpl userTask = findActivity(pi, "task");
    checkFoxFailedJobConfig(userTask);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/parse/OperatonFailedJobParseListenerTest.testUserTask.bpmn20.xml" })
  @Test
  public void testUserTaskParseFailedJobRetryTimeCycleInActivitiNamespace() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("asyncUserTaskFailedJobRetryTimeCycle");

    ActivityImpl userTask = findActivity(pi, "task");
    checkFoxFailedJobConfig(userTask);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/parse/FoxFailedJobParseListenerTest.testUserTask.bpmn20.xml" })
  @Test
  public void testNotAsyncUserTaskParseFailedJobRetryTimeCycle() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("notAsyncUserTaskFailedJobRetryTimeCycle");

    ActivityImpl userTask = findActivity(pi, "notAsyncTask");
    checkNotContainingFoxFailedJobConfig(userTask);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/parse/FoxFailedJobParseListenerTest.testUserTask.bpmn20.xml" })
  @Test
  public void testAsyncUserTaskButWithoutParseFailedJobRetryTimeCycle() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("asyncUserTaskButWithoutFailedJobRetryTimeCycle");

    ActivityImpl userTask = findActivity(pi, "asyncTaskWithoutFailedJobRetryTimeCycle");
    checkNotContainingFoxFailedJobConfig(userTask);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/parse/FoxFailedJobParseListenerTest.testTimer.bpmn20.xml" })
  @Test
  public void testTimerBoundaryEventWithFailedJobRetryTimeCycle() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("boundaryEventWithFailedJobRetryTimeCycle");

    ActivityImpl boundaryActivity = findActivity(pi, "boundaryTimerWithFailedJobRetryTimeCycle");
    checkFoxFailedJobConfig(boundaryActivity);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/parse/FoxFailedJobParseListenerTest.testTimer.bpmn20.xml" })
  @Test
  public void testTimerBoundaryEventWithoutFailedJobRetryTimeCycle() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("boundaryEventWithoutFailedJobRetryTimeCycle");

    ActivityImpl boundaryActivity = findActivity(pi, "boundaryTimerWithoutFailedJobRetryTimeCycle");
    checkNotContainingFoxFailedJobConfig(boundaryActivity);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/parse/FoxFailedJobParseListenerTest.testTimer.bpmn20.xml" })
  @Test
  public void testTimerStartEventWithFailedJobRetryTimeCycle() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("startEventWithFailedJobRetryTimeCycle");

    ActivityImpl startEvent = findActivity(pi, "startEventFailedJobRetryTimeCycle");
    checkFoxFailedJobConfig(startEvent);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/parse/FoxFailedJobParseListenerTest.testTimer.bpmn20.xml" })
  @Test
  public void testIntermediateCatchTimerEventWithFailedJobRetryTimeCycle() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("intermediateTimerEventWithFailedJobRetryTimeCycle");

    ActivityImpl timer = findActivity(pi, "timerEventWithFailedJobRetryTimeCycle");
    checkFoxFailedJobConfig(timer);
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/parse/FoxFailedJobParseListenerTest.testSignal.bpmn20.xml" })
  @Test
  public void testSignalEventWithFailedJobRetryTimeCycle() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("signalEventWithFailedJobRetryTimeCycle");

    ActivityImpl signal = findActivity(pi, "signalWithFailedJobRetryTimeCycle");
    checkFoxFailedJobConfig(signal);
  }

  @Deployment
  @Test
  public void testMultiInstanceBodyWithFailedJobRetryTimeCycle() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    ActivityImpl miBody = findMultiInstanceBody(pi, "task");
    checkFoxFailedJobConfig(miBody);

    ActivityImpl innerActivity = findActivity(pi, "task");
    checkNotContainingFoxFailedJobConfig(innerActivity);
  }

  @Deployment
  @Test
  public void testInnerMultiInstanceActivityWithFailedJobRetryTimeCycle() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    ActivityImpl miBody = findMultiInstanceBody(pi, "task");
    checkNotContainingFoxFailedJobConfig(miBody);

    ActivityImpl innerActivity = findActivity(pi, "task");
    checkFoxFailedJobConfig(innerActivity);
  }

  @Deployment
  @Test
  public void testMultiInstanceBodyAndInnerActivityWithFailedJobRetryTimeCycle() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    ActivityImpl miBody = findMultiInstanceBody(pi, "task");
    checkFoxFailedJobConfig(miBody);

    ActivityImpl innerActivity = findActivity(pi, "task");
    checkFoxFailedJobConfig(innerActivity);
  }

  protected ActivityImpl findActivity(ProcessInstance pi, String activityId) {

    ProcessInstanceWithVariablesImpl entity = (ProcessInstanceWithVariablesImpl) pi;
    ProcessDefinitionEntity processDefEntity = entity.getExecutionEntity().getProcessDefinition();

    assertThat(processDefEntity).isNotNull();
    ActivityImpl activity = processDefEntity.findActivity(activityId);
    assertThat(activity).isNotNull();
    return activity;
  }

  protected ActivityImpl findMultiInstanceBody(ProcessInstance pi, String activityId) {
    return findActivity(pi, activityId + BpmnParse.MULTI_INSTANCE_BODY_ID_SUFFIX);
  }

  protected void checkFoxFailedJobConfig(ActivityImpl activity) {
    assertThat(activity).isNotNull();

    assertThat(activity.getProperties().contains(FAILED_JOB_CONFIGURATION)).isTrue();

    Object value = activity.getProperties().get(FAILED_JOB_CONFIGURATION).getRetryIntervals().get(0);
    assertThat(value).isEqualTo("R5/PT5M");
  }

  protected void checkNotContainingFoxFailedJobConfig(ActivityImpl activity) {
    assertThat(activity.getProperties().contains(FAILED_JOB_CONFIGURATION)).isFalse();
  }

}
