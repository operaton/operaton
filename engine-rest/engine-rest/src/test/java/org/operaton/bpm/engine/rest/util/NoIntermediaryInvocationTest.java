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
package org.operaton.bpm.engine.rest.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoAssertionError;

import static org.operaton.bpm.engine.rest.helper.NoIntermediaryInvocation.immediatelyAfter;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Thorben Lindhauer
 */
@SuppressWarnings("java:S5778")
class NoIntermediaryInvocationTest {

  protected Foo foo;

  @BeforeEach
  void setUp() {
    foo = Mockito.mock(Foo.class);
  }

  @Test
  void testSucceeds() {

    // given
    foo.getFoo();
    foo.getBar();

    // when
    InOrder inOrder = Mockito.inOrder(foo);
    inOrder.verify(foo).getFoo();

    // then
    inOrder.verify(foo, immediatelyAfter()).getBar();

  }

  @Test
  void testFailureWhenInvocationNotPresent() {
    // given
    foo.getFoo();
    foo.getBaz();

    // when
    InOrder inOrder = Mockito.inOrder(foo);
    inOrder.verify(foo).getFoo();

    // then
    assertThatThrownBy(() -> inOrder.verify(foo, immediatelyAfter()).getBar())
      .isInstanceOf(MockitoAssertionError.class);
  }

  @Test
  void testFailureWhenInvocationNotPresentCase2() {
    // given
    foo.getFoo();

    // when
    InOrder inOrder = Mockito.inOrder(foo);
    inOrder.verify(foo).getFoo();

    // then
    assertThatThrownBy(() -> inOrder.verify(foo, immediatelyAfter()).getBar())
      .isInstanceOf(MockitoAssertionError.class);
  }

  @Test
  void testFailureOnWrongInvocationOrder() {
    // given
    foo.getBar();
    foo.getFoo();

    // when
    InOrder inOrder = Mockito.inOrder(foo);
    inOrder.verify(foo).getFoo();

    // then
    assertThatThrownBy(() -> inOrder.verify(foo, immediatelyAfter()).getBar())
      .isInstanceOf(MockitoAssertionError.class);
  }

  @Test
  void testFailureWithIntermittentInvocations() {
    // given
    foo.getFoo();
    foo.getBaz();
    foo.getBar();

    // when
    InOrder inOrder = Mockito.inOrder(foo);
    inOrder.verify(foo).getFoo();

    // then
    assertThatThrownBy(() -> inOrder.verify(foo, immediatelyAfter()).getBar())
      .isInstanceOf(MockitoAssertionError.class);
  }

  public interface Foo {

    String getFoo();

    String getBar();

    String getBaz();
  }
}
