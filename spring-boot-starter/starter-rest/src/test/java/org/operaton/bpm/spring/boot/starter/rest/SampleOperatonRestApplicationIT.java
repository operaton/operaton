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

import java.io.ByteArrayInputStream;

import my.own.custom.spring.boot.project.SampleOperatonRestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;

import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.rest.dto.runtime.ProcessInstanceDto;
import org.operaton.bpm.engine.runtime.ProcessInstance;
import org.operaton.bpm.engine.runtime.VariableInstance;
import org.operaton.bpm.spring.boot.starter.property.OperatonBpmProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@AutoConfigureTestRestTemplate
@SpringBootTest(classes = SampleOperatonRestApplication.class, webEnvironment = RANDOM_PORT)
class SampleOperatonRestApplicationIT {

  private final TestRestTemplate testRestTemplate;
  private final RuntimeService runtimeService;
  private final OperatonBpmProperties operatonBpmProperties;

  @Autowired
  public SampleOperatonRestApplicationIT(TestRestTemplate testRestTemplate, RuntimeService runtimeService, OperatonBpmProperties operatonBpmProperties) {
      this.testRestTemplate = testRestTemplate;
      this.runtimeService = runtimeService;
      this.operatonBpmProperties = operatonBpmProperties;
  }

  @Test
  void restApiIsAvailable() {
    ResponseEntity<String> entity = testRestTemplate.getForEntity("/engine-rest/engine/", String.class);
    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(entity.getBody()).isEqualTo("[{\"name\":\"testEngine\"}]");
  }

  @Test
  void startProcessInstanceByCustomResource() {
    ResponseEntity<ProcessInstanceDto> entity = testRestTemplate.postForEntity("/engine-rest/process/start", HttpEntity.EMPTY, ProcessInstanceDto.class);
    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(entity.getBody()).isNotNull();

    // find the process instance
    final ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(entity.getBody().getId()).singleResult();
    assertThat(entity.getBody().getId()).isEqualTo(processInstance.getProcessInstanceId());
  }

  @Test
  void multipartFileUploadOperatonRestIsWorking() {
    final String variableName = "testvariable";
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestProcess");
    LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
    map.add("data", new ClassPathResource("/bpmn/test.bpmn"));
    map.add("valueType", "File");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.setContentDispositionFormData("data", "test.bpmn");

    HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
    ResponseEntity<String> exchange = testRestTemplate.exchange("/engine-rest/engine/{enginename}/process-instance/{id}/variables/{variableName}/data",
        HttpMethod.POST, requestEntity, String.class, operatonBpmProperties.getProcessEngineName(), processInstance.getId(), variableName);

    assertThat(exchange.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery().processInstanceIdIn(processInstance.getId()).variableName(variableName)
        .singleResult();
    ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream) variableInstance.getValue();
    assertThat(byteArrayInputStream.available()).isPositive();
  }

  @Test
  void fetchAndLockExternalTaskWithLongPollingIsRunning() {

    String requestJson = "{"
      + "  \"workerId\":\"aWorkerId\","
      + "  \"maxTasks\":2,"
      + "  \"topics\":"
      + "      [{\"topicName\": \"aTopicName\","
      + "      \"lockDuration\": 10000"
      + "      }]"
      + "}";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<String> requestEntity = new HttpEntity<>(requestJson, headers);
    ResponseEntity<String> entity = testRestTemplate.postForEntity("/engine-rest/engine/{enginename}/external-task/fetchAndLock", requestEntity, String.class,
      operatonBpmProperties.getProcessEngineName());
    assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(entity.getBody()).isEqualTo("[]");
  }

}
