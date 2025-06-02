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
 */
package org.operaton.bpm.dmn.feel.impl.juel.transform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import org.operaton.bpm.dmn.feel.impl.juel.FeelSyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListTransformerTest {

  ListTransformer transformer = new ListTransformer();

  FeelToJuelTransform mockTransform;

  @BeforeEach
  void setUp() {
    mockTransform = mock(FeelToJuelTransform.class);
  }

  @Test
  void canTransformReturnsTrueForMultipleExpressions() {
    assertTrue(transformer.canTransform("1, 2, 3"));
  }

  @Test
  void canTransformReturnsFalseForSingleExpression() {
    assertFalse(transformer.canTransform("1"));
  }

  @Test
  void transformReturnsJoinedJuelExpression() {
    when(mockTransform.transformSimplePositiveUnaryTest("1", "input")).thenReturn("input == 1");
    // Note: The leading space before "2" is preserved in the input "1, 2"
    when(mockTransform.transformSimplePositiveUnaryTest(" 2", "input")).thenReturn("input == 2");

    String result = transformer.transform(mockTransform, "1, 2", "input");

    assertEquals("(input == 1) || (input == 2)", result);
    verify(mockTransform).transformSimplePositiveUnaryTest("1", "input");
    verify(mockTransform).transformSimplePositiveUnaryTest(" 2", "input");
  }

  @ParameterizedTest
  @ValueSource(strings = {"1, , 3", "1,  , 3", "1,,,4", ",1,2,3,", ",1,2,3", "1,2,3,"})
  void throwsExceptionForEmptyExpressions(String invalidExpression) {
    Exception exception = assertThrows(FeelSyntaxException.class, () ->
      transformer.transform(mockTransform, invalidExpression, "input"));

    assertTrue(exception.getMessage().contains("can not have empty elements"));
  }

  @Test
  void splitExpressionHandlesQuotedCommasCorrectly() {
    List<String> result = transformer.collectExpressions("\"1,2\", 3");

    assertEquals(2, result.size());
    assertEquals("\"1,2\"", result.get(0));
    assertEquals(" 3", result.get(1));
  }

  @Test
  void joinExpressionsCombinesMultipleExpressionsWithOr() {
    String result = transformer.joinExpressions(List.of("input == 1", "input == 2"));

    assertEquals("(input == 1) || (input == 2)", result);
  }

  @Test
  void handlesNestedQuotesCorrectly() {
    List<String> result = transformer.collectExpressions("\"a\"\"b\", c");

    assertEquals(2, result.size());
    assertEquals("\"a\"\"b\"", result.get(0));
    assertEquals(" c", result.get(1));
  }

  @Test
  void handlesQuotedExpressions() {
    when(mockTransform.transformSimplePositiveUnaryTest("\"abc\"", "input")).thenReturn("input == \"abc\"");
    when(mockTransform.transformSimplePositiveUnaryTest(" \"def\"", "input")).thenReturn("input == \"def\"");

    String result = transformer.transform(mockTransform, "\"abc\", \"def\"", "input");

    assertEquals("(input == \"abc\") || (input == \"def\")", result);
  }

  @Test
  void handlesComplexNestedExpressions() {
    when(mockTransform.transformSimplePositiveUnaryTest("\"a,b,c\"", "x")).thenReturn("x == \"a,b,c\"");
    when(mockTransform.transformSimplePositiveUnaryTest(" d", "x")).thenReturn("x == d");

    String result = transformer.transform(mockTransform, "\"a,b,c\", d", "x");

    assertEquals("(x == \"a,b,c\") || (x == d)", result);
  }

  @Test
  void handlesEmptyQuotedString() {
    when(mockTransform.transformSimplePositiveUnaryTest("\"\"", "x")).thenReturn("x == \"\"");
    when(mockTransform.transformSimplePositiveUnaryTest(" not empty", "x")).thenReturn("x != \"\"");

    String result = transformer.transform(mockTransform, "\"\", not empty", "x");

    assertEquals("(x == \"\") || (x != \"\")", result);
  }

  @Test
  void handlesWhitespaceAroundCommas() {
    when(mockTransform.transformSimplePositiveUnaryTest("1 ", "input")).thenReturn("input == 1");
    when(mockTransform.transformSimplePositiveUnaryTest(" 2 ", "input")).thenReturn("input == 2");
    when(mockTransform.transformSimplePositiveUnaryTest(" 3", "input")).thenReturn("input == 3");

    String result = transformer.transform(mockTransform, "1 , 2 , 3", "input");

    assertEquals("(input == 1) || (input == 2) || (input == 3)", result);
  }
}
