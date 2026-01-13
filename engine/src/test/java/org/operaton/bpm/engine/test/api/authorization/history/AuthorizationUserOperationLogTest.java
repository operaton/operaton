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
package org.operaton.bpm.engine.test.api.authorization.history;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.bpm.engine.EntityTypes;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.history.UserOperationLogQuery;
import org.operaton.bpm.engine.impl.cfg.auth.DefaultPermissionProvider;
import org.operaton.bpm.engine.impl.cfg.auth.PermissionProvider;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.test.RequiredHistoryLevel;
import org.operaton.bpm.engine.test.api.authorization.AuthorizationTest;
import org.operaton.bpm.engine.test.api.identity.TestPermissions;
import org.operaton.bpm.engine.test.api.identity.TestResource;

import static org.operaton.bpm.engine.authorization.Resources.OPERATION_LOG_CATEGORY;
import static org.operaton.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.operaton.bpm.engine.authorization.UserOperationLogCategoryPermissions.READ;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.CATEGORY_ADMIN;
import static org.operaton.bpm.engine.history.UserOperationLogEntry.CATEGORY_OPERATOR;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Tobias Metzke
 *
 */
@RequiredHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL)
class AuthorizationUserOperationLogTest extends AuthorizationTest {

  @ParameterizedTest
  @MethodSource("authorizationCreationParameters")
  void testLogCreatedOnAuthorizationCreation(String queryProperty, String expectedNewValue) {
    // given
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_ADMIN, userId, READ);
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    assertThat(query.count()).isZero();

    // when
    createGrantAuthorizationGroup(PROCESS_DEFINITION, Authorization.ANY, "testGroupId", ProcessDefinitionPermissions.DELETE);

    // then
    assertThat(query.count()).isEqualTo(6);

