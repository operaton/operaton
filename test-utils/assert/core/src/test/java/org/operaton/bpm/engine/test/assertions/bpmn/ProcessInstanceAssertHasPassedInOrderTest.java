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

class ProcessInstanceAssertHasPassedInOrderTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassedInOrder.bpmn"
  })
  void hasPassedInOrderOnlyActivityRunningInstanceSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassedInOrder"
    );
    // When
    complete(taskQuery().singleResult());
    // Then
    assertThat(processInstance).hasPassedInOrder("UserTask_1");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassedInOrder.bpmn"
  })
  void hasPassedInOrderOnlyActivityRunningInstanceFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassedInOrder"
    );
    // Then
    expect(() -> assertThat(processInstance).hasPassedInOrder("UserTask_1"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassedInOrder.bpmn"
  })
  void hasPassedInOrderParallelActivitiesRunningInstanceSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassedInOrder"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // Then
    assertThat(processInstance).hasPassedInOrder("UserTask_1");
    // And
    assertThat(processInstance).hasPassedInOrder("UserTask_2");
    // And
    assertThat(processInstance).hasPassedInOrder("UserTask_1", "UserTask_2");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassedInOrder.bpmn"
  })
  void hasPassedInOrderParallelActivitiesRunningInstanceFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassedInOrder"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // Then
    expect(() -> assertThat(processInstance).hasPassedInOrder("UserTask_3"));
    // And
    expect(() -> assertThat(processInstance).hasPassedInOrder("UserTask_4"));
    // And
    expect(() -> assertThat(processInstance).hasPassedInOrder("UserTask_5"));
    // And
    expect(() -> assertThat(processInstance).hasPassedInOrder("UserTask_2", "UserTask_1"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassedInOrder.bpmn"
  })
  void hasPassedInOrderSeveralActivitiesRunningInstanceSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassedInOrder"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_4").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
    // Then
    assertThat(processInstance).hasPassedInOrder("UserTask_1");
    // And
    assertThat(processInstance).hasPassedInOrder("UserTask_2");
    // And
    assertThat(processInstance).hasPassedInOrder("UserTask_3");
    // And
    assertThat(processInstance).hasPassedInOrder("UserTask_4");
    // And
    assertThat(processInstance).hasPassedInOrder("UserTask_1", "UserTask_2", "UserTask_4");
    // And
    assertThat(processInstance).hasPassedInOrder("UserTask_1", "UserTask_3");
    /*
     * Hint: passedInOrder can not be guaranteed for [UserTask_2, UserTask_4, UserTask_3] due to
     * parallel execution paths when endTime can't be determined with enough precision
     */
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassedInOrder.bpmn"
  })
  void hasPassedInOrderSeveralActivitiesRunningInstanceFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassedInOrder"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
    // Then
    expect(() -> assertThat(processInstance).hasPassedInOrder("UserTask_4"));
    // And
    expect(() -> assertThat(processInstance).hasPassedInOrder("UserTask_2", "UserTask_1"));
    // And
    expect(() -> assertThat(processInstance).hasPassedInOrder("UserTask_3", "UserTask_1"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassedInOrder.bpmn"
  })
  void hasPassedInOrderSeveralActivitiesHistoricInstanceSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassedInOrder"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_4").singleResult());
    // Then
    assertThat(processInstance).hasPassedInOrder("UserTask_1");
    // And
    assertThat(processInstance).hasPassedInOrder("UserTask_2");
    // And
    assertThat(processInstance).hasPassedInOrder("UserTask_3");
    // And
    assertThat(processInstance).hasPassedInOrder("UserTask_4");
    // And
    assertThat(processInstance).hasPassedInOrder("UserTask_1", "UserTask_2", "UserTask_4");
    // And
    assertThat(processInstance).hasPassedInOrder("UserTask_1", "UserTask_3", "UserTask_4");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassedInOrder.bpmn"
  })
  void hasPassedInOrderSeveralActivitiesHistoricInstanceFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassedInOrder"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_4").singleResult());
    // Then
    expect(() -> assertThat(processInstance).hasPassedInOrder("UserTask_4", "UserTask_1"));
    // And
    expect(() -> assertThat(processInstance).hasPassedInOrder("UserTask_2", "UserTask_1", "UserTask_3", "UserTask_4"));
    // And
    expect(() -> assertThat(processInstance).hasPassedInOrder("UserTask_4", "UserTask_3"));
    // And
    expect(() -> assertThat(processInstance).hasPassedInOrder("UserTask_5"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isWaitingAt.bpmn"
  })
  void hasPassedInOrderNullError() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isWaitingAt"
    );
    // Then
    expect(() -> {
      String[] passed = null;
      assertThat(processInstance).hasPassedInOrder(passed);
    });
    expect(() -> assertThat(processInstance).hasPassedInOrder("ok", null));
    expect(() -> assertThat(processInstance).hasPassedInOrder(null, "ok"));
    expect(() -> {
      String[] args = new String[]{};
      assertThat(processInstance).hasPassedInOrder(args);
    });
  }

}
