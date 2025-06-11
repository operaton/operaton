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

import java.util.Date;
import java.util.List;
import org.operaton.bpm.engine.history.HistoricActivityStatistics;
import org.operaton.bpm.engine.history.HistoricActivityStatisticsQuery;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.interceptor.CommandExecutor;
import static org.operaton.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 *
 * @author Roman Smirnov
 *
 */
public class HistoricActivityStatisticsQueryImpl extends AbstractQuery<HistoricActivityStatisticsQuery, HistoricActivityStatistics> implements HistoricActivityStatisticsQuery {

  private static final long serialVersionUID = 1L;

  protected String processDefinitionId;

  protected boolean includeFinished;
  protected boolean includeCanceled;
  protected boolean includeCompleteScope;
  protected boolean includeIncidents;

  protected Date startedBefore;
  protected Date startedAfter;
  protected Date finishedBefore;
  protected Date finishedAfter;

  protected String[] processInstanceIds;

  public HistoricActivityStatisticsQueryImpl(String processDefinitionId, CommandExecutor commandExecutor) {
    super(commandExecutor);
    this.processDefinitionId = processDefinitionId;
  }

  @Override
  public HistoricActivityStatisticsQuery includeFinished() {
    includeFinished = true;
    return this;
  }

  @Override
  public HistoricActivityStatisticsQuery includeCanceled() {
    includeCanceled = true;
    return this;
  }

  @Override
  public HistoricActivityStatisticsQuery includeCompleteScope() {
    includeCompleteScope = true;
    return this;
  }

  @Override
  public HistoricActivityStatisticsQuery includeIncidents() {
    includeIncidents = true;
    return this;
  }

  @Override
  public HistoricActivityStatisticsQuery startedAfter(Date date) {
    startedAfter = date;
    return this;
  }

  @Override
  public HistoricActivityStatisticsQuery startedBefore(Date date) {
    startedBefore = date;
    return this;
  }

  @Override
  public HistoricActivityStatisticsQuery finishedAfter(Date date) {
    finishedAfter = date;
    return this;
  }

  @Override
  public HistoricActivityStatisticsQuery finishedBefore(Date date) {
    finishedBefore = date;
    return this;
  }

  @Override
  public HistoricActivityStatisticsQuery processInstanceIdIn(String... processInstanceIds) {
    ensureNotNull("processInstanceIds", (Object[]) processInstanceIds);
    this.processInstanceIds = processInstanceIds;
    return this;
  }

  @Override
  public HistoricActivityStatisticsQuery orderByActivityId() {
    return orderBy(HistoricActivityStatisticsQueryProperty.ACTIVITY_ID_);
  }

  @Override
  public long executeCount(CommandContext commandContext) {
    checkQueryOk();
    return
      commandContext
        .getHistoricStatisticsManager()
        .getHistoricStatisticsCountGroupedByActivity(this);
  }

  @Override
  public List<HistoricActivityStatistics> executeList(CommandContext commandContext, Page page) {
    checkQueryOk();
    return
      commandContext
        .getHistoricStatisticsManager()
        .getHistoricStatisticsGroupedByActivity(this, page);
  }

  @Override
  protected void checkQueryOk() {
    super.checkQueryOk();
    ensureNotNull("No valid process definition id supplied", "processDefinitionId", processDefinitionId);
  }

  // getters /////////////////////////////////////////////////

  public String getProcessDefinitionId() {
    return processDefinitionId;
  }

  public boolean isIncludeFinished() {
    return includeFinished;
  }

  public boolean isIncludeCanceled() {
    return includeCanceled;
  }

  public boolean isIncludeCompleteScope() {
    return includeCompleteScope;
  }

  public String[] getProcessInstanceIds() {
    return processInstanceIds;
  }

  public boolean isIncludeIncidents() {
    return includeIncidents;
  }

}
