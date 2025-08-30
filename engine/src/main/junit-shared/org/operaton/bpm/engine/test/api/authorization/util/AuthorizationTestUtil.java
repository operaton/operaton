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
package org.operaton.bpm.engine.test.api.authorization.util;

import java.util.HashMap;
import java.util.Map;

import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.MissingAuthorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.authorization.Resources;
import org.operaton.bpm.engine.impl.util.ResourceTypeUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Thorben Lindhauer
 *
 */
public class AuthorizationTestUtil {

  protected static Map<Integer, Resource> resourcesByType = new HashMap<>();

  static {
    for (Resource resource : Resources.values()) {
      resourcesByType.put(resource.resourceType(), resource);
    }
  }

  public static Resource getResourceByType(int type) {
    return resourcesByType.get(type);
  }

  /**
   * Checks if the info has the expected parameters.
   *
   * @param expectedPermissionName to use
   * @param expectedResourceName to use
   * @param expectedResourceId to use
   * @param info to check
   */
  public static void assertExceptionInfo(String expectedPermissionName, String expectedResourceName, String expectedResourceId,
      MissingAuthorization info) {
    assertThat(info.getViolatedPermissionName()).isEqualTo(expectedPermissionName);
    assertThat(info.getResourceType()).isEqualTo(expectedResourceName);
    assertThat(info.getResourceId()).isEqualTo(expectedResourceId);
  }

  /**
   * @return the set of permission for the given authorization
   */
  public static Permission[] getPermissions(Authorization authorization)
  {
    int resourceType = authorization.getResourceType();
    Permission[] permissionsByResourceType = ResourceTypeUtil.getPermissionsByResourceType(resourceType);

    return authorization.getPermissions(permissionsByResourceType);
  }

  private AuthorizationTestUtil() {
  }
}
