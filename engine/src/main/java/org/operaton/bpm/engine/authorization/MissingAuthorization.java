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
package org.operaton.bpm.engine.authorization;

import java.util.Objects;

/**
 * Wrapper containing the missing authorization information. It contains the name of the violated permission,
 * the type of the resouce and the Id of the resource.
 *
 * @author Filip Hrisafov
 */
public class MissingAuthorization {

  private final String permissionName;
  private final String resourceType;
  protected String resourceId;

  public MissingAuthorization(String permissionName, String resourceType, String resourceId) {
    this.permissionName = permissionName;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
  }

  public String getViolatedPermissionName() {
    return permissionName;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getResourceId() {
    return resourceId;
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()
        + "[permissionName=%s, resourceType=%s, resourceId=%s]".formatted(permissionName, resourceType, resourceId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MissingAuthorization that = (MissingAuthorization) o;
    return Objects.equals(permissionName, that.permissionName) && Objects.equals(resourceType, that.resourceType)
        && Objects.equals(resourceId, that.resourceId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(permissionName, resourceType, resourceId);
  }
}
