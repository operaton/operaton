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
package org.operaton.bpm.quarkus.engine.test.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.util.List;

import io.quarkus.test.QuarkusUnitTest;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.quarkus.engine.extension.event.OperatonEngineStartupEvent;
import org.operaton.bpm.quarkus.engine.test.helper.ProcessEngineAwareExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ProcessEngineMultipleDeploymentTest {

  @RegisterExtension
  protected static final QuarkusUnitTest unitTest = new ProcessEngineAwareExtension()
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
          .addAsResource("org/operaton/bpm/quarkus/engine/test/deployment/simpleServiceTaskProcess.bpmn"));

  @ApplicationScoped
  static class MyConfig {

    BpmnModelInstance process1 = Bpmn.createExecutableProcess("process-one")
        .startEvent()
        .userTask()
        .endEvent()
        .done();

    BpmnModelInstance process2 = Bpmn.createExecutableProcess("process-two")
        .startEvent()
        .serviceTask()
          .operatonExpression("${true}")
        .endEvent()
        .done();

    @Inject
    RepositoryService repositoryService;

    public void createDeployment1(@Observes OperatonEngineStartupEvent event) {
      repositoryService.createDeployment()
          .name("deployment-1")
          .addModelInstance("process-one.bpmn", process1)
          .addModelInstance("process-two.bpmn", process2)
          .deploy();
    }

    public void createDeployment2(@Observes OperatonEngineStartupEvent event) {
      repositoryService.createDeployment()
          .name("deployment-2")
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
    assertThat(deployments).hasSize(2);
    assertThat(processDefinitions).hasSize(3);

    List<String> deploymentNames = deployments.stream()
        .map(Deployment::getName)
        .toList();
    assertThat(deploymentNames).contains("deployment-1", "deployment-2");
  }

}
