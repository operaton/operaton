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
package org.operaton.bpm.engine.test.api.history;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.history.HistoricDecisionInstance;
import org.operaton.bpm.engine.history.HistoricDecisionInstanceQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.revoke;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
@Parameterized
public class BatchHistoricDecisionInstanceDeletionAuthorizationTest {

  protected static final String DECISION = "decision";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected DecisionService decisionService;
  protected HistoryService historyService;
  protected ManagementService managementService;

  protected List<String> decisionInstanceIds;

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
      scenario()
        .withoutAuthorizations()
        .failsDueToRequired(
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
          grant(Resources.BATCH, "*", "userId", BatchPermissions.CREATE_BATCH_DELETE_DECISION_INSTANCES)
        ),
      scenario()
        .withAuthorizations(
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE)
        )
        .failsDueToRequired(
          grant(Resources.DECISION_DEFINITION, "*", "userId", Permissions.DELETE_HISTORY)
        ),
      scenario()
        .withAuthorizations(
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
          grant(Resources.DECISION_DEFINITION, "*", "userId", Permissions.DELETE_HISTORY)
        ),
      scenario()
        .withAuthorizations(
          grant(Resources.BATCH, "*", "userId", BatchPermissions.CREATE_BATCH_DELETE_DECISION_INSTANCES),
          grant(Resources.DECISION_DEFINITION, "*", "userId", Permissions.DELETE_HISTORY)
        ),
      scenario()
        .withAuthorizations(
          revoke(Resources.BATCH, "*", "userId", BatchPermissions.CREATE_BATCH_DELETE_DECISION_INSTANCES),
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE)
          )
        .failsDueToRequired(
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
          grant(Resources.BATCH, "*", "userId", BatchPermissions.CREATE_BATCH_DELETE_DECISION_INSTANCES)
        )
        .succeeds()
    );
  }

  @BeforeEach
  void executeDecisionInstances() {
    decisionInstanceIds = new ArrayList<>();
    testRule.deploy("org/operaton/bpm/engine/test/api/dmn/Example.dmn");

    VariableMap variables = Variables.createVariables()
        .putValue("status", "silver")
        .putValue("sum", 723);

    for (int i = 0; i < 10; i++) {
      decisionService.evaluateDecisionByKey(DECISION).variables(variables).evaluate();
    }

    List<HistoricDecisionInstance> decisionInstances = historyService.createHistoricDecisionInstanceQuery().list();
    for(HistoricDecisionInstance decisionInstance : decisionInstances) {
      decisionInstanceIds.add(decisionInstance.getId());
    }
  }

  @AfterEach
  void tearDown() {
    authRule.deleteUsersAndGroups();
  }

  @AfterEach
  void removeBatches() {
    for (Batch batch : managementService.createBatchQuery().list()) {
      managementService.deleteBatch(batch.getId(), true);
    }

    // remove history of completed batches
    for (HistoricBatch historicBatch : historyService.createHistoricBatchQuery().list()) {
      historyService.deleteHistoricBatch(historicBatch.getId());
    }
  }

  @TestTemplate
  void executeBatch() {
    // given
    authRule.init(scenario)
      .withUser("userId")
      .start();

    HistoricDecisionInstanceQuery query = historyService.createHistoricDecisionInstanceQuery().decisionDefinitionKey(DECISION);

    Batch batch = historyService.deleteHistoricDecisionInstancesAsync(decisionInstanceIds, query, null);

    if (batch != null) {
      Job job = managementService.createJobQuery().jobDefinitionId(batch.getSeedJobDefinitionId()).singleResult();

      // seed job
      managementService.executeJob(job.getId());

      for (Job pending : managementService.createJobQuery().jobDefinitionId(batch.getBatchJobDefinitionId()).list()) {
        managementService.executeJob(pending.getId());
      }
    }
    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(batch.getCreateUserId()).isEqualTo("userId");
    }
  }
}
