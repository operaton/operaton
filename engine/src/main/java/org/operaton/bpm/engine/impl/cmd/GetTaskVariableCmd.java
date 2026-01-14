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

import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Tom Baeyens
 */
public class GetTaskVariableCmd implements Command<Object> {
  protected String taskId;
  protected String variableName;
  protected boolean isLocal;

  public GetTaskVariableCmd(String taskId, String variableName, boolean isLocal) {
    this.taskId = taskId;
    this.variableName = variableName;
    this.isLocal = isLocal;
  }

  @Override
  public Object execute(CommandContext commandContext) {
    ensureNotNull("taskId", taskId);
    ensureNotNull("variableName", variableName);

    TaskEntity task = Context
      .getCommandContext()
      .getTaskManager()
      .findTaskById(taskId);

    ensureNotNull("task %s doesn't exist".formatted(taskId), "task", task);

    checkGetTaskVariable(task, commandContext);

    Object value;

    if (isLocal) {
      value = task.getVariableLocal(variableName);
    } else {
      value = task.getVariable(variableName);
    }

    return value;
  }

  protected void checkGetTaskVariable(TaskEntity task, CommandContext commandContext) {
    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadTaskVariable(task);
    }
  }
}
