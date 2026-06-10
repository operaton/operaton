/*
 * Copyright 2026 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0; you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.run.property;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperatonBpmRunAuthenticationPropertiesTest {

  @Test
  void shouldKeepBasicAuthenticationAsDefault() {
    OperatonBpmRunAuthenticationProperties properties = new OperatonBpmRunAuthenticationProperties();

    assertThat(properties.getAuthentication()).isEqualTo(OperatonBpmRunAuthenticationProperties.BASIC_AUTH);
  }

  @Test
  void shouldAcceptSupportedAuthenticationModes() {
    OperatonBpmRunAuthenticationProperties properties = new OperatonBpmRunAuthenticationProperties();

    properties.setAuthentication(OperatonBpmRunAuthenticationProperties.COMPOSITE_AUTH);
    assertThat(properties.getAuthentication()).isEqualTo(OperatonBpmRunAuthenticationProperties.COMPOSITE_AUTH);

    properties.setAuthentication(OperatonBpmRunAuthenticationProperties.PSEUDO_AUTH);
    assertThat(properties.getAuthentication()).isEqualTo(OperatonBpmRunAuthenticationProperties.PSEUDO_AUTH);

    properties.setAuthentication(OperatonBpmRunAuthenticationProperties.BASIC_AUTH);
    assertThat(properties.getAuthentication()).isEqualTo(OperatonBpmRunAuthenticationProperties.BASIC_AUTH);
  }

  @Test
  void shouldRejectUnsupportedAuthenticationMode() {
    OperatonBpmRunAuthenticationProperties properties = new OperatonBpmRunAuthenticationProperties();

    assertThatThrownBy(() -> properties.setAuthentication("unknown"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("basic", "composite", "pseudo");
  }

}
