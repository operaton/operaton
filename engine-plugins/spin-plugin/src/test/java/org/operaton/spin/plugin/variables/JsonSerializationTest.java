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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.skyscreamer.jsonassert.JSONAssert;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.DeploymentExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.bpm.engine.variable.value.builder.SerializedObjectValueBuilder;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.spin.DataFormats;

import static org.operaton.bpm.engine.variable.Variables.objectValue;
import static org.operaton.bpm.engine.variable.Variables.serializedObjectValue;
import static org.operaton.spin.plugin.variables.TypedValueAssert.assertObjectValueDeserializedNull;
import static org.operaton.spin.plugin.variables.TypedValueAssert.assertObjectValueSerializedNull;
import static org.operaton.spin.plugin.variables.TypedValueAssert.assertUntypedNullValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(ProcessEngineExtension.class)
class JsonSerializationTest {

  protected static final String ONE_TASK_PROCESS = "org/operaton/spin/plugin/oneTaskProcess.bpmn20.xml";
  protected static final String SERVICE_TASK_PROCESS = "org/operaton/spin/plugin/serviceTaskProcess.bpmn20.xml";

  protected static final String JSON_FORMAT_NAME = DataFormats.JSON_DATAFORMAT_NAME;

  @RegisterExtension
  DeploymentExtension deploymentExtension = new DeploymentExtension();

  RuntimeService runtimeService;
  TaskService taskService;
  ProcessEngineConfiguration processEngineConfiguration;


  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void serializationAsJson() throws Exception {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    JsonSerializable bean = new JsonSerializable("a String", 42, true);
    // request object to be serialized as JSON
    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(bean).serializationDataFormat(JSON_FORMAT_NAME).create());

    // validate untyped value
    Object value = runtimeService.getVariable(instance.getId(), "simpleBean");
    assertThat(value).isEqualTo(bean);

    // validate typed value
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertThat(typedValue.getType()).isEqualTo(ValueType.OBJECT);

    assertThat(typedValue.isDeserialized()).isTrue();

    assertThat(typedValue.getValue()).isEqualTo(bean);
    assertThat(typedValue.getValue(JsonSerializable.class)).isEqualTo(bean);
    assertThat(typedValue.getObjectType()).isEqualTo(JsonSerializable.class);

    assertThat(typedValue.getSerializationDataFormat()).isEqualTo(JSON_FORMAT_NAME);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(JsonSerializable.class.getName());
    JSONAssert.assertEquals(bean.toExpectedJsonString(), typedValue.getValueSerialized(), true);
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void listSerializationAsJson() throws Exception {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    List<JsonSerializable> beans = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      beans.add(new JsonSerializable("a String" + i, 42 + i, true));
    }

    runtimeService.setVariable(instance.getId(), "simpleBeans", objectValue(beans).serializationDataFormat(JSON_FORMAT_NAME).create());

    // validate untyped value
    Object value = runtimeService.getVariable(instance.getId(), "simpleBeans");
    assertThat(value).isEqualTo(beans);

    // validate typed value
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBeans");
    assertThat(typedValue.getType()).isEqualTo(ValueType.OBJECT);
    assertThat(typedValue.getValue()).isEqualTo(beans);
    assertThat(typedValue.isDeserialized()).isTrue();
    assertThat(typedValue.getSerializationDataFormat()).isEqualTo(JSON_FORMAT_NAME);
    assertThat(typedValue.getObjectTypeName()).isNotNull();
    JSONAssert.assertEquals(toExpectedJsonArray(beans), typedValue.getValueSerialized(), true);

  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void failingSerialization() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    FailingSerializationBean failingBean = new FailingSerializationBean("a String", 42, true);

    String instanceId = instance.getId();
    var value = objectValue(failingBean).serializationDataFormat(JSON_FORMAT_NAME).create();
    assertThatThrownBy(() -> runtimeService.setVariable(instanceId, "simpleBean", value))
      .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void failingDeserialization() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String instanceId = instance.getId();

    FailingDeserializationBean failingBean = new FailingDeserializationBean("a String", 42, true);

    runtimeService.setVariable(instanceId, "simpleBean", objectValue(failingBean).serializationDataFormat(JSON_FORMAT_NAME));

