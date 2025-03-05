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
package org.operaton.bpm.engine.test.assertions.bpmn;

import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.claim;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.complete;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.taskQuery;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;
import org.junit.Rule;

public class TaskAssertIsAssignedToTest extends ProcessAssertTestCase {

  @Rule
  public ProcessEngineRule processEngineRule = new ProcessEngineRule();

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-isAssignedTo.bpmn"
  })
  void isAssignedToSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-isAssignedTo"
    );
    // When
    claim(taskQuery().singleResult(), "fozzie");
    // Then
    assertThat(processInstance).task().isAssignedTo("fozzie");
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-isAssignedTo.bpmn"
  })
  void isAssignedToNotAssignedFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-isAssignedTo"
    );
    // Then
    expect(() -> assertThat(processInstance).task().isAssignedTo("fozzie"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-isAssignedTo.bpmn"
  })
  void isAssignedToOtherAssigneeFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-isAssignedTo"
    );
    // When
    claim(taskQuery().singleResult(), "fozzie");
    // Then
    expect(() -> assertThat(processInstance).task().isAssignedTo("gonzo"));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-isAssignedTo.bpmn"
  })
  void isAssignedToNullFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-isAssignedTo"
    );
    // When
    claim(taskQuery().singleResult(), "fozzie");
    // Then
    expect(() -> assertThat(processInstance).task().isAssignedTo(null));
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-isAssignedTo.bpmn"
  })
  void isAssignedToNonExistingTaskFailure() {
    // Given
    runtimeService().startProcessInstanceByKey(
      "TaskAssert-isAssignedTo"
    );
    // When
    final Task task = taskQuery().singleResult();
    complete(task);
    // Then
    expect(() -> assertThat(task).isAssignedTo("fozzie"));
  }

}
