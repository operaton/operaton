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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
 * @author Tassilo Weidner
 */
public class TopicSubscriptionManager implements Runnable {

  protected static final TopicSubscriptionManagerLogger LOG = ExternalTaskClientLogger.TOPIC_SUBSCRIPTION_MANAGER_LOGGER;

  protected ReentrantLock ACQUISITION_MONITOR = new ReentrantLock(false);
  protected Condition IS_WAITING = ACQUISITION_MONITOR.newCondition();
  protected AtomicBoolean isRunning = new AtomicBoolean(false);

  protected ExternalTaskServiceImpl externalTaskService;

  protected EngineClient engineClient;

  protected CopyOnWriteArrayList<TopicSubscription> subscriptions;
  protected List<TopicRequestDto> taskTopicRequests;
  protected Map<String, ExternalTaskHandler> externalTaskHandlers;

  protected Thread thread;

  protected BackoffStrategy backoffStrategy;
  protected AtomicBoolean isBackoffStrategyDisabled;

  protected TypedValues typedValues;

  protected long clientLockDuration;

  public TopicSubscriptionManager(EngineClient engineClient, TypedValues typedValues, long clientLockDuration) {
    this.engineClient = engineClient;
    this.subscriptions = new CopyOnWriteArrayList<>();
    this.taskTopicRequests = new ArrayList<>();
    this.externalTaskHandlers = new HashMap<>();
    this.clientLockDuration = clientLockDuration;
    this.typedValues = typedValues;
    this.externalTaskService = new ExternalTaskServiceImpl(engineClient);
    this.isBackoffStrategyDisabled = new AtomicBoolean(false);
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

    if (!taskTopicRequests.isEmpty()) {
      FetchAndLockResponseDto fetchAndLockResponse = fetchAndLock(taskTopicRequests);

      fetchAndLockResponse.getExternalTasks().forEach(externalTask -> {
        String topicName = externalTask.getTopicName();
        ExternalTaskHandler taskHandler = externalTaskHandlers.get(topicName);

        if (taskHandler != null) {
          handleExternalTask(externalTask, taskHandler);
        }
        else {
          LOG.taskHandlerIsNull(topicName);
        }
      });

      if (!isBackoffStrategyDisabled.get()) {
        runBackoffStrategy(fetchAndLockResponse);
      }
    }
  }

  protected void prepareAcquisition(TopicSubscription subscription) {
    TopicRequestDto taskTopicRequest = TopicRequestDto.fromTopicSubscription(subscription, clientLockDuration);
    taskTopicRequests.add(taskTopicRequest);

    String topicName = subscription.getTopicName();
    ExternalTaskHandler externalTaskHandler = subscription.getExternalTaskHandler();
    externalTaskHandlers.put(topicName, externalTaskHandler);
  }

  protected FetchAndLockResponseDto fetchAndLock(List<TopicRequestDto> subscriptions) {
    List<ExternalTask> externalTasks = null;

    try {
      LOG.fetchAndLock(subscriptions);
      externalTasks = engineClient.fetchAndLock(subscriptions);

    } catch (EngineClientException ex) {
      LOG.exceptionWhilePerformingFetchAndLock(ex);
      return new FetchAndLockResponseDto(LOG.handledEngineClientException("fetching and locking task", ex));
    }

    return new FetchAndLockResponseDto(externalTasks);
  }

  @SuppressWarnings("rawtypes")
  protected void handleExternalTask(ExternalTask externalTask, ExternalTaskHandler taskHandler) {
    ExternalTaskImpl task = (ExternalTaskImpl) externalTask;

    Map<String, TypedValueField> variables = task.getVariables();
    Map<String, VariableValue> wrappedVariables = typedValues.wrapVariables(task, variables);
    task.setReceivedVariableMap(wrappedVariables);

    try {
      taskHandler.execute(task, externalTaskService);
    } catch (ExternalTaskClientException e) {
      LOG.exceptionOnExternalTaskServiceMethodInvocation(task.getTopicName(), e);
    } catch (Exception e) {
      LOG.exceptionWhileExecutingExternalTaskHandler(task.getTopicName(), e);
    }
  }

  public synchronized void stop() {
    if (isRunning.compareAndSet(true, false)) {
      resume();

      try {
        thread.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.exceptionWhileShuttingDown(e);
      }
    }
  }

  public synchronized void start() {
    if (isRunning.compareAndSet(false, true)) {
      thread = new Thread(this, TopicSubscriptionManager.class.getSimpleName());
      thread.start();
    }
  }

  protected void subscribe(TopicSubscription subscription) {
    if (!subscriptions.addIfAbsent(subscription)) {
      String topicName = subscription.getTopicName();
      throw LOG.topicNameAlreadySubscribedException(topicName);
    }

    resume();
  }

  protected void unsubscribe(TopicSubscriptionImpl subscription) {
    subscriptions.remove(subscription);
  }

  public EngineClient getEngineClient() {
    return engineClient;
  }

  public List<TopicSubscription> getSubscriptions() {
    return subscriptions;
  }

  public boolean isRunning() {
    return isRunning.get();
  }

  public void setBackoffStrategy(BackoffStrategy backOffStrategy) {
    this.backoffStrategy = backOffStrategy;
  }

  protected void runBackoffStrategy(FetchAndLockResponseDto fetchAndLockResponse) {
    try {
      List<ExternalTask> externalTasks = fetchAndLockResponse.getExternalTasks();
      if (backoffStrategy instanceof ErrorAwareBackoffStrategy errorAwareBackoffStrategy) {
        ExternalTaskClientException exception = fetchAndLockResponse.getError();
        errorAwareBackoffStrategy.reconfigure(externalTasks, exception);
      } else {
        backoffStrategy.reconfigure(externalTasks);
      }

      long waitTime = backoffStrategy.calculateBackoffTime();
      suspend(waitTime);
    } catch (Exception e) {
      LOG.exceptionWhileExecutingBackoffStrategyMethod(e);
    }
  }

  protected void suspend(long waitTime) {
    if (waitTime > 0 && isRunning.get()) {
      ACQUISITION_MONITOR.lock();
      try {
        if (isRunning.get()) {
          long endTime = System.currentTimeMillis() + waitTime;
          long remainingTime = waitTime;
          // Loop until either the wait times out or a resume signal is received
          while (remainingTime > 0 && isRunning.get()) {
            boolean wasSignaled = IS_WAITING.await(remainingTime, TimeUnit.MILLISECONDS);
            // If the await was signaled, exit immediately.
            if (wasSignaled) {
              break;
            }
            // Recalculate the remaining time for the wait.
            remainingTime = endTime - System.currentTimeMillis();
          }
          // Log timeout only if no signal was received and the thread is still running
          if (remainingTime <= 0 && isRunning.get()) {
            LOG.timeout(waitTime);
          }
        }
      } catch (InterruptedException e) {
        // we ignore and just continue to execute tasks
        LOG.exceptionWhileExecutingBackoffStrategyMethod(e);
      } finally {
        ACQUISITION_MONITOR.unlock();
      }
    }
  }

  protected void resume() {
    ACQUISITION_MONITOR.lock();
    try {
      IS_WAITING.signal();
    }
    finally {
      ACQUISITION_MONITOR.unlock();
    }
  }

  public void disableBackoffStrategy() {
    this.isBackoffStrategyDisabled.set(true);
  }

}
