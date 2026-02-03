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

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.delegate.Expression;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.form.FormProperty;
import org.operaton.bpm.engine.form.FormType;
import org.operaton.bpm.engine.impl.el.StartProcessVariableScope;
import org.operaton.bpm.engine.impl.form.FormPropertyImpl;
import org.operaton.bpm.engine.impl.form.type.AbstractFormFieldType;
import org.operaton.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.operaton.bpm.engine.variable.VariableMap;


/**
 * @author Tom Baeyens
 */
public class FormPropertyHandler {

  protected String id;
  protected String name;
  protected AbstractFormFieldType type;
  protected boolean isReadable;
  protected boolean isWritable;
  protected boolean isRequired;
  protected String variableName;
  protected Expression variableExpression;
  protected Expression defaultExpression;

  public FormProperty createFormProperty(ExecutionEntity execution) {
    FormPropertyImpl formProperty = new FormPropertyImpl(this);
    Object modelValue = null;

    modelValue = getModelValue(execution, modelValue);

    if (modelValue instanceof String string) {
      formProperty.setValue(string);
    } else if (type != null) {
      String formValue = type.convertModelValueToFormValue(modelValue);
      formProperty.setValue(formValue);
    } else if (modelValue != null) {
      formProperty.setValue(modelValue.toString());
    }

    return formProperty;
  }

  private Object getModelValue(ExecutionEntity execution, Object modelValue) {
    if (execution !=null) {
      if (variableName != null || variableExpression == null) {
        final String varName = variableName != null ? variableName : id;
        if (execution.hasVariable(varName)) {
          modelValue = execution.getVariable(varName);
        } else if (defaultExpression != null) {
          modelValue = defaultExpression.getValue(execution);
        }
      } else {
        modelValue = variableExpression.getValue(execution);
      }
    } else {
      // Execution is null, the form-property is used in a start-form. Default value
      // should be available (ACT-1028) even though no execution is available.
      if (defaultExpression != null) {
        modelValue = defaultExpression.getValue(StartProcessVariableScope.getSharedInstance());
      }
    }
    return modelValue;
  }

  public void submitFormProperty(VariableScope variableScope, VariableMap variables) {
    validateFormProperty(variables);
    Object modelValue = getSubmittedValue(variableScope, variables);
    if (modelValue != null) {
      assignValueToVariable(variableScope, modelValue);
    }
  }

  private void validateFormProperty(VariableMap variables) {
    if (!isWritable && variables.containsKey(id)) {
      throw new ProcessEngineException("form property '%s' is not writable".formatted(id));
    }

    if (isRequired && !variables.containsKey(id) && defaultExpression == null) {
      throw new ProcessEngineException("form property '%s' is required".formatted(id));
    }
  }

  private Object getSubmittedValue(VariableScope variableScope, VariableMap variables) {
    if (variables.containsKey(id)) {
      Object propertyValue = variables.remove(id);
      if (type != null) {
        return type.convertFormValueToModelValue(propertyValue);
      } else {
        return propertyValue;
      }
    } else if (defaultExpression != null) {
      Object expressionValue = defaultExpression.getValue(variableScope);
      if (expressionValue != null) {
        if (type != null) {
          return type.convertFormValueToModelValue(expressionValue.toString());
        } else {
          return expressionValue.toString();
        }
      } else if (isRequired) {
        throw new ProcessEngineException("form property '%s' is required".formatted(id));
      }
    }
    return null;
  }

  private void assignValueToVariable(VariableScope variableScope, Object modelValue) {
    if (variableName != null) {
      variableScope.setVariable(variableName, modelValue);
    } else if (variableExpression != null) {
      variableExpression.setValue(modelValue, variableScope);
    } else {
      variableScope.setVariable(id, modelValue);
    }
  }

  // getters and setters //////////////////////////////////////////////////////

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public FormType getType() {
    return type;
  }

  public void setType(AbstractFormFieldType type) {
    this.type = type;
  }

  public boolean isReadable() {
    return isReadable;
  }

  public void setReadable(boolean isReadable) {
    this.isReadable = isReadable;
  }

  public boolean isRequired() {
    return isRequired;
  }

  public void setRequired(boolean isRequired) {
    this.isRequired = isRequired;
  }

  public String getVariableName() {
    return variableName;
  }

  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }

  public Expression getVariableExpression() {
    return variableExpression;
  }

  public void setVariableExpression(Expression variableExpression) {
    this.variableExpression = variableExpression;
  }

  public Expression getDefaultExpression() {
    return defaultExpression;
  }

  public void setDefaultExpression(Expression defaultExpression) {
    this.defaultExpression = defaultExpression;
  }

  public boolean isWritable() {
    return isWritable;
  }

  public void setWritable(boolean isWritable) {
    this.isWritable = isWritable;
  }
}
