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
package org.operaton.bpm.engine.test.cmmn.cmmn10;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionQuery;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Roman Smirnov
 *
 */
class Cmmn10CompatibilityTest extends CmmnTest {

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/cmm10/Cmmn10CompatibilityTest.testRequiredRule.cmmn")
  @Test
  void testRequiredRule() {
    CaseInstance caseInstance =
        createCaseInstanceByKey("case", Variables.createVariables().putValue("required", true));
    var caseInstanceId = caseInstance.getId();

    CaseExecution taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");

    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isRequired()).isTrue();

    assertThatThrownBy(() -> caseService.completeCaseExecution(caseInstanceId)).isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/cmm10/Cmmn10CompatibilityTest.testManualActivationRule.cmmn")
  @Test
  void testManualActivationRule() {
    createCaseInstanceByKey("case", Variables.createVariables().putValue("manual", false));

    CaseExecution taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");

    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isActive()).isTrue();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/cmm10/Cmmn10CompatibilityTest.testManualActivationRuleWithoutCondition.cmmn")
  @Test
  void testManualActivationRuleWithoutCondition() {
    createCaseInstanceByKey("case", Variables.createVariables().putValue("manual", false));

    CaseExecution taskExecution = queryCaseExecutionByActivityId("PI_HumanTask_1");

    assertThat(taskExecution).isNotNull();
    assertThat(taskExecution.isEnabled()).isTrue();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/cmm10/Cmmn10CompatibilityTest.testRepetitionRule.cmmn")
  @Test
  void testRepetitionRule() {
    // given
    createCaseInstanceByKey("case", Variables.createVariables().putValue("repetition", true));

    String secondHumanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_2").getId();

    // when
    complete(secondHumanTaskId);

    // then
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1");
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.available().count()).isEqualTo(1);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/cmm10/Cmmn10CompatibilityTest.testRepetitionRuleWithoutEntryCriteria.cmmn")
  @Test
  void testRepetitionRuleWithoutEntryCriteria() {
    // given
    createCaseInstanceByKey("case", Variables.createVariables().putValue("repetition", true));

    String firstHumanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    complete(firstHumanTaskId);

    // then
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1");
    assertThat(query.count()).isEqualTo(1);
    assertThat(query.active().count()).isEqualTo(1);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/cmm10/Cmmn10CompatibilityTest.testRepetitionRuleCustomStandardEvent.cmmn")
  @Test
  void testRepetitionRuleWithoutEntryCriteriaAndCustomStandardEvent() {
    // given
    createCaseInstanceByKey("case", Variables.createVariables().putValue("repetition", true));

    String firstHumanTaskId = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    disable(firstHumanTaskId);

    // then
    CaseExecutionQuery query = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1");
    assertThat(query.count()).isEqualTo(2);
    assertThat(query.enabled().count()).isEqualTo(1);
    assertThat(query.disabled().count()).isEqualTo(1);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/cmm10/Cmmn10CompatibilityTest.testPlanItemEntryCriterion.cmmn")
  @Test
  void testPlanItemEntryCriterion() {
    // given
    createCaseInstanceByKey("case");
    String humanTask = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    complete(humanTask);

    // then
    assertThat(queryCaseExecutionByActivityId("PI_HumanTask_2").isActive()).isTrue();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/cmm10/Cmmn10CompatibilityTest.testPlanItemExitCriterion.cmmn")
  @Test
  void testPlanItemExitCriterion() {
    // given
    createCaseInstanceByKey("case");

    String humanTask = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    complete(humanTask);

    // then
    assertThat(queryCaseExecutionByActivityId("PI_HumanTask_2")).isNull();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/cmm10/Cmmn10CompatibilityTest.testCasePlanModelExitCriterion.cmmn")
  @Test
  void testCasePlanModelExitCriterion() {
    // given
    String caseInstanceId = createCaseInstanceByKey("case").getId();

    String humanTask = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();

    // when
    complete(humanTask);

    // then
    assertThat(queryCaseExecutionById(caseInstanceId).isTerminated()).isTrue();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/cmm10/Cmmn10CompatibilityTest.testSentryIfPartCondition.cmmn")
  @Test
  void testSentryIfPartCondition() {
    // given
    createCaseInstanceByKey("case", Variables.createVariables().putValue("value", 99));

    String humanTask1 = queryCaseExecutionByActivityId("PI_HumanTask_1").getId();
    String humanTask2 = queryCaseExecutionByActivityId("PI_HumanTask_2").getId();

    assertThat(queryCaseExecutionById(humanTask2).isAvailable()).isTrue();

    // when
    caseService
      .withCaseExecution(humanTask1)
      .setVariable("value", 999)
      .manualStart();

    // then
    assertThat(queryCaseExecutionById(humanTask2).isEnabled()).isTrue();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/cmmn/cmm10/Cmmn10CompatibilityTest.testDescription.cmmn")
  @Test
  void testDescription() {
    // given
    createCaseInstanceByKey("case");

    // when
    var humanTask = queryCaseExecutionByActivityId("PI_HumanTask_1");
    assertThat(humanTask).isNotNull();

    // then
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task.getDescription()).isEqualTo("This is a description!");
  }
}
