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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.repository.CaseDefinition;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionCommandBuilder;
import org.operaton.bpm.engine.runtime.CaseExecutionQuery;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.CaseInstanceQuery;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.ObjectValue;
import org.operaton.bpm.engine.variable.value.StringValue;

import static org.operaton.bpm.engine.variable.Variables.booleanValue;
import static org.operaton.bpm.engine.variable.Variables.createVariables;
import static org.operaton.bpm.engine.variable.Variables.integerValue;
import static org.operaton.bpm.engine.variable.Variables.stringValue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Roman Smirnov
 *
 */
class CaseServiceTest {

  @RegisterExtension
  protected static ProcessEngineExtension engineExtension = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineExtension);

  protected CaseService caseService;
  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;

  @Test
  void testCreateCaseInstanceQuery() {
    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    assertThat(query).isNotNull();
  }

  @Test
  void testCreateCaseExecutionQuery() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    assertThat(query).isNotNull();
  }

  @Test
  void testWithCaseExecution() {
    CaseExecutionCommandBuilder builder = caseService.withCaseExecution("aCaseExecutionId");

    assertThat(builder).isNotNull();
  }

  @Test
  void testManualStartInvalidCaseExecution() {
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution("invalid");
    assertThatThrownBy(commandBuilder::manualStart)
      .isInstanceOf(NotFoundException.class);

    CaseExecutionCommandBuilder commandBuilder2 = caseService.withCaseExecution(null);
    assertThatThrownBy(commandBuilder2::manualStart)
      .isInstanceOf(NotValidException.class);
  }

  @Test
  void testCompleteInvalidCaseExecution() {
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
  void testCloseInvalidCaseExecution() {
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
  void testTerminateInvalidCaseExecution() {
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution("invalid");
    assertThatThrownBy(commandBuilder::terminate)
      .withFailMessage("The case execution should not be found.")
      .isInstanceOf(NotFoundException.class);

    CaseExecutionCommandBuilder commandBuilder2 = caseService.withCaseExecution(null);
    assertThatThrownBy(commandBuilder2::terminate)
      .withFailMessage("The case execution should not be found.")
      .isInstanceOf(NotValidException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteSetVariable() {
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

    assertThat(result).isEmpty();

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
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
  void testExecuteSetVariableTyped() {
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

    assertThat(result).isEmpty();

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(stringValue("abc"));
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(integerValue(null));
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteSetVariables() {
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

    assertThat(result).isEmpty();

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
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
  void testExecuteSetVariablesTyped() {
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

    assertThat(result).isEmpty();

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(stringValue("abc"));
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(integerValue(null));
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteSetVariableAndVariables() {
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

    assertThat(result).isEmpty();

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else if ("aThirdVariable".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aThirdVariable");
        assertThat(variable.getValue()).isEqualTo(123);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteSetVariableAndVariablesTyped() {
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

    assertThat(result).isEmpty();

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(stringValue("abc"));
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(integerValue(null));
      } else if ("aThirdVariable".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aThirdVariable");
        assertThat(variable.getTypedValue()).isEqualTo(booleanValue(null));
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteSetVariableLocal() {
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

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
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
  void testExecuteSetVariablesLocal() {
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

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
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
  void testExecuteSetVariablesLocalTyped() {
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

    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(stringValue("abc"));
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getTypedValue()).isEqualTo(integerValue(null));
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteSetVariableLocalAndVariablesLocal() {
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

    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else if ("aThirdVariable".equals(variable.getName())) {
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

    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    for (VariableInstance variable : result) {

      assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
      assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");
      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);
      } else if ("aThirdVariable".equals(variable.getName())) {
        assertThat(variable.getName()).isEqualTo("aThirdVariable");
        assertThat(variable.getValue()).isEqualTo(123);
      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteSetVariableAndVariablesLocal() {
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

    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    VariableInstance aThirdVariable = result.get(0);

    assertThat(aThirdVariable).isNotNull();
    assertThat(aThirdVariable.getName()).isEqualTo("aThirdVariable");
    assertThat(aThirdVariable.getValue()).isEqualTo(123);

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(3);

    for (VariableInstance variable : result) {


      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");

      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(999);

      } else if ("aThirdVariable".equals(variable.getName())) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("aThirdVariable");
        assertThat(variable.getValue()).isEqualTo(123);

      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteRemoveVariable() {
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

    assertThat(result).isEmpty();

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(result).isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteRemoveVariables() {
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

    assertThat(result).isEmpty();

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(result).isEmpty();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteRemoveVariableAndVariables() {
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

    assertThat(result).isEmpty();

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(result).isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteRemoveVariableLocal() {
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

    assertThat(result).isEmpty();

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(result).isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteRemoveVariablesLocal() {
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

    assertThat(result).isEmpty();

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(result).isEmpty();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteRemoveVariableLocalAndVariablesLocal() {
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

    assertThat(result).isEmpty();

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(result).isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteRemoveVariableAndVariablesLocal() {
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

    assertThat(result).isEmpty();

    // query by caseInstanceId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseInstanceIdIn(caseInstanceId)
        .list();

    assertThat(result).isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteRemoveAndSetSameVariable() {
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

    // when/then
    assertThatThrownBy(() -> caseService
        .withCaseExecution(caseExecutionId)
        .removeVariable("aVariableName")
        .setVariable("aVariableName", "xyz")
        .execute())
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("Cannot set and remove a variable with the same variable name: 'aVariableName' within a command.");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testExecuteRemoveAndSetSameLocal() {
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

    // when/then
    assertThatThrownBy(() -> caseService
        .withCaseExecution(caseExecutionId)
        .setVariableLocal("aVariableName", "xyz")
        .removeVariableLocal("aVariableName")
        .execute())
      .isInstanceOf(NotValidException.class)
      .hasMessageContaining("Cannot set and remove a variable with the same variable name: 'aVariableName' within a command.");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariables() {
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
    verifyGetVariables(variables);

    assertThat(caseService.getVariablesTyped(caseExecutionId, true)).isEqualTo(variables);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariablesTyped() {
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
    verifyGetVariables(variables);

    assertThat(caseService.getVariablesTyped(caseExecutionId, true)).isEqualTo(variables);
  }

  @Test
  void testGetVariablesInvalidCaseExecutionId() {

    assertThatThrownBy(() -> caseService.getVariables("invalid"), "The case execution should not be found.")
        .isInstanceOf(NotFoundException.class);

    assertThatThrownBy(() -> caseService.getVariables(null), "The case execution should not be found.")
        .isInstanceOf(NotValidException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariablesWithVariableNames() {
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
        .setVariable("thirdVariable", "xyz")
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
    verifyGetVariables(variables);

    assertThat(caseService.getVariables(caseExecutionId, names)).isEqualTo(variables);
  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariablesWithVariableNamesTyped() {
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
        .setVariable("thirdVariable", "xyz")
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
    verifyGetVariables(variables);

    assertThat(caseService.getVariables(caseExecutionId, names)).isEqualTo(variables);
  }

  @Test
  void testGetVariablesWithVariablesNamesInvalidCaseExecutionId() {
    assertThatThrownBy(() -> caseService.getVariables("invalid", null), "The case execution should not be found.")
        .isInstanceOf(NotFoundException.class);

    assertThatThrownBy(() -> caseService.getVariables(null, null), "The case execution should not be found.")
        .isInstanceOf(NotValidException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariablesLocal() {
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
    verifyGetVariables(variables);

    assertThat(caseService.getVariablesLocal(caseExecutionId)).isEqualTo(variables);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariablesLocalTyped() {
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
    verifyGetVariables(variables);

    assertThat(caseService.getVariablesLocalTyped(caseExecutionId, true)).isEqualTo(variables);
  }

  @Test
  void testGetVariablesLocalInvalidCaseExecutionId() {
    assertThatThrownBy(() -> caseService.getVariablesLocal("invalid"), "The case execution should not be found.")
        .isInstanceOf(NotFoundException.class);

    assertThatThrownBy(() -> caseService.getVariablesLocal(null), "The case execution should not be found.")
        .isInstanceOf(NotValidException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariablesLocalWithVariableNames() {
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
    verifyGetVariables(variables);

    assertThat(caseService.getVariablesLocal(caseExecutionId, names)).isEqualTo(variables);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariablesLocalWithVariableNamesTyped() {
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
    verifyGetVariables(variables);

    assertThat(caseService.getVariablesLocal(caseExecutionId, names)).isEqualTo(variables);
  }

  @Test
  void testGetVariablesLocalWithVariablesNamesInvalidCaseExecutionId() {

    assertThatThrownBy(() -> caseService.getVariablesLocal("invalid", null), "The case execution should not be found.")
        .isInstanceOf(NotFoundException.class);

    assertThatThrownBy(() -> caseService.getVariablesLocal(null, null), "The case execution should not be found.")
        .isInstanceOf(NotValidException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariable() {
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
        .setVariable("thirdVariable", "xyz")
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
    assertThat(value)
            .isNotNull()
            .isEqualTo("abc");
  }

  @Test
  void testGetVariableInvalidCaseExecutionId() {
    assertThatThrownBy(() -> caseService.getVariable("invalid", "aVariableName")).isInstanceOf(NotFoundException.class);

    assertThatThrownBy(() -> caseService.getVariable(null, "aVariableName")).isInstanceOf(NotValidException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariableLocal() {
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
    assertThat(value)
            .isNotNull()
            .isEqualTo("abc");
  }

  @Test
  void testGetVariableLocalInvalidCaseExecutionId() {
    assertThatThrownBy(() -> caseService.getVariableLocal("invalid", "aVariableName"), "The case execution should not be found.")
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> caseService.getVariableLocal(null, "aVariableName"), "The case execution should not be found.")
        .isInstanceOf(NotValidException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariableTyped() {
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
        .setVariable("aSerializedObject", Variables.objectValue(List.of("1", "2")).create())
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
    assertThat(stringValue.getValue()).isNotNull();
    assertThat(objectValue.getValue()).isNotNull();
    assertThat(objectValue.isDeserialized()).isTrue();
    assertThat(objectValue.getValue()).isEqualTo(List.of("1", "2"));
    assertThat(serializedObjectValue.isDeserialized()).isFalse();
    assertThat(serializedObjectValue.getValueSerialized()).isNotNull();
  }

  @Test
  void testGetVariableTypedInvalidCaseExecutionId() {
    assertThatThrownBy(() -> caseService.getVariableTyped("invalid", "aVariableName"), "The case execution should not be found.")
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> caseService.getVariableTyped(null, "aVariableName"), "The case execution should not be found.")
        .isInstanceOf(NotValidException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testSetVariable() {
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

    assertThat(result).isEmpty();

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseInstanceId)
        .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    VariableInstance variable = result.get(0);
    assertThat(variable.getName()).isEqualTo("aVariableName");
    assertThat(variable.getValue()).isEqualTo("abc");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testSetVariables() {
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

    assertThat(result).isEmpty();

    // query by case instance id
    result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseInstanceId)
        .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variable : result) {
      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");

      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseInstanceId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(123);

      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testSetVariableLocal() {
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

    assertThat(result).isEmpty();

    // query by caseExecutionId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(1);

    VariableInstance variable = result.get(0);
    assertThat(variable.getName()).isEqualTo("aVariableName");
    assertThat(variable.getValue()).isEqualTo("abc");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testSetVariablesLocal() {
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

    assertThat(result).isEmpty();

    // query by caseExecutionId
    result = runtimeService
        .createVariableInstanceQuery()
        .caseExecutionIdIn(caseExecutionId)
        .list();

    assertThat(result)
            .isNotEmpty()
            .hasSize(2);

    for (VariableInstance variable : result) {
      if ("aVariableName".equals(variable.getName())) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("aVariableName");
        assertThat(variable.getValue()).isEqualTo("abc");

      } else if ("anotherVariableName".equals(variable.getName())) {
        assertThat(variable.getCaseExecutionId()).isEqualTo(caseExecutionId);
        assertThat(variable.getCaseInstanceId()).isEqualTo(caseInstanceId);

        assertThat(variable.getName()).isEqualTo("anotherVariableName");
        assertThat(variable.getValue()).isEqualTo(123);

      } else {
        fail("Unexpected variable: " + variable.getName());
      }
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariableTypedLocal() {
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
        .setVariableLocal("aSerializedObject", Variables.objectValue(List.of("1", "2")).create())
        .execute();

     // when
     StringValue stringValue = caseService.getVariableLocalTyped(caseExecutionId, "aVariableName");
     ObjectValue objectValue = caseService.getVariableLocalTyped(caseExecutionId, "aSerializedObject");
     ObjectValue serializedObjectValue = caseService.getVariableLocalTyped(caseExecutionId, "aSerializedObject", false);

    // then
    assertThat(stringValue.getValue()).isNotNull();
    assertThat(objectValue.getValue()).isNotNull();
    assertThat(objectValue.isDeserialized()).isTrue();
    assertThat(objectValue.getValue()).isEqualTo(List.of("1", "2"));
    assertThat(serializedObjectValue.isDeserialized()).isFalse();
    assertThat(serializedObjectValue.getValueSerialized()).isNotNull();
  }

  @Test
  void testGetVariableLocalTypedInvalidCaseExecutionId() {
    assertThatThrownBy(() -> caseService.getVariableLocalTyped("invalid", "aVariableName"), "The case execution should not be found.")
        .isInstanceOf(NotFoundException.class);
    assertThatThrownBy(() -> caseService.getVariableLocalTyped(null, "aVariableName"), "The case execution should not be found.")
        .isInstanceOf(NotValidException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testRemoveVariable() {
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
    assertThat(runtimeService.createVariableInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testRemoveVariables() {
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
    assertThat(variable).isNotNull();
    assertThat(variable.getName()).isEqualTo("aThirdVariable");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testRemoveVariableLocal() {
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
    assertThat(runtimeService.createVariableInstanceQuery().count()).isOne();

    // when
    caseService.removeVariableLocal(caseExecutionId, "aVariableName");

    // then the variable should be gone
    assertThat(runtimeService.createVariableInstanceQuery().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testRemoveVariablesLocal() {
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
    assertThat(variable).isNotNull();
    assertThat(variable.getName()).isEqualTo("aThirdVariable");
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/loan-application.cmmn")
  @Test
  void testCreateCaseInstanceById() {
    // given
    // there exists a deployment containing a case definition with key "loanApplication"

    CaseDefinition caseDefinition = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey("loanApplication")
      .singleResult();

    assertThat(caseDefinition).isNotNull();

    // when
    // create a new case instance by id

    CaseInstance caseInstance = caseService
      .withCaseDefinition(caseDefinition.getId())
      .create();

    // then
    // the returned caseInstance is not null

    assertThat(caseInstance).isNotNull();

    // verify that the case instance is persisted using the API

    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .caseInstanceId(caseInstance.getId())
      .singleResult();

    assertThat(instance).isNotNull();

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/loan-application.cmmn")
  @Test
  void testCreateCaseInstanceByKey() {
    // given
    // there exists a deployment containing a case definition with key "loanApplication"

    CaseDefinition caseDefinition = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey("loanApplication")
      .singleResult();

    assertThat(caseDefinition).isNotNull();

    // when
    // create a new case instance by key

    CaseInstance caseInstance = caseService
      .withCaseDefinitionByKey(caseDefinition.getKey())
      .create();

    // then
    // the returned caseInstance is not null

    assertThat(caseInstance).isNotNull();

    // verify that the case instance is persisted using the API

    CaseInstance instance = caseService
      .createCaseInstanceQuery()
      .caseInstanceId(caseInstance.getId())
      .singleResult();

    assertThat(instance).isNotNull();

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/loan-application.cmmn")
  @Test
  void testCaseExecutionQuery() {
    // given
    // there exists a deployment containing a case definition with key "loanApplication"

    CaseDefinition caseDefinition = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey("loanApplication")
      .singleResult();

    assertThat(caseDefinition).isNotNull();

    // when
    // create a new case instance by key

    CaseInstance caseInstance = caseService
      .withCaseDefinitionByKey(caseDefinition.getKey())
      .create();

    // then
    // the returned caseInstance is not null

    assertThat(caseInstance).isNotNull();

    // verify that there are three case execution:
    // - the case instance itself (i.e. for the casePlanModel)
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

    assertThat(casePlanModelExecution).isNotNull();

    CaseExecution stageExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1")
        .singleResult();

    assertThat(stageExecution).isNotNull();

    CaseExecution humanTaskExecution = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_6")
        .singleResult();

    assertThat(humanTaskExecution).isNotNull();

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/loan-application.cmmn")
  @Test
  void testCaseInstanceQuery() {
    // given
    // there exists a deployment containing a case definition with key "loanApplication"

    CaseDefinition caseDefinition = repositoryService
      .createCaseDefinitionQuery()
      .caseDefinitionKey("loanApplication")
      .singleResult();

    assertThat(caseDefinition).isNotNull();

    // when
    // create a new case instance by key

    CaseInstance caseInstance = caseService
      .withCaseDefinitionByKey(caseDefinition.getKey())
      .create();

    // then
    // the returned caseInstance is not null

    assertThat(caseInstance).isNotNull();

    // verify that there is one caseInstance

    // only select ACTIVE case instances
    List<CaseInstance> caseInstances = caseService
        .createCaseInstanceQuery()
        .active()
        .list();

    assertThat(caseInstances).hasSize(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariablesByEmptyList() {
    // given
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();

    // when
    Map<String, Object> variables = caseService.getVariables(caseInstanceId, new ArrayList<>());

    // then
    assertThat(variables).isNotNull().isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariablesTypedByEmptyList() {
    // given
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();

    // when
    Map<String, Object> variables = caseService.getVariablesTyped(caseInstanceId, new ArrayList<>(), false);

    // then
    assertThat(variables).isNotNull().isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariablesLocalByEmptyList() {
    // given
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();

    // when
    Map<String, Object> variables = caseService.getVariablesLocal(caseInstanceId, new ArrayList<>());

    // then
    assertThat(variables).isNotNull().isEmpty();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testGetVariablesLocalTypedByEmptyList() {
    // given
    String caseInstanceId = caseService.createCaseInstanceByKey("oneTaskCase").getId();

    // when
    Map<String, Object> variables = caseService.getVariablesLocalTyped(caseInstanceId, new ArrayList<>(), false);

    // then
    assertThat(variables).isNotNull().isEmpty();
  }

  protected void verifyGetVariables(Map<String, Object> variables) {
    assertThat(variables)
      .isNotNull()
      .isNotEmpty()
      .hasSize(2)
      .containsEntry("aVariableName", "abc")
      .containsEntry("anotherVariableName", 999);
  }

}
