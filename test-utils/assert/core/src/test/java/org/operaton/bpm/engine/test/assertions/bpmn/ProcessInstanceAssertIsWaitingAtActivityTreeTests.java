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

class ProcessInstanceAssertIsWaitingAtActivityTreeTests extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isWaitingAt-ActivityTreeTests.bpmn"
  })
  void isWaitingAtAsyncBefore() {
    // When
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-isWaitingAt-ActivityTreeTests"
    );
    // Then
    assertThat(processInstance).isWaitingAt("AsyncServiceTask");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isWaitingAt-ActivityTreeTests.bpmn"})
  void isWaitingAtSubprocess() {
    // When
    final ProcessInstance processInstance = runtimeService()
        .createProcessInstanceByKey("ProcessInstanceAssert-isWaitingAt-ActivityTreeTests")
        .startAfterActivity("AsyncServiceTask")
        .execute();
    // Then
    assertThat(processInstance).isWaitingAt("SubProcess", "SubSubProcess", "NestedAsyncServiceTask");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isWaitingAt-Subprocesses.bpmn"})
  void isWaitingAtNestedUserTask() {
    // When
    final ProcessInstance processInstance = runtimeService()
        .startProcessInstanceByKey("ProcessInstanceAssert-isWaitingAt-Subprocesses");

    // Then
    assertThat(processInstance).isWaitingAtExactly("SubProcess", "SubSubProcess", "NestedUserTask");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-isWaitingAt-AsyncUserTask.bpmn"})
  void isWaitingAtAsyncUserTask() {
    // When
    ProcessInstance processInstance = runtimeService()
        .startProcessInstanceByKey("ProcessInstanceAssert-isWaitingAt-AsyncUserTask");

    // Then
    assertThat(processInstance).isWaitingAt("AsyncUserTask");

    // And when
    execute(job()); //async before

    //Then
    assertThat(processInstance).isWaitingAt("AsyncUserTask");

    // And when
    complete(task());

    // Then
    assertThat(processInstance).isWaitingAt("AsyncUserTask");

    // And when
    execute(job()); // async after

    // Then
    assertThat(processInstance).isEnded();
  }

}
