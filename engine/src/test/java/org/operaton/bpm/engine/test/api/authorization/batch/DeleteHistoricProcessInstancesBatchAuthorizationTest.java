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
package org.operaton.bpm.engine.test.api.authorization.batch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenarioWithCount;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Askar Akhmerov
 */
@Parameterized
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
public class DeleteHistoricProcessInstancesBatchAuthorizationTest extends AbstractBatchAuthorizationTest {

  protected static final long BATCH_OPERATIONS = 3;

  @Parameter
  public AuthorizationScenarioWithCount scenario;

  protected HistoryService historyService;

  @BeforeEach
  void setupHistoricService() {
    historyService = engineRule.getHistoryService();
  }

  @Override
  @AfterEach
  public void cleanBatch() {
    super.cleanBatch();
    List<HistoricProcessInstance> list = historyService.createHistoricProcessInstanceQuery().list();

    if (!list.isEmpty()) {
      List<String> instances = new ArrayList<>();
      for (HistoricProcessInstance hpi : list) {
        instances.add(hpi.getId());
      }
      historyService.deleteHistoricProcessInstances(instances);
    }
  }

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestExtension.asParameters(
        AuthorizationScenarioWithCount.scenario()
            .withCount(1L)
            .withAuthorizations(
                grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
                grant(Resources.PROCESS_DEFINITION, "Process_1", "userId", Permissions.READ_HISTORY, Permissions.DELETE_HISTORY),
                grant(Resources.PROCESS_DEFINITION, "Process_2", "userId", Permissions.READ_HISTORY)
            )
            .failsDueToRequired(
                grant(Resources.PROCESS_DEFINITION, "Process_2", "userId", Permissions.DELETE_HISTORY)
            ),
        AuthorizationScenarioWithCount.scenario()
            .withCount(0L)
            .withAuthorizations(
                grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
                grant(Resources.PROCESS_DEFINITION, "Process_1", "userId", Permissions.READ_HISTORY, Permissions.DELETE_HISTORY),
                grant(Resources.PROCESS_DEFINITION, "Process_2", "userId", Permissions.READ_HISTORY, Permissions.DELETE_HISTORY)
            ),
        AuthorizationScenarioWithCount.scenario()
            .withCount(0L)
            .withAuthorizations(
                grant(Resources.BATCH, "*", "userId", BatchPermissions.CREATE_BATCH_DELETE_FINISHED_PROCESS_INSTANCES),
                grant(Resources.PROCESS_DEFINITION, "Process_1", "userId", Permissions.READ_HISTORY, Permissions.DELETE_HISTORY),
                grant(Resources.PROCESS_DEFINITION, "Process_2", "userId", Permissions.READ_HISTORY, Permissions.DELETE_HISTORY)
            ).succeeds()
    );
  }

  @TestTemplate
  void testWithTwoInvocationsProcessInstancesList() {
    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJob(2);
    setupAndExecuteHistoricProcessInstancesListTest();

    // then
    assertScenario();

    assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(getScenario().getCount());
  }

  @TestTemplate
  void testProcessInstancesList() {
    setupAndExecuteHistoricProcessInstancesListTest();
    // then
    assertScenario();
  }

  protected void setupAndExecuteHistoricProcessInstancesListTest() {
    //given
    List<String> processInstanceIds = List.of(processInstance.getId(), processInstance2.getId());
    runtimeService.deleteProcessInstances(processInstanceIds, null, true, false);

    List<String> historicProcessInstances = new ArrayList<>();
    for (HistoricProcessInstance hpi : historyService.createHistoricProcessInstanceQuery().list()) {
      historicProcessInstances.add(hpi.getId());
    }

    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("Process_1", sourceDefinition.getKey())
        .bindResource("Process_2", sourceDefinition2.getKey())
        .start();

    // when
    batch = historyService.deleteHistoricProcessInstancesAsync(
        historicProcessInstances, TEST_REASON);

    executeSeedAndBatchJobs();
  }

  @Override
  public AuthorizationScenarioWithCount getScenario() {
    return scenario;
  }

  protected void assertScenario() {
    if (authRule.assertScenario(getScenario())) {
      Batch batch = engineRule.getManagementService().createBatchQuery().singleResult();
      assertThat(batch.getCreateUserId()).isEqualTo("userId");

      if (testHelper.isHistoryLevelFull()) {
        assertThat(engineRule.getHistoryService().createUserOperationLogQuery().entityType(EntityTypes.PROCESS_INSTANCE).count()).isEqualTo(BATCH_OPERATIONS);
        HistoricBatch historicBatch = engineRule.getHistoryService().createHistoricBatchQuery().list().get(0);
        assertThat(historicBatch.getCreateUserId()).isEqualTo("userId");
      }
      assertThat(historyService.createHistoricProcessInstanceQuery().count()).isZero();
    }
  }
}
