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
package org.operaton.bpm.engine.test.bpmn.sequenceflow;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Sebastian Menski
 */
class ConditionalScriptSequenceFlowTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;

  @Deployment
  @Test
  void testScriptExpression() {
    String[] directions = new String[] { "left", "right" };
    Map<String, Object> variables = new HashMap<>();

    for (String direction : directions) {
      variables.put("foo", direction);
      runtimeService.startProcessInstanceByKey("process", variables);

      Task task = taskService.createTaskQuery().singleResult();
      assertThat(task.getTaskDefinitionKey()).isEqualTo(direction);
      taskService.complete(task.getId());
    }

  }

  @Deployment
  @Test
  void testScriptExpressionWithNonBooleanResult() {
    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process"))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("condition script returns non-Boolean");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/sequenceflow/ConditionalScriptSequenceFlowTest.testScriptResourceExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/sequenceflow/condition-left.groovy"
  })
  @Test
  void testScriptResourceExpression() {
    String[] directions = new String[] { "left", "right" };
    Map<String, Object> variables = new HashMap<>();

    for (String direction : directions) {
      variables.put("foo", direction);
      runtimeService.startProcessInstanceByKey("process", variables);

      Task task = taskService.createTaskQuery().singleResult();
      assertThat(task.getTaskDefinitionKey()).isEqualTo(direction);
      taskService.complete(task.getId());
    }

  }

}
