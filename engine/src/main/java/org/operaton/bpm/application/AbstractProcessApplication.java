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
package org.operaton.bpm.application;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import jakarta.el.BeanELResolver;
import jakarta.el.ELResolver;

import javax.script.ScriptEngine;

import org.operaton.bpm.application.impl.DefaultElResolverLookup;
import org.operaton.bpm.application.impl.ProcessApplicationLogger;
import org.operaton.bpm.application.impl.ProcessApplicationScriptEnvironment;
import org.operaton.bpm.container.RuntimeContainerDelegate;
import org.operaton.bpm.engine.ProcessEngines;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.operaton.bpm.engine.delegate.TaskListener;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.scripting.ExecutableScript;
import org.operaton.bpm.engine.impl.util.ClassLoaderUtil;
import org.operaton.bpm.engine.impl.variable.serializer.VariableSerializers;
import org.operaton.bpm.engine.repository.DeploymentBuilder;


/**
 * @author Daniel Meyer
 */
public abstract class AbstractProcessApplication implements ProcessApplicationInterface {

  private static final ProcessApplicationLogger LOG = ProcessEngineLogger.PROCESS_APPLICATION_LOGGER;

  protected ELResolver processApplicationElResolver;
  protected BeanELResolver processApplicationBeanElResolver;
  protected ProcessApplicationScriptEnvironment processApplicationScriptEnvironment;

  protected VariableSerializers variableSerializers;

  protected boolean isDeployed;

  protected String defaultDeployToEngineName = ProcessEngines.NAME_DEFAULT;

  // deployment /////////////////////////////////////////////////////

  @Override
  public void deploy() {
    if (isDeployed) {
      LOG.alreadyDeployed();
    } else {
      try {
        ProcessApplicationReference reference = getReference();
        Context.setCurrentProcessApplication(reference);

        // deploy the application
        RuntimeContainerDelegate.INSTANCE.get().deployProcessApplication(this);
        isDeployed = true;

      } finally {
        Context.removeCurrentProcessApplication();

      }
    }
  }

  @Override
  public void undeploy() {
    if (!isDeployed) {
      LOG.notDeployed();
    } else {
      // delegate stopping of the process application to the runtime container.
      RuntimeContainerDelegate.INSTANCE.get().undeployProcessApplication(this);
      isDeployed = false;
    }
  }

  @Override
  public void createDeployment(String processArchiveName, DeploymentBuilder deploymentBuilder) {
    // default implementation does nothing
  }

  // Runtime ////////////////////////////////////////////

  @Override
  public String getName() {
    Class<? extends AbstractProcessApplication> processApplicationClass = getClass();
    String name = null;

    ProcessApplication annotation = processApplicationClass.getAnnotation(ProcessApplication.class);
    if (annotation != null) {
      name = annotation.value();

      if (name == null || name.isEmpty()) {
        name = annotation.name();
      }
    }


    if (name == null || name.isEmpty()) {
      name = autodetectProcessApplicationName();
    }

    return name;
  }

  /**
   * Override this method to autodetect an application name in case the
   * {@link ProcessApplication} annotation was used but without parameter.
   */
  protected abstract String autodetectProcessApplicationName();

  @Override
  public <T> T execute(Callable<T> callable) throws ProcessApplicationExecutionException {
    ClassLoader originalClassloader = ClassLoaderUtil.getContextClassloader();
    ClassLoader processApplicationClassloader = getProcessApplicationClassloader();

    try {
      ClassLoaderUtil.setContextClassloader(processApplicationClassloader);

      return callable.call();

    } catch (Exception e) {
      throw LOG.processApplicationExecutionException(e);
    } finally {
      ClassLoaderUtil.setContextClassloader(originalClassloader);
    }
  }

  @Override
  public <T> T execute(Callable<T> callable, InvocationContext invocationContext) throws ProcessApplicationExecutionException {
    // allows to hook into the invocation
    return execute(callable);
  }

  @Override
  public ClassLoader getProcessApplicationClassloader() {
    // the default implementation uses the classloader that loaded
    // the application-provided subclass of this class.
    return ClassLoaderUtil.getClassloader(getClass());
  }

  @Override
  public ProcessApplicationInterface getRawObject() {
    return this;
  }

  @Override
  public Map<String, String> getProperties() {
    return Collections.emptyMap();
  }

  @Override
  public synchronized ELResolver getElResolver() {
    if (processApplicationElResolver == null) {
      processApplicationElResolver = initProcessApplicationElResolver();
    }
    return processApplicationElResolver;

  }

  @Override
  public synchronized BeanELResolver getBeanElResolver() {
    if (processApplicationBeanElResolver == null) {
      processApplicationBeanElResolver = new BeanELResolver();
    }
    return processApplicationBeanElResolver;
  }

  /**
   * <p>Initializes the process application provided ElResolver. This implementation uses the
   * Java SE {@link ServiceLoader} facilities for resolving implementations of {@link ProcessApplicationElResolver}.</p>
   * <p>
   * <p>If you want to provide a custom implementation in your application, place a file named
   * <code>META-INF/org.operaton.bpm.application.ProcessApplicationElResolver</code> inside your application
   * which contains the fully qualified classname of your implementation. Or simply override this method.</p>
   *
   * @return the process application ElResolver.
   */
  protected ELResolver initProcessApplicationElResolver() {

    return DefaultElResolverLookup.lookupResolver(this);

  }

  @Override
  public ExecutionListener getExecutionListener() {
    return null;
  }

  @Override
  public TaskListener getTaskListener() {
    return null;
  }

  /**
   * see {@link ProcessApplicationScriptEnvironment#getScriptEngineForName(String, boolean)}
   */
  public ScriptEngine getScriptEngineForName(String name, boolean cache) {
    return getProcessApplicationScriptEnvironment().getScriptEngineForName(name, cache);
  }

  /**
   * see {@link ProcessApplicationScriptEnvironment#getEnvironmentScripts()}
   */
  public Map<String, List<ExecutableScript>> getEnvironmentScripts() {
    return getProcessApplicationScriptEnvironment().getEnvironmentScripts();
  }

  protected synchronized ProcessApplicationScriptEnvironment getProcessApplicationScriptEnvironment() {
    if (processApplicationScriptEnvironment == null) {
      processApplicationScriptEnvironment = new ProcessApplicationScriptEnvironment(this);
    }
    return processApplicationScriptEnvironment;
  }

  public VariableSerializers getVariableSerializers() {
    return variableSerializers;
  }

  public void setVariableSerializers(VariableSerializers variableSerializers) {
    this.variableSerializers = variableSerializers;
  }

  /**
   * <p>Provides the default Process Engine name to deploy to, if no Process Engine
   * was defined in <code>processes.xml</code>.</p>
   *
   * @return the default deploy-to Process Engine name.
   *         The default value is "default".
   */
  public String getDefaultDeployToEngineName() {
    return defaultDeployToEngineName;
  }

  /**
   * <p>Programmatically set the name of the Process Engine to deploy to if no Process Engine
   * is defined in <code>processes.xml</code>. This allows to circumvent the "default" Process
   * Engine name and set a custom one.</p>
   *
   * @param defaultDeployToEngineName
   */
  protected void setDefaultDeployToEngineName(String defaultDeployToEngineName) {
    this.defaultDeployToEngineName = defaultDeployToEngineName;
  }
}
