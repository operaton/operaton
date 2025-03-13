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
package org.operaton.bpm.engine.test.api.authorization;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_INSTANCE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import org.operaton.bpm.engine.runtime.EventSubscription;
import org.operaton.bpm.engine.runtime.EventSubscriptionQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;

/**
 * @author Roman Smirnov
 *
 */
class EventSubscriptionAuthorizationTest extends AuthorizationTest {

  protected static final String ONE_TASK_PROCESS_KEY = "oneTaskProcess";
  protected static final String SIGNAL_BOUNDARY_PROCESS_KEY = "signalBoundaryProcess";

  @Override
  @BeforeEach
  public void setUp() {
    testRule.deploy(
        "org/operaton/bpm/engine/test/api/oneMessageBoundaryEventProcess.bpmn20.xml",
        "org/operaton/bpm/engine/test/api/authorization/signalBoundaryEventProcess.bpmn20.xml");
    super.setUp();
  }

  @Test
  void testSimpleQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    // when
    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleQueryWithReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    // then
    verifyQueryResults(query, 1);

    EventSubscription eventSubscription = query.singleResult();
    assertThat(eventSubscription).isNotNull();
    assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testSimpleQueryWithReadPermissionOnAnyProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    // then
    verifyQueryResults(query, 1);

    EventSubscription eventSubscription = query.singleResult();
    assertThat(eventSubscription).isNotNull();
    assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testSimpleQueryWithMultiple() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleQueryWithReadInstancesPermissionOnOneTaskProcess() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_INSTANCE);

    // when
    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    // then
    verifyQueryResults(query, 1);

    EventSubscription eventSubscription = query.singleResult();
    assertThat(eventSubscription).isNotNull();
    assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testSimpleQueryWithReadInstancesPermissionOnAnyProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE);

    // when
    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    // then
    verifyQueryResults(query, 1);

    EventSubscription eventSubscription = query.singleResult();
    assertThat(eventSubscription).isNotNull();
    assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);

    // when
    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testQueryWithReadPermissionOnProcessInstance() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    String processInstanceId = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY).getId();

    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, userId, READ);

    // when
    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    // then
    verifyQueryResults(query, 1);

    EventSubscription eventSubscription = query.singleResult();
    assertThat(eventSubscription).isNotNull();
    assertThat(eventSubscription.getProcessInstanceId()).isEqualTo(processInstanceId);
  }

  @Test
  void testQueryWithReadPermissionOnAnyProcessInstance() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);

    // when
    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    // then
    verifyQueryResults(query, 7);
  }

  @Test
  void testQueryWithReadInstancesPermissionOnOneTaskProcess() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ONE_TASK_PROCESS_KEY, userId, READ_INSTANCE);

    // when
    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    // then
    verifyQueryResults(query, 3);
  }

  @Test
  void testQueryWithReadInstancesPermissionOnAnyProcessDefinition() {
    // given
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);

    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_DEFINITION, ANY, userId, READ_INSTANCE);

    // when
    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    // then
    verifyQueryResults(query, 7);
  }

  @Test
  void shouldNotFindSubscriptionWithRevokedReadPermissionOnAnyProcessInstance() {
    // given
    ProcessInstance instance1 = startProcessInstanceByKey(ONE_TASK_PROCESS_KEY);
    ProcessInstance instance2 = startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);

    createGrantAuthorization(PROCESS_INSTANCE, ANY, userId, READ);
    createRevokeAuthorization(PROCESS_INSTANCE, instance1.getId(), userId, READ);
    createRevokeAuthorization(PROCESS_INSTANCE, instance2.getId(), userId, READ);

    // when
    EventSubscriptionQuery query = runtimeService.createEventSubscriptionQuery();

    // then
    verifyQueryResults(query, 0);
  }
}
