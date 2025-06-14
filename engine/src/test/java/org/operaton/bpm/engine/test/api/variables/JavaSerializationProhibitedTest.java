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
package org.operaton.bpm.engine.test.api.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.test.util.TypedValueAssert.assertObjectValueDeserialized;
import static org.operaton.bpm.engine.test.util.TypedValueAssert.assertObjectValueSerializedJava;
import static org.operaton.bpm.engine.variable.Variables.objectValue;
import static org.operaton.bpm.engine.variable.Variables.serializedObjectValue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.impl.variable.serializer.JavaObjectSerializer;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * @author Svetlana Dorokhova
 */
class JavaSerializationProhibitedTest {

  protected static final String ONE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/variables/oneTaskProcess.bpmn20.xml";

  protected static final String JAVA_DATA_FORMAT = Variables.SerializationDataFormats.JAVA.getName();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;

  @BeforeEach
  void init() {
    ((ProcessEngineConfigurationImpl) engineRule.getProcessEngine().getProcessEngineConfiguration())
        .getVariableSerializers()
        .addSerializer(new JavaCustomSerializer());
  }

  //still works for normal objects (not serialized)
  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void testSetJavaObject() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    JavaSerializable javaSerializable = new JavaSerializable("foo");
    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(javaSerializable).serializationDataFormat(JAVA_DATA_FORMAT).create());

    // validate untyped value
    JavaSerializable value = (JavaSerializable) runtimeService.getVariable(instance.getId(), "simpleBean");

    assertThat(value).isEqualTo(javaSerializable);

    // validate typed value
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertObjectValueDeserialized(typedValue, javaSerializable);
    assertObjectValueSerializedJava(typedValue, javaSerializable);
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void testSetJavaObjectSerialized() throws Exception {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    JavaSerializable javaSerializable = new JavaSerializable("foo");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.getEncoder().encode(baos.toByteArray()),
        engineRule.getProcessEngine());
    ObjectValue serializedJavaObject = serializedObjectValue(serializedObject)
      .serializationDataFormat(JAVA_DATA_FORMAT)
      .objectTypeName(JavaSerializable.class.getName())
      .create();

    // when/then
    assertThatThrownBy(() -> runtimeService.setVariable(processInstanceId, "simpleBean",
      serializedJavaObject))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot set variable with name simpleBean. Java serialization format is prohibited");

  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void testSetJavaObjectSerializedEmptySerializationDataFormat() throws Exception {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    JavaSerializable javaSerializable = new JavaSerializable("foo");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.getEncoder().encode(baos.toByteArray()),
        engineRule.getProcessEngine());
    ObjectValue serializedObjectValue = serializedObjectValue(serializedObject)
      .objectTypeName(JavaSerializable.class.getName())
      .create();

    // when/then
    assertThatThrownBy(() -> runtimeService.setVariable(processInstanceId, "simpleBean",
      serializedObjectValue))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot set variable with name simpleBean. Java serialization format is prohibited");

  }

  @Test
  void testStandaloneTaskTransientVariableSerializedObject() {
    // given
    Task task = taskService.newTask();
    task.setName("gonzoTask");
    taskService.saveTask(task);
    String taskId = task.getId();
    ObjectValue serializedObjectValue = serializedObjectValue("any value")
      .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
      .setTransient(true)
      .create();

    try {
      // when/then
      assertThatThrownBy(() -> taskService.setVariable(taskId, "instrument",
        serializedObjectValue))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Cannot set variable with name instrument. Java serialization format is prohibited");
    } finally {
      taskService.deleteTask(taskId, true);
    }

  }

  private static class JavaCustomSerializer extends JavaObjectSerializer {

    @Override
    protected boolean canWriteValue(TypedValue typedValue) {
      //do NOT check serializationDataFormat
      return true;
    }
  }
}
