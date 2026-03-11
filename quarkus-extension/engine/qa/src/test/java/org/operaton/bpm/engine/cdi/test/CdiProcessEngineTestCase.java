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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import jakarta.enterprise.inject.spi.BeanManager;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.BpmPlatform;
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
import org.operaton.bpm.engine.cdi.impl.util.ProgrammaticBeanLookup;
import org.operaton.bpm.engine.experimental.InjectProcessVariable;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.test.TestHelper;
import org.operaton.bpm.engine.test.Deployment;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class CdiProcessEngineTestCase {

  protected String deploymentId;

  protected BeanManager beanManager;

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

  protected Set<InstanceHandle<?>> beanInstanceHandles = new HashSet<>();


  @RegisterExtension
  protected static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
          .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                  .addAsResource("application.properties")
                  .addPackages(true, CdiProcessEngineTestCase.class.getPackage())
                  .addPackages(true, InjectProcessVariable.class.getPackage()));

  @BeforeEach
  void before(TestInfo testInfo) throws Throwable {
    Set<String> processEngineNames = BpmPlatform.getProcessEngineService()
        .getProcessEngineNames();
    if (processEngineNames.size() > 1) {
      throw new RuntimeException("More than one process engines registered");
    }
    processEngine =
        BpmPlatform.getProcessEngineService().getProcessEngine(processEngineNames.stream().findFirst().get());
    Arc.container().requestContext().activate();
    beanManager = Arc.container().beanManager();
    processEngineConfiguration =
        (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
    processEngineConfiguration.setEnableExpressionsInAdhocQueries(true);
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

    Method testMethod = testInfo.getTestMethod().orElse(null);
    assertThat(testMethod).isNotNull();

    Class<?> javaTestClass = testMethod.getDeclaringClass();
    String testMethodName = testMethod.getName();

    String[] resources = getDeployment(javaTestClass, testMethod);

    deploymentId = deploy(javaTestClass, testMethodName, resources);
  }

  private String[] getDeployment(Class<?> testClass, Method method) throws Throwable {
    String[] resources = null;
    var isDeploymentPresent = false;
    // Look for @Deployment Annotation
    for (var annotation : method.getAnnotations()) {
      // It can be behind a Proxy
      // Using annotation name comparison here due to isAssignableFrom not working here (probably due to multiple Classloaders involved)
      if (Proxy.isProxyClass(annotation.getClass()) && Deployment.class.getName().equals(annotation.annotationType().getName())) {
        resources = (String[]) Proxy.getInvocationHandler(annotation)
          .invoke(annotation, Deployment.class.getDeclaredMethod("resources"), null);
        isDeploymentPresent = true;
        break;
      } else if (annotation instanceof Deployment deploymentAnnotation) {
        resources = deploymentAnnotation.resources();
        isDeploymentPresent = true;
        break;
      }
    }
    if(isDeploymentPresent) {
      // if @Deployment Annotation is present but there are no resources specified use test class and method name to create a corresponding resource
      return resources.length > 0 ? resources : new String[] {TestHelper.getBpmnProcessDefinitionResource(testClass, method.getName())};
    }
    return null;
  }

  @AfterEach
  void after() {
    Arc.container().requestContext().deactivate();

    beanInstanceHandles.forEach(bean -> {
      try {
        bean.destroy();
      } catch (UnsupportedOperationException ignored) {
        // Eagerly destroying InjectableBusinessProcessContext is unsupported
        // See https://jira.camunda.com/browse/CAM-13755
      }
    });

    beanInstanceHandles.clear();

    if (deploymentId != null) {
      repositoryService.deleteDeployment(deploymentId, true, true, true);
      deploymentId = null;
    }

    beanManager = null;
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

  protected <T> T getBeanInstance(Class<T> clazz) {
    InjectableInstance<T> select = Arc.container().select(clazz);
    InstanceHandle<T> handle = select.getHandle();
    beanInstanceHandles.add(handle);
    return handle.get();
  }

  protected Object getBeanInstance(String name) {
    InstanceHandle<Object> instance = Arc.container().instance(name);
    beanInstanceHandles.add(instance);
    return instance.get();
  }

  protected BeanManager getBeanManager() {
    return ProgrammaticBeanLookup.lookup(BeanManager.class);
  }

  protected String deploy(Class<?> testClass, String methodName, String[] resources) {
    if (resources != null) {
      return TestHelper.annotationDeploymentSetUp(processEngine, resources, testClass, methodName);
    }
    return null;
  }
}
