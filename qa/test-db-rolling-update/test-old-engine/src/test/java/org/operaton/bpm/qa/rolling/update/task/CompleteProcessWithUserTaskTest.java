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
package org.operaton.bpm.qa.rolling.update.task;

import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.qa.rolling.update.AbstractRollingUpdateTestCase;
import org.operaton.bpm.qa.rolling.update.RollingUpdateTest;
import org.operaton.bpm.qa.upgrade.ScenarioUnderTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test ensures that the old engine can complete an
 * existing process with user task on the new schema.
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@ScenarioUnderTest("ProcessWithUserTaskScenario")
class CompleteProcessWithUserTaskTest extends AbstractRollingUpdateTestCase {

  @RollingUpdateTest
  @ScenarioUnderTest("init.1")
  void testCompleteProcessWithUserTask() {
    //given an already started process instance
    ProcessInstance oldInstance = rule.processInstance();
    assertThat(oldInstance).isNotNull();

    //which waits on an user task
    TaskService taskService = rule.getTaskService();
    Task userTask = taskService.createTaskQuery().processInstanceId(oldInstance.getId()).singleResult();
    assertThat(userTask).isNotNull();

    //when completing the user task
    taskService.complete(userTask.getId());

    //then there exists no more tasks
    //and the process instance is also completed
    assertThat(rule.taskQuery().count()).isZero();
    rule.assertScenarioEnded();
  }

}
