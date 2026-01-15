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
package org.operaton.bpm.engine.impl.jobexecutor.historycleanup;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.operaton.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.operaton.bpm.engine.impl.context.Context;
import org.operaton.bpm.engine.impl.interceptor.CommandContext;
import org.operaton.bpm.engine.impl.util.ClockUtil;

/**
 * @author Svetlana Dorokhova.
 */
public final class HistoryCleanupHelper {

  private static final DateTimeFormatter TIME_FORMAT_WITHOUT_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mm");

  private static final DateTimeFormatter TIME_FORMAT_WITHOUT_SECONDS_WITH_TIMEZONE = DateTimeFormatter.ofPattern("yyyy-MM-ddHH:mmZ");

  private static final DateTimeFormatter DATE_FORMAT_WITHOUT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private HistoryCleanupHelper () {
  }

  /**
   * Returns the max retries used for cleanup jobs. If the configuration is null, the default value used will be
   * defaultNumberOfRetries, the configuration used for all jobs.
   *
   * @return the effective max number of retries
   */
  public static int getMaxRetries() {
    ProcessEngineConfigurationImpl config = Context.getProcessEngineConfiguration();
    boolean isConfiguredByUser = config.getHistoryCleanupDefaultNumberOfRetries() != Integer.MIN_VALUE;

    if (!isConfiguredByUser) {
      return config.getDefaultNumberOfRetries();
    }

    return config.getHistoryCleanupDefaultNumberOfRetries();
  }

  /**
   * Checks if given date is within a batch window. Batch window start time is checked inclusively.
   * @param date
   * @return
   */
  public static boolean isWithinBatchWindow(Date date, ProcessEngineConfigurationImpl configuration) {
    if (configuration.getBatchWindowManager().isBatchWindowConfigured(configuration)) {
      BatchWindow batchWindow = configuration.getBatchWindowManager().getCurrentOrNextBatchWindow(date, configuration);
      if (batchWindow == null) {
        return false;
      }
      return batchWindow.isWithin(date);
    } else {
      return false;
    }
  }

  public static Date parseTimeConfiguration(String time) throws ParseException {
    LocalDate today = ClockUtil.getCurrentTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    String todayString = DATE_FORMAT_WITHOUT_TIME.format(today);
    try {
      ZonedDateTime parsedDateTime = ZonedDateTime.parse(todayString + time, TIME_FORMAT_WITHOUT_SECONDS_WITH_TIMEZONE);
      return Date.from(parsedDateTime.toInstant());
    } catch (DateTimeParseException ex) {
      try {
        LocalDateTime parsedDateTime = LocalDateTime.parse(todayString + time, TIME_FORMAT_WITHOUT_SECONDS);
        return Date.from(parsedDateTime.atZone(ZoneId.systemDefault()).toInstant());
      } catch (DateTimeParseException e) {
        throw new ParseException(e.getMessage(), e.getErrorIndex());
      }
    }
  }

  private static Integer getHistoryCleanupBatchSize(CommandContext commandContext) {
    return commandContext.getProcessEngineConfiguration().getHistoryCleanupBatchSize();
  }

