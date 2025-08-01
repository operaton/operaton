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
package org.operaton.bpm.engine.spring.test.application;

import org.operaton.bpm.application.PostDeploy;
import org.operaton.bpm.application.PreUndeploy;
import org.operaton.bpm.application.ProcessApplicationExecutionException;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.spring.application.SpringProcessApplication;

import java.util.concurrent.Callable;

/**
 * @author Daniel Meyer
 *
 */
@SuppressWarnings("unused")
public class PostDeployRegistrationPa extends SpringProcessApplication {

  protected boolean isPostDeployInvoked;
  protected boolean isPreUndeployInvoked;
  protected String deploymentId;

  @PostDeploy
  public void registerProcessApplication(ProcessEngine processEngine) {

    // lookup existing deployment
    ProcessDefinition processDefinition = processEngine.getRepositoryService()
      .createProcessDefinitionQuery()
      .processDefinitionKey("startToEnd")
      .latestVersion()
      .singleResult();

    deploymentId = processDefinition.getDeploymentId();

    // register with the process engine
    processEngine.getManagementService()
      .registerProcessApplication(deploymentId, getReference());


    isPostDeployInvoked = true;
  }

  @PreUndeploy
  public void unregisterProcessApplication(ProcessEngine processEngine) {

    // unregister with the process engine
    processEngine.getManagementService()
      .unregisterProcessApplication(deploymentId, true);

    isPreUndeployInvoked = true;

  }


  // customization of Process Application for unit test ////////////////////////////

  protected boolean isInvoked;

  @Override
  public void start() {
    // do not auto-deploy the process application : we want to manually deploy
    // from the test-case
  }

  /** override execute to intercept calls from process engine and record that we are invoked. */
  @Override
  public <T> T execute(Callable<T> callable) throws ProcessApplicationExecutionException {
    T result = super.execute(callable);
    isInvoked = true;
    return result;
  }

  public boolean isInvoked() {
    return isInvoked;
  }

  public boolean isPostDeployInvoked() {
    return isPostDeployInvoked;
  }

  public boolean isPreUndeployInvoked() {
    return isPreUndeployInvoked;
  }

}
