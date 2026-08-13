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

import java.util.LinkedHashMap;
import java.util.Map;

import org.operaton.connect.a2a.A2aRequest;
import org.operaton.connect.a2a.A2aResponse;
import org.operaton.connect.impl.AbstractConnectorRequest;
import org.operaton.connect.spi.Connector;

/**
 * Holds the input parameters of an A2A call. Values arrive either through the fluent methods, when the
 * connector is used from Java, or through {@code setRequestParameter}, when the connect plugin applies the
 * {@code operaton:inputParameter} mappings of a service task.
 */
public class A2aRequestImpl extends AbstractConnectorRequest<A2aResponse> implements A2aRequest {

  @SuppressWarnings("rawtypes")
  public A2aRequestImpl(Connector connector) {
    super(connector);
  }

  @Override
  public A2aRequest operation(String operation) {
    setRequestParameter(PARAM_OPERATION, operation);
    return this;
  }

  @Override
  public A2aRequest url(String url) {
    setRequestParameter(PARAM_URL, url);
    return this;
  }

  @Override
  public A2aRequest header(String field, String value) {
    Map<String, String> headers = getRequestParameter(PARAM_HEADERS);
    if (headers == null) {
      headers = new LinkedHashMap<>();
      setRequestParameter(PARAM_HEADERS, headers);
    }
    headers.put(field, value);
    return this;
  }

  @Override
  public A2aRequest message(String message) {
    setRequestParameter(PARAM_MESSAGE, message);
    return this;
  }

  @Override
  public A2aRequest taskId(String taskId) {
    setRequestParameter(PARAM_TASK_ID, taskId);
    return this;
  }

  @Override
  public A2aRequest contextId(String contextId) {
    setRequestParameter(PARAM_CONTEXT_ID, contextId);
    return this;
  }

}
