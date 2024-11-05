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
package org.operaton.spin.plugin.el;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.spin.json.SpinJsonNode;
import org.operaton.spin.plugin.script.TestVariableScope;
import org.operaton.spin.xml.SpinXmlElement;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>Testcase ensuring integration of operaton Spin into Process Engine expression language.</p>
 *
 * @author Daniel Meyer
 *
 */
class SpinFunctionsTest {
  @RegisterExtension
  static ProcessEngineExtension engineExtension = ProcessEngineExtension.builder().build();
  private ProcessEngineConfigurationImpl processEngineConfiguration;
  private RepositoryService repositoryService;
  private RuntimeService runtimeService;

  String xmlString = "<elementName attrName=\"attrValue\" />";
  String jsonString = "{\"foo\": \"bar\"}";

  @BeforeEach
  void setUp() {
    processEngineConfiguration = engineExtension.getProcessEngineConfiguration();
    runtimeService = engineExtension.getRuntimeService();
    repositoryService = engineExtension.getRepositoryService();
  }

  @SuppressWarnings("unchecked")
  protected <T> T executeExpression(String expression) {

    final TestVariableScope varScope = new TestVariableScope();

    final Expression compiledExpression = processEngineConfiguration.getExpressionManager()
      .createExpression(expression);

    return (T) processEngineConfiguration.getCommandExecutorTxRequired()
      .execute(commandContext -> compiledExpression.getValue(varScope));
  }

  @Test
  void spinSAvailable() {

    SpinXmlElement spinXmlEl = executeExpression("${ S('" + xmlString + "') }");
    assertNotNull(spinXmlEl);
    assertEquals("elementName", spinXmlEl.name());
  }

  @Test
  void spinXMLAvailable() {

    SpinXmlElement spinXmlEl = executeExpression("${ XML('" + xmlString + "') }");
    assertNotNull(spinXmlEl);
    assertEquals("elementName", spinXmlEl.name());
  }

  @Test
  void spinJSONAvailable() {

    SpinJsonNode spinJsonEl = executeExpression("${ JSON('" + jsonString + "') }");
    assertNotNull(spinJsonEl);
    assertEquals("bar", spinJsonEl.prop("foo").stringValue());
  }

  @Test
  void spinXPathAvailable() {

    String elName = executeExpression("${ S('" + xmlString + "').xPath('/elementName').element().name() }");
    assertNotNull(elName);
    assertEquals("elementName", elName);
  }

  @Test
  void spinJsonPathAvailable() {

    String property = executeExpression("${ S('" + jsonString + "').jsonPath('$.foo').stringValue() }");
    assertNotNull(property);
    assertEquals("bar", property);
  }

  @Test
  void spinAvailableInBpmn() {

    BpmnModelInstance bpmnModelInstance = Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .serviceTask()
        .operatonExpression("${ execution.setVariable('customer', "
                                + "S(xmlVar).xPath('/customers/customer').element().toString()"
                             +")}")
      .receiveTask("wait")
      .endEvent()
    .done();

    Deployment deployment = repositoryService.createDeployment()
      .addModelInstance("process.bpmn", bpmnModelInstance)
      .deploy();

    Map<String, Object> variables = new HashMap<>();
    variables.put("xmlVar", "<customers><customer /></customers>");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    String customerXml = (String) runtimeService.getVariable(pi.getId(), "customer");
    assertNotNull(customerXml);
    assertTrue(customerXml.contains("customer"));
    assertFalse(customerXml.contains("customers"));

    runtimeService.signal(pi.getId());

    repositoryService.deleteDeployment(deployment.getId(), true);

  }
}