    UserOperationLogEntry entry = query.property(queryProperty).singleResult();
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_ADMIN);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.AUTHORIZATION);
    assertThat(entry.getNewValue()).isEqualTo(expectedNewValue);
  }

  static Stream<Arguments> authorizationCreationParameters() {
    return Stream.of(
      Arguments.of("permissionBits", String.valueOf(ProcessDefinitionPermissions.DELETE.getValue())),
      Arguments.of("permissions", ProcessDefinitionPermissions.DELETE.getName()),
      Arguments.of("type", String.valueOf(Authorization.AUTH_TYPE_GRANT)),
      Arguments.of("resource", Resources.PROCESS_DEFINITION.resourceName()),
      Arguments.of("resourceId", Authorization.ANY),
      Arguments.of("groupId", "testGroupId")
    );
  }

  @ParameterizedTest
  @MethodSource("authorizationUpdateParameters")
  void testLogCreatedOnAuthorizationUpdate(String queryProperty, String expectedNewValue, String expectedOrgValue) {
    // given
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    Authorization authorization = createGrantAuthorizationWithoutAuthentication(Resources.PROCESS_DEFINITION, Authorization.ANY, "testUserId",
      Permissions.DELETE);
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_ADMIN, userId, READ);
    assertThat(query.count()).isZero();

    // when
    authorization.addPermission(Permissions.READ);
    authorization.setResource(Resources.PROCESS_INSTANCE);
    authorization.setResourceId("abc123");
    authorization.setGroupId("testGroupId");
    authorization.setUserId(null);
    saveAuthorization(authorization);

    // then
    assertThat(query.count()).isEqualTo(7);

    UserOperationLogEntry entry = query.property(queryProperty).singleResult();
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_UPDATE);
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_ADMIN);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.AUTHORIZATION);

    if (expectedNewValue == null) {
      assertThat(entry.getNewValue()).isNull();
    } else {
      assertThat(entry.getNewValue()).isEqualTo(expectedNewValue);
    }

    if (expectedOrgValue == null) {
      assertThat(entry.getOrgValue()).isNull();
    } else {
      assertThat(entry.getOrgValue()).isEqualTo(expectedOrgValue);
    }
  }

  static Stream<Arguments> authorizationUpdateParameters() {
    return Stream.of(
      Arguments.of("permissionBits", String.valueOf(Permissions.DELETE.getValue() | Permissions.READ.getValue()), String.valueOf(Permissions.DELETE.getValue())),
      Arguments.of("permissions", Permissions.READ.getName() + ", " + Permissions.DELETE.getName(), Permissions.DELETE.getName()),
      Arguments.of("type", String.valueOf(Authorization.AUTH_TYPE_GRANT), String.valueOf(Authorization.AUTH_TYPE_GRANT)),
      Arguments.of("resource", Resources.PROCESS_INSTANCE.resourceName(), Resources.PROCESS_DEFINITION.resourceName()),
      Arguments.of("resourceId", "abc123", Authorization.ANY),
      Arguments.of("userId", null, "testUserId"),
      Arguments.of("groupId", "testGroupId", null)
    );
  }

  @ParameterizedTest
  @MethodSource("authorizationDeletionParameters")
  void testLogCreatedOnAuthorizationDeletion(String queryProperty, String expectedNewValue) {
    // given
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    Authorization authorization = createGrantAuthorizationWithoutAuthentication(Resources.PROCESS_DEFINITION, Authorization.ANY, "testUserId",
      ProcessDefinitionPermissions.DELETE);
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_ADMIN, userId, READ);
    assertThat(query.count()).isZero();

    // when
    authorizationService.deleteAuthorization(authorization.getId());

    // then
    assertThat(query.count()).isEqualTo(6);

    UserOperationLogEntry entry = query.property(queryProperty).singleResult();
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_DELETE);
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_ADMIN);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.AUTHORIZATION);
    assertThat(entry.getNewValue()).isEqualTo(expectedNewValue);
  }

  static Stream<Arguments> authorizationDeletionParameters() {
    return Stream.of(
      Arguments.of("permissionBits", String.valueOf(ProcessDefinitionPermissions.DELETE.getValue())),
      Arguments.of("permissions", ProcessDefinitionPermissions.DELETE.getName()),
      Arguments.of("type", String.valueOf(Authorization.AUTH_TYPE_GRANT)),
      Arguments.of("resource", Resources.PROCESS_DEFINITION.resourceName()),
      Arguments.of("resourceId", Authorization.ANY),
      Arguments.of("userId", "testUserId")
    );
  }

  @Test
  void testLogCreatedOnAuthorizationCreationWithExceedingPermissionStringList() {
    // given
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_ADMIN, userId, READ);
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    assertThat(query.count()).isZero();

    // when
    PermissionProvider permissionProvider = processEngineConfiguration.getPermissionProvider();
    processEngineConfiguration.setPermissionProvider(new TestPermissionProvider());
    createGrantAuthorizationGroup(TestResource.RESOURCE1, Authorization.ANY, "testGroupId", TestPermissions.LONG_NAME);
    processEngineConfiguration.setPermissionProvider(permissionProvider);

    // then
    assertThat(query.count()).isEqualTo(6);

    UserOperationLogEntry entry = query.property("permissions").singleResult();
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_ADMIN);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.AUTHORIZATION);
    assertThat(entry.getNewValue()).isEqualTo(TestPermissions.LONG_NAME.getName().substring(0, StringUtil.DB_MAX_STRING_LENGTH));
  }

  @Test
  void testLogCreatedOnAuthorizationCreationWithAllPermission() {
    // given
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_ADMIN, userId, READ);
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    assertThat(query.count()).isZero();

    // when
    PermissionProvider permissionProvider = processEngineConfiguration.getPermissionProvider();
    processEngineConfiguration.setPermissionProvider(new TestPermissionProvider());
    createGrantAuthorizationGroup(TestResource.RESOURCE1, Authorization.ANY, "testGroupId", TestPermissions.ALL);
    processEngineConfiguration.setPermissionProvider(permissionProvider);

    // then
    assertThat(query.count()).isEqualTo(6);

    UserOperationLogEntry entry = query.property("permissions").singleResult();
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_ADMIN);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.AUTHORIZATION);
    assertThat(entry.getNewValue()).isEqualTo(TestPermissions.ALL.getName());
  }

  @Test
  void testLogCreatedOnAuthorizationCreationWithNonePermission() {
    // given
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_ADMIN, userId, READ);
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    assertThat(query.count()).isZero();

    // when
    PermissionProvider permissionProvider = processEngineConfiguration.getPermissionProvider();
    processEngineConfiguration.setPermissionProvider(new TestPermissionProvider());
    createGrantAuthorizationGroup(TestResource.RESOURCE1, Authorization.ANY, "testGroupId", TestPermissions.NONE);
    processEngineConfiguration.setPermissionProvider(permissionProvider);

    // then
    assertThat(query.count()).isEqualTo(6);

    UserOperationLogEntry entry = query.property("permissions").singleResult();
    assertThat(entry.getOperationType()).isEqualTo(UserOperationLogEntry.OPERATION_TYPE_CREATE);
    assertThat(entry.getCategory()).isEqualTo(UserOperationLogEntry.CATEGORY_ADMIN);
    assertThat(entry.getEntityType()).isEqualTo(EntityTypes.AUTHORIZATION);
    assertThat(entry.getNewValue()).isEqualTo(TestPermissions.NONE.getName());
  }

  @Test
  void testLogCreatedOnAuthorizationCreationWithoutAuthorization() {
    // given
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    assertThat(query.count()).isZero();

    // when
    createGrantAuthorizationGroup(PROCESS_DEFINITION, Authorization.ANY, "testGroupId", ProcessDefinitionPermissions.DELETE);

    // then the user is not authorised
    assertThat(query.count()).isZero();
  }

  @Test
  void testLogCreatedOnAuthorizationCreationWithReadPermissionOnAnyCategoryPermission() {
    // given
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, Authorization.ANY, userId, READ);
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    assertThat(query.count()).isZero();

    // when
    createGrantAuthorizationGroup(PROCESS_DEFINITION, Authorization.ANY, "testGroupId", ProcessDefinitionPermissions.DELETE);

    // then the user is authorised
    assertThat(query.count()).isEqualTo(6);
  }

  @Test
  void testLogCreatedOnAuthorizationCreationWithReadPermissionOnWrongCategory() {
    // given
    createGrantAuthorizationWithoutAuthentication(OPERATION_LOG_CATEGORY, CATEGORY_OPERATOR, userId, READ);
    UserOperationLogQuery query = historyService.createUserOperationLogQuery();
    assertThat(query.count()).isZero();

    // when
    createGrantAuthorizationGroup(PROCESS_DEFINITION, Authorization.ANY, "testGroupId", ProcessDefinitionPermissions.DELETE);

    // then the user is not authorised
    assertThat(query.count()).isZero();
  }

  public static class TestPermissionProvider extends DefaultPermissionProvider {
    @Override
    public String getNameForResource(int resourceType) {
      for (Resource resource : TestResource.values()) {
        if (resourceType == resource.resourceType()) {
          return resource.resourceName();
        }
      }
      return null;
    }

    @Override
    public Permission[] getPermissionsForResource(int resourceType) {
      return TestPermissions.values();
    }
  }
}
