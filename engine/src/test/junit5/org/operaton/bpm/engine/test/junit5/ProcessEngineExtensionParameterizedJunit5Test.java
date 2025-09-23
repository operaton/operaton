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
package org.operaton.bpm.engine.test.junit5;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ProcessEngineExtension.class)
class ProcessEngineExtensionParameterizedJunit5Test {

  ProcessEngine engine;

  @ParameterizedTest
  @ValueSource(ints = {1, 2})
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

  @ParameterizedTest
  @ValueSource(strings = {"A", "B"})
  @Deployment(resources = "org/operaton/bpm/engine/test/junit5/ProcessEngineExtensionParameterizedJunit5Test.extensionUsageExample.bpmn20.xml")
  void extensionUsageExampleWithNamedAnnotation(String value) {
    Map<String,Object> variables = new HashMap<>();
    variables.put("key", value);
    RuntimeService runtimeService = engine.getRuntimeService();
    runtimeService.startProcessInstanceByKey("extensionUsage", variables);

    TaskService taskService = engine.getTaskService();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("My Task");

    taskService.complete(task.getId());
    assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();

    HistoryService historyService = engine.getHistoryService();
    HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertThat(variableInstance.getValue()).isEqualTo(value);
  }

  /**
   * The rule should work with tests that have no deployment annotation
   */
  @ParameterizedTest
  @EmptySource
  void testWithoutDeploymentAnnotation(String argument) {
    assertThat(argument).isEmpty();
  }

}
