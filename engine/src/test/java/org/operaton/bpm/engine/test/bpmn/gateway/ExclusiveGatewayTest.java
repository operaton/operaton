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
package org.operaton.bpm.engine.test.bpmn.gateway;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.Problem;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.commons.utils.CollectionUtil;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Joram Barrez
 */
class ExclusiveGatewayTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  RepositoryService repositoryService;
  TaskService taskService;

  @Deployment
  @Test
  void testDivergingExclusiveGateway() {
    for (int i = 1; i <= 3; i++) {
      ProcessInstance pi = runtimeService.startProcessInstanceByKey("exclusiveGwDiverging", CollectionUtil.singletonMap("input", i));
      assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("Task " + i);
      runtimeService.deleteProcessInstance(pi.getId(), "testing deletion");
    }
  }

  @Deployment
  @Test
  void testMergingExclusiveGateway() {
    runtimeService.startProcessInstanceByKey("exclusiveGwMerging");
    assertThat(taskService.createTaskQuery().count()).isEqualTo(3);
  }

  // If there are multiple outgoing seqFlow with valid conditions, the first
  // defined one should be chosen.
  @Deployment
  @Test
  void testMultipleValidConditions() {
    runtimeService.startProcessInstanceByKey("exclusiveGwMultipleValidConditions", CollectionUtil.singletonMap("input", 5));
    assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("Task 2");
  }

  @Deployment
  @Test
  void testNoSequenceFlowSelected() {
    // given
    var variables = CollectionUtil.singletonMap("input", 4);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("exclusiveGwNoSeqFlowSelected", variables))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("ENGINE-02004 No outgoing sequence flow for the element with id 'exclusiveGw' could be selected for continuing the process.");
  }

  /**
   * Test for bug ACT-10: whitespaces/newlines in expressions lead to exceptions
   */
  @Deployment
  @Test
  void testWhitespaceInExpression() {
    // Starting a process instance will lead to an exception if whitespace are incorrectly handled
    Map<String, Object> variables = Map.of("input", 1);
    assertThatCode(() -> runtimeService.startProcessInstanceByKey("whiteSpaceInExpression", variables))
      .doesNotThrowAnyException();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/gateway/ExclusiveGatewayTest.testDivergingExclusiveGateway.bpmn20.xml"})
  @Test
  void testUnknownVariableInExpression() {
    // given
    var variables = CollectionUtil.singletonMap("iinput", 1);

    // when/then
    // Instead of 'input' we're starting a process instance with the name 'iinput' (ie. a typo)
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("exclusiveGwDiverging", variables))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Unknown property used in expression");
  }

  @Deployment
  @Test
  void testDecideBasedOnBeanProperty() {
    runtimeService.startProcessInstanceByKey("decisionBasedOnBeanProperty",
            CollectionUtil.singletonMap("order", new ExclusiveGatewayTestOrder(150)));

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("Standard service");
  }

  @Deployment
  @Test
  void testDecideBasedOnListOrArrayOfBeans() {
    List<ExclusiveGatewayTestOrder> orders = new ArrayList<>();
    orders.add(new ExclusiveGatewayTestOrder(50));
    orders.add(new ExclusiveGatewayTestOrder(300));
    orders.add(new ExclusiveGatewayTestOrder(175));

    ProcessInstance pi = runtimeService.startProcessInstanceByKey(
            "decisionBasedOnListOrArrayOfBeans", CollectionUtil.singletonMap("orders", orders));

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("Gold Member service");


    // Arrays are usable in exactly the same way
    ExclusiveGatewayTestOrder[] orderArray = orders.toArray(new ExclusiveGatewayTestOrder[orders.size()]);
    orderArray[1].setPrice(10);
    pi = runtimeService.startProcessInstanceByKey(
            "decisionBasedOnListOrArrayOfBeans", CollectionUtil.singletonMap("orders", orderArray));

    task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("Basic service");
  }

  @Deployment
  @Test
  void testDecideBasedOnBeanMethod() {
    runtimeService.startProcessInstanceByKey("decisionBasedOnBeanMethod",
            CollectionUtil.singletonMap("order", new ExclusiveGatewayTestOrder(300)));

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("Gold Member service");
  }

  @Deployment
  @Test
  void testInvalidMethodExpression() {
    // given
    var variables = CollectionUtil.singletonMap("order", new ExclusiveGatewayTestOrder(50));

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("invalidMethodExpression", variables))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Unknown method used in expression");
  }

  @Deployment
  @Test
  void testDefaultSequenceFlow() {

    // Input == 1 -> default is not selected
    String procId = runtimeService.startProcessInstanceByKey("exclusiveGwDefaultSequenceFlow",
            CollectionUtil.singletonMap("input", 1)).getId();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Input is one");
    runtimeService.deleteProcessInstance(procId, null);

    runtimeService.startProcessInstanceByKey("exclusiveGwDefaultSequenceFlow",
            CollectionUtil.singletonMap("input", 5));
    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Default input");
  }

  @Deployment
  @Test
  void testNoIdOnSequenceFlow() {
    runtimeService.startProcessInstanceByKey("noIdOnSequenceFlow", CollectionUtil.singletonMap("input", 3));
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Input is more than one");
  }

  @Test
  void testFlowWithoutConditionNoDefaultFlow() {
    String flowWithoutConditionNoDefaultFlow = "<?xml version='1.0' encoding='UTF-8'?>" +
            "<definitions id='definitions' xmlns='http://www.omg.org/spec/BPMN/20100524/MODEL' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:activiti='http://activiti.org/bpmn' targetNamespace='Examples'>" +
            "  <process id='exclusiveGwDefaultSequenceFlow' isExecutable='true'> " +
            "    <startEvent id='theStart' /> " +
            "    <sequenceFlow id='flow1' sourceRef='theStart' targetRef='exclusiveGw' /> " +

            "    <exclusiveGateway id='exclusiveGw' name='Exclusive Gateway' /> " + // no default = "flow3" !!
            "    <sequenceFlow id='flow2' sourceRef='exclusiveGw' targetRef='theTask1'> " +
            "      <conditionExpression xsi:type='tFormalExpression'>${input == 1}</conditionExpression> " +
            "    </sequenceFlow> " +
            "    <sequenceFlow id='flow3' sourceRef='exclusiveGw' targetRef='theTask2'/> " +  // one would be OK
            "    <sequenceFlow id='flow4' sourceRef='exclusiveGw' targetRef='theTask2'/> " +  // but two unconditional not!

            "    <userTask id='theTask1' name='Input is one' /> " +
            "    <userTask id='theTask2' name='Default input' /> " +
            "  </process>" +
            "</definitions>";
    var deploymentBuilder = repositoryService.createDeployment().addString("myprocess.bpmn20.xml", flowWithoutConditionNoDefaultFlow);
    try {
      deploymentBuilder.deploy();
      fail("Could deploy a process definition with a sequence flow out of a XOR Gateway without condition with is not the default flow.");
    }
    catch (ParseException e) {
      assertThat(e.getMessage()).contains("Exclusive Gateway 'exclusiveGw' has outgoing sequence flow 'flow3' without condition which is not the default flow.");
      Problem error = e.getResourceReports().get(0).getErrors().get(0);
      assertThat(error.getMainElementId()).isEqualTo("exclusiveGw");
      assertThat(error.getElementIds()).containsExactlyInAnyOrder("exclusiveGw", "flow3");
    }
  }

  @Test
  void testDefaultFlowWithCondition() {
    String defaultFlowWithCondition = "<?xml version='1.0' encoding='UTF-8'?>" +
            "<definitions id='definitions' xmlns='http://www.omg.org/spec/BPMN/20100524/MODEL' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:activiti='http://activiti.org/bpmn' targetNamespace='Examples'>" +
            "  <process id='exclusiveGwDefaultSequenceFlow' isExecutable='true'> " +
            "    <startEvent id='theStart' /> " +
            "    <sequenceFlow id='flow1' sourceRef='theStart' targetRef='exclusiveGw' /> " +

            "    <exclusiveGateway id='exclusiveGw' name='Exclusive Gateway' default='flow3' /> " +
            "    <sequenceFlow id='flow2' sourceRef='exclusiveGw' targetRef='theTask1'> " +
            "      <conditionExpression xsi:type='tFormalExpression'>${input == 1}</conditionExpression> " +
            "    </sequenceFlow> " +
            "    <sequenceFlow id='flow3' sourceRef='exclusiveGw' targetRef='theTask2'> " +
            "      <conditionExpression xsi:type='tFormalExpression'>${input == 3}</conditionExpression> " +
            "    </sequenceFlow> " +

            "    <userTask id='theTask1' name='Input is one' /> " +
            "    <userTask id='theTask2' name='Default input' /> " +
            "  </process>" +
            "</definitions>";
    var deploymentBuilder = repositoryService.createDeployment().addString("myprocess.bpmn20.xml", defaultFlowWithCondition);
    try {
      deploymentBuilder.deploy();
      fail("Could deploy a process definition with a sequence flow out of a XOR Gateway without condition with is not the default flow.");
    }
    catch (ParseException e) {
      assertThat(e.getMessage()).contains("Exclusive Gateway 'exclusiveGw' has outgoing sequence flow 'flow3' which is the default flow but has a condition too.");
      Problem error = e.getResourceReports().get(0).getErrors().get(0);
      assertThat(error.getMainElementId()).isEqualTo("exclusiveGw");
      assertThat(error.getElementIds()).containsExactlyInAnyOrder("exclusiveGw", "flow3");
    }
  }

  @Test
  void testNoOutgoingFlow() {
    String noOutgoingFlow = "<?xml version='1.0' encoding='UTF-8'?>" +
            "<definitions id='definitions' xmlns='http://www.omg.org/spec/BPMN/20100524/MODEL' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:activiti='http://activiti.org/bpmn' targetNamespace='Examples'>" +
            "  <process id='exclusiveGwDefaultSequenceFlow' isExecutable='true'> " +
            "    <startEvent id='theStart' /> " +
            "    <sequenceFlow id='flow1' sourceRef='theStart' targetRef='exclusiveGw' /> " +
            "    <exclusiveGateway id='exclusiveGw' name='Exclusive Gateway' /> " +
            "  </process>" +
            "</definitions>";
    var deploymentBuilder = repositoryService.createDeployment().addString("myprocess.bpmn20.xml", noOutgoingFlow);
    try {
      deploymentBuilder.deploy();
      fail("Could deploy a process definition with a sequence flow out of a XOR Gateway without condition with is not the default flow.");
    }
    catch (ParseException e) {
      assertThat(e.getMessage()).contains("Exclusive Gateway 'exclusiveGw' has no outgoing sequence flows.");
      assertThat(e.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("exclusiveGw");
    }

  }

  // see CAM-4172
  @Deployment
  @Test
  void testLoopWithManyIterations() {
    int numOfIterations = 1000;

    VariableMap variables = Variables.createVariables().putValue("numOfIterations", numOfIterations);
    // this should not fail
    assertThatCode(() -> runtimeService.startProcessInstanceByKey("testProcess", variables))
      .doesNotThrowAnyException();
  }

  /**
   * The test process has an XOR gateway where, the 'input' variable is used to
   * select one of the outgoing sequence flow. Every one of those sequence flow
   * goes to another task, allowing us to test the decision very easily.
   */
  @Deployment
  @Test
  void testDecisionFunctionality() {

    Map<String, Object> variables = new HashMap<>();

    // Test with input == 1
    variables.put("input", 1);
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("exclusiveGateway", variables);
    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task.getName()).isEqualTo("Send e-mail for more information");

    // Test with input == 2
    variables.put("input", 2);
    pi = runtimeService.startProcessInstanceByKey("exclusiveGateway", variables);
    task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task.getName()).isEqualTo("Check account balance");

    // Test with input == 3
    variables.put("input", 3);
    pi = runtimeService.startProcessInstanceByKey("exclusiveGateway", variables);
    task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
    assertThat(task.getName()).isEqualTo("Call customer");

    // Test with input == 4
    variables.put("input", 4);
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("exclusiveGateway", variables)).isInstanceOf(ProcessEngineException.class);

  }
}
