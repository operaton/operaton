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
package org.operaton.bpm.engine.test.bpmn.event.signal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Nikola Koevski
 */
class SignalEventPayloadTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(config -> config.setJavaSerializationFormatEnabled(true))
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  TaskService taskService;

  /**
   * Test case for CAM-8820 with a catching Start Signal event.
   * Using Source and Target Variable name mapping attributes.
   */
  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.throwSignalWithPayload.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.catchSignalWithPayloadStart.bpmn20.xml"})
  void testSignalPayloadStart() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("payloadVar1", "payloadVal1");
    variables.put("payloadVar2", "payloadVal2");

    // when
    runtimeService.startProcessInstanceByKey("throwPayloadSignal", variables);

    // then
    Task catchingPiUserTask = taskService.createTaskQuery().singleResult();

    List<VariableInstance> catchingPiVariables = runtimeService.createVariableInstanceQuery()
      .processInstanceIdIn(catchingPiUserTask.getProcessInstanceId())
      .list();
    assertThat(catchingPiVariables).hasSize(2);

    for(VariableInstance variable : catchingPiVariables) {
      if(variable.getName().equals("payloadVar1Target")) {
        assertThat(variable.getValue()).isEqualTo("payloadVal1");
      } else {
        assertThat(variable.getValue()).isEqualTo("payloadVal2");
      }
    }
  }

  /**
   * Test case for CAM-8820 with a catching Intermediate Signal event.
   * Using Source and Target Variable name mapping attributes.
   */
  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.throwSignalWithPayload.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.catchSignalWithPayloadIntermediate.bpmn20.xml"})
  void testSignalPayloadIntermediate() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("payloadVar1", "payloadVal1");
    variables.put("payloadVar2", "payloadVal2");
    ProcessInstance catchingPI = runtimeService.startProcessInstanceByKey("catchIntermediatePayloadSignal");

    // when
    runtimeService.startProcessInstanceByKey("throwPayloadSignal", variables);

    // then
    List<VariableInstance> catchingPiVariables = runtimeService
      .createVariableInstanceQuery()
      .processInstanceIdIn(catchingPI.getId())
      .list();
    assertThat(catchingPiVariables).hasSize(2);

    for(VariableInstance variable : catchingPiVariables) {
      if(variable.getName().equals("payloadVar1Target")) {
        assertThat(variable.getValue()).isEqualTo("payloadVal1");
      } else {
        assertThat(variable.getValue()).isEqualTo("payloadVal2");
      }
    }
  }

  /**
   * Test case for CAM-8820 with an expression as a source.
   */
  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.throwSignalWithExpressionPayload.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.catchSignalWithPayloadIntermediate.bpmn20.xml"})
  void testSignalSourceExpressionPayload() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("payloadVar", "Val");
    ProcessInstance catchingPI = runtimeService.startProcessInstanceByKey("catchIntermediatePayloadSignal");

    // when
    runtimeService.startProcessInstanceByKey("throwExpressionPayloadSignal", variables);

    // then
    List<VariableInstance> catchingPiVariables = runtimeService
      .createVariableInstanceQuery()
      .processInstanceIdIn(catchingPI.getId())
      .list();
    assertThat(catchingPiVariables).hasSize(1);

    assertThat(catchingPiVariables.get(0).getName()).isEqualTo("srcExpressionResVal");
    assertThat(catchingPiVariables.get(0).getValue()).isEqualTo("sourceVal");
  }

  /**
   * Test case for CAM-8820 with all the (global) source variables
   * as the signal payload.
   */
  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.throwSignalWithAllVariablesPayload.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.catchSignalWithPayloadIntermediate.bpmn20.xml"})
  void testSignalAllSourceVariablesPayload() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("payloadVar1", "payloadVal1");
    variables.put("payloadVar2", "payloadVal2");
    ProcessInstance catchingPI = runtimeService.startProcessInstanceByKey("catchIntermediatePayloadSignal");

    // when
    runtimeService.startProcessInstanceByKey("throwPayloadSignal", variables);

    // then
    List<VariableInstance> catchingPiVariables = runtimeService
      .createVariableInstanceQuery()
      .processInstanceIdIn(catchingPI.getId())
      .list();
    assertThat(catchingPiVariables).hasSize(2);

    for(VariableInstance variable : catchingPiVariables) {
      if(variable.getName().equals("payloadVar1")) {
        assertThat(variable.getValue()).isEqualTo("payloadVal1");
      } else {
        assertThat(variable.getValue()).isEqualTo("payloadVal2");
      }
    }
  }

  /**
   * Test case for CAM-8820 with all the (local) source variables
   * as the signal payload.
   */
  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.throwEndSignalEventWithAllLocalVariablesPayload.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.catchSignalWithPayloadIntermediate.bpmn20.xml"})
  void testSignalAllLocalSourceVariablesPayload() {
    // given
    Map<String, Object> variables = new HashMap<>();
    variables.put("payloadVar1", "payloadVal1");
    String localVar1 = "localVar1";
    String localVal1 = "localVal1";
    String localVal2 = "localVal2";
    ProcessInstance catchingPI = runtimeService.startProcessInstanceByKey("catchIntermediatePayloadSignal");

    // when
    runtimeService.startProcessInstanceByKey("throwPayloadSignal", variables);

    // then
    List<VariableInstance> catchingPiVariables = runtimeService
      .createVariableInstanceQuery()
      .processInstanceIdIn(catchingPI.getId())
      .list();
    assertThat(catchingPiVariables).hasSize(2);

    for(VariableInstance variable : catchingPiVariables) {
      if(variable.getName().equals(localVar1)) {
        assertThat(variable.getValue()).isEqualTo(localVal1);
      } else {
        assertThat(variable.getValue()).isEqualTo(localVal2);
      }
    }
  }

  /**
   * Test case for CAM-8820 with a Business Key
   * as signal payload.
   */
  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.throwSignalWithBusinessKeyPayload.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.catchSignalWithPayloadStart.bpmn20.xml"})
  void testSignalBusinessKeyPayload() {
    // given
    String businessKey = "aBusinessKey";

    // when
    runtimeService.startProcessInstanceByKey("throwBusinessKeyPayloadSignal", businessKey);

    // then
    ProcessInstance catchingPI = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(catchingPI.getBusinessKey()).isEqualTo(businessKey);
  }

  /**
   * Test case for CAM-8820 with all possible options for a signal payload.
   */
  @Test
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.throwSignalWithAllOptions.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/signal/SignalEventPayloadTests.catchSignalWithPayloadStart.bpmn20.xml"})
  void testSignalPayloadWithAllOptions() {
    // given
    Map<String, Object> variables = new HashMap<>();
    String globalVar1 = "payloadVar1";
    String globalVal1 = "payloadVar1";
    String globalVar2 = "payloadVar2";
    String globalVal2 = "payloadVal2";
    variables.put(globalVar1, globalVal1);
    variables.put(globalVar2, globalVal2);
    String localVar1 = "localVar1";
    String localVal1 = "localVal1";
    String localVar2 = "localVar2";
    String localVal2 = "localVal2";
    String businessKey = "aBusinessKey";

    // when
    runtimeService.startProcessInstanceByKey("throwCompletePayloadSignal", businessKey, variables);

    // then
    Task catchingPiUserTask = taskService.createTaskQuery().singleResult();
    ProcessInstance catchingPI = runtimeService.createProcessInstanceQuery().processInstanceId(catchingPiUserTask.getProcessInstanceId()).singleResult();
    assertThat(catchingPI.getBusinessKey()).isEqualTo(businessKey);

    List<VariableInstance> targetVariables = runtimeService.createVariableInstanceQuery().processInstanceIdIn(catchingPiUserTask.getProcessInstanceId()).list();
    assertThat(targetVariables).hasSize(4);

    for (VariableInstance variable : targetVariables) {
      if (variable.getName().equals(globalVar1 + "Target")) {
        assertThat(variable.getValue()).isEqualTo(globalVal1);
      } else if (variable.getName().equals(globalVar2 + "Target")) {
        assertThat(variable.getValue()).isEqualTo(globalVal2 + "Source");
      } else if (variable.getName().equals(localVar1)) {
        assertThat(variable.getValue()).isEqualTo(localVal1);
      } else if (variable.getName().equals(localVar2)) {
        assertThat(variable.getValue()).isEqualTo(localVal2);
      }
    }
  }
}
