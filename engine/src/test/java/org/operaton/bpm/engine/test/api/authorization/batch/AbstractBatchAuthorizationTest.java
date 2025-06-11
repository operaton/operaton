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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatch;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

import static org.operaton.bpm.engine.test.api.runtime.migration.ModifiableBpmnModelInstance.modify;

/**
 * @author Askar Akhmerov
 */
public abstract class AbstractBatchAuthorizationTest {
  protected static final String TEST_REASON = "test reason";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  protected AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);
  @RegisterExtension
  protected ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  protected ProcessDefinition sourceDefinition;
  protected ProcessDefinition sourceDefinition2;
  protected ProcessInstance processInstance;
  protected ProcessInstance processInstance2;
  protected Batch batch;
  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected int invocationsPerBatchJob;

  @BeforeEach
  public void setUp() {
    authRule.createUserAndGroup("userId", "groupId");
    runtimeService = engineRule.getRuntimeService();
    managementService = engineRule.getManagementService();
    invocationsPerBatchJob = engineRule.getProcessEngineConfiguration().getInvocationsPerBatchJob();
  }

  @BeforeEach
  public void deployProcesses() {
    sourceDefinition = testHelper.deployAndGetDefinition(modify(ProcessModels.ONE_TASK_PROCESS)
        .changeElementId(ProcessModels.PROCESS_KEY, "ONE_TASK_PROCESS"));
    sourceDefinition2 = testHelper.deployAndGetDefinition(modify(ProcessModels.TWO_TASKS_PROCESS)
        .changeElementId(ProcessModels.PROCESS_KEY, "TWO_TASKS_PROCESS"));
    processInstance = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition.getId());
    processInstance2 = engineRule.getRuntimeService().startProcessInstanceById(sourceDefinition2.getId());
  }

  @AfterEach
  public void tearDown() {
    authRule.deleteUsersAndGroups();
    engineRule.getProcessEngineConfiguration().setInvocationsPerBatchJob(invocationsPerBatchJob);
  }

  @AfterEach
  public void cleanBatch() {
    Batch runningBatch = engineRule.getManagementService().createBatchQuery().singleResult();
    if (runningBatch != null) {
      engineRule.getManagementService().deleteBatch(runningBatch.getId(), true);
    }

    HistoricBatch historicBatch = engineRule.getHistoryService().createHistoricBatchQuery().singleResult();
    if (historicBatch != null) {
      engineRule.getHistoryService().deleteHistoricBatch(historicBatch.getId());
    }
  }

  protected void executeSeedAndBatchJobs() {
    executeSeedJobs();

    for (Job pending : managementService.createJobQuery().jobDefinitionId(batch.getBatchJobDefinitionId()).list()) {
      managementService.executeJob(pending.getId());
    }
  }

  public void executeSeedJobs() {
    while(getSeedJob() != null) {
      managementService.executeJob(getSeedJob().getId());
    }
  }

  public Job getSeedJob() {
    return engineRule.getManagementService().createJobQuery()
        .jobDefinitionId(batch.getSeedJobDefinitionId())
        .singleResult();
  }

  protected abstract AuthorizationScenario getScenario();
}
