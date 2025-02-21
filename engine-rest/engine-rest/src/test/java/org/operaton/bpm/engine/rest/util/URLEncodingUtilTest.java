package org.operaton.bpm.engine.rest.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class URLEncodingUtilTest {

  @Test
  void testEncode() {
    assertThat(URLEncodingUtil.encode("simple text")).isEqualTo("simple%20text");
    assertThat(URLEncodingUtil.encode("/path/to/resource")).isEqualTo("%2Fpath%2Fto%2Fresource");
    assertThat(URLEncodingUtil.encode("\\path\\to\\resource")).isEqualTo("%5Cpath%5Cto%5Cresource");
    assertThat(URLEncodingUtil.encode("special characters & symbols")).isEqualTo("special%20characters%20%26%20symbols");
  }

  @Test
  void testBuildAttachmentValue() {
    String fileName = "example file.txt";
    String expected = "attachment; filename=\"example file.txt\"; filename*=UTF-8''example%20file.txt";
    assertThat(URLEncodingUtil.buildAttachmentValue(fileName)).isEqualTo(expected);
  }
}