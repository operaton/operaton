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

import org.operaton.bpm.engine.runtime.CaseExecutionCommandBuilder;
import org.operaton.bpm.engine.runtime.CaseInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;

import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.assertThat;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.caseService;

public class HumanTaskAssertVariablesTest extends ProcessAssertTestCase {

  public static final String TASK_A = "PI_TaskA";
  public static final String CASE_KEY = "Case_HumanTaskAssert-variables";

  @Test
  @Deployment(resources = {"cmmn/HumanTaskAssert-variables.cmmn"})
  void variablesSuccess() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When
    setVariablesOnHumanTask();

    // Then
    assertThat(caseInstance).humanTask(TASK_A).variables().containsEntry("aVariable", "aValue");
    // And
    assertThat(caseInstance).humanTask(TASK_A).variables().containsEntry("bVariable", "bValue");
    // And
    assertThat(caseInstance).humanTask(TASK_A).variables().containsKey("aVariable");
    // And
    assertThat(caseInstance).humanTask(TASK_A).variables().containsKey("bVariable");
    // And
    assertThat(caseInstance).humanTask(TASK_A).variables().containsKeys("aVariable", "bVariable");
    // And
    assertThat(caseInstance).humanTask(TASK_A).variables().containsValue("aValue");
    // And
    assertThat(caseInstance).humanTask(TASK_A).variables().doesNotContainValue("cValue");
    // And
    assertThat(caseInstance).humanTask(TASK_A).variables().doesNotContainEntry("cVariable", "cValue");
    // And
    assertThat(caseInstance).humanTask(TASK_A).variables().isNotEmpty();
  }

  @Test
  @Deployment(resources = {"cmmn/HumanTaskAssert-variables.cmmn"})
  void variablesSuccessNoVariables() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When

    // Then
    assertThat(caseInstance).humanTask(TASK_A).variables().isEmpty();
    // And
    assertThat(caseInstance).humanTask(TASK_A).variables().doesNotContainEntry("aVariable", "aValue");
  }

  @Test
  @Deployment(resources = {"cmmn/HumanTaskAssert-variables.cmmn"})
  void variablesFailureHumanTaskCompleted() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When
    setAVariableOnHumanTaskAndCompleteHumanTask();

    // Then
    expect(() -> assertThat(caseInstance).humanTask(TASK_A).variables().containsEntry("aVariable", "aValue"));
  }

  @Test
  @Deployment(resources = {"cmmn/HumanTaskAssert-variables.cmmn"})
  void hasVariablesSuccess() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When
    setVariablesOnHumanTask();

    // Then
    assertThat(caseInstance).humanTask(TASK_A).hasVariables("aVariable", "bVariable");
    // And
    assertThat(caseInstance).humanTask(TASK_A).hasVariables("aVariable");
    // And
    assertThat(caseInstance).humanTask(TASK_A).hasVariables("bVariable");
    // And
    assertThat(caseInstance).humanTask(TASK_A).hasVariables();
  }

  @Test
  @Deployment(resources = {"cmmn/HumanTaskAssert-variables.cmmn"})
  void hasVariablesFailure() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When

    // Then
    expect(() -> assertThat(caseInstance).humanTask(TASK_A).hasVariables());
  }

  @Test
  @Deployment(resources = {"cmmn/HumanTaskAssert-variables.cmmn"})
  void hasNoVariablesSuccess() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When

    // Then
    assertThat(caseInstance).humanTask(TASK_A).hasNoVariables();
  }

  @Test
  @Deployment(resources = {"cmmn/HumanTaskAssert-variables.cmmn"})
  void hasNoVariablesFailure() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When
    setVariablesOnHumanTask();

    // Then
    expect(() -> assertThat(caseInstance).humanTask(TASK_A).hasNoVariables());
  }

  @Test
  @Deployment(resources = {"cmmn/HumanTaskAssert-variables.cmmn"})
  void variablesSuccessNoHumanTaskVariables() {
    // Given
    final CaseInstance caseInstance = createCaseInstanceWithVariable();

    // When

    // Then
    assertThat(caseInstance).humanTask(TASK_A).variables().isEmpty();
  }

  @Test
  @Deployment(resources = {"cmmn/HumanTaskAssert-variables.cmmn"})
  void variablesFailureNoHumanTaskVariables() {
    // Given
    final CaseInstance caseInstance = createCaseInstanceWithVariable();

    // When

    // Then
    expect(() -> assertThat(caseInstance).humanTask(TASK_A).variables().isNotEmpty());
  }

  @Test
  @Deployment(resources = {"cmmn/HumanTaskAssert-variables.cmmn"})
  void hasNoVariablesSuccessNoHumanTaskVariables() {
    // Given
    final CaseInstance caseInstance = createCaseInstanceWithVariable();

    // When

    // Then
    assertThat(caseInstance).humanTask(TASK_A).hasNoVariables();
  }

  @Test
  @Deployment(resources = {"cmmn/HumanTaskAssert-variables.cmmn"})
  void hasVariablesFailureNoHumanTaskVariables() {
    // Given
    final CaseInstance caseInstance = createCaseInstanceWithVariable();

    // When

    // Then
    expect(() -> assertThat(caseInstance).humanTask(TASK_A).hasVariables());
  }

  private CaseInstance createCaseInstance() {
    return caseService().createCaseInstanceByKey(CASE_KEY);
  }

  private CaseInstance createCaseInstanceWithVariable() {
    CaseInstance caseInstance = createCaseInstance();
    caseExecution(caseInstance.getActivityId()).setVariable("aVariable", "aValue");
    return caseInstance;
  }

  private void setAVariableOnHumanTaskAndCompleteHumanTask() {
    caseExecution(TASK_A).setVariable("aVariable", "aValue").complete();
  }

  private void setVariablesOnHumanTask() {
    caseExecution(TASK_A).setVariable("aVariable", "aValue").setVariable("bVariable", "bValue").execute();
  }

  private CaseExecutionCommandBuilder caseExecution(String activityId) {
    return caseService().withCaseExecution(caseService().createCaseExecutionQuery().activityId(activityId).singleResult().getId());
  }

}
