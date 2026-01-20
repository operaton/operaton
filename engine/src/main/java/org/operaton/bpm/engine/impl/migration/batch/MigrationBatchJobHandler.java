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
package org.operaton.bpm.engine.impl.migration.batch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.impl.batch.AbstractBatchJobHandler;
import org.operaton.bpm.engine.impl.batch.BatchJobContext;
import org.operaton.bpm.engine.impl.batch.BatchJobDeclaration;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.core.variable.VariableUtil;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.jobexecutor.JobDeclaration;
import org.operaton.bpm.engine.impl.json.MigrationBatchConfigurationJsonConverter;
import org.operaton.bpm.engine.impl.migration.MigrateProcessInstanceCmd;
import org.operaton.bpm.engine.impl.migration.MigrationPlanExecutionBuilderImpl;
import org.operaton.bpm.engine.impl.migration.MigrationPlanImpl;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.MessageEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.operaton.bpm.engine.migration.MigrationPlanExecutionBuilder;
import org.operaton.bpm.engine.variable.impl.VariableMapImpl;

/**
 * Job handler for batch migration jobs. The batch migration job
 * migrates a list of process instances.
 */
public class MigrationBatchJobHandler extends AbstractBatchJobHandler<MigrationBatchConfiguration> {

  public static final BatchJobDeclaration JOB_DECLARATION = new BatchJobDeclaration(Batch.TYPE_PROCESS_INSTANCE_MIGRATION);
  private static final MigrationBatchConfigurationJsonConverter JSON_CONVERTER =
      new MigrationBatchConfigurationJsonConverter();

  @Override
  public String getType() {
    return Batch.TYPE_PROCESS_INSTANCE_MIGRATION;
  }

  @Override
  public JobDeclaration<BatchJobContext, MessageEntity> getJobDeclaration() {
    return JOB_DECLARATION;
  }

  protected MigrationBatchConfigurationJsonConverter getJsonConverterInstance() {
    return JSON_CONVERTER;
  }

  @Override
  protected MigrationBatchConfiguration createJobConfiguration(MigrationBatchConfiguration configuration, List<String> processIdsForJob) {
    return new MigrationBatchConfiguration(
        processIdsForJob,
        configuration.getMigrationPlan(),
        configuration.isSkipCustomListeners(),
        configuration.isSkipIoMappings(),
        configuration.getBatchId()
    );
  }

  @Override
  protected void postProcessJob(MigrationBatchConfiguration configuration, JobEntity job, MigrationBatchConfiguration jobConfiguration) {
    if (job.getDeploymentId() == null) {
      CommandContext commandContext = Context.getCommandContext();
      String sourceProcessDefinitionId = configuration.getMigrationPlan().getSourceProcessDefinitionId();

      ProcessDefinitionEntity processDefinition = getProcessDefinition(commandContext, sourceProcessDefinitionId);
      job.setDeploymentId(processDefinition.getDeploymentId());
    }
  }

  @Override
  public void executeHandler(MigrationBatchConfiguration batchConfiguration,
                             ExecutionEntity execution,
                             CommandContext commandContext,
                             String tenantId) {

    MigrationPlanImpl migrationPlan = (MigrationPlanImpl) batchConfiguration.getMigrationPlan();

    String batchId = batchConfiguration.getBatchId();
    setVariables(batchId, migrationPlan, commandContext);

    MigrationPlanExecutionBuilder executionBuilder = commandContext.getProcessEngineConfiguration()
        .getRuntimeService()
        .newMigration(migrationPlan)
        .processInstanceIds(batchConfiguration.getIds());

    if (batchConfiguration.isSkipCustomListeners()) {
      executionBuilder.skipCustomListeners();
    }
    if (batchConfiguration.isSkipIoMappings()) {
      executionBuilder.skipIoMappings();
    }

    commandContext.executeWithOperationLogPrevented(
        new MigrateProcessInstanceCmd((MigrationPlanExecutionBuilderImpl)executionBuilder, true));
  }

  protected void setVariables(String batchId,
                              MigrationPlanImpl migrationPlan,
                              CommandContext commandContext) {
    Map<String, ?> variables = null;
    if (batchId != null) {
      variables = VariableUtil.findBatchVariablesSerialized(batchId, commandContext);
      if (variables != null) {
        migrationPlan.setVariables(new VariableMapImpl(new HashMap<>(variables)));
      }
    }
  }

  protected ProcessDefinitionEntity getProcessDefinition(CommandContext commandContext, String processDefinitionId) {
    return commandContext.getProcessEngineConfiguration()
        .getDeploymentCache()
        .findDeployedProcessDefinitionById(processDefinitionId);
  }

}
