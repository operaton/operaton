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
package org.operaton.bpm.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricActivityInstance;
import org.operaton.bpm.engine.history.HistoricActivityInstanceQuery;
import org.operaton.bpm.engine.history.HistoricDetail;
import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.HistoricVariableInstanceQuery;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
public class HistoricVariableInstanceScopeTest extends PluggableProcessEngineTest {

  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSetVariableOnProcessInstanceStart() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("testVar", "testValue");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
    assertThat(query.count()).isEqualTo(1);

    HistoricVariableInstance variable = query.singleResult();
    assertThat(variable).isNotNull();

    // the variable is in the process instance scope
    assertThat(variable.getActivityInstanceId()).isEqualTo(pi.getId());

    taskService.complete(taskService.createTaskQuery().singleResult().getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSetVariableLocalOnUserTask() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("oneTaskProcess");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    taskService.setVariableLocal(task.getId(), "testVar", "testValue");
    ExecutionEntity taskExecution = (ExecutionEntity) runtimeService.createExecutionQuery()
        .executionId(task.getExecutionId())
        .singleResult();
    assertThat(taskExecution).isNotNull();

    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
    assertThat(query.count()).isEqualTo(1);

    HistoricVariableInstance variable = query.singleResult();
    assertThat(variable).isNotNull();

    // the variable is in the task scope
    assertThat(variable.getActivityInstanceId()).isEqualTo(taskExecution.getActivityInstanceId());

    taskService.complete(task.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"})
  @Test
  public void testSetVariableOnProcessIntanceStartAndSetVariableLocalOnUserTask() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("testVar", "testValue");
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    taskService.setVariableLocal(task.getId(), "testVar", "anotherTestValue");
    ExecutionEntity taskExecution = (ExecutionEntity) runtimeService.createExecutionQuery().singleResult();
    assertThat(taskExecution).isNotNull();

    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
    assertThat(query.count()).isEqualTo(2);

    List<HistoricVariableInstance> result = query.list();

    HistoricVariableInstance firstVar = result.get(0);
    assertThat(firstVar.getVariableName()).isEqualTo("testVar");
    assertThat(firstVar.getValue()).isEqualTo("testValue");
    // the variable is in the process instance scope
    assertThat(firstVar.getActivityInstanceId()).isEqualTo(pi.getId());

    HistoricVariableInstance secondVar = result.get(1);
    assertThat(secondVar.getVariableName()).isEqualTo("testVar");
    assertThat(secondVar.getValue()).isEqualTo("anotherTestValue");
    // the variable is in the task scope
    assertThat(secondVar.getActivityInstanceId()).isEqualTo(taskExecution.getActivityInstanceId());

    taskService.complete(task.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/oneSubProcess.bpmn20.xml"})
  @Test
  public void testSetVariableOnUserTaskInsideSubProcess() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    taskService.setVariable(task.getId(), "testVar", "testValue");

    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
    assertThat(query.count()).isEqualTo(1);

    HistoricVariableInstance variable = query.singleResult();
    // the variable is in the process instance scope
    assertThat(variable.getActivityInstanceId()).isEqualTo(pi.getId());

    taskService.complete(task.getId());
    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testSetVariableOnServiceTaskInsideSubProcess() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
    assertThat(query.count()).isEqualTo(1);

    HistoricVariableInstance variable = query.singleResult();
    // the variable is in the process instance scope
    assertThat(variable.getActivityInstanceId()).isEqualTo(pi.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testSetVariableLocalOnServiceTaskInsideSubProcess() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
    assertThat(query.count()).isEqualTo(1);

    String activityInstanceId = historyService.createHistoricActivityInstanceQuery()
        .activityId("SubProcess_1")
        .singleResult()
        .getId();

    HistoricVariableInstance variable = query.singleResult();
    // the variable is in the sub process scope
    assertThat(variable.getActivityInstanceId()).isEqualTo(activityInstanceId);

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testSetVariableLocalOnTaskInsideParallelBranch() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    taskService.setVariableLocal(task.getId(), "testVar", "testValue");
    ExecutionEntity taskExecution = (ExecutionEntity) runtimeService.createExecutionQuery()
        .executionId(task.getExecutionId())
        .singleResult();
    assertThat(taskExecution).isNotNull();

    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
    assertThat(query.count()).isEqualTo(1);

    HistoricVariableInstance variable = query.singleResult();
    // the variable is in the user task scope
    assertThat(variable.getActivityInstanceId()).isEqualTo(taskExecution.getActivityInstanceId());

    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/history/HistoricVariableInstanceScopeTest.testSetVariableLocalOnTaskInsideParallelBranch.bpmn"})
  @Test
  public void testSetVariableOnTaskInsideParallelBranch() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    taskService.setVariable(task.getId(), "testVar", "testValue");

    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
    assertThat(query.count()).isEqualTo(1);

    HistoricVariableInstance variable = query.singleResult();
    // the variable is in the process instance scope
    assertThat(variable.getActivityInstanceId()).isEqualTo(pi.getId());

    taskService.complete(task.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testSetVariableOnServiceTaskInsideParallelBranch() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
    assertThat(query.count()).isEqualTo(1);

    HistoricVariableInstance variable = query.singleResult();
    // the variable is in the process instance scope
    assertThat(variable.getActivityInstanceId()).isEqualTo(pi.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment
  @Test
  public void testSetVariableLocalOnServiceTaskInsideParallelBranch() {
    ProcessInstance pi = runtimeService.startProcessInstanceByKey("process");

    HistoricActivityInstance serviceTask = historyService.createHistoricActivityInstanceQuery()
        .activityId("serviceTask1")
        .singleResult();
    assertThat(serviceTask).isNotNull();

    HistoricVariableInstanceQuery query = historyService.createHistoricVariableInstanceQuery();
    assertThat(query.count()).isEqualTo(1);

    HistoricVariableInstance variable = query.singleResult();
    // the variable is in the service task scope
    assertThat(variable.getActivityInstanceId()).isEqualTo(serviceTask.getId());

    testRule.assertProcessEnded(pi.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testHistoricCaseVariableInstanceQuery() {
    // start case instance with variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("foo", "bar");
    String caseInstanceId =  caseService.createCaseInstanceByKey("oneTaskCase", variables).getId();

    String caseExecutionId = caseService.createCaseExecutionQuery().activityId("CasePlanModel_1").singleResult().getId();
    String taskExecutionId = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult().getId();

    // set variable on both executions
    caseService.setVariableLocal(caseExecutionId, "case", "execution");
    caseService.setVariableLocal(taskExecutionId, "task", "execution");

    // update variable on both executions
    caseService.setVariableLocal(caseExecutionId, "case", "update");
    caseService.setVariableLocal(taskExecutionId, "task", "update");

    assertThat(historyService.createHistoricVariableInstanceQuery().count()).isEqualTo(3);
    assertThat(historyService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceId).count()).isEqualTo(3);
    assertThat(historyService.createHistoricVariableInstanceQuery().caseExecutionIdIn(caseExecutionId, taskExecutionId).count()).isEqualTo(3);
    assertThat(historyService.createHistoricVariableInstanceQuery().caseExecutionIdIn(caseExecutionId).count()).isEqualTo(2);
    assertThat(historyService.createHistoricVariableInstanceQuery().caseExecutionIdIn(taskExecutionId).count()).isEqualTo(1);

    HistoryLevel historyLevel = processEngineConfiguration.getHistoryLevel();
    if (historyLevel.equals(HistoryLevel.HISTORY_LEVEL_FULL)) {
      assertThat(historyService.createHistoricDetailQuery().count()).isEqualTo(5);
      assertThat(historyService.createHistoricDetailQuery().caseInstanceId(caseInstanceId).count()).isEqualTo(5);
      assertThat(historyService.createHistoricDetailQuery().caseExecutionId(caseExecutionId).count()).isEqualTo(3);
      assertThat(historyService.createHistoricDetailQuery().caseExecutionId(taskExecutionId).count()).isEqualTo(2);
    }
  }

  @Deployment
  @Test
  public void testInputMappings() {
    // given
    String processInstanceId = runtimeService.startProcessInstanceByKey("process").getId();

    HistoricActivityInstanceQuery activityInstanceQuery = historyService
        .createHistoricActivityInstanceQuery()
        .processInstanceId(processInstanceId);

    String theService1Id = activityInstanceQuery.activityId("theService1").singleResult().getId();
    String theService2Id = activityInstanceQuery.activityId("theService2").singleResult().getId();
    String theTaskId = activityInstanceQuery.activityId("theTask").singleResult().getId();

    // when (1)
    HistoricVariableInstance firstVariable = historyService
      .createHistoricVariableInstanceQuery()
      .variableName("firstInputVariable")
      .singleResult();

    // then (1)
    assertThat(firstVariable.getActivityInstanceId()).isEqualTo(theService1Id);

    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      HistoricDetail firstVariableDetail = historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .variableInstanceId(firstVariable.getId())
        .singleResult();
      assertThat(firstVariableDetail.getActivityInstanceId()).isEqualTo(theService1Id);
    }

    // when (2)
    HistoricVariableInstance secondVariable = historyService
      .createHistoricVariableInstanceQuery()
      .variableName("secondInputVariable")
      .singleResult();

    // then (2)
    assertThat(secondVariable.getActivityInstanceId()).isEqualTo(theService2Id);

    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      HistoricDetail secondVariableDetail = historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .variableInstanceId(secondVariable.getId())
        .singleResult();
      assertThat(secondVariableDetail.getActivityInstanceId()).isEqualTo(theService2Id);
    }

    // when (3)
    HistoricVariableInstance thirdVariable = historyService
      .createHistoricVariableInstanceQuery()
      .variableName("thirdInputVariable")
      .singleResult();

    // then (3)
    assertThat(thirdVariable.getActivityInstanceId()).isEqualTo(theTaskId);

    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      HistoricDetail thirdVariableDetail = historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .variableInstanceId(thirdVariable.getId())
        .singleResult();
      assertThat(thirdVariableDetail.getActivityInstanceId()).isEqualTo(theTaskId);
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCmmnActivityInstanceIdOnCaseInstance() {

    // given
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase");

    String taskExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(taskExecutionId)
      .setVariable("foo", "bar")
      .execute();

    // then
    HistoricVariableInstance variable = historyService
        .createHistoricVariableInstanceQuery()
        .variableName("foo")
        .singleResult();

    assertThat(variable).isNotNull();
    assertThat(variable.getActivityInstanceId()).isEqualTo(caseInstance.getId());

    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      HistoricDetail variableDetail = historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .variableInstanceId(variable.getId())
        .singleResult();
      assertThat(variableDetail.getActivityInstanceId()).isEqualTo(taskExecutionId);
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCmmnActivityInstanceIdOnCaseExecution() {

    // given
    caseService.createCaseInstanceByKey("oneTaskCase");

    String taskExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(taskExecutionId)
      .setVariableLocal("foo", "bar")
      .execute();

    // then
    HistoricVariableInstance variable = historyService
        .createHistoricVariableInstanceQuery()
        .variableName("foo")
        .singleResult();

    assertThat(variable).isNotNull();
    assertThat(variable.getActivityInstanceId()).isEqualTo(taskExecutionId);

    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      HistoricDetail variableDetail = historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .variableInstanceId(variable.getId())
        .singleResult();
      assertThat(variableDetail.getActivityInstanceId()).isEqualTo(taskExecutionId);
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCmmnActivityInstanceIdOnTask() {

    // given
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase");

    String taskExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    Task task = taskService
        .createTaskQuery()
        .singleResult();

    // when
    taskService.setVariable(task.getId(), "foo", "bar");

    // then
    HistoricVariableInstance variable = historyService
        .createHistoricVariableInstanceQuery()
        .variableName("foo")
        .singleResult();

    assertThat(variable).isNotNull();
    assertThat(variable.getActivityInstanceId()).isEqualTo(caseInstance.getId());

    if(processEngineConfiguration.getHistoryLevel().getId() > ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
      HistoricDetail variableDetail = historyService
        .createHistoricDetailQuery()
        .variableUpdates()
        .variableInstanceId(variable.getId())
        .singleResult();
      assertThat(variableDetail.getActivityInstanceId()).isEqualTo(taskExecutionId);
    }

  }

}
