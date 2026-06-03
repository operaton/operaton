/*
 * Copyright 2026 the Operaton contributors.
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

import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.authorization.ProcessDefinitionPermissions;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.authorization.Resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResourceTypeUtilTest {

  // --- resourceIsContainedInArray ---

  @Test
  void resourceIsContainedInArray_shouldReturnTrueWhenResourcePresent() {
    Resource[] resources = { Resources.PROCESS_DEFINITION, Resources.TASK };
    assertThat(ResourceTypeUtil.resourceIsContainedInArray(
        Resources.PROCESS_DEFINITION.resourceType(), resources)).isTrue();
  }

  @Test
  void resourceIsContainedInArray_shouldReturnFalseWhenResourceAbsent() {
    Resource[] resources = { Resources.PROCESS_DEFINITION, Resources.TASK };
    assertThat(ResourceTypeUtil.resourceIsContainedInArray(
        Resources.BATCH.resourceType(), resources)).isFalse();
  }

  // --- getPermissionsByResourceType ---

  @Test
  void getPermissionsByResourceType_shouldReturnSpecificPermissionsForKnownType() {
    Permission[] permissions = ResourceTypeUtil.getPermissionsByResourceType(
        Resources.PROCESS_DEFINITION.resourceType());
    assertThat(permissions).isEqualTo(ProcessDefinitionPermissions.values());
  }

  @Test
  void getPermissionsByResourceType_shouldFallBackToDefaultPermissionsForUnknownType() {
    Permission[] permissions = ResourceTypeUtil.getPermissionsByResourceType(-999);
    assertThat(permissions).isEqualTo(Permissions.values());
  }

  // --- getPermissionByNameAndResourceType ---

  @Test
  void getPermissionByNameAndResourceType_shouldReturnMatchingPermission() {
    Permission perm = ResourceTypeUtil.getPermissionByNameAndResourceType(
        "READ", Resources.PROCESS_DEFINITION.resourceType());
    assertThat(perm.getName()).isEqualTo("READ");
  }

  @Test
  void getPermissionByNameAndResourceType_shouldThrowForInvalidPermissionName() {
    int processDefinitionResourceType = Resources.PROCESS_DEFINITION.resourceType();
    assertThatThrownBy(() -> ResourceTypeUtil.getPermissionByNameAndResourceType(
        "NONEXISTENT_PERMISSION", processDefinitionResourceType))
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("NONEXISTENT_PERMISSION");
  }

  // --- getResourceByType ---

  @Test
  void getResourceByType_shouldReturnResourceForValidType() {
    Resource resource = ResourceTypeUtil.getResourceByType(Resources.TASK.resourceType());
    assertThat(resource).isEqualTo(Resources.TASK);
  }

  @Test
  void getResourceByType_shouldReturnNullForUnknownType() {
    assertThat(ResourceTypeUtil.getResourceByType(-999)).isNull();
  }
}
