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
package org.operaton.bpm.client.topic.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.operaton.bpm.client.backoff.BackoffStrategy;
import org.operaton.bpm.client.impl.EngineClient;
import org.operaton.bpm.client.task.ExternalTask;
import org.operaton.bpm.client.task.ExternalTaskHandler;
import org.operaton.bpm.client.task.impl.ExternalTaskImpl;
import org.operaton.bpm.client.topic.TopicSubscription;
import org.operaton.bpm.client.variable.impl.TypedValueField;
import org.operaton.bpm.client.variable.impl.TypedValues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutorRunnerTest {

  private static final long CLIENT_LOCK_DURATION = 10_000L;

  EngineClient engineClient;
  TypedValues typedValues;
  ThreadPoolExecutor executor;
  ExternalTaskExecutionStats executionStats;
  ExecutorRunner runner;
  BackoffStrategy backoffStrategy;

  @BeforeEach
  void setUp() {
    engineClient = mock(EngineClient.class);
    typedValues = mock(TypedValues.class);
    executor = new ThreadPoolExecutor(2, 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    executionStats = new ExternalTaskExecutionStats();
    runner = new ExecutorRunner(engineClient, typedValues, CLIENT_LOCK_DURATION, executor, 2, executionStats);
    backoffStrategy = mock(BackoffStrategy.class);
    when(backoffStrategy.copy()).thenReturn(backoffStrategy);
    when(backoffStrategy.calculateBackoffTime()).thenReturn(0L);
    runner.setBackoffStrategy(backoffStrategy);
    when(typedValues.wrapVariables(any(ExternalTask.class), any())).thenReturn(Collections.emptyMap());
  }

  @AfterEach
  void tearDown() {
    runner.stop();
    executor.shutdownNow();
  }

  @Test
  void fetchesOnlyCapacityNotAlreadyUsedByExecutor() throws Exception {
    CountDownLatch releaseBlockedTask = new CountDownLatch(1);
    CountDownLatch blockedTaskStarted = new CountDownLatch(1);
    executor.submit(() -> {
      blockedTaskStarted.countDown();
      releaseBlockedTask.await();
      return null;
    });
    assertThat(blockedTaskStarted.await(2, TimeUnit.SECONDS)).isTrue();

    when(engineClient.fetchAndLock(anyList(), anyInt())).thenReturn(Collections.emptyList());
    runner.subscribe(createSubscription("topic", mock(ExternalTaskHandler.class)));

    runner.acquire();

    ArgumentCaptor<Integer> maxTasksCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(engineClient).fetchAndLock(anyList(), maxTasksCaptor.capture());
    assertThat(maxTasksCaptor.getValue()).isEqualTo(3);

    releaseBlockedTask.countDown();
  }

  @Test
  void executesFetchedTasksOnExecutorAndRecordsStats() throws Exception {
    ExternalTaskImpl task = createTask("task-1", "topic", Date.from(Instant.now().plus(1, ChronoUnit.MINUTES)));
    when(engineClient.fetchAndLock(anyList(), anyInt())).thenReturn(Collections.singletonList(task));
    CountDownLatch executed = new CountDownLatch(1);
    ExternalTaskHandler handler = (externalTask, externalTaskService) -> executed.countDown();
    runner.subscribe(createSubscription("topic", handler));

    runner.acquire();

    assertThat(executed.await(2, TimeUnit.SECONDS)).isTrue();
    await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
      ExternalTaskExecutionStats.TaskStats stats = executionStats.getStats("process-topic", "topic");
      assertThat(stats.getCount()).isEqualTo(1);
    });
  }

  @Test
  void skipsTaskWhenLockAlreadyExpired() {
    ExternalTaskImpl task = createTask("task-1", "topic", Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)));
    when(engineClient.fetchAndLock(anyList(), anyInt())).thenReturn(Collections.singletonList(task));
    ExternalTaskHandler handler = mock(ExternalTaskHandler.class);
    runner.subscribe(createSubscription("topic", handler));

    runner.acquire();

    verify(handler, after(300).never()).execute(any(ExternalTask.class), any());
    assertThat(executionStats.getStats("process-topic", "topic")).isNull();
  }

  @Test
  void reconfiguresBackoffWithFetchedTasks() {
    ExternalTaskImpl task = createTask("task-1", "topic", Date.from(Instant.now().plus(1, ChronoUnit.MINUTES)));
    when(engineClient.fetchAndLock(anyList(), anyInt())).thenReturn(Collections.singletonList(task));
    runner.subscribe(createSubscription("topic", mock(ExternalTaskHandler.class)));

    runner.acquire();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<java.util.List<ExternalTask>> tasksCaptor = ArgumentCaptor.forClass(java.util.List.class);
    verify(backoffStrategy, times(1)).reconfigure(tasksCaptor.capture());
    assertThat(tasksCaptor.getValue()).containsExactly(task);
  }

  private TopicSubscription createSubscription(String topicName, ExternalTaskHandler handler) {
    TopicSubscription subscription = mock(TopicSubscription.class);
    when(subscription.getTopicName()).thenReturn(topicName);
    when(subscription.getExternalTaskHandler()).thenReturn(handler);
    when(subscription.getLockDuration()).thenReturn(CLIENT_LOCK_DURATION);
    return subscription;
  }

  private ExternalTaskImpl createTask(String id, String topicName, Date lockExpirationTime) {
    ExternalTaskImpl task = new ExternalTaskImpl();
    task.setId(id);
    task.setTopicName(topicName);
    task.setProcessDefinitionKey("process-" + topicName);
    task.setVariables(Map.<String, TypedValueField>of());
    task.setLockExpirationTime(lockExpirationTime);
    return task;
  }
}
