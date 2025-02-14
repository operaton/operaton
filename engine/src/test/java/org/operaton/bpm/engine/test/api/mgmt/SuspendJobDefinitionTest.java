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
import org.operaton.bpm.engine.impl.jobexecutor.TimerSuspendJobDefinitionHandler;
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

/**
 * @author roman.smirnov
 */
public class SuspendJobDefinitionTest extends PluggableProcessEngineTest {

  @After
  public void tearDown() {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    commandExecutor.execute(commandContext -> {
      commandContext.getHistoricJobLogManager().deleteHistoricJobLogsByHandlerType(TimerSuspendJobDefinitionHandler.TYPE);
      return null;
    });

  }

  // Test ManagementService#suspendJobDefinitionById() /////////////////////////

  @Test
  public void testSuspensionById_shouldThrowProcessEngineException() {
    try {
      managementService.suspendJobDefinitionById(null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testSuspensionByIdAndSuspendJobsFlag_shouldThrowProcessEngineException() {
    try {
      managementService.suspendJobDefinitionById(null, false);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.suspendJobDefinitionById(null, true);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testSuspensionByIdAndSuspendJobsFlagAndExecutionDate_shouldThrowProcessEngineException() {
    try {
      managementService.suspendJobDefinitionById(null, false, null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.suspendJobDefinitionById(null, true, null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    Date suspensionDate = new Date();
    try {
      managementService.suspendJobDefinitionById(null, false, suspensionDate);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.suspendJobDefinitionById(null, true, suspensionDate);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionById_shouldRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionById(jobDefinition.getId());

    // then
    // there exists a suspended job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());

    // there does not exist any active job definition
    jobDefinitionQuery = managementService.createJobDefinitionQuery().active();
    assertThat(jobDefinitionQuery.list()).isEmpty();

    // the corresponding job is still active
    JobQuery jobQuery = managementService.createJobQuery().active();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job activeJob = jobQuery.singleResult();
    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());

    assertThat(activeJob.isSuspended()).isFalse();

    jobQuery = managementService.createJobQuery().suspended();
    assertThat(jobQuery.count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByIdAndSuspendJobsFlag_shouldRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionById(jobDefinition.getId(), false);

    // then
    // there exists a suspended job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the corresponding job is still active
    JobQuery jobQuery = managementService.createJobQuery().active();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job activeJob = jobQuery.singleResult();
    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());

    assertThat(activeJob.isSuspended()).isFalse();

    jobQuery = managementService.createJobQuery().suspended();
    assertThat(jobQuery.count()).isEqualTo(0);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByIdAndSuspendJobsFlag_shouldSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionById(jobDefinition.getId(), true);

    // then
    // there exists a suspended job definition...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // ...and a suspended job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery().suspended();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job suspendedJob = jobQuery.singleResult();
    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionById_shouldExecuteImmediatelyAndRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionById(jobDefinition.getId(), false, null);

    // then
    // there exists a suspended job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the corresponding job is still active
    JobQuery jobQuery = managementService.createJobQuery().active();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job activeJob = jobQuery.singleResult();
    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());

    assertThat(activeJob.isSuspended()).isFalse();

    jobQuery = managementService.createJobQuery().suspended();
    assertThat(jobQuery.count()).isEqualTo(0);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionById_shouldExecuteImmediatelyAndSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionById(jobDefinition.getId(), true, null);

    // then
    // there exists a suspended job definition...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // ...and a suspended job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery().suspended();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job suspendedJob = jobQuery.singleResult();
    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionById_shouldExecuteDelayedAndRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionById(jobDefinition.getId(), false, oneWeekLater());

    // then
    // the job definition is still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    // there exists a job for the delayed suspension execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedSuspensionJob = jobQuery.timers().active().singleResult();
    assertThat(delayedSuspensionJob).isNotNull();
    String deploymentId = repositoryService.createProcessDefinitionQuery()
        .processDefinitionId(jobDefinition.getProcessDefinitionId()).singleResult().getDeploymentId();
    assertThat(delayedSuspensionJob.getDeploymentId()).isEqualTo(deploymentId);

    // execute job
    managementService.executeJob(delayedSuspensionJob.getId());

    // the job definition should be suspended
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the corresponding job is still active
    jobQuery = managementService.createJobQuery().active();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job activeJob = jobQuery.singleResult();
    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());

    assertThat(activeJob.isSuspended()).isFalse();

    jobQuery = managementService.createJobQuery().suspended();
    assertThat(jobQuery.count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionById_shouldExecuteDelayedAndSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionById(jobDefinition.getId(), true, oneWeekLater());

    // then
    // the job definition is still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    // there exists a job for the delayed suspension execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedSuspensionJob = jobQuery.timers().active().singleResult();
    assertThat(delayedSuspensionJob).isNotNull();
    String deploymentId = repositoryService.createProcessDefinitionQuery()
        .processDefinitionId(jobDefinition.getProcessDefinitionId()).singleResult().getDeploymentId();
    assertThat(delayedSuspensionJob.getDeploymentId()).isEqualTo(deploymentId);

    // execute job
    managementService.executeJob(delayedSuspensionJob.getId());

    // the job definition should be suspended
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the corresponding job is still suspended
    jobQuery = managementService.createJobQuery().suspended();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job suspendedJob = jobQuery.singleResult();
    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());

    assertThat(suspendedJob.isSuspended()).isTrue();

    jobQuery = managementService.createJobQuery().active();
    assertThat(jobQuery.count()).isEqualTo(0);
  }

  // Test ManagementService#suspendJobDefinitionByProcessDefinitionId() /////////////////////////

  @Test
  public void testSuspensionByProcessDefinitionId_shouldThrowProcessEngineException() {
    try {
      managementService.suspendJobDefinitionByProcessDefinitionId(null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testSuspensionByProcessDefinitionIdAndSuspendJobsFlag_shouldThrowProcessEngineException() {
    try {
      managementService.suspendJobDefinitionByProcessDefinitionId(null, false);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.suspendJobDefinitionByProcessDefinitionId(null, true);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testSuspensionByProcessDefinitionIdAndSuspendJobsFlagAndExecutionDate_shouldThrowProcessEngineException() {
    Date suspensionDate = new Date();

    try {
      managementService.suspendJobDefinitionByProcessDefinitionId(null, false, null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.suspendJobDefinitionByProcessDefinitionId(null, true, null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.suspendJobDefinitionByProcessDefinitionId(null, false, suspensionDate);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.suspendJobDefinitionByProcessDefinitionId(null, true, suspensionDate);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionId_shouldRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionId(processDefinition.getId());

    // then
    // there exists a suspended job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());

    // there does not exist any active job definition
    jobDefinitionQuery = managementService.createJobDefinitionQuery().active();
    assertThat(jobDefinitionQuery.list()).isEmpty();

    // the corresponding job is still active
    JobQuery jobQuery = managementService.createJobQuery().active();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job activeJob = jobQuery.singleResult();
    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());

    assertThat(activeJob.isSuspended()).isFalse();

    jobQuery = managementService.createJobQuery().suspended();
    assertThat(jobQuery.count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionIdAndSuspendJobsFlag_shouldRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionId(processDefinition.getId(), false);

    // then
    // there exists a suspended job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the corresponding job is still active
    JobQuery jobQuery = managementService.createJobQuery().active();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job activeJob = jobQuery.singleResult();
    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());

    assertThat(activeJob.isSuspended()).isFalse();

    jobQuery = managementService.createJobQuery().suspended();
    assertThat(jobQuery.count()).isEqualTo(0);

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionIdAndSuspendJobsFlag_shouldSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionId(processDefinition.getId(), true);

    // then
    // there exists a suspended job definition...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // ...and a suspended job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery().suspended();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job suspendedJob = jobQuery.singleResult();
    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionId_shouldExecuteImmediatelyAndRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionId(processDefinition.getId(), false, null);

    // then
    // there exists a suspended job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the corresponding job is still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job activeJob = jobQuery.active().singleResult();
    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());

    assertThat(activeJob.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionId_shouldExecuteImmediatelyAndSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionId(processDefinition.getId(), true, null);

    // then
    // there exists a suspended job definition...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // ...and a suspended job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery().suspended();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job suspendedJob = jobQuery.singleResult();
    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionId_shouldExecuteDelayedAndRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionId(processDefinition.getId(), false, oneWeekLater());

    // then
    // the job definition is still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    // there exists a job for the delayed suspension execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedSuspensionJob = jobQuery.timers().active().singleResult();
    assertThat(delayedSuspensionJob).isNotNull();
    assertThat(delayedSuspensionJob.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());

    // execute job
    managementService.executeJob(delayedSuspensionJob.getId());

    // the job definition should be suspended
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the corresponding job is still active
    jobQuery = managementService.createJobQuery().active();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job activeJob = jobQuery.singleResult();
    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());

    assertThat(activeJob.isSuspended()).isFalse();

    jobQuery = managementService.createJobQuery().suspended();
    assertThat(jobQuery.count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionId_shouldExecuteDelayedAndSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionId(processDefinition.getId(), true, oneWeekLater());

    // then
    // the job definition is still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    // there exists a job for the delayed suspension execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedSuspensionJob = jobQuery.timers().active().singleResult();
    assertThat(delayedSuspensionJob).isNotNull();
    assertThat(delayedSuspensionJob.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());

    // execute job
    managementService.executeJob(delayedSuspensionJob.getId());

    // the job definition should be suspended
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the corresponding job is suspended
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(0);
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  // Test ManagementService#suspendJobDefinitionByProcessDefinitionKey() /////////////////////////

  @Test
  public void testSuspensionByProcessDefinitionKey_shouldThrowProcessEngineException() {
    try {
      managementService.suspendJobDefinitionByProcessDefinitionKey(null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testSuspensionByProcessDefinitionKeyAndSuspendJobsFlag_shouldThrowProcessEngineException() {
    try {
      managementService.suspendJobDefinitionByProcessDefinitionKey(null, false);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.suspendJobDefinitionByProcessDefinitionKey(null, true);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }
  }

  @Test
  public void testSuspensionByProcessDefinitionKeyAndSuspendJobsFlagAndExecutionDate_shouldThrowProcessEngineException() {
    Date suspensionDate = new Date();
    try {
      managementService.suspendJobDefinitionByProcessDefinitionKey(null, false, null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.suspendJobDefinitionByProcessDefinitionKey(null, true, null);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.suspendJobDefinitionByProcessDefinitionKey(null, false, suspensionDate);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

    try {
      managementService.suspendJobDefinitionByProcessDefinitionKey(null, true, suspensionDate);
      fail("A ProcessEngineException was expected.");
    } catch (ProcessEngineException e) {
      // expected
    }

  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionKey_shouldRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(processDefinition.getKey());

    // then
    // there exists a suspended job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());

    // there does not exist any active job definition
    jobDefinitionQuery = managementService.createJobDefinitionQuery().active();
    assertThat(jobDefinitionQuery.list()).isEmpty();

    // the corresponding job is still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job activeJob = jobQuery.active().singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionKeyAndSuspendJobsFlag_shouldRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(processDefinition.getKey(), false);

    // then
    // there exists a suspended job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the corresponding job is still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job activeJob = jobQuery.active().singleResult();
    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());

    assertThat(activeJob.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionKeyAndSuspendJobsFlag_shouldSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(processDefinition.getKey(), true);

    // then
    // there exists a suspended job definition...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // ...and a suspended job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery().suspended();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job suspendedJob = jobQuery.singleResult();
    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionKey_shouldExecuteImmediatelyAndRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(processDefinition.getKey(), false, null);

    // then
    // there exists a suspended job definition
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the corresponding job is still active
    JobQuery jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job activeJob = jobQuery.active().singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionKey_shouldExecuteImmediatelyAndSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(processDefinition.getKey(), true, null);

    // then
    // there exists a suspended job definition...
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery().suspended();

    assertThat(jobDefinitionQuery.count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // ...and a suspended job of the provided job definition
    JobQuery jobQuery = managementService.createJobQuery().suspended();

    assertThat(jobQuery.count()).isEqualTo(1);

    Job suspendedJob = jobQuery.singleResult();
    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionKey_shouldExecuteDelayedAndRetainJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(processDefinition.getKey(), false, oneWeekLater());

    // then
    // the job definition is still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    // there exists a job for the delayed suspension execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedSuspensionJob = jobQuery.timers().active().singleResult();
    assertThat(delayedSuspensionJob).isNotNull();
    assertThat(delayedSuspensionJob.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());

    // execute job
    managementService.executeJob(delayedSuspensionJob.getId());

    // the job definition should be suspended
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the corresponding job is still active
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(1);

    Job activeJob = jobQuery.active().singleResult();

    assertThat(activeJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(activeJob.isSuspended()).isFalse();
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionKey_shouldExecuteDelayedAndSuspendJobs() {
    // given
    // a deployed process definition with asynchronous continuation
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // a running process instance with a failed job
    Map<String, Object> params = new HashMap<>();
    params.put("fail", Boolean.TRUE);
    runtimeService.startProcessInstanceByKey("suspensionProcess", params);

    // a job definition (which was created for the asynchronous continuation)
    JobDefinition jobDefinition = managementService.createJobDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(processDefinition.getKey(), true, oneWeekLater());

    // then
    // the job definition is still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(1);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    // there exists a job for the delayed suspension execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedSuspensionJob = jobQuery.timers().active().singleResult();
    assertThat(delayedSuspensionJob).isNotNull();
    assertThat(delayedSuspensionJob.getDeploymentId()).isEqualTo(processDefinition.getDeploymentId());

    // execute job
    managementService.executeJob(delayedSuspensionJob.getId());

    // the job definition should be suspended
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(1);

    JobDefinition suspendedJobDefinition = jobDefinitionQuery.suspended().singleResult();

    assertThat(suspendedJobDefinition.getId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJobDefinition.isSuspended()).isTrue();

    // the corresponding job is suspended
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(0);
    assertThat(jobQuery.suspended().count()).isEqualTo(1);

    Job suspendedJob = jobQuery.suspended().singleResult();

    assertThat(suspendedJob.getJobDefinitionId()).isEqualTo(jobDefinition.getId());
    assertThat(suspendedJob.isSuspended()).isTrue();
  }

  // Test ManagementService#suspendJobDefinitionByProcessDefinitionKey() with multiple process definition
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

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(key);

    // then
    // all job definitions are suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(3);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);

    // but the jobs are still active
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(3);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  public void testMultipleSuspensionByProcessDefinitionKeyAndSuspendJobsFlag_shouldRetainJobs() {
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

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(key, false);

    // then
    // all job definitions are suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(3);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);

    // but the jobs are still active
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(3);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Test
  public void testMultipleSuspensionByProcessDefinitionKeyAndSuspendJobsFlag_shouldSuspendJobs() {
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

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(key, true);

    // then
    // all job definitions are suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(3);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);

    // and the jobs too
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(3);
    assertThat(jobQuery.active().count()).isEqualTo(0);

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

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(key, false, null);

    // then
    // all job definitions are suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(3);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);

    // but the jobs are still active
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(3);

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

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(key, true, null);

    // then
    // all job definitions are suspended
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(3);
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);

    // and the jobs too
    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(3);
    assertThat(jobQuery.active().count()).isEqualTo(0);

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

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(key, false, oneWeekLater());

    // then
    // the job definition is still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    // there exists a job for the delayed suspension execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedSuspensionJob = jobQuery.timers().active().singleResult();
    assertThat(delayedSuspensionJob).isNotNull();
    String expectedDeploymentId = repositoryService.createProcessDefinitionQuery()
        .orderByProcessDefinitionVersion().desc().list().get(0).getDeploymentId();
    assertThat(delayedSuspensionJob.getDeploymentId()).isEqualTo(expectedDeploymentId);

    // execute job
    managementService.executeJob(delayedSuspensionJob.getId());

    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(3);

    // but the jobs are still active
    jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(3);

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

    // when
    // suspend the job definition
    managementService.suspendJobDefinitionByProcessDefinitionKey(key, true, oneWeekLater());

    // then
    // the job definitions are still active
    JobDefinitionQuery jobDefinitionQuery = managementService.createJobDefinitionQuery();
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(3);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(0);

    // there exists a job for the delayed suspension execution
    JobQuery jobQuery = managementService.createJobQuery();

    Job delayedSuspensionJob = jobQuery.timers().active().singleResult();
    assertThat(delayedSuspensionJob).isNotNull();
    String expectedDeploymentId = repositoryService.createProcessDefinitionQuery()
        .orderByProcessDefinitionVersion().desc().list().get(0).getDeploymentId();
    assertThat(delayedSuspensionJob.getDeploymentId()).isEqualTo(expectedDeploymentId);

    // execute job
    managementService.executeJob(delayedSuspensionJob.getId());

    // the job definition should be suspended
    assertThat(jobDefinitionQuery.active().count()).isEqualTo(0);
    assertThat(jobDefinitionQuery.suspended().count()).isEqualTo(3);

    // the corresponding jobs are suspended
    jobQuery = managementService.createJobQuery();

    assertThat(jobQuery.active().count()).isEqualTo(0);
    assertThat(jobQuery.suspended().count()).isEqualTo(3);

    // Clean DB
    for (org.operaton.bpm.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByIdUsingBuilder() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // a job definition (which was created for the asynchronous continuation)
    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    JobDefinition jobDefinition = query.singleResult();
    assertThat(jobDefinition.isSuspended()).isFalse();

    // when
    // suspend the job definition
    managementService
      .updateJobDefinitionSuspensionState()
      .byJobDefinitionId(jobDefinition.getId())
      .suspend();

    // then
    // there exists a suspended job definition
    assertThat(query.suspended().count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionIdUsingBuilder() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // a job definition (which was created for the asynchronous continuation)
    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    JobDefinition jobDefinition = query.singleResult();
    assertThat(jobDefinition.isSuspended()).isFalse();

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

    // when
    // suspend the job definition
    managementService
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionId(processDefinition.getId())
      .suspend();

    // then
    // there exists a suspended job definition
    assertThat(query.suspended().count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionByProcessDefinitionKeyUsingBuilder() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // a job definition (which was created for the asynchronous continuation)
    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    JobDefinition jobDefinition = query.singleResult();
    assertThat(jobDefinition.isSuspended()).isFalse();

    // when
    // suspend the job definition
    managementService
      .updateJobDefinitionSuspensionState()
      .byProcessDefinitionKey("suspensionProcess")
      .suspend();

    // then
    // there exists a suspended job definition
    assertThat(query.suspended().count()).isEqualTo(1);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testSuspensionJobDefinitionIncludeJobsdUsingBuilder() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // a job definition (which was created for the asynchronous continuation)
    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    JobDefinition jobDefinition = query.singleResult();
    assertThat(jobDefinition.isSuspended()).isFalse();

    JobQuery jobQuery = managementService.createJobQuery();
    assertThat(jobQuery.suspended().count()).isEqualTo(0);
    assertThat(jobQuery.active().count()).isEqualTo(1);


    // when
    // suspend the job definition and the job
    managementService
      .updateJobDefinitionSuspensionState()
      .byJobDefinitionId(jobDefinition.getId())
      .includeJobs(true)
      .suspend();

    // then
    // there exists a suspended job definition and job
    assertThat(query.suspended().count()).isEqualTo(1);

    assertThat(jobQuery.suspended().count()).isEqualTo(1);
    assertThat(jobQuery.active().count()).isEqualTo(0);
  }

  @Deployment(resources = {"org/operaton/bpm/engine/test/api/mgmt/SuspensionTest.testBase.bpmn"})
  @Test
  public void testDelayedSuspensionUsingBuilder() {
    // given
    // a deployed process definition with asynchronous continuation

    // a running process instance with a failed job
    runtimeService.startProcessInstanceByKey("suspensionProcess",
        Variables.createVariables().putValue("fail", true));

    // a job definition (which was created for the asynchronous continuation)
    JobDefinitionQuery query = managementService.createJobDefinitionQuery();
    JobDefinition jobDefinition = query.singleResult();

    // when
    // suspend the job definition in one week
    managementService
      .updateJobDefinitionSuspensionState()
      .byJobDefinitionId(jobDefinition.getId())
      .executionDate(oneWeekLater())
      .suspend();

    // then
    // the job definition is still active
    assertThat(query.active().count()).isEqualTo(1);
    assertThat(query.suspended().count()).isEqualTo(0);

    // there exists a job for the delayed suspension execution
    Job delayedSuspensionJob = managementService.createJobQuery().timers().active().singleResult();
    assertThat(delayedSuspensionJob).isNotNull();
    String expectedDeploymentId = repositoryService.createProcessDefinitionQuery()
        .processDefinitionId(jobDefinition.getProcessDefinitionId()).singleResult().getDeploymentId();
    assertThat(delayedSuspensionJob.getDeploymentId()).isEqualTo(expectedDeploymentId);

    // execute job
    managementService.executeJob(delayedSuspensionJob.getId());

    // the job definition should be suspended
    assertThat(query.active().count()).isEqualTo(0);
    assertThat(query.suspended().count()).isEqualTo(1);
  }

  protected Date oneWeekLater() {
    Date startTime = new Date();
    ClockUtil.setCurrentTime(startTime);
    long oneWeekFromStartTime = startTime.getTime() + (7 * 24 * 60 * 60 * 1000);
    return new Date(oneWeekFromStartTime);
  }

}
