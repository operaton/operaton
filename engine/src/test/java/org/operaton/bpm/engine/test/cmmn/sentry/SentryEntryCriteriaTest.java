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
package org.operaton.bpm.engine.test.cmmn.sentry;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseSentryPartQueryImpl;
import org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState;
import org.operaton.bpm.engine.impl.cmmn.execution.CmmnSentryPart;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionQuery;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
class SentryEntryCriteriaTest extends CmmnTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testSequenceEnableTask.cmmn"})
  @Test
  void testSequenceEnableTask() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isAvailable()).isTrue();

    // (1) then
    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isAvailable()).isTrue();

    assertThat(caseService.getVariable(caseInstanceId, "start")).isNull();

    // (2) when
    complete(firstHumanTaskId);

    // (2) then
    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isEnabled()).isTrue();

    Object enableVariable = caseService.getVariable(caseInstanceId, "enable");
    assertThat(enableVariable).isNotNull();
    assertThat((Boolean) enableVariable).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testSequenceAutoStartTask.cmmn"})
  @Test
  void testSequenceAutoStartTask() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isAvailable()).isTrue();

    // (1) then
    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isAvailable()).isTrue();

    assertThat(caseService.getVariable(caseInstanceId, "enable")).isNull();
    assertThat(caseService.getVariable(caseInstanceId, "start")).isNull();

    // (2) when
    complete(firstHumanTaskId);

    // (2) then
    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isActive()).isTrue();

    assertThat(caseService.getVariable(caseInstanceId, "enable")).isNull();
    Object startVariable = caseService.getVariable(caseInstanceId, "start");
    assertThat(startVariable).isNotNull();
    assertThat((Boolean) startVariable).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testSequenceEnableStage.cmmn"})
  @Test
  void testSequenceEnableStage() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution stage = queryCaseExecutionByActivityId("PI_Stage_1");
    String stageId = stage.getId();
    assertThat(stage.isAvailable()).isTrue();

    // (1) then
    stage = queryCaseExecutionById(stageId);
    assertThat(stage.isAvailable()).isTrue();

    assertThat(caseService.getVariable(caseInstanceId, "enable")).isNull();

    // (2) when
    complete(firstHumanTaskId);

    // (2) then
    stage = queryCaseExecutionById(stageId);
    assertThat(stage.isEnabled()).isTrue();

    Object enableVariable = caseService.getVariable(caseInstanceId, "enable");
    assertThat(enableVariable).isNotNull();
    assertThat((Boolean) enableVariable).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testSequenceAutoStartStage.cmmn"})
  @Test
  void testSequenceAutoStartStage() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution stage = queryCaseExecutionByActivityId("PI_Stage_1");
    String stageId = stage.getId();
    assertThat(stage.isAvailable()).isTrue();

    // (1) then
    stage = queryCaseExecutionById(stageId);
    assertThat(stage.isAvailable()).isTrue();

    assertThat(caseService.getVariable(caseInstanceId, "enable")).isNull();
    assertThat(caseService.getVariable(caseInstanceId, "start")).isNull();

    // (2) when
    complete(firstHumanTaskId);

    // (2) then
    stage = queryCaseExecutionById(stageId);
    assertThat(stage.isActive()).isTrue();

    assertThat(caseService.getVariable(caseInstanceId, "enable")).isNull();
    Object startVariable = caseService.getVariable(caseInstanceId, "start");
    assertThat(startVariable).isNotNull();
    assertThat((Boolean) startVariable).isTrue();

    CaseExecutionQuery query = caseService
      .createCaseExecutionQuery()
      .enabled();

    assertThat(query.count()).isEqualTo(2);

    for (CaseExecution child : query.list()) {
      assertThat(child.getParentId()).isEqualTo(stageId);
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testSequenceOccurMilestone.cmmn"})
  @Test
  void testSequenceOccurMilestone() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution milestone = queryCaseExecutionByActivityId("PI_Milestone_1");
    String milestoneId = milestone.getId();
    assertThat(milestone.isAvailable()).isTrue();

    // (1) then
    milestone = queryCaseExecutionById(milestoneId);
    assertThat(milestone.isAvailable()).isTrue();

    assertThat(caseService.getVariable(caseInstanceId, "occur")).isNull();

    // (2) when
    complete(firstHumanTaskId);

    // (2) then
    milestone = queryCaseExecutionById(milestoneId);
    assertThat(milestone).isNull();

    Object occurVariable = caseService.getVariable(caseInstanceId, "occur");
    assertThat(occurVariable).isNotNull();
    assertThat((Boolean) occurVariable).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testSequence.cmmn"})
  @Test
  void testSequence() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isAvailable()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();
    assertThat(thirdHumanTask.isAvailable()).isTrue();

    // (1) then
    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isAvailable()).isTrue();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isAvailable()).isTrue();

    assertThat(caseService.getVariable(caseInstanceId, "start")).isNull();

    // (2) when (complete first human task) /////////////////////////////////////////////
    complete(firstHumanTaskId);

    // (2) then
    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isActive()).isTrue();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isAvailable()).isTrue();

    Object enableVariable = caseService.getVariable(caseInstanceId, "start");
    assertThat(enableVariable).isNotNull();
    assertThat((Boolean) enableVariable).isTrue();

    // reset variable
    caseService
      .withCaseExecution(caseInstanceId)
      .removeVariable("start")
      .execute();

    // (3) then
    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isAvailable()).isTrue();

    assertThat(caseService.getVariable(caseInstanceId, "start")).isNull();

    // (4) when (complete second human task) //////////////////////////////////////////
    complete(secondHumanTaskId);

    // (4) then
    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isActive()).isTrue();

    enableVariable = caseService.getVariable(caseInstanceId, "start");
    assertThat(enableVariable).isNotNull();
    assertThat((Boolean) enableVariable).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testSequenceWithIfPart.cmmn"})
  @Test
  void testSequenceWithIfPartNotSatisfied() {
    // given
    createCaseInstance();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isAvailable()).isTrue();

    // when
    caseService
      .withCaseExecution(firstHumanTaskId)
      .setVariable("value", 99)
      .complete();

    // then
    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask).isNull();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isAvailable()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testSequenceWithIfPart.cmmn"})
  @Test
  void testSequenceWithIfPartSatisfied() {
    // given
    createCaseInstance();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isAvailable()).isTrue();

    // when
    caseService
      .withCaseExecution(firstHumanTaskId)
      .setVariable("value", 100)
      .complete();

    // then
    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask).isNull();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isActive()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testAndFork.cmmn"})
  @Test
  void testAndFork() {
    // given
    createCaseInstance();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isAvailable()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();
    assertThat(thirdHumanTask.isAvailable()).isTrue();

    CaseSentryPartQueryImpl query = createCaseSentryPartQuery();
    CmmnSentryPart part = query.singleResult();
    assertThat(part.isSatisfied()).isFalse();

    // when
    complete(firstHumanTaskId);

    // then
    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isActive()).isTrue();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isActive()).isTrue();

    part = query.singleResult();
    assertThat(part).isNotNull();
    assertThat(part.isSatisfied()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testAndJoin.cmmn"})
  @Test
  void testAndJoin() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isActive()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();
    assertThat(thirdHumanTask.isAvailable()).isTrue();

    // (1) when
    complete(firstHumanTaskId);

    // (1) then
    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isAvailable()).isTrue();

    assertThat(caseService.getVariable(caseInstanceId, "start")).isNull();

    // (2) when
    complete(secondHumanTaskId);

    // (2) then
    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isActive()).isTrue();

    Object startVariable = caseService.getVariable(caseInstanceId, "start");
    assertThat(startVariable).isNotNull();
    assertThat((Boolean) startVariable).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testSequenceCombinedWithAndJoin.cmmn"})
  @Test
  void testSequenceCombinedWithAndJoin() {
    // given
    createCaseInstance();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isAvailable()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();
    assertThat(thirdHumanTask.isAvailable()).isTrue();

    // (1) when
    complete(firstHumanTaskId);

    // (1) then
    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isActive()).isTrue();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isAvailable()).isTrue(); // still available

    // (2) when
    complete(secondHumanTaskId);

    // (2) then
    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isActive()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testOrFork.cmmn"})
  @Test
  void testOrFork() {
    // given
    createCaseInstance();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isAvailable()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();
    assertThat(thirdHumanTask.isAvailable()).isTrue();

    // when
    caseService
      .withCaseExecution(firstHumanTaskId)
      .setVariable("value", 80)
      .complete();

    // then
    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isAvailable()).isTrue();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isActive()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testOrJoin.cmmn"})
  @Test
  void testOrJoin() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isActive()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();
    assertThat(thirdHumanTask.isAvailable()).isTrue();

    // (1) when
    complete(firstHumanTaskId);

    // (1) then
    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isActive()).isTrue();

    Object startVariable = caseService.getVariable(caseInstanceId, "start");
    assertThat(startVariable).isNotNull();
    assertThat((Boolean) startVariable).isTrue();

    // (2) when
    complete(secondHumanTaskId);

    // (2) then
    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isActive()).isTrue(); // is still active
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testCycle.cmmn"})
  @Test
  void testCycle() {
    // given
    createCaseInstance();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isAvailable()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isAvailable()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();
    assertThat(thirdHumanTask.isAvailable()).isTrue();


    assertThatThrownBy(() ->
      manualStart(firstHumanTaskId)).withFailMessage("First human task should be available.")
      .isInstanceOf(NotAllowedException.class);

    assertThatThrownBy(() ->
      manualStart(secondHumanTaskId)).withFailMessage("It should not be possible to start the second human task manually.")
      .isInstanceOf(NotAllowedException.class);

    assertThatThrownBy(() ->
      manualStart(thirdHumanTaskId)).withFailMessage("It should not be possible to third the second human task manually.")
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testEnableByInstanceCreation.cmmn"})
  @Test
  void testEnableByInstanceCreation() {
    // given + when
    createCaseInstance();

    // then
    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    assertThat(secondHumanTask.isActive()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testEnableOnParentSuspendInsideStage.cmmn"})
  @Test
  @Disabled("Fixme")
  void testEnableOnParentSuspendInsideStage() {
    // given
    createCaseInstance();

    CaseExecution stage = queryCaseExecutionByActivityId("PI_Stage_1");
    String stageId = stage.getId();

    manualStart(stageId);

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();

    assertThat(firstHumanTask.isEnabled()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();

    assertThat(secondHumanTask.isAvailable()).isTrue();

    // (1) when
    suspend(stageId);

    // (1) then
    stage = queryCaseExecutionById(stageId);
    assertThat(((CaseExecutionEntity) stage).isSuspended()).isTrue();

    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(((CaseExecutionEntity) firstHumanTask).isSuspended()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(((CaseExecutionEntity) secondHumanTask).isSuspended()).isTrue();
    assertThat(((CaseExecutionEntity) secondHumanTask).getPreviousState()).isEqualTo(CaseExecutionState.ENABLED);

    // (2) when
    resume(stageId);

    // (2) then
    stage = queryCaseExecutionById(stageId);
    assertThat(stage.isActive()).isTrue();

    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask.isEnabled()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isEnabled()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testEnableOnParentResumeInsideStage.cmmn"})
  @Test
  @Disabled("Fixme")
  void testEnableOnParentResumeInsideStage() {
    // given
    createCaseInstance();

    CaseExecution stage = queryCaseExecutionByActivityId("PI_Stage_1");
    String stageId = stage.getId();

    manualStart(stageId);

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();

    assertThat(firstHumanTask.isEnabled()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();

    assertThat(secondHumanTask.isAvailable()).isTrue();

    // (1) when
    suspend(stageId);

    // (1) then
    stage = queryCaseExecutionById(stageId);
    assertThat(((CaseExecutionEntity) stage).isSuspended()).isTrue();

    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(((CaseExecutionEntity) firstHumanTask).isSuspended()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(((CaseExecutionEntity) secondHumanTask).isSuspended()).isTrue();
    assertThat(((CaseExecutionEntity) secondHumanTask).getPreviousState()).isEqualTo(CaseExecutionState.AVAILABLE);

    // (2) when
    resume(stageId);

    // (2) then
    stage = queryCaseExecutionById(stageId);
    assertThat(stage.isActive()).isTrue();

    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask.isEnabled()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isEnabled()).isTrue();
  }

  /**
   * Please note that suspension and/or resuming is currently
   * not supported by the public API. Furthermore the given
   * test is not a very useful use case in that just a milestone
   * will be suspended.
   */
  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testResume.cmmn"})
  @Test
  @Disabled("Fixme")
  void testResume() {
    // given
    createCaseInstance();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();

    assertThat(firstHumanTask.isEnabled()).isTrue();

    CaseExecution milestone = queryCaseExecutionByActivityId("PI_Milestone_1");
    String milestoneId = milestone.getId();

    assertThat(milestone.isAvailable()).isTrue();

    suspend(milestoneId);

    // (1) when
    manualStart(firstHumanTaskId);
    complete(firstHumanTaskId);

    // (1) then
    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask).isNull();

    milestone = queryCaseExecutionById(milestoneId);
    assertThat(((CaseExecutionEntity) milestone).isSuspended()).isTrue();

    // (2) when
    resume(milestoneId);

    // (2)
    milestone = queryCaseExecutionById(milestoneId);
    assertThat(milestone).isNull();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testFireAlsoNotAffectedSentries.cmmn"})
  @Test
  void testFireAlsoNotAffectedSentries() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();

    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();

    assertThat(secondHumanTask.isAvailable()).isTrue();

    CaseExecution milestone = queryCaseExecutionByActivityId("PI_Milestone_1");
    String milestoneId = milestone.getId();

    assertThat(milestone.isAvailable()).isTrue();

    caseService
      .withCaseExecution(caseInstanceId)
      .setVariable("value", 99)
      .execute();

    // (1) when
    complete(firstHumanTaskId);

    // (1) then
    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask).isNull();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isAvailable()).isTrue();

    // (2) when
    caseService
      .withCaseExecution(caseInstanceId)
      .setVariable("value", 101)
      .execute();

    // (2) then
    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isActive()).isTrue();

    milestone = queryCaseExecutionById(milestoneId);
    // milestone occurs when the sentry was evaluated successfully after value is set to 101
    assertThat(milestone).isNull();
  }

  @Deployment
  @Test
  void testCaseFileItemOnPart() {
    createCaseInstance().getId();

    CaseExecution humanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");

    // sentry has been ignored
    assertThat(humanTask.isActive()).isTrue();
  }

  @Deployment
  @Test
  void testReusableStage() {
    // given
    createCaseInstance();

    String firstStageId = queryCaseExecutionByActivityId("PI_Stage_1").getId();
    String secondStageId = queryCaseExecutionByActivityId("PI_Stage_2").getId();

    List<CaseExecution> humanTasks = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_1")
      .active()
      .list();
    assertThat(humanTasks).hasSize(2);

    String humanTaskInsideFirstStageId = null;
    if (((CaseExecutionEntity) humanTasks.get(0)).getParentId().equals(firstStageId)) {
      humanTaskInsideFirstStageId = humanTasks.get(0).getId();
    }
    else {
      humanTaskInsideFirstStageId = humanTasks.get(1).getId();
    }

    // when
    complete(humanTaskInsideFirstStageId);

    // then
    CaseExecution secondHumanTaskInsideFirstStage = caseService
      .createCaseExecutionQuery()
      .activityId("PI_HumanTask_2")
      .active()
      .singleResult();
    assertThat(((CaseExecutionEntity) secondHumanTaskInsideFirstStage).getParentId()).isEqualTo(firstStageId);

    // PI_HumanTask_1 in PI_Stage_2 is enabled
    CaseExecution firstHumanTaskInsideSecondStage = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertThat(firstHumanTaskInsideSecondStage).isNotNull();
    assertThat(firstHumanTaskInsideSecondStage.isActive()).isTrue();
    assertThat(((CaseExecutionEntity) firstHumanTaskInsideSecondStage).getParentId()).isEqualTo(secondStageId);

    // PI_HumanTask_2 in PI_Stage_2 is available
    CaseExecution secondHumanTaskInsideSecondStage = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2")
        .available()
        .singleResult();
    assertThat(secondHumanTaskInsideSecondStage).isNotNull();
    assertThat(secondHumanTaskInsideSecondStage.isAvailable()).isTrue();
    assertThat(((CaseExecutionEntity) secondHumanTaskInsideSecondStage).getParentId()).isEqualTo(secondStageId);
  }

  /**
   * CAM-3226
   */
  @Deployment
  @Test
  void testSentryShouldNotBeEvaluatedAfterStageComplete() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    // when
    CaseExecution stageExecution = caseService.createCaseExecutionQuery().activityId("PI_Stage_1").singleResult();
    assertThat(stageExecution).isNotNull();

    // .. there is a local stage variable
    caseService.setVariableLocal(stageExecution.getId(), "value", 99);

    // .. and the stage is activated (such that the tasks are instantiated)
    caseService.manuallyStartCaseExecution(stageExecution.getId());

    CaseExecution task1Execution = caseService.createCaseExecutionQuery().activityId("PI_HumanTask_1").singleResult();
    assertThat(task1Execution).isNotNull();

    // then
    // .. completing the stage should be successful; evaluating Sentry_1 should not fail
    caseService.completeCaseExecution(task1Execution.getId());
    stageExecution = caseService.createCaseExecutionQuery().activityId("PI_Stage_1").singleResult();
    assertThat(stageExecution).isNull();

    // .. and the case plan model should have completed
    CaseExecution casePlanModelExecution = caseService.createCaseExecutionQuery().caseExecutionId(caseInstanceId).singleResult();
    assertThat(casePlanModelExecution).isNotNull();
    assertThat(casePlanModelExecution.isActive()).isFalse();

    caseService.closeCaseInstance(caseInstanceId);
  }

  @Deployment
  @Test
  void testIfPartOnCaseInstanceCreate() {

    // when
    createCaseInstanceByKey("case", Variables.putValue("value", 101));

    // then
    CaseExecution caseExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertThat(caseExecution.isActive()).isTrue();

  }

  @Deployment
  @Test
  void testIfPartOnCaseInstanceCreateWithSentry() {

    // when
    createCaseInstanceByKey("case", Variables.putValue("myVar", 101));

    // then
    CaseExecution caseExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertThat(caseExecution.isActive()).isTrue();

  }

  @Deployment(resources = {
      "org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.testShouldNotTriggerCompletionTwice.cmmn",
      "org/operaton/bpm/engine/test/cmmn/sentry/SentryEntryCriteriaTest.noop.bpmn20.xml"
  })
  @Test
  void testShouldNotTriggerCompletionTwice() {
    // when
    CaseInstance ci = caseService.createCaseInstanceByKey("case");

    // then
    assertThat(ci.isCompleted()).isTrue();
  }

}
