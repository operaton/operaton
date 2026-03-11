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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionCommandBuilder;
import org.operaton.bpm.engine.runtime.CaseExecutionQuery;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Roman Smirnov
 *
 */
@ExtendWith(ProcessEngineExtension.class)
@ExtendWith(ProcessEngineTestExtension.class)
class CaseServiceHumanTaskTest {

  protected RepositoryService repositoryService;
  protected TaskService taskService;
  protected RuntimeService runtimeService;
  protected CaseService caseService;

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testManualStart() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
        .withCaseDefinition(caseDefinitionId)
        .create();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    // an enabled child case execution of
    // the case instance
    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    // activate child case execution
    caseService
        .withCaseExecution(caseExecutionId)
        .manualStart();

    // then

    // the child case execution is active...
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    assertThat(caseExecution.isActive()).isTrue();
    // ... and not enabled
    assertThat(caseExecution.isEnabled()).isFalse();

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertThat(task).isNotNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testManualStartWithVariable() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create()
        .getId();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    // an enabled child case execution of
    // the case instance
    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    // activate child case execution
    caseService
        .withCaseExecution(caseExecutionId)
        .setVariable("aVariableName", "abc")
        .setVariable("anotherVariableName", 999)
        .manualStart();

    // then

    // the child case execution is active...
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    assertThat(caseExecution.isActive()).isTrue();
    // ... and not enabled
    assertThat(caseExecution.isEnabled()).isFalse();

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertThat(task).isNotNull();

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

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testManualStartWithVariables() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create()
        .getId();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    // an enabled child case execution of
    // the case instance
    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 999);

    // when
    // activate child case execution
    caseService
        .withCaseExecution(caseExecutionId)
        .setVariables(variables)
        .manualStart();

    // then

    // the child case execution is active...
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    assertThat(caseExecution.isActive()).isTrue();
    // ... and not enabled
    assertThat(caseExecution.isEnabled()).isFalse();

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertThat(task).isNotNull();

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

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testStart() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
      .withCaseDefinition(caseDefinitionId)
      .create();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    // an enabled child case execution of
    // the case instance
    String caseExecutionId = caseExecutionQuery
      .activityId("PI_HumanTask_1")
      .singleResult()
      .getId();

    // when
    // activate child case execution

    // then

    // the child case execution is active...
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    assertThat(caseExecution.isActive()).isTrue();
    // ... and not enabled
    assertThat(caseExecution.isEnabled()).isFalse();

    // there exists a task
    Task task = taskService
      .createTaskQuery()
      .caseExecutionId(caseExecutionId)
      .singleResult();

    assertThat(task).isNotNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testStartWithVariable() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    VariableMap variableMap = Variables.createVariables().putValue("aVariableName", "abc").putValue("anotherVariableName", 999);
    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariables(variableMap)
        .create()
        .getId();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    // an enabled child case execution of
    // the case instance
    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    // activate child case execution

    // then

    // the child case execution is active...
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    assertThat(caseExecution.isActive()).isTrue();
    // ... and not enabled
    assertThat(caseExecution.isEnabled()).isFalse();

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertThat(task).isNotNull();

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

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testStartWithVariables() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 999);

    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariables(variables)
        .create()
        .getId();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    // an enabled child case execution of
    // the case instance
    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // then

    // the child case execution is active...
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    assertThat(caseExecution.isActive()).isTrue();
    // ... and not enabled
    assertThat(caseExecution.isEnabled()).isFalse();

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertThat(task).isNotNull();

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

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testManualStartWithLocalVariable() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create()
        .getId();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    // an enabled child case execution of
    // the case instance
    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    // activate child case execution
    caseService
      .withCaseExecution(caseExecutionId)
      .setVariableLocal("aVariableName", "abc")
      .setVariableLocal("anotherVariableName", 999)
      .manualStart();

    // then

    // the child case execution is active...
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    assertThat(caseExecution.isActive()).isTrue();
    // ... and not enabled
    assertThat(caseExecution.isEnabled()).isFalse();

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertThat(task).isNotNull();

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

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testManualStartWithLocalVariables() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create()
        .getId();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    // an enabled child case execution of
    // the case instance
    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 999);

    // when
    // activate child case execution
    caseService
      .withCaseExecution(caseExecutionId)
      .setVariablesLocal(variables)
      .manualStart();

    // then

    // the child case execution is active...
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    assertThat(caseExecution.isActive()).isTrue();
    // ... and not enabled
    assertThat(caseExecution.isEnabled()).isFalse();

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertThat(task).isNotNull();

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

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testReenableAnEnabledHumanTask() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance

    caseService
        .withCaseDefinition(caseDefinitionId)
        .create();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::reenable)
        .withFailMessage("It should not be possible to re-enable an enabled human task.")
        .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testReenableAnDisabledHumanTask() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
        .withCaseDefinition(caseDefinitionId)
        .create();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // the human task is disabled
    caseService
      .withCaseExecution(caseExecutionId)
      .disable();

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .reenable();

    // then
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    // the human task is disabled
    assertThat(caseExecution.isDisabled()).isFalse();
    assertThat(caseExecution.isActive()).isFalse();
    assertThat(caseExecution.isEnabled()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testReenableAnActiveHumanTask() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
        .withCaseDefinition(caseDefinitionId)
        .create();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    assertThatThrownBy(commandBuilder::reenable)
        .withFailMessage("It should not be possible to re-enable an active human task.")
        .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testDisableAnEnabledHumanTask() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance and the containing
    // human task is enabled
    caseService
        .withCaseDefinition(caseDefinitionId)
        .create();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .disable();

    // then
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    // the human task is disabled
    assertThat(caseExecution.isDisabled()).isTrue();
    assertThat(caseExecution.isActive()).isFalse();
    assertThat(caseExecution.isEnabled()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testDisableADisabledHumanTask() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
        .withCaseDefinition(caseDefinitionId)
        .create();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // the human task is disabled
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);
    commandBuilder.disable();

    // when
    assertThatThrownBy(commandBuilder::disable)
        // then
        .withFailMessage("It should not be possible to disable a already disabled human task.")
        .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testDisableAnActiveHumanTask() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
        .withCaseDefinition(caseDefinitionId)
        .create();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::disable)
        // then
        .withFailMessage("It should not be possible to disable an active human task.")
        .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testManualStartOfADisabledHumanTask() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
        .withCaseDefinition(caseDefinitionId)
        .create();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .disable();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::manualStart)
        // then
        .withFailMessage("It should not be possible to start a disabled human task manually.")
        .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testManualStartOfAnActiveHumanTask() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
        .withCaseDefinition(caseDefinitionId)
        .create();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::manualStart)
        // then
        .withFailMessage("It should not be possible to start an already active human task manually.")
        .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testComplete() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .manualStart();

    Task task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNotNull();

    // when

    caseService
      .withCaseExecution(caseExecutionId)
      .complete();

    // then

    // the task has been completed and has been deleted
    task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNull();

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(caseExecution).isNull();

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .active()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCompleteShouldCompleteCaseInstance() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    Task task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNotNull();

    // when

    caseService
      .withCaseExecution(caseExecutionId)
      .complete();

    // then

    // the task has been completed and has been deleted
    task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNull();

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(caseExecution).isNull();

    // the case instance has been completed
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .completed()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testCompleteShouldCompleteCaseInstanceViaTaskService() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .manualStart();

    Task task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNotNull();

    // when

    taskService.complete(task.getId());

    // then

    // the task has been completed and has been deleted
    task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNull();

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(caseExecution).isNull();

    // the case instance has been completed
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .completed()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testDisableShouldCompleteCaseInstance() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when

    caseService
      .withCaseExecution(caseExecutionId)
      .disable();

    // then

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(caseExecution).isNull();

    // the case instance has been completed
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .completed()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testCompleteAnEnabledHumanTask() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::complete)
        // then
        .withFailMessage("Should not be able to complete task.")
        .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testCompleteADisabledHumanTask() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .disable();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::complete)
        // then
        .withFailMessage("Should not be able to complete task.")
        .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testCompleteWithSetVariable() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .manualStart();

    Task task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNotNull();

    // when

    caseService
      .withCaseExecution(caseExecutionId)
      .setVariable("aVariableName", "abc")
      .setVariable("anotherVariableName", 999)
      .complete();

    // then

    // the task has been completed and has been deleted
    task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNull();

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(caseExecution).isNull();

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .active()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();

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

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testCompleteWithSetVariableLocal() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .manualStart();

    Task task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNotNull();

    // when

    caseService
      .withCaseExecution(caseExecutionId)
      .setVariableLocal("aVariableName", "abc")
      .setVariableLocal("anotherVariableName", 999)
      .complete();

    // then

    // the task has been completed and has been deleted
    task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNull();

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(caseExecution).isNull();

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .active()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();

    // the variables has been set and due to the completion
    // also removed in one command
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertThat(result).isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testCompleteWithRemoveVariable() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .setVariable("aVariableName", "abc")
      .setVariable("anotherVariableName", 999)
      .manualStart();

    Task task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNotNull();

    // when

    caseService
      .withCaseExecution(caseExecutionId)
      .removeVariable("aVariableName")
      .removeVariable("anotherVariableName")
      .complete();

    // then

    // the task has been completed and has been deleted
    task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNull();

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(caseExecution).isNull();

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .active()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();

    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertThat(result).isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testCompleteWithRemoveVariableLocal() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .setVariableLocal("aVariableName", "abc")
      .setVariableLocal("anotherVariableName", 999)
      .manualStart();

    Task task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNotNull();

    // when

    caseService
      .withCaseExecution(caseExecutionId)
      .removeVariableLocal("aVariableName")
      .removeVariableLocal("anotherVariableName")
      .complete();

    // then

    // the task has been completed and has been deleted
    task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNull();

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(caseExecution).isNull();

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .active()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();

    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertThat(result).isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testClose() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::close)
        // then
        .withFailMessage("It should not be possible to close a task.")
        .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testTerminate() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
      .createCaseDefinitionQuery()
      .singleResult()
      .getId();

    caseService
      .withCaseDefinition(caseDefinitionId)
      .create()
      .getId();

    CaseExecution taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");

    // when
    caseService.withCaseExecution(taskExecution.getId())
      .terminate();

    // then
    taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertThat(taskExecution).isNull();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testTerminateNonFluent() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    CaseExecution taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");

    // when
    caseService.terminateCaseExecution(taskExecution.getId());

    // then
    taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertThat(taskExecution).isNull();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testTerminateNonActiveHumanTask() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    CaseExecution taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");

    String taskExecutionId = taskExecution.getId();

    // when
    assertThatThrownBy(() -> caseService.terminateCaseExecution(taskExecutionId))
        // then
        .withFailMessage("It should not be possible to terminate a task.")
        .isInstanceOf(NotAllowedException.class)
        .hasMessageContaining("The case execution must be in state 'active' to terminate");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testManualStartWithVariablesNonFluent() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
      .withCaseDefinition(caseDefinitionId)
      .create().getId();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    // an enabled child case execution of
    // the case instance
    String caseExecutionId = caseExecutionQuery
      .activityId("PI_HumanTask_1")
      .singleResult()
      .getId();

    // when
    // activate child case execution
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "aValue");
    caseService.manuallyStartCaseExecution(caseExecutionId, variables);

    // then

    // the child case execution is active...
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    assertThat(caseExecution.isActive()).isTrue();
    // ... and not enabled
    assertThat(caseExecution.isEnabled()).isFalse();

    // there exists a task
    Task task = taskService
      .createTaskQuery()
      .caseExecutionId(caseExecutionId)
      .singleResult();

    assertThat(task).isNotNull();

    // there is a variable set on the case instance
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();

    assertThat(variable).isNotNull();
    assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(variable.getName()).isEqualTo("aVariable");
    assertThat(variable.getValue()).isEqualTo("aValue");

  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testDisableNonFluent() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance and the containing
    // human task is enabled
    caseService
        .withCaseDefinition(caseDefinitionId)
        .create();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService.disableCaseExecution(caseExecutionId);

    // then
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    // the human task is disabled
    assertThat(caseExecution.isDisabled()).isTrue();
    assertThat(caseExecution.isActive()).isFalse();
    assertThat(caseExecution.isEnabled()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testDisableNonFluentWithVariables() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance and the containing
    // human task is enabled
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create().getId();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "aValue");
    caseService.disableCaseExecution(caseExecutionId, variables);

    // then
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    // the human task is disabled
    assertThat(caseExecution.isDisabled()).isTrue();
    assertThat(caseExecution.isActive()).isFalse();
    assertThat(caseExecution.isEnabled()).isFalse();

    // there is a variable set on the case instance
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();

    assertThat(variable).isNotNull();
    assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(variable.getName()).isEqualTo("aVariable");
    assertThat(variable.getValue()).isEqualTo("aValue");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testReenableNonFluent() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
        .withCaseDefinition(caseDefinitionId)
        .create();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // the human task is disabled
    caseService
      .withCaseExecution(caseExecutionId)
      .disable();

    // when
    caseService.reenableCaseExecution(caseExecutionId);

    // then
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    // the human task is disabled
    assertThat(caseExecution.isDisabled()).isFalse();
    assertThat(caseExecution.isActive()).isFalse();
    assertThat(caseExecution.isEnabled()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testReenableNonFluentWithVariables() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create().getId();

    CaseExecutionQuery caseExecutionQuery = caseService.createCaseExecutionQuery();

    String caseExecutionId = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // the human task is disabled
    caseService
      .withCaseExecution(caseExecutionId)
      .disable();

    // when
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "aValue");
    caseService.reenableCaseExecution(caseExecutionId, variables);

    // then
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    // the human task is disabled
    assertThat(caseExecution.isDisabled()).isFalse();
    assertThat(caseExecution.isActive()).isFalse();
    assertThat(caseExecution.isEnabled()).isTrue();

    // there is a variable set on the case instance
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();

    assertThat(variable).isNotNull();
    assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(variable.getName()).isEqualTo("aVariable");
    assertThat(variable.getValue()).isEqualTo("aValue");

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testCompleteNonFluent() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .manualStart();

    Task task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNotNull();

    // when

    caseService.completeCaseExecution(caseExecutionId);

    // then

    // the task has been completed and has been deleted
    task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNull();

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(caseExecution).isNull();

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .active()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  void testCompleteWithVariablesNonFluent() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .manualStart();

    Task task = taskService
        .createTaskQuery()
        .singleResult();

    assertThat(task).isNotNull();

    // when
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "aValue");

    caseService.completeCaseExecution(caseExecutionId, variables);

    // then

    // the task has been completed and has been deleted
    assertThat(taskService.createTaskQuery().singleResult()).isNull();

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(caseExecution).isNull();

    // there is a variable set on the case instance
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();

    assertThat(variable).isNotNull();
    assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(variable.getName()).isEqualTo("aVariable");
    assertThat(variable.getValue()).isEqualTo("aValue");

  }

  protected CaseExecution queryCaseExecutionByActivityId(String activityId) {
    return caseService
      .createCaseExecutionQuery()
      .activityId(activityId)
      .singleResult();
  }
}
