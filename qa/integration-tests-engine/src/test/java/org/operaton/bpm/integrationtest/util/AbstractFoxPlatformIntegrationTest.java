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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.operaton.bpm.engine.test.util.JobExecutorHelper;

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
    JobExecutorHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, JobExecutorHelper.JOBS_WAIT_TIMEOUT_MS);
  }

  public void waitForJobExecutorToProcessAllJobs(long maxMillisToWait) {
    JobExecutorHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, maxMillisToWait);
  }

}
