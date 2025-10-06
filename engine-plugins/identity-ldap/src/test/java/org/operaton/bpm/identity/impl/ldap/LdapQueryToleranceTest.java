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
package org.operaton.bpm.identity.impl.ldap;

import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.engine.test.junit5.ProcessEngineLoggingExtension;
import org.operaton.bpm.identity.ldap.util.LdapTestExtension;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Thorben Lindhauer
 *
 */
class LdapQueryToleranceTest {

  @RegisterExtension
  @Order(1)
  static LdapTestExtension ldapExtension = new LdapTestExtension();

  @RegisterExtension
  @Order(2)
  static ProcessEngineExtension engineRule = ProcessEngineExtension
          .builder()
          .configurationResource("invalid-id-attributes.cfg.xml")
          .configurator(ldapExtension::injectLdapUrlIntoProcessEngineConfiguration)
          .closeEngineAfterAllTests()
          .build();

  @RegisterExtension
  ProcessEngineLoggingExtension loggingRule = new ProcessEngineLoggingExtension()
      .level(Level.ERROR)
      .watch("org.operaton.bpm.identity.impl.ldap");

  ProcessEngine processEngine;

  @Test
  void testNotReturnGroupsWithNullId() {
    // given
    // LdapTestEnvironment creates six roles (groupOfNames) by default;
    // these won't return a group id, because they do not have the group id attribute
    // defined in the ldap plugin config
    // the plugin should not return such groups and instead log an error

    // when
    List<Group> groups = processEngine.getIdentityService().createGroupQuery().list();
    long count = processEngine.getIdentityService().createGroupQuery().count();

    // then
    // groups with id null were not returned
    assertThat(groups).isEmpty();
    assertThat(count).isZero();
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog("LDAP-00004 LDAP group query returned a group with id null.");
    assertThat(filteredLog).hasSize(12); // 2 queries * 6 roles (groupOfNames)
  }

  @Test
  void testNotReturnUsersWithNullId() {
    // given
    // LdapTestEnvironment creates 12 users by default;
    // these won't return a group id, because they do not have the group id attribute
    // defined in the ldap plugin config
    // the plugin should not return such groups and instead log an error

    // when
    List<User> users = processEngine.getIdentityService().createUserQuery().list();
    long count = processEngine.getIdentityService().createUserQuery().count();

    // then
    // groups with id null were not returned
    assertThat(users).isEmpty();
    assertThat(count).isZero();
    List<ILoggingEvent> filteredLog = loggingRule.getFilteredLog("LDAP-00004 LDAP user query returned a user with id null.");
    assertThat(filteredLog).hasSize(24); // 2 queries * 12 users
  }
}
