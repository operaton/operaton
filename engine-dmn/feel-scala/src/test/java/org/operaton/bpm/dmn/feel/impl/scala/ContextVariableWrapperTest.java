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
package org.operaton.bpm.dmn.feel.impl.scala;

import java.util.Set;

import camundajar.impl.scala.Option;
import camundajar.impl.scala.Some;
import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class ContextVariableWrapperTest {
  VariableContext context = mock(VariableContext.class);

  ContextVariableWrapper sut = new ContextVariableWrapper(context);

  @Test
  void getVariable_whenContextContainsBooleanVariable_thenReturnsSomeWithValue() {
    // given
    var value = new PrimitiveTypeValueImpl.BooleanValueImpl(TRUE);
    doReturn(true).when(context).containsVariable("myVar");
    doReturn(value).when(context).resolve("myVar");

    // when
    Option<?> result = sut.getVariable("myVar");

    // then
    assertThat(result).isEqualTo(new Some<Boolean>(TRUE));
  }

  @Test
  void keys_whenContextHasKeys_thenReturnsAllKeys() {
    // given
    doReturn(Set.of("a", "b", "c")).when(context).keySet();

    // when
    var result = sut.keys().toList();

    // then
    assertThat(result.size()).isEqualTo(3);
    assertThat(result.contains("a")).isTrue();
    assertThat(result.contains("b")).isTrue();
    assertThat(result.contains("c")).isTrue();
    assertThat(result.contains("d")).isFalse();
  }
}
