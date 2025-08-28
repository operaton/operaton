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
package org.operaton.bpm.container.impl.jboss.test;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLStreamException;

import org.operaton.bpm.container.impl.jboss.config.ManagedProcessEngineMetadata;
import org.operaton.bpm.container.impl.jboss.extension.Attribute;
import org.operaton.bpm.container.impl.jboss.extension.BpmPlatformExtension;
import org.operaton.bpm.container.impl.jboss.extension.Element;
import org.operaton.bpm.container.impl.jboss.extension.ModelConstants;
import org.operaton.bpm.container.impl.jboss.extension.SubsystemAttributeDefinitons;
import org.operaton.bpm.container.impl.jboss.service.MscManagedProcessEngineController;
import org.operaton.bpm.container.impl.jboss.service.ServiceNames;
import org.operaton.bpm.container.impl.metadata.spi.ProcessEnginePluginXml;
import org.operaton.bpm.container.impl.plugin.BpmPlatformPlugin;
import org.operaton.bpm.container.impl.plugin.BpmPlatformPlugins;
import org.operaton.bpm.engine.impl.jobexecutor.JobExecutor;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.threads.ManagedQueueExecutorService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.junit.jupiter.api.Test;


/**
 *
 * @author nico.rehwaldt@camunda.com
 * @author christian.lipphardt@camunda.com
 */
public class JBossSubsystemXMLTest extends AbstractSubsystemTest {

  public static final String SUBSYSTEM_WITH_SINGLE_ENGINE = "subsystemWithSingleEngine.xml";
  public static final String SUBSYSTEM_WITH_ENGINES = "subsystemWithEngines.xml";
  public static final String SUBSYSTEM_WITH_PROCESS_ENGINES_ELEMENT_ONLY = "subsystemWithProcessEnginesElementOnly.xml";
  public static final String SUBSYSTEM_WITH_ENGINES_AND_PROPERTIES = "subsystemWithEnginesAndProperties.xml";
  public static final String SUBSYSTEM_WITH_ENGINES_PROPERTIES_PLUGINS = "subsystemWithEnginesPropertiesPlugins.xml";
  public static final String SUBSYSTEM_WITH_DUPLICATE_ENGINE_NAMES = "subsystemWithDuplicateEngineNames.xml";
  public static final String SUBSYSTEM_WITH_JOB_EXECUTOR = "subsystemWithJobExecutor.xml";
  public static final String SUBSYSTEM_WITH_PROCESS_ENGINES_AND_JOB_EXECUTOR = "subsystemWithProcessEnginesAndJobExecutor.xml";
  public static final String SUBSYSTEM_WITH_JOB_EXECUTOR_AND_PROPERTIES = "subsystemWithJobExecutorAndProperties.xml";
  public static final String SUBSYSTEM_WITH_JOB_EXECUTOR_WITHOUT_ACQUISITION_STRATEGY = "subsystemWithJobExecutorAndWithoutAcquisitionStrategy.xml";
  public static final String SUBSYSTEM_WITH_ALL_OPTIONS = "subsystemWithAllOptions.xml";
  public static final String SUBSYSTEM_WITH_REQUIRED_OPTIONS = "subsystemWithRequiredOptions.xml";
  public static final String SUBSYSTEM_WITH_NO_OPTIONS = "subsystemWithNoOptions.xml";
  public static final String SUBSYSTEM_WITH_ALL_OPTIONS_WITH_EXPRESSIONS = "subsystemWithAllOptionsWithExpressions.xml";

  public static final String LOCK_TIME_IN_MILLIS = "lockTimeInMillis";
  public static final String WAIT_TIME_IN_MILLIS = "waitTimeInMillis";
  public static final String MAX_JOBS_PER_ACQUISITION = "maxJobsPerAcquisition";

  public static final ServiceName PLATFORM_SERVICE_NAME = ServiceNames.forMscRuntimeContainerDelegate();
  public static final ServiceName PLATFORM_JOBEXECUTOR_SERVICE_NAME = ServiceNames.forMscExecutorService();
  public static final ServiceName PLATFORM_BPM_PLATFORM_PLUGINS_SERVICE_NAME = ServiceNames.forBpmPlatformPlugins();
  public static final ServiceName PLATFORM_JOBEXECUTOR_MANAGED_THREAD_POOL_SERVICE_NAME =
      ServiceNames.forManagedThreadPool(SubsystemAttributeDefinitons.THREAD_POOL_NAME.getDefaultValue().asString());

  public static final ServiceName PROCESS_ENGINE_SERVICE_BINDING_SERVICE_NAME = ContextNames.GLOBAL_CONTEXT_SERVICE_NAME
    .append("operaton-bpm-platform")
    .append("process-engine")
    .append("ProcessEngineService!org.operaton.bpm.ProcessEngineService");

  public JBossSubsystemXMLTest() {
    super(ModelConstants.SUBSYSTEM_NAME, new BpmPlatformExtension(), getSubsystemRemoveOrderComparator());
  }

