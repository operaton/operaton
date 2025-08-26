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
package org.operaton.bpm.engine.test.cmmn.stage;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.history.HistoricCaseActivityInstance;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionQuery;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.CaseInstanceQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
class AutoCompleteTest extends CmmnTest {

  protected static final String CASE_DEFINITION_KEY = "case";

  @Deployment
  @Test
  void testCasePlanModel() {
    // given
    // a deployed process

    // when
    String caseInstanceId = createCaseInstanceByKey(CASE_DEFINITION_KEY).getId();

    // then
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .singleResult();

    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();

    // humanTask1 and humanTask2 are not available
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    assertThat(query.activityId("PI_HumanTask_1").singleResult()).isNull();
    assertThat(query.activityId("PI_HumanTask_2").singleResult()).isNull();
  }

  @Deployment
  @Test
  void testStage() {
    // given
    String caseInstanceId = createCaseInstanceByKey(CASE_DEFINITION_KEY).getId();

    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1");

    String stageId = query.singleResult().getId();

    // when
    caseService.manuallyStartCaseExecution(stageId);

    // then

    // the instance is still active (contains
    // a further human task)
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();

    // humanTask1 is still available
    assertThat(query.activityId("PI_HumanTask_1").singleResult()).isNotNull();

    // stage, humanTask2, humanTask3 are not available
    assertThat(query.activityId("PI_Stage_1").singleResult()).isNull();
    assertThat(query.activityId("PI_HumanTask_2").singleResult()).isNull();
    assertThat(query.activityId("PI_HumanTask_3").singleResult()).isNull();
  }

  @Deployment
  @Test
  void testManualActivationDisabled() {
    // given
    // a deployed case definition

    // when (1)
    String caseInstanceId = createCaseInstanceByKey(CASE_DEFINITION_KEY).getId();

    // then (1)
    CaseInstanceQuery instanceQuery = caseService
      .createCaseInstanceQuery()
      .caseInstanceId(caseInstanceId);

    CaseInstance caseInstance = instanceQuery.singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();

    CaseExecutionQuery executionQuery = caseService.createCaseExecutionQuery();

    String humanTask2Id = executionQuery
      .activityId("PI_HumanTask_2")
      .singleResult()
      .getId();

    // when (2)
    caseService.completeCaseExecution(humanTask2Id);

    // then (2)
    caseInstance = instanceQuery.singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();

    // humanTask1 and humanTask2 are not available
    assertThat(executionQuery.activityId("PI_HumanTask_1").singleResult()).isNull();
    assertThat(executionQuery.activityId("PI_HumanTask_2").singleResult()).isNull();
  }

  @Deployment
  @Test
  void testManualActivationDisabledInsideStage() {
    // given
    String caseInstanceId = createCaseInstanceByKey(CASE_DEFINITION_KEY).getId();

    CaseExecutionQuery executionQuery = caseService.createCaseExecutionQuery();

    assertThat(executionQuery.activityId("PI_Stage_1").singleResult()).isNotNull();

    // then (1)
    CaseExecution stage = executionQuery
        .activityId("PI_Stage_1")
        .singleResult();
    assertThat(stage).isNotNull();
    assertThat(stage.isActive()).isTrue();

    String humanTask2Id = executionQuery
        .activityId("PI_HumanTask_2")
        .singleResult()
        .getId();

    // when (2)
    complete(humanTask2Id);

    // then (2)
    // the instance is still active (contains
    // a further human task)
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();

    // humanTask1 is still available
    assertThat(executionQuery.activityId("PI_HumanTask_1").singleResult()).isNotNull();

    // stage, humanTask2, humanTask3 are not available
    assertThat(executionQuery.activityId("PI_Stage_1").singleResult()).isNull();
    assertThat(executionQuery.activityId("PI_HumanTask_2").singleResult()).isNull();
    assertThat(executionQuery.activityId("PI_HumanTask_3").singleResult()).isNull();
  }

  @Deployment
  @Test
  void testNested() {
    // given
    // a deployed case definition

    CaseExecutionQuery executionQuery = caseService.createCaseExecutionQuery();

    // when
    String caseInstanceId = createCaseInstanceByKey(CASE_DEFINITION_KEY).getId();

    // then
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();

    // stage, humanTask1, humanTask2, humanTask3 are not available
    assertThat(executionQuery.activityId("PI_Stage_1").singleResult()).isNull();
    assertThat(executionQuery.activityId("PI_HumanTask_1").singleResult()).isNull();
    assertThat(executionQuery.activityId("PI_HumanTask_2").singleResult()).isNull();
    assertThat(executionQuery.activityId("PI_HumanTask_3").singleResult()).isNull();
  }

