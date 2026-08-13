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

import org.junit.jupiter.api.Test;

import org.operaton.connect.a2a.A2aResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Output mapping: what a modeller can actually reference from an {@code operaton:outputParameter}.
 */
class A2aResponseImplTest {

  private static final int MAX = 64 * 1024;

  @Test
  void textIsTheTextOfTheFinalMessage() {
    A2aResponse response = response(FakeA2aAgent.snapshot("t1", "c1", A2aErrors.STATE_COMPLETED, "The answer is 42"));

    assertThat(response.getText()).isEqualTo("The answer is 42");
    assertThat(response.getTaskId()).isEqualTo("t1");
    assertThat(response.getContextId()).isEqualTo("c1");
    assertThat(response.getState()).isEqualTo(A2aErrors.STATE_COMPLETED);
    assertThat(response.isTruncated()).isFalse();
  }

  @Test
  void multipleTextPartsAreJoinedByNewlines() {
    Map<String, Object> message = Map.of("parts", List.of(
        Map.of("type", "text", "text", "first"),
        Map.of("type", "data", "data", Map.of("ignored", true)),
        Map.of("type", "text", "text", "second")));
    A2aAgent.TaskSnapshot snapshot = new A2aAgent.TaskSnapshot("t1", "c1", A2aErrors.STATE_COMPLETED,
        message, List.of(), Map.of(), List.of(), false);

    assertThat(response(snapshot).getText()).isEqualTo("first\nsecond");
  }

  @Test
  void everyArtifactIsReachableNotJustTheFirst() {
    A2aAgent.TaskSnapshot snapshot = new A2aAgent.TaskSnapshot("t1", "c1", A2aErrors.STATE_COMPLETED,
        FakeA2aAgent.message("done"),
        List.of(FakeA2aAgent.artifact("summary", "a summary"), FakeA2aAgent.artifact("detail", "the detail")),
        Map.of(), List.of(), false);

    A2aResponse response = response(snapshot);
    assertThat(response.getArtifacts()).hasSize(2);
    Map<String, Object> parameters = response.getResponseParameters();
    assertThat(parameters.get(A2aResponse.PARAM_ARTIFACT_TEXT)).isEqualTo("a summary\nthe detail");
  }

  @Test
  void allDocumentedOutputsArePresent() {
    Map<String, Object> parameters =
        response(FakeA2aAgent.snapshot("t1", "c1", A2aErrors.STATE_COMPLETED, "hi")).getResponseParameters();

    assertThat(parameters).containsKeys(A2aResponse.PARAM_TEXT, A2aResponse.PARAM_ARTIFACT_TEXT,
        A2aResponse.PARAM_STATUS_MESSAGE, A2aResponse.PARAM_ARTIFACTS, A2aResponse.PARAM_TASK,
        A2aResponse.PARAM_TASK_ID, A2aResponse.PARAM_CONTEXT_ID, A2aResponse.PARAM_STATE,
        A2aResponse.PARAM_TASK_METADATA, A2aResponse.PARAM_MESSAGE_METADATA, A2aResponse.PARAM_TRUNCATED);
  }

  @Test
  void historyIsOnlyExposedWhenAskedFor() {
    A2aAgent.TaskSnapshot snapshot = FakeA2aAgent.snapshot("t1", "c1", A2aErrors.STATE_COMPLETED, "hi");

    assertThat(new A2aResponseImpl(snapshot, false, MAX).getResponseParameters())
        .doesNotContainKey(A2aResponse.PARAM_HISTORY);
    assertThat(new A2aResponseImpl(snapshot, true, MAX).getResponseParameters())
        .containsKey(A2aResponse.PARAM_HISTORY);
  }

  @Test
  void theTaskOutputCarriesTheWholeTask() {
    Map<String, Object> parameters =
        response(FakeA2aAgent.snapshot("t1", "c1", A2aErrors.STATE_COMPLETED, "hi")).getResponseParameters();

    assertThat(parameters.get(A2aResponse.PARAM_TASK)).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> task = (Map<String, Object>) parameters.get(A2aResponse.PARAM_TASK);
    assertThat(task).containsEntry("id", "t1").containsEntry("contextId", "c1").containsKeys("status", "artifacts");
  }

  @Test
  void metadataOfTheFinalMessageIsExposedSeparately() {
    Map<String, Object> parameters =
        response(FakeA2aAgent.snapshot("t1", "c1", A2aErrors.STATE_COMPLETED, "hi")).getResponseParameters();

    assertThat(parameters.get(A2aResponse.PARAM_MESSAGE_METADATA)).isEqualTo(Map.of("model", "test-model"));
  }

  @Test
  void anAnswerWithoutATaskStillMapsItsText() {
    A2aAgent.TaskSnapshot snapshot = new A2aAgent.TaskSnapshot(null, "c1", null,
        FakeA2aAgent.message("just a message"), List.of(), Map.of(), List.of(), false);

    A2aResponse response = response(snapshot);
    assertThat(response.getText()).isEqualTo("just a message");
    assertThat(response.getTaskId()).isNull();
    assertThat(response.getResponseParameters().get(A2aResponse.PARAM_TASK)).isNull();
  }

  @Test
  void anEmptyStatusMessageDoesNotBlowUp() {
    A2aAgent.TaskSnapshot snapshot = new A2aAgent.TaskSnapshot("t1", "c1", A2aErrors.STATE_COMPLETED,
        null, List.of(), Map.of(), List.of(), false);

    A2aResponse response = response(snapshot);
    assertThat(response.getText()).isEmpty();
    assertThat(response.getResponseParameters().get(A2aResponse.PARAM_MESSAGE_METADATA)).isEqualTo(Map.of());
  }

  @Test
  void tooMuchTextIsCutShortAndFlagged() {
    String tooLong = "x".repeat(120);
    A2aAgent.TaskSnapshot snapshot = new A2aAgent.TaskSnapshot("t1", "c1", A2aErrors.STATE_COMPLETED,
        FakeA2aAgent.message(tooLong), List.of(), Map.of(), List.of(), false);

    A2aResponseImpl response = new A2aResponseImpl(snapshot, false, 100);
    Map<String, Object> parameters = response.getResponseParameters();

    assertThat((String) parameters.get(A2aResponse.PARAM_TEXT)).startsWith("x".repeat(100)).contains("truncated");
    assertThat(parameters.get(A2aResponse.PARAM_TRUNCATED)).isEqualTo(true);
  }

  @Test
  void truncationFlaggedByTheAgentReaderIsPassedThrough() {
    A2aAgent.TaskSnapshot snapshot = new A2aAgent.TaskSnapshot("t1", "c1", A2aErrors.STATE_COMPLETED,
        FakeA2aAgent.message("small"), List.of(), Map.of(), List.of(), true);

    assertThat(response(snapshot).isTruncated()).isTrue();
  }

  private static A2aResponse response(A2aAgent.TaskSnapshot snapshot) {
    return new A2aResponseImpl(snapshot, false, MAX);
  }

}
