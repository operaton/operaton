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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.RequestConfig.Builder;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;

import org.operaton.connect.httpclient.HttpBaseRequest;
import org.operaton.connect.httpclient.HttpResponse;
import org.operaton.connect.httpclient.impl.util.ParseUtil;
import org.operaton.connect.impl.AbstractConnector;

public abstract class AbstractHttpConnector<Q extends HttpBaseRequest<Q, R>, R extends HttpResponse> extends AbstractConnector<Q, R> {

  protected static final HttpConnectorLogger LOG = HttpLogger.HTTP_LOGGER;

  protected CloseableHttpClient httpClient;
  protected final Charset charset;

  protected AbstractHttpConnector(String connectorId) {
    super(connectorId);
    httpClient = createClient();
    charset = StandardCharsets.UTF_8;
  }

  protected CloseableHttpClient createClient() {
    return HttpClients.createSystem();
  }

  public CloseableHttpClient getHttpClient() {
    return httpClient;
  }

  public void setHttpClient(CloseableHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public R execute(Q request) {
    R invocationResult;
    HttpUriRequestBase httpRequest = createHttpRequest(request);
    HttpRequestInvocation invocation = new HttpRequestInvocation(httpRequest, request, requestInterceptors, httpClient);
    try {
      invocationResult = createResponse((CloseableHttpResponse) invocation.proceed());
    } catch (Exception e) {
      throw LOG.unableToExecuteRequest(e);
    }
    handleErrorResponse(request, invocationResult);
    return invocationResult;
  }

  protected void handleErrorResponse(Q request, R invocationResult) {
    Map<String, Object> configOptions = request.getConfigOptions();
    if (configOptions != null && invocationResult != null) {
      int statusCode = invocationResult.getStatusCode();
      String connectorResponse = invocationResult.getResponse();
      Object handleHttpError = Optional.ofNullable(configOptions.get("throw-http-error")).orElse("FALSE");
      if (Boolean.parseBoolean(handleHttpError.toString()) && statusCode >= 400 && statusCode <= 599) {
        throw LOG.httpRequestError(statusCode, connectorResponse);
      }
    }
  }

  protected abstract R createResponse(CloseableHttpResponse response);

  /**
   * creates a apache Http* representation of the request.
   *
   * @param request the given request
   * @return {@link HttpUriRequestBase} an apache representation of the request
   */
  protected <T extends HttpUriRequestBase> T createHttpRequest(Q request) {
    T httpRequest = createHttpRequestBase(request);

    applyConfig(httpRequest, request.getConfigOptions());

    applyHeaders(httpRequest, request.getHeaders());

    applyPayload(httpRequest, request);

    return httpRequest;
  }

  @SuppressWarnings("unchecked")
  protected <T extends HttpUriRequestBase> T createHttpRequestBase(Q request) {
    String url = request.getUrl();
    if (url != null && !url.trim().isEmpty()) {
      String method = request.getMethod();
      if (HttpGet.METHOD_NAME.equals(method)) {
        return (T) new HttpGet(url);
      } else if (HttpPost.METHOD_NAME.equals(method)) {
        return (T) new HttpPost(url);
      } else if (HttpPut.METHOD_NAME.equals(method)) {
        return (T) new HttpPut(url);
      } else if (HttpDelete.METHOD_NAME.equals(method)) {
        return (T) new HttpDelete(url);
      } else if (HttpPatch.METHOD_NAME.equals(method)) {
        return (T) new HttpPatch(url);
      } else if (HttpHead.METHOD_NAME.equals(method)) {
        return (T) new HttpHead(url);
      } else if (HttpOptions.METHOD_NAME.equals(method)) {
        return (T) new HttpOptions(url);
      } else if (HttpTrace.METHOD_NAME.equals(method)) {
        return (T) new HttpTrace(url);
      } else {
        throw LOG.unknownHttpMethod(method);
      }
    }
    else {
      throw LOG.requestUrlRequired();
    }
  }

  protected <T extends HttpUriRequestBase> void applyHeaders(T httpRequest, Map<String, String> headers) {
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        httpRequest.setHeader(entry.getKey(), entry.getValue());
        LOG.setHeader(entry.getKey(), entry.getValue());
      }
    }
  }

  protected <T extends HttpUriRequestBase> void applyPayload(T httpRequest, Q request) {
    if (httpMethodSupportsPayload(httpRequest)) {
      if (request.getPayload() != null) {
        byte[] bytes = request.getPayload().getBytes(charset);
        ByteArrayInputStream payload = new ByteArrayInputStream(bytes);
        InputStreamEntity entity = new InputStreamEntity(payload, bytes.length);
        ((HttpEntityEnclosingRequestBase) httpRequest).setEntity(entity);
      }
    }
    else if (request.getPayload() != null) {
      LOG.payloadIgnoredForHttpMethod(request.getMethod());
    }
  }

  protected <T extends HttpUriRequestBase> boolean httpMethodSupportsPayload(T httpRequest) {
    return httpRequest instanceof HttpEntityEnclosingRequestBase;
  }

  protected <T extends HttpUriRequestBase> void applyConfig(T httpRequest, Map<String, Object> configOptions) {
    Builder configBuilder = RequestConfig.custom();
    if (configOptions != null && !configOptions.isEmpty()) {
      ParseUtil.parseConfigOptions(configOptions, configBuilder);
    }
    RequestConfig requestConfig = configBuilder.build();
    httpRequest.setConfig(requestConfig);
  }


}
