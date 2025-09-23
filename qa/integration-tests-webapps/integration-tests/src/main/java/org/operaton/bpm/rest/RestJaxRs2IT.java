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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.operaton.bpm.AbstractWebIntegrationTest;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("java:S5960")
class RestJaxRs2IT extends AbstractWebIntegrationTest {

  private static final String ENGINE_DEFAULT_PATH = "engine/default";
  private static final String FETCH_AND_LOCK_PATH = ENGINE_DEFAULT_PATH + "/external-task/fetchAndLock";

  @BeforeEach
  void createClient() {
    preventRaceConditions();
    createClient(getRestCtxPath());
  }

  @Test
  @Timeout(value = 10000, unit = TimeUnit.MILLISECONDS)
  void shouldUseJaxRs2Artifact() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("workerId", "aWorkerId");
    payload.put("asyncResponseTimeout", 1000 * 60 * 30 + 1);

    HttpResponse<JsonNode> response = Unirest.post(appBasePath + FETCH_AND_LOCK_PATH)
            .header(ACCEPT, APPLICATION_JSON)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .body(payload)
            .asJson();

    assertThat(response.getStatus()).isEqualTo(400);
    String responseMessage = response.getBody().getObject().get("message").toString();
    assertThat(responseMessage).isEqualTo("The asynchronous response timeout cannot be set to a value greater than 1800000 milliseconds");
  }

  @Test
  void shouldPerform500ConcurrentRequests() throws Exception {
    Callable<String> performRequest = () -> {
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("workerId", "aWorkerId");
      requestBody.put("asyncResponseTimeout", 1000);

      HttpResponse<String> response = Unirest.post(appBasePath + FETCH_AND_LOCK_PATH)
              .header(CONTENT_TYPE, APPLICATION_JSON)
              .body(requestBody)
              .asString();

      return response.getBody();
    };

    int requestsCount = 500;
    ExecutorService service = Executors.newFixedThreadPool(requestsCount);

    try {
      List<Callable<String>> requests = new ArrayList<>();
      for (int i = 0; i < requestsCount; i++) {
        requests.add(performRequest);
      }

      List<Future<String>> futures = service.invokeAll(requests);
      service.shutdown();
      boolean terminated = service.awaitTermination(1, TimeUnit.HOURS);
      if (!terminated) {
        service.shutdownNow();
      }

      for (Future<String> future : futures) {
        assertThat(future.get()).isEqualTo("[]");
      }
    } finally {
      if (!service.isShutdown()) {
        service.shutdown();
      }
    }
  }

}
