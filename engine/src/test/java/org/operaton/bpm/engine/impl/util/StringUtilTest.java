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
package org.operaton.bpm.engine.impl.util;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * @author Tobias Metzke
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class StringUtilTest {

  ProcessEngine processEngine;

  @Test
  void shouldAllowTrimToMaximumLength() {
    // given
    String fittingThreeByteMessage = repeatCharacter("\u9faf", StringUtil.DB_MAX_STRING_LENGTH);
    String exceedingMessage = repeatCharacter("a", StringUtil.DB_MAX_STRING_LENGTH * 2);

    // then
    assertThat(fittingThreeByteMessage.substring(0, StringUtil.DB_MAX_STRING_LENGTH)).isEqualTo(StringUtil.trimToMaximumLengthAllowed(fittingThreeByteMessage));
    assertThat(exceedingMessage.substring(0, StringUtil.DB_MAX_STRING_LENGTH)).isEqualTo(StringUtil.trimToMaximumLengthAllowed(exceedingMessage));
  }

  @Test
  void shouldConvertByteArrayToString() {
    // given
    String message = "This is a message string";
    byte[] bytes = message.getBytes();

    // when
    String stringFromBytes = StringUtil.fromBytes(bytes, processEngine);

    // then
    assertThat(stringFromBytes).isEqualTo(message);
  }

  @Test
  void shouldConvertNullByteArrayToEmptyString() {
    // given
    byte[] bytes = null;

    // when
    String stringFromBytes = StringUtil.fromBytes(bytes, processEngine);

    // then
    assertThat(stringFromBytes).isEmpty();
  }

  protected static String repeatCharacter(String encodedCharacter, int numCharacters) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < numCharacters; i++) {
      sb.append(encodedCharacter);
    }
    return sb.toString();
  }

  @ParameterizedTest
  @MethodSource("isExpressionArgs")
  void isExpression_shouldDetectExpressionSyntax(String input, boolean expected) {
    assertThat(StringUtil.isExpression(input)).isEqualTo(expected);
  }

  static Stream<Arguments> isExpressionArgs() {
    return Stream.of(
      arguments("${foo}",     true),
      arguments("#{foo}",     true),
      arguments("  ${foo}  ", true),
      arguments("  #{bar}  ", true),
      arguments("literal",    false),
      arguments("{foo}",      false),
      arguments("",           false)
    );
  }

  @Test
  void isExpression_shouldThrowOnNullInput() {
    assertThatThrownBy(() -> StringUtil.isExpression(null))
      .isInstanceOf(NullPointerException.class);
  }

  @ParameterizedTest
  @MethodSource("splitArgs")
  void split_shouldHandleVariousInputs(String text, String regex, String[] expected) {
    assertThat(StringUtil.split(text, regex)).isEqualTo(expected);
  }

  static Stream<Arguments> splitArgs() {
    return Stream.of(
      arguments("a,b,c",     ",",  new String[]{"a", "b", "c"}),
      arguments("a , b , c", ",",  new String[]{"a", "b", "c"}),
      arguments(null,        ",",  new String[0]),
      arguments("abc",       null, new String[]{"abc"})
    );
  }

  @ParameterizedTest
  @MethodSource("hasTextArgs")
  void hasText_shouldReturnTrueOnlyForNonEmptyStrings(String input, boolean expected) {
    assertThat(StringUtil.hasText(input)).isEqualTo(expected);
  }

  static Stream<Arguments> hasTextArgs() {
    return Stream.of(
      arguments(null,    false),
      arguments("",     false),
      arguments("hi",   true),
      arguments("  ",   true)
    );
  }

  @Test
  void hasAnySuffix_shouldMatchKnownSuffixes() {
    String[] suffixes = {".bpmn", ".xml"};
    assertThat(StringUtil.hasAnySuffix("process.bpmn", suffixes)).isTrue();
    assertThat(StringUtil.hasAnySuffix("process.xml",  suffixes)).isTrue();
    assertThat(StringUtil.hasAnySuffix("process.txt",  suffixes)).isFalse();
  }

  @Test
  void join_withIterator_shouldJoinWithCommaSpace() {
    Iterator<String> iterator = List.of("a", "b", "c").iterator();
    assertThat(StringUtil.join(iterator)).isEqualTo("a, b, c");
  }

  @Test
  void join_withSingleElementIterator_shouldReturnElementWithoutDelimiter() {
    Iterator<String> iterator = List.of("only").iterator();
    assertThat(StringUtil.join(iterator)).isEqualTo("only");
  }

  @Test
  void join_withEmptyIterator_shouldReturnEmptyString() {
    Iterator<String> iterator = List.<String>of().iterator();
    assertThat(StringUtil.join(iterator)).isEmpty();
  }
}
