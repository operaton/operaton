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

class ProcessInstanceAssertHasNotPassedTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasNotPassed.bpmn"
  })
  void hasNotPassedOnlyActivityRunningInstanceFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasNotPassed"
    );
    // When
    complete(taskQuery().singleResult(), withVariables("doUserTask5", false));
    // Then
    expect(() -> assertThat(processInstance).hasNotPassed("UserTask_1"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasNotPassed.bpmn"
  })
  void hasNotPassedOnlyActivityRunningInstanceSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasNotPassed"
    );
    // Then
    assertThat(processInstance).hasNotPassed("UserTask_1");
    // And
    assertThat(processInstance).hasNotPassed("UserTask_2", "UserTask_3", "UserTask_4");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasNotPassed.bpmn"
  })
  void hasNotPassedParallelActivitiesRunningInstanceSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasNotPassed"
    );
    // When
    complete(taskQuery().singleResult(), withVariables("doUserTask5", false));
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // Then
    assertThat(processInstance).hasNotPassed("UserTask_3");
    // And
    assertThat(processInstance).hasNotPassed("UserTask_4");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasNotPassed.bpmn"
  })
  void hasNotPassedParallelActivitiesRunningInstanceFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasNotPassed"
    );
    // When
    complete(taskQuery().singleResult(), withVariables("doUserTask5", false));
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // Then
    expect(() -> assertThat(processInstance).hasNotPassed("UserTask_2", "UserTask_3", "UserTask_4"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasNotPassed.bpmn"
  })
  void hasNotPassedSeveralActivitiesRunningInstanceSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasNotPassed"
    );
    // When
    complete(taskQuery().singleResult(), withVariables("doUserTask5", false));
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
    // Then
    assertThat(processInstance).hasNotPassed("UserTask_4", "UserTask_5");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasNotPassed.bpmn"
  })
  void hasNotPassedSeveralActivitiesHistoricInstanceSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasNotPassed"
    );
    // When
    complete(taskQuery().singleResult(), withVariables("doUserTask5", false));
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_4").singleResult());
    // Then
    assertThat(processInstance).hasNotPassed("UserTask_5");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasNotPassed.bpmn"
  })
  void hasNotPassedSeveralActivitiesHistoricInstanceFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasNotPassed"
    );
    // When
    complete(taskQuery().singleResult(), withVariables("doUserTask5", true));
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_5").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_4").singleResult());
    // Then
    expect(() -> assertThat(processInstance).hasNotPassed("UserTask_5"));
  }

}
