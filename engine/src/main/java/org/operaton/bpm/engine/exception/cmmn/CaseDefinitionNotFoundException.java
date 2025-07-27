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
package org.operaton.bpm.engine.exception.cmmn;

import java.io.Serial;


/**
 * <p>This is exception is thrown when a specific case definition is not found.</p>
 *
 * @author Roman Smirnov
 *
 */
public class CaseDefinitionNotFoundException extends CaseException {

  @Serial private static final long serialVersionUID = 1L;

  public CaseDefinitionNotFoundException() {
  }

  public CaseDefinitionNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public CaseDefinitionNotFoundException(String message) {
    super(message);
  }

  public CaseDefinitionNotFoundException(Throwable cause) {
    super(cause);
  }

}
