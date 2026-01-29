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
package org.operaton.bpm.engine.test.api.runtime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.CaseInstanceQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class CaseInstanceQueryTest {

  private static final String CASE_DEFINITION_KEY = "oneTaskCase";
  private static final String CASE_DEFINITION_KEY_2 = "oneTaskCase2";

  RepositoryService repositoryService;
  CaseService caseService;
  RuntimeService runtimeService;

  private List<String> caseInstanceIds;

  /**
   * Setup starts 4 case instances of oneTaskCase
   * and 1 instance of oneTaskCase2
   */
  @BeforeEach
  void setUp() {

    repositoryService.createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
      .addClasspathResource("org/operaton/bpm/engine/test/api/cmmn/oneTaskCase2.cmmn")
      .deploy();

    caseInstanceIds = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      String id = caseService
          .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
          .businessKey(String.valueOf(i))
          .create()
          .getId();

      caseInstanceIds.add(id);
    }
    String id = caseService
        .withCaseDefinitionByKey(CASE_DEFINITION_KEY_2)
        .businessKey("1")
        .create()
        .getId();

    caseInstanceIds.add(id);
  }

  @AfterEach
  void tearDown() {
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  @Test
  void testCaseInstanceProperties() {
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY_2)
        .singleResult()
        .getId();

    CaseInstance caseInstance = caseService
      .createCaseInstanceQuery()
      .caseDefinitionKey(CASE_DEFINITION_KEY_2)
      .singleResult();

    assertThat(caseInstance.getId()).isNotNull();
    assertThat(caseInstance.getCaseInstanceId()).isEqualTo(caseInstance.getId());
    assertThat(caseInstance.getBusinessKey()).isEqualTo("1");
    assertThat(caseInstance.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(caseInstance.getActivityId()).isEqualTo("CasePlanModel_1");
    assertThat(caseInstance.getActivityName()).isNull();
    assertThat(caseInstance.getParentId()).isNull();
    assertThat(caseInstance.isActive()).isTrue();
    assertThat(caseInstance.isEnabled()).isFalse();

  }

  @Test
  void testQueryWithoutQueryParameter() {
    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    verifyQueryResults(query, 5);
  }

  @Test
  void testQueryByCaseDefinitionKey() {
    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.caseDefinitionKey(CASE_DEFINITION_KEY_2);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidCaseDefinitionKey() {
    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.caseDefinitionKey("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseDefinitionKey(null)).isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryByCaseDefinitionId() {
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY)
        .singleResult()
        .getId();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.caseDefinitionId(caseDefinitionId);

    verifyQueryResults(query, 4);
  }

  @Test
  void testQueryByInvalidCaseDefinitionId() {
    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.caseDefinitionId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseDefinitionId(null)).isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryByActive() {
    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.active();

    verifyQueryResults(query, 5);
  }

  @Test
  void testQueryByCompleted() {

    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

    repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/cmmn/oneTaskCaseWithManualActivation.cmmn")
        .deploy();

    for (int i = 0; i < 4; i++) {
      String id = caseService
          .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
          .businessKey(String.valueOf(i))
          .create()
          .getId();

      caseInstanceIds.add(id);
    }

    List<CaseExecution> executions = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .list();

    for (CaseExecution caseExecution : executions) {
      caseService
        .withCaseExecution(caseExecution.getId())
        .disable();
    }

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.completed();

    verifyQueryResults(query, 4);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/CaseInstanceQueryTest.testQueryByTerminated.cmmn"})
  @Test
  void testQueryByTerminated() {
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("termination")
        .create()
        .getId();

    String caseExecutionId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .caseInstanceId(caseInstanceId)
        .singleResult()
        .getId();

    caseService
      .withCaseExecution(caseExecutionId)
      .complete();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.terminated();

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByCaseInstanceBusinessKey() {
    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.caseInstanceBusinessKey("1");

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByInvalidCaseInstanceBusinessKey() {
    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.caseInstanceBusinessKey("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseInstanceBusinessKey(null)).isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryByCaseInstanceBusinessKeyAndCaseDefinitionKey() {
    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query
      .caseInstanceBusinessKey("0")
      .caseDefinitionKey(CASE_DEFINITION_KEY);

    verifyQueryResults(query, 1);

    query
      .caseInstanceBusinessKey("1")
      .caseDefinitionKey(CASE_DEFINITION_KEY);

    verifyQueryResults(query, 1);

    query
      .caseInstanceBusinessKey("2")
      .caseDefinitionKey(CASE_DEFINITION_KEY);

    verifyQueryResults(query, 1);

    query
      .caseInstanceBusinessKey("3")
      .caseDefinitionKey(CASE_DEFINITION_KEY);

    verifyQueryResults(query, 1);

    query
      .caseInstanceBusinessKey("1")
      .caseDefinitionKey(CASE_DEFINITION_KEY_2);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByCaseInstanceId() {
    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    for (String caseInstanceId : caseInstanceIds) {
      query.caseInstanceId(caseInstanceId);

      verifyQueryResults(query, 1);
    }

  }

  @Test
  void testQueryByInvalidCaseInstanceId() {
    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.caseInstanceId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseInstanceId(null)).isInstanceOf(NotValidException.class);

  }

  @Test
  void testQueryByNullVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueEquals("aNullValue", null);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByStringVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueEquals("aStringValue", "abc");

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByBooleanVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueEquals("aBooleanValue", true);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByShortVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueEquals("aShortValue", (short) 123);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByIntegerVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueEquals("anIntegerValue", 456);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByLongVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueEquals("aLongValue", (long) 789);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByDateVariableValueEquals() {
    Date now = new Date();
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDateValue", now)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueEquals("aDateValue", now);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByDoubleVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueEquals("aDoubleValue", 1.5);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByByteArrayVariableValueEquals() {
    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aByteArrayValue", bytes)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    var caseInstanceQuery = query.variableValueEquals("aByteArrayValue", bytes);

    assertThatThrownBy(caseInstanceQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryBySerializableVariableValueEquals() {
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aSerializableValue", serializable)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    var caseInstanceQuery = query.variableValueEquals("aSerializableValue", serializable);

    assertThatThrownBy(caseInstanceQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByStringVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueNotEquals("aStringValue", "abd");

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByBooleanVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueNotEquals("aBooleanValue", false);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByShortVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueNotEquals("aShortValue", (short) 124);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByIntegerVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueNotEquals("anIntegerValue", 457);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByLongVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueNotEquals("aLongValue", (long) 790);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByDateVariableValueNotEquals() {
    Date now = new Date();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDateValue", now)
      .create();

    Date before = new Date(now.getTime() - 100000);

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueNotEquals("aDateValue", before);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByDoubleVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueNotEquals("aDoubleValue", 1.6);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByByteArrayVariableValueNotEquals() {
    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aByteArrayValue", bytes)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    var caseInstanceQuery = query.variableValueNotEquals("aByteArrayValue", bytes);

    assertThatThrownBy(caseInstanceQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryBySerializableVariableValueNotEquals() {
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aSerializableValue", serializable)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    var caseInstanceQuery = query.variableValueNotEquals("aSerializableValue", serializable);

    assertThatThrownBy(caseInstanceQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByNullVariableValueGreaterThan() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var query = caseService.createCaseInstanceQuery();

    // when/then
    assertThatThrownBy(() -> query.variableValueGreaterThan("aNullValue", null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Booleans and null cannot be used in 'greater than' condition");
  }

  @Test
  void testQueryByStringVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThan("aStringValue", "ab");

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByBooleanVariableValueGreaterThan() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();
    var query = caseService.createCaseInstanceQuery();

    // when/then
    assertThatThrownBy(() -> query.variableValueGreaterThan("aBooleanValue", false))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Booleans and null cannot be used in 'greater than' condition");
  }

  @Test
  void testQueryByShortVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThan("aShortValue", (short) 122);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByIntegerVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThan("anIntegerValue", 455);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByLongVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThan("aLongValue", (long) 788);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByDateVariableValueGreaterThan() {
    Date now = new Date();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDateValue", now)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    Date before = new Date(now.getTime() - 100000);

    query.variableValueGreaterThan("aDateValue", before);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByDoubleVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThan("aDoubleValue", 1.4);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByByteArrayVariableValueGreaterThan() {
    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aByteArrayValue", bytes)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    var caseInstanceQuery = query.variableValueGreaterThan("aByteArrayValue", bytes);

    assertThatThrownBy(caseInstanceQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryBySerializableVariableGreaterThan() {
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aSerializableValue", serializable)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    var caseInstanceQuery = query.variableValueGreaterThan("aSerializableValue", serializable);

    assertThatThrownBy(caseInstanceQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByNullVariableValueGreaterThanOrEqual() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var query = caseService.createCaseInstanceQuery();

    // when/then
    assertThatThrownBy(() -> query.variableValueGreaterThanOrEqual("aNullValue", null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Booleans and null cannot be used in 'greater than or equal' condition");
  }

  @Test
  void testQueryByStringVariableValueGreaterThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThanOrEqual("aStringValue", "ab");

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThanOrEqual("aStringValue", "abc");

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByBooleanVariableValueGreaterThanOrEqual() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();
    var query = caseService.createCaseInstanceQuery();

    // when/then
    assertThatThrownBy(() -> query.variableValueGreaterThanOrEqual("aBooleanValue", false))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Booleans and null cannot be used in 'greater than or equal' condition");
  }

  @Test
  void testQueryByShortVariableValueGreaterThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThanOrEqual("aShortValue", (short) 122);

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThanOrEqual("aShortValue", (short) 123);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByIntegerVariableValueGreaterThanOrEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThanOrEqual("anIntegerValue", 455);

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThanOrEqual("anIntegerValue", 456);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByLongVariableValueGreaterThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThanOrEqual("aLongValue", (long) 788);

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThanOrEqual("aLongValue", (long) 789);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByDateVariableValueGreaterThanOrEqual() {
    Date now = new Date();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDateValue", now)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    Date before = new Date(now.getTime() - 100000);

    query.variableValueGreaterThanOrEqual("aDateValue", before);

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThanOrEqual("aDateValue", now);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByDoubleVariableValueGreaterThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThanOrEqual("aDoubleValue", 1.4);

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueGreaterThanOrEqual("aDoubleValue", 1.5);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByByteArrayVariableValueGreaterThanOrEqual() {
    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aByteArrayValue", bytes)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    var caseInstanceQuery = query.variableValueGreaterThanOrEqual("aByteArrayValue", bytes);

    assertThatThrownBy(caseInstanceQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryBySerializableVariableGreaterThanOrEqual() {
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aSerializableValue", serializable)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    var caseInstanceQuery = query.variableValueGreaterThanOrEqual("aSerializableValue", serializable);

    assertThatThrownBy(caseInstanceQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByNullVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var query = caseService.createCaseInstanceQuery();

    assertThatThrownBy(() -> query.variableValueLessThan("aNullValue", null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Booleans and null cannot be used in 'less than' condition");
  }

  @Test
  void testQueryByStringVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueLessThan("aStringValue", "abd");

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByBooleanVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();
    var query = caseService.createCaseInstanceQuery();

    assertThatThrownBy(() -> query.variableValueLessThan("aBooleanValue", false))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Booleans and null cannot be used in 'less than' condition");
  }

  @Test
  void testQueryByShortVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueLessThan("aShortValue", (short) 124);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByIntegerVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueLessThan("anIntegerValue", 457);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByLongVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueLessThan("aLongValue", (long) 790);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByDateVariableValueLessThan() {
    Date now = new Date();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDateValue", now)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    Date after = new Date(now.getTime() + 100000);

    query.variableValueLessThan("aDateValue", after);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByDoubleVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueLessThan("aDoubleValue", 1.6);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByByteArrayVariableValueLessThan() {
    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aByteArrayValue", bytes)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    var caseInstanceQuery = query.variableValueLessThan("aByteArrayValue", bytes);

    assertThatThrownBy(caseInstanceQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryBySerializableVariableLessThan() {
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aSerializableValue", serializable)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    var caseInstanceQuery = query.variableValueLessThan("aSerializableValue", serializable);

    assertThatThrownBy(caseInstanceQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByNullVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var query = caseService.createCaseInstanceQuery();

    assertThatThrownBy(() -> query.variableValueLessThanOrEqual("aNullValue", null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Booleans and null cannot be used in 'less than or equal' condition");

  }

  @Test
  void testQueryByStringVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueLessThanOrEqual("aStringValue", "abd");

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueLessThanOrEqual("aStringValue", "abc");

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByBooleanVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();
    var query = caseService.createCaseInstanceQuery();

    assertThatThrownBy(() -> query.variableValueLessThanOrEqual("aBooleanValue", false))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Booleans and null cannot be used in 'less than or equal' condition");

  }

  @Test
  void testQueryByShortVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueLessThanOrEqual("aShortValue", (short) 124);

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueLessThanOrEqual("aShortValue", (short) 123);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByIntegerVariableValueLessThanOrEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueLessThanOrEqual("anIntegerValue", 457);

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueLessThanOrEqual("anIntegerValue", 456);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByLongVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueLessThanOrEqual("aLongValue", (long) 790);

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueLessThanOrEqual("aLongValue", (long) 789);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByDateVariableValueLessThanOrEqual() {
    Date now = new Date();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDateValue", now)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    Date after = new Date(now.getTime() + 100000);

    query.variableValueLessThanOrEqual("aDateValue", after);

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueLessThanOrEqual("aDateValue", now);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByDoubleVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueLessThanOrEqual("aDoubleValue", 1.6);

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueLessThanOrEqual("aDoubleValue", 1.5);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByByteArrayVariableValueLessThanOrEqual() {
    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aByteArrayValue", bytes)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    var caseInstanceQuery = query.variableValueLessThanOrEqual("aByteArrayValue", bytes);

    assertThatThrownBy(caseInstanceQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryBySerializableVariableLessThanOrEqual() {
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aSerializableValue", serializable)
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    var caseInstanceQuery = query.variableValueLessThanOrEqual("aSerializableValue", serializable);

    assertThatThrownBy(caseInstanceQuery::list).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testQueryByNullVariableValueLike() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var query = caseService.createCaseInstanceQuery();

    assertThatThrownBy(() -> query.variableValueLike("aNullValue", null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Booleans and null cannot be used in 'like' condition");

  }

  @Test
  void testQueryByStringVariableValueLike() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    query.variableValueLike("aStringValue", "ab%");

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueLike("aStringValue", "%bc");

    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();

    query.variableValueLike("aStringValue", "%b%");

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByNullVariableValueNotLike() {
    caseService
            .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
            .setVariable("aNullValue", null)
            .create();
    var query = caseService.createCaseInstanceQuery();

    assertThatThrownBy(() -> query.variableValueNotLike("aNullValue", null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Booleans and null cannot be used in 'not like' condition");

  }

  @Test
  void testQueryByStringVariableValueNotLike() {
    caseService
            .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
            .setVariable("aStringValue", "abc")
            .create();

    CaseInstanceQuery query = caseService.createCaseInstanceQuery();
    query.variableValueNotLike("aStringValue", "abc%");
    verifyQueryResults(query, 0);

    query = caseService.createCaseInstanceQuery();
    query.variableValueNotLike("aStringValue", "abd%");
    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();
    query.variableValueNotLike("aStringValue", "%bd");
    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();
    query.variableValueNotLike("aStringValue", "%d%");
    verifyQueryResults(query, 1);

    query = caseService.createCaseInstanceQuery();
    query.variableValueNotLike("nonExistentValue", "%abc%");
    verifyQueryResults(query, 0);
  }

  @Test
  void testQuerySorting() {
    CaseInstanceQuery query = caseService.createCaseInstanceQuery();

    // asc
    query
      .orderByCaseDefinitionId()
      .asc();
    verifyQueryResults(query, 5);

    query = caseService.createCaseInstanceQuery();

    query
      .orderByCaseDefinitionKey()
      .asc();
    verifyQueryResults(query, 5);

    query = caseService.createCaseInstanceQuery();

    query
      .orderByCaseInstanceId()
      .asc();
    verifyQueryResults(query, 5);

    // desc

    query = caseService.createCaseInstanceQuery();

    query
      .orderByCaseDefinitionId()
      .desc();
    verifyQueryResults(query, 5);

    query = caseService.createCaseInstanceQuery();

    query
      .orderByCaseDefinitionKey()
      .desc();
    verifyQueryResults(query, 5);

    query = caseService.createCaseInstanceQuery();

    query
      .orderByCaseInstanceId()
      .desc();
    verifyQueryResults(query, 5);
  }

  @Test
  void testCaseVariableValueEqualsNumber() {
    // long
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("var", 123L)
      .create();

    // non-matching long
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("var", 12345L)
      .create();

    // short
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("var", (short) 123)
      .create();

    // double
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("var", 123.0d)
      .create();

    // integer
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("var", 123)
      .create();

    // untyped null (should not match)
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("var", null)
      .create();

    // typed null (should not match)
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("var", Variables.longValue(null))
      .create();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("var", "123")
      .create();

    assertThat(caseService.createCaseInstanceQuery().variableValueEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(caseService.createCaseInstanceQuery().variableValueEquals("var", Variables.numberValue(123L)).count()).isEqualTo(4);
    assertThat(caseService.createCaseInstanceQuery().variableValueEquals("var", Variables.numberValue(123.0d)).count()).isEqualTo(4);
    assertThat(caseService.createCaseInstanceQuery().variableValueEquals("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);

    assertThat(caseService.createCaseInstanceQuery().variableValueEquals("var", Variables.numberValue(null)).count()).isOne();

    // other operators
    assertThat(caseService.createCaseInstanceQuery().variableValueNotEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(caseService.createCaseInstanceQuery().variableValueGreaterThan("var", Variables.numberValue(123L)).count()).isOne();
    assertThat(caseService.createCaseInstanceQuery().variableValueGreaterThanOrEqual("var", Variables.numberValue(123.0d)).count()).isEqualTo(5);
    assertThat(caseService.createCaseInstanceQuery().variableValueLessThan("var", Variables.numberValue((short) 123)).count()).isZero();
    assertThat(caseService.createCaseInstanceQuery().variableValueLessThanOrEqual("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/superProcessWithCaseCallActivity.bpmn20.xml"})
  @Test
  void testQueryBySuperProcessInstanceId() {
    String superProcessInstanceId = runtimeService.startProcessInstanceByKey("subProcessQueryTest").getId();

    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .superProcessInstanceId(superProcessInstanceId);

    verifyQueryResults(query, 1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/runtime/superProcessWithCaseCallActivityInsideSubProcess.bpmn20.xml"})
  @Test
  void testQueryBySuperProcessInstanceIdNested() {
    String superProcessInstanceId = runtimeService.startProcessInstanceByKey("subProcessQueryTest").getId();

    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .superProcessInstanceId(superProcessInstanceId);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidSuperProcessInstanceId() {
    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .superProcessInstanceId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.superProcessInstanceId(null)).isInstanceOf(NotValidException.class);

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneProcessTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml"})
  @Test
  void testQueryBySubProcessInstanceId() {
    String superCaseInstanceId = caseService.createCaseInstanceByKey("oneProcessTaskCase").getId();

    String subProcessInstanceId = runtimeService
        .createProcessInstanceQuery()
        .superCaseInstanceId(superCaseInstanceId)
        .singleResult()
        .getId();

    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .subProcessInstanceId(subProcessInstanceId);

    verifyQueryResults(query, 1);

    CaseInstance caseInstance = query.singleResult();
    assertThat(caseInstance.getId()).isEqualTo(superCaseInstanceId);
  }

  @Test
  void testQueryByInvalidSubProcessInstanceId() {
    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .subProcessInstanceId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.subProcessInstanceId(null)).isInstanceOf(NotValidException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn"})
  @Test
  void testQueryBySuperCaseInstanceId() {
    String superCaseInstanceId = caseService.createCaseInstanceByKey("oneCaseTaskCase").getId();

    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .superCaseInstanceId(superCaseInstanceId);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByInvalidSuperCaseInstanceId() {
    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .superCaseInstanceId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.superCaseInstanceId(null)).isInstanceOf(NotValidException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn"})
  @Test
  void testQueryBySubCaseInstanceId() {
    String superCaseInstanceId = caseService.createCaseInstanceByKey("oneCaseTaskCase").getId();

    String subCaseInstanceId = caseService
        .createCaseInstanceQuery()
        .superCaseInstanceId(superCaseInstanceId)
        .singleResult()
        .getId();

    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .subCaseInstanceId(subCaseInstanceId);

    verifyQueryResults(query, 1);

    CaseInstance caseInstance = query.singleResult();
    assertThat(caseInstance.getId()).isEqualTo(superCaseInstanceId);
  }

  @Test
  void testQueryByInvalidSubCaseInstanceId() {
    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .subCaseInstanceId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.subCaseInstanceId(null)).isInstanceOf(NotValidException.class);
  }

  @Test
  void testQueryByDeploymentId() {
    String deploymentId = repositoryService
        .createDeploymentQuery()
        .singleResult()
        .getId();

    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .deploymentId(deploymentId);

    verifyQueryResults(query, 5);
  }

  @Test
  void testQueryByInvalidDeploymentId() {
    CaseInstanceQuery query = caseService
        .createCaseInstanceQuery()
        .deploymentId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.deploymentId(null)).isInstanceOf(NotValidException.class);
  }

}
