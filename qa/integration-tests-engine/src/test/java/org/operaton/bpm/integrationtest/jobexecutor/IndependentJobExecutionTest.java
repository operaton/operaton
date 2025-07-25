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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.protocol.servlet5.arq514hack.descriptors.api.web.WebAppDescriptor;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.ProcessEngineService;
import org.operaton.bpm.application.ProcessApplicationDeploymentInfo;
import org.operaton.bpm.application.ProcessApplicationInfo;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cmd.AcquireJobsCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.AcquiredJobs;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.TestContainer;

@ExtendWith(ArquillianExtension.class)
public class IndependentJobExecutionTest extends AbstractFoxPlatformIntegrationTest {

  private ProcessEngine engine1;
  private ProcessEngineConfigurationImpl engine1Configuration;

  @BeforeEach
  void setEngines() {
    ProcessEngineService engineService = BpmPlatform.getProcessEngineService();
    engine1 = engineService.getProcessEngine("engine1");
    engine1Configuration = ((ProcessEngineImpl) engine1).getProcessEngineConfiguration();
  }

  @Deployment(order = 0, name="pa1")
  public static WebArchive processArchive1() {

    WebArchive deployment = initWebArchiveDeployment("pa1.war", "org/operaton/bpm/integrationtest/jobexecutor/IndependentJobExecutionTest.pa1.xml")
        .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/IndependentJobExecutionTest.process1.bpmn20.xml")
        .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class).version("3.0").exportAsString()));

    TestContainer.addContainerSpecificProcessEngineConfigurationClass(deployment);

    return deployment;

  }

  @Deployment(order = 1, name="pa2")
  public static WebArchive processArchive2() {

    return initWebArchiveDeployment("pa2.war", "org/operaton/bpm/integrationtest/jobexecutor/IndependentJobExecutionTest.pa2.xml")
        .addAsResource("org/operaton/bpm/integrationtest/jobexecutor/IndependentJobExecutionTest.process2.bpmn20.xml")
        .setWebXML(new StringAsset(Descriptors.create(WebAppDescriptor.class).version("3.0").exportAsString()));
  }

  @OperateOnDeployment("pa1")
  @Test
  void testDeploymentRegistration() {
    Set<String> registeredDeploymentsForEngine1 = engine1.getManagementService().getRegisteredDeployments();
    Set<String> registeredDeploymentsForDefaultEngine = processEngine.getManagementService().getRegisteredDeployments();

    ProcessApplicationInfo pa1Info = getProcessApplicationDeploymentInfo("pa1");

    List<ProcessApplicationDeploymentInfo> pa1DeploymentInfo = pa1Info.getDeploymentInfo();

    Assertions.assertEquals(1, pa1DeploymentInfo.size());
    assertThat(registeredDeploymentsForEngine1).contains(pa1DeploymentInfo.get(0).getDeploymentId());

    ProcessApplicationInfo pa2Info = getProcessApplicationDeploymentInfo("pa2");

    List<ProcessApplicationDeploymentInfo> pa2DeploymentInfo = pa2Info.getDeploymentInfo();
    Assertions.assertEquals(1, pa2DeploymentInfo.size());
    assertThat(registeredDeploymentsForDefaultEngine).contains(pa2DeploymentInfo.get(0).getDeploymentId());
  }

  private ProcessApplicationInfo getProcessApplicationDeploymentInfo(String applicationName) {
    ProcessApplicationInfo processApplicationInfo = BpmPlatform.getProcessApplicationService().getProcessApplicationInfo(applicationName);
    if (processApplicationInfo == null) {
      processApplicationInfo = BpmPlatform.getProcessApplicationService().getProcessApplicationInfo("/" + applicationName);
    }
    return processApplicationInfo;

  }

  @OperateOnDeployment("pa1")
  @Test
  void testDeploymentAwareJobAcquisition() {
    JobExecutor jobExecutor1 = engine1Configuration.getJobExecutor();

    ProcessInstance instance1 = engine1.getRuntimeService().startProcessInstanceByKey("archive1Process");
    ProcessInstance instance2 = processEngine.getRuntimeService().startProcessInstanceByKey("archive2Process");

    Job job1 = managementService.createJobQuery().processInstanceId(instance1.getId()).singleResult();
    Job job2 = managementService.createJobQuery().processInstanceId(instance2.getId()).singleResult();


    // the deployment aware configuration should only return the jobs of the registered deployments
    CommandExecutor commandExecutor = engine1Configuration.getCommandExecutorTxRequired();
    AcquiredJobs acquiredJobs = commandExecutor.execute(new AcquireJobsCmd(jobExecutor1));

    Assertions.assertEquals(1, acquiredJobs.size());
    assertThat(acquiredJobs.contains(job1.getId())).isTrue();
    assertThat(acquiredJobs.contains(job2.getId())).isFalse();
  }

  @OperateOnDeployment("pa1")
  @Test
  void testDeploymentUnawareJobAcquisition() {
    JobExecutor defaultJobExecutor = processEngineConfiguration.getJobExecutor();

    ProcessInstance instance1 = engine1.getRuntimeService().startProcessInstanceByKey("archive1Process");
    ProcessInstance instance2 = processEngine.getRuntimeService().startProcessInstanceByKey("archive2Process");

    Job job1 = managementService.createJobQuery().processInstanceId(instance1.getId()).singleResult();
    Job job2 = managementService.createJobQuery().processInstanceId(instance2.getId()).singleResult();

    // the deployment unaware configuration should return both jobs
    CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutorTxRequired();
    processEngineConfiguration.setJobExecutorDeploymentAware(false);
    try {
      AcquiredJobs acquiredJobs = commandExecutor.execute(new AcquireJobsCmd(defaultJobExecutor));

      Assertions.assertEquals(2, acquiredJobs.size());
      assertThat(acquiredJobs.contains(job1.getId())).isTrue();
      assertThat(acquiredJobs.contains(job2.getId())).isTrue();
    } finally {
      processEngineConfiguration.setJobExecutorDeploymentAware(true);
    }
  }
}
