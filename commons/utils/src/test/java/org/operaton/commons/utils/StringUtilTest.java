/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.commons.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.operaton.commons.utils.StringUtil.defaultString;
import static org.operaton.commons.utils.StringUtil.getStackTrace;
import static org.operaton.commons.utils.StringUtil.isExpression;
import static org.operaton.commons.utils.StringUtil.join;
import static org.operaton.commons.utils.StringUtil.split;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastian Menski
 */
class StringUtilTest {

  @Test
  void expressionDetection() {
    assertThat(isExpression("${test}")).isTrue();
    assertThat(isExpression("${a(b,c)}")).isTrue();
    assertThat(isExpression("${ test }")).isTrue();
    assertThat(isExpression(" ${test} ")).isTrue();
    assertThat(isExpression(" \n${test} ")).isTrue();

    assertThat(isExpression("#{test}")).isTrue();
    assertThat(isExpression("#{a(b,c)}")).isTrue();
    assertThat(isExpression("#{ test }")).isTrue();
    assertThat(isExpression(" #{test} ")).isTrue();
    assertThat(isExpression(" \n#{test} ")).isTrue();

    assertThat(isExpression("test")).isFalse();
    assertThat(isExpression("    test")).isFalse();
    assertThat(isExpression("{test}")).isFalse();
    assertThat(isExpression("(test)")).isFalse();
    assertThat(isExpression("")).isFalse();
    assertThat(isExpression(null)).isFalse();
  }

  @Test
  void stringSplit() {
    assertThat(split("a,b,c", ",")).hasSize(3).containsExactly("a", "b", "c");
    assertThat(split("aaaxbaaxc", "a{2}x")).hasSize(3).containsExactly("a", "b", "c");
    assertThat(split(null, ",")).isNull();
    assertThat(split("abc", ",")).hasSize(1).containsExactly("abc");
    assertThat(split("a,b,c", null)).hasSize(1).containsExactly("a,b,c");
  }

  @Test
  void stringJoin() {
    assertThat(join(",", "a", "b", "c")).isEqualTo("a,b,c");
    assertThat(join(", ", "a", "b", "c")).isEqualTo("a, b, c");
    assertThat(join(null, "a", "b", "c")).isEqualTo("abc");
    assertThat(join(",", "")).isEmpty();
    assertThat(join(null, (String[]) null)).isNull();
    assertThat(join("aax", "a", "b", "c")).isEqualTo("aaaxbaaxc");
  }

  @Test
  void testDefaultString() {
    assertThat(defaultString(null)).isEmpty();
    assertThat(defaultString("")).isEmpty();
    assertThat(defaultString("bat")).isEqualTo("bat");
  }

  @ParameterizedTest
  @MethodSource("sanitizeString_args")
  void sanitizeString(String input, String expected) {
    assertThat(StringUtil.sanitize(input)).isEqualTo(expected);
  }

  static Stream<Arguments> sanitizeString_args () {
    return Stream.of(
      arguments(null, "(null)"),
      arguments("", ""),
      arguments("Hello World!", "Hello World!"),
      arguments("Hello\nWorld!", "Hello\\nWorld!"),
      arguments("Hello\rWorld!", "Hello\\rWorld!"),
      arguments("Hello\r\nWorld!", "Hello\\r\\nWorld!"),
      arguments("Hello\tWorld!", "Hello\\tWorld!")
    );
  }

  @Test
  void getStacktrace() {
    Throwable th = new IllegalArgumentException("Wrong argument!", new NullPointerException("This shouldn't have been empty"));
    assertThat(getStackTrace(th)).containsSubsequence("java.lang.IllegalArgumentException: Wrong argument!",
      "at org.operaton.commons.utils.StringUtilTest.getStacktrace(StringUtilTest.java:",
      "Caused by: java.lang.NullPointerException: This shouldn't have been empty");
  }

}
