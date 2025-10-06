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
package org.operaton.bpm.quarkus.engine.test.deployment;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.quarkus.engine.extension.event.OperatonEngineStartupEvent;
import org.operaton.bpm.quarkus.engine.test.helper.ProcessEngineAwareExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessEngineSingleDeploymentTest {

  @RegisterExtension
  protected static final QuarkusUnitTest unitTest = new ProcessEngineAwareExtension()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
          .addAsResource("org/operaton/bpm/quarkus/engine/test/deployment/simpleServiceTaskProcess.bpmn"));

  @ApplicationScoped
  static class MyConfig {

    @Inject
    RepositoryService repositoryService;

    public void createDeployment(@Observes OperatonEngineStartupEvent event) {
      repositoryService.createDeployment()
          .addClasspathResource("org/operaton/bpm/quarkus/engine/test/deployment/simpleServiceTaskProcess.bpmn")
          .deploy();
    }

  }

  @Inject
  public ProcessEngine processEngine;

  @Test
  void shouldHaveDeployedResources() {
    // given
    RepositoryService repositoryService = processEngine.getRepositoryService();

    // when
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();

    // then
    assertThat(deployments).hasSize(1);
    assertThat(processDefinitions).hasSize(1);

    Deployment deployment = deployments.get(0);
    ProcessDefinition processDefinition = processDefinitions.get(0);
    assertThat(processDefinition.getDeploymentId()).isEqualTo(deployment.getId());
  }

}
