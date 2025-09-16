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
package org.operaton.commons.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class MdcAccessTest {

  @BeforeEach
  void setUp() {
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void shouldPutValueToMdc() {
    // when
    MdcAccess.put("foo", "bar");
    // then
    assertThat(MDC.getCopyOfContextMap()).hasSize(1);
    assertThat(MDC.get("foo")).isEqualTo("bar");
  }

  @Test
  void shouldPutNullValueToMdc() {
    // given
    MDC.put("foo", "bar");
    // when
    MdcAccess.put("foo", null);
    // then
    assertThat(MDC.get("foo")).isNull();
  }

  @Test
  void shouldNotPutNullKeyToMdc() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
      // when
      MdcAccess.put(null, "bar"));
  }

  @Test
  void shouldGetValueFromMdc() {
    // given
    MDC.put("foo", "bar");
    // when
    String value = MdcAccess.get("foo");
    // then
    assertThat(value).isEqualTo("bar");
  }

  @Test
  void shouldNotGetNullKeyFromMdc() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
      // when
      MdcAccess.get(null));
  }

  @Test
  void shouldRemoveFromMdc() {
    // given
    MDC.put("foo", "bar");
    MDC.put("baz", "fooz");
    // when
    MdcAccess.remove("foo");
    // then
    assertThat(MDC.getCopyOfContextMap()).hasSize(1);
    assertThat(MDC.get("baz")).isEqualTo("fooz");
  }

  @Test
  void shouldNotRemoveNullKeyFromMdc() {
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
      // when
      MdcAccess.remove(null));
  }

}
