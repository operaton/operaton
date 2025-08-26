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
package org.operaton.bpm.engine.test.api.runtime.migration.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineProvider;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.migration.MigrationInstructionsBuilder;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.api.runtime.BatchHelper;
import org.operaton.bpm.engine.test.api.runtime.migration.MigrationTestRule;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.migration.MigrationTestExtension;

public class BatchMigrationHelper extends BatchHelper {

  protected MigrationTestRule migrationRule;
  protected MigrationTestExtension migrationExtension;

  public ProcessDefinition sourceProcessDefinition;
  public ProcessDefinition targetProcessDefinition;


  public BatchMigrationHelper(ProcessEngineProvider processEngineProvider) {
    super(processEngineProvider);
  }

  public BatchMigrationHelper(ProcessEngineProvider processEngineProvider, MigrationTestExtension migrationExtension) {
    super(processEngineProvider);
    this.migrationExtension = migrationExtension;
  }

  public BatchMigrationHelper(ProcessEngineProvider processEngineProvider, MigrationTestRule migrationRule) {
    super(processEngineProvider);
    this.migrationRule = migrationRule;
  }

  public ProcessDefinition getSourceProcessDefinition() {
    return sourceProcessDefinition;
  }

  public ProcessDefinition getTargetProcessDefinition() {
    return targetProcessDefinition;
  }

  public Batch createMigrationBatchWithSize(int batchSize) {
    int invocationsPerBatchJob = ((ProcessEngineConfigurationImpl) getProcessEngine().getProcessEngineConfiguration()).getInvocationsPerBatchJob();
    return migrateProcessInstancesAsync(invocationsPerBatchJob * batchSize);
  }

  public Batch migrateProcessInstancesAsync(int numberOfProcessInstances) {
    if (migrationRule != null) {
      sourceProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
      targetProcessDefinition = migrationRule.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    } else {
      sourceProcessDefinition = migrationExtension.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
      targetProcessDefinition = migrationExtension.deployAndGetDefinition(ProcessModels.ONE_TASK_PROCESS);
    }
    return migrateProcessInstancesAsync(numberOfProcessInstances, sourceProcessDefinition, targetProcessDefinition);
  }

  public Batch migrateProcessInstancesAsyncForTenant(int numberOfProcessInstances, String tenantId) {
    if (migrationRule != null) {
      sourceProcessDefinition = migrationRule.deployForTenantAndGetDefinition(tenantId, ProcessModels.ONE_TASK_PROCESS);
      targetProcessDefinition = migrationRule.deployForTenantAndGetDefinition(tenantId, ProcessModels.ONE_TASK_PROCESS);
    } else {
      sourceProcessDefinition = migrationExtension.deployForTenantAndGetDefinition(tenantId, ProcessModels.ONE_TASK_PROCESS);
      targetProcessDefinition = migrationExtension.deployForTenantAndGetDefinition(tenantId, ProcessModels.ONE_TASK_PROCESS);
    }
    return migrateProcessInstancesAsync(numberOfProcessInstances, sourceProcessDefinition, targetProcessDefinition);
  }

  public Batch migrateProcessInstanceAsync(ProcessDefinition sourceProcessDefinition, ProcessDefinition targetProcessDefinition) {
    return migrateProcessInstancesAsync(1, sourceProcessDefinition, targetProcessDefinition);
  }

  public Batch migrateProcessInstancesAsync(int numberOfProcessInstances,
                                            ProcessDefinition sourceProcessDefinition,
                                            ProcessDefinition targetProcessDefinition,
                                            Map<String, Object> variables,
                                            boolean authenticated) {
    List<String> processInstanceIds = new ArrayList<>(numberOfProcessInstances);
    RuntimeService runtimeService = getRuntimeService();
    for (int i = 0; i < numberOfProcessInstances; i++) {
      String srcProcessDefinitionId = sourceProcessDefinition.getId();
      String processInstanceId =
              runtimeService.startProcessInstanceById(srcProcessDefinitionId).getId();
      processInstanceIds.add(processInstanceId);
    }

    if (authenticated) {
      getIdentityService().setAuthenticatedUserId("user");
    }

    MigrationInstructionsBuilder planBuilder = runtimeService
        .createMigrationPlan(sourceProcessDefinition.getId(), targetProcessDefinition.getId())
        .mapEqualActivities();

    if (variables != null) {
      planBuilder.setVariables(variables);
    }

    return runtimeService.newMigration(planBuilder.build())
        .processInstanceIds(processInstanceIds)
        .executeAsync();
  }

  public Batch migrateProcessInstancesAsync(int numberOfProcessInstances, ProcessDefinition sourceProcessDefinition, ProcessDefinition targetProcessDefinition, Map<String, Object> variables) {
    return migrateProcessInstancesAsync(numberOfProcessInstances, sourceProcessDefinition, targetProcessDefinition, variables, false);
  }

  public Batch migrateProcessInstancesAsync(int numberOfProcessInstances, ProcessDefinition sourceProcessDefinition, ProcessDefinition targetProcessDefinition) {
    return migrateProcessInstancesAsync(numberOfProcessInstances, sourceProcessDefinition, targetProcessDefinition, null, false);
  }

  @Override
  public JobDefinition getExecutionJobDefinition(Batch batch) {
    return getManagementService()
      .createJobDefinitionQuery().jobDefinitionId(batch.getBatchJobDefinitionId()).jobType(Batch.TYPE_PROCESS_INSTANCE_MIGRATION).singleResult();
  }

  public long countSourceProcessInstances() {
    return getRuntimeService()
      .createProcessInstanceQuery().processDefinitionId(sourceProcessDefinition.getId()).count();
  }

  public long countTargetProcessInstances() {
    return getRuntimeService()
      .createProcessInstanceQuery().processDefinitionId(targetProcessDefinition.getId()).count();
  }
}
