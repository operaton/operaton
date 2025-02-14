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

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.TimerActivateJobDefinitionHandler;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.management.JobDefinition;
import org.operaton.bpm.engine.management.JobDefinitionQuery;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.JobQuery;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.bpm.engine.variable.Variables;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ActivateJobDefinitionTest extends PluggableProcessEngineTest {

  @After
  public void tearDown() {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerActivateJobDefinitionHandler.TYPE);
      return null;
    });
  }

  // Test ManagementService#activateJobDefinitionById() /////////////////////////

  @Test
  public void testActivationById_shouldThrowProcessEngineException() {
    try {
      managementService.activateJobDefinitionById(null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testActivationByIdAndActivateJobsFlag_shouldThrowProcessEngineException() {
    try {
      managementService.activateJobDefinitionById(null, false);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.activateJobDefinitionById(null, true);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testActivationByIdAndActivateJobsFlagAndExecutionDate_shouldThrowProcessEngineException() {
    Date activationDate = new Date();
    try {
      managementService.activateJobDefinitionById(null, false, null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.activateJobDefinitionById(null, true, null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.activateJobDefinitionById(null, false, activationDate);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.activateJobDefinitionById(null, true, activationDate);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationById_shouldRetainJobs() {
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

    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(0);
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByIdAndActivateJobsFlag_shouldRetainJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(0);
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByIdAndActivateJobsFlag_shouldSuspendJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // ...and an active job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job activeJob = jobQuery.singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationById_shouldExecuteImmediatelyAndRetainJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(0);
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationById_shouldExecuteImmediatelyAndSuspendJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // ...and an active job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job activeJob = jobQuery.singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationById_shouldExecuteDelayedAndRetainJobs() {
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
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(1);

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
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(0);
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationById_shouldExecuteDelayedAndSuspendJobs() {
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
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(1);

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
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is active
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job activeJob = jobQuery.singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();
  }

  // Test ManagementService#activateJobDefinitionByProcessDefinitionId() /////////////////////////

  @Test
  public void testActivationByProcessDefinitionId_shouldThrowProcessEngineException() {
    try {
      managementService.activateJobDefinitionByProcessDefinitionId(null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testActivationByProcessDefinitionIdAndActivateJobsFlag_shouldThrowProcessEngineException() {
    try {
      managementService.activateJobDefinitionByProcessDefinitionId(null, false);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.activateJobDefinitionByProcessDefinitionId(null, true);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testActivationByProcessDefinitionIdAndActivateJobsFlagAndExecutionDate_shouldThrowProcessEngineException() {
    Date activationDate = new Date();
    try {
      managementService.activateJobDefinitionByProcessDefinitionId(null, false, null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.activateJobDefinitionByProcessDefinitionId(null, true, null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.activateJobDefinitionByProcessDefinitionId(null, false, activationDate);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.activateJobDefinitionByProcessDefinitionId(null, true, activationDate);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionId_shouldRetainJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(0);
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionIdAndActivateJobsFlag_shouldRetainJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(0);
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionIdAndActivateJobsFlag_shouldSuspendJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // ...and an active job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job activeJob = jobQuery.singleResult();
    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionId_shouldExecuteImmediatelyAndRetainJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(1);
    assertThat(jobQuery.active().count()).isEqualTo(0);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionId_shouldExecuteImmediatelyAndSuspendJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // ...and an active job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job activeJob = jobQuery.singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionId_shouldExecuteDelayedAndRetainJobs() {
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
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(1);

    // there exists a job for the delayed activation execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedActivationJob = jobQuery.timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be active
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(0);
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionId_shouldExecuteDelayedAndSuspendJobs() {
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
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(1);

    // there exists a job for the delayed activation execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedActivationJob = jobQuery.timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be active
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is active
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(1);
    assertThat(jobQuery.suspended().count()).isEqualTo(0);

    Job activeJob = jobQuery.active().singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();
  }

  // Test ManagementService#activateJobDefinitionByProcessDefinitionKey() /////////////////////////

  @Test
  public void testActivationByProcessDefinitionKey_shouldThrowProcessEngineException() {
    try {
      managementService.activateJobDefinitionByProcessDefinitionKey(null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testActivationByProcessDefinitionKeyAndActivateJobsFlag_shouldThrowProcessEngineException() {
    try {
      managementService.activateJobDefinitionByProcessDefinitionKey(null, false);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.activateJobDefinitionByProcessDefinitionKey(null, true);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testActivationByProcessDefinitionKeyAndActivateJobsFlagAndExecutionDate_shouldThrowProcessEngineException() {
    Date activationDate = new Date();
    try {
      managementService.activateJobDefinitionByProcessDefinitionKey(null, false, null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.activateJobDefinitionByProcessDefinitionKey(null, true, null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.activateJobDefinitionByProcessDefinitionKey(null, false, activationDate);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.activateJobDefinitionByProcessDefinitionKey(null, true, activationDate);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionKey_shouldRetainJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(1);
    assertThat(jobQuery.active().count()).isEqualTo(0);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionKeyAndActivateJobsFlag_shouldRetainJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(1);
    assertThat(jobQuery.active().count()).isEqualTo(0);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionKeyAndActivateJobsFlag_shouldSuspendJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // ...and an active job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.active().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionKey_shouldExecuteImmediatelyAndRetainJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(1);
    assertThat(jobQuery.active().count()).isEqualTo(0);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionKey_shouldExecuteImmediatelyAndSuspendJobs() {
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

    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // ...and an active job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.active().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isFalse();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionKey_shouldExecuteDelayedAndRetainJobs() {
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
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(1);

    // there exists a job for the delayed activation execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedActivationJob = jobQuery.timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be active
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is still suspended
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(1);
    assertThat(jobQuery.active().count()).isEqualTo(0);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionKey_shouldExecuteDelayedAndSuspendJobs() {
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
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(1);

    // there exists a job for the delayed activation execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedActivationJob = jobQuery.timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be active
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    JobDefinition activeJobDefinition = jobDefinitionQuery.active().singleResult();

    assertThat(activeJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJobDefinition.isSuspended()).isFalse();

    // the corresponding job is active
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(1);
    assertThat(jobQuery.suspended().count()).isEqualTo(0);

    Job activeJob = jobQuery.active().singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();

  }

  // Test ManagementService#activateJobDefinitionByProcessDefinitionKey() with multiple process definition
  // with same process definition key

  @Test
  public void testMultipleSuspensionByProcessDefinitionKey_shouldRetainJobs() {
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
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);

    // but the jobs are still suspended
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(3);
    assertThat(jobQuery.active().count()).isEqualTo(0);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  public void testMultipleSuspensionByProcessDefinitionKeyAndActivateJobsFlag_shouldRetainJobs() {
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
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);

    // but the jobs are still suspended
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(3);
    assertThat(jobQuery.active().count()).isEqualTo(0);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  public void testMultipleSuspensionByProcessDefinitionKeyAndActivateJobsFlag_shouldSuspendJobs() {
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
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);

    // and the jobs too
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(3);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  @Test
  public void testMultipleSuspensionByProcessDefinitionKey_shouldExecuteImmediatelyAndRetainJobs() {
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
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);

    // but the jobs are still suspended
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(3);
    assertThat(jobQuery.active().count()).isEqualTo(0);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  @Test
  public void testMultipleSuspensionByProcessDefinitionKey_shouldExecuteImmediatelyAndSuspendJobs() {
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
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);

    // and the jobs too
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(3);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  @Test
  public void testMultipleSuspensionByProcessDefinitionKey_shouldExecuteDelayedAndRetainJobs() {
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
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
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
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    // but the jobs are still suspended
    jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(3);
    assertThat(jobQuery.active().count()).isEqualTo(0);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

  }

  @Test
  public void testMultipleSuspensionByProcessDefinitionKey_shouldExecuteDelayedAndSuspendJobs() {
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
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
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
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    // the corresponding jobs are active
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(3);
    assertThat(jobQuery.suspended().count()).isEqualTo(0);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByIdUsingBuilder() {
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

    assertThat(query.active().count()).isEqualTo(0);
    assertThat(query.suspended().count()).isEqualTo(1);

    // when
    // activate the job definition
    managementService
      .updateJobDefinitionSuspensionState()
      .byJobDefinitionId(jobDefinition.getId())
      .activate();

    // then
    // there exists an active job definition
    assertThat(query.active().count()).isEqualTo(1);
    assertThat(query.suspended().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionIdUsingBuilder() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(0);
    assertThat(query.suspended().count()).isEqualTo(1);

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // when
    // activate the job definition
    managementService
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .activate();

    // then
    // there exists an active job definition
    assertThat(query.active().count()).isEqualTo(1);
    assertThat(query.suspended().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationByProcessDefinitionKeyUsingBuilder() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // a job definition (which was created for the asynchronous continuation)
    // ...which will be suspended with the corresponding jobs
    managementService.suspendJobDefinitionByProcessDefinitionKey("suspensionProcess", true);

    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    assertThat(query.active().count()).isEqualTo(0);
    assertThat(query.suspended().count()).isEqualTo(1);

    // when
    // activate the job definition
    managementService
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey("suspensionProcess")
      .activate();

    // then
    // there exists an active job definition
    assertThat(query.active().count()).isEqualTo(1);
    assertThat(query.suspended().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testActivationJobDefinitionIncludingJobsUsingBuilder() {
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
    assertThat(jobQuery.active().count()).isEqualTo(0);
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    // when
    // activate the job definition
    managementService
      .updateJobDefinitionSuspensionState()
      .byJobDefinitionId(jobDefinition.getId())
      .includeJobs(true)
      .activate();

    // then
    // there exists an active job definition
    assertThat(jobQuery.active().count()).isEqualTo(1);
    assertThat(jobQuery.suspended().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testDelayedActivationUsingBuilder() {
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
    assertThat(query.active().count()).isEqualTo(0);
    assertThat(query.suspended().count()).isEqualTo(1);

    // there exists a job for the delayed activation execution
    Job delayedActivationJob = managementService.createJobQuery().timers().active().singleResult();
    assertThat(delayedActivationJob).isNotNull();
    String expectedDeploymentId = repositoryService.createProcessDefinitionQuery()
        .processDefinitionId(jobDefinition.getProcessDefinitionId()).singleResult().getDeploymentId();
    assertThat(delayedActivationJob.getDeploymentId()).isEqualTo(expectedDeploymentId);

    // execute job
    managementService.executeJob(delayedActivationJob.getId());

    // the job definition should be suspended
    assertThat(query.active().count()).isEqualTo(1);
    assertThat(query.suspended().count()).isEqualTo(0);
  }

  protected Date oneWeekLater() {
    // one week from now
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    long oneWeekFromStartTime = startTime.getTime() + (7 * 24 * 60 * 60 * 1000);
    return new Date(oneWeekFromStartTime);
  }

}
