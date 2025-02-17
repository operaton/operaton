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
package org.operaton.bpm.application.impl.embedded;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Set;

import ch.qos.logback.classic.Level;
import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.container.RuntimeContainerDelegate;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessApplicationDeployment;
import org.operaton.bpm.engine.repository.Resource;
import org.operaton.bpm.engine.test.util.PluggableProcessEngineTest;
import org.operaton.commons.testing.ProcessEngineLoggingRule;

import org.junit.*;

/**
 * @author Daniel Meyer
 *
 */
public class EmbeddedProcessApplicationTest extends PluggableProcessEngineTest {

  protected static final String CONFIG_LOGGER = "org.operaton.bpm.application";
  @Rule
  public ProcessEngineLoggingRule loggingRule = new ProcessEngineLoggingRule()
                                                    .watch(CONFIG_LOGGER)
                                                    .level(Level.WARN);

  protected RuntimeContainerDelegate runtimeContainerDelegate = RuntimeContainerDelegate.INSTANCE.get();
  protected boolean defaultEngineRegistered;

  public void registerProcessEngine() {
    runtimeContainerDelegate.registerProcessEngine(processEngine);
    defaultEngineRegistered = true;
  }

  @Before
  public void setUp() {
    defaultEngineRegistered = false;
  }

  @After
  public void tearDown() {
    if (defaultEngineRegistered) {
      runtimeContainerDelegate.unregisterProcessEngine(processEngine);
    }
  }

  @Test
  public void testDeployAppWithoutEngine() {
    TestApplicationWithoutEngine processApplication = new TestApplicationWithoutEngine();

    assertThatCode(processApplication::deploy).doesNotThrowAnyException();
    assertThatCode(processApplication::undeploy).doesNotThrowAnyException();
  }

  @Test
  public void testDeployAppWithoutProcesses() {

    registerProcessEngine();

    TestApplicationWithoutProcesses processApplication = new TestApplicationWithoutProcesses();
    processApplication.deploy();

    ProcessEngine processEngine = BpmPlatform.getProcessEngineService().getDefaultProcessEngine();
    long deployments = processEngine.getRepositoryService().createDeploymentQuery().count();
    assertThat(deployments).isZero();

    processApplication.undeploy();

  }

  @Test
  public void testDeployAppWithCustomEngine() {

    TestApplicationWithCustomEngine processApplication = new TestApplicationWithCustomEngine();
    processApplication.deploy();

    ProcessEngine processEngine = BpmPlatform.getProcessEngineService().getProcessEngine("embeddedEngine");
    assertThat(processEngine).isNotNull();
    assertThat(processEngine.getName()).isEqualTo("embeddedEngine");

    ProcessEngineConfiguration configuration = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration();

    // assert engine properties specified
    assertThat(configuration.isJobExecutorDeploymentAware()).isTrue();
    assertThat(configuration.isJobExecutorPreferTimerJobs()).isTrue();
    assertThat(configuration.isJobExecutorAcquireByDueDate()).isTrue();
    assertThat(configuration.getJdbcMaxActiveConnections()).isEqualTo(5);

    processApplication.undeploy();

  }

  @Test
  public void testDeployAppWithoutDmn() {
    // given
    TestApplicationWithoutDmn processApplication = new TestApplicationWithoutDmn();
    processApplication.deploy();

    ProcessEngine processEngine = BpmPlatform.getProcessEngineService().getProcessEngine("embeddedEngine");
    assertThat(processEngine).isNotNull();
    assertThat(processEngine.getName()).isEqualTo("embeddedEngine");

    ProcessEngineConfigurationImpl configuration = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration();

    // assert engine properties specified
    assertThat(configuration.isJobExecutorDeploymentAware()).isTrue();
    assertThat(configuration.isJobExecutorPreferTimerJobs()).isTrue();
    assertThat(configuration.isJobExecutorAcquireByDueDate()).isTrue();
    assertThat(configuration.getJdbcMaxActiveConnections()).isEqualTo(5);
    assertThat(configuration.isDmnEnabled()).isFalse();

    // when
    processApplication.undeploy();

    // then
    assertThat(loggingRule
        .getFilteredLog("ENGINE-07018 Unregistering process application for deployment but could " +
                        "not remove process definitions from deployment cache."))
        .isEmpty();
  }

