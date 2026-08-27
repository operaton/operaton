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
package org.operaton.bpm.engine.impl.cmd;


import java.util.Collections;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.ModificationBatchConfiguration;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.ProcessInstanceModificationBuilderImpl;
import org.operaton.bpm.engine.impl.batch.BatchConfiguration;
import org.operaton.bpm.engine.impl.batch.DeploymentMapping;
import org.operaton.bpm.engine.impl.batch.DeploymentMappings;
import org.operaton.bpm.engine.impl.batch.builder.BatchBuilder;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionManager;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;

import static java.util.Objects.requireNonNull;

/**
 * @author Yana Vasileva
 *
 */
public @NullMarked class ModifyProcessInstanceAsyncCmd implements Command<Batch> {

  private static final CommandLogger LOG = ProcessEngineLogger.CMD_LOGGER;

  protected ProcessInstanceModificationBuilderImpl builder;

  public ModifyProcessInstanceAsyncCmd(ProcessInstanceModificationBuilderImpl builder) {
    this.builder = builder;
  }

  @Override
  public Batch execute(CommandContext commandContext) {
    String processInstanceId = builder.getProcessInstanceId();

    ExecutionManager executionManager = commandContext.getExecutionManager();
    ExecutionEntity processInstance = executionManager.findExecutionById(processInstanceId);

    ensureProcessInstanceExists(processInstanceId, processInstance);
    requireNonNull(processInstance);

    String processDefinitionId = processInstance.getProcessDefinitionId();
    String tenantId = processInstance.getTenantId();

    String deploymentId = commandContext.getProcessEngineConfiguration().getDeploymentCache()
      .findDeployedProcessDefinitionById(processDefinitionId)
      .getDeploymentId();
     requireNonNull(deploymentId, "a deployed process definition must be associated with a deployment");

    return new BatchBuilder(commandContext)
        .type(Batch.TYPE_PROCESS_INSTANCE_MODIFICATION)
        .config(getConfiguration(processDefinitionId, deploymentId))
        .tenantId(tenantId)
        .totalJobs(1)
        .permission(BatchPermissions.CREATE_BATCH_MODIFY_PROCESS_INSTANCES)
        .operationLogHandler(this::writeOperationLog)
        .build();
  }

  protected void ensureProcessInstanceExists(String processInstanceId,
                                             @Nullable ExecutionEntity processInstance) {
    if (processInstance == null) {
      throw LOG.processInstanceDoesNotExist(processInstanceId);
    }
  }

  protected String getLogEntryOperation() {
    return UserOperationLogEntry.OPERATION_TYPE_MODIFY_PROCESS_INSTANCE;
  }

  protected void writeOperationLog(CommandContext commandContext) {
    commandContext.getOperationLogManager().logProcessInstanceOperation(getLogEntryOperation(),
        builder.getProcessInstanceId(),
        null,
        null,
        Collections.singletonList(PropertyChange.EMPTY_CHANGE),
        builder.getAnnotation());
  }

  public BatchConfiguration getConfiguration(String processDefinitionId, String deploymentId) {
    return new ModificationBatchConfiguration(
        Collections.singletonList(builder.getProcessInstanceId()),
        DeploymentMappings.of(new DeploymentMapping(deploymentId, 1)),
        processDefinitionId,
        builder.getModificationOperations(),
        builder.isSkipCustomListeners(),
        builder.isSkipIoMappings());
  }

}
