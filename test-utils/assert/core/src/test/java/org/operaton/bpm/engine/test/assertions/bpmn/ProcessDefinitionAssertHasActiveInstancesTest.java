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

import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

import org.junit.jupiter.api.Test;

public class ProcessDefinitionAssertHasActiveInstancesTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessDefinitionAssert-hasActiveInstances.bpmn"
  })
  void hasActiveInstancesOneStartedSuccess() {
    // Given
    final ProcessDefinition processDefinition =
      processDefinitionQuery().processDefinitionKey("ProcessDefinitionAssert-hasActiveInstances").singleResult();
    // When
    runtimeService().startProcessInstanceByKey(
      "ProcessDefinitionAssert-hasActiveInstances"
    );
    // Then
    assertThat(processDefinition).hasActiveInstances(1);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessDefinitionAssert-hasActiveInstances.bpmn"
  })
  void hasActiveInstancesOneStartedFailure() {
    // Given
    final ProcessDefinition processDefinition =
      processDefinitionQuery().processDefinitionKey("ProcessDefinitionAssert-hasActiveInstances").singleResult();
    // When
    runtimeService().startProcessInstanceByKey(
      "ProcessDefinitionAssert-hasActiveInstances"
    );
    // Then
    expect(() -> assertThat(processDefinition).hasActiveInstances(0));
    // And
    expect(() -> assertThat(processDefinition).hasActiveInstances(2));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessDefinitionAssert-hasActiveInstances.bpmn"
  })
  void hasActiveInstancesTwoStartedSuccess() {
    // Given
    final ProcessDefinition processDefinition =
      processDefinitionQuery().processDefinitionKey("ProcessDefinitionAssert-hasActiveInstances").singleResult();
    // When
    runtimeService().startProcessInstanceByKey(
      "ProcessDefinitionAssert-hasActiveInstances"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessDefinitionAssert-hasActiveInstances"
    );
    // Then
    assertThat(processDefinition).hasActiveInstances(2);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessDefinitionAssert-hasActiveInstances.bpmn"
  })
  void hasActiveInstancesTwoStartedFailure() {
    // Given
    final ProcessDefinition processDefinition =
      processDefinitionQuery().processDefinitionKey("ProcessDefinitionAssert-hasActiveInstances").singleResult();
    // When
    runtimeService().startProcessInstanceByKey(
      "ProcessDefinitionAssert-hasActiveInstances"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessDefinitionAssert-hasActiveInstances"
    );
    // Then
    expect(() -> assertThat(processDefinition).hasActiveInstances(0));
    // And
    expect(() -> assertThat(processDefinition).hasActiveInstances(1));
    // And
    expect(() -> assertThat(processDefinition).hasActiveInstances(3));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessDefinitionAssert-hasActiveInstances.bpmn"
  })
  void hasActiveInstancesTwoStartedOneEndedSuccess() {
    // Given
    final ProcessDefinition processDefinition =
      processDefinitionQuery().processDefinitionKey("ProcessDefinitionAssert-hasActiveInstances").singleResult();
    // When
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessDefinitionAssert-hasActiveInstances"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessDefinitionAssert-hasActiveInstances"
    );
    // And
    complete(task(processInstance));
    // Then
    assertThat(processDefinition).hasActiveInstances(1);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessDefinitionAssert-hasActiveInstances.bpmn"
  })
  void hasActiveInstancesTwoStartedOneEndedFailure() {
    // Given
    final ProcessDefinition processDefinition =
      processDefinitionQuery().processDefinitionKey("ProcessDefinitionAssert-hasActiveInstances").singleResult();
    // When
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessDefinitionAssert-hasActiveInstances"
    );
    // And
    runtimeService().startProcessInstanceByKey(
      "ProcessDefinitionAssert-hasActiveInstances"
    );
    // And
    complete(task(processInstance));
    // Then
    expect(() -> assertThat(processDefinition).hasActiveInstances(0));
    // And
    expect(() -> assertThat(processDefinition).hasActiveInstances(2));
  }

}
