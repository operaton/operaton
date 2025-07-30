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
package org.operaton.bpm.engine.impl.cmd.batch;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotEmpty;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.authorization.BatchPermissions;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.UserOperationLogEntry;
import org.operaton.bpm.engine.impl.HistoricProcessInstanceQueryImpl;
import org.operaton.bpm.engine.impl.batch.BatchConfiguration;
import org.operaton.bpm.engine.impl.batch.BatchElementConfiguration;
import org.operaton.bpm.engine.impl.batch.builder.BatchBuilder;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;
import org.operaton.commons.utils.CollectionUtil;

/**
 * @author Askar Akhmerov
 */
public class DeleteHistoricProcessInstancesBatchCmd implements Command<Batch> {

  protected final String deleteReason;
  protected List<String> historicProcessInstanceIds;
  protected HistoricProcessInstanceQuery historicProcessInstanceQuery;

  public DeleteHistoricProcessInstancesBatchCmd(List<String> historicProcessInstanceIds, HistoricProcessInstanceQuery historicProcessInstanceQuery,
      String deleteReason) {
    super();
    this.historicProcessInstanceIds = historicProcessInstanceIds;
    this.historicProcessInstanceQuery = historicProcessInstanceQuery;
    this.deleteReason = deleteReason;
  }

  @Override
  public Batch execute(CommandContext commandContext) {
    BatchElementConfiguration elementConfiguration = collectHistoricProcessInstanceIds(commandContext);

    ensureNotEmpty(BadUserRequestException.class, "historicProcessInstanceIds", elementConfiguration.getIds());

    return new BatchBuilder(commandContext)
        .type(Batch.TYPE_HISTORIC_PROCESS_INSTANCE_DELETION)
        .config(getConfiguration(elementConfiguration))
        .permission(BatchPermissions.CREATE_BATCH_DELETE_FINISHED_PROCESS_INSTANCES)
        .operationLogHandler(this::writeUserOperationLog)
        .build();
  }

  protected BatchElementConfiguration collectHistoricProcessInstanceIds(CommandContext commandContext) {

    BatchElementConfiguration elementConfiguration = new BatchElementConfiguration();

    List<String> processInstanceIds = this.getHistoricProcessInstanceIds();
    if (!CollectionUtil.isEmpty(processInstanceIds)) {
      HistoricProcessInstanceQueryImpl query = new HistoricProcessInstanceQueryImpl();
      query.processInstanceIds(new HashSet<>(processInstanceIds));
      elementConfiguration.addDeploymentMappings(commandContext.runWithoutAuthorization(query::listDeploymentIdMappings), processInstanceIds);
    }

    HistoricProcessInstanceQueryImpl processInstanceQuery = (HistoricProcessInstanceQueryImpl) this.historicProcessInstanceQuery;
    if (processInstanceQuery != null) {
      elementConfiguration.addDeploymentMappings(processInstanceQuery.listDeploymentIdMappings());
    }

    return elementConfiguration;
  }

  public List<String> getHistoricProcessInstanceIds() {
    return historicProcessInstanceIds;
  }

  protected void writeUserOperationLog(CommandContext commandContext, int numInstances) {
    List<PropertyChange> propertyChanges = new ArrayList<>();
    propertyChanges.add(new PropertyChange("nrOfInstances", null, numInstances));
    propertyChanges.add(new PropertyChange("async", null, true));
    propertyChanges.add(new PropertyChange("deleteReason", null, deleteReason));

    commandContext.getOperationLogManager()
        .logProcessInstanceOperation(UserOperationLogEntry.OPERATION_TYPE_DELETE_HISTORY,
            null,
            null,
            null,
            propertyChanges);
  }

  public BatchConfiguration getConfiguration(BatchElementConfiguration elementConfiguration) {
    return new BatchConfiguration(elementConfiguration.getIds(), elementConfiguration.getMappings(), false);
  }

}
