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
package org.operaton.connect.a2a.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.operaton.connect.a2a.A2aConnector;
import org.operaton.connect.a2a.A2aRequest;
import org.operaton.connect.a2a.A2aResponse;
import org.operaton.connect.impl.AbstractConnector;

import static org.operaton.connect.a2a.impl.A2aConnectorLogger.LOG;

/**
 * Runs one A2A operation per service task.
 *
 * <p>
 * The three operations differ only in how long they stay: {@code sendAsync} sends and leaves, {@code sendSync}
 * sends and then polls until the task settles or the wait budget runs out, and {@code getTask} only reads.
 * All three finish by handing the resulting task to {@link A2aResponseImpl} for output mapping, and let
 * {@link A2aErrors} decide whether the process sees a {@code BpmnError}, a retry, or normal outputs.
 * </p>
 */
public class A2aConnectorImpl extends AbstractConnector<A2aRequest, A2aResponse> implements A2aConnector {

  /**
   * Clients are cached because building one reads the agent card over HTTP, which is not something to do on
   * every process instance.
   */
  private static final int MAX_CACHED_AGENTS = 64;

  private final Map<A2aAgent.Config, A2aAgent> agents = new ConcurrentHashMap<>();

  private Function<A2aAgent.Config, A2aAgent> agentFactory = SdkA2aAgent::new;

  public A2aConnectorImpl() {
    super(A2aConnector.ID);
  }

  public A2aConnectorImpl(String connectorId) {
    super(connectorId);
  }

  @Override
  public A2aRequest createRequest() {
    return new A2aRequestImpl(this);
  }

  @Override
  public A2aResponse execute(A2aRequest request) {
    String operation = A2aParams.string(request, A2aRequest.PARAM_OPERATION);
    if (operation == null) {
      throw LOG.operationRequired();
    }
    String url = A2aParams.string(request, A2aRequest.PARAM_URL);
    if (url == null) {
      throw LOG.urlRequired();
    }

    int maxVariableSize = A2aParams.integer(request, A2aRequest.PARAM_MAX_VARIABLE_SIZE,
        A2aRequest.DEFAULT_MAX_VARIABLE_SIZE);
    Map<String, String> headers = A2aParams.stringMap(request, A2aRequest.PARAM_HEADERS);
    if (!headers.isEmpty()) {
      LOG.headersSet(String.join(", ", headers.keySet()));
    }

    A2aAgent.Config config = new A2aAgent.Config(url,
        headers,
        A2aParams.integer(request, A2aRequest.PARAM_CONNECT_TIMEOUT, A2aRequest.DEFAULT_CONNECT_TIMEOUT_MS),
        A2aParams.integer(request, A2aRequest.PARAM_READ_TIMEOUT, A2aRequest.DEFAULT_READ_TIMEOUT_MS),
        maxVariableSize);
    A2aAgent agent = agentFor(config);

    A2aAgent.TaskSnapshot snapshot = invoke(agent, request, operation, url);
    A2aErrors.failIfUnsuccessful(snapshot);
    return new A2aResponseImpl(snapshot,
        A2aParams.bool(request, A2aRequest.PARAM_INCLUDE_HISTORY, false),
        maxVariableSize);
  }

  /** Runs the operation through the interceptor chain and normalises whatever comes back out of it. */
  private A2aAgent.TaskSnapshot invoke(A2aAgent agent, A2aRequest request, String operation, String url) {
    A2aRequestInvocation invocation = new A2aRequestInvocation(agent,
        () -> dispatch(agent, request, operation, url), request, requestInterceptors);
    try {
      return (A2aAgent.TaskSnapshot) invocation.proceed();
    } catch (A2aCallException e) {
      throw A2aErrors.translate(e, url);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      // Only an interceptor can get us here; the agent itself throws A2aCallException.
      throw LOG.transportFailure(url, e);
    }
  }

