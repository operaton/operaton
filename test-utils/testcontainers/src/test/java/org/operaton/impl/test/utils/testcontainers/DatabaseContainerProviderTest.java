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
package org.operaton.impl.test.utils.testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Disabled("""
        This test is currently not part of the test suite, as not every used Docker image is compatible with every processor architecture.
        It can be started manually to verify the Testcontainers setup
        """)
class DatabaseContainerProviderTest {

  /**
   * Tests if the test utils are able to start containers and access them via JDBC URL for various databases using Testcontainers.
   *
   * @param jdbcUrl       The JDBC URL to use for connecting to the database.
   * @param versionStatement The SQL statement to execute to retrieve the database version.
   * @param dbVersion     The expected database version.
   */
  @ParameterizedTest(name = "{3}:{2}")
  @CsvSource({
    "jdbc:tc:operatonpostgresql:13.2:///process-engine, SELECT version();, 13.2, postgres",
    "jdbc:tc:operatonmariadb:10.0://localhost:3306/process-engine, SELECT version();, 10.0, mariadb",
    "jdbc:tc:operatonmysql:5.7://localhost:3306/process-engine, SELECT version();, 5.7, mysql",
    "jdbc:tc:operatonmysql:8.0://localhost:3306/process-engine, SELECT version();, 8.0, mysql",
    "jdbc:tc:operatonsqlserver:2022-latest://localhost:1433/process-engine, SELECT @@VERSION, 2022, sqlserver",
    "jdbc:tc:operatonoracle:21-faststart://localhost:1521, SELECT * FROM v$version, 21c, oracle",
    "jdbc:tc:operatondb2:12.1.2.0://localhost:50000, SELECT SERVICE_LEVEL FROM TABLE(SYSPROC.ENV_GET_INST_INFO()), v12, db2"
  })
  void testJdbcTestcontainersUrl(String jdbcUrl, String versionStatement, String dbVersion, String dbLabel) {
    // when
    try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
      connection.setAutoCommit(false);
      ResultSet rs = connection.prepareStatement(versionStatement).executeQuery();
      if (rs.next()) {
        // then
        String version = rs.getString(1);
        assertThat(version).contains(dbVersion);
      }
      connection.rollback();
    } catch (SQLException exception) {
      fail("Testcontainers failed to spin up a Docker container: " + exception.getMessage());
    }
  }
}