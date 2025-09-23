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
import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionCommandBuilder;
import org.operaton.bpm.engine.runtime.CaseExecutionQuery;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Roman Smirnov
 *
 */
@ExtendWith(ProcessEngineExtension.class)
@ExtendWith(ProcessEngineTestExtension.class)
class CaseServiceStageTest {

  protected CaseService caseService;
  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
  @Test
  void testStartAutomated() {
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
        .activityId("PI_Stage_1")
        .singleResult()
        .getId();
    assertThat(caseExecutionId).isNotNull();

    // then

    // the child case execution is active...
    CaseExecution caseExecution = caseExecutionQuery.singleResult();
    assertThat(caseExecution.isActive()).isTrue();
    // ... and not enabled
    assertThat(caseExecution.isEnabled()).isFalse();

    // there exists two new case execution:
    verifyTasksState(caseExecutionQuery);


  }

  protected void verifyTasksState(CaseExecutionQuery caseExecutionQuery) {
    // (1) one case case execution representing "PI_HumanTask_1"
    CaseExecution firstHumanTask = caseExecutionQuery
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(firstHumanTask).isNotNull();
    assertThat(firstHumanTask.isActive()).isTrue();
    assertThat(firstHumanTask.isEnabled()).isFalse();

    // (2) one case case execution representing "PI_HumanTask_2"
    CaseExecution secondHumanTask = caseExecutionQuery
        .activityId("PI_HumanTask_2")
        .singleResult();

    assertThat(secondHumanTask).isNotNull();
    assertThat(secondHumanTask.isActive()).isTrue();
    assertThat(secondHumanTask.isEnabled()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCaseWithManualActivation.cmmn"})
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
      .activityId("PI_Stage_1")
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

    // there exists two new case execution:

    // (1) one case case execution representing "PI_HumanTask_1"
    verifyTasksState(caseExecutionQuery);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCaseWithManualActivation.cmmn"})
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
        .activityId("PI_Stage_1")
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

    // (1) one case case execution representing "PI_HumanTask_1"
    verifyTasksState(caseExecutionQuery);

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    verifyVariables(caseInstanceId, caseInstanceId, result);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCaseWithManualActivation.cmmn"})
  @Test
  void testManualWithVariables() {
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
        .activityId("PI_Stage_1")
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

    // (1) one case case execution representing "PI_HumanTask_1"
    verifyTasksState(caseExecutionQuery);

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    verifyVariables(caseInstanceId, caseInstanceId, result);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCaseWithManualActivation.cmmn"})
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
        .activityId("PI_Stage_1")
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

    // (1) one case case execution representing "PI_HumanTask_1"
    verifyTasksState(caseExecutionQuery);

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    verifyVariables(caseInstanceId, caseExecutionId, result);

  }

  protected void verifyVariables(String caseInstanceId, String caseExecutionId, List<VariableInstance> result) {
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

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCaseWithManualActivation.cmmn"})
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
        .activityId("PI_Stage_1")
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

    // (1) one case case execution representing "PI_HumanTask_1"
    verifyTasksState(caseExecutionQuery);

    // the case instance has two variables:
    // - aVariableName
    // - anotherVariableName
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .list();

    verifyVariables(caseInstanceId, caseExecutionId, result);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
  @Test
  void testReenableAnEnabledStage() {
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
        .activityId("PI_Stage_1")
        .singleResult()
        .getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::reenable)
      // then
      .withFailMessage("It should not be possible to re-enable an enabled stage.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskAndOneStageWithManualActivationCase.cmmn"})
  @Test
  void testReenableAnDisabledStage() {
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
        .activityId("PI_Stage_1")
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

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
  @Test
  void testReenableAnActiveStage() {
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
        .activityId("PI_Stage_1")
        .singleResult()
        .getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::reenable)
      // then
      .withFailMessage("It should not be possible to re-enable an active human task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskAndOneStageWithManualActivationCase.cmmn"})
  @Test
  void testDisableAnEnabledStage() {
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
        .activityId("PI_Stage_1")
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

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskAndOneStageWithManualActivationCase.cmmn"})
  @Test
  void testDisableADisabledStage() {
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
        .activityId("PI_Stage_1")
        .singleResult()
        .getId();

    // the human task is disabled
    caseService
      .withCaseExecution(caseExecutionId)
      .disable();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::disable)
      // then
        .withFailMessage("It should not be possible to disable a already disabled human task.")
        .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
  @Test
  void testDisableAnActiveStage() {
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
        .activityId("PI_Stage_1")
        .singleResult()
        .getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::disable)
      // then
      .withFailMessage("It should not be possible to disable an active human task.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskAndOneStageWithManualActivationCase.cmmn"})
  @Test
  void testManualStartOfADisabledStage() {
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
        .activityId("PI_Stage_1")
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

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
  @Test
  void testManualStartOfAnActiveStage() {
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
        .activityId("PI_Stage_1")
        .singleResult()
        .getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::manualStart)
      // then
      .withFailMessage("It should not be possible to start an already active human task manually.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCaseWithManualActivation.cmmn"})
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
        .activityId("PI_Stage_1")
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
        .activityId("PI_Stage_1")
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

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
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
        .activityId("PI_Stage_1")
        .singleResult()
        .getId();
    assertThat(caseExecutionId).isNotNull();

    // when

    caseService
      .withCaseExecution(queryCaseExecutionByActivityId("PI_HumanTask_1").getId())
      .complete();
    caseService
      .withCaseExecution(queryCaseExecutionByActivityId("PI_HumanTask_2").getId())
      .complete();

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

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskAndOneStageCase.cmmn"})
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
        .activityId("PI_Stage_1")
        .singleResult()
        .getId();
    assertThat(caseExecutionId).isNotNull();

    // when
    caseService
      .withCaseExecution(queryCaseExecutionByActivityId("PI_HumanTask_11").getId())
      .complete();

    caseService
      .withCaseExecution(queryCaseExecutionByActivityId("PI_HumanTask_2").getId())
      .complete();

    // then

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1")
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

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
  @Test
  void testCompleteEnabledStage() {
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
        .activityId("PI_Stage_1")
        .singleResult()
        .getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::complete)
      // then
      .withFailMessage("Should not be able to complete stage.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskAndOneStageWithManualActivationCase.cmmn"})
  @Test
  void testCompleteDisabledStage() {
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
        .activityId("PI_Stage_1")
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .disable();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::complete)
      // then
      .withFailMessage("Should not be able to complete stage.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/emptyStageCase.cmmn"})
  @Test
  void testAutoCompletionOfEmptyStage() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    // then

    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1")
        .singleResult();

