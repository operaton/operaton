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
package org.operaton.bpm.engine.test.bpmn.iomapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.impl.persistence.entity.TimerEntity;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Thorben Lindhauer
 *
 */
public class InputOutputEventTest extends PluggableProcessEngineTest {

  @Before
  public void setUp() {


    VariableLogDelegate.reset();
  }


  @Deployment
  @Test
  public void testMessageThrowEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // input mapping
    Map<String, Object> mappedVariables = VariableLogDelegate.LOCAL_VARIABLES;
    assertThat(mappedVariables.size()).isEqualTo(1);
    assertThat(mappedVariables.get("mappedVariable")).isEqualTo("mappedValue");

    // output mapping
    String variable = (String) runtimeService.getVariableLocal(processInstance.getId(), "outVariable");
    assertThat(variable).isEqualTo("mappedValue");
  }

  @Deployment
  @Test
  public void testMessageCatchEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    Execution messageExecution = runtimeService.createExecutionQuery().activityId("messageCatch").singleResult();

    Map<String, Object> localVariables = runtimeService.getVariablesLocal(messageExecution.getId());
    assertThat(localVariables.size()).isEqualTo(1);
    assertThat(localVariables.get("mappedVariable")).isEqualTo("mappedValue");

    Map<String, Object> variables = new HashMap<>();
    variables.put("messageVariable", "outValue");
    runtimeService.messageEventReceived("IncomingMessage", messageExecution.getId(), variables);

    // output mapping
    String variable = (String) runtimeService.getVariableLocal(processInstance.getId(), "outVariable");
    assertThat(variable).isEqualTo("outValue");
  }

  @Deployment
  @Test
  public void testTimerCatchEvent() {
    Map<String, Object> variables = new HashMap<>();
    Date dueDate = DateTimeUtil.now().plusMinutes(5).toDate();
    variables.put("outerVariable", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(dueDate));
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    Job job = managementService.createJobQuery().singleResult();
    TimerEntity timer = (TimerEntity) job;
    assertDateEquals(dueDate, timer.getDuedate());
  }

  protected void assertDateEquals(Date expected, Date actual) {
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    assertThat(format.format(actual)).isEqualTo(format.format(expected));
  }

  @Deployment
  @Test
  public void testNoneThrowEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    Map<String, Object> mappedVariables = VariableLogDelegate.LOCAL_VARIABLES;
    assertThat(mappedVariables.size()).isEqualTo(1);
    assertThat(mappedVariables.get("mappedVariable")).isEqualTo("mappedValue");

    // output mapping
    String variable = (String) runtimeService.getVariableLocal(processInstance.getId(), "outVariable");
    assertThat(variable).isEqualTo("mappedValue");
  }

  @Test
  public void testMessageStartEvent() {
    var deploymentBuilder = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputEventTest.testMessageStartEvent.bpmn20.xml");

    try {
      deploymentBuilder.deploy();
      fail("expected exception");
    } catch (ParseException e) {
      testRule.assertTextPresent("operaton:inputOutput mapping unsupported for element type 'startEvent'", e.getMessage());
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("start");
    }
  }

  @Test
  public void testNoneEndEvent() {
    var deploymentBuilder = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputEventTest.testNoneEndEvent.bpmn20.xml");
    try {
      deploymentBuilder.deploy();
      fail("expected exception");
    } catch (ParseException e) {
      testRule.assertTextPresent("operaton:outputParameter not allowed for element type 'endEvent'", e.getMessage());
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("endMapping");
    }
  }

  @Deployment
  @Test
  public void testMessageEndEvent() {
    runtimeService.startProcessInstanceByKey("testProcess");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

    // input mapping
    Map<String, Object> mappedVariables = VariableLogDelegate.LOCAL_VARIABLES;
    assertThat(mappedVariables.size()).isEqualTo(1);
    assertThat(mappedVariables.get("mappedVariable")).isEqualTo("mappedValue");
  }

  @Deployment
  @Test
  public void testMessageCatchAfterEventGateway() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    runtimeService.createMessageCorrelation("foo")
      .processInstanceId(processInstance.getId())
      .correlate();

    // then
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery()
      .processInstanceIdIn(processInstance.getId())
      .variableName("foo")
      .singleResult();

    assertNotNull(variableInstance);
    assertThat(variableInstance.getValue()).isEqualTo("bar");
  }

  @Deployment
  @Test
  public void testTimerCatchAfterEventGateway() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Job job = managementService.createJobQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    // when
    managementService.executeJob(job.getId());

    // then
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery()
      .processInstanceIdIn(processInstance.getId())
      .variableName("foo")
      .singleResult();

    assertNotNull(variableInstance);
    assertThat(variableInstance.getValue()).isEqualTo("bar");
  }

  @Deployment
  @Test
  public void testSignalCatchAfterEventGateway() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Execution execution = runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .signalEventSubscriptionName("foo")
      .singleResult();

    assertNotNull(execution);

    // when
    runtimeService.signalEventReceived("foo", execution.getId());

    // then
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery()
      .processInstanceIdIn(processInstance.getId())
      .variableName("foo")
      .singleResult();

    assertNotNull(variableInstance);
    assertThat(variableInstance.getValue()).isEqualTo("bar");
  }

  @Deployment
  @Test
  public void testConditionalCatchAfterEventGateway() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    runtimeService.setVariable(processInstance.getId(), "var", 1);

    // then
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery()
      .processInstanceIdIn(processInstance.getId())
      .variableName("foo")
      .singleResult();

    assertNotNull(variableInstance);
    assertThat(variableInstance.getValue()).isEqualTo("bar");
  }

  @Test
  public void testMessageBoundaryEvent() {
    var deploymentBuilder = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputEventTest.testMessageBoundaryEvent.bpmn20.xml");
    try {
      deploymentBuilder.deploy();
      fail("expected exception");
    } catch (ParseException e) {
      testRule.assertTextPresent("operaton:inputOutput mapping unsupported for element type 'boundaryEvent'", e.getMessage());
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("messageBoundary");
    }
  }

  @After
  public void tearDown() {


    VariableLogDelegate.reset();
  }

}
