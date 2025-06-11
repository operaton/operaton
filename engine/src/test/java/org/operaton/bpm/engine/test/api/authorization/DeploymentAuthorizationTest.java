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
package org.operaton.bpm.engine.test.api.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Permissions.CREATE;
import static org.operaton.bpm.engine.authorization.Permissions.DELETE;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Permissions.UPDATE;
import static org.operaton.bpm.engine.authorization.Resources.DEPLOYMENT;
import static org.operaton.bpm.engine.test.util.QueryTestHelper.verifyQueryResults;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.operaton.bpm.application.ProcessApplicationReference;
import org.operaton.bpm.application.ProcessApplicationRegistration;
import org.operaton.bpm.application.impl.EmbeddedProcessApplication;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.AuthorizationQuery;
import org.operaton.bpm.engine.authorization.Groups;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.authorization.SystemPermissions;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentQuery;
import org.operaton.bpm.engine.repository.Resource;

/**
 * @author Roman Smirnov
 *
 */
class DeploymentAuthorizationTest extends AuthorizationTest {

  protected static final String FIRST_RESOURCE = "org/operaton/bpm/engine/test/api/oneTaskProcess.bpmn20.xml";
  protected static final String SECOND_RESOURCE = "org/operaton/bpm/engine/test/api/authorization/messageBoundaryEventProcess.bpmn20.xml";

  // query ////////////////////////////////////////////////////////////

