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
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionCommandBuilder;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.CaseInstanceBuilder;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.runtime.VariableInstanceQuery;
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
class CaseServiceCaseInstanceTest {

  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected CaseService caseService;

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByKey() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .create();

    // then
    assertThat(caseInstance).isNotNull();

    // check properties
    assertThat(caseInstance.getBusinessKey()).isNull();
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertThat(caseInstance.isActive()).isTrue();
    assertThat(caseInstance.isEnabled()).isFalse();

    // get persisted case instance
    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    // should have the same properties
    assertThat(instance.getId()).isEqualTo(caseInstance.getId());
    assertThat(instance.getBusinessKey()).isEqualTo(caseInstance.getBusinessKey());
    assertThat(instance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
    assertThat(instance.getCaseInstanceId()).isEqualTo(caseInstance.getCaseInstanceId());
    assertThat(instance.isActive()).isEqualTo(caseInstance.isActive());
    assertThat(instance.isEnabled()).isEqualTo(caseInstance.isEnabled());
  }

  @Test
  void testCreateByInvalidKey() {
    CaseInstanceBuilder caseDefinitionWithInvalidKey = caseService.withCaseDefinitionByKey("invalid");
    assertThatThrownBy(caseDefinitionWithInvalidKey::create)
        .isInstanceOf(NotFoundException.class);

    CaseInstanceBuilder caseDefinitionWithNullKey = caseService
        .withCaseDefinitionByKey(null);
    assertThatThrownBy(caseDefinitionWithNullKey::create)
        .isInstanceOf(NotValidException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateById() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
      .createCaseDefinitionQuery()
      .singleResult()
      .getId();

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinition(caseDefinitionId)
        .create();

    // then
    assertThat(caseInstance).isNotNull();

    // check properties
    assertThat(caseInstance.getBusinessKey()).isNull();
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertThat(caseInstance.isActive()).isTrue();
    assertThat(caseInstance.isEnabled()).isFalse();

    // get persistent case instance
    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    // should have the same properties
    assertThat(instance.getId()).isEqualTo(caseInstance.getId());
    assertThat(instance.getBusinessKey()).isEqualTo(caseInstance.getBusinessKey());
    assertThat(instance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
    assertThat(instance.getCaseInstanceId()).isEqualTo(caseInstance.getCaseInstanceId());
    assertThat(instance.isActive()).isEqualTo(caseInstance.isActive());
    assertThat(instance.isEnabled()).isEqualTo(caseInstance.isEnabled());

  }

  @Test
  void testCreateByInvalidId() {
    CaseInstanceBuilder builderForInvalidCaseDefinition = caseService.withCaseDefinition("invalid");
    assertThatThrownBy(builderForInvalidCaseDefinition::create)
        .isInstanceOf(NotFoundException.class);

    CaseInstanceBuilder builderForNullCaseDefinition = caseService.withCaseDefinition(null);
    assertThatThrownBy(builderForNullCaseDefinition::create)
        .isInstanceOf(NotValidException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByKeyWithBusinessKey() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
      .createCaseDefinitionQuery()
      .singleResult()
      .getId();

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .businessKey("aBusinessKey")
        .create();

    // then
    assertThat(caseInstance).isNotNull();

    // check properties
    assertThat(caseInstance.getBusinessKey()).isEqualTo("aBusinessKey");
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertThat(caseInstance.isActive()).isTrue();
    assertThat(caseInstance.isEnabled()).isFalse();

    // get persistent case instance
    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    // should have the same properties
    assertThat(instance.getId()).isEqualTo(caseInstance.getId());
    assertThat(instance.getBusinessKey()).isEqualTo(caseInstance.getBusinessKey());
    assertThat(instance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
    assertThat(instance.getCaseInstanceId()).isEqualTo(caseInstance.getCaseInstanceId());
    assertThat(instance.isActive()).isEqualTo(caseInstance.isActive());
    assertThat(instance.isEnabled()).isEqualTo(caseInstance.isEnabled());

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByIdWithBusinessKey() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
      .createCaseDefinitionQuery()
      .singleResult()
      .getId();

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinition(caseDefinitionId)
        .businessKey("aBusinessKey")
        .create();

    // then
    assertThat(caseInstance).isNotNull();

    // check properties
    assertThat(caseInstance.getBusinessKey()).isEqualTo("aBusinessKey");
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertThat(caseInstance.isActive()).isTrue();
    assertThat(caseInstance.isEnabled()).isFalse();

    // get persistent case instance
    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    // should have the same properties
    assertThat(instance.getId()).isEqualTo(caseInstance.getId());
    assertThat(instance.getBusinessKey()).isEqualTo(caseInstance.getBusinessKey());
    assertThat(instance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
    assertThat(instance.getCaseInstanceId()).isEqualTo(caseInstance.getCaseInstanceId());
    assertThat(instance.isActive()).isEqualTo(caseInstance.isActive());
    assertThat(instance.isEnabled()).isEqualTo(caseInstance.isEnabled());

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByKeyWithVariable() {
    // given a deployed case definition

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .setVariable("aVariableName", "aVariableValue")
        .setVariable("anotherVariableName", 999)
        .create();

    // then
    assertThat(caseInstance).isNotNull();

    // there should exist two variables
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    List<VariableInstance> result = query
      .caseInstanceIdIn(caseInstance.getId())
      .orderByVariableName()
      .asc()
      .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variableInstance : result) {
      if ("aVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("aVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("aVariableValue");
      } else if ("anotherVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("anotherVariableName");
        assertThat(variableInstance.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByKeyWithVariables() {
    // given a deployed case definition
    Map<String, Object> variables = new HashMap<>();

    variables.put("aVariableName", "aVariableValue");
    variables.put("anotherVariableName", 999);

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .setVariables(variables)
        .create();

    // then
    assertThat(caseInstance).isNotNull();

    // there should exist two variables
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    List<VariableInstance> result = query
      .caseInstanceIdIn(caseInstance.getId())
      .orderByVariableName()
      .asc()
      .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variableInstance : result) {
      if ("aVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("aVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("aVariableValue");
      } else if ("anotherVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("anotherVariableName");
        assertThat(variableInstance.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByIdWithVariable() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("aVariableName", "aVariableValue")
        .setVariable("anotherVariableName", 999)
        .create();

    // then
    assertThat(caseInstance).isNotNull();

    // there should exist two variables
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    List<VariableInstance> result = query
      .caseInstanceIdIn(caseInstance.getId())
      .orderByVariableName()
      .asc()
      .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variableInstance : result) {
      if ("aVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("aVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("aVariableValue");
      } else if ("anotherVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("anotherVariableName");
        assertThat(variableInstance.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByIdWithVariables() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    Map<String, Object> variables = new HashMap<>();

    variables.put("aVariableName", "aVariableValue");
    variables.put("anotherVariableName", 999);

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariables(variables)
        .create();

    // then
    assertThat(caseInstance).isNotNull();

    // there should exist two variables
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    List<VariableInstance> result = query
      .caseInstanceIdIn(caseInstance.getId())
      .orderByVariableName()
      .asc()
      .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variableInstance : result) {
      if ("aVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("aVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("aVariableValue");
      } else if ("anotherVariableName".equals(variableInstance.getName())) {
        assertThat(variableInstance.getName()).isEqualTo("anotherVariableName");
        assertThat(variableInstance.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testManualStart() {
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

    // when
    CaseExecutionCommandBuilder builder = caseService.withCaseExecution(caseInstanceId);
    assertThatThrownBy(builder::manualStart)
        .withFailMessage("It should not be possible to start a case instance manually.")
        .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testDisable() {
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

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseInstanceId);

    // when
    assertThatThrownBy(commandBuilder::disable)
        .withFailMessage("It should not be possible to disable a case instance.")
        .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testReenable() {
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

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseInstanceId);

    // when
    assertThatThrownBy(commandBuilder::reenable)
        .withFailMessage("It should not be possible to re-enable a case instance.")
        .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testCompleteWithEnabledTask() {
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

    // when

    caseService
        .withCaseExecution(caseInstanceId)
        .complete();

    // then

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(caseExecution).isNull();

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
  @Test
  void testCompleteWithEnabledStage() {
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

    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();


    CaseExecution caseExecution2 = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2")
        .singleResult();

    // when

    caseService
      .withCaseExecution(caseExecution.getId())
      .complete();

    caseService
      .withCaseExecution(caseExecution2.getId())
      .complete();

    // then

    // the corresponding case execution has been also
    // deleted and completed
    CaseExecution caseExecution3 = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1")
        .singleResult();

    assertThat(caseExecution3).isNull();

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCompleteWithActiveTask() {
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

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseInstanceId);

    // when
    assertThatThrownBy(commandBuilder::complete)
        .withFailMessage("It should not be possible to complete a case instance containing an active task.")
        .isInstanceOf(ProcessEngineException.class);

    // then

    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult();

    assertThat(caseExecution).isNotNull();
    assertThat(caseExecution.isActive()).isTrue();

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
  @Test
  void testCompleteWithActiveStage() {
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

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseInstanceId);

    // when
    assertThatThrownBy(commandBuilder::complete)
        .withFailMessage("It should not be possible to complete a case instance containing an active stage.")
        .isInstanceOf(ProcessEngineException.class);

    // then

    CaseExecution caseExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1")
        .singleResult();

    assertThat(caseExecution).isNotNull();
    assertThat(caseExecution.isActive()).isTrue();

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();
  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/emptyCasePlanModelCase.cmmn"})
  @Test
  void testAutoCompletionOfEmptyCase() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    caseService
       .withCaseDefinition(caseDefinitionId)
       .create();

    // then
    CaseInstance caseInstance = caseService
      .createCaseInstanceQuery()
      .completed()
      .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCloseAnActiveCaseInstance() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseInstanceId);

    assertThatThrownBy(commandBuilder::close)
        .withFailMessage("It should not be possible to close an active case instance.")
        .isInstanceOf(ProcessEngineException.class);

    // then
    CaseInstance caseInstance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testCloseACompletedCaseInstance() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // disable human task -> case instance is completed
    caseService
      .withCaseExecution(caseExecutionId)
      .disable();

    // when
    caseService
      .withCaseExecution(caseInstanceId)
      .close();

    // then
    CaseInstance caseInstance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    assertThat(caseInstance).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testTerminateActiveCaseInstance() {
    // given:
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    assertThat(queryCaseExecutionByActivityId("CasePlanModel_1")).isNotNull();

    CaseExecution taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertThat(taskExecution.isActive()).isTrue();

    caseService.withCaseExecution(caseInstanceId)
      .terminate();

    CaseExecution caseInstance = queryCaseExecutionByActivityId("CasePlanModel_1");
    assertThat(caseInstance.isTerminated()).isTrue();

    assertThat(queryCaseExecutionByActivityId("PI_HumanTask_1")).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testTerminateNonActiveCaseInstance() {
    // given:
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    assertThat(queryCaseExecutionByActivityId("CasePlanModel_1")).isNotNull();

    CaseExecution taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertThat(taskExecution.isEnabled()).isTrue();

    caseService.completeCaseExecution(caseInstanceId);

    assertThatThrownBy(() -> caseService.terminateCaseExecution(caseInstanceId), "It should not be possible to terminate a task.")
        .isInstanceOf(NotAllowedException.class)
        .hasMessageContaining("The case execution must be in state 'active' to terminate");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testTerminateActiveCaseInstanceNonFluent() {
    // given:
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    assertThat(queryCaseExecutionByActivityId("CasePlanModel_1")).isNotNull();

    CaseExecution taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertThat(taskExecution.isActive()).isTrue();

    caseService.terminateCaseExecution(caseInstanceId);

    CaseExecution caseInstance = queryCaseExecutionByActivityId("CasePlanModel_1");
    assertThat(caseInstance.isTerminated()).isTrue();

    assertThat(queryCaseExecutionByActivityId("PI_HumanTask_1")).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByKeyNonFluent() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase");

    // then
    assertThat(caseInstance).isNotNull();

    // check properties
    assertThat(caseInstance.getBusinessKey()).isNull();
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertThat(caseInstance.isActive()).isTrue();
    assertThat(caseInstance.isEnabled()).isFalse();

    // get persisted case instance
    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    // should have the same properties
    assertThat(instance.getId()).isEqualTo(caseInstance.getId());
    assertThat(instance.getBusinessKey()).isEqualTo(caseInstance.getBusinessKey());
    assertThat(instance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
    assertThat(instance.getCaseInstanceId()).isEqualTo(caseInstance.getCaseInstanceId());
    assertThat(instance.isActive()).isEqualTo(caseInstance.isActive());
    assertThat(instance.isEnabled()).isEqualTo(caseInstance.isEnabled());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByIdNonFluent() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    CaseInstance caseInstance = caseService.createCaseInstanceById(caseDefinitionId);

    // then
    assertThat(caseInstance).isNotNull();

    // check properties
    assertThat(caseInstance.getBusinessKey()).isNull();
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertThat(caseInstance.isActive()).isTrue();
    assertThat(caseInstance.isEnabled()).isFalse();

    // get persisted case instance
    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    // should have the same properties
    assertThat(instance.getId()).isEqualTo(caseInstance.getId());
    assertThat(instance.getBusinessKey()).isEqualTo(caseInstance.getBusinessKey());
    assertThat(instance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
    assertThat(instance.getCaseInstanceId()).isEqualTo(caseInstance.getCaseInstanceId());
    assertThat(instance.isActive()).isEqualTo(caseInstance.isActive());
    assertThat(instance.isEnabled()).isEqualTo(caseInstance.isEnabled());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByKeyWithBusinessKeyNonFluent() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase", "aBusinessKey");

    // then
    assertThat(caseInstance).isNotNull();

    // check properties
    assertThat(caseInstance.getBusinessKey()).isEqualTo("aBusinessKey");
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertThat(caseInstance.isActive()).isTrue();
    assertThat(caseInstance.isEnabled()).isFalse();

    // get persisted case instance
    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    // should have the same properties
    assertThat(instance.getId()).isEqualTo(caseInstance.getId());
    assertThat(instance.getBusinessKey()).isEqualTo(caseInstance.getBusinessKey());
    assertThat(instance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
    assertThat(instance.getCaseInstanceId()).isEqualTo(caseInstance.getCaseInstanceId());
    assertThat(instance.isActive()).isEqualTo(caseInstance.isActive());
    assertThat(instance.isEnabled()).isEqualTo(caseInstance.isEnabled());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByIdWithBusinessKeyNonFluent() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    CaseInstance caseInstance = caseService.createCaseInstanceById(caseDefinitionId, "aBusinessKey");

    // then
    assertThat(caseInstance).isNotNull();

    // check properties
    assertThat(caseInstance.getBusinessKey()).isEqualTo("aBusinessKey");
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertThat(caseInstance.isActive()).isTrue();
    assertThat(caseInstance.isEnabled()).isFalse();

    // get persisted case instance
    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    // should have the same properties
    assertThat(instance.getId()).isEqualTo(caseInstance.getId());
    assertThat(instance.getBusinessKey()).isEqualTo(caseInstance.getBusinessKey());
    assertThat(instance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
    assertThat(instance.getCaseInstanceId()).isEqualTo(caseInstance.getCaseInstanceId());
    assertThat(instance.isActive()).isEqualTo(caseInstance.isActive());
    assertThat(instance.isEnabled()).isEqualTo(caseInstance.isEnabled());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByKeyWithVariablesNonFluent() {
    // given a deployed case definition

    // when
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "aValue");
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase", variables);

    // then
    assertThat(caseInstance).isNotNull();

    VariableInstance variable = runtimeService.createVariableInstanceQuery()
      .caseInstanceIdIn(caseInstance.getId())
      .singleResult();

    assertThat(variable).isNotNull();
    assertThat(variable.getName()).isEqualTo("aVariable");
    assertThat(variable.getValue()).isEqualTo("aValue");
    assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstance.getId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByIdWithVariablesNonFluent() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "aValue");
    CaseInstance caseInstance = caseService.createCaseInstanceById(caseDefinitionId, variables);

    // then
    assertThat(caseInstance).isNotNull();

    VariableInstance variable = runtimeService.createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstance.getId())
        .singleResult();

    assertThat(variable).isNotNull();
    assertThat(variable.getName()).isEqualTo("aVariable");
    assertThat(variable.getValue()).isEqualTo("aValue");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByKeyWithVariablesAndBusinessKeyNonFluent() {
    // given a deployed case definition

    // when
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "aValue");
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase", "aBusinessKey", variables);

    // then
    assertThat(caseInstance).isNotNull();

    assertThat(caseInstance.getBusinessKey()).isEqualTo("aBusinessKey");
    assertThat(runtimeService.createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstance.getId())
        .variableValueEquals("aVariable", "aValue")
        .count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testCreateByIdWithVariablesAndBusinessKeyNonFluent() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "aValue");
    CaseInstance caseInstance = caseService.createCaseInstanceById(caseDefinitionId, "aBusinessKey", variables);

    // then
    assertThat(caseInstance).isNotNull();

    assertThat(caseInstance.getBusinessKey()).isEqualTo("aBusinessKey");
    assertThat(runtimeService.createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstance.getId())
        .variableValueEquals("aVariable", "aValue")
        .count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  void testCloseNonFluent() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // disable human task -> case instance is completed
    caseService
      .withCaseExecution(caseExecutionId)
      .disable();

    // when
    caseService.closeCaseInstance(caseInstanceId);

    // then
    CaseInstance caseInstance = caseService
      .createCaseInstanceQuery()
      .singleResult();

    assertThat(caseInstance).isNull();
  }

  protected CaseExecution queryCaseExecutionByActivityId(String activityId) {
    return caseService
      .createCaseExecutionQuery()
      .activityId(activityId)
      .singleResult();
  }
}
