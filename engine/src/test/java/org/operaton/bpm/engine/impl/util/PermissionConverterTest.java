/*
 * Copyright 2025 the Operaton contributors.
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
package org.operaton.bpm.engine.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.auth.DefaultPermissionProvider;

@ExtendWith(MockitoExtension.class)
class PermissionConverterTest {

  @Mock
  private ProcessEngineConfigurationImpl processEngineConfiguration;

  @Mock
  private Authorization authorization;

  @Test
  void shouldConvertEmptyPermissionArray() {
    Permission[] permissions = PermissionConverter.getPermissionsForNames(new String[] {},
        Resources.PROCESS_DEFINITION.resourceType(), processEngineConfiguration);

    assertThat(permissions).isEmpty();
  }

  @Test
  void shouldConvertSinglePermissionName() {
    DefaultPermissionProvider permissionProvider = new DefaultPermissionProvider();
    when(processEngineConfiguration.getPermissionProvider()).thenReturn(permissionProvider);
    String[] names = { "READ" };
    int resourceType = Resources.PROCESS_DEFINITION.resourceType();

    Permission[] permissions = PermissionConverter.getPermissionsForNames(names, resourceType,
        processEngineConfiguration);

    Stream<String> permissionNames = Arrays.stream(permissions).map(Permission::getName);
    assertThat(permissionNames).containsExactly(names);
  }

  @Test
  void shouldConvertMultiplePermissionNames() {
    DefaultPermissionProvider permissionProvider = new DefaultPermissionProvider();
    when(processEngineConfiguration.getPermissionProvider()).thenReturn(permissionProvider);
    String[] names = { "READ", "UPDATE", "DELETE" };
    int resourceType = Resources.PROCESS_DEFINITION.resourceType();

    Permission[] permissions = PermissionConverter.getPermissionsForNames(names, resourceType,
        processEngineConfiguration);

    Stream<String> permissionNames = Arrays.stream(permissions).map(Permission::getName);
    assertThat(permissionNames).containsExactly(names);
  }

  @Test
  void shouldThrowExceptionForUnknownPermissionName() {
    DefaultPermissionProvider permissionProvider = new DefaultPermissionProvider();
    when(processEngineConfiguration.getPermissionProvider()).thenReturn(permissionProvider);
    String[] names = { "read" };
    int resourceType = Resources.PROCESS_DEFINITION.resourceType();

    Exception exception = assertThrows(Exception.class,
        () -> PermissionConverter.getPermissionsForNames(names, resourceType, processEngineConfiguration));

    String expectedMessage = String.format("The permission '%s' is not valid for '%s' resource type.", names[0],
        Resources.PROCESS_DEFINITION.name());
    assertThat(exception).hasMessageContaining(expectedMessage);
  }

  @Test
  void shouldConvertDuplicatePermissionNames() {
    DefaultPermissionProvider permissionProvider = new DefaultPermissionProvider();
    when(processEngineConfiguration.getPermissionProvider()).thenReturn(permissionProvider);
    String[] names = { "READ", "UPDATE", "READ" };
    int resourceType = Resources.PROCESS_DEFINITION.resourceType();

    Permission[] permissions = PermissionConverter.getPermissionsForNames(names, resourceType,
        processEngineConfiguration);

    Stream<String> permissionNames = Arrays.stream(permissions).map(Permission::getName);
    assertThat(permissionNames).containsExactly(names);
  }

  @Test
  void shouldHandlePermissionsForResourceTypeNotInPermissionEnums() {
    DefaultPermissionProvider permissionProvider = new DefaultPermissionProvider();
    when(processEngineConfiguration.getPermissionProvider()).thenReturn(permissionProvider);
    String[] names = { "CREATE" };
    int resourceType = Resources.APPLICATION.resourceType();

    Permission[] permissions = PermissionConverter.getPermissionsForNames(names, resourceType,
        processEngineConfiguration);

    Stream<String> permissionNames = Arrays.stream(permissions).map(Permission::getName);
    assertThat(permissionNames).containsExactly(names);
  }

  @Test
  void shouldReturnAllForGlobalAuthorizationWithAllPermissionsGranted() {
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GLOBAL);
    when(authorization.isEveryPermissionGranted()).thenReturn(true);
    Permission[] permissions = { Permissions.READ, Permissions.UPDATE };

    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).containsExactly("ALL");
  }

  @Test
  void shouldReturnAllForGrantAuthorizationWithAllPermissionsGranted() {
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(true);
    Permission[] permissions = { Permissions.READ, Permissions.UPDATE };

    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).containsExactly("ALL");
  }

  @Test
  void shouldReturnAllForRevokeAuthorizationWithAllPermissionsRevoked() {
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_REVOKE);
    when(authorization.isEveryPermissionRevoked()).thenReturn(true);
    Permission[] permissions = { Permissions.READ, Permissions.UPDATE };

    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).containsExactly("ALL");
  }

  @Test
  void shouldConvertSpecificPermissionsToNames() {
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.READ, Permissions.UPDATE, Permissions.DELETE };

    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).containsExactly("READ", "UPDATE", "DELETE");
  }

  @Test
  void shouldFilterOutNonePermission() {
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.NONE, Permissions.READ, Permissions.UPDATE };

    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).containsExactly("READ", "UPDATE");
  }

  @Test
  void shouldFilterOutAllPermission() {
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.ALL, Permissions.READ, Permissions.UPDATE };

    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).containsExactly("READ", "UPDATE");
  }

  @Test
  void shouldFilterOutBothNoneAndAllPermissions() {
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.NONE, Permissions.READ, Permissions.ALL, Permissions.UPDATE };

    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).containsExactly("READ", "UPDATE");
  }

  @Test
  void shouldReturnEmptyArrayWhenOnlyNoneAndAllPermissions() {
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.NONE, Permissions.ALL };

    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).isEmpty();
  }

  @Test
  void shouldHandleEmptyPermissionsArray() {
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = {};

    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).isEmpty();
  }

  @Test
  void shouldHandleRevokeAuthorizationWithSpecificPermissions() {
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_REVOKE);
    when(authorization.isEveryPermissionRevoked()).thenReturn(false);
    Permission[] permissions = { Permissions.READ, Permissions.DELETE };

    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).containsExactly("READ", "DELETE");
  }

  @Test
  void shouldHandleSinglePermission() {
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.CREATE };

    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).containsExactly("CREATE");
  }

  @Test
  void shouldMaintainOrderOfPermissions() {
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.CREATE, Permissions.READ, Permissions.UPDATE, Permissions.DELETE };

    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).containsExactly("CREATE", "READ", "UPDATE", "DELETE");
  }

  @Test
  void shouldConvertDuplicatePermissionsToNames() {
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.READ, Permissions.UPDATE, Permissions.READ };

    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).containsExactly("READ", "UPDATE", "READ");
  }

  @Test
  void shouldRoundTripConversion() {
    DefaultPermissionProvider permissionProvider = new DefaultPermissionProvider();
    when(processEngineConfiguration.getPermissionProvider()).thenReturn(permissionProvider);
    String[] originalNames = { "READ", "UPDATE", "DELETE" };
    int resourceType = Resources.PROCESS_DEFINITION.resourceType();

    Permission[] permissions = PermissionConverter.getPermissionsForNames(originalNames, resourceType,
        processEngineConfiguration);
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    String[] convertedNames = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(convertedNames).containsExactlyInAnyOrder(originalNames);
  }
}
