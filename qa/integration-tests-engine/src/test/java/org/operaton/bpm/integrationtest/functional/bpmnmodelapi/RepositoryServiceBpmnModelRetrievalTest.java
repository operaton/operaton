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
package org.operaton.bpm.integrationtest.functional.bpmnmodelapi;

import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

import org.jboss.arquillian.container.test.api.Deployment;

import static org.assertj.core.api.Assertions.assertThat;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Daniel Meyer
 *
 */
@RunWith(Arquillian.class)
public class RepositoryServiceBpmnModelRetrievalTest extends AbstractFoxPlatformIntegrationTest {

  private static final String TEST_PROCESS = "testProcess";

  @Deployment
  public static WebArchive createProcessApplication() {
    return initWebArchiveDeployment()
        .addAsResource(new StringAsset(Bpmn.convertToString(Bpmn.createExecutableProcess(TEST_PROCESS).operatonHistoryTimeToLive(180).startEvent().done())), "testProcess.bpmn20.xml");
  }

  @Test
  public void shouldReturnBpmnModelInstance() {

    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey(TEST_PROCESS)
      .singleResult();

    BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(processDefinition.getId());
    assertThat(bpmnModelInstance).isNotNull();

  }
}
