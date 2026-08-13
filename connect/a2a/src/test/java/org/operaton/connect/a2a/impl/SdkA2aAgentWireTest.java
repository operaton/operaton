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

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import org.operaton.connect.a2a.A2aResponse;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real A2A wire protocol, rather than a scripted agent.
 *
 * <p>
 * The card and the JSON-RPC payloads here are copies of what a real A2A v1.0 agent (google-adk with
 * {@code a2a-sdk} 1.1.2) actually put on the wire, down to the details that matter: the result is wrapped in a
 * {@code task} envelope, parts carry a bare {@code text} field with no kind discriminator, and the answer comes
 * back as an artifact rather than as a closing status message.
 * </p>
 */
@WireMockTest
class SdkA2aAgentWireTest {

  private static final String AGENT_CARD_PATH = "/.well-known/agent-card.json";

  /** As returned by the agent: the answer is an artifact and there is no closing status message. */
  private static final String COMPLETED_TASK = """
      {"jsonrpc":"2.0","id":"1","result":{"task":{
        "id":"task-1",
        "contextId":"ctx-1",
        "status":{"state":"TASK_STATE_COMPLETED"},
        "artifacts":[{"artifactId":"reply","parts":[{"text":"ping"}],"extensions":[]}],
        "history":[]
      }}}""";

  private static final String WORKING_TASK = """
      {"jsonrpc":"2.0","id":"1","result":{"task":{
        "id":"task-1",
        "contextId":"ctx-1",
        "status":{"state":"TASK_STATE_WORKING"},
        "artifacts":[],
        "history":[]
      }}}""";

  private static final String FAILED_TASK = """
      {"jsonrpc":"2.0","id":"1","result":{"task":{
        "id":"task-1",
        "contextId":"ctx-1",
        "status":{"state":"TASK_STATE_FAILED","message":{"messageId":"m1",
          "role":"ROLE_AGENT","parts":[{"text":"I could not do that"}]}},
        "artifacts":[],
        "history":[]
      }}}""";

  /**
   * GetTask returns the task directly, without the {@code task} envelope that SendMessage uses. SendMessage can
   * answer with either a task or a bare message, so it needs the discriminator; GetTask cannot.
   */
  private static final String COMPLETED_TASK_UNWRAPPED = """
      {"jsonrpc":"2.0","id":"1","result":{
        "id":"task-1",
        "contextId":"ctx-1",
        "status":{"state":"TASK_STATE_COMPLETED"},
        "artifacts":[{"artifactId":"reply","parts":[{"text":"ping"}],"extensions":[]}],
        "history":[]
      }}""";

  /** What an agent without the optional ListTasks method answers. */
  private static final String METHOD_NOT_FOUND = """
      {"jsonrpc":"2.0","id":"1","error":{"code":-32601,"message":"Method not found"}}""";

  @Test
  void sendsAMessageAndMapsTheTaskThatComesBack(WireMockRuntimeInfo wireMock) {
    stubCard(wireMock);
    stubMethod("SendMessage", COMPLETED_TASK);

    A2aAgent.TaskSnapshot snapshot = agent(wireMock).send(command("Say ping"));

    assertThat(snapshot.taskId()).isEqualTo("task-1");
    assertThat(snapshot.contextId()).isEqualTo("ctx-1");
    assertThat(snapshot.state()).isEqualTo(A2aErrors.STATE_COMPLETED);
    assertThat(snapshot.artifacts()).hasSize(1);
    assertThat(snapshot.truncated()).isFalse();
  }

  @Test
  void theAnswerIsReadableAsPlainTextEvenThoughItArrivedAsAnArtifact() {
    // The whole point of the text fallback: this agent shape returns no closing status message at all.
    A2aAgent.TaskSnapshot snapshot = new A2aAgent.TaskSnapshot("task-1", "ctx-1", A2aErrors.STATE_COMPLETED,
        null, List.of(FakeA2aAgent.artifact("reply", "ping")), Map.of(), List.of(), false);

    assertThat(new A2aResponseImpl(snapshot, false, 65536).getText()).isEqualTo("ping");
  }

