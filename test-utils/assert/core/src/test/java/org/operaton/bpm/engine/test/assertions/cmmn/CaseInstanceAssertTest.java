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

import org.operaton.bpm.engine.runtime.CaseExecution;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.*;

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
		CmmnAwareTests.assertThat(taskA).isCompleted();
		// And
		CmmnAwareTests.assertThat(caseInstance).isCompleted();
	}

  @Test
  @Deployment(resources = {"cmmn/TaskTest.cmmn"})
  void returnsHumanTaskAssertForGivenActivityId() {
		// Given
		CaseInstance caseInstance = aStartedCase();
		CaseExecution taskAId = caseService()
				.createCaseExecutionQuery()
				.activityId(TASK_A).singleResult();
		// Then
		CmmnAwareTests.assertThat(caseInstance).isActive();
		// When
		HumanTaskAssert caseTaskAssert = CmmnAwareTests.assertThat(caseInstance).humanTask(TASK_A);
		// Then
		CaseExecution actual = caseTaskAssert.getActual();
		assertThat(actual)
				.overridingErrorMessage(
						"Expected case execution " + toString(actual)
								+ " to be equal to " + toString(taskAId))
				.extracting(CaseExecution::getId)
				.isEqualTo(taskAId.getId());
	}

  @Test
  @Deployment(resources = {"cmmn/TaskTest.cmmn"})
  void isCaseInstance() {
    // Given
    CaseInstance caseInstance = aStartedCase();
    // Then
    CmmnAwareTests.assertThat((CaseExecution) caseInstance).isCaseInstance();
  }

	private CaseInstance aStartedCase() {
		return caseService().createCaseInstanceByKey("Case_TaskTests");
	}

  protected String toString(
    CaseExecution caseExecution) {
    return caseExecution != null ? ("%s {"
      + "id='%s', " + "caseDefinitionId='%s', " + "activityType='%s'"
      + "}").formatted(caseExecution.getClass().getSimpleName(),
      caseExecution.getId(),
      caseExecution.getCaseDefinitionId(),
      caseExecution.getActivityType()) : null;
  }
}
