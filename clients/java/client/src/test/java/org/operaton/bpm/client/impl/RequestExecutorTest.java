/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0; you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.impl.io.DefaultClassicHttpResponseFactory;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.client.exception.RestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class RequestExecutorTest {

  private final RequestExecutor requestExecutor = new RequestExecutor(null, new ObjectMapper());

  @Test
  void shouldCreateRestExceptionForErrorResponseWithoutBody() throws Exception {
    ClassicHttpResponse response = DefaultClassicHttpResponseFactory.INSTANCE.newHttpResponse(500);

    RestException exception = catchThrowableOfType(
        () -> requestExecutor.handleResponse(Void.class).handleResponse(response),
        RestException.class);

    assertThat(exception.getHttpStatusCode()).isEqualTo(500);
    assertThat(exception.getMessage()).isEqualTo("No body in response, unable to parse error message");
  }

  @Test
  void shouldCreateRestExceptionForErrorResponseWithEmptyBody() throws Exception {
    ClassicHttpResponse response = DefaultClassicHttpResponseFactory.INSTANCE.newHttpResponse(401);
    response.setEntity(new ByteArrayEntity(new byte[0], ContentType.APPLICATION_JSON));

    RestException exception = catchThrowableOfType(
        () -> requestExecutor.handleResponse(Void.class).handleResponse(response),
        RestException.class);

    assertThat(exception.getHttpStatusCode()).isEqualTo(401);
    assertThat(exception.getMessage()).isEqualTo("No body in response, unable to parse error message");
  }

}
