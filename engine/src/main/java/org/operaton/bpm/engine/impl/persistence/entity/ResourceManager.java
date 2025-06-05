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
package org.operaton.bpm.engine.impl.persistence.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.engine.impl.persistence.AbstractManager;


/**
 * @author Tom Baeyens
 */
public class ResourceManager extends AbstractManager {

  private static final String DEPLOYMENT_ID = "deploymentId";
  private static final String DEPLOYMENT_NAME = "deploymentName";
  private static final String RESOURCE_ID = "resourceId";
  private static final String RESOURCE_IDS = "resourceIds";
  private static final String RESOURCE_NAME = "resourceName";
  private static final String RESOURCE_NAMES = "resourceNames";
  private static final String RESOURCES_TO_FIND = "resourcesToFind";
  private static final String SOURCE = "source";

  public void insertResource(ResourceEntity resource) {
    getDbEntityManager().insert(resource);
  }

  public void deleteResourcesByDeploymentId(String deploymentId) {
    getDbEntityManager().delete(ResourceEntity.class, "deleteResourcesByDeploymentId", deploymentId);
  }

  public ResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
    Map<String, Object> params = new HashMap<>();
    params.put(DEPLOYMENT_ID, deploymentId);
    params.put(RESOURCE_NAME, resourceName);
    return (ResourceEntity) getDbEntityManager().selectOne("selectResourceByDeploymentIdAndResourceName", params);
  }

  @SuppressWarnings("unchecked")
  public List<ResourceEntity> findResourceByDeploymentIdAndResourceNames(String deploymentId, String... resourceNames) {
    Map<String, Object> params = new HashMap<>();
    params.put(DEPLOYMENT_ID, deploymentId);
    params.put(RESOURCE_NAMES, resourceNames);
    return getDbEntityManager().selectList("selectResourceByDeploymentIdAndResourceNames", params);
  }

  public ResourceEntity findResourceByDeploymentIdAndResourceId(String deploymentId, String resourceId) {
    Map<String, Object> params = new HashMap<>();
    params.put(DEPLOYMENT_ID, deploymentId);
    params.put(RESOURCE_ID, resourceId);
    return (ResourceEntity) getDbEntityManager().selectOne("selectResourceByDeploymentIdAndResourceId", params);
  }

  @SuppressWarnings("unchecked")
  public List<ResourceEntity> findResourceByDeploymentIdAndResourceIds(String deploymentId, String... resourceIds) {
    Map<String, Object> params = new HashMap<>();
    params.put(DEPLOYMENT_ID, deploymentId);
    params.put(RESOURCE_IDS, resourceIds);
    return getDbEntityManager().selectList("selectResourceByDeploymentIdAndResourceIds", params);
  }

  @SuppressWarnings("unchecked")
  public List<ResourceEntity> findResourcesByDeploymentId(String deploymentId) {
    return getDbEntityManager().selectList("selectResourcesByDeploymentId", deploymentId);
  }

  @SuppressWarnings("unchecked")
  public Map<String, ResourceEntity> findLatestResourcesByDeploymentName(String deploymentName, Set<String> resourcesToFind, String source, String tenantId) {
    Map<String, Object> params = new HashMap<>();
    params.put(DEPLOYMENT_NAME, deploymentName);
    params.put(RESOURCES_TO_FIND, resourcesToFind);
    params.put(SOURCE, source);
    params.put(TENANT_ID, tenantId);

    List<ResourceEntity> resources = getDbEntityManager().selectList("selectLatestResourcesByDeploymentName", params);

    Map<String, ResourceEntity> existingResourcesByName = new HashMap<>();
    for (ResourceEntity existingResource : resources) {
      existingResourcesByName.put(existingResource.getName(), existingResource);
    }

    return existingResourcesByName;
  }

}
