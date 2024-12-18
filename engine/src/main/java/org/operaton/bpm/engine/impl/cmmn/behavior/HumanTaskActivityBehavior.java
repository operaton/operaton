/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.cmmn.behavior;

import org.operaton.bpm.engine.impl.cmmn.execution.CmmnActivityExecution;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.el.ExpressionManager;
import org.operaton.bpm.engine.impl.persistence.entity.SuspensionState;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.task.TaskDecorator;
import org.operaton.bpm.engine.impl.task.TaskDefinition;

/**
 * @author Roman Smirnov
 *
 */
public class HumanTaskActivityBehavior extends TaskActivityBehavior {

  protected TaskDecorator taskDecorator;

  @Override
  protected void performStart(CmmnActivityExecution execution) {
    execution.createTask(taskDecorator);
  }

  @Override
  protected void performTerminate(CmmnActivityExecution execution) {
    terminating(execution);
    super.performTerminate(execution);
  }

  @Override
  protected void performExit(CmmnActivityExecution execution) {
    terminating(execution);
    super.performExit(execution);
  }

  protected void terminating(CmmnActivityExecution execution) {
    TaskEntity task = getTask(execution);
    // it can happen that a there does not exist
    // a task, because the given execution was never
    // active.
    if (task != null) {
      task.delete("terminated", false);
    }
  }

  @Override
  protected void completing(CmmnActivityExecution execution) {
    TaskEntity task = getTask(execution);
    if (task != null) {
      task.caseExecutionCompleted();
    }
  }

  @Override
  protected void manualCompleting(CmmnActivityExecution execution) {
    completing(execution);
  }

  protected void suspending(CmmnActivityExecution execution) {
    String id = execution.getId();

    Context
      .getCommandContext()
      .getTaskManager()
      .updateTaskSuspensionStateByCaseExecutionId(id, SuspensionState.SUSPENDED);
  }

  @Override
  protected void resuming(CmmnActivityExecution execution) {
    String id = execution.getId();

    Context
      .getCommandContext()
      .getTaskManager()
      .updateTaskSuspensionStateByCaseExecutionId(id, SuspensionState.ACTIVE);
  }

  protected TaskEntity getTask(CmmnActivityExecution execution) {
    return Context
        .getCommandContext()
        .getTaskManager()
        .findTaskByCaseExecutionId(execution.getId());
  }

  @Override
  protected String getTypeName() {
    return "human task";
  }

  // getters/setters /////////////////////////////////////////////////

  public TaskDecorator getTaskDecorator() {
    return taskDecorator;
  }

  public void setTaskDecorator(TaskDecorator taskDecorator) {
    this.taskDecorator = taskDecorator;
  }

  public TaskDefinition getTaskDefinition() {
    return taskDecorator.getTaskDefinition();
  }

  public ExpressionManager getExpressionManager() {
    return taskDecorator.getExpressionManager();
  }

}
