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

import java.util.Map;
import java.util.stream.Stream;

import com.github.tomakehurst.wiremock.http.FixedDelayDistribution;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.connect.httpclient.impl.ConnectionConfigOption;
import org.operaton.connect.httpclient.impl.HttpConnectorImpl;
import org.operaton.connect.httpclient.impl.util.ParseUtil;

import static org.operaton.connect.httpclient.impl.RequestConfigOption.CONNECTION_REQUEST_TIMEOUT;
import static org.operaton.connect.httpclient.impl.RequestConfigOption.CONNECTION_TIMEOUT;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.wiremock.webhooks.Webhooks.webhook;

@WireMockTest
public class HttpConnectionConfigTest {

    public static final String EXAMPLE_URL = "https://operaton.org";

    static Stream<Arguments> timeout_args() {
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
    @Disabled("""
        FIXME: Getting not the expected exception, needs investigation.
        The test fails with:
           HTCL-02004 Unable to read connectorResponse: Attempted read on closed stream
        when setting *any* timeout value (even very high ones).
        The console log output shows that the requested timeout value is correctly set on the request.
           ep-0000000001 connecting endpoint (1 MILLISECONDS)
           ep-0000000001 connecting endpoint to http://localhost:58032 (1 MILLISECONDS)
    """)
    void shouldThrowTimeoutException(WireMockRuntimeInfo wmRuntimeInfo) {
        // given
        stubFor(get(urlEqualTo("/"))
            .willReturn(aResponse().withStatus(200)));

        // create request and set the connection timeout on the request itself
        var httpRequest = connector.createRequest()
            .url("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/")
            .configOption(CONNECTION_TIMEOUT.getName(), Timeout.ofMilliseconds(1))
            .get();

        // when, then
        assertThatThrownBy(httpRequest::execute)
            .hasMessageContaining("Unable to execute HTTP request")
            .hasCauseExactlyInstanceOf(ConnectTimeoutException.class);
    }

    @Test
    @Disabled("""
        FIXME: Getting not the expected exception, needs investigation.
        The test fails with:
           HTCL-02004 Unable to read connectorResponse: Attempted read on closed stream
        when setting *any* timeout value (even very high ones).
        The console log output shows that the requested timeout value is correctly set on the request.
            ex-0000000001 acquiring endpoint (1 MILLISECONDS)
           ep-0000000001 connecting endpoint to http://localhost:58032 (1 MILLISECONDS)
        Furthermore the wiremock server does not seem to delay the response as configured.
    """)
    void shouldThrowTimeoutException_whenRequestExceedsConnectionRequestTimeout(WireMockRuntimeInfo wmRuntimeInfo) {
        // given
        stubFor(get(urlEqualTo("/slow"))
            .withServeEventListener("webhook", webhook().withDelay(new FixedDelayDistribution(100)))
            .willReturn(aResponse().withStatus(200)));

        // create request and set the connection timeout on the request itself
        var httpRequest = connector.createRequest()
            .url("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/slow")
            .configOption(CONNECTION_REQUEST_TIMEOUT.getName(), Timeout.ofMilliseconds(1))
            .get();

        // when, then
        assertThatThrownBy(httpRequest::execute)
            .hasMessageContaining("Unable to execute HTTP request")
            .hasCauseExactlyInstanceOf(ConnectTimeoutException.class);
    }

}
