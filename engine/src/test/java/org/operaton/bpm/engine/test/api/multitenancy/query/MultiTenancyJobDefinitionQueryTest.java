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
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenancyJobDefinitionQueryTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected ManagementService managementService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    BpmnModelInstance process = Bpmn.createExecutableProcess("testProcess")
      .startEvent()
        .timerWithDuration("PT1M")
      .userTask()
      .endEvent()
      .done();

   testRule.deploy(process);
    testRule.deployForTenant(TENANT_ONE, process);
    testRule.deployForTenant(TENANT_TWO, process);

    // the deployed process definition contains a timer start event
    // - so a job definition is created on deployment.
  }

  @Test
  void testQueryNoTenantIdSet() {
    JobDefinitionQuery query = managementService
        .createJobDefinitionQuery();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByTenantId() {
    JobDefinitionQuery query = managementService
        .createJobDefinitionQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isOne();

    query = managementService
        .createJobDefinitionQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIds() {
    JobDefinitionQuery query = managementService
        .createJobDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryByDefinitionsWithoutTenantIds() {
    JobDefinitionQuery query = managementService
        .createJobDefinitionQuery()
        .withoutTenantId();

    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryByTenantIdsIncludeDefinitionsWithoutTenantId() {
    JobDefinitionQuery query = managementService
        .createJobDefinitionQuery()
        .tenantIdIn(TENANT_ONE)
        .includeJobDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = managementService
        .createJobDefinitionQuery()
        .tenantIdIn(TENANT_TWO)
        .includeJobDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = managementService
        .createJobDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeJobDefinitionsWithoutTenantId();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByNonExistingTenantId() {
    JobDefinitionQuery query = managementService
        .createJobDefinitionQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testFailQueryByTenantIdNull() {
    var jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThatThrownBy(() -> jobDefinitionQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void testQuerySortingAsc() {
    // exclude job definitions without tenant id because of database-specific ordering
    List<JobDefinition> jobDefinitions = managementService.createJobDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc()
        .list();

    assertThat(jobDefinitions).hasSize(2);
    assertThat(jobDefinitions.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(jobDefinitions.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testQuerySortingDesc() {
    // exclude job definitions without tenant id because of database-specific ordering
    List<JobDefinition> jobDefinitions = managementService.createJobDefinitionQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .desc()
        .list();

    assertThat(jobDefinitions).hasSize(2);
    assertThat(jobDefinitions.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(jobDefinitions.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    assertThat(query.count()).isOne();
  }

  @Test
  void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    JobDefinitionQuery query = managementService.createJobDefinitionQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).includeJobDefinitionsWithoutTenantId().count()).isEqualTo(2L);
  }

  @Test
  void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    JobDefinitionQuery query = managementService.createJobDefinitionQuery();

    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isOne();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isOne();
    assertThat(query.withoutTenantId().count()).isOne();
  }

  @Test
  void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    assertThat(query.count()).isEqualTo(3L);
  }

}
