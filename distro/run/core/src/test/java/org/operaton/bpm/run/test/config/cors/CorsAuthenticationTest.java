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
package org.operaton.bpm.run.test.config.cors;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.identity.Group;
import org.operaton.bpm.engine.impl.persistence.entity.GroupEntity;
import org.operaton.bpm.run.test.AbstractRestTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Note: To run this test via an IDE you must set the system property
 * {@code sun.net.http.allowRestrictedHeaders} to {@code true}.
 * (e.g. System.setProperty("sun.net.http.allowRestrictedHeaders", "true");)
 *
 * @see <a href="https://jira.camunda.com/browse/CAM-11290">CAM-11290</a>
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles(profiles = {"test-cors-enabled", "test-auth-enabled", "test-demo-user"}, inheritProfiles = false)
class CorsAuthenticationTest extends AbstractRestTest {

  TestRestTemplate authTestRestTemplate;

  @Autowired
  ProcessEngine processEngine;

  @BeforeEach
  void init() {
    authTestRestTemplate = testRestTemplate.withBasicAuth("demo", "demo");
  }

  @AfterEach
  void tearDown() {
    processEngine.getIdentityService().deleteGroup("groupId");
  }

  @Test
  void shouldPassAuthenticatedSimpleCorsRequest() {
    // given
    // cross-origin but allowed through wildcard
    String origin = "http://other.origin";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.ORIGIN, origin);

    // when
    var response = authTestRestTemplate.exchange(CONTEXT_PATH + "/task", HttpMethod.GET, new HttpEntity<>(headers), List.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getAccessControlAllowOrigin()).contains("*");
  }

  @Test
  void shouldPassAuthenticatedCorsRequest() {
    // given
    // cross-origin but allowed through wildcard
    String origin = "http://other.origin";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.ORIGIN, origin);

    Group group = new GroupEntity("groupId");

    // create group
    processEngine.getIdentityService().saveGroup(new GroupEntity("groupId"));

    group.setName("updatedGroupName");

    // when
    var response = authTestRestTemplate.exchange(CONTEXT_PATH + "/group/" + group.getId(), HttpMethod.PUT, new HttpEntity<>(group, headers),
        String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getHeaders().getAccessControlAllowOrigin()).contains("*");
  }

  @Test
  void shouldNotPassNonAuthenticatedCorsRequest() {
    // given
    // cross-origin but allowed through wildcard
    String origin = "http://other.origin";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.ORIGIN, origin);

    // when
    var response = testRestTemplate.exchange(CONTEXT_PATH + "/task", HttpMethod.GET, new HttpEntity<>(headers), List.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getHeaders().get("WWW-Authenticate")).containsExactly("Basic realm=\"default\"");
  }

  @Test
  void shouldPassNonAuthenticatedPreflightRequest() {
    // given
    // cross-origin but allowed through wildcard
    String origin = "http://other.origin";

    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.HOST, "localhost");
    headers.add(HttpHeaders.ORIGIN, origin);
    headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.PUT.name());
    headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, HttpHeaders.ORIGIN);

    // when
    var response = testRestTemplate.exchange(CONTEXT_PATH + "/task", HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
