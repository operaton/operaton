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
package org.operaton.spin.plugin.variables;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.ObjectValue;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.variable.Variables.objectValue;
import static org.operaton.bpm.engine.variable.Variables.serializedObjectValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Here we test how the engine behaves, when more than one object serializers are available.
 *
 * @author Svetlana Dorokhova
 */
@ExtendWith(ProcessEngineExtension.class)
class JavaSerializationTest {
  RuntimeService runtimeService;
  TaskService taskService;
  protected static final String ONE_TASK_PROCESS = "org/operaton/spin/plugin/oneTaskProcess.bpmn20.xml";

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void serializationAsJava() {
      ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

      JavaSerializable bean = new JavaSerializable("a String", 42, true);
      // request object to be serialized as Java
      runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(bean).serializationDataFormat(Variables.SerializationDataFormats.JAVA).create());

      // validate untyped value
      Object value = runtimeService.getVariable(instance.getId(), "simpleBean");
      assertThat(value).isEqualTo(bean);

      // validate typed value
      ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
      assertThat(typedValue.getType()).isEqualTo(ValueType.OBJECT);

      assertThat(typedValue.isDeserialized()).isTrue();

      assertThat(typedValue.getValue()).isEqualTo(bean);
      assertThat(typedValue.getValue(JavaSerializable.class)).isEqualTo(bean);
      assertThat(typedValue.getObjectType()).isEqualTo(JavaSerializable.class);

      assertThat(typedValue.getSerializationDataFormat()).isEqualTo(Variables.SerializationDataFormats.JAVA.getName());
      assertThat(typedValue.getObjectTypeName()).isEqualTo(JavaSerializable.class.getName());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void javaSerializedValuesAreProhibited() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = instance.getId();
    // request object to be serialized as Java
    var serializedObjectValue = serializedObjectValue("").serializationDataFormat(Variables.SerializationDataFormats.JAVA).create();

    assertThatThrownBy(() -> runtimeService.setVariable(processInstanceId, "simpleBean", serializedObjectValue))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("ENGINE-17007 Cannot set variable with name simpleBean. Java serialization format is prohibited");
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void javaSerializedValuesAreProhibitedForTransient() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = instance.getId();
    // request object to be serialized as Java
    var serializedObjectValue = serializedObjectValue("").serializationDataFormat(Variables.SerializationDataFormats.JAVA).create();

    assertThatThrownBy(() -> runtimeService.setVariable(processInstanceId, "simpleBean", serializedObjectValue))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("ENGINE-17007 Cannot set variable with name simpleBean. Java serialization format is prohibited");
  }

  @Test
  void standaloneTaskVariable() {
    Task task = taskService.newTask();
    task.setName("gonzoTask");
    taskService.saveTask(task);

    String taskId = task.getId();
    var serializedObjectValue = serializedObjectValue("trumpet").serializationDataFormat(Variables.SerializationDataFormats.JAVA).create();

    assertThatThrownBy(() -> taskService.setVariable(taskId, "instrument", serializedObjectValue))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("ENGINE-17007 Cannot set variable with name instrument. Java serialization format is prohibited");

    taskService.deleteTask(taskId, true);
  }

  @Test
  void standaloneTaskTransientVariable() {
    Task task = taskService.newTask();
    task.setName("gonzoTask");
    taskService.saveTask(task);

    String taskId = task.getId();
    var serializedObjectValue = serializedObjectValue("trumpet")
      .serializationDataFormat(Variables.SerializationDataFormats.JAVA).setTransient(true)
      .create();

    assertThatThrownBy(() -> taskService.setVariable(taskId, "instrument", serializedObjectValue))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("ENGINE-17007 Cannot set variable with name instrument. Java serialization format is prohibited");

    taskService.deleteTask(taskId, true);
  }

}
