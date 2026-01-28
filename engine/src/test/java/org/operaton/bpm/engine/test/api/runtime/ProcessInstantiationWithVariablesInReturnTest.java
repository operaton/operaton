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
package org.operaton.bpm.engine.test.api.runtime;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.variables.JavaSerializable;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;

import static org.operaton.bpm.engine.test.util.TypedValueAssert.assertObjectValueSerializedJava;
import static org.operaton.bpm.engine.variable.Variables.serializedObjectValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Represents the test class for the process instantiation on which
 * the process instance is returned with variables.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@RequiredHistoryLevel(ProcessEngineConfigurationImpl.HISTORY_AUDIT)
class ProcessInstantiationWithVariablesInReturnTest {

  protected static final String SUBPROCESS_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocess.bpmn20.xml";
  protected static final String SET_VARIABLE_IN_DELEGATE_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstantiationWithVariablesInReturn.setVariableInDelegate.bpmn20.xml";
  protected static final String SET_VARIABLE_IN_DELEGATE_WITH_WAIT_STATE_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstantiationWithVariablesInReturn.setVariableInDelegateWithWaitState.bpmn20.xml";
  protected static final String SIMPLE_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstantiationWithVariablesInReturn.simpleProcess.bpmn20.xml";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .closeEngineAfterAllTests()
      .randomEngineName()
      .configurator(configuration -> configuration.setJavaSerializationFormatEnabled(true))
      .build();
  @RegisterExtension
  ProcessEngineTestExtension testHelper = new ProcessEngineTestExtension(engineRule);

  ProcessEngine processEngine;
  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;

  private void checkVariables(VariableMap map, int expectedSize) {
    List<HistoricVariableInstance> variables = engineRule.getHistoryService()
            .createHistoricVariableInstanceQuery()
            .orderByVariableName()
            .asc()
            .list();

    assertThat(variables).hasSize(expectedSize);

    assertThat(map).hasSize(variables.size());
    for (HistoricVariableInstance instance : variables) {
      assertThat(map).containsKey(instance.getName());
      Object instanceValue = instance.getTypedValue().getValue();
      Object mapValue = map.getValueTyped(instance.getName()).getValue();
      if (instanceValue == null) {
        assertThat(mapValue).isNull();
      } else if (instanceValue instanceof byte[] bytes) {
        assertThat((byte[]) mapValue).isEqualTo(bytes);
      } else {
        assertThat(mapValue).isEqualTo(instanceValue);
      }
    }
  }

  private void testVariablesWithoutDeserialization(String processDefinitionKey) throws Exception {
    //given serializable variable
    JavaSerializable javaSerializable = new JavaSerializable("foo");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.getEncoder().encode(baos.toByteArray()),
        engineRule.getProcessEngine());

    //when execute process with serialized variable and wait state
    ProcessInstanceWithVariables procInstance = engineRule.getRuntimeService()
            .createProcessInstanceByKey(processDefinitionKey)
            .setVariable("serializedVar", serializedObjectValue(serializedObject)
              .serializationDataFormat(Variables.SerializationDataFormats.JAVA)
              .objectTypeName(JavaSerializable.class.getName())
              .create())
            .executeWithVariablesInReturn(false, false);

    //then returned instance contains serialized variable
    VariableMap map = procInstance.getVariables();
    assertThat(map).isNotNull();

    ObjectValue serializedVar = (ObjectValue) map.getValueTyped("serializedVar");
    assertThat(serializedVar.isDeserialized()).isFalse();
    assertObjectValueSerializedJava(serializedVar, javaSerializable);

