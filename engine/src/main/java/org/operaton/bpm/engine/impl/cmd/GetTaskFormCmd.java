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

import org.operaton.bpm.engine.form.TaskFormData;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.form.handler.TaskFormHandler;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskManager;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Tom Baeyens
 */
public class GetTaskFormCmd implements Command<TaskFormData>, Serializable {

  @Serial private static final long serialVersionUID = 1L;
  protected String taskId;

  public GetTaskFormCmd(String taskId) {
    this.taskId = taskId;
  }

  @Override
  public TaskFormData execute(CommandContext commandContext) {
    TaskManager taskManager = commandContext.getTaskManager();
    TaskEntity task = taskManager.findTaskById(taskId);
    ensureNotNull("No task found for taskId '%s'".formatted(taskId), "task", task);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadTaskVariable(task);
    }

    if (task.getTaskDefinition() != null) {
      TaskFormHandler taskFormHandler = task.getTaskDefinition().getTaskFormHandler();
      ensureNotNull("No taskFormHandler specified for task '%s'".formatted(taskId), "taskFormHandler", taskFormHandler);

      return taskFormHandler.createTaskForm(task);
    } else {
      // Standalone task, no TaskFormData available
      return null;
    }
  }

}
