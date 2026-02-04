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
package org.operaton.commons.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Stefan Hentschel.
 */
class EnsureUtilTest {

  @Test
  void ensureNotNull() {
    var string = "string";

    assertThatCode(() -> EnsureUtil.ensureNotNull("string", string))
      .doesNotThrowAnyException();
  }

  @Test
  void shouldFailEnsureNotNull() {
    String string = null;
    assertThatThrownBy(() -> EnsureUtil.ensureNotNull("string", string))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void ensureParameterInstanceOfClass() {
    Object string = "string";

    assertThatCode(() ->
      assertThat(EnsureUtil.ensureParamInstanceOf("string", string, String.class))
        .isInstanceOf(String.class)).doesNotThrowAnyException();
  }

  @Test
  void shouldFailEnsureParameterInstanceOfClass() {
    Object string = "string";
    assertThatThrownBy(() -> EnsureUtil.ensureParamInstanceOf("string", string, Integer.class))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
