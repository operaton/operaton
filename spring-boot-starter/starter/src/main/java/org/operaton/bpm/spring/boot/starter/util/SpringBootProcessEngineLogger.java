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
package org.operaton.bpm.spring.boot.starter.util;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.operaton.bpm.engine.impl.history.handler.HistoryEventHandler;
import org.springframework.core.io.Resource;

import org.operaton.bpm.engine.filter.Filter;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.spring.boot.starter.property.GenericProperties;
import org.operaton.commons.logging.BaseLogger;

public class SpringBootProcessEngineLogger extends BaseLogger {
  public static final String PROJECT_CODE = "STARTER";
  public static final String PROJECT_ID = "SB";
  public static final String PACKAGE = "org.operaton.bpm.spring.boot";

  public static final SpringBootProcessEngineLogger LOG = createLogger(SpringBootProcessEngineLogger.class, PROJECT_CODE, PACKAGE, PROJECT_ID);

  public void creatingInitialAdminUser(User adminUser) {
    logDebug("010", "Creating initial Admin User: {}", adminUser);
  }

  public void skipAdminUserCreation(User existingUser) {
    logDebug("011", "Skip creating initial Admin User, user does exist: {}", existingUser);
  }

  public void createInitialFilter(Filter filter) {
    logInfo("015", "Create initial filter: id={} name={}", filter.getId(), filter.getName());
  }

  public void skipCreateInitialFilter(String filterName) {
    logInfo("016",
        "Skip initial filter creation, the filter with this name already exists: {}",
        filterName);
  }

  public void skipAutoDeployment() {
    logInfo("020", "ProcessApplication enabled: autoDeployment via springConfiguration#deploymentResourcePattern is disabled");
  }

  public void autoDeployResources(Set<Resource> resources) {
    // Only log the description of `Resource` objects since log libraries that serialize them and
    // therefore consume the input stream make the deployment fail since the input stream has
    // already been consumed.
    Set<String> resourceDescriptions = resources.stream()
        .filter(Objects::nonNull)
        .map(Resource::getDescription)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    logInfo("021", "Auto-Deploying resources: {}", resourceDescriptions);
  }

  public void configureJobExecutorPool(Integer corePoolSize, Integer maxPoolSize) {
    logInfo("040", "Setting up jobExecutor with corePoolSize={}, maxPoolSize:{}", corePoolSize, maxPoolSize);
  }

  public SpringBootStarterException exceptionDuringBinding(String message) {
    return new SpringBootStarterException(exceptionMessage(
        "050", message));
  }

  public void propertiesApplied(GenericProperties genericProperties) {
    logDebug("051", "Properties bound to configuration: {}", genericProperties);
  }

  public void ignoringInvalidDefaultSerializationFormat(String defaultSerializationFormat) {
    logWarn("060", "Ignoring invalid defaultSerializationFormat='{}'", defaultSerializationFormat);
  }

  public void ignoringInvalidProcessEngineName(String processEngineName) {
    logWarn("061", "Ignoring invalid processEngineName='{}' - must not be null, blank or contain hyphen", processEngineName);
  }

  public void registerCustomJobHandler(String type) {
    logInfo("062", "Registered custom JobHandler: '{}'", type);
  }

  public void registerCustomHistoryEventHandler(Class<? extends HistoryEventHandler> historyEventHandlerType) {
    logInfo("063", "Register custom HistoryEventHandler: '{}'", historyEventHandlerType);
  }
}
