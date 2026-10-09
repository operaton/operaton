/*
 * Copyright 2026 the Operaton contributors.
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
 */
package org.operaton.bpm.dmn.feel.impl.juel.transform;

import java.util.regex.Matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.operaton.bpm.dmn.feel.impl.juel.FeelSyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntervalTransformerTest {

  protected IntervalTransformer intervalTransformer;
  protected FeelToJuelTransform feelToJuelTransform;

  @BeforeEach
  void init() {
    intervalTransformer = new IntervalTransformer();
    feelToJuelTransform = new FeelToJuelTransformImpl();
  }

  @ParameterizedTest
  @ValueSource(strings = {"[0..12]", "(0..12)", "]0..12]"})
  void canTransformAcceptsIntervalStartSymbols(String feelExpression) {
    assertThat(intervalTransformer.canTransform(feelExpression)).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"12", "x", ")0..12]", "not(0..12)"})
  void canTransformRejectsNonIntervalExpressions(String feelExpression) {
    assertThat(intervalTransformer.canTransform(feelExpression)).isFalse();
  }

  @ParameterizedTest
  @CsvSource({
    "'[', '>='",
    "'(', '>'",
    "']', '>'"
  })
  void transformLowerEndpointComparator(String startIntervalSymbol, String expectedComparator) {
    assertThat(intervalTransformer.transformLowerEndpointComparator(startIntervalSymbol)).isEqualTo(expectedComparator);
  }

  @ParameterizedTest
  @CsvSource({
    "']', '<='",
    "')', '<'",
    "'[', '<'"
  })
  void transformUpperEndpointComparator(String stopIntervalSymbol, String expectedComparator) {
    assertThat(intervalTransformer.transformUpperEndpointComparator(stopIntervalSymbol)).isEqualTo(expectedComparator);
  }

  @Test
  void transformsValidIntervalExpression() {
    String juelExpression = intervalTransformer.transform(feelToJuelTransform, "[0..12]", "x");
    assertThat(juelExpression).isEqualTo("x >= 0 && x <= 12");
  }

  @Test
  void transformThrowsOnInvalidIntervalExpression() {
    assertThatThrownBy(() -> intervalTransformer.transform(feelToJuelTransform, "0..12", "x"))
      .isInstanceOf(FeelSyntaxException.class);
  }

  @ParameterizedTest
  @CsvSource({
    "'[0..12]', '[', '0', '12', ']'",
    "'[0..12)', '[', '0', '12', ')'",
    "'[0..12[', '[', '0', '12', '['",
    "'(0..12]', '(', '0', '12', ']'",
    "'(0..12)', '(', '0', '12', ')'",
    "'(0..12[', '(', '0', '12', '['",
    "']0..12]', ']', '0', '12', ']'",
    "']0..12)', ']', '0', '12', ')'",
    "']0..12[', ']', '0', '12', '['",
    "'[0.12..13.37]', '[', '0.12', '13.37', ']'",
    "'[.12...37]', '[', '.12', '.37', ']'",
    "'[a..b]', '[', 'a', 'b', ']'",
    "'[customer.age..customer.maxAge]', '[', 'customer.age', 'customer.maxAge', ']'",
    "'[a..b..c]', '[', 'a', 'b..c', ']'"
  })
  void intervalPatternMatchesValidIntervalExpressions(String feelExpression, String expectedStartSymbol,
      String expectedLowerEndpoint, String expectedUpperEndpoint, String expectedStopSymbol) {
    Matcher matcher = IntervalTransformer.INTERVAL_PATTERN.matcher(feelExpression);

    assertThat(matcher.matches()).isTrue();
    assertThat(matcher.group(1)).isEqualTo(expectedStartSymbol);
    assertThat(matcher.group(2)).isEqualTo(expectedLowerEndpoint);
    assertThat(matcher.group(3)).isEqualTo(expectedUpperEndpoint);
    assertThat(matcher.group(4)).isEqualTo(expectedStopSymbol);
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "0..12",     // missing start and stop interval symbols
    "[0..12",    // missing stop interval symbol
    "0..12]",    // missing start interval symbol
    "[0.12]",    // missing '..' separator
    "[..12]",    // missing lower endpoint
    "[0..]",     // missing upper endpoint
    "[0.]",      // missing separator and endpoint
    "{0..12}",   // invalid start/stop symbols
    "",          // empty expression
    " [0..12]",  // leading whitespace
    "[0..12] ",  // trailing whitespace
    "[0..12]x"   // trailing garbage
  })
  void intervalPatternDoesNotMatchInvalidIntervalExpressions(String feelExpression) {
    Matcher matcher = IntervalTransformer.INTERVAL_PATTERN.matcher(feelExpression);

    assertThat(matcher.matches()).isFalse();
  }

}
