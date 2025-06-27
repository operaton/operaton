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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.TimerActivateProcessDefinitionHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerSuspendProcessDefinitionHandler;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

class MultiTenancyProcessDefinitionSuspensionStateTest {

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  protected static final String PROCESS_DEFINITION_KEY = "testProcess";

  protected static final BpmnModelInstance PROCESS = Bpmn.createExecutableProcess(PROCESS_DEFINITION_KEY)
      .startEvent()
      .userTask()
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
  void suspendAndActivateProcessDefinitionsForAllTenants() {
    // given activated process definitions
    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // first suspend
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    // then activate
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .activate();

    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();
  }

  @Test
  void suspendProcessDefinitionForTenant() {
    // given activated process definitions
    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void suspendProcessDefinitionForNonTenant() {
    // given activated process definitions
    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void activateProcessDefinitionForTenant() {
    // given suspend process definitions
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.active().count()).isZero();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void activateProcessDefinitionForNonTenant() {
    // given suspend process definitions
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.active().count()).isZero();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void suspendAndActivateProcessDefinitionsIncludeInstancesForAllTenants() {
    // given activated process instances
    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // first suspend
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .includeProcessInstances(true)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    // then activate
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .includeProcessInstances(true)
      .activate();

    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();
  }

  @Test
  void suspendProcessDefinitionIncludeInstancesForTenant() {
    // given activated process instances
    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .includeProcessInstances(true)
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void suspendProcessDefinitionIncludeInstancesForNonTenant() {
    // given activated process instances
    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .includeProcessInstances(true)
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void activateProcessDefinitionIncludeInstancesForTenant() {
    // given suspended process instances
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .includeProcessInstances(true)
      .suspend();

    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.active().count()).isZero();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .includeProcessInstances(true)
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void activateProcessDefinitionIncludeInstancesForNonTenant() {
    // given suspended process instances
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .includeProcessInstances(true)
      .suspend();

    ProcessInstanceQuery query = engineRule.getRuntimeService().createProcessInstanceQuery();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.active().count()).isZero();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .includeProcessInstances(true)
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void delayedSuspendProcessDefinitionsForAllTenants() {
    // given activated process definitions

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .executionDate(tomorrow())
      .suspend();

    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // when execute the job to suspend the process definitions
    Job job = engineRule.getManagementService().createJobQuery().timers().singleResult();
    assertThat(job).isNotNull();
    List<String> expectedDeploymentIds = query.active().list().stream().map(ProcessDefinition::getDeploymentId).toList();
    assertThat(expectedDeploymentIds).contains(job.getDeploymentId());

    engineRule.getManagementService().executeJob(job.getId());

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);
  }

  @Test
  void delayedSuspendProcessDefinitionsForTenant() {
    // given activated process definitions

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .executionDate(tomorrow())
      .suspend();

    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // when execute the job to suspend the process definition
    Job job = engineRule.getManagementService().createJobQuery().timers().singleResult();
    assertThat(job).isNotNull();
    String expectedDeploymentId = engineRule.getRepositoryService().createProcessDefinitionQuery()
        .active().tenantIdIn(TENANT_ONE).singleResult().getDeploymentId();
    assertThat(job.getDeploymentId()).isEqualTo(expectedDeploymentId);

    engineRule.getManagementService().executeJob(job.getId());

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void delayedSuspendProcessDefinitionsForNonTenant() {
    // given activated process definitions

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .executionDate(tomorrow())
      .suspend();

    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // when execute the job to suspend the process definition
    Job job = engineRule.getManagementService().createJobQuery().timers().singleResult();
    assertThat(job).isNotNull();
    String expectedDeploymentId = engineRule.getRepositoryService().createProcessDefinitionQuery()
        .active().withoutTenantId().singleResult().getDeploymentId();
    assertThat(job.getDeploymentId()).isEqualTo(expectedDeploymentId);

    engineRule.getManagementService().executeJob(job.getId());

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void delayedActivateProcessDefinitionsForAllTenants() {
    // given suspended process definitions
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .executionDate(tomorrow())
      .activate();

    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.active().count()).isZero();

    // when execute the job to activate the process definitions
    Job job = engineRule.getManagementService().createJobQuery().timers().singleResult();
    assertThat(job).isNotNull();
    List<String> expectedDeploymentIds = query.suspended().list().stream().map(ProcessDefinition::getDeploymentId).toList();
    assertThat(expectedDeploymentIds).contains(job.getDeploymentId());

    engineRule.getManagementService().executeJob(job.getId());

    assertThat(query.suspended().count()).isZero();
    assertThat(query.active().count()).isEqualTo(3L);
  }

  @Test
  void delayedActivateProcessDefinitionsForTenant() {
    // given suspended process definitions
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .executionDate(tomorrow())
      .activate();

    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.active().count()).isZero();

    // when execute the job to activate the process definition
    Job job = engineRule.getManagementService().createJobQuery().timers().singleResult();
    assertThat(job).isNotNull();
    String expectedDeploymentId = engineRule.getRepositoryService().createProcessDefinitionQuery()
        .suspended().tenantIdIn(TENANT_ONE).singleResult().getDeploymentId();
    assertThat(job.getDeploymentId()).isEqualTo(expectedDeploymentId);

    engineRule.getManagementService().executeJob(job.getId());

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void delayedActivateProcessDefinitionsForNonTenant() {
    // given suspended process definitions
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .executionDate(tomorrow())
      .activate();

    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.active().count()).isZero();

    // when execute the job to activate the process definition
    Job job = engineRule.getManagementService().createJobQuery().timers().singleResult();
    assertThat(job).isNotNull();
    String expectedDeploymentId = engineRule.getRepositoryService().createProcessDefinitionQuery()
        .suspended().withoutTenantId().singleResult().getDeploymentId();
    assertThat(job.getDeploymentId()).isEqualTo(expectedDeploymentId);

    engineRule.getManagementService().executeJob(job.getId());

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void suspendProcessDefinitionIncludingJobDefinitionsForAllTenants() {
    // given activated jobs
    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);
  }

  @Test
  void suspendProcessDefinitionIncludingJobDefinitionsForTenant() {
    // given activated jobs
    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void suspendProcessDefinitionIncludingJobDefinitionsForNonTenant() {
    // given activated jobs
    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void activateProcessDefinitionIncludingJobDefinitionsForAllTenants() {
    // given suspended jobs
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .activate();

    assertThat(query.suspended().count()).isZero();
    assertThat(query.active().count()).isEqualTo(3L);
  }

  @Test
  void activateProcessDefinitionIncludingJobDefinitionsForTenant() {
    // given suspended jobs
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void activateProcessDefinitionIncludingJobDefinitionsForNonTenant() {
    // given suspended jobs
    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.active().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void suspendProcessDefinitionNoAuthenticatedTenants() {
    // given activated process definitions
    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getIdentityService().setAuthentication("user", null, null);

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    engineRule.getIdentityService().clearAuthentication();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isEqualTo(1L);
    assertThat(query.suspended().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void failToSuspendProcessDefinitionByIdNoAuthenticatedTenants() {
    ProcessDefinition processDefinition = engineRule.getRepositoryService().createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_DEFINITION_KEY).tenantIdIn(TENANT_ONE).singleResult();

    engineRule.getIdentityService().setAuthentication("user", null, null);
    var updateProcessDefinitionSuspensionStateBuilder = engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinition.getId());

    // when/then
    assertThatThrownBy(updateProcessDefinitionSuspensionStateBuilder::suspend)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot update the process definition '"+ processDefinition.getId()
      + "' because it belongs to no authenticated tenant");
  }

  @Test
  void suspendProcessDefinitionWithAuthenticatedTenant() {
    // given activated process definitions
    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getIdentityService().setAuthentication("user", null, List.of(TENANT_ONE));

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    engineRule.getIdentityService().clearAuthentication();

    assertThat(query.active().count()).isEqualTo(1L);
    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().tenantIdIn(TENANT_TWO).count()).isEqualTo(1L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.suspended().withoutTenantId().count()).isEqualTo(1L);
  }

  @Test
  void suspendProcessDefinitionDisabledTenantCheck() {
    // given activated process definitions
    ProcessDefinitionQuery query = engineRule.getRepositoryService().createProcessDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    ProcessEngineConfigurationImpl processEngineConfiguration = engineRule.getProcessEngineConfiguration();
    processEngineConfiguration.setTenantCheckEnabled(false);
    engineRule.getIdentityService().setAuthentication("user", null, null);

    engineRule.getRepositoryService()
      .updateProcessDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE, TENANT_TWO).includeProcessDefinitionsWithoutTenantId().count()).isEqualTo(3L);
  }

  protected Date tomorrow() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_YEAR, 1);
    return calendar.getTime();
  }

  @AfterEach
  void tearDown() {
    CommandExecutor commandExecutor = engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerActivateProcessDefinitionHandler.TYPE);
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerSuspendProcessDefinitionHandler.TYPE);
      return null;
    });
  }

}
