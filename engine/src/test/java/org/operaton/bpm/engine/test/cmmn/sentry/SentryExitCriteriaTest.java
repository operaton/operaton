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
package org.operaton.bpm.engine.test.cmmn.sentry;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.impl.cmmn.entity.runtime.CaseExecutionEntity;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.junit.Test;

/**
 * @author Roman Smirnov
 *
 */
public class SentryExitCriteriaTest extends CmmnTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryExitCriteriaTest.testExitTask.cmmn"})
  @Test
  public void testExitTask() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();

    assertThat(secondHumanTask.isActive()).isTrue();

    assertThat(caseService.getVariable(caseInstanceId, "exit")).isNull();

    // (1) when
    complete(firstHumanTaskId);

    // (2) then
    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNull();

    Object exitVariable = caseService.getVariable(caseInstanceId, "exit");
    assertThat(exitVariable).isNotNull();
    assertThat((Boolean) exitVariable).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryExitCriteriaTest.testExitStage.cmmn"})
  @Test
  public void testExitStage() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution stage = queryCaseExecutionByActivityId("PI_Stage_1");
    String stageId = stage.getId();
    assertThat(stage.isActive()).isTrue();

    stage = queryCaseExecutionById(stageId);
    assertThat(stage.isActive()).isTrue();

    CaseExecution milestone = queryCaseExecutionByActivityId("PI_Milestone_1");
    String milestoneId = milestone.getId();
    assertThat(milestone.isAvailable()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isActive()).isTrue();

    // (2) then
    stage = queryCaseExecutionById(stageId);
    assertThat(stage.isActive()).isTrue();

    assertThat(caseService.getVariable(caseInstanceId, "exit")).isNull();
    assertThat(caseService.getVariable(caseInstanceId, "parentTerminate")).isNull();

    // (2) when
    complete(firstHumanTaskId);

    // (2) then
    stage = queryCaseExecutionById(stageId);
    assertThat(stage).isNull();

    milestone = queryCaseExecutionById(milestoneId);
    assertThat(milestone).isNull();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNull();

    Object exitVariable = caseService.getVariable(caseInstanceId, "exit");
    assertThat(exitVariable).isNotNull();
    assertThat((Boolean) exitVariable).isTrue();

    Object parentTerminateVariable = caseService.getVariable(caseInstanceId, "parentTerminate");
    assertThat(parentTerminateVariable).isNotNull();
    assertThat((Boolean) parentTerminateVariable).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryExitCriteriaTest.testAndJoin.cmmn"})
  @Test
  public void testAndJoin() {
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
    assertThat(thirdHumanTask.isActive()).isTrue();

    // (1) when
    complete(firstHumanTaskId);

    // (1) then
    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask.isActive()).isTrue();

    assertThat(caseService.getVariable(caseInstanceId, "exit")).isNull();

    // (2) when
    complete(secondHumanTaskId);

    // (2) then
    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask).isNull();

    Object exitVariable = caseService.getVariable(caseInstanceId, "exit");
    assertThat(exitVariable).isNotNull();
    assertThat((Boolean) exitVariable).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryExitCriteriaTest.testAndFork.cmmn"})
  @Test
  public void testAndFork() {
    // given
    createCaseInstance();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isActive()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();
    assertThat(thirdHumanTask.isActive()).isTrue();

    // when
    complete(firstHumanTaskId);

    // then
    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNull();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryExitCriteriaTest.testOrJoin.cmmn"})
  @Test
  public void testOrJoin() {
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
    assertThat(thirdHumanTask.isActive()).isTrue();

    // (1) when
    complete(firstHumanTaskId);

    // (1) then
    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask).isNull();

    Object exitVariable = caseService.getVariable(caseInstanceId, "exit");
    assertThat(exitVariable).isNotNull();
    assertThat((Boolean) exitVariable).isTrue();

    // (2) when
    complete(secondHumanTaskId);

    // (2) then
    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryExitCriteriaTest.testOrFork.cmmn"})
  @Test
  public void testOrFork() {
    // given
    createCaseInstance();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isActive()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();
    assertThat(thirdHumanTask.isActive()).isTrue();

    // when
    caseService
      .withCaseExecution(firstHumanTaskId)
      .setVariable("value", 80)
      .complete();

    // then
    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNotNull();
    assertThat(secondHumanTask.isActive()).isTrue();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask).isNull();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryExitCriteriaTest.testCycle.cmmn"})
  @Test
  public void testCycle() {
    // given
    createCaseInstance();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isActive()).isTrue();

    CaseExecution thirdHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_3");
    String thirdHumanTaskId = thirdHumanTask.getId();
    assertThat(thirdHumanTask.isActive()).isTrue();

    // when
    complete(firstHumanTaskId);

    // then
    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask).isNull();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNull();

    thirdHumanTask = queryCaseExecutionById(thirdHumanTaskId);
    assertThat(thirdHumanTask).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryExitCriteriaTest.testExitTaskWithIfPart.cmmn"})
  @Test
  public void testExitTaskWithIfPartSatisfied() {
    // given
    createCaseInstance();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isActive()).isTrue();

    // when
    caseService
      .withCaseExecution(firstHumanTaskId)
      .setVariable("value", 100)
      .complete();

    // then
    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask).isNull();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNull();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryExitCriteriaTest.testExitTaskWithIfPart.cmmn"})
  @Test
  public void testExitTaskWithIfPartNotSatisfied() {
    // given
    createCaseInstance();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();
    assertThat(firstHumanTask.isActive()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();
    assertThat(secondHumanTask.isActive()).isTrue();

    // when
    caseService
      .withCaseExecution(firstHumanTaskId)
      .setVariable("value", 99)
      .complete();

    // then
    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask).isNull();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isActive()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryExitCriteriaTest.testExitCriteriaOnCasePlanModel.cmmn"})
  @Test
  public void testExitCriteriaOnCasePlanModel() {
    // given
    String caseInstanceId = createCaseInstance().getId();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();

    assertThat(firstHumanTask.isActive()).isTrue();

    // when
    complete(firstHumanTaskId);

    // then
    CaseExecution caseInstance = queryCaseExecutionById(caseInstanceId);
    assertThat(caseInstance.isTerminated()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryExitCriteriaTest.testExitOnParentSuspendInsideStage.cmmn"})
  public void FAILING_testExitOnParentSuspendInsideStage() {
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

    // when
    suspend(stageId);

    // then
    stage = queryCaseExecutionById(stageId);
    assertThat(((CaseExecutionEntity) stage).isSuspended()).isTrue();

    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(((CaseExecutionEntity) firstHumanTask).isSuspended()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNull();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryExitCriteriaTest.testExitOnParentResumeInsideStage.cmmn"})
  public void FAILING_testExitOnParentResumeInsideStage() {
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

    // (1) when
    suspend(stageId);

    // (1) then
    stage = queryCaseExecutionById(stageId);
    assertThat(((CaseExecutionEntity) stage).isSuspended()).isTrue();

    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(((CaseExecutionEntity) firstHumanTask).isSuspended()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(((CaseExecutionEntity) secondHumanTask).isSuspended()).isTrue();

    // (2) when
    resume(stageId);

    // (2) then
    stage = queryCaseExecutionById(stageId);
    assertThat(((CaseExecutionEntity) stage).isActive()).isTrue();

    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask.isEnabled()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryExitCriteriaTest.testExitActiveTask.cmmn"})
  @Test
  public void testExitActiveTask() {
    // given
    createCaseInstance();

    CaseExecution firstHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    String firstHumanTaskId = firstHumanTask.getId();

    assertThat(firstHumanTask.isEnabled()).isTrue();

    CaseExecution secondHumanTask = queryCaseExecutionByActivityId("PI_HumanTask_2");
    String secondHumanTaskId = secondHumanTask.getId();

    assertThat(secondHumanTask.isEnabled()).isTrue();

    manualStart(secondHumanTaskId);

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isActive()).isTrue();

    Task secondTask = taskService
        .createTaskQuery()
        .singleResult();
    assertThat(secondTask).isNotNull();

    // when
    manualStart(firstHumanTaskId);

    // then
    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask.isActive()).isTrue();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNull();

    secondTask = taskService
        .createTaskQuery()
        .taskId(secondTask.getId())
        .singleResult();
    assertThat(secondTask).isNull();

  }
}
