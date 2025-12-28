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
package org.operaton.bpm.engine.test.bpmn.el;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.impl.mock.Mocks;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author Frederik Heremans
 */
class ExpressionManagerTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RepositoryService repositoryService;
  RuntimeService runtimeService;
  IdentityService identityService;
  TaskService taskService;
  HistoryService historyService;

  protected String deploymentId;

  @AfterEach
  void clear() {
    Mocks.reset();

    if (deploymentId != null) {
      repositoryService.deleteDeployment(deploymentId, true);
      deploymentId = null;
    }
  }

  @Deployment
  @Test
  void testMethodExpressions() {
    // Process contains 2 service tasks. one containing a method with no params, the other
    // contains a method with 2 params. When the process completes without exception,
    // test passed.
    Map<String, Object> vars = new HashMap<>();
    vars.put("aString", "abcdefgh");
    runtimeService.startProcessInstanceByKey("methodExpressionProcess", vars);

    assertThat(runtimeService.createProcessInstanceQuery().processDefinitionKey("methodExpressionProcess").count()).isZero();
  }

  @Deployment
  @Test
  void testExecutionAvailable() {
    Map<String, Object> vars = new HashMap<>();

    vars.put("myVar", new ExecutionTestVariable());
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testExecutionAvailableProcess", vars);

    // Check of the testMethod has been called with the current execution
    String value = (String) runtimeService.getVariable(processInstance.getId(), "testVar");
    assertThat(value).isNotNull().isEqualTo("myValue");
  }

  @Deployment
  @Test
  void testAuthenticatedUserIdAvailable() {
    try {
      // Setup authentication
      identityService.setAuthenticatedUserId("frederik");
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testAuthenticatedUserIdAvailableProcess");

      // Check if the variable that has been set in service-task is the authenticated user
      String value = (String) runtimeService.getVariable(processInstance.getId(), "theUser");
      assertThat(value).isEqualTo("frederik");
    } finally {
      // Cleanup
      identityService.clearAuthentication();
    }
  }

  @Deployment
  @Test
  void testResolvesVariablesFromDifferentScopes() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("assignee", "michael");

    runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getAssignee()).isEqualTo("michael");

    variables.put("assignee", "johnny");
    ProcessInstance secondInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);
    task = taskService.createTaskQuery().processInstanceId(secondInstance.getId()).singleResult();
    assertThat(task.getAssignee()).isEqualTo("johnny");
  }

  @Deployment
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testSetVariableByExpressionFromListener() {
    // given
    runtimeService.startProcessInstanceByKey("fieldInjectionTest", Variables.putValue("myCounter", 5));
    // when
    taskService.complete(taskService.createTaskQuery().singleResult().getId());
    // then
    assertThat(historyService.createHistoricVariableInstanceQuery().variableValueEquals("myCounter", 6).count()).isOne();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void testJuelExpressionWithNonPublicClass() {
    final BpmnModelInstance process = Bpmn.createExecutableProcess("testProcess")
        .startEvent()
          .exclusiveGateway()
            .condition("true", "${list.contains('foo')}")
            .userTask("userTask")
          .moveToLastGateway()
            .condition("false", "${!list.contains('foo')}")
            .endEvent()
        .done();

    deploymentId = repositoryService.createDeployment()
        .addModelInstance("testProcess.bpmn", process)
        .deploy()
        .getId();

    runtimeService.startProcessInstanceByKey("testProcess",
        Variables.createVariables().putValue("list", Arrays.asList("foo", "bar")));

    HistoricActivityInstance userTask = historyService.createHistoricActivityInstanceQuery()
        .activityId("userTask")
        .singleResult();
    assertThat(userTask).isNotNull();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldCompareWithBigDecimal() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("testProcess")
        .startEvent()
          .exclusiveGateway()
            .condition("true", "${total.compareTo(myValue) >= 0}")
            .userTask("userTask")
          .moveToLastGateway()
            .condition("false", "${total.compareTo(myValue) < 0}")
            .endEvent()
        .done();

    deploymentId = repositoryService.createDeployment()
        .addModelInstance("testProcess.bpmn", process)
        .deploy()
        .getId();

    // when
    runtimeService.startProcessInstanceByKey("testProcess",
        Variables.createVariables()
            .putValue("total", BigDecimal.valueOf(123))
            .putValue("myValue", BigDecimal.valueOf(0)));

    // then
    HistoricActivityInstance userTask = historyService.createHistoricActivityInstanceQuery()
        .activityId("userTask")
        .singleResult();
    assertThat(userTask).isNotNull();
  }

  @Deployment
  @Test
  void shouldResolveMethodExpressionTwoParametersSameType() {
    // given process with two service tasks that resolve expression and store the result as variable
    Map<String, Object> vars = new HashMap<>();
    vars.put("myVar", new ExpressionTestParameter());

    // when the process is started
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", vars);

    // then no exceptions are thrown and two variables are saved
    boolean task1Var = (boolean) runtimeService.getVariable(processInstance.getId(), "task1Var");
    assertThat(task1Var).isTrue();
    String task2Var = (String) runtimeService.getVariable(processInstance.getId(), "task2Var");
    assertThat(task2Var).isEqualTo("lastParam");
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldResolveMethodExpressionWithOneNullParameter() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("testProcess")
        .startEvent()
          .exclusiveGateway()
            .condition("true", "${myBean.myMethod(execution.getVariable('v'), "
                + "execution.getVariable('w'), execution.getVariable('x'), "
                + "execution.getVariable('y'), execution.getVariable('z'))}")
            .userTask("userTask")
          .moveToLastGateway()
            .condition("false", "${false}")
            .endEvent()
        .done();

    deploymentId = repositoryService.createDeployment()
        .addModelInstance("testProcess.bpmn", process)
        .deploy()
        .getId();

    Mocks.register("myBean", new MyBean());

    // when
    runtimeService.startProcessInstanceByKey("testProcess",
        Variables.createVariables()
            .putValue("v", "a")
            .putValue("w", null)
            .putValue("x", "b")
            .putValue("y", "c")
            .putValue("z", "d"));

    // then
    HistoricActivityInstance userTask = historyService.createHistoricActivityInstanceQuery()
        .activityId("userTask")
        .singleResult();

    assertThat(userTask).isNotNull();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldResolveMethodExpressionWithTwoNullParameter() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("testProcess")
        .startEvent()
          .exclusiveGateway()
            .condition("true", "${myBean.myMethod(execution.getVariable('v'), "
                + "execution.getVariable('w'), execution.getVariable('x'), "
                + "execution.getVariable('y'), execution.getVariable('z'))}")
            .userTask("userTask")
          .moveToLastGateway()
            .condition("false", "${false}")
            .endEvent()
        .done();

    deploymentId = repositoryService.createDeployment()
        .addModelInstance("testProcess.bpmn", process)
        .deploy()
        .getId();

    Mocks.register("myBean", new MyBean());

    // when
    runtimeService.startProcessInstanceByKey("testProcess",
        Variables.createVariables()
            .putValue("v", "a")
            .putValue("w", null)
            .putValue("x", "b")
            .putValue("y", null)
            .putValue("z", "d"));

    // then
    HistoricActivityInstance userTask = historyService.createHistoricActivityInstanceQuery()
        .activityId("userTask")
        .singleResult();

    assertThat(userTask).isNotNull();
  }

  @Test
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  void shouldResolveMethodExpressionWithNoNullParameter() {
    // given
    BpmnModelInstance process = Bpmn.createExecutableProcess("testProcess")
        .startEvent()
          .exclusiveGateway()
            .condition("true", "${myBean.myMethod(execution.getVariable('v'), "
                + "execution.getVariable('w'), execution.getVariable('x'), "
                + "execution.getVariable('y'), execution.getVariable('z'))}")
            .userTask("userTask")
          .moveToLastGateway()
            .condition("false", "${false}")
            .endEvent()
        .done();

    deploymentId = repositoryService.createDeployment()
        .addModelInstance("testProcess.bpmn", process)
        .deploy()
        .getId();

    Mocks.register("myBean", new MyBean());

    // when
    runtimeService.startProcessInstanceByKey("testProcess",
        Variables.createVariables()
            .putValue("v", "a")
            .putValue("w", "b")
            .putValue("x", "c")
            .putValue("y", "d")
            .putValue("z", "e"));

    // then
    HistoricActivityInstance userTask = historyService.createHistoricActivityInstanceQuery()
        .activityId("userTask")
        .singleResult();

    assertThat(userTask).isNotNull();
  }

  /*
   * The following method expression tests are inspired by the OverloadedMethodTest from Eclipse Expressly:
   * https://github.com/eclipse-ee4j/expressly.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("evaluateExpression_args")
  void shouldEvaluateMethodExpressionWithVariousArgumentTypes(String name, String expression, Object expectedOutput) {
    // given
    Mocks.register("intVal", 12345678);

    // when & then
    assertMethodExpressionResult(expression, expectedOutput);
  }

  static Stream<Arguments> evaluateExpression_args () {
    return Stream.of(
      arguments("invoke method with String arg",
        "myBean.myStringMethod('foo')", "foo"),
      arguments("invoke method with null String arg",
        "myBean.myStringMethod(execution.getVariable('foo'))", ""),
      arguments("invoke method with null primitive arg",
        "myBean.myIntMethod(execution.getVariable('foo'))", 0),
      arguments("invoke method with null Object arg",
        "myBean.myObjectMethod(execution.getVariable('foo'))", null),
      arguments("resolve method expression with no arg",
        "myBean.methodWithNoArg()", "methodWithNoArg"),
      arguments("resolve method expression with overloaded single arg - 1",
        "myBean.methodWithSingleArg(i1)", "I1"),
      arguments("resolve method expression with overloaded single arg - 2",
        "myBean.methodWithSingleArg(i2)", "I2Impl"),
      arguments("resolve method expression with overloaded single arg - 3",
        "myBean.methodWithSingleArg(i12)", "I1AndI2Impl"),
      arguments("resolve method expression with overloaded double args - 1",
        "myBean.methodWithDoubleArgs(i1, i2)", "I1Impl, I2"),
      arguments("resolve method expression with overloaded double args - 2",
        "myBean.methodWithDoubleArgs(i12, i2)", "I1, I2"),
      arguments("resolve method expression with overloaded double args - 3",
        "myBean.methodWithDoubleArgs(i12, i12)", "I1AndI2Impl, I1AndI2Impl"),
      arguments("resolve method expression with overloaded double args - 4",
        "myBean.methodWithDoubleArgs(i12s, i12)", "I1AndI2Impl, I1AndI2Impl"),
      arguments("resolve method expression with overloaded double args - 5",
        "myBean.methodWithDoubleArgs(i12s, i12s)", "I1AndI2Impl, I1AndI2Impl"),
      arguments("resolve method expression with ambiguous args - 1",
        "myBean.methodWithAmbiguousArgs(i12, i2)", "I1AndI2Impl, I2"),
      arguments("resolve method expression with ambiguous args - 2",
        "myBean.methodWithAmbiguousArgs(i1, i12)", "I1, I1AndI2Impl"),
      arguments("resolve method expression with coercible args - 1",
        "myBean.methodWithCoercibleArgs('foo', 'bar')", "String, String"),
      arguments("resolve method expression with coercible args - 2",
        "myBean.methodWithCoercibleArgs(i1, i12)", "String, String"),
      arguments("resolve method expression with coercible args - 3",
        "myBean.methodWithCoercibleArgs2(i1, 12345678)", "String, String"),
      arguments("resolve method expression with coercible args - 4",
        "myBean.methodWithCoercibleArgs2(i1, intVal)", "String, String"),
      arguments("resolve method expression with coercible args - 5",
        "myBean.methodWithCoercibleArgs2(12345678, 12345678)", "Integer, Integer"),
      arguments("resolve method expression with coercible args - 6",
        "myBean.methodWithCoercibleArgs2(intVal, intVal)", "Integer, Integer"),
      arguments("resolve method expression with var args - 1",
        "myBean.methodWithVarArgs(i1)", "I1, I1..."),
      arguments("resolve method expression with var args - 2",
        "myBean.methodWithVarArgs(i1, i1)", "I1, I1..."),
      arguments("resolve method expression with var args - 3",
        "myBean.methodWithVarArgs(i12, i1, i12)", "I1, I1..."),
      arguments("resolve method expression with var args - 4",
        "myBean.methodWithVarArgs2(i1)", "I1, I1AndI2Impl..."),
      arguments("resolve method expression with var args - 5",
        "myBean.methodWithVarArgs2(i12)", "I1, I1AndI2Impl..."),
      arguments("resolve method expression with var args - 6",
        "myBean.methodWithVarArgs2(i1, i1)", "I1, I1..."),
      arguments("resolve method expression with var args - 7",
        "myBean.methodWithVarArgs2(i1, i12)", "I1, I1AndI2Impl...")
    );
  }

  @Test
  void shouldFailResolveMethodExpressionNonExistingMethod() {
    // given
    String expression = "myBean.methodNotExisted()";
    // when
    assertThatThrownBy(() -> assertMethodExpressionResult(expression, null))
    // then
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Unknown method");
  }

  protected void assertMethodExpressionResult(String expression, Object result) {
    BpmnModelInstance process = Bpmn.createExecutableProcess("testProcess")
        .startEvent()
        .serviceTask()
          .operatonExpression("${" + expression + "}")
          .operatonResultVariable("output")
        .userTask()
        .endEvent()
        .done();

    deploymentId = repositoryService.createDeployment()
        .addModelInstance("testProcess.bpmn", process)
        .deploy()
        .getId();

    Mocks.register("myBean", new MyBean());
    Mocks.register("i1", new I1Impl());
    Mocks.register("i2", new I2Impl());
    Mocks.register("i12", new I1AndI2Impl());
    Mocks.register("i12s", new I1AndI2ImplSub());

    // when
    runtimeService.startProcessInstanceByKey("testProcess");

    // then
    VariableInstance output = runtimeService.createVariableInstanceQuery().variableName("output").singleResult();
    assertThat(output.getValue()).isEqualTo(result);
  }

  @SuppressWarnings("unused")
  public static class MyBean {

    public String methodWithNoArg() {
      return "methodWithNoArg";
    }

    public String methodWithSingleArg(I1 i1) {
      return "I1";
    }

    public String methodWithSingleArg(I2 i2) {
      return "I2";
    }

    public String methodWithSingleArg(I2Impl i2) {
      return "I2Impl";
    }

    public String methodWithSingleArg(I1AndI2Impl i1) {
      return "I1AndI2Impl";
    }

    public String methodWithDoubleArgs(I1 i1, I2 i2) {
      return "I1, I2";
    }

    public String methodWithDoubleArgs(I1Impl i1, I2 i2) {
      return "I1Impl, I2";
    }

    public String methodWithDoubleArgs(I1AndI2Impl i1, I1AndI2Impl i2) {
      return "I1AndI2Impl, I1AndI2Impl";
    }

    public String methodWithAmbiguousArgs(I1AndI2Impl i1, I2 i2) {
      return "I1AndI2Impl, I2";
    }

    public String methodWithAmbiguousArgs(I1 i1, I1AndI2Impl i2) {
      return "I1, I1AndI2Impl";
    }

    public String methodWithCoercibleArgs(String s1, String s2) {
      return "String, String";
    }

    public String methodWithCoercibleArgs2(String s1, String s2) {
      return "String, String";
    }

    public String methodWithCoercibleArgs2(Integer s1, Integer s2) {
      return "Integer, Integer";
    }

    public String methodWithVarArgs(I1 i1, I1... i2) {
      return "I1, I1...";
    }

    public String methodWithVarArgs2(I1 i1, I1... i2) {
      return "I1, I1...";
    }

    public String methodWithVarArgs2(I1 i1, I1AndI2Impl... i2) {
      return "I1, I1AndI2Impl...";
    }

    public boolean myMethod(String v, String w, String x, String y, String z) {
      return true;
    }

    public String myStringMethod(String v) {
      return v;
    }

    public int myIntMethod(int v) {
      return v;
    }

    public I1 myObjectMethod(I1 v) {
      return v;
    }
  }

  public interface I1 {}

  public interface I2 {}

  public static class I1Impl implements I1 {}

  public static class I2Impl implements I2 {}

  public static class I1AndI2Impl implements I1, I2 {}

  public static class I1AndI2ImplSub extends I1AndI2Impl {}

}
