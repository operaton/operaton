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
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;

import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.complete;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.processInstanceQuery;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.task;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.assertThat;
import static org.operaton.bpm.engine.test.assertions.cmmn.CmmnAwareTests.caseService;

public class ProcessTaskAssertVariablesTest extends ProcessAssertTestCase {

  public static final String TASK_A = "PI_TaskA";
  public static final String USER_TASK = "UserTask_1";
  public static final String CASE_KEY = "Case_ProcessTaskAssert-variables";

  @Test
  @Deployment(resources = {"cmmn/ProcessTaskAssert-variables.cmmn", "cmmn/ProcessTaskAssert-calledProcess.bpmn"})
  void variablesSuccess() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When
    setVariablesOnProcessTask();

    // Then
    assertThat(caseInstance).processTask(TASK_A).variables().containsEntry("aVariable", "aValue");
    // And
    assertThat(caseInstance).processTask(TASK_A).variables().containsEntry("bVariable", "bValue");
    // And
    assertThat(caseInstance).processTask(TASK_A).variables().containsKey("aVariable");
    // And
    assertThat(caseInstance).processTask(TASK_A).variables().containsKey("bVariable");
    // And
    assertThat(caseInstance).processTask(TASK_A).variables().containsKeys("aVariable", "bVariable");
    // And
    assertThat(caseInstance).processTask(TASK_A).variables().containsValue("aValue");
    // And
    assertThat(caseInstance).processTask(TASK_A).variables().doesNotContainValue("cValue");
    // And
    assertThat(caseInstance).processTask(TASK_A).variables().doesNotContainEntry("cVariable", "cValue");
    // And
    assertThat(caseInstance).processTask(TASK_A).variables().isNotEmpty();
  }

  @Test
  @Deployment(resources = {"cmmn/ProcessTaskAssert-variables.cmmn", "cmmn/ProcessTaskAssert-calledProcess.bpmn"})
  void variablesSuccessNoVariables() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When

    // Then
    assertThat(caseInstance).processTask(TASK_A).variables().isEmpty();
    // And
    assertThat(caseInstance).processTask(TASK_A).variables().doesNotContainEntry("aVariable", "aValue");
  }

  @Test
  @Deployment(resources = {"cmmn/ProcessTaskAssert-variables.cmmn", "cmmn/ProcessTaskAssert-calledProcess.bpmn"})
  void variablesFailureProcessTaskCompleted() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When
    setAVariableOnProcessTaskAndCompleteProcessTask(caseInstance);

    // Then
    expect(() -> assertThat(caseInstance).processTask(TASK_A).variables().containsEntry("aVariable", "aValue"));
  }

  @Test
  @Deployment(resources = {"cmmn/ProcessTaskAssert-variables.cmmn", "cmmn/ProcessTaskAssert-calledProcess.bpmn"})
  void hasVariablesSuccess() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When
    setVariablesOnProcessTask();

    // Then
    assertThat(caseInstance).processTask(TASK_A).hasVariables("aVariable", "bVariable");
    // And
    assertThat(caseInstance).processTask(TASK_A).hasVariables("aVariable");
    // And
    assertThat(caseInstance).processTask(TASK_A).hasVariables("bVariable");
    // And
    assertThat(caseInstance).processTask(TASK_A).hasVariables();
  }

  @Test
  @Deployment(resources = {"cmmn/ProcessTaskAssert-variables.cmmn", "cmmn/ProcessTaskAssert-calledProcess.bpmn"})
  void hasVariablesFailure() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When

    // Then
    expect(() -> assertThat(caseInstance).processTask(TASK_A).hasVariables());
  }

  @Test
  @Deployment(resources = {"cmmn/ProcessTaskAssert-variables.cmmn", "cmmn/ProcessTaskAssert-calledProcess.bpmn"})
  void hasNoVariablesSuccess() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When

    // Then
    assertThat(caseInstance).processTask(TASK_A).hasNoVariables();
  }

  @Test
  @Deployment(resources = {"cmmn/ProcessTaskAssert-variables.cmmn", "cmmn/ProcessTaskAssert-calledProcess.bpmn"})
  void hasNoVariablesFailure() {
    // Given
    final CaseInstance caseInstance = createCaseInstance();

    // When
    setVariablesOnProcessTask();

    // Then
    expect(() -> assertThat(caseInstance).processTask(TASK_A).hasNoVariables());
  }

  @Test
  @Deployment(resources = {"cmmn/ProcessTaskAssert-variables.cmmn", "cmmn/ProcessTaskAssert-calledProcess.bpmn"})
  void variablesSuccessNoProcessTaskVariables() {
    // Given
    final CaseInstance caseInstance = createCaseInstanceWithVariable();

    // When

    // Then
    assertThat(caseInstance).processTask(TASK_A).variables().isEmpty();
  }

  @Test
  @Deployment(resources = {"cmmn/ProcessTaskAssert-variables.cmmn", "cmmn/ProcessTaskAssert-calledProcess.bpmn"})
  void variablesFailureNoProcessTaskVariables() {
    // Given
    final CaseInstance caseInstance = createCaseInstanceWithVariable();

    // When

    // Then
    expect(() -> assertThat(caseInstance).processTask(TASK_A).variables().isNotEmpty());
  }

  @Test
  @Deployment(resources = {"cmmn/ProcessTaskAssert-variables.cmmn", "cmmn/ProcessTaskAssert-calledProcess.bpmn"})
  void hasNoVariablesSuccessNoProcessTaskVariables() {
    // Given
    final CaseInstance caseInstance = createCaseInstanceWithVariable();

    // When

    // Then
    assertThat(caseInstance).processTask(TASK_A).hasNoVariables();
  }

  @Test
  @Deployment(resources = {"cmmn/ProcessTaskAssert-variables.cmmn", "cmmn/ProcessTaskAssert-calledProcess.bpmn"})
  void hasVariablesFailureNoProcessTaskVariables() {
    // Given
    final CaseInstance caseInstance = createCaseInstanceWithVariable();

    // When

    // Then
    expect(() -> assertThat(caseInstance).processTask(TASK_A).hasVariables());
  }

  private ProcessInstance calledProcessInstance(CaseInstance caseInstance) {
    return processInstanceQuery().superCaseInstanceId(caseInstance.getId()).singleResult();
  }

  private CaseInstance createCaseInstance() {
    return caseService().createCaseInstanceByKey(CASE_KEY);
  }

  private CaseInstance createCaseInstanceWithVariable() {
    CaseInstance caseInstance = createCaseInstance();
    caseExecution(caseInstance.getActivityId()).setVariable("aVariable", "aValue");
    return caseInstance;
  }

  private void setAVariableOnProcessTaskAndCompleteProcessTask(CaseInstance caseInstance) {
    caseExecution(TASK_A).setVariable("aVariable", "aValue");
    complete(task(USER_TASK, calledProcessInstance(caseInstance)));
  }

  private void setVariablesOnProcessTask() {
    caseExecution(TASK_A).setVariable("aVariable", "aValue").setVariable("bVariable", "bValue").execute();
  }

  private CaseExecutionCommandBuilder caseExecution(String activityId) {
    return caseService().withCaseExecution(caseService().createCaseExecutionQuery().activityId(activityId).singleResult().getId());
  }

}
