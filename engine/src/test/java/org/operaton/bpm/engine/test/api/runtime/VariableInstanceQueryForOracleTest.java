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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.engine.test.api.runtime.migration.models.ProcessModels;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.variable.Variables;

class VariableInstanceQueryForOracleTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @Test
  void testQueryWhen0InstancesActive() {
    // given
    assumeTrue("oracle".equals(engineRule.getProcessEngineConfiguration().getDatabaseType()));

    // then
    List<VariableInstance> variables = engineRule.getRuntimeService().createVariableInstanceQuery().list();
    assertThat(variables).isEmpty();
  }

  @Test
  void testQueryWhen1InstanceActive() {
    // given
    assumeTrue("oracle".equals(engineRule.getProcessEngineConfiguration().getDatabaseType()));
    RuntimeService runtimeService = engineRule.getRuntimeService();
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);

    // when
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process",
        Variables.createVariables().putValue("foo", "bar"));
    String activityInstanceId = runtimeService.getActivityInstance(processInstance.getId()).getId();

    // then
    List<VariableInstance> variables = engineRule.getRuntimeService().createVariableInstanceQuery()
        .activityInstanceIdIn(activityInstanceId).list();
    assertThat(variables).hasSize(1);
  }

  @Test
  void testQueryWhen1000InstancesActive() {
    // given
    assumeTrue("oracle".equals(engineRule.getProcessEngineConfiguration().getDatabaseType()));
    RuntimeService runtimeService = engineRule.getRuntimeService();
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);
    String[] ids = new String[1000];

    // when
    for (int i = 0; i < 1000; i++) {
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process",
          Variables.createVariables().putValue("foo", "bar"));
      String activityInstanceId = runtimeService.getActivityInstance(processInstance.getId()).getId();
      ids[i] = activityInstanceId;
    }

    // then
    List<VariableInstance> variables = engineRule.getRuntimeService().createVariableInstanceQuery()
        .activityInstanceIdIn(ids).list();
    assertThat(variables).hasSize(1000);
  }

  @Test
  void testQueryWhen1001InstancesActive() {
    // given
    assumeTrue("oracle".equals(engineRule.getProcessEngineConfiguration().getDatabaseType()));
    RuntimeService runtimeService = engineRule.getRuntimeService();
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);
    String[] ids = new String[1001];

    // when
    for (int i = 0; i < 1001; i++) {
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process",
          Variables.createVariables().putValue("foo", "bar"));
      String activityInstanceId = runtimeService.getActivityInstance(processInstance.getId()).getId();
      ids[i] = activityInstanceId;
    }

    // then
    List<VariableInstance> variables = engineRule.getRuntimeService().createVariableInstanceQuery()
        .activityInstanceIdIn(ids).list();
    assertThat(variables).hasSize(1001);
  }

  @Test
  void testQueryWhen2001InstancesActive() {
    // given
    assumeTrue("oracle".equals(engineRule.getProcessEngineConfiguration().getDatabaseType()));
    RuntimeService runtimeService = engineRule.getRuntimeService();
    testRule.deploy(ProcessModels.TWO_TASKS_PROCESS);
    String[] ids = new String[2001];

    // when
    for (int i = 0; i < 2001; i++) {
      ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Process",
          Variables.createVariables().putValue("foo", "bar"));
      String activityInstanceId = runtimeService.getActivityInstance(processInstance.getId()).getId();
      ids[i] = activityInstanceId;
    }

    // then
    List<VariableInstance> variables = engineRule.getRuntimeService().createVariableInstanceQuery()
        .activityInstanceIdIn(ids).list();
    assertThat(variables).hasSize(2001);
  }
}
