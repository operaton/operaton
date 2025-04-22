package org.operaton.bpm.engine.impl.jobexecutor.historycleanup;

import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class HistoryCleanupHelperTest {
  @Test
  public void testlistMinuteChunks(){
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
  public void testlistMinuteChunksInvalidArguments(){
    assertThrows(IllegalArgumentException.class, () -> HistoryCleanupHelper.listMinuteChunks(0));
    assertThrows(IllegalArgumentException.class, () -> HistoryCleanupHelper.listMinuteChunks(61));
  }
}
