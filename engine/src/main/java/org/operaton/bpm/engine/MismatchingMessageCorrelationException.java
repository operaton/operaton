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
package org.operaton.bpm.engine;

import java.io.Serial;
import java.util.Map;

/**
 * @author Thorben Lindhauer
 */
public class MismatchingMessageCorrelationException extends
    ProcessEngineException {

  @Serial private static final long serialVersionUID = 1L;

  public MismatchingMessageCorrelationException(String message) {
    super(message);
  }

  public MismatchingMessageCorrelationException(String messageName, String reason) {
    this("Cannot correlate message '%s': %s".formatted(messageName, reason));
  }

  public MismatchingMessageCorrelationException(String messageName,
      String businessKey, Map<String, Object> correlationKeys) {
    this("Cannot correlate message '%s' with process instance business key '%s' and correlation keys %s".formatted(messageName, businessKey, correlationKeys));
  }

  public MismatchingMessageCorrelationException(String messageName,
      String businessKey, Map<String, Object> correlationKeys, String reason) {
    this("Cannot correlate message '%s' with process instance business key '%s' and correlation keys %s: %s".formatted(messageName, businessKey, correlationKeys, reason));
  }
}
