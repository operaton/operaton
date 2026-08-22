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
package org.operaton.connect.a2a;

import org.operaton.connect.spi.Connector;

/**
 * A connector which calls an AI agent over the
 * <a href="https://a2a-protocol.org/">Agent2Agent (A2A) protocol</a>.
 *
 * <p>
 * The connector is meant to be used declaratively from a BPMN {@code serviceTask}, so that adding an
 * agent call to a process does not require writing Java:
 * </p>
 *
 * <pre>{@code
 * <serviceTask id="askAgent">
 *   <extensionElements>
 *     <operaton:connector>
 *       <operaton:connectorId>a2a</operaton:connectorId>
 *       <operaton:inputOutput>
 *         <operaton:inputParameter name="operation">sendSync</operaton:inputParameter>
 *         <operaton:inputParameter name="url">https://agent.example.com</operaton:inputParameter>
 *         <operaton:inputParameter name="message">${question}</operaton:inputParameter>
 *         <operaton:outputParameter name="answer">${text}</operaton:outputParameter>
 *       </operaton:inputOutput>
 *     </operaton:connector>
 *   </extensionElements>
 * </serviceTask>
 * }</pre>
 *
 * @see A2aRequest for the supported input parameters
 * @see A2aResponse for the produced output parameters
 * @since 2.2
 */
public interface A2aConnector extends Connector<A2aRequest> {

  /** The connector id to reference from {@code operaton:connectorId}. */
  String ID = "a2a";

}
