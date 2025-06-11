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
package org.operaton.bpm.engine.test.api.multitenancy.query.history;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.history.HistoricProcessInstance;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author Askar Akhmerov
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_AUDIT)
public class MultiTenancyHistoricProcessInstanceStateTest {
  public static final String PROCESS_ID = "process1";
  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  protected static ProcessEngineExtension processEngineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension processEngineTestRule = new ProcessEngineTestExtension(processEngineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected IdentityService identityService;

  @Test
  void testSuspensionWithTenancy() {
    BpmnModelInstance instance = Bpmn.createExecutableProcess(PROCESS_ID)
        .startEvent()
        .userTask()
        .endEvent()
        .done();
    ProcessDefinition processDefinition = processEngineTestRule.deployAndGetDefinition(instance);
    ProcessDefinition processDefinition1 = processEngineTestRule.deployForTenantAndGetDefinition(TENANT_ONE, instance);
    ProcessDefinition processDefinition2 = processEngineTestRule.deployForTenantAndGetDefinition(TENANT_TWO, instance);

    ProcessInstance processInstance = processEngineRule.getRuntimeService()
        .startProcessInstanceById(processDefinition.getId());
    ProcessInstance processInstance1 = processEngineRule.getRuntimeService()
        .startProcessInstanceById(processDefinition1.getId());
    ProcessInstance processInstance2 = processEngineRule.getRuntimeService()
        .startProcessInstanceById(processDefinition2.getId());

    //suspend Tenant one
    processEngineRule.getRuntimeService().updateProcessInstanceSuspensionState()
        .byProcessDefinitionKey(processDefinition1.getKey())
        .processDefinitionTenantId(processDefinition1.getTenantId()).suspend();

    String[] processInstances = {
        processInstance1.getId(),
        processInstance2.getId(),
        processInstance.getId()};

    verifyStates(processInstances,
        new String[]{
            HistoricProcessInstance.STATE_SUSPENDED,
            HistoricProcessInstance.STATE_ACTIVE,
            HistoricProcessInstance.STATE_ACTIVE});


    //suspend without tenant
    processEngineRule.getRuntimeService().updateProcessInstanceSuspensionState()
        .byProcessDefinitionKey(processDefinition.getKey())
        .processDefinitionWithoutTenantId().suspend();

    verifyStates(processInstances,
        new String[]{
            HistoricProcessInstance.STATE_SUSPENDED,
            HistoricProcessInstance.STATE_ACTIVE,
            HistoricProcessInstance.STATE_SUSPENDED});

    //reactivate without tenant
    processEngineRule.getRuntimeService().updateProcessInstanceSuspensionState()
        .byProcessDefinitionKey(processDefinition.getKey())
        .processDefinitionWithoutTenantId().activate();


    verifyStates(processInstances,
        new String[]{
            HistoricProcessInstance.STATE_SUSPENDED,
            HistoricProcessInstance.STATE_ACTIVE,
            HistoricProcessInstance.STATE_ACTIVE});
  }

  protected void verifyStates(String[] processInstances, String[] states) {
    for (int i = 0; i < processInstances.length; i++) {
      assertThat(
          processEngineRule.getHistoryService().createHistoricProcessInstanceQuery()
              .processInstanceId(processInstances[i]).singleResult().getState()).isEqualTo(states[i]);
    }
  }
}
