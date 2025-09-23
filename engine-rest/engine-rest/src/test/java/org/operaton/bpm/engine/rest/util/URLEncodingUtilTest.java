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