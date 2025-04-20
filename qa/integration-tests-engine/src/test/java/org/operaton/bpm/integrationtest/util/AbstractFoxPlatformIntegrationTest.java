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
package org.operaton.bpm.integrationtest.util;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.ProcessEngineService;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;


public abstract class AbstractFoxPlatformIntegrationTest {

  protected static final long JOBS_WAIT_TIMEOUT_MS = 20_000L;

  protected Logger logger = Logger.getLogger(AbstractFoxPlatformIntegrationTest.class.getName());

  protected ProcessEngineService processEngineService;
  protected ProcessEngine processEngine;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected FormService formService;
  protected HistoryService historyService;
  protected IdentityService identityService;
  protected ManagementService managementService;
  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected CaseService caseService;
  protected DecisionService decisionService;

  protected static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer(DockerImageName.parse("postgres:13.2"));

  static {
    postgreSQLContainer.start();
  }

  public static WebArchive initWebArchiveDeployment(String name, String processesXmlPath) {
    WebArchive archive = ShrinkWrap.create(WebArchive.class, name)
              .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
              .addAsLibraries(DeploymentHelper.getEngineCdi())
              .addAsLibraries(DeploymentHelper.getAssertJ())
              .addAsResource(processesXmlPath, "META-INF/processes.xml")
              .addClass(AbstractFoxPlatformIntegrationTest.class)
              .addClass(TestConstants.class);

    TestContainer.addContainerSpecificResources(archive);

    return archive;
  }
  public static WebArchive initWebArchiveDeployment(String name) {
    return initWebArchiveDeployment(name, "META-INF/processes.xml");
  }

  public static WebArchive initWebArchiveDeployment() {
    return initWebArchiveDeployment("test.war");
  }

  @Before
  public void setupBeforeTest() {
    processEngineService = BpmPlatform.getProcessEngineService();
    processEngine = processEngineService.getDefaultProcessEngine();
    processEngineConfiguration = ((ProcessEngineImpl)processEngine).getProcessEngineConfiguration();
    processEngineConfiguration.getJobExecutor().shutdown(); // make sure the job executor is down
    formService = processEngine.getFormService();
    historyService = processEngine.getHistoryService();
    identityService = processEngine.getIdentityService();
    managementService = processEngine.getManagementService();
    repositoryService = processEngine.getRepositoryService();
    runtimeService = processEngine.getRuntimeService();
    taskService = processEngine.getTaskService();
    caseService = processEngine.getCaseService();
    decisionService = processEngine.getDecisionService();
  }

  public void waitForJobExecutorToProcessAllJobs() {
    waitForJobExecutorToProcessAllJobs(JOBS_WAIT_TIMEOUT_MS);
  }

  public void waitForJobExecutorToProcessAllJobs(long maxMillisToWait) {

    JobExecutor jobExecutor = processEngineConfiguration.getJobExecutor();
    waitForJobExecutorToProcessAllJobs(jobExecutor, maxMillisToWait);
  }

  public void waitForJobExecutorToProcessAllJobs(JobExecutor jobExecutor, long maxMillisToWait) {

    int checkInterval = 1000;

    jobExecutor.start();

    try {
      Timer timer = new Timer();
      InterruptTask task = new InterruptTask(Thread.currentThread());
      timer.schedule(task, maxMillisToWait);
      boolean areJobsAvailable = true;
      try {
        while (areJobsAvailable && !task.isTimeLimitExceeded()) {
          Thread.sleep(checkInterval);
          areJobsAvailable = areJobsAvailable();
        }
      } catch (InterruptedException e) {
      } finally {
        timer.cancel();
      }
      if (areJobsAvailable) {
        throw new RuntimeException("time limit of " + maxMillisToWait + " was exceeded (still " + numberOfJobsAvailable() + " jobs available)");
      }

    } finally {
      jobExecutor.shutdown();
    }
  }

  public boolean areJobsAvailable() {
    List<Job> list = managementService.createJobQuery().list();
    for (Job job : list) {
      if (isJobAvailable(job)) {
        return true;
      }
    }
    return false;
  }

  public boolean isJobAvailable(Job job) {
    return job.getRetries() > 0 && (job.getDuedate() == null || ClockUtil.getCurrentTime().after(job.getDuedate()));
  }

  public int numberOfJobsAvailable() {
    int numberOfJobs = 0;
    List<Job> jobs = managementService.createJobQuery().list();
    for (Job job : jobs) {
      if (isJobAvailable(job)) {
        numberOfJobs++;
      }
    }
    return numberOfJobs;
  }

  private static class InterruptTask extends TimerTask {

    protected boolean timeLimitExceeded = false;
    protected Thread thread;

    public InterruptTask(Thread thread) {
      this.thread = thread;
    }
    public boolean isTimeLimitExceeded() {
      return timeLimitExceeded;
    }

    @Override
    public void run() {
      timeLimitExceeded = true;
      thread.interrupt();
    }
  }

}
