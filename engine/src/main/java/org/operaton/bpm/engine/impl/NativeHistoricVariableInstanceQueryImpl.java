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
package org.operaton.bpm.engine.impl;

import java.io.Serial;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.history.HistoricVariableInstance;
import org.operaton.bpm.engine.history.NativeHistoricVariableInstanceQuery;
import org.operaton.bpm.engine.impl.cmd.CommandLogger;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.HistoricVariableInstanceEntity;


public class NativeHistoricVariableInstanceQueryImpl extends AbstractNativeQuery<NativeHistoricVariableInstanceQuery, HistoricVariableInstance>
  implements NativeHistoricVariableInstanceQuery {

  private static final CommandLogger LOG = ProcessEngineLogger.CMD_LOGGER;

  @Serial private static final long serialVersionUID = 1L;

  protected boolean isCustomObjectDeserializationEnabled = true;

  public NativeHistoricVariableInstanceQueryImpl(CommandContext commandContext) {
    super(commandContext);
  }

  public NativeHistoricVariableInstanceQueryImpl(CommandExecutor commandExecutor) {
    super(commandExecutor);
  }


  //results ////////////////////////////////////////////////////////////////

  @Override
  public NativeHistoricVariableInstanceQuery disableCustomObjectDeserialization() {
        this.isCustomObjectDeserializationEnabled = false;
        return this;
  }

  @Override
  public List<HistoricVariableInstance> executeList(CommandContext commandContext, Map<String, Object> parameterMap, int firstResult, int maxResults) {
    List<HistoricVariableInstance> historicVariableInstances = commandContext
            .getHistoricVariableInstanceManager()
            .findHistoricVariableInstancesByNativeQuery(parameterMap, firstResult, maxResults);

    if (historicVariableInstances!=null) {
      for (HistoricVariableInstance historicVariableInstance: historicVariableInstances) {

        HistoricVariableInstanceEntity variableInstanceEntity = (HistoricVariableInstanceEntity) historicVariableInstance;
          try {
            variableInstanceEntity.getTypedValue(isCustomObjectDeserializationEnabled);
          } catch(Exception t) {
            // do not fail if one of the variables fails to load
            LOG.exceptionWhileGettingValueForVariable(t);
          }
      }
    }
    return historicVariableInstances;
  }

  @Override
  public long executeCount(CommandContext commandContext, Map<String, Object> parameterMap) {
    return commandContext
      .getHistoricVariableInstanceManager()
      .findHistoricVariableInstanceCountByNativeQuery(parameterMap);
  }

}
