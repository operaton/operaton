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
package org.operaton.bpm.engine.test.bpmn.scripttask;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.ScriptCompilationException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Sebastian Menski
 */
class ExternalScriptTaskTest {
  private static final String GREETING_PY = "org/operaton/bpm/engine/test/bpmn/scripttask/greeting.py";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;

  @Deployment
  @Test
  void testDefaultExternalScript() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment
  @Test
  void testDefaultExternalScriptAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptPath", GREETING_PY);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/scripttask/ExternalScriptTaskTest.testDefaultExternalScriptAsVariable.bpmn20.xml"})
  @Test
  void testDefaultExternalScriptAsNonExistingVariable() {
    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process"))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Cannot resolve identifier 'scriptPath'");
  }

  @ParameterizedTest
  @CsvSource({
      "org/operaton/bpm/engine/test/bpmn/scripttask/ExternalScriptTaskTest.testDefaultExternalScriptAsBean.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/scripttask/ExternalScriptTaskTest.testScriptInClasspath.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/scripttask/ExternalScriptTaskTest.testScriptInDeployment.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/scripttask/ExternalScriptTaskTest.testScriptInClasspathAsBean.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/scripttask/ExternalScriptTaskTest.testScriptInDeploymentAsBean.bpmn20.xml"
  })
  void withScriptResourceBean(String bpmnResource) {
    // given
    testRule.deploy(bpmnResource, GREETING_PY);

    // when
    Map<String, Object> variables = Map.of("scriptResourceBean", new ScriptResourceBean());
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment
  @Test
  void testScriptInClasspathAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptPath", "classpath://org/operaton/bpm/engine/test/bpmn/scripttask/greeting.py");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment
  @Test
  void testScriptNotFoundInClasspath() {
    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Unable to find resource at path classpath://org/operaton/bpm/engine/test/bpmn/scripttask/notexisting.py");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/scripttask/ExternalScriptTaskTest.testScriptInDeployment.bpmn20.xml",
      GREETING_PY
  })
  @Test
  void testScriptInDeploymentAfterCacheWasCleaned() {
    processEngineConfiguration.getDeploymentCache().discardProcessDefinitionCache();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/scripttask/ExternalScriptTaskTest.testScriptInDeploymentAsVariable.bpmn20.xml",
      GREETING_PY
  })
  @Test
  void testScriptInDeploymentAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptPath", "deployment://org/operaton/bpm/engine/test/bpmn/scripttask/greeting.py");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    String greeting = (String) runtimeService.getVariable(processInstance.getId(), "greeting");
    assertThat(greeting).isNotNull().isEqualTo("Greetings Operaton speaking");
  }

  @Deployment
  @Test
  void testScriptNotFoundInDeployment() {
    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process"))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Unable to find resource at path deployment://org/operaton/bpm/engine/test/bpmn/scripttask/notexisting.py");
  }

  @Deployment
  @Test
  void testNotExistingImport() {
    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process"))
        .isInstanceOf(ScriptCompilationException.class)
        .hasMessageContaining("import unknown");
  }

}
