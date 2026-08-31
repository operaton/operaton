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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.a2aproject.sdk.client.http.A2AHttpClient;
import org.a2aproject.sdk.client.http.A2AHttpResponse;
import org.a2aproject.sdk.client.http.ServerSentEvent;

/**
 * An {@link A2AHttpClient} that puts a read timeout on every request.
 *
 * <p>
 * The SDK's own {@code JdkA2AHttpClient} never calls {@code HttpRequest.Builder.timeout(...)}, and neither the
 * {@link A2AHttpClient} interface nor its builders expose a way to set one. Without this class an unresponsive
 * agent would hold a job executor thread until the OS gave up on the socket, which breaks the rule that
 * nothing in a process engine may block without a bound.
 * </p>
 */
public class TimeoutAwareA2AHttpClient implements A2AHttpClient {

  private final HttpClient httpClient;
  private final Duration readTimeout;

  public TimeoutAwareA2AHttpClient(int connectTimeoutMs, int readTimeoutMs) {
    this(HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(connectTimeoutMs))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build(), readTimeoutMs);
  }

  public TimeoutAwareA2AHttpClient(HttpClient httpClient, int readTimeoutMs) {
    this.httpClient = httpClient;
    this.readTimeout = Duration.ofMillis(readTimeoutMs);
  }

  @Override
  public GetBuilder createGet() {
    return new TimeoutAwareGetBuilder();
  }

  @Override
  public PostBuilder createPost() {
    return new TimeoutAwarePostBuilder();
  }

  @Override
  public DeleteBuilder createDelete() {
    return new TimeoutAwareDeleteBuilder();
  }

  private A2AHttpResponse send(HttpRequest request) throws IOException, InterruptedException {
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return new StringResponse(response.statusCode(), response.body());
  }

  /**
   * The connector never enables streaming, so no server-sent-event support is needed here. If streaming is ever
   * turned on, this is the one place that has to grow an implementation.
   */
  private static CompletableFuture<Void> streamingNotSupported() {
    // ponytail: SSE deliberately unimplemented because the connector polls instead of holding a stream open
    // on a job executor thread. Implement here if a streaming operation is ever added.
    throw new UnsupportedOperationException(
        "The A2A connector does not use server-sent events; it polls with the 'sendSync' operation instead");
  }

  private record StringResponse(int status, String body) implements A2AHttpResponse {

    @Override
    public boolean success() {
      return status >= 200 && status < 300;
    }
  }

  private abstract class TimeoutAwareBuilder<T extends Builder<T>> implements Builder<T> {

    protected String url;
    protected final Map<String, String> headers = new LinkedHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public T url(String url) {
      this.url = url;
      return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T addHeaders(Map<String, String> additionalHeaders) {
      if (additionalHeaders != null) {
        headers.putAll(additionalHeaders);
      }
      return (T) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T addHeader(String name, String value) {
      if (name != null && value != null) {
        headers.put(name, value);
      }
      return (T) this;
    }

    protected HttpRequest.Builder request() {
      HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(readTimeout);
      headers.forEach(builder::header);
      return builder;
    }
  }

  private final class TimeoutAwareGetBuilder extends TimeoutAwareBuilder<GetBuilder> implements GetBuilder {

    @Override
    public A2AHttpResponse get() throws IOException, InterruptedException {
      return send(request().GET().build());
    }

    @Override
    public CompletableFuture<Void> getAsyncSSE(Consumer<ServerSentEvent> eventConsumer,
                                               Consumer<Throwable> errorConsumer,
                                               Runnable completeRunnable) {
      return streamingNotSupported();
    }
  }

  private final class TimeoutAwarePostBuilder extends TimeoutAwareBuilder<PostBuilder> implements PostBuilder {

    private String body = "";

    @Override
    public PostBuilder body(String body) {
      this.body = body;
      return this;
    }

    @Override
    public A2AHttpResponse post() throws IOException, InterruptedException {
      return send(request().POST(HttpRequest.BodyPublishers.ofString(body)).build());
    }

    @Override
    public CompletableFuture<Void> postAsyncSSE(Consumer<ServerSentEvent> eventConsumer,
                                                Consumer<Throwable> errorConsumer,
                                                Runnable completeRunnable) {
      return streamingNotSupported();
    }
  }

  private final class TimeoutAwareDeleteBuilder extends TimeoutAwareBuilder<DeleteBuilder> implements DeleteBuilder {

    @Override
    public A2AHttpResponse delete() throws IOException, InterruptedException {
      return send(request().DELETE().build());
    }
  }

}
