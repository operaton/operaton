package org.operaton.bpm.engine.rest.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class URLEncodingUtilTest {

  @Test
  public void testEncode() {
    assertEquals("simple%20text", URLEncodingUtil.encode("simple text"));
    assertEquals("%2Fpath%2Fto%2Fresource", URLEncodingUtil.encode("/path/to/resource"));
    assertEquals("%5Cpath%5Cto%5Cresource", URLEncodingUtil.encode("\\path\\to\\resource"));
    assertEquals("special%20characters%20%26%20symbols", URLEncodingUtil.encode("special characters & symbols"));
  }

  @Test
  public void testBuildAttachmentValue() {
    String fileName = "example file.txt";
    String expected = "attachment; filename=\"example file.txt\"; filename*=UTF-8''example%20file.txt";
    assertEquals(expected, URLEncodingUtil.buildAttachmentValue(fileName));
  }
}