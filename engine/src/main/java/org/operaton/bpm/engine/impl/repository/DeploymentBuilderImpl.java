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
package org.operaton.bpm.engine.impl.repository;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.exception.NotValidException;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;
import org.operaton.bpm.engine.impl.RepositoryServiceImpl;
import org.operaton.bpm.engine.impl.cmd.CommandLogger;
import org.operaton.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.operaton.bpm.engine.impl.persistence.entity.ResourceEntity;
import org.operaton.bpm.engine.impl.util.IoUtil;
import org.operaton.bpm.engine.impl.util.ReflectUtil;
import org.operaton.bpm.engine.impl.util.StringUtil;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.engine.repository.DeploymentBuilder;
import org.operaton.bpm.engine.repository.DeploymentWithDefinitions;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.cmmn.Cmmn;
import org.operaton.bpm.model.cmmn.CmmnModelInstance;
import org.operaton.bpm.model.dmn.Dmn;
import org.operaton.bpm.model.dmn.DmnModelInstance;
import org.operaton.commons.utils.CollectionUtil;

import static org.operaton.bpm.engine.impl.ResourceSuffixes.BPMN_RESOURCE_SUFFIXES;
import static org.operaton.bpm.engine.impl.ResourceSuffixes.CMMN_RESOURCE_SUFFIXES;
import static org.operaton.bpm.engine.impl.ResourceSuffixes.DMN_RESOURCE_SUFFIXES;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotContainsNull;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotEmpty;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class DeploymentBuilderImpl implements DeploymentBuilder, Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private static final String DEPLOYMENT_ID = "deploymentId";
  private static final String MODEL_INSTANCE = "modelInstance";
  private static final String RESOURCE_IDS = "resourceIds";
  private static final String RESOURCE_NAME = "resourceName";
  private static final String RESOURCE_NAMES = "resourceNames";

  private static final CommandLogger LOG = ProcessEngineLogger.CMD_LOGGER;

  protected transient RepositoryServiceImpl repositoryService;
  protected DeploymentEntity deployment = new DeploymentEntity();
  protected boolean isDuplicateFilterEnabled;
  protected boolean deployChangedOnly;
  protected Date processDefinitionsActivationDate;

  protected String nameFromDeployment;
  private final Set<String> deployments = new HashSet<>();
  private final Map<String, Set<String>> deploymentResourcesById = new HashMap<>();
  private final Map<String, Set<String>> deploymentResourcesByName = new HashMap<>();

  public DeploymentBuilderImpl(RepositoryServiceImpl repositoryService) {
    this.repositoryService = repositoryService;
  }

  @Override
  public DeploymentBuilder addInputStream(String resourceName, InputStream inputStream) {
    ensureNotNull("inputStream for resource '%s' is null".formatted(resourceName), "inputStream", inputStream);
    byte[] bytes = IoUtil.readInputStream(inputStream, resourceName);

    return addBytes(resourceName, bytes);
  }

  @Override
  public DeploymentBuilder addClasspathResource(String resource) {
    InputStream inputStream = ReflectUtil.getResourceAsStream(resource);
    ensureNotNull("resource '%s' not found".formatted(resource), "inputStream", inputStream);
    return addInputStream(resource, inputStream);
  }

  @Override
  public DeploymentBuilder addString(String resourceName, String text) {
    ensureNotNull("text", text);

    byte[] bytes = repositoryService != null && repositoryService.getDeploymentCharset() != null
      ? text.getBytes(repositoryService.getDeploymentCharset())
      : text.getBytes();

    return addBytes(resourceName, bytes);
  }

  @Override
  public DeploymentBuilder addModelInstance(String resourceName, CmmnModelInstance modelInstance) {
    ensureNotNull(MODEL_INSTANCE, modelInstance);

    validateResouceName(resourceName, CMMN_RESOURCE_SUFFIXES);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Cmmn.writeModelToStream(outputStream, modelInstance);

    return addBytes(resourceName, outputStream.toByteArray());
  }

  @Override
  public DeploymentBuilder addModelInstance(String resourceName, BpmnModelInstance modelInstance) {
    ensureNotNull(MODEL_INSTANCE, modelInstance);

    validateResouceName(resourceName, BPMN_RESOURCE_SUFFIXES);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Bpmn.writeModelToStream(outputStream, modelInstance);

    return addBytes(resourceName, outputStream.toByteArray());
  }

  @Override
  public DeploymentBuilder addModelInstance(String resourceName, DmnModelInstance modelInstance) {
    ensureNotNull(MODEL_INSTANCE, modelInstance);

    validateResouceName(resourceName, DMN_RESOURCE_SUFFIXES);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Dmn.writeModelToStream(outputStream, modelInstance);

    return addBytes(resourceName, outputStream.toByteArray());
  }

  private void validateResouceName(String resourceName, String[] resourceSuffixes) {
    if (!StringUtil.hasAnySuffix(resourceName, resourceSuffixes)) {
      LOG.warnDeploymentResourceHasWrongName(resourceName, resourceSuffixes);
    }
  }

  protected DeploymentBuilder addBytes(String resourceName, byte[] bytes) {
    ResourceEntity resource = new ResourceEntity();
    resource.setBytes(bytes);
    resource.setName(resourceName);
    deployment.addResource(resource);

    return this;
  }

  @Override
  public DeploymentBuilder addZipInputStream(ZipInputStream zipInputStream) {
    try {
      ZipEntry entry = zipInputStream.getNextEntry();
      while (entry != null) {
        if (!entry.isDirectory()) {
          String entryName = entry.getName();
          addInputStream(entryName, zipInputStream);
        }
        entry = zipInputStream.getNextEntry();
      }
    } catch (Exception e) {
      throw new ProcessEngineException("problem reading zip input stream", e);
    }
    return this;
  }

  @Override
  public DeploymentBuilder addDeploymentResources(String deploymentId) {
    ensureNotNull(NotValidException.class, DEPLOYMENT_ID, deploymentId);
    deployments.add(deploymentId);
    return this;
  }

  @Override
  public DeploymentBuilder addDeploymentResourceById(String deploymentId, String resourceId) {
    ensureNotNull(NotValidException.class, DEPLOYMENT_ID, deploymentId);
    ensureNotNull(NotValidException.class, "resourceId", resourceId);

    CollectionUtil.addToMapOfSets(deploymentResourcesById, deploymentId, resourceId);

    return this;
  }

  @Override
  public DeploymentBuilder addDeploymentResourcesById(String deploymentId, List<String> resourceIds) {
    ensureNotNull(NotValidException.class, DEPLOYMENT_ID, deploymentId);

    ensureNotNull(NotValidException.class, RESOURCE_IDS, resourceIds);
    ensureNotEmpty(NotValidException.class, RESOURCE_IDS, resourceIds);
    ensureNotContainsNull(NotValidException.class, RESOURCE_IDS, resourceIds);

    CollectionUtil.addCollectionToMapOfSets(deploymentResourcesById, deploymentId, resourceIds);

    return this;
  }

  @Override
  public DeploymentBuilder addDeploymentResourceByName(String deploymentId, String resourceName) {
    ensureNotNull(NotValidException.class, DEPLOYMENT_ID, deploymentId);
    ensureNotNull(NotValidException.class, RESOURCE_NAME, resourceName);

    CollectionUtil.addToMapOfSets(deploymentResourcesByName, deploymentId, resourceName);

    return this;
  }

  @Override
  public DeploymentBuilder addDeploymentResourcesByName(String deploymentId, List<String> resourceNames) {
    ensureNotNull(NotValidException.class, DEPLOYMENT_ID, deploymentId);

    ensureNotNull(NotValidException.class, RESOURCE_NAMES, resourceNames);
    ensureNotEmpty(NotValidException.class, RESOURCE_NAMES, resourceNames);
    ensureNotContainsNull(NotValidException.class, RESOURCE_NAMES, resourceNames);

    CollectionUtil.addCollectionToMapOfSets(deploymentResourcesByName, deploymentId, resourceNames);

    return this;
  }

  @Override
  public DeploymentBuilder name(String name) {
    if (nameFromDeployment != null && !nameFromDeployment.isEmpty()) {
      String message = "Cannot set the deployment name to '%s', because the property 'nameForDeployment' has been already set to '%s'.".formatted(name, nameFromDeployment);
      throw new NotValidException(message);
    }
    deployment.setName(name);
    return this;
  }

  @Override
  public DeploymentBuilder nameFromDeployment(String deploymentId) {
    String name = deployment.getName();
    if (name != null && !name.isEmpty()) {
      String message = "Cannot set the given deployment id '%s' to get the name from it, because the deployment name has been already set to '%s'.".formatted(deploymentId, name);
      throw new NotValidException(message);
    }
    nameFromDeployment = deploymentId;
    return this;
  }

  @Override
  public DeploymentBuilder enableDuplicateFiltering(boolean deployChangedOnly) {
    this.isDuplicateFilterEnabled = true;
    this.deployChangedOnly = deployChangedOnly;
    return this;
  }

  @Override
  public DeploymentBuilder activateProcessDefinitionsOn(Date date) {
    this.processDefinitionsActivationDate = date;
    return this;
  }

  @Override
  public DeploymentBuilder source(String source) {
    deployment.setSource(source);
    return this;
  }

  @Override
  public DeploymentBuilder tenantId(String tenantId) {
    deployment.setTenantId(tenantId);
    return this;
  }

  @Override
  public Deployment deploy() {
    return deployWithResult();
  }

  @Override
  public DeploymentWithDefinitions deployWithResult() {
    return repositoryService.deployWithResult(this);
  }


  @Override
  public Collection<String> getResourceNames() {
    if(deployment.getResources() == null) {
      return Collections.<String>emptySet();
    } else {
      return deployment.getResources().keySet();
    }
  }

  // getters and setters //////////////////////////////////////////////////////

  public DeploymentEntity getDeployment() {
    return deployment;
  }

  public boolean isDuplicateFilterEnabled() {
    return isDuplicateFilterEnabled;
  }

  public boolean isDeployChangedOnly() {
    return deployChangedOnly;
  }

  public Date getProcessDefinitionsActivationDate() {
    return processDefinitionsActivationDate;
  }

  public String getNameFromDeployment() {
    return nameFromDeployment;
  }

  public Set<String> getDeployments() {
    return deployments;
  }

  public Map<String, Set<String>> getDeploymentResourcesById() {
    return deploymentResourcesById;
  }

  public Map<String, Set<String>> getDeploymentResourcesByName() {
    return deploymentResourcesByName;
  }

}
