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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(ArquillianExtension.class)
public class TestWarDeploymentWithDmn extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
    return initWebArchiveDeployment()
            .addAsResource("org/operaton/bpm/integrationtest/testDeployProcessArchiveWithDmn.dmn");
  }

  @Test
  void testDeployProcessArchive() {
    assertThat(processEngine).isNotNull();
    RepositoryService repositoryService = processEngine.getRepositoryService();
    long count = repositoryService.createDecisionDefinitionQuery()
      .decisionDefinitionKey("testDeployProcessArchiveWithDmn")
      .count();

    assertThat(count).isOne();
  }

}
