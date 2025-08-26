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
package org.operaton.bpm.engine.test.bpmn;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.OperatonFormDefinition;
import org.operaton.bpm.engine.repository.Resource;
import org.operaton.bpm.engine.test.form.deployment.FindOperatonFormDefinitionsCmd;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperatonFormDefinitionStrictParseTest {

  private static final String FORM = "org/operaton/bpm/engine/test/bpmn/OperatonFormDefinitionStrictParseTest.anyForm.form";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  ProcessEngineConfigurationImpl processEngineConfiguration;
  RepositoryService repositoryService;

  @AfterEach
  void reset() {
    processEngineConfiguration.setDisableStrictOperatonFormParsing(false);
  }

  @Test
  void shouldParseAnyFormFile_strictParsingDisabled() {
    // given
    processEngineConfiguration.setDisableStrictOperatonFormParsing(true);

    // when
    testRule.deploy(FORM);

    // then deployment was successful
    List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
    assertThat(deployments).hasSize(1);

    // resource was still deployed
    List<Resource> resources = repositoryService.getDeploymentResources(deployments.get(0).getId());
    assertThat(resources).hasSize(1);
    assertThat(resources.get(0).getName()).isEqualTo(FORM);

    // no form definition was created
    List<OperatonFormDefinition> formDefinitions = engineRule.getProcessEngineConfiguration().getCommandExecutorTxRequired()
        .execute(new FindOperatonFormDefinitionsCmd());
    assertThat(formDefinitions).isEmpty();

  }

  @Test
  void shouldNotParseAnyFormFile_strictParsingEnabled() {
    // given
    processEngineConfiguration.setDisableStrictOperatonFormParsing(false);

    // then deployment fails with an exception
    assertThatThrownBy(() -> {
      testRule.deploy(FORM);
    }).isInstanceOf(ProcessEngineException.class)
    .hasMessageContaining("ENGINE-09033 Could not parse Operaton Form resource org/operaton/bpm/engine/test/bpmn/OperatonFormDefinitionStrictParseTest.anyForm.form.");
    assertThat(repositoryService.createDeploymentQuery().list()).isEmpty();
  }
}
