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

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.Failure;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;

import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.processInstanceQuery;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.task;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.assertThat;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.caseService;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.complete;

public class ProcessTaskAssertIsActiveTest extends ProcessAssertTestCase {

  public static final String TASK_A = "PI_TaskA";
  public static final String USER_TASK = "UserTask_1";
  public static final String CASE_KEY = "Case_ProcessTaskAssertIsActiveTest";

  @Test
  @Deployment(resources = {"cmmn/ProcessTaskAssertIsActiveTest.cmmn", "cmmn/ProcessTaskAssert-calledProcess.bpmn"})
  void isActiveSuccess() {
    // Given
    // case model is deployed
    // When
    CaseInstance caseInstance = givenCaseIsCreated();
    // Then
    assertThat(caseInstance).processTask(TASK_A).isActive();
  }

  @Test
  @Deployment(resources = {"cmmn/ProcessTaskAssertIsActiveTest.cmmn", "cmmn/ProcessTaskAssert-calledProcess.bpmn"})
  void isActiveFailure() {
    // Given
    final CaseInstance caseInstance = givenCaseIsCreated();
    // When
    complete(task(USER_TASK, calledProcessInstance(caseInstance)));
    // Then
    expect(new Failure() {
      @Override
      public void when() {
        assertThat(caseInstance).processTask(TASK_A).isActive();
      }
    });
  }

  private ProcessInstance calledProcessInstance(CaseInstance caseInstance) {
    return processInstanceQuery().superCaseInstanceId(caseInstance.getId()).singleResult();
  }

  private CaseInstance givenCaseIsCreated() {
    return caseService().createCaseInstanceByKey(CASE_KEY);
  }
}
