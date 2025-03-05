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

public class ProcessInstanceAssertHasPassedTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassed.bpmn"
  })
  void hasPassedOnlyActivityRunningInstanceSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassed"
    );
    // When
    complete(taskQuery().singleResult());
    // Then
    assertThat(processInstance).hasPassed("UserTask_1");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassed.bpmn"
  })
  void hasPassedOnlyActivityRunningInstanceFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassed"
    );
    // Then
    expect(() -> assertThat(processInstance).hasPassed("UserTask_1"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassed.bpmn"
  })
  void hasPassedParallelActivitiesRunningInstanceSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassed"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // Then
    assertThat(processInstance).hasPassed("UserTask_1");
    // And
    assertThat(processInstance).hasPassed("UserTask_2");
    // And
    assertThat(processInstance).hasPassed("UserTask_1", "UserTask_2");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassed.bpmn"
  })
  void hasPassedParallelActivitiesRunningInstanceFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassed"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // Then
    expect(() -> assertThat(processInstance).hasPassed("UserTask_3"));
    expect(() -> assertThat(processInstance).hasPassed("UserTask_4"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassed.bpmn"
  })
  void hasPassedSeveralActivitiesRunningInstanceSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassed"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
    // Then
    assertThat(processInstance).hasPassed("UserTask_1");
    // And
    assertThat(processInstance).hasPassed("UserTask_2");
    // And
    assertThat(processInstance).hasPassed("UserTask_3");
    // And
    assertThat(processInstance).hasPassed("UserTask_1", "UserTask_2", "UserTask_3");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassed.bpmn"
  })
  void hasPassedSeveralActivitiesRunningInstanceFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassed"
    );
    // When
    complete(taskQuery().singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
    // Then
    expect(() -> assertThat(processInstance).hasPassed("UserTask_4"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassed.bpmn"
  })
  void hasPassedSeveralActivitiesHistoricInstanceSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassed"
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
    assertThat(processInstance).hasPassed("UserTask_1");
    // And
    assertThat(processInstance).hasPassed("UserTask_2");
    // And
    assertThat(processInstance).hasPassed("UserTask_3");
    // And
    assertThat(processInstance).hasPassed("UserTask_4");
    // And
    assertThat(processInstance).hasPassed("UserTask_1", "UserTask_2", "UserTask_3", "UserTask_4");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassed.bpmn"
  })
  void hasPassedSeveralActivitiesHistoricInstanceFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassed"
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
    expect(() -> assertThat(processInstance).hasPassed("UserTask_5"));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isWaitingAt.bpmn"
  })
  void hasPassedNullError() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isWaitingAt"
    );
    // Then
    expect(() -> {
      String[] passed = null;
      assertThat(processInstance).hasPassed(passed);
    });
    expect(() -> assertThat(processInstance).hasPassed("ok", null));
    expect(() -> assertThat(processInstance).hasPassed(null, "ok"));
    expect(() -> {
      String[] args = new String[]{};
      assertThat(processInstance).hasPassed(args);
    });
  }

}
