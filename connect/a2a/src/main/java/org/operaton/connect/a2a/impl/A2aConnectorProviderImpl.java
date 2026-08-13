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

import org.operaton.connect.a2a.A2aConnector;
import org.operaton.connect.a2a.A2aConnectorProvider;

/**
 * Registers the A2A connector with {@code Connectors} through {@link java.util.ServiceLoader}.
 */
public class A2aConnectorProviderImpl implements A2aConnectorProvider {

  @Override
  public String getConnectorId() {
    return A2aConnector.ID;
  }

  @Override
  public A2aConnector createConnectorInstance() {
    return new A2aConnectorImpl();
  }

}
