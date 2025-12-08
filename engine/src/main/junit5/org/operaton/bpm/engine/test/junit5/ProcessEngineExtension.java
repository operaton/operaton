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
package org.operaton.bpm.engine.test.junit5;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;

import org.operaton.bpm.engine.*;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.diagnostics.PlatformDiagnosticsRegistry;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.persistence.entity.JobEntity;
import org.operaton.bpm.engine.impl.test.TestHelper;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.runtime.Job;
import org.operaton.bpm.engine.test.Deployment;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.util.ProcessEngineUtils;

/**
 * JUnit 5 Extension to create and inject a {@link ProcessEngine} into test classes.
 * <p>
 * This extension provides a managed instance of {@link ProcessEngine} and its services, which can
 * be automatically injected into test fields. The process engine configuration can be provided
 * through an {@code operaton.cfg.xml} file on the classpath or customized using a builder pattern.
 * </p>
 *
 * <h3>Basic Usage:</h3>
 * <pre>
 * <code>@ExtendWith(ProcessEngineExtension.class)</code>
 * class YourTest {
 *
 *   ProcessEngine processEngine;
 *   RuntimeService runtimeService;
 *   TaskService taskService;
 *   // other engine services
 *   ...
 * }
 * </pre>
 *
 * <p>
 * For advanced usage, where specific configurations are required, the extension can be registered
 * manually with a custom configuration file:
 * </p>
 * <pre>
 * <code>@RegisterExtension</code>
 * ProcessEngineExtension extension = ProcessEngineExtension.builder()
 *    .configurationResource("customConfiguration.xml")
 *    .build();
 * </pre>
 *
 * <h3>Annotations:</h3>
 * <ul>
 * <li>{@link Deployment} - Deploys a specified deployment before each test and
 * cascades deletion after each test execution.</li>
 * <li>{@link RequiredHistoryLevel} - Skips tests if the engine's history level does not meet the required level.</li>
 * </ul>
 *
 * <h3>Injected Services:</h3>
 * <p>
 * In addition to {@link ProcessEngine}, the extension also injects various BPM services:
 * {@link RepositoryService}, {@link RuntimeService}, {@link TaskService}, {@link HistoryService},
 * {@link IdentityService}, {@link ManagementService}, {@link FormService}, {@link FilterService},
 * {@link AuthorizationService}, {@link CaseService}, {@link ExternalTaskService}, and
 * {@link DecisionService}. Each of these services can be injected directly into the test class
 * fields to simplify access within tests.
 * </p>
 *
 * <h3>Builder Pattern:</h3>
 * <p>
 * This extension supports a fluent builder pattern for advanced configuration:
 * </p>
 * <pre>
 * <code>ProcessEngineExtension extension = ProcessEngineExtension.builder()
 *     .configurationResource("myCustomConfig.xml")
 *     .ensureCleanAfterTest(true)
 *     .build();</code>
 * </pre>
 * <ul>
 * <li>{@code configurationResource(String)} - Specifies a custom configuration file.</li>
 * <li>{@code ensureCleanAfterTest(boolean)} - Ensures a clean database state after each test.</li>
 * <li>{@code useProcessEngine(ProcessEngine)} - Reuses an existing process engine instance.</li>
 * <li>{@code manageDeployment(Deployment)} - Adds deployments that will be managed and cleaned up after tests.</li>
 * </ul>
 *
 * <h3>Setting History Level:</h3>
 * <p>
 * If you need the history service for your tests then you can specify the
 * required history level of the test method or class, using the
 * {@link RequiredHistoryLevel} annotation. If the current history level of the
 * process engine is lower than the specified one then the test is skipped.
 * </p>
 *
 * <h3>Test Lifecycle Callbacks:</h3>
 * <p>
 * This extension implements multiple JUnit lifecycle callbacks:
 * <ul>
 * <li>{@link TestInstancePostProcessor} - Initializes and injects the process engine instance before each test.</li>
 * <li>{@link BeforeEachCallback} - Sets up deployment and history level requirements before each test.</li>
 * <li>{@link AfterEachCallback} - Cleans up deployments, services, and resets the engine state after each test.</li>
 * <li>{@link AfterAllCallback} - Clears service references once all tests in a class have been executed.</li>
 * </ul>
 * </p>
 *
 * <h3>Database and Diagnostics:</h3>
 * <p>
 * After each test execution, the extension ensures a clean database state by validating and
 * cleaning the engine's cache and database if configured to do so.
 * This feature is particularly useful for tests that rely on database consistency.
 * The extension also resets the engine’s diagnostic state to maintain a clean testing environment.
 * </p>
 */
