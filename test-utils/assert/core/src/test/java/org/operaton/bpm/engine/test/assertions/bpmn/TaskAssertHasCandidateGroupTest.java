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

class TaskAssertHasCandidateGroupTest extends ProcessAssertTestCase {

  private static final String CANDIDATE_GROUP = "candidateGroup";
  private static final String ASSIGNEE = "assignee";

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroup.bpmn"
  })
  void hasCandidateGroupPreDefinedSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroup"
    );
    // Then
    assertThat(processInstance).task().hasCandidateGroup(CANDIDATE_GROUP);
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroup.bpmn"
  })
  void hasCandidateGroupPreDefinedFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroup"
    );
    // When
    complete(taskQuery().singleResult());
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroup(CANDIDATE_GROUP));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroup.bpmn"
  })
  void hasCandidateGroupPredefinedRemovedFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroup"
    );
    // When
    taskService().deleteCandidateGroup(taskQuery().singleResult().getId(), CANDIDATE_GROUP);
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroup(CANDIDATE_GROUP));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroup.bpmn"
  })
  void hasCandidateGroupPreDefinedOtherFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroup"
    );
    // When
    taskService().deleteCandidateGroup(taskQuery().singleResult().getId(), CANDIDATE_GROUP);
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroup("otherCandidateGroup"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroup.bpmn"
  })
  void hasCandidateGroupExplicitlySetSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroup"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
    // Then
    assertThat(processInstance).task().hasCandidateGroup("explicitCandidateGroupId");
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroup.bpmn"
  })
  void hasCandidateGroupExplicitlySetRemovedFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroup"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
    // When
    taskService().deleteCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroup("explicitCandidateGroupId"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroup.bpmn"
  })
  void hasCandidateGroupExplicitlySetOtherFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroup"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
    // When
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroup("otherCandidateGroup"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroup.bpmn"
  })
  void hasCandidateGroupMoreThanOneSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroup"
    );
    // When
    taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
    // Then
    assertThat(processInstance).task().hasCandidateGroup(CANDIDATE_GROUP);
    // And
    assertThat(processInstance).task().hasCandidateGroup("explicitCandidateGroupId");
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroup.bpmn"
  })
  void hasCandidateGroupMoreThanOneFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroup"
    );
    // When
    taskService().addCandidateGroup(taskQuery().singleResult().getId(), "explicitCandidateGroupId");
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroup("otherCandidateGroup"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroup.bpmn"
  })
  void hasCandidateGroupNullFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroup"
    );
    // Then
    expect(() -> assertThat(processInstance).task().hasCandidateGroup(null));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroup.bpmn"
  })
  void hasCandidateGroupNonExistingTaskFailure() {
    // Given
    runtimeService().startProcessInstanceByKey(
      "TaskAssert-hasCandidateGroup"
    );
    // When
    final Task task = taskQuery().singleResult();
    complete(task);
    // Then
    expect(() -> assertThat(task).hasCandidateGroup(CANDIDATE_GROUP));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-hasCandidateGroup.bpmn"
  })
  void hasCandidateGroupAssignedFailure() {
    // Given
    final ProcessInstance pi = runtimeService().startProcessInstanceByKey(
        "TaskAssert-hasCandidateGroup"
    );
    // When
    claim(task(pi), ASSIGNEE);
    // Then
    expect(() -> assertThat(task(pi)).hasCandidateGroup(CANDIDATE_GROUP));
  }

}
