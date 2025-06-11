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
package org.operaton.bpm.engine.test.api.externaltask;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.util.SingleConsumerCondition;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * Tests the signalling of external task conditions
 */
@ExtendWith(ProcessEngineExtension.class)
class ExternalTaskConditionsTest {

  @Mock
  public SingleConsumerCondition condition;

  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected ExternalTaskService externalTaskService;

  private String deploymentId;

  private final BpmnModelInstance testProcess = Bpmn.createExecutableProcess("theProcess")
    .startEvent()
    .serviceTask("theTask")
        .operatonExternalTask("theTopic")
    .done();

  @BeforeEach
  void setUp() {

    MockitoAnnotations.openMocks(this);

    ProcessEngineImpl.EXT_TASK_CONDITIONS.addConsumer(condition);

    deploymentId = repositoryService
        .createDeployment()
        .addModelInstance("process.bpmn", testProcess)
        .deploy()
        .getId();
  }

  @AfterEach
  void tearDown() {

    ProcessEngineImpl.EXT_TASK_CONDITIONS.removeConsumer(condition);

    if (deploymentId != null) {
      repositoryService.deleteDeployment(deploymentId, true);
    }
  }

  @Test
  void shouldSignalConditionOnTaskCreate() {

    // when
    runtimeService
      .startProcessInstanceByKey("theProcess");

    // then
    verify(condition, times(1)).signal();
  }

  @Test
  void shouldSignalConditionOnTaskCreateMultipleTimes() {

    // when
    runtimeService
      .startProcessInstanceByKey("theProcess");
    runtimeService
      .startProcessInstanceByKey("theProcess");

    // then
    verify(condition, times(2)).signal();
  }

  @Test
  void shouldSignalConditionOnUnlock() {

    // given

    runtimeService
      .startProcessInstanceByKey("theProcess");

    reset(condition); // clear signal for create

    LockedExternalTask lockedTask = externalTaskService.fetchAndLock(1, "theWorker")
      .topic("theTopic", 10000)
      .execute()
      .get(0);

    // when
    externalTaskService.unlock(lockedTask.getId());

    // then
    verify(condition, times(1)).signal();
  }

}
