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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.HistoricProcessInstanceQueryImpl;
import org.operaton.bpm.engine.impl.ProcessInstanceQueryImpl;
import org.operaton.bpm.engine.impl.UpdateProcessInstancesSuspensionStateBuilderImpl;
import org.operaton.bpm.engine.impl.batch.BatchElementConfiguration;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;
import org.operaton.commons.utils.CollectionUtil;
import org.operaton.bpm.engine.impl.util.EnsureUtil;

public abstract class AbstractUpdateProcessInstancesSuspendStateCmd<T> implements Command<T> {

  protected UpdateProcessInstancesSuspensionStateBuilderImpl builder;
  protected CommandExecutor commandExecutor;
  protected boolean suspending;

  protected AbstractUpdateProcessInstancesSuspendStateCmd(CommandExecutor commandExecutor, UpdateProcessInstancesSuspensionStateBuilderImpl builder, boolean suspending) {
    this.commandExecutor = commandExecutor;
    this.builder = builder;
    this.suspending = suspending;
  }

  protected BatchElementConfiguration collectProcessInstanceIds(CommandContext commandContext) {
    BatchElementConfiguration elementConfiguration = new BatchElementConfiguration();

    List<String> processInstanceIds = builder.getProcessInstanceIds();
    EnsureUtil.ensureNotContainsNull(BadUserRequestException.class,
        "Cannot be null.", "Process Instance ids", processInstanceIds);
    if (!CollectionUtil.isEmpty(processInstanceIds)) {
      ProcessInstanceQueryImpl query = new ProcessInstanceQueryImpl();
      query.processInstanceIds(new HashSet<>(processInstanceIds));
      elementConfiguration.addDeploymentMappings(
          commandContext.runWithoutAuthorization(query::listDeploymentIdMappings), processInstanceIds);
    }

    ProcessInstanceQueryImpl processInstanceQuery = (ProcessInstanceQueryImpl) builder.getProcessInstanceQuery();
    if( processInstanceQuery != null) {
      elementConfiguration.addDeploymentMappings(processInstanceQuery.listDeploymentIdMappings());
    }

    HistoricProcessInstanceQueryImpl historicProcessInstanceQuery = (HistoricProcessInstanceQueryImpl ) builder.getHistoricProcessInstanceQuery();
    if( historicProcessInstanceQuery != null) {
      elementConfiguration.addDeploymentMappings(historicProcessInstanceQuery.listDeploymentIdMappings());
    }

    return elementConfiguration;
  }

  protected void writeUserOperationLog(CommandContext commandContext, int numInstances,
                                       boolean async) {

    List<PropertyChange> propertyChanges = new ArrayList<>();
    propertyChanges.add(new PropertyChange("nrOfInstances", null, numInstances));
    propertyChanges.add(new PropertyChange("async", null, async));

    String operationType;
    if(suspending) {
      operationType = UserOperationLogEntry.OPERATION_TYPE_SUSPEND_JOB;

    } else {
      operationType = UserOperationLogEntry.OPERATION_TYPE_ACTIVATE_JOB;
    }
    commandContext.getOperationLogManager()
        .logProcessInstanceOperation(operationType,
          null,
          null,
          null,
          propertyChanges);
  }

  protected void writeUserOperationLogAsync(CommandContext commandContext, int numInstances) {
    writeUserOperationLog(commandContext, numInstances, true);
  }

}
