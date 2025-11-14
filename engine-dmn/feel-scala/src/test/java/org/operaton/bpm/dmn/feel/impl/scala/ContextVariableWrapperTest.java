package org.operaton.bpm.dmn.feel.impl.scala;

import camundajar.impl.scala.Option;
import camundajar.impl.scala.Some;
import org.junit.jupiter.api.Test;
import org.operaton.bpm.engine.variable.context.VariableContext;
import org.operaton.bpm.engine.variable.impl.value.PrimitiveTypeValueImpl;

import java.util.Set;

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
