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
package org.operaton.bpm.engine.impl.cmd.batch.removaltime;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotEmpty;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

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
import org.operaton.bpm.engine.impl.batch.removaltime.SetRemovalTimeBatchConfiguration;
import org.operaton.bpm.engine.impl.history.SetRemovalTimeToHistoricProcessInstancesBuilderImpl;
import org.operaton.bpm.engine.impl.history.SetRemovalTimeToHistoricProcessInstancesBuilderImpl.Mode;
import org.operaton.bpm.engine.impl.interceptor.Command;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.persistence.entity.PropertyChange;
import org.operaton.bpm.engine.impl.util.CollectionUtil;

/**
 * @author Tassilo Weidner
 */
public class SetRemovalTimeToHistoricProcessInstancesCmd implements Command<Batch> {

  protected SetRemovalTimeToHistoricProcessInstancesBuilderImpl builder;

  public SetRemovalTimeToHistoricProcessInstancesCmd(SetRemovalTimeToHistoricProcessInstancesBuilderImpl builder) {
    this.builder = builder;
  }

  @Override
  public Batch execute(CommandContext commandContext) {
    if (builder.getQuery() == null && builder.getIds() == null) {
      throw new BadUserRequestException("Neither query nor ids provided.");
    }

    BatchElementConfiguration elementConfiguration = collectInstances(commandContext);

    ensureNotNull(BadUserRequestException.class, "removalTime", builder.getMode());
    ensureNotEmpty(BadUserRequestException.class, "historicProcessInstances", elementConfiguration.getIds());

    return new BatchBuilder(commandContext)
        .type(Batch.TYPE_PROCESS_SET_REMOVAL_TIME)
        .config(getConfiguration(elementConfiguration))
        .permission(BatchPermissions.CREATE_BATCH_SET_REMOVAL_TIME)
        .operationLogHandler(this::writeUserOperationLog)
        .build();
  }

  protected BatchElementConfiguration collectInstances(CommandContext commandContext) {
    BatchElementConfiguration elementConfiguration = new BatchElementConfiguration();
    HistoricProcessInstanceQuery instanceQuery = builder.getQuery();
    if (instanceQuery != null) {
      elementConfiguration.addDeploymentMappings(((HistoricProcessInstanceQueryImpl) instanceQuery).listDeploymentIdMappings());
    }

    List<String> instanceIds = builder.getIds();
    if (!CollectionUtil.isEmpty(instanceIds)) {
      HistoricProcessInstanceQueryImpl query = new HistoricProcessInstanceQueryImpl();
      query.processInstanceIds(new HashSet<>(instanceIds));
      elementConfiguration.addDeploymentMappings(commandContext.runWithoutAuthorization(query::listDeploymentIdMappings));
    }
    return elementConfiguration;
  }

  protected BatchConfiguration getConfiguration(BatchElementConfiguration elementConfiguration) {
    return new SetRemovalTimeBatchConfiguration(elementConfiguration.getIds(), elementConfiguration.getMappings())
        .setHierarchical(builder.isHierarchical())
        .setHasRemovalTime(hasRemovalTime())
        .setRemovalTime(builder.getRemovalTime())
        .setUpdateInChunks(builder.isUpdateInChunks())
        .setChunkSize(builder.getChunkSize());
  }

  protected boolean hasRemovalTime() {
    return builder.getMode() == Mode.ABSOLUTE_REMOVAL_TIME ||
      builder.getMode() == Mode.CLEARED_REMOVAL_TIME;
  }

  protected void writeUserOperationLog(CommandContext commandContext, int numInstances) {
    List<PropertyChange> propertyChanges = new ArrayList<>();
    propertyChanges.add(new PropertyChange("mode", null, builder.getMode()));
    propertyChanges.add(new PropertyChange("removalTime", null, builder.getRemovalTime()));
    propertyChanges.add(new PropertyChange("hierarchical", null, builder.isHierarchical()));
    propertyChanges.add(new PropertyChange("nrOfInstances", null, numInstances));
    propertyChanges.add(new PropertyChange("async", null, true));
    propertyChanges.add(new PropertyChange("updateInChunks", null, builder.isUpdateInChunks()));
    propertyChanges.add(new PropertyChange("chunkSize", null, builder.getChunkSize()));

    commandContext.getOperationLogManager()
      .logProcessInstanceOperation(UserOperationLogEntry.OPERATION_TYPE_SET_REMOVAL_TIME, propertyChanges);
  }

}
