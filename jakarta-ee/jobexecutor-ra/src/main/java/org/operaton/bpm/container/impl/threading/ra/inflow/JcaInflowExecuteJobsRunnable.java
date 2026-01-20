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
package org.operaton.bpm.container.impl.threading.ra.inflow;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.UnavailableException;
import jakarta.resource.spi.endpoint.MessageEndpoint;

import org.operaton.bpm.container.impl.threading.ra.JcaExecutorServiceConnector;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.jobexecutor.ExecuteJobsRunnable;
import org.operaton.bpm.engine.impl.jobexecutor.JobFailureCollector;


/**
 * JCA inflow implementation of the {@link ExecuteJobsRunnable}
 * @author Daniel Meyer
 *
 */
public class JcaInflowExecuteJobsRunnable extends ExecuteJobsRunnable {

  private final Logger log = Logger.getLogger(JcaInflowExecuteJobsRunnable.class.getName());

  protected final JcaExecutorServiceConnector ra;

  protected static AtomicReference<Method> method = new AtomicReference<>();

  public JcaInflowExecuteJobsRunnable(List<String> jobIds, ProcessEngineImpl processEngine, JcaExecutorServiceConnector connector) {
    super(jobIds, processEngine);
    this.ra = connector;
    if (method.get() == null) {
      loadMethod();
    }
  }

  @Override
  protected void executeJob(String nextJobId, CommandExecutor commandExecutor, JobFailureCollector jobFailureCollector) {
    JobExecutionHandlerActivation jobHandlerActivation = ra.getJobHandlerActivation();
    if (jobHandlerActivation == null) {
      // TODO: stop acquisition / only activate acquisition if MDB active?
      log.warning("Cannot execute acquired job, no JobExecutionHandler MDB deployed.");
      return;
    }
    MessageEndpoint endpoint = null;
    try {
      endpoint = jobHandlerActivation.getMessageEndpointFactory().createEndpoint(null);

      beforeDelivery(endpoint);

      performExecute(nextJobId, commandExecutor, jobFailureCollector, (JobExecutionHandler) endpoint);

      afterDelivery(endpoint);
    } catch (UnavailableException e) {
      log.log(Level.SEVERE, e, () -> "UnavailableException while attempting to create messaging endpoint for executing job");
    } finally {
      if (endpoint != null) {
        endpoint.release();
      }
    }
  }

  private void beforeDelivery(MessageEndpoint endpoint) {
    try {
      endpoint.beforeDelivery(method.get());
    } catch (NoSuchMethodException e) {
      log.log(Level.WARNING, e, () -> "NoSuchMethodException while invoking beforeDelivery() on MessageEndpoint '%s'".formatted(endpoint));
    } catch (ResourceException e) {
      log.log(Level.WARNING, e, () -> "ResourceException while invoking beforeDelivery() on MessageEndpoint '%s'".formatted(endpoint));
    }
  }

  private void afterDelivery(MessageEndpoint endpoint) {
    try {
      endpoint.afterDelivery();
    } catch (ResourceException e) {
      log.log(Level.WARNING, e, () -> "ResourceException while invoking afterDelivery() on MessageEndpoint '%s'".formatted(endpoint));
    }
  }

  private void performExecute(String nextJobId, CommandExecutor commandExecutor, JobFailureCollector jobFailureCollector,
      JobExecutionHandler endpoint) {
    try {
      endpoint.executeJob(nextJobId, commandExecutor);
    } catch (Exception e) {
      if (ProcessEngineLogger.shouldLogJobException(processEngine.getProcessEngineConfiguration(), jobFailureCollector.getJob())) {
        log.log(Level.WARNING, e, () -> "Exception while executing job with id '%s'".formatted(nextJobId));
      }
    }
  }

  protected void loadMethod() {
    try {
      method.set(JobExecutionHandler.class.getMethod("executeJob", String.class, CommandExecutor.class));
    } catch (SecurityException e) {
      throw new IllegalStateException("SecurityException while invoking getMethod() on class "+JobExecutionHandler.class, e);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException("NoSuchMethodException while invoking getMethod() on class "+JobExecutionHandler.class, e);
    }
  }

  /**
   * Context class loader switch is not necessary since
   * the loader used for job execution is successor of the engine's
   * @see org.operaton.bpm.engine.impl.jobexecutor.ExecuteJobsRunnable#switchClassLoader()
   *
   * @return the context class loader of the current thread.
   */
  @Override
  protected ClassLoader switchClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }
}
