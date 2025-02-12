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

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.runtime.*;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
public class CaseServiceCaseInstanceTest extends PluggableProcessEngineTest {

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByKey() {
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
    assertNotNull(caseInstance);

    // check properties
    assertNull(caseInstance.getBusinessKey());
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertTrue(caseInstance.isActive());
    assertFalse(caseInstance.isEnabled());

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
  public void testCreateByInvalidKey() {
    CaseInstanceBuilder caseDefinitionWithInvalidKey = caseService.withCaseDefinitionByKey("invalid");
    assertThatThrownBy(caseDefinitionWithInvalidKey::create)
        .isInstanceOf(NotFoundException.class);

    CaseInstanceBuilder caseDefinitionWithNullKey = caseService
        .withCaseDefinitionByKey(null);
    assertThatThrownBy(caseDefinitionWithNullKey::create)
        .isInstanceOf(NotValidException.class);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateById() {
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
    assertNotNull(caseInstance);

    // check properties
    assertNull(caseInstance.getBusinessKey());
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertTrue(caseInstance.isActive());
    assertFalse(caseInstance.isEnabled());

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
  public void testCreateByInvalidId() {
    CaseInstanceBuilder builderForInvalidCaseDefinition = caseService.withCaseDefinition("invalid");
    assertThatThrownBy(builderForInvalidCaseDefinition::create)
        .isInstanceOf(NotFoundException.class);

    CaseInstanceBuilder builderForNullCaseDefinition = caseService.withCaseDefinition(null);
    assertThatThrownBy(builderForNullCaseDefinition::create)
        .isInstanceOf(NotValidException.class);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByKeyWithBusinessKey() {
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
    assertNotNull(caseInstance);

    // check properties
    assertThat(caseInstance.getBusinessKey()).isEqualTo("aBusinessKey");
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertTrue(caseInstance.isActive());
    assertFalse(caseInstance.isEnabled());

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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByIdWithBusinessKey() {
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
    assertNotNull(caseInstance);

    // check properties
    assertThat(caseInstance.getBusinessKey()).isEqualTo("aBusinessKey");
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertTrue(caseInstance.isActive());
    assertFalse(caseInstance.isEnabled());

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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByKeyWithVariable() {
    // given a deployed case definition

    // when
    CaseInstance caseInstance = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .setVariable("aVariableName", "aVariableValue")
        .setVariable("anotherVariableName", 999)
        .create();

    // then
    assertNotNull(caseInstance);

    // there should exist two variables
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    List<VariableInstance> result = query
      .caseInstanceIdIn(caseInstance.getId())
      .orderByVariableName()
      .asc()
      .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    for (VariableInstance variableInstance : result) {
      if (variableInstance.getName().equals("aVariableName")) {
        assertThat(variableInstance.getName()).isEqualTo("aVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("aVariableValue");
      } else if (variableInstance.getName().equals("anotherVariableName")) {
        assertThat(variableInstance.getName()).isEqualTo("anotherVariableName");
        assertThat(variableInstance.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByKeyWithVariables() {
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
    assertNotNull(caseInstance);

    // there should exist two variables
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    List<VariableInstance> result = query
      .caseInstanceIdIn(caseInstance.getId())
      .orderByVariableName()
      .asc()
      .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    for (VariableInstance variableInstance : result) {
      if (variableInstance.getName().equals("aVariableName")) {
        assertThat(variableInstance.getName()).isEqualTo("aVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("aVariableValue");
      } else if (variableInstance.getName().equals("anotherVariableName")) {
        assertThat(variableInstance.getName()).isEqualTo("anotherVariableName");
        assertThat(variableInstance.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByIdWithVariable() {
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
    assertNotNull(caseInstance);

    // there should exist two variables
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    List<VariableInstance> result = query
      .caseInstanceIdIn(caseInstance.getId())
      .orderByVariableName()
      .asc()
      .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    for (VariableInstance variableInstance : result) {
      if (variableInstance.getName().equals("aVariableName")) {
        assertThat(variableInstance.getName()).isEqualTo("aVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("aVariableValue");
      } else if (variableInstance.getName().equals("anotherVariableName")) {
        assertThat(variableInstance.getName()).isEqualTo("anotherVariableName");
        assertThat(variableInstance.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByIdWithVariables() {
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
    assertNotNull(caseInstance);

    // there should exist two variables
    VariableInstanceQuery query = runtimeService.createVariableInstanceQuery();

    List<VariableInstance> result = query
      .caseInstanceIdIn(caseInstance.getId())
      .orderByVariableName()
      .asc()
      .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    for (VariableInstance variableInstance : result) {
      if (variableInstance.getName().equals("aVariableName")) {
        assertThat(variableInstance.getName()).isEqualTo("aVariableName");
        assertThat(variableInstance.getValue()).isEqualTo("aVariableValue");
      } else if (variableInstance.getName().equals("anotherVariableName")) {
        assertThat(variableInstance.getName()).isEqualTo("anotherVariableName");
        assertThat(variableInstance.getValue()).isEqualTo(999);
      } else {
        fail("Unexpected variable: " + variableInstance.getName());
      }

    }

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testManualStart() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testDisable() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testReenable() {
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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testCompleteWithEnabledTask() {
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

    assertNull(caseExecution);

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isCompleted());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
  @Test
  public void testCompleteWithEnabledStage() {
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

    assertNull(caseExecution3);

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isCompleted());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCompleteWithActiveTask() {
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

    assertNotNull(caseExecution);
    assertTrue(caseExecution.isActive());

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isActive());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneStageCase.cmmn"})
  @Test
  public void testCompleteWithActiveStage() {
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

    assertNotNull(caseExecution);
    assertTrue(caseExecution.isActive());

    // the case instance is still active
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .singleResult();

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isActive());
  }


  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/emptyCasePlanModelCase.cmmn"})
  @Test
  public void testAutoCompletionOfEmptyCase() {
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

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isCompleted());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCloseAnActiveCaseInstance() {
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

    assertNotNull(caseInstance);
    assertTrue(caseInstance.isActive());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testCloseACompletedCaseInstance() {
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

    assertNull(caseInstance);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testTerminateActiveCaseInstance() {
    // given:
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();
 
    assertNotNull(queryCaseExecutionByActivityId("CasePlanModel_1"));
    
    CaseExecution taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertTrue(taskExecution.isActive());

    caseService.withCaseExecution(caseInstanceId)
      .terminate();

    CaseExecution caseInstance = queryCaseExecutionByActivityId("CasePlanModel_1");
    assertTrue(caseInstance.isTerminated());
    
    assertNull(queryCaseExecutionByActivityId("PI_HumanTask_1"));
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testTerminateNonActiveCaseInstance() {
    // given:
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();
 
    assertNotNull(queryCaseExecutionByActivityId("CasePlanModel_1"));
    
    CaseExecution taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertTrue(taskExecution.isEnabled());

    caseService.completeCaseExecution(caseInstanceId);

    try {
      // when
      caseService.terminateCaseExecution(caseInstanceId);
      fail("It should not be possible to terminate a task.");
    } catch (NotAllowedException e) {
      boolean result = e.getMessage().contains("The case execution must be in state 'active' to terminate");
      assertTrue(result);
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testTerminateActiveCaseInstanceNonFluent() {
    // given:
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    String caseInstanceId = caseService
       .withCaseDefinition(caseDefinitionId)
       .create()
       .getId();
 
    assertNotNull(queryCaseExecutionByActivityId("CasePlanModel_1"));
    
    CaseExecution taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertTrue(taskExecution.isActive());

    caseService.terminateCaseExecution(caseInstanceId);

    CaseExecution caseInstance = queryCaseExecutionByActivityId("CasePlanModel_1");
    assertTrue(caseInstance.isTerminated());
    
    assertNull(queryCaseExecutionByActivityId("PI_HumanTask_1"));
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByKeyNonFluent() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase");

    // then
    assertNotNull(caseInstance);

    // check properties
    assertNull(caseInstance.getBusinessKey());
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertTrue(caseInstance.isActive());
    assertFalse(caseInstance.isEnabled());

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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByIdNonFluent() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    CaseInstance caseInstance = caseService.createCaseInstanceById(caseDefinitionId);

    // then
    assertNotNull(caseInstance);

    // check properties
    assertNull(caseInstance.getBusinessKey());
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertTrue(caseInstance.isActive());
    assertFalse(caseInstance.isEnabled());

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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByKeyWithBusinessKeyNonFluent() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase", "aBusinessKey");

    // then
    assertNotNull(caseInstance);

    // check properties
    assertThat(caseInstance.getBusinessKey()).isEqualTo("aBusinessKey");
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertTrue(caseInstance.isActive());
    assertFalse(caseInstance.isEnabled());

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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByIdWithBusinessKeyNonFluent() {
    // given a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // when
    CaseInstance caseInstance = caseService.createCaseInstanceById(caseDefinitionId, "aBusinessKey");

    // then
    assertNotNull(caseInstance);

    // check properties
    assertThat(caseInstance.getBusinessKey()).isEqualTo("aBusinessKey");
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertTrue(caseInstance.isActive());
    assertFalse(caseInstance.isEnabled());

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

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByKeyWithVariablesNonFluent() {
    // given a deployed case definition

    // when
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "aValue");
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase", variables);

    // then
    assertNotNull(caseInstance);

    VariableInstance variable = runtimeService.createVariableInstanceQuery()
      .caseInstanceIdIn(caseInstance.getId())
      .singleResult();

    assertNotNull(variable);
    assertThat(variable.getName()).isEqualTo("aVariable");
    assertThat(variable.getValue()).isEqualTo("aValue");
    assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstance.getId());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByIdWithVariablesNonFluent() {
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
    assertNotNull(caseInstance);

    VariableInstance variable = runtimeService.createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstance.getId())
        .singleResult();

    assertNotNull(variable);
    assertThat(variable.getName()).isEqualTo("aVariable");
    assertThat(variable.getValue()).isEqualTo("aValue");
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByKeyWithVariablesAndBusinessKeyNonFluent() {
    // given a deployed case definition

    // when
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "aValue");
    CaseInstance caseInstance = caseService.createCaseInstanceByKey("oneTaskCase", "aBusinessKey", variables);

    // then
    assertNotNull(caseInstance);

    assertThat(caseInstance.getBusinessKey()).isEqualTo("aBusinessKey");
    assertThat(runtimeService.createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstance.getId())
        .variableValueEquals("aVariable", "aValue")
        .count()).isEqualTo(1);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testCreateByIdWithVariablesAndBusinessKeyNonFluent() {
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
    assertNotNull(caseInstance);

    assertThat(caseInstance.getBusinessKey()).isEqualTo("aBusinessKey");
    assertThat(runtimeService.createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstance.getId())
        .variableValueEquals("aVariable", "aValue")
        .count()).isEqualTo(1);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn"})
  @Test
  public void testCloseNonFluent() {
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

    assertNull(caseInstance);
  }

  protected CaseExecution queryCaseExecutionByActivityId(String activityId) {
    return caseService
      .createCaseExecutionQuery()
      .activityId(activityId)
      .singleResult();
  }
}
