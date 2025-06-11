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

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */
class SentryVariableOnPartExitCriteriaTest extends CmmnTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryVariableOnPartExitCriteriaTest.testExitTaskWithVariableOnPart.cmmn"})
  @Test
  void testExitTaskWithVariableOnPartSatisfied() {
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
      .setVariable("variable_1", 100)
      .complete();

    // then
    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask).isNull();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryVariableOnPartExitCriteriaTest.testExitTaskWithVariableOnPart.cmmn"})
  @Test
  void testExitTaskWithVariableOnPartNotSatisfied() {
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
      .setVariable("variable", 100)
      .complete();

    // then
    firstHumanTask = queryCaseExecutionById(firstHumanTaskId);
    assertThat(firstHumanTask).isNull();

    secondHumanTask = queryCaseExecutionById(secondHumanTaskId);
    assertThat(secondHumanTask.isActive()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryVariableOnPartExitCriteriaTest.testExitTaskWithMultipleOnPart.cmmn"})
  @Test
  void testExitTaskWithMultipleOnPartSatisfied() {
    // given
    createCaseInstance();

    CaseExecution stageExecution;

    CaseExecution humanTask1 = queryCaseExecutionByActivityId("HumanTask_1");
    assertThat(humanTask1.isActive()).isTrue();

    CaseExecution humanTask2 = queryCaseExecutionByActivityId("HumanTask_2");
    assertThat(humanTask2.isActive()).isTrue();

    complete(humanTask1.getId());

    stageExecution = queryCaseExecutionByActivityId("Stage_1");
    // Still if part and variable on part conditions are yet to be satisfied for the exit criteria
    assertThat(stageExecution).isNotNull();

    caseService.setVariable(stageExecution.getId(), "value", 99);
    stageExecution = queryCaseExecutionByActivityId("Stage_1");
    // Still if part is yet to be satisfied for the exit criteria
    assertThat(stageExecution).isNotNull();

    caseService.setVariable(stageExecution.getId(), "value", 101);
    stageExecution = queryCaseExecutionByActivityId("Stage_1");
    // exit criteria satisfied
    assertThat(stageExecution).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/sentry/SentryVariableOnPartExitCriteriaTest.testExitTasksOfDifferentScopes.cmmn"})
  @Test
  void testExitMultipleTasksOfDifferentScopes() {
    // given
    createCaseInstance();

    CaseExecution stageExecution1 = queryCaseExecutionByActivityId("Stage_1");

    caseService.setVariable(stageExecution1.getId(), "value", 101);

    stageExecution1 = queryCaseExecutionByActivityId("Stage_1");
    assertThat(stageExecution1).isNull();

    CaseExecution stageExecution2 = queryCaseExecutionByActivityId("Stage_2");
    assertThat(stageExecution2).isNull();

  }
}
