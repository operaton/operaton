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
package org.operaton.connect.impl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.operaton.connect.spi.Connector;
import org.operaton.connect.spi.ConnectorRequest;
import org.operaton.connect.spi.ConnectorRequestInterceptor;
import org.operaton.connect.spi.ConnectorResponse;

/**
 * Abstract implementation of the connector interface.
 *
 * <p>
 * This implementation provides a linked list of interceptors and related methods for
 * handling interceptor invocation.
 * </p>
 *
 * @author Daniel Meyer
 *
 */
public abstract class AbstractConnector<Q extends ConnectorRequest<R>, R extends ConnectorResponse> implements Connector<Q> {

  protected String connectorId;

  /**
   * The {@link ConnectorRequestInterceptor} chain
   */
  protected List<ConnectorRequestInterceptor> requestInterceptors = new LinkedList<>();

  protected AbstractConnector(String connectorId) {
    this.connectorId = connectorId;
  }

  @Override
  public String getId() {
    return connectorId;
  }

  @Override
  public List<ConnectorRequestInterceptor> getRequestInterceptors() {
    return requestInterceptors;
  }

  @Override
  public void setRequestInterceptors(List<ConnectorRequestInterceptor> requestInterceptors) {
    this.requestInterceptors = requestInterceptors;
  }

  @Override
  public Connector<Q> addRequestInterceptor(ConnectorRequestInterceptor interceptor) {
    requestInterceptors.add(interceptor);
    return this;
  }

  @Override
  public Connector<Q> addRequestInterceptors(Collection<ConnectorRequestInterceptor> interceptors) {
    requestInterceptors.addAll(interceptors);
    return this;
  }

}
