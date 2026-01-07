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
package org.operaton.bpm.engine.impl.cmd;

import java.util.Collection;
import java.util.Map;

import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;

/**
 * Patches task variables: First, applies modifications to existing variables and then deletes
 * specified variables.
 *
 * @author kristin.polenz@camunda.com
 *
 */
public class PatchTaskVariablesCmd extends AbstractPatchVariablesCmd {
  public PatchTaskVariablesCmd(String taskId, Map<String, ? extends Object> modifications, Collection<String> deletions, boolean isLocal) {
    super(taskId, modifications, deletions, isLocal);
  }

  @Override
  protected AbstractSetVariableCmd getSetVariableCmd() {
    return new SetTaskVariablesCmd(entityId, variables, isLocal);
  }

  @Override
  protected AbstractRemoveVariableCmd getRemoveVariableCmd() {
    return new RemoveTaskVariablesCmd(entityId, deletions, isLocal);
  }

  @Override
  public void logVariableOperation(CommandContext commandContext) {
    commandContext.getOperationLogManager().logVariableOperation(getLogEntryOperation(), null, entityId,
      PropertyChange.EMPTY_CHANGE);
  }
}
