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
package org.operaton.bpm.engine.impl.el;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.operaton.bpm.engine.delegate.VariableScope;
import org.operaton.bpm.engine.impl.core.variable.CoreVariableInstance;
import org.operaton.bpm.engine.variable.VariableMap;
import org.operaton.bpm.engine.variable.Variables;
import org.operaton.bpm.engine.variable.value.TypedValue;


/**
 * Variable-scope only used to resolve variables when NO execution is active but
 * expression-resolving is needed. This occurs eg. when start-form properties have default's
 * defined. Even though variables are not available yet, expressions should be resolved
 * anyway.
 *
 * @author Frederik Heremans
 */
public class StartProcessVariableScope implements VariableScope {

  private static final StartProcessVariableScope INSTANCE = new StartProcessVariableScope();

  private static final VariableMap EMPTY_VARIABLE_MAP = Variables.fromMap(Collections.emptyMap());

  /**
   * Since a {@link StartProcessVariableScope} has no state, it's safe to use the same
   * instance to prevent too many useless instances created.
   */
  public static StartProcessVariableScope getSharedInstance()  {
    return INSTANCE;
  }

  @Override
  public String getVariableScopeKey() {
    return "scope";
  }

  @Override
  public VariableMap getVariables() {
    return EMPTY_VARIABLE_MAP;
  }

  @Override
  public VariableMap getVariablesLocal() {
    return EMPTY_VARIABLE_MAP;
  }

  @Override
  public Object getVariable(String variableName) {
    return null;
  }

  @Override
  public Object getVariableLocal(String variableName) {
    return null;
  }

  @Override
  public VariableMap getVariablesTyped(boolean deserializeObjectValues) {
    return getVariables();
  }

  @Override
  public VariableMap getVariablesLocalTyped() {
    return getVariablesLocalTyped(true);
  }

  @Override
  public VariableMap getVariablesTyped() {
    return getVariablesTyped(true);
  }

  @Override
  public VariableMap getVariablesLocalTyped(boolean deserializeObjectValues) {
    return getVariablesLocal();
  }

  @SuppressWarnings("unused")
  public Object getVariable(String variableName, boolean deserializeObjectValue) {
    return null;
  }

  @SuppressWarnings("unused")
  public Object getVariableLocal(String variableName, boolean deserializeObjectValue) {
    return null;
  }

  @Override
  public <T extends TypedValue> T getVariableTyped(String variableName) {
    return null;
  }

  @Override
  public <T extends TypedValue> T getVariableTyped(String variableName, boolean deserializeObjectValue) {
    return null;
  }

  @Override
  public <T extends TypedValue> T getVariableLocalTyped(String variableName) {
    return null;
  }

  @Override
  public <T extends TypedValue> T getVariableLocalTyped(String variableName, boolean deserializeObjectValue) {
    return null;
  }

  @Override
  public void setVariable(String variableName, Object value) {
    throw new UnsupportedOperationException("No execution active, no variables can be set");
  }

  @Override
  public void setVariableLocal(String variableName, Object value) {
    throw new UnsupportedOperationException("No execution active, no variables can be set");
  }

  @Override
  public void setVariables(Map<String, ? extends Object> variables) {
    throw new UnsupportedOperationException("No execution active, no variables can be set");
  }

  @Override
  public void setVariablesLocal(Map<String, ? extends Object> variables) {
    throw new UnsupportedOperationException("No execution active, no variables can be set");
  }

  @Override
  public boolean hasVariables() {
    return false;
  }

  @Override
  public boolean hasVariablesLocal() {
    return false;
  }

  @Override
  public boolean hasVariable(String variableName) {
    return false;
  }

  @Override
  public boolean hasVariableLocal(String variableName) {
    return false;
  }

  @Override
  public void removeVariable(String variableName) {
    throw new UnsupportedOperationException("No execution active, no variables can be removed");
  }

  @Override
  public void removeVariableLocal(String variableName) {
    throw new UnsupportedOperationException("No execution active, no variables can be removed");
  }

  @Override
  public void removeVariables() {
    throw new UnsupportedOperationException("No execution active, no variables can be removed");
  }

  @Override
  public void removeVariablesLocal() {
    throw new UnsupportedOperationException("No execution active, no variables can be removed");
  }

  @Override
  public void removeVariables(Collection<String> variableNames) {
    throw new UnsupportedOperationException("No execution active, no variables can be removed");
  }

  @Override
  public void removeVariablesLocal(Collection<String> variableNames) {
    throw new UnsupportedOperationException("No execution active, no variables can be removed");
  }

  public Map<String, CoreVariableInstance> getVariableInstances() {
    return Collections.emptyMap();
  }

  @SuppressWarnings("unused")
  public CoreVariableInstance getVariableInstance(String name) {
    return null;
  }

  public Map<String, CoreVariableInstance> getVariableInstancesLocal() {
    return Collections.emptyMap();
  }

  @SuppressWarnings("unused")
  public CoreVariableInstance getVariableInstanceLocal(String name) {
    return null;
  }

}
