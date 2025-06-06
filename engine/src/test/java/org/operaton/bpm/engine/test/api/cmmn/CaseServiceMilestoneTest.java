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
package org.operaton.bpm.engine.test.api.cmmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.exception.NotAllowedException;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseExecutionCommandBuilder;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

/**
 * @author Roman Smirnov
 *
 */
@ExtendWith(ProcessEngineExtension.class)
@ExtendWith(ProcessEngineTestExtension.class)
class CaseServiceMilestoneTest {

  static final String DEFINITION_KEY = "oneMilestoneCase";
  static final String MILESTONE_KEY = "PI_Milestone_1";

  protected TaskService taskService;
  protected CaseService caseService;

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneMilestoneCase.cmmn"})
  @Test
  void testManualStart() {
    // given
    createCaseInstance(DEFINITION_KEY).getId();
    String caseTaskId = queryCaseExecutionByActivityId(MILESTONE_KEY).getId();
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);

    // when
    assertThatThrownBy(commandBuilder::manualStart)
      // then
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneMilestoneCase.cmmn"})
  @Test
  void testDisable() {
    // given
    createCaseInstance(DEFINITION_KEY).getId();
    String caseTaskId = queryCaseExecutionByActivityId(MILESTONE_KEY).getId();
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);

    // when
    assertThatThrownBy(commandBuilder::disable)
      // then
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneMilestoneCase.cmmn"})
  @Test
  void testReenable() {
    // given
    createCaseInstance(DEFINITION_KEY).getId();
    String caseTaskId = queryCaseExecutionByActivityId(MILESTONE_KEY).getId();
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);

    // when
    assertThatThrownBy(commandBuilder::reenable)
      // then
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneMilestoneCase.cmmn"})
  @Test
  void testComplete() {
    // given
    createCaseInstance(DEFINITION_KEY).getId();
    String caseTaskId = queryCaseExecutionByActivityId(MILESTONE_KEY).getId();
    CaseExecutionCommandBuilder commandBuilder = caseService.withCaseExecution(caseTaskId);

    // when
    assertThatThrownBy(commandBuilder::complete)
      // then
      .isInstanceOf(NotAllowedException.class);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneMilestoneCase.cmmn"})
  @Test
  void testTerminate() {
    // given
    createCaseInstance(DEFINITION_KEY).getId();

    String caseTaskId = queryCaseExecutionByActivityId(MILESTONE_KEY).getId();

    caseService
     .withCaseExecution(caseTaskId)
     .terminate();

    CaseExecution caseMilestone = queryCaseExecutionByActivityId(MILESTONE_KEY);
    assertThat(caseMilestone).isNull();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/cmmn/oneMilestoneCase.cmmn"})
  @Test
  void testTerminateNonFluent() {
    // given
    createCaseInstance(DEFINITION_KEY).getId();
    CaseExecution caseMilestone = queryCaseExecutionByActivityId(MILESTONE_KEY);
    assertThat(caseMilestone).isNotNull();

    caseService.terminateCaseExecution(caseMilestone.getId());

    caseMilestone = queryCaseExecutionByActivityId(MILESTONE_KEY);
    assertThat(caseMilestone).isNull();

  }

  protected CaseInstance createCaseInstance(String caseDefinitionKey) {
    return caseService
        .withCaseDefinitionByKey(caseDefinitionKey)
        .create();
  }

  protected CaseExecution queryCaseExecutionByActivityId(String activityId) {
    return caseService
        .createCaseExecutionQuery()
        .activityId(activityId)
        .singleResult();
  }

  protected CaseInstance queryCaseInstanceByKey(String caseDefinitionKey) {
    return caseService
        .createCaseInstanceQuery()
        .caseDefinitionKey(caseDefinitionKey)
        .singleResult();
  }

  protected Task queryTask() {
    return taskService
        .createTaskQuery()
        .singleResult();
  }

}
