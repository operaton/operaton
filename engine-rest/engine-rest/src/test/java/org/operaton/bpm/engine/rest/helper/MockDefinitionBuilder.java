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
package org.operaton.bpm.engine.rest.helper;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.operaton.bpm.engine.repository.ProcessDefinition;

public class MockDefinitionBuilder {

  private String id;
  private String key;
  private String category;
  private String description;
  private String name;
  private int version;
  private String resource;
  private String deploymentId;
  private String diagram;
  private boolean suspended;
  private boolean startFormKey;
  private String tenantId;
  private String versionTag;
  private boolean isStartableInTasklist = true;

  public MockDefinitionBuilder id(String id) {
    this.id = id;
    return this;
  }

  public MockDefinitionBuilder key(String key) {
    this.key = key;
    return this;
  }

  public MockDefinitionBuilder category(String category) {
    this.category = category;
    return this;
  }

  public MockDefinitionBuilder description(String description) {
    this.description = description;
    return this;
  }

  public MockDefinitionBuilder name(String name) {
    this.name = name;
    return this;
  }

  public MockDefinitionBuilder version(int version) {
    this.version = version;
    return this;
  }

  public MockDefinitionBuilder resource(String resource) {
    this.resource = resource;
    return this;
  }

  public MockDefinitionBuilder deploymentId(String deploymentId) {
    this.deploymentId = deploymentId;
    return this;
  }

  public MockDefinitionBuilder diagram(String diagram) {
    this.diagram = diagram;
    return this;
  }

  public MockDefinitionBuilder suspended(boolean suspended) {
    this.suspended = suspended;
    return this;
  }

  public MockDefinitionBuilder startFormKey(boolean startFormKey) {
    this.startFormKey = startFormKey;
    return this;
  }

  public MockDefinitionBuilder tenantId(String tenantId) {
    this.tenantId = tenantId;
    return this;
  }

  public MockDefinitionBuilder versionTag(String versionTag) {
    this.versionTag = versionTag;
    return this;
  }

  public MockDefinitionBuilder isStartableInTasklist(boolean isStartableInTasklist) {
    this.isStartableInTasklist = isStartableInTasklist;
    return this;
  }

  public ProcessDefinition build() {
    ProcessDefinition mockDefinition = mock(ProcessDefinition.class);
    when(mockDefinition.getId()).thenReturn(id);
    when(mockDefinition.getCategory()).thenReturn(category);
    when(mockDefinition.getName()).thenReturn(name);
    when(mockDefinition.getKey()).thenReturn(key);
    when(mockDefinition.getDescription()).thenReturn(description);
    when(mockDefinition.getVersion()).thenReturn(version);
    when(mockDefinition.getResourceName()).thenReturn(resource);
    when(mockDefinition.getDeploymentId()).thenReturn(deploymentId);
    when(mockDefinition.getDiagramResourceName()).thenReturn(diagram);
    when(mockDefinition.isSuspended()).thenReturn(suspended);
    when(mockDefinition.hasStartFormKey()).thenReturn(startFormKey);
    when(mockDefinition.getTenantId()).thenReturn(tenantId);
    when(mockDefinition.getVersionTag()).thenReturn(versionTag);
    when(mockDefinition.isStartableInTasklist()).thenReturn(isStartableInTasklist);
    return mockDefinition;
  }
}
