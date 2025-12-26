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
package org.operaton.bpm.engine.test.api.runtime;

import java.util.List;

import org.junit.jupiter.api.extension.RegisterExtension;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

class VariableInstanceQueryForOracleTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 1000, 1001, 2001})
  void queryGivenNumberOfInstancesActive(int numberOfActiveInstances) {
    // given
    assumeThat("oracle".equals(engineRule.getProcessEngineConfiguration().getDatabaseType()))
      .isTrue();
    RuntimeService runtimeService = engineRule.getRuntimeService();
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);
    String[] ids = new String[numberOfActiveInstances];

    // when
    for (int i = 0; i < numberOfActiveInstances; i++) {
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process",
        Variables.createVariables().putValue("foo", "bar"));
      String activityInstanceId = runtimeService.getActivityInstance(processInstance.getId()).getId();
      ids[i] = activityInstanceId;
    }

    // then
    List<VariableInstance> variables = engineRule.getRuntimeService().createVariableInstanceQuery()
      .activityInstanceIdIn(ids).list();
    assertThat(variables).hasSize(numberOfActiveInstances);
  }
}
