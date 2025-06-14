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

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serializable;

import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskManager;


/**
 * @author Tom Baeyens
 */
public class DelegateTaskCmd implements Command<Object>, Serializable {

  private static final long serialVersionUID = 1L;

  protected String taskId;
  protected String userId;

  public DelegateTaskCmd(String taskId, String userId) {
    this.taskId = taskId;
    this.userId = userId;
  }

  @Override
  public Object execute(CommandContext commandContext) {
    ensureNotNull("taskId", taskId);

    TaskManager taskManager = commandContext.getTaskManager();
    TaskEntity task = taskManager.findTaskById(taskId);
    ensureNotNull("Cannot find task with id " + taskId, "task", task);

    checkDelegateTask(task, commandContext);

    task.delegate(userId);

    task.triggerUpdateEvent();
    task.logUserOperation(UserOperationLogEntry.OPERATION_TYPE_DELEGATE);

    return null;
  }

  protected void checkDelegateTask(TaskEntity task, CommandContext commandContext) {
    for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkTaskAssign(task);
    }
  }
}
