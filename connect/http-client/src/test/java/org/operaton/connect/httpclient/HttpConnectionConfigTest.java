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

import java.net.NoRouteToHostException;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.config.ConnectionConfig;
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
import org.operaton.connect.httpclient.impl.ConnectionConfigOption;
import org.operaton.connect.httpclient.impl.HttpConnectorImpl;
import org.operaton.connect.httpclient.impl.util.ParseUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.AUTHENTICATION_ENABLED;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.CIRCULAR_REDIRECTS_ALLOWED;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.CONNECTION_KEEP_ALIVE;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.CONNECTION_REQUEST_TIMEOUT;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.CONNECTION_TIMEOUT;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.CONTENT_COMPRESSION_ENABLED;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.COOKIE_SPEC;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.EXPECT_CONTINUE_ENABLED;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.HARD_CANCELLATION_ENABLED;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.MAX_REDIRECTS;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.PROXY;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.PROXY_PREFERRED_AUTH_SCHEMES;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.REDIRECTS_ENABLED;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.RESPONSE_TIMEOUT;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.TARGET_PREFERRED_AUTH_SCHEMES;

public class HttpConnectionConfigTest {

  public static final String EXAMPLE_URL = "https://operaton.org";

  static Stream<Arguments> timeout_args () {
    return Stream.of(
        arguments(Timeout.ofSeconds(10), 10000),
        arguments(Timeout.ofMinutes(1), 60000),
        arguments(Timeout.ofMilliseconds(500), 500)
    );
  }

  protected HttpConnectorImpl connector;

  @BeforeEach
  void createConnector() {
    connector = new HttpConnectorImpl();
  }

  @ParameterizedTest
  @MethodSource("timeout_args")
  void shouldParseConnectionTimeout(Object timeoutValue, int expectedTimeout) {
    // given
    connector.configOption(ConnectionConfigOption.CONNECTION_TIMEOUT.getName(), timeoutValue);
    HttpRequest request = connector.createRequest()
      .configOption(CONNECTION_TIMEOUT.getName(), timeoutValue);
    Map<String, Object> configOptions = request.getConfigOptions();

    ConnectionConfig.Builder configBuilder = ConnectionConfig.custom();
    ParseUtil.parseConfigOptions(configOptions, configBuilder);

    // when
    ConnectionConfig config = configBuilder.build();

    // then
    assertThat(config.getConnectTimeout().toMilliseconds()).isEqualTo(expectedTimeout);
  }

  @Test
  void shouldThrowTimeoutException() {
    // given
    var httpRequest = connector.createRequest()
      .url(EXAMPLE_URL)
      .get();

    // lowest unit possible to trigger timeout
    connector.configOption(ConnectionConfigOption.CONNECTION_TIMEOUT.getName(), Timeout.ofMilliseconds(1));

    // when, then
    assertThatThrownBy(httpRequest::execute)
      .isInstanceOf(ConnectorRequestException.class)
      .hasMessageContaining("Unable to execute HTTP request")
      // see DefaultHttpClientConnectionOperator.connect: Socket#bind might throw NoRouteToHostException on timeout
      .hasCauseExactlyInstanceOf(NoRouteToHostException.class);
  }

}
