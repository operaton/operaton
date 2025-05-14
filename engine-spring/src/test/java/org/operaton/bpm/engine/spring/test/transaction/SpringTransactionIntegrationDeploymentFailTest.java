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
package org.operaton.bpm.engine.spring.test.transaction;

import org.operaton.bpm.engine.spring.test.SpringProcessEngineTestCase;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Svetlana Dorokhova
 */

@ContextConfiguration("classpath:org/operaton/bpm/engine/spring/test/transaction/SpringTransactionIntegrationDeploymentFailTest-context.xml")
class SpringTransactionIntegrationDeploymentFailTest extends SpringProcessEngineTestCase {

  @Override
  @AfterEach
  protected void tearDown(TestInfo testInfo) throws Exception {
    //must not be needed after CAM-4250 is fixed
    processEngineConfiguration.getDeploymentCache().discardProcessDefinitionCache();
    super.tearDown(testInfo);
  }

  @Test
  void failingAfterDeployment() {
//    given
    final BpmnModelInstance model = Bpmn.createExecutableProcess().startEvent().userTask().endEvent().done();

    //when
    // 1. deploy the process
    // 2. it fails in post command interceptor (see FailDeploymentsPlugin)
    // 3. transaction is rolling back
    // 4. DeploymentFailListener is called
    try {
      processEngine.getRepositoryService().createDeployment().addModelInstance("model.bpmn", model).deploy();
    } catch (Exception ex) {
      //expected exception
    }

    //then
    // DeploymentFailListener succeeded to remove registered deployments
    assertThat(processEngineConfiguration.getRegisteredDeployments()).isEmpty();
  }

}
