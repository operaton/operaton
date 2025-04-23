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
package org.operaton.bpm.engine.test.cmmn.milestone;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.cmmn.CmmnTest;
import org.junit.jupiter.api.Test;

/**
 * @author Roman Smirnov
 *
 */
class MilestoneTest extends CmmnTest {

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/milestone/MilestoneTest.testWithoutEntryCriterias.cmmn"})
  @Test
  void testWithoutEntryCriterias() {
    // given

    // when
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    // then
    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .singleResult();

    assertThat(caseInstance.isCompleted()).isTrue();

    Object occurVariable = caseService.getVariable(caseInstanceId, "occur");
    assertThat(occurVariable).isNotNull();
    assertThat((Boolean) occurVariable).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/milestone/MilestoneTest.testWithEntryCriteria.cmmn"})
  @Test
  void testWithEntryCriteria() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    CaseExecution milestone = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1")
        .singleResult();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_1")
        .singleResult()
        .getId();

    assertThat(milestone.isAvailable()).isTrue();

    // then
    assertThat(caseService.getVariable(caseInstanceId, "occur")).isNull();

    milestone = caseService
        .createCaseExecutionQuery()
        .available()
        .singleResult();

    assertThat(milestone.isAvailable()).isTrue();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // then
    Object occurVariable = caseService.getVariable(caseInstanceId, "occur");
    assertThat(occurVariable).isNotNull();
    assertThat((Boolean) occurVariable).isTrue();

    milestone = caseService
        .createCaseExecutionQuery()
        .available()
        .singleResult();

    assertThat(milestone).isNull();

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .singleResult();

    assertThat(caseInstance.isCompleted()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/milestone/MilestoneTest.testWithMultipleEntryCriterias.cmmn"})
  @Test
  void testWithMultipleEntryCriterias() {
    // given
    String caseInstanceId = caseService
        .withCaseDefinitionByKey("case")
        .create()
        .getId();

    CaseExecution milestone = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1")
        .singleResult();

    String humanTaskId = caseService
        .createCaseExecutionQuery()
        .activityId("PI_HumanTask_2")
        .singleResult()
        .getId();

    assertThat(milestone.isAvailable()).isTrue();

    // then
    assertThat(caseService.getVariable(caseInstanceId, "occur")).isNull();

    milestone = caseService
        .createCaseExecutionQuery()
        .available()
        .singleResult();

    assertThat(milestone.isAvailable()).isTrue();

    // when
    caseService
      .withCaseExecution(humanTaskId)
      .complete();

    // then
    Object occurVariable = caseService.getVariable(caseInstanceId, "occur");
    assertThat(occurVariable).isNotNull();
    assertThat((Boolean) occurVariable).isTrue();

    milestone = caseService
        .createCaseExecutionQuery()
        .available()
        .singleResult();

    assertThat(milestone).isNull();

    CaseInstance caseInstance = caseService
        .createCaseInstanceQuery()
        .caseInstanceId(caseInstanceId)
        .singleResult();

    assertThat(caseInstance.isActive()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/cmmn/milestone/MilestoneTest.testWithEntryCriteria.cmmn"})
  @Test
  void testActivityType() {
    // given
    caseService
      .withCaseDefinitionByKey("case")
      .create()
      .getId();

    // when
    CaseExecution milestone = caseService
        .createCaseExecutionQuery()
        .activityId("PI_Milestone_1")
        .singleResult();

    // then
    assertThat(milestone.getActivityType()).isEqualTo("milestone");
  }

}
