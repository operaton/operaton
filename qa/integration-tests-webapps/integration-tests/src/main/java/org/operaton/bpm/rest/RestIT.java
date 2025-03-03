/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.rest;

import org.operaton.bpm.AbstractWebIntegrationTest;
import org.operaton.bpm.engine.rest.hal.Hal;
import org.operaton.bpm.engine.rest.mapper.JacksonConfigurator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

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
  public void testScenario() throws JSONException, IOException, InterruptedException {
    // get process definitions for default engine
    log.info("Checking " + appBasePath + PROCESS_DEFINITION_PATH);
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + PROCESS_DEFINITION_PATH))
      .header("Accept", MediaType.APPLICATION_JSON)
      .GET()
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());

    JSONArray definitionsJson = new JSONArray(response.body());

    // invoice example
    assertEquals(3, definitionsJson.length());

    // order of results is not consistent between database types
    for (int i = 0; i < definitionsJson.length(); i++) {
      JSONObject definitionJson = definitionsJson.getJSONObject(i);
      assertTrue(definitionJson.isNull("description"));
      assertFalse(definitionJson.getBoolean("suspended"));
      if (definitionJson.getString("key").equals("ReviewInvoice")) {
        assertEquals("http://bpmn.io/schema/bpmn", definitionJson.getString("category"));
        assertEquals("Review Invoice", definitionJson.getString("name"));
        assertEquals("reviewInvoice.bpmn", definitionJson.getString("resource"));
      } else if (definitionJson.getString("key").equals("invoice")) {
        assertEquals("http://www.omg.org/spec/BPMN/20100524/MODEL", definitionJson.getString("category"));
        assertEquals("Invoice Receipt", definitionJson.getString("name"));
        assertTrue(definitionJson.getString("resource").matches("invoice\\.v[1,2]\\.bpmn"));
      } else {
        fail("Unexpected definition key in response JSON.");
      }
    }
  }

  @Test
  public void assertJodaTimePresent() throws IOException, InterruptedException, JSONException {
    log.info("Checking " + appBasePath + TASK_PATH);

    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + TASK_PATH + "?dueAfter=2000-01-01T00-00-00"))
      .header("Accept", MediaType.APPLICATION_JSON)
      .GET()
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());

    JSONArray definitionsJson = new JSONArray(response.body());
    assertEquals(6, definitionsJson.length());
  }

  @Test
  public void testDelayedJobDefinitionSuspension() throws Exception {
    log.info("Checking " + appBasePath + JOB_DEFINITION_PATH + "/suspended");

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("processDefinitionKey", "jobExampleProcess");
    requestBody.put("suspended", true);
    requestBody.put("includeJobs", true);
    requestBody.put("executionDate", "2014-08-25T13:55:45");

    String jsonPayload = new JSONObject(requestBody).toString();
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + JOB_DEFINITION_PATH + "/suspended"))
      .header("Accept", MediaType.APPLICATION_JSON)
      .header("Content-Type", MediaType.APPLICATION_JSON)
      .PUT(HttpRequest.BodyPublishers.ofString(jsonPayload))
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(204, response.statusCode());
  }

  @Test
  public void testTaskQueryContentType() throws Exception {
    String resourcePath = appBasePath + TASK_PATH;
    log.info("Checking " + resourcePath);
    assertMediaTypesOfResource(resourcePath, false);
  }

  @Test
  public void testSingleTaskContentType() throws JSONException, IOException, InterruptedException {
    // get id of first task
    String taskId = getFirstTask().getString("id");

    String resourcePath = appBasePath + TASK_PATH + "/" + taskId;
    log.info("Checking " + resourcePath);
    assertMediaTypesOfResource(resourcePath, false);
  }

  @Test
  public void testTaskFilterResultContentType() throws JSONException, IOException, InterruptedException {
    // create filter for first task, so single result will not throw an exception
    JSONObject firstTask = getFirstTask();
    Map<String, Object> query = new HashMap<>();
    query.put("taskDefinitionKey", firstTask.getString("taskDefinitionKey"));
    query.put("processInstanceId", firstTask.getString("processInstanceId"));
    Map<String, Object> filter = new HashMap<>();
    filter.put("resourceType", "Task");
    filter.put("name", "IT Test Filter");
    filter.put("query", query);

    String jsonPayload = new JSONObject(filter).toString();
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + FILTER_PATH + "/create"))
      .header("Accept", MediaType.APPLICATION_JSON)
      .header("Content-Type", MediaType.APPLICATION_JSON)
      .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    String filterId = new JSONObject(response.body()).getString("id");

    String resourcePath = appBasePath + FILTER_PATH + "/" + filterId + "/list";
    log.info("Checking " + resourcePath);
    assertMediaTypesOfResource(resourcePath, true);

    resourcePath = appBasePath + FILTER_PATH + "/" + filterId + "/singleResult";
    log.info("Checking " + resourcePath);
    assertMediaTypesOfResource(resourcePath, true);

    // delete test filter
    request = HttpRequest.newBuilder().uri(URI.create(appBasePath + FILTER_PATH + "/" + filterId)).DELETE().build();
    response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(204, response.statusCode());
  }

  @Test
  public void shouldSerializeDateWithDefinedFormat() throws JSONException, IOException, InterruptedException {
    // when
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + SCHEMA_LOG_PATH))
      .header("Accept", MediaType.APPLICATION_JSON)
      .GET()
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    // then
    assertEquals(200, response.statusCode());
    JSONObject logElement = new JSONArray(response.body()).getJSONObject(0);
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
  public void testPolymorphicSerialization() throws JSONException, IOException, InterruptedException {
    JSONObject historicVariableUpdate = getFirstHistoricVariableUpdates();

    // variable update specific property
    assertTrue(historicVariableUpdate.has("variableName"));
  }

  /**
   * Uses Jackson's object mapper directly
   */
  @Test
  public void testProcessInstanceQuery() throws IOException, InterruptedException, JSONException {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + PROCESS_INSTANCE_PATH + "?variables=invoiceNumber_eq_GPFE-23232323"))
      .header("Accept", MediaType.APPLICATION_JSON)
      .GET()
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    JSONArray instancesJson = new JSONArray(response.body());

    assertEquals(200, response.statusCode());
    // invoice example instance
    assertEquals(2, instancesJson.length());
  }

  @Test
  public void testComplexObjectJacksonSerialization() throws IOException, InterruptedException, JSONException {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + PROCESS_DEFINITION_PATH + "/statistics?incidents=true"))
      .header("Accept", MediaType.APPLICATION_JSON)
      .GET()
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    JSONArray definitionStatistics = new JSONArray(response.body());

    assertEquals(200, response.statusCode());
    // invoice example instance
    assertEquals(3, definitionStatistics.length());

    // check that definition is also serialized
    for (int i = 0; i < definitionStatistics.length(); i++) {
      JSONObject definitionStatistic = definitionStatistics.getJSONObject(i);
      assertEquals("org.operaton.bpm.engine.rest.dto.repository.ProcessDefinitionStatisticsResultDto",
        definitionStatistic.getString("@class"));
      assertEquals(0, definitionStatistic.getJSONArray("incidents").length());
      JSONObject definition = definitionStatistic.getJSONObject("definition");
      assertTrue(definition.getString("name").toLowerCase().contains("invoice"));
      assertFalse(definition.getBoolean("suspended"));
    }
  }

  @Test
  public void testOptionsRequest() throws IOException, InterruptedException, JSONException {
    //since WAS 9 contains patched cxf, which does not support OPTIONS request, we have to test this
    String resourcePath = appBasePath + FILTER_PATH;
    log.info("Send OPTIONS request to " + resourcePath);

    // given
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(resourcePath))
      .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    // then
    assertNotNull(response);
    assertEquals(200, response.statusCode());
    JSONObject entity = new JSONObject(response.body());
    assertNotNull(entity.has("links"));
  }

  @Test
  public void testEmptyBodyFilterIsActive() throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + FILTER_PATH + "/create"))
      .header("Accept", MediaType.APPLICATION_JSON)
      .header("Content-Type", MediaType.APPLICATION_JSON)
      .POST(HttpRequest.BodyPublishers.ofString("{}"))
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertEquals(400, response.statusCode());
  }

  protected JSONObject getFirstTask() throws JSONException, IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + TASK_PATH))
      .header("Accept", MediaType.APPLICATION_JSON)
      .GET()
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    JSONArray tasks = new JSONArray(response.body());
    return tasks.getJSONObject(0);
  }

  protected JSONObject getFirstHistoricVariableUpdates() throws JSONException, IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(appBasePath + HISTORIC_DETAIL_PATH + "?variableUpdates=true"))
      .header("Accept", MediaType.APPLICATION_JSON)
      .GET()
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    JSONArray updates = new JSONArray(response.body());
    return updates.getJSONObject(0);
  }

  protected void assertMediaTypesOfResource(String resourcePath, boolean postSupported)
    throws IOException, InterruptedException, JSONException {
    assertMediaTypes(resourcePath, postSupported, MediaType.APPLICATION_JSON_TYPE.getType());
    assertMediaTypes(resourcePath, postSupported, MediaType.APPLICATION_JSON_TYPE.getType(), MediaType.WILDCARD);
    assertMediaTypes(resourcePath, postSupported, MediaType.APPLICATION_JSON_TYPE.getType(),
      MediaType.APPLICATION_JSON);
    assertMediaTypes(resourcePath, postSupported, Hal.APPLICATION_HAL_JSON, Hal.APPLICATION_HAL_JSON);
    assertMediaTypes(resourcePath, postSupported, Hal.APPLICATION_HAL_JSON, Hal.APPLICATION_HAL_JSON,
      MediaType.APPLICATION_JSON + "; q=0.5");
    assertMediaTypes(resourcePath, postSupported, MediaType.APPLICATION_JSON_TYPE.getType(),
      Hal.APPLICATION_HAL_JSON + "; q=0.5", MediaType.APPLICATION_JSON);
    assertMediaTypes(resourcePath, postSupported, MediaType.APPLICATION_JSON_TYPE.getType(),
      Hal.APPLICATION_HAL_JSON + "; q=0.5 ", MediaType.APPLICATION_JSON + "; q=0.6");
    assertMediaTypes(resourcePath, postSupported, Hal.APPLICATION_HAL_JSON, Hal.APPLICATION_HAL_JSON + "; q=0.6",
      MediaType.APPLICATION_JSON + "; q=0.5");
  }

  protected void assertMediaTypes(String resourcePath,
                                  boolean postSupported,
                                  String expectedMediaType,
                                  String... acceptMediaTypes) throws IOException, InterruptedException, JSONException {
    // test GET request
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(resourcePath))
      .header("Accept", String.join(",", acceptMediaTypes))
      .GET()
      .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    assertMediaType(response, expectedMediaType);

    if (postSupported) {
      // test POST request
      request = HttpRequest.newBuilder()
        .uri(URI.create(resourcePath))
        .header("Accept", String.join(",", acceptMediaTypes))
        .header("Content-Type", MediaType.APPLICATION_JSON)
        .POST(HttpRequest.BodyPublishers.ofString("{}"))
        .build();
      response = client.send(request, HttpResponse.BodyHandlers.ofString());
      assertMediaType(response, expectedMediaType);
    }
  }

  protected void assertMediaType(HttpResponse<String> response, String expected) {
    String actual = response.headers().firstValue("Content-Type").orElse("");
    assertEquals(200, response.statusCode());
    // use startsWith cause sometimes server also returns quality parameters (e.g. websphere/wink)
    assertTrue("Expected: " + expected + " Actual: " + actual, actual.startsWith(expected));
  }
}
