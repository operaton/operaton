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
package org.operaton.bpm.engine.test.cmmn.repetition;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionQuery;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
class RepetitionRuleTest extends CmmnTest {

  private static final String CASE_ID = "case";

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/repetition/RepetitionRuleTest.testVariableBasedRule.cmmn")
  @Test
  void testVariableBasedRepetitionRuleEvaluatesToTrue() {
    // given
    VariableMap variables = Variables.createVariables().putValue("repeat", true);
    createCaseInstanceByKey("case", variables);

    String humanTask1 = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    complete(humanTask1);

    // then
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");

    assertThat(query.count()).isEqualTo(2);
    assertThat(query.available().count()).isOne();
    assertThat(query.active().count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/repetition/RepetitionRuleTest.testVariableBasedRule.cmmn")
  @Test
  void testVariableBasedRepetitionRuleEvaluatesToFalse() {
    // given
    VariableMap variables = Variables.createVariables().putValue("repeat", false);
    createCaseInstanceByKey("case", variables);

    String humanTask1 = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    complete(humanTask1);

    // then
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");
    assertThat(query.count()).isOne();
    assertThat(query.active().count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/repetition/RepetitionRuleTest.testDefaultVariableBasedRule.cmmn")
  @Test
  void testDefaultVariableBasedRepetitionRuleEvaluatesToTrue() {
    // given
    VariableMap variables = Variables.createVariables().putValue("repeat", true);
    createCaseInstanceByKey("case", variables);

    String humanTask1 = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    complete(humanTask1);

    // then
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");

    assertThat(query.count()).isEqualTo(2);
    assertThat(query.available().count()).isOne();
    assertThat(query.active().count()).isOne();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/repetition/RepetitionRuleTest.testDefaultVariableBasedRule.cmmn")
  @Test
  void testDefaultVariableBasedRepetitionRuleEvaluatesToFalse() {
    // given
    VariableMap variables = Variables.createVariables().putValue("repeat", false);
    createCaseInstanceByKey("case", variables);

    String humanTask1 = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    complete(humanTask1);

    // then
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");
    assertThat(query.count()).isOne();
    assertThat(query.active().count()).isOne();
  }

  @Deployment
  @Test
  void testRepeatTask() {
    // given
    createCaseInstance();

    String firstHumanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    complete(firstHumanTaskId);

    // then
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");

    assertThat(query.count()).isEqualTo(2);

    CaseExecution originInstance = query.active().singleResult();
    assertThat(originInstance).isNotNull();

    CaseExecution repetitionInstance = query.available().singleResult();
    assertThat(repetitionInstance).isNotNull();
  }

  @Deployment
  @Test
  void testRepeatStage() {
    // given
    createCaseInstance();

    String firstHumanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    complete(firstHumanTaskId);

    // then
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1");

    assertThat(query.count()).isEqualTo(2);

    CaseExecution originInstance = query.active().singleResult();
    assertThat(originInstance).isNotNull();

    CaseExecution repetitionInstance = query.available().singleResult();
    assertThat(repetitionInstance).isNotNull();
  }

  @Deployment
  @Test
  void testRepeatMilestone() {
    // given
    createCaseInstance();

    String firstHumanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();
    String milestoneId = queryCaseExecutionByActivityId("PI_Milestone_1").getId();

    // when
    complete(firstHumanTaskId);

    // then
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1");

    assertThat(query.count()).isOne();
    assertThat(query.singleResult().isAvailable()).isTrue();
    assertThat(query.singleResult().getId()).isNotEqualTo(milestoneId);
  }

  @Deployment
  @Test
  void testRepeatTaskMultipleTimes() {
    // given
    createCaseInstance();

    String firstHumanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when (1)
    disable(firstHumanTaskId);

    // then (1)
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");

    assertThat(query.count()).isEqualTo(2);

    CaseExecution originInstance = query.active().singleResult();
    assertThat(originInstance).isNotNull();

    CaseExecution repetitionInstance = query.available().singleResult();
    assertThat(repetitionInstance).isNotNull();

    // when (2)
    reenable(firstHumanTaskId);
    disable(firstHumanTaskId);

    // then (2)
    query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");

    assertThat(query.count()).isEqualTo(3);

    // active instances
    assertThat(query.active().count()).isEqualTo(2);

    // available instances
    assertThat(query.available().count()).isOne();
  }

  @Deployment
  @Test
  void testRepeatStageMultipleTimes() {
    // given
    createCaseInstance();

    String firstHumanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when (1)
    disable(firstHumanTaskId);

    // then (1)
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1");

    assertThat(query.count()).isEqualTo(2);

    CaseExecution originInstance = query.active().singleResult();
    assertThat(originInstance).isNotNull();

    CaseExecution repetitionInstance = query.available().singleResult();
    assertThat(repetitionInstance).isNotNull();

    // when (2)
    reenable(firstHumanTaskId);
    disable(firstHumanTaskId);

    // then (2)
    query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1");

    assertThat(query.count()).isEqualTo(3);

    // enabled instances
    assertThat(query.active().count()).isEqualTo(2);

    // available instances
    assertThat(query.available().count()).isOne();
  }

  @Deployment
  @Test
  void testRepeatMilestoneMultipleTimes() {
    // given
    createCaseInstance();

    String firstHumanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();
    String milestoneId = queryCaseExecutionByActivityId("PI_Milestone_1").getId();

    // when (1)
    disable(firstHumanTaskId);

    // then (2)
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1");

    assertThat(query.count()).isOne();
    assertThat(query.singleResult().isAvailable()).isTrue();
    assertThat(query.singleResult().getId()).isNotEqualTo(milestoneId);

    // when (2)
    reenable(firstHumanTaskId);
    disable(firstHumanTaskId);

    // then (2)
    query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1");

    assertThat(query.count()).isOne();
    assertThat(query.singleResult().isAvailable()).isTrue();
    assertThat(query.singleResult().getId()).isNotEqualTo(milestoneId);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/repetition/RepetitionRuleTest.testRepeatTaskWithoutEntryCriteria.cmmn")
  @Test
  void testRepeatTaskWithoutEntryCriteriaWhenCompleting() {
    // given
    String caseInstanceId = createCaseInstanceByKey(CASE_ID,Variables.createVariables().putValue("repeating", true)).getId();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1");
    assertThat(query.count()).isOne();

    CaseExecution activeCaseExecution = query.active().singleResult();
    assertThat(activeCaseExecution).isNotNull();

    // when (1)
    complete(activeCaseExecution.getId());

    // then (1)
    query = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1");
    assertThat(query.count()).isOne();

    activeCaseExecution = query.active().singleResult();
    assertThat(activeCaseExecution).isNotNull();

    // when (2)
    caseService.setVariable(caseInstanceId,"repeating",false);
    complete(activeCaseExecution.getId());

    // then (2)
    query = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1");
    assertThat(query.count()).isZero();

    // then (3)
    query = caseService.createCaseExecutionQuery();
    assertThat(query.count()).isOne();
    assertThat(query.singleResult().getId()).isEqualTo(caseInstanceId);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/repetition/RepetitionRuleTest.testRepeatStageWithoutEntryCriteria.cmmn")
  @Test
  void testRepeatStageWithoutEntryCriteriaWhenCompleting() {
    // given
    String caseInstanceId = createCaseInstanceByKey(CASE_ID,Variables.createVariables().putValue("repeating",true)).getId();

    CaseExecutionQuery stageQuery = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1");
    assertThat(stageQuery.count()).isOne();

    CaseExecution activeStageCaseExecution = stageQuery.active().singleResult();
    assertThat(activeStageCaseExecution).isNotNull();

    CaseExecution humanTaskCaseExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");

    // when (1)
    complete(humanTaskCaseExecution.getId());

    // then (1)
    stageQuery = caseService.createCaseExecutionQuery().activityId("PI_Stage_1");
    assertThat(stageQuery.count()).isOne();

    activeStageCaseExecution = stageQuery.active().singleResult();
    assertThat(activeStageCaseExecution).isNotNull();

    humanTaskCaseExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");

    // when (2)
    caseService.setVariable(caseInstanceId,"repeating",false);
    complete(humanTaskCaseExecution.getId());

    // then (3)
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    assertThat(query.count()).isOne();
    assertThat(query.singleResult().getId()).isEqualTo(caseInstanceId);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/repetition/RepetitionRuleTest.testRepeatTaskWithoutEntryCriteria.cmmn")
  @Test
  void testRepeatTaskWithoutEntryCriteriaWhenTerminating() {
    // given
    String caseInstanceId = createCaseInstanceByKey(CASE_ID,Variables.createVariables().putValue("repeating",true)).getId();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1");
    assertThat(query.count()).isOne();

    CaseExecution activeCaseExecution = query.active().singleResult();
    assertThat(activeCaseExecution).isNotNull();

    // when (1)
    terminate(activeCaseExecution.getId());

    // then (1)
    query = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1");
    assertThat(query.count()).isOne();

    activeCaseExecution = query.active().singleResult();
    assertThat(activeCaseExecution).isNotNull();

    // when (2)
    caseService.setVariable(caseInstanceId,"repeating",false);
    terminate(activeCaseExecution.getId());

    // then (2)
    query = caseService.createCaseExecutionQuery();
    assertThat(query.count()).isOne();
    assertThat(query.singleResult().getId()).isEqualTo(caseInstanceId);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/repetition/RepetitionRuleTest.testRepeatStageWithoutEntryCriteria.cmmn")
  @Test
  void testRepeatStageWithoutEntryCriteriaWhenTerminating() {
    // given
    String caseInstanceId = createCaseInstanceByKey(CASE_ID,Variables.createVariables().putValue("repeating",true)).getId();

    CaseExecutionQuery stageQuery = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1");
    assertThat(stageQuery.count()).isOne();

    CaseExecution activeStageCaseExecution = stageQuery.active().singleResult();
    assertThat(activeStageCaseExecution).isNotNull();

    CaseExecution humanTaskCaseExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");

    // when (1)
    terminate(humanTaskCaseExecution.getId());

    // then (1)
    stageQuery = caseService.createCaseExecutionQuery().activityId("PI_Stage_1");
    assertThat(stageQuery.count()).isOne();

    activeStageCaseExecution = stageQuery.active().singleResult();
    assertThat(activeStageCaseExecution).isNotNull();

    humanTaskCaseExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");

    // when (2)
    caseService.setVariable(caseInstanceId,"repeating",false);
    terminate(humanTaskCaseExecution.getId());

    // then (2)
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    assertThat(query.count()).isOne();
    assertThat(query.singleResult().getId()).isEqualTo(caseInstanceId);
  }

  @Deployment
  @Test
  void testRepeatTaskWithoutEntryCriteriaOnCustomStandardEvent() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecutionQuery query = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1");
    assertThat(query.count()).isOne();

    CaseExecution enabledCaseExecution = query.enabled().singleResult();
    assertThat(enabledCaseExecution).isNotNull();

    // when (1)
    disable(enabledCaseExecution.getId());

    // then (1)
    query = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1");
    assertThat(query.count()).isEqualTo(2);

    enabledCaseExecution = query.enabled().singleResult();
    assertThat(enabledCaseExecution).isNotNull();

    // when (2)
    disable(enabledCaseExecution.getId());

    // then (2)
    query = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1");
    assertThat(query.count()).isEqualTo(3);

    enabledCaseExecution = query.enabled().singleResult();
    assertThat(enabledCaseExecution).isNotNull();

    // when (3)
    complete(caseInstanceId);

    // then (3)
    query = caseService.createCaseExecutionQuery();
    assertThat(query.count()).isOne();
    assertThat(query.singleResult().getId()).isEqualTo(caseInstanceId);
  }

  @Deployment
  @Test
  void testRepeatStageWithoutEntryCriteriaOnCustomStandardEvent() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecutionQuery stageQuery = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Stage_1");
    assertThat(stageQuery.count()).isOne();

    CaseExecution enabledStageCaseExecution = stageQuery.enabled().singleResult();
    assertThat(enabledStageCaseExecution).isNotNull();

    // when (1)
    disable(enabledStageCaseExecution.getId());

    // then (1)
    stageQuery = caseService.createCaseExecutionQuery().activityId("PI_Stage_1");
    assertThat(stageQuery.count()).isEqualTo(2);

    enabledStageCaseExecution = stageQuery.enabled().singleResult();
    assertThat(enabledStageCaseExecution).isNotNull();

    // when (2)
    disable(enabledStageCaseExecution.getId());

    // then (2)
    stageQuery = caseService.createCaseExecutionQuery().activityId("PI_Stage_1");
    assertThat(stageQuery.count()).isEqualTo(3);

    enabledStageCaseExecution = stageQuery.enabled().singleResult();
    assertThat(enabledStageCaseExecution).isNotNull();

    // when (3)
    complete(caseInstanceId);

    // then (3)
    CaseExecutionQuery query = caseService.createCaseExecutionQuery();
    assertThat(query.count()).isOne();
    assertThat(query.singleResult().getId()).isEqualTo(caseInstanceId);
  }

  @Deployment
  @Test
  void testNonRepeatableTaskDependsOnRepeatableTask() {
    // given
    createCaseInstance();

    CaseExecutionQuery availableQuery = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .available();

    // fire three times entry criteria of repeatable task
    // -> three enabled tasks
    // -> one available task
    fireEntryCriteria(availableQuery.singleResult().getId());
    fireEntryCriteria(availableQuery.singleResult().getId());
    fireEntryCriteria(availableQuery.singleResult().getId());

    // get any enabled task
    CaseExecutionQuery enabledQuery = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .active();

    String enabledTaskId = enabledQuery
        .listPage(0, 1)
        .get(0)
        .getId();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    assertThat(secondHumanTask).isNotNull();
    assertThat(secondHumanTask.isAvailable()).isTrue();

    // when
    complete(enabledTaskId);

    // then
    // there is only one instance of PI_HumanTask_2
    secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    assertThat(secondHumanTask).isNotNull();
    assertThat(secondHumanTask.isActive()).isTrue();
  }

  @Deployment
  @Test
  void testRepeatableTaskDependsOnAnotherRepeatableTask() {
    // given
    createCaseInstance();

    CaseExecutionQuery availableQuery = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .available();

    // fire three times entry criteria of repeatable task
    // -> three enabled tasks
    // -> one available task
    fireEntryCriteria(availableQuery.singleResult().getId());
    fireEntryCriteria(availableQuery.singleResult().getId());
    fireEntryCriteria(availableQuery.singleResult().getId());

    // get any enabled task
    CaseExecutionQuery activeQuery = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .active();

    String activeTaskId = activeQuery
        .listPage(0, 1)
        .get(0)
        .getId();

    // when (1)
    complete(activeTaskId);

    // then (1)
    CaseExecutionQuery query = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_2");
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.active().count()).isOne();
    assertThat(query.available().count()).isOne();

    // when (2)
    // get another enabled task
    activeTaskId = activeQuery
        .listPage(0, 1)
        .get(0)
        .getId();
    complete(activeTaskId);

    // then (2)
    query = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_2");
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.active().count()).isEqualTo(2);
    assertThat(query.available().count()).isOne();
  }

  @Deployment
  @Test
  void testLimitedRepetitions() {
    // given
    VariableMap variables = Variables.createVariables().putValue("repetition", 0);
    createCaseInstanceByKey("case", variables);

    CaseExecutionQuery availableQuery = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .available();

    // fire three times entry criteria of repeatable task
    // -> three enabled tasks
    // -> one available task
    fireEntryCriteria(availableQuery.singleResult().getId());
    fireEntryCriteria(availableQuery.singleResult().getId());
    fireEntryCriteria(availableQuery.singleResult().getId());

    // get any enabled task
    CaseExecutionQuery activeQuery = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .active();

    String activeTaskId = activeQuery
        .listPage(0, 1)
        .get(0)
        .getId();

    // when (1)
    complete(activeTaskId);

    // then (1)
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.active().count()).isOne();
    assertThat(query.available().count()).isOne();

    // when (2)
    activeTaskId = activeQuery
        .listPage(0, 1)
        .get(0)
        .getId();

    complete(activeTaskId);

    // then (2)
    query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.active().count()).isEqualTo(2);
    assertThat(query.available().count()).isOne();

    // when (3)
    activeTaskId = activeQuery
        .listPage(0, 1)
        .get(0)
        .getId();

    complete(activeTaskId);

    // then (3)
    query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");
    assertThat(query.count()).isEqualTo(3);
    assertThat(query.active().count()).isEqualTo(3);
    assertThat(query.available().count()).isZero();
  }

  @Deployment
  @Test
  void testLimitedSequentialRepetitions() {
    // given
    VariableMap variables = Variables.createVariables().putValue("repetition", 0);
    createCaseInstanceByKey("case", variables);

    CaseExecutionQuery activeQuery = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .active();
    String enabledCaseExecutionId = activeQuery.singleResult().getId();

    // when (1)
    complete(enabledCaseExecutionId);

    // then (1)
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1");
    assertThat(query.count()).isOne();
    assertThat(query.active().count()).isOne();

    // when (2)
    enabledCaseExecutionId = activeQuery.singleResult().getId();
    complete(enabledCaseExecutionId);

    // then (2)
    query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1");
    assertThat(query.count()).isOne();
    assertThat(query.active().count()).isOne();

    // when (3)
    enabledCaseExecutionId = activeQuery.singleResult().getId();
    complete(enabledCaseExecutionId);

    // then (3)
    query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1");
    assertThat(query.count()).isZero();
  }

  @Deployment
  @Test
  void testLimitedParallelRepetitions() {
    // given
    VariableMap variables = Variables.createVariables().putValue("repetition", 0);
    createCaseInstanceByKey("case", variables);

    // when (1)
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1");

    // then (1)
    assertThat(query.count()).isEqualTo(3);

    // when (2)
    // complete any task
    String caseExecutionId = query.listPage(0, 1).get(0).getId();
    complete(caseExecutionId);

    // then (2)
    assertThat(query.count()).isEqualTo(2);
  }

  @Deployment
  @Test
  void testAutoCompleteStage() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    String humanTask1 = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    complete(humanTask1);

    // then
    CaseExecution stage = queryCaseExecutionByActivityId("PI_Stage_1");
    assertThat(stage).isNull();

    CaseInstance caseInstance = (CaseInstance) queryCaseExecutionById(caseInstanceId);
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment
  @Test
  void testAutoCompleteStageWithoutEntryCriteria() {
    // given
    VariableMap variables = Variables.createVariables().putValue("manualActivation", false);
    String caseInstanceId = createCaseInstanceByKey("case", variables).getId();

    // then (1)
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1");
    assertThat(query.count()).isOne();

    assertThat(query.active().count()).isOne();
    String activeTaskId = query.singleResult().getId();

    // when (2)
    // completing active task
    complete(activeTaskId);

    // then (2)
    // the stage should be completed
    CaseExecution stage = queryCaseExecutionByActivityId("PI_Stage_1");
    assertThat(stage).isNull();

    CaseInstance caseInstance = (CaseInstance) queryCaseExecutionById(caseInstanceId);
    assertThat(caseInstance.isCompleted()).isTrue();
  }

  @Deployment
  @Test
  void testAutoCompleteStageAutoActivationRepeatableTask() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    assertThat(queryCaseExecutionByActivityId("PI_Stage_1")).isNotNull();

    // then (1)
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1");
    assertThat(query.count()).isOne();

    assertThat(query.active().count()).isOne();
    String activeTaskId = query.singleResult().getId();

    // when (2)
    // completing active task
    complete(activeTaskId);

    // then (2)
    query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1");
    assertThat(query.count()).isOne();

    assertThat(query.active().count()).isOne();

    CaseExecution stage = queryCaseExecutionByActivityId("PI_Stage_1");
    assertThat(stage).isNotNull();
    assertThat(stage.isActive()).isTrue();

    CaseInstance caseInstance = (CaseInstance) queryCaseExecutionById(caseInstanceId);
    assertThat(caseInstance.isActive()).isTrue();
  }

  @Deployment
  @Test
  void testAutoCompleteStageRequiredRepeatableTask() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    // then (1)
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1");
    assertThat(query.count()).isOne();

    assertThat(query.active().count()).isOne();
    String activeTaskId = query.singleResult().getId();

    // when (2)
    complete(activeTaskId);

    // then (2)
    query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1");
    assertThat(query.count()).isOne();
    assertThat(query.active().count()).isOne();

    CaseExecution stage = queryCaseExecutionByActivityId("PI_Stage_1");
    assertThat(stage).isNotNull();
    assertThat(stage.isActive()).isTrue();

    CaseInstance caseInstance = (CaseInstance) queryCaseExecutionById(caseInstanceId);
    assertThat(caseInstance.isActive()).isTrue();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/repetition/RepetitionRuleTest.testRepeatTask.cmmn")
  @Test
  void testShouldNotRepeatTaskAfterCompletion() {
    // given
    createCaseInstance();
    String humanTask1 = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when (1)
    complete(humanTask1);

    // then (1)
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.available().count()).isOne();
    assertThat(query.active().count()).isOne();

    // when (2)
    String humanTask2 = query.active().singleResult().getId();
    complete(humanTask2);

    // then (2)
    query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");
    assertThat(query.count()).isOne();
    assertThat(query.available().count()).isOne();
  }

  @Deployment
  @Test
  void testIgnoreRepeatOnStandardEvent() {
    // given
    createCaseInstance();

    String humanTask1 = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();
    complete(humanTask1);

    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");
    assertThat(query.count()).isEqualTo(2);

    // when
    String humanTask2 = query.enabled().singleResult().getId();
    disable(humanTask2);

    // then
    query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");
    assertThat(query.count()).isEqualTo(2);
  }

  @Deployment
  @Test
  void testDefaultValueWithoutCondition() {
    createCaseInstanceByKey("case");
    String humanTask1 = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    complete(humanTask1);

    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");

    assertThat(query.count()).isEqualTo(2);
    assertThat(query.available().count()).isOne();
    assertThat(query.active().count()).isOne();
  }

  @Deployment
  @Test
  void testDefaultValueWithEmptyCondition() {
    createCaseInstanceByKey("case");
    String humanTask1 = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    complete(humanTask1);

    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2");

    assertThat(query.count()).isEqualTo(2);
    assertThat(query.available().count()).isOne();
    assertThat(query.active().count()).isOne();
  }

  // helper ////////////////////////////////////////////////////////

  protected void fireEntryCriteria(final String caseExecutionId) {
    executeHelperCaseCommand(new HelperCaseCommand() {
      @Override
      public void execute() {
        getExecution(caseExecutionId).fireEntryCriteria();
      }
    });
  }

}
