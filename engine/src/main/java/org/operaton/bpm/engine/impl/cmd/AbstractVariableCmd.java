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

import org.operaton.bpm.engine.impl.core.variable.scope.AbstractVariableScope;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.pvm.runtime.Callback;
import org.operaton.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

/**
 * @author Stefan Hentschel.
 */
public abstract class AbstractVariableCmd implements Command<Void> {
  protected CommandContext commandContext;
  protected String entityId;
  protected boolean isLocal;
  protected boolean preventLogUserOperation;

  protected AbstractVariableCmd(String entityId, boolean isLocal) {
    this.entityId = entityId;
    this.isLocal = isLocal;
  }

  public AbstractVariableCmd disableLogUserOperation() {
    this.preventLogUserOperation = true;
    return this;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    this.commandContext = commandContext;

    AbstractVariableScope scope = getEntity();

    if (scope != null) {
      executeOperation(scope);
      onSuccess(scope);
      if(!preventLogUserOperation) {
        logVariableOperation(scope);
      }
    }

    return null;
  }

  protected abstract AbstractVariableScope getEntity();

  protected abstract ExecutionEntity getContextExecution();

  protected abstract void logVariableOperation(AbstractVariableScope scope);

  protected abstract void executeOperation(AbstractVariableScope scope);

  protected abstract String getLogEntryOperation();

  @SuppressWarnings("unused")
  protected void onSuccess(AbstractVariableScope scope) {
    ExecutionEntity contextExecution = getContextExecution();
    if (contextExecution != null) {
      contextExecution.dispatchDelayedEventsAndPerformOperation((Callback<PvmExecutionImpl, Void>) null);
    }
  }
}