  @Deployment
  @Test
  void testRequiredEnabled() {
    // given
    // a deployed case definition

    CaseExecutionQuery executionQuery = caseService.createCaseExecutionQuery();
    CaseInstanceQuery instanceQuery = caseService.createCaseInstanceQuery();

    // when (1)
    String caseInstanceId = createCaseInstanceByKey(CASE_DEFINITION_KEY).getId();

    // then (1)
    CaseInstance caseInstance = instanceQuery
        .caseInstanceId(caseInstanceId)
        .singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();

    String humanTask1Id = executionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();
    manualStart(humanTask1Id);

    // when (2)
    complete(humanTask1Id);

    // then (2)
    caseInstance = instanceQuery.singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();

    String humanTask2Id = executionQuery
        .activityId("PI_HumanTask_2")
        .singleResult()
        .getId();
    manualStart(humanTask2Id);

    // when (3)
    complete(humanTask2Id);

    // then (3)
    caseInstance = instanceQuery.singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment
  @Test
  void testRequiredEnabledInsideStage() {
    // given
    String caseInstanceId = createCaseInstanceByKey(CASE_DEFINITION_KEY).getId();

    CaseExecutionQuery executionQuery = caseService.createCaseExecutionQuery();

    String humanTask3Id = executionQuery
        .activityId("PI_HumanTask_3")
        .singleResult()
        .getId();

    // when (1)
    complete(humanTask3Id);

    // then (1)
    CaseExecution stage = executionQuery
        .activityId("PI_Stage_1")
        .singleResult();
    assertThat(stage).isNotNull();
    assertThat(stage.isActive()).isTrue();

    String humanTask2Id = executionQuery
        .activityId("PI_HumanTask_2")
        .singleResult()
        .getId();

    // when (2)
    complete(humanTask2Id);

    // then (2)
    assertThat(executionQuery.activityId("PI_Stage_1").singleResult()).isNull();

    CaseInstance caseInstance = caseService
      .createCaseInstanceQuery()
      .caseInstanceId(caseInstanceId)
      .singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();
  }

  @Deployment
  @Test
  void testEntryCriteriaAndManualActivationDisabled() {
    // given
    String caseInstanceId = createCaseInstanceByKey(CASE_DEFINITION_KEY).getId();

    CaseExecutionQuery executionQuery = caseService.createCaseExecutionQuery();

    String humanTask1Id = executionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    // when (1)
    complete(humanTask1Id);

    // then (1)
    CaseInstanceQuery instanceQuery = caseService
        .createCaseInstanceQuery()
        .caseInstanceId(caseInstanceId);

    CaseInstance caseInstance = instanceQuery.singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isActive()).isTrue();

    String humanTask2Id = executionQuery
        .activityId("PI_HumanTask_2")
        .singleResult()
        .getId();

    // when (2)
    complete(humanTask2Id);

    // then (2)
    caseInstance = instanceQuery.singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment
  @Test
  void testExitCriteriaAndRequiredEnabled() {
    // given
    String caseInstanceId = createCaseInstanceByKey(CASE_DEFINITION_KEY).getId();

    CaseExecutionQuery executionQuery = caseService.createCaseExecutionQuery();

    String humanTask1Id = executionQuery
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    CaseExecution humanTask2 = executionQuery
      .activityId("PI_HumanTask_2")
      .singleResult();

    manualStart(humanTask2.getId());

    // when
    complete(humanTask1Id);

    // then
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/stage/AutoCompleteTest.testRequiredEnabled.cmmn"})
  @Test
  void testTerminate() {
    // given
    // a deployed case definition

    String caseInstanceId = createCaseInstanceByKey(CASE_DEFINITION_KEY).getId();

    CaseExecutionQuery executionQuery = caseService.createCaseExecutionQuery();
    CaseInstanceQuery instanceQuery = caseService.createCaseInstanceQuery().caseInstanceId(caseInstanceId);

    String humanTask2Id = executionQuery
      .activityId("PI_HumanTask_2")
      .singleResult()
      .getId();
    manualStart(humanTask2Id);

    // when
    terminate(humanTask2Id);

    // then
    CaseInstance caseInstance = instanceQuery.singleResult();
    assertThat(caseInstance).isNotNull();
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/stage/AutoCompleteTest.testProcessTasksOnStage.cmmn",
      "org/operaton/bpm/engine/test/cmmn/stage/AutoCompleteTest.testProcessTasksOnStage.bpmn"
  })
  @RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
  @Test
  void testProcessTasksOnStage() {
    // given

    // when
    createCaseInstanceByKey(CASE_DEFINITION_KEY);

    List<HistoricCaseActivityInstance> historicCaseActivityInstances =
      historyService.createHistoricCaseActivityInstanceQuery()
      .caseActivityType("processTask")
      .list();

    // then
    assertThat(historicCaseActivityInstances).hasSize(2);
  }

}
