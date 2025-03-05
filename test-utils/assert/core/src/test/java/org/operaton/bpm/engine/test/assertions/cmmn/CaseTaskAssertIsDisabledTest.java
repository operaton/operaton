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
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.disable;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.assertions.helpers.Failure;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;
import org.junit.Rule;

public class CaseTaskAssertIsDisabledTest extends ProcessAssertTestCase {

  public static final String TASK_A = "PI_TaskA";
  public static final String TASK_B = "PI_TaskB";
  public static final String CASE_KEY = "Case_CaseTaskAssertIsDisabledTest";
  public static final String CASE_KEY_B = "Case_CaseTaskAssertIsDisabledTest_CaseB";

  @Rule
  public ProcessEngineRule processEngineRule = new ProcessEngineRule();

  @Test
  @Deployment(resources = {"cmmn/CaseTaskAssertIsDisabledTest.cmmn"})
  void isDisabledSuccess() {
    // Given
    final CaseInstance caseInstance = givenCaseIsCreated();
    // When
    CaseTaskAssert caseTask = assertThat(caseInstance).caseTask(TASK_A);
    disable(caseExecution(TASK_A, caseInstance));
    // Then
    caseTask.isDisabled();
  }

  @Test
  @Deployment(resources = {"cmmn/CaseTaskAssertIsDisabledTest.cmmn"})
  void isDisabledFailure() {
    // Given
    final CaseInstance caseInstance = givenCaseIsCreated();
    // When
    // Then
    expect(new Failure() {
      @Override
      public void when() {
        assertThat(caseInstance).caseTask(TASK_A).isDisabled();
      }
    });
  }

  private CaseInstance givenCaseIsCreated() {
    return caseService().createCaseInstanceByKey(CASE_KEY);
  }
}