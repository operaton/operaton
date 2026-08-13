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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.connect.ConnectorRequestException;
import org.operaton.connect.a2a.A2aRequest;
import org.operaton.connect.a2a.A2aResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers what the connector does with a request and with whatever the agent answers, using a scripted agent so
 * that timing and failure modes are exact.
 */
class A2aConnectorImplTest {

  private static final String URL = "https://agent.example.com";
  private static final String KEY = "process-instance-1-activity-instance-1";

  private FakeA2aAgent agent;
  private A2aConnectorImpl connector;

  @BeforeEach
  void setUp() {
    agent = new FakeA2aAgent();
    connector = new A2aConnectorImpl();
    connector.setAgentFactory(config -> agent);
  }

  @Test
  void sendSyncMapsTheAnswerToOutputs() {
    agent.sendResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_COMPLETED, "42"));

    A2aResponse response = connector.execute(sendSync());

    assertThat(response.getText()).isEqualTo("42");
    assertThat(response.getTaskId()).isEqualTo("task-1");
    assertThat(response.getState()).isEqualTo(A2aErrors.STATE_COMPLETED);
    assertThat(agent.sends).hasSize(1);
  }

  @Test
  void theMessageIdIsTheSameAcrossRetriesOfTheSameActivity() {
    agent.sendResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_COMPLETED, "42"));

    connector.execute(sendSync());
    connector.execute(sendSync());

    assertThat(agent.sends.get(0).messageId()).isEqualTo(agent.sends.get(1).messageId());
  }

  @Test
  void aDerivedContextIdIsUsedWhenTheModellerDoesNotSupplyOne() {
    agent.sendResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_COMPLETED, "42"));

    connector.execute(sendSync());

    assertThat(agent.sends.get(0).contextId()).isEqualTo(A2aIds.contextId(KEY));
  }

  @Test
  void aRetryReattachesInsteadOfDispatchingASecondTask() {
    agent.reattachableTaskId = java.util.Optional.of("task-1");
    agent.getTaskResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_COMPLETED, "42"));

    A2aResponse response = connector.execute(sendSync());

    assertThat(agent.sends).isEmpty();
    assertThat(agent.getTaskCalls).containsExactly("task-1");
    assertThat(response.getText()).isEqualTo("42");
  }

  @Test
  void reattachCanBeTurnedOff() {
    agent.reattachableTaskId = java.util.Optional.of("task-1");
    agent.sendResults.add(FakeA2aAgent.snapshot("task-2", "ctx-1", A2aErrors.STATE_COMPLETED, "42"));

    A2aRequest request = sendSync();
    request.setRequestParameter(A2aRequest.PARAM_REATTACH_ON_RETRY, "false");
    connector.execute(request);

    assertThat(agent.sends).hasSize(1);
  }

  @Test
  void anExplicitContextIdIsNeverProbedForReattachment() {
    // A modeller-supplied context belongs to a longer conversation, so an earlier task in it is not ours.
    agent.reattachableTaskId = java.util.Optional.of("task-from-earlier-turn");
    agent.sendResults.add(FakeA2aAgent.snapshot("task-2", "my-conversation", A2aErrors.STATE_COMPLETED, "42"));

    A2aRequest request = sendSync();
    request.setRequestParameter(A2aRequest.PARAM_CONTEXT_ID, "my-conversation");
    connector.execute(request);

    assertThat(agent.sends).hasSize(1);
    assertThat(agent.sends.get(0).contextId()).isEqualTo("my-conversation");
  }

  @Test
  void sendSyncPollsUntilTheTaskSettles() {
    agent.sendResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_SUBMITTED, null));
    agent.getTaskResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_WORKING, null));
    agent.getTaskResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_COMPLETED, "42"));

    A2aRequest request = sendSync();
    request.setRequestParameter(A2aRequest.PARAM_POLL_INTERVAL, "1");
    A2aResponse response = connector.execute(request);

    assertThat(agent.getTaskCalls).hasSize(2);
    assertThat(response.getText()).isEqualTo("42");
  }

  @Test
  void anAgentThatNeverFinishesRaisesTheTimeoutError() {
    agent.sendResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_WORKING, null));
    agent.getTaskResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_WORKING, null));

    A2aRequest request = sendSync();
    request.setRequestParameter(A2aRequest.PARAM_WAIT_TIMEOUT, "30");
    request.setRequestParameter(A2aRequest.PARAM_POLL_INTERVAL, "1");

    assertThatThrownBy(() -> connector.execute(request))
        .isInstanceOf(BpmnError.class)
        .hasFieldOrPropertyWithValue("errorCode", A2aErrors.CODE_TIMEOUT);
  }

  @Test
  void aFailedTaskBecomesACatchableBpmnError() {
    agent.sendResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_FAILED, "I could not do it"));

    assertThatThrownBy(() -> connector.execute(sendSync()))
        .isInstanceOf(BpmnError.class)
        .hasFieldOrPropertyWithValue("errorCode", A2aErrors.CODE_TASK_FAILED)
        // Outputs are not mapped on the error path, so the ids have to travel in the message.
        .hasMessageContaining("task-1");
  }

  @Test
  void aRejectedTaskAndACanceledTaskHaveTheirOwnErrorCodes() {
    agent.sendResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_REJECTED, null));
    assertThatThrownBy(() -> connector.execute(sendSync()))
        .isInstanceOf(BpmnError.class)
        .hasFieldOrPropertyWithValue("errorCode", A2aErrors.CODE_TASK_REJECTED);

    setUp();
    agent.sendResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_CANCELED, null));
    assertThatThrownBy(() -> connector.execute(sendSync()))
        .isInstanceOf(BpmnError.class)
        .hasFieldOrPropertyWithValue("errorCode", A2aErrors.CODE_TASK_CANCELED);
  }

  @Test
  void aTaskWaitingForInputIsNotAnErrorAndKeepsItsIds() {
    agent.sendResults.add(
        FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_INPUT_REQUIRED, "Which invoice do you mean?"));

    A2aResponse response = connector.execute(sendSync());

    assertThat(response.getState()).isEqualTo(A2aErrors.STATE_INPUT_REQUIRED);
    assertThat(response.getTaskId()).isEqualTo("task-1");
    assertThat(response.getContextId()).isEqualTo("ctx-1");
    assertThat(response.getText()).isEqualTo("Which invoice do you mean?");
  }

  @Test
  void aTransportFailureIsLeftForTheJobExecutorToRetry() {
    agent.sendFailure = new A2aCallException("connection reset", null, true);

    assertThatThrownBy(() -> connector.execute(sendSync()))
        .isInstanceOf(ConnectorRequestException.class)
        .isNotInstanceOf(BpmnError.class);
  }

  @Test
  void aPermanentProtocolFailureBecomesABpmnError() {
    agent.sendFailure = new A2aCallException("method not found", null, false);

    assertThatThrownBy(() -> connector.execute(sendSync()))
        .isInstanceOf(BpmnError.class)
        .hasFieldOrPropertyWithValue("errorCode", A2aErrors.CODE_PROTOCOL_ERROR);
  }

  @Test
  void sendAsyncReturnsWithoutWaiting() {
    agent.sendResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_SUBMITTED, null));

    A2aRequest request = connector.createRequest()
        .operation(A2aRequest.OPERATION_SEND_ASYNC)
        .url(URL)
        .message("do the thing");
    request.setRequestParameter(A2aRequest.PARAM_CALLBACK_URL, "https://my-engine.example.com/a2a/callback");
    request.setRequestParameter(A2aRequest.PARAM_CALLBACK_TOKEN, "shared-secret");

    A2aResponse response = connector.execute(request);

    assertThat(agent.getTaskCalls).isEmpty();
    assertThat(response.getState()).isEqualTo(A2aErrors.STATE_SUBMITTED);
    assertThat(response.getTaskId()).isEqualTo("task-1");
    A2aAgent.SendCommand sent = agent.sends.get(0);
    assertThat(sent.returnImmediately()).isTrue();
    assertThat(sent.callbackUrl()).isEqualTo("https://my-engine.example.com/a2a/callback");
    assertThat(sent.callbackToken()).isEqualTo("shared-secret");
  }

  @Test
  void getTaskReadsAKnownTaskWithoutSending() {
    agent.getTaskResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_COMPLETED, "42"));

    A2aRequest request = connector.createRequest()
        .operation(A2aRequest.OPERATION_GET_TASK)
        .url(URL)
        .taskId("task-1");

    assertThat(connector.execute(request).getText()).isEqualTo("42");
    assertThat(agent.sends).isEmpty();
  }

  @Test
  void everyPartTypeAndAllMessageFieldsReachTheAgent() {
    agent.sendResults.add(FakeA2aAgent.snapshot("task-1", "ctx-1", A2aErrors.STATE_COMPLETED, "ok"));

    A2aRequest request = sendSync();
    request.setRequestParameter(A2aRequest.PARAM_PARTS, List.of(
        Map.of("type", "text", "text", "extra text"),
        Map.of("type", "file", "uri", "https://files.example.com/a.pdf", "mimeType", "application/pdf"),
        Map.of("type", "data", "data", Map.of("invoiceNumber", "2026-1"))));
    request.setRequestParameter(A2aRequest.PARAM_METADATA, Map.of("caller", "operaton"));
    request.setRequestParameter(A2aRequest.PARAM_EXTENSIONS, "https://example.com/ext/v1");
    request.setRequestParameter(A2aRequest.PARAM_REFERENCE_TASK_IDS, List.of("earlier-task"));
    request.setRequestParameter(A2aRequest.PARAM_REQUEST_METADATA, Map.of("priority", "high"));
    request.setRequestParameter(A2aRequest.PARAM_TENANT, "tenant-a");
    request.setRequestParameter(A2aRequest.PARAM_ACCEPTED_OUTPUT_MODES, "text/plain,application/json");
    request.setRequestParameter(A2aRequest.PARAM_HISTORY_LENGTH, "5");

    connector.execute(request);

    A2aAgent.SendCommand sent = agent.sends.get(0);
    assertThat(sent.text()).isEqualTo("What is the answer?");
    assertThat(sent.parts()).hasSize(3);
    assertThat(sent.metadata()).containsEntry("caller", "operaton");
    assertThat(sent.extensions()).containsExactly("https://example.com/ext/v1");
    assertThat(sent.referenceTaskIds()).containsExactly("earlier-task");
    assertThat(sent.requestMetadata()).containsEntry("priority", "high");
    assertThat(sent.tenant()).isEqualTo("tenant-a");
    assertThat(sent.acceptedOutputModes()).containsExactly("text/plain", "application/json");
    assertThat(sent.historyLength()).isEqualTo(5);
  }

  @Test
  void missingRequiredInputsAreRejectedWithAUsefulMessage() {
    assertThatThrownBy(() -> connector.execute(connector.createRequest().url(URL).message("hi")))
        .isInstanceOf(ConnectorRequestException.class)
        .hasMessageContaining("operation");

    assertThatThrownBy(() -> connector.execute(
        connector.createRequest().operation(A2aRequest.OPERATION_SEND_SYNC).message("hi")))
        .isInstanceOf(ConnectorRequestException.class)
        .hasMessageContaining("url");

    assertThatThrownBy(() -> connector.execute(
        connector.createRequest().operation(A2aRequest.OPERATION_GET_TASK).url(URL)))
        .isInstanceOf(ConnectorRequestException.class)
        .hasMessageContaining("taskId");

    assertThatThrownBy(() -> connector.execute(
        connector.createRequest().operation(A2aRequest.OPERATION_SEND_SYNC).url(URL)))
        .isInstanceOf(ConnectorRequestException.class)
        .hasMessageContaining("parts");

    assertThatThrownBy(() -> connector.execute(
        connector.createRequest().operation("teleport").url(URL).message("hi")))
        .isInstanceOf(ConnectorRequestException.class)
        .hasMessageContaining("teleport");
  }

  @Test
  void theConnectorIsRegisteredUnderTheDocumentedId() {
    assertThat(connector.getId()).isEqualTo("a2a");
  }

  private A2aRequest sendSync() {
    A2aRequest request = connector.createRequest()
        .operation(A2aRequest.OPERATION_SEND_SYNC)
        .url(URL)
        .message("What is the answer?");
    request.setRequestParameter(A2aRequest.PARAM_IDEMPOTENCY_KEY, KEY);
    return request;
  }

}
