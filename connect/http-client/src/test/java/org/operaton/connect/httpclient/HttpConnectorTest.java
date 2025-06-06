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
package org.operaton.connect.httpclient;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.commons.utils.IoUtil;
import org.operaton.connect.ConnectorRequestException;
import org.operaton.connect.Connectors;
import org.operaton.connect.httpclient.impl.HttpConnectorImpl;
import org.operaton.connect.impl.DebugRequestInterceptor;
import org.operaton.connect.spi.Connector;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HttpConnectorTest {

  public static final String EXAMPLE_URL = "http://operaton.org/example";
  public static final String EXAMPLE_CONTENT_TYPE = "application/json";
  public static final String EXAMPLE_PAYLOAD = "operaton";

  protected HttpConnector connector;
  protected DebugRequestInterceptor interceptor;

  @BeforeEach
  void createConnector() {
    connector = new HttpConnectorImpl();
    interceptor = new DebugRequestInterceptor(false);
    connector.addRequestInterceptor(interceptor);
  }

  @Test
  void shouldDiscoverConnector() {
    Connector http = Connectors.getConnector(HttpConnector.ID);
    assertThat(http).isNotNull();
  }

  @Test
  void shouldFailWithoutMethod() {
    HttpRequest request = connector.createRequest().url("localhost");
    assertThatThrownBy(request::execute)
      .isInstanceOf(ConnectorRequestException.class);
  }

  @Test
  void shouldFailWithoutUrl() {
    HttpRequest request = connector.createRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(ConnectorRequestException.class);
  }

  @Test
  void shouldCreateHttpGetRequest() {
    connector.createRequest().url(EXAMPLE_URL).get().execute();
    verifyHttpRequest(HttpGet.class);
  }

  @Test
  void shouldCreateHttpPostRequest() {
    connector.createRequest().url(EXAMPLE_URL).post().execute();
    verifyHttpRequest(HttpPost.class);
  }

  @Test
  void shouldCreateHttpPutRequest() {
    connector.createRequest().url(EXAMPLE_URL).put().execute();
    verifyHttpRequest(HttpPut.class);
  }

  @Test
  void shouldCreateHttpDeleteRequest() {
    connector.createRequest().url(EXAMPLE_URL).delete().execute();
    verifyHttpRequest(HttpDelete.class);
  }

  @Test
  void shouldCreateHttpPatchRequest() {
    connector.createRequest().url(EXAMPLE_URL).patch().execute();
    verifyHttpRequest(HttpPatch.class);
  }

  @Test
  void shouldCreateHttpHeadRequest() {
    connector.createRequest().url(EXAMPLE_URL).head().execute();
    verifyHttpRequest(HttpHead.class);
  }

  @Test
  void shouldCreateHttpOptionsRequest() {
    connector.createRequest().url(EXAMPLE_URL).options().execute();
    verifyHttpRequest(HttpOptions.class);
  }

  @Test
  void shouldCreateHttpTraceRequest() {
    connector.createRequest().url(EXAMPLE_URL).trace().execute();
    verifyHttpRequest(HttpTrace.class);
  }

  @Test
  void shouldSetUrlOnHttpRequest() {
    connector.createRequest().url(EXAMPLE_URL).get().execute();
    HttpGet request = interceptor.getTarget();
    assertThat(request.getURI().toASCIIString()).isEqualTo(EXAMPLE_URL);
  }

  @Test
  void shouldSetContentTypeOnHttpRequest() {
    connector.createRequest().url(EXAMPLE_URL).contentType(EXAMPLE_CONTENT_TYPE).get().execute();
    HttpGet request = interceptor.getTarget();
    Header[] headers = request.getHeaders(HttpBaseRequest.HEADER_CONTENT_TYPE);
    assertThat(headers).hasSize(1);
    assertThat(headers[0].getName()).isEqualTo(HttpBaseRequest.HEADER_CONTENT_TYPE);
    assertThat(headers[0].getValue()).isEqualTo(EXAMPLE_CONTENT_TYPE);
  }

  @Test
  void shouldSetHeadersOnHttpRequest() {
    connector.createRequest().url(EXAMPLE_URL).header("foo", "bar").header("hello", "world").get().execute();
    HttpGet request = interceptor.getTarget();
    Header[] headers = request.getAllHeaders();
    assertThat(headers).hasSize(2);
  }

  @Test
  void shouldSetPayloadOnHttpRequest() throws IOException {
    connector.createRequest().url(EXAMPLE_URL).payload(EXAMPLE_PAYLOAD).post().execute();
    HttpPost request = interceptor.getTarget();
    String content = IoUtil.inputStreamAsString(request.getEntity().getContent());
    assertThat(content).isEqualTo(EXAMPLE_PAYLOAD);
  }

  @Test
  void shouldSetContentLength() {
    connector.createRequest().url(EXAMPLE_URL).payload(EXAMPLE_PAYLOAD).post().execute();
    HttpPost request = interceptor.getTarget();
    long contentLength = request.getEntity().getContentLength();

    assertThat(contentLength).isEqualTo(EXAMPLE_PAYLOAD.length());
  }

  protected void verifyHttpRequest(Class<? extends HttpRequestBase> requestClass) {
    Object target = interceptor.getTarget();
    assertThat(target).isInstanceOf(requestClass);

    HttpRequest request = interceptor.getRequest();
    HttpRequestBase requestBase = (HttpRequestBase) target;
    assertThat(requestBase.getMethod()).isEqualTo(request.getMethod());
  }

}
