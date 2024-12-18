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
package org.operaton.bpm.engine.test.bpmn.event.end;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.bpmn.executionlistener.RecorderExecutionListener;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 * @author Nico Rehwaldt
 */
public class TerminateEndEventTest extends PluggableProcessEngineTest {

  public static int serviceTaskInvokedCount = 0;

  public static class CountDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      serviceTaskInvokedCount++;

      // leave only 3 out of n subprocesses
      execution.setVariableLocal("terminate", serviceTaskInvokedCount > 3);
    }
  }

  public static int serviceTaskInvokedCount2 = 0;

  public static class CountDelegate2 implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      serviceTaskInvokedCount2++;
    }
  }

  @Deployment
  @Test
  public void testProcessTerminate() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
    assertEquals(3, executionEntities);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateTask").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testTerminateWithSubProcess() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // should terminate the process and
    long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
    assertEquals(4, executionEntities);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateWithCallActivity.bpmn",
    "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessNoTerminate.bpmn"
  })
  @Test
  public void testTerminateWithCallActivity() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
    assertEquals(4, executionEntities);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preTerminateEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testTerminateInSubProcess() {
    serviceTaskInvokedCount = 0;

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // should terminate the subprocess and continue the parent
    long executionEntities = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).count();
    assertEquals(1, executionEntities);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  /**
   * CAM-4067
   */
  @Deployment
  @Test
  public void testTerminateInSubProcessShouldNotInvokeProcessEndListeners() {
    RecorderExecutionListener.clear();

    // when process instance is started and terminate end event in subprocess executed
    runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // then the outer task still exists
    Task outerTask = taskService.createTaskQuery().singleResult();
    assertNotNull(outerTask);
    assertEquals("outerTask", outerTask.getTaskDefinitionKey());

    // and the process end listener was not invoked
    assertTrue(RecorderExecutionListener.getRecordedEvents().isEmpty());

  }

  /**
   * CAM-4067
   */
  @Deployment
  @Test
  public void testTerminateInSubProcessConcurrentShouldNotInvokeProcessEndListeners() {
    RecorderExecutionListener.clear();

    // when process instance is started and terminate end event in subprocess executed
    runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // then the outer task still exists
    Task outerTask = taskService.createTaskQuery().singleResult();
    assertNotNull(outerTask);
    assertEquals("outerTask", outerTask.getTaskDefinitionKey());

    // and the process end listener was not invoked
    assertTrue(RecorderExecutionListener.getRecordedEvents().isEmpty());

  }

  /**
   * CAM-4067
   */
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInSubProcess.bpmn")
  @Test
  public void testTerminateInSubProcessShouldNotEndProcessInstanceInHistory() {
    // when process instance is started and terminate end event in subprocess executed
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // then the historic process instance should not appear ended
    testRule.assertProcessNotEnded(pi.getId());

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().singleResult();

      assertNotNull(hpi);
      assertNull(hpi.getEndTime());
      assertNull(hpi.getDurationInMillis());
      assertNull(hpi.getDeleteReason());
    }
  }

  @Deployment
  @Test
  public void testTerminateInSubProcessConcurrent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    long executionEntities = runtimeService.createExecutionQuery().count();
    assertEquals(1, executionEntities);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  /**
   * CAM-4067
   */
  @Deployment(resources = "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInSubProcessConcurrent.bpmn")
  @Test
  public void testTerminateInSubProcessConcurrentShouldNotEndProcessInstanceInHistory() {
    // when process instance is started and terminate end event in subprocess executed
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // then the historic process instance should not appear ended
    testRule.assertProcessNotEnded(pi.getId());

    if (processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_NONE) {
      HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery().singleResult();

      assertNotNull(hpi);
      assertNull(hpi.getEndTime());
      assertNull(hpi.getDurationInMillis());
      assertNull(hpi.getDeleteReason());
    }
  }

  @Deployment
  @Test
  public void testTerminateInSubProcessConcurrentMultiInstance() {
    serviceTaskInvokedCount = 0;

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    long executionEntities = runtimeService.createExecutionQuery().count();
    assertEquals(12, executionEntities);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    long executionEntities2 = runtimeService.createExecutionQuery().count();
    assertEquals(10, executionEntities2);

    List<Task> tasks = taskService.createTaskQuery().list();
    for (Task t : tasks) {
      taskService.complete(t.getId());
    }

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testTerminateInSubProcessMultiInstance() {
    serviceTaskInvokedCount = 0;

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    long executionEntities = runtimeService.createExecutionQuery().count();
    assertEquals(1, executionEntities);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }


  @Deployment
  @Test
  public void testTerminateInSubProcessSequentialConcurrentMultiInstance() {
    serviceTaskInvokedCount = 0;
    serviceTaskInvokedCount2 = 0;

    // Starting multi instance with 5 instances; terminating 2, finishing 3
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    long remainingExecutions = runtimeService.createExecutionQuery().count();

    // outer execution still available
    assertEquals(1, remainingExecutions);

    // three finished
    assertEquals(3, serviceTaskInvokedCount2);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    // last task remaining
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivity.bpmn",
    "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessTerminate.bpmn"
  })
  @Test
  public void testTerminateInCallActivity() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // should terminate the called process and continue the parent
    long executionEntities = runtimeService.createExecutionQuery().count();
    assertEquals(1, executionEntities);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityMulitInstance.bpmn",
    "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessTerminate.bpmn"
  })
  @Test
  public void testTerminateInCallActivityMulitInstance() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // should terminate the called process and continue the parent
    long executionEntities = runtimeService.createExecutionQuery().count();
    assertEquals(1, executionEntities);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrent.bpmn",
    "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessConcurrentTerminate.bpmn"
  })
  @Test
  public void testTerminateInCallActivityConcurrent() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // should terminate the called process and continue the parent
    long executionEntities = runtimeService.createExecutionQuery().count();
    assertEquals(1, executionEntities);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources={
    "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.testTerminateInCallActivityConcurrentMulitInstance.bpmn",
    "org/operaton/bpm/engine/test/bpmn/event/end/TerminateEndEventTest.subProcessConcurrentTerminate.bpmn"
  })
  @Test
  public void testTerminateInCallActivityConcurrentMulitInstance() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("terminateEndEventExample");

    // should terminate the called process and continue the parent
    long executionEntities = runtimeService.createExecutionQuery().count();
    assertEquals(1, executionEntities);

    Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).taskDefinitionKey("preNormalEnd").singleResult();
    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }
}