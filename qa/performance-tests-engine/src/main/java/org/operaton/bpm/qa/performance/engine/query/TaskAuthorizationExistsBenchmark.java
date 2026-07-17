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
package org.operaton.bpm.qa.performance.engine.query;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;

import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.RuntimeService;
import org.operaton.bpm.engine.TaskService;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.identity.Authentication;
import org.operaton.bpm.engine.repository.Deployment;
import org.operaton.bpm.model.bpmn.Bpmn;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.qa.performance.engine.junit.PerfTestProcessEngine;

import static org.operaton.bpm.engine.authorization.Authorization.ANY;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Resources.TASK;

public class TaskAuthorizationExistsBenchmark {

  protected static final String DEPLOYMENT_NAME = "task-authorization-exists-benchmark";
  protected static final String PROCESS_KEY = "taskAuthorizationExistsBenchmark";
  protected static final String USER_ID = "task-auth-benchmark-user";
  protected static final String CSV_HEADER = String.join(",",
      "timestamp",
      "branch",
      "commit",
      "database",
      "tasks",
      "groups",
      "matchingAuthorizations",
      "pageSize",
      "warmupIterations",
      "iterations",
      "visibleTasks",
      "operation",
      "totalMs",
      "avgMs",
      "minMs",
      "p50Ms",
      "p95Ms",
      "maxMs",
      "observedSum");

  public static void main(String[] args) throws Exception {
    new TaskAuthorizationExistsBenchmark().run(BenchmarkOptions.fromSystemProperties());
  }

  public void run(BenchmarkOptions options) throws Exception {
    ProcessEngine engine = PerfTestProcessEngine.getInstance();

    try {
      ProcessEngineConfigurationImpl configuration =
          (ProcessEngineConfigurationImpl) engine.getProcessEngineConfiguration();
      configuration.setAuthorizationEnabled(false);

      cleanupBenchmarkData(engine, options);
      assertNoExistingTasks(engine);

      String deploymentId = setupData(engine, options);
      List<String> authorizationIds = createAuthorizations(engine.getAuthorizationService(), options);

      try {
        configuration.setAuthorizationEnabled(true);
        List<String> groupIds = groupIds(options.groupCount);
        engine.getIdentityService().setAuthentication(new Authentication(USER_ID, groupIds));

        try {
          long visibleTasks = engine.getTaskService().createTaskQuery().count();
          if (visibleTasks != options.taskCount) {
            throw new IllegalStateException("Expected " + options.taskCount
                + " visible tasks but got " + visibleTasks);
          }

          warmup(engine.getTaskService(), options);

          List<Measurement> measurements = List.of(
              measure("listPage", options.iterations, () -> engine.getTaskService().createTaskQuery().listPage(0, options.pageSize).size()),
              measure("listPageOrderedByCreateTimeDesc", options.iterations, () -> engine.getTaskService().createTaskQuery()
                  .orderByTaskCreateTime()
                  .desc()
                  .listPage(0, options.pageSize)
                  .size()),
              measure("count", options.iterations, () -> engine.getTaskService().createTaskQuery().count())
          );

          writeMeasurements(options, measurements, visibleTasks);
        } finally {
          engine.getIdentityService().clearAuthentication();
        }
      } finally {
        if (options.cleanupAfterRun) {
          configuration.setAuthorizationEnabled(false);
          cleanupAuthorizations(engine.getAuthorizationService(), authorizationIds);
          engine.getRepositoryService().deleteDeployment(deploymentId, true);
        }
      }
    } finally {
      engine.close();
    }
  }

  protected static String setupData(ProcessEngine engine, BenchmarkOptions options) {
    RepositoryService repositoryService = engine.getRepositoryService();
    RuntimeService runtimeService = engine.getRuntimeService();

    BpmnModelInstance process = Bpmn.createExecutableProcess(PROCESS_KEY)
        .operatonHistoryTimeToLive(180)
        .startEvent()
        .userTask("wait")
        .name("Benchmark task")
        .endEvent()
        .done();

    String deploymentId = repositoryService.createDeployment()
        .name(DEPLOYMENT_NAME)
        .addModelInstance("task-authorization-exists-benchmark.bpmn", process)
        .deploy()
        .getId();

    for (int i = 0; i < options.taskCount; i++) {
      runtimeService.startProcessInstanceByKey(PROCESS_KEY);
    }

    return deploymentId;
  }

