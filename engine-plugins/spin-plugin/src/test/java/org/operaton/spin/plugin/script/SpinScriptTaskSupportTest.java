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
package org.operaton.spin.plugin.script;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.DeploymentExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 *
 */
@ExtendWith(ProcessEngineExtension.class)
public class SpinScriptTaskSupportTest {

  public static Object[] data() {
      return new Object[][] {
               { "groovy", "" },
               { "javascript", "" },
               { "python", "" },
               { "ruby", "$" }
         };
  }
  public String language;
  public String variablePrefix;

  RuntimeService runtimeService;

  @RegisterExtension
  static DeploymentExtension deploymentExtension = new DeploymentExtension();

  @MethodSource("data")
  @ParameterizedTest(name = "{index}: {0}")
  void spinAvailable(String language, String variablePrefix) {
    initSpinScriptTaskSupportTest(language, variablePrefix);
    deployProcess(language, setVariableScript("name", "S('<test />').name()"));
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    String variable = (String) runtimeService.getVariable(pi.getId(), "name");
    assertThat(variable).isEqualTo("test");
  }

  @MethodSource("data")
  @ParameterizedTest(name = "{index}: {0}")
  void twoScriptTasks(String language, String variablePrefix) {
    initSpinScriptTaskSupportTest(language, variablePrefix);
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .scriptTask()
        .scriptFormat(language)
        .scriptText(setVariableScript("task1Name", "S('<task1 />').name()"))
      .scriptTask()
        .scriptFormat(language)
        .scriptText(setVariableScript("task2Name", "S('<task2 />').name()"))
      .userTask()
      .endEvent()
    .done();

    deploymentExtension.deploy(modelInstance);

    // when
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    // then
    Object task1Name = runtimeService.getVariable(pi.getId(), "task1Name");
    assertThat(task1Name).isEqualTo("task1");

    Object task2Name = runtimeService.getVariable(pi.getId(), "task2Name");
    assertThat(task2Name).isEqualTo("task2");
  }

  protected String setVariableScript(String name, String valueExpression) {
    return scriptVariableName("execution") + ".setVariable('" + name + "',  " + valueExpression + ")";
  }

  protected String scriptVariableName(String name) {
    return variablePrefix + name;
  }

  protected void deployProcess(String scriptFormat, String scriptText) {
    BpmnModelInstance process = createProcess(scriptFormat, scriptText);

    deploymentExtension.deploy(process);
  }

  protected BpmnModelInstance createProcess(String scriptFormat, String scriptText) {

    return Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .scriptTask()
        .scriptFormat(scriptFormat)
        .scriptText(scriptText)
      .userTask()
      .endEvent()
    .done();

  }

  public void initSpinScriptTaskSupportTest(String language, String variablePrefix) {
    this.language = language;
    this.variablePrefix = variablePrefix;
  }
}
