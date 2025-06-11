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
package org.operaton.bpm.engine.test.api.multitenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

class MultiTenancyTimerStartEventTest {

  protected static final BpmnModelInstance PROCESS = Bpmn.createExecutableProcess()
      .startEvent()
        .timerWithDuration("PT1M")
      .userTask()
      .endEvent()
      .done();

  protected static final String TENANT_ONE = "tenant1";
  protected static final String TENANT_TWO = "tenant2";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected ManagementService managementService;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;

  @Test
  void startProcessInstanceWithTenantId() {

    testRule.deployForTenant(TENANT_ONE, PROCESS);

    Job job = managementService.createJobQuery().singleResult();
    assertThat(job.getTenantId()).isEqualTo(TENANT_ONE);

    managementService.executeJob(job.getId());

    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertThat(processInstance).isNotNull();
    assertThat(processInstance.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void startProcessInstanceTwoTenants() {

    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);

    Job jobForTenantOne = managementService.createJobQuery().tenantIdIn(TENANT_ONE).singleResult();
    assertThat(jobForTenantOne).isNotNull();
    managementService.executeJob(jobForTenantOne.getId());

    Job jobForTenantTwo = managementService.createJobQuery().tenantIdIn(TENANT_TWO).singleResult();
    assertThat(jobForTenantTwo).isNotNull();
    managementService.executeJob(jobForTenantTwo.getId());

    assertThat(runtimeService.createProcessInstanceQuery().tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(runtimeService.createProcessInstanceQuery().tenantIdIn(TENANT_TWO).count()).isEqualTo(1L);
  }

  @Test
  void deleteJobsWhileUndeployment() {
    Deployment deploymentForTenantOne = testRule.deployForTenant(TENANT_ONE, PROCESS);
    Deployment deploymentForTenantTwo = testRule.deployForTenant(TENANT_TWO, PROCESS);

    JobQuery query = managementService.createJobQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isEqualTo(1L);

    repositoryService.deleteDeployment(deploymentForTenantOne.getId(), true);

    assertThat(query.tenantIdIn(TENANT_ONE).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isEqualTo(1L);

    repositoryService.deleteDeployment(deploymentForTenantTwo.getId(), true);

    assertThat(query.tenantIdIn(TENANT_ONE).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  void dontCreateNewJobsWhileReDeployment() {
    testRule.deployForTenant(TENANT_ONE, PROCESS);
    testRule.deployForTenant(TENANT_TWO, PROCESS);
    testRule.deployForTenant(TENANT_ONE, PROCESS);

    JobQuery query = managementService.createJobQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isEqualTo(1L);
  }

  @Test
  void failedJobRetryTimeCycle() {

    testRule.deployForTenant(TENANT_ONE, Bpmn.createExecutableProcess("failingProcess")
      .startEvent()
        .timerWithDuration("PT1M")
        .operatonFailedJobRetryTimeCycle("R5/PT1M")
      .serviceTask()
        .operatonExpression("${failing}")
      .endEvent()
      .done());

    testRule.deployForTenant(TENANT_TWO, Bpmn.createExecutableProcess("failingProcess")
      .startEvent()
        .timerWithDuration("PT1M")
        .operatonFailedJobRetryTimeCycle("R4/PT1M")
      .serviceTask()
        .operatonExpression("${failing}")
      .endEvent()
      .done());

    List<Job> jobs = managementService.createJobQuery().timers().list();
    executeFailingJobs(jobs);

    Job jobTenantOne = managementService.createJobQuery().tenantIdIn(TENANT_ONE).singleResult();
    Job jobTenantTwo = managementService.createJobQuery().tenantIdIn(TENANT_TWO).singleResult();

    assertThat(jobTenantOne.getRetries()).isEqualTo(4);
    assertThat(jobTenantTwo.getRetries()).isEqualTo(3);
  }

  @Test
  void timerStartEventWithTimerCycle() {

    testRule.deployForTenant(TENANT_ONE, Bpmn.createExecutableProcess()
        .startEvent()
          .timerWithCycle("R2/PT1M")
        .userTask()
        .endEvent()
        .done());

    // execute first timer cycle
    Job job = managementService.createJobQuery().singleResult();
    assertThat(job.getTenantId()).isEqualTo(TENANT_ONE);
    managementService.executeJob(job.getId());

    // execute second timer cycle
    job = managementService.createJobQuery().singleResult();
    assertThat(job.getTenantId()).isEqualTo(TENANT_ONE);
    managementService.executeJob(job.getId());

    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
    assertThat(query.withoutTenantId().count()).isZero();
  }

  protected void executeFailingJobs(List<Job> jobs) {
    jobs.forEach(job -> {
      String jobId = job.getId();
      assertThatThrownBy(() -> managementService.executeJob(jobId))
        .isInstanceOf(ProcessEngineException.class)
        .hasMessageContaining("Cannot resolve identifier 'failing'");
    });
  }

}
