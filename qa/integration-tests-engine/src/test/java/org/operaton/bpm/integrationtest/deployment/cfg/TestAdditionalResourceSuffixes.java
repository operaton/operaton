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
package org.operaton.bpm.integrationtest.deployment.cfg;

import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.repository.ProcessDefinitionQuery;
import org.operaton.bpm.engine.repository.Resource;
import org.operaton.bpm.integrationtest.util.AbstractFoxPlatformIntegrationTest;
import org.operaton.bpm.integrationtest.util.DeploymentHelper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastian Menski
 */
@ExtendWith(ArquillianExtension.class)
public class TestAdditionalResourceSuffixes extends AbstractFoxPlatformIntegrationTest {

  @Deployment
  public static WebArchive processArchive() {
      return ShrinkWrap.create(WebArchive.class)
        .addAsWebInfResource("org/operaton/bpm/integrationtest/beans.xml", "beans.xml")
        .addAsLibraries(DeploymentHelper.getEngineCdi())
        .addAsLibraries(DeploymentHelper.getTestingLibs())
        .addClass(AbstractFoxPlatformIntegrationTest.class)
        .addClass(DummyProcessApplication.class)
        .addAsResource("org/operaton/bpm/integrationtest/deployment/cfg/processes-additional-resource-suffixes.xml", "META-INF/processes.xml")
        .addAsResource("org/operaton/bpm/integrationtest/deployment/cfg/invoice-it.bpmn20.xml")
        .addAsResource("org/operaton/bpm/integrationtest/deployment/cfg/hello.groovy")
        .addAsResource("org/operaton/bpm/integrationtest/deployment/cfg/hello.py");
  }

  @Test
  void testDeployProcessArchive() {
    assertThat(processEngine).isNotNull();
    RepositoryService repositoryService = processEngine.getRepositoryService();

    ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery()
      .processDefinitionKey("invoice-it");

    assertThat(processDefinitionQuery.count()).isOne();
    ProcessDefinition processDefinition = processDefinitionQuery.singleResult();

    String deploymentId = repositoryService.createDeploymentQuery()
      .deploymentId(processDefinition.getDeploymentId())
      .singleResult()
      .getId();
    List<Resource> deploymentResources = repositoryService.getDeploymentResources(deploymentId);
    assertThat(deploymentResources).hasSize(3);
  }

}
