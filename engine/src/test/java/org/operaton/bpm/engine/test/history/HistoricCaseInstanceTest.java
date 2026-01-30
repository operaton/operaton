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
package org.operaton.bpm.engine.test.history;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.history.HistoricCaseInstance;
import org.operaton.bpm.engine.history.HistoricCaseInstanceQuery;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.calendar.DateTimeUtil;
import org.operaton.bpm.engine.impl.history.HistoryLevel;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Sebastian Menski
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
class HistoricCaseInstanceTest extends CmmnTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/emptyStageCase.cmmn"})
  @Test
  void testCaseInstanceProperties() {
    CaseInstance caseInstance = createCaseInstance();

    HistoricCaseInstance historicInstance = queryHistoricCaseInstance(caseInstance.getId());

    // assert case instance properties are set correctly
    assertThat(historicInstance.getId()).isEqualTo(caseInstance.getId());
    assertThat(historicInstance.getBusinessKey()).isEqualTo(caseInstance.getBusinessKey());
    assertThat(historicInstance.getCaseDefinitionId()).isEqualTo(caseInstance.getCaseDefinitionId());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/emptyStageWithManualActivationCase.cmmn"})
  @Test
  void testCaseInstanceStates() {
    String caseInstanceId = createCaseInstance().getId();

    HistoricCaseInstance historicCaseInstance = queryHistoricCaseInstance(caseInstanceId);

    assertThat(historicCaseInstance.isActive()).isTrue();
    assertCount(1, historicQuery().active());
    assertCount(1, historicQuery().notClosed());

    // start empty stage to complete case instance
    String stageExecutionId = queryCaseExecutionByActivityId("PI_Stage_1").getId();
    manualStart(stageExecutionId);

    historicCaseInstance = queryHistoricCaseInstance(caseInstanceId);
    assertThat(historicCaseInstance.isCompleted()).isTrue();
    assertCount(1, historicQuery().completed());
    assertCount(1, historicQuery().notClosed());

    // reactive and terminate case instance
    reactivate(caseInstanceId);
    terminate(caseInstanceId);

    historicCaseInstance = queryHistoricCaseInstance(caseInstanceId);
    assertThat(historicCaseInstance.isTerminated()).isTrue();
    assertCount(1, historicQuery().terminated());
    assertCount(1, historicQuery().notClosed());

    // reactive and suspend case instance
    reactivate(caseInstanceId);
    suspend(caseInstanceId);

    historicCaseInstance = queryHistoricCaseInstance(caseInstanceId);
    // not public API
    assertThat(((HistoricCaseInstanceEntity) historicCaseInstance).isSuspended()).isTrue();
    assertCount(1, historicQuery().notClosed());

    // close case instance
    close(caseInstanceId);

    historicCaseInstance = queryHistoricCaseInstance(caseInstanceId);
    assertThat(historicCaseInstance.isClosed()).isTrue();
    assertCount(1, historicQuery().closed());
    assertCount(0, historicQuery().notClosed());
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/emptyStageWithManualActivationCase.cmmn"})
  @Test
  void testHistoricCaseInstanceDates() {
    // create test dates
    long duration = 72 * 3600 * 1000;
    Date created = ClockUtil.getCurrentTime();
    Date closed = new Date(created.getTime() + duration);

    // create instance
    ClockUtil.setCurrentTime(created);
    String caseInstanceId = createCaseInstance().getId();

    terminate(caseInstanceId);

    // close instance
    ClockUtil.setCurrentTime(closed);
    close(caseInstanceId);

    HistoricCaseInstance historicCaseInstance = queryHistoricCaseInstance(caseInstanceId);

    // read historic dates ignoring milliseconds
    Date createTime = historicCaseInstance.getCreateTime();
    Date closeTime = historicCaseInstance.getCloseTime();
    Long durationInMillis = historicCaseInstance.getDurationInMillis();

    assertDateSimilar(created, createTime);
    assertDateSimilar(closed, closeTime);

    // test that duration is as expected with a maximal difference of one second
    assertThat(durationInMillis)
      .isGreaterThanOrEqualTo(duration)
      .isLessThan(duration + 1000);

    // test queries
    Date beforeCreate = new Date(created.getTime() - 3600 * 1000);
    Date afterClose = new Date(closed.getTime() + 3600 * 1000);

    assertCount(1, historicQuery().createdAfter(beforeCreate));
    assertCount(0, historicQuery().createdAfter(closed));

    assertCount(0, historicQuery().createdBefore(beforeCreate));
    assertCount(1, historicQuery().createdBefore(closed));

    assertCount(0, historicQuery().createdBefore(beforeCreate).createdAfter(closed));

    assertCount(1, historicQuery().closedAfter(created));
    assertCount(0, historicQuery().closedAfter(afterClose));

    assertCount(0, historicQuery().closedBefore(created));
    assertCount(1, historicQuery().closedBefore(afterClose));

    assertCount(0, historicQuery().closedBefore(created).closedAfter(afterClose));

    assertCount(1, historicQuery().closedBefore(afterClose).closedAfter(created));
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/emptyStageCase.cmmn"})
  @Test
  void testCreateUser() {
    String userId = "test";
    identityService.setAuthenticatedUserId(userId);

    String caseInstanceId = createCaseInstance().getId();

    HistoricCaseInstance historicCaseInstance = queryHistoricCaseInstance(caseInstanceId);
    assertThat(historicCaseInstance.getCreateUserId()).isEqualTo(userId);
    assertCount(1, historicQuery().createdBy(userId));

    identityService.setAuthenticatedUserId(null);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testSuperCaseInstance() {
    String caseInstanceId  = createCaseInstanceByKey("oneCaseTaskCase").getId();
    queryCaseExecutionByActivityId("PI_CaseTask_1").getId();

    HistoricCaseInstance historicCaseInstance = historicQuery()
      .superCaseInstanceId(caseInstanceId)
      .singleResult();

    assertThat(historicCaseInstance).isNotNull();
    assertThat(historicCaseInstance.getSuperCaseInstanceId()).isEqualTo(caseInstanceId);

    String superCaseInstanceId = historicQuery()
      .subCaseInstanceId(historicCaseInstance.getId())
      .singleResult()
      .getId();

    assertThat(superCaseInstanceId).isEqualTo(caseInstanceId);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/repository/three_.cmmn"
  })
  @Test
  void testHistoricCaseInstanceQuery() {
    CaseInstance oneTaskCase = createCaseInstanceByKey("oneTaskCase", "oneBusiness");
    CaseInstance twoTaskCase = createCaseInstanceByKey("twoTaskCase", "twoBusiness");
    createCaseInstanceByKey("xyz_", "xyz_");

    assertCount(1, historicQuery().caseInstanceId(oneTaskCase.getId()));
    assertCount(1, historicQuery().caseInstanceId(twoTaskCase.getId()));

    Set<String> caseInstanceIds = new HashSet<>();
    caseInstanceIds.add(oneTaskCase.getId());
    caseInstanceIds.add("unknown1");
    caseInstanceIds.add(twoTaskCase.getId());
    caseInstanceIds.add("unknown2");

    assertCount(2, historicQuery().caseInstanceIds(caseInstanceIds));
    assertCount(0, historicQuery().caseInstanceIds(caseInstanceIds).caseInstanceId("someOtherId"));

    assertCount(1, historicQuery().caseDefinitionId(oneTaskCase.getCaseDefinitionId()));

    assertCount(1, historicQuery().caseDefinitionKey("oneTaskCase"));

    assertCount(3, historicQuery().caseDefinitionKeyNotIn(List.of("unknown")));
    assertCount(2, historicQuery().caseDefinitionKeyNotIn(List.of("oneTaskCase")));
    assertCount(1, historicQuery().caseDefinitionKeyNotIn(List.of("oneTaskCase", "twoTaskCase")));
    assertCount(0, historicQuery().caseDefinitionKeyNotIn(List.of("oneTaskCase")).caseDefinitionKey("oneTaskCase"));
    var emptyCaseDefinitionKeys = List.of("");
    var historicCaseInstanceQuery = historicQuery();


    assertThatThrownBy(() -> historicCaseInstanceQuery.caseDefinitionKeyNotIn(emptyCaseDefinitionKeys)).isInstanceOf(NotValidException.class);


    assertCount(1, historicQuery().caseDefinitionName("One Task Case"));

    assertCount(2, historicQuery().caseDefinitionNameLike("%T%"));
    assertCount(1, historicQuery().caseDefinitionNameLike("One%"));
    assertCount(0, historicQuery().caseDefinitionNameLike("%Process%"));
    assertCount(1, historicQuery().caseDefinitionNameLike("%z\\_"));

    assertCount(1, historicQuery().caseInstanceBusinessKey("oneBusiness"));

    assertCount(2, historicQuery().caseInstanceBusinessKeyLike("%Business"));
    assertCount(1, historicQuery().caseInstanceBusinessKeyLike("one%"));
    assertCount(0, historicQuery().caseInstanceBusinessKeyLike("%unknown%"));
    assertCount(1, historicQuery().caseInstanceBusinessKeyLike("%z\\_"));
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryByVariable() {
    String caseInstanceId = createCaseInstance().getId();
    caseService.setVariable(caseInstanceId, "foo", "bar");
    caseService.setVariable(caseInstanceId, "number", 10);

    assertCount(1, historicQuery().variableValueEquals("foo", "bar"));
    assertCount(0, historicQuery().variableValueNotEquals("foo", "bar"));
    assertCount(1, historicQuery().variableValueNotEquals("foo", "lol"));
    assertCount(0, historicQuery().variableValueEquals("foo", "lol"));
    assertCount(1, historicQuery().variableValueLike("foo", "%a%"));
    assertCount(0, historicQuery().variableValueLike("foo", "%lol%"));
    assertCount(0, historicQuery().variableValueNotLike("foo", "%a%"));
    assertCount(1, historicQuery().variableValueNotLike("foo", "%lol%"));

    assertCount(1, historicQuery().variableValueEquals("number", 10));
    assertCount(0, historicQuery().variableValueNotEquals("number", 10));
    assertCount(1, historicQuery().variableValueNotEquals("number", 1));
    assertCount(1, historicQuery().variableValueGreaterThan("number", 1));
    assertCount(0, historicQuery().variableValueLessThan("number", 1));
    assertCount(1, historicQuery().variableValueGreaterThanOrEqual("number", 10));
    assertCount(0, historicQuery().variableValueLessThan("number", 10));
    assertCount(1, historicQuery().variableValueLessThan("number", 20));
    assertCount(0, historicQuery().variableValueGreaterThan("number", 20));
    assertCount(1, historicQuery().variableValueLessThanOrEqual("number", 10));
    assertCount(0, historicQuery().variableValueGreaterThan("number", 10));
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  @Test
  void testCaseVariableValueEqualsNumber() {
    // long
    caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("var", 123L)
      .create();

    // non-matching long
    caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("var", 12345L)
      .create();

    // short
    caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("var", (short) 123)
      .create();

    // double
    caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("var", 123.0d)
      .create();

    // integer
    caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("var", 123)
      .create();

    // untyped null (should not match)
    caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("var", null)
      .create();

    // typed null (should not match)
    caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("var", Variables.longValue(null))
      .create();

    caseService
      .withCaseDefinitionByKey("oneTaskCase")
      .setVariable("var", "123")
      .create();

    assertThat(historicQuery().variableValueEquals("var", Variables.numberValue(123)).count()).isEqualTo(4);
    assertThat(historicQuery().variableValueEquals("var", Variables.numberValue(123L)).count()).isEqualTo(4);
    assertThat(historicQuery().variableValueEquals("var", Variables.numberValue(123.0d)).count()).isEqualTo(4);
    assertThat(historicQuery().variableValueEquals("var", Variables.numberValue((short) 123)).count()).isEqualTo(4);

    assertThat(historicQuery().variableValueEquals("var", Variables.numberValue(null)).count()).isOne();
  }


  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testQueryPaging() {
    createCaseInstance();
    createCaseInstance();
    createCaseInstance();
    createCaseInstance();

    assertThat(historicQuery().listPage(0, 3)).hasSize(3);
    assertThat(historicQuery().listPage(2, 2)).hasSize(2);
    assertThat(historicQuery().listPage(3, 2)).hasSize(1);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"
  })
  @SuppressWarnings("unchecked")
  @Test
  void testQuerySorting() {
    String oneCaseInstanceId = createCaseInstanceByKey("oneTaskCase", "oneBusinessKey").getId();
    String twoCaseInstanceId = createCaseInstanceByKey("twoTaskCase", "twoBusinessKey").getId();

    // terminate and close case instances => close time and duration is set
    terminate(oneCaseInstanceId);
    close(oneCaseInstanceId);
    // set time ahead to get different durations
    ClockUtil.setCurrentTime(DateTimeUtil.now().plusHours(1).toDate());
    terminate(twoCaseInstanceId);
    close(twoCaseInstanceId);

    HistoricCaseInstance oneCaseInstance = queryHistoricCaseInstance(oneCaseInstanceId);
    HistoricCaseInstance twoCaseInstance = queryHistoricCaseInstance(twoCaseInstanceId);

    // sort by case instance ids
    @SuppressWarnings("rawtypes")
    List<? extends Comparable> sortedList = Arrays.asList(oneCaseInstance.getId(), twoCaseInstance.getId());
    Collections.sort(sortedList);

    List<HistoricCaseInstance> instances = historicQuery().orderByCaseInstanceId().asc().list();
    assertThat(instances).hasSize(2);
    assertThat(instances).extracting("id").containsExactly(sortedList.get(0), sortedList.get(1));

    instances = historicQuery().orderByCaseInstanceId().desc().list();
    assertThat(instances).hasSize(2);
    assertThat(instances).extracting("id").containsExactly(sortedList.get(1), sortedList.get(0));

    // sort by case definition ids
    sortedList = Arrays.asList(oneCaseInstance.getCaseDefinitionId(), twoCaseInstance.getCaseDefinitionId());
    Collections.sort(sortedList);

    instances = historicQuery().orderByCaseDefinitionId().asc().list();
    assertThat(instances).hasSize(2);
    assertThat(instances).extracting("caseDefinitionId").containsExactly(sortedList.get(0), sortedList.get(1));

    instances = historicQuery().orderByCaseDefinitionId().desc().list();
    assertThat(instances).hasSize(2);
    assertThat(instances).extracting("caseDefinitionId").containsExactly(sortedList.get(1), sortedList.get(0));

    // sort by business keys
    sortedList = Arrays.asList(oneCaseInstance.getBusinessKey(), twoCaseInstance.getBusinessKey());
    Collections.sort(sortedList);

    instances = historicQuery().orderByCaseInstanceBusinessKey().asc().list();
    assertThat(instances).hasSize(2);
    assertThat(instances).extracting("businessKey").containsExactly(sortedList.get(0), sortedList.get(1));

    instances = historicQuery().orderByCaseInstanceBusinessKey().desc().list();
    assertThat(instances).hasSize(2);
    assertThat(instances).extracting("businessKey").containsExactly(sortedList.get(1), sortedList.get(0));

    // sort by create time
    sortedList = Arrays.asList(oneCaseInstance.getCreateTime(), twoCaseInstance.getCreateTime());
    Collections.sort(sortedList);

    instances = historicQuery().orderByCaseInstanceCreateTime().asc().list();
    assertThat(instances).hasSize(2);
    assertThat(instances).extracting("createTime").containsExactly(sortedList.get(0), sortedList.get(1));

    instances = historicQuery().orderByCaseInstanceCreateTime().desc().list();
    assertThat(instances).hasSize(2);
    assertThat(instances).extracting("createTime").containsExactly(sortedList.get(1), sortedList.get(0));

    // sort by close time
    sortedList = Arrays.asList(oneCaseInstance.getCloseTime(), twoCaseInstance.getCloseTime());
    Collections.sort(sortedList);

    instances = historicQuery().orderByCaseInstanceCloseTime().asc().list();
    assertThat(instances).hasSize(2);
    assertThat(instances).extracting("closeTime").containsExactly(sortedList.get(0), sortedList.get(1));

    instances = historicQuery().orderByCaseInstanceCloseTime().desc().list();
    assertThat(instances).hasSize(2);
    assertThat(instances).extracting("closeTime").containsExactly(sortedList.get(1), sortedList.get(0));

    // sort by duration
    sortedList = Arrays.asList(oneCaseInstance.getDurationInMillis(), twoCaseInstance.getDurationInMillis());
    Collections.sort(sortedList);

    instances = historicQuery().orderByCaseInstanceDuration().asc().list();
    assertThat(instances).hasSize(2);
    assertThat(instances).extracting("durationInMillis").containsExactly(sortedList.get(0), sortedList.get(1));

    instances = historicQuery().orderByCaseInstanceDuration().desc().list();
    assertThat(instances).hasSize(2);
    assertThat(instances).extracting("durationInMillis").containsExactly(sortedList.get(1), sortedList.get(0));

  }

  @Test
  void testInvalidSorting() {
    // given
    var historicCaseInstanceQuery = historicQuery();

    // when/then
    assertThatThrownBy(historicCaseInstanceQuery::asc)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("You should call any of the orderBy methods first before specifying a direction: currentOrderingProperty is null");

    assertThatThrownBy(historicCaseInstanceQuery::desc)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("You should call any of the orderBy methods first before specifying a direction: currentOrderingProperty is null");

    var historicCaseInstanceQuery1 = historicCaseInstanceQuery.orderByCaseInstanceId();
    assertThatThrownBy(historicCaseInstanceQuery1::count)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessage("Invalid query: call asc() or desc() after using orderByXX(): direction is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testNativeQuery() {
    String id = createCaseInstance().getId();
    createCaseInstance();
    createCaseInstance();
    createCaseInstance();

    String tablePrefix = processEngineConfiguration.getDatabaseTablePrefix();
    String tableName = managementService.getTableName(HistoricCaseInstance.class);

    assertThat(tableName).isEqualTo(tablePrefix + "ACT_HI_CASEINST");
    assertThat(managementService.getTableName(HistoricCaseInstanceEntity.class)).isEqualTo(tableName);

    assertThat(historyService.createNativeHistoricCaseInstanceQuery().sql("SELECT * FROM " + tableName).list()).hasSize(4);
    assertThat(historyService.createNativeHistoricCaseInstanceQuery().sql("SELECT count(*) FROM " + tableName).count()).isEqualTo(4);

    assertThat(historyService.createNativeHistoricCaseInstanceQuery().sql("SELECT count(*) FROM " + tableName + " H1, " + tableName + " H2").count()).isEqualTo(16);

    // select with distinct
    assertThat(historyService.createNativeHistoricCaseInstanceQuery().sql("SELECT DISTINCT * FROM " + tableName).list()).hasSize(4);

    assertThat(historyService.createNativeHistoricCaseInstanceQuery().sql("SELECT count(*) FROM " + tableName + " H WHERE H.ID_ = '" + id + "'").count()).isOne();
    assertThat(historyService.createNativeHistoricCaseInstanceQuery().sql("SELECT * FROM " + tableName + " H WHERE H.ID_ = '" + id + "'").list()).hasSize(1);

    // use parameters
    assertThat(historyService.createNativeHistoricCaseInstanceQuery().sql("SELECT count(*) FROM " + tableName + " H WHERE H.ID_ = #{caseInstanceId}").parameter("caseInstanceId", id).count()).isOne();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"})
  @Test
  void testNativeQueryPaging() {
    createCaseInstance();
    createCaseInstance();
    createCaseInstance();
    createCaseInstance();

    String tableName = managementService.getTableName(HistoricCaseInstance.class);
    assertThat(historyService.createNativeHistoricCaseInstanceQuery().sql("SELECT * FROM " + tableName).listPage(0, 3)).hasSize(3);
    assertThat(historyService.createNativeHistoricCaseInstanceQuery().sql("SELECT * FROM " + tableName).listPage(2, 2)).hasSize(2);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/emptyStageWithManualActivationCase.cmmn"})
  @Test
  void testDeleteHistoricCaseInstance() {
    CaseInstance caseInstance = createCaseInstance();

    String caseInstanceId = caseInstance.getId();
    HistoricCaseInstance historicInstance = queryHistoricCaseInstance(caseInstanceId);
    assertThat(historicInstance).isNotNull();
    var historicInstanceId = historicInstance.getId();

    assertThatThrownBy(() -> historyService.deleteHistoricCaseInstance(historicInstanceId)).isInstanceOf(NullValueException.class);

    terminate(caseInstanceId);
    close(caseInstanceId);

    identityService.setAuthenticatedUserId("testUser");
    historyService.deleteHistoricCaseInstance(historicInstance.getId());
    identityService.clearAuthentication();

    if (processEngineConfiguration.getHistoryLevel().getId() >= HistoryLevel.HISTORY_LEVEL_FULL.getId()) {
      // a user operation log should have been created
      assertThat(historyService.createUserOperationLogQuery().count()).isOne();
      UserOperationLogEntry entry = historyService.createUserOperationLogQuery().singleResult();
      assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_OPERATOR);
      assertThat(entry.getEntityType()).isEqualTo(EntityTypes.CASE_INSTANCE);
      assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY);
      assertThat(entry.getCaseInstanceId()).isEqualTo(caseInstanceId);
      assertThat(entry.getProperty()).isNull();
      assertThat(entry.getOrgValue()).isNull();
      assertThat(entry.getNewValue()).isNull();
    }

    assertCount(0, historicQuery());
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/runtime/superProcessWithCaseCallActivity.bpmn20.xml",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testQueryBySuperProcessInstanceId() {
    String superProcessInstanceId = runtimeService.startProcessInstanceByKey("subProcessQueryTest").getId();

    HistoricCaseInstanceQuery query = historyService
        .createHistoricCaseInstanceQuery()
        .superProcessInstanceId(superProcessInstanceId);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isOne();

    HistoricCaseInstance subCaseInstance = query.singleResult();
    assertThat(subCaseInstance).isNotNull();
    assertThat(subCaseInstance.getSuperProcessInstanceId()).isEqualTo(superProcessInstanceId);
    assertThat(subCaseInstance.getSuperCaseInstanceId()).isNull();
  }

  @Test
  void testQueryByInvalidSuperProcessInstanceId() {
    HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery();

    query.superProcessInstanceId("invalid");

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

    query.caseInstanceId(null);

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

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

    HistoricCaseInstanceQuery query = historyService
        .createHistoricCaseInstanceQuery()
        .subProcessInstanceId(subProcessInstanceId);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isOne();

    HistoricCaseInstance caseInstance = query.singleResult();
    assertThat(caseInstance.getId()).isEqualTo(superCaseInstanceId);
    assertThat(caseInstance.getSuperCaseInstanceId()).isNull();
    assertThat(caseInstance.getSuperProcessInstanceId()).isNull();
  }

  @Test
  void testQueryByInvalidSubProcessInstanceId() {
    HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery();

    query.subProcessInstanceId("invalid");

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

    query.caseInstanceId(null);

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testQueryBySuperCaseInstanceId() {
    String superCaseInstanceId = caseService.createCaseInstanceByKey("oneCaseTaskCase").getId();

    HistoricCaseInstanceQuery query = historyService
        .createHistoricCaseInstanceQuery()
        .superCaseInstanceId(superCaseInstanceId);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isOne();

    HistoricCaseInstance caseInstance = query.singleResult();
    assertThat(caseInstance.getSuperCaseInstanceId()).isEqualTo(superCaseInstanceId);
    assertThat(caseInstance.getSuperProcessInstanceId()).isNull();
  }

  @Test
  void testQueryByInvalidSuperCaseInstanceId() {
    HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery();

    query.superCaseInstanceId("invalid");

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

    query.caseInstanceId(null);

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testQueryBySubCaseInstanceId() {
    String superCaseInstanceId = caseService.createCaseInstanceByKey("oneCaseTaskCase").getId();

    String subCaseInstanceId = caseService
        .createCaseInstanceQuery()
        .superCaseInstanceId(superCaseInstanceId)
        .singleResult()
        .getId();

    HistoricCaseInstanceQuery query = historyService
        .createHistoricCaseInstanceQuery()
        .subCaseInstanceId(subCaseInstanceId);

    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isOne();

    HistoricCaseInstance caseInstance = query.singleResult();
    assertThat(caseInstance.getId()).isEqualTo(superCaseInstanceId);
    assertThat(caseInstance.getSuperProcessInstanceId()).isNull();
    assertThat(caseInstance.getSuperCaseInstanceId()).isNull();
  }

  @Test
  void testQueryByInvalidSubCaseInstanceId() {
    HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery();

    query.subCaseInstanceId("invalid");

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();

    query.caseInstanceId(null);

    assertThat(query.count()).isZero();
    assertThat(query.list()).isEmpty();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testQueryByCaseActivityId() {

    // given
    createCaseInstanceByKey("oneTaskCase");

    // when
    HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery()
      .caseActivityIdIn("PI_HumanTask_1");

    // then
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isOne();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/oneCaseTaskCase.cmmn",
      "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn"
  })
  @Test
  void testQueryByCaseActivityIds() {

    // given
    createCaseInstanceByKey("oneCaseTaskCase");

    // when
    HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery()
      .caseActivityIdIn("PI_HumanTask_1", "PI_CaseTask_1");

    // then
    assertThat(query.list()).hasSize(2);
    assertThat(query.count()).isEqualTo(2);
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/api/cmmn/twoTaskCase.cmmn"
  })
  @Test
  void testDistinctQueryByCaseActivityIds() {

    // given
    createCaseInstanceByKey("twoTaskCase");

    // when
    HistoricCaseInstanceQuery query = historyService.createHistoricCaseInstanceQuery()
      .caseActivityIdIn("PI_HumanTask_1", "PI_HumanTask_2");

    // then
    assertThat(query.list()).hasSize(1);
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByNonExistingCaseActivityId() {
    HistoricCaseInstanceQuery query = historyService
        .createHistoricCaseInstanceQuery()
        .caseActivityIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testFailQueryByCaseActivityIdNull() {
    var historicCaseInstanceQuery = historyService.createHistoricCaseInstanceQuery();
    assertThatThrownBy(() -> historicCaseInstanceQuery.caseActivityIdIn((String) null)).isInstanceOf(NullValueException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  @Test
  void testRetrieveCaseDefinitionKey() {

    // given
    String id = createCaseInstance("oneTaskCase").getId();

    // when
    HistoricCaseInstance caseInstance = historyService.createHistoricCaseInstanceQuery()
        .caseInstanceId(id)
        .singleResult();

    // then
    assertThat(caseInstance.getCaseDefinitionKey()).isEqualTo("oneTaskCase");

  }

  @Deployment(resources = "org/operaton/bpm/engine/test/api/cmmn/oneTaskCase.cmmn")
  @Test
  void testRetrieveCaseDefinitionName() {

    // given
    String id = createCaseInstance("oneTaskCase").getId();

    // when
    HistoricCaseInstance caseInstance = historyService.createHistoricCaseInstanceQuery()
        .caseInstanceId(id)
        .singleResult();

    // then
    assertThat(caseInstance.getCaseDefinitionName()).isEqualTo("One Task Case");

  }

  protected HistoricCaseInstance queryHistoricCaseInstance(String caseInstanceId) {
    HistoricCaseInstance historicCaseInstance = historicQuery()
      .caseInstanceId(caseInstanceId)
      .singleResult();
    assertThat(historicCaseInstance).isNotNull();
    return historicCaseInstance;
  }

  protected HistoricCaseInstanceQuery historicQuery() {
    return historyService.createHistoricCaseInstanceQuery();
  }

  protected void assertCount(long count, HistoricCaseInstanceQuery historicQuery) {
    assertThat(historicQuery.count()).isEqualTo(count);
  }

  protected void assertDateSimilar(Date date1, Date date2) {
    long difference = Math.abs(date1.getTime() - date2.getTime());
    assertThat(difference).isLessThan(1000);
  }

}
