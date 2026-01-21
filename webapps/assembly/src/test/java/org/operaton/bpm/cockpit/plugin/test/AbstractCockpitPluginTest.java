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
package org.operaton.bpm.cockpit.plugin.test;

import java.util.Collections;
import java.util.List;

import org.apache.ibatis.logging.LogFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.cockpit.Cockpit;
import org.operaton.bpm.cockpit.db.CommandExecutor;
import org.operaton.bpm.cockpit.db.QueryService;
import org.operaton.bpm.cockpit.impl.DefaultCockpitRuntimeDelegate;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.LogUtil;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.operaton.bpm.engine.impl.test.TestHelper.executeJobIgnoringException;

/**
 *
 * @author nico.rehwaldt
 */
public abstract class AbstractCockpitPluginTest {

  private static final TestCockpitRuntimeDelegate RUNTIME_DELEGATE = new TestCockpitRuntimeDelegate();
  private static final String DEFAULT_BPMN_RESOURCE_NAME = "process.bpmn20.xml";

  static {
    LogUtil.readJavaUtilLoggingConfigFromClasspath();

    // this ensures that mybatis uses the jdk logging
    LogFactory.useJdkLogging();
    // with an upgrade of mybatis, this might have to become org.mybatis.generator.logging.LogFactory.forceJavaLogging();
  }

  protected ProcessEngine processEngine;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected AuthorizationService authorizationService;

  @RegisterExtension
  public static ProcessEngineExtension processEngineExtension = ProcessEngineExtension.builder().ensureCleanAfterTest(false).build();

  @BeforeAll
  public static void beforeClass() {
    Cockpit.setCockpitRuntimeDelegate(RUNTIME_DELEGATE);
  }

  @AfterAll
  public static void afterClass() {
    Cockpit.setCockpitRuntimeDelegate(null);
  }

  @BeforeEach
  public void before() {
    RUNTIME_DELEGATE.engine = getProcessEngine();
  }

  @AfterEach
  public void after() {
    RUNTIME_DELEGATE.engine = null;
    getProcessEngine().getIdentityService().clearAuthentication();
  }

  public ProcessEngine getProcessEngine() {
    return processEngine;
  }

  protected CommandExecutor getCommandExecutor() {
    return Cockpit.getCommandExecutor("default");
  }

  protected QueryService getQueryService() {
    return Cockpit.getQueryService("default");
  }

  public void executeAvailableJobs() {
    ManagementService managementService = getProcessEngine().getManagementService();
    List<Job> jobs = managementService.createJobQuery().withRetriesLeft().list();

    if (jobs.isEmpty()) {
      return;
    }

    for (Job job : jobs) {
      executeJobIgnoringException(managementService, job.getId());
    }

    executeAvailableJobs();
  }

  public Deployment deploy(String... resources) {
    return deploy(createDeploymentBuilder(), Collections.<BpmnModelInstance> emptyList(), List.of(resources));
  }

  public Deployment deployForTenant(String tenantId, String... resources) {
    return deploy(createDeploymentBuilder().tenantId(tenantId), Collections.<BpmnModelInstance> emptyList(), List.of(resources));
  }

  protected Deployment deploy(DeploymentBuilder deploymentBuilder, List<BpmnModelInstance> bpmnModelInstances, List<String> resources) {
    int i = 0;
    for (BpmnModelInstance bpmnModelInstance : bpmnModelInstances) {
      deploymentBuilder.addModelInstance(i + "_" + DEFAULT_BPMN_RESOURCE_NAME, bpmnModelInstance);
      i++;
    }

    for (String resource : resources) {
      deploymentBuilder.addClasspathResource(resource);
    }

    Deployment deployment = deploymentBuilder.deploy();

    processEngineExtension.manageDeployment(deployment);

    return deployment;
  }

  protected DeploymentBuilder createDeploymentBuilder() {
    return getProcessEngine().getRepositoryService().createDeployment();
  }

  private static class TestCockpitRuntimeDelegate extends DefaultCockpitRuntimeDelegate {

    public ProcessEngine engine;

    @Override
    public ProcessEngine getProcessEngine(String processEngineName) {

      // always return default engine for plugin tests
      return engine;
    }
  }
}
