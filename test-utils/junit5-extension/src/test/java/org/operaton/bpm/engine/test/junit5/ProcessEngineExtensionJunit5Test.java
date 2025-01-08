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
package org.operaton.bpm.engine.test.junit5;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ProcessEngineExtension.class)
class ProcessEngineExtensionJunit5Test {

  ProcessEngine engine;

  @Test
  @Deployment
  void extensionUsageExample() {
    RuntimeService runtimeService = engine.getRuntimeService();
    runtimeService.startProcessInstanceByKey("extensionUsage");

    TaskService taskService = engine.getTaskService();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("My Task");

    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
  }

  /**
   * The extension should work with tests that have no deployment annotation
   */
  @Test
  void testWithoutDeploymentAnnotation() {
    assertThat(engine).isNotNull();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
  void requiredHistoryLevelAudit() {

    assertThat(currentHistoryLevel()).isIn(
        ProcessEngineConfiguration.HISTORY_AUDIT, ProcessEngineConfiguration.HISTORY_FULL);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_ACTIVITY)
  void requiredHistoryLevelActivity() {

    assertThat(currentHistoryLevel()).isIn(
        ProcessEngineConfiguration.HISTORY_ACTIVITY,
        ProcessEngineConfiguration.HISTORY_AUDIT,
        ProcessEngineConfiguration.HISTORY_FULL);
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void requiredHistoryLevelFull() {

    assertThat(currentHistoryLevel()).isEqualTo(ProcessEngineConfiguration.HISTORY_FULL);
  }

  protected String currentHistoryLevel() {
    return engine.getProcessEngineConfiguration().getHistory();
  }

}
