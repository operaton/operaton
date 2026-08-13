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
import java.util.Map;

import org.a2aproject.sdk.spec.A2AClientHTTPError;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The reattach probe calls {@code ListTasks}, which A2A makes optional. Agents that do not implement it answer
 * with JSON-RPC {@code -32601}, and that must never be allowed to fail the send it was only trying to optimise.
 *
 * <p>
 * This is a regression test for exactly that: an earlier version treated any unrecognised probe failure as
 * retryable, which turned a missing optional method into a permanently failing service task against every agent
 * without {@code ListTasks}, which is most of them.
 * </p>
 */
class SdkA2aAgentTest {

  @Test
  void aMissingOptionalMethodDoesNotCountAsATransportFailure() {
    // What an agent without ListTasks actually produces, once the SDK is done with it.
    assertThat(SdkA2aAgent.isTransportFailure(new RuntimeException("Method not found"))).isFalse();
    assertThat(SdkA2aAgent.isTransportFailure(new NullPointerException())).isFalse();
    assertThat(SdkA2aAgent.isTransportFailure(httpError(404))).isFalse();
    assertThat(SdkA2aAgent.isTransportFailure(httpError(400))).isFalse();
  }

  @Test
  void aRealTransportProblemDoesCount() {
    assertThat(SdkA2aAgent.isTransportFailure(new IOException("connection reset"))).isTrue();
    assertThat(SdkA2aAgent.isTransportFailure(httpError(500))).isTrue();
    assertThat(SdkA2aAgent.isTransportFailure(httpError(503))).isTrue();
    assertThat(SdkA2aAgent.isTransportFailure(httpError(429))).isTrue();
    assertThat(SdkA2aAgent.isTransportFailure(httpError(408))).isTrue();
  }

  @Test
  void theCauseChainIsSearchedNotJustTheTopException() {
    assertThat(SdkA2aAgent.isTransportFailure(new IllegalStateException(new IOException("reset")))).isTrue();
    assertThat(SdkA2aAgent.isTransportFailure(new IllegalStateException(httpError(503)))).isTrue();
    assertThat(SdkA2aAgent.isTransportFailure(new IllegalStateException(new RuntimeException("nope")))).isFalse();
  }

  @Test
  void aSelfReferencingCauseChainTerminates() {
    RuntimeException looping = new RuntimeException("loop") {
      @Override
      public synchronized Throwable getCause() {
        return this;
      }
    };

    assertThat(SdkA2aAgent.isTransportFailure(looping)).isFalse();
  }

  private static A2AClientHTTPError httpError(int code) {
    return new A2AClientHTTPError(code, "failed", "", Map.of());
  }

}
