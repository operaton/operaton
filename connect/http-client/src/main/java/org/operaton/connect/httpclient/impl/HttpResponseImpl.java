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
package org.operaton.connect.httpclient.impl;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;

import org.operaton.commons.utils.IoUtil;
import org.operaton.connect.httpclient.HttpResponse;
import org.operaton.connect.impl.AbstractCloseableConnectorResponse;

public class HttpResponseImpl extends AbstractCloseableConnectorResponse implements HttpResponse {

  private static final HttpConnectorLogger LOG = HttpLogger.HTTP_LOGGER;

  protected ClassicHttpResponse httpResponse;

  public HttpResponseImpl(ClassicHttpResponse httpResponse) {
    this.httpResponse = httpResponse;
  }

  @Override
  public Integer getStatusCode() {
    return getResponseParameter(PARAM_NAME_STATUS_CODE);
  }

  @Override
  public String getResponse() {
    return getResponseParameter(PARAM_NAME_RESPONSE);
  }

  @Override
  public Map<String, String> getHeaders() {
    return getResponseParameter(PARAM_NAME_RESPONSE_HEADERS);
  }

  @Override
  public String getHeader(String field) {
    Map<String, String> headers = getHeaders();
    if (headers != null) {
      return headers.get(field);
    }
    else {
      return null;
    }
  }

  protected void collectResponseParameters(Map<String, Object> responseParameters) {
    responseParameters.put(PARAM_NAME_STATUS_CODE, httpResponse.getCode());
    collectResponseHeaders();

    if (httpResponse.getEntity() != null) {
      try {
        String response = new String(httpResponse.getEntity().getContent().readAllBytes());
        responseParameters.put(PARAM_NAME_RESPONSE, response);
      } catch (IOException e) {
        throw LOG.unableToReadResponse(e);
      } finally {
        IoUtil.closeSilently(httpResponse);
      }
    }
  }

  protected void collectResponseHeaders() {
    Map<String, String> headers = new HashMap<>();
    for (Header header : httpResponse.getHeaders()) {
      headers.put(header.getName(), header.getValue());
    }
    responseParameters.put(PARAM_NAME_RESPONSE_HEADERS, headers);
  }

  protected Closeable getClosable() {
    return httpResponse;
  }

}
