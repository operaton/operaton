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
package org.operaton.bpm.engine.test.api.authorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.test.api.authorization.util.AuthorizationScenario;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.engine.test.junit5.authorization.AuthorizationTestExtension;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

class AuthorizationLoggingTest {

  protected static final String CONTEXT_LOGGER = "org.operaton.bpm.engine.context";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder().build();
  @RegisterExtension
  AuthorizationTestExtension authRule = new AuthorizationTestExtension(engineRule);

  @RegisterExtension
  public ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension()
      .watch(CONTEXT_LOGGER)
      .level(Level.DEBUG);

  @AfterEach
  void tearDown() {
    engineRule.getProcessEngineConfiguration().setAuthorizationEnabled(false);
    AuthorizationService authorizationService = engineRule.getAuthorizationService();
    for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }

  @Test
  void shouldLogOnDebugLevel() {
    // given
    AuthorizationScenario scenario = new AuthorizationScenario().withoutAuthorizations();

    authRule.init(scenario)
        .withUser("userId")
        .start();

    // when
    engineRule.getManagementService().getTelemetryData();

    // then
    String message = "ENGINE-03110 Required admin authenticated group or user or any of the following permissions:";
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog(CONTEXT_LOGGER, message);

    assertThat(filteredLog).hasSize(1);
    assertThat(filteredLog.get(0).getLevel()).isEqualTo(Level.DEBUG);
  }

}