  @Test
  void theOutgoingRequestCarriesTheDeterministicMessageIdAndTheText(WireMockRuntimeInfo wireMock) {
    stubCard(wireMock);
    stubMethod("SendMessage", COMPLETED_TASK);

    agent(wireMock).send(command("Say ping"));

    verify(postRequestedFor(urlPathEqualTo("/"))
        .withRequestBody(matchingJsonPath("$.method", equalTo("SendMessage")))
        .withRequestBody(matchingJsonPath("$.params.message.messageId", equalTo("msg-deterministic")))
        .withRequestBody(matchingJsonPath("$.params.message.parts[0].text", equalTo("Say ping"))));
  }

  /**
   * Regression test at the wire level. {@code ListTasks} is optional and this is the exact response a real agent
   * gives when it does not implement it. It must not be allowed to fail the send.
   */
  @Test
  void anAgentWithoutListTasksDegradesInsteadOfFailing(WireMockRuntimeInfo wireMock) {
    stubCard(wireMock);
    stubMethod("ListTasks", METHOD_NOT_FOUND);

    Optional<String> found = agent(wireMock).findLatestTaskId("ctx-1");

    assertThat(found).isEmpty();
  }

  @Test
  void readsAKnownTask(WireMockRuntimeInfo wireMock) {
    stubCard(wireMock);
    stubMethod("GetTask", COMPLETED_TASK_UNWRAPPED);

    A2aAgent.TaskSnapshot snapshot = agent(wireMock).getTask("task-1", null);

    assertThat(snapshot.taskId()).isEqualTo("task-1");
    assertThat(snapshot.state()).isEqualTo(A2aErrors.STATE_COMPLETED);
  }

  @Test
  void aTaskStillWorkingIsReportedAsSuchRatherThanWaitedOnHere(WireMockRuntimeInfo wireMock) {
    stubCard(wireMock);
    stubMethod("SendMessage", WORKING_TASK);

    A2aAgent.TaskSnapshot snapshot = agent(wireMock).send(command("take your time"));

    assertThat(snapshot.state()).isEqualTo(A2aErrors.STATE_WORKING);
    assertThat(A2aErrors.isSettled(snapshot.state())).isFalse();
  }

  @Test
  void aFailedTaskKeepsTheAgentsExplanation(WireMockRuntimeInfo wireMock) {
    stubCard(wireMock);
    stubMethod("SendMessage", FAILED_TASK);

    A2aAgent.TaskSnapshot snapshot = agent(wireMock).send(command("do the impossible"));

    assertThat(snapshot.state()).isEqualTo(A2aErrors.STATE_FAILED);
    assertThat(new A2aResponseImpl(snapshot, false, 65536).getText()).isEqualTo("I could not do that");
  }

  private static void stubCard(WireMockRuntimeInfo wireMock) {
    String base = wireMock.getHttpBaseUrl();
    stubFor(get(urlEqualTo(AGENT_CARD_PATH)).willReturn(okJson("""
        {
          "name":"stub-agent",
          "description":"A2A v1.0 stub",
          "version":"1",
          "capabilities":{"streaming":false,"pushNotifications":true,"extendedAgentCard":false},
          "defaultInputModes":["text/plain"],
          "defaultOutputModes":["text/plain"],
          "skills":[],
          "url":"%s",
          "preferredTransport":"JSONRPC",
          "supportedInterfaces":[{"url":"%s","protocolBinding":"JSONRPC","protocolVersion":"1.0"}]
        }""".formatted(base, base))));
  }

  private static void stubMethod(String method, String response) {
    stubFor(post(urlPathEqualTo("/"))
        .withRequestBody(matchingJsonPath("$.method", equalTo(method)))
        .willReturn(okJson(response)));
  }

  private static A2aAgent agent(WireMockRuntimeInfo wireMock) {
    return new SdkA2aAgent(new A2aAgent.Config(wireMock.getHttpBaseUrl(), Map.of(), 5_000, 10_000, 65_536));
  }

  private static A2aAgent.SendCommand command(String text) {
    return new A2aAgent.SendCommand("msg-deterministic", null, null, text, List.of(), Map.of(), List.of(),
        List.of(), Map.of(), null, List.of(), null, null, null, null, null, false);
  }

}
