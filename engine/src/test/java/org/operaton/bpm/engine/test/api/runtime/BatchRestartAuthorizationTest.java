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
package org.operaton.bpm.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;

import java.util.Collection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

/**
 *
 * @author Anna Pazola
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
@Parameterized
public class BatchRestartAuthorizationTest {

  protected static final String TEST_REASON = "test reason";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);
  BatchModificationHelper helper = new BatchModificationHelper(engineRule);

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
      scenario()
        .withoutAuthorizations()
        .failsDueToRequired(
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
          grant(Resources.BATCH, "*", "userId", BatchPermissions.CREATE_BATCH_RESTART_PROCESS_INSTANCES)
        ),
      scenario()
        .withAuthorizations(
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE)
        )
        .failsDueToRequired(
          grant(Resources.PROCESS_DEFINITION, "Process", "userId", Permissions.READ_HISTORY)
        ),
      scenario()
        .withAuthorizations(
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
          grant(Resources.PROCESS_DEFINITION, "Process", "userId", Permissions.READ_HISTORY)
        )
        .failsDueToRequired(
          grant(Resources.PROCESS_INSTANCE, "*", "userId", Permissions.CREATE)
        ),
      scenario()
        .withAuthorizations(
          grant(Resources.BATCH, "*", "userId", Permissions.CREATE),
          grant(Resources.PROCESS_DEFINITION, "Process", "userId", Permissions.READ_HISTORY, Permissions.CREATE_INSTANCE),
          grant(Resources.PROCESS_INSTANCE, "*", "userId", Permissions.CREATE)
        ),
      scenario()
        .withAuthorizations(
          grant(Resources.BATCH, "*", "userId", BatchPermissions.CREATE_BATCH_RESTART_PROCESS_INSTANCES),
          grant(Resources.PROCESS_DEFINITION, "Process", "userId", Permissions.READ_HISTORY, Permissions.CREATE_INSTANCE),
          grant(Resources.PROCESS_INSTANCE, "*", "userId", Permissions.CREATE)
        )
        .succeeds()
    );
  }

  @AfterEach
  void tearDown() {
    authRule.deleteUsersAndGroups();
  }

  @AfterEach
  void cleanBatch() {
    Batch batch = engineRule.getManagementService().createBatchQuery().singleResult();
    if (batch != null) {
      engineRule.getManagementService().deleteBatch(
          batch.getId(), true);
    }

    HistoricBatch historicBatch = engineRule.getHistoryService().createHistoricBatchQuery().singleResult();
    if (historicBatch != null) {
      engineRule.getHistoryService().deleteHistoricBatch(
          historicBatch.getId());
    }
  }

  @AfterEach
  void removeBatches() {
    helper.removeAllRunningAndHistoricBatches();
  }

  @TestTemplate
  void executeBatch() {
    //given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ProcessModels.TWO_TASKS_PROCESS);

    ProcessInstance processInstance1 = engineRule.getRuntimeService().startProcessInstanceByKey("Process");
    ProcessInstance processInstance2 = engineRule.getRuntimeService().startProcessInstanceByKey("Process");
    engineRule.getRuntimeService().deleteProcessInstance(processInstance1.getId(), TEST_REASON);
    engineRule.getRuntimeService().deleteProcessInstance(processInstance2.getId(), TEST_REASON);

    authRule
        .init(scenario)
        .withUser("userId")
        .bindResource("processInstance1", processInstance1.getId())
        .bindResource("restartedProcessInstance", "*")
        .bindResource("processInstance2", processInstance2.getId())
        .bindResource("processDefinition", "Process")
        .bindResource("batchId", "*")
        .start();

    Batch batch = engineRule.getRuntimeService()
        .restartProcessInstances(processDefinition.getId())
        .processInstanceIds(processInstance1.getId(), processInstance2.getId())
        .startAfterActivity("userTask1")
        .executeAsync();

    if (batch != null) {
      Job job = engineRule.getManagementService().createJobQuery().jobDefinitionId(batch.getSeedJobDefinitionId()).singleResult();

      // seed job
      engineRule.getManagementService().executeJob(job.getId());

      for (Job pending : engineRule.getManagementService().createJobQuery().jobDefinitionId(batch.getBatchJobDefinitionId()).list()) {
        engineRule.getManagementService().executeJob(pending.getId());
      }
    }
    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(batch.getCreateUserId()).isEqualTo("userId");
    }
  }
}
