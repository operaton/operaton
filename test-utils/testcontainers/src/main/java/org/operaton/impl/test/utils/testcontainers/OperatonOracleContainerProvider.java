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

package org.operaton.impl.test.utils.testcontainers;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Objects;

public class OperatonOracleContainerProvider extends JdbcDatabaseContainerProvider {

  private static final String NAME = "operatonoracle";

  private static final List<String> XE_VERSIONS = List.of("21", "18", "11");

  @Override
  public boolean supports(String databaseType) {
    return NAME.equals(databaseType);
  }

  @Override
  public JdbcDatabaseContainer<?> newInstance(String tag) {
    Objects.requireNonNull(tag);
    // tags starting with 21, 18, or 11 are routed to gvenzl/oracle-xe.
    if (XE_VERSIONS.stream().anyMatch(tag::startsWith)) {
      DockerImageName dockerImageName = TestcontainersHelper
              .resolveDockerImageName("oracle", tag, "gvenzl/oracle-xe");
      return new org.testcontainers.containers.OracleContainer(dockerImageName);
      // anything else goes to gvenzl/oracle-free, including "latest", "slim" and "full".
    } else {
      DockerImageName dockerImageName = TestcontainersHelper
              .resolveDockerImageName("oracle", tag, "gvenzl/oracle-free");
      return new org.testcontainers.oracle.OracleContainer(dockerImageName);
    }
  }
}