    assertThatThrownBy(serializedVar::getValue)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Object is not deserialized");
  }

  @Test
  @Deployment(resources = SIMPLE_PROCESS)
  void testReturnVariablesFromStartWithoutDeserialization() throws Exception {
    testVariablesWithoutDeserialization("simpleProcess");
  }

  @Test
  @Deployment(resources = SUBPROCESS_PROCESS)
  void testReturnVariablesFromStartWithoutDeserializationWithWaitstate() throws Exception {
    testVariablesWithoutDeserialization("subprocess");
  }

  @Test
  @Deployment(resources = SIMPLE_PROCESS)
  void testReturnVariablesFromStart() {
    //given execute process with variables
    ProcessInstanceWithVariables procInstance = engineRule.getRuntimeService()
            .createProcessInstanceByKey("simpleProcess")
            .setVariable("aVariable1", "aValue1")
            .setVariableLocal("aVariable2", "aValue2")
            .setVariables(Variables.createVariables().putValue("aVariable3", "aValue3"))
            .setVariablesLocal(Variables.createVariables().putValue("aVariable4", new byte[]{127, 34, 64}))
            .executeWithVariablesInReturn(false, false);

    //when returned instance contains variables
    VariableMap map = procInstance.getVariables();
    assertThat(map).isNotNull();

    // then variables equal to variables which are accessible via query
    checkVariables(map, 4);
  }

  @Test
  @Deployment(resources = SUBPROCESS_PROCESS)
  void testReturnVariablesFromStartWithWaitstate() {
    //given execute process with variables and wait state
    ProcessInstanceWithVariables procInstance = engineRule.getRuntimeService()
            .createProcessInstanceByKey("subprocess")
            .setVariable("aVariable1", "aValue1")
            .setVariableLocal("aVariable2", "aValue2")
            .setVariables(Variables.createVariables().putValue("aVariable3", "aValue3"))
            .setVariablesLocal(Variables.createVariables().putValue("aVariable4", new byte[]{127, 34, 64}))
            .executeWithVariablesInReturn(false, false);

    //when returned instance contains variables
    VariableMap map = procInstance.getVariables();
    assertThat(map).isNotNull();

    // then variables equal to variables which are accessible via query
    checkVariables(map, 4);
  }

  @Test
  @Deployment(resources = SUBPROCESS_PROCESS)
  void testReturnVariablesFromStartWithWaitstateStartInSubProcess() {
    //given execute process with variables and wait state in sub process
    ProcessInstanceWithVariables procInstance = engineRule.getRuntimeService()
            .createProcessInstanceByKey("subprocess")
            .setVariable("aVariable1", "aValue1")
            .setVariableLocal("aVariable2", "aValue2")
            .setVariables(Variables.createVariables().putValue("aVariable3", "aValue3"))
            .setVariablesLocal(Variables.createVariables().putValue("aVariable4", new byte[]{127, 34, 64}))
            .startBeforeActivity("innerTask")
            .executeWithVariablesInReturn(true, true);

    //when returned instance contains variables
    VariableMap map = procInstance.getVariables();
    assertThat(map).isNotNull();

    // then variables equal to variables which are accessible via query
    checkVariables(map, 4);
  }

  @Test
  @Deployment(resources = SET_VARIABLE_IN_DELEGATE_PROCESS)
  void testReturnVariablesFromExecution() {

    //given executed process which sets variables in java delegate
    ProcessInstanceWithVariables procInstance = engineRule.getRuntimeService().createProcessInstanceByKey("variableProcess")
            .executeWithVariablesInReturn();
    //when returned instance contains variables
    VariableMap map = procInstance.getVariables();
    assertThat(map).isNotNull();

    // then variables equal to variables which are accessible via query
    checkVariables(map, 8);
  }

  @Test
  @Deployment(resources = SET_VARIABLE_IN_DELEGATE_WITH_WAIT_STATE_PROCESS)
  void testReturnVariablesFromExecutionWithWaitstate() {

    //given executed process which sets variables in java delegate
    ProcessInstanceWithVariables procInstance = engineRule.getRuntimeService().createProcessInstanceByKey("variableProcess")
            .executeWithVariablesInReturn();
    //when returned instance contains variables
    VariableMap map = procInstance.getVariables();
    assertThat(map).isNotNull();

    // then variables equal to variables which are accessible via query
    checkVariables(map, 8);
  }

  @Test
  @Deployment(resources = SET_VARIABLE_IN_DELEGATE_PROCESS)
  void testReturnVariablesFromStartAndExecution() {

    //given executed process which sets variables in java delegate
    ProcessInstanceWithVariables procInstance = engineRule.getRuntimeService().createProcessInstanceByKey("variableProcess")
            .setVariable("aVariable1", "aValue1")
            .setVariableLocal("aVariable2", "aValue2")
            .setVariables(Variables.createVariables().putValue("aVariable3", "aValue3"))
            .setVariablesLocal(Variables.createVariables().putValue("aVariable4", new byte[]{127, 34, 64}))
            .executeWithVariablesInReturn();
    //when returned instance contains variables
    VariableMap map = procInstance.getVariables();
    assertThat(map).isNotNull();

    // then variables equal to variables which are accessible via query
    checkVariables(map, 12);
  }

  @Test
  @Deployment(resources = SET_VARIABLE_IN_DELEGATE_WITH_WAIT_STATE_PROCESS)
  void testReturnVariablesFromStartAndExecutionWithWaitstate() {

    //given executed process which overwrites these four variables in java delegate
    // and adds four additional variables
    ProcessInstanceWithVariables procInstance = engineRule.getRuntimeService().createProcessInstanceByKey("variableProcess")
            .setVariable("stringVar", "aValue1")
            .setVariableLocal("integerVar", 56789)
            .setVariables(Variables.createVariables().putValue("longVar", 123L))
            .setVariablesLocal(Variables.createVariables().putValue("byteVar", new byte[]{127, 34, 64}))
            .executeWithVariablesInReturn(false, false);
    //when returned instance contains variables
    VariableMap map = procInstance.getVariables();
    assertThat(map).isNotNull();

    // then variables equal to variables which are accessible via query
    checkVariables(map, 8);
  }

}
