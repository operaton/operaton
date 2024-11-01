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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.DeploymentExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.spin.DataFormats;
import org.operaton.spin.SpinRuntimeException;
import org.operaton.spin.plugin.variable.type.SpinValueType;
import org.operaton.spin.plugin.variable.value.XmlValue;
import org.operaton.spin.plugin.variable.value.builder.XmlValueBuilder;
import org.operaton.spin.xml.SpinXmlElement;
import static org.operaton.spin.DataFormats.xml;
import static org.operaton.spin.plugin.variable.SpinValues.xmlValue;
import static org.operaton.spin.plugin.variable.type.SpinValueType.XML;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Roman Smirnov
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class XmlValueTest {

  protected static final String ONE_TASK_PROCESS = "org/operaton/spin/plugin/oneTaskProcess.bpmn20.xml";
  protected static final String XML_FORMAT_NAME = DataFormats.XML_DATAFORMAT_NAME;

  protected static final String ONE_TASK_PROCESS_KEY = "oneTaskProcess";

  protected String xmlString = "<elementName attrName=\"attrValue\" />";
  protected String brokenXmlString = "<elementName attrName=attrValue\" />";

  protected String variableName = "x";
  private RuntimeService runtimeService = null;
  private TaskService taskService = null;

  @RegisterExtension
  DeploymentExtension deploymentExtension = new DeploymentExtension();

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void getUntypedXmlValue() {
    // given
    XmlValue xmlValue = xmlValue(xmlString).create();
    VariableMap variables = Variables.createVariables().putValueTyped(variableName, xmlValue);

    String processInstanceId = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS_KEY, variables).getId();

    // when
    SpinXmlElement value = (SpinXmlElement) runtimeService.getVariable(processInstanceId, variableName);

    // then
    assertTrue(value.hasAttr("attrName"));
    assertEquals("attrValue", value.attr("attrName").value());
    assertTrue(value.childElements().isEmpty());
    assertEquals(xml().getName(), value.getDataFormatName());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void getTypedXmlValue() {
    // given
    XmlValue xmlValue = xmlValue(xmlString).create();
    VariableMap variables = Variables.createVariables().putValueTyped(variableName, xmlValue);

    String processInstanceId = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS_KEY, variables).getId();

    // when
    XmlValue typedValue = runtimeService.getVariableTyped(processInstanceId, variableName);

    // then
    SpinXmlElement value = typedValue.getValue();
    assertTrue(value.hasAttr("attrName"));
    assertEquals("attrValue", value.attr("attrName").value());
    assertTrue(value.childElements().isEmpty());

    assertTrue(typedValue.isDeserialized());
    assertEquals(XML, typedValue.getType());
    assertEquals(XML_FORMAT_NAME, typedValue.getSerializationDataFormat());
    assertEquals(xmlString, typedValue.getValueSerialized());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void brokenXmlSerialization() {
    // given
    XmlValue value = xmlValue(brokenXmlString).create();

    String processInstanceId = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();

    Assertions.assertDoesNotThrow(() -> {
      // when
      runtimeService.setVariable(processInstanceId, variableName, value);
    }, "no exception expected");
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void failingDeserialization() {
    // given
    XmlValue value = xmlValue(brokenXmlString).create();

    String processInstanceId = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    runtimeService.setVariable(processInstanceId, variableName, value);

    try {
      // when
      runtimeService.getVariable(processInstanceId, variableName);
      fail("exception expected");
    } catch (ProcessEngineException e) {
      // happy path
    }

    try {
      runtimeService.getVariableTyped(processInstanceId, variableName);
      fail("exception expected");
    } catch(ProcessEngineException e) {
      // happy path
    }

    // However, I can access the serialized value
    XmlValue xmlValue = runtimeService.getVariableTyped(processInstanceId, variableName, false);
    assertFalse(xmlValue.isDeserialized());
    assertEquals(brokenXmlString, xmlValue.getValueSerialized());

    // but not the deserialized properties
    try {
      xmlValue.getValue();
      fail("exception expected");
    } catch(SpinRuntimeException e) {
    }
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void failForNonExistingSerializationFormat() {
    // given
    XmlValueBuilder builder = xmlValue(xmlString).serializationDataFormat("non existing data format");
    String processInstanceId = runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();

    try {
      // when (1)
      runtimeService.setVariable(processInstanceId, variableName, builder);
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // then (1)
      assertThat(e.getMessage()).contains("Cannot find serializer for value");
      // happy path
    }

    try {
      // when (2)
      runtimeService.setVariable(processInstanceId, variableName, builder.create());
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      // then (2)
      assertThat(e.getMessage()).contains("Cannot find serializer for value");
      // happy path
    }
  }

  @Deployment(resources = "org/operaton/spin/plugin/xmlConditionProcess.bpmn20.xml")
  @Test
  void xmlValueInCondition() {
    // given
    String xml = "<customer age=\"22\" />";
    XmlValue value = xmlValue(xml).create();
    VariableMap variables = Variables.createVariables().putValueTyped("customer", value);

    // when
    runtimeService.startProcessInstanceByKey("process", variables);

    // then
    Task task = taskService.createTaskQuery().singleResult();
    assertEquals("task1", task.getTaskDefinitionKey());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void transientXmlValueFluent() {
    // given
    XmlValue xmlValue = xmlValue(xmlString).setTransient(true).create();
    VariableMap variables = Variables.createVariables().putValueTyped(variableName, xmlValue);

    // when
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS_KEY, variables).getId();

    // then
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().list();
    assertEquals(0, variableInstances.size());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  void transientXmlValue() {
    // given
    XmlValue xmlValue = xmlValue(xmlString, true).create();
    VariableMap variables = Variables.createVariables().putValueTyped(variableName, xmlValue);

    // when
    runtimeService.startProcessInstanceByKey(ONE_TASK_PROCESS_KEY, variables).getId();

    // then
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().list();
    assertEquals(0, variableInstances.size());
  }

  /**
   * See https://app.camunda.com/jira/browse/CAM-9932
   */
  @Test
  void transientXmlSpinVariables() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("aProcess")
        .startEvent()
        .serviceTask()
          .operatonClass(XmlDelegate.class)
        .userTask()
        .endEvent()
        .done();
    deploymentExtension.deploy(modelInstance);

    // when
    String processInstanceId = runtimeService.startProcessInstanceByKey("aProcess").getId();

    // then
    Object value = runtimeService.getVariable(processInstanceId, "xmlVariable");
    assertThat(value).isNull();
  }

  @Test
  void applyValueInfoFromSerializedValue() {
    // given
    Map<String, Object> valueInfo = new HashMap<>();
    valueInfo.put(ValueType.VALUE_INFO_TRANSIENT, true);

    // when
    XmlValue xmlValue = (XmlValue) SpinValueType.XML.createValueFromSerialized(xmlString, valueInfo);

    // then
    assertTrue(xmlValue.isTransient());
    Map<String, Object> returnedValueInfo = SpinValueType.XML.getValueInfo(xmlValue);
    assertEquals(true, returnedValueInfo.get(ValueType.VALUE_INFO_TRANSIENT));
  }

  @Test
  void deserializeTransientXmlValue() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("foo")
        .startEvent()
        .exclusiveGateway("gtw")
          .sequenceFlowId("flow1")
          .condition("cond", "${XML(" + variableName + ").attr('attrName').value() == 'attrValue'}")
          .userTask("userTask1")
          .endEvent()
        .moveToLastGateway()
          .sequenceFlowId("flow2")
          .userTask("userTask2")
          .endEvent()
        .done();

    deploymentExtension.deploy(modelInstance);

    XmlValue xmlValue = xmlValue(xmlString, true).create();
    VariableMap variables = Variables.createVariables().putValueTyped(variableName, xmlValue);

    // when
    runtimeService.startProcessInstanceByKey("foo", variables);

    // then
    List<VariableInstance> variableInstances = runtimeService.createVariableInstanceQuery().list();
    assertEquals(0, variableInstances.size());

    Task task = taskService.createTaskQuery().singleResult();
    assertNotNull(task);
    assertEquals("userTask1", task.getTaskDefinitionKey());
  }
}
