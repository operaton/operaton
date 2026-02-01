/*
 * Copyright 2025 the Operaton contributors.
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
package org.operaton.connect.httpclient.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.operaton.connect.spi.ConnectorRequest;
import org.operaton.connect.spi.ConnectorRequestInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HttpRequestInvocationTest {

  private HttpClient httpClient;
  private HttpGet httpRequest;
  private ConnectorRequest<?> connectorRequest;
  private List<ConnectorRequestInterceptor> interceptorChain;
  private ClassicHttpResponse mockResponse;

  @BeforeEach
  void setUp() {
    httpClient = mock(HttpClient.class);
    httpRequest = new HttpGet("http://example.com");
    connectorRequest = mock(ConnectorRequest.class);
    interceptorChain = new ArrayList<>();
    mockResponse = mock(ClassicHttpResponse.class);
  }

  @Test
  void shouldExecuteHttpRequestWithResponseHandler() throws Exception {
    // given
    when(httpClient.execute(eq(httpRequest), any(HttpClientResponseHandler.class)))
        .thenReturn(mockResponse);

    HttpRequestInvocation invocation = new HttpRequestInvocation(
        httpRequest, connectorRequest, interceptorChain, httpClient);

    // when
    Object result = invocation.invokeTarget();

    // then
    assertThat(result).isEqualTo(mockResponse);
    verify(httpClient).execute(eq(httpRequest), any(HttpClientResponseHandler.class));
  }

  @Test
  void shouldReturnResponseFromResponseHandler() throws Exception {
    // given
    ArgumentCaptor<HttpClientResponseHandler<ClassicHttpResponse>> handlerCaptor =
        ArgumentCaptor.forClass(HttpClientResponseHandler.class);

    when(httpClient.execute(eq(httpRequest), handlerCaptor.capture()))
        .thenAnswer(invocation -> {
          HttpClientResponseHandler<ClassicHttpResponse> handler = handlerCaptor.getValue();
          return handler.handleResponse(mockResponse);
        });

    HttpRequestInvocation invocation = new HttpRequestInvocation(
        httpRequest, connectorRequest, interceptorChain, httpClient);

    // when
    Object result = invocation.invokeTarget();

    // then
    assertThat(result).isEqualTo(mockResponse);
    HttpClientResponseHandler<ClassicHttpResponse> capturedHandler = handlerCaptor.getValue();
    assertThat(capturedHandler.handleResponse(mockResponse)).isEqualTo(mockResponse);
  }

  @Test
  void shouldPropagateExceptionFromHttpClient() throws Exception {
    // given
    Exception expectedException = new RuntimeException("HTTP execution failed");
    when(httpClient.execute(eq(httpRequest), any(HttpClientResponseHandler.class)))
        .thenThrow(expectedException);

    HttpRequestInvocation invocation = new HttpRequestInvocation(
        httpRequest, connectorRequest, interceptorChain, httpClient);

    // when/then
    assertThatThrownBy(invocation::invokeTarget)
        .isEqualTo(expectedException);
  }

  @Test
  void shouldWorkWithInterceptorChain() throws Exception {
    // given
    when(httpClient.execute(eq(httpRequest), any(HttpClientResponseHandler.class)))
        .thenReturn(mockResponse);

    ConnectorRequestInterceptor interceptor = mock(ConnectorRequestInterceptor.class);
    when(interceptor.handleInvocation(any())).thenAnswer(invocation ->
        ((HttpRequestInvocation) invocation.getArgument(0)).proceed());

    interceptorChain.add(interceptor);

    HttpRequestInvocation invocation = new HttpRequestInvocation(
        httpRequest, connectorRequest, interceptorChain, httpClient);

    // when
    Object result = invocation.proceed();

    // then
    assertThat(result).isEqualTo(mockResponse);
    verify(interceptor).handleInvocation(invocation);
    verify(httpClient).execute(eq(httpRequest), any(HttpClientResponseHandler.class));
  }

  @Test
  void shouldProvideAccessToTarget() {
    // given
    HttpRequestInvocation invocation = new HttpRequestInvocation(
        httpRequest, connectorRequest, interceptorChain, httpClient);

    // when
    Object target = invocation.getTarget();

    // then
    assertThat(target).isEqualTo(httpRequest);
  }

  @Test
  void shouldProvideAccessToRequest() {
    // given
    HttpRequestInvocation invocation = new HttpRequestInvocation(
        httpRequest, connectorRequest, interceptorChain, httpClient);

    // when
    ConnectorRequest<?> request = invocation.getRequest();

    // then
    assertThat(request).isEqualTo(connectorRequest);
  }
}
