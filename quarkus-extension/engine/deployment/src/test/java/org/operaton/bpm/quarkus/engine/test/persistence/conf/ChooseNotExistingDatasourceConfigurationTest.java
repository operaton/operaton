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
package org.operaton.bpm.quarkus.engine.test.persistence.conf;

import jakarta.enterprise.inject.UnsatisfiedResolutionException;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.quarkus.engine.test.helper.ProcessEngineAwareExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ChooseNotExistingDatasourceConfigurationTest {

  @RegisterExtension
  static QuarkusUnitTest unitTest = new ProcessEngineAwareExtension()
      .withConfigurationResource("org/operaton/bpm/quarkus/engine/test/persistence/conf/multiple-datasources-application.properties")
      .overrideConfigKey("quarkus.operaton.datasource", "quaternary")
      .assertException(throwable -> assertThat(throwable)
          .hasMessageContaining("quaternary")
          .isInstanceOf(UnsatisfiedResolutionException.class))
      .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

  @Test
  void shouldExpectException() {
    // Exception is raised during application bootstrap.
    // See assertion in the extension registration above.
  }

}
