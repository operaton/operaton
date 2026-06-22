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
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyJobQueryTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected ManagementService managementService;
  protected RuntimeService runtimeService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    BpmnModelInstance asyncTaskProcess = Bpmn.createExecutableProcess("testProcess")
      .startEvent()
      .userTask()
        .operatonAsyncBefore()
      .endEvent()
    .done();

   testRule.deploy(asyncTaskProcess);
    testRule.deployForTenant(TENANT_ONE, asyncTaskProcess);
    testRule.deployForTenant(TENANT_TWO, asyncTaskProcess);

    runtimeService.createProcessInstanceByKey("testProcess").processDefinitionWithoutTenantId().execute();
    runtimeService.createProcessInstanceByKey("testProcess").processDefinitionTenantId(TENANT_ONE).execute();
    runtimeService.createProcessInstanceByKey("testProcess").processDefinitionTenantId(TENANT_TWO).execute();
  }

  @Test
  void testQueryNoTenantIdSet() {
    JobQuery query = managementService
        .createJobQuery();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByTenantId() {
    JobQuery query = managementService
        .createJobQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = managementService
        .createJobQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIds() {
    JobQuery query = managementService
        .createJobQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryByJobsWithoutTenantId() {
    JobQuery query = managementService
        .createJobQuery()
        .withoutTenantId();

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIdsIncludeJobsWithoutTenantId() {
    JobQuery query = managementService
        .createJobQuery()
        .tenantIdIn(TENANT_ONE)
        .includeJobsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = managementService
        .createJobQuery()
        .tenantIdIn(TENANT_TWO)
        .includeJobsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = managementService
        .createJobQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeJobsWithoutTenantId();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByNonExistingTenantId() {
    JobQuery query = managementService
        .createJobQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testFailQueryByTenantIdNull() {
    var jobQuery = managementService.createJobQuery();

    assertThatThrownBy(() -> jobQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void testQuerySortingAsc() {
    // exclude jobs without tenant id because of database-specific ordering
    List<Job> jobs = managementService.createJobQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc()
        .list();

    assertThat(jobs).hasSize(2);
    assertThat(jobs.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(jobs.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testQuerySortingDesc() {
    // exclude jobs without tenant id because of database-specific ordering
    List<Job> jobs = managementService.createJobQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .desc()
        .list();

    assertThat(jobs).hasSize(2);
    assertThat(jobs.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(jobs.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    JobQuery query = managementService.createJobQuery();
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    JobQuery query = managementService.createJobQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).includeJobsWithoutTenantId().count()).isEqualTo(2L);
  }

  @Test
  void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    JobQuery query = managementService.createJobQuery();

    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
  }

  @Test
  void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    JobQuery query = managementService.createJobQuery();
    assertThat(query.count()).isEqualTo(3L);
  }

}
