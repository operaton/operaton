/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.impl.bpmn.behavior;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.ProcessEngineServices;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.impl.persistence.entity.util.TypedValueField;
import org.operaton.bpm.engine.impl.variable.serializer.ValueFieldsImpl;
import org.operaton.bpm.engine.runtime.Incident;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.SerializableValue;
import org.operaton.bpm.engine.variable.value.TypedValue;
import org.operaton.bpm.model.bpmn.BpmnModelInstance;
import org.operaton.bpm.model.bpmn.instance.FlowElement;

/**
 * An input-mapping read view of the outer execution with the current iteration's loop variables.
 * Capturing the loop variables once keeps input parameters independent of earlier mapping targets.
 * Execution metadata and mutations retain their original outer-execution semantics; no execution
 * or persistent variable is created for this view.
 */
public class MultiInstanceInputMappingExecution implements DelegateExecution {

  protected final DelegateExecution outerExecution;
  protected final VariableMap iterationVariables = Variables.createVariables();

  public MultiInstanceInputMappingExecution(DelegateExecution outerExecution, DelegateExecution iterationExecution,
      String elementVariable) {
    this.outerExecution = outerExecution;
    captureVariable(iterationExecution, MultiInstanceActivityBehavior.LOOP_COUNTER);
    if (elementVariable != null) {
      captureVariable(iterationExecution, elementVariable);
    }
  }

  protected void captureVariable(DelegateExecution execution, String name) {
    if (execution.hasVariableLocal(name)) {
      iterationVariables.putValueTyped(name, execution.getVariableLocalTyped(name, false));
    }
  }

  protected <T extends TypedValue> T getIterationVariable(String name, boolean deserializeValue) {
    TypedValue value = iterationVariables.getValueTyped(name);
    if (deserializeValue && value instanceof SerializableValue serializableValue && !serializableValue.isDeserialized()) {
      // Deserialize only on demand, using independent value fields so an earlier input assignment
      // cannot replace the captured value. In particular, an unused element must remain unread.
      TypedValueField valueField = new TypedValueField(new ValueFieldsImpl() {
        @Override
        public String getName() {
          return name;
        }
      }, false);
      valueField.setValue(value);
      value = valueField.getTypedValue(true, value.isTransient());
      iterationVariables.putValueTyped(name, value);
    }
    @SuppressWarnings("unchecked")
    T typedValue = (T) value;
    return typedValue;
  }

  protected void addIterationVariables(VariableMap variables, boolean deserializeValues) {
    for (String name : iterationVariables.keySet()) {
      variables.putValueTyped(name, getIterationVariable(name, deserializeValues));
    }
  }

  public boolean hasIterationVariables() {
    return !iterationVariables.isEmpty();
  }

  @Override
  public String getVariableScopeKey() {
    return outerExecution.getVariableScopeKey();
  }

  @Override
  public Map<String, Object> getVariables() {
    return getVariablesTyped();
  }

  @Override
  public VariableMap getVariablesTyped() {
    return getVariablesTyped(true);
  }

  @Override
  public VariableMap getVariablesTyped(boolean deserializeValues) {
    VariableMap variables = Variables.createVariables();
    addIterationVariables(variables, deserializeValues);
    for (String name : outerExecution.getVariableNames()) {
      if (!variables.containsKey(name)) {
        variables.putValueTyped(name, outerExecution.getVariableTyped(name, deserializeValues));
      }
    }
    return variables;
  }

  @Override
  public Object getVariable(String variableName) {
    return iterationVariables.containsKey(variableName)
        ? getIterationVariable(variableName, true).getValue() : outerExecution.getVariable(variableName);
  }

  @Override
  public <T extends TypedValue> T getVariableTyped(String variableName) {
    return getVariableTyped(variableName, true);
  }

  @Override
  public <T extends TypedValue> T getVariableTyped(String variableName, boolean deserializeValue) {
    return iterationVariables.containsKey(variableName)
        ? getIterationVariable(variableName, deserializeValue)
        : outerExecution.getVariableTyped(variableName, deserializeValue);
  }

  @Override
  public Set<String> getVariableNames() {
    Set<String> names = new HashSet<>(outerExecution.getVariableNames());
    names.addAll(iterationVariables.keySet());
    return names;
  }

  @Override
  public boolean hasVariable(String variableName) {
    return iterationVariables.containsKey(variableName) || outerExecution.hasVariable(variableName);
  }

  @Override
  public boolean hasVariables() {
    return !iterationVariables.isEmpty() || outerExecution.hasVariables();
  }

  @Override
  public Map<String, Object> getVariablesLocal() {
    return getVariablesLocalTyped();
  }

  @Override
  public VariableMap getVariablesLocalTyped() {
    return getVariablesLocalTyped(true);
  }

  @Override
  public VariableMap getVariablesLocalTyped(boolean deserializeValues) {
    VariableMap variables = Variables.createVariables();
    addIterationVariables(variables, deserializeValues);
    for (String name : outerExecution.getVariableNamesLocal()) {
      if (!variables.containsKey(name)) {
        variables.putValueTyped(name, outerExecution.getVariableLocalTyped(name, deserializeValues));
      }
    }
    return variables;
  }

