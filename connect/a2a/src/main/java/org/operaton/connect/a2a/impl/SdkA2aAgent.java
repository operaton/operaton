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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.Gson;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientEvent;
import org.a2aproject.sdk.client.MessageEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.TaskUpdateEvent;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.http.A2ACardResolver;
import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfig;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResult;
import org.a2aproject.sdk.spec.A2AClientHTTPError;
import org.a2aproject.sdk.spec.A2AClientJSONError;
import org.a2aproject.sdk.spec.A2AProtocolError;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.Artifact;
import org.a2aproject.sdk.spec.AuthenticationInfo;
import org.a2aproject.sdk.spec.DataPart;
import org.a2aproject.sdk.spec.FileContent;
import org.a2aproject.sdk.spec.FilePart;
import org.a2aproject.sdk.spec.FileWithBytes;
import org.a2aproject.sdk.spec.FileWithUri;
import org.a2aproject.sdk.spec.ListTasksParams;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.MessageSendConfiguration;
import org.a2aproject.sdk.spec.MessageSendParams;
import org.a2aproject.sdk.spec.Part;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskPushNotificationConfig;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskStatus;
import org.a2aproject.sdk.spec.TextPart;

import static org.operaton.connect.a2a.impl.A2aConnectorLogger.LOG;

/**
 * The only class that knows the A2A Java SDK exists.
 *
 * <p>
 * Everything it returns is a plain {@code String}, {@code Map} or {@code List}, so a change to the SDK or to the
 * protocol version is contained here and in {@link TimeoutAwareA2AHttpClient}. In exchange this class is longer
 * than the rest of the module put together, which is the intended trade.
 * </p>
 *
 * <p>
 * Streaming is deliberately switched off. The SDK would happily consume a server-sent-event stream, but doing so
 * on a job executor thread means holding a connection open for as long as the agent takes to think. The connector
 * sends once and polls instead.
 * </p>
 */
public class SdkA2aAgent implements A2aAgent {

  private static final String DEFAULT_AGENT_CARD_PATH = "/.well-known/agent-card.json";

  /** Used only to flatten agent-supplied JSON into plain collections. */
  private static final Gson GSON = new Gson();

  private static final String KEY_TYPE = "type";
  private static final String KEY_TEXT = "text";
  private static final String KEY_FILE = "file";
  private static final String KEY_DATA = "data";
  private static final String KEY_METADATA = "metadata";
  private static final String KEY_PARTS = "parts";
  private static final String KEY_MIME_TYPE = "mimeType";
  private static final String KEY_NAME = "name";
  private static final String KEY_URI = "uri";
  private static final String KEY_BYTES = "bytes";

  private final Config config;
  private final Client client;

