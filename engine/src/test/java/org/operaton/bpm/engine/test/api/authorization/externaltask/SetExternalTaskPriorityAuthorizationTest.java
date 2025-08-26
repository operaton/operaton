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
package org.operaton.bpm.engine.test.api.authorization.externaltask;

import org.junit.jupiter.api.TestTemplate;

import org.operaton.bpm.engine.externaltask.ExternalTask;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Christopher Zell <christopher.zell@camunda.com>
 */
@Parameterized
class SetExternalTaskPriorityAuthorizationTest extends HandleExternalTaskAuthorizationTest {

  @TestTemplate
  @Deployment(resources = "org/operaton/bpm/engine/test/api/externaltask/oneExternalTaskProcess.bpmn20.xml")
  void testSetPriority() {

    // given
    ProcessInstance processInstance = engineRule.getRuntimeService().startProcessInstanceByKey("oneExternalTaskProcess");
    ExternalTask task = engineRule.getExternalTaskService().createExternalTaskQuery().singleResult();

    // when
    authRule
      .init(scenario)
      .withUser("userId")
      .bindResource("processInstanceId", processInstance.getId())
      .bindResource("processDefinitionKey", "oneExternalTaskProcess")
      .start();

    engineRule.getExternalTaskService().setPriority(task.getId(), 5);

    // then
    if (authRule.assertScenario(scenario)) {
      task = engineRule.getExternalTaskService().createExternalTaskQuery().singleResult();
      assertThat(task.getPriority()).isEqualTo(5);
    }
  }
}
