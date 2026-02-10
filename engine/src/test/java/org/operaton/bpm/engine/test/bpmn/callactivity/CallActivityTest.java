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
package org.operaton.bpm.engine.test.bpmn.callactivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.EventSubscriptionQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.type.ValueType;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.builder.CallActivityBuilder;
import org.operaton.bpm.model.bpmn.instance.CallActivity;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonIn;
import org.operaton.bpm.model.bpmn.instance.operaton.OperatonOut;
import org.operaton.commons.utils.CollectionUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Joram Barrez
 * @author Nils Preusker
 * @author Bernd Ruecker
 * @author Falko Menge
 */
class CallActivityTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  TaskService taskService;
  ManagementService managementService;
  RepositoryService repositoryService;
  HistoryService historyService;

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  @Test
  void testCallSimpleSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

    // one task in the subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();
    assertThat(taskBeforeSubProcess.getName()).isEqualTo("Task before subprocess");

    // Completing the task continues the process which leads to calling the subprocess
    taskService.complete(taskBeforeSubProcess.getId());
    Task taskInSubProcess = taskQuery.singleResult();
    assertThat(taskInSubProcess.getName()).isEqualTo("Task in subprocess");

    // Completing the task in the subprocess, finishes the subprocess
    taskService.complete(taskInSubProcess.getId());
    Task taskAfterSubProcess = taskQuery.singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after subprocess");

    // Completing this task end the process instance
    taskService.complete(taskAfterSubProcess.getId());
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = { "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcess.bpmn20.xml" })
  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "Simple sub process, org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcessParentVariableAccess.bpmn20.xml",
      "Concurrent sub process, org/operaton/bpm/engine/test/bpmn/callactivity/concurrentSubProcessParentVariableAccess.bpmn20.xml"
  })
  void testAccessSuperInstanceVariables(String name, String bpmnResource) {
    testRule.deploy(bpmnResource);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

    // one task in the subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();
    assertThat(taskBeforeSubProcess.getName()).isEqualTo("Task before subprocess");

    // the variable does not yet exist
    assertThat(runtimeService.getVariable(processInstance.getId(), "greeting")).isNull();

    // completing the task executed the sub process
    taskService.complete(taskBeforeSubProcess.getId());

    // now the variable exists
    assertThat(runtimeService.getVariable(processInstance.getId(), "greeting")).isEqualTo("hello");

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "Regular expression, org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcessWithExpressions.bpmn20.xml",
      "Hash expression, org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcessWithHashExpressions.bpmn20.xml"
  })
  void testCallSimpleSubProcessWithExpressionVariants(String name, String bpmnResource) {
    testRule.deploy(bpmnResource);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSimpleSubProcess");

    // one task in the subprocess should be active after starting the process
    // instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();
    assertThat(taskBeforeSubProcess.getName()).isEqualTo("Task before subprocess");

    // Completing the task continues the process which leads to calling the
    // subprocess. The sub process we want to call is passed in as a variable
    // into this task
    taskService.setVariable(taskBeforeSubProcess.getId(), "simpleSubProcessExpression", "simpleSubProcess");
    taskService.complete(taskBeforeSubProcess.getId());
    Task taskInSubProcess = taskQuery.singleResult();
    assertThat(taskInSubProcess.getName()).isEqualTo("Task in subprocess");

    // Completing the task in the subprocess, finishes the subprocess
    taskService.complete(taskInSubProcess.getId());
    Task taskAfterSubProcess = taskQuery.singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after subprocess");

    // Completing this task end the process instance
    taskService.complete(taskAfterSubProcess.getId());
    testRule.assertProcessEnded(processInstance.getId());
  }

  /**
   * Test case for a possible tricky case: reaching the end event of the
   * subprocess leads to an end event in the super process instance.
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessEndsSuperProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessEndsSuperProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessEndsSuperProcess");

    // one task in the subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();
    assertThat(taskBeforeSubProcess.getName()).isEqualTo("Task in subprocess");

    // Completing this task ends the subprocess which leads to the end of the whole process instance
    taskService.complete(taskBeforeSubProcess.getId());
    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().list()).isEmpty();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testCallParallelSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleParallelSubProcess.bpmn20.xml"})
  @Test
  void testCallParallelSubProcess() {
    runtimeService.startProcessInstanceByKey("callParallelSubProcess");

    // The two tasks in the parallel subprocess should be active
    TaskQuery taskQuery = taskService
            .createTaskQuery()
            .orderByTaskName()
            .asc();
    List<Task> tasks = taskQuery.list();
    assertThat(tasks).hasSize(2);

    Task taskA = tasks.get(0);
    Task taskB = tasks.get(1);
    assertThat(taskA.getName()).isEqualTo("Task A");
    assertThat(taskB.getName()).isEqualTo("Task B");

    // Completing the first task should not end the subprocess
    taskService.complete(taskA.getId());
    assertThat(taskQuery.list()).hasSize(1);

    // Completing the second task should end the subprocess and end the whole process instance
    taskService.complete(taskB.getId());
    assertThat(runtimeService.createExecutionQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testCallSequentialSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcessWithExpressions.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess2.bpmn20.xml"})
  @Test
  void testCallSequentialSubProcessWithExpressions() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callSequentialSubProcess");

    // FIRST sub process calls simpleSubProcess
    // one task in the subprocess should be active after starting the process
    // instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();
    assertThat(taskBeforeSubProcess.getName()).isEqualTo("Task before subprocess");

    // Completing the task continues the process which leads to calling the
    // subprocess. The sub process we want to call is passed in as a variable
    // into this task
    taskService.setVariable(taskBeforeSubProcess.getId(), "simpleSubProcessExpression", "simpleSubProcess");
    taskService.complete(taskBeforeSubProcess.getId());
    Task taskInSubProcess = taskQuery.singleResult();
    assertThat(taskInSubProcess.getName()).isEqualTo("Task in subprocess");

    // Completing the task in the subprocess, finishes the subprocess
    taskService.complete(taskInSubProcess.getId());
    Task taskAfterSubProcess = taskQuery.singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after subprocess");

    // Completing this task end the process instance
    taskService.complete(taskAfterSubProcess.getId());

    // SECOND sub process calls simpleSubProcess2
    // one task in the subprocess should be active after starting the process
    // instance
    taskQuery = taskService.createTaskQuery();
    taskBeforeSubProcess = taskQuery.singleResult();
    assertThat(taskBeforeSubProcess.getName()).isEqualTo("Task before subprocess");

    // Completing the task continues the process which leads to calling the
    // subprocess. The sub process we want to call is passed in as a variable
    // into this task
    taskService.setVariable(taskBeforeSubProcess.getId(), "simpleSubProcessExpression", "simpleSubProcess2");
    taskService.complete(taskBeforeSubProcess.getId());
    taskInSubProcess = taskQuery.singleResult();
    assertThat(taskInSubProcess.getName()).isEqualTo("Task in subprocess 2");

    // Completing the task in the subprocess, finishes the subprocess
    taskService.complete(taskInSubProcess.getId());
    taskAfterSubProcess = taskQuery.singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after subprocess");

    // Completing this task end the process instance
    taskService.complete(taskAfterSubProcess.getId());
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testTimerOnCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testTimerOnCallActivity() {
    // After process start, the task in the subprocess should be active
    runtimeService.startProcessInstanceByKey("timerOnCallActivity");
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskInSubProcess = taskQuery.singleResult();
    assertThat(taskInSubProcess.getName()).isEqualTo("Task in subprocess");

    Job timer = managementService.createJobQuery().singleResult();
    assertThat(timer).isNotNull();

    managementService.executeJob(timer.getId());

    Task escalatedTask = taskQuery.singleResult();
    assertThat(escalatedTask.getName()).isEqualTo("Escalated Task");

    // Completing the task ends the complete process
    taskService.complete(escalatedTask.getId());
    assertThat(runtimeService.createExecutionQuery().list()).isEmpty();
  }

  /**
   * Test case for handing over process variables to a sub process
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessDataInputOutput.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessWithDataInputOutput() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("superVariable", "Hello from the super process.");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessDataInputOutput", vars);

    // one task in the subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();
    assertThat(taskBeforeSubProcess.getName()).isEqualTo("Task in subprocess");
    assertThat(runtimeService.getVariable(taskBeforeSubProcess.getProcessInstanceId(), "subVariable")).isEqualTo("Hello from the super process.");
    assertThat(taskService.getVariable(taskBeforeSubProcess.getId(), "subVariable")).isEqualTo("Hello from the super process.");

    runtimeService.setVariable(taskBeforeSubProcess.getProcessInstanceId(), "subVariable", "Hello from sub process.");

    // super variable is unchanged
    assertThat(runtimeService.getVariable(processInstance.getId(), "superVariable")).isEqualTo("Hello from the super process.");

    // Completing this task ends the subprocess which leads to a task in the super process
    taskService.complete(taskBeforeSubProcess.getId());

    // one task in the subprocess should be active after starting the process instance
    Task taskAfterSubProcess = taskQuery.singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task in super process");
    assertThat(runtimeService.getVariable(processInstance.getId(), "superVariable")).isEqualTo("Hello from sub process.");
    assertThat(taskService.getVariable(taskAfterSubProcess.getId(), "superVariable")).isEqualTo("Hello from sub process.");

    vars.clear();
    vars.put("x", 5L);

    // Completing this task ends the super process which leads to a task in the super process
    taskService.complete(taskAfterSubProcess.getId(), vars);

    // now we are the second time in the sub process but passed variables via expressions
    Task taskInSecondSubProcess = taskQuery.singleResult();
    assertThat(taskInSecondSubProcess.getName()).isEqualTo("Task in subprocess");
    assertThat(runtimeService.getVariable(taskInSecondSubProcess.getProcessInstanceId(), "y")).isEqualTo(10L);
    assertThat(taskService.getVariable(taskInSecondSubProcess.getId(), "y")).isEqualTo(10L);

    // Completing this task ends the subprocess which leads to a task in the super process
    taskService.complete(taskInSecondSubProcess.getId());

    // one task in the subprocess should be active after starting the process instance
    Task taskAfterSecondSubProcess = taskQuery.singleResult();
    assertThat(taskAfterSecondSubProcess.getName()).isEqualTo("Task in super process");
    assertThat(runtimeService.getVariable(taskAfterSecondSubProcess.getProcessInstanceId(), "z")).isEqualTo(15L);
    assertThat(taskService.getVariable(taskAfterSecondSubProcess.getId(), "z")).isEqualTo(15L);

    // and end last task in Super process
    taskService.complete(taskAfterSecondSubProcess.getId());

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().list()).isEmpty();
  }

  /**
   * Test case for handing over process variables to a sub process via the typed
   * api and passing only certain variables
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessLimitedDataInputOutputTypedApi.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessWithLimitedDataInputOutputTypedApi() {

    TypedValue superVariable = Variables.stringValue(null);
    VariableMap vars = Variables.createVariables();
    vars.putValueTyped("superVariable", superVariable);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessDataInputOutput", vars);

    // one task in the subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskInSubProcess = taskQuery.singleResult();
    assertThat(taskInSubProcess.getName()).isEqualTo("Task in subprocess");
    assertThat(runtimeService.<TypedValue>getVariableTyped(taskInSubProcess.getProcessInstanceId(), "subVariable")).isEqualTo(superVariable);
    assertThat(taskService.<TypedValue>getVariableTyped(taskInSubProcess.getId(), "subVariable")).isEqualTo(superVariable);

    TypedValue subVariable = Variables.stringValue(null);
    runtimeService.setVariable(taskInSubProcess.getProcessInstanceId(), "subVariable", subVariable);

    // super variable is unchanged
    assertThat(runtimeService.<TypedValue>getVariableTyped(processInstance.getId(), "superVariable")).isEqualTo(superVariable);

    // Completing this task ends the subprocess which leads to a task in the super process
    taskService.complete(taskInSubProcess.getId());

    Task taskAfterSubProcess = taskQuery.singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task in super process");
    assertThat(runtimeService.<TypedValue>getVariableTyped(processInstance.getId(), "superVariable")).isEqualTo(subVariable);
    assertThat(taskService.<TypedValue>getVariableTyped(taskAfterSubProcess.getId(), "superVariable")).isEqualTo(subVariable);

    // Completing this task ends the super process which leads to a task in the super process
    taskService.complete(taskAfterSubProcess.getId());

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().list()).isEmpty();
  }

  /**
   * Test case for handing over process variables to a sub process via the typed
   * api and passing all variables
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessAllDataInputOutputTypedApi.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessWithAllDataInputOutputTypedApi() {

    TypedValue superVariable = Variables.stringValue(null);
    VariableMap vars = Variables.createVariables();
    vars.putValueTyped("superVariable", superVariable);

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessDataInputOutput", vars);

    // one task in the subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskInSubProcess = taskQuery.singleResult();
    assertThat(taskInSubProcess.getName()).isEqualTo("Task in subprocess");
    assertThat(runtimeService.<TypedValue>getVariableTyped(taskInSubProcess.getProcessInstanceId(), "superVariable")).isEqualTo(superVariable);
    assertThat(taskService.<TypedValue>getVariableTyped(taskInSubProcess.getId(), "superVariable")).isEqualTo(superVariable);

    TypedValue subVariable = Variables.stringValue(null);
    runtimeService.setVariable(taskInSubProcess.getProcessInstanceId(), "subVariable", subVariable);

    // Completing this task ends the subprocess which leads to a task in the super process
    taskService.complete(taskInSubProcess.getId());

    Task taskAfterSubProcess = taskQuery.singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task in super process");
    assertThat(runtimeService.<TypedValue>getVariableTyped(processInstance.getId(), "subVariable")).isEqualTo(subVariable);
    assertThat(taskService.<TypedValue>getVariableTyped(taskAfterSubProcess.getId(), "superVariable")).isEqualTo(superVariable);

    // Completing this task ends the super process which leads to a task in the super process
    taskService.complete(taskAfterSubProcess.getId());

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().list()).isEmpty();
  }

  /**
   * Test case for handing over process variables without target attribute set
   */
  @Test
  void testSubProcessWithDataInputOutputWithoutTarget() {
    String processId = "subProcessDataInputOutputWithoutTarget";

    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess(processId)
            .startEvent()
            .callActivity("callActivity")
            .calledElement("simpleSubProcess")
            .userTask()
            .endEvent()
            .done();

    CallActivityBuilder callActivityBuilder = ((CallActivity) modelInstance.getModelElementById("callActivity")).builder();

    // create operaton:in with source but without target
    OperatonIn operatonIn = modelInstance.newInstance(OperatonIn.class);
    operatonIn.setOperatonSource("superVariable");
    callActivityBuilder.addExtensionElement(operatonIn);

    deployAndExpectException(modelInstance);
    // set target
    operatonIn.setOperatonTarget("subVariable");

    // create operaton:in with sourceExpression but without target
    operatonIn = modelInstance.newInstance(OperatonIn.class);
    operatonIn.setOperatonSourceExpression("${x+5}");
    callActivityBuilder.addExtensionElement(operatonIn);

    deployAndExpectException(modelInstance);
    // set target
    operatonIn.setOperatonTarget("subVariable2");

    // create operaton:out with source but without target
    OperatonOut operatonOut = modelInstance.newInstance(OperatonOut.class);
    operatonOut.setOperatonSource("subVariable");
    callActivityBuilder.addExtensionElement(operatonOut);

    deployAndExpectException(modelInstance);
    // set target
    operatonOut.setOperatonTarget("superVariable");

    // create operaton:out with sourceExpression but without target
    operatonOut = modelInstance.newInstance(OperatonOut.class);
    operatonOut.setOperatonSourceExpression("${y+1}");
    callActivityBuilder.addExtensionElement(operatonOut);

    deployAndExpectException(modelInstance);
    // set target
    operatonOut.setOperatonTarget("superVariable2");

    // then - no exception should be thrown
    String deploymentId = repositoryService.createDeployment().addModelInstance("process.bpmn", modelInstance).deploy().getId();
    repositoryService.deleteDeployment(deploymentId, true);
  }

  /**
   * Test case for handing over a null process variables to a sub process
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessDataInputOutput.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/dataSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessWithNullDataInput() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("subProcessDataInputOutput").getId();

    // the variable named "subVariable" is not set on process instance
    VariableInstance variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("subVariable")
            .singleResult();
    assertThat(variable).isNull();

    variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("superVariable")
            .singleResult();
    assertThat(variable).isNull();

    // the sub process instance is in the task
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("Task in subprocess");

    // the value of "subVariable" is null
    assertThat(taskService.getVariable(task.getId(), "subVariable")).isNull();

    String subProcessInstanceId = task.getProcessInstanceId();
    assertThat(subProcessInstanceId).isNotEqualTo(processInstanceId);

    // the variable "subVariable" is set on the sub process instance
    variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(subProcessInstanceId)
            .variableName("subVariable")
            .singleResult();

    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isNull();
    assertThat(variable.getName()).isEqualTo("subVariable");
  }

  /**
   * Test case for handing over a null process variables to a sub process
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessDataInputOutputAsExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/dataSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessWithNullDataInputAsExpression() {
    Map<String, Object> params = new HashMap<>();
    params.put("superVariable", null);
    String processInstanceId = runtimeService.startProcessInstanceByKey("subProcessDataInputOutput", params).getId();

    // the variable named "subVariable" is not set on process instance
    VariableInstance variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("subVariable")
            .singleResult();
    assertThat(variable).isNull();

    variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("superVariable")
            .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isNull();

    // the sub process instance is in the task
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("Task in subprocess");

    // the value of "subVariable" is null
    assertThat(taskService.getVariable(task.getId(), "subVariable")).isNull();

    String subProcessInstanceId = task.getProcessInstanceId();
    assertThat(subProcessInstanceId).isNotEqualTo(processInstanceId);

    // the variable "subVariable" is set on the sub process instance
    variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(subProcessInstanceId)
            .variableName("subVariable")
            .singleResult();

    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isNull();
    assertThat(variable.getName()).isEqualTo("subVariable");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessDataInputOutput.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/dataSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessWithNullDataOutput() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("subProcessDataInputOutput").getId();

    // the variable named "subVariable" is not set on process instance
    VariableInstance variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("subVariable")
            .singleResult();
    assertThat(variable).isNull();

    variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("superVariable")
            .singleResult();
    assertThat(variable).isNull();

    // the sub process instance is in the task
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("Task in subprocess");

    taskService.complete(task.getId());

    variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("subVariable")
            .singleResult();
    assertThat(variable).isNull();

    variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("superVariable")
            .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isNull();

    variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("hisLocalVariable")
            .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isNull();

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessDataInputOutputAsExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/dataSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessWithNullDataOutputAsExpression() {
    Map<String, Object> params = new HashMap<>();
    params.put("superVariable", null);
    String processInstanceId = runtimeService.startProcessInstanceByKey("subProcessDataInputOutput", params).getId();

    // the variable named "subVariable" is not set on process instance
    VariableInstance variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("subVariable")
            .singleResult();
    assertThat(variable).isNull();

    variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("superVariable")
            .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isNull();

    // the sub process instance is in the task
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    assertThat(task.getName()).isEqualTo("Task in subprocess");

    VariableMap variables = Variables.createVariables().putValue("myLocalVariable", null);
    taskService.complete(task.getId(), variables);

    variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("subVariable")
            .singleResult();
    assertThat(variable).isNull();

    variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("superVariable")
            .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isNull();

    variable = runtimeService
            .createVariableInstanceQuery()
            .processInstanceIdIn(processInstanceId)
            .variableName("hisLocalVariable")
            .singleResult();
    assertThat(variable).isNotNull();
    assertThat(variable.getValue()).isNull();

  }

  private void deployAndExpectException(BpmnModelInstance modelInstance) {
    // given
    var deploymentBuilder = repositoryService.createDeployment().addModelInstance("process.bpmn", modelInstance);

    // when/then
    assertThatThrownBy(() -> testRule.deploy(deploymentBuilder))
        .isInstanceOf(ParseException.class)
        .hasMessageContaining("Missing attribute 'target'")
        .extracting(e -> ((ParseException) e).getResourceReports().get(0).getErrors().get(0).getMainElementId())
        .isEqualTo("callActivity");
  }

  /**
   * Test case for handing over process variables to a sub process
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testTwoSubProcesses.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testTwoSubProcesses() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callTwoSubProcesses");

    List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList)
            .isNotNull()
            .hasSize(3);

    List<Task> taskList = taskService.createTaskQuery().list();
    assertThat(taskList)
            .isNotNull()
            .hasSize(2);

    runtimeService.deleteProcessInstance(processInstance.getId(), "Test cascading");

    instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList).isEmpty();

    taskList = taskService.createTaskQuery().list();
    assertThat(taskList).isEmpty();
  }

  /**
   * Test case for handing all over process variables to a sub process
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessAllDataInputOutput.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessAllDataInputOutput() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("superVariable", "Hello from the super process.");
    vars.put("testVariable", "Only a test.");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessAllDataInputOutput", vars);

    // one task in the super process should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();
    assertThat(taskBeforeSubProcess.getName()).isEqualTo("Task before subprocess");
    assertThat(runtimeService.getVariable(taskBeforeSubProcess.getProcessInstanceId(), "superVariable")).isEqualTo("Hello from the super process.");
    assertThat(taskService.getVariable(taskBeforeSubProcess.getId(), "superVariable")).isEqualTo("Hello from the super process.");
    assertThat(runtimeService.getVariable(taskBeforeSubProcess.getProcessInstanceId(), "testVariable")).isEqualTo("Only a test.");
    assertThat(taskService.getVariable(taskBeforeSubProcess.getId(), "testVariable")).isEqualTo("Only a test.");

    taskService.complete(taskBeforeSubProcess.getId());

    // one task in sub process should be active after starting sub process instance
    taskQuery = taskService.createTaskQuery();
    Task taskInSubProcess = taskQuery.singleResult();
    assertThat(taskInSubProcess.getName()).isEqualTo("Task in subprocess");
    assertThat(runtimeService.getVariable(taskInSubProcess.getProcessInstanceId(), "superVariable")).isEqualTo("Hello from the super process.");
    assertThat(taskService.getVariable(taskInSubProcess.getId(), "superVariable")).isEqualTo("Hello from the super process.");
    assertThat(runtimeService.getVariable(taskInSubProcess.getProcessInstanceId(), "testVariable")).isEqualTo("Only a test.");
    assertThat(taskService.getVariable(taskInSubProcess.getId(), "testVariable")).isEqualTo("Only a test.");

    // changed variables in sub process
    runtimeService.setVariable(taskInSubProcess.getProcessInstanceId(), "superVariable", "Hello from sub process.");
    runtimeService.setVariable(taskInSubProcess.getProcessInstanceId(), "testVariable", "Variable changed in sub process.");

    taskService.complete(taskInSubProcess.getId());

    // task after sub process in super process
    taskQuery = taskService.createTaskQuery();
    Task taskAfterSubProcess = taskQuery.singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after subprocess");

    // variables are changed after finished sub process
    assertThat(runtimeService.getVariable(processInstance.getId(), "superVariable")).isEqualTo("Hello from sub process.");
    assertThat(runtimeService.getVariable(processInstance.getId(), "testVariable")).isEqualTo("Variable changed in sub process.");

    taskService.complete(taskAfterSubProcess.getId());

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().list()).isEmpty();
  }

  /**
   * Test case for handing all over process variables to a sub process
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessAllDataInputOutputWithAdditionalInputMapping.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessAllDataInputOutputWithAdditionalInputMapping() {
    Map<String, Object> vars = new HashMap<>();
    vars.put("superVariable", "Hello from the super process.");
    vars.put("testVariable", "Only a test.");

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessAllDataInputOutput", vars);

    // one task in the super process should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();
    assertThat(taskBeforeSubProcess.getName()).isEqualTo("Task before subprocess");
    assertThat(runtimeService.getVariable(taskBeforeSubProcess.getProcessInstanceId(), "superVariable")).isEqualTo("Hello from the super process.");
    assertThat(taskService.getVariable(taskBeforeSubProcess.getId(), "superVariable")).isEqualTo("Hello from the super process.");
    assertThat(runtimeService.getVariable(taskBeforeSubProcess.getProcessInstanceId(), "testVariable")).isEqualTo("Only a test.");
    assertThat(taskService.getVariable(taskBeforeSubProcess.getId(), "testVariable")).isEqualTo("Only a test.");

    taskService.complete(taskBeforeSubProcess.getId());

    // one task in sub process should be active after starting sub process instance
    taskQuery = taskService.createTaskQuery();
    Task taskInSubProcess = taskQuery.singleResult();
    assertThat(taskInSubProcess.getName()).isEqualTo("Task in subprocess");
    assertThat(runtimeService.getVariable(taskInSubProcess.getProcessInstanceId(), "superVariable")).isEqualTo("Hello from the super process.");
    assertThat(runtimeService.getVariable(taskInSubProcess.getProcessInstanceId(), "subVariable")).isEqualTo("Hello from the super process.");
    assertThat(taskService.getVariable(taskInSubProcess.getId(), "superVariable")).isEqualTo("Hello from the super process.");
    assertThat(runtimeService.getVariable(taskInSubProcess.getProcessInstanceId(), "testVariable")).isEqualTo("Only a test.");
    assertThat(taskService.getVariable(taskInSubProcess.getId(), "testVariable")).isEqualTo("Only a test.");

    // changed variables in sub process
    runtimeService.setVariable(taskInSubProcess.getProcessInstanceId(), "superVariable", "Hello from sub process.");
    runtimeService.setVariable(taskInSubProcess.getProcessInstanceId(), "testVariable", "Variable changed in sub process.");

    taskService.complete(taskInSubProcess.getId());

    // task after sub process in super process
    taskQuery = taskService.createTaskQuery();
    Task taskAfterSubProcess = taskQuery.singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after subprocess");

    // variables are changed after finished sub process
    assertThat(runtimeService.getVariable(processInstance.getId(), "superVariable")).isEqualTo("Hello from sub process.");
    assertThat(runtimeService.getVariable(processInstance.getId(), "testVariable")).isEqualTo("Variable changed in sub process.");

    taskService.complete(taskAfterSubProcess.getId());

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().list()).isEmpty();
  }

  /**
   * This testcase verifies that <operaton:out variables="all" /> works also in
   * case super process has no variables
   *
   * @see <a href="https://app.camunda.com/jira/browse/CAM-1617">CAM-1617</a>
   *
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessAllDataInputOutput.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessAllDataOutput() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessAllDataInputOutput");

    // one task in the super process should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();
    assertThat(taskBeforeSubProcess.getName()).isEqualTo("Task before subprocess");

    taskService.complete(taskBeforeSubProcess.getId());

    // one task in sub process should be active after starting sub process instance
    taskQuery = taskService.createTaskQuery();
    Task taskInSubProcess = taskQuery.singleResult();
    assertThat(taskInSubProcess.getName()).isEqualTo("Task in subprocess");

    // add variables to sub process
    runtimeService.setVariable(taskInSubProcess.getProcessInstanceId(), "superVariable", "Hello from sub process.");
    runtimeService.setVariable(taskInSubProcess.getProcessInstanceId(), "testVariable", "Variable changed in sub process.");

    taskService.complete(taskInSubProcess.getId());

    // task after sub process in super process
    taskQuery = taskService.createTaskQuery();
    Task taskAfterSubProcess = taskQuery.singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after subprocess");

    // variables are copied to super process instance after sub process instance finishes
    assertThat(runtimeService.getVariable(processInstance.getId(), "superVariable")).isEqualTo("Hello from sub process.");
    assertThat(runtimeService.getVariable(processInstance.getId(), "testVariable")).isEqualTo("Variable changed in sub process.");

    taskService.complete(taskAfterSubProcess.getId());

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().list()).isEmpty();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessLocalInputAllVariables.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessLocalInputAllVariables() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessLocalInputAllVariables");
    Task beforeCallActivityTask = taskService.createTaskQuery().singleResult();

    // when setting a variable in a process instance
    runtimeService.setVariable(processInstance.getId(), "callingProcessVar1", "val1");

    // and executing the call activity
    taskService.complete(beforeCallActivityTask.getId());

    // then only the local variable specified in the io mapping is passed to the called instance
    ProcessInstance calledInstance = runtimeService.createProcessInstanceQuery()
            .superProcessInstanceId(processInstance.getId())
            .singleResult();

    Map<String, Object> calledInstanceVariables = runtimeService.getVariables(calledInstance.getId());
    assertThat(calledInstanceVariables)
            .hasSize(1)
            .containsEntry("inputParameter", "val2");

    // when setting a variable in the called process instance
    runtimeService.setVariable(calledInstance.getId(), "calledProcessVar1", 42L);

    // and completing it
    Task calledProcessInstanceTask = taskService.createTaskQuery().singleResult();
    taskService.complete(calledProcessInstanceTask.getId());

    // then the call activity output variable has been mapped to the process instance execution
    // and the output mapping variable as well
    Map<String, Object> callingInstanceVariables = runtimeService.getVariables(processInstance.getId());
    assertThat(callingInstanceVariables)
            .hasSize(3)
            .containsEntry("callingProcessVar1", "val1")
            .containsEntry("calledProcessVar1", 42L)
            .containsEntry("outputParameter", 43L);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessLocalInputSingleVariable.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessLocalInputSingleVariable() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessLocalInputSingleVariable");
    Task beforeCallActivityTask = taskService.createTaskQuery().singleResult();

    // when setting a variable in a process instance
    runtimeService.setVariable(processInstance.getId(), "callingProcessVar1", "val1");

    // and executing the call activity
    taskService.complete(beforeCallActivityTask.getId());

    // then the local variable specified in the io mapping is passed to the called instance
    ProcessInstance calledInstance = runtimeService.createProcessInstanceQuery()
            .superProcessInstanceId(processInstance.getId())
            .singleResult();

    Map<String, Object> calledInstanceVariables = runtimeService.getVariables(calledInstance.getId());
    assertThat(calledInstanceVariables)
            .hasSize(1)
            .containsEntry("mappedInputParameter", "val2");

    // when setting a variable in the called process instance
    runtimeService.setVariable(calledInstance.getId(), "calledProcessVar1", 42L);

    // and completing it
    Task calledProcessInstanceTask = taskService.createTaskQuery().singleResult();
    taskService.complete(calledProcessInstanceTask.getId());

    // then the call activity output variable has been mapped to the process instance execution
    // and the output mapping variable as well
    Map<String, Object> callingInstanceVariables = runtimeService.getVariables(processInstance.getId());
    assertThat(callingInstanceVariables)
            .hasSize(4)
            .containsEntry("callingProcessVar1", "val1")
            .containsEntry("mappedInputParameter", "val2")
            .containsEntry("calledProcessVar1", 42L)
            .containsEntry("outputParameter", 43L);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessLocalInputSingleVariableExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessLocalInputSingleVariableExpression() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessLocalInputSingleVariableExpression");
    Task beforeCallActivityTask = taskService.createTaskQuery().singleResult();

    // when executing the call activity
    taskService.complete(beforeCallActivityTask.getId());

    // then the local input parameter can be resolved because its source expression variable
    // is defined in the call activity's input mapping
    ProcessInstance calledInstance = runtimeService.createProcessInstanceQuery()
            .superProcessInstanceId(processInstance.getId())
            .singleResult();

    Map<String, Object> calledInstanceVariables = runtimeService.getVariables(calledInstance.getId());
    assertThat(calledInstanceVariables)
            .hasSize(1)
            .containsEntry("mappedInputParameter", 43L);

    // and completing it
    Task callActivityTask = taskService.createTaskQuery().singleResult();
    taskService.complete(callActivityTask.getId());

    // and executing a call activity in parameter where the source variable is not mapped by an activity
    // input parameter fails
    runtimeService.setVariable(processInstance.getId(), "globalVariable", "42");
    var beforeSecondCallActivityTaskId = taskService.createTaskQuery().singleResult().getId();

    // when/then
    assertThatThrownBy(() -> taskService.complete(beforeSecondCallActivityTaskId))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Cannot resolve identifier 'globalVariable'");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessLocalOutputAllVariables.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessLocalOutputAllVariables() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessLocalOutputAllVariables");
    Task beforeCallActivityTask = taskService.createTaskQuery().singleResult();

    // when setting a variable in a process instance
    runtimeService.setVariable(processInstance.getId(), "callingProcessVar1", "val1");

    // and executing the call activity
    taskService.complete(beforeCallActivityTask.getId());

    // then all variables have been mapped into the called instance
    ProcessInstance calledInstance = runtimeService.createProcessInstanceQuery()
            .superProcessInstanceId(processInstance.getId())
            .singleResult();

    Map<String, Object> calledInstanceVariables = runtimeService.getVariables(calledInstance.getId());
    assertThat(calledInstanceVariables)
            .hasSize(2)
            .containsEntry("callingProcessVar1", "val1")
            .containsEntry("inputParameter", "val2");

    // when setting a variable in the called process instance
    runtimeService.setVariable(calledInstance.getId(), "calledProcessVar1", 42L);

    // and completing it
    Task calledProcessInstanceTask = taskService.createTaskQuery().singleResult();
    taskService.complete(calledProcessInstanceTask.getId());

    // then only the output mapping variable has been mapped into the calling process instance
    Map<String, Object> callingInstanceVariables = runtimeService.getVariables(processInstance.getId());
    assertThat(callingInstanceVariables)
            .hasSize(2)
            .containsEntry("callingProcessVar1", "val1")
            .containsEntry("outputParameter", 43L);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessLocalOutputSingleVariable.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessLocalOutputSingleVariable() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessLocalOutputSingleVariable");
    Task beforeCallActivityTask = taskService.createTaskQuery().singleResult();

    // when setting a variable in a process instance
    runtimeService.setVariable(processInstance.getId(), "callingProcessVar1", "val1");

    // and executing the call activity
    taskService.complete(beforeCallActivityTask.getId());

    // then all variables have been mapped into the called instance
    ProcessInstance calledInstance = runtimeService.createProcessInstanceQuery()
            .superProcessInstanceId(processInstance.getId())
            .singleResult();

    Map<String, Object> calledInstanceVariables = runtimeService.getVariables(calledInstance.getId());
    assertThat(calledInstanceVariables)
            .hasSize(2)
            .containsEntry("callingProcessVar1", "val1")
            .containsEntry("inputParameter", "val2");

    // when setting a variable in the called process instance
    runtimeService.setVariable(calledInstance.getId(), "calledProcessVar1", 42L);

    // and completing it
    Task calledProcessInstanceTask = taskService.createTaskQuery().singleResult();
    taskService.complete(calledProcessInstanceTask.getId());

    // then only the output mapping variable has been mapped into the calling process instance
    Map<String, Object> callingInstanceVariables = runtimeService.getVariables(processInstance.getId());
    assertThat(callingInstanceVariables)
            .hasSize(2)
            .containsEntry("callingProcessVar1", "val1")
            .containsEntry("outputParameter", 43L);
  }

  /**
   * Test case for handing businessKey to a sub process
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessBusinessKeyInput.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSubProcessBusinessKeyInput() {
    String businessKey = "myBusinessKey";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subProcessBusinessKeyInput", businessKey);

    // one task in the super process should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();
    assertThat(taskBeforeSubProcess.getName()).isEqualTo("Task before subprocess");
    assertThat(processInstance.getBusinessKey()).isEqualTo("myBusinessKey");

    taskService.complete(taskBeforeSubProcess.getId());

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      // called process started so businesskey should be written in history
      HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
      assertThat(hpi.getBusinessKey()).isEqualTo(businessKey);

      assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(businessKey).list()).hasSize(2);
    }

    // one task in sub process should be active after starting sub process instance
    taskQuery = taskService.createTaskQuery();
    Task taskInSubProcess = taskQuery.singleResult();
    assertThat(taskInSubProcess.getName()).isEqualTo("Task in subprocess");
    ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().processInstanceId(taskInSubProcess.getProcessInstanceId()).singleResult();
    assertThat(subProcessInstance.getBusinessKey()).isEqualTo("myBusinessKey");

    taskService.complete(taskInSubProcess.getId());

    // task after sub process in super process
    taskQuery = taskService.createTaskQuery();
    Task taskAfterSubProcess = taskQuery.singleResult();
    assertThat(taskAfterSubProcess.getName()).isEqualTo("Task after subprocess");

    taskService.complete(taskAfterSubProcess.getId());

    testRule.assertProcessEnded(processInstance.getId());
    assertThat(runtimeService.createExecutionQuery().list()).isEmpty();

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).finished().singleResult();
      assertThat(hpi.getBusinessKey()).isEqualTo(businessKey);

      assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(businessKey).finished().list()).hasSize(2);
    }
  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testInterruptingEventSubProcessEventSubscriptions.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/interruptingEventSubProcessEventSubscriptions.bpmn20.xml"})
  @Test
  void testInterruptingMessageEventSubProcessEventSubscriptionsInsideCallActivity() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callInterruptingEventSubProcess");

    // one task in the call activity subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskInsideCallActivity = taskQuery.singleResult();
    assertThat(taskInsideCallActivity.getTaskDefinitionKey()).isEqualTo("taskBeforeInterruptingEventSubprocess");

    // we should have no event subscriptions for the parent process
    assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    // we should have two event subscriptions for the called process instance, one for message and one for signal
    String calledProcessInstanceId = taskInsideCallActivity.getProcessInstanceId();
    EventSubscriptionQuery eventSubscriptionQuery = runtimeService.createEventSubscriptionQuery().processInstanceId(calledProcessInstanceId);
    List<EventSubscription> subscriptions = eventSubscriptionQuery.list();
    assertThat(subscriptions).hasSize(2);

    // start the message interrupting event sub process
    runtimeService.correlateMessage("newMessage");
    Task taskAfterMessageStartEvent = taskQuery.processInstanceId(calledProcessInstanceId).singleResult();
    assertThat(taskAfterMessageStartEvent.getTaskDefinitionKey()).isEqualTo("taskAfterMessageStartEvent");

    // no subscriptions left
    assertThat(eventSubscriptionQuery.count()).isZero();

    // Complete the task inside the called process instance
    taskService.complete(taskAfterMessageStartEvent.getId());

    testRule.assertProcessEnded(calledProcessInstanceId);
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testInterruptingEventSubProcessEventSubscriptions.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/interruptingEventSubProcessEventSubscriptions.bpmn20.xml"})
  @Test
  void testInterruptingSignalEventSubProcessEventSubscriptionsInsideCallActivity() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callInterruptingEventSubProcess");

    // one task in the call activity subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskInsideCallActivity = taskQuery.singleResult();
    assertThat(taskInsideCallActivity.getTaskDefinitionKey()).isEqualTo("taskBeforeInterruptingEventSubprocess");

    // we should have no event subscriptions for the parent process
    assertThat(runtimeService.createEventSubscriptionQuery().processInstanceId(processInstance.getId()).count()).isZero();
    // we should have two event subscriptions for the called process instance, one for message and one for signal
    String calledProcessInstanceId = taskInsideCallActivity.getProcessInstanceId();
    EventSubscriptionQuery eventSubscriptionQuery = runtimeService.createEventSubscriptionQuery().processInstanceId(calledProcessInstanceId);
    List<EventSubscription> subscriptions = eventSubscriptionQuery.list();
    assertThat(subscriptions).hasSize(2);

    // start the signal interrupting event sub process
    runtimeService.signalEventReceived("newSignal");
    Task taskAfterSignalStartEvent = taskQuery.processInstanceId(calledProcessInstanceId).singleResult();
    assertThat(taskAfterSignalStartEvent.getTaskDefinitionKey()).isEqualTo("taskAfterSignalStartEvent");

    // no subscriptions left
    assertThat(eventSubscriptionQuery.count()).isZero();

    // Complete the task inside the called process instance
    taskService.complete(taskAfterSignalStartEvent.getId());

    testRule.assertProcessEnded(calledProcessInstanceId);
    testRule.assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testLiteralSourceExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  @Test
  void testInputParameterLiteralSourceExpression() {
    runtimeService.startProcessInstanceByKey("process");

    String subInstanceId = runtimeService
            .createProcessInstanceQuery()
            .processDefinitionKey("simpleSubProcess")
            .singleResult()
            .getId();

    Object variable = runtimeService.getVariable(subInstanceId, "inLiteralVariable");
    assertThat(variable).isEqualTo("inLiteralValue");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testLiteralSourceExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  @Test
  void testOutputParameterLiteralSourceExpression() {
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    String taskId = taskService
            .createTaskQuery()
            .singleResult()
            .getId();
    taskService.complete(taskId);

    Object variable = runtimeService.getVariable(processInstanceId, "outLiteralVariable");
    assertThat(variable).isEqualTo("outLiteralValue");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessDataOutputOnError.bpmn",
      "org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithError.bpmn"
  })
  @Test
  void testSubProcessDataOutputOnError() {
    String variableName = "subVariable";
    Object variableValue = "Hello from Subprocess";

    runtimeService.startProcessInstanceByKey("Process_1");
    //first task is the one in the subprocess
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("SubTask");

    runtimeService.setVariable(task.getProcessInstanceId(), variableName, variableValue);
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Task after error");

    Object variable = runtimeService.getVariable(task.getProcessInstanceId(), variableName);
    assertThat(variable).isEqualTo(variableValue);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessDataOutputOnThrownError.bpmn",
      "org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithThrownError.bpmn"
  })
  @Test
  void testSubProcessDataOutputOnThrownError() {
    String variableName = "subVariable";
    Object variableValue = "Hello from Subprocess";

    runtimeService.startProcessInstanceByKey("Process_1");
    //first task is the one in the subprocess
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("SubTask");

    runtimeService.setVariable(task.getProcessInstanceId(), variableName, variableValue);
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Task after error");

    Object variable = runtimeService.getVariable(task.getProcessInstanceId(), variableName);
    assertThat(variable).isEqualTo(variableValue);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testTwoSubProcessesDataOutputOnError.bpmn",
      "org/operaton/bpm/engine/test/bpmn/callactivity/subProcessCallErrorSubProcess.bpmn",
      "org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithError.bpmn"
  })
  @Test
  void testTwoSubProcessesDataOutputOnError() {
    String variableName = "subVariable";
    Object variableValue = "Hello from Subprocess";

    runtimeService.startProcessInstanceByKey("Process_1");
    //first task is the one in the subprocess
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("SubTask");

    runtimeService.setVariable(task.getProcessInstanceId(), variableName, variableValue);
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Task after error");

    Object variable = runtimeService.getVariable(task.getProcessInstanceId(), variableName);
    //both processes have and out mapping for all, so we want the variable to be propagated to the process with the event handler
    assertThat(variable).isEqualTo(variableValue);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testTwoSubProcessesLimitedDataOutputOnError.bpmn",
      "org/operaton/bpm/engine/test/bpmn/callactivity/subProcessCallErrorSubProcessWithLimitedOutMapping.bpmn",
      "org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithError.bpmn"
  })
  @Test
  void testTwoSubProcessesLimitedDataOutputOnError() {
    String variableName1 = "subSubVariable1";
    String variableName2 = "subSubVariable2";
    String variableName3 = "subVariable";
    Object variableValue = "Hello from Subsubprocess";
    Object variableValue2 = "Hello from Subprocess";

    runtimeService.startProcessInstanceByKey("Process_1");

    //task in first subprocess (second process in general)
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Task");
    runtimeService.setVariable(task.getProcessInstanceId(), variableName3, variableValue2);
    taskService.complete(task.getId());
    //task in the second subprocess (third process in general)
    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("SubTask");
    runtimeService.setVariable(task.getProcessInstanceId(), variableName1, "foo");
    runtimeService.setVariable(task.getProcessInstanceId(), variableName2, variableValue);
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Task after error");

    //the two subprocess don't pass all their variables, so we check that not all were passed
    Object variable = runtimeService.getVariable(task.getProcessInstanceId(), variableName2);
    assertThat(variable).isEqualTo(variableValue);
    variable = runtimeService.getVariable(task.getProcessInstanceId(), variableName3);
    assertThat(variable).isEqualTo(variableValue2);
    variable = runtimeService.getVariable(task.getProcessInstanceId(), variableName1);
    assertThat(variable).isNull();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityAdvancedTest.testCallProcessByVersionAsExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCallCaseByVersionAsExpression() {
    // given

    String bpmnResourceName = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";

    String secondDeploymentId = repositoryService.createDeployment()
            .addClasspathResource(bpmnResourceName)
            .deploy()
            .getId();

    String thirdDeploymentId = repositoryService.createDeployment()
            .addClasspathResource(bpmnResourceName)
            .deploy()
            .getId();

    String processDefinitionIdInSecondDeployment = repositoryService
            .createProcessDefinitionQuery()
            .processDefinitionKey("oneTaskProcess")
            .deploymentId(secondDeploymentId)
            .singleResult()
            .getId();

    VariableMap variables = Variables.createVariables().putValue("myVersion", 2);

    // when
    runtimeService.startProcessInstanceByKey("process", variables).getId();

    // then
    ProcessInstance subInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").singleResult();
    assertThat(subInstance).isNotNull();

    assertThat(subInstance.getProcessDefinitionId()).isEqualTo(processDefinitionIdInSecondDeployment);

    repositoryService.deleteDeployment(secondDeploymentId, true);
    repositoryService.deleteDeployment(thirdDeploymentId, true);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivityAdvancedTest.testCallProcessByVersionAsDelegateExpression.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCallCaseByVersionAsDelegateExpression() {
    processEngineConfiguration.getBeans().put("myDelegate", new MyVersionDelegate());

    // given
    String bpmnResourceName = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";

    String secondDeploymentId = repositoryService.createDeployment()
            .addClasspathResource(bpmnResourceName)
            .deploy()
            .getId();

    String thirdDeploymentId = repositoryService.createDeployment()
            .addClasspathResource(bpmnResourceName)
            .deploy()
            .getId();

    String processDefinitionIdInSecondDeployment = repositoryService
            .createProcessDefinitionQuery()
            .processDefinitionKey("oneTaskProcess")
            .deploymentId(secondDeploymentId)
            .singleResult()
            .getId();

    VariableMap variables = Variables.createVariables().putValue("myVersion", 2);

    // when
    runtimeService.startProcessInstanceByKey("process", variables).getId();

    // then
    ProcessInstance subInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("oneTaskProcess").singleResult();
    assertThat(subInstance).isNotNull();

    assertThat(subInstance.getProcessDefinitionId()).isEqualTo(processDefinitionIdInSecondDeployment);

    repositoryService.deleteDeployment(secondDeploymentId, true);
    repositoryService.deleteDeployment(thirdDeploymentId, true);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithVersionTag.bpmn20.xml"})
  @Test
  void testCallProcessByVersionTag() {
    // given
    BpmnModelInstance modelInstance = getModelWithCallActivityVersionTagBinding("ver_tag_1");

   testRule.deploy(modelInstance);

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // then
    ProcessInstance subInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess").superProcessInstanceId(processInstance.getId()).singleResult();
    assertThat(subInstance).isNotNull();

    // clean up
    cleanupDeployments();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithVersionTag.bpmn20.xml"})
  @Test
  void testCallProcessByVersionTagAsExpression() {
    // given
    BpmnModelInstance modelInstance = getModelWithCallActivityVersionTagBinding("${versionTagExpr}");

   testRule.deploy(modelInstance);

    // when
    VariableMap variables = Variables.createVariables().putValue("versionTagExpr", "ver_tag_1");
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process", variables);

    // then
    ProcessInstance subInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess").superProcessInstanceId(processInstance.getId()).singleResult();
    assertThat(subInstance).isNotNull();

    // clean up
    cleanupDeployments();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithVersionTag.bpmn20.xml"})
  @Test
  void testCallProcessByVersionTagAsDelegateExpression() {
    // given
    processEngineConfiguration.getBeans().put("myDelegate", new MyVersionDelegate());
    BpmnModelInstance modelInstance = getModelWithCallActivityVersionTagBinding("${myDelegate.getVersionTag()}");

   testRule.deploy(modelInstance);

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");

    // then
    ProcessInstance subInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey("subProcess").superProcessInstanceId(processInstance.getId()).singleResult();
    assertThat(subInstance).isNotNull();

    // clean up
    cleanupDeployments();
  }

  @Test
  void testCallProcessWithoutVersionTag() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("process")
        .startEvent()
        .callActivity("callActivity")
        .calledElement("subProcess")
        .operatonCalledElementBinding("versionTag")
        .endEvent()
        .done();

    // when/then
    assertThatThrownBy(() -> testRule.deploy(modelInstance))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Could not parse BPMN process.")
        .hasMessageContaining("Missing attribute 'calledElementVersionTag' when 'calledElementBinding' has value 'versionTag'");
  }

  @Test
  void testCallProcessByVersionTagNoneSubprocess() {
    // given
    BpmnModelInstance modelInstance = getModelWithCallActivityVersionTagBinding("ver_tag_1");
   testRule.deploy(modelInstance);

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process"))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("no processes deployed with key = 'subProcess', versionTag = 'ver_tag_1' and tenant-id = 'null': processDefinition is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithVersionTag.bpmn20.xml"})
  @Test
  void testCallProcessByVersionTagTwoSubprocesses() {
    // given
    BpmnModelInstance modelInstance = getModelWithCallActivityVersionTagBinding("ver_tag_1");
   testRule.deploy(modelInstance);
   testRule.deploy("org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithVersionTag.bpmn20.xml");

    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("process"))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("There are '2' results for a process definition with key 'subProcess', versionTag 'ver_tag_1' and tenant-id '{}'.");

    // clean up
    cleanupDeployments();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/orderProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/checkCreditProcess.bpmn20.xml"
  })
  @Test
  void testOrderProcessWithCallActivity() {
    // After the process has started, the 'verify credit history' task should be active
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("orderProcess");
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task verifyCreditTask = taskQuery.singleResult();
    assertThat(verifyCreditTask.getName()).isEqualTo("Verify credit history");

    // Verify with Query API
    ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(pi.getId()).singleResult();
    assertThat(subProcessInstance).isNotNull();
    assertThat(runtimeService.createProcessInstanceQuery().subProcessInstanceId(subProcessInstance.getId()).singleResult().getId()).isEqualTo(pi.getId());

    // Completing the task with approval, will end the subprocess and continue the original process
    taskService.complete(verifyCreditTask.getId(), CollectionUtil.singletonMap("creditApproved", true));
    Task prepareAndShipTask = taskQuery.singleResult();
    assertThat(prepareAndShipTask.getName()).isEqualTo("Prepare and Ship");
  }

  /**
   * Test case for checking deletion of process instances in call activity subprocesses
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  @Test
  void testDeleteProcessInstanceInCallActivity() {
    // given
    runtimeService.startProcessInstanceByKey("callSimpleSubProcess");


    // one task in the subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();

    // Completing the task continues the process which leads to calling the subprocess
    taskService.complete(taskBeforeSubProcess.getId());
    Task taskInSubProcess = taskQuery.singleResult();


    List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList)
            .isNotNull()
            .hasSize(2);


    // when
    // Delete the ProcessInstance in the sub process
    runtimeService.deleteProcessInstance(taskInSubProcess.getProcessInstanceId(), "Test upstream deletion");

    // then

    // How many process Instances
    instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList).isEmpty();
  }

  /**
   * Test case for checking deletion of process instances in call activity subprocesses
   * <p>
   * Checks that deletion of process Instance will respect other process instances in the scope
   * and stop its upward deletion propagation will stop at this point
   * </p>
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testTwoSubProcesses.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"})
  @Test
  void testSingleDeletionWithTwoSubProcesses() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("callTwoSubProcesses");

    List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList)
            .isNotNull()
            .hasSize(3);

    List<Task> taskList = taskService.createTaskQuery().list();
    assertThat(taskList)
            .isNotNull()
            .hasSize(2);

    List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getProcessInstanceId());
    assertThat(activeActivityIds)
            .isNotNull()
            .hasSize(2);

    // when
    runtimeService.deleteProcessInstance(taskList.get(0).getProcessInstanceId(), "Test upstream deletion");

    // then
    // How many process Instances
    instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList)
            .isNotNull()
            .hasSize(2);

    // How man call activities
    activeActivityIds = runtimeService.getActiveActivityIds(processInstance.getProcessInstanceId());
    assertThat(activeActivityIds)
            .isNotNull()
            .hasSize(1);
  }

  /**
   * Test case for checking deletion of process instances in nested call activity subprocesses
   * <p>
   * Checking that nested call activities will propagate upward over multiple nested levels
   * </p>
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testNestedCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  @Test
  void testDeleteMultilevelProcessInstanceInCallActivity() {
    // given
    runtimeService.startProcessInstanceByKey("nestedCallActivity");

    // one task in the subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();

    // Completing the task continues the process which leads to calling the subprocess
    taskService.complete(taskBeforeSubProcess.getId());
    Task taskInSubProcess = taskQuery.singleResult();

    // Completing the task continues the sub process which leads to calling the deeper subprocess
    taskService.complete(taskInSubProcess.getId());
    Task taskInNestedSubProcess = taskQuery.singleResult();

    List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList)
            .isNotNull()
            .hasSize(3);

    // when
    // Delete the ProcessInstance in the sub process
    runtimeService.deleteProcessInstance(taskInNestedSubProcess.getProcessInstanceId(), "Test cascading upstream deletion");


    // then
    // How many process Instances
    instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList).isEmpty();
  }

  /**
   * Test case for checking deletion of process instances in nested call activity subprocesses
   * <p>
   * The test defines a process waiting on three nested call activities to complete
   * </p>
   * <p>
   * At each nested level there is only one process instance, which is waiting on the next level to complete
   * </p>
   * <p>
   * When we delete the process instance of the most inner call activity sub process the expected behaviour is that
   * the delete will propagate upward and delete all process instances.
   * </p>
   */
  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testCallSimpleSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testNestedCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testDoubleNestedCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/callactivity/simpleSubProcess.bpmn20.xml"
  })
  @Test
  void testDeleteDoubleNestedProcessInstanceInCallActivity() {
    // given
    runtimeService.startProcessInstanceByKey("doubleNestedCallActivity");

    // one task in the subprocess should be active after starting the process instance
    TaskQuery taskQuery = taskService.createTaskQuery();
    Task taskBeforeSubProcess = taskQuery.singleResult();

    // Completing the task continues the process which leads to calling the subprocess
    taskService.complete(taskBeforeSubProcess.getId());
    Task taskInSubProcess = taskQuery.singleResult();

    // Completing the task continues the sub process which leads to calling the deeper subprocess
    taskService.complete(taskInSubProcess.getId());
    Task taskInNestedSubProcess = taskQuery.singleResult();


    // Completing the task continues the sub process which leads to calling the deeper subprocess
    taskService.complete(taskInNestedSubProcess.getId());
    Task taskInDoubleNestedSubProcess = taskQuery.singleResult();


    List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList)
            .isNotNull()
            .hasSize(4);

    // when
    // Delete the ProcessInstance in the sub process
    runtimeService.deleteProcessInstance(taskInDoubleNestedSubProcess.getProcessInstanceId(), "Test cascading upstream deletion");


    // then
    // How many process Instances
    instanceList = runtimeService.createProcessInstanceQuery().list();
    assertThat(instanceList).isEmpty();

  }

  @Test
  void testTransientVariableInputMapping() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("simpleSubProcess")
      .startEvent()
      .serviceTask()
      .operatonClass(AssertTransientVariableDelegate.class)
      .userTask()
      .endEvent()
      .done();

   testRule.deploy("org/operaton/bpm/engine/test/bpmn/callactivity/CallActivity.testSubProcessAllDataInputOutputTypedApi.bpmn20.xml");
   testRule.deploy(modelInstance);

    VariableMap variables = Variables.createVariables().putValue("var", Variables.stringValue("value", true));

    // when
    runtimeService.startProcessInstanceByKey("subProcessDataInputOutput", variables);

    // then
    // presence of transient variable is asserted in delegate

    // and
    long numVariables = runtimeService.createVariableInstanceQuery().count();
    assertThat(numVariables).isZero();
  }


  @Test
  void testTransientVariableOutputMapping() {
    // given
    BpmnModelInstance modelInstance = Bpmn.createExecutableProcess("superProcess")
      .startEvent()
      .callActivity()
      .calledElement("oneTaskProcess")
      .operatonOut("var", "var")
      .serviceTask()
      .operatonClass(AssertTransientVariableDelegate.class)
      .userTask()
      .endEvent()
      .done();

   testRule.deploy(modelInstance);
   testRule.deploy("org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml");

    runtimeService.startProcessInstanceByKey("superProcess");
    Task task = taskService.createTaskQuery().singleResult();


    // when
    VariableMap variables = Variables.createVariables().putValue("var", Variables.stringValue("value", true));
    taskService.complete(task.getId(), variables);

    // then
    // presence of transient variable was asserted in delegate

    long numVariables = runtimeService.createVariableInstanceQuery().count();
    assertThat(numVariables).isZero();
  }


  public static class AssertTransientVariableDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      TypedValue typedValue = execution.getVariableTyped("var");
      assertThat(typedValue).isNotNull();
      assertThat(typedValue.getType()).isEqualTo(ValueType.STRING);
      assertThat(typedValue.isTransient()).isTrue();
      assertThat(typedValue.getValue()).isEqualTo("value");
    }

  }

  protected BpmnModelInstance getModelWithCallActivityVersionTagBinding(String versionTag) {
    return Bpmn.createExecutableProcess("process")
        .startEvent()
        .callActivity("callActivity")
        .calledElement("subProcess")
        .operatonCalledElementBinding("versionTag")
        .operatonCalledElementVersionTag(versionTag)
        .endEvent()
        .done();
  }

  protected void cleanupDeployments() {
    List<org.operaton.bpm.engine.repository.Deployment> deployments = repositoryService.createDeploymentQuery().list();
    for (org.operaton.bpm.engine.repository.Deployment currentDeployment : deployments) {
      repositoryService.deleteDeployment(currentDeployment.getId(), true);
    }
  }
}
