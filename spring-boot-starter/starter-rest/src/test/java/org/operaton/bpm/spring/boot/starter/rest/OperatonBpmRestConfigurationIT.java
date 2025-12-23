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
package org.operaton.bpm.spring.boot.starter.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.operaton.bpm.engine.rest.dto.repository.ProcessDefinitionDto;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;
import org.operaton.bpm.spring.boot.starter.rest.test.TestRestApplication;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestRestTemplate
@SpringBootTest(classes = {TestRestApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OperatonBpmRestConfigurationIT {

  private final TestRestTemplate testRestTemplate;
  private final OperatonBpmProperties operatonBpmProperties;

  @Autowired
  public OperatonBpmRestConfigurationIT(TestRestTemplate testRestTemplate, OperatonBpmProperties operatonBpmProperties) {
      this.testRestTemplate = testRestTemplate;
      this.operatonBpmProperties = operatonBpmProperties;
  }

  @Test
  void processDefinitionTest() {
    // start process
    testRestTemplate.postForEntity("/engine-rest/start/process", HttpEntity.EMPTY, String.class);

    ResponseEntity<ProcessDefinitionDto> entity = testRestTemplate.getForEntity("/engine-rest/engine/{engineName}/process-definition/key/TestProcess/",
        ProcessDefinitionDto.class, operatonBpmProperties.getProcessEngineName());

    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(entity.getBody().getKey()).isEqualTo("TestProcess");
  }
}
