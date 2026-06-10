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
package org.operaton.bpm.spring.boot.starter.security.oauth2;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@AutoConfigureTestRestTemplate
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/issuer/jwks",
    "operaton.bpm.oauth2.identity-provider.group-name-attribute=groups"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OperatonBpmSecurityAutoConfigResourceServerApplicationIT extends AbstractSpringSecurityIT {

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Autowired
  private JwtAuthenticationConverter jwtAuthenticationConverter;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @Test
  void testSpringSecurityAutoConfigurationCorrectlySet() {
    assertThat(getBeanForClass(OperatonSpringSecurityOAuth2CommonAutoConfiguration.class, webApplicationContext)).isNotNull();
    assertThat(getBeanForClass(OperatonSpringSecurityOAuth2EngineAutoConfiguration.class, webApplicationContext)).isNotNull();
    assertThat(getBeanForClass(OperatonSpringSecurityOAuth2WebappAutoConfiguration.class, webApplicationContext)).isNull();
    assertThat(getBeanForClass(OperatonBpmSpringSecurityDisableAutoConfiguration.class, webApplicationContext)).isNull();
  }

  @Test
  void testEngineRestWhitelistRemainsAvailableWithoutAuthentication() {
    ResponseEntity<String> entity = testRestTemplate.getForEntity(baseUrl + "/engine-rest/engine/", String.class);

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(entity.getBody()).isEqualTo(EXPECTED_NAME_DEFAULT);
  }

  @Test
  void testEngineRestResourceRequiresAuthentication() {
    ResponseEntity<String> entity = testRestTemplate.getForEntity(baseUrl + "/engine-rest/user/", String.class);

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void testEngineRestResourceAcceptsJwtAuthentication() {
    when(jwtDecoder.decode("token")).thenReturn(createJwt(List.of("management")));
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("token");

    ResponseEntity<String> entity = testRestTemplate.exchange(baseUrl + "/engine-rest/user/",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        String.class);

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(entity.getBody()).contains(AUTHORIZED_USER);
  }

  @Test
  void testJwtAuthenticationConverterUsesConfiguredGroupClaimAndPreferredUserName() {
    JwtAuthenticationToken authentication = (JwtAuthenticationToken) jwtAuthenticationConverter.convert(createJwt(List.of("sales", "support")));

    assertThat(authentication.getName()).isEqualTo(AUTHORIZED_USER);
    assertThat(authentication.getAuthorities())
        .extracting(authority -> authority.getAuthority())
        .contains("sales", "support");
  }

  private static Jwt createJwt(List<String> groups) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .subject(AUTHORIZED_USER)
        .claim("preferred_username", AUTHORIZED_USER)
        .claim("given_name", "Bob")
        .claim("family_name", "Builder")
        .claim("email", "bob@example.org")
        .claim("groups", groups)
        .build();
  }
}
