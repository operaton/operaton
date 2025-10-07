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
package org.operaton.bpm.engine.test.bpmn.event.end;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nico Rehwaldt
 */
public class TerminateEndEventTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;

  public static int serviceTaskInvokedCount;

  public static class CountDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      serviceTaskInvokedCount++;

      // leave only 3 out of n subprocesses
      execution.setVariableLocal("terminate", serviceTaskInvokedCount > 3);
    }
  }

  public static int serviceTaskInvokedCount2;

  public static class CountDelegate2 implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      serviceTaskInvokedCount2++;
    }
  }

  @Deployment
  @Test
  void testProcessTerminate() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
    assertThat(executionEntities).isEqualTo(3);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateTask").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testTerminateWithSubProcess() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // should terminate the process and
    long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
    assertThat(executionEntities).isEqualTo(4);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateWithCallActivity.bpmn",
      "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessNoTerminate.bpmn"
  })
  @Test
  void testTerminateWithCallActivity() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
    assertThat(executionEntities).isEqualTo(4);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testTerminateInSubProcess() {
    serviceTaskInvokedCount = 0;

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // should terminate the subprocess and continue the parent
    long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
    assertThat(executionEntities).isOne();

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  /**
   * CAM-4067
   */
  @Deployment
  @Test
  void testTerminateInSubProcessShouldNotInvokeProcessEndListeners() {
    RecorderExecutionListener.clear();

    // when process instance is started and terminate end event in subprocess executed
    runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // then the outer task still exists
    Task outerTask = taskService.createTaskQuery().singleResult();
    assertThat(outerTask).isNotNull();
    assertThat(outerTask.getTaskDefinitionKey()).isEqualTo("outerTask");

    // and the process end listener was not invoked
    assertThat(RecorderExecutionListener.getRecordedEvents()).isEmpty();

  }

  /**
   * CAM-4067
   */
  @Deployment
  @Test
  void testTerminateInSubProcessConcurrentShouldNotInvokeProcessEndListeners() {
    RecorderExecutionListener.clear();

    // when process instance is started and terminate end event in subprocess executed
    runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // then the outer task still exists
    Task outerTask = taskService.createTaskQuery().singleResult();
    assertThat(outerTask).isNotNull();
    assertThat(outerTask.getTaskDefinitionKey()).isEqualTo("outerTask");

    // and the process end listener was not invoked
    assertThat(RecorderExecutionListener.getRecordedEvents()).isEmpty();

  }

  /**
   * CAM-4067
   */
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInSubProcess.bpmn")
  @Test
  void testTerminateInSubProcessShouldNotEndProcessInstanceInHistory() {
    // when process instance is started and terminate end event in subprocess executed
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // then the historic process instance should not appear ended
    testRule.assertProcessNotEnded(pi.getId());

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().singleResult();

      assertThat(hpi).isNotNull();
      assertThat(hpi.getEndTime()).isNull();
      assertThat(hpi.getDurationInMillis()).isNull();
      assertThat(hpi.getDeleteReason()).isNull();
    }
  }

  @Deployment
  @Test
  void testTerminateInSubProcessConcurrent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    long executionEntities = runtimeService.createExecutionQuery().count();
    assertThat(executionEntities).isOne();

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  /**
   * CAM-4067
   */
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInSubProcessConcurrent.bpmn")
  @Test
  void testTerminateInSubProcessConcurrentShouldNotEndProcessInstanceInHistory() {
    // when process instance is started and terminate end event in subprocess executed
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // then the historic process instance should not appear ended
    testRule.assertProcessNotEnded(pi.getId());

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().singleResult();

      assertThat(hpi).isNotNull();
      assertThat(hpi.getEndTime()).isNull();
      assertThat(hpi.getDurationInMillis()).isNull();
      assertThat(hpi.getDeleteReason()).isNull();
    }
  }

  @Deployment
  @Test
  void testTerminateInSubProcessConcurrentMultiInstance() {
    serviceTaskInvokedCount = 0;

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    long executionEntities = runtimeService.createExecutionQuery().count();
    assertThat(executionEntities).isEqualTo(12);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    long executionEntities2 = runtimeService.createExecutionQuery().count();
    assertThat(executionEntities2).isEqualTo(10);

    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task t : tasks) {
      taskService.complete(t.getId());
    }

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  void testTerminateInSubProcessMultiInstance() {
    serviceTaskInvokedCount = 0;

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    long executionEntities = runtimeService.createExecutionQuery().count();
    assertThat(executionEntities).isOne();

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }


  @Deployment
  @Test
  void testTerminateInSubProcessSequentialConcurrentMultiInstance() {
    serviceTaskInvokedCount = 0;
    serviceTaskInvokedCount2 = 0;

    // Starting multi instance with 5 instances; terminating 2, finishing 3
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    long remainingExecutions = runtimeService.createExecutionQuery().count();

    // outer execution still available
    assertThat(remainingExecutions).isOne();

    // three finished
    assertThat(serviceTaskInvokedCount2).isEqualTo(3);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    // last task remaining
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivity.bpmn",
      "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessTerminate.bpmn"
  })
  @Test
  void testTerminateInCallActivity() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // should terminate the called process and continue the parent
    long executionEntities = runtimeService.createExecutionQuery().count();
    assertThat(executionEntities).isOne();

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityMulitInstance.bpmn",
      "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessTerminate.bpmn"
  })
  @Test
  void testTerminateInCallActivityMulitInstance() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // should terminate the called process and continue the parent
    long executionEntities = runtimeService.createExecutionQuery().count();
    assertThat(executionEntities).isOne();

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrent.bpmn",
      "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessConcurrentTerminate.bpmn"
  })
  @Test
  void testTerminateInCallActivityConcurrent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // should terminate the called process and continue the parent
    long executionEntities = runtimeService.createExecutionQuery().count();
    assertThat(executionEntities).isOne();

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrentMulitInstance.bpmn",
      "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessConcurrentTerminate.bpmn"
  })
  @Test
  void testTerminateInCallActivityConcurrentMulitInstance() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // should terminate the called process and continue the parent
    long executionEntities = runtimeService.createExecutionQuery().count();
    assertThat(executionEntities).isOne();

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }
}
