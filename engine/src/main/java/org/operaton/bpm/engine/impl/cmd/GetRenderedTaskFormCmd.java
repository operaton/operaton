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
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.form.engine.FormEngine;
import org.operaton.bpm.engine.impl.form.handler.TaskFormHandler;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskManager;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;


/**
 * @author Tom Baeyens
 */
public class GetRenderedTaskFormCmd  implements Command<Object>, Serializable {

  @Serial private static final long serialVersionUID = 1L;
  protected String taskId;
  protected String formEngineName;

  public GetRenderedTaskFormCmd(String taskId, String formEngineName) {
    this.taskId = taskId;
    this.formEngineName = formEngineName;
  }


  @Override
  public Object execute(CommandContext commandContext) {
    TaskManager taskManager = commandContext.getTaskManager();
    TaskEntity task = taskManager.findTaskById(taskId);
    ensureNotNull("Task '%s' not found".formatted(taskId), "task", task);

    for(CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkReadTaskVariable(task);
    }
    ensureNotNull("Task form definition for '%s' not found".formatted(taskId), "task.getTaskDefinition()", task.getTaskDefinition());

    TaskFormHandler taskFormHandler = task.getTaskDefinition().getTaskFormHandler();
    if (taskFormHandler == null) {
      return null;
    }

    FormEngine formEngine = Context
      .getProcessEngineConfiguration()
      .getFormEngines()
      .get(formEngineName);

    ensureNotNull("No formEngine '%s' defined process engine configuration".formatted(formEngineName), "formEngine", formEngine);

    TaskFormData taskForm = taskFormHandler.createTaskForm(task);

    return formEngine.renderTaskForm(taskForm);
  }
}
