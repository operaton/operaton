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
package org.operaton.bpm.engine.impl.util;

import java.util.Arrays;
import java.util.List;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Permissions;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * <p>
 * Converts between the String-Array based and the Integer-based representation
 * of permissions.
 * </p>
 *
 * @author Daniel Meyer
 * @author Tobias Metzke
 *
 */
public final class PermissionConverter {
  private PermissionConverter() {
  }

  public static Permission[] getPermissionsForNames(String[] names, int resourceType, ProcessEngineConfiguration engineConfiguration) {
    return Arrays.stream(names)
        .map(name -> ((ProcessEngineConfigurationImpl) engineConfiguration).getPermissionProvider()
            .getPermissionForName(name, resourceType))
        .distinct()
        .toArray(Permission[]::new);
  }

  public static String[] getNamesForPermissions(Authorization authorization, Permission[] permissions) {
    int type = authorization.getAuthorizationType();

    // special case all permissions are granted
    if ((type == Authorization.AUTH_TYPE_GLOBAL || type == Authorization.AUTH_TYPE_GRANT)
        && authorization.isEveryPermissionGranted()) {
      return new String[] { Permissions.ALL.getName() };
    }

    // special case all permissions are revoked
    if (type == Authorization.AUTH_TYPE_REVOKE && authorization.isEveryPermissionRevoked()) {
      return new String[] { Permissions.ALL.getName() };
    }

    List<String> permissionsToFilter = List.of(Permissions.NONE.getName(), Permissions.ALL.getName());
    return Arrays.stream(permissions)
        .map(Permission::getName)
        .filter(name -> !permissionsToFilter.contains(name))
        .distinct()
        .toArray(String[]::new);
  }
}
