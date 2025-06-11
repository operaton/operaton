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
package org.operaton.bpm.engine.test.standalone.deploy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

class DeploymentAutoHistoryTest {

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(configuration -> {
      configuration.setJdbcUrl("jdbc:h2:mem:DeploymentTest-HistoryLevelAuto;DB_CLOSE_DELAY=1000");
      configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP);
      configuration.setHistoryLevel(null);
      configuration.setHistory(ProcessEngineConfiguration.HISTORY_AUTO);
    })
    .build();

  @Test
  void shouldCreateDeployment() {
     BpmnModelInstance instance = Bpmn.createExecutableProcess("process").startEvent().endEvent().done();

     DeploymentWithDefinitions deployment = engineRule.getRepositoryService()
         .createDeployment()
         .addModelInstance("foo.bpmn", instance)
         .deployWithResult();

     long count = engineRule.getRepositoryService().createDeploymentQuery().count();
    assertThat(count).isEqualTo(1);
     engineRule.getRepositoryService().deleteDeployment(deployment.getId(), true);
  }

}
