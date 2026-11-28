/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import org.jspecify.annotations.NullMarked;
import org.operaton.bpm.engine.impl.HistoricTaskInstanceQueryImpl;
import org.operaton.bpm.engine.impl.cfg.CommandChecker;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;

@NullMarked
final class TaskArtifactReadAuthorization {

  private TaskArtifactReadAuthorization() {
  }

  static boolean canReadTask(CommandContext commandContext, String taskId) {
    TaskEntity task = commandContext.getTaskManager().findTaskById(taskId);
    if (task != null) {
      for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
        checker.checkReadTask(task);
      }
      return true;
    }

    // Use the regular historic query so authorization and tenant filtering apply to completed tasks.
    var query = new HistoricTaskInstanceQueryImpl();
    query.taskId(taskId);
    return query.executeCount(commandContext) == 1;
  }
}
