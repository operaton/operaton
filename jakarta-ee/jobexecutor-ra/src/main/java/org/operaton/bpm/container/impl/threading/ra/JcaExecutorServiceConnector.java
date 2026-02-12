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
package org.operaton.bpm.container.impl.threading.ra;

import java.io.Serial;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ConfigProperty;
import jakarta.resource.spi.Connector;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.TransactionSupport;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;

import javax.transaction.xa.XAResource;

import org.operaton.bpm.container.impl.threading.ra.commonj.CommonJWorkManagerExecutorService;
import org.operaton.bpm.container.impl.threading.ra.inflow.JobExecutionHandler;
import org.operaton.bpm.container.impl.threading.ra.inflow.JobExecutionHandlerActivation;
import org.operaton.bpm.container.impl.threading.ra.inflow.JobExecutionHandlerActivationSpec;


/**
 * <p>The {@link ResourceAdapter} responsible for bootstrapping the JcaExecutorService</p>
 *
 * @author Daniel Meyer
 */
@Connector(
  reauthenticationSupport = false,
  transactionSupport = TransactionSupport.TransactionSupportLevel.NoTransaction
)
public class JcaExecutorServiceConnector implements ResourceAdapter {

  public static final String ORG_OPERATON_BPM_ENGINE_PROCESS_ENGINE = "org.operaton.bpm.engine.ProcessEngine";

  /**
   * This class must be free of engine classes to make it possible to install
   * the resource adapter without shared libraries. Some deployments scenarios might
   * require that.
   * <p>
   * The wrapper class was introduced to provide more meaning to an otherwise
   * unspecified property.
   * </p>
   */
  public class ExecutorServiceWrapper {
    /**
     * will hold a org.operaton.bpm.container.ExecutorService reference
     */
    protected Object executorService;

    public Object getExecutorService() {
      return executorService;
    }

    private void setExecutorService(Object executorService) {
      this.executorService = executorService;
    }

  }

  protected ExecutorServiceWrapper executorServiceWrapper = new ExecutorServiceWrapper();

  @Serial
  private static final long serialVersionUID = 1L;

  private static final Logger LOG = Logger.getLogger(JcaExecutorServiceConnector.class.getName());

  protected JobExecutionHandlerActivation jobHandlerActivation;

  public JcaExecutorServiceConnector() {
    // no arg-constructor
  }

  // Configuration Properties //////////////////////////////////////////

  @ConfigProperty(
      type = Boolean.class,
      defaultValue = "false",
      description = "If set to 'true', the CommonJ WorkManager is used instead of the Jca Work Manager."
      + "Can only be used on platforms where a CommonJ WorkManager is available (such as IBM & Oracle)"
  )
  protected Boolean isUseCommonJWorkManager = false;


  @ConfigProperty(
      type=String.class,
      defaultValue = "wm/operaton-bpm-workmanager",
      description="Allows specifying the name of a CommonJ WorkManager."
  )
  protected String commonJWorkManagerName = "wm/operaton-bpm-workmanager";


  // RA-Lifecycle ///////////////////////////////////////////////////

  @Override
  public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {

    try {
      Class.forName(ORG_OPERATON_BPM_ENGINE_PROCESS_ENGINE);
    } catch (Exception e) {
      LOG.info("ProcessEngine classes not found in shared libraries. Not initializing operaton Platform JobExecutor Resource Adapter.");
      return;
    }

    // initialize the ExecutorService (CommonJ or JCA, depending on configuration)
    if(isUseCommonJWorkManager == Boolean.TRUE) {
      if(commonJWorkManagerName != null && !commonJWorkManagerName.isEmpty()) {
        executorServiceWrapper.setExecutorService(new CommonJWorkManagerExecutorService(this, commonJWorkManagerName));
      } else {
        throw new JcaConfigException("Resource Adapter configuration property 'isUseCommonJWorkManager' is set to true but 'commonJWorkManagerName' is not provided.");
      }

    } else {
      executorServiceWrapper.setExecutorService(new JcaWorkManagerExecutorService(this, ctx.getWorkManager()));
    }

    LOG.log(Level.INFO, "Operaton executor service started.");
  }

  @Override
  public void stop() {
    try {
      Class.forName(ORG_OPERATON_BPM_ENGINE_PROCESS_ENGINE);
    } catch (Exception e) {
      return;
    }

    LOG.log(Level.INFO, "Operaton executor service stopped.");

  }

  // JobHandler activation / deactivation ///////////////////////////

  @Override
  public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) throws ResourceException {
    if(jobHandlerActivation != null) {
      throw new ResourceException("The Operaton job executor can only service a single MessageEndpoint for job execution. " +
      		"Make sure not to deploy more than one MDB implementing the '"+JobExecutionHandler.class.getName()+"' interface.");
    }
    JobExecutionHandlerActivation activation = new JobExecutionHandlerActivation(this, endpointFactory, (JobExecutionHandlerActivationSpec) spec);
    activation.start();
    jobHandlerActivation = activation;
  }

  @Override
  public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
    try {
      if(jobHandlerActivation != null) {
        jobHandlerActivation.stop();
      }
    } finally {
      jobHandlerActivation = null;
    }
  }

  // unsupported (No TX Support) ////////////////////////////////////////////

  @Override
  public XAResource[] getXAResources(ActivationSpec[] specs) {
    LOG.finest("getXAResources()");
    return new XAResource[0];
  }

  // getters ///////////////////////////////////////////////////////////////

  public ExecutorServiceWrapper getExecutorServiceWrapper() {
    return executorServiceWrapper;
  }

  public JobExecutionHandlerActivation getJobHandlerActivation() {
    return jobHandlerActivation;
  }

  public Boolean getIsUseCommonJWorkManager() {
    return isUseCommonJWorkManager;
  }

  public void setIsUseCommonJWorkManager(Boolean isUseCommonJWorkManager) {
    this.isUseCommonJWorkManager = isUseCommonJWorkManager;
  }

  public String getCommonJWorkManagerName() {
    return commonJWorkManagerName;
  }

  public void setCommonJWorkManagerName(String commonJWorkManagerName) {
    this.commonJWorkManagerName = commonJWorkManagerName;
  }


  // misc //////////////////////////////////////////////////////////////////


  @Override
  public int hashCode() {
    return 17;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (other == this) {
      return true;
    }
    return other instanceof JcaExecutorServiceConnector;
  }

}
