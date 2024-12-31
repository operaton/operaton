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
package org.operaton.bpm.spring.boot.starter;

import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.spring.boot.starter.AdditionalCammundaBpmConfigurations.AfterStandardConfiguration;
import org.operaton.bpm.spring.boot.starter.AdditionalCammundaBpmConfigurations.BeforeStandardConfiguration;
import org.operaton.bpm.spring.boot.starter.test.nonpa.TestApplication;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
  classes = {TestApplication.class, AdditionalCammundaBpmConfigurations.class},
  webEnvironment = WebEnvironment.NONE,
  properties = {"operaton.bpm.admin-user.id=admin"}
)
class OperatonAutoConfigurationIT extends AbstractOperatonAutoConfigurationIT {

  @Test
  void autoDeploymentTest() {
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionName("TestProcess").singleResult();
    assertThat(processDefinition).isNotNull();
  }

  @Test
  void jobConfigurationTest() {
    assertThat(jobExecutor.isActive()).isTrue();
  }

  @Test
  void orderedConfigurationTest() {
    assertThat(BeforeStandardConfiguration.processed).isTrue();
    assertThat(AfterStandardConfiguration.processed).isTrue();
  }

  @Test
  void adminUserCreatedWithDefaultPassword() {
    assertThat(identityService.checkPassword("admin", "admin")).isTrue();
  }
}
