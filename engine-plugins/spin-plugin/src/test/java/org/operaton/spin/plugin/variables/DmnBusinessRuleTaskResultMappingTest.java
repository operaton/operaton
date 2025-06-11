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
package org.operaton.spin.plugin.variables;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The test is copied from the engine to check how JSON serialization will behave with DMN result object.
 *
 * @author Svetlana Dorokhova
 */
class DmnBusinessRuleTaskResultMappingTest {

  protected static final String TEST_DECISION = "org/operaton/spin/plugin/DmnBusinessRuleTaskResultMappingTest.dmn11.xml";
  protected static final String CUSTOM_MAPPING_BPMN = "org/operaton/spin/plugin/DmnBusinessRuleTaskResultMappingTest.testCustomOutputMapping.bpmn20.xml";
  protected static final String SINGLE_ENTRY_BPMN = "org/operaton/spin/plugin/DmnBusinessRuleTaskResultMappingTest.testSingleEntry.bpmn20.xml";
  protected static final String DEFAULT_MAPPING_BPMN = "org/operaton/spin/plugin/DmnBusinessRuleTaskResultMappingTest.testDefaultMapping.bpmn20.xml";
  protected static final String STORE_DECISION_RESULT_BPMN = "org/operaton/spin/plugin/DmnBusinessRuleTaskResultMappingTest.testStoreDecisionResult.bpmn20.xml";

  @RegisterExtension
  static ProcessEngineExtension engineExtension = ProcessEngineExtension.builder()
          .configurationResource("org/operaton/spin/plugin/json.operaton.cfg.xml").build();
  RuntimeService runtimeService;
  HistoryService historyService;

  @Deployment(resources = {STORE_DECISION_RESULT_BPMN, TEST_DECISION})
  @Test
  void storeDecisionResult() {
    ProcessInstance processInstance = startTestProcess("multiple entries");

    //deserialization is not working for this type of object -> deserializeValue parameter is false
    assertNotNull(runtimeService.getVariableTyped(processInstance.getId(), "result", false));
  }

  @Deployment(resources = {CUSTOM_MAPPING_BPMN, TEST_DECISION})
  @Test
  void customOutputMapping() {
    ProcessInstance processInstance = startTestProcess("multiple entries");

    assertEquals("foo", runtimeService.getVariable(processInstance.getId(), "result1"));
    assertEquals(Variables.stringValue("foo"), runtimeService.getVariableTyped(processInstance.getId(), "result1"));

    assertEquals("bar", runtimeService.getVariable(processInstance.getId(), "result2"));
    assertEquals(Variables.stringValue("bar"), runtimeService.getVariableTyped(processInstance.getId(), "result2"));
  }

  @Deployment(resources = {SINGLE_ENTRY_BPMN, TEST_DECISION})
  @Test
  void singleEntryMapping() {
    ProcessInstance processInstance = startTestProcess("single entry");

    assertEquals("foo", runtimeService.getVariable(processInstance.getId(), "result"));
    assertEquals(Variables.stringValue("foo"), runtimeService.getVariableTyped(processInstance.getId(), "result"));
  }

  @Deployment(resources = {DEFAULT_MAPPING_BPMN, TEST_DECISION})
  @Test
  void transientDecisionResult() {
    // when a decision is evaluated and the result is stored in a transient variable "decisionResult"
    ProcessInstance processInstance = startTestProcess("single entry");

    // then the variable should not be available outside the business rule task
    assertNull(runtimeService.getVariable(processInstance.getId(), "decisionResult"));
    // and should not create an entry in history since it is not persistent
    assertNull(historyService.createHistoricVariableInstanceQuery().variableName("decisionResult").singleResult());
  }

  protected ProcessInstance startTestProcess(String input) {
    return runtimeService.startProcessInstanceByKey("testProcess", Collections.<String, Object>singletonMap("input", input));
  }

}
