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
package org.operaton.spin.plugin.script;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

class SpinJRubyScriptTaskTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();

  RuntimeService runtimeService;
  RepositoryService repositoryService;

  @Test
  @Disabled("CAM-11114")
  void shouldNotLeakVariables() {
    // given
    String varName = "var";
    String varValue = "val";

    BpmnModelInstance model1 = Bpmn.createExecutableProcess("process1")
      .startEvent()
      .scriptTask()
        .scriptFormat("ruby")
        .scriptText("") // do nothing
        .endEvent()
        .done();

    BpmnModelInstance model2 = Bpmn.createExecutableProcess("process2")
      .startEvent()
      .scriptTask()
        .scriptFormat("ruby")
        .scriptText("$execution.setVariable('" + varName + "', $" + varName + ")")
      .userTask()
      .endEvent()
    .done();

    Deployment deployment = repositoryService.createDeployment()
        .addModelInstance("process1.bpmn", model1)
        .addModelInstance("process2.bpmn", model2)
        .deploy();
    engineRule.manageDeployment(deployment);

    VariableMap variables = Variables.createVariables().putValue(varName, varValue);
    runtimeService.startProcessInstanceByKey("process1", variables);

    // when
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("process2");

    // then
    Object actualVarValue = runtimeService.getVariable(instance.getId(), varName);
    // $var is not defined in the context of the second process instance, so the resulting value
    // should be null
    assertThat(actualVarValue).isNull();
  }
}
