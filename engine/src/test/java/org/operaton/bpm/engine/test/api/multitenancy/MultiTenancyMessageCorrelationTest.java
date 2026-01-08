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
package org.operaton.bpm.engine.test.api.multitenancy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.MismatchingMessageCorrelationException;
import org.operaton.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.task.Task;
import org.operaton.bpm.engine.task.TaskQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

class MultiTenancyMessageCorrelationTest {

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

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @Test
  void correlateMessageToStartEventNoTenantIdSetForNonTenant() {
    testRule.deploy(MESSAGE_START_PROCESS);

    engineRule.getRuntimeService().createMessageCorrelation("message").correlate();

    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.count()).isOne();
    assertThat(query.singleResult().getTenantId()).isNull();
  }

  @Test
  void correlateMessageToStartEventNoTenantIdSetForTenant() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);

    engineRule.getRuntimeService().createMessageCorrelation("message").correlate();

    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void correlateMessageToStartEventWithoutTenantId() {
    testRule.deploy(MESSAGE_START_PROCESS);
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);

    engineRule.getRuntimeService().createMessageCorrelation("message")
      .withoutTenantId()
      .correlate();

    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.count()).isOne();
    assertThat(query.singleResult().getTenantId()).isNull();
  }

  @Test
  void correlateMessageToStartEventWithTenantId() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_START_PROCESS);

    engineRule.getRuntimeService().createMessageCorrelation("message")
      .tenantId(TENANT_ONE)
      .correlate();

    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  void correlateMessageToIntermediateCatchEventNoTenantIdSetForNonTenant() {
    testRule.deploy(MESSAGE_CATCH_PROCESS);

    engineRule.getRuntimeService().startProcessInstanceByKey("messageCatch");

    engineRule.getRuntimeService().createMessageCorrelation("message").correlate();

    TaskQuery query = engineRule.getTaskService().createTaskQuery();
    assertThat(query.count()).isOne();
  }

  @Test
  void correlateMessageToIntermediateCatchEventNoTenantIdSetForTenant() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);

    engineRule.getRuntimeService().startProcessInstanceByKey("messageCatch");

    engineRule.getRuntimeService().createMessageCorrelation("message").correlate();

    TaskQuery query = engineRule.getTaskService().createTaskQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void correlateMessageToIntermediateCatchEventWithoutTenantId() {
    testRule.deploy(MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);

    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionWithoutTenantId().execute();
    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();

    engineRule.getRuntimeService().createMessageCorrelation("message")
      .withoutTenantId()
      .correlate();

    TaskQuery query = engineRule.getTaskService().createTaskQuery();
    assertThat(query.count()).isOne();
    assertThat(query.singleResult().getTenantId()).isNull();
  }

  @Test
  void correlateMessageToIntermediateCatchEventWithTenantId() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_CATCH_PROCESS);

    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();
    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_TWO).execute();

    engineRule.getRuntimeService().createMessageCorrelation("message")
      .tenantId(TENANT_ONE)
      .correlate();

    TaskQuery query = engineRule.getTaskService().createTaskQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  void correlateMessageToStartAndIntermediateCatchEventWithoutTenantId() {
    testRule.deploy(MESSAGE_START_PROCESS, MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS, MESSAGE_CATCH_PROCESS);

    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionWithoutTenantId().execute();

    engineRule.getRuntimeService().createMessageCorrelation("message")
      .withoutTenantId()
      .correlateAll();

    List<Task> tasks = engineRule.getTaskService().createTaskQuery().list();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getTenantId()).isNull();
    assertThat(tasks.get(1).getTenantId()).isNull();
  }

  @Test
  void correlateMessageToStartAndIntermediateCatchEventWithTenantId() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS, MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_START_PROCESS, MESSAGE_CATCH_PROCESS);

    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();
    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_TWO).execute();

    engineRule.getRuntimeService().createMessageCorrelation("message")
      .tenantId(TENANT_ONE)
      .correlateAll();

    TaskQuery query = engineRule.getTaskService().createTaskQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  void correlateMessageToMultipleIntermediateCatchEventsWithoutTenantId() {
    testRule.deploy(MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);

    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionWithoutTenantId().execute();
    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionWithoutTenantId().execute();

    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();
    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();

    engineRule.getRuntimeService().createMessageCorrelation("message")
      .withoutTenantId()
      .correlateAll();

    List<Task> tasks = engineRule.getTaskService().createTaskQuery().list();
    assertThat(tasks).hasSize(2);
    assertThat(tasks.get(0).getTenantId()).isNull();
    assertThat(tasks.get(1).getTenantId()).isNull();
  }

  @Test
  void correlateMessageToMultipleIntermediateCatchEventsWithTenantId() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_CATCH_PROCESS);

    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();
    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();

    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_TWO).execute();
    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_TWO).execute();

    engineRule.getRuntimeService().createMessageCorrelation("message")
      .tenantId(TENANT_ONE)
      .correlateAll();

    TaskQuery query = engineRule.getTaskService().createTaskQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  void correlateStartMessageWithoutTenantId() {
    testRule.deploy(MESSAGE_START_PROCESS);
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);

    engineRule.getRuntimeService().createMessageCorrelation("message")
      .withoutTenantId()
      .correlateStartMessage();

    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.count()).isOne();
    assertThat(query.singleResult().getTenantId()).isNull();
  }

  @Test
  void correlateStartMessageWithTenantId() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_START_PROCESS);

    engineRule.getRuntimeService().createMessageCorrelation("message")
      .tenantId(TENANT_ONE)
      .correlateStartMessage();

    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  void correlateMessagesToStartEventsForMultipleTenants() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_START_PROCESS);

    engineRule.getRuntimeService().createMessageCorrelation("message").correlateAll();

    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void correlateMessagesToIntermediateCatchEventsForMultipleTenants() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_CATCH_PROCESS);

    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();
    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_TWO).execute();

    engineRule.getRuntimeService().createMessageCorrelation("message").correlateAll();

    TaskQuery query = engineRule.getTaskService().createTaskQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void correlateMessagesToStartAndIntermediateCatchEventForMultipleTenants() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_CATCH_PROCESS);

    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_TWO).execute();

    engineRule.getRuntimeService().createMessageCorrelation("message").correlateAll();

    assertThat(engineRule.getRuntimeService().createProcessInstanceQuery().tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(engineRule.getTaskService().createTaskQuery().tenantIdIn(TENANT_TWO).count()).isOne();
  }

  public void failToCorrelateMessageToIntermediateCatchEventsForMultipleTenants() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_CATCH_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_CATCH_PROCESS);

    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_ONE).execute();
    engineRule.getRuntimeService().createProcessInstanceByKey("messageCatch").processDefinitionTenantId(TENANT_TWO).execute();

    var messageCorrelationBuilder = engineRule.getRuntimeService().createMessageCorrelation("message");

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlate)
      .isInstanceOf(MismatchingMessageCorrelationException.class)
      .hasMessageContaining("Cannot correlate a message with name 'message' to a single execution");
  }

  @Test
  void testSubscriptionsWhenDeletingGroupsProcessDefinitionsByIds() {
    // given
    String processDefId1 = testRule.deployForTenantAndGetDefinition(TENANT_ONE, MESSAGE_START_PROCESS).getId();
    String processDefId2 = testRule.deployForTenantAndGetDefinition(TENANT_ONE, MESSAGE_START_PROCESS).getId();
    String processDefId3 = testRule.deployForTenantAndGetDefinition(TENANT_ONE, MESSAGE_START_PROCESS).getId();

    @SuppressWarnings("unused")
    String processDefId4 = testRule.deployAndGetDefinition(MESSAGE_START_PROCESS).getId();
    String processDefId5 = testRule.deployAndGetDefinition(MESSAGE_START_PROCESS).getId();
    String processDefId6 = testRule.deployAndGetDefinition(MESSAGE_START_PROCESS).getId();

    BpmnModelInstance processAnotherKey = Bpmn.createExecutableProcess("anotherKey")
        .startEvent()
          .message("sophisticated message")
        .userTask()
        .endEvent()
        .done();

    String processDefId7 = testRule.deployForTenantAndGetDefinition(TENANT_ONE, processAnotherKey).getId();
    String processDefId8 = testRule.deployForTenantAndGetDefinition(TENANT_ONE, processAnotherKey).getId();
    String processDefId9 = testRule.deployForTenantAndGetDefinition(TENANT_ONE, processAnotherKey).getId();

    // assume
    assertThat(engineRule.getRuntimeService().createEventSubscriptionQuery().count()).isEqualTo(3);

    // when
    engineRule.getRepositoryService()
              .deleteProcessDefinitions()
              .byIds(processDefId8, processDefId5, processDefId3, processDefId9, processDefId1)
              .delete();

    // then
    List<EventSubscription> list = engineRule.getRuntimeService().createEventSubscriptionQuery().list();
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

  @Test
  void failToCorrelateMessageToStartEventsForMultipleTenants() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_START_PROCESS);

    var messageCorrelationBuilder = engineRule.getRuntimeService().createMessageCorrelation("message");

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlate)
      .isInstanceOf(MismatchingMessageCorrelationException.class)
      .hasMessageContaining("Cannot correlate a message with name 'message' to a single process definition");
  }

  @Test
  void failToCorrelateStartMessageForMultipleTenants() {
    testRule.deployForTenant(TENANT_ONE, MESSAGE_START_PROCESS);
    testRule.deployForTenant(TENANT_TWO, MESSAGE_START_PROCESS);

    var messageCorrelationBuilder = engineRule.getRuntimeService().createMessageCorrelation("message");

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlateStartMessage)
      .isInstanceOf(MismatchingMessageCorrelationException.class)
      .hasMessageContaining("Cannot correlate a message with name 'message' to a single process definition");

  }

  @Test
  void failToCorrelateMessageByProcessInstanceIdWithoutTenantId() {
    // given
    var messageCorrelationBuilder = engineRule.getRuntimeService().createMessageCorrelation("message")
      .processInstanceId("id")
      .withoutTenantId();

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlate)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot specify a tenant-id");
  }

  @Test
  void failToCorrelateMessageByProcessInstanceIdAndTenantId() {
    // given
    var messageCorrelationBuilder = engineRule.getRuntimeService().createMessageCorrelation("message")
      .processInstanceId("id")
      .tenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlate)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot specify a tenant-id");
  }

  @Test
  void failToCorrelateMessageByProcessDefinitionIdWithoutTenantId() {
    // given
    var messageCorrelationBuilder = engineRule.getRuntimeService().createMessageCorrelation("message")
      .processDefinitionId("id")
      .withoutTenantId();

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlateStartMessage)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot specify a tenant-id");
  }

  @Test
  void failToCorrelateMessageByProcessDefinitionIdAndTenantId() {
    // given
    var messageCorrelationBuilder = engineRule.getRuntimeService().createMessageCorrelation("message")
      .processDefinitionId("id")
      .tenantId(TENANT_ONE);

    // when/then
    assertThatThrownBy(messageCorrelationBuilder::correlateStartMessage)
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Cannot specify a tenant-id");
  }

}
