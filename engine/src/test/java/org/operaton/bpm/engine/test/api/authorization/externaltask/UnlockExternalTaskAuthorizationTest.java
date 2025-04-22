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
package org.operaton.bpm.engine.test.api.authorization.externaltask;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.TestTemplate;
import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.externaltask.LockedExternalTask;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;

/**
 * @author Thorben Lindhauer
 *
 */
@Parameterized
class UnlockExternalTaskAuthorizationTest extends HandleExternalTaskAuthorizationTest {

  @TestTemplate
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  void testSetJobPriority() {

    // given
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("oneExternalTaskProcess");
    List<LockedExternalTask> tasks = engineRule.getExternalTaskService()
        .fetchAndLock(5, "workerId")
        .topic("externalTaskTopic", 5000L)
        .execute();

    LockedExternalTask task = tasks.get(0);

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("processInstanceId", processInstance.getId())
      .bindResource("processDefinitionKey", "oneExternalTaskProcess")
      .start();

    engineRule.getExternalTaskService().unlock(task.getId());

    // then
    if (authRule.assertScenario(scenario)) {
      ExternalTask externalTask = engineRule.getExternalTaskService().createExternalTaskQuery().singleResult();
      assertThat(externalTask.getLockExpirationTime()).isNull();
    }
  }

}
