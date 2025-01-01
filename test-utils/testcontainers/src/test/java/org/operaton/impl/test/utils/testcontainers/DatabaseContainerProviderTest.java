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

@Disabled("This test should not be run on our CI, as it requires a Docker-in-Docker image to run successfully.")
class DatabaseContainerProviderTest {

  @ParameterizedTest(name = "{index} => jdbcUrl={0}, versionStatement={1}, dbVersion={2}")
  @CsvSource({
    // The Operaton PostgreSQL 13.2 image is compatible with Testcontainers.
    // For older versions, please use the public Docker images (DockerHub repo: postgres).
    "jdbc:tc:oppostgresql:13.2:///process-engine, SELECT version();, 13.2",
    // The current Operaton MariaDB images are compatible with Testcontainers.
    // The username and password need to be explicitly declared.
    "jdbc:tc:opmariadb:10.0://localhost:3306/process-engine?user=operaton&password=operaton, SELECT version();, 10.0",
      // The current Operaton MySQL images are compatible with Testcontainers.
      // The username and password need to be explicitly declared.
    "jdbc:tc:opmysql:5.7://localhost:3306/process-engine?user=operaton&password=operaton, SELECT version();, 5.7",
    "jdbc:tc:opmysql:8.0://localhost:3306/process-engine?user=operaton&password=operaton, SELECT version();, 8.0",
      // The current Operaton SqlServer 2017/2019 images are compatible with Testcontainers.
    "jdbc:tc:opsqlserver:2017:///process-engine, SELECT @@VERSION, 2017",
    "jdbc:tc:opsqlserver:2019:///process-engine, SELECT @@VERSION, 2019"
    // The current Operaton DB2 images are not compatible with Testcontainers.
    // { "jdbc:tc:opdb2:11.1:///engine?user=operaton&password=operaton", "SELECT * FROM SYSIBMADM.ENV_INST_INFO;", "11.1"},
    // The current Operaton Oracle images are not compatible with Testcontainers.
    // { "jdbc:tc:oporacle:thin:@localhost:1521:xe?user=operaton&password=operaton", "SELECT * FROM v$version;", "18" }
  })
  void testJdbcTestcontainersUrl(String jdbcUrl, String versionStatement, String dbVersion) {
    // when
    try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
      connection.setAutoCommit(false);
      ResultSet rs = connection.prepareStatement(versionStatement).executeQuery();
      if (rs.next()) {
        // then
        String version = rs.getString(1);
        assertThat(version).contains(dbVersion);
      }
    } catch (SQLException throwables) {
      fail("Testcontainers failed to spin up a Docker container: " + throwables.getMessage());
    }
  }
}