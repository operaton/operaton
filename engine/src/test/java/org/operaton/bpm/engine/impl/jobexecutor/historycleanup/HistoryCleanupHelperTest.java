package org.operaton.bpm.engine.impl.jobexecutor.historycleanup;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    assertThrows(IllegalArgumentException.class, () -> HistoryCleanupHelper.listMinuteChunks(0));
    assertThrows(IllegalArgumentException.class, () -> HistoryCleanupHelper.listMinuteChunks(61));
  }
}
