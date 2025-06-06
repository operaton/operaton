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
package org.operaton.bpm.integrationtest.jobexecutor.beans;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.cdi.impl.ManagedJobExecutor;

@Startup
@Singleton
@SuppressWarnings("unused")
public class ManagedJobExecutorBean {

  @Resource
  private ManagedExecutorService managedExecutorService;

  protected ProcessEngine processEngine;
  protected ManagedJobExecutor jobExecutor;

  @PostConstruct
  public void startEngine() {
    // Using fully-qualified class name instead of import statement to allow for automatic Jakarta transformation
    org.operaton.bpm.engine.impl.cfg.JakartaTransactionProcessEngineConfiguration processEngineConfiguration =
        new org.operaton.bpm.engine.impl.cfg.JakartaTransactionProcessEngineConfiguration();
    processEngineConfiguration.setDatabaseSchemaUpdate("true")
      .setHistory("auto")
      .setDbMetricsReporterActivate(false)
      .setDataSourceJndiName("java:jboss/datasources/ProcessEngine");
    processEngineConfiguration.setTransactionManagerJndiName("java:/TransactionManager");
    jobExecutor = new ManagedJobExecutor(managedExecutorService);
    processEngine = processEngineConfiguration
        .setJobExecutor(jobExecutor)
        .buildProcessEngine();
  }

  @PreDestroy
  public void stopEngine() {
    processEngine.close();
    jobExecutor.shutdown();
  }

  public ProcessEngine getProcessEngine() {
    return processEngine;
  }
}
