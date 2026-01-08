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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.identity.PasswordPolicyResult;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.identity.DefaultPasswordPolicyImpl;
import org.operaton.bpm.engine.impl.identity.PasswordPolicyUserDataRuleImpl;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ExtendWith(ProcessEngineExtension.class)
public class PasswordPolicyUserDataTest {

  public static final String CANDIDATE_PASSWORD = "mypassword";

  ProcessEngineConfigurationImpl processEngineConfiguration;
  IdentityService identityService;

  @BeforeEach
  void init() {
    processEngineConfiguration
        .setEnablePasswordPolicy(true)
        .setPasswordPolicy(new DefaultPasswordPolicyImpl());
  }

  @AfterEach
  void reset() {
    processEngineConfiguration
        .setEnablePasswordPolicy(false)
        .setPasswordPolicy(null);
  }

  @Test
  void shouldViolateRule() {
    // given
    String attributeValue = CANDIDATE_PASSWORD;

    // when
    Map<String, PasswordPolicyResult> results = getResultsForAttributes(attributeValue);

    // then
    assertRuleViolated(results);
  }

  @Test
  void shouldFulfillRule() {
    // given
    String attributeValue = "another value";

    // when
    Map<String, PasswordPolicyResult> results = getResultsForAttributes(attributeValue);

    // then
    assertRuleFulfilled(results);
  }

  @Test
  void shouldViolateRuleOnIgnoreCaseAttributeValue() {
    // given
    String attributeValue = "MYPASSWORD";

    // when
    Map<String, PasswordPolicyResult> results = getResultsForAttributes(attributeValue);

    // then
    assertRuleViolated(results);
  }

  @Test
  void shouldFulfillRuleOnEmptyAttributeValue() {
    // given
    String attributeValue = "";

    // when
    Map<String, PasswordPolicyResult> results = getResultsForAttributes(attributeValue);

    // then
    assertRuleFulfilled(results);
  }

  @Test
  void shouldFulfillRuleOnNullAttributeValue() {
    // given
    String attributeValue = null;

    // when
    Map<String, PasswordPolicyResult> results = getResultsForAttributes(attributeValue);

    // then
    assertRuleFulfilled(results);
  }

  // helper ////////////////////////////////////////////////////////////////////////////////////////

  public Map<String, PasswordPolicyResult> getResultsForAttributes(String attrValue) {
    Map<String, PasswordPolicyResult> results = new HashMap<>();

    List<Method> methods = Arrays.asList(User.class.getMethods());

    methods.forEach(method -> {
      String methodName = method.getName();
      if (methodName.startsWith("set") && !"setPassword".equals(methodName)) {
        User user = identityService.newUser("");

        try {
          method.invoke(user, attrValue);

        } catch (IllegalAccessException | InvocationTargetException e) {
          fail(e.getMessage());

        }

        PasswordPolicyResult passwordPolicyResult =
            identityService.checkPasswordAgainstPolicy(CANDIDATE_PASSWORD, user);

        results.put(methodName, passwordPolicyResult);
      }
    });

    return results;
  }

  protected void assertRuleViolated(Map<String, PasswordPolicyResult> results) {
    results.forEach((methodName, result) -> {
      try {
        assertThat(result.getViolatedRules())
            .extracting("placeholder")
            .contains(PasswordPolicyUserDataRuleImpl.PLACEHOLDER);

      } catch (AssertionError e) {
        fail("Rule not violated with %s:%s".formatted(methodName, e.getMessage()));

      }
    });
  }

  protected void assertRuleFulfilled(Map<String, PasswordPolicyResult> results) {
    results.forEach((methodName, result) -> {
      try {
        assertThat(result.getFulfilledRules())
            .extracting("placeholder")
            .contains(PasswordPolicyUserDataRuleImpl.PLACEHOLDER);

      } catch (AssertionError e) {
        fail("Rule not fulfilled with %s:%s".formatted(methodName, e.getMessage()));

      }
    });
  }

}
