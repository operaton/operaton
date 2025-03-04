/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
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

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.OracleContainerProvider;
import org.testcontainers.utility.DockerImageName;

public class OperatonOracleContainerProvider extends OracleContainerProvider {

  private static final String NAME = "operatonoracle";

  @Override
  public boolean supports(String databaseType) {
    return NAME.equals(databaseType);
  }

  @Override
  public JdbcDatabaseContainer<?> newInstance(String tag) {
    if("aaarch64".equals(System.getProperty("os.arch"))) {
      throw new IllegalStateException("The Oracle Testcontainers tests cannot be executed on ARM architecture, as Oracle Database does not support it.");
    }
    DockerImageName dockerImageName = TestcontainersHelper
      .resolveDockerImageName("oracle", tag, "gvenzl/oracle-xe");
    return new OracleContainer(dockerImageName);
  }
}