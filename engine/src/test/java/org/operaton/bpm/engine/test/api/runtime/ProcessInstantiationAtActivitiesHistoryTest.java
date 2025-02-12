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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
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
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 * @author Thorben Lindhauer
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class ProcessInstantiationAtActivitiesHistoryTest extends PluggableProcessEngineTest {

  protected static final String PARALLEL_GATEWAY_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.parallelGateway.bpmn20.xml";
  protected static final String EXCLUSIVE_GATEWAY_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGateway.bpmn20.xml";
  protected static final String SUBPROCESS_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocess.bpmn20.xml";
  protected static final String ASYNC_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGatewayAsyncTask.bpmn20.xml";

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  public void testHistoricProcessInstanceForSingleActivityInstantiation() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .startBeforeActivity("task1")
      .execute();

    // then
    HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertNotNull(historicInstance);
    assertThat(historicInstance.getId()).isEqualTo(instance.getId());
    assertNotNull(historicInstance.getStartTime());
    assertNull(historicInstance.getEndTime());

    // should be the first activity started
    assertThat(historicInstance.getStartActivityId()).isEqualTo("task1");

    HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery().singleResult();
    assertNotNull(historicActivityInstance);
    assertThat(historicActivityInstance.getActivityId()).isEqualTo("task1");
    assertNotNull(historicActivityInstance.getId());
    assertNotEquals(instance.getId(), historicActivityInstance.getId());
    assertNotNull(historicActivityInstance.getStartTime());
    assertNull(historicActivityInstance.getEndTime());
  }

  @Deployment(resources = SUBPROCESS_PROCESS)
  @Test
  public void testHistoricActivityInstancesForSubprocess() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("subprocess")
      .startBeforeActivity("innerTask")
      .startBeforeActivity("theSubProcessStart")
      .execute();

    // then
    HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertNotNull(historicInstance);
    assertThat(historicInstance.getId()).isEqualTo(instance.getId());
    assertNotNull(historicInstance.getStartTime());
    assertNull(historicInstance.getEndTime());

    // should be the first activity started
    assertThat(historicInstance.getStartActivityId()).isEqualTo("innerTask");

    // subprocess, subprocess start event, two innerTasks
    assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(4);

    HistoricActivityInstance subProcessInstance = historyService.createHistoricActivityInstanceQuery()
        .activityId("subProcess").singleResult();
    assertNotNull(subProcessInstance);
    assertThat(subProcessInstance.getActivityId()).isEqualTo("subProcess");
    assertNotNull(subProcessInstance.getId());
    assertNotEquals(instance.getId(), subProcessInstance.getId());
    assertNotNull(subProcessInstance.getStartTime());
    assertNull(subProcessInstance.getEndTime());

    HistoricActivityInstance startEventInstance = historyService.createHistoricActivityInstanceQuery()
        .activityId("theSubProcessStart").singleResult();
    assertNotNull(startEventInstance);
    assertThat(startEventInstance.getActivityId()).isEqualTo("theSubProcessStart");
    assertNotNull(startEventInstance.getId());
    assertNotEquals(instance.getId(), startEventInstance.getId());
    assertNotNull(startEventInstance.getStartTime());
    assertNotNull(startEventInstance.getEndTime());

    List<HistoricActivityInstance> innerTaskInstances = historyService.createHistoricActivityInstanceQuery()
        .activityId("innerTask").list();

    assertThat(innerTaskInstances).hasSize(2);

    for (HistoricActivityInstance innerTaskInstance : innerTaskInstances) {
      assertNotNull(innerTaskInstance);
      assertThat(innerTaskInstance.getActivityId()).isEqualTo("innerTask");
      assertNotNull(innerTaskInstance.getId());
      assertNotEquals(instance.getId(), innerTaskInstance.getId());
      assertNotNull(innerTaskInstance.getStartTime());
      assertNull(innerTaskInstance.getEndTime());
    }
  }

  @Deployment(resources = ASYNC_PROCESS)
  @Test
  public void testHistoricProcessInstanceAsyncStartEvent() {
    // when
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .startBeforeActivity("task2")
      .setVariable("aVar", "aValue")
      .execute();

    // then
    HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertNotNull(historicInstance);
    assertThat(historicInstance.getId()).isEqualTo(instance.getId());
    assertNotNull(historicInstance.getStartTime());
    assertNull(historicInstance.getEndTime());

    // should be the first activity started
    assertThat(historicInstance.getStartActivityId()).isEqualTo("task2");

    // task2 wasn't entered yet
    assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(0);

    // history events for variables exist already
    ActivityInstance activityInstance = runtimeService.getActivityInstance(instance.getId());

    HistoricVariableInstance historicVariable = historyService.createHistoricVariableInstanceQuery()
        .variableName("aVar")
        .singleResult();

    assertNotNull(historicVariable);
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(instance.getId());
    assertThat(historicVariable.getActivityInstanceId()).isEqualTo(activityInstance.getId());
    assertThat(historicVariable.getName()).isEqualTo("aVar");
    assertThat(historicVariable.getValue()).isEqualTo("aValue");

    HistoricDetail historicDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(historicVariable.getId()).singleResult();
    assertThat(historicDetail.getProcessInstanceId()).isEqualTo(instance.getId());
    assertNotNull(historicDetail);
    // TODO: fix if this is not ok due to CAM-3886
    assertNull(historicDetail.getActivityInstanceId());
    assertTrue(historicDetail instanceof HistoricVariableUpdate);
    assertThat(((HistoricVariableUpdate) historicDetail).getVariableName()).isEqualTo("aVar");
    assertThat(((HistoricVariableUpdate) historicDetail).getValue()).isEqualTo("aValue");
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  public void testHistoricVariableInstanceForSingleActivityInstantiation() {
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

    assertNotNull(historicVariable);
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(instance.getId());
    assertThat(historicVariable.getActivityInstanceId()).isEqualTo(activityInstance.getId());
    assertThat(historicVariable.getName()).isEqualTo("aVar");
    assertThat(historicVariable.getValue()).isEqualTo("aValue");

    HistoricDetail historicDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(historicVariable.getId()).singleResult();
    assertThat(historicDetail.getProcessInstanceId()).isEqualTo(instance.getId());
    assertNotNull(historicDetail);
    // TODO: fix if this is not ok due to CAM-3886
    assertNull(historicDetail.getActivityInstanceId());
    assertTrue(historicDetail instanceof HistoricVariableUpdate);
    assertThat(((HistoricVariableUpdate) historicDetail).getVariableName()).isEqualTo("aVar");
    assertThat(((HistoricVariableUpdate) historicDetail).getValue()).isEqualTo("aValue");
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  public void testHistoricVariableInstanceSetOnProcessInstance() {
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

    assertNotNull(historicVariable);
    assertThat(historicVariable.getProcessInstanceId()).isEqualTo(instance.getId());
    assertThat(historicVariable.getActivityInstanceId()).isEqualTo(activityInstance.getId());
    assertThat(historicVariable.getName()).isEqualTo("aVar");
    assertThat(historicVariable.getValue()).isEqualTo("aValue");

    HistoricDetail historicDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(historicVariable.getId()).singleResult();
    assertThat(historicDetail.getProcessInstanceId()).isEqualTo(instance.getId());
    assertNotNull(historicDetail);
    // TODO: fix if this is not ok due to CAM-3886
    assertThat(historicDetail.getActivityInstanceId()).isEqualTo(instance.getId());
    assertTrue(historicDetail instanceof HistoricVariableUpdate);
    assertThat(((HistoricVariableUpdate) historicDetail).getVariableName()).isEqualTo("aVar");
    assertThat(((HistoricVariableUpdate) historicDetail).getValue()).isEqualTo("aValue");
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  public void testHistoricProcessInstanceForSynchronousCompletion() {
    // when the process instance ends immediately
    ProcessInstance instance = runtimeService
      .createProcessInstanceByKey("exclusiveGateway")
      .startAfterActivity("task1")
      .execute();

    // then
    HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertNotNull(historicInstance);
    assertThat(historicInstance.getId()).isEqualTo(instance.getId());
    assertNotNull(historicInstance.getStartTime());
    assertNotNull(historicInstance.getEndTime());

    assertThat(historicInstance.getStartActivityId()).isEqualTo("join");
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  public void testSkipCustomListenerEnsureHistoryWritten() {
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
    assertNotNull(instance);
    assertThat(instance.getTaskId()).isEqualTo(task.getId());
    assertThat(instance.getAssignee()).isEqualTo("kermit");
  }

  protected void completeTasksInOrder(String... taskNames) {
    for (String taskName : taskNames) {
      // complete any task with that name
      List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey(taskName).listPage(0, 1);
      assertTrue("task for activity " + taskName + " does not exist", !tasks.isEmpty());
      taskService.complete(tasks.get(0).getId());
    }
  }
}
