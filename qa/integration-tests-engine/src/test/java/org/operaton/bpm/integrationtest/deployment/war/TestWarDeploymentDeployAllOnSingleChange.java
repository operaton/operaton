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
package org.operaton.bpm.integrationtest.deployment.war;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;


@ExtendWith(ArquillianExtension.class)
public class TestWarDeploymentDeployAllOnSingleChange extends AbstractFoxPlatformIntegrationTest {

  private static final String PA1 = "PA1";
  private static final String PA2 = "PA2";

  @Deployment(order=1, name=PA1)
  public static WebArchive processArchive1() {
    return initWebArchiveDeployment("pa1.war")
      .addAsResource("org/operaton/bpm/integrationtest/deployment/war/testDeployProcessArchiveV1.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/deployment/war/testDeployProcessArchiveUnchanged.bpmn20.xml");
  }

  @Deployment(order=2, name=PA2)
  public static WebArchive processArchive2() {
    return initWebArchiveDeployment("pa2.war", "org/operaton/bpm/integrationtest/deployment/war/deployAllOnSingleChange_processes.xml")
      .addAsResource("org/operaton/bpm/integrationtest/deployment/war/testDeployProcessArchiveV2.bpmn20.xml")
      .addAsResource("org/operaton/bpm/integrationtest/deployment/war/testDeployProcessArchiveUnchanged.bpmn20.xml");

  }

  @Test
  @OperateOnDeployment(PA2)
  void deployProcessArchive() {
    assertThat(processEngine).isNotNull();
    RepositoryService repositoryService = processEngine.getRepositoryService();
    long count = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey("testDeployProcessArchive")
      .count();

    Assertions.assertEquals(2, count);

    count = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey("testDeployProcessArchiveUnchanged")
        .count();

    Assertions.assertEquals(2, count);
  }


}
