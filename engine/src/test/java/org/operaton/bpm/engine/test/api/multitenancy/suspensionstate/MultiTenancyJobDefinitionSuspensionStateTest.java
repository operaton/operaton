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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.TimerActivateJobDefinitionHandler;
import org.operaton.bpm.engine.impl.jobexecutor.TimerSuspendJobDefinitionHandler;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;

class MultiTenancyJobDefinitionSuspensionStateTest {

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
  void suspendAndActivateJobDefinitionsForAllTenants() {
    // given activated job definitions
    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // first suspend
    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    // then activate
    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .activate();

    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();
  }

  @Test
  void suspendJobDefinitionForTenant() {
    // given activated job definitions
    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isOne();
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void suspendJobDefinitionForNonTenant() {
    // given activated job definitions
    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isOne();
    assertThat(query.suspended().withoutTenantId().count()).isOne();
  }

  @Test
  void activateJobDefinitionForTenant() {
    // given suspend job definitions
    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.active().count()).isZero();

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isOne();
    assertThat(query.active().tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void jobProcessDefinitionForNonTenant() {
    // given suspend job definitions
    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.active().count()).isZero();

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isOne();
    assertThat(query.active().withoutTenantId().count()).isOne();
  }

  @Test
  void suspendAndActivateJobDefinitionsIncludingJobsForAllTenants() {
    // given activated job definitions
    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // first suspend
    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .includeJobs(true)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    // then activate
    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .includeJobs(true)
      .activate();

    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();
  }

  @Test
  void suspendJobDefinitionIncludingJobsForTenant() {
    // given activated job definitions
    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .includeJobs(true)
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isOne();
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void suspendJobDefinitionIncludingJobsForNonTenant() {
    // given activated job definitions
    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .includeJobs(true)
      .suspend();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isOne();
    assertThat(query.suspended().withoutTenantId().count()).isOne();
  }

  @Test
  void activateJobDefinitionIncludingJobsForTenant() {
    // given suspend job definitions
    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .includeJobs(true)
      .suspend();

    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.active().count()).isZero();

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .includeJobs(true)
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isOne();
    assertThat(query.active().tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void activateJobDefinitionIncludingJobsForNonTenant() {
    // given suspend job definitions
    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .includeJobs(true)
      .suspend();

    JobQuery query = engineRule.getManagementService().createJobQuery();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.active().count()).isZero();

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .includeJobs(true)
      .activate();

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isOne();
    assertThat(query.active().withoutTenantId().count()).isOne();
  }

  @Test
  void delayedSuspendJobDefinitionsForAllTenants() {
    // given activated job definitions

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .executionDate(tomorrow())
      .suspend();

    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // when execute the job to suspend the job definitions
    Job job = engineRule.getManagementService().createJobQuery().timers().singleResult();
    assertThat(job).isNotNull();
    assertThat(getDeploymentIds(query.active())).contains(job.getDeploymentId());

    engineRule.getManagementService().executeJob(job.getId());

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);
  }

  @Test
  void delayedSuspendJobDefinitionsForTenant() {
    // given activated job definitions

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .executionDate(tomorrow())
      .suspend();

    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // when execute the job to suspend the job definitions
    Job job = engineRule.getManagementService().createJobQuery().timers().singleResult();
    assertThat(job).isNotNull();
    JobDefinition expectedJobDefinition = engineRule.getManagementService().createJobDefinitionQuery()
        .active().tenantIdIn(TENANT_ONE).singleResult();
    assertThat(job.getDeploymentId()).isEqualTo(getDeploymentId(expectedJobDefinition));

    engineRule.getManagementService().executeJob(job.getId());

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isOne();
    assertThat(query.suspended().tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void delayedSuspendJobDefinitionsForNonTenant() {
    // given activated job definitions

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .executionDate(tomorrow())
      .suspend();

    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    // when execute the job to suspend the job definitions
    Job job = engineRule.getManagementService().createJobQuery().timers().singleResult();
    assertThat(job).isNotNull();
    JobDefinition expectedJobDefinition = engineRule.getManagementService().createJobDefinitionQuery()
        .active().withoutTenantId().singleResult();
    assertThat(job.getDeploymentId()).isEqualTo(getDeploymentId(expectedJobDefinition));

    engineRule.getManagementService().executeJob(job.getId());

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isOne();
    assertThat(query.suspended().withoutTenantId().count()).isOne();
  }

  @Test
  void delayedActivateJobDefinitionsForAllTenants() {
    // given suspend job definitions
    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .executionDate(tomorrow())
      .activate();

    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    // when execute the job to activate the job definitions
    Job job = engineRule.getManagementService().createJobQuery().timers().singleResult();
    assertThat(job).isNotNull();
    assertThat(getDeploymentIds(query.suspended())).contains(job.getDeploymentId());

    engineRule.getManagementService().executeJob(job.getId());

    assertThat(query.suspended().count()).isZero();
    assertThat(query.active().count()).isEqualTo(3L);
  }

  @Test
  void delayedActivateJobDefinitionsForTenant() {
    // given suspend job definitions
    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionTenantId(TENANT_ONE)
      .executionDate(tomorrow())
      .activate();

    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    // when execute the job to activate the job definitions
    Job job = engineRule.getManagementService().createJobQuery().timers().singleResult();
    assertThat(job).isNotNull();
    JobDefinition expectedJobDefinition = engineRule.getManagementService().createJobDefinitionQuery()
        .suspended().tenantIdIn(TENANT_ONE).singleResult();
    assertThat(job.getDeploymentId()).isEqualTo(getDeploymentId(expectedJobDefinition));

    engineRule.getManagementService().executeJob(job.getId());

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isOne();
    assertThat(query.active().tenantIdIn(TENANT_ONE).count()).isOne();
  }

  @Test
  void delayedActivateJobDefinitionsForNonTenant() {
    // given suspend job definitions
    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .processDefinitionWithoutTenantId()
      .executionDate(tomorrow())
      .activate();

    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);

    // when execute the job to activate the job definitions
    Job job = engineRule.getManagementService().createJobQuery().timers().singleResult();
    assertThat(job).isNotNull();
    JobDefinition expectedJobDefinition = engineRule.getManagementService().createJobDefinitionQuery()
        .suspended().withoutTenantId().singleResult();
    assertThat(job.getDeploymentId()).isEqualTo(getDeploymentId(expectedJobDefinition));

    engineRule.getManagementService().executeJob(job.getId());

    assertThat(query.suspended().count()).isEqualTo(2L);
    assertThat(query.active().count()).isOne();
    assertThat(query.active().withoutTenantId().count()).isOne();
  }

  @Test
  void suspendJobDefinitionNoAuthenticatedTenants() {
    // given activated job definitions
    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getIdentityService().setAuthentication("user", null, null);

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    engineRule.getIdentityService().clearAuthentication();

    assertThat(query.active().count()).isEqualTo(2L);
    assertThat(query.suspended().count()).isOne();
    assertThat(query.suspended().withoutTenantId().count()).isOne();
  }

  @Test
  void suspendJobDefinitionWithAuthenticatedTenant() {
    // given activated job definitions
    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getIdentityService().setAuthentication("user", null, List.of(TENANT_ONE));

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
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
  void suspendJobDefinitionDisabledTenantCheck() {
    // given activated job definitions
    JobDefinitionQuery query = engineRule.getManagementService().createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(3L);
    assertThat(query.suspended().count()).isZero();

    engineRule.getProcessEngineConfiguration().setTenantCheckEnabled(false);
    engineRule.getIdentityService().setAuthentication("user", null, null);

    engineRule.getManagementService()
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey(PROCESS_DEFINITION_KEY)
      .suspend();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isEqualTo(3L);
    assertThat(query.suspended().tenantIdIn(TENANT_ONE, TENANT_TWO).includeJobDefinitionsWithoutTenantId().count()).isEqualTo(3L);
  }

  protected Date tomorrow() {
    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DAY_OF_YEAR, 1);
    return calendar.getTime();
  }

  protected List<String> getDeploymentIds(JobDefinitionQuery jobDefinitionQuery){
    return jobDefinitionQuery.list().stream().map(this::getDeploymentId).toList();
  }

  protected String getDeploymentId(JobDefinition jobDefinition) {
    return engineRule.getRepositoryService().createProcessDefinitionQuery()
      .processDefinitionId(jobDefinition.getProcessDefinitionId())
      .singleResult()
      .getDeploymentId();
  }

  @AfterEach
  void tearDown() {
    CommandExecutor commandExecutor = engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerActivateJobDefinitionHandler.TYPE);
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerSuspendJobDefinitionHandler.TYPE);
      return null;
    });
  }

}
