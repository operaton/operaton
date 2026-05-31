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
package org.operaton.bpm.engine.impl.calendar;

import java.util.Date;
import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DateTimeUtilTest {

  @Test
  void now_shouldReturnDateTimeCloseToCurrentTime() {
    long before = System.currentTimeMillis();
    DateTime now = DateTimeUtil.now();
    long after = System.currentTimeMillis();

    assertThat(now.getMillis()).isBetween(before, after);
  }

  @ParameterizedTest
  @MethodSource("parseDateTimeArgs")
  void parseDateTime_shouldParseIsoString(String isoString, int year, int month, int day, int hour, int minute) {
    DateTime result = DateTimeUtil.parseDateTime(isoString);
    assertThat(result.getYear()).isEqualTo(year);
    assertThat(result.getMonthOfYear()).isEqualTo(month);
    assertThat(result.getDayOfMonth()).isEqualTo(day);
    assertThat(result.getHourOfDay()).isEqualTo(hour);
    assertThat(result.getMinuteOfHour()).isEqualTo(minute);
  }

  static Stream<Arguments> parseDateTimeArgs() {
    return Stream.of(
      arguments("2025-01-15T10:30:00", 2025, 1,  15, 10, 30),
      arguments("2024-12-31T23:59:00", 2024, 12, 31, 23, 59),
      arguments("2000-06-01T00:00:00", 2000, 6,  1,  0,  0)
    );
  }

  @Test
  void parseDate_shouldReturnJavaUtilDate() {
    Date result = DateTimeUtil.parseDate("2025-06-01T12:00:00");
    assertThat(result).isNotNull().isInstanceOf(Date.class);

    java.util.Calendar cal = java.util.Calendar.getInstance();
    cal.setTime(result);
    assertThat(cal.get(java.util.Calendar.YEAR)).isEqualTo(2025);
    assertThat(cal.get(java.util.Calendar.MONTH)).isEqualTo(java.util.Calendar.JUNE);
    assertThat(cal.get(java.util.Calendar.DAY_OF_MONTH)).isEqualTo(1);
  }
}
