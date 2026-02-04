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
package org.operaton.bpm.engine.test.history.dmn;

import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.history.NativeHistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.DecisionRequirementsDefinition;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.util.ResetDmnConfigUtil;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.propertyComparator;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.verifySorting;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Philipp Ossler
 * @author Ingo Richtsmeier
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
@ExtendWith(ProcessEngineExtension.class)
class HistoricDecisionInstanceQueryTest {

  protected static final String DECISION_CASE = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.caseWithDecisionTask.cmmn";
  protected static final String DECISION_PROCESS = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.processWithBusinessRuleTask.bpmn20.xml";
  protected static final String DECISION_PROCESS_WITH_UNDERSCORE = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.processWithBusinessRuleTask_.bpmn20.xml";

  protected static final String DECISION_SINGLE_OUTPUT_DMN = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.decisionSingleOutput.dmn11.xml";
  protected static final String DECISION_SINGLE_OUTPUT_DMN_WITH_UNDERSCORE = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.decisionSingleOutput_.dmn11.xml";
  protected static final String DECISION_NO_INPUT_DMN = "org/operaton/bpm/engine/test/history/HistoricDecisionInstanceTest.noInput.dmn11.xml";

  protected static final String DRG_DMN = "org/operaton/bpm/engine/test/dmn/deployment/drdDish.dmn11.xml";

  protected static final String DECISION_DEFINITION_KEY = "testDecision";
  protected static final String DISH_DECISION = "dish-decision";

  ProcessEngineConfigurationImpl processEngineConfiguration;
  HistoryService historyService;
  RepositoryService repositoryService;
  DecisionService decisionService;
  RuntimeService runtimeService;
  CaseService caseService;
  IdentityService identityService;

