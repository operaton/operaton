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
package org.operaton.bpm.engine.test.api.multitenancy.tenantcheck;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentQuery;
import org.operaton.bpm.engine.repository.Resource;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * @author kristin.polenz
 */
class MultiTenancyDeploymentCmdsTenantCheckTest {

  protected static final String TENANT_TWO = "tenant2";
  protected static final String TENANT_ONE = "tenant1";

  protected static final BpmnModelInstance emptyProcess = Bpmn.createExecutableProcess().startEvent().done();
  protected static final BpmnModelInstance startEndProcess = Bpmn.createExecutableProcess().startEvent().endEvent().done();

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  static ProcessEngineTestExtension testRule = new ProcessEngineTestExtension(engineRule);

  protected RepositoryService repositoryService;
  protected IdentityService identityService;
  protected ProcessEngineConfiguration processEngineConfiguration;

  @Test
  void createDeploymentForAnotherTenant() {
    identityService.setAuthentication("user", null, null);

    repositoryService.createDeployment().addModelInstance("emptyProcess.bpmn", emptyProcess)
      .tenantId(TENANT_ONE).deploy();

    identityService.clearAuthentication();

    DeploymentQuery query = repositoryService.createDeploymentQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void createDeploymentWithAuthenticatedTenant() {
    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    repositoryService.createDeployment().addModelInstance("emptyProcess.bpmn", emptyProcess)
      .tenantId(TENANT_ONE).deploy();

    identityService.clearAuthentication();

    DeploymentQuery query = repositoryService.createDeploymentQuery();
    assertThat(query.count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
  }

  @Test
  void createDeploymentDisabledTenantCheck() {
    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    repositoryService.createDeployment().addModelInstance("emptyProcessOne", emptyProcess).tenantId(TENANT_ONE).deploy();
    repositoryService.createDeployment().addModelInstance("emptyProcessTwo", startEndProcess).tenantId(TENANT_TWO).deploy();

    DeploymentQuery query = repositoryService.createDeploymentQuery();
    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isEqualTo(1L);
  }

  @Test
  void failToDeleteDeploymentNoAuthenticatedTenant() {
    Deployment deployment = testRule.deployForTenant(TENANT_ONE, emptyProcess);
    String deploymentId = deployment.getId();

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.deleteDeployment(deploymentId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot delete the deployment");

  }

  @Test
  void deleteDeploymentWithAuthenticatedTenant() {
    Deployment deployment = testRule.deployForTenant(TENANT_ONE, emptyProcess);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    repositoryService.deleteDeployment(deployment.getId());

    identityService.clearAuthentication();

    DeploymentQuery query = repositoryService.createDeploymentQuery();
    assertThat(query.count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isZero();
  }

  @Test
  void deleteDeploymentDisabledTenantCheck() {
    Deployment deploymentOne = testRule.deployForTenant(TENANT_ONE, emptyProcess);
    Deployment deploymentTwo = testRule.deployForTenant(TENANT_TWO, startEndProcess);

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    repositoryService.deleteDeployment(deploymentOne.getId());
    repositoryService.deleteDeployment(deploymentTwo.getId());

    DeploymentQuery query = repositoryService.createDeploymentQuery();
    assertThat(query.count()).isZero();
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isZero();
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isZero();
  }

  @Test
  void failToGetDeploymentResourceNamesNoAuthenticatedTenant() {
    Deployment deployment = testRule.deployForTenant(TENANT_ONE, emptyProcess);
    String deploymentId = deployment.getId();

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getDeploymentResourceNames(deploymentId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the deployment");
  }

  @Test
  void getDeploymentResourceNamesWithAuthenticatedTenant() {
    Deployment deployment = testRule.deployForTenant(TENANT_ONE, emptyProcess);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    List<String> deploymentResourceNames = repositoryService.getDeploymentResourceNames(deployment.getId());
    assertThat(deploymentResourceNames).hasSize(1);
  }

  @Test
  void getDeploymentResourceNamesDisabledTenantCheck() {
    Deployment deploymentOne = testRule.deployForTenant(TENANT_ONE, emptyProcess);
    Deployment deploymentTwo = testRule.deployForTenant(TENANT_TWO, startEndProcess);

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    List<String> deploymentResourceNames = repositoryService.getDeploymentResourceNames(deploymentOne.getId());
    assertThat(deploymentResourceNames).hasSize(1);

    deploymentResourceNames = repositoryService.getDeploymentResourceNames(deploymentTwo.getId());
    assertThat(deploymentResourceNames).hasSize(1);
  }

  @Test
  void failToGetDeploymentResourcesNoAuthenticatedTenant() {
    Deployment deployment = testRule.deployForTenant(TENANT_ONE, emptyProcess);
    String deploymentId = deployment.getId();

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getDeploymentResources(deploymentId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the deployment");
  }

  @Test
  void getDeploymentResourcesWithAuthenticatedTenant() {
    Deployment deployment = testRule.deployForTenant(TENANT_ONE, emptyProcess);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    List<Resource> deploymentResources = repositoryService.getDeploymentResources(deployment.getId());
    assertThat(deploymentResources).hasSize(1);
  }

  @Test
  void getDeploymentResourcesDisabledTenantCheck() {
    Deployment deploymentOne = testRule.deployForTenant(TENANT_ONE, emptyProcess);
    Deployment deploymentTwo = testRule.deployForTenant(TENANT_TWO, startEndProcess);

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    List<Resource> deploymentResources = repositoryService.getDeploymentResources(deploymentOne.getId());
    assertThat(deploymentResources).hasSize(1);

    deploymentResources = repositoryService.getDeploymentResources(deploymentTwo.getId());
    assertThat(deploymentResources).hasSize(1);
  }

  @Test
  void failToGetResourceAsStreamNoAuthenticatedTenant() {
    Deployment deployment = testRule.deployForTenant(TENANT_ONE, emptyProcess);
    String deploymentId = deployment.getId();

    Resource resource = repositoryService.getDeploymentResources(deploymentId).get(0);
    String resourceName = resource.getName();

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getResourceAsStream(deploymentId, resourceName))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the deployment");
  }

  @Test
  void getResourceAsStreamWithAuthenticatedTenant() {
    Deployment deployment = testRule.deployForTenant(TENANT_ONE, emptyProcess);

    Resource resource = repositoryService.getDeploymentResources(deployment.getId()).get(0);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    InputStream inputStream = repositoryService.getResourceAsStream(deployment.getId(), resource.getName());
    assertThat(inputStream).isNotNull();
  }

  @Test
  void getResourceAsStreamDisabledTenantCheck() {
    Deployment deploymentOne = testRule.deployForTenant(TENANT_ONE, emptyProcess);
    Deployment deploymentTwo = testRule.deployForTenant(TENANT_TWO, startEndProcess);

    Resource resourceOne = repositoryService.getDeploymentResources(deploymentOne.getId()).get(0);
    Resource resourceTwo = repositoryService.getDeploymentResources(deploymentTwo.getId()).get(0);

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    InputStream inputStream = repositoryService.getResourceAsStream(deploymentOne.getId(), resourceOne.getName());
    assertThat(inputStream).isNotNull();

    inputStream = repositoryService.getResourceAsStream(deploymentTwo.getId(), resourceTwo.getName());
    assertThat(inputStream).isNotNull();
  }

  @Test
  void failToGetResourceAsStreamByIdNoAuthenticatedTenant() {
    Deployment deployment = testRule.deployForTenant(TENANT_ONE, emptyProcess);
    String deploymentId = deployment.getId();

    Resource resource = repositoryService.getDeploymentResources(deploymentId).get(0);
    String resourceId = resource.getId();

    identityService.setAuthentication("user", null, null);

    // when/then
    assertThatThrownBy(() -> repositoryService.getResourceAsStreamById(deploymentId, resourceId))
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the deployment");
  }

  @Test
  void getResourceAsStreamByIdWithAuthenticatedTenant() {
    Deployment deployment = testRule.deployForTenant(TENANT_ONE, emptyProcess);

    Resource resource = repositoryService.getDeploymentResources(deployment.getId()).get(0);

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    InputStream inputStream = repositoryService.getResourceAsStreamById(deployment.getId(), resource.getId());
    assertThat(inputStream).isNotNull();
  }

  @Test
  void getResourceAsStreamByIdDisabledTenantCheck() {
    Deployment deploymentOne = testRule.deployForTenant(TENANT_ONE, emptyProcess);
    Deployment deploymentTwo = testRule.deployForTenant(TENANT_TWO, startEndProcess);

    Resource resourceOne = repositoryService.getDeploymentResources(deploymentOne.getId()).get(0);
    Resource resourceTwo = repositoryService.getDeploymentResources(deploymentTwo.getId()).get(0);

    processEngineConfiguration.setTenantCheckEnabled(false);
    identityService.setAuthentication("user", null, null);

    InputStream inputStream = repositoryService.getResourceAsStreamById(deploymentOne.getId(), resourceOne.getId());
    assertThat(inputStream).isNotNull();

    inputStream = repositoryService.getResourceAsStreamById(deploymentTwo.getId(), resourceTwo.getId());
    assertThat(inputStream).isNotNull();
  }

  @Test
  void redeployForDifferentAuthenticatedTenants() {
    Deployment deploymentOne = repositoryService.createDeployment()
      .addModelInstance("emptyProcess.bpmn", emptyProcess)
      .addModelInstance("startEndProcess.bpmn", startEndProcess)
      .tenantId(TENANT_ONE)
      .deploy();

    identityService.setAuthentication("user", null, List.of(TENANT_TWO));
    var deploymentBuilder = repositoryService.createDeployment()
      .addDeploymentResources(deploymentOne.getId())
      .tenantId(TENANT_TWO);

    // when/then
    assertThatThrownBy(deploymentBuilder::deploy)
      .isInstanceOf(ProcessEngineException.class)
      .hasMessageContaining("Cannot get the deployment");
  }

  @Test
  void redeployForTheSameAuthenticatedTenant() {
    Deployment deploymentOne = repositoryService.createDeployment()
      .addModelInstance("emptyProcess.bpmn", emptyProcess)
      .addModelInstance("startEndProcess.bpmn", startEndProcess)
      .tenantId(TENANT_ONE)
      .deploy();

    identityService.setAuthentication("user", null, List.of(TENANT_ONE));

    repositoryService.createDeployment()
        .addDeploymentResources(deploymentOne.getId())
        .tenantId(TENANT_ONE)
        .deploy();

    DeploymentQuery query = repositoryService.createDeploymentQuery();
    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(2L);
  }

  @Test
  void redeployForDifferentAuthenticatedTenantsDisabledTenantCheck() {
    Deployment deploymentOne = repositoryService.createDeployment()
      .addModelInstance("emptyProcess.bpmn", emptyProcess)
      .addModelInstance("startEndProcess.bpmn", startEndProcess)
      .tenantId(TENANT_ONE)
      .deploy();

    identityService.setAuthentication("user", null, null);
    processEngineConfiguration.setTenantCheckEnabled(false);

    repositoryService.createDeployment()
        .addDeploymentResources(deploymentOne.getId())
        .tenantId(TENANT_TWO)
        .deploy();

    DeploymentQuery query = repositoryService.createDeploymentQuery();
    assertThat(query.count()).isEqualTo(2L);
    assertThat(query.tenantIdIn(TENANT_ONE).count()).isEqualTo(1L);
    assertThat(query.tenantIdIn(TENANT_TWO).count()).isEqualTo(1L);
  }

  @AfterEach
  void tearDown() {
    identityService.clearAuthentication();
    for(Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }
  }
}
