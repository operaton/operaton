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
package org.operaton.bpm.engine.impl.form.validator;

import java.io.Serial;

import org.operaton.bpm.engine.impl.form.FormException;

/**
 * Runtime exception for validation of form fields.
 *
 * @author Thomas Skjolberg
 */
public class FormFieldValidatorException extends FormException {

  @Serial private static final long serialVersionUID = 1L;

  /** bpmn element id */
  protected final String id;
  protected final String name;
  protected final String config;
  protected final transient Object value;

  public FormFieldValidatorException(String id, String name, String config, Object value, String message,
      Throwable cause) {
    super(message, cause);

    this.id = id;
    this.name = name;
    this.config = config;
    this.value = value;
  }

  public FormFieldValidatorException(String id, String name, String config, Object value, String message) {
    super(message);

    this.id = id;
    this.name = name;
    this.config = config;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getConfig() {
    return config;
  }

  public Object getValue() {
    return value;
  }

  public String getId() {
    return id;
  }

}
