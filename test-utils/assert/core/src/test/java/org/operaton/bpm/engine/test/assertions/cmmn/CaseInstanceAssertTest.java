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

import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class CaseInstanceAssertTest extends ProcessAssertTestCase {

	public static final String TASK_A = "PI_TaskA";

  @Test
  @Deployment(resources = {"cmmn/TaskTest.cmmn"})
  void returnsCaseTaskAssertForCompletedTasks() {
		// Given
		CaseInstance caseInstance = aStartedCase();
		CaseExecution taskA = caseExecution(TASK_A, caseInstance);
		// When
		caseService().completeCaseExecution(caseExecutionQuery().activityId(TASK_A).singleResult().getId());
    // Then
		assertThat(taskA).isCompleted();
		// And
		assertThat(caseInstance).isCompleted();
	}

  @Test
  @Deployment(resources = {"cmmn/TaskTest.cmmn"})
  void returnsHumanTaskAssertForGivenActivityId() {
		// Given
		CaseInstance caseInstance = aStartedCase();
		CaseExecution pi_taskA = caseService()
				.createCaseExecutionQuery()
				.activityId(TASK_A).singleResult();
		// Then
		assertThat(caseInstance).isActive();
		// When
		HumanTaskAssert caseTaskAssert = assertThat(caseInstance).humanTask(TASK_A);
		// Then
		CaseExecution actual = caseTaskAssert.getActual();
		Assertions
				.assertThat(actual)
				.overridingErrorMessage(
						"Expected case execution " + toString(actual)
								+ " to be equal to " + toString(pi_taskA))
				.isEqualToComparingOnlyGivenFields(pi_taskA, "id");
	}

  @Test
  @Deployment(resources = {"cmmn/TaskTest.cmmn"})
  void isCaseInstance() {
    // Given
    CaseInstance caseInstance = aStartedCase();
    // Then
    assertThat((CaseExecution) caseInstance).isCaseInstance();
  }

	private CaseInstance aStartedCase() {
		return caseService().createCaseInstanceByKey("Case_TaskTests");
	}

  protected String toString(
    CaseExecution caseExecution) {
    return caseExecution != null ? String.format("%s {"
        + "id='%s', " + "caseDefinitionId='%s', " + "activityType='%s'"
        + "}", caseExecution.getClass().getSimpleName(),
      caseExecution.getId(),
      caseExecution.getCaseDefinitionId(),
      caseExecution.getActivityType()) : null;
  }
}
