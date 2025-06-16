/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.integrationtest.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.operaton.impl.test.utils.testcontainers.OperatonMSSQLContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * {@link org.jboss.arquillian.core.api.annotation.Observer} for Arquillian lifecycle events.
 * Observes {@link ContainerRegistry} event and facilitates ability to start jdbc database
 * container before and provide connection information to Arquillian container.
 */
@SuppressWarnings("rawtypes")
public class ArquillianEventObserver {

  private static final String POSTGRES = "postgres";
  private static final String POSTGRES_VERSION = "13.2";

  private static final String SQLSERVER = "sqlserver";
  private static final String SQLSERVER_VERSION = "2022-latest";

  // Initialized with providers, so we do not start all containers at the same time here statically upon initialization
  private static final Map<String, Supplier<JdbcDatabaseContainer>> AVAILABLE_DB_CONTAINERS = new HashMap<>();

  static {
    AVAILABLE_DB_CONTAINERS.put(POSTGRES, () -> new PostgreSQLContainer(POSTGRES + ":" + POSTGRES_VERSION));
    AVAILABLE_DB_CONTAINERS.put(SQLSERVER, () -> new OperatonMSSQLContainer("mcr.microsoft.com/mssql/server" + ":" + SQLSERVER_VERSION).acceptLicense());
  }

  private static JdbcDatabaseContainer dbContainer;

  /**
   * Listens for the Arquillian ContainerRegistry event to start the appropriate jdbc database container
   * via testcontainers before the actual Arquillian container is started. Which database container is
   * started depends on two environment variables:
   * <code>database.type</code>
   * These should be passed as VM arguments for local testing and are preconfigured in the appropriate
   * Maven profiles
   *
   * @param registry      arquillian container registry
   * @param serviceLoader arquillian server loader
   */
  public void onContainerRegistryEvent(@Observes ContainerRegistry registry, ServiceLoader serviceLoader) {
    var containerName = System.getProperty("databaseType");

    if (containerName != null && AVAILABLE_DB_CONTAINERS.containsKey(containerName)) {
      dbContainer = AVAILABLE_DB_CONTAINERS.get(containerName).get();
      dbContainer.start();
      //Assume that there is only one container in the registry
      registry.getContainers().stream().findFirst().ifPresent(container -> {
        var jvmArguments = container.getContainerConfiguration().getContainerProperty("javaVmArguments");
        jvmArguments += " -Dengine-connection-url=" + dbContainer.getJdbcUrl();
        container.getContainerConfiguration().overrideProperty("javaVmArguments", jvmArguments);
      });
    }
  }
}
