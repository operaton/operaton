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

import java.util.Map;

import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.operaton.impl.test.utils.testcontainers.OperatonDb2ContainerProvider;
import org.operaton.impl.test.utils.testcontainers.OperatonMSSQLContainerProvider;
import org.operaton.impl.test.utils.testcontainers.OperatonMariaDBContainerProvider;
import org.operaton.impl.test.utils.testcontainers.OperatonMySqlContainerProvider;
import org.operaton.impl.test.utils.testcontainers.OperatonOracleContainerProvider;
import org.operaton.impl.test.utils.testcontainers.OperatonPostgreSQLContainerProvider;
import org.testcontainers.containers.JdbcDatabaseContainer;

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

  private static final String MARIADB = "mariadb";
  private static final String MARIADB_VERSION = "10.0";

  private static final String ORACLE = "oracle";
  private static final String ORACLE_VERSION = "21-faststart";

  private static final String DB2 = "db2";
  private static final String DB2_VERSION = "12.1.2.0";

  private static final String MYSQL = "mysql";
  private static final String MYSQL_VERSION = "9.2.0";

  // Initialized with providers, so we do not start all containers at the same time here statically upon initialization
  private static final Map<String, JdbcDatabaseContainer> AVAILABLE_DB_CONTAINERS = Map.of(
          POSTGRES, new OperatonPostgreSQLContainerProvider().newInstance(POSTGRES_VERSION),
          SQLSERVER, new OperatonMSSQLContainerProvider().newInstance(SQLSERVER_VERSION),
          MARIADB, new OperatonMariaDBContainerProvider().newInstance(MARIADB_VERSION),
          ORACLE, new OperatonOracleContainerProvider().newInstance(ORACLE_VERSION),
          DB2, new OperatonDb2ContainerProvider().newInstance(DB2_VERSION),
          MYSQL, new OperatonMySqlContainerProvider().newInstance(MYSQL_VERSION)
  );

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
      JdbcDatabaseContainer dbContainer = AVAILABLE_DB_CONTAINERS.get(containerName);
      dbContainer.start();

      //Assume that there is only one container in the registry
      registry.getContainers().stream().findFirst().ifPresent(container -> {
        var jvmArguments = container.getContainerConfiguration().getContainerProperty("javaVmArguments");
        jvmArguments += " -Dengine-connection-url=" + dbContainer.getJdbcUrl();
        jvmArguments += " -Ddatabase.username=" + dbContainer.getUsername();
        jvmArguments += " -Ddatabase.password=" + dbContainer.getPassword();
        container.getContainerConfiguration().overrideProperty("javaVmArguments", jvmArguments);
      });
    }
  }
}
