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
package org.operaton.bpm.client.variable;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.client.ExternalTaskClient;
import org.operaton.bpm.client.dto.ProcessDefinitionDto;
import org.operaton.bpm.client.dto.ProcessInstanceDto;
import org.operaton.bpm.client.rule.ClientRule;
import org.operaton.bpm.client.rule.EngineRule;
import org.operaton.bpm.client.task.ExternalTask;
import org.operaton.bpm.client.task.ExternalTaskService;
import org.operaton.bpm.client.util.RecordingExternalTaskHandler;
import org.operaton.bpm.client.util.RecordingInvocationHandler;
import org.operaton.bpm.client.util.RecordingInvocationHandler.RecordedInvocation;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;

import static org.operaton.bpm.client.rule.ClientRule.LOCK_DURATION;
import static org.operaton.bpm.client.util.ProcessModels.EXTERNAL_TASK_TOPIC_BAR;
import static org.operaton.bpm.client.util.ProcessModels.EXTERNAL_TASK_TOPIC_FOO;
import static org.operaton.bpm.client.util.ProcessModels.TWO_EXTERNAL_TASK_PROCESS;
import static org.operaton.bpm.client.util.PropertyUtil.DEFAULT_PROPERTIES_PATH;
import static org.operaton.bpm.client.util.PropertyUtil.OPERATON_ENGINE_NAME;
import static org.operaton.bpm.client.util.PropertyUtil.OPERATON_ENGINE_REST;
import static org.operaton.bpm.client.util.PropertyUtil.loadProperties;
import static org.operaton.bpm.engine.variable.Variables.SerializationDataFormats.JAVA;
import static org.operaton.bpm.engine.variable.type.ValueType.OBJECT;
import static org.assertj.core.api.Assertions.assertThat;

class JavaSerializationIT {

  protected static final String ENGINE_NAME = "/engine/another-engine";
  protected static final String VARIABLE_NAME_JAVA = "javaVariable";

  protected static final JavaSerializable VARIABLE_VALUE_JAVA_DESERIALIZED = new JavaSerializable("a String", 42, true);

  protected static final String VARIABLE_VALUE_JAVA_SERIALIZED = VARIABLE_VALUE_JAVA_DESERIALIZED.toExpectedByteArrayString();

  protected static final ObjectValue VARIABLE_VALUE_JAVA_OBJECT_VALUE = Variables
      .serializedObjectValue(VARIABLE_VALUE_JAVA_SERIALIZED)
      .objectTypeName(JavaSerializable.class.getName())
      .serializationDataFormat(JAVA)
      .create();

  @RegisterExtension
  static ClientRule clientRule = new ClientRule(() -> {
    Properties properties = loadProperties(DEFAULT_PROPERTIES_PATH);
    String baseUrl = properties.getProperty(OPERATON_ENGINE_REST) + ENGINE_NAME;
    return ExternalTaskClient.create()
        .baseUrl(baseUrl)
        .disableAutoFetching()
        .lockDuration(LOCK_DURATION);
  });

  @RegisterExtension
  static EngineRule engineRule = new EngineRule(() -> {
    Properties properties = loadProperties(DEFAULT_PROPERTIES_PATH);
    properties.put(OPERATON_ENGINE_NAME, ENGINE_NAME);
    return properties;
  });

  protected ExternalTaskClient client;

  protected ProcessDefinitionDto processDefinition;
  protected ProcessInstanceDto processInstance;

  protected RecordingExternalTaskHandler handler = new RecordingExternalTaskHandler();
  protected RecordingInvocationHandler invocationHandler = new RecordingInvocationHandler();

  @BeforeEach
  void setup() {
    client = clientRule.client();
    processDefinition = engineRule.deploy(TWO_EXTERNAL_TASK_PROCESS).get(0);

    handler.clear();
    invocationHandler.clear();
  }

