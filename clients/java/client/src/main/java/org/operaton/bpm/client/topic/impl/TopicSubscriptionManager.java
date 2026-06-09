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
package org.operaton.bpm.client.topic.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.operaton.bpm.client.backoff.BackoffStrategy;
import org.operaton.bpm.client.backoff.ExponentialBackoffStrategy;
import org.operaton.bpm.client.impl.EngineClient;
import org.operaton.bpm.client.impl.ExternalTaskClientLogger;
import org.operaton.bpm.client.task.ExternalTaskHandler;
import org.operaton.bpm.client.task.ExternalTaskHandlerWithSpecificExecutor;
import org.operaton.bpm.client.topic.TopicSubscription;
import org.operaton.bpm.client.variable.impl.TypedValues;

/**
 * Routes topic subscriptions to acquisition runners backed by task execution pools.
 *
 * <p>Each distinct {@link ThreadPoolExecutor} gets one {@link ExecutorRunner}. Handlers
 * may opt into a dedicated executor by implementing {@link ExternalTaskHandlerWithSpecificExecutor};
 * all other handlers use the client's default executor.
 */
public class TopicSubscriptionManager {

  protected static final TopicSubscriptionManagerLogger LOG = ExternalTaskClientLogger.TOPIC_SUBSCRIPTION_MANAGER_LOGGER;

  protected final AtomicBoolean isRunning = new AtomicBoolean(false);
  protected final EngineClient engineClient;
  protected final TypedValues typedValues;
  protected final long clientLockDuration;
  protected final ThreadPoolExecutor defaultThreadPoolExecutor;
  protected final double maxFetchedTasksMultiplier;
  protected final Map<ThreadPoolExecutor, ExecutorRunner> runnersByExecutor = new ConcurrentHashMap<>();
  protected final AtomicBoolean isBackoffStrategyDisabled = new AtomicBoolean(false);
  protected final ExternalTaskExecutionStats executionStats = new ExternalTaskExecutionStats();
  protected final boolean statsSchedulerEnabled;

  protected BackoffStrategy backoffStrategy = new ExponentialBackoffStrategy();
  protected ScheduledExecutorService statsScheduler;

  public TopicSubscriptionManager(EngineClient engineClient, TypedValues typedValues, long clientLockDuration) {
    this(engineClient, typedValues, clientLockDuration, createDefaultExecutor(), 1, true);
  }

  public TopicSubscriptionManager(EngineClient engineClient, TypedValues typedValues, long clientLockDuration,
                                  ThreadPoolExecutor defaultThreadPoolExecutor, double maxFetchedTasksMultiplier,
                                  boolean statsSchedulerEnabled) {
    this.engineClient = engineClient;
    this.clientLockDuration = clientLockDuration;
    this.typedValues = typedValues;
    this.defaultThreadPoolExecutor = Objects.requireNonNull(defaultThreadPoolExecutor, "defaultThreadPoolExecutor");
    if (maxFetchedTasksMultiplier < 1) {
      throw new IllegalArgumentException("maxFetchedTasksMultiplier must be >= 1");
    }
    this.maxFetchedTasksMultiplier = maxFetchedTasksMultiplier;
    this.statsSchedulerEnabled = statsSchedulerEnabled;
  }

  protected static ThreadPoolExecutor createDefaultExecutor() {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }

  protected void subscribe(TopicSubscription subscription) {
    if (getSubscriptions().contains(subscription)) {
      throw LOG.topicNameAlreadySubscribedException(subscription.getTopicName());
    }

    ExternalTaskHandler externalTaskHandler = subscription.getExternalTaskHandler();
    if (externalTaskHandler instanceof ExternalTaskHandlerWithSpecificExecutor handlerWithSpecificExecutor) {
      addSubscription(handlerWithSpecificExecutor.getThreadPoolExecutor(), subscription);
    }
    else {
      addSubscription(defaultThreadPoolExecutor, subscription);
    }
  }

  protected void addSubscription(ThreadPoolExecutor executor, TopicSubscription subscription) {
    Objects.requireNonNull(executor, "executor");
    runnersByExecutor.computeIfAbsent(executor, this::prepareExecutorRunner)
        .subscribe(subscription);
  }

  protected ExecutorRunner prepareExecutorRunner(ThreadPoolExecutor executor) {
    ExecutorRunner executorRunner = new ExecutorRunner(engineClient, typedValues, clientLockDuration, executor,
        maxFetchedTasksMultiplier, executionStats);
    executorRunner.setBackoffStrategy(backoffStrategy);
    if (isBackoffStrategyDisabled.get()) {
      executorRunner.disableBackoffStrategy();
    }
    if (isRunning.get()) {
      executorRunner.start();
    }
    return executorRunner;
  }

  protected void unsubscribe(TopicSubscriptionImpl subscription) {
    runnersByExecutor.values().forEach(runner -> runner.unsubscribe(subscription));
  }

  public synchronized void stop() {
    if (isRunning.compareAndSet(true, false)) {
      runnersByExecutor.values().forEach(ExecutorRunner::stop);
      stopStatsScheduler();
    }
  }

  public synchronized void start() {
    if (isRunning.compareAndSet(false, true)) {
      runnersByExecutor.values().forEach(ExecutorRunner::start);
      if (statsSchedulerEnabled) {
        startStatsScheduler();
      }
    }
  }

  protected void startStatsScheduler() {
    statsScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "ExternalTaskStatsLogger");
      thread.setDaemon(true);
      return thread;
    });

    statsScheduler.scheduleAtFixedRate(() -> {
      try {
        ExternalTaskExecutionStatsLogger.logStats(executionStats);
        executionStats.reset();
      }
      catch (Exception e) {
        LOG.exceptionWhileExecutingBackoffStrategyMethod(e);
      }
    }, 5, 5, TimeUnit.MINUTES);
  }

  protected void stopStatsScheduler() {
    if (statsScheduler != null) {
      statsScheduler.shutdown();
      try {
        if (!statsScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          statsScheduler.shutdownNow();
        }
      }
      catch (InterruptedException e) {
        statsScheduler.shutdownNow();
        Thread.currentThread().interrupt();
      }
      finally {
        statsScheduler = null;
      }
    }
  }

  public void disableBackoffStrategy() {
    this.isBackoffStrategyDisabled.set(true);
    runnersByExecutor.values().forEach(ExecutorRunner::disableBackoffStrategy);
  }

  public EngineClient getEngineClient() {
    return engineClient;
  }

  public List<TopicSubscription> getSubscriptions() {
    return runnersByExecutor.values().stream()
        .flatMap(runner -> runner.getSubscriptions().stream())
        .toList();
  }

  public boolean isRunning() {
    return isRunning.get();
  }

  public void setBackoffStrategy(BackoffStrategy backOffStrategy) {
    this.backoffStrategy = backOffStrategy;
    runnersByExecutor.values().forEach(runner -> runner.setBackoffStrategy(backOffStrategy));
  }

  protected void acquire() {
    runnersByExecutor.values().forEach(ExecutorRunner::acquire);
  }

  public ExternalTaskExecutionStats getExecutionStats() {
    return executionStats;
  }
}
