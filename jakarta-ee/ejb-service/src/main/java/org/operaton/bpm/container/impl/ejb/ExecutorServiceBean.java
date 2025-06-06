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
package org.operaton.bpm.container.impl.ejb;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.resource.ResourceException;

import org.operaton.bpm.container.ExecutorService;
import org.operaton.bpm.container.impl.threading.ra.outbound.JcaExecutorServiceConnection;
import org.operaton.bpm.container.impl.threading.ra.outbound.JcaExecutorServiceConnectionFactory;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.impl.ProcessEngineImpl;

/**
 * Bean exposing the JCA implementation of the {@link ExecutorService} as Stateless Bean.
 *
 * @author Daniel Meyer
 *
 */
@Stateless
@Local(ExecutorService.class)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class ExecutorServiceBean implements ExecutorService {

  @Resource(mappedName="eis/JcaExecutorServiceConnectionFactory")
  protected JcaExecutorServiceConnectionFactory executorConnectionFactory;

  protected JcaExecutorServiceConnection executorConnection;

  @PostConstruct
  protected void openConnection() {
    try {
      executorConnection = executorConnectionFactory.getConnection();
    } catch (ResourceException e) {
      throw new ProcessEngineException("Could not open connection to executor service connection factory ", e);
    }
  }

  @PreDestroy
  protected void closeConnection() {
    if(executorConnection != null) {
      executorConnection.closeConnection();
    }
  }

  public boolean schedule(Runnable runnable, boolean isLongRunning) {
    return executorConnection.schedule(runnable, isLongRunning);
  }

  public Runnable getExecuteJobsRunnable(List<String> jobIds, ProcessEngineImpl processEngine) {
    return executorConnection.getExecuteJobsRunnable(jobIds, processEngine);
  }

}
