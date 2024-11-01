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
package org.operaton.spin.plugin.variables;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
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
import org.operaton.spin.Spin;
import org.operaton.spin.impl.util.SpinIoUtil;
import org.operaton.spin.xml.SpinXmlElement;

import java.io.Reader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.operaton.bpm.engine.variable.Variables.objectValue;
import static org.operaton.bpm.engine.variable.Variables.serializedObjectValue;
import static org.operaton.spin.plugin.variables.TypedValueAssert.*;

class XmlSerializationTest {

  protected static final String ONE_TASK_PROCESS = "org/operaton/spin/plugin/oneTaskProcess.bpmn20.xml";

  protected static final String XML_FORMAT_NAME = DataFormats.XML_DATAFORMAT_NAME;

  protected String originalSerializationFormat;

  @RegisterExtension
  static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder().build();

  @RegisterExtension
  DeploymentExtension deploymentExtension = new DeploymentExtension(processEngineExtension.getRepositoryService());
  private RuntimeService runtimeService;
  private ProcessEngineConfigurationImpl processEngineConfiguration;
  private TaskService taskService;

  @BeforeEach
  void setUp () {
    runtimeService = processEngineExtension.getRuntimeService();
    processEngineConfiguration = processEngineExtension.getProcessEngineConfiguration();
    taskService = processEngineExtension.getTaskService();
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void serializationAsXml() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    XmlSerializable bean = new XmlSerializable("a String", 42, true);
    // request object to be serialized as XML
    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(bean).serializationDataFormat(XML_FORMAT_NAME).create());

    // validate untyped value
    Object value = runtimeService.getVariable(instance.getId(), "simpleBean");
    assertEquals(bean, value);

    // validate typed value
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertEquals(ValueType.OBJECT, typedValue.getType());

    assertTrue(typedValue.isDeserialized());

    assertEquals(bean, typedValue.getValue());
    assertEquals(bean, typedValue.getValue(XmlSerializable.class));
    assertEquals(XmlSerializable.class, typedValue.getObjectType());

    assertEquals(XML_FORMAT_NAME, typedValue.getSerializationDataFormat());
    assertEquals(XmlSerializable.class.getName(), typedValue.getObjectTypeName());
    SpinXmlElement serializedValue = Spin.XML(typedValue.getValueSerialized());
    assertEquals(bean.getStringProperty(), serializedValue.childElement("stringProperty").textContent());
    assertEquals(bean.getBooleanProperty(), Boolean.parseBoolean(serializedValue.childElement("booleanProperty").textContent()));
    assertEquals(bean.getIntProperty(), Integer.parseInt(serializedValue.childElement("intProperty").textContent()));
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void failingSerialization() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    FailingXmlSerializable failingBean = new FailingXmlSerializable("a String", 42, true);

