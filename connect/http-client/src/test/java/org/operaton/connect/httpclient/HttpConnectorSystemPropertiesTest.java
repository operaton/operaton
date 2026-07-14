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

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.operaton.connect.httpclient.impl.AbstractHttpConnector;
import org.operaton.connect.httpclient.impl.HttpConnectorImpl;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.apache.hc.core5.http.HttpHeaders.USER_AGENT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Since Apache HTTP client makes it extremely hard to test the proper configuration
 * of a http client, this is more of an integration test that checks that a
 * system property is respected
 *
 * @author Thorben Lindhauer
 */
@WireMockTest
class HttpConnectorSystemPropertiesTest {

  protected Set<String> updatedSystemProperties;

  @BeforeEach
  void setUp() {
    updatedSystemProperties = new HashSet<>();
    stubFor(get(urlEqualTo("/")).willReturn(aResponse().withStatus(200)));
  }

  @AfterEach
  void clearCustomSystemProperties() {
    for (String property : updatedSystemProperties) {
      System.getProperties().remove(property);
    }
  }

  public void setSystemProperty(String property, String value) {
    if (!System.getProperties().containsKey(property)) {
      updatedSystemProperties.add(property);
      System.setProperty(property, value);
    } else {
      throw new RuntimeException("Cannot perform test: System property "
          + property + " is already set. Will not attempt to overwrite this property.");
    }
  }

  @Test
  void shouldSetUserAgentFromSystemProperty(WireMockRuntimeInfo wmRuntimeInfo) {
    // given
    setSystemProperty("http.agent", "foo");

    HttpConnector customConnector = new HttpConnectorImpl();

    // when
    customConnector.createRequest().url("http://localhost:" + wmRuntimeInfo.getHttpPort()).get().execute();

    // then
    verify(getRequestedFor(urlEqualTo("/")).withHeader(USER_AGENT, equalTo("foo")));

  }

  @Test
  void shouldApplyPayloadUsingCharsetFromSystemProperty(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    // given
    setSystemProperty(AbstractHttpConnector.PROPERTY_CHARSET, "UTF-16");
    stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(200)));
    String payload = "café";

    // when
    HttpConnectorImpl connector = new HttpConnectorImpl();
    connector.createRequest()
        .url("http://localhost:" + wmRuntimeInfo.getHttpPort())
        .contentType("text/plain")
        .payload(payload)
        .post()
        .execute();

    // then
    assertThat(readRequestPayload(StandardCharsets.UTF_16)).isEqualTo(payload);
  }

  @Test
  void shouldApplyPayloadUsingUtf8ByDefault(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
    // given
    stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(200)));
    String payload = "café";

    // when
    HttpConnectorImpl connector = new HttpConnectorImpl();
    connector.createRequest()
        .url("http://localhost:" + wmRuntimeInfo.getHttpPort())
        .contentType("text/plain")
        .payload(payload)
        .post()
        .execute();

    // then
    assertThat(readRequestPayload(StandardCharsets.UTF_8)).isEqualTo(payload);
  }

  protected String readRequestPayload(Charset charset) throws CharacterCodingException {
    List<LoggedRequest> requests = findAll(postRequestedFor(urlEqualTo("/")));
    assertThat(requests).hasSize(1);
    CharsetDecoder decoder = charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT);
    return decoder.decode(ByteBuffer.wrap(requests.get(0).getBody())).toString();
  }
}
