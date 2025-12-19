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
import java.util.Map;

import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.form.handler.TaskFormHandler;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionVariableSnapshotObserver;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskManager;
import org.operaton.bpm.engine.impl.task.TaskDefinition;
import org.operaton.bpm.engine.task.DelegationState;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class SubmitTaskFormCmd implements Command<VariableMap>, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  protected String taskId;
  protected VariableMap properties;

  // only fetch variables if they are actually requested;
  // this avoids unnecessary loading of variables
  protected boolean returnVariables;
  protected boolean deserializeValues;

  public SubmitTaskFormCmd(String taskId, Map<String, Object> properties, boolean returnVariables, boolean deserializeValues) {
    this.taskId = taskId;
    this.properties = Variables.fromMap(properties);
    this.returnVariables = returnVariables;
    this.deserializeValues = deserializeValues;
  }

  @Override
  public VariableMap execute(CommandContext commandContext) {
    ensureNotNull("taskId", taskId);
    TaskManager taskManager = commandContext.getTaskManager();
    TaskEntity task = taskManager.findTaskById(taskId);
    ensureNotNull("Cannot find task with id %s".formatted(taskId), "task", task);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkTaskWork(task);
    }

    TaskDefinition taskDefinition = task.getTaskDefinition();
    if(taskDefinition != null) {
      TaskFormHandler taskFormHandler = taskDefinition.getTaskFormHandler();
      taskFormHandler.submitFormVariables(properties, task);
    } else {
      // set variables on standalone task
      task.setVariables(properties);
    }

    ExecutionEntity execution = task.getProcessInstance();
    ExecutionVariableSnapshotObserver variablesListener = null;
    if (returnVariables && execution != null) {
      variablesListener = new ExecutionVariableSnapshotObserver(execution, false, deserializeValues);
    }

    // complete or resolve the task
    if (DelegationState.PENDING.equals(task.getDelegationState())) {
      task.resolve();
      task.logUserOperation(UserOperationLogEntry.OPERATION_TYPE_RESOLVE);
      task.triggerUpdateEvent();
    } else {
      task.logUserOperation(UserOperationLogEntry.OPERATION_TYPE_COMPLETE);
      task.complete();
    }

    if (returnVariables)
    {
      if (variablesListener != null) {
        return variablesListener.getVariables();
      } else {
        return task.getCaseDefinitionId() == null ? null : task.getVariablesTyped(false);
      }
    }
    else
    {
      return null;
    }
  }
}
