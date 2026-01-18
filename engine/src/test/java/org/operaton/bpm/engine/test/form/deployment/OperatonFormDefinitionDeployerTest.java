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
package org.operaton.bpm.engine.test.form.deployment;

import java.util.Collection;
import java.util.List;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.OperatonFormDefinition;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameter;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameterized;
import org.operaton.bpm.engine.test.junit5.ParameterizedTestExtension.Parameters;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.engine.test.util.OperatonFormUtils;

import static org.assertj.core.api.Assertions.assertThat;

@Parameterized
public class OperatonFormDefinitionDeployerTest {

  protected static final String BPMN_USER_TASK_FORM_REF_DEPLOYMENT = "org/operaton/bpm/engine/test/form/deployment/OperatonFormDefinitionDeployerTest.shouldDeployProcessWithOperatonFormDefinitionBindingDeployment.bpmn";
  protected static final String BPMN_USER_TASK_FORM_REF_LATEST = "org/operaton/bpm/engine/test/form/deployment/OperatonFormDefinitionDeployerTest.shouldDeployProcessWithOperatonFormDefinitionBindingLatest.bpmn";
  protected static final String BPMN_USER_TASK_FORM_REF_VERSION = "org/operaton/bpm/engine/test/form/deployment/OperatonFormDefinitionDeployerTest.shouldDeployProcessWithOperatonFormDefinitionBindingVersion.bpmn";
  protected static final String SIMPLE_FORM = "org/operaton/bpm/engine/test/form/deployment/OperatonFormDefinitionDeployerTest.simple_form.form";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RepositoryService repositoryService;
  ProcessEngineConfigurationImpl processEngineConfiguration;

  @Parameter(0)
  public String bpmnResource;

  @Parameters
  public static Collection<Object> params() {
    return List.of(new String[] {
        BPMN_USER_TASK_FORM_REF_DEPLOYMENT,
        BPMN_USER_TASK_FORM_REF_LATEST,
        BPMN_USER_TASK_FORM_REF_VERSION });
  }

  @TestTemplate
  void shouldDeployProcessWithOperatonFormDefinition() {
    String deploymentId = testRule.deploy(bpmnResource, SIMPLE_FORM).getId();

    // there should only be one deployment
    long deploymentCount = repositoryService.createDeploymentQuery().count();
    assertThat(deploymentCount).isOne();

    // there should only be one OperatonFormDefinition
    List<OperatonFormDefinition> definitions = OperatonFormUtils.findAllOperatonFormDefinitionEntities(processEngineConfiguration);
    assertThat(definitions).hasSize(1);
    assertThat(definitions.get(0).getDeploymentId()).isEqualTo(deploymentId);
  }
}