    assertThatThrownBy(() -> runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(failingBean).serializationDataFormat(XML_FORMAT_NAME)))
            .isInstanceOf(ProcessEngineException.class)
            .hasMessageContaining("I am failing");
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void failingDeserialization() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    FailingXmlDeserializationBean failingBean = new FailingXmlDeserializationBean("a String", 42, true);

    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(failingBean).serializationDataFormat(XML_FORMAT_NAME));

    assertThatThrownBy(() -> runtimeService.getVariable(instance.getId(), "simpleBean"))
            .isInstanceOf(ProcessEngineException.class)
            .hasMessageContaining("Cannot deserialize object in variable 'simpleBean'");

    assertThatThrownBy(() -> runtimeService.getVariableTyped(instance.getId(), "simpleBean"))
            .isInstanceOf(ProcessEngineException.class);

    // However, I can access the serialized value
    ObjectValue objectValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean", false);
    assertFalse(objectValue.isDeserialized());
    assertNotNull(objectValue.getObjectTypeName());
    assertNotNull(objectValue.getValueSerialized());

    // but not the deserialized properties
    assertThatThrownBy(() -> objectValue.getValue())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Object is not deserialized");

    assertThatThrownBy(() -> objectValue.getValue(XmlSerializable.class))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Object is not deserialized");

    assertThatThrownBy(() -> objectValue.getObjectType())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Object is not deserialized");
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void failForNonExistingSerializationFormat() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    XmlSerializable XmlSerializable = new XmlSerializable();

    assertThatThrownBy(() -> runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(XmlSerializable).serializationDataFormat("non existing data format")))
            .isInstanceOf(ProcessEngineException.class)
            .hasMessageContaining("Cannot find serializer for value");
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void variableValueCaching() {
    final ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    processEngineConfiguration.getCommandExecutorTxRequired().execute(new Command<Void>() {

      @Override
      public Void execute(CommandContext commandContext) {
        XmlSerializable bean = new XmlSerializable("a String", 42, true);
        runtimeService.setVariable(instance.getId(), "simpleBean", bean);

        Object returnedBean = runtimeService.getVariable(instance.getId(), "simpleBean");
        assertSame(bean, returnedBean);

        return null;
      }
    });

    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().singleResult();

    Object returnedBean = variableInstance.getValue();
    Object theSameReturnedBean = variableInstance.getValue();
    assertSame(returnedBean, theSameReturnedBean);
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void getSerializedVariableValue() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    XmlSerializable bean = new XmlSerializable("a String", 42, true);
    runtimeService.setVariable(instance.getId(), "simpleBean", objectValue(bean).serializationDataFormat(XML_FORMAT_NAME).create());

    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean", false);

    SpinXmlElement serializedValue = Spin.XML(typedValue.getValueSerialized());
    assertEquals(bean.getStringProperty(), serializedValue.childElement("stringProperty").textContent());
    assertEquals(bean.getBooleanProperty(), Boolean.parseBoolean(serializedValue.childElement("booleanProperty").textContent()));
    assertEquals(bean.getIntProperty(), Integer.parseInt(serializedValue.childElement("intProperty").textContent()));
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void setSerializedVariableValue() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    XmlSerializable bean = new XmlSerializable("a String", 42, true);
    String beanAsXml = bean.toExpectedXmlString();

    SerializedObjectValueBuilder serializedValue = serializedObjectValue(beanAsXml)
      .serializationDataFormat(XML_FORMAT_NAME)
      .objectTypeName(bean.getClass().getCanonicalName());

    runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);

    // java object can be retrieved
    XmlSerializable returnedBean = (XmlSerializable) runtimeService.getVariable(instance.getId(), "simpleBean");
    assertEquals(bean, returnedBean);

    // validate typed value metadata
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertEquals(bean, typedValue.getValue());
    assertEquals(XML_FORMAT_NAME, typedValue.getSerializationDataFormat());
    assertEquals(bean.getClass().getCanonicalName(), typedValue.getObjectTypeName());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void setSerializedVariableValueNoTypeName() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    XmlSerializable bean = new XmlSerializable("a String", 42, true);
    String beanAsXml = bean.toExpectedXmlString();

    SerializedObjectValueBuilder serializedValue = serializedObjectValue(beanAsXml)
      .serializationDataFormat(XML_FORMAT_NAME);
      // no type name

    assertThatThrownBy(() -> runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue))
            .isInstanceOf(ProcessEngineException.class)
            .hasMessageContaining("no 'objectTypeName' provided for non-null value");
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void setSerializedVariableValueMismatchingTypeName() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    XmlSerializable bean = new XmlSerializable("a String", 42, true);
    String beanAsXml = bean.toExpectedXmlString();

    SerializedObjectValueBuilder serializedValue = serializedObjectValue(beanAsXml)
      .serializationDataFormat(XML_FORMAT_NAME)
      .objectTypeName("Insensible type name."); // < not a valid type name

    runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);

    assertThatThrownBy(() -> runtimeService.getVariable(instance.getId(), "simpleBean"))
            .isInstanceOf(ProcessEngineException.class);
  }


  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void setSerializedVariableValueNull() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    SerializedObjectValueBuilder serializedValue = serializedObjectValue()
      .serializationDataFormat(XML_FORMAT_NAME)
      .objectTypeName(XmlSerializable.class.getCanonicalName());

    runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);

    // null can be retrieved
    XmlSerializable returnedBean = (XmlSerializable) runtimeService.getVariable(instance.getId(), "simpleBean");
    assertNull(returnedBean);

    // validate typed value metadata
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertNull(typedValue.getValue());
    assertNull(typedValue.getValueSerialized());
    assertEquals(XML_FORMAT_NAME, typedValue.getSerializationDataFormat());
    assertEquals(XmlSerializable.class.getCanonicalName(), typedValue.getObjectTypeName());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void setSerializedVariableValueNullNoTypeName() {
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    SerializedObjectValueBuilder serializedValue = serializedObjectValue()
      .serializationDataFormat(XML_FORMAT_NAME);
    // no objectTypeName specified

    runtimeService.setVariable(instance.getId(), "simpleBean", serializedValue);

    // null can be retrieved
    XmlSerializable returnedBean = (XmlSerializable) runtimeService.getVariable(instance.getId(), "simpleBean");
    assertNull(returnedBean);

    // validate typed value metadata
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "simpleBean");
    assertNull(typedValue.getValue());
    assertNull(typedValue.getValueSerialized());
    assertEquals(XML_FORMAT_NAME, typedValue.getSerializationDataFormat());
    assertNull(typedValue.getObjectTypeName());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void setJavaOjectNullDeserialized() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // set null value as "deserialized" object
    runtimeService.setVariable(instance.getId(), "nullObject",
        objectValue(null)
        .serializationDataFormat(XML_FORMAT_NAME)
        .create());

    // get null value via untyped api
    assertNull(runtimeService.getVariable(instance.getId(), "nullObject"));

    // get null via typed api
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject");
    assertObjectValueDeserializedNull(typedValue);
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void setJavaOjectNullSerialized() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // set null value as "serialized" object
    runtimeService.setVariable(instance.getId(), "nullObject",
        serializedObjectValue()
        .serializationDataFormat(XML_FORMAT_NAME)
        .create()); // Note: no object type name provided

    // get null value via untyped api
    assertNull(runtimeService.getVariable(instance.getId(), "nullObject"));

    // get null via typed api
    ObjectValue deserializedTypedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject");
    assertObjectValueDeserializedNull(deserializedTypedValue);

    ObjectValue serializedTypedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject", false);
    assertObjectValueSerializedNull(serializedTypedValue);
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void setJavaOjectNullSerializedObjectTypeName() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    String typeName = "some.type.Name";

    // set null value as "serialized" object
    runtimeService.setVariable(instance.getId(), "nullObject",
        serializedObjectValue()
        .serializationDataFormat(XML_FORMAT_NAME)
        .objectTypeName(typeName) // This time an objectTypeName is provided
        .create());

    // get null value via untyped api
    assertNull(runtimeService.getVariable(instance.getId(), "nullObject"));

    // get null via typed api
    ObjectValue deserializedTypedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject");
    assertNotNull(deserializedTypedValue);
    assertTrue(deserializedTypedValue.isDeserialized());
    assertEquals(XML_FORMAT_NAME, deserializedTypedValue.getSerializationDataFormat());
    assertNull(deserializedTypedValue.getValue());
    assertNull(deserializedTypedValue.getValueSerialized());
    assertNull(deserializedTypedValue.getObjectType());
    assertEquals(typeName, deserializedTypedValue.getObjectTypeName());

    ObjectValue serializedTypedValue = runtimeService.getVariableTyped(instance.getId(), "nullObject", false);
    assertNotNull(serializedTypedValue);
    assertFalse(serializedTypedValue.isDeserialized());
    assertEquals(XML_FORMAT_NAME, serializedTypedValue.getSerializationDataFormat());
    assertNull(serializedTypedValue.getValueSerialized());
    assertEquals(typeName, serializedTypedValue.getObjectTypeName());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void setUntypedNullForExistingVariable() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // initially the variable has a value
    XmlSerializable object = new XmlSerializable();

    runtimeService.setVariable(instance.getId(), "varName",
        objectValue(object)
        .serializationDataFormat(XML_FORMAT_NAME)
        .create());

    // get value via untyped api
    assertEquals(object, runtimeService.getVariable(instance.getId(), "varName"));

    // set the variable to null via untyped Api
    runtimeService.setVariable(instance.getId(), "varName", null);

    // variable is now untyped null
    TypedValue nullValue = runtimeService.getVariableTyped(instance.getId(), "varName");
    assertUntypedNullValue(nullValue);

  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void setTypedNullForExistingVariable() {

    ProcessInstance instance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    // initially the variable has a value
    XmlSerializable javaSerializable = new XmlSerializable();

    runtimeService.setVariable(instance.getId(), "varName",
        objectValue(javaSerializable)
        .serializationDataFormat(XML_FORMAT_NAME)
        .create());

    // get value via untyped api
    assertEquals(javaSerializable, runtimeService.getVariable(instance.getId(), "varName"));

    // set the variable to null via typed Api
    runtimeService.setVariable(instance.getId(), "varName", objectValue(null));

    // variable is still of type object
    ObjectValue typedValue = runtimeService.getVariableTyped(instance.getId(), "varName");
    assertObjectValueDeserializedNull(typedValue);
  }

  @Test
  void transientXmlValue() {
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

    XmlSerializable bean = new XmlSerializable("bar", 42, true);
    ObjectValue xmlValue = serializedObjectValue(bean.toExpectedXmlString(), true)
        .serializationDataFormat(XML_FORMAT_NAME)
        .objectTypeName(XmlSerializable.class.getName())
        .create();
    VariableMap variables = Variables.createVariables().putValueTyped("x", xmlValue);

    // when
    runtimeService.startProcessInstanceByKey("foo", variables);

    // then
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().list();
    assertEquals(0, variableInstances.size());

    Task task = taskService.createTaskQuery().singleResult();
    assertNotNull(task);
    assertEquals("userTask1", task.getTaskDefinitionKey());
  }

  @Test
  void overloadedAppendMethod() {
    // given
    deploymentExtension.deploy(Bpmn.createExecutableProcess("spin-xml-issue")
        .startEvent()
        .serviceTask()
          .operatonExpression("${XML(\"<result/>\").append(xmlInput.xPath(\"//cosigner/*\").elementList()).toString()}")
          .operatonResultVariable("output")
        .userTask()
        .endEvent()
        .done());

    Reader xmlInput = SpinIoUtil.classpathResourceAsReader("org/operaton/spin/plugin/XmlSerializationTest-input.xml");
    String expectedOutput = SpinIoUtil.fileAsString("org/operaton/spin/plugin/XmlSerializationTest-output.xml");
    VariableMap variables = Variables.putValue("xmlInput", Spin.XML(xmlInput));
    // when
    runtimeService.startProcessInstanceByKey("spin-xml-issue", variables);
    // then
    VariableInstance output = runtimeService.createVariableInstanceQuery().variableName("output").singleResult();
    assertThat((String) output.getValue()).isEqualToIgnoringWhitespace(expectedOutput);
  }
}