  @Test
  void testSimpleDeploymentQueryWithoutAuthorization() {
    // given
    createDeployment(null);

    // when
    DeploymentQuery query = repositoryService.createDeploymentQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testSimpleDeploymentQueryWithReadPermissionOnDeployment() {
    // given
    String deploymentId = createDeployment(null);
    createGrantAuthorization(DEPLOYMENT, deploymentId, userId, READ);

    // when
    DeploymentQuery query = repositoryService.createDeploymentQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleDeploymentQueryWithReadPermissionOnAnyDeployment() {
    // given
    createDeployment(null);
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    // when
    DeploymentQuery query = repositoryService.createDeploymentQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testSimpleDeploymentQueryWithMultiple() {
    // given
    String deploymentId = createDeployment(null);
    createGrantAuthorization(DEPLOYMENT, deploymentId, userId, READ);
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    // when
    DeploymentQuery query = repositoryService.createDeploymentQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testDeploymentQueryWithoutAuthorization() {
    // given
    createDeployment("first");
    createDeployment("second");

    // when
    DeploymentQuery query = repositoryService.createDeploymentQuery();

    // then
    verifyQueryResults(query, 0);
  }

  @Test
  void testDeploymentQueryWithReadPermissionOnDeployment() {
    // given
    String deploymentId1 = createDeployment("first");
    createDeployment("second");
    createGrantAuthorization(DEPLOYMENT, deploymentId1, userId, READ);

    // when
    DeploymentQuery query = repositoryService.createDeploymentQuery();

    // then
    verifyQueryResults(query, 1);
  }

  @Test
  void testDeploymentQueryWithReadPermissionOnAnyDeployment() {
    // given
    createDeployment("first");
    createDeployment("second");
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    // when
    DeploymentQuery query = repositoryService.createDeploymentQuery();

    // then
    verifyQueryResults(query, 2);
  }

  @Test
  void shouldNotFindDeploymentWithRevokedReadPermissionOnAnyDeployment() {
    // given
    createDeployment("first");
    createDeployment("second");
    createGrantAuthorization(DEPLOYMENT, ANY, ANY, READ);
    createRevokeAuthorization(DEPLOYMENT, ANY, userId, READ);

    // when
    DeploymentQuery query = repositoryService.createDeploymentQuery();

    // then
    verifyQueryResults(query, 0);
  }

  // create deployment ///////////////////////////////////////////////

  @Test
  void testCreateDeploymentWithoutAuthoriatzion() {
    // given
    var deploymentBuilder = repositoryService
        .createDeployment()
        .addClasspathResource(FIRST_RESOURCE);

    // when
    assertThatThrownBy(deploymentBuilder::deploy)
      // then
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(CREATE.getName())
      .hasMessageContaining(DEPLOYMENT.resourceName());
  }

  @Test
  void testCreateDeployment() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, CREATE);

    // when
    Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(FIRST_RESOURCE)
      .deploy();

    // mark deployment for cleanup
    deploymentIds.add(deployment.getId());

    // then
    disableAuthorization();
    DeploymentQuery query = repositoryService.createDeploymentQuery();
    verifyQueryResults(query, 1);
    enableAuthorization();
  }

  // delete deployment //////////////////////////////////////////////

  @Test
  void testDeleteDeploymentWithoutAuthorization() {
    // given
    String deploymentId = createDeployment(null);

    // when
    assertThatThrownBy(() -> repositoryService.deleteDeployment(deploymentId))
      // then
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(DELETE.getName())
      .hasMessageContaining(DEPLOYMENT.resourceName());
  }

  @Test
  void testDeleteDeploymentWithDeletePermissionOnDeployment() {
    // given
    String deploymentId = createDeployment(null);
    createGrantAuthorization(DEPLOYMENT, deploymentId, userId, DELETE);

    // when
    repositoryService.deleteDeployment(deploymentId);

    // then
    disableAuthorization();
    DeploymentQuery query = repositoryService.createDeploymentQuery();
    verifyQueryResults(query, 0);
    enableAuthorization();
  }

  @Test
  void testDeleteDeploymentWithDeletePermissionOnAnyDeployment() {
    // given
    String deploymentId = createDeployment(null);
    createGrantAuthorization(DEPLOYMENT, ANY, userId, DELETE);

    // when
    repositoryService.deleteDeployment(deploymentId);

    // then
    disableAuthorization();
    DeploymentQuery query = repositoryService.createDeploymentQuery();
    verifyQueryResults(query, 0);
    enableAuthorization();
  }

  // get deployment resource names //////////////////////////////////

  @Test
  void testGetDeploymentResourceNamesWithoutAuthorization() {
    // given
    String deploymentId = createDeployment(null);

    // when
    assertThatThrownBy(() -> repositoryService.getDeploymentResourceNames(deploymentId))
      // then
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(READ.getName())
      .hasMessageContaining(DEPLOYMENT.resourceName());
  }

  @Test
  void testGetDeploymentResourceNamesWithReadPermissionOnDeployment() {
    // given
    String deploymentId = createDeployment(null);
    createGrantAuthorization(DEPLOYMENT, deploymentId, userId, READ);

    // when
    List<String> names = repositoryService.getDeploymentResourceNames(deploymentId);

    // then
    assertThat(names)
            .isNotEmpty()
            .hasSize(2)
            .contains(FIRST_RESOURCE)
            .contains(SECOND_RESOURCE);
  }

  @Test
  void testGetDeploymentResourceNamesWithReadPermissionOnAnyDeployment() {
    // given
    String deploymentId = createDeployment(null);
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    // when
    List<String> names = repositoryService.getDeploymentResourceNames(deploymentId);

    // then
    assertThat(names)
            .isNotEmpty()
            .hasSize(2)
            .contains(FIRST_RESOURCE)
            .contains(SECOND_RESOURCE);
  }

  // get deployment resources //////////////////////////////////

  @Test
  void testGetDeploymentResourcesWithoutAuthorization() {
    // given
    String deploymentId = createDeployment(null);

    // when
    assertThatThrownBy(() -> repositoryService.getDeploymentResources(deploymentId))
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(READ.getName())
      .hasMessageContaining(DEPLOYMENT.resourceName());
  }

  @Test
  void testGetDeploymentResourcesWithReadPermissionOnDeployment() {
    // given
    String deploymentId = createDeployment(null);
    createGrantAuthorization(DEPLOYMENT, deploymentId, userId, READ);

    // when
    List<Resource> resources = repositoryService.getDeploymentResources(deploymentId);

    // then
    assertThat(resources)
            .isNotEmpty()
            .hasSize(2);
  }

  @Test
  void testGetDeploymentResourcesWithReadPermissionOnAnyDeployment() {
    // given
    String deploymentId = createDeployment(null);
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    // when
    List<Resource> resources = repositoryService.getDeploymentResources(deploymentId);

    // then
    assertThat(resources)
            .isNotEmpty()
            .hasSize(2);
  }

  // get resource as stream //////////////////////////////////

  @Test
  void testGetResourceAsStreamWithoutAuthorization() {
    // given
    String deploymentId = createDeployment(null);

    assertThatThrownBy(() -> repositoryService.getResourceAsStream(deploymentId, FIRST_RESOURCE))
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(READ.getName())
      .hasMessageContaining(DEPLOYMENT.resourceName());
  }

  @Test
  void testGetResourceAsStreamWithReadPermissionOnDeployment() {
    // given
    String deploymentId = createDeployment(null);
    createGrantAuthorization(DEPLOYMENT, deploymentId, userId, READ);

    // when
    InputStream stream = repositoryService.getResourceAsStream(deploymentId, FIRST_RESOURCE);

    // then
    assertThat(stream).isNotNull();
  }

  @Test
  void testGetResourceAsStreamWithReadPermissionOnAnyDeployment() {
    // given
    String deploymentId = createDeployment(null);
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    // when
    InputStream stream = repositoryService.getResourceAsStream(deploymentId, FIRST_RESOURCE);

    // then
    assertThat(stream).isNotNull();
  }

  // get resource as stream by id//////////////////////////////////

  @Test
  void testGetResourceAsStreamByIdWithoutAuthorization() {
    // given
    String deploymentId = createDeployment(null);

    disableAuthorization();
    List<Resource> resources = repositoryService.getDeploymentResources(deploymentId);
    enableAuthorization();
    String resourceId = resources.get(0).getId();

    assertThatThrownBy(() -> repositoryService.getResourceAsStreamById(deploymentId, resourceId))
      .isInstanceOf(AuthorizationException.class)
      .hasMessageContaining(userId)
      .hasMessageContaining(READ.getName())
      .hasMessageContaining(DEPLOYMENT.resourceName());
  }

  @Test
  void testGetResourceAsStreamByIdWithReadPermissionOnDeployment() {
    // given
    String deploymentId = createDeployment(null);
    createGrantAuthorization(DEPLOYMENT, deploymentId, userId, READ);

    disableAuthorization();
    List<Resource> resources = repositoryService.getDeploymentResources(deploymentId);
    enableAuthorization();
    String resourceId = resources.get(0).getId();

    // when
    InputStream stream = repositoryService.getResourceAsStreamById(deploymentId, resourceId);

    // then
    assertThat(stream).isNotNull();
  }

  @Test
  void testGetResourceAsStreamByIdWithReadPermissionOnAnyDeployment() {
    // given
    String deploymentId = createDeployment(null);
    createGrantAuthorization(DEPLOYMENT, ANY, userId, READ);

    disableAuthorization();
    List<Resource> resources = repositoryService.getDeploymentResources(deploymentId);
    enableAuthorization();
    String resourceId = resources.get(0).getId();

    // when
    InputStream stream = repositoryService.getResourceAsStreamById(deploymentId, resourceId);

    // then
    assertThat(stream).isNotNull();
  }

  // should create authorization /////////////////////////////////////

  @Test
  void testCreateAuthorizationOnDeploy() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, CREATE);
    Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(FIRST_RESOURCE)
      .deploy();

    // mark deployment for cleanup
    deploymentIds.add(deployment.getId());

    // when
    Authorization authorization = authorizationService
      .createAuthorizationQuery()
      .userIdIn(userId)
      .resourceId(deployment.getId())
      .singleResult();

    // then
    assertThat(authorization).isNotNull();
    assertThat(authorization.isPermissionGranted(READ)).isTrue();
    assertThat(authorization.isPermissionGranted(DELETE)).isTrue();
    assertThat(authorization.isPermissionGranted(UPDATE)).isFalse();
  }

