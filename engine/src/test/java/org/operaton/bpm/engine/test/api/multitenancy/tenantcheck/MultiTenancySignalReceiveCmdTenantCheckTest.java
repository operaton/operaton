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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.runtime.Execution;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author kristin.polenz
 */
class MultiTenancySignalReceiveCmdTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final BpmnModelInstance SIGNAL_START_PROCESS = Bpmn.createExecutableProcess("signalStart")
      .startEvent()
        .signal("signal")
      .userTask()
      .endEvent()
      .done();

  protected static final BpmnModelInstance SIGNAL_CATCH_PROCESS = Bpmn.createExecutableProcess("signalCatch")
      .startEvent()
      .intermediateCatchEvent()
        .signal("signal")
      .userTask()
      .endEvent()
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected IdentityService identityService;

  @Test
  void sendSignalToStartEventNoAuthenticatedTenants() {
    testRule.deploy(SIGNAL_START_PROCESS);
    testRule.deployForTenant(TENANT_ONE, SIGNAL_START_PROCESS);

    identityService.setAuthentication("user", null, null);

    runtimeService.createSignalEvent("signal").send();

    identityService.clearAuthentication();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isZero();
  }

  @Test
  void sendSignalToStartEventWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, SIGNAL_START_PROCESS);
    testRule.deployForTenant(TENANT_TWO, SIGNAL_START_PROCESS);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    runtimeService.createSignalEvent("signal").send();

    identityService.clearAuthentication();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  void sendSignalToStartEventDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, SIGNAL_START_PROCESS);
    testRule.deployForTenant(TENANT_TWO, SIGNAL_START_PROCESS);

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    runtimeService.createSignalEvent("signal").send();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void sendSignalToIntermediateCatchEventNoAuthenticatedTenants() {
    testRule.deploy(SIGNAL_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_ONE, SIGNAL_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionWithoutTenantId().execute();
    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_ONE).execute();

    identityService.setAuthentication("user", null, null);

    runtimeService.createSignalEvent("signal").send();

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isZero();
    assertThat(taskService.createTaskQuery().withoutTenantId().count()).isOne();
  }

  @Test
  void sendSignalToIntermediateCatchEventWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, SIGNAL_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, SIGNAL_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_TWO).execute();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    runtimeService.createSignalEvent("signal").send();

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  void sendSignalToIntermediateCatchEventDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, SIGNAL_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, SIGNAL_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_TWO).execute();

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    runtimeService.createSignalEvent("signal").send();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void sendSignalToStartAndIntermediateCatchEventNoAuthenticatedTenants() {
    testRule.deploy(SIGNAL_START_PROCESS, SIGNAL_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_ONE, SIGNAL_START_PROCESS, SIGNAL_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionWithoutTenantId().execute();
    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_ONE).execute();

    identityService.setAuthentication("user", null, null);

    runtimeService.createSignalEvent("signal").send();

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isZero();
    assertThat(taskService.createTaskQuery().withoutTenantId().count()).isEqualTo(2L);
  }

  @Test
  void sendSignalToStartAndIntermediateCatchEventWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, SIGNAL_START_PROCESS, SIGNAL_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, SIGNAL_START_PROCESS, SIGNAL_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_TWO).execute();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    runtimeService.createSignalEvent("signal").send();

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  void sendSignalToStartAndIntermediateCatchEventDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, SIGNAL_START_PROCESS, SIGNAL_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, SIGNAL_START_PROCESS, SIGNAL_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_TWO).execute();

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    runtimeService.createSignalEvent("signal").send();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isEqualTo(4L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isEqualTo(2L);
  }

  @Test
  void sendSignalToIntermediateCatchEventWithExecutionIdAndAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, SIGNAL_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_ONE).execute();

    Execution execution = runtimeService.createExecutionQuery()
      .processDefinitionKey("signalCatch")
      .signalEventSubscriptionName("signal")
      .singleResult();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    runtimeService.createSignalEvent("signal").executionId(execution.getId()).send();

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void failToSendSignalToIntermediateCatchEventWithExecutionIdAndNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, SIGNAL_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_ONE).execute();

    Execution execution = runtimeService.createExecutionQuery()
      .processDefinitionKey("signalCatch")
      .signalEventSubscriptionName("signal")
      .singleResult();

    identityService.setAuthentication("user", null, null);
    var signalEventReceivedBuilder = runtimeService.createSignalEvent("signal")
      .executionId(execution.getId());

    // when/then
    assertThatThrownBy(signalEventReceivedBuilder::send)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process instance");

  }

  @Test
  void signalIntermediateCatchEventNoAuthenticatedTenants() {
    testRule.deploy(SIGNAL_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("signalCatch").execute();

    Execution execution = runtimeService.createExecutionQuery()
      .processDefinitionKey("signalCatch")
      .signalEventSubscriptionName("signal")
      .singleResult();

    identityService.setAuthentication("user", null, null);

    runtimeService.signal(execution.getId(), "signal", null, null);

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
  }

  @Test
  void signalIntermediateCatchEventWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, SIGNAL_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_ONE).execute();

    Execution execution = runtimeService.createExecutionQuery()
      .processDefinitionKey("signalCatch")
      .signalEventSubscriptionName("signal")
      .singleResult();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    runtimeService.signal(execution.getId(), "signal", null, null);

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void signalIntermediateCatchEventDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, SIGNAL_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, SIGNAL_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("signalCatch").processDefinitionTenantId(TENANT_TWO).execute();

    Execution execution = runtimeService.createExecutionQuery()
      .processDefinitionKey("signalCatch")
      .signalEventSubscriptionName("signal")
      .tenantIdIn(TENANT_ONE)
      .singleResult();

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    runtimeService.signal(execution.getId(), "signal", null, null);

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isOne();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  void failToSignalIntermediateCatchEventNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, SIGNAL_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("signalCatch").execute();

    Execution execution = runtimeService.createExecutionQuery()
      .processDefinitionKey("signalCatch")
      .signalEventSubscriptionName("signal")
      .singleResult();
    String executionId = execution.getId();

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> runtimeService.signal(executionId, "signal", null, null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process instance");
  }
}
