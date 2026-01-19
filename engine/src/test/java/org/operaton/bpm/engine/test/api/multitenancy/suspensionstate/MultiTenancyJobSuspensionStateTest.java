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
package org.operaton.bpm.engine.test.api.multitenancy.suspensionstate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

class MultiTenancyJobSuspensionStateTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String PROCESS_DEFINITION_KEY = "testProcess";

  protected static final BpmnModelInstance PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
      .startEvent()
        .operatonAsyncBefore()
      .endEvent()
      .done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  @BeforeEach
  void setUp() {

    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);
    testRule.deploy(PROCESS);

    engineRule.getRuntimeService().createProcessInstanceByKey(PROCESS_DEFINITION_KEY).processDefinitionTenantId(TENANT_ONE).execute();
    engineRule.getRuntimeService().createProcessInstanceByKey(PROCESS_DEFINITION_KEY).processDefinitionTenantId(TENANT_TWO).execute();
    engineRule.getRuntimeService().createProcessInstanceByKey(PROCESS_DEFINITION_KEY).processDefinitionWithoutTenantId().execute();
  }

  @Test
  void suspendAndActivateJobsForAllTenants() {
    // given activated jobs
    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // first suspend
    engineRule.getManagementService()
      .updateJobSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    // then activate
    engineRule.getManagementService()
      .updateJobSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .activate();

    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();
  }

  @Test
  void suspendJobForTenant() {
    // given activated jobs
    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getManagementService()
      .updateJobSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isOne();
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void suspendJobsForNonTenant() {
    // given activated jobs
    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getManagementService()
      .updateJobSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isOne();
    assertThat(query.suspended().withoutTenantId().count()).isOne();
  }

  @Test
  void activateJobsForTenant() {
    // given suspend jobs
    engineRule.getManagementService()
      .updateJobSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.active().count()).isZero();

    engineRule.getManagementService()
      .updateJobSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isOne();
    assertThat(query.active().tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void activateJobsForNonTenant() {
    // given suspend jobs
    engineRule.getManagementService()
      .updateJobSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.active().count()).isZero();

    engineRule.getManagementService()
      .updateJobSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isOne();
    assertThat(query.active().withoutTenantId().count()).isOne();
  }

  @Test
  void suspendJobNoAuthenticatedTenants() {
    // given activated jobs
    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getIdentityService().setAuthentication("user", null, null);

    engineRule.getManagementService()
      .updateJobSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    engineRule.getIdentityService().clearAuthentication();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isOne();
    assertThat(query.suspended().withoutTenantId().count()).isOne();
  }

  @Test
  void suspendJobWithAuthenticatedTenant() {
    // given activated jobs
    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getIdentityService().setAuthentication("user", null, List.of(TENANT_ONE));

    engineRule.getManagementService()
      .updateJobSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    engineRule.getIdentityService().clearAuthentication();

    assertThat(query.active().count()).isOne();
    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().tenantIdIn(TENANT_TWO).count()).isOne();
    assertThat(query.suspended().withoutTenantId().count()).isOne();
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void suspendJobDisabledTenantCheck() {
    // given activated jobs
    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    engineRule.getIdentityService().setAuthentication("user", null, null);

    engineRule.getManagementService()
      .updateJobSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE, TENANT_TWO).includeJobsWithoutTenantId().count()).isEqualTo(3L);
  }

}
