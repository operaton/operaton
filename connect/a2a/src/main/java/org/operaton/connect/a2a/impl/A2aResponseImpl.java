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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.operaton.connect.a2a.A2aResponse;
import org.operaton.connect.impl.AbstractConnectorResponse;

import static org.operaton.connect.a2a.impl.A2aConnectorLogger.LOG;

/**
 * Exposes an A2A task as connector output parameters.
 *
 * <p>
 * Alongside the structured values, two conveniences are produced because they are what most processes actually
 * want: {@code text}, the text of the agent's final message, and {@code artifactText}, the text of everything
 * it produced. Both are plain strings so they can go straight into a user task form or an email.
 * </p>
 */
public class A2aResponseImpl extends AbstractConnectorResponse implements A2aResponse {

  static final String PART_TYPE = "type";
  static final String PART_TYPE_TEXT = "text";

  private static final String TRUNCATION_MARKER = "... [truncated by maxVariableSize]";

  private final A2aAgent.TaskSnapshot snapshot;
  private final boolean includeHistory;
  private final int maxVariableSize;

  private boolean textTruncated;

  public A2aResponseImpl(A2aAgent.TaskSnapshot snapshot, boolean includeHistory, int maxVariableSize) {
    this.snapshot = snapshot;
    this.includeHistory = includeHistory;
    this.maxVariableSize = maxVariableSize;
  }

  @Override
  protected void collectResponseParameters(Map<String, Object> responseParameters) {
    responseParameters.put(PARAM_TEXT, getText());
    responseParameters.put(PARAM_ARTIFACT_TEXT, artifactText());
    responseParameters.put(PARAM_STATUS_MESSAGE, snapshot.statusMessage());
    responseParameters.put(PARAM_ARTIFACTS, snapshot.artifacts());
    responseParameters.put(PARAM_TASK, task());
    responseParameters.put(PARAM_TASK_ID, snapshot.taskId());
    responseParameters.put(PARAM_CONTEXT_ID, snapshot.contextId());
    responseParameters.put(PARAM_STATE, snapshot.state());
    responseParameters.put(PARAM_TASK_METADATA, snapshot.taskMetadata());
    responseParameters.put(PARAM_MESSAGE_METADATA, messageMetadata());
    responseParameters.put(PARAM_TRUNCATED, isTruncated());
    if (includeHistory) {
      responseParameters.put(PARAM_HISTORY, snapshot.history());
    }
  }

  @Override
  public String getText() {
    return bounded(textOf(partsOf(snapshot.statusMessage())), PARAM_TEXT);
  }

  @Override
  public String getTaskId() {
    return snapshot.taskId();
  }

  @Override
  public String getContextId() {
    return snapshot.contextId();
  }

  @Override
  public String getState() {
    return snapshot.state();
  }

  @Override
  public List<Map<String, Object>> getArtifacts() {
    return snapshot.artifacts();
  }

  @Override
  public boolean isTruncated() {
    // getText() and artifactText() have to have run for textTruncated to be meaningful, and
    // collectResponseParameters() calls both before it reads this.
    return snapshot.truncated() || textTruncated;
  }

  private String artifactText() {
    List<Map<String, Object>> parts = new ArrayList<>();
    for (Map<String, Object> artifact : snapshot.artifacts()) {
      parts.addAll(partsOf(artifact));
    }
    return bounded(textOf(parts), PARAM_ARTIFACT_TEXT);
  }

  private Map<String, Object> task() {
    if (snapshot.taskId() == null) {
      // The agent answered with a bare message rather than creating a task.
      return null;
    }
    Map<String, Object> status = new LinkedHashMap<>();
    status.put(PARAM_STATE, snapshot.state());
    status.put("message", snapshot.statusMessage());

    Map<String, Object> task = new LinkedHashMap<>();
    task.put("id", snapshot.taskId());
    task.put("contextId", snapshot.contextId());
    task.put("status", status);
    task.put("artifacts", snapshot.artifacts());
    task.put("metadata", snapshot.taskMetadata());
    return task;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> messageMetadata() {
    Map<String, Object> message = snapshot.statusMessage();
    if (message == null) {
      return Map.of();
    }
    Object metadata = message.get("metadata");
    return metadata instanceof Map ? (Map<String, Object>) metadata : Map.of();
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> partsOf(Map<String, Object> messageOrArtifact) {
    if (messageOrArtifact == null) {
      return List.of();
    }
    Object parts = messageOrArtifact.get("parts");
    return parts instanceof List ? (List<Map<String, Object>>) parts : List.of();
  }

  /** Joins the text of every text part, which is what a process almost always wants to read. */
  private static String textOf(List<Map<String, Object>> parts) {
    StringBuilder text = new StringBuilder();
    for (Map<String, Object> part : parts) {
      if (PART_TYPE_TEXT.equals(part.get(PART_TYPE)) && part.get(PART_TYPE_TEXT) != null) {
        if (!text.isEmpty()) {
          text.append('\n');
        }
        text.append(part.get(PART_TYPE_TEXT));
      }
    }
    return text.toString();
  }

  /**
   * Keeps a string small enough to be a process variable. Individual parts are already bounded when the task is
   * read, but joining many of them can still add up.
   */
  private String bounded(String text, String what) {
    if (text.length() <= maxVariableSize) {
      return text;
    }
    LOG.valueTruncated(what, text.length(), maxVariableSize);
    textTruncated = true;
    return text.substring(0, maxVariableSize) + TRUNCATION_MARKER;
  }

}
