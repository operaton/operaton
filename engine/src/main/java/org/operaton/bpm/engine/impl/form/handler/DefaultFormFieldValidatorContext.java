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

import java.util.Map;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.form.validator.FormFieldValidatorContext;
import org.operaton.bpm.engine.impl.persistence.entity.TaskEntity;
import org.operaton.bpm.engine.variable.VariableMap;

/**
 * @author Daniel Meyer
 *
 */
public class DefaultFormFieldValidatorContext implements FormFieldValidatorContext {

  protected VariableScope variableScope;
  protected String configuration;
  protected VariableMap submittedValues;
  protected FormFieldHandler formFieldHandler;

  public DefaultFormFieldValidatorContext(VariableScope variableScope, String configuration, VariableMap submittedValues,
    FormFieldHandler formFieldHandler) {
    super();
    this.variableScope = variableScope;
    this.configuration = configuration;
    this.submittedValues = submittedValues;
    this.formFieldHandler = formFieldHandler;
  }

  @Override
  public FormFieldHandler getFormFieldHandler() {
    return formFieldHandler;
  }

  @Override
  public DelegateExecution getExecution() {
    if(variableScope instanceof DelegateExecution delegateExecution) {
      return delegateExecution;
    }
    else if(variableScope instanceof TaskEntity taskEntity){
      return taskEntity.getExecution();
    }
    else {
      return null;
    }
  }

  @Override
  public VariableScope getVariableScope() {
    return variableScope;
  }

  @Override
  public String getConfiguration() {
    return configuration;
  }

  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }

  @Override
  public Map<String, Object> getSubmittedValues() {
    return submittedValues;
  }

}
