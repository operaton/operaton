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
package org.operaton.bpm.engine.test.api.dmn;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.dmn.engine.impl.DefaultDmnEngineConfiguration;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.repository.DecisionDefinition;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.ResetDmnConfigUtil;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

/**
 * @author Tobias Metzke
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class DecisionServiceUserOperationLogTest {

  protected static final String DMN_DECISION_TABLE = "org/operaton/bpm/engine/test/api/dmn/Example.dmn";
  protected static final String DMN_DECISION_TABLE_V2 = "org/operaton/bpm/engine/test/api/dmn/Example_v2.dmn";

  protected static final String DMN_DECISION_LITERAL_EXPRESSION = "org/operaton/bpm/engine/test/api/dmn/DecisionWithLiteralExpression.dmn";
  protected static final String DMN_DECISION_LITERAL_EXPRESSION_V2 = "org/operaton/bpm/engine/test/api/dmn/DecisionWithLiteralExpression_v2.dmn";

  protected static final String DECISION_DEFINITION_KEY = "decision";

  protected static final String USER_ID = "userId";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected DecisionService decisionService;
  protected RepositoryService repositoryService;
  protected HistoryService historyService;
  protected IdentityService identityService;

  @BeforeEach
  void init() {
    identityService.clearAuthentication();
  }

  @BeforeEach
  void enableDmnFeelLegacyBehavior() {
    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        engineRule.getProcessEngineConfiguration()
            .getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(true)
        .init();
  }

  @AfterEach
  void disableDmnFeelLegacyBehavior() {

    DefaultDmnEngineConfiguration dmnEngineConfiguration =
        engineRule.getProcessEngineConfiguration()
            .getDmnEngineConfiguration();

    ResetDmnConfigUtil.reset(dmnEngineConfiguration)
        .enableFeelLegacyBehavior(false)
        .init();
  }

  @Deployment(resources = DMN_DECISION_TABLE)
  @Test
  void logCreationOnEvaluateDecisionTableById() {
    // given
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().singleResult();

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    decisionService.evaluateDecisionTableById(decisionDefinition.getId(), createVariables());
    identityService.clearAuthentication();

    // then
    assertOperationLog(decisionDefinition);
  }

  @Deployment(resources = DMN_DECISION_TABLE)
  @Test
  void logCreationOnEvaluateDecisionTableByKey() {
    // given
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().singleResult();

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    decisionService.evaluateDecisionTableByKey(DECISION_DEFINITION_KEY, createVariables());
    identityService.clearAuthentication();

    // then
    assertOperationLog(decisionDefinition);
  }

  @Deployment(resources = DMN_DECISION_TABLE)
  @Test
  void logCreationOnEvaluateDecisionTableByKeyAndLatestVersion() {
    testRule.deploy(DMN_DECISION_TABLE_V2);

    // given
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().latestVersion().singleResult();

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    decisionService.evaluateDecisionTableByKey(DECISION_DEFINITION_KEY, createVariables());
    identityService.clearAuthentication();

    // then
    assertOperationLog(decisionDefinition);
  }

  @Deployment(resources = DMN_DECISION_TABLE)
  @Test
  void logCreationOnEvaluateDecisionTableByKeyAndVersion() {
    testRule.deploy(DMN_DECISION_TABLE_V2);

    // given
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().decisionDefinitionVersion(1).singleResult();

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    decisionService.evaluateDecisionTableByKeyAndVersion(DECISION_DEFINITION_KEY, 1, createVariables());
    identityService.clearAuthentication();

    // then
    assertOperationLog(decisionDefinition);
  }

  @Deployment(resources = DMN_DECISION_TABLE)
  @Test
  void logCreationOnEvaluateDecisionTableByKeyAndNullVersion() {
    testRule.deploy(DMN_DECISION_TABLE_V2);

    // given
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().latestVersion().singleResult();

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    decisionService.evaluateDecisionTableByKeyAndVersion(DECISION_DEFINITION_KEY, null, createVariables());
    identityService.clearAuthentication();

    // then
    assertOperationLog(decisionDefinition);
  }

  @Deployment(resources = DMN_DECISION_LITERAL_EXPRESSION)
  @Test
  void logCreationOnEvaluateDecisionById() {
    // given
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().singleResult();

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    decisionService.evaluateDecisionById(decisionDefinition.getId()).variables(createVariables()).evaluate();
    identityService.clearAuthentication();

    // then
    assertOperationLog(decisionDefinition);
  }

  @Deployment(resources = DMN_DECISION_LITERAL_EXPRESSION)
  @Test
  void logCreationOnEvaluateDecisionByKey() {
    // given
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().singleResult();

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY).variables(createVariables()).evaluate();
    identityService.clearAuthentication();

    // then
    assertOperationLog(decisionDefinition);
  }

  @Deployment(resources = DMN_DECISION_LITERAL_EXPRESSION)
  @Test
  void logCreationOnEvaluateDecisionByKeyAndLatestVersion() {
    testRule.deploy(DMN_DECISION_LITERAL_EXPRESSION_V2);

    // given
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().latestVersion().singleResult();

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY).variables(createVariables()).evaluate();
    identityService.clearAuthentication();

    // then
    assertOperationLog(decisionDefinition);
  }

  @Deployment(resources = DMN_DECISION_LITERAL_EXPRESSION)
  @Test
  void logCreationOnEvaluateDecisionByKeyAndVersion() {
    testRule.deploy(DMN_DECISION_LITERAL_EXPRESSION_V2);

    // given
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().decisionDefinitionVersion(1).singleResult();

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY).version(1).variables(createVariables()).evaluate();
    identityService.clearAuthentication();

    // then
    assertOperationLog(decisionDefinition);
  }

  @Deployment(resources = DMN_DECISION_LITERAL_EXPRESSION)
  @Test
  void logCreationOnEvaluateDecisionByKeyAndNullVersion() {
    testRule.deploy(DMN_DECISION_LITERAL_EXPRESSION_V2);

    // given
    DecisionDefinition decisionDefinition = repositoryService.createDecisionDefinitionQuery().latestVersion().singleResult();

    // when
    identityService.setAuthenticatedUserId(USER_ID);
    decisionService.evaluateDecisionByKey(DECISION_DEFINITION_KEY).version(null).variables(createVariables()).evaluate();
    identityService.clearAuthentication();

    // then
    assertOperationLog(decisionDefinition);
  }

  protected VariableMap createVariables() {
    return Variables.createVariables().putValue("status", "silver").putValue("sum", 723);
  }

  protected void assertOperationLog(DecisionDefinition definition) {
    assertThat(historyService.createUserOperationLogQuery().count()).isEqualTo(2L);
    assertLogEntry("decisionDefinitionId", definition.getId());
    assertLogEntry("decisionDefinitionKey", definition.getKey());
  }

  protected void assertLogEntry(String property, Object newValue) {
    UserOperationLogEntry entry = historyService.createUserOperationLogQuery().property(property).singleResult();
    assertThat(entry).isNotNull();
    assertThat(entry.getOrgValue()).isNull();
    assertThat(entry.getNewValue()).isEqualTo(String.valueOf(newValue));
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.DECISION_DEFINITION);
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_EVALUATE);
  }

}