    assertThatThrownBy(() -> runtimeService.getVariable(instanceId, "simpleBean"))
            .isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> runtimeService.getVariableTyped(instanceId, "simpleBean"))
            .isInstanceOf(ProcessEngineException.class);

    // However, I can access the serialized value
    ObjectValue objectValue = runtimeService.getVariableTyped(instanceId, "simpleBean", false);
    assertThat(objectValue.isDeserialized()).isFalse();
    assertThat(objectValue.getObjectTypeName()).isNotNull();
    assertThat(objectValue.getValueSerialized()).isNotNull();

    // but not the deserialized properties
    assertThatThrownBy(objectValue::getValue)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Object is not deserialized");

    assertThatThrownBy(() -> objectValue.getValue(JsonSerializable.class))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Object is not deserialized");

    assertThatThrownBy(objectValue::getObjectType)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Object is not deserialized");
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void failForNonExistingSerializationFormat() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String instanceId = instance.getId();

    JsonSerializable jsonSerializable = new JsonSerializable();

    var objectValue = objectValue(jsonSerializable).serializationDataFormat("non existing data format").create();
    assertThatThrownBy(() -> runtimeService.setVariable(instanceId, "simpleBean", objectValue))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot find serializer for value");
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void variableValueCaching() {
    final ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    ((ProcessEngineConfigurationImpl)processEngineConfiguration).getCommandExecutorTxRequired().execute(
      (Command<Void>) commandContext -> {
        JsonSerializable bean = new JsonSerializable("a String", 42, true);
        runtimeService.setVariable(instance.getId(), "simpleBean", bean);

        Object returnedBean = runtimeService.getVariable(instance.getId(), "simpleBean");
        assertThat(returnedBean).isSameAs(bean);

        return null;
      });

    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().singleResult();

    Object returnedBean = variableInstance.getValue();
    Object theSameReturnedBean = variableInstance.getValue();
    assertThat(theSameReturnedBean).isSameAs(returnedBean);
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void getSerializedVariableValue() throws Exception {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    JsonSerializable bean = new JsonSerializable("a String", 42, true);
    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(bean).serializationDataFormat(JSON_FORMAT_NAME).create());

    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean", false);

    String serializedValue = typedValue.getValueSerialized();
    JSONAssert.assertEquals(bean.toExpectedJsonString(), serializedValue, true);
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void setSerializedVariableValue() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    JsonSerializable bean = new JsonSerializable("a String", 42, true);
    String beanAsJson = bean.toExpectedJsonString();

    SerializedObjectValueBuilder serializedValue = serializedObjectValue(beanAsJson)
      .serializationDataFormat(JSON_FORMAT_NAME)
      .objectTypeName(bean.getClass().getCanonicalName());

    runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);

    // java object can be retrieved
    JsonSerializable returnedBean = (JsonSerializable) runtimeService.getVariable(instance.getId(), "simpleBean");
    assertThat(returnedBean).isEqualTo(bean);

    // validate typed value metadata
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertThat(typedValue.getValue()).isEqualTo(bean);
    assertThat(typedValue.getSerializationDataFormat()).isEqualTo(JSON_FORMAT_NAME);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(bean.getClass().getCanonicalName());
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void setSerializedVariableValueNoTypeName() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    JsonSerializable bean = new JsonSerializable("a String", 42, true);
    String beanAsJson = bean.toExpectedJsonString();

    SerializedObjectValueBuilder serializedValue = serializedObjectValue(beanAsJson)
      .serializationDataFormat(JSON_FORMAT_NAME);
      // no type name

    assertThatThrownBy(() -> runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue))
      .isInstanceOf(Exception.class)
      .hasMessageContaining("no 'objectTypeName' provided for non-null value");
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void setSerializedVariableValueMismatchingTypeName() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String instanceId = instance.getId();
    JsonSerializable bean = new JsonSerializable("a String", 42, true);
    String beanAsJson = bean.toExpectedJsonString();

    SerializedObjectValueBuilder serializedValue = serializedObjectValue(beanAsJson)
      .serializationDataFormat(JSON_FORMAT_NAME)
      .objectTypeName("Insensible type name."); // < not a valid type name

    runtimeService.setVariable(instanceId, "simpleBean", serializedValue);

    assertThatThrownBy(() -> runtimeService.getVariable(instanceId, "simpleBean"))
            .isInstanceOf(ProcessEngineException.class);

    serializedValue = serializedObjectValue(beanAsJson)
      .serializationDataFormat(JSON_FORMAT_NAME)
      .objectTypeName(JsonSerializationTest.class.getName()); // < not the right type name

    runtimeService.setVariable(instanceId, "simpleBean", serializedValue);

    assertThatThrownBy(() -> runtimeService.getVariable(instanceId, "simpleBean"))
            .isInstanceOf(ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void setSerializedVariableValueNull() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    SerializedObjectValueBuilder serializedValue = serializedObjectValue()
      .serializationDataFormat(JSON_FORMAT_NAME)
      .objectTypeName(JsonSerializable.class.getCanonicalName());

    runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);

    // null can be retrieved
    JsonSerializable returnedBean = (JsonSerializable) runtimeService.getVariable(instance.getId(), "simpleBean");
    assertThat(returnedBean).isNull();

    // validate typed value metadata
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getValueSerialized()).isNull();
    assertThat(typedValue.getSerializationDataFormat()).isEqualTo(JSON_FORMAT_NAME);
    assertThat(typedValue.getObjectTypeName()).isEqualTo(JsonSerializable.class.getCanonicalName());
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void setSerializedVariableValueNullNoTypeName() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    SerializedObjectValueBuilder serializedValue = serializedObjectValue()
      .serializationDataFormat(JSON_FORMAT_NAME);
    // no objectTypeName specified

    runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);

    // null can be retrieved
    JsonSerializable returnedBean = (JsonSerializable) runtimeService.getVariable(instance.getId(), "simpleBean");
    assertThat(returnedBean).isNull();

    // validate typed value metadata
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertThat(typedValue.getValue()).isNull();
    assertThat(typedValue.getValueSerialized()).isNull();
    assertThat(typedValue.getSerializationDataFormat()).isEqualTo(JSON_FORMAT_NAME);
    assertThat(typedValue.getObjectTypeName()).isNull();
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void setJavaOjectNullDeserialized() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // set null value as "deserialized" object
    runtimeService.setVariable(instance.getId(), "nullObject",
        objectValue(null)
        .serializationDataFormat(JSON_FORMAT_NAME)
        .create());

    // get null value via untyped api
    assertThat(runtimeService.getVariable(instance.getId(), "nullObject")).isNull();

    // get null via typed api
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject");
    assertObjectValueDeserializedNull(typedValue);

  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void setJavaOjectNullSerialized() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // set null value as "serialized" object
    runtimeService.setVariable(instance.getId(), "nullObject",
        serializedObjectValue()
        .serializationDataFormat(JSON_FORMAT_NAME)
        .create()); // Note: no object type name provided

    // get null value via untyped api
    assertThat(runtimeService.getVariable(instance.getId(), "nullObject")).isNull();

    // get null via typed api
    ObjectValue deserializedTypedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject");
    assertObjectValueDeserializedNull(deserializedTypedValue);

    ObjectValue serializedTypedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject", false);
    assertObjectValueSerializedNull(serializedTypedValue);
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void setJavaOjectNullSerializedObjectTypeName() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    String typeName = "some.type.Name";

    // set null value as "serialized" object
    runtimeService.setVariable(instance.getId(), "nullObject",
        serializedObjectValue()
        .serializationDataFormat(JSON_FORMAT_NAME)
        .objectTypeName(typeName) // This time an objectTypeName is provided
        .create());

    // get null value via untyped api
    assertThat(runtimeService.getVariable(instance.getId(), "nullObject")).isNull();

    // get null via typed api
    ObjectValue deserializedTypedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject");
    assertThat(deserializedTypedValue).isNotNull();
    assertThat(deserializedTypedValue.isDeserialized()).isTrue();
    assertThat(deserializedTypedValue.getSerializationDataFormat()).isEqualTo(JSON_FORMAT_NAME);
    assertThat(deserializedTypedValue.getValue()).isNull();
    assertThat(deserializedTypedValue.getValueSerialized()).isNull();
    assertThat(deserializedTypedValue.getObjectType()).isNull();
    assertThat(deserializedTypedValue.getObjectTypeName()).isEqualTo(typeName);

    ObjectValue serializedTypedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject", false);
    assertThat(serializedTypedValue).isNotNull();
    assertThat(serializedTypedValue.isDeserialized()).isFalse();
    assertThat(serializedTypedValue.getSerializationDataFormat()).isEqualTo(JSON_FORMAT_NAME);
    assertThat(serializedTypedValue.getValueSerialized()).isNull();
    assertThat(serializedTypedValue.getObjectTypeName()).isEqualTo(typeName);
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void setUntypedNullForExistingVariable() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // initially the variable has a value
    JsonSerializable object = new JsonSerializable();

    runtimeService.setVariable(instance.getId(), "varName",
        objectValue(object)
        .serializationDataFormat(JSON_FORMAT_NAME)
        .create());

    // get value via untyped api
    assertThat(runtimeService.getVariable(instance.getId(), "varName")).isEqualTo(object);

    // set the variable to null via untyped Api
    runtimeService.setVariable(instance.getId(), "varName", null);

    // variable is now untyped null
    TypedValue nullValue = runtimeService.getVariableTyped(instance.getId(), "varName");
    assertUntypedNullValue(nullValue);
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void setTypedNullForExistingVariable() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // initially the variable has a value
    JsonSerializable javaSerializable = new JsonSerializable();

    runtimeService.setVariable(instance.getId(), "varName",
        objectValue(javaSerializable)
        .serializationDataFormat(JSON_FORMAT_NAME)
        .create());

    // get value via untyped api
    assertThat(runtimeService.getVariable(instance.getId(), "varName")).isEqualTo(javaSerializable);

    // set the variable to null via typed Api
    runtimeService.setVariable(instance.getId(), "varName", objectValue(null));

    // variable is still of type object
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "varName");
    assertObjectValueDeserializedNull(typedValue);
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  void removeVariable() {
    // given a serialized json variable
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    JsonSerializable bean = new JsonSerializable("a String", 42, true);
    String beanAsJson = bean.toExpectedJsonString();

    SerializedObjectValueBuilder serializedValue = serializedObjectValue(beanAsJson)
      .serializationDataFormat(JSON_FORMAT_NAME)
      .objectTypeName(bean.getClass().getCanonicalName());

    runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);

    // when
    runtimeService.removeVariable(instance.getId(), "simpleBean");

    // then
    assertThat(runtimeService.getVariable(instance.getId(), "simpleBean")).isNull();
    assertThat((Object) runtimeService.getVariableTyped(instance.getId(), "simpleBean")).isNull();
    assertThat((Object) runtimeService.getVariableTyped(instance.getId(), "simpleBean", false)).isNull();
  }

  /**
   * CAM-3222
   */
  @Test
  @Deployment(resources = SERVICE_TASK_PROCESS)
  void implicitlyUpdateEmptyList() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("serviceTaskProcess",
        Variables.createVariables()
          .putValueTyped("listVar",
            Variables.objectValue(new ArrayList<JsonSerializable>())
              .serializationDataFormat("application/json").create())
          .putValue("delegate", new UpdateValueDelegate()));

    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "listVar");
    // this should match Jackson's format
    String expectedTypeName = ArrayList.class.getName() + "<" + JsonSerializable.class.getName() + ">";
    assertThat(typedValue.getObjectTypeName()).isEqualTo(expectedTypeName);

    List<JsonSerializable> list = (List<JsonSerializable>) typedValue.getValue();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.get(0) instanceof JsonSerializable).isTrue();
    assertThat(list.get(0).getStringProperty()).isEqualTo(UpdateValueDelegate.STRING_PROPERTY);
  }

  @Test
  void transientJsonValue() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("foo")
        .startEvent()
        .exclusiveGateway("gtw")
          .sequenceFlowId("flow1")
          .condition("cond", "${x.stringProperty == \"bar\"}")
          .userTask("userTask1")
          .endEvent()
        .moveToLastGateway()
          .sequenceFlowId("flow2")
          .userTask("userTask2")
          .endEvent()
        .done();

    deploymentExtension.deploy(modelInstance);

    JsonSerializable bean = new JsonSerializable("bar", 42, true);
    ObjectValue jsonValue = serializedObjectValue(bean.toExpectedJsonString(), true)
        .serializationDataFormat(JSON_FORMAT_NAME)
        .objectTypeName(JsonSerializable.class.getName())
        .create();
    VariableMap variables = Variables.createVariables().putValueTyped("x", jsonValue);

    // when
    runtimeService.startProcessInstanceByKey("foo", variables);

    // then
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().list();
    assertThat(variableInstances.size()).isEqualTo(0);

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask1");
  }

  protected String toExpectedJsonArray(List<JsonSerializable> beans) {
    StringBuilder jsonBuilder = new StringBuilder();

    jsonBuilder.append("[");
    for (int i = 0; i < beans.size(); i++) {
      jsonBuilder.append(beans.get(i).toExpectedJsonString());

      if (i != beans.size() - 1)  {
        jsonBuilder.append(", ");
      }
    }
    jsonBuilder.append("]");

    return jsonBuilder.toString();
  }

}
