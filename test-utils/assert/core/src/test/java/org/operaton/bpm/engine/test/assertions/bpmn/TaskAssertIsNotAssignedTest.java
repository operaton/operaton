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

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

import org.junit.jupiter.api.Test;

class TaskAssertIsNotAssignedTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-isNotAssigned.bpmn"
  })
  void isNotAssignedSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-isNotAssigned"
    );
    // Then
    assertThat(processInstance).task().isNotAssigned();
  }

  @Test
  @Deployment(resources = {"bpmn/TaskAssert-isNotAssigned.bpmn"
  })
  void isNotAssignedFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "TaskAssert-isNotAssigned"
    );
    // When
    claim(taskQuery().singleResult(), "fozzie");
    // Then
    expect(() -> assertThat(processInstance).task().isNotAssigned());
  }

}
