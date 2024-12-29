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
package org.operaton.bpm.engine.cdi.test.impl.task;

import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
class CdiTaskServiceTest extends CdiProcessEngineTestCase {

  @Test
  void claimTask() {
    Task newTask = taskService.newTask();
    taskService.saveTask(newTask);
    taskService.claim(newTask.getId(), "kermit");
    taskService.deleteTask(newTask.getId(),true);
  }

  @Test
  @Deployment
  void taskAssigneeExpression() {
    // given
    runtimeService.startProcessInstanceByKey("taskTest");
    identityService.setAuthenticatedUserId("user");

    // when
    taskService.createTaskQuery().taskAssigneeExpression("${currentUser()}").list();

    // then no exception is thrown
  }

}
