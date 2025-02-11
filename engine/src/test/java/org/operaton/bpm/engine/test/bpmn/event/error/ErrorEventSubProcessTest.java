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
package org.operaton.bpm.engine.test.bpmn.event.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.bpmn.event.error.ThrowErrorDelegate.throwError;
import static org.operaton.bpm.engine.test.bpmn.event.error.ThrowErrorDelegate.throwException;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.junit.Test;


/**
 * @author Falko Menge
 */
public class ErrorEventSubProcessTest extends PluggableProcessEngineTest {

  @Deployment
  // an event subprocesses takes precedence over a boundary event
  @Test
  public void testEventSubprocessTakesPrecedence() {
    String procId = runtimeService.startProcessInstanceByKey("CatchErrorInEmbeddedSubProcess").getId();
    assertThatErrorHasBeenCaught(procId);
  }

  @Deployment
  // an event subprocess with errorCode takes precedence over a catch-all handler
  @Test
  public void testErrorCodeTakesPrecedence() {
    String procId = runtimeService.startProcessInstanceByKey("CatchErrorInEmbeddedSubProcess").getId();

    // The process will throw an error event,
    // which is caught and escalated by a User Task
    assertThat(taskService.createTaskQuery()
        .taskDefinitionKey("taskAfterErrorCatch2") // <!>
        .count()).as("No tasks found in task list.").isEqualTo(1);
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Escalated Task");

    // Completing the Task will end the process instance
    taskService.complete(task.getId());
    testRule.assertProcessEnded(procId);

  }

  @Deployment
  @Test
  public void testCatchErrorInEmbeddedSubProcess() {
    String procId = runtimeService.startProcessInstanceByKey("CatchErrorInEmbeddedSubProcess").getId();
    assertThatErrorHasBeenCaught(procId);
  }

  @Deployment
  @Test
  public void testCatchErrorThrownByScriptTaskInEmbeddedSubProcess() {
    String procId = runtimeService.startProcessInstanceByKey("CatchErrorThrownByScriptTaskInEmbeddedSubProcess").getId();
    assertThatErrorHasBeenCaught(procId);
  }

  @Deployment
  @Test
  public void testCatchErrorThrownByScriptTaskInEmbeddedSubProcessWithErrorCode() {
    String procId = runtimeService.startProcessInstanceByKey("CatchErrorThrownByScriptTaskInEmbeddedSubProcessWithErrorCode").getId();
    assertThatErrorHasBeenCaught(procId);
  }

  @Deployment
  @Test
  public void testCatchErrorThrownByScriptTaskInTopLevelProcess() {
    String procId = runtimeService.startProcessInstanceByKey("CatchErrorThrownByScriptTaskInTopLevelProcess").getId();
    assertThatErrorHasBeenCaught(procId);
  }

