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

import org.testcontainers.db2.Db2Container;
import org.testcontainers.containers.Db2ContainerProvider;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

public class OperatonDb2ContainerProvider extends Db2ContainerProvider {

  private static final String NAME = "operatondb2";

  @Override
  public boolean supports(String databaseType) {
    return NAME.equals(databaseType);
  }

  @Override
  public JdbcDatabaseContainer<?> newInstance(String tag) {
    DockerImageName dockerImageName = TestcontainersHelper
      .resolveDockerImageName("ibmdb2", tag, "icr.io/db2_community/db2");
    return new Db2Container(dockerImageName).acceptLicense();
  }
}
