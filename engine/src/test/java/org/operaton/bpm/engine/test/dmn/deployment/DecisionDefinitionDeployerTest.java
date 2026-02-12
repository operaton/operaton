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
package org.operaton.bpm.engine.test.dmn.deployment;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.repository.DecisionDefinitionQuery;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinitionQuery;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.repository.DeploymentQuery;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.dmn.Dmn;
import org.operaton.bpm.model.dmn.DmnModelInstance;
import org.operaton.bpm.model.dmn.HitPolicy;
import org.operaton.bpm.model.dmn.impl.DmnModelConstants;
import org.operaton.bpm.model.dmn.instance.Decision;
import org.operaton.bpm.model.dmn.instance.DecisionTable;
import org.operaton.bpm.model.dmn.instance.Definitions;
import org.operaton.bpm.model.dmn.instance.Input;
import org.operaton.bpm.model.dmn.instance.InputExpression;
import org.operaton.bpm.model.dmn.instance.Output;
import org.operaton.bpm.model.dmn.instance.Text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecisionDefinitionDeployerTest {

  protected static final String DMN_CHECK_ORDER_RESOURCE = "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testDmnDeployment.dmn11.xml";
  protected static final String DMN_CHECK_ORDER_RESOURCE_DMN_SUFFIX = "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testDmnDeployment.dmn";
  protected static final String DMN_SCORE_RESOURCE = "org/operaton/bpm/engine/test/dmn/deployment/dmnScore.dmn11.xml";

  protected static final String DMN_DECISION_LITERAL_EXPRESSION = "org/operaton/bpm/engine/test/dmn/deployment/DecisionWithLiteralExpression.dmn";

  protected static final String DMN_DECISION_LEGACY = "org/operaton/bpm/engine/test/dmn/deployment/Example.Legacy.dmn";

  protected static final String DRD_SCORE_RESOURCE = "org/operaton/bpm/engine/test/dmn/deployment/drdScore.dmn11.xml";
  protected static final String DRD_SCORE_V2_RESOURCE = "org/operaton/bpm/engine/test/dmn/deployment/drdScore_v2.dmn11.xml";
  protected static final String DRD_DISH_RESOURCE = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml";

  @RegisterExtension
  static ProcessEngineExtension processEngineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(processEngineRule);

  RepositoryService repositoryService;

  @Test
  void dmnDeployment() {
    String deploymentId = testRule.deploy(DMN_CHECK_ORDER_RESOURCE).getId();

    // there should be decision deployment
    DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();

    assertThat(deploymentQuery.count()).isOne();

    // there should be one decision definition
    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();
    assertThat(query.count()).isOne();

    DecisionDefinition decisionDefinition = query.singleResult();

    assertThat(decisionDefinition.getId()).startsWith("decision:1:");
    assertThat(decisionDefinition.getCategory()).isEqualTo("http://operaton.org/schema/1.0/dmn");
    assertThat(decisionDefinition.getName()).isEqualTo("CheckOrder");
    assertThat(decisionDefinition.getKey()).isEqualTo("decision");
    assertThat(decisionDefinition.getVersion()).isEqualTo(1);
    assertThat(decisionDefinition.getResourceName()).isEqualTo(DMN_CHECK_ORDER_RESOURCE);
    assertThat(decisionDefinition.getDeploymentId()).isEqualTo(deploymentId);
    assertThat(decisionDefinition.getDiagramResourceName()).isNull();
  }

  @Test
  void dmnDeploymentWithDmnSuffix() {
    String deploymentId = testRule.deploy(DMN_CHECK_ORDER_RESOURCE_DMN_SUFFIX).getId();

    // there should be one deployment
    DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();

    assertThat(deploymentQuery.count()).isOne();

    // there should be one case definition
    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();
    assertThat(query.count()).isOne();

    DecisionDefinition decisionDefinition = query.singleResult();

    assertThat(decisionDefinition.getId()).startsWith("decision:1:");
    assertThat(decisionDefinition.getCategory()).isEqualTo("http://operaton.org/schema/1.0/dmn");
    assertThat(decisionDefinition.getName()).isEqualTo("CheckOrder");
    assertThat(decisionDefinition.getKey()).isEqualTo("decision");
    assertThat(decisionDefinition.getVersion()).isEqualTo(1);
    assertThat(decisionDefinition.getResourceName()).isEqualTo(DMN_CHECK_ORDER_RESOURCE_DMN_SUFFIX);
    assertThat(decisionDefinition.getDeploymentId()).isEqualTo(deploymentId);
    assertThat(decisionDefinition.getDiagramResourceName()).isNull();
  }

  @Test
  void dmnDeploymentWithDecisionLiteralExpression() {
    String deploymentId = testRule.deploy(DMN_DECISION_LITERAL_EXPRESSION).getId();

    // there should be decision deployment
    DeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();
    assertThat(deploymentQuery.count()).isOne();

    // there should be one decision definition
    DecisionDefinitionQuery query = repositoryService.createDecisionDefinitionQuery();
    assertThat(query.count()).isOne();

    DecisionDefinition decisionDefinition = query.singleResult();

    assertThat(decisionDefinition.getId()).startsWith("decisionLiteralExpression:1:");
    assertThat(decisionDefinition.getCategory()).isEqualTo("http://operaton.org/schema/1.0/dmn");
    assertThat(decisionDefinition.getKey()).isEqualTo("decisionLiteralExpression");
    assertThat(decisionDefinition.getName()).isEqualTo("Decision with Literal Expression");
    assertThat(decisionDefinition.getVersion()).isEqualTo(1);
    assertThat(decisionDefinition.getResourceName()).isEqualTo(DMN_DECISION_LITERAL_EXPRESSION);
    assertThat(decisionDefinition.getDeploymentId()).isEqualTo(deploymentId);
    assertThat(decisionDefinition.getDiagramResourceName()).isNull();
  }

  @Test
  void dmnDeploymentWithLegacyDmnDefinition() {
    String deploymentId = testRule.deploy(DMN_DECISION_LEGACY).getId();

    assertThat(deploymentId).isNotNull();
  }

  @Deployment
  @Test
  void longDecisionDefinitionKey() {
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().singleResult();

    assertThat(decisionDefinition.getId()).doesNotStartWith("o123456789");
    assertThat(decisionDefinition.getKey()).isEqualTo("o123456789o123456789o123456789o123456789o123456789o123456789o123456789");
  }

  @Test
  void duplicateIdInDeployment() {
    String resourceName1 = "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testDuplicateIdInDeployment.dmn11.xml";
    String resourceName2 = "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testDuplicateIdInDeployment2.dmn11.xml";

    // when/then
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment()
      .addClasspathResource(resourceName1)
      .addClasspathResource(resourceName2)
      .name("duplicateIds");
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("duplicateDecision");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testDecisionDiagramResource.dmn11.xml",
      "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testDecisionDiagramResource.png"
  })
  @Test
  void getDecisionDiagramResource() {
    String resourcePrefix = "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testDecisionDiagramResource";

    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().singleResult();

    assertThat(decisionDefinition.getResourceName()).isEqualTo(resourcePrefix + ".dmn11.xml");
    assertThat(decisionDefinition.getKey()).isEqualTo("decision");

    String diagramResourceName = decisionDefinition.getDiagramResourceName();
    assertThat(diagramResourceName).isEqualTo(resourcePrefix + ".png");

    InputStream diagramStream = repositoryService.getResourceAsStream(decisionDefinition.getDeploymentId(), diagramResourceName);
    final byte[] diagramBytes = IoUtil.readInputStream(diagramStream, "diagram stream");
    assertThat(diagramBytes).hasSize(2540);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testMultipleDecisionDiagramResource.dmn11.xml",
      "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testMultipleDecisionDiagramResource.decision1.png",
      "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testMultipleDecisionDiagramResource.decision2.png",
      "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testMultipleDecisionDiagramResource.decision3.png"
  })
  @Test
  void multipleDiagramResourcesProvided() {
    String resourcePrefix = "org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testMultipleDecisionDiagramResource.";

    DecisionDefinitionQuery decisionDefinitionQuery = repositoryService.createDecisionDefinitionQuery();
    assertThat(decisionDefinitionQuery.count()).isEqualTo(3);

    for (DecisionDefinition decisionDefinition : decisionDefinitionQuery.list()) {
      assertThat(decisionDefinition.getDiagramResourceName()).isEqualTo(resourcePrefix + decisionDefinition.getKey() + ".png");
    }
  }

  @Test
  void drdDeployment() {
    String deploymentId = testRule.deploy(DRD_SCORE_RESOURCE).getId();

    // there should be one decision requirements definition
    DecisionRequirementsDefinitionQuery query = repositoryService.createDecisionRequirementsDefinitionQuery();
    assertThat(query.count()).isOne();

    DecisionRequirementsDefinition decisionRequirementsDefinition = query.singleResult();

    assertThat(decisionRequirementsDefinition.getId()).startsWith("score:1:");
    assertThat(decisionRequirementsDefinition.getKey()).isEqualTo("score");
    assertThat(decisionRequirementsDefinition.getName()).isEqualTo("Score");
    assertThat(decisionRequirementsDefinition.getCategory()).isEqualTo("test-drd-1");
    assertThat(decisionRequirementsDefinition.getVersion()).isEqualTo(1);
    assertThat(decisionRequirementsDefinition.getResourceName()).isEqualTo(DRD_SCORE_RESOURCE);
    assertThat(decisionRequirementsDefinition.getDeploymentId()).isEqualTo(deploymentId);
    assertThat(decisionRequirementsDefinition.getDiagramResourceName()).isNull();

    // both decisions should have a reference to the decision requirements definition
    List<DecisionDefinition> decisions = repositoryService.createDecisionDefinitionQuery().orderByDecisionDefinitionKey().asc().list();
    assertThat(decisions).hasSize(2);

    DecisionDefinition firstDecision = decisions.get(0);
    assertThat(firstDecision.getKey()).isEqualTo("score-decision");
    assertThat(firstDecision.getDecisionRequirementsDefinitionId()).isEqualTo(decisionRequirementsDefinition.getId());
    assertThat(firstDecision.getDecisionRequirementsDefinitionKey()).isEqualTo("score");

    DecisionDefinition secondDecision = decisions.get(1);
    assertThat(secondDecision.getKey()).isEqualTo("score-result");
    assertThat(secondDecision.getDecisionRequirementsDefinitionId()).isEqualTo(decisionRequirementsDefinition.getId());
    assertThat(secondDecision.getDecisionRequirementsDefinitionKey()).isEqualTo("score");
  }

  @Deployment(resources = DMN_CHECK_ORDER_RESOURCE)
  @Test
  void noDrdForSingleDecisionDeployment() {
    // when the DMN file contains only a single decision definition
    assertThat(repositoryService.createDecisionDefinitionQuery().count()).isOne();

    // then no decision requirements definition should be created
    assertThat(repositoryService.createDecisionRequirementsDefinitionQuery().count()).isZero();
    // and the decision should not be linked to a decision requirements definition
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().singleResult();
    assertThat(decisionDefinition.getDecisionRequirementsDefinitionId()).isNull();
    assertThat(decisionDefinition.getDecisionRequirementsDefinitionKey()).isNull();
  }

  @Deployment(resources = {DRD_SCORE_RESOURCE, DRD_DISH_RESOURCE})
  @Test
  void multipleDrdDeployment() {
    // there should be two decision requirements definitions
    List<DecisionRequirementsDefinition> decisionRequirementsDefinitions = repositoryService
        .createDecisionRequirementsDefinitionQuery()
        .orderByDecisionRequirementsDefinitionCategory()
        .asc()
        .list();

    assertThat(decisionRequirementsDefinitions).hasSize(2);
    assertThat(decisionRequirementsDefinitions.get(0).getKey()).isEqualTo("score");
    assertThat(decisionRequirementsDefinitions.get(1).getKey()).isEqualTo("dish");

    // the decisions should have a reference to the decision requirements definition
    List<DecisionDefinition> decisions = repositoryService.createDecisionDefinitionQuery().orderByDecisionDefinitionCategory().asc().list();
    assertThat(decisions).hasSize(5);
    assertThat(decisions.get(0).getDecisionRequirementsDefinitionId()).isEqualTo(decisionRequirementsDefinitions.get(0).getId());
    assertThat(decisions.get(1).getDecisionRequirementsDefinitionId()).isEqualTo(decisionRequirementsDefinitions.get(0).getId());
    assertThat(decisions.get(2).getDecisionRequirementsDefinitionId()).isEqualTo(decisionRequirementsDefinitions.get(1).getId());
    assertThat(decisions.get(3).getDecisionRequirementsDefinitionId()).isEqualTo(decisionRequirementsDefinitions.get(1).getId());
    assertThat(decisions.get(4).getDecisionRequirementsDefinitionId()).isEqualTo(decisionRequirementsDefinitions.get(1).getId());
  }

  @Test
  void duplicateDrdIdInDeployment() {

    // when/then
    var deploymentBuilder = repositoryService.createDeployment()
      .addClasspathResource(DRD_SCORE_RESOURCE)
      .addClasspathResource(DRD_SCORE_V2_RESOURCE)
      .name("duplicateIds");
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("definitions");
  }

  @Test
  void deployMultipleDecisionsWithSameDrdId() {
    // when deploying two decision with the same drd id `definitions`
    testRule.deploy(DMN_SCORE_RESOURCE, DMN_CHECK_ORDER_RESOURCE);

    // then create two decision definitions and
    // ignore the duplicated drd id since no drd is created
    assertThat(repositoryService.createDecisionDefinitionQuery().count()).isEqualTo(2);
    assertThat(repositoryService.createDecisionRequirementsDefinitionQuery().count()).isZero();
  }

  @Test
  void deployDecisionIndependentFromDrd() {
    String deploymentIdDecision = testRule.deploy(DMN_SCORE_RESOURCE).getId();
    String deploymentIdDrd = testRule.deploy(DRD_SCORE_RESOURCE).getId();

    // there should be one decision requirements definition
    DecisionRequirementsDefinitionQuery query = repositoryService.createDecisionRequirementsDefinitionQuery();
    assertThat(query.count()).isOne();

    DecisionRequirementsDefinition decisionRequirementsDefinition = query.singleResult();
    assertThat(decisionRequirementsDefinition.getVersion()).isEqualTo(1);
    assertThat(decisionRequirementsDefinition.getDeploymentId()).isEqualTo(deploymentIdDrd);

    // and two deployed decisions with different versions
    List<DecisionDefinition> decisions = repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey("score-decision")
        .orderByDecisionDefinitionVersion().asc()
        .list();

    assertThat(decisions).hasSize(2);

    DecisionDefinition firstDecision = decisions.get(0);
    assertThat(firstDecision.getVersion()).isEqualTo(1);
    assertThat(firstDecision.getDeploymentId()).isEqualTo(deploymentIdDecision);
    assertThat(firstDecision.getDecisionRequirementsDefinitionId()).isNull();

    DecisionDefinition secondDecision = decisions.get(1);
    assertThat(secondDecision.getVersion()).isEqualTo(2);
    assertThat(secondDecision.getDeploymentId()).isEqualTo(deploymentIdDrd);
    assertThat(secondDecision.getDecisionRequirementsDefinitionId()).isEqualTo(decisionRequirementsDefinition.getId());
  }

  @Test
  void testDeployDmnModelInstance() {
    // given
    DmnModelInstance dmnModelInstance = createDmnModelInstance();

    // when
    testRule.deploy(repositoryService.createDeployment().addModelInstance("foo.dmn", dmnModelInstance));

    // then
    assertThat(repositoryService.createDecisionDefinitionQuery()
        .decisionDefinitionResourceName("foo.dmn").singleResult()).isNotNull();
  }

  @Test
  void testDeployDmnModelInstanceNegativeHistoryTimeToLive() {
    // given
    DmnModelInstance dmnModelInstance = createDmnModelInstanceNegativeHistoryTimeToLive();
    var deploymentBuilder = repositoryService.createDeployment().addModelInstance("foo.dmn", dmnModelInstance);

    // when/then
    assertThatThrownBy(() -> testRule.deploy(deploymentBuilder))
      .isInstanceOf(ProcessEngineException.class)
      .satisfies(ex -> assertThat(ex.getCause().getMessage()).contains("negative value is not allowed"));
  }

  @SuppressWarnings("deprecation")
  protected static DmnModelInstance createDmnModelInstanceNegativeHistoryTimeToLive() {
    DmnModelInstance modelInstance = Dmn.createEmptyModel();
    Definitions definitions = modelInstance.newInstance(Definitions.class);
    definitions.setId(DmnModelConstants.DMN_ELEMENT_DEFINITIONS);
    definitions.setName(DmnModelConstants.DMN_ELEMENT_DEFINITIONS);
    definitions.setNamespace(DmnModelConstants.OPERATON_NS);
    modelInstance.setDefinitions(definitions);

    Decision decision = modelInstance.newInstance(Decision.class);
    decision.setId("Decision-1");
    decision.setName("foo");
    decision.setOperatonHistoryTimeToLiveString("-5");
    modelInstance.getDefinitions().addChildElement(decision);

    return modelInstance;
  }

  @SuppressWarnings("deprecation")
  protected static DmnModelInstance createDmnModelInstance() {
    DmnModelInstance modelInstance = Dmn.createEmptyModel();
    Definitions definitions = modelInstance.newInstance(Definitions.class);
    definitions.setId(DmnModelConstants.DMN_ELEMENT_DEFINITIONS);
    definitions.setName(DmnModelConstants.DMN_ELEMENT_DEFINITIONS);
    definitions.setNamespace(DmnModelConstants.OPERATON_NS);
    modelInstance.setDefinitions(definitions);

    Decision decision = modelInstance.newInstance(Decision.class);
    decision.setId("Decision-1");
    decision.setName("foo");
    decision.setOperatonHistoryTimeToLiveString("5");
    modelInstance.getDefinitions().addChildElement(decision);

    DecisionTable decisionTable = modelInstance.newInstance(DecisionTable.class);
    decisionTable.setId(DmnModelConstants.DMN_ELEMENT_DECISION_TABLE);
    decisionTable.setHitPolicy(HitPolicy.FIRST);
    decision.addChildElement(decisionTable);

    Input input = modelInstance.newInstance(Input.class);
    input.setId("Input-1");
    input.setLabel("Input");
    decisionTable.addChildElement(input);

    InputExpression inputExpression = modelInstance.newInstance(InputExpression.class);
    inputExpression.setId("InputExpression-1");
    Text inputExpressionText = modelInstance.newInstance(Text.class);
    inputExpressionText.setTextContent("input");
    inputExpression.setText(inputExpressionText);
    inputExpression.setTypeRef("string");
    input.setInputExpression(inputExpression);

    Output output = modelInstance.newInstance(Output.class);
    output.setName("output");
    output.setLabel("Output");
    output.setTypeRef("string");
    decisionTable.addChildElement(output);

    return modelInstance;
  }

  @Test
  void testDeployAndGetDecisionDefinition() {

    // given decision model
    DmnModelInstance dmnModelInstance = createDmnModelInstance();

    // when decision model is deployed
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().addModelInstance("foo.dmn", dmnModelInstance);
    DeploymentWithDefinitions deployment = testRule.deploy(deploymentBuilder);

    // then deployment contains definition
    List<DecisionDefinition> deployedDecisionDefinitions = deployment.getDeployedDecisionDefinitions();
    assertThat(deployedDecisionDefinitions).hasSize(1);
    assertThat(deployment.getDeployedDecisionRequirementsDefinitions()).isNull();
    assertThat(deployment.getDeployedProcessDefinitions()).isNull();
    assertThat(deployment.getDeployedCaseDefinitions()).isNull();

    // and persisted definition are equal to deployed definition
    DecisionDefinition persistedDecisionDef = repositoryService.createDecisionDefinitionQuery()
      .decisionDefinitionResourceName("foo.dmn").singleResult();
    assertThat(deployedDecisionDefinitions.get(0).getId()).isEqualTo(persistedDecisionDef.getId());
  }

  @Test
  void testDeployEmptyDecisionDefinition() {

    // given empty decision model
    DmnModelInstance modelInstance = Dmn.createEmptyModel();
    Definitions definitions = modelInstance.newInstance(Definitions.class);
    definitions.setId(DmnModelConstants.DMN_ELEMENT_DEFINITIONS);
    definitions.setName(DmnModelConstants.DMN_ELEMENT_DEFINITIONS);
    definitions.setNamespace(DmnModelConstants.OPERATON_NS);
    modelInstance.setDefinitions(definitions);

    // when decision model is deployed
    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().addModelInstance("foo.dmn", modelInstance);
    DeploymentWithDefinitions deployment = testRule.deploy(deploymentBuilder);

    // then deployment contains no definitions
    assertThat(deployment.getDeployedDecisionDefinitions()).isNull();
    assertThat(deployment.getDeployedDecisionRequirementsDefinitions()).isNull();

    // and there are no persisted definitions
    assertThat(repositoryService.createDecisionDefinitionQuery()
        .decisionDefinitionResourceName("foo.dmn").singleResult()).isNull();
  }


  @Test
  void testDeployAndGetDRDDefinition() {

    // when decision requirement graph is deployed
    DeploymentWithDefinitions deployment = testRule.deploy(DRD_SCORE_RESOURCE);

    // then deployment contains definitions
    List<DecisionDefinition> deployedDecisionDefinitions = deployment.getDeployedDecisionDefinitions();
    assertThat(deployedDecisionDefinitions).hasSize(2);

    List<DecisionRequirementsDefinition> deployedDecisionRequirementsDefinitions = deployment.getDeployedDecisionRequirementsDefinitions();
    assertThat(deployedDecisionRequirementsDefinitions).hasSize(1);

    assertThat(deployment.getDeployedProcessDefinitions()).isNull();
    assertThat(deployment.getDeployedCaseDefinitions()).isNull();

    // and persisted definitions are equal to deployed definitions
    DecisionRequirementsDefinition persistedDecisionRequirementsDefinition = repositoryService.createDecisionRequirementsDefinitionQuery()
      .decisionRequirementsDefinitionResourceName(DRD_SCORE_RESOURCE).singleResult();
    assertThat(deployedDecisionRequirementsDefinitions.get(0).getId()).isEqualTo(persistedDecisionRequirementsDefinition.getId());

    List<DecisionDefinition> persistedDecisionDefinitions = repositoryService.createDecisionDefinitionQuery().decisionDefinitionResourceName(DRD_SCORE_RESOURCE).list();
    assertThat(persistedDecisionDefinitions).hasSize(deployedDecisionDefinitions.size());
  }

  @Test
  void testDeployDecisionDefinitionWithIntegerHistoryTimeToLive() {
    // when
    DeploymentWithDefinitions deployment = testRule.deploy("org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testDecisionDefinitionWithIntegerHistoryTimeToLive.dmn11.xml");

    // then
    List<DecisionDefinition> deployedDecisionDefinitions = deployment.getDeployedDecisionDefinitions();
    assertThat(deployedDecisionDefinitions).hasSize(1);
    Integer historyTimeToLive = deployedDecisionDefinitions.get(0).getHistoryTimeToLive();
    assertThat(historyTimeToLive).isNotNull();
    assertThat((int) historyTimeToLive).isEqualTo(5);
  }

  @Test
  void testDeployDecisionDefinitionWithStringHistoryTimeToLive() {
    // when
    DeploymentWithDefinitions deployment = testRule.deploy("org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testDecisionDefinitionWithStringHistoryTimeToLive.dmn11.xml");

    // then
    List<DecisionDefinition> deployedDecisionDefinitions = deployment.getDeployedDecisionDefinitions();
    assertThat(deployedDecisionDefinitions).hasSize(1);
    Integer historyTimeToLive = deployedDecisionDefinitions.get(0).getHistoryTimeToLive();
    assertThat(historyTimeToLive).isNotNull();
    assertThat((int) historyTimeToLive).isEqualTo(5);
  }

  @Test
  void testDeployDecisionDefinitionWithMalformedStringHistoryTimeToLive() {
    // when/then
    assertThatThrownBy(() -> testRule.deploy("org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testDecisionDefinitionWithMalformedHistoryTimeToLive.dmn11.xml"))
      .isInstanceOf(ProcessEngineException.class)
      .satisfies(e -> assertThat(e.getCause().getMessage()).contains("Cannot parse historyTimeToLive"));
  }

  @Test
  void testDeployDecisionDefinitionWithEmptyHistoryTimeToLive() {
      DeploymentWithDefinitions deployment = testRule.deploy("org/operaton/bpm/engine/test/dmn/deployment/DecisionDefinitionDeployerTest.testDecisionDefinitionWithEmptyHistoryTimeToLive.dmn11.xml");

      // then
      List<DecisionDefinition> deployedDecisionDefinitions = deployment.getDeployedDecisionDefinitions();
    assertThat(deployedDecisionDefinitions).hasSize(1);
      Integer historyTimeToLive = deployedDecisionDefinitions.get(0).getHistoryTimeToLive();
    assertThat(historyTimeToLive).isNull();
  }

}
