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
package org.operaton.bpm.engine.impl.batch.deletion;

import java.util.List;

import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.impl.batch.AbstractBatchJobHandler;
import org.operaton.bpm.engine.impl.batch.BatchConfiguration;
import org.operaton.bpm.engine.impl.batch.BatchJobContext;
import org.operaton.bpm.engine.impl.batch.BatchJobDeclaration;
import org.operaton.bpm.engine.impl.cmd.DeleteHistoricProcessInstancesCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.jobexecutor.JobDeclaration;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.MessageEntity;

/**
 * @author Askar Akhmerov
 */
public class DeleteHistoricProcessInstancesJobHandler extends AbstractBatchJobHandler<BatchConfiguration> {

  public static final BatchJobDeclaration JOB_DECLARATION = new BatchJobDeclaration(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION);
  private static final DeleteHistoricProcessInstanceBatchConfigurationJsonConverter JSON_CONVERTER =
      new DeleteHistoricProcessInstanceBatchConfigurationJsonConverter();

  @Override
  public String getType() {
    return Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION;
  }

  protected DeleteHistoricProcessInstanceBatchConfigurationJsonConverter getJsonConverterInstance() {
    return JSON_CONVERTER;
  }

  @Override
  public JobDeclaration<BatchJobContext, MessageEntity> getJobDeclaration() {
    return JOB_DECLARATION;
  }

  @Override
  protected BatchConfiguration createJobConfiguration(BatchConfiguration configuration, List<String> processIdsForJob) {
    return new BatchConfiguration(processIdsForJob, configuration.isFailIfNotExists());
  }

  @Override
  public void executeHandler(BatchConfiguration batchConfiguration,
                             ExecutionEntity execution,
                             CommandContext commandContext,
                             String tenantId) {

    commandContext.executeWithOperationLogPrevented(
        new DeleteHistoricProcessInstancesCmd(
            batchConfiguration.getIds(),
            batchConfiguration.isFailIfNotExists()));
  }

}
