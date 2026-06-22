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
package org.operaton.bpm.engine.test.api.runtime;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessInstantiationAtStartEventTest {

  protected static final String PROCESS_DEFINITION_KEY = "testProcess";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  RepositoryService repositoryService;

  @BeforeEach
  void setUp() {
   testRule.deploy(Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
        .startEvent()
        .userTask()
        .endEvent()
        .done());
  }

  @Test
  void testStartProcessInstanceById() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    runtimeService.createProcessInstanceById(processDefinition.getId()).execute();

    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();
  }

  @Test
  void testStartProcessInstanceByKey() {

    runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY).execute();

    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();
  }

  @Test
  void testStartProcessInstanceAndSetBusinessKey() {

    runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY).businessKey("businessKey").execute();

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getBusinessKey()).isEqualTo("businessKey");
  }

  @Test
  void testStartProcessInstanceAndSetCaseInstanceId() {

    runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY).caseInstanceId("caseInstanceId").execute();

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getCaseInstanceId()).isEqualTo("caseInstanceId");
  }

  @Test
  void testStartProcessInstanceAndSetVariable() {

    ProcessInstance processInstance = runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY).setVariable("var", "value").execute();

    Object variable = runtimeService.getVariable(processInstance.getId(), "var");
    assertThat(variable).isEqualTo("value");
  }

  @Test
  void testStartProcessInstanceAndSetVariables() {
    Map<String, Object> variables = Variables.createVariables().putValue("var1", "v1").putValue("var2", "v2");

    ProcessInstance processInstance = runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY).setVariables(variables).execute();

    assertThat(runtimeService.getVariables(processInstance.getId())).isEqualTo(variables);
  }

  @Test
  void testStartProcessInstanceNoSkipping() {

    runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY).execute(false, false);

    assertThat(runtimeService.createProcessInstanceQuery().count()).isOne();
  }

  @Test
  void testFailToStartProcessInstanceSkipListeners() {
    // given
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    // when/then
    assertThatThrownBy(() -> processInstantiationBuilder.execute(true, false))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot skip");
  }

  @Test
  void testFailToStartProcessInstanceSkipInputOutputMapping() {
    // given
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey(PROCESS_DEFINITION_KEY);

    // when/then
    assertThatThrownBy(() -> processInstantiationBuilder.execute(false, true))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot skip");
  }

}
