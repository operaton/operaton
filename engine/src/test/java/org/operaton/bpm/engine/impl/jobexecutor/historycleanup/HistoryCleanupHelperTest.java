/*
 * Copyright 2025 the Operaton contributors.
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
 * 
 */
package org.operaton.bpm.engine.impl.jobexecutor.historycleanup;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class HistoryCleanupHelperTest {
  @Test
  void testlistMinuteChunks(){
    assertThat(
      HistoryCleanupHelper.listMinuteChunks(1))
    .isEqualTo(new int[][] {
      {0, 59}
    });

    assertThat(
      HistoryCleanupHelper.listMinuteChunks(2))
    .isEqualTo(new int[][] {
      {0, 29},
      {30, 59}
    });
  }

  @Test
  void testlistMinuteChunksInvalidArguments(){
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> HistoryCleanupHelper.listMinuteChunks(0));
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> HistoryCleanupHelper.listMinuteChunks(61));
  }
}
