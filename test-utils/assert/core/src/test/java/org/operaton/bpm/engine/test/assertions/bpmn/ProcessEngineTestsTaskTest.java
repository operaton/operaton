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

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;

import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

class ProcessEngineTestsTaskTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // When
    assertThat(processInstance).isNotNull();
    // Then
    assertThat(task()).isNotNull().hasDefinitionKey("UserTask_1");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskOnlyActivityFailure() {
    // Given
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // Then
    expect(BpmnAwareTests::task, IllegalStateException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTwoActivitiesFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    assertThat(processInstance).isNotNull();
    // And
    complete(task());
    // When
    expect(BpmnAwareTests::task, ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTaskDefinitionKeyOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // When
    assertThat(processInstance).isNotNull();
    // Then
    assertThat(task("UserTask_1")).isNotNull().hasDefinitionKey("UserTask_1");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTaskDefinitionKeyTwoActivitiesSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // When
    assertThat(processInstance).isNotNull();
    complete(task());
    // Then
    assertThat(task("UserTask_2")).isNotNull().hasDefinitionKey("UserTask_2");
    // And
    assertThat(task("UserTask_3")).isNotNull().hasDefinitionKey("UserTask_3");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTaskDefinitionKeyOnlyActivityFailure() {
    // Given
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // Then
    expect(() -> task("UserTask_1"), IllegalStateException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTaskQueryOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // When
    assertThat(processInstance).isNotNull();
    // Then
    assertThat(task(taskQuery().taskDefinitionKey("UserTask_1"))).isNotNull().hasDefinitionKey("UserTask_1");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTaskQueryOnlyActivityFailure() {
    // Given
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // Then
    expect(() -> task(taskQuery().taskDefinitionKey("UserTask_1")), IllegalStateException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTaskQueryTwoActivitiesSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // When
    assertThat(processInstance).isNotNull();
    complete(task());
    // Then
    assertThat(task(taskQuery().taskDefinitionKey("UserTask_2"))).isNotNull().hasDefinitionKey("UserTask_2");
    // And
    assertThat(task(taskQuery().taskDefinitionKey("UserTask_3"))).isNotNull().hasDefinitionKey("UserTask_3");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTaskQueryTwoActivitiesFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    assertThat(processInstance).isNotNull();
    // And
    complete(task());
    // When
    expect(() -> task(taskQuery()), ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskProcessInstanceOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // Then
    assertThat(task(processInstance)).isNotNull().hasDefinitionKey("UserTask_1");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTwoActivitiesProcessInstanceFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    complete(task(processInstance));
    // When
    expect(() -> task(processInstance), ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTaskDefinitionKeyProcessInstanceOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // Then
    assertThat(task("UserTask_1", processInstance)).isNotNull().hasDefinitionKey("UserTask_1");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTaskDefinitionKeyProcessInstanceTwoActivitiesSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // When
    complete(task(processInstance));
    // Then
    assertThat(task("UserTask_2", processInstance)).isNotNull().hasDefinitionKey("UserTask_2");
    // And
    assertThat(task("UserTask_3", processInstance)).isNotNull().hasDefinitionKey("UserTask_3");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTaskQueryProcessInstanceOnlyActivitySuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // Then
    assertThat(task(taskQuery().taskDefinitionKey("UserTask_1"), processInstance)).isNotNull().hasDefinitionKey("UserTask_1");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTaskQueryProcessInstanceTwoActivitiesSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // When
    complete(task(processInstance));
    // Then
    assertThat(task(taskQuery().taskDefinitionKey("UserTask_2"), processInstance)).isNotNull().hasDefinitionKey("UserTask_2");
    // And
    assertThat(task(taskQuery().taskDefinitionKey("UserTask_3"), processInstance)).isNotNull().hasDefinitionKey("UserTask_3");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-task.bpmn"
  })
  void taskTaskQueryProcessInstanceTwoActivitiesFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-task"
    );
    // And
    complete(task(processInstance));
    // When
    expect(() -> task(taskQuery(), processInstance), ProcessEngineException.class);
  }

}
