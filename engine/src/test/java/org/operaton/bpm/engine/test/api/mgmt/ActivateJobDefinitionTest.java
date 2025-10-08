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
package org.operaton.bpm.engine.test.api.mgmt;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.TimerActivateJobDefinitionHandler;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.variable.Variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(ProcessEngineExtension.class)
class ActivateJobDefinitionTest {

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected ManagementService managementService;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;

  @AfterEach
  void tearDown() {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerActivateJobDefinitionHandler.TYPE);
      return null;
    });
  }

  // Test ManagementService#activateJobDefinitionById() /////////////////////////

  @Test
  void testActivationById_shouldThrowProcessEngineException() {
    assertThatThrownBy(() -> managementService.activateJobDefinitionById(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testActivationByIdAndActivateJobsFlag_shouldThrowProcessEngineException() {
    assertThatThrownBy(() -> managementService.activateJobDefinitionById(null, false)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> managementService.activateJobDefinitionById(null, true)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testActivationByIdAndActivateJobsFlagAndExecutionDate_shouldThrowProcessEngineException() {
    Date activationDate = new Date();
    assertThatThrownBy(() -> managementService.activateJobDefinitionById(null, false, null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> managementService.activateJobDefinitionById(null, true, null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> managementService.activateJobDefinitionById(null, false, activationDate)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> managementService.activateJobDefinitionById(null, true, activationDate)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationById_shouldRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionById(jobDefinition.getId());

    // then
    // there exists an active job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.active().count()).isOne();
    assertThat(jobDefinitionQuery.suspended().count()).isZero();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job suspendedJob = jobQuery.singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByIdAndActivateJobsFlag_shouldRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionById(jobDefinition.getId(), false);

    // then
    // there exists an active job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job suspendedJob = jobQuery.singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByIdAndActivateJobsFlag_shouldSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionById(jobDefinition.getId(), true);

    // then
    // there exists an active job definition...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // ...and an active job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job activeJob = jobQuery.singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationById_shouldExecuteImmediatelyAndRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionById(jobDefinition.getId(), false, null);

    // then
    // there exists an active job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job suspendedJob = jobQuery.singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationById_shouldExecuteImmediatelyAndSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionById(jobDefinition.getId(), true, null);

    // then
    // there exists an active job definition...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // ...and an active job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job activeJob = jobQuery.singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationById_shouldExecuteDelayedAndRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionById(jobDefinition.getId(), false, oneWeekLater());

    // then
    // the job definition is still suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    // there exists a job for the delayed activation execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedActivationJob = jobQuery.timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    String deploymentId = repositoryService.createProcessDefinitionQuery()
        .processDefinitionId(jobDefinition.getProcessDefinitionId()).singleResult().getDeploymentId();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(deploymentId);

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be suspended
    assertThat(jobDefinitionQuery.active().count()).isOne();
    assertThat(jobDefinitionQuery.suspended().count()).isZero();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job suspendedJob = jobQuery.singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationById_shouldExecuteDelayedAndSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionById(jobDefinition.getId(), true, oneWeekLater());

    // then
    // the job definition is still suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    // there exists a job for the delayed activation execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedActivationJob = jobQuery.timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    String deploymentId = repositoryService.createProcessDefinitionQuery()
        .processDefinitionId(jobDefinition.getProcessDefinitionId()).singleResult().getDeploymentId();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(deploymentId);

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be active
    assertThat(jobDefinitionQuery.active().count()).isOne();
    assertThat(jobDefinitionQuery.suspended().count()).isZero();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is active
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job activeJob = jobQuery.singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();
  }

  // Test ManagementService#activateJobDefinitionByProcessDefinitionId() /////////////////////////

  @Test
  void testActivationByProcessDefinitionId_shouldThrowProcessEngineException() {
    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionId(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testActivationByProcessDefinitionIdAndActivateJobsFlag_shouldThrowProcessEngineException() {
    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionId(null, false)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionId(null, true)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testActivationByProcessDefinitionIdAndActivateJobsFlagAndExecutionDate_shouldThrowProcessEngineException() {
    Date activationDate = new Date();
    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionId(null, false, null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionId(null, true, null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionId(null, false, activationDate)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionId(null, true, activationDate)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionId_shouldRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionId(processDefinition.getId());

    // then
    // there exists an active job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionIdAndActivateJobsFlag_shouldRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionId(processDefinition.getId(), false);

    // then
    // there exists an active job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionIdAndActivateJobsFlag_shouldSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionId(processDefinition.getId(), true);

    // then
    // there exists an active job definition...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // ...and an active job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job activeJob = jobQuery.singleResult();
    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionId_shouldExecuteImmediatelyAndRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionId(processDefinition.getId(), false, null);

    // then
    // there exists an active job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isOne();
    assertThat(jobQuery.active().count()).isZero();

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionId_shouldExecuteImmediatelyAndSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionId(processDefinition.getId(), true, null);

    // then
    // there exists an active job definition...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // ...and an active job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job activeJob = jobQuery.singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionId_shouldExecuteDelayedAndRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionId(processDefinition.getId(), false, oneWeekLater());

    // then
    // the job definition is still suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    // there exists a job for the delayed activation execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedActivationJob = jobQuery.timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be active
    assertThat(jobDefinitionQuery.active().count()).isOne();
    assertThat(jobDefinitionQuery.suspended().count()).isZero();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionId_shouldExecuteDelayedAndSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionId(processDefinition.getId(), true, oneWeekLater());

    // then
    // the job definition is still suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    // there exists a job for the delayed activation execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedActivationJob = jobQuery.timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be active
    assertThat(jobDefinitionQuery.active().count()).isOne();
    assertThat(jobDefinitionQuery.suspended().count()).isZero();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is active
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isOne();
    assertThat(jobQuery.suspended().count()).isZero();

    Job activeJob = jobQuery.active().singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();
  }

  // Test ManagementService#activateJobDefinitionByProcessDefinitionKey() /////////////////////////

  @Test
  void testActivationByProcessDefinitionKey_shouldThrowProcessEngineException() {
    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionKey(null)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testActivationByProcessDefinitionKeyAndActivateJobsFlag_shouldThrowProcessEngineException() {
    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionKey(null, false)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionKey(null, true)).isInstanceOf(ProcessEngineException.class);
  }

  @Test
  void testActivationByProcessDefinitionKeyAndActivateJobsFlagAndExecutionDate_shouldThrowProcessEngineException() {
    Date activationDate = new Date();
    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionKey(null, false, null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionKey(null, true, null)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionKey(null, false, activationDate)).isInstanceOf(ProcessEngineException.class);

    assertThatThrownBy(() -> managementService.activateJobDefinitionByProcessDefinitionKey(null, true, activationDate)).isInstanceOf(ProcessEngineException.class);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionKey_shouldRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(processDefinition.getKey());

    // then
    // there exists an active job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isOne();
    assertThat(jobQuery.active().count()).isZero();

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionKeyAndActivateJobsFlag_shouldRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(processDefinition.getKey(), false);

    // then
    // there exists an active job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isOne();
    assertThat(jobQuery.active().count()).isZero();

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionKeyAndActivateJobsFlag_shouldSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(processDefinition.getKey(), true);

    // then
    // there exists an active job definition...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // ...and an active job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job suspendedJob = jobQuery.active().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionKey_shouldExecuteImmediatelyAndRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(processDefinition.getKey(), false, null);

    // then
    // there exists an active job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isOne();
    assertThat(jobQuery.active().count()).isZero();

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionKey_shouldExecuteImmediatelyAndSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(processDefinition.getKey(), true, null);

    // then
    // there exists an active job definition...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();

    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isOne();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // ...and an active job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isOne();

    Job suspendedJob = jobQuery.active().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionKey_shouldExecuteDelayedAndRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(processDefinition.getKey(), false, oneWeekLater());

    // then
    // the job definition is still suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    // there exists a job for the delayed activation execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedActivationJob = jobQuery.timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be active
    assertThat(jobDefinitionQuery.active().count()).isOne();
    assertThat(jobDefinitionQuery.suspended().count()).isZero();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isOne();
    assertThat(jobQuery.active().count()).isZero();

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionKey_shouldExecuteDelayedAndSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(processDefinition.getKey(), true, oneWeekLater());

    // then
    // the job definition is still suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isOne();

    // there exists a job for the delayed activation execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedActivationJob = jobQuery.timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be active
    assertThat(jobDefinitionQuery.active().count()).isOne();
    assertThat(jobDefinitionQuery.suspended().count()).isZero();

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is active
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isOne();
    assertThat(jobQuery.suspended().count()).isZero();

    Job activeJob = jobQuery.active().singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();

  }

  // Test ManagementService#activateJobDefinitionByProcessDefinitionKey() with multiple process definition
  // with same process definition key

  @Test
  void testMultipleSuspensionByProcessDefinitionKey_shouldRetainJobs() {
    // given
    String key = "suspensionProcess";

    // Deploy three processes and start for each deployment a process instance
    // with a failed job
    int nrOfProcessDefinitions = 3;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn").deploy();
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(key);

    // then
    // all job definitions are active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);

    // but the jobs are still suspended
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(3);
    assertThat(jobQuery.active().count()).isZero();

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  void testMultipleSuspensionByProcessDefinitionKeyAndActivateJobsFlag_shouldRetainJobs() {
    // given
    String key = "suspensionProcess";

    // Deploy three processes and start for each deployment a process instance
    // with a failed job
    int nrOfProcessDefinitions = 3;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn").deploy();
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(key, false);

    // then
    // all job definitions are active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);

    // but the jobs are still suspended
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(3);
    assertThat(jobQuery.active().count()).isZero();

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  void testMultipleSuspensionByProcessDefinitionKeyAndActivateJobsFlag_shouldSuspendJobs() {
    // given
    String key = "suspensionProcess";

    // Deploy three processes and start for each deployment a process instance
    // with a failed job
    int nrOfProcessDefinitions = 3;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn").deploy();
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(key, true);

    // then
    // all job definitions are active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);

    // and the jobs too
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(3);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  @Test
  void testMultipleSuspensionByProcessDefinitionKey_shouldExecuteImmediatelyAndRetainJobs() {
    // given
    String key = "suspensionProcess";

    // Deploy three processes and start for each deployment a process instance
    // with a failed job
    int nrOfProcessDefinitions = 3;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn").deploy();
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(key, false, null);

    // then
    // all job definitions are active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);

    // but the jobs are still suspended
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(3);
    assertThat(jobQuery.active().count()).isZero();

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  @Test
  void testMultipleSuspensionByProcessDefinitionKey_shouldExecuteImmediatelyAndSuspendJobs() {
    // given
    String key = "suspensionProcess";

    // Deploy three processes and start for each deployment a process instance
    // with a failed job
    int nrOfProcessDefinitions = 3;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn").deploy();
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(key, true, null);

    // then
    // all job definitions are active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.suspended().count()).isZero();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);

    // and the jobs too
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isZero();
    assertThat(jobQuery.active().count()).isEqualTo(3);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  @Test
  void testMultipleSuspensionByProcessDefinitionKey_shouldExecuteDelayedAndRetainJobs() {
    // given
    String key = "suspensionProcess";

    // Deploy three processes and start for each deployment a process instance
    // with a failed job
    int nrOfProcessDefinitions = 3;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn").deploy();
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(key, false, oneWeekLater());

    // then
    // the job definition is still suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(3);

    // there exists a job for the delayed activation execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedActivationJob = jobQuery.timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    String expectedDeploymentId = repositoryService.createProcessDefinitionQuery()
        .orderByProcessDefinitionVersion().desc().list().get(0).getDeploymentId();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(expectedDeploymentId);

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be active
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);
    assertThat(jobDefinitionQuery.suspended().count()).isZero();

    // but the jobs are still suspended
    jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(3);
    assertThat(jobQuery.active().count()).isZero();

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  @Test
  void testMultipleSuspensionByProcessDefinitionKey_shouldExecuteDelayedAndSuspendJobs() {
    // given
    String key = "suspensionProcess";

    // Deploy three processes and start for each deployment a process instance
    // with a failed job
    int nrOfProcessDefinitions = 3;
    for (int i=0; i<nrOfProcessDefinitions; i++) {
      repositoryService.createDeployment()
        .addClasspathResource("org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn").deploy();
      Map<String, Object> params = new HashMap<>();
      params.put("fail", Boolean.TRUE);
      runtimeService.startProcessInstanceByKey(key, params);
    }

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    // when
    // activate the job definition
    managementService.activateJobDefinitionByProcessDefinitionKey(key, true, oneWeekLater());

    // then
    // the job definitions are still suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isZero();
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(3);

    // there exists a job for the delayed activation execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedActivationJob = jobQuery.timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    String expectedDeploymentId = repositoryService.createProcessDefinitionQuery()
        .orderByProcessDefinitionVersion().desc().list().get(0).getDeploymentId();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(expectedDeploymentId);

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be active
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);
    assertThat(jobDefinitionQuery.suspended().count()).isZero();

    // the corresponding jobs are active
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(3);
    assertThat(jobQuery.suspended().count()).isZero();

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByIdUsingBuilder() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    JobDefinition jobDefinition = query.singleResult();

    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isOne();

    // when
    // activate the job definition
    managementService
      .updateJobDefinitionSuspensionState()
      .byJobDefinitionId(jobDefinition.getId())
      .activate();

    // then
    // there exists an active job definition
    assertThat(query.active().count()).isOne();
    assertThat(query.suspended().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionIdUsingBuilder() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isOne();

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // when
    // activate the job definition
    managementService
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .activate();

    // then
    // there exists an active job definition
    assertThat(query.active().count()).isOne();
    assertThat(query.suspended().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationByProcessDefinitionKeyUsingBuilder() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isOne();

    // when
    // activate the job definition
    managementService
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey("suspensionProcess")
      .activate();

    // then
    // there exists an active job definition
    assertThat(query.active().count()).isOne();
    assertThat(query.suspended().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testActivationJobDefinitionIncludingJobsUsingBuilder() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    JobDefinition jobDefinition = query.singleResult();

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.active().count()).isZero();
    assertThat(jobQuery.suspended().count()).isOne();

    // when
    // activate the job definition
    managementService
      .updateJobDefinitionSuspensionState()
      .byJobDefinitionId(jobDefinition.getId())
      .includeJobs(true)
      .activate();

    // then
    // there exists an active job definition
    assertThat(jobQuery.active().count()).isOne();
    assertThat(jobQuery.suspended().count()).isZero();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  void testDelayedActivationUsingBuilder() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    JobDefinition jobDefinition = query.singleResult();

    // when
    // activate the job definition
    managementService
      .updateJobDefinitionSuspensionState()
      .byJobDefinitionId(jobDefinition.getId())
      .executionDate(oneWeekLater())
      .activate();

    // then
    // the job definition is still suspended
    assertThat(query.active().count()).isZero();
    assertThat(query.suspended().count()).isOne();

    // there exists a job for the delayed activation execution
    Job delayedActivationJob = managementService.createJobQuery().timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    String expectedDeploymentId = repositoryService.createProcessDefinitionQuery()
        .processDefinitionId(jobDefinition.getProcessDefinitionId()).singleResult().getDeploymentId();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(expectedDeploymentId);

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be suspended
    assertThat(query.active().count()).isOne();
    assertThat(query.suspended().count()).isZero();
  }

  protected Date oneWeekLater() {
    // one week from now
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    long oneWeekFromStartTime = startTime.getTime() + (7 * 24 * 60 * 60 * 1000);
    return new Date(oneWeekFromStartTime);
  }

}
