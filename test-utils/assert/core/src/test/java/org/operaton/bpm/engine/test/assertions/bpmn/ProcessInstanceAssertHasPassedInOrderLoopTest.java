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

class ProcessInstanceAssertHasPassedInOrderLoopTest extends ProcessAssertTestCase {

  @Test
  @Deployment(resources = {"bpmn/ProcessInstanceAssert-hasPassedInOrder-loop.bpmn"
  })
  void hasPassedInOrderSeveralActivitiesHistoricInstance() {
    // Given
    final ProcessInstance processInstance = runtimeService().startProcessInstanceByKey(
      "ProcessInstanceAssert-hasPassedInOrder-loop",
      withVariables("exit", false)
    );
    // When
    complete(taskQuery().taskDefinitionKey("UserTask_1").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_2").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_4").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_3").singleResult());
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_4").singleResult(), withVariables("exit", true));
    // And
    complete(taskQuery().taskDefinitionKey("UserTask_5").singleResult());
    // Then
    assertThat(processInstance).hasPassedInOrder("UserTask_1", "UserTask_2", "UserTask_5");
    // And
    assertThat(processInstance).hasPassedInOrder("UserTask_1", "UserTask_3", "UserTask_4", "UserTask_3", "UserTask_4", "UserTask_5");
    // And
    expect(() -> assertThat(processInstance).hasPassedInOrder("UserTask_1", "UserTask_3", "UserTask_4", "UserTask_3", "UserTask_4", "UserTask_3", "UserTask_4", "UserTask_5"));
  }

}
