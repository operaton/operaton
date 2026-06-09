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
package org.operaton.bpm.engine.test.api.form;

import java.util.Date;

import org.junit.jupiter.api.Test;

import org.operaton.bpm.engine.impl.form.type.DateFormType;

import static org.assertj.core.api.Assertions.assertThat;

class DateFormTypeTest {

  @SuppressWarnings("deprecation")
  @Test
  void shouldAcceptExistingDateAsLegacyFormValue() {
    Date date = new Date(1_700_000_000_000L);

    Object modelValue = new DateFormType("dd/MM/yyyy").convertFormValueToModelValue(date);

    assertThat(modelValue).isSameAs(date);
  }

}
