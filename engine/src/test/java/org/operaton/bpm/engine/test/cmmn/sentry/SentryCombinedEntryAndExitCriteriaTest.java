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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.impl.cmmn.execution.CaseExecutionState;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Roman Smirnov
 *
 */
@Disabled("FIXME: Error on 'manualStart': The case execution is already in state 'active'")
class SentryCombinedEntryAndExitCriteriaTest extends CmmnTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryCombinedEntryAndExitCriteriaTest.testParentResumeInsideStage.cmmn"})
  @Test
  void parentResumeInsideStage() {
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

    assertThat(secondHumanTask.isEnabled()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();

    assertThat(thirdHumanTask.isAvailable()).isTrue();

    // (1) when
    suspend(stageId);

    // (1) then
    stage = queryCaseExecutionById(stageId);
    assertThat(((CaseExecutionEntity) stage).isSuspended()).isTrue();

    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(((CaseExecutionEntity) firstHumanTask).isSuspended()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(((CaseExecutionEntity) secondHumanTask).isSuspended()).isTrue();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(((CaseExecutionEntity) thirdHumanTask).isSuspended()).isTrue();

    // (2) when
    resume(stageId);

    // (2) then
    stage = queryCaseExecutionById(stageId);
    assertThat(stage.isActive()).isTrue();

    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask.isEnabled()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNull();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isEnabled()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryCombinedEntryAndExitCriteriaTest.testParentSuspendInsideStage.cmmn"})
  @Test
  void parentSuspendInsideStage() {
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

    assertThat(secondHumanTask.isEnabled()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();

    assertThat(thirdHumanTask.isAvailable()).isTrue();

    // when
    suspend(stageId);

    // then
    stage = queryCaseExecutionById(stageId);
    assertThat(((CaseExecutionEntity) stage).isSuspended()).isTrue();

    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(((CaseExecutionEntity) firstHumanTask).isSuspended()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNull();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(((CaseExecutionEntity) thirdHumanTask).isSuspended()).isTrue();
    assertThat(((CaseExecutionEntity) thirdHumanTask).getPreviousState()).isEqualTo(CaseExecutionState.ENABLED);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryCombinedEntryAndExitCriteriaTest.testParentResumeInsideStageDifferentPlanItemOrder.cmmn"})
  @Test
  void parentResumeInsideStageDifferentPlanItemOrder() {
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

    assertThat(secondHumanTask.isEnabled()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();

    assertThat(thirdHumanTask.isAvailable()).isTrue();

    // (1) when
    suspend(stageId);

    // (1) then
    stage = queryCaseExecutionById(stageId);
    assertThat(((CaseExecutionEntity) stage).isSuspended()).isTrue();

    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(((CaseExecutionEntity) firstHumanTask).isSuspended()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(((CaseExecutionEntity) secondHumanTask).isSuspended()).isTrue();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(((CaseExecutionEntity) thirdHumanTask).isSuspended()).isTrue();

    // (2) when
    resume(stageId);

    // (2) then
    stage = queryCaseExecutionById(stageId);
    assertThat(stage.isActive()).isTrue();

    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask.isEnabled()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNull();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isEnabled()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryCombinedEntryAndExitCriteriaTest.testParentSuspendInsideStageDifferentPlanItemOrder.cmmn"})
  @Test
  void parentSuspendInsideStageDifferentPlanItemOrder() {
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

    assertThat(secondHumanTask.isEnabled()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();

    assertThat(thirdHumanTask.isAvailable()).isTrue();

    // when
    suspend(stageId);

    // then
    stage = queryCaseExecutionById(stageId);
    assertThat(((CaseExecutionEntity) stage).isSuspended()).isTrue();

    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(((CaseExecutionEntity) firstHumanTask).isSuspended()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNull();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(((CaseExecutionEntity) thirdHumanTask).isSuspended()).isTrue();
    assertThat(((CaseExecutionEntity) thirdHumanTask).getPreviousState()).isEqualTo(CaseExecutionState.ENABLED);

  }
}
