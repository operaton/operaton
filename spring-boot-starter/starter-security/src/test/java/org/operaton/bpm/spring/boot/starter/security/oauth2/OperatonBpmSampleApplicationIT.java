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
package org.operaton.bpm.spring.boot.starter.security.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestRestTemplate
class OperatonBpmSampleApplicationIT extends AbstractSpringSecurityIT {

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Test
  void testSpringSecurityAutoConfigurationCorrectlySet() {
    // given oauth2 client not configured
    // when retrieving config beans then only SpringSecurityDisabledAutoConfiguration is present
    assertThat(getBeanForClass(OperatonSpringSecurityOAuth2AutoConfiguration.class, webApplicationContext)).isNull();
    assertThat(getBeanForClass(OperatonBpmSpringSecurityDisableAutoConfiguration.class, webApplicationContext)).isNotNull();
  }

  @Test
  void testWebappApiIsAvailableAndRequiresAuthorization() {
    // given oauth2 client disabled
    // when calling the webapp api
    ResponseEntity<String> entity = testRestTemplate.getForEntity(baseUrl + "/operaton/api/engine/engine/default/user", String.class);
    // then webapp api returns unauthorized
    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void testRestApiIsAvailable() {
    // given oauth2 client disabled
    // when calling the rest api
    ResponseEntity<String> entity = testRestTemplate.getForEntity(baseUrl + "/engine-rest/engine/", String.class);
    // then rest api is accessible
    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(entity.getBody()).isEqualTo(EXPECTED_NAME_DEFAULT);
  }
}