  protected static List<String> createAuthorizations(AuthorizationService authorizationService, BenchmarkOptions options) {
    List<String> authorizationIds = new ArrayList<>();
    authorizationIds.add(grantUser(authorizationService, USER_ID, ANY));
    authorizationIds.add(grantUser(authorizationService, ANY, ANY));

    for (String groupId : groupIds(options.groupCount)) {
      authorizationIds.add(grantGroup(authorizationService, groupId, ANY));
      authorizationIds.add(grantGroup(authorizationService, groupId, PROCESS_KEY));
    }
    return authorizationIds;
  }

  protected static void cleanupBenchmarkData(ProcessEngine engine, BenchmarkOptions options) {
    RepositoryService repositoryService = engine.getRepositoryService();
    List<Deployment> deployments = repositoryService.createDeploymentQuery().deploymentName(DEPLOYMENT_NAME).list();
    for (Deployment deployment : deployments) {
      repositoryService.deleteDeployment(deployment.getId(), true);
    }

    AuthorizationService authorizationService = engine.getAuthorizationService();
    for (Authorization authorization : authorizationService.createAuthorizationQuery().userIdIn(USER_ID).list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }

    String[] groupIds = groupIds(options.groupCount).toArray(String[]::new);
    for (Authorization authorization : authorizationService.createAuthorizationQuery().groupIdIn(groupIds).list()) {
      authorizationService.deleteAuthorization(authorization.getId());
    }
  }

  protected static void cleanupAuthorizations(AuthorizationService authorizationService, List<String> authorizationIds) {
    for (String authorizationId : authorizationIds) {
      authorizationService.deleteAuthorization(authorizationId);
    }
  }

  protected static void assertNoExistingTasks(ProcessEngine engine) {
    long existingTasks = engine.getTaskService().createTaskQuery().count();
    if (existingTasks > 0) {
      throw new IllegalStateException("Task authorization benchmark expects an empty task table, but found "
          + existingTasks + " existing tasks. Use a fresh database or remove unrelated task data before running it.");
    }
  }

  protected static List<String> groupIds(int groupCount) {
    List<String> groupIds = new ArrayList<>(groupCount);
    for (int i = 0; i < groupCount; i++) {
      groupIds.add("task-auth-benchmark-group-" + i);
    }
    return groupIds;
  }

  protected static String grantUser(AuthorizationService authorizationService, String userId, String resourceId) {
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setResource(TASK);
    authorization.setResourceId(resourceId);
    authorization.addPermission(READ);
    authorization.setUserId(userId);
    authorizationService.saveAuthorization(authorization);
    return authorization.getId();
  }

  protected static String grantGroup(AuthorizationService authorizationService, String groupId, String resourceId) {
    Authorization authorization = authorizationService.createNewAuthorization(AUTH_TYPE_GRANT);
    authorization.setResource(TASK);
    authorization.setResourceId(resourceId);
    authorization.addPermission(READ);
    authorization.setGroupId(groupId);
    authorizationService.saveAuthorization(authorization);
    return authorization.getId();
  }

  protected static void warmup(TaskService taskService, BenchmarkOptions options) {
    for (int i = 0; i < options.warmupIterations; i++) {
      taskService.createTaskQuery().listPage(0, options.pageSize);
      taskService.createTaskQuery().orderByTaskCreateTime().desc().listPage(0, options.pageSize);
      taskService.createTaskQuery().count();
    }
  }

  protected static Measurement measure(String operation, int iterations, LongSupplier supplier) {
    long[] durations = new long[iterations];
    long observed = 0;
    for (int i = 0; i < iterations; i++) {
      long start = System.nanoTime();
      observed += supplier.getAsLong();
      durations[i] = System.nanoTime() - start;
    }
    return new Measurement(operation, durations, observed);
  }

