/*
 * Copyright CIB software GmbH and/or licensed to CIB software GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. CIB software licenses this file to you under the Apache License,
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
package org.operaton.bpm.identity.impl.scim;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.identity.scim.util.ScimTestEnvironment;
import org.operaton.bpm.identity.scim.util.ScimTestEnvironmentExtension;

/**
 * Tests for SCIM user queries.
 */
class ScimUserModifyTest {

  @RegisterExtension
  @Order(1)
  static ScimTestEnvironmentExtension scimExtension = new ScimTestEnvironmentExtension();

  @RegisterExtension
  @Order(2)
  static ProcessEngineExtension engineRule = ProcessEngineExtension.builder()
      .configurationResource("operaton.modify.cfg.xml")
      .configurator(scimExtension::injectScimUrlIntoProcessEngineConfiguration)
      .closeEngineAfterAllTests()
      .build();

  IdentityService identityService;
  ScimTestEnvironment scimTestEnvironment;

  @BeforeEach
  void setup() {
    scimTestEnvironment = scimExtension.getScimTestEnvironment();
  }

  @Test
  public void testCreateUser() {
    assertThat(!identityService.isReadOnly());
    ScimUserEntity user = (ScimUserEntity) identityService.newUser("oscar");
    user.setFirstName("Oscar");
    user.setLastName("The Crouch");
    user.setEmail("oscar@operaton.org");
    identityService.saveUser(user);
  }

  @Test
  public void testUpdateUser() {
    assertThat(!identityService.isReadOnly());
    ScimUserEntity user = (ScimUserEntity) identityService.newUser("oscar");
    user.setScimId("user-oscar");
    user.setFirstName("Oscar");
    user.setLastName("The (Even Cleaner) Crouch");
    user.setEmail("oscar@operaton.org");
    identityService.saveUser(user);
  }

  @Test
  public void testDeleteUser() {
    assertThat(!identityService.isReadOnly());  
    identityService.deleteUser("oscar");
  }
}
