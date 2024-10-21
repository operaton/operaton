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
package org.operaton.bpm.engine.impl.cmd;

import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;

/**
 * Command to change task priority to a new value.
 */
public class SetTaskPriorityCmd extends AbstractSetTaskPropertyCmd<Integer> {

  /**
   * Public constructor.
   *
   * @param taskId   the id of the referenced task, non-null
   * @param priority the new priority value to set, non-null
   */
  public SetTaskPriorityCmd(String taskId, Integer priority) {
    super(taskId, priority, true);
  }

  @Override
  protected String getUserOperationLogName() {
    return UserOperationLogEntry.OPERATION_TYPE_SET_PRIORITY;
  }

  @Override
  protected void executeSetOperation(TaskEntity task, Integer priority) {
    task.setPriority(priority);
  }
}