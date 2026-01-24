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
package org.operaton.bpm.engine.test.bpmn.event.error;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ParseException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.commons.utils.CollectionUtil;

import static org.operaton.bpm.engine.test.bpmn.event.error.ThrowErrorDelegate.throwError;
import static org.operaton.bpm.engine.test.bpmn.event.error.ThrowErrorDelegate.throwException;
import static org.assertj.core.api.Assertions.*;


/**
 * @author Joram Barrez
 * @author Falko Menge
 */
class BoundaryErrorEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  IdentityService identityService;
  RepositoryService repositoryService;
  TaskService taskService;
  HistoryService historyService;

  @BeforeEach
  void setUp() {
    // Normally the UI will do this automatically for us
    identityService.setAuthenticatedUserId("kermit");
  }

  @AfterEach
  void tearDown() {
    identityService.clearAuthentication();
  }

  @Deployment
  @Test
  void testCatchErrorOnEmbeddedSubprocess() {
    runtimeService.startProcessInstanceByKey("boundaryErrorOnEmbeddedSubprocess");

    // After process start, usertask in subprocess should exist
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("subprocessTask");

    // After task completion, error end event is reached and caught
    taskService.complete(task.getId());
    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("task after catching the error");
  }

  @Test
  void testThrowErrorWithoutErrorCode() {
    // given
    var deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testThrowErrorWithoutErrorCode.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
        .isInstanceOf(ParseException.class)
        .hasMessageContaining("'errorCode' is mandatory on errors referenced by throwing error event definitions")
        .satisfies(e -> {
          ParseException parseException = (ParseException) e;
          assertThat(parseException.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("theEnd");
        });
  }

  @Test
  void testThrowErrorWithEmptyErrorCode() {
    // given
    var deploymentBuilder = repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testThrowErrorWithEmptyErrorCode.bpmn20.xml");

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
        .isInstanceOf(ParseException.class)
        .hasMessageContaining("'errorCode' is mandatory on errors referenced by throwing error event definitions")
        .satisfies(e -> {
          ParseException parseException = (ParseException) e;
          assertThat(parseException.getResourceReports().get(0).getErrors().get(0).getMainElementId()).isEqualTo("theEnd");
        });
  }

  @Deployment
  @Test
  void testCatchErrorOnEmbeddedSubprocessWithEmptyErrorCode() {
    testCatchErrorOnEmbeddedSubprocess();
  }

  @Deployment
  @Test
  void testCatchErrorOnEmbeddedSubprocessWithoutErrorCode() {
    testCatchErrorOnEmbeddedSubprocess();
  }

  @Deployment
  @Test
  void testCatchErrorOfInnerSubprocessOnOuterSubprocess() {
    runtimeService.startProcessInstanceByKey("boundaryErrorTest");

    List<Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getName()).isEqualTo("Inner subprocess task 1");
    assertThat(tasks.get(1).getName()).isEqualTo("Inner subprocess task 2");

    // Completing task 2, will cause the end error event to throw error with code 123
    taskService.complete(tasks.get(1).getId());
    Task taskAfterError = taskService.createTaskQuery().singleResult();
    assertThat(taskAfterError.getName()).isEqualTo("task outside subprocess");
  }

  @Deployment
  @Test
  void testCatchErrorInConcurrentEmbeddedSubprocesses() {
    assertErrorCaughtInConcurrentEmbeddedSubprocesses("boundaryEventTestConcurrentSubprocesses");
  }

  @Deployment
  @Test
  void testCatchErrorInConcurrentEmbeddedSubprocessesThrownByScriptTask() {
    assertErrorCaughtInConcurrentEmbeddedSubprocesses("catchErrorInConcurrentEmbeddedSubprocessesThrownByScriptTask");
  }

  private void assertErrorCaughtInConcurrentEmbeddedSubprocesses(String processDefinitionKey) {
    // Completing task A will lead to task D
    String procId = runtimeService.startProcessInstanceByKey(processDefinitionKey).getId();
    List<Task> tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getName()).isEqualTo("task A");
    assertThat(tasks.get(1).getName()).isEqualTo("task B");
    taskService.complete(tasks.get(0).getId());
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("task D");
    taskService.complete(task.getId());
    testRule.assertProcessEnded(procId);

    // Completing task B will lead to task C
    procId = runtimeService.startProcessInstanceByKey(processDefinitionKey).getId();
    assertThat(procId).isNotNull();
    tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getName()).isEqualTo("task A");
    assertThat(tasks.get(1).getName()).isEqualTo("task B");
    taskService.complete(tasks.get(1).getId());

    tasks = taskService.createTaskQuery().orderByTaskName().asc().list();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getName()).isEqualTo("task A");
    assertThat(tasks.get(1).getName()).isEqualTo("task C");
    taskService.complete(tasks.get(1).getId());
    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("task A");

    taskService.complete(task.getId());
    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("task D");
  }

  @Deployment
  @Test
  void testDeeplyNestedErrorThrown() {

    // Input = 1 -> error1 will be thrown, which will destroy ALL BUT ONE
    // subprocess, which leads to an end event, which ultimately leads to ending the process instance
    String procId = runtimeService.startProcessInstanceByKey("deeplyNestedErrorThrown").getId();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Nested task");
    taskService.complete(task.getId(), CollectionUtil.singletonMap("input", 1));
    testRule.assertProcessEnded(procId);

    // Input == 2 -> error2 will be thrown, leading to a userTask outside all subprocesses
    procId = runtimeService.startProcessInstanceByKey("deeplyNestedErrorThrown").getId();
    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Nested task");
    taskService.complete(task.getId(), CollectionUtil.singletonMap("input", 2));
    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("task after catch");
    taskService.complete(task.getId());
    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  @SuppressWarnings("deprecation")
  void testDeeplyNestedErrorThrownOnlyAutomaticSteps() {
    // input == 1 -> error2 is thrown -> caught on subprocess2 -> end event in subprocess -> proc inst end 1
    String procId = runtimeService.startProcessInstanceByKey("deeplyNestedErrorThrown",
            CollectionUtil.singletonMap("input", 1)).getId();
    testRule.assertProcessEnded(procId);

    HistoricProcessInstance hip;
    int historyLevel = processEngineConfiguration.getHistoryLevel().getId();
    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      hip = historyService.createHistoricProcessInstanceQuery().processInstanceId(procId).singleResult();
      assertThat(hip.getEndActivityId()).isEqualTo("processEnd1");
    }
    // input == 2 -> error2 is thrown -> caught on subprocess1 -> proc inst end 2
    procId = runtimeService.startProcessInstanceByKey("deeplyNestedErrorThrown",
            CollectionUtil.singletonMap("input", 1)).getId();
    testRule.assertProcessEnded(procId);

    if (historyLevel> ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      hip = historyService.createHistoricProcessInstanceQuery().processInstanceId(procId).singleResult();
      assertThat(hip.getEndActivityId()).isEqualTo("processEnd1");
    }
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorOnCallActivity-parent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml"
  })
  @Test
  void testCatchErrorOnCallActivity() {
    String procId = runtimeService.startProcessInstanceByKey("catchErrorOnCallActivity").getId();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Task in subprocess");

    // Completing the task will reach the end error event,
    // which is caught on the call activity boundary
    taskService.complete(task.getId());
    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Escalated Task");

    // Completing the task will end the process instance
    taskService.complete(task.getId());
    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorOnCallActivity-parent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml"
  })
  @SuppressWarnings("deprecation")
  @Test
  @Disabled("Emd activity id is not set")
  void testCatchErrorOnCallActivityShouldEndCalledProcessProperly() {
    // given a process instance that has instantiated (called) a sub process instance
    runtimeService.startProcessInstanceByKey("catchErrorOnCallActivity").getId();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Task in subprocess");

    // when an error end event is triggered in the sub process instance and catched in the super process instance
    taskService.complete(task.getId());
    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Escalated Task");

    // then the called historic process instance should have properly ended
    HistoricProcessInstance historicSubProcessInstance = historyService.createHistoricProcessInstanceQuery().processDefinitionKey("simpleSubProcess").singleResult();
    assertThat(historicSubProcessInstance).isNotNull();
    assertThat(historicSubProcessInstance.getDeleteReason()).isEqualTo("Cancel scope activity Activity(catchError) executed.");
    assertThat(historicSubProcessInstance.getEndActivityId()).isEqualTo("theEnd");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml"
  })
  @Test
  void testUncaughtError() {
    // given
    runtimeService.startProcessInstanceByKey("simpleSubProcess");
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Task in subprocess");

    // when/then
    // Completing the task will reach the end error event,
    // which is never caught in the process
    assertThatThrownBy(() -> taskService.complete(task.getId()))
        .isInstanceOf(BpmnError.class)
        .hasMessageContaining("No catching boundary event found for error with errorCode 'myError', neither in same process nor in parent process");
  }


  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testUncaughtErrorOnCallActivity-parent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml"
  })
  @Test
  void testUncaughtErrorOnCallActivity() {
    // given
    runtimeService.startProcessInstanceByKey("uncaughtErrorOnCallActivity");
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Task in subprocess");

    // when/then
    // Completing the task will reach the end error event,
    // which is never caught in the process
    assertThatThrownBy(() -> taskService.complete(task.getId()))
        .isInstanceOf(BpmnError.class)
        .hasMessageContaining("No catching boundary event found for error with errorCode 'myError', neither in same process nor in parent process");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByCallActivityOnSubprocess.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownByCallActivityOnSubprocess() {
    String procId = runtimeService.startProcessInstanceByKey("catchErrorOnSubprocess").getId();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Task in subprocess");

    // Completing the task will reach the end error event,
    // which is caught on the call activity boundary
    taskService.complete(task.getId());
    task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Escalated Task");

    // Completing the task will end the process instance
    taskService.complete(task.getId());
    testRule.assertProcessEnded(procId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByCallActivityOnCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess2ndLevel.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.subprocess.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownByCallActivityOnCallActivity() {
      String procId = runtimeService.startProcessInstanceByKey("catchErrorOnCallActivity2ndLevel").getId();

      Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Task in subprocess");

      taskService.complete(task.getId());

      task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Escalated Task");

      // Completing the task will end the process instance
      taskService.complete(task.getId());
      testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testCatchErrorOnParallelMultiInstance() {
    String procId = runtimeService.startProcessInstanceByKey("catchErrorOnParallelMi").getId();
    List<Task> tasks = taskService.createTaskQuery().list();
    assertThat(tasks).hasSize(5);

    // Complete two subprocesses, just to make it a bit more complex
    Map<String, Object> vars = new HashMap<>();
    vars.put("throwError", false);
    taskService.complete(tasks.get(2).getId(), vars);
    taskService.complete(tasks.get(3).getId(), vars);

    // Reach the error event
    vars.put("throwError", true);
    taskService.complete(tasks.get(1).getId(), vars);

    assertThat(taskService.createTaskQuery().count()).isZero();
    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testCatchErrorOnSequentialMultiInstance() {
    String procId = runtimeService.startProcessInstanceByKey("catchErrorOnSequentialMi").getId();

    // complete one task
    Map<String, Object> vars = new HashMap<>();
    vars.put("throwError", false);
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId(), vars);

    // complete second task and throw error
    vars.put("throwError", true);
    task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId(), vars);

    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testCatchErrorThrownBySignallableActivityBehaviour() {
    String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownBySignallableActivityBehaviour").getId();
    assertThat(procId).as("Didn't get a process id from runtime service").isNotNull();
    ActivityInstance processActivityInstance = runtimeService.getActivityInstance(procId);
    ActivityInstance serviceTask = processActivityInstance.getChildActivityInstances()[0];
    assertThat(serviceTask.getActivityId()).as("Expected the service task to be active after starting the process").isEqualTo("serviceTask");
    runtimeService.signal(serviceTask.getExecutionIds()[0]);
    assertThatErrorHasBeenCaught(procId);
  }

  @Deployment
  @Test
  void testCatchErrorThrownByJavaDelegateOnServiceTask() {
    String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnServiceTask").getId();
    assertThatErrorHasBeenCaught(procId);

    HashMap<String, Object> variables = new HashMap<>();
    variables.put("exceptionType", true);
    procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnServiceTask", variables).getId();
    assertThatExceptionHasBeenCaught(procId);
  }

  @Deployment
  @Test
  void testCatchErrorThrownByJavaDelegateOnServiceTaskNotCancelActivity() {
    String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnServiceTaskNotCancelActiviti").getId();
    assertThatErrorHasBeenCaught(procId);
  }

  @Deployment
  @Test
  void testCatchErrorThrownByJavaDelegateOnServiceTaskWithErrorCode() {
    String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnServiceTaskWithErrorCode").getId();
    assertThatErrorHasBeenCaught(procId);
  }

  @Deployment
  @Test
  void testCatchErrorThrownByJavaDelegateOnEmbeddedSubProcess() {
    String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnEmbeddedSubProcess").getId();
    assertThatErrorHasBeenCaught(procId);

    Map<String, Object> variables = new HashMap<>();
    variables.put("exceptionType", true);
    procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnEmbeddedSubProcess", variables).getId();
    assertThatExceptionHasBeenCaught(procId);
  }

  @Deployment
  @Test
  void testCatchErrorThrownByJavaDelegateOnEmbeddedSubProcessInduction() {
    String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnEmbeddedSubProcessInduction").getId();
    assertThatErrorHasBeenCaught(procId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByJavaDelegateOnCallActivity-parent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByJavaDelegateOnCallActivity-child.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownByJavaDelegateOnCallActivity() {
    String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnCallActivity-parent").getId();
    assertThatErrorHasBeenCaught(procId);

    Map<String, Object> variables = new HashMap<>();
    variables.put("exceptionType", true);
    procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnCallActivity-parent", variables).getId();
    assertThatExceptionHasBeenCaught(procId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByJavaDelegateOnCallActivity-child.bpmn20.xml"
  })
  @Test
  void testUncaughtErrorThrownByJavaDelegateOnServiceTask() {
    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnCallActivity-child"))
        .isInstanceOf(BpmnError.class)
        .hasMessageContaining("No catching boundary event found for error with errorCode '23', neither in same process nor in parent process");
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  void testCatchExceptionThrownByExecuteOfAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess", throwException()).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownByExecuteOfAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess", throwError()).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  void testCatchExceptionThrownBySignalMethodOfAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess").getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
    assertThat(serviceTask).isNotNull();

    runtimeService.setVariables(pi, throwException());
    runtimeService.signal(serviceTask.getId());

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat((Boolean) runtimeService.getVariable(pi, "signaled")).isTrue();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment
  @Test
  void testCatchExceptionExpressionThrownByFollowUpTask() {
    Map<String, Object> vars = throwException();
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess", vars)).isInstanceOf(ProcessEngineException.class);

    assertThat(taskService.createTaskQuery().singleResult()).isNull();
  }

  @Deployment
  @Test
  void testCatchExceptionClassDelegateThrownByFollowUpTask() {
    Map<String, Object> vars = throwException();
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess", vars)).isInstanceOf(ProcessEngineException.class);

    assertThat(taskService.createTaskQuery().singleResult()).isNull();
  }

  @Deployment
  @Test
  void testCatchExceptionExpressionThrownByFollowUpScopeTask() {
    Map<String, Object> vars = throwException();
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testProcess", vars)).isInstanceOf(ProcessEngineException.class);
    assertThat(taskService.createTaskQuery().singleResult()).isNull();
  }


  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByAbstractBpmnActivityBehavior.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownBySignalOfAbstractBpmnActivityBehavior() {
    String pi = runtimeService.startProcessInstanceByKey("testProcess").getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
    assertThat(serviceTask).isNotNull();

    runtimeService.setVariables(pi, throwError());
    runtimeService.signal(serviceTask.getId());

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat((Boolean) runtimeService.getVariable(pi, "signaled")).isTrue();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByDelegateExpression.bpmn20.xml"
  })
  @Test
  void testCatchExceptionThrownByExecuteOfDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    variables.putAll(throwException());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByDelegateExpression.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownByExecuteOfDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    variables.putAll(throwError());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByDelegateExpression.bpmn20.xml"
  })
  @Test
  void testCatchExceptionThrownBySignalMethodOfDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
    assertThat(serviceTask).isNotNull();

    runtimeService.setVariables(pi, throwException());
    runtimeService.signal(serviceTask.getId());

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat((Boolean) runtimeService.getVariable(pi, "signaled")).isTrue();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskException");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByDelegateExpression.bpmn20.xml"
  })
  @Test
  void testCatchErrorThrownBySignalOfDelegateExpression() {
    VariableMap variables = Variables.createVariables().putValue("myDelegate", new ThrowErrorDelegate());
    String pi = runtimeService.startProcessInstanceByKey("testProcess", variables).getId();

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat(runtimeService.getVariable(pi, "signaled")).isNull();

    Execution serviceTask = runtimeService.createExecutionQuery().processInstanceId(pi).activityId("serviceTask").singleResult();
    assertThat(serviceTask).isNotNull();

    runtimeService.setVariables(pi, throwError());
    runtimeService.signal(serviceTask.getId());

    assertThat((Boolean) runtimeService.getVariable(pi, "executed")).isTrue();
    assertThat((Boolean) runtimeService.getVariable(pi, "signaled")).isTrue();

    Task userTask = taskService.createTaskQuery().processInstanceId(pi).singleResult();
    assertThat(userTask).isNotNull();
    assertThat(userTask.getTaskDefinitionKey()).isEqualTo("userTaskError");

    taskService.complete(userTask.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testUncaughtErrorThrownByJavaDelegateOnCallActivity-parent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByJavaDelegateOnCallActivity-child.bpmn20.xml"
  })
  @Test
  void testUncaughtErrorThrownByJavaDelegateOnCallActivity() {
    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("uncaughtErrorThrownByJavaDelegateOnCallActivity-parent"))
        .isInstanceOf(BpmnError.class)
        .hasMessageContaining("No catching boundary event found for error with errorCode '23', neither in same process nor in parent process");
  }

  @Deployment
  @Test
  void testCatchErrorThrownByJavaDelegateOnMultiInstanceServiceTaskSequential() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("executionsBeforeError", 2);
    String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnMultiInstanceServiceTaskSequential", variables).getId();
    assertThatErrorHasBeenCaught(procId);

    variables.put("executionsBeforeError", 2);
    variables.put("exceptionType", true);
    procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnMultiInstanceServiceTaskSequential", variables).getId();
    assertThatExceptionHasBeenCaught(procId);
  }

  @Deployment
  @Test
  void testCatchErrorThrownByJavaDelegateOnMultiInstanceServiceTaskParallel() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("executionsBeforeError", 2);
    String procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnMultiInstanceServiceTaskParallel", variables).getId();
    assertThatErrorHasBeenCaught(procId);

    variables.put("executionsBeforeError", 2);
    variables.put("exceptionType", true);
    procId = runtimeService.startProcessInstanceByKey("catchErrorThrownByJavaDelegateOnMultiInstanceServiceTaskParallel", variables).getId();
    assertThatExceptionHasBeenCaught(procId);
  }

  @Deployment
  @Test
  void testErrorThrownByJavaDelegateNotCaughtByOtherEventType() {
    String procId = runtimeService.startProcessInstanceByKey("testErrorThrownByJavaDelegateNotCaughtByOtherEventType").getId();
    assertThatErrorHasBeenCaught(procId);
  }

  private void assertThatErrorHasBeenCaught(String procId) {
    // The service task will throw an error event,
    // which is caught on the service task boundary
    assertThat(taskService.createTaskQuery().count()).as("No tasks found in task list.").isOne();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Escalated Task");

    // Completing the task will end the process instance
    taskService.complete(task.getId());
    testRule.assertProcessEnded(procId);
  }

  private void assertThatExceptionHasBeenCaught(String procId) {
    // The service task will throw an error event,
    // which is caught on the service task boundary
    assertThat(taskService.createTaskQuery().count()).as("No tasks found in task list.").isOne();
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getName()).isEqualTo("Escalated Exception Task");

    // Completing the task will end the process instance
    taskService.complete(task.getId());
    testRule.assertProcessEnded(procId);
  }

  @Deployment
  @Test
  void testConcurrentExecutionsInterruptedOnDestroyScope() {

    // this test makes sure that if the first concurrent execution destroys the scope
    // (due to the interrupting boundary catch), the second concurrent execution does not
    // move forward.

    // if the test fails, it produces a constraint violation in db.

    assertThatCode(() -> runtimeService.startProcessInstanceByKey("process"))
        .doesNotThrowAnyException();
  }

  @Deployment
  @Test
  void testCatchErrorThrownByExpressionOnServiceTask() {
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("bpmnErrorBean", new BpmnErrorBean());
    String procId = runtimeService.startProcessInstanceByKey("testCatchErrorThrownByExpressionOnServiceTask", variables).getId();
    assertThatErrorHasBeenCaught(procId);
  }

  @Deployment
  @Test
  void testCatchErrorThrownByDelegateExpressionOnServiceTask() {
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("bpmnErrorBean", new BpmnErrorBean());
    String procId = runtimeService.startProcessInstanceByKey("testCatchErrorThrownByDelegateExpressionOnServiceTask", variables).getId();
    assertThatErrorHasBeenCaught(procId);

    variables.put("exceptionType", true);
    procId = runtimeService.startProcessInstanceByKey("testCatchErrorThrownByDelegateExpressionOnServiceTask", variables).getId();
    assertThatExceptionHasBeenCaught(procId);
  }

  @Deployment
  @Test
  void testCatchErrorThrownByJavaDelegateProvidedByDelegateExpressionOnServiceTask() {
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("bpmnErrorBean", new BpmnErrorBean());
    String procId = runtimeService.startProcessInstanceByKey("testCatchErrorThrownByJavaDelegateProvidedByDelegateExpressionOnServiceTask", variables).getId();
    assertThatErrorHasBeenCaught(procId);
  }

  @Deployment
  @Test
  void testCatchExceptionThrownByExpressionOnServiceTask() {
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("bpmnErrorBean", new BpmnErrorBean());
    String procId = runtimeService.startProcessInstanceByKey("testCatchExceptionThrownByExpressionOnServiceTask", variables).getId();
    assertThatExceptionHasBeenCaught(procId);
  }

  @Deployment
  @Test
  void testCatchExceptionThrownByScriptTask() {
    HashMap<String, Object> variables = new HashMap<>();
    String procId = runtimeService.startProcessInstanceByKey("testCatchExceptionThrownByScriptTask", variables).getId();
    assertThatExceptionHasBeenCaught(procId);
  }

  @Deployment
  @Test
  void testCatchSpecializedExceptionThrownByDelegate() {
    HashMap<String, Object> variables = new HashMap<>();
    variables.put("bpmnErrorBean", new BpmnErrorBean());
    String procId = runtimeService.startProcessInstanceByKey("testCatchSpecializedExceptionThrownByDelegate", variables).getId();
    assertThatExceptionHasBeenCaught(procId);
  }

  @Deployment
  @Test
  void testUncaughtRuntimeException() {
    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testUncaughtRuntimeException"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("This should not be caught!");
  }

  @Deployment
  @Test
  void testUncaughtBusinessExceptionWrongErrorCode() {
    // when/then
    assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("testUncaughtBusinessExceptionWrongErrorCode"))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("couldn't execute activity <serviceTask id=\"serviceTask\" ...>: Business Exception");
  }

  @Deployment
  @Test
  void testCatchErrorOnSubprocessThrownByNonInterruptingEventSubprocess() {
    runtimeService.startProcessInstanceByKey("testProcess");
    EventSubscription messageSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
    runtimeService.messageEventReceived("message", messageSubscription.getExecutionId());

    // should successfully have reached the task following the boundary event
    Execution taskExecution = runtimeService.createExecutionQuery().activityId("afterBoundaryTask").singleResult();
    assertThat(taskExecution).isNotNull();
    Task task = taskService.createTaskQuery().executionId(taskExecution.getId()).singleResult();
    assertThat(task).isNotNull();
  }

  @Deployment
  @Test
  void testCatchErrorOnSubprocessThrownByInterruptingEventSubprocess() {
    runtimeService.startProcessInstanceByKey("testProcess");
    EventSubscription messageSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
    runtimeService.messageEventReceived("message", messageSubscription.getExecutionId());

    // should successfully have reached the task following the boundary event
    Execution taskExecution = runtimeService.createExecutionQuery().activityId("afterBoundaryTask").singleResult();
    assertThat(taskExecution).isNotNull();
    Task task = taskService.createTaskQuery().executionId(taskExecution.getId()).singleResult();
    assertThat(task).isNotNull();
  }

  @Deployment
  @Test
  void testCatchErrorOnSubprocessThrownByNestedEventSubprocess() {
    runtimeService.startProcessInstanceByKey("testProcess");

    // trigger outer event subprocess
    EventSubscription messageSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
    runtimeService.messageEventReceived("outerMessage", messageSubscription.getExecutionId());

    // trigger inner event subprocess
    messageSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
    runtimeService.messageEventReceived("innerMessage", messageSubscription.getExecutionId());

    // should successfully have reached the task following the boundary event
    Execution taskExecution = runtimeService.createExecutionQuery().activityId("afterBoundaryTask").singleResult();
    assertThat(taskExecution).isNotNull();
    Task task = taskService.createTaskQuery().executionId(taskExecution.getId()).singleResult();
    assertThat(task).isNotNull();
  }

  @Deployment
  @Test
  void testCatchErrorOnSubprocessSetsErrorVariables(){
    runtimeService.startProcessInstanceByKey("Process_1");
    //the name used in "operaton:errorCodeVariable" in the BPMN
    String variableName = "errorVariable";
    Object errorCode = "error1";

    checkErrorVariable(variableName, errorCode);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/ThrowErrorProcess.bpmn",
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByCallActivityOnSubprocessSetsErrorCodeVariable.bpmn"
  })
  @Test
  void testCatchErrorThrownByCallActivityOnSubprocessSetsErrorVariables(){
    runtimeService.startProcessInstanceByKey("Process_1");
    //the name used in "operaton:errorCodeVariable" in the BPMN
    String variableName = "errorVariable";
    //the code we gave the thrown error
    Object errorCode = "error";

    checkErrorVariable(variableName, errorCode);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchErrorThrownByMultiInstanceSubProcessSetsErrorCodeVariable.bpmn"
  })
  @Test
  void testCatchErrorThrownByMultiInstanceSubProcessSetsErrorVariables(){
    runtimeService.startProcessInstanceByKey("Process_1");
    //the name used in "operaton:errorCodeVariable" in the BPMN
    String variableName = "errorVariable";
    //the code we gave the thrown error
    Object errorCode = "error";

    checkErrorVariable(variableName, errorCode);
  }

  private void checkErrorVariable(String variableName, Object expectedValue){
    VariableInstance errorVariable = runtimeService.createVariableInstanceQuery().variableName(variableName).singleResult();
    assertThat(errorVariable).isNotNull();
    assertThat(errorVariable.getValue()).isEqualTo(expectedValue);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/error/BoundaryErrorEventTest.testCatchBpmnErrorThrownByJavaDelegateInCallActivityOnSubprocessSetsErrorVariables.bpmn",
      "org/operaton/bpm/engine/test/bpmn/callactivity/subProcessWithThrownError.bpmn"
  })
  @Test
  void testCatchBpmnErrorThrownByJavaDelegateInCallActivityOnSubprocessSetsErrorVariables(){
    runtimeService.startProcessInstanceByKey("Process_1");
    Task task = taskService.createTaskQuery().singleResult();
    taskService.complete(task.getId());
    //the name used in "operaton:errorCodeVariable" in the BPMN
    String variableName = "errorCode";
    //the code we gave the thrown error
    Object errorCode = "errorCode";
    checkErrorVariable(variableName, errorCode);
    checkErrorVariable("errorMessageVariable", "ouch!");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/event/error/reviewSalesLead.bpmn20.xml"})
  @Test
  void testReviewSalesLeadProcess() {

    // After starting the process, a task should be assigned to the 'initiator' (normally set by GUI)
    Map<String, Object> variables = new HashMap<>();
    variables.put("details", "very interesting");
    variables.put("customerName", "Alfresco");
    String procId = runtimeService.startProcessInstanceByKey("reviewSaledLead", variables).getId();
    Task task = taskService.createTaskQuery().taskAssignee("kermit").singleResult();
    assertThat(task.getName()).isEqualTo("Provide new sales lead");

    // After completing the task, the review subprocess will be active
    taskService.complete(task.getId());
    Task ratingTask = taskService.createTaskQuery().taskCandidateGroup("accountancy").singleResult();
    assertThat(ratingTask.getName()).isEqualTo("Review customer rating");
    Task profitabilityTask = taskService.createTaskQuery().taskCandidateGroup("management").singleResult();
    assertThat(profitabilityTask.getName()).isEqualTo("Review profitability");

    // Complete the management task by stating that not enough info was provided
    // This should throw the error event, which closes the subprocess
    variables = new HashMap<>();
    variables.put("notEnoughInformation", true);
    taskService.complete(profitabilityTask.getId(), variables);

    // The 'provide additional details' task should now be active
    Task provideDetailsTask = taskService.createTaskQuery().taskAssignee("kermit").singleResult();
    assertThat(provideDetailsTask.getName()).isEqualTo("Provide additional details");

    // Providing more details (i.e. completing the task), will activate the subprocess again
    taskService.complete(provideDetailsTask.getId());
    List<Task> reviewTasks = taskService.createTaskQuery().orderByTaskName().asc().list();
    assertThat(reviewTasks.get(0).getName()).isEqualTo("Review customer rating");
    assertThat(reviewTasks.get(1).getName()).isEqualTo("Review profitability");

    // Completing both tasks normally ends the process
    taskService.complete(reviewTasks.get(0).getId());
    variables.put("notEnoughInformation", false);
    taskService.complete(reviewTasks.get(1).getId(), variables);
    testRule.assertProcessEnded(procId);
  }
}
