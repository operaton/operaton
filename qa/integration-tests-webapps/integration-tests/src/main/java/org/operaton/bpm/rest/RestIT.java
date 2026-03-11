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
package org.operaton.bpm.rest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.operaton.bpm.AbstractWebIntegrationTest;
import org.operaton.bpm.engine.rest.hal.Hal;
import org.operaton.bpm.engine.rest.mapper.JacksonConfigurator;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class RestIT extends AbstractWebIntegrationTest {

  private static final String ENGINE_DEFAULT_PATH = "engine/default";

  private static final String PROCESS_DEFINITION_PATH = ENGINE_DEFAULT_PATH + "/process-definition";

  private static final String JOB_DEFINITION_PATH = ENGINE_DEFAULT_PATH + "/job-definition";

  private static final String TASK_PATH = ENGINE_DEFAULT_PATH + "/task";

  private static final String FILTER_PATH = ENGINE_DEFAULT_PATH + "/filter";

  private static final String HISTORIC_DETAIL_PATH = ENGINE_DEFAULT_PATH + "/history/detail";

  private static final String PROCESS_INSTANCE_PATH = ENGINE_DEFAULT_PATH + "/process-instance";

  private static final String SCHEMA_LOG_PATH = ENGINE_DEFAULT_PATH + "/schema/log";

  private static final Logger log = LoggerFactory.getLogger(RestIT.class);

  @BeforeEach
  void createClient() {
    preventRaceConditions();
    createClient(getRestCtxPath());
  }

  @Test
  void testScenario() throws Exception {
    // get process definitions for default engine
    log.info("Checking {}{}", appBasePath, PROCESS_DEFINITION_PATH);
    HttpResponse<JsonNode> response = Unirest.get(appBasePath + PROCESS_DEFINITION_PATH)
            .header(ACCEPT, APPLICATION_JSON)
            .asJson();

    assertThat(response.getStatus()).isEqualTo(200);

    JSONArray definitionsJson = response.getBody().getArray();

    // invoice example
    assertThat(definitionsJson).hasSize(3);

    // order of results is not consistent between database types
    for (int i = 0; i < definitionsJson.length(); i++) {
      JSONObject definitionJson = definitionsJson.getJSONObject(i);
      assertThat(definitionJson.isNull("description")).isTrue();
      assertThat(definitionJson.getBoolean("suspended")).isFalse();
      if ("ReviewInvoice".equals(definitionJson.getString("key"))) {
        assertThat(definitionJson.getString("category")).isEqualTo("http://bpmn.io/schema/bpmn");
        assertThat(definitionJson.getString("name")).isEqualTo("Review Invoice");
        assertThat(definitionJson.getString("resource")).isEqualTo("reviewInvoice.bpmn");
      } else if ("invoice".equals(definitionJson.getString("key"))) {
        assertThat(definitionJson.getString("category")).isEqualTo("http://www.omg.org/spec/BPMN/20100524/MODEL");
        assertThat(definitionJson.getString("name")).isEqualTo("Invoice Receipt");
        assertThat(definitionJson.getString("resource")).matches("invoice\\.v[1,2]\\.bpmn");
      } else {
        fail("Unexpected definition key in response JSON.");
      }
    }
  }

  @Test
  void assertJodaTimePresent() {
    log.info("Checking {}{}", appBasePath, TASK_PATH);

    HttpResponse<JsonNode> response = Unirest.get(appBasePath + TASK_PATH)
            .queryString("dueAfter", "2000-01-01T00:00:00.000+0200")
            .header(ACCEPT, APPLICATION_JSON)
            .asJson();

    assertThat(response.getStatus()).isEqualTo(200);

    JSONArray definitionsJson = response.getBody().getArray();
    assertThat(definitionsJson.length()).isEqualTo(4);
  }

  @Test
  void testDelayedJobDefinitionSuspension() {
    log.info("Checking {}{}/suspended", appBasePath, JOB_DEFINITION_PATH);

    // Create request body as a Map (or you can use a custom DTO if required)
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("processDefinitionKey", "jobExampleProcess");
    requestBody.put("suspended", true);
    requestBody.put("includeJobs", true);
    requestBody.put("executionDate", "2014-08-25T13:55:45");

    HttpResponse<String> response = Unirest.put(appBasePath + JOB_DEFINITION_PATH + "/suspended")
            .header(ACCEPT, APPLICATION_JSON)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .body(requestBody)
            .asString();

    assertThat(response.getStatus()).isEqualTo(204);
  }

  @Test
  void testTaskQueryContentType() {
    String resourcePath = appBasePath + TASK_PATH;
    log.info("Checking {}", resourcePath);
    assertMediaTypesOfResource(resourcePath, false);
  }

  @Test
  void testSingleTaskContentType() {
    // get id of first task
    String taskId = getFirstTask().getString("id");

    String resourcePath = appBasePath + TASK_PATH + "/" + taskId;
    log.info("Checking {}", resourcePath);
    assertMediaTypesOfResource(resourcePath, false);
  }

  @Test
  void testTaskFilterResultContentType() {
    // create filter for first task, so single result will not throw an exception
    JSONObject firstTask = getFirstTask();
    Map<String, Object> query = new HashMap<>();
    query.put("taskDefinitionKey", firstTask.getString("taskDefinitionKey"));
    query.put("processInstanceId", firstTask.getString("processInstanceId"));

    Map<String, Object> filter = new HashMap<>();
    filter.put("resourceType", "Task");
    filter.put("name", "IT Test Filter");
    filter.put("query", query);

    HttpResponse<JsonNode> response = Unirest.post(appBasePath + FILTER_PATH + "/create")
            .header(ACCEPT, APPLICATION_JSON)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .body(filter)
            .asJson();

    assertThat(response.getStatus()).isEqualTo(200);
    String filterId = response.getBody().getObject().getString("id");

    // Check the filter resource (list)
    String resourcePathList = "%s%s/%s/list".formatted(appBasePath, FILTER_PATH, filterId);
    log.info("Checking {}", resourcePathList);
    assertMediaTypesOfResource(resourcePathList, true);


    // Check the filter resource (singleResult)
    String resourcePathSingleResult = "%s%s/%s/singleResult".formatted(appBasePath, FILTER_PATH, filterId);
    log.info("Checking {}", resourcePathSingleResult);
    assertMediaTypesOfResource(resourcePathSingleResult, true);

    // delete test filter
    HttpResponse<String> deleteResponse = Unirest.delete(appBasePath + FILTER_PATH + "/" + filterId).asString();
    assertThat(deleteResponse.getStatus()).isEqualTo(204);

  }

  @Test
  void shouldSerializeDateWithDefinedFormat() {
    // when
    HttpResponse<JsonNode> response = Unirest.get(appBasePath + SCHEMA_LOG_PATH)
            .header(ACCEPT, APPLICATION_JSON)
            .asJson();

    // Then
    assertThat(response.getStatus()).isEqualTo(200);
    JSONObject logElement = response.getBody().getArray().getJSONObject(0);

    String timestamp = logElement.getString("timestamp");
    try {
      new SimpleDateFormat(JacksonConfigurator.DEFAULT_DATE_FORMAT).parse(timestamp);
    } catch (ParseException pex) {
      fail("Couldn't parse timestamp from schema log: " + timestamp);
    }
  }

  /**
   * Tests that a feature implemented via Jackson-2 annotations works:
   * polymorphic serialization of historic details
   */
  @Test
  void testPolymorphicSerialization() {
    JSONObject historicVariableUpdate = getFirstHistoricVariableUpdates();

    // variable update specific property
    assertThat(historicVariableUpdate.has("variableName")).isTrue();
  }

  /**
   * Uses Jackson's object mapper directly
   */
  @Test
  void testProcessInstanceQuery() {
    // Send GET request with query parameter and accept header
    HttpResponse<JsonNode> response = Unirest.get(appBasePath + PROCESS_INSTANCE_PATH)
            .queryString("variables", "invoiceNumber_eq_GPFE-23232323")
            .header(ACCEPT, APPLICATION_JSON)
            .asJson();

    JSONArray instancesJson = response.getBody().getArray();
    // Assert the response status is 200
    assertThat(response.getStatus()).isEqualTo(200);

    // Assert the number of instances in the response
    // The response is expected to be a JSON array, so use instancesJson.size() to get the length
    assertThat(instancesJson).hasSize(2);
  }

  @Test
  void testComplexObjectJacksonSerialization() {
    HttpResponse<JsonNode> response = Unirest.get(appBasePath + PROCESS_DEFINITION_PATH + "/statistics")
            .queryString("incidents", "true")
            .header(ACCEPT, APPLICATION_JSON)
            .asJson();

    JSONArray definitionStatistics = response.getBody().getArray();
    assertThat(response.getStatus()).isEqualTo(200);

    // Assert the length of the definition statistics array
    assertThat(definitionStatistics).hasSize(3);

    // Check that the definition is also serialized correctly
    for (int i = 0; i < definitionStatistics.length(); i++) {
      JSONObject definitionStatistic = definitionStatistics.getJSONObject(i);

      // Assert the class type
      assertThat(definitionStatistic.getString("@class")).isEqualTo(
              "org.operaton.bpm.engine.rest.dto.repository.ProcessDefinitionStatisticsResultDto");

      // Assert the incidents length
      assertThat(definitionStatistic.getJSONArray("incidents")).isEmpty();

      // Get the definition object
      var definition = definitionStatistic.getJSONObject("definition");

      // Check the name contains "invoice" (case-insensitive)
      assertThat(definition.getString("name").toLowerCase()).contains("invoice");

      // Check if the definition is not suspended
      assertThat(definition.getBoolean("suspended")).isFalse();
    }
  }

  @Test
  void testOptionsRequest() {
    // Given
    String resourcePath = appBasePath + FILTER_PATH;
    log.info("Send OPTIONS request to {}", resourcePath);

    // Send OPTIONS request
    HttpResponse<JsonNode> response = Unirest.options(resourcePath).asJson();

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(200);

    JSONObject entity = response.getBody().getObject();
    assertThat(entity.has("links")).isTrue();
  }

  @Test
  void testEmptyBodyFilterIsActive() {
    HttpResponse<String> response = Unirest.post(appBasePath + FILTER_PATH + "/create")
            .header(ACCEPT, APPLICATION_JSON)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .body("")
            .asString();

    assertThat(response.getStatus()).isEqualTo(400);
  }

  protected JSONObject getFirstTask() {
    HttpResponse<JsonNode> response = Unirest.get(appBasePath + TASK_PATH)
            .header(ACCEPT, APPLICATION_JSON)
            .asJson();

    JSONArray tasks = response.getBody().getArray();
    return tasks.getJSONObject(0);
  }

  protected JSONObject getFirstHistoricVariableUpdates() {
    HttpResponse<JsonNode> response = Unirest.get(appBasePath + HISTORIC_DETAIL_PATH)
            .queryString("variableUpdates", "true")
            .header(ACCEPT, APPLICATION_JSON)
            .asJson();

    JSONArray updates = response.getBody().getArray();
    return updates.getJSONObject(0);
  }

  protected void assertMediaTypesOfResource(String resourcePath, boolean postSupported) {
    assertMediaTypes(resourcePath, postSupported, APPLICATION_JSON);
    assertMediaTypes(resourcePath, postSupported, APPLICATION_JSON, "*/*");
    assertMediaTypes(resourcePath, postSupported, APPLICATION_JSON, APPLICATION_JSON);
    assertMediaTypes(resourcePath, postSupported, Hal.APPLICATION_HAL_JSON, Hal.APPLICATION_HAL_JSON);
    assertMediaTypes(resourcePath, postSupported, Hal.APPLICATION_HAL_JSON, Hal.APPLICATION_HAL_JSON, "application/json; q=0.5");
    assertMediaTypes(resourcePath, postSupported, APPLICATION_JSON, Hal.APPLICATION_HAL_JSON + "; q=0.5", APPLICATION_JSON);
    assertMediaTypes(resourcePath, postSupported, APPLICATION_JSON, Hal.APPLICATION_HAL_JSON + "; q=0.5 ",
            "application/json; q=0.6");
    assertMediaTypes(resourcePath, postSupported, Hal.APPLICATION_HAL_JSON, Hal.APPLICATION_HAL_JSON + "; q=0.6",
            "application/json; q=0.5");
  }

  // Method to check media types for GET and POST requests
  protected void assertMediaTypes(String resourcePath, boolean postSupported, String expectedMediaType,
          String... acceptMediaTypes) {
    // Test GET request
    HttpResponse<String> response = Unirest.get(resourcePath)
            .header(ACCEPT, String.join(",", acceptMediaTypes))
            .asString();
    assertMediaType(response, expectedMediaType);

    if (postSupported) {
      // Test POST request
      response = Unirest.post(resourcePath)
              .header(ACCEPT, String.join(",", acceptMediaTypes))
              .header(CONTENT_TYPE, APPLICATION_JSON)
              .body(Collections.emptyMap())
              .asString();
      assertMediaType(response, expectedMediaType);
    }
  }

  protected void assertMediaType(HttpResponse<String> response, String expected) {
    String actual = response.getHeaders().getFirst("Content-Type");
    assertThat(response.getStatus()).isEqualTo(200);
    // use startsWith cause sometimes server also returns quality parameters
    assertThat(actual)
            .as("Expected: %s Actual: %s".formatted(expected, actual))
            .startsWith(expected);
  }
}
