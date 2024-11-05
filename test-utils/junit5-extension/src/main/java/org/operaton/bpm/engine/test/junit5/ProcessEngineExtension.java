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
package org.operaton.bpm.engine.test.junit5;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.*;
import org.operaton.bpm.engine.*;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.diagnostics.PlatformDiagnosticsRegistry;
import org.operaton.bpm.engine.impl.test.TestHelper;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Junit 5 Extension to create and inject a {@link ProcessEngine} into the test class.
 * <p>
 * If you provide a {@code operaton.cfg.xml} file on the classpath. This file is used to configure the process engine.
 * <br>
 * Usage:
 * </p>
 * <pre>
 * <code>@ExtendWith(ProcessEngineExtension.class)</code>
 * class YourTest {
 *
 *   // provide a property where the extension can inject the process engine or its services
 *   ProcessEngine processEngine;
 *   ProcessEngineConfiguration processEngineConfiguration;
 *   RuntimeService runtimeService;
 *
 *   ...
 * }
 * </pre>
 *
 * <p>
 * If you want to choose the {@code operaton.cfg.xml} file that is used in the test programmatically,
 * you can register the extension directly and use the builder pattern to configure it.
 * <br>
 * Usage with configuration:
 *
 * </p>
 * Usage:
 * <p>
 * <pre>
 * <code>@RegisterExtension</code>
 * ProcessEngineExtension extension = ProcessEngineExtension.builder()
 *    .configurationResource("myConfigurationFile.xml")
 *    .build();}
 * </pre>
 * </p>
 * <p>
 * You can declare a deployment with the {@link Deployment} annotation. This
 * base class will make sure that this deployment gets deployed before the setUp
 * and {@link RepositoryService#deleteDeployment(String, boolean) cascade
 * deleted} after the tearDown.
 * </p>
 * <p>
 * If you need the history service for your tests then you can specify the
 * required history level of the test method or class, using the
 * {@link RequiredHistoryLevel} annotation. If the current history level of the
 * process engine is lower than the specified one then the test is skipped.
 * </p>
 */
