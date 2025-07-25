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
package org.operaton.bpm.integrationtest.jobexecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.util.JobExecutorWaitUtils;
import org.operaton.bpm.integrationtest.jobexecutor.beans.ManagedJobExecutorBean;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;
import org.operaton.bpm.integrationtest.util.TestContainer;

@ExtendWith(ArquillianExtension.class)
public class ManagedJobExecutorTest {

  @Deployment
  public static WebArchive createDeployment() {
    WebArchive archive = ShrinkWrap.create(WebArchive.class, "test.war")
        .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
        .addAsLibraries(DeploymentHelper.getEngineCdi())
        .addClass(JobExecutorWaitUtils.class)
        .addAsLibraries(DeploymentHelper.getTestingLibs())
        .addClass(ManagedJobExecutorTest.class)
        .addClass(ManagedJobExecutorBean.class)
        .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/ManagedJobExecutorTest.testManagedExecutorUsed.bpmn20.xml");

    TestContainer.addContainerSpecificResourcesForNonPa(archive);

    return archive;
  }

  protected ProcessEngine processEngine;
  protected ManagementService managementService;
  protected RuntimeService runtimeService;

  @BeforeEach
  void setUpCdiProcessEngineTestCase() {
    processEngine = (ProgrammaticBeanLookup.lookup(ManagedJobExecutorBean.class)).getProcessEngine();
    managementService = processEngine.getManagementService();
    runtimeService = processEngine.getRuntimeService();
  }

  @AfterEach
  void tearDownCdiProcessEngineTestCase() {
    processEngine = null;
    managementService = null;
    runtimeService = null;
  }

  @Test
  void testManagedExecutorUsed() {
    org.operaton.bpm.engine.repository.Deployment deployment = processEngine.getRepositoryService().createDeployment()
      .addClasspathResource("org/operaton/bpm/integrationtest/jobexecutor/ManagedJobExecutorTest.testManagedExecutorUsed.bpmn20.xml")
      .deploy();

    try {
      String pid = runtimeService.startProcessInstanceByKey("testBusinessProcessScopedWithJobExecutor").getId();

      assertEquals(1L, managementService.createJobQuery().processInstanceId(pid).count());

      executeJobs(pid);

      assertEquals(0L, managementService.createJobQuery().processInstanceId(pid).count());

      assertEquals("bar", runtimeService.createVariableInstanceQuery().processInstanceIdIn(pid).variableName("foo").singleResult().getValue());
    } finally {
      processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
    }

  }

  private void executeJobs(String pid) {
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
    JobExecutorWaitUtils.waitForJobExecutorToProcessAllJobs(pid, 5000L, 25L, processEngineConfiguration.getJobExecutor(), processEngineConfiguration.getManagementService());
  }

}
