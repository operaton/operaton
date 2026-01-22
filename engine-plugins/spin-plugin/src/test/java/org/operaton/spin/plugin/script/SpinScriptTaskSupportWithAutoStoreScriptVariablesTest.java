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

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Daniel Meyer
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class SpinScriptTaskSupportWithAutoStoreScriptVariablesTest {
  private ProcessEngineConfigurationImpl processEngineConfiguration;
  private RepositoryService repositoryService;
  private RuntimeService runtimeService;

  protected static final String TEST_SCRIPT = """
                                        var_s = S('{}')
                                        var_xml = XML('<root/>')
                                        var_json = JSON('{}')
                                        """;

  protected ProcessInstance processInstance;
  private TaskService taskService;

  @BeforeEach
  void setUp() {
    processEngineConfiguration.setAutoStoreScriptVariables(true);
  }

  @AfterEach
  void tearDown() {
    processEngineConfiguration.setAutoStoreScriptVariables(false);
    runtimeService.deleteProcessInstance(processInstance.getId(), "Test shutdown");
  }

  @Test
  void spinInternalVariablesNotExportedGroovyScriptTask() {
    String importXML = "XML = org.operaton.spin.Spin.&XML\n";
    String importJSON = "JSON = org.operaton.spin.Spin.&JSON\n";

    String script = importXML + importJSON + TEST_SCRIPT;

    deployProcess("groovy", script);

    startProcess();
    checkVariables("foo", "var_s", "var_xml", "var_json");
    continueProcess();
    checkVariables("foo", "var_s", "var_xml", "var_json");
  }

  @Test
  @Disabled("https://jira.camunda.com/browse/CAM-5869")
  void testSpinInternalVariablesNotExportedByJavascriptScriptTask() {
    String importXML = "var XML = org.operaton.spin.Spin.XML;\n";
    String importJSON = "var JSON = org.operaton.spin.Spin.JSON;\n";

    String script = importXML + importJSON + TEST_SCRIPT;

    deployProcess("javascript", script);

    startProcess();
    checkVariables("foo", "var_s", "var_xml", "var_json");
    continueProcess();
    checkVariables("foo", "var_s", "var_xml", "var_json");
  }

  @Test
  void spinInternalVariablesNotExportedByPythonScriptTask() {
    String importXML = "import org.operaton.spin.Spin.XML as XML;\n";
    String importJSON = "import org.operaton.spin.Spin.JSON as JSON;\n";

    String script = importXML + importJSON + TEST_SCRIPT;

    deployProcess("python", script);

    startProcess();
    checkVariables("foo", "var_s", "var_xml", "var_json");
    continueProcess();
    checkVariables("foo", "var_s", "var_xml", "var_json");
  }

  @Test
  void spinInternalVariablesNotExportedByRubyScriptTask() {
    String importXML = "def XML(*args)\n\torg.operaton.spin.Spin.XML(*args)\nend\n";
    String importJSON = "def JSON(*args)\n\torg.operaton.spin.Spin.JSON(*args)\nend\n";

    String script = importXML + importJSON + TEST_SCRIPT;

    deployProcess("ruby", script);

    startProcess();
    checkVariablesJRuby("foo");
    continueProcess();
    checkVariablesJRuby("foo");
  }

  protected void startProcess() {
    VariableMap variables = Variables.putValue("foo", "bar");
    processInstance = runtimeService.startProcessInstanceByKey("testProcess", variables);
  }

  protected void continueProcess() {
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());
  }

  protected void checkVariables(String... expectedVariables) {
    Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
    checkVariablesValues(expectedVariables, variables);

    assertThat(variables).hasSize(expectedVariables.length);
  }

  protected void checkVariablesJRuby(String... expectedVariables) {

    Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
    checkVariablesValues(expectedVariables, variables);

    // do not assert number of actual variables here, because JRuby leaks variables (see CAM-11114)
  }

  protected void checkVariablesValues(String[] expectedVariables, Map<String, Object> actualVariables) {
    assertThat(actualVariables).doesNotContainKeys("S", "XML", "JSON");

    for (String expectedVariable : expectedVariables) {
      assertThat(actualVariables).containsKey(expectedVariable);
    }
  }

  protected void deployProcess(String scriptFormat, String scriptText) {
    BpmnModelInstance process = createProcess(scriptFormat, scriptText);
    repositoryService.createDeployment()
      .addModelInstance("testProcess.bpmn", process)
      .addString("testScript.txt", scriptText)
      .deploy();
  }

  protected BpmnModelInstance createProcess(String scriptFormat, String scriptText) {

    return Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .scriptTask()
        .scriptFormat(scriptFormat)
        .scriptText(scriptText)
      .userTask()
      .scriptTask()
        .scriptFormat(scriptFormat)
        .operatonResource("deployment://testScript.txt")
      .userTask()
      .endEvent()
    .done();

  }
}
