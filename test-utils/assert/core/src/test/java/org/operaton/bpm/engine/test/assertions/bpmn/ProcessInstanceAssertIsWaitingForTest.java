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
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;

import org.junit.jupiter.api.Test;

class ProcessInstanceAssertIsWaitingForTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isWaitingFor.bpmn"
  })
  void isWaitingForOneMessageSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isWaitingFor"
    );
    // Then
    assertThat(processInstance).isWaitingFor("myMessage");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isWaitingFor-2.bpmn"
  })
  void isWaitingForTwoMessagesSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isWaitingFor-2"
    );
    // Then
    assertThat(processInstance).isWaitingFor("myMessage", "yourMessage");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isWaitingFor-2.bpmn"
  })
  void isWaitingForOneOfTwoMessagesSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isWaitingFor-2"
    );
    // When
    runtimeService().correlateMessage("myMessage");
    // Then
    assertThat(processInstance).isWaitingFor("yourMessage");
    // And
    expect(() -> assertThat(processInstance).isWaitingFor("myMessage"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isWaitingFor.bpmn"
  })
  void isWaitingForOneMessageFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isWaitingFor"
    );
    // When
    runtimeService().correlateMessage("myMessage");
    // Then
    expect(() -> assertThat(processInstance).isWaitingFor("myMessage"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isWaitingFor.bpmn"
  })
  void isWaitingForNotWaitingForOneOfOneFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isWaitingFor"
    );
    // Then
    expect(() -> assertThat(processInstance).isWaitingFor("yourMessage"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isWaitingFor.bpmn"
  })
  void isWaitingForNotWaitingForOneOfTwoFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isWaitingFor"
    );
    // Then
    expect(() -> assertThat(processInstance).isWaitingFor("myMessage", "yourMessage"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isWaitingFor.bpmn"
  })
  void isWaitingForNullError() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isWaitingFor"
    );
    // Then
    expect(() -> assertThat(processInstance).isWaitingFor());
    // And
    expect(() -> {
      String[] waitingFor = null;
      assertThat(processInstance).isWaitingFor(waitingFor);
    });
    // And
    expect(() -> assertThat(processInstance).isWaitingFor("myMessage", null));
  }

}
