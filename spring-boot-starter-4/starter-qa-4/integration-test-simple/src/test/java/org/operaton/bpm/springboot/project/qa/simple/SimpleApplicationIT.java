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
package org.operaton.bpm.springboot.project.qa.simple;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.diagnostics.DiagnosticsRegistry;
import org.operaton.bpm.engine.impl.telemetry.dto.ApplicationServerImpl;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {Application.class},
  webEnvironment = SpringBootTest.WebEnvironment.NONE)
class SimpleApplicationIT {

  @Autowired
  RuntimeService runtimeService;

  @Autowired
  ProcessEngine processEngine;

  @Test
  void shouldStartApplicationSuccessfully() {
    // then no exception due to missing classes is thrown
    assertThat(runtimeService).isNotNull();
  }

  /**
   * Verifies that a Spring Boot project without spring-boot-starter-web and
   * spring-boot-starter-jersey (i.e. without servlet API) still works correctly.
   */
  @Test
  void shouldNotDetermineApplicationServer() {

    DiagnosticsRegistry diagnosticsRegistry = ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).getDiagnosticsRegistry();

    // then
    ApplicationServerImpl applicationServer = diagnosticsRegistry.getApplicationServer();
    assertThat(applicationServer).isNull();
  }

}
