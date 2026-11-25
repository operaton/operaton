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
package org.operaton.bpm.engine.impl.cfg;

import java.text.ParseException;
import java.util.Date;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.operaton.bpm.engine.impl.ProcessEngineLogger;

import org.jspecify.annotations.Nullable;
import org.operaton.bpm.engine.impl.jobexecutor.historycleanup.HistoryCleanupHelper;

/**
 * @author Svetlana Dorokhova.
 */
public @NullMarked class BatchWindowConfiguration {

  protected static final ConfigurationLogger LOG = ProcessEngineLogger.CONFIG_LOGGER;

  private @Nullable String startTime;

  private @Nullable Date startTimeAsDate;

  private @Nullable String endTime = "00:00";

  private @Nullable Date endTimeAsDate;

  public BatchWindowConfiguration() {
  }

  public BatchWindowConfiguration(@Nullable String startTime, @Nullable String endTime) {
    this.startTime = startTime;
    initStartTimeAsDate();
    if (endTime != null) {
      this.endTime = endTime;
    }
    initEndTimeAsDate();
  }

  private void initStartTimeAsDate() {
    try {
      startTimeAsDate = HistoryCleanupHelper.parseTimeConfiguration(startTime);
    } catch (ParseException e) {
      throw LOG.invalidPropertyValue("startTime", startTime);
    }
  }

  private void initEndTimeAsDate() {
    try {
      endTimeAsDate = HistoryCleanupHelper.parseTimeConfiguration(endTime);
    } catch (ParseException e) {
      throw LOG.invalidPropertyValue("endTime", endTime);
    }
  }

  public @Nullable String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
    initStartTimeAsDate();
  }

  public @Nullable String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
    initEndTimeAsDate();
  }

  public @Nullable Date getStartTimeAsDate() {
    return startTimeAsDate;
  }

  public @Nullable Date getEndTimeAsDate() {
    return endTimeAsDate;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BatchWindowConfiguration that = (BatchWindowConfiguration) o;

    if (!Objects.equals(startTime, that.startTime)) {
      return false;
    }
    return Objects.equals(endTime, that.endTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startTime, endTime);
  }
}
