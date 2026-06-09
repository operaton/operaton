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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.operaton.bpm.client.backoff.BackoffStrategy;
import org.operaton.bpm.client.backoff.ErrorAwareBackoffStrategy;
import org.operaton.bpm.client.exception.ExternalTaskClientException;
import org.operaton.bpm.client.impl.EngineClient;
import org.operaton.bpm.client.impl.EngineClientException;
import org.operaton.bpm.client.impl.ExternalTaskClientLogger;
import org.operaton.bpm.client.task.ExternalTask;
import org.operaton.bpm.client.task.ExternalTaskHandler;
import org.operaton.bpm.client.task.impl.ExternalTaskImpl;
import org.operaton.bpm.client.task.impl.ExternalTaskServiceImpl;
import org.operaton.bpm.client.topic.TopicSubscription;
import org.operaton.bpm.client.topic.impl.dto.FetchAndLockResponseDto;
import org.operaton.bpm.client.topic.impl.dto.TopicRequestDto;
import org.operaton.bpm.client.variable.impl.TypedValueField;
import org.operaton.bpm.client.variable.impl.TypedValues;
import org.operaton.bpm.client.variable.impl.VariableValue;

/**
 * Drives one fetch-and-lock loop for one task execution pool.
 */
public class ExecutorRunner implements Runnable {

  protected static final TopicSubscriptionManagerLogger LOG = ExternalTaskClientLogger.TOPIC_SUBSCRIPTION_MANAGER_LOGGER;
  private static final AtomicInteger INSTANCE_COUNTER = new AtomicInteger();

  protected final String threadName;
  protected final ThreadPoolExecutor taskExecutor;
  protected final double maxFetchedTasksMultiplier;
  protected final ExternalTaskExecutionStats executionStats;
  protected final ExternalTaskServiceImpl externalTaskService;
  protected final EngineClient engineClient;
  protected final TypedValues typedValues;
  protected final long clientLockDuration;

  protected CopyOnWriteArrayList<TopicSubscription> subscriptions = new CopyOnWriteArrayList<>();
  protected List<TopicRequestDto> taskTopicRequests = new ArrayList<>();
  protected Map<String, ExternalTaskHandler> externalTaskHandlers = new HashMap<>();
  protected Thread thread;
  protected BackoffStrategy backoffStrategy;
  protected AtomicBoolean isBackoffStrategyDisabled = new AtomicBoolean(false);
  protected ReentrantLock acquisitionMonitor = new ReentrantLock(false);
  protected Condition isWaiting = acquisitionMonitor.newCondition();
  protected AtomicBoolean isRunning = new AtomicBoolean(false);

  public ExecutorRunner(EngineClient engineClient, TypedValues typedValues, long clientLockDuration,
                        ThreadPoolExecutor taskExecutor, double maxFetchedTasksMultiplier,
                        ExternalTaskExecutionStats executionStats) {
    this.engineClient = engineClient;
    this.clientLockDuration = clientLockDuration;
    this.typedValues = typedValues;
    this.taskExecutor = taskExecutor;
    this.maxFetchedTasksMultiplier = maxFetchedTasksMultiplier;
    this.executionStats = executionStats;
    this.externalTaskService = new ExternalTaskServiceImpl(engineClient);
    this.threadName = ExecutorRunner.class.getSimpleName() + "-" + INSTANCE_COUNTER.incrementAndGet();
  }

  @Override
  public void run() {
    while (isRunning.get()) {
      try {
        acquire();
      }
      catch (Exception e) {
        LOG.exceptionWhileAcquiringTasks(e);
      }
    }
  }

  protected void acquire() {
    taskTopicRequests.clear();
    externalTaskHandlers.clear();
    subscriptions.forEach(this::prepareAcquisition);

    if (taskTopicRequests.isEmpty()) {
      return;
    }

    int maxTasksToFetch = calculateMaxTasksToFetch();
    if (maxTasksToFetch <= 0) {
      LOG.allThreadsAreBusy(taskExecutor.getActiveCount(), taskExecutor.getQueue().size(), getTopicNames());
      CompletableFuture.runAsync(() -> {
      }, taskExecutor).join();
      return;
    }

    FetchAndLockResponseDto fetchAndLockResponse = fetchAndLock(taskTopicRequests, maxTasksToFetch);

    fetchAndLockResponse.getExternalTasks().forEach(externalTask -> {
      String topicName = externalTask.getTopicName();
      ExternalTaskHandler taskHandler = externalTaskHandlers.get(topicName);

      if (taskHandler != null) {
        CompletableFuture.runAsync(() -> handleExternalTask(externalTask, taskHandler), taskExecutor);
      }
      else {
        LOG.taskHandlerIsNull(topicName);
      }
    });

    if (!isBackoffStrategyDisabled.get()) {
      runBackoffStrategy(fetchAndLockResponse);
    }
  }

  protected int calculateMaxTasksToFetch() {
    int executorCapacity = Math.max(1, taskExecutor.getCorePoolSize());
    int maxTasks = (int) (executorCapacity * maxFetchedTasksMultiplier);
    int tasksInProgress = taskExecutor.getActiveCount() + taskExecutor.getQueue().size();
    return maxTasks - tasksInProgress;
  }

  protected String getTopicNames() {
    return externalTaskHandlers.keySet().stream()
        .sorted()
        .collect(Collectors.joining(", "));
  }