  /**
   * Creates next batch object for history cleanup. First searches for historic process instances ready for cleanup. If there is still some place left in batch (configured batch
   * size was not reached), searches for historic decision instances and also adds them to the batch. Then if there is still some place left in batch, searches for historic case
   * instances and historic batches - and adds them to the batch.
   *
   * @param commandContext
   * @return
   */
  public static void prepareNextBatch(HistoryCleanupBatch historyCleanupBatch, CommandContext commandContext) {
    final HistoryCleanupJobHandlerConfiguration configuration = historyCleanupBatch.getConfiguration();
    final Integer batchSize = getHistoryCleanupBatchSize(commandContext);
    ProcessEngineConfigurationImpl processEngineConfiguration = commandContext.getProcessEngineConfiguration();

    //add process instance ids
    final List<String> historicProcessInstanceIds = commandContext.getHistoricProcessInstanceManager()
        .findHistoricProcessInstanceIdsForCleanup(batchSize, configuration.getMinuteFrom(), configuration.getMinuteTo());
    if (!historicProcessInstanceIds.isEmpty()) {
      historyCleanupBatch.setHistoricProcessInstanceIds(historicProcessInstanceIds);
    }

    //if batch is not full, add decision instance ids
    if (historyCleanupBatch.size() < batchSize && processEngineConfiguration.isDmnEnabled()) {
      final List<String> historicDecisionInstanceIds = commandContext.getHistoricDecisionInstanceManager()
          .findHistoricDecisionInstanceIdsForCleanup(batchSize - historyCleanupBatch.size(), configuration.getMinuteFrom(), configuration.getMinuteTo());
      if (!historicDecisionInstanceIds.isEmpty()) {
        historyCleanupBatch.setHistoricDecisionInstanceIds(historicDecisionInstanceIds);
      }
    }

    //if batch is not full, add case instance ids
    if (historyCleanupBatch.size() < batchSize && processEngineConfiguration.isCmmnEnabled()) {
      final List<String> historicCaseInstanceIds = commandContext.getHistoricCaseInstanceManager()
          .findHistoricCaseInstanceIdsForCleanup(batchSize - historyCleanupBatch.size(), configuration.getMinuteFrom(), configuration.getMinuteTo());
      if (!historicCaseInstanceIds.isEmpty()) {
        historyCleanupBatch.setHistoricCaseInstanceIds(historicCaseInstanceIds);
      }
    }

    //if batch is not full, add batch ids
    Map<String, Integer> batchOperationsForHistoryCleanup = processEngineConfiguration.getParsedBatchOperationsForHistoryCleanup();
    if (historyCleanupBatch.size() < batchSize && batchOperationsForHistoryCleanup != null && !batchOperationsForHistoryCleanup.isEmpty()) {
      List<String> historicBatchIds = commandContext
          .getHistoricBatchManager()
          .findHistoricBatchIdsForCleanup(batchSize - historyCleanupBatch.size(), batchOperationsForHistoryCleanup, configuration.getMinuteFrom(), configuration.getMinuteTo());
      if (!historicBatchIds.isEmpty()) {
        historyCleanupBatch.setHistoricBatchIds(historicBatchIds);
      }
    }

    //if batch is not full, add task metric ids
    Integer parsedTaskMetricsTimeToLive = processEngineConfiguration.getParsedTaskMetricsTimeToLive();
    if (parsedTaskMetricsTimeToLive != null && historyCleanupBatch.size() < batchSize) {
      final List<String> taskMetricIds = commandContext.getMeterLogManager()
          .findTaskMetricsForCleanup(batchSize - historyCleanupBatch.size(), parsedTaskMetricsTimeToLive, configuration.getMinuteFrom(), configuration.getMinuteTo());
      if (!taskMetricIds.isEmpty()) {
        historyCleanupBatch.setTaskMetricIds(taskMetricIds);
      }
    }
  }

  public static int[][] listMinuteChunks(int numberOfChunks) throws IllegalArgumentException {
    if(numberOfChunks <= 0 || numberOfChunks > 60){
      throw new IllegalArgumentException("Number of chunks must be greater than 0, but is %s".formatted(numberOfChunks));
    }
    final int[][] minuteChunks = new int[numberOfChunks][2];
    int chunkLength = 60 / numberOfChunks;
    for (int i = 0; i < numberOfChunks; i++) {
      minuteChunks[i][0] = chunkLength * i;
      minuteChunks[i][1] = chunkLength * (i + 1) - 1;
    }
    minuteChunks[numberOfChunks - 1][1] = 59;
    return minuteChunks;
  }

  public static boolean isBatchWindowConfigured(CommandContext commandContext) {
    return commandContext.getProcessEngineConfiguration().getBatchWindowManager().isBatchWindowConfigured(commandContext.getProcessEngineConfiguration());
  }
}
