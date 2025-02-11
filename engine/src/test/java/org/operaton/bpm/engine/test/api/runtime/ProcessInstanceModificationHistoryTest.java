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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.impl.history.event.HistoricVariableUpdateEventEntity;
import org.operaton.bpm.engine.runtime.ActivityInstance;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.engine.variable.Variables;
import org.junit.Test;

/**
 * @author Thorben Lindhauer
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
public class ProcessInstanceModificationHistoryTest extends PluggableProcessEngineTest {

  protected static final String ONE_TASK_PROCESS = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";
  protected static final String EXCLUSIVE_GATEWAY_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGateway.bpmn20.xml";
  protected static final String EXCLUSIVE_GATEWAY_ASYNC_TASK_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGatewayAsyncTask.bpmn20.xml";
  protected static final String SUBPROCESS_PROCESS = "org/operaton/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocess.bpmn20.xml";

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  public void testStartBeforeWithVariablesInHistory() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task2")
      .setVariable("procInstVar", "procInstValue")
      .setVariableLocal("localVar", "localValue")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());

    HistoricVariableInstance procInstVariable = historyService.createHistoricVariableInstanceQuery()
      .variableName("procInstVar")
      .singleResult();

    assertNotNull(procInstVariable);
    assertThat(procInstVariable.getActivityInstanceId()).isEqualTo(updatedTree.getId());
    assertThat(procInstVariable.getName()).isEqualTo("procInstVar");
    assertThat(procInstVariable.getValue()).isEqualTo("procInstValue");

    HistoricDetail procInstanceVarDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(procInstVariable.getId()).singleResult();
    assertNotNull(procInstanceVarDetail);
    // when starting before/after an activity instance, the activity instance id of the
    // execution is null and so is the activity instance id of the historic detail
    assertNull(procInstanceVarDetail.getActivityInstanceId());

    HistoricVariableInstance localVariable = historyService.createHistoricVariableInstanceQuery()
      .variableName("localVar")
      .singleResult();

    assertNotNull(localVariable);
    assertNull(localVariable.getActivityInstanceId());
    assertThat(localVariable.getName()).isEqualTo("localVar");
    assertThat(localVariable.getValue()).isEqualTo("localValue");

    HistoricDetail localInstanceVarDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(localVariable.getId()).singleResult();
    assertNotNull(localInstanceVarDetail);
    assertNull(localInstanceVarDetail.getActivityInstanceId());

    completeTasksInOrder("task1", "task2");
    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_ASYNC_TASK_PROCESS)
  @Test
  public void testStartBeforeAsyncWithVariablesInHistory() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task2")
      .setVariable("procInstVar", "procInstValue")
      .setVariableLocal("localVar", "localValue")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());

    HistoricVariableInstance procInstVariable = historyService.createHistoricVariableInstanceQuery()
      .variableName("procInstVar")
      .singleResult();

    assertNotNull(procInstVariable);
    assertThat(procInstVariable.getActivityInstanceId()).isEqualTo(updatedTree.getId());
    assertThat(procInstVariable.getName()).isEqualTo("procInstVar");
    assertThat(procInstVariable.getValue()).isEqualTo("procInstValue");

    HistoricDetail procInstanceVarDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(procInstVariable.getId()).singleResult();
    assertNotNull(procInstanceVarDetail);
    // when starting before/after an activity instance, the activity instance id of the
    // execution is null and so is the activity instance id of the historic detail
    assertNull(procInstanceVarDetail.getActivityInstanceId());

    HistoricVariableInstance localVariable = historyService.createHistoricVariableInstanceQuery()
      .variableName("localVar")
      .singleResult();

    assertNotNull(localVariable);
    // the following is null because localVariable is local on a concurrent execution
    // but the concurrent execution does not execute an activity at the time the variable is set
    assertNull(localVariable.getActivityInstanceId());
    assertThat(localVariable.getName()).isEqualTo("localVar");
    assertThat(localVariable.getValue()).isEqualTo("localValue");

    HistoricDetail localInstanceVarDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(localVariable.getId()).singleResult();
    assertNotNull(localInstanceVarDetail);
    assertNull(localInstanceVarDetail.getActivityInstanceId());

    // end process instance
    completeTasksInOrder("task1");

    Job job = managementService.createJobQuery().singleResult();
    managementService.executeJob(job.getId());

    completeTasksInOrder("task2");
    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment(resources = SUBPROCESS_PROCESS)
  @Test
  public void testStartBeforeScopeWithVariablesInHistory() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subprocess");

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("innerTask")
      .setVariable("procInstVar", "procInstValue")
      .setVariableLocal("localVar", "localValue")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());

    HistoricVariableInstance procInstVariable = historyService.createHistoricVariableInstanceQuery()
      .variableName("procInstVar")
      .singleResult();

    assertNotNull(procInstVariable);
    assertThat(procInstVariable.getActivityInstanceId()).isEqualTo(updatedTree.getId());
    assertThat(procInstVariable.getName()).isEqualTo("procInstVar");
    assertThat(procInstVariable.getValue()).isEqualTo("procInstValue");

    HistoricDetail procInstanceVarDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(procInstVariable.getId()).singleResult();
    assertNotNull(procInstanceVarDetail);
    // when starting before/after an activity instance, the activity instance id of the
    // execution is null and so is the activity instance id of the historic detail
    assertNull(procInstanceVarDetail.getActivityInstanceId());

    HistoricVariableInstance localVariable = historyService.createHistoricVariableInstanceQuery()
      .variableName("localVar")
      .singleResult();

    assertNotNull(localVariable);
    assertThat(localVariable.getActivityInstanceId()).isEqualTo(updatedTree.getActivityInstances("subProcess")[0].getId());
    assertThat(localVariable.getName()).isEqualTo("localVar");
    assertThat(localVariable.getValue()).isEqualTo("localValue");

    HistoricDetail localInstanceVarDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(localVariable.getId()).singleResult();
    assertNotNull(localInstanceVarDetail);
    assertNull(localInstanceVarDetail.getActivityInstanceId());

  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  public void testStartTransitionWithVariablesInHistory() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startTransition("flow2")
      .setVariable("procInstVar", "procInstValue")
      .setVariableLocal("localVar", "localValue")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());

    HistoricVariableInstance procInstVariable = historyService.createHistoricVariableInstanceQuery()
      .variableName("procInstVar")
      .singleResult();

    assertNotNull(procInstVariable);
    assertThat(procInstVariable.getActivityInstanceId()).isEqualTo(updatedTree.getId());
    assertThat(procInstVariable.getName()).isEqualTo("procInstVar");
    assertThat(procInstVariable.getValue()).isEqualTo("procInstValue");

    HistoricDetail procInstanceVarDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(procInstVariable.getId()).singleResult();
    assertNotNull(procInstanceVarDetail);
    assertThat(procInstVariable.getActivityInstanceId()).isEqualTo(updatedTree.getId());

    HistoricVariableInstance localVariable = historyService.createHistoricVariableInstanceQuery()
      .variableName("localVar")
      .singleResult();

    assertNotNull(localVariable);
    assertThat(procInstVariable.getActivityInstanceId()).isEqualTo(updatedTree.getId());
    assertThat(localVariable.getName()).isEqualTo("localVar");
    assertThat(localVariable.getValue()).isEqualTo("localValue");

    HistoricDetail localInstanceVarDetail = historyService.createHistoricDetailQuery()
        .variableInstanceId(localVariable.getId()).singleResult();
    assertNotNull(localInstanceVarDetail);
    // when starting before/after an activity instance, the activity instance id of the
    // execution is null and so is the activity instance id of the historic detail
    assertNull(localInstanceVarDetail.getActivityInstanceId());

    completeTasksInOrder("task1", "task1");
    testRule.assertProcessEnded(processInstance.getId());

  }

  @Deployment(resources = ONE_TASK_PROCESS)
  @Test
  public void testCancelTaskShouldCancelProcessInstance() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("oneTaskProcess").getId();

    // when
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .cancelAllForActivity("theTask")
      .execute(true, false);

    // then
    HistoricProcessInstance instance = historyService.createHistoricProcessInstanceQuery().singleResult();
    assertNotNull(instance);

    assertThat(instance.getId()).isEqualTo(processInstanceId);
    assertNotNull(instance.getEndTime());
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  @Test
  public void testSkipCustomListenerEnsureHistoryWritten() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("exclusiveGateway").getId();

    // when creating the task skipping custom listeners
    runtimeService.createProcessInstanceModification(processInstanceId)
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

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneAsyncTaskProcess.bpmn20.xml"})
  @Test
  public void testHistoricVariablesOnAsyncBefore() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess",
        Variables.createVariables().putValue("foo", "bar"));

    executeJob(managementService.createJobQuery().singleResult());

    // when
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("theStart")
      .execute(true, true);

    // then
    HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertNotNull(variable);
    assertThat(variable.getProcessInstanceId()).isEqualTo(processInstance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/history/oneAsyncTaskProcess.bpmn20.xml"})
  @Test
  public void testModifyWithNonInitialVariables() {
    // given
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    executeJob(managementService.createJobQuery().singleResult());

    // when
    runtimeService.createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("theStart")
      .setVariable("var1", "value1")
      .execute(true, true);

    // then
    HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().singleResult();
    assertNotNull(variable);
    assertThat(variable.getProcessInstanceId()).isEqualTo(processInstance.getId());

    HistoricVariableUpdateEventEntity historicDetail = (HistoricVariableUpdateEventEntity) historyService.createHistoricDetailQuery()
        .processInstanceId(processInstance.getId())
        .singleResult();

    assertNotNull(historicDetail);
    assertFalse(historicDetail.isInitial());
    assertThat(historicDetail.getVariableName()).isEqualTo("var1");
    assertThat(historicDetail.getTextValue()).isEqualTo("value1");
  }

  protected ActivityInstance getChildInstanceForActivity(ActivityInstance activityInstance, String activityId) {
    if (activityId.equals(activityInstance.getActivityId())) {
      return activityInstance;
    }

    for (ActivityInstance childInstance : activityInstance.getChildActivityInstances()) {
      ActivityInstance instance = getChildInstanceForActivity(childInstance, activityId);
      if (instance != null) {
        return instance;
      }
    }

    return null;
  }

  protected void completeTasksInOrder(String... taskNames) {
    for (String taskName : taskNames) {
      // complete any task with that name
      List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey(taskName).listPage(0, 1);
      assertTrue("task for activity " + taskName + " does not exist", !tasks.isEmpty());
      taskService.complete(tasks.get(0).getId());
    }
  }


  protected void executeJob(Job job) {
    while (job != null && job.getRetries() > 0) {
      try {
        managementService.executeJob(job.getId());
      }
      catch (Exception e) {
        // ignore
      }

      job = managementService.createJobQuery().jobId(job.getId()).singleResult();
    }
  }

}