  @BeforeEach
  void setUp() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        processEngineConfiguration.getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(true)
        .init();


  }

  @AfterEach
  void tearDown() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        processEngineConfiguration.getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(false)
        .init();


  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryIncludeInputsForNonExistingDecision() {
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().includeInputs();
    assertThat(query.singleResult()).isNull();

    startProcessInstanceAndEvaluateDecision();

    assertThat(query.decisionInstanceId("nonExisting").singleResult()).isNull();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryIncludeOutputs() {

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();
    var historicDecisionInstance = query.singleResult();

    assertThatThrownBy(historicDecisionInstance::getOutputs).isInstanceOf(ProcessEngineException.class);

    assertThat(query.includeOutputs().singleResult().getOutputs()).hasSize(1);
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryIncludeOutputsForNonExistingDecision() {
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().includeOutputs();
    assertThat(query.singleResult()).isNull();

    startProcessInstanceAndEvaluateDecision();

    assertThat(query.decisionInstanceId("nonExisting").singleResult()).isNull();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_NO_INPUT_DMN})
  @Test
  void testQueryIncludeInputsNoInput() {

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.includeInputs().singleResult().getInputs()).isEmpty();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_NO_INPUT_DMN})
  @Test
  void testQueryIncludeOutputsNoInput() {

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.includeOutputs().singleResult().getOutputs()).isEmpty();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryPaging() {

    startProcessInstanceAndEvaluateDecision();
    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.listPage(0, 2)).hasSize(2);
    assertThat(query.listPage(1, 1)).hasSize(1);
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQuerySortByEvaluationTime() {

    startProcessInstanceAndEvaluateDecision();
    waitASignificantAmountOfTime();
    startProcessInstanceAndEvaluateDecision();

    List<HistoricDecisionInstance> orderAsc = historyService.createHistoricDecisionInstanceQuery().orderByEvaluationTime().asc().list();
    assertThat(orderAsc.get(0).getEvaluationTime().before(orderAsc.get(1).getEvaluationTime())).isTrue();

    List<HistoricDecisionInstance> orderDesc = historyService.createHistoricDecisionInstanceQuery().orderByEvaluationTime().desc().list();
    assertThat(orderDesc.get(0).getEvaluationTime().after(orderDesc.get(1).getEvaluationTime())).isTrue();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQuerySortByDecisionInstanceId() {
    for (int i = 0; i < 5; i++) {
      startProcessInstanceAndEvaluateDecision();
    }

    List<HistoricDecisionInstance> orderAsc = historyService.createHistoricDecisionInstanceQuery()
        .orderByDecisionInstanceId()
        .asc()
        .list();
    assertThat(orderAsc).hasSize(5);
    verifySorting(orderAsc, propertyComparator(HistoricDecisionInstance::getId));

    List<HistoricDecisionInstance> orderDesc = historyService.createHistoricDecisionInstanceQuery()
        .orderByDecisionInstanceId()
        .desc()
        .list();
    assertThat(orderDesc).hasSize(5);
    verifySorting(orderDesc, inverted(propertyComparator(HistoricDecisionInstance::getId)));
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByDecisionInstanceId() {
    ProcessInstance pi1 = startProcessInstanceAndEvaluateDecision();
    ProcessInstance pi2 = startProcessInstanceAndEvaluateDecision();

    String decisionInstanceId1 = historyService.createHistoricDecisionInstanceQuery().processInstanceId(pi1.getId()).singleResult().getId();
    String decisionInstanceId2 = historyService.createHistoricDecisionInstanceQuery().processInstanceId(pi2.getId()).singleResult().getId();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.decisionInstanceId(decisionInstanceId1).count()).isOne();
    assertThat(query.decisionInstanceId(decisionInstanceId2).count()).isOne();
    assertThat(query.decisionInstanceId("unknown").count()).isZero();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByDecisionInstanceIds() {
    ProcessInstance pi1 = startProcessInstanceAndEvaluateDecision();
    ProcessInstance pi2 = startProcessInstanceAndEvaluateDecision();

    String decisionInstanceId1 = historyService.createHistoricDecisionInstanceQuery().processInstanceId(pi1.getId()).singleResult().getId();
    String decisionInstanceId2 = historyService.createHistoricDecisionInstanceQuery().processInstanceId(pi2.getId()).singleResult().getId();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.decisionInstanceIdIn(decisionInstanceId1).count()).isOne();
    assertThat(query.decisionInstanceIdIn(decisionInstanceId2).count()).isOne();
    assertThat(query.decisionInstanceIdIn(decisionInstanceId1, decisionInstanceId2).count()).isEqualTo(2L);
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByDecisionDefinitionId() {
    String decisionDefinitionId = repositoryService.createDecisionDefinitionQuery()
        .decisionDefinitionKey(DECISION_DEFINITION_KEY).singleResult().getId();

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.decisionDefinitionId(decisionDefinitionId).count()).isOne();
    assertThat(query.decisionDefinitionId("other id").count()).isZero();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN, DRG_DMN})
  @Test
  void testQueryByDecisionDefinitionIdIn() {
    //given
    String decisionDefinitionId = repositoryService.createDecisionDefinitionQuery().decisionDefinitionKey(DECISION_DEFINITION_KEY).singleResult().getId();
    String decisionDefinitionId2 = repositoryService.createDecisionDefinitionQuery()
        .decisionDefinitionKey(DISH_DECISION).singleResult().getId();

    //when
    startProcessInstanceAndEvaluateDecision();
    decisionService.evaluateDecisionTableByKey(DISH_DECISION)
        .variables(Variables.createVariables().putValue("temperature", 21).putValue("dayType", "Weekend"))
        .evaluate();

    //then
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.decisionDefinitionIdIn(decisionDefinitionId, decisionDefinitionId2).count()).isEqualTo(2L);
    assertThat(query.decisionDefinitionIdIn("other id", "anotherFake").count()).isZero();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN, DRG_DMN})
  @Test
  void testQueryByInvalidDecisionDefinitionIdIn() {
    // given
    var query = historyService.createHistoricDecisionInstanceQuery();

    // when/then
    assertThatThrownBy(() -> query.decisionDefinitionIdIn("aFake", null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("decisionDefinitionIdIn contains null value");
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN, DRG_DMN})
  @Test
  void testQueryByDecisionDefinitionKeyIn() {

    //when
    startProcessInstanceAndEvaluateDecision();
    decisionService.evaluateDecisionTableByKey(DISH_DECISION)
        .variables(Variables.createVariables().putValue("temperature", 21).putValue("dayType", "Weekend"))
        .evaluate();

    //then
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.decisionDefinitionKeyIn(DISH_DECISION, DECISION_DEFINITION_KEY).count()).isEqualTo(2L);
    assertThat(query.decisionDefinitionKeyIn("other id", "anotherFake").count()).isZero();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN, DRG_DMN})
  @Test
  void testQueryByInvalidDecisionDefinitionKeyIn() {
    // given
    var query = historyService.createHistoricDecisionInstanceQuery();

    // when/then
    assertThatThrownBy(() -> query.decisionDefinitionKeyIn("aFake", null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("decisionDefinitionKeyIn contains null value");
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByDecisionDefinitionKey() {

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.decisionDefinitionKey(DECISION_DEFINITION_KEY).count()).isOne();
    assertThat(query.decisionDefinitionKey("other key").count()).isZero();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByDecisionDefinitionName() {

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.decisionDefinitionName("sample decision").count()).isOne();
    assertThat(query.decisionDefinitionName("other name").count()).isZero();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_PROCESS_WITH_UNDERSCORE, DECISION_SINGLE_OUTPUT_DMN, DECISION_SINGLE_OUTPUT_DMN_WITH_UNDERSCORE})
  @Test
  void testQueryByDecisionDefinitionNameLike() {

    startProcessInstanceAndEvaluateDecision();
    startProcessInstanceAndEvaluateDecisionWithUnderscore();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.decisionDefinitionNameLike("%ample dec%").count()).isOne();
    assertThat(query.decisionDefinitionNameLike("%ample\\_%").count()).isOne();

  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByInvalidDecisionDefinitionNameLike() {

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.decisionDefinitionNameLike("%invalid%").count()).isZero();

    assertThatThrownBy(() -> query.decisionDefinitionNameLike(null)).isInstanceOf(NotValidException.class);
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByProcessDefinitionKey() {
    String processDefinitionKey = repositoryService.createProcessDefinitionQuery().singleResult().getKey();

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.processDefinitionKey(processDefinitionKey).count()).isOne();
    assertThat(query.processDefinitionKey("other process").count()).isZero();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByProcessDefinitionId() {
    String processDefinitionId = repositoryService.createProcessDefinitionQuery().singleResult().getId();

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.processDefinitionId(processDefinitionId).count()).isOne();
    assertThat(query.processDefinitionId("other process").count()).isZero();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByProcessInstanceId() {

    startProcessInstanceAndEvaluateDecision();

    String processInstanceId = runtimeService.createProcessInstanceQuery().singleResult().getId();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.processInstanceId(processInstanceId).count()).isOne();
    assertThat(query.processInstanceId("other process").count()).isZero();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByActivityId() {

    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.activityIdIn("task").count()).isOne();
    assertThat(query.activityIdIn("other activity").count()).isZero();
    assertThat(query.activityIdIn("task", "other activity").count()).isOne();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByActivityInstanceId() {

    startProcessInstanceAndEvaluateDecision();

    String activityInstanceId = historyService.createHistoricActivityInstanceQuery().activityId("task").singleResult().getId();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();
    assertThat(query.activityInstanceIdIn(activityInstanceId).count()).isOne();
    assertThat(query.activityInstanceIdIn("other activity").count()).isZero();
    assertThat(query.activityInstanceIdIn(activityInstanceId, "other activity").count()).isOne();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByEvaluatedBefore() {
    Date beforeEvaluated = new Date(1441612000);
    Date evaluated = new Date(1441613000);
    Date afterEvaluated = new Date(1441614000);

    ClockUtil.setCurrentTime(evaluated);
    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();
    assertThat(query.evaluatedBefore(afterEvaluated).count()).isOne();
    assertThat(query.evaluatedBefore(evaluated).count()).isOne();
    assertThat(query.evaluatedBefore(beforeEvaluated).count()).isZero();

    ClockUtil.reset();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByEvaluatedAfter() {
    Date beforeEvaluated = new Date(1441612000);
    Date evaluated = new Date(1441613000);
    Date afterEvaluated = new Date(1441614000);

    ClockUtil.setCurrentTime(evaluated);
    startProcessInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();
    assertThat(query.evaluatedAfter(beforeEvaluated).count()).isOne();
    assertThat(query.evaluatedAfter(evaluated).count()).isOne();
    assertThat(query.evaluatedAfter(afterEvaluated).count()).isZero();

    ClockUtil.reset();
  }

  @Deployment(resources = {DECISION_CASE, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByCaseDefinitionKey() {
    createCaseInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.caseDefinitionKey("case").count()).isOne();
  }

  @Test
  void testQueryByInvalidCaseDefinitionKey() {
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.caseDefinitionKey("invalid").count()).isZero();

    assertThatThrownBy(() -> query.caseDefinitionKey(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {DECISION_CASE, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByCaseDefinitionId() {
    CaseInstance caseInstance = createCaseInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.caseDefinitionId(caseInstance.getCaseDefinitionId()).count()).isOne();
  }

  @Test
  void testQueryByInvalidCaseDefinitionId() {
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.caseDefinitionId("invalid").count()).isZero();

    assertThatThrownBy(() -> query.caseDefinitionId(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {DECISION_CASE, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByCaseInstanceId() {
    CaseInstance caseInstance = createCaseInstanceAndEvaluateDecision();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.caseInstanceId(caseInstance.getId()).count()).isOne();
  }

  @Test
  void testQueryByInvalidCaseInstanceId() {
    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.caseInstanceId("invalid").count()).isZero();

    assertThatThrownBy(() -> query.caseInstanceId(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByUserId() {
    evaluateDecisionWithAuthenticatedUser("demo");

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.userId("demo").count()).isOne();
  }

  @Deployment(resources = {DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testQueryByInvalidUserId() {
    evaluateDecisionWithAuthenticatedUser("demo");

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.userId("dem1").count()).isZero();

    assertThatThrownBy(() -> query.userId(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Deployment(resources = {DRG_DMN})
  @Test
  void testQueryByRootDecisionInstanceId() {
    decisionService.evaluateDecisionTableByKey(DISH_DECISION)
      .variables(Variables.createVariables().putValue("temperature", 21).putValue("dayType", "Weekend"))
      .evaluate();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();
    assertThat(query.count()).isEqualTo(3L);

    String rootDecisionInstanceId = query.decisionDefinitionKey(DISH_DECISION).singleResult().getId();
    String requiredDecisionInstanceId1 = query.decisionDefinitionKey("season").singleResult().getId();
    String requiredDecisionInstanceId2 = query.decisionDefinitionKey("guestCount").singleResult().getId();

    query = historyService.createHistoricDecisionInstanceQuery();
    assertThat(query.rootDecisionInstanceId(rootDecisionInstanceId).count()).isEqualTo(3L);
    assertThat(query.rootDecisionInstanceId(requiredDecisionInstanceId1).count()).isZero();
    assertThat(query.rootDecisionInstanceId(requiredDecisionInstanceId2).count()).isZero();
  }

  @Deployment(resources = {DRG_DMN})
  @Test
  void testQueryByRootDecisionInstancesOnly() {
    decisionService.evaluateDecisionTableByKey(DISH_DECISION)
      .variables(Variables.createVariables().putValue("temperature", 21).putValue("dayType", "Weekend"))
      .evaluate();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.rootDecisionInstancesOnly().count()).isOne();
    assertThat(query.rootDecisionInstancesOnly().singleResult().getDecisionDefinitionKey()).isEqualTo(DISH_DECISION);
  }

  @Deployment(resources = {DRG_DMN})
  @Test
  void testQueryByDecisionRequirementsDefinitionId() {
    decisionService.evaluateDecisionTableByKey(DISH_DECISION)
      .variables(Variables.createVariables().putValue("temperature", 21).putValue("dayType", "Weekend"))
      .evaluate();

    DecisionRequirementsDefinition decisionRequirementsDefinition = repositoryService.createDecisionRequirementsDefinitionQuery().singleResult();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.decisionRequirementsDefinitionId("notExisting").count()).isZero();
    assertThat(query.decisionRequirementsDefinitionId(decisionRequirementsDefinition.getId()).count()).isEqualTo(3L);
  }

  @Deployment(resources = {DRG_DMN})
  @Test
  void testQueryByDecisionRequirementsDefinitionKey() {
    decisionService.evaluateDecisionTableByKey(DISH_DECISION)
      .variables(Variables.createVariables().putValue("temperature", 21).putValue("dayType", "Weekend"))
      .evaluate();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery();

    assertThat(query.decisionRequirementsDefinitionKey("notExisting").count()).isZero();
    assertThat(query.decisionRequirementsDefinitionKey("dish").count()).isEqualTo(3L);
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testNativeQuery() {

    startProcessInstanceAndEvaluateDecision();

    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();

    NativeHistoricDecisionInstanceQuery nativeQuery = historyService
        .createNativeHistoricDecisionInstanceQuery().sql("SELECT * FROM " + tablePrefix + "ACT_HI_DECINST");

    assertThat(nativeQuery.list()).hasSize(1);

    NativeHistoricDecisionInstanceQuery nativeQueryWithParameter = historyService
        .createNativeHistoricDecisionInstanceQuery()
        .sql("SELECT * FROM " + tablePrefix + "ACT_HI_DECINST H WHERE H.DEC_DEF_KEY_ = #{decisionDefinitionKey}");

    assertThat(nativeQueryWithParameter.parameter("decisionDefinitionKey", DECISION_DEFINITION_KEY).list()).hasSize(1);
    assertThat(nativeQueryWithParameter.parameter("decisionDefinitionKey", "other decision").list()).isEmpty();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testNativeCountQuery() {

    startProcessInstanceAndEvaluateDecision();

    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();

    NativeHistoricDecisionInstanceQuery nativeQuery = historyService
        .createNativeHistoricDecisionInstanceQuery().sql("SELECT count(*) FROM " + tablePrefix + "ACT_HI_DECINST");

    assertThat(nativeQuery.count()).isOne();
  }

  @Deployment(resources = {DECISION_PROCESS, DECISION_SINGLE_OUTPUT_DMN})
  @Test
  void testNativeQueryPaging() {

    startProcessInstanceAndEvaluateDecision();
    startProcessInstanceAndEvaluateDecision();

    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();

    NativeHistoricDecisionInstanceQuery nativeQuery = historyService.createNativeHistoricDecisionInstanceQuery()
        .sql("SELECT * FROM " + tablePrefix + "ACT_HI_DECINST");

    assertThat(nativeQuery.listPage(0, 2)).hasSize(2);
    assertThat(nativeQuery.listPage(1, 1)).hasSize(1);
  }

  protected ProcessInstance startProcessInstanceAndEvaluateDecision() {
    return runtimeService.startProcessInstanceByKey("testProcess", getVariables());
  }

  protected ProcessInstance startProcessInstanceAndEvaluateDecisionWithUnderscore() {
    return runtimeService.startProcessInstanceByKey("testProcess_", getVariables());
  }

  protected CaseInstance createCaseInstanceAndEvaluateDecision() {
    return caseService
        .withCaseDefinitionByKey("case")
        .setVariables(getVariables())
        .create();
  }

  protected void evaluateDecisionWithAuthenticatedUser(String userId) {
    identityService.setAuthenticatedUserId(userId);
    VariableMap variables = Variables.putValue("input1", "test");
    decisionService.evaluateDecisionTableByKey(DECISION_DEFINITION_KEY, variables);
  }

  protected VariableMap getVariables() {
    VariableMap variables = Variables.createVariables();
    variables.put("input1", "test");
    return variables;
  }

  /**
   * Use between two rule evaluations to ensure the expected order by evaluation time.
   */
  protected void waitASignificantAmountOfTime() {
    DateTime now = new DateTime(ClockUtil.getCurrentTime());
    ClockUtil.setCurrentTime(now.plusSeconds(10).toDate());
  }

}
