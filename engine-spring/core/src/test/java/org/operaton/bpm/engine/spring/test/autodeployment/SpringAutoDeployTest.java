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
package org.operaton.bpm.engine.spring.test.autodeployment;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.*;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
class SpringAutoDeployTest {

  protected static final String CTX_PATH
    = "org/operaton/bpm/engine/spring/test/autodeployment/SpringAutoDeployTest-context.xml";
  protected static final String CTX_CREATE_DROP_CLEAN_DB
    = "org/operaton/bpm/engine/spring/test/autodeployment/SpringAutoDeployTest-create-drop-clean-db-context.xml";
  protected static final String CTX_DYNAMIC_DEPLOY_PATH
  = "org/operaton/bpm/engine/spring/test/autodeployment/SpringAutoDeployTest-dynamic-deployment-context.xml";

  protected static final String CTX_CMMN_PATH
    = "org/operaton/bpm/engine/spring/test/autodeployment/SpringAutoDeployCmmnTest-context.xml";

  protected static final String CTX_CMMN_BPMN_TOGETHER_PATH
      = "org/operaton/bpm/engine/spring/test/autodeployment/SpringAutoDeployCmmnBpmnTest-context.xml";

  protected static final String CTX_DEPLOY_CHANGE_ONLY_PATH
      = "org/operaton/bpm/engine/spring/test/autodeployment/SpringAutoDeployDeployChangeOnlyTest-context.xml";

  protected static final String CTX_TENANT_ID_PATH
      = "org/operaton/bpm/engine/spring/test/autodeployment/SpringAutoDeployTenantIdTest-context.xml";

  protected static final String CTX_CUSTOM_NAME_PATH
      = "org/operaton/bpm/engine/spring/test/autodeployment/SpringAutoDeployCustomNameTest-context.xml";


  protected ClassPathXmlApplicationContext applicationContext;
  protected RepositoryService repositoryService;

  protected void createAppContext(String path) {
    this.applicationContext = new ClassPathXmlApplicationContext(path);
    this.repositoryService = applicationContext.getBean(RepositoryService.class);
  }

  @AfterEach
  void tearDown() {
    DynamicResourceProducer.clearResources();
    removeAllDeployments();
    this.applicationContext.close();
    this.applicationContext = null;
    this.repositoryService = null;
  }

  @Test
  void basicActivitiSpringIntegration() {
    createAppContext(CTX_PATH);
    List<ProcessDefinition> processDefinitions = repositoryService
      .createProcessDefinitionQuery()
      .list();

    Set<String> processDefinitionKeys = new HashSet<>();
    for (ProcessDefinition processDefinition: processDefinitions) {
      processDefinitionKeys.add(processDefinition.getKey());
    }

    Set<String> expectedProcessDefinitionKeys = new HashSet<>();
    expectedProcessDefinitionKeys.add("a");
    expectedProcessDefinitionKeys.add("b");
    expectedProcessDefinitionKeys.add("c");

    assertThat(processDefinitionKeys).isEqualTo(expectedProcessDefinitionKeys);
  }

  @Test
  void noRedeploymentForSpringContainerRestart() {
    createAppContext(CTX_PATH);
    DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();
    assertThat(deploymentQuery.count()).isEqualTo(1);
    ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
    assertThat(processDefinitionQuery.count()).isEqualTo(3);

    // Creating a new app context with same resources doesn't lead to more deployments
    applicationContext.close();
    applicationContext = new ClassPathXmlApplicationContext(CTX_PATH);
    assertThat(deploymentQuery.count()).isEqualTo(1);
    assertThat(processDefinitionQuery.count()).isEqualTo(3);
  }

  @Test
  void autoDeployCmmn() {
    createAppContext(CTX_CMMN_PATH);

    List<CaseDefinition> definitions = repositoryService.createCaseDefinitionQuery().list();

    assertThat(definitions).hasSize(1);
  }

  @Test
  void autoDeployCmmnAndBpmnTogether() {
    createAppContext(CTX_CMMN_BPMN_TOGETHER_PATH);

    long caseDefs = repositoryService.createCaseDefinitionQuery().count();
    long procDefs = repositoryService.createProcessDefinitionQuery().count();

    assertThat(caseDefs).isEqualTo(1);
    assertThat(procDefs).isEqualTo(3);
  }

  // when deployChangeOnly=true, new deployment should be created only for the changed resources
  @Test
  void deployChangeOnly() {
    // given
    BpmnModelInstance model1 = Bpmn.createExecutableProcess("model1").startEvent("oldId").endEvent().done();
    BpmnModelInstance model2 = Bpmn.createExecutableProcess("model1").startEvent("newId").endEvent().done();
    BpmnModelInstance model3 = Bpmn.createExecutableProcess("model2").startEvent().endEvent().done();

    DynamicResourceProducer.addResource("a.bpmn", model1);
    DynamicResourceProducer.addResource("b.bpmn", model3);

    createAppContext(CTX_DEPLOY_CHANGE_ONLY_PATH);

    // assume
    assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);

    // when
    applicationContext.close();

    DynamicResourceProducer.clearResources();
    DynamicResourceProducer.addResource("a.bpmn", model2);
    DynamicResourceProducer.addResource("b.bpmn", model3);

    applicationContext = new ClassPathXmlApplicationContext(CTX_DEPLOY_CHANGE_ONLY_PATH);
    repositoryService = (RepositoryService) applicationContext.getBean("repositoryService");

    // then
    assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3);
  }

  // Updating the bpmn20 file should lead to a new deployment when restarting the Spring container
  @Test
  void resourceRedeploymentAfterProcessDefinitionChange() {
    // given
    BpmnModelInstance model1 = Bpmn.createExecutableProcess("model1").startEvent("oldId").endEvent().done();
    BpmnModelInstance model2 = Bpmn.createExecutableProcess("model1").startEvent("newId").endEvent().done();
    BpmnModelInstance model3 = Bpmn.createExecutableProcess("model2").startEvent().endEvent().done();

    DynamicResourceProducer.addResource("a.bpmn", model1);
    DynamicResourceProducer.addResource("b.bpmn", model3);

    createAppContext(CTX_DYNAMIC_DEPLOY_PATH);
    assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
    applicationContext.close();

    // when
    DynamicResourceProducer.clearResources();
    DynamicResourceProducer.addResource("a.bpmn", model2);
    DynamicResourceProducer.addResource("b.bpmn", model3);

    applicationContext = new ClassPathXmlApplicationContext(CTX_DYNAMIC_DEPLOY_PATH);
    repositoryService = (RepositoryService) applicationContext.getBean("repositoryService");

    // then
    // Assertions come AFTER the file write! Otherwise, the process file is messed up if the assertions fail.
    assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(2);
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(4);
  }

  @Test
  void autoDeployWithCreateDropOnCleanDb() {
    createAppContext(CTX_CREATE_DROP_CLEAN_DB);
    assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(3);
  }

  @Test
  void autoDeployTenantId() {
    createAppContext(CTX_TENANT_ID_PATH);

    DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();

    assertThat(deploymentQuery.tenantIdIn("tenant1").count()).isEqualTo(1);
  }

  @Test
  void autoDeployWithoutTenantId() {
    createAppContext(CTX_CMMN_BPMN_TOGETHER_PATH);

    DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();

    assertThat(deploymentQuery.withoutTenantId().count()).isEqualTo(1);
  }

  @Test
  void autoDeployCustomName() {
    createAppContext(CTX_CUSTOM_NAME_PATH);

    assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(1);
  }

  // --Helper methods ----------------------------------------------------------

  private void removeAllDeployments() {
    for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

}