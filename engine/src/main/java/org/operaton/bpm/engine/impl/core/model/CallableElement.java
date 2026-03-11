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
package org.operaton.bpm.engine.impl.core.model;

import java.util.ArrayList;
import java.util.List;

import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;

/**
 * @author Roman Smirnov
 *
 */
public class CallableElement extends BaseCallableElement {

  protected ParameterValueProvider businessKeyValueProvider;
  protected List<CallableElementParameter> inputs;
  protected List<CallableElementParameter> outputs;
  protected List<CallableElementParameter> outputsLocal;

  public CallableElement() {
    this.inputs = new ArrayList<>();
    this.outputs = new ArrayList<>();
    this.outputsLocal = new ArrayList<>();
  }

  // definitionKey ////////////////////////////////////////////////////////////////

  // binding /////////////////////////////////////////////////////////////////////

  // version //////////////////////////////////////////////////////////////////////

  // businessKey /////////////////////////////////////////////////////////////////

  public String getBusinessKey(VariableScope variableScope) {
    if (businessKeyValueProvider == null) {
      return null;
    }

    Object result = businessKeyValueProvider.getValue(variableScope);

    if (result != null && !(result instanceof String)) {
      throw new ClassCastException("Cannot cast '%s' to String".formatted(result));
    }

    return (String) result;
  }

  public ParameterValueProvider getBusinessKeyValueProvider() {
    return businessKeyValueProvider;
  }

  public void setBusinessKeyValueProvider(ParameterValueProvider businessKeyValueProvider) {
    this.businessKeyValueProvider = businessKeyValueProvider;
  }

  // inputs //////////////////////////////////////////////////////////////////////

  public List<CallableElementParameter> getInputs() {
    return inputs;
  }

  public void addInput(CallableElementParameter input) {
    inputs.add(input);
  }

  public void addInputs(List<CallableElementParameter> inputs) {
    this.inputs.addAll(inputs);
  }

  public VariableMap getInputVariables(VariableScope variableScope) {
    return getVariables(getInputs(), variableScope);
  }

  // outputs /////////////////////////////////////////////////////////////////////

  public List<CallableElementParameter> getOutputs() {
    return outputs;
  }

  public List<CallableElementParameter> getOutputsLocal() {
    return outputsLocal;
  }

  public void addOutput(CallableElementParameter output) {
    outputs.add(output);
  }

  public void addOutputLocal(CallableElementParameter output) {
    outputsLocal.add(output);
  }

  public void addOutputs(List<CallableElementParameter> outputs) {
    this.outputs.addAll(outputs);
  }

  public VariableMap getOutputVariables(VariableScope calledElementScope) {
    return getVariables(getOutputs(), calledElementScope);
  }

  public VariableMap getOutputVariablesLocal(VariableScope calledElementScope) {
    return getVariables(getOutputsLocal(), calledElementScope);
  }

  // variables //////////////////////////////////////////////////////////////////

  protected VariableMap getVariables(List<CallableElementParameter> params, VariableScope variableScope) {
    VariableMap result = Variables.createVariables();

    for (CallableElementParameter param : params) {
      param.applyTo(variableScope, result);
    }

    return result;
  }

  // deployment id //////////////////////////////////////////////////////////////

}
