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
package org.operaton.bpm.engine.test.api.authorization.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.operaton.bpm.engine.AuthorizationException;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.MissingAuthorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resource;

/**
 * @author Thorben Lindhauer
 *
 */
public class AuthorizationScenarioInstance {

  protected AuthorizationScenario scenario;

  protected List<Authorization> createdAuthorizations = new ArrayList<>();
  protected List<Authorization> missingAuthorizations = new ArrayList<>();

  public AuthorizationScenarioInstance(AuthorizationScenario scenario, AuthorizationService authorizationService,
      Map<String, String> resourceBindings) {
    this.scenario = scenario;
    init(authorizationService, resourceBindings);
  }

  public void init(AuthorizationService authorizationService, Map<String, String> resourceBindings) {
    for (AuthorizationSpec authorizationSpec : scenario.getGivenAuthorizations()) {
      Authorization authorization = authorizationSpec.instantiate(authorizationService, resourceBindings);
      authorizationService.saveAuthorization(authorization);
      createdAuthorizations.add(authorization);
    }

    for (AuthorizationSpec authorizationSpec : scenario.getMissingAuthorizations()) {
      Authorization authorization = authorizationSpec.instantiate(authorizationService, resourceBindings);
      missingAuthorizations.add(authorization);
    }
  }

  public void tearDown(AuthorizationService authorizationService) {
    Set<String> activeAuthorizations = new HashSet<>();
    for (Authorization activeAuthorization : authorizationService.createAuthorizationQuery().list()) {
      activeAuthorizations.add(activeAuthorization.getId());
    }

    for (Authorization createdAuthorization : createdAuthorizations) {
      if (activeAuthorizations.contains(createdAuthorization.getId())) {
        authorizationService.deleteAuthorization(createdAuthorization.getId());
      }
    }
  }

  public void assertAuthorizationException(AuthorizationException e) {
    if (!missingAuthorizations.isEmpty() && e != null) {
      String message = e.getMessage();

      List<MissingAuthorization> actualMissingAuthorizations = getActualMissingAuthorizations(e);
      List<MissingAuthorization> expectedMissingAuthorizations = new ArrayList<>();
      for (Authorization authorization : missingAuthorizations) {
        expectedMissingAuthorizations.add(asMissingAuthorization(authorization));
      }

      assertThat(actualMissingAuthorizations).containsExactlyInAnyOrderElementsOf(expectedMissingAuthorizations);

      for (Authorization missingAuthorization : missingAuthorizations) {
        assertThat(message).contains(missingAuthorization.getUserId());
        assertThat(e.getUserId()).isEqualTo(missingAuthorization.getUserId());

        Permission[] permissions = AuthorizationTestUtil.getPermissions(missingAuthorization);
        for (Permission permission : permissions) {
          if (permission.getValue() != Permissions.NONE.getValue()) {
            assertThat(message).contains(permission.getName());
            break;
          }
        }

        if (!Authorization.ANY.equals(missingAuthorization.getResourceId())) {
          // missing ANY authorizations are not explicitly represented in the error message
          assertThat(message).contains(missingAuthorization.getResourceId());
        }

        Resource resource = AuthorizationTestUtil.getResourceByType(missingAuthorization.getResourceType());
        assertThat(message).contains(resource.resourceName());
      }
    }
    else if (missingAuthorizations.isEmpty() && e == null) {
      // nothing to do
    }
    else {
      if (e != null) {
        fail(describeScenarioFailure("Expected no authorization exception but got one: " + e.getMessage()));
      }
      else {
        fail(describeScenarioFailure("Expected failure due to missing authorizations but code under test was successful"));
      }
    }
  }

  protected static MissingAuthorization asMissingAuthorization(Authorization authorization) {
    String permissionName = null;
    String resourceId = null;
    String resourceName = null;

    Permission[] permissions = AuthorizationTestUtil.getPermissions(authorization);
    for (Permission permission : permissions) {
      if (permission.getValue() != Permissions.NONE.getValue()) {
        permissionName = permission.getName();
        break;
      }
    }

    resourceId = authorization.getResourceId();

    Resource resource = AuthorizationTestUtil.getResourceByType(authorization.getResourceType());
    resourceName = resource.resourceName();
    return new MissingAuthorization(permissionName, resourceName, resourceId);
  }

  protected String describeScenarioFailure(String message) {
    return message + "\n"
        + "\n"
        + "Scenario: \n"
        + scenario.toString();
  }

  protected List<MissingAuthorization> getActualMissingAuthorizations(AuthorizationException e) {
    List<MissingAuthorization> actualMissingAuthorizations = new ArrayList<>();
    for (MissingAuthorization missingAuthorization : e.getMissingAuthorizations()) {
      String violatedPermissionName = missingAuthorization.getViolatedPermissionName();
      String resourceType = missingAuthorization.getResourceType();
      String resourceId = missingAuthorization.getResourceId();
      if (resourceId == null) {
        // ANY resourceId authorization
        resourceId = "*";
      }
      actualMissingAuthorizations.add(new MissingAuthorization(violatedPermissionName, resourceType, resourceId));
    }
    return actualMissingAuthorizations;
  }
}