  @Deployment
  @Test
  public void testCatchErrorThrownByScriptTaskInsideSubProcessInTopLevelProcess() {
    String procId = runtimeService.startProcessInstanceByKey("CatchErrorThrownByScriptTaskInsideSubProcessInTopLevelProcess").getId();
    assertThatErrorHasBeenCaught(procId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testThrowErrorInScriptTaskInsideCallActivitiCatchInTopLevelProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByJavaDelegateOnCallActivity-child.bpmn20.xml" })
  @Test
  public void testThrowErrorInScriptTaskInsideCallActivitiCatchInTopLevelProcess() {
    String procId = runtimeService.startProcessInstanceByKey("testThrowErrorInScriptTaskInsideCallActivitiCatchInTopLevelProcess").getId();
    assertThatErrorHasBeenCaught(procId);
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testCatchErrorThrownByAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  public void testCatchExceptionThrownByExecuteOfAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess", throwException()).getId();

    assertTrue((Boolean) runtimeService.getVariable(pi, "executed"));
    assertNull(runtimeService.getVariable(pi, "signaled"));

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertNotNull(userTask);
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testCatchErrorThrownByAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  public void testCatchErrorThrownByExecuteOfAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess", throwError()).getId();

    assertTrue((Boolean) runtimeService.getVariable(pi, "executed"));
    assertNull(runtimeService.getVariable(pi, "signaled"));

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertNotNull(userTask);
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testCatchErrorThrownByAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  public void testCatchExceptionThrownBySignalOfAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess").getId();

    assertTrue((Boolean) runtimeService.getVariable(pi, "executed"));
    assertNull(runtimeService.getVariable(pi, "signaled"));

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
    assertNotNull(serviceTask);

    runtimeService.setVariables(pi, throwException());
    runtimeService.signal(serviceTask.getId());

    assertTrue((Boolean) runtimeService.getVariable(pi, "executed"));
    assertTrue((Boolean) runtimeService.getVariable(pi, "signaled"));

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertNotNull(userTask);
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testCatchErrorThrownByAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  public void testCatchErrorThrownBySignalOfAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess").getId();

    assertTrue((Boolean) runtimeService.getVariable(pi, "executed"));
    assertNull(runtimeService.getVariable(pi, "signaled"));

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
    assertNotNull(serviceTask);

    runtimeService.setVariables(pi, throwError());
    runtimeService.signal(serviceTask.getId());

    assertTrue((Boolean) runtimeService.getVariable(pi, "executed"));
    assertTrue((Boolean) runtimeService.getVariable(pi, "signaled"));

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertNotNull(userTask);
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testCatchErrorThrownByDelegateExpression.bpmn20.xml"
  })
  @Test
  public void testCatchExceptionThrownByExecuteOfDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    variables.putAll(throwException());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertTrue((Boolean) runtimeService.getVariable(pi, "executed"));
    assertNull(runtimeService.getVariable(pi, "signaled"));

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertNotNull(userTask);
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testCatchErrorThrownByDelegateExpression.bpmn20.xml"
  })
  @Test
  public void testCatchErrorThrownByExecuteOfDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    variables.putAll(throwError());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertTrue((Boolean) runtimeService.getVariable(pi, "executed"));
    assertNull(runtimeService.getVariable(pi, "signaled"));

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertNotNull(userTask);
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testCatchErrorThrownByDelegateExpression.bpmn20.xml"
  })
  @Test
  public void testCatchExceptionThrownBySignalOfDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertTrue((Boolean) runtimeService.getVariable(pi, "executed"));
    assertNull(runtimeService.getVariable(pi, "signaled"));

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
    assertNotNull(serviceTask);

    runtimeService.setVariables(pi, throwException());
    runtimeService.signal(serviceTask.getId());

    assertTrue((Boolean) runtimeService.getVariable(pi, "executed"));
    assertTrue((Boolean) runtimeService.getVariable(pi, "signaled"));

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertNotNull(userTask);
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
    "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testCatchErrorThrownByDelegateExpression.bpmn20.xml"
  })
  @Test
  public void testCatchErrorThrownBySignalOfDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertTrue((Boolean) runtimeService.getVariable(pi, "executed"));
    assertNull(runtimeService.getVariable(pi, "signaled"));

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
    assertNotNull(serviceTask);

    runtimeService.setVariables(pi, throwError());
    runtimeService.signal(serviceTask.getId());

    assertTrue((Boolean) runtimeService.getVariable(pi, "executed"));
    assertTrue((Boolean) runtimeService.getVariable(pi, "signaled"));

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertNotNull(userTask);
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  private void assertThatErrorHasBeenCaught(String procId) {
    // The process will throw an error event,
    // which is caught and escalated by a User Task
    assertThat(taskService.createTaskQuery().count()).as("No tasks found in task list.").isEqualTo(1);
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Escalated Task");

    // Completing the Task will end the process instance
    taskService.complete(task.getId());
    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  public void testCatchErrorEventSubprocessSetErrorVariables(){
    runtimeService.startProcessInstanceByKey("Process_1");
    //the name used in "operaton:errorCodeVariable" in the BPMN
    String variableName = "errorCode";
    VariableInstance errorVariable = runtimeService.createVariableInstanceQuery().variableName(variableName).singleResult();

    assertThat(errorVariable).isNotNull();
    //the code we gave the thrown error
    Object errorCode = "error";
    assertThat(errorVariable.getValue()).isEqualTo(errorCode);

  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/bpmn/event/error/ThrowErrorProcess.bpmn",
      "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testCatchErrorFromCallActivitySetsErrorVariables.bpmn"
  })
  @Test
  public void testCatchErrorFromCallActivitySetsErrorVariable(){
    runtimeService.startProcessInstanceByKey("Process_1");
    //the name used in "operaton:errorCodeVariable" in the BPMN
    String variableName = "errorCode";
    VariableInstance errorVariable = runtimeService.createVariableInstanceQuery().variableName(variableName).singleResult();

    assertThat(errorVariable).isNotNull();
    //the code we gave the thrown error
    Object errorCode = "error";
    assertThat(errorVariable.getValue()).isEqualTo(errorCode);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testCatchBpmnErrorFromJavaDelegateInsideCallActivitySetsErrorVariable.bpmn",
      "org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithThrownError.bpmn"
    })
  @Test
  public void testCatchBpmnErrorFromJavaDelegateInsideCallActivitySetsErrorVariable(){
    runtimeService.startProcessInstanceByKey("Process_1");
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    //the name used in "operaton:errorCodeVariable" in the BPMN
    String variableName = "errorCode";
    //the code we gave the thrown error
    Object errorCode = "errorCode";
    VariableInstance errorVariable = runtimeService.createVariableInstanceQuery().variableName(variableName).singleResult();
    assertThat(errorVariable.getValue()).isEqualTo(errorCode);

    errorVariable = runtimeService.createVariableInstanceQuery().variableName("errorMessageVariable").singleResult();
    assertThat(errorVariable.getValue()).isEqualTo("ouch!");
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testThrowErrorInLoop.bpmn20.xml"
    })
  @Test
  public void testShouldNotThrowErrorInLoop(){
    runtimeService.startProcessInstanceByKey("looping-error");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("WaitState");
    taskService.complete(task.getId());

    assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("ErrorHandlingUserTask");
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testThrowErrorInLoopWithCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/error/ThrowErrorToCallActivity.bpmn20.xml"
    })
  @Test
  public void testShouldNotThrowErrorInLoopWithCallActivity(){
    runtimeService.startProcessInstanceByKey("CallActivityErrorInLoop");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("ErrorLog");
    taskService.complete(task.getId());

    assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("ErrorHandlingUserTask");
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testThrowErrorInLoopWithMultipleSubProcess.bpmn20.xml",
    })
  @Test
  public void testShouldNotThrowErrorInLoopForMultipleSubProcess(){
    runtimeService.startProcessInstanceByKey("looping-error");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("LoggerTask");
    taskService.complete(task.getId());

    assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("ErrorHandlingTask");
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/bpmn/event/error/ErrorEventSubProcessTest.testThrowErrorInLoopFromCallActivityToEventSubProcess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/error/ThrowErrorToCallActivity.bpmn20.xml"
    })
  public void FAILING_testShouldNotThrowErrorInLoopFromCallActivityToEventSubProcess(){
    runtimeService.startProcessInstanceByKey("Process_1");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("userTask");
    taskService.complete(task.getId());

    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("ErrorLog");
    taskService.complete(task.getId());

    // TODO: Loop exists when error thrown from call activity to event sub process
    // as they both have different process definition - CAM-6212
    assertThat(taskService.createTaskQuery().singleResult().getName()).isEqualTo("BoundaryEventTask");
  }

  @Deployment
  @Test
  public void testThrownAnErrorInEventSubprocessInSubprocessDifferentTransaction() {
    runtimeService.startProcessInstanceByKey("eventSubProcess");

    Task taskBefore = taskService.createTaskQuery().singleResult();
    assertNotNull(taskBefore);
    assertThat(taskBefore.getName()).isEqualTo("inside subprocess");

    Job job = managementService.createJobQuery().singleResult();
    assertNotNull(job);

    //when job is executed task is created
    managementService.executeJob(job.getId());

    Task taskDuring = taskService.createTaskQuery().taskName("inside event sub").singleResult();
    assertNotNull(taskDuring);

    taskService.complete(taskDuring.getId());

    Task taskAfter = taskService.createTaskQuery().singleResult();
    assertNotNull(taskAfter);
    assertThat(taskAfter.getName()).isEqualTo("after catch");

    Job jobAfter = managementService.createJobQuery().singleResult();
    assertNull(jobAfter);
  }

  @Deployment
  @Test
  public void testThrownAnErrorInEventSubprocessInSubprocess() {
    runtimeService.startProcessInstanceByKey("eventSubProcess");

    Task taskBefore = taskService.createTaskQuery().singleResult();
    assertNotNull(taskBefore);
    assertThat(taskBefore.getName()).isEqualTo("inside subprocess");

    Job job = managementService.createJobQuery().singleResult();
    assertNotNull(job);

    //when job is executed task is created
    managementService.executeJob(job.getId());

    Task taskAfter = taskService.createTaskQuery().singleResult();
    assertNotNull(taskAfter);
    assertThat(taskAfter.getName()).isEqualTo("after catch");

    Job jobAfter = managementService.createJobQuery().singleResult();
    assertNull(jobAfter);
  }
}
