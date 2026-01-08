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
package org.operaton.bpm.engine.spring;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipInputStream;

import javax.sql.DataSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.operaton.bpm.engine.impl.interceptor.*;
import org.operaton.bpm.engine.repository.DeploymentBuilder;

import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRED;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

/**
 * @author Tom Baeyens
 * @author David Syer
 * @author Joram Barrez
 * @author Daniel Meyer
 */
public class SpringTransactionsProcessEngineConfiguration extends ProcessEngineConfigurationImpl {

  protected PlatformTransactionManager transactionManager;
  protected String deploymentName = "SpringAutoDeployment";
  protected Resource[] deploymentResources = new Resource[0];
  protected String deploymentTenantId;
  protected boolean deployChangedOnly;

  public SpringTransactionsProcessEngineConfiguration() {
    transactionsExternallyManaged = true;
  }

  @Override
  public ProcessEngine buildProcessEngine() {
    ProcessEngine processEngine = super.buildProcessEngine();
    autoDeployResources(processEngine);
    return processEngine;
  }

  protected Collection<CommandInterceptor> getDefaultCommandInterceptorsTxRequired() {
    if (transactionManager==null) {
      throw new ProcessEngineException("transactionManager is required property for SpringProcessEngineConfiguration, use "+StandaloneProcessEngineConfiguration.class.getName()+" otherwise");
    }

    List<CommandInterceptor> defaultCommandInterceptorsTxRequired = new ArrayList<>();
    if (!isDisableExceptionCode()) {
      defaultCommandInterceptorsTxRequired.add(getExceptionCodeInterceptor());
    }
    defaultCommandInterceptorsTxRequired.add(new LogInterceptor());
    defaultCommandInterceptorsTxRequired.add(new CommandCounterInterceptor(this));
    defaultCommandInterceptorsTxRequired.add(new ProcessApplicationContextInterceptor(this));
    defaultCommandInterceptorsTxRequired.add(new SpringTransactionInterceptor(transactionManager, PROPAGATION_REQUIRED, this));
    CommandContextInterceptor commandContextInterceptor = new CommandContextInterceptor(commandContextFactory, this);
    defaultCommandInterceptorsTxRequired.add(commandContextInterceptor);
    return defaultCommandInterceptorsTxRequired;
  }

  protected Collection<CommandInterceptor> getDefaultCommandInterceptorsTxRequiresNew() {
    List<CommandInterceptor> defaultCommandInterceptorsTxRequiresNew = new ArrayList<>();
    if (!isDisableExceptionCode()) {
      defaultCommandInterceptorsTxRequiresNew.add(getExceptionCodeInterceptor());
    }
    defaultCommandInterceptorsTxRequiresNew.add(new LogInterceptor());
    defaultCommandInterceptorsTxRequiresNew.add(new CommandCounterInterceptor(this));
    defaultCommandInterceptorsTxRequiresNew.add(new ProcessApplicationContextInterceptor(this));
    defaultCommandInterceptorsTxRequiresNew.add(new SpringTransactionInterceptor(transactionManager, PROPAGATION_REQUIRES_NEW, this));
    CommandContextInterceptor commandContextInterceptor = new CommandContextInterceptor(commandContextFactory, this, true);
    defaultCommandInterceptorsTxRequiresNew.add(commandContextInterceptor);
    return defaultCommandInterceptorsTxRequiresNew;
  }

  @Override
  protected void initTransactionContextFactory() {
    if(transactionContextFactory == null && transactionManager != null) {
      transactionContextFactory = new SpringTransactionContextFactory(transactionManager);
    }
  }

  protected void autoDeployResources(ProcessEngine processEngine) {
    if (deploymentResources!=null && deploymentResources.length>0) {
      RepositoryService repositoryService = processEngine.getRepositoryService();

      DeploymentBuilder deploymentBuilder = repositoryService
        .createDeployment()
        .enableDuplicateFiltering(deployChangedOnly)
        .name(deploymentName)
        .tenantId(deploymentTenantId);

      for (Resource resource : deploymentResources) {
        String resourceName = null;

        if (resource instanceof ContextResource contextResource) {
          resourceName = contextResource.getPathWithinContext();

        } else if (resource instanceof ByteArrayResource) {
          resourceName = resource.getDescription();

        } else {
          resourceName = getFileResourceName(resource);
        }

        try {
          if ( resourceName.endsWith(".bar")
               || resourceName.endsWith(".zip")
               || resourceName.endsWith(".jar") ) {
            deploymentBuilder.addZipInputStream(new ZipInputStream(resource.getInputStream()));
          } else {
            deploymentBuilder.addInputStream(resourceName, resource.getInputStream());
          }
        } catch (IOException e) {
          throw new ProcessEngineException("couldn't auto deploy resource '%s': %s".formatted(resource, e.getMessage()), e);
        }
      }

      deploymentBuilder.deploy();
    }
  }

  protected String getFileResourceName(Resource resource) {
    try {
      return resource.getFile().getAbsolutePath();
    } catch (IOException e) {
      return resource.getFilename();
    }
  }

  @Override
  public ProcessEngineConfigurationImpl setDataSource(DataSource dataSource) {
    if(dataSource instanceof TransactionAwareDataSourceProxy) {
      return super.setDataSource(dataSource);
    } else {
      // Wrap datasource in Transaction-aware proxy
      DataSource proxiedDataSource = new TransactionAwareDataSourceProxy(dataSource);
      return super.setDataSource(proxiedDataSource);
    }
  }

  public PlatformTransactionManager getTransactionManager() {
    return transactionManager;
  }

  public void setTransactionManager(PlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  public String getDeploymentName() {
    return deploymentName;
  }

  public void setDeploymentName(String deploymentName) {
    this.deploymentName = deploymentName;
  }

  public Resource[] getDeploymentResources() {
    return deploymentResources;
  }

  public void setDeploymentResources(Resource[] deploymentResources) {
    this.deploymentResources = deploymentResources;
  }

  public String getDeploymentTenantId() {
    return deploymentTenantId;
  }

  public void setDeploymentTenantId(String deploymentTenantId) {
    this.deploymentTenantId = deploymentTenantId;
  }

  public boolean isDeployChangedOnly() {
    return deployChangedOnly;
  }

  public void setDeployChangedOnly(boolean deployChangedOnly) {
    this.deployChangedOnly = deployChangedOnly;
  }

}
