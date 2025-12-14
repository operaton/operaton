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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.cfg.auth.DefaultPermissionProvider;

@ExtendWith(MockitoExtension.class)
class PermissionConverterTest {

  private static final String READ = "READ";
  private static final String UPDATE = "UPDATE";
  private static final String DELETE = "DELETE";
  private static final String CREATE = "CREATE";
  private static final String ALL = "ALL";

  @Mock
  private ProcessEngineConfigurationImpl processEngineConfiguration;

  @Mock
  private Authorization authorization;

  DefaultPermissionProvider permissionProvider;

  @BeforeEach
  void setUp() {
    permissionProvider = new DefaultPermissionProvider();
    lenient().when(processEngineConfiguration.getPermissionProvider()).thenReturn(permissionProvider);
  }

  @Test
  void shouldConvertEmptyPermissionArray() {
    // given
    String[] names = {};

    // when
    Permission[] permissions = PermissionConverter.getPermissionsForNames(names,
        Resources.PROCESS_DEFINITION.resourceType(), processEngineConfiguration);

    // then
    assertThat(permissions).isEmpty();
  }

  @Test
  void shouldConvertSinglePermissionName() {
    // given
    String[] names = { READ };
    int resourceType = Resources.PROCESS_DEFINITION.resourceType();

    // when
    Permission[] permissions = PermissionConverter.getPermissionsForNames(names, resourceType,
        processEngineConfiguration);

    // then
    Stream<String> permissionNames = Arrays.stream(permissions).map(Permission::getName);
    assertThat(permissionNames).containsExactly(names);
  }

  @Test
  void shouldConvertMultiplePermissionNames() {
    // given
    String[] names = { READ, UPDATE, DELETE };
    int resourceType = Resources.PROCESS_DEFINITION.resourceType();

    // when
    Permission[] permissions = PermissionConverter.getPermissionsForNames(names, resourceType,
        processEngineConfiguration);

    // then
    Stream<String> permissionNames = Arrays.stream(permissions).map(Permission::getName);
    assertThat(permissionNames).containsExactly(names);
  }

  @Test
  void shouldThrowExceptionForUnknownPermissionName() {
    // given
    String[] names = { "read" };
    int resourceType = Resources.PROCESS_DEFINITION.resourceType();

    // when
    assertThatThrownBy(() -> PermissionConverter.getPermissionsForNames(names, resourceType,
        processEngineConfiguration))
        .isInstanceOf(BadUserRequestException.class)
        .hasMessageContaining(String.format("The permission '%s' is not valid for '%s' resource type.", names[0],
            Resources.PROCESS_DEFINITION.name()));
  }

  @Test
  void shouldConvertDuplicatePermissionNamesAndDeduplicate() {
    // given
    String[] names = { READ, UPDATE, READ };
    int resourceType = Resources.PROCESS_DEFINITION.resourceType();

    // when
    Permission[] permissions = PermissionConverter.getPermissionsForNames(names, resourceType,
        processEngineConfiguration);

    // then
    Stream<String> permissionNames = Arrays.stream(permissions).map(Permission::getName);
    assertThat(permissionNames).containsExactly(READ, UPDATE);
  }

  @Test
  void shouldHandlePermissionsForResourceTypeNotInPermissionEnums() {
    // given
    String[] names = { CREATE };
    int resourceType = Resources.APPLICATION.resourceType();

    // when
    Permission[] permissions = PermissionConverter.getPermissionsForNames(names, resourceType,
        processEngineConfiguration);

    // then
    Stream<String> permissionNames = Arrays.stream(permissions).map(Permission::getName);
    assertThat(permissionNames).containsExactly(names);
  }

