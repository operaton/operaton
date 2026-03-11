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
package org.operaton.bpm.engine.test.bpmn.event.end;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.OptimisticLockingException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Joram Barrez
 */
class EndEventTest {

  @RegisterExtension
  protected static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  protected ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RuntimeService runtimeService;
  TaskService taskService;

  @BeforeEach
  void setUp() {
    EndEventTestJavaDelegate.timesCalled.set(0);
  }

  // Test case for ACT-1259
  @Deployment
  @Test
  void testConcurrentEndOfSameProcess() throws Exception {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskWithDelay");
    Task task = taskService.createTaskQuery().singleResult();
    assertThat(task).isNotNull();

    // We will now start two threads that both complete the task.
    // In the process, the task is followed by a delay of one second.
    // This will cause both threads to call the taskService.complete method with enough time,
    // before ending the process. Both threads will now try to end the process
    // and only one should succeed (due to optimistic locking).
    TaskCompleter taskCompleter1 = new TaskCompleter(task.getId());
    TaskCompleter taskCompleter2 = new TaskCompleter(task.getId());

    assertThat(taskCompleter1.isSucceeded()).isFalse();
    assertThat(taskCompleter2.isSucceeded()).isFalse();

    taskCompleter1.start();
    taskCompleter2.start();
    taskCompleter1.join();
    taskCompleter2.join();

    int successCount = 0;
    if (taskCompleter1.isSucceeded()) {
      successCount++;
    }
    if (taskCompleter2.isSucceeded()) {
      successCount++;
    }

    assertThat(successCount).as("(Only) one thread should have been able to successfully end the process").isEqualTo(1);
    testRule.assertProcessEnded(processInstance.getId());
  }

  /** Helper class for concurrent testing */
  class TaskCompleter extends Thread {

    protected String taskId;
    protected boolean succeeded;

    public TaskCompleter(String taskId) {
      this.taskId = taskId;
    }

    public boolean isSucceeded() {
      return succeeded;
    }

    @Override
    public void run() {
      try {
        taskService.complete(taskId);
        succeeded = true;
      } catch (OptimisticLockingException ae) {
        // Exception is expected for one of the threads
      }
    }
  }

}
