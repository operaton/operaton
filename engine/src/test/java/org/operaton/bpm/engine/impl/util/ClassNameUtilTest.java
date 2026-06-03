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
package org.operaton.bpm.engine.impl.util;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ClassNameUtilTest {

  @ParameterizedTest
  @MethodSource("classArgs")
  void getClassNameWithoutPackage_withClass_shouldReturnSimpleName(Class<?> clazz, String expected) {
    assertThat(ClassNameUtil.getClassNameWithoutPackage(clazz)).isEqualTo(expected);
  }

  static Stream<Arguments> classArgs() {
    return Stream.of(
      arguments(String.class,        "String"),
      arguments(ClassNameUtil.class, "ClassNameUtil"),
      arguments(Object.class,        "Object"),
      arguments(int[].class,         "[I")
    );
  }

  @ParameterizedTest
  @MethodSource("objectArgs")
  void getClassNameWithoutPackage_withObject_shouldReturnSimpleName(Object object, String expected) {
    assertThat(ClassNameUtil.getClassNameWithoutPackage(object)).isEqualTo(expected);
  }

  static Stream<Arguments> objectArgs() {
    return Stream.of(
      arguments("hello",          "String"),
      arguments(42,               "Integer"),
      arguments(new Object(),     "Object")
    );
  }
}