public class ProcessEngineExtension implements TestWatcher,
    TestInstancePostProcessor, BeforeEachCallback, AfterEachCallback, BeforeAllCallback,
    AfterAllCallback, ParameterResolver, ProcessEngineServices, ProcessEngineProvider {

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
  protected boolean ensureCleanAfterTest;
  protected List<String> additionalDeployments = new ArrayList<>();
  protected boolean randomName;
  protected boolean closeEngine;
  protected boolean closeEngineEachTest;

  protected Consumer<ProcessEngineConfigurationImpl> processEngineConfigurator;


  // SETUP

  public void initializeProcessEngine() {
    Consumer<ProcessEngineConfigurationImpl> configurator = processEngineConfigurator;
    if (randomName) {
      if (processEngineConfigurator == null) {
        configurator = config -> config.setProcessEngineName(ProcessEngineUtils.newRandomProcessEngineName());
      } else {
        configurator = config -> {
          config.setProcessEngineName(ProcessEngineUtils.newRandomProcessEngineName());
          processEngineConfigurator.accept(config);
        };
      }
    }
    processEngine = TestHelper.getProcessEngine(configurationResource, configurator);
    processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
  }

  public void initializeServices() {
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
  public void beforeEach(ExtensionContext context) {
    LOG.debug("beforeEach: {}", context.getDisplayName());

    if (!ProcessEngines.isRegisteredProcessEngine(processEngine.getName())) {
      initializeProcessEngine();
      initializeServices();
    }

    final Method testMethod = context.getTestMethod().orElseThrow(illegalStateException("testMethod not set"));
    final Class<?> testClass = context.getTestClass().orElseThrow(illegalStateException("testClass not set"));

    // we disable the authorization check when deploying before the test starts
    boolean authorizationEnabled = processEngineConfiguration.isAuthorizationEnabled();
    boolean tenantCheckEnabled = processEngineConfiguration.isTenantCheckEnabled();
    try {
      processEngineConfiguration.setAuthorizationEnabled(false);
      deploymentId = TestHelper.annotationDeploymentSetUp(processEngine, testClass, testMethod.getName(), null, testMethod.getParameterTypes());
      boolean hasRequiredHistoryLevel = TestHelper.annotationRequiredHistoryLevelCheck(processEngine, testClass, testMethod.getName(), testMethod.getParameterTypes());
      boolean hasRequiredDatabase = TestHelper.annotationRequiredDatabaseCheck(processEngine, testClass, testMethod.getName(), testMethod.getParameterTypes());
      Assumptions.assumeTrue(hasRequiredHistoryLevel, "ignored because the current history level is too low");
      Assumptions.assumeTrue(hasRequiredDatabase, "ignored because the database doesn't match the required ones");
    } finally {
      // after the initialization we restore authorization to the state defined by the test
      processEngineConfiguration.setAuthorizationEnabled(authorizationEnabled);
      processEngineConfiguration.setTenantCheckEnabled(tenantCheckEnabled);
    }

  }

  @Override
  public void afterEach(ExtensionContext context) {
    identityService.clearAuthentication();

    final String testMethod = context.getTestMethod().orElseThrow(illegalStateException("testMethod not set")).getName();
    final Class<?> testClass = context.getTestClass().orElseThrow(illegalStateException("testClass not set"));

    try {
      processEngineConfiguration.setTenantCheckEnabled(false);
      TestHelper.annotationDeploymentTearDown(processEngine, deploymentId, testClass, testMethod);
      deploymentId = null;
      for (String additionalDeployment : additionalDeployments) {
        TestHelper.deleteDeployment(processEngine, additionalDeployment);
      }
      additionalDeployments.clear();
    } finally {
      processEngine.getProcessEngineConfiguration().setTenantCheckEnabled(true);
    }

   TestHelper.resetIdGenerator(processEngineConfiguration);
   ClockUtil.reset();
   PlatformDiagnosticsRegistry.clear();

   TestHelper.clearUserOperationLog(processEngineConfiguration);

   // finally clear database and fail test if database is dirty
   if (ensureCleanAfterTest) {
     TestHelper.assertAndEnsureCleanDbAndCache(processEngine);
   }
   if (closeEngineEachTest) {
     processEngine.close();
   }
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    if (processEngine == null) {
      initializeProcessEngine();
      initializeServices();
    }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (closeEngine && processEngine != null) {
      try {
        deleteHistoryCleanupJob();
        processEngine.close();
      } finally {
        // null the references after all tests because the instance is hold in static reference
        processEngine = null;
        processEngineConfiguration = null;
        repositoryService = null;
        runtimeService = null;
        taskService = null;
        historyService = null;
        identityService = null;
        managementService = null;
        formService = null;
        filterService = null;
        authorizationService = null;
        caseService = null;
        externalTaskService = null;
        decisionService = null;
      }
    }
  }

  private void deleteHistoryCleanupJob() {
    if (processEngine == null) {
      return;
    }
    List<Job> jobs;
    try {
      jobs = processEngine.getHistoryService().findHistoryCleanupJobs();
    } catch (ProcessEngineException e) {
      jobs = Collections.emptyList();
    }

    for (final Job job: jobs) {
      ((ProcessEngineConfigurationImpl)processEngine.getProcessEngineConfiguration()).getCommandExecutorTxRequired().execute((Command<Void>) commandContext -> {
        commandContext.getJobManager().deleteJob((JobEntity) job);
        commandContext.getHistoricJobLogManager().deleteHistoricJobLogByJobId(job.getId());
        return null;
      });
    }
  }


  @Override
  public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
    if (processEngine == null) {
      initializeProcessEngine();
    }
    // allow other extensions to access the engine instance created by this extension
    context.getStore(ExtensionContext.Namespace.create("Operaton")).put(ProcessEngine.class, processEngine);
    initializeServices();
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

      serviceInstance.ifPresent(instance -> getAllFields(testInstance.getClass())
              .filter(field -> field.getType() != Object.class && field.getType().isAssignableFrom(serviceType))
              .forEach(field -> inject(testInstance, field, instance)));
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

  public ProcessEngineExtension configurator (Consumer<ProcessEngineConfigurationImpl> processEngineConfigurator) {
    this.processEngineConfigurator = processEngineConfigurator;
    return this;
  }

  /**
   * When set then the created ProcessEngine will be closed after all tests in the class have been executed.
   * <p>
   * Use this method before calling #{@link #build()}.
   * </p>
   */
  public ProcessEngineExtension closeEngineAfterAllTests() {
    this.closeEngine = true;
    return this;
  }

  /**
   * When set then the created ProcessEngine will be closed after each test and recreated afterwards.
   */
  public ProcessEngineExtension closeEngineAfterEachTest() {
    this.closeEngineEachTest = true;
    return this;
  }

  /**
   * Sets the process engine name to a random name.
   * <p>
   * Use this method before calling #{@link #build()}.
   * </p>
   */
  public ProcessEngineExtension randomEngineName() {
    this.randomName = true;
    return this;
  }

  public ProcessEngineExtension build() {
    return this;
  }

  // HELPER

  protected Supplier<IllegalStateException> illegalStateException(String msg) {
    return () -> new IllegalStateException(msg);
  }

  /**
   * @deprecated Use {@link #inject(Object, Field, Object)} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
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

  @Override
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
