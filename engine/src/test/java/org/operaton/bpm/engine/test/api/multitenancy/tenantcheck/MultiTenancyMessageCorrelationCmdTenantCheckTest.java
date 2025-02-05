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
import org.operaton.bpm.engine.MismatchingMessageCorrelationException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
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

public class MultiTenancyMessageCorrelationCmdTenantCheckTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final BpmnModelInstance MESSAGE_START_PROCESS = Bpmn.createExecutableProcess("messageStart")
      .startEvent()
        .message("message")
      .userTask()
      .endEvent()
      .done();

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
  public void correlateMessageToStartEventNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_START_PROCESS);
    testRule.deploy(MESSAGE_START_PROCESS);

    identityService.setAuthentication("user", null, null);

    runtimeService.createMessageCorrelation("message")
      .correlateStartMessage();

    identityService.clearAuthentication();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  public void correlateMessageToStartEventWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_START_PROCESS);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    runtimeService.createMessageCorrelation("message")
      .correlateStartMessage();

    identityService.clearAuthentication();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  public void correlateMessageToStartEventDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_START_PROCESS);

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    runtimeService.createMessageCorrelation("message")
      .tenantId(TENANT_ONE)
      .correlateStartMessage();

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  public void correlateMessageToIntermediateCatchEventNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_CATCH_PROCESS);
    testRule.deploy(MESSAGE_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_TWO).execute();
    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionWithoutTenantId().execute();

    identityService.setAuthentication("user", null, null);

    runtimeService.createMessageCorrelation("message")
      .correlate();

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(taskService.createTaskQuery().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  public void correlateMessageToIntermediateCatchEventWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_TWO).execute();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    runtimeService.createMessageCorrelation("message")
      .correlate();

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  public void correlateMessageToIntermediateCatchEventDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_TWO).execute();

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    runtimeService.createMessageCorrelation("message")
      .tenantId(TENANT_ONE)
      .correlate();

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  public void correlateMessageToStartAndIntermediateCatchEventWithNoAuthenticatedTenants() {
    testRule.deploy(MESSAGE_START_PROCESS, MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS, MESSAGE_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionWithoutTenantId().execute();
    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();

    identityService.setAuthentication("user", null, null);

    runtimeService.createMessageCorrelation("message")
      .correlateAll();

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isZero();
    assertThat(taskService.createTaskQuery().withoutTenantId().count()).isEqualTo(2L);
  }

  @Test
  public void correlateMessageToStartAndIntermediateCatchEventWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_TWO, MESSAGE_START_PROCESS, MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS, MESSAGE_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_TWO).execute();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    runtimeService.createMessageCorrelation("message")
      .correlateAll();

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  public void correlateMessageToStartAndIntermediateCatchEventDisabledTenantCheck() {
    testRule.deployForTenant(TENANT_TWO, MESSAGE_START_PROCESS, MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS, MESSAGE_CATCH_PROCESS);

    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_TWO).execute();

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    runtimeService.createMessageCorrelation("message")
      .correlateAll();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isEqualTo(4L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isEqualTo(2L);
  }

  @Test
  public void failToCorrelateMessageByProcessInstanceIdNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);

    ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("messageCatch")
        .processDefinitionTenantId(TENANT_ONE).execute();

    identityService.setAuthentication("user", null, null);
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation("message")
      .processInstanceId(processInstance.getId());

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlate)
      .isInstanceOf(MismatchingMessageCorrelationException.class)
      .hasMessageContaining("Cannot correlate message");
  }

  @Test
  public void correlateMessageByProcessInstanceIdWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);

    ProcessInstance processInstance = runtimeService.createProcessInstanceByKey("messageCatch").execute();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    runtimeService.createMessageCorrelation("message")
      .processInstanceId(processInstance.getId())
      .correlate();

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  public void failToCorrelateMessageByProcessDefinitionIdNoAuthenticatedTenants() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);

    ProcessDefinition processDefinition = engineRule.getRepositoryService().createProcessDefinitionQuery().
        processDefinitionKey("messageStart").tenantIdIn(TENANT_ONE).singleResult();

    identityService.setAuthentication("user", null, null);
    var messageCorrelationBuilder = runtimeService.createMessageCorrelation("message")
      .processDefinitionId(processDefinition.getId());

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlateStartMessage)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot create an instance of the process definition");

  }

  @Test
  public void correlateMessageByProcessDefinitionIdWithAuthenticatedTenant() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);

    ProcessDefinition processDefinition = engineRule.getRepositoryService().createProcessDefinitionQuery().
        processDefinitionKey("messageStart").singleResult();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    runtimeService.createMessageCorrelation("message")
      .processDefinitionId(processDefinition.getId())
      .correlateStartMessage();

    identityService.clearAuthentication();

    TaskQuery query = taskService.createTaskQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

}