  public SdkA2aAgent(Config config) {
    this.config = config;
    A2AHttpClient httpClient = new TimeoutAwareA2AHttpClient(config.connectTimeoutMs(), config.readTimeoutMs());
    try {
      AgentCard card = resolveCard(httpClient, config);
      this.client = Client.builder(card)
          .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig(httpClient))
          .clientConfig(ClientConfig.builder()
              .setStreaming(false)
              .setPolling(true)
              // Only the JSON-RPC transport is registered, so the client's preference has to win over a card
              // that would rather be spoken to over gRPC.
              .setUseClientPreference(true)
              .build())
          .build();
    } catch (Exception e) {
      throw classify(e, "Could not connect to the A2A agent at '%s'".formatted(config.url()));
    }
  }

  @Override
  public TaskSnapshot send(SendCommand command) {
    MessageSendParams.Builder params = MessageSendParams.builder().message(toMessage(command));
    MessageSendConfiguration configuration = toConfiguration(command);
    if (configuration != null) {
      params.configuration(configuration);
    }
    if (!command.requestMetadata().isEmpty()) {
      params.metadata(command.requestMetadata());
    }
    if (command.tenant() != null) {
      params.tenant(command.tenant());
    }

    AtomicReference<ClientEvent> result = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    try {
      client.sendMessage(params.build(), List.of((event, card) -> result.set(event)), failure::set, callContext());
    } catch (Exception e) {
      throw classify(e, "Sending a message to the A2A agent at '%s' failed".formatted(config.url()));
    }
    if (failure.get() != null) {
      throw classify(failure.get(), "The A2A agent at '%s' reported an error".formatted(config.url()));
    }
    ClientEvent event = result.get();
    if (event == null) {
      throw new A2aCallException("The A2A agent at '%s' accepted the message but returned no task or message"
          .formatted(config.url()), null, false);
    }
    return toSnapshot(event);
  }

  @Override
  public TaskSnapshot getTask(String taskId, Integer historyLength) {
    try {
      Task task = client.getTask(TaskQueryParams.builder()
          .id(taskId)
          .historyLength(historyLength)
          .build(), callContext());
      return fromTask(task);
    } catch (Exception e) {
      throw classify(e, "Reading A2A task '%s' from '%s' failed".formatted(taskId, config.url()));
    }
  }

  @Override
  public Optional<String> findLatestTaskId(String contextId) {
    try {
      ListTasksResult result = client.listTasks(ListTasksParams.builder()
          .contextId(contextId)
          .pageSize(1)
          .includeArtifacts(false)
          .build(), callContext());
      List<Task> tasks = result == null ? null : result.tasks();
      if (tasks == null || tasks.isEmpty()) {
        return Optional.empty();
      }
      // A derived context holds at most one task, the one an earlier attempt of this activity created, so the
      // first result is the one to reattach to and ordering does not matter.
      return Optional.ofNullable(tasks.get(0).id());
    } catch (Exception e) {
      // The probe is best-effort. ListTasks is optional in A2A and a great many agents answer it with
      // "method not found", so anything short of a clear transport problem has to degrade to a plain send.
      // Treating an unrecognised failure as retryable here would take down every send to such an agent.
      if (isTransportFailure(e)) {
        // The agent may well be reachable again in a moment. Failing here is better than sending a second
        // message and paying for the same work twice.
        throw classify(e, "Probing for an existing A2A task failed");
      }
      LOG.reattachProbeUnsupported(config.url());
      return Optional.empty();
    }
  }

  @Override
  public void close() {
    try {
      client.close();
    } catch (RuntimeException e) {
      LOG.exceptionWhileClosingAgent(e);
    }
  }

  private static AgentCard resolveCard(A2AHttpClient httpClient, Config config) {
    String url = config.url();
    String baseUrl;
    String cardPath;
    if (url.endsWith(".json")) {
      URI uri = URI.create(url);
      baseUrl = uri.getScheme() + "://" + uri.getAuthority();
      cardPath = uri.getRawPath();
    } else {
      baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
      cardPath = DEFAULT_AGENT_CARD_PATH;
    }
    return A2ACardResolver.builder()
        .httpClient(httpClient)
        .baseUrl(baseUrl)
        .agentCardPath(cardPath)
        .authHeaders(config.headers())
        .build()
        .getAgentCard();
  }

  private ClientCallContext callContext() {
    return new ClientCallContext(new HashMap<>(), new LinkedHashMap<>(config.headers()));
  }

  private Message toMessage(SendCommand command) {
    List<Part<?>> parts = new ArrayList<>();
    if (command.text() != null) {
      parts.add(new TextPart(command.text()));
    }
    for (Map<String, Object> part : command.parts()) {
      parts.add(toPart(part));
    }

    Message.Builder builder = Message.builder()
        .role(Message.Role.ROLE_USER)
        .messageId(command.messageId())
        .parts(parts);
    if (command.contextId() != null) {
      builder.contextId(command.contextId());
    }
    if (command.taskId() != null) {
      builder.taskId(command.taskId());
    }
    if (!command.referenceTaskIds().isEmpty()) {
      builder.referenceTaskIds(command.referenceTaskIds());
    }
    if (!command.metadata().isEmpty()) {
      builder.metadata(command.metadata());
    }
    if (!command.extensions().isEmpty()) {
      builder.extensions(command.extensions());
    }
    return builder.build();
  }

  private Part<?> toPart(Map<String, Object> part) {
    Object type = part.get(KEY_TYPE);
    String typeName = type == null ? null : type.toString();
    Map<String, Object> metadata = mapOrNull(part.get(KEY_METADATA));

    if (KEY_TEXT.equals(typeName)) {
      return new TextPart(string(part, KEY_TEXT), metadata);
    }
    if (KEY_DATA.equals(typeName)) {
      return new DataPart(part.get(KEY_DATA), metadata);
    }
    if (KEY_FILE.equals(typeName)) {
      return new FilePart(toFileContent(part), metadata);
    }
    throw LOG.unknownPartType(typeName);
  }

  private FileContent toFileContent(Map<String, Object> part) {
    String mimeType = string(part, KEY_MIME_TYPE);
    String name = string(part, KEY_NAME);
    String uri = string(part, KEY_URI);
    if (uri != null) {
      return new FileWithUri(mimeType, name, uri);
    }
    String bytes = string(part, KEY_BYTES);
    if (bytes == null) {
      throw LOG.filePartNeedsUriOrBytes();
    }
    if (bytes.length() > config.maxVariableSize()) {
      throw LOG.inlineFileTooLarge(bytes.length(), config.maxVariableSize());
    }
    return new FileWithBytes(mimeType, name, bytes);
  }

  private MessageSendConfiguration toConfiguration(SendCommand command) {
    MessageSendConfiguration.Builder builder = MessageSendConfiguration.builder();
    boolean configured = false;
    if (!command.acceptedOutputModes().isEmpty()) {
      builder.acceptedOutputModes(command.acceptedOutputModes());
      configured = true;
    }
    if (command.historyLength() != null) {
      builder.historyLength(command.historyLength());
      configured = true;
    }
    if (command.callbackUrl() != null) {
      builder.taskPushNotificationConfig(toPushNotificationConfig(command));
      configured = true;
    }
    if (command.returnImmediately()) {
      builder.returnImmediately(true);
      configured = true;
    }
    return configured ? builder.build() : null;
  }

  /**
   * The task id is left unset on purpose: the agent has not created the task yet, and binds this configuration to
   * the task it creates for this message.
   */
  private static TaskPushNotificationConfig toPushNotificationConfig(SendCommand command) {
    TaskPushNotificationConfig.Builder builder = TaskPushNotificationConfig.builder().url(command.callbackUrl());
    if (command.callbackToken() != null) {
      builder.token(command.callbackToken());
    }
    if (command.callbackAuthScheme() != null) {
      builder.authentication(new AuthenticationInfo(command.callbackAuthScheme(), command.callbackAuthCredentials()));
    }
    if (command.tenant() != null) {
      builder.tenant(command.tenant());
    }
    return builder.build();
  }

  private TaskSnapshot toSnapshot(ClientEvent event) {
    if (event instanceof TaskEvent taskEvent) {
      return fromTask(taskEvent.getTask());
    }
    if (event instanceof TaskUpdateEvent updateEvent) {
      return fromTask(updateEvent.getTask());
    }
    if (event instanceof MessageEvent messageEvent) {
      return fromMessage(messageEvent.getMessage());
    }
    throw new A2aCallException("The A2A agent at '%s' returned an unexpected event of type '%s'"
        .formatted(config.url(), event.getClass().getName()), null, false);
  }

  /** An agent may answer a simple question directly, without creating a task at all. */
  private TaskSnapshot fromMessage(Message message) {
    AtomicBoolean truncated = new AtomicBoolean();
    Map<String, Object> statusMessage = toMap(message, truncated);
    return new TaskSnapshot(message.taskId(), message.contextId(), null, statusMessage,
        List.of(), Map.of(), List.of(), truncated.get());
  }

  private TaskSnapshot fromTask(Task task) {
    AtomicBoolean truncated = new AtomicBoolean();
    TaskStatus status = task.status();
    String state = status == null || status.state() == null ? null : status.state().name();
    Map<String, Object> statusMessage = status == null || status.message() == null
        ? null
        : toMap(status.message(), truncated);

    List<Map<String, Object>> artifacts = new ArrayList<>();
    if (task.artifacts() != null) {
      for (Artifact artifact : task.artifacts()) {
        artifacts.add(toMap(artifact, truncated));
      }
    }
    List<Map<String, Object>> history = new ArrayList<>();
    if (task.history() != null) {
      for (Message message : task.history()) {
        history.add(toMap(message, truncated));
      }
    }
    return new TaskSnapshot(task.id(), task.contextId(), state, statusMessage, artifacts,
        normalizeMap(task.metadata()), history, truncated.get());
  }

  private Map<String, Object> toMap(Message message, AtomicBoolean truncated) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("messageId", message.messageId());
    map.put("role", message.role() == null ? null : message.role().name());
    map.put("contextId", message.contextId());
    map.put("taskId", message.taskId());
    map.put(KEY_PARTS, toMaps(message.parts(), truncated));
    map.put(KEY_METADATA, normalizeMap(message.metadata()));
    map.put("extensions", message.extensions() == null ? List.of() : List.copyOf(message.extensions()));
    return map;
  }

  private Map<String, Object> toMap(Artifact artifact, AtomicBoolean truncated) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("artifactId", artifact.artifactId());
    map.put(KEY_NAME, artifact.name());
    map.put("description", artifact.description());
    map.put(KEY_PARTS, toMaps(artifact.parts(), truncated));
    map.put(KEY_METADATA, normalizeMap(artifact.metadata()));
    map.put("extensions", artifact.extensions() == null ? List.of() : List.copyOf(artifact.extensions()));
    return map;
  }

  private List<Map<String, Object>> toMaps(List<Part<?>> parts, AtomicBoolean truncated) {
    List<Map<String, Object>> result = new ArrayList<>();
    if (parts == null) {
      return result;
    }
    for (Part<?> part : parts) {
      result.add(toMap(part, truncated));
    }
    return result;
  }

  private Map<String, Object> toMap(Part<?> part, AtomicBoolean truncated) {
    Map<String, Object> map = new LinkedHashMap<>();
    if (part instanceof TextPart textPart) {
      map.put(KEY_TYPE, KEY_TEXT);
      map.put(KEY_TEXT, boundedText(textPart.text(), truncated));
      map.put(KEY_METADATA, normalizeMap(textPart.metadata()));
      return map;
    }
    if (part instanceof DataPart dataPart) {
      map.put(KEY_TYPE, KEY_DATA);
      map.put(KEY_DATA, normalize(dataPart.data()));
      map.put(KEY_METADATA, normalizeMap(dataPart.metadata()));
      return map;
    }
    if (part instanceof FilePart filePart) {
      map.put(KEY_TYPE, KEY_FILE);
      putFileContent(map, filePart.file(), truncated);
      map.put(KEY_METADATA, normalizeMap(filePart.metadata()));
      return map;
    }
    map.put(KEY_TYPE, "unknown");
    return map;
  }

  /**
   * Writes a returned file into the output map. A file that arrives as a URI stays a URI, which costs nothing.
   * A file that arrives inline is only passed on while it is small enough to belong in a process variable;
   * beyond that only its description travels, so that a multi-megabyte payload cannot end up in the history
   * tables.
   */
  private void putFileContent(Map<String, Object> map, FileContent content, AtomicBoolean truncated) {
    if (content == null) {
      return;
    }
    map.put(KEY_MIME_TYPE, content.mimeType());
    map.put(KEY_NAME, content.name());
    if (content instanceof FileWithUri fileWithUri) {
      map.put(KEY_URI, fileWithUri.uri());
      return;
    }
    if (content instanceof FileWithBytes fileWithBytes) {
      String bytes = fileWithBytes.bytes();
      if (bytes != null && bytes.length() > config.maxVariableSize()) {
        LOG.valueTruncated("inline file part", bytes.length(), config.maxVariableSize());
        truncated.set(true);
        map.put("sizeBytes", bytes.length());
        map.put("truncated", Boolean.TRUE);
      } else {
        map.put(KEY_BYTES, bytes);
      }
    }
  }

  private String boundedText(String text, AtomicBoolean truncated) {
    if (text == null || text.length() <= config.maxVariableSize()) {
      return text;
    }
    LOG.valueTruncated("text part", text.length(), config.maxVariableSize());
    truncated.set(true);
    return text.substring(0, config.maxVariableSize());
  }

  private static String string(Map<String, Object> map, String key) {
    Object value = map.get(key);
    return value == null ? null : value.toString();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> mapOrNull(Object value) {
    if (!(value instanceof Map<?, ?> map) || map.isEmpty()) {
      return null;
    }
    return (Map<String, Object>) value;
  }

  private static Map<String, Object> normalizeMap(Map<String, Object> value) {
    if (value == null || value.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> result = new LinkedHashMap<>();
    value.forEach((key, item) -> result.put(key, normalize(item)));
    return result;
  }

  /**
   * Turns whatever the SDK hands back into plain collections that can be stored as a process variable.
   *
   * <p>
   * Data parts and metadata are free-form JSON, and depending on how the SDK deserialised them they can arrive
   * as Gson tree nodes rather than as maps. Round-tripping the unknown cases through Gson is the cheapest way
   * to get something a process variable can hold.
   * </p>
   */
  private static Object normalize(Object value) {
    if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
      return value;
    }
    if (value instanceof Map<?, ?> map) {
      Map<String, Object> result = new LinkedHashMap<>();
      map.forEach((key, item) -> result.put(String.valueOf(key), normalize(item)));
      return result;
    }
    if (value instanceof List<?> list) {
      List<Object> result = new ArrayList<>();
      for (Object item : list) {
        result.add(normalize(item));
      }
      return result;
    }
    return normalize(GSON.fromJson(GSON.toJson(value), Object.class));
  }

  /**
   * Whether a failure is positively evidence of a transport problem, rather than merely unrecognised.
   *
   * <p>
   * {@link #classify} deliberately treats an unknown failure as retryable, which is the right default when a call
   * we need has failed. For the optional reattach probe the safe default is the opposite one, so this asks for
   * proof instead of assuming.
   * </p>
   */
  // package-private so the reattach-probe decision can be unit tested directly
  static boolean isTransportFailure(Throwable cause) {
    A2AClientHTTPError httpError = findCause(cause, A2AClientHTTPError.class);
    if (httpError != null) {
      int code = httpError.getCode();
      return code == 408 || code == 425 || code == 429 || code >= 500;
    }
    return findCause(cause, IOException.class) != null;
  }

  private A2aCallException classify(Throwable cause, String message) {
    A2AClientHTTPError httpError = findCause(cause, A2AClientHTTPError.class);
    if (httpError != null) {
      int code = httpError.getCode();
      boolean retryable = code == 408 || code == 425 || code == 429 || code >= 500;
      return new A2aCallException("%s: HTTP %d".formatted(message, code), cause, retryable);
    }
    if (findCause(cause, A2AProtocolError.class) != null || findCause(cause, A2AClientJSONError.class) != null) {
      return new A2aCallException("%s: %s".formatted(message, cause.getMessage()), cause, false);
    }
    // Connection resets, DNS failures and read timeouts all end up here. Retrying is the right default, and a
    // permanent problem still surfaces once the job runs out of retries.
    return new A2aCallException("%s: %s".formatted(message, cause.getMessage()), cause, true);
  }

  private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
    Throwable current = throwable;
    while (current != null) {
      if (type.isInstance(current)) {
        return type.cast(current);
      }
      current = current.getCause() == current ? null : current.getCause();
    }
    return null;
  }

}
