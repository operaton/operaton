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
package org.operaton.bpm.engine.repository;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.operaton.bpm.application.ProcessApplication;
import org.operaton.bpm.application.ProcessApplicationInterface;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;

/**
 * <p>Builder for a {@link ProcessApplication} deployment</p>
 *
 * <p>A process application deployment is different from a regular deployment.
 * Besides deploying a set of process definitions to the database,
 * this deployment has the additional side effect that the process application
 * is registered for the deployment. This means that the process engine will exeute
 * all process definitions contained in the deployment in the context of the process
 * application (by calling the process application's
 * {@link ProcessApplicationInterface#execute(java.util.concurrent.Callable)} method.<p>
 *
 * @author Daniel Meyer
 *
 */
public interface ProcessApplicationDeploymentBuilder extends DeploymentBuilder {

  /**
   * <p>If this method is called, additional registrations will be created for
   * previous versions of the deployment.</p>
   */
  ProcessApplicationDeploymentBuilder resumePreviousVersions();

  /**
   * This method defines on what additional registrations will be based.
   * The value will only be recognized if {@link #resumePreviousVersions()} is set.
   * <p>
   * @see ResumePreviousBy
   * @see #resumePreviousVersions()
   * @param resumeByProcessDefinitionKey one of the constants from {@link ResumePreviousBy}
   */
  ProcessApplicationDeploymentBuilder resumePreviousVersionsBy(String resumePreviousVersionsBy);

  @Override
  ProcessApplicationDeployment deploy();

  // overridden methods //////////////////////////////

  @Override
  ProcessApplicationDeploymentBuilder addInputStream(String resourceName, InputStream inputStream);

  @Override
  ProcessApplicationDeploymentBuilder addClasspathResource(String resource);

  @Override
  ProcessApplicationDeploymentBuilder addString(String resourceName, String text);

  @Override
  ProcessApplicationDeploymentBuilder addModelInstance(String resourceName, BpmnModelInstance modelInstance);

  @Override
  ProcessApplicationDeploymentBuilder addZipInputStream(ZipInputStream zipInputStream);

  @Override
  ProcessApplicationDeploymentBuilder name(String name);

  @Override
  ProcessApplicationDeploymentBuilder nameFromDeployment(String deploymentId);

  @Override
  ProcessApplicationDeploymentBuilder source(String source);

  /**
   * @deprecated use {@link #enableDuplicateFiltering(boolean)} instead
   */
  @Deprecated(forRemoval = true)
  @Override
  ProcessApplicationDeploymentBuilder enableDuplicateFiltering();

  @Override
  ProcessApplicationDeploymentBuilder enableDuplicateFiltering(boolean deployChangedOnly);

  @Override
  ProcessApplicationDeploymentBuilder activateProcessDefinitionsOn(Date date);

  @Override
  ProcessApplicationDeploymentBuilder addDeploymentResources(String deploymentId);

  @Override
  ProcessApplicationDeploymentBuilder addDeploymentResourceById(String deploymentId, String resourceId);

  @Override
  ProcessApplicationDeploymentBuilder addDeploymentResourcesById(String deploymentId, List<String> resourceIds);

  @Override
  ProcessApplicationDeploymentBuilder addDeploymentResourceByName(String deploymentId, String resourceName);

  @Override
  ProcessApplicationDeploymentBuilder addDeploymentResourcesByName(String deploymentId, List<String> resourceNames);

}
