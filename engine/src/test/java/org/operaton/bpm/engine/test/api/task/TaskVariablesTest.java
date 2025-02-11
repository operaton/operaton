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
package org.operaton.bpm.engine.test.api.task;

import static org.operaton.bpm.engine.variable.Variables.objectValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.StringValue;

import org.junit.Test;


/**
 * @author Tom Baeyens
 */
public class TaskVariablesTest extends PluggableProcessEngineTest {

  @Test
  public void testStandaloneTaskVariables() {
    Task task = taskService.newTask();
    task.setName("gonzoTask");
    taskService.saveTask(task);

    String taskId = task.getId();
    taskService.setVariable(taskId, "instrument", "trumpet");
    assertThat(taskService.getVariable(taskId, "instrument")).isEqualTo("trumpet");

    taskService.deleteTask(taskId, true);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/task/TaskVariablesTest.testTaskExecutionVariables.bpmn20.xml"})
  @Test
  public void testTaskExecutionVariableLongValue() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();

    StringBuffer longString = new StringBuffer();
    longString.append("tensymbols".repeat(500));
    try {
      runtimeService.setVariable(processInstanceId, "var", longString.toString());
    } catch (Exception ex) {
      if (!(ex instanceof BadUserRequestException)) {
        fail("BadUserRequestException is expected, but another exception was received:  " + ex);
      }
      assertThat(ex.getMessage()).isEqualTo("Variable value is too long");
    }

  }

  @Deployment
  @Test
  public void testTaskExecutionVariables() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();
    String taskId = taskService.createTaskQuery().singleResult().getId();

    Map<String, Object> expectedVariables = new HashMap<>();
    assertThat(runtimeService.getVariables(processInstanceId)).isEqualTo(expectedVariables);
    assertThat(taskService.getVariables(taskId)).isEqualTo(expectedVariables);
    assertThat(runtimeService.getVariablesLocal(processInstanceId)).isEqualTo(expectedVariables);
    assertThat(taskService.getVariablesLocal(taskId)).isEqualTo(expectedVariables);

    runtimeService.setVariable(processInstanceId, "instrument", "trumpet");

    expectedVariables = new HashMap<>();
    assertThat(taskService.getVariablesLocal(taskId)).isEqualTo(expectedVariables);
    expectedVariables.put("instrument", "trumpet");
    assertThat(runtimeService.getVariables(processInstanceId)).isEqualTo(expectedVariables);
    assertThat(taskService.getVariables(taskId)).isEqualTo(expectedVariables);
    assertThat(runtimeService.getVariablesLocal(processInstanceId)).isEqualTo(expectedVariables);

    taskService.setVariable(taskId, "player", "gonzo");

    expectedVariables = new HashMap<>();
    assertThat(taskService.getVariablesLocal(taskId)).isEqualTo(expectedVariables);
    expectedVariables.put("player", "gonzo");
    expectedVariables.put("instrument", "trumpet");
    assertThat(runtimeService.getVariables(processInstanceId)).isEqualTo(expectedVariables);
    assertThat(taskService.getVariables(taskId)).isEqualTo(expectedVariables);
    assertThat(runtimeService.getVariablesLocal(processInstanceId)).isEqualTo(expectedVariables);
    assertThat(runtimeService.getVariablesLocal(processInstanceId, null)).isEqualTo(expectedVariables);
    assertThat(runtimeService.getVariablesLocalTyped(processInstanceId, null, true)).isEqualTo(expectedVariables);

    taskService.setVariableLocal(taskId, "budget", "unlimited");

    expectedVariables = new HashMap<>();
    expectedVariables.put("budget", "unlimited");
    assertThat(taskService.getVariablesLocal(taskId)).isEqualTo(expectedVariables);
    assertThat(taskService.getVariablesLocalTyped(taskId, true)).isEqualTo(expectedVariables);
    expectedVariables.put("player", "gonzo");
    expectedVariables.put("instrument", "trumpet");
    assertThat(taskService.getVariables(taskId)).isEqualTo(expectedVariables);
    assertThat(taskService.getVariablesTyped(taskId, true)).isEqualTo(expectedVariables);

    assertThat(taskService.getVariables(taskId, null)).isEqualTo(expectedVariables);
    assertThat(taskService.getVariablesTyped(taskId, null, true)).isEqualTo(expectedVariables);

    expectedVariables = new HashMap<>();
    expectedVariables.put("player", "gonzo");
    expectedVariables.put("instrument", "trumpet");
    assertThat(runtimeService.getVariables(processInstanceId)).isEqualTo(expectedVariables);
    assertThat(runtimeService.getVariablesLocal(processInstanceId)).isEqualTo(expectedVariables);


    // typed variable API

    ArrayList<String> serializableValue = new ArrayList<>();
    serializableValue.add("1");
    serializableValue.add("2");
    taskService.setVariable(taskId, "objectVariable", objectValue(serializableValue).create());

    ArrayList<String> serializableValueLocal = new ArrayList<>();
    serializableValueLocal.add("3");
    serializableValueLocal.add("4");
    taskService.setVariableLocal(taskId, "objectVariableLocal", objectValue(serializableValueLocal).create());

    Object value = taskService.getVariable(taskId, "objectVariable");
    assertThat(value).isEqualTo(serializableValue);

    Object valueLocal = taskService.getVariableLocal(taskId, "objectVariableLocal");
    assertThat(valueLocal).isEqualTo(serializableValueLocal);

    ObjectValue typedValue = taskService.getVariableTyped(taskId, "objectVariable");
    assertThat(typedValue.getValue()).isEqualTo(serializableValue);

    ObjectValue serializedValue = taskService.getVariableTyped(taskId, "objectVariable", false);
    assertFalse(serializedValue.isDeserialized());

    ObjectValue typedValueLocal = taskService.getVariableLocalTyped(taskId, "objectVariableLocal");
    assertThat(typedValueLocal.getValue()).isEqualTo(serializableValueLocal);

    ObjectValue serializedValueLocal = taskService.getVariableLocalTyped(taskId, "objectVariableLocal", false);
    assertFalse(serializedValueLocal.isDeserialized());

    try {
      StringValue val = taskService.getVariableTyped(taskId, "objectVariable");
      fail("expected exception");
    }
    catch(ClassCastException e) {
      //happy path
    }

  }
}
