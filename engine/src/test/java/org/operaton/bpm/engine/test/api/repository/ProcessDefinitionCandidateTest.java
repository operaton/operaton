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
package org.operaton.bpm.engine.test.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.task.IdentityLink;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

class ProcessDefinitionCandidateTest {

  protected static final String TENANT_ONE = "tenant1";

  protected static final String CANDIDATE_STARTER_USER = "org/operaton/bpm/engine/test/api/repository/ProcessDefinitionCandidateTest.testCandidateStarterUser.bpmn20.xml";
  protected static final String CANDIDATE_STARTER_USERS = "org/operaton/bpm/engine/test/api/repository/ProcessDefinitionCandidateTest.testCandidateStarterUsers.bpmn20.xml";

  protected static final String CANDIDATE_STARTER_GROUP = "org/operaton/bpm/engine/test/api/repository/ProcessDefinitionCandidateTest.testCandidateStarterGroup.bpmn20.xml";
  protected static final String CANDIDATE_STARTER_GROUPS = "org/operaton/bpm/engine/test/api/repository/ProcessDefinitionCandidateTest.testCandidateStarterGroups.bpmn20.xml";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  RepositoryService repositoryService;

  @Test
  void shouldPropagateTenantIdToCandidateStarterUser() {
    // when
    DeploymentBuilder builder = repositoryService.createDeployment()
      .addClasspathResource(CANDIDATE_STARTER_USER)
      .tenantId(TENANT_ONE);
    testRule.deploy(builder);

    // then
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(processDefinition.getId());
    assertThat(links).hasSize(1);

    IdentityLink link = links.get(0);
    assertThat(link.getTenantId()).isNotNull();
    assertThat(link.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void shouldPropagateTenantIdToCandidateStarterUsers() {
    // when
    DeploymentBuilder builder = repositoryService.createDeployment()
      .addClasspathResource(CANDIDATE_STARTER_USERS)
      .tenantId(TENANT_ONE);
    testRule.deploy(builder);

    // then
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(processDefinition.getId());
    assertThat(links).hasSize(3);

    for (IdentityLink link : links) {
      assertThat(link.getTenantId()).isNotNull();
      assertThat(link.getTenantId()).isEqualTo(TENANT_ONE);
    }
  }

  @Test
  void shouldPropagateTenantIdToCandidateStarterGroup() {
    // when
    DeploymentBuilder builder = repositoryService.createDeployment()
      .addClasspathResource(CANDIDATE_STARTER_GROUP)
      .tenantId(TENANT_ONE);
    testRule.deploy(builder);

    // then
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(processDefinition.getId());
    assertThat(links).hasSize(1);

    IdentityLink link = links.get(0);
    assertThat(link.getTenantId()).isNotNull();
    assertThat(link.getTenantId()).isEqualTo(TENANT_ONE);
  }

  @Test
  void shouldPropagateTenantIdToCandidateStarterGroups() {
    // when
    DeploymentBuilder builder = repositoryService.createDeployment()
      .addClasspathResource(CANDIDATE_STARTER_GROUPS)
      .tenantId(TENANT_ONE);
    testRule.deploy(builder);

    // then
    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
    List<IdentityLink> links = repositoryService.getIdentityLinksForProcessDefinition(processDefinition.getId());
    assertThat(links).hasSize(3);

    for (IdentityLink link : links) {
      assertThat(link.getTenantId()).isNotNull();
      assertThat(link.getTenantId()).isEqualTo(TENANT_ONE);
    }
  }
}
