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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.history.HistoricExternalTaskLog;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;

import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessEngineTestsCompleteExternalTaskTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeTaskOnlySuccess() {
    // Given
    ProcessInstance processInstance = getProcessInstanceStarted();
    assertThat(processInstance).hasNotPassed("ExternalTask_1");
    // When
    complete(externalTask(processInstance));
    // Then
    assertThat(processInstance).hasPassed("ExternalTask_1");
    assertThat(processInstance).isEnded();
    // And
    assertThat(getExternalTaskLogEntry().getWorkerId()).isEqualTo(DEFAULT_WORKER_EXTERNAL_TASK);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeTaskOnlyFailure() {
    // Given
    final ProcessInstance processInstance = getProcessInstanceStarted();
    // And
    final ExternalTask task = externalTask(processInstance);
    // When
    complete(task);
    // Then
    expect(() -> complete(task), NotFoundException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeNullTaskOnlyFailure() {
    // Given
    getProcessInstanceStarted();
    // Then
    expect(() -> complete((ExternalTask) null), IllegalArgumentException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeTaskOnlyMultiplePerTopicFailure() {
    // Given
    final ProcessInstance processInstance = getProcessInstanceStarted();
    getProcessInstanceStarted();
    // And
    final ExternalTask task = externalTask(processInstance);
    // Then
    expect(() -> {
      // one of the two complete-calls will hit the external task of the other process instance
      complete(task);
      complete(task);
    }, IllegalStateException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeWithVariablesSuccess() {
    // Given
    ProcessInstance processInstance = getProcessInstanceStarted();
    // When
    complete(externalTask(processInstance), withVariables("a", "b"));
    // Then
    assertThat(processInstance).isEnded().variables().containsEntry("a", "b");
    // And
    assertThat(getExternalTaskLogEntry().getWorkerId()).isEqualTo(DEFAULT_WORKER_EXTERNAL_TASK);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeWithVariablesFailure() {
    // Given
    final ProcessInstance processInstance = getProcessInstanceStarted();
    // And
    final ExternalTask task = externalTask(processInstance);
    // When
    complete(task, withVariables("a", "b"));
    // Then
    expect(() -> complete(task, withVariables("a", "b")), NotFoundException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeWithVariablesNullTaskFailure() {
    // Given
    getProcessInstanceStarted();
    // Then
    expect(() -> complete((ExternalTask) null, withVariables("a", "b")), IllegalArgumentException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeWithNullVariablesFailure() {
    // Given
    final ProcessInstance processInstance = getProcessInstanceStarted();
    // And
    final ExternalTask task = externalTask(processInstance);
    // Then
    expect(() -> complete(task, null), IllegalArgumentException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeLockedTaskSuccess() {
    // Given
    ProcessInstance processInstance = getProcessInstanceStarted();
    assertThat(processInstance).hasNotPassed("ExternalTask_1");
    ExternalTask task = externalTask();
    // When
    List<LockedExternalTask> lockedTasks = fetchAndLock(task.getTopicName(), DEFAULT_WORKER_EXTERNAL_TASK, 1);
    assertThat(lockedTasks).hasSize(1);
    complete(lockedTasks.get(0));
    // Then
    assertThat(processInstance).hasPassed("ExternalTask_1");
    assertThat(processInstance).isEnded();
    // And
    assertThat(getExternalTaskLogEntry().getWorkerId()).isEqualTo(DEFAULT_WORKER_EXTERNAL_TASK);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeLockedTaskFailure() {
    // Given
    final ProcessInstance processInstance = getProcessInstanceStarted();
    // And
    ExternalTask task = externalTask(processInstance);
    // When
    final List<LockedExternalTask> lockedTasks = fetchAndLock(task.getTopicName(), DEFAULT_WORKER_EXTERNAL_TASK, 1);
    assertThat(lockedTasks).hasSize(1);
    complete(lockedTasks.get(0));
    // Then
    expect(() -> complete(lockedTasks.get(0)), NotFoundException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeNullLockedTaskFailure() {
    // Given
    getProcessInstanceStarted();
    // Then
    expect(() -> complete((LockedExternalTask) null), IllegalArgumentException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeLockedTaskWithVariablesSuccess() {
    // Given
    ProcessInstance processInstance = getProcessInstanceStarted();
    assertThat(processInstance).hasNotPassed("ExternalTask_1");
    ExternalTask task = externalTask();
    // When
    List<LockedExternalTask> lockedTasks = fetchAndLock(task.getTopicName(), DEFAULT_WORKER_EXTERNAL_TASK, 1);
    assertThat(lockedTasks).hasSize(1);
    complete(lockedTasks.get(0), withVariables("a", "b"));
    // Then
    assertThat(processInstance).hasPassed("ExternalTask_1");
    assertThat(processInstance).isEnded().variables().containsEntry("a", "b");
    // And
    assertThat(getExternalTaskLogEntry().getWorkerId()).isEqualTo(DEFAULT_WORKER_EXTERNAL_TASK);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeLockedTaskWithVariablesFailure() {
    // Given
    final ProcessInstance processInstance = getProcessInstanceStarted();
    // And
    ExternalTask task = externalTask(processInstance);
    // When
    final List<LockedExternalTask> lockedTasks = fetchAndLock(task.getTopicName(), DEFAULT_WORKER_EXTERNAL_TASK, 1);
    assertThat(lockedTasks).hasSize(1);
    complete(lockedTasks.get(0), withVariables("a", "b"));
    // Then
    expect(() -> complete(lockedTasks.get(0), withVariables("a", "b")), NotFoundException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeNullLockedTaskWithVariablesFailure() {
    // Given
    getProcessInstanceStarted();
    // Then
    expect(() -> complete((LockedExternalTask) null, withVariables("a", "b")), IllegalArgumentException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-completeExternalTask.bpmn"})
  void completeLockedTaskWithNullVariablesFailure() {
    // Given
    ProcessInstance processInstance = getProcessInstanceStarted();
    // And
    ExternalTask task = externalTask(processInstance);
    // When
    final List<LockedExternalTask> lockedTasks = fetchAndLock(task.getTopicName(), DEFAULT_WORKER_EXTERNAL_TASK, 1);
    assertThat(lockedTasks).hasSize(1);
    // Then
    expect(() -> complete(lockedTasks.get(0), null), IllegalArgumentException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ExternalTaskAssert-localVariables.bpmn"})
  void completeLockedTaskWithLocalVariablesSuccess() {
    // Given
    ProcessInstance pi = runtimeService().startProcessInstanceByKey("ExternalTaskAssert-localVariables");

    // Assume
    assertThat(externalTaskQuery().singleResult()).isNotNull();
    assertThat(externalTaskQuery().singleResult()).hasTopicName("External_1");

    LockedExternalTask task = fetchAndLock("External_1", "worker1", 1).get(0);
    assertThat(task.getActivityId()).isEqualTo("ExternalTask_1");

    // When
    complete(
      task,
      Collections.emptyMap(),
      withVariables(
        "local_variable_1", "value_1"));

    // Then
    assertThat(externalTaskQuery().singleResult()).isNotNull();
    assertThat(externalTaskQuery().singleResult()).hasTopicName("Noop");
    assertThat(pi).variables().containsKey("variable_1");
  }

  @Test
  @Deployment(resources = {"bpmn/ExternalTaskAssert-localVariables.bpmn"})
  void completeLockedTaskWithLocalVariablesFailure() {
    // Given
    runtimeService().startProcessInstanceByKey("ExternalTaskAssert-localVariables");

    // Assume
    assertThat(externalTaskQuery().singleResult()).isNotNull();

    // When
    LockedExternalTask task = fetchAndLock("External_1", "worker1", 1).get(0);
    assertThat(task.getActivityId()).isEqualTo("ExternalTask_1");

    // Then
    expect(()->complete(task), ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ExternalTaskAssert-localVariables.bpmn"})
  void completeTaskWithLocalVariablesSuccess() {
    // Given
    ProcessInstance pi = runtimeService().startProcessInstanceByKey("ExternalTaskAssert-localVariables");

    // Assume
    assertThat(externalTaskQuery().singleResult()).isNotNull();
    assertThat(externalTaskQuery().singleResult()).hasTopicName("External_1");

    // When
    complete(
      externalTaskQuery().singleResult(),
        Collections.emptyMap(),
        withVariables(
          "local_variable_1", "value_1"));

    // Then
    assertThat(externalTaskQuery().singleResult()).isNotNull();
    assertThat(externalTaskQuery().singleResult()).hasTopicName("Noop");
    assertThat(pi).variables().containsKey("variable_1");
  }

  @Test
  @Deployment(resources = {"bpmn/ExternalTaskAssert-localVariables.bpmn"})
  void completeTaskWithoutLocalVariablesFailure() {
    // Given
    runtimeService().startProcessInstanceByKey("ExternalTaskAssert-localVariables");

    // Assume
    assertThat(externalTaskQuery().singleResult()).isNotNull();

    // When & Then
    expect(()->complete(externalTaskQuery().singleResult()), ProcessEngineException.class);
  }

  private ProcessInstance getProcessInstanceStarted() {
    return runtimeService().startProcessInstanceByKey("ProcessEngineTests-completeExternalTask");
  }

  private HistoricExternalTaskLog getExternalTaskLogEntry() {
    return historyService().createHistoricExternalTaskLogQuery().successLog().singleResult();
  }
}
