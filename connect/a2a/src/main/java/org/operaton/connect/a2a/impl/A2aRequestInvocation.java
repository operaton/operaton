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

import java.util.List;
import java.util.function.Supplier;

import org.operaton.connect.impl.AbstractRequestInvocation;
import org.operaton.connect.spi.ConnectorRequest;
import org.operaton.connect.spi.ConnectorRequestInterceptor;

/**
 * Routes one A2A operation through the connector's interceptor chain.
 *
 * <p>
 * The whole operation is a single invocation, including the polling an operation may do internally, so that an
 * interceptor sees one agent call per service task rather than one per HTTP request.
 * </p>
 */
public class A2aRequestInvocation extends AbstractRequestInvocation<A2aAgent> {

  private final Supplier<A2aAgent.TaskSnapshot> call;

  public A2aRequestInvocation(A2aAgent agent,
                              Supplier<A2aAgent.TaskSnapshot> call,
                              ConnectorRequest<?> request,
                              List<ConnectorRequestInterceptor> interceptorChain) {
    super(agent, request, interceptorChain);
    this.call = call;
  }

  @Override
  public Object invokeTarget() {
    return call.get();
  }

}