  protected void prepareAcquisition(TopicSubscription subscription) {
    TopicRequestDto taskTopicRequest = TopicRequestDto.fromTopicSubscription(subscription, clientLockDuration);
    taskTopicRequests.add(taskTopicRequest);

    String topicName = subscription.getTopicName();
    ExternalTaskHandler externalTaskHandler = subscription.getExternalTaskHandler();
    externalTaskHandlers.put(topicName, externalTaskHandler);
  }

  protected FetchAndLockResponseDto fetchAndLock(List<TopicRequestDto> subscriptions, int maxTasks) {
    List<ExternalTask> externalTasks;

    try {
      LOG.fetchAndLock(subscriptions, maxTasks);
      externalTasks = engineClient.fetchAndLock(subscriptions, maxTasks);
    }
    catch (EngineClientException ex) {
      LOG.exceptionWhilePerformingFetchAndLock(ex);
      return new FetchAndLockResponseDto(LOG.handledEngineClientException("fetching and locking task", ex));
    }

    return new FetchAndLockResponseDto(externalTasks);
  }

  protected void handleExternalTask(ExternalTask externalTask, ExternalTaskHandler taskHandler) {
    ExternalTaskImpl task = (ExternalTaskImpl) externalTask;

    Map<String, TypedValueField> variables = task.getVariables();
    Map<String, VariableValue<?>> wrappedVariables = typedValues.wrapVariables(task, variables);
    task.setReceivedVariableMap(wrappedVariables);

    if (checkLockExpired(task)) {
      return;
    }

    long startTime = System.currentTimeMillis();
    try {
      taskHandler.execute(task, externalTaskService);
    }
    catch (ExternalTaskClientException e) {
      LOG.exceptionOnExternalTaskServiceMethodInvocation(task.getTopicName(), e);
    }
    catch (Exception e) {
      LOG.exceptionWhileExecutingExternalTaskHandler(task.getTopicName(), e);
    }
    finally {
      long executionTime = System.currentTimeMillis() - startTime;
      executionStats.recordExecution(task.getProcessDefinitionKey(), task.getTopicName(), executionTime);
    }
  }

  protected boolean checkLockExpired(ExternalTaskImpl task) {
    if (task.getLockExpirationTime() == null) {
      return false;
    }

    long timeUntilLockExpires = task.getLockExpirationTime().getTime() - System.currentTimeMillis();
    if (timeUntilLockExpires > 0) {
      return false;
    }

    LOG.taskLockAlreadyExpired(task.getId(), task.getTopicName(), task.getLockExpirationTime());
    return true;
  }

  protected void runBackoffStrategy(FetchAndLockResponseDto fetchAndLockResponse) {
    try {
      List<ExternalTask> externalTasks = fetchAndLockResponse.getExternalTasks();
      if (backoffStrategy instanceof ErrorAwareBackoffStrategy errorAwareBackoffStrategy) {
        ExternalTaskClientException exception = fetchAndLockResponse.getError();
        errorAwareBackoffStrategy.reconfigure(externalTasks, exception);
      }
      else {
        backoffStrategy.reconfigure(externalTasks);
      }

      long waitTime = backoffStrategy.calculateBackoffTime();
      suspend(waitTime);
    }
    catch (Exception e) {
      LOG.exceptionWhileExecutingBackoffStrategyMethod(e);
    }
  }

  @SuppressWarnings("java:S2142") // interruption is used as a wake-up signal for the acquisition loop
  protected void suspend(long waitTime) {
    if (waitTime > 0 && isRunning.get()) {
      acquisitionMonitor.lock();
      try {
        if (isRunning.get()) {
          long endTime = System.currentTimeMillis() + waitTime;
          long remainingTime = waitTime;
          while (remainingTime > 0 && isRunning.get()) {
            boolean wasSignaled = isWaiting.await(remainingTime, TimeUnit.MILLISECONDS);
            if (wasSignaled) {
              break;
            }
            remainingTime = endTime - System.currentTimeMillis();
          }
          if (remainingTime <= 0 && isRunning.get()) {
            LOG.timeout(waitTime);
          }
        }
      }
      catch (InterruptedException e) {
        LOG.exceptionWhileExecutingBackoffStrategyMethod(e);
      }
      finally {
        acquisitionMonitor.unlock();
      }
    }
  }

  public void subscribe(TopicSubscription subscription) {
    if (!subscriptions.addIfAbsent(subscription)) {
      String topicName = subscription.getTopicName();
      throw LOG.topicNameAlreadySubscribedException(topicName);
    }

    resume();
  }

  protected void unsubscribe(TopicSubscriptionImpl subscription) {
    subscriptions.remove(subscription);
  }

  protected void resume() {
    acquisitionMonitor.lock();
    try {
      isWaiting.signal();
    }
    finally {
      acquisitionMonitor.unlock();
    }
  }

  public synchronized void start() {
    if (isRunning.compareAndSet(false, true)) {
      thread = new Thread(this, threadName);
      thread.start();
    }
  }

  public synchronized void stop() {
    if (isRunning.compareAndSet(true, false)) {
      resume();

      try {
        if (thread != null) {
          thread.join();
        }
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.exceptionWhileShuttingDown(e);
      }
    }
  }

  public void setBackoffStrategy(BackoffStrategy backOffStrategy) {
    this.backoffStrategy = backOffStrategy.copy();
  }

  public void disableBackoffStrategy() {
    this.isBackoffStrategyDisabled.set(true);
  }

  public List<TopicSubscription> getSubscriptions() {
    return subscriptions;
  }
}
