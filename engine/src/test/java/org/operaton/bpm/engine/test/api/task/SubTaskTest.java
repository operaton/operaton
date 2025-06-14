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
package org.operaton.bpm.engine.test.api.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;


/**
 * @author Tom Baeyens
 */
@ExtendWith(ProcessEngineExtension.class)
class SubTaskTest {

  ProcessEngineConfigurationImpl processEngineConfiguration;
  TaskService taskService;
  HistoryService historyService;

  @Test
  void testSubTask() {
    Task gonzoTask = taskService.newTask();
    gonzoTask.setName("gonzoTask");
    taskService.saveTask(gonzoTask);

    Task subTaskOne = taskService.newTask();
    subTaskOne.setName("subtask one");
    String gonzoTaskId = gonzoTask.getId();
    subTaskOne.setParentTaskId(gonzoTaskId);
    taskService.saveTask(subTaskOne);

    Task subTaskTwo = taskService.newTask();
    subTaskTwo.setName("subtask two");
    subTaskTwo.setParentTaskId(gonzoTaskId);
    taskService.saveTask(subTaskTwo);

    String subTaskId = subTaskOne.getId();
    assertThat(taskService.getSubTasks(subTaskId)).isEmpty();
    assertThat(historyService
        .createHistoricTaskInstanceQuery()
        .taskParentTaskId(subTaskId)
        .list()).isEmpty();

    List<Task> subTasks = taskService.getSubTasks(gonzoTaskId);
    Set<String> subTaskNames = new HashSet<>();
    for (Task subTask: subTasks) {
      subTaskNames.add(subTask.getName());
    }

    if(processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_AUDIT.getId()) {
      Set<String> expectedSubTaskNames = new HashSet<>();
      expectedSubTaskNames.add("subtask one");
      expectedSubTaskNames.add("subtask two");

      assertThat(subTaskNames).isEqualTo(expectedSubTaskNames);

      List<HistoricTaskInstance> historicSubTasks = historyService
        .createHistoricTaskInstanceQuery()
        .taskParentTaskId(gonzoTaskId)
        .list();

      subTaskNames = new HashSet<>();
      for (HistoricTaskInstance historicSubTask: historicSubTasks) {
        subTaskNames.add(historicSubTask.getName());
      }

      assertThat(subTaskNames).isEqualTo(expectedSubTaskNames);
    }

    taskService.deleteTask(gonzoTaskId, true);
  }
}
