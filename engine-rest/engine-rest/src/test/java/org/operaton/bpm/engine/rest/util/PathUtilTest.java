package org.operaton.bpm.engine.rest.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathUtilTest {

  @Test
  void testDecodePathParam() {
    // Test cases
    assertThat(PathUtil.decodePathParam("%2Fpath%2Fto%2Fresource")).isEqualTo("/path/to/resource");
    assertThat(PathUtil.decodePathParam("%5Cpath%5Cto%5Cresource")).isEqualTo("\\path\\to\\resource");
    assertThat(PathUtil.decodePathParam("%2Fpath%5Cto%2Fresource")).isEqualTo("/path\\to/resource");
    assertThat(PathUtil.decodePathParam("simplePath")).isEqualTo("simplePath");
  }
}