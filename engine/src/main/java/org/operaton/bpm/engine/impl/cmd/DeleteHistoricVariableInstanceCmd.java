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

import java.io.Serial;
import java.io.Serializable;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;
import org.operaton.bpm.engine.impl.repository.ResourceDefinitionEntity;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotEmpty;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Tobias Metzke
 *
 */
public class DeleteHistoricVariableInstanceCmd implements Command<Void>, Serializable {

  @Serial private static final long serialVersionUID = 1L;
  private final String variableInstanceId;

  public DeleteHistoricVariableInstanceCmd(String variableInstanceId) {
    this.variableInstanceId = variableInstanceId;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    ensureNotEmpty(BadUserRequestException.class,"variableInstanceId", variableInstanceId);

    HistoricVariableInstanceEntity variable = commandContext.getHistoricVariableInstanceManager().findHistoricVariableInstanceByVariableInstanceId(variableInstanceId);
    ensureNotNull(NotFoundException.class, "No historic variable instance found with id: %s".formatted(variableInstanceId), "variable", variable);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkDeleteHistoricVariableInstance(variable);
    }

    commandContext
      .getHistoricDetailManager()
      .deleteHistoricDetailsByVariableInstanceId(variableInstanceId);

    commandContext
      .getHistoricVariableInstanceManager()
      .deleteHistoricVariableInstanceByVariableInstanceId(variableInstanceId);

    // create user operation log
    ResourceDefinitionEntity<?> definition = null;
    try {
      if (variable.getProcessDefinitionId() != null) {
        definition = commandContext.getProcessEngineConfiguration().getDeploymentCache().findDeployedProcessDefinitionById(variable.getProcessDefinitionId());
      } else if (variable.getCaseDefinitionId() != null) {
        definition = commandContext.getProcessEngineConfiguration().getDeploymentCache().findDeployedCaseDefinitionById(variable.getCaseDefinitionId());
      }
    } catch (NotFoundException e) {
      // definition has been deleted already
    }
    commandContext.getOperationLogManager().logHistoricVariableOperation(
        UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY, variable, definition, new PropertyChange("name", null, variable.getName()));

    return null;
  }
}
