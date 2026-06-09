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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Tracks external task execution statistics for one client instance.
 */
public class ExternalTaskExecutionStats {

  protected final Map<String, TaskStats> statsMap = new ConcurrentHashMap<>();

  public void recordExecution(String processDefinitionKey, String topicName, long executionTimeMs) {
    String key = createKey(processDefinitionKey, topicName);
    statsMap.computeIfAbsent(key, k -> new TaskStats(processDefinitionKey, topicName))
        .recordExecution(executionTimeMs);
  }

  public void reset() {
    statsMap.values().forEach(TaskStats::reset);
  }

  public TaskStats getStats(String processDefinitionKey, String topicName) {
    return statsMap.get(createKey(processDefinitionKey, topicName));
  }

  public Map<String, TaskStats> getAllStats() {
    return new ConcurrentHashMap<>(statsMap);
  }

  public void clear() {
    statsMap.clear();
  }

  protected String createKey(String processDefinitionKey, String topicName) {
    return processDefinitionKey + ":" + topicName;
  }

  public static class TaskStats {

    protected final String processDefinitionKey;
    protected final String topicName;
    protected final LongAdder count = new LongAdder();
    protected final LongAdder totalTimeMs = new LongAdder();
    protected final AtomicLong minTimeMs = new AtomicLong(Long.MAX_VALUE);
    protected final AtomicLong maxTimeMs = new AtomicLong(Long.MIN_VALUE);

    public TaskStats(String processDefinitionKey, String topicName) {
      this.processDefinitionKey = processDefinitionKey;
      this.topicName = topicName;
    }

    void recordExecution(long executionTimeMs) {
      count.increment();
      totalTimeMs.add(executionTimeMs);
      updateMin(executionTimeMs);
      updateMax(executionTimeMs);
    }

    void reset() {
      count.reset();
      totalTimeMs.reset();
      minTimeMs.set(Long.MAX_VALUE);
      maxTimeMs.set(Long.MIN_VALUE);
    }

    protected void updateMin(long value) {
      long currentMin;
      do {
        currentMin = minTimeMs.get();
        if (value >= currentMin) {
          return;
        }
      } while (!minTimeMs.compareAndSet(currentMin, value));
    }

    protected void updateMax(long value) {
      long currentMax;
      do {
        currentMax = maxTimeMs.get();
        if (value <= currentMax) {
          return;
        }
      } while (!maxTimeMs.compareAndSet(currentMax, value));
    }

    public String getProcessDefinitionKey() {
      return processDefinitionKey;
    }

    public String getTopicName() {
      return topicName;
    }

    public long getCount() {
      return count.sum();
    }

    public long getTotalTimeMs() {
      return totalTimeMs.sum();
    }

    public long getMinTimeMs() {
      long min = minTimeMs.get();
      return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getMaxTimeMs() {
      long max = maxTimeMs.get();
      return max == Long.MIN_VALUE ? 0 : max;
    }

    public double getAverageTimeMs() {
      long currentCount = count.sum();
      return currentCount > 0 ? (double) totalTimeMs.sum() / currentCount : 0;
    }

    @Override
    public String toString() {
      return "TaskStats{" +
          "processDefinitionKey='" + processDefinitionKey + '\'' +
          ", topicName='" + topicName + '\'' +
          ", count=" + getCount() +
          ", totalTimeMs=" + getTotalTimeMs() +
          ", minTimeMs=" + getMinTimeMs() +
          ", maxTimeMs=" + getMaxTimeMs() +
          ", avgTimeMs=" + String.format("%.2f", getAverageTimeMs()) +
          '}';
    }
  }
}
