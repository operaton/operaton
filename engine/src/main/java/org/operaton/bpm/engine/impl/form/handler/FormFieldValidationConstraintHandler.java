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
package org.operaton.bpm.engine.impl.form.handler;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.form.FormFieldValidationConstraint;
import org.operaton.bpm.engine.impl.form.FormFieldValidationConstraintImpl;
import org.operaton.bpm.engine.impl.form.validator.FormFieldValidationException;
import org.operaton.bpm.engine.impl.form.validator.FormFieldValidator;
import org.operaton.bpm.engine.impl.form.validator.FormFieldValidatorContext;
import org.operaton.bpm.engine.impl.form.validator.FormFieldValidatorException;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.variable.VariableMap;

import static java.util.Objects.requireNonNull;

/**
 * <p>Wrapper for a validation constraint</p>
 *
 * @author Daniel Meyer
 *
 */
public @NullMarked class FormFieldValidationConstraintHandler {

  protected @Nullable String name;
  protected @Nullable String config;
  protected @Nullable FormFieldValidator validator;

  @SuppressWarnings("unused")
  public FormFieldValidationConstraint createValidationConstraint(ExecutionEntity execution) {
    return new FormFieldValidationConstraintImpl(name, config);
  }

  // submit /////////////////////////////////

  public void validate(Object submittedValue, VariableMap submittedValues, FormFieldHandler formFieldHandler, VariableScope variableScope) {
    try {
      requireNonNull(validator);
      requireNonNull(config);
      FormFieldValidatorContext context = new DefaultFormFieldValidatorContext(variableScope, config, submittedValues, formFieldHandler);
      if(!validator.validate(submittedValue, context)) {
        throw new FormFieldValidatorException(formFieldHandler.getId(), name, config, submittedValue, "Invalid value submitted for form field '%s': validation of %s failed.".formatted(formFieldHandler.getId(), this));
      }
    } catch(FormFieldValidationException e) {
      throw new FormFieldValidatorException(formFieldHandler.getId(), name, config, submittedValue, "Invalid value submitted for form field '%s': validation of %s failed.".formatted(formFieldHandler.getId(), this), e);
    }
  }

  // getter / setter ////////////////////////

  public @Nullable String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  public void setConfig(@Nullable String config) {
    this.config = config;
  }

  public @Nullable String getConfig() {
    return config;
  }

  public void setValidator(@Nullable FormFieldValidator validator) {
    this.validator = validator;
  }

  public @Nullable FormFieldValidator getValidator() {
    return validator;
  }

  @Override
  public String toString() {
    return name + (config != null ? ("(%s)".formatted(config)) : "");
  }

}
