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
package org.operaton.bpm.engine.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.ProcessEngine;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;


/**
 * @author Tobias Metzke
 *
 */
@ExtendWith(ProcessEngineExtension.class)
class StringUtilTest {

  ProcessEngine processEngine;

  @Test
  void shouldAllowTrimToMaximumLength() {
    // given
    String fittingThreeByteMessage = repeatCharacter("\u9faf", StringUtil.DB_MAX_STRING_LENGTH);
    String exceedingMessage = repeatCharacter("a", StringUtil.DB_MAX_STRING_LENGTH * 2);

    // then
    assertThat(fittingThreeByteMessage.substring(0, StringUtil.DB_MAX_STRING_LENGTH)).isEqualTo(StringUtil.trimToMaximumLengthAllowed(fittingThreeByteMessage));
    assertThat(exceedingMessage.substring(0, StringUtil.DB_MAX_STRING_LENGTH)).isEqualTo(StringUtil.trimToMaximumLengthAllowed(exceedingMessage));
  }

  @Test
  void shouldConvertByteArrayToString() {
    // given
    String message = "This is a message string";
    byte[] bytes = message.getBytes();

    // when
    String stringFromBytes = StringUtil.fromBytes(bytes, processEngine);

    // then
    assertThat(stringFromBytes).isEqualTo(message);
  }

  @Test
  void shouldConvertNullByteArrayToEmptyString() {
    // given
    byte[] bytes = null;

    // when
    String stringFromBytes = StringUtil.fromBytes(bytes, processEngine);

    // then
    assertThat(stringFromBytes).isEmpty();
  }

  protected static String repeatCharacter(String encodedCharacter, int numCharacters) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < numCharacters; i++) {
      sb.append(encodedCharacter);
    }
    return sb.toString();
  }
}
