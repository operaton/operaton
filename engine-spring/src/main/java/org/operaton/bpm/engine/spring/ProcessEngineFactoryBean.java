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

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Dave Syer
 * @author Christian Stettler
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class ProcessEngineFactoryBean implements FactoryBean<ProcessEngine>, DisposableBean, ApplicationContextAware {

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected ApplicationContext applicationContext;
  protected ProcessEngineImpl processEngine;

  @Override
  public void destroy() throws Exception {
    if (processEngine != null) {
      processEngine.close();
    }
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public ProcessEngine getObject() throws Exception {
    if (processEngine == null) {
      initializeExpressionManager();
      initializeTransactionExternallyManaged();

      processEngine = (ProcessEngineImpl) processEngineConfiguration.buildProcessEngine();
    }

    return processEngine;
  }

  protected void initializeExpressionManager() {
    if (processEngineConfiguration.getExpressionManager() == null && applicationContext != null) {
      processEngineConfiguration.setExpressionManager(
          new SpringExpressionManager(applicationContext, processEngineConfiguration.getBeans()));
    }
  }

  protected void initializeTransactionExternallyManaged() {
    if (processEngineConfiguration instanceof SpringProcessEngineConfiguration engineConfiguration &&
            engineConfiguration.getTransactionManager() != null) { // remark: any config can be injected, so we cannot have SpringConfiguration as member
      processEngineConfiguration.setTransactionsExternallyManaged(true);
    }
  }

  @Override
  public Class<ProcessEngine> getObjectType() {
    return ProcessEngine.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  // getters and setters //////////////////////////////////////////////////////

  public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
    return processEngineConfiguration;
  }


  public void setProcessEngineConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
    this.processEngineConfiguration = processEngineConfiguration;
  }
}
