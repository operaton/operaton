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
package org.operaton.bpm.engine.spring.test;

import java.util.ServiceLoader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.diagnostics.PlatformDiagnosticsRegistry;
import org.operaton.bpm.engine.impl.test.TestHelper;
import org.operaton.bpm.engine.impl.util.ClockUtil;


/**
 * @author Joram Barrez
 */
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
@ExtendWith(SpringExtension.class)
public abstract class SpringProcessEngineTestCase implements ApplicationContextAware {

  protected ProcessEngine processEngine;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected ManagementService managementService;
  protected TestContextManager testContextManager;

  protected ConfigurableApplicationContext applicationContext;
  private String deploymentId;

  protected SpringProcessEngineTestCase() {
    super();
    this.testContextManager = new TestContextManager(getClass());

    SpringTestHelper testHelper = lookupTestHelper();
    testHelper.beforeTestClass(testContextManager);
  }

  protected SpringTestHelper lookupTestHelper()
  {
    ServiceLoader<SpringTestHelper> serviceLoader = ServiceLoader.load(SpringTestHelper.class);
    return serviceLoader.iterator().next();
  }

  @BeforeEach
  protected void setUp(TestInfo testInfo) throws Exception {
    processEngine = applicationContext.getBean(ProcessEngine.class);
    processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
    runtimeService = processEngine.getRuntimeService();
    taskService = processEngine.getTaskService();
    managementService = processEngine.getManagementService();
    deploymentSetUp(testInfo);

    testContextManager.prepareTestInstance(this); // this will initialize all dependencies
  }

  private void deploymentSetUp(TestInfo testInfo) {
    var testClass = testInfo.getTestClass().orElseThrow();
    var testMethod = testInfo.getTestMethod().orElseThrow();

    deploymentId = TestHelper.annotationDeploymentSetUp(processEngine, testClass, testMethod.getName(), null, testMethod.getParameterTypes());
    boolean hasRequiredHistoryLevel = TestHelper.annotationRequiredHistoryLevelCheck(processEngine, testClass, testMethod.getName(), testMethod.getParameterTypes());
    boolean hasRequiredDatabase = TestHelper.annotationRequiredDatabaseCheck(processEngine, testClass, testMethod.getName(), testMethod.getParameterTypes());

    Assumptions.assumeTrue(hasRequiredHistoryLevel, "ignored because the current history level is too low");
    Assumptions.assumeTrue(hasRequiredDatabase, "ignored because the database doesn't match the required ones");
  }

  @AfterEach
  protected void tearDown(TestInfo testInfo) throws Exception {
    var testClass = testInfo.getTestClass().orElseThrow();
    var testMethod = testInfo.getTestMethod().orElseThrow();

    TestHelper.annotationDeploymentTearDown(processEngine, deploymentId, testClass, testMethod.getName());
    deploymentId = null;

    TestHelper.resetIdGenerator(processEngineConfiguration);
    ClockUtil.reset();
    PlatformDiagnosticsRegistry.clear();

    testContextManager.afterTestClass();
    applicationContext.close();
    applicationContext = null;
    processEngine = null;
    processEngineConfiguration = null;
    runtimeService = null;
    taskService = null;
    managementService = null;
    testContextManager = null;


    CachedIntrospectionResults.clearClassLoader(getClass().getClassLoader());
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = (ConfigurableApplicationContext) applicationContext;
  }

}
