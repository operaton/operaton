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
package org.operaton.bpm.engine.test.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

/**
 * @author Daniel Meyer
 *
 */
class ReadOnlyIdentityServiceTest {

  protected static final String CONFIGURATION_RESOURCE = "org/operaton/bpm/engine/test/api/identity/read.only.identity.service.operaton.cfg.xml";

  @RegisterExtension
  public ProcessEngineExtension engineRule = ProcessEngineExtension.builder().configurationResource(CONFIGURATION_RESOURCE).build();
  
  protected IdentityService identityService;

  @BeforeEach
  void setUp() {
    identityService = engineRule.getIdentityService();

    assertThat(identityService.isReadOnly()).isTrue();
  }

  @Test
  void newUser() {
    // when/then
    assertThatThrownBy(() -> identityService.newUser("user"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void saveUser() {
    // when/then
    assertThatThrownBy(() -> identityService.saveUser(null))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void deleteUser() {
    // when/then
    assertThatThrownBy(() -> identityService.deleteUser("user"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void newGroup() {
    // when/then
    assertThatThrownBy(() -> identityService.newGroup("group"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void saveGroup() {
    // when/then
    assertThatThrownBy(() -> identityService.saveGroup(null))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void deleteGroup() {
    // when/then
    assertThatThrownBy(() -> identityService.deleteGroup("group"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void newTenant() {
    // when/then
    assertThatThrownBy(() -> identityService.newTenant("tenant"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void saveTenant() {
    // when/then
    assertThatThrownBy(() -> identityService.saveTenant(null))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void deleteTenant() {
    // when/then
    assertThatThrownBy(() -> identityService.deleteTenant("tenant"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void createGroupMembership() {
    // when/then
    assertThatThrownBy(() -> identityService.createMembership("user", "group"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void deleteGroupMembership() {
    // when/then
    assertThatThrownBy(() -> identityService.deleteMembership("user", "group"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void createTenantUserMembership() {
    // when/then
    assertThatThrownBy(() -> identityService.createTenantUserMembership("tenant", "user"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void createTenantGroupMembership() {
    // when/then
    assertThatThrownBy(() -> identityService.createTenantGroupMembership("tenant", "group"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void deleteTenantUserMembership() {
    // when/then
    assertThatThrownBy(() -> identityService.deleteTenantUserMembership("tenant", "user"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void deleteTenantGroupMembership() {
    // when/then
    assertThatThrownBy(() -> identityService.deleteTenantGroupMembership("tenant", "group"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessageContaining("This identity service implementation is read-only.");
  }

  @Test
  void checkPassword() {
    assertThat(identityService.checkPassword("user", "password")).isFalse();
  }

  @Test
  void createQuery() {
    assertThat(identityService.createUserQuery().list()).isNotNull();
    assertThat(identityService.createGroupQuery().list()).isNotNull();
    assertThat(identityService.createTenantQuery().list()).isNotNull();
  }

}
