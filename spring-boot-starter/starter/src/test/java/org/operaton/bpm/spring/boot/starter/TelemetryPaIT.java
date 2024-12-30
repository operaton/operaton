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
package org.operaton.bpm.spring.boot.starter;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.diagnostics.DiagnosticsRegistry;
import org.operaton.bpm.engine.impl.telemetry.dto.ApplicationServerImpl;
import org.operaton.bpm.spring.boot.starter.test.pa.TestProcessApplication;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
  classes = {TestProcessApplication.class},
  webEnvironment = WebEnvironment.RANDOM_PORT
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TelemetryPaIT extends AbstractOperatonAutoConfigurationIT {

  @Test
  void shouldSubmitApplicationServerData() {
    DiagnosticsRegistry diagnosticsRegistry = ((ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration()).getDiagnosticsRegistry();

    // then
    ApplicationServerImpl applicationServer = diagnosticsRegistry.getApplicationServer();
    assertThat(applicationServer).isNotNull();
    assertThat(applicationServer.getVendor()).isEqualTo("Apache Tomcat");
    assertThat(applicationServer.getVersion()).isNotNull();
  }
}