  private static Map<String, String> EXPRESSION_PROPERTIES = new HashMap<>();

  static {
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.process-engine.test.isDefault", "true");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.process-engine.test.datasource", "java:jboss/datasources/ExampleDS");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.process-engine.test.history-level", "audit");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.process-engine.test.configuration", "org.operaton.bpm.container.impl.jboss.config.ManagedJtaProcessEngineConfiguration");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.process-engine.test.property.job-acquisition-name", "default");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.process-engine.test.plugin.ldap.class", "org.operaton.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.process-engine.test.plugin.ldap.property.test", "abc");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.process-engine.test.plugin.ldap.property.number", "123");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.process-engine.test.plugin.ldap.property.bool", "true");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.job-executor.thread-pool-name", "job-executor-tp");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.job-executor.core-threads", "5");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.job-executor.max-threads", "15");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.job-executor.queue-length", "15");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.job-executor.keepalive-time", "10");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.job-executor.allow-core-timeout", "false");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.job-executor.job-acquisition.default.acquisition-strategy", "SEQUENTIAL");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.job-executor.job-acquisition.default.property.lockTimeInMillis", "300000");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.job-executor.job-acquisition.default.property.waitTimeInMillis", "5000");
    EXPRESSION_PROPERTIES.put("org.operaton.bpm.jboss.job-executor.job-acquisition.default.property.maxJobsPerAcquisition", "3");
  }

  @Test
  public void testParseSubsystemXml() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_PROCESS_ENGINES_ELEMENT_ONLY);

    List<ModelNode> operations = parse(subsystemXml);

    assertThat(operations.size()).isEqualTo(1);
    //The add subsystem operation will happen first
    ModelNode addSubsystem = operations.get(0);
    assertThat(addSubsystem.get(OP).asString()).isEqualTo(ADD);
    PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
    assertThat(addr.size()).isEqualTo(1);
    PathElement element = addr.getElement(0);
    assertThat(element.getKey()).isEqualTo(SUBSYSTEM);
    assertThat(element.getValue()).isEqualTo(ModelConstants.SUBSYSTEM_NAME);
  }

  @Test
  public void testParseSubsystemXmlWithEngines() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_ENGINES);

    List<ModelNode> operations = parse(subsystemXml);
    assertThat(operations.size()).isEqualTo(3);
  }

  @Test
  public void testParseSubsystemXmlWithEnginesAndProperties() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_ENGINES_AND_PROPERTIES);

    List<ModelNode> operations = parse(subsystemXml);
    assertThat(operations.size()).isEqualTo(5);
  }

  @Test
  public void testParseSubsystemXmlWithEnginesPropertiesPlugins() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_ENGINES_PROPERTIES_PLUGINS);

    List<ModelNode> operations = parse(subsystemXml);
    assertThat(operations.size()).isEqualTo(3);
  }

  @Test
  public void testInstallSubsystemWithEnginesPropertiesPlugins() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_ENGINES_PROPERTIES_PLUGINS);

    KernelServices services = createKernelServicesBuilder(null)
            .setSubsystemXml(subsystemXml)
            .build();

    ServiceContainer container = services.getContainer();

    assertThat(container.getRequiredService(PLATFORM_SERVICE_NAME)).withFailMessage("platform service should be installed").isNotNull();
    assertThat(container.getRequiredService(PROCESS_ENGINE_SERVICE_BINDING_SERVICE_NAME)).withFailMessage("process engine service should be bound in JNDI").isNotNull();

    ServiceController<?> defaultEngineService = container.getService(ServiceNames.forManagedProcessEngine("__default"));

    assertThat(defaultEngineService).withFailMessage("process engine controller for engine __default is installed ").isNotNull();

    ManagedProcessEngineMetadata metadata = ((MscManagedProcessEngineController) defaultEngineService.getService()).getProcessEngineMetadata();
    Map<String, String> configurationProperties = metadata.getConfigurationProperties();
    assertThat(configurationProperties.get("job-name")).isEqualTo("default");
    assertThat(configurationProperties.get("job-acquisition")).isEqualTo("default");
    assertThat(configurationProperties.get("job-acquisition-name")).isEqualTo("default");

    Map<String, String> foxLegacyProperties = metadata.getFoxLegacyProperties();
    assertThat(foxLegacyProperties.isEmpty()).isTrue();

    assertThat(container.getRequiredService(ServiceNames.forManagedProcessEngine("__default"))).withFailMessage("process engine controller for engine __default is installed ").isNotNull();
    assertThat(container.getRequiredService(ServiceNames.forManagedProcessEngine("__test"))).withFailMessage("process engine controller for engine __test is installed ").isNotNull();

    // check we have parsed the plugin configurations
    metadata = ((MscManagedProcessEngineController) container.getRequiredService(ServiceNames.forManagedProcessEngine("__test")).getService())
            .getProcessEngineMetadata();
    List<ProcessEnginePluginXml> pluginConfigurations = metadata.getPluginConfigurations();

    ProcessEnginePluginXml processEnginePluginXml = pluginConfigurations.get(0);
    assertThat(processEnginePluginXml.getPluginClass()).isEqualTo("org.operaton.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin");
    Map<String, String> processEnginePluginXmlProperties = processEnginePluginXml.getProperties();
    assertThat(processEnginePluginXmlProperties.get("test")).isEqualTo("abc");
    assertThat(processEnginePluginXmlProperties.get("number")).isEqualTo("123");
    assertThat(processEnginePluginXmlProperties.get("bool")).isEqualTo("true");

    processEnginePluginXml = pluginConfigurations.get(1);
    assertThat(processEnginePluginXml.getPluginClass()).isEqualTo("org.operaton.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin");
    processEnginePluginXmlProperties = processEnginePluginXml.getProperties();
    assertThat(processEnginePluginXmlProperties.get("test")).isEqualTo("cba");
    assertThat(processEnginePluginXmlProperties.get("number")).isEqualTo("321");
    assertThat(processEnginePluginXmlProperties.get("bool")).isEqualTo("false");

    // test correct subsystem removal
    assertRemoveSubsystemResources(services);
    try {
      ServiceController<?> service = container.getRequiredService(ServiceNames.forManagedProcessEngine("__default"));
      fail("Service '" + service.getName() + "' should have been removed.");
    } catch (Exception expected) {
      // nop
    }
    try {
      ServiceController<?> service = container.getRequiredService(ServiceNames.forManagedProcessEngine("__test"));
      fail("Service '" + service.getName() + "' should have been removed.");
    } catch (Exception expected) {
      // nop
    }
  }

  @Test
  public void testInstallSubsystemXml() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_PROCESS_ENGINES_ELEMENT_ONLY);

    KernelServices services = createKernelServicesBuilder(null)
        .setSubsystemXml(subsystemXml)
        .build();

    ServiceContainer container = services.getContainer();

    assertThat(container.getService(PLATFORM_SERVICE_NAME)).withFailMessage("platform service should be installed").isNotNull();
    assertThat(container.getService(PROCESS_ENGINE_SERVICE_BINDING_SERVICE_NAME)).withFailMessage("process engine service should be bound in JNDI").isNotNull();
    assertThat(container.getService(PLATFORM_JOBEXECUTOR_SERVICE_NAME)).isNull();
  }

  @Test
  public void testInstallSubsystemXmlPlatformPlugins() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_PROCESS_ENGINES_ELEMENT_ONLY);

    KernelServices services = createKernelServicesBuilder(null)
        .setSubsystemXml(subsystemXml)
        .build();

    ServiceContainer container = services.getContainer();
    ServiceController<?> serviceController = container.getService(PLATFORM_BPM_PLATFORM_PLUGINS_SERVICE_NAME);
    assertThat(serviceController).isNotNull();
    Object platformPlugins = serviceController.getValue();
    assertThat(platformPlugins).isNotNull();
    assertThat(platformPlugins instanceof BpmPlatformPlugins).isTrue();
    List<BpmPlatformPlugin> plugins = ((BpmPlatformPlugins) platformPlugins).getPlugins();
    assertThat(plugins.size()).isEqualTo(1);
    assertThat(plugins.get(0) instanceof ExampleBpmPlatformPlugin).isTrue();
  }

  @Test
  public void testInstallSubsystemWithEnginesXml() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_ENGINES);

    KernelServices services = createKernelServicesBuilder(null)
        .setSubsystemXml(subsystemXml)
        .build();


    ServiceContainer container = services.getContainer();
    assertThat(container.getService(PLATFORM_SERVICE_NAME)).withFailMessage("platform service should be installed").isNotNull();
    assertThat(container.getService(PROCESS_ENGINE_SERVICE_BINDING_SERVICE_NAME)).withFailMessage("process engine service should be bound in JNDI").isNotNull();

    assertThat(container.getService(ServiceNames.forManagedProcessEngine("__default"))).withFailMessage("process engine controller for engine __default is installed ").isNotNull();
    assertThat(container.getService(ServiceNames.forManagedProcessEngine("__test"))).withFailMessage("process engine controller for engine __test is installed ").isNotNull();
  }

  @Test
  public void testInstallSubsystemWithEnginesAndPropertiesXml() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_ENGINES_AND_PROPERTIES);

    KernelServices services = createKernelServicesBuilder(null)
        .setSubsystemXml(subsystemXml)
        .build();
    ServiceContainer container = services.getContainer();


    assertThat(container.getService(PLATFORM_SERVICE_NAME)).withFailMessage("platform service should be installed").isNotNull();
    assertThat(container.getService(PROCESS_ENGINE_SERVICE_BINDING_SERVICE_NAME)).withFailMessage("process engine service should be bound in JNDI").isNotNull();

    ServiceController<?> defaultEngineService = container.getService(ServiceNames.forManagedProcessEngine("__default"));

    assertThat(defaultEngineService).withFailMessage("process engine controller for engine __default is installed ").isNotNull();

    ManagedProcessEngineMetadata metadata = ((MscManagedProcessEngineController) defaultEngineService.getService()).getProcessEngineMetadata();
    Map<String, String> configurationProperties = metadata.getConfigurationProperties();
    assertThat(configurationProperties.get("job-name")).isEqualTo("default");
    assertThat(configurationProperties.get("job-acquisition")).isEqualTo("default");
    assertThat(configurationProperties.get("job-acquisition-name")).isEqualTo("default");

    Map<String, String> foxLegacyProperties = metadata.getFoxLegacyProperties();
    assertThat(foxLegacyProperties.isEmpty()).isTrue();

    assertThat(container.getService(ServiceNames.forManagedProcessEngine("__test"))).withFailMessage("process engine controller for engine __test is installed ").isNotNull();
    assertThat(container.getService(ServiceNames.forManagedProcessEngine("__emptyPropertiesTag"))).withFailMessage("process engine controller for engine __emptyPropertiesTag is installed ").isNotNull();
    assertThat(container.getService(ServiceNames.forManagedProcessEngine("__noPropertiesTag"))).withFailMessage("process engine controller for engine __noPropertiesTag is installed ").isNotNull();
  }

  @Test
  public void testInstallSubsystemWithDuplicateEngineNamesXml() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_DUPLICATE_ENGINE_NAMES);

    try {
      createKernelServicesBuilder(null)
          .setSubsystemXml(subsystemXml)
          .build();

    } catch (XMLStreamException fpe) {
      assertThat(fpe.getNestedException().getMessage())
              .as("Duplicate process engine detected!")
              .contains("A process engine with name '__test' already exists.");
    }
  }

  @Test
  public void testInstallSubsystemWithSingleEngineXml() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_SINGLE_ENGINE);

    KernelServices services = createKernelServicesBuilder(null)
        .setSubsystemXml(subsystemXml)
        .build();
    ServiceContainer container = services.getContainer();

    assertThat(container.getService(PLATFORM_SERVICE_NAME)).withFailMessage("platform service should be installed").isNotNull();
    assertThat(container.getService(PROCESS_ENGINE_SERVICE_BINDING_SERVICE_NAME)).withFailMessage("process engine service should be bound in JNDI").isNotNull();

    assertThat(container.getService(ServiceNames.forManagedProcessEngine("__default"))).withFailMessage("process engine controller for engine __default is installed ").isNotNull();

    String persistedSubsystemXml = services.getPersistedSubsystemXml();
    compareXml(null, subsystemXml, persistedSubsystemXml);
  }

  @Test
  public void testParseSubsystemWithJobExecutorXml() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_JOB_EXECUTOR);
