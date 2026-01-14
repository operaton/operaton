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
package org.operaton.bpm.engine.test.standalone.authentication;

import java.util.Date;

import ch.qos.logback.classic.Level;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.util.ClockUtil;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineTestExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.operaton.bpm.engine.test.util.DateTestUtil.parseDate;

class LoginAttemptsTest {

  private static final String INDENTITY_LOGGER = "org.operaton.bpm.engine.identity";

  @RegisterExtension
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
    .randomEngineName().closeEngineAfterAllTests()
    .configurator(configuration -> {
      configuration.setJdbcUrl("jdbc:h2:mem:LoginAttemptsTest;DB_CLOSE_DELAY=1000");
      configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP);
      configuration.setLoginMaxAttempts(5);
      configuration.setLoginDelayFactor(2);
      configuration.setLoginDelayMaxTime(30);
      configuration.setLoginDelayBase(1);
    })
    .build();
  @RegisterExtension
  ProcessEngineTestExtension engineTestRule = new ProcessEngineTestExtension(engineRule);
  @RegisterExtension
  ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension()
                                                      .watch(INDENTITY_LOGGER)
                                                      .level(Level.INFO);

  IdentityService identityService;
  ProcessEngine processEngine;

  @AfterEach
  void tearDown() {
    ClockUtil.setCurrentTime(new Date());
    for (User user : identityService.createUserQuery().list()) {
      identityService.deleteUser(user.getId());
    }
  }

  @Test
  void testUsuccessfulAttemptsResultInLockedUser() {
    // given
    User user = identityService.newUser("johndoe");
    user.setPassword("xxx");
    identityService.saveUser(user);

    Date now = parseDate("2000-01-24T13:00:00");
    ClockUtil.setCurrentTime(now);
    // when
    for (int i = 0; i <= 6; i++) {
      assertThat(identityService.checkPassword("johndoe", "invalid pwd")).isFalse();
      now = DateUtils.addSeconds(now, 5);
      ClockUtil.setCurrentTime(now);
    }

    // then
    assertThat(loggingRule.getFilteredLog(INDENTITY_LOGGER, "The user with id 'johndoe' is permanently locked.")).hasSize(1);
  }
}
