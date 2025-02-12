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

import static org.operaton.bpm.engine.variable.Variables.booleanValue;
import static org.operaton.bpm.engine.variable.Variables.createVariables;
import static org.operaton.bpm.engine.variable.Variables.integerValue;
import static org.operaton.bpm.engine.variable.Variables.stringValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.runtime.*;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.StringValue;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
public class CaseServiceTest extends PluggableProcessEngineTest {

  @Test
  public void testCreateCaseInstanceQuery() {
    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    assertNotNull(query);
  }

  @Test
  public void testCreateCaseExecutionQuery() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    assertNotNull(query);
  }

  @Test
  public void testWithCaseExecution() {
    CaseExecutionCommandBuilder builder = caseService.withCaseExecution("aCaseExecutionId");

    assertNotNull(builder);
  }

  @Test
  public void testManualStartInvalidCaseExecution() {
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution("invalid");
    assertThatThrownBy(commandBuilder::manualStart)
      .isInstanceOf(NotFoundException.class);

    CaseExecutionCommandBuilder commandBuilder2 = caseService.withCaseExecution(null);
    assertThatThrownBy(commandBuilder2::manualStart)
      .isInstanceOf(NotValidException.class);
  }

  @Test
  public void testCompleteInvalidCaseExeuction() {
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution("invalid");
    assertThatThrownBy(commandBuilder::complete)
      .withFailMessage("The case execution should not be found.")
      .isInstanceOf(NotFoundException.class);

    CaseExecutionCommandBuilder commandBuilder2 = caseService.withCaseExecution(null);
    assertThatThrownBy(commandBuilder2::complete)
      .withFailMessage("The case execution should not be found.")
      .isInstanceOf(NotValidException.class);
  }

  @Test
  public void testCloseInvalidCaseExeuction() {
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution("invalid");
    assertThatThrownBy(commandBuilder::close)
      .withFailMessage("The case execution should not be found.")
      .isInstanceOf(NotFoundException.class);

    CaseExecutionCommandBuilder commandBuilder2 = caseService.withCaseExecution(null);
    assertThatThrownBy(commandBuilder2::close)
      .withFailMessage("The case execution should not be found.")
      .isInstanceOf(NotValidException.class);
  }

  @Test
  public void testTerminateInvalidCaseExeuction() {
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution("invalid");
    assertThatThrownBy(commandBuilder::terminate)
      .withFailMessage("The case execution should not be found.")
      .isInstanceOf(NotFoundException.class);

    CaseExecutionCommandBuilder commandBuilder2 = caseService.withCaseExecution(null);
    assertThatThrownBy(commandBuilder2::terminate)
      .withFailMessage("The case execution should not be found.")
      .isInstanceOf(NotValidException.class);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteSetVariable() {
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

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .setVariable("aVariableName", "abc")
      .setVariable("anotherVariableName", 999)
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
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
  public void testExecuteSetVariableTyped() {
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

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .setVariable("aVariableName", stringValue("abc"))
      .setVariable("anotherVariableName", integerValue(null))
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if (variable.getName().equals("aVariableName")) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(stringValue("abc"));
      } else if (variable.getName().equals("anotherVariableName")) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(integerValue(null));
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteSetVariables() {
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

    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 999);

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .setVariables(variables)
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
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
  public void testExecuteSetVariablesTyped() {
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

    VariableMap variables = createVariables()
        .putValueTyped("aVariableName", stringValue("abc"))
        .putValueTyped("anotherVariableName", integerValue(null));

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .setVariables(variables)
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if (variable.getName().equals("aVariableName")) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(stringValue("abc"));
      } else if (variable.getName().equals("anotherVariableName")) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(integerValue(null));
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteSetVariableAndVariables() {
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

    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 999);

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .setVariables(variables)
      .setVariable("aThirdVariable", 123)
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(3);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if (variable.getName().equals("aVariableName")) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if (variable.getName().equals("anotherVariableName")) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else if (variable.getName().equals("aThirdVariable")) {
        assertThat(variable.getName()).isEqualTo("aThirdVariable");
        assertThat(variable.getValue()).isEqualTo(123);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }


  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteSetVariableAndVariablesTyped() {
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

    VariableMap variables = createVariables()
        .putValueTyped("aVariableName", stringValue("abc"))
        .putValueTyped("anotherVariableName", integerValue(null));

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .setVariables(variables)
      .setVariable("aThirdVariable", booleanValue(null))
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(3);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if (variable.getName().equals("aVariableName")) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(stringValue("abc"));
      } else if (variable.getName().equals("anotherVariableName")) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(integerValue(null));
      } else if (variable.getName().equals("aThirdVariable")) {
        assertThat(variable.getName()).isEqualTo("aThirdVariable");
        assertThat(variable.getTypedValue()).isEqualTo(booleanValue(null));
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteSetVariableLocal() {
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

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .setVariableLocal("aVariableName", "abc")
      .setVariableLocal("anotherVariableName", 999)
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
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

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
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
  public void testExecuteSetVariablesLocal() {
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

    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 999);

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .setVariablesLocal(variables)
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
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

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
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
  public void testExecuteSetVariablesLocalTyped() {
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

    VariableMap variables = createVariables()
        .putValueTyped("aVariableName", stringValue("abc"))
        .putValueTyped("anotherVariableName", integerValue(null));

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .setVariablesLocal(variables)
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if (variable.getName().equals("aVariableName")) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(stringValue("abc"));
      } else if (variable.getName().equals("anotherVariableName")) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(integerValue(null));
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteSetVariableLocalAndVariablesLocal() {
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

    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 999);

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .setVariablesLocal(variables)
      .setVariableLocal("aThirdVariable", 123)
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(3);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if (variable.getName().equals("aVariableName")) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if (variable.getName().equals("anotherVariableName")) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else if (variable.getName().equals("aThirdVariable")) {
        assertThat(variable.getName()).isEqualTo("aThirdVariable");
        assertThat(variable.getValue()).isEqualTo(123);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(3);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if (variable.getName().equals("aVariableName")) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if (variable.getName().equals("anotherVariableName")) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else if (variable.getName().equals("aThirdVariable")) {
        assertThat(variable.getName()).isEqualTo("aThirdVariable");
        assertThat(variable.getValue()).isEqualTo(123);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteSetVariableAndVariablesLocal() {
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

    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 999);

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .setVariables(variables)
      .setVariableLocal("aThirdVariable", 123)
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(1);

    VariableInstance aThirdVariable = result.get(0);

    assertNotNull(aThirdVariable);
    assertThat(aThirdVariable.getName()).isEqualTo("aThirdVariable");
    assertThat(aThirdVariable.getValue()).isEqualTo(123);

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(3);

    for (VariableInstance variable : result) {


      if (variable.getName().equals("aVariableName")) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");

      } else if (variable.getName().equals("anotherVariableName")) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);

      } else if (variable.getName().equals("aThirdVariable")) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("aThirdVariable");
        assertThat(variable.getValue()).isEqualTo(123);

      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteRemoveVariable() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("aVariableName", "abc")
        .setVariable("anotherVariableName", 999)
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
      .removeVariable("aVariableName")
      .removeVariable("anotherVariableName")
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertTrue(result.isEmpty());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteRemoveVariables() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("aVariableName", "abc")
        .setVariable("anotherVariableName", 999)
        .create()
        .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    List<String> variableNames = new ArrayList<>();
    variableNames.add("aVariableName");
    variableNames.add("anotherVariableName");

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .removeVariables(variableNames)
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertTrue(result.isEmpty());

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteRemoveVariableAndVariables() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("aVariableName", "abc")
        .setVariable("anotherVariableName", 999)
        .setVariable("aThirdVariable", 123)
        .create()
        .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    List<String> variableNames = new ArrayList<>();
    variableNames.add("aVariableName");
    variableNames.add("anotherVariableName");

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .removeVariables(variableNames)
      .removeVariable("aThirdVariable")
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertTrue(result.isEmpty());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteRemoveVariableLocal() {
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
      .setVariableLocal("aVariableName", "abc")
      .setVariableLocal("anotherVariableName", 999)
      .execute();

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .removeVariableLocal("aVariableName")
      .removeVariableLocal("anotherVariableName")
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertTrue(result.isEmpty());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteRemoveVariablesLocal() {
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
      .setVariableLocal("aVariableName", "abc")
      .setVariableLocal("anotherVariableName", 999)
      .execute();

    List<String> variableNames = new ArrayList<>();
    variableNames.add("aVariableName");
    variableNames.add("anotherVariableName");

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .removeVariablesLocal(variableNames)
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertTrue(result.isEmpty());

  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteRemoveVariableLocalAndVariablesLocal() {
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
      .setVariableLocal("aVariableName", "abc")
      .setVariableLocal("anotherVariableName", 999)
      .setVariableLocal("aThirdVariable", 123)
      .execute();

    List<String> variableNames = new ArrayList<>();
    variableNames.add("aVariableName");
    variableNames.add("anotherVariableName");

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .removeVariablesLocal(variableNames)
      .removeVariableLocal("aThirdVariable")
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertTrue(result.isEmpty());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteRemoveVariableAndVariablesLocal() {
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
      .setVariable("aVariableName", "abc")
      .setVariable("anotherVariableName", 999)
      .setVariableLocal("aThirdVariable", 123)
      .execute();

    List<String> variableNames = new ArrayList<>();
    variableNames.add("aVariableName");
    variableNames.add("anotherVariableName");

    // when
    caseService
      .withCaseExecution(caseExecutionId)
      .removeVariables(variableNames)
      .removeVariableLocal("aThirdVariable")
      .execute();

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertTrue(result.isEmpty());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteRemoveAndSetSameVariable() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
     caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("aVariableName", "abc")
        .create()
        .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    try {
      // when
      caseService
        .withCaseExecution(caseExecutionId)
        .removeVariable("aVariableName")
        .setVariable("aVariableName", "xyz")
        .execute();
    } catch (NotValidException e) {
      // then
      testRule.assertTextPresent("Cannot set and remove a variable with the same variable name: 'aVariableName' within a command.", e.getMessage());
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testExecuteRemoveAndSetSameLocal() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
     caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("aVariableName", "abc")
        .create()
        .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    try {
      // when
      caseService
        .withCaseExecution(caseExecutionId)
        .setVariableLocal("aVariableName", "xyz")
        .removeVariableLocal("aVariableName")
        .execute();
    } catch (NotValidException e) {
      // then
      testRule.assertTextPresent("Cannot set and remove a variable with the same variable name: 'aVariableName' within a command.", e.getMessage());
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariables() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
     caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("aVariableName", "abc")
        .setVariable("anotherVariableName", 999)
        .create()
        .getId();

     String caseExecutionId = caseService
         .createCaseExecutionQuery()
         .activityId("PI_HumanTask_1")
         .singleResult()
         .getId();

     // when
     Map<String, Object> variables = caseService.getVariables(caseExecutionId);

     // then
     assertNotNull(variables);
     assertFalse(variables.isEmpty());
    assertThat(variables)
            .hasSize(2)
            .containsEntry("aVariableName", "abc")
            .containsEntry("anotherVariableName", 999);

    assertThat(caseService.getVariablesTyped(caseExecutionId, true)).isEqualTo(variables);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariablesTyped() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
     caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("aVariableName", "abc")
        .setVariable("anotherVariableName", 999)
        .create()
        .getId();

     String caseExecutionId = caseService
         .createCaseExecutionQuery()
         .activityId("PI_HumanTask_1")
         .singleResult()
         .getId();

     // when
     VariableMap variables = caseService.getVariablesTyped(caseExecutionId);

     // then
     assertNotNull(variables);
     assertFalse(variables.isEmpty());
    assertThat(variables).hasSize(2);

    assertThat(variables).containsEntry("aVariableName", "abc");
    assertThat(variables).containsEntry("anotherVariableName", 999);

    assertThat(caseService.getVariablesTyped(caseExecutionId, true)).isEqualTo(variables);
  }

  @Test
  public void testGetVariablesInvalidCaseExecutionId() {

    try {
      caseService.getVariables("invalid");
      fail("The case execution should not be found.");
    } catch (NotFoundException e) {

    }

    try {
      caseService.getVariables(null);
      fail("The case execution should not be found.");
    } catch (NotValidException e) {

    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariablesWithVariableNames() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
     caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("aVariableName", "abc")
        .setVariable("anotherVariableName", 999)
        .setVariable("thirVariable", "xyz")
        .create()
        .getId();

     String caseExecutionId = caseService
         .createCaseExecutionQuery()
         .activityId("PI_HumanTask_1")
         .singleResult()
         .getId();

     List<String> names = new ArrayList<>();
     names.add("aVariableName");
     names.add("anotherVariableName");

     // when
     Map<String, Object> variables = caseService.getVariables(caseExecutionId, names);

     // then
     assertNotNull(variables);
     assertFalse(variables.isEmpty());
    assertThat(variables)
            .hasSize(2)
            .containsEntry("aVariableName", "abc")
            .containsEntry("anotherVariableName", 999);

    assertThat(caseService.getVariables(caseExecutionId, names)).isEqualTo(variables);
  }


  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariablesWithVariableNamesTyped() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
     caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("aVariableName", "abc")
        .setVariable("anotherVariableName", 999)
        .setVariable("thirVariable", "xyz")
        .create()
        .getId();

     String caseExecutionId = caseService
         .createCaseExecutionQuery()
         .activityId("PI_HumanTask_1")
         .singleResult()
         .getId();

     List<String> names = new ArrayList<>();
     names.add("aVariableName");
     names.add("anotherVariableName");

     // when
     VariableMap variables = caseService.getVariablesTyped(caseExecutionId, names, true);

     // then
     assertNotNull(variables);
     assertFalse(variables.isEmpty());
    assertThat(variables).hasSize(2);

    assertThat(variables).containsEntry("aVariableName", "abc");
    assertThat(variables).containsEntry("anotherVariableName", 999);

    assertThat(caseService.getVariables(caseExecutionId, names)).isEqualTo(variables);
  }

  @Test
  public void testGetVariablesWithVariablesNamesInvalidCaseExecutionId() {

    try {
      caseService.getVariables("invalid", null);
      fail("The case execution should not be found.");
    } catch (NotFoundException e) {

    }

    try {
      caseService.getVariables(null, null);
      fail("The case execution should not be found.");
    } catch (NotValidException e) {

    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariablesLocal() {
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
       .setVariableLocal("aVariableName", "abc")
       .setVariableLocal("anotherVariableName", 999)
       .execute();

     // when
     Map<String, Object> variables = caseService.getVariablesLocal(caseExecutionId);

     // then
     assertNotNull(variables);
     assertFalse(variables.isEmpty());
    assertThat(variables)
            .hasSize(2)
            .containsEntry("aVariableName", "abc")
            .containsEntry("anotherVariableName", 999);

    assertThat(caseService.getVariablesLocal(caseExecutionId)).isEqualTo(variables);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariablesLocalTyped() {
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
       .setVariableLocal("aVariableName", "abc")
       .setVariableLocal("anotherVariableName", 999)
       .execute();

     // when
     VariableMap variables = caseService.getVariablesLocalTyped(caseExecutionId);

     // then
     assertNotNull(variables);
     assertFalse(variables.isEmpty());
    assertThat(variables).hasSize(2);

    assertThat(variables).containsEntry("aVariableName", "abc");
    assertThat(variables).containsEntry("anotherVariableName", 999);

    assertThat(caseService.getVariablesLocalTyped(caseExecutionId, true)).isEqualTo(variables);
  }

  @Test
  public void testGetVariablesLocalInvalidCaseExecutionId() {

    try {
      caseService.getVariablesLocal("invalid");
      fail("The case execution should not be found.");
    } catch (NotFoundException e) {

    }

    try {
      caseService.getVariablesLocal(null);
      fail("The case execution should not be found.");
    } catch (NotValidException e) {

    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariablesLocalWithVariableNames() {
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
      .setVariableLocal("aVariableName", "abc")
      .setVariableLocal("anotherVariableName", 999)
      .execute();

     List<String> names = new ArrayList<>();
     names.add("aVariableName");
     names.add("anotherVariableName");

     // when
     Map<String, Object> variables = caseService.getVariablesLocal(caseExecutionId, names);

     // then
     assertNotNull(variables);
     assertFalse(variables.isEmpty());
    assertThat(variables)
            .hasSize(2)
            .containsEntry("aVariableName", "abc")
            .containsEntry("anotherVariableName", 999);

    assertThat(caseService.getVariablesLocal(caseExecutionId, names)).isEqualTo(variables);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariablesLocalWithVariableNamesTyped() {
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
      .setVariableLocal("aVariableName", "abc")
      .setVariableLocal("anotherVariableName", 999)
      .execute();

     List<String> names = new ArrayList<>();
     names.add("aVariableName");
     names.add("anotherVariableName");

     // when
     VariableMap variables = caseService.getVariablesLocalTyped(caseExecutionId, names, true);

     // then
     assertNotNull(variables);
     assertFalse(variables.isEmpty());
    assertThat(variables).hasSize(2);

    assertThat(variables).containsEntry("aVariableName", "abc");
    assertThat(variables).containsEntry("anotherVariableName", 999);

    assertThat(caseService.getVariablesLocal(caseExecutionId, names)).isEqualTo(variables);
  }
  @Test
  public void testGetVariablesLocalWithVariablesNamesInvalidCaseExecutionId() {

    try {
      caseService.getVariablesLocal("invalid", null);
      fail("The case execution should not be found.");
    } catch (NotFoundException e) {

    }

    try {
      caseService.getVariablesLocal(null, null);
      fail("The case execution should not be found.");
    } catch (NotValidException e) {

    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariable() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
     caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("aVariableName", "abc")
        .setVariable("anotherVariableName", 999)
        .setVariable("thirVariable", "xyz")
        .create()
        .getId();

     String caseExecutionId = caseService
         .createCaseExecutionQuery()
         .activityId("PI_HumanTask_1")
         .singleResult()
         .getId();

     // when
     Object value = caseService.getVariable(caseExecutionId, "aVariableName");

     // then
     assertNotNull(value);
    assertThat(value).isEqualTo("abc");
  }

  @Test
  public void testGetVariableInvalidCaseExecutionId() {
    try {
      caseService.getVariable("invalid", "aVariableName");
      fail("The case execution should not be found.");
    } catch (NotFoundException e) {

    }

    try {
      caseService.getVariable(null, "aVariableName");
      fail("The case execution should not be found.");
    } catch (NotValidException e) {

    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariableLocal() {
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
       .setVariableLocal("aVariableName", "abc")
       .setVariableLocal("anotherVariableName", 999)
       .execute();

     // when
     Object value = caseService.getVariableLocal(caseExecutionId, "aVariableName");

     // then
     assertNotNull(value);
    assertThat(value).isEqualTo("abc");
  }

  @Test
  public void testGetVariableLocalInvalidCaseExecutionId() {
    try {
      caseService.getVariableLocal("invalid", "aVariableName");
      fail("The case execution should not be found.");
    } catch (NotFoundException e) {

    }

    try {
      caseService.getVariableLocal(null, "aVariableName");
      fail("The case execution should not be found.");
    } catch (NotValidException e) {

    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariableTyped() {
    // given:
    // a deployed case definition
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .singleResult()
        .getId();

    // an active case instance
     caseService
        .withCaseDefinition(caseDefinitionId)
        .setVariable("aVariableName", "abc")
        .setVariable("anotherVariableName", 999)
        .setVariable("aSerializedObject", Variables.objectValue(Arrays.asList("1", "2")).create())
        .create()
        .getId();

     String caseExecutionId = caseService
         .createCaseExecutionQuery()
         .activityId("PI_HumanTask_1")
         .singleResult()
         .getId();

     // when
     StringValue stringValue = caseService.getVariableTyped(caseExecutionId, "aVariableName");
     ObjectValue objectValue = caseService.getVariableTyped(caseExecutionId, "aSerializedObject");
     ObjectValue serializedObjectValue = caseService.getVariableTyped(caseExecutionId, "aSerializedObject", false);

     // then
     assertNotNull(stringValue.getValue());
     assertNotNull(objectValue.getValue());
     assertTrue(objectValue.isDeserialized());
    assertThat(objectValue.getValue()).isEqualTo(Arrays.asList("1", "2"));
     assertFalse(serializedObjectValue.isDeserialized());
     assertNotNull(serializedObjectValue.getValueSerialized());
  }

  @Test
  public void testGetVariableTypedInvalidCaseExecutionId() {
    try {
      caseService.getVariableTyped("invalid", "aVariableName");
      fail("The case execution should not be found.");
    } catch (NotFoundException e) {

    }

    try {
      caseService.getVariableTyped(null, "aVariableName");
      fail("The case execution should not be found.");
    } catch (NotValidException e) {

    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testSetVariable() {
    // given:
    // a deployed case definition
    // and an active case instance
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .create()
        .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService.setVariable(caseExecutionId, "aVariableName", "abc");

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseInstanceId)
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(1);

    VariableInstance variable = result.get(0);
    assertThat(variable.getName()).isEqualTo("aVariableName");
    assertThat(variable.getValue()).isEqualTo("abc");
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testSetVariables() {
    // given:
    // a deployed case definition
    // and an active case instance
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .create()
        .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 123);
    caseService.setVariables(caseExecutionId, variables);

    // then

    // query by caseExecutionId
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertTrue(result.isEmpty());

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseInstanceId)
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    for (VariableInstance variable : result) {
      if (variable.getName().equals("aVariableName")) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");

      } else if (variable.getName().equals("anotherVariableName")) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(123);

      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testSetVariableLocal() {
    // given:
    // a deployed case definition
    // an active case instance
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .create()
        .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService.setVariableLocal(caseExecutionId, "aVariableName", "abc");

    // then

    // query by case instance id
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseInstanceId)
        .list();

    assertTrue(result.isEmpty());

    // query by caseExecutionId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(1);

    VariableInstance variable = result.get(0);
    assertThat(variable.getName()).isEqualTo("aVariableName");
    assertThat(variable.getValue()).isEqualTo("abc");
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testSetVariablesLocal() {
    // given:
    // a deployed case definition
    // and an active case instance
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .create()
        .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariableName", "abc");
    variables.put("anotherVariableName", 123);
    caseService.setVariablesLocal(caseExecutionId, variables);

    // then

    // query by case instance id
    List<VariableInstance> result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseInstanceId)
        .list();

    assertTrue(result.isEmpty());

    // query by caseExecutionId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertFalse(result.isEmpty());
    assertThat(result).hasSize(2);

    for (VariableInstance variable : result) {
      if (variable.getName().equals("aVariableName")) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");

      } else if (variable.getName().equals("anotherVariableName")) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(123);

      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariableTypedLocal() {
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

     caseService.withCaseExecution(caseExecutionId)
        .setVariableLocal("aVariableName", "abc")
        .setVariableLocal("anotherVariableName", 999)
        .setVariableLocal("aSerializedObject", Variables.objectValue(Arrays.asList("1", "2")).create())
        .execute();

     // when
     StringValue stringValue = caseService.getVariableLocalTyped(caseExecutionId, "aVariableName");
     ObjectValue objectValue = caseService.getVariableLocalTyped(caseExecutionId, "aSerializedObject");
     ObjectValue serializedObjectValue = caseService.getVariableLocalTyped(caseExecutionId, "aSerializedObject", false);

     // then
     assertNotNull(stringValue.getValue());
     assertNotNull(objectValue.getValue());
     assertTrue(objectValue.isDeserialized());
    assertThat(objectValue.getValue()).isEqualTo(Arrays.asList("1", "2"));
     assertFalse(serializedObjectValue.isDeserialized());
     assertNotNull(serializedObjectValue.getValueSerialized());
  }

  @Test
  public void testGetVariableLocalTypedInvalidCaseExecutionId() {
    try {
      caseService.getVariableLocalTyped("invalid", "aVariableName");
      fail("The case execution should not be found.");
    } catch (NotFoundException e) {

    }

    try {
      caseService.getVariableLocalTyped(null, "aVariableName");
      fail("The case execution should not be found.");
    } catch (NotValidException e) {

    }
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testRemoveVariable() {
    // given:
    // a deployed case definition
    // and an active case instance
    caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .setVariable("aVariableName", "abc")
        .create()
        .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService.removeVariable(caseExecutionId, "aVariableName");

    // then the variable should be gone
    assertThat(runtimeService.createVariableInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testRemoveVariables() {
    // given:
    // a deployed case definition
    // and an active case instance
    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "abc");
    variables.put("anotherVariable", 123);

    caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .setVariables(variables)
        .setVariable("aThirdVariable", "def")
        .create()
        .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when
    caseService.removeVariables(caseExecutionId, variables.keySet());

    // then there should be only one variable left
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();
    assertNotNull(variable);
    assertThat(variable.getName()).isEqualTo("aThirdVariable");
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testRemoveVariableLocal() {
    // given:
    // a deployed case definition
    // and an active case instance
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .create()
        .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    caseService.setVariableLocal(caseExecutionId, "aVariableName", "abc");

    // when
    caseService.removeVariableLocal(caseInstanceId, "aVariableName");

    // then the variable should still be there
    assertThat(runtimeService.createVariableInstanceQuery().count()).isEqualTo(1);

    // when
    caseService.removeVariableLocal(caseExecutionId, "aVariableName");

    // then the variable should be gone
    assertThat(runtimeService.createVariableInstanceQuery().count()).isEqualTo(0);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testRemoveVariablesLocal() {
    // given:
    // a deployed case definition
    // and an active case instance
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("oneTaskCase")
        .create()
        .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();


    Map<String, Object> variables = new HashMap<>();
    variables.put("aVariable", "abc");
    variables.put("anotherVariable", 123);

    caseService.setVariablesLocal(caseExecutionId, variables);
    caseService.setVariableLocal(caseExecutionId, "aThirdVariable", "def");

    // when
    caseService.removeVariablesLocal(caseInstanceId, variables.keySet());

    // then no variables should have been removed
    assertThat(runtimeService.createVariableInstanceQuery().count()).isEqualTo(3);

    // when
    caseService.removeVariablesLocal(caseExecutionId, variables.keySet());

    // then there should be only one variable left
    VariableInstance variable = runtimeService.createVariableInstanceQuery().singleResult();
    assertNotNull(variable);
    assertThat(variable.getName()).isEqualTo("aThirdVariable");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/loan-application.cmmn")
  @Test
  public void testCreateCaseInstanceById() {
    // given
    // there exists a deployment containing a case definition with key "loanApplication"

    CaseDefinition caseDefinition = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey("loanApplication")
      .singleResult();

    assertNotNull(caseDefinition);

    // when
    // create a new case instance by id

    CaseInstance caseInstance = caseService
      .withCaseDefinition(caseDefinition.getId())
      .create();

    // then
    // the returned caseInstance is not null

    assertNotNull(caseInstance);

    // verify that the case instance is persisted using the API

    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .caseInstanceId(caseInstance.getId())
      .singleResult();

    assertNotNull(instance);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/loan-application.cmmn")
  @Test
  public void testCreateCaseInstanceByKey() {
    // given
    // there exists a deployment containing a case definition with key "loanApplication"

    CaseDefinition caseDefinition = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey("loanApplication")
      .singleResult();

    assertNotNull(caseDefinition);

    // when
    // create a new case instance by key

    CaseInstance caseInstance = caseService
      .withCaseDefinitionByKey(caseDefinition.getKey())
      .create();

    // then
    // the returned caseInstance is not null

    assertNotNull(caseInstance);

    // verify that the case instance is persisted using the API

    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .caseInstanceId(caseInstance.getId())
      .singleResult();

    assertNotNull(instance);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/loan-application.cmmn")
  @Test
  public void testCaseExecutionQuery() {
    // given
    // there exists a deployment containing a case definition with key "loanApplication"

    CaseDefinition caseDefinition = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey("loanApplication")
      .singleResult();

    assertNotNull(caseDefinition);

    // when
    // create a new case instance by key

    CaseInstance caseInstance = caseService
      .withCaseDefinitionByKey(caseDefinition.getKey())
      .create();

    // then
    // the returned caseInstance is not null

    assertNotNull(caseInstance);

    // verify that there are three case execution:
    // - the case instance itself (ie. for the casePlanModel)
    // - a case execution for the stage
    // - a case execution for the humanTask

    List<CaseExecution> caseExecutions = caseService
        .createCaseExecutionQuery()
        .caseInstanceId(caseInstance.getId())
        .list();

    assertThat(caseExecutions).hasSize(3);

    CaseExecution casePlanModelExecution = caseService
        .createCaseExecutionQuery()
        .activityId("CasePlanModel_1")
        .singleResult();

    assertNotNull(casePlanModelExecution);

    CaseExecution stageExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1")
        .singleResult();

    assertNotNull(stageExecution);

    CaseExecution humanTaskExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_6")
        .singleResult();

    assertNotNull(humanTaskExecution);

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/loan-application.cmmn")
  @Test
  public void testCaseInstanceQuery() {
    // given
    // there exists a deployment containing a case definition with key "loanApplication"

    CaseDefinition caseDefinition = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey("loanApplication")
      .singleResult();

    assertNotNull(caseDefinition);

    // when
    // create a new case instance by key

    CaseInstance caseInstance = caseService
      .withCaseDefinitionByKey(caseDefinition.getKey())
      .create();

    // then
    // the returned caseInstance is not null

    assertNotNull(caseInstance);

    // verify that there is one caseInstance

    // only select ACTIVE case instances
    List<CaseInstance> caseInstances = caseService
        .createCaseInstanceQuery()
        .active()
        .list();

    assertThat(caseInstances).hasSize(1);
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariablesByEmptyList() {
    // given
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();

    // when
    Map<String, Object> variables = caseService.getVariables(caseInstanceId, new ArrayList<>());

    // then
    assertNotNull(variables);
    assertTrue(variables.isEmpty());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariablesTypedByEmptyList() {
    // given
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();

    // when
    Map<String, Object> variables = caseService.getVariablesTyped(caseInstanceId, new ArrayList<>(), false);

    // then
    assertNotNull(variables);
    assertTrue(variables.isEmpty());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariablesLocalByEmptyList() {
    // given
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();

    // when
    Map<String, Object> variables = caseService.getVariablesLocal(caseInstanceId, new ArrayList<>());

    // then
    assertNotNull(variables);
    assertTrue(variables.isEmpty());
  }

  @Deployment(resources={"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  public void testGetVariablesLocalTypedByEmptyList() {
    // given
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();

    // when
    Map<String, Object> variables = caseService.getVariablesLocalTyped(caseInstanceId, new ArrayList<>(), false);

    // then
    assertNotNull(variables);
    assertTrue(variables.isEmpty());
  }

}
