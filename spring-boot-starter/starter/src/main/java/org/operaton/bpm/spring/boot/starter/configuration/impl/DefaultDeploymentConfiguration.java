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
package org.operaton.bpm.spring.boot.starter.configuration.impl;

import org.operaton.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.operaton.bpm.spring.boot.starter.configuration.OperatonDeploymentConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

public class DefaultDeploymentConfiguration extends AbstractOperatonConfiguration
    implements OperatonDeploymentConfiguration {
  @SuppressWarnings({ "java:S1845", "java:S116" })
  private final Logger LOGGER = LoggerFactory.getLogger(DefaultDeploymentConfiguration.class);

  public DefaultDeploymentConfiguration(OperatonBpmProperties operatonBpmProperties) {
    super(operatonBpmProperties);
  }

  @Override
  public void preInit(SpringProcessEngineConfiguration configuration) {
    if (operatonBpmProperties.isAutoDeploymentEnabled()) {
      final Set<Resource> resources = getDeploymentResources();
      configuration.setDeploymentResources(resources.toArray(new Resource[resources.size()]));
      LOG.autoDeployResources(resources);
    }
  }

  @Override
  public Set<Resource> getDeploymentResources() {

    final ResourceArrayPropertyEditor resolver = new ResourceArrayPropertyEditor();

    try {
      final String[] resourcePattern = operatonBpmProperties.getDeploymentResourcePattern();
      LOGGER.debug("resolving deployment resources for pattern {}", (Object[]) resourcePattern);
      resolver.setValue(resourcePattern);

      return Arrays.stream((Resource[]) resolver.getValue())
          .peek(resource -> LOGGER.debug("processing deployment resource {}", resource))
          .filter(this::isFile)
          .peek(resource -> LOGGER.debug("added deployment resource {}", resource))
          .collect(Collectors.toSet());

    } catch (final RuntimeException e) {
      LOGGER.error("unable to resolve resources", e);
    }
    return emptySet();
  }

  private boolean isFile(Resource resource) {

    if (resource.isReadable()) {
      if (resource instanceof UrlResource || resource instanceof ClassPathResource) {
        try {
          URL url = resource.getURL();
          return !url.toString().endsWith("/");
        } catch (IOException e) {
          LOGGER.debug("unable to handle {} as URL", resource, e);
        }
      } else {
        try {
          return !resource.getFile().isDirectory();
        } catch (IOException e) {
          LOGGER.debug("unable to handle {} as file", resource, e);
        }
      }
    }
    LOGGER.warn("unable to determine if resource {} is a deployable resource", resource);
    return false;
  }

}