  protected A2aAgent.TaskSnapshot dispatch(A2aAgent agent, A2aRequest request, String operation, String url) {
    return switch (operation) {
      case A2aRequest.OPERATION_GET_TASK -> getTask(agent, request);
      case A2aRequest.OPERATION_SEND_SYNC -> sendAndWait(agent, request);
      case A2aRequest.OPERATION_SEND_ASYNC -> sendAsync(agent, request);
      default -> throw LOG.unknownOperation(operation);
    };
  }

  private A2aAgent.TaskSnapshot getTask(A2aAgent agent, A2aRequest request) {
    String taskId = A2aParams.string(request, A2aRequest.PARAM_TASK_ID);
    if (taskId == null) {
      throw LOG.taskIdRequiredForGetTask();
    }
    return agent.getTask(taskId, A2aParams.integerOrNull(request, A2aRequest.PARAM_HISTORY_LENGTH));
  }

  /**
   * Sends and returns without waiting. With a {@code callbackUrl} the agent is asked to push a notification when
   * it is done; without one, nothing pushes and the process is expected to poll with {@code getTask} on a timer.
   * Both are legitimate, so a missing callback URL is not an error.
   */
  private A2aAgent.TaskSnapshot sendAsync(A2aAgent agent, A2aRequest request) {
    return sendOrReattach(agent, request, true);
  }

  /**
   * Sends, then polls until the task settles. Polling rather than holding an SSE stream open keeps a job
   * executor thread free of long-lived connections, at the cost of up to one poll interval of latency.
   */
  private A2aAgent.TaskSnapshot sendAndWait(A2aAgent agent, A2aRequest request) {
    A2aAgent.TaskSnapshot snapshot = sendOrReattach(agent, request, false);

    long waitTimeout = A2aParams.integer(request, A2aRequest.PARAM_WAIT_TIMEOUT, A2aRequest.DEFAULT_WAIT_TIMEOUT_MS);
    long pollInterval = A2aParams.integer(request, A2aRequest.PARAM_POLL_INTERVAL, A2aRequest.DEFAULT_POLL_INTERVAL_MS);
    Integer historyLength = A2aParams.integerOrNull(request, A2aRequest.PARAM_HISTORY_LENGTH);
    long deadline = System.currentTimeMillis() + waitTimeout;

    while (!A2aErrors.isSettled(snapshot.state())) {
      long remaining = deadline - System.currentTimeMillis();
      if (remaining <= 0) {
        throw A2aErrors.timedOut(snapshot, waitTimeout);
      }
      LOG.waitingForTask(snapshot.taskId(), snapshot.state(), remaining);
      sleep(Math.min(pollInterval, remaining), snapshot.taskId());
      snapshot = agent.getTask(snapshot.taskId(), historyLength);
    }
    return snapshot;
  }

  /**
   * Reattaches to a task an earlier, rolled back attempt already created, rather than paying an agent twice for
   * the same work.
   *
   * <p>
   * This is only attempted when the context id was derived from the idempotency key, because such a context
   * holds exactly one task per activity instance. When the modeler supplies a context id explicitly the context
   * belongs to a longer conversation and an earlier task in it is not ours to reattach to.
   * </p>
   */
  private A2aAgent.TaskSnapshot sendOrReattach(A2aAgent agent, A2aRequest request, boolean returnImmediately) {
    String idempotencyKey = A2aParams.string(request, A2aRequest.PARAM_IDEMPOTENCY_KEY);
    String explicitContextId = A2aParams.string(request, A2aRequest.PARAM_CONTEXT_ID);
    String contextId = explicitContextId != null ? explicitContextId : A2aIds.contextId(idempotencyKey);
    boolean contextIsOurs = explicitContextId == null && idempotencyKey != null;

    if (contextIsOurs && A2aParams.bool(request, A2aRequest.PARAM_REATTACH_ON_RETRY, true)) {
      Optional<String> running = agent.findLatestTaskId(contextId);
      if (running.isPresent()) {
        LOG.reattachedToTask(running.get(), contextId);
        return agent.getTask(running.get(), A2aParams.integerOrNull(request, A2aRequest.PARAM_HISTORY_LENGTH));
      }
    }

    A2aAgent.SendCommand command = buildCommand(request, contextId, idempotencyKey, returnImmediately);
    LOG.sendingMessage(returnImmediately ? A2aRequest.OPERATION_SEND_ASYNC : A2aRequest.OPERATION_SEND_SYNC,
        A2aParams.string(request, A2aRequest.PARAM_URL), command.messageId());
    return agent.send(command);
  }

