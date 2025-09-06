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
package org.operaton.bpm.spring.boot.starter.configuration.impl.custom;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.util.ReflectionTestUtils;

import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.test.helper.StandaloneInMemoryTestConfiguration;
import org.operaton.bpm.spring.boot.starter.util.SpringBootProcessEngineLogger;

import static org.assertj.core.api.Assertions.assertThat;

class CreateAdminUserConfigurationTest {
  @RegisterExtension
  static final ProcessEngineLoggingExtension loggingExtension = new ProcessEngineLoggingExtension()
    .watch(SpringBootProcessEngineLogger.PACKAGE, Level.DEBUG);

  static CreateAdminUserConfiguration createAdminUserConfiguration() {
    OperatonBpmProperties properties = new OperatonBpmProperties();
    properties.getAdminUser().setId("admin");
    properties.getAdminUser().setPassword("password");

    var adminUserConfiguration = new CreateAdminUserConfiguration(properties);
    adminUserConfiguration.init();
    return adminUserConfiguration;
  }


  @Test
  void createAdminUser() {
    ProcessEngineExtension processEngineExtension = new StandaloneInMemoryTestConfiguration(createAdminUserConfiguration()).extension();
    User user = processEngineExtension.getIdentityService().createUserQuery().userId("admin").singleResult();
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo("admin@localhost");
  }

  @Test
  void shouldLogInitialAdminUserCreationOnDebug() {
    var processEngineConfiguration = new StandaloneInMemoryTestConfiguration(createAdminUserConfiguration());
    processEngineConfiguration.buildProcessEngine();
    verifyLogs(Level.DEBUG, "STARTER-SB010 Creating initial Admin User: AdminUserProperty[id=admin, firstName=Admin, lastName=Admin, email=admin@localhost, password=******]");
  }

  protected void verifyLogs(Level logLevel, String message) {
    List<ILoggingEvent> logs = loggingExtension.getLog();
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getLevel()).isEqualTo(logLevel);
    assertThat(logs.get(0).getFormattedMessage()).containsIgnoringCase(message);
  }

}
