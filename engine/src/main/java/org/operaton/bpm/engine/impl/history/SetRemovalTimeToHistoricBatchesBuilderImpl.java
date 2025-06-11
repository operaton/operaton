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

import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.batch.Batch;
import org.operaton.bpm.engine.batch.history.HistoricBatchQuery;
import org.operaton.bpm.engine.history.SetRemovalTimeSelectModeForHistoricBatchesBuilder;
import org.operaton.bpm.engine.history.SetRemovalTimeToHistoricBatchesBuilder;
import org.operaton.bpm.engine.impl.cmd.batch.removaltime.SetRemovalTimeToHistoricBatchesCmd;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNull;

/**
 * @author Tassilo Weidner
 */
public class SetRemovalTimeToHistoricBatchesBuilderImpl implements SetRemovalTimeSelectModeForHistoricBatchesBuilder {

  protected HistoricBatchQuery query;
  protected List<String> ids;
  protected Mode mode;
  protected Date removalTime;

  protected CommandExecutor commandExecutor;

  public SetRemovalTimeToHistoricBatchesBuilderImpl(CommandExecutor commandExecutor) {
    mode = null;
    this.commandExecutor = commandExecutor;
  }

  @Override
  public SetRemovalTimeToHistoricBatchesBuilder byQuery(HistoricBatchQuery query) {
    this.query = query;
    return this;
  }

  @Override
  public SetRemovalTimeToHistoricBatchesBuilder byIds(String... ids) {
    this.ids = ids != null ? Arrays.asList(ids) : null;
    return this;
  }

  @Override
  public SetRemovalTimeToHistoricBatchesBuilder absoluteRemovalTime(Date removalTime) {
    ensureNull(BadUserRequestException.class, "The removal time modes are mutually exclusive","mode", mode);

    this.mode = Mode.ABSOLUTE_REMOVAL_TIME;
    this.removalTime = removalTime;
    return this;
  }

  @Override
  public SetRemovalTimeToHistoricBatchesBuilder calculatedRemovalTime() {
    ensureNull(BadUserRequestException.class, "The removal time modes are mutually exclusive","mode", mode);

    this.mode = Mode.CALCULATED_REMOVAL_TIME;
    return this;
  }

  @Override
  public SetRemovalTimeToHistoricBatchesBuilder clearedRemovalTime() {
    ensureNull(BadUserRequestException.class, "The removal time modes are mutually exclusive","mode", mode);

    mode = Mode.CLEARED_REMOVAL_TIME;
    return this;
  }

  @Override
  public Batch executeAsync() {
    return commandExecutor.execute(new SetRemovalTimeToHistoricBatchesCmd(this));
  }

  public HistoricBatchQuery getQuery() {
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

  public enum Mode
  {
    CALCULATED_REMOVAL_TIME,
    ABSOLUTE_REMOVAL_TIME,
    CLEARED_REMOVAL_TIME;
  }

}
