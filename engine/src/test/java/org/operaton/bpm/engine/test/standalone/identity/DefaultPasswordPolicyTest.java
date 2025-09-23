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
package org.operaton.bpm.engine.test.standalone.identity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.exception.NullValueException;
import org.operaton.bpm.engine.identity.PasswordPolicy;
import org.operaton.bpm.engine.identity.PasswordPolicyResult;
import org.operaton.bpm.engine.identity.PasswordPolicyRule;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.identity.DefaultPasswordPolicyImpl;
import org.operaton.bpm.engine.impl.identity.PasswordPolicyDigitRuleImpl;
import org.operaton.bpm.engine.impl.identity.PasswordPolicyLengthRuleImpl;
import org.operaton.bpm.engine.impl.identity.PasswordPolicyLowerCaseRuleImpl;
import org.operaton.bpm.engine.impl.identity.PasswordPolicySpecialCharacterRuleImpl;
import org.operaton.bpm.engine.impl.identity.PasswordPolicyUpperCaseRuleImpl;
import org.operaton.bpm.engine.impl.identity.PasswordPolicyUserDataRuleImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Miklas Boskamp
 */
@ExtendWith(ProcessEngineExtension.class)
class DefaultPasswordPolicyTest {

  ProcessEngineConfigurationImpl processEngineConfiguration;
  IdentityService identityService;

  // enforces a minimum length of 10 characters, at least one upper, one
  // lower case, one digit and one special character
  protected PasswordPolicy policy = new DefaultPasswordPolicyImpl();

  @BeforeEach
  void init() {
    processEngineConfiguration
      .setPasswordPolicy(new DefaultPasswordPolicyImpl())
      .setEnablePasswordPolicy(true);
  }

  @AfterEach
  void resetProcessEngineConfig() {
    processEngineConfiguration
      .setPasswordPolicy(null)
      .setEnablePasswordPolicy(false);
  }

  @Test
  void testGoodPassword() {
    PasswordPolicyResult result = identityService.checkPasswordAgainstPolicy(policy, "LongPas$w0rd");
    assertThat(result.getViolatedRules()).isEmpty();
    assertThat(result.getFulfilledRules()).hasSize(6);
    assertThat(result.isValid()).isTrue();
  }

  @Test
  void shouldCheckValidPassword_WithoutPassingPolicy() {
    // given

    // when
    PasswordPolicyResult result = identityService.checkPasswordAgainstPolicy("LongPas$w0rd");

    // then
    assertThat(result).isNotNull();
  }

  @Test
  void testPasswordWithoutLowerCase() {
    PasswordPolicyResult result = identityService.checkPasswordAgainstPolicy(policy, "LONGPAS$W0RD");
    checkThatPasswordWasInvalid(result);

    PasswordPolicyRule rule = result.getViolatedRules().get(0);
    assertThat(rule.getPlaceholder()).isEqualTo(PasswordPolicyLowerCaseRuleImpl.PLACEHOLDER);
    assertThat(rule).isInstanceOf(PasswordPolicyLowerCaseRuleImpl.class);
  }

  @Test
  void testPasswordWithoutUpperCase() {
    PasswordPolicyResult result = identityService.checkPasswordAgainstPolicy(policy, "longpas$w0rd");
    checkThatPasswordWasInvalid(result);

    PasswordPolicyRule rule = result.getViolatedRules().get(0);
    assertThat(rule.getPlaceholder()).isEqualTo(PasswordPolicyUpperCaseRuleImpl.PLACEHOLDER);
    assertThat(rule).isInstanceOf(PasswordPolicyUpperCaseRuleImpl.class);
  }

  @Test
  void testPasswordWithoutSpecialChar() {
    PasswordPolicyResult result = identityService.checkPasswordAgainstPolicy(policy, "LongPassw0rd");
    checkThatPasswordWasInvalid(result);

    PasswordPolicyRule rule = result.getViolatedRules().get(0);
    assertThat(rule.getPlaceholder()).isEqualTo(PasswordPolicySpecialCharacterRuleImpl.PLACEHOLDER);
    assertThat(rule).isInstanceOf(PasswordPolicySpecialCharacterRuleImpl.class);
  }

  @Test
  void testPasswordWithoutDigit() {
    PasswordPolicyResult result = identityService.checkPasswordAgainstPolicy(policy, "LongPas$word");
    checkThatPasswordWasInvalid(result);

    PasswordPolicyRule rule = result.getViolatedRules().get(0);
    assertThat(rule.getPlaceholder()).isEqualTo(PasswordPolicyDigitRuleImpl.PLACEHOLDER);
    assertThat(rule).isInstanceOf(PasswordPolicyDigitRuleImpl.class);
  }

