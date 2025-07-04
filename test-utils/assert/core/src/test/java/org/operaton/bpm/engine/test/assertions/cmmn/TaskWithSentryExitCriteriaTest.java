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
package org.operaton.bpm.engine.test.assertions.cmmn;

import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.assertThat;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.caseExecution;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.caseService;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.manuallyStart;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;

public class TaskWithSentryExitCriteriaTest extends ProcessAssertTestCase {

  public static final String TASK_A = "PI_HT_A";
  public static final String TASK_B = "PI_HT_B";

  /**
   * Introduces:
   */
  @Test
  @Deployment(resources = {"cmmn/TaskWithSentryTestExitCriteria.cmmn"})
  void case_is_active_and_task_a_and_task_b_should_be_enabled() {
    // Given
    // case model is deployed
    // When
    CaseInstance caseInstance = givenCaseIsCreated();
    // Then
    assertThat(caseInstance).isActive().humanTask(TASK_A).isEnabled();
    assertThat(caseInstance).isActive().humanTask(TASK_B).isEnabled();
  }

  /**
   * Introduces:
   * task.isTerminated()
   */
  @Test
  @Deployment(resources = {"cmmn/TaskWithSentryTestExitCriteria.cmmn"})
  void case_is_active_and_task_a_should_be_terminated_and_task_b_active() {
    // Given
    CaseInstance caseInstance = givenCaseIsCreatedAndTaskAActive();
    // When
    CaseExecution taskA = caseExecution(TASK_A, caseInstance);
    manuallyStart(caseExecution(TASK_B, caseInstance));
    // Then
    assertThat(taskA).isTerminated();
    assertThat(caseInstance).isActive().humanTask(TASK_B).isActive();
  }

  private CaseInstance givenCaseIsCreated() {
    return caseService().createCaseInstanceByKey("Case_TaskWithSentryExitCriteriaTest");
  }

  private CaseInstance givenCaseIsCreatedAndTaskAActive() {
    CaseInstance caseInstance = givenCaseIsCreated();
    manuallyStart(caseExecution(TASK_A, caseInstance));
    return caseInstance;
  }

}
