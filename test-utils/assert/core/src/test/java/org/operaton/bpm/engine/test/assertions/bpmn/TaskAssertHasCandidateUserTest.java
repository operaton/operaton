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

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;

import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

class TaskAssertHasCandidateUserTest extends ProcessAssertTestCase {

  private static final String CANDIDATE_USER = "candidateUser";
  private static final String ASSIGNEE = "assignee";

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateUser.bpmn"
  })
  void hasCandidateUserPreDefinedSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateUser"
    );
    // Then
    assertThat(processInstance).task().hasCandidateUser(CANDIDATE_USER);
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateUser.bpmn"
  })
  void hasCandidateUserPreDefinedFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateUser"
    );
    // When
    complete(taskQuery().singleResult());
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateUser(CANDIDATE_USER));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateUser.bpmn"
  })
  void hasCandidateUserPredefinedRemovedFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateUser"
    );
    // When
    taskService().deleteCandidateUser(taskQuery().singleResult().getId(), CANDIDATE_USER);
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateUser(CANDIDATE_USER));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateUser.bpmn"
  })
  void hasCandidateUserPreDefinedOtherFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateUser"
    );
    // When
    taskService().deleteCandidateUser(taskQuery().singleResult().getId(), CANDIDATE_USER);
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateUser("otherCandidateUser"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateUser.bpmn"
  })
  void hasCandidateUserExplicitlySetSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateUser"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    taskService().addCandidateUser(taskQuery().singleResult().getId(), "explicitCandidateUserId");
    // Then
    assertThat(processInstance).task().hasCandidateUser("explicitCandidateUserId");
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateUser.bpmn"
  })
  void hasCandidateUserExplicitlySetRemovedFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateUser"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    taskService().addCandidateUser(taskQuery().singleResult().getId(), "explicitCandidateUserId");
    // When
    taskService().deleteCandidateUser(taskQuery().singleResult().getId(), "explicitCandidateUserId");
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateUser("explicitCandidateUserId"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateUser.bpmn"
  })
  void hasCandidateUserExplicitlySetOtherFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateUser"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    taskService().addCandidateUser(taskQuery().singleResult().getId(), "explicitCandidateUserId");
    // When
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateUser("otherCandidateUser"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateUser.bpmn"
  })
  void hasCandidateUserMoreThanOneSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateUser"
    );
    // When
    taskService().addCandidateUser(taskQuery().singleResult().getId(), "explicitCandidateUserId");
    // Then
    assertThat(processInstance).task().hasCandidateUser(CANDIDATE_USER);
    // And
    assertThat(processInstance).task().hasCandidateUser("explicitCandidateUserId");
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateUser.bpmn"
  })
  void hasCandidateUserMoreThanOneFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateUser"
    );
    // When
    taskService().addCandidateUser(taskQuery().singleResult().getId(), "explicitCandidateUserId");
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateUser("otherCandidateUser"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateUser.bpmn"
  })
  void hasCandidateUserNullFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateUser"
    );
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateUser(null));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateUser.bpmn"
  })
  void hasCandidateUserNonExistingTaskFailure() {
    // Given
    runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateUser"
    );
    // When
    final Task task = taskQuery().singleResult();
    complete(task);
    // Then
    expect(() -> assertThat(task).hasCandidateUser(CANDIDATE_USER));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateUser.bpmn"
  })
  void hasCandidateUserAssignedFailure() {
    // Given
    runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateUser"
    );
    // When
    final Task task = taskQuery().singleResult();
    taskService().setAssignee(task.getId(), ASSIGNEE);
    // Then
    expect(() -> assertThat(task).hasCandidateUser(CANDIDATE_USER));
  }

}
