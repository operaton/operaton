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
package org.operaton.bpm.engine.test.api.cmmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionCommandBuilder;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Roman Smirnov
 *
 */
@ExtendWith(ProcessEngineExtension.class)
@ExtendWith(ProcessEngineTestExtension.class)
class CaseServiceProcessTaskTest {

  static final String DEFINITION_KEY = "oneProcessTaskCase";
  static final String PROCESS_TASK_KEY = "PI_ProcessTask_1";

  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected CaseService caseService;

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testStart() {
    // given
    String caseInstanceId = createCaseInstance(DEFINITION_KEY).getId();

    ProcessInstance processInstance;

    // then
    processInstance = queryProcessInstance();

    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertThat(processTask.isActive()).isTrue();
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

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testManualStart() {
    // given
    String caseInstanceId = createCaseInstance(DEFINITION_KEY).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNull();

    // when
    caseService
      .withCaseExecution(processTaskId)
      .manualStart();

    // then
    processInstance = queryProcessInstance();

    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertThat(processTask.isActive()).isTrue();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testManualStartWithVariables() {
    // given
    String caseInstanceId = createCaseInstance(DEFINITION_KEY).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNull();

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

    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertThat(processTask.isActive()).isTrue();

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    verifyVariables(caseInstanceId, result);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testManualStartWithLocalVariable() {
    // given
    String caseInstanceId = createCaseInstance(DEFINITION_KEY).getId();
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNull();

    // when
    caseService
      .withCaseExecution(processTaskId)
      .setVariableLocal("aVariableName", "abc")
      .setVariableLocal("anotherVariableName", 999)
      .manualStart();

    // then
    processInstance = queryProcessInstance();

    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getCaseInstanceId()).isEqualTo(caseInstanceId);

    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertThat(processTask.isActive()).isTrue();

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

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

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn"
  })
  @Test
  void testReenableAnEnabledProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNull();
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(processTaskId);

    // when
    assertThatThrownBy(commandBuilder::reenable)
      // then
      .withFailMessage("It should not be possible to re-enable an enabled process task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskWithManualActivationAndOneHumanTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testReenableADisabledProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNull();

    caseService
      .withCaseExecution(processTaskId)
      .disable();

    // when
    caseService
      .withCaseExecution(processTaskId)
      .reenable();

    // then
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertThat(processTask.isEnabled()).isTrue();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testReenableAnActiveProcessTask() {
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

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskWithManualActivationAndOneHumanTaskCase.cmmn"})
  @Test
  void testDisableAnEnabledProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    ProcessInstance processInstance = queryProcessInstance();
    assertThat(processInstance).isNull();

    // when
    caseService
      .withCaseExecution(processTaskId)
      .disable();

    // then
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertThat(processTask.isDisabled()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskWithManualActivationAndOneHumanTaskCase.cmmn"})
  @Test
  void testDisableADisabledProcessTask() {
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

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testDisableAnActiveProcessTask() {
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

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskWithManualActivationAndOneHumanTaskCase.cmmn"})
  @Test
  void testManualStartOfADisabledProcessTask() {
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

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testManualStartOfAnActiveProcessTask() {
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

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testComplete() {
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

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testCompleteProcessInstanceShouldCompleteProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();
    assertThat(processTaskId).isNotNull();

    String taskId = queryTask().getId();

    // when
    taskService.complete(taskId);

    // then
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertThat(processTask).isNull();

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn"})
  @Test
  void testDisableShouldCompleteCaseInstance() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String processTaskId = queryCaseExecutionByActivityId(PROCESS_TASK_KEY).getId();

    // when

    caseService
      .withCaseExecution(processTaskId)
      .disable();

    // then
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertThat(processTask).isNull();

    // the case instance has been completed
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .completed()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn"})
  @Test
  void testCompleteAnEnabledProcessTask() {
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

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskWithManualActivationAndOneHumanTaskCase.cmmn"})
  @Test
  void testCompleteADisabledProcessTask() {
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

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn"})
  @Test
  void testClose() {
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

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testTerminate() {
    // given
    createCaseInstance(DEFINITION_KEY);
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertThat(processTask.isActive()).isTrue();
    // when
    caseService
      .withCaseExecution(processTask.getId())
      .terminate();

    processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertThat(processTask).isNull();

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testTerminateNonFluent() {
    // given
    createCaseInstance(DEFINITION_KEY);
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertThat(processTask.isActive()).isTrue();

    // when
    caseService
      .terminateCaseExecution(processTask.getId());

    processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertThat(processTask).isNull();

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml"
  })
  @Test
  void testTerminateNonActiveProcessTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    CaseExecution processTask = queryCaseExecutionByActivityId(PROCESS_TASK_KEY);
    assertThat(processTask.isEnabled()).isTrue();

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
