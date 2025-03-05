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
package org.operaton.bpm.engine.test.assertions.cmmn;

import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.assertThat;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.caseExecution;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.caseService;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.complete;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;

public class TaskTest extends ProcessAssertTestCase {

  public static final String TASK_A = "PI_TaskA";
  public static final String CASE_KEY = "Case_TaskTests";

  /**
   * Introduces:
   * assertThat(CaseInstance)
   * caseInstance.isActive()
   * caseInstance.activity(id)
   * task.isActive()
   */
  @Test
  @Deployment(resources = {"cmmn/TaskTest.cmmn"})
  void case_and_task_should_be_active() {
    // Given
    // case model is deployed
    // When
    CaseInstance caseInstance = caseService().createCaseInstanceByKey(CASE_KEY);
    // Then
    assertThat(caseInstance).isActive().humanTask(TASK_A).isActive();
  }

  /**
   * Introduces:
   * caseExecution(id, caseInstance)
   * complete(caseExecution)
   * caseInstance.isCompleted()
   * task.isCompleted()
   */
  @Test
  @Deployment(resources = {"cmmn/TaskTest.cmmn"})
  void case_should_complete_when_task_is_completed() {
    // Given
    CaseInstance caseInstance = givenCaseIsCreated();
    // When
    CaseExecution taskA;
    complete(taskA = caseExecution(TASK_A, caseInstance));
    // Then
    assertThat(caseInstance).isCompleted();
    assertThat(taskA).isCompleted();
  }

  private CaseInstance givenCaseIsCreated() {
    return caseService().createCaseInstanceByKey(CASE_KEY);
  }

}
