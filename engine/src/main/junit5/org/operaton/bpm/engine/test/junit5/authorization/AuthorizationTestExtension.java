/*
 * Copyright 2024 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.junit5.authorization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationExceptionInterceptor;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenarioInstance;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit 5 extension that replaces the legacy JUnit 4 AuthorizationTestRule.
 * <p>
 * This extension provides an API for starting an authorization scenario,
 * asserting its outcome, and managing authorizations, users, and groups.
 * </p>
 *
 * <p>Usage Example:</p>
 * <pre>
 * &#64;RegisterExtension
 * static ProcessEngineExtension engineExtension = ProcessEngineExtension.builder().build();
 *
 * <p>
 * &#64;RegisterExtension
 * AuthorizationTestExtension authExtension = new AuthorizationTestExtension(engineExtension);
 * </p>
 *
 * <p>
 * // In a test method:
 * &#64;Test
 * public void testSomething() {
 *   authExtension.createGrantAuthorization(Resources.AUTHORIZATION, "*", "testUser", Permissions.CREATE);
 *   authExtension.start(myScenario);
 *   // ... perform operations ...
 *   assertTrue(authExtension.assertScenario(myScenario));
 * }
 * </pre>
 * </p>
 */
public class AuthorizationTestExtension implements BeforeEachCallback, AfterEachCallback {

  private final ProcessEngineExtension processEngineExtension;

  private final AuthorizationExceptionInterceptor interceptor;
  private AuthorizationScenarioInstance scenarioInstance;

  private final List<Authorization> managedAuthorizations = new ArrayList<>();
  private final List<User> managedUsers = new ArrayList<>();
  private final List<Group> managedGroups = new ArrayList<>();

  /**
   * Constructs the extension with the given ProcessEngine.
   *
   * @param processEngineExtension extension from which to get process engine instance to use
   */
  public AuthorizationTestExtension(ProcessEngineExtension processEngineExtension) {
    this.processEngineExtension = processEngineExtension;
    this.interceptor = new AuthorizationExceptionInterceptor();
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    ProcessEngineConfigurationImpl engineConfiguration =
        (ProcessEngineConfigurationImpl) processEngineExtension.getProcessEngine().getProcessEngineConfiguration();
    interceptor.reset();
    engineConfiguration.getCommandInterceptorsTxRequired().get(0).setNext(interceptor);
    interceptor.setNext(engineConfiguration.getCommandInterceptorsTxRequired().get(1));
  }

  @Override
  public void afterEach(ExtensionContext context) {
    processEngineExtension.getProcessEngine().getIdentityService().clearAuthentication();
    deleteManagedAuthorizations();
    ProcessEngineConfigurationImpl engineConfiguration =
        (ProcessEngineConfigurationImpl) processEngineExtension.getProcessEngine().getProcessEngineConfiguration();
    engineConfiguration.getCommandInterceptorsTxRequired().get(0).setNext(interceptor.getNext());
    interceptor.setNext(null);
    assertThat(managedUsers).as("Users have been created but not deleted").isEmpty();
    assertThat(managedGroups).as("Groups have been created but not deleted").isEmpty();
  }

  /**
   * Enables authorization and (optionally) sets the authenticated user.
   *
   * @param userId the user ID to authenticate (can be null)
   */
  public void enableAuthorization(String userId) {
    processEngineExtension.getProcessEngine().getProcessEngineConfiguration().setAuthorizationEnabled(true);
    if (userId != null) {
      processEngineExtension.getProcessEngine().getIdentityService().setAuthenticatedUserId(userId);
    }
  }

  /**
   * Disables authorization and clears authentication.
   */
  public void disableAuthorization() {
    processEngineExtension.getProcessEngine().getProcessEngineConfiguration().setAuthorizationEnabled(false);
    processEngineExtension.getProcessEngine().getIdentityService().clearAuthentication();
  }

  /**
   * Starts an authorization scenario with default resource bindings.
   *
   * @param scenario the authorization scenario to start.
   */
  public void start(AuthorizationScenario scenario) {
    start(scenario, null, new HashMap<>());
  }

  /**
   * Starts an authorization scenario with the given user and resource bindings.
   *
   * @param scenario the authorization scenario to start.
   * @param userId the user ID to set as authenticated.
   * @param resourceBindings the resource bindings for the scenario.
   */
  public void start(AuthorizationScenario scenario, String userId, Map<String, String> resourceBindings) {
    assertThat(interceptor.getLastException()).isNull();
    scenarioInstance = new AuthorizationScenarioInstance(
        scenario,
        processEngineExtension.getProcessEngine().getAuthorizationService(),
        resourceBindings);
    enableAuthorization(userId);
    interceptor.activate();
  }

  /**
   * Asserts that the given authorization scenario has executed as expected.
   *
   * @param scenario the scenario to assert.
   * @return true if no exception was thrown (scenario succeeded), false otherwise.
   */
  public boolean assertScenario(AuthorizationScenario scenario) {
    interceptor.deactivate();
    disableAuthorization();
    scenarioInstance.tearDown(processEngineExtension.getProcessEngine().getAuthorizationService());
    scenarioInstance.assertAuthorizationException(interceptor.getLastException());
    scenarioInstance = null;
    return scenarioSucceeded();
  }

