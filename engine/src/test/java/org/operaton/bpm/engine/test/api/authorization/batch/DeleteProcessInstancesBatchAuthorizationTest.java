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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.TestTemplate;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Askar Akhmerov
 */
@Parameterized
public class DeleteProcessInstancesBatchAuthorizationTest extends AbstractBatchAuthorizationTest {

  protected static final long BATCH_OPERATIONS = 3L;

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestExtension.asParameters(
        scenario()
            .withAuthorizations(
                grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
                grant(Resources.PROCESS_INSTANCE, "processInstance1", "userId", Permissions.READ, Permissions.DELETE),
                grant(Resources.PROCESS_INSTANCE, "processInstance2", "userId", Permissions.READ)
            )
            .failsDueToRequired(
                grant(Resources.PROCESS_INSTANCE, "processInstance2", "userId", Permissions.DELETE),
                grant(Resources.PROCESS_DEFINITION, "Process_2", "userId", Permissions.DELETE_INSTANCE)
            ),
        scenario()
            .withAuthorizations(
                grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
                grant(Resources.PROCESS_INSTANCE, "processInstance1", "userId", Permissions.ALL),
                grant(Resources.PROCESS_INSTANCE, "processInstance2", "userId", Permissions.ALL)
            ).succeeds(),
        scenario()
            .withAuthorizations(
                grant(Resources.BATCH, "*", "userId", BatchPermissions.CREATE_BATCH_DELETE_RUNNING_PROCESS_INSTANCES),
                grant(Resources.PROCESS_INSTANCE, "processInstance1", "userId", Permissions.ALL),
                grant(Resources.PROCESS_INSTANCE, "processInstance2", "userId", Permissions.ALL)
            ).succeeds(),
        scenario()
            .withAuthorizations(
                grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
                grant(Resources.PROCESS_DEFINITION, "Process_2", "userId", Permissions.READ_INSTANCE, Permissions.DELETE_INSTANCE),
                grant(Resources.PROCESS_DEFINITION, "Process_1", "userId", Permissions.READ_INSTANCE, Permissions.DELETE_INSTANCE)
            ).succeeds()
    );
  }

  @TestTemplate
  void testWithTwoInvocationsProcessInstancesList() {
    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJob(2);
    setupAndExecuteProcessInstancesListTest();

    // then
    assertScenario();
  }

  @TestTemplate
  void testProcessInstancesList() {
    setupAndExecuteProcessInstancesListTest();
    // then
    assertScenario();
  }

  @TestTemplate
  void testWithQuery() {
    //given
    ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery()
        .processInstanceIds(new HashSet<>(List.of(processInstance.getId(), processInstance2.getId())));

    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("processInstance1", processInstance.getId())
        .bindResource("processInstance2", processInstance2.getId())
        .bindResource("Process_2", sourceDefinition2.getKey())
        .start();

    // when

    batch = runtimeService.deleteProcessInstancesAsync(null,
        processInstanceQuery, TEST_REASON);
    executeSeedAndBatchJobs();

    // then
    if (authRule.assertScenario(scenario) && testHelper.isHistoryLevelFull()) {
      assertThat(engineRule.getHistoryService().createUserOperationLogQuery().entityType(EntityTypes.PROCESS_INSTANCE).count()).isEqualTo(BATCH_OPERATIONS);
    }
  }

  protected void setupAndExecuteProcessInstancesListTest() {
    //given
    List<String> processInstanceIds = List.of(processInstance.getId(), processInstance2.getId());
    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("processInstance1", processInstance.getId())
        .bindResource("processInstance2", processInstance2.getId())
        .bindResource("Process_2", sourceDefinition2.getKey())
        .bindResource("Process_1", sourceDefinition.getKey())
        .start();

    // when
    batch = runtimeService.deleteProcessInstancesAsync(
        processInstanceIds, null, TEST_REASON);

    executeSeedAndBatchJobs();
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

      if (authRule.scenarioSucceeded()) {
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
      }
    }
  }

  @Override
  public AuthorizationScenario getScenario() {
    return scenario;
  }
}
