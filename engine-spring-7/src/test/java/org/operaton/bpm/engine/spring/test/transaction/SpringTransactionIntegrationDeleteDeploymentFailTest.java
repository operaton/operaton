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
package org.operaton.bpm.engine.spring.test.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.test.context.ContextConfiguration;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.spring.test.SpringProcessEngineTestCase;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Svetlana Dorokhova
 */

@ContextConfiguration("classpath:org/operaton/bpm/engine/spring/test/transaction/SpringTransactionIntegrationDeleteDeploymentFailTest-context.xml")
class SpringTransactionIntegrationDeleteDeploymentFailTest extends SpringProcessEngineTestCase {

  private String deploymentId;

  @Override
  @AfterEach
  protected void tearDown(TestInfo testInfo) throws Exception {
    processEngineConfiguration.getCommandExecutorTxRequired().execute((Command<Void>) commandContext -> {
      commandContext
        .getDeploymentManager()
        .deleteDeployment(deploymentId, false, false, false);
      return null;
    });
    super.tearDown(testInfo);
  }

  @Test
  void failingAfterDeleteDeployment() {
    //given
    final BpmnModelInstance model = Bpmn.createExecutableProcess().startEvent().userTask().endEvent().done();
    RepositoryService repositoryService = processEngine.getRepositoryService();
    deploymentId = repositoryService.createDeployment().addModelInstance("model.bpmn", model).deploy().getId();

    //when
    // 1. delete deployment
    // 2. it fails in post command interceptor (see FailDeleteDeploymentsPlugin)
    // 3. transaction is rolling back
    // 4. DeleteDeploymentFailListener is called
    assertThatThrownBy(() -> repositoryService.deleteDeployment(deploymentId))
      .isInstanceOf(RuntimeException.class);

    //then
    // DeleteDeploymentFailListener succeeded to registered deployments back
    assertThat(processEngineConfiguration.getRegisteredDeployments()).hasSize(1);
  }

}
