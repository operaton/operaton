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

class ProcessInstanceAssertHasNoVariablesTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasNoVariables.bpmn"
  })
  void hasNoVariablesNoneSuccess() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasNoVariables"
    );
    // Then
    assertThat(processInstance).hasNoVariables();
    // When
    complete(task(processInstance));
    // Then
    assertThat(processInstance).hasNoVariables();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasNoVariables.bpmn"
  })
  void hasNoVariablesOneFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasNoVariables", withVariables("aVariable", "aValue")
    );
    // Then
    expect(() -> assertThat(processInstance).hasNoVariables());
    // When
    complete(task(processInstance));
    // Then
    expect(() -> assertThat(processInstance).hasNoVariables());
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasNoVariables.bpmn"
  })
  void hasNoVariablesTwoFailure() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasNoVariables", withVariables("firstVariable", "firstValue", "secondVariable", "secondValue")
    );
    // Then
    expect(() -> assertThat(processInstance).hasNoVariables());
    // When
    complete(task(processInstance));
    // Then
    expect(() -> assertThat(processInstance).hasNoVariables());
  }

}
