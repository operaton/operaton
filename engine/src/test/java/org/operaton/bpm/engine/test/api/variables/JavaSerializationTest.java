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
package org.operaton.bpm.engine.test.api.variables;

import static org.assertj.core.api.Assertions.*;
import static org.operaton.bpm.engine.test.util.TypedValueAssert.assertObjectValueDeserialized;
import static org.operaton.bpm.engine.test.util.TypedValueAssert.assertObjectValueDeserializedNull;
import static org.operaton.bpm.engine.test.util.TypedValueAssert.assertObjectValueSerializedJava;
import static org.operaton.bpm.engine.test.util.TypedValueAssert.assertObjectValueSerializedNull;
import static org.operaton.bpm.engine.test.util.TypedValueAssert.assertUntypedNullValue;
import static org.operaton.bpm.engine.variable.Variables.objectValue;
import static org.operaton.bpm.engine.variable.Variables.serializedObjectValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.digest._apacheCommonsCodec.Base64;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.ProcessEngineBootstrapRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class JavaSerializationTest {

  protected static final String ONE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/variables/oneTaskProcess.bpmn20.xml";

  protected static final String JAVA_DATA_FORMAT = Variables.SerializationDataFormats.JAVA.getName();

  @ClassRule
  public static ProcessEngineBootstrapRule bootstrapRule = new ProcessEngineBootstrapRule(configuration ->
      configuration.setJavaSerializationFormatEnabled(true));
  protected ProvidedProcessEngineRule engineRule = new ProvidedProcessEngineRule(bootstrapRule);
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  private RuntimeService runtimeService;
  private TaskService taskService;

  @Before
  public void init() {
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSerializationAsJava() {
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
  public void testSetJavaObjectSerialized() throws Exception {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    JavaSerializable javaSerializable = new JavaSerializable("foo");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.encodeBase64(baos.toByteArray()), engineRule.getProcessEngine());

    runtimeService.setVariable(instance.getId(), "simpleBean",
        serializedObjectValue(serializedObject)
        .serializationDataFormat(JAVA_DATA_FORMAT)
        .objectTypeName(JavaSerializable.class.getName())
        .create());

    // validate untyped value
    JavaSerializable value = (JavaSerializable) runtimeService.getVariable(instance.getId(), "simpleBean");
    assertThat(value).isEqualTo(javaSerializable);

    // validate typed value
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertObjectValueDeserialized(typedValue, javaSerializable);
    assertObjectValueSerializedJava(typedValue, javaSerializable);
  }

  @Test
  @Deployment
  public void testJavaObjectDeserializedInFirstCommand() throws Exception {

    // this test makes sure that if a serialized value is set, it can be deserialized in the same command in which it is set.

    // given
    // a serialized Java Object
    JavaSerializable javaSerializable = new JavaSerializable("foo");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.encodeBase64(baos.toByteArray()), engineRule.getProcessEngine());

    // if
    // I start a process instance in which a Java Delegate reads the value in its deserialized form
    VariableMap variables = Variables.createVariables()
      .putValue("varName", serializedObjectValue(serializedObject).serializationDataFormat(JAVA_DATA_FORMAT)
        .objectTypeName(JavaSerializable.class.getName())
        .create());

    // then
    // it does not fail
    assertThatCode(() -> runtimeService.startProcessInstanceByKey("oneTaskProcess", variables))
      .doesNotThrowAnyException();
  }

  @Test
  @Deployment
  public void testJavaObjectNotDeserializedIfNotRequested() throws Exception {

    // this test makes sure that if a serialized value is set, it is not automatically deserialized if deserialization is not requested

    // given
    // a serialized Java Object
    FailingJavaSerializable javaSerializable = new FailingJavaSerializable("foo");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    byte[] serializedObjectBytes = baos.toByteArray();
    String serializedObject = StringUtil.fromBytes(Base64.encodeBase64(serializedObjectBytes), engineRule.getProcessEngine());

    // which cannot be deserialized
    ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(serializedObjectBytes));
    assertThatThrownBy(objectInputStream::readObject)
      .isInstanceOf(RuntimeException.class)
      .hasMessageContaining("Exception while deserializing object");

    // if
    // I start a process instance in which a Java Delegate reads the value in its serialized form
    runtimeService.startProcessInstanceByKey("oneTaskProcess", Variables.createVariables()
      .putValue("varName", serializedObjectValue(serializedObject)
        .serializationDataFormat(JAVA_DATA_FORMAT)
        .objectTypeName(JavaSerializable.class.getName())
        .create()));

    // then
    // it does not fail
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSetJavaObjectNullDeserialized() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // set null value as "deserialized" object
    runtimeService.setVariable(instance.getId(), "nullObject",
        objectValue(null)
        .serializationDataFormat(JAVA_DATA_FORMAT)
        .create());

    // get null value via untyped api
    assertNull(runtimeService.getVariable(instance.getId(), "nullObject"));

    // get null via typed api
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject");
    assertObjectValueDeserializedNull(typedValue);

  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSetJavaObjectNullSerialized() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // set null value as "serialized" object
    runtimeService.setVariable(instance.getId(), "nullObject",
        serializedObjectValue()
        .serializationDataFormat(JAVA_DATA_FORMAT)
        .create()); // Note: no object type name provided

    // get null value via untyped api
    assertNull(runtimeService.getVariable(instance.getId(), "nullObject"));

    // get null via typed api
    ObjectValue deserializedTypedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject");
    assertObjectValueDeserializedNull(deserializedTypedValue);

    ObjectValue serializedTypedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject", false);
    assertObjectValueSerializedNull(serializedTypedValue);
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSetJavaObjectNullSerializedObjectTypeName() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    String typeName = "some.type.Name";

    // set null value as "serialized" object
    runtimeService.setVariable(instance.getId(), "nullObject",
        serializedObjectValue()
        .serializationDataFormat(JAVA_DATA_FORMAT)
        .objectTypeName(typeName) // This time an objectTypeName is provided
        .create());

    // get null value via untyped api
    assertNull(runtimeService.getVariable(instance.getId(), "nullObject"));

    // get null via typed api
    ObjectValue deserializedTypedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject");
    assertNotNull(deserializedTypedValue);
    assertTrue(deserializedTypedValue.isDeserialized());
    assertThat(deserializedTypedValue.getSerializationDataFormat()).isEqualTo(JAVA_DATA_FORMAT);
    assertNull(deserializedTypedValue.getValue());
    assertNull(deserializedTypedValue.getValueSerialized());
    assertNull(deserializedTypedValue.getObjectType());
    assertThat(deserializedTypedValue.getObjectTypeName()).isEqualTo(typeName);

    ObjectValue serializedTypedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject", false);
    assertNotNull(serializedTypedValue);
    assertFalse(serializedTypedValue.isDeserialized());
    assertThat(serializedTypedValue.getSerializationDataFormat()).isEqualTo(JAVA_DATA_FORMAT);
    assertNull(serializedTypedValue.getValueSerialized());
    assertThat(serializedTypedValue.getObjectTypeName()).isEqualTo(typeName);
  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSetUntypedNullForExistingVariable() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // initially the variable has a value
    JavaSerializable javaSerializable = new JavaSerializable("foo");

    runtimeService.setVariable(instance.getId(), "varName",
        objectValue(javaSerializable)
        .serializationDataFormat(JAVA_DATA_FORMAT)
        .create());

    // get value via untyped api
    assertThat(runtimeService.getVariable(instance.getId(), "varName")).isEqualTo(javaSerializable);

    // set the variable to null via untyped Api
    runtimeService.setVariable(instance.getId(), "varName", null);

    // variable is now untyped null
    TypedValue nullValue = runtimeService.getVariableTyped(instance.getId(), "varName");
    assertUntypedNullValue(nullValue);

  }

  @Test
  @Deployment(resources = ONE_TASK_PROCESS)
  public void testSetTypedNullForExistingVariable() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // initially the variable has a value
    JavaSerializable javaSerializable = new JavaSerializable("foo");

    runtimeService.setVariable(instance.getId(), "varName",
        objectValue(javaSerializable)
        .serializationDataFormat(JAVA_DATA_FORMAT)
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
  public void testStandaloneTaskTransientVariable() throws IOException {
    Task task = taskService.newTask();
    task.setName("gonzoTask");
    taskService.saveTask(task);
    String taskId = task.getId();
    try (var baos = new ByteArrayOutputStream(); var oos = new ObjectOutputStream(baos)) {
      oos.writeObject("trumpet");
      String serializedObject = StringUtil.fromBytes(Base64.encodeBase64(baos.toByteArray()), engineRule.getProcessEngine());

      taskService.setVariable(taskId, "instrument",
        serializedObjectValue(serializedObject)
          .objectTypeName(String.class.getName())
          .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
          .create());
      assertThat(taskService.getVariable(taskId, "instrument")).isEqualTo("trumpet");
    } finally {
      taskService.deleteTask(taskId, true);
    }

  }

  @Test
  public void testTransientObjectValue() throws IOException {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("foo")
        .startEvent()
        .exclusiveGateway("gtw")
          .sequenceFlowId("flow1")
          .condition("cond", "${x.property == \"bar\"}")
          .userTask("userTask1")
          .endEvent()
        .moveToLastGateway()
          .sequenceFlowId("flow2")
          .userTask("userTask2")
          .endEvent()
        .done();

    testRule.deploy(modelInstance);

    JavaSerializable bean = new JavaSerializable("bar");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(bean);
    String serializedObject = StringUtil.fromBytes(Base64.encodeBase64(baos.toByteArray()), engineRule.getProcessEngine());
    ObjectValue javaValue = serializedObjectValue(serializedObject, true)
        .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
        .objectTypeName(JavaSerializable.class.getName())
        .create();
    VariableMap variables = Variables.createVariables().putValueTyped("x", javaValue);

    // when
    runtimeService.startProcessInstanceByKey("foo", variables);

    // then
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().list();
    assertThat(variableInstances.size()).isEqualTo(0);

    Task task = taskService.createTaskQuery().singleResult();
    assertNotNull(task);
    assertThat(task.getTaskDefinitionKey()).isEqualTo("userTask1");
  }

}
