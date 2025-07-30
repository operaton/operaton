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
package org.operaton.bpm.cockpit.plugin.base.authorization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.operaton.bpm.cockpit.plugin.test.AbstractCockpitPluginTest;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.repository.ProcessDefinition;
import org.operaton.bpm.engine.runtime.ProcessInstance;

import java.util.List;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Permissions.ALL;
import static org.operaton.bpm.engine.authorization.Resources.AUTHORIZATION;
import static org.operaton.bpm.engine.authorization.Resources.USER;

/**
 * @author Roman Smirnov
 *
 */
public abstract class AuthorizationTest extends AbstractCockpitPluginTest {

  protected String engineName;
  protected ProcessEngineConfigurationImpl processEngineConfiguration;

  protected String userId = "test";
  protected String groupId = "accounting";
  protected User user;
  protected Group group;

  @BeforeEach
  public void setUp() {
    user = createUser(userId);
    group = createGroup(groupId);

    identityService.createMembership(userId, groupId);

    identityService.setAuthentication(userId, List.of(groupId));
    enableAuthorization();
  }

  @AfterEach
  public void tearDown() {
    disableAuthorization();
    identityService.createUserQuery().list().stream().map(User::getId).forEach(identityService::deleteUser);
    identityService.createGroupQuery().list().stream().map(Group::getId).forEach(identityService::deleteGroup);
    authorizationService.createAuthorizationQuery().list().stream().map(Authorization::getId)
        .forEach(authorizationService::deleteAuthorization);
  }

  // user ////////////////////////////////////////////////////////////////

  protected User createUser(String userId) {
    User newUser = identityService.newUser(userId);
    identityService.saveUser(newUser);

    // give user all permission to manipulate authorizations
    Authorization authorization = createGrantAuthorization(AUTHORIZATION, ANY);
    authorization.setUserId(userId);
    authorization.addPermission(ALL);
    saveAuthorization(authorization);

    // give user all permission to manipulate users
    authorization = createGrantAuthorization(USER, ANY);
    authorization.setUserId(userId);
    authorization.addPermission(Permissions.ALL);
    saveAuthorization(authorization);

    return newUser;
  }

  // group //////////////////////////////////////////////////////////////

  protected Group createGroup(String groupId) {
    Group newGroup = identityService.newGroup(groupId);
    identityService.saveGroup(newGroup);
    return newGroup;
  }

  // authorization ///////////////////////////////////////////////////////

  protected void createGrantAuthorization(Resource resource, String resourceId, String userId, Permission... permissions) {
    Authorization authorization = createGrantAuthorization(resource, resourceId);
    authorization.setUserId(userId);
    for (Permission permission : permissions) {
      authorization.addPermission(permission);
    }
    saveAuthorization(authorization);
  }

  protected Authorization createGrantAuthorization(Resource resource, String resourceId) {
    return createAuthorization(AUTH_TYPE_GRANT, resource, resourceId);
  }

  protected Authorization createAuthorization(int type, Resource resource, String resourceId) {
    Authorization authorization = authorizationService.createNewAuthorization(type);

    authorization.setResource(resource);
    if (resourceId != null) {
      authorization.setResourceId(resourceId);
    }

    return authorization;
  }

  protected void saveAuthorization(Authorization authorization) {
    authorizationService.saveAuthorization(authorization);
  }

  // enable/disable authorization //////////////////////////////////////////////

  protected void enableAuthorization() {
    processEngineConfiguration.setAuthorizationEnabled(true);
  }

  protected void disableAuthorization() {
    processEngineConfiguration.setAuthorizationEnabled(false);
  }

  // actions (executed without authorization) ///////////////////////////////////

  protected ProcessDefinition selectProcessDefinitionByKey(String processDefinitionKey) {
    disableAuthorization();
    ProcessDefinition definition = repositoryService
        .createProcessDefinitionQuery()
        .processDefinitionKey(processDefinitionKey)
        .singleResult();
    enableAuthorization();
    return definition;
  }

  protected ProcessInstance selectAnyProcessInstanceByKey(String processDefinitionKey) {
    disableAuthorization();
    ProcessInstance instance = runtimeService.createProcessInstanceQuery().processDefinitionKey(processDefinitionKey).listPage(0, 1).get(0);
    enableAuthorization();
    return instance;
  }

  protected void startProcessInstances(String processDefinitionKey, int numOfInstances) {
    disableAuthorization();
    for (int i = 0; i < numOfInstances; i++) {
      runtimeService.startProcessInstanceByKey(processDefinitionKey, "businessKey_" + i);
    }
    enableAuthorization();
  }

  protected Deployment createDeployment(String name, String... resources) {
    disableAuthorization();
    DeploymentBuilder builder = repositoryService.createDeployment();
    for (String resource : resources) {
      builder.addClasspathResource(resource);
    }
    Deployment deployment = builder.deploy();
    enableAuthorization();
    return deployment;
  }

  protected void deleteDeployment(String deploymentId) {
    disableAuthorization();
    repositoryService.deleteDeployment(deploymentId, true);
    enableAuthorization();
  }

}
