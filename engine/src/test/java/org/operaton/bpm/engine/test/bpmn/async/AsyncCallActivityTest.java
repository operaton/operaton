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
package org.operaton.bpm.engine.test.bpmn.async;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class AsyncCallActivityTest {

  RuntimeService runtimeService;
  ManagementService managementService;
  TaskService taskService;

  @Deployment(resources = {"org/operaton/bpm/engine/test/bpmn/async/AsyncCallActivityTest.asyncStartEvent.bpmn20.xml",
      "org/operaton/bpm/engine/test/bpmn/async/AsyncCallActivityTest.testCallSubProcess.bpmn20.xml"})
  @Test
  void testCallProcessWithAsyncOnStartEvent() {

    runtimeService.startProcessInstanceByKey("callAsyncSubProcess");

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job).isNotNull();

    managementService.executeJob(job.getId());

    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();
    taskService.complete(task.getId());

  }
}
