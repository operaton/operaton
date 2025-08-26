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
package org.operaton.bpm.engine.test.bpmn.gateway;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.bpmn.iomapping.VariableLogDelegate;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
class EventBasedGatewayInputOutputTest {

  protected static final BpmnModelInstance EVENT_GATEWAY_PROCESS =
    Bpmn.createExecutableProcess("process")
      .startEvent()
      .eventBasedGateway()
      .intermediateCatchEvent("conditionalEvent")
        .operatonOutputParameter("eventOutput", "foo")
        .conditionalEventDefinition()
        .condition("${moveOn}")
        .conditionalEventDefinitionDone()
      .serviceTask("inputParameterTask")
        .operatonInputParameter("variable1", "testValue")
        .operatonClass(VariableLogDelegate.class)
      .endEvent()
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  HistoryService historyService;

  protected boolean skipOutputMappingVal;

  @BeforeEach
  void setUp() {
    skipOutputMappingVal = processEngineConfiguration.isSkipOutputMappingOnCanceledActivities();
    processEngineConfiguration.setSkipOutputMappingOnCanceledActivities(true);
    VariableLogDelegate.reset();
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setSkipOutputMappingOnCanceledActivities(skipOutputMappingVal);
    VariableLogDelegate.reset();
  }

  @Test
  void shouldProcessInputOutputParametersAfterEventGateway() {
    // given
    testRule.deploy(EVENT_GATEWAY_PROCESS);

    // when
    runtimeService.startProcessInstanceByKey("process", Variables.putValue("moveOn", true));

    // then
    List<HistoricVariableInstance> vars = historyService.createHistoricVariableInstanceQuery()
        .variableName("eventOutput")
        .list();
    assertThat(vars).hasSize(1);
    assertThat(vars.get(0).getValue()).isEqualTo("foo");

    assertThat(VariableLogDelegate.localVariables).hasSize(1);
    vars = historyService.createHistoricVariableInstanceQuery()
        .variableName("variable1")
        .list();
    assertThat(vars).hasSize(1);
    assertThat(vars.get(0).getValue()).isEqualTo("testValue");
  }

  @Test
  void shouldNotProcessInputOutputParametersAfterEventGatewayDeletion() {
    // given
    testRule.deploy(EVENT_GATEWAY_PROCESS);
    String instanceId = runtimeService.startProcessInstanceByKey("process", Variables.putValue("moveOn", false)).getId();

    // when
    runtimeService.deleteProcessInstance(instanceId, "manual cancelation");

    // then
    assertThat(VariableLogDelegate.localVariables).isEmpty();
    assertThat(historyService.createHistoricVariableInstanceQuery()
        .variableName("eventOutput")
        .count()).isZero();
    assertThat(historyService.createHistoricVariableInstanceQuery()
        .variableName("variable1")
        .count()).isZero();
  }
}