  @Test
  void testShortPassword() {
    PasswordPolicyResult result = identityService.checkPasswordAgainstPolicy(policy, "Pas$w0rd");
    checkThatPasswordWasInvalid(result);

    PasswordPolicyRule rule = result.getViolatedRules().get(0);
    assertThat(rule.getPlaceholder()).isEqualTo(PasswordPolicyLengthRuleImpl.PLACEHOLDER);
    assertThat(rule).isInstanceOf(PasswordPolicyLengthRuleImpl.class);
  }

  @Test
  void shouldThrowNullValueException_policyNull() {
    // given

    // when/then
    assertThatThrownBy(() -> identityService.checkPasswordAgainstPolicy(null, "Pas$w0rd"))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("policy is null");
  }

  @Test
  void shouldThrowNullValueException_passwordNull() {
    // given

    // when/then
    assertThatThrownBy(() -> identityService.checkPasswordAgainstPolicy(policy, null))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("password is null");
  }

  @Test
  void shouldGetPasswordPolicy() {
    // given

    // then
    PasswordPolicy passwordPolicy = identityService.getPasswordPolicy();

    // when
    assertThat(passwordPolicy).isNotNull();
  }

  @Test
  void shouldUpdateUserDetailsWithoutPolicyCheck() {
    // given
    // first, create a new user
    User user = identityService.newUser("johndoe");
    user.setFirstName("John");
    user.setLastName("Doe");
    user.setEmail("john@doe.com");
    user.setPassword("Passw0rds!");
    identityService.saveUser(user);

    // when
    // fetch and update the user
    user = identityService.createUserQuery().userId("johndoe").singleResult();
    user.setEmail("jane@donnel.com");
    user.setFirstName("Jane");
    user.setLastName("Donnel");
    identityService.saveUser(user);

    // then
    user = identityService.createUserQuery().userId("johndoe").singleResult();
    assertThat(user.getFirstName()).isEqualTo("Jane");
    assertThat(user.getLastName()).isEqualTo("Donnel");
    assertThat(user.getEmail()).isEqualTo("jane@donnel.com");
    assertThat(identityService.checkPassword("johndoe", "Passw0rds!")).isTrue();

    identityService.deleteUser(user.getId());
  }

  @Test
  void shouldCheckUserRuleWithPolicyPassed() {
    // given
    User user = identityService.newUser("myUserId");
    String candidatePassword = "myUserId";

    // when
    PasswordPolicyResult result = identityService.checkPasswordAgainstPolicy(policy, candidatePassword, user);

    // then
    assertThat(result.getViolatedRules())
        .extracting("placeholder")
        .contains(PasswordPolicyUserDataRuleImpl.PLACEHOLDER);
  }

  @Test
  void shouldCheckPasswordNull() {
    // given
    User user = identityService.newUser("myUserId");
    String candidatePassword = null;

    // when/then
    assertThatThrownBy(() -> identityService.checkPasswordAgainstPolicy(candidatePassword, user))
      .isInstanceOf(NullValueException.class)
      .hasMessageContaining("password is null");
  }

  @Test
  void shouldCheckPasswordEmpty() {
    // given
    User user = identityService.newUser("myUserId");
    String candidatePassword = "";

    // when
    PasswordPolicyResult result =
        identityService.checkPasswordAgainstPolicy(candidatePassword, user);

    // then
    assertThat(result.getFulfilledRules())
        .extracting("placeholder")
        .contains(PasswordPolicyUserDataRuleImpl.PLACEHOLDER);
  }

  @Test
  void shouldCheckUserNull() {
    // given
    User user = null;
    String candidatePassword = "my-password";

    // when
    PasswordPolicyResult result =
        identityService.checkPasswordAgainstPolicy(candidatePassword, user);

    // then
    assertThat(result.getFulfilledRules())
        .extracting("placeholder")
        .contains(PasswordPolicyUserDataRuleImpl.PLACEHOLDER);
  }

  private void checkThatPasswordWasInvalid(PasswordPolicyResult result) {
    assertThat(result.getViolatedRules()).hasSize(1);
    assertThat(result.getFulfilledRules()).hasSize(5);
    assertThat(result.isValid()).isFalse();
  }
}
