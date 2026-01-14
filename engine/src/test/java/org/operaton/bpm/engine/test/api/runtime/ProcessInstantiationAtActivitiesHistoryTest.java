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
package org.operaton.bpm.engine.test.api.runtime;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableUpdate;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
@ExtendWith(ProcessEngineExtension.class)
class ProcessInstantiationAtActivitiesHistoryTest {

  protected static final String PARALLEL_GATEWAY_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.parallelGateway.bpmn20.xml";
  protected static final String EXCLUSIVE_GATEWAY_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGateway.bpmn20.xml";
  protected static final String SUBPROCESS_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocess.bpmn20.xml";
  protected static final String ASYNC_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGatewayAsyncTask.bpmn20.xml";

  RuntimeService runtimeService;
  TaskService taskService;
  HistoryService historyService;

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testHistoricProcessInstanceForSingleActivityInstantiation() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .startBeforeActivity("task1")
      .execute();

    // then
    HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertThat(historicInstance).isNotNull();
    assertThat(historicInstance.getId()).isEqualTo(instance.getId());
    assertThat(historicInstance.getStartTime()).isNotNull();
    assertThat(historicInstance.getEndTime()).isNull();

    // should be the first activity started
    assertThat(historicInstance.getStartActivityId()).isEqualTo("task1");

    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().singleResult();
    assertThat(historicActivityInstance).isNotNull();
    assertThat(historicActivityInstance.getActivityId()).isEqualTo("task1");
    assertThat(historicActivityInstance.getId()).isNotNull();
    assertThat(historicActivityInstance.getId()).isNotEqualTo(instance.getId());
    assertThat(historicActivityInstance.getStartTime()).isNotNull();
    assertThat(historicActivityInstance.getEndTime()).isNull();
  }

  @Deployment(resources = SUBPROCESS_PROCESS)
  @Test
  void testHistoricActivityInstancesForSubprocess() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("subprocess")
      .startBeforeActivity("innerTask")
      .startBeforeActivity("theSubProcessStart")
      .execute();

    // then
    HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertThat(historicInstance).isNotNull();
    assertThat(historicInstance.getId()).isEqualTo(instance.getId());
    assertThat(historicInstance.getStartTime()).isNotNull();
    assertThat(historicInstance.getEndTime()).isNull();

    // should be the first activity started
    assertThat(historicInstance.getStartActivityId()).isEqualTo("innerTask");

    // subprocess, subprocess start event, two innerTasks
    assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(4);

    HistoricActivityInstance subProcessInstance = historyService.createHistoricActivityInstanceQuery()
        .activityId("subProcess").singleResult();
    assertThat(subProcessInstance).isNotNull();
    assertThat(subProcessInstance.getActivityId()).isEqualTo("subProcess");
    assertThat(subProcessInstance.getId()).isNotNull();
    assertThat(subProcessInstance.getId()).isNotEqualTo(instance.getId());
    assertThat(subProcessInstance.getStartTime()).isNotNull();
    assertThat(subProcessInstance.getEndTime()).isNull();

    HistoricActivityInstance startEventInstance = historyService.createHistoricActivityInstanceQuery()
        .activityId("theSubProcessStart").singleResult();
    assertThat(startEventInstance).isNotNull();
    assertThat(startEventInstance.getActivityId()).isEqualTo("theSubProcessStart");
    assertThat(startEventInstance.getId()).isNotNull();
    assertThat(startEventInstance.getId()).isNotEqualTo(instance.getId());
    assertThat(startEventInstance.getStartTime()).isNotNull();
    assertThat(startEventInstance.getEndTime()).isNotNull();

    List<HistoricActivityInstance> innerTaskInstances = historyService.createHistoricActivityInstanceQuery()
        .activityId("innerTask").list();

    assertThat(innerTaskInstances).hasSize(2);

    for (HistoricActivityInstance innerTaskInstance : innerTaskInstances) {
      assertThat(innerTaskInstance).isNotNull();
      assertThat(innerTaskInstance.getActivityId()).isEqualTo("innerTask");
      assertThat(innerTaskInstance.getId()).isNotNull();
      assertThat(innerTaskInstance.getId()).isNotEqualTo(instance.getId());
      assertThat(innerTaskInstance.getStartTime()).isNotNull();
      assertThat(innerTaskInstance.getEndTime()).isNull();
    }
  }

  @Deployment(resources = ASYNC_PROCESS)
  @Test
  void testHistoricProcessInstanceAsyncStartEvent() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .startBeforeActivity("task2")
      .setVariable("aVar", "aValue")
      .execute();

    // then
    HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertThat(historicInstance).isNotNull();
    assertThat(historicInstance.getId()).isEqualTo(instance.getId());
    assertThat(historicInstance.getStartTime()).isNotNull();
    assertThat(historicInstance.getEndTime()).isNull();

    // should be the first activity started
    assertThat(historicInstance.getStartActivityId()).isEqualTo("task2");

    // task2 wasn't entered yet
    assertThat(historyService.createHistoricActivityInstanceQuery().count()).isZero();

    // history events for variables exist already
    ActivityInstance activityInstance = runtimeService.getActivityInstance(instance.getId());

    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery()
        .variableName("aVar")
        .singleResult();

    assertThat(historicVariable).isNotNull();
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(instance.getId());
    assertThat(historicVariable.getActivityInstanceId()).isEqualTo(activityInstance.getId());
    assertThat(historicVariable.getName()).isEqualTo("aVar");
    assertThat(historicVariable.getValue()).isEqualTo("aValue");

    HistoricDetail historicDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(historicVariable.getId()).singleResult();
    assertThat(historicDetail.getProcessInstanceId()).isEqualTo(instance.getId());
    assertThat(historicDetail).isNotNull();
    assertThat(historicDetail.getActivityInstanceId()).isNull();
    assertThat(historicDetail).isInstanceOf(HistoricVariableUpdate.class);
    assertThat(((HistoricVariableUpdate) historicDetail).getVariableName()).isEqualTo("aVar");
    assertThat(((HistoricVariableUpdate) historicDetail).getValue()).isEqualTo("aValue");
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testHistoricVariableInstanceForSingleActivityInstantiation() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .startBeforeActivity("task1")
      .setVariable("aVar", "aValue")
      .execute();

    ActivityInstance activityInstance = runtimeService.getActivityInstance(instance.getId());

    // then
    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery()
        .variableName("aVar")
        .singleResult();

    assertThat(historicVariable).isNotNull();
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(instance.getId());
    assertThat(historicVariable.getActivityInstanceId()).isEqualTo(activityInstance.getId());
    assertThat(historicVariable.getName()).isEqualTo("aVar");
    assertThat(historicVariable.getValue()).isEqualTo("aValue");

    HistoricDetail historicDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(historicVariable.getId()).singleResult();
    assertThat(historicDetail.getProcessInstanceId()).isEqualTo(instance.getId());
    assertThat(historicDetail).isNotNull();
    assertThat(historicDetail.getActivityInstanceId()).isNull();
    assertThat(historicDetail).isInstanceOf(HistoricVariableUpdate.class);
    assertThat(((HistoricVariableUpdate) historicDetail).getVariableName()).isEqualTo("aVar");
    assertThat(((HistoricVariableUpdate) historicDetail).getValue()).isEqualTo("aValue");
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testHistoricVariableInstanceSetOnProcessInstance() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      // set the variables directly on the instance
      .setVariable("aVar", "aValue")
      .startBeforeActivity("task1")
      .execute();

    ActivityInstance activityInstance = runtimeService.getActivityInstance(instance.getId());

    // then
    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery()
        .variableName("aVar")
        .singleResult();

    assertThat(historicVariable).isNotNull();
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(instance.getId());
    assertThat(historicVariable.getActivityInstanceId()).isEqualTo(activityInstance.getId());
    assertThat(historicVariable.getName()).isEqualTo("aVar");
    assertThat(historicVariable.getValue()).isEqualTo("aValue");

    HistoricDetail historicDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(historicVariable.getId()).singleResult();
    assertThat(historicDetail.getProcessInstanceId()).isEqualTo(instance.getId());
    assertThat(historicDetail).isNotNull();
    assertThat(historicDetail.getActivityInstanceId()).isEqualTo(instance.getId());
    assertThat(historicDetail).isInstanceOf(HistoricVariableUpdate.class);
    assertThat(((HistoricVariableUpdate) historicDetail).getVariableName()).isEqualTo("aVar");
    assertThat(((HistoricVariableUpdate) historicDetail).getValue()).isEqualTo("aValue");
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testHistoricProcessInstanceForSynchronousCompletion() {
    // when the process instance ends immediately
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .startAfterActivity("task1")
      .execute();

    // then
    HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertThat(historicInstance).isNotNull();
    assertThat(historicInstance.getId()).isEqualTo(instance.getId());
    assertThat(historicInstance.getStartTime()).isNotNull();
    assertThat(historicInstance.getEndTime()).isNotNull();

    assertThat(historicInstance.getStartActivityId()).isEqualTo("join");
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  void testSkipCustomListenerEnsureHistoryWritten() {
    // when creating the task skipping custom listeners
    runtimeService.createProcessInstanceByKey("exclusiveGateway")
      .startBeforeActivity("task2")
      .execute(true, false);

    // then the task assignment history (which uses a task listener) is written
    Task task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();

    HistoricActivityInstance instance = historyService
        .createHistoricActivityInstanceQuery()
        .activityId("task2")
        .singleResult();
    assertThat(instance).isNotNull();
    assertThat(instance.getTaskId()).isEqualTo(task.getId());
    assertThat(instance.getAssignee()).isEqualTo("kermit");
  }

  protected void completeTasksInOrder(String... taskNames) {
    for (String taskName : taskNames) {
      // complete any task with that name
      List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey(taskName).listPage(0, 1);
      assertThat(!tasks.isEmpty()).as("task for activity %s does not exist".formatted(taskName)).isTrue();
      taskService.complete(tasks.get(0).getId());
    }
  }
}
