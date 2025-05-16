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

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.api.runtime.util.SetBusinessKeyListener.BUSINESS_KEY_VARIABLE;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.JavaDelegate;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.runtime.util.SetBusinessKeyListener;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class SetBusinessKeyTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected static final String PROCESS_KEY = "process";

  protected static final BpmnModelInstance SYNC_SERVICE_TASK_PROCESS =
      Bpmn.createExecutableProcess(PROCESS_KEY)
      .startEvent("startEvent")
      .serviceTask()
        .operatonClass(SetBusinessKeyDelegate.class)
      .userTask("userTask2")
      .endEvent("endEvent")
      .done();

  protected static final BpmnModelInstance ASYNC_SERVICE_TASK_PROCESS =
      Bpmn.createExecutableProcess(PROCESS_KEY)
      .startEvent("startEvent")
      .serviceTask()
        .operatonAsyncBefore()
        .operatonClass(SetBusinessKeyDelegate.class)
      .userTask("userTask2")
      .endEvent("endEvent")
      .done();

  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;
  ManagementService managementService;

  @Test
  void testNewKeyInSyncServiceTask() {
    // given
    testRule.deploy(SYNC_SERVICE_TASK_PROCESS);

    // when
    String newBusinessKeyValue = "newBusinessKey";
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }

  @Test
  void testNewKeyInAsyncServiceTask() {
    // given
    testRule.deploy(ASYNC_SERVICE_TASK_PROCESS);

    String newBusinessKeyValue = "newBusinessKey";
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    // when
    executeJob();

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }

  @Test
  void testNewKeyInStartExecListener() {
    // given
    String listener = ExecutionListener.EVENTNAME_START;
    BpmnModelInstance process = createModelExecutionListener(listener);
    testRule.deploy(process);

    // when
    String newBusinessKeyValue = "newBusinessKey";
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }

  @Test
  void testNewKeyInEndExecListener() {
    // given
    String listener = ExecutionListener.EVENTNAME_END;
    BpmnModelInstance process = createModelExecutionListener(listener);
    testRule.deploy(process);

    String newBusinessKeyValue = "newBusinessKey";
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    completeTask("userTask1");

    // assume
    assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask2").singleResult()).isNotNull();

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }


  @Test
  void testNewKeyInStartTaskListener() {
    // given
    String listener = TaskListener.EVENTNAME_CREATE;
    BpmnModelInstance process = createModelTaskListener(listener);
    testRule.deploy(process);

    // when
    String newBusinessKeyValue = "newBusinessKey";
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }

  @Test
  void testNewKeyInAssignTaskListener() {
    // given
    String listener = TaskListener.EVENTNAME_ASSIGNMENT;
    BpmnModelInstance process = createModelTaskListener(listener);
    testRule.deploy(process);

    String newBusinessKeyValue = "newBusinessKey";
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    // when
    taskService.setAssignee(taskService.createTaskQuery().singleResult().getId(), "newUserId");

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }

  @Test
  void testNewKeyInEndTaskListener() {
    // given
    String listener = TaskListener.EVENTNAME_COMPLETE;
    BpmnModelInstance process = createModelTaskListener(listener);
    testRule.deploy(process);

    String newBusinessKeyValue = "newBusinessKey";
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    completeTask("userTask1");

    // assume
    assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask2").singleResult()).isNotNull();

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }

  @Test
  @Deployment
  void testNewKeyInTimeoutTaskListener() {
    // given
    String newBusinessKeyValue = "newBusinessKey";
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    // when
    ClockUtil.offset(TimeUnit.MINUTES.toMillis(70L));
    testRule.waitForJobExecutorToProcessAllJobs(5000L);

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }

  @Test
  void testUpdateKeyInSyncServiceTask() {
    // given
    testRule.deploy(SYNC_SERVICE_TASK_PROCESS);

    // when
    String newBusinessKeyValue = "newBusinessKey";
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, "aBusinessKey", Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }

  @Test
  void testUpdateKeyInAsyncServiceTask() {
    // given
    testRule.deploy(ASYNC_SERVICE_TASK_PROCESS);

    String newBusinessKeyValue = "newBusinessKey";
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, "aBusinessKey", Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    // when
    executeJob();

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }

  @Test
  void testUpdateKeyInStartExecListener() {
    // given
    String listener = ExecutionListener.EVENTNAME_START;
    BpmnModelInstance process = createModelExecutionListener(listener);
    testRule.deploy(process);

    // when
    String newBusinessKeyValue = "newBusinessKey";
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, "aBusinessKey", Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }

  @Test
  void testUpdateKeyInEndExecListener() {
    // given
    String listener = ExecutionListener.EVENTNAME_END;
    BpmnModelInstance process = createModelExecutionListener(listener);
    testRule.deploy(process);

    String newBusinessKeyValue = "newBusinessKey";
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, "aBusinessKey", Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    completeTask("userTask1");

    // assume
    assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask2").singleResult()).isNotNull();

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }

  @Test
  void testUpdateKeyInEndTaskListener() {
    // given
    String listener = TaskListener.EVENTNAME_COMPLETE;
    BpmnModelInstance process = createModelTaskListener(listener);
    testRule.deploy(process);

    String newBusinessKeyValue = "newBusinessKey";
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, "aBusinessKey", Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    // when
    completeTask("userTask1");

    // assume
    assertThat(taskService.createTaskQuery().taskDefinitionKey("userTask2").singleResult()).isNotNull();

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }

  @Test
  void testUpdateKeyNullValueInStartTaskListener() {
    // given
    String listener = TaskListener.EVENTNAME_CREATE;
    BpmnModelInstance process = createModelTaskListener(listener);
    testRule.deploy(process);

    // when
    String newBusinessKeyValue = null;
    runtimeService.startProcessInstanceByKey(PROCESS_KEY, "aBusinessKey", Variables.createVariables().putValue(BUSINESS_KEY_VARIABLE, newBusinessKeyValue));

    // then
    checkBusinessKeyChanged(newBusinessKeyValue);
  }

  protected void checkBusinessKeyChanged(String newBusinessKeyValue) {
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_KEY).singleResult();
    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getBusinessKey()).isEqualTo(newBusinessKeyValue);

    HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertThat(historicInstance).isNotNull();
    assertThat(historicInstance.getBusinessKey()).isEqualTo(newBusinessKeyValue);
  }

  protected BpmnModelInstance createModelExecutionListener(String listener) {
    return Bpmn.createExecutableProcess(PROCESS_KEY)
    .startEvent("startEvent")
    .userTask("userTask1").name("User task")
      .operatonExecutionListenerExpression(listener,
            "${execution.setProcessBusinessKey(execution.getVariable(\"" + BUSINESS_KEY_VARIABLE + "\"))}")
    .userTask("userTask2")
    .endEvent("endEvent")
    .done();
  }

  protected BpmnModelInstance createModelTaskListener(String listener) {
    return Bpmn.createExecutableProcess(PROCESS_KEY)
    .startEvent("startEvent")
    .userTask("userTask1").name("User task")
      .operatonTaskListenerClass(listener, SetBusinessKeyListener.class)
    .userTask("userTask2")
    .endEvent("endEvent")
    .done();
  }

  protected void executeJob() {
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();
    managementService.executeJob(job.getId());
  }

  protected void completeTask(String key) {
    Task task = taskService.createTaskQuery().taskDefinitionKey(key).singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());
  }

  public static class SetBusinessKeyDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
      String newKeyValue = (String) execution.getVariable(BUSINESS_KEY_VARIABLE);
      execution.setProcessBusinessKey(newKeyValue);
    }

  }

}