  @Override
  public Object getVariableLocal(String variableName) {
    return iterationVariables.containsKey(variableName)
        ? getIterationVariable(variableName, true).getValue() : outerExecution.getVariableLocal(variableName);
  }

  @Override
  public <T extends TypedValue> T getVariableLocalTyped(String variableName) {
    return getVariableLocalTyped(variableName, true);
  }

  @Override
  public <T extends TypedValue> T getVariableLocalTyped(String variableName, boolean deserializeValue) {
    return iterationVariables.containsKey(variableName)
        ? getIterationVariable(variableName, deserializeValue)
        : outerExecution.getVariableLocalTyped(variableName, deserializeValue);
  }

  @Override
  public Set<String> getVariableNamesLocal() {
    Set<String> names = new HashSet<>(outerExecution.getVariableNamesLocal());
    names.addAll(iterationVariables.keySet());
    return names;
  }

  @Override
  public boolean hasVariableLocal(String variableName) {
    return iterationVariables.containsKey(variableName) || outerExecution.hasVariableLocal(variableName);
  }

  @Override
  public boolean hasVariablesLocal() {
    return !iterationVariables.isEmpty() || outerExecution.hasVariablesLocal();
  }

  @Override
  public void setVariable(String variableName, Object value) {
    outerExecution.setVariable(variableName, value);
  }

  @Override
  public void setVariableLocal(String variableName, Object value) {
    outerExecution.setVariableLocal(variableName, value);
  }

  @Override
  public void setVariables(Map<String, ? extends Object> variables) {
    outerExecution.setVariables(variables);
  }

  @Override
  public void setVariablesLocal(Map<String, ? extends Object> variables) {
    outerExecution.setVariablesLocal(variables);
  }

  @Override
  public void removeVariable(String variableName) {
    outerExecution.removeVariable(variableName);
  }

  @Override
  public void removeVariableLocal(String variableName) {
    outerExecution.removeVariableLocal(variableName);
  }

  @Override
  public void removeVariables(Collection<String> variableNames) {
    outerExecution.removeVariables(variableNames);
  }

  @Override
  public void removeVariablesLocal(Collection<String> variableNames) {
    outerExecution.removeVariablesLocal(variableNames);
  }

  @Override
  public void removeVariables() {
    outerExecution.removeVariables();
  }

  @Override
  public void removeVariablesLocal() {
    outerExecution.removeVariablesLocal();
  }

  @Override
  public String getId() {
    return outerExecution.getId();
  }

  @Override
  public String getEventName() {
    return outerExecution.getEventName();
  }

  @Override
  public String getBusinessKey() {
    return outerExecution.getBusinessKey();
  }

  @Override
  public String getProcessInstanceId() {
    return outerExecution.getProcessInstanceId();
  }

  @Override
  public String getProcessBusinessKey() {
    return outerExecution.getProcessBusinessKey();
  }

  @Override
  public void setProcessBusinessKey(String businessKey) {
    outerExecution.setProcessBusinessKey(businessKey);
  }

  @Override
  public String getProcessDefinitionId() {
    return outerExecution.getProcessDefinitionId();
  }

  @Override
  public String getParentId() {
    return outerExecution.getParentId();
  }

  @Override
  public String getCurrentActivityId() {
    return outerExecution.getCurrentActivityId();
  }

  @Override
  public String getCurrentActivityName() {
    return outerExecution.getCurrentActivityName();
  }

  @Override
  public String getActivityInstanceId() {
    return outerExecution.getActivityInstanceId();
  }

  @Override
  public String getParentActivityInstanceId() {
    return outerExecution.getParentActivityInstanceId();
  }

  @Override
  public String getCurrentTransitionId() {
    return outerExecution.getCurrentTransitionId();
  }

  @Override
  public DelegateExecution getProcessInstance() {
    return outerExecution.getProcessInstance();
  }

  @Override
  public DelegateExecution getSuperExecution() {
    return outerExecution.getSuperExecution();
  }

  @Override
  public boolean isCanceled() {
    return outerExecution.isCanceled();
  }

  @Override
  public String getTenantId() {
    return outerExecution.getTenantId();
  }

  @Override
  public void setVariable(String variableName, Object value, String activityId) {
    outerExecution.setVariable(variableName, value, activityId);
  }

  @Override
  public Incident createIncident(String incidentType, String configuration) {
    return outerExecution.createIncident(incidentType, configuration);
  }

  @Override
  public Incident createIncident(String incidentType, String configuration, String message) {
    return outerExecution.createIncident(incidentType, configuration, message);
  }

  @Override
  public void resolveIncident(String incidentId) {
    outerExecution.resolveIncident(incidentId);
  }

  @Override
  public BpmnModelInstance getBpmnModelInstance() {
    return outerExecution.getBpmnModelInstance();
  }

  @Override
  public FlowElement getBpmnModelElementInstance() {
    return outerExecution.getBpmnModelElementInstance();
  }

  @Override
  public ProcessEngineServices getProcessEngineServices() {
    return outerExecution.getProcessEngineServices();
  }

  @Override
  public ProcessEngine getProcessEngine() {
    return outerExecution.getProcessEngine();
  }

}
