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

import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.complete;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.task;
import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.withVariables;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;
import org.junit.Rule;

public class ProcessEngineTestsCompleteTest extends ProcessAssertTestCase {

  @Rule
  public ProcessEngineRule processEngineRule = new ProcessEngineRule();

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-complete.bpmn"
  })
  void completeSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-complete"
    );
    // When
    complete(task(processInstance));
    // Then
    assertThat(processInstance).isEnded();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-complete.bpmn"
  })
  void completeFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-complete"
    );
    // And
    final Task task = task(processInstance);
    // When
    complete(task);
    // Then
    expect(() -> complete(task), ProcessEngineException.class);
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-complete.bpmn"
  })
  void completeWithVariablesSuccess() {
    // Given
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-complete"
    );
    // When
    complete(task(processInstance), withVariables("a", "b"));
    // Then
    assertThat(processInstance).isEnded();
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-complete.bpmn"
  })
  void completeWithVariablesFailure() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessEngineTests-complete"
    );
    // And
    final Task task = task(processInstance);
    // When
    complete(task);
    // Then
    expect(() -> complete(task, withVariables("a", "b")), ProcessEngineException.class);
  }

}