  private A2aAgent.SendCommand buildCommand(A2aRequest request,
                                            String contextId,
                                            String idempotencyKey,
                                            boolean returnImmediately) {
    String text = A2aParams.string(request, A2aRequest.PARAM_MESSAGE);
    List<Map<String, Object>> parts = A2aParams.mapList(request, A2aRequest.PARAM_PARTS);
    if (text == null && parts.isEmpty()) {
      throw LOG.messageOrPartsRequired();
    }
    String messageId = A2aParams.string(request, A2aRequest.PARAM_MESSAGE_ID);
    if (messageId == null) {
      messageId = A2aIds.messageId(idempotencyKey);
    }
    return new A2aAgent.SendCommand(messageId,
        contextId,
        A2aParams.string(request, A2aRequest.PARAM_TASK_ID),
        text,
        parts,
        A2aParams.objectMap(request, A2aRequest.PARAM_METADATA),
        A2aParams.stringList(request, A2aRequest.PARAM_EXTENSIONS),
        A2aParams.stringList(request, A2aRequest.PARAM_REFERENCE_TASK_IDS),
        A2aParams.objectMap(request, A2aRequest.PARAM_REQUEST_METADATA),
        A2aParams.string(request, A2aRequest.PARAM_TENANT),
        A2aParams.stringList(request, A2aRequest.PARAM_ACCEPTED_OUTPUT_MODES),
        A2aParams.integerOrNull(request, A2aRequest.PARAM_HISTORY_LENGTH),
        A2aParams.string(request, A2aRequest.PARAM_CALLBACK_URL),
        A2aParams.string(request, A2aRequest.PARAM_CALLBACK_TOKEN),
        A2aParams.string(request, A2aRequest.PARAM_CALLBACK_AUTH_SCHEME),
        A2aParams.string(request, A2aRequest.PARAM_CALLBACK_AUTH_CREDENTIALS),
        returnImmediately);
  }

  private void sleep(long millis, String taskId) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw LOG.interrupted(taskId);
    }
  }

  protected A2aAgent agentFor(A2aAgent.Config config) {
    // ponytail: fixed-size cache emptied wholesale when full. Swap for an LRU if one engine talks to
    // more than MAX_CACHED_AGENTS distinct agents and card fetches start showing up in latency.
    if (agents.size() >= MAX_CACHED_AGENTS && !agents.containsKey(config)) {
      closeAgents();
    }
    return agents.computeIfAbsent(config, agentFactory);
  }

  /**
   * Replaces how clients are created, so that a test can run the whole connector, including its polling and
   * reattachment logic, against a scripted agent instead of a real one. Discards any cached clients.
   *
   * @param agentFactory builds an agent from a config; {@code SdkA2aAgent::new} in production
   */
  public void setAgentFactory(Function<A2aAgent.Config, A2aAgent> agentFactory) {
    closeAgents();
    this.agentFactory = agentFactory;
  }

  /** Closes and forgets every cached client. Safe to call at any time; the next request rebuilds what it needs. */
  public void closeAgents() {
    for (A2aAgent agent : agents.values()) {
      try {
        agent.close();
      } catch (RuntimeException e) {
        LOG.exceptionWhileClosingAgent(e);
      }
    }
    agents.clear();
  }

}