  @ParameterizedTest
  @ValueSource(ints = { Authorization.AUTH_TYPE_GLOBAL, Authorization.AUTH_TYPE_GRANT })
  void shouldReturnAllWithForGivenAuthorizationTypeWhenAllPermissionsGranted(int authorizationType) {
    // given
    when(authorization.getAuthorizationType()).thenReturn(authorizationType);
    when(authorization.isEveryPermissionGranted()).thenReturn(true);
    Permission[] permissions = { Permissions.READ, Permissions.UPDATE };

    // when
    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    // then
    assertThat(names).containsExactly(ALL);
  }

  @Test
  void shouldReturnAllForRevokeAuthorizationWithAllPermissionsRevoked() {
    // given
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_REVOKE);
    when(authorization.isEveryPermissionRevoked()).thenReturn(true);
    Permission[] permissions = { Permissions.READ, Permissions.UPDATE };

    // when
    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    // then
    assertThat(names).containsExactly(ALL);
  }

  @Test
  void shouldConvertSpecificPermissionsToNames() {
    // given
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.READ, Permissions.UPDATE, Permissions.DELETE };

    // when
    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    // then
    assertThat(names).containsExactly(READ, UPDATE, DELETE);
  }

  @Test
  void shouldFilterOutNonePermission() {
    // given
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.NONE, Permissions.READ, Permissions.UPDATE };

    // when
    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    // then
    assertThat(names).containsExactly(READ, UPDATE);
  }

  @Test
  void shouldFilterOutAllPermission() {
    // given
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.ALL, Permissions.READ, Permissions.UPDATE };

    // when
    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    // then
    assertThat(names).containsExactly(READ, UPDATE);
  }

  @Test
  void shouldFilterOutBothNoneAndAllPermissions() {
    // given
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.NONE, Permissions.READ, Permissions.ALL, Permissions.UPDATE };

    // when
    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    // then
    assertThat(names).containsExactly(READ, UPDATE);
  }

  @Test
  void shouldReturnEmptyArrayWhenOnlyNoneAndAllPermissions() {
    // given
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.NONE, Permissions.ALL };

    // when
    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    // then
    assertThat(names).isEmpty();
  }

  @Test
  void shouldHandleEmptyPermissionsArray() {
    // given
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = {};

    // when
    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    // then
    assertThat(names).isEmpty();
  }

  @Test
  void shouldHandleRevokeAuthorizationWithSpecificPermissions() {
    // given
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_REVOKE);
    when(authorization.isEveryPermissionRevoked()).thenReturn(false);
    Permission[] permissions = { Permissions.READ, Permissions.DELETE };

    // when
    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    // then
    assertThat(names).containsExactly(READ, DELETE);
  }

  @Test
  void shouldHandleSinglePermission() {
    // given
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.CREATE };

    // when
    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    assertThat(names).containsExactly(CREATE);
  }

  @Test
  void shouldMaintainOrderOfPermissions() {
    // given
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.CREATE, Permissions.READ, Permissions.UPDATE, Permissions.DELETE };

    // when
    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    // then
    assertThat(names).containsExactly(CREATE, READ, UPDATE, DELETE);
  }

  @Test
  void shouldConvertDuplicatePermissionsToNamesAndDeduplicate() {
    // given
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    Permission[] permissions = { Permissions.READ, Permissions.UPDATE, Permissions.READ };

    // when
    String[] names = PermissionConverter.getNamesForPermissions(authorization, permissions);

    // then
    assertThat(names).containsExactly(READ, UPDATE);
  }

  @Test
  void shouldRoundTripConversion() {
    // given
    String[] originalNames = { READ, UPDATE, DELETE };
    int resourceType = Resources.PROCESS_DEFINITION.resourceType();

    // when
    Permission[] permissions = PermissionConverter.getPermissionsForNames(originalNames, resourceType,
        processEngineConfiguration);
    when(authorization.getAuthorizationType()).thenReturn(Authorization.AUTH_TYPE_GRANT);
    when(authorization.isEveryPermissionGranted()).thenReturn(false);
    String[] convertedNames = PermissionConverter.getNamesForPermissions(authorization, permissions);

    // then
    assertThat(convertedNames).containsExactlyInAnyOrder(originalNames);
  }
}
