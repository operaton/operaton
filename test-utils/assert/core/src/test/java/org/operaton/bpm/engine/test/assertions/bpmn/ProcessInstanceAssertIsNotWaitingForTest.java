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
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;

import org.junit.jupiter.api.Test;

class ProcessInstanceAssertIsNotWaitingForTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isNotWaitingFor.bpmn"
  })
  void isNotWaitingForOneMessageSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isNotWaitingFor"
    );
    // When
    runtimeService().correlateMessage("myMessage");
    // Then
    assertThat(processInstance).isNotWaitingFor("myMessage");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isNotWaitingFor-2.bpmn"
  })
  void isNotWaitingForTwoMessagesSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isNotWaitingFor-2"
    );
    // When
    runtimeService().correlateMessage("myMessage");
    // And
    runtimeService().correlateMessage("yourMessage");
    // Then
    assertThat(processInstance).isNotWaitingFor("myMessage", "yourMessage");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isNotWaitingFor-2.bpmn"
  })
  void isNotWaitingForOneOfTwoMessagesSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isNotWaitingFor-2"
    );
    // When
    runtimeService().correlateMessage("myMessage");
    // Then
    assertThat(processInstance).isNotWaitingFor("myMessage");
    // And
    expect(() -> assertThat(processInstance).isNotWaitingFor("yourMessage"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isNotWaitingFor.bpmn"
  })
  void isNotWaitingForOneMessageFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isNotWaitingFor"
    );
    // Then
    expect(() -> assertThat(processInstance).isNotWaitingFor("myMessage"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isNotWaitingFor.bpmn"
  })
  void isNotWaitingForNotWaitingForOneOfOneSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isNotWaitingFor"
    );
    // Then
    assertThat(processInstance).isNotWaitingFor("yourMessage");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isNotWaitingFor.bpmn"
  })
  void isNotWaitingForNotWaitingForOneOfTwoFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isNotWaitingFor"
    );
    // Then
    expect(() -> assertThat(processInstance).isNotWaitingFor("myMessage", "yourMessage"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isNotWaitingFor.bpmn"
  })
  void isNotWaitingForNullError() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isNotWaitingFor"
    );
    // Then
    expect(() -> assertThat(processInstance).isNotWaitingFor());
    // And
    expect(() -> {
      String[] waitingFor = null;
      assertThat(processInstance).isNotWaitingFor(waitingFor);
    });
    // And
    expect(() -> assertThat(processInstance).isNotWaitingFor("myMessage", null));
  }

}
