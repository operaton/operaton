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

import java.util.Map;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.form.handler.FormFieldHandler;

/**
 * <p>Object passed in to a {@link FormFieldValidator} providing access to validation properties</p>
 *
 * @author Daniel Meyer
 *
 */
public interface FormFieldValidatorContext {

  FormFieldHandler getFormFieldHandler();

  /** @return the execution
   * @deprecated Use {@link #getVariableScope()} instead.
   */
  @Deprecated(forRemoval = true, since = "1.0")
  DelegateExecution getExecution();

  /**
   * @return the variable scope in which the value is submitted
   */
  VariableScope getVariableScope();

  /** @return the configuration of this validator */
  String getConfiguration();

  /** @return all values submitted in the form */
  Map<String, Object> getSubmittedValues();

}
