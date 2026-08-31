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
package org.operaton.connect.a2a.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.operaton.connect.ConnectorRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A literal input parameter in a diagram is always a string, while the same parameter written as an expression
 * can be any type. Both have to work, so both are covered here, together with the null, empty and blank cases.
 */
class A2aParamsTest {

  private final A2aRequestImpl request = new A2aRequestImpl(null);

  @Test
  void stringIsTrimmedAndBlankCountsAsAbsent() {
    request.setRequestParameter("a", "  hello  ");
    request.setRequestParameter("b", "   ");
    request.setRequestParameter("c", "");

    assertThat(A2aParams.string(request, "a")).isEqualTo("hello");
    assertThat(A2aParams.string(request, "b")).isNull();
    assertThat(A2aParams.string(request, "c")).isNull();
    assertThat(A2aParams.string(request, "missing")).isNull();
  }

  @Test
  void integerAcceptsBothLiteralsAndExpressionResults() {
    request.setRequestParameter("literal", "3000");
    request.setRequestParameter("expression", 4000);
    request.setRequestParameter("padded", " 5000 ");

    assertThat(A2aParams.integer(request, "literal", 1)).isEqualTo(3000);
    assertThat(A2aParams.integer(request, "expression", 1)).isEqualTo(4000);
    assertThat(A2aParams.integer(request, "padded", 1)).isEqualTo(5000);
    assertThat(A2aParams.integer(request, "missing", 42)).isEqualTo(42);
    assertThat(A2aParams.integerOrNull(request, "missing")).isNull();
  }

  @Test
  void aNonNumericNumberIsRejectedWithTheParameterName() {
    request.setRequestParameter("waitTimeout", "soon");

    assertThatThrownBy(() -> A2aParams.integer(request, "waitTimeout", 1))
        .isInstanceOf(ConnectorRequestException.class)
        .hasMessageContaining("waitTimeout");
  }

  @Test
  void booleanAcceptsBothLiteralsAndExpressionResults() {
    request.setRequestParameter("literal", "true");
    request.setRequestParameter("expression", Boolean.TRUE);
    request.setRequestParameter("nonsense", "yes");

    assertThat(A2aParams.bool(request, "literal", false)).isTrue();
    assertThat(A2aParams.bool(request, "expression", false)).isTrue();
    assertThat(A2aParams.bool(request, "nonsense", true)).isFalse();
    assertThat(A2aParams.bool(request, "missing", true)).isTrue();
  }

  @Test
  void headerMapDropsEntriesWithoutAValue() {
    Map<String, Object> headers = new LinkedHashMap<>();
    headers.put("Authorization", "Bearer token");
    headers.put("X-Empty", null);
    request.setRequestParameter("headers", headers);

    assertThat(A2aParams.stringMap(request, "headers"))
        .containsExactly(Map.entry("Authorization", "Bearer token"));
  }

  @Test
  void stringListSplitsACommaSeparatedLiteral() {
    request.setRequestParameter("modes", "text/plain, application/json ,");

    assertThat(A2aParams.stringList(request, "modes")).containsExactly("text/plain", "application/json");
  }

  @Test
  void stringListAlsoAcceptsARealList() {
    request.setRequestParameter("modes", List.of("text/plain", "application/json"));

    assertThat(A2aParams.stringList(request, "modes")).containsExactly("text/plain", "application/json");
  }

  @Test
  void absentCollectionsAreEmptyRatherThanNull() {
    assertThat(A2aParams.stringMap(request, "missing")).isEmpty();
    assertThat(A2aParams.objectMap(request, "missing")).isEmpty();
    assertThat(A2aParams.stringList(request, "missing")).isEmpty();
    assertThat(A2aParams.mapList(request, "missing")).isEmpty();
  }

  @Test
  void aMapWhereAListWasExpectedIsRejected() {
    request.setRequestParameter("parts", Map.of("type", "text"));

    assertThatThrownBy(() -> A2aParams.mapList(request, "parts"))
        .isInstanceOf(ConnectorRequestException.class)
        .hasMessageContaining("parts");
  }

  @Test
  void partsMustBeAListOfMaps() {
    request.setRequestParameter("parts", List.of("not a map"));

    assertThatThrownBy(() -> A2aParams.mapList(request, "parts"))
        .isInstanceOf(ConnectorRequestException.class);
  }

}
