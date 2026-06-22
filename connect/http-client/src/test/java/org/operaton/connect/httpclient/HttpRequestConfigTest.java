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

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.RequestConfig.Builder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.connect.ConnectorRequestException;
import org.operaton.connect.httpclient.impl.HttpConnectorImpl;
import org.operaton.connect.httpclient.impl.util.ParseUtil;

import static org.operaton.connect.httpclient.impl.RequestConfigOption.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class HttpRequestConfigTest {

  public static final String EXAMPLE_URL = "https://operaton.org";

  static Stream<Arguments> timeout_args () {
    return Stream.of(
        arguments(Timeout.ofSeconds(10), 10000),
        arguments(Timeout.ofMinutes(1), 60000),
        arguments(Timeout.ofMilliseconds(500), 500)
    );
  }

  protected HttpConnector connector;

  @BeforeEach
  void createConnector() {
    connector = new HttpConnectorImpl();
  }

  @Test
  void shouldParseAuthenticationEnabled() {
    // given
    HttpRequest request = connector.createRequest()
        .configOption(AUTHENTICATION_ENABLED.getName(), false);
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.isAuthenticationEnabled()).isFalse();
  }

  @Test
  void shouldParseCircularRedirectsAllowed() {
    // given
    HttpRequest request = connector.createRequest()
        .configOption(CIRCULAR_REDIRECTS_ALLOWED.getName(), true);
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.isCircularRedirectsAllowed()).isTrue();
  }

  @ParameterizedTest
  @MethodSource("timeout_args")
  void shouldParseConnectionTimeout(Object timeoutValue, int expectedTimeout) {
    // given
    HttpRequest request = connector.createRequest()
        .configOption(CONNECTION_TIMEOUT.getName(), timeoutValue);
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.getConnectTimeout().toMilliseconds()).isEqualTo(expectedTimeout);
  }

  @ParameterizedTest
  @MethodSource("timeout_args")
  void shouldParseConnectionRequestTimeout(Object timeoutValue, int expectedTimeout) {
    // given
    HttpRequest request = connector.createRequest()
        .configOption(CONNECTION_REQUEST_TIMEOUT.getName(), timeoutValue);
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.getConnectionRequestTimeout().toMilliseconds()).isEqualTo(expectedTimeout);
  }

  @Test
  void shouldParseContentCompressionEnabled() {
    // given
    HttpRequest request = connector.createRequest()
        .configOption(CONTENT_COMPRESSION_ENABLED.getName(), false);
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.isContentCompressionEnabled()).isFalse();
  }

  @Test
  void shouldParseCookieSpec() {
    // given
    HttpRequest request = connector.createRequest()
        .configOption(COOKIE_SPEC.getName(), "test");
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.getCookieSpec()).isEqualTo("test");
  }

  @Test
  void shouldParseMaxRedirects() {
    // given
    HttpRequest request = connector.createRequest()
            .configOption(MAX_REDIRECTS.getName(), -2);
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.getMaxRedirects()).isEqualTo(-2);
  }

  @Test
  void shouldParseProxy() {
    // given
    HttpHost testHost = new HttpHost("test");
    HttpRequest request = connector.createRequest()
            .configOption(PROXY.getName(), testHost);
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.getProxy()).isEqualTo(testHost);
  }

  @Test
  void shouldParseProxyPreferredAuthSchemes() {
    // given
    ArrayList<String> testArray = new ArrayList<>();
    HttpRequest request = connector.createRequest()
            .configOption(PROXY_PREFERRED_AUTH_SCHEMES.getName(), testArray);
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.getProxyPreferredAuthSchemes()).isEqualTo(testArray);
  }

  @Test
  void shouldParseRedirectsEnabled() {
    // given
    HttpRequest request = connector.createRequest()
            .configOption(REDIRECTS_ENABLED.getName(), false);
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.isRedirectsEnabled()).isFalse();
  }

  @Test
  void shouldParseTargetPreferredAuthSchemes() {
    // given
    ArrayList<String> testArray = new ArrayList<>();
    HttpRequest request = connector.createRequest()
            .configOption(TARGET_PREFERRED_AUTH_SCHEMES.getName(), testArray);
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.getTargetPreferredAuthSchemes()).isEqualTo(testArray);
  }

  @Test
  void shouldParseConnectionKeepAlive() {
    // given
    HttpRequest request = connector.createRequest()
            .configOption(CONNECTION_KEEP_ALIVE.getName(), Timeout.ofSeconds(10));
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.getConnectionKeepAlive()).isEqualTo(Timeout.ofSeconds(10));
  }

  @Test
  void shouldParseExpectContinueEnabled() {
    // given
    HttpRequest request = connector.createRequest()
            .configOption(EXPECT_CONTINUE_ENABLED.getName(), true);
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.isExpectContinueEnabled()).isTrue();
  }

  @Test
  void shouldParseHardCancellationEnabled() {
    // given
    HttpRequest request = connector.createRequest()
            .configOption(HARD_CANCELLATION_ENABLED.getName(), true);
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.isHardCancellationEnabled()).isTrue();
  }

  @Test
  void shouldParseResponseTimeout() {
    // given
    HttpRequest request = connector.createRequest()
            .configOption(RESPONSE_TIMEOUT.getName(), Timeout.ofSeconds(10));
    Map<String, Object> configOptions = request.getConfigOptions();

    Builder configBuilder = RequestConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    RequestConfig config = configBuilder.build();

    // then
    assertThat(config.getResponseTimeout()).isEqualTo(Timeout.ofSeconds(10));
  }

  @Test
  void shouldThrowClassCastExceptionStringToBoolean() {
    // given
    var httpRequest = connector.createRequest()
      .url(EXAMPLE_URL)
      .get()
      .configOption(AUTHENTICATION_ENABLED.getName(), "true");

    // when, then
    assertThatThrownBy(httpRequest::execute)
    .isInstanceOf(ConnectorRequestException.class)
      .hasMessageContaining("Invalid value for request configuration option: " + AUTHENTICATION_ENABLED.getName())
      .hasCauseInstanceOf(ClassCastException.class)
      .rootCause().hasMessageContaining("java.lang.String cannot be cast to class java.lang.Boolean");
  }

  @Test
  void shouldThrowClassCastExceptionIntToHttpHost() {
    // given
    var httpRequest = connector.createRequest()
      .url(EXAMPLE_URL)
      .get()
      .configOption(PROXY_PREFERRED_AUTH_SCHEMES.getName(), 0);

    // when, then
    assertThatThrownBy(httpRequest::execute)
      .isInstanceOf(ConnectorRequestException.class)
      .hasMessageContaining("Invalid value for request configuration option: " + PROXY_PREFERRED_AUTH_SCHEMES.getName())
      .hasCauseInstanceOf(ClassCastException.class)
      .rootCause().hasMessageContaining("java.lang.Integer cannot be cast to class java.util.Collection");
  }

}
