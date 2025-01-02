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
package org.operaton.bpm.engine.test.api.runtime;

import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.digest._apacheCommonsCodec.Base64;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.runtime.ProcessInstanceWithVariables;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.variables.JavaSerializable;
import org.operaton.bpm.engine.test.util.ProcessEngineBootstrapRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import static org.operaton.bpm.engine.test.util.TypedValueAssert.assertObjectValueSerializedJava;
import static org.operaton.bpm.engine.variable.Variables.serializedObjectValue;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import static junit.framework.TestCase.*;

/**
 * Represents the test class for the process instantiation on which
 * the process instance is returned with variables.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@RequiredHistoryLevel(ProcessEngineConfigurationImpl.HISTORY_AUDIT)
public class ProcessInstantiationWithVariablesInReturnTest {

  protected static final String SUBPROCESS_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocess.bpmn20.xml";
  protected static final String SET_VARIABLE_IN_DELEGATE_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstantiationWithVariablesInReturn.setVariableInDelegate.bpmn20.xml";
  protected static final String SET_VARIABLE_IN_DELEGATE_WITH_WAIT_STATE_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstantiationWithVariablesInReturn.setVariableInDelegateWithWaitState.bpmn20.xml";
  protected static final String SIMPLE_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstantiationWithVariablesInReturn.simpleProcess.bpmn20.xml";

  @ClassRule
  public static ProcessEngineBootstrapRule bootstrapRule = new ProcessEngineBootstrapRule(configuration ->
      configuration.setJavaSerializationFormatEnabled(true));
  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule(bootstrapRule);
  protected ProcessEngineTestRule testHelper = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain chain = RuleChain.outerRule(engineRule).around(testHelper);

  private void checkVariables(VariableMap map, int expectedSize) {
    List<HistoricVariableInstance> variables = engineRule.getHistoryService()
            .createHistoricVariableInstanceQuery()
            .orderByVariableName()
            .asc()
            .list();

    assertEquals(expectedSize, variables.size());

    assertEquals(variables.size(), map.size());
    for (HistoricVariableInstance instance : variables) {
      assertTrue(map.containsKey(instance.getName()));
      Object instanceValue = instance.getTypedValue().getValue();
      Object mapValue = map.getValueTyped(instance.getName()).getValue();
      if (instanceValue == null) {
        assertNull(mapValue);
      } else if (instanceValue instanceof byte[] bytes) {
        assertTrue(Arrays.equals(bytes, (byte[]) mapValue));
      } else {
        assertEquals(instanceValue, mapValue);
      }
    }
  }

  private void testVariablesWithoutDeserialization(String processDefinitionKey) throws Exception {
    //given serializable variable
    JavaSerializable javaSerializable = new JavaSerializable("foo");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    new ObjectOutputStream(baos).writeObject(javaSerializable);
    String serializedObject = StringUtil.fromBytes(Base64.encodeBase64(baos.toByteArray()), engineRule.getProcessEngine());

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
    assertNotNull(map);

    ObjectValue serializedVar = (ObjectValue) map.getValueTyped("serializedVar");
    assertFalse(serializedVar.isDeserialized());
    assertObjectValueSerializedJava(serializedVar, javaSerializable);

    //access on value should fail because variable is not deserialized
    try {
      serializedVar.getValue();
      Assert.fail("Deserialization should fail!");
    } catch (IllegalStateException ise) {
      assertTrue(ise.getMessage().equals("Object is not deserialized."));
    }
  }

  @Test
  @Deployment(resources = SIMPLE_PROCESS)
  public void testReturnVariablesFromStartWithoutDeserialization() throws Exception {
    testVariablesWithoutDeserialization("simpleProcess");
  }

  @Test
  @Deployment(resources = SUBPROCESS_PROCESS)
  public void testReturnVariablesFromStartWithoutDeserializationWithWaitstate() throws Exception {
    testVariablesWithoutDeserialization("subprocess");
  }

  @Test
  @Deployment(resources = SIMPLE_PROCESS)
  public void testReturnVariablesFromStart() {
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
    assertNotNull(map);

    // then variables equal to variables which are accessible via query
    checkVariables(map, 4);
  }

  @Test
  @Deployment(resources = SUBPROCESS_PROCESS)
  public void testReturnVariablesFromStartWithWaitstate() {
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
    assertNotNull(map);

    // then variables equal to variables which are accessible via query
    checkVariables(map, 4);
  }

  @Test
  @Deployment(resources = SUBPROCESS_PROCESS)
  public void testReturnVariablesFromStartWithWaitstateStartInSubProcess() {
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
    assertNotNull(map);

    // then variables equal to variables which are accessible via query
    checkVariables(map, 4);
  }

  @Test
  @Deployment(resources = SET_VARIABLE_IN_DELEGATE_PROCESS)
  public void testReturnVariablesFromExecution() {

    //given executed process which sets variables in java delegate
    ProcessInstanceWithVariables procInstance = engineRule.getRuntimeService().createProcessInstanceByKey("variableProcess")
            .executeWithVariablesInReturn();
    //when returned instance contains variables
    VariableMap map = procInstance.getVariables();
    assertNotNull(map);

    // then variables equal to variables which are accessible via query
    checkVariables(map, 8);
  }

  @Test
  @Deployment(resources = SET_VARIABLE_IN_DELEGATE_WITH_WAIT_STATE_PROCESS)
  public void testReturnVariablesFromExecutionWithWaitstate() {

    //given executed process which sets variables in java delegate
    ProcessInstanceWithVariables procInstance = engineRule.getRuntimeService().createProcessInstanceByKey("variableProcess")
            .executeWithVariablesInReturn();
    //when returned instance contains variables
    VariableMap map = procInstance.getVariables();
    assertNotNull(map);

    // then variables equal to variables which are accessible via query
    checkVariables(map, 8);
  }

  @Test
  @Deployment(resources = SET_VARIABLE_IN_DELEGATE_PROCESS)
  public void testReturnVariablesFromStartAndExecution() {

    //given executed process which sets variables in java delegate
    ProcessInstanceWithVariables procInstance = engineRule.getRuntimeService().createProcessInstanceByKey("variableProcess")
            .setVariable("aVariable1", "aValue1")
            .setVariableLocal("aVariable2", "aValue2")
            .setVariables(Variables.createVariables().putValue("aVariable3", "aValue3"))
            .setVariablesLocal(Variables.createVariables().putValue("aVariable4", new byte[]{127, 34, 64}))
            .executeWithVariablesInReturn();
    //when returned instance contains variables
    VariableMap map = procInstance.getVariables();
    assertNotNull(map);

    // then variables equal to variables which are accessible via query
    checkVariables(map, 12);
  }

  @Test
  @Deployment(resources = SET_VARIABLE_IN_DELEGATE_WITH_WAIT_STATE_PROCESS)
  public void testReturnVariablesFromStartAndExecutionWithWaitstate() {

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
    assertNotNull(map);

    // then variables equal to variables which are accessible via query
    checkVariables(map, 8);
  }

}
