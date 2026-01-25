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
package org.operaton.bpm.engine.test.bpmn.iomapping;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.impl.persistence.entity.TimerEntity;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 *
 */
class InputOutputEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  RepositoryService repositoryService;
  ManagementService managementService;

  @BeforeEach
  void setUp() {
    VariableLogDelegate.reset();
  }

  @Deployment
  @Test
  void testMessageThrowEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // input mapping
    Map<String, Object> mappedVariables = VariableLogDelegate.localVariables;
    assertThat(mappedVariables)
            .hasSize(1)
            .containsEntry("mappedVariable", "mappedValue");

    // output mapping
    String variable = (String) runtimeService.getVariableLocal(processInstance.getId(), "outVariable");
    assertThat(variable).isEqualTo("mappedValue");
  }

  @Deployment
  @Test
  void testMessageCatchEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    Execution messageExecution = runtimeService.createExecutionQuery().activityId("messageCatch").singleResult();

    Map<String, Object> localVariables = runtimeService.getVariablesLocal(messageExecution.getId());
    assertThat(localVariables)
            .hasSize(1)
            .containsEntry("mappedVariable", "mappedValue");

    Map<String, Object> variables = new HashMap<>();
    variables.put("messageVariable", "outValue");
    runtimeService.messageEventReceived("IncomingMessage", messageExecution.getId(), variables);

    // output mapping
    String variable = (String) runtimeService.getVariableLocal(processInstance.getId(), "outVariable");
    assertThat(variable).isEqualTo("outValue");
  }

  @Deployment
  @Test
  void testTimerCatchEvent() {
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
  void testNoneThrowEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    Map<String, Object> mappedVariables = VariableLogDelegate.localVariables;
    assertThat(mappedVariables)
            .hasSize(1)
            .containsEntry("mappedVariable", "mappedValue");

    // output mapping
    String variable = (String) runtimeService.getVariableLocal(processInstance.getId(), "outVariable");
    assertThat(variable).isEqualTo("mappedValue");
  }

  @Test
  void testMessageStartEvent() {
    // given
    var deploymentBuilder = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputEventTest.testMessageStartEvent.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
        .isInstanceOf(ParseException.class)
        .hasMessageContaining("operaton:inputOutput mapping unsupported for element type 'startEvent'")
        .extracting(e -> ((ParseException) e).getResourceReports().get(0).getErrors().get(0).getMainElementId())
        .isEqualTo("start");
  }

  @Test
  void testNoneEndEvent() {
    // given
    var deploymentBuilder = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputEventTest.testNoneEndEvent.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
        .isInstanceOf(ParseException.class)
        .hasMessageContaining("operaton:outputParameter not allowed for element type 'endEvent'")
        .extracting(e -> ((ParseException) e).getResourceReports().get(0).getErrors().get(0).getMainElementId())
        .isEqualTo("endMapping");
  }

  @Deployment
  @Test
  void testMessageEndEvent() {
    runtimeService.startProcessInstanceByKey("testProcess");

    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    // input mapping
    Map<String, Object> mappedVariables = VariableLogDelegate.localVariables;
    assertThat(mappedVariables)
            .hasSize(1)
            .containsEntry("mappedVariable", "mappedValue");
  }

  @Deployment
  @Test
  void testMessageCatchAfterEventGateway() {
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

    assertThat(variableInstance).isNotNull();
    assertThat(variableInstance.getValue()).isEqualTo("bar");
  }

  @Deployment
  @Test
  void testTimerCatchAfterEventGateway() {
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

    assertThat(variableInstance).isNotNull();
    assertThat(variableInstance.getValue()).isEqualTo("bar");
  }

  @Deployment
  @Test
  void testSignalCatchAfterEventGateway() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    Execution execution = runtimeService.createExecutionQuery()
      .processInstanceId(processInstance.getId())
      .signalEventSubscriptionName("foo")
      .singleResult();

    assertThat(execution).isNotNull();

    // when
    runtimeService.signalEventReceived("foo", execution.getId());

    // then
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery()
      .processInstanceIdIn(processInstance.getId())
      .variableName("foo")
      .singleResult();

    assertThat(variableInstance).isNotNull();
    assertThat(variableInstance.getValue()).isEqualTo("bar");
  }

  @Deployment
  @Test
  void testConditionalCatchAfterEventGateway() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // when
    runtimeService.setVariable(processInstance.getId(), "var", 1);

    // then
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery()
      .processInstanceIdIn(processInstance.getId())
      .variableName("foo")
      .singleResult();

    assertThat(variableInstance).isNotNull();
    assertThat(variableInstance.getValue()).isEqualTo("bar");
  }

  @Test
  void testMessageBoundaryEvent() {
    // given
    var deploymentBuilder = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputEventTest.testMessageBoundaryEvent.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
        .isInstanceOf(ParseException.class)
        .hasMessageContaining("operaton:inputOutput mapping unsupported for element type 'boundaryEvent'")
        .extracting(e -> ((ParseException) e).getResourceReports().get(0).getErrors().get(0).getMainElementId())
        .isEqualTo("messageBoundary");
  }

  @AfterEach
  void tearDown() {
    VariableLogDelegate.reset();
  }

}