    assertThat(caseExecution).isNull();

    CaseInstance caseInstance = caseService
      .createCaseInstanceQuery()
      .completed()
      .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
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
        .activityId("PI_Stage_1")
        .singleResult()
        .getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseExecutionId);

    // when
    assertThatThrownBy(commandBuilder::close)
      // then
      .withFailMessage("It should not be possible to close a stage.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
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

    CaseExecution stageExecution = queryCaseExecutionByActivityId("PI_Stage_1");

    // when
    CaseExecution humanTaskExecution1 = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertThat(humanTaskExecution1.isActive()).isTrue();

    CaseExecution humanTaskExecution2 = queryCaseExecutionByActivityId("PI_HumanTask_2");
    assertThat(humanTaskExecution2.isActive()).isTrue();

    caseService.withCaseExecution(stageExecution.getId())
      .terminate();

    // then
    stageExecution = queryCaseExecutionByActivityId("PI_Stage_1");
    assertThat(stageExecution).isNull();

    humanTaskExecution1 = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertThat(humanTaskExecution1).isNull();

    humanTaskExecution2 = queryCaseExecutionByActivityId("PI_HumanTask_2");
    assertThat(humanTaskExecution2).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
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
      .create()
      .getId();

    CaseExecution stageExecution = queryCaseExecutionByActivityId("PI_Stage_1");

    // when
    caseService.terminateCaseExecution(stageExecution.getId());

    // then
    stageExecution = queryCaseExecutionByActivityId("PI_Stage_1");
    assertThat(stageExecution).isNull();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCaseWithManualActivation.cmmn"})
  @Test
  void testTerminateWithNonActiveState() {
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

    CaseExecution stageExecution = queryCaseExecutionByActivityId("PI_Stage_1");
    String executionId = stageExecution.getId();

    // when
    assertThatThrownBy(() -> caseService.terminateCaseExecution(executionId))
      // then
      .withFailMessage("It should not be possible to terminate a task.")
      .isInstanceOf(NotAllowedException.class)
      .hasMessageContaining("The case execution must be in state 'active' to terminate");
  }

  protected CaseExecution queryCaseExecutionByActivityId(String activityId) {
    return caseService
      .createCaseExecutionQuery()
      .activityId(activityId)
      .singleResult();
  }
}
