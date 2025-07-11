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
package org.operaton.bpm.engine.impl.core.variable.scope;

import org.operaton.bpm.engine.impl.cmmn.execution.CmmnExecution;
import org.operaton.bpm.engine.impl.persistence.entity.VariableInstanceEntity;
import org.operaton.bpm.model.cmmn.VariableTransition;

/**
 *
 * @author Deivarayan Azhagappan
 *
 */
public class VariableOnPartListener implements VariableInstanceLifecycleListener<VariableInstanceEntity> {

  protected CmmnExecution execution;

  public VariableOnPartListener(CmmnExecution execution) {
    this.execution = execution;
  }

  @Override
  public void onCreate(VariableInstanceEntity variable, AbstractVariableScope sourceScope) {
    execution.handleVariableTransition(variable.getName(), VariableTransition.create.name());
  }

  @Override
  public void onUpdate(VariableInstanceEntity variable, AbstractVariableScope sourceScope) {
    execution.handleVariableTransition(variable.getName(), VariableTransition.update.name());
  }

  @Override
  public void onDelete(VariableInstanceEntity variable, AbstractVariableScope sourceScope) {
    execution.handleVariableTransition(variable.getName(), VariableTransition.delete.name());
  }

}
