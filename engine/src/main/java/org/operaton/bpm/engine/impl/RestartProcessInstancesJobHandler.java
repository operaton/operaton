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
package org.operaton.bpm.engine.impl;

import java.util.List;

import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.impl.batch.AbstractBatchJobHandler;
import org.operaton.bpm.engine.impl.batch.BatchJobContext;
import org.operaton.bpm.engine.impl.batch.BatchJobDeclaration;
import org.operaton.bpm.engine.impl.cmd.RestartProcessInstancesCmd;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.JobDeclaration;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.MessageEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;

/**
 *
 * @author Anna Pazola
 *
 */
public class RestartProcessInstancesJobHandler extends AbstractBatchJobHandler<RestartProcessInstancesBatchConfiguration>{

  public static final BatchJobDeclaration JOB_DECLARATION = new BatchJobDeclaration(Batch.TYPE_PROCESS_INSTANCE_RESTART);
  private static final RestartProcessInstancesBatchConfigurationJsonConverter JSON_CONVERTER =
      new RestartProcessInstancesBatchConfigurationJsonConverter();

  @Override
  public String getType() {
    return Batch.TYPE_PROCESS_INSTANCE_RESTART;
  }

  @Override
  protected void postProcessJob(RestartProcessInstancesBatchConfiguration configuration, JobEntity job, RestartProcessInstancesBatchConfiguration jobConfiguration) {
    if (job.getDeploymentId() == null) {
      CommandContext commandContext = Context.getCommandContext();
      ProcessDefinitionEntity processDefinitionEntity = commandContext.getProcessEngineConfiguration().getDeploymentCache()
          .findDeployedProcessDefinitionById(configuration.getProcessDefinitionId());
      job.setDeploymentId(processDefinitionEntity.getDeploymentId());
    }
  }

  @Override
  public void executeHandler(RestartProcessInstancesBatchConfiguration batchConfiguration,
                             ExecutionEntity execution,
                             CommandContext commandContext,
                             String tenantId) {

    String processDefinitionId = batchConfiguration.getProcessDefinitionId();
    RestartProcessInstanceBuilderImpl builder =
        new RestartProcessInstanceBuilderImpl(processDefinitionId);

    builder.processInstanceIds(batchConfiguration.getIds());

    builder.setInstructions(batchConfiguration.getInstructions());

    if (batchConfiguration.isInitialVariables()) {
      builder.initialSetOfVariables();
    }

    if (batchConfiguration.isSkipCustomListeners()) {
      builder.skipCustomListeners();
    }

    if (batchConfiguration.isWithoutBusinessKey()) {
      builder.withoutBusinessKey();
    }

    if (batchConfiguration.isSkipIoMappings()) {
      builder.skipIoMappings();
    }

    CommandExecutor commandExecutor = commandContext.getProcessEngineConfiguration()
        .getCommandExecutorTxRequired();
    commandContext.executeWithOperationLogPrevented(
        new RestartProcessInstancesCmd(commandExecutor, builder));

  }

  @Override
  public JobDeclaration<BatchJobContext, MessageEntity> getJobDeclaration() {
    return JOB_DECLARATION;
  }

  @Override
  protected RestartProcessInstancesBatchConfiguration createJobConfiguration(RestartProcessInstancesBatchConfiguration configuration,
      List<String> processIdsForJob) {
    return new RestartProcessInstancesBatchConfiguration(processIdsForJob, configuration.getInstructions(), configuration.getProcessDefinitionId(),
        configuration.isInitialVariables(), configuration.isSkipCustomListeners(), configuration.isSkipIoMappings(), configuration.isWithoutBusinessKey());
  }

  @Override
  protected RestartProcessInstancesBatchConfigurationJsonConverter getJsonConverterInstance() {
    return JSON_CONVERTER;
  }

}
