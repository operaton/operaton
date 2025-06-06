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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

class MultiTenancyDeploymentQueryTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RepositoryService repositoryService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    BpmnModelInstance emptyProcess = Bpmn.createExecutableProcess().startEvent().done();

    testRule.deploy(emptyProcess);
    testRule.deployForTenant(TENANT_ONE, emptyProcess);
    testRule.deployForTenant(TENANT_TWO, emptyProcess);
  }

  @Test
  void testQueryNoTenantIdSet() {
    DeploymentQuery query = repositoryService
        .createDeploymentQuery();

   assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByTenantId() {
    DeploymentQuery query = repositoryService
        .createDeploymentQuery()
        .tenantIdIn(TENANT_ONE);

    assertThat(query.count()).isEqualTo(1L);

    query = repositoryService
        .createDeploymentQuery()
        .tenantIdIn(TENANT_TWO);

    assertThat(query.count()).isEqualTo(1L);
  }

  @Test
  void testQueryByTenantIds() {
    DeploymentQuery query = repositoryService
        .createDeploymentQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO);

    assertThat(query.count()).isEqualTo(2L);
  }

  @Test
  void testQueryWithoutTenantId() {
    DeploymentQuery query = repositoryService
        .createDeploymentQuery()
        .withoutTenantId();

    assertThat(query.count()).isEqualTo(1L);
  }

  @Test
  void testQueryByTenantIdsIncludeDeploymentsWithoutTenantId() {
    DeploymentQuery query = repositoryService
        .createDeploymentQuery()
        .tenantIdIn(TENANT_ONE)
        .includeDeploymentsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = repositoryService
        .createDeploymentQuery()
        .tenantIdIn(TENANT_TWO)
        .includeDeploymentsWithoutTenantId();

    assertThat(query.count()).isEqualTo(2L);

    query = repositoryService
        .createDeploymentQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .includeDeploymentsWithoutTenantId();

    assertThat(query.count()).isEqualTo(3L);
  }

  @Test
  void testQueryByNonExistingTenantId() {
    DeploymentQuery query = repositoryService
        .createDeploymentQuery()
        .tenantIdIn("nonExisting");

    assertThat(query.count()).isZero();
  }

  @Test
  void testFailQueryByTenantIdNull() {
    var deploymentQuery = repositoryService.createDeploymentQuery();

    assertThatThrownBy(() -> deploymentQuery.tenantIdIn((String) null))
        .isInstanceOf(NullValueException.class)
        .hasMessage("tenantIds contains null value");
  }

  @Test
  void testQuerySortingAsc() {
    // exclude deployments without tenant id because of database-specific ordering
    List<Deployment> deployments = repositoryService.createDeploymentQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .asc()
        .list();

    assertThat(deployments).hasSize(2);
    assertThat(deployments.get(0).getTenantId()).isEqualTo(TENANT_ONE);
    assertThat(deployments.get(1).getTenantId()).isEqualTo(TENANT_TWO);
  }

  @Test
  void testQuerySortingDesc() {
    // exclude deployments without tenant id because of database-specific ordering
    List<Deployment> deployments = repositoryService.createDeploymentQuery()
        .tenantIdIn(TENANT_ONE, TENANT_TWO)
        .orderByTenantId()
        .desc()
        .list();

    assertThat(deployments).hasSize(2);
    assertThat(deployments.get(0).getTenantId()).isEqualTo(TENANT_TWO);
    assertThat(deployments.get(1).getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void testQueryNoAuthenticatedTenants() {
    identityService.setAuthentication("user", null, null);

    DeploymentQuery query = repositoryService.createDeploymentQuery();
    assertThat(query.count()).isEqualTo(1L);
  }

  @Test
  void testQueryAuthenticatedTenant() {
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE));

    DeploymentQuery query = repositoryService.createDeploymentQuery();

    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE, TENANT_TWO).includeDeploymentsWithoutTenantId().count()).isEqualTo(2L);
  }

  @Test
  void testQueryAuthenticatedTenants() {
    identityService.setAuthentication("user", null, Arrays.asList(TENANT_ONE, TENANT_TWO));

    DeploymentQuery query = repositoryService.createDeploymentQuery();

    assertThat(query.count()).isEqualTo(3L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isEqualTo(1L);
    assertThat(query.withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void testQueryDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    DeploymentQuery query = repositoryService.createDeploymentQuery();
    assertThat(query.count()).isEqualTo(3L);
  }


}
