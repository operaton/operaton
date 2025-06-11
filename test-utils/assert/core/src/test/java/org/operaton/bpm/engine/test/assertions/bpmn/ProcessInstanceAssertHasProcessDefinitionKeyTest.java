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

class ProcessInstanceAssertHasProcessDefinitionKeyTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasProcessDefinitionKey-1.bpmn", "bpmn/ProcessInstanceAssert-hasProcessDefinitionKey-2.bpmn"
  })
  void hasProcessDefinitionKeySuccess() {
    // When
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasProcessDefinitionKey-1"
    );
    // Then
    assertThat(processInstance).hasProcessDefinitionKey("ProcessInstanceAssert-hasProcessDefinitionKey-1");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasProcessDefinitionKey-1.bpmn", "bpmn/ProcessInstanceAssert-hasProcessDefinitionKey-2.bpmn"
  })
  void hasProcessDefinitionKeyFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasProcessDefinitionKey-2"
    );
    // When
    runtimeService().suspendProcessInstanceById(processInstance.getId());
    // Then
    expect(() -> assertThat(processInstance).hasProcessDefinitionKey("ProcessInstanceAssert-hasProcessDefinitionKey-1"));
  }

}
