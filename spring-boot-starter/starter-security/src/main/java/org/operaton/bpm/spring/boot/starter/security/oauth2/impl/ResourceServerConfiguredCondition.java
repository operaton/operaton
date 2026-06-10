/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0; you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
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

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ResourceServerConfiguredCondition extends SpringBootCondition {

  private static final String[] RESOURCE_SERVER_PROPERTIES = {
      "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
      "spring.security.oauth2.resourceserver.jwt.issuer-uri",
      "spring.security.oauth2.resourceserver.jwt.public-key-location",
      "spring.security.oauth2.resourceserver.opaquetoken.introspection-uri",
      "spring.security.oauth2.resourceserver.opaque-token.introspection-uri"
  };

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    return isResourceServerConfigured(context) ?
        new ConditionOutcome(true, "OAuth2 resource server is configured") :
        new ConditionOutcome(false, "No OAuth2 resource server is configured");
  }

  static boolean isResourceServerConfigured(ConditionContext context) {
    Environment environment = context.getEnvironment();
    for (String property : RESOURCE_SERVER_PROPERTIES) {
      if (environment.containsProperty(property)) {
        return true;
      }
    }
    return false;
  }
}
