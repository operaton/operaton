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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.client.backoff.BackoffStrategy;
import org.operaton.bpm.client.backoff.ExponentialBackoffStrategy;
import org.operaton.bpm.client.impl.EngineClient;
import org.operaton.bpm.client.task.ExternalTaskHandler;
import org.operaton.bpm.client.task.ExternalTaskHandlerWithSpecificExecutor;
import org.operaton.bpm.client.topic.TopicSubscription;
import org.operaton.bpm.client.variable.impl.TypedValues;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackoffStrategyIsolationTest {

  @Test
  void providesIndependentBuiltInBackoffStrategyCopiesToEachRunner() {
    EngineClient engineClient = mock(EngineClient.class);
    TypedValues typedValues = mock(TypedValues.class);
    when(engineClient.fetchAndLock(anyList(), anyInt())).thenReturn(Collections.emptyList());

    ThreadPoolExecutor defaultExecutor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>());
    ThreadPoolExecutor customExecutor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>());
    try {
      TopicSubscriptionManager manager = new TopicSubscriptionManager(engineClient, typedValues, 10_000L,
          defaultExecutor, 1, false);
      ExponentialBackoffStrategy sharedStrategy = new ExponentialBackoffStrategy(100L, 2, 5_000L);
      manager.setBackoffStrategy(sharedStrategy);

      manager.subscribe(createSubscription("topic1", mock(ExternalTaskHandler.class)));
      ExternalTaskHandlerWithSpecificExecutor customHandler = mock(ExternalTaskHandlerWithSpecificExecutor.class);
      when(customHandler.getThreadPoolExecutor()).thenReturn(customExecutor);
      manager.subscribe(createSubscription("topic2", customHandler));

      assertThat(manager.runnersByExecutor).hasSize(2);
      BackoffStrategy firstCopy = manager.runnersByExecutor.get(defaultExecutor).backoffStrategy;
      BackoffStrategy secondCopy = manager.runnersByExecutor.get(customExecutor).backoffStrategy;

      assertThat(firstCopy).isNotSameAs(sharedStrategy);
      assertThat(secondCopy).isNotSameAs(sharedStrategy);
      assertThat(firstCopy).isNotSameAs(secondCopy);

      firstCopy.reconfigure(Collections.emptyList());
      secondCopy.reconfigure(Collections.emptyList());
      secondCopy.reconfigure(Collections.emptyList());

      assertThat(firstCopy.calculateBackoffTime()).isEqualTo(100L);
      assertThat(secondCopy.calculateBackoffTime()).isEqualTo(200L);
    }
    finally {
      defaultExecutor.shutdownNow();
      customExecutor.shutdownNow();
    }
  }

  private TopicSubscription createSubscription(String topicName, ExternalTaskHandler handler) {
    TopicSubscription subscription = mock(TopicSubscription.class);
    when(subscription.getTopicName()).thenReturn(topicName);
    when(subscription.getExternalTaskHandler()).thenReturn(handler);
    when(subscription.getLockDuration()).thenReturn(10_000L);
    return subscription;
  }
}
