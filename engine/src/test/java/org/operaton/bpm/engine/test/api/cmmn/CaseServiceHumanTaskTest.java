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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.runtime.*;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
public class CaseServiceHumanTaskTest extends PluggableProcessEngineTest {

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testManualStart() {
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
    assertTrue(caseExecution.isActive());
    // ... and not enabled
    assertFalse(caseExecution.isEnabled());

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertNotNull(task);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testManualStartWithVariable() {
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
    assertTrue(caseExecution.isActive());
    // ... and not enabled
    assertFalse(caseExecution.isEnabled());

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertNotNull(task);

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testManualStartWithVariables() {
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
    assertTrue(caseExecution.isActive());
    // ... and not enabled
    assertFalse(caseExecution.isEnabled());

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertNotNull(task);

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testStart() {
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
    assertTrue(caseExecution.isActive());
    // ... and not enabled
    assertFalse(caseExecution.isEnabled());

    // there exists a task
    Task task = taskService
      .createTaskQuery()
      .caseExecutionId(caseExecutionId)
      .singleResult();

    assertNotNull(task);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testStartWithVariable() {
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
    assertTrue(caseExecution.isActive());
    // ... and not enabled
    assertFalse(caseExecution.isEnabled());

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertNotNull(task);

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testStartWithVariables() {
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
    assertTrue(caseExecution.isActive());
    // ... and not enabled
    assertFalse(caseExecution.isEnabled());

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertNotNull(task);

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testManualStartWithLocalVariable() {
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
    assertTrue(caseExecution.isActive());
    // ... and not enabled
    assertFalse(caseExecution.isEnabled());

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertNotNull(task);

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testManualStartWithLocalVariables() {
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
    assertTrue(caseExecution.isActive());
    // ... and not enabled
    assertFalse(caseExecution.isEnabled());

    // there exists a task
    Task task = taskService
        .createTaskQuery()
        .caseExecutionId(caseExecutionId)
        .singleResult();

    assertNotNull(task);

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testReenableAnEnabledHumanTask() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testReenableAnDisabledHumanTask() {
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
    assertFalse(caseExecution.isDisabled());
    assertFalse(caseExecution.isActive());
    assertTrue(caseExecution.isEnabled());

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testReenableAnActiveHumanTask() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testDisableAnEnabledHumanTask() {
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
    assertTrue(caseExecution.isDisabled());
    assertFalse(caseExecution.isActive());
    assertFalse(caseExecution.isEnabled());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testDisableADisabledHumanTask() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testDisableAnActiveHumanTask() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testManualStartOfADisabledHumanTask() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testManualStartOfAnActiveHumanTask() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testComplete() {
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

    assertNotNull(task);

    // when

    caseService
      .withCaseExecution(caseExecutionId)
      .complete();

    // then

    // the task has been completed and has been deleted
    task = taskService
        .createTaskQuery()
        .singleResult();

    assertNull(task);

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertNull(caseExecution);

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .active()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isActive());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCompleteShouldCompleteCaseInstance() {
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

    assertNotNull(task);

    // when

    caseService
      .withCaseExecution(caseExecutionId)
      .complete();

    // then

    // the task has been completed and has been deleted
    task = taskService
        .createTaskQuery()
        .singleResult();

    assertNull(task);

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertNull(caseExecution);

    // the case instance has been completed
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .completed()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isCompleted());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testCompleteShouldCompleteCaseInstanceViaTaskService() {
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

    assertNotNull(task);

    // when

    taskService.complete(task.getId());

    // then

    // the task has been completed and has been deleted
    task = taskService
        .createTaskQuery()
        .singleResult();

    assertNull(task);

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertNull(caseExecution);

    // the case instance has been completed
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .completed()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isCompleted());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testDisableShouldCompleteCaseInstance() {
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

    assertNull(caseExecution);

    // the case instance has been completed
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .completed()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isCompleted());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testCompleteAnEnabledHumanTask() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testCompleteADisabledHumanTask() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testCompleteWithSetVariable() {
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

    assertNotNull(task);

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

    assertNull(task);

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertNull(caseExecution);

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .active()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isActive());

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testCompleteWithSetVariableLocal() {
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

    assertNotNull(task);

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

    assertNull(task);

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertNull(caseExecution);

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .active()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isActive());

    // the variables has been set and due to the completion
    // also removed in one command
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertTrue(result.isEmpty());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testCompleteWithRemoveVariable() {
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

    assertNotNull(task);

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

    assertNull(task);

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertNull(caseExecution);

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .active()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isActive());

    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertTrue(result.isEmpty());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testCompleteWithRemoveVariableLocal() {
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

    assertNotNull(task);

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

    assertNull(task);

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertNull(caseExecution);

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .active()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isActive());

    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertTrue(result.isEmpty());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testClose() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testTerminate() {
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
    assertNull(taskExecution);
    
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testTerminateNonFluent() {
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
    assertNull(taskExecution);
    
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testTerminateNonActiveHumanTask() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testManualStartNonFluent() {
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

    // then

    // the child case execution is active...
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    assertTrue(caseExecution.isActive());
    // ... and not enabled
    assertFalse(caseExecution.isEnabled());

    // there exists a task
    Task task = taskService
      .createTaskQuery()
      .caseExecutionId(caseExecutionId)
      .singleResult();

    assertNotNull(task);
  }


  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testManualStartWithVariablesNonFluent() {
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
    assertTrue(caseExecution.isActive());
    // ... and not enabled
    assertFalse(caseExecution.isEnabled());

    // there exists a task
    Task task = taskService
      .createTaskQuery()
      .caseExecutionId(caseExecutionId)
      .singleResult();

    assertNotNull(task);

    // there is a variable set on the case instance
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();

    assertNotNull(variable);
    assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(variable.getName()).isEqualTo("aVariable");
    assertThat(variable.getValue()).isEqualTo("aValue");

  }


  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testDisableNonFluent() {
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
    assertTrue(caseExecution.isDisabled());
    assertFalse(caseExecution.isActive());
    assertFalse(caseExecution.isEnabled());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testDisableNonFluentWithVariables() {
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
    assertTrue(caseExecution.isDisabled());
    assertFalse(caseExecution.isActive());
    assertFalse(caseExecution.isEnabled());

    // there is a variable set on the case instance
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();

    assertNotNull(variable);
    assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(variable.getName()).isEqualTo("aVariable");
    assertThat(variable.getValue()).isEqualTo("aValue");
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testReenableNonFluent() {
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
    assertFalse(caseExecution.isDisabled());
    assertFalse(caseExecution.isActive());
    assertTrue(caseExecution.isEnabled());

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testReenableNonFluentWithVariables() {
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
    assertFalse(caseExecution.isDisabled());
    assertFalse(caseExecution.isActive());
    assertTrue(caseExecution.isEnabled());

    // there is a variable set on the case instance
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();

    assertNotNull(variable);
    assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
    assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(variable.getName()).isEqualTo("aVariable");
    assertThat(variable.getValue()).isEqualTo("aValue");

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testCompleteNonFluent() {
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

    assertNotNull(task);

    // when

    caseService.completeCaseExecution(caseExecutionId);

    // then

    // the task has been completed and has been deleted
    task = taskService
        .createTaskQuery()
        .singleResult();

    assertNull(task);

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertNull(caseExecution);

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .active()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isActive());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"})
  @Test
  public void testCompleteWithVariablesNonFluent() {
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

    assertNotNull(task);

    // when
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "aValue");

    caseService.completeCaseExecution(caseExecutionId, variables);

    // then

    // the task has been completed and has been deleted
    assertNull(taskService.createTaskQuery().singleResult());

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertNull(caseExecution);

    // there is a variable set on the case instance
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();

    assertNotNull(variable);
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