  protected static void writeMeasurements(BenchmarkOptions options, List<Measurement> measurements, long visibleTasks)
      throws IOException {
    Path outputFile = Path.of(options.outputFile);
    Path parent = outputFile.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    boolean writeHeader = !Files.exists(outputFile);
    List<String> lines = new ArrayList<>();
    if (writeHeader) {
      lines.add(CSV_HEADER);
    }

    for (Measurement measurement : measurements) {
      lines.add(measurement.toCsv(options, visibleTasks));
      System.out.println(measurement.toConsoleLine(options));
    }

    if (Files.exists(outputFile)) {
      Files.write(outputFile, lines, StandardCharsets.UTF_8,
          java.nio.file.StandardOpenOption.WRITE,
          java.nio.file.StandardOpenOption.APPEND);
    } else {
      Files.write(outputFile, lines, StandardCharsets.UTF_8,
          java.nio.file.StandardOpenOption.WRITE,
          java.nio.file.StandardOpenOption.CREATE_NEW);
    }
  }

  protected record BenchmarkOptions(String branch, String commit, String database, int taskCount, int groupCount,
                                    int pageSize, int warmupIterations, int iterations, String outputFile,
                                    boolean cleanupAfterRun) {

    static BenchmarkOptions fromSystemProperties() {
      return new BenchmarkOptions(
          property("benchmark.branch", "unknown"),
          property("benchmark.commit", "unknown"),
          property("benchmark.database", "unknown"),
          intProperty("benchmark.tasks", 5000),
          intProperty("benchmark.groups", 10),
          intProperty("benchmark.pageSize", 15),
          intProperty("benchmark.warmup", 30),
          intProperty("benchmark.iterations", 200),
          property("benchmark.outputFile", "target/task-authorization-exists-benchmark.csv"),
          booleanProperty("benchmark.cleanup", true));
    }

    int matchingAuthorizations() {
      return 2 + groupCount * 2;
    }

    static String property(String name, String defaultValue) {
      String value = System.getProperty(name);
      return value == null || value.isBlank() ? defaultValue : value;
    }

    static int intProperty(String name, int defaultValue) {
      String value = System.getProperty(name);
      return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    static boolean booleanProperty(String name, boolean defaultValue) {
      String value = System.getProperty(name);
      return value == null || value.isBlank() ? defaultValue : Boolean.parseBoolean(value);
    }
  }

  protected record Measurement(String operation, long[] durationsNanos, long observedSum) {

    String toCsv(BenchmarkOptions options, long visibleTasks) {
      long[] sorted = sortedDurations();
      return String.join(",",
          Instant.now().toString(),
          options.branch,
          options.commit,
          options.database,
          Integer.toString(options.taskCount),
          Integer.toString(options.groupCount),
          Integer.toString(options.matchingAuthorizations()),
          Integer.toString(options.pageSize),
          Integer.toString(options.warmupIterations),
          Integer.toString(options.iterations),
          Long.toString(visibleTasks),
          operation,
          formatMs(totalNanos()),
          formatMs(totalNanos() / (double) durationsNanos.length),
          formatMs(sorted[0]),
          formatMs(percentile(sorted, 0.50)),
          formatMs(percentile(sorted, 0.95)),
          formatMs(sorted[sorted.length - 1]),
          Long.toString(observedSum));
    }

    String toConsoleLine(BenchmarkOptions options) {
      long[] sorted = sortedDurations();
      return "%s %s %s: avg=%sms p50=%sms p95=%sms max=%sms".formatted(
          options.branch,
          options.database,
          operation,
          formatMs(totalNanos() / (double) durationsNanos.length),
          formatMs(percentile(sorted, 0.50)),
          formatMs(percentile(sorted, 0.95)),
          formatMs(sorted[sorted.length - 1]));
    }

    long totalNanos() {
      long total = 0;
      for (long duration : durationsNanos) {
        total += duration;
      }
      return total;
    }

    long[] sortedDurations() {
      long[] sorted = Arrays.copyOf(durationsNanos, durationsNanos.length);
      Arrays.sort(sorted);
      return sorted;
    }

    static long percentile(long[] sorted, double percentile) {
      int index = (int) Math.ceil(percentile * sorted.length) - 1;
      return sorted[Math.max(0, Math.min(index, sorted.length - 1))];
    }

    static String formatMs(long nanos) {
      return formatMs((double) nanos);
    }

    static String formatMs(double nanos) {
      return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000D);
    }
  }
}
