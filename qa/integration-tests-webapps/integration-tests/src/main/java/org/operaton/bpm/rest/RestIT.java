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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.operaton.bpm.AbstractWebIntegrationTest;
import org.operaton.bpm.engine.rest.hal.Hal;
import org.operaton.bpm.engine.rest.mapper.JacksonConfigurator;
import org.junit.Before;
import org.junit.Test;

import jakarta.ws.rs.core.MediaType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class RestIT extends AbstractWebIntegrationTest {

  private static final String ENGINE_DEFAULT_PATH = "engine/default";

  private static final String PROCESS_DEFINITION_PATH = ENGINE_DEFAULT_PATH + "/process-definition";

  private static final String JOB_DEFINITION_PATH = ENGINE_DEFAULT_PATH + "/job-definition";

  private static final String TASK_PATH = ENGINE_DEFAULT_PATH + "/task";

  private static final String FILTER_PATH = ENGINE_DEFAULT_PATH + "/filter";

  private static final String HISTORIC_DETAIL_PATH = ENGINE_DEFAULT_PATH + "/history/detail";

  private static final String PROCESS_INSTANCE_PATH = ENGINE_DEFAULT_PATH + "/process-instance";

  private static final String SCHEMA_LOG_PATH = ENGINE_DEFAULT_PATH + "/schema/log";


  private static final Logger log = Logger.getLogger(RestIT.class.getName());

  @Before
  public void createClient() throws Exception {
    preventRaceConditions();
    createClient(getRestCtxPath());
  }

  @Test
  public void testScenario() throws JsonProcessingException {
    // get process definitions for default engine
    log.info("Checking " + appBasePath + PROCESS_DEFINITION_PATH);
    target = client.target(appBasePath + PROCESS_DEFINITION_PATH);

    // Send GET request with the desired Accept header
    response = target.request(MediaType.APPLICATION_JSON)
            .get(Response.class);

    assertEquals(200, response.getStatus());

    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode definitionsJson = (ArrayNode) objectMapper.readTree(response.getEntity().toString());

    // invoice example
    assertEquals(3, definitionsJson.size());

    // order of results is not consistent between database types
    for (int i = 0; i < definitionsJson.size(); i++) {
      JsonNode definitionJson = definitionsJson.get(i);

      // Check if 'description' is null
      assertTrue(definitionJson.get("description") == null);

      // Check if 'suspended' is false
      assertFalse(definitionJson.get("suspended").asBoolean());

      // Check conditions based on 'key'
      if ("ReviewInvoice".equals(definitionJson.get("key").asText())) {
        assertEquals("http://bpmn.io/schema/bpmn", definitionJson.get("category").asText());
        assertEquals("Review Invoice", definitionJson.get("name").asText());
        assertEquals("reviewInvoice.bpmn", definitionJson.get("resource").asText());
      } else if ("invoice".equals(definitionJson.get("key").asText())) {
        assertEquals("http://www.omg.org/spec/BPMN/20100524/MODEL", definitionJson.get("category").asText());
        assertEquals("Invoice Receipt", definitionJson.get("name").asText());
        assertTrue(definitionJson.get("resource").asText().matches("invoice\\.v[1,2]\\.bpmn"));
      } else {
        fail("Unexpected definition key in response JSON.");
      }
    }
  }

  @Test
  public void assertJodaTimePresent() throws JsonProcessingException {
    log.info("Checking " + appBasePath + TASK_PATH);

    target = client.target(appBasePath + TASK_PATH)
            .queryParam("dueAfter", "2000-01-01T00-00-00");

    // Send GET request with the desired Accept header
    response = target.request(MediaType.APPLICATION_JSON)
            .get(Response.class);

    assertEquals(200, response.getStatus());

    ObjectMapper objectMapper = new ObjectMapper();
    ArrayNode definitionsJson = (ArrayNode) objectMapper.readTree(response.getEntity().toString());
    assertEquals(6, definitionsJson.size());
  }

  @Test
  public void testDelayedJobDefinitionSuspension() {
    log.info("Checking " + appBasePath + JOB_DEFINITION_PATH + "/suspended");

    target = client.target(appBasePath + JOB_DEFINITION_PATH + "/suspended");

    // Create request body as a Map (or you can use a custom DTO if required)
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("processDefinitionKey", "jobExampleProcess");
    requestBody.put("suspended", true);
    requestBody.put("includeJobs", true);
    requestBody.put("executionDate", "2014-08-25T13:55:45");

    // Send PUT request with entity and headers
    response = target.request(MediaType.APPLICATION_JSON)
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .put(Entity.entity(requestBody, MediaType.APPLICATION_JSON));
    assertEquals(204, response.getStatus());
  }

  @Test
  public void testTaskQueryContentType() {
    String resourcePath = appBasePath + TASK_PATH;
    log.info("Checking " + resourcePath);
    assertMediaTypesOfResource(resourcePath, false);
  }

  @Test
  public void testSingleTaskContentType() throws JsonProcessingException {
    // get id of first task
    String taskId = getFirstTask().get("id").asText();

    String resourcePath = appBasePath + TASK_PATH + "/" + taskId;
    log.info("Checking " + resourcePath);
    assertMediaTypesOfResource(resourcePath, false);
  }

  @Test
  public void testTaskFilterResultContentType() throws JsonProcessingException {
    // create filter for first task, so single result will not throw an exception
    JsonNode firstTask = getFirstTask();
    Map<String, Object> query = new HashMap<>();
    query.put("taskDefinitionKey", firstTask.get("taskDefinitionKey").asText());
    query.put("processInstanceId", firstTask.get("processInstanceId").asText());

    Map<String, Object> filter = new HashMap<>();
    filter.put("resourceType", "Task");
    filter.put("name", "IT Test Filter");
    filter.put("query", query);

    // Create the filter using Jackson
    target = client.target(appBasePath + FILTER_PATH + "/create");

    // Send POST request with entity and accept header
    response = target.request()
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(filter, MediaType.APPLICATION_JSON));

    assertEquals(200, response.getStatus());

    // Get the 'id' from the response
    String responseBody = response.readEntity(String.class);
    JsonNode responseJson = new ObjectMapper().readTree(responseBody);
    String filterId = responseJson.get("id").asText();

    // Check the filter resource (list)
    String resourcePath = appBasePath + FILTER_PATH + "/" + filterId + "/list";
    log.info("Checking " + resourcePath);
    assertMediaTypesOfResource(resourcePath, true);

    // Check the filter resource (singleResult)
    resourcePath = appBasePath + FILTER_PATH + "/" + filterId + "/singleResult";
    log.info("Checking " + resourcePath);
    assertMediaTypesOfResource(resourcePath, true);

    // delete test filter
    target = client.target(appBasePath + FILTER_PATH + "/" + filterId);
    response = target.request().delete(Response.class);
    assertEquals(204, response.getStatus());
  }

  @Test
  public void shouldSerializeDateWithDefinedFormat() throws Exception {
    // when
    target = client.target(appBasePath + SCHEMA_LOG_PATH);
    response = target.request()
            .accept(MediaType.APPLICATION_JSON)
            .get(Response.class);

    // Then
    assertEquals(200, response.getStatus());

    // Parse the response body to a JsonNode (assuming it's an array)
    String responseBody = response.readEntity(String.class);

    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode logArray = objectMapper.readTree(responseBody);

    // Get the first element in the array (assuming the response is an array)
    JsonNode logElement = logArray.get(0);

    // Extract the 'timestamp' field
    String timestamp = logElement.get("timestamp").asText();

    // Try to parse the timestamp using the defined date format
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
  public void testPolymorphicSerialization() throws Exception {
    // Get the first historic variable update
    JsonNode historicVariableUpdate = getFirstHistoricVariableUpdates();

    // Assert that the 'variableName' field is present
    assertTrue(historicVariableUpdate.has("variableName"));
  }

  /**
   * Uses Jackson's object mapper directly
   */
  @Test
  public void testProcessInstanceQuery() throws Exception {
    // Make the GET request with the query parameter 'variables'
    target = client.target(appBasePath + PROCESS_INSTANCE_PATH);

    // Send GET request with query parameter and accept header
    response = target.queryParam("variables", "invoiceNumber_eq_GPFE-23232323")
            .request(MediaType.APPLICATION_JSON)
            .get();

    // Read the response body as a String
    String responseBody = response.readEntity(String.class);
    response.close();

    // Use ObjectMapper to parse the response body into a JsonNode
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode instancesJson = objectMapper.readTree(responseBody);

    // Assert the response status is 200
    assertEquals(200, response.getStatus());

    // Assert the number of instances in the response
    // The response is expected to be a JSON array, so use instancesJson.size() to get the length
    assertEquals(2, instancesJson.size());
  }

  @Test
  public void testComplexObjectJacksonSerialization() throws Exception {
    // Make the GET request to retrieve process definition statistics
    target = client.target(appBasePath + PROCESS_DEFINITION_PATH + "/statistics");

    // Send GET request with query parameter and accept header
    response = target.queryParam("incidents", "true")
            .request(MediaType.APPLICATION_JSON)
            .get();

    // Read the response body as a String
    String responseBody = response.readEntity(String.class);

    // Use ObjectMapper to parse the response body into a JsonNode (Array)
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode definitionStatistics = objectMapper.readTree(responseBody);

    // Assert the response status is 200 (We already know the response is 200 since we are manually reading the response body)
    assertEquals(200, response.getStatus());

    // Assert the length of the definition statistics array
    assertEquals(3, definitionStatistics.size());

    // Check that the definition is also serialized correctly
    for (int i = 0; i < definitionStatistics.size(); i++) {
      JsonNode definitionStatistic = definitionStatistics.get(i);

      // Assert the class type
      assertEquals("org.operaton.bpm.engine.rest.dto.repository.ProcessDefinitionStatisticsResultDto", definitionStatistic.get("@class").asText());

      // Assert the incidents length
      assertEquals(0, definitionStatistic.get("incidents").size());

      // Get the definition object
      JsonNode definition = definitionStatistic.get("definition");

      // Check the name contains "invoice" (case-insensitive)
      assertTrue(definition.get("name").asText().toLowerCase().contains("invoice"));

      // Check if the definition is not suspended
      assertFalse(definition.get("suspended").asBoolean());
    }
  }

  @Test
  public void testOptionsRequest() throws Exception {
    // Given
    String resourcePath = appBasePath + FILTER_PATH;
    log.info("Send OPTIONS request to " + resourcePath);

    target = client.target(resourcePath);

    // Send OPTIONS request
    response = target.request().options(Response.class);

    // Then
    assertNotNull(response);
    assertEquals(200, response.getStatus());

    // Parse the response entity using Jackson (convert response entity to a JsonNode)
    String responseBody = response.readEntity(String.class);

    // Use ObjectMapper to convert response body to JsonNode
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode entity = objectMapper.readTree(responseBody);

    // Assert that the "links" field is present in the response entity
    assertNotNull(entity.get("links"));
  }

  @Test
  public void testEmptyBodyFilterIsActive() {
    target = client.target(appBasePath + FILTER_PATH + "/create");

    // Send POST request with null entity and JSON media type
    response = target.request()
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(null, MediaType.APPLICATION_JSON));

    assertEquals(400, response.getStatus());
    response.close();
  }

  protected JsonNode getFirstTask() throws JsonProcessingException {
    // Make the GET request to retrieve the tasks
    target = client.target(appBasePath + TASK_PATH);

    // Send the GET request and accept JSON response
    response = target.request()
            .accept(MediaType.APPLICATION_JSON)
            .get();

    // Read the response body as a string
    String responseBody = response.readEntity(String.class);

    // Use Jackson ObjectMapper to parse the response JSON into a JsonNode
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode tasks = objectMapper.readTree(responseBody);

    // Return the first task from the array (assuming it's an array)
    return tasks.get(0);  // Get the first element in the array (JsonNode)
  }

  protected JsonNode getFirstHistoricVariableUpdates() throws JsonProcessingException {
    // Make the GET request with the query parameter 'variableUpdates=true'
    target = client.target(appBasePath + HISTORIC_DETAIL_PATH)
            .queryParam("variableUpdates", "true");

    // Send the GET request and accept JSON response
    response = target.request()
            .accept(MediaType.APPLICATION_JSON)
            .get();

    // Read the response body as a String
    String responseBody = response.readEntity(String.class);

    // Parse the response body as a JsonNode (Array)
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode updates = objectMapper.readTree(responseBody);

    // Return the first item in the array (assuming the response is an array)
    return updates.get(0); // returns the first element of the array
  }

  protected void assertMediaTypesOfResource(String resourcePath, boolean postSupported) {
    WebTarget resource = client.target(resourcePath);
    assertMediaTypes(resource, postSupported, MediaType.APPLICATION_JSON);
    assertMediaTypes(resource, postSupported, MediaType.APPLICATION_JSON, MediaType.WILDCARD);
    assertMediaTypes(resource, postSupported, MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON);
    assertMediaTypes(resource, postSupported, Hal.APPLICATION_HAL_JSON, Hal.APPLICATION_HAL_JSON);
    assertMediaTypes(resource, postSupported, Hal.APPLICATION_HAL_JSON, Hal.APPLICATION_HAL_JSON, MediaType.APPLICATION_JSON + "; q=0.5");
    assertMediaTypes(resource, postSupported, MediaType.APPLICATION_JSON, Hal.APPLICATION_HAL_JSON + "; q=0.5", MediaType.APPLICATION_JSON);
    assertMediaTypes(resource, postSupported, MediaType.APPLICATION_JSON, Hal.APPLICATION_HAL_JSON + "; q=0.5 ", MediaType.APPLICATION_JSON + "; q=0.6");
    assertMediaTypes(resource, postSupported, Hal.APPLICATION_HAL_JSON, Hal.APPLICATION_HAL_JSON + "; q=0.6", MediaType.APPLICATION_JSON + "; q=0.5");
  }

  // Method to check media types for GET and POST requests
  protected void assertMediaTypes(WebTarget resource, boolean postSupported, String expectedMediaType, String... acceptMediaTypes) {
    // Test GET request
    response = resource.request().accept(acceptMediaTypes).get();
    assertMediaType(response, expectedMediaType);
    response.close();

    if (postSupported) {
      // Test POST request
      response = resource.request()
              .accept(acceptMediaTypes)
              .post(Entity.entity(Collections.emptyMap(), MediaType.APPLICATION_JSON));
      assertMediaType(response, expectedMediaType);
      response.close();
    }
  }

  protected void assertMediaType(Response response, String expected) {
    MediaType actual = response.getMediaType();
    assertEquals(200, response.getStatus());
    // use startsWith cause sometimes server also returns quality parameters (e.g. websphere/wink)
    assertTrue("Expected: " + expected + " Actual: " + actual, actual.toString().startsWith(expected));
  }

}
