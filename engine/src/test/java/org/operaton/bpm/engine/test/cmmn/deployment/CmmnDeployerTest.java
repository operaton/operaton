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
package org.operaton.bpm.engine.test.cmmn.deployment;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.repository.CaseDefinitionQuery;
import org.operaton.bpm.engine.repository.DeploymentQuery;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.operaton.bpm.model.cmmn.Cmmn;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.cmmn.instance.Case;
import org.operaton.bpm.model.cmmn.instance.CasePlanModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
class CmmnDeployerTest extends CmmnTest {

  @Test
  void testCmmnDeployment() {
    String deploymentId = processEngine
        .getRepositoryService()
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testSimpleDeployment.cmmn")
        .deploy()
        .getId();

    // there should be one deployment
    RepositoryService repositoryService = processEngine.getRepositoryService();
    DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();

    assertThat(deploymentQuery.count()).isOne();

    // there should be one case definition
    CaseDefinitionQuery query = processEngine.getRepositoryService().createCaseDefinitionQuery();
    assertThat(query.count()).isOne();

    CaseDefinition caseDefinition = query.singleResult();
    assertThat(caseDefinition.getKey()).isEqualTo("Case_1");

    processEngine.getRepositoryService().deleteDeployment(deploymentId);
  }

  @Test
  void testDeployTwoCasesWithDuplicateIdAtTheSameTime() {
    // given
    String cmmnResourceName1 = "org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testSimpleDeployment.cmmn";
    String cmmnResourceName2 = "org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testSimpleDeployment2.cmmn";
    var deploymentBuilder = repositoryService.createDeployment()
              .addClasspathResource(cmmnResourceName1)
              .addClasspathResource(cmmnResourceName2)
              .name("duplicateAtTheSameTime");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(Exception.class);

    // Verify that nothing is deployed
    assertThat(repositoryService.createDeploymentQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testCaseDiagramResource.cmmn",
      "org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testCaseDiagramResource.png"})
  @Test
  void testCaseDiagramResource() {
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    final CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();

    assertThat(caseDefinition.getResourceName()).isEqualTo("org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testCaseDiagramResource.cmmn");
    assertThat(caseDefinition.getKey()).isEqualTo("Case_1");

    final String diagramResourceName = caseDefinition.getDiagramResourceName();
    assertThat(diagramResourceName).isEqualTo("org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testCaseDiagramResource.png");

    final InputStream diagramStream = repositoryService.getResourceAsStream(deploymentId,
        "org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testCaseDiagramResource.png");
    final byte[] diagramBytes = IoUtil.readInputStream(diagramStream, "diagram stream");
    assertThat(diagramBytes).hasSize(2540);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testMultipleDiagramResourcesProvided.cmmn",
      "org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testMultipleDiagramResourcesProvided.a.png",
      "org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testMultipleDiagramResourcesProvided.b.png",
      "org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testMultipleDiagramResourcesProvided.c.png"})
  @Test
  void testMultipleDiagramResourcesProvided() {
    final CaseDefinition caseA = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("a").singleResult();
    final CaseDefinition caseB = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("b").singleResult();
    final CaseDefinition caseC = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("c").singleResult();

    assertThat(caseA.getDiagramResourceName()).isEqualTo("org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testMultipleDiagramResourcesProvided.a.png");
    assertThat(caseB.getDiagramResourceName()).isEqualTo("org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testMultipleDiagramResourcesProvided.b.png");
    assertThat(caseC.getDiagramResourceName()).isEqualTo("org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testMultipleDiagramResourcesProvided.c.png");
  }

  @Test
  void testDeployCmmn10XmlFile() {
    verifyCmmnResourceDeployed("org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testDeployCmmn10XmlFile.cmmn10.xml");

  }

  @Test
  void testDeployCmmn11XmlFile() {
    verifyCmmnResourceDeployed("org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testDeployCmmn11XmlFile.cmmn11.xml");
  }

  protected void verifyCmmnResourceDeployed(String resourcePath) {
    String deploymentId = processEngine
        .getRepositoryService()
        .createDeployment()
        .addClasspathResource(resourcePath)
        .deploy()
        .getId();

    // there should be one deployment
    RepositoryService repositoryService = processEngine.getRepositoryService();
    DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();

    assertThat(deploymentQuery.count()).isOne();

    // there should be one case definition
    CaseDefinitionQuery query = processEngine.getRepositoryService().createCaseDefinitionQuery();
    assertThat(query.count()).isOne();

    CaseDefinition caseDefinition = query.singleResult();
    assertThat(caseDefinition.getKey()).isEqualTo("Case_1");

    processEngine.getRepositoryService().deleteDeployment(deploymentId);

  }

  @Test
  void testDeployCmmnModelInstance() {
    // given
    CmmnModelInstance modelInstance = createCmmnModelInstance();

    // when
    testRule.deploy(repositoryService.createDeployment().addModelInstance("foo.cmmn", modelInstance));

    // then
    assertThat(repositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("foo.cmmn").singleResult()).isNotNull();
  }

  protected static CmmnModelInstance createCmmnModelInstance() {
    final CmmnModelInstance modelInstance = Cmmn.createEmptyModel();
    org.operaton.bpm.model.cmmn.instance.Definitions definitions = modelInstance.newInstance(org.operaton.bpm.model.cmmn.instance.Definitions.class);
    definitions.setTargetNamespace("http://operaton.org/examples");
    modelInstance.setDefinitions(definitions);

    Case caseElement = modelInstance.newInstance(Case.class);
    caseElement.setId("a-case");
    definitions.addChildElement(caseElement);

    CasePlanModel casePlanModel = modelInstance.newInstance(CasePlanModel.class);
    caseElement.setCasePlanModel(casePlanModel);

    Cmmn.writeModelToStream(System.out, modelInstance);

    return modelInstance;
  }

  @Test
  void testDeployAndGetCaseDefinition() {
    // given case model
    final CmmnModelInstance modelInstance = createCmmnModelInstance();

    // when case model is deployed
    DeploymentWithDefinitions deployment = testRule.deploy(repositoryService.createDeployment()
      .addModelInstance("foo.cmmn", modelInstance));

    // then deployment contains deployed case definition
    List<CaseDefinition> deployedCaseDefinitions = deployment.getDeployedCaseDefinitions();
    assertThat(deployedCaseDefinitions).hasSize(1);
    assertThat(deployment.getDeployedProcessDefinitions()).isNull();
    assertThat(deployment.getDeployedDecisionDefinitions()).isNull();
    assertThat(deployment.getDeployedDecisionRequirementsDefinitions()).isNull();

    // and persisted case definition is equal to deployed case definition
    CaseDefinition persistedCaseDefinition = repositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("foo.cmmn").singleResult();
    assertThat(deployedCaseDefinitions.get(0).getId()).isEqualTo(persistedCaseDefinition.getId());
  }

  @Test
  void testDeployEmptyCaseDefinition() {

    // given empty case model
    final CmmnModelInstance modelInstance = Cmmn.createEmptyModel();
    org.operaton.bpm.model.cmmn.instance.Definitions definitions = modelInstance.newInstance(org.operaton.bpm.model.cmmn.instance.Definitions.class);
    definitions.setTargetNamespace("http://operaton.org/examples");
    modelInstance.setDefinitions(definitions);

    // when case model is deployed
    DeploymentWithDefinitions deployment = testRule.deploy(repositoryService.createDeployment()
      .addModelInstance("foo.cmmn", modelInstance));

    // then no case definition is deployed
    assertThat(deployment.getDeployedCaseDefinitions()).isNull();

    // and there exist not persisted case definition
    assertThat(repositoryService.createCaseDefinitionQuery().caseDefinitionResourceName("foo.cmmn").singleResult()).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testDeployCaseDefinitionWithIntegerHistoryTimeToLive.cmmn")
  @Test
  void testDeployCaseDefinitionWithIntegerHistoryTimeToLive() {
    CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();
    Integer historyTimeToLive = caseDefinition.getHistoryTimeToLive();
    assertThat(historyTimeToLive).isNotNull();
    assertThat((int) historyTimeToLive).isEqualTo(5);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testDeployCaseDefinitionWithStringHistoryTimeToLive.cmmn")
  @Test
  void testDeployCaseDefinitionWithStringHistoryTimeToLive() {
    CaseDefinition caseDefinition = repositoryService.createCaseDefinitionQuery().singleResult();
    Integer historyTimeToLive = caseDefinition.getHistoryTimeToLive();
    assertThat(historyTimeToLive).isNotNull();
    assertThat((int) historyTimeToLive).isEqualTo(5);
  }

  @Test
  void testDeployCaseDefinitionWithMalformedHistoryTimeToLive() {
    // when/then
    assertThatThrownBy(() -> testRule.deploy("org/operaton/bpm/engine/test/cmmn/deployment/CmmnDeploymentTest.testDeployCaseDefinitionWithMalformedHistoryTimeToLive.cmmn"))
      .isInstanceOf(ProcessEngineException.class)
      .cause()
      .hasMessageContaining("Cannot parse historyTimeToLive");
  }
}