//    System.out.println(normalizeXML(subsystemXml));

    List<ModelNode> operations = parse(subsystemXml);
//    System.out.println(operations);
    assertThat(operations.size()).isEqualTo(4);

    ModelNode jobExecutor = operations.get(1);
    PathAddress pathAddress = PathAddress.pathAddress(jobExecutor.get(ModelDescriptionConstants.OP_ADDR));
    assertThat(pathAddress.size()).isEqualTo(2);

    PathElement element = pathAddress.getElement(0);
    assertThat(element.getKey()).isEqualTo(ModelDescriptionConstants.SUBSYSTEM);
    assertThat(element.getValue()).isEqualTo(ModelConstants.SUBSYSTEM_NAME);
    element = pathAddress.getElement(1);
    assertThat(element.getKey()).isEqualTo(Element.JOB_EXECUTOR.getLocalName());
    assertThat(element.getValue()).isEqualTo(Attribute.DEFAULT.getLocalName());

    assertThat(jobExecutor.get(Element.THREAD_POOL_NAME.getLocalName()).asString()).isEqualTo("job-executor-tp");

    ModelNode jobAcquisition = operations.get(2);
    assertThat(jobAcquisition.get(Attribute.NAME.getLocalName()).asString()).isEqualTo("default");
    assertThat(jobAcquisition.get(Element.ACQUISITION_STRATEGY.getLocalName()).asString()).isEqualTo("SEQUENTIAL");
    assertThat(jobAcquisition.has(Element.PROPERTIES.getLocalName())).isFalse();

    jobAcquisition = operations.get(3);
    assertThat(jobAcquisition.get(Attribute.NAME.getLocalName()).asString()).isEqualTo("anders");
    assertThat(jobAcquisition.get(Element.ACQUISITION_STRATEGY.getLocalName()).asString()).isEqualTo("SEQUENTIAL");
  }

  @Test
  public void testInstallSubsystemWithJobExecutorXml() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_JOB_EXECUTOR);
    KernelServices services = createKernelServicesBuilder(null)
        .setSubsystemXml(subsystemXml)
        .build();
    ServiceContainer container = services.getContainer();

    commonSubsystemServicesAreInstalled(container);
  }

  @Test
  public void testParseSubsystemWithJobExecutorAndPropertiesXml() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_JOB_EXECUTOR_AND_PROPERTIES);

    List<ModelNode> operations = parse(subsystemXml);
    assertThat(operations.size()).isEqualTo(5);

    // "default" job acquisition ///////////////////////////////////////////////////////////
    ModelNode jobAcquisition = operations.get(2);
    assertThat(jobAcquisition.get(Attribute.NAME.getLocalName()).asString()).isEqualTo("default");
    assertThat(jobAcquisition.has(Element.PROPERTIES.getLocalName())).isFalse();

    // "anders" job acquisition ////////////////////////////////////////////////////////////
    jobAcquisition = operations.get(3);
    assertThat(jobAcquisition.get(Attribute.NAME.getLocalName()).asString()).isEqualTo("anders");
    assertThat(jobAcquisition.has(Element.PROPERTIES.getLocalName())).isTrue();
    assertThat(jobAcquisition.hasDefined(Element.PROPERTIES.getLocalName())).isTrue();

    ModelNode properties = jobAcquisition.get(Element.PROPERTIES.getLocalName());
    assertThat(properties.asPropertyList().size()).isEqualTo(3);

    assertThat(properties.has(LOCK_TIME_IN_MILLIS)).isTrue();
    assertThat(properties.hasDefined(LOCK_TIME_IN_MILLIS)).isTrue();
    assertThat(properties.get(LOCK_TIME_IN_MILLIS).asInt()).isEqualTo(600000);

    assertThat(properties.has(WAIT_TIME_IN_MILLIS)).isTrue();
    assertThat(properties.hasDefined(WAIT_TIME_IN_MILLIS)).isTrue();
    assertThat(properties.get(WAIT_TIME_IN_MILLIS).asInt()).isEqualTo(10000);

    assertThat(properties.has(MAX_JOBS_PER_ACQUISITION)).isTrue();
    assertThat(properties.hasDefined(MAX_JOBS_PER_ACQUISITION)).isTrue();
    assertThat(properties.get(MAX_JOBS_PER_ACQUISITION).asInt()).isEqualTo(5);

    // "mixed" job acquisition ////////////////////////////////////////////////////////////
    jobAcquisition = operations.get(4);
    assertThat(jobAcquisition.get(Attribute.NAME.getLocalName()).asString()).isEqualTo("mixed");
    assertThat(jobAcquisition.has(Element.PROPERTIES.getLocalName())).isTrue();
    assertThat(jobAcquisition.hasDefined(Element.PROPERTIES.getLocalName())).isTrue();

    properties = jobAcquisition.get(Element.PROPERTIES.getLocalName());
    assertThat(properties.asPropertyList().size()).isEqualTo(1);

    assertThat(properties.has(LOCK_TIME_IN_MILLIS)).isTrue();
    assertThat(properties.hasDefined(LOCK_TIME_IN_MILLIS)).isTrue();
    assertThat(properties.get(LOCK_TIME_IN_MILLIS).asInt()).isEqualTo(500000);

    assertThat(properties.has(WAIT_TIME_IN_MILLIS)).isFalse();
    assertThat(properties.hasDefined(WAIT_TIME_IN_MILLIS)).isFalse();
    assertThat(properties.has(MAX_JOBS_PER_ACQUISITION)).isFalse();
    assertThat(properties.hasDefined(MAX_JOBS_PER_ACQUISITION)).isFalse();


  }

  @Test
  public void testInstallSubsystemWithJobExecutorAndPropertiesXml() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_JOB_EXECUTOR_AND_PROPERTIES);

    KernelServices services = createKernelServicesBuilder(null)
        .setSubsystemXml(subsystemXml)
        .build();
    ServiceContainer container = services.getContainer();

    commonSubsystemServicesAreInstalled(container);

    // "default" job acquisition ///////////////////////////////////////////////////////////
    ServiceController<?> defaultJobAcquisitionService = container.getService(ServiceNames.forMscRuntimeContainerJobExecutorService("default"));
    assertThat(defaultJobAcquisitionService).withFailMessage("platform job acquisition service 'default' should be installed").isNotNull();

    Object value = defaultJobAcquisitionService.getValue();
    assertThat(value).isNotNull();
    assertThat(value instanceof JobExecutor).isTrue();

    JobExecutor defaultJobExecutor = (JobExecutor) value;
    assertThat(defaultJobExecutor.getLockTimeInMillis()).isEqualTo(300000);
    assertThat(defaultJobExecutor.getWaitTimeInMillis()).isEqualTo(5000);
    assertThat(defaultJobExecutor.getMaxJobsPerAcquisition()).isEqualTo(3);

    // ServiceName: 'org.operaton.bpm.platform.job-executor.job-executor-tp'
    ServiceController<?> managedQueueExecutorServiceController = container.getService(ServiceNames.forManagedThreadPool(SubsystemAttributeDefinitons.DEFAULT_JOB_EXECUTOR_THREADPOOL_NAME));
    assertThat(managedQueueExecutorServiceController).isNotNull();
    Object managedQueueExecutorServiceObject = managedQueueExecutorServiceController.getValue();
    assertThat(managedQueueExecutorServiceObject).isNotNull();
    assertThat(managedQueueExecutorServiceObject instanceof ManagedQueueExecutorService).isTrue();
    ManagedQueueExecutorService managedQueueExecutorService = (ManagedQueueExecutorService) managedQueueExecutorServiceObject;
    assertThat(managedQueueExecutorService.getCoreThreads())
            .as("Number of core threads is wrong")
            .isEqualTo(SubsystemAttributeDefinitons.DEFAULT_CORE_THREADS);
    assertThat(managedQueueExecutorService.getMaxThreads())
            .as("Number of max threads is wrong")
            .isEqualTo(SubsystemAttributeDefinitons.DEFAULT_MAX_THREADS);
    assertThat(TimeUnit.NANOSECONDS.toSeconds(managedQueueExecutorService.getKeepAlive())).isEqualTo(SubsystemAttributeDefinitons.DEFAULT_KEEPALIVE_TIME);
    assertThat(managedQueueExecutorService.isBlocking()).isEqualTo(false);
    assertThat(managedQueueExecutorService.isAllowCoreTimeout()).isEqualTo(SubsystemAttributeDefinitons.DEFAULT_ALLOW_CORE_TIMEOUT);

    ServiceController<?> threadFactoryService = container.getService(ServiceNames.forThreadFactoryService(SubsystemAttributeDefinitons.DEFAULT_JOB_EXECUTOR_THREADPOOL_NAME));
    assertThat(threadFactoryService).isNotNull();
    assertThat(threadFactoryService.getValue() instanceof ThreadFactory).isTrue();

    // "anders" job acquisition /////////////////////////////////////////////////////////
    ServiceController<?> andersJobAcquisitionService = container.getService(ServiceNames.forMscRuntimeContainerJobExecutorService("anders"));
    assertThat(andersJobAcquisitionService).withFailMessage("platform job acquisition service 'anders' should be installed").isNotNull();

    value = andersJobAcquisitionService.getValue();
    assertThat(value).isNotNull();
    assertThat(value instanceof JobExecutor).isTrue();

    JobExecutor andersJobExecutor = (JobExecutor) value;
    assertThat(andersJobExecutor.getLockTimeInMillis()).isEqualTo(600000);
    assertThat(andersJobExecutor.getWaitTimeInMillis()).isEqualTo(10000);
    assertThat(andersJobExecutor.getMaxJobsPerAcquisition()).isEqualTo(5);

    // "mixed" job acquisition /////////////////////////////////////////////////////////
    ServiceController<?> mixedJobAcquisitionService = container.getService(ServiceNames.forMscRuntimeContainerJobExecutorService("mixed"));
    assertThat(mixedJobAcquisitionService).withFailMessage("platform job acquisition service 'mixed' should be installed").isNotNull();

    value = mixedJobAcquisitionService.getValue();
    assertThat(value).isNotNull();
    assertThat(value instanceof JobExecutor).isTrue();

    JobExecutor mixedJobExecutor = (JobExecutor) value;
    assertThat(mixedJobExecutor.getLockTimeInMillis()).isEqualTo(500000);
    // default values
    assertThat(mixedJobExecutor.getWaitTimeInMillis()).isEqualTo(5000);
    assertThat(mixedJobExecutor.getMaxJobsPerAcquisition()).isEqualTo(3);

  }

  @Test
  public void testJobAcquisitionStrategyOptional() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_JOB_EXECUTOR_WITHOUT_ACQUISITION_STRATEGY);
    KernelServices services = createKernelServicesBuilder(null)
        .setSubsystemXml(subsystemXml)
        .build();
    ServiceContainer container = services.getContainer();

    commonSubsystemServicesAreInstalled(container);
  }

  @Test
  public void testParseSubsystemXmlWithEnginesAndJobExecutor() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_PROCESS_ENGINES_AND_JOB_EXECUTOR);

    List<ModelNode> operations = parse(subsystemXml);
    System.out.println(operations);
    assertThat(operations.size()).isEqualTo(6);
  }

  @Test
  public void testInstallSubsystemXmlWithEnginesAndJobExecutor() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_PROCESS_ENGINES_AND_JOB_EXECUTOR);

    KernelServices services = createKernelServicesBuilder(null)
        .setSubsystemXml(subsystemXml)
        .build();
    ServiceContainer container = services.getContainer();

    commonSubsystemServicesAreInstalled(container);
    assertThat(container.getService(ServiceNames.forManagedProcessEngine("__default"))).withFailMessage("process engine controller for engine __default is installed ").isNotNull();
    assertThat(container.getService(ServiceNames.forManagedProcessEngine("__test"))).withFailMessage("process engine controller for engine __test is installed ").isNotNull();

    String persistedSubsystemXml = services.getPersistedSubsystemXml();
    compareXml(null, subsystemXml, persistedSubsystemXml);
  }

  @Test
  public void testParseAndMarshalModelWithAllAvailableOptions() throws Exception {
    parseAndMarshalSubsystemModelFromFile(SUBSYSTEM_WITH_ALL_OPTIONS);
  }

  @Test
  public void testParseAndMarshalModelWithRequiredOptionsOnly() throws Exception {
    parseAndMarshalSubsystemModelFromFile(SUBSYSTEM_WITH_REQUIRED_OPTIONS);
  }

  @Test
  public void testParseAndMarshalModelWithAllAvailableOptionsWithExpressions() throws Exception {
    System.getProperties().putAll(EXPRESSION_PROPERTIES);
    try {
      parseAndMarshalSubsystemModelFromFile(SUBSYSTEM_WITH_ALL_OPTIONS_WITH_EXPRESSIONS);
    } finally {
      for (String key : EXPRESSION_PROPERTIES.keySet()) {
        System.clearProperty(key);
      }
    }
  }

  @Test
  public void testParseSubsystemXmlWithAllOptionsWithExpressions() throws Exception {
    String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_ALL_OPTIONS_WITH_EXPRESSIONS);

    List<ModelNode> operations = parse(subsystemXml);

    assertThat(operations.size()).isEqualTo(4);
    // all elements with expression allowed should be an expression now
    assertExpressionType(operations.get(1), "default", "datasource", "history-level", "configuration");
    assertExpressionType(operations.get(1).get("properties"), "job-acquisition-name");
    assertExpressionType(operations.get(1).get("plugins").get(0), "class");
    assertExpressionType(operations.get(1).get("plugins").get(0).get("properties"), "test", "number", "bool");
    assertExpressionType(operations.get(2), "thread-pool-name", "core-threads", "max-threads", "queue-length", "keepalive-time", "allow-core-timeout");
    assertExpressionType(operations.get(3), "acquisition-strategy");
    assertExpressionType(operations.get(3).get("properties"), "lockTimeInMillis", "waitTimeInMillis", "maxJobsPerAcquisition");
    // all other elements should be string still
    assertStringType(operations.get(1), "name");// process-engine name
    assertStringType(operations.get(3), "name");// job-acquisition name
  }

  @Test
  public void testInstallSubsystemXmlWithAllOptionsWithExpressions() throws Exception {
    System.getProperties().putAll(EXPRESSION_PROPERTIES);
    try {
      String subsystemXml = FileUtils.readFile(SUBSYSTEM_WITH_ALL_OPTIONS_WITH_EXPRESSIONS);
      KernelServices services = createKernelServicesBuilder(null)
          .setSubsystemXml(subsystemXml)
          .build();
      ServiceContainer container = services.getContainer();

      assertThat(container.getRequiredService(PLATFORM_SERVICE_NAME)).withFailMessage("platform service should be installed").isNotNull();
      assertThat(container.getRequiredService(PROCESS_ENGINE_SERVICE_BINDING_SERVICE_NAME)).withFailMessage("process engine service should be bound in JNDI").isNotNull();

      ServiceController<?> defaultEngineService = container.getService(ServiceNames.forManagedProcessEngine("__test"));

      assertThat(defaultEngineService).withFailMessage("process engine controller for engine __test is installed ").isNotNull();

      ManagedProcessEngineMetadata metadata = ((MscManagedProcessEngineController) defaultEngineService.getService()).getProcessEngineMetadata();
      Map<String, String> configurationProperties = metadata.getConfigurationProperties();
      assertThat(configurationProperties.get("job-acquisition-name")).isEqualTo("default");

      Map<String, String> foxLegacyProperties = metadata.getFoxLegacyProperties();
      assertThat(foxLegacyProperties.isEmpty()).isTrue();

      assertThat(container.getRequiredService(ServiceNames.forManagedProcessEngine("__test"))).withFailMessage("process engine controller for engine __test is installed ").isNotNull();

      // check we have parsed the plugin configurations
      List<ProcessEnginePluginXml> pluginConfigurations = metadata.getPluginConfigurations();

      assertThat(pluginConfigurations.size()).isEqualTo(1);

      ProcessEnginePluginXml processEnginePluginXml = pluginConfigurations.get(0);
      assertThat(processEnginePluginXml.getPluginClass()).isEqualTo("org.operaton.bpm.identity.impl.ldap.plugin.LdapIdentityProviderPlugin");
      Map<String, String> processEnginePluginXmlProperties = processEnginePluginXml.getProperties();
      assertThat(processEnginePluginXmlProperties.get("test")).isEqualTo("abc");
      assertThat(processEnginePluginXmlProperties.get("number")).isEqualTo("123");
      assertThat(processEnginePluginXmlProperties.get("bool")).isEqualTo("true");
    } finally {
      for (String key : EXPRESSION_PROPERTIES.keySet()) {
        System.clearProperty(key);
      }
    }
  }

  // HELPERS

  /**
   *  Parses and marshall the given subsystemXmlFile into one controller, then reads the model into second one.
   *  Compares the models afterwards.
   * @param subsystemXmlFile the name of the subsystem xml file
   * @throws Exception
   */
  protected void parseAndMarshalSubsystemModelFromFile(String subsystemXmlFile) throws Exception {
    String subsystemXml = FileUtils.readFile(subsystemXmlFile);
    // Parse the subsystem xml and install into the first controller
    KernelServices servicesA = createKernelServicesBuilder(null).setSubsystemXml(subsystemXml).build();
    // Get the model and the persisted xml from the first controller
    ModelNode modelA = servicesA.readWholeModel();
    String marshalled = servicesA.getPersistedSubsystemXml();

    // Make sure the xml is the same
    compareXml(null, subsystemXml, marshalled);

    // Install the persisted xml from the first controller into a second controller
    KernelServices servicesB = createKernelServicesBuilder(null).setSubsystemXml(marshalled).build();
    ModelNode modelB = servicesB.readWholeModel();

    // Make sure the models from the two controllers are identical
    compare(modelA, modelB);
  }

  protected void commonSubsystemServicesAreInstalled(ServiceContainer container) {
    assertThat(container.getService(PLATFORM_SERVICE_NAME)).withFailMessage("platform service should be installed").isNotNull();
    assertThat(container.getService(PROCESS_ENGINE_SERVICE_BINDING_SERVICE_NAME)).withFailMessage("process engine service should be bound in JNDI").isNotNull();
    assertThat(container.getService(PLATFORM_JOBEXECUTOR_SERVICE_NAME)).withFailMessage("platform jobexecutor service should be installed").isNotNull();
    assertThat(container.getService(PLATFORM_JOBEXECUTOR_MANAGED_THREAD_POOL_SERVICE_NAME)).withFailMessage("platform jobexecutor managed threadpool service should be installed").isNotNull();
    assertThat(container.getService(PLATFORM_BPM_PLATFORM_PLUGINS_SERVICE_NAME)).withFailMessage("bpm platform plugins service should be installed").isNotNull();
  }

  protected static Comparator<PathAddress> getSubsystemRemoveOrderComparator() {
    final List<PathAddress> REMOVE_ORDER_OF_SUBSYSTEM = Arrays.asList(
        PathAddress.pathAddress(BpmPlatformExtension.SUBSYSTEM_PATH),
        PathAddress.pathAddress(BpmPlatformExtension.JOB_EXECUTOR_PATH),
        PathAddress.pathAddress(BpmPlatformExtension.JOB_ACQUISTIONS_PATH),
        PathAddress.pathAddress(BpmPlatformExtension.PROCESS_ENGINES_PATH)
    );

    return new Comparator<PathAddress>() {
      protected PathAddress normalizePathAddress(PathAddress pathAddress) {
        if (pathAddress.size() == 1 && pathAddress.equals(REMOVE_ORDER_OF_SUBSYSTEM.get(0))) {
          return pathAddress;
        } else {
          // strip subsystem parentAddress
          return pathAddress.subAddress(1, pathAddress.size());
        }
      }

      protected int getOrder(PathAddress pathAddress) {
        for (PathAddress orderedAddress : REMOVE_ORDER_OF_SUBSYSTEM) {
          if (orderedAddress.getLastElement().getKey().equals(pathAddress.getLastElement().getKey())) {
            return REMOVE_ORDER_OF_SUBSYSTEM.indexOf(orderedAddress);
          }
        }
        throw new IllegalArgumentException("Unable to find a match for path address: " + pathAddress);
      }

      @Override
      public int compare(PathAddress o1, PathAddress o2) {
        int orderO1 = getOrder(normalizePathAddress(o1));
        int orderO2 = getOrder(normalizePathAddress(o2));

        if (orderO1 < orderO2) {
          return -1;
        } else if (orderO1 > orderO2) {
          return 1;
        } else {
          return 0;
        }
      }
    };
  }

  private void assertExpressionType(ModelNode operation, String... elements) {
    assertModelType(ModelType.EXPRESSION, operation, elements);
  }

  private void assertStringType(ModelNode operation, String... elements) {
    assertModelType(ModelType.STRING, operation, elements);
  }

  private void assertModelType(ModelType type, ModelNode operation, String... elements) {
    for (String element : elements) {
      assertThat(operation.get(element).getType()).isEqualTo(type);
    }
  }
}
