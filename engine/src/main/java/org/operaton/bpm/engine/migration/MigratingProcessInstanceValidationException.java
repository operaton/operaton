/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.migration;

import org.operaton.bpm.engine.ProcessEngineException;

/**
 * Thrown if at least one migration instruction cannot be applied to the activity instance it matches. Contains
 * a object that contains the details for all validation errors.
 *
 * @author Thorben Lindhauer
 */
public class MigratingProcessInstanceValidationException extends ProcessEngineException {

  private static final long serialVersionUID = 1L;

  protected final MigratingProcessInstanceValidationReport validationReport;

  public MigratingProcessInstanceValidationException(String message, MigratingProcessInstanceValidationReport validationReport) {
    super(message);
    this.validationReport = validationReport;
  }

  /**
   * A report with all instructions that cannot be applied to the given process instance
   */
  public MigratingProcessInstanceValidationReport getValidationReport() {
    return validationReport;
  }

}
