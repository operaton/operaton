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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.ProcessEngineRule;
import org.operaton.bpm.engine.test.util.ProcessEngineTestRule;
import org.operaton.bpm.engine.test.util.ProvidedProcessEngineRule;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

public class MultiTenancyMessageEventReceivedCmdTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final BpmnModelInstance MESSAGE_CATCH_PROCESS = Bpmn.createExecutableProcess("messageCatch")
      .startEvent()
      .intermediateCatchEvent()
        .message("message")
      .userTask()
      .endEvent()
      .done();

  protected ProcessEngineRule engineRule = new ProvidedProcessEngineRule();
  protected ProcessEngineTestRule testRule = new ProcessEngineTestRule(engineRule);

  @Rule
  public RuleChain ruleChain = RuleChain.outerRule(engineRule).around(testRule);

  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected IdentityService identityService;

  @Before
  public void setUp() {
    runtimeService = engineRule.getRuntimeService();
    taskService = engineRule.getTaskService();
    identityService = engineRule.getIdentityService();
  }

  @Test
  public void correlateReceivedMessageToIntermediateCatchEventNoAuthenticatedTenants() {
    testRule.deploy(MESSAGE_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("messageCatch").execute();

    Execution execution = runtimeService.createExecutionQuery()
      .processDefinitionKey("messageCatch")
      .messageEventSubscriptionName("message")
      .singleResult();

    identityService.setAuthentication("user", null, null);

    runtimeService.messageEventReceived("message", execution.getId());

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isZero();
    assertThat(taskService.createTaskQuery().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  public void correlateReceivedMessageToIntermediateCatchEventWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("messageCatch").execute();

    Execution execution = runtimeService.createExecutionQuery()
      .processDefinitionKey("messageCatch")
      .messageEventSubscriptionName("message")
      .singleResult();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    runtimeService.messageEventReceived("message", execution.getId());

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  public void correlateReceivedMessageToIntermediateCatchEventDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_CATCH_PROCESS);

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_TWO).execute();

    Execution execution = runtimeService.createExecutionQuery()
      .processDefinitionKey("messageCatch")
      .messageEventSubscriptionName("message")
      .tenantIdIn(TENANT_ONE)
      .singleResult();

    runtimeService.messageEventReceived("message", execution.getId());

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  public void failToCorrelateReceivedMessageToIntermediateCatchEventNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();

    Execution execution = runtimeService.createExecutionQuery()
      .processDefinitionKey("messageCatch")
      .messageEventSubscriptionName("message")
      .tenantIdIn(TENANT_ONE)
      .singleResult();
    String executionId = execution.getId();

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> runtimeService.messageEventReceived("message", executionId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process instance");

  }

}
