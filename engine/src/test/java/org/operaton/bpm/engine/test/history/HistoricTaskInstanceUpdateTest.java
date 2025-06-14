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
package org.operaton.bpm.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricTaskInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;


/**
 * @author Frederik Heremans
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
@ExtendWith(ProcessEngineExtension.class)
class HistoricTaskInstanceUpdateTest {

  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;

  @Deployment
  @Test
  void testHistoricTaskInstanceUpdate() {
    runtimeService.startProcessInstanceByKey("HistoricTaskInstanceTest").getId();

    Task task = taskService.createTaskQuery().singleResult();

    // Update and save the task's fields before it is finished
    task.setPriority(12345);
    task.setDescription("Updated description");
    task.setName("Updated name");
    task.setAssignee("gonzo");
    taskService.saveTask(task);

    taskService.complete(task.getId());
    assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(1);

    HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().singleResult();
    assertThat(historicTaskInstance.getName()).isEqualTo("Updated name");
    assertThat(historicTaskInstance.getDescription()).isEqualTo("Updated description");
    assertThat(historicTaskInstance.getAssignee()).isEqualTo("gonzo");
    assertThat(historicTaskInstance.getTaskDefinitionKey()).isEqualTo("task");
  }
}
