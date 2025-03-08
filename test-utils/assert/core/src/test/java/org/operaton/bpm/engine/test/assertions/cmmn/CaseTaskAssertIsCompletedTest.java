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

import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.Failure;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.*;

import org.junit.jupiter.api.Test;

public class CaseTaskAssertIsCompletedTest extends ProcessAssertTestCase {

  public static final String TASK_A = "PI_TaskA";
  public static final String TASK_B = "PI_TaskB";
  public static final String CASE_KEY = "Case_CaseTaskAssertIsCompletedTest";
  public static final String CASE_KEY_B = "Case_CaseTaskAssertIsCompletedTest_CaseB";

  @Test
  @Deployment(resources = {"cmmn/CaseTaskAssertIsCompletedTest.cmmn"})
  void isCompletedSuccess() {
    // Given
    CaseInstance caseInstance = givenCaseIsCreated();
    CaseInstance caseInstanceB = caseService().createCaseInstanceQuery().caseDefinitionKey(CASE_KEY_B).singleResult();
    // When
    CaseTaskAssert caseTask = assertThat(caseInstance).caseTask(TASK_A);
    complete(caseExecution(TASK_B, caseInstanceB));
    // Then
    caseTask.isCompleted();
  }

  @Test
  @Deployment(resources = {"cmmn/CaseTaskAssertIsCompletedTest.cmmn"})
  void isCompletedFailure() {
    // Given
    final CaseInstance caseInstance = givenCaseIsCreated();
    // When
    // Then
    expect(new Failure() {
      @Override
      public void when() {
        assertThat(caseInstance).caseTask(TASK_A).isCompleted();
      }
    });
  }

  private CaseInstance givenCaseIsCreated() {
    return caseService().createCaseInstanceByKey(CASE_KEY);
  }
}