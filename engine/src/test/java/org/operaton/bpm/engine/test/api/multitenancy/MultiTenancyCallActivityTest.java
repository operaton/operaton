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

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.CaseInstanceQuery;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyCallActivityTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String CMMN = "org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testSimpleDeployment.cmmn";

  protected static final BpmnModelInstance SUB_PROCESS = Bpmn.createExecutableProcess("subProcess")
      .startEvent()
      .userTask()
      .endEvent()
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected CaseService caseService;

  @Test
  void testStartProcessInstanceWithDeploymentBinding() {

    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
      .startEvent()
      .callActivity()
        .calledElement("subProcess")
        .operatonCalledElementBinding("deployment")
      .endEvent()
      .done();

    testRule.deployForTenant(TENANT_ONE, SUB_PROCESS, callingProcess);
    testRule.deployForTenant(TENANT_TWO, SUB_PROCESS, callingProcess);

    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_TWO).execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testStartProcessInstanceWithLatestBindingSameVersion() {

    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
      .startEvent()
      .callActivity()
        .calledElement("subProcess")
        .operatonCalledElementBinding("latest")
      .endEvent()
      .done();

    testRule.deployForTenant(TENANT_ONE, SUB_PROCESS, callingProcess);
    testRule.deployForTenant(TENANT_TWO, SUB_PROCESS, callingProcess);

    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_TWO).execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testStartProcessInstanceWithLatestBindingDifferentVersion() {

    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
      .startEvent()
      .callActivity()
        .calledElement("subProcess")
        .operatonCalledElementBinding("latest")
      .endEvent()
      .done();

    testRule.deployForTenant(TENANT_ONE, SUB_PROCESS, callingProcess);

    testRule.deployForTenant(TENANT_TWO, SUB_PROCESS, callingProcess);
    testRule.deployForTenant(TENANT_TWO, SUB_PROCESS);

    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_TWO).execute();

    ProcessDefinition latestSubProcessTenantTwo = repositoryService.createProcessDefinitionQuery()
        .tenantIdIn(TENANT_TWO).processDefinitionKey("subProcess").latestVersion().singleResult();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).processDefinitionId(latestSubProcessTenantTwo.getId()).count()).isOne();
  }

  @Test
  void testStartProcessInstanceWithVersionBinding() {

    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
      .startEvent()
      .callActivity()
        .calledElement("subProcess")
        .operatonCalledElementBinding("version")
        .operatonCalledElementVersion("1")
      .endEvent()
      .done();

    testRule.deployForTenant(TENANT_ONE, SUB_PROCESS, callingProcess);
    testRule.deployForTenant(TENANT_TWO, SUB_PROCESS, callingProcess);

    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_TWO).execute();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testStartProcessInstanceWithVersionTagBinding() {
    // given
    BpmnModelInstance callingProcess = createCallingProcess("callingProcess", "ver_tag_1");

    testRule.deployForTenant(TENANT_ONE, callingProcess);
    testRule.deployForTenant(TENANT_ONE, "org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithVersionTag.bpmn20.xml");
    testRule.deployForTenant(TENANT_TWO, callingProcess);
    testRule.deployForTenant(TENANT_TWO, "org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithVersionTag2.bpmn20.xml");

    // when
    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_TWO).execute();

    // then
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess");
    assertThat(query.activityIdIn("Task_1").tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.activityIdIn("Task_2").tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testFailStartProcessInstanceFromOtherTenantWithDeploymentBinding() {
    // given
    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
      .startEvent()
      .callActivity()
        .calledElement("subProcess")
        .operatonCalledElementBinding("deployment")
      .endEvent()
      .done();

    testRule.deployForTenant(TENANT_ONE, callingProcess);
    testRule.deployForTenant(TENANT_TWO, SUB_PROCESS);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("callingProcess")
        .processDefinitionTenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(() -> processInstantiationBuilder.execute())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no processes deployed with key = 'subProcess'");
  }

  @Test
  void testFailStartProcessInstanceFromOtherTenantWithLatestBinding() {
    // given
    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
      .startEvent()
      .callActivity()
        .calledElement("subProcess")
        .operatonCalledElementBinding("latest")
      .endEvent()
      .done();

    testRule.deployForTenant(TENANT_ONE, callingProcess);
    testRule.deployForTenant(TENANT_TWO, SUB_PROCESS);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("callingProcess")
        .processDefinitionTenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(() -> processInstantiationBuilder.execute())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no processes deployed with key 'subProcess'");
  }

  @Test
  void testFailStartProcessInstanceFromOtherTenantWithVersionBinding() {
    // given
    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
      .startEvent()
      .callActivity()
        .calledElement("subProcess")
        .operatonCalledElementBinding("version")
        .operatonCalledElementVersion("2")
      .endEvent()
      .done();

    testRule.deployForTenant(TENANT_ONE, SUB_PROCESS, callingProcess);

    testRule.deployForTenant(TENANT_TWO, SUB_PROCESS);
    testRule.deployForTenant(TENANT_TWO, SUB_PROCESS);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("callingProcess")
        .processDefinitionTenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(() -> processInstantiationBuilder.execute())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no processes deployed with key = 'subProcess'");
  }

  @Test
  void testFailStartProcessInstanceFromOtherTenantWithVersionTagBinding() {
    // given
    BpmnModelInstance callingProcess = createCallingProcess("callingProcess", "ver_tag_2");
    testRule.deployForTenant(TENANT_ONE, callingProcess);
    testRule.deployForTenant(TENANT_TWO, "org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithVersionTag2.bpmn20.xml");
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("callingProcess")
        .processDefinitionTenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(() -> processInstantiationBuilder.execute())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no processes deployed with key = 'subProcess'");
  }

  @Test
  void testStartCaseInstanceWithDeploymentBinding() {

    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
      .startEvent()
      .callActivity()
        .operatonCaseRef("Case_1")
        .operatonCaseBinding("deployment")
      .endEvent()
      .done();

    testRule.deployForTenant(TENANT_ONE, callingProcess, CMMN);
    testRule.deployForTenant(TENANT_TWO, callingProcess, CMMN);

    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_TWO).execute();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery().caseDefinitionKey("Case_1");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testStartCaseInstanceWithLatestBindingSameVersion() {

    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
      .startEvent()
      .callActivity()
        .operatonCaseRef("Case_1")
        .operatonCaseBinding("latest")
      .endEvent()
      .done();

    testRule.deployForTenant(TENANT_ONE, callingProcess, CMMN);
    testRule.deployForTenant(TENANT_TWO, callingProcess, CMMN);

    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_TWO).execute();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery().caseDefinitionKey("Case_1");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testStartCaseInstanceWithLatestBindingDifferentVersion() {

    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
        .startEvent()
        .callActivity()
          .operatonCaseRef("Case_1")
          .operatonCaseBinding("latest")
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, callingProcess, CMMN);

    testRule.deployForTenant(TENANT_TWO, callingProcess, CMMN);
    testRule.deployForTenant(TENANT_TWO, CMMN);

    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_TWO).execute();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery().caseDefinitionKey("Case_1");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();

    CaseDefinition latestCaseDefinitionTenantTwo = repositoryService.createCaseDefinitionQuery().tenantIdIn(TENANT_TWO).latestVersion().singleResult();
    query = caseService.createCaseInstanceQuery().caseDefinitionId(latestCaseDefinitionTenantTwo.getId());
    assertThat(query.count()).isOne();
  }

  @Test
  void testStartCaseInstanceWithVersionBinding() {

    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
      .startEvent()
      .callActivity()
        .operatonCaseRef("Case_1")
        .operatonCaseBinding("version")
        .operatonCaseVersion("1")
      .endEvent()
      .done();

    testRule.deployForTenant(TENANT_ONE, callingProcess, CMMN);
    testRule.deployForTenant(TENANT_TWO, callingProcess, CMMN);

    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("callingProcess").processDefinitionTenantId(TENANT_TWO).execute();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery().caseDefinitionKey("Case_1");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testFailStartCaseInstanceFromOtherTenantWithDeploymentBinding() {
    // given
    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
        .startEvent()
        .callActivity()
          .operatonCaseRef("Case_1")
          .operatonCaseBinding("deployment")
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, callingProcess);
    testRule.deployForTenant(TENANT_TWO, CMMN);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("callingProcess")
        .processDefinitionTenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(() -> processInstantiationBuilder.execute())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no case definition deployed with key = 'Case_1'");
  }

  @Test
  void testFailStartCaseInstanceFromOtherTenantWithLatestBinding() {
    // given
    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
        .startEvent()
        .callActivity()
          .operatonCaseRef("Case_1")
          .operatonCaseBinding("latest")
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, callingProcess);
    testRule.deployForTenant(TENANT_TWO, CMMN);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("callingProcess")
        .processDefinitionTenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(() -> processInstantiationBuilder.execute())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no case definition deployed with key 'Case_1'");
  }

  @Test
  void testFailStartCaseInstanceFromOtherTenantWithVersionBinding() {
    // given
    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
        .startEvent()
        .callActivity()
          .operatonCaseRef("Case_1")
          .operatonCaseBinding("version")
          .operatonCaseVersion("2")
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, callingProcess, CMMN);

    testRule.deployForTenant(TENANT_TWO, CMMN);
    testRule.deployForTenant(TENANT_TWO, CMMN);
    var processInstantiationBuilder = runtimeService.createProcessInstanceByKey("callingProcess")
        .processDefinitionTenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(() -> processInstantiationBuilder.execute())
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("no case definition deployed with key = 'Case_1'");
  }

  @Test
  void testCalledElementTenantIdConstant() {

    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
        .startEvent()
        .callActivity()
          .calledElement("subProcess")
          .operatonCalledElementTenantId(TENANT_ONE)
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, SUB_PROCESS);
   testRule.deploy(callingProcess);

    runtimeService.startProcessInstanceByKey("callingProcess");

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void testCalledElementTenantIdExpression() {

    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
        .startEvent()
        .callActivity()
          .calledElement("subProcess")
          .operatonCalledElementTenantId("${'"+TENANT_ONE+"'}")
        .endEvent()
        .done();

    testRule.deployForTenant(TENANT_ONE, SUB_PROCESS);
   testRule.deploy(callingProcess);

    runtimeService.startProcessInstanceByKey("callingProcess");

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void testCaseRefTenantIdConstant() {

    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
      .startEvent()
      .callActivity()
        .operatonCaseRef("Case_1")
        .operatonCaseTenantId(TENANT_ONE)
      .endEvent()
      .done();

    testRule.deployForTenant(TENANT_ONE, CMMN);
   testRule.deploy(callingProcess);

    runtimeService.startProcessInstanceByKey("callingProcess");

    CaseInstanceQuery query = caseService.createCaseInstanceQuery().caseDefinitionKey("Case_1");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();

  }

  @Test
  void testCaseRefTenantIdExpression() {

    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
      .startEvent()
      .callActivity()
        .operatonCaseRef("Case_1")
        .operatonCaseTenantId("${'"+TENANT_ONE+"'}")
      .endEvent()
      .done();

    testRule.deployForTenant(TENANT_ONE, CMMN);
   testRule.deploy(callingProcess);

    runtimeService.startProcessInstanceByKey("callingProcess");

    CaseInstanceQuery query = caseService.createCaseInstanceQuery().caseDefinitionKey("Case_1");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void testCaseRefTenantIdCompositeExpression() {
    // given
    BpmnModelInstance callingProcess = Bpmn.createExecutableProcess("callingProcess")
      .startEvent()
      .callActivity()
      .operatonCaseRef("Case_1")
      .operatonCaseTenantId("tenant${'1'}")
      .endEvent()
      .done();

    testRule.deployForTenant(TENANT_ONE, CMMN);
   testRule.deploy(callingProcess);

    // when
    runtimeService.startProcessInstanceByKey("callingProcess");

    // then
    CaseInstanceQuery query = caseService.createCaseInstanceQuery().caseDefinitionKey("Case_1");
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  protected BpmnModelInstance createCallingProcess(String processId, String versionTagValue) {
    return Bpmn.createExecutableProcess(processId)
        .startEvent()
        .callActivity()
          .calledElement("subProcess")
          .operatonCalledElementBinding("versionTag")
          .operatonCalledElementVersionTag(versionTagValue)
        .endEvent()
        .done();
  }

}
