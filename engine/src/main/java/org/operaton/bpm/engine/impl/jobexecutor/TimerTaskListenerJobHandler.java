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
package org.operaton.bpm.engine.impl.jobexecutor;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;

/**
 * {@link JobHandler} implementation for timer task listeners which can be defined for user tasks.
 *
 * <p>
 * The configuration contains the id of the activity as well as the id of the task listener.
 * </p>
 *
 */
public class TimerTaskListenerJobHandler extends TimerEventJobHandler {

  public static final String TYPE = "timer-task-listener";

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public void execute(TimerJobConfiguration configuration, ExecutionEntity execution, CommandContext commandContext, String tenantId) {
    String activityId = configuration.getTimerElementKey();
    TaskEntity targetTask = null;
    for (TaskEntity task : execution.getTasks()) {
      if (task.getTaskDefinitionKey().equals(activityId)) {
        targetTask = task;
      }
    }

    if (targetTask != null) {
      targetTask.triggerTimeoutEvent(configuration.getTimerElementSecondaryKey());
    } else {
      throw new ProcessEngineException("Error while triggering timeout task listener '%s': cannot find task for activity id '%s'.".formatted(configuration.getTimerElementSecondaryKey(), configuration.getTimerElementKey()));
    }

  }

}