  @Test
  void shouldGetDeserializedJava() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JAVA, VARIABLE_VALUE_JAVA_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    JavaSerializable variableValue = task.getVariable(VARIABLE_NAME_JAVA);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_JAVA_DESERIALIZED);
  }

  @Test
  void shouldGetTypedDeserializedJava() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JAVA, VARIABLE_VALUE_JAVA_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_JAVA);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_JAVA_DESERIALIZED);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(JavaSerializable.class.getName());
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isTrue();
  }

  @Test
  void shouldGetTypedSerializedJava() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JAVA, VARIABLE_VALUE_JAVA_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_JAVA, false);
    assertThat(typedValue.getValueSerialized()).isEqualTo(VARIABLE_VALUE_JAVA_SERIALIZED);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(JavaSerializable.class.getName());
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isFalse();
  }

  @Test
  void shouldDeserializeNull() {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue()
        .serializationDataFormat(JAVA)
        .objectTypeName(JavaSerializable.class.getName())
        .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JAVA, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    JavaSerializable returnedBean = task.getVariable(VARIABLE_NAME_JAVA);
    assertThat(returnedBean).isNull();
  }

  @Test
  void shouldDeserializeNullTyped() {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue()
        .serializationDataFormat(JAVA)
        .objectTypeName(JavaSerializable.class.getName())
        .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JAVA, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_JAVA);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getObjectTypeName()).isEqualTo(JavaSerializable.class.getName());
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isTrue();
  }

  @Test
  void shouldDeserializeNullWithoutTypeName()  {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue()
        .serializationDataFormat(JAVA)
        .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JAVA, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    JavaSerializable returnedBean = task.getVariable(VARIABLE_NAME_JAVA);
    assertThat(returnedBean).isNull();
  }

  @Test
  void shouldDeserializeNullTypedWithoutTypeName()  {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue()
        .serializationDataFormat(JAVA)
        .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JAVA, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_JAVA);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getObjectTypeName()).isNull();
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isTrue();
  }

  @Test
  void shoudSetVariableTyped() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME_JAVA, Variables.objectValue(VARIABLE_VALUE_JAVA_DESERIALIZED).serializationDataFormat(JAVA).create());
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue serializedValue = task.getVariableTyped(VARIABLE_NAME_JAVA, false);
    assertThat(serializedValue.isDeserialized()).isFalse();
    assertThat(serializedValue.getValueSerialized()).isEqualTo(VARIABLE_VALUE_JAVA_SERIALIZED);
    assertThat(serializedValue.getType()).isEqualTo(OBJECT);
    assertThat(serializedValue.getObjectTypeName()).isEqualTo(JavaSerializable.class.getName());

    ObjectValue deserializedValue = task.getVariableTyped(VARIABLE_NAME_JAVA);
    assertThat(deserializedValue.isDeserialized()).isTrue();
    assertThat(deserializedValue.getValue()).isEqualTo(VARIABLE_VALUE_JAVA_DESERIALIZED);
    assertThat(deserializedValue.getType()).isEqualTo(OBJECT);
    assertThat(deserializedValue.getObjectTypeName()).isEqualTo(JavaSerializable.class.getName());

    JavaSerializable variableValue = task.getVariable(VARIABLE_NAME_JAVA);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_JAVA_DESERIALIZED);
  }

  @Test
  void shoudSetVariableTyped_Null() {
    // given
    engineRule.startProcessInstance(processDefinition.getId());

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(invocationHandler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !invocationHandler.getInvocations().isEmpty());

    RecordedInvocation invocation = invocationHandler.getInvocations().get(0);
    ExternalTask fooTask = invocation.getExternalTask();
    ExternalTaskService fooService = invocation.getExternalTaskService();

    client.subscribe(EXTERNAL_TASK_TOPIC_BAR)
      .handler(handler)
      .open();

    // when
    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME_JAVA, Variables.objectValue(null)
        .serializationDataFormat(JAVA)
        .create());
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue serializedValue = task.getVariableTyped(VARIABLE_NAME_JAVA, false);
    assertThat(serializedValue.isDeserialized()).isFalse();
    assertThat(serializedValue.getValueSerialized()).isNull();
    assertThat(serializedValue.getType()).isEqualTo(OBJECT);
    assertThat(serializedValue.getObjectTypeName()).isNull();

    ObjectValue deserializedValue = task.getVariableTyped(VARIABLE_NAME_JAVA);
    assertThat(deserializedValue.isDeserialized()).isTrue();
    assertThat(deserializedValue.getValue()).isNull();
    assertThat(deserializedValue.getType()).isEqualTo(OBJECT);
    assertThat(deserializedValue.getObjectTypeName()).isNull();

    JavaSerializable variableValue = task.getVariable(VARIABLE_NAME_JAVA);
    assertThat(variableValue).isNull();
  }
}
