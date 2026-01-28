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

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
import org.operaton.spin.Spin;
import org.operaton.spin.SpinList;
import org.operaton.spin.xml.SpinXmlElement;

import static org.operaton.bpm.client.util.ProcessModels.*;
import static org.operaton.bpm.engine.variable.Variables.SerializationDataFormats.XML;
import static org.operaton.bpm.engine.variable.type.ValueType.OBJECT;
import static org.assertj.core.api.Assertions.*;

class XmlSerializationIT {

  protected static final String VARIABLE_NAME_XML = "xmlVariable";
  protected static final String XML_DATAFORMAT_NAME = XML.getName();

  protected static final XmlSerializable VARIABLE_VALUE_XML_DESERIALIZED = new XmlSerializable("a String", 42, true);
  protected static final XmlSerializables VARIABLE_VALUE_XML_LIST_DESERIALIZED = new XmlSerializables(Arrays.asList(VARIABLE_VALUE_XML_DESERIALIZED, VARIABLE_VALUE_XML_DESERIALIZED));

  protected static final XmlSerializableNoAnnotation VARIABLE_VALUE_XML_NO_ANNOTATION_DESERIALIZED = new XmlSerializableNoAnnotation("a String", 42, true);

  protected static final String VARIABLE_VALUE_XML_SERIALIZED = VARIABLE_VALUE_XML_DESERIALIZED.toExpectedXmlString();
  protected static final String VARIABLE_VALUE_XML_LIST_SERIALIZED = VARIABLE_VALUE_XML_LIST_DESERIALIZED.toExpectedXmlString();

  protected static final ObjectValue VARIABLE_VALUE_XML_OBJECT_VALUE = Variables
      .serializedObjectValue(VARIABLE_VALUE_XML_DESERIALIZED.toExpectedXmlString())
      .objectTypeName(XmlSerializable.class.getName())
      .serializationDataFormat(XML_DATAFORMAT_NAME)
      .create();

  protected static final ObjectValue VARIABLE_VALUE_XML_NO_ANNOTATION_OBJECT_VALUE = Variables
    .serializedObjectValue(VARIABLE_VALUE_XML_NO_ANNOTATION_DESERIALIZED.toExpectedXmlString())
    .objectTypeName(XmlSerializableNoAnnotation.class.getName())
    .serializationDataFormat(XML_DATAFORMAT_NAME)
    .create();

