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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalTaskExecutionStatsTest {

  @Test
  void recordsAndResetsExecutionStats() {
    ExternalTaskExecutionStats stats = new ExternalTaskExecutionStats();

    stats.recordExecution("process", "topic", 10L);
    stats.recordExecution("process", "topic", 20L);

    ExternalTaskExecutionStats.TaskStats taskStats = stats.getStats("process", "topic");
    assertThat(taskStats.getCount()).isEqualTo(2);
    assertThat(taskStats.getTotalTimeMs()).isEqualTo(30L);
    assertThat(taskStats.getMinTimeMs()).isEqualTo(10L);
    assertThat(taskStats.getMaxTimeMs()).isEqualTo(20L);
    assertThat(taskStats.getAverageTimeMs()).isEqualTo(15.0);

    stats.reset();

    assertThat(taskStats.getCount()).isZero();
    assertThat(taskStats.getTotalTimeMs()).isZero();
    assertThat(taskStats.getMinTimeMs()).isZero();
    assertThat(taskStats.getMaxTimeMs()).isZero();
    assertThat(taskStats.getAverageTimeMs()).isZero();
  }
}
