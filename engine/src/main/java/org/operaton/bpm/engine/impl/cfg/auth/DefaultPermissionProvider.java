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
package org.operaton.bpm.engine.impl.cfg.auth;

import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.impl.util.ResourceTypeUtil;

/**
 * Default implementation of {@link PermissionProvider}
 *
 * @author Yana.Vasileva
 * @author Tobias Metzke
 *
 */
public class DefaultPermissionProvider implements PermissionProvider {

  @Override
  public Permission getPermissionForName(String name, int resourceType) {
    return ResourceTypeUtil.getPermissionByNameAndResourceType(name, resourceType);
  }

  @Override
  public Permission[] getPermissionsForResource(int resourceType) {
    return ResourceTypeUtil.getPermissionsByResourceType(resourceType);
  }

  @Override
  public String getNameForResource(int resourceType) {
    Resource resourceByType = ResourceTypeUtil.getResourceByType(resourceType);
    return resourceByType == null ? null : resourceByType.resourceName();
  }

}
