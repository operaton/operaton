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
package org.operaton.bpm.engine.test.api.authorization.externaltask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.ExternalTaskQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationTestRule;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ExternalTaskModels;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario.scenario;
import static org.operaton.bpm.engine.test.api.authorization.util.AuthorizationSpec.grant;
import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
public class SetExternalTasksRetriesBatchAuthorizationTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @Parameter
  public AuthorizationScenario scenario;

  @Parameters
  public static Collection<AuthorizationScenario[]> scenarios() {
    return AuthorizationTestRule.asParameters(
      scenario()
        .withAuthorizations(
            grant(Resources.PROCESS_DEFINITION, "processDefinition", "userId", Permissions.READ, Permissions.READ_INSTANCE))
        .failsDueToRequired(
            grant(Resources.BATCH, "batchId", "userId", Permissions.CREATE),
            grant(Resources.BATCH, "batchId", "userId", BatchPermissions.CREATE_BATCH_SET_EXTERNAL_TASK_RETRIES)),
      scenario()
        .withAuthorizations(
            grant(Resources.PROCESS_DEFINITION, "processDefinition", "userId", Permissions.READ, Permissions.READ_INSTANCE, Permissions.UPDATE_INSTANCE),
            grant(Resources.BATCH, "batchId", "userId", Permissions.CREATE))
        .succeeds(),
      scenario()
        .withAuthorizations(
            grant(Resources.PROCESS_DEFINITION, "processDefinition", "userId", Permissions.READ, Permissions.READ_INSTANCE, Permissions.UPDATE_INSTANCE),
            grant(Resources.BATCH, "batchId", "userId", BatchPermissions.CREATE_BATCH_SET_EXTERNAL_TASK_RETRIES))
        .succeeds(),
      scenario()
        .withAuthorizations(
          grant(Resources.PROCESS_DEFINITION, "processDefinition", "userId", Permissions.READ, Permissions.READ_INSTANCE),
          grant(Resources.BATCH, "batchId", "userId", Permissions.CREATE),
          grant(Resources.PROCESS_INSTANCE, "processInstance1", "userId", Permissions.UPDATE))
        .succeeds(),
      scenario()
        .withAuthorizations(
            grant(Resources.PROCESS_DEFINITION, "processDefinition", "userId", Permissions.READ, Permissions.READ_INSTANCE),
            grant(Resources.BATCH, "batchId", "userId", Permissions.CREATE))
        .failsDueToRequired(
            grant(Resources.PROCESS_DEFINITION, "processDefinition", "userId", Permissions.UPDATE_INSTANCE),
            grant(Resources.PROCESS_INSTANCE, "processInstance1", "userId", Permissions.UPDATE))
      );
  }

  @BeforeEach
  void setUp() {
    authRule.createUserAndGroup("userId", "groupId");
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

  @TestTemplate
  void testSetRetriesAsync() {

    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);
    ProcessInstance processInstance1 = engineRule.getRuntimeService().startProcessInstanceByKey("Process");
    List<ExternalTask> externalTasks = engineRule.getExternalTaskService().createExternalTaskQuery().list();

    ArrayList<String> externalTaskIds = new ArrayList<>();

    for (ExternalTask task : externalTasks) {
      externalTaskIds.add(task.getId());
    }

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("batchId", "*")
      .bindResource("processInstance1", processInstance1.getId())
      .bindResource("processDefinition", processDefinition.getKey())
      .start();

    Batch batch = engineRule.getExternalTaskService().setRetriesAsync(externalTaskIds, null, 5);
    if (batch != null) {
      executeSeedAndBatchJobs(batch);
    }

    // then
    if (authRule.assertScenario(scenario)) {
      externalTasks = engineRule.getExternalTaskService().createExternalTaskQuery().list();
      for ( ExternalTask task : externalTasks) {
        assertThat((int) task.getRetries()).isEqualTo(5);
      }
    }
  }

  @TestTemplate
  void testSetRetriesWithQueryAsync() {

    // given
    ProcessDefinition processDefinition = testRule.deployAndGetDefinition(ExternalTaskModels.ONE_EXTERNAL_TASK_PROCESS);
    ProcessInstance processInstance1 = engineRule.getRuntimeService().startProcessInstanceByKey("Process");
    List<ExternalTask> externalTasks;

    ExternalTaskQuery externalTaskQuery = engineRule.getExternalTaskService().createExternalTaskQuery();

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("batchId", "*")
      .bindResource("processInstance1", processInstance1.getId())
      .bindResource("processDefinition", processDefinition.getKey())
      .start();

    Batch batch = engineRule.getExternalTaskService().setRetriesAsync(null, externalTaskQuery, 5);
    if (batch != null) {
      executeSeedAndBatchJobs(batch);
    }

    // then
    if (authRule.assertScenario(scenario)) {
      assertThat(batch.getCreateUserId()).isEqualTo("userId");

      externalTasks = engineRule.getExternalTaskService().createExternalTaskQuery().list();
      for ( ExternalTask task : externalTasks) {
        assertThat((int) task.getRetries()).isEqualTo(5);
      }
    }
  }

  public void executeSeedAndBatchJobs(Batch batch) {
    Job job = engineRule.getManagementService().createJobQuery().jobDefinitionId(batch.getSeedJobDefinitionId()).singleResult();
    // seed job
    engineRule.getManagementService().executeJob(job.getId());

    for (Job pending : engineRule.getManagementService().createJobQuery().jobDefinitionId(batch.getBatchJobDefinitionId()).list()) {
      engineRule.getManagementService().executeJob(pending.getId());
    }
  }
}
