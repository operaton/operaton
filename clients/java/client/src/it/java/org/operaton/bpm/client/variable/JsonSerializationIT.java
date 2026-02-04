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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;

import org.operaton.bpm.client.ExternalTaskClient;
import org.operaton.bpm.client.dto.ProcessDefinitionDto;
import org.operaton.bpm.client.dto.ProcessInstanceDto;
import org.operaton.bpm.client.exception.ValueMapperException;
import org.operaton.bpm.client.rule.ClientRule;
import org.operaton.bpm.client.rule.EngineRule;
import org.operaton.bpm.client.task.ExternalTask;
import org.operaton.bpm.client.task.ExternalTaskService;
import org.operaton.bpm.client.util.RecordingExternalTaskHandler;
import org.operaton.bpm.client.util.RecordingInvocationHandler;
import org.operaton.bpm.client.util.RecordingInvocationHandler.RecordedInvocation;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;

import static org.operaton.bpm.client.util.ProcessModels.EXTERNAL_TASK_TOPIC_BAR;
import static org.operaton.bpm.client.util.ProcessModels.EXTERNAL_TASK_TOPIC_FOO;
import static org.operaton.bpm.client.util.ProcessModels.TWO_EXTERNAL_TASK_PROCESS;
import static org.operaton.bpm.engine.variable.Variables.SerializationDataFormats.JSON;
import static org.operaton.bpm.engine.variable.type.ValueType.OBJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonSerializationIT {

  protected static final String VARIABLE_NAME_JSON = "jsonVariable";
  protected static final String JSON_DATAFORMAT_NAME = JSON.getName();

  protected static final JsonSerializable VARIABLE_VALUE_JSON_DESERIALIZED = new JsonSerializable("a String", 42, true);

  protected static final String VARIABLE_VALUE_JSON_SERIALIZED = VARIABLE_VALUE_JSON_DESERIALIZED.toExpectedJsonString();
  protected static final String VARIABLE_VALUE_JSON_LIST_SERIALIZED = String.format("[%s, %s]", VARIABLE_VALUE_JSON_SERIALIZED, VARIABLE_VALUE_JSON_SERIALIZED);

  protected static final ObjectValue VARIABLE_VALUE_JSON_OBJECT_VALUE = Variables
      .serializedObjectValue(VARIABLE_VALUE_JSON_DESERIALIZED.toExpectedJsonString())
      .objectTypeName(JsonSerializable.class.getName())
      .serializationDataFormat(JSON_DATAFORMAT_NAME)
      .create();

  protected static final ObjectValue VARIABLE_VALUE_JSON_LIST_OBJECT_VALUE = Variables
      .serializedObjectValue(VARIABLE_VALUE_JSON_LIST_SERIALIZED)
      .objectTypeName(String.format("java.util.ArrayList<%s>", JsonSerializable.class.getName()))
      .serializationDataFormat(JSON_DATAFORMAT_NAME)
      .create();

  @RegisterExtension
  static ClientRule clientRule = new ClientRule();
  @RegisterExtension
  static EngineRule engineRule = new EngineRule();

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
  void shouldGetDeserializedJson() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, VARIABLE_VALUE_JSON_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    JsonSerializable variableValue = task.getVariable(VARIABLE_NAME_JSON);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_JSON_DESERIALIZED);
  }

  @Test
  void shouldGetTypedDeserializedJson() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, VARIABLE_VALUE_JSON_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_JSON);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_JSON_DESERIALIZED);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(JsonSerializable.class.getName());
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isTrue();
  }

  @Test
  void shouldGetTypedSerializedJson() throws JSONException {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, VARIABLE_VALUE_JSON_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_JSON, false);
    JSONAssert.assertEquals(VARIABLE_VALUE_JSON_DESERIALIZED.toExpectedJsonString(), new String(typedValue.getValueSerialized()), true);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(JsonSerializable.class.getName());
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isFalse();
  }

  @Test
  void shouldGetJsonAsList() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, VARIABLE_VALUE_JSON_LIST_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    List<JsonSerializable> variableValue = task.getVariable(VARIABLE_NAME_JSON);
    assertThat(variableValue).hasSize(2);
    assertThat(variableValue).containsExactly(VARIABLE_VALUE_JSON_DESERIALIZED, VARIABLE_VALUE_JSON_DESERIALIZED);
  }

  @Test
  void shouldGetTypedDeserializedJsonAsList() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, VARIABLE_VALUE_JSON_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_JSON);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_JSON_DESERIALIZED);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(JsonSerializable.class.getName());
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isTrue();
  }

  @Test
  void shouldGetTypedSerializedJsonAsList() throws JSONException {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, VARIABLE_VALUE_JSON_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_JSON, false);
    JSONAssert.assertEquals(VARIABLE_VALUE_JSON_DESERIALIZED.toExpectedJsonString(), new String(typedValue.getValueSerialized()), true);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(JsonSerializable.class.getName());
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isFalse();
  }

  @Test
  void shouldFailWhileDeserialization() {
    // given
    ObjectValue objectValue = Variables.serializedObjectValue(VARIABLE_VALUE_JSON_SERIALIZED)
      .objectTypeName(FailingDeserializationBean.class.getName())
      .serializationDataFormat(JSON_DATAFORMAT_NAME)
      .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, objectValue);

    // when + then

    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
            .handler(handler)
            .open();

    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);
    assertThatThrownBy(() ->
            task.getVariable(VARIABLE_NAME_JSON)
    ).isInstanceOf(ValueMapperException.class);
  }

  @Test
  void shouldFailWhileDeserializationTypedValue() {
    // given
    ObjectValue objectValue = Variables.serializedObjectValue(VARIABLE_VALUE_JSON_SERIALIZED)
      .objectTypeName(FailingDeserializationBean.class.getName())
      .serializationDataFormat(JSON_DATAFORMAT_NAME)
      .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, objectValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
            .handler(handler)
            .open();

    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);
    assertThatThrownBy(() ->
            task.getVariable(VARIABLE_NAME_JSON)
    ).isInstanceOf(ValueMapperException.class);
  }

  @Test
  void shouldStillReturnSerializedJsonWhenDeserializationFails() throws JSONException {
    // given
    ObjectValue objectValue = Variables.serializedObjectValue(VARIABLE_VALUE_JSON_SERIALIZED)
      .objectTypeName(FailingDeserializationBean.class.getName())
      .serializationDataFormat(JSON_DATAFORMAT_NAME)
      .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, objectValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    assertThatThrownBy(() -> task.getVariableTyped(VARIABLE_NAME_JSON))
      .isInstanceOf(Exception.class);

    // However, the serialized value can be accessed
    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_JSON, false);
    JSONAssert.assertEquals(VARIABLE_VALUE_JSON_DESERIALIZED.toExpectedJsonString(), new String(typedValue.getValueSerialized()), true);
    assertThat(typedValue.getObjectTypeName()).isNotNull();
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isFalse();

    // but not the deserialized properties
    assertThatThrownBy(typedValue::getValue)
      .isInstanceOf(IllegalStateException.class);

    assertThatThrownBy(() -> typedValue.getValue(JsonSerializable.class))
      .isInstanceOf(IllegalStateException.class);

    assertThatThrownBy(typedValue::getObjectType)
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldFailWhileDeserializationDueToMismatchingTypeName() throws JSONException {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue(VARIABLE_VALUE_JSON_SERIALIZED)
      .serializationDataFormat(JSON_DATAFORMAT_NAME)
      .objectTypeName("Insensible type name")  // < not a valid type name
      .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO).handler(handler).open();

    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    // then
    assertThatThrownBy(() ->
      task.getVariable(VARIABLE_NAME_JSON)
    ).isInstanceOf(ValueMapperException.class);
  }

  @Test
  void shouldFailWhileDeserializationDueToWrongTypeName() throws JSONException {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue(VARIABLE_VALUE_JSON_SERIALIZED)
      .serializationDataFormat(JSON_DATAFORMAT_NAME)
      .objectTypeName(JsonSerializationIT.class.getName())  // < not the right type name
      .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
            .handler(handler)
            .open();

    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    assertThatThrownBy(() ->
            task.getVariable(VARIABLE_NAME_JSON)
    ).isInstanceOf(ValueMapperException.class);
  }

  @Test
  void shouldDeserializeNull() throws JSONException {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue()
        .serializationDataFormat(JSON_DATAFORMAT_NAME)
        .objectTypeName(JsonSerializable.class.getName())
        .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    JsonSerializable returnedBean = task.getVariable(VARIABLE_NAME_JSON);
    assertThat(returnedBean).isNull();
  }

  @Test
  void shouldDeserializeNullTyped() throws JSONException {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue()
        .serializationDataFormat(JSON_DATAFORMAT_NAME)
        .objectTypeName(JsonSerializable.class.getName())
        .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_JSON);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getObjectTypeName()).isEqualTo(JsonSerializable.class.getName());
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isTrue();
  }

  @Test
  void shouldDeserializeNullWithoutTypeName()  {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue()
        .serializationDataFormat(JSON_DATAFORMAT_NAME)
        .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    JsonSerializable returnedBean = task.getVariable(VARIABLE_NAME_JSON);
    assertThat(returnedBean).isNull();
  }

  @Test
  void shouldDeserializeNullTypedWithoutTypeName()  {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue()
        .serializationDataFormat(JSON_DATAFORMAT_NAME)
        .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_JSON, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_JSON);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getObjectTypeName()).isNull();
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isTrue();
  }

  @Test
  void shoudSetVariable() throws JSONException {
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
    variables.put(VARIABLE_NAME_JSON, VARIABLE_VALUE_JSON_DESERIALIZED);
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue serializedValue = task.getVariableTyped(VARIABLE_NAME_JSON, false);
    assertThat(serializedValue.isDeserialized()).isFalse();
    JSONAssert.assertEquals(VARIABLE_VALUE_JSON_SERIALIZED, new String(serializedValue.getValueSerialized()), true);
    assertThat(serializedValue.getType()).isEqualTo(OBJECT);
    assertThat(serializedValue.getObjectTypeName()).isEqualTo(JsonSerializable.class.getName());

    ObjectValue deserializedValue = task.getVariableTyped(VARIABLE_NAME_JSON);
    assertThat(deserializedValue.isDeserialized()).isTrue();
    assertThat(deserializedValue.getValue()).isEqualTo(VARIABLE_VALUE_JSON_DESERIALIZED);
    assertThat(deserializedValue.getType()).isEqualTo(OBJECT);
    assertThat(deserializedValue.getObjectTypeName()).isEqualTo(JsonSerializable.class.getName());

    JsonSerializable variableValue = task.getVariable(VARIABLE_NAME_JSON);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_JSON_DESERIALIZED);
  }

  @Test
  void shoudSetVariableTyped() throws JSONException {
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
    variables.put(VARIABLE_NAME_JSON, VARIABLE_VALUE_JSON_OBJECT_VALUE);
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue serializedValue = task.getVariableTyped(VARIABLE_NAME_JSON, false);
    assertThat(serializedValue.isDeserialized()).isFalse();
    JSONAssert.assertEquals(VARIABLE_VALUE_JSON_DESERIALIZED.toExpectedJsonString(), new String(serializedValue.getValueSerialized()), true);
    assertThat(serializedValue.getType()).isEqualTo(OBJECT);
    assertThat(serializedValue.getObjectTypeName()).isEqualTo(JsonSerializable.class.getName());

    ObjectValue deserializedValue = task.getVariableTyped(VARIABLE_NAME_JSON);
    assertThat(deserializedValue.isDeserialized()).isTrue();
    assertThat(deserializedValue.getValue()).isEqualTo(VARIABLE_VALUE_JSON_DESERIALIZED);
    assertThat(deserializedValue.getType()).isEqualTo(OBJECT);
    assertThat(deserializedValue.getObjectTypeName()).isEqualTo(JsonSerializable.class.getName());

    JsonSerializable variableValue = task.getVariable(VARIABLE_NAME_JSON);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_JSON_DESERIALIZED);
  }

  @Test
  void shoudSetVariableUntyped() throws JSONException {
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
    variables.put(VARIABLE_NAME_JSON, Variables.untypedValue(VARIABLE_VALUE_JSON_DESERIALIZED));
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue serializedValue = task.getVariableTyped(VARIABLE_NAME_JSON, false);
    assertThat(serializedValue.isDeserialized()).isFalse();
    JSONAssert.assertEquals(VARIABLE_VALUE_JSON_DESERIALIZED.toExpectedJsonString(), new String(serializedValue.getValueSerialized()), true);
    assertThat(serializedValue.getType()).isEqualTo(OBJECT);
    assertThat(serializedValue.getObjectTypeName()).isEqualTo(JsonSerializable.class.getName());

    ObjectValue deserializedValue = task.getVariableTyped(VARIABLE_NAME_JSON);
    assertThat(deserializedValue.isDeserialized()).isTrue();
    assertThat(deserializedValue.getValue()).isEqualTo(VARIABLE_VALUE_JSON_DESERIALIZED);
    assertThat(deserializedValue.getType()).isEqualTo(OBJECT);
    assertThat(deserializedValue.getObjectTypeName()).isEqualTo(JsonSerializable.class.getName());

    JsonSerializable variableValue = task.getVariable(VARIABLE_NAME_JSON);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_JSON_DESERIALIZED);
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
    variables.put(VARIABLE_NAME_JSON, Variables.objectValue(null)
        .serializationDataFormat(JSON)
        .create());
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue serializedValue = task.getVariableTyped(VARIABLE_NAME_JSON, false);
    assertThat(serializedValue.isDeserialized()).isFalse();
    assertThat(serializedValue.getValueSerialized()).isNull();
    assertThat(serializedValue.getType()).isEqualTo(OBJECT);
    assertThat(serializedValue.getObjectTypeName()).isNull();

    ObjectValue deserializedValue = task.getVariableTyped(VARIABLE_NAME_JSON);
    assertThat(deserializedValue.isDeserialized()).isTrue();
    assertThat(deserializedValue.getValue()).isNull();
    assertThat(deserializedValue.getType()).isEqualTo(OBJECT);
    assertThat(deserializedValue.getObjectTypeName()).isNull();

    JsonSerializable variableValue = task.getVariable(VARIABLE_NAME_JSON);
    assertThat(variableValue).isNull();
  }

  @Test
  void shoudSetJsonListVariable() throws JSONException {
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
    List<JsonSerializable> variable = new ArrayList<>();
    variable.add(VARIABLE_VALUE_JSON_DESERIALIZED);
    variable.add(VARIABLE_VALUE_JSON_DESERIALIZED);

    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME_JSON, variable);
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue serializedValue = task.getVariableTyped(VARIABLE_NAME_JSON, false);
    assertThat(serializedValue.isDeserialized()).isFalse();
    JSONAssert.assertEquals(VARIABLE_VALUE_JSON_LIST_SERIALIZED, new String(serializedValue.getValueSerialized()), true);
    assertThat(serializedValue.getType()).isEqualTo(OBJECT);
    assertThat(serializedValue.getObjectTypeName()).isEqualTo("java.util.ArrayList<org.operaton.bpm.client.variable.JsonSerializable>");

    ObjectValue deserializedValue = task.getVariableTyped(VARIABLE_NAME_JSON);
    assertThat(deserializedValue.isDeserialized()).isTrue();
    assertThat(deserializedValue.getValue()).isEqualTo(variable);
    assertThat(deserializedValue.getType()).isEqualTo(OBJECT);
    assertThat(deserializedValue.getObjectTypeName()).isEqualTo("java.util.ArrayList<org.operaton.bpm.client.variable.JsonSerializable>");

    List<JsonSerializable> variableValue = task.getVariable(VARIABLE_NAME_JSON);
    assertThat(variableValue).isEqualTo(variable);
  }

  @Test
  void shoudSetJsonListVariableTyped() throws JSONException {
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
    List<JsonSerializable> variable = new ArrayList<>();
    variable.add(VARIABLE_VALUE_JSON_DESERIALIZED);
    variable.add(VARIABLE_VALUE_JSON_DESERIALIZED);

    Map<String, Object> variables = Variables.createVariables();
    variables.put(VARIABLE_NAME_JSON, Variables.objectValue(variable).create());
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue serializedValue = task.getVariableTyped(VARIABLE_NAME_JSON, false);
    assertThat(serializedValue.isDeserialized()).isFalse();
    JSONAssert.assertEquals(VARIABLE_VALUE_JSON_LIST_SERIALIZED, new String(serializedValue.getValueSerialized()), true);
    assertThat(serializedValue.getType()).isEqualTo(OBJECT);
    assertThat(serializedValue.getObjectTypeName()).isEqualTo("java.util.ArrayList<org.operaton.bpm.client.variable.JsonSerializable>");

    ObjectValue deserializedValue = task.getVariableTyped(VARIABLE_NAME_JSON);
    assertThat(deserializedValue.isDeserialized()).isTrue();
    assertThat(deserializedValue.getValue()).isEqualTo(variable);
    assertThat(deserializedValue.getType()).isEqualTo(OBJECT);
    assertThat(deserializedValue.getObjectTypeName()).isEqualTo("java.util.ArrayList<org.operaton.bpm.client.variable.JsonSerializable>");

    List<JsonSerializable> variableValue = task.getVariable(VARIABLE_NAME_JSON);
    assertThat(variableValue).isEqualTo(variable);
  }

  @Test
  void shouldFailWithMapperNotFound() {
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

    // when + then
    Map<String, Object> variables = Variables.createVariables();
    ObjectValue objectValue = Variables.objectValue(VARIABLE_VALUE_JSON_DESERIALIZED)
        .serializationDataFormat("not existing data format")
        .create();
    variables.put(VARIABLE_NAME_JSON, objectValue);
    assertThatThrownBy(() -> fooService.complete(fooTask, variables))
            .isInstanceOf(ValueMapperException.class);
  }

}
