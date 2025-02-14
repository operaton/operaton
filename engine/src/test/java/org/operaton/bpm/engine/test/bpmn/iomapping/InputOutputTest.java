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
package org.operaton.bpm.engine.test.bpmn.iomapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Testcase for operaton input / output in BPMN
 *
 * @author Daniel Meyer
 *
 */
public class InputOutputTest extends PluggableProcessEngineTest {

  // Input parameters /////////////////////////////////////////

  @Deployment
  @Test
  public void testInputNullValue() {
    runtimeService.startProcessInstanceByKey("testProcess");
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getTypeName()).isEqualTo("null");
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @Test
  public void testInputStringConstantValue() {
    runtimeService.startProcessInstanceByKey("testProcess");
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo("stringValue");
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }


  @Deployment
  @Test
  public void testInputElValue() {
    runtimeService.startProcessInstanceByKey("testProcess");
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2l);
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @Test
  public void testInputScriptValue() {
    runtimeService.startProcessInstanceByKey("testProcess");
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @Test
  public void testInputScriptValueAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptSource", "return 1 + 1");
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @Test
  public void testInputScriptValueAsBean() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("onePlusOneBean", new OnePlusOneBean());
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @Test
  public void testInputExternalScriptValue() {
    runtimeService.startProcessInstanceByKey("testProcess");
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @Test
  public void testInputExternalScriptValueAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptPath", "org/operaton/bpm/engine/test/bpmn/iomapping/oneplusone.groovy");
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @Test
  public void testInputExternalScriptValueAsBean() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("onePlusOneBean", new OnePlusOneBean());
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @Test
  public void testInputExternalClasspathScriptValue() {
    runtimeService.startProcessInstanceByKey("testProcess");
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @Test
  public void testInputExternalClasspathScriptValueAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptPath", "classpath://org/operaton/bpm/engine/test/bpmn/iomapping/oneplusone.groovy");
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @Test
  public void testInputExternalClasspathScriptValueAsBean() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("onePlusOneBean", new OnePlusOneBean());
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testInputExternalDeploymentScriptValue.bpmn",
    "org/operaton/bpm/engine/test/bpmn/iomapping/oneplusone.groovy"
  })
  @Test
  public void testInputExternalDeploymentScriptValue() {
    runtimeService.startProcessInstanceByKey("testProcess");
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testInputExternalDeploymentScriptValueAsVariable.bpmn",
    "org/operaton/bpm/engine/test/bpmn/iomapping/oneplusone.groovy"
  })
  @Test
  public void testInputExternalDeploymentScriptValueAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptPath", "deployment://org/operaton/bpm/engine/test/bpmn/iomapping/oneplusone.groovy");
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testInputExternalDeploymentScriptValueAsBean.bpmn",
    "org/operaton/bpm/engine/test/bpmn/iomapping/oneplusone.groovy"
  })
  @Test
  public void testInputExternalDeploymentScriptValueAsBean() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("onePlusOneBean", new OnePlusOneBean());
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @SuppressWarnings("unchecked")
  @Test
  public void testInputListElValues() {
    runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    List<Object> value = (List<Object>) variable.getValue();
    assertThat(value.get(0)).isEqualTo(2l);
    assertThat(value.get(1)).isEqualTo(3l);
    assertThat(value.get(2)).isEqualTo(4l);
  }

  @Deployment
  @SuppressWarnings("unchecked")
  @Test
  public void testInputListMixedValues() {
    runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    List<Object> value = (List<Object>) variable.getValue();
    assertThat(value.get(0)).isEqualTo("constantStringValue");
    assertThat(value.get(1)).isEqualTo("elValue");
    assertThat(value.get(2)).isEqualTo("scriptValue");
  }

  @Deployment
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testInputMapElValues() {
    runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    TreeMap<String, Object> value = (TreeMap) variable.getValue();
    assertThat(value)
            .containsEntry("a", 2l)
            .containsEntry("b", 3l)
            .containsEntry("c", 4l);

  }

  @Deployment
  @Test
  public void testInputMultipleElValue() {
    runtimeService.startProcessInstanceByKey("testProcess");
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance var1 = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(var1).isNotNull();
    assertThat(var1.getValue()).isEqualTo(2l);
    assertThat(var1.getExecutionId()).isEqualTo(execution.getId());

    VariableInstance var2 = runtimeService.createVariableInstanceQuery().variableName("var2").singleResult();
    assertThat(var2).isNotNull();
    assertThat(var2.getValue()).isEqualTo(3l);
    assertThat(var2.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @Test
  public void testInputMultipleMixedValue() {
    runtimeService.startProcessInstanceByKey("testProcess");
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance var1 = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(var1).isNotNull();
    assertThat(var1.getValue()).isEqualTo(2l);
    assertThat(var1.getExecutionId()).isEqualTo(execution.getId());

    VariableInstance var2 = runtimeService.createVariableInstanceQuery().variableName("var2").singleResult();
    assertThat(var2).isNotNull();
    assertThat(var2.getValue()).isEqualTo("stringConstantValue");
    assertThat(var2.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testInputNested() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("exprKey", "b");
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    Execution execution = runtimeService.createExecutionQuery().activityId("wait").singleResult();

    VariableInstance var1 = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    TreeMap<String, Object> value = (TreeMap) var1.getValue();
    List<Object> nestedList = (List<Object>) value.get("a");
    assertThat(nestedList.get(0)).isEqualTo("stringInListNestedInMap");
    assertThat(nestedList.get(1)).isEqualTo("b");
    assertThat(value).containsEntry("b", "stringValueWithExprKey");

    VariableInstance var2 = runtimeService.createVariableInstanceQuery().variableName("var2").singleResult();
    assertThat(var2).isNotNull();
    assertThat(var2.getValue()).isEqualTo("stringConstantValue");
    assertThat(var2.getExecutionId()).isEqualTo(execution.getId());
  }

  @Deployment
  @SuppressWarnings("unchecked")
  @Test
  public void testInputNestedListValues() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("exprKey", "vegie");
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    List<Object> value = (List<Object>) variable.getValue();
    assertThat(value.get(0)).isEqualTo("constantStringValue");
    assertThat(value.get(1)).isEqualTo("elValue");
    assertThat(value.get(2)).isEqualTo("scriptValue");

    List<Object> nestedList = (List<Object>) value.get(3);
    List<Object> nestedNestedList = (List<Object>) nestedList.get(0);
    assertThat(nestedNestedList.get(0)).isEqualTo("a");
    assertThat(nestedNestedList.get(1)).isEqualTo("b");
    assertThat(nestedNestedList.get(2)).isEqualTo("c");
    assertThat(nestedList.get(1)).isEqualTo("d");

    TreeMap<String, Object> nestedMap = (TreeMap<String, Object>) value.get(4);
    assertThat(nestedMap)
            .containsEntry("foo", "bar")
            .containsEntry("hello", "world")
            .containsEntry("vegie", "potato");
  }

  @Deployment
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testInputMapElKey() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("varExpr1", "a");
    variables.put("varExpr2", "b");
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    TreeMap<String, Object> value = (TreeMap) variable.getValue();
    assertThat(value)
            .containsEntry("a", "potato")
            .containsEntry("b", "tomato");
  }

  @Deployment
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testInputMapElMixedKey() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("varExpr1", "a");
    variables.put("varExpr2", "b");
    variables.put("varExprMapValue", "avocado");
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    TreeMap<String, Object> value = (TreeMap) variable.getValue();
    assertThat(value)
            .containsEntry("a", "potato")
            .containsEntry("b", "tomato");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testInputMapElKey.bpmn")
  @Test
  public void testInputMapElUndefinedKey() {
    try {
      runtimeService.startProcessInstanceByKey("testProcess");
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("Unknown property used in expression: ${varExpr1}", e.getMessage());
    }
  }

  // output parameter ///////////////////////////////////////////////////////

  @Deployment
  @Test
  public void testOutputNullValue() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getTypeName()).isEqualTo("null");
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment
  @Test
  public void testOutputStringConstantValue() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo("stringValue");
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }


  @Deployment
  @Test
  public void testOutputElValue() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2l);
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment
  @Test
  public void testOutputScriptValue() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment
  @Test
  public void testOutputScriptValueAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptSource", "return 1 + 1");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  // related to CAM-8072
  @Test
  public void testOutputParameterAvailableAfterParallelGateway() {
    // given
    BpmnModelInstance processDefinition = Bpmn.createExecutableProcess("process")
      .startEvent()
      .serviceTask()
        .operatonOutputParameter("variable", "A")
        .operatonExpression("${'this value does not matter'}")
      .parallelGateway("fork")
      .endEvent()
      .moveToNode("fork")
        .serviceTask().operatonExpression("${variable}")
        .receiveTask()
      .endEvent()
    .done();

    // when
   testRule.deploy(processDefinition);
    runtimeService.startProcessInstanceByKey("process");

    // then
    VariableInstance variableInstance = runtimeService
      .createVariableInstanceQuery()
      .variableName("variable")
      .singleResult();
    assertThat(variableInstance).isNotNull();
  }

  @Deployment
  @Test
  public void testOutputScriptValueAsBean() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("onePlusOneBean", new OnePlusOneBean());
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment
  @Test
  public void testOutputExternalScriptValue() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment
  @Test
  public void testOutputExternalScriptValueAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptPath", "org/operaton/bpm/engine/test/bpmn/iomapping/oneplusone.groovy");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment
  @Test
  public void testOutputExternalScriptValueAsBean() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("onePlusOneBean", new OnePlusOneBean());
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment
  @Test
  public void testOutputExternalClasspathScriptValue() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment
  @Test
  public void testOutputExternalClasspathScriptValueAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptPath", "classpath://org/operaton/bpm/engine/test/bpmn/iomapping/oneplusone.groovy");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment
  @Test
  public void testOutputExternalClasspathScriptValueAsBean() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("onePlusOneBean", new OnePlusOneBean());
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testOutputExternalDeploymentScriptValue.bpmn",
    "org/operaton/bpm/engine/test/bpmn/iomapping/oneplusone.groovy"
  })
  @Test
  public void testOutputExternalDeploymentScriptValue() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testOutputExternalDeploymentScriptValueAsVariable.bpmn",
    "org/operaton/bpm/engine/test/bpmn/iomapping/oneplusone.groovy"
  })
  @Test
  public void testOutputExternalDeploymentScriptValueAsVariable() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("scriptPath", "deployment://org/operaton/bpm/engine/test/bpmn/iomapping/oneplusone.groovy");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testOutputExternalDeploymentScriptValueAsBean.bpmn",
    "org/operaton/bpm/engine/test/bpmn/iomapping/oneplusone.groovy"
  })
  @Test
  public void testOutputExternalDeploymentScriptValueAsBean() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("onePlusOneBean", new OnePlusOneBean());
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo(2);
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment
  @SuppressWarnings("unchecked")
  @Test
  public void testOutputListElValues() {
    runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    List<Object> value = (List<Object>) variable.getValue();
    assertThat(value.get(0)).isEqualTo(2l);
    assertThat(value.get(1)).isEqualTo(3l);
    assertThat(value.get(2)).isEqualTo(4l);
  }

  @Deployment
  @SuppressWarnings("unchecked")
  @Test
  public void testOutputListMixedValues() {
    runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    List<Object> value = (List<Object>) variable.getValue();
    assertThat(value.get(0)).isEqualTo("constantStringValue");
    assertThat(value.get(1)).isEqualTo("elValue");
    assertThat(value.get(2)).isEqualTo("scriptValue");
  }

  @Deployment
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testOutputMapElValues() {
    runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    TreeMap<String, Object> value = (TreeMap) variable.getValue();
    assertThat(value)
            .containsEntry("a", 2l)
            .containsEntry("b", 3l)
            .containsEntry("c", 4l);

  }

  @Deployment
  @Test
  public void testOutputMultipleElValue() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance var1 = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(var1).isNotNull();
    assertThat(var1.getValue()).isEqualTo(2l);
    assertThat(var1.getExecutionId()).isEqualTo(pi.getId());

    VariableInstance var2 = runtimeService.createVariableInstanceQuery().variableName("var2").singleResult();
    assertThat(var2).isNotNull();
    assertThat(var2.getValue()).isEqualTo(3l);
    assertThat(var2.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment
  @Test
  public void testOutputMultipleMixedValue() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");

    VariableInstance var1 = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(var1).isNotNull();
    assertThat(var1.getValue()).isEqualTo(2l);
    assertThat(var1.getExecutionId()).isEqualTo(pi.getId());

    VariableInstance var2 = runtimeService.createVariableInstanceQuery().variableName("var2").singleResult();
    assertThat(var2).isNotNull();
    assertThat(var2.getValue()).isEqualTo("stringConstantValue");
    assertThat(var2.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testOutputNested() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("exprKey", "b");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance var1 = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    TreeMap<String, Object> value = (TreeMap) var1.getValue();
    List<Object> nestedList = (List<Object>) value.get("a");
    assertThat(nestedList.get(0)).isEqualTo("stringInListNestedInMap");
    assertThat(nestedList.get(1)).isEqualTo("b");
    assertThat(var1.getExecutionId()).isEqualTo(pi.getId());
    assertThat(value).containsEntry("b", "stringValueWithExprKey");

    VariableInstance var2 = runtimeService.createVariableInstanceQuery().variableName("var2").singleResult();
    assertThat(var2).isNotNull();
    assertThat(var2.getValue()).isEqualTo("stringConstantValue");
    assertThat(var2.getExecutionId()).isEqualTo(pi.getId());
  }

  @Deployment
  @SuppressWarnings("unchecked")
  @Test
  public void testOutputListNestedValues() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("exprKey", "vegie");
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    List<Object> value = (List<Object>) variable.getValue();
    assertThat(value.get(0)).isEqualTo("constantStringValue");
    assertThat(value.get(1)).isEqualTo("elValue");
    assertThat(value.get(2)).isEqualTo("scriptValue");

    List<Object> nestedList = (List<Object>) value.get(3);
    List<Object> nestedNestedList = (List<Object>) nestedList.get(0);
    assertThat(nestedNestedList.get(0)).isEqualTo("a");
    assertThat(nestedNestedList.get(1)).isEqualTo("b");
    assertThat(nestedNestedList.get(2)).isEqualTo("c");
    assertThat(nestedList.get(1)).isEqualTo("d");

    TreeMap<String, Object> nestedMap = (TreeMap<String, Object>) value.get(4);
    assertThat(nestedMap)
            .containsEntry("foo", "bar")
            .containsEntry("hello", "world")
            .containsEntry("vegie", "potato");
  }

  @Deployment
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testOutputMapElKey() {


    Map<String, Object> variables = new HashMap<>();
    variables.put("varExpr1", "a");
    variables.put("varExpr2", "b");
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    TreeMap<String, Object> value = (TreeMap) variable.getValue();
    assertThat(value)
            .containsEntry("a", "potato")
            .containsEntry("b", "tomato");
  }

  @Deployment
  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Test
  public void testOutputMapElMixedKey() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("varExpr1", "a");
    variables.put("varExpr2", "b");
    runtimeService.startProcessInstanceByKey("testProcess", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var1").singleResult();
    assertThat(variable).isNotNull();
    TreeMap<String, Object> value = (TreeMap) variable.getValue();
    assertThat(value)
            .containsEntry("a", "potato")
            .containsEntry("b", "tomato");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testOutputMapElKey.bpmn")
  @Test
  public void testOutputMapElUndefinedKey() {
    try {
      runtimeService.startProcessInstanceByKey("testProcess");
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("Unknown property used in expression: ${varExpr1}", e.getMessage());
    }
  }

  // ensure Io supported on event subprocess /////////////////////////////////

  @Test
  public void testInterruptingEventSubprocessIoSupport() {
    var deploymentBuilder = repositoryService
        .createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testInterruptingEventSubprocessIoSupport.bpmn");
    try {
      deploymentBuilder.deploy();
      fail("exception expected");
    } catch (ParseException e) {
      // happy path
      testRule.assertTextPresent("operaton:inputOutput mapping unsupported for element type 'subProcess' with attribute 'triggeredByEvent = true'", e.getMessage());
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("SubProcess_1");
    }
  }

  @Deployment
  @Test
  public void testSubprocessIoSupport() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("processVar", "value");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess", variables);

    Execution subprocessExecution = runtimeService.createExecutionQuery().activityId("subprocessTask").singleResult();
    Map<String, Object> variablesLocal = runtimeService.getVariablesLocal(subprocessExecution.getId());
    assertThat(variablesLocal)
            .hasSize(1)
            .containsEntry("innerVar", "value");

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    String outerVariable = (String) runtimeService.getVariableLocal(processInstance.getId(), "outerVar");
    assertThat(outerVariable).isNotNull();
    assertThat(outerVariable).isEqualTo("value");


  }

  @Deployment
  @Test
  public void testSequentialMIActivityIoSupport() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("counter", new AtomicInteger());
    variables.put("nrOfLoops", 2);
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("miSequentialActivity", variables);

    // first sequential mi execution
    Execution miExecution = runtimeService.createExecutionQuery().activityId("miTask").singleResult();
    assertThat(miExecution).isNotNull();
    assertThat(miExecution.getId()).isNotEqualTo(instance.getId());
    assertThat(runtimeService.getVariable(miExecution.getId(), "loopCounter")).isEqualTo(0);

    // input mapping
    assertThat(runtimeService.createVariableInstanceQuery().variableName("miCounterValue").count()).isEqualTo(1);
    assertThat(runtimeService.getVariableLocal(miExecution.getId(), "miCounterValue")).isEqualTo(1);

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // second sequential mi execution
    miExecution = runtimeService.createExecutionQuery().activityId("miTask").singleResult();
    assertThat(miExecution).isNotNull();
    assertThat(miExecution.getId()).isNotEqualTo(instance.getId());
    assertThat(runtimeService.getVariable(miExecution.getId(), "loopCounter")).isEqualTo(1);

    // input mapping
    assertThat(runtimeService.createVariableInstanceQuery().variableName("miCounterValue").count()).isEqualTo(1);
    assertThat(runtimeService.getVariableLocal(miExecution.getId(), "miCounterValue")).isEqualTo(2);

    task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // variable does not exist outside of scope
    assertThat(runtimeService.createVariableInstanceQuery().variableName("miCounterValue").count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testSequentialMISubprocessIoSupport() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("counter", new AtomicInteger());
    variables.put("nrOfLoops", 2);
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("miSequentialSubprocess", variables);

    // first sequential mi execution
    Execution miScopeExecution = runtimeService.createExecutionQuery().activityId("task").singleResult();
    assertThat(miScopeExecution).isNotNull();
    assertThat(runtimeService.getVariable(miScopeExecution.getId(), "loopCounter")).isEqualTo(0);

    // input mapping
    assertThat(runtimeService.createVariableInstanceQuery().variableName("miCounterValue").count()).isEqualTo(1);
    assertThat(runtimeService.getVariableLocal(miScopeExecution.getId(), "miCounterValue")).isEqualTo(1);

    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // second sequential mi execution
    miScopeExecution = runtimeService.createExecutionQuery().activityId("task").singleResult();
    assertThat(miScopeExecution).isNotNull();
    assertThat(miScopeExecution.getId()).isNotEqualTo(instance.getId());
    assertThat(runtimeService.getVariable(miScopeExecution.getId(), "loopCounter")).isEqualTo(1);

    // input mapping
    assertThat(runtimeService.createVariableInstanceQuery().variableName("miCounterValue").count()).isEqualTo(1);
    assertThat(runtimeService.getVariableLocal(miScopeExecution.getId(), "miCounterValue")).isEqualTo(2);

    task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());

    // variable does not exist outside of scope
    assertThat(runtimeService.createVariableInstanceQuery().variableName("miCounterValue").count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testParallelMIActivityIoSupport() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("counter", new AtomicInteger());
    variables.put("nrOfLoops", 2);
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("miParallelActivity", variables);

    Set<Integer> counters = new HashSet<>();

    // first mi execution
    Execution miExecution1 = runtimeService.createExecutionQuery().activityId("miTask")
        .variableValueEquals("loopCounter", 0).singleResult();
    assertThat(miExecution1).isNotNull();
    assertThat(miExecution1.getId()).isNotEqualTo(instance.getId());
    counters.add((Integer) runtimeService.getVariableLocal(miExecution1.getId(), "miCounterValue"));

    // second mi execution
    Execution miExecution2 = runtimeService.createExecutionQuery().activityId("miTask")
        .variableValueEquals("loopCounter", 1).singleResult();
    assertThat(miExecution2).isNotNull();
    assertThat(miExecution2.getId()).isNotEqualTo(instance.getId());
    counters.add((Integer) runtimeService.getVariableLocal(miExecution2.getId(), "miCounterValue"));

    assertThat(counters).contains(1);
    assertThat(counters).contains(2);

    assertThat(runtimeService.createVariableInstanceQuery().variableName("miCounterValue").count()).isEqualTo(2);

    for (Task task : taskService.createTaskQuery().list()) {
      taskService.complete(task.getId());
    }

    // variable does not exist outside of scope
    assertThat(runtimeService.createVariableInstanceQuery().variableName("miCounterValue").count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testParallelMISubprocessIoSupport() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("counter", new AtomicInteger());
    variables.put("nrOfLoops", 2);
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("miParallelSubprocess", variables);

    Set<Integer> counters = new HashSet<>();

    // first parallel mi execution
    Execution miScopeExecution1 = runtimeService.createExecutionQuery().activityId("task")
        .variableValueEquals("loopCounter", 0).singleResult();
    assertThat(miScopeExecution1).isNotNull();
    counters.add((Integer) runtimeService.getVariableLocal(miScopeExecution1.getId(), "miCounterValue"));

    // second parallel mi execution
    Execution miScopeExecution2 = runtimeService.createExecutionQuery().activityId("task")
        .variableValueEquals("loopCounter", 1).singleResult();
    assertThat(miScopeExecution2).isNotNull();
    assertThat(miScopeExecution2.getId()).isNotEqualTo(instance.getId());
    counters.add((Integer) runtimeService.getVariableLocal(miScopeExecution2.getId(), "miCounterValue"));

    assertThat(counters).contains(1);
    assertThat(counters).contains(2);

    assertThat(runtimeService.createVariableInstanceQuery().variableName("miCounterValue").count()).isEqualTo(2);

    for (Task task : taskService.createTaskQuery().list()) {
      taskService.complete(task.getId());
    }

    // variable does not exist outside of scope
    assertThat(runtimeService.createVariableInstanceQuery().variableName("miCounterValue").count()).isEqualTo(0);
  }

  @Test
  public void testMIOutputMappingDisallowed() {
    var deploymentBuilder = repositoryService.createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testMIOutputMappingDisallowed.bpmn20.xml");
    try {
      deploymentBuilder.deploy();
      fail("Exception expected");
    } catch (ParseException e) {
      testRule.assertTextPresent("operaton:outputParameter not allowed for multi-instance constructs", e.getMessage());
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("miTask");
    }

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testThrowErrorInScriptInputOutputMapping.bpmn")
  @Ignore
  @Test
  public void testBpmnErrorInScriptInputMapping() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", "in");
    variables.put("exception", new BpmnError("error"));
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    //we will only reach the user task if the BPMNError from the script was handled by the boundary event
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("User Task");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testThrowErrorInScriptInputOutputMapping.bpmn")
  @Test
  public void testExceptionInScriptInputMapping() {
    String exceptionMessage = "myException";
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", "in");
    variables.put("exception", new RuntimeException(exceptionMessage));
    try {
      runtimeService.startProcessInstanceByKey("testProcess", variables);
    } catch(RuntimeException re){
      assertThat(re.getMessage()).contains(exceptionMessage);
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testThrowErrorInScriptInputOutputMapping.bpmn")
  @Ignore
  @Test
  public void testBpmnErrorInScriptOutputMapping() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", "out");
    variables.put("exception", new BpmnError("error"));
    runtimeService.startProcessInstanceByKey("testProcess", variables);
    //we will only reach the user task if the BPMNError from the script was handled by the boundary event
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("User Task");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/iomapping/InputOutputTest.testThrowErrorInScriptInputOutputMapping.bpmn")
  @Test
  public void testExceptionInScriptOutputMapping() {
    String exceptionMessage = "myException";
    Map<String, Object> variables = new HashMap<>();
    variables.put("throwInMapping", "out");
    variables.put("exception", new RuntimeException(exceptionMessage));
    try {
      runtimeService.startProcessInstanceByKey("testProcess", variables);
    } catch(RuntimeException re){
      assertThat(re.getMessage()).contains(exceptionMessage);
    }
  }

  @Deployment
  @Ignore
  @Test
  public void testOutputMappingOnErrorBoundaryEvent() {

    // case 1: no error occurs
    runtimeService.startProcessInstanceByKey("testProcess");

    Task task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskOk");

    // then: variable mapped exists
    assertThat(runtimeService.createVariableInstanceQuery().variableName("localNotMapped").count()).isEqualTo(0);
    assertThat(runtimeService.createVariableInstanceQuery().variableName("localMapped").count()).isEqualTo(0);
    assertThat(runtimeService.createVariableInstanceQuery().variableName("mapped").count()).isEqualTo(1);

    taskService.complete(task.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

    // case 2: error occurs
    runtimeService.startProcessInstanceByKey("testProcess", Collections.<String, Object>singletonMap("throwError", true));

    task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskError");

    assertThat(runtimeService.createVariableInstanceQuery().variableName("localNotMapped").count()).isEqualTo(0);
    assertThat(runtimeService.createVariableInstanceQuery().variableName("localMapped").count()).isEqualTo(0);
    assertThat(runtimeService.createVariableInstanceQuery().variableName("mapped").count()).isEqualTo(0);

    taskService.complete(task.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Ignore
  @Test
  public void testOutputMappingOnMessageBoundaryEvent() {

    // case 1: no error occurs
    runtimeService.startProcessInstanceByKey("testProcess");

    Task task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("wait");

    taskService.complete(task.getId());

    task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskOk");

    // then: variable mapped exists
    assertThat(runtimeService.createVariableInstanceQuery().variableName("mapped").count()).isEqualTo(1);

    taskService.complete(task.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

    // case 2: error occurs
    runtimeService.startProcessInstanceByKey("testProcess", Collections.<String, Object>singletonMap("throwError", true));

    task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("wait");

    runtimeService.correlateMessage("message");

    task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskError");

    assertThat(runtimeService.createVariableInstanceQuery().variableName("mapped").count()).isEqualTo(0);

    taskService.complete(task.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Ignore
  @Test
  public void testOutputMappingOnTimerBoundaryEvent() {

    // case 1: no error occurs
    runtimeService.startProcessInstanceByKey("testProcess");

    Task task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("wait");

    taskService.complete(task.getId());

    task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskOk");

    // then: variable mapped exists
    assertThat(runtimeService.createVariableInstanceQuery().variableName("mapped").count()).isEqualTo(1);

    taskService.complete(task.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);

    // case 2: error occurs
    runtimeService.startProcessInstanceByKey("testProcess", Collections.<String, Object>singletonMap("throwError", true));

    task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("wait");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    managementService.executeJob(job.getId());

    task = taskService.createTaskQuery().singleResult();

    assertThat(task).isNotNull();
    assertThat(task.getTaskDefinitionKey()).isEqualTo("taskError");

    assertThat(runtimeService.createVariableInstanceQuery().variableName("mapped").count()).isEqualTo(0);

    taskService.complete(task.getId());

    assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment
  @Test
  public void testScopeActivityInstanceId() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstanceId);
    ActivityInstance theTaskInstance = tree.getActivityInstances("theTask")[0];

    // when
    VariableInstance variableInstance = runtimeService
      .createVariableInstanceQuery()
      .singleResult();

    // then
    assertThat(variableInstance.getActivityInstanceId()).isEqualTo(theTaskInstance.getId());
  }

  @Test
  public void testCompositeExpressionForInputValue() {

    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
      .startEvent()
      .receiveTask()
        .operatonInputParameter("var", "Hello World${'!'}")
      .endEvent("end")
      .done();

   testRule.deploy(instance);
    runtimeService.startProcessInstanceByKey("Process");

    // when
    VariableInstance variableInstance = runtimeService
      .createVariableInstanceQuery()
      .variableName("var")
      .singleResult();

    // then
    assertThat(variableInstance.getValue()).isEqualTo("Hello World!");
  }

  @Test
  public void testCompositeExpressionForOutputValue() {

    // given
    BpmnModelInstance instance = Bpmn.createExecutableProcess("Process")
      .startEvent()
      .serviceTask()
        .operatonExpression("${true}")
        .operatonInputParameter("var1", "World!")
        .operatonOutputParameter("var2", "Hello ${var1}")
      .userTask()
      .endEvent("end")
      .done();

   testRule.deploy(instance);
    runtimeService.startProcessInstanceByKey("Process");

    // when
    VariableInstance variableInstance = runtimeService
      .createVariableInstanceQuery()
      .variableName("var2")
      .singleResult();

    // then
    assertThat(variableInstance.getValue()).isEqualTo("Hello World!");
  }

  @Deployment
  @Test
  public void testOutputPlainTask() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("foo", "bar");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process", variables);

    VariableInstance variable = runtimeService.createVariableInstanceQuery().variableName("var").singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isEqualTo("baroque");
    assertThat(variable.getExecutionId()).isEqualTo(pi.getId());
  }
}
