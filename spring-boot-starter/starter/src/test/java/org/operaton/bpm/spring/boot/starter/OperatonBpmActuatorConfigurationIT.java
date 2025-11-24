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
package org.operaton.bpm.spring.boot.starter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import org.operaton.bpm.spring.boot.starter.test.nonpa.TestApplication;

import static org.springframework.test.util.AssertionErrors.assertTrue;

@AutoConfigureTestRestTemplate
@SpringBootTest(classes = {TestApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OperatonBpmActuatorConfigurationIT extends AbstractOperatonAutoConfigurationIT {

  private final TestRestTemplate testRestTemplate;

  @Autowired
  public OperatonBpmActuatorConfigurationIT(TestRestTemplate testRestTemplate) {
      this.testRestTemplate = testRestTemplate;
  }

  @Test
  void jobExecutorHealthIndicatorTest() {
    final String body = getHealthBody();
    assertTrue("wrong body " + body, body.contains("jobExecutor\":{\"status\":\"UP\""));
  }

  @Test
  void processEngineHealthIndicatorTest() {
    final String body = getHealthBody();
    assertTrue("wrong body " + body, body.contains("processEngine\":{\"status\":\"UP\",\"details\":{\"name\":\"testEngine\"}}"));
  }

  private String getHealthBody() {
    ResponseEntity<String> entity = testRestTemplate.getForEntity("/actuator/health", String.class);
    return entity.getBody();
  }
}
