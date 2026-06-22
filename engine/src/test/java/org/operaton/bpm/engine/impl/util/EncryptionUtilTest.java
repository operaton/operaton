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

class EncryptionUtilTest {

  @ParameterizedTest
  @MethodSource("saltPasswordArgs")
  void saltPassword_shouldConcatenatePasswordAndSalt(String password, String salt, String expected) {
    assertThat(EncryptionUtil.saltPassword(password, salt)).isEqualTo(expected);
  }

  static Stream<Arguments> saltPasswordArgs() {
    return Stream.of(
      arguments("password", "salt123",  "passwordsalt123"),
      arguments("password", "",         "password"),
      arguments("password", null,       "password"),   // null salt treated as empty per pre-7.7 compat
      arguments("",         "salt",     "salt"),
      arguments("secret",   "abc",      "secretabc")
    );
  }
}
