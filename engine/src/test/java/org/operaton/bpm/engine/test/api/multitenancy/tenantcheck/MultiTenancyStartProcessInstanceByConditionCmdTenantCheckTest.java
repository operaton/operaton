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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.event.EventType;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

class MultiTenancyStartProcessInstanceByConditionCmdTenantCheckTest {
  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final BpmnModelInstance PROCESS = Bpmn.createExecutableProcess("conditionStart")
      .startEvent()
        .conditionalEventDefinition()
          .condition("${true}")
        .conditionalEventDefinitionDone()
      .userTask()
      .endEvent()
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  public IdentityService identityService;
  public RepositoryService repositoryService;
  public RuntimeService runtimeService;

  @Test
  void testNoAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);
    testRule.deploy(PROCESS);

    ensureEventSubscriptions(3);

    identityService.setAuthentication("user", null, null);

    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("foo", "bar");

    // when
    List<ProcessInstance> instances = engineRule.getRuntimeService()
      .createConditionEvaluation()
      .setVariables(variableMap)
      .evaluateStartConditions();

    // then
    assertThat(instances)
            .isNotNull()
            .hasSize(1);

    identityService.clearAuthentication();

    ProcessInstanceQuery processInstanceQuery = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(processInstanceQuery.count()).isOne();
    assertThat(processInstanceQuery.withoutTenantId().count()).isOne();
  }

  @Test
  void testWithAuthenticatedTenant() {
    // given
    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    ensureEventSubscriptions(2);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("foo", "bar");

    // when
    List<ProcessInstance> processInstances = engineRule.getRuntimeService()
      .createConditionEvaluation()
      .setVariables(variableMap)
      .tenantId(TENANT_ONE)
      .evaluateStartConditions();

    // then
    assertThat(processInstances)
            .isNotNull()
            .hasSize(1);

    identityService.clearAuthentication();

    ProcessInstanceQuery processInstanceQuery = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(processInstanceQuery.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(processInstanceQuery.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  void testWithAuthenticatedTenant2() {
    // given
    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    ensureEventSubscriptions(2);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("foo", "bar");

    // when
    List<ProcessInstance> processInstances = engineRule.getRuntimeService()
      .createConditionEvaluation()
      .setVariables(variableMap)
      .evaluateStartConditions();

    // then
    assertThat(processInstances)
            .isNotNull()
            .hasSize(1);

    identityService.clearAuthentication();

    ProcessInstanceQuery processInstanceQuery = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(processInstanceQuery.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(processInstanceQuery.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  void testDisabledTenantCheck() {
    // given
    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    ensureEventSubscriptions(2);

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    Map<String, Object> variableMap = new HashMap<>();
    variableMap.put("foo", "bar");

    // when
    List<ProcessInstance> evaluateStartConditions = engineRule.getRuntimeService()
      .createConditionEvaluation()
      .setVariables(variableMap)
      .evaluateStartConditions();
    assertThat(evaluateStartConditions).hasSize(2);

    identityService.clearAuthentication();
  }

  @Test
  void testFailToEvaluateConditionByProcessDefinitionIdNoAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    ensureEventSubscriptions(1);

    ProcessDefinition processDefinition = engineRule.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey("conditionStart").singleResult();

    identityService.setAuthentication("user", null, null);

    var conditionEvaluationBuilder = engineRule.getRuntimeService()
      .createConditionEvaluation()
      .setVariable("foo", "bar")
      .processDefinitionId(processDefinition.getId());

    // when/then
    assertThatThrownBy(conditionEvaluationBuilder::evaluateStartConditions)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot create an instance of the process definition");
  }

  @Test
  void testEvaluateConditionByProcessDefinitionIdWithAuthenticatedTenants() {
    // given
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    ensureEventSubscriptions(1);

    ProcessDefinition processDefinition = engineRule.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey("conditionStart").singleResult();

    identityService = engineRule.getIdentityService();
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    // when
    List<ProcessInstance> instances = engineRule.getRuntimeService()
      .createConditionEvaluation()
      .setVariable("foo", "bar")
      .tenantId(TENANT_ONE)
      .processDefinitionId(processDefinition.getId())
      .evaluateStartConditions();

    // then
    assertThat(instances)
            .isNotNull()
            .hasSize(1);
    assertThat(instances.get(0).getTenantId()).isEqualTo(TENANT_ONE);

    identityService.clearAuthentication();

    ProcessInstanceQuery processInstanceQuery = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(processInstanceQuery.tenantIdIn(TENANT_ONE).count()).isOne();

    EventSubscription eventSubscription = engineRule.getRuntimeService().createEventSubscriptionQuery().singleResult();
    assertThat(eventSubscription.getEventType()).isEqualTo(EventType.CONDITONAL.name());
  }

  @Test
  void testSubscriptionsWhenDeletingGroupsProcessDefinitionsByIds() {
    // given
    String processDefId1 = testRule.deployForTenantAndGetDefinition(TENANT_ONE, PROCESS).getId();
    String processDefId2 = testRule.deployForTenantAndGetDefinition(TENANT_ONE, PROCESS).getId();
    String processDefId3 = testRule.deployForTenantAndGetDefinition(TENANT_ONE, PROCESS).getId();

    @SuppressWarnings("unused")
    String processDefId4 = testRule.deployAndGetDefinition(PROCESS).getId();
    String processDefId5 = testRule.deployAndGetDefinition(PROCESS).getId();
    String processDefId6 = testRule.deployAndGetDefinition(PROCESS).getId();

    BpmnModelInstance processAnotherKey = Bpmn.createExecutableProcess("anotherKey")
        .startEvent()
          .conditionalEventDefinition()
            .condition("${true}")
          .conditionalEventDefinitionDone()
        .userTask()
        .endEvent()
        .done();

    String processDefId7 = testRule.deployForTenantAndGetDefinition(TENANT_ONE, processAnotherKey).getId();
    String processDefId8 = testRule.deployForTenantAndGetDefinition(TENANT_ONE, processAnotherKey).getId();
    String processDefId9 = testRule.deployForTenantAndGetDefinition(TENANT_ONE, processAnotherKey).getId();

    // assume
    assertThat(runtimeService.createEventSubscriptionQuery().count()).isEqualTo(3);

    // when
    repositoryService.deleteProcessDefinitions()
                     .byIds(processDefId8, processDefId5, processDefId3, processDefId9, processDefId1)
                     .delete();

    // then
    List<EventSubscription> list = runtimeService.createEventSubscriptionQuery().list();
    assertThat(list).hasSize(3);
    for (EventSubscription eventSubscription : list) {
      EventSubscriptionEntity eventSubscriptionEntity = (EventSubscriptionEntity) eventSubscription;
      if (eventSubscriptionEntity.getConfiguration().equals(processDefId2)) {
        assertThat(eventSubscription.getTenantId()).isEqualTo(TENANT_ONE);
      } else if (eventSubscriptionEntity.getConfiguration().equals(processDefId6)) {
        assertThat(eventSubscription.getTenantId()).isNull();
      } else if (eventSubscriptionEntity.getConfiguration().equals(processDefId7)) {
        assertThat(eventSubscription.getTenantId()).isEqualTo(TENANT_ONE);
      } else {
        fail("This process definition '%s' and the respective event subscription should not exist.".formatted(eventSubscriptionEntity.getConfiguration()));
      }
    }
  }

  protected void ensureEventSubscriptions(int count) {
    List<EventSubscription> eventSubscriptions = engineRule.getRuntimeService().createEventSubscriptionQuery().list();
    assertThat(eventSubscriptions).hasSize(count);
    for (EventSubscription eventSubscription : eventSubscriptions) {
      assertThat(eventSubscription.getEventType()).isEqualTo(EventType.CONDITONAL.name());
    }
  }
}
