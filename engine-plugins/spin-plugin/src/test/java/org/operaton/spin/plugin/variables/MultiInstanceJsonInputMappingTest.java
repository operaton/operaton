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
package org.operaton.spin.plugin.variables;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.junit5.DeploymentExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.spin.json.SpinJsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.spin.plugin.variable.SpinValues.jsonValue;

class MultiInstanceJsonInputMappingTest {

  private static final String PROCESS_KEY = "jsonCollectionInputMapping";
  private static final String LABEL_EXPRESSION = "${item.prop('label').stringValue()}";
  private static final String EXECUTION_LABEL_EXPRESSION =
      "${execution.getVariable('item').prop('label').stringValue()}";

  @RegisterExtension
  static ProcessEngineExtension engineExtension = ProcessEngineExtension.builder().build();

  @RegisterExtension
  DeploymentExtension deploymentExtension = new DeploymentExtension(engineExtension.getRepositoryService());

  private RuntimeService runtimeService;
  private TaskService taskService;

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void mapsJsonArrayElements(boolean sequential) {
    deploymentExtension.deploy(createProcess(sequential, false, LABEL_EXPRESSION, EXECUTION_LABEL_EXPRESSION));

    String processInstanceId = startProcess("[{\"label\":\"Alpha\"},{\"label\":\"Beta\"}]");

    assertMappedValuesAndComplete(processInstanceId, sequential, List.of("Alpha", "Beta"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void keepsJsonElementAvailableAfterSameNameInputParameter(boolean sequential) {
    deploymentExtension.deploy(createProcess(sequential, true, LABEL_EXPRESSION, EXECUTION_LABEL_EXPRESSION));

    String processInstanceId = startProcess("[{\"label\":\"Alpha\"},{\"label\":\"Beta\"}]");

    assertMappedValuesAndComplete(processInstanceId, sequential, List.of("Alpha", "Beta"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void mapsJsonNullElementInsteadOfParentElement(boolean sequential) {
    String expression = "${item.isNull() ? 'null element' : item.prop('label').stringValue()}";
    String executionExpression = "${execution.getVariable('item').isNull() ? 'null element'"
        + " : execution.getVariable('item').prop('label').stringValue()}";
    deploymentExtension.deploy(createProcess(sequential, false, expression, executionExpression));

    String processInstanceId = startProcess("[null,{\"label\":\"Beta\"}]");

    assertMappedValuesAndComplete(processInstanceId, sequential, List.of("null element", "Beta"));
  }

  private BpmnModelInstance createProcess(boolean sequential, boolean overwriteElement, String expression,
      String executionExpression) {
    var taskBuilder = Bpmn.createExecutableProcess(PROCESS_KEY).startEvent().userTask("miTask");
    if (overwriteElement) {
      taskBuilder.operatonInputParameter("item", "mapped item");
    }
    taskBuilder.operatonInputParameter("mappedLabel", expression)
        .operatonInputParameter("mappedExecutionLabel", executionExpression)
        .operatonInputParameter("mappedIndex", "${loopCounter}")
        .operatonInputParameter("mappedExecutionIndex", "${execution.getVariable('loopCounter')}");

    var loopBuilder = taskBuilder.multiInstance()
        .operatonCollection("${items.elements()}")
        .operatonElementVariable("item");
    if (sequential) {
      loopBuilder.sequential();
    } else {
      loopBuilder.parallel();
    }
    return loopBuilder.multiInstanceDone().endEvent().done();
  }

  private String startProcess(String jsonArray) {
    return runtimeService.startProcessInstanceByKey(PROCESS_KEY, Variables.createVariables()
        .putValueTyped("items", jsonValue(jsonArray).create())
        .putValueTyped("item", jsonValue("{\"label\":\"Parent\"}").create())
        .putValue("loopCounter", -1)).getId();
  }

  private void assertMappedValuesAndComplete(String processInstanceId, boolean sequential, List<String> expected) {
    Set<Integer> visitedIndexes = new HashSet<>();
    List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
    assertThat(tasks).hasSize(sequential ? 1 : expected.size());

    while (!tasks.isEmpty()) {
      for (Task task : tasks) {
        String executionId = task.getExecutionId();
        int index = (Integer) runtimeService.getVariableLocal(executionId, "loopCounter");
        assertThat(visitedIndexes.add(index)).isTrue();
        assertThat(runtimeService.getVariableLocal(executionId, "mappedLabel")).isEqualTo(expected.get(index));
        assertThat(runtimeService.getVariableLocal(executionId, "mappedExecutionLabel"))
            .isEqualTo(expected.get(index));
        assertThat(runtimeService.getVariableLocal(executionId, "mappedIndex")).isEqualTo(index);
        assertThat(runtimeService.getVariableLocal(executionId, "mappedExecutionIndex")).isEqualTo(index);

        SpinJsonNode parentElement = (SpinJsonNode) runtimeService.getVariableLocal(processInstanceId, "item");
        assertThat(parentElement.prop("label").stringValue()).isEqualTo("Parent");
        assertThat(runtimeService.getVariableLocal(processInstanceId, "loopCounter")).isEqualTo(-1);
        assertThat(runtimeService.getVariablesLocal(processInstanceId)).doesNotContainKey("mappedLabel");

        taskService.complete(task.getId());
      }
      tasks = taskService.createTaskQuery().processInstanceId(processInstanceId).list();
    }

    assertThat(visitedIndexes).hasSize(expected.size());
    assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count()).isZero();
  }
}
