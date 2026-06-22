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

import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.assertions.helpers.ProcessAssertTestCase;

import static org.operaton.bpm.engine.test.assertions.bpmn.BpmnAwareTests.findId;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessEngineTestsFindIdTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = "bpmn/ProcessEngineTests-findTest.bpmn")
  void findPlainTaskByName() {
    // Given
    // Process model deployed
    // When
    String id = findId("Plain task");
    // Then
    assertThat(id).isEqualTo("PlainTask_TestID");
  }

  @Test
  @Deployment(resources = "bpmn/ProcessEngineTests-findTest.bpmn")
  void findEndEventByName() {
    // Given
    // Process model deployed
    // When
    String end = findId("End");
    // Then
    assertThat(end).isEqualTo("End_TestID");
  }

  @Test
  @Deployment(resources = "bpmn/ProcessEngineTests-findTest.bpmn")
  void findAttachedEventByName() {
    // Given
    // Process model deployed
    // When
    String attachedBoundaryEvent = findId("2 days");
    // Then
    assertThat(attachedBoundaryEvent).isEqualTo("n2Days_TestID");
  }

  @Test
  @Deployment(resources = "bpmn/ProcessEngineTests-findTest.bpmn")
  void findGatewayByName() {
    // Given
    // process model deployed
    // When
    String gateway = findId("Continue?");
    // Then
    assertThat(gateway).isEqualTo("Continue_TestID");
  }

  @Test
  @Deployment(resources = "bpmn/ProcessEngineTests-findTest.bpmn")
  void nameNotFound() {
    expect(() -> findId("This should not be found"), "doesn't exist");
  }

  @Test
  @Deployment(resources = "bpmn/ProcessEngineTests-findTest.bpmn")
  void nameNull() {
    expect(() -> findId(null), IllegalArgumentException.class);
  }

  @Test
  @Deployment(resources = "bpmn/ProcessEngineTests-findTest.bpmn")
  void findAllElements() {
    // Given
    // Process model deployed
    // When
    String start = findId("Start");
    String plainTask = findId("Plain task");
    String userTask = findId("User task");
    String receiveTask = findId("Receive task");
    String attachedBoundaryEvent = findId("2 days");
    String gateway = findId("Continue?");
    String end = findId("End");
    String messageEnd = findId("Message End");
    //Then
    assertThat(start).isEqualTo("Start_TestID");
    assertThat(plainTask).isEqualTo("PlainTask_TestID");
    assertThat(userTask).isEqualTo("UserTask_TestID");
    assertThat(receiveTask).isEqualTo("ReceiveTask_TestID");
    assertThat(attachedBoundaryEvent).isEqualTo("n2Days_TestID");
    assertThat(gateway).isEqualTo("Continue_TestID");
    assertThat(end).isEqualTo("End_TestID");
    assertThat(messageEnd).isEqualTo("MessageEnd_TestID");
  }

  @Test
  @Deployment(resources = "bpmn/ProcessEngineTests-findInTwoPools.bpmn")
  void findInTwoPoolsInPool1() {
    // Given
    // Process model with two pools deployed
    // When
    String callActivity = findId("Call activity one");
    // Then
    assertThat(callActivity).isEqualTo("CallActivityOne_TestID");
  }

  @Test
  @Deployment(resources = "bpmn/ProcessEngineTests-findInTwoPools.bpmn")
  void findTwoPoolsInPool2() {
    // Given
    // Process model with two pools deployed
    // When
    String task = findId("Subprocess task");
    // Then
    assertThat(task).isEqualTo("SubProcessTask_TestID");
  }

  @Test
  @Deployment(resources = {"bpmn/ProcessEngineTests-findTest.bpmn", "bpmn/ProcessEngineTests-findInTwoPools.bpmn"})
  void findOneInEachOfTwoDiagrams() {
    // Given
    // Two process models deployed
    // When
    String start = findId("Start");
    String plainTask = findId("Plain task");
    String startSuperProcess = findId("Super started");
    String taskTwo = findId("Task two");
    String proc2Started = findId("Proc 2 started");
    // Then
    assertThat(start).isEqualTo("Start_TestID");
    assertThat(plainTask).isEqualTo("PlainTask_TestID");
    assertThat(startSuperProcess).isEqualTo("SuperStarted_TestID");
    assertThat(taskTwo).isEqualTo("TaskTwo_TestID");
    assertThat(proc2Started).isEqualTo("Proc2Started_TestID");
  }

  @Test
  @Deployment(resources = "bpmn/ProcessEngineTests-findDuplicateNames.bpmn")
  void processWithDuplicateNames() {
    expect(() -> findId("Task one"), "not unique");
    expect(() -> findId("Event one"), "not unique");
    expect(() -> findId("Gateway one"), "not unique");
  }

  @Test
  @Deployment(resources = "bpmn/ProcessEngineTests-findDuplicateNamesOnTaskAndGateway.bpmn")
  void processWithDuplicateNamesOnDifferentElementsTypes() {
    expect(() -> findId("Element one"), "not unique");
  }

  @Test
  @Deployment(resources = "bpmn/ProcessEngineTests-findDuplicateNamesOnTaskAndGateway.bpmn")
  void processWithDuplicateNamesBindTheUniqueOnly() {
    // Given
    // Process model with two pools and a mix of duplicate and unique names deployed
    // When
    String startOne = findId("Start one");
    String endTwo = findId("End two");
    // Then
    assertThat(startOne).isEqualTo("StartOne_TestID");
    assertThat(endTwo).isEqualTo("EndTwo_TestID");
  }
}
