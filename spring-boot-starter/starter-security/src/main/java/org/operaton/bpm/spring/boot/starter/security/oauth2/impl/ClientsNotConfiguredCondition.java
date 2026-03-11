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
package org.operaton.bpm.spring.boot.starter.security.oauth2.impl;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition that matches if no {@code spring.security.oauth2.client.registration} properties are defined
 */
public class ClientsNotConfiguredCondition extends SpringBootCondition {

  private static final String OAUTH2_CLIENT_REGISTRATION_PROPERTY_PREFIX = "spring.security.oauth2.client.registration";

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return hasClientRegistrations(context) ?
        new ConditionOutcome(false, "OAuth2 client is configured") :
        new ConditionOutcome(true, "No OAuth2 client is configured");
  }

  /**
   * Checks if any OAuth2 client registrations are present in the active application properties.
   *
   * @return true if at least one client registration is present and set, false otherwise
   */
  private boolean hasClientRegistrations(ConditionContext context) {
    Binder binder = Binder.get(context.getEnvironment());
    return binder.bind(OAUTH2_CLIENT_REGISTRATION_PROPERTY_PREFIX, Map.class)
        .map(map -> !map.isEmpty())
        .orElse(false);
  }
}
