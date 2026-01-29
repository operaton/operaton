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
package org.operaton.connect.plugin.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.operaton.bpm.engine.impl.core.variable.CoreVariableInstance;
import org.operaton.bpm.engine.impl.core.variable.scope.AbstractVariableScope;
import org.operaton.bpm.engine.impl.core.variable.scope.SimpleVariableInstance;
import org.operaton.bpm.engine.impl.core.variable.scope.SimpleVariableInstance.SimpleVariableInstanceFactory;
import org.operaton.bpm.engine.impl.core.variable.scope.VariableInstanceFactory;
import org.operaton.bpm.engine.impl.core.variable.scope.VariableInstanceLifecycleListener;
import org.operaton.bpm.engine.impl.core.variable.scope.VariableStore;
import org.operaton.connect.spi.ConnectorRequest;
import org.operaton.connect.spi.ConnectorResponse;

/**
 * Exposes a connector request as variableScope.
 *
 * @author Daniel Meyer
 *
 */
public class ConnectorVariableScope extends AbstractVariableScope {

  protected AbstractVariableScope parent;

  protected transient VariableStore<SimpleVariableInstance> variableStore;

  public ConnectorVariableScope(AbstractVariableScope parent) {
    this.parent = parent;
    this.variableStore = new VariableStore<>();
  }

  @Override
  public String getVariableScopeKey() {
    return "connector";
  }

  protected VariableStore<CoreVariableInstance> getVariableStore() {
    return (VariableStore) variableStore;
  }

  @Override
  protected VariableInstanceFactory<CoreVariableInstance> getVariableInstanceFactory() {
    return (VariableInstanceFactory) SimpleVariableInstanceFactory.INSTANCE;
  }

  @Override
  protected List<VariableInstanceLifecycleListener<CoreVariableInstance>> getVariableInstanceLifecycleListeners() {
    return Collections.emptyList();
  }

  @Override
  public AbstractVariableScope getParentVariableScope() {
    return parent;
  }

  public void writeToRequest(ConnectorRequest<?> request) {
    for (CoreVariableInstance variable : variableStore.getVariables()) {
      request.setRequestParameter(variable.getName(), variable.getTypedValue(true).getValue());
    }
  }

  public void readFromResponse(ConnectorResponse response) {
    Map<String, Object> responseParameters = response.getResponseParameters();
    for (Entry<String, Object> entry : responseParameters.entrySet()) {
      setVariableLocal(entry.getKey(), entry.getValue());
    }
  }

}
