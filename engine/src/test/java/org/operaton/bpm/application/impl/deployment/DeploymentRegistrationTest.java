/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.application.impl.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ManagementService;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author Roman Smirnov
 *
 */
@ExtendWith(ProcessEngineExtension.class)
public class DeploymentRegistrationTest {

  protected static final String DEPLOYMENT_NAME = "my-deployment";

  protected static final String PROCESS_KEY = "process-1";
  protected static final String BPMN_RESOURCE = "path/to/my/process1.bpmn";

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected RuntimeService runtimeService;
  protected RepositoryService repositoryService;
  protected ManagementService managementService;

  @Test
  public void testNoRegistrationCheckIfNoProcessApplicationIsDeployed() {

    // create two deployments; both contain a process with the same key
    Deployment deployment1 = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance(BPMN_RESOURCE, createProcessWithServiceTask(PROCESS_KEY))
        .deploy();

    Deployment deployment2 = repositoryService
        .createDeployment()
        .name(DEPLOYMENT_NAME)
        .addDeploymentResources(deployment1.getId())
        .deploy();

    // assume an empty deployment cache (e.g. on a different engine)
    processEngineConfiguration.getDeploymentCache().discardProcessDefinitionCache();

    // then starting a process instance for the latest version
    //
    // The context switch mechanism for process definitions in redeployments
    // is to look up the process application registration from a previous version
    // of the same process. This can trigger fetching these process definitions
    // from the database.
    //
    // In case where there are no process application registrations anyway (e.g. embedded engine),
    // this logic should not be executed.
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);

    ProcessDefinition version1 = repositoryService.createProcessDefinitionQuery().deploymentId(deployment1.getId()).singleResult();
    ProcessDefinition version2 = repositoryService.createProcessDefinitionQuery().deploymentId(deployment2.getId()).singleResult();

    // accordingly the process definition cache should only contain the latest version now
    var cache = processEngineConfiguration.getDeploymentCache().getProcessDefinitionCache();
    assertThat(cache.get(version2.getId())).isNotNull();
    assertThat(cache.get(version1.getId())).isNull();

    deleteDeployments(deployment1, deployment2);
  }

  // helper ///////////////////////////////////////////

  protected void deleteDeployments(Deployment... deployments){
    for (Deployment deployment : deployments) {
      repositoryService.deleteDeployment(deployment.getId(), true);
      managementService.unregisterProcessApplication(deployment.getId(), false);
    }
  }

  protected BpmnModelInstance createProcessWithServiceTask(String key) {
    return Bpmn.createExecutableProcess(key)
      .startEvent()
      .serviceTask()
        .operatonExpression("${true}")
      .endEvent()
    .done();
  }

}
