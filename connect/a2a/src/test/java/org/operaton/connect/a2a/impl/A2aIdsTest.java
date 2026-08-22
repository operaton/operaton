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
package org.operaton.connect.a2a.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole point of these ids is that a retried job produces the same ones, so that is what is asserted here.
 */
class A2aIdsTest {

  private static final String KEY = "process-instance-1-activity-instance-1";

  @Test
  void messageIdIsStableForTheSameKey() {
    assertThat(A2aIds.messageId(KEY)).isEqualTo(A2aIds.messageId(KEY));
  }

  @Test
  void contextIdIsStableForTheSameKey() {
    assertThat(A2aIds.contextId(KEY)).isEqualTo(A2aIds.contextId(KEY));
  }

  @Test
  void differentKeysProduceDifferentIds() {
    assertThat(A2aIds.messageId(KEY)).isNotEqualTo(A2aIds.messageId(KEY + "-other"));
  }

  @Test
  void messageAndContextIdDifferForTheSameKey() {
    assertThat(A2aIds.messageId(KEY)).isNotEqualTo(A2aIds.contextId(KEY));
  }

  @Test
  void idsArePrefixedAndBounded() {
    assertThat(A2aIds.messageId(KEY)).startsWith("msg-").hasSize(36);
    assertThat(A2aIds.contextId(KEY)).startsWith("ctx-").hasSize(36);
  }

  @Test
  void withoutAKeyIdsAreRandomSoNothingIsAccidentallyDeduplicated() {
    assertThat(A2aIds.messageId(null)).isNotEqualTo(A2aIds.messageId(null));
    assertThat(A2aIds.contextId(null)).isNotEqualTo(A2aIds.contextId(null));
  }

}
