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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.operaton.bpm.client.backoff.BackoffStrategy;
import org.operaton.bpm.client.impl.EngineClient;
import org.operaton.bpm.client.task.ExternalTaskHandler;
import org.operaton.bpm.client.task.ExternalTaskHandlerWithSpecificExecutor;
import org.operaton.bpm.client.topic.TopicSubscription;
import org.operaton.bpm.client.variable.impl.TypedValues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopicSubscriptionManagerTest {

  private static final long CLIENT_LOCK_DURATION = 10_000L;

  EngineClient engineClient;
  TypedValues typedValues;
  ThreadPoolExecutor defaultExecutor;
  TopicSubscriptionManager manager;
  BackoffStrategy backoffStrategy;

  @BeforeEach
  void setUp() {
    engineClient = mock(EngineClient.class);
    typedValues = mock(TypedValues.class);
    defaultExecutor = new ThreadPoolExecutor(2, 2, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    manager = new TopicSubscriptionManager(engineClient, typedValues, CLIENT_LOCK_DURATION, defaultExecutor, 1.5, false);
    backoffStrategy = mock(BackoffStrategy.class);
    when(backoffStrategy.copy()).thenReturn(backoffStrategy);
    when(backoffStrategy.calculateBackoffTime()).thenReturn(100L);
    manager.setBackoffStrategy(backoffStrategy);
    when(engineClient.fetchAndLock(anyList(), anyInt())).thenReturn(Collections.emptyList());
  }

  @AfterEach
  void tearDown() {
    manager.stop();
    defaultExecutor.shutdownNow();
  }

  @Test
  void routesSubscriptionsToDefaultAndSpecificExecutors() {
    ThreadPoolExecutor customExecutor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>());
    try {
      TopicSubscription defaultSubscription = createSubscription("defaultTopic", mock(ExternalTaskHandler.class));
      ExternalTaskHandlerWithSpecificExecutor customHandler = mock(ExternalTaskHandlerWithSpecificExecutor.class);
      when(customHandler.getThreadPoolExecutor()).thenReturn(customExecutor);
      TopicSubscription customSubscription = createSubscription("customTopic", customHandler);

      manager.subscribe(defaultSubscription);
      manager.subscribe(customSubscription);

      assertThat(manager.runnersByExecutor).containsOnlyKeys(defaultExecutor, customExecutor);
      assertThat(manager.getSubscriptions()).containsExactlyInAnyOrder(defaultSubscription, customSubscription);
    }
    finally {
      customExecutor.shutdownNow();
    }
  }

  @Test
  void fetchesPerExecutorUsingEachExecutorCapacity() {
    ThreadPoolExecutor customExecutor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>());
    try {
      TopicSubscription defaultSubscription = createSubscription("defaultTopic", mock(ExternalTaskHandler.class));
      ExternalTaskHandlerWithSpecificExecutor customHandler = mock(ExternalTaskHandlerWithSpecificExecutor.class);
      when(customHandler.getThreadPoolExecutor()).thenReturn(customExecutor);
      TopicSubscription customSubscription = createSubscription("customTopic", customHandler);
      manager.subscribe(defaultSubscription);
      manager.subscribe(customSubscription);

      manager.acquire();

      ArgumentCaptor<Integer> maxTasksCaptor = ArgumentCaptor.forClass(Integer.class);
      verify(engineClient, times(2)).fetchAndLock(anyList(), maxTasksCaptor.capture());
      assertThat(maxTasksCaptor.getAllValues()).containsExactlyInAnyOrder(3, 1);
    }
    finally {
      customExecutor.shutdownNow();
    }
  }

  @Test
  void startsRunnersAddedWhileManagerIsAlreadyRunning() {
    manager.start();

    TopicSubscription subscription = createSubscription("topic", mock(ExternalTaskHandler.class));
    manager.subscribe(subscription);

    ExecutorRunner runner = manager.runnersByExecutor.get(defaultExecutor);
    assertThat(runner.isRunning.get()).isTrue();

    manager.stop();

    assertThat(runner.isRunning.get()).isFalse();
  }

  private TopicSubscription createSubscription(String topicName, ExternalTaskHandler handler) {
    TopicSubscription subscription = mock(TopicSubscription.class);
    when(subscription.getTopicName()).thenReturn(topicName);
    when(subscription.getExternalTaskHandler()).thenReturn(handler);
    when(subscription.getLockDuration()).thenReturn(CLIENT_LOCK_DURATION);
    return subscription;
  }
}
