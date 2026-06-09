/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.client.topic.impl;

import java.util.Map;

import org.operaton.bpm.client.topic.impl.ExternalTaskExecutionStats.TaskStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs collected external task execution statistics.
 */
public final class ExternalTaskExecutionStatsLogger {

  protected static final Logger LOGGER = LoggerFactory.getLogger(ExternalTaskExecutionStatsLogger.class);
  public static final String COLUMNS_HEADER = String.format("%-40s %-30s %8s %10s %10s %10s %10s%n",
      "Process Definition Key", "Topic Name", "Count", "Total(ms)", "Min(ms)", "Max(ms)", "Avg(ms)");
  public static final String STATS_DATA_FORMAT = "%-40s %-30s %8d %10d %10d %10d %10.2f%n";

  private ExternalTaskExecutionStatsLogger() {
  }

  public static void logStats(ExternalTaskExecutionStats stats) {
    Map<String, TaskStats> allStats = stats.getAllStats();

    if (allStats.isEmpty()) {
      LOGGER.info("No execution statistics available");
      return;
    }

    StringBuilder builder = new StringBuilder();
    builder.append("\n=== External Task Execution Statistics ===\n");
    builder.append(COLUMNS_HEADER);
    builder.append("-".repeat(130)).append("\n");

    allStats.values().stream()
        .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
        .forEach(taskStats -> builder.append(String.format(STATS_DATA_FORMAT,
            truncate(taskStats.getProcessDefinitionKey(), 40),
            truncate(taskStats.getTopicName(), 30),
            taskStats.getCount(),
            taskStats.getTotalTimeMs(),
            taskStats.getMinTimeMs(),
            taskStats.getMaxTimeMs(),
            taskStats.getAverageTimeMs())));

    builder.append("=".repeat(130)).append("\n");
    LOGGER.info("{}", builder);
  }

  protected static String truncate(String value, int maxLength) {
    if (value == null) {
      return "null";
    }
    return value.length() > maxLength ? value.substring(0, maxLength - 3) + "..." : value;
  }
}
