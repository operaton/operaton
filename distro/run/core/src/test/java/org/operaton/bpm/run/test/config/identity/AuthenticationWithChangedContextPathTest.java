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
package org.operaton.bpm.run.test.config.identity;

import org.operaton.bpm.run.property.OperatonBpmRunAuthenticationProperties;
import org.operaton.bpm.run.test.AbstractRestTest;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles(profiles = {
    "test-changed-rest-context-path",
    "test-auth-enabled"
})
@TestPropertySource(properties = {
    OperatonBpmRunAuthenticationProperties.PREFIX + "=basic"
})
class AuthenticationWithChangedContextPathTest extends AbstractRestTest {

  @Test
  void shouldBlockRequest() {
    // given

    // when
    var response = testRestTemplate.exchange("/rest/task",
        HttpMethod.GET, HttpEntity.EMPTY, List.class);

    // then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getHeaders().get("WWW-Authenticate"))
        .containsExactly("Basic realm=\"default\"");
  }

}