  @Test
  public void testDeployAppWithCustomDefaultEngine() {

    // Test if it's possible to set a custom default engine name.
    // This might happen when the "default" ProcessEngine is not available,
    // but a ProcessApplication doesn't define a ProcessEngine to deploy to.
    String processApplicationName = "test-app";
    String customEngineName = "customDefaultEngine";
    TestApplicationWithCustomDefaultEngine processApplication = new TestApplicationWithCustomDefaultEngine();

    processApplication.deploy();

    String deployedToProcessEngineName = runtimeContainerDelegate.getProcessApplicationService()
      .getProcessApplicationInfo(processApplicationName)
      .getDeploymentInfo()
      .get(0)
      .getProcessEngineName();

    assertThat(processApplication.getDefaultDeployToEngineName()).isEqualTo(customEngineName);
    assertThat(deployedToProcessEngineName).isEqualTo(customEngineName);

    processApplication.undeploy();
  }

  @Test
  public void testDeployAppReusingExistingEngine() {

    registerProcessEngine();

    TestApplicationReusingExistingEngine processApplication = new TestApplicationReusingExistingEngine();
    processApplication.deploy();

    assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);

    processApplication.undeploy();

    assertThat(repositoryService.createDeploymentQuery().count()).isZero();

  }

  @Test
  public void testDeployAppWithAdditionalResourceSuffixes() {
    registerProcessEngine();

    TestApplicationWithAdditionalResourceSuffixes processApplication = new TestApplicationWithAdditionalResourceSuffixes();
    processApplication.deploy();


    Deployment deployment = repositoryService.createDeploymentQuery().singleResult();

    assertThat(deployment).isNotNull();

    List<Resource> deploymentResources = repositoryService.getDeploymentResources(deployment.getId());
    assertThat(deploymentResources).hasSize(4);

    processApplication.undeploy();
    assertThat(repositoryService.createDeploymentQuery().count()).isZero();
  }

  @Test
  public void testDeployAppWithResources() {
    registerProcessEngine();

    TestApplicationWithResources processApplication = new TestApplicationWithResources();
    processApplication.deploy();

    Deployment deployment = repositoryService.createDeploymentQuery().singleResult();

    assertThat(deployment).isNotNull();

    List<Resource> deploymentResources = repositoryService.getDeploymentResources(deployment.getId());
    assertThat(deploymentResources).hasSize(4);

    processApplication.undeploy();
    assertThat(repositoryService.createDeploymentQuery().count()).isZero();
  }

  @Test
  public void testDeploymentSourceProperty() {
    registerProcessEngine();

    TestApplicationWithResources processApplication = new TestApplicationWithResources();
    processApplication.deploy();

    Deployment deployment = repositoryService.createDeploymentQuery().singleResult();

    assertThat(deployment).isNotNull();
    assertThat(deployment.getSource()).isEqualTo(ProcessApplicationDeployment.PROCESS_APPLICATION_DEPLOYMENT_SOURCE);

    processApplication.undeploy();
  }

  @Test
  public void testDeployProcessApplicationWithNameAttribute() {
    TestApplicationWithCustomName pa = new TestApplicationWithCustomName();

    pa.deploy();

    Set<String> deployedPAs = runtimeContainerDelegate.getProcessApplicationService().getProcessApplicationNames();
    assertThat(deployedPAs).containsExactly(TestApplicationWithCustomName.NAME);

    pa.undeploy();
  }

  @Test
  public void testDeployWithTenantIds() {
    registerProcessEngine();

    TestApplicationWithTenantId processApplication = new TestApplicationWithTenantId();
    processApplication.deploy();

    List<Deployment> deployments = repositoryService
        .createDeploymentQuery()
        .orderByTenantId()
        .asc()
        .list();

    assertThat(deployments).hasSize(2);
    assertThat(deployments.get(0).getTenantId()).isEqualTo("tenant1");
    assertThat(deployments.get(1).getTenantId()).isEqualTo("tenant2");

    processApplication.undeploy();
  }

  @Test
  public void testDeployWithoutTenantId() {
    registerProcessEngine();

    TestApplicationWithResources processApplication = new TestApplicationWithResources();
    processApplication.deploy();

    Deployment deployment = repositoryService.createDeploymentQuery().singleResult();

    assertThat(deployment).isNotNull();
    assertThat(deployment.getTenantId()).isNull();

    processApplication.undeploy();
  }

}
