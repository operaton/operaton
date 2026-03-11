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
package org.operaton.bpm.engine.cdi.test.api.annotation;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.impl.annotation.CompleteTaskInterceptor;
import org.operaton.bpm.engine.cdi.test.CdiProcessEngineTestCase;
import org.operaton.bpm.engine.cdi.test.impl.beans.DeclarativeProcessController;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testcase for assuring that the {@link CompleteTaskInterceptor} works as
 * expected
 *
 * @author Daniel Meyer
 */
class CompleteTaskTest extends CdiProcessEngineTestCase {

  @Test
  @Deployment(resources = "org/operaton/bpm/engine/cdi/test/api/annotation/CompleteTaskTest.bpmn20.xml")
  void testCompleteTask() {

    BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

    businessProcess.startProcessByKey("keyOfTheProcess");

    Task task = taskService.createTaskQuery().singleResult();

    // associate current unit of work with the task:
    businessProcess.startTask(task.getId());

    getBeanInstance(DeclarativeProcessController.class).completeTask();

    // assert that now the task is completed
    assertThat(taskService.createTaskQuery().singleResult()).isNull();
  }


}
