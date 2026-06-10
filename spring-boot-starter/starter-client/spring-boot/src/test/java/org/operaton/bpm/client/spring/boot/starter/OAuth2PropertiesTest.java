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
package org.operaton.bpm.client.spring.boot.starter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.operaton.bpm.client.spring.boot.starter.OAuth2Properties.AssertionProperties.AssertionType;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
  "operaton.bpm.client.oauth2.token-uri=https://idp.example.test/token",
  "operaton.bpm.client.oauth2.client-id=external-task-client",
  "operaton.bpm.client.oauth2.client-secret=secret",
  "operaton.bpm.client.oauth2.scope=engine-rest/.default",
  "operaton.bpm.client.oauth2.audience=engine-rest",
  "operaton.bpm.client.oauth2.resource=api://engine-rest",
  "operaton.bpm.client.oauth2.expiry-buffer-seconds=45",
  "operaton.bpm.client.oauth2.additional-parameters.tenant=demo",
  "operaton.bpm.client.oauth2.assertion.type=jjwt",
  "operaton.bpm.client.oauth2.assertion.key-location=classpath:client-key.pem"
})
@ExtendWith(SpringExtension.class)
class OAuth2PropertiesTest extends ParsePropertiesHelper {

  @Test
  void shouldBindOAuth2Properties() {
    OAuth2Properties oauth2 = properties.getOauth2();

    assertThat(oauth2.getTokenUri()).isEqualTo("https://idp.example.test/token");
    assertThat(oauth2.getClientId()).isEqualTo("external-task-client");
    assertThat(oauth2.getClientSecret()).isEqualTo("secret");
    assertThat(oauth2.getScope()).isEqualTo("engine-rest/.default");
    assertThat(oauth2.getAudience()).isEqualTo("engine-rest");
    assertThat(oauth2.getResource()).isEqualTo("api://engine-rest");
    assertThat(oauth2.getExpiryBufferSeconds()).isEqualTo(45L);
    assertThat(oauth2.getAdditionalParameters()).containsEntry("tenant", "demo");
    assertThat(oauth2.getAssertion().getType()).isEqualTo(AssertionType.JJWT);
    assertThat(oauth2.getAssertion().getKeyLocation()).isEqualTo("classpath:client-key.pem");
  }
}
