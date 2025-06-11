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
package org.operaton.bpm.engine.test.assertions.bpmn;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

import org.junit.jupiter.api.Test;

class ProcessEngineTestsExternalTaskTest extends ProcessAssertTestCase {

  private static final String EXTERNAL_TASK_3 = "ExternalTask_3";
  private static final String EXTERNAL_TASK_2 = "ExternalTask_2";
  private static final String EXTERNAL_TASK_1 = "ExternalTask_1";

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // When
    assertThat(processInstance).isNotNull();
    // Then
    assertThat(externalTask()).isNotNull().hasActivityId(EXTERNAL_TASK_1);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskOnlyActivityFailure() {
    // Given
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // Then
    expect(BpmnAwareTests::externalTask, IllegalStateException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskTwoActivitiesSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // When
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // Then
    assertThat(processInstance).isNotNull();
    // And
    assertThat(externalTask()).isNotNull();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskTwoActivitiesFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    assertThat(processInstance).isNotNull();
    // And
    complete(externalTask());
    // Then
    expect(BpmnAwareTests::externalTask, ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskActivityIdOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // When
    assertThat(processInstance).isNotNull();
    // Then
    assertThat(externalTask(EXTERNAL_TASK_1)).isNotNull().hasActivityId(EXTERNAL_TASK_1);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskActivityIdOnlyActivityFailure() {
    // Given
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // Then
    expect(() -> task(EXTERNAL_TASK_1), IllegalStateException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskActivityIdTwoActivitiesSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // When
    assertThat(processInstance).isNotNull();
    complete(externalTask());
    // Then
    assertThat(externalTask(EXTERNAL_TASK_2)).isNotNull().hasActivityId(EXTERNAL_TASK_2);
    // And
    assertThat(externalTask(EXTERNAL_TASK_3)).isNotNull().hasActivityId(EXTERNAL_TASK_3);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskTaskQueryOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // When
    assertThat(processInstance).isNotNull();
    // Then
    assertThat(externalTask(externalTaskQuery())).isNotNull().hasActivityId(EXTERNAL_TASK_1);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskTaskQueryOnlyActivityFailure() {
    // Given
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // Then
    expect(() -> externalTask(externalTaskQuery()), IllegalStateException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskTaskQueryTwoActivitiesSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // When
    assertThat(processInstance).isNotNull();
    complete(externalTask());
    // Then
    assertThat(externalTask(externalTaskQuery().activityId(EXTERNAL_TASK_2))).isNotNull().hasActivityId(EXTERNAL_TASK_2);
    // And
    assertThat(externalTask(externalTaskQuery().activityId(EXTERNAL_TASK_3))).isNotNull().hasActivityId(EXTERNAL_TASK_3);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskTaskQueryTwoActivitiesFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    assertThat(processInstance).isNotNull();
    // And
    complete(externalTask());
    // When
    expect(() -> externalTask(externalTaskQuery()), ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskProcessInstanceOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // Then
    assertThat(externalTask(processInstance)).isNotNull().hasActivityId(EXTERNAL_TASK_1);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskProcessInstanceTwoActivitiesFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    complete(externalTask(processInstance));
    // When
    expect(() -> externalTask(processInstance), ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskActivityIdProcessInstanceOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // Then
    assertThat(externalTask(EXTERNAL_TASK_1, processInstance)).isNotNull().hasActivityId(EXTERNAL_TASK_1);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskActivityIdProcessInstanceTwoActivitiesSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // When
    complete(externalTask(processInstance));
    // Then
    assertThat(externalTask(EXTERNAL_TASK_2, processInstance)).isNotNull().hasActivityId(EXTERNAL_TASK_2);
    // And
    assertThat(externalTask(EXTERNAL_TASK_3, processInstance)).isNotNull().hasActivityId(EXTERNAL_TASK_3);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskTaskQueryProcessInstanceOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // Then
    assertThat(externalTask(externalTaskQuery().activityId(EXTERNAL_TASK_1), processInstance)).isNotNull().hasActivityId(EXTERNAL_TASK_1);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskTaskQueryProcessInstanceTwoActivitiesSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // When
    complete(externalTask(processInstance));
    // Then
    assertThat(externalTask(externalTaskQuery().activityId(EXTERNAL_TASK_2), processInstance)).isNotNull().hasActivityId(EXTERNAL_TASK_2);
    // And
    assertThat(externalTask(externalTaskQuery().activityId(EXTERNAL_TASK_3), processInstance)).isNotNull().hasActivityId(EXTERNAL_TASK_3);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-externalTask.bpmn"})
  void taskTaskQueryProcessInstanceTwoActivitiesFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    runtimeService().startProcessInstanceByKey("ProcessEngineTests-externalTask");
    // And
    complete(externalTask(processInstance));
    // When
    expect(() -> externalTask(externalTaskQuery(), processInstance), ProcessEngineException.class);
  }

}
