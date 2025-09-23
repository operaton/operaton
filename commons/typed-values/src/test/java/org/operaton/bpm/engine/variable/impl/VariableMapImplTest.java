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
package org.operaton.bpm.engine.variable.impl;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

class VariableMapImplTest {
    @ParameterizedTest(name = "{index} - value={1}")
    @MethodSource("containsValue_shouldReturnExpected_args")
    void containsValue_shouldReturnExpected(VariableMapImpl variableMap, Object value, boolean expected) {
        boolean result = variableMap.containsValue(value);
        assertThat(result).isEqualTo(expected);
    }

    static Stream<Arguments> containsValue_shouldReturnExpected_args() {
        return Stream.of(
            Arguments.of(new VariableMapImpl().putValue("key", "testValue"), "testValue", true),
            Arguments.of(new VariableMapImpl().putValue("key", 123), 123, true),
            Arguments.of(new VariableMapImpl().putValue("key", null), null, true),
            Arguments.of(new VariableMapImpl(), null, false),
            Arguments.of(new VariableMapImpl(), "nonExistentValue", false)
        );
    }

}
