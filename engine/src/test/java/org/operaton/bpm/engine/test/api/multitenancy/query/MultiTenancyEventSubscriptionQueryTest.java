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
package org.operaton.bpm.engine.test.api.multitenancy.query;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.EventSubscriptionQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyEventSubscriptionQueryTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    BpmnModelInstance process = Bpmn.createExecutableProcess("testProcess")
      .startEvent()
        .message("start")
        .userTask()
        .endEvent()
        .done();

    testRule.deploy(process);
    testRule.deployForTenant(TENANT_ONE, process);
    testRule.deployForTenant(TENANT_TWO, process);

    // the deployed process definition contains a message start event
    // - so a message event subscription is created on deployment.
  }

  @Test
  void testQueryNoTenantIdSet() {
    EventSubscriptionQuery query = runtimeService
        .createEventSubscriptionQuery();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByTenantId() {
    EventSubscriptionQuery query = runtimeService
        .createEventSubscriptionQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = runtimeService
        .createEventSubscriptionQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIds() {
    EventSubscriptionQuery query = runtimeService
        .createEventSubscriptionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryBySubscriptionsWithoutTenantId() {
    EventSubscriptionQuery query = runtimeService
        .createEventSubscriptionQuery()
        .withoutTenantId();

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIdsIncludeSubscriptionsWithoutTenantId() {
    EventSubscriptionQuery query = runtimeService
        .createEventSubscriptionQuery()
        .tenantIdIn(TENANT_ONE)
        .includeEventSubscriptionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = runtimeService
        .createEventSubscriptionQuery()
        .tenantIdIn(TENANT_TWO)
        .includeEventSubscriptionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = runtimeService
        .createEventSubscriptionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeEventSubscriptionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByNonExistingTenantId() {
    EventSubscriptionQuery query = runtimeService.
        createEventSubscriptionQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testFailQueryByTenantIdNull() {
    var eventSubscriptionQuery = runtimeService.createEventSubscriptionQuery();

    assertThatThrownBy(() -> eventSubscriptionQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void testQuerySortingAsc() {
    // exclude subscriptions without tenant id because of database-specific ordering
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc()
        .list();

    assertThat(eventSubscriptions).hasSize(2);
    assertThat(eventSubscriptions.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(eventSubscriptions.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testQuerySortingDesc() {
    // exclude subscriptions without tenant id because of database-specific ordering
    List<EventSubscription> eventSubscriptions = runtimeService.createEventSubscriptionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .desc()
        .list();

    assertThat(eventSubscriptions).hasSize(2);
    assertThat(eventSubscriptions.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(eventSubscriptions.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).includeEventSubscriptionsWithoutTenantId().count()).isEqualTo(2L);
  }

  @Test
  void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
  }

  @Test
  void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();
    assertThat(query.count()).isEqualTo(3L);
  }

}
