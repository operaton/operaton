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
package org.operaton.bpm.engine.impl.form.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.form.FormField;
import org.operaton.bpm.engine.form.FormFieldValidationConstraint;
import org.operaton.bpm.engine.form.FormProperty;
import org.operaton.bpm.engine.form.FormType;
import org.operaton.bpm.engine.impl.form.FormFieldValidationConstraintImpl;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;

/**
 * @author Daniel Meyer
 *
 */
public class FormPropertyAdapter implements FormField {

  protected FormProperty formProperty;
  protected List<FormFieldValidationConstraint> validationConstraints;

  public FormPropertyAdapter(FormProperty formProperty) {
    super();
    this.formProperty = formProperty;

    validationConstraints = new ArrayList<>();
    if(formProperty.isRequired()) {
      validationConstraints.add(new FormFieldValidationConstraintImpl(HtmlFormEngine.CONSTRAINT_REQUIRED, null));
    }
    if(!formProperty.isWritable()) {
      validationConstraints.add(new FormFieldValidationConstraintImpl(HtmlFormEngine.CONSTRAINT_READONLY, null));
    }
  }

  @Override
  public String getId() {
    return formProperty.getId();
  }

  @Override
  public String getLabel() {
    return formProperty.getName();
  }

  @Override
  public FormType getType() {
    return formProperty.getType();
  }

  @Override
  public String getTypeName() {
    return formProperty.getType().getName();
  }

  @Override
  public Object getDefaultValue() {
    return formProperty.getValue();
  }

  @Override
  public List<FormFieldValidationConstraint> getValidationConstraints() {
    return validationConstraints;
  }

  @Override
  public Map<String, String> getProperties() {
    return Collections.emptyMap();
  }

  @Override
  public boolean isBusinessKey() {
    return false;
  }

  public TypedValue getDefaultValueTyped() {
    return getValue();
  }

  @Override
  public TypedValue getValue() {
    return Variables.stringValue(formProperty.getValue());
  }

}
