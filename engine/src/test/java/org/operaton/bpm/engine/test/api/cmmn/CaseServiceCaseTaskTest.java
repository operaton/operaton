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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionCommandBuilder;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.engine.variable.Variables;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
public class CaseServiceCaseTaskTest extends PluggableProcessEngineTest {

  protected static final String DEFINITION_KEY = "oneCaseTaskCase";
  protected static final String DEFINITION_KEY_2 = "oneTaskCase";
  protected static final String CASE_TASK_KEY = "PI_CaseTask_1";


  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  public void testManualStart() {
    // given
    createCaseInstance(DEFINITION_KEY).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();

    CaseInstance subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNull(subCaseInstance);

    // when
    caseService
        .withCaseExecution(caseTaskId)
        .manualStart();

    // then
    subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNotNull(subCaseInstance);
    assertTrue(subCaseInstance.isActive());

    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertTrue(caseTask.isActive());
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  public void testManualStartWithVariable() {
    // given
    String superCaseInstanceId = createCaseInstance(DEFINITION_KEY).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();

    CaseInstance subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNull(subCaseInstance);

    // when
    caseService
        .withCaseExecution(caseTaskId)
        .setVariable("aVariableName", "abc")
        .setVariable("anotherVariableName", 999)
        .manualStart();

    // then
    subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNotNull(subCaseInstance);
    assertTrue(subCaseInstance.isActive());

    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertTrue(caseTask.isActive());

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    verifyVariables(superCaseInstanceId, result);

  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  public void testManualStartWithVariables() {
    // given
    String superCaseInstanceId = createCaseInstance(DEFINITION_KEY).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();

    CaseInstance subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNull(subCaseInstance);

    // variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 999);

    // when
    caseService
        .withCaseExecution(caseTaskId)
        .setVariables(variables)
        .manualStart();

    // then
    subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNotNull(subCaseInstance);
    assertTrue(subCaseInstance.isActive());

    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertTrue(caseTask.isActive());

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    verifyVariables(superCaseInstanceId, result);

  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testStart() {
    // given
    createCaseInstance(DEFINITION_KEY).getId();
    queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();

    CaseInstance subCaseInstance;

    // then
    subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNotNull(subCaseInstance);
    assertTrue(subCaseInstance.isActive());

    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertTrue(caseTask.isActive());
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testStartWithVariable() {
    // given
    String superCaseInstanceId = createCaseInstance(DEFINITION_KEY,
        Variables.createVariables()
            .putValue("aVariableName", "abc")
            .putValue("anotherVariableName", 999)).getId();

    CaseInstance subCaseInstance;

    // then
    subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNotNull(subCaseInstance);
    assertTrue(subCaseInstance.isActive());

    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertTrue(caseTask.isActive());

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    verifyVariables(superCaseInstanceId, result);

  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testStartWithVariables() {
    // given
    // variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 999);

    String superCaseInstanceId = createCaseInstance(DEFINITION_KEY, variables).getId();

    CaseInstance subCaseInstance;

    // then
    subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNotNull(subCaseInstance);
    assertTrue(subCaseInstance.isActive());

    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertTrue(caseTask.isActive());

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    verifyVariables(superCaseInstanceId, result);

  }

  protected void verifyVariables(String superCaseInstanceId, List<VariableInstance> result) {
    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(superCaseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(superCaseInstanceId);

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
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testManualStartWithLocalVariable() {
    // given
    String superCaseInstanceId = createCaseInstance(DEFINITION_KEY).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();

    CaseInstance subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNull(subCaseInstance);

    // when
    caseService
      .withCaseExecution(caseTaskId)
      .setVariableLocal("aVariableName", "abc")
      .setVariableLocal("anotherVariableName", 999)
      .manualStart();

    // then
    subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNotNull(subCaseInstance);
    assertTrue(subCaseInstance.isActive());

    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertTrue(caseTask.isActive());

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseTaskId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(superCaseInstanceId);

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
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testManualStartWithLocalVariables() {
    // given
    String superCaseInstanceId = createCaseInstance(DEFINITION_KEY).getId();
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();

    CaseInstance subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNull(subCaseInstance);

    // variables
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 999);

    // when
    // activate child case execution
    caseService
      .withCaseExecution(caseTaskId)
      .setVariablesLocal(variables)
      .manualStart();

    // then
    subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNotNull(subCaseInstance);
    assertTrue(subCaseInstance.isActive());

    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertTrue(caseTask.isActive());

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseTaskId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(superCaseInstanceId);

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
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCaseWithManualActivation.cmmn"
      })
  @Test
  public void testReenableAnEnabledCaseTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();

    CaseInstance subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNull(subCaseInstance);
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);

    // when
    assertThatThrownBy(commandBuilder::reenable)
      // then
      .withFailMessage("It should not be possible to re-enable an enabled case task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskAndOneHumanTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testReenableADisabledCaseTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();

    CaseInstance subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNull(subCaseInstance);

    caseService
      .withCaseExecution(caseTaskId)
      .disable();

    // when
    caseService
      .withCaseExecution(caseTaskId)
      .reenable();

    // then
    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertTrue(caseTask.isEnabled());
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testReenableAnActiveCaseTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);

    // when
    assertThatThrownBy(commandBuilder::reenable)
      // then
      .withFailMessage("It should not be possible to re-enable an active case task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskAndOneHumanTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testDisableAnEnabledCaseTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();

    CaseInstance subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertNull(subCaseInstance);

    // when
    caseService
      .withCaseExecution(caseTaskId)
      .disable();

    // then
    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertTrue(caseTask.isDisabled());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskAndOneHumanTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testDisableADisabledCaseTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);
    commandBuilder.disable();

    // when
    assertThatThrownBy(commandBuilder::disable)
      // then
      .withFailMessage("It should not be possible to disable a already disabled case task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testDisableAnActiveCaseTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);

    // when
    assertThatThrownBy(commandBuilder::disable)
      // then
      .withFailMessage("It should not be possible to disable an active case task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskAndOneHumanTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testManualStartOfADisabledCaseTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);
    commandBuilder.disable();

    // when
    assertThatThrownBy(commandBuilder::manualStart)
      // then
      .withFailMessage("It should not be possible to start a disabled case task manually.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testManualStartOfAnActiveCaseTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);

    // when
    assertThatThrownBy(commandBuilder::manualStart)
      // then
      .withFailMessage("It should not be possible to start an already active case task manually.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testComplete() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);

    // when
    assertThatThrownBy(commandBuilder::complete)
      // then
      .withFailMessage("It should not be possible to complete a case task, while the process instance is still running.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testCloseCaseInstanceShouldCompleteCaseTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String humanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    CaseInstance subCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY_2);
    assertTrue(subCaseInstance.isCompleted());

    // when
    caseService
      .withCaseExecution(subCaseInstance.getId())
      .close();

    // then
    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertNull(caseTask);

    CaseInstance superCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY);
    assertNotNull(superCaseInstance);
    assertTrue(superCaseInstance.isCompleted());

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testDisableShouldCompleteCaseInstance() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();

    // when
    caseService
      .withCaseExecution(caseTaskId)
      .disable();

    // then
    CaseExecution caseTask = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertThat(caseTask).isNull();
    // the case instance has been completed
    CaseInstance superCaseInstance = queryCaseInstanceByKey(DEFINITION_KEY);
    assertNotNull(superCaseInstance);
    assertTrue(superCaseInstance.isCompleted());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testCompleteAnEnabledCaseTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);

    // when
    assertThatThrownBy(commandBuilder::complete)
      // then
      .withFailMessage("Should not be able to complete an enabled case task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskAndOneHumanTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testCompleteADisabledCaseTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);
    commandBuilder.disable();

    // when
    assertThatThrownBy(commandBuilder::complete)
      // then
      .withFailMessage("Should not be able to complete a disabled case task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testClose() {
    // given
    createCaseInstance(DEFINITION_KEY);
    String caseTaskId = queryCaseExecutionByActivityId(CASE_TASK_KEY).getId();
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);

    // when
    assertThatThrownBy(commandBuilder::close)
      // then
      .withFailMessage("It should not be possible to close a case task.")
      .isInstanceOf(NotAllowedException.class);

  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testTerminate() {
    // given
    createCaseInstance(DEFINITION_KEY);
    CaseExecution caseTaskExecution = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    // when
    caseService
      .withCaseExecution(caseTaskExecution.getId())
      .terminate();

    caseTaskExecution = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertNull(caseTaskExecution);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testTerminateNonFluent() {
    // given
    createCaseInstance(DEFINITION_KEY);
    CaseExecution caseTaskExecution = queryCaseExecutionByActivityId(CASE_TASK_KEY);

    // when
    caseService
      .terminateCaseExecution(caseTaskExecution.getId());

    caseTaskExecution = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    assertNull(caseTaskExecution);
  }

  @Deployment(resources={
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCaseWithManualActivation.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
      })
  @Test
  public void testTerminateNonActiveCaseTask() {
    // given
    createCaseInstance(DEFINITION_KEY);
    CaseExecution caseTaskExecution = queryCaseExecutionByActivityId(CASE_TASK_KEY);
    String caseTaskExecutionId = caseTaskExecution.getId();

    // when
    assertThatThrownBy(() -> caseService.terminateCaseExecution(caseTaskExecutionId))
      // then
      .withFailMessage("It should not be possible to terminate a case task.")
      .isInstanceOf(NotAllowedException.class)
      .hasMessageContaining("The case execution must be in state 'active' to terminate");
  }
  
  protected CaseInstance createCaseInstance(String caseDefinitionKey) {
    return caseService
        .withCaseDefinitionByKey(caseDefinitionKey)
        .create();
  }

  protected CaseInstance createCaseInstance(String caseDefinitionKey, Map<String, Object> variables) {
    return caseService
        .withCaseDefinitionByKey(caseDefinitionKey)
        .setVariables(variables)
        .create();
  }

  protected CaseExecution queryCaseExecutionByActivityId(String activityId) {
    return caseService
        .createCaseExecutionQuery()
        .activityId(activityId)
        .singleResult();
  }

  protected CaseInstance queryCaseInstanceByKey(String caseDefinitionKey) {
    return caseService
        .createCaseInstanceQuery()
        .caseDefinitionKey(caseDefinitionKey)
        .singleResult();
  }

}
