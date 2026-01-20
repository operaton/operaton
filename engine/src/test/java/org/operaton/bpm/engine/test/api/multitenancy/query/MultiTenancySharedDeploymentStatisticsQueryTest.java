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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.management.DeploymentStatistics;
import org.operaton.bpm.engine.management.IncidentStatistics;
import org.operaton.bpm.engine.test.api.multitenancy.StaticTenantIdTestProvider;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */
class MultiTenancySharedDeploymentStatisticsQueryTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String ONE_TASK_PROCESS_DEFINITION_KEY = "oneTaskProcess";
  protected static final String FAILED_JOBS_PROCESS_DEFINITION_KEY = "ExampleProcess";
  protected static final String ANOTHER_FAILED_JOBS_PROCESS_DEFINITION_KEY = "AnotherExampleProcess";

  protected static StaticTenantIdTestProvider tenantIdProvider;

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .closeEngineAfterAllTests()
      .randomEngineName()
      .configurator(configuration -> {
        tenantIdProvider = new StaticTenantIdTestProvider(TENANT_ONE);
        configuration.setTenantIdProvider(tenantIdProvider);
      })
      .build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RuntimeService runtimeService;
  protected ManagementService managementService;
  protected IdentityService identityService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  protected static final BpmnModelInstance oneTaskProcess = Bpmn.createExecutableProcess(ONE_TASK_PROCESS_DEFINITION_KEY)
    .startEvent()
    .userTask()
    .done();

  protected static final BpmnModelInstance failingProcess = Bpmn.createExecutableProcess(FAILED_JOBS_PROCESS_DEFINITION_KEY)
    .startEvent()
    .serviceTask()
      .operatonClass("org.operaton.bpm.engine.test.api.multitenancy.FailingDelegate")
      .operatonAsyncBefore()
    .done();

  protected static final BpmnModelInstance anotherFailingProcess = Bpmn.createExecutableProcess(ANOTHER_FAILED_JOBS_PROCESS_DEFINITION_KEY)
    .startEvent()
    .serviceTask()
      .operatonClass("org.operaton.bpm.engine.test.api.multitenancy.FailingDelegate")
      .operatonAsyncBefore()
    .done();

  @Test
  void activeProcessInstancesCountWithNoAuthenticatedTenant() {

    testRule.deploy(oneTaskProcess);

    startProcessInstances(ONE_TASK_PROCESS_DEFINITION_KEY);

    identityService.setAuthentication("user", null, null);

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);
    // user must see only the process instances that belongs to no tenant
    assertThat(deploymentStatistics.get(0).getInstances()).isEqualTo(1);

  }

  @Test
  void activeProcessInstancesCountWithAuthenticatedTenant() {

    testRule.deploy(oneTaskProcess);

    startProcessInstances(ONE_TASK_PROCESS_DEFINITION_KEY);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);
    // user can see the process instances that belongs to tenant1 and instances that have no tenant
    assertThat(deploymentStatistics.get(0).getInstances()).isEqualTo(2);

  }

  @Test
  void activeProcessInstancesCountWithDisabledTenantCheck() {

    testRule.deploy(oneTaskProcess);

    startProcessInstances(ONE_TASK_PROCESS_DEFINITION_KEY);

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);
    assertThat(deploymentStatistics.get(0).getInstances()).isEqualTo(3);
  }

  @Test
  void activeProcessInstancesCountWithMultipleAuthenticatedTenants() {

    testRule.deploy(oneTaskProcess);

    startProcessInstances(ONE_TASK_PROCESS_DEFINITION_KEY);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);
    // user can see all the active process instances
    assertThat(deploymentStatistics.get(0).getInstances()).isEqualTo(3);

  }

  @Test
  void failedJobsCountWithWithNoAuthenticatedTenant() {

    testRule.deploy(failingProcess);

    startProcessInstances(FAILED_JOBS_PROCESS_DEFINITION_KEY);

    testRule.executeAvailableJobs();

    identityService.setAuthentication("user", null, null);

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
      .includeFailedJobs()
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);
    assertThat(deploymentStatistics.get(0).getFailedJobs()).isEqualTo(1);

  }

  @Test
  void failedJobsCountWithWithDisabledTenantCheck() {

    testRule.deploy(failingProcess);

    startProcessInstances(FAILED_JOBS_PROCESS_DEFINITION_KEY);

    testRule.executeAvailableJobs();

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
      .includeFailedJobs()
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);
    assertThat(deploymentStatistics.get(0).getFailedJobs()).isEqualTo(3);

  }

  @Test
  void failedJobsCountWithAuthenticatedTenant() {

    testRule.deploy(failingProcess);

    startProcessInstances(FAILED_JOBS_PROCESS_DEFINITION_KEY);

    testRule.executeAvailableJobs();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
     .includeFailedJobs()
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);
    assertThat(deploymentStatistics.get(0).getFailedJobs()).isEqualTo(2);
  }

  @Test
  void failedJobsCountWithMultipleAuthenticatedTenants() {

    testRule.deploy(failingProcess);

    startProcessInstances(FAILED_JOBS_PROCESS_DEFINITION_KEY);

    testRule.executeAvailableJobs();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
      .includeFailedJobs()
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);
    assertThat(deploymentStatistics.get(0).getFailedJobs()).isEqualTo(3);
  }

  @Test
  void incidentsCountWithNoAuthenticatedTenant() {

    testRule.deploy(failingProcess);

    startProcessInstances(FAILED_JOBS_PROCESS_DEFINITION_KEY);

    testRule.executeAvailableJobs();

    identityService.setAuthentication("user", null, null);

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
      .includeIncidents()
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);

    List<IncidentStatistics> incidentStatistics = deploymentStatistics.get(0).getIncidentStatistics();
    assertThat(incidentStatistics).hasSize(1);
    assertThat(incidentStatistics.get(0).getIncidentCount()).isEqualTo(1);
  }

  @Test
  void incidentsCountWithDisabledTenantCheck() {

    testRule.deploy(failingProcess);

    startProcessInstances(FAILED_JOBS_PROCESS_DEFINITION_KEY);

    testRule.executeAvailableJobs();

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
      .includeIncidents()
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);

    List<IncidentStatistics> incidentStatistics = deploymentStatistics.get(0).getIncidentStatistics();
    assertThat(incidentStatistics).hasSize(1);
    assertThat(incidentStatistics.get(0).getIncidentCount()).isEqualTo(3);
  }

  @Test
  void incidentsCountWithAuthenticatedTenant() {

    testRule.deploy(failingProcess);

    startProcessInstances(FAILED_JOBS_PROCESS_DEFINITION_KEY);

    testRule.executeAvailableJobs();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
      .includeIncidents()
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);

    List<IncidentStatistics> incidentStatistics = deploymentStatistics.get(0).getIncidentStatistics();
    assertThat(incidentStatistics).hasSize(1);
    assertThat(incidentStatistics.get(0).getIncidentCount()).isEqualTo(2);
  }

  @Test
  void incidentsCountWithMultipleAuthenticatedTenants() {

    testRule.deploy(failingProcess);

    startProcessInstances(FAILED_JOBS_PROCESS_DEFINITION_KEY);

    testRule.executeAvailableJobs();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE, TENANT_TWO));

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
      .includeIncidents()
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);
    List<IncidentStatistics> incidentStatistics = deploymentStatistics.get(0).getIncidentStatistics();
    assertThat(incidentStatistics).hasSize(1);
    assertThat(incidentStatistics.get(0).getIncidentCount()).isEqualTo(3);
  }

  @Test
  void incidentsCountWithIncidentTypeAndAuthenticatedTenant() {

    testRule.deploy(failingProcess);

    startProcessInstances(FAILED_JOBS_PROCESS_DEFINITION_KEY);

    testRule.executeAvailableJobs();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
      .includeIncidentsForType("failedJob")
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);

    List<IncidentStatistics> incidentStatistics = deploymentStatistics.get(0).getIncidentStatistics();
    assertThat(incidentStatistics).hasSize(1);
    assertThat(incidentStatistics.get(0).getIncidentCount()).isEqualTo(2);
  }

  @Test
  void instancesFailedJobsAndIncidentsCountWithAuthenticatedTenant() {

    testRule.deploy(failingProcess,anotherFailingProcess);

    startProcessInstances(FAILED_JOBS_PROCESS_DEFINITION_KEY);
    startProcessInstances(ANOTHER_FAILED_JOBS_PROCESS_DEFINITION_KEY);

    testRule.executeAvailableJobs();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    List<DeploymentStatistics> deploymentStatistics = managementService
      .createDeploymentStatisticsQuery()
      .includeFailedJobs()
      .includeIncidents()
      .list();

    // then
    assertThat(deploymentStatistics).hasSize(1);
    DeploymentStatistics singleDeploymentStatistics = deploymentStatistics.get(0);
    assertThat(singleDeploymentStatistics.getInstances()).isEqualTo(4);
    assertThat(singleDeploymentStatistics.getFailedJobs()).isEqualTo(4);

    List<IncidentStatistics> incidentStatistics = singleDeploymentStatistics.getIncidentStatistics();
    assertThat(incidentStatistics).hasSize(1);
    assertThat(incidentStatistics.get(0).getIncidentCount()).isEqualTo(4);
  }

  protected void startProcessInstances(String key) {
    setTenantIdProvider(null);
    runtimeService.startProcessInstanceByKey(key);

    setTenantIdProvider(TENANT_ONE);
    runtimeService.startProcessInstanceByKey(key);

    setTenantIdProvider(TENANT_TWO);
    runtimeService.startProcessInstanceByKey(key);
  }

  protected void setTenantIdProvider(String tenantId) {
    tenantIdProvider.setTenantIdProvider(tenantId);
  }
}