  // clear authorization /////////////////////////////////////

  @Test
  void testClearAuthorizationOnDeleteDeployment() {
    // given
    createGrantAuthorization(DEPLOYMENT, ANY, userId, CREATE);
    Deployment deployment = repositoryService
      .createDeployment()
      .addClasspathResource(FIRST_RESOURCE)
      .deploy();

    String deploymentId = deployment.getId();

    AuthorizationQuery query = authorizationService
      .createAuthorizationQuery()
      .userIdIn(userId)
      .resourceId(deploymentId);

    Authorization authorization = query.singleResult();
    assertThat(authorization).isNotNull();

    // when
    repositoryService.deleteDeployment(deploymentId);

    authorization = query.singleResult();
    assertThat(authorization).isNull();
  }

  // register process application ///////////////////////////////////

  @Test
  void shouldRegisterProcessApplicationAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication();
    ProcessApplicationReference reference = processApplication.getReference();
    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();

    // when
    ProcessApplicationRegistration registration = managementService.registerProcessApplication(deploymentId, reference);

    // then
    assertThat(registration).isNotNull();
    assertThat(getProcessApplicationForDeployment(deploymentId)).isNotNull();
  }

  @Test
  void shouldRegisterProcessApplicationWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.SET);
    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication();
    ProcessApplicationReference reference = processApplication.getReference();
    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();

    // when
    ProcessApplicationRegistration registration = managementService.registerProcessApplication(deploymentId, reference);

    // then
    assertThat(registration).isNotNull();
    assertThat(getProcessApplicationForDeployment(deploymentId)).isNotNull();
  }

  @Test
  void shouldRegisterProcessApplicationWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.SET);
    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication();
    ProcessApplicationReference reference = processApplication.getReference();
    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();

    // when
    ProcessApplicationRegistration registration = managementService.registerProcessApplication(deploymentId, reference);

    // then
    assertThat(registration).isNotNull();
    assertThat(getProcessApplicationForDeployment(deploymentId)).isNotNull();
  }

  @Test
  void shouldNotRegisterProcessApplicationWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.registerProcessApplication(null, null);
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.SET));
  }

  // unregister process application ///////////////////////////////////
  @Test
  void shouldUnregisterProcessApplicationAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication();
    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();
    ProcessApplicationReference reference = processApplication.getReference();
    registerProcessApplication(deploymentId, reference);

    // when
    managementService.unregisterProcessApplication(deploymentId, true);

    // then
    assertThat(getProcessApplicationForDeployment(deploymentId)).isNull();
  }

  @Test
  void shouldUnregisterProcessApplicationWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.SET);

    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication();
    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();
    ProcessApplicationReference reference = processApplication.getReference();
    registerProcessApplication(deploymentId, reference);

    // when
    managementService.unregisterProcessApplication(deploymentId, true);

    // then
    assertThat(getProcessApplicationForDeployment(deploymentId)).isNull();
  }

  @Test
  void shouldUnregisterProcessApplicationWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.SET);

    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication();
    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();
    ProcessApplicationReference reference = processApplication.getReference();
    registerProcessApplication(deploymentId, reference);

    // when
    managementService.unregisterProcessApplication(deploymentId, true);

    // then
    assertThat(getProcessApplicationForDeployment(deploymentId)).isNull();
  }

  @Test
  void shouldNotUnregisterProcessApplicationWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.unregisterProcessApplication("anyDeploymentId", true);
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.SET));
  }

  // get process application for deployment ///////////////////////////////////

  @Test
  void shouldGetProcessApplicationForDeploymentAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication();
    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();
    ProcessApplicationReference reference = processApplication.getReference();
    registerProcessApplication(deploymentId, reference);

    // when
    String application = managementService.getProcessApplicationForDeployment(deploymentId);

    // then
    assertThat(application).isNotNull();
  }

  @Test
  void shouldGetProcessApplicationForDeploymentWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication();
    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();
    ProcessApplicationReference reference = processApplication.getReference();
    registerProcessApplication(deploymentId, reference);

    // when
    String application = managementService.getProcessApplicationForDeployment(deploymentId);

    // then
    assertThat(application).isNotNull();
  }

  @Test
  void shouldGetProcessApplicationForDeploymentWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    EmbeddedProcessApplication processApplication = new EmbeddedProcessApplication();
    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();
    ProcessApplicationReference reference = processApplication.getReference();
    registerProcessApplication(deploymentId, reference);

    // when
    String application = managementService.getProcessApplicationForDeployment(deploymentId);

    // then
    assertThat(application).isNotNull();
  }

  @Test
  void shouldNotGetProcessApplicationForDeploymentWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.getProcessApplicationForDeployment("anyDeploymentId");
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.READ));
  }

  // get registered deployments ///////////////////////////////////

  @Test
  void shouldGetRegisteredDeploymentsAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();

    // when
    Set<String> deployments = managementService.getRegisteredDeployments();

    // then
    assertThat(deployments).contains(deploymentId);
  }

  @Test
  void shouldGetRegisteredDeploymentsWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();

    // when
    Set<String> deployments = managementService.getRegisteredDeployments();

    // then
    assertThat(deployments).contains(deploymentId);
  }

  @Test
  void shouldGetRegisteredDeploymentsWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.READ);

    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();

    // when
    Set<String> deployments = managementService.getRegisteredDeployments();

    // then
    assertThat(deployments).contains(deploymentId);
  }

  @Test
  void shouldNotGetRegisteredDeploymentsWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.getRegisteredDeployments();
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.READ));
  }

  // register deployment for job executor ///////////////////////////////////

  @Test
  void shouldRegisterDeploymentAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();

    // when
    managementService.registerDeploymentForJobExecutor(deploymentId);

    // then
    assertThat(getRegisteredDeployments()).contains(deploymentId);
  }

  @Test
  void shouldRegisterDeploymentWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.SET);

    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();

    // when
    managementService.registerDeploymentForJobExecutor(deploymentId);

    // then
    assertThat(getRegisteredDeployments()).contains(deploymentId);
  }

  @Test
  void shouldRegisterDeploymentWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.SET);

    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();

    // when
    managementService.registerDeploymentForJobExecutor(deploymentId);

    // then
    assertThat(getRegisteredDeployments()).contains(deploymentId);
  }

  @Test
  void shouldNotRegisterDeploymentWithoutAuthorization() {
    // given
    disableAuthorization();
    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();
    enableAuthorization();

    assertThatThrownBy(() -> {
      // when
      managementService.registerDeploymentForJobExecutor(deploymentId);
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.SET));
  }

  // unregister deployment for job executor ///////////////////////////////////

  @Test
  void shouldUnregisterDeploymentAsOperatonAdmin() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));

    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();

    // when
    managementService.unregisterDeploymentForJobExecutor(deploymentId);

    // then
    assertThat(getRegisteredDeployments()).doesNotContain(deploymentId);
  }

  @Test
  void shouldUnregisterDeploymentWithPermission() {
    // given
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.SET);

    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();

    // when
    managementService.unregisterDeploymentForJobExecutor(deploymentId);

    // then
    assertThat(getRegisteredDeployments()).doesNotContain(deploymentId);
  }

  @Test
  void shouldUnregisterDeploymentWithAdminAndPermission() {
    // given
    identityService.setAuthentication(userId, Collections.singletonList(Groups.OPERATON_ADMIN));
    createGrantAuthorization(Resources.SYSTEM, "*", userId, SystemPermissions.SET);

    String deploymentId = createDeployment(null, FIRST_RESOURCE).getId();

    // when
    managementService.unregisterDeploymentForJobExecutor(deploymentId);

    // then
    assertThat(getRegisteredDeployments()).doesNotContain(deploymentId);
  }

  @Test
  void shouldNotUnregisterDeploymentWithoutAuthorization() {
    // given

    assertThatThrownBy(() -> {
      // when
      managementService.unregisterDeploymentForJobExecutor("anyDeploymentId");
    })
        // then
        .hasMessageContaining(permissionException(Resources.SYSTEM, SystemPermissions.SET));
  }

  // helper /////////////////////////////////////////////////////////

  protected String createDeployment(String name) {
    return createDeployment(name, FIRST_RESOURCE, SECOND_RESOURCE).getId();
  }

  protected void registerProcessApplication(String deploymentId, ProcessApplicationReference reference) {
    disableAuthorization();
    managementService.registerProcessApplication(deploymentId, reference);
    enableAuthorization();
  }

  protected String getProcessApplicationForDeployment(String deploymentId) {
    disableAuthorization();
    String applications = managementService.getProcessApplicationForDeployment(deploymentId);
    enableAuthorization();
    return applications;
  }

  protected Set<String> getRegisteredDeployments() {
    disableAuthorization();
    Set<String> deployments = managementService.getRegisteredDeployments();
    enableAuthorization();
    return deployments;
  }

}
