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
package org.operaton.bpm.run;

import org.apache.commons.lang3.StringUtils;
import org.operaton.bpm.spring.boot.starter.configuration.impl.DefaultDeploymentConfiguration;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OperatonBpmRunDeploymentConfiguration extends DefaultDeploymentConfiguration {

  private final String deploymentDir;

  private static final Logger log = LoggerFactory.getLogger(OperatonBpmRunDeploymentConfiguration.class);

  public OperatonBpmRunDeploymentConfiguration(String deploymentDir, OperatonBpmProperties operatonBpmProperties) {
    super(operatonBpmProperties);
    this.deploymentDir = deploymentDir;
  }

  @Override
  public Set<Resource> getDeploymentResources() {
    if (!StringUtils.isEmpty(deploymentDir)) {
      Path resourceDir = Path.of(deploymentDir);

      try (Stream<Path> stream = Files.walk(resourceDir)) {
        return stream.filter(file -> !Files.isDirectory(file)).map(FileSystemResource::new).collect(Collectors.toSet());
      } catch (IOException e) {
        log.warn("An error occurred while retrieving deployment resources from the dir {}", resourceDir, e);
      }
    }
    return Collections.emptySet();
  }

  protected String getNormalizedDeploymentDir() {
    String result = deploymentDir;

    if (result != null && "\\".equals(File.separator)) {
      result = result.replace("\\", "/");
    }
    return result;
  }
}
