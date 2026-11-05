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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.impl.cmd.UpdateProcessInstancesSuspendStateBatchCmd;
import org.operaton.bpm.engine.impl.cmd.UpdateProcessInstancesSuspendStateCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import org.operaton.bpm.engine.runtime.ProcessInstanceQuery;
import org.operaton.bpm.engine.runtime.UpdateProcessInstancesSuspensionStateBuilder;

import static java.util.Objects.requireNonNull;

public @NullMarked class UpdateProcessInstancesSuspensionStateBuilderImpl implements UpdateProcessInstancesSuspensionStateBuilder {

  protected List<String> processInstanceIds;
  protected @Nullable ProcessInstanceQuery processInstanceQuery;
  protected @Nullable HistoricProcessInstanceQuery historicProcessInstanceQuery;
  protected @Nullable CommandExecutor commandExecutor;
  protected @Nullable String processDefinitionId;

  public UpdateProcessInstancesSuspensionStateBuilderImpl(CommandExecutor commandExecutor) {
    this.processInstanceIds = new ArrayList<>();
    this.commandExecutor = commandExecutor;
  }

  public UpdateProcessInstancesSuspensionStateBuilderImpl(List<String> processInstanceIds) {
    this.processInstanceIds = processInstanceIds;
  }

  private CommandExecutor getCommandExecutor() {
    return requireNonNull(commandExecutor);
  }

  @Override
  public UpdateProcessInstancesSuspensionStateBuilder byProcessInstanceIds(List<String> processInstanceIds) {
    this.processInstanceIds.addAll(processInstanceIds);
    return this;
  }

  @Override
  public UpdateProcessInstancesSuspensionStateBuilder byProcessInstanceIds(String... processInstanceIds) {
    this.processInstanceIds.addAll(Arrays.asList(processInstanceIds));
    return this;
  }

  @Override
  public UpdateProcessInstancesSuspensionStateBuilder byProcessInstanceQuery(ProcessInstanceQuery processInstanceQuery) {
    this.processInstanceQuery = processInstanceQuery;
    return this;
  }

  @Override
  public UpdateProcessInstancesSuspensionStateBuilder byHistoricProcessInstanceQuery(HistoricProcessInstanceQuery historicProcessInstanceQuery) {
    this.historicProcessInstanceQuery = historicProcessInstanceQuery;
    return this;
  }

  @Override
  public void suspend() {
    getCommandExecutor().execute(new UpdateProcessInstancesSuspendStateCmd(getCommandExecutor(), this, true));
  }

  @Override
  public void activate() {
    getCommandExecutor().execute(new UpdateProcessInstancesSuspendStateCmd(getCommandExecutor(), this, false));
  }

  @Override
  public Batch suspendAsync() {
    return getCommandExecutor().execute(new UpdateProcessInstancesSuspendStateBatchCmd(getCommandExecutor(), this, true));
  }

  @Override
  public Batch activateAsync() {
    return getCommandExecutor().execute(new UpdateProcessInstancesSuspendStateBatchCmd(getCommandExecutor(), this, false));
  }

  public List<String> getProcessInstanceIds() {
    return processInstanceIds;
  }

  public @Nullable ProcessInstanceQuery getProcessInstanceQuery() {
    return processInstanceQuery;
  }

  public @Nullable HistoricProcessInstanceQuery getHistoricProcessInstanceQuery() {
    return historicProcessInstanceQuery;
  }

}
