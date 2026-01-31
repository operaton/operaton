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
package org.operaton.bpm.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.operaton.bpm.engine.impl.ProcessEngineInfoImpl;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.commons.utils.CollectionUtil;

/**
 * Helper for initializing and closing process engines in server environments.
 * <br>
 * All created {@link ProcessEngine}s will be registered with this class. <br>
 * The activiti-webapp-init webapp will call the {@link #init()} method when the
 * webapp is deployed and it will call the {@link #destroy()} method when the
 * webapp is destroyed, using a context-listener
 * (<code>org.operaton.bpm.engine.test.impl.servlet.listener.ProcessEnginesServletContextListener</code>).
 * That way, all applications can just use the {@link #getProcessEngines()} to
 * obtain pre-initialized and cached process engines. <br>
 * <br>
 * Please note that there is <b>no lazy initialization</b> of process engines,
 * so make sure the context-listener is configured or {@link ProcessEngine}s are
 * already created so they were registered on this class.<br>
 * <br>
 * The {@link #init()} method will try to build one {@link ProcessEngine} for
 * each operaton.cfg.xml file found on the classpath. If you have more than one,
 * make sure you specify different process.engine.name values.
 *
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public final class ProcessEngines {

  private static final ProcessEngineLogger LOG = ProcessEngineLogger.INSTANCE;

  public static final String NAME_DEFAULT = "default";

  private static boolean isInitialized;
  private static Map<String, ProcessEngine> processEngines = new ConcurrentHashMap<>();
  private static final Map<String, ProcessEngineInfo> PROCESS_ENGINE_INFOS_BY_NAME = new ConcurrentHashMap<>();
  private static final Map<String, ProcessEngineInfo> PROCESS_ENGINE_INFOS_BY_RESOURCE_URL = new ConcurrentHashMap<>();
  private static final List<ProcessEngineInfo> PROCESS_ENGINE_INFOS = new CopyOnWriteArrayList<>();

  private ProcessEngines() {
    // prevent instantiation
  }

  public static synchronized void init() {
    init(true);
  }

  /**
   * Initializes all process engines that can be found on the classpath for
   * resources <code>operaton.cfg.xml</code> (plain Activiti style
   * configuration) and for resources <code>activiti-context.xml</code> (Spring
   * style configuration).
   */
  public static synchronized void init(boolean forceCreate) {
    if (isInitialized) {
      LOG.processEngineAlreadyInitialized();
    }

    if (processEngines == null) {
      processEngines = new ConcurrentHashMap<>();
    }

    ClassLoader classLoader = ReflectUtil.getClassLoader();
    Set<URI> configResources = getResources(classLoader, "operaton.cfg.xml", "activiti.cfg.xml", forceCreate);
    if (configResources.isEmpty() && !forceCreate) {
      return;
    }

    for (URI resource : configResources) {
      initProcessEngineFromResource(resource);
    }

    Set<URI> springResources = getResources(classLoader, "activiti-context.xml", null, forceCreate);
    if (springResources.isEmpty() && !forceCreate) {
      return;
    }

    for (URI resource : springResources) {
      initProcessEngineFromSpringResource(resource);
    }

    isInitialized = true;
  }

  // Remove duplicated configuration URL's using set. Some classloaders may
  // return identical URL's twice, causing duplicate startups
  private static Set<URI> getResources(ClassLoader classLoader, String resourceName,
      String fallbackResourceName, boolean forceCreate) {
    try {
      return CollectionUtil.toSet(classLoader.getResources(resourceName), ReflectUtil::urlToURI);
    } catch (IOException e) {
      if (fallbackResourceName != null) {
        try {
          return CollectionUtil.toSet(classLoader.getResources(fallbackResourceName), ReflectUtil::urlToURI);
        } catch (IOException ex) {
          return handleGetResourcesException(resourceName, fallbackResourceName, forceCreate, ex);
        }
      }
      return handleGetResourcesException(resourceName, fallbackResourceName, forceCreate, e);
    }
  }

  private static Set<URI> handleGetResourcesException(String resourceName, String fallbackResourceName,
      boolean forceCreate, IOException e) {
    if (forceCreate) {
      String message = "problem retrieving " + resourceName
          + (fallbackResourceName != null ? " and " + fallbackResourceName : "") + " resources on the classpath: "
          + System.getProperty("java.class.path");
      throw new ProcessEngineException(message, e);
    }
    return Collections.emptySet();
  }

  /**
   * @deprecated Planned for removal
   */
  @Deprecated(since = "1.1", forRemoval = true)
  @SuppressWarnings("java:S1133")
  protected static void initProcessEngineFromSpringResource(URL resource) {
    try {
      initProcessEngineFromSpringResource(resource.toURI());
    } catch (URISyntaxException e) {
      throw new ProcessEngineException(
        "couldn't initialize process engine from spring configuration resource %s: %s".formatted(resource.toString(),
          e.getMessage()),
        e);
    }
  }

  private static void initProcessEngineFromSpringResource(URI resource) {
    try {
      Class<?> springConfigurationHelperClass = ReflectUtil.loadClass(
          "org.operaton.bpm.engine.spring.SpringConfigurationHelper");
      Method method = springConfigurationHelperClass.getMethod("buildProcessEngine", URI.class);
      ProcessEngine processEngine = (ProcessEngine) method.invoke(null, resource);

      String processEngineName = processEngine.getName();
      ProcessEngineInfo processEngineInfo = new ProcessEngineInfoImpl(processEngineName, resource.toString(), null);
      PROCESS_ENGINE_INFOS_BY_NAME.put(processEngineName, processEngineInfo);
      PROCESS_ENGINE_INFOS_BY_RESOURCE_URL.put(resource.toString(), processEngineInfo);

    } catch (Exception e) {
      throw new ProcessEngineException(
          "couldn't initialize process engine from spring configuration resource %s: %s".formatted(resource.toString(),
              e.getMessage()),
          e);
    }
  }

  /**
   * Registers the given process engine. No {@link ProcessEngineInfo} will be
   * available for this process engine. An engine that is registered will be
   * closed when the {@link ProcessEngines#destroy()} is called.
   */
  public static void registerProcessEngine(ProcessEngine processEngine) {
    processEngines.put(processEngine.getName(), processEngine);
  }

  /**
   * Unregisters the given process engine.
   */
  public static void unregister(ProcessEngine processEngine) {
    processEngines.remove(processEngine.getName());
  }

  /**
   * Check if the given process engine with that name is already registered.
   */
  public static boolean isRegisteredProcessEngine(String processEngineName) {
    return processEngines.containsKey(processEngineName);
  }

  private static ProcessEngineInfo initProcessEngineFromResource(URI resourceUrl) {
    ProcessEngineInfo processEngineInfo = PROCESS_ENGINE_INFOS_BY_RESOURCE_URL.get(resourceUrl.toString());
    // if there is an existing process engine info
    if (processEngineInfo != null) {
      // remove that process engine from the member fields
      PROCESS_ENGINE_INFOS.remove(processEngineInfo);
      if (processEngineInfo.getException() == null) {
        String processEngineName = processEngineInfo.getName();
        processEngines.remove(processEngineName);
        PROCESS_ENGINE_INFOS_BY_NAME.remove(processEngineName);
      }
      PROCESS_ENGINE_INFOS_BY_RESOURCE_URL.remove(processEngineInfo.getResourceUrl());
    }

    String resourceUrlString = resourceUrl.toString();
    try {
      LOG.initializingProcessEngineForResource(resourceUrl);
      ProcessEngine processEngine = buildProcessEngine(resourceUrl);
      String processEngineName = processEngine.getName();
      LOG.initializingProcessEngine(processEngine.getName());
      processEngineInfo = new ProcessEngineInfoImpl(processEngineName, resourceUrlString, null);
      processEngines.put(processEngineName, processEngine);
      PROCESS_ENGINE_INFOS_BY_NAME.put(processEngineName, processEngineInfo);
    } catch (RuntimeException e) {
      LOG.exceptionWhileInitializingProcessengine(e);
      processEngineInfo = new ProcessEngineInfoImpl(null, resourceUrlString, getExceptionString(e));
    }
    PROCESS_ENGINE_INFOS_BY_RESOURCE_URL.put(resourceUrlString, processEngineInfo);
    PROCESS_ENGINE_INFOS.add(processEngineInfo);
    return processEngineInfo;
  }

  private static String getExceptionString(Throwable e) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    e.printStackTrace(pw);
    return sw.toString();
  }

  private static ProcessEngine buildProcessEngine(URI resource) {
    InputStream inputStream = null;
    try {
      inputStream = resource.toURL().openStream();
      ProcessEngineConfiguration processEngineConfiguration = ProcessEngineConfiguration
          .createProcessEngineConfigurationFromInputStream(inputStream);
      return processEngineConfiguration.buildProcessEngine();

    } catch (IOException e) {
      throw new ProcessEngineException("couldn't open resource stream: %s".formatted(e.getMessage()), e);
    } finally {
      IoUtil.closeSilently(inputStream);
    }
  }

  /**
   * Get initialization results.
   */
  public static List<ProcessEngineInfo> getProcessEngineInfos() {
    return PROCESS_ENGINE_INFOS;
  }

  /**
   * Get initialization results. Only info will be available for process engines
   * which were added in the {@link ProcessEngines#init()}. No
   * {@link ProcessEngineInfo} is available for engines which were registered
   * programmatically.
   */
  public static ProcessEngineInfo getProcessEngineInfo(String processEngineName) {
    return PROCESS_ENGINE_INFOS_BY_NAME.get(processEngineName);
  }

  public static ProcessEngine getDefaultProcessEngine() {
    return getDefaultProcessEngine(true);
  }

  public static ProcessEngine getDefaultProcessEngine(boolean forceCreate) {
    return getProcessEngine(NAME_DEFAULT, forceCreate);
  }

  public static ProcessEngine getProcessEngine(String processEngineName) {
    return getProcessEngine(processEngineName, true);
  }

  /**
   * obtain a process engine by name.
   *
   * @param processEngineName is the name of the process engine or null for the
   *                          default process engine.
   */
  public static ProcessEngine getProcessEngine(String processEngineName, boolean forceCreate) {
    if (!isInitialized) {
      init(forceCreate);
    }
    return processEngines.get(processEngineName);
  }

  /**
   * retries to initialize a process engine that previously failed.
   */
  public static ProcessEngineInfo retry(String resourceUrl) {
    try {
      return initProcessEngineFromResource(new URI(resourceUrl));
    } catch (URISyntaxException e) {
      throw new ProcessEngineException("invalid uri: %s".formatted(resourceUrl), e);
    }
  }

  /**
   * provides access to process engine to application clients in a managed server environment.
   */
  public static Map<String, ProcessEngine> getProcessEngines() {
    return processEngines;
  }

  /**
   * closes all process engines. This method should be called when the server shuts down.
   */
  public static synchronized void destroy() {
    if (isInitialized) {
      Map<String, ProcessEngine> engines = new HashMap<>(processEngines);
      processEngines = new ConcurrentHashMap<>();

      for (var processEngine : engines.values()) {
        try {
          processEngine.close();
        } catch (Exception e) {
          LOG.exceptionWhileClosingProcessEngine(
              processEngine.getName() == null
                  ? "the default process engine"
                  : "process engine " + processEngine.getName(),
              e);
        }
      }

      PROCESS_ENGINE_INFOS_BY_NAME.clear();
      PROCESS_ENGINE_INFOS_BY_RESOURCE_URL.clear();
      PROCESS_ENGINE_INFOS.clear();

      isInitialized = false;
    }
  }
}
