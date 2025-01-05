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
package org.operaton.bpm.run.test;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.diagnostics.OperatonIntegration;
import org.operaton.bpm.engine.impl.telemetry.dto.TelemetryDataImpl;
import org.operaton.bpm.run.OperatonApp;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = {OperatonApp.class})
class TelemetryDataTest {

  @Autowired
  ProcessEngine engine;

  @Test
  void shouldAddOperatonIntegration() {
    // given
    ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) engine.getProcessEngineConfiguration();

    // then
    TelemetryDataImpl telemetryData = processEngineConfiguration.getTelemetryData();
    Set<String> operatonIntegration = telemetryData.getProduct().getInternals().getOperatonIntegration();
    assertThat(operatonIntegration)
      .containsExactlyInAnyOrder(OperatonIntegration.CAMUNDA_BPM_RUN, OperatonIntegration.SPRING_BOOT_STARTER);
  }
}
