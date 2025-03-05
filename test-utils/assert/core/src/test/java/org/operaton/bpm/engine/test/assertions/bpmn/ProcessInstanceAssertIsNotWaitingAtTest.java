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

import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

import org.junit.jupiter.api.Test;

public class ProcessInstanceAssertIsNotWaitingAtTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isNotWaitingAt.bpmn"
  })
  void isNotWaitingAtOnlyActivitySuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isNotWaitingAt"
    );
    // Then
    assertThat(processInstance).isNotWaitingAt("UserTask_2");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isNotWaitingAt.bpmn"
  })
  void isNotWaitingAtOnlyActivityFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isNotWaitingAt"
    );
    // Then
    expect(() -> assertThat(processInstance).isNotWaitingAt("UserTask_1"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isNotWaitingAt.bpmn"
  })
  void isNotWaitingAtNonExistingActivitySuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isNotWaitingAt"
    );
    // Then
    assertThat(processInstance).isNotWaitingAt("NonExistingUserTask");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isNotWaitingAt.bpmn"
  })
  void isNotWaitingAtOneOfTwoActivitiesSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isNotWaitingAt"
    );
    // When
    complete(taskQuery().singleResult());
    // Then
    assertThat(processInstance).isNotWaitingAt("UserTask_1");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isNotWaitingAt.bpmn"
  })
  void isNotWaitingAtOneOfTwoActivitiesFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isNotWaitingAt"
    );
    // When
    complete(taskQuery().singleResult());
    // Then
    expect(() -> assertThat(processInstance).isNotWaitingAt("UserTask_2"));
    // And
    expect(() -> assertThat(processInstance).isNotWaitingAt("UserTask_1", "UserTask_2"));
    // And
    expect(() -> assertThat(processInstance).isNotWaitingAt("UserTask_2", "UserTask_3"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isNotWaitingAt.bpmn"
  })
  void isNotWaitingAtNullError() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isNotWaitingAt"
    );
    // Then
    expect(() -> {
      String[] waitingAt = null;
      assertThat(processInstance).isNotWaitingAt(waitingAt);
    });
    // And
    expect(() -> assertThat(processInstance).isNotWaitingAt("ok", null));
    // And
    expect(() -> assertThat(processInstance).isNotWaitingAt(null, "ok"));
    // And
    expect(() -> {
      String[] args = new String[]{};
      assertThat(processInstance).isNotWaitingAt(args);
    });
  }

}