public class ProcessEngineExtension implements TestWatcher,
    TestInstancePostProcessor, BeforeTestExecutionCallback, AfterTestExecutionCallback,
    AfterAllCallback, ParameterResolver, ProcessEngineServices {

  protected static final Logger LOG = ProcessEngineLogger.TEST_LOGGER.getLogger();

  protected ProcessEngine processEngine;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RepositoryService repositoryService;
  protected RuntimeService runtimeService;
  protected TaskService taskService;
  protected HistoryService historyService;
  protected IdentityService identityService;
  protected ManagementService managementService;
  protected FormService formService;
  protected FilterService filterService;
  protected AuthorizationService authorizationService;
  protected CaseService caseService;
  protected ExternalTaskService externalTaskService;
  protected DecisionService decisionService;

  protected String configurationResource = "operaton.cfg.xml";
  protected String deploymentId;
  protected boolean ensureCleanAfterTest = false;
  protected List<String> additionalDeployments = new ArrayList<>();

  // SETUP

  protected void initializeProcessEngine() {
    processEngine = TestHelper.getProcessEngine(configurationResource);
    processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
  }

  protected void initializeServices() {
    processEngineConfiguration = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration();
    repositoryService = processEngine.getRepositoryService();
    runtimeService = processEngine.getRuntimeService();
    taskService = processEngine.getTaskService();
    historyService = processEngine.getHistoryService();
    identityService = processEngine.getIdentityService();
    managementService = processEngine.getManagementService();
    formService = processEngine.getFormService();
    authorizationService = processEngine.getAuthorizationService();
    caseService = processEngine.getCaseService();
    filterService = processEngine.getFilterService();
    externalTaskService = processEngine.getExternalTaskService();
    decisionService = processEngine.getDecisionService();
  }

  protected void clearServiceReferences() {
    processEngineConfiguration = null;
    repositoryService = null;
    runtimeService = null;
    taskService = null;
    formService = null;
    historyService = null;
    identityService = null;
    managementService = null;
    authorizationService = null;
    caseService = null;
    filterService = null;
    externalTaskService = null;
    decisionService = null;
  }

  // TEST EXECUTION

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(ProcessEngine.class);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    if (ProcessEngine.class.equals(parameterContext.getParameter().getType())) {
      LOG.debug("resolve the processEngine as parameter");
      return getProcessEngine();
    } else {
      return null;
    }
  }

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    LOG.debug("beforeTestExecution: {}", context.getDisplayName());

    final Method testMethod = context.getTestMethod().orElseThrow(illegalStateException("testMethod not set"));
    final Class<?> testClass = context.getTestClass().orElseThrow(illegalStateException("testClass not set"));

    deploymentId = TestHelper.annotationDeploymentSetUp(processEngine, testClass, testMethod.getName(), null, testMethod.getParameterTypes());
    boolean hasRequiredHistoryLevel = TestHelper.annotationRequiredHistoryLevelCheck(processEngine, testClass, testMethod.getName(), testMethod.getParameterTypes());
    boolean hasRequiredDatabase = TestHelper.annotationRequiredDatabaseCheck(processEngine, testClass, testMethod.getName(), testMethod.getParameterTypes());

    Assumptions.assumeTrue(hasRequiredHistoryLevel, "ignored because the current history level is too low");
    Assumptions.assumeTrue(hasRequiredDatabase, "ignored because the database doesn't match the required ones");
  }

  @Override
  public void afterTestExecution(ExtensionContext context) {
    final String testMethod = context.getTestMethod().orElseThrow(illegalStateException("testMethod not set")).getName();
    final Class<?> testClass = context.getTestClass().orElseThrow(illegalStateException("testClass not set"));

   TestHelper.annotationDeploymentTearDown(processEngine, deploymentId, testClass, testMethod);
   deploymentId = null;
   for (String additionalDeployment : additionalDeployments) {
     TestHelper.deleteDeployment(processEngine, additionalDeployment);
   }
   additionalDeployments.clear();

   TestHelper.resetIdGenerator(processEngineConfiguration);
   ClockUtil.reset();
   PlatformDiagnosticsRegistry.clear();

   // finally clear database and fail test if database is dirty
   if (ensureCleanAfterTest) {
     TestHelper.assertAndEnsureCleanDbAndCache(processEngine);
   }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    clearServiceReferences();
  }

  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
    if (processEngine == null) {
      initializeProcessEngine();
      // allow other extensions to access the engine instance created by this extension
      context.getStore(ExtensionContext.Namespace.create("Operaton")).put(ProcessEngine.class, processEngine);
    }
    getAllFields(testInstance.getClass())
            .filter(field -> field.getType() == ProcessEngine.class)
            .forEach(field -> inject(testInstance, field, processEngine));
    getAllFields(testInstance.getClass())
            .filter(field -> ProcessEngineConfiguration.class.isAssignableFrom(field.getType()))
            .forEach(field -> inject(testInstance, field, processEngine.getProcessEngineConfiguration()));

    Arrays.stream(ProcessEngineServices.class.getDeclaredMethods())
            .filter(method -> method.getName().startsWith("get"))
            .map(Method::getReturnType)
            .forEach(serviceType -> injectProcessEngineService(testInstance, serviceType));
  }

  private Stream<Field> getAllFields(Class<?> clazz) {
    Stream<Field> fields = Stream.of(clazz.getDeclaredFields());
    Class<?> superclass = clazz.getSuperclass();

    return superclass != null
            ? Stream.concat(fields, getAllFields(superclass))
            : fields;
  }

  private void injectProcessEngineService(Object testInstance, Class<?> serviceType) {
    Objects.requireNonNull(processEngine, "ProcessEngine not initialized");
    Optional<Object> serviceInstance = Arrays.stream(ProcessEngineServices.class.getDeclaredMethods())
              .filter(method -> method.getReturnType() == serviceType)
              .findFirst()
              .map(method -> {
                try {
                  return method.invoke(processEngine);
                } catch (IllegalAccessException | InvocationTargetException e) {
                  throw new RuntimeException(e);
                }
              });

    if (serviceInstance.isPresent()) {
      getAllFields(testInstance.getClass())
              .filter(field -> field.getType().isAssignableFrom(serviceType))
              .forEach(field -> inject(testInstance, field, serviceInstance.get()));
    }
  }

  // FLUENT BUILDER

  public static ProcessEngineExtension builder() {
    return new ProcessEngineExtension();
  }

  public ProcessEngineExtension configurationResource(String configurationResource) {
    this.configurationResource = configurationResource;
    return this;
  }

  public ProcessEngineExtension useProcessEngine(ProcessEngine engine) {
    this.setProcessEngine(engine);
    return this;
  }

  public ProcessEngineExtension ensureCleanAfterTest(boolean ensureCleanAfterTest) {
    this.ensureCleanAfterTest = ensureCleanAfterTest;
    return this;
  }

  public ProcessEngineExtension manageDeployment(org.operaton.bpm.engine.repository.Deployment deployment) {
    this.additionalDeployments.add(deployment.getId());
    return this;
  }

  public ProcessEngineExtension build() {
    if (processEngine == null) {
      initializeProcessEngine();
    }
    initializeServices();
    return this;
  }

  // HELPER

  protected Supplier<IllegalStateException> illegalStateException(String msg) {
    return () -> new IllegalStateException(msg);
  }

  /**
   * @deprecated Use {@link #inject(Object, Field, Object)} instead
   */
  @Deprecated(forRemoval = true)
  protected void inject(Object instance, Field field) {
    inject(instance, field, processEngine);
  }

  protected void inject(Object testInstance, Field field, Object serviceInstance) {
    field.setAccessible(true);
    try {
      field.set(testInstance, serviceInstance);
    } catch (IllegalAccessException iae) {
      throw new RuntimeException(iae);
    }
  }

  // GETTER / SETTER

  public void setCurrentTime(Date currentTime) {
    ClockUtil.setCurrentTime(currentTime);
  }

  public ProcessEngine getProcessEngine() {
    return processEngine;
  }

  public void setProcessEngine(ProcessEngine processEngine) {
    this.processEngine = processEngine;
    initializeServices();
  }

  public String getConfigurationResource() {
    return configurationResource;
  }

  public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
    return processEngineConfiguration;
  }

  @Override
  public RepositoryService getRepositoryService() {
    return repositoryService;
  }

  public void setRepositoryService(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  @Override
  public RuntimeService getRuntimeService() {
    return runtimeService;
  }

  public void setRuntimeService(RuntimeService runtimeService) {
    this.runtimeService = runtimeService;
  }

  @Override
  public TaskService getTaskService() {
    return taskService;
  }

  public void setTaskService(TaskService taskService) {
    this.taskService = taskService;
  }

  @Override
  public HistoryService getHistoryService() {
    return historyService;
  }

  public void setHistoryService(HistoryService historyService) {
    this.historyService = historyService;
  }

  @Override
  public IdentityService getIdentityService() {
    return identityService;
  }

  public void setIdentityService(IdentityService identityService) {
    this.identityService = identityService;
  }

  @Override
  public ManagementService getManagementService() {
    return managementService;
  }

  public void setManagementService(ManagementService managementService) {
    this.managementService = managementService;
  }

  @Override
  public FormService getFormService() {
    return formService;
  }

  public void setFormService(FormService formService) {
    this.formService = formService;
  }

  @Override
  public FilterService getFilterService() {
    return filterService;
  }

  public void setFilterService(FilterService filterService) {
    this.filterService = filterService;
  }

  @Override
  public AuthorizationService getAuthorizationService() {
    return authorizationService;
  }

  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Override
  public CaseService getCaseService() {
    return caseService;
  }

  public void setCaseService(CaseService caseService) {
    this.caseService = caseService;
  }

  @Override
  public ExternalTaskService getExternalTaskService() {
    return externalTaskService;
  }

  public void setExternalTaskService(ExternalTaskService externalTaskService) {
    this.externalTaskService = externalTaskService;
  }

  @Override
  public DecisionService getDecisionService() {
    return decisionService;
  }

  public void setDecisionService(DecisionService decisionService) {
    this.decisionService = decisionService;
  }

  public void setProcessEngineConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
    this.processEngineConfiguration = processEngineConfiguration;
  }

  public void setConfigurationResource(String configurationResource) {
    this.configurationResource = configurationResource;
  }

  public String getDeploymentId() {
    return deploymentId;
  }

  public boolean isEnsureCleanAfterTest() {
    return ensureCleanAfterTest;
  }

  public void setEnsureCleanAfterTest(boolean ensureCleanAfterTest) {
    this.ensureCleanAfterTest = ensureCleanAfterTest;
  }

}
