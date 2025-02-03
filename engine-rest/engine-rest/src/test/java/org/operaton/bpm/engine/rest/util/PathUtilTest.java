package org.operaton.bpm.engine.rest.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PathUtilTest {

  @Test
  public void testDecodePathParam() {
    // Test cases
    assertEquals("/path/to/resource", PathUtil.decodePathParam("%2Fpath%2Fto%2Fresource"));
    assertEquals("\\path\\to\\resource", PathUtil.decodePathParam("%5Cpath%5Cto%5Cresource"));
    assertEquals("/path\\to/resource", PathUtil.decodePathParam("%2Fpath%5Cto%2Fresource"));
    assertEquals("simplePath", PathUtil.decodePathParam("simplePath"));
  }
}