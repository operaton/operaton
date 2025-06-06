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
package org.operaton.bpm.spring.boot.starter;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestApplication;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestEventCaptor;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
  classes = {TestApplication.class},
  webEnvironment = WebEnvironment.NONE
)
@ActiveProfiles("noeventing")
@Transactional
class OperatonEventingDisabledIT extends AbstractOperatonAutoConfigurationIT {

  private final RuntimeService runtime;
  private final TaskService taskService;
  private final TestEventCaptor eventCaptor;

  @Autowired
  public OperatonEventingDisabledIT(RuntimeService runtime, TaskService taskService, TestEventCaptor eventCaptor) {
      this.runtime = runtime;
      this.taskService = taskService;
      this.eventCaptor = eventCaptor;
  }

  private ProcessInstance instance;

  @BeforeEach
  void init() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey("eventing")
      .singleResult();
    assertThat(processDefinition).isNotNull();

    eventCaptor.historyEvents.clear();
    instance = runtime.startProcessInstanceByKey("eventing");
  }

  @AfterEach
  void stop() {
    if (instance != null) {
      // update stale instance
      instance = runtime.createProcessInstanceQuery().processInstanceId(instance.getProcessInstanceId()).active().singleResult();
      if (instance != null) {
        runtime.deleteProcessInstance(instance.getProcessInstanceId(), "eventing shutdown");
      }
    }
  }

  @Test
  final void shouldEventTaskCreation() {

    Task task = taskService.createTaskQuery().active().singleResult();
    taskService.complete(task.getId());

    assertThat(eventCaptor.taskEvents).isEmpty();
    assertThat(eventCaptor.executionEvents).isEmpty();
    assertThat(eventCaptor.historyEvents).isEmpty();

  }

}
