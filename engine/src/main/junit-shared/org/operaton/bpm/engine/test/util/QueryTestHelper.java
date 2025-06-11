/*
 * Copyright 2025 the Operaton contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.engine.test.util;

import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.query.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public final class QueryTestHelper {

  private QueryTestHelper() {
  }

  public static void verifyQueryResults(Query<?, ?> query, int countExpected) {
    assertThat(query.list()).hasSize(countExpected);
    assertThat(query.count()).isEqualTo(countExpected);

    if (countExpected == 1) {
      assertThat(query.singleResult()).isNotNull();
    } else if (countExpected > 1) {
      assertThatThrownBy(query::singleResult).isInstanceOf(ProcessEngineException.class).hasMessageMatching("Query return \\d+ results instead of max 1");
    } else if (countExpected == 0) {
      assertThat(query.singleResult()).isNull();
    }
  }
}