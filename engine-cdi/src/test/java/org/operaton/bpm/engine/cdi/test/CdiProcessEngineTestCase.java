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
package org.operaton.bpm.engine.cdi.test;

import java.util.logging.Logger;
import jakarta.enterprise.inject.spi.BeanManager;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.BpmPlatform;
import org.operaton.bpm.container.RuntimeContainerDelegate;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.CaseService;
import org.operaton.bpm.engine.DecisionService;
import org.operaton.bpm.engine.ExternalTaskService;
import org.operaton.bpm.engine.FilterService;
import org.operaton.bpm.engine.FormService;
import org.operaton.bpm.engine.HistoryService;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.cdi.BusinessProcess;
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.util.LogUtil;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Daniel Meyer
 * When creating a new test class, extend it with this class.
 * <p>
 * Migrated to JUnit 5 by registering a shared {@link org.operaton.bpm.engine.test.junit5.ProcessEngineExtension}
 * once per suite and widening the Arquillian deployment so CDI still sees every test bean.
 * We resolve the bean manager lazily now, matching the container lifecycle that the extension enforces.
 */
@ExtendWith(ArquillianExtension.class)
public abstract class CdiProcessEngineTestCase {

  static {
    LogUtil.readJavaUtilLoggingConfigFromClasspath();
  }

  protected Logger logger = Logger.getLogger(getClass().getName());

  @RegisterExtension
  protected static final ProcessEngineExtension processEngineExtension =
    ProcessEngineExtension.builder()
      .configurationResource("activiti.cfg.xml")
      .build();

  @Deployment
  public static JavaArchive createDeployment() {

    return ShrinkWrap.create(JavaArchive.class)
      .addPackages(true,
        "org.operaton.bpm.engine.cdi",
        "org.operaton.bpm.engine.cdi.test",
        "org.operaton.bpm.engine.experimental")
      .addAsManifestResource("META-INF/beans.xml", "beans.xml");
  }

  protected ProcessEngine processEngine;
  protected FormService formService;
  protected HistoryService historyService;
  protected IdentityService identityService;
  protected ManagementService managementService;
  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected AuthorizationService authorizationService;
  protected FilterService filterService;
  protected ExternalTaskService externalTaskService;
  protected CaseService caseService;
  protected DecisionService decisionService;

  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  @BeforeEach
  void setUpCdiProcessEngineTestCase() {

    processEngine = processEngineExtension.getProcessEngine();

    if (BpmPlatform.getProcessEngineService().getDefaultProcessEngine() == null) {
      RuntimeContainerDelegate.INSTANCE.get().registerProcessEngine(processEngine);
    }

    processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
    formService = processEngine.getFormService();
    historyService = processEngine.getHistoryService();
    identityService = processEngine.getIdentityService();
    managementService = processEngine.getManagementService();
    repositoryService = processEngine.getRepositoryService();
    runtimeService = processEngine.getRuntimeService();
    taskService = processEngine.getTaskService();
    authorizationService = processEngine.getAuthorizationService();
    filterService = processEngine.getFilterService();
    externalTaskService = processEngine.getExternalTaskService();
    caseService = processEngine.getCaseService();
    decisionService = processEngine.getDecisionService();
  }

  @AfterEach
  void afterEachCdiProcessEngineTestCase() {
    tearDownCdiProcessEngineTestCase();
  }

  protected void tearDownCdiProcessEngineTestCase() {
    if (processEngine != null) {
      RuntimeContainerDelegate.INSTANCE.get().unregisterProcessEngine(processEngine);
    }
    processEngine = null;
    processEngineConfiguration = null;
    formService = null;
    historyService = null;
    identityService = null;
    managementService = null;
    repositoryService = null;
    runtimeService = null;
    taskService = null;
    authorizationService = null;
    filterService = null;
    externalTaskService = null;
    caseService = null;
    decisionService = null;
  }

  protected BeanManager getBeanManager() {
    return ProgrammaticBeanLookup.lookup(BeanManager.class);
  }

  protected void endConversationAndBeginNew(String processInstanceId) {
    getBeanInstance(BusinessProcess.class).associateExecutionById(processInstanceId);
  }

  protected <T> T getBeanInstance(Class<T> clazz) {
    return ProgrammaticBeanLookup.lookup(clazz);
  }

  protected Object getBeanInstance(String name) {
    return ProgrammaticBeanLookup.lookup(name);
  }

}
