/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.connect.httpclient;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.operaton.connect.httpclient.impl.HttpConnectorImpl;
import org.operaton.connect.impl.DebugRequestInterceptor;

class HttpResponseTest {

  protected HttpConnector connector;
  protected TestResponse testResponse;

  @BeforeEach
  void getConnector() {
    testResponse = new TestResponse();
    connector = new HttpConnectorImpl();
    connector.addRequestInterceptor(new DebugRequestInterceptor(testResponse));
  }

  @Test
  void responseCode() {
    testResponse.statusCode(123);
    HttpResponse response = getResponse();
    assertThat(response.getStatusCode()).isEqualTo(123);
  }

  @Test
  void responseBody() {
    testResponse.payload("test");
    HttpResponse response = getResponse();
    assertThat(response.getResponse()).isEqualTo("test");
  }

  @Test
  void emptyResponseBody() {
    testResponse.payload("");
    HttpResponse response = getResponse();
    assertThat(response.getResponse()).isEmpty();
  }

  @Test
  void nullResponseBody() {
    HttpResponse response = getResponse();
    assertThat(response.getResponse()).isNull();
  }

  @Test
  void emptyHeaders() {
    HttpResponse response = getResponse();
    assertThat(response.getHeaders()).isEmpty();
  }

  @Test
  void headers() {
    testResponse
      .header("foo", "bar")
      .header("hello", "world");
    HttpResponse response = getResponse();
    assertThat(response.getHeaders())
      .hasSize(2)
      .containsEntry("foo", "bar")
      .containsEntry("hello", "world")
      .doesNotContainKey("unknown");

    assertThat(response.getHeader("foo")).isEqualTo("bar");
    assertThat(response.getHeader("hello")).isEqualTo("world");
    assertThat(response.getHeader("unknown")).isNull();
  }

  protected HttpResponse getResponse() {
    return connector.createRequest().url("http://operaton.com").get().execute();
  }

}
