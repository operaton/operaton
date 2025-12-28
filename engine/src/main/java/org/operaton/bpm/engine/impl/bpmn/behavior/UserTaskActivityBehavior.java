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
package org.operaton.bpm.engine.impl.bpmn.behavior;

import java.util.Collection;

import org.operaton.bpm.engine.impl.el.ExpressionManager;
import org.operaton.bpm.engine.impl.migration.instance.MigratingActivityInstance;
import org.operaton.bpm.engine.impl.migration.instance.MigratingUserTaskInstance;
import org.operaton.bpm.engine.impl.migration.instance.parser.MigratingInstanceParseContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity.TaskState;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.operaton.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.operaton.bpm.engine.impl.pvm.delegate.MigrationObserverBehavior;
import org.operaton.bpm.engine.impl.task.TaskDecorator;
import org.operaton.bpm.engine.impl.task.TaskDefinition;

/**
 * activity implementation for the user task.
 *
 * @author Joram Barrez
 * @author Roman Smirnov
 */
public class UserTaskActivityBehavior extends TaskActivityBehavior implements MigrationObserverBehavior {

  protected TaskDecorator taskDecorator;

  /**
   * @deprecated since 1.0, use {@link #UserTaskActivityBehavior(TaskDecorator)} instead,
   *             which provides a more modular approach to task decoration.
   */
  @Deprecated(since = "1.0")
  public UserTaskActivityBehavior(ExpressionManager expressionManager, TaskDefinition taskDefinition) {
    this.taskDecorator = new TaskDecorator(taskDefinition, expressionManager);
  }

  public UserTaskActivityBehavior(TaskDecorator taskDecorator) {
    this.taskDecorator = taskDecorator;
  }

  @Override
  public void performExecution(ActivityExecution execution) throws Exception {
    TaskEntity task = new TaskEntity((ExecutionEntity) execution);
    task.insert();

    // initialize task properties
    taskDecorator.decorate(task, execution);

    // fire lifecycle events after task is initialized
    task.transitionTo(TaskState.STATE_CREATED);
  }

  @Override
  public void signal(ActivityExecution execution, String signalName, Object signalData) throws Exception {
    leave(execution);
  }

  // migration

  @Override
  public void migrateScope(ActivityExecution scopeExecution) {
    // nothing to do
  }

  @Override
  public void onParseMigratingInstance(MigratingInstanceParseContext parseContext, MigratingActivityInstance migratingInstance) {
    ExecutionEntity execution = migratingInstance.resolveRepresentativeExecution();

    for (TaskEntity task : execution.getTasks()) {
      migratingInstance.addMigratingDependentInstance(new MigratingUserTaskInstance(task, migratingInstance));
      parseContext.consume(task);

      Collection<VariableInstanceEntity> variables = task.getVariablesInternal();

      if (variables != null) {
        for (VariableInstanceEntity variable : variables) {
          // we don't need to represent task variables in the migrating instance structure because
          // they are migrated by the MigratingTaskInstance as well
          parseContext.consume(variable);
        }
      }
    }

  }

  // getters

  public TaskDefinition getTaskDefinition() {
    return taskDecorator.getTaskDefinition();
  }

  public ExpressionManager getExpressionManager() {
    return taskDecorator.getExpressionManager();
  }

  public TaskDecorator getTaskDecorator() {
    return taskDecorator;
  }

}
