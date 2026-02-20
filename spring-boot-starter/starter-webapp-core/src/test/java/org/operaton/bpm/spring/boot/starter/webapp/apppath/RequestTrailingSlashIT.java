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
package org.operaton.bpm.spring.boot.starter.webapp.apppath;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.operaton.bpm.spring.boot.starter.webapp.WebappTestApp;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestRestTemplate
@SpringBootTest(
  classes = {WebappTestApp.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RequestTrailingSlashIT {

  public static final List<String> REDIRECT_PATHS = List.of("/app", "/app/cockpit", "/app/admin", "/app/tasklist", "/app/welcome");

  @Autowired
  private TestRestTemplate client;


  @LocalServerPort
  public int port;

  @Test
  void shouldRedirectPathWithMissingTrailingSlash() {
    // given
    List<ResponseEntity<String>> responses = new ArrayList<>();

    // when calling different paths with and without trailing slashes
    for (String path : REDIRECT_PATHS) {
      String url = "http://localhost:" + port + "/operaton" + path;
      responses.add(client.getForEntity(url, String.class));
      responses.add(client.getForEntity(url + "/", String.class));
    }

    // then all paths should be found
    assertThat(responses).extracting("statusCode").containsOnly(HttpStatus.OK);
  }

}
