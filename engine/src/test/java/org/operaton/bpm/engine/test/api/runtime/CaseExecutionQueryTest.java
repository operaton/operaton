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
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionQuery;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.NullTolerantComparator;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.caseExecutionByDefinitionId;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.caseExecutionByDefinitionKey;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.caseExecutionById;
import static org.operaton.bpm.engine.test.api.runtime.TestOrderingUtil.inverted;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class CaseExecutionQueryTest {

  private static final String CASE_DEFINITION_KEY = "oneTaskCase";
  private static final String CASE_DEFINITION_KEY_2 = "twoTaskCase";

  ProcessEngine processEngine;
  RepositoryService repositoryService;
  CaseService caseService;

  /**
   * Setup starts 4 case instances of oneTaskCase
   * and 1 instance of twoTaskCase
   */
  @BeforeEach
  void setUp() {

    repositoryService.createDeployment()
      .addClasspathResource("org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
      .addClasspathResource("org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn")
      .deploy();

    for (int i = 0; i < 4; i++) {
      caseService
        .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
        .businessKey(i + "")
        .create();
    }
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY_2)
      .businessKey("1")
      .create();
  }

  @AfterEach
  void tearDown() {
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  protected void verifyQueryWithOrdering(CaseExecutionQuery query, int countExpected, NullTolerantComparator<CaseExecution> expectedOrdering) {
    verifyQueryResults(query, countExpected);
    TestOrderingUtil.verifySorting(query.list(), expectedOrdering);
  }

  @Test
  void testQueryWithoutQueryParameter() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    verifyQueryResults(query, 11);
  }

  @Test
  void testQueryByCaseDefinitionKey() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseDefinitionKey(CASE_DEFINITION_KEY);

    verifyQueryResults(query, 8);

    query.caseDefinitionKey(CASE_DEFINITION_KEY_2);

    verifyQueryResults(query, 3);
  }

  @Test
  void testQueryByInvalidCaseDefinitionKey() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseDefinitionKey("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseDefinitionKey(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("caseDefinitionKey is null");
  }

  @Test
  void testQueryByCaseDefinitionId() {
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY)
        .singleResult()
        .getId();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseDefinitionId(caseDefinitionId);

    verifyQueryResults(query, 8);

    caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY_2)
        .singleResult()
        .getId();

    query.caseDefinitionId(caseDefinitionId);

    verifyQueryResults(query, 3);
  }

  @Test
  void testQueryByInvalidCaseDefinitionId() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseDefinitionId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseDefinitionId(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("caseDefinitionId is null");
  }

  @Test
  void testQueryByCaseInstaceId() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    List<CaseInstance> caseInstances = caseService
        .createCaseInstanceQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY)
        .list();

    for (CaseInstance caseInstance : caseInstances) {
      query.caseInstanceId(caseInstance.getId());

      verifyQueryResults(query, 2);
    }

    CaseInstance instance = caseService
        .createCaseInstanceQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY_2)
        .singleResult();

    query.caseInstanceId(instance.getId());

    verifyQueryResults(query, 3);
  }

  @Test
  void testQueryByInvalidCaseInstanceId() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseInstanceId(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("caseInstanceId is null");
  }

  @Test
  void testQueryByCaseInstanceBusinessKey() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceBusinessKey("0");

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByInvalidCaseInstanceBusinessKey() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceBusinessKey("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseInstanceBusinessKey(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("caseInstanceBusinessKey is null");
  }

  @Test
  void testQueryByCaseInstanceBusinessKeyAndCaseDefinitionKey() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query
      .caseInstanceBusinessKey("0")
      .caseDefinitionKey(CASE_DEFINITION_KEY);

    verifyQueryResults(query, 2);

    query
      .caseInstanceBusinessKey("1")
      .caseDefinitionKey(CASE_DEFINITION_KEY);

    verifyQueryResults(query, 2);

    query
      .caseInstanceBusinessKey("2")
      .caseDefinitionKey(CASE_DEFINITION_KEY);

    verifyQueryResults(query, 2);

    query
      .caseInstanceBusinessKey("3")
      .caseDefinitionKey(CASE_DEFINITION_KEY);

    verifyQueryResults(query, 2);

    query
      .caseInstanceBusinessKey("1")
      .caseDefinitionKey(CASE_DEFINITION_KEY_2);

    verifyQueryResults(query, 3);

  }

  @Test
  void testQueryByCaseExecutionId() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    List<CaseExecution> executions = caseService
        .createCaseExecutionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY_2)
        .list();

    for (CaseExecution execution : executions) {
      query.caseExecutionId(execution.getId());

      verifyQueryResults(query, 1);
    }

  }

  @Test
  void testQueryByInvalidCaseExecutionId() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseExecutionId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.caseExecutionId(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("caseExecutionId is null");
  }

  @Test
  void testQueryByActivityId() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.activityId("PI_HumanTask_1");

    verifyQueryResults(query, 5);

    query.activityId("PI_HumanTask_2");

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByInvalidActivityId() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.activityId("invalid");

    verifyQueryResults(query, 0);

    assertThatThrownBy(() -> query.activityId(null))
      .isInstanceOf(NotValidException.class)
      .hasMessage("activityId is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneMilestoneCase.cmmn"})
  @Test
  void testQueryByAvailable() {
    caseService
      .withCaseDefinitionByKey("oneMilestoneCase")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.available();

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByEnabled() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.enabled();

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByActive() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.active();

    verifyQueryResults(query, 9);

  }

  @Test
  void testQueryByDisabled() {
    List<CaseExecution> caseExecutions= caseService
        .createCaseExecutionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY_2)
        .activityId("PI_HumanTask_1")
        .list();

    for (CaseExecution caseExecution : caseExecutions) {
      caseService
        .withCaseExecution(caseExecution.getId())
        .disable();
    }

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.disabled();

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByNullVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueEquals("aNullValue", null);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByStringVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueEquals("aStringValue", "abc");

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByBooleanVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueEquals("aBooleanValue", true);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByShortVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueEquals("aShortValue", (short) 123);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByIntegerVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueEquals("anIntegerValue", 456);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByLongVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueEquals("aDateValue", now);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByDoubleVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.variableValueEquals("aByteArrayValue", bytes);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Variables of type ByteArray cannot be used to query");
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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.variableValueEquals("aSerializableValue", serializable);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Object values cannot be used to query");
  }

  @Test
  void testQueryByStringVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueNotEquals("aStringValue", "abd");

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByBooleanVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueNotEquals("aBooleanValue", false);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByShortVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueNotEquals("aShortValue", (short) 124);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByIntegerVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueNotEquals("anIntegerValue", 457);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByLongVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueNotEquals("aDateValue", before);

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByDoubleVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.variableValueNotEquals("aByteArrayValue", bytes);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Variables of type ByteArray cannot be used to query");
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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.variableValueNotEquals("aSerializableValue", serializable);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Object values cannot be used to query");
  }

  @Test
  void testQueryByNullVariableValueGreaterThan() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.variableValueGreaterThan("aNullValue", null))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'greater than' condition");
  }

  @Test
  void testQueryByStringVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

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
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.variableValueGreaterThan("aBooleanValue", false))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'greater than' condition");
  }

  @Test
  void testQueryByShortVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueGreaterThan("aShortValue", (short) 122);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByIntegerVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueGreaterThan("anIntegerValue", 455);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByLongVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.variableValueGreaterThan("aByteArrayValue", bytes);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Variables of type ByteArray cannot be used to query");
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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.variableValueGreaterThan("aSerializableValue", serializable);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Object values cannot be used to query");
  }

  @Test
  void testQueryByNullVariableValueGreaterThanOrEqual() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.variableValueGreaterThanOrEqual("aNullValue", null))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'greater than or equal' condition");

  }

  @Test
  void testQueryByStringVariableValueGreaterThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueGreaterThanOrEqual("aStringValue", "ab");

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

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
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.variableValueGreaterThanOrEqual("aBooleanValue", false))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'greater than or equal' condition");

  }

  @Test
  void testQueryByShortVariableValueGreaterThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueGreaterThanOrEqual("aShortValue", (short) 122);

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

    query.variableValueGreaterThanOrEqual("aShortValue", (short) 123);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByIntegerVariableValueGreaterThanOrEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueGreaterThanOrEqual("anIntegerValue", 455);

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

    query.variableValueGreaterThanOrEqual("anIntegerValue", 456);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByLongVariableValueGreaterThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueGreaterThanOrEqual("aLongValue", (long) 788);

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    Date before = new Date(now.getTime() - 100000);

    query.variableValueGreaterThanOrEqual("aDateValue", before);

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

    query.variableValueGreaterThanOrEqual("aDateValue", now);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByDoubleVariableValueGreaterThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueGreaterThanOrEqual("aDoubleValue", 1.4);

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.variableValueGreaterThanOrEqual("aByteArrayValue", bytes);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Variables of type ByteArray cannot be used to query");
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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.variableValueGreaterThanOrEqual("aSerializableValue", serializable);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Object values cannot be used to query");
  }

  @Test
  void testQueryByNullVariableValueLessThan() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.variableValueLessThan("aNullValue", null))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'less than' condition");

  }

  @Test
  void testQueryByStringVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueLessThan("aStringValue", "abd");

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByBooleanVariableValueLessThan() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.variableValueLessThan("aBooleanValue", false))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'less than' condition");

  }

  @Test
  void testQueryByShortVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueLessThan("aShortValue", (short) 124);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByIntegerVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueLessThan("anIntegerValue", 457);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByLongVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.variableValueLessThan("aByteArrayValue", bytes);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Variables of type ByteArray cannot be used to query");
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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.variableValueLessThan("aSerializableValue", serializable);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Object values cannot be used to query");
  }

  @Test
  void testQueryByNullVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.variableValueLessThanOrEqual("aNullValue", null))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'less than or equal' condition");

  }

  @Test
  void testQueryByStringVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueLessThanOrEqual("aStringValue", "abd");

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

    query.variableValueLessThanOrEqual("aStringValue", "abc");

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByBooleanVariableValueLessThanOrEqual() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.variableValueLessThanOrEqual("aBooleanValue", false))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'less than or equal' condition");

  }

  @Test
  void testQueryByShortVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueLessThanOrEqual("aShortValue", (short) 124);

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

    query.variableValueLessThanOrEqual("aShortValue", (short) 123);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByIntegerVariableValueLessThanOrEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueLessThanOrEqual("anIntegerValue", 457);

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

    query.variableValueLessThanOrEqual("anIntegerValue", 456);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByLongVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueLessThanOrEqual("aLongValue", (long) 790);

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    Date after = new Date(now.getTime() + 100000);

    query.variableValueLessThanOrEqual("aDateValue", after);

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

    query.variableValueLessThanOrEqual("aDateValue", now);

    verifyQueryResults(query, 1);

  }

  @Test
  void testQueryByDoubleVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueLessThanOrEqual("aDoubleValue", 1.6);

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.variableValueLessThanOrEqual("aByteArrayValue", bytes);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Variables of type ByteArray cannot be used to query");
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

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.variableValueLessThanOrEqual("aSerializableValue", serializable);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Object values cannot be used to query");
  }

  @Test
  void testQueryByNullVariableValueLike() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.variableValueLike("aNullValue", null))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'like' condition");

  }

  @Test
  void testQueryByStringVariableValueLike() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.variableValueLike("aStringValue", "ab%");

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

    query.variableValueLike("aStringValue", "%bc");

    verifyQueryResults(query, 1);

    query = caseService.createCaseExecutionQuery();

    query.variableValueLike("aStringValue", "%b%");

    verifyQueryResults(query, 1);
  }

  @Test
  void testQueryByNullCaseInstanceVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueEquals("aNullValue", null);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByStringCaseInstanceVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueEquals("aStringValue", "abc");

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByBooleanCaseInstanceVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueEquals("aBooleanValue", true);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByShortCaseInstanceVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueEquals("aShortValue", (short) 123);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByIntegerCaseInstanceVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueEquals("anIntegerValue", 456);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByLongCaseInstanceVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueEquals("aLongValue", (long) 789);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByDateCaseInstanceVariableValueEquals() {
    Date now = new Date();
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDateValue", now)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueEquals("aDateValue", now);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByDoubleCaseInstanceVariableValueEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueEquals("aDoubleValue", 1.5);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByByteArrayCaseInstanceVariableValueEquals() {
    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aByteArrayValue", bytes)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.caseInstanceVariableValueEquals("aByteArrayValue", bytes);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Variables of type ByteArray cannot be used to query");
  }

  @Test
  void testQueryBySerializableCaseInstanceVariableValueEquals() {
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aSerializableValue", serializable)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.caseInstanceVariableValueEquals("aSerializableValue", serializable);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Object values cannot be used to query");
  }

  @Test
  void testQueryByStringCaseInstanceVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueNotEquals("aStringValue", "abd");

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByBooleanCaseInstanceVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueNotEquals("aBooleanValue", false);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByShortCaseInstanceVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueNotEquals("aShortValue", (short) 124);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByIntegerCaseInstanceVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueNotEquals("anIntegerValue", 457);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByLongCaseInstanceVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueNotEquals("aLongValue", (long) 790);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByDateCaseInstanceVariableValueNotEquals() {
    Date now = new Date();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDateValue", now)
      .create();

    Date before = new Date(now.getTime() - 100000);

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueNotEquals("aDateValue", before);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByDoubleCaseInstanceVariableValueNotEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueNotEquals("aDoubleValue", 1.6);

    verifyQueryResults(query, 2);
  }

  @Test
  void testQueryByByteArrayCaseInstanceVariableValueNotEquals() {
    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aByteArrayValue", bytes)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.caseInstanceVariableValueNotEquals("aByteArrayValue", bytes);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Variables of type ByteArray cannot be used to query");
  }

  @Test
  void testQueryBySerializableCaseInstanceVariableValueNotEquals() {
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aSerializableValue", serializable)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.caseInstanceVariableValueNotEquals("aSerializableValue", serializable);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Object values cannot be used to query");
  }

  @Test
  void testQueryByNullCaseInstanceVariableValueGreaterThan() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.caseInstanceVariableValueGreaterThan("aNullValue", null))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'greater than' condition");

  }

  @Test
  void testQueryByStringCaseInstanceVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThan("aStringValue", "ab");

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByBooleanCaseInstanceVariableValueGreaterThan() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.caseInstanceVariableValueGreaterThan("aBooleanValue", false))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'greater than' condition");

  }

  @Test
  void testQueryByShortCaseInstanceVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThan("aShortValue", (short) 122);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByIntegerCaseInstanceVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThan("anIntegerValue", 455);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByLongCaseInstanceVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThan("aLongValue", (long) 788);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByDateCaseInstanceVariableValueGreaterThan() {
    Date now = new Date();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDateValue", now)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    Date before = new Date(now.getTime() - 100000);

    query.caseInstanceVariableValueGreaterThan("aDateValue", before);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByDoubleCaseInstanceVariableValueGreaterThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThan("aDoubleValue", 1.4);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByByteArrayCaseInstanceVariableValueGreaterThan() {
    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aByteArrayValue", bytes)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.caseInstanceVariableValueGreaterThan("aByteArrayValue", bytes);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Variables of type ByteArray cannot be used to query");
  }

  @Test
  void testQueryBySerializableCaseInstanceVariableGreaterThan() {
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aSerializableValue", serializable)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.caseInstanceVariableValueGreaterThan("aSerializableValue", serializable);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Object values cannot be used to query");
  }

  @Test
  void testQueryByNullCaseInstanceVariableValueGreaterThanOrEqual() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.caseInstanceVariableValueGreaterThanOrEqual("aNullValue", null))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'greater than or equal' condition");

  }

  @Test
  void testQueryByStringCaseInstanceVariableValueGreaterThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThanOrEqual("aStringValue", "ab");

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThanOrEqual("aStringValue", "abc");

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByBooleanCaseInstanceVariableValueGreaterThanOrEqual() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.caseInstanceVariableValueGreaterThanOrEqual("aBooleanValue", false))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'greater than or equal' condition");

  }

  @Test
  void testQueryByShortCaseInstanceVariableValueGreaterThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThanOrEqual("aShortValue", (short) 122);

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThanOrEqual("aShortValue", (short) 123);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByIntegerCaseInstanceVariableValueGreaterThanOrEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThanOrEqual("anIntegerValue", 455);

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThanOrEqual("anIntegerValue", 456);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByLongCaseInstanceVariableValueGreaterThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThanOrEqual("aLongValue", (long) 788);

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThanOrEqual("aLongValue", (long) 789);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByDateCaseInstanceVariableValueGreaterThanOrEqual() {
    Date now = new Date();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDateValue", now)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    Date before = new Date(now.getTime() - 100000);

    query.caseInstanceVariableValueGreaterThanOrEqual("aDateValue", before);

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThanOrEqual("aDateValue", now);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByDoubleCaseInstanceVariableValueGreaterThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThanOrEqual("aDoubleValue", 1.4);

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueGreaterThanOrEqual("aDoubleValue", 1.5);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByByteArrayCaseInstanceVariableValueGreaterThanOrEqual() {
    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aByteArrayValue", bytes)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.caseInstanceVariableValueGreaterThanOrEqual("aByteArrayValue", bytes);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Variables of type ByteArray cannot be used to query");
  }

  @Test
  void testQueryBySerializableCaseInstanceVariableGreaterThanOrEqual() {
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aSerializableValue", serializable)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.caseInstanceVariableValueGreaterThanOrEqual("aSerializableValue", serializable);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Object values cannot be used to query");
  }

  @Test
  void testQueryByNullCaseInstanceVariableValueLessThan() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.caseInstanceVariableValueLessThan("aNullValue", null))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'less than' condition");

  }

  @Test
  void testQueryByStringCaseInstanceVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThan("aStringValue", "abd");

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByBooleanCaseInstanceVariableValueLessThan() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.caseInstanceVariableValueLessThan("aBooleanValue", false))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'less than' condition");

  }

  @Test
  void testQueryByShortCaseInstanceVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThan("aShortValue", (short) 124);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByIntegerCaseInstanceVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThan("anIntegerValue", 457);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByLongCaseInstanceVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThan("aLongValue", (long) 790);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByDateCaseInstanceVariableValueLessThan() {
    Date now = new Date();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDateValue", now)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    Date after = new Date(now.getTime() + 100000);

    query.caseInstanceVariableValueLessThan("aDateValue", after);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByDoubleCaseInstanceVariableValueLessThan() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThan("aDoubleValue", 1.6);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByByteArrayCaseInstanceVariableValueLessThan() {
    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aByteArrayValue", bytes)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.caseInstanceVariableValueLessThan("aByteArrayValue", bytes);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Variables of type ByteArray cannot be used to query");
  }

  @Test
  void testQueryBySerializableCaseInstanceVariableLessThan() {
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aSerializableValue", serializable)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.caseInstanceVariableValueLessThan("aSerializableValue", serializable);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Object values cannot be used to query");
  }

  @Test
  void testQueryByNullCaseInstanceVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.caseInstanceVariableValueLessThanOrEqual("aNullValue", null))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'less than or equal' condition");

  }

  @Test
  void testQueryByStringCaseInstanceVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThanOrEqual("aStringValue", "abd");

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThanOrEqual("aStringValue", "abc");

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByBooleanCaseInstanceVariableValueLessThanOrEqual() {
    // given
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aBooleanValue", true)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.caseInstanceVariableValueLessThanOrEqual("aBooleanValue", false))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'less than or equal' condition");

  }

  @Test
  void testQueryByShortCaseInstanceVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aShortValue", (short) 123)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThanOrEqual("aShortValue", (short) 124);

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThanOrEqual("aShortValue", (short) 123);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByIntegerCaseInstanceVariableValueLessThanOrEquals() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("anIntegerValue", 456)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThanOrEqual("anIntegerValue", 457);

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThanOrEqual("anIntegerValue", 456);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByLongCaseInstanceVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aLongValue", (long) 789)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThanOrEqual("aLongValue", (long) 790);

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThanOrEqual("aLongValue", (long) 789);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByDateCaseInstanceVariableValueLessThanOrEqual() {
    Date now = new Date();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDateValue", now)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    Date after = new Date(now.getTime() + 100000);

    query.caseInstanceVariableValueLessThanOrEqual("aDateValue", after);

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThanOrEqual("aDateValue", now);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByDoubleCaseInstanceVariableValueLessThanOrEqual() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aDoubleValue", 1.5)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThanOrEqual("aDoubleValue", 1.6);

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLessThanOrEqual("aDoubleValue", 1.5);

    verifyQueryResults(query, 2);

  }

  @Test
  void testQueryByByteArrayCaseInstanceVariableValueLessThanOrEqual() {
    byte[] bytes = "somebytes".getBytes();

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aByteArrayValue", bytes)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.caseInstanceVariableValueLessThanOrEqual("aByteArrayValue", bytes);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Variables of type ByteArray cannot be used to query");
  }

  @Test
  void testQueryBySerializableCaseInstanceVariableLessThanOrEqual() {
    List<String> serializable = new ArrayList<>();
    serializable.add("one");
    serializable.add("two");
    serializable.add("three");

    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aSerializableValue", serializable)
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    var caseExecutionQuery = query.caseInstanceVariableValueLessThanOrEqual("aSerializableValue", serializable);

    assertThatThrownBy(caseExecutionQuery::list)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Object values cannot be used to query");
  }

  @Test
  void testQueryByNullCaseInstanceVariableValueLike() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aNullValue", null)
      .create();
    var caseExecutionQuery = caseService.createCaseExecutionQuery();

    // when/then
    assertThatThrownBy(() -> caseExecutionQuery.caseInstanceVariableValueLike("aNullValue", null))
        .isInstanceOf(NotValidException.class)
        .hasMessage("Booleans and null cannot be used in 'like' condition");

  }

  @Test
  void testQueryByStringCaseInstanceVariableValueLike() {
    caseService
      .withCaseDefinitionByKey(CASE_DEFINITION_KEY)
      .setVariable("aStringValue", "abc")
      .create();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLike("aStringValue", "ab%");

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLike("aStringValue", "%bc");

    verifyQueryResults(query, 2);

    query = caseService.createCaseExecutionQuery();

    query.caseInstanceVariableValueLike("aStringValue", "%b%");

    verifyQueryResults(query, 2);
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

    assertThat(caseService.createCaseExecutionQuery().variableValueEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(caseService.createCaseExecutionQuery().variableValueEquals("var", Variables.numberValue(123L)).count()).isEqualTo(4);
    assertThat(caseService.createCaseExecutionQuery().variableValueEquals("var", Variables.numberValue(123.0d)).count()).isEqualTo(4);
    assertThat(caseService.createCaseExecutionQuery().variableValueEquals("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);

    assertThat(caseService.createCaseExecutionQuery().variableValueEquals("var", Variables.numberValue(null)).count()).isOne();

    // other operators
    assertThat(caseService.createCaseExecutionQuery().variableValueNotEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(caseService.createCaseExecutionQuery().variableValueGreaterThan("var", Variables.numberValue(123L)).count()).isOne();
    assertThat(caseService.createCaseExecutionQuery().variableValueGreaterThanOrEqual("var", Variables.numberValue(123.0d)).count()).isEqualTo(5);
    assertThat(caseService.createCaseExecutionQuery().variableValueLessThan("var", Variables.numberValue((short) 123)).count()).isZero();
    assertThat(caseService.createCaseExecutionQuery().variableValueLessThanOrEqual("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);

    // two executions per case instance match the query
    assertThat(caseService.createCaseExecutionQuery().caseInstanceVariableValueEquals("var", Variables.numberValue(123)).count()).isEqualTo(8);
    assertThat(caseService.createCaseExecutionQuery().caseInstanceVariableValueEquals("var", Variables.numberValue(123L)).count()).isEqualTo(8);
    assertThat(caseService.createCaseExecutionQuery().caseInstanceVariableValueEquals("var", Variables.numberValue(123.0d)).count()).isEqualTo(8);
    assertThat(caseService.createCaseExecutionQuery().caseInstanceVariableValueEquals("var", Variables.numberValue((short) 123)).count()).isEqualTo(8);

    assertThat(caseService.createCaseExecutionQuery().caseInstanceVariableValueEquals("var", Variables.numberValue(null)).count()).isEqualTo(2);

    // other operators
    assertThat(caseService.createCaseExecutionQuery().caseInstanceVariableValueNotEquals("var", Variables.numberValue(123)).count()).isEqualTo(8);
    assertThat(caseService.createCaseExecutionQuery().caseInstanceVariableValueGreaterThan("var", Variables.numberValue(123L)).count()).isEqualTo(2);
    assertThat(caseService.createCaseExecutionQuery().caseInstanceVariableValueGreaterThanOrEqual("var", Variables.numberValue(123.0d)).count()).isEqualTo(10);
    assertThat(caseService.createCaseExecutionQuery().caseInstanceVariableValueLessThan("var", Variables.numberValue((short) 123)).count()).isZero();
    assertThat(caseService.createCaseExecutionQuery().caseInstanceVariableValueLessThanOrEqual("var", Variables.numberValue((short) 123)).count()).isEqualTo(8);

  }


  @Test
  void testQuerySorting() {
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();

    // asc
    query
      .orderByCaseDefinitionId()
      .asc();
    verifyQueryWithOrdering(query, 11, caseExecutionByDefinitionId());

    query = caseService.createCaseExecutionQuery();

    query
      .orderByCaseDefinitionKey()
      .asc();
    verifyQueryWithOrdering(query, 11, caseExecutionByDefinitionKey(processEngine));

    query = caseService.createCaseExecutionQuery();

    query
      .orderByCaseExecutionId()
      .asc();
    verifyQueryWithOrdering(query, 11, caseExecutionById());


    // desc

    query = caseService.createCaseExecutionQuery();

    query
      .orderByCaseDefinitionId()
      .desc();
    verifyQueryWithOrdering(query, 11, inverted(caseExecutionByDefinitionId()));

    query = caseService.createCaseExecutionQuery();

    query
      .orderByCaseDefinitionKey()
      .desc();
    verifyQueryWithOrdering(query, 11, inverted(caseExecutionByDefinitionKey(processEngine)));

    query = caseService.createCaseExecutionQuery();

    query
      .orderByCaseExecutionId()
      .desc();
    verifyQueryWithOrdering(query, 11, inverted(caseExecutionById()));
  }

  @Test
  void testCaseExecutionProperties() {
    // given
    String caseDefinitionId = repositoryService
        .createCaseDefinitionQuery()
        .caseDefinitionKey(CASE_DEFINITION_KEY)
        .singleResult()
        .getId();

    String caseInstanceId = caseService
        .withCaseDefinition(caseDefinitionId)
        .create()
        .getId();

    // when
    CaseExecution task  = caseService
        .createCaseExecutionQuery()
        .caseInstanceId(caseInstanceId)
        .activityId("PI_HumanTask_1")
        .singleResult();

    // then
    assertThat(task.getActivityId()).isEqualTo("PI_HumanTask_1");
    assertThat(task.getActivityName()).isEqualTo("A HumanTask");
    assertThat(task.getCaseDefinitionId()).isEqualTo(caseDefinitionId);
    assertThat(task.getCaseInstanceId()).isEqualTo(caseInstanceId);
    assertThat(task.getParentId()).isEqualTo(caseInstanceId);
    assertThat(task.getActivityType()).isEqualTo("humanTask");
    assertThat(task.getActivityDescription()).isNotNull();
    assertThat(task.getId()).isNotNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/required/RequiredRuleTest.testVariableBasedRule.cmmn")
  @Test
  void testQueryByRequired() {
    caseService.createCaseInstanceByKey("case", Collections.singletonMap("required", true));

    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .required();

    verifyQueryResults(query, 1);

    CaseExecution execution = query.singleResult();
    assertThat(execution).isNotNull();
    assertThat(execution.isRequired()).isTrue();
  }

  @Test
  void testNullBusinessKeyForChildExecutions() {
    caseService.createCaseInstanceByKey(CASE_DEFINITION_KEY, "7890");
    List<CaseExecution> executions = caseService.createCaseExecutionQuery().caseInstanceBusinessKey("7890").list();
    for (CaseExecution e : executions) {
      if (((CaseExecutionEntity) e).isCaseInstanceExecution()) {
        assertThat(((CaseExecutionEntity) e).getBusinessKeyWithoutCascade()).isEqualTo("7890");
      } else {
        assertThat(((CaseExecutionEntity) e).getBusinessKeyWithoutCascade()).isNull();
      }
    }
  }

}
