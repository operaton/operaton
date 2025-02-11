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
package org.operaton.bpm.engine.test.api.cmmn;

import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.runtime.*;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
public class CaseServiceProcessTaskTest extends PluggableProcessEngineTest {

  protected final String DEFINITION_KEY = "oneProcessTaskCase";
  protected final String PROCESS_TASK_KEY = "PI_ProcessTask_1";

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  public void testStart() {
    // given
    String caseInstanceId = createCaseInstance(DEFINITION_KEY).getId();

    ProcessInstance processInstance;

    // then
    processInstance = queryProcessInstance();

    assertNotNull(processInstance);
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertTrue(processTask.isActive());
  }

  protected void verifyVariables(String caseInstanceId, List<VariableInstance> result) {
    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if (variable.getName().equals("aVariableName")) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if (variable.getName().equals("anotherVariableName")) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
      })
  @Test
  public void testManualStart() {
    // given
    String caseInstanceId = createCaseInstance(DEFINITION_KEY).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    ProcessInstance processInstance = queryProcessInstance();
    assertNull(processInstance);

    // when
    caseService
      .withCaseExecution(processTaskId)
      .manualStart();

    // then
    processInstance = queryProcessInstance();

    assertNotNull(processInstance);
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertTrue(processTask.isActive());
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
      })
  @Test
  public void testManualStartWithVariables() {
    // given
    String caseInstanceId = createCaseInstance(DEFINITION_KEY).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    ProcessInstance processInstance = queryProcessInstance();
    assertNull(processInstance);

    // variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 999);

    // when
    caseService
      .withCaseExecution(processTaskId)
      .setVariables(variables)
      .manualStart();

    // then
    processInstance = queryProcessInstance();

    assertNotNull(processInstance);
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertTrue(processTask.isActive());

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result.size()).isEqualTo(2);

    verifyVariables(caseInstanceId, result);

  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
      })
  @Test
  public void testManualStartWithLocalVariable() {
    // given
    String caseInstanceId = createCaseInstance(DEFINITION_KEY).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    ProcessInstance processInstance = queryProcessInstance();
    assertNull(processInstance);

    // when
    caseService
      .withCaseExecution(processTaskId)
      .setVariableLocal("aVariableName", "abc")
      .setVariableLocal("anotherVariableName", 999)
      .manualStart();

    // then
    processInstance = queryProcessInstance();

    assertNotNull(processInstance);
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertTrue(processTask.isActive());

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result.size()).isEqualTo(2);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(processTaskId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if (variable.getName().equals("aVariableName")) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if (variable.getName().equals("anotherVariableName")) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }

  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn"
      })
  @Test
  public void testReenableAnEnabledProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    ProcessInstance processInstance = queryProcessInstance();
    assertNull(processInstance);
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(processTaskId);

    // when
    assertThatThrownBy(commandBuilder::reenable)
      // then
      .withFailMessage("It should not be possible to re-enable an enabled process task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskWithManualActivationAndOneHumanTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
      })
  @Test
  public void testReenableADisabledProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    ProcessInstance processInstance = queryProcessInstance();
    assertNull(processInstance);

    caseService
      .withCaseExecution(processTaskId)
      .disable();

    // when
    caseService
      .withCaseExecution(processTaskId)
      .reenable();

    // then
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertTrue(processTask.isEnabled());
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
      })
  @Test
  public void testReenableAnActiveProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(processTaskId);

    // when
    assertThatThrownBy(commandBuilder::reenable)
      // then
      .withFailMessage("It should not be possible to re-enable an active process task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskWithManualActivationAndOneHumanTaskCase.cmmn"})
  @Test
  public void testDisableAnEnabledProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    ProcessInstance processInstance = queryProcessInstance();
    assertNull(processInstance);

    // when
    caseService
      .withCaseExecution(processTaskId)
      .disable();

    // then
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertTrue(processTask.isDisabled());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskWithManualActivationAndOneHumanTaskCase.cmmn"})
  @Test
  public void testDisableADisabledProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    caseService
      .withCaseExecution(processTaskId)
      .disable();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(processTaskId);

    // when
    assertThatThrownBy(commandBuilder::disable)
      // then
      .withFailMessage("It should not be possible to disable a already disabled process task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
      })
  @Test
  public void testDisableAnActiveProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(processTaskId);

    // when
    assertThatThrownBy(commandBuilder::disable)
      // then
      .withFailMessage("It should not be possible to disable an active process task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskWithManualActivationAndOneHumanTaskCase.cmmn"})
  @Test
  public void testManualStartOfADisabledProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    caseService
      .withCaseExecution(processTaskId)
      .disable();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(processTaskId);

    // when
    assertThatThrownBy(commandBuilder::manualStart)
      // then
      .withFailMessage("It should not be possible to start a disabled process task manually.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
      })
  @Test
  public void testManualStartOfAnActiveProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(processTaskId);

    // when
    assertThatThrownBy(commandBuilder::manualStart)
      // then
      .withFailMessage("It should not be possible to start an already active process task manually.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
      })
  @Test
  public void testComplete() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(processTaskId);

    // when
    assertThatThrownBy(commandBuilder::complete)
      // then
      .withFailMessage("It should not be possible to complete a process task, while the process instance is still running.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
      })
  @Test
  public void testCompleteProcessInstanceShouldCompleteProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    String taskId = queryTask().getId();

    // when
    taskService.complete(taskId);

    // then
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertNull(processTask);

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isCompleted());

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testDisableShouldCompleteCaseInstance() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    // when

    caseService
      .withCaseExecution(processTaskId)
      .disable();

    // then
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertNull(processTask);

    // the case instance has been completed
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .completed()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isCompleted());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testCompleteAnEnabledProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(processTaskId);

    // when
    assertThatThrownBy(commandBuilder::complete)
      // then
      .withFailMessage("It should not be possible to complete an enabled process task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskWithManualActivationAndOneHumanTaskCase.cmmn"})
  @Test
  public void testCompleteADisabledProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    caseService
      .withCaseExecution(processTaskId)
      .disable();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(processTaskId);

    // when
    assertThatThrownBy(commandBuilder::complete)
      // then
      .withFailMessage("It should not be possible to complete a disabled process task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testClose() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(processTaskId);

    // when
    assertThatThrownBy(commandBuilder::close)
      // then
      .withFailMessage("It should not be possible to close a process task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
      })
  @Test
  public void testTerminate() {
    // given
    createCaseInstance(DEFINITION_KEY);
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertTrue(processTask.isActive());
    // when
    caseService
      .withCaseExecution(processTask.getId())
      .terminate();
    
    processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertNull(processTask);

  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
      })
  @Test
  public void testTerminateNonFluent() {
    // given
    createCaseInstance(DEFINITION_KEY);
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertTrue(processTask.isActive());

    // when
    caseService
      .terminateCaseExecution(processTask.getId());
    
    processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertNull(processTask);

  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
      })
  @Test
  public void testTerminateNonActiveProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertTrue(processTask.isEnabled());

    String taskId = processTask.getId();

    // when
    assertThatThrownBy(() -> caseService.terminateCaseExecution(taskId))
      // then
      .withFailMessage("It should not be possible to terminate a task.")
      .isInstanceOf(NotAllowedException.class)
      .hasMessageContaining("The case execution must be in state 'active' to terminate");
  }

  protected CaseInstance createCaseInstance(String caseDefinitionKey) {
    return caseService
        .withCaseDefinitionByKey(caseDefinitionKey)
        .create();
  }

  protected CaseExecution queryCaseExecutionByActivityId(String activityId) {
    return caseService
        .createCaseExecutionQuery()
        .activityId(activityId)
        .singleResult();
  }

  protected ProcessInstance queryProcessInstance() {
    return runtimeService
        .createProcessInstanceQuery()
        .singleResult();
  }

  protected Task queryTask() {
    return taskService
        .createTaskQuery()
        .singleResult();
  }

}
