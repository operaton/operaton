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
package org.operaton.bpm.engine.test.api.multitenancy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MultiTenancyBusinessRuleTaskTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String DMN_FILE = "org/operaton/bpm/engine/test/api/multitenancy/simpleDecisionTable.dmn";
  protected static final String DMN_FILE_VERSION_TWO = "org/operaton/bpm/engine/test/api/multitenancy/simpleDecisionTable_v2.dmn";

  protected static final String RESULT_OF_VERSION_ONE = "A";
  protected static final String RESULT_OF_VERSION_TWO = "C";

  public static final String DMN_FILE_VERSION_TAG = "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionVersionTagOkay.dmn11.xml";
  public static final String DMN_FILE_VERSION_TAG_TWO = "org/operaton/bpm/engine/test/dmn/businessruletask/DmnBusinessRuleTaskTest.testDecisionVersionTagOkay_v2.dmn11.xml";

  protected static final String RESULT_OF_VERSION_TAG_ONE = "A";
  protected static final String RESULT_OF_VERSION_TAG_TWO = "C";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;

  @Test
  void testEvaluateDecisionWithDeploymentBinding() {

    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decision")
          .operatonDecisionRefBinding("deployment")
          .operatonMapDecisionResult("singleEntry")
          .operatonResultVariable("decisionVar")
        .operatonAsyncAfter()
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, process, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, process, DMN_FILE_VERSION_TWO);

    ProcessInstance processInstanceOne = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold")
      .processDefinitionTenantId(TENANT_ONE).execute();

    ProcessInstance processInstanceTwo = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold")
      .processDefinitionTenantId(TENANT_TWO).execute();

    assertThat((String)runtimeService.getVariable(processInstanceOne.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
    assertThat((String)runtimeService.getVariable(processInstanceTwo.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_TWO);
  }

  @Test
  void testEvaluateDecisionWithLatestBindingSameVersion() {

    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decision")
          .operatonDecisionRefBinding("latest")
          .operatonMapDecisionResult("singleEntry")
          .operatonResultVariable("decisionVar")
        .operatonAsyncAfter()
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, process, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, process, DMN_FILE_VERSION_TWO);

    ProcessInstance processInstanceOne = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold")
      .processDefinitionTenantId(TENANT_ONE).execute();

    ProcessInstance processInstanceTwo = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold")
      .processDefinitionTenantId(TENANT_TWO).execute();

    assertThat((String)runtimeService.getVariable(processInstanceOne.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
    assertThat((String)runtimeService.getVariable(processInstanceTwo.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_TWO);
  }

  @Test
  void testEvaluateDecisionWithLatestBindingDifferentVersions() {

    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decision")
          .operatonDecisionRefBinding("latest")
          .operatonMapDecisionResult("singleEntry")
          .operatonResultVariable("decisionVar")
        .operatonAsyncAfter()
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, process, DMN_FILE);

    testRule.deployForTenant(TENANT_TWO, process, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE_VERSION_TWO);

    ProcessInstance processInstanceOne = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold")
      .processDefinitionTenantId(TENANT_ONE).execute();

    ProcessInstance processInstanceTwo = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold")
      .processDefinitionTenantId(TENANT_TWO).execute();

    assertThat((String)runtimeService.getVariable(processInstanceOne.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
    assertThat((String)runtimeService.getVariable(processInstanceTwo.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_TWO);
  }

  @Test
  void testEvaluateDecisionWithVersionBinding() {

    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decision")
          .operatonDecisionRefBinding("version")
          .operatonDecisionRefVersion("1")
          .operatonMapDecisionResult("singleEntry")
          .operatonResultVariable("decisionVar")
        .operatonAsyncAfter()
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, process, DMN_FILE);
    testRule.deployForTenant(TENANT_ONE, DMN_FILE_VERSION_TWO);

    testRule.deployForTenant(TENANT_TWO, process, DMN_FILE_VERSION_TWO);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE);

    ProcessInstance processInstanceOne = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold")
      .processDefinitionTenantId(TENANT_ONE).execute();

    ProcessInstance processInstanceTwo = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold")
      .processDefinitionTenantId(TENANT_TWO).execute();

    assertThat((String)runtimeService.getVariable(processInstanceOne.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
    assertThat((String)runtimeService.getVariable(processInstanceTwo.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_TWO);
  }

  @Test
  void testEvaluateDecisionWithVersionTagBinding() {
    // given
    testRule.deployForTenant(TENANT_ONE, DMN_FILE_VERSION_TAG);
    testRule.deploy(Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decision")
          .operatonDecisionRefTenantId(TENANT_ONE)
          .operatonDecisionRefBinding("versionTag")
          .operatonDecisionRefVersionTag("0.0.2")
          .operatonMapDecisionResult("singleEntry")
          .operatonResultVariable("decisionVar")
        .endEvent()
          .operatonAsyncBefore()
        .done());

    // when
    ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("process")
        .setVariable("status", "gold")
        .execute();

    // then
    assertThat((String)runtimeService.getVariable(processInstance.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_TAG_ONE);
  }

  @Test
  void testEvaluateDecisionWithVersionTagBinding_ResolveTenantFromDefinition() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decision")
          .operatonDecisionRefBinding("versionTag")
          .operatonDecisionRefVersionTag("0.0.2")
          .operatonMapDecisionResult("singleEntry")
          .operatonResultVariable("decisionVar")
        .endEvent()
          .operatonAsyncBefore()
        .done();

    testRule.deployForTenant(TENANT_ONE, process, DMN_FILE_VERSION_TAG);
    testRule.deployForTenant(TENANT_TWO, process, DMN_FILE_VERSION_TAG_TWO);

    ProcessInstance processInstanceOne = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold")
      .processDefinitionTenantId(TENANT_ONE).execute();

    ProcessInstance processInstanceTwo = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold")
      .processDefinitionTenantId(TENANT_TWO).execute();

    assertThat((String)runtimeService.getVariable(processInstanceOne.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_TAG_ONE);
    assertThat((String)runtimeService.getVariable(processInstanceTwo.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_TAG_TWO);
  }

  @Test
  void testFailEvaluateDecisionFromOtherTenantWithDeploymentBinding() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decision")
          .operatonDecisionRefBinding("deployment")
        .operatonAsyncAfter()
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, process);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("process")
        .processDefinitionTenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(processInstantiationBuilder::execute)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("no decision definition deployed with key = 'decision'");
  }

  @Test
  void testFailEvaluateDecisionFromOtherTenantWithLatestBinding() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decision")
          .operatonDecisionRefBinding("latest")
        .operatonAsyncAfter()
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, process);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("process")
        .processDefinitionTenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(processInstantiationBuilder::execute)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("no decision definition deployed with key 'decision'");
  }

  @Test
  void testFailEvaluateDecisionFromOtherTenantWithVersionBinding() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decision")
          .operatonDecisionRefBinding("version")
          .operatonDecisionRefVersion("2")
        .operatonAsyncAfter()
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, process, DMN_FILE);

    testRule.deployForTenant(TENANT_TWO, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("process")
        .processDefinitionTenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(processInstantiationBuilder::execute)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("no decision definition deployed with key = 'decision', version = '2' and tenant-id = 'tenant1'");
  }

  @Test
  void testFailEvaluateDecisionFromOtherTenantWithVersionTagBinding() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
        .operatonDecisionRef("decision")
        .operatonDecisionRefBinding("versionTag")
        .operatonDecisionRefVersionTag("0.0.2")
        .operatonMapDecisionResult("singleEntry")
        .operatonResultVariable("result")
        .operatonAsyncAfter()
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, process);

    testRule.deployForTenant(TENANT_TWO, DMN_FILE_VERSION_TAG);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("process")
        .processDefinitionTenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(processInstantiationBuilder::execute)
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("no decision definition deployed with key = 'decision', versionTag = '0.0.2' and tenant-id = 'tenant1': decisionDefinition is null");
  }

  @Test
  void testEvaluateDecisionTenantIdConstant() {

    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decision")
          .operatonDecisionRefBinding("latest")
          .operatonDecisionRefTenantId(TENANT_ONE)
          .operatonMapDecisionResult("singleEntry")
          .operatonResultVariable("decisionVar")
        .operatonAsyncAfter()
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE_VERSION_TWO);
   testRule.deploy(process);

    ProcessInstance processInstanceOne = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold").execute();

    assertThat((String)runtimeService.getVariable(processInstanceOne.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
  }

  @Test
  void testEvaluateDecisionWithoutTenantIdConstant() {

    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decision")
          .operatonDecisionRefBinding("latest")
          .operatonDecisionRefTenantId("${null}")
          .operatonMapDecisionResult("singleEntry")
          .operatonResultVariable("decisionVar")
        .operatonAsyncAfter()
        .endEvent()
        .done();

   testRule.deploy(DMN_FILE);
    testRule.deployForTenant(TENANT_ONE, process);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE_VERSION_TWO);

    ProcessInstance processInstanceOne = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold").execute();

    assertThat((String)runtimeService.getVariable(processInstanceOne.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
  }

  @Test
  void testEvaluateDecisionTenantIdExpression() {

    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
        .startEvent()
        .businessRuleTask()
          .operatonDecisionRef("decision")
          .operatonDecisionRefBinding("latest")
          .operatonDecisionRefTenantId("${'"+TENANT_ONE+"'}")
          .operatonMapDecisionResult("singleEntry")
          .operatonResultVariable("decisionVar")
        .operatonAsyncAfter()
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE_VERSION_TWO);
   testRule.deploy(process);

    ProcessInstance processInstanceOne = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold").execute();

    assertThat((String)runtimeService.getVariable(processInstanceOne.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
  }

  @Test
  void testEvaluateDecisionTenantIdCompositeExpression() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("process")
      .startEvent()
      .businessRuleTask()
      .operatonDecisionRef("decision")
      .operatonDecisionRefBinding("latest")
      .operatonDecisionRefTenantId("tenant${'1'}")
      .operatonMapDecisionResult("singleEntry")
      .operatonResultVariable("decisionVar")
      .operatonAsyncAfter()
      .endEvent()
      .done();
    testRule.deployForTenant(TENANT_ONE, DMN_FILE);
    testRule.deployForTenant(TENANT_TWO, DMN_FILE_VERSION_TWO);
   testRule.deploy(process);

    // when
    ProcessInstance processInstanceOne = runtimeService.createProcessInstanceByKey("process")
      .setVariable("status", "gold").execute();

    // then
    assertThat((String)runtimeService.getVariable(processInstanceOne.getId(), "decisionVar")).isEqualTo(RESULT_OF_VERSION_ONE);
  }

}
