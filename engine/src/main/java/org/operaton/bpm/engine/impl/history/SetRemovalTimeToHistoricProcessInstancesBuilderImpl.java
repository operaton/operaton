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
package org.operaton.bpm.engine.impl.history;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.history.HistoricProcessInstanceQuery;
import org.operaton.bpm.engine.history.SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder;
import org.operaton.bpm.engine.history.SetRemovalTimeToHistoricProcessInstancesBuilder;
import org.operaton.bpm.engine.impl.batch.removaltime.ProcessSetRemovalTimeJobHandler;
import org.operaton.bpm.engine.impl.cmd.batch.removaltime.SetRemovalTimeToHistoricProcessInstancesCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNull;

/**
 * @author Tassilo Weidner
 */
public class SetRemovalTimeToHistoricProcessInstancesBuilderImpl implements SetRemovalTimeSelectModeForHistoricProcessInstancesBuilder {

  protected HistoricProcessInstanceQuery query;
  protected List<String> ids;
  protected Date removalTime;
  protected Mode mode;
  protected boolean isHierarchical;
  protected boolean updateInChunks;
  protected Integer chunkSize;

  protected CommandExecutor commandExecutor;

  public SetRemovalTimeToHistoricProcessInstancesBuilderImpl(CommandExecutor commandExecutor) {
    this.commandExecutor = commandExecutor;
  }

  @Override
  public SetRemovalTimeToHistoricProcessInstancesBuilder byQuery(HistoricProcessInstanceQuery query) {
    this.query = query;
    return this;
  }

  @Override
  public SetRemovalTimeToHistoricProcessInstancesBuilder byIds(String... ids) {
    this.ids = ids !=  null ? Arrays.asList(ids) : null;
    return this;
  }

  @Override
  public SetRemovalTimeToHistoricProcessInstancesBuilder absoluteRemovalTime(Date removalTime) {
    ensureNull(BadUserRequestException.class, "The removal time modes are mutually exclusive","mode", mode);

    this.mode = Mode.ABSOLUTE_REMOVAL_TIME;
    this.removalTime = removalTime;
    return this;
  }

  @Override
  public SetRemovalTimeToHistoricProcessInstancesBuilder calculatedRemovalTime() {
    ensureNull(BadUserRequestException.class, "The removal time modes are mutually exclusive","mode", mode);

    this.mode = Mode.CALCULATED_REMOVAL_TIME;
    return this;
  }

  @Override
  public SetRemovalTimeToHistoricProcessInstancesBuilder clearedRemovalTime() {
    ensureNull(BadUserRequestException.class, "The removal time modes are mutually exclusive","mode", mode);

    this.mode = Mode.CLEARED_REMOVAL_TIME;
    return this;
  }

  @Override
  public SetRemovalTimeToHistoricProcessInstancesBuilder hierarchical() {
    isHierarchical = true;
    return this;
  }

  @Override
  public SetRemovalTimeToHistoricProcessInstancesBuilder updateInChunks() {
    updateInChunks = true;
    return this;
  }

  @Override
  public SetRemovalTimeToHistoricProcessInstancesBuilder chunkSize(int chunkSize) {
    if (chunkSize > ProcessSetRemovalTimeJobHandler.MAX_CHUNK_SIZE || chunkSize <= 0) {
      throw new BadUserRequestException("The value for chunk size should be between 1 and %s".formatted(
        ProcessSetRemovalTimeJobHandler.MAX_CHUNK_SIZE));
    }

    this.chunkSize = chunkSize;
    return this;
  }

  @Override
  public Batch executeAsync() {
    return commandExecutor.execute(new SetRemovalTimeToHistoricProcessInstancesCmd(this));
  }

  public HistoricProcessInstanceQuery getQuery() {
    return query;
  }

  public List<String> getIds() {
    return ids;
  }

  public Date getRemovalTime() {
    return removalTime;
  }

  public Mode getMode() {
    return mode;
  }

  public boolean isHierarchical() {
    return isHierarchical;
  }

  public boolean isUpdateInChunks() {
    return updateInChunks;
  }

  public Integer getChunkSize() {
    return chunkSize;
  }

  public enum Mode
  {
    CALCULATED_REMOVAL_TIME,
    ABSOLUTE_REMOVAL_TIME,
    CLEARED_REMOVAL_TIME
  }
}