  protected static final ObjectValue VARIABLE_VALUE_XML_LIST_OBJECT_VALUE = Variables
      .serializedObjectValue(VARIABLE_VALUE_XML_LIST_SERIALIZED)
      .objectTypeName(XmlSerializables.class.getName())
      .serializationDataFormat(XML_DATAFORMAT_NAME)
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
  void shouldGetDeserializedXml() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, VARIABLE_VALUE_XML_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    XmlSerializable variableValue = task.getVariable(VARIABLE_NAME_XML);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_XML_DESERIALIZED);
  }

  @Test
  void shouldGetDeserializedXmlNoAnnotation() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, VARIABLE_VALUE_XML_NO_ANNOTATION_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    XmlSerializableNoAnnotation variableValue = task.getVariable(VARIABLE_NAME_XML);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_XML_NO_ANNOTATION_DESERIALIZED);
  }

  @Test
  void shouldGetTypedDeserializedXml() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, VARIABLE_VALUE_XML_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_XML);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_XML_DESERIALIZED);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(XmlSerializable.class.getName());
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isTrue();
  }

  @Test
  void shouldGetTypedSerializedXml() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, VARIABLE_VALUE_XML_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_XML, false);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(XmlSerializable.class.getName());
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isFalse();

    SpinXmlElement serializedValue = Spin.XML(typedValue.getValueSerialized());
    assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getStringProperty()).isEqualTo(serializedValue.childElement("stringProperty").textContent());
    assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getBooleanProperty()).isEqualTo(Boolean.parseBoolean(serializedValue.childElement("booleanProperty").textContent()));
    assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getIntProperty()).isEqualTo(Integer.parseInt(serializedValue.childElement("intProperty").textContent()));
  }

  @Test
  void shouldGetXmlAsList() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, VARIABLE_VALUE_XML_LIST_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    XmlSerializables variableValue = task.getVariable(VARIABLE_NAME_XML);
    assertThat(variableValue.size()).isEqualTo(2);
    assertThat(variableValue.get(0)).isEqualTo(VARIABLE_VALUE_XML_DESERIALIZED);
    assertThat(variableValue.get(1)).isEqualTo(VARIABLE_VALUE_XML_DESERIALIZED);
  }

  @Test
  void shouldGetTypedDeserializedXmlAsList() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, VARIABLE_VALUE_XML_LIST_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_XML);
    assertThat(typedValue.getValue()).isEqualTo(VARIABLE_VALUE_XML_LIST_DESERIALIZED);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(XmlSerializables.class.getName());
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isTrue();
  }

  @Test
  void shouldGetTypedSerializedXmlAsList() {
    // given
    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, VARIABLE_VALUE_XML_LIST_OBJECT_VALUE);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_XML, false);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(XmlSerializables.class.getName());
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isFalse();

    SpinXmlElement serializedValue = Spin.XML(typedValue.getValueSerialized());
    SpinList<SpinXmlElement> childElements = serializedValue.childElements();
    childElements.forEach(c -> {
      assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getStringProperty()).isEqualTo(c.childElement("stringProperty").textContent());
      assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getBooleanProperty()).isEqualTo(Boolean.parseBoolean(c.childElement("booleanProperty").textContent()));
      assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getIntProperty()).isEqualTo(Integer.parseInt(c.childElement("intProperty").textContent()));
    });
  }

  @Test
  void shouldFailWhileDeserialization() {
    // given
    ObjectValue objectValue = Variables.serializedObjectValue(VARIABLE_VALUE_XML_SERIALIZED)
      .objectTypeName(FailingDeserializationBean.class.getName())
      .serializationDataFormat(XML_DATAFORMAT_NAME)
      .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, objectValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
            .handler(handler)
            .open();


    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);
    // then
    assertThatThrownBy(() ->
            task.getVariable(VARIABLE_NAME_XML)
    ).isInstanceOf(ValueMapperException.class);
  }

  @Test
  void shouldFailWhileDeserializationTypedValue() {
    // given
    ObjectValue objectValue = Variables.serializedObjectValue(VARIABLE_VALUE_XML_SERIALIZED)
      .objectTypeName(FailingDeserializationBean.class.getName())
      .serializationDataFormat(XML_DATAFORMAT_NAME)
      .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, objectValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    //then
    ExternalTask task = handler.getHandledTasks().get(0);
    assertThatThrownBy(() ->
            task.getVariable(VARIABLE_NAME_XML)
    ).isInstanceOf(ValueMapperException.class);
  }

  @Test
  void shouldStillReturnSerializedXmlWhenDeserializationFails() {
    // given
    ObjectValue objectValue = Variables.serializedObjectValue(VARIABLE_VALUE_XML_SERIALIZED)
      .objectTypeName(FailingDeserializationBean.class.getName())
      .serializationDataFormat(XML_DATAFORMAT_NAME)
      .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, objectValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    try {
      task.getVariableTyped(VARIABLE_NAME_XML);
      fail("exception expected");
    }
    catch (Exception e) {
    }

    // However, the serialized value can be accessed
    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_XML, false);
    assertThat(typedValue.getObjectTypeName()).isNotNull();
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isFalse();

    SpinXmlElement serializedValue = Spin.XML(typedValue.getValueSerialized());
    assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getStringProperty()).isEqualTo(serializedValue.childElement("stringProperty").textContent());
    assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getBooleanProperty()).isEqualTo(Boolean.parseBoolean(serializedValue.childElement("booleanProperty").textContent()));
    assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getIntProperty()).isEqualTo(Integer.parseInt(serializedValue.childElement("intProperty").textContent()));

    // but not the deserialized properties
    try {
      typedValue.getValue();
      fail("exception expected");
    }
    catch(IllegalStateException e) {
    }

    try {
      typedValue.getValue(XmlSerializable.class);
      fail("exception expected");
    }
    catch(IllegalStateException e) {
    }

    try {
      typedValue.getObjectType();
      fail("exception expected");
    }
    catch(IllegalStateException e) {
    }
  }

  @Test
  void shouldFailWhileDeserializationDueToMismatchingTypeName() {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue(VARIABLE_VALUE_XML_SERIALIZED)
      .serializationDataFormat(XML_DATAFORMAT_NAME)
      .objectTypeName("Insensible type name")  // < not a valid type name
      .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    // then
    ExternalTask task = handler.getHandledTasks().get(0);
    assertThatThrownBy(() ->
            task.getVariable(VARIABLE_NAME_XML)
    ).isInstanceOf(ValueMapperException.class);
  }

  @Test
  void shouldFailWhileDeserializationDueToWrongTypeName() {
    // given

    // not reachable class
    class Foo {}

    ObjectValue serializedValue = Variables.serializedObjectValue(VARIABLE_VALUE_XML_SERIALIZED)
      .serializationDataFormat(XML_DATAFORMAT_NAME)
      .objectTypeName(Foo.class.getName())  // < not the right type name
      .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    // then
    ExternalTask task = handler.getHandledTasks().get(0);
    assertThatThrownBy(() ->
            task.getVariable(VARIABLE_NAME_XML)
    ).isInstanceOf(ValueMapperException.class);
  }

  @Test
  void shouldDeserializeNull() {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue()
        .serializationDataFormat(XML_DATAFORMAT_NAME)
        .objectTypeName(XmlSerializable.class.getName())
        .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    XmlSerializable returnedBean = task.getVariable(VARIABLE_NAME_XML);
    assertThat(returnedBean).isNull();
  }

  @Test
  void shouldDeserializeNullTyped() {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue()
        .serializationDataFormat(XML_DATAFORMAT_NAME)
        .objectTypeName(XmlSerializable.class.getName())
        .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_XML);
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getObjectTypeName()).isEqualTo(XmlSerializable.class.getName());
    assertThat(typedValue.getType()).isEqualTo(OBJECT);
    assertThat(typedValue.isDeserialized()).isTrue();
  }

  @Test
  void shouldDeserializeNullWithoutTypeName()  {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue()
        .serializationDataFormat(XML_DATAFORMAT_NAME)
        .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    XmlSerializable returnedBean = task.getVariable(VARIABLE_NAME_XML);
    assertThat(returnedBean).isNull();
  }

  @Test
  void shouldDeserializeNullTypedWithoutTypeName()  {
    // given
    ObjectValue serializedValue = Variables.serializedObjectValue()
        .serializationDataFormat(XML_DATAFORMAT_NAME)
        .create();

    engineRule.startProcessInstance(processDefinition.getId(), VARIABLE_NAME_XML, serializedValue);

    // when
    client.subscribe(EXTERNAL_TASK_TOPIC_FOO)
      .handler(handler)
      .open();

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue typedValue = task.getVariableTyped(VARIABLE_NAME_XML);
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
    variables.put(VARIABLE_NAME_XML, Variables.objectValue(VARIABLE_VALUE_XML_DESERIALIZED).serializationDataFormat(XML).create());
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue serializedValue = task.getVariableTyped(VARIABLE_NAME_XML, false);
    assertThat(serializedValue.isDeserialized()).isFalse();
    assertThat(serializedValue.getType()).isEqualTo(OBJECT);
    assertThat(serializedValue.getObjectTypeName()).isEqualTo(XmlSerializable.class.getName());

    SpinXmlElement spinElement = Spin.XML(serializedValue.getValueSerialized());
    assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getStringProperty()).isEqualTo(spinElement.childElement("stringProperty").textContent());
    assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getBooleanProperty()).isEqualTo(Boolean.parseBoolean(spinElement.childElement("booleanProperty").textContent()));
    assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getIntProperty()).isEqualTo(Integer.parseInt(spinElement.childElement("intProperty").textContent()));

    ObjectValue deserializedValue = task.getVariableTyped(VARIABLE_NAME_XML);
    assertThat(deserializedValue.isDeserialized()).isTrue();
    assertThat(deserializedValue.getValue()).isEqualTo(VARIABLE_VALUE_XML_DESERIALIZED);
    assertThat(deserializedValue.getType()).isEqualTo(OBJECT);
    assertThat(deserializedValue.getObjectTypeName()).isEqualTo(XmlSerializable.class.getName());

    XmlSerializable variableValue = task.getVariable(VARIABLE_NAME_XML);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_XML_DESERIALIZED);
  }

  @Test
  void shoudSetVariableTypedNoAnnotation() {
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
    variables.put(VARIABLE_NAME_XML, Variables.objectValue(VARIABLE_VALUE_XML_NO_ANNOTATION_DESERIALIZED).serializationDataFormat(XML).create());
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue serializedValue = task.getVariableTyped(VARIABLE_NAME_XML, false);
    assertThat(serializedValue.isDeserialized()).isFalse();
    assertThat(serializedValue.getType()).isEqualTo(OBJECT);
    assertThat(serializedValue.getObjectTypeName()).isEqualTo(XmlSerializableNoAnnotation.class.getName());

    SpinXmlElement spinElement = Spin.XML(serializedValue.getValueSerialized());
    assertThat(VARIABLE_VALUE_XML_NO_ANNOTATION_DESERIALIZED.getStringProperty()).isEqualTo(spinElement.childElement("stringProperty").textContent());
    assertThat(VARIABLE_VALUE_XML_NO_ANNOTATION_DESERIALIZED.getBooleanProperty()).isEqualTo(Boolean.parseBoolean(spinElement.childElement("booleanProperty").textContent()));
    assertThat(VARIABLE_VALUE_XML_NO_ANNOTATION_DESERIALIZED.getIntProperty()).isEqualTo(Integer.parseInt(spinElement.childElement("intProperty").textContent()));

    ObjectValue deserializedValue = task.getVariableTyped(VARIABLE_NAME_XML);
    assertThat(deserializedValue.isDeserialized()).isTrue();
    assertThat(deserializedValue.getValue()).isEqualTo(VARIABLE_VALUE_XML_NO_ANNOTATION_DESERIALIZED);
    assertThat(deserializedValue.getType()).isEqualTo(OBJECT);
    assertThat(deserializedValue.getObjectTypeName()).isEqualTo(XmlSerializableNoAnnotation.class.getName());

    XmlSerializableNoAnnotation variableValue = task.getVariable(VARIABLE_NAME_XML);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_XML_NO_ANNOTATION_DESERIALIZED);
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
    variables.put(VARIABLE_NAME_XML, Variables.objectValue(null)
        .serializationDataFormat(XML)
        .create());
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue serializedValue = task.getVariableTyped(VARIABLE_NAME_XML, false);
    assertThat(serializedValue.isDeserialized()).isFalse();
    assertThat(serializedValue.getValueSerialized()).isNull();
    assertThat(serializedValue.getType()).isEqualTo(OBJECT);
    assertThat(serializedValue.getObjectTypeName()).isNull();

    ObjectValue deserializedValue = task.getVariableTyped(VARIABLE_NAME_XML);
    assertThat(deserializedValue.isDeserialized()).isTrue();
    assertThat(deserializedValue.getValue()).isNull();
    assertThat(deserializedValue.getType()).isEqualTo(OBJECT);
    assertThat(deserializedValue.getObjectTypeName()).isNull();

    XmlSerializable variableValue = task.getVariable(VARIABLE_NAME_XML);
    assertThat(variableValue).isNull();
  }

  @Test
  void shoudSetXmlListVariable() {
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
    variables.put(VARIABLE_NAME_XML, Variables.objectValue(VARIABLE_VALUE_XML_LIST_DESERIALIZED).serializationDataFormat(XML).create());
    fooService.complete(fooTask, variables);

    // then
    clientRule.waitForFetchAndLockUntil(() -> !handler.getHandledTasks().isEmpty());

    ExternalTask task = handler.getHandledTasks().get(0);

    ObjectValue serializedValue = task.getVariableTyped(VARIABLE_NAME_XML, false);
    assertThat(serializedValue.isDeserialized()).isFalse();
    assertThat(serializedValue.getType()).isEqualTo(OBJECT);
    assertThat(serializedValue.getObjectTypeName()).isEqualTo("org.operaton.bpm.client.variable.XmlSerializables");

    SpinXmlElement spinElement = Spin.XML(serializedValue.getValueSerialized());
    SpinList<SpinXmlElement> childElements = spinElement.childElements();
    childElements.forEach(c -> {
      assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getStringProperty()).isEqualTo(c.childElement("stringProperty").textContent());
      assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getBooleanProperty()).isEqualTo(Boolean.parseBoolean(c.childElement("booleanProperty").textContent()));
      assertThat(VARIABLE_VALUE_XML_DESERIALIZED.getIntProperty()).isEqualTo(Integer.parseInt(c.childElement("intProperty").textContent()));
    });

    ObjectValue deserializedValue = task.getVariableTyped(VARIABLE_NAME_XML);
    assertThat(deserializedValue.isDeserialized()).isTrue();
    assertThat(deserializedValue.getValue()).isEqualTo(VARIABLE_VALUE_XML_LIST_DESERIALIZED);
    assertThat(deserializedValue.getType()).isEqualTo(OBJECT);
    assertThat(deserializedValue.getObjectTypeName()).isEqualTo("org.operaton.bpm.client.variable.XmlSerializables");

    XmlSerializables variableValue = task.getVariable(VARIABLE_NAME_XML);
    assertThat(variableValue).isEqualTo(VARIABLE_VALUE_XML_LIST_DESERIALIZED);
  }

}
