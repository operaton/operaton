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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.operaton.bpm.AbstractWebIntegrationTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

public class RestJaxRs2IT extends AbstractWebIntegrationTest {

  private static final String ENGINE_DEFAULT_PATH = "engine/default";
  private static final String FETCH_AND_LOCK_PATH = ENGINE_DEFAULT_PATH + "/external-task/fetchAndLock";

  @BeforeEach
  public void createClient() throws Exception {
    preventRaceConditions();
    createClient(getRestCtxPath());
  }

  @Test @Timeout(value=10000, unit=TimeUnit.MILLISECONDS)
  public void shouldUseJaxRs2Artifact() throws Exception {
    Map<String, Object> payload = new HashMap<>();
    payload.put("workerId", "aWorkerId");
    payload.put("asyncResponseTimeout", 1000 * 60 * 30 + 1);

    target = client.target(appBasePath + FETCH_AND_LOCK_PATH);

    // Make POST request with the payload
    response = target.request()
            .accept(MediaType.APPLICATION_JSON)
            .post(Entity.entity(payload, MediaType.APPLICATION_JSON));

    assertThat(response.getStatus()).isEqualTo(400);
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode responseJson = objectMapper.readTree(response.getEntity().toString());
    String responseMessage = responseJson.get("message").asText();
    assertThat("The asynchronous response timeout cannot be set to a value greater than 1800000 milliseconds".equals(responseMessage)).isTrue();
    response.close();
  }

  @Test
  public void shouldPerform500ConcurrentRequests() throws Exception {
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    final CloseableHttpClient httpClient = HttpClients.custom()
      .setConnectionManager(cm)
      .build();

    Callable<String> performRequest = new Callable<>() {

      @Override
      public String call() throws IOException {
        HttpPost request = new HttpPost(appBasePath + FETCH_AND_LOCK_PATH);
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        StringEntity stringEntity = new StringEntity("{ \"workerId\": \"aWorkerId\", \"asyncResponseTimeout\": 1000 }");
        request.setEntity(stringEntity);

        var response = httpClient.execute(request, HttpClientContext.create());
        String responseBody = null;
        try {
          HttpEntity entity = response.getEntity();
          responseBody = EntityUtils.toString(entity);
          request.releaseConnection();
        } finally {
          response.close();
        }

        return responseBody;
      }

    };

    int requestsCount = 500;
    ExecutorService service = Executors.newFixedThreadPool(requestsCount);

    List<Callable<String>> requests = new ArrayList<>();
    for (int i = 0; i < requestsCount; i++) {
      requests.add(performRequest);
    }

    List<Future<String>> futures = service.invokeAll(requests);
    service.shutdown();
    service.awaitTermination(1, TimeUnit.HOURS);

    for (Future<String> future : futures) {
      assertThat("[]").isEqualTo(future.get());
    }
  }

}
