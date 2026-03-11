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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.management.ActivityStatisticsQuery;
import org.operaton.bpm.engine.management.DeploymentStatistics;
import org.operaton.bpm.engine.management.DeploymentStatisticsQuery;
import org.operaton.bpm.engine.management.ProcessDefinitionStatistics;
import org.operaton.bpm.engine.management.ProcessDefinitionStatisticsQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

class MultiTenancyStatisticsQueryTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {

    BpmnModelInstance process = Bpmn.createExecutableProcess("EmptyProcess")
    .startEvent().done();

    BpmnModelInstance singleTaskProcess = Bpmn.createExecutableProcess("SingleTaskProcess")
      .startEvent()
        .userTask()
      .done();

    testRule.deploy(process);
    testRule.deployForTenant(TENANT_ONE, singleTaskProcess);
    testRule.deployForTenant(TENANT_TWO, process);
  }

  @Test
  void testDeploymentStatistics() {
    List<DeploymentStatistics> deploymentStatistics = managementService
        .createDeploymentStatisticsQuery()
        .list();

    assertThat(deploymentStatistics).hasSize(3);

    Set<String> tenantIds = collectDeploymentTenantIds(deploymentStatistics);
    assertThat(tenantIds).contains(null, TENANT_ONE, TENANT_TWO);
  }

  @Test
  void testProcessDefinitionStatistics() {
    List<ProcessDefinitionStatistics> processDefinitionStatistics = managementService
      .createProcessDefinitionStatisticsQuery()
      .list();

    assertThat(processDefinitionStatistics).hasSize(3);

    Set<String> tenantIds = collectDefinitionTenantIds(processDefinitionStatistics);
    assertThat(tenantIds).contains(null, TENANT_ONE, TENANT_TWO);
  }

  @Test
  void testQueryNoAuthenticatedTenantsForDeploymentStatistics() {
    identityService.setAuthentication("user", null, null);

    DeploymentStatisticsQuery query = managementService.createDeploymentStatisticsQuery();
    assertThat(query.count()).isOne();

    Set<String> tenantIds = collectDeploymentTenantIds(query.list());
    assertThat(tenantIds).hasSize(1);
    assertThat(tenantIds.iterator().next()).isNull();
  }

  @Test
  void testQueryAuthenticatedTenantForDeploymentStatistics() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    DeploymentStatisticsQuery query = managementService.createDeploymentStatisticsQuery();

    assertThat(query.count()).isEqualTo(2L);

    Set<String> tenantIds = collectDeploymentTenantIds(query.list());
    assertThat(tenantIds).containsExactlyInAnyOrder(null, TENANT_ONE);
  }

  @Test
  void testQueryAuthenticatedTenantsForDeploymentStatistics() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    DeploymentStatisticsQuery query = managementService.createDeploymentStatisticsQuery();

    assertThat(query.count()).isEqualTo(3L);

    Set<String> tenantIds = collectDeploymentTenantIds(query.list());
    assertThat(tenantIds).containsExactlyInAnyOrder(null, TENANT_ONE, TENANT_TWO);
  }

  @Test
  void testQueryDisabledTenantCheckForDeploymentStatistics() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    DeploymentStatisticsQuery query = managementService.createDeploymentStatisticsQuery();

    assertThat(query.count()).isEqualTo(3L);

    Set<String> tenantIds = collectDeploymentTenantIds(query.list());
    assertThat(tenantIds).containsExactlyInAnyOrder(null, TENANT_ONE, TENANT_TWO);
  }

  @Test
  void testQueryNoAuthenticatedTenantsForProcessDefinitionStatistics() {
    identityService.setAuthentication("user", null, null);

    ProcessDefinitionStatisticsQuery query = managementService.createProcessDefinitionStatisticsQuery();
    assertThat(query.count()).isOne();

    Set<String> tenantIds = collectDefinitionTenantIds(query.list());
    assertThat(tenantIds).hasSize(1);
    assertThat(tenantIds.iterator().next()).isNull();
  }

  @Test
  void testQueryAuthenticatedTenantForProcessDefinitionStatistics() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    ProcessDefinitionStatisticsQuery query = managementService.createProcessDefinitionStatisticsQuery();

    assertThat(query.count()).isEqualTo(2L);

    Set<String> tenantIds = collectDefinitionTenantIds(query.list());
    assertThat(tenantIds).containsExactlyInAnyOrder(null, TENANT_ONE);
  }

  @Test
  void testQueryAuthenticatedTenantsForProcessDefinitionStatistics() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    ProcessDefinitionStatisticsQuery query = managementService.createProcessDefinitionStatisticsQuery();

    assertThat(query.count()).isEqualTo(3L);

    Set<String> tenantIds = collectDefinitionTenantIds(query.list());
    assertThat(tenantIds).containsExactlyInAnyOrder(null, TENANT_ONE, TENANT_TWO);
  }

  @Test
  void testQueryDisabledTenantCheckForProcessDefinitionStatistics() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    ProcessDefinitionStatisticsQuery query = managementService.createProcessDefinitionStatisticsQuery();

    assertThat(query.count()).isEqualTo(3L);

    Set<String> tenantIds = collectDefinitionTenantIds(query.list());
    assertThat(tenantIds).containsExactlyInAnyOrder(null, TENANT_ONE, TENANT_TWO);
  }

  @Test
  void testActivityStatistics() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("SingleTaskProcess");

    ActivityStatisticsQuery query = managementService.createActivityStatisticsQuery(processInstance.getProcessDefinitionId());

    assertThat(query.count()).isOne();

  }

  @Test
  void testQueryAuthenticatedTenantForActivityStatistics() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("SingleTaskProcess");

    ActivityStatisticsQuery query = managementService.createActivityStatisticsQuery(processInstance.getProcessDefinitionId());

    assertThat(query.count()).isOne();

  }

  @Test
  void testQueryNoAuthenticatedTenantForActivityStatistics() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("SingleTaskProcess");

    identityService.setAuthentication("user", null);

    ActivityStatisticsQuery query = managementService.createActivityStatisticsQuery(processInstance.getProcessDefinitionId());

    assertThat(query.count()).isZero();

  }

  @Test
  void testQueryDisabledTenantCheckForActivityStatistics() {

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("SingleTaskProcess");

    identityService.setAuthentication("user", null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    ActivityStatisticsQuery query = managementService.createActivityStatisticsQuery(processInstance.getProcessDefinitionId());

    assertThat(query.count()).isOne();

  }

  protected Set<String> collectDeploymentTenantIds(List<DeploymentStatistics> deploymentStatistics) {
    Set<String> tenantIds = new HashSet<>();

    for (DeploymentStatistics statistics : deploymentStatistics) {
      tenantIds.add(statistics.getTenantId());
    }
    return tenantIds;
  }

  protected Set<String> collectDefinitionTenantIds(List<ProcessDefinitionStatistics> processDefinitionStatistics) {
    Set<String> tenantIds = new HashSet<>();

    for (ProcessDefinitionStatistics statistics : processDefinitionStatistics) {
      tenantIds.add(statistics.getTenantId());
    }
    return tenantIds;
  }

}
