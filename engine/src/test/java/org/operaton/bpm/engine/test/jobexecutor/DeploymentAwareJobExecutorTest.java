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
package org.operaton.bpm.engine.test.jobexecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.FileNotFoundException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.Page;
import org.operaton.bpm.engine.impl.cmd.AcquireJobsCmd;
import org.operaton.bpm.engine.impl.cmd.DeleteJobsCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.AcquiredJobs;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.AcquirableJobEntity;
import org.operaton.bpm.engine.impl.persistence.entity.MessageEntity;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DeploymentAwareJobExecutorTest extends PluggableProcessEngineTest {

  protected ProcessEngine otherProcessEngine = null;

  @Before
  public void setUp() {
    processEngineConfiguration.setJobExecutorDeploymentAware(true);
  }

  @After
  public void tearDown() {
    processEngineConfiguration.setJobExecutorDeploymentAware(false);
    closeDownProcessEngine();
  }

  protected void closeDownProcessEngine() {
    if (otherProcessEngine != null) {
      otherProcessEngine.close();
      ProcessEngines.unregister(otherProcessEngine);
      otherProcessEngine = null;
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  @Test
  public void testProcessingOfJobsWithMatchingDeployment() {
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    Set<String> registeredDeployments = managementService.getRegisteredDeployments();
    assertThat(registeredDeployments).containsExactly(deploymentId);

    Job executableJob = managementService.createJobQuery().singleResult();

    String otherDeploymentId =
        deployAndInstantiateWithNewEngineConfiguration(
            "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcessVersion2.bpmn20.xml");

    // assert that two jobs have been created, one for each deployment
    List<Job> jobs = managementService.createJobQuery().list();
    assertThat(jobs).hasSize(2);
    Set<String> jobDeploymentIds = new HashSet<>();
    jobDeploymentIds.add(jobs.get(0).getDeploymentId());
    jobDeploymentIds.add(jobs.get(1).getDeploymentId());

    assertThat(jobDeploymentIds).containsExactlyInAnyOrder(deploymentId, otherDeploymentId);

    // select executable jobs for executor of first engine
    AcquiredJobs acquiredJobs = getExecutableJobs(processEngineConfiguration.getJobExecutor());
    assertThat(acquiredJobs.size()).isEqualTo(1);
    assertThat(acquiredJobs.contains(executableJob.getId())).isTrue();

    repositoryService.deleteDeployment(otherDeploymentId, true);
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  @Test
  public void testExplicitDeploymentRegistration() {
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    String otherDeploymentId =
        deployAndInstantiateWithNewEngineConfiguration(
            "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcessVersion2.bpmn20.xml");

    processEngine.getManagementService().registerDeploymentForJobExecutor(otherDeploymentId);

    List<Job> jobs = managementService.createJobQuery().list();

    AcquiredJobs acquiredJobs = getExecutableJobs(processEngineConfiguration.getJobExecutor());
    assertThat(acquiredJobs.size()).isEqualTo(2);
    for (Job job : jobs) {
      assertThat(acquiredJobs.contains(job.getId())).isTrue();
    }

    repositoryService.deleteDeployment(otherDeploymentId, true);
  }

  @Test
  public void testRegistrationOfNonExistingDeployment() {
    String nonExistingDeploymentId = "some non-existing id";
    var managementService = processEngine.getManagementService();

    try {
      managementService.registerDeploymentForJobExecutor(nonExistingDeploymentId);
      fail("Registering a non-existing deployment should not succeed");
    } catch (ProcessEngineException e) {
      testRule.assertTextPresent("Deployment " + nonExistingDeploymentId + " does not exist", e.getMessage());
      // happy path
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  @Test
  public void testDeploymentUnregistrationOnUndeployment() {
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    assertThat(managementService.getRegisteredDeployments()).hasSize(1);

    repositoryService.deleteDeployment(deploymentId, true);

    assertThat(managementService.getRegisteredDeployments()).isEmpty();
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  @Test
  public void testNoUnregistrationOnFailingUndeployment() {
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    try {
      repositoryService.deleteDeployment(deploymentId, false);
      fail("");
    } catch (Exception e) {
      // should still be registered, if not successfully undeployed
      assertThat(managementService.getRegisteredDeployments()).hasSize(1);
    }
  }

  @Deployment(resources = "org/operaton/bpm/engine/test/jobexecutor/simpleAsyncProcess.bpmn20.xml")
  @Test
  public void testExplicitDeploymentUnregistration() {
    String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
    runtimeService.startProcessInstanceByKey("simpleAsyncProcess");

    processEngine.getManagementService().unregisterDeploymentForJobExecutor(deploymentId);

    AcquiredJobs acquiredJobs = getExecutableJobs(processEngineConfiguration.getJobExecutor());
    assertThat(acquiredJobs.size()).isZero();
  }

  @Test
  public void testJobsWithoutDeploymentIdAreAlwaysProcessed() {
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();

    String messageId = commandExecutor.execute(commandContext -> {
      MessageEntity message = new MessageEntity();
      commandContext.getJobManager().send(message);
      return message.getId();
    });

    AcquiredJobs acquiredJobs = getExecutableJobs(processEngineConfiguration.getJobExecutor());
    assertThat(acquiredJobs.size()).isEqualTo(1);
    assertThat(acquiredJobs.contains(messageId)).isTrue();

    commandExecutor.execute(new DeleteJobsCmd(messageId, true));
  }

  private AcquiredJobs getExecutableJobs(JobExecutor jobExecutor) {
    return processEngineConfiguration.getCommandExecutorTxRequired().execute(new AcquireJobsCmd(jobExecutor));
  }

  private String deployAndInstantiateWithNewEngineConfiguration(String resource) {
    // 1. create another process engine
    try {
      otherProcessEngine = ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource("operaton.cfg.xml")
        .buildProcessEngine();
    } catch (RuntimeException ex) {
      if (ex.getCause() != null && ex.getCause() instanceof FileNotFoundException) {
        otherProcessEngine = ProcessEngineConfiguration
          .createProcessEngineConfigurationFromResource("activiti.cfg.xml")
          .buildProcessEngine();
      } else {
        throw ex;
      }
    }

    // 2. deploy again
    RepositoryService otherRepositoryService = otherProcessEngine.getRepositoryService();

    String deploymentId = otherRepositoryService.createDeployment()
      .addClasspathResource(resource)
      .deploy().getId();

    // 3. start instance (i.e. create job)
    ProcessDefinition newDefinition = otherRepositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult();
    otherProcessEngine.getRuntimeService().startProcessInstanceById(newDefinition.getId());

    return deploymentId;
  }

  @Deployment(resources="org/operaton/bpm/engine/test/jobexecutor/processWithTimerCatch.bpmn20.xml")
  @Test
  public void testIntermediateTimerEvent() {


    runtimeService.startProcessInstanceByKey("testProcess");

    Set<String> registeredDeployments = processEngineConfiguration.getRegisteredDeployments();


    Job existingJob = managementService.createJobQuery().singleResult();

    ClockUtil.setCurrentTime(new Date(System.currentTimeMillis() + 61 * 1000));

    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    assertThat(acquirableJobs).hasSize(1);
    assertThat(acquirableJobs.get(0).getId()).isEqualTo(existingJob.getId());

    registeredDeployments.clear();

    acquirableJobs = findAcquirableJobs();

    assertThat(acquirableJobs).isEmpty();
  }

  @Deployment(resources="org/operaton/bpm/engine/test/jobexecutor/processWithTimerStart.bpmn20.xml")
  @Test
  public void testTimerStartEvent() {

    Set<String> registeredDeployments = processEngineConfiguration.getRegisteredDeployments();

    Job existingJob = managementService.createJobQuery().singleResult();

    ClockUtil.setCurrentTime(new Date(System.currentTimeMillis()+1000));

    List<AcquirableJobEntity> acquirableJobs = findAcquirableJobs();

    assertThat(acquirableJobs).hasSize(1);
    assertThat(acquirableJobs.get(0).getId()).isEqualTo(existingJob.getId());

    registeredDeployments.clear();

    acquirableJobs = findAcquirableJobs();

    assertThat(acquirableJobs).isEmpty();
  }

  protected List<AcquirableJobEntity> findAcquirableJobs() {
    return processEngineConfiguration.getCommandExecutorTxRequired().execute(commandContext -> commandContext
        .getJobManager()
        .findNextJobsToExecute(new Page(0, 100)));
  }

}
