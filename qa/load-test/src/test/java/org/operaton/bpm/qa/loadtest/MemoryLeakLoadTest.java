/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.qa.loadtest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Load test to detect memory leaks.
 *
 * <p>Run with: {@code ./mvnw verify -pl qa/load-test -Pload-test}</p>
 *
 * <p>System properties:
 * <ul>
 *   <li>{@code loadtest.users} - concurrent users (default: 30)</li>
 *   <li>{@code loadtest.warmup.seconds} - warmup duration (default: 10)</li>
 *   <li>{@code loadtest.sustained.seconds} - sustained load duration (default: 60)</li>
 *   <li>{@code loadtest.processKey} - process definition key (default: item-approval)</li>
 *   <li>{@code loadtest.withVariablesInReturn} - include variables in response (default: true)</li>
 * </ul>
 *
 * @see <a href="https://github.com/operaton/operaton/issues/2761">#2761</a>
 */
@SpringBootTest(
    classes = LoadTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings("java:S2925") // Thread.sleep allowed in this test
class MemoryLeakLoadTest {

  private static final Logger LOG = LoggerFactory.getLogger(MemoryLeakLoadTest.class);

  private static final int CONCURRENT_USERS = Integer.getInteger("loadtest.users", 30);
  private static final int WARMUP_SECONDS = Integer.getInteger("loadtest.warmup.seconds", 10);
  private static final int SUSTAINED_SECONDS = Integer.getInteger("loadtest.sustained.seconds", 60);
  private static final int MEMORY_SAMPLES = Integer.getInteger("loadtest.memory.samples", 6);
  private static final String PROCESS_KEY = System.getProperty("loadtest.processKey", "item-approval");
  private static final boolean WITH_VARIABLES_IN_RETURN =
      Boolean.parseBoolean(System.getProperty("loadtest.withVariablesInReturn", "true"));

  @LocalServerPort
  int serverPort;

  WireMockServer wireMockServer;
  HttpClient httpClient;

  @BeforeAll
  void setUp() {
    wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    wireMockServer.start();
    setupWireMockStubs();

    httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    LOG.info("=== Load Test Configuration ===");
    LOG.info("Operaton server port: {}", serverPort);
    LOG.info("WireMock port: {}", wireMockServer.port());
    LOG.info("Process key: {}", PROCESS_KEY);
    LOG.info("Concurrent users: {}", CONCURRENT_USERS);
    LOG.info("Warmup: {}s, Sustained: {}s", WARMUP_SECONDS, SUSTAINED_SECONDS);
    LOG.info("withVariablesInReturn: {}", WITH_VARIABLES_IN_RETURN);
    LOG.info("Max heap: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);
  }

  @AfterAll
  void tearDown() {
    if (wireMockServer != null) {
      wireMockServer.stop();
    }
  }

  @Test
  void memoryStaysStableUnderSustainedLoad() throws Exception {
    assertThat(MEMORY_SAMPLES)
        .as("loadtest.memory.samples must be at least 1")
        .isGreaterThanOrEqualTo(1);

    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    // Phase 1: Warmup
    LOG.info("=== Phase 1: Warmup ({} seconds, {} concurrent users) ===", WARMUP_SECONDS, CONCURRENT_USERS);
    LoadResult warmupResult = runLoad(WARMUP_SECONDS);
    LOG.info("Warmup complete: {} successful, {} failed", warmupResult.successCount, warmupResult.failureCount);

    forceGc();
    long baselineHeapMB = memoryMXBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;
    LOG.info("=== Baseline after warmup + GC: {} MB ===", baselineHeapMB);

    // Phase 2: Sustained load with memory monitoring
    LOG.info("=== Phase 2: Sustained load ({} seconds) ===", SUSTAINED_SECONDS);
    List<Long> memorySamples = new CopyOnWriteArrayList<>();
    int sampleIntervalSeconds = Math.max(1, (int) Math.ceil((double) SUSTAINED_SECONDS / MEMORY_SAMPLES));

    Thread sampler = new Thread(() -> {
      try {
        for (int i = 0; i < MEMORY_SAMPLES; i++) {
          Thread.sleep(sampleIntervalSeconds * 1000L);
          forceGc();
          long usedMB = memoryMXBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;
          memorySamples.add(usedMB);
          LOG.info("Memory sample {}/{}: {} MB (delta from baseline: +{} MB)",
              i + 1, MEMORY_SAMPLES, usedMB, usedMB - baselineHeapMB);
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
    sampler.setDaemon(true);
    sampler.start();

    LoadResult sustainedResult = runLoad(SUSTAINED_SECONDS);
    sampler.join(10000);

    LOG.info("Sustained load complete: {} successful, {} failed",
        sustainedResult.successCount, sustainedResult.failureCount);

    // Phase 3: Post-load
    forceGc();
    long finalHeapMB = memoryMXBean.getHeapMemoryUsage().getUsed() / 1024 / 1024;
    LOG.info("=== Final heap after GC: {} MB (baseline was {} MB, growth: +{} MB) ===",
        finalHeapMB, baselineHeapMB, finalHeapMB - baselineHeapMB);

    // Assertions
    assertThat(sustainedResult.successCount.get())
        .as("At least some process instances should complete successfully")
        .isGreaterThan(0);

    long maxAllowedMB = Math.max(baselineHeapMB * 2, baselineHeapMB + 300);
    assertThat(finalHeapMB)
        .as("Heap after GC should not exceed %d MB (baseline: %d MB). "
            + "If this fails, there is likely a memory leak.", maxAllowedMB, baselineHeapMB)
        .isLessThanOrEqualTo(maxAllowedMB);

    if (memorySamples.size() >= 4) {
      long firstQuarterAvg = averageOf(memorySamples.subList(0, memorySamples.size() / 4));
      long lastQuarterAvg = averageOf(memorySamples.subList(
          memorySamples.size() * 3 / 4, memorySamples.size()));
      long growth = lastQuarterAvg - firstQuarterAvg;
      LOG.info("Memory trend: first quarter avg {} MB, last quarter avg {} MB, growth {} MB",
          firstQuarterAvg, lastQuarterAvg, growth);

      assertThat(growth)
          .as("Memory should not grow by more than 200 MB between first and last quarter of sustained load. "
              + "First quarter: %d MB, Last quarter: %d MB", firstQuarterAvg, lastQuarterAvg)
          .isLessThan(200);
    }

    // Log summary
    LOG.info("=== LOAD TEST SUMMARY ===");
    LOG.info("Total processes: {}", sustainedResult.successCount.get() + sustainedResult.failureCount.get());
    LOG.info("Successful: {}, Failed: {}", sustainedResult.successCount, sustainedResult.failureCount);
    LOG.info("Avg response time: {} ms", sustainedResult.totalResponseTimeMs.get() /
        Math.max(1, sustainedResult.successCount.get()));
    LOG.info("Baseline heap: {} MB, Final heap: {} MB", baselineHeapMB, finalHeapMB);
    LOG.info("Memory samples: {}", memorySamples);
  }

  private LoadResult runLoad(int durationSeconds) throws InterruptedException {
    LoadResult result = new LoadResult();
    ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
    CountDownLatch startGate = new CountDownLatch(1);
    long endTime = System.currentTimeMillis() + durationSeconds * 1000L;

    for (int i = 0; i < CONCURRENT_USERS; i++) {
      executor.submit(() -> {
        try {
          startGate.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
        while (System.currentTimeMillis() < endTime && !Thread.currentThread().isInterrupted()) {
          try {
            long start = System.nanoTime();
            int statusCode = startProcess();
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            if (statusCode == 200) {
              result.successCount.incrementAndGet();
              result.totalResponseTimeMs.addAndGet(elapsed);
            } else {
              result.failureCount.incrementAndGet();
              if (result.failureCount.get() <= 5) {
                LOG.warn("Process start returned HTTP {}", statusCode);
              }
            }
            Thread.sleep(ThreadLocalRandom.current().nextLong(1000, 2000));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
          } catch (Exception e) {
            result.failureCount.incrementAndGet();
            if (result.failureCount.get() <= 5) {
              LOG.warn("Process start failed: {}", e.getMessage());
            }
          }
        }
      });
    }

    startGate.countDown();
    executor.shutdown();
    long shutdownTimeoutSeconds = durationSeconds + 30L;
    boolean terminated = executor.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS);
    if (!terminated) {
      List<Runnable> queuedTasks = executor.shutdownNow();
      assertThat(terminated)
          .as("Load executor did not terminate within %s seconds; cancelled %s queued tasks",
              shutdownTimeoutSeconds, queuedTasks.size())
          .isTrue();
    }
    return result;
  }

  private int startProcess() throws Exception {
    String wmPort = String.valueOf(wireMockServer.port());
    String requestBody;

    if ("simple-process".equals(PROCESS_KEY)) {
      requestBody = "{ \"withVariablesInReturn\": " + WITH_VARIABLES_IN_RETURN + " }";
    } else {
      requestBody = """
          {
            "withVariablesInReturn": %s,
            "variables": {
              "INPUT": {
                "type": "Json",
                "value": "{\\"policy\\":\\"EVALUATION\\",\\"itemId\\":\\"001\\",\\"amount\\":50,\\"score\\":20}",
                "valueInfo": {
                  "serializationDataFormat": "application/json"
                }
              },
              "BASE_URL": {
                "type": "String",
                "value": "http://localhost:%s"
              },
              "SERVICE_API_URL": {
                "type": "String",
                "value": "http://localhost:%s"
              },
              "API_KEY": {
                "type": "String",
                "value": "test-api-key"
              }
            }
          }
          """.formatted(WITH_VARIABLES_IN_RETURN, wmPort, wmPort);
    }

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + serverPort +
            "/engine-rest/process-definition/key/" + PROCESS_KEY + "/start"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .timeout(Duration.ofSeconds(60))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return response.statusCode();
  }

  private void setupWireMockStubs() {
    wireMockServer.stubFor(get(urlPathEqualTo("/api/items/001"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("""
                {
                  "itemId": "item-001",
                  "entityId": "entity-001",
                  "score": 35,
                  "relationship": {
                    "enrollmentDate": "2020-10-01",
                    "provider": { "id": "provider-001" },
                    "category": 1,
                    "score": 24,
                    "quota": 4000.0,
                    "baseQuota": 3200.0,
                    "adjustedQuota": 2800.0,
                    "endDate": null,
                    "status": "ACTIVE"
                  }
                }
                """)));

    wireMockServer.stubFor(get(urlPathEqualTo("/api/service/status-a"))
        .withQueryParam("id", equalTo("entity-001"))
        .withHeader("API-KEY", equalTo("test-api-key"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{ \"hasStatusA\": false }")));

    wireMockServer.stubFor(get(urlPathEqualTo("/api/service/status-b"))
        .withQueryParam("id", equalTo("entity-001"))
        .withHeader("API-KEY", equalTo("test-api-key"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{ \"hasStatusB\": false }")));
  }

  private static void forceGc() {
    System.gc();
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    System.gc();
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static long averageOf(List<Long> values) {
    return values.stream().mapToLong(Long::longValue).sum() / values.size();
  }

  static class LoadResult {
    final AtomicInteger successCount = new AtomicInteger();
    final AtomicInteger failureCount = new AtomicInteger();
    final AtomicLong totalResponseTimeMs = new AtomicLong();
  }
}
