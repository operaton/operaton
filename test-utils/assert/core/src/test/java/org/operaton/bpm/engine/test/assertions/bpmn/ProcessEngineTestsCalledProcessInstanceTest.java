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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

import org.junit.jupiter.api.Test;

class ProcessEngineTestsCalledProcessInstanceTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-calledProcessInstance-superProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess2.bpmn"
  })
  void calledProcessInstanceNoArgsCalledTooEarlyFailure() {
    // Given
    final String processDefinitionKey = "ProcessEngineTests-calledProcessInstance-superProcess1";
    runtimeService().startProcessInstanceByKey(processDefinitionKey);
    // Then
    assertFailureOnCalledProcessInstance(BpmnAwareTests::calledProcessInstance);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-calledProcessInstance-superProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess2.bpmn"
  })
  void calledProcessInstanceArgProcessInstanceCalledTooEarlySuccess() {
    // Given
    final String processDefinitionKey = "ProcessEngineTests-calledProcessInstance-superProcess1";
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(processDefinitionKey);
    // Then
    assertThat(calledProcessInstance(processInstance)).isNotNull();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-calledProcessInstance-superProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess2.bpmn"
  })
  void calledProcessInstanceArgProcessInstanceQueryCalledTooEarlyFailure() {
    // Given
    final String processDefinitionKey = "ProcessEngineTests-calledProcessInstance-superProcess1";
    runtimeService().startProcessInstanceByKey(processDefinitionKey);
    // Then
    assertFailureOnCalledProcessInstance(() -> calledProcessInstance(processInstanceQuery()));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-calledProcessInstance-superProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess2.bpmn"
  })
  void calledProcessInstanceArgProcessDefinitionKeyCalledTooEarlyFailure() {
    // Given
    final String processDefinitionKey = "ProcessEngineTests-calledProcessInstance-superProcess1";
    runtimeService().startProcessInstanceByKey(processDefinitionKey);
    // Then
    assertFailureOnCalledProcessInstance(() -> calledProcessInstance(processDefinitionKey));
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-calledProcessInstance-superProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess2.bpmn"
  })
  void calledProcessInstanceArgProcessDefinitionKeyAndProcessInstanceCalledTooEarlySuccess() {
    // Given
    final String processDefinitionKey = "ProcessEngineTests-calledProcessInstance-superProcess1";
    final String subProcessDefinitionKey = "ProcessEngineTests-calledProcessInstance-subProcess1";
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(processDefinitionKey);
    // Then
    assertThat(calledProcessInstance(subProcessDefinitionKey, processInstance)).isNotNull();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-calledProcessInstance-superProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess2.bpmn"
  })
  void calledProcessInstanceArgProcessInstanceQueryAndProcessInstanceCalledTooEarlySuccess() {
    // Given
    final String processDefinitionKey = "ProcessEngineTests-calledProcessInstance-superProcess1";
    final String subProcessDefinitionKey = "ProcessEngineTests-calledProcessInstance-subProcess1";
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(processDefinitionKey);
    // Then
    assertThat(calledProcessInstance(processInstanceQuery().processDefinitionKey(subProcessDefinitionKey), processInstance))
      .isNotNull();
  }

  private void assertFailureOnCalledProcessInstance(Runnable runnable) {
    assertThatThrownBy(runnable::run, "call to calledProcessInstance() should have thrown an error")
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-calledProcessInstance-superProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess2.bpmn"
  })
  void calledProcessInstanceFirstOfTwoSequentialSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-calledProcessInstance-superProcess1"
    );
    // When
    assertThat(processInstance)
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-superProcess1");
    // Then
    assertThat(calledProcessInstance())
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess1");
    // And
    assertThat(calledProcessInstance(processInstance))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess1");
    // When
    assertThat(processInstance)
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-superProcess1");
    // And
    assertThat(calledProcessInstance("ProcessEngineTests-calledProcessInstance-subProcess1"))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess1");
    // And
    assertThat(calledProcessInstance("ProcessEngineTests-calledProcessInstance-subProcess1", processInstance))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess1");
    // When
    assertThat(processInstance)
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-superProcess1");
    // And
    assertThat(calledProcessInstance(processInstanceQuery().processDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess1")))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess1");
    // And
    assertThat(calledProcessInstance(processInstanceQuery().processDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess1"), processInstance))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess1");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-calledProcessInstance-superProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess2.bpmn"
  })
  void calledProcessInstanceSecondOfTwoSequentialSuccess() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-calledProcessInstance-superProcess1"
    );
    // And
    complete(task("UserTask_1", calledProcessInstance(processInstance)));
    // When
    assertThat(processInstance)
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-superProcess1");
    // Then
    assertThat(calledProcessInstance())
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess2");
    // And
    assertThat(calledProcessInstance(processInstance))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess2");
    // When
    assertThat(processInstance)
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-superProcess1");
    // And
    assertThat(calledProcessInstance("ProcessEngineTests-calledProcessInstance-subProcess2"))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess2");
    // And
    assertThat(calledProcessInstance("ProcessEngineTests-calledProcessInstance-subProcess2", processInstance))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess2");
    // When
    assertThat(processInstance)
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-superProcess1");
    // And
    assertThat(calledProcessInstance(processInstanceQuery().processDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess2")))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess2");
    // And
    assertThat(calledProcessInstance(processInstanceQuery().processDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess2"), processInstance))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess2");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-calledProcessInstance-superProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess2.bpmn"
  })
  void calledProcessInstanceSecondOfTwoSequentialFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-calledProcessInstance-superProcess1"
    );
    // And
    complete(task("UserTask_1", calledProcessInstance(processInstance)));
    // Then
    expect(() -> assertThat(calledProcessInstance())
      .isNotNull());
    // And
    expect(() -> assertThat(calledProcessInstance("ProcessEngineTests-calledProcessInstance-subProcess2"))
      .isNotNull());
    // And
    expect(() -> assertThat(calledProcessInstance(processInstanceQuery().processDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess2")))
      .isNotNull());
    // When
    assertThat(processInstance)
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-superProcess1");
    // Then
    expect(() -> assertThat(calledProcessInstance())
      .isNull());
    // And
    expect(() -> assertThat(calledProcessInstance("ProcessEngineTests-calledProcessInstance-subProcess1"))
      .isNotNull());
    // And
    expect(() -> assertThat(calledProcessInstance(processInstanceQuery().processDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess1")))
      .isNotNull());
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-calledProcessInstance-superProcess2.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess2.bpmn"
  })
  void calledProcessInstanceTwoOfTwoParallelSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-calledProcessInstance-superProcess2"
    );
    // Then
    assertThat(calledProcessInstance("ProcessEngineTests-calledProcessInstance-subProcess1", processInstance))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess1");
    // And
    assertThat(calledProcessInstance(processInstanceQuery().processDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess1"), processInstance))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess1");
    // And
    assertThat(calledProcessInstance("ProcessEngineTests-calledProcessInstance-subProcess2", processInstance))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess2");
    // And
    assertThat(calledProcessInstance(processInstanceQuery().processDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess2"), processInstance))
      .hasProcessDefinitionKey("ProcessEngineTests-calledProcessInstance-subProcess2");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-calledProcessInstance-superProcess2.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess1.bpmn", "bpmn/ProcessEngineTests-calledProcessInstance-subProcess2.bpmn"
  })
  void calledProcessInstanceTwoOfTwoParallelFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-calledProcessInstance-superProcess2"
    );
    // Then
    expect(() -> assertThat(calledProcessInstance("ProcessEngineTests-calledProcessInstance-subProcess3", processInstance))
      .isNotNull());
  }

}
