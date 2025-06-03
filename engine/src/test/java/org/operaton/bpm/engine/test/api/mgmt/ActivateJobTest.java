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
package org.operaton.bpm.engine.test.api.mgmt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;

/**
 * @author roman.smirnov
 */
@ExtendWith(ProcessEngineExtension.class)
class ActivateJobTest {

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected ManagementService managementService;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;

  @Test
  void testActivationById_shouldThrowProcessEngineException() {
    assertThatThrownBy(() -> managementService.activateJobById(null))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("jobId is null");
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationById_shouldActivateJob() {
    // given

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // suspended job definitions and corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    assertThat(job.isSuspended()).isTrue();

    // when
    // the job will be activated
    managementService.activateJobById(job.getId());

    // then
    // the job should be active
    assertThat(jobQuery.active().count()).isEqualTo(1);
    assertThat(jobQuery.suspended().count()).isZero();

    Job activeJob = jobQuery.active().singleResult();

    assertThat(activeJob.getId()).isEqualTo(job.getId());
    assertThat(activeJob.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByJobDefinitionId_shouldActivateJob() {
    // given

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // suspended job definitions and corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // the job definition
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    assertThat(job.isSuspended()).isTrue();

    // when
    // the job will be activated
    managementService.activateJobByJobDefinitionId(jobDefinition.getId());

    // then
    // the job should be activated
    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job activeJob = jobQuery.active().singleResult();

    assertThat(activeJob.getId()).isEqualTo(job.getId());
    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivateByProcessInstanceId_shouldActivateJob() {
    // given

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // suspended job definitions and corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // the job definition
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    assertThat(job.isSuspended()).isTrue();

    // when
    // the job will be activate
    managementService.activateJobByProcessInstanceId(processInstance.getId());

    // then
    // the job should be suspended
    assertThat(jobQuery.active().count()).isEqualTo(1);
    assertThat(jobQuery.suspended().count()).isZero();

    Job suspendedJob = jobQuery.active().singleResult();

    assertThat(suspendedJob.getId()).isEqualTo(job.getId());
    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionId_shouldActivateJob() {
    // given
    // a deployed process definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // suspended job definitions and corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    assertThat(job.isSuspended()).isTrue();

    // when
    // the job will be activated
    managementService.activateJobByProcessDefinitionId(processDefinition.getId());

    // then
    // the job should be active
    assertThat(jobQuery.active().count()).isEqualTo(1);
    assertThat(jobQuery.suspended().count()).isZero();

    Job activeJob = jobQuery.active().singleResult();

    assertThat(activeJob.getId()).isEqualTo(job.getId());
    assertThat(activeJob.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionKey_shouldActivateJob() {
    // given
    // a deployed process definition
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // suspended job definitions and corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    assertThat(job.isSuspended()).isTrue();

    // when
    // the job will be activated
    managementService.activateJobByProcessDefinitionKey(processDefinition.getKey());

    // then
    // the job should be suspended
    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job activeJob = jobQuery.active().singleResult();

    assertThat(activeJob.getId()).isEqualTo(job.getId());
    assertThat(activeJob.isSuspended()).isFalse();
  }

  @Test
  void testMultipleActivationByProcessDefinitionKey_shouldActivateJob() {
    // given
    String key = "suspensionProcess";

    // Deploy three processes and start for each deployment a process instance
    // with a failed job
    int nrOfProcessDefinitions = 3;
    for (int i=0; i < nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn").deploy();
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // suspended job definitions and corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey(key, true);

    // when
    // the job will be suspended
    managementService.activateJobByProcessDefinitionKey(key);

    // then
    // the job should be activated
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(3);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByIdUsingBuilder() {
    // given

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // suspended job definitions and corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    Job job = jobQuery.singleResult();
    assertThat(job.isSuspended()).isTrue();

    // when
    // the job will be activated
    managementService
      .updateJobSuspensionState()
      .byJobId(job.getId())
      .activate();

    // then
    // the job should be active
    assertThat(jobQuery.active().count()).isEqualTo(1);
    assertThat(jobQuery.suspended().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByJobDefinitionIdUsingBuilder() {
    // given

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // suspended job definitions and corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // the job will be activated
    managementService
      .updateJobSuspensionState()
      .byJobDefinitionId(jobDefinition.getId())
      .activate();

    // then
    // the job should be active
    assertThat(jobQuery.active().count()).isEqualTo(1);
    assertThat(jobQuery.suspended().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessInstanceIdUsingBuilder() {
    // given

    // a running process instance with a failed job
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // suspended job definitions and corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    // when
    // the job will be activated
    managementService
      .updateJobSuspensionState()
      .byProcessInstanceId(processInstance.getId())
      .activate();

    // then
    // the job should be active
    assertThat(jobQuery.active().count()).isEqualTo(1);
    assertThat(jobQuery.suspended().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionIdUsingBuilder() {
    // given

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // suspended job definitions and corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // when
    // the job will be activated
    managementService
      .updateJobSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .activate();

    // then
    // the job should be active
    assertThat(jobQuery.active().count()).isEqualTo(1);
    assertThat(jobQuery.suspended().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionKeyUsingBuilder() {
    // given

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // suspended job definitions and corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // the failed job
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    // when
    // the job will be activated
    managementService
      .updateJobSuspensionState()
      .byProcessDefinitionKey("suspensionProcess")
      .activate();

    // then
    // the job should be active
    assertThat(jobQuery.active().count()).isEqualTo(1);
    assertThat(jobQuery.suspended().count()).isZero();
  }

}