  /**
   * Returns true if the authorization scenario succeeded (no exception was thrown).
   */
  public boolean scenarioSucceeded() {
    return interceptor.getLastException() == null;
  }

  /**
   * Returns true if the authorization scenario failed (an exception was thrown).
   */
  public boolean scenarioFailed() {
    return interceptor.getLastException() != null;
  }

  /**
   * Creates a grant authorization.
   *
   * @param resource the resource type.
   * @param resourceId the resource id (can be null).
   * @param userId the user id.
   * @param permissions the permissions to grant.
   */
  public void createGrantAuthorization(Resource resource, String resourceId, String userId, Permission... permissions) {
    Authorization authorization = createAuthorization(Authorization.AUTH_TYPE_GRANT, resource, resourceId);
    authorization.setUserId(userId);
    for (Permission permission : permissions) {
      authorization.addPermission(permission);
    }
    processEngineExtension.getProcessEngine().getAuthorizationService().saveAuthorization(authorization);
    manageAuthorization(authorization);
  }

  /**
   * Creates a revoke authorization.
   *
   * @param resource the resource type.
   * @param resourceId the resource id (can be null).
   * @param userId the user id.
   * @param permissions the permissions to revoke.
   */
  public void createRevokeAuthorization(Resource resource, String resourceId, String userId, Permission... permissions) {
    Authorization authorization = createAuthorization(Authorization.AUTH_TYPE_REVOKE, resource, resourceId);
    authorization.setUserId(userId);
    for (Permission permission : permissions) {
      authorization.removePermission(permission);
    }
    processEngineExtension.getProcessEngine().getAuthorizationService().saveAuthorization(authorization);
    manageAuthorization(authorization);
  }

  /**
   * Helper method to create a new authorization.
   *
   * @param type the type of authorization (grant or revoke).
   * @param resource the resource type.
   * @param resourceId the resource id (can be null).
   * @return the new authorization.
   */
  protected Authorization createAuthorization(int type, Resource resource, String resourceId) {
    Authorization authorization = processEngineExtension.getProcessEngine().getAuthorizationService().createNewAuthorization(type);
    authorization.setResource(resource);
    if (resourceId != null) {
      authorization.setResourceId(resourceId);
    }
    return authorization;
  }

  /**
   * Registers an authorization for cleanup.
   *
   * @param authorization the authorization to manage.
   */
  public void manageAuthorization(Authorization authorization) {
    managedAuthorizations.add(authorization);
  }

  /**
   * Creates a new user and group and registers them for cleanup.
   *
   * @param userId the user id.
   * @param groupId the group id.
   */
  public void createUserAndGroup(String userId, String groupId) {
    IdentityService identityService = processEngineExtension.getProcessEngine().getIdentityService();
    User user = identityService.newUser(userId);
    identityService.saveUser(user);
    managedUsers.add(user);

    Group group = identityService.newGroup(groupId);
    identityService.saveGroup(group);
    managedGroups.add(group);
  }

  /**
   * Deletes all managed authorizations.
   */
  public void deleteManagedAuthorizations() {
    for (Authorization authorization : managedAuthorizations) {
      processEngineExtension.getProcessEngine().getAuthorizationService().deleteAuthorization(authorization.getId());
    }
    managedAuthorizations.clear();
  }


  public void deleteUsersAndGroups() {
    for (User user : managedUsers) {
      processEngineExtension.getProcessEngine().getIdentityService().deleteUser(user.getId());
    }
    managedUsers.clear();

    for (Group group : managedGroups) {
      processEngineExtension.getProcessEngine().getIdentityService().deleteGroup(group.getId());
    }
    managedGroups.clear();
  }

  public static Collection<AuthorizationScenario[]> asParameters(AuthorizationScenario... scenarios) {
    List<AuthorizationScenario[]> scenarioList = new ArrayList<>();
    for (AuthorizationScenario scenario : scenarios) {
      scenarioList.add(new AuthorizationScenario[]{ scenario });
    }

    return scenarioList;
  }

  /**
   * Returns a builder for creating an authorization scenario instance.
   *
   * @param scenario the authorization scenario.
   * @return the builder.
   */
  public AuthorizationScenarioInstanceBuilder init(AuthorizationScenario scenario) {
    return new AuthorizationScenarioInstanceBuilder(this, scenario);
  }

  /**
   * Builder for fluent construction of an authorization scenario instance.
   */
  public static class AuthorizationScenarioInstanceBuilder {
    private final AuthorizationTestExtension extension;
    private final AuthorizationScenario scenario;
    private String userId;
    private final Map<String, String> resourceBindings = new HashMap<>();

    public AuthorizationScenarioInstanceBuilder(AuthorizationTestExtension extension, AuthorizationScenario scenario) {
      this.extension = extension;
      this.scenario = scenario;
    }

    public AuthorizationScenarioInstanceBuilder withUser(String userId) {
      this.userId = userId;
      return this;
    }

    public AuthorizationScenarioInstanceBuilder bindResource(String key, String value) {
      resourceBindings.put(key, value);
      return this;
    }

    public void start() {
      extension.start(scenario, userId, resourceBindings);
    }
  }
}
